//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Adapts the strict authoring-model JSON codec to the desktop document boundary.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.json;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.rohingosling.eca.application.ClientDocumentCodec;
import com.rohingosling.eca.application.ClientDocumentSession;

//*********************************************************************************************************************
// Class: AuthoringModelClientDocumentCodec
//
// Description:
//
//   Adapts the strict authoring-model JSON codec to the desktop document boundary.
//
//*********************************************************************************************************************

public final class AuthoringModelClientDocumentCodec implements ClientDocumentCodec
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final AuthoringModelJsonCodec authoringModelJsonCodec;

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: AuthoringModelClientDocumentCodec
    //
    // Description:
    //
    //   Creates the AuthoringModelClientDocumentCodec instance from the supplied values.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public AuthoringModelClientDocumentCodec ()
    {
        // Construct the authoring model JSON codec instance required by the authoring model client document codec
        // operation.

        this.authoringModelJsonCodec = new AuthoringModelJsonCodec ();
    }

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

    @Override
    public ClientDocumentSession.Content read ( byte [] document )
    {
        // Validate the required document before continuing.

        Objects.requireNonNull ( document, "document" );

        // Return a newly constructed client document session content containing the operation result.

        return new ClientDocumentSession.Content ( this.authoringModelJsonCodec.read ( document ) );
    }

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

    @Override
    public byte [] write ( ClientDocumentSession.Content content )
    {
        // Validate the required content before continuing.

        Objects.requireNonNull ( content, "content" );

        // Return the result produced by get bytes.

        return this.authoringModelJsonCodec.writePretty ( content.getAuthoringModel () )
            .getBytes ( StandardCharsets.UTF_8 );
    }

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

    @Override
    public String revision ( ClientDocumentSession.Content content )
    {
        // Validate the required content before continuing.

        Objects.requireNonNull ( content, "content" );

        // Return the result produced by revision.

        return this.authoringModelJsonCodec.revision ( content.getAuthoringModel () );
    }
}
