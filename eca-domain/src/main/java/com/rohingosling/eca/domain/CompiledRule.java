//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Represents one immutable, well-formed event-condition-action rule.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.domain;

import java.util.Objects;

//*********************************************************************************************************************
// Class: CompiledRule
//
// Description:
//
//   Represents one immutable, well-formed event-condition-action rule.
//
//*********************************************************************************************************************

public final class CompiledRule
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final RuleId ruleId;
    private final EventId eventId;
    private final CompiledConditionSet conditionSet;
    private final ActionId actionId;

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

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
    // Method: getEventId
    //
    // Description:
    //
    //   Returns the event id.
    //
    // Returns:
    //
    //   The event id.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public EventId getEventId ()
    {
        // Return the event ID to the caller.

        return this.eventId;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getConditionSet
    //
    // Description:
    //
    //   Returns the condition set.
    //
    // Returns:
    //
    //   The condition set.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public CompiledConditionSet getConditionSet ()
    {
        // Return the condition set to the caller.

        return this.conditionSet;
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

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: CompiledRule
    //
    // Description:
    //
    //   Creates the CompiledRule instance from the supplied values.
    //
    // Arguments:
    //
    //   ruleId (RuleId):
    //     The rule id to use.
    //
    //   eventDefinition (EventDefinition):
    //     The event definition to use.
    //
    //   conditionSet (CompiledConditionSet):
    //     The condition set to use.
    //
    //   actionId (ActionId):
    //     The action id to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public CompiledRule (
        RuleId ruleId,
        EventDefinition eventDefinition,
        CompiledConditionSet conditionSet,
        ActionId actionId
    )
    {
        // Perform the require non null and validate concrete conditions calls required by the compiled rule operation.

        Objects.requireNonNull ( ruleId, "ruleId" );
        Objects.requireNonNull ( eventDefinition, "eventDefinition" );
        Objects.requireNonNull ( conditionSet, "conditionSet" );
        Objects.requireNonNull ( actionId, "actionId" );

        eventDefinition.validateConcreteConditions ( conditionSet );

        this.ruleId       = ruleId;

        // Update the event ID from the get event ID result.

        this.eventId      = eventDefinition.getEventId ();
        this.conditionSet = conditionSet;
        this.actionId     = actionId;
    }
}
