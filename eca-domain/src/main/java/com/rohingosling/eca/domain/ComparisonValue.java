//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Represents one compiled typed comparison used by a reusable condition.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.domain;

import java.util.Objects;

//*********************************************************************************************************************
// Class: ComparisonValue
//
// Description:
//
//   Represents one compiled typed comparison used by a reusable condition.
//
//*********************************************************************************************************************

public final class ComparisonValue implements ConditionValue
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final ParameterDefinition parameterDefinition;
    private final ConditionOperator operator;
    private final Object firstOperand;
    private final Object secondOperand;

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
    // Method: getOperator
    //
    // Description:
    //
    //   Returns the operator.
    //
    // Returns:
    //
    //   The operator.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ConditionOperator getOperator ()
    {
        // Return the operator to the caller.

        return this.operator;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getFirstOperand
    //
    // Description:
    //
    //   Returns the first operand.
    //
    // Returns:
    //
    //   The first operand.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Object getFirstOperand ()
    {
        // Return the first operand to the caller.

        return this.firstOperand;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getSecondOperand
    //
    // Description:
    //
    //   Returns the second operand.
    //
    // Returns:
    //
    //   The second operand.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Object getSecondOperand ()
    {
        // Return the second operand to the caller.

        return this.secondOperand;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: ComparisonValue
    //
    // Description:
    //
    //   Creates the ComparisonValue instance from the supplied values.
    //
    // Arguments:
    //
    //   parameterDefinition (ParameterDefinition):
    //     The parameter definition to use.
    //
    //   operator (ConditionOperator):
    //     The operator to use.
    //
    //   firstOperand (Object):
    //     The first operand to use.
    //
    //   secondOperand (Object):
    //     The second operand to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ComparisonValue (
        ParameterDefinition parameterDefinition,
        ConditionOperator operator,
        Object firstOperand,
        Object secondOperand
    )
    {
        // Validate the required parameter definition and operator before continuing.

        this.parameterDefinition = Objects.requireNonNull ( parameterDefinition, "parameterDefinition" );
        this.operator            = Objects.requireNonNull ( operator, "operator" );

        // Reject the operation when operator equals condition operator any.

        if ( operator == ConditionOperator.ANY )
        {
            throw new IllegalArgumentException ( "ANY conditions compile as explicit wildcards." );
        }

        // Reject the operation when operator supports does not succeed.

        if ( !operator.supports ( parameterDefinition.getParameterType () ) )
        {
            throw new IllegalArgumentException (
                "Operator " + operator + " is not supported by " + parameterDefinition.getParameterType () + "."
            );
        }

        // Reject the operation when parameter definition accepts concrete value does not succeed.

        if ( !parameterDefinition.acceptsConcreteValue ( firstOperand ) )
        {
            throw new IllegalArgumentException ( "The first operand does not belong to the parameter domain." );
        }

        // Reject the operation when operator operand count equals 2 and parameter definition accepts concrete value
        // does not succeed.

        if (
            operator.getOperandCount () == 2
                && !parameterDefinition.acceptsConcreteValue ( secondOperand )
        )
        {
            throw new IllegalArgumentException ( "The second operand does not belong to the parameter domain." );
        }

        // Reject the operation when operator operand count equals 1 and second operand is available.

        if ( operator.getOperandCount () == 1 && secondOperand != null )
        {
            throw new IllegalArgumentException ( "The selected operator accepts only one operand." );
        }

        // Reject the operation when operator is range and compare first operand second operand is at least 0.

        if (
            operator.isRange ()
                && compare ( firstOperand, secondOperand ) >= 0
        )
        {
            throw new IllegalArgumentException ( "A range's lower operand must be less than its upper operand." );
        }

        this.firstOperand  = firstOperand;
        this.secondOperand = secondOperand;
    }

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
    //   candidateValue (Object):
    //     The candidate value to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public boolean matches ( Object candidateValue )
    {
        // Stop this path and return its result when parameter definition accepts concrete value does not succeed.

        if ( !this.parameterDefinition.acceptsConcreteValue ( candidateValue ) )
        {
            // Return false for this outcome when parameter definition accepts concrete value does not succeed.

            return false;
        }

        // Initialize the first comparison by applying compare.

        int firstComparison = compare ( candidateValue, this.firstOperand );

        // Return the matches result to the caller.

        return switch ( this.operator )
        {
            // Handle equals through this switch branch.

            case EQUALS                -> firstComparison == 0;

            // Handle not equals through this switch branch.

            case NOT_EQUALS            -> firstComparison != 0;

            // Handle greater than through this switch branch.

            case GREATER_THAN          -> firstComparison > 0;

            // Handle greater than or equal through this switch branch.

            case GREATER_THAN_OR_EQUAL -> firstComparison >= 0;

            // Handle less than through this switch branch.

            case LESS_THAN             -> firstComparison < 0;

            // Handle less than or equal through this switch branch.

            case LESS_THAN_OR_EQUAL    -> firstComparison <= 0;

            // Handle between exclusive through this switch branch.

            case BETWEEN_EXCLUSIVE     ->
                firstComparison > 0 && compare ( candidateValue, this.secondOperand ) < 0;

            // Handle between inclusive through this switch branch.

            case BETWEEN_INCLUSIVE     ->
                firstComparison >= 0 && compare ( candidateValue, this.secondOperand ) <= 0;

            // Handle any through this switch branch.

            case ANY -> throw new IllegalStateException ( "ANY conditions compile as explicit wildcards." );
        };
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: compare
    //
    // Description:
    //
    //   Performs the compare operation.
    //
    // Arguments:
    //
    //   leftValue (Object):
    //     The left value to use.
    //
    //   rightValue (Object):
    //     The right value to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @SuppressWarnings ( "unchecked" )
    private static int compare ( Object leftValue, Object rightValue )
    {
        // Return the result produced by compare to.

        return ((Comparable <Object>) leftValue).compareTo ( rightValue );
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

        // Stop this path and return its result when object is not a comparison value.

        if ( !( object instanceof ComparisonValue ) )
        {
            // Return false for this outcome when object is not a comparison value.

            return false;
        }

        ComparisonValue comparisonValue = (ComparisonValue) object;

        // Return whether parameter definition matches comparison value parameter definition and operator equals
        // comparison value operator and first operand matches comparison value first operand and objects matches
        // second operand.

        return this.parameterDefinition.equals ( comparisonValue.parameterDefinition )
            && this.operator == comparisonValue.operator
            && this.firstOperand.equals ( comparisonValue.firstOperand )
            && Objects.equals ( this.secondOperand, comparisonValue.secondOperand );
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

        return Objects.hash (
            this.parameterDefinition,
            this.operator,
            this.firstOperand,
            this.secondOperand
        );
    }
}
