#Requires -Version 5.1

#----------------------------------------------------------------------------------------------------------------------
# Project: ECA Rule Engine 2
# Version: 2.0
# Date:    2026
# Author:  Rohin Gosling
#
# Description:
#
#   Stages the exact Phase 10 candidate executables, launches a disposable Windows Sandbox, waits for its acceptance
#   result, and publishes the successful clean-machine evidence into the native verification directory.
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
    [string] $EvidenceDirectory,
    [switch] $PrepareOnly
)

$ErrorActionPreference = 'Stop'

function Resolve-Dumpbin
{
    $visualStudioRoot = Join-Path $env:ProgramFiles 'Microsoft Visual Studio'
    $dumpbinPath = Get-ChildItem `
        -LiteralPath $visualStudioRoot `
        -Recurse `
        -Filter 'dumpbin.exe' `
        -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -match '\\Hostx64\\x64\\|\\Hostx86\\x64\\' } |
        Sort-Object FullName -Descending |
        Select-Object -First 1 -ExpandProperty FullName

    if ( [string]::IsNullOrWhiteSpace( $dumpbinPath ) )
    {
        throw 'dumpbin.exe was not found. Install the Visual Studio x64 C++ build tools.'
    }

    return $dumpbinPath
}

$projectDirectory = ( Resolve-Path -LiteralPath ( Join-Path $PSScriptRoot '..\..\..' ) ).Path

if ( [string]::IsNullOrWhiteSpace( $ServerExecutablePath ) )
{
    $ServerExecutablePath = Join-Path $projectDirectory 'eca-server\target\eca-server.exe'
}

if ( [string]::IsNullOrWhiteSpace( $ClientExecutablePath ) )
{
    $ClientExecutablePath = Join-Path $projectDirectory 'eca-client\target\gluonfx\x86_64-windows\eca-client.exe'
}

if ( [string]::IsNullOrWhiteSpace( $EvidenceDirectory ) )
{
    $EvidenceDirectory = Join-Path $projectDirectory 'build\verification\native\clean-windows-11'
}

$resolvedServerPath   = ( Resolve-Path -LiteralPath $ServerExecutablePath ).Path
$resolvedClientPath   = ( Resolve-Path -LiteralPath $ClientExecutablePath ).Path
$resolvedEvidencePath = [System.IO.Path]::GetFullPath( $EvidenceDirectory )
$dumpbinPath          = Resolve-Dumpbin
$toolsetVersionMatch  = [regex]::Match(
    $dumpbinPath,
    '\\VC\\Tools\\MSVC\\(?<Version>\d+\.\d+\.\d+)\\',
    [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
)

if ( -not $toolsetVersionMatch.Success )
{
    throw "The selected dumpbin path does not identify its MSVC toolset version: $dumpbinPath"
}

$windowsSandboxPath = Join-Path $env:SystemRoot 'System32\WindowsSandbox.exe'

if ( -not ( Test-Path -LiteralPath $windowsSandboxPath -PathType Leaf ) )
{
    throw 'Windows Sandbox is not installed on this machine.'
}

$existingSandboxProcesses = @(
    Get-Process -ErrorAction SilentlyContinue |
        Where-Object {
            $_.ProcessName -in @(
                'vmmemWindowsSandbox',
                'WindowsSandbox',
                'WindowsSandboxClient',
                'WindowsSandboxRemoteSession',
                'WindowsSandboxServer'
            )
        }
)

if ( -not $PrepareOnly -and $existingSandboxProcesses.Count -ne 0 )
{
    throw (
        'Windows Sandbox is already running or has retained a host-compute process. Close it and wait for every ' +
        'Windows Sandbox process to exit. If vmmemWindowsSandbox remains, restart Windows before retrying this gate.'
    )
}

$stagingNamePrefix = if ( $PrepareOnly )
{
    'phase-10-windows-vm-'
}
else
{
    'phase-10-windows-sandbox-'
}
$stagingRoot = Join-Path $projectDirectory ( 'target\' + $stagingNamePrefix + [guid]::NewGuid() )
$candidateDirectory = Join-Path $stagingRoot 'candidates'
$pendingEvidenceDirectory = Join-Path $stagingRoot 'evidence'
$configurationPath = Join-Path $stagingRoot 'phase-10.wsb'

New-Item -ItemType Directory -Path $candidateDirectory -Force | Out-Null
New-Item -ItemType Directory -Path $pendingEvidenceDirectory -Force | Out-Null
Copy-Item -LiteralPath $resolvedServerPath -Destination ( Join-Path $candidateDirectory 'eca-server.exe' )
Copy-Item -LiteralPath $resolvedClientPath -Destination ( Join-Path $candidateDirectory 'eca-client.exe' )
Copy-Item `
    -LiteralPath ( Join-Path $PSScriptRoot 'invoke-clean-windows-acceptance.ps1' ) `
    -Destination ( Join-Path $candidateDirectory 'invoke-clean-windows-acceptance.ps1' )

$manifest = [ordered] @{
    serverSha256       = ( Get-FileHash -LiteralPath $resolvedServerPath -Algorithm SHA256 ).Hash
    clientSha256       = ( Get-FileHash -LiteralPath $resolvedClientPath -Algorithm SHA256 ).Hash
    msvcToolsetVersion = $toolsetVersionMatch.Groups.get_Item( 'Version' ).Value
}
$manifest |
    ConvertTo-Json |
    Set-Content -LiteralPath ( Join-Path $candidateDirectory 'candidate-manifest.json' ) -Encoding UTF8

$escapedCandidateDirectory = [System.Security.SecurityElement]::Escape( $candidateDirectory )
$escapedEvidenceDirectory  = [System.Security.SecurityElement]::Escape( $pendingEvidenceDirectory )
$configuration = @"
<Configuration>
  <MappedFolders>
    <MappedFolder>
      <HostFolder>$escapedCandidateDirectory</HostFolder>
      <SandboxFolder>C:\ECA-Candidates</SandboxFolder>
      <ReadOnly>true</ReadOnly>
    </MappedFolder>
    <MappedFolder>
      <HostFolder>$escapedEvidenceDirectory</HostFolder>
      <SandboxFolder>C:\ECA-Evidence</SandboxFolder>
      <ReadOnly>false</ReadOnly>
    </MappedFolder>
  </MappedFolders>
  <Networking>Enable</Networking>
  <ClipboardRedirection>Disable</ClipboardRedirection>
  <PrinterRedirection>Disable</PrinterRedirection>
  <VideoInput>Disable</VideoInput>
  <MemoryInMB>8192</MemoryInMB>
  <LogonCommand>
    <Command>powershell.exe -NoProfile -ExecutionPolicy Bypass -File C:\ECA-Candidates\invoke-clean-windows-acceptance.ps1 -CandidateDirectory C:\ECA-Candidates -EvidenceDirectory C:\ECA-Evidence -ShutdownWhenComplete</Command>
  </LogonCommand>
</Configuration>
"@
$configuration | Set-Content -LiteralPath $configurationPath -Encoding UTF8

if ( $PrepareOnly )
{
    Write-Output "Prepared clean-Windows VM acceptance bundle: $stagingRoot"
    Write-Output (
        'Copy this directory into the VM, then run: powershell.exe -NoProfile -ExecutionPolicy Bypass ' +
        '-File .\candidates\invoke-clean-windows-acceptance.ps1 -CandidateDirectory .\candidates ' +
        '-EvidenceDirectory .\evidence'
    )
    return
}

$sandboxProcess = Start-Process `
    -FilePath $windowsSandboxPath `
    -ArgumentList "`"$configurationPath`"" `
    -PassThru
$statusPath = Join-Path $pendingEvidenceDirectory 'status.txt'
$deadline   = [DateTime]::UtcNow.AddMinutes( 15 )
$sandboxObserved = $false
$startupDeadline = [DateTime]::UtcNow.AddSeconds( 45 )

while ( -not ( Test-Path -LiteralPath $statusPath -PathType Leaf ) -and [DateTime]::UtcNow -lt $deadline )
{
    $sandboxProcesses = @(
        Get-Process -ErrorAction SilentlyContinue |
            Where-Object {
                $_.ProcessName -in @(
                    'vmmemWindowsSandbox',
                    'WindowsSandboxClient',
                    'WindowsSandboxRemoteSession',
                    'WindowsSandboxServer'
                )
            }
    )

    if ( $sandboxProcesses.Count -ne 0 )
    {
        $sandboxObserved = $true
    }
    elseif ( $sandboxObserved )
    {
        throw 'Windows Sandbox closed before exporting an acceptance result.'
    }
    elseif ( $sandboxProcess.HasExited -and [DateTime]::UtcNow -ge $startupDeadline )
    {
        throw "Windows Sandbox did not start within forty-five seconds. Launcher exit code: $($sandboxProcess.ExitCode)"
    }

    Start-Sleep -Milliseconds 500
}

if ( -not ( Test-Path -LiteralPath $statusPath -PathType Leaf ) )
{
    throw "Windows Sandbox did not finish within fifteen minutes. Staging: $stagingRoot"
}

$status = Get-Content -LiteralPath $statusPath -Raw -Encoding UTF8

if ( -not $status.TrimStart().StartsWith( 'PASS', [System.StringComparison]::Ordinal ) )
{
    throw "The clean Windows 11 acceptance failed.`n$status`nStaging: $stagingRoot"
}

New-Item -ItemType Directory -Path $resolvedEvidencePath -Force | Out-Null

foreach ( $evidenceFile in Get-ChildItem -LiteralPath $pendingEvidenceDirectory -File )
{
    Copy-Item -LiteralPath $evidenceFile.FullName -Destination $resolvedEvidencePath -Force
}

Write-Output (
    'PASS: Clean Windows 11 Sandbox acceptance completed. Report: ' +
    ( Join-Path $resolvedEvidencePath 'clean-windows-11.md' )
)
