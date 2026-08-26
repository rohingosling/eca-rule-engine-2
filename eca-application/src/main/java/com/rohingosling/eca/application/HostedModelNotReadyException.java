//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Reports that evaluation was requested before an active model became available.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.application;

//*********************************************************************************************************************
// Class: HostedModelNotReadyException
//
// Description:
//
//   Reports that evaluation was requested before an active model became available.
//
//*********************************************************************************************************************

public final class HostedModelNotReadyException extends IllegalStateException
{
    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: HostedModelNotReadyException
    //
    // Description:
    //
    //   Creates the HostedModelNotReadyException instance from the supplied values.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public HostedModelNotReadyException ()
    {
        // Initialize the inherited state through the base-class constructor.

        super ( "Evaluation is unavailable because no active model is loaded." );
    }
}
