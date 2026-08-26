@echo off
setlocal EnableExtensions DisableDelayedExpansion

rem -------------------------------------------------------------------------------------------------------------------
rem Project: eca-rule-engine-2
rem File:    generate-paper.bat
rem Version: 2.0
rem Date:    2025
rem Author:  Rohin Gosling
rem
rem Description:
rem
rem   Converts the stateless ECA rule-engine technical note from Markdown to LaTeX, compiles the LaTeX source twice
rem   with MiKTeX, and publishes the matching LaTeX and PDF files only after the complete build succeeds.
rem
rem Usage:
rem
rem   generate-paper.bat
rem   generate-paper.bat --no-pause
rem
rem -------------------------------------------------------------------------------------------------------------------

set "PAUSE_ON_EXIT=1"

rem Select the command path that matches the current runtime state.

if /I "%~1"=="--no-pause" set "PAUSE_ON_EXIT=0"

rem Iterate over the command output or path value required by this setup step.

for %%I in ("%~dp0.") do set "SCRIPTS_DIRECTORY=%%~fI"

rem Iterate over the command output or path value required by this setup step.

for %%I in ("%SCRIPTS_DIRECTORY%\..\..") do set "PRIVATE_DIRECTORY=%%~fI"

rem Iterate over the command output or path value required by this setup step.

for %%I in ("%PRIVATE_DIRECTORY%\..") do set "WORKSPACE_DIRECTORY=%%~fI"

set "PAPER_DIRECTORY=%PRIVATE_DIRECTORY%\docs\technical-note"
set "MARKDOWN_FILE=%PAPER_DIRECTORY%\stateless-eca-rule-engine.md"
set "LATEX_FILE=%PAPER_DIRECTORY%\stateless-eca-rule-engine.tex"
set "PDF_FILE=%PAPER_DIRECTORY%\stateless-eca-rule-engine.pdf"
set "CONVERTER_FILE=%SCRIPTS_DIRECTORY%\generate_paper.py"
set "BUILD_DIRECTORY=%WORKSPACE_DIRECTORY%\tmp\paper-generation\stateless-eca-rule-engine"
set "STAGED_LATEX_FILE=%BUILD_DIRECTORY%\stateless-eca-rule-engine.tex"
set "STAGED_PDF_FILE=%BUILD_DIRECTORY%\stateless-eca-rule-engine.pdf"
set "STAGED_LOG_FILE=%BUILD_DIRECTORY%\stateless-eca-rule-engine.log"
set "STAGED_FINAL_LATEX_FILE=%LATEX_FILE%.new"
set "STAGED_FINAL_PDF_FILE=%PDF_FILE%.new"
set "PREVIOUS_LATEX_FILE=%BUILD_DIRECTORY%\previous-stateless-eca-rule-engine.tex"
set "PREVIOUS_PDF_FILE=%BUILD_DIRECTORY%\previous-stateless-eca-rule-engine.pdf"

rem Handle the branch where the required file or directory is unavailable.

if not exist "%MARKDOWN_FILE%" goto markdown_not_found

rem Handle the branch where the required file or directory is unavailable.

if not exist "%CONVERTER_FILE%" goto converter_not_found

rem Locate a compatible Python runtime through the helper routine.

call :find_python

rem Handle the branch where the preceding command reported a failure.

if errorlevel 1 goto python_not_found

rem Locate the PDFLaTeX executable through the helper routine.

call :find_pdflatex

rem Handle the branch where the preceding command reported a failure.

if errorlevel 1 goto pdflatex_not_found

rem Handle the branch where the target file or directory is present.

if exist "%BUILD_DIRECTORY%" rmdir /s /q "%BUILD_DIRECTORY%"

rem Handle the branch where the target file or directory is present.

if exist "%BUILD_DIRECTORY%" goto build_directory_failed

rem Create the working directory required by the build.

mkdir "%BUILD_DIRECTORY%"

rem Handle the branch where the preceding command reported a failure.

if errorlevel 1 goto build_directory_failed

echo.
echo Converting Markdown to LaTeX ...
echo.

rem Handle the branch where the optional environment setting is available.

if defined PYTHON_LAUNCHER_ARGUMENT (

    rem Run the selected external tool with the prepared arguments.

    "%PYTHON_EXECUTABLE%" %PYTHON_LAUNCHER_ARGUMENT% "%CONVERTER_FILE%" --input "%MARKDOWN_FILE%" --output "%STAGED_LATEX_FILE%"

rem Use the alternative command path when the preceding condition is false.

) else (

    rem Run the selected external tool with the prepared arguments.

    "%PYTHON_EXECUTABLE%" "%CONVERTER_FILE%" --input "%MARKDOWN_FILE%" --output "%STAGED_LATEX_FILE%"
)

rem Handle the branch where the preceding command reported a failure.

if errorlevel 1 goto conversion_failed

rem Handle the branch where the required file or directory is unavailable.

if not exist "%STAGED_LATEX_FILE%" goto latex_not_produced

echo.
echo Compiling PDF - pass 1 of 2 ...
echo.

rem Enter the working directory required by the following external tool call.

pushd "%PAPER_DIRECTORY%"

rem Handle the branch where the preceding command reported a failure.

if errorlevel 1 goto paper_directory_failed

rem Run the selected external tool with the prepared arguments.

"%PDFLATEX_EXECUTABLE%" --enable-installer --interaction=nonstopmode --halt-on-error --output-directory="%BUILD_DIRECTORY%" "%STAGED_LATEX_FILE%"
set "LATEX_RESULT=%ERRORLEVEL%"

rem Restore the caller working directory after the external tool call.

popd

rem Select the command path that matches the current runtime state.

if not "%LATEX_RESULT%"=="0" goto first_pass_failed

echo.
echo Compiling PDF - pass 2 of 2 ...
echo.

rem Enter the working directory required by the following external tool call.

pushd "%PAPER_DIRECTORY%"

rem Handle the branch where the preceding command reported a failure.

if errorlevel 1 goto paper_directory_failed

rem Run the selected external tool with the prepared arguments.

"%PDFLATEX_EXECUTABLE%" --enable-installer --interaction=nonstopmode --halt-on-error --output-directory="%BUILD_DIRECTORY%" "%STAGED_LATEX_FILE%"
set "LATEX_RESULT=%ERRORLEVEL%"

rem Restore the caller working directory after the external tool call.

popd

rem Select the command path that matches the current runtime state.

if not "%LATEX_RESULT%"=="0" goto second_pass_failed

rem Handle the branch where the required file or directory is unavailable.

if not exist "%STAGED_PDF_FILE%" goto pdf_not_produced

rem Handle the branch where the required file or directory is unavailable.

if not exist "%STAGED_LOG_FILE%" goto log_not_produced

rem Scan the tool output for the status markers used by the next decision.

findstr /i /c:"undefined on input line" /c:"There were undefined references" /c:"Rerun to get" /c:"Label(s) may have changed" "%STAGED_LOG_FILE%" >nul

rem Handle the branch where the preceding command reported a failure.

if errorlevel 2 goto log_scan_failed

rem Handle the branch where the preceding command completed successfully.

if not errorlevel 1 goto unresolved_references

rem Copy the generated artifact into its staged publication location.

copy /y "%STAGED_LATEX_FILE%" "%STAGED_FINAL_LATEX_FILE%" >nul

rem Handle the branch where the preceding command reported a failure.

if errorlevel 1 goto publish_failed

rem Copy the generated artifact into its staged publication location.

copy /y "%STAGED_PDF_FILE%" "%STAGED_FINAL_PDF_FILE%" >nul

rem Handle the branch where the preceding command reported a failure.

if errorlevel 1 goto publish_failed

set "HAD_EXISTING_LATEX_FILE=0"
set "HAD_EXISTING_PDF_FILE=0"
set "LATEX_WAS_PUBLISHED=0"
set "PDF_WAS_PUBLISHED=0"

rem Handle the branch where the target file or directory is present.

if exist "%LATEX_FILE%" (

    rem Copy the generated artifact into its staged publication location.

    copy /y "%LATEX_FILE%" "%PREVIOUS_LATEX_FILE%" >nul

    rem Handle the branch where the preceding command reported a failure.

    if errorlevel 1 goto publish_failed
    set "HAD_EXISTING_LATEX_FILE=1"
)

rem Handle the branch where the target file or directory is present.

if exist "%PDF_FILE%" (

    rem Copy the generated artifact into its staged publication location.

    copy /y "%PDF_FILE%" "%PREVIOUS_PDF_FILE%" >nul

    rem Handle the branch where the preceding command reported a failure.

    if errorlevel 1 goto publish_failed
    set "HAD_EXISTING_PDF_FILE=1"
)

rem Move the staged artifact into its final publication location.

move /y "%STAGED_FINAL_LATEX_FILE%" "%LATEX_FILE%" >nul

rem Handle the branch where the preceding command reported a failure.

if errorlevel 1 goto publish_rollback
set "LATEX_WAS_PUBLISHED=1"

rem Move the staged artifact into its final publication location.

move /y "%STAGED_FINAL_PDF_FILE%" "%PDF_FILE%" >nul

rem Handle the branch where the preceding command reported a failure.

if errorlevel 1 goto publish_rollback
set "PDF_WAS_PUBLISHED=1"

rem Remove the temporary build directory after its contents are no longer needed.

rmdir /s /q "%BUILD_DIRECTORY%"
set "CLEANUP_WARNING=0"

rem Handle the branch where the target file or directory is present.

if exist "%BUILD_DIRECTORY%" set "CLEANUP_WARNING=1"

echo.
echo SUCCESS: The paper has been generated.
echo.
echo LaTeX: %LATEX_FILE%
echo PDF:   %PDF_FILE%
echo.

rem Select the command path that matches the current runtime state.

if "%CLEANUP_WARNING%"=="1" (
    echo WARNING: The temporary build directory could not be removed:
    echo %BUILD_DIRECTORY%
    echo.
)

set "FINAL_RESULT=0"
goto finish

:find_python

set "PYTHON_EXECUTABLE="
set "PYTHON_LAUNCHER_ARGUMENT="

rem Handle the branch where the target file or directory is present.

if exist "%WORKSPACE_DIRECTORY%\.venv\Scripts\python.exe" (

    rem Run the selected external tool with the prepared arguments.

    "%WORKSPACE_DIRECTORY%\.venv\Scripts\python.exe" -c "import sys; raise SystemExit(0 if sys.version_info >= (3, 10) else 1)" >nul 2>&1

    rem Handle the branch where the preceding command completed successfully.

    if not errorlevel 1 (
        set "PYTHON_EXECUTABLE=%WORKSPACE_DIRECTORY%\.venv\Scripts\python.exe"

        rem Return exit status 0 to the calling process.

        exit /b 0
    )
)

rem Search the executable path for a compatible tool installation.

where py >nul 2>&1

rem Handle the branch where the preceding command completed successfully.

if not errorlevel 1 (
    py -3 -c "import sys; raise SystemExit(0 if sys.version_info >= (3, 10) else 1)" >nul 2>&1

    rem Handle the branch where the preceding command completed successfully.

    if not errorlevel 1 (
        set "PYTHON_EXECUTABLE=py"
        set "PYTHON_LAUNCHER_ARGUMENT=-3"

        rem Return exit status 0 to the calling process.

        exit /b 0
    )
)

rem Search the executable path for a compatible tool installation.

where python >nul 2>&1

rem Handle the branch where the preceding command completed successfully.

if not errorlevel 1 (
    python -c "import sys; raise SystemExit(0 if sys.version_info >= (3, 10) else 1)" >nul 2>&1

    rem Handle the branch where the preceding command completed successfully.

    if not errorlevel 1 (
        set "PYTHON_EXECUTABLE=python"

        rem Return exit status 0 to the calling process.

        exit /b 0
    )
)

rem Return exit status 1 to the calling process.

exit /b 1

:find_pdflatex

set "PDFLATEX_EXECUTABLE="

rem Search the executable path for a compatible tool installation.

where pdflatex >nul 2>&1

rem Handle the branch where the preceding command completed successfully.

if not errorlevel 1 (
    set "PDFLATEX_EXECUTABLE=pdflatex"

    rem Return exit status 0 to the calling process.

    exit /b 0
)

rem Handle the branch where the target file or directory is present.

if exist "%LOCALAPPDATA%\Programs\MiKTeX\miktex\bin\x64\pdflatex.exe" (
    set "PDFLATEX_EXECUTABLE=%LOCALAPPDATA%\Programs\MiKTeX\miktex\bin\x64\pdflatex.exe"

    rem Return exit status 0 to the calling process.

    exit /b 0
)

rem Handle the branch where the target file or directory is present.

if exist "%ProgramFiles%\MiKTeX\miktex\bin\x64\pdflatex.exe" (
    set "PDFLATEX_EXECUTABLE=%ProgramFiles%\MiKTeX\miktex\bin\x64\pdflatex.exe"

    rem Return exit status 0 to the calling process.

    exit /b 0
)

rem Handle the branch where the target file or directory is present.

if exist "%ProgramFiles(x86)%\MiKTeX\miktex\bin\x64\pdflatex.exe" (
    set "PDFLATEX_EXECUTABLE=%ProgramFiles(x86)%\MiKTeX\miktex\bin\x64\pdflatex.exe"

    rem Return exit status 0 to the calling process.

    exit /b 0
)

rem Return exit status 1 to the calling process.

exit /b 1

:markdown_not_found

echo.
echo ERROR: Markdown input not found:
echo %MARKDOWN_FILE%
echo.
set "FINAL_RESULT=1"
goto finish

:converter_not_found

echo.
echo ERROR: Markdown-to-LaTeX converter not found:
echo %CONVERTER_FILE%
echo.
set "FINAL_RESULT=1"
goto finish

:python_not_found

echo.
echo ERROR: Python 3 could not be found.
echo Install Python 3.10 or newer, or make a compatible py.exe or python.exe available on PATH.
echo.
set "FINAL_RESULT=1"
goto finish

:pdflatex_not_found

echo.
echo ERROR: MiKTeX pdflatex.exe could not be found.
echo Install MiKTeX or make pdflatex.exe available on PATH.
echo.
set "FINAL_RESULT=1"
goto finish

:build_directory_failed

echo.
echo ERROR: The temporary build directory could not be prepared:
echo %BUILD_DIRECTORY%
echo.
set "FINAL_RESULT=1"
goto finish

:paper_directory_failed

echo.
echo ERROR: The paper directory could not be opened:
echo %PAPER_DIRECTORY%
echo.
echo The existing LaTeX and PDF files were not changed.
echo.
set "FINAL_RESULT=1"
goto finish

:conversion_failed

echo.
echo ERROR: Markdown-to-LaTeX conversion failed.
echo The existing LaTeX and PDF files were not changed.
echo.
set "FINAL_RESULT=1"
goto finish

:latex_not_produced

echo.
echo ERROR: Conversion completed without producing the staged LaTeX file.
echo The existing LaTeX and PDF files were not changed.
echo.
set "FINAL_RESULT=1"
goto finish

:first_pass_failed

echo.
echo ERROR: The first LaTeX pass failed.
echo Review the build log in:
echo %BUILD_DIRECTORY%
echo.
echo The existing LaTeX and PDF files were not changed.
echo.
set "FINAL_RESULT=1"
goto finish

:second_pass_failed

echo.
echo ERROR: The second LaTeX pass failed.
echo Review the build log in:
echo %BUILD_DIRECTORY%
echo.
echo The existing LaTeX and PDF files were not changed.
echo.
set "FINAL_RESULT=1"
goto finish

:pdf_not_produced

echo.
echo ERROR: LaTeX completed without producing the staged PDF file.
echo The existing LaTeX and PDF files were not changed.
echo.
set "FINAL_RESULT=1"
goto finish

:log_not_produced

echo.
echo ERROR: LaTeX completed without producing a build log.
echo The existing LaTeX and PDF files were not changed.
echo.
set "FINAL_RESULT=1"
goto finish

:unresolved_references

echo.
echo ERROR: Citations or cross-references remain unresolved after the second LaTeX pass.
echo Review the build log:
echo %STAGED_LOG_FILE%
echo.
echo The existing LaTeX and PDF files were not changed.
echo.
set "FINAL_RESULT=1"
goto finish

:log_scan_failed

echo.
echo ERROR: The final LaTeX build log could not be checked:
echo %STAGED_LOG_FILE%
echo.
echo The existing LaTeX and PDF files were not changed.
echo.
set "FINAL_RESULT=1"
goto finish

:publish_rollback

set "ROLLBACK_FAILED=0"

rem Select the command path that matches the current runtime state.

if "%LATEX_WAS_PUBLISHED%"=="1" (

    rem Select the command path that matches the current runtime state.

    if "%HAD_EXISTING_LATEX_FILE%"=="1" (

        rem Copy the generated artifact into its staged publication location.

        copy /y "%PREVIOUS_LATEX_FILE%" "%LATEX_FILE%" >nul

        rem Handle the branch where the preceding command reported a failure.

        if errorlevel 1 set "ROLLBACK_FAILED=1"

    rem Use the alternative command path when the preceding condition is false.

    ) else (

        rem Handle the branch where the target file or directory is present.

        if exist "%LATEX_FILE%" del /q "%LATEX_FILE%" >nul 2>&1

        rem Handle the branch where the target file or directory is present.

        if exist "%LATEX_FILE%" set "ROLLBACK_FAILED=1"
    )
)

rem Select the command path that matches the current runtime state.

if "%PDF_WAS_PUBLISHED%"=="1" (

    rem Select the command path that matches the current runtime state.

    if "%HAD_EXISTING_PDF_FILE%"=="1" (

        rem Copy the generated artifact into its staged publication location.

        copy /y "%PREVIOUS_PDF_FILE%" "%PDF_FILE%" >nul

        rem Handle the branch where the preceding command reported a failure.

        if errorlevel 1 set "ROLLBACK_FAILED=1"

    rem Use the alternative command path when the preceding condition is false.

    ) else (

        rem Handle the branch where the target file or directory is present.

        if exist "%PDF_FILE%" del /q "%PDF_FILE%" >nul 2>&1

        rem Handle the branch where the target file or directory is present.

        if exist "%PDF_FILE%" set "ROLLBACK_FAILED=1"
    )
)

rem Handle the branch where the target file or directory is present.

if exist "%STAGED_FINAL_LATEX_FILE%" del /q "%STAGED_FINAL_LATEX_FILE%" >nul 2>&1

rem Handle the branch where the target file or directory is present.

if exist "%STAGED_FINAL_PDF_FILE%" del /q "%STAGED_FINAL_PDF_FILE%" >nul 2>&1

echo.
echo ERROR: The generated LaTeX and PDF files could not both be published.

rem Select the command path that matches the current runtime state.

if "%ROLLBACK_FAILED%"=="0" (
    echo The previous output pair remains intact.

rem Use the alternative command path when the preceding condition is false.

) else (
    echo WARNING: Automatic rollback was incomplete. Inspect both output files before using them.
)

echo.
echo The successful staged build remains in:
echo %BUILD_DIRECTORY%
echo.
set "FINAL_RESULT=1"
goto finish

:publish_failed

rem Handle the branch where the target file or directory is present.

if exist "%STAGED_FINAL_LATEX_FILE%" del /q "%STAGED_FINAL_LATEX_FILE%" >nul 2>&1

rem Handle the branch where the target file or directory is present.

if exist "%STAGED_FINAL_PDF_FILE%" del /q "%STAGED_FINAL_PDF_FILE%" >nul 2>&1

echo.
echo ERROR: The generated paper could not be published to:
echo %PAPER_DIRECTORY%
echo.
echo The successful staged build remains in:
echo %BUILD_DIRECTORY%
echo.
set "FINAL_RESULT=1"
goto finish

:finish

rem Select the command path that matches the current runtime state.

if "%PAUSE_ON_EXIT%"=="1" pause

endlocal & exit /b %FINAL_RESULT%
