//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Represents one actionable authoring-model validation diagnostic.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.model;

import java.util.Objects;

//*********************************************************************************************************************
// Class: ValidationDiagnostic
//
// Description:
//
//   Represents one actionable authoring-model validation diagnostic.
//
//*********************************************************************************************************************

public final class ValidationDiagnostic
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final String entityId;
    private final String field;
    private final String code;
    private final ValidationSeverity severity;
    private final String remedy;

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getEntityId
    //
    // Description:
    //
    //   Returns the entity id.
    //
    // Returns:
    //
    //   The entity id.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String getEntityId ()
    {
        // Return the entity ID to the caller.

        return this.entityId;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getField
    //
    // Description:
    //
    //   Returns the field.
    //
    // Returns:
    //
    //   The field.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String getField ()
    {
        // Return the field to the caller.

        return this.field;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getCode
    //
    // Description:
    //
    //   Returns the code.
    //
    // Returns:
    //
    //   The code.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String getCode ()
    {
        // Return the code to the caller.

        return this.code;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getSeverity
    //
    // Description:
    //
    //   Returns the severity.
    //
    // Returns:
    //
    //   The severity.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ValidationSeverity getSeverity ()
    {
        // Return the severity to the caller.

        return this.severity;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getRemedy
    //
    // Description:
    //
    //   Returns the remedy.
    //
    // Returns:
    //
    //   The remedy.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String getRemedy ()
    {
        // Return the remedy to the caller.

        return this.remedy;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: ValidationDiagnostic
    //
    // Description:
    //
    //   Creates the ValidationDiagnostic instance from the supplied values.
    //
    // Arguments:
    //
    //   entityId (String):
    //     The entity id to use.
    //
    //   field (String):
    //     The field to use.
    //
    //   code (String):
    //     The code to use.
    //
    //   severity (ValidationSeverity):
    //     The severity to use.
    //
    //   remedy (String):
    //     The remedy to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ValidationDiagnostic (
        String entityId,
        String field,
        String code,
        ValidationSeverity severity,
        String remedy
    )
    {
        // Validate the required entity ID, field, code, severity, and remedy before continuing.

        this.entityId = Objects.requireNonNull ( entityId, "entityId" );
        this.field    = Objects.requireNonNull ( field, "field" );
        this.code     = Objects.requireNonNull ( code, "code" );
        this.severity = Objects.requireNonNull ( severity, "severity" );
        this.remedy   = Objects.requireNonNull ( remedy, "remedy" );
    }
}
