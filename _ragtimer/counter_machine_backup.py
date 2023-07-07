import os
import reactions_v5
import depgraph
import prefix_parser
import sys
import subprocess
import math

class Options:
    # infile = "8reaction_input.txt"
    infile = "../model.ragtimer"
    traceList = "trace_list.txt"        #list of all of the traces by themselves
    reactionList = "reaction_list.txt"  #information on each of the traces is stored in this file

class State:
    def __init__(self, name):
        self.name = name
        self.transitions = []

class Transition:
    def __init__(self, name, source, destination):
        self.name = name
        self.source = source
        self.destination = destination
        self.updater = []


class Reaction:
    def __init__(self, priority, tierCount, executions):
        self.reactants = []
        self.reactantsNum = [] 
        self.products = []
        self.productsNum = []
        self.priority = priority
        self.tierCount = tierCount
        self.executions = executions

class Species:
    def __init__(self, value, name):
        self.value = value
        self.name = name
        
def counter(runswanted, reactions1, prefix, prefix_index, loose=False, printing=False):
    reactions = []  #stores all the reactions
    count = 0
    state_vector = []
    state_vector_value = []

    for obj in reactions1:  #transfers information from dependency graph to reactions in this file
        count += 1
        reactantsTemp = obj.reactants
        productsTemp = obj.products
        reactNumTemp = []
        prodNumTemp = []
        for x in range(len(reactantsTemp)):
            reactNumTemp.append(1)
        for x in range(len(productsTemp)):
            prodNumTemp.append(1)
        count1 = 0
        for y in reactions1:
            if y.tier == obj.tier:
                count1 += 1
        reactions.append(Reaction(obj.tier, count1, obj.executions))
        reactions[count-1].reactants = reactantsTemp
        reactions[count-1].products = productsTemp
        reactions[count-1].reactantsNum = reactNumTemp
        reactions[count-1].productsNum = prodNumTemp

    numOfReactions = count

    speciesList = []

    spec = []
    for s in prefix.varnames:   #add prefixes
        spec.append(s)
    
    if printing:
        print("prefix.varnames", prefix.varnames)
        print("SPEC", spec)
    ###

    chemicals = [] # Stores string names of chemicals
    initials = [] # Stores initial values of chemicals
    # initials = [] # Stores initial values of chemicals
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
        if len(prefix.values[prefix_index]) > 0:
            for v in prefix.values[prefix_index]:
                initials.append(int(v))
        else:
            if not line or line == "":
                print("ERROR! CANNOT READ SECOND LINE")
                quit()
            for val in line.split():
                initials.append(int(val))
        
        # print(initials)


        # Read the line of target values (-1 is don't care)
        line = inpt.readline().strip()
        if not line or line == "":
            print("ERROR! CANNOT READ THIRD LINE")
            quit()
        for val in line.split():
            targets.append(int(val))

    for obj in spec:    #add each species and it's initial values to speciesList
        count = 0
        for x in chemicals:
            count += 1
            if x == obj:
                speciesList.append(Species(initials[count-1], x))


    if printing:
        print("The initial values reported are:")

        for obj in speciesList: #initial values reported are displayed
            print(obj.name,"=",obj.value)

        print("\n")

    count = 0
    for obj in targets:     #define targetSpecies and targetNum
        count += 1
        if obj == -1:
            continue
        else:
            targetSpecies = chemicals[count - 1]  
            targetNum = obj

    for obj in speciesList: #upOrDown corresponds to the inequality for target, right now only >= and <= is supported, (= is also implied)
        if obj.name == targetSpecies:
            if int(obj.value) > int(targetNum):
                upOrDown = "3"
            elif int(obj.value) < int(targetNum):
                upOrDown = "1"
            elif int(obj.value) == int(targetNum):
                print("\nSpecified target is already achieved in target state, please start over")
                exit()

    count = 0
    for obj in reactions:   #set up state_vector and state_vector_value
        count += 1
        if obj.priority != -1:
            for x in obj.reactants:
                for y in speciesList:
                    if x == y.name:
                        if x not in state_vector:
                            state_vector.append(y.name)
                            state_vector_value.append(y.value)
            for x in obj.products:
                for y in speciesList:
                    if x == y.name:
                        if x not in state_vector:
                            state_vector.append(y.name)
                            state_vector_value.append(y.value)
            react = "R" + str(count) + "_exec1"
            state_vector.append(react)
            state_vector_value.append(obj.executions)

    print("state_vector", state_vector, "\n", "state_vector_value", state_vector_value, "\n")

    transit1 = []
    transit2 = []

    states = []
    states.append(State("q1", transit1))
    states.append(State("q2", transit2))

    #figure out how to systematically build the graph for this section below
    for x in state_vector:
        if x[2:5] == "exec":
            updater = []
            transit1.append(Transition(x, "q1", "q1", updater))

    print("done")
    if loose:  #if loose is specified add in one more reaction (figure out how to choose which reaction to add)
        loose = False