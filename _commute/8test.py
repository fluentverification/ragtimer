import subprocess
import math
import os
import random
import sys

react = "8react"
timeUnits = "20"
tracePath = "paths/other/a.txt"
prop = "G_bg = 50"
flex = "0.3"
recursion = "1"
directory = "time8Run" + flex

options = open("options.txt", "w")
options.write(f"model models/{react}/model.sm\n")
options.write(f"trace {tracePath}\n")
options.write(f"property {prop}\n")
options.write(f"recursionBound {recursion}\n")
options.write(f"timeBound {timeUnits}\n")
options.write(f"flexibility {flex}\n")
options.write(f"terminate time\n")
options.write(f"cycleLength 0\n")
options.write("export both\n")
options.write("verbose")

options.close()

os.system(f"make")
os.system(f"mkdir results/{directory}")
os.system(f"/usr/bin/time -o results/{directory}/time.txt make test > results/{directory}/out.txt")
os.system(f"mv prism.* results/{directory}/")
os.system(f"mv storm.* results/{directory}/")
os.system(f"/usr/bin/time -o results/{directory}/prismTime.txt prism -importmodel results/{directory}/prism.tra,sta,lab -ctmc models/{react}/fullPro.csl > results/{directory}/prismOut.txt")
#os.system(f"""/usr/bin/time -o results/{directory}/stormTime.txt storm --explicit results/{directory}/storm.tra results/{directory}/storm.lab --prop 'P=? [true U[0,{timeUnits}] "target"]' > results/{directory}/stormOut.txt""")
os.system(f"cp options.txt results/{directory}/")
