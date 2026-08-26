//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Identifies one action value selected, but never executed, by the mathematical core.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.domain;

//*********************************************************************************************************************
// Class: ActionId
//
// Description:
//
//   Identifies one action value selected, but never executed, by the mathematical core.
//
//*********************************************************************************************************************

public final class ActionId extends StableIdentifier
{
    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: ActionId
    //
    // Description:
    //
    //   Creates the ActionId instance from the supplied values.
    //
    // Arguments:
    //
    //   value (String):
    //     The value to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ActionId ( String value )
    {
        // Initialize the inherited state through the base-class constructor.

        super ( value );
    }
}
