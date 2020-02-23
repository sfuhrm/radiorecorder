#! /bin/bash

BASEDIR=$(cd $(dirname "$0")/..; pwd)
JAR=${BASEDIR}/lib/radiorecorder-*.jar

java -Dlog4j.configurationFile=file:///${BASEDIR}/etc/log4j2.xml -jar ${JAR} "$@"
