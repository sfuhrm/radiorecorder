VERSION=$(cat circle_maven_version.txt)

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
