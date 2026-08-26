//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies hosted-model startup, persistence, replacement, concurrency, limits, logging, and shutdown.
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.rohingosling.eca.application.ActiveModelStore;
import com.rohingosling.eca.application.HostedEvaluation;
import com.rohingosling.eca.application.HostedModelApplication;
import com.rohingosling.eca.application.HostedModelFactory;
import com.rohingosling.eca.application.HostedModelLimits;
import com.rohingosling.eca.application.HostedModelNotReadyException;
import com.rohingosling.eca.application.HostedModelPersistenceException;
import com.rohingosling.eca.application.HostedModelReplacement;
import com.rohingosling.eca.application.HostedModelSnapshot;
import com.rohingosling.eca.application.HostedModelStartupException;
import com.rohingosling.eca.domain.ActionResult;
import com.rohingosling.eca.domain.EvaluationOutcome;
import com.rohingosling.eca.json.JsonHostedModelFactory;
import com.rohingosling.eca.model.ModelValidationException;
import com.rohingosling.eca.model.OccurrenceDocument;
import com.rohingosling.eca.server.AtomicFileActiveModelStore;
import com.rohingosling.eca.server.StructuredHostedModelLogger;

//*********************************************************************************************************************
// Class: HostedModelPhaseFourTest
//
// Description:
//
//   Verifies hosted-model startup, persistence, replacement, concurrency, limits, logging, and shutdown.
//
//*********************************************************************************************************************

final class HostedModelPhaseFourTest
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    private static final String OLD_ACTION_ID = "action-local-courier";
    private static final String NEW_ACTION_ID = "action-international-courier";

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: startupWithoutModel_isValidButNotEvaluationReady
    //
    // Description:
    //
    //   Verifies that startup without model is valid but not evaluation ready.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void startupWithoutModel_isValidButNotEvaluationReady ()
    {
        // Initialize the application by applying create application.

        HostedModelApplication application = createApplication ( new MemoryActiveModelStore () );

        // Verify the startup without model is valid but not evaluation ready scenario against its expected outcome.

        assertThat ( application.getActiveModel () ).isEmpty ();
        assertThat ( application.inspectReadiness ().isReady () ).isFalse ();
        assertThatThrownBy ( () -> application.evaluateOccurrence ( unmatchedOccurrence () ) )
            .isInstanceOf ( HostedModelNotReadyException.class )
            .hasMessageContaining ( "no active model" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: replacementPersistsCanonicalModelAndRestoresItOnRestart
    //
    // Description:
    //
    //   Performs the replacement persists canonical model and restores it on restart operation.
    //
    // Arguments:
    //
    //   temporaryDirectory (Path):
    //     The temporary directory to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void replacementPersistsCanonicalModelAndRestoresItOnRestart ( @TempDir Path temporaryDirectory )
        throws IOException
    {
        // Prepare the active model path, active model store, first application, replacement, and first snapshot values
        // needed by the replacement persists canonical model and restores it on restart operation.

        Path activeModelPath = temporaryDirectory.resolve ( "active-model.json" );
        AtomicFileActiveModelStore activeModelStore = new AtomicFileActiveModelStore ( activeModelPath );
        HostedModelApplication firstApplication     = createApplication ( activeModelStore );
        HostedModelReplacement replacement          = firstApplication.replaceActiveModel ( testModelDocument () );
        HostedModelSnapshot firstSnapshot           = firstApplication.getActiveModel ().orElseThrow ();

        // Verify the replacement persists canonical model and restores it on restart scenario against its expected
        // outcome.

        assertThat ( Files.readAllBytes ( activeModelPath ) ).isEqualTo ( firstSnapshot.getCanonicalDocument () );
        assertThat ( replacement.getRevision () ).isEqualTo ( firstSnapshot.getRevision () );
        assertThat ( replacement.getSummary ().getParameterCount () ).isEqualTo ( 7 );
        assertThat ( replacement.getSummary ().getPayloadCount () ).isEqualTo ( 3 );
        assertThat ( replacement.getSummary ().getEventCount () ).isEqualTo ( 3 );
        assertThat ( replacement.getSummary ().getConditionCount () ).isEqualTo ( 17 );
        assertThat ( replacement.getSummary ().getConditionSetCount () ).isEqualTo ( 7 );
        assertThat ( replacement.getSummary ().getActionCount () ).isEqualTo ( 3 );
        assertThat ( replacement.getSummary ().getRuleCount () ).isEqualTo ( 10 );

        // Open the scoped resources for the protected operation and close them automatically afterward.

        try ( Stream <Path> directoryEntries = Files.list ( temporaryDirectory ) )
        {
            // Verify the replacement persists canonical model and restores it on restart scenario against its expected
            // outcome.

            assertThat ( directoryEntries.map ( path -> path.getFileName ().toString () ) )
                .noneMatch ( fileName -> fileName.endsWith ( ".tmp" ) );
        }

        // Initialize the restarted application by applying create application.

        HostedModelApplication restartedApplication = createApplication (
            new AtomicFileActiveModelStore ( activeModelPath )
        );

        // Verify the replacement persists canonical model and restores it on restart scenario against its expected
        // outcome.

        assertThat ( restartedApplication.inspectReadiness ().isReady () ).isTrue ();
        assertThat (
            restartedApplication.getActiveModel ().orElseThrow ().getRevision ()
        ).isEqualTo ( firstSnapshot.getRevision () );
        assertThat (
            restartedApplication.evaluateOccurrence ( unmatchedOccurrence () ).getEvaluationResult ().getOutcome ()
        ).isEqualTo ( EvaluationOutcome.NO_ACTION );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: corruptPersistedModelFailsStartupWithActionableError
    //
    // Description:
    //
    //   Performs the corrupt persisted model fails startup with actionable error operation.
    //
    // Arguments:
    //
    //   temporaryDirectory (Path):
    //     The temporary directory to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void corruptPersistedModelFailsStartupWithActionableError ( @TempDir Path temporaryDirectory )
        throws IOException
    {
        // Initialize the active model path by applying resolve.

        Path activeModelPath = temporaryDirectory.resolve ( "active-model.json" );

        // Verify the corrupt persisted model fails startup with actionable error scenario against its expected
        // outcome.

        Files.writeString ( activeModelPath, "{ corrupt", StandardCharsets.UTF_8 );

        assertThatThrownBy (
            () -> createApplication ( new AtomicFileActiveModelStore ( activeModelPath ) )
        )
            .isInstanceOf ( HostedModelStartupException.class )
            .hasMessageContaining ( "persisted active model" )
            .hasMessageContaining ( "Repair or remove" )
            .hasCauseInstanceOf ( IllegalArgumentException.class );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: failedReplacementRetainsActiveAndDurableSnapshot
    //
    // Description:
    //
    //   Performs the failed replacement retains active and durable snapshot operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void failedReplacementRetainsActiveAndDurableSnapshot ()
    {
        // Prepare the original document, active model store, hosted model factory, application, original revision, and
        // durable document values needed by the failed replacement retains active and durable snapshot operation.

        byte [] originalDocument = testModelDocument ();
        MemoryActiveModelStore activeModelStore = new MemoryActiveModelStore ( originalDocument );
        ToggleHostedModelFactory hostedModelFactory = new ToggleHostedModelFactory (
            new JsonHostedModelFactory ()
        );
        HostedModelApplication application = new HostedModelApplication (
            hostedModelFactory,
            activeModelStore,
            () ->
            {
            }
        );
        String originalRevision   = application.getActiveModel ().orElseThrow ().getRevision ();
        byte [] durableDocument   = activeModelStore.getPersistedDocument ();

        // Verify the failed replacement retains active and durable snapshot scenario against its expected outcome.

        assertThatThrownBy ( () -> application.replaceActiveModel ( "{".getBytes ( StandardCharsets.UTF_8 ) ) )
            .isInstanceOf ( IllegalArgumentException.class );
        assertOldStateRetained ( application, activeModelStore, originalRevision, durableDocument );

        // Initialize the invalid document by applying text document, replace, and document text.

        byte [] invalidDocument = textDocument (
            documentText ( originalDocument ).replace (
                "\"schemaVersion\" : \"1.0\"",
                "\"schemaVersion\" : \"unsupported\""
            )
        );

        // Verify the failed replacement retains active and durable snapshot scenario against its expected outcome.

        assertThatThrownBy ( () -> application.replaceActiveModel ( invalidDocument ) )
            .isInstanceOf ( ModelValidationException.class );
        assertOldStateRetained ( application, activeModelStore, originalRevision, durableDocument );

        hostedModelFactory.setCompilationFailureEnabled ( true );

        assertThatThrownBy ( () -> application.replaceActiveModel ( renamedModelDocument ( "compile-failure" ) ) )
            .isInstanceOf ( IllegalStateException.class )
            .hasMessageContaining ( "compile" );
        assertOldStateRetained ( application, activeModelStore, originalRevision, durableDocument );

        hostedModelFactory.setCompilationFailureEnabled ( false );
        activeModelStore.setPersistenceFailureEnabled ( true );

        assertThatThrownBy ( () -> application.replaceActiveModel ( renamedModelDocument ( "persist-failure" ) ) )
            .isInstanceOf ( HostedModelPersistenceException.class );
        assertOldStateRetained ( application, activeModelStore, originalRevision, durableDocument );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: competingReplacementsAreSerializedInObservedOrder
    //
    // Description:
    //
    //   Performs the competing replacements are serialized in observed order operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @RepeatedTest ( 10 )
    void competingReplacementsAreSerializedInObservedOrder () throws Exception
    {
        // Prepare the active model store, application, executor service, and second replacement started values needed
        // by the competing replacements are serialized in observed order operation.

        BlockingActiveModelStore activeModelStore = new BlockingActiveModelStore ();
        HostedModelApplication application        = createApplication ( activeModelStore );
        ExecutorService executorService           = Executors.newFixedThreadPool ( 2 );
        CountDownLatch secondReplacementStarted   = new CountDownLatch ( 1 );

        try
        {
            // Initialize the first replacement by applying submit, replace active model, and renamed model document.

            Future <HostedModelReplacement> firstReplacement = executorService.submit (
                () -> application.replaceActiveModel ( renamedModelDocument ( "first" ) )
            );

            // Verify the competing replacements are serialized in observed order scenario against its expected
            // outcome.

            assertThat ( activeModelStore.awaitBlockedPersistence () ).isTrue ();

            // Initialize the second replacement by applying submit, count down, replace active model, and renamed
            // model document.

            Future <HostedModelReplacement> secondReplacement = executorService.submit (
                () ->
                {
                    // Release threads waiting on the coordination latch.

                    secondReplacementStarted.countDown ();

                    // Return the result produced by replace active model.

                    return application.replaceActiveModel ( renamedModelDocument ( "second" ) );
                }
            );

            // Verify the competing replacements are serialized in observed order scenario against its expected
            // outcome.

            assertThat ( secondReplacementStarted.await ( 5, TimeUnit.SECONDS ) ).isTrue ();
            assertThat ( activeModelStore.getMaximumConcurrentPersistCount () ).isEqualTo ( 1 );
            assertThat ( secondReplacement.isDone () ).isFalse ();

            activeModelStore.releaseBlockedPersistence ();

            // Prepare the first result and second result values needed by the competing replacements are serialized in
            // observed order operation.

            HostedModelReplacement firstResult  = firstReplacement.get ( 5, TimeUnit.SECONDS );
            HostedModelReplacement secondResult = secondReplacement.get ( 5, TimeUnit.SECONDS );

            // Verify the competing replacements are serialized in observed order scenario against its expected
            // outcome.

            assertThat ( activeModelStore.getMaximumConcurrentPersistCount () ).isEqualTo ( 1 );
            assertThat ( activeModelStore.getPersistedDocuments () ).hasSize ( 2 );
            assertThat ( firstResult.getRevision () ).isNotEqualTo ( secondResult.getRevision () );
            assertThat (
                application.getActiveModel ().orElseThrow ().getRevision ()
            ).isEqualTo ( secondResult.getRevision () );
        }

        // Complete the required cleanup regardless of how the protected operation finishes.

        finally
        {
            // Perform the release blocked persistence and shutdown now calls required by the competing replacements
            // are serialized in observed order operation.

            activeModelStore.releaseBlockedPersistence ();
            executorService.shutdownNow ();
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: evaluationRemainsLockFreeAndObservesOnlyCompleteSnapshots
    //
    // Description:
    //
    //   Performs the evaluation remains lock free and observes only complete snapshots operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @RepeatedTest ( 10 )
    void evaluationRemainsLockFreeAndObservesOnlyCompleteSnapshots () throws Exception
    {
        // Prepare the original document, replacement document, active model store, application, original revision,
        // executor service, stop evaluations, actions by revision, and evaluation tasks values needed by the
        // evaluation remains lock free and observes only complete snapshots operation.

        byte [] originalDocument = testModelDocument ();
        byte [] replacementDocument = changedActionModelDocument ();
        BlockingActiveModelStore activeModelStore = new BlockingActiveModelStore ( originalDocument );
        HostedModelApplication application        = createApplication ( activeModelStore );
        String originalRevision                   = application.getActiveModel ().orElseThrow ().getRevision ();
        ExecutorService executorService           = Executors.newFixedThreadPool ( 5 );
        AtomicBoolean stopEvaluations             = new AtomicBoolean ();
        Map <String, Set <String>> actionsByRevision = new ConcurrentHashMap <String, Set <String>> ();
        ArrayList <Future <?>> evaluationTasks       = new ArrayList <Future <?>> ();

        try
        {
            // Initialize the replacement by applying submit and replace active model.

            Future <HostedModelReplacement> replacement = executorService.submit (
                () -> application.replaceActiveModel ( replacementDocument )
            );

            // Verify the evaluation remains lock free and observes only complete snapshots scenario against its
            // expected outcome.

            assertThat ( activeModelStore.awaitBlockedPersistence () ).isTrue ();

            // Initialize the lock free evaluation by applying submit, evaluate occurrence, and local standard
            // occurrence.

            Future <HostedEvaluation> lockFreeEvaluation = executorService.submit (
                () -> application.evaluateOccurrence ( localStandardOccurrence () )
            );

            // Verify the evaluation remains lock free and observes only complete snapshots scenario against its
            // expected outcome.

            assertThat (
                lockFreeEvaluation.get ( 1, TimeUnit.SECONDS ).getRevision ()
            ).isEqualTo ( originalRevision );

            // Repeat the loop while i is less than 4.

            for ( int i = 0; i < 4; i++ )
            {
                // Add executor service submit - while !stop evaluations get record action by revis to the evaluation
                // tasks.

                evaluationTasks.add (
                    executorService.submit (
                        () ->
                        {
                            // Continue processing while stop evaluations get does not succeed.

                            while ( !stopEvaluations.get () )
                            {
                                // Complete the evaluation remains lock free and observes only complete snapshots step
                                // by calling record action by revision.

                                recordActionByRevision ( application, actionsByRevision );
                            }
                        }
                    )
                );
            }

            // Repeat the loop while i is less than 200.

            for ( int i = 0; i < 200; i++ )
            {
                // Complete the evaluation remains lock free and observes only complete snapshots step by calling
                // record action by revision.

                recordActionByRevision ( application, actionsByRevision );
            }

            // Complete the evaluation remains lock free and observes only complete snapshots step by calling release
            // blocked persistence.

            activeModelStore.releaseBlockedPersistence ();

            // Initialize the replacement result by applying get.

            HostedModelReplacement replacementResult = replacement.get ( 5, TimeUnit.SECONDS );

            // Repeat the loop while i is less than 200.

            for ( int i = 0; i < 200; i++ )
            {
                // Complete the evaluation remains lock free and observes only complete snapshots step by calling
                // record action by revision.

                recordActionByRevision ( application, actionsByRevision );
            }

            // Set the set on the stop evaluations.

            stopEvaluations.set ( true );

            // Process each evaluation task supplied by evaluation tasks.

            for ( Future <?> evaluationTask : evaluationTasks )
            {
                // Retrieve the completed value from the asynchronous operation.

                evaluationTask.get ( 5, TimeUnit.SECONDS );
            }

            // Verify the evaluation remains lock free and observes only complete snapshots scenario against its
            // expected outcome.

            assertThat ( actionsByRevision.keySet () )
                .containsExactlyInAnyOrder ( originalRevision, replacementResult.getRevision () );
            assertThat ( actionsByRevision.get ( originalRevision ) ).containsExactly ( OLD_ACTION_ID );
            assertThat ( actionsByRevision.get ( replacementResult.getRevision () ) )
                .containsExactly ( NEW_ACTION_ID );
        }

        // Complete the required cleanup regardless of how the protected operation finishes.

        finally
        {
            // Perform the set, release blocked persistence, and shutdown now calls required by the evaluation remains
            // lock free and observes only complete snapshots operation.

            stopEvaluations.set ( true );
            activeModelStore.releaseBlockedPersistence ();
            executorService.shutdownNow ();
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: configurableLimitsAndTypedLogsExcludeSensitiveContent
    //
    // Description:
    //
    //   Performs the configurable limits and typed logs exclude sensitive content operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void configurableLimitsAndTypedLogsExcludeSensitiveContent ()
    {
        // Prepare the log messages, shutdown requested, active model store, application, and sensitive model text
        // values needed by the configurable limits and typed logs exclude sensitive content operation.

        ArrayList <String> logMessages = new ArrayList <String> ();
        AtomicBoolean shutdownRequested = new AtomicBoolean ();
        MemoryActiveModelStore activeModelStore = new MemoryActiveModelStore ();
        HostedModelApplication application = new HostedModelApplication (
            new JsonHostedModelFactory (),
            activeModelStore,
            () -> shutdownRequested.set ( true ),
            new StructuredHostedModelLogger ( logMessages::add, true ),
            HostedModelLimits.defaults ()
        );
        String sensitiveModelText = documentText ( testModelDocument () ).replace (
            "Selects a courier for orders, returns, and cancellations.",
            "SECRET_CONTROL_TOKEN"
        );

        // Apply replace active model, text document, evaluate occurrence, cancel occurrence, and request shutdown to
        // the application for the configurable limits and typed logs exclude sensitive content operation.

        application.replaceActiveModel ( textDocument ( sensitiveModelText ) );
        application.evaluateOccurrence ( cancelOccurrence ( "SECRET_COMPLETE_PAYLOAD" ) );
        application.requestShutdown ();

        // Initialize the complete log by applying join.

        String completeLog = String.join ( "\n", logMessages );

        // Verify the configurable limits and typed logs exclude sensitive content scenario against its expected
        // outcome.

        assertThat ( completeLog ).contains ( "event=startup", "event=model-replacement", "event=evaluation" );
        assertThat ( completeLog ).contains ( "revision=sha256:" );
        assertThat ( completeLog ).doesNotContain (
            "SECRET_CONTROL_TOKEN",
            "SECRET_COMPLETE_PAYLOAD",
            "\"schemaVersion\"",
            "\"payload\""
        );
        assertThat ( shutdownRequested ).isTrue ();

        // Initialize the limited application by applying test model document.

        HostedModelApplication limitedApplication = new HostedModelApplication (
            new JsonHostedModelFactory (),
            new MemoryActiveModelStore (),
            () ->
            {
            },
            new StructuredHostedModelLogger ( logMessages::add, false ),
            new HostedModelLimits ( testModelDocument ().length - 1 )
        );

        // Verify the configurable limits and typed logs exclude sensitive content scenario against its expected
        // outcome.

        assertThatThrownBy ( () -> limitedApplication.replaceActiveModel ( testModelDocument () ) )
            .isInstanceOf ( IllegalArgumentException.class )
            .hasMessageContaining ( "configured" );
        assertThat ( limitedApplication.getActiveModel () ).isEmpty ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: snapshotDefensivelyCopiesCanonicalDocuments
    //
    // Description:
    //
    //   Performs the snapshot defensively copies canonical documents operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void snapshotDefensivelyCopiesCanonicalDocuments ()
    {
        // Prepare the model document and application values needed by the snapshot defensively copies canonical
        // documents operation.

        byte [] modelDocument = testModelDocument ();
        HostedModelApplication application = createApplication ( new MemoryActiveModelStore () );

        // Complete the snapshot defensively copies canonical documents step by calling replace active model.

        application.replaceActiveModel ( modelDocument );

        // Prepare the snapshot and first canonical copy values needed by the snapshot defensively copies canonical
        // documents operation.

        HostedModelSnapshot snapshot = application.getActiveModel ().orElseThrow ();
        byte [] firstCanonicalCopy   = snapshot.getCanonicalDocument ();
        byte originalFirstByte       = firstCanonicalCopy [ 0 ];

        // Overwrite the test buffer to verify defensive copying.

        Arrays.fill ( modelDocument, (byte) 'x' );
        firstCanonicalCopy [ 0 ] = (byte) 'x';

        // Verify the snapshot defensively copies canonical documents scenario against its expected outcome.

        assertThat ( snapshot.getCanonicalDocument () [ 0 ] ).isEqualTo ( originalFirstByte );
        assertThat ( application.inspectReadiness ().getSummary ().orElseThrow ().getRevision () )
            .isEqualTo ( snapshot.getRevision () );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: recordActionByRevision
    //
    // Description:
    //
    //   Performs the record action by revision operation.
    //
    // Arguments:
    //
    //   application (HostedModelApplication):
    //     The application to use.
    //
    //   actionsByRevision (Map <String, Set <String>>):
    //     The actions by revision to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void recordActionByRevision (
        HostedModelApplication application,
        Map <String, Set <String>> actionsByRevision
    )
    {
        // Prepare the evaluation and action result values needed by the record action by revision operation.

        HostedEvaluation evaluation = application.evaluateOccurrence (
            localStandardOccurrence ()
        );
        ActionResult actionResult = (ActionResult) evaluation.getEvaluationResult ();

        // Add action result action ID value to the new key set.

        actionsByRevision.computeIfAbsent (
            evaluation.getRevision (),
            ignoredRevision -> ConcurrentHashMap.newKeySet ()
        ).add ( actionResult.getActionId ().getValue () );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: assertOldStateRetained
    //
    // Description:
    //
    //   Performs the assert old state retained operation.
    //
    // Arguments:
    //
    //   application (HostedModelApplication):
    //     The application to use.
    //
    //   activeModelStore (MemoryActiveModelStore):
    //     The active model store to use.
    //
    //   originalRevision (String):
    //     The original revision to use.
    //
    //   durableDocument (byte []):
    //     The durable document to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void assertOldStateRetained (
        HostedModelApplication application,
        MemoryActiveModelStore activeModelStore,
        String originalRevision,
        byte [] durableDocument
    )
    {
        // Verify the assert old state retained scenario against its expected outcome.

        assertThat ( application.getActiveModel ().orElseThrow ().getRevision () ).isEqualTo ( originalRevision );
        assertThat ( activeModelStore.getPersistedDocument () ).isEqualTo ( durableDocument );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createApplication
    //
    // Description:
    //
    //   Performs the create application operation.
    //
    // Arguments:
    //
    //   activeModelStore (ActiveModelStore):
    //     The active model store to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static HostedModelApplication createApplication ( ActiveModelStore activeModelStore )
    {
        // Return a newly constructed hosted model application containing the operation result.

        return new HostedModelApplication (
            new JsonHostedModelFactory (),
            activeModelStore,
            () ->
            {
            }
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: testModelDocument
    //
    // Description:
    //
    //   Performs the test model document operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static byte [] testModelDocument ()
    {
        // Initialize the project root by applying of and get property.

        Path projectRoot = Path.of ( System.getProperty ( "eca.project.root" ) );

        try
        {
            // Return the result produced by read all bytes.

            return Files.readAllBytes ( projectRoot.resolve ( "examples/eca-rule-engine-example.json" ) );
        }

        // Handle I/O failures captured as exception.

        catch ( IOException exception )
        {
            throw new IllegalStateException ( "The test model fixture could not be read.", exception );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: renamedModelDocument
    //
    // Description:
    //
    //   Performs the renamed model document operation.
    //
    // Arguments:
    //
    //   suffix (String):
    //     The suffix to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static byte [] renamedModelDocument ( String suffix )
    {
        // Return the result produced by text document.

        return textDocument (
            documentText ( testModelDocument () ).replace (
                "\"name\" : \"ECA Rule Engine Example\"",
                "\"name\" : \"ECA Rule Engine Example " + suffix + "\""
            )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: changedActionModelDocument
    //
    // Description:
    //
    //   Performs the changed action model document operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static byte [] changedActionModelDocument ()
    {
        // Return the result produced by text document.

        return textDocument (
            documentText ( testModelDocument () ).replace (
                "\"actionId\" : \"action-local-courier\"",
                "\"actionId\" : \"action-international-courier\""
            )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: localStandardOccurrence
    //
    // Description:
    //
    //   Performs the local standard occurrence operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static OccurrenceDocument localStandardOccurrence ()
    {
        // Return a newly constructed occurrence document containing the operation result.

        return new OccurrenceDocument (
            "event-order-product",
            Map.of (
                "parameter-delivery-type", "STANDARD",
                "parameter-product-category", "RETAIL",
                "parameter-region", "LOCAL",
                "parameter-vip", false
            ),
            true
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: cancelOccurrence
    //
    // Description:
    //
    //   Performs the cancel occurrence operation.
    //
    // Arguments:
    //
    //   orderReference (String):
    //     The order reference to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static OccurrenceDocument cancelOccurrence ( String orderReference )
    {
        // Return a newly constructed occurrence document containing the operation result.

        return new OccurrenceDocument (
            "event-cancel-order",
            Map.of ( "parameter-order-reference", orderReference ),
            true
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: unmatchedOccurrence
    //
    // Description:
    //
    //   Performs the unmatched occurrence operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static OccurrenceDocument unmatchedOccurrence ()
    {
        // Return whether cel occurrence.

        return cancelOccurrence ( "ORDER-1000" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: documentText
    //
    // Description:
    //
    //   Performs the document text operation.
    //
    // Arguments:
    //
    //   document (byte []):
    //     The document to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String documentText ( byte [] document )
    {
        // Return a newly constructed string containing the operation result.

        return new String ( document, StandardCharsets.UTF_8 );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: textDocument
    //
    // Description:
    //
    //   Performs the text document operation.
    //
    // Arguments:
    //
    //   document (String):
    //     The document to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static byte [] textDocument ( String document )
    {
        // Return the result produced by get bytes.

        return document.getBytes ( StandardCharsets.UTF_8 );
    }

    //*****************************************************************************************************************
    // Class: MemoryActiveModelStore
    //
    // Description:
    //
    //   Provides the memory active model store behavior.
    //
    //*****************************************************************************************************************

    private static class MemoryActiveModelStore implements ActiveModelStore
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private byte [] persistedDocument;
        private boolean persistenceFailureEnabled;

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/2: MemoryActiveModelStore
        //
        // Description:
        //
        //   Creates the MemoryActiveModelStore instance from the supplied values.
        //
        //-------------------------------------------------------------------------------------------------------------

        private MemoryActiveModelStore ()
        {
        }

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 2/2: MemoryActiveModelStore
        //
        // Description:
        //
        //   Creates the MemoryActiveModelStore instance from the supplied values.
        //
        // Arguments:
        //
        //   persistedDocument (byte []):
        //     The persisted document to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private MemoryActiveModelStore ( byte [] persistedDocument )
        {
            // Update the persisted document from the copy of result.

            this.persistedDocument = Arrays.copyOf ( persistedDocument, persistedDocument.length );
        }

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: load
        //
        // Description:
        //
        //   Performs the load operation.
        //
        // Returns:
        //
        //   The result of the operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public synchronized Optional <byte []> load ()
        {
            // Return the value selected according to persisted document is unavailable.

            return this.persistedDocument == null
                ? Optional.empty ()
                : Optional.of ( Arrays.copyOf ( this.persistedDocument, this.persistedDocument.length ) );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: persist
        //
        // Description:
        //
        //   Performs the persist operation.
        //
        // Arguments:
        //
        //   canonicalDocument (byte []):
        //     The canonical document to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public synchronized void persist ( byte [] canonicalDocument )
        {
            // Reject the operation when persistence failure enabled is true.

            if ( this.persistenceFailureEnabled )
            {
                throw new HostedModelPersistenceException (
                    "Synthetic persistence failure.",
                    new IOException ( "Synthetic persistence failure." )
                );
            }

            // Update the persisted document from the copy of result.

            this.persistedDocument = Arrays.copyOf ( canonicalDocument, canonicalDocument.length );
        }

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getPersistedDocument
        //
        // Description:
        //
        //   Returns the persisted document.
        //
        // Returns:
        //
        //   The persisted document.
        //
        //-------------------------------------------------------------------------------------------------------------

        private synchronized byte [] getPersistedDocument ()
        {
            // Return an immutable copy of persisted document.

            return Arrays.copyOf ( this.persistedDocument, this.persistedDocument.length );
        }

        //=============================================================================================================
        // Mutators
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: setPersistenceFailureEnabled
        //
        // Description:
        //
        //   Sets the persistence failure enabled.
        //
        // Arguments:
        //
        //   persistenceFailureEnabled (boolean):
        //     The persistence failure enabled to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private synchronized void setPersistenceFailureEnabled ( boolean persistenceFailureEnabled )
        {
            this.persistenceFailureEnabled = persistenceFailureEnabled;
        }
    }

    //*****************************************************************************************************************
    // Class: ToggleHostedModelFactory
    //
    // Description:
    //
    //   Provides the toggle hosted model factory behavior.
    //
    //*****************************************************************************************************************

    private static final class ToggleHostedModelFactory implements HostedModelFactory
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final HostedModelFactory delegate;
        private volatile boolean compilationFailureEnabled;

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: ToggleHostedModelFactory
        //
        // Description:
        //
        //   Creates the ToggleHostedModelFactory instance from the supplied values.
        //
        // Arguments:
        //
        //   delegate (HostedModelFactory):
        //     The delegate to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private ToggleHostedModelFactory ( HostedModelFactory delegate )
        {
            this.delegate = delegate;
        }

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: create
        //
        // Description:
        //
        //   Performs the create operation.
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
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public HostedModelSnapshot create ( byte [] modelDocument )
        {
            // Reject the operation when compilation failure enabled is true.

            if ( this.compilationFailureEnabled )
            {
                throw new IllegalStateException ( "Synthetic compile failure." );
            }

            // Return the result produced by create.

            return this.delegate.create ( modelDocument );
        }

        //=============================================================================================================
        // Mutators
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: setCompilationFailureEnabled
        //
        // Description:
        //
        //   Sets the compilation failure enabled.
        //
        // Arguments:
        //
        //   compilationFailureEnabled (boolean):
        //     The compilation failure enabled to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private void setCompilationFailureEnabled ( boolean compilationFailureEnabled )
        {
            this.compilationFailureEnabled = compilationFailureEnabled;
        }
    }

    //*****************************************************************************************************************
    // Class: BlockingActiveModelStore
    //
    // Description:
    //
    //   Provides the blocking active model store behavior.
    //
    //*****************************************************************************************************************

    private static final class BlockingActiveModelStore extends MemoryActiveModelStore
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final AtomicBoolean firstPersistence = new AtomicBoolean ( true );
        private final CountDownLatch persistenceEntered = new CountDownLatch ( 1 );
        private final CountDownLatch persistenceRelease = new CountDownLatch ( 1 );
        private final AtomicInteger concurrentPersistCount = new AtomicInteger ();
        private final AtomicInteger maximumConcurrentPersistCount = new AtomicInteger ();
        private final List <byte []> persistedDocuments = Collections.synchronizedList (
            new ArrayList <byte []> ()
        );

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/2: BlockingActiveModelStore
        //
        // Description:
        //
        //   Creates the BlockingActiveModelStore instance from the supplied values.
        //
        //-------------------------------------------------------------------------------------------------------------

        private BlockingActiveModelStore ()
        {
        }

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 2/2: BlockingActiveModelStore
        //
        // Description:
        //
        //   Creates the BlockingActiveModelStore instance from the supplied values.
        //
        // Arguments:
        //
        //   persistedDocument (byte []):
        //     The persisted document to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private BlockingActiveModelStore ( byte [] persistedDocument )
        {
            // Initialize the inherited state through the base-class constructor.

            super ( persistedDocument );
        }

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: persist
        //
        // Description:
        //
        //   Performs the persist operation.
        //
        // Arguments:
        //
        //   canonicalDocument (byte []):
        //     The canonical document to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public void persist ( byte [] canonicalDocument )
        {
            // Initialize the current persist count by applying increment and get.

            int currentPersistCount = this.concurrentPersistCount.incrementAndGet ();

            // Complete the persist step by calling accumulate and get.

            this.maximumConcurrentPersistCount.accumulateAndGet ( currentPersistCount, Math::max );

            try
            {
                // Handle the branch where first persistence compare and set succeeds.

                if ( this.firstPersistence.compareAndSet ( true, false ) )
                {
                    // Release threads waiting on the coordination latch.

                    this.persistenceEntered.countDown ();

                    // Reject the operation when persistence release await does not succeed.

                    if ( !this.persistenceRelease.await ( 5, TimeUnit.SECONDS ) )
                    {
                        throw new IllegalStateException ( "Timed out waiting to release persistence." );
                    }
                }

                // Perform the persist, add, and copy of calls required by the persist operation.

                super.persist ( canonicalDocument );
                this.persistedDocuments.add ( Arrays.copyOf ( canonicalDocument, canonicalDocument.length ) );
            }

            // Handle interrupted failures captured as exception.

            catch ( InterruptedException exception )
            {
                // Perform the interrupt and current thread calls required by the persist operation.

                Thread.currentThread ().interrupt ();

                throw new IllegalStateException ( "Persistence was interrupted.", exception );
            }

            // Complete the required cleanup regardless of how the protected operation finishes.

            finally
            {
                // Complete the persist step by calling decrement and get.

                this.concurrentPersistCount.decrementAndGet ();
            }
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: awaitBlockedPersistence
        //
        // Description:
        //
        //   Performs the await blocked persistence operation.
        //
        // Returns:
        //
        //   The result of the operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        private boolean awaitBlockedPersistence () throws InterruptedException
        {
            // Return the result produced by await.

            return this.persistenceEntered.await ( 5, TimeUnit.SECONDS );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: releaseBlockedPersistence
        //
        // Description:
        //
        //   Performs the release blocked persistence operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        private void releaseBlockedPersistence ()
        {
            // Release threads waiting on the coordination latch.

            this.persistenceRelease.countDown ();
        }

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getMaximumConcurrentPersistCount
        //
        // Description:
        //
        //   Returns the maximum concurrent persist count.
        //
        // Returns:
        //
        //   The maximum concurrent persist count.
        //
        //-------------------------------------------------------------------------------------------------------------

        private int getMaximumConcurrentPersistCount ()
        {
            // Return the result produced by get.

            return this.maximumConcurrentPersistCount.get ();
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getPersistedDocuments
        //
        // Description:
        //
        //   Returns the persisted documents.
        //
        // Returns:
        //
        //   The persisted documents.
        //
        //-------------------------------------------------------------------------------------------------------------

        private List <byte []> getPersistedDocuments ()
        {
            synchronized ( this.persistedDocuments )
            {
                // Return an immutable copy of persisted documents.

                return List.copyOf ( this.persistedDocuments );
            }
        }
    }
}
