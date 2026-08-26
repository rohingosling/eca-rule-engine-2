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
set "EXAMPLE_MODEL=%PROJECT_DIRECTORY%examples\eca-rule-engine-example.json"
set "BUILD_LOG=%PROJECT_DIRECTORY%eca-server\target\run-server-build.log"
set "JAVA_COMMAND=java"

rem Handle the branch where the Maven Wrapper is unavailable.

if not exist "%PROJECT_DIRECTORY%mvnw.cmd" (
    echo ERROR: Maven Wrapper was not found at "%PROJECT_DIRECTORY%mvnw.cmd".

    rem Return exit status 1 to the calling process.

    exit /b 1
)

rem Prefer the Java runtime selected explicitly by the caller when it is available.

if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_COMMAND=%JAVA_HOME%\bin\java.exe"

rem Silence the Java runtime warnings raised by the build tool's own dependencies. They report restricted native access
rem and deprecated sun.misc.Unsafe calls made by Maven's console and collection libraries, which describe the build
rem tool rather than the ECA Rule Engine.

set "MAVEN_OPTS=%MAVEN_OPTS% --enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow"

rem Enter the working directory required by the following external tool call.

pushd "%PROJECT_DIRECTORY%"

rem Create the build-output directory so that the build log can be written on a first run.

if not exist "%PROJECT_DIRECTORY%eca-server\target" mkdir "%PROJECT_DIRECTORY%eca-server\target"

rem Build the requested server and its dependency closure through the project wrapper.
rem
rem The build writes to a log rather than to the terminal, so that build-tool progress and third-party packaging
rem warnings do not reach the operator. The complete log is printed when the build fails, so no diagnostic is lost.

call "%PROJECT_DIRECTORY%mvnw.cmd" -f "%PROJECT_DIRECTORY%pom.xml" -pl :eca-server -am package -DskipTests > "%BUILD_LOG%" 2>&1

rem Handle the branch where the preceding command reported a failure.

if errorlevel 1 (
    set "RUN_EXIT_CODE=!ERRORLEVEL!"

    echo ERROR: The server build failed. The complete build log is at "%BUILD_LOG%" and follows.
    echo.

    type "%BUILD_LOG%"

    rem Restore the caller working directory after the external tool call.

    popd

    rem Return exit status !RUN_EXIT_CODE! to the calling process.

    exit /b !RUN_EXIT_CODE!
)

rem Run the packaged server. The caller's command-line arguments take precedence when they are supplied. Absent any
rem caller arguments, start the server hosting the bundled example model so that a freshly launched server always has a
rem model to serve.

if not "%~1"=="" (
    "%JAVA_COMMAND%" -jar "%SERVER_JAR%" %*
) else if exist "%EXAMPLE_MODEL%" (
    "%JAVA_COMMAND%" -jar "%SERVER_JAR%" start --model "%EXAMPLE_MODEL%"
) else (
    echo WARNING: The example model was not found at "%EXAMPLE_MODEL%".
    echo WARNING: Starting the server without a hosted model.

    "%JAVA_COMMAND%" -jar "%SERVER_JAR%" start
)

set "RUN_EXIT_CODE=%ERRORLEVEL%"

rem Restore the caller working directory after the external tool call.

popd

rem Return exit status %RUN_EXIT_CODE% to the calling process.

exit /b %RUN_EXIT_CODE%
