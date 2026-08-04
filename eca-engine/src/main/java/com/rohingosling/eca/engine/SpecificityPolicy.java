//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Implements the technical note's weighted specificity policy: two points per concrete condition, one point per
//   explicit wildcard, and zero points per omitted key (`docs/technical-note/stateless-eca-rule-engine.pdf`).
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.engine;

import java.util.Objects;

import com.rohingosling.eca.domain.CompiledConditionSet;
import com.rohingosling.eca.domain.ConditionValue;

//*********************************************************************************************************************
// Class: SpecificityPolicy
//
// Description:
//
//   Implements the technical note's weighted specificity policy: two points per concrete condition, one point per
//   explicit wildcard, and zero points per omitted key (`docs/technical-note/stateless-eca-rule-engine.pdf`).
//
//*********************************************************************************************************************

public final class SpecificityPolicy
{
    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: calculate
    //
    // Description:
    //
    //   Performs the calculate operation.
    //
    // Arguments:
    //
    //   conditionSet (CompiledConditionSet):
    //     The condition set to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public int calculate ( CompiledConditionSet conditionSet )
    {
        // Validate the required condition set before continuing.

        Objects.requireNonNull ( conditionSet, "conditionSet" );

        int concreteBindingCount = 0;
        int wildcardBindingCount = 0;

        // Process each condition value supplied by condition set bindings values.

        for ( ConditionValue conditionValue : conditionSet.getBindings ().values () )
        {
            // Handle the branch where condition value is concrete.

            if ( conditionValue.isConcrete () )
            {
                concreteBindingCount++;
            }

            // Handle the alternative path when the preceding condition is not satisfied.

            else
            {
                wildcardBindingCount++;
            }
        }

        // Return the result produced by add exact.

        return Math.addExact ( Math.multiplyExact ( concreteBindingCount, 2 ), wildcardBindingCount );
    }
}
