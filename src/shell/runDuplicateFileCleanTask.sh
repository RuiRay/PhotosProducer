#!/bin/bash

# param 1: input directory
# sh /Users/xxx/src/shell/runDuplicateFileCleanTask.sh /xxx/input

####################################### content #########################################

shellDir=`cd $(dirname $0); pwd -P`
projectDir=`cd ${shellDir}/../../; pwd -P`
echo "shell: projectDir = ${projectDir}"

# dir: project/
cd ${projectDir}
rm -r build/
mkdir build/

# dir: project/src/java/
cd src/java/
javac -encoding UTF-8 -d ../../build task/DuplicateFileCleanTask.java

# dir: project/  System.getProperty("user.dir")
cd ${projectDir}
java -classpath ./build/ task.DuplicateFileCleanTask $@

echo "shell: done."

