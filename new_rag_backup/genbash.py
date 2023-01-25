with open("scripts/run_lots.sh", "w") as w:
    w.write("make\n")
    for i in range(10,1000, 10):
        w.write("/usr/bin/time -o results/" + str(i) + "time.txt python3 main.py " + str(i) + " &> results/" + str(i) + ".txt\n")