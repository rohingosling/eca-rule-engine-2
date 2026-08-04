//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Supplies the production Picocli start, stop, model, help, and version command contract.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.rohingosling.eca.application.HostedModelSnapshot;
import com.rohingosling.eca.json.JsonHostedModelFactory;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command (
    name = "eca-server",
    synopsisHeading = "%nUsage: ",
    description =
    {
        "",
        "ECA Rule Engine 2 server.",
    },
    commandListHeading = "%nCommands:%n",
    footer = "",
    mixinStandardHelpOptions = true,
    version = ServerMain.VERSION,
    subcommands =
    {
        ServerMain.StartCommand.class,
        ServerMain.StopCommand.class,
        ServerMain.ModelCommand.class,
    }
)

//*********************************************************************************************************************
// Class: ServerMain
//
// Description:
//
//   Supplies the production Picocli start, stop, model, help, and version command contract.
//
//*********************************************************************************************************************

public final class ServerMain implements Runnable
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    public static final String VERSION = "eca-server 2.0.0";

    @Spec

    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private CommandSpec commandSpec;

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: main
    //
    // Description:
    //
    //   Starts the application using the supplied command-line arguments.
    //
    // Arguments:
    //
    //   arguments (String[]):
    //     The command-line arguments.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static void main ( String[] arguments )
    {
        // Initialize the exit code by applying execute.

        int exitCode = execute ( arguments );

        // Handle the branch where exit code differs from 0.

        if ( exitCode != 0 )
        {
            // Terminate the process with the computed exit status.

            System.exit ( exitCode );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: execute
    //
    // Description:
    //
    //   Performs the execute operation.
    //
    // Arguments:
    //
    //   arguments (String...):
    //     The command-line arguments.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static int execute ( String... arguments )
    {
        // Prepare the command line and effective arguments values needed by the execute operation.

        CommandLine commandLine = new CommandLine ( new ServerMain () );
        String[] effectiveArguments = arguments.length == 0
            ? new String[] { "start" }
            : arguments;

        // Perform the set execution exception handler, println, get err, and safe message calls required by the
        // execute operation.

        commandLine.setExecutionExceptionHandler (
            ( exception, parsedCommandLine, parseResult ) ->
            {
                // Perform the println, get err, and safe message calls required by the execute operation.

                parsedCommandLine.getErr ().println (
                    "ERROR: " + safeMessage ( exception, "Unexpected internal failure." )
                );

                // Return 7 as the execute result.

                return 7;
            }
        );

        // Return the result produced by execute.

        return commandLine.execute ( effectiveArguments );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: run
    //
    // Description:
    //
    //   Performs the run operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public void run ()
    {
        // Perform the usage, command line, and get out calls required by the run operation.

        this.commandSpec.commandLine ().usage ( this.commandSpec.commandLine ().getOut () );
    }

    @Command (
        name = "start",
        description = "Start the HTTP server in the foreground.",
        mixinStandardHelpOptions = true,
        version = VERSION
    )

    //*****************************************************************************************************************
    // Class: StartCommand
    //
    // Description:
    //
    //   Provides the start command behavior.
    //
    //*****************************************************************************************************************

    static final class StartCommand implements Callable <Integer>
    {
        @Option ( names = "--host", defaultValue = "127.0.0.1", description = "Listener host." )

        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private String host;

        @Option ( names = "--port", defaultValue = "8080", description = "Listener port; zero selects a free port." )
        private int port;

        @Option ( names = "--data-directory", description = "Server data directory." )
        private Path dataDirectory;

        @Option ( names = "--token-file", description = "Protected bearer-token file." )
        private Path tokenFile;

        @Option (
            names = "--allowed-origin",
            paramLabel = "<origin>",
            description = "Exact HTTP(S) browser origin to allow; repeat for additional origins."
        )
        private List <String> allowedOrigins = new ArrayList <> ();

        @Option (
            names = { "-m", "--model" },
            paramLabel = "<model-name.json>",
            description = "Validate, persist, and host a model during startup."
        )
        private Path modelPath;

        @Option (
            names = "--model-body-limit",
            defaultValue = "16777216",
            description = "Maximum model request bytes."
        )
        private int maximumModelBodyBytes;

        @Option (
            names = "--evaluation-body-limit",
            defaultValue = "1048576",
            description = "Maximum evaluation request bytes."
        )
        private int maximumEvaluationBodyBytes;

        @Option (
            names = "--header-limit",
            defaultValue = "32768",
            description = "Maximum HTTP header-block bytes."
        )
        private int maximumHeaderBytes;

        @Option (
            names = "--request-timeout",
            defaultValue = "30",
            description = "Request read timeout in seconds."
        )
        private int requestTimeoutSeconds;

        @Option (
            names = "--shutdown-timeout",
            defaultValue = "15",
            description = "Graceful shutdown timeout in seconds."
        )
        private int shutdownTimeoutSeconds;

        @Spec
        private CommandSpec commandSpec;

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: call
        //
        // Description:
        //
        //   Performs the call operation.
        //
        // Returns:
        //
        //   The result of the operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public Integer call ()
        {
            // Prepare the standard output, standard error, and selected data directory values needed by the call
            // operation.

            PrintWriter standardOutput = this.commandSpec.commandLine ().getOut ();
            PrintWriter standardError  = this.commandSpec.commandLine ().getErr ();
            Path selectedDataDirectory = this.dataDirectory == null
                ? ServerConfiguration.defaultDataDirectory ()
                : this.dataDirectory;
            ServerConfiguration configuration;

            // Apply configure logging and write banner to the server terminal output for the call operation.

            ServerTerminalOutput.configureLogging ();
            ServerTerminalOutput.writeBanner ( standardOutput );

            try
            {
                // Update the configuration from the of seconds result.

                configuration = new ServerConfiguration (
                    this.host,
                    this.port,
                    selectedDataDirectory,
                    this.tokenFile,
                    System.getenv ( "ECA_SERVER_TOKEN" ),
                    this.allowedOrigins,
                    this.maximumModelBodyBytes,
                    this.maximumEvaluationBodyBytes,
                    this.maximumHeaderBytes,
                    Duration.ofSeconds ( this.requestTimeoutSeconds ),
                    Duration.ofSeconds ( this.shutdownTimeoutSeconds )
                );
            }

            // Handle runtime failures captured as exception.

            catch ( RuntimeException exception )
            {
                // Apply println and safe message to the standard error for the call operation.

                standardError.println (
                    "ERROR: " + safeMessage ( exception, "The server configuration is invalid." )
                );

                // Return process exit status 4.

                return 4;
            }

            // Handle the branch where model path is available.

            if ( this.modelPath != null )
            {
                try
                {
                    // Validate and install the configured startup model.

                    installStartupModel ( this.modelPath, configuration );
                }

                // Handle I/O failures captured as exception.

                catch ( IOException exception )
                {
                    // Write the status message to the selected command stream.

                    standardError.println ( "ERROR: The startup model file could not be read." );

                    // Return process exit status 4 when model path is available.

                    return 4;
                }

                // Handle illegal argument failures captured as exception.

                catch ( IllegalArgumentException exception )
                {
                    // Apply println and safe message to the standard error for the call operation.

                    standardError.println (
                        "ERROR: " + safeMessage ( exception, "The startup model is invalid." )
                    );

                    // Return process exit status 3 when model path is available.

                    return 3;
                }

                // Handle runtime failures captured as exception.

                catch ( RuntimeException exception )
                {
                    // Apply println and safe message to the standard error for the call operation.

                    standardError.println (
                        "ERROR: " + safeMessage (
                            exception,
                            "The startup model could not be persisted."
                        )
                    );

                    // Return process exit status 4 when model path is available.

                    return 4;
                }
            }

            // Open the scoped resources for the protected operation and close them automatically afterward.

            try ( EcaHttpServer server = EcaHttpServer.start ( configuration ) )
            {
                // Initialize the shutdown hook with a new thread.

                Thread shutdownHook = new Thread ( server::close, "eca-server-shutdown" );

                // Perform the add shutdown hook and get runtime calls required by the call operation.

                Runtime.getRuntime ().addShutdownHook ( shutdownHook );

                try
                {
                    // Perform the log listening, get host, get port, and await termination calls required by the call
                    // operation.

                    ServerTerminalOutput.logListening (
                        configuration.getHost (),
                        server.getPort ()
                    );
                    server.awaitTermination ();
                }

                // Complete the required cleanup regardless of how the protected operation finishes.

                finally
                {
                    try
                    {
                        // Perform the remove shutdown hook and get runtime calls required by the call operation.

                        Runtime.getRuntime ().removeShutdownHook ( shutdownHook );
                    }

                    // Handle illegal state failures captured as exception.

                    catch ( IllegalStateException exception )
                    {
                        // Process shutdown is already in progress.

                    }
                }

                // Return process exit status 0.

                return 0;
            }

            // Handle interrupted failures captured as exception.

            catch ( InterruptedException exception )
            {
                // Perform the interrupt, current thread, and println calls required by the call operation.

                Thread.currentThread ().interrupt ();
                standardError.println ( "ERROR: Server execution was interrupted." );

                // Return process exit status 4.

                return 4;
            }

            // Handle runtime failures captured as exception.

            catch ( RuntimeException exception )
            {
                // Apply println and safe message to the standard error for the call operation.

                standardError.println (
                    "ERROR: " + safeMessage ( exception, "The server could not be started." )
                );

                // Return process exit status 4.

                return 4;
            }
        }
    }

    @Command (
        name = "stop",
        description = "Request authenticated graceful shutdown.",
        mixinStandardHelpOptions = true,
        version = VERSION
    )

    //*****************************************************************************************************************
    // Class: StopCommand
    //
    // Description:
    //
    //   Provides the stop command behavior.
    //
    //*****************************************************************************************************************

    static final class StopCommand implements Callable <Integer>
    {
        @Option (
            names = "--server-url",
            defaultValue = "http://127.0.0.1:8080",
            description = "Server base URL."
        )

        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private String serverUrl;

        @Option ( names = "--data-directory", description = "Server data directory for token discovery." )
        private Path dataDirectory;

        @Option ( names = "--token-file", description = "Protected bearer-token file." )
        private Path tokenFile;

        @Option ( names = "--timeout", defaultValue = "30", description = "Request timeout in seconds." )
        private int timeoutSeconds;

        @Spec
        private CommandSpec commandSpec;

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: call
        //
        // Description:
        //
        //   Performs the call operation.
        //
        // Returns:
        //
        //   The result of the operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public Integer call ()
        {
            // Prepare the standard output and standard error values needed by the call operation.

            PrintWriter standardOutput = this.commandSpec.commandLine ().getOut ();
            PrintWriter standardError  = this.commandSpec.commandLine ().getErr ();

            try
            {
                // Prepare the client and response values needed by the call operation.

                ServerCommandClient client = createClient (
                    this.serverUrl,
                    this.tokenFile,
                    this.dataDirectory,
                    this.timeoutSeconds
                );
                ServerCommandClient.CommandResponse response = client.stop ();

                // Handle the branch where response is not successful.

                if ( !response.isSuccessful () )
                {
                    // Perform the println and read problem detail calls required by the call operation.

                    standardError.println ( "ERROR: " + client.readProblemDetail ( response ) );

                    // Return process exit status 6 when response is not successful.

                    return 6;
                }

                // Write the status message to the selected command stream.

                standardOutput.println ( "Graceful shutdown accepted." );

                // Return process exit status 0.

                return 0;
            }

            // Handle server command client server connection failures captured as exception.

            catch ( ServerCommandClient.ServerConnectionException exception )
            {
                // Perform the println and get message calls required by the call operation.

                standardError.println ( "ERROR: " + exception.getMessage () );

                // Return process exit status 5.

                return 5;
            }

            // Handle runtime failures captured as exception.

            catch ( RuntimeException exception )
            {
                // Apply println and safe message to the standard error for the call operation.

                standardError.println (
                    "ERROR: " + safeMessage ( exception, "The stop command configuration is invalid." )
                );

                // Return process exit status 4.

                return 4;
            }
        }
    }

    @Command (
        name = "model",
        description = "Validate and upload an authoring-model JSON file.",
        mixinStandardHelpOptions = true,
        version = VERSION
    )

    //*****************************************************************************************************************
    // Class: ModelCommand
    //
    // Description:
    //
    //   Provides the model command behavior.
    //
    //*****************************************************************************************************************

    static final class ModelCommand implements Callable <Integer>
    {
        @Parameters ( index = "0", paramLabel = "<model-name.json>", description = "Model JSON file." )

        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private Path modelPath;

        @Option (
            names = "--server-url",
            defaultValue = "http://127.0.0.1:8080",
            description = "Server base URL."
        )
        private String serverUrl;

        @Option ( names = "--data-directory", description = "Server data directory for token discovery." )
        private Path dataDirectory;

        @Option ( names = "--token-file", description = "Protected bearer-token file." )
        private Path tokenFile;

        @Option ( names = "--timeout", defaultValue = "30", description = "Request timeout in seconds." )
        private int timeoutSeconds;

        @Spec
        private CommandSpec commandSpec;

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: call
        //
        // Description:
        //
        //   Performs the call operation.
        //
        // Returns:
        //
        //   The result of the operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public Integer call ()
        {
            // Prepare the standard output and standard error values needed by the call operation.

            PrintWriter standardOutput = this.commandSpec.commandLine ().getOut ();
            PrintWriter standardError  = this.commandSpec.commandLine ().getErr ();
            byte [] modelDocument;

            try
            {
                // Update the model document from the read all bytes result.

                modelDocument = Files.readAllBytes ( this.modelPath );

                // Complete the call step by calling create.

                new JsonHostedModelFactory ().create ( modelDocument );
            }

            // Handle I/O failures captured as exception.

            catch ( IOException exception )
            {
                // Write the status message to the selected command stream.

                standardError.println ( "ERROR: The model file could not be read." );

                // Return process exit status 4.

                return 4;
            }

            // Handle illegal argument failures captured as exception.

            catch ( IllegalArgumentException exception )
            {
                // Apply println and safe message to the standard error for the call operation.

                standardError.println (
                    "ERROR: " + safeMessage ( exception, "The model is invalid." )
                );

                // Return process exit status 3.

                return 3;
            }

            try
            {
                // Prepare the client and response values needed by the call operation.

                ServerCommandClient client = createClient (
                    this.serverUrl,
                    this.tokenFile,
                    this.dataDirectory,
                    this.timeoutSeconds
                );
                ServerCommandClient.CommandResponse response = client.replaceModel ( modelDocument );

                // Handle the branch where response is not successful.

                if ( !response.isSuccessful () )
                {
                    // Perform the println and read problem detail calls required by the call operation.

                    standardError.println ( "ERROR: " + client.readProblemDetail ( response ) );

                    // Return process exit status 6 when response is not successful.

                    return 6;
                }

                // Perform the println and read model revision calls required by the call operation.

                standardOutput.println (
                    "Model accepted at revision " + client.readModelRevision ( response ) + "."
                );

                // Return process exit status 0.

                return 0;
            }

            // Handle server command client server connection failures captured as exception.

            catch ( ServerCommandClient.ServerConnectionException exception )
            {
                // Perform the println and get message calls required by the call operation.

                standardError.println ( "ERROR: " + exception.getMessage () );

                // Return process exit status 5.

                return 5;
            }

            // Handle runtime failures captured as exception.

            catch ( RuntimeException exception )
            {
                // Apply println and safe message to the standard error for the call operation.

                standardError.println (
                    "ERROR: " + safeMessage ( exception, "The model command configuration is invalid." )
                );

                // Return process exit status 4.

                return 4;
            }
        }
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: installStartupModel
    //
    // Description:
    //
    //   Performs the install startup model operation.
    //
    // Arguments:
    //
    //   modelPath (Path):
    //     The model path to use.
    //
    //   configuration (ServerConfiguration):
    //     The configuration to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void installStartupModel (
        Path modelPath,
        ServerConfiguration configuration
    ) throws IOException
    {
        // Initialize the model document by applying read all bytes.

        byte [] modelDocument = Files.readAllBytes ( modelPath );

        // Apply enforce model document limit and get maximum model body bytes to the configuration for the install
        // startup model operation.

        enforceModelDocumentLimit (
            modelDocument,
            configuration.getMaximumModelBodyBytes ()
        );

        // Prepare the model snapshot and canonical document values needed by the install startup model operation.

        HostedModelSnapshot modelSnapshot = new JsonHostedModelFactory ().create ( modelDocument );
        byte [] canonicalDocument         = modelSnapshot.getCanonicalDocument ();

        // Perform the enforce model document limit, get maximum model body bytes, persist, resolve, and get data
        // directory calls required by the install startup model operation.

        enforceModelDocumentLimit (
            canonicalDocument,
            configuration.getMaximumModelBodyBytes ()
        );

        new AtomicFileActiveModelStore (
            configuration.getDataDirectory ().resolve ( "active-model.json" )
        ).persist ( canonicalDocument );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: enforceModelDocumentLimit
    //
    // Description:
    //
    //   Performs the enforce model document limit operation.
    //
    // Arguments:
    //
    //   modelDocument (byte []):
    //     The model document to use.
    //
    //   maximumModelBodyBytes (int):
    //     The maximum model body bytes to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void enforceModelDocumentLimit (
        byte [] modelDocument,
        int maximumModelBodyBytes
    )
    {
        // Reject the operation when model document length exceeds maximum model body bytes.

        if ( modelDocument.length > maximumModelBodyBytes )
        {
            throw new IllegalArgumentException (
                "The startup model exceeds the configured model body limit."
            );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createClient
    //
    // Description:
    //
    //   Performs the create client operation.
    //
    // Arguments:
    //
    //   serverUrl (String):
    //     The server url to use.
    //
    //   tokenFile (Path):
    //     The token file to use.
    //
    //   dataDirectory (Path):
    //     The data directory to use.
    //
    //   timeoutSeconds (int):
    //     The timeout seconds to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static ServerCommandClient createClient (
        String serverUrl,
        Path tokenFile,
        Path dataDirectory,
        int timeoutSeconds
    )
    {
        // Reject the operation when timeout seconds is at most 0.

        if ( timeoutSeconds <= 0 )
        {
            throw new IllegalArgumentException ( "timeout must be positive." );
        }

        // Initialize the environment token by applying getenv.

        String environmentToken = System.getenv ( "ECA_SERVER_TOKEN" );
        String bearerToken;

        // Handle the branch where token file is available.

        if ( tokenFile != null )
        {
            // Update the bearer token from the load existing result.

            bearerToken = ControlTokenFile.loadExisting ( tokenFile );
        }

        // Handle the alternative where environment token is available and environment token contains text.

        else if ( environmentToken != null && !environmentToken.isBlank () )
        {
            // Update the bearer token from the trim result.

            bearerToken = environmentToken.trim ();
        }

        // Handle the alternative path when the preceding condition is not satisfied.

        else
        {
            // Initialize the selected data directory by applying default data directory.

            Path selectedDataDirectory = dataDirectory == null
                ? ServerConfiguration.defaultDataDirectory ()
                : dataDirectory;

            // Update the bearer token from the resolve result.

            bearerToken = ControlTokenFile.loadExisting (
                selectedDataDirectory.resolve ( "control-token" )
            );
        }

        // Prepare the request timeout and connect timeout values needed by the create client operation.

        Duration requestTimeout = Duration.ofSeconds ( timeoutSeconds );
        Duration connectTimeout = Duration.ofSeconds ( Math.min ( 5, timeoutSeconds ) );

        // Return a newly constructed server command client containing the operation result.

        return new ServerCommandClient (
            serverUrl,
            bearerToken,
            connectTimeout,
            requestTimeout
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: safeMessage
    //
    // Description:
    //
    //   Performs the safe message operation.
    //
    // Arguments:
    //
    //   throwable (Throwable):
    //     The exception that caused the operation to fail.
    //
    //   fallback (String):
    //     The fallback to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String safeMessage ( Throwable throwable, String fallback )
    {
        // Initialize the message by applying get message.

        String message = throwable.getMessage ();

        // Return the value selected according to message is unavailable or message is blank.

        return message == null || message.isBlank () ? fallback : message;
    }
}
