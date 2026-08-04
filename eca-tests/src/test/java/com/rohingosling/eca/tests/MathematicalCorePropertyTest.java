//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies specificity, monotonic matching, deterministic ties, statelessness, and concurrent purity over generated
//   invariant-preserving finite models and occurrence sequences.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.tests;

import static com.rohingosling.eca.tests.MathematicalCoreFixtures.conditions;
import static com.rohingosling.eca.tests.MathematicalCoreFixtures.concrete;
import static com.rohingosling.eca.tests.MathematicalCoreFixtures.event;
import static com.rohingosling.eca.tests.MathematicalCoreFixtures.payload;
import static com.rohingosling.eca.tests.MathematicalCoreFixtures.rule;
import static com.rohingosling.eca.tests.MathematicalCoreFixtures.stringParameter;
import static com.rohingosling.eca.tests.MathematicalCoreFixtures.wildcard;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.rohingosling.eca.domain.ActionResult;
import com.rohingosling.eca.domain.CompiledConditionSet;
import com.rohingosling.eca.domain.CompiledRule;
import com.rohingosling.eca.domain.CompiledRuleBase;
import com.rohingosling.eca.domain.ConditionValue;
import com.rohingosling.eca.domain.EventDefinition;
import com.rohingosling.eca.domain.EventOccurrence;
import com.rohingosling.eca.domain.EvaluationResult;
import com.rohingosling.eca.domain.ParameterDefinition;
import com.rohingosling.eca.domain.ParameterId;
import com.rohingosling.eca.domain.Payload;
import com.rohingosling.eca.domain.PayloadValue;
import com.rohingosling.eca.engine.ConditionMatcher;
import com.rohingosling.eca.engine.RuleEngine;
import com.rohingosling.eca.engine.SpecificityPolicy;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Provide;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Test;

//*********************************************************************************************************************
// Class: MathematicalCorePropertyTest
//
// Description:
//
//   Verifies specificity, monotonic matching, deterministic ties, statelessness, and concurrent purity over generated
//   invariant-preserving finite models and occurrence sequences.
//
//*********************************************************************************************************************

final class MathematicalCorePropertyTest
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    private static final List <ParameterDefinition> PARAMETERS = Arrays.asList (
        stringParameter ( "parameter-0" ),
        stringParameter ( "parameter-1" ),
        stringParameter ( "parameter-2" ),
        stringParameter ( "parameter-3" ),
        stringParameter ( "parameter-4" ),
        stringParameter ( "parameter-5" )
    );
    private static final EventDefinition EVENT = new EventDefinition (
        new com.rohingosling.eca.domain.EventId ( "event-property" ),
        PARAMETERS
    );

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: generatedSpecificityEqualsWeightedBindingCounts
    //
    // Description:
    //
    //   Performs the generated specificity equals weighted binding counts operation.
    //
    // Arguments:
    //
    //   concreteBindingCount (int):
    //     The concrete binding count to use.
    //
    //   wildcardBindingCount (int):
    //     The wildcard binding count to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Property ( tries = 250 )
    void generatedSpecificityEqualsWeightedBindingCounts (
        @ForAll @IntRange ( min = 0, max = 6 ) int concreteBindingCount,
        @ForAll @IntRange ( min = 0, max = 6 ) int wildcardBindingCount
    )
    {
        // Initialize the condition values with a new array list.

        ArrayList <ConditionValue> conditionValues = new ArrayList <ConditionValue> ();

        // Repeat the loop while i is less than concrete binding count.

        for ( int i = 0; i < concreteBindingCount; i++ )
        {
            // Initialize the parameter definition by applying string parameter.

            ParameterDefinition parameterDefinition = stringParameter ( "concrete-" + i );

            // Add concrete parameter definition "value-" + i to the condition values.

            conditionValues.add ( concrete ( parameterDefinition, "value-" + i ) );
        }

        // Repeat the loop while i is less than wildcard binding count.

        for ( int i = 0; i < wildcardBindingCount; i++ )
        {
            // Initialize the parameter definition by applying string parameter.

            ParameterDefinition parameterDefinition = stringParameter ( "wildcard-" + i );

            // Add wildcard parameter definition to the condition values.

            conditionValues.add ( wildcard ( parameterDefinition ) );
        }

        // Initialize the condition set by applying conditions and to array.

        CompiledConditionSet conditionSet = conditions (
            conditionValues.toArray ( new ConditionValue [ 0 ] )
        );

        // Verify the generated specificity equals weighted binding counts scenario against its expected outcome.

        assertThat ( new SpecificityPolicy ().calculate ( conditionSet ) )
            .isEqualTo ( 2 * concreteBindingCount + wildcardBindingCount );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: generatedPayloadExtensionCannotInvalidateExistingMatch
    //
    // Description:
    //
    //   Performs the generated payload extension cannot invalidate existing match operation.
    //
    // Arguments:
    //
    //   conditionIndexes (Set <Integer>):
    //     The condition indexes to use.
    //
    //   extensionIndexes (Set <Integer>):
    //     The extension indexes to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Property ( tries = 250 )
    void generatedPayloadExtensionCannotInvalidateExistingMatch (
        @ForAll ( "parameterIndexSets" ) Set <Integer> conditionIndexes,
        @ForAll ( "parameterIndexSets" ) Set <Integer> extensionIndexes
    )
    {
        // Prepare the condition bindings and base payload bindings values needed by the generated payload extension
        // cannot invalidate existing match operation.

        Map <ParameterId, ConditionValue> conditionBindings = new LinkedHashMap <ParameterId, ConditionValue> ();
        Map <ParameterId, PayloadValue> basePayloadBindings = new LinkedHashMap <ParameterId, PayloadValue> ();

        // Process each condition index supplied by condition indexes.

        for ( Integer conditionIndex : conditionIndexes )
        {
            // Initialize the parameter definition by applying get and int value.

            ParameterDefinition parameterDefinition = PARAMETERS.get ( conditionIndex.intValue () );

            // Perform the put, get parameter ID, and concrete calls required by the generated payload extension cannot
            // invalidate existing match operation.

            conditionBindings.put (
                parameterDefinition.getParameterId (),
                concrete ( parameterDefinition, "value-" + conditionIndex )
            );
            basePayloadBindings.put (
                parameterDefinition.getParameterId (),
                concrete ( parameterDefinition, "value-" + conditionIndex )
            );
        }

        // Initialize the extended payload bindings with a new linked hash map.

        Map <ParameterId, PayloadValue> extendedPayloadBindings = new LinkedHashMap <ParameterId, PayloadValue> (
            basePayloadBindings
        );

        // Process each extension index supplied by extension indexes.

        for ( Integer extensionIndex : extensionIndexes )
        {
            // Initialize the parameter definition by applying get and int value.

            ParameterDefinition parameterDefinition = PARAMETERS.get ( extensionIndex.intValue () );

            // Perform the put if absent, get parameter ID, and concrete calls required by the generated payload
            // extension cannot invalidate existing match operation.

            extendedPayloadBindings.putIfAbsent (
                parameterDefinition.getParameterId (),
                concrete ( parameterDefinition, "extra-" + extensionIndex )
            );
        }

        // Prepare the condition matcher and condition set values needed by the generated payload extension cannot
        // invalidate existing match operation.

        ConditionMatcher conditionMatcher = new ConditionMatcher ();
        CompiledConditionSet conditionSet  = new CompiledConditionSet ( conditionBindings );

        // Verify the generated payload extension cannot invalidate existing match scenario against its expected
        // outcome.

        assertThat ( conditionMatcher.matches ( conditionSet, new Payload ( basePayloadBindings ) ) ).isTrue ();
        assertThat ( conditionMatcher.matches ( conditionSet, new Payload ( extendedPayloadBindings ) ) ).isTrue ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: generatedRuleOrderCannotChangeLexicographicTie
    //
    // Description:
    //
    //   Performs the generated rule order cannot change lexicographic tie operation.
    //
    // Arguments:
    //
    //   ruleIdentifierOrder (List <String>):
    //     The rule identifier order to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Property
    void generatedRuleOrderCannotChangeLexicographicTie (
        @ForAll ( "ruleIdentifierOrders" ) List <String> ruleIdentifierOrder
    )
    {
        // Initialize the rules with a new array list.

        ArrayList <CompiledRule> rules = new ArrayList <CompiledRule> ();

        // Process each rule identifier supplied by rule identifier order.

        for ( String ruleIdentifier : ruleIdentifierOrder )
        {
            // Add rule identifier event conditions "action-" + rule identifier to the rules.

            rules.add (
                rule (
                    ruleIdentifier,
                    EVENT,
                    conditions (),
                    "action-" + ruleIdentifier
                )
            );
        }

        // Prepare the rule engine and result values needed by the generated rule order cannot change lexicographic tie
        // operation.

        RuleEngine ruleEngine = new RuleEngine ( new CompiledRuleBase ( rules ) );
        ActionResult result   = (ActionResult) ruleEngine.evaluate (
            new EventOccurrence ( EVENT, payload () )
        );

        // Verify the generated rule order cannot change lexicographic tie scenario against its expected outcome.

        assertThat ( result.getRuleId ().getValue () ).isEqualTo ( "rule-alpha" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: generatedPriorOccurrenceSequenceCannotChangeTargetResult
    //
    // Description:
    //
    //   Performs the generated prior occurrence sequence cannot change target result operation.
    //
    // Arguments:
    //
    //   occurrenceStates (List <Integer>):
    //     The occurrence states to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Property ( tries = 150 )
    void generatedPriorOccurrenceSequenceCannotChangeTargetResult (
        @ForAll ( "occurrenceStateSequences" ) List <Integer> occurrenceStates
    )
    {
        // Prepare the parameter, wildcard rule, concrete rule, rule engine, target, and before values needed by the
        // generated prior occurrence sequence cannot change target result operation.

        ParameterDefinition parameter = PARAMETERS.get ( 0 );
        CompiledRule wildcardRule      = rule (
            "rule-wildcard",
            EVENT,
            conditions ( wildcard ( parameter ) ),
            "action-wildcard"
        );
        CompiledRule concreteRule = rule (
            "rule-concrete",
            EVENT,
            conditions ( concrete ( parameter, "target" ) ),
            "action-concrete"
        );
        RuleEngine ruleEngine       = new RuleEngine (
            new CompiledRuleBase ( List.of ( wildcardRule, concreteRule ) )
        );
        EventOccurrence target      = new EventOccurrence (
            EVENT,
            payload ( concrete ( parameter, "target" ) )
        );
        EvaluationResult before     = ruleEngine.evaluate ( target );

        // Process each occurrence state supplied by occurrence states.

        for ( Integer occurrenceState : occurrenceStates )
        {
            Payload generatedPayload;

            // Handle the branch where occurrence state int value equals 0.

            if ( occurrenceState.intValue () == 0 )
            {
                // Update the generated payload from the payload result.

                generatedPayload = payload ();
            }

            // Handle the alternative where occurrence state int value equals 1.

            else if ( occurrenceState.intValue () == 1 )
            {
                // Update the generated payload from the concrete result.

                generatedPayload = payload ( concrete ( parameter, "target" ) );
            }

            // Handle the alternative path when the preceding condition is not satisfied.

            else
            {
                // Update the generated payload from the concrete result.

                generatedPayload = payload ( concrete ( parameter, "other" ) );
            }

            // Complete the generated prior occurrence sequence cannot change target result step by calling evaluate.

            ruleEngine.evaluate ( new EventOccurrence ( EVENT, generatedPayload ) );
        }

        // Initialize the after by applying evaluate.

        EvaluationResult after = ruleEngine.evaluate ( target );

        // Verify the generated prior occurrence sequence cannot change target result scenario against its expected
        // outcome.

        assertThat ( after ).isEqualTo ( before );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: concurrentQueriesShareNoWritableEvaluationState
    //
    // Description:
    //
    //   Performs the concurrent queries share no writable evaluation state operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void concurrentQueriesShareNoWritableEvaluationState () throws Exception
    {
        // Prepare the parameter, wildcard rule, concrete rule, rule engine, target, expected result, executor service,
        // and tasks values needed by the concurrent queries share no writable evaluation state operation.

        ParameterDefinition parameter = PARAMETERS.get ( 0 );
        CompiledRule wildcardRule      = rule (
            "rule-wildcard",
            EVENT,
            conditions ( wildcard ( parameter ) ),
            "action-wildcard"
        );
        CompiledRule concreteRule = rule (
            "rule-concrete",
            EVENT,
            conditions ( concrete ( parameter, "target" ) ),
            "action-concrete"
        );
        RuleEngine ruleEngine  = new RuleEngine (
            new CompiledRuleBase ( List.of ( wildcardRule, concreteRule ) )
        );
        EventOccurrence target = new EventOccurrence (
            EVENT,
            payload ( concrete ( parameter, "target" ) )
        );
        EvaluationResult expectedResult = ruleEngine.evaluate ( target );
        ExecutorService executorService = Executors.newFixedThreadPool ( 8 );
        ArrayList <Callable <EvaluationResult>> tasks = new ArrayList <Callable <EvaluationResult>> ();

        // Repeat the loop while i is less than 500.

        for ( int i = 0; i < 500; i++ )
        {
            // Add - rule engine evaluate target to the tasks.

            tasks.add ( () -> ruleEngine.evaluate ( target ) );
        }

        try
        {
            // Initialize the futures by applying invoke all.

            List <Future <EvaluationResult>> futures = executorService.invokeAll ( tasks );

            // Process each future supplied by futures.

            for ( Future <EvaluationResult> future : futures )
            {
                // Verify the concurrent queries share no writable evaluation state scenario against its expected
                // outcome.

                assertThat ( future.get () ).isEqualTo ( expectedResult );
            }
        }

        // Complete the required cleanup regardless of how the protected operation finishes.

        finally
        {
            // Verify the concurrent queries share no writable evaluation state scenario against its expected outcome.

            executorService.shutdown ();
            assertThat ( executorService.awaitTermination ( 10, TimeUnit.SECONDS ) ).isTrue ();
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: parameterIndexSets
    //
    // Description:
    //
    //   Performs the parameter index sets operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Provide
    Arbitrary <Set <Integer>> parameterIndexSets ()
    {
        // Return the result produced by of max size.

        return Arbitraries.integers ()
            .between ( 0, PARAMETERS.size () - 1 )
            .set ()
            .ofMaxSize ( PARAMETERS.size () );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: ruleIdentifierOrders
    //
    // Description:
    //
    //   Performs the rule identifier orders operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Provide
    Arbitrary <List <String>> ruleIdentifierOrders ()
    {
        // Return the result produced by of.

        return Arbitraries.of (
            List.of ( "rule-alpha", "rule-bravo", "rule-charlie" ),
            List.of ( "rule-alpha", "rule-charlie", "rule-bravo" ),
            List.of ( "rule-bravo", "rule-alpha", "rule-charlie" ),
            List.of ( "rule-bravo", "rule-charlie", "rule-alpha" ),
            List.of ( "rule-charlie", "rule-alpha", "rule-bravo" ),
            List.of ( "rule-charlie", "rule-bravo", "rule-alpha" )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: occurrenceStateSequences
    //
    // Description:
    //
    //   Performs the occurrence state sequences operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Provide
    Arbitrary <List <Integer>> occurrenceStateSequences ()
    {
        // Return the result produced by of max size.

        return Arbitraries.integers ().between ( 0, 2 ).list ().ofMaxSize ( 40 );
    }
}
