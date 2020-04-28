# CSYE 6225 - Spring 2020

## Student Information

| Name | NEU ID | Email Address |
| --- | --- | --- |
| Kalpati Sai Swagath | 001417567| kalpatiananthakris.s@husky.neu.edu |
| | | |

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
Clone the repository into a local repository.

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
