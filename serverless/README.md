# serverless

## Email Service Using AWS Lambda Function
As a user,
> You will be able to request bills due in x days from postman.

## Getting Started
Clone the repository on your local machine.

# Task 1: AWS CLI Command For CloudFormation

#### CREATE SERVERLESS STACK

Create stack by going to serverless directory in infrastructure repo


# Task 2: Trigger Circle CI for EmailLambda.jar to update function

 aws lambda update-function-code --function-name  EmailLambda  --s3-bucket ${BUCKET_NAME} --s3-key EmailLambda.jar --region ${AWS_REGION}