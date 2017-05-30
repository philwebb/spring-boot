#!/bin/sh

set -eu

_main() {
  gitversion=1.5.4
  local tmpdir
  tmpdir="$(mktemp -d git_lfs_install.XXXXXX)"

  cd "$tmpdir"
  curl -Lo git.tar.gz "https://github.com/github/git-lfs/releases/download/v${gitversion}/git-lfs-linux-amd64-${gitversion}.tar.gz"
  gunzip git.tar.gz
  tar xf git.tar
  mv "git-lfs-${gitversion}/git-lfs" /usr/bin
  cd ..
  rm -rf "$tmpdir"
  git lfs install --skip-smudge
}

_main "$@"
