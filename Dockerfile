FROM docker.io/maven:3.9.0-eclipse-temurin-11 as build

RUN cd /src && mvn clean package

FROM docker.io/debian:bullseye-slim

ENV JAVA_HOME=/opt/java/openjdk
COPY --from=docker.io/eclipse-temurin:17 $JAVA_HOME $JAVA_HOME
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
RUN cd /src &&           jpackage \
            --main-jar lib/radiorecorder-${{ needs.build.outputs.version }}.jar \
            --type ${{ matrix.jpkg_type }} \
            --app-version ${PACKAGE_VERSION} \
            --input ${APPDIR} \
            --license-file ${APPDIR}/LICENSE \
            --linux-deb-maintainer s@sfuhrm.de \
            --linux-app-category sound \
            --install-dir /usr \
            ${{ env.JPACKAGE_COMMON_OPTS }}

