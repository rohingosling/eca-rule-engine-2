//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Defines the adapter port that parses, validates, canonicalizes, and compiles a hosted model.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.application;

//---------------------------------------------------------------------------------------------------------------------
// Interface: HostedModelFactory
//
// Description:
//
//   Defines the adapter port that parses, validates, canonicalizes, and compiles a hosted model.
//
//---------------------------------------------------------------------------------------------------------------------

public interface HostedModelFactory
{
    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
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
    //-----------------------------------------------------------------------------------------------------------------

    HostedModelSnapshot create ( byte [] modelDocument );
}
