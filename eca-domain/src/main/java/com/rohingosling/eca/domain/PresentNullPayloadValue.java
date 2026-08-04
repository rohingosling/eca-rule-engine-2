//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Represents a payload key that is present with the distinguished null marker.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.domain;

import java.util.Objects;

//*********************************************************************************************************************
// Class: PresentNullPayloadValue
//
// Description:
//
//   Represents a payload key that is present with the distinguished null marker.
//
//*********************************************************************************************************************

public final class PresentNullPayloadValue implements PayloadValue
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final ParameterDefinition parameterDefinition;

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

    @Override
    public ParameterDefinition getParameterDefinition ()
    {
        // Return the parameter definition to the caller.

        return this.parameterDefinition;
    }

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

    @Override
    public boolean isConcrete ()
    {
        // Return false for this outcome.

        return false;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: PresentNullPayloadValue
    //
    // Description:
    //
    //   Creates the PresentNullPayloadValue instance from the supplied values.
    //
    // Arguments:
    //
    //   parameterDefinition (ParameterDefinition):
    //     The parameter definition to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public PresentNullPayloadValue ( ParameterDefinition parameterDefinition )
    {
        // Validate the required parameter definition before continuing.

        this.parameterDefinition = Objects.requireNonNull ( parameterDefinition, "parameterDefinition" );
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

        // Stop this path and return its result when object is not a present null payload value.

        if ( !( object instanceof PresentNullPayloadValue ) )
        {
            // Return false for this outcome when object is not a present null payload value.

            return false;
        }

        PresentNullPayloadValue payloadValue = (PresentNullPayloadValue) object;

        // Return whether the current value matches the comparison target.

        return this.parameterDefinition.equals ( payloadValue.parameterDefinition );
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

        return Objects.hash ( this.parameterDefinition );
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: toString
    //
    // Description:
    //
    //   Creates the string representation of this value.
    //
    // Returns:
    //
    //   The string representation of this value.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public String toString ()
    {
        // Return the to string text to the caller.

        return "null";
    }
}
