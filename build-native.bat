@echo off
setlocal

set "PROJECT_DIRECTORY=%~dp0"

rem Invoke the subordinate build routine required by this workflow.

call "%PROJECT_DIRECTORY%tools\native\build-native-server.bat"

rem Return the command status immediately when this failure condition is detected.

if errorlevel 1 exit /b %ERRORLEVEL%

rem Invoke the subordinate build routine required by this workflow.

call "%PROJECT_DIRECTORY%tools\native\build-native-client.bat"

rem Return exit status %ERRORLEVEL% to the calling process.

exit /b %ERRORLEVEL%
