//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Publishes one immutable authoring, canonical-document, compiled-model, and evaluator snapshot.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.application;

import java.util.Arrays;
import java.util.Objects;

import com.rohingosling.eca.domain.EvaluationResult;
import com.rohingosling.eca.domain.EventOccurrence;
import com.rohingosling.eca.engine.RuleEngine;
import com.rohingosling.eca.model.AuthoringModel;
import com.rohingosling.eca.model.CompiledModel;

//*********************************************************************************************************************
// Class: HostedModelSnapshot
//
// Description:
//
//   Publishes one immutable authoring, canonical-document, compiled-model, and evaluator snapshot.
//
//*********************************************************************************************************************

public final class HostedModelSnapshot
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final AuthoringModel authoringModel;
    private final byte [] canonicalDocument;
    private final CompiledModel compiledModel;
    private final RuleEngine ruleEngine;
    private final HostedModelSummary summary;

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getAuthoringModel
    //
    // Description:
    //
    //   Returns the authoring model.
    //
    // Returns:
    //
    //   The authoring model.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public AuthoringModel getAuthoringModel ()
    {
        // Return the authoring model to the caller.

        return this.authoringModel;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getCompiledModel
    //
    // Description:
    //
    //   Returns the compiled model.
    //
    // Returns:
    //
    //   The compiled model.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public CompiledModel getCompiledModel ()
    {
        // Return the compiled model to the caller.

        return this.compiledModel;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getRevision
    //
    // Description:
    //
    //   Returns the revision.
    //
    // Returns:
    //
    //   The revision.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String getRevision ()
    {
        // Return the result produced by get revision.

        return this.compiledModel.getRevision ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getSummary
    //
    // Description:
    //
    //   Returns the summary.
    //
    // Returns:
    //
    //   The summary.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public HostedModelSummary getSummary ()
    {
        // Return the summary to the caller.

        return this.summary;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: HostedModelSnapshot
    //
    // Description:
    //
    //   Creates the HostedModelSnapshot instance from the supplied values.
    //
    // Arguments:
    //
    //   authoringModel (AuthoringModel):
    //     The authoring model to use.
    //
    //   canonicalDocument (byte []):
    //     The canonical document to use.
    //
    //   compiledModel (CompiledModel):
    //     The compiled model to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public HostedModelSnapshot (
        AuthoringModel authoringModel,
        byte [] canonicalDocument,
        CompiledModel compiledModel
    )
    {
        // Perform the require non null, copy of, get rule base, and get revision calls required by the hosted model
        // snapshot operation.

        this.authoringModel     = Objects.requireNonNull ( authoringModel, "authoringModel" );
        this.canonicalDocument  = Arrays.copyOf (
            Objects.requireNonNull ( canonicalDocument, "canonicalDocument" ),
            canonicalDocument.length
        );
        this.compiledModel      = Objects.requireNonNull ( compiledModel, "compiledModel" );
        this.ruleEngine         = new RuleEngine ( compiledModel.getRuleBase () );
        this.summary            = new HostedModelSummary ( authoringModel, compiledModel.getRevision () );

        // Reject the operation when compiled model authoring model differs from authoring model.

        if ( compiledModel.getAuthoringModel () != authoringModel )
        {
            throw new IllegalArgumentException (
                "The compiled model must reference the snapshot authoring model."
            );
        }
    }

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getCanonicalDocument
    //
    // Description:
    //
    //   Returns the canonical document.
    //
    // Returns:
    //
    //   The canonical document.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public byte [] getCanonicalDocument ()
    {
        // Return an immutable copy of canonical document.

        return Arrays.copyOf ( this.canonicalDocument, this.canonicalDocument.length );
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
        // Return the result produced by evaluate.

        return this.ruleEngine.evaluate ( occurrence );
    }
}
