#Requires -Version 5.1

#----------------------------------------------------------------------------------------------------------------------
# Project: ECA Rule Engine 2
# Version: 2.0
# Date:    2026
# Author:  Rohin Gosling
#
# Description:
#
#   Generates tag-bound GitHub Release notes with the required user journey, exact executable hashes, source commit,
#   toolchain identities, hosted verification boundary, and unsigned-artifact boundary.
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
    [string] $Tag,

    [Parameter(Mandatory = $true)]
    [ValidatePattern( '^[0-9a-fA-F]{40}$' )]
    [string] $SourceCommit,

    [Parameter(Mandatory = $true)]
    [string] $ArtifactDirectory,

    [Parameter(Mandatory = $true)]
    [string] $WorkflowRunUrl,

    [Parameter(Mandatory = $true)]
    [string] $MsvcToolsetVersion,

    [Parameter(Mandatory = $true)]
    [string] $OutputPath
)

$ErrorActionPreference = 'Stop'

$tagMatch = [regex]::Match(
    $Tag,
    '^v(?<Version>(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)\.(?:0|[1-9]\d*))$'
)

if ( -not $tagMatch.Success )
{
    throw "The release tag must use exact syntax v<major>.<minor>.<patch>: $Tag"
}

$resolvedArtifactDirectory = ( Resolve-Path -LiteralPath $ArtifactDirectory ).Path
$serverPath                = Join-Path $resolvedArtifactDirectory 'eca-server.exe'
$clientPath                = Join-Path $resolvedArtifactDirectory 'eca-client.exe'
$serverHash                = ( Get-FileHash -LiteralPath $serverPath -Algorithm SHA256 ).Hash
$clientHash                = ( Get-FileHash -LiteralPath $clientPath -Algorithm SHA256 ).Hash
$version                   = $tagMatch.Groups.get_Item( 'Version' ).Value

$releaseNotes = @(
    "# ECA Rule Engine $version",
    '',
    '## What''s New',
    '',
    '- Added Licences and Release Notes tabs to the About dialog in both clients.',
    '- Refined the web About dialog and Console navigation controls with a cleaner Fluent-style appearance.',
    '- Improved web cursor feedback so interactive controls use intuitive desktop-style pointers.',
    '- Updated Console navigation buttons in both clients to fit their content and use left-aligned text.',
    '- Made web Apply buttons commit changes and close their modal dialogs.',
    '- Expanded web revision fields and dialogs for long revision hashes, and made Dark the default web theme.',
    '',
    '## Windows 11 x64 Quick Start',
    '',
    (
        '1. Install or update Microsoft''s signed ' +
        '[Visual C++ v14 x64 Redistributable](https://aka.ms/vc14/vc_redist.x64.exe).'
    ),
    '2. Download both `eca-server.exe` and `eca-client.exe` from this release into one folder.',
    '3. Start `eca-server.exe`, leave its terminal open, and then start `eca-client.exe`.',
    '',
    'The Microsoft installer may request elevation or a restart. Both ECA executables require that centrally installed',
    'runtime. They are unsigned academic artifacts and may be blocked by Smart App Control or an organization-managed',
    'Application Control policy; do not weaken the machine''s security policy to run them.',
    '',
    '## Project-Supplied Assets',
    '',
    '- `eca-server.exe`',
    '- `eca-client.exe`',
    '',
    'GitHub automatically presents source code ZIP and tar archives. Those generated archives are not project-supplied',
    'binary assets.',
    '',
    '## Integrity and Provenance',
    '',
    '| Field | Value |',
    '|---|---|',
    "| Source commit | ``$SourceCommit`` |",
    "| Workflow run | [GitHub Actions]($WorkflowRunUrl) |",
    "| ``eca-server.exe`` SHA-256 | ``$serverHash`` |",
    "| ``eca-client.exe`` SHA-256 | ``$clientHash`` |",
    '| Signature status | Unsigned, as designed for this academic release |',
    '| JVM verification toolchain | Oracle JDK 25.0.2 |',
    (
        '| Maven Wrapper | Apache Maven 3.8.8; SHA-256 ' +
        '`2E181515CE8AE14B7A904C40BB4794831F5FD1D9641107A13B916AF15AF4001A` |'
    ),
    '| Server native-image toolchain | GraalVM 25.0.4 |',
    '| Client native-image toolchain | Gluon GraalVM 22.1.0.1-Final, Java 17.0.3 |',
    '| Gluon toolchain archive SHA-256 | `CD1ECD4AF199AE3D849F9E696125CF042B672E863D4EC7169CC32FDA847F1970` |',
    '| JavaFX static SDK | 21-ea+11.3 |',
    '| JavaFX static SDK SHA-256 | `E3BB86AA840695164C3D5ED634AAD7A7C89ECC311FD509E5ED4A427BDD1B1ED9` |',
    "| MSVC toolset | $MsvcToolsetVersion |",
    '| Hosted verification | GitHub-hosted Windows Server 2025 x64; full rendered client launch for this tag |',
    '| Microsoft x64 runtime installer | Official Microsoft URL; Authenticode signature verified by the workflow |',
    (
        '| Windows 11 compatibility | Supported by pre-release Windows 11 x64 verification; ' +
        'unsigned-policy caveat applies |'
    ),
    '',
    'The two release assets are byte-for-byte copies of the commit-addressed Actions artifact verified in the same tag',
    'workflow run. The tag job did not rebuild them.'
)

$resolvedOutputPath = [System.IO.Path]::GetFullPath( $OutputPath )
$outputDirectory = Split-Path -Parent $resolvedOutputPath

if ( -not ( Test-Path -LiteralPath $outputDirectory ) )
{
    New-Item -ItemType Directory -Path $outputDirectory | Out-Null
}

$releaseNotes | Set-Content -LiteralPath $resolvedOutputPath -Encoding UTF8

Write-Output "PASS: Generated release notes for $Tag at $resolvedOutputPath."
