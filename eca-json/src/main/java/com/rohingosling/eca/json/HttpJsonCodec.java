//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Serializes HTTP transport documents and classifies JSON/model boundary failures for adapters.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rohingosling.eca.application.ClientEvaluationResult;
import com.rohingosling.eca.application.HostedEvaluation;
import com.rohingosling.eca.application.EvaluateOccurrenceUseCase;
import com.rohingosling.eca.application.HostedModelReadiness;
import com.rohingosling.eca.application.HostedModelSummary;
import com.rohingosling.eca.model.ModelValidationException;

//*********************************************************************************************************************
// Class: HttpJsonCodec
//
// Description:
//
//   Serializes HTTP transport documents and classifies JSON/model boundary failures for adapters.
//
//*********************************************************************************************************************

public final class HttpJsonCodec
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final ObjectMapper objectMapper;

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: HttpJsonCodec
    //
    // Description:
    //
    //   Creates the HttpJsonCodec instance from the supplied values.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public HttpJsonCodec ()
    {
        // Construct the object mapper instance required by the HTTP JSON codec operation.

        this.objectMapper = new ObjectMapper ();
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: liveness
    //
    // Description:
    //
    //   Performs the liveness operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String liveness ()
    {
        // Return the result produced by write.

        return this.write ( this.objectMapper.createObjectNode ().put ( "status", "UP" ) );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readiness
    //
    // Description:
    //
    //   Performs the readiness operation.
    //
    // Arguments:
    //
    //   readiness (HostedModelReadiness):
    //     The readiness to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String readiness ( HostedModelReadiness readiness )
    {
        // Initialize the root by applying create object node.

        ObjectNode root = this.objectMapper.createObjectNode ();

        // Perform the put, is ready, if present, get summary, and get revision calls required by the readiness
        // operation.

        root.put ( "status", readiness.isReady () ? "READY" : "NOT_READY" );
        root.put ( "ready", readiness.isReady () );
        readiness.getSummary ().ifPresent (
            summary -> root.put ( "modelRevision", summary.getRevision () )
        );

        // Return the result produced by write.

        return this.write ( root );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: evaluation
    //
    // Description:
    //
    //   Performs the evaluation operation.
    //
    // Arguments:
    //
    //   hostedEvaluation (HostedEvaluation):
    //     The hosted evaluation to use.
    //
    //   elapsedMicroseconds (long):
    //     The elapsed microseconds to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String evaluation ( HostedEvaluation hostedEvaluation, long elapsedMicroseconds )
    {
        // Initialize the root by applying create object node.

        ObjectNode root = this.objectMapper.createObjectNode ();

        // Store hosted evaluation outcome under "outcome" in the root.

        root.put ( "outcome", hostedEvaluation.getOutcome () );

        // Handle the branch where hosted evaluation is action.

        if ( hostedEvaluation.isAction () )
        {
            // Perform the put, get action ID, get rule ID, and get specificity calls required by the evaluation
            // operation.

            root.put ( "actionId", hostedEvaluation.getActionId () );
            root.put ( "ruleId", hostedEvaluation.getRuleId () );
            root.put ( "specificity", hostedEvaluation.getSpecificity () );
        }

        // Perform the put and get revision calls required by the evaluation operation.

        root.put ( "modelRevision", hostedEvaluation.getRevision () );
        root.put ( "elapsedMicroseconds", elapsedMicroseconds );

        // Return the result produced by write.

        return this.write ( root );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: evaluate
    //
    // Description:
    //
    //   Performs the evaluate operation.
    //
    // Arguments:
    //
    //   evaluateOccurrenceUseCase (EvaluateOccurrenceUseCase):
    //     The evaluate occurrence use case to use.
    //
    //   occurrenceDocument (byte []):
    //     The occurrence document to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public HostedEvaluation evaluate (
        EvaluateOccurrenceUseCase evaluateOccurrenceUseCase,
        byte [] occurrenceDocument
    )
    {
        // Initialize the occurrence codec with a new event occurrence JSON codec.

        EventOccurrenceJsonCodec occurrenceCodec = new EventOccurrenceJsonCodec ();

        // Return the result produced by evaluate occurrence.

        return evaluateOccurrenceUseCase.evaluateOccurrence (
            occurrenceCodec.read ( new String ( occurrenceDocument, java.nio.charset.StandardCharsets.UTF_8 ) )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: summary
    //
    // Description:
    //
    //   Performs the summary operation.
    //
    // Arguments:
    //
    //   summary (HostedModelSummary):
    //     The summary to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String summary ( HostedModelSummary summary )
    {
        // Initialize the root by applying create object node.

        ObjectNode root = this.objectMapper.createObjectNode ();

        // Perform the put, get model ID, get revision, get parameter count, get payload count, get event count, get
        // condition count, get condition set count, get action count, and get rule count calls required by the summary
        // operation.

        root.put ( "modelId", summary.getModelId () );
        root.put ( "modelRevision", summary.getRevision () );
        root.put ( "parameterCount", summary.getParameterCount () );
        root.put ( "payloadCount", summary.getPayloadCount () );
        root.put ( "eventCount", summary.getEventCount () );
        root.put ( "conditionCount", summary.getConditionCount () );
        root.put ( "conditionSetCount", summary.getConditionSetCount () );
        root.put ( "actionCount", summary.getActionCount () );
        root.put ( "ruleCount", summary.getRuleCount () );

        // Return the result produced by write.

        return this.write ( root );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: stopping
    //
    // Description:
    //
    //   Performs the stopping operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String stopping ()
    {
        // Return the result produced by write.

        return this.write ( this.objectMapper.createObjectNode ().put ( "status", "STOPPING" ) );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: problem
    //
    // Description:
    //
    //   Performs the problem operation.
    //
    // Arguments:
    //
    //   status (int):
    //     The status to use.
    //
    //   title (String):
    //     The title to use.
    //
    //   detail (String):
    //     The detail to use.
    //
    //   instance (String):
    //     The instance to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String problem ( int status, String title, String detail, String instance )
    {
        // Initialize the root by applying create object node.

        ObjectNode root = this.objectMapper.createObjectNode ();

        // Store "about:blank" under "type" in the root.

        root.put ( "type", "about:blank" );
        root.put ( "title", title );
        root.put ( "status", status );
        root.put ( "detail", detail );
        root.put ( "instance", instance );

        // Return the result produced by write.

        return this.write ( root );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readModelRevision
    //
    // Description:
    //
    //   Performs the read model revision operation.
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

    public String readModelRevision ( String document )
    {
        try
        {
            // Initialize the revision by applying as text, path, and read tree.

            String revision = this.objectMapper.readTree ( document ).path ( "modelRevision" ).asText ();

            // Reject the operation when revision is blank.

            if ( revision.isBlank () )
            {
                throw new IllegalArgumentException (
                    "The server response does not contain modelRevision."
                );
            }

            // Return the revision to the caller.

            return revision;
        }

        // Handle I/O failures captured as exception.

        catch ( IOException exception )
        {
            throw new IllegalArgumentException ( "The server response is not valid JSON.", exception );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readEvaluation
    //
    // Description:
    //
    //   Performs the read evaluation operation.
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

    public ClientEvaluationResult readEvaluation ( String document )
    {
        try
        {
            // Initialize the root by applying read tree.

            JsonNode root = this.objectMapper.readTree ( document );

            // Reject the operation when root is unavailable or root is not object.

            if ( root == null || !root.isObject () )
            {
                throw new IllegalArgumentException (
                    "The evaluation response must be a JSON object."
                );
            }

            // Prepare the outcome, model revision, and elapsed node values needed by the read evaluation operation.

            String outcome       = requiredText ( root, "outcome" );
            String modelRevision = requiredText ( root, "modelRevision" );
            JsonNode elapsedNode = root.get ( "elapsedMicroseconds" );

            // Reject the operation when elapsed node is unavailable or elapsed node is not convert to long or elapsed
            // node long value is less than 0.

            if ( elapsedNode == null || !elapsedNode.canConvertToLong () || elapsedNode.longValue () < 0 )
            {
                throw new IllegalArgumentException (
                    "The evaluation response contains an invalid elapsedMicroseconds value."
                );
            }

            // Handle the branch where outcome matches "action".

            if ( outcome.equals ( "ACTION" ) )
            {
                // Initialize the specificity node by applying get.

                JsonNode specificityNode = root.get ( "specificity" );

                // Reject the operation when specificity node is unavailable or specificity node is not convert to int
                // or specificity node int value is less than 0.

                if (
                    specificityNode == null
                        || !specificityNode.canConvertToInt ()
                        || specificityNode.intValue () < 0
                )
                {
                    throw new IllegalArgumentException (
                        "The ACTION response contains an invalid specificity value."
                    );
                }

                // Return a newly constructed client evaluation result containing the operation result when outcome
                // matches "action".

                return new ClientEvaluationResult (
                    outcome,
                    requiredText ( root, "actionId" ),
                    requiredText ( root, "ruleId" ),
                    Integer.valueOf ( specificityNode.intValue () ),
                    modelRevision,
                    elapsedNode.longValue (),
                    0
                );
            }

            // Handle the branch where outcome matches "no action".

            if ( outcome.equals ( "NO_ACTION" ) )
            {
                // Reject the operation when root has has or root has has or root has has.

                if (
                    root.has ( "actionId" )
                        || root.has ( "ruleId" )
                        || root.has ( "specificity" )
                )
                {
                    throw new IllegalArgumentException (
                        "The NO_ACTION response must omit actionId, ruleId, and specificity."
                    );
                }

                // Return a newly constructed client evaluation result containing the operation result when outcome
                // matches "no action".

                return new ClientEvaluationResult (
                    outcome,
                    null,
                    null,
                    null,
                    modelRevision,
                    elapsedNode.longValue (),
                    0
                );
            }

            throw new IllegalArgumentException (
                "The evaluation response contains unsupported outcome " + outcome + "."
            );
        }

        // Handle JSON processing failures captured as exception.

        catch ( JsonProcessingException exception )
        {
            throw new IllegalArgumentException (
                "The evaluation response is not valid JSON.",
                exception
            );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readProblemDetail
    //
    // Description:
    //
    //   Performs the read problem detail operation.
    //
    // Arguments:
    //
    //   document (String):
    //     The document to use.
    //
    //   statusCode (int):
    //     The status code to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String readProblemDetail ( String document, int statusCode )
    {
        try
        {
            // Prepare the root and detail values needed by the read problem detail operation.

            JsonNode root = this.objectMapper.readTree ( document );
            String detail = root.path ( "detail" ).asText ();

            // Return the value selected according to detail is blank.

            return detail.isBlank ()
                ? "The remote API rejected the request with HTTP " + statusCode + "."
                : detail;
        }

        // Handle I/O failures captured as exception.

        catch ( IOException exception )
        {
            // Return the composed read problem detail value.

            return "The remote API rejected the request with HTTP " + statusCode + ".";
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: isMalformedJson
    //
    // Description:
    //
    //   Indicates whether malformed json.
    //
    // Arguments:
    //
    //   throwable (Throwable):
    //     The exception that caused the operation to fail.
    //
    // Returns:
    //
    //   `true` when the condition is satisfied; otherwise `false`.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public boolean isMalformedJson ( Throwable throwable )
    {
        Throwable currentThrowable = throwable;

        // Continue processing while current throwable is available.

        while ( currentThrowable != null )
        {
            // Stop this path and return its result when current throwable is a JSON processing exception.

            if ( currentThrowable instanceof JsonProcessingException )
            {
                // Return true for this outcome when current throwable is a JSON processing exception.

                return true;
            }

            // Update the current throwable from the get cause result.

            currentThrowable = currentThrowable.getCause ();
        }

        // Return false for this outcome.

        return false;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: isModelValidationFailure
    //
    // Description:
    //
    //   Indicates whether model validation failure.
    //
    // Arguments:
    //
    //   throwable (Throwable):
    //     The exception that caused the operation to fail.
    //
    // Returns:
    //
    //   `true` when the condition is satisfied; otherwise `false`.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public boolean isModelValidationFailure ( Throwable throwable )
    {
        // Return whether throwable is a model validation exception.

        return throwable instanceof ModelValidationException;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validationDetail
    //
    // Description:
    //
    //   Performs the validation detail operation.
    //
    // Arguments:
    //
    //   throwable (Throwable):
    //     The exception that caused the operation to fail.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String validationDetail ( Throwable throwable )
    {
        ModelValidationException exception = (ModelValidationException) throwable;

        // Return the value selected according to exception diagnostics contains no values.

        return exception.getDiagnostics ().isEmpty ()
            ? exception.getMessage ()
            : exception.getDiagnostics ().get ( 0 ).getRemedy ();
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
    //   root (ObjectNode):
    //     The root to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private String write ( ObjectNode root )
    {
        try
        {
            // Return the result produced by write value as string.

            return this.objectMapper.writeValueAsString ( root );
        }

        // Handle JSON processing failures captured as exception.

        catch ( JsonProcessingException exception )
        {
            throw new IllegalStateException ( "An HTTP response could not be serialized.", exception );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requiredText
    //
    // Description:
    //
    //   Performs the required text operation.
    //
    // Arguments:
    //
    //   root (JsonNode):
    //     The root to use.
    //
    //   fieldName (String):
    //     The field name to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String requiredText ( JsonNode root, String fieldName )
    {
        // Initialize the value by applying get.

        JsonNode value = root.get ( fieldName );

        // Reject the operation when value is unavailable or value is not textual or value text value is blank.

        if ( value == null || !value.isTextual () || value.textValue ().isBlank () )
        {
            throw new IllegalArgumentException (
                "The server response does not contain a valid " + fieldName + "."
            );
        }

        // Return the result produced by text value.

        return value.textValue ();
    }
}
