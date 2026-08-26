//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies the shared Java and TypeScript Phase 15 compiled-summary, evaluation-vector, and null/absence contracts.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohingosling.eca.domain.ActionResult;
import com.rohingosling.eca.domain.CompiledConditionSet;
import com.rohingosling.eca.domain.CompiledRule;
import com.rohingosling.eca.domain.ConcreteValue;
import com.rohingosling.eca.domain.ConditionValue;
import com.rohingosling.eca.domain.EvaluationOutcome;
import com.rohingosling.eca.domain.EvaluationResult;
import com.rohingosling.eca.domain.ExplicitWildcard;
import com.rohingosling.eca.domain.ParameterDefinition;
import com.rohingosling.eca.domain.ParameterId;
import com.rohingosling.eca.domain.ParameterType;
import com.rohingosling.eca.domain.Payload;
import com.rohingosling.eca.domain.PayloadValue;
import com.rohingosling.eca.domain.PresentNullPayloadValue;
import com.rohingosling.eca.engine.ConditionMatcher;
import com.rohingosling.eca.engine.RuleEngine;
import com.rohingosling.eca.engine.SpecificityPolicy;
import com.rohingosling.eca.json.AuthoringModelJsonCodec;
import com.rohingosling.eca.json.EventOccurrenceJsonCodec;
import com.rohingosling.eca.model.AuthoringModel;
import com.rohingosling.eca.model.AuthoringModelCompiler;
import com.rohingosling.eca.model.CompiledModel;

//*********************************************************************************************************************
// Class: WebEvaluatorParityPhaseFifteenTest
//
// Description:
//
//   Verifies the shared Java and TypeScript Phase 15 compiled-summary, evaluation-vector, and null/absence contracts.
//
//*********************************************************************************************************************

final class WebEvaluatorParityPhaseFifteenTest
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    private static final String CONTRACT_ROOT = "/fixtures/contract/";

    //=================================================================================================================
    // Tests
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: sharedCompiledSummaryMatchesResolvedJavaModel
    //
    // Description:
    //
    //   Verifies that the checked-in compiled summary matches the resolved Java model and specificity policy.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void sharedCompiledSummaryMatchesResolvedJavaModel () throws IOException
    {
        // Prepare the source model, compiled model, expected summary, and specificity policy.

        AuthoringModelJsonCodec codec = new AuthoringModelJsonCodec ();
        AuthoringModel sourceModel = codec.read (
            readResource ( CONTRACT_ROOT + "valid/phase-14-ordering-and-integers.json" )
        );
        CompiledModel compiledModel = new AuthoringModelCompiler ().compile (
            sourceModel,
            codec.revision ( sourceModel )
        );
        JsonNode expectedSummary = new ObjectMapper ().readTree (
            readResource ( CONTRACT_ROOT + "expected/phase-15-compiled-summary.json" )
        );
        SpecificityPolicy specificityPolicy = new SpecificityPolicy ();

        // Verify the stable model identity, revision, and sorted resolved identifier inventories.

        assertThat ( compiledModel.getAuthoringModel ().getModelId () )
            .isEqualTo ( expectedSummary.path ( "modelId" ).asText () );
        assertThat ( compiledModel.getRevision () )
            .isEqualTo ( expectedSummary.path ( "revision" ).asText () );
        assertThat ( compiledModel.getParameterDefinitions ().keySet ().stream ().sorted ().toList () )
            .containsExactlyElementsOf ( textValues ( expectedSummary.path ( "parameterIds" ) ) );
        assertThat (
            sourceModel.getPayloads ().stream ()
                .map ( AuthoringModel.PayloadDefinition::getId )
                .sorted ()
                .toList ()
        )
            .containsExactlyElementsOf ( textValues ( expectedSummary.path ( "payloadIds" ) ) );
        assertThat ( compiledModel.getEventDefinitions ().keySet ().stream ().sorted ().toList () )
            .containsExactlyElementsOf ( textValues ( expectedSummary.path ( "eventIds" ) ) );
        assertThat ( compiledModel.getConditionSets ().keySet ().stream ().sorted ().toList () )
            .containsExactlyElementsOf ( textValues ( expectedSummary.path ( "conditionSetIds" ) ) );
        assertThat ( compiledModel.getActions ().keySet ().stream ().sorted ().toList () )
            .containsExactlyElementsOf ( textValues ( expectedSummary.path ( "actionIds" ) ) );

        // Verify every resolved rule reference and compiled specificity in stable rule-ID order.

        List <CompiledRule> rules = compiledModel.getRuleBase ().getRules ().stream ()
            .sorted ( ( first, second ) -> first.getRuleId ().compareTo ( second.getRuleId () ) )
            .toList ();
        JsonNode expectedRules = expectedSummary.path ( "rules" );

        assertThat ( rules ).hasSize ( expectedRules.size () );

        for ( int i = 0; i < rules.size (); i++ )
        {
            CompiledRule rule = rules.get ( i );
            JsonNode expectedRule = expectedRules.get ( i );

            assertThat ( rule.getRuleId ().getValue () ).isEqualTo ( expectedRule.path ( "id" ).asText () );
            assertThat ( rule.getEventId ().getValue () ).isEqualTo ( expectedRule.path ( "eventId" ).asText () );
            assertThat ( rule.getActionId ().getValue () ).isEqualTo ( expectedRule.path ( "actionId" ).asText () );
            assertThat ( specificityPolicy.calculate ( rule.getConditionSet () ) )
                .isEqualTo ( expectedRule.path ( "specificity" ).asInt () );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: sharedEvaluationVectorsReturnExactJavaResults
    //
    // Description:
    //
    //   Verifies every checked-in golden occurrence and result against the Java compiler and evaluator.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void sharedEvaluationVectorsReturnExactJavaResults () throws IOException
    {
        // Prepare the shared contract, codecs, and compiled model inventory.

        JsonNode contract = new ObjectMapper ().readTree (
            readResource ( CONTRACT_ROOT + "evaluation/phase-15-evaluation-vectors.json" )
        );
        AuthoringModelJsonCodec modelCodec = new AuthoringModelJsonCodec ();
        EventOccurrenceJsonCodec occurrenceCodec = new EventOccurrenceJsonCodec ();
        LinkedHashMap <String, CompiledModel> compiledModels = new LinkedHashMap <String, CompiledModel> ();

        // Compile every referenced model and verify its checked-in canonical revision.

        contract.path ( "models" ).properties ().forEach ( modelEntry ->
        {
            String modelIdentifier = modelEntry.getKey ();
            JsonNode modelReference = modelEntry.getValue ();
            AuthoringModel model = modelCodec.read (
                readProjectResource ( modelReference.path ( "path" ).asText () )
            );
            String expectedRevision = modelReference.path ( "revision" ).asText ();

            assertThat ( modelCodec.revision ( model ) ).as ( modelIdentifier ).isEqualTo ( expectedRevision );
            compiledModels.put (
                modelIdentifier,
                new AuthoringModelCompiler ().compile ( model, expectedRevision )
            );
        } );

        // Evaluate every occurrence and compare its outcome, selected identifiers, specificity, and model revision.

        for ( JsonNode vector : contract.path ( "vectors" ) )
        {
            String vectorIdentifier = vector.path ( "id" ).asText ();
            CompiledModel compiledModel = compiledModels.get ( vector.path ( "model" ).asText () );
            EvaluationResult result = new RuleEngine ( compiledModel.getRuleBase () ).evaluate (
                compiledModel.createOccurrence (
                    occurrenceCodec.read ( vector.path ( "occurrence" ).asText () )
                )
            );
            JsonNode expected = vector.path ( "expected" );

            assertThat ( result.getOutcome ().name () )
                .as ( vectorIdentifier )
                .isEqualTo ( expected.path ( "outcome" ).asText () );
            assertThat ( compiledModel.getRevision () )
                .as ( vectorIdentifier )
                .isEqualTo ( expected.path ( "modelRevision" ).asText () );

            if ( result.getOutcome () == EvaluationOutcome.ACTION )
            {
                ActionResult actionResult = (ActionResult) result;

                assertThat ( actionResult.getActionId ().getValue () )
                    .as ( vectorIdentifier )
                    .isEqualTo ( expected.path ( "actionId" ).asText () );
                assertThat ( actionResult.getRuleId ().getValue () )
                    .as ( vectorIdentifier )
                    .isEqualTo ( expected.path ( "ruleId" ).asText () );
                assertThat ( actionResult.getSpecificity () )
                    .as ( vectorIdentifier )
                    .isEqualTo ( expected.path ( "specificity" ).asInt () );
            }
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: sharedNullAndAbsenceMatrixMatchesJavaSemantics
    //
    // Description:
    //
    //   Verifies all 24 shared technical-note null and absence rows against the Java mathematical core.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void sharedNullAndAbsenceMatrixMatchesJavaSemantics () throws IOException
    {
        // Prepare the shared matrix, parameter definitions, matcher, and specificity policy.

        JsonNode scenarios = new ObjectMapper ().readTree (
            readResource ( CONTRACT_ROOT + "evaluation/phase-15-null-absence-matrix.json" )
        );
        List <ParameterDefinition> parameters = List.of (
            stringParameter ( "key-0" ),
            stringParameter ( "key-1" ),
            stringParameter ( "key-2" )
        );
        ConditionMatcher conditionMatcher = new ConditionMatcher ();
        SpecificityPolicy specificityPolicy = new SpecificityPolicy ();

        assertThat ( scenarios ).hasSize ( 24 );

        // Verify each matrix row's match and specificity results.

        for ( int i = 0; i < scenarios.size (); i++ )
        {
            JsonNode scenario = scenarios.get ( i );
            CompiledConditionSet conditionSet = matrixConditionSet (
                scenario.path ( "condition" ).asText (),
                parameters
            );
            Payload payload = matrixPayload ( scenario.path ( "payload" ).asText (), parameters );

            assertThat ( conditionMatcher.matches ( conditionSet, payload ) )
                .as ( "matrix row %s", i + 1 )
                .isEqualTo ( scenario.path ( "match" ).asBoolean () );
            assertThat ( specificityPolicy.calculate ( conditionSet ) )
                .as ( "matrix row %s", i + 1 )
                .isEqualTo ( scenario.path ( "specificity" ).asInt () );
        }
    }

    //=================================================================================================================
    // Helpers
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: matrixConditionSet
    //
    // Description:
    //
    //   Creates the compiled condition set represented by one matrix state string.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static CompiledConditionSet matrixConditionSet (
        String states,
        List <ParameterDefinition> parameters
    )
    {
        LinkedHashMap <ParameterId, ConditionValue> bindings =
            new LinkedHashMap <ParameterId, ConditionValue> ();

        for ( int i = 0; i < states.length (); i++ )
        {
            ParameterDefinition parameter = parameters.get ( i );

            if ( states.charAt ( i ) == 'V' )
            {
                bindings.put ( parameter.getParameterId (), new ConcreteValue ( parameter, "value" ) );
            }
            else if ( states.charAt ( i ) == 'N' )
            {
                bindings.put ( parameter.getParameterId (), new ExplicitWildcard ( parameter ) );
            }
        }

        return new CompiledConditionSet ( bindings );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: matrixPayload
    //
    // Description:
    //
    //   Creates the compiled payload represented by one matrix state string.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Payload matrixPayload ( String states, List <ParameterDefinition> parameters )
    {
        LinkedHashMap <ParameterId, PayloadValue> bindings = new LinkedHashMap <ParameterId, PayloadValue> ();

        for ( int i = 0; i < states.length (); i++ )
        {
            ParameterDefinition parameter = parameters.get ( i );

            if ( states.charAt ( i ) == 'V' )
            {
                bindings.put ( parameter.getParameterId (), new ConcreteValue ( parameter, "value" ) );
            }
            else if ( states.charAt ( i ) == 'N' )
            {
                bindings.put ( parameter.getParameterId (), new PresentNullPayloadValue ( parameter ) );
            }
        }

        return new Payload ( bindings );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: stringParameter
    //
    // Description:
    //
    //   Creates one string parameter definition for the shared matrix.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static ParameterDefinition stringParameter ( String identifier )
    {
        return new ParameterDefinition (
            new ParameterId ( identifier ),
            ParameterType.STRING,
            List.of ()
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: textValues
    //
    // Description:
    //
    //   Copies one JSON string array into a Java string list.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static List <String> textValues ( JsonNode arrayNode )
    {
        ArrayList <String> values = new ArrayList <String> ();

        for ( JsonNode value : arrayNode )
        {
            values.add ( value.asText () );
        }

        return values;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readProjectResource
    //
    // Description:
    //
    //   Resolves one project-relative shared model path to its Maven test-resource path.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String readProjectResource ( String projectRelativePath )
    {
        String testResourcePrefix = "eca-tests/src/test/resources/";
        String resourceName = projectRelativePath.startsWith ( testResourcePrefix )
            ? "/" + projectRelativePath.substring ( testResourcePrefix.length () )
            : "/" + projectRelativePath;

        return readResource ( resourceName );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readResource
    //
    // Description:
    //
    //   Reads one UTF-8 classpath resource with normalized line endings.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String readResource ( String resourceName )
    {
        try (
            InputStream resourceStream = WebEvaluatorParityPhaseFifteenTest.class.getResourceAsStream (
                resourceName
            )
        )
        {
            if ( resourceStream == null )
            {
                throw new IllegalArgumentException ( "Missing test resource: " + resourceName );
            }

            return new String ( resourceStream.readAllBytes (), StandardCharsets.UTF_8 )
                .replace ( "\r\n", "\n" );
        }
        catch ( IOException exception )
        {
            throw new IllegalStateException ( "Could not read test resource: " + resourceName, exception );
        }
    }
}
