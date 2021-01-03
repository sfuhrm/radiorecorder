FROM centos:7

ARG JDK_URL=https://github.com/AdoptOpenJDK/openjdk15-binaries/releases/download/jdk-15.0.1%2B9/OpenJDK15U-jdk_x64_linux_hotspot_15.0.1_9.tar.gz

RUN yum install --assumeyes rpm-build git openssh-clients && \
  mkdir /usr/lib/jdk && \
  curl --insecure --location $JDK_URL --output - | \
  tar -xzf- --strip-components 1 --directory /usr/lib/jdk

ENV JAVA_HOME=/usr/lib/jdk
ENV PATH=$JAVA_HOME/bin:$PATH
