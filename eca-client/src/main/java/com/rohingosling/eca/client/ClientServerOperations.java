//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Defines asynchronous server operations used by the desktop presenter.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.client;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.rohingosling.eca.application.ClientDocumentSession;
import com.rohingosling.eca.application.ClientEvaluationResult;
import com.rohingosling.eca.application.ClientSimulator.SimulationRequest;

//---------------------------------------------------------------------------------------------------------------------
// Interface: ClientServerOperations
//
// Description:
//
//   Defines asynchronous server operations used by the desktop presenter.
//
//---------------------------------------------------------------------------------------------------------------------

public interface ClientServerOperations
{
    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: testConnection
    //
    // Description:
    //
    //   Performs the test connection operation.
    //
    // Arguments:
    //
    //   settings (ClientConnectionSettings):
    //     The settings to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    CompletableFuture <String> testConnection ( ClientConnectionSettings settings );

    //-----------------------------------------------------------------------------------------------------------------
    // Method: pullModel
    //
    // Description:
    //
    //   Performs the pull model operation.
    //
    // Arguments:
    //
    //   settings (ClientConnectionSettings):
    //     The settings to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    CompletableFuture <ClientDocumentSession.Content> pullModel ( ClientConnectionSettings settings );

    //-----------------------------------------------------------------------------------------------------------------
    // Method: pushModel
    //
    // Description:
    //
    //   Performs the push model operation.
    //
    // Arguments:
    //
    //   settings (ClientConnectionSettings):
    //     The settings to use.
    //
    //   content (ClientDocumentSession.Content):
    //     The content to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    CompletableFuture <String> pushModel (
        ClientConnectionSettings settings,
        ClientDocumentSession.Content content
    );

    //-----------------------------------------------------------------------------------------------------------------
    // Method: pullModelWithRevision
    //
    // Description:
    //
    //   Performs the pull model with revision operation.
    //
    // Arguments:
    //
    //   settings (ClientConnectionSettings):
    //     The settings to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    default CompletableFuture <ModelPull> pullModelWithRevision (
        ClientConnectionSettings settings
    )
    {
        // Return the result produced by then apply.

        return this.pullModel ( settings ).thenApply (
            content -> new ModelPull ( content, this.modelRevision ( content ) )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: pushModelWithRevision
    //
    // Description:
    //
    //   Performs the push model with revision operation.
    //
    // Arguments:
    //
    //   settings (ClientConnectionSettings):
    //     The settings to use.
    //
    //   content (ClientDocumentSession.Content):
    //     The content to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    default CompletableFuture <ModelPush> pushModelWithRevision (
        ClientConnectionSettings settings,
        ClientDocumentSession.Content content
    )
    {
        // Return the result produced by then apply.

        return this.pushModel ( settings, content ).thenApply (
            message -> new ModelPush ( message, this.modelRevision ( content ) )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: evaluateOccurrence
    //
    // Description:
    //
    //   Performs the evaluate occurrence operation.
    //
    // Arguments:
    //
    //   settings (ClientConnectionSettings):
    //     The settings to use.
    //
    //   simulationRequest (SimulationRequest):
    //     The simulation request to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    default CompletableFuture <ClientEvaluationResult> evaluateOccurrence (
        ClientConnectionSettings settings,
        SimulationRequest simulationRequest
    )
    {
        // Return the result produced by failed future.

        return CompletableFuture.failedFuture (
            new UnsupportedOperationException ( "Server evaluation is not implemented." )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: modelRevision
    //
    // Description:
    //
    //   Performs the model revision operation.
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

    default String modelRevision ( ClientDocumentSession.Content content )
    {
        // Return the model revision text to the caller.

        return "";
    }

    //*****************************************************************************************************************
    // Class: ModelPull
    //
    // Description:
    //
    //   Provides the model pull behavior.
    //
    //*****************************************************************************************************************

    final class ModelPull
    {
        private final ClientDocumentSession.Content content;

        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String revision;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getContent
        //
        // Description:
        //
        //   Returns the content.
        //
        // Returns:
        //
        //   The content.
        //
        //-------------------------------------------------------------------------------------------------------------

        public ClientDocumentSession.Content getContent ()
        {
            // Return the content to the caller.

            return this.content;
        }

        //-------------------------------------------------------------------------------------------------------------
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
        //-------------------------------------------------------------------------------------------------------------

        public String getRevision ()
        {
            // Return the revision to the caller.

            return this.revision;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: ModelPull
        //
        // Description:
        //
        //   Creates the ModelPull instance from the supplied values.
        //
        // Arguments:
        //
        //   content (ClientDocumentSession.Content):
        //     The content to use.
        //
        //   revision (String):
        //     The revision to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        public ModelPull ( ClientDocumentSession.Content content, String revision )
        {
            // Validate the required content and revision before continuing.

            this.content  = Objects.requireNonNull ( content, "content" );
            this.revision = Objects.requireNonNull ( revision, "revision" );
        }
    }

    //*****************************************************************************************************************
    // Class: ModelPush
    //
    // Description:
    //
    //   Provides the model push behavior.
    //
    //*****************************************************************************************************************

    final class ModelPush
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String message;
        private final String revision;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getMessage
        //
        // Description:
        //
        //   Returns the message.
        //
        // Returns:
        //
        //   The message.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getMessage ()
        {
            // Return the message to the caller.

            return this.message;
        }

        //-------------------------------------------------------------------------------------------------------------
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
        //-------------------------------------------------------------------------------------------------------------

        public String getRevision ()
        {
            // Return the revision to the caller.

            return this.revision;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: ModelPush
        //
        // Description:
        //
        //   Creates the ModelPush instance from the supplied values.
        //
        // Arguments:
        //
        //   message (String):
        //     The message to use.
        //
        //   revision (String):
        //     The revision to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        public ModelPush ( String message, String revision )
        {
            // Validate the required message and revision before continuing.

            this.message  = Objects.requireNonNull ( message, "message" );
            this.revision = Objects.requireNonNull ( revision, "revision" );
        }
    }
}
