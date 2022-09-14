package simulate;

import java.io.File;
import java.io.FileNotFoundException;

import java.io.BufferedReader;
import java.io.FileReader;

import java.io.IOException;

import parser.Values;
import parser.ast.Expression;
import parser.ast.ModulesFile;

import prism.Prism;
import prism.PrismDevNullLog;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismPrintStreamLog;

import simulator.SimulatorEngine;


/**
Based in large part on github/prismmodelchecker/prism-api/src/SimulateModel.java
*/

public class GetProbability
{
  public static void main(String[] args)
  {
    new GetProbability().run();
  }

  public void run()
  {
    try {

      int pathCount = 0;
      double recordHigh = 0.0f;
      int invalidSince = -2;

      boolean newInit = false;

      // give PRISM output a log file
      PrismLog mainLog = new PrismDevNullLog();

      // initialise (with an s) PRISM engine
      Prism prism = new Prism(mainLog);
      prism.initialise();

      // parse the prism model
      // For now, model.sm is the model file.
      // ModulesFile modulesFile = prism.parseModelFile(new File("../models/DonovanYeastPolarization/yeastPolarization.sm"));
      ModulesFile modulesFile = prism.parseModelFile(new File("../model.sm"));

      // load the prism model
      prism.loadPRISMModel(modulesFile);

      // if the model has unknown constant values, deal with that here.
      // for now, we assume NO UNDEFINED CONSTANTS

      // load the model into the simulator
      prism.loadModelIntoSimulator();
      SimulatorEngine sim = prism.getSimulator();

      // initialize total model probability (lower bound) to zero
      double totalProbability = 0.0;
		
      // read the CSL property for making sure we end up in a target state
      FileReader fr_p = new FileReader("../model.csl");
      BufferedReader br_p = new BufferedReader(fr_p);
      String x_p;
      x_p = br_p.readLine();
      Expression target = prism.parsePropertiesString(x_p).getProperty(0);

      // Read in the traces
			FileReader fr = new FileReader("trace_list.txt");
			BufferedReader br = new BufferedReader(fr);
			String x;

      // set up a placeholder, temporary index
      int index;

      do {
        // Read in the next input line
			  x = br.readLine();

        pathCount++;
        // System.out.println(pathCount);
        // System.out.println(x);

        // Double if we're at the end of the file
        if (x == null) {
          break;
        }

        // Break the string into a transition set
        String[] tr_st=x.split("\\s+");
        
        if (tr_st[0].contains("_PREFIX_")) {
          String staStr = "Variables: (";
          newInit = true;
          // Get variable names
          int vari = 0;
          while (true) {
            staStr += sim.getVariableName(vari);;
            if (sim.getVariableName(vari+1) == null) {
              staStr += ")\n";
              break;
            }
            staStr += ",";
            vari++;
          }
          System.out.println(staStr);
        }

        // create a new path
        sim.createNewPath();
        sim.initialisePath(null);
        // sim.createNewOnTheFlyPath(); // recommended for efficiency but can break things

        // Take each transition and collect the rates
        double pathProbability = 1.0;
        double totalRate = 0.0;
        // System.out.printf("%d length\n", tr_st.length);
        
        int tdx = 0;
        if (newInit) tdx = 1;
        // Loop through each transition in the trace string
        for (; tdx < tr_st.length; tdx++) {
          if (tr_st[tdx] == "") {
            continue;
          }
          index = -1;
          totalRate = 0.0;
          // Loop in order to get the total rate of possible outgoing transitions
          // at the current state
          for (int idx=0; idx < sim.getNumTransitions(); idx++) {
            // System.out.printf("tr %d: %s %f\n", idx, sim.getTransitionActionString(idx), sim.getTransitionProbability(idx));
            // System.out.printf("%s ", sim.getTransitionActionString(idx).replace("[","").replace("]",""));
            totalRate += sim.getTransitionProbability(idx);
          }
          // Loop to find which available transition is chosen
          for (int idx=0; idx < sim.getNumTransitions(); idx++) {
            String s1 = String.format("[%s]",tr_st[tdx]);
            String s2 = sim.getTransitionActionString(idx);
            // System.out.println(s2);
            if (s1.equalsIgnoreCase(s2)) {
                index = idx;
                break;
            }
          }
          // Make sure we found the valid transition
          if (index == -1) {
            System.out.printf("ERROR - Index remains -1, invalid trace at line %d at %d", pathCount, tdx);
            System.out.println(" with transition " + tr_st[tdx]);
            for (int idx=0; idx < sim.getNumTransitions(); idx++) {
              String s2 = sim.getTransitionActionString(idx);
              System.out.println("AVTRAN-" + s2);
            }
            // System.out.println(pathCount);
            break;
          }
          // Get the transition probability
          double transition_probability = sim.getTransitionProbability(index) / totalRate;
            // System.out.printf("\n======= tr %s (%d) %e ===========\n\n", tr_st[tdx], index, transition_probability);
            // System.out.printf("\n");
          // Update the path probability
          pathProbability *= transition_probability;
          // Fire the transition
          sim.manualTransition(index);
        }

        if (newInit) {
          System.out.printf("Transitions: %s\nProbability: %e\nState: %s\n\n", x, pathProbability, sim.getCurrentState());
        }

        // Check that we end up at a state satisfying the property
        else if (target.evaluateBoolean(sim.getCurrentState())) {
          // If we end up in a target state, update our total probability
          totalProbability += pathProbability;
          if (recordHigh < pathProbability) {
            recordHigh = pathProbability;
            System.out.printf("New highest probability: %e at line %d\n", recordHigh, pathCount);
          }
          if (invalidSince >= 0) {
            System.out.printf("Target state not reached and probability not counted from line %d to line %d\n", invalidSince, pathCount-1);
            invalidSince = -1;
          }
        }
        else {
          if (invalidSince < 0) {
            invalidSince = pathCount;
          }
          // System.out.printf("Target not reached. Current State: %s\n", sim.getCurrentState());
          // sim.getPathFull().exportToLog(new PrismPrintStreamLog(System.out), true, ",", null);
          // System.out.printf(">> Path Reaches Target :)\n");
          // System.out.printf("pathProbability %e\n", pathProbability);
        }

      } while (x != null);

      // close PRISM
      prism.closeDown();

      if (!newInit) {
        System.out.printf("Total probability: %e\n", totalProbability);
      }
    } 
    catch (FileNotFoundException e) {
			System.out.println("FileNotFound Error: " + e.getMessage());
			System.exit(1);
		} 
    catch (PrismException e) {
			System.out.println("PrismException Error: " + e.getMessage());
			System.exit(1);
		}
    catch (IOException e) {
      System.out.println("IOException Error: " + e.getMessage());
			System.exit(1);
    }
  }
}