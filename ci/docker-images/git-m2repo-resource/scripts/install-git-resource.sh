#!/bin/sh

set -eu

_main() {
  version=096e9228292cf0342a8b748b8cf34ecc3edb6cc0
  tmpdir="$(mktemp -d git_resource_install.XXXXXX)"
  cd "$tmpdir"
  git clone https://github.com/concourse/git-resource.git .
  git checkout "${version}"
  mkdir -p /opt/resource/git
  cp -r assets/* /opt/resource/git
  chmod +x /opt/resource/git/*
}

_main "$@"
