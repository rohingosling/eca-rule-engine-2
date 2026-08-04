//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Identifies and lexically orders one compiled rule.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.domain;

//*********************************************************************************************************************
// Class: RuleId
//
// Description:
//
//   Identifies and lexically orders one compiled rule.
//
//*********************************************************************************************************************

public final class RuleId extends StableIdentifier implements Comparable <RuleId>
{
    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: RuleId
    //
    // Description:
    //
    //   Creates the RuleId instance from the supplied values.
    //
    // Arguments:
    //
    //   value (String):
    //     The value to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public RuleId ( String value )
    {
        // Initialize the inherited state through the base-class constructor.

        super ( value );
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: compareTo
    //
    // Description:
    //
    //   Performs the compare to operation.
    //
    // Arguments:
    //
    //   ruleId (RuleId):
    //     The rule id to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public int compareTo ( RuleId ruleId )
    {
        // Return the result produced by compare to.

        return this.getValue ().compareTo ( ruleId.getValue () );
    }
}
