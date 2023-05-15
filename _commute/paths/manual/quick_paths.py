import random

paths = []

for a in range(1,50):
	print(a)
	for i in range(0,7000):
		st = ""
		for j in range(0,int(50/a)):
			action = random.choice(["r8 ","r3 "])
			st = st + (a * (action) + (a * "r5 "))
		if st not in paths:
			paths.append(st)

with open("lazy.txt", "w") as lz:
	for path in paths:
		lz.write(path + "\n")

print(len(paths), "paths generated")
