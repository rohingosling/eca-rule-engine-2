//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Provides the shared JSON boundary used by the production server and desktop client.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

//*********************************************************************************************************************
// Class: JsonSupport
//
// Description:
//
//   Provides the shared JSON boundary used by the production server and desktop client.
//
//*********************************************************************************************************************

public final class JsonSupport
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper ();

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: JsonSupport
    //
    // Description:
    //
    //   Creates the JsonSupport instance from the supplied values.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private JsonSupport ()
    {
        throw new UnsupportedOperationException ( "JsonSupport cannot be instantiated." );
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createStatusDocument
    //
    // Description:
    //
    //   Performs the create status document operation.
    //
    // Arguments:
    //
    //   status (String):
    //     The status to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static String createStatusDocument ( String status )
    {
        // Initialize the status node by applying create object node.

        ObjectNode statusNode = OBJECT_MAPPER.createObjectNode ();

        // Store status under "status" in the status node.

        statusNode.put ( "status", status );

        try
        {
            // Return the result produced by write value as string.

            return OBJECT_MAPPER.writeValueAsString ( statusNode );
        }

        // Handle JSON processing failures captured as exception.

        catch ( JsonProcessingException exception )
        {
            throw new IllegalStateException ( "The status document could not be serialized.", exception );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readStatus
    //
    // Description:
    //
    //   Performs the read status operation.
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

    public static String readStatus ( String document )
    {
        try
        {
            // Initialize the document node by applying read tree.

            JsonNode documentNode = OBJECT_MAPPER.readTree ( document );

            // Return the result produced by as text.

            return documentNode.path ( "status" ).asText ();
        }

        // Handle JSON processing failures captured as exception.

        catch ( JsonProcessingException exception )
        {
            throw new IllegalArgumentException ( "The status document is not valid JSON.", exception );
        }
    }
}
