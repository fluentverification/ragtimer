---
title: "Setting Up"
author: "Landon Taylor"
created: "08 June 2022"
---

# Setting Up the Path Commuter

This document will walk you through setup.

## Installation

1. Make sure you can use Python 2.7 and Python 3 from the same terminal. Ubuntu 18 is the only OS that has worked for me for this.
2. Ensure PRISM is installed and in the right directory by following the PRISM API installation instructions
3. Install IVy 1.7 with submodules
4. Install JDK 13
5. Install Python 2.7 (for IVy)
6. Install Python 3 (for script)

## Check Your Installation

Run `ivy_check`, `prism`, `python2.7`, `python3`, and `javac`. If each works, you're probably good to go on the software prerequisite front.

## Modify the PRISM directory

Make sure PRISM is on your PATH, preferably using the following on Ubuntu:

```bash
PRISM=YOUR_PATH/prism/prism
export PRISM
PATH="$PATH:$PRISM/bin"
export PATH
```

Then modify the top line of `Makefile` to accurately reflect your PRISM root folder. This is the folder of the git clone (i.e. prism rather than prism/prism).
