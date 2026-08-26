#Requires -Version 5.1

#----------------------------------------------------------------------------------------------------------------------
# Project: ECA Rule Engine 2
# Version: 2.0
# Date:    2026
# Author:  Rohin Gosling
#
# Description:
#
#   Audits every available server, client, launcher, and JavaFX native input for Microsoft C/C++ runtime selection
#   directives. The result records why the approved central Microsoft x64 runtime deployment is required.
#
# TODO:
#
#   None.
#
#----------------------------------------------------------------------------------------------------------------------

[CmdletBinding()]
param
(
    [string] $ServerGraalVMHome,
    [string] $ClientGraalVMHome,
    [string] $JavaFXStaticSdkHome,
    [string] $ReportPath
)

$ErrorActionPreference = 'Stop'

function Resolve-Dumpbin
{
    $dumpbinCommand = Get-Command 'dumpbin.exe' -ErrorAction SilentlyContinue

    if ( $null -ne $dumpbinCommand )
    {
        return $dumpbinCommand.Source
    }

    $visualStudioRoot = Join-Path $env:ProgramFiles 'Microsoft Visual Studio'
    $candidates       = @(
        Get-ChildItem `
            -LiteralPath $visualStudioRoot `
            -Recurse `
            -Filter 'dumpbin.exe' `
            -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -match '\\Hostx64\\x64\\|\\Hostx86\\x64\\' } |
            Sort-Object FullName -Descending
    )

    if ( $candidates.Count -eq 0 )
    {
        throw 'dumpbin.exe was not found. Install the Visual Studio x64 C++ build tools.'
    }

    return $candidates.GetValue( 0 ).FullName
}

function Add-AuditInputs
{
    param
    (
        [Parameter(Mandatory = $true)]
        [AllowEmptyCollection()]
        [System.Collections.Generic.List[object]] $Inputs,

        [Parameter(Mandatory = $true)]
        [string] $Scope,

        [Parameter(Mandatory = $true)]
        [string] $RootPath,

        [Parameter(Mandatory = $true)]
        [string[]] $Extensions
    )

    if ( -not ( Test-Path -LiteralPath $RootPath -PathType Container ) )
    {
        throw "The required native input directory was not found for ${Scope}: $RootPath"
    }

    foreach ( $extension in $Extensions )
    {
        foreach ( $inputFile in Get-ChildItem -LiteralPath $RootPath -Recurse -File -Filter "*.$extension" )
        {
            $relativePath = $inputFile.FullName.Substring( $RootPath.Length ).TrimStart( '\', '/' )

            $Inputs.Add(
                [pscustomobject] @{
                    Scope       = $Scope
                    DisplayPath = "$Scope/$($relativePath.Replace( '\', '/' ))"
                    FullPath    = $inputFile.FullName
                }
            )
        }
    }
}

function Add-AuditInput
{
    param
    (
        [Parameter(Mandatory = $true)]
        [AllowEmptyCollection()]
        [System.Collections.Generic.List[object]] $Inputs,

        [Parameter(Mandatory = $true)]
        [string] $Scope,

        [Parameter(Mandatory = $true)]
        [string] $InputPath
    )

    $resolvedInputPath = ( Resolve-Path -LiteralPath $InputPath ).Path

    $Inputs.Add(
        [pscustomobject] @{
            Scope       = $Scope
            DisplayPath = "$Scope/$( Split-Path -Leaf $resolvedInputPath )"
            FullPath    = $resolvedInputPath
        }
    )
}

function Add-ReportedLibraries
{
    param
    (
        [Parameter(Mandatory = $true)]
        [AllowEmptyCollection()]
        [System.Collections.Generic.List[object]] $Inputs,

        [Parameter(Mandatory = $true)]
        [string] $Scope,

        [Parameter(Mandatory = $true)]
        [string] $ProjectDirectory,

        [Parameter(Mandatory = $true)]
        [string] $ReportDirectory
    )

    $nativeLibraryReport = Get-ChildItem `
        -LiteralPath $ReportDirectory `
        -File `
        -Filter 'native_library_info_*.txt' |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if ( $null -eq $nativeLibraryReport )
    {
        throw "The native-library report required for ${Scope} was not found."
    }

    foreach ( $line in Get-Content -LiteralPath $nativeLibraryReport.FullName -Encoding UTF8 )
    {
        $candidatePath = $line.Trim()

        if ( -not $candidatePath.EndsWith( '.lib', [System.StringComparison]::OrdinalIgnoreCase ) )
        {
            continue
        }

        if ( Test-Path -LiteralPath $candidatePath -PathType Leaf )
        {
            Add-AuditInput `
                -Inputs $Inputs `
                -Scope $Scope `
                -InputPath $candidatePath
            continue
        }

        $toolchainIndex = $candidatePath.IndexOf( '.toolchains\', [System.StringComparison]::OrdinalIgnoreCase )

        if ( $toolchainIndex -lt 0 )
        {
            throw "The reported native library path is outside the pinned project toolchains: $candidatePath"
        }

        $projectRelativePath = $candidatePath.Substring( $toolchainIndex )

        Add-AuditInput `
            -Inputs $Inputs `
            -Scope $Scope `
            -InputPath ( Join-Path $ProjectDirectory $projectRelativePath )
    }
}

function Get-RuntimeDirectives
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string] $DumpbinPath,

        [Parameter(Mandatory = $true)]
        [string] $InputPath
    )

    $output = @( & $DumpbinPath /DIRECTIVES $InputPath 2>&1 )

    if ( $LASTEXITCODE -ne 0 )
    {
        throw "dumpbin /DIRECTIVES failed for '$InputPath' with exit code $LASTEXITCODE."
    }

    $directives = @(
        foreach ( $line in $output )
        {
            foreach ( $match in [regex]::Matches( $line, 'DEFAULTLIB:\"?([A-Za-z0-9._-]+)' ) )
            {
                $match.Groups.get_Item( 1 ).Value.ToUpperInvariant()
            }
        }
    )

    return @( $directives | Sort-Object -Unique )
}

$projectDirectory = ( Resolve-Path -LiteralPath ( Join-Path $PSScriptRoot '..\..\..' ) ).Path

if ( [string]::IsNullOrWhiteSpace( $ServerGraalVMHome ) )
{
    $ServerGraalVMHome = if ( [string]::IsNullOrWhiteSpace( $env:SERVER_GRAALVM_HOME ) )
    {
        Join-Path $projectDirectory '.toolchains\graalvm\graalvm-jdk-25.0.4+7.1'
    }
    else
    {
        $env:SERVER_GRAALVM_HOME
    }
}

if ( [string]::IsNullOrWhiteSpace( $ClientGraalVMHome ) )
{
    $ClientGraalVMHome = if ( [string]::IsNullOrWhiteSpace( $env:CLIENT_GRAALVM_HOME ) )
    {
        Join-Path $projectDirectory '.toolchains\gluon\graalvm-svm-java17-windows-gluon-22.1.0.1-Final'
    }
    else
    {
        $env:CLIENT_GRAALVM_HOME
    }
}

if ( [string]::IsNullOrWhiteSpace( $JavaFXStaticSdkHome ) )
{
    if ( [string]::IsNullOrWhiteSpace( $env:USERPROFILE ) )
    {
        throw 'USERPROFILE is required to resolve the Gluon JavaFX static SDK.'
    }

    $JavaFXStaticSdkHome = Join-Path (
        $env:USERPROFILE
    ) '.gluon\substrate\javafxStaticSdk\21-ea+11.3\windows-x86_64\sdk'
}

if ( [string]::IsNullOrWhiteSpace( $ReportPath ) )
{
    $ReportPath = Join-Path $projectDirectory 'build\verification\native\static-runtime-audit.md'
}

$resolvedServerGraalVMHome  = ( Resolve-Path -LiteralPath $ServerGraalVMHome ).Path
$resolvedClientGraalVMHome  = ( Resolve-Path -LiteralPath $ClientGraalVMHome ).Path
$resolvedJavaFXStaticSdkHome = ( Resolve-Path -LiteralPath $JavaFXStaticSdkHome ).Path
$resolvedReportPath          = [System.IO.Path]::GetFullPath( $ReportPath )
$dumpbinPath                 = Resolve-Dumpbin
$inputs                      = [System.Collections.Generic.List[object]]::new()
$serverReportDirectory       = Join-Path $projectDirectory 'eca-server\target\reports'
$clientReportDirectory       = Join-Path (
    $projectDirectory
) 'eca-client\target\gluonfx\x86_64-windows\gvm\eca-client\reports'

Add-ReportedLibraries `
    -Inputs $inputs `
    -Scope 'Linked Oracle GraalVM input' `
    -ProjectDirectory $projectDirectory `
    -ReportDirectory $serverReportDirectory
Add-ReportedLibraries `
    -Inputs $inputs `
    -Scope 'Linked Gluon GraalVM input' `
    -ProjectDirectory $projectDirectory `
    -ReportDirectory $clientReportDirectory

$clientPkcsLibraryPath = Join-Path (
    $resolvedClientGraalVMHome
) 'lib\static\windows-amd64\j2pkcs11.lib'

Add-AuditInput `
    -Inputs $inputs `
    -Scope 'Linked Gluon GraalVM input' `
    -InputPath $clientPkcsLibraryPath
Add-AuditInputs `
    -Inputs $inputs `
    -Scope 'Gluon JavaFX static SDK' `
    -RootPath ( Join-Path $resolvedJavaFXStaticSdkHome 'lib' ) `
    -Extensions @( 'lib' )

$generatedClientDirectory = Join-Path $projectDirectory 'eca-client\target\gluonfx\x86_64-windows\gvm'

if ( Test-Path -LiteralPath $generatedClientDirectory -PathType Container )
{
    Add-AuditInputs `
        -Inputs $inputs `
        -Scope 'Generated Gluon client inputs' `
        -RootPath $generatedClientDirectory `
        -Extensions @( 'obj' )
}

$dynamicRuntimeLibraries = @( 'MSVCRT', 'MSVCPRT', 'VCRUNTIME', 'VCRUNTIMED' )
$uniqueInputs             = @( $inputs | Sort-Object FullPath -Unique )
$auditResults             = @(
    foreach ( $input in $uniqueInputs )
    {
        $directives        = Get-RuntimeDirectives -DumpbinPath $dumpbinPath -InputPath $input.FullPath
        $dynamicDirectives = @( $directives | Where-Object { $_ -in $dynamicRuntimeLibraries } )

        [pscustomobject] @{
            DisplayPath       = $input.DisplayPath
            Directives        = $directives
            DynamicDirectives = $dynamicDirectives
        }
    }
)
$failingResults = @( $auditResults | Where-Object { $_.DynamicDirectives.Count -ne 0 } )
$staticResults  = @( $auditResults | Where-Object { $_.Directives -contains 'LIBCMT' -or $_.Directives -contains 'LIBCPMT' } )
$resultText     = if ( $failingResults.Count -eq 0 ) { 'Static runtime selected' } else { 'Dynamic runtime selected' }
$reportDirectory = Split-Path -Parent $resolvedReportPath
$dumpbinVersion = @(
    & $dumpbinPath /? 2>&1 |
        Where-Object { $_ -match 'Version' } |
        Select-Object -First 1
).GetValue( 0 ).Trim()
$windowsSdkLibraryRoot = Join-Path (
    [Environment]::GetEnvironmentVariable( 'ProgramFiles(x86)' )
) 'Windows Kits\10\Lib'
$windowsSdkVersion = Get-ChildItem `
    -LiteralPath $windowsSdkLibraryRoot `
    -Directory `
    -ErrorAction SilentlyContinue |
    Sort-Object { [version] $_.Name } -Descending |
    Select-Object -First 1 -ExpandProperty Name
$serverToolchainVersion = @( & ( Join-Path $resolvedServerGraalVMHome 'bin\java.exe' ) --version 2>&1 )
$clientToolchainVersion = @( & ( Join-Path $resolvedClientGraalVMHome 'bin\java.exe' ) --version 2>&1 )
$staticSdkArchivePath = Join-Path (
    $env:USERPROFILE
) '.gluon\substrate\openjfx-21-ea+11.3-windows-x86_64-static.zip'
$staticSdkArchiveHash = ( Get-FileHash -LiteralPath $staticSdkArchivePath -Algorithm SHA256 ).Hash

if ( -not ( Test-Path -LiteralPath $reportDirectory ) )
{
    New-Item -ItemType Directory -Path $reportDirectory | Out-Null
}

$reportLines = @(
    '# MSVC Runtime Selection Audit',
    '',
    '| Field | Value |',
    '|---|---|',
    "| Date | $( Get-Date -Format 'yyyy-MM-dd HH:mm:ss K' ) |",
    "| Result | $resultText |",
    "| Native inputs inspected | $($auditResults.Count) |",
    "| Inputs selecting a dynamic MSVC runtime | $($failingResults.Count) |",
    "| Inputs selecting a static MSVC runtime | $($staticResults.Count) |",
    "| MSVC tools | $dumpbinVersion |",
    "| Windows SDK | $windowsSdkVersion |",
    "| Server toolchain | $($serverToolchainVersion.GetValue( 0 )) |",
    "| Client toolchain | $($clientToolchainVersion.GetValue( 0 )) |",
    '| JavaFX static SDK | `21-ea+11.3` |',
    "| JavaFX static SDK SHA-256 | ``$staticSdkArchiveHash`` |",
    '',
    '## Dynamic-Runtime Inputs',
    '',
    '| Input | Directives |',
    '|---|---|',
    $(
        if ( $failingResults.Count -eq 0 )
        {
            '| None | None |'
        }
        else
        {
            $failingResults | ForEach-Object {
                "| ``$($_.DisplayPath)`` | ``$($_.DynamicDirectives -join ', ')`` |"
            }
        }
    ),
    '',
    '## Interpretation',
    '',
    'A `/DEFAULTLIB:MSVCRT` or `/DEFAULTLIB:MSVCPRT` directive proves that the supplied object or archive was built',
    'for the dynamic Microsoft C/C++ runtime. A final-link `/MT` switch cannot recompile that input. This finding is',
    'the retained technical basis for requiring the official centrally installed Microsoft x64 Redistributable.'
)

$reportLines | Set-Content -LiteralPath $resolvedReportPath -Encoding UTF8

Write-Output (
    "COMPLETE: $($failingResults.Count) of $($auditResults.Count) inspected input(s) select the dynamic MSVC " +
    "runtime. Report: $resolvedReportPath"
)
