#!/usr/bin/env bash
echo "Clean up for next Deployment"


PID=`ps -C java -o pid=`

kill -9 $PID


echo "Stopped existing application"

mkdir -p /home/ubuntu/appdeploy
sudo rm -rf /home/ubuntu/appdeploy/

cd /opt
sudo mkdir cloudwatch
sudo rm -rf /opt/cloudwatch/cloudwatch-config.json
