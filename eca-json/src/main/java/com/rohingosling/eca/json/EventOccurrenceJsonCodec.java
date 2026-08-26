//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Translates strict occurrence JSON into a lossless framework-neutral occurrence document.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.json;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rohingosling.eca.application.ClientSimulator.SimulationRequest;
import com.rohingosling.eca.model.OccurrenceDocument;

//*********************************************************************************************************************
// Class: EventOccurrenceJsonCodec
//
// Description:
//
//   Translates strict occurrence JSON into a lossless framework-neutral occurrence document.
//
//*********************************************************************************************************************

public final class EventOccurrenceJsonCodec
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    private static final Set <String> TOP_LEVEL_FIELDS = Set.of ( "eventId", "payload" );

    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final ObjectMapper objectMapper;

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: EventOccurrenceJsonCodec
    //
    // Description:
    //
    //   Creates the EventOccurrenceJsonCodec instance from the supplied values.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public EventOccurrenceJsonCodec ()
    {
        // Initialize the JSON factory by applying build, enable, and builder.

        JsonFactory jsonFactory = JsonFactory.builder ()
            .enable ( StreamReadFeature.STRICT_DUPLICATE_DETECTION )
            .build ();

        // Construct the object mapper instance required by the event occurrence JSON codec operation.

        this.objectMapper = new ObjectMapper ( jsonFactory );
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: read
    //
    // Description:
    //
    //   Performs the read operation.
    //
    // Arguments:
    //
    //   document (String):
    //     The document to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public OccurrenceDocument read ( String document )
    {
        // Validate the required document before continuing.

        Objects.requireNonNull ( document, "document" );

        try
        {
            // Initialize the parsed node by applying read tree and get bytes.

            JsonNode parsedNode = this.objectMapper.readTree ( document.getBytes ( StandardCharsets.UTF_8 ) );

            // Reject the operation when parsed node is unavailable or parsed node is not object.

            if ( parsedNode == null || !parsedNode.isObject () )
            {
                throw new IllegalArgumentException ( "The occurrence must be a JSON object." );
            }

            ObjectNode rootNode = (ObjectNode) parsedNode;

            // Reject unrecognized JSON fields before decoding the object.

            rejectUnknownFields ( rootNode );

            // Initialize the event ID node by applying get.

            JsonNode eventIdNode = rootNode.get ( "eventId" );

            // Reject the operation when event ID node is unavailable or event ID node is not textual.

            if ( eventIdNode == null || !eventIdNode.isTextual () )
            {
                throw new IllegalArgumentException ( "eventId must be a string." );
            }

            // Initialize the payload node by applying get.

            JsonNode payloadNode = rootNode.get ( "payload" );

            // Stop this path and return its result when payload node is unavailable.

            if ( payloadNode == null )
            {
                // Return a newly constructed occurrence document containing the operation result when payload node is
                // unavailable.

                return new OccurrenceDocument (
                    eventIdNode.textValue (),
                    Map.of (),
                    false
                );
            }

            // Reject the operation when payload node is not object.

            if ( !payloadNode.isObject () )
            {
                throw new IllegalArgumentException ( "payload must be a JSON object." );
            }

            // Return a newly constructed occurrence document containing the operation result.

            return new OccurrenceDocument (
                eventIdNode.textValue (),
                readPayload ( (ObjectNode) payloadNode ),
                true
            );
        }

        // Handle I/O failures captured as exception.

        catch ( IOException exception )
        {
            throw new IllegalArgumentException ( "The occurrence is not valid JSON.", exception );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: write
    //
    // Description:
    //
    //   Performs the write operation.
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

    public String write ( OccurrenceDocument occurrenceDocument )
    {
        // Validate the required occurrence document before continuing.

        Objects.requireNonNull ( occurrenceDocument, "occurrenceDocument" );

        // Initialize the root node by applying create object node.

        ObjectNode rootNode = this.objectMapper.createObjectNode ();

        // Store occurrence document event ID under "event id" in the root node.

        rootNode.put ( "eventId", occurrenceDocument.getEventId () );

        // Handle the branch where occurrence document is payload present.

        if ( occurrenceDocument.isPayloadPresent () )
        {
            // Initialize the payload node by applying create object node.

            ObjectNode payloadNode = this.objectMapper.createObjectNode ();

            // Perform the for each, sorted, stream, entry set, get payload, comparing by key, set, get key, value to
            // tree, and get value calls required by the write operation.

            occurrenceDocument.getPayload ().entrySet ().stream ()
                .sorted ( Map.Entry.comparingByKey () )
                .forEach (
                    entry -> payloadNode.set (
                        entry.getKey (),
                        this.objectMapper.valueToTree ( entry.getValue () )
                    )
                );

            rootNode.set ( "payload", payloadNode );
        }

        try
        {
            // Return the result produced by write value as string.

            return this.objectMapper.writeValueAsString ( rootNode );
        }

        // Handle JSON processing failures captured as exception.

        catch ( JsonProcessingException exception )
        {
            throw new IllegalStateException ( "The occurrence could not be serialized.", exception );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: write
    //
    // Description:
    //
    //   Performs the write operation.
    //
    // Arguments:
    //
    //   simulationRequest (SimulationRequest):
    //     The simulation request to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String write ( SimulationRequest simulationRequest )
    {
        // Validate the required simulation request before continuing.

        Objects.requireNonNull ( simulationRequest, "simulationRequest" );

        // Return the result produced by write.

        return this.write (
            new OccurrenceDocument (
                simulationRequest.getEventIdentifier (),
                simulationRequest.getPayload (),
                true
            )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readPayload
    //
    // Description:
    //
    //   Performs the read payload operation.
    //
    // Arguments:
    //
    //   payloadNode (ObjectNode):
    //     The payload node to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Map <String, Object> readPayload ( ObjectNode payloadNode )
    {
        // Prepare the payload and fields values needed by the read payload operation.

        LinkedHashMap <String, Object> payload = new LinkedHashMap <String, Object> ();
        Iterator <Map.Entry <String, JsonNode>> fields = payloadNode.properties ().iterator ();

        // Continue processing while fields has next.

        while ( fields.hasNext () )
        {
            // Prepare the field and value values needed by the read payload operation.

            Map.Entry <String, JsonNode> field = fields.next ();
            JsonNode value                     = field.getValue ();
            Object convertedValue;

            // Handle the branch where value is null.

            if ( value.isNull () )
            {
                convertedValue = null;
            }

            // Handle the alternative where value is textual.

            else if ( value.isTextual () )
            {
                // Update the converted value from the text value result.

                convertedValue = value.textValue ();
            }

            // Handle the alternative where value is boolean.

            else if ( value.isBoolean () )
            {
                // Update the converted value from the boolean value result.

                convertedValue = value.booleanValue ();
            }

            // Handle the alternative where value is integral number.

            else if ( value.isIntegralNumber () )
            {
                // Update the converted value from the big integer value result.

                convertedValue = value.canConvertToLong () ? value.longValue () : value.bigIntegerValue ();
            }

            // Handle the alternative where value is floating point number.

            else if ( value.isFloatingPointNumber () )
            {
                // Update the converted value from the as text result.

                convertedValue = new BigDecimal ( value.asText () );
            }

            // Reject the alternative state that remains after the preceding condition.

            else
            {
                throw new IllegalArgumentException (
                    "Payload property " + field.getKey () + " must be a scalar value or null."
                );
            }

            // Store converted value under field key in the payload.

            payload.put ( field.getKey (), convertedValue );
        }

        // Return the payload to the caller.

        return payload;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: rejectUnknownFields
    //
    // Description:
    //
    //   Performs the reject unknown fields operation.
    //
    // Arguments:
    //
    //   rootNode (ObjectNode):
    //     The root node to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void rejectUnknownFields ( ObjectNode rootNode )
    {
        // Initialize the field names by applying field names.

        Iterator <String> fieldNames = rootNode.fieldNames ();

        // Continue processing while field names has next.

        while ( fieldNames.hasNext () )
        {
            // Initialize the field name by applying next.

            String fieldName = fieldNames.next ();

            // Reject the operation when top level fields does not contain field name.

            if ( !TOP_LEVEL_FIELDS.contains ( fieldName ) )
            {
                throw new IllegalArgumentException ( "Unknown occurrence field: " + fieldName );
            }
        }
    }
}
