#!/bin/sh

docker run \
    -p 5683:5683/udp \
    -p 5684:5684/udp \
    -p 5783:5783/udp \
    -p 5784:5784/udp \
    -p 8081:8081 \
    -p 4001:4001 \
    -p 5001:5001 \
    lwm2m-server
