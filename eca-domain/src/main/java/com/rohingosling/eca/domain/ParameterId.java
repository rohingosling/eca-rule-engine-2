//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Identifies one typed parameter in the immutable mathematical core.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.domain;

//*********************************************************************************************************************
// Class: ParameterId
//
// Description:
//
//   Identifies one typed parameter in the immutable mathematical core.
//
//*********************************************************************************************************************

public final class ParameterId extends StableIdentifier
{
    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: ParameterId
    //
    // Description:
    //
    //   Creates the ParameterId instance from the supplied values.
    //
    // Arguments:
    //
    //   value (String):
    //     The value to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ParameterId ( String value )
    {
        // Initialize the inherited state through the base-class constructor.

        super ( value );
    }
}
