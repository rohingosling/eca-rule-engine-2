@echo off
setlocal

pushd "%~dp0eca-web" || exit /b 1

call npm ci
if errorlevel 1 goto :failure

call npm run verify
if errorlevel 1 goto :failure

popd
exit /b 0

:failure
set "build_exit_code=%errorlevel%"
popd
exit /b %build_exit_code%
