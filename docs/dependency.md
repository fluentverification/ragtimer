# Dependency Graph Documentation

## Input File Format

All horizontal whitespace is a tab character.
All vertical whitespace is a newline character.

Example for the Donovan Yeast Polarization Model:

```
r     l    rl   g    ga   gbg  gd
50    2    0    50   0    0    0
-1    -1   -1   -1   -1   50   -1
r1    0    >    r    0.0038
r2    r    >    0    0.0004
r3    r    l    >    rl   l    0.042
r4    rl   >    r    0.010
r5    rl   g    >    ga   gbg  0.011
r6    ga   >    gd   0.100
r7    gbg  gd   >    g    1050
r8    0    >    rl   3.21
```

Line 1: Tab-separated list of chemical names

Line 2: ORDERED initial conditions for each chemical

Line 3: ORDERED target conditions, with -1 meaning "don't care"

Line 4 to end: List of reactions in the following format:

```
name    (tab-sep reactants)    >    (tab-sep products)    rate constant
```

All reactions and chemicals must have unique names. Names must include
no whitespace characters. Use the number `0` to indicate a null reactant or product (i.e. spontaneous production or consumption). Catalysts can be included as normal; their behavior is accounted for in the dependency graph builder.

## Using the Python Functions and Objects

Because this project lies in a single file, simply put `depgraph.py` in the same folder as your other Python files, then at the top of any file that accesses this functionality, add `import depgraph` on its own line. Then, you can use the following functionality.

For the sake of ease of access, everything in the file is accessible, but there are no global variables. The following function call is currently implemented:

`makeDepGraph(infile)` where `infile` is a string path to a model file, as defined above. This function kickstarts the process of parsing the input file and building the dependency graph. It returns an array of every reaction in the model (call one reaction `r` representing some `makeDepGraph(infile)[k]`), along with the following information:

- `r.name` (string) is the name of the reaction as input by the user

- `r.rate` (double) is the reaction rate as input by the user

- `r.reactants` (array of strings) contains each reaction's input reactants

- `r.products` (array of strings) contains each reaction's output products

- `r.dependsOn` (array of reactions) contains the reactions required for `r` to execute

- `r.dependCount` (array of integers) contains ordered required execution counts for each reaction required for `r`. The ordering corresponds to `r.dependsOn`. That is, `r.dependsOn[i]` needs to execute `r.dependCount[i]` times.

- `r.executions` is the largest minimal number of times `r` is expected to execute (that is, if we consider the required executions up every branch of the dependency graph).

- `r.tier` is the reaction's depth in the graph. Reactions that lead directly to the target are on tier `0`, and the default tier (which is the resulting tier for reactions not required to reach a target) is `-1`. Note that if a reaction appears on multiple tiers, the final tier is the lowest value, to ensure it receives the full priority it deserves.

At the moment, the functions print useful information to the console, which can be read as described in the next section.

## Console Output

As the dependency graph builder executes, it produces console output in the order it explores reactions. 

### Normal Output

At each recursive function call, assuming we have not formed a cycle, it prints according to the following template:

```
-----------------------------------------------------
TIER 1 Checking r8 From parent r5
-----------------------------------------------------

Current Initial State    [50, 2, 0, 50, 50, 50, 0]
Current Target State     [-1, -1, 50, 50, -1, 50, -1]
Delta Target-Initial     [0, 0, 50, 0, 0, 0, 0]
Chemicals Required       ['rl']
In these quantities      [50]

Required Executions      50
Initial After 50 Execs   [50, 2, 50, 50, 50, 50, 0]
Target After 50 Execs    [-1, -1, 50, 50, -1, 50, -1]

r8 - Rate 3.21
Reactants []
Products ['rl']
Required Executions 50
Tier 1
```

This prints the current working tier, with the following information:

- **Current initial state** is the state reached assuming we already fired the parent reaction(s) the required number of times.

- **Current target state** is the state desired assuming we already fired the parent reaction(s) the required number of times.

- **Delta target-initial** indicates what we need to reach the target from the initial state.

- **Chemicals required** is the list of string chemical names we need to generate in order to reach the parent reaction. **In these quantities** indicates how many of each required chemical we really need.

- **Required executions** indicates how many times we need to fire the reaction in question.

- **Initial after n execs** gives us an updated initial state if we fire the desired reaction n times. **Target after n execs** is similar.

- The updated reaction is finally printed with its name, rate, reaction info, required execution count, and tier.

### Cyclical Output

If a cycle is detected, the output will appear as follows, and execution will move on. The printed information follows the same format as normal output.

```
-----------------------------------------------------
TIER 3 Checking r3 From parent r4
-----------------------------------------------------

Current Initial State    [50, 52, 50, 50, 50, 50, 0]
Current Target State     [50, 50, 60, 50, -1, 50, -1]
Delta Target-Initial     [0, -2, 10, 0, 0, 0, 0]
Chemicals Required       ['rl']
In these quantities      [10]

r3 in reaction history. CYCLE DETECTED.
```

### Necessary Reaction Output

At the end of execution, the necessary reactions are printed, in a similar format to the following:

```
=====================================================
NECESSARY REACTIONS
=====================================================

r3 - Rate 0.042
Reactants ['r', 'l']
Products ['rl', 'l']
Required Executions 50
Tier 1


r5 - Rate 0.011
Reactants ['rl', 'g']
Products ['ga', 'gbg']
Required Executions 50
Tier 0
 - Depends On r3 50 times
 - Depends On r8 50 times


r8 - Rate 3.21
Reactants []
Products ['rl']
Required Executions 50
Tier 1
```

When reactions are printed, the graph structure is also indicated. For instance, `r5` depends on up to `50` executions each of `r3` and `r8`. This printout is a reduced overview of the `reactions` array returned by `makeDepGraph`.  

If you desire to print information for every reaction (and not just necessary reactions), update the following lines:

```python
	for react in reactions:
		if react.tier > -1:
			print(react)
```

to the following:

```python
	for react in reactions:
		print(react)
```

## Future Features:

- Check if the target state is reachable automatically using the graph structure
- Add support for stoichiometry constants
- Add support for decrementing (not just incrementing) toward the target