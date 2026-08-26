//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Defines the serialization boundary used by the desktop document workflow.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.application;

//---------------------------------------------------------------------------------------------------------------------
// Interface: ClientDocumentCodec
//
// Description:
//
//   Defines the serialization boundary used by the desktop document workflow.
//
//---------------------------------------------------------------------------------------------------------------------

public interface ClientDocumentCodec
{
    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: read
    //
    // Description:
    //
    //   Performs the read operation.
    //
    // Arguments:
    //
    //   document (byte []):
    //     The document to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    ClientDocumentSession.Content read ( byte [] document );

    //-----------------------------------------------------------------------------------------------------------------
    // Method: write
    //
    // Description:
    //
    //   Performs the write operation.
    //
    // Arguments:
    //
    //   content (ClientDocumentSession.Content):
    //     The content to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    byte [] write ( ClientDocumentSession.Content content );

    //-----------------------------------------------------------------------------------------------------------------
    // Method: revision
    //
    // Description:
    //
    //   Performs the revision operation.
    //
    // Arguments:
    //
    //   content (ClientDocumentSession.Content):
    //     The content to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    default String revision ( ClientDocumentSession.Content content )
    {
        // Return the revision text to the caller.

        return "";
    }
}
