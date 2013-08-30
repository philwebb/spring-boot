#!/bin/sh

# This script is executed by the CI server before each build

# Write a file that we can use to set Maven Profiles
rm building-version.* 2>/dev/null
version=`cat pom.xml| grep -m 1 "<version>.*</version>"`
echo "$version" | grep -q "\-SNAPSHOT<"
if [ $? -eq 0 ];then
	echo "Setting 'building-version' profile file to SNAPSHOT"
	touch building-version.snapshot
fi
echo "$version" | grep -q "\.M.*<"
if [ $? -eq 0 ];then
	echo "Setting 'building-version' profile file to MILESTONE"
	touch building-version.milestone
fi
echo "$version" | grep -q "\.RELEASE<"
if [ $? -eq 0 ];then
	echo "Setting 'building-version' profile file to RELEASE"
	touch building-version.release
fi


# Clean Grapes
if [ -z "${GROOVY_HOME}" ];then
  GROOVY_HOME=~/.groovy
fi
echo "Cleaning ${GROOVY_HOME}/grapes"
rm -rf "${GROOVY_HOME}"/grapes 2>/dev/null