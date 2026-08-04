//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Defines asynchronous local-file operations used by the desktop presenter.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.client;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import com.rohingosling.eca.application.ClientDocumentSession;

//---------------------------------------------------------------------------------------------------------------------
// Interface: ClientDocumentOperations
//
// Description:
//
//   Defines asynchronous local-file operations used by the desktop presenter.
//
//---------------------------------------------------------------------------------------------------------------------

public interface ClientDocumentOperations extends AutoCloseable
{
    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: open
    //
    // Description:
    //
    //   Performs the open operation.
    //
    // Arguments:
    //
    //   path (Path):
    //     The path to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    CompletableFuture <ClientDocumentSession.Content> open ( Path path );

    //-----------------------------------------------------------------------------------------------------------------
    // Method: save
    //
    // Description:
    //
    //   Performs the save operation.
    //
    // Arguments:
    //
    //   path (Path):
    //     The path to use.
    //
    //   content (ClientDocumentSession.Content):
    //     The content to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    CompletableFuture <Void> save ( Path path, ClientDocumentSession.Content content );

    //-----------------------------------------------------------------------------------------------------------------
    // Method: close
    //
    // Description:
    //
    //   Performs the close operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    void close ();
}
