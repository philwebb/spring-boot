#!/bin/bash
set -e

source $(dirname $0)/common.sh
repository=$(pwd)/distribution-repository

setup_symlinks
cleanup_maven_repo

pushd spring-boot > /dev/null
run_maven -f spring-boot-project/pom.xml clean deploy -U -Dfull -DaltDeploymentRepository=distribution::default::file://${repository}
popd > /dev/null
