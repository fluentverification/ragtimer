cp ../models/SingleSpecies/2reaction.ragtimer ../model.ragtimer
cp ../models/SingleSpecies/model.sm ../model.sm
cp ../models/SingleSpecies/model.csl ../model.csl
python3 main.py 10000 loose 

cp ../models/KuwaharaEnzyme/6reaction.ragtimer ../model.ragtimer
cp ../models/KuwaharaEnzyme/model.sm ../model.sm
cp ../models/KuwaharaEnzyme/model.csl ../model.csl
python3 main.py 10000 loose

cp ../models/DonovanYeastPolarization/8reaction_input_adding.txt ../model.ragtimer
cp ../models/DonovanYeastPolarization/model.sm ../model.sm
cp ../models/DonovanYeastPolarization/model.csl ../model.csl
python3 main.py 10000
