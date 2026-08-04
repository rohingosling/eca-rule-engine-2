//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Publishes one immutable validated authoring model and its compiled mathematical-core representation.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.rohingosling.eca.domain.ActionId;
import com.rohingosling.eca.domain.CompiledConditionSet;
import com.rohingosling.eca.domain.CompiledRuleBase;
import com.rohingosling.eca.domain.ConcreteValue;
import com.rohingosling.eca.domain.EventDefinition;
import com.rohingosling.eca.domain.EventOccurrence;
import com.rohingosling.eca.domain.ParameterDefinition;
import com.rohingosling.eca.domain.ParameterId;
import com.rohingosling.eca.domain.ParameterType;
import com.rohingosling.eca.domain.Payload;
import com.rohingosling.eca.domain.PayloadValue;
import com.rohingosling.eca.domain.PresentNullPayloadValue;

//*********************************************************************************************************************
// Class: CompiledModel
//
// Description:
//
//   Publishes one immutable validated authoring model and its compiled mathematical-core representation.
//
//*********************************************************************************************************************

public final class CompiledModel
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final AuthoringModel authoringModel;
    private final String revision;
    private final Map <String, ParameterDefinition> parameterDefinitions;
    private final Map <String, EventDefinition> eventDefinitions;
    private final Map <String, CompiledConditionSet> conditionSets;
    private final Map <String, ActionId> actions;
    private final CompiledRuleBase ruleBase;

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getAuthoringModel
    //
    // Description:
    //
    //   Returns the authoring model.
    //
    // Returns:
    //
    //   The authoring model.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public AuthoringModel getAuthoringModel ()
    {
        // Return the authoring model to the caller.

        return this.authoringModel;
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
    // Method: getParameterDefinitions
    //
    // Description:
    //
    //   Returns the parameter definitions.
    //
    // Returns:
    //
    //   The parameter definitions.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Map <String, ParameterDefinition> getParameterDefinitions ()
    {
        // Return the parameter definitions to the caller.

        return this.parameterDefinitions;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getEventDefinitions
    //
    // Description:
    //
    //   Returns the event definitions.
    //
    // Returns:
    //
    //   The event definitions.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Map <String, EventDefinition> getEventDefinitions ()
    {
        // Return the event definitions to the caller.

        return this.eventDefinitions;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getConditionSets
    //
    // Description:
    //
    //   Returns the condition sets.
    //
    // Returns:
    //
    //   The condition sets.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Map <String, CompiledConditionSet> getConditionSets ()
    {
        // Return the condition sets to the caller.

        return this.conditionSets;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getActions
    //
    // Description:
    //
    //   Returns the actions.
    //
    // Returns:
    //
    //   The actions.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Map <String, ActionId> getActions ()
    {
        // Return the actions to the caller.

        return this.actions;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getRuleBase
    //
    // Description:
    //
    //   Returns the rule base.
    //
    // Returns:
    //
    //   The rule base.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public CompiledRuleBase getRuleBase ()
    {
        // Return the rule base to the caller.

        return this.ruleBase;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: CompiledModel
    //
    // Description:
    //
    //   Creates the CompiledModel instance from the supplied values.
    //
    // Arguments:
    //
    //   authoringModel (AuthoringModel):
    //     The authoring model to use.
    //
    //   revision (String):
    //     The revision to use.
    //
    //   parameterDefinitions (Map <String, ParameterDefinition>):
    //     The parameter definitions to use.
    //
    //   eventDefinitions (Map <String, EventDefinition>):
    //     The event definitions to use.
    //
    //   conditionSets (Map <String, CompiledConditionSet>):
    //     The condition sets to use.
    //
    //   actions (Map <String, ActionId>):
    //     The actions to use.
    //
    //   ruleBase (CompiledRuleBase):
    //     The rule base to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public CompiledModel (
        AuthoringModel authoringModel,
        String revision,
        Map <String, ParameterDefinition> parameterDefinitions,
        Map <String, EventDefinition> eventDefinitions,
        Map <String, CompiledConditionSet> conditionSets,
        Map <String, ActionId> actions,
        CompiledRuleBase ruleBase
    )
    {
        // Apply require non null and immutable map to the objects for the compiled model operation.

        this.authoringModel       = Objects.requireNonNull ( authoringModel, "authoringModel" );
        this.revision             = Objects.requireNonNull ( revision, "revision" );
        this.parameterDefinitions = immutableMap ( parameterDefinitions, "parameterDefinitions" );
        this.eventDefinitions     = immutableMap ( eventDefinitions, "eventDefinitions" );
        this.conditionSets        = immutableMap ( conditionSets, "conditionSets" );
        this.actions              = immutableMap ( actions, "actions" );
        this.ruleBase             = Objects.requireNonNull ( ruleBase, "ruleBase" );
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createOccurrence
    //
    // Description:
    //
    //   Performs the create occurrence operation.
    //
    // Arguments:
    //
    //   eventId (String):
    //     The event id to use.
    //
    //   payloadBindings (Map <String, ?>):
    //     The payload bindings to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public EventOccurrence createOccurrence ( String eventId, Map <String, ?> payloadBindings )
    {
        // Validate the required event ID and payload bindings before continuing.

        Objects.requireNonNull ( eventId, "eventId" );
        Objects.requireNonNull ( payloadBindings, "payloadBindings" );

        // Initialize the event definition by applying get.

        EventDefinition eventDefinition = this.eventDefinitions.get ( eventId );

        // Reject the operation when event definition is unavailable.

        if ( eventDefinition == null )
        {
            throw new IllegalArgumentException ( "Unknown event identifier: " + eventId );
        }

        // Initialize the compiled bindings with a new linked hash map.

        LinkedHashMap <ParameterId, PayloadValue> compiledBindings =
            new LinkedHashMap <ParameterId, PayloadValue> ();

        // Process each binding supplied by payload bindings entry set.

        for ( Map.Entry <String, ?> binding : payloadBindings.entrySet () )
        {
            // Initialize the parameter definition by applying get and get key.

            ParameterDefinition parameterDefinition = this.parameterDefinitions.get ( binding.getKey () );

            // Reject the operation when parameter definition is unavailable.

            if ( parameterDefinition == null )
            {
                throw new IllegalArgumentException ( "Unknown parameter identifier: " + binding.getKey () );
            }

            PayloadValue payloadValue;

            // Handle the branch where binding value is unavailable.

            if ( binding.getValue () == null )
            {
                // Construct the present null payload value instance required by the create occurrence operation.

                payloadValue = new PresentNullPayloadValue ( parameterDefinition );
            }

            // Handle the alternative path when the preceding condition is not satisfied.

            else
            {
                // Update the payload value from the get value result.

                payloadValue = new ConcreteValue (
                    parameterDefinition,
                    normalizeValue ( parameterDefinition, binding.getValue () )
                );
            }

            // Store payload value under parameter definition parameter ID in the compiled bindings.

            compiledBindings.put ( parameterDefinition.getParameterId (), payloadValue );
        }

        // Return a newly constructed event occurrence containing the operation result.

        return new EventOccurrence ( eventDefinition, new Payload ( compiledBindings ) );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createOccurrence
    //
    // Description:
    //
    //   Performs the create occurrence operation.
    //
    // Arguments:
    //
    //   occurrenceDocument (OccurrenceDocument):
    //     The occurrence document to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public EventOccurrence createOccurrence ( OccurrenceDocument occurrenceDocument )
    {
        // Validate the required occurrence document before continuing.

        Objects.requireNonNull ( occurrenceDocument, "occurrenceDocument" );

        // Initialize the event definition by applying get and get event ID.

        EventDefinition eventDefinition = this.eventDefinitions.get ( occurrenceDocument.getEventId () );

        // Reject the operation when event definition is unavailable.

        if ( eventDefinition == null )
        {
            throw new IllegalArgumentException (
                "Unknown event identifier: " + occurrenceDocument.getEventId ()
            );
        }

        // Reject the operation when occurrence document is not payload present and event definition permitted
        // parameter definitions contains values.

        if (
            !occurrenceDocument.isPayloadPresent ()
            && !eventDefinition.getPermittedParameterDefinitions ().isEmpty ()
        )
        {
            throw new IllegalArgumentException (
                "payload may be omitted only for an event with an empty payload schema."
            );
        }

        // Return the result produced by create occurrence.

        return this.createOccurrence ( occurrenceDocument.getEventId (), occurrenceDocument.getPayload () );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: normalizeValue
    //
    // Description:
    //
    //   Performs the normalize value operation.
    //
    // Arguments:
    //
    //   parameterDefinition (ParameterDefinition):
    //     The parameter definition to use.
    //
    //   value (Object):
    //     The value to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Object normalizeValue ( ParameterDefinition parameterDefinition, Object value )
    {
        // Stop this path and return its result when parameter definition parameter type differs from parameter type
        // integer.

        if ( parameterDefinition.getParameterType () != ParameterType.INTEGER )
        {
            // Return the value to the caller when parameter definition parameter type differs from parameter type
            // integer.

            return value;
        }

        // Stop this path and return its result when value is a big decimal.

        if ( value instanceof BigDecimal )
        {
            // Return the result produced by long value exact when value is a big decimal.

            return ( (BigDecimal) value ).longValueExact ();
        }

        // Stop this path and return its result when value is a big integer.

        if ( value instanceof BigInteger )
        {
            // Return the result produced by long value exact when value is a big integer.

            return ( (BigInteger) value ).longValueExact ();
        }

        // Stop this path and return its result when value is a number.

        if ( value instanceof Number )
        {
            // Return the result produced by long value when value is a number.

            return ( (Number) value ).longValue ();
        }

        // Return the value to the caller.

        return value;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: immutableMap
    //
    // Description:
    //
    //   Performs the immutable map operation.
    //
    // Arguments:
    //
    //   values (Map <String, T>):
    //     The values to use.
    //
    //   fieldName (String):
    //     The field name to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static <T> Map <String, T> immutableMap ( Map <String, T> values, String fieldName )
    {
        // Validate the required values before continuing.

        Objects.requireNonNull ( values, fieldName );

        // Return an immutable copy of new linked hash map string t values.

        return Collections.unmodifiableMap ( new LinkedHashMap <String, T> ( values ) );
    }
}
