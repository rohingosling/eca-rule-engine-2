#Requires -Version 5.1

#----------------------------------------------------------------------------------------------------------------------
# Project: ECA Rule Engine 2
# Version: 2.0
# Date:    2025
# Author:  Rohin Gosling
#
# Description:
#
#   Starts the production native server directly with an initial model, verifies its terminal presentation and health
#   endpoints, and requests authenticated graceful shutdown.
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
    [string] $ExecutablePath
)

$ErrorActionPreference = 'Stop'

# Prepare the resolved executable path, project directory, model path, temporary root path, temporary directory, stop
# output path, stop error path, and listener values required by the script operation.

$resolvedExecutablePath = ( Resolve-Path -LiteralPath $ExecutablePath ).Path
$projectDirectory       = ( Resolve-Path -LiteralPath ( Join-Path $PSScriptRoot '..\..\..' ) ).Path
$modelPath              = ( Resolve-Path -LiteralPath (
    Join-Path $projectDirectory 'examples\eca-rule-engine-example.json'
) ).Path
$temporaryRootPath      = [System.IO.Path]::GetFullPath( [System.IO.Path]::GetTempPath() )
$temporaryDirectory     = Join-Path $temporaryRootPath ( 'eca-server-native-smoke-' + [guid]::NewGuid() )
$stopOutputPath         = Join-Path $temporaryDirectory 'stop.stdout.log'
$stopErrorPath          = Join-Path $temporaryDirectory 'stop.stderr.log'
$listener               = [System.Net.Sockets.TcpListener]::new( [System.Net.IPAddress]::Loopback, 0 )
$serverProcess          = $null

# Perform the new item, out null, and start calls required by the script operation.

New-Item -ItemType Directory -Path $temporaryDirectory | Out-Null

$listener.Start()
$port = ( [System.Net.IPEndPoint] $listener.LocalEndpoint ).Port

# Perform the stop calls required by the script operation.

$listener.Stop()

# Run the protected operation and route failures through the matching handler.

try
{
    $serverArguments = 'start --model "{0}" --host 127.0.0.1 --port {1} --data-directory "{2}"' -f `
        $modelPath, `
        $port, `
        $temporaryDirectory

    # Initialize the server process start information by applying new.

    $serverProcessStartInformation                        = [System.Diagnostics.ProcessStartInfo]::new()
    $serverProcessStartInformation.FileName               = $resolvedExecutablePath
    $serverProcessStartInformation.Arguments              = $serverArguments
    $serverProcessStartInformation.UseShellExecute        = $false
    $serverProcessStartInformation.CreateNoWindow         = $true
    $serverProcessStartInformation.RedirectStandardOutput = $true
    $serverProcessStartInformation.RedirectStandardError  = $true

    # Initialize the server process by applying new.

    $serverProcess                                        = [System.Diagnostics.Process]::new()
    $serverProcess.StartInfo                              = $serverProcessStartInformation

    # Handle the branch where not server process.start.

    if ( -not $serverProcess.Start() )
    {
        throw 'The native server process could not be started.'
    }

    $baseURL  = "http://127.0.0.1:$port"

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

            # Perform the start sleep calls required by the script operation.

            Start-Sleep -Milliseconds 200
        }
    }

    # Handle the branch where null equals liveness or liveness.status differs from up.

    if ( ( $null -eq $liveness ) -or ( $liveness.status -ne 'UP' ) )
    {
        # Handle the branch where not server process.has exited.

        if ( -not $serverProcess.HasExited )
        {
            # Perform the stop process and wait for exit calls required by the script operation.

            Stop-Process -Id $serverProcess.Id -Force
            $serverProcess.WaitForExit()
        }

        # Prepare the server output and server error values required by the script operation.

        $serverOutput = $serverProcess.StandardOutput.ReadToEnd()
        $serverError  = $serverProcess.StandardError.ReadToEnd()

        throw "The native server did not become healthy.`n$serverOutput`n$serverError"
    }

    # Prepare the readiness and active model values required by the script operation.

    $readiness = Invoke-RestMethod `
        -Uri "$baseURL/api/v1/health/ready" `
        -Method Get `
        -TimeoutSec 2
    $activeModel = Invoke-RestMethod `
        -Uri "$baseURL/api/v1/model" `
        -Method Get `
        -TimeoutSec 2

    # Handle the branch where not readiness.ready or string is null or white space readiness.model revision or active
    # model.model ID differs from eca mode test 1.

    if (
        ( -not $readiness.ready ) -or
        [string]::IsNullOrWhiteSpace( $readiness.modelRevision ) -or
        ( $activeModel.modelId -ne 'eca-rule-engine-example' )
    )
    {
        throw 'The native server did not activate the startup model before accepting requests.'
    }

    # Perform the command calls required by the script operation.

    & $resolvedExecutablePath `
        stop `
        --server-url $baseURL `
        --data-directory $temporaryDirectory `
        --timeout 5 `
        1> $stopOutputPath `
        2> $stopErrorPath
    $stopExitCode = $LASTEXITCODE

    # Handle the branch where stop exit code differs from 0.

    if ( $stopExitCode -ne 0 )
    {
        # Initialize the stop error by applying get content.

        $stopError = Get-Content -LiteralPath $stopErrorPath -Raw -ErrorAction SilentlyContinue

        throw "The native stop command failed with exit code $stopExitCode.`n$stopError"
    }

    # Handle the branch where not server process.wait for exit 10000.

    if ( -not $serverProcess.WaitForExit( 10000 ) )
    {
        throw 'The native server did not stop gracefully.'
    }

    # Handle the branch where server process.exit code differs from 0.

    if ( $serverProcess.ExitCode -ne 0 )
    {
        throw "The native server exited with code $($serverProcess.ExitCode)."
    }

    # Prepare the server output and server error values required by the script operation.

    $serverOutput   = $serverProcess.StandardOutput.ReadToEnd()
    $serverError    = $serverProcess.StandardError.ReadToEnd()
    $expectedBanner = [Environment]::NewLine `
        + 'ECA (Event Condition Action) Rule Engine Server' + [Environment]::NewLine `
        + 'Version 2.1.0' + [Environment]::NewLine `
        + 'Author: Rohin Gosling (2024)' + [Environment]::NewLine `
        + [Environment]::NewLine

    # Handle the branch where not server output.starts with expected banner system.string comparison ordinal.

    if ( -not $serverOutput.StartsWith( $expectedBanner, [System.StringComparison]::Ordinal ) )
    {
        throw "The native server output does not begin with the expected banner.`n$serverOutput`n$serverError"
    }

    $expectedOutputFragments = @(
        '  event          = startup',
        '  status         = ready',
        '  model-id       = eca-rule-engine-example',
        "  eca-server listening on http://127.0.0.1:$port"
    )

    # Process each value supplied by the selected collection.

    foreach ( $expectedOutputFragment in $expectedOutputFragments )
    {
        # Handle the branch where not server output.contains expected output fragment.

        if ( -not $serverOutput.Contains( $expectedOutputFragment ) )
        {
            throw "The native server output is missing '$expectedOutputFragment'.`n$serverOutput`n$serverError"
        }
    }

    # Perform the write output calls required by the script operation.

    Write-Output (
        'PASS: Native server presentation, startup-model activation, health, authenticated stop, graceful exit, ' +
        'and direct-executable checks succeeded.'
    )
}

# Complete the required cleanup regardless of how the protected operation finishes.

finally
{
    # Handle the branch where null differs from server process and not server process.has exited.

    if ( ( $null -ne $serverProcess ) -and ( -not $serverProcess.HasExited ) )
    {
        # Perform the stop process and wait for exit calls required by the script operation.

        Stop-Process -Id $serverProcess.Id -Force
        $serverProcess.WaitForExit()
    }

    # Handle the branch where server process is available.

    if ( $null -ne $serverProcess )
    {
        # Perform the dispose calls required by the script operation.

        $serverProcess.Dispose()
    }

    # Initialize the resolved temporary directory by applying get full path.

    $resolvedTemporaryDirectory = [System.IO.Path]::GetFullPath( $temporaryDirectory )

    # Handle the branch where test path literal path resolved temporary directory and resolved temporary
    # directory.starts with temporary root path system.string comparison ordinal ignore case.

    if (
        ( Test-Path -LiteralPath $resolvedTemporaryDirectory ) -and
        $resolvedTemporaryDirectory.StartsWith( $temporaryRootPath, [System.StringComparison]::OrdinalIgnoreCase )
    )
    {
        # Perform the remove item calls required by the script operation.

        Remove-Item -LiteralPath $resolvedTemporaryDirectory -Recurse -Force
    }
}
