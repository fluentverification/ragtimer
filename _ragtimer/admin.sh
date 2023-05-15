cp ../models/SingleSpecies/2reaction.ragtimer ../model.ragtimer
cp ../models/SingleSpecies/model.sm ../model.sm
cp ../models/SingleSpecies/model.csl ../model.csl
/usr/bin/time -o ../results/ragtimer/2reaction_50000time.txt python3 main.py 50000 loose &> ../results/ragtimer/2reaction_50000.txt

cp ../models/KuwaharaEnzyme/6reaction.ragtimer ../model.ragtimer
cp ../models/KuwaharaEnzyme/model.sm ../model.sm
cp ../models/KuwaharaEnzyme/model.csl ../model.csl
/usr/bin/time -o ../results/ragtimer/6reaction_50000time.txt python3 main.py 50000 loose &> ../results/ragtimer/6reaction_50000.txt

cp ../models/DonovanYeastPolarization/8reaction_input_adding.txt ../model.ragtimer
cp ../models/DonovanYeastPolarization/model.sm ../model.sm
cp ../models/DonovanYeastPolarization/model.csl ../model.csl
/usr/bin/time -o ../results/ragtimer/8reaction_50000time.txt python3 main.py 50000 &> ../results/ragtimer/8reaction_50000.txt
