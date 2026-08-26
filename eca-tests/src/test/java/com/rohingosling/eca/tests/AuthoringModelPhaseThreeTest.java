//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies lossless authoring, validation, compilation, canonical JSON, revisions, and courier-selection vectors.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohingosling.eca.domain.ActionResult;
import com.rohingosling.eca.domain.CompiledConditionSet;
import com.rohingosling.eca.domain.EvaluationOutcome;
import com.rohingosling.eca.domain.EvaluationResult;
import com.rohingosling.eca.domain.EventOccurrence;
import com.rohingosling.eca.domain.ParameterId;
import com.rohingosling.eca.domain.PayloadValue;
import com.rohingosling.eca.engine.RuleEngine;
import com.rohingosling.eca.engine.SpecificityPolicy;
import com.rohingosling.eca.json.AuthoringModelJsonCodec;
import com.rohingosling.eca.json.EventOccurrenceJsonCodec;
import com.rohingosling.eca.model.AuthoringModel;
import com.rohingosling.eca.model.AuthoringModelCompiler;
import com.rohingosling.eca.model.CompiledModel;
import com.rohingosling.eca.model.ModelLimits;
import com.rohingosling.eca.model.ModelValidator;
import com.rohingosling.eca.model.OccurrenceDocument;
import com.rohingosling.eca.model.ValidationDiagnostic;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

//*********************************************************************************************************************
// Class: AuthoringModelPhaseThreeTest
//
// Description:
//
//   Verifies lossless authoring, validation, compilation, canonical JSON, revisions, and courier-selection vectors.
//
//*********************************************************************************************************************

final class AuthoringModelPhaseThreeTest
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    private static final String TEST_MODEL_RESOURCE              = "/examples/eca-rule-engine-example.json";
    private static final String COURIER_SELECTION_MODEL_RESOURCE = "/examples/courier-selection-example.json";

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: currentTestModel_hasExpectedCountsAndValidates
    //
    // Description:
    //
    //   Verifies that current test model has expected counts and validates.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void currentTestModel_hasExpectedCountsAndValidates ()
    {
        // Initialize the model by applying read test model.

        AuthoringModel model = readTestModel ();

        // Verify the current test model has expected counts and validates scenario against its expected outcome.

        assertThat ( new ModelValidator ().validate ( model ) ).isEmpty ();
        assertThat ( model.getParameters () ).hasSize ( 7 );
        assertThat ( model.getPayloads () ).hasSize ( 3 );
        assertThat ( model.getEvents () ).hasSize ( 3 );
        assertThat ( model.getConditions () ).hasSize ( 17 );
        assertThat ( model.getConditionSets () ).hasSize ( 7 );
        assertThat ( model.getActions () ).hasSize ( 3 );
        assertThat ( model.getRules () ).hasSize ( 10 );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: curatedCourierSelectionExample_hasExpectedCountsAndValidates
    //
    // Description:
    //
    //   Verifies that curated courier selection example has expected counts and validates.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void curatedCourierSelectionExample_hasExpectedCountsAndValidates ()
    {
        // Skip this private-fixture test when the curated public distribution omits the courier-selection example.

        Assumptions.assumeTrue (
            AuthoringModelPhaseThreeTest.class.getResource ( COURIER_SELECTION_MODEL_RESOURCE ) != null,
            "Courier-selection example is not available in this distribution."
        );

        // Initialize the model by applying read and read resource.

        AuthoringModel model = new AuthoringModelJsonCodec ().read (
            readResource ( COURIER_SELECTION_MODEL_RESOURCE )
        );

        // Verify the curated courier selection example has expected counts and validates scenario against its expected
        // outcome.

        assertThat ( new ModelValidator ().validate ( model ) ).isEmpty ();
        assertThat ( model.getParameters () ).hasSize ( 8 );
        assertThat ( model.getPayloads () ).hasSize ( 4 );
        assertThat ( model.getEvents () ).hasSize ( 4 );
        assertThat ( model.getConditions () ).hasSize ( 8 );
        assertThat ( model.getConditionSets () ).hasSize ( 27 );
        assertThat ( model.getActions () ).hasSize ( 3 );
        assertThat ( model.getRules () ).hasSize ( 27 );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: canonicalJson_roundTripsAndIgnoresSourceEntityOrdering
    //
    // Description:
    //
    //   Verifies that canonical json round trips and ignores source entity ordering.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void canonicalJson_roundTripsAndIgnoresSourceEntityOrdering ()
    {
        // Prepare the codec, model, reversed model, canonical JSON, and round trip JSON values needed by the canonical
        // JSON round trips and ignores source entity ordering operation.

        AuthoringModelJsonCodec codec = new AuthoringModelJsonCodec ();
        AuthoringModel model          = readTestModel ();
        AuthoringModel reversedModel  = reverseEntityOrdering ( model );

        byte [] canonicalJson = codec.writeCanonical ( model );
        byte [] roundTripJson = codec.writeCanonical (
            codec.read ( codec.writePretty ( model ) )
        );

        // Verify the canonical JSON round trips and ignores source entity ordering scenario against its expected
        // outcome.

        assertThat ( roundTripJson ).isEqualTo ( canonicalJson );
        assertThat ( codec.writeCanonical ( reversedModel ) ).isEqualTo ( canonicalJson );
        assertThat ( codec.revision ( reversedModel ) ).isEqualTo ( codec.revision ( model ) );
        assertThat ( codec.revision ( model ) ).matches ( "^sha256:[0-9a-f]{64}$" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: explicitWildcardAndOmission_remainDistinctThroughCompilation
    //
    // Description:
    //
    //   Verifies that explicit wildcard and omission remain distinct through compilation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void explicitWildcardAndOmission_remainDistinctThroughCompilation ()
    {
        // Prepare the codec, wildcard model, omitted model, compiled wildcard, compiled omission, wildcard, omission,
        // and specificity policy values needed by the explicit wildcard and omission remain distinct through
        // compilation operation.

        AuthoringModelJsonCodec codec = new AuthoringModelJsonCodec ();
        AuthoringModel wildcardModel  = codec.read ( minimalBindingModel ( "\"condition-value\": null" ) );
        AuthoringModel omittedModel   = codec.read ( minimalBindingModel ( "" ) );

        CompiledModel compiledWildcard = new AuthoringModelCompiler ().compile (
            wildcardModel,
            codec.revision ( wildcardModel )
        );
        CompiledModel compiledOmission = new AuthoringModelCompiler ().compile (
            omittedModel,
            codec.revision ( omittedModel )
        );
        CompiledConditionSet wildcard = compiledWildcard.getConditionSets ().get ( "condition-set-value" );
        CompiledConditionSet omission = compiledOmission.getConditionSets ().get ( "condition-set-value" );
        SpecificityPolicy specificityPolicy = new SpecificityPolicy ();

        // Verify the explicit wildcard and omission remain distinct through compilation scenario against its expected
        // outcome.

        assertThat ( wildcard.getBindings () ).hasSize ( 1 );
        assertThat ( omission.getBindings () ).isEmpty ();
        assertThat ( specificityPolicy.calculate ( wildcard ) ).isEqualTo ( 1 );
        assertThat ( specificityPolicy.calculate ( omission ) ).isZero ();
        assertThat ( codec.revision ( wildcardModel ) ).isNotEqualTo ( codec.revision ( omittedModel ) );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: occurrencePayload_preservesConcreteNullAndOmittedStates
    //
    // Description:
    //
    //   Verifies that occurrence payload preserves concrete null and omitted states.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void occurrencePayload_preservesConcreteNullAndOmittedStates ()
    {
        // Prepare the codec, model, compiled model, occurrence codec, concrete document, null document, omitted
        // document, concrete occurrence, null occurrence, omitted occurrence, concrete payload, null payload, and
        // omitted payload values needed by the occurrence payload preserves concrete null and omitted states
        // operation.

        AuthoringModelJsonCodec codec = new AuthoringModelJsonCodec ();
        AuthoringModel model          = codec.read ( minimalBindingModel ( "" ) );
        CompiledModel compiledModel   = new AuthoringModelCompiler ().compile ( model, codec.revision ( model ) );
        EventOccurrenceJsonCodec occurrenceCodec = new EventOccurrenceJsonCodec ();

        OccurrenceDocument concreteDocument = occurrenceCodec.read (
            """
            {"eventId":"event-value","payload":{"parameter-value":"value"}}
            """
        );
        OccurrenceDocument nullDocument = occurrenceCodec.read (
            """
            {"eventId":"event-value","payload":{"parameter-value":null}}
            """
        );
        OccurrenceDocument omittedDocument = occurrenceCodec.read (
            """
            {"eventId":"event-value","payload":{}}
            """
        );

        EventOccurrence concreteOccurrence = compiledModel.createOccurrence (
            occurrenceCodec.read ( occurrenceCodec.write ( concreteDocument ) )
        );
        EventOccurrence nullOccurrence = compiledModel.createOccurrence (
            occurrenceCodec.read ( occurrenceCodec.write ( nullDocument ) )
        );
        EventOccurrence omittedOccurrence = compiledModel.createOccurrence (
            occurrenceCodec.read ( occurrenceCodec.write ( omittedDocument ) )
        );

        Map <ParameterId, PayloadValue> concretePayload = concreteOccurrence.getPayload ().getBindings ();
        Map <ParameterId, PayloadValue> nullPayload     = nullOccurrence.getPayload ().getBindings ();
        Map <ParameterId, PayloadValue> omittedPayload  = omittedOccurrence.getPayload ().getBindings ();

        // Verify the occurrence payload preserves concrete null and omitted states scenario against its expected
        // outcome.

        assertThat ( concretePayload.values () ).singleElement ().matches ( PayloadValue::isConcrete );
        assertThat ( nullPayload.values () ).singleElement ().matches ( value -> !value.isConcrete () );
        assertThat ( omittedPayload ).isEmpty ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: strictCodec_rejectsMalformedDuplicateUnknownAndWrongTypes
    //
    // Description:
    //
    //   Verifies that strict codec rejects malformed duplicate unknown and wrong types.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void strictCodec_rejectsMalformedDuplicateUnknownAndWrongTypes ()
    {
        // Initialize the codec with a new authoring model JSON codec.

        AuthoringModelJsonCodec codec = new AuthoringModelJsonCodec ();

        // Process each fixture name supplied by list of "malformed-json" "duplicate-member json" "unknown-field json".

        for ( String fixtureName : List.of (
            "malformed-json.json",
            "duplicate-member.json",
            "unknown-field.json",
            "wrong-json-type.json"
        ) )
        {
            // Verify the strict codec rejects malformed duplicate unknown and wrong types scenario against its
            // expected outcome.

            assertThatThrownBy (
                () -> codec.read ( readResource ( "/fixtures/invalid/" + fixtureName ) )
            ).isInstanceOf ( IllegalArgumentException.class );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: semanticFixtures_emitIntendedDiagnosticCodes
    //
    // Description:
    //
    //   Verifies that semantic fixtures emit intended diagnostic codes.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void semanticFixtures_emitIntendedDiagnosticCodes ()
    {
        // Initialize the expected codes with a new linked hash map.

        Map <String, String> expectedCodes = new LinkedHashMap <String, String> ();

        // Store model validator unsupported schema version under "unsupported-schema-version json" in the expected
        // codes.

        expectedCodes.put ( "unsupported-schema-version.json", ModelValidator.UNSUPPORTED_SCHEMA_VERSION );
        expectedCodes.put ( "blank-field.json", ModelValidator.BLANK_FIELD );
        expectedCodes.put ( "invalid-identifier.json", ModelValidator.INVALID_IDENTIFIER );
        expectedCodes.put ( "duplicate-identifier.json", ModelValidator.DUPLICATE_IDENTIFIER );
        expectedCodes.put ( "unresolved-reference.json", ModelValidator.UNRESOLVED_REFERENCE );
        expectedCodes.put ( "invalid-parameter-type.json", ModelValidator.INVALID_PARAMETER_TYPE );
        expectedCodes.put ( "invalid-enum-domain.json", ModelValidator.INVALID_ENUM_DOMAIN );
        expectedCodes.put ( "duplicate-reference.json", ModelValidator.DUPLICATE_REFERENCE );
        expectedCodes.put ( "invalid-concrete-value.json", ModelValidator.INVALID_CONCRETE_VALUE );
        expectedCodes.put ( "non-integral-integer.json", ModelValidator.NON_INTEGRAL_INTEGER );
        expectedCodes.put ( "enum-value-out-of-domain.json", ModelValidator.ENUM_VALUE_OUT_OF_DOMAIN );
        expectedCodes.put ( "duplicate-parameter-binding.json", ModelValidator.DUPLICATE_PARAMETER_BINDING );
        expectedCodes.put ( "rule-condition-not-permitted.json", ModelValidator.RULE_CONDITION_NOT_PERMITTED );

        // Prepare the codec and validator values needed by the semantic fixtures emit intended diagnostic codes
        // operation.

        AuthoringModelJsonCodec codec = new AuthoringModelJsonCodec ();
        ModelValidator validator      = new ModelValidator ();

        // Process each fixture supplied by expected codes entry set.

        for ( Map.Entry <String, String> fixture : expectedCodes.entrySet () )
        {
            // Prepare the model and diagnostic codes values needed by the semantic fixtures emit intended diagnostic
            // codes operation.

            AuthoringModel model = codec.read (
                readResource ( "/fixtures/invalid/" + fixture.getKey () )
            );
            List <String> diagnosticCodes = validator.validate ( model ).stream ()
                .map ( ValidationDiagnostic::getCode )
                .collect ( Collectors.toList () );

            // Verify the semantic fixtures emit intended diagnostic codes scenario against its expected outcome.

            assertThat ( diagnosticCodes )
                .as ( fixture.getKey () )
                .contains ( fixture.getValue () );
        }

        // Prepare the size limited model and size diagnostic codes values needed by the semantic fixtures emit
        // intended diagnostic codes operation.

        AuthoringModel sizeLimitedModel = codec.read (
            readResource ( "/fixtures/invalid/size-limit.json" )
        );
        List <String> sizeDiagnosticCodes = new ModelValidator ( new ModelLimits ( 0, 0 ) )
            .validate ( sizeLimitedModel )
            .stream ()
            .map ( ValidationDiagnostic::getCode )
            .collect ( Collectors.toList () );

        // Verify the semantic fixtures emit intended diagnostic codes scenario against its expected outcome.

        assertThat ( sizeDiagnosticCodes ).contains ( ModelValidator.SIZE_LIMIT_EXCEEDED );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: courierRules_selectExpectedActionsAndSpecificity
    //
    // Description:
    //
    //   Verifies that courier rules select expected actions and specificity.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void courierRules_selectExpectedActionsAndSpecificity ()
    {
        // Prepare the codec, model, compiled model, rule engine, conditions, and condition sets values needed by the
        // courier rules select expected actions and specificity operation.

        AuthoringModelJsonCodec codec = new AuthoringModelJsonCodec ();
        AuthoringModel model          = readTestModel ();
        CompiledModel compiledModel   = new AuthoringModelCompiler ().compile ( model, codec.revision ( model ) );
        RuleEngine ruleEngine         = new RuleEngine ( compiledModel.getRuleBase () );

        Map <String, AuthoringModel.ConditionDefinition> conditions = model.getConditions ().stream ()
            .collect (
                Collectors.toMap (
                    AuthoringModel.ConditionDefinition::getId,
                    Function.identity ()
                )
            );
        Map <String, AuthoringModel.ConditionSetDefinition> conditionSets = model.getConditionSets ().stream ()
            .collect (
                Collectors.toMap (
                    AuthoringModel.ConditionSetDefinition::getId,
                    Function.identity ()
                )
            );

        // Process each rule supplied by model rules.

        for ( AuthoringModel.RuleDefinition rule : model.getRules () )
        {
            // Prepare the condition set and payload values needed by the courier rules select expected actions and
            // specificity operation.

            AuthoringModel.ConditionSetDefinition conditionSet = conditionSets.get ( rule.getConditionSetId () );
            Map <String, Object> payload = new LinkedHashMap <String, Object> ();
            int expectedSpecificity      = 0;

            // Process each condition identifier supplied by condition set condition IDs.

            for ( String conditionIdentifier : conditionSet.getConditionIds () )
            {
                // Initialize the condition by applying get.

                AuthoringModel.ConditionDefinition condition = conditions.get ( conditionIdentifier );

                // Handle the branch where condition operator matches "any".

                if ( condition.getOperator ().equals ( "ANY" ) )
                {
                    expectedSpecificity++;

                    continue;
                }

                // Verify the courier rules select expected actions and specificity scenario against its expected
                // outcome.

                assertThat ( condition.getOperator () ).as ( conditionIdentifier ).isEqualTo ( "EQUALS" );
                expectedSpecificity += 2;

                // Store condition first value under condition parameter ID in the payload.

                payload.put ( condition.getParameterId (), condition.getFirstValue () );
            }

            // Initialize the evaluation result by applying evaluate, create occurrence, and get event ID.

            EvaluationResult evaluationResult = ruleEngine.evaluate (
                compiledModel.createOccurrence ( rule.getEventId (), payload )
            );

            // Verify the courier rules select expected actions and specificity scenario against its expected outcome.

            assertThat ( evaluationResult ).as ( rule.getId () ).isInstanceOf ( ActionResult.class );

            ActionResult actionResult = (ActionResult) evaluationResult;

            // Verify the courier rules select expected actions and specificity scenario against its expected outcome.

            assertThat ( actionResult.getRuleId ().getValue () ).as ( rule.getId () ).isEqualTo ( rule.getId () );
            assertThat ( actionResult.getActionId ().getValue () ).as ( rule.getId () ).isEqualTo ( rule.getActionId () );
            assertThat ( actionResult.getSpecificity () ).as ( rule.getId () ).isEqualTo ( expectedSpecificity );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: unmatchedCourierOccurrence_returnsNoAction
    //
    // Description:
    //
    //   Verifies that unmatched courier occurrence returns no action.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void unmatchedCourierOccurrence_returnsNoAction ()
    {
        // Prepare the codec, model, compiled model, and result values needed by the unmatched courier occurrence
        // returns no action operation.

        AuthoringModelJsonCodec codec = new AuthoringModelJsonCodec ();
        AuthoringModel model          = readTestModel ();
        CompiledModel compiledModel   = new AuthoringModelCompiler ().compile ( model, codec.revision ( model ) );
        EvaluationResult result       = new RuleEngine ( compiledModel.getRuleBase () ).evaluate (
            compiledModel.createOccurrence (
                "event-cancel-order",
                Map.of ( "parameter-order-reference", "ORDER-1000" )
            )
        );

        // Verify the unmatched courier occurrence returns no action scenario against its expected outcome.

        assertThat ( result.getOutcome () ).isEqualTo ( EvaluationOutcome.NO_ACTION );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: publishedSchema_declaresDraftTwentyTwentyTwelve
    //
    // Description:
    //
    //   Verifies that published schema declares draft twenty twenty twelve.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void publishedSchema_declaresDraftTwentyTwentyTwelve () throws IOException
    {
        // Prepare the schema and condition schema values needed by the published schema declares draft twenty twenty
        // twelve operation.

        String schema = readResource ( "/schema/eca-authoring-model.schema.json" );
        JsonNode conditionSchema = new ObjectMapper ().readTree ( schema )
            .path ( "$defs" )
            .path ( "condition" );

        // Verify the published schema declares draft twenty twenty twelve scenario against its expected outcome.

        assertThat ( schema ).contains ( "https://json-schema.org/draft/2020-12/schema" );
        assertThat ( schema ).contains ( "\"additionalProperties\": false" );
        assertThat ( conditionSchema.path ( "properties" ).path ( "value" ).path ( "$ref" ).asText () )
            .isEqualTo ( "#/$defs/concreteValue" );
        assertThat ( conditionSchema.path ( "properties" ).path ( "secondValue" ).path ( "$ref" ).asText () )
            .isEqualTo ( "#/$defs/concreteValue" );
        assertThat ( conditionSchema.path ( "oneOf" ).size () ).isEqualTo ( 4 );
        assertThat ( conditionSchema.path ( "oneOf" ).toString () )
            .contains ( "\"const\":\"ANY\"", "BETWEEN_EXCLUSIVE", "BETWEEN_INCLUSIVE" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readTestModel
    //
    // Description:
    //
    //   Performs the read test model operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static AuthoringModel readTestModel ()
    {
        // Return the result produced by read.

        return new AuthoringModelJsonCodec ().read ( readResource ( TEST_MODEL_RESOURCE ) );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readResource
    //
    // Description:
    //
    //   Performs the read resource operation.
    //
    // Arguments:
    //
    //   resourceName (String):
    //     The resource name to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String readResource ( String resourceName )
    {
        // Open the scoped resources for the protected operation and close them automatically afterward.

        try ( InputStream resourceStream = AuthoringModelPhaseThreeTest.class.getResourceAsStream ( resourceName ) )
        {
            // Verify the read resource scenario against its expected outcome.

            assertThat ( resourceStream ).as ( resourceName ).isNotNull ();

            // Return a newly constructed string containing the operation result.

            return new String ( resourceStream.readAllBytes (), StandardCharsets.UTF_8 );
        }

        // Handle I/O failures captured as exception.

        catch ( IOException exception )
        {
            throw new IllegalStateException ( "Could not read resource " + resourceName + ".", exception );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: minimalBindingModel
    //
    // Description:
    //
    //   Performs the minimal binding model operation.
    //
    // Arguments:
    //
    //   binding (String):
    //     The binding to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String minimalBindingModel ( String binding )
    {
        // Return the result produced by formatted.

        return """
            {
                "schemaVersion": "1.0",
                "modelId": "binding-model",
                "name": "Binding model",
                "description": "Exercises one binding.",
                "parameters": [
                    {
                        "id": "parameter-value",
                        "name": "Value",
                        "description": "String value.",
                        "type": "STRING"
                    }
                ],
                "payloads": [
                    {
                        "id": "payload-value",
                        "name": "Value payload",
                        "description": "Permits the string value.",
                        "parameterIds": [ "parameter-value" ]
                    }
                ],
                "events": [
                    {
                        "id": "event-value",
                        "name": "Value event",
                        "description": "Carries the value.",
                        "payloadId": "payload-value"
                    }
                ],
                "conditions": [
                    {
                        "id": "condition-value",
                        "name": "Value condition",
                        "description": "Matches the value.",
                        "parameterId": "parameter-value"
                    }
                ],
                "conditionSets": [
                    {
                        "id": "condition-set-value",
                        "name": "Value set",
                        "description": "Contains the test binding.",
                        "bindings": { %s }
                    }
                ],
                "actions": [],
                "rules": []
            }
            """.formatted ( binding );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: reverseEntityOrdering
    //
    // Description:
    //
    //   Performs the reverse entity ordering operation.
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

    private static AuthoringModel reverseEntityOrdering ( AuthoringModel model )
    {
        // Return a newly constructed authoring model containing the operation result.

        return new AuthoringModel (
            model.getSchemaVersion (),
            model.getModelId (),
            model.getName (),
            model.getDescription (),
            reversed ( model.getParameters () ),
            reversed ( model.getPayloads () ),
            reversed ( model.getEvents () ),
            reversed ( model.getConditions () ),
            reversed ( model.getConditionSets () ),
            reversed ( model.getActions () ),
            reversed ( model.getRules () )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: reversed
    //
    // Description:
    //
    //   Performs the reversed operation.
    //
    // Arguments:
    //
    //   values (List <T>):
    //     The values to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static <T> List <T> reversed ( List <T> values )
    {
        // Initialize the reversed values with a new array list.

        ArrayList <T> reversedValues = new ArrayList <T> ( values );

        // Reverse the collected values for the alternate-order check.

        Collections.reverse ( reversedValues );

        // Return the reversed values to the caller.

        return reversedValues;
    }
}
