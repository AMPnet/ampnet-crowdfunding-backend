#!/bin/sh

java -jar crowdfunding-backend.jar & echo $! > ./pid.file
