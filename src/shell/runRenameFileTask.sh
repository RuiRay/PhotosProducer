#!/bin/bash

# param 1: photos directory
# param 2: photos directory
# ...
# sh /Users/xxx/src/shell/runRenameFileTask.sh /xxx/photosDir1/ /xxx/photosDir2/

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
javac -encoding UTF-8 -Djava.ext.dirs=../libs/ -d ../../build task/RenameFileTask.java

# dir: project/  System.getProperty("user.dir")
cd ${projectDir}
java -Djava.ext.dirs=./src/libs/ -DuseCache=false -classpath ./build/ task.RenameFileTask $@

echo "shell: done."

