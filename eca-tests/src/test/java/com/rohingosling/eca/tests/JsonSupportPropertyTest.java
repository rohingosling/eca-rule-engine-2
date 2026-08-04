//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies the jqwik and AssertJ property-test configuration through the shared JSON boundary.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.rohingosling.eca.json.JsonSupport;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

//*********************************************************************************************************************
// Class: JsonSupportPropertyTest
//
// Description:
//
//   Verifies the jqwik and AssertJ property-test configuration through the shared JSON boundary.
//
//*********************************************************************************************************************

final class JsonSupportPropertyTest
{
    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: statusDocument_roundTripsArbitraryText
    //
    // Description:
    //
    //   Verifies that status document round trips arbitrary text.
    //
    // Arguments:
    //
    //   status (String):
    //     The status to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Property
    void statusDocument_roundTripsArbitraryText ( @ForAll String status )
    {
        // Initialize the status document by applying create status document.

        String statusDocument = JsonSupport.createStatusDocument ( status );

        // Verify the status document round trips arbitrary text scenario against its expected outcome.

        assertThat ( JsonSupport.readStatus ( statusDocument ) ).isEqualTo ( status );
    }
}
