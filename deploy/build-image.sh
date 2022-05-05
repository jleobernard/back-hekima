#!/usr/bin/env bash
set -e
FULL_PATH_TO_SCRIPT="$(realpath "$0")"
SCRIPT_DIRECTORY="$(dirname "$FULL_PATH_TO_SCRIPT")"
PROJECT_BASE="$(realpath "$SCRIPT_DIRECTORY/..")"
cd $PROJECT_BASE
mvn clean package spring-boot:repackage -DskipTests
cp target/notes.jar deploy/image
docker build $PROJECT_BASE/deploy/image -t jleobernard/notes:2.0.1 --no-cache
docker login
docker push jleobernard/notes:2.0.1
docker logout