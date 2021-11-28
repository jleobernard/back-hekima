#!/usr/bin/env bash
set -e
FINAL_DIR=/opt/containers/back-hekima
mvn clean install package -DskipTests
rc=$?; if [[ $rc != 0 ]]; then echo "Compilation error"; exit $rc; fi
ssh mandela "mkdir -p $FINAL_DIR"
scp -r deploy/validation/* mandela:$FINAL_DIR
scp target/hekima.jar mandela:$FINAL_DIR/docker
ssh mandela "cd $FINAL_DIR && docker-compose down"
rc=$?; if [[ $rc != 0 ]]; then echo "Cannot stop container"; fi
ssh mandela "cd $FINAL_DIR && docker-compose up -d --build"
rc=$?; if [[ $rc != 0 ]]; then echo "Cannot build container"; exit $rc; fi