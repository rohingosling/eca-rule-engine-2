#Requires -Version 5.1

#----------------------------------------------------------------------------------------------------------------------
# Project: ECA Rule Engine 2
# Version: 2.0
# Date:    2025
# Author:  Rohin Gosling
#
# Description:
#
#   Measures native server cold start, working set, 10,000-rule evaluation latency and throughput, 100-request
#   concurrency, and model-replacement availability. Writes a reproducible production native benchmark report.
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
    [string] $ReportPath,
    [int] $MeasuredRequestCount = 300
)

$ErrorActionPreference = 'Stop'

# Perform the add type calls required by the script operation.

Add-Type -AssemblyName System.Net.Http

function Get-AvailablePort
{
    # Initialize the listener by applying new.

    $listener = [System.Net.Sockets.TcpListener]::new(
        [System.Net.IPAddress]::Loopback,
        0
    )

    # Run the protected operation and route failures through the matching handler.

    try
    {
        # Perform the start calls required by the get available port operation.

        $listener.Start()

        # Return the available TCP port to the caller.

        return ( [System.Net.IPEndPoint] $listener.LocalEndpoint ).Port
    }

    # Complete the required cleanup regardless of how the protected operation finishes.

    finally
    {
        # Perform the stop calls required by the get available port operation.

        $listener.Stop()
    }
}

function New-BenchmarkModel
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string] $ModelIdentifier,

        [Parameter(Mandatory = $true)]
        [string] $ActionIdentifier
    )

    # Prepare the events and rules values required by the new benchmark model operation.

    $events = [System.Collections.Generic.List[object]]::new()
    $rules  = [System.Collections.Generic.List[object]]::new()

    # Repeat the indexed loop while its condition remains true.

    for ( $i = 0; $i -lt 10; $i++ )
    {
        $eventIdentifier = 'event-{0:D2}' -f $i

        # Perform the add calls required by the new benchmark model operation.

        $events.Add(
            [ordered] @{
                id          = $eventIdentifier
                name        = "Benchmark Event $i"
                description = "Benchmark event $i."
                payloadId   = 'payload-benchmark'
            }
        )
    }

    # Repeat the indexed loop while its condition remains true.

    for ( $i = 0; $i -lt 10000; $i++ )
    {
        $eventIdentifier = 'event-{0:D2}' -f ( $i % 10 )

        # Perform the add calls required by the new benchmark model operation.

        $rules.Add(
            [ordered] @{
                id             = 'rule-{0:D5}' -f $i
                name           = "Benchmark Rule $i"
                description    = "Benchmark rule $i."
                eventId        = $eventIdentifier
                conditionSetId = 'condition-set-benchmark'
                actionId       = $ActionIdentifier
            }
        )
    }

    # Initialize the model by applying to array.

    $model = [ordered] @{
        schemaVersion = '1.0'
        modelId       = $ModelIdentifier
        name          = 'Production Native Benchmark'
        description   = 'Ten events and ten thousand deterministic benchmark rules.'
        parameters    = @(
            [ordered] @{
                id          = 'parameter-benchmark'
                name        = 'Benchmark value'
                description = 'Integer payload value used by the benchmark.'
                type        = 'INTEGER'
            }
        )
        payloads      = @(
            [ordered] @{
                id           = 'payload-benchmark'
                name         = 'Benchmark payload'
                description  = 'Benchmark payload.'
                parameterIds = @( 'parameter-benchmark' )
            }
        )
        events        = $events.ToArray()
        conditions    = @(
            [ordered] @{
                id          = 'condition-benchmark'
                name        = 'Benchmark equality'
                description = 'Matches the benchmark payload value.'
                parameterId = 'parameter-benchmark'
                operator    = 'EQUALS'
                value       = 1
            }
        )
        conditionSets = @(
            [ordered] @{
                id           = 'condition-set-benchmark'
                name         = 'Benchmark condition set'
                description  = 'Single benchmark condition.'
                conditionIds = @( 'condition-benchmark' )
            }
        )
        actions       = @(
            [ordered] @{
                id          = $ActionIdentifier
                name        = 'Benchmark action'
                description = 'Benchmark action.'
            }
        )
        rules         = $rules.ToArray()
    }

    # Return the result produced by convert to JSON.

    return $model | ConvertTo-Json -Depth 10 -Compress
}

function New-JsonRequest
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string] $Method,

        [Parameter(Mandatory = $true)]
        [uri] $Uri,

        [string] $Body,
        [string] $BearerToken
    )

    # Initialize the request by applying new.

    $request = [System.Net.Http.HttpRequestMessage]::new(
        [System.Net.Http.HttpMethod]::new( $Method ),
        $Uri
    )

    # Handle the branch where body contains text.

    if ( -not [string]::IsNullOrWhiteSpace( $Body ) )
    {
        # Initialize the request.content by applying new.

        $request.Content = [System.Net.Http.StringContent]::new(
            $Body,
            [System.Text.Encoding]::UTF8,
            'application/json'
        )
    }

    # Handle the branch where bearer token contains text.

    if ( -not [string]::IsNullOrWhiteSpace( $BearerToken ) )
    {
        # Initialize the request.headers.authorization by applying new.

        $request.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new(
            'Bearer',
            $BearerToken
        )
    }

    # Return the request to the caller.

    return $request
}

function Send-JsonRequest
{
    param
    (
        [Parameter(Mandatory = $true)]
        [System.Net.Http.HttpClient] $Client,

        [Parameter(Mandatory = $true)]
        [string] $Method,

        [Parameter(Mandatory = $true)]
        [uri] $Uri,

        [string] $Body,
        [string] $BearerToken,
        [int] $ExpectedStatusCode = 200
    )

    # Initialize the request by applying new JSON request.

    $request  = New-JsonRequest `
        -Method $Method `
        -Uri $Uri `
        -Body $Body `
        -BearerToken $BearerToken
    $response = $null

    # Run the protected operation and route failures through the matching handler.

    try
    {
        # Prepare the response and response body values required by the send JSON request operation.

        $response     = $Client.SendAsync( $request ).GetAwaiter().GetResult()
        $responseBody = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()

        # Handle the branch where int response.status code differs from expected status code.

        if ( [int] $response.StatusCode -ne $ExpectedStatusCode )
        {
            throw (
                "HTTP $Method $Uri returned $([int] $response.StatusCode), " +
                "expected $ExpectedStatusCode.`n$responseBody"
            )
        }

        # Return the response body to the caller.

        return $responseBody
    }

    # Complete the required cleanup regardless of how the protected operation finishes.

    finally
    {
        # Handle the branch where response is available.

        if ( $null -ne $response )
        {
            # Perform the dispose calls required by the send JSON request operation.

            $response.Dispose()
        }

        # Perform the dispose calls required by the send JSON request operation.

        $request.Dispose()
    }
}

function Assert-EvaluationResponse
{
    param
    (
        [Parameter(Mandatory = $true)]
        [string] $ResponseBody,

        [Parameter(Mandatory = $true)]
        [string[]] $AllowedActionIdentifiers
    )

    # Initialize the evaluation by applying convert from JSON.

    $evaluation = $ResponseBody | ConvertFrom-Json

    # Handle the branch where evaluation.outcome differs from action or evaluation.action ID notin allowed action
    # identifiers.

    if (
        $evaluation.outcome -ne 'ACTION' -or
        $evaluation.actionId -notin $AllowedActionIdentifiers
    )
    {
        throw "The benchmark returned an incorrect evaluation result: $ResponseBody"
    }
}

function Invoke-ConcurrentEvaluations
{
    param
    (
        [Parameter(Mandatory = $true)]
        [System.Net.Http.HttpClient] $Client,

        [Parameter(Mandatory = $true)]
        [uri] $EvaluationUri,

        [Parameter(Mandatory = $true)]
        [string] $OccurrenceDocument,

        [Parameter(Mandatory = $true)]
        [int] $RequestCount,

        [Parameter(Mandatory = $true)]
        [string[]] $AllowedActionIdentifiers
    )

    # Prepare the requests and tasks values required by the invoke concurrent evaluations operation.

    $requests = [System.Collections.Generic.List[System.Net.Http.HttpRequestMessage]]::new()
    $tasks    = [System.Collections.Generic.List[System.Threading.Tasks.Task[System.Net.Http.HttpResponseMessage]]]::new()

    # Run the protected operation and route failures through the matching handler.

    try
    {
        # Repeat the indexed loop while its condition remains true.

        for ( $i = 0; $i -lt $RequestCount; $i++ )
        {
            # Initialize the request by applying new JSON request.

            $request = New-JsonRequest `
                -Method 'POST' `
                -Uri $EvaluationUri `
                -Body $OccurrenceDocument

            # Perform the add and send async calls required by the invoke concurrent evaluations operation.

            $requests.Add( $request )
            $tasks.Add( $Client.SendAsync( $request ) )
        }

        # Perform the wait all and to array calls required by the invoke concurrent evaluations operation.

        [System.Threading.Tasks.Task]::WaitAll(
            [System.Threading.Tasks.Task[]] $tasks.ToArray()
        )

        # Process each value supplied by the selected collection.

        foreach ( $task in $tasks )
        {
            # Initialize the response by applying get result and get awaiter.

            $response = $task.GetAwaiter().GetResult()

            # Run the protected operation and route failures through the matching handler.

            try
            {
                # Initialize the response body by applying get result, get awaiter, and read as string async.

                $responseBody = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()

                # Handle the branch where int response.status code differs from 200.

                if ( [int] $response.StatusCode -ne 200 )
                {
                    throw "A concurrent evaluation returned HTTP $([int] $response.StatusCode)."
                }

                # Perform the assert evaluation response calls required by the invoke concurrent evaluations operation.

                Assert-EvaluationResponse `
                    -ResponseBody $responseBody `
                    -AllowedActionIdentifiers $AllowedActionIdentifiers
            }

            # Complete the required cleanup regardless of how the protected operation finishes.

            finally
            {
                # Perform the dispose calls required by the invoke concurrent evaluations operation.

                $response.Dispose()
            }
        }
    }

    # Complete the required cleanup regardless of how the protected operation finishes.

    finally
    {
        # Process each value supplied by the selected collection.

        foreach ( $request in $requests )
        {
            # Perform the dispose calls required by the invoke concurrent evaluations operation.

            $request.Dispose()
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

# Handle the branch where report path is blank.

if ( [string]::IsNullOrWhiteSpace( $ReportPath ) )
{
    # Initialize the report path by applying join path.

    $ReportPath = Join-Path $projectDirectory 'build\verification\native\native-performance.md'
}

# Handle the branch where measured request count is less than 100.

if ( $MeasuredRequestCount -lt 100 )
{
    throw 'MeasuredRequestCount must be at least 100.'
}

# Prepare the resolved server path, resolved report path, report directory, temporary root base, temporary directory,
# data directory, output path, and error path values required by the script operation.

$resolvedServerPath     = ( Resolve-Path -LiteralPath $ServerExecutablePath ).Path
$resolvedReportPath     = [System.IO.Path]::GetFullPath( $ReportPath )
$reportDirectory        = Split-Path -Parent $resolvedReportPath
$temporaryRootBase      = [System.IO.Path]::GetFullPath( [System.IO.Path]::GetTempPath() )
$temporaryDirectory     = Join-Path $temporaryRootBase ( 'eca-native-benchmark-' + [guid]::NewGuid() )
$dataDirectory          = Join-Path $temporaryDirectory 'data'
$outputPath             = Join-Path $temporaryDirectory 'server.stdout.log'
$errorPath              = Join-Path $temporaryDirectory 'server.stderr.log'
$serverProcess          = $null

# Initialize the HTTP client handler by applying new.

$httpClientHandler      = [System.Net.Http.HttpClientHandler]::new()
$httpClientHandler.UseProxy                 = $false
$httpClientHandler.MaxConnectionsPerServer = 200

# Prepare the HTTP client and HTTP client.timeout values required by the script operation.

$httpClient             = [System.Net.Http.HttpClient]::new( $httpClientHandler )
$httpClient.Timeout     = [TimeSpan]::FromSeconds( 30 )

# Perform the new item and out null calls required by the script operation.

New-Item -ItemType Directory -Path $dataDirectory -Force | Out-Null

# Handle the branch where not test path literal path report directory.

if ( -not ( Test-Path -LiteralPath $reportDirectory ) )
{
    # Perform the new item and out null calls required by the script operation.

    New-Item -ItemType Directory -Path $reportDirectory | Out-Null
}

# Run the protected operation and route failures through the matching handler.

try
{
    # Initialize the port by applying get available port.

    $port                    = Get-AvailablePort
    $baseURL                 = "http://127.0.0.1:$port"
    $livenessUri             = [uri] "$baseURL/api/v1/health/live"
    $modelUri                = [uri] "$baseURL/api/v1/model"
    $evaluationUri           = [uri] "$baseURL/api/v1/evaluations"
    $serverArguments         = 'start --host 127.0.0.1 --port {0} --data-directory "{1}"' -f `
        $port, `
        $dataDirectory

    # Prepare the cold start stopwatch, server process, and deadline values required by the script operation.

    $coldStartStopwatch      = [System.Diagnostics.Stopwatch]::StartNew()
    $serverProcess           = Start-Process `
        -FilePath $resolvedServerPath `
        -ArgumentList $serverArguments `
        -WorkingDirectory $temporaryDirectory `
        -WindowStyle Hidden `
        -PassThru `
        -RedirectStandardOutput $outputPath `
        -RedirectStandardError $errorPath
    $deadline                = [DateTime]::UtcNow.AddSeconds( 20 )
    $liveness                = $null
    $startupProbeError       = $null

    # Continue processing while the loop condition remains true.

    while ( ( $null -eq $liveness ) -and ( [DateTime]::UtcNow -lt $deadline ) )
    {
        # Run the protected operation and route failures through the matching handler.

        try
        {
            # Initialize the liveness by applying send JSON request.

            $liveness = Send-JsonRequest `
                -Client $httpClient `
                -Method 'GET' `
                -Uri $livenessUri
        }

        # Handle failures raised by the protected operation.

        catch
        {
            # Initialize the startup probe error by applying to string.

            $startupProbeError = $_.Exception.ToString()

            # Handle the branch where server process.has exited.

            if ( $serverProcess.HasExited )
            {
                break
            }

            # Perform the start sleep calls required by the script operation.

            Start-Sleep -Milliseconds 25
        }
    }

    # Perform the stop calls required by the script operation.

    $coldStartStopwatch.Stop()

    # Handle the branch where liveness is unavailable.

    if ( $null -eq $liveness )
    {
        # Prepare the server output and server error values required by the script operation.

        $serverOutput = Get-Content -LiteralPath $outputPath -Raw -ErrorAction SilentlyContinue
        $serverError  = Get-Content -LiteralPath $errorPath -Raw -ErrorAction SilentlyContinue

        throw (
            "The native benchmark server did not become live.`n" +
            "Last probe error:`n$startupProbeError`n" +
            "$serverOutput`n$serverError"
        )
    }

    # Prepare the token path and deadline values required by the script operation.

    $tokenPath = Join-Path $dataDirectory 'control-token'
    $deadline  = [DateTime]::UtcNow.AddSeconds( 5 )

    # Continue processing while the loop condition remains true.

    while ( -not ( Test-Path -LiteralPath $tokenPath ) -and [DateTime]::UtcNow -lt $deadline )
    {
        # Perform the start sleep calls required by the script operation.

        Start-Sleep -Milliseconds 25
    }

    # Handle the branch where not test path literal path token path.

    if ( -not ( Test-Path -LiteralPath $tokenPath ) )
    {
        throw 'The native benchmark server did not create its control token.'
    }

    # Initialize the bearer token by applying trim and get content.

    $bearerToken          = ( Get-Content -LiteralPath $tokenPath -Raw ).Trim()
    $primaryAction        = 'action-primary'
    $secondaryAction      = 'action-secondary'

    # Prepare the primary model and secondary model values required by the script operation.

    $primaryModel         = New-BenchmarkModel `
        -ModelIdentifier 'native-benchmark-primary' `
        -ActionIdentifier $primaryAction
    $secondaryModel       = New-BenchmarkModel `
        -ModelIdentifier 'native-benchmark-secondary' `
        -ActionIdentifier $secondaryAction
    $occurrenceDocument   = '{"eventId":"event-00","payload":{"parameter-benchmark":1}}'

    # Perform the send JSON request and out null calls required by the script operation.

    Send-JsonRequest `
        -Client $httpClient `
        -Method 'PUT' `
        -Uri $modelUri `
        -Body $primaryModel `
        -BearerToken $bearerToken `
        -ExpectedStatusCode 201 | Out-Null

    # Repeat the indexed loop while its condition remains true.

    for ( $i = 0; $i -lt 25; $i++ )
    {
        # Initialize the warmup response by applying send JSON request.

        $warmupResponse = Send-JsonRequest `
            -Client $httpClient `
            -Method 'POST' `
            -Uri $evaluationUri `
            -Body $occurrenceDocument

        # Perform the assert evaluation response calls required by the script operation.

        Assert-EvaluationResponse `
            -ResponseBody $warmupResponse `
            -AllowedActionIdentifiers @( $primaryAction )
    }

    # Prepare the latencies and throughput stopwatch values required by the script operation.

    $latencies           = [System.Collections.Generic.List[double]]::new()
    $throughputStopwatch = [System.Diagnostics.Stopwatch]::StartNew()

    # Repeat the indexed loop while its condition remains true.

    for ( $i = 0; $i -lt $MeasuredRequestCount; $i++ )
    {
        # Prepare the request stopwatch and response body values required by the script operation.

        $requestStopwatch = [System.Diagnostics.Stopwatch]::StartNew()
        $responseBody     = Send-JsonRequest `
            -Client $httpClient `
            -Method 'POST' `
            -Uri $evaluationUri `
            -Body $occurrenceDocument

        # Perform the stop, add, and assert evaluation response calls required by the script operation.

        $requestStopwatch.Stop()
        $latencies.Add( $requestStopwatch.Elapsed.TotalMilliseconds )
        Assert-EvaluationResponse `
            -ResponseBody $responseBody `
            -AllowedActionIdentifiers @( $primaryAction )
    }

    # Perform the stop calls required by the script operation.

    $throughputStopwatch.Stop()

    # Prepare the sorted latencies, percentile index, p95 milliseconds, and throughput per second values required by
    # the script operation.

    $sortedLatencies       = @( $latencies | Sort-Object )
    $percentileIndex       = [Math]::Ceiling( $sortedLatencies.Count * 0.95 ) - 1
    $p95Milliseconds       = [Math]::Round(
        $sortedLatencies.GetValue( $percentileIndex ),
        3
    )
    $throughputPerSecond   = [Math]::Round(
        $MeasuredRequestCount / $throughputStopwatch.Elapsed.TotalSeconds,
        1
    )

    # Perform the invoke concurrent evaluations calls required by the script operation.

    Invoke-ConcurrentEvaluations `
        -Client $httpClient `
        -EvaluationUri $evaluationUri `
        -OccurrenceDocument $occurrenceDocument `
        -RequestCount 100 `
        -AllowedActionIdentifiers @( $primaryAction )

    # Prepare the replacement requests and replacement tasks values required by the script operation.

    $replacementRequests = [System.Collections.Generic.List[System.Net.Http.HttpRequestMessage]]::new()
    $replacementTasks    = [System.Collections.Generic.List[System.Threading.Tasks.Task[System.Net.Http.HttpResponseMessage]]]::new()

    # Run the protected operation and route failures through the matching handler.

    try
    {
        # Repeat the indexed loop while its condition remains true.

        for ( $i = 0; $i -lt 100; $i++ )
        {
            # Initialize the request by applying new JSON request.

            $request = New-JsonRequest `
                -Method 'POST' `
                -Uri $evaluationUri `
                -Body $occurrenceDocument

            # Perform the add and send async calls required by the script operation.

            $replacementRequests.Add( $request )
            $replacementTasks.Add( $httpClient.SendAsync( $request ) )
        }

        # Initialize the replacement stopwatch by applying start new.

        $replacementStopwatch = [System.Diagnostics.Stopwatch]::StartNew()

        # Perform the send JSON request, out null, stop, wait all, and to array calls required by the script operation.

        Send-JsonRequest `
            -Client $httpClient `
            -Method 'PUT' `
            -Uri $modelUri `
            -Body $secondaryModel `
            -BearerToken $bearerToken | Out-Null

        $replacementStopwatch.Stop()

        [System.Threading.Tasks.Task]::WaitAll(
            [System.Threading.Tasks.Task[]] $replacementTasks.ToArray()
        )

        # Process each value supplied by the selected collection.

        foreach ( $task in $replacementTasks )
        {
            # Initialize the response by applying get result and get awaiter.

            $response = $task.GetAwaiter().GetResult()

            # Run the protected operation and route failures through the matching handler.

            try
            {
                # Initialize the response body by applying get result, get awaiter, and read as string async.

                $responseBody = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()

                # Handle the branch where int response.status code differs from 200.

                if ( [int] $response.StatusCode -ne 200 )
                {
                    throw "An evaluation failed during replacement with HTTP $([int] $response.StatusCode)."
                }

                # Perform the assert evaluation response calls required by the script operation.

                Assert-EvaluationResponse `
                    -ResponseBody $responseBody `
                    -AllowedActionIdentifiers @( $primaryAction, $secondaryAction )
            }

            # Complete the required cleanup regardless of how the protected operation finishes.

            finally
            {
                # Perform the dispose calls required by the script operation.

                $response.Dispose()
            }
        }
    }

    # Complete the required cleanup regardless of how the protected operation finishes.

    finally
    {
        # Process each value supplied by the selected collection.

        foreach ( $request in $replacementRequests )
        {
            # Perform the dispose calls required by the script operation.

            $request.Dispose()
        }
    }

    # Perform the refresh calls required by the script operation.

    $serverProcess.Refresh()

    # Initialize the working set mi b by applying round.

    $workingSetMiB = [Math]::Round( $serverProcess.WorkingSet64 / 1MB, 1 )

    # Run the protected operation and route failures through the matching handler.

    try
    {
        # Prepare the processor name, memory gi b, and operating system values required by the script operation.

        $processorName = ( Get-CimInstance Win32_Processor | Select-Object -First 1 ).Name
        $memoryGiB     = [Math]::Round(
            ( Get-CimInstance Win32_ComputerSystem ).TotalPhysicalMemory / 1GB,
            1
        )
        $operatingSystem = Get-CimInstance Win32_OperatingSystem
        $operatingSystemDescription = (
            "$($operatingSystem.Caption) $($operatingSystem.Version), build $($operatingSystem.BuildNumber)"
        )
    }

    # Handle failures raised by the protected operation.

    catch
    {
        $processorName              = $env:PROCESSOR_IDENTIFIER
        $memoryGiB                  = 'Unavailable'
        $operatingSystemDescription = [Environment]::OSVersion.VersionString
    }

    # Prepare the cold start milliseconds and replacement milliseconds values required by the script operation.

    $coldStartMilliseconds = [Math]::Round( $coldStartStopwatch.Elapsed.TotalMilliseconds, 1 )
    $replacementMilliseconds = [Math]::Round(
        $replacementStopwatch.Elapsed.TotalMilliseconds,
        1
    )

    # Handle the branch where cold start milliseconds is at most 2000.0.

    $coldStartResult = if ( $coldStartMilliseconds -le 2000.0 ) { 'Pass' } else { 'Miss' }

    # Handle the branch where p95 milliseconds is at most 50.0.

    $latencyResult   = if ( $p95Milliseconds -le 50.0 ) { 'Pass' } else { 'Miss' }

    # Initialize the report lines by applying get date.

    $reportLines     = @(

        # Perform the get date calls required by the script operation.

        '# Native Performance',
        '',
        '| Field | Value |',
        '|---|---|',

        # Perform the get date calls required by the script operation.

        "| Date | $( Get-Date -Format 'yyyy-MM-dd HH:mm:ss K' ) |",
        "| Operating system | $operatingSystemDescription |",
        "| Processor | $processorName |",
        "| Installed memory | $memoryGiB GiB |",
        '| Model shape | 1 parameter, 1 payload, 10 events, 1 condition set, 1 action, 10,000 rules |',
        '| Payload | One concrete integer binding |',
        "| Measured sequential requests | $MeasuredRequestCount after 25 warmups |",
        '| Concurrent requests | 100 |',
        '',
        '## Results',
        '',
        '| Scenario | Target | Measurement | Result |',
        '|---|---:|---:|---|',
        "| Server cold start without model | <= 2,000 ms | $coldStartMilliseconds ms | $coldStartResult |",
        "| Evaluation p95 | <= 50 ms | $p95Milliseconds ms | $latencyResult |",
        "| Sequential throughput | Report | $throughputPerSecond requests/s | Recorded |",
        "| Native server working set | Report | $workingSetMiB MiB | Recorded |",
        '| Concurrent evaluation load | 100 correct responses | 100 correct responses | Pass |',
        "| Model replacement | Evaluations remain available | $replacementMilliseconds ms; 100/100 available | Pass |",
        '',
        'The benchmark uses loopback HTTP and the production native executable. A target miss requires an',
        'evidence-based disposition before release; it does not authorize semantic changes.'
    )

    # Perform the set content and write output calls required by the script operation.

    $reportLines | Set-Content -LiteralPath $resolvedReportPath -Encoding UTF8

    Write-Output "PASS: Native performance and availability measurements completed. Report: $resolvedReportPath"
}

# Complete the required cleanup regardless of how the protected operation finishes.

finally
{
    # Handle the branch where null differs from server process and not server process.has exited.

    if ( $null -ne $serverProcess -and -not $serverProcess.HasExited )
    {
        # Run the protected operation and route failures through the matching handler.

        try
        {
            # Perform the command and out null calls required by the script operation.

            & $resolvedServerPath `
                stop `
                --server-url $baseURL `
                --data-directory $dataDirectory `
                --timeout 5 `
                2>&1 | Out-Null
        }

        # Handle failures raised by the protected operation.

        catch
        {
            # The force-stop fallback below owns benchmark cleanup.

        }

        # Handle the branch where not server process.wait for exit 10000.

        if ( -not $serverProcess.WaitForExit( 10000 ) )
        {
            # Perform the stop process and wait for exit calls required by the script operation.

            Stop-Process -Id $serverProcess.Id -Force
            $serverProcess.WaitForExit()
        }
    }

    # Perform the dispose calls required by the script operation.

    $httpClient.Dispose()
    $httpClientHandler.Dispose()

    # Initialize the resolved temporary directory by applying get full path.

    $resolvedTemporaryDirectory = [System.IO.Path]::GetFullPath( $temporaryDirectory )

    # Handle the branch where test path literal path resolved temporary directory and resolved temporary
    # directory.starts with temporary root base system.string comparison ordinal ignore case.

    if (
        ( Test-Path -LiteralPath $resolvedTemporaryDirectory ) -and
        $resolvedTemporaryDirectory.StartsWith(
            $temporaryRootBase,
            [System.StringComparison]::OrdinalIgnoreCase
        )
    )
    {
        # Perform the remove item calls required by the script operation.

        Remove-Item -LiteralPath $resolvedTemporaryDirectory -Recurse -Force
    }
}
