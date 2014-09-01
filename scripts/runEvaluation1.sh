#!/bin/bash

testSuite="testsuites/evaluation-1.xml"

OLD_DIR=`pwd`

cd ..

echo "Use the Original version of the Schedule"
cp -v ./src//test/resources/at/ac/testing/mocks/Schedule.pbj.original ./src//test/resources/at/ac/testing/mocks/Schedule.pbj

mvn clean test -P'Evaluation1' -DsuiteFile="$testSuite"  2>&1 | tee output.log

OUTPUT_DIR="reports/"`basename $testSuite .xml`

echo "Exporting Surefire Report Folder to $OUTPUT_DIR"

if [ -d "$OUTPUT_DIR" ]; then rm -rf $OUTPUT_DIR; fi

mv -v ./target/surefire-reports $OUTPUT_DIR

echo "Moving output.log to $OUTPUT_DIR"

mv -v output.log "$OUTPUT_DIR/"

cd $OLD_DIR
