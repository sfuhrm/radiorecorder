VERSION=$(cat circle_maven_version.txt)

apt-get update && apt-get install --assume-yes fakeroot curl binutils

curl --location https://github.com/AdoptOpenJDK/openjdk14-binaries/releases/download/jdk-14.0.2%2B12/OpenJDK14U-jdk_x64_linux_hotspot_14.0.2_12.tar.gz --output - |
tar -xzvf-
PATH=$PWD/jdk-14.0.2+12/bin:$PATH

jpackage \
--name radiorecorder \
--type deb \
--app-version ${VERSION} \
--copyright "Stephan Fuhrmann" \
--vendor "Stephan Fuhrmann" \
--description "Records and plays internet radio streams" \
--license-file LICENSE \
--main-jar lib/radiorecorder-${VERSION}.jar \
--main-class de.sfuhrm.radiorecorder.Main \
--input target/radiorecorder-${VERSION}-app/radiorecorder-${VERSION} \
--linux-deb-maintainer s@sfuhrm.de \
--linux-app-category sound \
--dest ./target/jpackage-app \
--verbose
