echo "Enter you AWS Account Name: "

read account

echo "Enter the Stack Name: "

read stack_name

echo "Enter the KeyPair: "

read KeyPair

echo "Enter the VPC Name: "

read vpcName

echo "Enter the Region where you want to create your Stack: "

read vpcRegion

echo "Enter the VPC CIDR BLOCK: "

read vpcCidr

#Taking Parameter to create Public Subnets

echo ":::::::>Creating PUBLIC Subnet<:::::::::::::"

echo ":::>Public Subnet-1<:::"

echo "Name: "

read pubSubnet1

echo "CIDR: "

read pubSubnet1CIDR

echo ":::>Public Subnet-2<:::"

echo "Name: "

read pubSubnet2

echo "CIDR: "

read pubSubnet2CIDR

echo ":::>Public Subnet-3<:::"

echo "Name: "

read pubSubnet3

echo "CIDR: "

read pubSubnet3CIDR

echo "DBUsername: "

read DBUsername

echo "DBPassword: "

read DBPassword

echo " "

#Taking Parameter to create Private Subnets

echo ":::::::>Creating PRIVATE Subnet<:::::::::::::"

echo ":::>Private Subnet-1<:::"

echo "Name: "

read pvtSubnet1

echo "CIDR: "

read pvtSubnet1CIDR

echo ":::>Private Subnet-2<:::"

echo "Name: "

read pvtSubnet2

echo "CIDR: "

read pvtSubnet2CIDR

echo ":::>Private Subnet-3<:::"

echo "Name: "

read pvtSubnet3

echo "CIDR: "

read pvtSubnet3CIDR

echo "Enter the AMI ID: "

read ImageID

echo "Enter EC2 instance size: "

read EC2VolumeSize

echo "Enter RDS instance size: "

read RDSVolumeSize

echo "========================================================="

echo "Script being Initialized with Template Body............."

echo "========================================================="

echo ""

stackId=$(aws cloudformation create-stack --stack-name $stack_name --template-body \
 file://application.json --profile $account --region $vpcRegion  --capabilities CAPABILITY_NAMED_IAM --parameters \
ParameterKey=vpcName,ParameterValue=$vpcName \
ParameterKey=ImageID,ParameterValue=$ImageID \
ParameterKey=vpcCidr,ParameterValue=$vpcCidr \
ParameterKey=KeyPair,ParameterValue=$KeyPair \
ParameterKey=EC2VolumeSize,ParameterValue=$EC2VolumeSize \
ParameterKey=RDSVolumeSize,ParameterValue=$RDSVolumeSize \
ParameterKey=pubSubnet1CIDR,ParameterValue=$pubSubnet1CIDR \
ParameterKey=pubSubnet2CIDR,ParameterValue=$pubSubnet2CIDR \
ParameterKey=pubSubnet3CIDR,ParameterValue=$pubSubnet3CIDR \
ParameterKey=pubSubnet1,ParameterValue=$stack_name$pubSubnet1 \
ParameterKey=pubSubnet2,ParameterValue=$stack_name$pubSubnet2 \
ParameterKey=pubSubnet3,ParameterValue=$stack_name$pubSubnet3 \
ParameterKey=pvtSubnet1CIDR,ParameterValue=$pvtSubnet1CIDR \
ParameterKey=pvtSubnet2CIDR,ParameterValue=$pvtSubnet2CIDR \
ParameterKey=pvtSubnet3CIDR,ParameterValue=$pvtSubnet3CIDR \
ParameterKey=pvtSubnet1,ParameterValue=$stack_name$pvtSubnet1 \
ParameterKey=pvtSubnet2,ParameterValue=$stack_name$pvtSubnet2 \
ParameterKey=pvtSubnet3,ParameterValue=$stack_name$pvtSubnet3 \
ParameterKey=vpcIdUnique,ParameterValue=vpcId$stack_name \
ParameterKey=DBUsername,ParameterValue=$DBUsername \
ParameterKey=DBPassword,ParameterValue=$DBPassword \
--query [StackId] --output text)

echo "###################################################################################################"
echo 'Your Stack Id: '$stackId
echo "###################################################################################################"

if [ -z $stackId ]; then
    echo 'Error occurred.Dont proceed. TERMINATED'
else
    aws cloudformation wait stack-create-complete --stack-name $stackId --profile $account --region $vpcRegion
    echo "Stack Created Successfully................"
fi