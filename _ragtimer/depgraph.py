
'''
Reaction objects store details about reactions.
'''

import math
import sys
from xmlrpc.client import MAXINT


class Reaction:

	# Initialize a basic, empty reaction
	def __init__(self, name, rate):
		self.name = name
		self.rate = rate
		self.reactants = []
		self.products = []
		self.dependsOn = []
		self.dependCount = []
		self.executions = 0
		self.tier = -1
		self.useless = False
		self.enabledToExecute = 0
		self.parents = []
	
	# Add a reactant to the reaction
	def addReactant(self, reactant):
		self.reactants.append(reactant)

	# Add a product to the reaction
	def addProduct(self, product):
		self.products.append(product)

	def check_usefulness(self, new_initials, new_targets, add_or_sub, parent):

		# print("Checking usefulness on", str(self.name))
		# print(new_initials)
		# print(new_targets)
		# print(len(self.dependsOn))
		infinite_dependsOn = True
		for d in self.dependCount:
			if d != 0:
				infinite_dependsOn = False
		if infinite_dependsOn:
			for i in range(len(new_initials)):
				deltaR = new_targets[i] - new_initials[i]
				# print("deltaR",deltaR)
				if ((add_or_sub[i] == 'a' and deltaR > 0) or (add_or_sub[i] == 's' and deltaR < 0)):
					# print("add_or_sub",add_or_sub[i])
					self.useless = True
					# print("Useless", str(self.name))
					if parent:
						parent.dependCount[len(parent.dependCount)-1] = 0
					self.tier = -1
					break
		else:
			self.useless = False

			# 	if new_targets[i] - 
			# for d in deltaTarget:
			# 	if d != 0:
			# 		self.useless = True

		# if self.useless and parent:
		# 	parent.dependCount[len(parent.dependCount)-1] = math.inf
		# 	parent.dependsOn.append("APPROPRIATE DEPENDENCY NOT FOUND. BRANCH UNREACHABLE")

	# Custom tostring function for reactions
	def __str__(self) -> str:
		r = "\n" + self.name
		r = r + " - Rate " + str(self.rate)
		r = r + "\nReactants " + str(self.reactants)
		r = r + "\nProducts " + str(self.products)
		r = r + "\nRequired Executions " + str(self.executions) 
		r = r + "\nEnabled Executions " + str(self.enabledToExecute) 
		r = r + "\nTier " + str(self.tier) + "\n"
		for d in range(len(self.dependsOn)):
			r = r + " - Depends On " + str(self.dependsOn[d].name) + " " + str(self.dependCount[d]) + " times\n"
		if self.useless:
			r = r + "USELESS"
		return r


# Found a bug. Recursion doesn't terminate when we have to deal with
# cases where every reaction has a dependency.

def printPrefixes(filename, path, reaction, paths, depth=0):
	path = reaction.name + "\t" + path
	print(path.replace("\t", " "))
	# input("-")
	# if (len(reaction.dependsOn) == 0 or reaction.name in path.split('\t')):
	if (len(reaction.dependsOn) == 0):
		with open(filename, 'a') as f:
			paths.append(path)
			# print("End of Path")
			# f.write("_PREFIX_\t" + path + "\n")
			return
	for r in reaction.dependsOn:
		if r.tier > -1:
			# print("printPrefixes", depth)
			printPrefixes(filename, path, r, paths, depth+1)



'''
Recursive graph building function
Inputs: recursion depth, reaction array, chemical name array, initial values, 
	target values, reaction history, and parent reaction
Output: None
'''

def buildGraph(recdepth, reactions, chemicals, initials, targets, parent, add_or_sub, printing=True):
	
	# print(add_or_sub)

	deltaTarget = []
	needChems = []
	needChemQty = []
	numChems = len(chemicals)

	# Find difference between current (initial or modified initial) state and target
	for i in range(numChems):
		if targets[i] > -1:
			deltaR = targets[i] - initials[i]
			# print(deltaR)
			if not ((add_or_sub[i] == 'a' and deltaR > 0) or (add_or_sub[i] == 's' and deltaR < 0)):
				deltaR = 0
			# 	deltaTarget.append(deltaR)
			# elif add_or_sub[i] == 's' and deltaR < 0:
			# else:
			deltaTarget.append(deltaR)
			needChems.append(chemicals[i])
			needChemQty.append(deltaR)
		else:
			deltaTarget.append(0)

	if printing:
		print("chemicals   \t" + str(chemicals))
		print("deltaTarget \t" + str(deltaTarget))
		print("add/subtract\t" + str(add_or_sub))

	needReactions = []

	# Figure out what reactions we need to generate/consume chemicals
	for c in range(len(needChems)):
		for r in range(len(reactions)):
			if needChemQty[c] > 0:
				if needChems[c] in reactions[r].products:
					needReactions.append(reactions[r])
					if printing:
						print("Check reaction", reactions[r].name, "to obtain", needChems[c])
			elif needChemQty[c] < 0:
				if needChems[c] in reactions[r].reactants:
					needReactions.append(reactions[r])
					if printing:
						print("Check reaction", reactions[r].name, "to reduce", needChems[c])

	# For every required reaction
	for r in needReactions:
		
		if printing:
			# Print user-readable information
			print()
			print(50*"-")
			if parent:
				print("TIER", recdepth, "Checking", r.name,"From parent",parent.name)
			else:
				print("TIER", recdepth, "Checking", r.name,"From parent",parent)
			print(50*"-")
			print()
			print("List of Chemicals    \t",chemicals)
			print("Current Initial State\t",initials)
			print("Current Target State \t",targets)
			print("Delta Target-Initial \t",deltaTarget)
			print("Chemicals Required   \t",needChems)
			print("In these quantities  \t",needChemQty)

		# Check for cycles and alert user
		if parent:
			if r.name in r.parents:
				if printing:
					print()
					print(r.name, "in reaction history. CYCLE DETECTED.\n")
					print()
				continue

		# Add current reaction to the reaction history (to look for cycles)
		# r_hist = []
		if parent:
			for rh in parent.parents:
				r.parents.append(rh)
		# r_hist.append(r.name)
		r.parents.append(r.name)

		# TODO: SIMPLIFY THE REACTION VECTOR SUCH THAT ALL WE DEAL WITH IS SUMS
		# TO DEAL WITH THE ANNOYING A -> A + A CASE.

		# Update required number of executions
		reqExec = 0
		for d in range(numChems):
			if printing:
				print("DELTATARGET", d, "= ", deltaTarget[d])
			if deltaTarget[d] > 0:
				for p in range(len(r.products)):
					if chemicals[d] in r.products[p]:
						if deltaTarget[d] > reqExec:
							reqExec = deltaTarget[d]
			elif deltaTarget[d] < 0:
				for p in range(len(r.reactants)):
					if chemicals[d] in r.reactants[p]:
						if -deltaTarget[d] > reqExec:
							reqExec = -deltaTarget[d]


		# Add to the required executions in the reaction object
		r.executions = r.executions + reqExec
		if printing:
			print("\nRequired Executions\t",r.executions)

		minEnabledExec = -1

		for c in range(numChems):
			if chemicals[c] in r.reactants:
				if initials[c] > 0:
					if minEnabledExec < 0:
						minEnabledExec = initials[c]
					else:
						if initials[c] < minEnabledExec:
							minEnabledExec = initials[c]
		
		if r.executions < minEnabledExec:
			minEnabledExec = r.executions

		r.enabledToExecute = minEnabledExec

		# Find out new "initial state" after reqExec executions
		new_initials = []
		for c in range(numChems):
			if chemicals[c] in r.products:
				new_initials.append(initials[c] + reqExec)
			else:
				new_initials.append(initials[c])

		# Find out new "target state" after reqExec executions
		new_targets = []
		for c in range(numChems):
			if chemicals[c] in r.reactants:
				new_targets.append(targets[c] + reqExec)
				if targets[c] == -1:
					new_targets[c] = new_targets[c] + 1
			else:
				new_targets.append(targets[c])
		
		if printing:
			# Print updated initial and target states, after r fires
			print("Initial After",reqExec,"Execs\t",new_initials)
			print("Target After",reqExec,"Execs\t",new_targets)

		# Update the parent to note this dependency
		if parent:
			parent.dependsOn.append(r)
			parent.dependCount.append(reqExec)

		# Set the tier to the recursion depth, if recursion depth is lower
		# or tier is not yet set
		if recdepth < r.tier or r.tier == -1:
			r.tier = recdepth

		if printing:
			# Print the updated reaction object
			print(r)

		# Recurse (find requirements for this reaction)
		buildGraph(recdepth+1, reactions, chemicals, new_initials, new_targets, r, add_or_sub, printing)

		r.check_usefulness(new_initials, new_targets, add_or_sub, parent)



'''
Master function to make the dependency graph
Input: File name (string) for reaction file in format from docs.md
Output: Array of reaction objects
'''

def makeDepGraph(infile, printing=True):
	
	# printing = True # TODO: Remove this line after debugging.
	
	chemicals = [] # Stores string names of chemicals
	initials = [] # Stores initial values of chemicals
	targets = [] # Stores target values of chemicals
	reactions = [] # Array of ALL reaction objects

	with open(infile, 'r') as inpt:
		
		# Read the line of chemical names
		line = inpt.readline().strip()
		if not line or line == "":
			print("ERROR IN DEPGRAPH! CANNOT READ FIRST LINE")
			quit()
		for chem in line.split():
			chemicals.append(str(chem).strip())

		# Read the line of initial values
		line = inpt.readline().strip()
		if not line or line == "":
			print("ERROR IN DEPGRAPH! CANNOT READ SECOND LINE")
			quit()
		for val in line.split():
			initials.append(int(val))

		# Read the line of target values (-1 is don't care)
		line = inpt.readline().strip()
		if not line or line == "":
			print("ERROR IN DEPGRAPH! CANNOT READ THIRD LINE")
			quit()
		for val in line.split():
			targets.append(int(val))
		
		# Parse the reactions, don't process anything yet
		while True:
			line = inpt.readline().strip()
			if not line or line == "":
				break
			sline = line.split()
			rname = sline[0]
			rate = float(sline[len(sline)-1])
			react = Reaction(rname, rate)
			switchToProducts = False
			for i in range(1,len(sline)-1):
				if ">" in sline[i]: # Check if we need to switch to reading products
					switchToProducts = True
				elif "0" in sline[i]: # Case for null reactant or product
					continue
				elif switchToProducts:
					react.addProduct(sline[i]) # Add products second
				else:
					react.addReactant(sline[i]) # Add reactants first
			reactions.append(react) # Add to reaction matrix

	# Uncomment to print reactions at initialization for debugging:
	# for react in reactions:
	# 	print(react)

	reaction_history = []

	if printing:
		print(50*"=")

	# Check if we should add or subtract for each chemical
	add_or_sub = []
	for i in range(len(initials)):
		if initials[i] > targets[i] and targets[i] >= 0:
			add_or_sub.append('s')
		else:
			add_or_sub.append('a')

	# Recursively find the necessary reactions
	buildGraph(0, reactions, chemicals, initials, targets, None, add_or_sub, printing)

	if printing:
		# Print the list of necessary reactions and their dependencies
		print()
		print(50*"=")
		print("NECESSARY REACTIONS")
		print(50*"=")

		# Print only necessary reactions:
		for react in reactions:
			if react.tier > -1:
				print(react)

		# Uncomment to print all reactions instead:
		# for react in reactions:
		# 	print(react)
		print(50*"=")

	unreachable = True
	for r in reactions:
		if r.tier >= 0 and not(r.useless):
			# print(r.name)
			unreachable = False
	
	if unreachable:
		# if printing:
		print("\nUNREACHABLE PROPERTY! Your probability is automatically zero!\n")
		return None
	
	return reactions


# Main function... use 8-reaction file if 
# no other input is provided
if __name__=="__main__":
	# makeDepGraph("8reaction_input.txt", True)
	makeDepGraph("../model.ragtimer", True)