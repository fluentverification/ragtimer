# Ragtimer Artifact

Produced for "Efficient Trace Generation for Rare-Event
Analysis in Chemical Reaction Networks" for 
Verification, Model Checking, and Abstract Interpretation 2023.

## Virtual Machine

Download and run the virtual machine as follows:

1. Download at (url)
2. Load virtual machine into preferred VM client. This VM has been tested in KVM on Debian 11.
3. Log in as user `Ragtimer User` with password `ragtimer`
4. Open a terminal. Source code for RAGTIMER is found in the `~/ragtimer` directory.

## Running a Ragtimer Test

1. Navigate to the `~/ragtimer/scripts` directory using `cd ~/ragtimer/scripts`.
2. In this folder is a shell script `artifact.sh` which will generate 10,000 traces for the three models mentioned in "Efficient Trace Generation for Rare-Event Analysis in Chemical Reaction Networks". These models are informally described below.
3. Expect in the printout for each model, with the final line in each printout providing the target's probability. Because RAGTIMER relies on randomized testing, each execution's probability will be slightly different.
4. If desired, modify the second line of `~/ragtimer/model.ragtimer` to produce an unreachable target state. Using tabs to separate, change `50	2	0	50	0	0	0` to `49	2	0	50	0	0	0`. Because the target state becomes unreachable, Ragtimer will report unreachability.

## Model Descriptions

1. **Single Species Production Degredation Model**. In this model, the goal is to produce a single species.
2. **Enzymatic Futile Cycle Model**. In this model, six reactions.
3. **Yeast Polarization Model**. In this model, eight reactions.