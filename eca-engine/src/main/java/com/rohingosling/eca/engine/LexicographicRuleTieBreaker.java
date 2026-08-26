//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Supplies the technical note's deterministic selector parameter by choosing the lexicographically smallest stable
//   identifier from each nonempty finite tie (`docs/technical-note/stateless-eca-rule-engine.pdf`).
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.engine;

import java.util.Collection;
import java.util.Objects;

import com.rohingosling.eca.domain.RuleId;

//*********************************************************************************************************************
// Class: LexicographicRuleTieBreaker
//
// Description:
//
//   Supplies the technical note's deterministic selector parameter by choosing the lexicographically smallest stable
//   identifier from each nonempty finite tie (`docs/technical-note/stateless-eca-rule-engine.pdf`).
//
//*********************************************************************************************************************

public final class LexicographicRuleTieBreaker implements RuleTieBreaker
{
    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: select
    //
    // Description:
    //
    //   Performs the select operation.
    //
    // Arguments:
    //
    //   ruleIds (Collection <RuleId>):
    //     The rule ids to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public RuleId select ( Collection <RuleId> ruleIds )
    {
        // Validate the required rule IDs before continuing.

        Objects.requireNonNull ( ruleIds, "ruleIds" );

        RuleId selectedRuleId = null;

        // Process each rule ID supplied by rule IDs.

        for ( RuleId ruleId : ruleIds )
        {
            // Validate the required rule ID before continuing.

            Objects.requireNonNull ( ruleId, "ruleIds must not contain null" );

            // Handle the branch where selected rule ID is unavailable or rule ID compare to selected rule ID is less
            // than 0.

            if ( selectedRuleId == null || ruleId.compareTo ( selectedRuleId ) < 0 )
            {
                selectedRuleId = ruleId;
            }
        }

        // Reject the operation when selected rule ID is unavailable.

        if ( selectedRuleId == null )
        {
            throw new IllegalArgumentException ( "A rule tie must contain at least one rule identifier." );
        }

        // Return the selected rule ID to the caller.

        return selectedRuleId;
    }
}
