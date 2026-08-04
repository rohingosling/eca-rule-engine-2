//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Implements the pure event-filter, conjunctive-match, maximum-specificity, and deterministic tie-selection query
//   defined by the stateless ECA rule-engine technical note (`docs/technical-note/stateless-eca-rule-engine.pdf`).
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.rohingosling.eca.domain.ActionResult;
import com.rohingosling.eca.domain.CompiledRule;
import com.rohingosling.eca.domain.CompiledRuleBase;
import com.rohingosling.eca.domain.EvaluationResult;
import com.rohingosling.eca.domain.EventId;
import com.rohingosling.eca.domain.EventOccurrence;
import com.rohingosling.eca.domain.NoActionResult;
import com.rohingosling.eca.domain.RuleId;

//*********************************************************************************************************************
// Class: RuleEngine
//
// Description:
//
//   Implements the pure event-filter, conjunctive-match, maximum-specificity, and deterministic tie-selection query
//   defined by the stateless ECA rule-engine technical note (`docs/technical-note/stateless-eca-rule-engine.pdf`).
//
//*********************************************************************************************************************

public final class RuleEngine
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final ConditionMatcher conditionMatcher;
    private final RuleTieBreaker ruleTieBreaker;
    private final Map <EventId, List <IndexedRule>> rulesByEventId;

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/2: RuleEngine
    //
    // Description:
    //
    //   Creates the RuleEngine instance from the supplied values.
    //
    // Arguments:
    //
    //   ruleBase (CompiledRuleBase):
    //     The rule base to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public RuleEngine ( CompiledRuleBase ruleBase )
    {
        // Delegate initialization to the primary rule engine constructor.

        this (
            ruleBase,
            new ConditionMatcher (),
            new SpecificityPolicy (),
            new LexicographicRuleTieBreaker ()
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 2/2: RuleEngine
    //
    // Description:
    //
    //   Creates the RuleEngine instance from the supplied values.
    //
    // Arguments:
    //
    //   ruleBase (CompiledRuleBase):
    //     The rule base to use.
    //
    //   conditionMatcher (ConditionMatcher):
    //     The condition matcher to use.
    //
    //   specificityPolicy (SpecificityPolicy):
    //     The specificity policy to use.
    //
    //   ruleTieBreaker (RuleTieBreaker):
    //     The rule tie breaker to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public RuleEngine
    (
        CompiledRuleBase ruleBase,
        ConditionMatcher conditionMatcher,
        SpecificityPolicy specificityPolicy,
        RuleTieBreaker ruleTieBreaker
    )
    {
        // Validate the required rule base, condition matcher, specificity policy, and rule tie breaker before
        // continuing.

        Objects.requireNonNull ( ruleBase, "ruleBase" );
        Objects.requireNonNull ( conditionMatcher, "conditionMatcher" );
        Objects.requireNonNull ( specificityPolicy, "specificityPolicy" );
        Objects.requireNonNull ( ruleTieBreaker, "ruleTieBreaker" );

        this.conditionMatcher = conditionMatcher;
        this.ruleTieBreaker   = ruleTieBreaker;

        // Update the rules by event ID from the create rule index result.

        this.rulesByEventId   = this.createRuleIndex ( ruleBase, specificityPolicy );
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: evaluate
    //
    // Description:
    //
    //   Performs the evaluate operation.
    //
    // Arguments:
    //
    //   occurrence (EventOccurrence):
    //     The occurrence to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public EvaluationResult evaluate ( EventOccurrence occurrence )
    {
        // Validate the required occurrence before continuing.

        Objects.requireNonNull ( occurrence, "occurrence" );

        // Prepare the candidate rules and tied rules values needed by the evaluate operation.

        List <IndexedRule> candidateRules = this.rulesByEventId.getOrDefault (
            occurrence.getEventId (),
            Collections.emptyList ()
        );
        List <IndexedRule> tiedRules = new ArrayList <IndexedRule> ();
        int maximumSpecificity      = -1;

        // Process each indexed rule supplied by candidate rules.

        for ( IndexedRule indexedRule : candidateRules )
        {
            // Skip the current item when condition matcher does not match the supplied values.

            if ( !this.conditionMatcher.matches (
                indexedRule.getRule ().getConditionSet (),
                occurrence.getPayload ()
            ) )
            {
                continue;
            }

            // Handle the branch where indexed rule specificity exceeds maximum specificity.

            if ( indexedRule.getSpecificity () > maximumSpecificity )
            {
                // Apply clear and add to the tied rules for the evaluate operation.

                tiedRules.clear ();
                tiedRules.add ( indexedRule );

                // Update the maximum specificity from the get specificity result.

                maximumSpecificity = indexedRule.getSpecificity ();
            }

            // Handle the alternative where indexed rule specificity equals maximum specificity.

            else if ( indexedRule.getSpecificity () == maximumSpecificity )
            {
                // Add indexed rule to the tied rules.

                tiedRules.add ( indexedRule );
            }
        }

        // Stop this path and return its result when tied rules contains no values.

        if ( tiedRules.isEmpty () )
        {
            // Return the shared no action result instance when tied rules contains no values.

            return NoActionResult.getInstance ();
        }

        // Initialize the tied rule IDs with a new array list.

        ArrayList <RuleId> tiedRuleIds = new ArrayList <RuleId> ();

        // Process each indexed rule supplied by tied rules.

        for ( IndexedRule indexedRule : tiedRules )
        {
            // Add indexed rule ID to the tied rule IDs.

            tiedRuleIds.add ( indexedRule.getRule ().getRuleId () );
        }

        // Initialize the selected rule ID by applying select.

        RuleId selectedRuleId = this.ruleTieBreaker.select ( tiedRuleIds );

        // Process each indexed rule supplied by tied rules.

        for ( IndexedRule indexedRule : tiedRules )
        {
            // Stop this path and return its result when indexed rule ID matches selected rule ID.

            if ( indexedRule.getRule ().getRuleId ().equals ( selectedRuleId ) )
            {
                // Return a newly constructed action result containing the operation result when indexed rule ID
                // matches selected rule ID.

                return new ActionResult (
                    indexedRule.getRule ().getActionId (),
                    indexedRule.getRule ().getRuleId (),
                    indexedRule.getSpecificity ()
                );
            }
        }

        throw new IllegalStateException ( "Rule tie-breaker selected an identifier outside the tie." );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createRuleIndex
    //
    // Description:
    //
    //   Performs the create rule index operation.
    //
    // Arguments:
    //
    //   ruleBase (CompiledRuleBase):
    //     The rule base to use.
    //
    //   specificityPolicy (SpecificityPolicy):
    //     The specificity policy to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private Map <EventId, List <IndexedRule>> createRuleIndex (
        CompiledRuleBase ruleBase,
        SpecificityPolicy specificityPolicy
    )
    {
        // Initialize the mutable index with a new linked hash map.

        LinkedHashMap <EventId, List <IndexedRule>> mutableIndex =
            new LinkedHashMap <EventId, List <IndexedRule>> ();

        // Process each rule supplied by rule base rules.

        for ( CompiledRule rule : ruleBase.getRules () )
        {
            // Initialize the indexed rules by applying compute if absent and get event ID.

            List <IndexedRule> indexedRules = mutableIndex.computeIfAbsent (
                rule.getEventId (),
                eventId -> new ArrayList <IndexedRule> ()
            );

            // Add new indexed rule specificity policy calculate rule condition set to the indexed rules.

            indexedRules.add (
                new IndexedRule (
                    rule,
                    specificityPolicy.calculate ( rule.getConditionSet () )
                )
            );
        }

        // Initialize the copied index with a new linked hash map.

        LinkedHashMap <EventId, List <IndexedRule>> copiedIndex =
            new LinkedHashMap <EventId, List <IndexedRule>> ();

        // Process each entry supplied by mutable index entry set.

        for ( Map.Entry <EventId, List <IndexedRule>> entry : mutableIndex.entrySet () )
        {
            // Initialize the copied rules by applying unmodifiable list and get value.

            List <IndexedRule> copiedRules = Collections.unmodifiableList (
                new ArrayList <IndexedRule> ( entry.getValue () )
            );

            // Store copied rules under entry key in the copied index.

            copiedIndex.put ( entry.getKey (), copiedRules );
        }

        // Return an immutable copy of copied index.

        return Collections.unmodifiableMap ( copiedIndex );
    }

    //*****************************************************************************************************************
    // Class: IndexedRule
    //
    // Description:
    //
    //   Provides the indexed rule behavior.
    //
    //*****************************************************************************************************************

    private static final class IndexedRule
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final CompiledRule rule;
        private final int specificity;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getRule
        //
        // Description:
        //
        //   Returns the rule.
        //
        // Returns:
        //
        //   The rule.
        //
        //-------------------------------------------------------------------------------------------------------------

        public CompiledRule getRule ()
        {
            // Return the rule to the caller.

            return this.rule;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getSpecificity
        //
        // Description:
        //
        //   Returns the specificity.
        //
        // Returns:
        //
        //   The specificity.
        //
        //-------------------------------------------------------------------------------------------------------------

        public int getSpecificity ()
        {
            // Return the specificity to the caller.

            return this.specificity;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: IndexedRule
        //
        // Description:
        //
        //   Creates the IndexedRule instance from the supplied values.
        //
        // Arguments:
        //
        //   rule (CompiledRule):
        //     The rule to use.
        //
        //   specificity (int):
        //     The specificity to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private IndexedRule ( CompiledRule rule, int specificity )
        {
            // Validate the required rule before continuing.

            this.rule        = Objects.requireNonNull ( rule, "rule" );
            this.specificity = specificity;
        }
    }
}
