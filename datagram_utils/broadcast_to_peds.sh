#!/bin/bash

MESSAGE_ID="$2"

if [ -z "$MESSAGE_ID" ]; then
    MESSAGE_ID="-1"
fi

CONFIG="`cat $1 \
    | sed '/^#.*$/d' \
    | sed '/^$/d'`"

echo "Sending $1 (bytes after filtering: $(echo \"$CONFIG\" | wc -c))"

echo -e "message_id=$MESSAGE_ID\n$CONFIG" \
    | socat -uv - UDP-DATAGRAM:255.255.255.255:55555,broadcast
