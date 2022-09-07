import os
import reactions_v5
import depgraph
import prefix_parser
import sys
import subprocess
import math

if __name__ == "__main__":

    print("Expect to see a message claiming an error: assertion failure.")
    print("This message indicates correct functionality.")

    i = sys.argv[1]
    each = False

    if "each" in sys.argv:
        each = True

    reactions1 = depgraph.makeDepGraph(reactions_v5.Options.infile, False)
    
    with open("trace_list.txt", "w") as t:
        t.write("")

    paths = []
    for r in reactions1:
        if (r.tier == 0):
            depgraph.printPrefixes("trace_list.txt", "", r, paths)

    # print(paths)
    # quit()

    o = subprocess.check_output(["make", "test"],universal_newlines=True)
    print(o)
    prefix = prefix_parser.parsePrefix(o)
    # print(prefix)

    j = len(prefix.values)

    iters = i

    if not each:
        # we will do AT LEAST as many total paths as they ask for
        iters = math.ceil(float(i) / float(j))

    prob = float(0.0)

    for a in range(len(paths)):
        print(50*"-")
        print(paths[a])
        print(50*"-")
        reactions_v5.randTest(iters, reactions1, prefix, a, False)

        # os.system("make test")
        o = subprocess.check_output(["make", "test"],universal_newlines=True)
        print(o)
        for line in o.splitlines(False):
            if "Total" in line:
                prob += float(line.split(": ")[1])

    print("Total Sum of Unique Path Probabilities:", prob)
