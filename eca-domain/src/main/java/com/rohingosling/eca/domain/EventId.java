//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Identifies one event type in the immutable mathematical core.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.domain;

//*********************************************************************************************************************
// Class: EventId
//
// Description:
//
//   Identifies one event type in the immutable mathematical core.
//
//*********************************************************************************************************************

public final class EventId extends StableIdentifier
{
    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: EventId
    //
    // Description:
    //
    //   Creates the EventId instance from the supplied values.
    //
    // Arguments:
    //
    //   value (String):
    //     The value to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public EventId ( String value )
    {
        // Initialize the inherited state through the base-class constructor.

        super ( value );
    }
}
