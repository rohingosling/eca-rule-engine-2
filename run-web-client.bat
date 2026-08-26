@echo off
setlocal EnableDelayedExpansion

set "PROJECT_DIRECTORY=%~dp0"
set "SUBSTITUTED_DRIVE_TARGET="

rem Resolve a substituted launch drive to its backing path, so that the web tooling and the Java tooling agree on one
rem project location.

for /f "tokens=2 delims=>" %%D in ('subst 2^>nul ^| findstr /b /i /c:"%~d0"') do (
    for /f "tokens=* delims= " %%P in ("%%D") do set "SUBSTITUTED_DRIVE_TARGET=%%P"
)

if defined SUBSTITUTED_DRIVE_TARGET set "PROJECT_DIRECTORY=!SUBSTITUTED_DRIVE_TARGET!%~p0"

set "WEB_DIRECTORY=%PROJECT_DIRECTORY%eca-web"
set "MODE=dev"

rem Read the requested serving mode. The development server rebuilds on every edit, while the preview server serves
rem the production artifact exactly as GitHub Pages serves it.

if not "%~1" == "" (
    if /i "%~1" == "dev" (
        set "MODE=dev"
    ) else if /i "%~1" == "preview" (
        set "MODE=preview"
    ) else (
        echo ERROR: Unknown mode "%~1". Use "dev" ^(default^) or "preview".

        rem Return exit status 1 to the calling process.

        exit /b 1
    )
)

rem Handle the branch where the web project is unavailable.

if not exist "%WEB_DIRECTORY%\package.json" (
    echo ERROR: The web project was not found at "%WEB_DIRECTORY%".

    rem Return exit status 1 to the calling process.

    exit /b 1
)

rem Enter the working directory required by the following external tool calls.

pushd "%WEB_DIRECTORY%" || exit /b 1

rem Install the pinned dependency closure on a first run. An existing closure is left untouched, so that repeated
rem launches start the server immediately.

if not exist "%WEB_DIRECTORY%\node_modules" (
    echo Installing web dependencies...

    call npm ci
    if errorlevel 1 goto :failure
)

rem Build the production artifact before serving it. The development server compiles on demand and needs no build.

if /i "%MODE%" == "preview" (
    echo Building the production artifact...

    call npm run build
    if errorlevel 1 goto :failure
)

echo.
echo Starting the %MODE% server. The application is served beneath /eca-rule-engine-2/.
echo Press Ctrl+C to stop it.
echo.

call npm run %MODE% -- --open
if errorlevel 1 goto :failure

popd

rem Return exit status 0 to the calling process.

exit /b 0

:failure
set "RUN_EXIT_CODE=%errorlevel%"

rem Restore the caller working directory after the external tool calls.

popd

rem Return exit status %RUN_EXIT_CODE% to the calling process.

exit /b %RUN_EXIT_CODE%
