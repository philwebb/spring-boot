#!/bin/bash
set -e

source $(dirname $0)/common.sh

curl \
  -u ${ARTIFACTORY_USERNAME}:${ARTIFACTORY_PASSWORD} \
  -H"Content-type:application/json" \
  --data-binary @promote-to-milestone.json \
  -X \
  POST "http://${ARTIFACTORY_SERVER}/artifactory/api/build/promote/${BUILD_NAME}/${BUILD_NUMBER}"
