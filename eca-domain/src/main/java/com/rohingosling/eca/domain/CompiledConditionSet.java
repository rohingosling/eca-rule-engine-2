//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Represents a finite immutable partial map of concrete conditions and explicit wildcards.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

//*********************************************************************************************************************
// Class: CompiledConditionSet
//
// Description:
//
//   Represents a finite immutable partial map of concrete conditions and explicit wildcards.
//
//*********************************************************************************************************************

public final class CompiledConditionSet
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final Map <ParameterId, ConditionValue> bindings;

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getBindings
    //
    // Description:
    //
    //   Returns the bindings.
    //
    // Returns:
    //
    //   The bindings.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Map <ParameterId, ConditionValue> getBindings ()
    {
        // Return the bindings to the caller.

        return this.bindings;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: CompiledConditionSet
    //
    // Description:
    //
    //   Creates the CompiledConditionSet instance from the supplied values.
    //
    // Arguments:
    //
    //   bindings (Map <ParameterId, ? extends ConditionValue>):
    //     The bindings to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public CompiledConditionSet ( Map <ParameterId, ? extends ConditionValue> bindings )
    {
        // Validate the required bindings before continuing.

        Objects.requireNonNull ( bindings, "bindings" );

        // Initialize the copied bindings with a new linked hash map.

        LinkedHashMap <ParameterId, ConditionValue> copiedBindings =
            new LinkedHashMap <ParameterId, ConditionValue> ();

        // Process each entry supplied by bindings entry set.

        for ( Map.Entry <ParameterId, ? extends ConditionValue> entry : bindings.entrySet () )
        {
            // Prepare the parameter ID and condition value values needed by the compiled condition set operation.

            ParameterId parameterId = Objects.requireNonNull ( entry.getKey (), "bindings must not contain null keys" );
            ConditionValue conditionValue = Objects.requireNonNull (
                entry.getValue (),
                "bindings must not contain null values"
            );

            // Reject the operation when parameter ID differs from condition value parameter definition parameter ID.

            if ( !parameterId.equals ( conditionValue.getParameterDefinition ().getParameterId () ) )
            {
                throw new IllegalArgumentException ( "Condition map key and value parameter identifiers must match." );
            }

            // Store condition value under parameter ID in the copied bindings.

            copiedBindings.put ( parameterId, conditionValue );
        }

        // Update the bindings from the unmodifiable map result.

        this.bindings = Collections.unmodifiableMap ( copiedBindings );
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: equals
    //
    // Description:
    //
    //   Compares this value with another object for equality.
    //
    // Arguments:
    //
    //   object (Object):
    //     The object to use.
    //
    // Returns:
    //
    //   `true` when the objects are equal; otherwise `false`.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public boolean equals ( Object object )
    {
        // Return immediately when the compared reference is this object.

        if ( this == object )
        {
            // Return true because the compared reference is this object.

            return true;
        }

        // Stop this path and return its result when object is not a compiled condition set.

        if ( !( object instanceof CompiledConditionSet ) )
        {
            // Return false for this outcome when object is not a compiled condition set.

            return false;
        }

        CompiledConditionSet conditionSet = (CompiledConditionSet) object;

        // Return whether the current value matches the comparison target.

        return this.bindings.equals ( conditionSet.bindings );
    }

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: hashCode
    //
    // Description:
    //
    //   Calculates the hash code for this value.
    //
    // Returns:
    //
    //   The hash code for this value.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public int hashCode ()
    {
        // Return the stable hash code derived from the object's value components.

        return Objects.hash ( this.bindings );
    }
}
