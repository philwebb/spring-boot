#!/bin/bash
set -e

source $(dirname $0)/common.sh
repository=$(pwd)/distribution-repository

pushd git-repo > /dev/null
run_maven -f spring-boot-project/spring-boot-dependencies/pom.xml clean deploy -U -Dfull -DaltDeploymentRepository=distribution::default::file://${repository} -DskipTests
popd > /dev/null
