//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Represents a finite immutable partial map of concrete and present-null payload bindings.
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
import java.util.Optional;

//*********************************************************************************************************************
// Class: Payload
//
// Description:
//
//   Represents a finite immutable partial map of concrete and present-null payload bindings.
//
//*********************************************************************************************************************

public final class Payload
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final Map <ParameterId, PayloadValue> bindings;

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

    public Map <ParameterId, PayloadValue> getBindings ()
    {
        // Return the bindings to the caller.

        return this.bindings;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: Payload
    //
    // Description:
    //
    //   Creates the Payload instance from the supplied values.
    //
    // Arguments:
    //
    //   bindings (Map <ParameterId, ? extends PayloadValue>):
    //     The bindings to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Payload ( Map <ParameterId, ? extends PayloadValue> bindings )
    {
        // Validate the required bindings before continuing.

        Objects.requireNonNull ( bindings, "bindings" );

        // Initialize the copied bindings with a new linked hash map.

        LinkedHashMap <ParameterId, PayloadValue> copiedBindings = new LinkedHashMap <ParameterId, PayloadValue> ();

        // Process each entry supplied by bindings entry set.

        for ( Map.Entry <ParameterId, ? extends PayloadValue> entry : bindings.entrySet () )
        {
            // Prepare the parameter ID and payload value values needed by the payload operation.

            ParameterId parameterId = Objects.requireNonNull ( entry.getKey (), "bindings must not contain null keys" );
            PayloadValue payloadValue = Objects.requireNonNull (
                entry.getValue (),
                "bindings must not contain null values"
            );

            // Reject the operation when parameter ID differs from payload value parameter definition parameter ID.

            if ( !parameterId.equals ( payloadValue.getParameterDefinition ().getParameterId () ) )
            {
                throw new IllegalArgumentException ( "Payload map key and value parameter identifiers must match." );
            }

            // Store payload value under parameter ID in the copied bindings.

            copiedBindings.put ( parameterId, payloadValue );
        }

        // Update the bindings from the unmodifiable map result.

        this.bindings = Collections.unmodifiableMap ( copiedBindings );
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: findBinding
    //
    // Description:
    //
    //   Performs the find binding operation.
    //
    // Arguments:
    //
    //   parameterId (ParameterId):
    //     The parameter id to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Optional <PayloadValue> findBinding ( ParameterId parameterId )
    {
        // Validate the required parameter ID before continuing.

        Objects.requireNonNull ( parameterId, "parameterId" );

        // Return an optional containing the value when it is available.

        return Optional.ofNullable ( this.bindings.get ( parameterId ) );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: containsParameter
    //
    // Description:
    //
    //   Performs the contains parameter operation.
    //
    // Arguments:
    //
    //   parameterId (ParameterId):
    //     The parameter id to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public boolean containsParameter ( ParameterId parameterId )
    {
        // Validate the required parameter ID before continuing.

        Objects.requireNonNull ( parameterId, "parameterId" );

        // Return the result produced by contains key.

        return this.bindings.containsKey ( parameterId );
    }

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

        // Stop this path and return its result when object is not a payload.

        if ( !( object instanceof Payload ) )
        {
            // Return false for this outcome when object is not a payload.

            return false;
        }

        Payload payload = (Payload) object;

        // Return whether the current value matches the comparison target.

        return this.bindings.equals ( payload.bindings );
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
