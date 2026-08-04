#Requires -Version 5.1

#----------------------------------------------------------------------------------------------------------------------
# Project: ECA Rule Engine 2
# Version: 2.0
# Date:    2025
# Author:  Rohin Gosling
#
# Description:
#
#   Compiles the checked-in Windows executable identity resources used by the server and client native link steps.
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
    [ValidateSet( 'Server', 'Client', 'All' )]
    [string] $Target
)

$ErrorActionPreference = 'Stop'

function Resolve-ResourceCompiler
{
    # Initialize the resource compiler command by applying get command.

    $resourceCompilerCommand = Get-Command 'rc.exe' -ErrorAction SilentlyContinue

    # Handle the branch where resource compiler command is available.

    if ( $null -ne $resourceCompilerCommand )
    {
        # Return the prepared result to the caller.

        return $resourceCompilerCommand.Source
    }

    # Prepare the windows kits root and candidates values required by the resolve resource compiler operation.

    $windowsKitsRoot = Join-Path ${env:ProgramFiles(x86)} 'Windows Kits\10\bin'
    $candidates      = @(

        # Perform the get child item, where object, and sort object calls required by the resolve resource compiler
        # operation.

        Get-ChildItem `
            -LiteralPath $windowsKitsRoot `
            -Recurse `
            -Filter 'rc.exe' `
            -ErrorAction SilentlyContinue |
            Where-Object { $_.Directory.Name -eq 'x64' } |
            Sort-Object FullName -Descending
    )

    # Handle the branch where candidates.count equals 0.

    if ( $candidates.Count -eq 0 )
    {
        throw 'rc.exe was not found. Install the Windows SDK x64 resource compiler.'
    }

    # Return the prepared result to the caller.

    return $candidates.GetValue( 0 ).FullName
}

function Resolve-ResourceConverter
{
    # Initialize the resource converter command by applying get command.

    $resourceConverterCommand = Get-Command 'cvtres.exe' -ErrorAction SilentlyContinue

    # Handle the branch where resource converter command is available.

    if ( $null -ne $resourceConverterCommand )
    {
        # Return the prepared result to the caller.

        return $resourceConverterCommand.Source
    }

    # Prepare the visual studio root and candidates values required by the resolve resource converter operation.

    $visualStudioRoot = Join-Path $env:ProgramFiles 'Microsoft Visual Studio'
    $candidates       = @(

        # Perform the get child item, where object, and sort object calls required by the resolve resource converter
        # operation.

        Get-ChildItem `
            -LiteralPath $visualStudioRoot `
            -Recurse `
            -Filter 'cvtres.exe' `
            -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -like '*\Hostx64\x64\cvtres.exe' } |
            Sort-Object FullName -Descending
    )

    # Handle the branch where candidates.count equals 0.

    if ( $candidates.Count -eq 0 )
    {
        throw 'cvtres.exe was not found. Install Visual Studio Desktop development with C++.'
    }

    # Return the prepared result to the caller.

    return $candidates.GetValue( 0 ).FullName
}

function Build-Resource
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string] $ResourceCompilerPath,

        [Parameter(Mandatory = $true)]
        [string] $ResourceConverterPath,

        [Parameter(Mandatory = $true)]
        [string] $SourcePath,

        [Parameter(Mandatory = $true)]
        [string] $OutputPath
    )

    # Prepare the source directory, source name, and compiled resource path values required by the build resource
    # operation.

    $sourceDirectory      = Split-Path -Parent $SourcePath
    $sourceName           = Split-Path -Leaf $SourcePath
    $compiledResourcePath = [System.IO.Path]::ChangeExtension( $OutputPath, '.res' )

    # Perform the push location calls required by the build resource operation.

    Push-Location $sourceDirectory

    # Run the protected operation and route failures through the matching handler.

    try
    {
        # Perform the command calls required by the build resource operation.

        & $ResourceCompilerPath /nologo /fo $compiledResourcePath $sourceName

        # Handle the branch where lastexitcode differs from 0.

        if ( $LASTEXITCODE -ne 0 )
        {
            throw "The Windows resource compiler failed for '$SourcePath' with exit code $LASTEXITCODE."
        }

        # Perform the command calls required by the build resource operation.

        & $ResourceConverterPath /machine:x64 "/out:$OutputPath" $compiledResourcePath

        # Handle the branch where lastexitcode differs from 0.

        if ( $LASTEXITCODE -ne 0 )
        {
            throw "The Windows resource converter failed for '$SourcePath' with exit code $LASTEXITCODE."
        }
    }

    # Complete the required cleanup regardless of how the protected operation finishes.

    finally
    {
        # Perform the pop location calls required by the build resource operation.

        Pop-Location
    }

    # Handle the branch where not test path literal path output path.

    if ( -not ( Test-Path -LiteralPath $OutputPath ) )
    {
        throw "The Windows resource compiler did not produce '$OutputPath'."
    }
}

# Prepare the project directory, resource directory, output directory, application icon path, client icon path,
# resource compiler path, and resource converter path values required by the script operation.

$projectDirectory      = ( Resolve-Path -LiteralPath ( Join-Path $PSScriptRoot '..\..' ) ).Path
$resourceDirectory     = Join-Path $projectDirectory 'assets\windows'
$outputDirectory       = Join-Path $projectDirectory 'build\native-resources'
$applicationIconPath   = Join-Path $projectDirectory 'assets\images\eca-rule-engine.ico'
$clientIconPath        = Join-Path $projectDirectory 'eca-client\src\windows\assets\icon.ico'
$resourceCompilerPath  = Resolve-ResourceCompiler
$resourceConverterPath = Resolve-ResourceConverter

# Handle the branch where not test path literal path application icon path.

if ( -not ( Test-Path -LiteralPath $applicationIconPath ) )
{
    throw "The generated application icon is missing: $applicationIconPath"
}

# Handle the branch where not test path literal path client icon path.

if ( -not ( Test-Path -LiteralPath $clientIconPath ) )
{
    throw "The generated GluonFX client icon is missing: $clientIconPath"
}

# Handle the branch where not test path literal path output directory.

if ( -not ( Test-Path -LiteralPath $outputDirectory ) )
{
    # Perform the new item and out null calls required by the script operation.

    New-Item -ItemType Directory -Path $outputDirectory | Out-Null
}

# Handle the branch where target in server all.

if ( $Target -in @( 'Server', 'All' ) )
{
    # Perform the build resource and join path calls required by the script operation.

    Build-Resource `
        -ResourceCompilerPath $resourceCompilerPath `
        -ResourceConverterPath $resourceConverterPath `
        -SourcePath ( Join-Path $resourceDirectory 'eca-server.rc' ) `
        -OutputPath ( Join-Path $outputDirectory 'eca-server.obj' )
}

# Handle the branch where target in client all.

if ( $Target -in @( 'Client', 'All' ) )
{
    # Perform the build resource and join path calls required by the script operation.

    Build-Resource `
        -ResourceCompilerPath $resourceCompilerPath `
        -ResourceConverterPath $resourceConverterPath `
        -SourcePath ( Join-Path $resourceDirectory 'eca-client.rc' ) `
        -OutputPath ( Join-Path $outputDirectory 'eca-client.obj' )
}

# Perform the write output calls required by the script operation.

Write-Output "Compiled $Target Windows native resource metadata with '$resourceCompilerPath' and '$resourceConverterPath'."
