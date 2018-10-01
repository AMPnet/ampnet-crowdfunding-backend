#!/bin/sh

stop_service() {
	kill $(cat ./pid.file) 2>/dev/null
}

stop_service || true
