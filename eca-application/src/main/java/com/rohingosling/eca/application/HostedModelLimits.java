//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Defines configurable hosted-model document limits enforced before parsing or persistence.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.application;

//*********************************************************************************************************************
// Class: HostedModelLimits
//
// Description:
//
//   Defines configurable hosted-model document limits enforced before parsing or persistence.
//
//*********************************************************************************************************************

public final class HostedModelLimits
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    private static final int DEFAULT_MAXIMUM_MODEL_DOCUMENT_BYTES = 16 * 1024 * 1024;

    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final int maximumModelDocumentBytes;

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getMaximumModelDocumentBytes
    //
    // Description:
    //
    //   Returns the maximum model document bytes.
    //
    // Returns:
    //
    //   The maximum model document bytes.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public int getMaximumModelDocumentBytes ()
    {
        // Return the maximum model document bytes to the caller.

        return this.maximumModelDocumentBytes;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: HostedModelLimits
    //
    // Description:
    //
    //   Creates the HostedModelLimits instance from the supplied values.
    //
    // Arguments:
    //
    //   maximumModelDocumentBytes (int):
    //     The maximum model document bytes to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public HostedModelLimits ( int maximumModelDocumentBytes )
    {
        // Reject the operation when maximum model document bytes is at most 0.

        if ( maximumModelDocumentBytes <= 0 )
        {
            throw new IllegalArgumentException ( "The maximum model document size must be positive." );
        }

        this.maximumModelDocumentBytes = maximumModelDocumentBytes;
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: defaults
    //
    // Description:
    //
    //   Creates a HostedModelLimits instance with the default configuration.
    //
    // Returns:
    //
    //   The resulting HostedModelLimits instance.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static HostedModelLimits defaults ()
    {
        // Return a newly constructed hosted model limits containing the operation result.

        return new HostedModelLimits ( DEFAULT_MAXIMUM_MODEL_DOCUMENT_BYTES );
    }
}
