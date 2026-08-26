#Requires -Version 5.1

#----------------------------------------------------------------------------------------------------------------------
# Project: ECA Rule Engine 2
# Version: 2.0
# Date:    2026
# Author:  Rohin Gosling
#
# Description:
#
#   Runs the Phase 10 candidate executables inside a disposable Windows Sandbox after installing the official
#   Microsoft Visual C++ v14 x64 Redistributable. The script exports clean-machine evidence through a mapped folder.
#
# TODO:
#
#   None.
#
#----------------------------------------------------------------------------------------------------------------------

[CmdletBinding()]
param
(
    [Parameter(Mandatory = $true)]
    [string] $CandidateDirectory,

    [Parameter(Mandatory = $true)]
    [string] $EvidenceDirectory,

    [switch] $ShutdownWhenComplete
)

$ErrorActionPreference = 'Stop'

function Add-ProcessModulePaths
{
    param
    (
        [Parameter(Mandatory = $true)]
        [System.Diagnostics.Process] $Process,

        [Parameter(Mandatory = $true)]
        [System.Collections.Generic.HashSet[string]] $ModulePaths
    )

    try
    {
        $Process.Refresh()

        foreach ( $processModule in $Process.Modules )
        {
            if ( -not [string]::IsNullOrWhiteSpace( $processModule.FileName ) )
            {
                [void] $ModulePaths.Add( $processModule.FileName )
            }
        }
    }

    catch [System.InvalidOperationException]
    {
        if ( -not $Process.HasExited )
        {
            throw
        }
    }
}

function Test-ValidMicrosoftSignature
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string] $Path
    )

    $signature = Get-AuthenticodeSignature -LiteralPath $Path

    return $signature.Status -eq 'Valid' -and
        $null -ne $signature.SignerCertificate -and
        $signature.SignerCertificate.Subject -match '(?:^|, )O=Microsoft Corporation(?:,|$)'
}

function Assert-LoadedModuleBoundary
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string[]] $ModulePaths,

        [Parameter(Mandatory = $true)]
        [string] $ExecutablePath,

        [switch] $AllowSignedMicrosoftShellModules
    )

    $resolvedExecutablePath = [System.IO.Path]::GetFullPath( $ExecutablePath )
    $windowsDirectoryPrefix = [System.IO.Path]::GetFullPath( $env:SystemRoot ).TrimEnd( '\' ) + '\'
    $systemDirectory        = [System.IO.Path]::GetFullPath( ( Join-Path $env:SystemRoot 'System32' ) ).TrimEnd( '\' )
    $windowsTextInputPrefix = [System.IO.Path]::GetFullPath(
        ( Join-Path $env:ProgramFiles 'Common Files\Microsoft Shared\Ink' )
    ).TrimEnd( '\' ) + '\'
    $approvedRuntimeNames   = @(
        'MSVCP140.dll',
        'MSVCP140_1.dll',
        'MSVCP140_2.dll',
        'VCRUNTIME140.dll',
        'VCRUNTIME140_1.dll'
    )
    $visualCppRuntimePattern =
        '^(?:CONCRT|MSVCP|MSVCR|VCCORLIB|VCOMP|VCRUNTIME)\d+(?:_[A-Za-z0-9]+)*\.dll$'

    foreach ( $modulePath in $ModulePaths )
    {
        $resolvedModulePath = [System.IO.Path]::GetFullPath( $modulePath )
        $moduleName         = Split-Path -Leaf $resolvedModulePath

        if ( $moduleName -match $visualCppRuntimePattern )
        {
            if ( $moduleName -notin $approvedRuntimeNames )
            {
                throw "An unapproved Microsoft Visual C++ runtime module was loaded: $resolvedModulePath"
            }

            $expectedRuntimePath = Join-Path $systemDirectory $moduleName

            if ( $resolvedModulePath -ne $expectedRuntimePath )
            {
                throw "A runtime module was loaded outside the central System32 installation: $resolvedModulePath"
            }
        }

        if (
            $resolvedModulePath -eq $resolvedExecutablePath -or
            $resolvedModulePath.StartsWith(
                $windowsDirectoryPrefix,
                [System.StringComparison]::OrdinalIgnoreCase
            )
        )
        {
            continue
        }

        if (
            $resolvedModulePath.StartsWith(
                $windowsTextInputPrefix,
                [System.StringComparison]::OrdinalIgnoreCase
            ) -and
            ( Test-ValidMicrosoftSignature -Path $resolvedModulePath )
        )
        {
            continue
        }

        if ( $AllowSignedMicrosoftShellModules -and ( Test-ValidMicrosoftSignature -Path $resolvedModulePath ) )
        {
            continue
        }

        throw "An application loaded a module outside the approved Windows boundary: $resolvedModulePath"
    }
}

function Get-CentralRuntimeEvidence
{
    param
    (
        [Parameter(Mandatory = $true)]
        [version] $MinimumVersion
    )

    $registryBaseKey = [Microsoft.Win32.RegistryKey]::OpenBaseKey(
        [Microsoft.Win32.RegistryHive]::LocalMachine,
        [Microsoft.Win32.RegistryView]::Registry64
    )
    $registryKey = $null

    try
    {
        $registryKey = $registryBaseKey.OpenSubKey(
            'SOFTWARE\Microsoft\VisualStudio\14.0\VC\Runtimes\x64'
        )

        if ( $null -eq $registryKey -or [int] $registryKey.GetValue( 'Installed', 0 ) -ne 1 )
        {
            throw 'The Microsoft Visual C++ v14 x64 Redistributable is not installed.'
        }

        $runtimeVersionText = [string] $registryKey.GetValue( 'Version', '' )
    }

    finally
    {
        if ( $null -ne $registryKey )
        {
            $registryKey.Dispose()
        }

        $registryBaseKey.Dispose()
    }

    if ( $runtimeVersionText -notmatch '^v?(?<Version>\d+\.\d+\.\d+(?:\.\d+)?)$' )
    {
        throw "The installed runtime version is invalid: '$runtimeVersionText'."
    }

    $runtimeVersion = [version] $Matches.get_Item( 'Version' )

    if ( $runtimeVersion -lt $MinimumVersion )
    {
        throw "The installed runtime $runtimeVersion is older than the release toolset $MinimumVersion."
    }

    $runtimeModuleEvidence = @(
        foreach (
            $runtimeModuleName in @(
                'MSVCP140.dll',
                'MSVCP140_1.dll',
                'MSVCP140_2.dll',
                'VCRUNTIME140.dll',
                'VCRUNTIME140_1.dll'
            )
        )
        {
            $runtimeModulePath = Join-Path $env:SystemRoot "System32\$runtimeModuleName"

            if ( -not ( Test-Path -LiteralPath $runtimeModulePath -PathType Leaf ) )
            {
                throw "An approved runtime module is missing: $runtimeModulePath"
            }

            if ( -not ( Test-ValidMicrosoftSignature -Path $runtimeModulePath ) )
            {
                throw "An approved runtime module has an invalid Microsoft signature: $runtimeModulePath"
            }

            $runtimeModuleItem = Get-Item -LiteralPath $runtimeModulePath

            [pscustomobject] @{
                Name      = $runtimeModuleName
                Path      = $runtimeModulePath
                Version   = $runtimeModuleItem.VersionInfo.FileVersion
                Signature = 'Valid - Microsoft Corporation'
            }
        }
    )

    return [pscustomobject] @{
        Version = $runtimeVersion
        Modules = $runtimeModuleEvidence
    }
}

function Get-ZoneIdentifierEvidence
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string] $Path
    )

    try
    {
        $zoneIdentifier = Get-Content -LiteralPath $Path -Stream 'Zone.Identifier' -Raw -ErrorAction Stop

        return $zoneIdentifier.Trim()
    }

    catch
    {
        return 'None or unavailable'
    }
}

function Write-FailureEvidence
{
    param
    (
        [Parameter(Mandatory = $true)]
        [System.Management.Automation.ErrorRecord] $ErrorRecord
    )

    @(
        'FAIL',
        $ErrorRecord.ToString(),
        $ErrorRecord.ScriptStackTrace
    ) | Set-Content -LiteralPath ( Join-Path $EvidenceDirectory 'status.txt' ) -Encoding UTF8
}

function Write-Progress
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string] $Message
    )

    ( '{0:o} {1}' -f ( Get-Date ), $Message ) |
        Set-Content -LiteralPath ( Join-Path $EvidenceDirectory 'progress.txt' ) -Encoding UTF8
}

$serverProcess     = $null
$clientProcess     = $null
$preferenceProcess = $null

try
{
    New-Item -ItemType Directory -Path $EvidenceDirectory -Force | Out-Null
    Write-Progress -Message 'Guest acceptance script started.'

    $manifestPath = Join-Path $CandidateDirectory 'candidate-manifest.json'
    $manifest     = Get-Content -LiteralPath $manifestPath -Raw -Encoding UTF8 | ConvertFrom-Json
    $sourceServerPath = Join-Path $CandidateDirectory 'eca-server.exe'
    $sourceClientPath = Join-Path $CandidateDirectory 'eca-client.exe'

    if (
        ( Get-FileHash -LiteralPath $sourceServerPath -Algorithm SHA256 ).Hash -ne $manifest.serverSha256 -or
        ( Get-FileHash -LiteralPath $sourceClientPath -Algorithm SHA256 ).Hash -ne $manifest.clientSha256
    )
    {
        throw 'The mapped candidate hashes do not match the host manifest.'
    }

    $operatingSystem = Get-CimInstance -ClassName Win32_OperatingSystem
    $operatingSystemBuild = [Environment]::OSVersion.Version.Build

    if ( -not [Environment]::Is64BitOperatingSystem -or $operatingSystemBuild -lt 22000 )
    {
        throw 'The clean-machine gate requires Windows 11 x64.'
    }

    $smartAppControlRegistryPath = 'HKLM:\SYSTEM\CurrentControlSet\Control\CI\Policy'
    $smartAppControlValue = Get-ItemPropertyValue `
        -LiteralPath $smartAppControlRegistryPath `
        -Name 'VerifiedAndReputablePolicyState' `
        -ErrorAction SilentlyContinue
    $smartAppControlState = switch ( $smartAppControlValue )
    {
        0 { 'Off' }
        1 { 'Enforce' }
        2 { 'Evaluation' }
        default { 'Not reported' }
    }

    @(
        "Smart App Control: $smartAppControlState",
        "PowerShell language mode: $($ExecutionContext.SessionState.LanguageMode)"
    ) | Set-Content -LiteralPath ( Join-Path $EvidenceDirectory 'application-control.txt' ) -Encoding UTF8

    $prohibitedToolCommands = @(
        Get-Command 'java.exe', 'javac.exe', 'native-image.cmd', 'cl.exe', 'dumpbin.exe' -ErrorAction SilentlyContinue
    )

    if ( $prohibitedToolCommands.Count -ne 0 )
    {
        throw 'The Windows Sandbox unexpectedly contains a Java, GraalVM, or Visual C++ development tool.'
    }

    $installerPath = Join-Path $env:TEMP 'vc_redist.x64.exe'
    Write-Progress -Message 'Downloading the official Microsoft x64 runtime installer.'
    Invoke-WebRequest `
        -Uri 'https://aka.ms/vc14/vc_redist.x64.exe' `
        -OutFile $installerPath `
        -TimeoutSec 60 `
        -UseBasicParsing

    if ( -not ( Test-ValidMicrosoftSignature -Path $installerPath ) )
    {
        throw 'The downloaded Microsoft x64 runtime installer signature is invalid.'
    }

    $installerHash    = ( Get-FileHash -LiteralPath $installerPath -Algorithm SHA256 ).Hash
    $installerVersion = ( Get-Item -LiteralPath $installerPath ).VersionInfo.ProductVersion
    Write-Progress -Message 'Installing the official Microsoft x64 runtime.'
    $installerProcess = Start-Process `
        -FilePath $installerPath `
        -ArgumentList '/install', '/quiet', '/norestart' `
        -Wait `
        -PassThru

    if ( $installerProcess.ExitCode -notin @( 0, 1638, 3010 ) )
    {
        throw "The Microsoft x64 runtime installer exited with code $($installerProcess.ExitCode)."
    }

    $restartRequired = $installerProcess.ExitCode -eq 3010

    if ( $restartRequired )
    {
        throw 'The Microsoft x64 runtime installer requires a restart; repeat this gate in a resettable VM.'
    }

    $runtimeEvidence = Get-CentralRuntimeEvidence -MinimumVersion ( [version] $manifest.msvcToolsetVersion )
    Write-Progress -Message 'The Microsoft x64 runtime is installed and verified.'
    $isolatedRoot    = Join-Path $env:LOCALAPPDATA 'ECA Phase 10 – 驗證'
    $serverDirectory = Join-Path $isolatedRoot 'server one file'
    $clientDirectory = Join-Path $isolatedRoot 'client one file'
    $serverDataDirectory = Join-Path $isolatedRoot 'server data'
    $processTemporaryDirectory = Join-Path $isolatedRoot 'process temporary'
    $serverPath = Join-Path $serverDirectory 'eca-server.exe'
    $clientPath = Join-Path $clientDirectory 'eca-client.exe'
    $serverOutputPath = Join-Path $EvidenceDirectory 'server.stdout.log'
    $serverErrorPath  = Join-Path $EvidenceDirectory 'server.stderr.log'
    $clientResultPath = Join-Path $EvidenceDirectory 'client-result.txt'
    $preferenceResultPath = Join-Path $EvidenceDirectory 'preference-result.txt'

    foreach (
        $directoryPath in @(
            $serverDirectory,
            $clientDirectory,
            $serverDataDirectory,
            $processTemporaryDirectory
        )
    )
    {
        New-Item -ItemType Directory -Path $directoryPath -Force | Out-Null
    }

    Copy-Item -LiteralPath $sourceServerPath -Destination $serverPath
    Copy-Item -LiteralPath $sourceClientPath -Destination $clientPath

    $sourceServerZone = Get-ZoneIdentifierEvidence -Path $sourceServerPath
    $sourceClientZone = Get-ZoneIdentifierEvidence -Path $sourceClientPath
    $copiedServerZone = Get-ZoneIdentifierEvidence -Path $serverPath
    $copiedClientZone = Get-ZoneIdentifierEvidence -Path $clientPath

    @(
        "Mapped server: $sourceServerZone",
        "Mapped client: $sourceClientZone",
        "Copied server before Unblock-File: $copiedServerZone",
        "Copied client before Unblock-File: $copiedClientZone"
    ) | Set-Content -LiteralPath ( Join-Path $EvidenceDirectory 'file-zone-evidence.txt' ) -Encoding UTF8

    Unblock-File -LiteralPath $serverPath
    Unblock-File -LiteralPath $clientPath
    Write-Progress -Message 'Candidate files were copied to their isolated launch directories.'

    $serverModulePaths      = [System.Collections.Generic.HashSet[string]]::new(
        [System.StringComparer]::OrdinalIgnoreCase
    )
    $clientModulePaths      = [System.Collections.Generic.HashSet[string]]::new(
        [System.StringComparer]::OrdinalIgnoreCase
    )
    $applicationModulePaths = [System.Collections.Generic.HashSet[string]]::new(
        [System.StringComparer]::OrdinalIgnoreCase
    )
    $previousLocalApplicationData = $env:LOCALAPPDATA
    $previousPath                 = $env:PATH
    $previousTemporaryDirectory   = $env:TEMP
    $previousTemporaryDirectory2  = $env:TMP

    try
    {
        $env:LOCALAPPDATA = $serverDataDirectory
        $env:PATH         = "$env:SystemRoot\System32;$env:SystemRoot"
        $env:TEMP         = $processTemporaryDirectory
        $env:TMP          = $processTemporaryDirectory

        $serverStopwatch = [System.Diagnostics.Stopwatch]::StartNew()
        $serverProcess = Start-Process `
            -FilePath $serverPath `
            -WorkingDirectory $serverDirectory `
            -WindowStyle Hidden `
            -PassThru `
            -RedirectStandardOutput $serverOutputPath `
            -RedirectStandardError $serverErrorPath
    }

    finally
    {
        $env:LOCALAPPDATA = $previousLocalApplicationData
        $env:PATH         = $previousPath
        $env:TEMP         = $previousTemporaryDirectory
        $env:TMP          = $previousTemporaryDirectory2
    }

    [void] $serverProcess.Handle
    Write-Progress -Message 'The no-argument native server is starting.'
    $serverDeadline = [DateTime]::UtcNow.AddSeconds( 30 )
    $serverLiveness = $null

    while ( $null -eq $serverLiveness -and [DateTime]::UtcNow -lt $serverDeadline )
    {
        try
        {
            $serverLiveness = Invoke-RestMethod `
                -Uri 'http://127.0.0.1:8080/api/v1/health/live' `
                -Method Get `
                -TimeoutSec 2
        }

        catch
        {
            if ( $serverProcess.HasExited )
            {
                break
            }

            Add-ProcessModulePaths -Process $serverProcess -ModulePaths $serverModulePaths
            Start-Sleep -Milliseconds 25
        }
    }

    Add-ProcessModulePaths -Process $serverProcess -ModulePaths $serverModulePaths
    $serverStopwatch.Stop()

    if ( $null -eq $serverLiveness -or $serverLiveness.status -ne 'UP' )
    {
        throw 'The no-argument native server did not become live on the documented endpoint.'
    }

    $clientArguments = (
        '--native-smoke="{0}" --native-smoke-phase=exercise --native-smoke-file-chooser=true ' +
        '--native-smoke-ui-automation=true --native-smoke-server-url="http://127.0.0.1:8080" ' +
        '--native-smoke-tls-url="https://www.microsoft.com/"'
    ) -f $clientResultPath

    try
    {
        $env:PATH = "$env:SystemRoot\System32;$env:SystemRoot"
        $env:TEMP = $processTemporaryDirectory
        $env:TMP  = $processTemporaryDirectory

        $clientStopwatch = [System.Diagnostics.Stopwatch]::StartNew()
        $clientProcess = Start-Process `
            -FilePath $clientPath `
            -ArgumentList $clientArguments `
            -WorkingDirectory $clientDirectory `
            -PassThru
    }

    finally
    {
        $env:PATH = $previousPath
        $env:TEMP = $previousTemporaryDirectory
        $env:TMP  = $previousTemporaryDirectory2
    }

    [void] $clientProcess.Handle
    Write-Progress -Message 'The rendered native client smoke is starting.'
    Add-Type -AssemblyName UIAutomationClient
    Add-Type -AssemblyName UIAutomationTypes

    $fileChooserObserved        = $false
    $javaFxDialogObserved       = $false
    $applicationWindowObserved = $false
    $accessibleElementCount     = 0
    $clientDeadline             = [DateTime]::UtcNow.AddSeconds( 60 )

    while (
        -not ( Test-Path -LiteralPath $clientResultPath ) -and
        -not $clientProcess.HasExited -and
        [DateTime]::UtcNow -lt $clientDeadline
    )
    {
        $processCondition = [System.Windows.Automation.PropertyCondition]::new(
            [System.Windows.Automation.AutomationElement]::ProcessIdProperty,
            $clientProcess.Id
        )
        $automationElements = [System.Windows.Automation.AutomationElement]::RootElement.FindAll(
            [System.Windows.Automation.TreeScope]::Descendants,
            $processCondition
        )

        foreach ( $automationElement in $automationElements )
        {
            if (
                $automationElement.Current.ControlType -eq [System.Windows.Automation.ControlType]::Window -and
                (
                    $automationElement.Current.Name -like '*ECA Rule Engine*' -or
                    $automationElement.Current.Name -eq 'About'
                )
            )
            {
                $applicationWindowObserved = $true
                $namedElements = @(
                    $automationElement.FindAll(
                        [System.Windows.Automation.TreeScope]::Descendants,
                        [System.Windows.Automation.Condition]::TrueCondition
                    ) |
                        Where-Object { -not [string]::IsNullOrWhiteSpace( $_.Current.Name ) }
                )
                $accessibleElementCount = [Math]::Max( $accessibleElementCount, $namedElements.Count )
            }

            if ( $automationElement.Current.Name -eq 'Open ECA Model' )
            {
                $windowPattern = $automationElement.GetCurrentPattern(
                    [System.Windows.Automation.WindowPattern]::Pattern
                )
                $windowPattern.Close()
                $fileChooserObserved = $true
            }

            if ( $automationElement.Current.Name -eq 'About' -and $accessibleElementCount -ge 5 )
            {
                $windowPattern = $automationElement.GetCurrentPattern(
                    [System.Windows.Automation.WindowPattern]::Pattern
                )
                $windowPattern.Close()
                $javaFxDialogObserved = $true
            }
        }

        Add-ProcessModulePaths -Process $clientProcess -ModulePaths $clientModulePaths

        if ( -not $fileChooserObserved -and -not $javaFxDialogObserved )
        {
            Add-ProcessModulePaths -Process $clientProcess -ModulePaths $applicationModulePaths
        }

        Start-Sleep -Milliseconds 50
    }

    Add-ProcessModulePaths -Process $clientProcess -ModulePaths $clientModulePaths
    $clientStopwatch.Stop()

    if ( -not ( Test-Path -LiteralPath $clientResultPath ) )
    {
        throw 'The rendered native client smoke did not finish within sixty seconds.'
    }

    $clientResult = ( Get-Content -LiteralPath $clientResultPath -Raw ).Trim()

    if ( $clientResult -notmatch '^PASS\|(?<OriginalTheme>light|dark)\|(?<PersistedTheme>light|dark)$' )
    {
        throw "The rendered native client smoke failed: $clientResult"
    }

    if ( -not $fileChooserObserved -or -not $javaFxDialogObserved )
    {
        throw 'Windows UI Automation did not interact with both native client dialogs.'
    }

    if ( -not $applicationWindowObserved -or $accessibleElementCount -lt 5 )
    {
        throw 'Windows UI Automation did not expose the JavaFX client controls.'
    }

    if ( -not $clientProcess.WaitForExit( 10000 ) -or $clientProcess.ExitCode -ne 0 )
    {
        throw 'The native client did not exit successfully after its rendered smoke.'
    }

    $originalTheme  = $Matches.get_Item( 'OriginalTheme' )
    $persistedTheme = $Matches.get_Item( 'PersistedTheme' )
    $preferenceArguments = (
        '--native-smoke="{0}" --native-smoke-phase=read-preferences ' +
        '--native-smoke-theme={1} --native-smoke-restore-theme={2}'
    ) -f $preferenceResultPath, $persistedTheme, $originalTheme

    try
    {
        $env:PATH = "$env:SystemRoot\System32;$env:SystemRoot"
        $env:TEMP = $processTemporaryDirectory
        $env:TMP  = $processTemporaryDirectory

        $preferenceProcess = Start-Process `
            -FilePath $clientPath `
            -ArgumentList $preferenceArguments `
            -WorkingDirectory $clientDirectory `
            -PassThru
    }

    finally
    {
        $env:PATH = $previousPath
        $env:TEMP = $previousTemporaryDirectory
        $env:TMP  = $previousTemporaryDirectory2
    }

    [void] $preferenceProcess.Handle
    Write-Progress -Message 'The native client preferences relaunch is starting.'
    $preferenceDeadline = [DateTime]::UtcNow.AddSeconds( 30 )

    while (
        -not ( Test-Path -LiteralPath $preferenceResultPath ) -and
        -not $preferenceProcess.HasExited -and
        [DateTime]::UtcNow -lt $preferenceDeadline
    )
    {
        Add-ProcessModulePaths -Process $preferenceProcess -ModulePaths $clientModulePaths
        Start-Sleep -Milliseconds 25
    }

    if (
        -not ( Test-Path -LiteralPath $preferenceResultPath ) -or
        ( Get-Content -LiteralPath $preferenceResultPath -Raw ).Trim() -ne 'PASS' -or
        -not $preferenceProcess.WaitForExit( 10000 ) -or
        $preferenceProcess.ExitCode -ne 0
    )
    {
        throw 'The native client did not restore preferences across a relaunch.'
    }

    $previousLocalApplicationData = $env:LOCALAPPDATA

    try
    {
        $env:LOCALAPPDATA = $serverDataDirectory

        & $serverPath stop --server-url 'http://127.0.0.1:8080' --timeout 5 2>&1 | Out-Null
    }

    finally
    {
        $env:LOCALAPPDATA = $previousLocalApplicationData
    }

    if ( $LASTEXITCODE -ne 0 -or -not $serverProcess.WaitForExit( 10000 ) )
    {
        throw 'The no-argument native server did not stop successfully.'
    }

    Assert-LoadedModuleBoundary `
        -ModulePaths @( $serverModulePaths ) `
        -ExecutablePath $serverPath
    Assert-LoadedModuleBoundary `
        -ModulePaths @( $applicationModulePaths ) `
        -ExecutablePath $clientPath
    Assert-LoadedModuleBoundary `
        -ModulePaths @( $clientModulePaths ) `
        -ExecutablePath $clientPath `
        -AllowSignedMicrosoftShellModules

    $serverFiles = @( Get-ChildItem -LiteralPath $serverDirectory -Force )
    $clientFiles = @( Get-ChildItem -LiteralPath $clientDirectory -Force )
    $temporaryFiles = @( Get-ChildItem -LiteralPath $processTemporaryDirectory -Force -Recurse )

    if ( $serverFiles.Count -ne 1 -or $serverFiles.GetValue( 0 ).Name -cne 'eca-server.exe' )
    {
        throw 'The clean server launch directory does not contain exactly one ECA executable.'
    }

    if ( $clientFiles.Count -ne 1 -or $clientFiles.GetValue( 0 ).Name -cne 'eca-client.exe' )
    {
        throw 'The clean client launch directory does not contain exactly one ECA executable.'
    }

    if ( $temporaryFiles.Count -ne 0 )
    {
        throw 'An ECA executable extracted or retained a temporary runtime artifact.'
    }

    @( $serverModulePaths | Sort-Object ) |
        Set-Content -LiteralPath ( Join-Path $EvidenceDirectory 'server-loaded-modules.txt' ) -Encoding UTF8
    @( $clientModulePaths | Sort-Object ) |
        Set-Content -LiteralPath ( Join-Path $EvidenceDirectory 'client-loaded-modules.txt' ) -Encoding UTF8

    $runtimeModuleRows = $runtimeEvidence.Modules | ForEach-Object {
        "| ``$($_.Name)`` | $($_.Version) | $($_.Signature) | ``$($_.Path)`` |"
    }
    $reportLines = @(
        '# Clean Windows 11 Native Acceptance',
        '',
        '| Field | Value |',
        '|---|---|',
        "| Date | $( Get-Date -Format 'yyyy-MM-dd HH:mm:ss K' ) |",
        "| Windows image | $($operatingSystem.Caption) $($operatingSystem.Version), build $operatingSystemBuild |",
        '| Architecture | x64 |',
        "| Smart App Control | $smartAppControlState |",
        '| Java, GraalVM, Visual Studio, and MSVC tools before launch | Not present |',
        '| Microsoft runtime URL | `https://aka.ms/vc14/vc_redist.x64.exe` |',
        "| Microsoft runtime installer SHA-256 | ``$installerHash`` |",
        "| Microsoft runtime installer version | $installerVersion |",
        '| Microsoft runtime installer signature | Valid - Microsoft Corporation |',
        "| Microsoft runtime installer exit code | $($installerProcess.ExitCode) |",
        '| Restart required | No |',
        "| Installed Microsoft x64 runtime | $($runtimeEvidence.Version) |",
        "| Release MSVC toolset | $($manifest.msvcToolsetVersion) |",
        "| Server SHA-256 | ``$($manifest.serverSha256)`` |",
        "| Client SHA-256 | ``$($manifest.clientSha256)`` |",
        "| Server no-argument cold start | $([Math]::Round( $serverStopwatch.Elapsed.TotalMilliseconds, 1 )) ms |",
        "| Client rendered smoke | $([Math]::Round( $clientStopwatch.Elapsed.TotalMilliseconds, 1 )) ms |",
        "| Named UI Automation controls | $accessibleElementCount |",
        '',
        '## Gate Results',
        '',
        '| Gate | Result |',
        '|---|---|',
        '| Three-step order: runtime, both candidates, server then client | Pass |',
        '| Exact candidate hashes | Pass |',
        '| No Java, GraalVM, Visual Studio, or MSVC build tools | Pass |',
        '| Current centrally installed signed Microsoft x64 runtime | Pass |',
        '| No-argument server launch and health | Pass |',
        '| Rendered JavaFX client, dialogs, file chooser, and accessibility | Pass |',
        '| Native client-to-server HTTP interoperability | Pass |',
        '| Native client TLS round trip | Pass |',
        '| Both themes and preferences across relaunch | Pass |',
        '| One executable in each launch directory | Pass |',
        '| No temporary runtime extraction | Pass |',
        '| Loaded-module boundary | Pass |',
        '',
        '## Microsoft Runtime Modules',
        '',
        '| Module | Version | Signature | Central path |',
        '|---|---|---|---|',
        $runtimeModuleRows,
        '',
        'Full loaded-module paths and server output are retained beside this report.'
    )

    $reportLines |
        Set-Content -LiteralPath ( Join-Path $EvidenceDirectory 'clean-windows-11.md' ) -Encoding UTF8
    Write-Progress -Message 'All clean Windows 11 acceptance gates passed.'
    'PASS' | Set-Content -LiteralPath ( Join-Path $EvidenceDirectory 'status.txt' ) -Encoding ASCII

    if ( $ShutdownWhenComplete )
    {
        Start-Process -FilePath 'shutdown.exe' -ArgumentList '/s', '/t', '5' -WindowStyle Hidden | Out-Null
    }
}

catch
{
    Write-FailureEvidence -ErrorRecord $_

    try
    {
        Get-WinEvent `
            -FilterHashtable @{
                LogName   = 'Microsoft-Windows-CodeIntegrity/Operational'
                StartTime = ( Get-Date ).AddMinutes( -15 )
            } `
            -ErrorAction Stop |
            Select-Object TimeCreated, Id, LevelDisplayName, Message |
            Format-List |
            Out-String -Width 240 |
            Set-Content `
                -LiteralPath ( Join-Path $EvidenceDirectory 'application-control-events.txt' ) `
                -Encoding UTF8
    }

    catch
    {
        'Code Integrity events were unavailable.' |
            Set-Content `
                -LiteralPath ( Join-Path $EvidenceDirectory 'application-control-events.txt' ) `
                -Encoding UTF8
    }

    if ( $ShutdownWhenComplete )
    {
        Start-Process -FilePath 'shutdown.exe' -ArgumentList '/s', '/t', '5' -WindowStyle Hidden | Out-Null
    }
    throw
}

finally
{
    foreach ( $process in @( $clientProcess, $preferenceProcess, $serverProcess ) )
    {
        if ( $null -ne $process -and -not $process.HasExited )
        {
            Stop-Process -Id $process.Id -Force
            $process.WaitForExit()
        }

        if ( $null -ne $process )
        {
            $process.Dispose()
        }
    }
}
