#Requires -Version 5.1

#----------------------------------------------------------------------------------------------------------------------
# Project: ECA Rule Engine 2
# Version: 2.0
# Date:    2026
# Author:  Rohin Gosling
#
# Description:
#
#   Stages the two verified native executables in an otherwise empty release directory and emits their hashes for a
#   GitHub Actions job when requested.
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
    [string] $ServerExecutablePath,

    [Parameter(Mandatory = $true)]
    [string] $ClientExecutablePath,

    [Parameter(Mandatory = $true)]
    [string] $DestinationDirectory,

    [string] $MsvcToolsetVersion,

    [string] $GitHubOutputPath
)

$ErrorActionPreference = 'Stop'

function Assert-ExactArtifactSet
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string] $DirectoryPath
    )

    $expectedNames = @( 'eca-client.exe', 'eca-server.exe' )
    $artifactItems = @( Get-ChildItem -LiteralPath $DirectoryPath -Force )
    $actualNames   = @( $artifactItems | ForEach-Object { $_.Name } | Sort-Object )

    if (
        $artifactItems.Count -ne 2 -or
        ( $artifactItems | Where-Object { -not $_.PSIsContainer } ).Count -ne 2 -or
        ( $actualNames -join "`n" ) -cne ( $expectedNames -join "`n" )
    )
    {
        throw (
            "The release directory must contain exactly eca-client.exe and eca-server.exe. Found: " +
            ( $actualNames -join ', ' )
        )
    }
}

$resolvedServerPath = ( Resolve-Path -LiteralPath $ServerExecutablePath ).Path
$resolvedClientPath = ( Resolve-Path -LiteralPath $ClientExecutablePath ).Path

if ( ( Split-Path -Leaf $resolvedServerPath ) -cne 'eca-server.exe' )
{
    throw "The server release input must be named exactly eca-server.exe: $resolvedServerPath"
}

if ( ( Split-Path -Leaf $resolvedClientPath ) -cne 'eca-client.exe' )
{
    throw "The client release input must be named exactly eca-client.exe: $resolvedClientPath"
}

$resolvedDestinationDirectory = [System.IO.Path]::GetFullPath( $DestinationDirectory )

if ( Test-Path -LiteralPath $resolvedDestinationDirectory )
{
    $existingItems = @( Get-ChildItem -LiteralPath $resolvedDestinationDirectory -Force )

    if ( $existingItems.Count -ne 0 )
    {
        throw "The release staging directory is not empty: $resolvedDestinationDirectory"
    }
}
else
{
    New-Item -ItemType Directory -Path $resolvedDestinationDirectory | Out-Null
}

Copy-Item -LiteralPath $resolvedServerPath -Destination ( Join-Path $resolvedDestinationDirectory 'eca-server.exe' )
Copy-Item -LiteralPath $resolvedClientPath -Destination ( Join-Path $resolvedDestinationDirectory 'eca-client.exe' )

Assert-ExactArtifactSet -DirectoryPath $resolvedDestinationDirectory

$stagedServerPath = Join-Path $resolvedDestinationDirectory 'eca-server.exe'
$stagedClientPath = Join-Path $resolvedDestinationDirectory 'eca-client.exe'
$serverHash       = ( Get-FileHash -LiteralPath $stagedServerPath -Algorithm SHA256 ).Hash
$clientHash       = ( Get-FileHash -LiteralPath $stagedClientPath -Algorithm SHA256 ).Hash

if ( -not [string]::IsNullOrWhiteSpace( $GitHubOutputPath ) )
{
    $outputLines = @(
        "server_sha256=$serverHash",
        "client_sha256=$clientHash"
    )

    if ( -not [string]::IsNullOrWhiteSpace( $MsvcToolsetVersion ) )
    {
        $outputLines += "msvc_toolset_version=$MsvcToolsetVersion"
    }

    $outputText = ( $outputLines -join [Environment]::NewLine ) + [Environment]::NewLine
    $utf8WithoutBom = [System.Text.UTF8Encoding]::new( $false )
    [System.IO.File]::AppendAllText(
        [System.IO.Path]::GetFullPath( $GitHubOutputPath ),
        $outputText,
        $utf8WithoutBom
    )
}

Write-Output (
    "PASS: Staged exactly eca-server.exe ($serverHash) and eca-client.exe ($clientHash) in " +
    "$resolvedDestinationDirectory."
)
