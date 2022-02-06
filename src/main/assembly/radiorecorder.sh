#! /bin/bash

BASEDIR=$(cd $(dirname "$0")/..; pwd)

java -jar ${JAR} "$@"
