#! /bin/bash

BASEDIR=$(cd $(dirname "$0")/..; pwd)
JAR=${BASEDIR}/lib/radiorecorder-${project.version}.jar
LOG_DIR="${BASEDIR}/log"

java -DLOG_DIR=${LOG_DIR} -Dlog4j.configurationFile=file:///${BASEDIR}/etc/log4j2.xml -jar ${JAR} "$@"
