#!/bin/sh

start_service() {
	java -jar crowdfunding-backend.jar & echo $! > ./pid.file
}

start_service || true
