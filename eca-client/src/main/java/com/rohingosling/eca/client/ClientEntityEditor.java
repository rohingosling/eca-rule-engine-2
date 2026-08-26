//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Renders the synchronized JavaFX category grids and individual entity forms for the desktop model editor.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.rohingosling.eca.application.ClientDocumentSession;
import com.rohingosling.eca.application.ClientModelEditor;
import com.rohingosling.eca.application.ClientModelEditor.BindingDraft;
import com.rohingosling.eca.application.ClientModelEditor.BindingState;
import com.rohingosling.eca.application.ClientModelEditor.ConditionBindingDescriptor;
import com.rohingosling.eca.application.ClientModelEditor.ConditionOperatorDraft;
import com.rohingosling.eca.application.ClientModelEditor.ConditionParameterDescriptor;
import com.rohingosling.eca.application.ClientModelEditor.ConditionSelectionDescriptor;
import com.rohingosling.eca.application.ClientModelEditor.EntityDraft;
import com.rohingosling.eca.application.ClientModelEditor.EntitySummary;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

//*********************************************************************************************************************
// Class: ClientEntityEditor
//
// Description:
//
//   Renders the synchronized JavaFX category grids and individual entity forms for the desktop model editor.
//
//*********************************************************************************************************************

final class ClientEntityEditor
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final ResourceBundle resourceBundle;
    private final ClientPresenter presenter;
    private final ClientModelEditor modelEditor;
    private final VBox root;
    private final BorderPane node;

    private ClientDocumentSession renderedSession;
    private String                preferredGridCategoryIdentifier;
    private String                preferredGridEntityIdentifier;

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: ClientEntityEditor
    //
    // Description:
    //
    //   Creates the ClientEntityEditor instance from the supplied values.
    //
    // Arguments:
    //
    //   resourceBundle (ResourceBundle):
    //     The resource bundle to use.
    //
    //   presenter (ClientPresenter):
    //     The presenter to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    ClientEntityEditor ( ResourceBundle resourceBundle, ClientPresenter presenter )
    {
        // Validate the required resource bundle and presenter before continuing.

        this.resourceBundle = Objects.requireNonNull ( resourceBundle, "resourceBundle" );
        this.presenter      = Objects.requireNonNull ( presenter, "presenter" );
        this.modelEditor    = new ClientModelEditor ();
        this.root           = new VBox ( 12.0 );

        ScrollPane scrollPane = new ScrollPane ( this.root );

        scrollPane.setFitToWidth ( true );
        scrollPane.setPannable ( true );
        scrollPane.getStyleClass ().add ( "editor-scroll-pane" );
        this.node = new BorderPane ( scrollPane );
        this.node.getStyleClass ().add ( "entity-editor" );

        // Set the fill width on the root.

        this.root.setFillWidth ( true );
    }

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getNode
    //
    // Description:
    //
    //   Returns the node.
    //
    // Returns:
    //
    //   The node.
    //
    //-----------------------------------------------------------------------------------------------------------------

    Node getNode ()
    {
        return this.node;
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

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

    void render ( ClientDocumentSession session )
    {
        // Validate the required session before continuing.

        this.renderedSession = Objects.requireNonNull ( session, "session" );

        // Stop this path and return its result when session selection kind differs from client document session
        // selection kind category and session selection kind differs from client document session selection kind
        // entity.

        if (
            session.getSelection ().getKind () != ClientDocumentSession.Selection.Kind.CATEGORY
                && session.getSelection ().getKind () != ClientDocumentSession.Selection.Kind.ENTITY
        )
        {
            return;
        }

        // Prepare the category identifier and entity identifier values needed by the render operation.

        String categoryIdentifier = session.getSelection ().getCategoryIdentifier ().orElseThrow ();
        String entityIdentifier = session.getSelection ().getEntityIdentifier ().orElse ( null );

        // Handle the branch where entity identifier is unavailable.

        if ( entityIdentifier == null )
        {
            // Complete the render step by calling show category grid.

            showCategoryGrid ( categoryIdentifier );
        }

        // Handle the alternative path when the preceding condition is not satisfied.

        else
        {
            // Perform the show entity form, create draft, and get content calls required by the render operation.

            showEntityForm (
                categoryIdentifier,
                entityIdentifier,
                this.modelEditor.createDraft (
                    session.getContent (),
                    categoryIdentifier,
                    entityIdentifier
                )
            );
        }
    }

    //=================================================================================================================
    // Mutators
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: setDisabled
    //
    // Description:
    //
    //   Sets the disabled.
    //
    // Arguments:
    //
    //   disabled (boolean):
    //     The disabled to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    void setDisabled ( boolean disabled )
    {
        // Set the disable on the root.

        this.node.setDisable ( disabled );
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: showCategoryGrid
    //
    // Description:
    //
    //   Performs the show category grid operation.
    //
    // Arguments:
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void showCategoryGrid ( String categoryIdentifier )
    {
        // Prepare the entity table, entities, identifier column, name column, and details column values needed by the
        // show category grid operation.

        TableView <EntitySummary> entityTable = new TableView <EntitySummary> ();
        List <EntitySummary> entities = this.modelEditor.getEntitySummaries (
            this.renderedSession.getContent (),
            categoryIdentifier
        );

        TableColumn <EntitySummary, String> identifierColumn =
            new TableColumn <EntitySummary, String> ( text ( "editor.column.id" ) );
        TableColumn <EntitySummary, String> nameColumn =
            new TableColumn <EntitySummary, String> ( text ( "editor.column.name" ) );
        TableColumn <EntitySummary, String> detailsColumn =
            new TableColumn <EntitySummary, String> (
                detailsColumnTitle ( categoryIdentifier )
            );

        // Perform the set cell value factory, get identifier, get value, get name, get details, set pref width, add,
        // get columns, set items, observable array list, set column resize policy, set placeholder, text, set pref
        // height, max, min, size, set row factory, set on mouse clicked, get click count, is empty, select entity, and
        // get item calls required by the show category grid operation.

        identifierColumn.setCellValueFactory (
            data -> new ReadOnlyStringWrapper ( data.getValue ().getIdentifier () )
        );
        nameColumn.setCellValueFactory (
            data -> new ReadOnlyStringWrapper ( data.getValue ().getName () )
        );
        detailsColumn.setCellValueFactory (
            data -> new ReadOnlyStringWrapper ( data.getValue ().getDetails () )
        );
        identifierColumn.setPrefWidth ( 230.0 );
        nameColumn.setPrefWidth ( 230.0 );
        detailsColumn.setPrefWidth ( 360.0 );
        entityTable.getColumns ().add ( identifierColumn );
        entityTable.getColumns ().add ( nameColumn );
        entityTable.getColumns ().add ( detailsColumn );
        entityTable.setItems ( FXCollections.observableArrayList ( entities ) );
        entityTable.setColumnResizePolicy ( TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN );
        entityTable.setPlaceholder ( new Label ( text ( "editor.grid.empty" ) ) );
        entityTable.setPrefHeight ( Math.max ( 220.0, Math.min ( 560.0, 48.0 + entities.size () * 28.0 ) ) );
        entityTable.setRowFactory (
            ignoredTable ->
            {
                // Initialize the row with a new table row.

                TableRow <EntitySummary> row = new TableRow <EntitySummary> ();

                // Perform the set on mouse clicked, get click count, is empty, select entity, get identifier, and get
                // item calls required by the show category grid operation.

                row.setOnMouseClicked (
                    event ->
                    {
                        // Handle the branch where event click count equals 2 and row contains values.

                        if ( event.getClickCount () == 2 && !row.isEmpty () )
                        {
                            // Perform the select entity, get identifier, and get item calls required by the show
                            // category grid operation.

                            this.presenter.selectEntity (
                                categoryIdentifier,
                                row.getItem ().getIdentifier ()
                            );
                        }
                    }
                );

                // Return the row to the caller.

                return row;
            }
        );

        // Prepare the add button, duplicate button, edit button, and delete button values needed by the show category
        // grid operation.

        Button addButton = new Button (
            text ( "button.add" ),
            ClientFluentIcons.create ( ClientFluentIcons.TOOLBAR_SIZE, "add" )
        );
        Button duplicateButton = new Button (
            text ( "button.duplicate" ),
            ClientFluentIcons.create ( ClientFluentIcons.TOOLBAR_SIZE, "copy_add" )
        );
        Button moveUpButton = new Button (
            text ( "button.move.up" ),
            ClientFluentIcons.create ( ClientFluentIcons.TOOLBAR_SIZE, "arrow_up" )
        );
        Button moveDownButton = new Button (
            text ( "button.move.down" ),
            ClientFluentIcons.create ( ClientFluentIcons.TOOLBAR_SIZE, "arrow_down" )
        );
        Button editButton = new Button (
            text ( "button.edit" ),
            ClientFluentIcons.create ( ClientFluentIcons.TOOLBAR_SIZE, "edit" )
        );
        Button deleteButton = new Button (
            text ( "button.delete" ),
            ClientFluentIcons.create ( ClientFluentIcons.TOOLBAR_SIZE, "delete" )
        );

        // Perform the set disable, add listener, selected item property, get selection model, set on action, show
        // entity form, create draft, get content, if present, selected entity, request duplicate entity, get
        // identifier, select entity, and request delete entity calls required by the show category grid operation.

        duplicateButton.setDisable ( true );
        moveUpButton.setDisable ( true );
        moveDownButton.setDisable ( true );
        editButton.setDisable ( true );
        deleteButton.setDisable ( true );
        entityTable.getSelectionModel ().selectedItemProperty ().addListener (
            ( observableValue, oldEntity, newEntity ) ->
            {
                boolean noSelection = newEntity == null;
                int selectedIndex = entityTable.getSelectionModel ().getSelectedIndex ();

                // Set the disable on the duplicate button.

                duplicateButton.setDisable ( noSelection );
                moveUpButton.setDisable ( noSelection || selectedIndex <= 0 );
                moveDownButton.setDisable ( noSelection || selectedIndex >= entityTable.getItems ().size () - 1 );
                editButton.setDisable ( noSelection );
                deleteButton.setDisable ( noSelection );
            }
        );
        addButton.setOnAction (
            event -> showEntityForm (
                categoryIdentifier,
                null,
                this.modelEditor.createDraft (
                    this.renderedSession.getContent (),
                    categoryIdentifier
                )
            )
        );
        duplicateButton.setOnAction (
            event -> selectedEntity ( entityTable ).ifPresent (
                entity -> this.presenter.requestDuplicateEntity (
                    categoryIdentifier,
                    entity.getIdentifier ()
                )
            )
        );
        moveUpButton.setOnAction (
            event -> selectedEntity ( entityTable ).ifPresent (
                entity ->
                {
                    this.preferredGridCategoryIdentifier = categoryIdentifier;
                    this.preferredGridEntityIdentifier   = entity.getIdentifier ();
                    this.presenter.requestMoveEntity ( categoryIdentifier, entity.getIdentifier (), -1 );
                }
            )
        );
        moveDownButton.setOnAction (
            event -> selectedEntity ( entityTable ).ifPresent (
                entity ->
                {
                    this.preferredGridCategoryIdentifier = categoryIdentifier;
                    this.preferredGridEntityIdentifier   = entity.getIdentifier ();
                    this.presenter.requestMoveEntity ( categoryIdentifier, entity.getIdentifier (), 1 );
                }
            )
        );
        editButton.setOnAction (
            event -> selectedEntity ( entityTable ).ifPresent (
                entity -> this.presenter.selectEntity (
                    categoryIdentifier,
                    entity.getIdentifier ()
                )
            )
        );
        deleteButton.setOnAction (
            event -> selectedEntity ( entityTable ).ifPresent (
                entity -> this.presenter.requestDeleteEntity (
                    categoryIdentifier,
                    entity.getIdentifier ()
                )
            )
        );

        // Prepare the grid content and grid group values needed by the show category grid operation.

        TitledPane gridGroup = ClientMain.createGroupBox (
            categoryName ( categoryIdentifier ),
            entityTable
        );
        HBox actionBar = ClientMain.createPageActionFooter (
            moveUpButton,
            moveDownButton,
            addButton,
            duplicateButton,
            deleteButton,
            editButton
        );
        ScrollPane actionBarScrollPane = new ScrollPane ( actionBar );

        actionBarScrollPane.setFitToHeight ( true );
        actionBarScrollPane.setFitToWidth ( true );
        actionBarScrollPane.setHbarPolicy ( ScrollPane.ScrollBarPolicy.AS_NEEDED );
        actionBarScrollPane.setVbarPolicy ( ScrollPane.ScrollBarPolicy.NEVER );
        actionBarScrollPane.getStyleClass ().add ( "page-action-footer-scroll" );

        // Perform the set vgrow, set all, and get children calls required by the show category grid operation.

        VBox.setVgrow ( entityTable, Priority.ALWAYS );
        VBox.setVgrow ( gridGroup, Priority.ALWAYS );
        this.root.getChildren ().setAll ( gridGroup );
        this.node.setBottom ( actionBarScrollPane );

        if ( categoryIdentifier.equals ( this.preferredGridCategoryIdentifier ) )
        {
            entityTable.getItems ().stream ()
                .filter ( entity -> entity.getIdentifier ().equals ( this.preferredGridEntityIdentifier ) )
                .findFirst ()
                .ifPresent (
                    entity ->
                    {
                        entityTable.getSelectionModel ().select ( entity );
                        entityTable.scrollTo ( entity );
                    }
                );
            this.preferredGridCategoryIdentifier = null;
            this.preferredGridEntityIdentifier   = null;
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: showEntityForm
    //
    // Description:
    //
    //   Performs the show entity form operation.
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
    //-----------------------------------------------------------------------------------------------------------------

    private void showEntityForm (
        String categoryIdentifier,
        String originalEntityIdentifier,
        EntityDraft draft
    )
    {
        // Prepare the common controls and sections values needed by the show entity form operation.

        CommonControls commonControls = createCommonControls ( draft );
        VBox sections = new VBox (
            12.0,
            createIdentityGroup ( commonControls )
        );
        Supplier <EntityDraft> draftSupplier;

        // Select the processing branch for category identifier.

        switch ( categoryIdentifier )
        {
            // Handle client model editor parameters through this switch branch.

            case ClientModelEditor.PARAMETERS:
                draftSupplier = addParameterControls ( sections, commonControls, draft );
                break;

            // Handle client model editor payloads through this switch branch.

            case ClientModelEditor.PAYLOADS:
                draftSupplier = addPayloadControls ( sections, commonControls, draft );
                break;

            // Handle client model editor events through this switch branch.

            case ClientModelEditor.EVENTS:
                draftSupplier = addEventControls ( sections, commonControls, draft );
                break;

            // Handle client model editor conditions through this switch branch.

            case ClientModelEditor.CONDITIONS:
                draftSupplier = addConditionControls ( sections, commonControls, draft );
                break;

            // Handle client model editor condition sets through this switch branch.

            case ClientModelEditor.CONDITION_SETS:
                draftSupplier = addConditionSetControls ( sections, commonControls, draft );
                break;

            // Handle client model editor actions through this switch branch.

            case ClientModelEditor.ACTIONS:
                draftSupplier = () -> EntityDraft.action (
                    commonControls.identifierField.getText ().trim (),
                    commonControls.nameField.getText ().trim (),
                    commonControls.descriptionField.getText ().trim ()
                );
                break;

            // Handle client model editor rules through this switch branch.

            case ClientModelEditor.RULES:
                draftSupplier = addRuleControls ( sections, commonControls, draft );
                break;

            // Handle the default case through this switch branch.

            default:
                throw new IllegalArgumentException ( "Unknown model category: " + categoryIdentifier );
        }

        // Prepare the apply button and cancel button values needed by the show entity form operation.

        Button applyButton  = new Button ( text ( "button.apply" ) );
        Button cancelButton = new Button ( text ( "button.cancel" ) );

        // Perform the set on action, get, request apply entity, select category, render, add, get children, create
        // page action footer, and set all calls required by the show entity form operation.

        applyButton.setOnAction (
            event ->
            {
                // Initialize the replacement draft by applying get.

                EntityDraft replacementDraft = draftSupplier.get ();

                // Complete the show entity form step by calling request apply entity.

                this.presenter.requestApplyEntity (
                    categoryIdentifier,
                    originalEntityIdentifier,
                    replacementDraft
                );
            }
        );
        cancelButton.setOnAction (
            event ->
            {
                // Handle the branch where original entity identifier is unavailable.

                if ( originalEntityIdentifier == null )
                {
                    // Complete the show entity form step by calling select category.

                    this.presenter.selectCategory ( categoryIdentifier );
                }

                // Handle the alternative path when the preceding condition is not satisfied.

                else
                {
                    // Complete the show entity form step by calling render.

                    render ( this.renderedSession );
                }
            }
        );

        this.root.getChildren ().setAll ( sections );
        this.node.setBottom ( ClientMain.createPageActionFooter ( applyButton, cancelButton ) );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createCommonControls
    //
    // Description:
    //
    //   Performs the create common controls operation.
    //
    // Arguments:
    //
    //   draft (EntityDraft):
    //     The draft to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private CommonControls createCommonControls ( EntityDraft draft )
    {
        // Prepare the identifier field, name field, and description field values needed by the create common controls
        // operation.

        TextField identifierField = new TextField ( draft.getId () );
        TextField nameField = new TextField ( draft.getName () );
        TextArea descriptionField = new TextArea ( draft.getDescription () );

        // Perform the set max width, set wrap text, and set pref row count calls required by the create common
        // controls operation.

        identifierField.setMaxWidth ( Double.MAX_VALUE );
        nameField.setMaxWidth ( Double.MAX_VALUE );
        descriptionField.setWrapText ( true );
        descriptionField.setPrefRowCount ( 3 );

        // Return a newly constructed common controls containing the operation result.

        return new CommonControls ( identifierField, nameField, descriptionField );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createIdentityGroup
    //
    // Description:
    //
    //   Performs the create identity group operation.
    //
    // Arguments:
    //
    //   controls (CommonControls):
    //     The controls to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private TitledPane createIdentityGroup ( CommonControls controls )
    {
        // Initialize the identity grid by applying create form grid.

        GridPane identityGrid = ClientMain.createFormGrid ();

        // Apply add form row and text to the client main for the create identity group operation.

        ClientMain.addFormRow (
            identityGrid,
            0,
            text ( "editor.field.id" ),
            controls.identifierField
        );
        ClientMain.addFormRow (
            identityGrid,
            1,
            text ( "editor.field.name" ),
            controls.nameField
        );
        ClientMain.addFormRow (
            identityGrid,
            2,
            text ( "editor.field.description" ),
            controls.descriptionField
        );

        // Return the result produced by create group box.

        return ClientMain.createGroupBox ( text ( "editor.identity.group" ), identityGrid );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: addParameterControls
    //
    // Description:
    //
    //   Performs the add parameter controls operation.
    //
    // Arguments:
    //
    //   sections (VBox):
    //     The sections to use.
    //
    //   commonControls (CommonControls):
    //     The common controls to use.
    //
    //   draft (EntityDraft):
    //     The draft to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private Supplier <EntityDraft> addParameterControls (
        VBox sections,
        CommonControls commonControls,
        EntityDraft draft
    )
    {
        // Prepare the type choice, enumeration values field, and parameter grid values needed by the add parameter
        // controls operation.

        ComboBox <String> typeChoice = new ComboBox <String> (
            FXCollections.observableArrayList (
                "STRING",
                "INTEGER",
                "BOOLEAN",
                "ENUM"
            )
        );
        TextArea enumerationValuesField = new TextArea (
            String.join ( System.lineSeparator (), draft.getEnumerationValues () )
        );
        GridPane parameterGrid = ClientMain.createFormGrid ();

        // Perform the set value, get parameter type, set max width, set pref row count, set prompt text, text, and add
        // form row calls required by the add parameter controls operation.

        typeChoice.setValue ( draft.getParameterType () );
        typeChoice.setMaxWidth ( Double.MAX_VALUE );
        enumerationValuesField.setPrefRowCount ( 5 );
        enumerationValuesField.setPromptText ( text ( "editor.parameter.enum.prompt" ) );
        ClientMain.addFormRow (
            parameterGrid,
            0,
            text ( "editor.parameter.type" ),
            typeChoice
        );

        // Prepare the enumeration values label and update enumeration values visibility values needed by the add
        // parameter controls operation.

        Label enumerationValuesLabel = ClientMain.addFormRow (
            parameterGrid,
            1,
            text ( "editor.parameter.enum.values" ),
            enumerationValuesField
        );
        Runnable updateEnumerationValuesVisibility = () ->
        {
            // Initialize the enumeration type selected by applying equals and get value.

            boolean enumerationTypeSelected = "ENUM".equals ( typeChoice.getValue () );

            // Perform the set managed and set visible calls required by the add parameter controls operation.

            enumerationValuesLabel.setManaged ( enumerationTypeSelected );
            enumerationValuesLabel.setVisible ( enumerationTypeSelected );
            enumerationValuesField.setManaged ( enumerationTypeSelected );
            enumerationValuesField.setVisible ( enumerationTypeSelected );
        };

        // Perform the add listener, value property, run, add, get children, create group box, and text calls required
        // by the add parameter controls operation.

        typeChoice.valueProperty ().addListener (
            ( observableValue, oldType, newType ) -> updateEnumerationValuesVisibility.run ()
        );
        updateEnumerationValuesVisibility.run ();
        sections.getChildren ().add (
            ClientMain.createGroupBox ( text ( "editor.parameter.group" ), parameterGrid )
        );

        // Return the add parameter controls result to the caller.

        return () -> EntityDraft.parameter (
            commonControls.identifierField.getText ().trim (),
            commonControls.nameField.getText ().trim (),
            commonControls.descriptionField.getText ().trim (),
            typeChoice.getValue (),
            "ENUM".equals ( typeChoice.getValue () )
                ? nonblankLines ( enumerationValuesField.getText () )
                : List.of ()
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: addPayloadControls
    //
    // Description:
    //
    //   Performs the add payload controls operation.
    //
    // Arguments:
    //
    //   sections (VBox):
    //     The sections to use.
    //
    //   commonControls (CommonControls):
    //     The common controls to use.
    //
    //   draft (EntityDraft):
    //     The draft to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private Supplier <EntityDraft> addPayloadControls (
        VBox sections,
        CommonControls commonControls,
        EntityDraft draft
    )
    {
        // Prepare the parameters list, selection model, and payload grid values needed by the add payload controls
        // operation.

        ListView <String> parametersList = new ListView <String> (
            FXCollections.observableArrayList (
                this.modelEditor.getCompatibleIdentifiers (
                    this.renderedSession.getContent (),
                    ClientModelEditor.PAYLOADS,
                    "parameterIds"
                )
            )
        );
        MultipleSelectionModel <String> selectionModel = parametersList.getSelectionModel ();
        GridPane payloadGrid = ClientMain.createFormGrid ();

        // Perform the set selection mode, for each, get parameter IDs, set pref height, add form row, text, add, get
        // children, and create group box calls required by the add payload controls operation.

        selectionModel.setSelectionMode ( SelectionMode.MULTIPLE );
        draft.getParameterIds ().forEach ( selectionModel::select );
        parametersList.setPrefHeight ( 220.0 );
        ClientMain.addFormRow (
            payloadGrid,
            0,
            text ( "editor.payload.parameters" ),
            parametersList
        );
        sections.getChildren ().add (
            ClientMain.createGroupBox ( text ( "editor.payload.group" ), payloadGrid )
        );

        // Return the add payload controls result to the caller.

        return () -> EntityDraft.payload (
            commonControls.identifierField.getText ().trim (),
            commonControls.nameField.getText ().trim (),
            commonControls.descriptionField.getText ().trim (),
            List.copyOf ( selectionModel.getSelectedItems () )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: addEventControls
    //
    // Description:
    //
    //   Performs the add event controls operation.
    //
    // Arguments:
    //
    //   sections (VBox):
    //     The sections to use.
    //
    //   commonControls (CommonControls):
    //     The common controls to use.
    //
    //   draft (EntityDraft):
    //     The draft to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private Supplier <EntityDraft> addEventControls (
        VBox sections,
        CommonControls commonControls,
        EntityDraft draft
    )
    {
        // Prepare the payload choice and event grid values needed by the add event controls operation.

        ComboBox <String> payloadChoice = referenceChoice (
            ClientModelEditor.EVENTS,
            "payloadId",
            draft.getPayloadId ()
        );
        GridPane eventGrid = ClientMain.createFormGrid ();

        // Perform the add form row, text, add, get children, and create group box calls required by the add event
        // controls operation.

        ClientMain.addFormRow (
            eventGrid,
            0,
            text ( "editor.event.payload" ),
            payloadChoice
        );
        sections.getChildren ().add (
            ClientMain.createGroupBox ( text ( "editor.event.group" ), eventGrid )
        );

        // Return the add event controls result to the caller.

        return () -> EntityDraft.event (
            commonControls.identifierField.getText ().trim (),
            commonControls.nameField.getText ().trim (),
            commonControls.descriptionField.getText ().trim (),
            payloadChoice.getValue ()
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: addConditionControls
    //
    // Description:
    //
    //   Performs the add condition controls operation.
    //
    // Arguments:
    //
    //   sections (VBox):
    //     The sections to use.
    //
    //   commonControls (CommonControls):
    //     The common controls to use.
    //
    //   draft (EntityDraft):
    //     The draft to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private Supplier <EntityDraft> addConditionControls (
        VBox sections,
        CommonControls commonControls,
        EntityDraft draft
    )
    {
        // Prepare the parameter choice, operator choice, value choice, second value choice, and condition grid values
        // needed by the add condition controls operation.

        ComboBox <String> parameterChoice = referenceChoice (
            ClientModelEditor.CONDITIONS,
            "parameterId",
            draft.getParameterId ()
        );
        ComboBox <ConditionOperatorDraft> operatorChoice = new ComboBox <ConditionOperatorDraft> ();
        ComboBox <String> valueChoice = editableValueChoice ( draft.getConditionValueText () );
        ComboBox <String> secondValueChoice = editableValueChoice (
            draft.getSecondConditionValueText ()
        );
        GridPane conditionGrid = ClientMain.createFormGrid ();

        // Apply add form row and text to the client main for the add condition controls operation.

        ClientMain.addFormRow (
            conditionGrid,
            0,
            text ( "editor.condition.parameter" ),
            parameterChoice
        );
        ClientMain.addFormRow (
            conditionGrid,
            1,
            text ( "editor.condition.operator" ),
            operatorChoice
        );

        // Prepare the value label and second value label values needed by the add condition controls operation.

        Label valueLabel = ClientMain.addFormRow (
            conditionGrid,
            2,
            text ( "editor.condition.value" ),
            valueChoice
        );
        Label secondValueLabel = ClientMain.addFormRow (
            conditionGrid,
            3,
            text ( "editor.condition.second.value" ),
            secondValueChoice
        );

        // Configure the operator choice with the required converter and max width values.

        operatorChoice.setConverter ( new ConditionOperatorStringConverter ( this.resourceBundle ) );
        operatorChoice.setMaxWidth ( Double.MAX_VALUE );

        // Prepare the update value visibility and update parameter controls values needed by the add condition
        // controls operation.

        Runnable updateValueVisibility = () ->
        {
            // Prepare the operator, first value visible, and second value visible values needed by the add condition
            // controls operation.

            ConditionOperatorDraft operator = operatorChoice.getValue ();
            boolean firstValueVisible = operator != null && operator.getOperandCount () >= 1;
            boolean secondValueVisible = operator != null && operator.getOperandCount () == 2;

            // Set the managed visible on the target.

            setManagedVisible ( valueLabel, firstValueVisible );
            setManagedVisible ( valueChoice, firstValueVisible );
            setManagedVisible ( secondValueLabel, secondValueVisible );
            setManagedVisible ( secondValueChoice, secondValueVisible );
        };
        Consumer <Boolean> updateParameterControls = resetValues ->
        {
            // Prepare the parameter, operators, and selected operator values needed by the add condition controls
            // operation.

            ConditionParameterDescriptor parameter = findParameter ( parameterChoice.getValue () );
            List <ConditionOperatorDraft> operators = this.modelEditor.getSupportedConditionOperators (
                parameter
            );
            ConditionOperatorDraft selectedOperator = parseOperator ( draft.getConditionOperator () );

            // Handle the branch where operators does not contain operator choice value.

            if ( !operators.contains ( operatorChoice.getValue () ) )
            {
                // Update the selected operator from the contains result.

                selectedOperator = operators.contains ( selectedOperator )
                    ? selectedOperator
                    : ConditionOperatorDraft.EQUALS;
            }

            // Handle the alternative path when the preceding condition is not satisfied.

            else
            {
                // Update the selected operator from the get value result.

                selectedOperator = operatorChoice.getValue ();
            }

            // Perform the set items, observable array list, and set value calls required by the add condition controls
            // operation.

            operatorChoice.setItems ( FXCollections.observableArrayList ( operators ) );
            operatorChoice.setValue ( selectedOperator );

            // Prepare the first value and second value values needed by the add condition controls operation.

            String firstValue = resetValues
                ? this.modelEditor.getDefaultConditionValue ( parameter )
                : valueChoice.getEditor ().getText ();
            String secondValue = resetValues
                ? ""
                : secondValueChoice.getEditor ().getText ();

            // Apply configure value choice and run to the update value visibility for the add condition controls
            // operation.

            configureValueChoice (
                valueChoice,
                parameter,
                firstValue
            );
            configureValueChoice (
                secondValueChoice,
                parameter,
                secondValue
            );
            updateValueVisibility.run ();
        };

        // Perform the add listener, value property, run, accept, add, get children, create group box, and text calls
        // required by the add condition controls operation.

        operatorChoice.valueProperty ().addListener (
            ( observableValue, oldOperator, newOperator ) -> updateValueVisibility.run ()
        );
        parameterChoice.valueProperty ().addListener (
            ( observableValue, oldIdentifier, newIdentifier ) -> updateParameterControls.accept ( true )
        );
        updateParameterControls.accept ( false );

        sections.getChildren ().add (
            ClientMain.createGroupBox ( text ( "editor.condition.group" ), conditionGrid )
        );

        // Return the add condition controls result to the caller.

        return () -> EntityDraft.condition (
            commonControls.identifierField.getText ().trim (),
            commonControls.nameField.getText ().trim (),
            commonControls.descriptionField.getText ().trim (),
            parameterChoice.getValue (),
            operatorChoice.getValue () == null ? "" : operatorChoice.getValue ().name (),
            valueChoice.getEditor ().getText (),
            secondValueChoice.getEditor ().getText ()
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: addConditionSetControls
    //
    // Description:
    //
    //   Performs the add condition set controls operation.
    //
    // Arguments:
    //
    //   sections (VBox):
    //     The sections to use.
    //
    //   commonControls (CommonControls):
    //     The common controls to use.
    //
    //   draft (EntityDraft):
    //     The draft to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private Supplier <EntityDraft> addConditionSetControls (
        VBox sections,
        CommonControls commonControls,
        EntityDraft draft
    )
    {
        // Prepare the condition choices, selection controls, and initial selected condition IDs values needed by the
        // add condition set controls operation.

        VBox conditionChoices = new VBox ( 8.0 );
        ArrayList <ConditionSelectionControls> selectionControls =
            new ArrayList <ConditionSelectionControls> ();
        ArrayList <String> initialSelectedConditionIds = new ArrayList <String> ();

        // Process each descriptor supplied by model editor get condition selection descriptors rendered session
        // content draft.

        for (
            ConditionSelectionDescriptor descriptor
                : this.modelEditor.getConditionSelectionDescriptors (
                    this.renderedSession.getContent (),
                    draft
                )
        )
        {
            // Initialize the condition choice by applying get name and get expression.

            CheckBox conditionChoice = new CheckBox (
                descriptor.getName () + " \u2014 " + descriptor.getExpression ()
            );

            // Perform the set selected, is selected, set wrap text, and set max width calls required by the add
            // condition set controls operation.

            conditionChoice.setSelected ( descriptor.isSelected () );
            conditionChoice.setWrapText ( true );
            conditionChoice.setMaxWidth ( Double.MAX_VALUE );

            // Handle the branch where descriptor is not configured.

            if ( !descriptor.isConfigured () )
            {
                // Apply set text, get text, and text to the condition choice for the add condition set controls
                // operation.

                conditionChoice.setText (
                    conditionChoice.getText ()
                        + " "
                        + text ( "editor.condition.set.configuration.required" )
                );
            }

            // Add new condition selection controls descriptor condition identifier condition choice to the selection
            // controls.

            selectionControls.add (
                new ConditionSelectionControls (
                    descriptor.getConditionIdentifier (),
                    conditionChoice
                )
            );

            // Handle the branch where descriptor is selected.

            if ( descriptor.isSelected () )
            {
                // Add descriptor condition identifier to the initial selected condition IDs.

                initialSelectedConditionIds.add ( descriptor.getConditionIdentifier () );
            }

            // Add condition choice to the get children.

            conditionChoices.getChildren ().add ( conditionChoice );
        }

        // Initialize the explanation by applying text.

        Label explanation = new Label ( text ( "editor.condition.set.explanation" ) );

        // Perform the set wrap text, add, and get style class calls required by the add condition set controls
        // operation.

        explanation.setWrapText ( true );
        explanation.getStyleClass ().add ( "secondary-text" );

        // Initialize the binding content with a new v box.

        VBox bindingContent = new VBox ( 10.0, conditionChoices, explanation );

        // Add client main create group box text "editor condition set group" binding content to the get children.

        sections.getChildren ().add (
            ClientMain.createGroupBox ( text ( "editor.condition.set.group" ), bindingContent )
        );

        // Return the add condition set controls result to the caller.

        return () ->
        {
            // Initialize the condition IDs by applying to list, map, filter, stream, and is selected.

            List <String> conditionIds = selectionControls.stream ()
                .filter ( controls -> controls.conditionChoice.isSelected () )
                .map ( controls -> controls.conditionIdentifier )
                .toList ();

            // Stop this path and return its result when draft uses predefined conditions does not succeed and
            // condition IDs matches initial selected condition IDs.

            if (
                !draft.usesPredefinedConditions ()
                    && conditionIds.equals ( initialSelectedConditionIds )
            )
            {
                // Return the result produced by legacy condition set when draft uses predefined conditions does not
                // succeed and condition IDs matches initial selected condition IDs.

                return EntityDraft.legacyConditionSet (
                    commonControls.identifierField.getText ().trim (),
                    commonControls.nameField.getText ().trim (),
                    commonControls.descriptionField.getText ().trim (),
                    draft.getBindings ()
                );
            }

            // Return the result produced by condition set.

            return EntityDraft.conditionSet (
                commonControls.identifierField.getText ().trim (),
                commonControls.nameField.getText ().trim (),
                commonControls.descriptionField.getText ().trim (),
                conditionIds
            );
        };
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: addRuleControls
    //
    // Description:
    //
    //   Performs the add rule controls operation.
    //
    // Arguments:
    //
    //   sections (VBox):
    //     The sections to use.
    //
    //   commonControls (CommonControls):
    //     The common controls to use.
    //
    //   draft (EntityDraft):
    //     The draft to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private Supplier <EntityDraft> addRuleControls (
        VBox sections,
        CommonControls commonControls,
        EntityDraft draft
    )
    {
        // Prepare the event choice, condition set choice, action choice, and rule grid values needed by the add rule
        // controls operation.

        ComboBox <String> eventChoice = referenceChoice (
            ClientModelEditor.RULES,
            "eventId",
            draft.getEventId ()
        );
        ComboBox <String> conditionSetChoice = referenceChoice (
            ClientModelEditor.RULES,
            "conditionSetId",
            draft.getConditionSetId ()
        );
        ComboBox <String> actionChoice = referenceChoice (
            ClientModelEditor.RULES,
            "actionId",
            draft.getActionId ()
        );
        GridPane ruleGrid = ClientMain.createFormGrid ();

        // Perform the add form row, text, add, get children, and create group box calls required by the add rule
        // controls operation.

        ClientMain.addFormRow ( ruleGrid, 0, text ( "editor.rule.event" ), eventChoice );
        ClientMain.addFormRow (
            ruleGrid,
            1,
            text ( "editor.rule.condition.set" ),
            conditionSetChoice
        );
        ClientMain.addFormRow ( ruleGrid, 2, text ( "editor.rule.action" ), actionChoice );
        sections.getChildren ().add (
            ClientMain.createGroupBox ( text ( "editor.rule.group" ), ruleGrid )
        );

        // Return the add rule controls result to the caller.

        return () -> EntityDraft.rule (
            commonControls.identifierField.getText ().trim (),
            commonControls.nameField.getText ().trim (),
            commonControls.descriptionField.getText ().trim (),
            eventChoice.getValue (),
            conditionSetChoice.getValue (),
            actionChoice.getValue ()
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createBindingControls
    //
    // Description:
    //
    //   Performs the create binding controls operation.
    //
    // Arguments:
    //
    //   descriptor (ConditionBindingDescriptor):
    //     The descriptor to use.
    //
    //   stateChangeHandler (Runnable):
    //     The state change handler to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private BindingControls createBindingControls (
        ConditionBindingDescriptor descriptor,
        Runnable stateChangeHandler
    )
    {
        // Prepare the state choice, value controls, type label, and row values needed by the create binding controls
        // operation.

        ComboBox <BindingState> stateChoice = new ComboBox <BindingState> (
            FXCollections.observableArrayList (
                BindingState.CONCRETE,
                BindingState.WILDCARD,
                BindingState.OMITTED
            )
        );
        ConcreteValueControls valueControls = createConcreteValueControls ( descriptor );
        Label typeLabel = new Label (
            descriptor.getParameterType ().orElse ( text ( "editor.condition.set.unresolved" ) )
        );
        HBox row = new HBox ( 8.0, stateChoice, valueControls.node, typeLabel );

        // Perform the set value, get state, get binding, set converter, set pref width, set min width, add, get style
        // class, set hgrow, and set alignment calls required by the create binding controls operation.

        stateChoice.setValue ( descriptor.getBinding ().getState () );
        stateChoice.setConverter ( new BindingStateStringConverter ( this.resourceBundle ) );
        stateChoice.setPrefWidth ( 150.0 );
        typeLabel.setMinWidth ( 72.0 );
        typeLabel.getStyleClass ().add ( "secondary-text" );
        HBox.setHgrow ( valueControls.node, Priority.ALWAYS );
        row.setAlignment ( Pos.CENTER_LEFT );

        // Initialize the update control state by applying set disable, get value, and run.

        Runnable updateControlState = () ->
        {
            // Perform the set disable, get value, and run calls required by the create binding controls operation.

            valueControls.node.setDisable ( stateChoice.getValue () != BindingState.CONCRETE );
            stateChangeHandler.run ();
        };

        // Perform the add listener, value property, and run calls required by the create binding controls operation.

        stateChoice.valueProperty ().addListener (
            ( observableValue, oldState, newState ) -> updateControlState.run ()
        );
        updateControlState.run ();

        // Return a newly constructed binding controls containing the operation result.

        return new BindingControls (
            descriptor.getConditionIdentifier (),
            stateChoice,
            valueControls.textSupplier,
            row
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createConcreteValueControls
    //
    // Description:
    //
    //   Performs the create concrete value controls operation.
    //
    // Arguments:
    //
    //   descriptor (ConditionBindingDescriptor):
    //     The descriptor to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private ConcreteValueControls createConcreteValueControls (
        ConditionBindingDescriptor descriptor
    )
    {
        // Handle the branch where descriptor parameter type filter "enum"::equals is present.

        if ( descriptor.getParameterType ().filter ( "ENUM"::equals ).isPresent () )
        {
            // Initialize the value choice by applying observable array list and get enumeration values.

            ComboBox <String> valueChoice = new ComboBox <String> (
                FXCollections.observableArrayList ( descriptor.getEnumerationValues () )
            );

            // Perform the set editable, set text, get editor, get concrete text, get binding, and set max width calls
            // required by the create concrete value controls operation.

            valueChoice.setEditable ( true );
            valueChoice.getEditor ().setText ( descriptor.getBinding ().getConcreteText () );
            valueChoice.setMaxWidth ( Double.MAX_VALUE );

            // Return a newly constructed concrete value controls containing the operation result when descriptor
            // parameter type filter "enum"::equals is present.

            return new ConcreteValueControls (
                valueChoice,
                () -> valueChoice.getEditor ().getText ()
            );
        }

        // Handle the branch where descriptor parameter type filter "boolean"::equals is present.

        if ( descriptor.getParameterType ().filter ( "BOOLEAN"::equals ).isPresent () )
        {
            // Initialize the value choice by applying observable array list.

            ComboBox <String> valueChoice = new ComboBox <String> (
                FXCollections.observableArrayList ( "true", "false" )
            );

            // Perform the set editable, set text, get editor, get concrete text, get binding, and set max width calls
            // required by the create concrete value controls operation.

            valueChoice.setEditable ( true );
            valueChoice.getEditor ().setText ( descriptor.getBinding ().getConcreteText () );
            valueChoice.setMaxWidth ( Double.MAX_VALUE );

            // Return a newly constructed concrete value controls containing the operation result when descriptor
            // parameter type filter "boolean"::equals is present.

            return new ConcreteValueControls (
                valueChoice,
                () -> valueChoice.getEditor ().getText ()
            );
        }

        // Initialize the value field by applying get concrete text and get binding.

        TextField valueField = new TextField ( descriptor.getBinding ().getConcreteText () );

        // Set the max width on the value field.

        valueField.setMaxWidth ( Double.MAX_VALUE );

        // Return a newly constructed concrete value controls containing the operation result.

        return new ConcreteValueControls ( valueField, valueField::getText );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: editableValueChoice
    //
    // Description:
    //
    //   Performs the editable value choice operation.
    //
    // Arguments:
    //
    //   value (String):
    //     The value to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static ComboBox <String> editableValueChoice ( String value )
    {
        // Initialize the value choice with a new combo box.

        ComboBox <String> valueChoice = new ComboBox <String> ();

        // Perform the set editable, set max width, set text, and get editor calls required by the editable value
        // choice operation.

        valueChoice.setEditable ( true );
        valueChoice.setMaxWidth ( Double.MAX_VALUE );
        valueChoice.getEditor ().setText ( value );

        // Return the value choice to the caller.

        return valueChoice;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: findParameter
    //
    // Description:
    //
    //   Performs the find parameter operation.
    //
    // Arguments:
    //
    //   parameterIdentifier (String):
    //     The parameter identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private ConditionParameterDescriptor findParameter ( String parameterIdentifier )
    {
        // Return the result produced by or else.

        return this.modelEditor.getConditionParameterDescriptor (
            this.renderedSession.getContent (),
            parameterIdentifier
        )
            .orElse ( null );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: parseOperator
    //
    // Description:
    //
    //   Performs the parse operator operation.
    //
    // Arguments:
    //
    //   operator (String):
    //     The operator to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static ConditionOperatorDraft parseOperator ( String operator )
    {
        try
        {
            // Return the result produced by value of.

            return ConditionOperatorDraft.valueOf ( operator );
        }

        // Handle illegal argument or null pointer failures captured as exception.

        catch ( IllegalArgumentException | NullPointerException exception )
        {
            // Return the equals to the caller.

            return ConditionOperatorDraft.EQUALS;
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: configureValueChoice
    //
    // Description:
    //
    //   Performs the configure value choice operation.
    //
    // Arguments:
    //
    //   valueChoice (ComboBox <String>):
    //     The value choice to use.
    //
    //   parameter (ConditionParameterDescriptor):
    //     The parameter to use.
    //
    //   value (String):
    //     The value to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void configureValueChoice (
        ComboBox <String> valueChoice,
        ConditionParameterDescriptor parameter,
        String value
    )
    {
        // Initialize the choices by applying of.

        List <String> choices = List.of ();

        // Handle the branch where parameter is available and "boolean" matches parameter type.

        if ( parameter != null && "BOOLEAN".equals ( parameter.getParameterType () ) )
        {
            // Update the choices from the of result.

            choices = List.of ( "true", "false" );
        }

        // Handle the alternative where parameter is available and "enum" matches parameter type.

        else if ( parameter != null && "ENUM".equals ( parameter.getParameterType () ) )
        {
            // Update the choices from the get enumeration values result.

            choices = parameter.getEnumerationValues ();
        }

        // Perform the set items, observable array list, set text, and get editor calls required by the configure value
        // choice operation.

        valueChoice.setItems ( FXCollections.observableArrayList ( choices ) );
        valueChoice.getEditor ().setText ( value );
    }

    //=================================================================================================================
    // Mutators
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: setManagedVisible
    //
    // Description:
    //
    //   Sets the managed visible.
    //
    // Arguments:
    //
    //   node (Node):
    //     The node to use.
    //
    //   visible (boolean):
    //     The visible to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void setManagedVisible ( Node node, boolean visible )
    {
        // Configure the node with the required managed and visible values.

        node.setManaged ( visible );
        node.setVisible ( visible );
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: referenceChoice
    //
    // Description:
    //
    //   Performs the reference choice operation.
    //
    // Arguments:
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   field (String):
    //     The field to use.
    //
    //   selectedIdentifier (String):
    //     The selected identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private ComboBox <String> referenceChoice (
        String categoryIdentifier,
        String field,
        String selectedIdentifier
    )
    {
        // Initialize the identifiers by applying get compatible identifiers and get content.

        ArrayList <String> identifiers = new ArrayList <String> (
            this.modelEditor.getCompatibleIdentifiers (
                this.renderedSession.getContent (),
                categoryIdentifier,
                field
            )
        );

        // Handle the branch where selected identifier is available and identifiers does not contain selected
        // identifier.

        if ( selectedIdentifier != null && !identifiers.contains ( selectedIdentifier ) )
        {
            // Add selected identifier to the identifiers.

            identifiers.add ( selectedIdentifier );
        }

        // Initialize the choice by applying observable array list.

        ComboBox <String> choice = new ComboBox <String> (
            FXCollections.observableArrayList ( identifiers )
        );

        // Configure the choice with the required value and max width values.

        choice.setValue ( selectedIdentifier );
        choice.setMaxWidth ( Double.MAX_VALUE );

        // Return the choice to the caller.

        return choice;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: updateSpecificity
    //
    // Description:
    //
    //   Performs the update specificity operation.
    //
    // Arguments:
    //
    //   specificityValue (Label):
    //     The specificity value to use.
    //
    //   bindingControls (Collection <BindingControls>):
    //     The binding controls to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void updateSpecificity (
        Label specificityValue,
        Collection <BindingControls> bindingControls
    )
    {
        // Initialize the bindings by applying to list, map, and stream.

        List <BindingDraft> bindings = bindingControls.stream ()
            .map ( BindingControls::toDraft )
            .toList ();

        // Perform the set text, to string, and get specificity calls required by the update specificity operation.

        specificityValue.setText ( Integer.toString ( this.modelEditor.getSpecificity ( bindings ) ) );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: detailsColumnTitle
    //
    // Description:
    //
    //   Performs the details column title operation.
    //
    // Arguments:
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private String detailsColumnTitle ( String categoryIdentifier )
    {
        // Return the details column title result to the caller.

        return switch ( categoryIdentifier )
        {
            // Handle client model editor parameters through this switch branch.

            case ClientModelEditor.PARAMETERS     -> text ( "editor.column.type" );

            // Handle client model editor payloads through this switch branch.

            case ClientModelEditor.PAYLOADS       -> text ( "editor.column.parameter.count" );

            // Handle client model editor events through this switch branch.

            case ClientModelEditor.EVENTS         -> text ( "editor.column.payload" );

            // Handle client model editor conditions through this switch branch.

            case ClientModelEditor.CONDITIONS     -> text ( "editor.column.predicate" );

            // Handle client model editor condition sets through this switch branch.

            case ClientModelEditor.CONDITION_SETS -> text ( "editor.column.condition.count" );

            // Handle client model editor actions through this switch branch.

            case ClientModelEditor.ACTIONS        -> text ( "editor.column.details" );

            // Handle client model editor rules through this switch branch.

            case ClientModelEditor.RULES          -> text ( "editor.column.references" );

            // Handle the default case through this switch branch.

            default -> text ( "editor.column.details" );
        };
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: categoryName
    //
    // Description:
    //
    //   Performs the category name operation.
    //
    // Arguments:
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private String categoryName ( String categoryIdentifier )
    {
        // Return the result produced by text.

        return text ( "tree." + categoryIdentifier );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: text
    //
    // Description:
    //
    //   Performs the text operation.
    //
    // Arguments:
    //
    //   resourceKey (String):
    //     The resource key to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private String text ( String resourceKey )
    {
        // Return the result produced by get string.

        return this.resourceBundle.getString ( resourceKey );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: nonblankLines
    //
    // Description:
    //
    //   Performs the nonblank lines operation.
    //
    // Arguments:
    //
    //   text (String):
    //     The text to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static List <String> nonblankLines ( String text )
    {
        // Return the result produced by collect.

        return text.lines ()
            .map ( String::trim )
            .filter ( line -> !line.isEmpty () )
            .collect ( Collectors.toList () );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: selectedEntity
    //
    // Description:
    //
    //   Performs the selected entity operation.
    //
    // Arguments:
    //
    //   table (TableView <EntitySummary>):
    //     The table to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static java.util.Optional <EntitySummary> selectedEntity (
        TableView <EntitySummary> table
    )
    {
        // Return an optional containing the value when it is available.

        return java.util.Optional.ofNullable ( table.getSelectionModel ().getSelectedItem () );
    }

    //*****************************************************************************************************************
    // Class: CommonControls
    //
    // Description:
    //
    //   Provides the common controls behavior.
    //
    //*****************************************************************************************************************

    private static final class CommonControls
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final TextField identifierField;
        private final TextField nameField;
        private final TextArea descriptionField;

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: CommonControls
        //
        // Description:
        //
        //   Creates the CommonControls instance from the supplied values.
        //
        // Arguments:
        //
        //   identifierField (TextField):
        //     The identifier field to use.
        //
        //   nameField (TextField):
        //     The name field to use.
        //
        //   descriptionField (TextArea):
        //     The description field to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private CommonControls (
            TextField identifierField,
            TextField nameField,
            TextArea descriptionField
        )
        {
            this.identifierField  = identifierField;
            this.nameField        = nameField;
            this.descriptionField = descriptionField;
        }
    }

    //*****************************************************************************************************************
    // Class: ConditionSelectionControls
    //
    // Description:
    //
    //   Provides the condition selection controls behavior.
    //
    //*****************************************************************************************************************

    private static final class ConditionSelectionControls
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String conditionIdentifier;
        private final CheckBox conditionChoice;

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: ConditionSelectionControls
        //
        // Description:
        //
        //   Creates the ConditionSelectionControls instance from the supplied values.
        //
        // Arguments:
        //
        //   conditionIdentifier (String):
        //     The condition identifier to use.
        //
        //   conditionChoice (CheckBox):
        //     The condition choice to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private ConditionSelectionControls (
            String conditionIdentifier,
            CheckBox conditionChoice
        )
        {
            this.conditionIdentifier = conditionIdentifier;
            this.conditionChoice     = conditionChoice;
        }
    }

    //*****************************************************************************************************************
    // Class: BindingControls
    //
    // Description:
    //
    //   Provides the binding controls behavior.
    //
    //*****************************************************************************************************************

    private static final class BindingControls
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String conditionIdentifier;
        private final ComboBox <BindingState> stateChoice;
        private final Supplier <String> valueSupplier;
        private final HBox row;

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: BindingControls
        //
        // Description:
        //
        //   Creates the BindingControls instance from the supplied values.
        //
        // Arguments:
        //
        //   conditionIdentifier (String):
        //     The condition identifier to use.
        //
        //   stateChoice (ComboBox <BindingState>):
        //     The state choice to use.
        //
        //   valueSupplier (Supplier <String>):
        //     The value supplier to use.
        //
        //   row (HBox):
        //     The row to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private BindingControls (
            String conditionIdentifier,
            ComboBox <BindingState> stateChoice,
            Supplier <String> valueSupplier,
            HBox row
        )
        {
            this.conditionIdentifier = conditionIdentifier;
            this.stateChoice         = stateChoice;
            this.valueSupplier       = valueSupplier;
            this.row                 = row;
        }

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: toDraft
        //
        // Description:
        //
        //   Performs the to draft operation.
        //
        // Returns:
        //
        //   The result of the operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        private BindingDraft toDraft ()
        {
            // Return the to draft result to the caller.

            return switch ( this.stateChoice.getValue () )
            {
                // Handle concrete through this switch branch.

                case CONCRETE -> BindingDraft.concrete (
                    this.conditionIdentifier,
                    this.valueSupplier.get ()
                );

                // Handle wildcard through this switch branch.

                case WILDCARD -> BindingDraft.wildcard ( this.conditionIdentifier );

                // Handle omitted through this switch branch.

                case OMITTED  -> BindingDraft.omitted ( this.conditionIdentifier );
            };
        }
    }

    //*****************************************************************************************************************
    // Class: ConcreteValueControls
    //
    // Description:
    //
    //   Provides the concrete value controls behavior.
    //
    //*****************************************************************************************************************

    private static final class ConcreteValueControls
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final Node node;
        private final Supplier <String> textSupplier;

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: ConcreteValueControls
        //
        // Description:
        //
        //   Creates the ConcreteValueControls instance from the supplied values.
        //
        // Arguments:
        //
        //   node (Node):
        //     The node to use.
        //
        //   textSupplier (Supplier <String>):
        //     The text supplier to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private ConcreteValueControls ( Node node, Supplier <String> textSupplier )
        {
            this.node         = node;
            this.textSupplier = textSupplier;
        }
    }

    //*****************************************************************************************************************
    // Class: BindingStateStringConverter
    //
    // Description:
    //
    //   Provides the binding state string converter behavior.
    //
    //*****************************************************************************************************************

    private static final class BindingStateStringConverter extends StringConverter <BindingState>
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final ResourceBundle resourceBundle;

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: BindingStateStringConverter
        //
        // Description:
        //
        //   Creates the BindingStateStringConverter instance from the supplied values.
        //
        // Arguments:
        //
        //   resourceBundle (ResourceBundle):
        //     The resource bundle to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private BindingStateStringConverter ( ResourceBundle resourceBundle )
        {
            this.resourceBundle = resourceBundle;
        }

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: toString
        //
        // Description:
        //
        //   Creates the string representation of this value.
        //
        // Arguments:
        //
        //   state (BindingState):
        //     The state to use.
        //
        // Returns:
        //
        //   The string representation of this value.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public String toString ( BindingState state )
        {
            // Stop this path and return its result when state is unavailable.

            if ( state == null )
            {
                // Return the to string text to the caller when state is unavailable.

                return "";
            }

            // Return the to string result to the caller.

            return switch ( state )
            {
                // Handle concrete through this switch branch.

                case CONCRETE -> this.resourceBundle.getString ( "editor.binding.concrete" );

                // Handle wildcard through this switch branch.

                case WILDCARD -> this.resourceBundle.getString ( "editor.binding.wildcard" );

                // Handle omitted through this switch branch.

                case OMITTED  -> this.resourceBundle.getString ( "editor.binding.omitted" );
            };
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: fromString
        //
        // Description:
        //
        //   Performs the from string operation.
        //
        // Arguments:
        //
        //   value (String):
        //     The value to use.
        //
        // Returns:
        //
        //   The result of the operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public BindingState fromString ( String value )
        {
            // Process each state supplied by binding state values.

            for ( BindingState state : BindingState.values () )
            {
                // Stop this path and return its result when to string state matches value.

                if ( toString ( state ).equals ( value ) )
                {
                    // Return the state to the caller when to string state matches value.

                    return state;
                }
            }

            // Return the omitted to the caller.

            return BindingState.OMITTED;
        }
    }

    //*****************************************************************************************************************
    // Class: ConditionOperatorStringConverter
    //
    // Description:
    //
    //   Provides the condition operator string converter behavior.
    //
    //*****************************************************************************************************************

    private static final class ConditionOperatorStringConverter extends StringConverter <ConditionOperatorDraft>
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final ResourceBundle resourceBundle;

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: ConditionOperatorStringConverter
        //
        // Description:
        //
        //   Creates the ConditionOperatorStringConverter instance from the supplied values.
        //
        // Arguments:
        //
        //   resourceBundle (ResourceBundle):
        //     The resource bundle to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private ConditionOperatorStringConverter ( ResourceBundle resourceBundle )
        {
            this.resourceBundle = resourceBundle;
        }

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: toString
        //
        // Description:
        //
        //   Creates the string representation of this value.
        //
        // Arguments:
        //
        //   operator (ConditionOperatorDraft):
        //     The operator to use.
        //
        // Returns:
        //
        //   The string representation of this value.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public String toString ( ConditionOperatorDraft operator )
        {
            // Stop this path and return its result when operator is unavailable.

            if ( operator == null )
            {
                // Return the to string text to the caller when operator is unavailable.

                return "";
            }

            // Return the result produced by get string.

            return this.resourceBundle.getString (
                "editor.condition.operator." + operator.name ().toLowerCase ().replace ( '_', '.' )
            );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: fromString
        //
        // Description:
        //
        //   Performs the from string operation.
        //
        // Arguments:
        //
        //   value (String):
        //     The value to use.
        //
        // Returns:
        //
        //   The result of the operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public ConditionOperatorDraft fromString ( String value )
        {
            // Process each operator supplied by condition operator draft values.

            for ( ConditionOperatorDraft operator : ConditionOperatorDraft.values () )
            {
                // Stop this path and return its result when to string operator matches value.

                if ( toString ( operator ).equals ( value ) )
                {
                    // Return the operator to the caller when to string operator matches value.

                    return operator;
                }
            }

            // Return the equals to the caller.

            return ConditionOperatorDraft.EQUALS;
        }
    }
}
