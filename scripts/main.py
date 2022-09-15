import os
import reactions_v5
import depgraph
import prefix_parser
import sys
import subprocess
import math

if __name__ == "__main__":

    PRINTING = False

    print("Expect to see a message claiming an error: assertion failure.")
    print("This message indicates correct functionality.")

    i = sys.argv[1]
    each = False
    loose = False

    if "each" in sys.argv:
        each = True

    if "loose" in sys.argv:
        loose = True

    reactions1 = depgraph.makeDepGraph(reactions_v5.Options.infile, printing=PRINTING)
    
    # with open("trace_list.txt", "w") as t:
    #     t.write("")

    paths = []

    for r in reactions1:
        # print(r)
        if (r.tier == 0):
            depgraph.printPrefixes("trace_list.txt", "", r, paths)
            
                # NEED TO HANDLE CASE WHERE THEY MAKE EQUIV. TRACES???
            
            # extraEnabled = 0
            # for dr in range(len(r.dependCount)):
            #     if r.dependCount[dr] > 0:
            #         if extraEnabled < r.enabledToExecute - r.dependCount[dr]:
            #             extraEnabled = r.enabledToExecute - r.dependCount[dr]
            # if extraEnabled > 0:
            #     paths.append(r.name + "\t" + paths[len(paths)-1])
            # # print(paths)
            # # input("newpaths")

    with open("trace_list.txt", "w") as trace_list:
        for path in paths:
            trace_list.write("_PREFIX_\t" + path + "\n")

    # print(paths)
    # quit()
    
    o = subprocess.check_output(["make", "test"],universal_newlines=True)
    prefix = prefix_parser.parsePrefix(o)

    if "ERROR" in o:
        print("old1", paths)
        enabledReacts = []
        for line in o.split("\n"):
            if str(line).find("AVTRAN") > -1:
                s0 = line.replace("]","").replace("[","")
                print(s0)
                s1 = s1.split("AVTRAN")[1]
                print(s1)
                s2 = s1.rstrip()
                print(s2)
                enabledReacts.append(s2)
                print("enabledReacts",enabledReacts)
            if "ERROR" in line:
                # invalid trace at line 1 at 0 with transition r6 
                pathnumber = int(line.split(" trace at line ")[1].split(" ")[0]) - 1
                print(pathnumber)
                rempath = paths[pathnumber]
                print(enabledReacts)
                for er in enabledReacts:
                    print(er + "\t" + paths[pathnumber])
                    paths.append(er + "\t" + paths[pathnumber])
                    # if er in paths[pathnumber]:
                    # for r in reactions1:
                    #     if r.name == er:
                paths.remove(paths[pathnumber])
                break
        print("new1", paths)
                            

                # reactionname = line.split("with transition ")[1].strip()
                # for r in reactions1:
                #     if r.name == reactionname:
                #         if r.enabledExecutions > 0:
                #             paths[pathnumber] = r.name
        # if r.enabledToExecute > 0 and r.tier == 0:
        #         paths.append(r.name + "\t" + paths[len(paths)-1])
        # print("ERROR IN o")

    # print(o)

        # else:
        #     prefix = prefix_parser.parsePrefix(o)
        # input("===")

                    
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
        reactions_v5.randTest(iters, reactions1, prefix, a, loose=loose, printing=PRINTING)

        # os.system("make test")
        o = subprocess.check_output(["make", "test"],universal_newlines=True)
        print(o)
        for line in o.splitlines(False):
            if "Total" in line:
                prob += float(line.split(": ")[1])

    print("Total Sum of Unique Path Probabilities:", prob)
