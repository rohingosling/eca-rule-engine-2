//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Defines a present compiled condition binding as either concrete equality or an explicit wildcard.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.domain;

//---------------------------------------------------------------------------------------------------------------------
// Interface: ConditionValue
//
// Description:
//
//   Defines a present compiled condition binding as either concrete equality or an explicit wildcard.
//
//---------------------------------------------------------------------------------------------------------------------

public interface ConditionValue
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
