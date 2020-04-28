echo "Enter the Account you want to use: "

read account

echo "Enter the Stack Name: "

read stack_name

echo "Enter the VPC Name: "

read vpcName

echo "Enter the region: "

read vpcRegion

echo "Enter the VPC CIDR BLOCK: "

read vpcCidr

echo "Enter the Subnet1: "

read subNetCidrBlock1

echo "Enter the Subnet2: "

read subNetCidrBlock2

echo "Enter the Subnet3: "

read subNetCidrBlock3

echo "Give subnet name 1: "

read subnetName1

echo "Give subnet name 2: "

read subnetName2

echo "Give subnet name 3: "

read subnetName3

echo "Initiating the creation attempt"

stackId=$(aws cloudformation create-stack --stack-name $stack_name --template-body \
 file://networking.json --profile $account --region $vpcRegion --parameters \
ParameterKey=vpcName,ParameterValue=$vpcName \
ParameterKey=vpcCidr,ParameterValue=$vpcCidr \
ParameterKey=subNetCidrBlock1,ParameterValue=$subNetCidrBlock1 \
ParameterKey=subNetCidrBlock2,ParameterValue=$subNetCidrBlock2 \
ParameterKey=subNetCidrBlock3,ParameterValue=$subNetCidrBlock3 \
ParameterKey=subnetName1,ParameterValue=$stack_name$subnetName1 \
ParameterKey=subnetName2,ParameterValue=$stack_name$subnetName2 \
ParameterKey=subnetName3,ParameterValue=$stack_name$subnetName3 \
ParameterKey=vpcIdUniq,ParameterValue=vpcId$stack_name \
--query [StackId] --output text)


echo $stackId

if [ -z $stackId ]; then
    echo 'Error occurred.Dont proceed. TERMINATED'
else
    aws cloudformation wait stack-create-complete --stack-name $stackId --profile $account
    echo "STACK CREATION COMPLETE."
fi