@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "SCRIPT_DIRECTORY=%~dp0"

rem Iterate over the command output or path value required by this setup step.

for %%I in ("%SCRIPT_DIRECTORY%..\..\..") do set "PROJECT_DIRECTORY=%%~fI"

rem Initialize the missing environment setting through the fallback branch.

if not defined CLIENT_GRAALVM_HOME (
    set "CLIENT_GRAALVM_HOME=%PROJECT_DIRECTORY%\.toolchains\gluon\graalvm-svm-java17-windows-gluon-22.1.0.1-Final"
)

rem Handle the branch where the required file or directory is unavailable.

if not exist "%CLIENT_GRAALVM_HOME%\bin\native-image.cmd" if not exist "%CLIENT_GRAALVM_HOME%\bin\native-image.exe" (
    echo ERROR: The pinned Gluon GraalVM 22.1.0.1 native-image tool was not found below CLIENT_GRAALVM_HOME.

    rem Return exit status 1 to the calling process.

    exit /b 1
)

rem Run the selected external tool with the prepared arguments.

"%CLIENT_GRAALVM_HOME%\bin\java.exe" --version 2>&1 | findstr /C:"17.0.3" >nul

rem Handle the branch where the preceding command reported a failure.

if errorlevel 1 (
    echo ERROR: CLIENT_GRAALVM_HOME must contain the pinned Gluon GraalVM 22.1.0.1 Java 17 toolchain.

    rem Return exit status 1 to the calling process.

    exit /b 1
)

rem Initialize the missing environment setting through the fallback branch.

if not defined VSCMD_ARG_TGT_ARCH (
    set "VSWHERE=%ProgramFiles(x86)%\Microsoft Visual Studio\Installer\vswhere.exe"

    rem Handle the branch where the required file or directory is unavailable.

    if not exist "!VSWHERE!" (
        echo ERROR: vswhere.exe was not found. Install Visual Studio with Desktop development with C++.

        rem Return exit status 1 to the calling process.

        exit /b 1
    )

    rem Iterate over the command output or path value required by this setup step.

    for /f "usebackq tokens=*" %%I in (`"!VSWHERE!" -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath`) do set "VISUAL_STUDIO_DIRECTORY=%%I"

    rem Initialize the missing environment setting through the fallback branch.

    if not defined VISUAL_STUDIO_DIRECTORY (
        echo ERROR: A Visual Studio x64 C++ toolchain was not found.

        rem Return exit status 1 to the calling process.

        exit /b 1
    )

    rem Invoke the subordinate build routine required by this workflow.

    call "!VISUAL_STUDIO_DIRECTORY!\Common7\Tools\VsDevCmd.bat" -arch=x64 -host_arch=x64

    rem Return the command status immediately when this failure condition is detected.

    if errorlevel 1 exit /b !ERRORLEVEL!
)

set "JAVA_HOME=%CLIENT_GRAALVM_HOME%"
set "GRAALVM_HOME=%CLIENT_GRAALVM_HOME%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "CLIENT_VERSION_RESOURCE=%PROJECT_DIRECTORY%\build\native-resources\eca-client.res"

rem Enter the working directory required by the following external tool call.

pushd "%PROJECT_DIRECTORY%"

rem Verify the pinned JavaFX static SDK archive before the native client build may consume it.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".github\scripts\native\prepare-javafx-static-sdk.ps1"
set "STATIC_SDK_EXIT_CODE=%ERRORLEVEL%"

rem Select the command path that matches the current runtime state.

if not "%STATIC_SDK_EXIT_CODE%"=="0" (

    rem Restore the caller working directory after the external tool call.

    popd

    rem Return exit status %STATIC_SDK_EXIT_CODE% to the calling process.

    exit /b %STATIC_SDK_EXIT_CODE%
)

rem Run the PowerShell helper that performs this native-build step.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".github\scripts\native\build-windows-resources.ps1" -Target Client
set "RESOURCE_EXIT_CODE=%ERRORLEVEL%"

rem Select the command path that matches the current runtime state.

if not "%RESOURCE_EXIT_CODE%"=="0" (

    rem Restore the caller working directory after the external tool call.

    popd

    rem Return exit status %RESOURCE_EXIT_CODE% to the calling process.

    exit /b %RESOURCE_EXIT_CODE%
)

rem Inspect each candidate and retain the first value that satisfies the nested condition.

for /f "usebackq tokens=*" %%I in (`where cvtres.exe`) do if not defined ECA_REAL_CVTRES set "ECA_REAL_CVTRES=%%I"

rem Initialize the missing environment setting through the fallback branch.

if not defined ECA_REAL_CVTRES (
    echo ERROR: The Visual Studio x64 resource converter was not found.

    rem Restore the caller working directory after the external tool call.

    popd

    rem Return exit status 1 to the calling process.

    exit /b 1
)

set "ECA_CLIENT_VERSION_RESOURCE=%CLIENT_VERSION_RESOURCE%"
set "CVTRES_WRAPPER=%PROJECT_DIRECTORY%\build\native-resources\cvtres.exe"
set "CVTRES_WRAPPER_OBJECT=%PROJECT_DIRECTORY%\build\native-resources\cvtres_wrapper.obj"

cl.exe /nologo /O2 /W4 /WX /TC ^
    "/Fo%CVTRES_WRAPPER_OBJECT%" ^
    "/Fe%CVTRES_WRAPPER%" ^
    ".github\scripts\native\cvtres_wrapper.c"

rem Handle the branch where the preceding command reported a failure.

if errorlevel 1 (
    echo ERROR: The native CVTRES forwarding executable could not be compiled.

    rem Restore the caller working directory after the external tool call.

    popd

    rem Return exit status 1 to the calling process.

    exit /b 1
)

set "PATH=%PROJECT_DIRECTORY%\build\native-resources;%PATH%"

rem Run the requested Maven build through the project wrapper.

call mvnw.cmd "-Denforcer.java.version=[17,18)" -pl eca-client -am clean install -DskipTests
set "FOUNDATION_BUILD_EXIT_CODE=%ERRORLEVEL%"

rem Select the command path that matches the current runtime state.

if not "%FOUNDATION_BUILD_EXIT_CODE%"=="0" (

    rem Restore the caller working directory after the external tool call.

    popd

    rem Return exit status %FOUNDATION_BUILD_EXIT_CODE% to the calling process.

    exit /b %FOUNDATION_BUILD_EXIT_CODE%
)

rem Run the requested Maven build through the project wrapper.

call mvnw.cmd "-Denforcer.java.version=[17,18)" -f eca-client\pom.xml -DskipTests package -Pclient-native com.gluonhq:gluonfx-maven-plugin:1.0.29:build
set "BUILD_EXIT_CODE=%ERRORLEVEL%"

rem Select the command path that matches the current runtime state.

if not "%BUILD_EXIT_CODE%"=="0" (

    rem Restore the caller working directory after the external tool call.

    popd

    rem Return exit status %BUILD_EXIT_CODE% to the calling process.

    exit /b %BUILD_EXIT_CODE%
)

rem Handle the branch where the required file or directory is unavailable.

if not exist "eca-client\target\gluonfx\x86_64-windows\eca-client.exe" (
    echo ERROR: The native build did not produce eca-client\target\gluonfx\x86_64-windows\eca-client.exe.

    rem Restore the caller working directory after the external tool call.

    popd

    rem Return exit status 1 to the calling process.

    exit /b 1
)

rem Restore the caller working directory after the external tool call.

popd

rem Return exit status 0 to the calling process.

exit /b 0
