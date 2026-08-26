#Requires -Version 5.1

#----------------------------------------------------------------------------------------------------------------------
# Project: ECA Rule Engine 2
# Version: 2.0
# Date:    2026
# Author:  Rohin Gosling
#
# Description:
#
#   Creates or resumes a matching draft GitHub Release, uploads exactly the two verified executables, downloads and
#   re-verifies the remote bytes, and publishes the draft without mutating an existing published release.
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
    [string] $Repository,

    [Parameter(Mandatory = $true)]
    [ValidatePattern( '^v(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)$' )]
    [string] $Tag,

    [Parameter(Mandatory = $true)]
    [string] $ArtifactDirectory,

    [Parameter(Mandatory = $true)]
    [string] $ReleaseNotesPath,

    [string] $ExpectedServerHash,

    [string] $ExpectedClientHash
)

$ErrorActionPreference = 'Stop'

function Invoke-CheckedGitHubCommand
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string[]] $Arguments
    )

    $commandOutput = @( & gh @Arguments 2>&1 )
    $commandExitCode = $LASTEXITCODE

    if ( $commandExitCode -ne 0 )
    {
        throw "gh $($Arguments -join ' ') failed: $($commandOutput -join [Environment]::NewLine)"
    }

    return $commandOutput
}

function Get-GitHubRelease
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string] $RepositoryName,

        [Parameter(Mandatory = $true)]
        [string] $ReleaseTag
    )

    $viewArguments = @(
        'release',
        'view',
        $ReleaseTag,
        '--repo',
        $RepositoryName,
        '--json',
        'assets,isDraft,tagName,targetCommitish,url'
    )
    $viewOutput = @( & gh @viewArguments 2>&1 )
    $viewExitCode = $LASTEXITCODE

    if ( $viewExitCode -eq 0 )
    {
        return ( $viewOutput -join [Environment]::NewLine | ConvertFrom-Json )
    }

    $viewMessage = $viewOutput -join [Environment]::NewLine

    if ( $viewMessage -match '(?i)release not found|HTTP 404' )
    {
        return $null
    }

    throw "Unable to inspect GitHub Release ${ReleaseTag}: $viewMessage"
}

function Assert-ExactReleaseAssets
{
    param
    (
        [Parameter(Mandatory = $true)]
        [object] $Release,

        [switch] $AllowMissing
    )

    $expectedNames = @( 'eca-client.exe', 'eca-server.exe' )
    $actualNames   = @( $Release.assets | ForEach-Object { $_.name } | Sort-Object )
    $unexpectedNames = @( $actualNames | Where-Object { $_ -notin $expectedNames } )

    if ( $unexpectedNames.Count -ne 0 )
    {
        throw "The GitHub Release has unexpected custom assets: $($unexpectedNames -join ', ')"
    }

    if ( -not $AllowMissing -and ( $actualNames -join "`n" ) -cne ( $expectedNames -join "`n" ) )
    {
        throw "The GitHub Release does not contain exactly the two required executable assets."
    }

    return $actualNames
}

function Assert-RemoteReleaseHashes
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string] $RepositoryName,

        [Parameter(Mandatory = $true)]
        [string] $ReleaseTag,

        [Parameter(Mandatory = $true)]
        [string] $ServerHash,

        [Parameter(Mandatory = $true)]
        [string] $ClientHash,

        [string[]] $AssetNames = @( 'eca-client.exe', 'eca-server.exe' )
    )

    $downloadDirectory = Join-Path (
        [System.IO.Path]::GetTempPath()
    ) ( 'eca-release-verification-' + [guid]::NewGuid() )
    New-Item -ItemType Directory -Path $downloadDirectory | Out-Null

    $downloadArguments = @(
        'release',
        'download',
        $ReleaseTag,
        '--repo',
        $RepositoryName,
        '--dir',
        $downloadDirectory
    )

    foreach ( $assetName in $AssetNames )
    {
        $downloadArguments += @( '--pattern', $assetName )
    }

    [void] ( Invoke-CheckedGitHubCommand -Arguments $downloadArguments )

    $downloadedItems = @( Get-ChildItem -LiteralPath $downloadDirectory -Force )
    $downloadedNames = @( $downloadedItems | ForEach-Object { $_.Name } | Sort-Object )
    $expectedNames   = @( $AssetNames | Sort-Object )

    if (
        $downloadedItems.Count -ne $expectedNames.Count -or
        ( $downloadedItems | Where-Object { -not $_.PSIsContainer } ).Count -ne $expectedNames.Count -or
        ( $downloadedNames -join "`n" ) -cne ( $expectedNames -join "`n" )
    )
    {
        throw 'Downloading the GitHub Release did not produce exactly the requested executable assets.'
    }

    if ( 'eca-server.exe' -in $AssetNames )
    {
        $downloadedServerHash = (
            Get-FileHash -LiteralPath ( Join-Path $downloadDirectory 'eca-server.exe' ) -Algorithm SHA256
        ).Hash

        if ( $downloadedServerHash -cne $ServerHash )
        {
            throw (
                'The downloaded server release asset hash does not match the verified tag artifact: ' +
                $downloadedServerHash
            )
        }
    }

    if ( 'eca-client.exe' -in $AssetNames )
    {
        $downloadedClientHash = (
            Get-FileHash -LiteralPath ( Join-Path $downloadDirectory 'eca-client.exe' ) -Algorithm SHA256
        ).Hash

        if ( $downloadedClientHash -cne $ClientHash )
        {
            throw (
                'The downloaded client release asset hash does not match the verified tag artifact: ' +
                $downloadedClientHash
            )
        }
    }
}

if ( -not ( Get-Command 'gh.exe' -ErrorAction SilentlyContinue ) )
{
    throw 'GitHub CLI is required to publish the release.'
}

if ( [string]::IsNullOrWhiteSpace( $env:GH_TOKEN ) )
{
    throw 'GH_TOKEN is required to publish the release.'
}

$resolvedArtifactDirectory = ( Resolve-Path -LiteralPath $ArtifactDirectory ).Path
$resolvedReleaseNotesPath  = ( Resolve-Path -LiteralPath $ReleaseNotesPath ).Path
$artifactItems             = @( Get-ChildItem -LiteralPath $resolvedArtifactDirectory -Force )
$artifactNames             = @( $artifactItems | ForEach-Object { $_.Name } | Sort-Object )
$expectedArtifactNames     = @( 'eca-client.exe', 'eca-server.exe' )

if (
    $artifactItems.Count -ne 2 -or
    ( $artifactItems | Where-Object { -not $_.PSIsContainer } ).Count -ne 2 -or
    ( $artifactNames -join "`n" ) -cne ( $expectedArtifactNames -join "`n" )
)
{
    throw 'The local release directory does not contain exactly eca-client.exe and eca-server.exe.'
}

$serverPath                = Join-Path $resolvedArtifactDirectory 'eca-server.exe'
$clientPath                = Join-Path $resolvedArtifactDirectory 'eca-client.exe'
$serverHash                = ( Get-FileHash -LiteralPath $serverPath -Algorithm SHA256 ).Hash
$clientHash                = ( Get-FileHash -LiteralPath $clientPath -Algorithm SHA256 ).Hash

if (
    -not [string]::IsNullOrWhiteSpace( $ExpectedServerHash ) -and
    $serverHash -cne $ExpectedServerHash.ToUpperInvariant()
)
{
    throw "The local server hash differs from the verified tag artifact: $serverHash"
}

if (
    -not [string]::IsNullOrWhiteSpace( $ExpectedClientHash ) -and
    $clientHash -cne $ExpectedClientHash.ToUpperInvariant()
)
{
    throw "The local client hash differs from the verified tag artifact: $clientHash"
}

$release = Get-GitHubRelease -RepositoryName $Repository -ReleaseTag $Tag

if ( $null -eq $release )
{
    [void] ( Invoke-CheckedGitHubCommand -Arguments @(
        'release',
        'create',
        $Tag,
        '--repo',
        $Repository,
        '--verify-tag',
        '--title',
        $Tag,
        '--notes-file',
        $resolvedReleaseNotesPath,
        '--draft'
    ) )

    $release = Get-GitHubRelease -RepositoryName $Repository -ReleaseTag $Tag
}

if ( $release.tagName -cne $Tag )
{
    throw "The existing release tag '$($release.tagName)' does not equal '$Tag'."
}

$existingAssetNames = Assert-ExactReleaseAssets -Release $release -AllowMissing

if ( -not $release.isDraft )
{
    Assert-ExactReleaseAssets -Release $release
    Assert-RemoteReleaseHashes `
        -RepositoryName $Repository `
        -ReleaseTag $Tag `
        -ServerHash $serverHash `
        -ClientHash $clientHash
    Write-Output "PASS: Published release $Tag is immutable and already contains the matching verified bytes."
    exit 0
}

$missingAssetPaths = @()

if ( 'eca-server.exe' -notin $existingAssetNames )
{
    $missingAssetPaths += $serverPath
}

if ( 'eca-client.exe' -notin $existingAssetNames )
{
    $missingAssetPaths += $clientPath
}

if ( $existingAssetNames.Count -ne 0 )
{
    Assert-RemoteReleaseHashes `
        -RepositoryName $Repository `
        -ReleaseTag $Tag `
        -ServerHash $serverHash `
        -ClientHash $clientHash `
        -AssetNames $existingAssetNames
}

if ( $missingAssetPaths.Count -ne 0 )
{
    $uploadArguments = @( 'release', 'upload', $Tag, '--repo', $Repository ) + $missingAssetPaths
    [void] ( Invoke-CheckedGitHubCommand -Arguments $uploadArguments )
}

$release = Get-GitHubRelease -RepositoryName $Repository -ReleaseTag $Tag
Assert-ExactReleaseAssets -Release $release
Assert-RemoteReleaseHashes `
    -RepositoryName $Repository `
    -ReleaseTag $Tag `
    -ServerHash $serverHash `
    -ClientHash $clientHash

[void] ( Invoke-CheckedGitHubCommand -Arguments @(
    'release',
    'edit',
    $Tag,
    '--repo',
    $Repository,
    '--notes-file',
    $resolvedReleaseNotesPath,
    '--draft=false'
) )

$publishedRelease = Get-GitHubRelease -RepositoryName $Repository -ReleaseTag $Tag

if ( $publishedRelease.isDraft )
{
    throw "GitHub Release $Tag remained a draft after the publish operation."
}

Assert-ExactReleaseAssets -Release $publishedRelease

Write-Output "PASS: Published immutable GitHub Release $Tag with exactly the two verified executable assets."
