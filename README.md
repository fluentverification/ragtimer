![RAGTIMER Logo](logo/logo_sm.png)

# RAGTIMER
Random Assume Guarantee Testing Induced Model Executions for Reachability (RAGTIMER)

## Installation Instructions

The following instructions should guide installation of RAGTIMER and its dependencies. Better instructions are coming soon.

1. Install Microsoft's version of IVy, with instructions for Debian 11 available at https://www.landonjtaylor.net/ivy

2. Install PRISM following instructions at https://prismmodelchecker.org

3. Modify Makefile in the RAGTIMER scripts directory to match your existing PRISM installation folder (not the prism/prism folder, simply the main prism directory)

4. Clone the RAGTIMER repository and play around. The model should be located at scripts/model.sm scripts/model.csl scripts/model.ragtimer as shown by existing files. 


## IVy Model Generator
### Input File Format

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

### IVy Models
Two IVy Models are generated based off of the information in the input file. The first is written to the file 'test_v2.ivy' and testing of this model only produces one trace before intentionally using assertion failure to stop the tests. The second is written to the file 'test_v3.ivy' and produces the number of traces that is specified by the user. 

## Dependency Graph
Dependency graph processing documentation can be found at [docs/dependency.md](docs/dependency.md)

## Running RAGTIMER
To run RAGTIMER navigate to the scripts folder and give the command:
```sh
 $ python3 main.py <number of traces wanted>
```
RAGTIMER uses the 'model.ragtimer' file found in this repository to generate results. To change the model please modify the 'model.ragtimer' file directly.

By default, RAGTIMER only produces the shortest traces to a specified target. If it is preferred that not only the shortest traces are produced, i.e. there are a limited number of shortest traces available, then add the "loose" parameter when running RAGTIMER. This produces traces that consider all the reactions available, but gives priority to the reactions needed to reach the target. 
```sh
 $ python3 main.py <number of traces wanted> loose
```

To produce the specified number of traces for each of the model's available prefixes add the "each" paramter when running RAGTIMER. 
```sh
 $ python3 main.py <number of traces wanted> each
```
