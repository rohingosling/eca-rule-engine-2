//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Reports a durable active-model storage failure.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.application;

//*********************************************************************************************************************
// Class: HostedModelPersistenceException
//
// Description:
//
//   Reports a durable active-model storage failure.
//
//*********************************************************************************************************************

public final class HostedModelPersistenceException extends IllegalStateException
{
    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: HostedModelPersistenceException
    //
    // Description:
    //
    //   Creates the HostedModelPersistenceException instance from the supplied values.
    //
    // Arguments:
    //
    //   message (String):
    //     The message to use.
    //
    //   cause (Throwable):
    //     The exception that caused the operation to fail.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public HostedModelPersistenceException ( String message, Throwable cause )
    {
        // Initialize the inherited state through the base-class constructor.

        super ( message, cause );
    }
}
