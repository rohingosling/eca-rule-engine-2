//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Defines the application port for retrieving the immutable active-model snapshot.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.application;

import java.util.Optional;

//---------------------------------------------------------------------------------------------------------------------
// Interface: GetActiveModelUseCase
//
// Description:
//
//   Defines the application port for retrieving the immutable active-model snapshot.
//
//---------------------------------------------------------------------------------------------------------------------

public interface GetActiveModelUseCase
{
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

    Optional <HostedModelSnapshot> getActiveModel ();
}
