//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Represents a lossless framework-neutral occurrence document before domain validation and compilation.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

//*********************************************************************************************************************
// Class: OccurrenceDocument
//
// Description:
//
//   Represents a lossless framework-neutral occurrence document before domain validation and compilation.
//
//*********************************************************************************************************************

public final class OccurrenceDocument
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final String eventId;
    private final Map <String, Object> payload;
    private final boolean payloadPresent;

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

    public String getEventId ()
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

    public Map <String, Object> getPayload ()
    {
        // Return the payload to the caller.

        return this.payload;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: isPayloadPresent
    //
    // Description:
    //
    //   Indicates whether payload present.
    //
    // Returns:
    //
    //   `true` when the condition is satisfied; otherwise `false`.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public boolean isPayloadPresent ()
    {
        // Return the payload present to the caller.

        return this.payloadPresent;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: OccurrenceDocument
    //
    // Description:
    //
    //   Creates the OccurrenceDocument instance from the supplied values.
    //
    // Arguments:
    //
    //   eventId (String):
    //     The event id to use.
    //
    //   payload (Map <String, ?>):
    //     The payload to use.
    //
    //   payloadPresent (boolean):
    //     The payload present to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public OccurrenceDocument (
        String eventId,
        Map <String, ?> payload,
        boolean payloadPresent
    )
    {
        // Validate the required event ID and payload before continuing.

        Objects.requireNonNull ( eventId, "eventId" );
        Objects.requireNonNull ( payload, "payload" );

        this.eventId        = eventId;

        // Update the payload from the immutable payload result.

        this.payload        = immutablePayload ( payload );
        this.payloadPresent = payloadPresent;
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: immutablePayload
    //
    // Description:
    //
    //   Performs the immutable payload operation.
    //
    // Arguments:
    //
    //   payload (Map <String, ?>):
    //     The payload to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Map <String, Object> immutablePayload ( Map <String, ?> payload )
    {
        // Initialize the copied payload with a new linked hash map.

        LinkedHashMap <String, Object> copiedPayload = new LinkedHashMap <String, Object> ();

        // Process each entry supplied by payload entry set.

        for ( Map.Entry <String, ?> entry : payload.entrySet () )
        {
            // Store entry value under objects require non null entry key "payload must not contain null keys" in the
            // copied payload.

            copiedPayload.put (
                Objects.requireNonNull ( entry.getKey (), "payload must not contain null keys" ),
                entry.getValue ()
            );
        }

        // Return an immutable copy of copied payload.

        return Collections.unmodifiableMap ( copiedPayload );
    }
}
