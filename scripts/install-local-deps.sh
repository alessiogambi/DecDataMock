#!/bin/bash

echo "Install the patched version2 of the Brooklyn Policy as local versions"

OLD_DIR=`pwd`

cd ..

mvn -P'InstallPatch,!pbnj' initialize 2>&1 | tee install-output.log

cd $OLD_DIR
