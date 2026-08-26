#Requires -Version 5.1

#----------------------------------------------------------------------------------------------------------------------
# Project: ECA Rule Engine 2
# Version: 2.0
# Date:    2026
# Author:  Rohin Gosling
#
# Description:
#
#   Verifies the exact two-file release set, hashes, unsigned status, Maven and application version agreement, server
#   command version, and Windows PE string and numeric version resources.
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
    [string] $ArtifactDirectory,

    [string] $ProjectDirectory,

    [string] $Tag,

    [string] $ExpectedServerHash,

    [string] $ExpectedClientHash,

    [switch] $SkipServerVersionExecution
)

$ErrorActionPreference = 'Stop'

function Get-RequiredRegexValue
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string] $Path,

        [Parameter(Mandatory = $true)]
        [string] $Pattern,

        [Parameter(Mandatory = $true)]
        [string] $Description
    )

    $text  = Get-Content -LiteralPath $Path -Raw -Encoding UTF8
    $match = [regex]::Match( $text, $Pattern )

    if ( -not $match.Success )
    {
        throw "The $Description version could not be read from '$Path'."
    }

    return $match.Groups.get_Item( 'Version' ).Value
}

function Get-MavenProjectVersion
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string] $Path
    )

    [xml] $projectDocument = Get-Content -LiteralPath $Path -Raw -Encoding UTF8
    $versionNode = $projectDocument.SelectSingleNode(
        "/*[local-name()='project']/*[local-name()='version']"
    )

    if ( $null -eq $versionNode -or [string]::IsNullOrWhiteSpace( $versionNode.InnerText ) )
    {
        throw "The Maven project version could not be read from '$Path'."
    }

    return $versionNode.InnerText.Trim()
}

function Assert-MavenModuleVersions
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string] $DirectoryPath,

        [Parameter(Mandatory = $true)]
        [string] $ExpectedVersion
    )

    $rootPomPath = Join-Path $DirectoryPath 'pom.xml'
    [xml] $projectDocument = Get-Content -LiteralPath $rootPomPath -Raw -Encoding UTF8
    $moduleNodes = $projectDocument.SelectNodes(
        "/*[local-name()='project']/*[local-name()='modules']/*[local-name()='module']"
    )

    foreach ( $moduleNode in $moduleNodes )
    {
        $modulePomPath = Join-Path $DirectoryPath ( Join-Path $moduleNode.InnerText.Trim() 'pom.xml' )
        [xml] $moduleDocument = Get-Content -LiteralPath $modulePomPath -Raw -Encoding UTF8
        $parentVersionNode = $moduleDocument.SelectSingleNode(
            "/*[local-name()='project']/*[local-name()='parent']/*[local-name()='version']"
        )

        if ( $null -eq $parentVersionNode -or $parentVersionNode.InnerText.Trim() -cne $ExpectedVersion )
        {
            throw "The Maven parent version in '$modulePomPath' does not equal $ExpectedVersion."
        }
    }
}

function Get-NumericVersionParts
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string] $VersionText
    )

    $versionParts = @( $VersionText.Split( '.' ) | ForEach-Object { [int] $_ } )

    if ( $versionParts.Count -lt 2 -or $versionParts.Count -gt 3 )
    {
        throw "The application version must have two or three numeric components: $VersionText"
    }

    while ( $versionParts.Count -lt 3 )
    {
        $versionParts += 0
    }

    return @( $versionParts.GetValue( 0 ), $versionParts.GetValue( 1 ), $versionParts.GetValue( 2 ), 0 )
}

function Assert-PeVersion
{
    param
    (
        [Parameter(Mandatory = $true)]
        [System.Diagnostics.FileVersionInfo] $VersionInformation,

        [Parameter(Mandatory = $true)]
        [string] $ExpectedVersion
    )

    if (
        $VersionInformation.FileVersion -cne $ExpectedVersion -or
        $VersionInformation.ProductVersion -cne $ExpectedVersion
    )
    {
        throw (
            "The PE string versions for '$($VersionInformation.FileName)' do not equal $ExpectedVersion. " +
            "FileVersion=$($VersionInformation.FileVersion); ProductVersion=$($VersionInformation.ProductVersion)."
        )
    }

    $expectedParts = Get-NumericVersionParts -VersionText $ExpectedVersion
    $actualFileParts = @(
        $VersionInformation.FileMajorPart,
        $VersionInformation.FileMinorPart,
        $VersionInformation.FileBuildPart,
        $VersionInformation.FilePrivatePart
    )
    $actualProductParts = @(
        $VersionInformation.ProductMajorPart,
        $VersionInformation.ProductMinorPart,
        $VersionInformation.ProductBuildPart,
        $VersionInformation.ProductPrivatePart
    )

    if (
        ( $actualFileParts -join '.' ) -cne ( $expectedParts -join '.' ) -or
        ( $actualProductParts -join '.' ) -cne ( $expectedParts -join '.' )
    )
    {
        throw (
            "The PE numeric versions for '$($VersionInformation.FileName)' do not equal " +
            "$($expectedParts -join '.')."
        )
    }
}

if ( [string]::IsNullOrWhiteSpace( $ProjectDirectory ) )
{
    $ProjectDirectory = Join-Path $PSScriptRoot '..\..\..'
}

$resolvedProjectDirectory  = ( Resolve-Path -LiteralPath $ProjectDirectory ).Path
$resolvedArtifactDirectory = ( Resolve-Path -LiteralPath $ArtifactDirectory ).Path
$artifactItems             = @( Get-ChildItem -LiteralPath $resolvedArtifactDirectory -Force )
$actualNames               = @( $artifactItems | ForEach-Object { $_.Name } | Sort-Object )
$expectedNames             = @( 'eca-client.exe', 'eca-server.exe' )

if (
    $artifactItems.Count -ne 2 -or
    ( $artifactItems | Where-Object { -not $_.PSIsContainer } ).Count -ne 2 -or
    ( $actualNames -join "`n" ) -cne ( $expectedNames -join "`n" )
)
{
    throw (
        "The release artifact must contain exactly eca-client.exe and eca-server.exe. Found: " +
        ( $actualNames -join ', ' )
    )
}

$serverPath = Join-Path $resolvedArtifactDirectory 'eca-server.exe'
$clientPath = Join-Path $resolvedArtifactDirectory 'eca-client.exe'
$serverHash = ( Get-FileHash -LiteralPath $serverPath -Algorithm SHA256 ).Hash
$clientHash = ( Get-FileHash -LiteralPath $clientPath -Algorithm SHA256 ).Hash

if (
    -not [string]::IsNullOrWhiteSpace( $ExpectedServerHash ) -and
    $serverHash -cne $ExpectedServerHash.ToUpperInvariant()
)
{
    throw "The server artifact hash differs from the verified build hash: $serverHash"
}

if (
    -not [string]::IsNullOrWhiteSpace( $ExpectedClientHash ) -and
    $clientHash -cne $ExpectedClientHash.ToUpperInvariant()
)
{
    throw "The client artifact hash differs from the verified build hash: $clientHash"
}

$serverSignature = Get-AuthenticodeSignature -LiteralPath $serverPath
$clientSignature = Get-AuthenticodeSignature -LiteralPath $clientPath

if ( $serverSignature.Status -ne 'NotSigned' -or $clientSignature.Status -ne 'NotSigned' )
{
    throw 'The academic release artifacts must remain unsigned.'
}

$mavenVersion = Get-MavenProjectVersion -Path ( Join-Path $resolvedProjectDirectory 'pom.xml' )
$serverCommandSourceVersion = Get-RequiredRegexValue `
    -Path (
        Join-Path $resolvedProjectDirectory 'eca-server\src\main\java\com\rohingosling\eca\server\ServerMain.java'
    ) `
    -Pattern 'public\s+static\s+final\s+String\s+VERSION\s*=\s*"eca-server\s+(?<Version>[^"]+)"' `
    -Description 'server command source'
$serverBannerSourceVersion = Get-RequiredRegexValue `
    -Path (
        Join-Path $resolvedProjectDirectory `
            'eca-server\src\main\java\com\rohingosling\eca\server\ServerTerminalOutput.java'
    ) `
    -Pattern 'public\s+static\s+final\s+String\s+PRODUCT_VERSION\s*=\s*"(?<Version>[^"]+)"' `
    -Description 'server banner source'
$clientAboutVersion = Get-RequiredRegexValue `
    -Path (
        Join-Path $resolvedProjectDirectory `
            'eca-client\src\main\resources\com\rohingosling\eca\client\messages.properties'
    ) `
    -Pattern '(?m)^about\.version=Version\s+(?<Version>[^\r\n]+)\r?$' `
    -Description 'client About source'

$expectedVersion = $mavenVersion

if ( -not [string]::IsNullOrWhiteSpace( $Tag ) )
{
    $tagMatch = [regex]::Match(
        $Tag,
        '^v(?<Version>(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)\.(?:0|[1-9]\d*))$'
    )

    if ( -not $tagMatch.Success )
    {
        throw "The release tag must use exact syntax v<major>.<minor>.<patch>: $Tag"
    }

    $expectedVersion = $tagMatch.Groups.get_Item( 'Version' ).Value

    if ( $mavenVersion -cne $expectedVersion )
    {
        throw "The release tag version $expectedVersion does not equal the Maven version $mavenVersion."
    }
}

Assert-MavenModuleVersions `
    -DirectoryPath $resolvedProjectDirectory `
    -ExpectedVersion $expectedVersion

$versionSources = @(
    [pscustomobject] @{ Name = 'server command source'; Version = $serverCommandSourceVersion },
    [pscustomobject] @{ Name = 'server banner source'; Version = $serverBannerSourceVersion },
    [pscustomobject] @{ Name = 'client About source'; Version = $clientAboutVersion }
)

foreach ( $versionSource in $versionSources )
{
    if ( $versionSource.Version -cne $expectedVersion )
    {
        throw (
            "The $($versionSource.Name) version $($versionSource.Version) does not equal $expectedVersion."
        )
    }
}

if ( -not $SkipServerVersionExecution )
{
    $serverVersionOutput = @( & $serverPath --version 2>&1 )
    $serverVersionExitCode = $LASTEXITCODE
    $serverVersionText = ( $serverVersionOutput -join [Environment]::NewLine ).Trim()

    if ( $serverVersionExitCode -ne 0 -or $serverVersionText -cne "eca-server $expectedVersion" )
    {
        throw (
            "The native server command version does not equal eca-server $expectedVersion. " +
            "ExitCode=$serverVersionExitCode; Output='$serverVersionText'."
        )
    }
}

Assert-PeVersion -VersionInformation ( Get-Item -LiteralPath $serverPath ).VersionInfo -ExpectedVersion $expectedVersion
Assert-PeVersion -VersionInformation ( Get-Item -LiteralPath $clientPath ).VersionInfo -ExpectedVersion $expectedVersion

Write-Output (
    "PASS: Release artifact contract verified for version $expectedVersion. " +
    "Server SHA-256: $serverHash. Client SHA-256: $clientHash."
)
