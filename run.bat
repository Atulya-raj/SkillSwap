@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
if not defined JAVA_HOME (
    if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot" (
        set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
    )
)

set "PATH=%JAVA_HOME%\bin;%SCRIPT_DIR%tools\apache-maven-3.9.9\bin;%PATH%"
set "CATALINA_HOME=%SCRIPT_DIR%tools\apache-tomcat-10.1.34"

echo === Building SkillSwap WAR ===
call "%SCRIPT_DIR%tools\apache-maven-3.9.9\bin\mvn.cmd" clean package -DskipTests
if errorlevel 1 (
    echo Build failed.
    exit /b 1
)

echo === Deploying to Tomcat ===
if exist "%CATALINA_HOME%\webapps\skillswap" rmdir /s /q "%CATALINA_HOME%\webapps\skillswap"
copy /y "%SCRIPT_DIR%target\skillswap.war" "%CATALINA_HOME%\webapps\skillswap.war"

echo === Starting Tomcat on port 8080 ===
echo Access at: http://localhost:8080/skillswap/
call "%CATALINA_HOME%\bin\catalina.bat" run
