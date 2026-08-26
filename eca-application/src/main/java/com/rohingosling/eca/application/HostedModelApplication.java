//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Implements the one-model hosting use cases with atomic snapshot capture and serialized durable replacement.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.application;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import com.rohingosling.eca.domain.EvaluationResult;
import com.rohingosling.eca.domain.EventOccurrence;
import com.rohingosling.eca.model.OccurrenceDocument;

//*********************************************************************************************************************
// Class: HostedModelApplication
//
// Description:
//
//   Implements the one-model hosting use cases with atomic snapshot capture and serialized durable replacement.
//
//*********************************************************************************************************************

public final class HostedModelApplication implements
    EvaluateOccurrenceUseCase,
    GetActiveModelUseCase,
    ReplaceActiveModelUseCase,
    InspectReadinessUseCase,
    RequestShutdownUseCase
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final HostedModelFactory hostedModelFactory;
    private final ActiveModelStore activeModelStore;
    private final ShutdownRequestHandler shutdownRequestHandler;
    private final HostedModelLogger hostedModelLogger;
    private final HostedModelLimits hostedModelLimits;
    private final AtomicReference <HostedModelSnapshot> activeSnapshot;
    private final ReentrantLock replacementLock;

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/2: HostedModelApplication
    //
    // Description:
    //
    //   Creates the HostedModelApplication instance from the supplied values.
    //
    // Arguments:
    //
    //   hostedModelFactory (HostedModelFactory):
    //     The hosted model factory to use.
    //
    //   activeModelStore (ActiveModelStore):
    //     The active model store to use.
    //
    //   shutdownRequestHandler (ShutdownRequestHandler):
    //     The shutdown request handler to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public HostedModelApplication (
        HostedModelFactory hostedModelFactory,
        ActiveModelStore activeModelStore,
        ShutdownRequestHandler shutdownRequestHandler
    )
    {
        // Perform the this, disabled, and defaults calls required by the hosted model application operation.

        this (
            hostedModelFactory,
            activeModelStore,
            shutdownRequestHandler,
            HostedModelLogger.disabled (),
            HostedModelLimits.defaults ()
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 2/2: HostedModelApplication
    //
    // Description:
    //
    //   Creates the HostedModelApplication instance from the supplied values.
    //
    // Arguments:
    //
    //   hostedModelFactory (HostedModelFactory):
    //     The hosted model factory to use.
    //
    //   activeModelStore (ActiveModelStore):
    //     The active model store to use.
    //
    //   shutdownRequestHandler (ShutdownRequestHandler):
    //     The shutdown request handler to use.
    //
    //   hostedModelLogger (HostedModelLogger):
    //     The hosted model logger to use.
    //
    //   hostedModelLimits (HostedModelLimits):
    //     The hosted model limits to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public HostedModelApplication (
        HostedModelFactory hostedModelFactory,
        ActiveModelStore activeModelStore,
        ShutdownRequestHandler shutdownRequestHandler,
        HostedModelLogger hostedModelLogger,
        HostedModelLimits hostedModelLimits
    )
    {
        // Validate the required hosted model factory, active model store, shutdown request handler, hosted model
        // logger, and hosted model limits before continuing.

        this.hostedModelFactory     = Objects.requireNonNull ( hostedModelFactory, "hostedModelFactory" );
        this.activeModelStore       = Objects.requireNonNull ( activeModelStore, "activeModelStore" );
        this.shutdownRequestHandler = Objects.requireNonNull (
            shutdownRequestHandler,
            "shutdownRequestHandler"
        );
        this.hostedModelLogger      = Objects.requireNonNull ( hostedModelLogger, "hostedModelLogger" );
        this.hostedModelLimits      = Objects.requireNonNull ( hostedModelLimits, "hostedModelLimits" );
        this.activeSnapshot         = new AtomicReference <HostedModelSnapshot> ();
        this.replacementLock        = new ReentrantLock ( true );

        // Complete the hosted model application step by calling restore persisted model.

        this.restorePersistedModel ();
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: evaluateOccurrence
    //
    // Description:
    //
    //   Performs the evaluate occurrence operation.
    //
    // Arguments:
    //
    //   occurrenceDocument (OccurrenceDocument):
    //     The occurrence document to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public HostedEvaluation evaluateOccurrence ( OccurrenceDocument occurrenceDocument )
    {
        // Validate the required occurrence document before continuing.

        Objects.requireNonNull ( occurrenceDocument, "occurrenceDocument" );

        // Initialize the captured snapshot by applying get.

        HostedModelSnapshot capturedSnapshot = this.activeSnapshot.get ();

        // Reject the operation when captured snapshot is unavailable.

        if ( capturedSnapshot == null )
        {
            throw new HostedModelNotReadyException ();
        }

        // Prepare the occurrence, result, and revision values needed by the evaluate occurrence operation.

        EventOccurrence occurrence = capturedSnapshot.getCompiledModel ().createOccurrence ( occurrenceDocument );
        EvaluationResult result    = capturedSnapshot.evaluate ( occurrence );
        String revision            = capturedSnapshot.getRevision ();

        // Perform the log safely, evaluation completed, name, and get outcome calls required by the evaluate
        // occurrence operation.

        this.logSafely (
            () -> this.hostedModelLogger.evaluationCompleted ( revision, result.getOutcome ().name () )
        );

        // Return a newly constructed hosted evaluation containing the operation result.

        return new HostedEvaluation ( result, revision );
    }

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getActiveModel
    //
    // Description:
    //
    //   Returns the active model.
    //
    // Returns:
    //
    //   The active model.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public Optional <HostedModelSnapshot> getActiveModel ()
    {
        // Return an optional containing the value when it is available.

        return Optional.ofNullable ( this.activeSnapshot.get () );
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: replaceActiveModel
    //
    // Description:
    //
    //   Performs the replace active model operation.
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

    @Override
    public HostedModelReplacement replaceActiveModel ( byte [] modelDocument )
    {
        // Validate the required model document before continuing.

        Objects.requireNonNull ( modelDocument, "modelDocument" );

        HostedModelSnapshot candidateSnapshot;

        try
        {
            // Complete the replace active model step by calling enforce document limit.

            this.enforceDocumentLimit ( modelDocument );

            // Update the candidate snapshot from the create result.

            candidateSnapshot = this.hostedModelFactory.create ( modelDocument );

            // Perform the enforce document limit and get canonical document calls required by the replace active model
            // operation.

            this.enforceDocumentLimit ( candidateSnapshot.getCanonicalDocument () );
        }

        // Handle runtime failures captured as exception.

        catch ( RuntimeException exception )
        {
            // Perform the log safely and replacement failed calls required by the replace active model operation.

            this.logSafely (
                () -> this.hostedModelLogger.replacementFailed ( HostedModelFailureStage.PREPARATION )
            );

            throw exception;
        }

        // Acquire the model-replacement lock before updating shared state.

        this.replacementLock.lock ();

        try
        {
            try
            {
                // Perform the persist and get canonical document calls required by the replace active model operation.

                this.activeModelStore.persist ( candidateSnapshot.getCanonicalDocument () );
            }

            // Handle runtime failures captured as exception.

            catch ( RuntimeException exception )
            {
                // Perform the log safely and replacement failed calls required by the replace active model operation.

                this.logSafely (
                    () -> this.hostedModelLogger.replacementFailed ( HostedModelFailureStage.PERSISTENCE )
                );

                throw exception;
            }

            // Perform the set, log safely, replacement succeeded, and get summary calls required by the replace active
            // model operation.

            this.activeSnapshot.set ( candidateSnapshot );
            this.logSafely (
                () -> this.hostedModelLogger.replacementSucceeded ( candidateSnapshot.getSummary () )
            );

            // Return a newly constructed hosted model replacement containing the operation result.

            return new HostedModelReplacement ( candidateSnapshot.getSummary () );
        }

        // Complete the required cleanup regardless of how the protected operation finishes.

        finally
        {
            // Release the model-replacement lock after the shared-state update.

            this.replacementLock.unlock ();
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: inspectReadiness
    //
    // Description:
    //
    //   Performs the inspect readiness operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public HostedModelReadiness inspectReadiness ()
    {
        // Initialize the captured snapshot by applying get.

        HostedModelSnapshot capturedSnapshot = this.activeSnapshot.get ();

        // Return the value selected according to captured snapshot is unavailable.

        return capturedSnapshot == null
            ? HostedModelReadiness.notReady ()
            : HostedModelReadiness.ready ( capturedSnapshot.getSummary () );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requestShutdown
    //
    // Description:
    //
    //   Performs the request shutdown operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public void requestShutdown ()
    {
        // Perform the log safely and request shutdown calls required by the request shutdown operation.

        this.logSafely ( this.hostedModelLogger::shutdownRequested );
        this.shutdownRequestHandler.requestShutdown ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: restorePersistedModel
    //
    // Description:
    //
    //   Performs the restore persisted model operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void restorePersistedModel ()
    {
        try
        {
            // Initialize the persisted document by applying load.

            Optional <byte []> persistedDocument = this.activeModelStore.load ();

            // Handle the branch where persisted document contains no values.

            if ( persistedDocument.isEmpty () )
            {
                // Record the operation through the this.

                this.logSafely ( this.hostedModelLogger::startupWithoutModel );

                return;
            }

            // Initialize the model document by applying get.

            byte [] modelDocument = persistedDocument.get ();

            // Complete the restore persisted model step by calling enforce document limit.

            this.enforceDocumentLimit ( modelDocument );

            // Initialize the restored snapshot by applying create.

            HostedModelSnapshot restoredSnapshot = this.hostedModelFactory.create ( modelDocument );

            // Perform the enforce document limit, get canonical document, set, log safely, startup restored, and get
            // summary calls required by the restore persisted model operation.

            this.enforceDocumentLimit ( restoredSnapshot.getCanonicalDocument () );
            this.activeSnapshot.set ( restoredSnapshot );
            this.logSafely (
                () -> this.hostedModelLogger.startupRestored ( restoredSnapshot.getSummary () )
            );
        }

        // Handle runtime failures captured as exception.

        catch ( RuntimeException exception )
        {
            // Perform the log safely and startup failed calls required by the restore persisted model operation.

            this.logSafely (
                () -> this.hostedModelLogger.startupFailed ( HostedModelFailureStage.STARTUP )
            );

            throw new HostedModelStartupException ( exception );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: enforceDocumentLimit
    //
    // Description:
    //
    //   Performs the enforce document limit operation.
    //
    // Arguments:
    //
    //   modelDocument (byte []):
    //     The model document to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void enforceDocumentLimit ( byte [] modelDocument )
    {
        // Reject the operation when model document length exceeds hosted model limits maximum model document bytes.

        if ( modelDocument.length > this.hostedModelLimits.getMaximumModelDocumentBytes () )
        {
            throw new IllegalArgumentException (
                "The model document exceeds the configured "
                    + this.hostedModelLimits.getMaximumModelDocumentBytes ()
                    + "-byte limit."
            );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: logSafely
    //
    // Description:
    //
    //   Performs the log safely operation.
    //
    // Arguments:
    //
    //   loggingAction (Runnable):
    //     The logging action to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void logSafely ( Runnable loggingAction )
    {
        try
        {
            // Execute the prepared callback.

            loggingAction.run ();
        }

        // Handle runtime failures captured as exception.

        catch ( RuntimeException exception )
        {
            // Logging is observational and must never alter hosted-model state.

        }
    }
}
