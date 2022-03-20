#!/bin/bash

# param 1: input dir
# param 2: output dir
# sh /Users/xxx/src/shell/runReduceImageSizeTask.sh /xxx/inputDir/ /xxx/outputDir/

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
javac -encoding UTF-8 -d ../../build task/ReduceImageSizeTask.java

# dir: project/  System.getProperty("user.dir")
cd ${projectDir}
java -classpath ./build/ task.ReduceImageSizeTask $@

echo "shell: done."

