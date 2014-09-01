#!/bin/bash

export MAVEN_OPTS="-Xmx1024m"

OLD_DIR=`pwd`

cd ..

echo "Use the Original version of the Schedule"
cp -v ./src//test/resources/at/ac/testing/mocks/Schedule.pbj.original ./src//test/resources/at/ac/testing/mocks/Schedule.pbj

for testSuite in \
	"testsuites/evaluation-2.1a.xml" \
	"testsuites/evaluation-2.2a.xml" \
	"testsuites/evaluation-2.1b.xml" \
	"testsuites/evaluation-2.2b.xml"

do
	echo "Processing $testSuite"
	mvn clean test -DsuiteFile="$testSuite" -P'Evaluation2' 2>&1 | tee output.log

	# Prepare the Package to mv to output folder
        OUTPUT_DIR="reports/"`basename $testSuite .xml`
        if [ -d "$OUTPUT_DIR" ]; then rm -rf $OUTPUT_DIR; fi
	mkdir $OUTPUT_DIR

	echo "Moving Surefire Report Folder to $OUTPUT_DIR"
	mv -v ./target/surefire-reports $OUTPUT_DIR

	echo "Moving output.log to $OUTPUT_DIR"
	mv -v output.log "$OUTPUT_DIR/"

done

cd $OLD_DIR
