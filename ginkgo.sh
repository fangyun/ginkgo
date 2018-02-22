#!/bin/bash

DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=y"
WORK_DIR=/home/fangyun/workspace/ginkgo
if [ "$1" = "-d" ];
then
    java -ea $DEBUG_OPTS -jar target/ginkgo-0.0.1-SNAPSHOT.jar log-file=$WORK_DIR
else
    java -ea -jar target/ginkgo-0.0.1-SNAPSHOT.jar log-file=$WORK_DIR
fi
