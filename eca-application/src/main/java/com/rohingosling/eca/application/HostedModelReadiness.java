//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Reports whether evaluation is ready and includes a non-sensitive summary when a model is active.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.application;

import java.util.Objects;
import java.util.Optional;

//*********************************************************************************************************************
// Class: HostedModelReadiness
//
// Description:
//
//   Reports whether evaluation is ready and includes a non-sensitive summary when a model is active.
//
//*********************************************************************************************************************

public final class HostedModelReadiness
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final HostedModelSummary summary;

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: isReady
    //
    // Description:
    //
    //   Indicates whether ready.
    //
    // Returns:
    //
    //   `true` when the condition is satisfied; otherwise `false`.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public boolean isReady ()
    {
        // Return whether summary is available.

        return this.summary != null;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: HostedModelReadiness
    //
    // Description:
    //
    //   Creates the HostedModelReadiness instance from the supplied values.
    //
    // Arguments:
    //
    //   summary (HostedModelSummary):
    //     The summary to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private HostedModelReadiness ( HostedModelSummary summary )
    {
        this.summary = summary;
    }

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getSummary
    //
    // Description:
    //
    //   Returns the summary.
    //
    // Returns:
    //
    //   The summary.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Optional <HostedModelSummary> getSummary ()
    {
        // Return an optional containing the value when it is available.

        return Optional.ofNullable ( this.summary );
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: notReady
    //
    // Description:
    //
    //   Creates a HostedModelReadiness instance for the not ready case.
    //
    // Returns:
    //
    //   The resulting HostedModelReadiness instance.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static HostedModelReadiness notReady ()
    {
        // Return a newly constructed hosted model readiness containing the operation result.

        return new HostedModelReadiness ( null );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: ready
    //
    // Description:
    //
    //   Creates a HostedModelReadiness instance for the ready case.
    //
    // Arguments:
    //
    //   summary (HostedModelSummary):
    //     The summary to use.
    //
    // Returns:
    //
    //   The resulting HostedModelReadiness instance.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static HostedModelReadiness ready ( HostedModelSummary summary )
    {
        // Return a newly constructed hosted model readiness containing the operation result.

        return new HostedModelReadiness ( Objects.requireNonNull ( summary, "summary" ) );
    }
}
