//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Translates desktop user intent into document and server operations without owning JavaFX widgets.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.client;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

import com.rohingosling.eca.application.ClientDocumentSession;
import com.rohingosling.eca.application.ClientModelEditor;
import com.rohingosling.eca.application.ClientModelEditor.DeleteResult;
import com.rohingosling.eca.application.ClientModelEditor.EditResult;
import com.rohingosling.eca.application.ClientModelEditor.EntityDraft;
import com.rohingosling.eca.application.ClientModelEditor.FieldError;
import com.rohingosling.eca.application.ClientSimulator;
import com.rohingosling.eca.application.ClientSimulator.EventDescriptor;
import com.rohingosling.eca.application.ClientSimulator.PayloadValueDraft;
import com.rohingosling.eca.application.ClientSimulator.SimulationRequest;
import com.rohingosling.eca.client.ClientView.DirtyDocumentDecision;
import com.rohingosling.eca.client.ClientView.MessageSeverity;

//*********************************************************************************************************************
// Class: ClientPresenter
//
// Description:
//
//   Translates desktop user intent into document and server operations without owning JavaFX widgets.
//
//*********************************************************************************************************************

public final class ClientPresenter
{
    private static final ResourceBundle MESSAGES = ResourceBundle.getBundle (
        "com.rohingosling.eca.client.messages",
        Locale.getDefault ()
    );

    //=================================================================================================================
    // User Defined Data Types
    //=================================================================================================================

    private enum BackgroundOperationType
    {
        LOCAL,
        REMOTE,
        CONNECTION_TEST
    }

    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final ClientView               view;
    private final ClientDocumentOperations documentOperations;
    private final ClientServerOperations   serverOperations;
    private final ClientModelEditor        modelEditor;
    private final ClientSimulator          simulator;

    private ClientDocumentSession    session;
    private ClientConnectionSettings connectionSettings;
    private CompletableFuture <?>    backgroundOperation;
    private String                   connectionStatus;
    private String                   serverRevision;

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getSession
    //
    // Description:
    //
    //   Returns the session.
    //
    // Returns:
    //
    //   The session.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ClientDocumentSession getSession ()
    {
        // Return the session to the caller.

        return this.session;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getConnectionSettings
    //
    // Description:
    //
    //   Returns the connection settings.
    //
    // Returns:
    //
    //   The connection settings.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ClientConnectionSettings getConnectionSettings ()
    {
        // Return the connection settings to the caller.

        return this.connectionSettings;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: isBackgroundOperationRunning
    //
    // Description:
    //
    //   Indicates whether background operation running.
    //
    // Returns:
    //
    //   `true` when the condition is satisfied; otherwise `false`.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public boolean isBackgroundOperationRunning ()
    {
        // Return whether background operation is available.

        return this.backgroundOperation != null;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getSimulatorEvents
    //
    // Description:
    //
    //   Returns the simulator events.
    //
    // Returns:
    //
    //   The simulator events.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public List <EventDescriptor> getSimulatorEvents ()
    {
        // Return the result produced by get events.

        return this.simulator.getEvents ( this.session.getContent () );
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: ClientPresenter
    //
    // Description:
    //
    //   Creates the ClientPresenter instance from the supplied values.
    //
    // Arguments:
    //
    //   view (ClientView):
    //     The view to use.
    //
    //   documentOperations (ClientDocumentOperations):
    //     The document operations to use.
    //
    //   serverOperations (ClientServerOperations):
    //     The server operations to use.
    //
    //   session (ClientDocumentSession):
    //     The session to use.
    //
    //   connectionSettings (ClientConnectionSettings):
    //     The connection settings to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ClientPresenter (
        ClientView view,
        ClientDocumentOperations documentOperations,
        ClientServerOperations serverOperations,
        ClientDocumentSession session,
        ClientConnectionSettings connectionSettings
    )
    {
        // Validate the required view, document operations, server operations, session, and connection settings before
        // continuing.

        this.view                 = Objects.requireNonNull ( view, "view" );
        this.documentOperations   = Objects.requireNonNull ( documentOperations, "documentOperations" );
        this.serverOperations     = Objects.requireNonNull ( serverOperations, "serverOperations" );
        this.modelEditor          = new ClientModelEditor ();
        this.simulator            = new ClientSimulator ();
        this.session              = Objects.requireNonNull ( session, "session" );
        this.connectionSettings   = Objects.requireNonNull ( connectionSettings, "connectionSettings" );
        this.backgroundOperation  = null;
        this.connectionStatus     = "Not tested";
        this.serverRevision       = "";
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: start
    //
    // Description:
    //
    //   Performs the start operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void start ()
    {
        // Apply render, set status, set connection status, and update revision status to the view for the start
        // operation.

        this.view.render ( this.session );
        this.view.setStatus ( "Ready" );
        setConnectionStatus ( "Not tested" );
        updateRevisionStatus ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requestNew
    //
    // Description:
    //
    //   Performs the request new operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void requestNew ()
    {
        // Stop this path and return its result when background operation is available.

        if ( this.backgroundOperation != null )
        {
            return;
        }

        // Continue the request only after the user has resolved any unsaved changes.

        continueAfterDirtyDecision (
            "create a new model",
            () ->
            {
                this.session = new ClientDocumentSession ();
                renderModelActivity ();
                this.view.setStatus ( "Created a new model" );
            }
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requestOpen
    //
    // Description:
    //
    //   Performs the request open operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void requestOpen ()
    {
        // Stop this path and return its result when background operation is available.

        if ( this.backgroundOperation != null )
        {
            return;
        }

        // Continue the request only after the user has resolved any unsaved changes.

        continueAfterDirtyDecision (
            "open another model",
            () -> this.view.chooseOpenPath ().ifPresent (
                path -> runBackgroundOperation (
                    this.documentOperations.open ( path ),
                    "Opening " + path.getFileName (),
                    content ->
                    {
                        this.session = ClientDocumentSession.loaded ( content, path );
                        renderModelActivity ();
                        this.view.setStatus ( "Opened " + path.getFileName () );
                    },
                    "Unable to Open Model",
                    BackgroundOperationType.LOCAL
                )
            )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requestSave
    //
    // Description:
    //
    //   Performs the request save operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void requestSave ()
    {
        // Stop this path and return its result when background operation is available.

        if ( this.backgroundOperation != null || this.session.getContent ().hasValidationErrors () )
        {
            return;
        }

        // Initialize the local path by applying get local path.

        Optional <Path> localPath = this.session.getLocalPath ();

        // Handle the branch where local path contains no values.

        if ( localPath.isEmpty () )
        {
            // Complete the request save step by calling request save as.

            requestSaveAs ();
            return;
        }

        // Apply save to and or else throw to the local path for the request save operation.

        saveTo ( localPath.orElseThrow () );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requestSaveAs
    //
    // Description:
    //
    //   Performs the request save as operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void requestSaveAs ()
    {
        // Stop this path and return its result when background operation is available.

        if ( this.backgroundOperation != null || this.session.getContent ().hasValidationErrors () )
        {
            return;
        }

        // Perform the if present, choose save path, and get local path calls required by the request save as
        // operation.

        this.view.chooseSavePath ( this.session.getLocalPath () ).ifPresent ( this::saveTo );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requestClose
    //
    // Description:
    //
    //   Performs the request close operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void requestClose ()
    {
        // Stop this path and return its result when background operation is available.

        if ( this.backgroundOperation != null )
        {
            return;
        }

        // Continue the request only after the user has resolved any unsaved changes.

        continueAfterDirtyDecision (
            "close the model",
            () ->
            {
                this.session = new ClientDocumentSession ();
                renderModelActivity ();
                this.view.setStatus ( "Closed the model" );
            }
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requestExit
    //
    // Description:
    //
    //   Performs the request exit operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void requestExit ()
    {
        // Continue the request only after the user has resolved any unsaved changes.

        continueAfterDirtyDecision (
            "exit",
            () ->
            {
                cancelBackgroundOperation ();
                this.documentOperations.close ();
                this.view.exitApplication ();
            }
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requestPull
    //
    // Description:
    //
    //   Performs the request pull operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void requestPull ()
    {
        // Stop this path and return its result when background operation is available.

        if ( this.backgroundOperation != null )
        {
            return;
        }

        // Continue the request only after the user has resolved any unsaved changes.

        continueAfterDirtyDecision (
            "pull the server model",
            () -> runBackgroundOperation (
                this.serverOperations.pullModelWithRevision ( this.connectionSettings ),
                "Pulling model from server",
                modelPull ->
                {
                    this.session        = ClientDocumentSession.pulled ( modelPull.getContent () );
                    this.serverRevision = modelPull.getRevision ();
                    renderModelActivity ();
                    this.view.setStatus ( "Pulled the server model; use Save As to create a local file" );
                },
                "Unable to Pull Model",
                BackgroundOperationType.REMOTE
            )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requestPush
    //
    // Description:
    //
    //   Performs the request push operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void requestPush ()
    {
        // Stop this path and return its result when background operation is available.

        if ( this.backgroundOperation != null )
        {
            return;
        }

        // Handle the branch where session is not push.

        if ( !this.session.canPush () )
        {
            // Complete the request push step by calling report error.

            this.view.reportError (
                "Unable to Push Model",
                "Repair the validation errors before pushing this model."
            );

            return;
        }

        // Perform the run background operation, push model with revision, get content, get revision, set connection
        // status, get message, update revision status, and set status calls required by the request push operation.

        runBackgroundOperation (
            this.serverOperations.pushModelWithRevision (
                this.connectionSettings,
                this.session.getContent ()
            ),
            "Pushing model to server",
            modelPush ->
            {
                // Update the server revision from the get revision result.

                this.serverRevision = modelPush.getRevision ();

                // Perform the set connection status, get message, update revision status, and set status calls
                // required by the request push operation.

                updateRevisionStatus ();
                this.view.setStatus ( modelPush.getMessage () );
            },
            "Unable to Push Model",
            BackgroundOperationType.REMOTE
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requestTestConnection
    //
    // Description:
    //
    //   Performs the request test connection operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void requestTestConnection ()
    {
        // Stop this path and return its result when background operation is available.

        if ( this.backgroundOperation != null )
        {
            return;
        }

        // Perform the run background operation, test connection, set connection status, and set status calls required
        // by the request test connection operation.

        runBackgroundOperation (
            this.serverOperations.testConnection ( this.connectionSettings ),
            "Testing server connection",
            message ->
            {
                // Configure the view with the required connection status and status values.

                this.view.setStatus ( message );
            },
            "Connection Test Failed",
            BackgroundOperationType.CONNECTION_TEST
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requestSettings
    //
    // Description:
    //
    //   Performs the request settings operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void requestSettings ()
    {
        // Stop this path and return its result when background operation is available.

        if ( this.backgroundOperation != null )
        {
            return;
        }

        // Apply settings returned by views that do not commit them from an in-dialog Apply action. The JavaFX view
        // applies immediately, so the equality guard also prevents a duplicate update when its dialog later closes.

        this.view.editSettings ( this.connectionSettings ).ifPresent (
            settings ->
            {
                if ( !this.connectionSettings.equals ( settings ) )
                {
                    applyConnectionSettings ( settings );
                }
            }
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: applyConnectionSettings
    //
    // Description:
    //
    //   Applies connection settings immediately, including while the Settings dialog remains open.
    //
    // Arguments:
    //
    //   settings (ClientConnectionSettings):
    //     The settings to apply.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void applyConnectionSettings ( ClientConnectionSettings settings )
    {
        Objects.requireNonNull ( settings, "settings" );

        boolean connectionSettingsChanged = !this.connectionSettings.equals ( settings );

        this.connectionSettings = settings;

        if ( connectionSettingsChanged )
        {
            this.serverRevision = "";
            setConnectionStatus ( "Not tested" );
            updateRevisionStatus ();
        }

        this.view.setStatus ( "Settings updated" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requestEvaluate
    //
    // Description:
    //
    //   Performs the request evaluate operation.
    //
    // Arguments:
    //
    //   eventIdentifier (String):
    //     The event identifier to use.
    //
    //   payloadDrafts (Map <String, PayloadValueDraft>):
    //     The payload drafts to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void requestEvaluate (
        String eventIdentifier,
        Map <String, PayloadValueDraft> payloadDrafts
    )
    {
        // Stop this path and return its result when background operation is available.

        if ( this.backgroundOperation != null )
        {
            return;
        }

        // Handle the branch where session is not push.

        if ( !this.session.canPush () )
        {
            // Complete the request evaluate step by calling report error.

            this.view.reportError (
                "Unable to Evaluate Occurrence",
                "Repair the validation errors before running a simulation."
            );

            return;
        }

        SimulationRequest simulationRequest;

        try
        {
            // Update the simulation request from the get content result.

            simulationRequest = this.simulator.createOccurrence (
                this.session.getContent (),
                eventIdentifier,
                payloadDrafts
            );
        }

        // Handle illegal argument failures captured as exception.

        catch ( IllegalArgumentException exception )
        {
            // Apply report error and safe message to the view for the request evaluate operation.

            this.view.reportError ( "Invalid Simulation Payload", safeMessage ( exception ) );

            return;
        }

        // Perform the run background operation, evaluate occurrence, get model revision, local revision, show
        // evaluation, set connection status, equals, update revision status, set status, is action, or else throw, and
        // get action identifier calls required by the request evaluate operation.

        runBackgroundOperation (
            this.serverOperations.evaluateOccurrence (
                this.connectionSettings,
                simulationRequest
            ),
            "Evaluating " + eventIdentifier,
            evaluationResult ->
            {
                // Update the server revision from the get model revision result.

                this.serverRevision = evaluationResult.getModelRevision ();

                // Initialize the local revision by applying local revision.

                String localRevision = localRevision ();

                // Perform the show evaluation, set connection status, equals, get model revision, update revision
                // status, set status, is action, or else throw, and get action identifier calls required by the
                // request evaluate operation.

                this.view.showEvaluation ( evaluationResult, localRevision );
                setConnectionStatus (
                    evaluationResult.getModelRevision ().equals ( localRevision )
                        ? "Connected — model revisions match"
                        : "Connected — server model differs from the local document"
                );
                updateRevisionStatus ();
                this.view.setStatus (
                    evaluationResult.isAction ()
                        ? "Evaluation selected "
                            + evaluationResult.getActionIdentifier ().orElseThrow ()
                        : "Evaluation returned no action"
                );
            },
            "Unable to Evaluate Occurrence",
            BackgroundOperationType.REMOTE
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requestValidate
    //
    // Description:
    //
    //   Performs the request validate operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void requestValidate ()
    {
        // Complete the request validate step by calling render model activity.

        renderModelActivity ();

        // Initialize the diagnostic count by applying size, get validation messages, and get content.

        int diagnosticCount = this.session.getContent ().getValidationMessages ().size ();

        // Set the status on the view.

        this.view.setStatus (
            diagnosticCount == 0
                ? "The model is valid"
                : "Validation found " + diagnosticCount + " diagnostic(s)"
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requestApplyModelDetails
    //
    // Description:
    //
    //   Performs the request apply model details operation.
    //
    // Arguments:
    //
    //   modelIdentifier (String):
    //     The model identifier to use.
    //
    //   name (String):
    //     The name to use.
    //
    //   description (String):
    //     The description to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void requestApplyModelDetails (
        String modelIdentifier,
        String name,
        String description
    )
    {
        // Stop this path and return its result when background operation is available.

        if ( this.backgroundOperation != null )
        {
            return;
        }

        // Initialize the result by applying apply model details and get content.

        EditResult result = this.modelEditor.applyModelDetails (
            this.session.getContent (),
            modelIdentifier,
            name,
            description
        );

        // Handle the branch where result is not successful.

        if ( !result.isSuccessful () )
        {
            // Perform the report error, collect, map, stream, get errors, get field, get message, and joining calls
            // required by the request apply model details operation.

            this.view.reportError (
                "Unable to Update Model Details",
                result.getErrors ().stream ()
                    .map ( error -> error.getField () + ": " + error.getMessage () )
                    .collect ( java.util.stream.Collectors.joining ( "\n" ) )
            );

            return;
        }

        // Perform the apply edit, or else throw, get content, render model activity, and set status calls required by
        // the request apply model details operation.

        this.session.applyEdit ( result.getContent ().orElseThrow () );
        renderModelActivity ();
        this.view.setStatus ( "Updated model details" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requestUndo
    //
    // Description:
    //
    //   Performs the request undo operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void requestUndo ()
    {
        // Stop this path and return its result when background operation is available.

        if ( this.backgroundOperation != null )
        {
            return;
        }

        // Perform the undo, render model activity, and set status calls required by the request undo operation.

        this.session.undo ();
        renderModelActivity ();
        this.view.setStatus ( "Undo" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requestRedo
    //
    // Description:
    //
    //   Performs the request redo operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void requestRedo ()
    {
        // Stop this path and return its result when background operation is available.

        if ( this.backgroundOperation != null )
        {
            return;
        }

        // Perform the redo, render model activity, and set status calls required by the request redo operation.

        this.session.redo ();
        renderModelActivity ();
        this.view.setStatus ( "Redo" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: selectCategory
    //
    // Description:
    //
    //   Performs the select category operation.
    //
    // Arguments:
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void selectCategory ( String categoryIdentifier )
    {
        // Perform the select category and render calls required by the select category operation.

        this.session.selectCategory ( categoryIdentifier );
        this.view.render ( this.session );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: selectModel
    //
    // Description:
    //
    //   Performs the select model operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void selectModel ()
    {
        // Perform the select model and render calls required by the select model operation.

        this.session.selectModel ();
        this.view.render ( this.session );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: selectEntity
    //
    // Description:
    //
    //   Performs the select entity operation.
    //
    // Arguments:
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   entityIdentifier (String):
    //     The entity identifier to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void selectEntity ( String categoryIdentifier, String entityIdentifier )
    {
        // Perform the select entity and render calls required by the select entity operation.

        this.session.selectEntity ( categoryIdentifier, entityIdentifier );
        this.view.render ( this.session );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: selectSimulator
    //
    // Description:
    //
    //   Performs the select simulator operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void selectSimulator ()
    {
        // Perform the select simulator and render calls required by the select simulator operation.

        this.session.selectSimulator ();
        this.view.render ( this.session );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requestApplyEntity
    //
    // Description:
    //
    //   Performs the request apply entity operation.
    //
    // Arguments:
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   originalEntityIdentifier (String):
    //     The original entity identifier to use.
    //
    //   draft (EntityDraft):
    //     The draft to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public EditResult requestApplyEntity (
        String categoryIdentifier,
        String originalEntityIdentifier,
        EntityDraft draft
    )
    {
        // Handle the branch where background operation is available.

        if ( this.backgroundOperation != null )
        {
            // Initialize the rejected result by applying rejected.

            EditResult rejectedResult = EditResult.rejected (
                "Wait for the background operation to finish or cancel it."
            );

            // Apply report rejected entity draft and get errors to the rejected result for the request apply entity
            // operation.

            reportRejectedEntityDraft (
                categoryIdentifier,
                originalEntityIdentifier,
                rejectedResult.getErrors ()
            );

            // Return the rejected result to the caller when background operation is available.

            return rejectedResult;
        }

        // Initialize the result by applying apply and get content.

        EditResult result = this.modelEditor.apply (
            this.session.getContent (),
            categoryIdentifier,
            originalEntityIdentifier,
            draft
        );

        // Handle the branch where result is successful.

        if ( result.isSuccessful () )
        {
            // Initialize the entity identifier by applying or else throw and get entity identifier.

            String entityIdentifier = result.getEntityIdentifier ().orElseThrow ();

            // Perform the apply edit, or else throw, get content, select entity, render model activity, and set status
            // calls required by the request apply entity operation.

            this.session.applyEdit ( result.getContent ().orElseThrow () );
            this.session.selectEntity ( categoryIdentifier, entityIdentifier );
            renderModelActivity ();
            this.view.setStatus (
                originalEntityIdentifier == null
                    ? "Added " + entityIdentifier
                    : "Updated " + entityIdentifier
            );
        }

        // Handle the alternative path when the preceding condition is not satisfied.

        else
        {
            // Apply report rejected entity draft and get errors to the result for the request apply entity operation.

            reportRejectedEntityDraft (
                categoryIdentifier,
                originalEntityIdentifier,
                result.getErrors ()
            );
        }

        // Return the result to the caller.

        return result;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requestDuplicateEntity
    //
    // Description:
    //
    //   Performs the request duplicate entity operation.
    //
    // Arguments:
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   entityIdentifier (String):
    //     The entity identifier to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void requestDuplicateEntity ( String categoryIdentifier, String entityIdentifier )
    {
        // Stop this path and return its result when background operation is available.

        if ( this.backgroundOperation != null )
        {
            return;
        }

        // Initialize the result by applying duplicate and get content.

        EditResult result = this.modelEditor.duplicate (
            this.session.getContent (),
            categoryIdentifier,
            entityIdentifier
        );

        // Handle the branch where result is successful.

        if ( result.isSuccessful () )
        {
            // Initialize the duplicate identifier by applying or else throw and get entity identifier.

            String duplicateIdentifier = result.getEntityIdentifier ().orElseThrow ();

            // Perform the apply edit, or else throw, get content, select entity, render model activity, and set status
            // calls required by the request duplicate entity operation.

            this.session.applyEdit ( result.getContent ().orElseThrow () );
            this.session.selectEntity ( categoryIdentifier, duplicateIdentifier );
            renderModelActivity ();
            this.view.setStatus ( "Duplicated " + entityIdentifier + " as " + duplicateIdentifier );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requestMoveEntity
    //
    // Description:
    //
    //   Moves a declaration one position up or down in its owning category.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void requestMoveEntity ( String categoryIdentifier, String entityIdentifier, int offset )
    {
        if ( this.backgroundOperation != null )
        {
            return;
        }

        EditResult result = this.modelEditor.move (
            this.session.getContent (),
            categoryIdentifier,
            entityIdentifier,
            offset
        );

        if ( !result.isSuccessful () )
        {
            return;
        }

        this.session.applyEdit ( result.getContent ().orElseThrow () );
        this.session.selectCategory ( categoryIdentifier );
        renderModelActivity ();
        this.view.setStatus (
            MessageFormat.format (
                MESSAGES.getString ( offset < 0 ? "status.entity.moved.up" : "status.entity.moved.down" ),
                entityIdentifier
            )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requestDeleteEntity
    //
    // Description:
    //
    //   Performs the request delete entity operation.
    //
    // Arguments:
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   entityIdentifier (String):
    //     The entity identifier to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void requestDeleteEntity ( String categoryIdentifier, String entityIdentifier )
    {
        // Stop this path and return its result when background operation is available.

        if ( this.backgroundOperation != null )
        {
            return;
        }

        // Initialize the result by applying delete and get content.

        DeleteResult result = this.modelEditor.delete (
            this.session.getContent (),
            categoryIdentifier,
            entityIdentifier
        );

        // Handle the branch where result is not successful.

        if ( !result.isSuccessful () )
        {
            // Perform the report error, join, and get references calls required by the request delete entity
            // operation.

            this.view.reportError (
                "Unable to Delete Entity",
                "The entity is still referenced by:\n"
                    + String.join ( "\n", result.getReferences () )
            );

            return;
        }

        // Perform the apply edit, or else throw, get content, select category, render model activity, and set status
        // calls required by the request delete entity operation.

        this.session.applyEdit ( result.getContent ().orElseThrow () );
        this.session.selectCategory ( categoryIdentifier );
        renderModelActivity ();
        this.view.setStatus ( "Deleted " + entityIdentifier + "; Undo restores it" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requestDeleteSelectedEntity
    //
    // Description:
    //
    //   Performs the request delete selected entity operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void requestDeleteSelectedEntity ()
    {
        // Perform the if present, get category identifier, get selection, get entity identifier, and request delete
        // entity calls required by the request delete selected entity operation.

        this.session.getSelection ().getCategoryIdentifier ().ifPresent (
            categoryIdentifier -> this.session.getSelection ().getEntityIdentifier ().ifPresent (
                entityIdentifier -> requestDeleteEntity ( categoryIdentifier, entityIdentifier )
            )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: cancelBackgroundOperation
    //
    // Description:
    //
    //   Performs the cancel background operation operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void cancelBackgroundOperation ()
    {
        // Handle the branch where background operation is available.

        if ( this.backgroundOperation != null )
        {
            // Complete the cancel background operation step by calling cancel.

            this.backgroundOperation.cancel ( true );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: saveTo
    //
    // Description:
    //
    //   Performs the save to operation.
    //
    // Arguments:
    //
    //   savePath (Path):
    //     The save path to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void saveTo ( Path savePath )
    {
        // Save without scheduling a follow-on document replacement.

        saveTo ( savePath, () -> { } );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: saveTo
    //
    // Description:
    //
    //   Saves the current document and invokes a continuation only after the save succeeds.
    //
    // Arguments:
    //
    //   savePath (Path):
    //     The save path to use.
    //
    //   successContinuation (Runnable):
    //     The continuation to invoke after a successful save.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void saveTo ( Path savePath, Runnable successContinuation )
    {
        Objects.requireNonNull ( successContinuation, "successContinuation" );

        // Initialize the saved content by applying get content.

        ClientDocumentSession.Content savedContent = this.session.getContent ();

        // Perform the run background operation, save, get file name, mark saved, render, and set status calls required
        // by the save to operation.

        runBackgroundOperation (
            this.documentOperations.save ( savePath, savedContent ),
            "Saving " + savePath.getFileName (),
            ignoredValue ->
            {
                // Perform the mark saved, render, set status, and get file name calls required by the save to
                // operation.

                this.session.markSaved ( savePath, savedContent );
                this.view.render ( this.session );
                this.view.setStatus ( "Saved " + savePath.getFileName () );
                successContinuation.run ();
            },
            "Unable to Save Model",
            BackgroundOperationType.LOCAL
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: renderModelActivity
    //
    // Description:
    //
    //   Performs the render model activity operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void renderModelActivity ()
    {
        // Apply render, report validation, and update revision status to the view for the render model activity
        // operation.

        this.view.render ( this.session );
        this.view.reportValidation ( this.session );
        updateRevisionStatus ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: continueAfterDirtyDecision
    //
    // Description:
    //
    //   Continues a replacing operation after the user cancels, saves, or discards unsaved changes.
    //
    // Arguments:
    //
    //   actionName (String):
    //     The action name to use.
    //
    //   continuation (Runnable):
    //     The replacing operation to invoke after the decision has been satisfied.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void continueAfterDirtyDecision ( String actionName, Runnable continuation )
    {
        Objects.requireNonNull ( continuation, "continuation" );

        if ( !this.session.isDirty () )
        {
            continuation.run ();
            return;
        }

        boolean canSave = this.backgroundOperation == null && !this.session.getContent ().hasValidationErrors ();
        DirtyDocumentDecision decision = this.view.chooseDirtyDocumentDecision ( actionName, canSave );

        if ( decision == DirtyDocumentDecision.DISCARD_AND_CONTINUE )
        {
            continuation.run ();
        }
        else if ( decision == DirtyDocumentDecision.SAVE_AND_CONTINUE && canSave )
        {
            Optional <Path> savePath = this.session.getLocalPath ();

            if ( savePath.isEmpty () )
            {
                savePath = this.view.chooseSavePath ( this.session.getLocalPath () );
            }

            savePath.ifPresent ( path -> saveTo ( path, continuation ) );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: runBackgroundOperation
    //
    // Description:
    //
    //   Performs the run background operation operation.
    //
    // Arguments:
    //
    //   operation (CompletableFuture <T>):
    //     The operation to use.
    //
    //   description (String):
    //     The description to use.
    //
    //   successHandler (Consumer <T>):
    //     The success handler to use.
    //
    //   errorTitle (String):
    //     The error title to use.
    //
    //   operationType (BackgroundOperationType):
    //     The operation type to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private <T> void runBackgroundOperation (
        CompletableFuture <T> operation,
        String description,
        Consumer <T> successHandler,
        String errorTitle,
        BackgroundOperationType operationType
    )
    {
        // Validate the required operation and operation type before continuing.

        Objects.requireNonNull ( operation, "operation" );
        Objects.requireNonNull ( operationType, "operationType" );

        // Stop this path and return its result when background operation is available.

        if ( this.backgroundOperation != null )
        {
            return;
        }

        this.backgroundOperation = operation;

        String previousConnectionStatus = this.connectionStatus;
        boolean remoteOperation = operationType != BackgroundOperationType.LOCAL;

        // Perform the set background operation, when complete, dispatch, accept, unwrap, set status, set connection
        // status, replace, name, get kind, report error, and diagnostic message calls required by the run background
        // operation operation.

        this.view.setBackgroundOperation ( true, description );
        if ( operationType == BackgroundOperationType.CONNECTION_TEST )
        {
            setConnectionStatus ( "Connecting" );
        }

        operation.whenComplete (
            ( result, throwable ) -> this.view.dispatch (
                () ->
                {
                    this.backgroundOperation = null;

                    // Set the background operation on the view.

                    this.view.setBackgroundOperation ( false, "" );

                    // Handle the branch where throwable is unavailable.

                    if ( throwable == null )
                    {
                        if ( remoteOperation )
                        {
                            setConnectionStatus ( "Connected" );
                        }

                        // Send the prepared value to its configured consumer.

                        successHandler.accept ( result );
                    }

                    // Handle the alternative where unwrap throwable is a cancellation exception.

                    else if ( unwrap ( throwable ) instanceof CancellationException )
                    {
                        restoreConnectionStatus ( previousConnectionStatus );

                        // Set the status on the view.

                        this.view.setStatus ( "Operation cancelled" );
                    }

                    // Handle the alternative path when the preceding condition is not satisfied.

                    else
                    {
                        // Initialize the cause by applying unwrap.

                        Throwable cause = unwrap ( throwable );

                        if ( remoteOperation )
                        {
                            updateConnectionStatusAfterRemoteFailure ( cause, previousConnectionStatus );
                        }

                        // Apply report error and diagnostic message to the view for the run background operation
                        // operation.

                        this.view.reportError ( errorTitle, diagnosticMessage ( cause ) );
                    }
                }
            )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: restoreConnectionStatus
    //
    // Description:
    //
    //   Restores a connection status only when an operation changed it.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void restoreConnectionStatus ( String previousConnectionStatus )
    {
        if ( !this.connectionStatus.equals ( previousConnectionStatus ) )
        {
            setConnectionStatus ( previousConnectionStatus );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: setConnectionStatus
    //
    // Description:
    //
    //   Records and presents the current connection status.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void setConnectionStatus ( String status )
    {
        this.connectionStatus = Objects.requireNonNull ( status, "status" );
        this.view.setConnectionStatus ( status );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: updateConnectionStatusAfterRemoteFailure
    //
    // Description:
    //
    //   Updates reachability after a remote failure without treating application-level replies as disconnections.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void updateConnectionStatusAfterRemoteFailure (
        Throwable cause,
        String previousConnectionStatus
    )
    {
        if ( !( cause instanceof ClientRemoteException ) )
        {
            restoreConnectionStatus ( previousConnectionStatus );
            return;
        }

        ClientRemoteException remoteException = (ClientRemoteException) cause;

        switch ( remoteException.getKind () )
        {
            case CONNECTION:
            case TIMEOUT:
            case PROTOCOL:
                setConnectionStatus ( "Disconnected" );
                break;

            case AUTHENTICATION:
            case NO_MODEL:
            case VALIDATION:
            case REMOTE:
                setConnectionStatus ( "Connected" );
                break;

            default:
                restoreConnectionStatus ( previousConnectionStatus );
                break;
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: unwrap
    //
    // Description:
    //
    //   Performs the unwrap operation.
    //
    // Arguments:
    //
    //   throwable (Throwable):
    //     The exception that caused the operation to fail.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Throwable unwrap ( Throwable throwable )
    {
        Throwable currentThrowable = throwable;

        // Continue processing while current throwable is a completion exception and current throwable cause is
        // available.

        while (
            ( currentThrowable instanceof CompletionException )
                && currentThrowable.getCause () != null
        )
        {
            // Update the current throwable from the get cause result.

            currentThrowable = currentThrowable.getCause ();
        }

        // Return the current throwable to the caller.

        return currentThrowable;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: reportRejectedEntityDraft
    //
    // Description:
    //
    //   Performs the report rejected entity draft operation.
    //
    // Arguments:
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   originalEntityIdentifier (String):
    //     The original entity identifier to use.
    //
    //   errors (List <FieldError>):
    //     The errors to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void reportRejectedEntityDraft (
        String categoryIdentifier,
        String originalEntityIdentifier,
        List <FieldError> errors
    )
    {
        String target = originalEntityIdentifier == null
            ? "new entity in " + categoryIdentifier
            : categoryIdentifier + "/" + originalEntityIdentifier;

        // Initialize the details by applying collect, map, stream, get field, get message, and joining.

        String details = errors.stream ()
            .map ( error -> error.getField () + ": " + error.getMessage () )
            .collect ( java.util.stream.Collectors.joining ( "\n" ) );

        // Complete the report rejected entity draft step by calling report message.

        this.view.reportMessage (
            MessageSeverity.ERROR,
            "Entity Editor",
            "Rejected " + target + ":\n" + details
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: diagnosticMessage
    //
    // Description:
    //
    //   Performs the diagnostic message operation.
    //
    // Arguments:
    //
    //   throwable (Throwable):
    //     The exception that caused the operation to fail.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String diagnosticMessage ( Throwable throwable )
    {
        // Handle the branch where throwable is a client remote exception.

        if ( throwable instanceof ClientRemoteException )
        {
            ClientRemoteException remoteException = (ClientRemoteException) throwable;

            // Return the composed diagnostic message value when throwable is a client remote exception.

            return remoteException.getKind ().name ().replace ( '_', ' ' )
                + ": "
                + safeMessage ( remoteException );
        }

        // Prepare the exception name and message values needed by the diagnostic message operation.

        String exceptionName = throwable.getClass ().getSimpleName ();
        String message       = safeMessage ( throwable );

        // Return the value selected according to exception name matches message.

        return exceptionName.equals ( message )
            ? exceptionName
            : exceptionName + ": " + message;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: safeMessage
    //
    // Description:
    //
    //   Performs the safe message operation.
    //
    // Arguments:
    //
    //   throwable (Throwable):
    //     The exception that caused the operation to fail.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String safeMessage ( Throwable throwable )
    {
        // Return the value selected according to throwable message is unavailable.

        return throwable.getMessage () == null
            ? throwable.getClass ().getSimpleName ()
            : throwable.getMessage ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: updateRevisionStatus
    //
    // Description:
    //
    //   Performs the update revision status operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void updateRevisionStatus ()
    {
        // Apply set model revision status and local revision to the view for the update revision status operation.

        this.view.setModelRevisionStatus ( localRevision (), this.serverRevision );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: localRevision
    //
    // Description:
    //
    //   Performs the local revision operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private String localRevision ()
    {
        // Return the result produced by model revision.

        return this.serverOperations.modelRevision ( this.session.getContent () );
    }
}
