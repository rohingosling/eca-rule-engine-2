//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies the shared JSON setup, server startup, temporary-directory, fixture, and port-allocation conventions.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import com.rohingosling.eca.json.JsonSupport;
import com.rohingosling.eca.server.EcaHttpServer;
import com.rohingosling.eca.server.ServerConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

//*********************************************************************************************************************
// Class: FoundationVerificationTest
//
// Description:
//
//   Verifies the shared JSON setup, server startup, temporary-directory, fixture, and port-allocation conventions.
//
//*********************************************************************************************************************

final class FoundationVerificationTest
{
    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: jsonSupport_preservesStatusValue
    //
    // Description:
    //
    //   Verifies that json support preserves status value.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void jsonSupport_preservesStatusValue ()
    {
        // Initialize the status document by applying create status document.

        String statusDocument = JsonSupport.createStatusDocument ( "UP" );

        // Verify the JSON support preserves status value scenario against its expected outcome.

        assertThat ( JsonSupport.readStatus ( statusDocument ) ).isEqualTo ( "UP" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: serverHealth_usesOperatingSystemAllocatedPort
    //
    // Description:
    //
    //   Verifies that server health uses operating system allocated port.
    //
    // Arguments:
    //
    //   temporaryDirectory (Path):
    //     The temporary directory to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void serverHealth_usesOperatingSystemAllocatedPort (
        @TempDir Path temporaryDirectory
    ) throws IOException, InterruptedException
    {
        // Initialize the configuration by applying of seconds.

        ServerConfiguration configuration = new ServerConfiguration (
            "127.0.0.1",
            0,
            temporaryDirectory,
            null,
            null,
            ServerConfiguration.DEFAULT_MODEL_BODY_BYTES,
            ServerConfiguration.DEFAULT_EVALUATION_BODY_BYTES,
            ServerConfiguration.DEFAULT_HEADER_BYTES,
            Duration.ofSeconds ( 30 ),
            Duration.ofSeconds ( 15 )
        );

        // Open the scoped resources for the protected operation and close them automatically afterward.

        try ( EcaHttpServer webServer = EcaHttpServer.start ( configuration ) )
        {
            // Prepare the request URI, request, and response values needed by the server health uses operating system
            // allocated port operation.

            URI requestUri = URI.create (
                "http://127.0.0.1:" + webServer.getPort () + EcaHttpServer.LIVENESS_PATH
            );
            HttpRequest request = HttpRequest.newBuilder ( requestUri ).GET ().build ();
            HttpResponse <String> response = HttpClient.newHttpClient ().send (
                request,
                HttpResponse.BodyHandlers.ofString ( StandardCharsets.UTF_8 )
            );

            // Verify the server health uses operating system allocated port scenario against its expected outcome.

            assertThat ( response.statusCode () ).isEqualTo ( 200 );
            assertThat ( JsonSupport.readStatus ( response.body () ) ).isEqualTo ( "UP" );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: fixtureCopy_usesJUnitTemporaryDirectory
    //
    // Description:
    //
    //   Verifies that fixture copy uses j unit temporary directory.
    //
    // Arguments:
    //
    //   temporaryDirectory (Path):
    //     The temporary directory to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void fixtureCopy_usesJUnitTemporaryDirectory ( @TempDir Path temporaryDirectory ) throws IOException
    {
        // Initialize the copied fixture path by applying resolve.

        Path copiedFixturePath = temporaryDirectory.resolve ( "status-up.json" );

        // Open the scoped resources for the protected operation and close them automatically afterward.

        try ( InputStream fixtureStream = FoundationVerificationTest.class.getResourceAsStream (
            "/fixtures/foundation/status-up.json"
        ) )
        {
            // Verify the fixture copy uses j unit temporary directory scenario against its expected outcome.

            assertThat ( fixtureStream ).isNotNull ();
            Files.copy ( fixtureStream, copiedFixturePath );
        }

        // Verify the fixture copy uses j unit temporary directory scenario against its expected outcome.

        assertThat ( JsonSupport.readStatus ( Files.readString ( copiedFixturePath ) ) ).isEqualTo ( "UP" );
    }
}
