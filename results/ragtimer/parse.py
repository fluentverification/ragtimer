
modelArr = ["2react", "6react", "8react"]
looseArr = ["", "loose"]
qtyArr = ["1", "10", "100"]

for model in modelArr:
    for loose in looseArr:
        for qty in qtyArr:
            folder = model + "/" + qty + "_" + loose
            with open(folder + "/ragtimer_time.txt", "r") as ragtimerTime:
                for line in ragtimerTime:
                    userTime = line.split(" ")[0].replace("user", "")
                    break
            newQty = int(qty)
            prob = 0.0
            with open(folder + "/ragtimer_output.txt", "r") as ragtimerOutput:
                for line in ragtimerOutput:
                    if "was a duplicate and has been thrown out" in line:
                        newQty = newQty - 1
                    elif "did not reach the target state" in line:
                        newQty = newQty - 1
                    elif "Total Sum of Unique Path Probabilities:" in line:
                    	prob = float(line.split(": ")[1])
            if loose == "":
                newLoose = "default"
            else:
                newLoose = "loose"
            print(model + "," + newLoose + ",no," + qty + ",0,0," + str(newQty) + "," + userTime + "," + str(prob) + ",0")
