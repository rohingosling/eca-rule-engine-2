//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies exact-origin browser access, preflight policy, revision exposure, and retained authentication.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.rohingosling.eca.server.ControlTokenFile;
import com.rohingosling.eca.server.EcaHttpServer;
import com.rohingosling.eca.server.ServerConfiguration;
import com.rohingosling.eca.server.ServerMain;

//*********************************************************************************************************************
// Class: ServerPhaseNineteenTest
//
// Description:
//
//   Verifies exact-origin browser access, preflight policy, revision exposure, and retained authentication.
//
//*********************************************************************************************************************

final class ServerPhaseNineteenTest
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    private static final String ALLOWED_ORIGIN = "https://rohingosling.github.io";
    private static final String DENIED_ORIGIN  = "https://attacker.example";
    private static final String JSON_MEDIA_TYPE = "application/json";

    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final HttpClient httpClient = HttpClient.newBuilder ()
        .connectTimeout ( Duration.ofSeconds ( 5 ) )
        .build ();

    @TempDir
    private Path temporaryDirectory;

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: allowedOrigins_areExactNormalizedAndSecureByDefault
    //
    // Description:
    //
    //   Verifies immutable normalized origins, the empty default, and rejection of unsafe origin forms.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void allowedOrigins_areExactNormalizedAndSecureByDefault ()
    {
        // Prepare default and explicitly configured server configurations.

        ServerConfiguration defaultConfiguration = this.configuration ( List.of () );
        ServerConfiguration configured = this.configuration (
            List.of (
                "HTTPS://ROHINGOSLING.GITHUB.IO",
                ALLOWED_ORIGIN,
                "http://127.0.0.1:4173"
            )
        );

        // Verify the default, normalization, duplicate removal, and immutable collection behavior.

        assertThat ( defaultConfiguration.getAllowedOrigins () ).isEmpty ();
        assertThat ( configured.getAllowedOrigins () )
            .containsExactly ( ALLOWED_ORIGIN, "http://127.0.0.1:4173" );
        assertThatThrownBy ( () -> configured.getAllowedOrigins ().add ( "https://example.com" ) )
            .isInstanceOf ( UnsupportedOperationException.class );

        // Reject wildcard, opaque, malformed, credential-bearing, path-bearing, query-bearing, and fragment origins.

        List <String> invalidOrigins = List.of (
            "*",
            "null",
            "file:///tmp/model.json",
            "https://rohingosling.github.io/",
            "https://rohingosling.github.io/project",
            "https://rohingosling.github.io?query=value",
            "https://rohingosling.github.io#fragment",
            "https://user@rohingosling.github.io",
            "https://rohingosling.github.io:99999",
            "https://",
            "rohingosling.github.io"
        );

        for ( String invalidOrigin : invalidOrigins )
        {
            assertThatThrownBy ( () -> this.configuration ( List.of ( invalidOrigin ) ) )
                .isInstanceOf ( IllegalArgumentException.class )
                .hasMessageContaining ( "exact HTTP or HTTPS origin" );
        }

        // Verify that the repeatable start option routes invalid entries through the stable configuration exit code.

        assertThat (
            ServerMain.execute (
                "start",
                "--allowed-origin",
                ALLOWED_ORIGIN,
                "--allowed-origin",
                "*",
                "--data-directory",
                this.temporaryDirectory.toString (),
                "--port",
                "0"
            )
        ).isEqualTo ( 4 );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: cors_isAbsentByDefault
    //
    // Description:
    //
    //   Verifies that actual and preflight responses remain cross-origin unreadable without explicit configuration.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void cors_isAbsentByDefault () throws Exception
    {
        // Open a default server with the browser allowlist intentionally empty.

        try ( EcaHttpServer server = EcaHttpServer.start ( this.configuration ( List.of () ) ) )
        {
            HttpResponse <String> actualResponse = this.send (
                server,
                "GET",
                EcaHttpServer.LIVENESS_PATH,
                null,
                Map.of ( "Origin", ALLOWED_ORIGIN )
            );
            HttpResponse <String> preflightResponse = this.preflight (
                server,
                ALLOWED_ORIGIN,
                EcaHttpServer.LIVENESS_PATH,
                "GET",
                "authorization",
                false
            );

            // Verify no response opts the origin into browser readability.

            assertThat ( actualResponse.statusCode () ).isEqualTo ( 200 );
            assertNoBrowserAccessHeaders ( actualResponse );
            assertThat ( preflightResponse.statusCode () ).isEqualTo ( 403 );
            assertNoBrowserAccessHeaders ( preflightResponse );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: preflight_allowsOnlyConfiguredRoutesMethodsAndHeaders
    //
    // Description:
    //
    //   Verifies successful preflights for every route and fail-closed handling for every unconfigured dimension.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void preflight_allowsOnlyConfiguredRoutesMethodsAndHeaders () throws Exception
    {
        // Prepare every supported route and method pair.

        String [][] routeMethods =
        {
            { EcaHttpServer.LIVENESS_PATH, "GET" },
            { EcaHttpServer.READINESS_PATH, "GET" },
            { EcaHttpServer.MODEL_PATH, "GET" },
            { EcaHttpServer.MODEL_PATH, "PUT" },
            { EcaHttpServer.EVALUATION_PATH, "POST" },
            { EcaHttpServer.STOP_PATH, "POST" },
        };

        // Open a server that allows exactly the GitHub Pages origin.

        try ( EcaHttpServer server = EcaHttpServer.start ( this.configuration ( List.of ( ALLOWED_ORIGIN ) ) ) )
        {
            // Verify every supported route without presenting a bearer token to the protected route handlers.

            for ( String [] routeMethod : routeMethods )
            {
                HttpResponse <String> response = this.preflight (
                    server,
                    ALLOWED_ORIGIN,
                    routeMethod [ 0 ],
                    routeMethod [ 1 ],
                    "authorization, content-type",
                    false
                );

                assertThat ( response.statusCode () ).isEqualTo ( 204 );
                assertThat ( response.headers ().firstValue ( "Access-Control-Allow-Origin" ) )
                    .contains ( ALLOWED_ORIGIN );
                assertThat ( response.headers ().firstValue ( "Access-Control-Allow-Methods" ) )
                    .contains ( routeMethod [ 1 ] );
                assertThat ( response.headers ().firstValue ( "Access-Control-Allow-Headers" ) )
                    .contains ( "Authorization, Content-Type" );
                assertThat ( response.headers ().firstValue ( "Vary" ) ).contains ( "Origin" );
                assertThat ( response.headers ().firstValue ( "Access-Control-Allow-Private-Network" ) )
                    .isEmpty ();
            }

            // Verify the transitional private-network response only when one valid allowed preflight requests it.

            HttpResponse <String> privateNetworkResponse = this.preflight (
                server,
                ALLOWED_ORIGIN,
                EcaHttpServer.MODEL_PATH,
                "PUT",
                "authorization, content-type",
                true
            );

            assertThat ( privateNetworkResponse.statusCode () ).isEqualTo ( 204 );
            assertThat ( privateNetworkResponse.headers ().firstValue ( "Access-Control-Allow-Private-Network" ) )
                .contains ( "true" );

            // Verify denied origin, route, method, header, and opaque-origin preflights remain unreadable.

            List <HttpResponse <String>> deniedResponses = List.of (
                this.preflight (
                    server,
                    DENIED_ORIGIN,
                    EcaHttpServer.MODEL_PATH,
                    "PUT",
                    "authorization, content-type",
                    true
                ),
                this.preflight ( server, ALLOWED_ORIGIN, "/api/v1/unknown", "GET", "authorization", false ),
                this.preflight (
                    server,
                    ALLOWED_ORIGIN,
                    EcaHttpServer.MODEL_PATH,
                    "DELETE",
                    "authorization",
                    false
                ),
                this.preflight (
                    server,
                    ALLOWED_ORIGIN,
                    EcaHttpServer.MODEL_PATH,
                    "PUT",
                    "authorization, x-secret",
                    false
                ),
                this.preflight ( server, "null", EcaHttpServer.LIVENESS_PATH, "GET", "authorization", false )
            );

            for ( HttpResponse <String> deniedResponse : deniedResponses )
            {
                assertThat ( deniedResponse.statusCode () ).isEqualTo ( 403 );
                assertNoBrowserAccessHeaders ( deniedResponse );
            }
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: actualResponses_exposeRevisionsWithoutWeakeningAuthentication
    //
    // Description:
    //
    //   Verifies allowed response exposure, denied-origin opacity, and retained bearer protection for replacement.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void actualResponses_exposeRevisionsWithoutWeakeningAuthentication () throws Exception
    {
        // Read the curated model used by both Java and browser acceptance workflows.

        byte [] modelDocument = Files.readAllBytes (
            projectRoot ().resolve ( "examples/eca-rule-engine-example.json" )
        );

        // Open a server that allows exactly the GitHub Pages origin.

        try ( EcaHttpServer server = EcaHttpServer.start ( this.configuration ( List.of ( ALLOWED_ORIGIN ) ) ) )
        {
            String token = ControlTokenFile.loadExisting ( this.temporaryDirectory.resolve ( "control-token" ) );

            // Exercise protected replacement with missing, invalid, and valid bearer credentials.

            HttpResponse <String> missingToken = this.send (
                server,
                "PUT",
                EcaHttpServer.MODEL_PATH,
                modelDocument,
                Map.of ( "Content-Type", JSON_MEDIA_TYPE, "Origin", ALLOWED_ORIGIN )
            );
            HttpResponse <String> invalidToken = this.send (
                server,
                "PUT",
                EcaHttpServer.MODEL_PATH,
                modelDocument,
                Map.of (
                    "Authorization",
                    "Bearer invalid-token",
                    "Content-Type",
                    JSON_MEDIA_TYPE,
                    "Origin",
                    ALLOWED_ORIGIN
                )
            );
            HttpResponse <String> validReplacement = this.send (
                server,
                "PUT",
                EcaHttpServer.MODEL_PATH,
                modelDocument,
                Map.of (
                    "Authorization",
                    "Bearer " + token,
                    "Content-Type",
                    JSON_MEDIA_TYPE,
                    "Origin",
                    ALLOWED_ORIGIN
                )
            );

            assertThat ( missingToken.statusCode () ).isEqualTo ( 401 );
            assertAllowedActualResponse ( missingToken );
            assertThat ( invalidToken.statusCode () ).isEqualTo ( 401 );
            assertAllowedActualResponse ( invalidToken );
            assertThat ( validReplacement.statusCode () ).isEqualTo ( 201 );
            assertAllowedActualResponse ( validReplacement );
            assertThat ( validReplacement.headers ().firstValue ( "ETag" ) ).isPresent ();
            assertThat ( validReplacement.headers ().firstValue ( "X-Model-Revision" ) ).isPresent ();

            // Verify allowed Pull exposes revision headers while a denied origin receives no readable CORS response.

            HttpResponse <String> allowedPull = this.send (
                server,
                "GET",
                EcaHttpServer.MODEL_PATH,
                null,
                Map.of ( "Origin", ALLOWED_ORIGIN )
            );
            HttpResponse <String> deniedPull = this.send (
                server,
                "GET",
                EcaHttpServer.MODEL_PATH,
                null,
                Map.of ( "Origin", DENIED_ORIGIN )
            );

            assertThat ( allowedPull.statusCode () ).isEqualTo ( 200 );
            assertAllowedActualResponse ( allowedPull );
            assertThat ( allowedPull.headers ().firstValue ( "ETag" ) ).isPresent ();
            assertThat ( allowedPull.headers ().firstValue ( "X-Model-Revision" ) ).isPresent ();
            assertThat ( deniedPull.statusCode () ).isEqualTo ( 200 );
            assertNoBrowserAccessHeaders ( deniedPull );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: configuration
    //
    // Description:
    //
    //   Creates one loopback server configuration with the supplied browser-origin collection.
    //
    // Arguments:
    //
    //   allowedOrigins (List <String>):
    //     The exact browser origins to allow.
    //
    // Returns:
    //
    //   The prepared server configuration.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private ServerConfiguration configuration ( List <String> allowedOrigins )
    {
        // Return a standard bounded loopback test configuration.

        return new ServerConfiguration (
            "127.0.0.1",
            0,
            this.temporaryDirectory,
            null,
            null,
            allowedOrigins,
            1024 * 1024,
            1024 * 1024,
            32 * 1024,
            Duration.ofSeconds ( 5 ),
            Duration.ofSeconds ( 5 )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: preflight
    //
    // Description:
    //
    //   Sends one browser preflight request.
    //
    // Arguments:
    //
    //   server (EcaHttpServer):
    //     The running server.
    //
    //   origin (String):
    //     The browser origin.
    //
    //   path (String):
    //     The API path.
    //
    //   method (String):
    //     The requested method.
    //
    //   requestedHeaders (String):
    //     The requested header list.
    //
    //   privateNetwork (boolean):
    //     Whether to request transitional private-network access.
    //
    // Returns:
    //
    //   The received HTTP response.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private HttpResponse <String> preflight (
        EcaHttpServer server,
        String origin,
        String path,
        String method,
        String requestedHeaders,
        boolean privateNetwork
    ) throws Exception
    {
        // Prepare the standard preflight headers.

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder (
            URI.create ( "http://127.0.0.1:" + server.getPort () + path )
        )
            .timeout ( Duration.ofSeconds ( 5 ) )
            .header ( "Origin", origin )
            .header ( "Access-Control-Request-Method", method );

        if ( requestedHeaders != null )
        {
            requestBuilder.header ( "Access-Control-Request-Headers", requestedHeaders );
        }

        if ( privateNetwork )
        {
            requestBuilder.header ( "Access-Control-Request-Private-Network", "true" );
        }

        // Send the preflight without any bearer credential.

        return this.httpClient.send (
            requestBuilder.method ( "OPTIONS", HttpRequest.BodyPublishers.noBody () ).build (),
            HttpResponse.BodyHandlers.ofString ()
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: send
    //
    // Description:
    //
    //   Sends one HTTP request with explicitly supplied headers.
    //
    // Arguments:
    //
    //   server (EcaHttpServer):
    //     The running server.
    //
    //   method (String):
    //     The HTTP method.
    //
    //   path (String):
    //     The API path.
    //
    //   body (byte []):
    //     The optional request body.
    //
    //   headers (Map <String, String>):
    //     The request headers.
    //
    // Returns:
    //
    //   The received HTTP response.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private HttpResponse <String> send (
        EcaHttpServer server,
        String method,
        String path,
        byte [] body,
        Map <String, String> headers
    ) throws Exception
    {
        // Prepare the request and copy every explicitly supplied header.

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder (
            URI.create ( "http://127.0.0.1:" + server.getPort () + path )
        ).timeout ( Duration.ofSeconds ( 5 ) );

        headers.forEach ( requestBuilder::header );

        // Send the request with the selected body publisher.

        return this.httpClient.send (
            requestBuilder.method (
                method,
                body == null
                    ? HttpRequest.BodyPublishers.noBody ()
                    : HttpRequest.BodyPublishers.ofByteArray ( body )
            ).build (),
            HttpResponse.BodyHandlers.ofString ( StandardCharsets.UTF_8 )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: assertAllowedActualResponse
    //
    // Description:
    //
    //   Verifies the shared exact-origin actual-response headers.
    //
    // Arguments:
    //
    //   response (HttpResponse <?>):
    //     The response to inspect.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void assertAllowedActualResponse ( HttpResponse <?> response )
    {
        // Verify exact origin, revision exposure, and cache variance without wildcard credentials.

        assertThat ( response.headers ().firstValue ( "Access-Control-Allow-Origin" ) )
            .contains ( ALLOWED_ORIGIN );
        assertThat ( response.headers ().firstValue ( "Access-Control-Expose-Headers" ) )
            .contains ( "ETag, X-Model-Revision" );
        assertThat ( response.headers ().firstValue ( "Vary" ) ).contains ( "Origin" );
        assertThat ( response.headers ().firstValue ( "Access-Control-Allow-Credentials" ) ).isEmpty ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: assertNoBrowserAccessHeaders
    //
    // Description:
    //
    //   Verifies that a response grants no browser cross-origin readability.
    //
    // Arguments:
    //
    //   response (HttpResponse <?>):
    //     The response to inspect.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void assertNoBrowserAccessHeaders ( HttpResponse <?> response )
    {
        // Verify every header capable of granting or exposing browser access is absent.

        assertThat ( response.headers ().firstValue ( "Access-Control-Allow-Origin" ) ).isEmpty ();
        assertThat ( response.headers ().firstValue ( "Access-Control-Allow-Methods" ) ).isEmpty ();
        assertThat ( response.headers ().firstValue ( "Access-Control-Allow-Headers" ) ).isEmpty ();
        assertThat ( response.headers ().firstValue ( "Access-Control-Expose-Headers" ) ).isEmpty ();
        assertThat ( response.headers ().firstValue ( "Access-Control-Allow-Private-Network" ) ).isEmpty ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: projectRoot
    //
    // Description:
    //
    //   Resolves the project root supplied by the Maven test configuration.
    //
    // Returns:
    //
    //   The project root path.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Path projectRoot ()
    {
        // Return the configured project root to the caller.

        return Path.of ( System.getProperty ( "eca.project.root" ) );
    }
}
