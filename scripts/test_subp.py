import subprocess

def escape_chars(s):
  s = s.replace("\\n, \n")
  s = s.replace("\\r, \r")
  s = s.replace("\\t, \t")
  s = s.replace("\\n, \n")
  s = s.replace("\\n, \n")
  return s

cppout = subprocess.check_output(["ivy_check", "test.ivy"], universal_newlines = True)


#check output:
# with open("checkoutput.txt", "w") as co:
print(str(cppout))