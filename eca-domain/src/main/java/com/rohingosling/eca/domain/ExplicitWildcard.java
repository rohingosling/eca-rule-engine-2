//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Represents a condition key that is explicitly present as an unconstrained wildcard.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.domain;

import java.util.Objects;

//*********************************************************************************************************************
// Class: ExplicitWildcard
//
// Description:
//
//   Represents a condition key that is explicitly present as an unconstrained wildcard.
//
//*********************************************************************************************************************

public final class ExplicitWildcard implements ConditionValue
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
    // Constructor 1/1: ExplicitWildcard
    //
    // Description:
    //
    //   Creates the ExplicitWildcard instance from the supplied values.
    //
    // Arguments:
    //
    //   parameterDefinition (ParameterDefinition):
    //     The parameter definition to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ExplicitWildcard ( ParameterDefinition parameterDefinition )
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

        // Stop this path and return its result when object is not an explicit wildcard.

        if ( !( object instanceof ExplicitWildcard ) )
        {
            // Return false for this outcome when object is not an explicit wildcard.

            return false;
        }

        ExplicitWildcard wildcard = (ExplicitWildcard) object;

        // Return whether the current value matches the comparison target.

        return this.parameterDefinition.equals ( wildcard.parameterDefinition );
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

        return "*";
    }
}
