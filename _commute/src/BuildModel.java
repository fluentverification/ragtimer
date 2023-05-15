// This provides a simulate package
package simulate;

// File handling
import java.io.File;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

// Array handling
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.transform.Source;

// PRISM Parser things
import parser.Values;
import parser.ast.Expression;
import parser.ast.ModulesFile;
import parser.State;

// PRISM things
import prism.Prism;
import prism.PrismDevNullLog;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismPrintStreamLog;

// PRISM Simulator
import simulator.SimulatorEngine;

import java.lang.*;
import java.util.*;

// This class takes care of everything we need the PRISM API for
public class BuildModel
{

  public class MemoryMonitor {
      private int maxUsedMemory;
      Runtime currentRuntime;
      /**
              Constructor to initialize maxUsedMemory to 0
      */
      public MemoryMonitor() {
              maxUsedMemory = 0;
              currentRuntime = Runtime.getRuntime(); // In Java, everything is a pointer so this won't duplicate memory
      }
      /**
              You will want to call this method whenever you think memory may be peaking,
              or whenever you allocate something.
      */
      public void registerMemoryUsage() {
              int usedMemory = (int)currentRuntime.totalMemory() - (int)currentRuntime.freeMemory();
              if (usedMemory > this.maxUsedMemory) {
                      this.maxUsedMemory = usedMemory;
              }
      }
      /**
              Call this whenever you wish to see the max used memory of your program up to that point
      */
      public int getMaxUsedMemory() {
              return maxUsedMemory;
      }
  }

  MemoryMonitor mm = new MemoryMonitor();

  // User-passed parameter file, see docs/tool/input.md
  public static final String OPTION_FILE = "options.txt";
  
  // Required parameters
  public String MODEL_NAME = "model.sm";
  public String TRACE_LIST_NAME = "forprism.trace";
  public String PROPERTY = "A=100";
  public Expression target;

  // Optional parameters
  public int MAX_DEPTH = 30;
  public double TIME_BOUND = 200.0;
  public boolean DO_PRINT = false;
  public boolean EXPORT_PRISM = true;
  public boolean EXPORT_STORM = true;
  public double FLEXIBILITY = 1.0f;
  public boolean TERMINATE_TIME = true;
  public boolean TERMINATE_DEPTH = true;
  public int CYCLE_LENGTH = 0;
  public double REMOVE_TOLERANCE = 0.0f;
  public int CYCLE_INIT;
  
  // By default, call BuildModel().run()
  public static void main(String[] args)
  {
    new BuildModel().run();
  }

  public void getParams() {
	System.out.println("Getting Parameters from input file");
	try {
		FileReader fr = new FileReader(OPTION_FILE);
		BufferedReader br = new BufferedReader(fr);
		String line;
		while ((line = br.readLine()) != null) {
		  String first = line.split(" ")[0];
		  String parameter = "";
		  if (line.split(" ").length > 1) {
			parameter = line.split(" ")[1];
		  }
		  if (first.contains("model")) {
			MODEL_NAME = parameter;
			System.out.println("Model Name: " + MODEL_NAME);
		  }
		  else if (first.contains("trace")) {
			TRACE_LIST_NAME = parameter;
			System.out.println("Trace List Name: " + TRACE_LIST_NAME);
		  }
		  else if (first.contains("property")) {
			PROPERTY = "";
			for (int i = 1; i < line.split(" ").length; i++) {
				PROPERTY += line.split(" ")[i];
			}
			// PROPERTY = parameter;
			System.out.println("Property: " + PROPERTY);
		  }
		  else if (first.contains("timeBound")) {
			TIME_BOUND = Double.parseDouble(parameter);
			System.out.println("Time Bound: " + TIME_BOUND);
		  }
		  else if (first.contains("recursionBound")) {
			MAX_DEPTH = Integer.parseInt(parameter);
			System.out.println("Max Depth: " + MAX_DEPTH);
		  }
		  else if (first.contains("export")) {
			if (parameter.contains("storm")) {
			  EXPORT_PRISM = false;
			  System.out.println("Export Storm Only");
			}
			else if (parameter.contains("prism")) {
			  EXPORT_STORM = false;
			  System.out.println("Export Prism Only");
			}
		  }
		  else if (first.contains("verbose")) {
        if (parameter.contains("t")) {
          DO_PRINT = true;
          System.out.println("Verbose mode enabled");
        }
        else {
          System.out.println("Verbose mode disabled");
        }
		  }
      else if (first.contains("terminate")) {
        if (parameter.contains("time")) {
          TERMINATE_DEPTH = false;
          System.out.println("Terminating with time bound");
        }
        else if (parameter.contains("depth")) {
          TERMINATE_TIME = false;
          System.out.println("Terminating with depth bound");
        }
        else {
          System.out.println("Terminating with time and depth, whichever comes first");
        }
		  }
      else if (first.contains("flexibility")) {
        FLEXIBILITY = Double.parseDouble(parameter);
        System.out.println("Flexibility: " + FLEXIBILITY);
      }
      else if (first.contains("cycleLength")) {
        CYCLE_LENGTH = Integer.parseInt(parameter);
        CYCLE_INIT = 2 * CYCLE_LENGTH; // allows stoichiometry constant of 2
        System.out.println("Cycle length: " + CYCLE_LENGTH);
        System.out.println("Cycle initial values for testing: " + CYCLE_INIT);
      }
      else if (first.contains("removeTolerance")) {
        REMOVE_TOLERANCE = Double.parseDouble(parameter);
        System.out.println("Removal tolerance: " + REMOVE_TOLERANCE);
      }
		}
    if (TERMINATE_TIME) TIME_BOUND = TIME_BOUND * FLEXIBILITY;
    if (TERMINATE_TIME) System.out.println("Terminating recursion at a time bound of " + TIME_BOUND);
		br.close();
	}
	catch (FileNotFoundException e) {
		System.out.println("FileNotFoundException Error: " + e.getMessage());
		System.exit(1);
	}
	catch (IOException e) {
		System.out.println("IOException Error: " + e.getMessage());
		System.exit(1);
	}
  }

  public int[] getIntVarVals(Object[] varVals) {
    int[] retArr = new int[varVals.length];
    // System.out.printf("getIntVarVals:");
    for (int i = 0; i < varVals.length; i++) {
      // Check if Object is an Integer or a String, handle appropriately
      if (varVals[i] instanceof Integer) {
        retArr[i] = (Integer) varVals[i];
        // System.out.printf(" I_%03d", retArr[i]);
        // intVars.add((Integer) vars.get(i));
      }
      else if (varVals[i] instanceof String) {
        retArr[i] = Integer.parseInt((String) varVals[i]);
        // System.out.printf(" S_%03d", retArr[i]);
        // intVars.add(Integer.parseInt((String) vars.get(i)));
      }
    }
    // System.out.println(".");
    return retArr;
  }

  // global running state index
  public int stateCount;
  public int transitionCount;

  // transition objects store the in-place index and the string name
  // for a state's outgoing transition in a single object.
  public class Transition {
    public int prismIndex;
    public int from;
    public int to;
    public double rate;
    public String name;
    public Transition(int prismIndex, String name, int from, int to, double rate) {
      this.prismIndex = prismIndex;
      this.name = name;
      this.from = from;
      this.to = to;
      this.rate = rate;
      transitionCount++;
    }
    public String prismTRA() {
      return (this.from) + " " + (this.to) + " " + (this.rate) + "\n";
    }
  }

  // global variable to store number of state variables
  public int numStateVariables;

  // state objects store the bulk of information about the model
  public class My_State {
    public int index;
    public int[] stateVars;
    public double totalOutgoingRate;
    public ArrayList<Transition> outgoingTrans;
    public ArrayList<My_State> nextStates;
    public boolean isTarget;
    public boolean isNewInit;
    public boolean isCycleState;

    public My_State(Object varVals[]) {
      this.isTarget = false;
      this.isNewInit = false;
      this.index = stateCount;
      // if (this.index == 709) { // strange debug procedure
      //   System.out.println("\n\n------------------------------------------------------------------------------------------\nAT STATE 709\n------------------------------------------------------------------------------------------\n\n");
      // }
      stateCount++;
      this.stateVars = getIntVarVals(varVals);
      this.totalOutgoingRate = 0.0;
      this.outgoingTrans = new ArrayList<Transition>();
      this.nextStates = new ArrayList<My_State>();
      this.isCycleState = false;
      stateList.add(this);

      if (DO_PRINT) System.out.printf("New state %s", this.prismSTA());
    }
    
    public double getMRT() {
      return 1.0f / totalOutgoingRate;
    }

    public My_State(int varVals[]) {
      this.isTarget = false;
      this.isNewInit = false;
      this.index = stateCount;
      stateCount++;
      this.stateVars = new int[numStateVariables];
      for (int i = 0; i < numStateVariables; i++) {
        this.stateVars[i] = varVals[i];
      }
      this.totalOutgoingRate = 0.0;
      this.outgoingTrans = new ArrayList<Transition>();
      this.nextStates = new ArrayList<My_State>();
      this.isCycleState = false;
      stateList.add(this);
      if (DO_PRINT) System.out.printf("New state %s", this.prismSTA());
    }

    // get outgoing rate not captured by notated transitions
    public double getAbsorbingRate() {
      double absorbRate = this.totalOutgoingRate;
      for (int i = 0; i < this.outgoingTrans.size(); i++) {
        absorbRate -= this.outgoingTrans.get(i).rate;
      }
      return absorbRate;
    }

    // My_State details for .sta files
    // Format is <index>:(<state_var>,<state_var>...)
    public String prismSTA() {
      String temp = (index) + ":(";
      for (int i=0; i<numStateVariables; i++) {
        if (i>0) temp += ",";
        temp += stateVars[i]; 
      }
      if (this.isTarget) {
        return temp + ") [ " + this.totalOutgoingRate + " TARGET ]\n";
      }
      return temp + ") [ " + this.totalOutgoingRate + " ]\n";
      // return temp + ")\n";
    }

    // My_State details for .tra files
    // Format is <index>:(<state_var>,<state_var>...)
    public String prismTRA() {
      String temp = "";
      for (int i=0; i<this.outgoingTrans.size(); i++) {
        temp += this.outgoingTrans.get(i).prismTRA(); 
      }
      return temp;
    }

  }

  public ArrayList<My_State> stateList = new ArrayList<My_State>();

  // TODO: FIX THIS ALGORITHM
  // add a parameter for how much pruning we want to do based on absorbing ratio
  public void removeDeadEnds() {

    boolean deadEndsExist = true;
    int statesRemoved = 0;
    int deadEndStatesRemoved = 0;
    int sinkStatesRemoved = 0;
    int transitionsRemoved = 0;

    boolean shouldRemoveDE = false;
    boolean shouldRemoveSINK = false;
    boolean shouldRemove = false;

    double averageRatio = 0.0f;
    double minRatio = 0.0f;
    double maxRatio = 0.0f;

    // uncomment to just debug the dead end removal
    // DO_PRINT = true;

    while (deadEndsExist) {
      if (DO_PRINT) System.out.println("\n\nDEAD ENDS EXIST LOOP BEGINS\n\n");
      deadEndsExist = false;
      for (int s = stateCount - 1; s > 0; s--) {
        if (stateList.get(s).isTarget) {
          continue;
        }
        shouldRemoveDE = (stateList.get(s).outgoingTrans.size() == 0);
        double removeRatio = (stateList.get(s).getAbsorbingRate() / stateList.get(s).totalOutgoingRate);
        // try only removing the additional cycle states for probability sinks
        shouldRemoveSINK = (stateList.get(s).isCycleState) && (REMOVE_TOLERANCE > 0.0f) && (removeRatio > REMOVE_TOLERANCE);
        shouldRemove = shouldRemoveDE || shouldRemoveSINK;

        if (shouldRemoveSINK) {
          if (DO_PRINT) System.out.println("Remove tolerance of " + REMOVE_TOLERANCE + " does not allow state " + s + " with ratio " + removeRatio);
          if (averageRatio == 0.0f) averageRatio = removeRatio;
          if (minRatio == 0.0f || removeRatio < minRatio) minRatio = removeRatio;
          if (maxRatio == 0.0f || removeRatio > maxRatio) maxRatio = removeRatio;
          else averageRatio = ((averageRatio * sinkStatesRemoved) + removeRatio) / (sinkStatesRemoved + 1);
        }
        if (shouldRemove) {
          if (DO_PRINT) System.out.printf("\n\nRemoving state " + s + " -- " + stateList.get(s).prismSTA());
          
          // remove any applicable transitions from state s (only applies for probability sinks)
          while (stateList.get(s).outgoingTrans.size() > 0) {
            if (DO_PRINT) System.out.printf("  Remove corresponding transition " + stateList.get(s).outgoingTrans.get(0).prismTRA());
            stateList.get(s).outgoingTrans.remove(0);
            transitionCount--;
            transitionsRemoved++;
          }
          
          // loop through whole state space
          for (int i = 0; i < stateCount; i++) {
            
            for (int j = 0; j < stateList.get(i).outgoingTrans.size(); j++) {
              
              // remove all s_i -> s_s
              if (stateList.get(i).outgoingTrans.get(j).to == s) {
                if (DO_PRINT) System.out.printf("  Remove %d->%d\n", stateList.get(i).outgoingTrans.get(j).from, stateList.get(i).outgoingTrans.get(j).to);
                stateList.get(i).outgoingTrans.remove(j);
                transitionCount--;
                transitionsRemoved++;
                j--; // (the index needs to correspond now that we removed one)
                continue;
              }
              
              // shift all s_i -> s_(>s) down by 1
              else if (stateList.get(i).outgoingTrans.get(j).to > s) {
                stateList.get(i).outgoingTrans.get(j).to--;
              }
              
              // shift all shift all s_(>s) -> s_l down by 1
              if (stateList.get(i).outgoingTrans.get(j).from > s) {
                stateList.get(i).outgoingTrans.get(j).from--;
              }
              
            }

            // set all s_(i>s) to s_(i-1)
            if (i > s) {
              stateList.get(i).index--;
              if (i-1 != stateList.get(i).index) System.out.println("INDICES NOT EQUAL. i=" + i + ", index=" + stateList.get(i).index);
            }
            
          }
          
          // remove state s
          stateList.remove(s);
          stateCount--;
          statesRemoved++;
          if (shouldRemoveDE) {
            deadEndStatesRemoved++;
          }
          else if (shouldRemoveSINK) {
            sinkStatesRemoved++;
          }

          if (DO_PRINT) {
            if (stateCount > s) {
              System.out.printf("Removed state " + s + ". State " + s + " is now " + stateList.get(s).prismSTA());
            }
            else {
              System.out.printf("Removed state " + s + ". End of state list.\n");
            }
          }

          deadEndsExist = true;

        }
      }
    }
    System.out.printf("Removed %d dead-end states and %d probability sink states (total %d states).\n", deadEndStatesRemoved, sinkStatesRemoved, statesRemoved);
    System.out.printf("Removed %d corresponding transitions.\n", transitionsRemoved);
    if (sinkStatesRemoved > 0) System.out.printf("Average ratio among removed states was %.4f (min %.4f and max %.4f).\nUser-provided tolerance was %.4f\n", averageRatio, minRatio, maxRatio, REMOVE_TOLERANCE);
  }


  public class Path {
    public ArrayList<My_State> states;
    public ArrayList<String> commutable;
    public Path() {
      this.states = new ArrayList<My_State>();
      this.commutable = new ArrayList<String>();
    }
  }


  // stores a SINGLE cycle without dealing with permutations
  public class Cycle {
    public String transitions;
    public int[] minVals;

    public Cycle(String transitions, int[] minVals) {
      this.transitions = transitions;
      this.minVals = minVals;
    }
  }

  public ArrayList<Cycle> cycleArray;
  public int cyclesAdded;
  public int newCycleStates;
  public int newCycleTrans;

  // recursively finds cycles within the appropriate length bounds
  public void findCyclePermutations(SimulatorEngine sim, State curState, ArrayList<String> reactionList, int cycleDepth, String cycle, int[] minVals) {
    try{
      mm.registerMemoryUsage();

      if (cycleDepth > CYCLE_LENGTH) return; // base case

      // System.out.println("Cycle: " + cycle);

      // System.out.println("Current State: " + sim.getCurrentState());
      int[] newMinVals = new int[numStateVariables];

      // set up our prism simulator at the desired state
      sim.initialisePath(curState);
      mm.registerMemoryUsage();

      // check if we have made it back to our base state
      boolean backToBase = true;
      int diff = 0;
      
      // loop through the state vars; see if we made if back to the origin
      if (cycleDepth > 0) {
        for (int i = 0; i < numStateVariables; i++) {
          // System.out.printf("%d ", getIntVarVals(sim.getCurrentState().varValues)[i]);
          diff = getIntVarVals(sim.getCurrentState().varValues)[i] - CYCLE_INIT;
          if (diff < minVals[i]) {
            newMinVals[i] = diff;
          }
          else {
            newMinVals[i] = minVals[i];
          }
          if (getIntVarVals(sim.getCurrentState().varValues)[i] != CYCLE_INIT) {
            backToBase = false;
          }
        }
        // System.out.println("");
      }
      else {
        backToBase = false;
      }
      
      if (backToBase) {
        cycleArray.add(new Cycle((cycle), newMinVals));
        mm.registerMemoryUsage();
        if (DO_PRINT) System.out.printf("Found cycle " + (cycle) + " with mins ");
        for (int i = 0; i < numStateVariables; i++) {
          if (DO_PRINT) System.out.printf("%d\t", newMinVals[i]);
        }
        if (DO_PRINT) System.out.println("");
        return;
      }
      else {
        // System.out.println("HERE with ");
        int numTransitions = reactionList.size();
        for (int i = 0; i < numTransitions; i++) {
          // simulate each reaction
          // start with a fresh transitionIndex
          int transitionIndex = -1;
          String desiredReaction = reactionList.get(i);
          // Compare our transition string with available transition strings
          for (int sim_tran = 0; sim_tran < sim.getNumTransitions(); sim_tran++) {
            // Update transitionIndex if we found the desired transition (i.e. names match)
            if (desiredReaction.equalsIgnoreCase(sim.getTransitionActionString(sim_tran))) {
              transitionIndex = sim_tran;
            }
          }
          // If we never found the correct transitions, try another reaction
          if (transitionIndex == -1) {
            continue;
          }
          // Take the transition
          sim.initialisePath(curState);
          sim.manualTransition(transitionIndex);
          // System.out.println("Took " + desiredReaction + " to get " + sim.getCurrentState());

          // pass in the new state plus the addition to the cycle and keep detecting
          findCyclePermutations(sim, sim.getCurrentState(), reactionList, cycleDepth+1, cycle + desiredReaction + "\t", newMinVals);
          
        }
      }
    }
    catch (PrismException e) {
			if (DO_PRINT) System.out.println("PrismException Error: " + e.getMessage());
			System.exit(1);
		}
  }

  // adds the cycles to the states
  public void simulateCycles(SimulatorEngine sim) {
    try {
      
      int existingStates = stateCount; // maybe look into adjusting this to grow as we add cycles, but need to guarantee termination
      
      //TODO: DEBUGGING ONLY
      int startScanning = 0;
      existingStates = startScanning + stateCount;

      // loop over every state
      for (int i = startScanning; i < existingStates; i++) {
      // for (int i = 0; i < existingStates; i++) {
        My_State currentState = stateList.get(i);
        // loop over every cycle
        for (int j = 0; j < 1; j++) {
        // for (int j = 0; j < cycleArray.size(); j++) {
          // check state variables against min values
          boolean minValOK = true;
          for (int k = 0; k < numStateVariables; k++) {
            // check min value against current state
            if (stateList.get(i).stateVars[k] + cycleArray.get(j).minVals[k] < 0) {
              minValOK = false;
              break;
            }
          }
          if (minValOK) {
            // set the prism state to the current state
            State cState = new State(numStateVariables);
            mm.registerMemoryUsage();
            for (int l = 0; l < numStateVariables; l++) {
              cState.setValue(l, stateList.get(i).stateVars[l]);
            }
            sim.initialisePath(cState);

            cyclesAdded++;

            if (DO_PRINT) System.out.println("Cycle insertion at state " + sim.getCurrentState());
  
            // simulate the cycle and add states and transitions
            int transitionIndex = -1;
            String[] cTrans = cycleArray.get(j).transitions.split("\\s+");
            for (int path_tran = 0; path_tran < cTrans.length; path_tran++) {
              // System.out.println("Attempting: " + cTrans[path_tran] + " - " + path_tran);
              transitionIndex = -1;
              double newTranRate = -1.0f;
              double totalOutgoingRate = 0.0f;
              // Compare our transition string with available transition strings
              for (int sim_tran = 0; sim_tran < sim.getNumTransitions(); sim_tran++) {
                // Update transitionIndex if we found the desired transition (i.e. names match)
                totalOutgoingRate += sim.getTransitionProbability(sim_tran);
                // if (DO_PRINT) System.out.println("Enabled transition " + sim.getTransitionActionString(sim_tran));
                if (cTrans[path_tran].equalsIgnoreCase(sim.getTransitionActionString(sim_tran))) {
                  transitionIndex = sim_tran;
                  newTranRate = sim.getTransitionProbability(sim_tran);
                  // System.out.println("Found transition " + cTrans[path_tran]);
                }
              }
              if (transitionIndex == -1) {
                if (DO_PRINT) System.out.printf("WARNING: Trace transition %s not available from current state %s\n", cTrans[path_tran], sim.getCurrentState());
                cyclesAdded--;
                break; // basically just skip this path
              }
              // Take the transition
              sim.manualTransition(transitionIndex);
              
              // update the total outgoing rate of the current state
              currentState.totalOutgoingRate = totalOutgoingRate; //TODO
              
              // Check if the state exists yet
              int indexOfFoundState = stateIsUnique(getIntVarVals(sim.getCurrentState().varValues));
              // System.out.println("indexOfFoundState: " + indexOfFoundState);
  
              // figure out what state to link here (at cycle insertion)
              My_State stateToAdd = null;
              if (indexOfFoundState == -1) {
                stateToAdd = new My_State(sim.getCurrentState().varValues);
                // need to get total outgoing rates for the newly discovered states!
                totalOutgoingRate = 0.0f;
                // Compare our transition string with available transition strings
                for (int sim_tran = 0; sim_tran < sim.getNumTransitions(); sim_tran++) {
                  // Update transitionIndex if we found the desired transition (i.e. names match)
                  totalOutgoingRate += sim.getTransitionProbability(sim_tran);
                }
                stateToAdd.totalOutgoingRate = totalOutgoingRate;
                stateToAdd.isCycleState = true;
                if (DO_PRINT) System.out.println("State outgoing rate changed to " + totalOutgoingRate + ". State is " + stateToAdd.prismSTA());
                // stateToAdd.addedWithCycle = true;
                newCycleStates++;
                mm.registerMemoryUsage();
                if (DO_PRINT) System.out.printf("Added previously undiscovered state %s", stateToAdd.prismSTA());
              }
              else {
                stateToAdd = stateList.get(indexOfFoundState);
                // make sure we haven't already made this transition
                if (currentState.nextStates.contains(stateToAdd)) {
                  // System.out.println("NEXT STATE ALREADY FOUND");
                  currentState = stateToAdd;
                  continue;
                }
              }
  
              // add the transition to the discovered state
              currentState.nextStates.add(stateToAdd);
              Transition newTrans = new Transition(transitionIndex, cTrans[path_tran], currentState.index, stateToAdd.index, newTranRate);
              if (newTrans.from == newTrans.to) {
                System.out.println("1 newTrans.to == newTrans.from == " + newTrans.to);
              }
              mm.registerMemoryUsage();
              currentState.outgoingTrans.add(newTrans);
              
              newCycleTrans++;

              // if we have reached the target 
              if (target.evaluateBoolean(sim.getCurrentState())) {
                if (DO_PRINT) System.out.println("Target Reached");
                stateToAdd.isTarget = true;
              }
        
              // walk along the cycle
              currentState = stateToAdd;
            }
          }
        }
      }
    }
    // Catch exceptions and give user the info
    catch (PrismException e) {
			if (DO_PRINT) System.out.println("PrismException Error: " + e.getMessage());
			System.exit(1);
		}
  }


  // add cycles master function
  // relies heavily on https://github.com/prismmodelchecker/prism/blob/master/prism/src/parser/State.java
  // and also https://github.com/prismmodelchecker/prism/blob/master/prism/src/simulator/SimulatorEngine.java
  public void addCycles(Prism prism) {

    cyclesAdded = 0;
    newCycleStates = 0;
    newCycleTrans = 0;
    
    if (CYCLE_LENGTH <= 1) return; // no sense finding 0-cycles and 1-cycles

    System.out.println("State graph built. Appending cycles where allowable.");

    // TODO: Check if we repeat ourselves, maybe by checking for previous cycles within cycles

    try {

      if (DO_PRINT) System.out.println("\n----------------------------------");
      if (DO_PRINT) System.out.println("Begin Cycles");
      if (DO_PRINT) System.out.println("----------------------------------\n");
      
      // set a zero state
      State zeroState = new State(numStateVariables);
      mm.registerMemoryUsage();

      if (DO_PRINT) System.out.println("Setting all state values to " + CYCLE_INIT);
    
      for (int i = 0; i < numStateVariables; i++) {
        zeroState.setValue(i, CYCLE_INIT); // lets us have stoichiometry of 2 throughout cycle
      }
      SimulatorEngine sim = prism.getSimulator();
      sim.createNewPath();
      sim.initialisePath(zeroState);
      
      if (DO_PRINT) System.out.println("Zero State: " + zeroState);
      if (DO_PRINT) System.out.println("Zero State: " + sim.getCurrentState());
  
      // Get list of reactions and store in reactionList

      ArrayList<String> reactionList = new ArrayList<String>();
      mm.registerMemoryUsage();

      String r;
      for (int sim_tran = 0; sim_tran < sim.getNumTransitions(); sim_tran++) {
        r = sim.getTransitionActionString(sim_tran);
        reactionList.add(r);
      }
    
      if (DO_PRINT) System.out.printf("Reaction List: ");
      if (DO_PRINT) System.out.println(reactionList);

      // Check every combination of reactions for zero-sums
      cycleArray = new ArrayList<Cycle>();
      int[] minVals = new int[numStateVariables];
      for (int i = 0; i < numStateVariables; i++) {
        minVals[i] = 0;
      }
      mm.registerMemoryUsage();

      findCyclePermutations(sim, zeroState, reactionList, 0, "", minVals);
      // public String findCyclePermutations(SimulatorEngine sim, ArrayList<String> reactionList, int cycleDepth, String cycle, int[] minVals)

      if (DO_PRINT) System.out.println("Successfully detected " + cycleArray.size() + " cycles.");
      // System.out.println(cycleArray);

      // Tack cycles onto the state space somehow...

      // For each state in the state space:
        // If all state values are above the min amounts for each cycle:
          // Simulate the cycle from the state, adding discovered states along the way
      
      simulateCycles(sim);
        

      if (DO_PRINT) System.out.println(" ");
      if (DO_PRINT) System.out.println("\n----------------------------------");
      if (DO_PRINT) System.out.println("End Cycles");
      if (DO_PRINT) System.out.println("----------------------------------\n");
    }
    // Catch exceptions and give user the info
    catch (PrismException e) {
			if (DO_PRINT) System.out.println("PrismException Error: " + e.getMessage());
			System.exit(1);
		}

    System.out.println("Added " + cyclesAdded + " cycles, with " + newCycleStates + " new states and " + newCycleTrans + " transitions.");

  }

  // object to store state variables in the tree structure
  public class StateVarNode {
    public int value;
    // public int stateIndex; // index of the first discovered state to have this value
    public ArrayList<Integer> stateIndices;
    public ArrayList<StateVarNode> children;
    public StateVarNode parent;
    public StateVarNode() {
      this.stateIndices = new ArrayList<Integer>();
      this.value = -1;
      this.children = new ArrayList<StateVarNode>();
      this.parent = null;
    }
    public StateVarNode(int value) {
      this.value = value;
      this.stateIndices = new ArrayList<Integer>();
      // this.stateIndices.add((Integer) stateCount-1); // was causing problems due to this line being duplicated
      // System.out.println("New StateVarNode for My_State " + (stateCount-1));
      this.children = new ArrayList<StateVarNode>();
    }
    // public void addChild(int value) {
    //   this.children.add(new StateVarNode(value));
    //   this.children.get(this.children.size()-1).parent = this;
    //   System.out.println("New parent " + this.children.get(this.children.size()-1).parent.value + " for " + this.children.get(this.children.size()-1).value );
    //   // maybe sort the list as well eventually?
    // }
  }

  // global state variable root is just an empty node
  public StateVarNode StateVarRoot = new StateVarNode();

  public int stateIsUnique(int varVals[]) {
    return stateIsUnique(varVals, false);
  }

  // function to check uniqueness and update unique state tree
  public int stateIsUnique(int varVals[], boolean printUnique) {
    // System.out.printf("stateIsUnique? (");
    // for (int i = 0; i < varVals.length; i++) {
    //   System.out.printf("%d", varVals[i]);
    //   if (i == varVals.length-1) System.out.printf(")\n");
    //   else System.out.printf(",");
    // }
 
    // loop through all the state variables to see if they exist
    ArrayList<Integer> possibleStates = new ArrayList<Integer>();
    StateVarNode cur = StateVarRoot;
    boolean foundStateVar = false;
    int foundStateIndex = -1;

    if (DO_PRINT) System.out.println("Check uniqueness of " + stateCount);

    for (int stateVar = 0; stateVar < numStateVariables; stateVar++) {
      
      foundStateVar = false;
      for (int i = 0; i < cur.children.size(); i++) { // new states have 0 kids anyway
        if (cur.children.get(i).value == varVals[stateVar]) {
          if (DO_PRINT) {
            if (printUnique && DO_PRINT) System.out.printf("\nM(");
            for (int aa = 0; aa < cur.children.get(i).stateIndices.size(); aa++) {
              if (printUnique && DO_PRINT) System.out.printf("%s.", cur.children.get(i).stateIndices.get(aa));
            }
            if (printUnique && DO_PRINT) System.out.printf(") %d and %d", varVals[stateVar], cur.children.get(i).value);
          }
          cur = cur.children.get(i);
          foundStateVar = true;
          // add all the possibilities as an intersection

          if (stateVar == 0) {
            for (int a = 0; a < cur.stateIndices.size(); a++) {
              possibleStates.add(cur.stateIndices.get(a));
            }
          }
          for (int a = 0; a < possibleStates.size(); a++) {
            // System.out.println("Checking possible state " + possibleStates.get(a));
            if (!(cur.stateIndices.contains(possibleStates.get(a)))) {
              // System.out.println("Removed possible state " + possibleStates.get(a));
              possibleStates.remove(a);
              a--;
            }
          }
          // foundStateIndex = cur.stateIndex; // TODO: This line breaks things
          break;
        }
      }
      if (!foundStateVar) { // if we didn't find it (i.e. state doesn't exist)
        if (DO_PRINT) {
          if (printUnique && DO_PRINT) System.out.printf("X() %d\n", varVals[stateVar]);
        }
        cur.children.add(new StateVarNode(varVals[stateVar]));
        cur.children.get(cur.children.size()-1).parent = cur;
        cur = cur.children.get(cur.children.size()-1);
        // System.out.println("\nAdded parental relationship " + cur.value + " child of " + cur.parent.value);
        foundStateIndex = -1;
        if (printUnique && DO_PRINT) System.out.println("AAAA");
        for (int aaa = 0; aaa < possibleStates.size(); aaa++) {
          if (printUnique && DO_PRINT) System.out.println(possibleStates.get(aaa));
        }
        for (int j = 0; j < possibleStates.size(); ) {
          possibleStates.remove(j);
        }
      }
      if (printUnique && DO_PRINT) System.out.println("BBBB");
      for (int aaa = 0; aaa < possibleStates.size(); aaa++) {
        if (printUnique && DO_PRINT) System.out.println(possibleStates.get(aaa));
      }
    }
    if (printUnique && DO_PRINT) System.out.println("CCCC");
    for (int aaa = 0; aaa < possibleStates.size(); aaa++) {
      if (printUnique && DO_PRINT) System.out.println(possibleStates.get(aaa));
    }
    if (printUnique && DO_PRINT) System.out.println("foundStateVar? " + foundStateVar);

    if (possibleStates.size() == 0 || !foundStateVar) {
      foundStateIndex = -1;
      // if (DO_PRINT) System.out.println(" State not found. Size is 0.");
      while (cur != null) {
        if (!cur.stateIndices.contains(stateCount)) {
          cur.stateIndices.add(stateCount);
          // if (DO_PRINT) System.out.println("added state " + stateCount + " to state var " + cur.value);
        }
        cur = cur.parent;
      }
    }
    else if (possibleStates.size() == 1) {
      foundStateIndex = possibleStates.get(0).intValue();
      if (DO_PRINT) System.out.println(" State found. Index is " + foundStateIndex);
      if (DO_PRINT) System.out.printf("State %s", stateList.get(foundStateIndex).prismSTA());
    }
    else {
      foundStateIndex = -1;
      System.out.println("\n ERROR ");
      System.out.println(" ERROR ");
      System.out.println(" Multiple states found. Size is " + possibleStates.size());
      for (int aaa = 0; aaa < possibleStates.size(); aaa++) {
        System.out.println(possibleStates.get(aaa));
      }
      System.out.println(" ERROR ");
      System.out.println(" ERROR ");
    }

    return foundStateIndex;
  }

  // save the number of state variables
  public int setNumStateVariables(Prism prism) {
    try {
      // Create a new simulation from the initial state
      SimulatorEngine sim = prism.getSimulator();
      sim.createNewPath();
      sim.initialisePath(null);
      Object varVals[] = sim.getCurrentState().varValues;
      numStateVariables = varVals.length;
      return varVals.length;
    }
    catch (PrismException e) {
			if (DO_PRINT) System.out.println("PrismException Error: " + e.getMessage());
			System.exit(1);
		}
    return -1;
  }

  public int deepestDepth;

  public void buildAndCommute(Prism prism, String[] transitions, String[] prefix, int depth, Path parentPath, Expression target)
  {
    try {

      boolean terminatedByMaxDepth = false;
      mm.registerMemoryUsage();

      if (depth == 0) deepestDepth = 0;
      else if (deepestDepth < depth) deepestDepth = depth;
      
      if (TERMINATE_DEPTH && depth > MAX_DEPTH) {
        if (DO_PRINT) System.out.println("Max recursion depth reached.");
        terminatedByMaxDepth = true;
        return;
      }
      
      int indexOfFoundState = -1;
      if (DO_PRINT) {
        if (prefix != null) {
          System.out.printf("\n%2d Prefix ", depth);
          for (int i = 0; i < prefix.length; i++) {
            System.out.printf("%s ", prefix[i]);
          }
          System.out.println(".");
        }
        else {
          System.out.println("\n 0 Prefix none");
        }
      }
      
      
      // Create a new simulation from the initial state
      SimulatorEngine sim = prism.getSimulator();
      sim.createNewPath();
      sim.initialisePath(null);
      
      if (DO_PRINT) System.out.println("Prism simulator initialized successfully.");
      if (DO_PRINT) System.out.println("Initial state: " + sim.getCurrentState());
      
      // temporary found transition index variable
      int transitionIndex;
      
      // initialize to the initial state each time
      int currentStateIndex = 0;
      
      // check if we've created an initial state
      if (stateList.size() == 0) {
        if (DO_PRINT) System.out.println("Initial state generated.");
        new My_State(sim.getCurrentState().varValues);
        // we know initial state is unique but we need to log its values with stateIsUnique.
        stateCount--;
        stateIsUnique(getIntVarVals(sim.getCurrentState().varValues));
        stateCount++;
        // System.out.println("ROOT " + printUniqueString());
      }
      
      // Save these states into a path
      Path seedPath = new Path();

      // update current state to be the model's initial state
      My_State currentState = stateList.get(0);
      currentState.isNewInit = true;
      
      // Walk along the prefix to get the new initial state
      int prefixLength = 0;
      double prefixTime = 0.0f;
      if (prefix != null) prefixLength = prefix.length;
      if (DO_PRINT) System.out.println("Prefix Length is " + prefixLength);
      for (int path_tran = 0; path_tran < prefixLength; path_tran++) {

        if (DO_PRINT) System.out.printf("Intermediate Prefix State is %s", currentState.prismSTA());

        // start with a fresh transitionIndex
        transitionIndex = -1;
        double newTranRate = -1.0f;
        double totalOutgoingRate = 0.0f;
        // Compare our transition string with available transition strings
        for (int sim_tran = 0; sim_tran < sim.getNumTransitions(); sim_tran++) {
          // Update transitionIndex if we found the desired transition (i.e. names match)
          totalOutgoingRate += sim.getTransitionProbability(sim_tran);
          if (prefix[path_tran].equalsIgnoreCase(sim.getTransitionActionString(sim_tran))) {
            transitionIndex = sim_tran;
            newTranRate = sim.getTransitionProbability(sim_tran);
            if (DO_PRINT) System.out.println("Transition " + sim.getTransitionActionString(sim_tran));
          }
        }
        // If we never found the correct transitions, report error
        if (transitionIndex == -1) {
          System.out.printf("ERROR: Prefix transition not available from current state: ");
          System.out.println(sim.getCurrentState());
          System.exit(10001);
        }

        if (DO_PRINT) System.out.println("Prism state before firing: " + sim.getCurrentState());
        // Take the transition
        sim.manualTransition(transitionIndex);
        if (DO_PRINT) System.out.println("Prism state after firing: " + sim.getCurrentState());

        // update the total outgoing rate of the current state
        currentState.totalOutgoingRate = totalOutgoingRate;

        // Update the prefix time duration and report
        if (TERMINATE_TIME) prefixTime += currentState.getMRT();
        
        // Check if the state exists yet
        indexOfFoundState = stateIsUnique(getIntVarVals(sim.getCurrentState().varValues), false);
        
        // figure out what state to link here
        My_State stateToAdd = null;
        if (indexOfFoundState == -1) {
          stateToAdd = new My_State(sim.getCurrentState().varValues);
        }
        else {
          stateToAdd = stateList.get(indexOfFoundState);
          // make sure we haven't already made this transition
          if (currentState.nextStates.contains(stateToAdd)) {
            seedPath.states.add(currentState);
            currentState = stateToAdd;
            continue;
          }
        }
        // add the transition to the discovered state
        currentState.nextStates.add(stateToAdd);
        Transition newTrans = new Transition(transitionIndex, prefix[path_tran], currentState.index, stateToAdd.index, newTranRate);
        if (newTrans.from == newTrans.to) {
          System.out.println("2 newTrans.to == newTrans.from == " + newTrans.to);
        }
        currentState.outgoingTrans.add(newTrans);

        // save the current state into the path
        seedPath.states.add(currentState);
        
        // walk along the trace
        currentState = stateToAdd;
        
      } // end walk along path prefix
      
      
      if (DO_PRINT) System.out.println("Successfully walked along trace prefix (set of commuted transitions)");
      
      if (DO_PRINT) System.out.printf("Prefix Terminal State is %s", currentState.prismSTA());
      if (DO_PRINT) System.out.printf("At sim state %s\n", sim.getCurrentState());
            
      // if we already end up in a known state after the prefix, end it here
      if (indexOfFoundState != -1 && currentState.isNewInit) {
        // if (DO_PRINT) System.out.println("Prefix Terminal State Exists. End Recursion Branch.\n");
        if (DO_PRINT) System.out.printf("Prefix Terminal State Exists at State %d. End Recursion Branch.\n", indexOfFoundState);
        // skip walking along the trace and jump right to terminal state
        // currentState = stateList.get(currentState.index + transitions.length);
        // System.out.printf("Jumped to state %s", currentState.prismSTA());
        return;
      }
      
      if (DO_PRINT && TERMINATE_TIME) System.out.println("Prefix time is " + prefixTime);

      // Mark we have a new "path initial state", as it were
      currentState.isNewInit = true;


      // Set up lists to check commutable transitions
      ArrayList<String> wasEnabled = new ArrayList<String>();
      
      // Add initially enabled transitions to start the commutable check
      for (int sim_tran = 0; sim_tran < sim.getNumTransitions(); sim_tran++) {
        seedPath.commutable.add(sim.getTransitionActionString(sim_tran));
      }

      // start saving the path time
      double pathTime = 0.0f;

      // Walk along the actual trace, making new states
      // and getting commutable transitions
      for (int path_tran = 0; path_tran < transitions.length; path_tran++) {

        // start with a fresh transitionIndex
        transitionIndex = -1;
        double newTranRate = -1.0f;
        double totalOutgoingRate = 0.0f;
        if (DO_PRINT) System.out.println(sim.getCurrentState());
        // Compare our transition string with available transition strings
        for (int sim_tran = 0; sim_tran < sim.getNumTransitions(); sim_tran++) {
          // Update transitionIndex if we found the desired transition (i.e. names match)
          totalOutgoingRate += sim.getTransitionProbability(sim_tran);
          // if (DO_PRINT) System.out.println("Enabled transition " + sim.getTransitionActionString(sim_tran));
          if (transitions[path_tran].equalsIgnoreCase(sim.getTransitionActionString(sim_tran))) {
            transitionIndex = sim_tran;
            newTranRate = sim.getTransitionProbability(sim_tran);
            if (DO_PRINT) System.out.println("Found transition " + transitions[path_tran]);
          }
        }
        // If we never found the correct transitions, report error
        if (transitionIndex == -1) {
          if (depth > 0) {
            if (DO_PRINT) System.out.printf("WARNING: Trace transition %s not available from current state %s\n", transitions[path_tran], sim.getCurrentState());
            return; // basically just skip this path
          }
          else { // if we're still at the base seed path
          System.out.printf("ERROR: Initial trace transition %s not available from current state %s\n", transitions[path_tran], sim.getCurrentState());
            System.exit(10002);
          }
        }
        // Take the transition
        sim.manualTransition(transitionIndex);
        
        // update the total outgoing rate of the current state
        currentState.totalOutgoingRate = totalOutgoingRate;

        // update the path time and report
        if (TERMINATE_TIME) pathTime += currentState.getMRT();
        
        
        // Check if the state exists yet
        indexOfFoundState = stateIsUnique(getIntVarVals(sim.getCurrentState().varValues));
        if (DO_PRINT && indexOfFoundState == -1) {
          // System.out.println("Path state is unique.");
        }

        // TODO: Since we're trying to commute, if this doesn't work, return
        
        // figure out what state to link here
        My_State stateToAdd = null;
        if (indexOfFoundState == -1) {
          stateToAdd = new My_State(sim.getCurrentState().varValues);
        }
        else {
          stateToAdd = stateList.get(indexOfFoundState);
          // make sure we haven't already made this transition
          if (currentState.nextStates.contains(stateToAdd)) {
            // System.out.println("NEXT STATE ALREADY FOUND");
            seedPath.states.add(currentState);
            currentState = stateToAdd;
            continue;
          }
        }
        // add the transition to the discovered state
        currentState.nextStates.add(stateToAdd);
        Transition newTrans = new Transition(transitionIndex, transitions[path_tran], currentState.index, stateToAdd.index, newTranRate);
        if (newTrans.from == newTrans.to) {
          System.out.println("3 newTrans.to == newTrans.from == " + newTrans.to);
        }
        currentState.outgoingTrans.add(newTrans);
        
        // save the current state into the path
        seedPath.states.add(currentState);
        
        // if we have reached the target 
        if (target.evaluateBoolean(sim.getCurrentState())) {
          if (DO_PRINT) System.out.println("Target Reached");
          stateToAdd.isTarget = true;
          seedPath.states.add(stateToAdd);
        }
        
        // walk along the trace
        currentState = stateToAdd;
        
        // update commutable transitions based on new state
        wasEnabled = seedPath.commutable;
        seedPath.commutable = new ArrayList<String>();
        for (int sim_tran = 0; sim_tran < sim.getNumTransitions(); sim_tran++) {
          String tempStr = sim.getTransitionActionString(sim_tran);
          if (wasEnabled.contains(tempStr)) {
            seedPath.commutable.add(tempStr);
          }
        }
        
      } // end walk along actual trace

      if (DO_PRINT && TERMINATE_TIME) System.out.println("Path time is " + pathTime);
      if (DO_PRINT && TERMINATE_TIME) System.out.printf("Mean Time for Prefix %2.6f \tPath %2.6f\tTotal %2.6f\n", prefixTime, pathTime, (prefixTime + pathTime));

      // end recursion based on mean residence time
      // if (TERMINATE_TIME && ((prefixTime + pathTime) > TIME_BOUND)) {
      //   System.out.println("Reached time bound recursion threshold at depth " + depth);
      //   // continue;
      // }

      if (DO_PRINT)  {
        System.out.println("One path explored: ");
        // for (int i = 0; i < seedPath.states.size(); i++) {
        //   System.out.printf("%d ", seedPath.states.get(i).index);
        // }
        // System.out.println(".");
      }
      // NOTE:
      // commutable transitions are now stored in seedPath.commutable.
      
      //
      //
      // COMMUTING TRANSITIONS FIRST ALONG THE CURRENT SAVED SEED PATH
      //
      //
      
      // sim.backtrackTo(seedPath.states.size()-0);
      // System.out.println("Backtrack seedPath.states.size()-0: " + sim.getCurrentState());
      // sim.backtrackTo(seedPath.states.size()-1);
      // System.out.println("Backtrack seedPath.states.size()-1: " + sim.getCurrentState());
      // sim.backtrackTo(seedPath.states.size()-2);
      // System.out.println("Backtrack seedPath.states.size()-2: " + sim.getCurrentState());
      // sim.backtrackTo(seedPath.states.size()-3);
      // System.out.println("Backtrack seedPath.states.size()-3: " + sim.getCurrentState());
      // sim.backtrackTo(seedPath.states.size()-4);


      if (DO_PRINT) System.out.printf("\n\nEntered commuting phase with %d transitions\n\n\n", seedPath.commutable.size());
      if (DO_PRINT) {
        for (int mm = 0; mm < seedPath.commutable.size(); mm++) {
          System.out.printf(" %s", seedPath.commutable.get(mm));
        }
        System.out.println(".\n");
      }

      // for each state in the seed path, backtracking toward the initial state + prefix
      for (int seedIndex = seedPath.states.size()-1; seedIndex >= 0; seedIndex--) {

        
        // for each commutable transition
        for (int ctran = 0; ctran < seedPath.commutable.size(); ctran++) {
          
          // set our current state to the seedpath state
          currentState = seedPath.states.get(seedIndex);
          if (DO_PRINT) System.out.printf("backtrackTo %d\n", seedIndex);
          // if (DO_PRINT) for (int aa = 0; aa < seedPath.states.size(); aa++) {
          //     System.out.printf(aa + " -- " + seedPath.states.get(aa).prismSTA());
          // }
          if (DO_PRINT) System.out.printf("seedPath.states.size() = %d\n", seedPath.states.size());
          sim.backtrackTo(seedIndex);
          if (DO_PRINT) System.out.printf("backtracked to %s\n", sim.getCurrentState());
          if (DO_PRINT) System.out.println("attempting to commute " + seedPath.commutable.get(ctran));
          if (DO_PRINT) System.out.printf("currentState %s", currentState.prismSTA());
          
          // fire the transition
          transitionIndex = -1;
          double newTranRate = -1.0f;
          double totalOutgoingRate = 0.0f;
          // Compare our transition string with available transition strings
          for (int sim_tran = 0; sim_tran < sim.getNumTransitions(); sim_tran++) {
            // Update transitionIndex if we found the desired transition (i.e. names match)
            totalOutgoingRate += sim.getTransitionProbability(sim_tran);
            if (seedPath.commutable.get(ctran).equalsIgnoreCase(sim.getTransitionActionString(sim_tran))) {
              transitionIndex = sim_tran;
              newTranRate = sim.getTransitionProbability(sim_tran);
            }
          }
          
          // If we never found the correct transitions, report error
          if (transitionIndex == -1) {
            if (DO_PRINT) System.out.printf("WARNING: Commutable transition %s not available from current state %s\n", seedPath.commutable.get(ctran), sim.getCurrentState());
            continue;
          }
          
          // Take the transition
          boolean test1 = false;
          if (DO_PRINT && sim.getTransitionActionString(transitionIndex).equalsIgnoreCase("[R1]")) {
            test1 = true;
            if (DO_PRINT) System.out.println("before: " + sim.getCurrentState());
          }

          sim.manualTransition(transitionIndex);


          // if (DO_PRINT) System.out.println("sim: " + sim.getCurrentState());
          if (DO_PRINT && test1) System.out.println("after: " + sim.getCurrentState());
          
          // update the total outgoing rate of the current state
          currentState.totalOutgoingRate = totalOutgoingRate;
          
          // Check if the state exists yet
          indexOfFoundState = stateIsUnique(getIntVarVals(sim.getCurrentState().varValues));
          if (DO_PRINT && test1 && indexOfFoundState > -1) System.out.printf("Index of found state: %d -- %s", indexOfFoundState, stateList.get(indexOfFoundState).prismSTA());

          if (DO_PRINT) System.out.printf("Sim state after commuting: %s\n", sim.getCurrentState());
          if (DO_PRINT) System.out.printf("Index of found state: %d\n", indexOfFoundState);
          
          // figure out what state to link here
          My_State stateToAdd = null;
          if (indexOfFoundState == -1) {
            stateToAdd = new My_State(sim.getCurrentState().varValues);
            // need to get total outgoing rates for the newly discovered states!
            totalOutgoingRate = 0.0f;
            // Compare our transition string with available transition strings
            for (int sim_tran = 0; sim_tran < sim.getNumTransitions(); sim_tran++) {
              // Update transitionIndex if we found the desired transition (i.e. names match)
              totalOutgoingRate += sim.getTransitionProbability(sim_tran);
            }
            stateToAdd.totalOutgoingRate = totalOutgoingRate;
            if (DO_PRINT) System.out.println("State outgoing rate changed to " + totalOutgoingRate + ". State is " + stateToAdd.prismSTA());
          }
          else {
            stateToAdd = stateList.get(indexOfFoundState);
            // make sure we haven't already made this transition
            if (currentState.nextStates.contains(stateToAdd)) {
              currentState = stateToAdd;
              continue;
            }
          }

          // add the transition to the discovered state
          currentState.nextStates.add(stateToAdd);

          Transition newTrans = new Transition(transitionIndex, seedPath.commutable.get(ctran), currentState.index, stateToAdd.index, newTranRate);
          if (newTrans.from == newTrans.to) {
            System.out.println("4 newTrans.to == newTrans.from == " + newTrans.to + newTrans.name);
            // if (DO_PRINT) for (int aa = 0; aa < stateList.size(); aa++) {
            //   System.out.printf(aa + " -- " + stateList.get(aa).prismSTA());
            // }
          }
          currentState.outgoingTrans.add(newTrans);

          if (DO_PRINT) System.out.printf("Added transition %s\n", newTrans.prismTRA() );

      }
    }

    sim = null; // delete the simulator here to free up memory, maybe save time

      
      // get the length of the prefix
      int prefixLen;
      if (prefix == null) {
        prefixLen = 0;
      }
      else {
        prefixLen = prefix.length;
      }

      // commute if we're within depth bounds
      if ((TERMINATE_DEPTH && depth < MAX_DEPTH) || (TERMINATE_TIME && ((pathTime + prefixTime) < TIME_BOUND))) {
        // for each commutable transition
        for (int ctran = 0; ctran < seedPath.commutable.size(); ctran++) {

          // append the commutable transition to the existing prefix
          String[] newPrefix = new String[prefixLen + 1];
          for (int i = 0; i < prefixLen; i++) {
            newPrefix[i] = prefix[i];
          }
          newPrefix[prefixLen] = seedPath.commutable.get(ctran);
  
          if (DO_PRINT) System.out.println("Recursing into next prefix: " + seedPath.commutable.get(ctran) + " with depth " + (depth+1));
  
          // build a new path and commute
          buildAndCommute(prism, transitions, newPrefix, depth+1, seedPath, target);
        }
      }
      if (DO_PRINT) System.out.println("A path has been successfully commuted");
      if (TERMINATE_TIME && terminatedByMaxDepth && depth == 0) {
        System.out.println("Terminated by maximum recursion depth.");
      }
      if (TERMINATE_TIME && !terminatedByMaxDepth && depth == 0) {
        System.out.println("Terminated by time bound * flexibility only with max depth " + deepestDepth + ".");
      }
    }
    catch (PrismException e) {
      if (DO_PRINT) System.out.println("PrismException Error: " + e.getMessage());
      System.exit(1);
    }
  }

  public int setAbsorbingState() {
    // set absorbing state to all -1
    int[] tempAbsorb = new int[numStateVariables];
    int absIndex = stateCount;
    for (int i = 0; i < numStateVariables; i++) {
      tempAbsorb[i] = -1;
    }
    My_State absorbingState = new My_State(tempAbsorb);
    double stateAbsorbRate;
    for (int i = 0; i < stateList.size(); i++) {
      stateAbsorbRate = stateList.get(i).getAbsorbingRate();
      if (stateAbsorbRate > 0) {
        stateList.get(i).outgoingTrans.add(new Transition(-1, "ABS", i, absIndex, stateAbsorbRate));
      }
    }
    return absIndex;
  }

  // Top-level function, runs algorithm
  public void run() 
  {
    try
    {

      System.out.println("Welcome to the model commutation tool.");
	  getParams();
      // start by resetting the state count
      stateCount = 0;
      
      // give PRISM output a log file
      PrismLog mainLog = new PrismDevNullLog();
      
      // initialise (with an s) PRISM engine
      Prism prism = new Prism(mainLog);
      prism.initialise();
      
      if (DO_PRINT) System.out.println("Prism initialized successfully.");
      // Parse the prism model
      // For now, MODEL_NAME is the hard-coded model file name
      ModulesFile modulesFile = prism.parseModelFile(new File(MODEL_NAME));
      if (DO_PRINT) System.out.println("Prism model parsed successfully.");
      
      // Load the prism model for checking
      prism.loadPRISMModel(modulesFile);
      if (DO_PRINT) System.out.println("Prism model loaded successfully.");
      
      // Load the model into the simulator engine
      prism.loadModelIntoSimulator();
      if (DO_PRINT) System.out.println("Prism model loaded into simulator successfully.");
      
      // Load the target property
      target = prism.parsePropertiesString(PROPERTY).getProperty(0);
      // Expression target = prism.parsePropertiesString(targetString).getProperty(0);
      
      System.out.println("Prism model and property loaded succesfully.");
      // set the number of state variables for the model
      setNumStateVariables(prism);
      if (DO_PRINT) System.out.printf("Number of state variables: %d\n", numStateVariables);

      // Read in the first line of the trace as a string
      // For now, the trace must go in forprism.trace (handled in python script)
			FileReader fr = new FileReader(TRACE_LIST_NAME);
			BufferedReader br = new BufferedReader(fr);
      String trace;
      String[] transitions;
      int num_transitions;

      int numPaths = 0;

      System.out.println("Iterating through each input trace.");
      System.out.println("Expect this to run silently for a moment.");

      // For each input trace
      while (true) {

        
        // read in a single seed trace
        trace = br.readLine();
        if (trace == null) break;
        
        numPaths++;

        if (numPaths % 25 == 0) {
          System.out.printf("Processed %d paths with a state count of %d\n", numPaths, stateCount);
        }
        
        // TODO: POSSIBLY TERMINATE BASED ON DELTA-STATECOUNT?
        // i.e. if stateCount hasn't changed in many paths, terminate
        
        // break the trace into an array of individual transitions
        transitions = trace.split("\\s+");
        for (int i = 0; i < transitions.length; i++) {
          transitions[i] = String.format("[%s]", transitions[i]);
        }
        num_transitions = transitions.length;
        
        if (DO_PRINT) System.out.println("Read trace with " + num_transitions + " transitions.");
        
        // construct states with transitions along the trace
        // then commute along the generated path
        buildAndCommute(prism, transitions, null, 0, null, target);

      }

      System.out.printf("Processed %d paths with a state count of %d\n", numPaths, stateCount);

      addCycles(prism);
      
      removeDeadEnds();      

      System.out.printf("\nFinal Count:\n%d states\n%d transitions\n\n", stateCount, transitionCount);

      System.out.println("Establishing an absorbing state.");
      int absorbIndex = setAbsorbingState();

      System.out.println("Begin printing model files.");

      // Initialize label string
      // TODO: Play with deadlock vs sink
      String labStr = "0=\"init\" 1=\"sink\"\n0: 0\n";
      if (EXPORT_PRISM) labStr += (absorbIndex + ": 1");
      
      String labStrS = "#DECLARATION\ninit absorb target\n#END\n0 init\n";

      // Initialize transition string
      String traStr = "";
      String traStrS = "ctmc\n";
      if (EXPORT_PRISM) traStr += ((stateList.size()) + " " + transitionCount + "\n");

      // Initialize state string
      String staStr = "";
      if (EXPORT_PRISM) staStr += "(";

      // Create a new simulation from the initial state, to get the var names
      SimulatorEngine sim = prism.getSimulator();
      sim.createNewPath();
      sim.initialisePath(null);
      
      if (EXPORT_PRISM) {
        // Get variable names for first line of .sta file
        int vari = 0;
        String varName = "";
        while (true) {
          varName = sim.getVariableName(vari);
          staStr += varName;
          if (sim.getVariableName(vari+1) == null) {
            staStr += ")\n";
            break;
          }
          staStr += ",";
          vari++;
        }
      }

      for (int a = 0; a < stateList.size(); a++) {
        if (EXPORT_PRISM) staStr += stateList.get(a).prismSTA();
        if (EXPORT_PRISM) traStr += stateList.get(a).prismTRA();
        if (EXPORT_STORM) traStrS += stateList.get(a).prismTRA();
        if (EXPORT_STORM && stateList.get(a).isTarget) {
          labStrS += (stateList.get(a).index + " target\n");
        }
      }

      if (EXPORT_STORM) traStrS += (absorbIndex + " " + absorbIndex + " 0.0");
      if (EXPORT_STORM) labStrS += (absorbIndex + " absorb");

      if (EXPORT_PRISM) {
        System.out.println("Now printing " + numPaths + " paths to PRISM model files.");
        // Write the state file to prism.sta
        BufferedWriter staWriter = new BufferedWriter(new FileWriter("prism.sta"));
        staWriter.write(staStr.trim());
        staWriter.close();
        
        // Write the transition file to prism.tra
        BufferedWriter traWriter = new BufferedWriter(new FileWriter("prism.tra"));
        traWriter.write(traStr.trim());
        traWriter.close();
        
        // Write the label file to prism.lab
        BufferedWriter labWriter = new BufferedWriter(new FileWriter("prism.lab"));
        labWriter.write(labStr.trim());
        labWriter.close();
        
      }

      if (EXPORT_STORM) {
        System.out.println("Now printing " + numPaths + " paths to STORM model files.");
        // Write the transition file to storm.tra
        BufferedWriter traWriterS = new BufferedWriter(new FileWriter("storm.tra"));
        traWriterS.write(traStrS.trim());
        traWriterS.close();
  
        // Write the label file to storm.lab
        BufferedWriter labWriterS = new BufferedWriter(new FileWriter("storm.lab"));
        labWriterS.write(labStrS.trim());
        labWriterS.close();
      }

      State zeroState = new State(numStateVariables);
      sim.createNewPath();
      double prismTotalOutgoing = 0.0f;

      System.out.println("---------------------------------------");
      System.out.println("BEGIN ERROR CHECKING");
      System.out.println("If an inconsistency is found in the state space, its details will be provided here.");
      System.out.println("Otherwise, assume the generated state-space is error-free.");
      System.out.println("This is an added confidence booster for the probability results.");
      
      for (int i = 0; i < stateCount; i++) {
        for (int k = 0; k < numStateVariables; k++) {
          zeroState.setValue(k, stateList.get(i).stateVars[k]); // lets us have stoichiometry of 2 throughout cycle
        }
        // if (DO_PRINT) System.out.println("Checking state " + i);
        for (int j = 0; j < stateList.get(i).outgoingTrans.size(); j++) {
          if (stateList.get(i).outgoingTrans.get(j).name == "ABS") continue; // skip absorbing state

          sim.initialisePath(zeroState);
          // fire the transition
          int transitionIndex = -1;
          double newTranRate = -1.0f;
          double totalOutgoingRate = 0.0f;
          
          // Compare our transition string with available transition strings
          for (int sim_tran = 0; sim_tran < sim.getNumTransitions(); sim_tran++) {
            // Update transitionIndex if we found the desired transition (i.e. names match)
            totalOutgoingRate += sim.getTransitionProbability(sim_tran);
            if (stateList.get(i).outgoingTrans.get(j).name.equalsIgnoreCase(sim.getTransitionActionString(sim_tran))) {
              transitionIndex = sim_tran;
              newTranRate = sim.getTransitionProbability(sim_tran);
            }
          }
          
          // If we never found the correct transitions, report error
          if (transitionIndex == -1) {
            System.out.println("\nChecking state " + i);
            System.out.printf("ERROR: Commutable transition %s not available from current state %s\n", stateList.get(i).outgoingTrans.get(j).name, sim.getCurrentState());
          }
          
          if (totalOutgoingRate != stateList.get(i).totalOutgoingRate) {
            System.out.println("\nChecking state " + i);
            System.out.println("ERROR: totalOutgoingRate (" + totalOutgoingRate + ") NOT EQUAL TO stateList.get(" + i + ").totalOutgoingRate (" + stateList.get(i).totalOutgoingRate);
          }
          
          if (newTranRate != stateList.get(i).outgoingTrans.get(j).rate) {
            System.out.println("\nChecking state " + i);
            System.out.println("ERROR: newTranRate (" + newTranRate + ") NOT EQUAL TO stateList.get(" + i + ").outgoingTrans.get(" + j + ").rate (" + stateList.get(i).outgoingTrans.get(j).rate);
          }
          
          sim.manualTransition(transitionIndex);
          
          int[] newStateVarVals = getIntVarVals(sim.getCurrentState().varValues);
          for (int l = 0; l < numStateVariables; l++) {
            if (newStateVarVals[l] != stateList.get(stateList.get(i).outgoingTrans.get(j).to).stateVars[l]) {
              System.out.println("\nChecking state " + i);
              System.out.println("ERROR: newStateVarVals[" + l + "] (" + newStateVarVals[l] + ") NOT EQUAL TO stateList.get(stateList.get(" + i + ").outgoingTrans.get(" + j + ").to).stateVars[" + l + "] (" + stateList.get(stateList.get(i).outgoingTrans.get(j).to).stateVars[l] + ")" );
              System.out.printf("From state " + stateList.get(i).prismSTA());
              System.out.println("Transition " + stateList.get(i).outgoingTrans.get(j).name);
              System.out.printf("Intended for state " + stateList.get(stateList.get(i).outgoingTrans.get(j).to).prismSTA());
              System.out.println("Arrived at state " + sim.getCurrentState());
            }
          }
        }
      }
      
      System.out.println("\nEND ERROR CHECKING");
      System.out.println("---------------------------------------");

      System.out.println("Finished! Processed " + numPaths + " paths.");
      
      // close PRISM
      prism.closeDown();
      
      System.out.println("Max Memory: " + mm.getMaxUsedMemory() + " bytes");
      System.out.println("Max Memory: " + (mm.getMaxUsedMemory() / 1000) + " KB");
      System.out.println("Max Memory: " + (mm.getMaxUsedMemory() / 1000000) + " MB");
      System.out.println("Max Memory: " + (mm.getMaxUsedMemory() / 1000000000) + " GB");

    }
    // Catch exceptions and give user the info
    catch (PrismException e) {
			if (DO_PRINT) System.out.println("PrismException Error: " + e.getMessage());
			System.exit(1);
		}
    catch (IOException e) {
      if (DO_PRINT) System.out.println("IOException Error: " + e.getMessage());
			System.exit(1);
    }

  }

}
