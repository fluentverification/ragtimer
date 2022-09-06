import os

with open("prefix_test.txt", "r") as a:
    with open("trace_list.txt", "w") as b:
        b.write(a.read())

os.system("make")
os.system("make test")