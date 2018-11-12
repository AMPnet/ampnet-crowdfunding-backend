#!/bin/bash

docker run -d -p 8545:8545 trufflesuite/ganache-cli:latest \
            --debug \
            --verbose \
            --account="0xa88400ec75febb4244f4a04d8290ae2fbdbedb874553eb86b91f10c9de4f5fa8, 100000000000000000000" \
            --account="0xec3cd0b40d2952cc77cac778461e89dd958684b43320ff0ba1cf3ee435badf32, 100000000000000000000" \
            --account="0x16675095b2ebbe3402d71c018158a8cef7b8cdad650e716de17c487190133932, 100000000000000000000"