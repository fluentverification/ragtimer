import subprocess
import math
import os
import depgraph

class Options:
    infile = "8reaction_input.txt"
    firstIvyModel = "test_v2.ivy"
    firstIvyModelName = "test_v2"
    firstTestResult = "test_v2.txt"
    secondIvyModel = "test_v3.ivy"
    secondIvyModelName = "test_v3"
    secondTestResult = "test_v3.txt"
    traceList = "trace_list.txt"
    reactionList = "reaction_list.txt"

class Reaction:
    def __init__(self, priority, executions):
        self.reactants = []
        self.reactantsNum = []
        self.products = []
        self.productsNum = []
        self.priority = priority
        self.executions = executions

class Species:
    def __init__(self, value, name):
        self.value = value
        self.name = name

class SecondTarget:
    def __init__(self, name, minAmount):
        self.name = name
        self.minAmount = minAmount

class TargetReaction:
    def __init__(self, reaction, secondTargets):
        self.reaction = reaction
        self.secondTargets = secondTargets

o = "{"
c = "}"

reactions1 = depgraph.makeDepGraph(Options.infile)

reactions = []
count = 0

for obj in reactions1:
    count += 1
    reactantsTemp = obj.reactants
    productsTemp = obj.products
    reactNumTemp = []
    prodNumTemp = []
    for x in range(len(reactantsTemp)):
        reactNumTemp.append(1)
    for x in range(len(productsTemp)):
        prodNumTemp.append(1)
    reactions.append(Reaction(obj.tier, obj.executions))
    reactions[count-1].reactants = reactantsTemp
    reactions[count-1].products = productsTemp
    reactions[count-1].reactantsNum = reactNumTemp
    reactions[count-1].productsNum = prodNumTemp

numOfReactions = count

print("\n")
spec = []

for obj in reactions: #adds all the species recorded in the reactions to a list
    count2 = 0
    for x in range(len(obj.reactants)):
        count2 += 1
        if obj.reactants[count2-1] not in spec:
            spec.append(obj.reactants[count2-1])
    count2 = 0
    for x in range(len(obj.products)):
        count2 += 1
        if obj.products[count2-1] not in spec:
            spec.append(obj.products[count2-1])

#print(spec)

speciesList = []
###

chemicals = [] # Stores string names of chemicals
initials = [] # Stores initial values of chemicals
targets = [] # Stores target values of chemicals

with open(Options.infile, 'r') as inpt:
    # Read the line of chemical names
    line = inpt.readline().strip()
    if not line or line == "":
        print("ERROR! CANNOT READ FIRST LINE")
        quit()
    for chem in line.split():
        chemicals.append(str(chem).strip())

    # Read the line of initial values
    line = inpt.readline().strip()
    if not line or line == "":
        print("ERROR! CANNOT READ SECOND LINE")
        quit()
    for val in line.split():
        initials.append(int(val))

    # Read the line of target values (-1 is don't care)
    line = inpt.readline().strip()
    if not line or line == "":
        print("ERROR! CANNOT READ THIRD LINE")
        quit()
    for val in line.split():
        targets.append(int(val))

for obj in spec:
    count = 0
    for x in chemicals:
        count += 1
        if x == obj:
            speciesList.append(Species(initials[count-1], x))


print("The initial values reported are:")

for obj in speciesList: #initial values reported are displayed
    print(obj.name,"=",obj.value)

print("\n")

count = 0
for obj in targets:
    count += 1
    if obj == -1:
        continue
    else:
        targetSpecies = chemicals[count - 1]
        targetNum = obj

for obj in speciesList:
    if obj.name == targetSpecies:
        if obj.value > int(targetNum):
            upOrDown = "3"
        elif obj.value < int(targetNum):
            upOrDown = "1"
        elif obj.value == int(targetNum):
            print("\nSpecified target is already achieved in target state, please start over")
            exit()

for obj in speciesList:
    if obj.name == targetSpecies:
        if upOrDown == "1":
            if obj.value >= int(targetNum):
                print("\nSpecified target is already achieved in target state, please start over")
                exit()
        elif upOrDown == "2":
            if obj.value > int(targetNum):
                print("\nSpecified target is already achieved in target state, please start over")
                exit()
        elif upOrDown == "3":
            if obj.value <= int(targetNum):
                print("\nSpecified target is already achieved in target state, please start over")
                exit()
        elif upOrDown == "4":
            if obj.value < int(targetNum):
                print("\nSpecified target is already achieved in target state, please start over")
                exit()
            if targetNum == "0":
                print("Error, no species can reach a value lower than zero")
                exit()
        else:
            print("Problem with information entered (check make sure your answer you only entered a integer between 1 and 4 and there are no spaces)")
            exit()


print("\n\nThe following priorities have been noted for the reactions: \n")

count = 0
for obj in reactions: #each reactions priority is displayed
    count += 1
    print("reaction", str(count), ":", str(obj.priority))

ivyFile = open(Options.firstIvyModel, "w") #an ivy model for the CRN is made to have assertion failure at first idling action

ivyFile.write(f"""#lang ivy 1.7

object updater = {o}
    type num
    interpret num -> bv[10]
    type exec_var
    interpret exec_var -> bv[8]
    type exec_stage
    interpret exec_stage -> bv[3]
    
    action incr(x:num) returns(y:num) = {o}
        y := x + 1
    {c}
    
    action decr(x:num) returns(y:num) = {o}
        y := x - 1
    {c}
{c}

""")

if upOrDown == "1":
    equality = ">= "
elif upOrDown == "2":
    equality = "> "
elif upOrDown == "3":
    equality = "<= "
elif upOrDown == "4":
    equality = "< "

ivyFile.write(f"""
object goal = {o}
    action achieved(v:updater.num)
    object spec = {o}
        before achieved {o}
            assert v {equality} {targetNum};
            protocol.idle := 1
        {c}
    {c}
{c}

""")

ivyFile.write("object enabled_checker = {\n\n\t")

count = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"action is_enabled_r{count}")
        count2 = 0 
        if (len(reactions[count-1].reactants) == 0):
            ivyFile.write(f""" returns(y:bool) = {o}
        y := true
    {c}
                
    """)
        else:
            for x in range(len(obj.reactants)):
                count2 += 1
                if count2 == 1:
                    ivyFile.write(f"(reactant{count2}:updater.num")
                    #print(len(obj.reactants))
                else:
                    ivyFile.write(f",reactant{count2}:updater.num")
                if count2 == len(obj.reactants):
                    ivyFile.write(f""") returns(y:bool) = {o}
        if """)
                    count3 = 0
                    for x in range(count2):
                        count3 += 1
                        if count3 == 1:
                            ivyFile.write(f"reactant{count3} >= 1")
                        else:
                            ivyFile.write(f" & reactant{count3} >= 1")
                    ivyFile.write(f""" {o}
            y := true
        {c}
        else {o}
            y := false
        {c}
    {c}
        
    """)

ivyFile.write("\n}\n\n")

ivyFile.write("object inspector = {\n\t")
count = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"action check_guard_r{count}")
        if len(obj.reactants) == 0:
            ivyFile.write(f"""
    before check_guard_r{count} {o}
        assert true
    {c}
    
    """)
        else:
            count2 = 0
            for x in range(len(obj.reactants)):
                count2 += 1
                if count2 == 1:
                    ivyFile.write(f"(reactant{count2}:updater.num")
                else:
                    ivyFile.write(f",reactant{count2}:updater.num")
                if count2 == len(obj.reactants):
                    ivyFile.write(f""")\n\tbefore check_guard_r{count} {o}
        assert """)
                    count3 = 0
                    for x in range(len(obj.reactants)):
                        count3 += 1
                        if count3 == 1:
                            ivyFile.write(f"reactant{count3} >= {obj.reactantsNum[count3-1]}")
                        else:
                            ivyFile.write(f" & reactant{count3} >= {obj.reactantsNum[count3-1]}")
                        if count3 == len(obj.reactants):
                            ivyFile.write("\n\t}\n\n\t")
        
ivyFile.write("\n}")

ivyFile.write("\n\nobject selector = {\n\t")

count = 0

for obj in reactions:
    count = count + 1
    if obj.priority != -1:
        ivyFile.write(f"individual r{count}_exec : updater.exec_var\n\t")

ivyFile.write("\n\t")
count = 0

for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"individual r{count}_rate : updater.exec_var\n\t")

ivyFile.write("\n\t")

count = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"individual r{count}_count : updater.exec_var\n\t")

ivyFile.write("\n\t")

count = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"individual r{count}_count_rate : updater.exec_var\n\t")

ivyFile.write("\n\t")

count = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"individual r{count}_stage : updater.exec_stage\n\t")

ivyFile.write("\n\n\tafter init {\n\t\t")

count = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"r{count}_count := 0;\n\t\t")

ivyFile.write("\n\n\t\t")
count = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"r{count}_count_rate := 4;\n\t\t")

ivyFile.write("\n\t}\n\n\t")

count = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"""action execute_r{count} returns(y:bool) = {o}
        r{count}_exec := r{count}_exec + 1;
        if r{count}_exec >= r{count}_rate {o}
            y := true;
            r{count}_exec := 0;
            r{count}_count := r{count}_count + 1
        {c}
        else {o}
            y := false
        {c};
        if r{count}_count >= r{count}_count_rate {o}
            r{count}_stage := r{count}_stage + 1;
            r{count}_count := 0
        {c};
        """)
        ivyFile.write(f"""if r{count}_stage = 0 {o}
            r{count}_count_rate := 4;
            r{count}_rate := {(obj.priority * 2) + 1}
        {c}
        else if r{count}_stage = 1 {o}
            r{count}_count_rate := 3;
            r{count}_rate := {(obj.priority * 3) + 1}
        {c}
        else if r{count}_stage = 2 {o}
            r{count}_count_rate := 5;
            r{count}_rate := {(obj.priority * 1) + 1}
        {c}
        else if r{count}_stage = 3 {o}
            r{count}_count_rate := 4;
            r{count}_rate := {(obj.priority * 2) + 1}
        {c}
        else if r{count}_stage = 4 {o}
            r{count}_count_rate := 4;
            r{count}_rate := {(obj.priority * 1) + 1}
        {c}
        else if r{count}_stage = 5 {o}
            r{count}_count_rate := 5;
            r{count}_rate := {(obj.priority * 3) + 1}
        {c}
        else if r{count}_stage = 6 {o}
            r{count}_count_rate := 3;
            r{count}_rate := {(obj.priority * 2) + 1}
        {c}
        else if r{count}_stage = 7 {o}
            r{count}_count_rate := 4;
            r{count}_rate := {(obj.priority * 1) + 1}
        {c}
        else {o}
            r{count}_stage := 0
        {c}
    {c}

    """)

ivyFile.write("\n}\n")

ivyFile.write("\nobject protocol = {\n\n\ttype 2bit\n\tinterpret 2bit -> bv[1]\n\tindividual idle : 2bit\n\n")

for obj in spec:
    ivyFile.write(f"\tindividual r_{obj} : updater.num\n")

count = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"\tindividual r{count}_executions : updater.num\n")

ivyFile.write("\n\tafter init {\n\t\t")

for obj in speciesList:
    ivyFile.write(f"r_{obj.name} := {obj.value};\n\t\t")

count = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"r{count}_executions := 0;\n\t\t")

ivyFile.write("idle := 0\n\t}\n\n\t")

count = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"""action update_r{count} =  {o}
        if selector.execute_r{count}""")
        if len(obj.reactants) == 0:
            ivyFile.write(f" {o}\n\t\t\tcall inspector.check_guard_r{count}")
            if len(obj.products) == 0:
                ivyFile.write(f";\n\t\t\tr{count}_executions := updater.incr(r{count}_executions)")
            else:
                count2 = 0
                for x in range(len(obj.products)):
                    count2 += 1
                    for y in range(obj.productsNum[count2-1]):
                        ivyFile.write(f";\n\t\t\tr_{obj.products[count2-1]} := updater.incr(r_{obj.products[count2-1]})")
                ivyFile.write(f";\n\t\t\tr{count}_executions := updater.incr(r{count}_executions)")
            if obj.priority == 0:
                ivyFile.write(f";\n\t\t\tif r_{targetSpecies} {equality}{targetNum} {o}\n\t\t\t\tcall goal.achieved(r_{targetSpecies})\n\t\t\t{c}\n\t\t")
            else:
                ivyFile.write("\n\t\t")
            ivyFile.write("}\n\t}\n\n\t")
        else:
            count2 = 0
            for x in range(len(obj.reactants)):
                count2 += 1
                if count2 == 1:
                    ivyFile.write(f" {o}\n\t\t\tcall inspector.check_guard_r{count}(r_{obj.reactants[count2-1]}")
                else:
                    ivyFile.write(f",r_{obj.reactants[count2-1]}")
                if len(obj.reactants) == count2:
                    ivyFile.write(")")
                    count3 = 0
                    for x in range(len(obj.reactants)):
                        count3 += 1
                        for y in range(obj.reactantsNum[count2-1]):
                            ivyFile.write(f";\n\t\t\tr_{obj.reactants[count3-1]} := updater.decr(r_{obj.reactants[count3-1]})")
            if len(obj.products) == 0:
                ivyFile.write(f";\n\t\t\tr{count}_executions := updater.incr(r{count}_executions)")
            else:
                count4 = 0
                for z in range(len(obj.products)):
                    count4 += 1
                    for t in range(obj.productsNum[count4-1]):
                        ivyFile.write(f";\n\t\t\tr_{obj.products[count4-1]} := updater.incr(r_{obj.products[count4-1]})")
                ivyFile.write(f";\n\t\t\tr{count}_executions := updater.incr(r{count}_executions)")
            if obj.priority == 0:
                ivyFile.write(f";\n\t\t\tif r_{targetSpecies} {equality}{targetNum} {o}\n\t\t\t\tcall goal.achieved(r_{targetSpecies})\n\t\t\t{c}\n\t\t")
            else:
                ivyFile.write("\n\t\t")
            ivyFile.write("}\n\t}\n\n\t")

ivyFile.write("\n\n\taction idling = {}\n\n\t")

ivyFile.write("\n\n\taction fail_test = {}\n\n\t")

count = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"before update_r{count}")
        if len(obj.reactants) == 0:
            ivyFile.write(f" {o}\n\t\tassert idle = 0;\n\t\tassert enabled_checker.is_enabled_r{count}")
            ivyFile.write("\n\t}\n\t")
        else:
            ivyFile.write(f" {o}\n\t\tassert idle = 0;\n\t\tassert enabled_checker.is_enabled_r{count}(")
            count2 = 0
            for x in range(len(obj.reactants)):
                count2 += 1
                if count2 == 1:
                    ivyFile.write(f"r_{obj.reactants[count2-1]}")
                else:
                    ivyFile.write(f",r_{obj.reactants[count2-1]}")
                if count2 == len(obj.reactants):
                    ivyFile.write(")\n\t}\n\t")


ivyFile.write("\n\n\tbefore idling {\n\t\tassert idle = 1\n\t}\n")

ivyFile.write("\n\n\tafter idling {\n\t\tassert idle = 0\n\t}\n\n") #this causes assertion failure for the first run

ivyFile.write("\n\n\tbefore fail_test {\n\t\tassert idle = 0;\n\t\tassert (")

count = 0
count1 = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        count1 += 1
        if count1 != 1:
            ivyFile.write(" & ")
        if len(obj.reactants) == 0:
            ivyFile.write(f"enabled_checker.is_enabled_r{count} = false")
        else:
            count2 = 0
            for x in range(len(obj.reactants)):
                count2 += 1
                if count2 == 1:
                    ivyFile.write(f"enabled_checker.is_enabled_r{count}(r_{obj.reactants[count2-1]}")
                else:
                    ivyFile.write(f",r_{obj.reactants[count2-1]}")
                if count2 == len(obj.reactants):
                    ivyFile.write(") = false")
    if count == numOfReactions:
        ivyFile.write(")\n\t}")
    
ivyFile.write("\n\n\tafter fail_test {\n\t\tassert false\n\t}\n\n}\n")

ivyFile.write("\nexport protocol.fail_test\n")

count = 0
for obj in reactions:
    count = count + 1
    if obj.priority != -1:
        ivyFile.write("export protocol.update_r")
        ivyFile.write(str(count))
        ivyFile.write("\n")


ivyFile.write("export protocol.idling\nimport goal.achieved\n")

count = 0
for obj in reactions:
    count= count + 1
    if obj.priority != -1:
        ivyFile.write("import inspector.check_guard_r")
        ivyFile.write(str(count))
        ivyFile.write("\n")

ivyFile.write("\nisolate iso_proto = protocol with enabled_checker, updater, goal, selector, inspector")

ivyFile.close()        #ivy model complete

ivy_to_cpp_command = subprocess.Popen(["ivy_to_cpp", "isolate=iso_proto", "target=test", "build=true", Options.firstIvyModel])
ivy_to_cpp_command.wait()

print("starting to run initial test")
os.system(f"./{Options.firstIvyModelName} seed=367 iters=10000 runs=1 >{Options.firstTestResult}")
print("finished initial test") #test is run and results are stored in test_v2.txt

first_iters = 0

with open(Options.firstTestResult, "r") as f: #The amount of iters needed to reach the goal in the first example is recorded
    count = 0
    while True:
        line = f.readline()
        if not line:
            break
        if line[0] == ">":
            if line[11:17] != "idling":
                first_iters += 1
        if line[11:20] == "fail_test":
            print("Error occurred during randomized testing!") #We need to decide what we want to do at this point

if first_iters >= 10000:
    print("Trace not found to specified target from randomized testing")
    exit(1)

print("The iters recorded for this initial example is", first_iters)

######
ivyFile = open(Options.secondIvyModel, "w") #an ivy model for the CRN is made without assertion failure at first idling action

ivyFile.write(f"""#lang ivy 1.7

object updater = {o}
    type num
    interpret num -> bv[10]
    type exec_var
    interpret exec_var -> bv[8]
    type exec_stage
    interpret exec_stage -> bv[3]
    
    action incr(x:num) returns(y:num) = {o}
        y := x + 1
    {c}
    
    action decr(x:num) returns(y:num) = {o}
        y := x - 1
    {c}
{c}

""")

if upOrDown == "1":
    equality = ">= "
elif upOrDown == "2":
    equality = "> "
elif upOrDown == "3":
    equality = "<= "
elif upOrDown == "4":
    equality = "< "

ivyFile.write(f"""
object goal = {o}
    action achieved(v:updater.num)
    object spec = {o}
        before achieved {o}
            assert v {equality} {targetNum};
            protocol.idle := 1
        {c}
    {c}
{c}

""")

ivyFile.write("object enabled_checker = {\n\n\t")

count = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"action is_enabled_r{count}")
        count2 = 0 
        if (len(reactions[count-1].reactants) == 0):
            ivyFile.write(f""" returns(y:bool) = {o}
        y := true
    {c}
                
    """)
        else:
            for x in range(len(obj.reactants)):
                count2 += 1
                if count2 == 1:
                    ivyFile.write(f"(reactant{count2}:updater.num")
                else:
                    ivyFile.write(f",reactant{count2}:updater.num")
                if count2 == len(obj.reactants):
                    ivyFile.write(f""") returns(y:bool) = {o}
        if """)
                    count3 = 0
                    for x in range(count2):
                        count3 += 1
                        if count3 == 1:
                            ivyFile.write(f"reactant{count3} >= 1")
                        else:
                            ivyFile.write(f" & reactant{count3} >= 1")
                    ivyFile.write(f""" {o}
            y := true
        {c}
        else {o}
            y := false
        {c}
    {c}
        
    """)

ivyFile.write("\n}\n\n")

ivyFile.write("object inspector = {\n\t")
count = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"action check_guard_r{count}")
        if len(obj.reactants) == 0:
            ivyFile.write(f"""
    before check_guard_r{count} {o}
        assert true
    {c}
    
    """)
        else:
            count2 = 0
            for x in range(len(obj.reactants)):
                count2 += 1
                if count2 == 1:
                    ivyFile.write(f"(reactant{count2}:updater.num")
                else:
                    ivyFile.write(f",reactant{count2}:updater.num")
                if count2 == len(obj.reactants):
                    ivyFile.write(f""")\n\tbefore check_guard_r{count} {o}
        assert """)
                    count3 = 0
                    for x in range(len(obj.reactants)):
                        count3 += 1
                        if count3 == 1:
                            ivyFile.write(f"reactant{count3} >= {obj.reactantsNum[count3-1]}")
                        else:
                            ivyFile.write(f" & reactant{count3} >= {obj.reactantsNum[count3-1]}")
                        if count3 == len(obj.reactants):
                            ivyFile.write("\n\t}\n\n\t")
        
ivyFile.write("\n}")

ivyFile.write("\n\nobject selector = {\n\t")

count = 0

for obj in reactions:
    count = count + 1
    if obj.priority != -1:
        ivyFile.write(f"individual r{count}_exec : updater.exec_var\n\t")

ivyFile.write("\n\t")
count = 0

for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"individual r{count}_rate : updater.exec_var\n\t")

ivyFile.write("\n\t")

count = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"individual r{count}_count : updater.exec_var\n\t")

ivyFile.write("\n\t")

count = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"individual r{count}_count_rate : updater.exec_var\n\t")

ivyFile.write("\n\t")

count = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"individual r{count}_stage : updater.exec_stage\n\t")

ivyFile.write("\n\n\tafter init {\n\t\t")

count = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"r{count}_count := 0;\n\t\t")

ivyFile.write("\n\n\t\t")
count = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"r{count}_count_rate := 4;\n\t\t")

ivyFile.write("\n\t}\n\n\t")

count = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"""action execute_r{count} returns(y:bool) = {o}
        r{count}_exec := r{count}_exec + 1;
        if r{count}_exec >= r{count}_rate {o}
            y := true;
            r{count}_exec := 0;
            r{count}_count := r{count}_count + 1
        {c}
        else {o}
            y := false
        {c};
        if r{count}_count >= r{count}_count_rate {o}
            r{count}_stage := r{count}_stage + 1;
            r{count}_count := 0
        {c};
        """)
        ivyFile.write(f"""if r{count}_stage = 0 {o}
            r{count}_count_rate := 4;
            r{count}_rate := {(obj.priority * 2) + 1}
        {c}
        else if r{count}_stage = 1 {o}
            r{count}_count_rate := 3;
            r{count}_rate := {(obj.priority * 3) + 1}
        {c}
        else if r{count}_stage = 2 {o}
            r{count}_count_rate := 5;
            r{count}_rate := {(obj.priority * 1) + 1}
        {c}
        else if r{count}_stage = 3 {o}
            r{count}_count_rate := 4;
            r{count}_rate := {(obj.priority * 2) + 1}
        {c}
        else if r{count}_stage = 4 {o}
            r{count}_count_rate := 4;
            r{count}_rate := {(obj.priority * 1) + 1}
        {c}
        else if r{count}_stage = 5 {o}
            r{count}_count_rate := 5;
            r{count}_rate := {(obj.priority * 3) + 1}
        {c}
        else if r{count}_stage = 6 {o}
            r{count}_count_rate := 3;
            r{count}_rate := {(obj.priority * 2) + 1}
        {c}
        else if r{count}_stage = 7 {o}
            r{count}_count_rate := 4;
            r{count}_rate := {(obj.priority * 1) + 1}
        {c}
        else {o}
            r{count}_stage := 0
        {c}
    {c}

    """)

ivyFile.write("\n}\n")

ivyFile.write("\nobject protocol = {\n\n\ttype 2bit\n\tinterpret 2bit -> bv[1]\n\tindividual idle : 2bit\n\n")

for obj in spec:
    ivyFile.write(f"\tindividual r_{obj} : updater.num\n")

count = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"\tindividual r{count}_executions : updater.num\n")

ivyFile.write("\n\tafter init {\n\t\t")

for obj in speciesList:
    ivyFile.write(f"r_{obj.name} := {obj.value};\n\t\t")

count = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"r{count}_executions := 0;\n\t\t")


ivyFile.write("idle := 0\n\t}\n\n\t")

count = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"""action update_r{count} =  {o}
        if selector.execute_r{count}""")
        if len(obj.reactants) == 0:
            ivyFile.write(f" {o}\n\t\t\tcall inspector.check_guard_r{count}")
            if len(obj.products) == 0:
                ivyFile.write(f";\n\t\t\tr{count}_executions := updater.incr(r{count}_executions)")
            else:
                count2 = 0
                for x in range(len(obj.products)):
                    count2 += 1
                    for y in range(obj.productsNum[count2-1]):
                        ivyFile.write(f";\n\t\t\tr_{obj.products[count2-1]} := updater.incr(r_{obj.products[count2-1]})")
                ivyFile.write(f";\n\t\t\tr{count}_executions := updater.incr(r{count}_executions)")
            if obj.priority == 0:
                ivyFile.write(f";\n\t\t\tif r_{targetSpecies} {equality}{targetNum} {o}\n\t\t\t\tcall goal.achieved(r_{targetSpecies})\n\t\t\t{c}\n\t\t")
            else:
                ivyFile.write("\n\t\t")
            ivyFile.write("}\n\t}\n\n\t")
        else:
            count2 = 0
            for x in range(len(obj.reactants)):
                count2 += 1
                if count2 == 1:
                    ivyFile.write(f" {o}\n\t\t\tcall inspector.check_guard_r{count}(r_{obj.reactants[count2-1]}")
                else:
                    ivyFile.write(f",r_{obj.reactants[count2-1]}")
                if len(obj.reactants) == count2:
                    ivyFile.write(")")
                    count3 = 0
                    for x in range(len(obj.reactants)):
                        count3 += 1
                        for y in range(obj.reactantsNum[count2-1]):
                            ivyFile.write(f";\n\t\t\tr_{obj.reactants[count3-1]} := updater.decr(r_{obj.reactants[count3-1]})")
            if len(obj.products) == 0:
                ivyFile.write(f";\n\t\t\tr{count}_executions := updater.incr(r{count}_executions)")
            else:
                count4 = 0
                for z in range(len(obj.products)):
                    count4 += 1
                    for t in range(obj.productsNum[count4-1]):
                        ivyFile.write(f";\n\t\t\tr_{obj.products[count4-1]} := updater.incr(r_{obj.products[count4-1]})")
                ivyFile.write(f";\n\t\t\tr{count}_executions := updater.incr(r{count}_executions)")
            if obj.priority == 0:
                ivyFile.write(f";\n\t\t\tif r_{targetSpecies} {equality}{targetNum} {o}\n\t\t\t\tcall goal.achieved(r_{targetSpecies})\n\t\t\t{c}\n\t\t")
            else:
                ivyFile.write("\n\t\t")
            ivyFile.write("}\n\t}\n\n\t")

ivyFile.write("\n\n\taction idling = {}\n\n\t")

ivyFile.write("\n\n\taction fail_test = {}\n\n\t")

count = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        ivyFile.write(f"before update_r{count}")
        if len(obj.reactants) == 0:
            ivyFile.write(f" {o}\n\t\tassert idle = 0;\n\t\tassert enabled_checker.is_enabled_r{count}")
            count1 = 0
            count2 = 0
            #if obj.priority != 0:
            for y in reactions:
                count1 += 1
                if y.priority == obj.priority:
                    count2 += 1
                    if count2 == 1:
                        ivyFile.write(f";\n\t\tassert (r{count1}_executions")
                    if count2 >= 2:
                        ivyFile.write(f" + r{count1}_executions")
                if count1 == numOfReactions:
                    ivyFile.write(") < ")
                    ivyFile.write(str(obj.executions))
            ivyFile.write("\n\t}\n\t")
        else:
            ivyFile.write(f" {o}\n\t\tassert idle = 0;\n\t\tassert enabled_checker.is_enabled_r{count}(")
            count3 = 0
            for x in range(len(obj.reactants)):
                count3 += 1
                if count3 == 1:
                    ivyFile.write(f"r_{obj.reactants[count3-1]}")
                else:
                    ivyFile.write(f",r_{obj.reactants[count3-1]}")
                if count3 == len(obj.reactants):
                    ivyFile.write(f")")
            count1 = 0
            count2 = 0
            for y in reactions:
                count1 += 1
                if y.priority == obj.priority:
                    count2 += 1
                    if count2 == 1:
                        ivyFile.write(f";\n\t\tassert (r{count1}_executions")
                    if count2 >= 2:
                        ivyFile.write(f" + r{count1}_executions")
                if count1 == numOfReactions:
                    ivyFile.write(") < ")
                    ###
                    ivyFile.write(str(obj.executions))
                    ###
            ivyFile.write("\n\t}\n\t")


ivyFile.write("\n\n\tbefore idling {\n\t\tassert idle = 1\n\t}\n")

ivyFile.write("\n\n\tbefore fail_test {\n\t\tassert idle = 0;\n\t\tassert (")

count = 0
count1 = 0
count2 = 0
count3 = 0
for obj in reactions:
    count += 1
    if obj.priority != -1:
        count1 += 1
        if count1 != 1:
            ivyFile.write(" & ")
        if len(obj.reactants) == 0:
            ivyFile.write(f"enabled_checker.is_enabled_r{count} = false")
        else:
            count4 = 0
            for x in range(len(obj.reactants)):
                count4 += 1
                if count4 == 1:
                    ivyFile.write(f"enabled_checker.is_enabled_r{count}(r_{obj.reactants[count4-1]}")
                else:
                    ivyFile.write(f",r_{obj.reactants[count4-1]}")
                if count4 == len(obj.reactants):
                    ivyFile.write(") = false")
    if count == numOfReactions:
        ivyFile.write(") | (((")
        for y in reactions:     ####This only works CRNs that don't need to go past the 3rd tier
            count2 += 1
            if y.priority == 0:
                count3 += 1
                if count3 == 1:
                    ivyFile.write(f"r{count2}_executions")
                if count3 >= 2:
                    ivyFile.write(f" + r{count2}_executions")
            if count2 == numOfReactions:
                ivyFile.write(") >= ")
                ivyFile.write(f"{y.executions})")
        count2 = 0
        count3 = 0
        for y in reactions:
            count2 += 1
            if y.priority == 1:
                count3 += 1
                if count3 == 1:
                    ivyFile.write(f" & ((r{count2}_executions")
                if count3 >= 2:
                    ivyFile.write(f" + r{count2}_executions")
            if count2 == numOfReactions and count3 > 0:
                ivyFile.write(") >= ")
                ivyFile.write(f"{y.executions})")
        count2 = 0
        count3 = 0
        for y in reactions:
            count2 += 1
            if y.priority == 2:
                count3 += 1
                if count3 == 1:
                    ivyFile.write(f" & ((r{count2}_executions")
                if count3 >= 2:
                    ivyFile.write(f" + r{count2}_executions")
            if count2 == numOfReactions and count3 > 0:
                ivyFile.write(") >= ")
                ivyFile.write(f"{y.executions})")
        count2 = 0
        count3 = 0
        for y in reactions:
            count2 += 1
            if y.priority == 3:
                count3 += 1
                if count3 == 1:
                    ivyFile.write(f" & ((r{count2}_executions")
                if count3 >= 2:
                    ivyFile.write(f" + r{count2}_executions")
            if count2 == numOfReactions and count3 > 0:
                ivyFile.write(") >= ")
                ivyFile.write(f"{y.executions})")
        ivyFile.write(f")\n\t{c}\n\n")
        
    
ivyFile.write("\n\n\tafter fail_test {\n\t\tassert false\n\t}\n\n}\n")

ivyFile.write("\nexport protocol.fail_test\n")

count = 0
for obj in reactions:
    count = count + 1
    if obj.priority != -1:
        ivyFile.write("export protocol.update_r")
        ivyFile.write(str(count))
        ivyFile.write("\n")


ivyFile.write("export protocol.idling\nimport goal.achieved\n")

count = 0
for obj in reactions:
    count= count + 1
    if obj.priority != -1:
        ivyFile.write("import inspector.check_guard_r")
        ivyFile.write(str(count))
        ivyFile.write("\n")

ivyFile.write("\nisolate iso_proto = protocol with enabled_checker, updater, goal, selector, inspector")

ivyFile.close()        #ivy model complete

ivy_to_cpp_command = subprocess.Popen(["ivy_to_cpp", "isolate=iso_proto", "target=test", "build=true", Options.secondIvyModel])
ivy_to_cpp_command.wait()

runswanted = input("How many traces do you want to the target specified? (Type an integer greater than 0): ") #Amount of traces desired is recorded

print("starting to run rest of tests")
firsthalf = f"./{Options.secondIvyModelName} iters="
middle = str(first_iters*1.25)
middle2 = " runs="
secondhalf = f" >{Options.secondTestResult}"
firstpart = firsthalf + middle + middle2 + runswanted
fullstring = firstpart + secondhalf
print(fullstring)
os.system(fullstring)
print("finished randomized testing")#More tests run with 1.25 times the amount of iters needed for the first test, for the specified number of traces wanted by the user

reaction_exec_count = []

for x in range(numOfReactions):
    reaction_exec_count.append(0)


iters = 0

transitions = 0

tracelist = open(Options.traceList, "w") #The traces by themselves are recorded in 'trace_list.txt'

transitionmap = open(Options.reactionList, "w")  #The traces and additional information is stored in 'reactoin_list.txt'

count3 = 0

with open(Options.secondTestResult, "r") as f:
    count = 0
    while True:
        count3 += 1
        line = f.readline()
        if not line:
            break
        if iters == 0:
            transitionmap.write("Run ")
            transitionmap.write(str(count+1))
            transitionmap.write(":\n\n")
        if line[0] == ">":
            if line[11:17] != "idling":
                iters += 1
                if iters == math.floor(first_iters * 1.25):
                    print("Error!\tRun", count+1, "did not reach the target state\n")
                if line[11:20] == "fail_test":
                    print("Error occurred during randomized testing!")
                    exit(1)
        if line[0] == "<":
            if line[2] == "i":
                transitions += 1
                transitionmap.write(line[24:26])
                transitionmap.write("\t")
                tracelist.write(line[24:26])
                tracelist.write("\t")
                reaction_exec_count[int(line[25])-1] += 1
        if line[0] == "t":
            count = count + 1
            transitionmap.write("\n\nRun ")
            transitionmap.write(str(count))
            transitionmap.write(" information\n\nIterations before idling was reached: ")
            transitionmap.write(str(iters))
            transitionmap.write("\nNumber of transitions: ")
            transitionmap.write(str(transitions))
            count2 = 0
            for x in range(numOfReactions):
                transitionmap.write("\nr")
                transitionmap.write(str(x+1))
                transitionmap.write("executions: ")
                transitionmap.write(str(reaction_exec_count[x]))
                reaction_exec_count[x] = 0
            transitionmap.write("\n\n\n\n")
            tracelist.write("\n")
            iters = 0
            transitions = 0

print(f"\nThe traces recorded and the information on those traces are stored in '{Options.traceList}'")
print(f"\nThe traces by themselves are found in '{Options.reactionList}'")
transitionmap.close()

tracelist.close()

Totaltran = 0
Totaliter = 0
Total = []

for x in range(numOfReactions):
    Total.append(0)

Totaliterlist = []
Totaltranlist = []

iterations = []
for x in range(numOfReactions):
    iterations.append([])

with open(Options.reactionList, "r") as f:
    count = 0
    while True:
        line = f.readline()
        if not line:
            break
        if line[0] == "N":
            Totaltran = Totaltran + int(line[23:26])
            Totaltranlist.append(int(line[23:26]))
        elif line[0] == "I":
            Totaliter = Totaliter + int(line[38:41])
            Totaliterlist.append(int(line[38:41]))
        for x in range(numOfReactions):
            stringnum = str(x+1)
            stringreact = "r" + stringnum
            if line[0:2] == stringreact:
                if len(line) == 17:
                    Total[x] = Total[x] + int(line[14:16])
                    iterations[x].append(int(line[14:16]))
                elif len(line) == 16:
                    Total[x] = Total[x] + int(line[14])
                    iterations[x].append(int(line[14]))

print("\n\nAverage number of transitions in a trace is:", Totaltran/int(runswanted))
print("\nThe biggest number of transitions recorded in a trace is:", max(Totaltranlist))
print("\nThe smallest number of transitions recorded in a trace is:", min(Totaltranlist))
print("\n\nAverage number of iterations needed in a trace is:", Totaliter/int(runswanted))
print("\nThe biggest number of iterations needed for a trace was:", max(Totaliterlist))
print("\nThe smallest number of iterations needed for a trace was:", min(Totaliterlist))

for x in range(numOfReactions):
    print("\n\nAverage number of reaction", x+1, "executions in a trace is:", Total[x]/int(runswanted))
    print("\nThe biggest number of reaction", x+1, "executions recorded in a trace is:", max(iterations[x]))
    print("\nThe smallest number of reaction", x+1, "executions recorded in a trace is:", min(iterations[x]))
