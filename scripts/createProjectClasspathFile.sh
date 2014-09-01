#!/bin/bash

OLD_DIR=`pwd`

cd ..

echo "Create Project Classpath file"

mvn dependency:build-classpath -Dmdep.outputFile=project-classpath

cd $OLD_DIR
