//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Defines the supported operators for reusable, typed condition predicates.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.domain;

//---------------------------------------------------------------------------------------------------------------------
// Enum: ConditionOperator
//
// Description:
//
//   Defines the supported operators for reusable, typed condition predicates.
//
//---------------------------------------------------------------------------------------------------------------------

public enum ConditionOperator
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    EQUALS ( 1 ),
    NOT_EQUALS ( 1 ),
    GREATER_THAN ( 1 ),
    GREATER_THAN_OR_EQUAL ( 1 ),
    LESS_THAN ( 1 ),
    LESS_THAN_OR_EQUAL ( 1 ),
    BETWEEN_EXCLUSIVE ( 2 ),
    BETWEEN_INCLUSIVE ( 2 ),
    ANY ( 0 );

    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final int operandCount;

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getOperandCount
    //
    // Description:
    //
    //   Returns the operand count.
    //
    // Returns:
    //
    //   The operand count.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public int getOperandCount ()
    {
        // Return the operand count to the caller.

        return this.operandCount;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: ConditionOperator
    //
    // Description:
    //
    //   Creates the ConditionOperator instance from the supplied values.
    //
    // Arguments:
    //
    //   operandCount (int):
    //     The operand count to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    ConditionOperator ( int operandCount )
    {
        this.operandCount = operandCount;
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: supports
    //
    // Description:
    //
    //   Performs the supports operation.
    //
    // Arguments:
    //
    //   parameterType (ParameterType):
    //     The parameter type to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public boolean supports ( ParameterType parameterType )
    {
        // Stop this path and return its result when this equals any or this equals equals or this equals not equals.

        if ( this == ANY || this == EQUALS || this == NOT_EQUALS )
        {
            // Return true for this outcome when this equals any or this equals equals or this equals not equals.

            return true;
        }

        // Return whether parameter type equals parameter type integer.

        return parameterType == ParameterType.INTEGER;
    }

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: isRange
    //
    // Description:
    //
    //   Indicates whether range.
    //
    // Returns:
    //
    //   `true` when the condition is satisfied; otherwise `false`.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public boolean isRange ()
    {
        // Return whether this equals between exclusive or this equals between inclusive.

        return this == BETWEEN_EXCLUSIVE || this == BETWEEN_INCLUSIVE;
    }
}
