#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export CATALINA_HOME="${SCRIPT_DIR}/tools/apache-tomcat-10.1.34"

echo "=== Building SkillSwap WAR ==="
mvn clean package -DskipTests

echo "=== Deploying to Tomcat ==="
rm -rf "${CATALINA_HOME}/webapps/skillswap"
cp "${SCRIPT_DIR}/target/skillswap.war" "${CATALINA_HOME}/webapps/skillswap.war"

echo "=== Starting Tomcat on port 8080 ==="
"${CATALINA_HOME}/bin/catalina.sh" run
