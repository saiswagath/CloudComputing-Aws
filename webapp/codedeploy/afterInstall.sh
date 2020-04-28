#!/usr/bin/env bash
cd /home/ubuntu
sudo systemctl enable amazon-cloudwatch-agent
sudo systemctl start amazon-cloudwatch-agent
cd /home/ubuntu/appdeploy
source /etc/profile.d/envars.sh
printenv
sudo rm -rf /home/ubuntu/applogs/*.log
nohup java -jar -Dspring.profiles.active=$springprofilesactive -Ddb.url=$dburl -Ddb.username=$springdatasourceusername -Ddb.password=$springdatasourcepassword -Dbucket.name=$bucketname demo-0.0.1-SNAPSHOT.war 1> /home/ubuntu/applogs/webapp.out 2>&1 </dev/null &
echo "after starting sprint boot"