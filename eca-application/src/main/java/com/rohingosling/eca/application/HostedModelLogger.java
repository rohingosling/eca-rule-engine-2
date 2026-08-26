//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Defines a typed logging port whose arguments exclude model documents, occurrence payloads, and credentials.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.application;

//---------------------------------------------------------------------------------------------------------------------
// Interface: HostedModelLogger
//
// Description:
//
//   Defines a typed logging port whose arguments exclude model documents, occurrence payloads, and credentials.
//
//---------------------------------------------------------------------------------------------------------------------

public interface HostedModelLogger
{
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

    void startupWithoutModel ();

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

    void startupRestored ( HostedModelSummary summary );

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

    void startupFailed ( HostedModelFailureStage failureStage );

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

    void replacementSucceeded ( HostedModelSummary summary );

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

    void replacementFailed ( HostedModelFailureStage failureStage );

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

    void evaluationCompleted ( String revision, String outcome );

    //-----------------------------------------------------------------------------------------------------------------
    // Method: shutdownRequested
    //
    // Description:
    //
    //   Performs the shutdown requested operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    void shutdownRequested ();

    //-----------------------------------------------------------------------------------------------------------------
    // Method: disabled
    //
    // Description:
    //
    //   Creates a HostedModelLogger instance for the disabled case.
    //
    // Returns:
    //
    //   The resulting HostedModelLogger instance.
    //
    //-----------------------------------------------------------------------------------------------------------------

    static HostedModelLogger disabled ()
    {
        // Return a newly constructed hosted model logger containing the operation result.

        return new HostedModelLogger ()
        {
            @Override
            public void startupWithoutModel ()
            {
            }

            @Override
            public void startupRestored ( HostedModelSummary summary )
            {
            }

            @Override
            public void startupFailed ( HostedModelFailureStage failureStage )
            {
            }

            @Override
            public void replacementSucceeded ( HostedModelSummary summary )
            {
            }

            @Override
            public void replacementFailed ( HostedModelFailureStage failureStage )
            {
            }

            @Override
            public void evaluationCompleted ( String revision, String outcome )
            {
            }

            @Override
            public void shutdownRequested ()
            {
            }
        };
    }
}
