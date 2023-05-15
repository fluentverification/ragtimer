
modelArr = ["2react", "6react", "8react"]
looseArr = ["", "loose"]
qtyArr = ["1", "10", "100"]
cycleArr = ["0", "2", "4"]
recBoundArr = ["2", "10", "20"]

for model in modelArr:
    for cycle in cycleArr:
        for recBound in recBoundArr:
            for loose in looseArr:
                for qty in qtyArr:
                    folder = model + "/q" + qty + "_r" + recBound + "_c" + cycle + "_" + loose
                    with open(folder + "/ragtimer_time.txt", "r") as ragtimerTime:
                        for line in ragtimerTime:
                            userTime = line.split(" ")[0].replace("user", "")
                            break
                    with open(folder + "/prism_time.txt", "r") as prismTimeFile:
                        for line in prismTimeFile:
                            prismTime = line.split(" ")[0].replace("user", "")
                            break
                    newQty = int(qty)
                    with open(folder + "/ragtimer_output.txt", "r") as ragtimerOutput:
                        for line in ragtimerOutput:
                            if "was a duplicate and has been thrown out" in line:
                                newQty = newQty - 1
                            elif "did not reach the target state" in line:
                                newQty = newQty - 1
                    prob = 0.0
                    with open(folder + "/prism_output.txt", "r") as ragtimerOutput:
                        for line in ragtimerOutput:
                            if "Result:" in line:
                                prob = float(line.split()[1])
                    if loose == "":
                        newLoose = "default"
                    else:
                        newLoose = "loose"
                    print(model + "," + newLoose + ",yes," + qty + "," + recBound + "," + cycle + "," + str(newQty) + "," + userTime + "," + str(prob) + "," + prismTime)
