import os
import reactions_v5
import depgraph
import prefix_parser
import sys
import subprocess

if __name__ == "__main__":

    i = sys.argv[1]

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

    for i in range(len(paths)):
        reactions_v5.randTest(i, reactions1, prefix, i, False, paths[i])

    os.system("make test")
