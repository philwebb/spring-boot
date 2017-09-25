#!/bin/bash
set -e

source $(dirname $0)/common.sh

echo "Handle build outcome $1"

git clone build-info-git-repo updated-build-info-git-repo
echo "spring-boot-builds@mailinator.com" > email-notification/to
