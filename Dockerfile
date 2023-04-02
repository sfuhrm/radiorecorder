FROM docker.io/debian:bullseye-slim

ENV JAVA_HOME=/opt/java/openjdk
COPY --from=docker.io/eclipse-temurin:17 $JAVA_HOME $JAVA_HOME
COPY . /src
ENV PATH="${JAVA_HOME}/bin:${PATH}"

RUN apt-get update && \
    apt-get install -y --no-install-recommends curl openssh-client git fakeroot curl binutils

ENV JPACKAGE_COMMON_OPTS: \
            --name radiorecorder \
            --copyright "Stephan Fuhrmann" \
            --vendor "Stephan Fuhrmann" \
            --description "Records and plays internet radio streams" \
            --main-class de.sfuhrm.radiorecorder.Main \
            --dest jpackage-app \
            --verbose 

ENV JAVA_HOME=/opt/java/openjdk/
RUN cd /src && \
          PROJECT_VERSION=$(cat project.version); \
          APPDIR=target/radiorecorder-${PROJECT_VERSION}-app; \
           jpackage \
            --main-jar radiorecorder-${PROJECT_VERSION}/lib/radiorecorder-${PROJECT_VERSION}.jar \
            --type deb \
            --app-version ${PROJECT_VERSION} \
            --input target/radiorecorder-${PROJECT_VERSION}-app \
            --license-file radiorecorder-${PROJECT_VERSION}/LICENSE \
            --linux-deb-maintainer s@sfuhrm.de \
            --linux-app-category sound \
            --install-dir /usr \
            ${JPACKAGE_COMMON_OPTS}
