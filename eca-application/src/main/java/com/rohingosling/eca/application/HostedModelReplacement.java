//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Reports the revision and entity counts accepted by a successful model replacement.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.application;

import java.util.Objects;

//*********************************************************************************************************************
// Class: HostedModelReplacement
//
// Description:
//
//   Reports the revision and entity counts accepted by a successful model replacement.
//
//*********************************************************************************************************************

public final class HostedModelReplacement
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final HostedModelSummary summary;

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

    public HostedModelSummary getSummary ()
    {
        // Return the summary to the caller.

        return this.summary;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getRevision
    //
    // Description:
    //
    //   Returns the revision.
    //
    // Returns:
    //
    //   The revision.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String getRevision ()
    {
        // Return the result produced by get revision.

        return this.summary.getRevision ();
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: HostedModelReplacement
    //
    // Description:
    //
    //   Creates the HostedModelReplacement instance from the supplied values.
    //
    // Arguments:
    //
    //   summary (HostedModelSummary):
    //     The summary to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public HostedModelReplacement ( HostedModelSummary summary )
    {
        // Validate the required summary before continuing.

        this.summary = Objects.requireNonNull ( summary, "summary" );
    }
}
