//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Represents a finite immutable rule base with unique stable rule identifiers.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

//*********************************************************************************************************************
// Class: CompiledRuleBase
//
// Description:
//
//   Represents a finite immutable rule base with unique stable rule identifiers.
//
//*********************************************************************************************************************

public final class CompiledRuleBase
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final List <CompiledRule> rules;

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getRules
    //
    // Description:
    //
    //   Returns the rules.
    //
    // Returns:
    //
    //   The rules.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public List <CompiledRule> getRules ()
    {
        // Return the rules to the caller.

        return this.rules;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: CompiledRuleBase
    //
    // Description:
    //
    //   Creates the CompiledRuleBase instance from the supplied values.
    //
    // Arguments:
    //
    //   rules (Collection <CompiledRule>):
    //     The rules to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public CompiledRuleBase ( Collection <CompiledRule> rules )
    {
        // Validate the required rules before continuing.

        Objects.requireNonNull ( rules, "rules" );

        // Prepare the copied rules and rule IDs values needed by the compiled rule base operation.

        ArrayList <CompiledRule> copiedRules = new ArrayList <CompiledRule> ();
        Set <RuleId> ruleIds                 = new LinkedHashSet <RuleId> ();

        // Process each rule supplied by rules.

        for ( CompiledRule rule : rules )
        {
            // Validate the required rule before continuing.

            Objects.requireNonNull ( rule, "rules must not contain null" );

            // Reject the operation when rule IDs add does not succeed.

            if ( !ruleIds.add ( rule.getRuleId () ) )
            {
                throw new IllegalArgumentException ( "Rule identifiers must be unique." );
            }

            // Add rule to the copied rules.

            copiedRules.add ( rule );
        }

        // Update the rules from the unmodifiable list result.

        this.rules = Collections.unmodifiableList ( copiedRules );
    }
}
