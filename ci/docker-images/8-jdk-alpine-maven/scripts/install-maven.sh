#!/bin/sh

set -eu

_main() {
  mavenversion=3.5.0
  mavensha=beb91419245395bd69a4a6edad5ca3ec1a8b64e41457672dc687c173a495f034
  mavenurl=https://www-us.apache.org/dist/maven/maven-3/${mavenversion}/binaries

  mkdir -p /usr/share/maven /usr/share/maven/ref
  curl -fsSL -o /tmp/apache-maven.tar.gz "${mavenurl}/apache-maven-${mavenversion}-bin.tar.gz"
  echo "${mavensha}  /tmp/apache-maven.tar.gz" | sha256sum -c -
  tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1
  rm -f /tmp/apache-maven.tar.gz
  ln -s /usr/share/maven/bin/mvn /usr/bin/mvn
}

_main "$@"
