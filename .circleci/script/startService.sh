#!/bin/sh

nohup java -jar crowdfunding-backend.jar > log.txt 2> errors.txt < /dev/null &
PID=$!
echo $PID > pid.file
