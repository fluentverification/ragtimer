import os
import reactions_v5
import sys

if __name__ == "__main__":

    # print()
    # print(80*"=")
    # print("Checking", i)
    # print(80*"=")
    # print()
    i = sys.argv[1]
    reactions_v5.randTest(i, False)
    os.system("make test")
    # os.system("rm test_v*")
    
#    with open("trace_list.txt", "w") as tl:
#        with open("lazy_paths.txt", "r") as lp:
#            tl.write(lp.read())
    
    # os.system("make")

    
    # for i in range(50,500,10):
    #     print()
    #     print(80*"=")
    #     print("Checking", i)
    #     print(80*"=")
    #     print()
    #     reactions_v5.randTest(i, False)
    #     os.system("make test")

    # os.system("python3 reactions_v5.py")

