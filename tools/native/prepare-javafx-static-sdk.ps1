#Requires -Version 5.1

#----------------------------------------------------------------------------------------------------------------------
# Project: ECA Rule Engine 2
# Version: 2.0
# Date:    2026
# Author:  Rohin Gosling
#
# Description:
#
#   Downloads the exact Gluon JavaFX static SDK archive accepted by the native client build and verifies its SHA-256
#   before GluonFX may extract or consume it.
#
# TODO:
#
#   None.
#
#----------------------------------------------------------------------------------------------------------------------

[CmdletBinding()]
param
(
    [string] $CacheDirectory
)

$ErrorActionPreference = 'Stop'

$staticSdkVersion = '21-ea+11.3'
$archiveName      = "openjfx-$staticSdkVersion-windows-x86_64-static.zip"
$expectedHash     = 'E3BB86AA840695164C3D5ED634AAD7A7C89ECC311FD509E5ED4A427BDD1B1ED9'
$downloadUri      = "https://download2.gluonhq.com/substrate/javafxstaticsdk/$archiveName"

# Initialize the cache directory from the standard Gluon cache location when the caller did not select one.

if ( [string]::IsNullOrWhiteSpace( $CacheDirectory ) )
{
    if ( [string]::IsNullOrWhiteSpace( $env:USERPROFILE ) )
    {
        throw 'USERPROFILE is required to resolve the Gluon cache directory.'
    }

    $CacheDirectory = Join-Path $env:USERPROFILE '.gluon\substrate'
}

$resolvedCacheDirectory = [System.IO.Path]::GetFullPath( $CacheDirectory )
$archivePath             = Join-Path $resolvedCacheDirectory $archiveName

# Create the cache directory when a clean build agent does not have it yet.

if ( -not ( Test-Path -LiteralPath $resolvedCacheDirectory ) )
{
    New-Item -ItemType Directory -Path $resolvedCacheDirectory | Out-Null
}

# Download to a unique temporary file so an interrupted transfer can never become a trusted cache entry.

if ( -not ( Test-Path -LiteralPath $archivePath -PathType Leaf ) )
{
    $temporaryArchivePath = Join-Path (
        [System.IO.Path]::GetTempPath()
    ) ( "$archiveName.$([guid]::NewGuid()).partial" )

    try
    {
        Invoke-WebRequest -Uri $downloadUri -OutFile $temporaryArchivePath

        $downloadedHash = ( Get-FileHash -LiteralPath $temporaryArchivePath -Algorithm SHA256 ).Hash

        if ( $downloadedHash -ne $expectedHash )
        {
            throw "Gluon JavaFX static SDK SHA-256 mismatch: $downloadedHash"
        }

        Move-Item -LiteralPath $temporaryArchivePath -Destination $archivePath
    }

    finally
    {
        if ( Test-Path -LiteralPath $temporaryArchivePath )
        {
            Remove-Item -LiteralPath $temporaryArchivePath -Force
        }
    }
}

$actualHash = ( Get-FileHash -LiteralPath $archivePath -Algorithm SHA256 ).Hash

if ( $actualHash -ne $expectedHash )
{
    throw "Gluon JavaFX static SDK SHA-256 mismatch: $actualHash"
}

Write-Output "PASS: Pinned Gluon JavaFX static SDK $staticSdkVersion has SHA-256 $actualHash."
