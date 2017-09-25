#!/bin/bash
set -e

source $(dirname $0)/common.sh
repository=$(pwd)/distribution-repository

git clone git-repo stage-git-repo

pushd stage-git-repo > /dev/null
version=$( get_revision_from_pom )
if [[ $RELEASE_TYPE = "M" ]]; then
	stage=$( get_next_milestone_release $version)
	next=$version
elif [[ $RELEASE_TYPE = "RC" ]]; then
	stage=$( get_next_rc_release $version)
	next=$version
elif [[ $RELEASE_TYPE = "RELEASE" ]]; then
	stage=$( strip_snapshot_suffix $version)
	next=$( bump_version_number $version)
else
	echo "Unknown release type $RELEASE_TYPE" >&2; exit 1;
fi

echo "Current version $version will be staged as $stage"
echo "Next version will be $next"

# run_maven -f spring-boot-project/spring-boot-dependencies/pom.xml clean deploy -U -Dfull -DaltDeploymentRepository=distribution::default::file://${repository} -DskipTests
popd > /dev/null
