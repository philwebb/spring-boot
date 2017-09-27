#!/bin/bash
set -e

source $(dirname $0)/common.sh

ls -l artifactory-repo
cat artifactory-repo/build-info.json

BUILD_NAME=$( cat artifactory-repo/build-info.json | jq -r '.buildInfo.name' )
BUILD_NUMBER=$( cat artifactory-repo/build-info.json | jq -r '.buildInfo.number' )

echo "$BUILD_NAME / $BUILD_NUMBER"

curl \
  -u ${ARTIFACTORY_USERNAME}:${ARTIFACTORY_PASSWORD} \
  -H"Content-type:application/json" \
  --data-binary @git-repo/ci/scripts/promote-to-milestone.json \
  -X \
  POST "${ARTIFACTORY_SERVER}/artifactory/api/build/promote/${BUILD_NAME}/${BUILD_NUMBER}" > /dev/null || { echo "Failed to promote" >&2; exit 1; }
