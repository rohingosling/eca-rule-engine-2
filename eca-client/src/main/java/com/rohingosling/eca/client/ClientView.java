//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Defines the JavaFX-independent view boundary consumed by the desktop presenter.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.client;

import java.nio.file.Path;
import java.util.Optional;

import com.rohingosling.eca.application.ClientDocumentSession;
import com.rohingosling.eca.application.ClientEvaluationResult;

//---------------------------------------------------------------------------------------------------------------------
// Interface: ClientView
//
// Description:
//
//   Defines the JavaFX-independent view boundary consumed by the desktop presenter.
//
//---------------------------------------------------------------------------------------------------------------------

public interface ClientView
{
    //=================================================================================================================
    // User Defined Data Types
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Enum: MessageSeverity
    //
    // Description:
    //
    //   Enumerates the supported message severity values.
    //
    //-----------------------------------------------------------------------------------------------------------------

    enum MessageSeverity
    {
        DEBUG,
        INFO,
        WARNING,
        ERROR
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: dispatch
    //
    // Description:
    //
    //   Performs the dispatch operation.
    //
    // Arguments:
    //
    //   runnable (Runnable):
    //     The runnable to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    void dispatch ( Runnable runnable );

    //-----------------------------------------------------------------------------------------------------------------
    // Method: render
    //
    // Description:
    //
    //   Performs the render operation.
    //
    // Arguments:
    //
    //   session (ClientDocumentSession):
    //     The session to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    void render ( ClientDocumentSession session );

    //-----------------------------------------------------------------------------------------------------------------
    // Method: reportValidation
    //
    // Description:
    //
    //   Performs the report validation operation.
    //
    // Arguments:
    //
    //   session (ClientDocumentSession):
    //     The session to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    void reportValidation ( ClientDocumentSession session );

    //=================================================================================================================
    // Mutators
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: setBackgroundOperation
    //
    // Description:
    //
    //   Sets the background operation.
    //
    // Arguments:
    //
    //   running (boolean):
    //     The running to use.
    //
    //   description (String):
    //     The description to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    void setBackgroundOperation ( boolean running, String description );

    //-----------------------------------------------------------------------------------------------------------------
    // Method: setStatus
    //
    // Description:
    //
    //   Sets the status.
    //
    // Arguments:
    //
    //   status (String):
    //     The status to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    void setStatus ( String status );

    //-----------------------------------------------------------------------------------------------------------------
    // Method: setConnectionStatus
    //
    // Description:
    //
    //   Sets the connection status.
    //
    // Arguments:
    //
    //   status (String):
    //     The status to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    void setConnectionStatus ( String status );

    //-----------------------------------------------------------------------------------------------------------------
    // Method: setModelRevisionStatus
    //
    // Description:
    //
    //   Sets the model revision status.
    //
    // Arguments:
    //
    //   localRevision (String):
    //     The local revision to use.
    //
    //   serverRevision (String):
    //     The server revision to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    default void setModelRevisionStatus ( String localRevision, String serverRevision )
    {
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: showEvaluation
    //
    // Description:
    //
    //   Performs the show evaluation operation.
    //
    // Arguments:
    //
    //   evaluationResult (ClientEvaluationResult):
    //     The evaluation result to use.
    //
    //   localRevision (String):
    //     The local revision to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    default void showEvaluation ( ClientEvaluationResult evaluationResult, String localRevision )
    {
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: reportMessage
    //
    // Description:
    //
    //   Performs the report message operation.
    //
    // Arguments:
    //
    //   severity (MessageSeverity):
    //     The severity to use.
    //
    //   source (String):
    //     The source to use.
    //
    //   message (String):
    //     The message to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    void reportMessage ( MessageSeverity severity, String source, String message );

    //-----------------------------------------------------------------------------------------------------------------
    // Method: reportError
    //
    // Description:
    //
    //   Performs the report error operation.
    //
    // Arguments:
    //
    //   title (String):
    //     The title to use.
    //
    //   message (String):
    //     The message to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    void reportError ( String title, String message );

    //-----------------------------------------------------------------------------------------------------------------
    // Method: confirmDiscard
    //
    // Description:
    //
    //   Performs the confirm discard operation.
    //
    // Arguments:
    //
    //   actionName (String):
    //     The action name to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    boolean confirmDiscard ( String actionName );

    //-----------------------------------------------------------------------------------------------------------------
    // Method: chooseOpenPath
    //
    // Description:
    //
    //   Performs the choose open path operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    Optional <Path> chooseOpenPath ();

    //-----------------------------------------------------------------------------------------------------------------
    // Method: chooseSavePath
    //
    // Description:
    //
    //   Performs the choose save path operation.
    //
    // Arguments:
    //
    //   currentPath (Optional <Path>):
    //     The current path to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    Optional <Path> chooseSavePath ( Optional <Path> currentPath );

    //-----------------------------------------------------------------------------------------------------------------
    // Method: editSettings
    //
    // Description:
    //
    //   Performs the edit settings operation.
    //
    // Arguments:
    //
    //   currentSettings (ClientConnectionSettings):
    //     The current settings to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    Optional <ClientConnectionSettings> editSettings ( ClientConnectionSettings currentSettings );

    //-----------------------------------------------------------------------------------------------------------------
    // Method: exitApplication
    //
    // Description:
    //
    //   Performs the exit application operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    void exitApplication ();
}
