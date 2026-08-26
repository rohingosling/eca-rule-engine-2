//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Adapts the strict JSON codec and model compiler to the hosted-model factory application port.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.json;

import java.util.Objects;

import com.rohingosling.eca.application.HostedModelFactory;
import com.rohingosling.eca.application.HostedModelSnapshot;
import com.rohingosling.eca.model.AuthoringModel;
import com.rohingosling.eca.model.AuthoringModelCompiler;
import com.rohingosling.eca.model.CompiledModel;
import com.rohingosling.eca.model.ModelLimits;
import com.rohingosling.eca.model.ModelValidator;

//*********************************************************************************************************************
// Class: JsonHostedModelFactory
//
// Description:
//
//   Adapts the strict JSON codec and model compiler to the hosted-model factory application port.
//
//*********************************************************************************************************************

public final class JsonHostedModelFactory implements HostedModelFactory
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final AuthoringModelJsonCodec authoringModelJsonCodec;
    private final AuthoringModelCompiler authoringModelCompiler;

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/2: JsonHostedModelFactory
    //
    // Description:
    //
    //   Creates the JsonHostedModelFactory instance from the supplied values.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public JsonHostedModelFactory ()
    {
        // Apply this and defaults to the model limits for the JSON hosted model factory operation.

        this ( ModelLimits.defaults () );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 2/2: JsonHostedModelFactory
    //
    // Description:
    //
    //   Creates the JsonHostedModelFactory instance from the supplied values.
    //
    // Arguments:
    //
    //   modelLimits (ModelLimits):
    //     The model limits to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public JsonHostedModelFactory ( ModelLimits modelLimits )
    {
        // Validate the required model limits before continuing.

        Objects.requireNonNull ( modelLimits, "modelLimits" );

        // Construct the authoring model JSON codec instance required by the JSON hosted model factory operation.

        this.authoringModelJsonCodec = new AuthoringModelJsonCodec ();
        this.authoringModelCompiler  = new AuthoringModelCompiler ( new ModelValidator ( modelLimits ) );
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: create
    //
    // Description:
    //
    //   Performs the create operation.
    //
    // Arguments:
    //
    //   modelDocument (byte []):
    //     The model document to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public HostedModelSnapshot create ( byte [] modelDocument )
    {
        // Prepare the authoring model, canonical document, revision, and compiled model values needed by the create
        // operation.

        AuthoringModel authoringModel = this.authoringModelJsonCodec.read ( modelDocument );
        byte [] canonicalDocument     = this.authoringModelJsonCodec.writeCanonical ( authoringModel );
        String revision               = this.authoringModelJsonCodec.revision ( authoringModel );
        CompiledModel compiledModel   = this.authoringModelCompiler.compile ( authoringModel, revision );

        // Return a newly constructed hosted model snapshot containing the operation result.

        return new HostedModelSnapshot ( authoringModel, canonicalDocument, compiledModel );
    }
}
