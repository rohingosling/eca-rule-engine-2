//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Owns the desktop client's active immutable authoring document and its complete session state.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.application;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.rohingosling.eca.model.AuthoringModel;
import com.rohingosling.eca.model.ModelValidator;
import com.rohingosling.eca.model.ValidationDiagnostic;
import com.rohingosling.eca.model.ValidationSeverity;

//*********************************************************************************************************************
// Class: ClientDocumentSession
//
// Description:
//
//   Owns the desktop client's active immutable authoring document and its complete session state.
//
//*********************************************************************************************************************

public final class ClientDocumentSession
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    public static final String APPLICATION_TITLE = "ECA Rule Engine Laboratory";
    public static final String APPLICATION_VERSION = "2.1.0";
    public static final String UNTITLED_FILE_NAME = "Untitled";

    private static final int HISTORY_LIMIT = 100;

    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private Content              content;
    private Content              cleanContent;
    private Path                 localPath;
    private Selection            selection;
    private final ArrayDeque <Content> undoHistory;
    private final ArrayDeque <Content> redoHistory;

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
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
    //-----------------------------------------------------------------------------------------------------------------

    public Content getContent ()
    {
        // Return the content to the caller.

        return this.content;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getLocalPath
    //
    // Description:
    //
    //   Returns the local path.
    //
    // Returns:
    //
    //   The local path.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Optional <Path> getLocalPath ()
    {
        // Return an optional containing the value when it is available.

        return Optional.ofNullable ( this.localPath );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getSelection
    //
    // Description:
    //
    //   Returns the selection.
    //
    // Returns:
    //
    //   The selection.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Selection getSelection ()
    {
        // Return the selection to the caller.

        return this.selection;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: isDirty
    //
    // Description:
    //
    //   Indicates whether dirty.
    //
    // Returns:
    //
    //   `true` when the condition is satisfied; otherwise `false`.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public boolean isDirty ()
    {
        // Return whether content differs from clean content.

        return this.content != this.cleanContent;
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: canUndo
    //
    // Description:
    //
    //   Performs the can undo operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public boolean canUndo ()
    {
        // Return whether undo history contains values.

        return !this.undoHistory.isEmpty ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: canRedo
    //
    // Description:
    //
    //   Performs the can redo operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public boolean canRedo ()
    {
        // Return whether redo history contains values.

        return !this.redoHistory.isEmpty ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: canPush
    //
    // Description:
    //
    //   Performs the can push operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public boolean canPush ()
    {
        // Return whether content has not validation errors.

        return !this.content.hasValidationErrors ();
    }

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getDisplayFileName
    //
    // Description:
    //
    //   Returns the display file name.
    //
    // Returns:
    //
    //   The display file name.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String getDisplayFileName ()
    {
        // Return the value selected according to local path is unavailable.

        return this.localPath == null ? UNTITLED_FILE_NAME : this.localPath.getFileName ().toString ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getWindowTitle
    //
    // Description:
    //
    //   Returns the window title.
    //
    // Returns:
    //
    //   The window title.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String getWindowTitle ()
    {
        // Initialize the dirty marker by applying is dirty.

        String dirtyMarker = this.isDirty () ? " *" : "";

        // Return the composed get window title value.

        return APPLICATION_TITLE
            + " (Version "
            + APPLICATION_VERSION
            + " - "
            + this.getDisplayFileName ()
            + dirtyMarker
            + ")";
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/2: ClientDocumentSession
    //
    // Description:
    //
    //   Creates the ClientDocumentSession instance from the supplied values.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ClientDocumentSession ()
    {
        // Apply this and empty to the content for the client document session operation.

        this ( Content.empty (), null );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 2/2: ClientDocumentSession
    //
    // Description:
    //
    //   Creates the ClientDocumentSession instance from the supplied values.
    //
    // Arguments:
    //
    //   content (Content):
    //     The content to use.
    //
    //   localPath (Path):
    //     The local path to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private ClientDocumentSession ( Content content, Path localPath )
    {
        // Validate the required content before continuing.

        this.content      = Objects.requireNonNull ( content, "content" );
        this.cleanContent = content;
        this.localPath    = localPath;

        // Complete the client document session step by calling model.

        this.selection    = Selection.model ();
        this.undoHistory  = new ArrayDeque <Content> ();
        this.redoHistory  = new ArrayDeque <Content> ();
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: loaded
    //
    // Description:
    //
    //   Creates a ClientDocumentSession instance for the loaded case.
    //
    // Arguments:
    //
    //   content (Content):
    //     The content to use.
    //
    //   localPath (Path):
    //     The local path to use.
    //
    // Returns:
    //
    //   The resulting ClientDocumentSession instance.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static ClientDocumentSession loaded ( Content content, Path localPath )
    {
        // Return a newly constructed client document session containing the operation result.

        return new ClientDocumentSession (
            Objects.requireNonNull ( content, "content" ),
            Objects.requireNonNull ( localPath, "localPath" )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: pulled
    //
    // Description:
    //
    //   Creates a ClientDocumentSession instance for the pulled case.
    //
    // Arguments:
    //
    //   content (Content):
    //     The content to use.
    //
    // Returns:
    //
    //   The resulting ClientDocumentSession instance.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static ClientDocumentSession pulled ( Content content )
    {
        // Return a newly constructed client document session containing the operation result.

        return new ClientDocumentSession ( Objects.requireNonNull ( content, "content" ), null );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: applyEdit
    //
    // Description:
    //
    //   Performs the apply edit operation.
    //
    // Arguments:
    //
    //   replacementContent (Content):
    //     The replacement content to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void applyEdit ( Content replacementContent )
    {
        // Validate the required replacement content before continuing.

        Objects.requireNonNull ( replacementContent, "replacementContent" );

        // Stop this path and return its result when replacement content equals content.

        if ( replacementContent == this.content )
        {
            return;
        }

        // Complete the apply edit step by calling push history.

        pushHistory ( this.undoHistory, this.content );
        this.content = replacementContent;

        // Clear the redo history before collecting replacement values.

        this.redoHistory.clear ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: undo
    //
    // Description:
    //
    //   Performs the undo operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void undo ()
    {
        // Stop this path and return its result when undo history contains no values.

        if ( this.undoHistory.isEmpty () )
        {
            return;
        }

        // Complete the undo step by calling push history.

        pushHistory ( this.redoHistory, this.content );

        // Update the content from the remove last result.

        this.content = this.undoHistory.removeLast ();

        // Complete the undo step by calling normalize selection.

        normalizeSelection ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: redo
    //
    // Description:
    //
    //   Performs the redo operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void redo ()
    {
        // Stop this path and return its result when redo history contains no values.

        if ( this.redoHistory.isEmpty () )
        {
            return;
        }

        // Complete the redo step by calling push history.

        pushHistory ( this.undoHistory, this.content );

        // Update the content from the remove last result.

        this.content = this.redoHistory.removeLast ();

        // Complete the redo step by calling normalize selection.

        normalizeSelection ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: markSaved
    //
    // Description:
    //
    //   Performs the mark saved operation.
    //
    // Arguments:
    //
    //   savedPath (Path):
    //     The saved path to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void markSaved ( Path savedPath )
    {
        // Complete the mark saved step by calling mark saved.

        markSaved ( savedPath, this.content );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: markSaved
    //
    // Description:
    //
    //   Performs the mark saved operation.
    //
    // Arguments:
    //
    //   savedPath (Path):
    //     The saved path to use.
    //
    //   savedContent (Content):
    //     The saved content to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public void markSaved ( Path savedPath, Content savedContent )
    {
        // Validate the required saved path and saved content before continuing.

        this.localPath    = Objects.requireNonNull ( savedPath, "savedPath" );
        this.cleanContent = Objects.requireNonNull ( savedContent, "savedContent" );
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
        // Update the selection from the category result.

        this.selection = Selection.category ( categoryIdentifier );
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
        // Update the selection from the model result.

        this.selection = Selection.model ();
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
        // Update the selection from the entity result.

        this.selection = Selection.entity ( categoryIdentifier, entityIdentifier );
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
        // Update the selection from the simulator result.

        this.selection = Selection.simulator ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: pushHistory
    //
    // Description:
    //
    //   Performs the push history operation.
    //
    // Arguments:
    //
    //   history (ArrayDeque <Content>):
    //     The history to use.
    //
    //   content (Content):
    //     The content to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void pushHistory ( ArrayDeque <Content> history, Content content )
    {
        // Handle the branch where history size equals history limit.

        if ( history.size () == HISTORY_LIMIT )
        {
            // Remove the selected value from the history.

            history.removeFirst ();
        }

        // Complete the push history step by calling add last.

        history.addLast ( content );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: normalizeSelection
    //
    // Description:
    //
    //   Performs the normalize selection operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void normalizeSelection ()
    {
        // Stop this path and return its result when selection kind differs from selection kind entity.

        if ( this.selection.getKind () != Selection.Kind.ENTITY )
        {
            return;
        }

        // Prepare the category identifier and entity identifier values needed by the normalize selection operation.

        String categoryIdentifier = this.selection.getCategoryIdentifier ().orElseThrow ();
        String entityIdentifier   = this.selection.getEntityIdentifier ().orElseThrow ();

        // Handle the branch where content get entity identifiers category identifier does not contain entity
        // identifier.

        if ( !this.content.getEntityIdentifiers ( categoryIdentifier ).contains ( entityIdentifier ) )
        {
            // Update the selection from the category result.

            this.selection = Selection.category ( categoryIdentifier );
        }
    }

    //*****************************************************************************************************************
    // Class: Content
    //
    // Description:
    //
    //   Provides the content behavior.
    //
    //*****************************************************************************************************************

    public static final class Content
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final AuthoringModel model;
        private final Summary summary;
        private final List <ValidationMessage> validationMessages;
        private final boolean validationErrors;
        private final Map <String, List <String>> entityIdentifiers;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
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
        //-------------------------------------------------------------------------------------------------------------

        public AuthoringModel getAuthoringModel ()
        {
            // Return the model to the caller.

            return this.model;
        }

        //-------------------------------------------------------------------------------------------------------------
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
        //-------------------------------------------------------------------------------------------------------------

        public Summary getSummary ()
        {
            // Return the summary to the caller.

            return this.summary;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getValidationMessages
        //
        // Description:
        //
        //   Returns the validation messages.
        //
        // Returns:
        //
        //   The validation messages.
        //
        //-------------------------------------------------------------------------------------------------------------

        public List <ValidationMessage> getValidationMessages ()
        {
            // Return the validation messages to the caller.

            return this.validationMessages;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: hasValidationErrors
        //
        // Description:
        //
        //   Indicates whether validation errors.
        //
        // Returns:
        //
        //   `true` when the condition is satisfied; otherwise `false`.
        //
        //-------------------------------------------------------------------------------------------------------------

        public boolean hasValidationErrors ()
        {
            // Return the validation errors to the caller.

            return this.validationErrors;
        }

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getEntityIdentifiers
        //
        // Description:
        //
        //   Returns the entity identifiers.
        //
        // Arguments:
        //
        //   categoryIdentifier (String):
        //     The category identifier to use.
        //
        // Returns:
        //
        //   The entity identifiers.
        //
        //-------------------------------------------------------------------------------------------------------------

        public List <String> getEntityIdentifiers ( String categoryIdentifier )
        {
            // Return the result produced by get or default.

            return this.entityIdentifiers.getOrDefault ( categoryIdentifier, List.of () );
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: Content
        //
        // Description:
        //
        //   Creates the Content instance from the supplied values.
        //
        // Arguments:
        //
        //   model (AuthoringModel):
        //     The model to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        public Content ( AuthoringModel model )
        {
            // Perform the require non null, get model ID, get name, get description, size, get parameters, get
            // payloads, get events, get conditions, get condition sets, get actions, and get rules calls required by
            // the content operation.

            this.model = Objects.requireNonNull ( model, "model" );
            this.summary = new Summary (
                model.getModelId (),
                model.getName (),
                model.getDescription (),
                model.getParameters ().size (),
                model.getPayloads ().size (),
                model.getEvents ().size (),
                model.getConditions ().size (),
                model.getConditionSets ().size (),
                model.getActions ().size (),
                model.getRules ().size ()
            );

            // Initialize the messages with a new array list.

            ArrayList <ValidationMessage> messages = new ArrayList <ValidationMessage> ();
            boolean errorsFound = false;

            // Process each diagnostic supplied by new model validator validate model.

            for ( ValidationDiagnostic diagnostic : new ModelValidator ().validate ( model ) )
            {
                // Add new validation message diagnostic entity ID diagnostic field diagnostic code to the messages.

                messages.add (
                    new ValidationMessage (
                        diagnostic.getEntityId (),
                        diagnostic.getField (),
                        diagnostic.getCode (),
                        diagnostic.getSeverity ().name (),
                        diagnostic.getRemedy ()
                    )
                );

                // Update the errors found from the get severity result.

                errorsFound = errorsFound || diagnostic.getSeverity () == ValidationSeverity.ERROR;
            }

            // Update the validation messages from the unmodifiable list result.

            this.validationMessages = Collections.unmodifiableList ( messages );
            this.validationErrors   = errorsFound;

            // Update the entity identifiers from the entity identifiers result.

            this.entityIdentifiers  = entityIdentifiers ( model );
        }

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: empty
        //
        // Description:
        //
        //   Creates a Content instance for the empty case.
        //
        // Returns:
        //
        //   The resulting Content instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        public static Content empty ()
        {
            // Return a newly constructed content containing the operation result.

            return new Content (
                new AuthoringModel (
                    "1.0",
                    "untitled-model",
                    "Untitled",
                    "New ECA model.",
                    List.of (),
                    List.of (),
                    List.of (),
                    List.of (),
                    List.of (),
                    List.of (),
                    List.of ()
                )
            );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: entityIdentifiers
        //
        // Description:
        //
        //   Performs the entity identifiers operation.
        //
        // Arguments:
        //
        //   model (AuthoringModel):
        //     The model to use.
        //
        // Returns:
        //
        //   The result of the operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        private static Map <String, List <String>> entityIdentifiers ( AuthoringModel model )
        {
            // Initialize the identifiers with a new linked hash map.

            LinkedHashMap <String, List <String>> identifiers = new LinkedHashMap <String, List <String>> ();

            // Perform the put, to list, map, stream, get parameters, get payloads, get events, get conditions, get
            // condition sets, get actions, and get rules calls required by the entity identifiers operation.

            identifiers.put (
                "parameters",
                model.getParameters ().stream ().map ( AuthoringModel.EntityDefinition::getId ).toList ()
            );
            identifiers.put (
                "payloads",
                model.getPayloads ().stream ().map ( AuthoringModel.EntityDefinition::getId ).toList ()
            );
            identifiers.put (
                "events",
                model.getEvents ().stream ().map ( AuthoringModel.EntityDefinition::getId ).toList ()
            );
            identifiers.put (
                "conditions",
                model.getConditions ().stream ().map ( AuthoringModel.EntityDefinition::getId ).toList ()
            );
            identifiers.put (
                "condition-sets",
                model.getConditionSets ().stream ().map ( AuthoringModel.EntityDefinition::getId ).toList ()
            );
            identifiers.put (
                "actions",
                model.getActions ().stream ().map ( AuthoringModel.EntityDefinition::getId ).toList ()
            );
            identifiers.put (
                "rules",
                model.getRules ().stream ().map ( AuthoringModel.EntityDefinition::getId ).toList ()
            );

            // Return an immutable copy of identifiers.

            return Collections.unmodifiableMap ( identifiers );
        }
    }

    //*****************************************************************************************************************
    // Class: Summary
    //
    // Description:
    //
    //   Provides the summary behavior.
    //
    //*****************************************************************************************************************

    public static final class Summary
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String modelId;
        private final String name;
        private final String description;
        private final int parameterCount;
        private final int payloadCount;
        private final int eventCount;
        private final int conditionCount;
        private final int conditionSetCount;
        private final int actionCount;
        private final int ruleCount;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
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
        //-------------------------------------------------------------------------------------------------------------

        public String getModelId ()
        {
            // Return the model ID to the caller.

            return this.modelId;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getName
        //
        // Description:
        //
        //   Returns the name.
        //
        // Returns:
        //
        //   The name.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getName ()
        {
            // Return the name to the caller.

            return this.name;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getDescription
        //
        // Description:
        //
        //   Returns the description.
        //
        // Returns:
        //
        //   The description.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getDescription ()
        {
            // Return the description to the caller.

            return this.description;
        }

        //-------------------------------------------------------------------------------------------------------------
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
        //-------------------------------------------------------------------------------------------------------------

        public int getParameterCount ()
        {
            // Return the parameter count to the caller.

            return this.parameterCount;
        }

        //-------------------------------------------------------------------------------------------------------------
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
        //-------------------------------------------------------------------------------------------------------------

        public int getPayloadCount ()
        {
            // Return the payload count to the caller.

            return this.payloadCount;
        }

        //-------------------------------------------------------------------------------------------------------------
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
        //-------------------------------------------------------------------------------------------------------------

        public int getEventCount ()
        {
            // Return the event count to the caller.

            return this.eventCount;
        }

        //-------------------------------------------------------------------------------------------------------------
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
        //-------------------------------------------------------------------------------------------------------------

        public int getConditionCount ()
        {
            // Return the condition count to the caller.

            return this.conditionCount;
        }

        //-------------------------------------------------------------------------------------------------------------
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
        //-------------------------------------------------------------------------------------------------------------

        public int getConditionSetCount ()
        {
            // Return the condition set count to the caller.

            return this.conditionSetCount;
        }

        //-------------------------------------------------------------------------------------------------------------
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
        //-------------------------------------------------------------------------------------------------------------

        public int getActionCount ()
        {
            // Return the action count to the caller.

            return this.actionCount;
        }

        //-------------------------------------------------------------------------------------------------------------
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
        //-------------------------------------------------------------------------------------------------------------

        public int getRuleCount ()
        {
            // Return the rule count to the caller.

            return this.ruleCount;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: Summary
        //
        // Description:
        //
        //   Creates the Summary instance from the supplied values.
        //
        // Arguments:
        //
        //   modelId (String):
        //     The model id to use.
        //
        //   name (String):
        //     The name to use.
        //
        //   description (String):
        //     The description to use.
        //
        //   parameterCount (int):
        //     The parameter count to use.
        //
        //   payloadCount (int):
        //     The payload count to use.
        //
        //   eventCount (int):
        //     The event count to use.
        //
        //   conditionCount (int):
        //     The condition count to use.
        //
        //   conditionSetCount (int):
        //     The condition set count to use.
        //
        //   actionCount (int):
        //     The action count to use.
        //
        //   ruleCount (int):
        //     The rule count to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private Summary (
            String modelId,
            String name,
            String description,
            int parameterCount,
            int payloadCount,
            int eventCount,
            int conditionCount,
            int conditionSetCount,
            int actionCount,
            int ruleCount
        )
        {
            this.modelId           = modelId;
            this.name              = name;
            this.description       = description;
            this.parameterCount    = parameterCount;
            this.payloadCount      = payloadCount;
            this.eventCount        = eventCount;
            this.conditionCount    = conditionCount;
            this.conditionSetCount = conditionSetCount;
            this.actionCount       = actionCount;
            this.ruleCount         = ruleCount;
        }
    }

    //*****************************************************************************************************************
    // Class: ValidationMessage
    //
    // Description:
    //
    //   Provides the validation message behavior.
    //
    //*****************************************************************************************************************

    public static final class ValidationMessage
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String entityIdentifier;
        private final String field;
        private final String code;
        private final String severity;
        private final String remedy;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getEntityIdentifier
        //
        // Description:
        //
        //   Returns the entity identifier.
        //
        // Returns:
        //
        //   The entity identifier.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getEntityIdentifier ()
        {
            // Return the entity identifier to the caller.

            return this.entityIdentifier;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getField
        //
        // Description:
        //
        //   Returns the field.
        //
        // Returns:
        //
        //   The field.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getField ()
        {
            // Return the field to the caller.

            return this.field;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getCode
        //
        // Description:
        //
        //   Returns the code.
        //
        // Returns:
        //
        //   The code.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getCode ()
        {
            // Return the code to the caller.

            return this.code;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getSeverity
        //
        // Description:
        //
        //   Returns the severity.
        //
        // Returns:
        //
        //   The severity.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getSeverity ()
        {
            // Return the severity to the caller.

            return this.severity;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getRemedy
        //
        // Description:
        //
        //   Returns the remedy.
        //
        // Returns:
        //
        //   The remedy.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getRemedy ()
        {
            // Return the remedy to the caller.

            return this.remedy;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: ValidationMessage
        //
        // Description:
        //
        //   Creates the ValidationMessage instance from the supplied values.
        //
        // Arguments:
        //
        //   entityIdentifier (String):
        //     The entity identifier to use.
        //
        //   field (String):
        //     The field to use.
        //
        //   code (String):
        //     The code to use.
        //
        //   severity (String):
        //     The severity to use.
        //
        //   remedy (String):
        //     The remedy to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private ValidationMessage (
            String entityIdentifier,
            String field,
            String code,
            String severity,
            String remedy
        )
        {
            this.entityIdentifier = entityIdentifier;
            this.field            = field;
            this.code             = code;
            this.severity         = severity;
            this.remedy           = remedy;
        }
    }

    //*****************************************************************************************************************
    // Class: Selection
    //
    // Description:
    //
    //   Provides the selection behavior.
    //
    //*****************************************************************************************************************

    public static final class Selection
    {
        //=============================================================================================================
        // User Defined Data Types
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Enum: Kind
        //
        // Description:
        //
        //   Enumerates the supported kind values.
        //
        //-------------------------------------------------------------------------------------------------------------

        public enum Kind
        {
            MODEL,
            CATEGORY,
            ENTITY,
            SIMULATOR
        }

        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final Kind kind;
        private final String categoryIdentifier;
        private final String entityIdentifier;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getKind
        //
        // Description:
        //
        //   Returns the kind.
        //
        // Returns:
        //
        //   The kind.
        //
        //-------------------------------------------------------------------------------------------------------------

        public Kind getKind ()
        {
            // Return the kind to the caller.

            return this.kind;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getCategoryIdentifier
        //
        // Description:
        //
        //   Returns the category identifier.
        //
        // Returns:
        //
        //   The category identifier.
        //
        //-------------------------------------------------------------------------------------------------------------

        public Optional <String> getCategoryIdentifier ()
        {
            // Return an optional containing the value when it is available.

            return Optional.ofNullable ( this.categoryIdentifier );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getEntityIdentifier
        //
        // Description:
        //
        //   Returns the entity identifier.
        //
        // Returns:
        //
        //   The entity identifier.
        //
        //-------------------------------------------------------------------------------------------------------------

        public Optional <String> getEntityIdentifier ()
        {
            // Return an optional containing the value when it is available.

            return Optional.ofNullable ( this.entityIdentifier );
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: Selection
        //
        // Description:
        //
        //   Creates the Selection instance from the supplied values.
        //
        // Arguments:
        //
        //   kind (Kind):
        //     The kind to use.
        //
        //   categoryIdentifier (String):
        //     The category identifier to use.
        //
        //   entityIdentifier (String):
        //     The entity identifier to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private Selection ( Kind kind, String categoryIdentifier, String entityIdentifier )
        {
            // Validate the required kind before continuing.

            this.kind               = Objects.requireNonNull ( kind, "kind" );
            this.categoryIdentifier = categoryIdentifier;
            this.entityIdentifier   = entityIdentifier;
        }

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: model
        //
        // Description:
        //
        //   Creates a Selection instance for the model case.
        //
        // Returns:
        //
        //   The resulting Selection instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        public static Selection model ()
        {
            // Return a newly constructed selection containing the operation result.

            return new Selection ( Kind.MODEL, null, null );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: category
        //
        // Description:
        //
        //   Creates a Selection instance for the category case.
        //
        // Arguments:
        //
        //   categoryIdentifier (String):
        //     The category identifier to use.
        //
        // Returns:
        //
        //   The resulting Selection instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        public static Selection category ( String categoryIdentifier )
        {
            // Return a newly constructed selection containing the operation result.

            return new Selection (
                Kind.CATEGORY,
                Objects.requireNonNull ( categoryIdentifier, "categoryIdentifier" ),
                null
            );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: entity
        //
        // Description:
        //
        //   Creates a Selection instance for the entity case.
        //
        // Arguments:
        //
        //   categoryIdentifier (String):
        //     The category identifier to use.
        //
        //   entityIdentifier (String):
        //     The entity identifier to use.
        //
        // Returns:
        //
        //   The resulting Selection instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        public static Selection entity ( String categoryIdentifier, String entityIdentifier )
        {
            // Return a newly constructed selection containing the operation result.

            return new Selection (
                Kind.ENTITY,
                Objects.requireNonNull ( categoryIdentifier, "categoryIdentifier" ),
                Objects.requireNonNull ( entityIdentifier, "entityIdentifier" )
            );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: simulator
        //
        // Description:
        //
        //   Creates a Selection instance for the simulator case.
        //
        // Returns:
        //
        //   The resulting Selection instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        public static Selection simulator ()
        {
            // Return a newly constructed selection containing the operation result.

            return new Selection ( Kind.SIMULATOR, null, null );
        }
    }
}
