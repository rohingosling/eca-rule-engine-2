//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Implements strict explicit Jackson codecs, stable pretty JSON, canonical JSON, and SHA-256 revisions.
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rohingosling.eca.model.AuthoringModel;
import com.rohingosling.eca.model.AuthoringModelCompiler;
import com.rohingosling.eca.model.CompiledModel;

//*********************************************************************************************************************
// Class: AuthoringModelJsonCodec
//
// Description:
//
//   Implements strict explicit Jackson codecs, stable pretty JSON, canonical JSON, and SHA-256 revisions.
//
//*********************************************************************************************************************

public final class AuthoringModelJsonCodec
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    private static final Set <String> TOP_LEVEL_FIELDS = fields (
        "schemaVersion",
        "modelId",
        "name",
        "description",
        "parameters",
        "payloads",
        "events",
        "conditions",
        "conditionSets",
        "actions",
        "rules"
    );

    private static final Set <String> PARAMETER_FIELDS =
        fields ( "id", "name", "description", "type", "enumValues" );
    private static final Set <String> PAYLOAD_FIELDS =
        fields ( "id", "name", "description", "parameterIds" );
    private static final Set <String> EVENT_FIELDS =
        fields ( "id", "name", "description", "payloadId" );
    private static final Set <String> CONDITION_FIELDS =
        fields ( "id", "name", "description", "parameterId", "operator", "value", "secondValue" );
    private static final Set <String> CONDITION_SET_FIELDS =
        fields ( "id", "name", "description", "bindings", "conditionIds" );
    private static final Set <String> ACTION_FIELDS =
        fields ( "id", "name", "description" );
    private static final Set <String> RULE_FIELDS =
        fields ( "id", "name", "description", "eventId", "conditionSetId", "actionId" );

    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final ObjectMapper objectMapper;

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: AuthoringModelJsonCodec
    //
    // Description:
    //
    //   Creates the AuthoringModelJsonCodec instance from the supplied values.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public AuthoringModelJsonCodec ()
    {
        // Initialize the JSON factory by applying build, enable, and builder.

        JsonFactory jsonFactory = JsonFactory.builder ()
            .enable ( StreamReadFeature.STRICT_DUPLICATE_DETECTION )
            .build ();

        // Construct the object mapper instance required by the authoring model JSON codec operation.

        this.objectMapper = new ObjectMapper ( jsonFactory );

        // Enable the required JSON mapper feature.

        this.objectMapper.enable ( DeserializationFeature.FAIL_ON_TRAILING_TOKENS );
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

    public AuthoringModel read ( String document )
    {
        // Validate the required document before continuing.

        Objects.requireNonNull ( document, "document" );

        // Return the result produced by read.

        return this.read ( document.getBytes ( StandardCharsets.UTF_8 ) );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: read
    //
    // Description:
    //
    //   Performs the read operation.
    //
    // Arguments:
    //
    //   document (byte []):
    //     The document to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public AuthoringModel read ( byte [] document )
    {
        // Validate the required document before continuing.

        Objects.requireNonNull ( document, "document" );

        try
        {
            // Prepare the root node and root values needed by the read operation.

            JsonNode rootNode = this.objectMapper.readTree ( document );
            ObjectNode root   = requireObject ( rootNode, "$" );

            // Reject unrecognized JSON fields before decoding the object.

            rejectUnknownFields ( root, TOP_LEVEL_FIELDS, "$" );

            // Return a newly constructed authoring model containing the operation result.

            return new AuthoringModel (
                requireText ( root, "schemaVersion", "$" ),
                requireText ( root, "modelId", "$" ),
                requireText ( root, "name", "$" ),
                requireText ( root, "description", "$" ),
                readParameters ( requireArray ( root, "parameters", "$" ) ),
                readPayloads ( requireArray ( root, "payloads", "$" ) ),
                readEvents ( requireArray ( root, "events", "$" ) ),
                readConditions ( requireArray ( root, "conditions", "$" ) ),
                readConditionSets ( requireArray ( root, "conditionSets", "$" ) ),
                readActions ( requireArray ( root, "actions", "$" ) ),
                readRules ( requireArray ( root, "rules", "$" ) )
            );
        }

        // Handle I/O failures captured as exception.

        catch ( IOException exception )
        {
            throw new IllegalArgumentException ( "The authoring model is not valid JSON.", exception );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: writePretty
    //
    // Description:
    //
    //   Performs the write pretty operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String writePretty ( AuthoringModel model )
    {
        // Validate the required model before continuing.

        Objects.requireNonNull ( model, "model" );

        try
        {
            // Configure an explicit stable two-space object indenter instead of relying on the dependency default.

            DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter ();

            prettyPrinter.indentObjectsWith ( new DefaultIndenter ( "  ", "\n" ) );

            // Return the composed write pretty value.

            return this.objectMapper.writer ( prettyPrinter ).writeValueAsString (
                this.createCanonicalTree ( model )
            ) + "\n";
        }

        // Handle JSON processing failures captured as exception.

        catch ( JsonProcessingException exception )
        {
            throw new IllegalStateException ( "The authoring model could not be serialized.", exception );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: writeCanonical
    //
    // Description:
    //
    //   Performs the write canonical operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public byte [] writeCanonical ( AuthoringModel model )
    {
        // Validate the required model before continuing.

        Objects.requireNonNull ( model, "model" );

        try
        {
            // Return the result produced by write value as bytes.

            return this.objectMapper.writeValueAsBytes ( this.createCanonicalTree ( model ) );
        }

        // Handle JSON processing failures captured as exception.

        catch ( JsonProcessingException exception )
        {
            throw new IllegalStateException ( "The authoring model could not be serialized.", exception );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: revision
    //
    // Description:
    //
    //   Performs the revision operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String revision ( AuthoringModel model )
    {
        try
        {
            // Prepare the digest and hexadecimal digest values needed by the revision operation.

            byte [] digest = MessageDigest.getInstance ( "SHA-256" ).digest ( this.writeCanonical ( model ) );
            StringBuilder hexadecimalDigest = new StringBuilder ( digest.length * 2 );

            // Process each value supplied by digest.

            for ( byte value : digest )
            {
                // Perform the append and format calls required by the revision operation.

                hexadecimalDigest.append ( String.format ( "%02x", value & 0xff ) );
            }

            // Return the composed revision value.

            return "sha256:" + hexadecimalDigest;
        }

        // Handle no such algorithm failures captured as exception.

        catch ( NoSuchAlgorithmException exception )
        {
            throw new IllegalStateException ( "SHA-256 is not available.", exception );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readAndCompile
    //
    // Description:
    //
    //   Performs the read and compile operation.
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

    public CompiledModel readAndCompile ( String document )
    {
        // Initialize the model by applying read.

        AuthoringModel model = this.read ( document );

        // Return the result produced by compile.

        return new AuthoringModelCompiler ().compile ( model, this.revision ( model ) );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createCanonicalTree
    //
    // Description:
    //
    //   Performs the create canonical tree operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private ObjectNode createCanonicalTree ( AuthoringModel model )
    {
        // Initialize the root by applying create object node.

        ObjectNode root = this.objectMapper.createObjectNode ();

        // Perform the put, get schema version, get model ID, get name, get description, set, write parameters, get
        // parameters, write payloads, get payloads, write events, get events, write conditions, get conditions, write
        // condition sets, get condition sets, write actions, get actions, write rules, and get rules calls required by
        // the create canonical tree operation.

        root.put ( "schemaVersion", model.getSchemaVersion () );
        root.put ( "modelId", model.getModelId () );
        root.put ( "name", model.getName () );
        root.put ( "description", model.getDescription () );
        root.set ( "parameters", this.writeParameters ( model.getParameters () ) );
        root.set ( "payloads", this.writePayloads ( model.getPayloads () ) );
        root.set ( "events", this.writeEvents ( model.getEvents () ) );
        root.set ( "conditions", this.writeConditions ( model.getConditions () ) );
        root.set ( "conditionSets", this.writeConditionSets ( model.getConditionSets () ) );
        root.set ( "actions", this.writeActions ( model.getActions () ) );
        root.set ( "rules", this.writeRules ( model.getRules () ) );

        // Return the root to the caller.

        return root;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readParameters
    //
    // Description:
    //
    //   Performs the read parameters operation.
    //
    // Arguments:
    //
    //   nodes (ArrayNode):
    //     The nodes to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static List <AuthoringModel.ParameterDefinition> readParameters ( ArrayNode nodes )
    {
        // Initialize the parameters with a new array list.

        ArrayList <AuthoringModel.ParameterDefinition> parameters =
            new ArrayList <AuthoringModel.ParameterDefinition> ();

        // Repeat the loop while i is less than nodes size.

        for ( int i = 0; i < nodes.size (); i++ )
        {
            String path      = "$.parameters[" + i + "]";

            // Prepare the node and enum node values needed by the read parameters operation.

            ObjectNode node  = requireObject ( nodes.get ( i ), path );
            JsonNode enumNode = node.get ( "enumValues" );

            // Reject unrecognized JSON fields before decoding the object.

            rejectUnknownFields ( node, PARAMETER_FIELDS, path );

            // Initialize the enumeration values by applying of, read text array, and require array.

            List <String> enumerationValues = enumNode == null
                ? List.of ()
                : readTextArray ( requireArray ( node, "enumValues", path ), path + ".enumValues" );

            // Add new authoring model parameter definition require text node "id" path require to the parameters.

            parameters.add (
                new AuthoringModel.ParameterDefinition (
                    requireText ( node, "id", path ),
                    requireText ( node, "name", path ),
                    requireText ( node, "description", path ),
                    requireText ( node, "type", path ),
                    enumerationValues
                )
            );
        }

        // Return the parameters to the caller.

        return parameters;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readPayloads
    //
    // Description:
    //
    //   Performs the read payloads operation.
    //
    // Arguments:
    //
    //   nodes (ArrayNode):
    //     The nodes to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static List <AuthoringModel.PayloadDefinition> readPayloads ( ArrayNode nodes )
    {
        // Initialize the payloads with a new array list.

        ArrayList <AuthoringModel.PayloadDefinition> payloads =
            new ArrayList <AuthoringModel.PayloadDefinition> ();

        // Repeat the loop while i is less than nodes size.

        for ( int i = 0; i < nodes.size (); i++ )
        {
            String path     = "$.payloads[" + i + "]";

            // Initialize the node by applying require object and get.

            ObjectNode node = requireObject ( nodes.get ( i ), path );

            // Apply reject unknown fields, add, require text, read text array, and require array to the payloads for
            // the read payloads operation.

            rejectUnknownFields ( node, PAYLOAD_FIELDS, path );
            payloads.add (
                new AuthoringModel.PayloadDefinition (
                    requireText ( node, "id", path ),
                    requireText ( node, "name", path ),
                    requireText ( node, "description", path ),
                    readTextArray (
                        requireArray ( node, "parameterIds", path ),
                        path + ".parameterIds"
                    )
                )
            );
        }

        // Return the payloads to the caller.

        return payloads;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readEvents
    //
    // Description:
    //
    //   Performs the read events operation.
    //
    // Arguments:
    //
    //   nodes (ArrayNode):
    //     The nodes to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static List <AuthoringModel.EventDefinition> readEvents ( ArrayNode nodes )
    {
        // Initialize the events with a new array list.

        ArrayList <AuthoringModel.EventDefinition> events =
            new ArrayList <AuthoringModel.EventDefinition> ();

        // Repeat the loop while i is less than nodes size.

        for ( int i = 0; i < nodes.size (); i++ )
        {
            String path     = "$.events[" + i + "]";

            // Initialize the node by applying require object and get.

            ObjectNode node = requireObject ( nodes.get ( i ), path );

            // Apply reject unknown fields, add, and require text to the events for the read events operation.

            rejectUnknownFields ( node, EVENT_FIELDS, path );
            events.add (
                new AuthoringModel.EventDefinition (
                    requireText ( node, "id", path ),
                    requireText ( node, "name", path ),
                    requireText ( node, "description", path ),
                    requireText ( node, "payloadId", path )
                )
            );
        }

        // Return the events to the caller.

        return events;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readConditions
    //
    // Description:
    //
    //   Performs the read conditions operation.
    //
    // Arguments:
    //
    //   nodes (ArrayNode):
    //     The nodes to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static List <AuthoringModel.ConditionDefinition> readConditions ( ArrayNode nodes )
    {
        // Initialize the conditions with a new array list.

        ArrayList <AuthoringModel.ConditionDefinition> conditions =
            new ArrayList <AuthoringModel.ConditionDefinition> ();

        // Repeat the loop while i is less than nodes size.

        for ( int i = 0; i < nodes.size (); i++ )
        {
            String path     = "$.conditions[" + i + "]";

            // Initialize the node by applying require object and get.

            ObjectNode node = requireObject ( nodes.get ( i ), path );

            // Reject unrecognized JSON fields before decoding the object.

            rejectUnknownFields ( node, CONDITION_FIELDS, path );

            // Handle the branch where node has has.

            if ( node.has ( "operator" ) )
            {
                // Prepare the operator and operand count values needed by the read conditions operation.

                String operator = requireText ( node, "operator", path );
                int operandCount = conditionOperandCount ( operator );

                // Reject the operation when operand count equals 0 and node has has or node has has.

                if ( operandCount == 0 && ( node.has ( "value" ) || node.has ( "secondValue" ) ) )
                {
                    throw new IllegalArgumentException (
                        path + " must not define operands for operator " + operator + "."
                    );
                }

                // Reject the operation when operand count equals 1 and node has has.

                if ( operandCount == 1 && node.has ( "secondValue" ) )
                {
                    throw new IllegalArgumentException (
                        path + " must not define secondValue for operator " + operator + "."
                    );
                }

                // Add new authoring model condition definition require text node "id" path require to the conditions.

                conditions.add (
                    new AuthoringModel.ConditionDefinition (
                        requireText ( node, "id", path ),
                        requireText ( node, "name", path ),
                        requireText ( node, "description", path ),
                        requireText ( node, "parameterId", path ),
                        operator,
                        readOptionalValue ( node, "value", path ),
                        readOptionalValue ( node, "secondValue", path )
                    )
                );
            }

            // Handle the alternative path when the preceding condition is not satisfied.

            else
            {
                // Reject the operation when node has has or node has has.

                if ( node.has ( "value" ) || node.has ( "secondValue" ) )
                {
                    throw new IllegalArgumentException (
                        path + " must define operator before defining condition operands."
                    );
                }

                // Add new authoring model condition definition require text node "id" path require to the conditions.

                conditions.add (
                    new AuthoringModel.ConditionDefinition (
                        requireText ( node, "id", path ),
                        requireText ( node, "name", path ),
                        requireText ( node, "description", path ),
                        requireText ( node, "parameterId", path )
                    )
                );
            }
        }

        // Return the conditions to the caller.

        return conditions;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readConditionSets
    //
    // Description:
    //
    //   Performs the read condition sets operation.
    //
    // Arguments:
    //
    //   nodes (ArrayNode):
    //     The nodes to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static List <AuthoringModel.ConditionSetDefinition> readConditionSets ( ArrayNode nodes )
    {
        // Initialize the condition sets with a new array list.

        ArrayList <AuthoringModel.ConditionSetDefinition> conditionSets =
            new ArrayList <AuthoringModel.ConditionSetDefinition> ();

        // Repeat the loop while i is less than nodes size.

        for ( int i = 0; i < nodes.size (); i++ )
        {
            String path     = "$.conditionSets[" + i + "]";

            // Initialize the node by applying require object and get.

            ObjectNode node = requireObject ( nodes.get ( i ), path );

            // Reject unrecognized JSON fields before decoding the object.

            rejectUnknownFields ( node, CONDITION_SET_FIELDS, path );

            // Handle the branch where node has has.

            if ( node.has ( "conditionIds" ) )
            {
                // Reject the operation when node has has.

                if ( node.has ( "bindings" ) )
                {
                    throw new IllegalArgumentException (
                        path + " must use conditionIds or legacy bindings, not both."
                    );
                }

                // Add new authoring model condition set definition require text node "id" path requ to the condition
                // sets.

                conditionSets.add (
                    new AuthoringModel.ConditionSetDefinition (
                        requireText ( node, "id", path ),
                        requireText ( node, "name", path ),
                        requireText ( node, "description", path ),
                        readTextArray (
                            requireArray ( node, "conditionIds", path ),
                            path + ".conditionIds"
                        )
                    )
                );
            }

            // Handle the alternative path when the preceding condition is not satisfied.

            else
            {
                // Add new authoring model condition set definition require text node "id" path requ to the condition
                // sets.

                conditionSets.add (
                    new AuthoringModel.ConditionSetDefinition (
                        requireText ( node, "id", path ),
                        requireText ( node, "name", path ),
                        requireText ( node, "description", path ),
                        readValueMap ( requireObjectMember ( node, "bindings", path ), path + ".bindings" )
                    )
                );
            }
        }

        // Return the condition sets to the caller.

        return conditionSets;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readActions
    //
    // Description:
    //
    //   Performs the read actions operation.
    //
    // Arguments:
    //
    //   nodes (ArrayNode):
    //     The nodes to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static List <AuthoringModel.ActionDefinition> readActions ( ArrayNode nodes )
    {
        // Initialize the actions with a new array list.

        ArrayList <AuthoringModel.ActionDefinition> actions =
            new ArrayList <AuthoringModel.ActionDefinition> ();

        // Repeat the loop while i is less than nodes size.

        for ( int i = 0; i < nodes.size (); i++ )
        {
            String path     = "$.actions[" + i + "]";

            // Initialize the node by applying require object and get.

            ObjectNode node = requireObject ( nodes.get ( i ), path );

            // Apply reject unknown fields, add, and require text to the actions for the read actions operation.

            rejectUnknownFields ( node, ACTION_FIELDS, path );
            actions.add (
                new AuthoringModel.ActionDefinition (
                    requireText ( node, "id", path ),
                    requireText ( node, "name", path ),
                    requireText ( node, "description", path )
                )
            );
        }

        // Return the actions to the caller.

        return actions;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readRules
    //
    // Description:
    //
    //   Performs the read rules operation.
    //
    // Arguments:
    //
    //   nodes (ArrayNode):
    //     The nodes to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static List <AuthoringModel.RuleDefinition> readRules ( ArrayNode nodes )
    {
        // Initialize the rules with a new array list.

        ArrayList <AuthoringModel.RuleDefinition> rules =
            new ArrayList <AuthoringModel.RuleDefinition> ();

        // Repeat the loop while i is less than nodes size.

        for ( int i = 0; i < nodes.size (); i++ )
        {
            String path     = "$.rules[" + i + "]";

            // Initialize the node by applying require object and get.

            ObjectNode node = requireObject ( nodes.get ( i ), path );

            // Apply reject unknown fields, add, and require text to the rules for the read rules operation.

            rejectUnknownFields ( node, RULE_FIELDS, path );
            rules.add (
                new AuthoringModel.RuleDefinition (
                    requireText ( node, "id", path ),
                    requireText ( node, "name", path ),
                    requireText ( node, "description", path ),
                    requireText ( node, "eventId", path ),
                    requireText ( node, "conditionSetId", path ),
                    requireText ( node, "actionId", path )
                )
            );
        }

        // Return the rules to the caller.

        return rules;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: writeParameters
    //
    // Description:
    //
    //   Performs the write parameters operation.
    //
    // Arguments:
    //
    //   values (Collection <AuthoringModel.ParameterDefinition>):
    //     The values to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private ArrayNode writeParameters ( Collection <AuthoringModel.ParameterDefinition> values )
    {
        // Initialize the nodes by applying create array node.

        ArrayNode nodes = this.objectMapper.createArrayNode ();

        // Process each value supplied by sorted values.

        for ( AuthoringModel.ParameterDefinition value : sorted ( values ) )
        {
            // Initialize the node by applying write entity fields and create object node.

            ObjectNode node = writeEntityFields ( this.objectMapper.createObjectNode (), value );

            // Store value type under "type" in the node.

            node.put ( "type", value.getType () );

            // Handle the branch where value enumeration values contains values.

            if ( !value.getEnumerationValues ().isEmpty () )
            {
                // Perform the set, write sorted text array, and get enumeration values calls required by the write
                // parameters operation.

                node.set ( "enumValues", writeSortedTextArray ( value.getEnumerationValues () ) );
            }

            // Add node to the nodes.

            nodes.add ( node );
        }

        // Return the nodes to the caller.

        return nodes;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: writePayloads
    //
    // Description:
    //
    //   Performs the write payloads operation.
    //
    // Arguments:
    //
    //   values (Collection <AuthoringModel.PayloadDefinition>):
    //     The values to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private ArrayNode writePayloads ( Collection <AuthoringModel.PayloadDefinition> values )
    {
        // Initialize the nodes by applying create array node.

        ArrayNode nodes = this.objectMapper.createArrayNode ();

        // Process each value supplied by sorted values.

        for ( AuthoringModel.PayloadDefinition value : sorted ( values ) )
        {
            // Initialize the node by applying write entity fields and create object node.

            ObjectNode node = writeEntityFields ( this.objectMapper.createObjectNode (), value );

            // Perform the set, write sorted text array, get parameter IDs, and add calls required by the write
            // payloads operation.

            node.set ( "parameterIds", writeSortedTextArray ( value.getParameterIds () ) );
            nodes.add ( node );
        }

        // Return the nodes to the caller.

        return nodes;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: writeEvents
    //
    // Description:
    //
    //   Performs the write events operation.
    //
    // Arguments:
    //
    //   values (Collection <AuthoringModel.EventDefinition>):
    //     The values to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private ArrayNode writeEvents ( Collection <AuthoringModel.EventDefinition> values )
    {
        // Initialize the nodes by applying create array node.

        ArrayNode nodes = this.objectMapper.createArrayNode ();

        // Process each value supplied by sorted values.

        for ( AuthoringModel.EventDefinition value : sorted ( values ) )
        {
            // Initialize the node by applying write entity fields and create object node.

            ObjectNode node = writeEntityFields ( this.objectMapper.createObjectNode (), value );

            // Perform the put, get payload ID, and add calls required by the write events operation.

            node.put ( "payloadId", value.getPayloadId () );
            nodes.add ( node );
        }

        // Return the nodes to the caller.

        return nodes;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: writeConditions
    //
    // Description:
    //
    //   Performs the write conditions operation.
    //
    // Arguments:
    //
    //   values (Collection <AuthoringModel.ConditionDefinition>):
    //     The values to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private ArrayNode writeConditions ( Collection <AuthoringModel.ConditionDefinition> values )
    {
        // Initialize the nodes by applying create array node.

        ArrayNode nodes = this.objectMapper.createArrayNode ();

        // Process each value supplied by sorted values.

        for ( AuthoringModel.ConditionDefinition value : sorted ( values ) )
        {
            // Initialize the node by applying write entity fields and create object node.

            ObjectNode node = writeEntityFields ( this.objectMapper.createObjectNode (), value );

            // Store value parameter ID under "parameter id" in the node.

            node.put ( "parameterId", value.getParameterId () );

            // Handle the branch where value has predicate.

            if ( value.hasPredicate () )
            {
                // Initialize the operand count by applying condition operand count and get operator.

                int operandCount = conditionOperandCount ( value.getOperator () );

                // Store value operator under "operator" in the node.

                node.put ( "operator", value.getOperator () );

                // Handle the branch where operand count is at least 1.

                if ( operandCount >= 1 )
                {
                    // Perform the set, create canonical value node, and get first value calls required by the write
                    // conditions operation.

                    node.set ( "value", this.createCanonicalValueNode ( value.getFirstValue () ) );
                }

                // Handle the branch where operand count equals 2.

                if ( operandCount == 2 )
                {
                    // Perform the set, create canonical value node, and get second value calls required by the write
                    // conditions operation.

                    node.set (
                        "secondValue",
                        this.createCanonicalValueNode ( value.getSecondValue () )
                    );
                }
            }

            // Add node to the nodes.

            nodes.add ( node );
        }

        // Return the nodes to the caller.

        return nodes;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: conditionOperandCount
    //
    // Description:
    //
    //   Performs the condition operand count operation.
    //
    // Arguments:
    //
    //   operator (String):
    //     The operator to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static int conditionOperandCount ( String operator )
    {
        // Return the condition operand count result to the caller.

        return switch ( operator )
        {
            // Handle "any" through this switch branch.

            case "ANY" -> 0;

            // Handle "between exclusive" and "between inclusive" through this switch branch.

            case "BETWEEN_EXCLUSIVE", "BETWEEN_INCLUSIVE" -> 2;

            // Handle "equals", "not equals", "greater than", "greater than or equal", "less than", and "less than or
            // equal" through this switch branch.

            case "EQUALS",
                 "NOT_EQUALS",
                 "GREATER_THAN",
                 "GREATER_THAN_OR_EQUAL",
                 "LESS_THAN",
                 "LESS_THAN_OR_EQUAL" -> 1;

            // Handle the default case through this switch branch.

            default -> throw new IllegalArgumentException (
                "Unknown condition operator: " + operator
            );
        };
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: writeConditionSets
    //
    // Description:
    //
    //   Performs the write condition sets operation.
    //
    // Arguments:
    //
    //   values (Collection <AuthoringModel.ConditionSetDefinition>):
    //     The values to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private ArrayNode writeConditionSets ( Collection <AuthoringModel.ConditionSetDefinition> values )
    {
        // Initialize the nodes by applying create array node.

        ArrayNode nodes = this.objectMapper.createArrayNode ();

        // Process each value supplied by sorted values.

        for ( AuthoringModel.ConditionSetDefinition value : sorted ( values ) )
        {
            // Initialize the node by applying write entity fields and create object node.

            ObjectNode node = writeEntityFields ( this.objectMapper.createObjectNode (), value );

            // Handle the branch where value uses predefined conditions succeeds.

            if ( value.usesPredefinedConditions () )
            {
                // Perform the set, write sorted text array, and get condition IDs calls required by the write
                // condition sets operation.

                node.set ( "conditionIds", writeSortedTextArray ( value.getConditionIds () ) );
            }

            // Handle the alternative path when the preceding condition is not satisfied.

            else
            {
                // Initialize the bindings node by applying create object node.

                ObjectNode bindingsNode = this.objectMapper.createObjectNode ();

                // Perform the for each, sorted, stream, entry set, get bindings, comparing by key, set, get key,
                // create canonical value node, and get value calls required by the write condition sets operation.

                value.getBindings ().entrySet ().stream ()
                    .sorted ( Map.Entry.comparingByKey () )
                    .forEach (
                        entry -> bindingsNode.set (
                            entry.getKey (),
                            this.createCanonicalValueNode ( entry.getValue () )
                        )
                    );

                node.set ( "bindings", bindingsNode );
            }

            // Add node to the nodes.

            nodes.add ( node );
        }

        // Return the nodes to the caller.

        return nodes;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createCanonicalValueNode
    //
    // Description:
    //
    //   Performs the create canonical value node operation.
    //
    // Arguments:
    //
    //   value (Object):
    //     The value to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private JsonNode createCanonicalValueNode ( Object value )
    {
        // Handle the branch where value is a big decimal.

        if ( value instanceof BigDecimal )
        {
            try
            {
                // Return the result produced by value to tree when value is a big decimal.

                return this.objectMapper.valueToTree ( ( (BigDecimal) value ).longValueExact () );
            }

            // Handle arithmetic failures captured as exception.

            catch ( ArithmeticException exception )
            {
                // Return the result produced by value to tree when value is a big decimal.

                return this.objectMapper.valueToTree ( value );
            }
        }

        // Return the result produced by value to tree.

        return this.objectMapper.valueToTree ( value );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: writeActions
    //
    // Description:
    //
    //   Performs the write actions operation.
    //
    // Arguments:
    //
    //   values (Collection <AuthoringModel.ActionDefinition>):
    //     The values to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private ArrayNode writeActions ( Collection <AuthoringModel.ActionDefinition> values )
    {
        // Initialize the nodes by applying create array node.

        ArrayNode nodes = this.objectMapper.createArrayNode ();

        // Process each value supplied by sorted values.

        for ( AuthoringModel.ActionDefinition value : sorted ( values ) )
        {
            // Add write entity fields object mapper create object node value to the nodes.

            nodes.add ( writeEntityFields ( this.objectMapper.createObjectNode (), value ) );
        }

        // Return the nodes to the caller.

        return nodes;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: writeRules
    //
    // Description:
    //
    //   Performs the write rules operation.
    //
    // Arguments:
    //
    //   values (Collection <AuthoringModel.RuleDefinition>):
    //     The values to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private ArrayNode writeRules ( Collection <AuthoringModel.RuleDefinition> values )
    {
        // Initialize the nodes by applying create array node.

        ArrayNode nodes = this.objectMapper.createArrayNode ();

        // Process each value supplied by sorted values.

        for ( AuthoringModel.RuleDefinition value : sorted ( values ) )
        {
            // Initialize the node by applying write entity fields and create object node.

            ObjectNode node = writeEntityFields ( this.objectMapper.createObjectNode (), value );

            // Perform the put, get event ID, get condition set ID, get action ID, and add calls required by the write
            // rules operation.

            node.put ( "eventId", value.getEventId () );
            node.put ( "conditionSetId", value.getConditionSetId () );
            node.put ( "actionId", value.getActionId () );
            nodes.add ( node );
        }

        // Return the nodes to the caller.

        return nodes;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: writeEntityFields
    //
    // Description:
    //
    //   Performs the write entity fields operation.
    //
    // Arguments:
    //
    //   node (ObjectNode):
    //     The node to use.
    //
    //   value (AuthoringModel.EntityDefinition):
    //     The value to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static ObjectNode writeEntityFields (
        ObjectNode node,
        AuthoringModel.EntityDefinition value
    )
    {
        // Perform the put, get ID, get name, and get description calls required by the write entity fields operation.

        node.put ( "id", value.getId () );
        node.put ( "name", value.getName () );
        node.put ( "description", value.getDescription () );

        // Return the node to the caller.

        return node;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: writeSortedTextArray
    //
    // Description:
    //
    //   Performs the write sorted text array operation.
    //
    // Arguments:
    //
    //   values (Collection <String>):
    //     The values to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private ArrayNode writeSortedTextArray ( Collection <String> values )
    {
        // Prepare the node and sorted values values needed by the write sorted text array operation.

        ArrayNode node              = this.objectMapper.createArrayNode ();
        ArrayList <String> sortedValues = new ArrayList <String> ( values );

        // Perform the sort, natural order, and for each calls required by the write sorted text array operation.

        sortedValues.sort ( Comparator.naturalOrder () );
        sortedValues.forEach ( node::add );

        // Return the node to the caller.

        return node;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: sorted
    //
    // Description:
    //
    //   Performs the sorted operation.
    //
    // Arguments:
    //
    //   values (Collection <T>):
    //     The values to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static <T extends AuthoringModel.EntityDefinition> List <T> sorted ( Collection <T> values )
    {
        // Initialize the sorted values with a new array list.

        ArrayList <T> sortedValues = new ArrayList <T> ( values );

        // Perform the sort and comparing calls required by the sorted operation.

        sortedValues.sort ( Comparator.comparing ( AuthoringModel.EntityDefinition::getId ) );

        // Return the sorted values to the caller.

        return sortedValues;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readTextArray
    //
    // Description:
    //
    //   Performs the read text array operation.
    //
    // Arguments:
    //
    //   values (ArrayNode):
    //     The values to use.
    //
    //   path (String):
    //     The path to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static List <String> readTextArray ( ArrayNode values, String path )
    {
        // Initialize the text values with a new array list.

        ArrayList <String> textValues = new ArrayList <String> ();

        // Repeat the loop while i is less than values size.

        for ( int i = 0; i < values.size (); i++ )
        {
            // Initialize the value by applying get.

            JsonNode value = values.get ( i );

            // Reject the operation when value is not textual.

            if ( !value.isTextual () )
            {
                throw new IllegalArgumentException ( path + "[" + i + "] must be a string." );
            }

            // Add value text value to the text values.

            textValues.add ( value.textValue () );
        }

        // Return the text values to the caller.

        return textValues;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readValueMap
    //
    // Description:
    //
    //   Performs the read value map operation.
    //
    // Arguments:
    //
    //   node (ObjectNode):
    //     The node to use.
    //
    //   path (String):
    //     The path to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Map <String, Object> readValueMap ( ObjectNode node, String path )
    {
        // Prepare the values and fields values needed by the read value map operation.

        LinkedHashMap <String, Object> values = new LinkedHashMap <String, Object> ();
        Iterator <Map.Entry <String, JsonNode>> fields = node.properties ().iterator ();

        // Continue processing while fields has next.

        while ( fields.hasNext () )
        {
            // Initialize the field by applying next.

            Map.Entry <String, JsonNode> field = fields.next ();

            // Store read value field value path + " " + field key under field key in the values.

            values.put ( field.getKey (), readValue ( field.getValue (), path + "." + field.getKey () ) );
        }

        // Return the values to the caller.

        return values;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readValue
    //
    // Description:
    //
    //   Performs the read value operation.
    //
    // Arguments:
    //
    //   node (JsonNode):
    //     The node to use.
    //
    //   path (String):
    //     The path to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Object readValue ( JsonNode node, String path )
    {
        // Stop this path and return its result when node is null.

        if ( node.isNull () )
        {
            // Return a null result to indicate that no value is available when node is null.

            return null;
        }

        // Stop this path and return its result when node is textual.

        if ( node.isTextual () )
        {
            // Return the result produced by text value when node is textual.

            return node.textValue ();
        }

        // Stop this path and return its result when node is boolean.

        if ( node.isBoolean () )
        {
            // Return the result produced by boolean value when node is boolean.

            return node.booleanValue ();
        }

        // Stop this path and return its result when node is integral number.

        if ( node.isIntegralNumber () )
        {
            // Return the value selected according to node is convert to long when node is integral number.

            return node.canConvertToLong () ? node.longValue () : node.bigIntegerValue ();
        }

        // Stop this path and return its result when node is floating point number.

        if ( node.isFloatingPointNumber () )
        {
            // Return a newly constructed big decimal containing the operation result when node is floating point
            // number.

            return new BigDecimal ( node.asText () );
        }

        // Stop this path and return its result when node is object.

        if ( node.isObject () )
        {
            // Return the result produced by read value map when node is object.

            return readValueMap ( (ObjectNode) node, path );
        }

        // Handle the branch where node is array.

        if ( node.isArray () )
        {
            // Initialize the values with a new array list.

            ArrayList <Object> values = new ArrayList <Object> ();

            // Repeat the loop while i is less than node size.

            for ( int i = 0; i < node.size (); i++ )
            {
                // Add read value node get i path + " " + i + " " to the values.

                values.add ( readValue ( node.get ( i ), path + "[" + i + "]" ) );
            }

            // Return the values to the caller when node is array.

            return values;
        }

        throw new IllegalArgumentException ( path + " contains an unsupported JSON value." );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readOptionalValue
    //
    // Description:
    //
    //   Performs the read optional value operation.
    //
    // Arguments:
    //
    //   node (ObjectNode):
    //     The node to use.
    //
    //   fieldName (String):
    //     The field name to use.
    //
    //   path (String):
    //     The path to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Object readOptionalValue ( ObjectNode node, String fieldName, String path )
    {
        // Initialize the value by applying get.

        JsonNode value = node.get ( fieldName );

        // Return the value selected according to value is unavailable.

        return value == null ? null : readValue ( value, path + "." + fieldName );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requireObject
    //
    // Description:
    //
    //   Performs the require object operation.
    //
    // Arguments:
    //
    //   node (JsonNode):
    //     The node to use.
    //
    //   path (String):
    //     The path to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static ObjectNode requireObject ( JsonNode node, String path )
    {
        // Reject the operation when node is unavailable or node is not object.

        if ( node == null || !node.isObject () )
        {
            throw new IllegalArgumentException ( path + " must be an object." );
        }

        // Return the require object result to the caller.

        return (ObjectNode) node;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requireObjectMember
    //
    // Description:
    //
    //   Performs the require object member operation.
    //
    // Arguments:
    //
    //   node (ObjectNode):
    //     The node to use.
    //
    //   fieldName (String):
    //     The field name to use.
    //
    //   path (String):
    //     The path to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static ObjectNode requireObjectMember ( ObjectNode node, String fieldName, String path )
    {
        // Return the result produced by require object.

        return requireObject ( node.get ( fieldName ), path + "." + fieldName );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requireArray
    //
    // Description:
    //
    //   Performs the require array operation.
    //
    // Arguments:
    //
    //   node (ObjectNode):
    //     The node to use.
    //
    //   fieldName (String):
    //     The field name to use.
    //
    //   path (String):
    //     The path to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static ArrayNode requireArray ( ObjectNode node, String fieldName, String path )
    {
        // Initialize the value by applying get.

        JsonNode value = node.get ( fieldName );

        // Reject the operation when value is unavailable or value is not array.

        if ( value == null || !value.isArray () )
        {
            throw new IllegalArgumentException ( path + "." + fieldName + " must be an array." );
        }

        // Return the require array result to the caller.

        return (ArrayNode) value;
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
    //   node (ObjectNode):
    //     The node to use.
    //
    //   fieldName (String):
    //     The field name to use.
    //
    //   path (String):
    //     The path to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String requireText ( ObjectNode node, String fieldName, String path )
    {
        // Initialize the value by applying get.

        JsonNode value = node.get ( fieldName );

        // Reject the operation when value is unavailable or value is not textual.

        if ( value == null || !value.isTextual () )
        {
            throw new IllegalArgumentException ( path + "." + fieldName + " must be a string." );
        }

        // Return the result produced by text value.

        return value.textValue ();
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
    //   node (ObjectNode):
    //     The node to use.
    //
    //   allowedFields (Set <String>):
    //     The allowed fields to use.
    //
    //   path (String):
    //     The path to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void rejectUnknownFields ( ObjectNode node, Set <String> allowedFields, String path )
    {
        // Initialize the field names by applying field names.

        Iterator <String> fieldNames = node.fieldNames ();

        // Continue processing while field names has next.

        while ( fieldNames.hasNext () )
        {
            // Initialize the field name by applying next.

            String fieldName = fieldNames.next ();

            // Reject the operation when allowed fields does not contain field name.

            if ( !allowedFields.contains ( fieldName ) )
            {
                throw new IllegalArgumentException ( path + " contains unknown field " + fieldName + "." );
            }
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: fields
    //
    // Description:
    //
    //   Performs the fields operation.
    //
    // Arguments:
    //
    //   values (String...):
    //     The values to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Set <String> fields ( String... values )
    {
        // Return an immutable copy of new hash set string arrays as list values.

        return Set.copyOf ( new HashSet <String> ( Arrays.asList ( values ) ) );
    }
}
