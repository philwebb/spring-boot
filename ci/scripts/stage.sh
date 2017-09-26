#!/bin/bash
set -e

source $(dirname $0)/common.sh
repository=$(pwd)/distribution-repository

pushd git-repo > /dev/null
git fetch --tags --all
popd > /dev/null

git clone git-repo stage-git-repo

pushd stage-git-repo > /dev/null

snapshotVersion=$( get_revision_from_pom )
if [[ $RELEASE_TYPE = "M" ]]; then
	stageVersion=$( get_next_milestone_release $snapshotVersion)
	nextVersion=$snapshotVersion
elif [[ $RELEASE_TYPE = "RC" ]]; then
	stage=$( get_next_rc_release $snapshotVersion)
	nextVersion=$version
elif [[ $RELEASE_TYPE = "RELEASE" ]]; then
	stage=$( strip_snapshot_suffix $snapshotVersion)
	nextVersion=$( bump_version_number $snapshotVersion)
else
	echo "Unknown release type $RELEASE_TYPE" >&2; exit 1;
fi

echo "Staging $stageVersion (next version will be $nextVersion)"

set_revision_to_pom "$stageVersion"
git config user.name "Spring Buildmaster" > /dev/null
git config user.email "buildmaster@springframework.org" > /dev/null
git add pom.xml > /dev/null
git commit -m"Release v$stageVersion" > /dev/null
git tag -a "v$stageVersion" -m"Release v$stageVersion" > /dev/null

run_maven -f spring-boot-project/spring-boot-dependencies/pom.xml clean deploy -U -Dfull -DaltDeploymentRepository=distribution::default::file://${repository} -DskipTests
# run_maven -f spring-boot-tests/spring-boot-integration-tests/pom.xml clean install -U -Dfull -Drepository=file://${repository} -DskipTests
# run_maven -f spring-boot-tests/spring-boot-deployment-tests/pom.xml clean install -U -Dfull -Drepository=file://${repository} -DskipTests

echo "Setting next development version (v$nextVersion)"
git reset --hard HEAD^ > /dev/null
if [[ $nextVersion != $snapshotVersion ]]; then
	set_revision_to_pom "$nextVersion"
	git add pom.xml > /dev/null
	git commit -m"Next development version (v$nextVersion)" > /dev/null
fi;

echo "DONE"

popd > /dev/null
