#!/usr/bin/env bash
set -e
FULL_PATH_TO_SCRIPT="$(realpath "$0")"
SCRIPT_DIRECTORY="$(dirname "$FULL_PATH_TO_SCRIPT")"
PROJECT_BASE="$(realpath "$SCRIPT_DIRECTORY/..")"
cd $PROJECT_BASE
mvn clean package spring-boot:repackage -DskipTests
cp target/hekima.jar deploy/image
docker build $PROJECT_BASE/deploy/image -t jleobernard/hekima:2.0.0 --no-cache
exit 0
docker login
docker push jleobernard/hekima:2.0.0
docker logout