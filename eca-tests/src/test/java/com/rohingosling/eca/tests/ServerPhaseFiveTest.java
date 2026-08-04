//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies the HTTP, authentication, limit, lifecycle, and command-line acceptance contract.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohingosling.eca.json.AuthoringModelJsonCodec;
import com.rohingosling.eca.server.ControlTokenFile;
import com.rohingosling.eca.server.EcaHttpServer;
import com.rohingosling.eca.server.ServerConfiguration;
import com.rohingosling.eca.server.ServerMain;

//*********************************************************************************************************************
// Class: ServerPhaseFiveTest
//
// Description:
//
//   Verifies the HTTP, authentication, limit, lifecycle, and command-line acceptance contract.
//
//*********************************************************************************************************************

final class ServerPhaseFiveTest
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    private static final String JSON_MEDIA_TYPE = "application/json";

    private final HttpClient httpClient = HttpClient.newBuilder ()
        .connectTimeout ( Duration.ofSeconds ( 5 ) )
        .build ();

    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final ObjectMapper objectMapper = new ObjectMapper ();

    @TempDir
    private Path temporaryDirectory;

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: healthWithoutModel_isPublicAndNonSensitive
    //
    // Description:
    //
    //   Verifies that health without model is public and non sensitive.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void healthWithoutModel_isPublicAndNonSensitive () throws Exception
    {
        // Open the scoped resources for the protected operation and close them automatically afterward.

        try ( EcaHttpServer server = this.startServer ( 64 * 1024, 1024 ) )
        {
            // Prepare the liveness and readiness values needed by the health without model is public and non sensitive
            // operation.

            HttpResponse <String> liveness = this.send (
                server,
                "GET",
                EcaHttpServer.LIVENESS_PATH,
                null,
                null,
                null
            );
            HttpResponse <String> readiness = this.send (
                server,
                "GET",
                EcaHttpServer.READINESS_PATH,
                null,
                null,
                null
            );

            // Verify the health without model is public and non sensitive scenario against its expected outcome.

            assertThat ( liveness.statusCode () ).isEqualTo ( 200 );
            assertThat ( liveness.body () ).contains ( "\"status\":\"UP\"" );
            assertThat ( readiness.statusCode () ).isEqualTo ( 503 );
            assertThat ( readiness.body () )
                .contains ( "\"ready\":false" )
                .doesNotContain ( this.temporaryDirectory.toString (), "token", "payload" );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: modelEvaluationPullAndStop_completeLifecycleWorkflow
    //
    // Description:
    //
    //   Verifies the complete model-installation, evaluation, retrieval, and graceful-stop lifecycle.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void modelEvaluationPullAndStop_completeLifecycleWorkflow () throws Exception
    {
        // Initialize the model document by applying read all bytes, resolve, and project root.

        byte [] modelDocument = Files.readAllBytes ( projectRoot ().resolve ( "examples/eca-rule-engine-example.json" ) );

        // Open the scoped resources for the protected operation and close them automatically afterward.

        try ( EcaHttpServer server = this.startServer ( 1024 * 1024, 4096 ) )
        {
            // Prepare the token, installation, and revision values needed by the model evaluation pull and stop
            // complete established workflow operation.

            String token = this.token ();
            HttpResponse <String> installation = this.send (
                server,
                "PUT",
                EcaHttpServer.MODEL_PATH,
                modelDocument,
                JSON_MEDIA_TYPE,
                token
            );
            String revision = this.objectMapper.readTree ( installation.body () )
                .path ( "modelRevision" )
                .asText ();

            // Verify the model evaluation pull and stop complete established workflow scenario against its expected
            // outcome.

            assertThat ( installation.statusCode () ).isEqualTo ( 201 );
            assertThat ( installation.headers ().firstValue ( "ETag" ) )
                .contains ( "\"" + revision + "\"" );

            // Prepare the action, no action, and pull values needed by the model evaluation pull and stop complete
            // established workflow operation.

            HttpResponse <String> action = this.send (
                server,
                "POST",
                EcaHttpServer.EVALUATION_PATH,
                actionOccurrence ().getBytes ( StandardCharsets.UTF_8 ),
                JSON_MEDIA_TYPE,
                null
            );
            HttpResponse <String> noAction = this.send (
                server,
                "POST",
                EcaHttpServer.EVALUATION_PATH,
                "{\"eventId\":\"event-cancel-order\",\"payload\":{}}".getBytes ( StandardCharsets.UTF_8 ),
                JSON_MEDIA_TYPE,
                null
            );
            HttpResponse <String> pull = this.send (
                server,
                "GET",
                EcaHttpServer.MODEL_PATH,
                null,
                null,
                null
            );

            // Verify the model evaluation pull and stop complete established workflow scenario against its expected
            // outcome.

            assertThat ( action.statusCode () ).isEqualTo ( 200 );
            assertThat ( action.body () )
                .contains (
                    "\"outcome\":\"ACTION\"",
                    "\"actionId\":\"action-local-courier\"",
                    "\"modelRevision\":\"" + revision + "\""
                );
            assertThat ( noAction.statusCode () ).isEqualTo ( 200 );
            assertThat ( noAction.body () )
                .contains ( "\"outcome\":\"NO_ACTION\"" )
                .doesNotContain ( "actionId", "ruleId", "specificity" );
            assertThat ( pull.statusCode () ).isEqualTo ( 200 );
            assertThat ( pull.headers ().firstValue ( "ETag" ) )
                .contains ( "\"" + revision + "\"" );

            // Initialize the model codec with a new authoring model JSON codec.

            AuthoringModelJsonCodec modelCodec = new AuthoringModelJsonCodec ();

            // Verify the model evaluation pull and stop complete established workflow scenario against its expected
            // outcome.

            assertThat ( pull.body ().getBytes ( StandardCharsets.UTF_8 ) )
                .isEqualTo ( modelCodec.writeCanonical ( modelCodec.read ( modelDocument ) ) );

            // Prepare the termination and stop values needed by the model evaluation pull and stop complete
            // established workflow operation.

            CompletableFuture <Void> termination = CompletableFuture.runAsync (
                () ->
                {
                    try
                    {
                        // Wait for the executor tasks to finish within the allotted timeout.

                        server.awaitTermination ();
                    }

                    // Handle interrupted failures captured as exception.

                    catch ( InterruptedException exception )
                    {
                        // Perform the interrupt and current thread calls required by the model evaluation pull and
                        // stop complete established workflow operation.

                        Thread.currentThread ().interrupt ();
                        throw new IllegalStateException ( exception );
                    }
                }
            );
            HttpResponse <String> stop = this.send (
                server,
                "POST",
                EcaHttpServer.STOP_PATH,
                null,
                null,
                token
            );

            // Verify the model evaluation pull and stop complete established workflow scenario against its expected
            // outcome.

            assertThat ( stop.statusCode () ).isEqualTo ( 202 );
            assertThat ( stop.body () ).contains ( "\"status\":\"STOPPING\"" );
            termination.get ( 5, TimeUnit.SECONDS );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: protectedRoutes_requireTheDiscoveredControlToken
    //
    // Description:
    //
    //   Verifies that protected routes require the discovered control token.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void protectedRoutes_requireTheDiscoveredControlToken () throws Exception
    {
        // Initialize the model document by applying read all bytes, resolve, and project root.

        byte [] modelDocument = Files.readAllBytes ( projectRoot ().resolve ( "examples/eca-rule-engine-example.json" ) );

        // Open the scoped resources for the protected operation and close them automatically afterward.

        try ( EcaHttpServer server = this.startServer ( 1024 * 1024, 4096 ) )
        {
            // Prepare the missing, invalid, and valid values needed by the protected routes require the discovered
            // control token operation.

            HttpResponse <String> missing = this.send (
                server,
                "PUT",
                EcaHttpServer.MODEL_PATH,
                modelDocument,
                JSON_MEDIA_TYPE,
                null
            );
            HttpResponse <String> invalid = this.send (
                server,
                "PUT",
                EcaHttpServer.MODEL_PATH,
                modelDocument,
                JSON_MEDIA_TYPE,
                "invalid-token-value-that-is-long-enough"
            );
            HttpResponse <String> valid = this.send (
                server,
                "PUT",
                EcaHttpServer.MODEL_PATH,
                modelDocument,
                JSON_MEDIA_TYPE,
                this.token ()
            );

            // Verify the protected routes require the discovered control token scenario against its expected outcome.

            assertThat ( missing.statusCode () ).isEqualTo ( 401 );
            assertThat ( invalid.statusCode () ).isEqualTo ( 401 );
            assertThat ( missing.headers ().firstValue ( "WWW-Authenticate" ) ).isPresent ();
            assertThat ( valid.statusCode () ).isEqualTo ( 201 );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: rejectedReplacement_retainsThePriorRevision
    //
    // Description:
    //
    //   Verifies that rejected replacement retains the prior revision.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void rejectedReplacement_retainsThePriorRevision () throws Exception
    {
        // Initialize the model document by applying read all bytes, resolve, and project root.

        byte [] modelDocument = Files.readAllBytes ( projectRoot ().resolve ( "examples/eca-rule-engine-example.json" ) );

        // Open the scoped resources for the protected operation and close them automatically afterward.

        try ( EcaHttpServer server = this.startServer ( 1024 * 1024, 4096 ) )
        {
            // Prepare the token, accepted, accepted entity tag, rejected, and pulled values needed by the rejected
            // replacement retains the prior revision operation.

            String token = this.token ();
            HttpResponse <String> accepted = this.send (
                server,
                "PUT",
                EcaHttpServer.MODEL_PATH,
                modelDocument,
                JSON_MEDIA_TYPE,
                token
            );
            String acceptedEntityTag = accepted.headers ().firstValue ( "ETag" ).orElseThrow ();
            HttpResponse <String> rejected = this.send (
                server,
                "PUT",
                EcaHttpServer.MODEL_PATH,
                "{\"schemaVersion\":".getBytes ( StandardCharsets.UTF_8 ),
                JSON_MEDIA_TYPE,
                token
            );
            HttpResponse <String> pulled = this.send (
                server,
                "GET",
                EcaHttpServer.MODEL_PATH,
                null,
                null,
                null
            );

            // Verify the rejected replacement retains the prior revision scenario against its expected outcome.

            assertThat ( rejected.statusCode () ).isEqualTo ( 400 );
            assertThat ( rejected.headers ().firstValue ( "Content-Type" ).orElse ( "" ) )
                .startsWith ( "application/problem+json" );
            assertThat ( pulled.headers ().firstValue ( "ETag" ) ).contains ( acceptedEntityTag );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: protocolErrors_areDistinctAndUseProblemDetails
    //
    // Description:
    //
    //   Verifies that protocol errors are distinct and use problem details.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void protocolErrors_areDistinctAndUseProblemDetails () throws Exception
    {
        // Open the scoped resources for the protected operation and close them automatically afterward.

        try ( EcaHttpServer server = this.startServer ( 64 * 1024, 64 ) )
        {
            // Prepare the missing model, unsupported media type, invalid occurrence, oversized, and not found values
            // needed by the protocol errors are distinct and use problem details operation.

            HttpResponse <String> missingModel = this.send (
                server,
                "GET",
                EcaHttpServer.MODEL_PATH,
                null,
                null,
                null
            );
            HttpResponse <String> unsupportedMediaType = this.send (
                server,
                "POST",
                EcaHttpServer.EVALUATION_PATH,
                "{}".getBytes ( StandardCharsets.UTF_8 ),
                "text/plain",
                null
            );
            HttpResponse <String> invalidOccurrence = this.send (
                server,
                "POST",
                EcaHttpServer.EVALUATION_PATH,
                "{\"eventId\":7}".getBytes ( StandardCharsets.UTF_8 ),
                JSON_MEDIA_TYPE,
                null
            );
            HttpResponse <String> oversized = this.send (
                server,
                "POST",
                EcaHttpServer.EVALUATION_PATH,
                ( "{\"eventId\":\"" + "x".repeat ( 100 ) + "\"}" ).getBytes ( StandardCharsets.UTF_8 ),
                JSON_MEDIA_TYPE,
                null
            );
            HttpResponse <String> notFound = this.send (
                server,
                "GET",
                "/api/v1/unknown",
                null,
                null,
                null
            );

            // Verify the protocol errors are distinct and use problem details scenario against its expected outcome.

            assertProblem ( missingModel, 404 );
            assertProblem ( unsupportedMediaType, 415 );
            assertProblem ( invalidOccurrence, 422 );
            assertProblem ( oversized, 413 );
            assertProblem ( notFound, 404 );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: nonLoopbackStartupWithoutCredentials_isRefused
    //
    // Description:
    //
    //   Verifies that non loopback startup without credentials is refused.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void nonLoopbackStartupWithoutCredentials_isRefused ()
    {
        // Initialize the configuration by applying of seconds.

        ServerConfiguration configuration = new ServerConfiguration (
            "0.0.0.0",
            0,
            this.temporaryDirectory,
            null,
            null,
            4096,
            1024,
            4096,
            Duration.ofSeconds ( 5 ),
            Duration.ofSeconds ( 5 )
        );

        // Verify the non loopback startup without credentials is refused scenario against its expected outcome.

        assertThatThrownBy ( () -> EcaHttpServer.start ( configuration ) )
            .isInstanceOf ( IllegalArgumentException.class )
            .hasMessageContaining ( "non-loopback" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: commandLine_helpVersionUsageAndValidationUseStableExitCodes
    //
    // Description:
    //
    //   Verifies that command line help version usage and validation use stable exit codes.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void commandLine_helpVersionUsageAndValidationUseStableExitCodes () throws Exception
    {
        // Initialize the malformed model by applying resolve.

        Path malformedModel = this.temporaryDirectory.resolve ( "malformed.json" );

        // Verify the command line help version usage and validation use stable exit codes scenario against its
        // expected outcome.

        Files.writeString ( malformedModel, "{", StandardCharsets.UTF_8 );

        assertThat ( ServerMain.execute ( "--help" ) ).isZero ();
        assertThat ( ServerMain.execute ( "--version" ) ).isZero ();
        assertThat ( ServerMain.execute ( "model" ) ).isEqualTo ( 2 );
        assertThat ( ServerMain.execute ( "model", malformedModel.toString () ) ).isEqualTo ( 3 );
        assertThat (
            ServerMain.execute (
                "start",
                "--model",
                malformedModel.toString (),
                "--data-directory",
                this.temporaryDirectory.toString (),
                "--port",
                "0"
            )
        ).isEqualTo ( 3 );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: startServer
    //
    // Description:
    //
    //   Performs the start server operation.
    //
    // Arguments:
    //
    //   modelBodyLimit (int):
    //     The model body limit to use.
    //
    //   evaluationBodyLimit (int):
    //     The evaluation body limit to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private EcaHttpServer startServer ( int modelBodyLimit, int evaluationBodyLimit )
    {
        // Return the result produced by start.

        return EcaHttpServer.start (
            new ServerConfiguration (
                "127.0.0.1",
                0,
                this.temporaryDirectory,
                null,
                null,
                modelBodyLimit,
                evaluationBodyLimit,
                32 * 1024,
                Duration.ofSeconds ( 5 ),
                Duration.ofSeconds ( 5 )
            )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: token
    //
    // Description:
    //
    //   Performs the token operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private String token ()
    {
        // Return the result produced by load existing.

        return ControlTokenFile.loadExisting ( this.temporaryDirectory.resolve ( "control-token" ) );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: send
    //
    // Description:
    //
    //   Performs the send operation.
    //
    // Arguments:
    //
    //   server (EcaHttpServer):
    //     The server to use.
    //
    //   method (String):
    //     The method to use.
    //
    //   path (String):
    //     The path to use.
    //
    //   body (byte []):
    //     The body to use.
    //
    //   contentType (String):
    //     The content type to use.
    //
    //   token (String):
    //     The token to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private HttpResponse <String> send (
        EcaHttpServer server,
        String method,
        String path,
        byte [] body,
        String contentType,
        String token
    ) throws IOException, InterruptedException
    {
        // Initialize the request builder by applying timeout, new builder, create, get port, and of seconds.

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder (
            URI.create ( "http://127.0.0.1:" + server.getPort () + path )
        ).timeout ( Duration.ofSeconds ( 5 ) );

        // Handle the branch where content type is available.

        if ( contentType != null )
        {
            // Attach the required HTTP header to the request.

            requestBuilder.header ( "Content-Type", contentType );
        }

        // Handle the branch where token is available.

        if ( token != null )
        {
            // Attach the required HTTP header to the request.

            requestBuilder.header ( "Authorization", "Bearer " + token );
        }

        // Perform the method, no body, and of byte array calls required by the send operation.

        requestBuilder.method (
            method,
            body == null
                ? HttpRequest.BodyPublishers.noBody ()
                : HttpRequest.BodyPublishers.ofByteArray ( body )
        );

        // Return the result produced by send.

        return this.httpClient.send ( requestBuilder.build (), HttpResponse.BodyHandlers.ofString () );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: assertProblem
    //
    // Description:
    //
    //   Performs the assert problem operation.
    //
    // Arguments:
    //
    //   response (HttpResponse <String>):
    //     The response to use.
    //
    //   status (int):
    //     The status to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void assertProblem ( HttpResponse <String> response, int status )
    {
        // Verify the assert problem scenario against its expected outcome.

        assertThat ( response.statusCode () ).isEqualTo ( status );
        assertThat ( response.headers ().firstValue ( "Content-Type" ).orElse ( "" ) )
            .startsWith ( "application/problem+json" );
        assertThat ( response.body () ).contains ( "\"status\":" + status, "\"type\":\"about:blank\"" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: actionOccurrence
    //
    // Description:
    //
    //   Performs the action occurrence operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String actionOccurrence ()
    {
        // Return the action occurrence text to the caller.

        return """
            {
                "eventId": "event-order-product",
                "payload": {
                    "parameter-quantity": 1,
                    "parameter-delivery-type": "STANDARD",
                    "parameter-product-category": "RETAIL",
                    "parameter-region": "LOCAL",
                    "parameter-vip": false
                }
            }
            """;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: projectRoot
    //
    // Description:
    //
    //   Performs the project root operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Path projectRoot ()
    {
        // Return the result produced by of.

        return Path.of ( System.getProperty ( "eca.project.root" ) );
    }
}
