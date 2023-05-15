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

// PRISM things
import prism.Prism;
import prism.PrismDevNullLog;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismPrintStreamLog;

// PRISM Simulator
import simulator.SimulatorEngine;

// This class takes care of everything we need the PRISM API for
public class BuildModel
{

  // User-passed parameter file, see docs/tool/input.md
  public static final String OPTION_FILE = "options.txt";
  
  // Required parameters
  public String MODEL_NAME = "model.sm";
  public String TRACE_LIST_NAME = "forprism.trace";
  public String PROPERTY = "A=100";

  // Optional parameters
  public int MAX_DEPTH = 30;
  public double TIME_BOUND = 200.0;
  public boolean DO_PRINT = false;
  public boolean EXPORT_PRISM = true;
  public boolean EXPORT_STORM = true;
  
  // By default, call BuildModel().run()
  public static void main(String[] args)
  {
    new BuildModel().run();
  }

  // Get options line-by-line
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
		}
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
  public class State {
    public int index;
    public int[] stateVars;
    public double totalOutgoingRate;
    public ArrayList<Transition> outgoingTrans;
    public ArrayList<State> nextStates;
    public boolean isTarget;
    public boolean isNewInit;

    public State(Object varVals[]) {
      this.isTarget = false;
      this.isNewInit = false;
      this.index = stateCount;
      stateCount++;
      this.stateVars = getIntVarVals(varVals);
      this.totalOutgoingRate = 0.0;
      this.outgoingTrans = new ArrayList<Transition>();
      this.nextStates = new ArrayList<State>();
      stateList.add(this);
      if (DO_PRINT) System.out.printf("New state %s", this.prismSTA());
    }
    
    public State(int varVals[]) {
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
      this.nextStates = new ArrayList<State>();
      stateList.add(this);
      System.out.printf("New state %s", this.prismSTA());
    }

    // get outgoing rate not captured by notated transitions
    public double getAbsorbingRate() {
      double absorbRate = this.totalOutgoingRate;
      for (int i = 0; i < this.outgoingTrans.size(); i++) {
        absorbRate -= this.outgoingTrans.get(i).rate;
      }
      return absorbRate;
    }

    // State details for .sta files
    // Format is <index>:(<state_var>,<state_var>...)
    public String prismSTA() {
      String temp = (index) + ":(";
      for (int i=0; i<numStateVariables; i++) {
        if (i>0) temp += ",";
        temp += stateVars[i]; 
      }
      return temp + ")\n";
    }

    // State details for .tra files
    // Format is <index>:(<state_var>,<state_var>...)
    public String prismTRA() {
      String temp = "";
      for (int i=0; i<this.outgoingTrans.size(); i++) {
        temp += this.outgoingTrans.get(i).prismTRA(); 
      }
      return temp;
    }

  }

  public ArrayList<State> stateList = new ArrayList<State>();

  public void removeDeadEnds() {
    int s = 0;
    int statesRemoved = 0;
    int transitionsRemoved = 0;
    boolean deadEndsExist = false;
    
    do {
      deadEndsExist = false;

      while (s < stateList.size()) {
        stateList.get(s).index = s;
        if (stateList.get(s).isTarget) {
          s++;
          continue;
        }
        for (int t = 0; t < stateList.get(s).outgoingTrans.size();) {
          if (stateList.get(s).outgoingTrans.get(t).to >= stateCount) {
            if (DO_PRINT) System.out.println("Removing transition to state " + stateList.get(s).outgoingTrans.get(t).to);
            stateList.get(s).outgoingTrans.remove(t);
            transitionCount--;
            transitionsRemoved++;
            deadEndsExist = true;
          }
          else {
            t++;
          }
        }
        if (stateList.get(s).outgoingTrans.size() == 0) {
          if (DO_PRINT) System.out.println("Removing state " + s);
          stateList.remove(s);
          stateCount--;
          statesRemoved++;
          deadEndsExist = true;
        }
        else {
          s++;
        }
      }
  
      // int t;
      // for (s = 0; s < stateList.size(); s++) {
      //   for (t = 0; t < stateList.get(s).outgoingTrans.size(); t++) {
      //     if (stateList.get(s).outgoingTrans.get(t).to > stateList.size()) {
      //       stateList.get(s).outgoingTrans.remove(t);
      //       System.out.println("Removed transition to state " + stateList.get(s).outgoingTrans.get(t).to);
      //     }
      //   }
      // }
    } while (deadEndsExist);

    System.out.printf("Removed %d dead-end states and %d corresponding transitions.\n", statesRemoved, transitionsRemoved);
  }

  public class Path {
    public ArrayList<State> states;
    public ArrayList<String> commutable;
    public Path() {
      this.states = new ArrayList<State>();
      this.commutable = new ArrayList<String>();
    }
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
      // System.out.println("New StateVarNode for State " + (stateCount-1));
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

  // public String printUniqueString() {
  //   String running = "";
  //   StateVarNode cur = StateVarRoot;
  //   for (int stateVar = 0; stateVar < numStateVariables; stateVar++) {
  //     for (int i = 0; i < cur.children.size(); i++) {
  //       running += (" " + cur.children.get(i).value);
  //     }
  //     // running += ("[" + cur.stateIndices.get(0) + "]");
  //     cur = cur.children.get(0);
  //   }
  //   return running;
  // }

  // function to check uniqueness and update unique state tree
  public int stateIsUnique(int varVals[]) {
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

    for (int stateVar = 0; stateVar < numStateVariables; stateVar++) {
      
      foundStateVar = false;
      for (int i = 0; i < cur.children.size(); i++) { // new states have 0 kids anyway
        if (cur.children.get(i).value == varVals[stateVar]) {
          if (DO_PRINT) {
            System.out.printf("\nM(");
            for (int aa = 0; aa < cur.children.get(i).stateIndices.size(); aa++) {
              System.out.printf("%s.", cur.children.get(i).stateIndices.get(aa));
            }
            System.out.printf(") %d and %d", varVals[stateVar], cur.children.get(i).value);
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
          System.out.printf("X() %d\n", varVals[stateVar]);
        }
        cur.children.add(new StateVarNode(varVals[stateVar]));
        cur.children.get(cur.children.size()-1).parent = cur;
        cur = cur.children.get(cur.children.size()-1);
        // System.out.println("\nAdded parental relationship " + cur.value + " child of " + cur.parent.value);
        foundStateIndex = -1;
        // System.out.println("AAAA");
        // for (int aaa = 0; aaa < possibleStates.size(); aaa++) {
        //   System.out.println(possibleStates.get(aaa));
        // }
        for (int j = 0; j < possibleStates.size(); ) {
          possibleStates.remove(j);
        }
      }
      // System.out.println("BBBB");
      // for (int aaa = 0; aaa < possibleStates.size(); aaa++) {
      //   System.out.println(possibleStates.get(aaa));
      // }
    }
    // System.out.println("CCCC");
    // for (int aaa = 0; aaa < possibleStates.size(); aaa++) {
    //   System.out.println(possibleStates.get(aaa));
    // }
    // System.out.println("foundStateVar? " + foundStateVar);

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

  public void buildAndCommute(Prism prism, String[] transitions, String[] prefix, int depth, Path parentPath, Expression target)
  {
    try {
      
      if (depth > MAX_DEPTH) {
        if (DO_PRINT) System.out.println("Max recursion depth reached.");
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
        new State(sim.getCurrentState().varValues);
        // we know initial state is unique but we need to log its values with stateIsUnique.
        stateIsUnique(getIntVarVals(sim.getCurrentState().varValues));
        // System.out.println("ROOT " + printUniqueString());
      }
      
      // update current state to be the model's initial state
      State currentState = stateList.get(0);
      currentState.isNewInit = true;
      
      // Walk along the prefix to get the new initial state
      int prefixLength = 0;
      if (prefix != null) prefixLength = prefix.length;
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
          }
        }
        // If we never found the correct transitions, report error
        if (transitionIndex == -1) {
          System.out.printf("ERROR: Prefix transition not available from current state: ");
          System.out.println(sim.getCurrentState());
          System.exit(10001);
        }
        // Take the transition
        sim.manualTransition(transitionIndex);

        // update the total outgoing rate of the current state
        currentState.totalOutgoingRate = totalOutgoingRate;
        
        // Check if the state exists yet
        indexOfFoundState = stateIsUnique(getIntVarVals(sim.getCurrentState().varValues));

        
        // figure out what state to link here
        State stateToAdd = null;
        if (indexOfFoundState == -1) {
          stateToAdd = new State(sim.getCurrentState().varValues);
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
        Transition newTrans = new Transition(transitionIndex, transitions[path_tran], currentState.index, stateToAdd.index, newTranRate);
        currentState.outgoingTrans.add(newTrans);
        
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

      // Mark we have a new "path initial state", as it were
      currentState.isNewInit = true;

      // Save these states into a path
      Path seedPath = new Path();

      // Set up lists to check commutable transitions
      ArrayList<String> wasEnabled = new ArrayList<String>();
      
      // Add initially enabled transitions to start the commutable check
      for (int sim_tran = 0; sim_tran < sim.getNumTransitions(); sim_tran++) {
        seedPath.commutable.add(sim.getTransitionActionString(sim_tran));
      }

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

        // Check if the state exists yet
        indexOfFoundState = stateIsUnique(getIntVarVals(sim.getCurrentState().varValues));
        if (DO_PRINT && indexOfFoundState == -1) {
          // System.out.println("Path state is unique.");
        }

        // TODO: Since we're trying to commute, if this doesn't work, return
        
        // figure out what state to link here
        State stateToAdd = null;
        if (indexOfFoundState == -1) {
          stateToAdd = new State(sim.getCurrentState().varValues);
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

      if (DO_PRINT)  {
        System.out.println("One path explored: ");
        for (int i = 0; i < seedPath.states.size(); i++) {
          System.out.printf("%d ", seedPath.states.get(i).index);
        }
        System.out.println(".");
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
          sim.backtrackTo(seedIndex);
          if (DO_PRINT) System.out.println(seedPath.commutable.get(ctran));
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
          sim.manualTransition(transitionIndex);
          if (DO_PRINT) System.out.println("sim: " + sim.getCurrentState());
          
          // update the total outgoing rate of the current state
          currentState.totalOutgoingRate = totalOutgoingRate;
          
          // Check if the state exists yet
          indexOfFoundState = stateIsUnique(getIntVarVals(sim.getCurrentState().varValues));

          if (DO_PRINT) System.out.printf("Sim state after commuting: %s\n", sim.getCurrentState());
          if (DO_PRINT) System.out.printf("Index of found state: %d\n", indexOfFoundState);
          
          // figure out what state to link here
          State stateToAdd = null;
          if (indexOfFoundState == -1) {
            stateToAdd = new State(sim.getCurrentState().varValues);
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
      if (depth < MAX_DEPTH) {
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
    State absorbingState = new State(tempAbsorb);
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
      // TODO: Read the file
      Expression target = prism.parsePropertiesString(PROPERTY).getProperty(0);
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

      System.out.println("Finished! Processed " + numPaths + " paths.");

      // close PRISM
      prism.closeDown();

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
