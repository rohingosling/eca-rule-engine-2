//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Reports that an authoring model cannot be compiled because validation failed.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//*********************************************************************************************************************
// Class: ModelValidationException
//
// Description:
//
//   Reports that an authoring model cannot be compiled because validation failed.
//
//*********************************************************************************************************************

public final class ModelValidationException extends IllegalArgumentException
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final List <ValidationDiagnostic> diagnostics;

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getDiagnostics
    //
    // Description:
    //
    //   Returns the diagnostics.
    //
    // Returns:
    //
    //   The diagnostics.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public List <ValidationDiagnostic> getDiagnostics ()
    {
        // Return the diagnostics to the caller.

        return this.diagnostics;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: ModelValidationException
    //
    // Description:
    //
    //   Creates the ModelValidationException instance from the supplied values.
    //
    // Arguments:
    //
    //   diagnostics (List <ValidationDiagnostic>):
    //     The diagnostics to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ModelValidationException ( List <ValidationDiagnostic> diagnostics )
    {
        // Initialize the inherited state through the base-class constructor.

        super ( "The authoring model contains validation errors." );

        // Update the diagnostics from the unmodifiable list result.

        this.diagnostics = Collections.unmodifiableList (
            new ArrayList <ValidationDiagnostic> ( diagnostics )
        );
    }
}
