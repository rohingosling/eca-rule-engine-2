//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Couples one evaluation result to the single hosted-model revision captured for that evaluation.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.application;

import java.util.Objects;

import com.rohingosling.eca.domain.ActionResult;
import com.rohingosling.eca.domain.EvaluationOutcome;
import com.rohingosling.eca.domain.EvaluationResult;

//*********************************************************************************************************************
// Class: HostedEvaluation
//
// Description:
//
//   Couples one evaluation result to the single hosted-model revision captured for that evaluation.
//
//*********************************************************************************************************************

public final class HostedEvaluation
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final EvaluationResult evaluationResult;
    private final String revision;

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getEvaluationResult
    //
    // Description:
    //
    //   Returns the evaluation result.
    //
    // Returns:
    //
    //   The evaluation result.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public EvaluationResult getEvaluationResult ()
    {
        // Return the evaluation result to the caller.

        return this.evaluationResult;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getRevision
    //
    // Description:
    //
    //   Returns the revision.
    //
    // Returns:
    //
    //   The revision.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String getRevision ()
    {
        // Return the revision to the caller.

        return this.revision;
    }

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

    public String getOutcome ()
    {
        // Return the result produced by name.

        return this.evaluationResult.getOutcome ().name ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: isAction
    //
    // Description:
    //
    //   Indicates whether action.
    //
    // Returns:
    //
    //   `true` when the condition is satisfied; otherwise `false`.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public boolean isAction ()
    {
        // Return whether evaluation result outcome equals evaluation outcome action.

        return this.evaluationResult.getOutcome () == EvaluationOutcome.ACTION;
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

    public String getActionId ()
    {
        // Return the result produced by get value.

        return this.actionResult ().getActionId ().getValue ();
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

    public String getRuleId ()
    {
        // Return the result produced by get value.

        return this.actionResult ().getRuleId ().getValue ();
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
        // Return the result produced by get specificity.

        return this.actionResult ().getSpecificity ();
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: HostedEvaluation
    //
    // Description:
    //
    //   Creates the HostedEvaluation instance from the supplied values.
    //
    // Arguments:
    //
    //   evaluationResult (EvaluationResult):
    //     The evaluation result to use.
    //
    //   revision (String):
    //     The revision to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public HostedEvaluation ( EvaluationResult evaluationResult, String revision )
    {
        // Validate the required evaluation result and revision before continuing.

        this.evaluationResult = Objects.requireNonNull ( evaluationResult, "evaluationResult" );
        this.revision         = Objects.requireNonNull ( revision, "revision" );
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: actionResult
    //
    // Description:
    //
    //   Performs the action result operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private ActionResult actionResult ()
    {
        // Reject the operation when evaluation result is not an action result.

        if ( !( this.evaluationResult instanceof ActionResult ) )
        {
            throw new IllegalStateException ( "The hosted evaluation has no action result." );
        }

        // Return the action result result to the caller.

        return (ActionResult) this.evaluationResult;
    }
}
