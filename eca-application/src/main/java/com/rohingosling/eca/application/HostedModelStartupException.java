//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Reports that persisted active-model data could not be restored safely during startup.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.application;

//*********************************************************************************************************************
// Class: HostedModelStartupException
//
// Description:
//
//   Reports that persisted active-model data could not be restored safely during startup.
//
//*********************************************************************************************************************

public final class HostedModelStartupException extends IllegalStateException
{
    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: HostedModelStartupException
    //
    // Description:
    //
    //   Creates the HostedModelStartupException instance from the supplied values.
    //
    // Arguments:
    //
    //   cause (Throwable):
    //     The exception that caused the operation to fail.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public HostedModelStartupException ( Throwable cause )
    {
        // Initialize the inherited state through the base-class constructor.

        super (
            "The persisted active model could not be parsed, validated, and compiled. "
                + "Repair or remove the active-model file before restarting.",
            cause
        );
    }
}
