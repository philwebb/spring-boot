#!/bin/bash
set -e

source $(dirname $0)/common.sh
repository=$(pwd)/distribution-repository

pushd git-repo > /dev/null
exit 1
# run_maven -f spring-boot-project/pom.xml clean deploy -U -Dfull -DaltDeploymentRepository=distribution::default::file://${repository} -DskipTests
popd > /dev/null
