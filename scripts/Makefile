
# Need to link to a PRISM distribution
# Link to root folder (i.e. not prism/prism)
PRISM_DIR = /usr/local/prism-src/prism

# For compilation, just need access to classes/jars in the PRISM distribution
# We look in both the top-level and the prism sub-directory
# (currently svn/git repos and downloaded distributions differ in structure)
PRISM_CLASSPATH = "$(PRISM_DIR)/classes:$(PRISM_DIR)/lib/*:$(PRISM_DIR)/prism/classes:$(PRISM_DIR)/prism/lib/*"

# This Makefile just builds all java files in src and puts the class files in classes

JAVA_FILES := $(shell cd src && find . -name '*.java')
CLASS_FILES = $(JAVA_FILES:%.java=classes/%.class)

default: all

all: init $(CLASS_FILES)

init:
	@mkdir -p classes

classes/%.class: src/%.java
	(javac -classpath $(PRISM_CLASSPATH) -d classes $<)

# Test execution

test:
	PRISM_DIR=$(PRISM_DIR) PRISM_MAINCLASS=simulate.BuildModel bin/run
# 	prism -importmodel buildModel.tra,sta,lab -exportmodel out.tra,sta,lab -ctmc pro.csl > final_prism_report.txt

# Clean up

clean:
	@rm -f $(CLASS_FILES)

celan: clean