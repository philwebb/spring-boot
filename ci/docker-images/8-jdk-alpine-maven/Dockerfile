FROM openjdk:8-jdk-alpine
LABEL Description="Spring Boot Maven base image" Vendor="Pivotal" Version="1.0"

ENV DEBIAN_FRONTEND noninteractive
ENV HOME /root

RUN apk add --no-cache curl tar bash

ADD scripts/install-maven.sh install-maven.sh
RUN ./install-maven.sh

ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"
