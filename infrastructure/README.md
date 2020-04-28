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
	<li> "terminatestack.sh": Deletes the stack and all application resources.</li>
</ul>


## Command used of importing certificate 
<ul>
	<li>sudo aws acm import-certificate --certificate fileb://certificate.pem --certificate-chain fileb://certificate_chain.pem --private-key fileb://mysslcertificate.key --profile prod</li>
</ul>