DecDataMock
===========

This repository contains the code of the declarative data mocks developed using the PBnJ tool for testing the elasticity of AutoScalerPolicy of the Brooklyn project.
In addition to the mocks, the repository contains test scaffolding, test driver, testsuite file, and the setup scripts.

## Requirements

To run the evaluation you need to have installed JAVA 7 (with java pointing correctly to it!) and  maven

## Setup

1.  Configure the PBNJ_HOME environment variable to point to **root-of-the-project**/tool/pbnj  
2.  Go to **root-of-the-project**/scripts  
  2.1	Run ./install-local-deps.sh to install into your local repository the patched version of the Brooklyn code  
  2.2 Run ./createProjectClasspathFile.sh to create a file named project-classpath that the pbnj uses to compile the executable specifications
    
## Run the Experiments

Go to **root-of-the-project**/scripts 

	./runEvaluation1.sh 
	
	./runEvaluation2.sh 

	./runEvaluation3.sh 

Each of the run scripts will run all the tests to replicate the Evaluation 1/2/3.
A report called evaluation-1/2/3 will be produced in the _reports_ folder

## Note

The first time you'll run any of the evaluation scripts maven will download A LOT of stuff, and that might take a while.

We tested the scripts on Mac Os X and Ubuntu. In case of problem, please send an email to a.gambi@infosys.tuwien.ac.at