import depgraph
import os

dg = depgraph.makeDepGraph("8reaction_input.txt", False)

with open("trace_list.txt", "w") as t:
    t.write("")

for r in dg:
    if (r.tier == 0):
        depgraph.printPrefixes("trace_list.txt", "", r)

os.system("make")
os.system("make test")