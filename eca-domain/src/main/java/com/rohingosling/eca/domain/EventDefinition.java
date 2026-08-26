//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Defines an event identifier and the finite set of parameters permitted by its payload schema.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

//*********************************************************************************************************************
// Class: EventDefinition
//
// Description:
//
//   Defines an event identifier and the finite set of parameters permitted by its payload schema.
//
//*********************************************************************************************************************

public final class EventDefinition
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final EventId eventId;
    private final Map <ParameterId, ParameterDefinition> permittedParameterDefinitions;

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

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
    // Method: getPermittedParameterDefinitions
    //
    // Description:
    //
    //   Returns the permitted parameter definitions.
    //
    // Returns:
    //
    //   The permitted parameter definitions.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Map <ParameterId, ParameterDefinition> getPermittedParameterDefinitions ()
    {
        // Return the permitted parameter definitions to the caller.

        return this.permittedParameterDefinitions;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: EventDefinition
    //
    // Description:
    //
    //   Creates the EventDefinition instance from the supplied values.
    //
    // Arguments:
    //
    //   eventId (EventId):
    //     The event id to use.
    //
    //   permittedParameterDefinitions (Collection <ParameterDefinition>):
    //     The permitted parameter definitions to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public EventDefinition (
        EventId eventId,
        Collection <ParameterDefinition> permittedParameterDefinitions
    )
    {
        // Validate the required event ID and permitted parameter definitions before continuing.

        Objects.requireNonNull ( eventId, "eventId" );
        Objects.requireNonNull ( permittedParameterDefinitions, "permittedParameterDefinitions" );

        // Initialize the copied definitions with a new linked hash map.

        LinkedHashMap <ParameterId, ParameterDefinition> copiedDefinitions =
            new LinkedHashMap <ParameterId, ParameterDefinition> ();

        // Process each parameter definition supplied by permitted parameter definitions.

        for ( ParameterDefinition parameterDefinition : permittedParameterDefinitions )
        {
            // Validate the required parameter definition before continuing.

            Objects.requireNonNull (
                parameterDefinition,
                "permittedParameterDefinitions must not contain null"
            );

            // Initialize the previous definition by applying put and get parameter ID.

            ParameterDefinition previousDefinition = copiedDefinitions.put (
                parameterDefinition.getParameterId (),
                parameterDefinition
            );

            // Reject the operation when previous definition is available.

            if ( previousDefinition != null )
            {
                throw new IllegalArgumentException ( "Permitted event parameter identifiers must be unique." );
            }
        }

        this.eventId                       = eventId;

        // Update the permitted parameter definitions from the unmodifiable map result.

        this.permittedParameterDefinitions = Collections.unmodifiableMap ( copiedDefinitions );
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validatePayload
    //
    // Description:
    //
    //   Performs the validate payload operation.
    //
    // Arguments:
    //
    //   payload (Payload):
    //     The payload to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void validatePayload ( Payload payload )
    {
        // Validate the required payload before continuing.

        Objects.requireNonNull ( payload, "payload" );

        // Process each payload value supplied by payload bindings values.

        for ( PayloadValue payloadValue : payload.getBindings ().values () )
        {
            // Initialize the permitted definition by applying get, get parameter ID, and get parameter definition.

            ParameterDefinition permittedDefinition = this.permittedParameterDefinitions.get (
                payloadValue.getParameterDefinition ().getParameterId ()
            );

            // Reject the operation when payload value parameter definition differs from permitted definition.

            if ( !payloadValue.getParameterDefinition ().equals ( permittedDefinition ) )
            {
                throw new IllegalArgumentException (
                    "Payload parameter is not permitted by event " + this.eventId + "."
                );
            }
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validateConcreteConditions
    //
    // Description:
    //
    //   Performs the validate concrete conditions operation.
    //
    // Arguments:
    //
    //   conditionSet (CompiledConditionSet):
    //     The condition set to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void validateConcreteConditions ( CompiledConditionSet conditionSet )
    {
        // Validate the required condition set before continuing.

        Objects.requireNonNull ( conditionSet, "conditionSet" );

        // Process each condition value supplied by condition set bindings values.

        for ( ConditionValue conditionValue : conditionSet.getBindings ().values () )
        {
            // Skip the current item when condition value is not concrete.

            if ( !conditionValue.isConcrete () )
            {
                continue;
            }

            // Initialize the permitted definition by applying get, get parameter ID, and get parameter definition.

            ParameterDefinition permittedDefinition = this.permittedParameterDefinitions.get (
                conditionValue.getParameterDefinition ().getParameterId ()
            );

            // Reject the operation when condition value parameter definition differs from permitted definition.

            if ( !conditionValue.getParameterDefinition ().equals ( permittedDefinition ) )
            {
                throw new IllegalArgumentException (
                    "Concrete condition parameter is not permitted by event " + this.eventId + "."
                );
            }
        }
    }
}
