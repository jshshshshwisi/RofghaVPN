#!/bin/sh
# Gradle Wrapper - downloads gradle if needed

# Set project root
APP_HOME=$( cd "${0%/*}" && pwd -P ) || exit
cd "$APP_HOME"

# Check if gradle exists
if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
    echo "Downloading Gradle Wrapper..."
    curl -sL "https://services.gradle.org/distributions/gradle-8.5-bin.zip" -o /tmp/gradle-dist.zip
    mkdir -p /tmp/gradle-dist
    unzip -q -o /tmp/gradle-dist.zip -d /tmp/gradle-dist/
    cp /tmp/gradle-dist/gradle-8.5/lib/gradle-launcher-*.jar gradle/wrapper/gradle-wrapper.jar
    rm -rf /tmp/gradle-dist*
fi

exec java -jar gradle/wrapper/gradle-wrapper.jar "$@"
