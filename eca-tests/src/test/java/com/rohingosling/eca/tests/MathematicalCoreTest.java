//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Directly verifies ENG-001 through ENG-018 and the immutable mathematical-core construction invariants.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.tests;

import static com.rohingosling.eca.tests.MathematicalCoreFixtures.conditionBindings;
import static com.rohingosling.eca.tests.MathematicalCoreFixtures.conditions;
import static com.rohingosling.eca.tests.MathematicalCoreFixtures.concrete;
import static com.rohingosling.eca.tests.MathematicalCoreFixtures.event;
import static com.rohingosling.eca.tests.MathematicalCoreFixtures.integerParameter;
import static com.rohingosling.eca.tests.MathematicalCoreFixtures.payload;
import static com.rohingosling.eca.tests.MathematicalCoreFixtures.payloadBindings;
import static com.rohingosling.eca.tests.MathematicalCoreFixtures.presentNull;
import static com.rohingosling.eca.tests.MathematicalCoreFixtures.rule;
import static com.rohingosling.eca.tests.MathematicalCoreFixtures.stringParameter;
import static com.rohingosling.eca.tests.MathematicalCoreFixtures.wildcard;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.rohingosling.eca.domain.ActionResult;
import com.rohingosling.eca.domain.CompiledConditionSet;
import com.rohingosling.eca.domain.CompiledRule;
import com.rohingosling.eca.domain.CompiledRuleBase;
import com.rohingosling.eca.domain.ConditionValue;
import com.rohingosling.eca.domain.EventDefinition;
import com.rohingosling.eca.domain.EventOccurrence;
import com.rohingosling.eca.domain.EvaluationOutcome;
import com.rohingosling.eca.domain.EvaluationResult;
import com.rohingosling.eca.domain.NoActionResult;
import com.rohingosling.eca.domain.ParameterDefinition;
import com.rohingosling.eca.domain.ParameterId;
import com.rohingosling.eca.domain.ParameterType;
import com.rohingosling.eca.domain.Payload;
import com.rohingosling.eca.domain.PayloadValue;
import com.rohingosling.eca.engine.ConditionMatcher;
import com.rohingosling.eca.engine.RuleEngine;
import com.rohingosling.eca.engine.SpecificityPolicy;

import org.junit.jupiter.api.Test;

//*********************************************************************************************************************
// Class: MathematicalCoreTest
//
// Description:
//
//   Directly verifies ENG-001 through ENG-018 and the immutable mathematical-core construction invariants.
//
//*********************************************************************************************************************

final class MathematicalCoreTest
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    private static final ParameterDefinition KEY_ZERO  = stringParameter ( "key-zero" );
    private static final ParameterDefinition KEY_ONE   = stringParameter ( "key-one" );
    private static final ParameterDefinition KEY_TWO   = stringParameter ( "key-two" );
    private static final ParameterDefinition KEY_EXTRA = stringParameter ( "key-extra" );

    private static final EventDefinition EVENT_ALPHA = event (
        "event-alpha",
        KEY_ZERO,
        KEY_ONE,
        KEY_TWO,
        KEY_EXTRA
    );
    private static final EventDefinition EVENT_BETA = event (
        "event-beta",
        KEY_ZERO,
        KEY_ONE,
        KEY_TWO,
        KEY_EXTRA
    );

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: eng001_eng017_invalidOccurrenceIsRejected
    //
    // Description:
    //
    //   Verifies that eng001 eng017 invalid occurrence is rejected.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void eng001_eng017_invalidOccurrenceIsRejected ()
    {
        // Prepare the restricted event and unpermitted payload values needed by the eng001 eng017 invalid occurrence
        // is rejected operation.

        EventDefinition restrictedEvent = event ( "event-restricted", KEY_ZERO );
        Payload unpermittedPayload       = payload ( concrete ( KEY_ONE, "value" ) );

        // Verify the eng001 eng017 invalid occurrence is rejected scenario against its expected outcome.

        assertThatThrownBy ( () -> new EventOccurrence ( null, payload () ) )
            .isInstanceOf ( NullPointerException.class );
        assertThatThrownBy ( () -> new EventOccurrence ( restrictedEvent, null ) )
            .isInstanceOf ( NullPointerException.class );
        assertThatThrownBy ( () -> new EventOccurrence ( restrictedEvent, unpermittedPayload ) )
            .isInstanceOf ( IllegalArgumentException.class );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: eng002_eventIndexConsidersOnlyMatchingEvent
    //
    // Description:
    //
    //   Verifies that eng002 event index considers only matching event.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void eng002_eventIndexConsidersOnlyMatchingEvent ()
    {
        // Prepare the other event rule, rule engine, and result values needed by the eng002 event index considers only
        // matching event operation.

        CompiledRule otherEventRule = rule (
            "rule-a",
            EVENT_BETA,
            conditions (),
            "action-other-event"
        );
        RuleEngine ruleEngine = new RuleEngine ( new CompiledRuleBase ( List.of ( otherEventRule ) ) );

        EvaluationResult result = ruleEngine.evaluate ( new EventOccurrence ( EVENT_ALPHA, payload () ) );

        // Verify the eng002 event index considers only matching event scenario against its expected outcome.

        assertThat ( result ).isSameAs ( NoActionResult.getInstance () );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: eng003_exactEqualityAndWildcardMatch
    //
    // Description:
    //
    //   Verifies that eng003 exact equality and wildcard match.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void eng003_exactEqualityAndWildcardMatch ()
    {
        // Prepare the condition set and condition matcher values needed by the eng003 exact equality and wildcard
        // match operation.

        CompiledConditionSet conditionSet = conditions (
            concrete ( KEY_ZERO, "value" ),
            wildcard ( KEY_ONE )
        );
        ConditionMatcher conditionMatcher = new ConditionMatcher ();

        // Verify the eng003 exact equality and wildcard match scenario against its expected outcome.

        assertThat (
            conditionMatcher.matches (
                conditionSet,
                payload ( concrete ( KEY_ZERO, "value" ), presentNull ( KEY_ONE ) )
            )
        ).isTrue ();
        assertThat (
            conditionMatcher.matches (
                conditionSet,
                payload ( concrete ( KEY_ZERO, "value" ) )
            )
        ).isTrue ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: eng004_extraPayloadBindingDoesNotInvalidateMatch
    //
    // Description:
    //
    //   Verifies that eng004 extra payload binding does not invalidate match.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void eng004_extraPayloadBindingDoesNotInvalidateMatch ()
    {
        // Prepare the condition set and condition matcher values needed by the eng004 extra payload binding does not
        // invalidate match operation.

        CompiledConditionSet conditionSet = conditions ( concrete ( KEY_ZERO, "value" ) );
        ConditionMatcher conditionMatcher = new ConditionMatcher ();

        // Verify the eng004 extra payload binding does not invalidate match scenario against its expected outcome.

        assertThat (
            conditionMatcher.matches (
                conditionSet,
                payload (
                    concrete ( KEY_ZERO, "value" ),
                    concrete ( KEY_EXTRA, "extra" )
                )
            )
        ).isTrue ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: eng005_nullAbsentUnequalAndConjunctionContradictionsFail
    //
    // Description:
    //
    //   Verifies that eng005 null absent unequal and conjunction contradictions fail.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void eng005_nullAbsentUnequalAndConjunctionContradictionsFail ()
    {
        // Prepare the condition set and condition matcher values needed by the eng005 null absent unequal and
        // conjunction contradictions fail operation.

        CompiledConditionSet conditionSet = conditions (
            concrete ( KEY_ZERO, "value" ),
            concrete ( KEY_ONE, "second" )
        );
        ConditionMatcher conditionMatcher = new ConditionMatcher ();

        // Verify the eng005 null absent unequal and conjunction contradictions fail scenario against its expected
        // outcome.

        assertThat (
            conditionMatcher.matches (
                conditionSet,
                payload ( presentNull ( KEY_ZERO ), concrete ( KEY_ONE, "second" ) )
            )
        ).isFalse ();
        assertThat (
            conditionMatcher.matches (
                conditionSet,
                payload ( concrete ( KEY_ONE, "second" ) )
            )
        ).isFalse ();
        assertThat (
            conditionMatcher.matches (
                conditionSet,
                payload ( concrete ( KEY_ZERO, "different" ), concrete ( KEY_ONE, "second" ) )
            )
        ).isFalse ();
        assertThat (
            conditionMatcher.matches (
                conditionSet,
                payload ( concrete ( KEY_ZERO, "value" ), concrete ( KEY_ONE, "different" ) )
            )
        ).isFalse ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: eng006_emptyConditionMatchesEveryValidPayload
    //
    // Description:
    //
    //   Verifies that eng006 empty condition matches every valid payload.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void eng006_emptyConditionMatchesEveryValidPayload ()
    {
        // Initialize the condition matcher with a new condition matcher.

        ConditionMatcher conditionMatcher = new ConditionMatcher ();

        // Verify the eng006 empty condition matches every valid payload scenario against its expected outcome.

        assertThat ( conditionMatcher.matches ( conditions (), payload () ) ).isTrue ();
        assertThat (
            conditionMatcher.matches (
                conditions (),
                payload (
                    concrete ( KEY_ZERO, "value" ),
                    presentNull ( KEY_ONE )
                )
            )
        ).isTrue ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: eng007_specificityWeightsConcreteWildcardAndOmission
    //
    // Description:
    //
    //   Verifies that eng007 specificity weights concrete wildcard and omission.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void eng007_specificityWeightsConcreteWildcardAndOmission ()
    {
        // Prepare the specificity policy and condition set values needed by the eng007 specificity weights concrete
        // wildcard and omission operation.

        SpecificityPolicy specificityPolicy = new SpecificityPolicy ();
        CompiledConditionSet conditionSet    = conditions (
            concrete ( KEY_ZERO, "value" ),
            wildcard ( KEY_ONE )
        );

        // Verify the eng007 specificity weights concrete wildcard and omission scenario against its expected outcome.

        assertThat ( specificityPolicy.calculate ( conditionSet ) ).isEqualTo ( 3 );
        assertThat ( specificityPolicy.calculate ( conditions () ) ).isZero ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: eng008_nonmatchingHighSpecificityRuleCannotCompete
    //
    // Description:
    //
    //   Verifies that eng008 nonmatching high specificity rule cannot compete.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void eng008_nonmatchingHighSpecificityRuleCannotCompete ()
    {
        // Prepare the nonmatching rule, matching rule, rule engine, and result values needed by the eng008 nonmatching
        // high specificity rule cannot compete operation.

        CompiledRule nonmatchingRule = rule (
            "rule-high",
            EVENT_ALPHA,
            conditions (
                concrete ( KEY_ZERO, "different" ),
                concrete ( KEY_ONE, "second" )
            ),
            "action-high"
        );
        CompiledRule matchingRule = rule (
            "rule-low",
            EVENT_ALPHA,
            conditions ( wildcard ( KEY_ZERO ) ),
            "action-low"
        );
        RuleEngine ruleEngine = new RuleEngine (
            new CompiledRuleBase ( List.of ( nonmatchingRule, matchingRule ) )
        );

        ActionResult result = (ActionResult) ruleEngine.evaluate (
            new EventOccurrence ( EVENT_ALPHA, payload ( concrete ( KEY_ZERO, "value" ) ) )
        );

        // Verify the eng008 nonmatching high specificity rule cannot compete scenario against its expected outcome.

        assertThat ( result.getActionId ().getValue () ).isEqualTo ( "action-low" );
        assertThat ( result.getSpecificity () ).isEqualTo ( 1 );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: eng009_greatestMatchingSpecificityWins
    //
    // Description:
    //
    //   Verifies that eng009 greatest matching specificity wins.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void eng009_greatestMatchingSpecificityWins ()
    {
        // Prepare the general rule, specific rule, rule engine, and result values needed by the eng009 greatest
        // matching specificity wins operation.

        CompiledRule generalRule = rule (
            "rule-general",
            EVENT_ALPHA,
            conditions ( wildcard ( KEY_ZERO ) ),
            "action-general"
        );
        CompiledRule specificRule = rule (
            "rule-specific",
            EVENT_ALPHA,
            conditions ( concrete ( KEY_ZERO, "value" ) ),
            "action-specific"
        );
        RuleEngine ruleEngine = new RuleEngine (
            new CompiledRuleBase ( List.of ( generalRule, specificRule ) )
        );

        ActionResult result = (ActionResult) ruleEngine.evaluate (
            new EventOccurrence ( EVENT_ALPHA, payload ( concrete ( KEY_ZERO, "value" ) ) )
        );

        // Verify the eng009 greatest matching specificity wins scenario against its expected outcome.

        assertThat ( result.getRuleId ().getValue () ).isEqualTo ( "rule-specific" );
        assertThat ( result.getSpecificity () ).isEqualTo ( 2 );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: eng010_eng011_eng013_lexicographicTieWinsIndependentlyOfSourceOrder
    //
    // Description:
    //
    //   Verifies that eng010 eng011 eng013 lexicographic tie wins independently of source order.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void eng010_eng011_eng013_lexicographicTieWinsIndependentlyOfSourceOrder ()
    {
        // Prepare the rule zulu, rule alpha, occurrence, forward engine, reverse engine, forward result, and reverse
        // result values needed by the eng010 eng011 eng013 lexicographic tie wins independently of source order
        // operation.

        CompiledRule ruleZulu = rule (
            "rule-zulu",
            EVENT_ALPHA,
            conditions ( wildcard ( KEY_ZERO ) ),
            "action-zulu"
        );
        CompiledRule ruleAlpha = rule (
            "rule-alpha",
            EVENT_ALPHA,
            conditions ( wildcard ( KEY_ONE ) ),
            "action-alpha"
        );
        EventOccurrence occurrence = new EventOccurrence ( EVENT_ALPHA, payload () );
        RuleEngine forwardEngine   = new RuleEngine (
            new CompiledRuleBase ( List.of ( ruleZulu, ruleAlpha ) )
        );
        RuleEngine reverseEngine   = new RuleEngine (
            new CompiledRuleBase ( List.of ( ruleAlpha, ruleZulu ) )
        );

        ActionResult forwardResult = (ActionResult) forwardEngine.evaluate ( occurrence );
        ActionResult reverseResult = (ActionResult) reverseEngine.evaluate ( occurrence );

        // Verify the eng010 eng011 eng013 lexicographic tie wins independently of source order scenario against its
        // expected outcome.

        assertThat ( forwardResult.getRuleId ().getValue () ).isEqualTo ( "rule-alpha" );
        assertThat ( reverseResult ).isEqualTo ( forwardResult );
        assertThat ( forwardResult.getOutcome () ).isEqualTo ( EvaluationOutcome.ACTION );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: eng011_mapIterationOrderCannotAffectResult
    //
    // Description:
    //
    //   Verifies that eng011 map iteration order cannot affect result.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void eng011_mapIterationOrderCannotAffectResult ()
    {
        // Initialize the forward condition bindings with a new linked hash map.

        Map <ParameterId, ConditionValue> forwardConditionBindings = new LinkedHashMap <ParameterId, ConditionValue> ();

        // Perform the put, get parameter ID, and concrete calls required by the eng011 map iteration order cannot
        // affect result operation.

        forwardConditionBindings.put ( KEY_ZERO.getParameterId (), concrete ( KEY_ZERO, "zero" ) );
        forwardConditionBindings.put ( KEY_ONE.getParameterId (), concrete ( KEY_ONE, "one" ) );

        // Initialize the reverse condition bindings with a new linked hash map.

        Map <ParameterId, ConditionValue> reverseConditionBindings = new LinkedHashMap <ParameterId, ConditionValue> ();

        // Perform the put, get parameter ID, and concrete calls required by the eng011 map iteration order cannot
        // affect result operation.

        reverseConditionBindings.put ( KEY_ONE.getParameterId (), concrete ( KEY_ONE, "one" ) );
        reverseConditionBindings.put ( KEY_ZERO.getParameterId (), concrete ( KEY_ZERO, "zero" ) );

        // Initialize the forward payload bindings with a new linked hash map.

        Map <ParameterId, PayloadValue> forwardPayloadBindings = new LinkedHashMap <ParameterId, PayloadValue> ();

        // Perform the put, get parameter ID, and concrete calls required by the eng011 map iteration order cannot
        // affect result operation.

        forwardPayloadBindings.put ( KEY_ZERO.getParameterId (), concrete ( KEY_ZERO, "zero" ) );
        forwardPayloadBindings.put ( KEY_ONE.getParameterId (), concrete ( KEY_ONE, "one" ) );

        // Initialize the reverse payload bindings with a new linked hash map.

        Map <ParameterId, PayloadValue> reversePayloadBindings = new LinkedHashMap <ParameterId, PayloadValue> ();

        // Perform the put, get parameter ID, and concrete calls required by the eng011 map iteration order cannot
        // affect result operation.

        reversePayloadBindings.put ( KEY_ONE.getParameterId (), concrete ( KEY_ONE, "one" ) );
        reversePayloadBindings.put ( KEY_ZERO.getParameterId (), concrete ( KEY_ZERO, "zero" ) );

        // Prepare the forward rule, reverse rule, forward engine, reverse engine, forward result, and reverse result
        // values needed by the eng011 map iteration order cannot affect result operation.

        CompiledRule forwardRule = rule (
            "rule-a",
            EVENT_ALPHA,
            new CompiledConditionSet ( forwardConditionBindings ),
            "action-a"
        );
        CompiledRule reverseRule = rule (
            "rule-a",
            EVENT_ALPHA,
            new CompiledConditionSet ( reverseConditionBindings ),
            "action-a"
        );
        RuleEngine forwardEngine = new RuleEngine ( new CompiledRuleBase ( List.of ( forwardRule ) ) );
        RuleEngine reverseEngine = new RuleEngine ( new CompiledRuleBase ( List.of ( reverseRule ) ) );

        EvaluationResult forwardResult = forwardEngine.evaluate (
            new EventOccurrence ( EVENT_ALPHA, new Payload ( forwardPayloadBindings ) )
        );
        EvaluationResult reverseResult = reverseEngine.evaluate (
            new EventOccurrence ( EVENT_ALPHA, new Payload ( reversePayloadBindings ) )
        );

        // Verify the eng011 map iteration order cannot affect result scenario against its expected outcome.

        assertThat ( reverseResult ).isEqualTo ( forwardResult );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: eng012_noMatchingRuleReturnsExplicitNoAction
    //
    // Description:
    //
    //   Verifies that eng012 no matching rule returns explicit no action.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void eng012_noMatchingRuleReturnsExplicitNoAction ()
    {
        // Prepare the rule, rule engine, and result values needed by the eng012 no matching rule returns explicit no
        // action operation.

        CompiledRule rule = rule (
            "rule-a",
            EVENT_ALPHA,
            conditions ( concrete ( KEY_ZERO, "required" ) ),
            "action-a"
        );
        RuleEngine ruleEngine = new RuleEngine ( new CompiledRuleBase ( List.of ( rule ) ) );

        EvaluationResult result = ruleEngine.evaluate ( new EventOccurrence ( EVENT_ALPHA, payload () ) );

        // Verify the eng012 no matching rule returns explicit no action scenario against its expected outcome.

        assertThat ( result ).isInstanceOf ( NoActionResult.class );
        assertThat ( result.getOutcome () ).isEqualTo ( EvaluationOutcome.NO_ACTION );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: eng014_repeatedQueryReturnsEqualResult
    //
    // Description:
    //
    //   Verifies that eng014 repeated query returns equal result.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void eng014_repeatedQueryReturnsEqualResult ()
    {
        // Prepare the rule, rule engine, occurrence, first result, and second result values needed by the eng014
        // repeated query returns equal result operation.

        CompiledRule rule = rule (
            "rule-a",
            EVENT_ALPHA,
            conditions (),
            "action-a"
        );
        RuleEngine ruleEngine       = new RuleEngine ( new CompiledRuleBase ( List.of ( rule ) ) );
        EventOccurrence occurrence = new EventOccurrence ( EVENT_ALPHA, payload () );

        EvaluationResult firstResult  = ruleEngine.evaluate ( occurrence );
        EvaluationResult secondResult = ruleEngine.evaluate ( occurrence );

        // Verify the eng014 repeated query returns equal result scenario against its expected outcome.

        assertThat ( secondResult ).isEqualTo ( firstResult );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: eng015_evaluationDoesNotMutateOccurrenceOrRuleBase
    //
    // Description:
    //
    //   Verifies that eng015 evaluation does not mutate occurrence or rule base.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void eng015_evaluationDoesNotMutateOccurrenceOrRuleBase ()
    {
        // Prepare the source payload bindings, source condition bindings, payload, condition set, rule, source rules,
        // rule base, occurrence, rule engine, and before values needed by the eng015 evaluation does not mutate
        // occurrence or rule base operation.

        Map <ParameterId, PayloadValue> sourcePayloadBindings = payloadBindings (
            concrete ( KEY_ZERO, "value" )
        );
        Map <ParameterId, ConditionValue> sourceConditionBindings = conditionBindings (
            concrete ( KEY_ZERO, "value" )
        );
        Payload payload                        = new Payload ( sourcePayloadBindings );
        CompiledConditionSet conditionSet      = new CompiledConditionSet ( sourceConditionBindings );
        CompiledRule rule                      = rule ( "rule-a", EVENT_ALPHA, conditionSet, "action-a" );
        List <CompiledRule> sourceRules        = new ArrayList <CompiledRule> ( List.of ( rule ) );
        CompiledRuleBase ruleBase              = new CompiledRuleBase ( sourceRules );
        EventOccurrence occurrence             = new EventOccurrence ( EVENT_ALPHA, payload );
        RuleEngine ruleEngine                  = new RuleEngine ( ruleBase );
        Map <ParameterId, PayloadValue> before = new LinkedHashMap <ParameterId, PayloadValue> (
            occurrence.getPayload ().getBindings ()
        );

        // Verify the eng015 evaluation does not mutate occurrence or rule base scenario against its expected outcome.

        ruleEngine.evaluate ( occurrence );

        assertThat ( occurrence.getPayload ().getBindings () ).isEqualTo ( before );
        assertThat ( ruleBase.getRules () ).containsExactly ( rule );

        sourcePayloadBindings.clear ();
        sourceConditionBindings.clear ();
        sourceRules.clear ();

        assertThat ( occurrence.getPayload ().getBindings () ).hasSize ( 1 );
        assertThat ( conditionSet.getBindings () ).hasSize ( 1 );
        assertThat ( ruleBase.getRules () ).containsExactly ( rule );
        assertThatThrownBy ( () -> occurrence.getPayload ().getBindings ().clear () )
            .isInstanceOf ( UnsupportedOperationException.class );
        assertThatThrownBy ( () -> conditionSet.getBindings ().clear () )
            .isInstanceOf ( UnsupportedOperationException.class );
        assertThatThrownBy ( () -> ruleBase.getRules ().clear () )
            .isInstanceOf ( UnsupportedOperationException.class );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: eng016_actionSelectionReturnsIdentifierWithoutExecutionBehavior
    //
    // Description:
    //
    //   Verifies that eng016 action selection returns identifier without execution behavior.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void eng016_actionSelectionReturnsIdentifierWithoutExecutionBehavior ()
    {
        // Prepare the rule, rule engine, and result values needed by the eng016 action selection returns identifier
        // without execution behavior operation.

        CompiledRule rule = rule (
            "rule-a",
            EVENT_ALPHA,
            conditions (),
            "action-selected-only"
        );
        RuleEngine ruleEngine = new RuleEngine ( new CompiledRuleBase ( List.of ( rule ) ) );

        ActionResult result = (ActionResult) ruleEngine.evaluate (
            new EventOccurrence ( EVENT_ALPHA, payload () )
        );

        // Verify the eng016 action selection returns identifier without execution behavior scenario against its
        // expected outcome.

        assertThat ( result.getActionId ().getValue () ).isEqualTo ( "action-selected-only" );
        assertThat ( result.getActionId ().getClass ().getDeclaredMethods () ).isEmpty ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: eng018_nullAndAbsenceMatrixMatchesTechnicalNote
    //
    // Description:
    //
    //   Verifies the ENG-018 null-and-absence matrix defined by the stateless ECA rule-engine technical note
    //   (`docs/technical-note/stateless-eca-rule-engine.pdf`).
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void eng018_nullAndAbsenceMatrixMatchesTechnicalNote ()
    {
        // Prepare the matcher, specificity policy, and scenario matrix used to exercise every null-and-absence state.

        ConditionMatcher conditionMatcher = new ConditionMatcher ();
        SpecificityPolicy specificityPolicy = new SpecificityPolicy ();

        List <Scenario> scenarios = Arrays.asList (
            scenario ( "VVV", "VVV", true, 6 ),
            scenario ( "NVV", "VVV", false, 6 ),
            scenario ( "VVV", "NVV", true, 5 ),
            scenario ( "NVV", "NVV", true, 5 ),
            scenario ( "VNV", "NVV", false, 5 ),
            scenario ( "NVV", "VNV", false, 5 ),
            scenario ( "NNV", "VNV", false, 5 ),
            scenario ( "NNV", "VVN", false, 5 ),
            scenario ( "VNV", "NNV", true, 4 ),
            scenario ( "VVN", "NNV", false, 4 ),
            scenario ( "NNN", "VVV", false, 6 ),
            scenario ( "VVV", "NNN", true, 3 ),
            scenario ( "VVV", "VVV", true, 6 ),
            scenario ( "AVV", "VVV", false, 6 ),
            scenario ( "VVV", "AVV", true, 4 ),
            scenario ( "AVV", "AVV", true, 4 ),
            scenario ( "VAV", "AVV", false, 4 ),
            scenario ( "AVV", "VAV", false, 4 ),
            scenario ( "AAV", "VAV", false, 4 ),
            scenario ( "AAV", "VVA", false, 4 ),
            scenario ( "VAV", "AAV", true, 2 ),
            scenario ( "VVA", "AAV", false, 2 ),
            scenario ( "AAA", "VVV", false, 6 ),
            scenario ( "VVV", "AAA", true, 0 )
        );

        int scenarioNumber = 1;

        // Process each scenario supplied by scenarios.

        for ( Scenario scenario : scenarios )
        {
            // Build the payload and condition set represented by the current matrix row.

            Payload scenarioPayload = createScenarioPayload ( scenario.getPayloadStates () );
            CompiledConditionSet scenarioConditionSet = createScenarioConditionSet (
                scenario.getConditionStates ()
            );

            // Verify the current row's match result and specificity against the technical-note expectations.

            assertThat ( conditionMatcher.matches ( scenarioConditionSet, scenarioPayload ) )
                .as ( "technical-note scenario %s match", scenarioNumber )
                .isEqualTo ( scenario.isExpectedMatch () );
            assertThat ( specificityPolicy.calculate ( scenarioConditionSet ) )
                .as ( "technical-note scenario %s specificity", scenarioNumber )
                .isEqualTo ( scenario.getExpectedSpecificity () );

            scenarioNumber++;
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: concreteValuesEnforceParameterDomains
    //
    // Description:
    //
    //   Performs the concrete values enforce parameter domains operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void concreteValuesEnforceParameterDomains ()
    {
        // Prepare the quantity parameter and courier parameter values needed by the concrete values enforce parameter
        // domains operation.

        ParameterDefinition quantityParameter = integerParameter ( "quantity" );
        ParameterDefinition courierParameter  = new ParameterDefinition (
            new ParameterId ( "courier" ),
            ParameterType.ENUM,
            List.of ( "LOCAL", "INTERNATIONAL" )
        );

        // Verify the concrete values enforce parameter domains scenario against its expected outcome.

        assertThat ( concrete ( quantityParameter, Long.valueOf ( 12L ) ).getValue () ).isEqualTo ( 12L );
        assertThat ( concrete ( courierParameter, "LOCAL" ).getValue () ).isEqualTo ( "LOCAL" );
        assertThatThrownBy ( () -> concrete ( quantityParameter, Integer.valueOf ( 12 ) ) )
            .isInstanceOf ( IllegalArgumentException.class );
        assertThatThrownBy ( () -> concrete ( courierParameter, "UNKNOWN" ) )
            .isInstanceOf ( IllegalArgumentException.class );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: compiledRulesEnforceConcreteEventSchemaButPermitExternalWildcard
    //
    // Description:
    //
    //   Performs the compiled rules enforce concrete event schema but permit external wildcard operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void compiledRulesEnforceConcreteEventSchemaButPermitExternalWildcard ()
    {
        // Initialize the restricted event by applying event.

        EventDefinition restrictedEvent = event ( "event-restricted", KEY_ZERO );

        // Verify the compiled rules enforce concrete event schema but permit external wildcard scenario against its
        // expected outcome.

        assertThatThrownBy (
            () -> rule (
                "rule-invalid",
                restrictedEvent,
                conditions ( concrete ( KEY_ONE, "value" ) ),
                "action-invalid"
            )
        ).isInstanceOf ( IllegalArgumentException.class );

        // Initialize the wildcard rule by applying rule, conditions, and wildcard.

        CompiledRule wildcardRule = rule (
            "rule-valid",
            restrictedEvent,
            conditions ( wildcard ( KEY_ONE ) ),
            "action-valid"
        );

        // Verify the compiled rules enforce concrete event schema but permit external wildcard scenario against its
        // expected outcome.

        assertThat ( wildcardRule.getConditionSet ().getBindings () ).containsKey ( KEY_ONE.getParameterId () );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: finiteRuleBaseRequiresUniqueIdentifiers
    //
    // Description:
    //
    //   Performs the finite rule base requires unique identifiers operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void finiteRuleBaseRequiresUniqueIdentifiers ()
    {
        // Prepare the first rule and second rule values needed by the finite rule base requires unique identifiers
        // operation.

        CompiledRule firstRule  = rule ( "rule-duplicate", EVENT_ALPHA, conditions (), "action-a" );
        CompiledRule secondRule = rule ( "rule-duplicate", EVENT_ALPHA, conditions (), "action-b" );

        // Verify the finite rule base requires unique identifiers scenario against its expected outcome.

        assertThatThrownBy ( () -> new CompiledRuleBase ( List.of ( firstRule, secondRule ) ) )
            .isInstanceOf ( IllegalArgumentException.class );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: scenario
    //
    // Description:
    //
    //   Performs the scenario operation.
    //
    // Arguments:
    //
    //   payloadStates (String):
    //     The payload states to use.
    //
    //   conditionStates (String):
    //     The condition states to use.
    //
    //   expectedMatch (boolean):
    //     The expected match to use.
    //
    //   expectedSpecificity (int):
    //     The expected specificity to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private Scenario scenario (
        String payloadStates,
        String conditionStates,
        boolean expectedMatch,
        int expectedSpecificity
    )
    {
        // Return a newly constructed scenario containing the operation result.

        return new Scenario ( payloadStates, conditionStates, expectedMatch, expectedSpecificity );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createScenarioPayload
    //
    // Description:
    //
    //   Performs the create scenario payload operation.
    //
    // Arguments:
    //
    //   states (String):
    //     The states to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private Payload createScenarioPayload ( String states )
    {
        ParameterDefinition[] parameterDefinitions = { KEY_ZERO, KEY_ONE, KEY_TWO };

        // Initialize the payload values with a new array list.

        ArrayList <PayloadValue> payloadValues      = new ArrayList <PayloadValue> ();

        // Repeat the loop while i is less than states length.

        for ( int i = 0; i < states.length (); i++ )
        {
            // Handle the branch where states char at i equals 'v'.

            if ( states.charAt ( i ) == 'V' )
            {
                // Add concrete parameter definitions i "value" to the payload values.

                payloadValues.add ( concrete ( parameterDefinitions [ i ], "value" ) );
            }

            // Handle the alternative where states char at i equals 'n'.

            else if ( states.charAt ( i ) == 'N' )
            {
                // Add present null parameter definitions i to the payload values.

                payloadValues.add ( presentNull ( parameterDefinitions [ i ] ) );
            }
        }

        // Return the result produced by payload.

        return payload ( payloadValues.toArray ( new PayloadValue [ 0 ] ) );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createScenarioConditionSet
    //
    // Description:
    //
    //   Performs the create scenario condition set operation.
    //
    // Arguments:
    //
    //   states (String):
    //     The states to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private CompiledConditionSet createScenarioConditionSet ( String states )
    {
        ParameterDefinition[] parameterDefinitions = { KEY_ZERO, KEY_ONE, KEY_TWO };

        // Initialize the condition values with a new array list.

        ArrayList <ConditionValue> conditionValues  = new ArrayList <ConditionValue> ();

        // Repeat the loop while i is less than states length.

        for ( int i = 0; i < states.length (); i++ )
        {
            // Handle the branch where states char at i equals 'v'.

            if ( states.charAt ( i ) == 'V' )
            {
                // Add concrete parameter definitions i "value" to the condition values.

                conditionValues.add ( concrete ( parameterDefinitions [ i ], "value" ) );
            }

            // Handle the alternative where states char at i equals 'n'.

            else if ( states.charAt ( i ) == 'N' )
            {
                // Add wildcard parameter definitions i to the condition values.

                conditionValues.add ( wildcard ( parameterDefinitions [ i ] ) );
            }
        }

        // Return the result produced by conditions.

        return conditions ( conditionValues.toArray ( new ConditionValue [ 0 ] ) );
    }

    //*****************************************************************************************************************
    // Class: Scenario
    //
    // Description:
    //
    //   Provides the scenario behavior.
    //
    //*****************************************************************************************************************

    private static final class Scenario
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String payloadStates;
        private final String conditionStates;
        private final boolean expectedMatch;
        private final int expectedSpecificity;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getPayloadStates
        //
        // Description:
        //
        //   Returns the payload states.
        //
        // Returns:
        //
        //   The payload states.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getPayloadStates ()
        {
            // Return the payload states to the caller.

            return this.payloadStates;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getConditionStates
        //
        // Description:
        //
        //   Returns the condition states.
        //
        // Returns:
        //
        //   The condition states.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getConditionStates ()
        {
            // Return the condition states to the caller.

            return this.conditionStates;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: isExpectedMatch
        //
        // Description:
        //
        //   Indicates whether expected match.
        //
        // Returns:
        //
        //   `true` when the condition is satisfied; otherwise `false`.
        //
        //-------------------------------------------------------------------------------------------------------------

        public boolean isExpectedMatch ()
        {
            // Return the expected match to the caller.

            return this.expectedMatch;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getExpectedSpecificity
        //
        // Description:
        //
        //   Returns the expected specificity.
        //
        // Returns:
        //
        //   The expected specificity.
        //
        //-------------------------------------------------------------------------------------------------------------

        public int getExpectedSpecificity ()
        {
            // Return the expected specificity to the caller.

            return this.expectedSpecificity;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: Scenario
        //
        // Description:
        //
        //   Creates the Scenario instance from the supplied values.
        //
        // Arguments:
        //
        //   payloadStates (String):
        //     The payload states to use.
        //
        //   conditionStates (String):
        //     The condition states to use.
        //
        //   expectedMatch (boolean):
        //     The expected match to use.
        //
        //   expectedSpecificity (int):
        //     The expected specificity to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private Scenario (
            String payloadStates,
            String conditionStates,
            boolean expectedMatch,
            int expectedSpecificity
        )
        {
            this.payloadStates       = payloadStates;
            this.conditionStates     = conditionStates;
            this.expectedMatch       = expectedMatch;
            this.expectedSpecificity = expectedSpecificity;
        }
    }
}
