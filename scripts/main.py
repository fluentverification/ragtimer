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
<<<<<<< HEAD

    # print(paths)
    # quit()

    o = subprocess.check_output(["make", "test"],universal_newlines=True)

    print(o)
    prefix = prefix_parser.parsePrefix(o)
    # print(prefix)

    for a in range(len(paths)):
        print(80*"-")
        print(paths[a])
        print(80*"-")
        reactions_v5.randTest(i, reactions1, prefix, a, False, paths[a])

        os.system("make test")
=======

    # print(paths)
    # quit()

    o = subprocess.check_output(["make", "test"],universal_newlines=True)

    print(o)
    prefix = prefix_parser.parsePrefix(o)
    # print(prefix)

    for i in range(len(paths)):
        reactions_v5.randTest(i, reactions1, prefix, i, False, paths[i])

    os.system("make test")
>>>>>>> 4536381df9921aaa93beb101aad6088e07aa4162
