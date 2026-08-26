//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Hosts the versioned ECA HTTP API, authentication boundary, persistence adapter, and graceful lifecycle.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.rohingosling.eca.application.HostedEvaluation;
import com.rohingosling.eca.application.HostedModelApplication;
import com.rohingosling.eca.application.HostedModelLimits;
import com.rohingosling.eca.application.HostedModelNotReadyException;
import com.rohingosling.eca.application.HostedModelReadiness;
import com.rohingosling.eca.application.HostedModelReplacement;
import com.rohingosling.eca.application.HostedModelSnapshot;
import com.rohingosling.eca.application.HostedModelSummary;
import com.rohingosling.eca.json.HttpJsonCodec;
import com.rohingosling.eca.json.JsonHostedModelFactory;

import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.helidon.webserver.http1.Http1Config;

//*********************************************************************************************************************
// Class: EcaHttpServer
//
// Description:
//
//   Hosts the versioned ECA HTTP API, authentication boundary, persistence adapter, and graceful lifecycle.
//
//*********************************************************************************************************************

public final class EcaHttpServer implements AutoCloseable
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    public static final String LIVENESS_PATH  = "/api/v1/health/live";
    public static final String READINESS_PATH = "/api/v1/health/ready";
    public static final String MODEL_PATH     = "/api/v1/model";
    public static final String EVALUATION_PATH = "/api/v1/evaluations";
    public static final String STOP_PATH       = "/api/v1/management/stop";

    private static final String JSON_MEDIA_TYPE         = "application/json";
    private static final String PROBLEM_JSON_MEDIA_TYPE = "application/problem+json";
    private static final String BEARER_PREFIX            = "Bearer ";

    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final ServerConfiguration configuration;
    private final WebServer webServer;
    private final CountDownLatch shutdownLatch;
    private final AtomicBoolean shutdownStarted;

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getPort
    //
    // Description:
    //
    //   Returns the port.
    //
    // Returns:
    //
    //   The port.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public int getPort ()
    {
        // Return the result produced by port.

        return this.webServer.port ();
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: EcaHttpServer
    //
    // Description:
    //
    //   Creates the EcaHttpServer instance from the supplied values.
    //
    // Arguments:
    //
    //   configuration (ServerConfiguration):
    //     The configuration to use.
    //
    //   webServer (WebServer):
    //     The web server to use.
    //
    //   shutdownLatch (CountDownLatch):
    //     The shutdown latch to use.
    //
    //   shutdownStarted (AtomicBoolean):
    //     The shutdown started to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private EcaHttpServer (
        ServerConfiguration configuration,
        WebServer webServer,
        CountDownLatch shutdownLatch,
        AtomicBoolean shutdownStarted
    )
    {
        this.configuration  = configuration;
        this.webServer      = webServer;
        this.shutdownLatch  = shutdownLatch;
        this.shutdownStarted = shutdownStarted;
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: start
    //
    // Description:
    //
    //   Creates a EcaHttpServer instance for the start case.
    //
    // Arguments:
    //
    //   configuration (ServerConfiguration):
    //     The configuration to use.
    //
    // Returns:
    //
    //   The resulting EcaHttpServer instance.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static EcaHttpServer start ( ServerConfiguration configuration )
    {
        // Prepare the bearer token, authenticator, shutdown latch, shutdown started, web server reference, shutdown
        // action, application, server JSON, and routing builder values needed by the start operation.

        String bearerToken = resolveBearerToken ( configuration );
        BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator ( bearerToken );
        CountDownLatch shutdownLatch            = new CountDownLatch ( 1 );
        AtomicBoolean shutdownStarted           = new AtomicBoolean ();
        AtomicReference <WebServer> webServerReference = new AtomicReference <WebServer> ();

        Runnable shutdownAction = () ->
        {
            // Stop this path and return its result when shutdown started compare and set does not succeed.

            if ( !shutdownStarted.compareAndSet ( false, true ) )
            {
                return;
            }

            // Perform the start, name, daemon, of platform, get, is running, stop, and count down calls required by
            // the start operation.

            Thread.ofPlatform ()
                .daemon ( true )
                .name ( "eca-server-stop" )
                .start (
                    () ->
                    {
                        // Initialize the web server by applying get.

                        WebServer webServer = webServerReference.get ();

                        // Handle the branch where web server is available and web server is running.

                        if ( webServer != null && webServer.isRunning () )
                        {
                            // Stop the web server and release its resources.

                            webServer.stop ();
                        }

                        // Release threads waiting on the coordination latch.

                        shutdownLatch.countDown ();
                    }
                );
        };

        HostedModelApplication application = new HostedModelApplication (
            new JsonHostedModelFactory (),
            new AtomicFileActiveModelStore (
                configuration.getDataDirectory ().resolve ( "active-model.json" )
            ),
            shutdownAction::run,
            new StructuredHostedModelLogger ( true ),
            new HostedModelLimits ( configuration.getMaximumModelBodyBytes () )
        );
        HttpJsonCodec serverJson                  = new HttpJsonCodec ();
        HttpRouting.Builder routingBuilder        = HttpRouting.builder ();

        // Register the HTTP routes exposed by the server.

        registerRoutes (
            routingBuilder,
            configuration,
            authenticator,
            application,
            serverJson
        );

        // Initialize the web server by applying start, build, add routing, add protocol, connection config, max in
        // memory entity, max payload size, shutdown grace period, shutdown hook, port, host, builder, get host, get
        // port, get shutdown grace period, get maximum model body bytes, connect timeout, read timeout, get request
        // timeout, max headers size, and get maximum header bytes.

        WebServer webServer = WebServer.builder ()
            .host ( configuration.getHost () )
            .port ( configuration.getPort () )
            .shutdownHook ( false )
            .shutdownGracePeriod ( configuration.getShutdownGracePeriod () )
            .maxPayloadSize ( configuration.getMaximumModelBodyBytes () )
            .maxInMemoryEntity ( configuration.getMaximumModelBodyBytes () )
            .connectionConfig (
                connectionBuilder -> connectionBuilder
                    .readTimeout ( configuration.getRequestTimeout () )
                    .connectTimeout ( configuration.getRequestTimeout () )
            )
            .addProtocol (
                Http1Config.builder ()
                    .maxHeadersSize ( configuration.getMaximumHeaderBytes () )
                    .build ()
            )
            .addRouting ( routingBuilder )
            .build ()
            .start ();

        // Set the set on the web server reference.

        webServerReference.set ( webServer );

        // Return a newly constructed ECA HTTP server containing the operation result.

        return new EcaHttpServer (
            configuration,
            webServer,
            shutdownLatch,
            shutdownStarted
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: awaitTermination
    //
    // Description:
    //
    //   Performs the await termination operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void awaitTermination () throws InterruptedException
    {
        // Wait for the coordination latch to be released.

        this.shutdownLatch.await ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: close
    //
    // Description:
    //
    //   Performs the close operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public void close ()
    {
        // Handle the branch where shutdown started compare and set succeeds.

        if ( this.shutdownStarted.compareAndSet ( false, true ) )
        {
            // Handle the branch where web server is running.

            if ( this.webServer.isRunning () )
            {
                // Stop the web server and release its resources.

                this.webServer.stop ();
            }

            // Release threads waiting on the coordination latch.

            this.shutdownLatch.countDown ();
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: registerRoutes
    //
    // Description:
    //
    //   Performs the register routes operation.
    //
    // Arguments:
    //
    //   routingBuilder (HttpRouting.Builder):
    //     The routing builder to use.
    //
    //   configuration (ServerConfiguration):
    //     The configuration to use.
    //
    //   authenticator (BearerTokenAuthenticator):
    //     The authenticator to use.
    //
    //   application (HostedModelApplication):
    //     The application to use.
    //
    //   serverJson (HttpJsonCodec):
    //     The server json to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void registerRoutes (
        HttpRouting.Builder routingBuilder,
        ServerConfiguration configuration,
        BearerTokenAuthenticator authenticator,
        HostedModelApplication application,
        HttpJsonCodec serverJson
    )
    {
        // Install browser preflight and response handling before the API routes and their authentication checks.

        routingBuilder.addFilter ( new BrowserAccessPolicy ( configuration ) );

        // Perform the get, send JSON, liveness, inspect readiness, is ready, readiness, get model, put, put model,
        // post, evaluate, stop, any, send problem, to string, and path calls required by the register routes
        // operation.

        routingBuilder.get (
            LIVENESS_PATH,
            ( request, response ) -> sendJson (
                response,
                Status.OK_200,
                serverJson.liveness ()
            )
        );
        routingBuilder.get (
            READINESS_PATH,
            ( request, response ) ->
            {
                // Initialize the readiness by applying inspect readiness.

                HostedModelReadiness readiness = application.inspectReadiness ();

                // Perform the send JSON, is ready, and readiness calls required by the register routes operation.

                sendJson (
                    response,
                    readiness.isReady () ? Status.OK_200 : Status.SERVICE_UNAVAILABLE_503,
                    serverJson.readiness ( readiness )
                );
            }
        );
        routingBuilder.get (
            MODEL_PATH,
            ( request, response ) -> getModel (
                request,
                response,
                authenticator,
                application,
                serverJson
            )
        );
        routingBuilder.put (
            MODEL_PATH,
            ( request, response ) -> putModel (
                request,
                response,
                configuration,
                authenticator,
                application,
                serverJson
            )
        );
        routingBuilder.post (
            EVALUATION_PATH,
            ( request, response ) -> evaluate (
                request,
                response,
                configuration,
                authenticator,
                application,
                serverJson
            )
        );
        routingBuilder.post (
            STOP_PATH,
            ( request, response ) -> stop (
                request,
                response,
                authenticator,
                application,
                serverJson
            )
        );
        routingBuilder.any (
            ( request, response ) -> sendProblem (
                response,
                serverJson,
                Status.NOT_FOUND_404,
                "Not Found",
                "No API route matches this request.",
                request.path ().toString ()
            )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getModel
    //
    // Description:
    //
    //   Returns the model.
    //
    // Arguments:
    //
    //   request (ServerRequest):
    //     The request to use.
    //
    //   response (ServerResponse):
    //     The response to use.
    //
    //   authenticator (BearerTokenAuthenticator):
    //     The authenticator to use.
    //
    //   application (HostedModelApplication):
    //     The application to use.
    //
    //   serverJson (HttpJsonCodec):
    //     The server json to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void getModel (
        ServerRequest request,
        ServerResponse response,
        BearerTokenAuthenticator authenticator,
        HostedModelApplication application,
        HttpJsonCodec serverJson
    )
    {
        // Stop this path and return its result when the operation is not loopback request and the operation
        // authenticate does not succeed.

        if ( !isLoopbackRequest ( request ) && !authenticate ( request, response, authenticator, serverJson ) )
        {
            return;
        }

        // Initialize the active model by applying get active model.

        Optional <HostedModelSnapshot> activeModel = application.getActiveModel ();

        // Handle the branch where active model contains no values.

        if ( activeModel.isEmpty () )
        {
            // Send the appropriate HTTP error response to the client.

            sendProblem (
                response,
                serverJson,
                Status.NOT_FOUND_404,
                "Active Model Not Found",
                "No active model is available.",
                MODEL_PATH
            );

            return;
        }

        // Initialize the snapshot by applying get.

        HostedModelSnapshot snapshot = activeModel.get ();

        // Perform the status, header, entity tag, get revision, send, and get canonical document calls required by the
        // get model operation.

        response.status ( Status.OK_200 );
        response.header ( HeaderNames.CONTENT_TYPE, JSON_MEDIA_TYPE );
        response.header ( HeaderNames.ETAG, entityTag ( snapshot.getRevision () ) );
        response.header ( "X-Model-Revision", snapshot.getRevision () );
        response.send ( snapshot.getCanonicalDocument () );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: putModel
    //
    // Description:
    //
    //   Performs the put model operation.
    //
    // Arguments:
    //
    //   request (ServerRequest):
    //     The request to use.
    //
    //   response (ServerResponse):
    //     The response to use.
    //
    //   configuration (ServerConfiguration):
    //     The configuration to use.
    //
    //   authenticator (BearerTokenAuthenticator):
    //     The authenticator to use.
    //
    //   application (HostedModelApplication):
    //     The application to use.
    //
    //   serverJson (HttpJsonCodec):
    //     The server json to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void putModel (
        ServerRequest request,
        ServerResponse response,
        ServerConfiguration configuration,
        BearerTokenAuthenticator authenticator,
        HostedModelApplication application,
        HttpJsonCodec serverJson
    )
    {
        // Stop this path and return its result when the operation authenticate does not succeed.

        if ( !authenticate ( request, response, authenticator, serverJson ) )
        {
            return;
        }

        // Stop this path and return its result when the operation require JSON media type does not succeed.

        if ( !requireJsonMediaType ( request, response, serverJson, MODEL_PATH ) )
        {
            return;
        }

        try
        {
            // Prepare the document, replacing existing model, replacement, and summary values needed by the put model
            // operation.

            byte [] document = readLimited (
                request,
                configuration.getMaximumModelBodyBytes ()
            );
            boolean replacingExistingModel = application.getActiveModel ().isPresent ();
            HostedModelReplacement replacement = application.replaceActiveModel ( document );
            HostedModelSummary summary           = replacement.getSummary ();

            // Perform the status, header, entity tag, get revision, send, and summary calls required by the put model
            // operation.

            response.status (
                replacingExistingModel ? Status.OK_200 : Status.CREATED_201
            );
            response.header ( HeaderNames.CONTENT_TYPE, JSON_MEDIA_TYPE );
            response.header ( HeaderNames.ETAG, entityTag ( summary.getRevision () ) );
            response.header ( "X-Model-Revision", summary.getRevision () );
            response.send ( serverJson.summary ( summary ) );
        }

        // Handle payload too large failures captured as exception.

        catch ( PayloadTooLargeException exception )
        {
            // Send the appropriate HTTP error response to the client.

            sendPayloadTooLarge ( response, serverJson, MODEL_PATH, exception );
        }

        // Handle illegal argument failures captured as exception.

        catch ( IllegalArgumentException exception )
        {
            // Prepare the validation failure and malformed JSON values needed by the put model operation.

            boolean validationFailure = serverJson.isModelValidationFailure ( exception );
            boolean malformedJson     = serverJson.isMalformedJson ( exception );

            // Apply send problem, validation detail, and safe detail to the server JSON for the put model operation.

            sendProblem (
                response,
                serverJson,
                malformedJson ? Status.BAD_REQUEST_400 : Status.UNPROCESSABLE_CONTENT_422,
                malformedJson
                    ? "Malformed JSON"
                    : validationFailure ? "Model Validation Failed" : "Invalid Model",
                validationFailure
                    ? serverJson.validationDetail ( exception )
                    : safeDetail ( exception, "The model document is invalid." ),
                MODEL_PATH
            );
        }

        // Handle runtime failures captured as exception.

        catch ( RuntimeException exception )
        {
            // Send the appropriate HTTP error response to the client.

            sendProblem (
                response,
                serverJson,
                Status.INTERNAL_SERVER_ERROR_500,
                "Model Replacement Failed",
                "The active model could not be replaced.",
                MODEL_PATH
            );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: evaluate
    //
    // Description:
    //
    //   Performs the evaluate operation.
    //
    // Arguments:
    //
    //   request (ServerRequest):
    //     The request to use.
    //
    //   response (ServerResponse):
    //     The response to use.
    //
    //   configuration (ServerConfiguration):
    //     The configuration to use.
    //
    //   authenticator (BearerTokenAuthenticator):
    //     The authenticator to use.
    //
    //   application (HostedModelApplication):
    //     The application to use.
    //
    //   serverJson (HttpJsonCodec):
    //     The server json to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void evaluate (
        ServerRequest request,
        ServerResponse response,
        ServerConfiguration configuration,
        BearerTokenAuthenticator authenticator,
        HostedModelApplication application,
        HttpJsonCodec serverJson
    )
    {
        // Stop this path and return its result when the operation is not loopback request and the operation
        // authenticate does not succeed.

        if ( !isLoopbackRequest ( request ) && !authenticate ( request, response, authenticator, serverJson ) )
        {
            return;
        }

        // Stop this path and return its result when the operation require JSON media type does not succeed.

        if ( !requireJsonMediaType ( request, response, serverJson, EVALUATION_PATH ) )
        {
            return;
        }

        try
        {
            // Prepare the document, start nanoseconds, hosted evaluation, and elapsed microseconds values needed by
            // the evaluate operation.

            byte [] document = readLimited (
                request,
                configuration.getMaximumEvaluationBodyBytes ()
            );
            long startNanoseconds = System.nanoTime ();
            HostedEvaluation hostedEvaluation = serverJson.evaluate ( application, document );
            long elapsedMicroseconds = Math.max (
                0,
                Duration.ofNanos ( System.nanoTime () - startNanoseconds ).toNanos () / 1000
            );

            // Apply send JSON and evaluation to the server JSON for the evaluate operation.

            sendJson (
                response,
                Status.OK_200,
                serverJson.evaluation ( hostedEvaluation, elapsedMicroseconds )
            );
        }

        // Handle payload too large failures captured as exception.

        catch ( PayloadTooLargeException exception )
        {
            // Send the appropriate HTTP error response to the client.

            sendPayloadTooLarge ( response, serverJson, EVALUATION_PATH, exception );
        }

        // Handle hosted model not ready failures captured as exception.

        catch ( HostedModelNotReadyException exception )
        {
            // Send the appropriate HTTP error response to the client.

            sendProblem (
                response,
                serverJson,
                Status.SERVICE_UNAVAILABLE_503,
                "Evaluation Unavailable",
                "No active model is available for evaluation.",
                EVALUATION_PATH
            );
        }

        // Handle illegal argument failures captured as exception.

        catch ( IllegalArgumentException exception )
        {
            // Apply send problem, is malformed JSON, and safe detail to the server JSON for the evaluate operation.

            sendProblem (
                response,
                serverJson,
                serverJson.isMalformedJson ( exception )
                    ? Status.BAD_REQUEST_400
                    : Status.UNPROCESSABLE_CONTENT_422,
                serverJson.isMalformedJson ( exception ) ? "Malformed JSON" : "Invalid Occurrence",
                safeDetail ( exception, "The occurrence document is invalid." ),
                EVALUATION_PATH
            );
        }

        // Handle runtime failures captured as exception.

        catch ( RuntimeException exception )
        {
            // Send the appropriate HTTP error response to the client.

            sendProblem (
                response,
                serverJson,
                Status.INTERNAL_SERVER_ERROR_500,
                "Evaluation Failed",
                "The occurrence could not be evaluated.",
                EVALUATION_PATH
            );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: stop
    //
    // Description:
    //
    //   Performs the stop operation.
    //
    // Arguments:
    //
    //   request (ServerRequest):
    //     The request to use.
    //
    //   response (ServerResponse):
    //     The response to use.
    //
    //   authenticator (BearerTokenAuthenticator):
    //     The authenticator to use.
    //
    //   application (HostedModelApplication):
    //     The application to use.
    //
    //   serverJson (HttpJsonCodec):
    //     The server json to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void stop (
        ServerRequest request,
        ServerResponse response,
        BearerTokenAuthenticator authenticator,
        HostedModelApplication application,
        HttpJsonCodec serverJson
    )
    {
        // Stop this path and return its result when the operation authenticate does not succeed.

        if ( !authenticate ( request, response, authenticator, serverJson ) )
        {
            return;
        }

        // Perform the status, header, when sent, send, and stopping calls required by the stop operation.

        response.status ( Status.ACCEPTED_202 );
        response.header ( HeaderNames.CONTENT_TYPE, JSON_MEDIA_TYPE );
        response.whenSent ( application::requestShutdown );
        response.send ( serverJson.stopping () );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: authenticate
    //
    // Description:
    //
    //   Performs the authenticate operation.
    //
    // Arguments:
    //
    //   request (ServerRequest):
    //     The request to use.
    //
    //   response (ServerResponse):
    //     The response to use.
    //
    //   authenticator (BearerTokenAuthenticator):
    //     The authenticator to use.
    //
    //   serverJson (HttpJsonCodec):
    //     The server json to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static boolean authenticate (
        ServerRequest request,
        ServerResponse response,
        BearerTokenAuthenticator authenticator,
        HttpJsonCodec serverJson
    )
    {
        // Initialize the authorization values by applying values and headers.

        List <String> authorizationValues = request.headers ().values ( HeaderNames.AUTHORIZATION );
        String suppliedToken              = "";

        // Handle the branch where authorization values size equals 1.

        if ( authorizationValues.size () == 1 )
        {
            // Initialize the authorization value by applying get first.

            String authorizationValue = authorizationValues.getFirst ();

            // Handle the branch where authorization value starts with succeeds.

            if ( authorizationValue.startsWith ( BEARER_PREFIX ) )
            {
                // Update the supplied token from the length result.

                suppliedToken = authorizationValue.substring ( BEARER_PREFIX.length () );
            }
        }

        // Stop this path and return its result when authenticator authenticate succeeds.

        if ( authenticator.authenticate ( suppliedToken ) )
        {
            // Return true for this outcome when authenticator authenticate succeeds.

            return true;
        }

        // Perform the header, send problem, to string, and path calls required by the authenticate operation.

        response.header ( HeaderNames.WWW_AUTHENTICATE, "Bearer realm=\"eca-server\"" );
        sendProblem (
            response,
            serverJson,
            Status.UNAUTHORIZED_401,
            "Unauthorized",
            "A valid bearer token is required.",
            request.path ().toString ()
        );

        // Return false for this outcome.

        return false;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requireJsonMediaType
    //
    // Description:
    //
    //   Performs the require json media type operation.
    //
    // Arguments:
    //
    //   request (ServerRequest):
    //     The request to use.
    //
    //   response (ServerResponse):
    //     The response to use.
    //
    //   serverJson (HttpJsonCodec):
    //     The server json to use.
    //
    //   instance (String):
    //     The instance to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static boolean requireJsonMediaType (
        ServerRequest request,
        ServerResponse response,
        HttpJsonCodec serverJson,
        String instance
    )
    {
        // Prepare the content type and normalized media type values needed by the require JSON media type operation.

        String contentType = request.headers ()
            .first ( HeaderNames.CONTENT_TYPE )
            .orElse ( "" );
        String normalizedMediaType = contentType
            .split ( ";", 2 ) [ 0 ]
            .trim ()
            .toLowerCase ( Locale.ROOT );

        // Stop this path and return its result when normalized media type matches JSON media type.

        if ( normalizedMediaType.equals ( JSON_MEDIA_TYPE ) )
        {
            // Return true for this outcome when normalized media type matches JSON media type.

            return true;
        }

        // Send the appropriate HTTP error response to the client.

        sendProblem (
            response,
            serverJson,
            Status.UNSUPPORTED_MEDIA_TYPE_415,
            "Unsupported Media Type",
            "Content-Type must be application/json.",
            instance
        );

        // Return false for this outcome.

        return false;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readLimited
    //
    // Description:
    //
    //   Performs the read limited operation.
    //
    // Arguments:
    //
    //   request (ServerRequest):
    //     The request to use.
    //
    //   maximumBytes (int):
    //     The maximum bytes to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static byte [] readLimited ( ServerRequest request, int maximumBytes )
    {
        // Initialize the content length by applying content length and headers.

        OptionalLong contentLength = request.headers ().contentLength ();

        // Reject the operation when content length is present and content length as long exceeds maximum bytes.

        if ( contentLength.isPresent () && contentLength.getAsLong () > maximumBytes )
        {
            throw new PayloadTooLargeException ( maximumBytes );
        }

        // Open the scoped resources for the protected operation and close them automatically afterward.

        try (
            InputStream inputStream = request.content ().inputStream ();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream (
                Math.min ( maximumBytes, 8192 )
            )
        )
        {
            byte [] buffer = new byte [ 8192 ];
            int totalBytes = 0;
            int bytesRead;

            // Continue processing while bytes read = input stream read buffer is at least 0.

            while ( ( bytesRead = inputStream.read ( buffer ) ) >= 0 )
            {
                totalBytes += bytesRead;

                // Reject the operation when total bytes exceeds maximum bytes.

                if ( totalBytes > maximumBytes )
                {
                    throw new PayloadTooLargeException ( maximumBytes );
                }

                // Write the prepared data through the output stream.

                outputStream.write ( buffer, 0, bytesRead );
            }

            // Return the result produced by to byte array.

            return outputStream.toByteArray ();
        }

        // Handle I/O failures captured as exception.

        catch ( IOException exception )
        {
            throw new IllegalArgumentException ( "The request body could not be read.", exception );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: isLoopbackRequest
    //
    // Description:
    //
    //   Indicates whether loopback request.
    //
    // Arguments:
    //
    //   request (ServerRequest):
    //     The request to use.
    //
    // Returns:
    //
    //   `true` when the condition is satisfied; otherwise `false`.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static boolean isLoopbackRequest ( ServerRequest request )
    {
        // Initialize the remote address by applying address and remote peer.

        SocketAddress remoteAddress = request.remotePeer ().address ();

        // Stop this path and return its result when remote address is not an inet socket address.

        if ( !( remoteAddress instanceof InetSocketAddress ) )
        {
            // Return false for this outcome when remote address is not an inet socket address.

            return false;
        }

        InetSocketAddress remoteSocketAddress = (InetSocketAddress) remoteAddress;

        // Return whether remote socket address is available and remote socket address is loopback address.

        return remoteSocketAddress.getAddress () != null
            && remoteSocketAddress.getAddress ().isLoopbackAddress ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: resolveBearerToken
    //
    // Description:
    //
    //   Performs the resolve bearer token operation.
    //
    // Arguments:
    //
    //   configuration (ServerConfiguration):
    //     The configuration to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String resolveBearerToken ( ServerConfiguration configuration )
    {
        // Stop this path and return its result when configuration token file is available.

        if ( configuration.getTokenFile () != null )
        {
            // Return the result produced by load existing when configuration token file is available.

            return ControlTokenFile.loadExisting ( configuration.getTokenFile () );
        }

        // Stop this path and return its result when configuration configured bearer token is available.

        if ( configuration.getConfiguredBearerToken () != null )
        {
            // Return the result produced by get configured bearer token when configuration configured bearer token is
            // available.

            return configuration.getConfiguredBearerToken ();
        }

        // Stop this path and create a same-user token when an unconfigured loopback listener uses the default workflow.

        if ( configuration.isLoopbackListener () )
        {
            // Return the result produced by load or create for the default loopback workflow.

            return ControlTokenFile.loadOrCreate ( configuration.resolveControlTokenFile () );
        }

        throw new IllegalArgumentException (
            "A non-loopback listener requires --token-file or ECA_SERVER_TOKEN."
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: entityTag
    //
    // Description:
    //
    //   Performs the entity tag operation.
    //
    // Arguments:
    //
    //   revision (String):
    //     The revision to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String entityTag ( String revision )
    {
        // Return the composed entity tag value.

        return "\"" + revision + "\"";
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: safeDetail
    //
    // Description:
    //
    //   Performs the safe detail operation.
    //
    // Arguments:
    //
    //   exception (RuntimeException):
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

    private static String safeDetail ( RuntimeException exception, String fallback )
    {
        // Initialize the message by applying get message.

        String message = exception.getMessage ();

        // Return the value selected according to message is unavailable or message is blank.

        return message == null || message.isBlank () ? fallback : message;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: sendPayloadTooLarge
    //
    // Description:
    //
    //   Performs the send payload too large operation.
    //
    // Arguments:
    //
    //   response (ServerResponse):
    //     The response to use.
    //
    //   serverJson (HttpJsonCodec):
    //     The server json to use.
    //
    //   instance (String):
    //     The instance to use.
    //
    //   exception (PayloadTooLargeException):
    //     The exception that caused the operation to fail.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void sendPayloadTooLarge (
        ServerResponse response,
        HttpJsonCodec serverJson,
        String instance,
        PayloadTooLargeException exception
    )
    {
        // Apply send problem and get maximum bytes to the exception for the send payload too large operation.

        sendProblem (
            response,
            serverJson,
            Status.REQUEST_ENTITY_TOO_LARGE_413,
            "Content Too Large",
            "The request body exceeds the configured "
                + exception.getMaximumBytes ()
                + "-byte limit.",
            instance
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: sendJson
    //
    // Description:
    //
    //   Performs the send json operation.
    //
    // Arguments:
    //
    //   response (ServerResponse):
    //     The response to use.
    //
    //   status (Status):
    //     The status to use.
    //
    //   document (String):
    //     The document to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void sendJson ( ServerResponse response, Status status, String document )
    {
        // Apply status, header, and send to the response for the send JSON operation.

        response.status ( status );
        response.header ( HeaderNames.CONTENT_TYPE, JSON_MEDIA_TYPE );
        response.send ( document );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: sendProblem
    //
    // Description:
    //
    //   Performs the send problem operation.
    //
    // Arguments:
    //
    //   response (ServerResponse):
    //     The response to use.
    //
    //   serverJson (HttpJsonCodec):
    //     The server json to use.
    //
    //   status (Status):
    //     The status to use.
    //
    //   title (String):
    //     The title to use.
    //
    //   detail (String):
    //     The detail to use.
    //
    //   instance (String):
    //     The instance to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void sendProblem (
        ServerResponse response,
        HttpJsonCodec serverJson,
        Status status,
        String title,
        String detail,
        String instance
    )
    {
        // Perform the status, header, send, problem, and code calls required by the send problem operation.

        response.status ( status );
        response.header ( HeaderNames.CONTENT_TYPE, PROBLEM_JSON_MEDIA_TYPE );
        response.send (
            serverJson.problem (
                status.code (),
                title,
                detail,
                instance
            )
        );
    }

    //*****************************************************************************************************************
    // Class: PayloadTooLargeException
    //
    // Description:
    //
    //   Provides the payload too large exception behavior.
    //
    //*****************************************************************************************************************

    private static final class PayloadTooLargeException extends IllegalArgumentException
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final int maximumBytes;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getMaximumBytes
        //
        // Description:
        //
        //   Returns the maximum bytes.
        //
        // Returns:
        //
        //   The maximum bytes.
        //
        //-------------------------------------------------------------------------------------------------------------

        int getMaximumBytes ()
        {
            // Return the maximum bytes to the caller.

            return this.maximumBytes;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: PayloadTooLargeException
        //
        // Description:
        //
        //   Creates the PayloadTooLargeException instance from the supplied values.
        //
        // Arguments:
        //
        //   maximumBytes (int):
        //     The maximum bytes to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        PayloadTooLargeException ( int maximumBytes )
        {
            // Initialize the inherited state through the base-class constructor.

            super ( "The request body exceeds its configured limit." );

            this.maximumBytes = maximumBytes;
        }
    }
}
