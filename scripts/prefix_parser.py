import depgraph
import os
import subprocess

class Prefix:
    # Initialize a basic, empty reaction
    def __init__(self):
        self.varnames = []
        self.probabilities = []
        self.transitions = []
        self.values = []

    def __str__(self) -> str:
        return str(self.varnames) + "\n" + str(self.probabilities) + "\n" + str(self.transitions) + "\n" + str(self.values)


def parsePrefix(o):

    prefix = Prefix()
    needVars = True

    for line in o.split("\n"):
        if needVars and "Variables:" in line:
            l = line.split("(")[1].split(")")[0].split(",")
            for v in l:
                if v != "":
                    prefix.varnames.append(v)
            needVars = False
        if "Transitions:" in line:
            trans = []
            l = line.split("\t")
            for t in l:
                if "_PREFIX_" not in t and t != "":
                    trans.append(t)
            prefix.transitions.append(trans)
        if "State:" in line:
            vals = []
            l = line.split("(")[1].split(")")[0].split(",")
            for v in l:
                if v != "":
                    vals.append(v)
            prefix.values.append(vals)
        if "Probability:" in line and "Total" not in line:
            l = line.split("Probability: ")[1]
            p = float(l)
            prefix.probabilities.append(p)

    return prefix



if __name__ == "__main__":
    dg = depgraph.makeDepGraph("8reaction_input.txt", False)

    with open("trace_list.txt", "w") as t:
        t.write("")

    for r in dg:
        if (r.tier == 0):
            depgraph.printPrefixes("trace_list.txt", "", r)

    os.system("make")
    o = subprocess.check_output(["make", "test"],universal_newlines=True)

    print("Printing output for debug reasons")
    print(o)

    print(parsePrefix(o))