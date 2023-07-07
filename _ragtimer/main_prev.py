from cgitb import enable
import os
import reactions_v5
import depgraph
import prefix_parser
import sys
import subprocess
import math



if __name__ == "__main__":

    PRINTING = False
    COMMUTE = False

    print()
    print(80*"=")
    print("Welcome to RAGTIMER for trace generation.")
    print("This is a work in progress. Please submit bug reports.")
    print(80*"=")
    print()

    i = sys.argv[1]
    each = False
    loose = False

    if "each" in sys.argv:
        each = True

    if "loose" in sys.argv:
        loose = True

    if "commute" in sys.argv:
        COMMUTE = True


    print("Constructing a dependency graph")
    reactions1 = depgraph.makeDepGraph(reactions_v5.Options.infile, printing=PRINTING)
    
    # with open("trace_list.txt", "w") as t:
    #     t.write("")

    paths = []

    print("Finished constructing a dependency graph, found these prefixes:")
    for r in reactions1:
        # print(r)
        if (r.tier == 0):
            depgraph.printPrefixes("trace_list.txt", "", r, paths)
            
    print()
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
    
    print(os.getcwd())
    o = subprocess.check_output(["make"], universal_newlines=True)
    # o = subprocess.check_output(["make", "test"], universal_newlines=True) #uncomment to show errors, if there are any
    o = subprocess.check_output(["make", "test"], universal_newlines=True, stderr=subprocess.DEVNULL)
    prefix = prefix_parser.parsePrefix(o)
    

    if "ERROR" in o:
        # print(o)
        # print("old1", paths)
        enabledReacts = []
        lines = o.split("\n")
        for line in range(len(lines)):
            # print(line)
            if "ERROR" in lines[line]:
                for l2 in lines:
                    # print("l2",l2)
                    if "AVTRAN-" in l2:
                        s0 = l2.replace("]","").replace("[","")
                        # print(s0)
                        s1 = s0.split("AVTRAN-")[1]
                        # print(s1)
                        s2 = s1.rstrip()
                        # print(s2)
                        if s2 not in enabledReacts:
                            enabledReacts.append(s2)
                        # print("enabledReacts",enabledReacts)
                # invalid trace at line 1 at 0 with transition r6 
                pathnumber = int(lines[line].split(" trace at line ")[1].split(" ")[0]) - 1
                # print(pathnumber)
                rempath = paths[pathnumber]
                # print(enabledReacts)
                for er in enabledReacts:
                    for r in reactions1:
                        if r.name == er and r.enabledToExecute > 0:
                            # print(er + "\t" + paths[pathnumber])
                            if (er + "\t" + paths[pathnumber]) not in paths:
                                paths.append(er + "\t" + paths[pathnumber])
                    # if er in paths[pathnumber]:
                    # for r in reactions1:
                    #     if r.name == er:
                paths.remove(paths[pathnumber])
                break
        # print("new1", paths)

        with open("trace_list.txt", "w") as trace_list:
            for path in paths:
                # print("PREFIX PATH: ", paths)
                trace_list.write("_PREFIX_\t" + path + "\n")

        o = subprocess.check_output(["make", "test"],universal_newlines=True)
        prefix = prefix_parser.parsePrefix(o)
                            

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
    print("Finding", iters)

    if not each:
        # we will do AT LEAST as many total paths as they ask for
        iters = math.ceil(float(i) / float(j))
        print("For each prefix:", iters)

    prob = float(0.0)

    # if COMMUTE:
    with open("../commute_traces.txt", "w") as ct:
        ct.write("")
        # print("WROTE")

    for a in range(len(paths)):
        print(50*"-")
        print("Testing Prefix", paths[a].replace("\t", " "))
        print(50*"-")

        reactions_v5.randTest(iters, reactions1, prefix, a, loose=loose, printing=PRINTING)
        # os.system("make test")
        # if COMMUTE:
        with open("trace_list.txt", "r") as traceList:
            with open("../commute_traces.txt", "a") as ct:
                for line in traceList:
                    ct.write(line.strip() + "\n")
                    # print(line.split()[0:3])
        if (not COMMUTE):
            o = subprocess.check_output(["make", "test"], universal_newlines=True, stderr=subprocess.DEVNULL)
            for line in o.splitlines(False):
                if "Total" in line:
                    prob += float(line.split(": ")[1])
                    print(line.replace("Total probability", "Probability in this prefix"))
            # print(o)
            print("Running total probability: ", prob)

    print()
    print(80*"=")
    print("Total Sum of Unique Path Probabilities:", prob)
    print(80*"=")
    print()
    print()
    print()
    print()
