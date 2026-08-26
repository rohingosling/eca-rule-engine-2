//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies the simulator, revision-aware transfers, remote-failure mapping, and end-to-end client workflow.
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
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.rohingosling.eca.application.ClientDocumentSession;
import com.rohingosling.eca.application.ClientEvaluationResult;
import com.rohingosling.eca.application.ClientSimulator;
import com.rohingosling.eca.application.ClientSimulator.EventDescriptor;
import com.rohingosling.eca.application.ClientSimulator.PayloadFieldDescriptor;
import com.rohingosling.eca.application.ClientSimulator.PayloadValueDraft;
import com.rohingosling.eca.application.ClientSimulator.SimulationRequest;
import com.rohingosling.eca.client.ClientConnectionSettings;
import com.rohingosling.eca.client.ClientDocumentOperations;
import com.rohingosling.eca.client.ClientPresenter;
import com.rohingosling.eca.client.ClientRemoteException;
import com.rohingosling.eca.client.ClientServerGateway;
import com.rohingosling.eca.client.ClientServerOperations;
import com.rohingosling.eca.client.ClientServerOperations.ModelPull;
import com.rohingosling.eca.client.ClientServerOperations.ModelPush;
import com.rohingosling.eca.client.ClientView;
import com.rohingosling.eca.client.ClientView.DirtyDocumentDecision;
import com.rohingosling.eca.json.AuthoringModelClientDocumentCodec;
import com.rohingosling.eca.json.AuthoringModelJsonCodec;
import com.rohingosling.eca.json.HttpJsonCodec;
import com.rohingosling.eca.server.ControlTokenFile;
import com.rohingosling.eca.server.EcaHttpServer;
import com.rohingosling.eca.server.ServerConfiguration;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

//*********************************************************************************************************************
// Class: ClientPhaseEightTest
//
// Description:
//
//   Verifies the simulator, revision-aware transfers, remote-failure mapping, and end-to-end client workflow.
//
//*********************************************************************************************************************

final class ClientPhaseEightTest
{
    @TempDir

    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private Path temporaryDirectory;

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: simulatorDerivesTypedFieldsAndPreservesConcreteNullAndOmittedStates
    //
    // Description:
    //
    //   Performs the simulator derives typed fields and preserves concrete null and omitted states operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void simulatorDerivesTypedFieldsAndPreservesConcreteNullAndOmittedStates ()
    {
        // Prepare the simulator, content, order event, fields, and payload drafts values needed by the simulator
        // derives typed fields and preserves concrete null and omitted states operation.

        ClientSimulator simulator = new ClientSimulator ();
        ClientDocumentSession.Content content = courierContent ();
        EventDescriptor orderEvent = simulator.getEvents ( content ).stream ()
            .filter ( event -> event.getIdentifier ().equals ( "event-order-product" ) )
            .findFirst ()
            .orElseThrow ();
        Map <String, PayloadFieldDescriptor> fields = orderEvent.getPayloadFields ().stream ()
            .collect (
                java.util.stream.Collectors.toMap (
                    PayloadFieldDescriptor::getIdentifier,
                    field -> field
                )
            );
        LinkedHashMap <String, PayloadValueDraft> payloadDrafts =
            new LinkedHashMap <String, PayloadValueDraft> ();

        // Perform the put, concrete, null value, and omitted calls required by the simulator derives typed fields and
        // preserves concrete null and omitted states operation.

        payloadDrafts.put (
            "parameter-delivery-type",
            PayloadValueDraft.concrete ( "STANDARD" )
        );
        payloadDrafts.put (
            "parameter-product-category",
            PayloadValueDraft.concrete ( "RETAIL" )
        );
        payloadDrafts.put ( "parameter-quantity", PayloadValueDraft.nullValue () );
        payloadDrafts.put ( "parameter-region", PayloadValueDraft.concrete ( "LOCAL" ) );
        payloadDrafts.put ( "parameter-vip", PayloadValueDraft.omitted () );

        // Initialize the simulation request by applying create occurrence and get identifier.

        SimulationRequest simulationRequest = simulator.createOccurrence (
            content,
            orderEvent.getIdentifier (),
            payloadDrafts
        );

        // Verify the simulator derives typed fields and preserves concrete null and omitted states scenario against
        // its expected outcome.

        assertThat ( fields.get ( "parameter-delivery-type" ).getType () ).isEqualTo ( "ENUM" );
        assertThat ( fields.get ( "parameter-quantity" ).getType () ).isEqualTo ( "INTEGER" );
        assertThat ( fields.get ( "parameter-vip" ).getType () ).isEqualTo ( "BOOLEAN" );
        assertThat ( simulationRequest.getPayload () )
            .containsEntry ( "parameter-delivery-type", "STANDARD" )
            .containsEntry ( "parameter-product-category", "RETAIL" )
            .containsEntry ( "parameter-quantity", null )
            .containsEntry ( "parameter-region", "LOCAL" )
            .doesNotContainKey ( "parameter-vip" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: simulatorRejectsInvalidTypedInputBeforeNetworkWork
    //
    // Description:
    //
    //   Performs the simulator rejects invalid typed input before network work operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void simulatorRejectsInvalidTypedInputBeforeNetworkWork ()
    {
        // Initialize the simulator with a new client simulator.

        ClientSimulator simulator = new ClientSimulator ();

        // Verify the simulator rejects invalid typed input before network work scenario against its expected outcome.

        assertThatThrownBy (
            () -> simulator.createOccurrence (
                courierContent (),
                "event-order-product",
                Map.of (
                    "parameter-quantity",
                    PayloadValueDraft.concrete ( "not-an-integer" )
                )
            )
        )
            .isInstanceOf ( IllegalArgumentException.class )
            .hasMessageContaining ( "signed 64-bit integer" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: evaluationResponseCodecEnforcesActionAndNoActionShapes
    //
    // Description:
    //
    //   Performs the evaluation response codec enforces action and no action shapes operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void evaluationResponseCodecEnforcesActionAndNoActionShapes ()
    {
        // Prepare the codec, action, and no action values needed by the evaluation response codec enforces action and
        // no action shapes operation.

        HttpJsonCodec codec = new HttpJsonCodec ();
        ClientEvaluationResult action = codec.readEvaluation (
            """
            {
              "outcome": "ACTION",
              "actionId": "action-local-courier",
              "ruleId": "rule-test",
              "specificity": 9,
              "modelRevision": "sha256:abc",
              "elapsedMicroseconds": 125
            }
            """
        );
        ClientEvaluationResult noAction = codec.readEvaluation (
            """
            {
              "outcome": "NO_ACTION",
              "modelRevision": "sha256:abc",
              "elapsedMicroseconds": 73
            }
            """
        );

        // Verify the evaluation response codec enforces action and no action shapes scenario against its expected
        // outcome.

        assertThat ( action.isAction () ).isTrue ();
        assertThat ( action.getActionIdentifier () ).contains ( "action-local-courier" );
        assertThat ( action.getRuleIdentifier () ).contains ( "rule-test" );
        assertThat ( action.getSpecificity () ).hasValue ( 9 );
        assertThat ( noAction.isAction () ).isFalse ();
        assertThat ( noAction.getActionIdentifier () ).isEmpty ();

        assertThatThrownBy (
            () -> codec.readEvaluation (
                """
                {
                  "outcome": "NO_ACTION",
                  "actionId": null,
                  "modelRevision": "sha256:abc",
                  "elapsedMicroseconds": 73
                }
                """
            )
        )
            .isInstanceOf ( IllegalArgumentException.class )
            .hasMessageContaining ( "must omit" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: courierPushPullAndEvaluationsPassThroughThePackagedBoundaries
    //
    // Description:
    //
    //   Performs the courier push pull and evaluations pass through the packaged boundaries operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void courierPushPullAndEvaluationsPassThroughThePackagedBoundaries () throws Exception
    {
        // Prepare the document codec, authoring codec, content, and simulator values needed by the courier push pull
        // and evaluations pass through the packaged boundaries operation.

        AuthoringModelClientDocumentCodec documentCodec = new AuthoringModelClientDocumentCodec ();
        AuthoringModelJsonCodec authoringCodec = new AuthoringModelJsonCodec ();
        ClientDocumentSession.Content content = courierContent ();
        ClientSimulator simulator = new ClientSimulator ();

        // Open the scoped resources for the protected operation and close them automatically afterward.

        try ( EcaHttpServer server = startServer () )
        {
            // Prepare the settings, gateway, and no model request values needed by the courier push pull and
            // evaluations pass through the packaged boundaries operation.

            ClientConnectionSettings settings = settings (
                server,
                ControlTokenFile.loadExisting (
                    this.temporaryDirectory.resolve ( "control-token" )
                )
            );
            ClientServerGateway gateway = new ClientServerGateway ( documentCodec );
            SimulationRequest noModelRequest = simulator.createOccurrence (
                content,
                "event-cancel-order",
                Map.of ()
            );

            // Verify the courier push pull and evaluations pass through the packaged boundaries scenario against its
            // expected outcome.

            assertThatThrownBy (
                () -> gateway.evaluateOccurrence ( settings, noModelRequest )
                    .get ( 5, TimeUnit.SECONDS )
            )
                .hasRootCauseInstanceOf ( ClientRemoteException.class )
                .rootCause ()
                .extracting ( throwable -> ( (ClientRemoteException) throwable ).getKind () )
                .isEqualTo ( ClientRemoteException.Kind.NO_MODEL );

            // Prepare the model push, model pull, order request, matching result, and cancellation result values
            // needed by the courier push pull and evaluations pass through the packaged boundaries operation.

            ModelPush modelPush = gateway.pushModelWithRevision ( settings, content )
                .get ( 5, TimeUnit.SECONDS );
            ModelPull modelPull = gateway.pullModelWithRevision ( settings )
                .get ( 5, TimeUnit.SECONDS );
            SimulationRequest orderRequest = simulator.createOccurrence (
                content,
                "event-order-product",
                Map.of (
                    "parameter-delivery-type",
                    PayloadValueDraft.concrete ( "STANDARD" ),
                    "parameter-product-category",
                    PayloadValueDraft.concrete ( "RETAIL" ),
                    "parameter-quantity",
                    PayloadValueDraft.nullValue (),
                    "parameter-region",
                    PayloadValueDraft.concrete ( "LOCAL" ),
                    "parameter-vip",
                    PayloadValueDraft.concrete ( "false" )
                )
            );
            ClientEvaluationResult matchingResult = gateway.evaluateOccurrence (
                settings,
                orderRequest
            ).get ( 5, TimeUnit.SECONDS );
            ClientEvaluationResult cancellationResult = gateway.evaluateOccurrence (
                settings,
                noModelRequest
            ).get ( 5, TimeUnit.SECONDS );

            // Verify the courier push pull and evaluations pass through the packaged boundaries scenario against its
            // expected outcome.

            assertThat ( modelPush.getRevision () )
                .isEqualTo ( documentCodec.revision ( content ) )
                .isEqualTo ( modelPull.getRevision () );
            assertThat (
                authoringCodec.writeCanonical ( modelPull.getContent ().getAuthoringModel () )
            ).containsExactly (
                authoringCodec.writeCanonical ( content.getAuthoringModel () )
            );
            assertThat ( matchingResult.getOutcome () ).isEqualTo ( "ACTION" );
            assertThat ( matchingResult.getActionIdentifier () ).contains ( "action-local-courier" );
            assertThat ( matchingResult.getRuleIdentifier () ).contains ( "rule-test" );
            assertThat ( matchingResult.getSpecificity () ).hasValue ( 9 );
            assertThat ( matchingResult.getModelRevision () ).isEqualTo ( modelPush.getRevision () );
            assertThat ( cancellationResult.getOutcome () ).isEqualTo ( "NO_ACTION" );
            assertThat ( cancellationResult.getModelRevision () ).isEqualTo ( modelPush.getRevision () );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: authenticationFailuresAreMappedWithoutChangingTheClientDocument
    //
    // Description:
    //
    //   Performs the authentication failures are mapped without changing the client document operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void authenticationFailuresAreMappedWithoutChangingTheClientDocument () throws Exception
    {
        // Initialize the content by applying courier content.

        ClientDocumentSession.Content content = courierContent ();

        // Open the scoped resources for the protected operation and close them automatically afterward.

        try ( EcaHttpServer server = startServer () )
        {
            // Initialize the gateway with a new client server gateway.

            ClientServerGateway gateway = new ClientServerGateway (
                new AuthoringModelClientDocumentCodec ()
            );

            // Verify the authentication failures are mapped without changing the client document scenario against its
            // expected outcome.

            assertThatThrownBy (
                () -> gateway.pushModelWithRevision ( settings ( server, "wrong-token" ), content )
                    .get ( 5, TimeUnit.SECONDS )
            )
                .hasRootCauseInstanceOf ( ClientRemoteException.class )
                .rootCause ()
                .extracting ( throwable -> ( (ClientRemoteException) throwable ).getKind () )
                .isEqualTo ( ClientRemoteException.Kind.AUTHENTICATION );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validationAndProtocolFailuresAreMappedForPresentation
    //
    // Description:
    //
    //   Performs the validation and protocol failures are mapped for presentation operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void validationAndProtocolFailuresAreMappedForPresentation () throws Exception
    {
        // Prepare the document codec, gateway, content, simulator, and server values needed by the validation and
        // protocol failures are mapped for presentation operation.

        AuthoringModelClientDocumentCodec documentCodec = new AuthoringModelClientDocumentCodec ();
        ClientServerGateway gateway = new ClientServerGateway ( documentCodec );
        ClientDocumentSession.Content content = courierContent ();
        ClientSimulator simulator = new ClientSimulator ();
        HttpServer server = HttpServer.create ( new InetSocketAddress ( "127.0.0.1", 0 ), 0 );

        // Perform the create context, equals, get request method, read all bytes, get request body, respond problem,
        // respond JSON, and start calls required by the validation and protocol failures are mapped for presentation
        // operation.

        server.createContext (
            "/api/v1/model",
            exchange ->
            {
                // Handle the branch where exchange request method matches "put".

                if ( exchange.getRequestMethod ().equals ( "PUT" ) )
                {
                    // Perform the read all bytes, get request body, and respond problem calls required by the
                    // validation and protocol failures are mapped for presentation operation.

                    exchange.getRequestBody ().readAllBytes ();
                    respondProblem ( exchange, 422, "Remote model validation failed." );
                }

                // Handle the alternative path when the preceding condition is not satisfied.

                else
                {
                    // Complete the validation and protocol failures are mapped for presentation step by calling
                    // respond problem.

                    respondProblem ( exchange, 404, "No active model is available." );
                }
            }
        );
        server.createContext (
            "/api/v1/evaluations",
            exchange ->
            {
                // Perform the read all bytes, get request body, and respond JSON calls required by the validation and
                // protocol failures are mapped for presentation operation.

                exchange.getRequestBody ().readAllBytes ();
                respondJson ( exchange, 200, "{}" );
            }
        );
        server.start ();

        try
        {
            // Prepare the settings and simulation request values needed by the validation and protocol failures are
            // mapped for presentation operation.

            ClientConnectionSettings settings = new ClientConnectionSettings (
                URI.create ( "http://127.0.0.1:" + server.getAddress ().getPort () + "/" ),
                Duration.ofSeconds ( 2 ),
                Duration.ofSeconds ( 2 ),
                ""
            );
            SimulationRequest simulationRequest = simulator.createOccurrence (
                content,
                "event-cancel-order",
                Map.of ()
            );

            // Verify the validation and protocol failures are mapped for presentation scenario against its expected
            // outcome.

            assertRemoteFailure (
                gateway.pushModelWithRevision ( settings, content ),
                ClientRemoteException.Kind.VALIDATION
            );
            assertRemoteFailure (
                gateway.pullModelWithRevision ( settings ),
                ClientRemoteException.Kind.NO_MODEL
            );
            assertRemoteFailure (
                gateway.evaluateOccurrence ( settings, simulationRequest ),
                ClientRemoteException.Kind.PROTOCOL
            );
        }

        // Complete the required cleanup regardless of how the protected operation finishes.

        finally
        {
            // Stop the server and release its resources.

            server.stop ( 0 );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: timeoutAndConnectionFailuresAreMappedForPresentation
    //
    // Description:
    //
    //   Performs the timeout and connection failures are mapped for presentation operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void timeoutAndConnectionFailuresAreMappedForPresentation () throws Exception
    {
        // Prepare the gateway and server values needed by the timeout and connection failures are mapped for
        // presentation operation.

        ClientServerGateway gateway = new ClientServerGateway (
            new AuthoringModelClientDocumentCodec ()
        );
        HttpServer server = HttpServer.create ( new InetSocketAddress ( "127.0.0.1", 0 ), 0 );

        // Perform the create context, sleep, respond JSON, interrupt, current thread, close, and start calls required
        // by the timeout and connection failures are mapped for presentation operation.

        server.createContext (
            "/api/v1/health/live",
            exchange ->
            {
                try
                {
                    // Apply sleep and respond JSON to the thread for the timeout and connection failures are mapped
                    // for presentation operation.

                    Thread.sleep ( 250 );
                    respondJson ( exchange, 200, "{\"status\":\"UP\"}" );
                }

                // Handle interrupted failures captured as exception.

                catch ( InterruptedException exception )
                {
                    // Perform the interrupt, current thread, and close calls required by the timeout and connection
                    // failures are mapped for presentation operation.

                    Thread.currentThread ().interrupt ();
                    exchange.close ();
                }

                // Handle I/O failures captured as exception.

                catch ( IOException exception )
                {
                    // Stop the exchange and release its resources.

                    exchange.close ();
                }
            }
        );
        server.start ();

        // Prepare the port and timeout settings values needed by the timeout and connection failures are mapped for
        // presentation operation.

        int port = server.getAddress ().getPort ();
        ClientConnectionSettings timeoutSettings = new ClientConnectionSettings (
            URI.create ( "http://127.0.0.1:" + port + "/" ),
            Duration.ofSeconds ( 1 ),
            Duration.ofMillis ( 50 ),
            ""
        );

        // Verify the timeout and connection failures are mapped for presentation scenario against its expected
        // outcome.

        assertRemoteFailure (
            gateway.testConnection ( timeoutSettings ),
            ClientRemoteException.Kind.TIMEOUT
        );

        server.stop ( 0 );

        // Initialize the stopped server settings by applying create, of millis, and of seconds.

        ClientConnectionSettings stoppedServerSettings = new ClientConnectionSettings (
            URI.create ( "http://127.0.0.1:" + port + "/" ),
            Duration.ofMillis ( 250 ),
            Duration.ofSeconds ( 1 ),
            ""
        );

        // Verify the timeout and connection failures are mapped for presentation scenario against its expected
        // outcome.

        assertRemoteFailure (
            gateway.testConnection ( stoppedServerSettings ),
            ClientRemoteException.Kind.CONNECTION
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: failedAndCancelledPhaseEightOperationsLeaveTheSessionUnchanged
    //
    // Description:
    //
    //   Performs the failed and cancelled phase eight operations leave the session unchanged operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void failedAndCancelledPhaseEightOperationsLeaveTheSessionUnchanged ()
    {
        // Prepare the original session, original content, view, server operations, and presenter values needed by the
        // failed and cancelled workflow eight operations leave the session unchanged operation.

        ClientDocumentSession originalSession = ClientDocumentSession.pulled ( courierContent () );
        ClientDocumentSession.Content originalContent = originalSession.getContent ();
        FakeView view = new FakeView ();
        FakeServerOperations serverOperations = new FakeServerOperations ();
        ClientPresenter presenter = new ClientPresenter (
            view,
            new FakeDocumentOperations (),
            serverOperations,
            originalSession,
            ClientConnectionSettings.defaults ()
        );

        // Update the server operations pull future from the failed future result.

        serverOperations.pullFuture = CompletableFuture.failedFuture (
            new ClientRemoteException (
                ClientRemoteException.Kind.CONNECTION,
                "Connection failed."
            )
        );

        // Verify the failed and cancelled workflow eight operations leave the session unchanged scenario against its
        // expected outcome.

        presenter.requestPull ();

        assertThat ( presenter.getSession () ).isSameAs ( originalSession );
        assertThat ( presenter.getSession ().getContent () ).isSameAs ( originalContent );
        assertThat ( view.lastError ).contains ( "CONNECTION" );

        // Update the server operations push future from the failed future result.

        serverOperations.pushFuture = CompletableFuture.failedFuture (
            new ClientRemoteException (
                ClientRemoteException.Kind.VALIDATION,
                "Remote validation failed."
            )
        );

        // Verify the failed and cancelled workflow eight operations leave the session unchanged scenario against its
        // expected outcome.

        presenter.requestPush ();

        assertThat ( presenter.getSession ().getContent () ).isSameAs ( originalContent );

        // Construct the completable future instance required by the failed and cancelled workflow eight operations
        // leave the session unchanged operation.

        serverOperations.evaluationFuture = new CompletableFuture <ClientEvaluationResult> ();

        // Verify the failed and cancelled workflow eight operations leave the session unchanged scenario against its
        // expected outcome.

        presenter.requestEvaluate (
            "event-cancel-order",
            Map.of ( "parameter-order-reference", PayloadValueDraft.omitted () )
        );
        presenter.cancelBackgroundOperation ();

        assertThat ( serverOperations.evaluationFuture.isCancelled () ).isTrue ();
        assertThat ( presenter.getSession ().getContent () ).isSameAs ( originalContent );
        assertThat ( view.lastStatus ).isEqualTo ( "Operation cancelled" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: ordinaryRemoteOperationsPreserveConnectionStatusUntilTheirOutcomeIsKnown
    //
    // Description:
    //
    //   Verifies pull, push, and evaluate activity does not replace the last connection result with a transient state.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void ordinaryRemoteOperationsPreserveConnectionStatusUntilTheirOutcomeIsKnown ()
    {
        FakeView view = new FakeView ();
        FakeServerOperations serverOperations = new FakeServerOperations ();
        ClientPresenter presenter = new ClientPresenter (
            view,
            new FakeDocumentOperations (),
            serverOperations,
            ClientDocumentSession.pulled ( courierContent () ),
            ClientConnectionSettings.defaults ()
        );

        presenter.start ();
        view.connectionStatuses.clear ();

        presenter.requestTestConnection ();

        assertThat ( view.connectionStatuses ).containsExactly ( "Connecting", "Connected" );
        assertThat ( view.lastConnectionStatus ).isEqualTo ( "Connected" );

        view.connectionStatuses.clear ();
        serverOperations.pullFuture = new CompletableFuture <ClientDocumentSession.Content> ();
        presenter.requestPull ();

        assertThat ( view.connectionStatuses ).isEmpty ();

        presenter.cancelBackgroundOperation ();

        assertThat ( view.connectionStatuses ).isEmpty ();
        assertThat ( view.lastConnectionStatus ).isEqualTo ( "Connected" );

        serverOperations.pushFuture = new CompletableFuture <String> ();
        presenter.requestPush ();

        assertThat ( view.connectionStatuses ).isEmpty ();

        presenter.cancelBackgroundOperation ();

        assertThat ( view.connectionStatuses ).isEmpty ();
        assertThat ( view.lastConnectionStatus ).isEqualTo ( "Connected" );

        serverOperations.evaluationFuture = new CompletableFuture <ClientEvaluationResult> ();
        presenter.requestEvaluate (
            "event-cancel-order",
            Map.of ( "parameter-order-reference", PayloadValueDraft.omitted () )
        );

        assertThat ( view.connectionStatuses ).isEmpty ();

        presenter.cancelBackgroundOperation ();

        assertThat ( view.connectionStatuses ).isEmpty ();
        assertThat ( view.lastConnectionStatus ).isEqualTo ( "Connected" );

        serverOperations.testConnectionFuture = new CompletableFuture <String> ();
        presenter.requestTestConnection ();

        assertThat ( view.connectionStatuses ).containsExactly ( "Connecting" );

        presenter.cancelBackgroundOperation ();

        assertThat ( view.connectionStatuses ).containsExactly ( "Connecting", "Connected" );
        assertThat ( view.lastConnectionStatus ).isEqualTo ( "Connected" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: remoteFailureConnectionStatusReflectsServerReachability
    //
    // Description:
    //
    //   Verifies server replies remain connected while transport and protocol failures become disconnected.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void remoteFailureConnectionStatusReflectsServerReachability ()
    {
        for ( ClientRemoteException.Kind failureKind : ClientRemoteException.Kind.values () )
        {
            FakeView view = new FakeView ();
            FakeServerOperations serverOperations = new FakeServerOperations ();
            ClientPresenter presenter = new ClientPresenter (
                view,
                new FakeDocumentOperations (),
                serverOperations,
                ClientDocumentSession.pulled ( courierContent () ),
                ClientConnectionSettings.defaults ()
            );

            presenter.start ();
            view.connectionStatuses.clear ();

            serverOperations.pullFuture = CompletableFuture.failedFuture (
                new ClientRemoteException ( failureKind, "Remote operation failed." )
            );

            presenter.requestPull ();

            String expectedConnectionStatus =
                failureKind == ClientRemoteException.Kind.CONNECTION
                    || failureKind == ClientRemoteException.Kind.TIMEOUT
                    || failureKind == ClientRemoteException.Kind.PROTOCOL
                        ? "Disconnected"
                        : "Connected";

            assertThat ( view.connectionStatuses )
                .as ( "connection transitions for %s", failureKind )
                .doesNotContain ( "Connecting" );
            assertThat ( view.lastConnectionStatus )
                .as ( "connection result for %s", failureKind )
                .isEqualTo ( expectedConnectionStatus );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: startServer
    //
    // Description:
    //
    //   Performs the start server operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private EcaHttpServer startServer ()
    {
        // Return the result produced by start.

        return EcaHttpServer.start (
            new ServerConfiguration (
                "127.0.0.1",
                0,
                this.temporaryDirectory,
                null,
                null,
                16 * 1024 * 1024,
                1024 * 1024,
                32 * 1024,
                Duration.ofSeconds ( 5 ),
                Duration.ofSeconds ( 5 )
            )
        );
    }

    //=================================================================================================================
    // Mutators
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: settings
    //
    // Description:
    //
    //   Sets the tings.
    //
    // Arguments:
    //
    //   server (EcaHttpServer):
    //     The server to use.
    //
    //   token (String):
    //     The token to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static ClientConnectionSettings settings ( EcaHttpServer server, String token )
    {
        // Return a newly constructed client connection settings containing the operation result.

        return new ClientConnectionSettings (
            URI.create ( "http://127.0.0.1:" + server.getPort () + "/" ),
            Duration.ofSeconds ( 2 ),
            Duration.ofSeconds ( 5 ),
            token
        );
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: courierContent
    //
    // Description:
    //
    //   Performs the courier content operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static ClientDocumentSession.Content courierContent ()
    {
        // Initialize the project root by applying of and get property.

        Path projectRoot = Path.of ( System.getProperty ( "eca.project.root" ) );

        try
        {
            // Return the result produced by read.

            return new AuthoringModelClientDocumentCodec ().read (
                Files.readAllBytes ( projectRoot.resolve ( "examples" ).resolve ( "eca-rule-engine-example.json" ) )
            );
        }

        // Handle I/O failures captured as exception.

        catch ( IOException exception )
        {
            throw new IllegalStateException ( "The accepted courier fixture could not be read.", exception );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: assertRemoteFailure
    //
    // Description:
    //
    //   Performs the assert remote failure operation.
    //
    // Arguments:
    //
    //   operation (CompletableFuture <?>):
    //     The operation to use.
    //
    //   expectedKind (ClientRemoteException.Kind):
    //     The expected kind to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void assertRemoteFailure (
        CompletableFuture <?> operation,
        ClientRemoteException.Kind expectedKind
    )
    {
        try
        {
            // Retrieve the completed value from the asynchronous operation.

            operation.get ( 5, TimeUnit.SECONDS );
            throw new AssertionError ( "The server operation unexpectedly succeeded." );
        }

        // Handle exception failures captured as exception.

        catch ( Exception exception )
        {
            // Verify the assert remote failure scenario against its expected outcome.

            assertThat ( exception.getCause () ).isInstanceOf ( ClientRemoteException.class );
            assertThat ( ( (ClientRemoteException) exception.getCause () ).getKind () )
                .isEqualTo ( expectedKind );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: respondProblem
    //
    // Description:
    //
    //   Performs the respond problem operation.
    //
    // Arguments:
    //
    //   exchange (HttpExchange):
    //     The exchange to use.
    //
    //   statusCode (int):
    //     The status code to use.
    //
    //   detail (String):
    //     The detail to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void respondProblem (
        HttpExchange exchange,
        int statusCode,
        String detail
    ) throws IOException
    {
        // Perform the set, get response headers, and respond calls required by the respond problem operation.

        exchange.getResponseHeaders ().set ( "Content-Type", "application/problem+json" );
        respond (
            exchange,
            statusCode,
            (
                "{\"type\":\"about:blank\",\"title\":\"Rejected\",\"status\":"
                    + statusCode
                    + ",\"detail\":\""
                    + detail
                    + "\"}"
            )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: respondJson
    //
    // Description:
    //
    //   Performs the respond json operation.
    //
    // Arguments:
    //
    //   exchange (HttpExchange):
    //     The exchange to use.
    //
    //   statusCode (int):
    //     The status code to use.
    //
    //   document (String):
    //     The document to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void respondJson (
        HttpExchange exchange,
        int statusCode,
        String document
    ) throws IOException
    {
        // Perform the set, get response headers, and respond calls required by the respond JSON operation.

        exchange.getResponseHeaders ().set ( "Content-Type", "application/json" );
        respond ( exchange, statusCode, document );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: respond
    //
    // Description:
    //
    //   Performs the respond operation.
    //
    // Arguments:
    //
    //   exchange (HttpExchange):
    //     The exchange to use.
    //
    //   statusCode (int):
    //     The status code to use.
    //
    //   document (String):
    //     The document to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void respond (
        HttpExchange exchange,
        int statusCode,
        String document
    ) throws IOException
    {
        // Initialize the response body by applying get bytes.

        byte [] responseBody = document.getBytes ( StandardCharsets.UTF_8 );

        // Perform the send response headers, write, get response body, and close calls required by the respond
        // operation.

        exchange.sendResponseHeaders ( statusCode, responseBody.length );
        exchange.getResponseBody ().write ( responseBody );
        exchange.close ();
    }

    //*****************************************************************************************************************
    // Class: FakeDocumentOperations
    //
    // Description:
    //
    //   Provides the fake document operations behavior.
    //
    //*****************************************************************************************************************

    private static final class FakeDocumentOperations implements ClientDocumentOperations
    {
        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: open
        //
        // Description:
        //
        //   Performs the open operation.
        //
        // Arguments:
        //
        //   path (Path):
        //     The path to use.
        //
        // Returns:
        //
        //   The result of the operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public CompletableFuture <ClientDocumentSession.Content> open ( Path path )
        {
            // Return the result produced by failed future.

            return CompletableFuture.failedFuture ( new UnsupportedOperationException () );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: save
        //
        // Description:
        //
        //   Performs the save operation.
        //
        // Arguments:
        //
        //   path (Path):
        //     The path to use.
        //
        //   content (ClientDocumentSession.Content):
        //     The content to use.
        //
        // Returns:
        //
        //   The result of the operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public CompletableFuture <Void> save (
            Path path,
            ClientDocumentSession.Content content
        )
        {
            // Return the result produced by failed future.

            return CompletableFuture.failedFuture ( new UnsupportedOperationException () );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: close
        //
        // Description:
        //
        //   Performs the close operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public void close ()
        {
        }
    }

    //*****************************************************************************************************************
    // Class: FakeServerOperations
    //
    // Description:
    //
    //   Provides the fake server operations behavior.
    //
    //*****************************************************************************************************************

    private static final class FakeServerOperations implements ClientServerOperations
    {
        private CompletableFuture <ClientDocumentSession.Content> pullFuture =
            CompletableFuture.completedFuture ( courierContent () );

        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private CompletableFuture <String> testConnectionFuture =
            CompletableFuture.completedFuture ( "Connected" );
        private CompletableFuture <String> pushFuture = CompletableFuture.completedFuture ( "Accepted" );
        private CompletableFuture <ClientEvaluationResult> evaluationFuture =
            CompletableFuture.failedFuture ( new UnsupportedOperationException () );

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
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
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public CompletableFuture <String> testConnection ( ClientConnectionSettings settings )
        {
            // Return the test connection future to the caller.

            return this.testConnectionFuture;
        }

        //-------------------------------------------------------------------------------------------------------------
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
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public CompletableFuture <ClientDocumentSession.Content> pullModel (
            ClientConnectionSettings settings
        )
        {
            // Return the pull future to the caller.

            return this.pullFuture;
        }

        //-------------------------------------------------------------------------------------------------------------
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
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public CompletableFuture <String> pushModel (
            ClientConnectionSettings settings,
            ClientDocumentSession.Content content
        )
        {
            // Return the push future to the caller.

            return this.pushFuture;
        }

        //-------------------------------------------------------------------------------------------------------------
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
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public CompletableFuture <ClientEvaluationResult> evaluateOccurrence (
            ClientConnectionSettings settings,
            SimulationRequest simulationRequest
        )
        {
            // Return the evaluation future to the caller.

            return this.evaluationFuture;
        }
    }

    //*****************************************************************************************************************
    // Class: FakeView
    //
    // Description:
    //
    //   Provides the fake view behavior.
    //
    //*****************************************************************************************************************

    private static final class FakeView implements ClientView
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final List <String> connectionStatuses = new ArrayList <> ();

        private String lastStatus           = "";
        private String lastError            = "";
        private String lastConnectionStatus = "";

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: dispatch
        //
        // Description:
        //
        //   Performs the dispatch operation.
        //
        // Arguments:
        //
        //   runnable (Runnable):
        //     The runnable to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public void dispatch ( Runnable runnable )
        {
            // Execute the prepared callback.

            runnable.run ();
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: render
        //
        // Description:
        //
        //   Performs the render operation.
        //
        // Arguments:
        //
        //   session (ClientDocumentSession):
        //     The session to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public void render ( ClientDocumentSession session )
        {
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: reportValidation
        //
        // Description:
        //
        //   Performs the report validation operation.
        //
        // Arguments:
        //
        //   session (ClientDocumentSession):
        //     The session to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public void reportValidation ( ClientDocumentSession session )
        {
        }

        //=============================================================================================================
        // Mutators
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: setBackgroundOperation
        //
        // Description:
        //
        //   Sets the background operation.
        //
        // Arguments:
        //
        //   running (boolean):
        //     The running to use.
        //
        //   description (String):
        //     The description to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public void setBackgroundOperation ( boolean running, String description )
        {
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: setStatus
        //
        // Description:
        //
        //   Sets the status.
        //
        // Arguments:
        //
        //   status (String):
        //     The status to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public void setStatus ( String status )
        {
            this.lastStatus = status;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: setConnectionStatus
        //
        // Description:
        //
        //   Sets the connection status.
        //
        // Arguments:
        //
        //   status (String):
        //     The status to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public void setConnectionStatus ( String status )
        {
            this.lastConnectionStatus = status;
            this.connectionStatuses.add ( status );
        }

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: reportMessage
        //
        // Description:
        //
        //   Performs the report message operation.
        //
        // Arguments:
        //
        //   severity (MessageSeverity):
        //     The severity to use.
        //
        //   source (String):
        //     The source to use.
        //
        //   message (String):
        //     The message to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public void reportMessage (
            MessageSeverity severity,
            String source,
            String message
        )
        {
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: reportError
        //
        // Description:
        //
        //   Performs the report error operation.
        //
        // Arguments:
        //
        //   title (String):
        //     The title to use.
        //
        //   message (String):
        //     The message to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public void reportError ( String title, String message )
        {
            this.lastError = title + ": " + message;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: chooseDirtyDocumentDecision
        //
        // Description:
        //
        //   Chooses how to handle unsaved changes before replacing the current document.
        //
        // Arguments:
        //
        //   actionName (String):
        //     The action name to use.
        //
        //   canSave (boolean):
        //     Indicates whether saving before continuing is currently available.
        //
        // Returns:
        //
        //   The result of the operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public DirtyDocumentDecision chooseDirtyDocumentDecision ( String actionName, boolean canSave )
        {
            // Return the discard-and-continue decision for this outcome.

            return DirtyDocumentDecision.DISCARD_AND_CONTINUE;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: chooseOpenPath
        //
        // Description:
        //
        //   Performs the choose open path operation.
        //
        // Returns:
        //
        //   The result of the operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public Optional <Path> chooseOpenPath ()
        {
            // Return an empty optional result because no value is available.

            return Optional.empty ();
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: chooseSavePath
        //
        // Description:
        //
        //   Performs the choose save path operation.
        //
        // Arguments:
        //
        //   currentPath (Optional <Path>):
        //     The current path to use.
        //
        // Returns:
        //
        //   The result of the operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public Optional <Path> chooseSavePath ( Optional <Path> currentPath )
        {
            // Return an empty optional result because no value is available.

            return Optional.empty ();
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: editSettings
        //
        // Description:
        //
        //   Performs the edit settings operation.
        //
        // Arguments:
        //
        //   currentSettings (ClientConnectionSettings):
        //     The current settings to use.
        //
        // Returns:
        //
        //   The result of the operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public Optional <ClientConnectionSettings> editSettings (
            ClientConnectionSettings currentSettings
        )
        {
            // Return an empty optional result because no value is available.

            return Optional.empty ();
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: exitApplication
        //
        // Description:
        //
        //   Performs the exit application operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public void exitApplication ()
        {
        }
    }
}
