#!/bin/bash

set -o xtrace
set -e

gradle build

docker build -t crowdfunding-backend -f etc/docker/Dockerfile .
