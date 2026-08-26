#Requires -Version 5.1

#----------------------------------------------------------------------------------------------------------------------
# Project: ECA Rule Engine 2
# Version: 2.0
# Date:    2025
# Author:  Rohin Gosling
#
# Description:
#
#   Verifies native executable identity, imports, checksums, exact filenames, isolated one-file execution, server
#   startup, and the automated JavaFX resource, renderer, dialog, icon, TLS, and file-chooser smoke path.
#
# TODO:
#
#   None.
#
#----------------------------------------------------------------------------------------------------------------------

[CmdletBinding()]
param
(
    [string] $ServerExecutablePath,
    [string] $ClientExecutablePath,
    [string] $ReportDirectory,
    [switch] $SkipClientLaunch,
    [switch] $UseAsciiTemporaryPath
)

$ErrorActionPreference = 'Stop'

function Resolve-Dumpbin
{
    # Initialize the dumpbin command by applying get command.

    $dumpbinCommand = Get-Command 'dumpbin.exe' -ErrorAction SilentlyContinue

    # Handle the branch where dumpbin command is available.

    if ( $null -ne $dumpbinCommand )
    {
        # Return the prepared result to the caller.

        return $dumpbinCommand.Source
    }

    # Prepare the visual studio root and candidates values required by the resolve dumpbin operation.

    $visualStudioRoot = Join-Path $env:ProgramFiles 'Microsoft Visual Studio'
    $candidates       = @(

        # Perform the get child item, where object, and sort object calls required by the resolve dumpbin operation.

        Get-ChildItem `
            -LiteralPath $visualStudioRoot `
            -Recurse `
            -Filter 'dumpbin.exe' `
            -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -match '\\Hostx64\\x64\\|\\Hostx86\\x64\\' } |
            Sort-Object FullName -Descending
    )

    # Handle the branch where candidates.count equals 0.

    if ( $candidates.Count -eq 0 )
    {
        throw 'dumpbin.exe was not found. Install the Visual Studio x64 C++ build tools.'
    }

    # Return the prepared result to the caller.

    return $candidates.GetValue( 0 ).FullName
}

function Remove-VerifiedTemporaryDirectory
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string] $TemporaryDirectory,

        [Parameter(Mandatory = $true)]
        [string] $TemporaryRoot
    )

    $resolvedTemporaryDirectory = [System.IO.Path]::GetFullPath( $TemporaryDirectory )
    $resolvedTemporaryRoot      = [System.IO.Path]::GetFullPath( $TemporaryRoot )

    if (
        -not $resolvedTemporaryDirectory.StartsWith(
            $resolvedTemporaryRoot,
            [System.StringComparison]::OrdinalIgnoreCase
        )
    )
    {
        throw "Refusing to remove a temporary directory outside the verified root: $resolvedTemporaryDirectory"
    }

    for ( $attempt = 1; $attempt -le 10; $attempt++ )
    {
        if ( -not ( Test-Path -LiteralPath $resolvedTemporaryDirectory ) )
        {
            return
        }

        try
        {
            Remove-Item -LiteralPath $resolvedTemporaryDirectory -Recurse -Force -ErrorAction Stop

            return
        }

        catch
        {
            if ( $attempt -eq 10 )
            {
                throw
            }

            Start-Sleep -Milliseconds 100
        }
    }
}

function Get-NativeDependencies
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string] $DumpbinPath,

        [Parameter(Mandatory = $true)]
        [string] $ExecutablePath,

        [Parameter(Mandatory = $true)]
        [string] $OutputPath
    )

    # Initialize the output by applying command.

    $output = @( & $DumpbinPath /DEPENDENTS $ExecutablePath 2>&1 )

    # Handle the branch where lastexitcode differs from 0.

    if ( $LASTEXITCODE -ne 0 )
    {
        throw "dumpbin /DEPENDENTS failed for '$ExecutablePath' with exit code $LASTEXITCODE."
    }

    # Perform the set content calls required by the get native dependencies operation.

    $output | Set-Content -LiteralPath $OutputPath -Encoding UTF8

    # Initialize the dependencies by applying get item.

    $dependencies = @(

        # Process each value supplied by the selected collection.

        foreach ( $line in $output )
        {
            # Handle the branch where line match s a za z0 9. .dll s.

            if ( $line -match '^\s+([A-Za-z0-9._-]+\.dll)\s*$' )
            {
                # Perform the get item calls required by the get native dependencies operation.

                $Matches.get_Item( 1 )
            }
        }
    )

    # Return the result produced by sort object.

    return @( $dependencies | Sort-Object -Unique )
}

function Assert-X64Executable
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string] $DumpbinPath,

        [Parameter(Mandatory = $true)]
        [string] $ExecutablePath
    )

    # Initialize the headers by applying command.

    $headers = @( & $DumpbinPath /HEADERS $ExecutablePath 2>&1 )

    # Handle the branch where lastexitcode differs from 0.

    if ( $LASTEXITCODE -ne 0 )
    {
        throw "dumpbin /HEADERS failed for '$ExecutablePath' with exit code $LASTEXITCODE."
    }

    # Handle the branch where not headers match machine x64.

    if ( -not ( $headers -match 'machine \(x64\)' ) )
    {
        throw "The executable is not a Windows x64 image: $ExecutablePath"
    }
}

function Get-ApprovedVisualCppRuntimeModuleNames
{
    return @(
        'MSVCP140.dll',
        'MSVCP140_1.dll',
        'MSVCP140_2.dll',
        'VCRUNTIME140.dll',
        'VCRUNTIME140_1.dll'
    )
}

function Get-MsvcToolsetVersion
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string] $DumpbinPath
    )

    $toolsetVersionMatch = [regex]::Match(
        $DumpbinPath,
        '\\VC\\Tools\\MSVC\\(?<Version>\d+\.\d+\.\d+)\\',
        [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
    )

    if ( -not $toolsetVersionMatch.Success )
    {
        throw "The selected dumpbin path does not identify its MSVC toolset version: $DumpbinPath"
    }

    return [version] $toolsetVersionMatch.Groups.get_Item( 'Version' ).Value
}

function Get-VisualCppRuntimeEvidence
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string] $DumpbinPath
    )

    if ( [string]::IsNullOrWhiteSpace( $env:SystemRoot ) )
    {
        throw 'SystemRoot is required to verify the Microsoft Visual C++ runtime.'
    }

    $runtimeRegistryPath = 'SOFTWARE\Microsoft\VisualStudio\14.0\VC\Runtimes\x64'
    $registryBaseKey     = [Microsoft.Win32.RegistryKey]::OpenBaseKey(
        [Microsoft.Win32.RegistryHive]::LocalMachine,
        [Microsoft.Win32.RegistryView]::Registry64
    )
    $runtimeRegistryKey = $null

    try
    {
        $runtimeRegistryKey = $registryBaseKey.OpenSubKey( $runtimeRegistryPath )

        if ( $null -eq $runtimeRegistryKey )
        {
            throw 'The Microsoft Visual C++ v14 x64 Redistributable is not installed.'
        }

        if ( [int] $runtimeRegistryKey.GetValue( 'Installed', 0 ) -ne 1 )
        {
            throw 'The Microsoft Visual C++ v14 x64 Redistributable is not marked as installed.'
        }

        $runtimeVersionText = [string] $runtimeRegistryKey.GetValue( 'Version', '' )
    }

    finally
    {
        if ( $null -ne $runtimeRegistryKey )
        {
            $runtimeRegistryKey.Dispose()
        }

        $registryBaseKey.Dispose()
    }

    if ( $runtimeVersionText -notmatch '^v?(?<Version>\d+\.\d+\.\d+(?:\.\d+)?)$' )
    {
        throw "The installed Microsoft Visual C++ runtime version is invalid: '$runtimeVersionText'."
    }

    $runtimeVersion = [version] $Matches.get_Item( 'Version' )
    $toolsetVersion = Get-MsvcToolsetVersion -DumpbinPath $DumpbinPath

    if ( $runtimeVersion -lt $toolsetVersion )
    {
        throw (
            "The installed Microsoft Visual C++ runtime $runtimeVersion is older than the release MSVC toolset " +
            "$toolsetVersion. Install or update the latest supported x64 Redistributable."
        )
    }

    $systemDirectory = [System.IO.Path]::GetFullPath( ( Join-Path $env:SystemRoot 'System32' ) ).TrimEnd( '\' )
    $runtimeModules  = @(
        foreach ( $runtimeModuleName in Get-ApprovedVisualCppRuntimeModuleNames )
        {
            $runtimeModulePath = Join-Path $systemDirectory $runtimeModuleName

            if ( -not ( Test-Path -LiteralPath $runtimeModulePath -PathType Leaf ) )
            {
                throw "The approved Microsoft Visual C++ runtime module is unavailable: $runtimeModulePath"
            }

            $runtimeModuleItem      = Get-Item -LiteralPath $runtimeModulePath
            $runtimeModuleSignature = Get-AuthenticodeSignature -LiteralPath $runtimeModulePath

            if (
                $runtimeModuleSignature.Status -ne 'Valid' -or
                $null -eq $runtimeModuleSignature.SignerCertificate -or
                $runtimeModuleSignature.SignerCertificate.Subject -notmatch '(?:^|, )O=Microsoft Corporation(?:,|$)'
            )
            {
                throw "The approved runtime module does not have a valid Microsoft signature: $runtimeModulePath"
            }

            $runtimeModuleVersion = [version] $runtimeModuleItem.VersionInfo.FileVersion

            if ( $runtimeModuleVersion -lt $toolsetVersion )
            {
                throw (
                    "The runtime module $runtimeModuleName version $runtimeModuleVersion is older than the release " +
                    "MSVC toolset $toolsetVersion."
                )
            }

            [pscustomobject] @{
                Name          = $runtimeModuleName
                Path          = $runtimeModulePath
                Version       = $runtimeModuleVersion
                Signature     = $runtimeModuleSignature.Status
                SignerSubject = $runtimeModuleSignature.SignerCertificate.Subject
            }
        }
    )

    return [pscustomobject] @{
        RegistryVersion = $runtimeVersion
        ToolsetVersion  = $toolsetVersion
        Modules         = $runtimeModules
    }
}

function Assert-AllowedDependencies
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string[]] $Dependencies,

        [Parameter(Mandatory = $true)]
        [string] $ArtifactName
    )

    $visualCppRuntimeDependencyPattern =
        '^(?:CONCRT|MSVCP|MSVCR|VCCORLIB|VCOMP|VCRUNTIME)\d+(?:_[A-Za-z0-9]+)*\.dll$'
    $approvedRuntimeDependencies = Get-ApprovedVisualCppRuntimeModuleNames
    $unapprovedRuntimeDependencies = @(
        $Dependencies |
            Where-Object {
                $_ -match $visualCppRuntimeDependencyPattern -and
                    $_ -notin $approvedRuntimeDependencies
            }
    )

    if ( $unapprovedRuntimeDependencies.Count -ne 0 )
    {
        throw (
            "$ArtifactName imports an unapproved Microsoft Visual C++ runtime module: " +
            ( $unapprovedRuntimeDependencies -join ', ' )
        )
    }

    $allowedDependencies = @(
        'ADVAPI32.dll',
        'api-ms-win-crt-convert-l1-1-0.dll',
        'api-ms-win-crt-environment-l1-1-0.dll',
        'api-ms-win-crt-filesystem-l1-1-0.dll',
        'api-ms-win-crt-heap-l1-1-0.dll',
        'api-ms-win-crt-locale-l1-1-0.dll',
        'api-ms-win-crt-math-l1-1-0.dll',
        'api-ms-win-crt-runtime-l1-1-0.dll',
        'api-ms-win-crt-stdio-l1-1-0.dll',
        'api-ms-win-crt-string-l1-1-0.dll',
        'COMDLG32.dll',
        'CRYPT32.dll',
        'dwmapi.dll',
        'GDI32.dll',
        'IMM32.dll',
        'IPHLPAPI.DLL',
        'KERNEL32.dll',
        'MSVCP140.dll',
        'MSVCP140_1.dll',
        'MSVCP140_2.dll',
        'MSWSOCK.dll',
        'ncrypt.dll',
        'ole32.dll',
        'OLEAUT32.dll',
        'PSAPI.DLL',
        'Secur32.dll',
        'SHELL32.dll',
        'UIAutomationCore.DLL',
        'urlmon.dll',
        'USER32.dll',
        'USERENV.dll',
        'VERSION.dll',
        'VCRUNTIME140.dll',
        'VCRUNTIME140_1.dll',
        'WINHTTP.dll',
        'WINMM.dll',
        'WS2_32.dll'
    )

    # Initialize the unexpected dependencies by applying where object.

    $unexpectedDependencies = @(

        # Perform the where object calls required by the assert allowed dependencies operation.

        $Dependencies |
            Where-Object { $_ -notin $allowedDependencies }
    )

    # Handle the branch where unexpected dependencies.count differs from 0.

    if ( $unexpectedDependencies.Count -ne 0 )
    {
        throw (
            "$ArtifactName imports unexpected native dependencies: " +
            ( $unexpectedDependencies -join ', ' )
        )
    }
}

function Add-ProcessModulePaths
{
    param
    (
        [Parameter(Mandatory = $true)]
        [System.Diagnostics.Process] $Process,

        [Parameter(Mandatory = $true)]
        [AllowEmptyCollection()]
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

function Assert-AllowedLoadedModules
{
    param
    (
        [Parameter(Mandatory = $true)]
        [AllowEmptyCollection()]
        [string[]] $ModulePaths,

        [Parameter(Mandatory = $true)]
        [string] $ArtifactName,

        [Parameter(Mandatory = $true)]
        [string] $ExecutablePath,

        [switch] $AllowExternalNonRuntimeModules
    )

    $visualCppRuntimeModulePattern =
        '^(?:CONCRT|MSVCP|MSVCR|VCCORLIB|VCOMP|VCRUNTIME)\d+(?:_[A-Za-z0-9]+)*\.dll$'
    $approvedRuntimeModuleNames = Get-ApprovedVisualCppRuntimeModuleNames
    $unapprovedRuntimeModulePaths = @(
        $ModulePaths |
            Where-Object {
                $moduleName = Split-Path -Leaf $_

                $moduleName -match $visualCppRuntimeModulePattern -and
                    $moduleName -notin $approvedRuntimeModuleNames
            }
    )

    if ( $unapprovedRuntimeModulePaths.Count -ne 0 )
    {
        throw (
            "$ArtifactName loaded an unapproved Microsoft Visual C++ runtime module: " +
            (
                (
                    $unapprovedRuntimeModulePaths |
                        ForEach-Object { Split-Path -Leaf $_ } |
                        Sort-Object -Unique
                ) -join ', '
            )
        )
    }

    if ( [string]::IsNullOrWhiteSpace( $env:SystemRoot ) )
    {
        throw 'SystemRoot is required to verify loaded native modules.'
    }

    $resolvedExecutablePath = [System.IO.Path]::GetFullPath( $ExecutablePath )
    $windowsDirectoryPrefix = [System.IO.Path]::GetFullPath( $env:SystemRoot ).TrimEnd( '\' ) + '\'
    $systemDirectory        = [System.IO.Path]::GetFullPath( ( Join-Path $env:SystemRoot 'System32' ) ).TrimEnd( '\' )
    $misplacedRuntimeModulePaths = @(
        $ModulePaths |
            Where-Object {
                $moduleName = Split-Path -Leaf $_

                if ( $moduleName -notin $approvedRuntimeModuleNames )
                {
                    return $false
                }

                $expectedModulePath = Join-Path $systemDirectory $moduleName
                $resolvedModulePath = [System.IO.Path]::GetFullPath( $_ )

                return $resolvedModulePath -ne $expectedModulePath
            }
    )

    if ( $misplacedRuntimeModulePaths.Count -ne 0 )
    {
        throw (
            "$ArtifactName loaded an approved runtime module from outside the central System32 installation: " +
            ( $misplacedRuntimeModulePaths -join ', ' )
        )
    }

    $externalModulePaths = @(
        $ModulePaths |
            Where-Object {
                $resolvedModulePath = [System.IO.Path]::GetFullPath( $_ )

                $resolvedModulePath -ne $resolvedExecutablePath -and
                    -not $resolvedModulePath.StartsWith(
                        $windowsDirectoryPrefix,
                        [System.StringComparison]::OrdinalIgnoreCase
                    )
            }
    )
    $windowsTextInputPrefix = [System.IO.Path]::GetFullPath(
        ( Join-Path $env:ProgramFiles 'Common Files\Microsoft Shared\Ink' )
    ).TrimEnd( '\' ) + '\'
    $unapprovedExternalModulePaths = @(
        foreach ( $externalModulePath in $externalModulePaths )
        {
            $resolvedExternalModulePath = [System.IO.Path]::GetFullPath( $externalModulePath )

            if (
                $resolvedExternalModulePath.StartsWith(
                    $windowsTextInputPrefix,
                    [System.StringComparison]::OrdinalIgnoreCase
                )
            )
            {
                $externalModuleSignature = Get-AuthenticodeSignature -LiteralPath $resolvedExternalModulePath

                if (
                    $externalModuleSignature.Status -eq 'Valid' -and
                    $null -ne $externalModuleSignature.SignerCertificate -and
                    $externalModuleSignature.SignerCertificate.Subject -match
                        '(?:^|, )O=Microsoft Corporation(?:,|$)'
                )
                {
                    continue
                }
            }

            $resolvedExternalModulePath
        }
    )

    if ( $unapprovedExternalModulePaths.Count -ne 0 -and -not $AllowExternalNonRuntimeModules )
    {
        throw (
            "$ArtifactName loaded a module from outside the Windows directory: " +
            ( $unapprovedExternalModulePaths -join ', ' )
        )
    }
}

function Assert-MachineTargetEvidence
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string] $ProjectDirectory
    )

    $serverBuildReportPath = Join-Path $ProjectDirectory 'eca-server\target\reports\native-build-output.json'

    if ( -not ( Test-Path -LiteralPath $serverBuildReportPath -PathType Leaf ) )
    {
        throw 'The server native build report required for CPU-target verification was not found.'
    }

    $serverBuildReport = Get-Content -LiteralPath $serverBuildReportPath -Raw -Encoding UTF8 | ConvertFrom-Json
    $serverMachineTarget = $serverBuildReport.general_info.graal_compiler.march

    if ( $serverMachineTarget -ne 'compatibility' )
    {
        throw "The native server machine target is '$serverMachineTarget', not 'compatibility'."
    }

    $clientDebugLog = Get-ChildItem `
        -LiteralPath ( Join-Path $ProjectDirectory 'eca-client\target\gluonfx\x86_64-windows\gvm\log' ) `
        -File `
        -Filter 'client-debug*.log' |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if ( $null -eq $clientDebugLog )
    {
        throw 'The client native build log required for CPU-target verification was not found.'
    }

    $clientBuildLog = Get-Content -LiteralPath $clientDebugLog.FullName -Raw -Encoding UTF8

    if (
        -not $clientBuildLog.Contains( '-H:-NativeArchitecture' ) -or
        -not $clientBuildLog.Contains( '-H:CPUFeatures=SSE2' )
    )
    {
        throw 'The native client build did not record its explicit baseline x64 CPU-feature settings.'
    }
}

function Assert-VersionInformation
{
    param
    (
        [Parameter(Mandatory = $true)]
        [System.Diagnostics.FileVersionInfo] $VersionInformation,

        [Parameter(Mandatory = $true)]
        [string] $ExpectedFilename,

        [Parameter(Mandatory = $true)]
        [string] $ExpectedDescription
    )

    # Handle the branch where version information.file version or product version differs from 2.1.0, company name
    # differs from rohin gosling, product name differs from eca rule engine 2, file description differs from expected
    # description, or original filename differs from expected filename.

    if (
        ( $VersionInformation.FileVersion -cne '2.1.0' ) -or
        ( $VersionInformation.ProductVersion -cne '2.1.0' ) -or
        ( $VersionInformation.CompanyName -ne 'Rohin Gosling' ) -or
        ( $VersionInformation.ProductName -ne 'ECA Rule Engine 2' ) -or
        ( $VersionInformation.FileDescription -ne $ExpectedDescription ) -or
        ( $VersionInformation.OriginalFilename -ne $ExpectedFilename )
    )
    {
        throw "The executable version resource is incomplete or inconsistent: $($VersionInformation.FileName)"
    }
}

function Invoke-IsolatedServerSmoke
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string] $ExecutablePath,

        [Parameter(Mandatory = $true)]
        [string] $TemporaryDirectory
    )

    # Prepare the launch directory, local application data directory, process temporary directory, output path, error
    # path, isolated path, process, and observed module paths values required by the invoke isolated server smoke
    # operation.

    $launchDirectory              = Join-Path $TemporaryDirectory 'server launch – 伺服器'
    $localApplicationDataDirectory = Join-Path $TemporaryDirectory 'server-local-application-data'
    $processTemporaryDirectory    = Join-Path $TemporaryDirectory 'server-process-temporary'
    $outputPath                   = Join-Path $TemporaryDirectory 'server.stdout.log'
    $errorPath                    = Join-Path $TemporaryDirectory 'server.stderr.log'
    $isolatedPath                 = Join-Path $launchDirectory 'eca-server.exe'
    $serverProcess                = $null
    $observedModulePaths          = [System.Collections.Generic.HashSet[string]]::new(
        [System.StringComparer]::OrdinalIgnoreCase
    )

    # Perform the new item, out null, and copy item calls required by the invoke isolated server smoke operation.

    New-Item -ItemType Directory -Path $launchDirectory | Out-Null
    New-Item -ItemType Directory -Path $localApplicationDataDirectory | Out-Null
    New-Item -ItemType Directory -Path $processTemporaryDirectory | Out-Null
    Copy-Item -LiteralPath $ExecutablePath -Destination $isolatedPath

    # Initialize the version output by applying command.

    $versionOutput = ( & $isolatedPath --version 2>&1 ) -join [Environment]::NewLine

    # Handle the branch where lastexitcode differs from 0 or not version output.contains eca server.

    if ( $LASTEXITCODE -ne 0 -or -not $versionOutput.Contains( 'eca-server' ) )
    {
        throw "The isolated native server version command failed.`n$versionOutput"
    }

    # Reserve and release the documented default port to prove it is available for the no-argument launch.

    $portProbe = [System.Net.Sockets.TcpListener]::new( [System.Net.IPAddress]::Loopback, 8080 )

    try
    {
        $portProbe.Start()
    }

    catch
    {
        throw 'The documented no-argument server port 8080 is unavailable for native verification.'
    }

    finally
    {
        $portProbe.Stop()
    }

    $baseURL = 'http://127.0.0.1:8080'

    # Initialize the stopwatch by applying start new.

    $stopwatch       = [System.Diagnostics.Stopwatch]::StartNew()

    # Run the protected operation and route failures through the matching handler.

    try
    {
        # Launch without arguments while redirecting mutable process locations away from the one-file launch directory.

        $previousLocalApplicationData = $env:LOCALAPPDATA
        $previousPath                 = $env:PATH
        $previousTemporaryDirectory   = $env:TEMP
        $previousTemporaryDirectory2  = $env:TMP

        try
        {
            $env:LOCALAPPDATA = $localApplicationDataDirectory
            $env:PATH         = "$env:SystemRoot\System32;$env:SystemRoot"
            $env:TEMP         = $processTemporaryDirectory
            $env:TMP          = $processTemporaryDirectory

            $serverProcess = Start-Process `
                -FilePath $isolatedPath `
                -WorkingDirectory $launchDirectory `
                -WindowStyle Hidden `
                -PassThru `
                -RedirectStandardOutput $outputPath `
                -RedirectStandardError $errorPath
        }

        finally
        {
            $env:LOCALAPPDATA = $previousLocalApplicationData
            $env:PATH         = $previousPath
            $env:TEMP         = $previousTemporaryDirectory
            $env:TMP          = $previousTemporaryDirectory2
        }

        [void] $serverProcess.Handle

        # Initialize the deadline by applying add seconds.

        $deadline = [DateTime]::UtcNow.AddSeconds( 20 )
        $liveness = $null

        # Continue processing while the loop condition remains true.

        while ( ( $null -eq $liveness ) -and ( [DateTime]::UtcNow -lt $deadline ) )
        {
            # Run the protected operation and route failures through the matching handler.

            try
            {
                # Initialize the liveness by applying invoke rest method.

                $liveness = Invoke-RestMethod `
                    -Uri "$baseURL/api/v1/health/live" `
                    -Method Get `
                    -TimeoutSec 2
            }

            # Handle failures raised by the protected operation.

            catch
            {
                # Handle the branch where server process.has exited.

                if ( $serverProcess.HasExited )
                {
                    break
                }

                Add-ProcessModulePaths -Process $serverProcess -ModulePaths $observedModulePaths

                # Perform the start sleep calls required by the invoke isolated server smoke operation.

                Start-Sleep -Milliseconds 25
            }
        }

        Add-ProcessModulePaths -Process $serverProcess -ModulePaths $observedModulePaths

        # Perform the stop calls required by the invoke isolated server smoke operation.

        $stopwatch.Stop()

        # Handle the branch where null equals liveness or liveness.status differs from up.

        if ( $null -eq $liveness -or $liveness.status -ne 'UP' )
        {
            # Prepare the server output and server error values required by the invoke isolated server smoke operation.

            $serverOutput = Get-Content -LiteralPath $outputPath -Raw -ErrorAction SilentlyContinue
            $serverError  = Get-Content -LiteralPath $errorPath -Raw -ErrorAction SilentlyContinue

            throw "The isolated native server did not become live.`n$serverOutput`n$serverError"
        }

        # Request shutdown with the same isolated local-application-data environment and ASCII command arguments.

        $previousLocalApplicationData = $env:LOCALAPPDATA

        try
        {
            $env:LOCALAPPDATA = $localApplicationDataDirectory

            & $isolatedPath `
                stop `
                --server-url $baseURL `
                --timeout 5 `
                2>&1 | Out-Null
        }

        finally
        {
            $env:LOCALAPPDATA = $previousLocalApplicationData
        }

        # Handle the branch where lastexitcode differs from 0.

        if ( $LASTEXITCODE -ne 0 )
        {
            throw "The isolated native server stop command failed with exit code $LASTEXITCODE."
        }

        # Handle the branch where not server process.wait for exit 10000.

        if ( -not $serverProcess.WaitForExit( 10000 ) )
        {
            throw 'The isolated native server did not stop within ten seconds.'
        }

        # Perform the refresh calls required by the invoke isolated server smoke operation.

        $serverProcess.Refresh()

        # Handle the branch where server process.exit code differs from 0.

        if ( $serverProcess.ExitCode -ne 0 )
        {
            throw "The isolated native server exited with code $($serverProcess.ExitCode)."
        }

        # Initialize the launch files by applying get child item.

        $launchFiles = @( Get-ChildItem -LiteralPath $launchDirectory -Force )

        # Handle the branch where launch files.count differs from 1 or launch files.get value 0 .name cne eca
        # server.exe.

        if ( $launchFiles.Count -ne 1 -or $launchFiles.GetValue( 0 ).Name -cne 'eca-server.exe' )
        {
            throw 'The native server created or required an adjacent launch-directory artifact.'
        }

        $processTemporaryFiles = @( Get-ChildItem -LiteralPath $processTemporaryDirectory -Force -Recurse )

        if ( $processTemporaryFiles.Count -ne 0 )
        {
            throw 'The native server extracted or retained an artifact in its isolated temporary directory.'
        }

        # Return the prepared result to the caller.

        return [pscustomobject] @{
            ElapsedMilliseconds = [Math]::Round( $stopwatch.Elapsed.TotalMilliseconds, 1 )
            ExecutablePath      = $isolatedPath
            ModulePaths         = @( $observedModulePaths | Sort-Object )
        }
    }

    # Complete the required cleanup regardless of how the protected operation finishes.

    finally
    {
        # Handle the branch where null differs from server process and not server process.has exited.

        if ( $null -ne $serverProcess -and -not $serverProcess.HasExited )
        {
            # Perform the stop process and wait for exit calls required by the invoke isolated server smoke operation.

            Stop-Process -Id $serverProcess.Id -Force
            $serverProcess.WaitForExit()
        }

        if ( $null -ne $serverProcess )
        {
            $serverProcess.Dispose()
        }
    }
}

function Close-AutomationWindow
{
    param
    (
        [Parameter(Mandatory = $true)]
        [System.Windows.Automation.AutomationElement] $Element
    )

    $treeWalker = [System.Windows.Automation.TreeWalker]::ControlViewWalker
    $candidate  = $Element

    # A name match alone does not imply a window, and none of the non-window matches support WindowPattern. The
    # application's dialogs are undecorated, so Windows carries their title on the header label inside the dialog
    # rather than on the window itself. Walk from the matched element up to the window that owns it and close that.

    while ( $null -ne $candidate )
    {
        $windowPattern = $null

        if (
            $candidate.TryGetCurrentPattern(
                [System.Windows.Automation.WindowPattern]::Pattern,
                [ref] $windowPattern
            )
        )
        {
            # Only the dialog or chooser above the application is ever closed. A menu item can carry a dialog's name,
            # and walking up from one reaches the main window, which must stay open for the rest of the smoke.

            if ( $candidate.Current.Name -like '*ECA Rule Engine*' )
            {
                return $false
            }

            $windowPattern.Close()

            return $true
        }

        $candidate = $treeWalker.GetParent( $candidate )

        if (
            $null -ne $candidate -and
            [System.Windows.Automation.Automation]::Compare(
                $candidate,
                [System.Windows.Automation.AutomationElement]::RootElement
            )
        )
        {
            return $false
        }
    }

    return $false
}

function Invoke-AutomationElement
{
    param
    (
        [Parameter(Mandatory = $true)]
        [System.Windows.Automation.AutomationElement] $Element
    )

    $invokePattern = $null

    if (
        -not $Element.TryGetCurrentPattern(
            [System.Windows.Automation.InvokePattern]::Pattern,
            [ref] $invokePattern
        )
    )
    {
        return $false
    }

    $invokePattern.Invoke()

    return $true
}

function Invoke-IsolatedClientSmoke
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string] $ExecutablePath,

        [Parameter(Mandatory = $true)]
        [string] $ServerExecutablePath,

        [Parameter(Mandatory = $true)]
        [string] $TemporaryDirectory
    )

    $launchDirectory           = Join-Path $TemporaryDirectory 'client launch – 用戶端'
    $serverLaunchDirectory     = Join-Path $TemporaryDirectory 'interop server launch – 服務器'
    $serverDataDirectory       = Join-Path $TemporaryDirectory 'interop-server-data'
    $processTemporaryDirectory = Join-Path $TemporaryDirectory 'client-process-temporary'
    $resultPath                = Join-Path (
        [System.IO.Path]::GetTempPath()
    ) ( 'eca-client-smoke-result-' + [guid]::NewGuid() + '.txt' )
    $preferenceResultPath      = Join-Path (
        [System.IO.Path]::GetTempPath()
    ) ( 'eca-client-preference-result-' + [guid]::NewGuid() + '.txt' )
    $isolatedPath              = Join-Path $launchDirectory 'eca-client.exe'
    $isolatedServerPath        = Join-Path $serverLaunchDirectory 'eca-server.exe'
    $serverOutputPath          = Join-Path $TemporaryDirectory 'interop-server.stdout.log'
    $serverErrorPath           = Join-Path $TemporaryDirectory 'interop-server.stderr.log'
    $clientProcess             = $null
    $preferenceProcess         = $null
    $serverProcess             = $null
    $observedModulePaths       = [System.Collections.Generic.HashSet[string]]::new(
        [System.StringComparer]::OrdinalIgnoreCase
    )
    $applicationModulePaths    = [System.Collections.Generic.HashSet[string]]::new(
        [System.StringComparer]::OrdinalIgnoreCase
    )
    $fileChooserObserved       = $false
    $javaFxDialogObserved      = $false
    $applicationWindowObserved = $false
    $accessibleElementCount    = 0
    $observedAutomationElements = [System.Collections.Generic.HashSet[string]]::new(
        [System.StringComparer]::OrdinalIgnoreCase
    )

    New-Item -ItemType Directory -Path $launchDirectory | Out-Null
    New-Item -ItemType Directory -Path $serverLaunchDirectory | Out-Null
    New-Item -ItemType Directory -Path $serverDataDirectory | Out-Null
    New-Item -ItemType Directory -Path $processTemporaryDirectory | Out-Null
    Copy-Item -LiteralPath $ExecutablePath -Destination $isolatedPath
    Copy-Item -LiteralPath $ServerExecutablePath -Destination $isolatedServerPath

    $portProbe = [System.Net.Sockets.TcpListener]::new( [System.Net.IPAddress]::Loopback, 0 )

    try
    {
        $portProbe.Start()
        $serverPort = ( [System.Net.IPEndPoint] $portProbe.LocalEndpoint ).Port
    }

    finally
    {
        $portProbe.Stop()
    }

    $serverBaseURL   = "http://127.0.0.1:$serverPort"
    $serverArguments = 'start --host 127.0.0.1 --port {0} --data-directory "{1}"' -f `
        $serverPort, `
        $serverDataDirectory
    $clientArguments = (
        '--native-smoke="{0}" --native-smoke-phase=exercise --native-smoke-file-chooser=true ' +
        '--native-smoke-ui-automation=true ' +
        '--native-smoke-server-url="{1}" --native-smoke-tls-url="https://www.microsoft.com/"'
    ) -f $resultPath, $serverBaseURL
    $previousTemporaryDirectory  = $env:TEMP
    $previousTemporaryDirectory2 = $env:TMP
    $previousPath                 = $env:PATH

    try
    {
        $env:TEMP = $processTemporaryDirectory
        $env:TMP  = $processTemporaryDirectory
        $env:PATH = "$env:SystemRoot\System32;$env:SystemRoot"

        $serverProcess = Start-Process `
            -FilePath $isolatedServerPath `
            -ArgumentList $serverArguments `
            -WorkingDirectory $serverLaunchDirectory `
            -WindowStyle Hidden `
            -PassThru `
            -RedirectStandardOutput $serverOutputPath `
            -RedirectStandardError $serverErrorPath
    }

    finally
    {
        $env:TEMP = $previousTemporaryDirectory
        $env:TMP  = $previousTemporaryDirectory2
        $env:PATH = $previousPath
    }

    try
    {
        $serverDeadline = [DateTime]::UtcNow.AddSeconds( 20 )
        $serverLiveness = $null

        while ( $null -eq $serverLiveness -and [DateTime]::UtcNow -lt $serverDeadline )
        {
            try
            {
                $serverLiveness = Invoke-RestMethod `
                    -Uri "$serverBaseURL/api/v1/health/live" `
                    -Method Get `
                    -TimeoutSec 2
            }

            catch
            {
                if ( $serverProcess.HasExited )
                {
                    break
                }

                Start-Sleep -Milliseconds 25
            }
        }

        if ( $null -eq $serverLiveness -or $serverLiveness.status -ne 'UP' )
        {
            $serverOutput = Get-Content -LiteralPath $serverOutputPath -Raw -ErrorAction SilentlyContinue
            $serverError  = Get-Content -LiteralPath $serverErrorPath -Raw -ErrorAction SilentlyContinue

            throw "The native interop server did not become live.`n$serverOutput`n$serverError"
        }

        $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

        try
        {
            $env:TEMP = $processTemporaryDirectory
            $env:TMP  = $processTemporaryDirectory
            $env:PATH = "$env:SystemRoot\System32;$env:SystemRoot"

            $clientProcess = Start-Process `
                -FilePath $isolatedPath `
                -ArgumentList $clientArguments `
                -WorkingDirectory $launchDirectory `
                -PassThru
        }

        finally
        {
            $env:TEMP = $previousTemporaryDirectory
            $env:TMP  = $previousTemporaryDirectory2
            $env:PATH = $previousPath
        }

        [void] $clientProcess.Handle

        Add-Type -AssemblyName UIAutomationClient
        Add-Type -AssemblyName UIAutomationTypes

        $deadline = [DateTime]::UtcNow.AddSeconds( 45 )

        while (
            -not ( Test-Path -LiteralPath $resultPath ) -and
            -not $clientProcess.HasExited -and
            [DateTime]::UtcNow -lt $deadline
        )
        {
            try
            {
                $processCondition = [System.Windows.Automation.PropertyCondition]::new(
                    [System.Windows.Automation.AutomationElement]::ProcessIdProperty,
                    $clientProcess.Id
                )
                $automationElements = [System.Windows.Automation.AutomationElement]::RootElement.FindAll(
                    [System.Windows.Automation.TreeScope]::Descendants,
                    $processCondition
                )

                $aboutDialogVisible = $false
                $dialogCloseButton  = $null

                foreach ( $automationElement in $automationElements )
                {
                    if ( -not [string]::IsNullOrWhiteSpace( $automationElement.Current.Name ) )
                    {
                        [void] $observedAutomationElements.Add(
                            "$($automationElement.Current.ControlType.ProgrammaticName): " +
                            $automationElement.Current.Name
                        )
                    }

                    if (
                        $automationElement.Current.ControlType -eq
                            [System.Windows.Automation.ControlType]::Window -and
                        (
                            $automationElement.Current.Name -like '*ECA Rule Engine*' -or
                            $automationElement.Current.Name -eq 'About'
                        )
                    )
                    {
                        $applicationWindowObserved = $true
                        $namedApplicationElements  = @(
                            $automationElement.FindAll(
                                [System.Windows.Automation.TreeScope]::Descendants,
                                [System.Windows.Automation.Condition]::TrueCondition
                            ) |
                                Where-Object { -not [string]::IsNullOrWhiteSpace( $_.Current.Name ) }
                        )
                        $accessibleElementCount = [Math]::Max(
                            $accessibleElementCount,
                            $namedApplicationElements.Count
                        )
                    }

                    if (
                        $automationElement.Current.Name -eq 'Open ECA Model' -and
                        ( Close-AutomationWindow -Element $automationElement )
                    )
                    {
                        $fileChooserObserved = $true
                    }

                    if ( $automationElement.Current.Name -eq 'About' )
                    {
                        $aboutDialogVisible = $true
                    }

                    if (
                        $automationElement.Current.ControlType -eq
                            [System.Windows.Automation.ControlType]::Button -and
                        $automationElement.Current.Name -eq 'Close dialog'
                    )
                    {
                        $dialogCloseButton = $automationElement
                    }
                }

                # The application's dialogs are undecorated, so JavaFX renders them inside the application window's
                # automation tree rather than as separate windows, and no window exists to close. They are dismissed
                # the way a person dismisses them: by invoking the close button on the dialog itself.

                if (
                    -not $javaFxDialogObserved -and
                    $aboutDialogVisible -and
                    $null -ne $dialogCloseButton -and
                    $accessibleElementCount -ge 5 -and
                    ( Invoke-AutomationElement -Element $dialogCloseButton )
                )
                {
                    $javaFxDialogObserved = $true
                }
            }

            catch [System.Windows.Automation.ElementNotAvailableException]
            {
                # The UI can change between discovery and inspection; the next poll observes its replacement.
            }

            catch [System.InvalidOperationException]
            {
                # An element can stop supporting a pattern between discovery and use; the next poll retries it.
            }

            Add-ProcessModulePaths -Process $clientProcess -ModulePaths $observedModulePaths

            if ( -not $fileChooserObserved -and -not $javaFxDialogObserved )
            {
                Add-ProcessModulePaths -Process $clientProcess -ModulePaths $applicationModulePaths
            }

            Start-Sleep -Milliseconds 50
        }

        Add-ProcessModulePaths -Process $clientProcess -ModulePaths $observedModulePaths
        $stopwatch.Stop()

        if ( -not ( Test-Path -LiteralPath $resultPath ) )
        {
            throw (
                'The native client did not complete its rendered smoke within forty-five seconds. Observed: ' +
                ( @( $observedAutomationElements | Sort-Object ) -join '; ' )
            )
        }

        $result = ( Get-Content -LiteralPath $resultPath -Raw ).Trim()

        if ( $result -notmatch '^PASS\|(?<OriginalTheme>light|dark)\|(?<PersistedTheme>light|dark)$' )
        {
            throw "The native client startup smoke failed: $result"
        }

        $originalTheme  = $Matches.get_Item( 'OriginalTheme' )
        $persistedTheme = $Matches.get_Item( 'PersistedTheme' )

        if ( -not $fileChooserObserved )
        {
            throw (
                'Windows UI Automation did not observe and cancel the native file chooser. Observed: ' +
                ( @( $observedAutomationElements | Sort-Object ) -join '; ' )
            )
        }

        if ( -not $javaFxDialogObserved )
        {
            throw (
                'Windows UI Automation did not observe and close the rendered JavaFX dialog. Observed: ' +
                ( @( $observedAutomationElements | Sort-Object ) -join '; ' )
            )
        }

        if ( -not $applicationWindowObserved -or $accessibleElementCount -lt 5 )
        {
            throw (
                'Windows UI Automation did not expose the rendered JavaFX application controls. Observed: ' +
                ( @( $observedAutomationElements | Sort-Object ) -join '; ' )
            )
        }

        if ( -not $clientProcess.WaitForExit( 10000 ) -or $clientProcess.ExitCode -ne 0 )
        {
            throw 'The native client rendered smoke did not exit successfully.'
        }

        $preferenceArguments = (
            '--native-smoke="{0}" --native-smoke-phase=read-preferences ' +
            '--native-smoke-theme={1} --native-smoke-restore-theme={2}'
        ) -f $preferenceResultPath, $persistedTheme, $originalTheme

        try
        {
            $env:TEMP = $processTemporaryDirectory
            $env:TMP  = $processTemporaryDirectory
            $env:PATH = "$env:SystemRoot\System32;$env:SystemRoot"

            $preferenceProcess = Start-Process `
                -FilePath $isolatedPath `
                -ArgumentList $preferenceArguments `
                -WorkingDirectory $launchDirectory `
                -PassThru
        }

        finally
        {
            $env:TEMP = $previousTemporaryDirectory
            $env:TMP  = $previousTemporaryDirectory2
            $env:PATH = $previousPath
        }

        [void] $preferenceProcess.Handle
        $preferenceDeadline = [DateTime]::UtcNow.AddSeconds( 30 )

        while (
            -not ( Test-Path -LiteralPath $preferenceResultPath ) -and
            -not $preferenceProcess.HasExited -and
            [DateTime]::UtcNow -lt $preferenceDeadline
        )
        {
            Add-ProcessModulePaths -Process $preferenceProcess -ModulePaths $observedModulePaths
            Start-Sleep -Milliseconds 25
        }

        Add-ProcessModulePaths -Process $preferenceProcess -ModulePaths $observedModulePaths

        if ( -not ( Test-Path -LiteralPath $preferenceResultPath ) )
        {
            throw 'The native client did not verify persisted preferences after relaunch.'
        }

        $preferenceResult = ( Get-Content -LiteralPath $preferenceResultPath -Raw ).Trim()

        if ( $preferenceResult -ne 'PASS' )
        {
            throw "The native client preference relaunch failed: $preferenceResult"
        }

        if ( -not $preferenceProcess.WaitForExit( 10000 ) -or $preferenceProcess.ExitCode -ne 0 )
        {
            throw 'The native client preference relaunch did not exit successfully.'
        }

        & $isolatedServerPath `
            stop `
            --server-url $serverBaseURL `
            --data-directory $serverDataDirectory `
            --timeout 5 `
            2>&1 | Out-Null

        if ( $LASTEXITCODE -ne 0 -or -not $serverProcess.WaitForExit( 10000 ) )
        {
            throw 'The native interop server did not stop successfully.'
        }

        $launchFiles       = @( Get-ChildItem -LiteralPath $launchDirectory -Force )
        $serverLaunchFiles = @( Get-ChildItem -LiteralPath $serverLaunchDirectory -Force )

        if ( $launchFiles.Count -ne 1 -or $launchFiles.GetValue( 0 ).Name -cne 'eca-client.exe' )
        {
            throw 'The native client created or required an adjacent launch-directory artifact.'
        }

        if ( $serverLaunchFiles.Count -ne 1 -or $serverLaunchFiles.GetValue( 0 ).Name -cne 'eca-server.exe' )
        {
            throw 'The native interop server created or required an adjacent launch-directory artifact.'
        }

        $processTemporaryFiles = @( Get-ChildItem -LiteralPath $processTemporaryDirectory -Force -Recurse )

        if ( $processTemporaryFiles.Count -ne 0 )
        {
            throw 'The native applications extracted or retained an artifact in the isolated temporary directory.'
        }

        return [pscustomobject] @{
            ElapsedMilliseconds = [Math]::Round( $stopwatch.Elapsed.TotalMilliseconds, 1 )
            ExecutablePath      = $isolatedPath
            ModulePaths         = @( $observedModulePaths | Sort-Object )
            ApplicationModules  = @( $applicationModulePaths | Sort-Object )
            AccessibleElements  = $accessibleElementCount
        }
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

        foreach ( $temporaryResultPath in @( $resultPath, $preferenceResultPath ) )
        {
            if ( Test-Path -LiteralPath $temporaryResultPath -PathType Leaf )
            {
                Remove-Item -LiteralPath $temporaryResultPath -Force
            }
        }
    }
}

# Initialize the project directory by applying resolve path and join path.

$projectDirectory = ( Resolve-Path -LiteralPath ( Join-Path $PSScriptRoot '..\..\..' ) ).Path

# Handle the branch where server executable path is blank.

if ( [string]::IsNullOrWhiteSpace( $ServerExecutablePath ) )
{
    # Initialize the server executable path by applying join path.

    $ServerExecutablePath = Join-Path $projectDirectory 'eca-server\target\eca-server.exe'
}

# Handle the branch where client executable path is blank.

if ( [string]::IsNullOrWhiteSpace( $ClientExecutablePath ) )
{
    # Initialize the client executable path by applying join path.

    $ClientExecutablePath = Join-Path (
        $projectDirectory
    ) 'eca-client\target\gluonfx\x86_64-windows\eca-client.exe'
}

# Handle the branch where report directory is blank.

if ( [string]::IsNullOrWhiteSpace( $ReportDirectory ) )
{
    # Initialize the report directory by applying join path.

    $ReportDirectory = Join-Path $projectDirectory 'build\verification\native'
}

# Prepare the resolved server path, resolved client path, resolved report path, dumpbin path, server dependency path,
# client dependency path, checksum path, report path, temporary root base, and temporary directory values required by
# the script operation.

$resolvedServerPath    = ( Resolve-Path -LiteralPath $ServerExecutablePath ).Path
$resolvedClientPath    = ( Resolve-Path -LiteralPath $ClientExecutablePath ).Path
$resolvedReportPath    = [System.IO.Path]::GetFullPath( $ReportDirectory )
$dumpbinPath           = Resolve-Dumpbin
$serverDependencyPath  = Join-Path $resolvedReportPath 'server-dependencies.txt'
$clientDependencyPath  = Join-Path $resolvedReportPath 'client-dependencies.txt'
$serverModulePath      = Join-Path $resolvedReportPath 'server-loaded-modules.txt'
$clientModulePath      = Join-Path $resolvedReportPath 'client-loaded-modules.txt'
$runtimeEvidencePath   = Join-Path $resolvedReportPath 'visual-cpp-runtime.txt'
$checksumPath          = Join-Path $resolvedReportPath 'checksums.sha256'
$reportPath            = Join-Path $resolvedReportPath 'native-verification.md'
$temporaryRootBase      = [System.IO.Path]::GetFullPath( [System.IO.Path]::GetTempPath() )
$temporaryDirectoryName = if ( $UseAsciiTemporaryPath )
{
    'eca-native-verification-' + [guid]::NewGuid()
}
else
{
    'ECA native verification – 驗證-' + [guid]::NewGuid()
}
$temporaryDirectory     = Join-Path $temporaryRootBase $temporaryDirectoryName
$pendingEvidenceDirectory    = Join-Path $temporaryDirectory 'pending-evidence'
$pendingServerDependencyPath = Join-Path $pendingEvidenceDirectory 'server-dependencies.txt'
$pendingClientDependencyPath = Join-Path $pendingEvidenceDirectory 'client-dependencies.txt'
$pendingServerModulePath     = Join-Path $pendingEvidenceDirectory 'server-loaded-modules.txt'
$pendingClientModulePath     = Join-Path $pendingEvidenceDirectory 'client-loaded-modules.txt'
$pendingRuntimeEvidencePath  = Join-Path $pendingEvidenceDirectory 'visual-cpp-runtime.txt'
$pendingChecksumPath         = Join-Path $pendingEvidenceDirectory 'checksums.sha256'
$pendingReportPath           = Join-Path $pendingEvidenceDirectory 'native-verification.md'

# Handle the branch where split path leaf resolved server path cne eca server.exe.

if ( ( Split-Path -Leaf $resolvedServerPath ) -cne 'eca-server.exe' )
{
    throw "Expected eca-server.exe but found '$resolvedServerPath'."
}

# Handle the branch where split path leaf resolved client path cne eca client.exe.

if ( ( Split-Path -Leaf $resolvedClientPath ) -cne 'eca-client.exe' )
{
    throw "Expected eca-client.exe but found '$resolvedClientPath'."
}

# Handle the branch where not test path literal path resolved report path.

if ( -not ( Test-Path -LiteralPath $resolvedReportPath ) )
{
    # Perform the new item and out null calls required by the script operation.

    New-Item -ItemType Directory -Path $resolvedReportPath | Out-Null
}

# Perform the new item and out null calls required by the script operation.

New-Item -ItemType Directory -Path $temporaryDirectory | Out-Null
New-Item -ItemType Directory -Path $pendingEvidenceDirectory | Out-Null

# Run the protected operation and route failures through the matching handler.

try
{
    # Prepare the server dependencies and client dependencies values required by the script operation.

    $serverDependencies = Get-NativeDependencies `
        -DumpbinPath $dumpbinPath `
        -ExecutablePath $resolvedServerPath `
        -OutputPath $pendingServerDependencyPath
    $clientDependencies = Get-NativeDependencies `
        -DumpbinPath $dumpbinPath `
        -ExecutablePath $resolvedClientPath `
        -OutputPath $pendingClientDependencyPath

    # Perform the assert x64 executable and assert allowed dependencies calls required by the script operation.

    Assert-X64Executable -DumpbinPath $dumpbinPath -ExecutablePath $resolvedServerPath
    Assert-X64Executable -DumpbinPath $dumpbinPath -ExecutablePath $resolvedClientPath
    Assert-MachineTargetEvidence -ProjectDirectory $projectDirectory
    $runtimeEvidence = Get-VisualCppRuntimeEvidence -DumpbinPath $dumpbinPath
    Assert-AllowedDependencies -Dependencies $serverDependencies -ArtifactName 'eca-server.exe'
    Assert-AllowedDependencies -Dependencies $clientDependencies -ArtifactName 'eca-client.exe'

    @(
        "Registry version: $($runtimeEvidence.RegistryVersion)",
        "MSVC toolset version: $($runtimeEvidence.ToolsetVersion)",
        '',
        'Module | Version | Signature | Path',
        '---|---|---|---',
        (
            $runtimeEvidence.Modules |
                ForEach-Object {
                    "$($_.Name) | $($_.Version) | $($_.Signature) - Microsoft | $($_.Path)"
                }
        )
    ) | Set-Content -LiteralPath $pendingRuntimeEvidencePath -Encoding UTF8

    # Prepare the server item and client item values required by the script operation.

    $serverItem               = Get-Item -LiteralPath $resolvedServerPath
    $clientItem               = Get-Item -LiteralPath $resolvedClientPath
    $serverVersionInformation = $serverItem.VersionInfo
    $clientVersionInformation = $clientItem.VersionInfo

    # Perform the assert version information calls required by the script operation.

    Assert-VersionInformation `
        -VersionInformation $serverVersionInformation `
        -ExpectedFilename 'eca-server.exe' `
        -ExpectedDescription 'ECA Rule Engine 2 Native Server'
    Assert-VersionInformation `
        -VersionInformation $clientVersionInformation `
        -ExpectedFilename 'eca-client.exe' `
        -ExpectedDescription 'ECA Rule Engine 2 Desktop Client'

    # Prepare the server signature and client signature values required by the script operation.

    $serverSignature = Get-AuthenticodeSignature -LiteralPath $resolvedServerPath
    $clientSignature = Get-AuthenticodeSignature -LiteralPath $resolvedClientPath

    # Handle the branch where server signature.status differs from not signed or client signature.status differs from
    # not signed.

    if ( $serverSignature.Status -ne 'NotSigned' -or $clientSignature.Status -ne 'NotSigned' )
    {
        throw 'The academic artifacts are expected to remain unsigned.'
    }

    # Prepare the server hash and client hash values required by the script operation.

    $serverHash = ( Get-FileHash -LiteralPath $resolvedServerPath -Algorithm SHA256 ).Hash
    $clientHash = ( Get-FileHash -LiteralPath $resolvedClientPath -Algorithm SHA256 ).Hash

    # Perform the set content calls required by the script operation.

    @(
        "$serverHash *eca-server.exe",
        "$clientHash *eca-client.exe"
    ) | Set-Content -LiteralPath $pendingChecksumPath -Encoding ASCII

    # Initialize the server cold start milliseconds by applying invoke isolated server smoke.

    $serverSmokeResult = Invoke-IsolatedServerSmoke `
        -ExecutablePath $resolvedServerPath `
        -TemporaryDirectory $temporaryDirectory

    Assert-AllowedLoadedModules `
        -ModulePaths $serverSmokeResult.ModulePaths `
        -ArtifactName 'eca-server.exe' `
        -ExecutablePath $serverSmokeResult.ExecutablePath

    @( $serverSmokeResult.ModulePaths | ForEach-Object { Split-Path -Leaf $_ } | Sort-Object -Unique ) |
        Set-Content -LiteralPath $pendingServerModulePath -Encoding UTF8

    $clientSmokeResult = $null

    # Handle the branch where not skip client launch.

    if ( -not $SkipClientLaunch )
    {
        # Initialize the client first window milliseconds by applying invoke isolated client smoke.

        $clientSmokeResult = Invoke-IsolatedClientSmoke `
            -ExecutablePath $resolvedClientPath `
            -ServerExecutablePath $resolvedServerPath `
            -TemporaryDirectory $temporaryDirectory

        Assert-AllowedLoadedModules `
            -ModulePaths $clientSmokeResult.ApplicationModules `
            -ArtifactName 'eca-client.exe' `
            -ExecutablePath $clientSmokeResult.ExecutablePath

        Assert-AllowedLoadedModules `
            -ModulePaths $clientSmokeResult.ModulePaths `
            -ArtifactName 'eca-client.exe after the Windows file chooser' `
            -ExecutablePath $clientSmokeResult.ExecutablePath `
            -AllowExternalNonRuntimeModules

        @( $clientSmokeResult.ModulePaths | Sort-Object -Unique ) |
            Set-Content -LiteralPath $pendingClientModulePath -Encoding UTF8
    }

    else
    {
        'Not run in this environment.' | Set-Content -LiteralPath $pendingClientModulePath -Encoding UTF8
    }

    # Handle the branch where skip client launch.

    $clientLaunchResult = if ( $SkipClientLaunch )
    {
        'Not run in this environment'
    }

    # Handle the alternative path when the preceding conditions are false.

    else
    {
        "Pass - $($clientSmokeResult.ElapsedMilliseconds) ms to rendered smoke marker"
    }

    # Initialize the report lines by applying get date and for each object.

    $reportLines = @(

        # Perform the get date and for each object calls required by the script operation.

        '# Native Verification',
        '',
        '| Field | Value |',
        '|---|---|',

        # Perform the get date calls required by the script operation.

        "| Date | $( Get-Date -Format 'yyyy-MM-dd HH:mm:ss K' ) |",
        "| Operating system | $([Environment]::OSVersion.VersionString) |",
        "| Processor | $env:PROCESSOR_IDENTIFIER |",
        "| Server SHA-256 | ``$serverHash`` |",
        "| Client SHA-256 | ``$clientHash`` |",
        "| Server size | $($serverItem.Length) bytes |",
        "| Client size | $($clientItem.Length) bytes |",
        "| Server version metadata | $($serverVersionInformation.FileVersion) |",
        "| Client version metadata | $($clientVersionInformation.FileVersion) |",
        "| Microsoft x64 runtime | $($runtimeEvidence.RegistryVersion) |",
        "| Release MSVC toolset | $($runtimeEvidence.ToolsetVersion) |",
        '| Runtime deployment | Centrally installed in `C:\\Windows\\System32` |',
        '| Runtime signatures | Valid Microsoft signatures |',
        "| Server cold start | $($serverSmokeResult.ElapsedMilliseconds) ms |",
        "| Client first window | $clientLaunchResult |",
        '| Signature scope | Unsigned academic artifacts, as designed |',
        '',
        '## Gate Results',
        '',
        '| Gate | Result |',
        '|---|---|',
        '| Exact executable names | Pass |',
        '| Windows x64 PE images | Pass |',
        '| Checked version metadata and icons | Pass |',
        '| Approved Windows and Microsoft Visual C++ v14 x64 imports only | Pass |',
        '| Microsoft x64 runtime version is current enough for the release toolset | Pass |',
        '| Microsoft x64 runtime location and signatures | Pass |',
        '| Broad Windows x64 CPU targets | Pass |',
        '| Server no-argument one-file execution from Unicode path | Pass |',

        # Handle the branch where skip client launch.

        "| Client one-file isolated execution | $( if ( $SkipClientLaunch ) { 'Not run' } else { 'Pass' } ) |",
        "| Native dialogs and Windows file chooser | $( if ( $SkipClientLaunch ) { 'Not run' } else { 'Pass' } ) |",
        "| Windows UI Automation accessibility exposure | $( if ( $SkipClientLaunch ) { 'Not run' } else { "Pass - $($clientSmokeResult.AccessibleElements) named controls" } ) |",
        "| Native client-to-server HTTP interoperability | $( if ( $SkipClientLaunch ) { 'Not run' } else { 'Pass' } ) |",
        "| Native client TLS round trip | $( if ( $SkipClientLaunch ) { 'Not run' } else { 'Pass' } ) |",
        "| Client preferences across relaunch | $( if ( $SkipClientLaunch ) { 'Not run' } else { 'Pass' } ) |",
        '| Adjacent or temporary product JARs, runtime directories, or DLLs | None |',
        '| Unapproved or application-local loaded Microsoft runtime modules | None |',
        '| Non-Windows application modules before the shell-owned file chooser | None |',
        '| Authenticode | Not signed; documented academic limitation |',
        '',
        '## Server Imports',
        '',
        ( $serverDependencies | ForEach-Object { "- ``$_``" } ),
        '',
        '## Client Imports',
        '',
        ( $clientDependencies | ForEach-Object { "- ``$_``" } ),
        '',
        'Raw `dumpbin /DEPENDENTS` output and observed loaded-module names are retained beside this report.'
    )

    # Perform the set content and write output calls required by the script operation.

    $reportLines | Set-Content -LiteralPath $pendingReportPath -Encoding UTF8

    $evidenceFiles = @(
        [pscustomobject] @{ Source = $pendingServerDependencyPath; Destination = $serverDependencyPath },
        [pscustomobject] @{ Source = $pendingClientDependencyPath; Destination = $clientDependencyPath },
        [pscustomobject] @{ Source = $pendingServerModulePath; Destination = $serverModulePath },
        [pscustomobject] @{ Source = $pendingClientModulePath; Destination = $clientModulePath },
        [pscustomobject] @{ Source = $pendingRuntimeEvidencePath; Destination = $runtimeEvidencePath },
        [pscustomobject] @{ Source = $pendingChecksumPath; Destination = $checksumPath },
        [pscustomobject] @{ Source = $pendingReportPath; Destination = $reportPath }
    )

    foreach ( $evidenceFile in $evidenceFiles )
    {
        Copy-Item `
            -LiteralPath $evidenceFile.Source `
            -Destination $evidenceFile.Destination `
            -Force
    }

    Write-Output "PASS: Native artifact verification completed. Report: $reportPath"
}

# Complete the required cleanup regardless of how the protected operation finishes.

finally
{
    Remove-VerifiedTemporaryDirectory `
        -TemporaryDirectory $temporaryDirectory `
        -TemporaryRoot $temporaryRootBase
}
