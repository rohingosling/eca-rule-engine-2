@echo off
setlocal EnableDelayedExpansion

set "PROJECT_DIRECTORY=%~dp0"
set "SUBSTITUTED_DRIVE_TARGET="

rem Resolve a substituted launch drive to its backing path. Maven 3.8.8 cannot report reactor progress when the
rem aggregator and child projects are represented by paths on different drive roots.

for /f "tokens=2 delims=>" %%D in ('subst 2^>nul ^| findstr /b /i /c:"%~d0"') do (
    for /f "tokens=* delims= " %%P in ("%%D") do set "SUBSTITUTED_DRIVE_TARGET=%%P"
)

if defined SUBSTITUTED_DRIVE_TARGET set "PROJECT_DIRECTORY=!SUBSTITUTED_DRIVE_TARGET!%~p0"

set "SERVER_JAR=%PROJECT_DIRECTORY%eca-server\target\eca-server.jar"
set "JAVA_COMMAND=java"

rem Handle the branch where the Maven Wrapper is unavailable.

if not exist "%PROJECT_DIRECTORY%mvnw.cmd" (
    echo ERROR: Maven Wrapper was not found at "%PROJECT_DIRECTORY%mvnw.cmd".

    rem Return exit status 1 to the calling process.

    exit /b 1
)

rem Prefer the Java runtime selected explicitly by the caller when it is available.

if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_COMMAND=%JAVA_HOME%\bin\java.exe"

rem Enter the working directory required by the following external tool call.

pushd "%PROJECT_DIRECTORY%"

rem Build the requested server and its dependency closure through the project wrapper.

call "%PROJECT_DIRECTORY%mvnw.cmd" -f "%PROJECT_DIRECTORY%pom.xml" -pl :eca-server -am package -DskipTests

rem Handle the branch where the preceding command reported a failure.

if errorlevel 1 (
    set "RUN_EXIT_CODE=!ERRORLEVEL!"

    rem Restore the caller working directory after the external tool call.

    popd

    rem Return exit status !RUN_EXIT_CODE! to the calling process.

    exit /b !RUN_EXIT_CODE!
)

rem Run the packaged server with the caller's command-line arguments.

"%JAVA_COMMAND%" -jar "%SERVER_JAR%" %*

set "RUN_EXIT_CODE=%ERRORLEVEL%"

rem Restore the caller working directory after the external tool call.

popd

rem Return exit status %RUN_EXIT_CODE% to the calling process.

exit /b %RUN_EXIT_CODE%
