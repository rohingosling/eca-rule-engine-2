//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Defines a present payload binding as either a concrete value or an explicit present-null value.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.domain;

//---------------------------------------------------------------------------------------------------------------------
// Interface: PayloadValue
//
// Description:
//
//   Defines a present payload binding as either a concrete value or an explicit present-null value.
//
//---------------------------------------------------------------------------------------------------------------------

public interface PayloadValue
{
    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getParameterDefinition
    //
    // Description:
    //
    //   Returns the parameter definition.
    //
    // Returns:
    //
    //   The parameter definition.
    //
    //-----------------------------------------------------------------------------------------------------------------

    ParameterDefinition getParameterDefinition ();

    //-----------------------------------------------------------------------------------------------------------------
    // Method: isConcrete
    //
    // Description:
    //
    //   Indicates whether concrete.
    //
    // Returns:
    //
    //   `true` when the condition is satisfied; otherwise `false`.
    //
    //-----------------------------------------------------------------------------------------------------------------

    boolean isConcrete ();
}
