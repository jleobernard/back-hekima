#!/usr/bin/env bash
set -e
NOTES_VERSION=`grep '<!--MY_VERSION-->'  pom.xml  | sed -E 's/.*<version>//' |  sed -E 's/<\/version>.*//' | cut -f 1 -d '-'`

FULL_PATH_TO_SCRIPT="$(realpath "$0")"
SCRIPT_DIRECTORY="$(dirname "$FULL_PATH_TO_SCRIPT")"
PROJECT_BASE="$(realpath "$SCRIPT_DIRECTORY/..")"
cd $PROJECT_BASE
mvn clean package spring-boot:repackage -DskipTests
cp target/notes.jar deploy/image
docker build $PROJECT_BASE/deploy/image -t jleobernard/notes:$NOTES_VERSION --no-cache
docker login
docker push jleobernard/notes:$NOTES_VERSION
#docker logout