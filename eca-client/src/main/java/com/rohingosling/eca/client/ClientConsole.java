//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2026
// Author:  Rohin Gosling
//
// Description:
//
//   Presents the bounded structured desktop Console with severity filters, follow-tail, clearing, and copy support.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.client;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

//*********************************************************************************************************************
// Class: ClientConsole
//
// Description:
//
//   Owns Console presentation state without coupling the application presenter to JavaFX table controls.
//
//*********************************************************************************************************************

final class ClientConsole
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    private static final int                 MAXIMUM_ENTRY_COUNT = 1_000;
    private static final DateTimeFormatter   TIME_FORMAT         = DateTimeFormatter.ofPattern ( "HH:mm:ss" );
    private static final AtomicLong          NEXT_IDENTIFIER     = new AtomicLong ();

    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final ObservableList <ConsoleEntry> entries;
    private final FilteredList <ConsoleEntry>   visibleEntries;
    private final CheckBox                      messagesFilter;
    private final CheckBox                      warningsFilter;
    private final CheckBox                      errorsFilter;
    private final CheckBox                      followTailFilter;
    private final TableView <ConsoleEntry>      table;
    private final VBox                          node;
    private final Consumer <Boolean>            followTailListener;

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    ClientConsole ( ResourceBundle resourceBundle, boolean followTail, Consumer <Boolean> followTailListener )
    {
        Objects.requireNonNull ( resourceBundle, "resourceBundle" );

        this.followTailListener = Objects.requireNonNull ( followTailListener, "followTailListener" );
        this.entries            = FXCollections.observableArrayList ();
        this.visibleEntries     = new FilteredList <> ( this.entries );
        this.messagesFilter     = createFilter ( resourceBundle.getString ( "console.filter.messages" ) );
        this.warningsFilter     = createFilter ( resourceBundle.getString ( "console.filter.warnings" ) );
        this.errorsFilter       = createFilter ( resourceBundle.getString ( "console.filter.errors" ) );
        this.followTailFilter   = createFilter ( resourceBundle.getString ( "console.follow.tail" ) );
        this.table              = createTable ( resourceBundle );

        this.followTailFilter.setSelected ( followTail );
        this.followTailFilter.selectedProperty ().addListener ( ( observableValue, previousValue, currentValue ) ->
        {
            this.followTailListener.accept ( currentValue );

            if ( currentValue )
            {
                scrollToTail ();
            }
        } );

        this.messagesFilter.selectedProperty ().addListener ( observableValue -> refreshFilter () );
        this.warningsFilter.selectedProperty ().addListener ( observableValue -> refreshFilter () );
        this.errorsFilter.selectedProperty ().addListener ( observableValue -> refreshFilter () );

        Button clearButton = new Button ( resourceBundle.getString ( "console.clear" ) );
        clearButton.setOnAction ( actionEvent -> clear () );

        Label title = new Label ( resourceBundle.getString ( "console.title" ) );
        title.getStyleClass ().add ( "console-title" );

        HBox controls = new HBox (
            10.0,
            this.messagesFilter,
            this.warningsFilter,
            this.errorsFilter,
            this.followTailFilter,
            clearButton
        );
        controls.setAlignment ( Pos.CENTER_RIGHT );
        controls.getStyleClass ().add ( "console-controls" );

        HBox titleBar = new HBox ( 8.0, title, controls );
        titleBar.setAlignment ( Pos.CENTER_LEFT );
        titleBar.setPadding ( new Insets ( 3.0, 7.0, 3.0, 9.0 ) );
        titleBar.getStyleClass ().add ( "console-title-bar" );
        HBox.setHgrow ( controls, Priority.ALWAYS );

        this.node = new VBox ( titleBar, this.table );
        this.node.getStyleClass ().add ( "console-panel" );
        VBox.setVgrow ( this.table, Priority.ALWAYS );
    }

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    Node getNode ()
    {
        return this.node;
    }

    boolean isFollowTail ()
    {
        return this.followTailFilter.isSelected ();
    }

    void setFollowTail ( boolean followTail )
    {
        this.followTailFilter.setSelected ( followTail );
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    void publish (
        ClientView.MessageSeverity severity,
        String code,
        String source,
        String text,
        String context,
        Runnable contextAction
    )
    {
        Objects.requireNonNull ( severity, "severity" );

        ConsoleEntry entry = new ConsoleEntry (
            NEXT_IDENTIFIER.incrementAndGet (),
            LocalTime.now (),
            ConsoleSeverity.fromMessageSeverity ( severity ),
            valueOrEmpty ( code ),
            valueOrEmpty ( source ),
            valueOrEmpty ( text ),
            valueOrEmpty ( context ),
            contextAction
        );

        this.entries.add ( entry );

        if ( this.entries.size () > MAXIMUM_ENTRY_COUNT )
        {
            this.entries.remove ( 0, this.entries.size () - MAXIMUM_ENTRY_COUNT );
        }

        if ( this.table.getSelectionModel ().isEmpty () && !this.visibleEntries.isEmpty () )
        {
            this.table.getSelectionModel ().selectFirst ();
        }

        if ( isFollowTail () )
        {
            scrollToTail ();
        }
    }

    void clear ()
    {
        this.entries.clear ();
    }

    private CheckBox createFilter ( String label )
    {
        CheckBox filter = new CheckBox ( label );
        filter.setSelected ( true );

        return filter;
    }

    private TableView <ConsoleEntry> createTable ( ResourceBundle resourceBundle )
    {
        TableView <ConsoleEntry> consoleTable = new TableView <> ( this.visibleEntries );

        consoleTable.setColumnResizePolicy ( TableView.UNCONSTRAINED_RESIZE_POLICY );
        consoleTable.setFixedCellSize ( 27.0 );
        consoleTable.setPlaceholder ( new Label ( resourceBundle.getString ( "console.empty" ) ) );
        consoleTable.getSelectionModel ().setSelectionMode ( SelectionMode.SINGLE );
        consoleTable.getStyleClass ().add ( "console-table" );
        consoleTable.setOnKeyPressed ( this::handleTableKeyPressed );
        consoleTable.setRowFactory ( ignoredTable -> createTableRow () );

        TableColumn <ConsoleEntry, String> severityColumn = createColumn (
            resourceBundle.getString ( "console.column.severity" ),
            92.0,
            entry -> resourceBundle.getString ( "console.severity." + entry.getSeverity ().getIdentifier () )
        );
        severityColumn.setCellFactory ( ignoredColumn -> createSeverityCell () );

        TableColumn <ConsoleEntry, String> contextColumn = createColumn (
            resourceBundle.getString ( "console.column.context" ),
            120.0,
            ConsoleEntry::getContext
        );
        contextColumn.setCellFactory ( ignoredColumn -> createContextCell () );
        TableColumn <ConsoleEntry, String> messageColumn = createColumn (
            resourceBundle.getString ( "console.column.text" ),
            280.0,
            ConsoleEntry::getText
        );

        messageColumn.setMinWidth ( 220.0 );
        messageColumn.prefWidthProperty ().bind (
            Bindings.createDoubleBinding (
                () -> Math.max ( 220.0, consoleTable.getWidth () - 570.0 ),
                consoleTable.widthProperty ()
            )
        );

        consoleTable.getColumns ().add (
            createColumn ( resourceBundle.getString ( "console.column.time" ), 72.0, entry -> entry.formattedTime () )
        );
        consoleTable.getColumns ().add ( severityColumn );
        consoleTable.getColumns ().add (
            createColumn ( resourceBundle.getString ( "console.column.code" ), 150.0, ConsoleEntry::getCode )
        );
        consoleTable.getColumns ().add (
            createColumn ( resourceBundle.getString ( "console.column.source" ), 108.0, ConsoleEntry::getSource )
        );
        consoleTable.getColumns ().add ( messageColumn );
        consoleTable.getColumns ().add ( contextColumn );

        return consoleTable;
    }

    private TableColumn <ConsoleEntry, String> createColumn (
        String label,
        double preferredWidth,
        java.util.function.Function <ConsoleEntry, String> valueProvider
    )
    {
        TableColumn <ConsoleEntry, String> column = new TableColumn <> ( label );

        column.setCellValueFactory ( cellData -> new ReadOnlyStringWrapper ( valueProvider.apply ( cellData.getValue () ) ) );
        column.setMinWidth ( Math.min ( preferredWidth, 72.0 ) );
        column.setPrefWidth ( preferredWidth );
        column.setReorderable ( false );

        return column;
    }

    private TableRow <ConsoleEntry> createTableRow ()
    {
        TableRow <ConsoleEntry> row = new TableRow <> ();

        row.itemProperty ().addListener ( ( observableValue, previousEntry, currentEntry ) ->
        {
            row.getStyleClass ().removeAll (
                "console-row-message",
                "console-row-warning",
                "console-row-error"
            );

            if ( currentEntry != null )
            {
                row.getStyleClass ().add ( "console-row-" + currentEntry.getSeverity ().getIdentifier () );
            }
        } );
        row.setOnMouseClicked ( mouseEvent ->
        {
            if ( mouseEvent.getButton () == MouseButton.PRIMARY && mouseEvent.getClickCount () == 2
                && !row.isEmpty () )
            {
                row.getItem ().activateContext ();
            }
        } );

        return row;
    }

    private TableCell <ConsoleEntry, String> createContextCell ()
    {
        return new TableCell <ConsoleEntry, String> ()
        {
            @Override
            protected void updateItem ( String context, boolean empty )
            {
                super.updateItem ( context, empty );

                ConsoleEntry entry = getTableRow () == null ? null : getTableRow ().getItem ();

                if ( empty || context == null || context.isBlank () || entry == null )
                {
                    setText ( null );
                    setGraphic ( null );
                    setAccessibleText ( null );
                }
                else
                {
                    Button contextButton = new Button ( context );
                    contextButton.setMinWidth ( Region.USE_PREF_SIZE );
                    contextButton.setMaxWidth ( Region.USE_PREF_SIZE );
                    contextButton.setOnAction ( actionEvent -> entry.activateContext () );
                    contextButton.getStyleClass ().add ( "console-context-button" );
                    double requiredContextColumnWidth = Math.ceil (
                        contextButton.prefWidth ( Region.USE_COMPUTED_SIZE )
                    ) + 16.0;

                    getTableColumn ().setPrefWidth (
                        Math.max ( getTableColumn ().getPrefWidth (), requiredContextColumnWidth )
                    );
                    setText ( null );
                    setGraphic ( contextButton );
                }
            }
        };
    }

    private TableCell <ConsoleEntry, String> createSeverityCell ()
    {
        return new TableCell <ConsoleEntry, String> ()
        {
            @Override
            protected void updateItem ( String severity, boolean empty )
            {
                super.updateItem ( severity, empty );
                getStyleClass ().removeAll (
                    "console-severity-message",
                    "console-severity-warning",
                    "console-severity-error"
                );

                ConsoleEntry entry = getTableRow () == null ? null : getTableRow ().getItem ();

                if ( empty || severity == null || entry == null )
                {
                    setText ( null );
                    setGraphic ( null );
                    setAccessibleText ( null );
                }
                else
                {
                    Label severityBadge = new Label ( entry.getSeverity ().getSymbol () );
                    severityBadge.getStyleClass ().add ( "console-severity-symbol" );

                    Label severityName = new Label ( severity );
                    HBox severityContent = new HBox ( 6.0, severityBadge, severityName );
                    severityContent.setAlignment ( Pos.CENTER_LEFT );
                    severityContent.getStyleClass ().add ( "console-severity-content" );

                    setText ( null );
                    setGraphic ( severityContent );
                    setAccessibleText ( entry.getSeverity ().getSymbol () + " " + severity );
                    getStyleClass ().add ( "console-severity-" + entry.getSeverity ().getIdentifier () );
                }
            }
        };
    }

    private void handleTableKeyPressed ( KeyEvent keyEvent )
    {
        if ( keyEvent.getCode () == KeyCode.C && keyEvent.isShortcutDown () )
        {
            ConsoleEntry selectedEntry = this.table.getSelectionModel ().getSelectedItem ();

            if ( selectedEntry != null )
            {
                ClipboardContent clipboardContent = new ClipboardContent ();
                clipboardContent.putString ( selectedEntry.copyText () );
                Clipboard.getSystemClipboard ().setContent ( clipboardContent );
                keyEvent.consume ();
            }
        }
        else if ( keyEvent.getCode () == KeyCode.ENTER || keyEvent.getCode () == KeyCode.SPACE )
        {
            ConsoleEntry selectedEntry = this.table.getSelectionModel ().getSelectedItem ();

            if ( selectedEntry != null && selectedEntry.activateContext () )
            {
                keyEvent.consume ();
            }
        }
    }

    private void refreshFilter ()
    {
        ConsoleEntry selectedEntry = this.table.getSelectionModel ().getSelectedItem ();

        this.visibleEntries.setPredicate ( entry -> switch ( entry.getSeverity () )
        {
            case MESSAGE -> this.messagesFilter.isSelected ();
            case WARNING -> this.warningsFilter.isSelected ();
            case ERROR   -> this.errorsFilter.isSelected ();
        } );

        if ( selectedEntry != null && this.visibleEntries.contains ( selectedEntry ) )
        {
            this.table.getSelectionModel ().select ( selectedEntry );
        }
        else if ( !this.visibleEntries.isEmpty () )
        {
            this.table.getSelectionModel ().selectFirst ();
        }
        else
        {
            this.table.getSelectionModel ().clearSelection ();
        }

        if ( isFollowTail () )
        {
            scrollToTail ();
        }
    }

    private void scrollToTail ()
    {
        Platform.runLater ( () ->
        {
            if ( !this.visibleEntries.isEmpty () )
            {
                this.table.scrollTo ( this.visibleEntries.size () - 1 );
            }
        } );
    }

    private String valueOrEmpty ( String value )
    {
        return value == null ? "" : value;
    }

    //=================================================================================================================
    // User Defined Data Types
    //=================================================================================================================

    private enum ConsoleSeverity
    {
        MESSAGE ( "message" ),
        WARNING ( "warning" ),
        ERROR   ( "error"   );

        private final String identifier;

        ConsoleSeverity ( String identifier )
        {
            this.identifier = identifier;
        }

        String getIdentifier ()
        {
            return this.identifier;
        }

        String getSymbol ()
        {
            return switch ( this )
            {
                case MESSAGE -> "M";
                case WARNING -> "W";
                case ERROR   -> "E";
            };
        }

        static ConsoleSeverity fromMessageSeverity ( ClientView.MessageSeverity messageSeverity )
        {
            return switch ( messageSeverity )
            {
                case WARNING -> WARNING;
                case ERROR   -> ERROR;
                case DEBUG, INFO -> MESSAGE;
            };
        }
    }

    private static final class ConsoleEntry
    {
        private final long            identifier;
        private final LocalTime       timestamp;
        private final ConsoleSeverity severity;
        private final String          code;
        private final String          source;
        private final String          text;
        private final String          context;
        private final Runnable        contextAction;

        ConsoleEntry (
            long identifier,
            LocalTime timestamp,
            ConsoleSeverity severity,
            String code,
            String source,
            String text,
            String context,
            Runnable contextAction
        )
        {
            this.identifier    = identifier;
            this.timestamp     = timestamp;
            this.severity      = severity;
            this.code          = code;
            this.source        = source;
            this.text          = text;
            this.context       = context;
            this.contextAction = contextAction;
        }

        ConsoleSeverity getSeverity ()
        {
            return this.severity;
        }

        String getCode ()
        {
            return this.code;
        }

        String getSource ()
        {
            return this.source;
        }

        String getText ()
        {
            return this.text;
        }

        String getContext ()
        {
            return this.context;
        }

        String formattedTime ()
        {
            return TIME_FORMAT.format ( this.timestamp );
        }

        String copyText ()
        {
            return formattedTime () + "\t" + this.severity.getIdentifier () + "\t" + this.code + "\t"
                + this.source + "\t" + this.text;
        }

        boolean activateContext ()
        {
            if ( this.context.isBlank () || this.contextAction == null )
            {
                return false;
            }

            this.contextAction.run ();

            return true;
        }

        @Override
        public boolean equals ( Object otherObject )
        {
            return otherObject instanceof ConsoleEntry otherEntry && this.identifier == otherEntry.identifier;
        }

        @Override
        public int hashCode ()
        {
            return Long.hashCode ( this.identifier );
        }
    }
}
