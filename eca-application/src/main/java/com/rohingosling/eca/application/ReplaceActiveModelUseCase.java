//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Defines the application port for replacing the active model from an external document.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.application;

//---------------------------------------------------------------------------------------------------------------------
// Interface: ReplaceActiveModelUseCase
//
// Description:
//
//   Defines the application port for replacing the active model from an external document.
//
//---------------------------------------------------------------------------------------------------------------------

public interface ReplaceActiveModelUseCase
{
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

    HostedModelReplacement replaceActiveModel ( byte [] modelDocument );
}
