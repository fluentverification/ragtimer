---
title: Algorithm for Counterexample Permuting
author: Landon Taylor
created: 8 June 2022
---

# Current Algorithm

The current algorithm is documented in the LaTeX note document. This algorithm appears to have a major limitation in which commuting more than one transition at a time becomes quite complicated, especially when verifying the correctness of transition independence. The proposed algorithm should resolve this issue.

# Proposed Algorithm

This proposed modified algorithm would enable us to discover reversible transitions, too!

## **Function commutePath(path $p$, prefix $x$):**

1. Find all the commutable transitions along $p$, whose transition sequence is given by $t_0, t_1, \ldots t_k \ldots t_n$.

2. Perform the following to fire all commutable transitions $t_i \in T_{com}$ transitions along $p$ and perform safety checks:
   
   1. Fire $t_i$ from each state $s_k$ along $p$, reaching state $s_k'$. Save $t_i$ for each state. If $s_n'$ is not a target state, skip $t_i$ and attempt $t_{i+1}$.
   
   2. Attempt to fire $t_k$ from each $s_k'$ (fire the transition parallel to the seed path). If it can be fired, save the transition from $s_k'$ to $s_{k+1}'$.
   
   3. If there exists a complete path $p_i$ from $s_0'$ to $s_n'$ (from the *commuted* initial state to the *commuted* target state), add $p_i$ to $P$ and $t_i$ to $X$. This path should **not** include the seed path initial state, and thus should thus have $t_i$ as its prefix.

3. Construct the model and report the probability, if desired. If the probability at this point is over a user-specified threshold, terminate.

4. For each commuted path $p_i \in P$ with its corresponding $x_i \in X$, call **commutePath($p_i$, $x_i$)**.
