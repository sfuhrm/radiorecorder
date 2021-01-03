set -e
set +x

export PATH=jdk-14.0.2+12/bin:$PATH
export MAVEN_OPTS=-"Xmx3072m -XX:MaxPermSize=512m -XX:+CMSClassUnloadingEnabled -XX:-UseGCOverheadLimit"

# version from maven pom
MAVEN_VERSION=$(cat circle_maven_version.txt)
# strip -SNAPSHOT, if existing
WIN_VERSION=$(echo ${MAVEN_VERSION} | cut -d"-" -f1)

echo > circle_win_version.txt ${WIN_VERSION}

jpackage \
--name radiorecorder \
--type msi \
--app-version ${WIN_VERSION} \
--copyright "Stephan Fuhrmann" \
--vendor "Stephan Fuhrmann" \
--description "Records and plays internet radio streams" \
--license-file LICENSE \
--main-jar lib/radiorecorder-${MAVEN_VERSION}.jar \
--input target/radiorecorder-${MAVEN_VERSION}-app/radiorecorder-${MAVEN_VERSION} \
--win-dir-chooser \
--win-console \
--dest ./target/jpackage-app \
--verbose
