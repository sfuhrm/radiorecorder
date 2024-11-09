#! /bin/bash

BASEDIR=$(cd $(dirname "$0")/..; pwd)
JAR=${BASEDIR}/lib/radiorecorder-${project.version}.jar
java -jar ${JAR} "$@"
