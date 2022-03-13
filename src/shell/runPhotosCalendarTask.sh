#!/bin/bash

# param 1: index.html
# param 2: photos directory
# param 3: photos directory
# ...
# sh /Users/xxx/src/shell/runPhotosCalendarTask.sh /xxx/outputDir/index.html /xxx/photosDir1/ /xxx/photosDir2/

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
javac -d ../../build task/PhotosCalendarTask.java

# dir: project/  System.getProperty("user.dir")
cd ${projectDir}
java -DuseCache=false -classpath ./build/ task.PhotosCalendarTask $@

echo "shell: done."

