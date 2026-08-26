//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Provides the immutable string identity shared by the mathematical core's typed identifiers.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.domain;

import java.util.Objects;

//*********************************************************************************************************************
// Class: StableIdentifier
//
// Description:
//
//   Provides the immutable string identity shared by the mathematical core's typed identifiers.
//
//*********************************************************************************************************************

public abstract class StableIdentifier
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final String value;

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

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

    public final String getValue ()
    {
        // Return the value to the caller.

        return this.value;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: StableIdentifier
    //
    // Description:
    //
    //   Creates the StableIdentifier instance from the supplied values.
    //
    // Arguments:
    //
    //   value (String):
    //     The value to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    protected StableIdentifier ( String value )
    {
        // Validate the required value before continuing.

        Objects.requireNonNull ( value, "value" );

        // Reject the operation when value trim contains no values.

        if ( value.trim ().isEmpty () )
        {
            throw new IllegalArgumentException ( "Identifier values must not be blank." );
        }

        this.value = value;
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
    public final boolean equals ( Object object )
    {
        // Return immediately when the compared reference is this object.

        if ( this == object )
        {
            // Return true because the compared reference is this object.

            return true;
        }

        // Stop this path and return its result when object is unavailable or get class differs from object class.

        if ( object == null || this.getClass () != object.getClass () )
        {
            // Return false for this outcome when object is unavailable or get class differs from object class.

            return false;
        }

        StableIdentifier identifier = (StableIdentifier) object;

        // Return whether the current value matches the comparison target.

        return this.value.equals ( identifier.value );
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
    public final int hashCode ()
    {
        // Return the stable hash code derived from the object's value components.

        return Objects.hash ( this.value );
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
    public final String toString ()
    {
        // Return the value to the caller.

        return this.value;
    }
}
