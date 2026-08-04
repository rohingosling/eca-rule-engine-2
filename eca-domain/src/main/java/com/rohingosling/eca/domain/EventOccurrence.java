//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Represents one valid immutable event occurrence and its current payload.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.domain;

import java.util.Objects;

//*********************************************************************************************************************
// Class: EventOccurrence
//
// Description:
//
//   Represents one valid immutable event occurrence and its current payload.
//
//*********************************************************************************************************************

public final class EventOccurrence
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final EventId eventId;
    private final Payload payload;

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
    // Method: getPayload
    //
    // Description:
    //
    //   Returns the payload.
    //
    // Returns:
    //
    //   The payload.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Payload getPayload ()
    {
        // Return the payload to the caller.

        return this.payload;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: EventOccurrence
    //
    // Description:
    //
    //   Creates the EventOccurrence instance from the supplied values.
    //
    // Arguments:
    //
    //   eventDefinition (EventDefinition):
    //     The event definition to use.
    //
    //   payload (Payload):
    //     The payload to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public EventOccurrence ( EventDefinition eventDefinition, Payload payload )
    {
        // Perform the require non null and validate payload calls required by the event occurrence operation.

        Objects.requireNonNull ( eventDefinition, "eventDefinition" );
        Objects.requireNonNull ( payload, "payload" );

        eventDefinition.validatePayload ( payload );

        // Update the event ID from the get event ID result.

        this.eventId = eventDefinition.getEventId ();
        this.payload = payload;
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

        // Stop this path and return its result when object is not an event occurrence.

        if ( !( object instanceof EventOccurrence ) )
        {
            // Return false for this outcome when object is not an event occurrence.

            return false;
        }

        EventOccurrence occurrence = (EventOccurrence) object;

        // Return whether event ID matches occurrence event ID and payload matches occurrence payload.

        return this.eventId.equals ( occurrence.eventId ) && this.payload.equals ( occurrence.payload );
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

        return Objects.hash ( this.eventId, this.payload );
    }
}
