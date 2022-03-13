#!/bin/bash

# param 1: export disk dir
# param 2: export file log
# param 3: photos directory
# param 4: photos directory
# ...
# sh /Users/xxx/src/shell/runSyncPhotosTask.sh /xxx/outputDir/ /xxx/outputDir/PhoneFile.log /sdcard/DCIM/Camera1

####################################### check #########################################

firstActiveDevice=`adb devices|sed -n 2p|grep device`
if [ -z "${firstActiveDevice}" ]; then
    echo '请先使用 adb 连接手机，并确认手机已进入「开发者模式」'
    adb devices
    exit 1
fi

secondActiveDevice=`adb devices|sed -n 3p|grep device`
if [ -n "${secondActiveDevice}" ]; then
    echo '通过 adb 连接到电脑的设备有多个，请只保留一个'
    adb devices
    exit 1
fi

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
javac -d ../../build task/SyncPhotosTask.java

# dir: project/  System.getProperty("user.dir")
cd ${projectDir}
java -DuseCache=false -classpath ./build/ task.SyncPhotosTask $@

echo "shell: done."

