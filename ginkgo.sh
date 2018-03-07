#!/bin/bash

DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=y"
WORK_DIR=`pwd`
if [ "$1" = "-d" ];
then
    java -Dfile.encoding=UTF-8 -Dginkgo.root=$WORK_DIR -ea $DEBUG_OPTS -jar target/ginkgo-0.0.1-SNAPSHOT.jar log-file=$WORK_DIR/log
else
    java -Dfile.encoding=UTF-8 -Dginkgo.root=$WORK_DIR -ea -jar target/ginkgo-0.0.1-SNAPSHOT.jar log-file=$WORK_DIR/log
fi
