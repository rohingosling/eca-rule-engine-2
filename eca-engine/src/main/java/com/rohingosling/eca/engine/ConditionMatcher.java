//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Implements the conjunctive condition predicate defined by the stateless ECA rule-engine technical note
//   (`docs/technical-note/stateless-eca-rule-engine.pdf`).
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.engine;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.rohingosling.eca.domain.CompiledConditionSet;
import com.rohingosling.eca.domain.ComparisonValue;
import com.rohingosling.eca.domain.ConcreteValue;
import com.rohingosling.eca.domain.ConditionValue;
import com.rohingosling.eca.domain.ParameterId;
import com.rohingosling.eca.domain.Payload;
import com.rohingosling.eca.domain.PayloadValue;

//*********************************************************************************************************************
// Class: ConditionMatcher
//
// Description:
//
//   Implements the conjunctive condition predicate defined by the stateless ECA rule-engine technical note
//   (`docs/technical-note/stateless-eca-rule-engine.pdf`).
//
//*********************************************************************************************************************

public final class ConditionMatcher
{
    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: matches
    //
    // Description:
    //
    //   Performs the matches operation.
    //
    // Arguments:
    //
    //   conditionSet (CompiledConditionSet):
    //     The condition set to use.
    //
    //   payload (Payload):
    //     The payload to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public boolean matches ( CompiledConditionSet conditionSet, Payload payload )
    {
        // Validate the required condition set and payload before continuing.

        Objects.requireNonNull ( conditionSet, "conditionSet" );
        Objects.requireNonNull ( payload, "payload" );

        // Process each entry supplied by condition set bindings entry set.

        for ( Map.Entry <ParameterId, ConditionValue> entry : conditionSet.getBindings ().entrySet () )
        {
            // Initialize the condition value by applying get value.

            ConditionValue conditionValue = entry.getValue ();

            // Skip the current item when condition value is not concrete.

            if ( !conditionValue.isConcrete () )
            {
                continue;
            }

            // Initialize the payload value by applying find binding and get key.

            Optional <PayloadValue> payloadValue = payload.findBinding ( entry.getKey () );

            // Stop this path and return its result when payload value contains no values or payload value get is not a
            // concrete value.

            if ( payloadValue.isEmpty () || !( payloadValue.get () instanceof ConcreteValue ) )
            {
                // Return false for this outcome when payload value contains no values or payload value get is not a
                // concrete value.

                return false;
            }

            // Handle the branch where condition value is a comparison value.

            if ( conditionValue instanceof ComparisonValue )
            {
                ComparisonValue comparisonValue = (ComparisonValue) conditionValue;

                // Initialize the concrete payload value by applying get.

                ConcreteValue concretePayloadValue = (ConcreteValue) payloadValue.get ();

                // Stop this path and return its result when comparison value does not match the supplied values.

                if ( !comparisonValue.matches ( concretePayloadValue.getValue () ) )
                {
                    // Return false for this outcome when comparison value does not match the supplied values.

                    return false;
                }

                continue;
            }

            // Stop this path and return its result when condition value differs from payload value get.

            if ( !conditionValue.equals ( payloadValue.get () ) )
            {
                // Return false for this outcome when condition value differs from payload value get.

                return false;
            }
        }

        // Return true for this outcome.

        return true;
    }
}
