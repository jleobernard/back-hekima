#!/usr/bin/env bash
set -e
FINAL_DIR=/opt/containers/back-hekima
mvn clean install package -DskipTests
rc=$?; if [[ $rc != 0 ]]; then echo "Compilation error"; exit $rc; fi
ssh ks-leo-noport "mkdir -p $FINAL_DIR"
scp -r deploy/validation/* ks-leo-noport:$FINAL_DIR
scp target/hekima.jar ks-leo-noport:$FINAL_DIR/docker
ssh ks-leo-noport "cd $FINAL_DIR && docker-compose down"
rc=$?; if [[ $rc != 0 ]]; then echo "Cannot stop container"; fi
ssh ks-leo-noport "cd $FINAL_DIR && docker-compose up -d --build"
rc=$?; if [[ $rc != 0 ]]; then echo "Cannot build container"; exit $rc; fi