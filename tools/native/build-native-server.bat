@echo off
setlocal

set "SCRIPT_DIRECTORY=%~dp0"

rem Iterate over the command output or path value required by this setup step.

for %%I in ("%SCRIPT_DIRECTORY%..\..") do set "PROJECT_DIRECTORY=%%~fI"

rem Initialize the missing environment setting through the fallback branch.

if not defined SERVER_GRAALVM_HOME (
    set "SERVER_GRAALVM_HOME=%PROJECT_DIRECTORY%\.toolchains\graalvm\graalvm-jdk-25.0.4+7.1"
)

rem Handle the branch where the required file or directory is unavailable.

if not exist "%SERVER_GRAALVM_HOME%\bin\native-image.cmd" if not exist "%SERVER_GRAALVM_HOME%\bin\native-image.exe" (
    echo ERROR: The pinned Oracle GraalVM 25.0.4 native-image tool was not found below SERVER_GRAALVM_HOME.

    rem Return exit status 1 to the calling process.

    exit /b 1
)

rem Run the selected external tool with the prepared arguments.

"%SERVER_GRAALVM_HOME%\bin\java.exe" --version 2>&1 | findstr /C:"25.0.4" >nul

rem Handle the branch where the preceding command reported a failure.

if errorlevel 1 (
    echo ERROR: SERVER_GRAALVM_HOME must contain the pinned Oracle GraalVM 25.0.4 toolchain.

    rem Return exit status 1 to the calling process.

    exit /b 1
)

set "JAVA_HOME=%SERVER_GRAALVM_HOME%"
set "GRAALVM_HOME=%SERVER_GRAALVM_HOME%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "SERVER_RESOURCE=%PROJECT_DIRECTORY%\build\native-resources\eca-server.obj"

rem Enter the working directory required by the following external tool call.

pushd "%PROJECT_DIRECTORY%"

rem Run the PowerShell helper that performs this native-build step.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "tools\native\build-windows-resources.ps1" -Target Server
set "RESOURCE_EXIT_CODE=%ERRORLEVEL%"

rem Select the command path that matches the current runtime state.

if not "%RESOURCE_EXIT_CODE%"=="0" (

    rem Restore the caller working directory after the external tool call.

    popd

    rem Return exit status %RESOURCE_EXIT_CODE% to the calling process.

    exit /b %RESOURCE_EXIT_CODE%
)

rem Run the requested Maven build through the project wrapper.

call mvnw.cmd "-Deca.server.resource=%SERVER_RESOURCE%" -pl eca-server -am clean package -Pserver-native
set "BUILD_EXIT_CODE=%ERRORLEVEL%"

rem Select the command path that matches the current runtime state.

if not "%BUILD_EXIT_CODE%"=="0" (

    rem Restore the caller working directory after the external tool call.

    popd

    rem Return exit status %BUILD_EXIT_CODE% to the calling process.

    exit /b %BUILD_EXIT_CODE%
)

rem Handle the branch where the required file or directory is unavailable.

if not exist "eca-server\target\eca-server.exe" (
    echo ERROR: The native build did not produce eca-server\target\eca-server.exe.

    rem Restore the caller working directory after the external tool call.

    popd

    rem Return exit status 1 to the calling process.

    exit /b 1
)

rem Run the selected external tool with the prepared arguments.

"eca-server\target\eca-server.exe" --version
set "SMOKE_EXIT_CODE=%ERRORLEVEL%"

rem Select the command path that matches the current runtime state.

if not "%SMOKE_EXIT_CODE%"=="0" (

    rem Restore the caller working directory after the external tool call.

    popd

    rem Return exit status %SMOKE_EXIT_CODE% to the calling process.

    exit /b %SMOKE_EXIT_CODE%
)

rem Run the PowerShell helper that performs this native-build step.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "tools\native\smoke-native-server.ps1" ^
    -ExecutablePath "eca-server\target\eca-server.exe"
set "SMOKE_EXIT_CODE=%ERRORLEVEL%"

rem Restore the caller working directory after the external tool call.

popd

rem Return exit status %SMOKE_EXIT_CODE% to the calling process.

exit /b %SMOKE_EXIT_CODE%
