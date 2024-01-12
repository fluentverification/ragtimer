import subprocess
import os
from datetime import datetime

modelArr = ["2react", "6react", "8react", "12react"]
looseArr = ["", "loose"]
qtyArr = ["1", "10", "100", "1000", "10000"]
cycleArr = ["0"]
recBoundArr = ["0"]

# Not commuting, just trace generation
resultFolder = "results/bench_" + datetime.today().strftime('%Y-%m-%d_%H-%M-%S') + "/"
os.system("mkdir " + resultFolder)
for model in modelArr:
    os.system("mkdir " + resultFolder + model)
    for cycle in cycleArr:
        for loose in looseArr:
            for recBound in recBoundArr:
                for qty in qtyArr:
                    
                    print()
                    print()
                    print(80*"*")
                    print(80*"*")
                    print(80*"*")
                    
                    # copy over the models
                    print("cp models/" + model + "/" + model + ".* .")
                    os.system("cp models/" + model + "/" + model + ".* .")
                    print("mv " + model + ".sm model.sm")
                    os.system("mv " + model + ".sm model.sm")
                    print("mv " + model + ".csl model.csl")
                    os.system("mv " + model + ".csl model.csl")
                    print("mv " + model + ".prop model.prop")
                    os.system("mv " + model + ".prop model.prop")
                    print("mv " + model + ".ragtimer model.ragtimer")
                    os.system("mv " + model + ".ragtimer model.ragtimer")
                    
                    # run ragtimer (incl. getting time)
                    with open("ragtimer_output.txt", "w") as rto:
                        command = ["/usr/bin/time", "-o", "ragtimer_time.txt", "python3", "ragtimer.py", "commute", loose, "qty", qty, "bound", "r", "recbound", recBound, "cycle", cycle, "1>", "ragtimer_output.txt", "2>", "/dev/null"]
                        print("running " + " ".join(command))
                        subprocess.run(command, stdout=rto, stderr=subprocess.DEVNULL)
                    
                    # run prism (and get time)
                    command = "/usr/bin/time -o prism_time.txt prism -importmodel _commute/prism.tra,sta,lab -ctmc model.csl > prism_output.txt"
                    # print("running " + " ".join(command))
                    print("running " + " ".join(command))
                    os.system(command)

                    # copy models and results into results folder
                    folder = "results/commute/" + model + "/q" + qty + "_r" + recBound + "_c" + cycle + "_" + loose
                    os.system("mkdir " + folder)
                    os.system("cp model.* " + folder)
                    os.system("cp *.txt " + folder)
                    os.system("cp *.py " + folder)
                    os.system("cp _commute/prism.* " + folder)
                    with open(folder + "timestamp.txt", "w") as f:
                        f.write(str(datetime.now()))
                    
                    