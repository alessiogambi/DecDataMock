#!/bin/bash

export MAVEN_OPTS="-Xmx1024m"

OLD_DIR=`pwd`

cd ..

#        "testsuites/evaluation-3.1.xml" 
#        "testsuites/evaluation-3.2.xml"
# Apply the Patch to Schedule: cp SimultaneousSchedule -> Schedule.pbj and recompile.
Echo "Apply the Schedule Patch for Simultaneous events"

cp -v ./src//test/resources/at/ac/testing/mocks/Schedule.pbj.evaluation3 ./src//test/resources/at/ac/testing/mocks/Schedule.pbj

for testSuite in \
	"testsuites/evaluation-3.1.xml" \
	"testsuites/evaluation-3.2.xml"

do
	echo "Processing $testSuite"
	mvn clean test -DsuiteFile="$testSuite" -P'Evaluation3' 2>&1 | tee output.log

	# Prepare the Package to mv to output folder
        OUTPUT_DIR="reports/"`basename $testSuite .xml` 
        if [ -d "$OUTPUT_DIR" ]; then rm -rf $OUTPUT_DIR; fi

	echo "Moving Surefire Report Folder to $OUTPUT_DIR"
	mv -v ./target/surefire-reports $OUTPUT_DIR

	echo "Moving output.log to $OUTPUT_DIR"
	mv -v output.log "$OUTPUT_DIR/"

done

cd $OLD_DIR
