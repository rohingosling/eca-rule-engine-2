//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Represents one typed non-null concrete value used by payload and condition bindings.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.domain;

import java.util.Objects;

//*********************************************************************************************************************
// Class: ConcreteValue
//
// Description:
//
//   Represents one typed non-null concrete value used by payload and condition bindings.
//
//*********************************************************************************************************************

public final class ConcreteValue implements PayloadValue, ConditionValue
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final ParameterDefinition parameterDefinition;
    private final Object value;

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
        // Return true for this outcome.

        return true;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getValue
    //
    // Description:
    //
    //   Returns the value.
    //
    // Returns:
    //
    //   The value.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Object getValue ()
    {
        // Return the value to the caller.

        return this.value;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: ConcreteValue
    //
    // Description:
    //
    //   Creates the ConcreteValue instance from the supplied values.
    //
    // Arguments:
    //
    //   parameterDefinition (ParameterDefinition):
    //     The parameter definition to use.
    //
    //   value (Object):
    //     The value to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ConcreteValue ( ParameterDefinition parameterDefinition, Object value )
    {
        // Validate the required parameter definition and value before continuing.

        Objects.requireNonNull ( parameterDefinition, "parameterDefinition" );
        Objects.requireNonNull ( value, "value" );

        // Reject the operation when parameter definition accepts concrete value does not succeed.

        if ( !parameterDefinition.acceptsConcreteValue ( value ) )
        {
            throw new IllegalArgumentException (
                "Value does not belong to parameter " + parameterDefinition.getParameterId () + "."
            );
        }

        this.parameterDefinition = parameterDefinition;
        this.value               = value;
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

        // Stop this path and return its result when object is not a concrete value.

        if ( !( object instanceof ConcreteValue ) )
        {
            // Return false for this outcome when object is not a concrete value.

            return false;
        }

        ConcreteValue concreteValue = (ConcreteValue) object;

        // Return whether parameter definition matches concrete value parameter definition and value matches concrete
        // value.

        return this.parameterDefinition.equals ( concreteValue.parameterDefinition )
            && this.value.equals ( concreteValue.value );
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

        return Objects.hash ( this.parameterDefinition, this.value );
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
        // Return the completed textual representation.

        return this.value.toString ();
    }
}
