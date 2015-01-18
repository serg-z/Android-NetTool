#!/bin/bash

REPEAT=5
# NOTE: system's "sleep" implementation should support floating point values
DELAY=0.2

if [ -n "$1" ]; then
    CONFIG_PATH="$1"
fi

if [ -n "$2" ]; then
    REPEAT="$2"
fi

if [ -n "$3" ]; then
    DELAY="$3"
fi

if [ -z "$CONFIG_PATH" ]; then
    echo "No config file provided"

    exit
fi

MESSAGE_ID=`date +%s`

echo "Sending $CONFIG_PATH; repeat: $REPEAT, delay: $DELAY"
echo "Message ID: $MESSAGE_ID"
echo "====="

for i in $(seq 1 $REPEAT); do
    ./broadcast_to_peds.sh "$CONFIG_PATH" $MESSAGE_ID

    sleep $DELAY
done
