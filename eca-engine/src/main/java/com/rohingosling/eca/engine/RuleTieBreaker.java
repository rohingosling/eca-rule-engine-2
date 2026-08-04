//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Defines the deterministic selector applied only to nonempty maximum-specificity rule-ID ties.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.engine;

import java.util.Collection;

import com.rohingosling.eca.domain.RuleId;

//---------------------------------------------------------------------------------------------------------------------
// Interface: RuleTieBreaker
//
// Description:
//
//   Defines the deterministic selector applied only to nonempty maximum-specificity rule-ID ties.
//
//---------------------------------------------------------------------------------------------------------------------

public interface RuleTieBreaker
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

    RuleId select ( Collection <RuleId> ruleIds );
}
