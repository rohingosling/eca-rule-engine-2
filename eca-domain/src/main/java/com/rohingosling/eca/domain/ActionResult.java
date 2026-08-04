//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Reports the action, winning rule, and specificity selected by one pure evaluation.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.domain;

import java.util.Objects;

//*********************************************************************************************************************
// Class: ActionResult
//
// Description:
//
//   Reports the action, winning rule, and specificity selected by one pure evaluation.
//
//*********************************************************************************************************************

public final class ActionResult implements EvaluationResult
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final ActionId actionId;
    private final RuleId ruleId;
    private final int specificity;

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getOutcome
    //
    // Description:
    //
    //   Returns the outcome.
    //
    // Returns:
    //
    //   The outcome.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public EvaluationOutcome getOutcome ()
    {
        // Return the action to the caller.

        return EvaluationOutcome.ACTION;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getActionId
    //
    // Description:
    //
    //   Returns the action id.
    //
    // Returns:
    //
    //   The action id.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ActionId getActionId ()
    {
        // Return the action ID to the caller.

        return this.actionId;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getRuleId
    //
    // Description:
    //
    //   Returns the rule id.
    //
    // Returns:
    //
    //   The rule id.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public RuleId getRuleId ()
    {
        // Return the rule ID to the caller.

        return this.ruleId;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getSpecificity
    //
    // Description:
    //
    //   Returns the specificity.
    //
    // Returns:
    //
    //   The specificity.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public int getSpecificity ()
    {
        // Return the specificity to the caller.

        return this.specificity;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: ActionResult
    //
    // Description:
    //
    //   Creates the ActionResult instance from the supplied values.
    //
    // Arguments:
    //
    //   actionId (ActionId):
    //     The action id to use.
    //
    //   ruleId (RuleId):
    //     The rule id to use.
    //
    //   specificity (int):
    //     The specificity to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ActionResult ( ActionId actionId, RuleId ruleId, int specificity )
    {
        // Validate the required action ID and rule ID before continuing.

        Objects.requireNonNull ( actionId, "actionId" );
        Objects.requireNonNull ( ruleId, "ruleId" );

        // Reject the operation when specificity is less than 0.

        if ( specificity < 0 )
        {
            throw new IllegalArgumentException ( "Specificity must not be negative." );
        }

        this.actionId   = actionId;
        this.ruleId     = ruleId;
        this.specificity = specificity;
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

        // Stop this path and return its result when object is not an action result.

        if ( !( object instanceof ActionResult ) )
        {
            // Return false for this outcome when object is not an action result.

            return false;
        }

        ActionResult actionResult = (ActionResult) object;

        // Return whether specificity equals action result specificity and action ID matches action result action ID
        // and rule ID matches action result rule ID.

        return this.specificity == actionResult.specificity
            && this.actionId.equals ( actionResult.actionId )
            && this.ruleId.equals ( actionResult.ruleId );
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

        return Objects.hash ( this.actionId, this.ruleId, this.specificity );
    }
}
