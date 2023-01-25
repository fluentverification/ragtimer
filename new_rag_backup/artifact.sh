make

# echo "FIRST TEST BEGINS"
# cp ../models/SingleSpecies/2reaction.ragtimer ../model.ragtimer
# cp ../models/SingleSpecies/model.sm ../model.sm
# cp ../models/SingleSpecies/model.csl ../model.csl
# /usr/bin/time -o ../results/ragtimer/2reaction_1time.txt python3 main.py 1 loose &> ../results/ragtimer/2reaction_1.txt

# echo "SECOND TEST BEGINS"
# cp ../models/KuwaharaEnzyme/6reaction.ragtimer ../model.ragtimer
# cp ../models/KuwaharaEnzyme/model.sm ../model.sm
# cp ../models/KuwaharaEnzyme/model.csl ../model.csl
# /usr/bin/time -o ../results/ragtimer/6reaction_1time.txt python3 main.py 1 loose &> ../results/ragtimer/6reaction_1.txt

echo "THIRD TEST BEGINS"
cp ../models/DonovanYeastPolarization/8reaction_input_adding.txt ../model.ragtimer
cp ../models/DonovanYeastPolarization/model.sm ../model.sm
cp ../models/DonovanYeastPolarization/model.csl ../model.csl
/usr/bin/time -o ../results/ragtimer/8reaction_1time.txt python3 main.py 10 &> ../results/ragtimer/8reaction_1.txt

echo "FOURTH TEST BEGINS"
cp ../models/SimplifiedMotilityRegulation/SimplifiedMotilityRegulation.ragtimer ../model.ragtimer
cp ../models/SimplifiedMotilityRegulation/motility.sm ../model.sm
cp ../models/SimplifiedMotilityRegulation/model.csl ../model.csl
# python3 main.py 1 > printPrefixesTrace.txt
/usr/bin/time -o ../results/ragtimer/motility_1time.txt python3 main.py 10 &> ../results/ragtimer/motility_1.txt

echo "TESTS COMPLETED"
