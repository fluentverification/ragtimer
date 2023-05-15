import subprocess
import math
import os
import random
import sys

react = "2react"
recursion = "1"
rootdir = '/cygdrive/c/Users/adminstrator/cex_permute/results/2reactTime'

flex = []
prob = []
time = []
states = []
tran = []
count1 = 0
for subdir, dirs, files in os.walk(rootdir):
    for file in files:
        if file == "prismOut.txt":
            print(float(subdir[70:75]))
            flex.append(float(subdir[70:75]))
            with open(subdir + "/prismOut.txt", "r") as f:
                count = 0
                while True:
                    line = f.readline()
                    if not line:
                        break
                    if line[0:6] == "Result":
                        #print(float(line.split() [1]))
                        prob.append(float(line.split() [1]))
                    #if line[0] == "N":
        elif file == "time.txt":
            with open(subdir + "/time.txt", "r") as f:
                count = 0
                while True:
                    count += 1
                    line = f.readline()
                    if not line:
                        break
                    if count == 1:
                        #print(float(line[9:13]))
                        time.append(float(line[9:13]))

        elif file == "out.txt":
            count1 += 1
            with open(subdir + "/out.txt", "r") as f:
                #count = 0
                while True:
                    #count += 1
                    line = f.readline()
                    #print(line.split() [1])
                    if not line:
                        break
                    if len(line.split()) > 1:
                        if (line.split() [1]) == "states":
                            #print(int(line[0:1]))
                            states.append(int(line.split()[0]))
                        elif (line.split() [1]) == "transitions":
                            #print(int(line[0:1]))
                            tran.append(int(line.split()[0]))
                    #if line[0] == "N":
            #print(subdir[72:75])

#print(count1)
#print(time)
print(flex)
#print(tran)
#print(states)
print(prob)
