//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Emits configurable structured hosting events without accepting model documents, payloads, or credentials.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.server;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.rohingosling.eca.application.HostedModelFailureStage;
import com.rohingosling.eca.application.HostedModelLogger;
import com.rohingosling.eca.application.HostedModelSummary;

//*********************************************************************************************************************
// Class: StructuredHostedModelLogger
//
// Description:
//
//   Emits configurable structured hosting events without accepting model documents, payloads, or credentials.
//
//*********************************************************************************************************************

public final class StructuredHostedModelLogger implements HostedModelLogger
{
    private static final Logger SYSTEM_LOGGER = Logger.getLogger (
        StructuredHostedModelLogger.class.getName ()
    );

    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final Consumer <String> logSink;
    private final boolean enabled;

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/2: StructuredHostedModelLogger
    //
    // Description:
    //
    //   Creates the StructuredHostedModelLogger instance from the supplied values.
    //
    // Arguments:
    //
    //   enabled (boolean):
    //     The enabled to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public StructuredHostedModelLogger ( boolean enabled )
    {
        // Delegate initialization to the primary structured hosted model logger constructor.

        this ( StructuredHostedModelLogger::writeSystemLog, enabled );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 2/2: StructuredHostedModelLogger
    //
    // Description:
    //
    //   Creates the StructuredHostedModelLogger instance from the supplied values.
    //
    // Arguments:
    //
    //   logSink (Consumer <String>):
    //     The log sink to use.
    //
    //   enabled (boolean):
    //     The enabled to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public StructuredHostedModelLogger ( Consumer <String> logSink, boolean enabled )
    {
        // Validate the required log sink before continuing.

        this.logSink = Objects.requireNonNull ( logSink, "logSink" );
        this.enabled = enabled;
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: startupWithoutModel
    //
    // Description:
    //
    //   Performs the startup without model operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public void startupWithoutModel ()
    {
        // Record the operation through the this.

        this.log ( "event=startup status=not-ready reason=no-active-model" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: startupRestored
    //
    // Description:
    //
    //   Performs the startup restored operation.
    //
    // Arguments:
    //
    //   summary (HostedModelSummary):
    //     The summary to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public void startupRestored ( HostedModelSummary summary )
    {
        // Apply log and summary fields to the this for the startup restored operation.

        this.log ( "event=startup status=ready " + summaryFields ( summary ) );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: startupFailed
    //
    // Description:
    //
    //   Performs the startup failed operation.
    //
    // Arguments:
    //
    //   failureStage (HostedModelFailureStage):
    //     The failure stage to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public void startupFailed ( HostedModelFailureStage failureStage )
    {
        // Perform the log and name calls required by the startup failed operation.

        this.log ( "event=startup status=failed stage=" + failureStage.name () );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: replacementSucceeded
    //
    // Description:
    //
    //   Performs the replacement succeeded operation.
    //
    // Arguments:
    //
    //   summary (HostedModelSummary):
    //     The summary to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public void replacementSucceeded ( HostedModelSummary summary )
    {
        // Apply log and summary fields to the this for the replacement succeeded operation.

        this.log ( "event=model-replacement status=accepted " + summaryFields ( summary ) );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: replacementFailed
    //
    // Description:
    //
    //   Performs the replacement failed operation.
    //
    // Arguments:
    //
    //   failureStage (HostedModelFailureStage):
    //     The failure stage to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public void replacementFailed ( HostedModelFailureStage failureStage )
    {
        // Perform the log and name calls required by the replacement failed operation.

        this.log ( "event=model-replacement status=rejected stage=" + failureStage.name () );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: evaluationCompleted
    //
    // Description:
    //
    //   Performs the evaluation completed operation.
    //
    // Arguments:
    //
    //   revision (String):
    //     The revision to use.
    //
    //   outcome (String):
    //     The outcome to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public void evaluationCompleted ( String revision, String outcome )
    {
        // Record the operation through the this.

        this.log (
            "event=evaluation status=completed revision=" + revision + " outcome=" + outcome
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: shutdownRequested
    //
    // Description:
    //
    //   Performs the shutdown requested operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public void shutdownRequested ()
    {
        // Record the operation through the this.

        this.log ( "event=shutdown status=requested" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: log
    //
    // Description:
    //
    //   Performs the log operation.
    //
    // Arguments:
    //
    //   message (String):
    //     The message to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void log ( String message )
    {
        // Handle the branch where enabled is true.

        if ( this.enabled )
        {
            // Send the prepared value to its configured consumer.

            this.logSink.accept ( message );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: writeSystemLog
    //
    // Description:
    //
    //   Performs the write system log operation.
    //
    // Arguments:
    //
    //   message (String):
    //     The message to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void writeSystemLog ( String message )
    {
        // Perform the logp and get name calls required by the write system log operation.

        SYSTEM_LOGGER.logp (
            Level.INFO,
            StructuredHostedModelLogger.class.getName (),
            "log",
            message
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: summaryFields
    //
    // Description:
    //
    //   Performs the summary fields operation.
    //
    // Arguments:
    //
    //   summary (HostedModelSummary):
    //     The summary to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String summaryFields ( HostedModelSummary summary )
    {
        // Validate the required summary before continuing.

        Objects.requireNonNull ( summary, "summary" );

        // Return the composed summary fields value.

        return "model-id=" + summary.getModelId ()
            + " revision=" + summary.getRevision ()
            + " parameters=" + summary.getParameterCount ()
            + " payloads=" + summary.getPayloadCount ()
            + " events=" + summary.getEventCount ()
            + " conditions=" + summary.getConditionCount ()
            + " condition-sets=" + summary.getConditionSetCount ()
            + " actions=" + summary.getActionCount ()
            + " rules=" + summary.getRuleCount ();
    }
}
