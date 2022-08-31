import os

if __name__ == "__main__":
    
    # with open("trace_list.txt", "w") as tl:
    #     with open("lazy_paths.txt", "r") as lp:
    #         tl.write(lp.read())
    
    os.system("python3 reactions_v5.py")
    os.system("make")
    os.system("make test")

