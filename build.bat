@echo off
setlocal

set "PROJECT_DIRECTORY=%~dp0"

rem Handle the branch where the required file or directory is unavailable.

if not exist "%PROJECT_DIRECTORY%mvnw.cmd" (
    echo ERROR: Maven Wrapper was not found at "%PROJECT_DIRECTORY%mvnw.cmd".

    rem Return exit status 1 to the calling process.

    exit /b 1
)

rem Enter the working directory required by the following external tool call.

pushd "%PROJECT_DIRECTORY%"

rem Run the requested Maven build through the project wrapper.

call mvnw.cmd clean verify %*
set "BUILD_EXIT_CODE=%ERRORLEVEL%"

rem Restore the caller working directory after the external tool call.

popd

rem Return exit status %BUILD_EXIT_CODE% to the calling process.

exit /b %BUILD_EXIT_CODE%
