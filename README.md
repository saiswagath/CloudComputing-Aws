# CSYE 6225 - Spring 2020

## Technology Stack of app
- Programming Language: Java 1.11
- Web Framework: Springboot 2.2.3.RELEASE
- Database: MySql
- IDE: IntelliJ
- Version Control: Git
- Project Management: Maven
- Test Tool: Postman
- Development Environment: Ubuntu

## Build Instructions
Clone the repository into a local repository

Use Maven to build:
<code>$ mvn clean install -Plocal</code>

run the application by executing in AWS EC2 using below command:
<code>$ java -Dspring.profiles.active=$springprofilesactive -Ddb.url=$dburl -Ddb.username=$springdatasourceusername -Ddb.password=$springdatasourcepassword -Dbucket.name=$bucketname -jar  demo-0.0.1-SNAPSHOT.war</code>

The server will be run at http://localhost:8080/, test can be done using Postman.

## Deploy Instructions
MySQL port is default 3306 for my application.

Server: server side as RESTful architectural style. As a default, it is listening at http://localhost:8080/


## Running Tests
Our test files are in the file "src/test", all the functional tests and module tests are included in this file.

## CI/CD
Continous Integration and Deployment with CircelCi and AWS Code Deploy


# AWS AMI for CSYE 6225

## Validate Template for AMI for cloud

sh
packer validate ubuntu-ami-template.json


## Build AMI

sh
packer build \
    -var 'aws_access_key=REDACTED' \
    -var 'aws_secret_key=REDACTED' \
    -var 'aws_region=us-east-1' \
    -var 'subnet_id=REDACTED' \
    ubuntu-ami.json


or 

packer build -var-file=./vars.json ubuntu-ami.json

# serverless

## Email Service Using AWS Lambda Function
As a user,
> You will be able to request bills due in x days from postman.

## Getting Started
Clone the repository on your local machine

# Task 1: AWS CLI Command For CloudFormation

#### CREATE SERVERLESS STACK

Create stack by going to serverless directory in infrastructure repo


# Task 2: Trigger Circle CI for EmailLambda.jar to update function

 aws lambda update-function-code --function-name  EmailLambda  --s3-bucket ${BUCKET_NAME} --s3-key EmailLambda.jar --region ${AWS_REGION}


 #AWS CLOUDFORMATION


##Scripts file path: /infrastructure/network
 
 <p>"networking.json"</p>
 <ul>
 	<li>The cloudFormation template for network stack is inside networking folder</li>
 </ul>

## "createstack.sh" script will
<ul>
  <li>Create a network stack taking STACK_NAME and other parameters as asked by shell script</li>
</ul>


## Termination stack scripts: 
	script should take STACK_NAME as parameter
<ul>
	<li> "terminatestack.sh": Delete the stack and all networking resources.</li>
</ul>


##Scripts file path: /infrastructure/application
 
 <p>"application.json"</p>
 <ul>
 	<li>The cloudFormation template for application stack is inside application folder</li>
 </ul>

## "createstack.sh" script will
<ul>
  <li>Create a application stack taking STACK_NAME and other parameters as asked by shell script</li>
</ul>


## Termination stack scripts: 
	script should take STACK_NAME as parameter
<ul>
	<li> "terminatestack.sh": Delete the stack and all application resources.</li>
</ul>


## Command used of importing certificate 
<ul>
	<li>sudo aws acm import-certificate --certificate fileb://certificate.pem --certificate-chain fileb://certificate_chain.pem --private-key fileb://mysslcertificate.key --profile prod</li>
</ul>
