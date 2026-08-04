//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Defines the non-null concrete value domain associated with one parameter identifier.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

//*********************************************************************************************************************
// Class: ParameterDefinition
//
// Description:
//
//   Defines the non-null concrete value domain associated with one parameter identifier.
//
//*********************************************************************************************************************

public final class ParameterDefinition
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final ParameterId parameterId;
    private final ParameterType parameterType;
    private final Set <String> enumerationValues;

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getParameterId
    //
    // Description:
    //
    //   Returns the parameter id.
    //
    // Returns:
    //
    //   The parameter id.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ParameterId getParameterId ()
    {
        // Return the parameter ID to the caller.

        return this.parameterId;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getParameterType
    //
    // Description:
    //
    //   Returns the parameter type.
    //
    // Returns:
    //
    //   The parameter type.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ParameterType getParameterType ()
    {
        // Return the parameter type to the caller.

        return this.parameterType;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getEnumerationValues
    //
    // Description:
    //
    //   Returns the enumeration values.
    //
    // Returns:
    //
    //   The enumeration values.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Set <String> getEnumerationValues ()
    {
        // Return the enumeration values to the caller.

        return this.enumerationValues;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: ParameterDefinition
    //
    // Description:
    //
    //   Creates the ParameterDefinition instance from the supplied values.
    //
    // Arguments:
    //
    //   parameterId (ParameterId):
    //     The parameter id to use.
    //
    //   parameterType (ParameterType):
    //     The parameter type to use.
    //
    //   enumerationValues (Collection <String>):
    //     The enumeration values to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ParameterDefinition (
        ParameterId parameterId,
        ParameterType parameterType,
        Collection <String> enumerationValues
    )
    {
        // Validate the required parameter ID, parameter type, and enumeration values before continuing.

        Objects.requireNonNull ( parameterId, "parameterId" );
        Objects.requireNonNull ( parameterType, "parameterType" );
        Objects.requireNonNull ( enumerationValues, "enumerationValues" );

        // Initialize the copied enumeration values with a new linked hash set.

        LinkedHashSet <String> copiedEnumerationValues = new LinkedHashSet <String> ();

        // Process each enumeration value supplied by enumeration values.

        for ( String enumerationValue : enumerationValues )
        {
            // Validate the required enumeration value before continuing.

            Objects.requireNonNull ( enumerationValue, "enumerationValues must not contain null" );

            // Reject the operation when enumeration value trim contains no values.

            if ( enumerationValue.trim ().isEmpty () )
            {
                throw new IllegalArgumentException ( "Enumeration values must not be blank." );
            }

            // Reject the operation when copied enumeration values add does not succeed.

            if ( !copiedEnumerationValues.add ( enumerationValue ) )
            {
                throw new IllegalArgumentException ( "Enumeration values must be unique." );
            }
        }

        // Reject the operation when parameter type equals parameter type enum and copied enumeration values contains
        // no values.

        if ( parameterType == ParameterType.ENUM && copiedEnumerationValues.isEmpty () )
        {
            throw new IllegalArgumentException ( "ENUM parameters require at least one enumeration value." );
        }

        // Reject the operation when parameter type differs from parameter type enum and copied enumeration values
        // contains values.

        if ( parameterType != ParameterType.ENUM && !copiedEnumerationValues.isEmpty () )
        {
            throw new IllegalArgumentException ( "Only ENUM parameters may define enumeration values." );
        }

        this.parameterId      = parameterId;
        this.parameterType    = parameterType;

        // Update the enumeration values from the unmodifiable set result.

        this.enumerationValues = Collections.unmodifiableSet ( copiedEnumerationValues );
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: acceptsConcreteValue
    //
    // Description:
    //
    //   Performs the accepts concrete value operation.
    //
    // Arguments:
    //
    //   value (Object):
    //     The value to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public boolean acceptsConcreteValue ( Object value )
    {
        // Stop this path and return its result when value is unavailable.

        if ( value == null )
        {
            // Return false for this outcome when value is unavailable.

            return false;
        }

        // Select the processing branch for parameter type.

        switch ( this.parameterType )
        {
            // Handle string through this switch branch.

            case STRING:

                // Return the accepts concrete value result to the caller.

                return value instanceof String;

            // Handle integer through this switch branch.

            case INTEGER:

                // Return the accepts concrete value result to the caller.

                return value instanceof Long;

            // Handle boolean through this switch branch.

            case BOOLEAN:

                // Return the accepts concrete value result to the caller.

                return value instanceof Boolean;

            // Handle enum through this switch branch.

            case ENUM:

                // Return whether value is a string and enumeration values contains value.

                return value instanceof String && this.enumerationValues.contains ( value );

            // Handle the default case through this switch branch.

            default:
                throw new IllegalStateException ( "Unsupported parameter type: " + this.parameterType );
        }
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

        // Stop this path and return its result when object is not a parameter definition.

        if ( !( object instanceof ParameterDefinition ) )
        {
            // Return false for this outcome when object is not a parameter definition.

            return false;
        }

        ParameterDefinition parameterDefinition = (ParameterDefinition) object;

        // Return whether parameter ID matches parameter definition parameter ID and parameter type equals parameter
        // definition parameter type and enumeration values matches parameter definition enumeration values.

        return this.parameterId.equals ( parameterDefinition.parameterId )
            && this.parameterType == parameterDefinition.parameterType
            && this.enumerationValues.equals ( parameterDefinition.enumerationValues );
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

        return Objects.hash ( this.parameterId, this.parameterType, this.enumerationValues );
    }
}
