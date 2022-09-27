#!/bin/sh

# Rather than having four shell scripts with basically the same thing in them, you could just pass
# in the model size via a parameter

set -e

ragrun() {
	export MODEL_SIZE=$1
	echo "Running Ragtimer for model size ${MODEL_SIZE}.."

	echo -n "Testing on the Single-species models..."
	cp ../models/SingleSpecies/2reaction.ragtimer ../model.ragtimer
	cp ../models/SingleSpecies/model.sm ../model.sm
	cp ../models/SingleSpecies/model.csl ../model.csl
	/usr/bin/time -o "../results/ragtimer/2reaction_${MODEL_SIZE}time.txt" python3 main.py $MODEL_SIZE loose &> "../results/ragtimer/2reaction_${MODEL_SIZE}.txt"
	echo "done."

	echo -n "Testing on the Kuwahara Enzyme models..."
	cp ../models/KuwaharaEnzyme/6reaction.ragtimer ../model.ragtimer
	cp ../models/KuwaharaEnzyme/model.sm ../model.sm
	cp ../models/KuwaharaEnzyme/model.csl ../model.csl
	/usr/bin/time -o "../results/ragtimer/6reaction_${MODEL_SIZE}time.txt" python3 main.py ${MODEL_SIZE} loose &> "../results/ragtimer/6reaction_${MODEL_SIZE}.txt"
	echo "done."

	echo -n "Testing on Donovan Yeast Polarization models..."
	cp ../models/DonovanYeastPolarization/8reaction_input_adding.txt ../model.ragtimer
	cp ../models/DonovanYeastPolarization/model.sm ../model.sm
	cp ../models/DonovanYeastPolarization/model.csl ../model.csl
	/usr/bin/time -o "../results/ragtimer/8reaction_${MODEL_SIZE}time.txt" python3 main.py ${MODEL_SIZE} &> "../results/ragtimer/8reaction_${MODEL_SIZE}.txt"
	echo "done."

}

ragrun $1
