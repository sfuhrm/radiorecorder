MAVEN_VERSION=$(cat circle_maven_version.txt)
RPM_VERSION=$(echo ${MAVEN_VERSION} | cut -d"-" -f1)

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
