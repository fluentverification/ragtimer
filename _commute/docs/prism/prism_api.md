---
title: "PRISM API Notes"
author: "Landon Taylor"
created: "08 June 2022"
---

# PRISM API Notes

The following notes are an unofficial documentation for some of the functionality of the PRISM API (in Java). At the time of the creation of this document, there is no known source for official PRISM API documentation.

Let `x` and other single letters always represent a user-named variable.

## Initialization

Import so many things:

```java
// from PRISM
import prism.Prism; // main PRISM things
import prism.PrismDevNullLog; // Logging
import prism.PrismLog; // Logging
import prism.PrismPrintStreamLog; // Logging
import prism.PrismException; // Exception handling

// from PRISM's parser
import parser.Values; // Parsing state variable values
import parser.ast.Expression; // Handling CSL properties
import parser.ast.ModulesFile; // Importing the model
```

Create a log called `mainLog` for PRISM to use:

```java
PrismLog mainLog = new PrismDevNullLog();
```

Initialize a PRISM simulation engine, where `mainLog` is of type `PrismLog`, noting the use of "s" in "initiali**s**e:

```java
Prism prism = new Prism(mainLog);
prism.initialise();
```

Parse a model from a file, then load the model for checking:

```java
ModulesFile modulesFile = prism.parseModelFile(new File("model.sm"));
prism.loadPRISMModel(modulesFile);
```

Load a model into a simulator engine:

```java
prism.loadModelIntoSimulator();
SimulatorEngine sim = prism.getSimulator();
```

Let `sim` be the `SimulatorEngine` we created at the previous step.

Initialize a new path, noting the use of "s" in "initiali**s**e".

```java
sim.createNewPath();
sim.initialisePath(null);
```

## Simulating

Let `sim` be the `SimulatorEngine` we created at initialization.

The following functions can be called from `sim` to simulate the model.

### Transitions and Probabilities

All of these functions can be found in the PRISM source code. See [prism/SimulatorEngine.java](https://github.com/prismmodelchecker/prism/blob/4fcd9756fe338f6f8cc1189a4e0c2ee2edb561bd/prism/src/simulator/SimulatorEngine.java#L1093) for details.

The following returns an `int` value representing the total number of transitions available at the current simulator state. 

```java
sim.getNumTransitions()
```

Available transitions are assigned and can be located using a numerical index. *This index changes at each state*. Transition names are stored in PRISM (surrounded by square brackets), so you can use the transition's name to find the index.

This task relies on this function to get the transition's name given its index `i`. It returns a `String` with the transition's name in square brackets.

```java
sim.getTransitionActionString(i)
```

The following loop can help you find the index of the transition given the transition's name. The value `index` is the index of the desired transition, and `t_name` is the transition's name stored in a `String`.

```java
int index = 0;
for (int i = 0; i < sim.getNumTransitions(); i++) {
    String s1 = String.format("[%s]", t_name);
    String s2 = sim.getTransitionActionString(i);
    if (s1.equalsIgnoreCase(s2)) {
        index = i;
        break;
    }
}
```

Of course, transitions also have associated probabilities (for a DTMC) or rates (for a CTMC). The following function returns a `double` with the *probability*(DTMC) or the *rate* (CTMC), where `i` is the transition index.

```java
sim.getTransitionProbability(i)
```

It is desirable to find the probability of a transition in a CTMC. From *Introduction to the Numerical Solution to Markov Chains* page 20:

> For an ergodic CTMC the one-step transition probabilities of its EMC, denoted by $s_{ij}$ [...] are given by
> 
> $$
> s_{ij} = \frac{q_{ij}}{\sum_{j\neq i}q_{ij}}, j \neq i
> $$
> 
> $$
> s_{ij} = 0, j = i
> $$

That is, the probability of transition `i` equals the rate of transition `i` divided by the sum of all other outgoing transition rates from the current state.

This calculation is implemented using the following loops. The loops remain separate because while the loop indices are identical, the second loop breaks when it finds the index of the state. Some clever manipulation can resolve this, but at the expense of the clarity desired in this document.

```java
// Get the total rate by accumulating all outgoing rates
for (int idx=0; idx < sim.getNumTransitions(); idx++) {
    totalRate += sim.getTransitionProbability(idx);
} 

// Get the index of the transition of interest, as before
for (int idx=0; idx < sim.getNumTransitions(); idx++) {
    String s1 = String.format("[%s]",tr_st[tdx]);
    String s2 = sim.getTransitionActionString(idx);
    if (s1.equalsIgnoreCase(s2)) {
        index = idx;
        break;
    }
}

// Get the rate of the transition of interest
double rate = sim.getTransitionProbability(index);

// Get the probability of the transition to fire
double transition_probability = rate / totalRate;
```

Once you have selected the transition you desire to fire, it is almost trivial to manually fire the transition, given its index `i`:

```java
sim.manualTransition(i);
```

You may need to go back after firing a transition. You can backtrack to a given step `i` (starting your path at step 0), using this function:

```java
sim.backtrackTo(i);
```

If you are keeping track of a rolling state index `j` as you walk along a path, you can call it this way to go back to your most recent state:

```java
sim.backtrackTo(j-1);
```

### States and Properties

The following function gets the current state (as a State object):

```java
sim.getCurrentState()
```

To get the state variable values from the current state, use the following variable (not function):

```java
sim.getCurrentState().varValues
```

This will return an Object array. The Object is often of type Integer or String. To get the value as an int, you can use the following to fill the integer array `values` with the variable values.

```java
// Get the objects from the state variable values
Object[] varList = sim.getCurrentState().varValues;
// Initialize a new array to store the variables
int[] values = new int[varList.length];
// Loop through the variable list to convert to ints
for (int i = 0; i < varList.length; i++) {
    // Check if Object values is an Integer or a String
    if (templist_c[i] instanceof Integer) {
        values[i] = (Integer) varList[i];
    }
    else if (templist_c[i] instanceof String) {
        values[i] = Integer.parseInt((String) varList[i]);
    }
}
```

The indices of state variables do not change at differing states, like transition indices do. If you need to see the name of a state variable at index `i`, use the following, which returns a String of the variable name.

```java
sim.getVariableName(i)
```

To parse a property from a string `csl_str`, the following sequence will create a target state expression `target` and get the first property it finds from the string. The string is a Boolean condition (such as `x > 5`)

```java
Expression target = prism.parsePropertiesString(csl_str).getProperty(0);
```

To evaluate whether a current state satisfies the property, the following function evaluates the Boolean property in `Expression target` to `true` or `false`:

```java
target.evaluateBoolean(sim.getCurrentState())
```

Of course, you can call it on any State object, so if you save a State `s` and check it later, it looks like this:

```java
State s = sim.getCurrentState()
// many lines of doing interesting things
if (target.evaluateBoolean(s)) // do something
```

There are a number of additional evaluate functions, but they are not covered here. See [prism/Expression.java](https://github.com/prismmodelchecker/prism/blob/master/prism/src/parser/ast/Expression.java) for details in code comments.

### Full Path and Termination

The full path can be accessed using the following:

```java
sim.getPathFull()
```

The path can be exported using the following:

```java
sim.getPathFull().exportToLog(
    new PrismPrintStreamLog(System.out), true, ",", null
);
```

Close PRISM down with the following:

```java
prism.closeDown();
```
