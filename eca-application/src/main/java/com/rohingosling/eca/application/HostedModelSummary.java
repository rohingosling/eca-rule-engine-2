//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Reports the active model revision and all seven authoring-entity counts without exposing model content.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.application;

import java.util.Objects;

import com.rohingosling.eca.model.AuthoringModel;

//*********************************************************************************************************************
// Class: HostedModelSummary
//
// Description:
//
//   Reports the active model revision and all seven authoring-entity counts without exposing model content.
//
//*********************************************************************************************************************

public final class HostedModelSummary
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final String modelId;
    private final String revision;
    private final int parameterCount;
    private final int payloadCount;
    private final int eventCount;
    private final int conditionCount;
    private final int conditionSetCount;
    private final int actionCount;
    private final int ruleCount;

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getModelId
    //
    // Description:
    //
    //   Returns the model id.
    //
    // Returns:
    //
    //   The model id.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String getModelId ()
    {
        // Return the model ID to the caller.

        return this.modelId;
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
        // Return the revision to the caller.

        return this.revision;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getParameterCount
    //
    // Description:
    //
    //   Returns the parameter count.
    //
    // Returns:
    //
    //   The parameter count.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public int getParameterCount ()
    {
        // Return the parameter count to the caller.

        return this.parameterCount;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getPayloadCount
    //
    // Description:
    //
    //   Returns the payload count.
    //
    // Returns:
    //
    //   The payload count.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public int getPayloadCount ()
    {
        // Return the payload count to the caller.

        return this.payloadCount;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getEventCount
    //
    // Description:
    //
    //   Returns the event count.
    //
    // Returns:
    //
    //   The event count.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public int getEventCount ()
    {
        // Return the event count to the caller.

        return this.eventCount;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getConditionCount
    //
    // Description:
    //
    //   Returns the condition count.
    //
    // Returns:
    //
    //   The condition count.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public int getConditionCount ()
    {
        // Return the condition count to the caller.

        return this.conditionCount;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getConditionSetCount
    //
    // Description:
    //
    //   Returns the condition set count.
    //
    // Returns:
    //
    //   The condition set count.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public int getConditionSetCount ()
    {
        // Return the condition set count to the caller.

        return this.conditionSetCount;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getActionCount
    //
    // Description:
    //
    //   Returns the action count.
    //
    // Returns:
    //
    //   The action count.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public int getActionCount ()
    {
        // Return the action count to the caller.

        return this.actionCount;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getRuleCount
    //
    // Description:
    //
    //   Returns the rule count.
    //
    // Returns:
    //
    //   The rule count.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public int getRuleCount ()
    {
        // Return the rule count to the caller.

        return this.ruleCount;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: HostedModelSummary
    //
    // Description:
    //
    //   Creates the HostedModelSummary instance from the supplied values.
    //
    // Arguments:
    //
    //   authoringModel (AuthoringModel):
    //     The authoring model to use.
    //
    //   revision (String):
    //     The revision to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public HostedModelSummary ( AuthoringModel authoringModel, String revision )
    {
        // Validate the required authoring model before continuing.

        Objects.requireNonNull ( authoringModel, "authoringModel" );

        // Perform the get model ID, require non null, size, get parameters, get payloads, get events, get conditions,
        // get condition sets, get actions, and get rules calls required by the hosted model summary operation.

        this.modelId           = authoringModel.getModelId ();
        this.revision          = Objects.requireNonNull ( revision, "revision" );
        this.parameterCount    = authoringModel.getParameters ().size ();
        this.payloadCount      = authoringModel.getPayloads ().size ();
        this.eventCount        = authoringModel.getEvents ().size ();
        this.conditionCount    = authoringModel.getConditions ().size ();
        this.conditionSetCount = authoringModel.getConditionSets ().size ();
        this.actionCount       = authoringModel.getActions ().size ();
        this.ruleCount         = authoringModel.getRules ().size ();
    }

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getTotalEntityCount
    //
    // Description:
    //
    //   Returns the total entity count.
    //
    // Returns:
    //
    //   The total entity count.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public int getTotalEntityCount ()
    {
        // Return the composed get total entity count value.

        return this.parameterCount
            + this.payloadCount
            + this.eventCount
            + this.conditionCount
            + this.conditionSetCount
            + this.actionCount
            + this.ruleCount;
    }
}
