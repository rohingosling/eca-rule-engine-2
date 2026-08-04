//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies the desktop document session, presenter, asynchronous adapters, and preference-safety rules.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.rohingosling.eca.application.ClientDocumentSession;
import com.rohingosling.eca.application.ClientModelEditor.EditResult;
import com.rohingosling.eca.application.ClientModelEditor.EntityDraft;
import com.rohingosling.eca.client.ClientConnectionSettings;
import com.rohingosling.eca.client.ClientDocumentFileService;
import com.rohingosling.eca.client.ClientDocumentOperations;
import com.rohingosling.eca.client.ClientMain;
import com.rohingosling.eca.client.ClientPreferencesStore;
import com.rohingosling.eca.client.ClientPresenter;
import com.rohingosling.eca.client.ClientServerGateway;
import com.rohingosling.eca.client.ClientServerOperations;
import com.rohingosling.eca.client.ClientUserGuide;
import com.rohingosling.eca.client.ClientView;
import com.rohingosling.eca.client.ClientView.MessageSeverity;
import com.rohingosling.eca.json.AuthoringModelClientDocumentCodec;
import com.rohingosling.eca.model.AuthoringModel;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

//*********************************************************************************************************************
// Class: ClientPhaseSixTest
//
// Description:
//
//   Verifies the desktop document session, presenter, asynchronous adapters, and preference-safety rules.
//
//*********************************************************************************************************************

final class ClientPhaseSixTest
{
    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: newDocumentTemplate_isImmediatelyValid
    //
    // Description:
    //
    //   Verifies that new document template is immediately valid.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void newDocumentTemplate_isImmediatelyValid ()
    {
        // Initialize the session with a new client document session.

        ClientDocumentSession session = new ClientDocumentSession ();

        // Verify the new document template is immediately valid scenario against its expected outcome.

        assertThat ( session.getContent ().getSummary ().getDescription () )
            .isEqualTo ( "New ECA model." );
        assertThat ( session.getContent ().getValidationMessages () ).isEmpty ();
        assertThat ( session.canPush () ).isTrue ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: startup_reportsReadyWithoutAnnouncingTemplateValidation
    //
    // Description:
    //
    //   Verifies that startup reports ready without announcing template validation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void startup_reportsReadyWithoutAnnouncingTemplateValidation ()
    {
        // Prepare the view, session, and presenter values needed by the startup reports ready without announcing
        // template validation operation.

        FakeView view = new FakeView ();
        ClientDocumentSession session = new ClientDocumentSession ();
        ClientPresenter presenter = presenter (
            view,
            new FakeDocumentOperations (),
            new FakeServerOperations (),
            session
        );

        // Verify the startup reports ready without announcing template validation scenario against its expected
        // outcome.

        presenter.start ();

        assertThat ( view.lastStatus ).isEqualTo ( "Ready" );
        assertThat ( view.validationReportCount ).isZero ();

        presenter.requestValidate ();

        assertThat ( view.validationReportCount ).isOne ();
        assertThat ( view.lastValidationSession ).isSameAs ( session );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: modelRootSelection_isDistinctFromCategoryAndEntitySelections
    //
    // Description:
    //
    //   Verifies that model root selection is distinct from category and entity selections.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void modelRootSelection_isDistinctFromCategoryAndEntitySelections ()
    {
        // Initialize the session with a new client document session.

        ClientDocumentSession session = new ClientDocumentSession ();

        // Verify the model root selection is distinct from category and entity selections scenario against its
        // expected outcome.

        assertThat ( session.getSelection ().getKind () )
            .isEqualTo ( ClientDocumentSession.Selection.Kind.MODEL );
        assertThat ( session.getSelection ().getCategoryIdentifier () ).isEmpty ();
        assertThat ( session.getSelection ().getEntityIdentifier () ).isEmpty ();

        session.selectCategory ( "parameters" );

        assertThat ( session.getSelection ().getKind () )
            .isEqualTo ( ClientDocumentSession.Selection.Kind.CATEGORY );

        session.selectEntity ( "parameters", "parameter-id" );

        assertThat ( session.getSelection ().getKind () )
            .isEqualTo ( ClientDocumentSession.Selection.Kind.ENTITY );

        session.selectModel ();

        assertThat ( session.getSelection ().getKind () )
            .isEqualTo ( ClientDocumentSession.Selection.Kind.MODEL );
        assertThat ( session.getSelection ().getCategoryIdentifier () ).isEmpty ();
        assertThat ( session.getSelection ().getEntityIdentifier () ).isEmpty ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: documentSession_tracksTitlesPathsDirtyStateSelectionAndHistory
    //
    // Description:
    //
    //   Verifies that document session tracks titles paths dirty state selection and history.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void documentSession_tracksTitlesPathsDirtyStateSelectionAndHistory ()
    {
        // Initialize the untitled session with a new client document session.

        ClientDocumentSession untitledSession = new ClientDocumentSession ();

        // Verify the document session tracks titles paths dirty state selection and history scenario against its
        // expected outcome.

        assertThat ( untitledSession.getWindowTitle () )
            .isEqualTo ( "ECA Rule Engine Laboratory (Version 2.0.0 - Untitled)" );
        assertThat ( untitledSession.isDirty () ).isFalse ();
        assertThat ( untitledSession.canUndo () ).isFalse ();

        // Prepare the model path and named session values needed by the document session tracks titles paths dirty
        // state selection and history operation.

        Path modelPath = Path.of ( "eca-rule-engine-example.json" );
        ClientDocumentSession namedSession = ClientDocumentSession.loaded (
            content ( "eca-rule-engine-example", "ECA Rule Engine Example", "1.0" ),
            modelPath
        );

        // Verify the document session tracks titles paths dirty state selection and history scenario against its
        // expected outcome.

        assertThat ( namedSession.getWindowTitle () )
            .isEqualTo ( "ECA Rule Engine Laboratory (Version 2.0.0 - eca-rule-engine-example.json)" );

        namedSession.applyEdit ( content ( "eca-rule-engine-example", "Changed Test Model", "1.0" ) );
        namedSession.selectEntity ( "rules", "rule-courier" );

        assertThat ( namedSession.isDirty () ).isTrue ();
        assertThat ( namedSession.getWindowTitle () )
            .isEqualTo ( "ECA Rule Engine Laboratory (Version 2.0.0 - eca-rule-engine-example.json *)" );
        assertThat ( namedSession.canUndo () ).isTrue ();
        assertThat ( namedSession.getSelection ().getCategoryIdentifier () ).contains ( "rules" );
        assertThat ( namedSession.getSelection ().getEntityIdentifier () ).contains ( "rule-courier" );

        namedSession.undo ();

        assertThat ( namedSession.isDirty () ).isFalse ();
        assertThat ( namedSession.canRedo () ).isTrue ();

        namedSession.redo ();

        assertThat ( namedSession.isDirty () ).isTrue ();

        namedSession.markSaved ( Path.of ( "changed-model.json" ) );

        assertThat ( namedSession.isDirty () ).isFalse ();
        assertThat ( namedSession.getDisplayFileName () ).isEqualTo ( "changed-model.json" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: semanticErrors_remainRepairableButPreventPush
    //
    // Description:
    //
    //   Verifies that semantic errors remain repairable but prevent push.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void semanticErrors_remainRepairableButPreventPush ()
    {
        // Initialize the session by applying pulled and content.

        ClientDocumentSession session = ClientDocumentSession.pulled (
            content ( "repair-model", "Repair Model", "unsupported" )
        );

        // Verify the semantic errors remain repairable but prevent push scenario against its expected outcome.

        assertThat ( session.getContent ().getValidationMessages () ).isNotEmpty ();
        assertThat ( session.canPush () ).isFalse ();

        session.applyEdit ( courierContent () );

        assertThat ( session.isDirty () ).isTrue ();
        assertThat ( session.canPush () ).isTrue ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: rejectedEntityDraft_isReportedThroughTheUnifiedMessageChannel
    //
    // Description:
    //
    //   Verifies that rejected entity draft is reported through the unified message channel.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void rejectedEntityDraft_isReportedThroughTheUnifiedMessageChannel ()
    {
        // Prepare the view, session, presenter, invalid draft, and result values needed by the rejected entity draft
        // is reported through the unified message channel operation.

        FakeView view = new FakeView ();
        ClientDocumentSession session = new ClientDocumentSession ();
        ClientPresenter presenter = presenter (
            view,
            new FakeDocumentOperations (),
            new FakeServerOperations (),
            session
        );
        EntityDraft invalidDraft = EntityDraft.parameter (
            "",
            "",
            "",
            "ENUM",
            List.of ()
        );

        EditResult result = presenter.requestApplyEntity ( "parameters", null, invalidDraft );

        // Verify the rejected entity draft is reported through the unified message channel scenario against its
        // expected outcome.

        assertThat ( result.isSuccessful () ).isFalse ();
        assertThat ( session.getContent ().getSummary ().getParameterCount () ).isZero ();
        assertThat ( view.lastMessageSeverity ).isEqualTo ( MessageSeverity.ERROR );
        assertThat ( view.lastMessageSource ).isEqualTo ( "Entity Editor" );
        assertThat ( view.lastMessage )
            .contains ( "Rejected new entity in parameters" )
            .contains ( "enumValues" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: saveAndOpen_useWorkerThreadAndAtomicReplacement
    //
    // Description:
    //
    //   Verifies that save and open use worker thread and atomic replacement.
    //
    // Arguments:
    //
    //   temporaryDirectory (Path):
    //     The temporary directory to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void saveAndOpen_useWorkerThreadAndAtomicReplacement ( @TempDir Path temporaryDirectory )
        throws Exception
    {
        // Prepare the worker thread name, executor service, file service, model path, and expected content values
        // needed by the save and open use worker thread and atomic replacement operation.

        AtomicReference <String> workerThreadName = new AtomicReference <String> ();
        ExecutorService executorService = Executors.newSingleThreadExecutor (
            runnable -> new Thread (
                () ->
                {
                    // Perform the set, get name, current thread, and run calls required by the save and open use
                    // worker thread and atomic replacement operation.

                    workerThreadName.set ( Thread.currentThread ().getName () );
                    runnable.run ();
                },
                "phase-six-file-worker"
            )
        );
        ClientDocumentFileService fileService = new ClientDocumentFileService (
            new AuthoringModelClientDocumentCodec (),
            executorService
        );
        Path modelPath = temporaryDirectory.resolve ( "model.json" );
        ClientDocumentSession.Content expectedContent = content (
            "atomic-model",
            "Atomic Model",
            "1.0"
        );

        try
        {
            // Verify the save and open use worker thread and atomic replacement scenario against its expected outcome.

            fileService.save ( modelPath, expectedContent ).get ( 5, TimeUnit.SECONDS );

            assertThat ( workerThreadName.get () ).isEqualTo ( "phase-six-file-worker" );
            assertThat ( Files.readString ( modelPath, StandardCharsets.UTF_8 ) )
                .contains ( "\"modelId\" : \"atomic-model\"" );

            // Initialize the opened content by applying get and open.

            ClientDocumentSession.Content openedContent = fileService.open ( modelPath )
                .get ( 5, TimeUnit.SECONDS );

            // Verify the save and open use worker thread and atomic replacement scenario against its expected outcome.

            assertThat ( openedContent.getSummary ().getName () ).isEqualTo ( "Atomic Model" );

            // Open the scoped resources for the protected operation and close them automatically afterward.

            try ( Stream <Path> entries = Files.list ( temporaryDirectory ) )
            {
                // Verify the save and open use worker thread and atomic replacement scenario against its expected
                // outcome.

                assertThat ( entries.map ( path -> path.getFileName ().toString () ) )
                    .noneMatch ( fileName -> fileName.endsWith ( ".tmp" ) );
            }
        }

        // Complete the required cleanup regardless of how the protected operation finishes.

        finally
        {
            // Perform the close and shutdown now calls required by the save and open use worker thread and atomic
            // replacement operation.

            fileService.close ();
            executorService.shutdownNow ();
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: saveUsesCurrentPathAndSaveAsChangesPathOnlyAfterSuccess
    //
    // Description:
    //
    //   Performs the save uses current path and save as changes path only after success operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void saveUsesCurrentPathAndSaveAsChangesPathOnlyAfterSuccess ()
    {
        // Prepare the current path, save as path, and session values needed by the save uses current path and save as
        // changes path only after success operation.

        Path currentPath = Path.of ( "current.json" );
        Path saveAsPath = Path.of ( "renamed.json" );
        ClientDocumentSession session = ClientDocumentSession.loaded (
            content ( "save-model", "Save Model", "1.0" ),
            currentPath
        );

        // Apply apply edit and content to the session for the save uses current path and save as changes path only
        // after success operation.

        session.applyEdit ( content ( "save-model", "Save Model 2", "1.0" ) );

        // Prepare the view, document operations, and presenter values needed by the save uses current path and save as
        // changes path only after success operation.

        FakeView view = new FakeView ();
        FakeDocumentOperations documentOperations = new FakeDocumentOperations ();
        ClientPresenter presenter = presenter ( view, documentOperations, new FakeServerOperations (), session );

        // Verify the save uses current path and save as changes path only after success scenario against its expected
        // outcome.

        presenter.requestSave ();

        assertThat ( documentOperations.savedPaths ).containsExactly ( currentPath );
        assertThat ( session.getLocalPath () ).contains ( currentPath );
        assertThat ( session.isDirty () ).isFalse ();

        session.applyEdit ( content ( "save-model", "Save Model 3", "1.0" ) );

        // Update the view save path from the of result.

        view.savePath = Optional.of ( saveAsPath );

        // Verify the save uses current path and save as changes path only after success scenario against its expected
        // outcome.

        presenter.requestSaveAs ();

        assertThat ( documentOperations.savedPaths ).containsExactly ( currentPath, saveAsPath );
        assertThat ( session.getLocalPath () ).contains ( saveAsPath );
        assertThat ( session.isDirty () ).isFalse ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: dirtySession_requiresConfirmationBeforeNewOpenPullCloseAndExit
    //
    // Description:
    //
    //   Verifies that dirty session requires confirmation before new open pull close and exit.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void dirtySession_requiresConfirmationBeforeNewOpenPullCloseAndExit ()
    {
        // Prepare the dirty session and new view values needed by the dirty session requires confirmation before new
        // open pull close and exit operation.

        ClientDocumentSession dirtySession = dirtySession ();

        FakeView newView = new FakeView ();
        newView.discardConfirmed = false;

        // Initialize the new presenter by applying presenter.

        ClientPresenter newPresenter = presenter (
            newView,
            new FakeDocumentOperations (),
            new FakeServerOperations (),
            dirtySession
        );

        // Verify the dirty session requires confirmation before new open pull close and exit scenario against its
        // expected outcome.

        newPresenter.requestNew ();

        assertThat ( newPresenter.getSession () ).isSameAs ( dirtySession );

        // Initialize the open view with a new fake view.

        FakeView openView = new FakeView ();
        openView.discardConfirmed = false;

        // Prepare the open operations and open presenter values needed by the dirty session requires confirmation
        // before new open pull close and exit operation.

        FakeDocumentOperations openOperations = new FakeDocumentOperations ();
        ClientPresenter openPresenter = presenter (
            openView,
            openOperations,
            new FakeServerOperations (),
            dirtySession ()
        );

        // Verify the dirty session requires confirmation before new open pull close and exit scenario against its
        // expected outcome.

        openPresenter.requestOpen ();

        assertThat ( openView.openChooserCount ).isZero ();
        assertThat ( openOperations.openedPaths ).isEmpty ();

        // Initialize the pull view with a new fake view.

        FakeView pullView = new FakeView ();
        pullView.discardConfirmed = false;

        // Prepare the pull operations and pull presenter values needed by the dirty session requires confirmation
        // before new open pull close and exit operation.

        FakeServerOperations pullOperations = new FakeServerOperations ();
        ClientPresenter pullPresenter = presenter (
            pullView,
            new FakeDocumentOperations (),
            pullOperations,
            dirtySession ()
        );

        // Verify the dirty session requires confirmation before new open pull close and exit scenario against its
        // expected outcome.

        pullPresenter.requestPull ();

        assertThat ( pullOperations.pullCount ).isZero ();

        // Initialize the close view with a new fake view.

        FakeView closeView = new FakeView ();
        closeView.discardConfirmed = false;

        // Prepare the close presenter and original close session values needed by the dirty session requires
        // confirmation before new open pull close and exit operation.

        ClientPresenter closePresenter = presenter (
            closeView,
            new FakeDocumentOperations (),
            new FakeServerOperations (),
            dirtySession ()
        );
        ClientDocumentSession originalCloseSession = closePresenter.getSession ();

        // Verify the dirty session requires confirmation before new open pull close and exit scenario against its
        // expected outcome.

        closePresenter.requestClose ();

        assertThat ( closePresenter.getSession () ).isSameAs ( originalCloseSession );

        // Initialize the exit view with a new fake view.

        FakeView exitView = new FakeView ();
        exitView.discardConfirmed = false;

        // Initialize the exit presenter by applying presenter and dirty session.

        ClientPresenter exitPresenter = presenter (
            exitView,
            new FakeDocumentOperations (),
            new FakeServerOperations (),
            dirtySession ()
        );

        // Verify the dirty session requires confirmation before new open pull close and exit scenario against its
        // expected outcome.

        exitPresenter.requestExit ();

        assertThat ( exitView.exitRequested ).isFalse ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: failedOpenAndSaveAs_leavePriorDocumentIntact
    //
    // Description:
    //
    //   Verifies that failed open and save as leave prior document intact.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void failedOpenAndSaveAs_leavePriorDocumentIntact ()
    {
        // Prepare the original session, open view, and open operations values needed by the failed open and save as
        // leave prior document intact operation.

        ClientDocumentSession originalSession = ClientDocumentSession.loaded (
            content ( "original-model", "Original Model", "1.0" ),
            Path.of ( "original.json" )
        );
        FakeView openView = new FakeView ();
        FakeDocumentOperations openOperations = new FakeDocumentOperations ();

        // Complete the failed open and save as leave prior document intact step by calling of.

        openView.openPath = Optional.of ( Path.of ( "broken.json" ) );
        openOperations.openFailure = new IOException ( "broken input" );

        // Initialize the open presenter by applying presenter.

        ClientPresenter openPresenter = presenter (
            openView,
            openOperations,
            new FakeServerOperations (),
            originalSession
        );

        // Verify the failed open and save as leave prior document intact scenario against its expected outcome.

        openPresenter.requestOpen ();

        assertThat ( openPresenter.getSession () ).isSameAs ( originalSession );
        assertThat ( openPresenter.getSession ().getLocalPath () ).contains ( Path.of ( "original.json" ) );
        assertThat ( openView.lastError ).contains ( "broken input" );

        // Initialize the untitled session with a new client document session.

        ClientDocumentSession untitledSession = new ClientDocumentSession ();

        // Apply apply edit and content to the untitled session for the failed open and save as leave prior document
        // intact operation.

        untitledSession.applyEdit ( content ( "untitled-model", "Changed", "1.0" ) );

        // Prepare the save view and save operations values needed by the failed open and save as leave prior document
        // intact operation.

        FakeView saveView = new FakeView ();
        FakeDocumentOperations saveOperations = new FakeDocumentOperations ();

        // Complete the failed open and save as leave prior document intact step by calling of.

        saveView.savePath = Optional.of ( Path.of ( "failed-save.json" ) );
        saveOperations.saveFailure = new IOException ( "disk full" );

        // Initialize the save presenter by applying presenter.

        ClientPresenter savePresenter = presenter (
            saveView,
            saveOperations,
            new FakeServerOperations (),
            untitledSession
        );

        // Verify the failed open and save as leave prior document intact scenario against its expected outcome.

        savePresenter.requestSaveAs ();

        assertThat ( untitledSession.getLocalPath () ).isEmpty ();
        assertThat ( untitledSession.isDirty () ).isTrue ();
        assertThat ( saveView.lastError ).contains ( "disk full" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: presenterDispatchesCompletionAndBlocksInvalidPush
    //
    // Description:
    //
    //   Performs the presenter dispatches completion and blocks invalid push operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void presenterDispatchesCompletionAndBlocksInvalidPush ()
    {
        // Prepare the view, document operations, server operations, invalid session, and presenter values needed by
        // the presenter dispatches completion and blocks invalid push operation.

        FakeView view = new FakeView ();
        FakeDocumentOperations documentOperations = new FakeDocumentOperations ();
        FakeServerOperations serverOperations = new FakeServerOperations ();
        ClientDocumentSession invalidSession = ClientDocumentSession.pulled (
            content ( "invalid-model", "Invalid Model", "unsupported" )
        );
        ClientPresenter presenter = presenter (
            view,
            documentOperations,
            serverOperations,
            invalidSession
        );

        // Verify the presenter dispatches completion and blocks invalid push scenario against its expected outcome.

        presenter.requestPush ();

        assertThat ( serverOperations.pushCount ).isZero ();
        assertThat ( view.lastError ).contains ( "Repair the validation errors" );

        // Perform the of and content calls required by the presenter dispatches completion and blocks invalid push
        // operation.

        view.openPath = Optional.of ( Path.of ( "replacement.json" ) );
        documentOperations.openedContent = content ( "replacement", "Replacement", "1.0" );

        // Verify the presenter dispatches completion and blocks invalid push scenario against its expected outcome.

        presenter.requestOpen ();

        assertThat ( view.dispatchCount ).isEqualTo ( 1 );
        assertThat ( presenter.getSession ().getContent ().getSummary ().getName () )
            .isEqualTo ( "Replacement" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: backgroundOperation_canBeCancelledWithoutReplacingDocument
    //
    // Description:
    //
    //   Verifies that background operation can be cancelled without replacing document.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void backgroundOperation_canBeCancelledWithoutReplacingDocument ()
    {
        // Prepare the original session, view, and document operations values needed by the background operation can be
        // cancelled without replacing document operation.

        ClientDocumentSession originalSession = ClientDocumentSession.loaded (
            content ( "original-model", "Original Model", "1.0" ),
            Path.of ( "original.json" )
        );
        FakeView view = new FakeView ();
        FakeDocumentOperations documentOperations = new FakeDocumentOperations ();

        // Construct the completable future instance required by the background operation can be cancelled without
        // replacing document operation.

        documentOperations.customOpenFuture = new CompletableFuture <ClientDocumentSession.Content> ();

        // Initialize the presenter by applying presenter.

        ClientPresenter presenter = presenter (
            view,
            documentOperations,
            new FakeServerOperations (),
            originalSession
        );

        // Verify the background operation can be cancelled without replacing document scenario against its expected
        // outcome.

        presenter.requestOpen ();

        assertThat ( presenter.isBackgroundOperationRunning () ).isTrue ();

        presenter.requestNew ();

        assertThat ( presenter.getSession () ).isSameAs ( originalSession );

        presenter.cancelBackgroundOperation ();

        assertThat ( documentOperations.customOpenFuture.isCancelled () ).isTrue ();
        assertThat ( presenter.isBackgroundOperationRunning () ).isFalse ();
        assertThat ( presenter.getSession () ).isSameAs ( originalSession );
        assertThat ( view.lastStatus ).isEqualTo ( "Operation cancelled" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: serverGatewayIsAsynchronousAndInteroperatesWithVersionedEndpoints
    //
    // Description:
    //
    //   Performs the server gateway is asynchronous and interoperates with versioned endpoints operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void serverGatewayIsAsynchronousAndInteroperatesWithVersionedEndpoints ()
        throws Exception
    {
        // Prepare the codec, hosted content, hosted document, liveness entered, release liveness, HTTP server, and
        // server executor values needed by the server gateway is asynchronous and interoperates with versioned
        // endpoints operation.

        AuthoringModelClientDocumentCodec codec = new AuthoringModelClientDocumentCodec ();
        ClientDocumentSession.Content hostedContent = content ( "hosted-model", "Hosted Model", "1.0" );
        byte [] hostedDocument = codec.write ( hostedContent );
        CountDownLatch livenessEntered = new CountDownLatch ( 1 );
        CountDownLatch releaseLiveness = new CountDownLatch ( 1 );
        HttpServer httpServer = HttpServer.create ( new InetSocketAddress ( "127.0.0.1", 0 ), 0 );
        ExecutorService serverExecutor = Executors.newSingleThreadExecutor (
            new NamedThreadFactory ( "phase-six-http-server" )
        );

        // Perform the set executor, create context, count down, await, respond, get bytes, equals, get request method,
        // read all bytes, get request body, and start calls required by the server gateway is asynchronous and
        // interoperates with versioned endpoints operation.

        httpServer.setExecutor ( serverExecutor );
        httpServer.createContext (
            "/api/v1/health/live",
            exchange ->
            {
                // Perform the count down, await, respond, and get bytes calls required by the server gateway is
                // asynchronous and interoperates with versioned endpoints operation.

                livenessEntered.countDown ();
                await ( releaseLiveness );
                respond ( exchange, 200, "{\"status\":\"UP\"}".getBytes ( StandardCharsets.UTF_8 ) );
            }
        );
        httpServer.createContext (
            "/api/v1/model",
            exchange ->
            {
                // Handle the branch where "get" matches exchange request method.

                if ( "GET".equals ( exchange.getRequestMethod () ) )
                {
                    // Complete the server gateway is asynchronous and interoperates with versioned endpoints step by
                    // calling respond.

                    respond ( exchange, 200, hostedDocument );
                }

                // Handle the alternative path when the preceding condition is not satisfied.

                else
                {
                    // Perform the read all bytes, get request body, respond, and get bytes calls required by the
                    // server gateway is asynchronous and interoperates with versioned endpoints operation.

                    exchange.getRequestBody ().readAllBytes ();
                    respond ( exchange, 200, "{}".getBytes ( StandardCharsets.UTF_8 ) );
                }
            }
        );
        httpServer.start ();

        try
        {
            // Prepare the settings, gateway, and liveness values needed by the server gateway is asynchronous and
            // interoperates with versioned endpoints operation.

            ClientConnectionSettings settings = new ClientConnectionSettings (
                URI.create ( "http://127.0.0.1:" + httpServer.getAddress ().getPort () + "/" ),
                Duration.ofSeconds ( 2 ),
                Duration.ofSeconds ( 5 ),
                "session-only-token"
            );
            ClientServerGateway gateway = new ClientServerGateway ( codec );
            CompletableFuture <String> liveness = gateway.testConnection ( settings );

            // Verify the server gateway is asynchronous and interoperates with versioned endpoints scenario against
            // its expected outcome.

            assertThat ( livenessEntered.await ( 5, TimeUnit.SECONDS ) ).isTrue ();
            assertThat ( liveness.isDone () ).isFalse ();

            releaseLiveness.countDown ();

            assertThat ( liveness.get ( 5, TimeUnit.SECONDS ) ).isEqualTo ( "Connected (HTTP 200)" );
            assertThat (
                gateway.pullModel ( settings ).get ( 5, TimeUnit.SECONDS ).getSummary ().getModelId ()
            ).isEqualTo ( "hosted-model" );
            assertThat ( gateway.pushModel ( settings, hostedContent ).get ( 5, TimeUnit.SECONDS ) )
                .isEqualTo ( "Model accepted (HTTP 200)" );
        }

        // Complete the required cleanup regardless of how the protected operation finishes.

        finally
        {
            // Perform the stop and shutdown now calls required by the server gateway is asynchronous and interoperates
            // with versioned endpoints operation.

            httpServer.stop ( 0 );
            serverExecutor.shutdownNow ();
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: preferenceValues_restoreWithinSafeBounds
    //
    // Description:
    //
    //   Verifies that preference values restore within safe bounds.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void preferenceValues_restoreWithinSafeBounds ()
    {
        // Verify the preference values restore within safe bounds scenario against its expected outcome.

        assertThat ( ClientPreferencesStore.sanitizeDividerPosition ( 0.33 ) ).isEqualTo ( 0.33 );
        assertThat ( ClientPreferencesStore.sanitizeDividerPosition ( -4.0 ) ).isEqualTo ( 0.24 );
        assertThat ( ClientPreferencesStore.sanitizeDividerPosition ( Double.NaN ) ).isEqualTo ( 0.24 );
        assertThat ( ClientPreferencesStore.sanitizeUserGuideDividerPosition ( 0.74 ) ).isEqualTo ( 0.74 );
        assertThat ( ClientPreferencesStore.sanitizeUserGuideDividerPosition ( 0.20 ) ).isEqualTo ( 0.70 );
        assertThat ( ClientPreferencesStore.sanitizeUserGuideDividerPosition ( Double.NaN ) ).isEqualTo ( 0.70 );
        assertThat ( ClientPreferencesStore.sanitizeDiagnosticsDividerPosition ( 0.70 ) ).isEqualTo ( 0.70 );
        assertThat ( ClientPreferencesStore.sanitizeDiagnosticsDividerPosition ( 0.10 ) ).isEqualTo ( 0.72 );
        assertThat ( ClientPreferencesStore.sanitizeDiagnosticsDividerPosition ( Double.NaN ) ).isEqualTo ( 0.72 );
        assertThat ( ClientPreferencesStore.sanitizeDimension ( 1_280.0, 760.0, 4_000.0 ) )
            .isEqualTo ( 1_280.0 );
        assertThat ( ClientPreferencesStore.sanitizeDimension ( 20.0, 760.0, 4_000.0 ) )
            .isEqualTo ( 760.0 );
        assertThat ( ClientPreferencesStore.sanitizeDimension ( Double.POSITIVE_INFINITY, 760.0, 4_000.0 ) )
            .isEqualTo ( 760.0 );
        assertThat ( ClientPreferencesStore.sanitizeThemeIdentifier ( "dark" ) ).isEqualTo ( "dark" );
        assertThat ( ClientPreferencesStore.sanitizeThemeIdentifier ( "light" ) ).isEqualTo ( "light" );
        assertThat ( ClientPreferencesStore.sanitizeThemeIdentifier ( "unsupported" ) ).isEqualTo ( "light" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: userGuide_coversEveryDetailContextWithLocalizedTextAndRenderedEquation
    //
    // Description:
    //
    //   Verifies that context-sensitive help covers every detail context with localized text and rendered equation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void userGuide_coversEveryDetailContextWithLocalizedTextAndRenderedEquation ()
        throws IOException
    {
        // Verify the context-sensitive help covers every detail context with localized text and rendered equation
        // scenario against
        // its expected outcome.

        assertThat ( ClientUserGuide.resolve ( null, false ) )
            .isEqualTo ( ClientUserGuide.Context.MODEL );
        assertThat ( ClientUserGuide.resolve ( "parameters", false ) )
            .isEqualTo ( ClientUserGuide.Context.PARAMETERS );
        assertThat ( ClientUserGuide.resolve ( "payloads", false ) )
            .isEqualTo ( ClientUserGuide.Context.PAYLOADS );
        assertThat ( ClientUserGuide.resolve ( "events", false ) )
            .isEqualTo ( ClientUserGuide.Context.EVENTS );
        assertThat ( ClientUserGuide.resolve ( "conditions", false ) )
            .isEqualTo ( ClientUserGuide.Context.CONDITIONS );
        assertThat ( ClientUserGuide.resolve ( "condition-sets", false ) )
            .isEqualTo ( ClientUserGuide.Context.CONDITION_SETS );
        assertThat ( ClientUserGuide.resolve ( "actions", false ) )
            .isEqualTo ( ClientUserGuide.Context.ACTIONS );
        assertThat ( ClientUserGuide.resolve ( "rules", false ) )
            .isEqualTo ( ClientUserGuide.Context.RULES );
        assertThat ( ClientUserGuide.resolve ( "rules", true ) )
            .isEqualTo ( ClientUserGuide.Context.SIMULATOR );

        // Initialize the messages by applying get bundle.

        ResourceBundle messages = ResourceBundle.getBundle (
            "com.rohingosling.eca.client.messages",
            Locale.ROOT
        );

        // Verify the context-sensitive help covers every detail context with localized text and rendered equation
        // scenario against
        // its expected outcome.

        assertThat ( messages.getString ( "outline.title" ) ).isEqualTo ( "Outline" );

        // Process each context supplied by client context-sensitive help context values.

        for ( ClientUserGuide.Context context : ClientUserGuide.Context.values () )
        {
            // Initialize the resource prefix by applying get localization suffix.

            String resourcePrefix = "user.guide." + context.getLocalizationSuffix ();

            // Verify the context-sensitive help covers every detail context with localized text and rendered equation
            // scenario
            // against its expected outcome.

            assertThat ( messages.getString ( resourcePrefix + ".overview" ) ).isNotBlank ();
            assertThat ( messages.getString ( resourcePrefix + ".steps" ) )
                .contains ( "1.", "2.", "3.", "4.", "5." );
            assertThat ( messages.getString ( resourcePrefix + ".equation.accessible" ) )
                .isNotBlank ();

            int lightEquationWidth  = 0;
            int lightEquationHeight = 0;

            // Process each dark mode supplied by list of false true.

            for ( boolean darkMode : List.of ( false, true ) )
            {
                // Open the scoped resources for the protected operation and close them automatically afterward.

                try ( InputStream equationStream = ClientMain.class.getResourceAsStream (
                    context.getEquationResourceName ( darkMode )
                ) )
                {
                    // Verify the context-sensitive help covers every detail context with localized text and rendered
                    // equation
                    // scenario against its expected outcome.

                    assertThat ( equationStream ).isNotNull ();

                    // Initialize the equation bytes by applying read all bytes.

                    byte[] equationBytes = equationStream.readAllBytes ();

                    // Verify the context-sensitive help covers every detail context with localized text and rendered
                    // equation
                    // scenario against its expected outcome.

                    assertThat ( equationBytes )
                        .startsWith (
                            ( byte ) 0x89,
                            ( byte ) 0x50,
                            ( byte ) 0x4E,
                            ( byte ) 0x47,
                            ( byte ) 0x0D,
                            ( byte ) 0x0A,
                            ( byte ) 0x1A,
                            ( byte ) 0x0A
                        );

                    // Prepare the equation width and equation height values needed by the context-sensitive help
                    // covers every
                    // detail context with localized text and rendered equation operation.

                    int equationWidth = ByteBuffer.wrap ( equationBytes ).getInt ( 16 );
                    int equationHeight = ByteBuffer.wrap ( equationBytes ).getInt ( 20 );

                    // Verify the context-sensitive help covers every detail context with localized text and rendered
                    // equation
                    // scenario against its expected outcome.

                    assertThat ( equationWidth ).isBetween ( 80, 400 );
                    assertThat ( equationHeight ).isBetween ( 30, 140 );

                    // Handle the branch where dark mode is true.

                    if ( darkMode )
                    {
                        // Verify the context-sensitive help covers every detail context with localized text and
                        // rendered equation
                        // scenario against its expected outcome.

                        assertThat ( equationWidth ).isEqualTo ( lightEquationWidth );
                        assertThat ( equationHeight ).isEqualTo ( lightEquationHeight );
                    }

                    // Handle the alternative path when the preceding condition is not satisfied.

                    else
                    {
                        lightEquationWidth  = equationWidth;
                        lightEquationHeight = equationHeight;
                    }

                    // Initialize the equation image by applying read.

                    BufferedImage equationImage = ImageIO.read (
                        new ByteArrayInputStream ( equationBytes )
                    );

                    // Verify the context-sensitive help covers every detail context with localized text and rendered
                    // equation
                    // scenario against its expected outcome.

                    assertThat ( equationImage ).isNotNull ();

                    // Initialize the background color by applying get rgb.

                    int backgroundColor = equationImage.getRGB ( 0, 0 );
                    int backgroundRed = ( backgroundColor >> 16 ) & 0xff;
                    int backgroundGreen = ( backgroundColor >> 8 ) & 0xff;
                    int backgroundBlue = backgroundColor & 0xff;
                    int backgroundBrightness = (
                        backgroundRed + backgroundGreen + backgroundBlue
                    ) / 3;

                    // Repeat the loop while x is less than equation image width.

                    for ( int x = 0; x < equationImage.getWidth (); x++ )
                    {
                        // Verify the context-sensitive help covers every detail context with localized text and
                        // rendered equation
                        // scenario against its expected outcome.

                        assertThat ( equationImage.getRGB ( x, 0 ) ).isEqualTo ( backgroundColor );
                        assertThat ( equationImage.getRGB ( x, equationImage.getHeight () - 1 ) )
                            .isEqualTo ( backgroundColor );
                    }

                    // Repeat the loop while y is less than equation image height - 1.

                    for ( int y = 1; y < equationImage.getHeight () - 1; y++ )
                    {
                        // Verify the context-sensitive help covers every detail context with localized text and
                        // rendered equation
                        // scenario against its expected outcome.

                        assertThat ( equationImage.getRGB ( 0, y ) ).isEqualTo ( backgroundColor );
                        assertThat ( equationImage.getRGB ( equationImage.getWidth () - 1, y ) )
                            .isEqualTo ( backgroundColor );
                    }

                    // Handle the branch where dark mode is true.

                    if ( darkMode )
                    {
                        int nonGrayscalePixelCount = 0;

                        // Repeat the loop while y is less than equation image height.

                        for ( int y = 0; y < equationImage.getHeight (); y++ )
                        {
                            // Repeat the loop while x is less than equation image width.

                            for ( int x = 0; x < equationImage.getWidth (); x++ )
                            {
                                // Initialize the pixel by applying get rgb.

                                int pixel = equationImage.getRGB ( x, y );
                                int red   = ( pixel >> 16 ) & 0xff;
                                int green = ( pixel >> 8 ) & 0xff;
                                int blue  = pixel & 0xff;

                                // Handle the branch where red differs from green or green differs from blue.

                                if ( red != green || green != blue )
                                {
                                    nonGrayscalePixelCount++;
                                }
                            }
                        }

                        // Verify the context-sensitive help covers every detail context with localized text and
                        // rendered equation
                        // scenario against its expected outcome.

                        assertThat ( backgroundBrightness ).isLessThan ( 90 );
                        assertThat ( backgroundRed ).isEqualTo ( backgroundGreen );
                        assertThat ( backgroundGreen ).isEqualTo ( backgroundBlue );
                        assertThat ( nonGrayscalePixelCount ).isZero ();
                    }

                    // Handle the alternative path when the preceding condition is not satisfied.

                    else
                    {
                        // Verify the context-sensitive help covers every detail context with localized text and
                        // rendered equation
                        // scenario against its expected outcome.

                        assertThat ( backgroundBrightness ).isGreaterThan ( 220 );
                    }
                }
            }
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: presenter
    //
    // Description:
    //
    //   Performs the presenter operation.
    //
    // Arguments:
    //
    //   view (FakeView):
    //     The view to use.
    //
    //   documentOperations (ClientDocumentOperations):
    //     The document operations to use.
    //
    //   serverOperations (ClientServerOperations):
    //     The server operations to use.
    //
    //   session (ClientDocumentSession):
    //     The session to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static ClientPresenter presenter (
        FakeView view,
        ClientDocumentOperations documentOperations,
        ClientServerOperations serverOperations,
        ClientDocumentSession session
    )
    {
        // Return a newly constructed client presenter containing the operation result.

        return new ClientPresenter (
            view,
            documentOperations,
            serverOperations,
            session,
            ClientConnectionSettings.defaults ()
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: dirtySession
    //
    // Description:
    //
    //   Performs the dirty session operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static ClientDocumentSession dirtySession ()
    {
        // Initialize the session by applying loaded, content, and of.

        ClientDocumentSession session = ClientDocumentSession.loaded (
            content ( "dirty-model", "Dirty Model", "1.0" ),
            Path.of ( "dirty.json" )
        );

        // Apply apply edit and content to the session for the dirty session operation.

        session.applyEdit ( content ( "dirty-model", "Changed Dirty Model", "1.0" ) );

        // Return the session to the caller.

        return session;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: content
    //
    // Description:
    //
    //   Performs the content operation.
    //
    // Arguments:
    //
    //   modelIdentifier (String):
    //     The model identifier to use.
    //
    //   name (String):
    //     The name to use.
    //
    //   schemaVersion (String):
    //     The schema version to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static ClientDocumentSession.Content content (
        String modelIdentifier,
        String name,
        String schemaVersion
    )
    {
        // Return a newly constructed client document session content containing the operation result.

        return new ClientDocumentSession.Content (
            new AuthoringModel (
                schemaVersion,
                modelIdentifier,
                name,
                "",
                List.of (),
                List.of (),
                List.of (),
                List.of (),
                List.of (),
                List.of (),
                List.of ()
            )
        );
    }

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
    //   responseBody (byte []):
    //     The response body to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void respond ( HttpExchange exchange, int statusCode, byte [] responseBody )
        throws IOException
    {
        // Perform the set, get response headers, send response headers, write, get response body, and close calls
        // required by the respond operation.

        exchange.getResponseHeaders ().set ( "Content-Type", "application/json" );
        exchange.sendResponseHeaders ( statusCode, responseBody.length );
        exchange.getResponseBody ().write ( responseBody );
        exchange.close ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: await
    //
    // Description:
    //
    //   Performs the await operation.
    //
    // Arguments:
    //
    //   countDownLatch (CountDownLatch):
    //     The count down latch to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void await ( CountDownLatch countDownLatch )
    {
        try
        {
            // Wait for the coordination latch to be released.

            countDownLatch.await ( 5, TimeUnit.SECONDS );
        }

        // Handle interrupted failures captured as exception.

        catch ( InterruptedException exception )
        {
            // Perform the interrupt and current thread calls required by the await operation.

            Thread.currentThread ().interrupt ();
            throw new IllegalStateException ( "Interrupted while awaiting the test server.", exception );
        }
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
        // Fields
        //=============================================================================================================

        private final List <Path> openedPaths = new ArrayList <Path> ();
        private final List <Path> savedPaths  = new ArrayList <Path> ();

        private ClientDocumentSession.Content openedContent = content (
            "opened-model",
            "Opened Model",
            "1.0"
        );
        private Throwable openFailure;
        private Throwable saveFailure;
        private CompletableFuture <ClientDocumentSession.Content> customOpenFuture;

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
            // Add path to the opened paths.

            this.openedPaths.add ( path );

            // Stop this path and return its result when custom open future is available.

            if ( this.customOpenFuture != null )
            {
                // Return the custom open future to the caller when custom open future is available.

                return this.customOpenFuture;
            }

            // Return the value selected according to open failure is unavailable.

            return this.openFailure == null
                ? CompletableFuture.completedFuture ( this.openedContent )
                : CompletableFuture.failedFuture ( this.openFailure );
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
        public CompletableFuture <Void> save ( Path path, ClientDocumentSession.Content content )
        {
            // Add path to the saved paths.

            this.savedPaths.add ( path );

            // Return the value selected according to save failure is unavailable.

            return this.saveFailure == null
                ? CompletableFuture.completedFuture ( null )
                : CompletableFuture.failedFuture ( this.saveFailure );
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
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private int pullCount;
        private int pushCount;

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
            // Return the result produced by completed future.

            return CompletableFuture.completedFuture ( "Connected" );
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
            this.pullCount++;

            // Return the result produced by completed future.

            return CompletableFuture.completedFuture (
                content ( "pulled-model", "Pulled Model", "1.0" )
            );
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
            this.pushCount++;

            // Return the result produced by completed future.

            return CompletableFuture.completedFuture ( "Accepted" );
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

        private boolean discardConfirmed = true;
        private boolean exitRequested;
        private int openChooserCount;
        private int dispatchCount;
        private String lastError = "";
        private String lastStatus = "";
        private String lastMessage = "";
        private String lastMessageSource = "";
        private MessageSeverity lastMessageSeverity;
        private int validationReportCount;
        private ClientDocumentSession lastValidationSession;
        private Optional <Path> openPath = Optional.of ( Path.of ( "opened.json" ) );
        private Optional <Path> savePath = Optional.empty ();

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
            this.dispatchCount++;

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
            this.validationReportCount++;
            this.lastValidationSession = session;
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
        public void reportMessage ( MessageSeverity severity, String source, String message )
        {
            this.lastMessageSeverity = severity;
            this.lastMessageSource   = source;
            this.lastMessage         = message;
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
            this.lastError = message;

            // Complete the report error step by calling report message.

            reportMessage ( MessageSeverity.ERROR, title, message );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: confirmDiscard
        //
        // Description:
        //
        //   Performs the confirm discard operation.
        //
        // Arguments:
        //
        //   actionName (String):
        //     The action name to use.
        //
        // Returns:
        //
        //   The result of the operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public boolean confirmDiscard ( String actionName )
        {
            // Return the discard confirmed to the caller.

            return this.discardConfirmed;
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
            this.openChooserCount++;

            // Return the open path to the caller.

            return this.openPath;
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
            // Return the save path to the caller.

            return this.savePath;
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
            this.exitRequested = true;
        }
    }

    //*****************************************************************************************************************
    // Class: NamedThreadFactory
    //
    // Description:
    //
    //   Provides the named thread factory behavior.
    //
    //*****************************************************************************************************************

    private static final class NamedThreadFactory implements ThreadFactory
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String threadName;

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: NamedThreadFactory
        //
        // Description:
        //
        //   Creates the NamedThreadFactory instance from the supplied values.
        //
        // Arguments:
        //
        //   threadName (String):
        //     The thread name to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private NamedThreadFactory ( String threadName )
        {
            this.threadName = threadName;
        }

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: newThread
        //
        // Description:
        //
        //   Performs the new thread operation.
        //
        // Arguments:
        //
        //   runnable (Runnable):
        //     The runnable to use.
        //
        // Returns:
        //
        //   The result of the operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public Thread newThread ( Runnable runnable )
        {
            // Initialize the thread with a new thread.

            Thread thread = new Thread ( runnable, this.threadName );

            // Set the daemon on the thread.

            thread.setDaemon ( true );

            // Return the thread to the caller.

            return thread;
        }
    }
}
