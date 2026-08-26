//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Calls the authenticated model-replacement and graceful-stop endpoints for server CLI commands.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.server;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

import com.rohingosling.eca.json.HttpJsonCodec;

//*********************************************************************************************************************
// Class: ServerCommandClient
//
// Description:
//
//   Calls the authenticated model-replacement and graceful-stop endpoints for server CLI commands.
//
//*********************************************************************************************************************

final class ServerCommandClient
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final URI serverUri;
    private final String bearerToken;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final HttpJsonCodec httpJsonCodec;

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: ServerCommandClient
    //
    // Description:
    //
    //   Creates the ServerCommandClient instance from the supplied values.
    //
    // Arguments:
    //
    //   serverUrl (String):
    //     The server url to use.
    //
    //   bearerToken (String):
    //     The bearer token to use.
    //
    //   connectTimeout (Duration):
    //     The connect timeout to use.
    //
    //   requestTimeout (Duration):
    //     The request timeout to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    ServerCommandClient (
        String serverUrl,
        String bearerToken,
        Duration connectTimeout,
        Duration requestTimeout
    )
    {
        // Perform the validate server URI, require non null, build, connect timeout, and new builder calls required by
        // the server command client operation.

        this.serverUri    = validateServerUri ( serverUrl );
        this.bearerToken  = Objects.requireNonNull ( bearerToken, "bearerToken" );
        this.timeout      = Objects.requireNonNull ( requestTimeout, "requestTimeout" );
        this.httpClient   = HttpClient.newBuilder ()
            .connectTimeout ( Objects.requireNonNull ( connectTimeout, "connectTimeout" ) )
            .build ();
        this.httpJsonCodec = new HttpJsonCodec ();
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: replaceModel
    //
    // Description:
    //
    //   Performs the replace model operation.
    //
    // Arguments:
    //
    //   modelDocument (byte []):
    //     The model document to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    CommandResponse replaceModel ( byte [] modelDocument )
    {
        // Initialize the request by applying build, put, header, request builder, and of byte array.

        HttpRequest request = this.requestBuilder ( EcaHttpServer.MODEL_PATH )
            .header ( "Content-Type", "application/json" )
            .PUT ( HttpRequest.BodyPublishers.ofByteArray ( modelDocument ) )
            .build ();

        // Return the result produced by send.

        return this.send ( request );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: stop
    //
    // Description:
    //
    //   Performs the stop operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    CommandResponse stop ()
    {
        // Initialize the request by applying build, post, request builder, and no body.

        HttpRequest request = this.requestBuilder ( EcaHttpServer.STOP_PATH )
            .POST ( HttpRequest.BodyPublishers.noBody () )
            .build ();

        // Return the result produced by send.

        return this.send ( request );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readModelRevision
    //
    // Description:
    //
    //   Performs the read model revision operation.
    //
    // Arguments:
    //
    //   response (CommandResponse):
    //     The response to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    String readModelRevision ( CommandResponse response )
    {
        // Return the result produced by read model revision.

        return this.httpJsonCodec.readModelRevision ( response.getBody () );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readProblemDetail
    //
    // Description:
    //
    //   Performs the read problem detail operation.
    //
    // Arguments:
    //
    //   response (CommandResponse):
    //     The response to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    String readProblemDetail ( CommandResponse response )
    {
        // Return the result produced by read problem detail.

        return this.httpJsonCodec.readProblemDetail (
            response.getBody (),
            response.getStatusCode ()
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requestBuilder
    //
    // Description:
    //
    //   Performs the request builder operation.
    //
    // Arguments:
    //
    //   path (String):
    //     The path to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private HttpRequest.Builder requestBuilder ( String path )
    {
        // Return the result produced by header.

        return HttpRequest.newBuilder ( this.serverUri.resolve ( path ) )
            .timeout ( this.timeout )
            .header ( "Accept", "application/json, application/problem+json" )
            .header ( "Authorization", "Bearer " + this.bearerToken );
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
    //   request (HttpRequest):
    //     The request to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private CommandResponse send ( HttpRequest request )
    {
        try
        {
            // Initialize the response by applying send and of string.

            HttpResponse <String> response = this.httpClient.send (
                request,
                HttpResponse.BodyHandlers.ofString ()
            );

            // Return a newly constructed command response containing the operation result.

            return new CommandResponse ( response.statusCode (), response.body () );
        }

        // Handle interrupted failures captured as exception.

        catch ( InterruptedException exception )
        {
            // Perform the interrupt and current thread calls required by the send operation.

            Thread.currentThread ().interrupt ();

            throw new ServerConnectionException ( "The request was interrupted.", exception );
        }

        // Handle I/O failures captured as exception.

        catch ( IOException exception )
        {
            throw new ServerConnectionException (
                "The server could not be reached before the configured timeout.",
                exception
            );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validateServerUri
    //
    // Description:
    //
    //   Performs the validate server uri operation.
    //
    // Arguments:
    //
    //   serverUrl (String):
    //     The server url to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static URI validateServerUri ( String serverUrl )
    {
        // Prepare the server URI and scheme values needed by the validate server URI operation.

        URI serverUri = URI.create ( Objects.requireNonNull ( serverUrl, "serverUrl" ) );
        String scheme = serverUri.getScheme () == null
            ? ""
            : serverUri.getScheme ().toLowerCase ( Locale.ROOT );

        // Reject the operation when scheme differs from "http" or scheme differs from "https" or server URI host is
        // unavailable or server URI query is available or server URI fragment is available.

        if (
            !( scheme.equals ( "http" ) || scheme.equals ( "https" ) )
            || serverUri.getHost () == null
            || serverUri.getQuery () != null
            || serverUri.getFragment () != null
        )
        {
            throw new IllegalArgumentException (
                "The server URL must be an absolute HTTP or HTTPS URL."
            );
        }

        // Initialize the normalized by applying to string.

        String normalized = serverUri.toString ();

        // Handle the branch where normalized ends with does not succeed.

        if ( !normalized.endsWith ( "/" ) )
        {
            normalized += "/";
        }

        // Return the result produced by create.

        return URI.create ( normalized );
    }

    //*****************************************************************************************************************
    // Class: CommandResponse
    //
    // Description:
    //
    //   Provides the command response behavior.
    //
    //*****************************************************************************************************************

    static final class CommandResponse
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final int statusCode;
        private final String body;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getStatusCode
        //
        // Description:
        //
        //   Returns the status code.
        //
        // Returns:
        //
        //   The status code.
        //
        //-------------------------------------------------------------------------------------------------------------

        int getStatusCode ()
        {
            // Return the status code to the caller.

            return this.statusCode;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getBody
        //
        // Description:
        //
        //   Returns the body.
        //
        // Returns:
        //
        //   The body.
        //
        //-------------------------------------------------------------------------------------------------------------

        String getBody ()
        {
            // Return the body to the caller.

            return this.body;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: CommandResponse
        //
        // Description:
        //
        //   Creates the CommandResponse instance from the supplied values.
        //
        // Arguments:
        //
        //   statusCode (int):
        //     The status code to use.
        //
        //   body (String):
        //     The body to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        CommandResponse ( int statusCode, String body )
        {
            this.statusCode = statusCode;
            this.body       = body == null ? "" : body;
        }

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: isSuccessful
        //
        // Description:
        //
        //   Indicates whether successful.
        //
        // Returns:
        //
        //   `true` when the condition is satisfied; otherwise `false`.
        //
        //-------------------------------------------------------------------------------------------------------------

        boolean isSuccessful ()
        {
            // Return whether status code is at least 200 and status code is less than 300.

            return this.statusCode >= 200 && this.statusCode < 300;
        }
    }

    //*****************************************************************************************************************
    // Class: ServerConnectionException
    //
    // Description:
    //
    //   Provides the server connection exception behavior.
    //
    //*****************************************************************************************************************

    static final class ServerConnectionException extends RuntimeException
    {
        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: ServerConnectionException
        //
        // Description:
        //
        //   Creates the ServerConnectionException instance from the supplied values.
        //
        // Arguments:
        //
        //   message (String):
        //     The message to use.
        //
        //   cause (Throwable):
        //     The exception that caused the operation to fail.
        //
        //-------------------------------------------------------------------------------------------------------------

        ServerConnectionException ( String message, Throwable cause )
        {
            // Initialize the inherited state through the base-class constructor.

            super ( message, cause );
        }
    }
}
