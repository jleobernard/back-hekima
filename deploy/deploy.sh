#!/usr/bin/env bash
FINAL_DIR=/opt/containers/back-hekima
mvn clean install package -DskipTests
rc=$?; if [[ $rc != 0 ]]; then echo "Compilation error"; exit $rc; fi
ssh ks-leo "mkdir -p $FINAL_DIR"
scp -r deploy/validation/* ks-leo:$FINAL_DIR
scp target/back-hekima.jar ks-leo:$FINAL_DIR/docker
ssh ks-leo "cd $FINAL_DIR && docker-compose down"
rc=$?; if [[ $rc != 0 ]]; then echo "Cannot stop container"; fi
ssh ks-leo "cd $FINAL_DIR && docker-compose up -d --build"
rc=$?; if [[ $rc != 0 ]]; then echo "Cannot build container"; exit $rc; fi