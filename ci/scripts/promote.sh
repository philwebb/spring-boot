#!/bin/bash
set -e

source $(dirname $0)/common.sh

ls -l artifactory-repo
cat artifactory-repo/build-info.json

buildName=$( cat artifactory-repo/build-info.json | jq -r '.buildInfo.name' )
buildNumber=$( cat artifactory-repo/build-info.json | jq -r '.buildInfo.number' )
targetRepo="libs-milestone-local"

curl \
  -u ${ARTIFACTORY_USERNAME}:${ARTIFACTORY_PASSWORD} \
  -H"Content-type:application/json" \
  -d "{\"status\": \"staged\", \"sourceRepo\": \"libs-staging-local\", \"targetRepo\": \"\"}"  \
  -f \
  -X \
  POST "${ARTIFACTORY_SERVER}/api/build/promote/${buildName}/${buildNumber}" > /dev/null || { echo "Failed to promote" >&2; exit 1; }

  echo "Promoted ${buildName}/${buildNumber} to ${targetRepo}"