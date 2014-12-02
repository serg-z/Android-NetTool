#!/bin/bash

CONFIG="`cat $1 \
    | sed '/^#.*$/d' \
    | sed '/^$/d'`"

echo "Sending $1 (bytes after filtering: $(echo \"$CONFIG\" | wc -c))"

echo "$CONFIG" | socat -uv - UDP-DATAGRAM:255.255.255.255:55555,broadcast
