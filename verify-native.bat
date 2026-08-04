@echo off
setlocal

set "PROJECT_DIRECTORY=%~dp0"
set "SERVER_EXECUTABLE=%PROJECT_DIRECTORY%eca-server\target\eca-server.exe"
set "CLIENT_EXECUTABLE=%PROJECT_DIRECTORY%eca-client\target\gluonfx\x86_64-windows\eca-client.exe"

rem Handle the branch where the required file or directory is unavailable.

if not exist "%SERVER_EXECUTABLE%" (
    echo ERROR: eca-server.exe was not found. Run build-native.bat first.

    rem Return exit status 1 to the calling process.

    exit /b 1
)

rem Handle the branch where the required file or directory is unavailable.

if not exist "%CLIENT_EXECUTABLE%" (
    echo ERROR: eca-client.exe was not found. Run build-native.bat first.

    rem Return exit status 1 to the calling process.

    exit /b 1
)

rem Enter the working directory required by the following external tool call.

pushd "%PROJECT_DIRECTORY%"

rem Run the PowerShell helper that performs this native-build step.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "tools\native\verify-native-artifacts.ps1"
set "VERIFY_EXIT_CODE=%ERRORLEVEL%"

rem Select the command path that matches the current runtime state.

if not "%VERIFY_EXIT_CODE%"=="0" (

    rem Restore the caller working directory after the external tool call.

    popd

    rem Return exit status %VERIFY_EXIT_CODE% to the calling process.

    exit /b %VERIFY_EXIT_CODE%
)

rem Run the PowerShell helper that performs this native-build step.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "tools\native\measure-native-performance.ps1"
set "PERFORMANCE_EXIT_CODE=%ERRORLEVEL%"

rem Restore the caller working directory after the external tool call.

popd

rem Return exit status %PERFORMANCE_EXIT_CODE% to the calling process.

exit /b %PERFORMANCE_EXIT_CODE%
