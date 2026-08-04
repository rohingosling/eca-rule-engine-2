//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Represents one validated server-evaluation response for presentation by the desktop client.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.application;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

//*********************************************************************************************************************
// Class: ClientEvaluationResult
//
// Description:
//
//   Represents one validated server-evaluation response for presentation by the desktop client.
//
//*********************************************************************************************************************

public final class ClientEvaluationResult
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final String outcome;
    private final String actionIdentifier;
    private final String ruleIdentifier;
    private final Integer specificity;
    private final String modelRevision;
    private final long serverElapsedMicroseconds;
    private final long roundTripMicroseconds;

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

    public String getOutcome ()
    {
        // Return the outcome to the caller.

        return this.outcome;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getModelRevision
    //
    // Description:
    //
    //   Returns the model revision.
    //
    // Returns:
    //
    //   The model revision.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String getModelRevision ()
    {
        // Return the model revision to the caller.

        return this.modelRevision;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getServerElapsedMicroseconds
    //
    // Description:
    //
    //   Returns the server elapsed microseconds.
    //
    // Returns:
    //
    //   The server elapsed microseconds.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public long getServerElapsedMicroseconds ()
    {
        // Return the server elapsed microseconds to the caller.

        return this.serverElapsedMicroseconds;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getRoundTripMicroseconds
    //
    // Description:
    //
    //   Returns the round trip microseconds.
    //
    // Returns:
    //
    //   The round trip microseconds.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public long getRoundTripMicroseconds ()
    {
        // Return the round trip microseconds to the caller.

        return this.roundTripMicroseconds;
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
        // Return whether the current value matches the comparison target.

        return this.outcome.equals ( "ACTION" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getActionIdentifier
    //
    // Description:
    //
    //   Returns the action identifier.
    //
    // Returns:
    //
    //   The action identifier.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Optional <String> getActionIdentifier ()
    {
        // Return an optional containing the value when it is available.

        return Optional.ofNullable ( this.actionIdentifier );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getRuleIdentifier
    //
    // Description:
    //
    //   Returns the rule identifier.
    //
    // Returns:
    //
    //   The rule identifier.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Optional <String> getRuleIdentifier ()
    {
        // Return an optional containing the value when it is available.

        return Optional.ofNullable ( this.ruleIdentifier );
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

    public OptionalInt getSpecificity ()
    {
        // Return the value selected according to specificity is unavailable.

        return this.specificity == null
            ? OptionalInt.empty ()
            : OptionalInt.of ( this.specificity.intValue () );
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: ClientEvaluationResult
    //
    // Description:
    //
    //   Creates the ClientEvaluationResult instance from the supplied values.
    //
    // Arguments:
    //
    //   outcome (String):
    //     The outcome to use.
    //
    //   actionIdentifier (String):
    //     The action identifier to use.
    //
    //   ruleIdentifier (String):
    //     The rule identifier to use.
    //
    //   specificity (Integer):
    //     The specificity to use.
    //
    //   modelRevision (String):
    //     The model revision to use.
    //
    //   serverElapsedMicroseconds (long):
    //     The server elapsed microseconds to use.
    //
    //   roundTripMicroseconds (long):
    //     The round trip microseconds to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ClientEvaluationResult (
        String outcome,
        String actionIdentifier,
        String ruleIdentifier,
        Integer specificity,
        String modelRevision,
        long serverElapsedMicroseconds,
        long roundTripMicroseconds
    )
    {
        // Perform the require outcome, require text, and require non negative calls required by the client evaluation
        // result operation.

        this.outcome                   = requireOutcome ( outcome );
        this.modelRevision             = requireText ( modelRevision, "modelRevision" );
        this.serverElapsedMicroseconds = requireNonNegative (
            serverElapsedMicroseconds,
            "serverElapsedMicroseconds"
        );
        this.roundTripMicroseconds     = requireNonNegative (
            roundTripMicroseconds,
            "roundTripMicroseconds"
        );

        // Handle the branch where outcome matches "action".

        if ( this.outcome.equals ( "ACTION" ) )
        {
            // Complete the client evaluation result step by calling require text.

            this.actionIdentifier = requireText ( actionIdentifier, "actionIdentifier" );
            this.ruleIdentifier   = requireText ( ruleIdentifier, "ruleIdentifier" );

            // Reject the operation when specificity is unavailable or specificity int value is less than 0.

            if ( specificity == null || specificity.intValue () < 0 )
            {
                throw new IllegalArgumentException (
                    "An ACTION evaluation requires a non-negative specificity."
                );
            }

            this.specificity = specificity;
        }

        // Handle the alternative path when the preceding condition is not satisfied.

        else
        {
            // Reject the operation when action identifier is available or rule identifier is available or specificity
            // is available.

            if ( actionIdentifier != null || ruleIdentifier != null || specificity != null )
            {
                throw new IllegalArgumentException (
                    "A NO_ACTION evaluation must not contain action, rule, or specificity fields."
                );
            }

            this.actionIdentifier = null;
            this.ruleIdentifier   = null;
            this.specificity      = null;
        }
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: withRoundTripMicroseconds
    //
    // Description:
    //
    //   Creates a ClientEvaluationResult instance with the round trip microseconds updated.
    //
    // Arguments:
    //
    //   elapsedMicroseconds (long):
    //     The elapsed microseconds to use.
    //
    // Returns:
    //
    //   The resulting ClientEvaluationResult instance.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ClientEvaluationResult withRoundTripMicroseconds ( long elapsedMicroseconds )
    {
        // Return a newly constructed client evaluation result containing the operation result.

        return new ClientEvaluationResult (
            this.outcome,
            this.actionIdentifier,
            this.ruleIdentifier,
            this.specificity,
            this.modelRevision,
            this.serverElapsedMicroseconds,
            elapsedMicroseconds
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requireOutcome
    //
    // Description:
    //
    //   Performs the require outcome operation.
    //
    // Arguments:
    //
    //   outcome (String):
    //     The outcome to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String requireOutcome ( String outcome )
    {
        // Initialize the required outcome by applying require text.

        String requiredOutcome = requireText ( outcome, "outcome" );

        // Reject the operation when required outcome differs from "action" and required outcome differs from "no
        // action".

        if ( !requiredOutcome.equals ( "ACTION" ) && !requiredOutcome.equals ( "NO_ACTION" ) )
        {
            throw new IllegalArgumentException ( "Unsupported evaluation outcome: " + requiredOutcome );
        }

        // Return the required outcome to the caller.

        return requiredOutcome;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requireText
    //
    // Description:
    //
    //   Performs the require text operation.
    //
    // Arguments:
    //
    //   value (String):
    //     The value to use.
    //
    //   fieldName (String):
    //     The field name to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String requireText ( String value, String fieldName )
    {
        // Initialize the required value by applying trim and require non null.

        String requiredValue = Objects.requireNonNull ( value, fieldName ).trim ();

        // Reject the operation when required value contains no values.

        if ( requiredValue.isEmpty () )
        {
            throw new IllegalArgumentException ( fieldName + " must not be blank." );
        }

        // Return the required value to the caller.

        return requiredValue;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requireNonNegative
    //
    // Description:
    //
    //   Performs the require non negative operation.
    //
    // Arguments:
    //
    //   value (long):
    //     The value to use.
    //
    //   fieldName (String):
    //     The field name to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static long requireNonNegative ( long value, String fieldName )
    {
        // Reject the operation when value is less than 0.

        if ( value < 0 )
        {
            throw new IllegalArgumentException ( fieldName + " must not be negative." );
        }

        // Return the value to the caller.

        return value;
    }
}
