#!/bin/sh

echo | java -jar crowdfunding-backend.jar & echo $! > ./pid.file &
