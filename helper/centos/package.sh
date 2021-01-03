MAVEN_VERSION=$(cat circle_maven_version.txt)
RPM_VERSION=$(echo ${MAVEN_VERSION} | cut -d"-" -f1)

curl --location https://github.com/AdoptOpenJDK/openjdk14-binaries/releases/download/jdk-14.0.2%2B12/OpenJDK14U-jdk_x64_linux_hotspot_14.0.2_12.tar.gz --output - |
tar -xzvf-
PATH=$PWD/jdk-14.0.2+12/bin:$PATH

yum install --assumeyes rpm-build

jpackage \
--name radiorecorder \
--type rpm \
--app-version ${RPM_VERSION} \
--copyright "Stephan Fuhrmann" \
--vendor "Stephan Fuhrmann" \
--description "Records and plays internet radio streams" \
--license-file LICENSE \
--main-jar lib/radiorecorder-${MAVEN_VERSION}.jar \
--main-class de.sfuhrm.radiorecorder.Main \
--input target/radiorecorder-${MAVEN_VERSION}-app/radiorecorder-${MAVEN_VERSION} \
--dest ./target/jpackage-app \
--verbose
