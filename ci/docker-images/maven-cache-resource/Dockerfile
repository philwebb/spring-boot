FROM springboot/8-jdk-alpine-maven

RUN ln -s /usr/bin/gpg2 /usr/bin/gpg
RUN apk add --no-cache git jq

ADD scripts/install-git-lfs.sh install-git-lfs.sh
RUN ./install-git-lfs.sh

ADD scripts/install-git-resource.sh install-git-resource.sh
RUN ./install-git-resource.sh

ADD assets/ /opt/resource/
RUN chmod +x /opt/resource/check /opt/resource/in /opt/resource/out
