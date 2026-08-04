//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Calls the versioned server health and model endpoints through the JDK asynchronous HTTP client.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.client;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

import com.rohingosling.eca.application.ClientDocumentCodec;
import com.rohingosling.eca.application.ClientDocumentSession;
import com.rohingosling.eca.application.ClientEvaluationResult;
import com.rohingosling.eca.application.ClientSimulator.SimulationRequest;
import com.rohingosling.eca.json.EventOccurrenceJsonCodec;
import com.rohingosling.eca.json.HttpJsonCodec;

//*********************************************************************************************************************
// Class: ClientServerGateway
//
// Description:
//
//   Calls the versioned server health and model endpoints through the JDK asynchronous HTTP client.
//
//*********************************************************************************************************************

public final class ClientServerGateway implements ClientServerOperations
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    private static final String LIVENESS_PATH   = "api/v1/health/live";
    private static final String MODEL_PATH      = "api/v1/model";
    private static final String EVALUATION_PATH = "api/v1/evaluations";

    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final ClientDocumentCodec      clientDocumentCodec;
    private final EventOccurrenceJsonCodec eventOccurrenceJsonCodec;
    private final HttpJsonCodec            httpJsonCodec;

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: ClientServerGateway
    //
    // Description:
    //
    //   Creates the ClientServerGateway instance from the supplied values.
    //
    // Arguments:
    //
    //   clientDocumentCodec (ClientDocumentCodec):
    //     The client document codec to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ClientServerGateway ( ClientDocumentCodec clientDocumentCodec )
    {
        // Validate the required client document codec before continuing.

        this.clientDocumentCodec      = Objects.requireNonNull ( clientDocumentCodec, "clientDocumentCodec" );
        this.eventOccurrenceJsonCodec = new EventOccurrenceJsonCodec ();
        this.httpJsonCodec            = new HttpJsonCodec ();
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: testConnection
    //
    // Description:
    //
    //   Performs the test connection operation.
    //
    // Arguments:
    //
    //   settings (ClientConnectionSettings):
    //     The settings to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public CompletableFuture <String> testConnection ( ClientConnectionSettings settings )
    {
        // Initialize the request by applying build, get, and request builder.

        HttpRequest request = requestBuilder ( settings, LIVENESS_PATH )
            .GET ()
            .build ();

        // Return the result produced by map cancellable.

        return mapCancellable (
            client ( settings ).sendAsync (
                request,
                HttpResponse.BodyHandlers.ofString ( StandardCharsets.UTF_8 )
            ),
            response ->
            {
                // Apply require successful response, status code, and body to the response for the test connection
                // operation.

                requireSuccessfulResponse (
                    response.statusCode (),
                    response.body (),
                    LIVENESS_PATH
                );

                // Return the composed test connection value.

                return "Connected (HTTP " + response.statusCode () + ")";
            },
            LIVENESS_PATH
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: pullModel
    //
    // Description:
    //
    //   Performs the pull model operation.
    //
    // Arguments:
    //
    //   settings (ClientConnectionSettings):
    //     The settings to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public CompletableFuture <ClientDocumentSession.Content> pullModel ( ClientConnectionSettings settings )
    {
        // Return the result produced by then apply.

        return this.pullModelWithRevision ( settings ).thenApply ( ModelPull::getContent );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: pullModelWithRevision
    //
    // Description:
    //
    //   Performs the pull model with revision operation.
    //
    // Arguments:
    //
    //   settings (ClientConnectionSettings):
    //     The settings to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public CompletableFuture <ModelPull> pullModelWithRevision (
        ClientConnectionSettings settings
    )
    {
        // Initialize the request builder by applying get and request builder.

        HttpRequest.Builder requestBuilder = requestBuilder ( settings, MODEL_PATH ).GET ();

        // Complete the pull model with revision step by calling authorize.

        authorize ( requestBuilder, settings );

        // Return the result produced by map cancellable.

        return mapCancellable (
            client ( settings ).sendAsync (
                requestBuilder.build (),
                HttpResponse.BodyHandlers.ofByteArray ()
            ),
            response ->
            {
                // Apply require successful response, status code, and body to the response for the pull model with
                // revision operation.

                requireSuccessfulResponse (
                    response.statusCode (),
                    new String ( response.body (), StandardCharsets.UTF_8 ),
                    MODEL_PATH
                );

                // Prepare the content and revision values needed by the pull model with revision operation.

                ClientDocumentSession.Content content = this.clientDocumentCodec.read ( response.body () );
                String revision = response.headers ().firstValue ( "ETag" )
                    .map ( ClientServerGateway::normalizeEntityTag )
                    .filter ( value -> !value.isBlank () )
                    .orElseGet ( () -> this.clientDocumentCodec.revision ( content ) );

                // Return a newly constructed model pull containing the operation result.

                return new ModelPull ( content, revision );
            },
            MODEL_PATH
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: pushModel
    //
    // Description:
    //
    //   Performs the push model operation.
    //
    // Arguments:
    //
    //   settings (ClientConnectionSettings):
    //     The settings to use.
    //
    //   content (ClientDocumentSession.Content):
    //     The content to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public CompletableFuture <String> pushModel (
        ClientConnectionSettings settings,
        ClientDocumentSession.Content content
    )
    {
        // Return the result produced by then apply.

        return this.pushModelWithRevision ( settings, content ).thenApply ( ModelPush::getMessage );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: pushModelWithRevision
    //
    // Description:
    //
    //   Performs the push model with revision operation.
    //
    // Arguments:
    //
    //   settings (ClientConnectionSettings):
    //     The settings to use.
    //
    //   content (ClientDocumentSession.Content):
    //     The content to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public CompletableFuture <ModelPush> pushModelWithRevision (
        ClientConnectionSettings settings,
        ClientDocumentSession.Content content
    )
    {
        // Prepare the document and request builder values needed by the push model with revision operation.

        byte [] document = this.clientDocumentCodec.write ( Objects.requireNonNull ( content, "content" ) );
        HttpRequest.Builder requestBuilder = requestBuilder ( settings, MODEL_PATH )
            .header ( "Content-Type", "application/json; charset=utf-8" )
            .PUT ( HttpRequest.BodyPublishers.ofByteArray ( document ) );

        // Complete the push model with revision step by calling authorize.

        authorize ( requestBuilder, settings );

        // Return the result produced by map cancellable.

        return mapCancellable (
            client ( settings ).sendAsync (
                requestBuilder.build (),
                HttpResponse.BodyHandlers.ofString ( StandardCharsets.UTF_8 )
            ),
            response ->
            {
                // Apply require successful response, status code, and body to the response for the push model with
                // revision operation.

                requireSuccessfulResponse (
                    response.statusCode (),
                    response.body (),
                    MODEL_PATH
                );

                String revision;

                try
                {
                    // Update the revision from the body result.

                    revision = this.httpJsonCodec.readModelRevision ( response.body () );
                }

                // Handle illegal argument failures captured as exception.

                catch ( IllegalArgumentException exception )
                {
                    // Update the revision from the revision result.

                    revision = this.clientDocumentCodec.revision ( content );
                }

                // Return a newly constructed model push containing the operation result.

                return new ModelPush (
                    "Model accepted (HTTP " + response.statusCode () + ")",
                    revision
                );
            },
            MODEL_PATH
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: evaluateOccurrence
    //
    // Description:
    //
    //   Performs the evaluate occurrence operation.
    //
    // Arguments:
    //
    //   settings (ClientConnectionSettings):
    //     The settings to use.
    //
    //   simulationRequest (SimulationRequest):
    //     The simulation request to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public CompletableFuture <ClientEvaluationResult> evaluateOccurrence (
        ClientConnectionSettings settings,
        SimulationRequest simulationRequest
    )
    {
        // Prepare the document, request builder, and start nanoseconds values needed by the evaluate occurrence
        // operation.

        String document = this.eventOccurrenceJsonCodec.write (
            Objects.requireNonNull ( simulationRequest, "simulationRequest" )
        );
        HttpRequest.Builder requestBuilder = requestBuilder ( settings, EVALUATION_PATH )
            .header ( "Content-Type", "application/json; charset=utf-8" )
            .POST ( HttpRequest.BodyPublishers.ofString ( document, StandardCharsets.UTF_8 ) );
        long startNanoseconds = System.nanoTime ();

        // Complete the evaluate occurrence step by calling authorize.

        authorize ( requestBuilder, settings );

        // Return the result produced by map cancellable.

        return mapCancellable (
            client ( settings ).sendAsync (
                requestBuilder.build (),
                HttpResponse.BodyHandlers.ofString ( StandardCharsets.UTF_8 )
            ),
            response ->
            {
                // Apply require successful response, status code, and body to the response for the evaluate occurrence
                // operation.

                requireSuccessfulResponse (
                    response.statusCode (),
                    response.body (),
                    EVALUATION_PATH
                );

                // Initialize the round trip microseconds by applying max, to nanos, of nanos, and nano time.

                long roundTripMicroseconds = Math.max (
                    0,
                    Duration.ofNanos ( System.nanoTime () - startNanoseconds ).toNanos () / 1000
                );

                // Return the result produced by with round trip microseconds.

                return this.httpJsonCodec.readEvaluation ( response.body () )
                    .withRoundTripMicroseconds ( roundTripMicroseconds );
            },
            EVALUATION_PATH
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: modelRevision
    //
    // Description:
    //
    //   Performs the model revision operation.
    //
    // Arguments:
    //
    //   content (ClientDocumentSession.Content):
    //     The content to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public String modelRevision ( ClientDocumentSession.Content content )
    {
        // Return the result produced by revision.

        return this.clientDocumentCodec.revision ( content );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: client
    //
    // Description:
    //
    //   Performs the client operation.
    //
    // Arguments:
    //
    //   settings (ClientConnectionSettings):
    //     The settings to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static HttpClient client ( ClientConnectionSettings settings )
    {
        // Return the fully configured client result.

        return HttpClient.newBuilder ()
            .connectTimeout ( settings.getConnectTimeout () )
            .build ();
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
    //   settings (ClientConnectionSettings):
    //     The settings to use.
    //
    //   relativePath (String):
    //     The relative path to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static HttpRequest.Builder requestBuilder (
        ClientConnectionSettings settings,
        String relativePath
    )
    {
        // Validate the required settings before continuing.

        Objects.requireNonNull ( settings, "settings" );

        // Initialize the endpoint by applying resolve and get base URI.

        URI endpoint = settings.getBaseUri ().resolve ( relativePath );

        // Return the result produced by header.

        return HttpRequest.newBuilder ( endpoint )
            .timeout ( settings.getRequestTimeout () )
            .header ( "Accept", "application/json" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: authorize
    //
    // Description:
    //
    //   Performs the authorize operation.
    //
    // Arguments:
    //
    //   requestBuilder (HttpRequest.Builder):
    //     The request builder to use.
    //
    //   settings (ClientConnectionSettings):
    //     The settings to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void authorize (
        HttpRequest.Builder requestBuilder,
        ClientConnectionSettings settings
    )
    {
        // Initialize the bearer token by applying bearer token.

        String bearerToken = bearerToken ( settings );

        // Handle the branch where bearer token contains text.

        if ( !bearerToken.isBlank () )
        {
            // Attach the required HTTP header to the request.

            requestBuilder.header ( "Authorization", "Bearer " + bearerToken );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requireSuccessfulResponse
    //
    // Description:
    //
    //   Performs the require successful response operation.
    //
    // Arguments:
    //
    //   statusCode (int):
    //     The status code to use.
    //
    //   responseBody (String):
    //     The response body to use.
    //
    //   relativePath (String):
    //     The relative path to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void requireSuccessfulResponse (
        int statusCode,
        String responseBody,
        String relativePath
    )
    {
        // Reject the operation when status code is less than 200 or status code is at least 300.

        if ( statusCode < 200 || statusCode >= 300 )
        {
            throw new CompletionException (
                new ClientRemoteException (
                    classifyStatus ( statusCode, relativePath ),
                    this.httpJsonCodec.readProblemDetail ( responseBody, statusCode )
                )
            );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: mapCancellable
    //
    // Description:
    //
    //   Performs the map cancellable operation.
    //
    // Arguments:
    //
    //   sourceFuture (CompletableFuture <SourceType>):
    //     The source future to use.
    //
    //   mapper (Function <SourceType, ResultType>):
    //     The mapper to use.
    //
    //   relativePath (String):
    //     The relative path to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static <SourceType, ResultType> CompletableFuture <ResultType> mapCancellable (
        CompletableFuture <SourceType> sourceFuture,
        Function <SourceType, ResultType> mapper,
        String relativePath
    )
    {
        // Initialize the result future with a new completable future.

        CompletableFuture <ResultType> resultFuture = new CompletableFuture <ResultType> ();

        // Perform the when complete, complete exceptionally, translate failure, complete, apply, is cancelled, and
        // cancel calls required by the map cancellable operation.

        sourceFuture.whenComplete (
            ( sourceValue, throwable ) ->
            {
                // Handle the branch where throwable is available.

                if ( throwable != null )
                {
                    // Apply complete exceptionally and translate failure to the result future for the map cancellable
                    // operation.

                    resultFuture.completeExceptionally (
                        translateFailure ( throwable, relativePath )
                    );
                }

                // Handle the alternative path when the preceding condition is not satisfied.

                else
                {
                    try
                    {
                        // Perform the complete and apply calls required by the map cancellable operation.

                        resultFuture.complete ( mapper.apply ( sourceValue ) );
                    }

                    // Handle throwable failures captured as mapping failure.

                    catch ( Throwable mappingFailure )
                    {
                        // Initialize the translated failure by applying translate failure.

                        Throwable translatedFailure = translateFailure (
                            mappingFailure,
                            relativePath
                        );

                        // Handle the branch where translated failure is an illegal argument exception.

                        if ( translatedFailure instanceof IllegalArgumentException )
                        {
                            // Construct the client remote exception instance required by the map cancellable
                            // operation.

                            translatedFailure = new ClientRemoteException (
                                ClientRemoteException.Kind.PROTOCOL,
                                "The server returned an invalid response for /" + relativePath + ".",
                                translatedFailure
                            );
                        }

                        // Complete the pending result with the captured failure.

                        resultFuture.completeExceptionally ( translatedFailure );
                    }
                }
            }
        );
        resultFuture.whenComplete (
            ( resultValue, throwable ) ->
            {
                // Handle the branch where result future is cancelled.

                if ( resultFuture.isCancelled () )
                {
                    // Complete the map cancellable step by calling cancel.

                    sourceFuture.cancel ( true );
                }
            }
        );

        // Return the result future to the caller.

        return resultFuture;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: classifyStatus
    //
    // Description:
    //
    //   Performs the classify status operation.
    //
    // Arguments:
    //
    //   statusCode (int):
    //     The status code to use.
    //
    //   relativePath (String):
    //     The relative path to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static ClientRemoteException.Kind classifyStatus ( int statusCode, String relativePath )
    {
        // Stop this path and return its result when status code equals 401 or status code equals 403.

        if ( statusCode == 401 || statusCode == 403 )
        {
            // Return the authentication to the caller when status code equals 401 or status code equals 403.

            return ClientRemoteException.Kind.AUTHENTICATION;
        }

        // Stop this path and return its result when status code equals 404 and relative path matches model path or
        // status code equals 503 and relative path matches evaluation path.

        if (
            statusCode == 404 && relativePath.equals ( MODEL_PATH )
                || statusCode == 503 && relativePath.equals ( EVALUATION_PATH )
        )
        {
            // Return the no model to the caller when status code equals 404 and relative path matches model path or
            // status code equals 503 and relative path matches evaluation path.

            return ClientRemoteException.Kind.NO_MODEL;
        }

        // Stop this path and return its result when status code equals 422.

        if ( statusCode == 422 )
        {
            // Return the validation to the caller when status code equals 422.

            return ClientRemoteException.Kind.VALIDATION;
        }

        // Stop this path and return its result when status code equals 400 or status code equals 413 or status code
        // equals 415.

        if ( statusCode == 400 || statusCode == 413 || statusCode == 415 )
        {
            // Return the protocol to the caller when status code equals 400 or status code equals 413 or status code
            // equals 415.

            return ClientRemoteException.Kind.PROTOCOL;
        }

        // Return the remote to the caller.

        return ClientRemoteException.Kind.REMOTE;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: translateFailure
    //
    // Description:
    //
    //   Performs the translate failure operation.
    //
    // Arguments:
    //
    //   throwable (Throwable):
    //     The exception that caused the operation to fail.
    //
    //   relativePath (String):
    //     The relative path to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Throwable translateFailure ( Throwable throwable, String relativePath )
    {
        // Initialize the cause by applying unwrap.

        Throwable cause = unwrap ( throwable );

        // Stop this path and return its result when cause is a cancellation exception.

        if ( cause instanceof CancellationException )
        {
            // Return the cause to the caller when cause is a cancellation exception.

            return cause;
        }

        // Stop this path and return its result when cause is a client remote exception.

        if ( cause instanceof ClientRemoteException )
        {
            // Return the cause to the caller when cause is a client remote exception.

            return cause;
        }

        // Stop this path and return its result when cause is an HTTP timeout exception.

        if ( cause instanceof HttpTimeoutException )
        {
            // Return a newly constructed client remote exception containing the operation result when cause is an HTTP
            // timeout exception.

            return new ClientRemoteException (
                ClientRemoteException.Kind.TIMEOUT,
                "The server request timed out while calling /" + relativePath + ".",
                cause
            );
        }

        // Stop this path and return its result when cause is a connect exception or cause is an I/O exception.

        if ( cause instanceof ConnectException || cause instanceof IOException )
        {
            // Return a newly constructed client remote exception containing the operation result when cause is a
            // connect exception or cause is an I/O exception.

            return new ClientRemoteException (
                ClientRemoteException.Kind.CONNECTION,
                "The server could not be reached at /" + relativePath + ".",
                cause
            );
        }

        // Return the cause to the caller.

        return cause;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: unwrap
    //
    // Description:
    //
    //   Performs the unwrap operation.
    //
    // Arguments:
    //
    //   throwable (Throwable):
    //     The exception that caused the operation to fail.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Throwable unwrap ( Throwable throwable )
    {
        Throwable currentThrowable = throwable;

        // Continue processing while current throwable is a completion exception and current throwable cause is
        // available.

        while (
            currentThrowable instanceof CompletionException
                && currentThrowable.getCause () != null
        )
        {
            // Update the current throwable from the get cause result.

            currentThrowable = currentThrowable.getCause ();
        }

        // Return the current throwable to the caller.

        return currentThrowable;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: bearerToken
    //
    // Description:
    //
    //   Performs the bearer token operation.
    //
    // Arguments:
    //
    //   settings (ClientConnectionSettings):
    //     The settings to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String bearerToken ( ClientConnectionSettings settings )
    {
        // Stop this path and return its result when settings bearer token contains text.

        if ( !settings.getBearerToken ().isBlank () )
        {
            // Return the result produced by get bearer token when settings bearer token contains text.

            return settings.getBearerToken ();
        }

        // Initialize the environment token by applying getenv.

        String environmentToken = System.getenv ( "ECA_SERVER_TOKEN" );

        // Stop this path and return its result when environment token is available and environment token contains
        // text.

        if ( environmentToken != null && !environmentToken.isBlank () )
        {
            // Return the result produced by trim when environment token is available and environment token contains
            // text.

            return environmentToken.trim ();
        }

        // Stop this path and return its result when the operation is not loopback host.

        if ( !isLoopbackHost ( settings.getBaseUri ().getHost () ) )
        {
            // Return the bearer token text to the caller when the operation is not loopback host.

            return "";
        }

        // Return the result produced by or else.

        return discoverLocalControlToken ().orElse ( "" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: discoverLocalControlToken
    //
    // Description:
    //
    //   Performs the discover local control token operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Optional <String> discoverLocalControlToken ()
    {
        // Prepare the local application data and token path values needed by the discover local control token
        // operation.

        String localApplicationData = System.getenv ( "LOCALAPPDATA" );
        Path tokenPath = localApplicationData == null || localApplicationData.isBlank ()
            ? Path.of (
                System.getProperty ( "user.home" ),
                "AppData",
                "Local",
                "EcaRuleEngine2",
                "control-token"
            )
            : Path.of ( localApplicationData, "EcaRuleEngine2", "control-token" );

        try
        {
            // Stop this path and return its result when files is not regular file.

            if ( !Files.isRegularFile ( tokenPath ) )
            {
                // Return an empty optional result because no value is available when files is not regular file.

                return Optional.empty ();
            }

            // Initialize the token by applying trim and read string.

            String token = Files.readString ( tokenPath, StandardCharsets.UTF_8 ).trim ();

            // Return the value selected according to token is blank.

            return token.isBlank () ? Optional.empty () : Optional.of ( token );
        }

        // Handle I/O or security failures captured as exception.

        catch ( IOException | SecurityException exception )
        {
            // Return an empty optional result because no value is available.

            return Optional.empty ();
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: isLoopbackHost
    //
    // Description:
    //
    //   Indicates whether loopback host.
    //
    // Arguments:
    //
    //   host (String):
    //     The host to use.
    //
    // Returns:
    //
    //   `true` when the condition is satisfied; otherwise `false`.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static boolean isLoopbackHost ( String host )
    {
        // Return whether host is available and host equals ignore case succeeds or host matches "::1" or host starts
        // with succeeds.

        return host != null
            && (
                host.equalsIgnoreCase ( "localhost" )
                    || host.equals ( "::1" )
                    || host.startsWith ( "127." )
            );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: normalizeEntityTag
    //
    // Description:
    //
    //   Performs the normalize entity tag operation.
    //
    // Arguments:
    //
    //   entityTag (String):
    //     The entity tag to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String normalizeEntityTag ( String entityTag )
    {
        // Initialize the normalized entity tag by applying trim.

        String normalizedEntityTag = entityTag.trim ();

        // Stop this path and return its result when normalized entity tag length is at least 2 and normalized entity
        // tag starts with succeeds and normalized entity tag ends with succeeds.

        if (
            normalizedEntityTag.length () >= 2
                && normalizedEntityTag.startsWith ( "\"" )
                && normalizedEntityTag.endsWith ( "\"" )
        )
        {
            // Return the result produced by substring when normalized entity tag length is at least 2 and normalized
            // entity tag starts with succeeds and normalized entity tag ends with succeeds.

            return normalizedEntityTag.substring ( 1, normalizedEntityTag.length () - 1 );
        }

        // Return the normalized entity tag to the caller.

        return normalizedEntityTag;
    }
}
