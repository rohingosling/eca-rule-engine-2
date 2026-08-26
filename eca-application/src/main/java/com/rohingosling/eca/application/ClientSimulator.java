//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Derives simulator fields from one authoring model and creates lossless typed occurrence documents.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.rohingosling.eca.model.AuthoringModel;

//*********************************************************************************************************************
// Class: ClientSimulator
//
// Description:
//
//   Derives simulator fields from one authoring model and creates lossless typed occurrence documents.
//
//*********************************************************************************************************************

public final class ClientSimulator
{
    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getEvents
    //
    // Description:
    //
    //   Returns the events.
    //
    // Arguments:
    //
    //   content (ClientDocumentSession.Content):
    //     The content to use.
    //
    // Returns:
    //
    //   The events.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public List <EventDescriptor> getEvents ( ClientDocumentSession.Content content )
    {
        // Prepare the model and event descriptors values needed by the get events operation.

        AuthoringModel model = Objects.requireNonNull ( content, "content" ).getAuthoringModel ();
        ArrayList <EventDescriptor> eventDescriptors = new ArrayList <EventDescriptor> ();

        // Process each event supplied by model events.

        for ( AuthoringModel.EventDefinition event : model.getEvents () )
        {
            // Prepare the payload and field descriptors values needed by the get events operation.

            AuthoringModel.PayloadDefinition payload = findPayload ( model, event.getPayloadId () )
                .orElse ( null );
            ArrayList <PayloadFieldDescriptor> fieldDescriptors =
                new ArrayList <PayloadFieldDescriptor> ();

            // Handle the branch where payload is available.

            if ( payload != null )
            {
                // Process each parameter identifier supplied by payload parameter IDs.

                for ( String parameterIdentifier : payload.getParameterIds () )
                {
                    // Perform the if present, find parameter, add, get ID, get name, get description, get type, and
                    // get enumeration values calls required by the get events operation.

                    findParameter ( model, parameterIdentifier ).ifPresent (
                        parameter -> fieldDescriptors.add (
                            new PayloadFieldDescriptor (
                                parameter.getId (),
                                parameter.getName (),
                                parameter.getDescription (),
                                parameter.getType (),
                                parameter.getEnumerationValues ()
                            )
                        )
                    );
                }
            }

            // Add new event descriptor event ID event name event description field descriptors to the event
            // descriptors.

            eventDescriptors.add (
                new EventDescriptor (
                    event.getId (),
                    event.getName (),
                    event.getDescription (),
                    fieldDescriptors
                )
            );
        }

        // Return an immutable copy of event descriptors.

        return Collections.unmodifiableList ( eventDescriptors );
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
    //   content (ClientDocumentSession.Content):
    //     The content to use.
    //
    //   eventIdentifier (String):
    //     The event identifier to use.
    //
    //   payloadDrafts (Map <String, PayloadValueDraft>):
    //     The payload drafts to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public SimulationRequest createOccurrence (
        ClientDocumentSession.Content content,
        String eventIdentifier,
        Map <String, PayloadValueDraft> payloadDrafts
    )
    {
        // Validate the required content and payload drafts before continuing.

        Objects.requireNonNull ( content, "content" );
        Objects.requireNonNull ( payloadDrafts, "payloadDrafts" );

        // Reject the operation when content has validation errors.

        if ( content.hasValidationErrors () )
        {
            throw new IllegalArgumentException (
                "Repair the model validation errors before running a simulation."
            );
        }

        // Prepare the event descriptor and payload values needed by the create occurrence operation.

        EventDescriptor eventDescriptor = this.getEvents ( content ).stream ()
            .filter ( event -> event.getIdentifier ().equals ( eventIdentifier ) )
            .findFirst ()
            .orElseThrow (
                () -> new IllegalArgumentException (
                    "The selected event is not defined by the current model."
                )
            );
        LinkedHashMap <String, Object> payload = new LinkedHashMap <String, Object> ();

        // Process each parameter identifier supplied by payload drafts key set.

        for ( String parameterIdentifier : payloadDrafts.keySet () )
        {
            // Initialize the known parameter by applying any match, stream, get payload fields, equals, and get
            // identifier.

            boolean knownParameter = eventDescriptor.getPayloadFields ().stream ()
                .anyMatch ( field -> field.getIdentifier ().equals ( parameterIdentifier ) );

            // Reject the operation when known parameter is false.

            if ( !knownParameter )
            {
                throw new IllegalArgumentException (
                    "The selected event does not permit payload property " + parameterIdentifier + "."
                );
            }
        }

        // Process each field descriptor supplied by event descriptor payload fields.

        for ( PayloadFieldDescriptor fieldDescriptor : eventDescriptor.getPayloadFields () )
        {
            // Initialize the draft by applying get or default, get identifier, and omitted.

            PayloadValueDraft draft = payloadDrafts.getOrDefault (
                fieldDescriptor.getIdentifier (),
                PayloadValueDraft.omitted ()
            );

            // Select the processing branch for draft state.

            switch ( draft.getState () )
            {
                // Handle omitted through this switch branch.

                case OMITTED:
                    break;

                // Handle null through this switch branch.

                case NULL:
                    payload.put ( fieldDescriptor.getIdentifier (), null );
                    break;

                // Handle concrete through this switch branch.

                case CONCRETE:
                    payload.put (
                        fieldDescriptor.getIdentifier (),
                        parseConcreteValue ( fieldDescriptor, draft.getText () )
                    );
                    break;

                // Handle the default case through this switch branch.

                default:
                    throw new IllegalStateException (
                        "Unsupported simulator payload state: " + draft.getState ()
                    );
            }
        }

        // Return a newly constructed simulation request containing the operation result.

        return new SimulationRequest ( eventDescriptor.getIdentifier (), payload );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: parseConcreteValue
    //
    // Description:
    //
    //   Performs the parse concrete value operation.
    //
    // Arguments:
    //
    //   fieldDescriptor (PayloadFieldDescriptor):
    //     The field descriptor to use.
    //
    //   text (String):
    //     The text to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Object parseConcreteValue (
        PayloadFieldDescriptor fieldDescriptor,
        String text
    )
    {
        // Validate the required text before continuing.

        String valueText = Objects.requireNonNull ( text, "text" );

        // Select the processing branch for field descriptor type.

        switch ( fieldDescriptor.getType () )
        {
            // Handle "string" through this switch branch.

            case "STRING":

                // Return the value text to the caller.

                return valueText;

            // Handle "integer" through this switch branch.

            case "INTEGER":
                try
                {
                    // Return the result produced by value of.

                    return Long.valueOf ( valueText.trim () );
                }

                // Handle number format failures captured as exception.

                catch ( NumberFormatException exception )
                {
                    throw new IllegalArgumentException (
                        fieldDescriptor.getName () + " must be a signed 64-bit integer.",
                        exception
                    );
                }

            // Handle "boolean" through this switch branch.

            case "BOOLEAN":

                // Stop this path and return its result when value text equals ignore case succeeds.

                if ( valueText.equalsIgnoreCase ( "true" ) )
                {
                    // Return the true to the caller when value text equals ignore case succeeds.

                    return Boolean.TRUE;
                }

                // Stop this path and return its result when value text equals ignore case succeeds.

                if ( valueText.equalsIgnoreCase ( "false" ) )
                {
                    // Return the false to the caller when value text equals ignore case succeeds.

                    return Boolean.FALSE;
                }

                throw new IllegalArgumentException (
                    fieldDescriptor.getName () + " must be true or false."
                );

            // Handle "enum" through this switch branch.

            case "ENUM":

                // Reject the operation when field descriptor enumeration values does not contain value text.

                if ( !fieldDescriptor.getEnumerationValues ().contains ( valueText ) )
                {
                    throw new IllegalArgumentException (
                        fieldDescriptor.getName () + " must use one of its defined enumeration values."
                    );
                }

                // Return the value text to the caller.

                return valueText;

            // Handle the default case through this switch branch.

            default:
                throw new IllegalArgumentException (
                    fieldDescriptor.getName ()
                        + " uses unsupported parameter type "
                        + fieldDescriptor.getType ()
                        + "."
                );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: findPayload
    //
    // Description:
    //
    //   Performs the find payload operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   payloadIdentifier (String):
    //     The payload identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Optional <AuthoringModel.PayloadDefinition> findPayload (
        AuthoringModel model,
        String payloadIdentifier
    )
    {
        // Return the result produced by find first.

        return model.getPayloads ().stream ()
            .filter ( payload -> payload.getId ().equals ( payloadIdentifier ) )
            .findFirst ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: findParameter
    //
    // Description:
    //
    //   Performs the find parameter operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   parameterIdentifier (String):
    //     The parameter identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Optional <AuthoringModel.ParameterDefinition> findParameter (
        AuthoringModel model,
        String parameterIdentifier
    )
    {
        // Return the result produced by find first.

        return model.getParameters ().stream ()
            .filter ( parameter -> parameter.getId ().equals ( parameterIdentifier ) )
            .findFirst ();
    }

    //=================================================================================================================
    // User Defined Data Types
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Enum: PayloadValueState
    //
    // Description:
    //
    //   Enumerates the supported payload value state values.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public enum PayloadValueState
    {
        OMITTED,
        NULL,
        CONCRETE
    }

    //*****************************************************************************************************************
    // Class: PayloadValueDraft
    //
    // Description:
    //
    //   Provides the payload value draft behavior.
    //
    //*****************************************************************************************************************

    public static final class PayloadValueDraft
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final PayloadValueState state;
        private final String text;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getState
        //
        // Description:
        //
        //   Returns the state.
        //
        // Returns:
        //
        //   The state.
        //
        //-------------------------------------------------------------------------------------------------------------

        public PayloadValueState getState ()
        {
            // Return the state to the caller.

            return this.state;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getText
        //
        // Description:
        //
        //   Returns the text.
        //
        // Returns:
        //
        //   The text.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getText ()
        {
            // Return the text to the caller.

            return this.text;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: PayloadValueDraft
        //
        // Description:
        //
        //   Creates the PayloadValueDraft instance from the supplied values.
        //
        // Arguments:
        //
        //   state (PayloadValueState):
        //     The state to use.
        //
        //   text (String):
        //     The text to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private PayloadValueDraft ( PayloadValueState state, String text )
        {
            // Validate the required state and text before continuing.

            this.state = Objects.requireNonNull ( state, "state" );
            this.text  = Objects.requireNonNull ( text, "text" );
        }

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: omitted
        //
        // Description:
        //
        //   Creates a PayloadValueDraft instance for the omitted case.
        //
        // Returns:
        //
        //   The resulting PayloadValueDraft instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        public static PayloadValueDraft omitted ()
        {
            // Return a newly constructed payload value draft containing the operation result.

            return new PayloadValueDraft ( PayloadValueState.OMITTED, "" );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: nullValue
        //
        // Description:
        //
        //   Creates a PayloadValueDraft instance for the null value case.
        //
        // Returns:
        //
        //   The resulting PayloadValueDraft instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        public static PayloadValueDraft nullValue ()
        {
            // Return a newly constructed payload value draft containing the operation result.

            return new PayloadValueDraft ( PayloadValueState.NULL, "" );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: concrete
        //
        // Description:
        //
        //   Creates a PayloadValueDraft instance for the concrete case.
        //
        // Arguments:
        //
        //   text (String):
        //     The text to use.
        //
        // Returns:
        //
        //   The resulting PayloadValueDraft instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        public static PayloadValueDraft concrete ( String text )
        {
            // Return a newly constructed payload value draft containing the operation result.

            return new PayloadValueDraft ( PayloadValueState.CONCRETE, text );
        }
    }

    //*****************************************************************************************************************
    // Class: SimulationRequest
    //
    // Description:
    //
    //   Provides the simulation request behavior.
    //
    //*****************************************************************************************************************

    public static final class SimulationRequest
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String eventIdentifier;
        private final Map <String, Object> payload;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getEventIdentifier
        //
        // Description:
        //
        //   Returns the event identifier.
        //
        // Returns:
        //
        //   The event identifier.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getEventIdentifier ()
        {
            // Return the event identifier to the caller.

            return this.eventIdentifier;
        }

        //-------------------------------------------------------------------------------------------------------------
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
        //-------------------------------------------------------------------------------------------------------------

        public Map <String, Object> getPayload ()
        {
            // Return the payload to the caller.

            return this.payload;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: SimulationRequest
        //
        // Description:
        //
        //   Creates the SimulationRequest instance from the supplied values.
        //
        // Arguments:
        //
        //   eventIdentifier (String):
        //     The event identifier to use.
        //
        //   payload (Map <String, Object>):
        //     The payload to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private SimulationRequest ( String eventIdentifier, Map <String, Object> payload )
        {
            // Perform the require non null and unmodifiable map calls required by the simulation request operation.

            this.eventIdentifier = Objects.requireNonNull ( eventIdentifier, "eventIdentifier" );
            this.payload         = Collections.unmodifiableMap (
                new LinkedHashMap <String, Object> ( payload )
            );
        }
    }

    //*****************************************************************************************************************
    // Class: EventDescriptor
    //
    // Description:
    //
    //   Provides the event descriptor behavior.
    //
    //*****************************************************************************************************************

    public static final class EventDescriptor
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String identifier;
        private final String name;
        private final String description;
        private final List <PayloadFieldDescriptor> payloadFields;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getIdentifier
        //
        // Description:
        //
        //   Returns the identifier.
        //
        // Returns:
        //
        //   The identifier.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getIdentifier ()
        {
            // Return the identifier to the caller.

            return this.identifier;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getName
        //
        // Description:
        //
        //   Returns the name.
        //
        // Returns:
        //
        //   The name.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getName ()
        {
            // Return the name to the caller.

            return this.name;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getDescription
        //
        // Description:
        //
        //   Returns the description.
        //
        // Returns:
        //
        //   The description.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getDescription ()
        {
            // Return the description to the caller.

            return this.description;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getPayloadFields
        //
        // Description:
        //
        //   Returns the payload fields.
        //
        // Returns:
        //
        //   The payload fields.
        //
        //-------------------------------------------------------------------------------------------------------------

        public List <PayloadFieldDescriptor> getPayloadFields ()
        {
            // Return the payload fields to the caller.

            return this.payloadFields;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: EventDescriptor
        //
        // Description:
        //
        //   Creates the EventDescriptor instance from the supplied values.
        //
        // Arguments:
        //
        //   identifier (String):
        //     The identifier to use.
        //
        //   name (String):
        //     The name to use.
        //
        //   description (String):
        //     The description to use.
        //
        //   payloadFields (List <PayloadFieldDescriptor>):
        //     The payload fields to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private EventDescriptor (
            String identifier,
            String name,
            String description,
            List <PayloadFieldDescriptor> payloadFields
        )
        {
            // Perform the require non null and copy of calls required by the event descriptor operation.

            this.identifier    = Objects.requireNonNull ( identifier, "identifier" );
            this.name          = Objects.requireNonNull ( name, "name" );
            this.description   = Objects.requireNonNull ( description, "description" );
            this.payloadFields = List.copyOf ( payloadFields );
        }

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: toString
        //
        // Description:
        //
        //   Creates the string representation of this value.
        //
        // Returns:
        //
        //   The string representation of this value.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public String toString ()
        {
            // Return the composed to string value.

            return this.name + " (" + this.identifier + ")";
        }
    }

    //*****************************************************************************************************************
    // Class: PayloadFieldDescriptor
    //
    // Description:
    //
    //   Provides the payload field descriptor behavior.
    //
    //*****************************************************************************************************************

    public static final class PayloadFieldDescriptor
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String identifier;
        private final String name;
        private final String description;
        private final String type;
        private final List <String> enumerationValues;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getIdentifier
        //
        // Description:
        //
        //   Returns the identifier.
        //
        // Returns:
        //
        //   The identifier.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getIdentifier ()
        {
            // Return the identifier to the caller.

            return this.identifier;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getName
        //
        // Description:
        //
        //   Returns the name.
        //
        // Returns:
        //
        //   The name.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getName ()
        {
            // Return the name to the caller.

            return this.name;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getDescription
        //
        // Description:
        //
        //   Returns the description.
        //
        // Returns:
        //
        //   The description.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getDescription ()
        {
            // Return the description to the caller.

            return this.description;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getType
        //
        // Description:
        //
        //   Returns the type.
        //
        // Returns:
        //
        //   The type.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getType ()
        {
            // Return the type to the caller.

            return this.type;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getEnumerationValues
        //
        // Description:
        //
        //   Returns the enumeration values.
        //
        // Returns:
        //
        //   The enumeration values.
        //
        //-------------------------------------------------------------------------------------------------------------

        public List <String> getEnumerationValues ()
        {
            // Return the enumeration values to the caller.

            return this.enumerationValues;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: PayloadFieldDescriptor
        //
        // Description:
        //
        //   Creates the PayloadFieldDescriptor instance from the supplied values.
        //
        // Arguments:
        //
        //   identifier (String):
        //     The identifier to use.
        //
        //   name (String):
        //     The name to use.
        //
        //   description (String):
        //     The description to use.
        //
        //   type (String):
        //     The type to use.
        //
        //   enumerationValues (List <String>):
        //     The enumeration values to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private PayloadFieldDescriptor (
            String identifier,
            String name,
            String description,
            String type,
            List <String> enumerationValues
        )
        {
            // Perform the require non null and copy of calls required by the payload field descriptor operation.

            this.identifier        = Objects.requireNonNull ( identifier, "identifier" );
            this.name              = Objects.requireNonNull ( name, "name" );
            this.description       = Objects.requireNonNull ( description, "description" );
            this.type              = Objects.requireNonNull ( type, "type" );
            this.enumerationValues = List.copyOf ( enumerationValues );
        }
    }
}
