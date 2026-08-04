//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Supplies the production JavaFX SDI document shell and its Model-View-Presenter view adapter.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.client;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import com.rohingosling.eca.application.ClientDocumentSession;
import com.rohingosling.eca.application.ClientDocumentSession.Selection;
import com.rohingosling.eca.application.ClientDocumentSession.Summary;
import com.rohingosling.eca.application.ClientEvaluationResult;
import com.rohingosling.eca.application.ClientSimulator.EventDescriptor;
import com.rohingosling.eca.application.ClientSimulator.PayloadFieldDescriptor;
import com.rohingosling.eca.application.ClientSimulator.PayloadValueDraft;
import com.rohingosling.eca.application.ClientSimulator.PayloadValueState;
import com.rohingosling.eca.json.AuthoringModelClientDocumentCodec;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.StringConverter;

//*********************************************************************************************************************
// Class: ClientMain
//
// Description:
//
//   Supplies the production JavaFX SDI document shell and its Model-View-Presenter view adapter.
//
//*********************************************************************************************************************

public final class ClientMain extends Application implements ClientView
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    public static final String GITHUB_URL = "https://github.com/rohingosling/eca-rule-engine-2";
    public static final String TECHNICAL_NOTE_URL =
        "https://github.com/rohingosling/eca-rule-engine-2/blob/main/docs/technical-note/"
            + "stateless-eca-rule-engine.pdf";

    private static final String APPLICATION_ICON_RESOURCE = "eca-rule-engine.png";
    private static final String NATIVE_SMOKE_PARAMETER              = "native-smoke";
    private static final String NATIVE_SMOKE_FILE_CHOOSER_PARAMETER = "native-smoke-file-chooser";
    private static final String NATIVE_SMOKE_PHASE_PARAMETER        = "native-smoke-phase";
    private static final String NATIVE_SMOKE_RESTORE_THEME_PARAMETER = "native-smoke-restore-theme";
    private static final String NATIVE_SMOKE_SERVER_URL_PARAMETER   = "native-smoke-server-url";
    private static final String NATIVE_SMOKE_THEME_PARAMETER        = "native-smoke-theme";
    private static final String NATIVE_SMOKE_TLS_URL_PARAMETER      = "native-smoke-tls-url";
    private static final String NATIVE_SMOKE_UI_AUTOMATION_PARAMETER = "native-smoke-ui-automation";

    private static final double            FORM_LABEL_MINIMUM_WIDTH = 160.0;
    private static final DateTimeFormatter MESSAGE_TIME_FORMAT      =
        DateTimeFormatter.ofPattern ( "HH:mm:ss" );

    private static final List <String> CATEGORY_IDENTIFIERS = List.of (
        "parameters",
        "payloads",
        "events",
        "conditions",
        "condition-sets",
        "actions",
        "rules"
    );

    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private ResourceBundle            resourceBundle;
    private ClientPreferencesStore    preferencesStore;
    private ClientPresenter           presenter;
    private ClientEntityEditor        entityEditor;
    private ClientDocumentSession     renderedSession;
    private Stage                     primaryStage;
    private BorderPane                applicationRoot;
    private SplitPane                 workspaceSplitPane;
    private SplitPane                 masterDetailSplitPane;
    private TreeView <NavigationItem> modelTree;
    private TreeItem <NavigationItem> modelRootItem;
    private TabPane                   detailTabs;
    private Tab                       editorTab;
    private Tab                       simulatorTab;
    private Label                     editorHeadingLabel;
    private TextField                 modelIdentifierValue;
    private TextField                 modelNameValue;
    private TextArea                  modelDescriptionValue;
    private Button                    applyModelDetailsButton;
    private Node                      modelOverviewNode;
    private Node                      entityEditorNode;
    private VBox                      editorDetailContent;
    private TextArea                  messageTerminal;
    private Label                     userGuideOverviewValue;
    private Label                     userGuideStepsValue;
    private ImageView                 userGuideEquationImage;
    private Map <Theme, Map <ClientUserGuide.Context, Image>> userGuideEquationImages;
    private Theme                     currentTheme;
    private Label                     documentStatusLabel;
    private Label                     validationStatusLabel;
    private Label                     connectionStatusLabel;
    private Label                     operationStatusLabel;
    private ProgressIndicator         progressIndicator;
    private Button                    cancelOperationButton;
    private ComboBox <EventDescriptor> simulatorEventSelector;
    private GridPane                  simulatorPayloadGrid;
    private Label                     simulatorAvailabilityLabel;
    private Label                     simulatorEmptyPayloadLabel;
    private Button                    evaluateButton;
    private Label                     evaluationOutcomeValue;
    private Label                     evaluationActionValue;
    private Label                     evaluationRuleValue;
    private Label                     evaluationSpecificityValue;
    private Label                     evaluationServerLatencyValue;
    private Label                     evaluationRoundTripLatencyValue;
    private Label                     localRevisionValue;
    private Label                     serverRevisionValue;
    private Label                     revisionReconciliationValue;
    private Map <String, SimulatorPayloadControl> simulatorPayloadControls;
    private ClientDocumentSession.Content        simulatorRenderedContent;
    private MenuItem                  saveMenuItem;
    private MenuItem                  saveAsMenuItem;
    private MenuItem                  undoMenuItem;
    private MenuItem                  redoMenuItem;
    private MenuItem                  pushMenuItem;
    private MenuItem                  deleteMenuItem;
    private Map <Theme, RadioMenuItem> themeMenuItems;
    private List <MenuItem>           operationSensitiveMenuItems;
    private boolean                   backgroundOperationRunning;
    private boolean                   rendering;
    private boolean                   exitAuthorized;
    private Set <String>              preferredExpandedCategories;

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: main
    //
    // Description:
    //
    //   Starts the application using the supplied command-line arguments.
    //
    // Arguments:
    //
    //   arguments (String[]):
    //     The command-line arguments.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static void main ( String[] arguments )
    {
        // Complete the main step by calling launch.

        launch ( arguments );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: start
    //
    // Description:
    //
    //   Performs the start operation.
    //
    // Arguments:
    //
    //   stage (Stage):
    //     The stage to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public void start ( Stage stage )
    {
        // Perform the get bundle and get default calls required by the start operation.

        this.resourceBundle = ResourceBundle.getBundle (
            "com.rohingosling.eca.client.messages",
            Locale.getDefault ()
        );
        this.preferencesStore = new ClientPreferencesStore ();
        this.primaryStage     = stage;

        // Update the current theme from the load theme identifier result.

        this.currentTheme     = Theme.fromIdentifier (
            this.preferencesStore.loadThemeIdentifier ()
        );

        // Initialize the client document codec with a new authoring model client document codec.

        AuthoringModelClientDocumentCodec clientDocumentCodec = new AuthoringModelClientDocumentCodec ();

        // Complete the start step by calling load connection settings.

        this.presenter = new ClientPresenter (
            this,
            new ClientDocumentFileService ( clientDocumentCodec ),
            new ClientServerGateway ( clientDocumentCodec ),
            new ClientDocumentSession (),
            this.preferencesStore.loadConnectionSettings ()
        );

        this.applicationRoot = new BorderPane ();

        // Perform the add, get style class, set top, create menu bar, set center, create workspace, set bottom, and
        // create status bar calls required by the start operation.

        this.applicationRoot.getStyleClass ().add ( this.currentTheme.getStyleClass () );
        this.applicationRoot.setTop ( createMenuBar () );
        this.applicationRoot.setCenter ( createWorkspace () );
        this.applicationRoot.setBottom ( createStatusBar () );

        // Prepare the window state and scene values needed by the start operation.

        ClientPreferencesStore.WindowState windowState = this.preferencesStore.loadWindowState ();
        Scene scene = new Scene (
            this.applicationRoot,
            windowState.getWidth (),
            windowState.getHeight ()
        );

        // Update the preferred expanded categories from the get expanded categories result.

        this.preferredExpandedCategories = windowState.getExpandedCategories ();

        // Perform the add, get stylesheets, to external form, get resource, set scene, set min width, set min height,
        // and get icons calls required by the start operation.

        scene.getStylesheets ().add ( ClientMain.class.getResource ( "client.css" ).toExternalForm () );

        stage.setScene ( scene );
        stage.setMinWidth ( 1_000.0 );
        stage.setMinHeight ( 560.0 );
        stage.getIcons ().add (
            new Image (
                ClientMain.class.getResource ( APPLICATION_ICON_RESOURCE ).toExternalForm (),
                false
            )
        );

        // Handle the branch where the operation is visible window position.

        if ( isVisibleWindowPosition ( windowState ) )
        {
            // Perform the set x, get x, set y, and get y calls required by the start operation.

            stage.setX ( windowState.getX () );
            stage.setY ( windowState.getY () );
        }

        // Perform the set divider positions, get master detail splitter position, get context-sensitive help splitter
        // position,
        // get diagnostics splitter position, set expanded, is tree expanded, set on close request, consume, request
        // exit, show, start, if present, native smoke result path, run later, and run native smoke calls required by
        // the start operation.

        this.masterDetailSplitPane.setDividerPositions (
            windowState.getMasterDetailSplitterPosition (),
            windowState.getUserGuideSplitterPosition ()
        );
        this.workspaceSplitPane.setDividerPositions ( windowState.getDiagnosticsSplitterPosition () );
        this.modelRootItem.setExpanded ( windowState.isTreeExpanded () );
        stage.setOnCloseRequest (
            event ->
            {
                // Handle the branch where exit authorized is false.

                if ( !this.exitAuthorized )
                {
                    // Perform the consume and request exit calls required by the start operation.

                    event.consume ();
                    this.presenter.requestExit ();
                }
            }
        );
        stage.show ();
        this.presenter.start ();

        nativeSmokeResultPath ().ifPresent (
            resultPath -> Platform.runLater ( () -> runNativeSmoke ( resultPath ) )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: nativeSmokeResultPath
    //
    // Description:
    //
    //   Performs the native smoke result path operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private Optional <Path> nativeSmokeResultPath ()
    {
        // Initialize the result path by applying get, get named, and get parameters.

        String resultPath = getParameters ().getNamed ().get ( NATIVE_SMOKE_PARAMETER );

        // Stop this path and return its result when result path is unavailable or result path is blank.

        if ( resultPath == null || resultPath.isBlank () )
        {
            // Return an empty optional result because no value is available when result path is unavailable or result
            // path is blank.

            return Optional.empty ();
        }

        // Return an optional containing the available value.

        return Optional.of ( Path.of ( resultPath ).toAbsolutePath ().normalize () );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: runNativeSmoke
    //
    // Description:
    //
    //   Performs the run native smoke operation.
    //
    // Arguments:
    //
    //   resultPath (Path):
    //     The result path to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void runNativeSmoke ( Path resultPath )
    {
        try
        {
            Map <String, String> namedParameters = getParameters ().getNamed ();
            String smokePhase = namedParameters.getOrDefault ( NATIVE_SMOKE_PHASE_PARAMETER, "exercise" );

            if ( smokePhase.equals ( "read-preferences" ) )
            {
                Theme expectedTheme = Theme.fromIdentifier (
                    requiredNativeSmokeParameter ( namedParameters, NATIVE_SMOKE_THEME_PARAMETER )
                );
                Theme restoredTheme = Theme.fromIdentifier (
                    requiredNativeSmokeParameter ( namedParameters, NATIVE_SMOKE_RESTORE_THEME_PARAMETER )
                );

                if ( this.currentTheme != expectedTheme )
                {
                    throw new IllegalStateException ( "The native client did not restore its persisted theme." );
                }

                this.applicationRoot.applyCss ();
                this.applicationRoot.layout ();
                this.applicationRoot.snapshot ( null, null );
                applyTheme ( restoredTheme );
                this.preferencesStore.flush ();

                Files.writeString (
                    resultPath,
                    "PASS" + System.lineSeparator (),
                    StandardCharsets.UTF_8
                );
                Platform.exit ();

                return;
            }

            // Reject the operation when primary stage is not showing.

            if ( !this.primaryStage.isShowing () )
            {
                throw new IllegalStateException ( "The primary stage is not showing." );
            }

            // Reject the operation when primary stage icons contains no values.

            if ( this.primaryStage.getIcons ().isEmpty () )
            {
                throw new IllegalStateException ( "The application icon was not loaded." );
            }

            // Reject the operation when primary stage scene stylesheets contains no values.

            if ( this.primaryStage.getScene ().getStylesheets ().isEmpty () )
            {
                throw new IllegalStateException ( "The application stylesheet was not loaded." );
            }

            // Reject the operation when resource bundle contains key does not succeed or resource bundle contains key
            // does not succeed.

            if (
                !this.resourceBundle.containsKey ( "about.title" ) ||
                !this.resourceBundle.containsKey ( "dialog.error.unknown" )
            )
            {
                throw new IllegalStateException ( "The client resource bundle is incomplete." );
            }

            // Initialize the file chooser by applying create file chooser and text.

            FileChooser fileChooser = createFileChooser ( text ( "file.open.title" ) );

            // Reject the operation when file chooser extension filters contains no values.

            if ( fileChooser.getExtensionFilters ().isEmpty () )
            {
                throw new IllegalStateException ( "The JSON file chooser filter was not created." );
            }

            // Prepare the error dialog and generic dialog values needed by the run native smoke operation.

            Alert errorDialog = new Alert (
                Alert.AlertType.ERROR,
                text ( "dialog.error.unknown" ),
                ButtonType.OK
            );
            Dialog <ButtonType> genericDialog = new Dialog <>();

            // Perform the set title, text, add, get button types, get dialog pane, and apply css calls required by the
            // run native smoke operation.

            errorDialog.initOwner ( this.primaryStage );
            genericDialog.initOwner ( this.primaryStage );
            genericDialog.setTitle ( text ( "about.title" ) );
            genericDialog.getDialogPane ().getButtonTypes ().add ( ButtonType.CLOSE );
            errorDialog.getDialogPane ().applyCss ();
            genericDialog.getDialogPane ().applyCss ();

            Platform.runLater ( errorDialog::close );
            errorDialog.showAndWait ();

            if (
                !Boolean.parseBoolean (
                    namedParameters.getOrDefault ( NATIVE_SMOKE_UI_AUTOMATION_PARAMETER, "false" )
                )
            )
            {
                Platform.runLater ( genericDialog::close );
            }

            genericDialog.showAndWait ();

            if (
                Boolean.parseBoolean (
                    namedParameters.getOrDefault ( NATIVE_SMOKE_FILE_CHOOSER_PARAMETER, "false" )
                ) &&
                fileChooser.showOpenDialog ( this.primaryStage ) != null
            )
            {
                throw new IllegalStateException ( "The automated native file chooser was expected to be cancelled." );
            }

            // Prepare the HTTP client, server request, server response, TLS request, and TLS response values needed by
            // the run native smoke operation.

            HttpClient httpClient = HttpClient.newBuilder ()
                .connectTimeout ( Duration.ofSeconds ( 5 ) )
                .followRedirects ( HttpClient.Redirect.NORMAL )
                .build ();
            URI serverBaseUri = URI.create (
                requiredNativeSmokeParameter ( namedParameters, NATIVE_SMOKE_SERVER_URL_PARAMETER )
            );
            HttpRequest serverRequest = HttpRequest.newBuilder (
                serverBaseUri.resolve ( "/api/v1/health/live" )
            )
                .timeout ( Duration.ofSeconds ( 5 ) )
                .GET ()
                .build ();
            HttpResponse <String> serverResponse = httpClient.sendAsync (
                serverRequest,
                HttpResponse.BodyHandlers.ofString ()
            )
                .orTimeout ( 10, java.util.concurrent.TimeUnit.SECONDS )
                .join ();

            if ( serverResponse.statusCode () != 200 || !serverResponse.body ().contains ( "UP" ) )
            {
                throw new IllegalStateException ( "The native client could not interoperate with the native server." );
            }

            HttpRequest tlsRequest = HttpRequest.newBuilder (
                URI.create ( requiredNativeSmokeParameter ( namedParameters, NATIVE_SMOKE_TLS_URL_PARAMETER ) )
            )
                .timeout ( Duration.ofSeconds ( 10 ) )
                .GET ()
                .build ();
            HttpResponse <Void> tlsResponse = httpClient.sendAsync (
                tlsRequest,
                HttpResponse.BodyHandlers.discarding ()
            )
                .orTimeout ( 20, java.util.concurrent.TimeUnit.SECONDS )
                .join ();

            if ( tlsResponse.statusCode () < 100 || tlsResponse.statusCode () > 599 )
            {
                throw new IllegalStateException ( "The native client TLS round trip failed." );
            }

            // Perform the select, get selection model, apply css, layout, snapshot, write string, line separator, and
            // exit calls required by the run native smoke operation.

            this.detailTabs.getSelectionModel ().select ( this.simulatorTab );
            this.applicationRoot.applyCss ();
            this.applicationRoot.layout ();
            this.applicationRoot.snapshot ( null, null );
            this.detailTabs.getSelectionModel ().select ( this.editorTab );

            for ( Node formLabelNode : this.applicationRoot.lookupAll ( ".form-label" ) )
            {
                if ( !( formLabelNode instanceof Label formLabel ) || formLabel.getLabelFor () == null )
                {
                    throw new IllegalStateException ( "A native form label is missing its accessibility target." );
                }
            }

            Theme originalTheme  = this.currentTheme;
            Theme persistedTheme = originalTheme == Theme.LIGHT ? Theme.DARK : Theme.LIGHT;

            applyTheme ( persistedTheme );
            this.applicationRoot.applyCss ();
            this.applicationRoot.layout ();
            this.applicationRoot.snapshot ( null, null );
            applyTheme ( originalTheme );
            this.preferencesStore.saveThemeIdentifier ( persistedTheme.getIdentifier () );
            this.preferencesStore.flush ();

            Files.writeString (
                resultPath,
                "PASS|" + originalTheme.getIdentifier () + "|" + persistedTheme.getIdentifier ()
                    + System.lineSeparator (),
                StandardCharsets.UTF_8
            );
            Platform.exit ();
        }

        // Handle I/O or runtime failures captured as exception.

        catch ( IOException | RuntimeException exception )
        {
            try
            {
                // Perform the write string and line separator calls required by the run native smoke operation.

                Files.writeString (
                    resultPath,
                    "FAIL: " + exception + System.lineSeparator (),
                    StandardCharsets.UTF_8
                );
            }

            // Handle I/O failures captured as ignored exception.

            catch ( IOException ignoredException )
            {
                // Complete the run native smoke step by calling add suppressed.

                exception.addSuppressed ( ignoredException );
            }

            // Perform the print stack trace and exit calls required by the run native smoke operation.

            exception.printStackTrace ();
            System.exit ( 1 );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requiredNativeSmokeParameter
    //
    // Description:
    //
    //   Returns a required named native-smoke parameter.
    //
    // Arguments:
    //
    //   namedParameters (Map<String, String>):
    //     The named application parameters.
    //
    //   parameterName (String):
    //     The required parameter name.
    //
    // Returns:
    //
    //   The required parameter value.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String requiredNativeSmokeParameter (
        Map <String, String> namedParameters,
        String parameterName
    )
    {
        String parameterValue = namedParameters.get ( parameterName );

        if ( parameterValue == null || parameterValue.isBlank () )
        {
            throw new IllegalArgumentException ( "Missing native-smoke parameter: " + parameterName );
        }

        return parameterValue;
    }

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

    @Override
    public void dispatch ( Runnable runnable )
    {
        // Schedule the UI update on the JavaFX application thread.

        Platform.runLater ( runnable );
    }

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

    @Override
    public void render ( ClientDocumentSession session )
    {
        this.renderedSession = session;
        this.rendering       = true;

        try
        {
            // Perform the set title, get window title, render navigation tree, render document overview, render editor
            // detail, render simulator, render status, render selection, get selection, render context-sensitive help,
            // and render
            // command state calls required by the render operation.

            this.primaryStage.setTitle ( session.getWindowTitle () );
            renderNavigationTree ( session );
            renderDocumentOverview ( session );
            renderEditorDetail ( session );
            renderSimulator ( session );
            renderStatus ( session );
            renderSelection ( session.getSelection () );
            renderUserGuide ();
            renderCommandState ( session );
        }

        // Complete the required cleanup regardless of how the protected operation finishes.

        finally
        {
            this.rendering = false;
        }
    }

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

    @Override
    public void reportValidation ( ClientDocumentSession session )
    {
        // Handle the branch where session content validation messages contains no values.

        if ( session.getContent ().getValidationMessages ().isEmpty () )
        {
            // Perform the report message and text calls required by the report validation operation.

            reportMessage (
                ClientView.MessageSeverity.INFO,
                "Model Validation",
                text ( "messages.validation.valid" )
            );

            return;
        }

        // Perform the for each, get validation messages, get content, report message, value of, get severity, get
        // code, get entity identifier, get field, and get remedy calls required by the report validation operation.

        session.getContent ().getValidationMessages ().forEach (
            message -> reportMessage (
                ClientView.MessageSeverity.valueOf ( message.getSeverity () ),
                "Model Validation",
                message.getCode ()
                    + " - "
                    + message.getEntityIdentifier ()
                    + "."
                    + message.getField ()
                    + ": "
                    + message.getRemedy ()
            )
        );
    }

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

    @Override
    public void setBackgroundOperation ( boolean running, String description )
    {
        this.backgroundOperationRunning = running;

        // Perform the set text, text, set visible, set managed, set disable, set disabled, and render simulator
        // command state calls required by the set background operation operation.

        this.operationStatusLabel.setText ( running ? description : text ( "status.ready" ) );
        this.progressIndicator.setVisible ( running );
        this.progressIndicator.setManaged ( running );
        this.cancelOperationButton.setVisible ( running );
        this.cancelOperationButton.setManaged ( running );
        this.cancelOperationButton.setDisable ( !running );
        this.entityEditor.setDisabled ( running );
        this.modelIdentifierValue.setDisable ( running );
        this.modelNameValue.setDisable ( running );
        this.modelDescriptionValue.setDisable ( running );
        this.applyModelDetailsButton.setDisable ( running );
        renderSimulatorCommandState ();

        // Handle the branch where running is true.

        if ( running )
        {
            // Complete the set background operation step by calling report message.

            reportMessage ( ClientView.MessageSeverity.DEBUG, "Operation", description );
        }

        // Handle the branch where rendered session is available.

        if ( this.renderedSession != null )
        {
            // Complete the set background operation step by calling render command state.

            renderCommandState ( this.renderedSession );
        }
    }

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

    @Override
    public void setStatus ( String status )
    {
        // Apply set text and report message to the operation status label for the set status operation.

        this.operationStatusLabel.setText ( status );
        reportMessage ( ClientView.MessageSeverity.INFO, "Application", status );
    }

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

    @Override
    public void setConnectionStatus ( String status )
    {
        // Apply set text and text to the connection status label for the set connection status operation.

        this.connectionStatusLabel.setText ( text ( "status.connection" ) + ": " + status );
    }

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

    @Override
    public void setModelRevisionStatus ( String localRevision, String serverRevision )
    {
        // Prepare the normalized local revision and normalized server revision values needed by the set model revision
        // status operation.

        String normalizedLocalRevision = localRevision == null || localRevision.isBlank ()
            ? text ( "simulator.revision.unknown" )
            : localRevision;
        String normalizedServerRevision = serverRevision == null || serverRevision.isBlank ()
            ? text ( "simulator.revision.unknown" )
            : serverRevision;

        // Set the text on the local revision value.

        this.localRevisionValue.setText ( normalizedLocalRevision );
        this.serverRevisionValue.setText ( normalizedServerRevision );

        // Handle the branch where normalized local revision matches text "simulator revision unknown" or normalized
        // server revision matches text "simulator revision unknown".

        if (
            normalizedLocalRevision.equals ( text ( "simulator.revision.unknown" ) )
                || normalizedServerRevision.equals ( text ( "simulator.revision.unknown" ) )
        )
        {
            // Apply set text and text to the revision reconciliation value for the set model revision status
            // operation.

            this.revisionReconciliationValue.setText (
                text ( "simulator.revision.not.compared" )
            );
        }

        // Handle the alternative where normalized local revision matches normalized server revision.

        else if ( normalizedLocalRevision.equals ( normalizedServerRevision ) )
        {
            // Apply set text and text to the revision reconciliation value for the set model revision status
            // operation.

            this.revisionReconciliationValue.setText ( text ( "simulator.revision.matches" ) );
        }

        // Handle the alternative path when the preceding condition is not satisfied.

        else
        {
            // Apply set text and text to the revision reconciliation value for the set model revision status
            // operation.

            this.revisionReconciliationValue.setText ( text ( "simulator.revision.differs" ) );
        }
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

    @Override
    public void showEvaluation ( ClientEvaluationResult evaluationResult, String localRevision )
    {
        // Perform the set text, get outcome, or else, get action identifier, text, get rule identifier, is present,
        // get specificity, to string, or else throw, get server elapsed microseconds, get round trip microseconds, set
        // model revision status, and get model revision calls required by the show evaluation operation.

        this.evaluationOutcomeValue.setText ( evaluationResult.getOutcome () );
        this.evaluationActionValue.setText (
            evaluationResult.getActionIdentifier ().orElse ( text ( "simulator.result.not.applicable" ) )
        );
        this.evaluationRuleValue.setText (
            evaluationResult.getRuleIdentifier ().orElse ( text ( "simulator.result.not.applicable" ) )
        );
        this.evaluationSpecificityValue.setText (
            evaluationResult.getSpecificity ().isPresent ()
                ? Integer.toString ( evaluationResult.getSpecificity ().orElseThrow () )
                : text ( "simulator.result.not.applicable" )
        );
        this.evaluationServerLatencyValue.setText (
            evaluationResult.getServerElapsedMicroseconds () + " \u00b5s"
        );
        this.evaluationRoundTripLatencyValue.setText (
            evaluationResult.getRoundTripMicroseconds () + " \u00b5s"
        );
        setModelRevisionStatus ( localRevision, evaluationResult.getModelRevision () );
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
    //   severity (ClientView.MessageSeverity):
    //     The severity to use.
    //
    //   source (String):
    //     The source to use.
    //
    //   message (String):
    //     The message to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public void reportMessage (
        ClientView.MessageSeverity severity,
        String source,
        String message
    )
    {
        // Prepare the normalized source, normalized message, continuation indent, terminal message, and line values
        // needed by the report message operation.

        String normalizedSource = source == null || source.isBlank ()
            ? "Application"
            : source.trim ();
        String normalizedMessage = message == null || message.isBlank ()
            ? text ( "dialog.error.unknown" )
            : message.trim ();
        String continuationIndent = System.lineSeparator () + "    ";
        String terminalMessage = normalizedMessage
            .replace ( "\r\n", "\n" )
            .replace ( '\r', '\n' )
            .replace ( "\n", continuationIndent );
        String line = "["
            + LocalTime.now ().format ( MESSAGE_TIME_FORMAT )
            + "] ["
            + severity.name ()
            + "] ["
            + normalizedSource
            + "] "
            + terminalMessage
            + System.lineSeparator ();

        // Apply append text, position caret, and get length to the message terminal for the report message operation.

        this.messageTerminal.appendText ( line );
        this.messageTerminal.positionCaret ( this.messageTerminal.getLength () );
    }

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

    @Override
    public void reportError ( String title, String message )
    {
        // Apply set text and report message to the operation status label for the report error operation.

        this.operationStatusLabel.setText ( title );
        reportMessage ( ClientView.MessageSeverity.ERROR, title, message );
    }

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

    @Override
    public boolean confirmDiscard ( String actionName )
    {
        // Initialize the confirmation by applying text.

        Alert confirmation = new Alert (
            Alert.AlertType.CONFIRMATION,
            text ( "dialog.discard.message.prefix" ) + " " + actionName + "?",
            ButtonType.YES,
            ButtonType.NO
        );

        // Apply init owner, set title, text, and set header text to the confirmation for the confirm discard
        // operation.

        confirmation.initOwner ( this.primaryStage );
        confirmation.setTitle ( text ( "dialog.discard.title" ) );
        confirmation.setHeaderText ( text ( "dialog.discard.header" ) );

        // Return whether confirmation show and wait or else button type no equals button type yes.

        return confirmation.showAndWait ().orElse ( ButtonType.NO ) == ButtonType.YES;
    }

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

    @Override
    public Optional <Path> chooseOpenPath ()
    {
        // Prepare the file chooser and selected file values needed by the choose open path operation.

        FileChooser fileChooser = createFileChooser ( text ( "file.open.title" ) );
        java.io.File selectedFile = fileChooser.showOpenDialog ( this.primaryStage );

        // Stop this path and return its result when selected file is unavailable.

        if ( selectedFile == null )
        {
            // Return an empty optional result because no value is available when selected file is unavailable.

            return Optional.empty ();
        }

        // Initialize the selected path by applying to path.

        Path selectedPath = selectedFile.toPath ();

        // Complete the choose open path step by calling remember directory.

        this.preferencesStore.rememberDirectory ( selectedPath );

        // Return an optional containing the available value.

        return Optional.of ( selectedPath );
    }

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

    @Override
    public Optional <Path> chooseSavePath ( Optional <Path> currentPath )
    {
        // Initialize the file chooser by applying create file chooser and text.

        FileChooser fileChooser = createFileChooser ( text ( "file.save.title" ) );

        // Perform the if present, set initial file name, to string, and get file name calls required by the choose
        // save path operation.

        currentPath.ifPresent (
            path -> fileChooser.setInitialFileName ( path.getFileName ().toString () )
        );

        // Handle the branch where current path contains no values.

        if ( currentPath.isEmpty () )
        {
            // Set the initial file name on the file chooser.

            fileChooser.setInitialFileName ( "eca-model.json" );
        }

        // Initialize the selected file by applying show save dialog.

        java.io.File selectedFile = fileChooser.showSaveDialog ( this.primaryStage );

        // Stop this path and return its result when selected file is unavailable.

        if ( selectedFile == null )
        {
            // Return an empty optional result because no value is available when selected file is unavailable.

            return Optional.empty ();
        }

        // Initialize the selected path by applying ensure JSON extension and to path.

        Path selectedPath = ensureJsonExtension ( selectedFile.toPath () );

        // Complete the choose save path step by calling remember directory.

        this.preferencesStore.rememberDirectory ( selectedPath );

        // Return an optional containing the available value.

        return Optional.of ( selectedPath );
    }

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

    @Override
    public Optional <ClientConnectionSettings> editSettings (
        ClientConnectionSettings currentSettings
    )
    {
        // Prepare the dialog, theme selector, appearance grid, base URL field, connect timeout field, request timeout
        // field, bearer token field, and connection grid values needed by the edit settings operation.

        Dialog <ButtonType> dialog = new Dialog <ButtonType> ();
        ComboBox <Theme> themeSelector = new ComboBox <Theme> ();
        GridPane appearanceGrid = createFormGrid ();
        TextField baseUrlField = new TextField ( currentSettings.getBaseUri ().toString () );
        TextField connectTimeoutField = new TextField (
            Long.toString ( currentSettings.getConnectTimeout ().toSeconds () )
        );
        TextField requestTimeoutField = new TextField (
            Long.toString ( currentSettings.getRequestTimeout ().toSeconds () )
        );
        PasswordField bearerTokenField = new PasswordField ();
        GridPane connectionGrid = createFormGrid ();

        // Perform the set all, get items, values, set value, set max width, set converter, text, get settings resource
        // key, and add form row calls required by the edit settings operation.

        themeSelector.getItems ().setAll ( Theme.values () );
        themeSelector.setValue ( this.currentTheme );
        themeSelector.setMaxWidth ( Double.MAX_VALUE );
        themeSelector.setConverter (
            new StringConverter <Theme> ()
            {
                @Override
                public String toString ( Theme theme )
                {
                    // Return the value selected according to theme is unavailable.

                    return theme == null ? "" : text ( theme.getSettingsResourceKey () );
                }

                @Override
                public Theme fromString ( String value )
                {
                    throw new UnsupportedOperationException (
                        "Application themes are selected, not parsed."
                    );
                }
            }
        );
        addFormRow ( appearanceGrid, 0, text ( "settings.theme.label" ), themeSelector );

        // Initialize the appearance group by applying create group box and text.

        TitledPane appearanceGroup = createGroupBox (
            text ( "settings.appearance.group.title" ),
            appearanceGrid
        );

        // Perform the set text, get bearer token, add form row, and text calls required by the edit settings
        // operation.

        bearerTokenField.setText ( currentSettings.getBearerToken () );
        addFormRow ( connectionGrid, 0, text ( "connection.url.label" ), baseUrlField );
        addFormRow ( connectionGrid, 1, text ( "connection.connect.timeout.label" ), connectTimeoutField );
        addFormRow ( connectionGrid, 2, text ( "connection.request.timeout.label" ), requestTimeoutField );
        addFormRow ( connectionGrid, 3, text ( "connection.token.label" ), bearerTokenField );

        // Initialize the token note by applying text.

        Label tokenNote = new Label ( text ( "connection.token.note" ) );

        // Perform the set wrap text, add, and get style class calls required by the edit settings operation.

        tokenNote.setWrapText ( true );
        tokenNote.getStyleClass ().add ( "secondary-text" );

        // Prepare the group content, connection group, settings group nodes, settings group list, settings detail
        // pane, and settings content values needed by the edit settings operation.

        VBox groupContent = new VBox ( 10.0, connectionGrid, tokenNote );
        TitledPane connectionGroup = createGroupBox (
            text ( "settings.server.group.title" ),
            groupContent
        );
        Map <SettingsGroup, Node> settingsGroupNodes = new EnumMap <SettingsGroup, Node> (
            SettingsGroup.class
        );
        ListView <SettingsGroup> settingsGroupList = new ListView <SettingsGroup> ();
        StackPane settingsDetailPane = new StackPane ();
        SplitPane settingsContent = new SplitPane ( settingsGroupList, settingsDetailPane );

        // Perform the set max size, put, set all, get items, values, set selection mode, get selection model, set cell
        // factory, update item, set text, text, get resource key, add listener, selected item property, get children,
        // get, set min width, set pref width, set accessible text, set divider positions, set pref size, add, get
        // style class, set resizable with parent, select, init owner, set title, set header text, set resizable, set
        // content, get dialog pane, get stylesheets, to external form, get resource, add all, and get button types
        // calls required by the edit settings operation.

        appearanceGroup.setMaxSize ( Double.MAX_VALUE, Double.MAX_VALUE );
        connectionGroup.setMaxSize ( Double.MAX_VALUE, Double.MAX_VALUE );
        settingsGroupNodes.put ( SettingsGroup.APPEARANCE, appearanceGroup );
        settingsGroupNodes.put ( SettingsGroup.SERVER, connectionGroup );
        settingsGroupList.getItems ().setAll ( SettingsGroup.values () );
        settingsGroupList.getSelectionModel ().setSelectionMode ( SelectionMode.SINGLE );
        settingsGroupList.setCellFactory (
            listView ->
                new ListCell <SettingsGroup> ()
                {
                    @Override
                    protected void updateItem ( SettingsGroup settingsGroup, boolean empty )
                    {
                        // Perform the update item, set text, text, and get resource key calls required by the update
                        // item operation.

                        super.updateItem ( settingsGroup, empty );
                        setText (
                            empty || settingsGroup == null
                                ? null
                                : text ( settingsGroup.getResourceKey () )
                        );
                    }
                }
        );
        settingsGroupList.getSelectionModel ().selectedItemProperty ().addListener (
            ( observableValue, oldSettingsGroup, newSettingsGroup ) ->
            {
                // Handle the branch where new settings group is available.

                if ( newSettingsGroup != null )
                {
                    // Perform the set all, get children, and get calls required by the edit settings operation.

                    settingsDetailPane.getChildren ().setAll (
                        settingsGroupNodes.get ( newSettingsGroup )
                    );
                }
            }
        );
        settingsGroupList.setMinWidth ( 160.0 );
        settingsGroupList.setPrefWidth ( 180.0 );
        settingsGroupList.setAccessibleText ( text ( "settings.groups.accessible" ) );
        settingsDetailPane.setMinWidth ( 500.0 );
        settingsContent.setDividerPositions ( 0.24 );
        settingsContent.setPrefSize ( 780.0, 420.0 );
        settingsContent.getStyleClass ().add ( "settings-master-detail" );
        SplitPane.setResizableWithParent ( settingsGroupList, false );
        settingsGroupList.getSelectionModel ().select ( SettingsGroup.APPEARANCE );

        dialog.initOwner ( this.primaryStage );
        dialog.setTitle ( text ( "settings.dialog.title" ) );
        dialog.setHeaderText ( null );
        dialog.setResizable ( true );
        dialog.getDialogPane ().setContent ( settingsContent );
        dialog.getDialogPane ().getStylesheets ().add (
            ClientMain.class.getResource ( "client.css" ).toExternalForm ()
        );
        dialog.getDialogPane ().getStyleClass ().add ( this.currentTheme.getStyleClass () );
        dialog.getDialogPane ().getButtonTypes ().addAll (
            new ButtonType ( text ( "button.apply" ), ButtonBar.ButtonData.OK_DONE ),
            new ButtonType ( text ( "button.cancel" ), ButtonBar.ButtonData.CANCEL_CLOSE )
        );

        // Initialize the result by applying show and wait.

        Optional <ButtonType> result = dialog.showAndWait ();

        // Stop this path and return its result when result contains no values or result or else throw button data
        // differs from button bar button data ok done.

        if ( result.isEmpty () || result.orElseThrow ().getButtonData () != ButtonBar.ButtonData.OK_DONE )
        {
            // Return an empty optional result because no value is available when result contains no values or result
            // or else throw button data differs from button bar button data ok done.

            return Optional.empty ();
        }

        try
        {
            // Initialize the settings by applying create, trim, get text, of seconds, and parse long.

            ClientConnectionSettings settings = new ClientConnectionSettings (
                URI.create ( baseUrlField.getText ().trim () ),
                Duration.ofSeconds ( Long.parseLong ( connectTimeoutField.getText ().trim () ) ),
                Duration.ofSeconds ( Long.parseLong ( requestTimeoutField.getText ().trim () ) ),
                bearerTokenField.getText ()
            );

            // Perform the save connection settings, apply theme, and get value calls required by the edit settings
            // operation.

            this.preferencesStore.saveConnectionSettings ( settings );
            applyTheme ( themeSelector.getValue () );

            // Return an optional containing the available value.

            return Optional.of ( settings );
        }

        // Handle illegal argument failures captured as exception.

        catch ( IllegalArgumentException exception )
        {
            // Apply report error, text, and get message to the exception for the edit settings operation.

            reportError ( text ( "connection.invalid.title" ), exception.getMessage () );

            // Return an empty optional result because no value is available.

            return Optional.empty ();
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: exitApplication
    //
    // Description:
    //
    //   Performs the exit application operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public void exitApplication ()
    {
        // Complete the exit application step by calling save window preferences.

        saveWindowPreferences ();
        this.exitAuthorized = true;

        // Perform the close and exit calls required by the exit application operation.

        this.primaryStage.close ();
        Platform.exit ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createMenuBar
    //
    // Description:
    //
    //   Performs the create menu bar operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private MenuBar createMenuBar ()
    {
        // Prepare the file menu, edit menu, view menu, help menu, new menu item, and open menu item values needed by
        // the create menu bar operation.

        Menu fileMenu = new Menu ( text ( "menu.file" ) );
        Menu editMenu = new Menu ( text ( "menu.edit" ) );
        Menu viewMenu = new Menu ( text ( "menu.view" ) );
        Menu helpMenu = new Menu ( text ( "menu.help" ) );

        MenuItem newMenuItem = menuItem ( "menu.file.new", this.presenterAction ( "new" ) );
        MenuItem openMenuItem = menuItem ( "menu.file.open", this.presenterAction ( "open" ) );

        // Apply menu item and presenter action to the this for the create menu bar operation.

        this.saveMenuItem   = menuItem ( "menu.file.save", this.presenterAction ( "save" ) );
        this.saveAsMenuItem = menuItem ( "menu.file.save.as", this.presenterAction ( "save-as" ) );

        // Prepare the close menu item and validate menu item values needed by the create menu bar operation.

        MenuItem closeMenuItem = menuItem ( "menu.file.close", this.presenterAction ( "close" ) );
        MenuItem validateMenuItem = menuItem ( "menu.file.validate", this.presenterAction ( "validate" ) );

        // Update the push menu item from the presenter action result.

        this.pushMenuItem = menuItem ( "menu.file.push", this.presenterAction ( "push" ) );

        // Prepare the pull menu item, test connection menu item, settings menu item, and exit menu item values needed
        // by the create menu bar operation.

        MenuItem pullMenuItem = menuItem ( "menu.file.pull", this.presenterAction ( "pull" ) );
        MenuItem testConnectionMenuItem = menuItem (
            "menu.file.test.connection",
            this.presenterAction ( "test-connection" )
        );
        MenuItem settingsMenuItem = menuItem (
            "menu.file.settings",
            this.presenterAction ( "settings" )
        );
        MenuItem exitMenuItem = menuItem ( "menu.file.exit", this.presenterAction ( "exit" ) );

        // Add new menu item to the get items.

        fileMenu.getItems ().addAll (
            newMenuItem,
            openMenuItem,
            this.saveMenuItem,
            this.saveAsMenuItem,
            closeMenuItem,
            new SeparatorMenuItem (),
            validateMenuItem,
            this.pushMenuItem,
            pullMenuItem,
            new SeparatorMenuItem (),
            testConnectionMenuItem,
            settingsMenuItem,
            new SeparatorMenuItem (),
            exitMenuItem
        );

        // Apply menu item and presenter action to the this for the create menu bar operation.

        this.undoMenuItem = menuItem ( "menu.edit.undo", this.presenterAction ( "undo" ) );
        this.redoMenuItem = menuItem ( "menu.edit.redo", this.presenterAction ( "redo" ) );

        // Prepare the cut menu item, copy menu item, and paste menu item values needed by the create menu bar
        // operation.

        MenuItem cutMenuItem = menuItem ( "menu.edit.cut", () -> editFocusedText ( "cut" ) );
        MenuItem copyMenuItem = menuItem ( "menu.edit.copy", () -> editFocusedText ( "copy" ) );
        MenuItem pasteMenuItem = menuItem ( "menu.edit.paste", () -> editFocusedText ( "paste" ) );

        // Update the delete menu item from the menu item result.

        this.deleteMenuItem = menuItem (
            "menu.edit.delete",
            this.presenter::requestDeleteSelectedEntity
        );

        // Perform the set disable, add all, and get items calls required by the create menu bar operation.

        this.deleteMenuItem.setDisable ( true );
        editMenu.getItems ().addAll (
            this.undoMenuItem,
            this.redoMenuItem,
            new SeparatorMenuItem (),
            cutMenuItem,
            copyMenuItem,
            pasteMenuItem,
            this.deleteMenuItem
        );

        // Prepare the simulator menu item, clear messages and diagnostics menu item, theme menu, and theme toggle
        // group values needed by the create menu bar operation.

        MenuItem simulatorMenuItem = menuItem (
            "menu.view.simulator",
            () -> this.presenter.selectSimulator ()
        );
        MenuItem clearMessagesAndDiagnosticsMenuItem = menuItem (
            "menu.view.clear.messages.and.diagnostics",
            this::clearMessagesAndDiagnostics
        );
        Menu themeMenu = new Menu ( text ( "menu.view.theme" ) );
        ToggleGroup themeToggleGroup = new ToggleGroup ();

        // Construct the enum map instance required by the create menu bar operation.

        this.themeMenuItems = new EnumMap <Theme, RadioMenuItem> ( Theme.class );

        // Process each theme supplied by theme values.

        for ( Theme theme : Theme.values () )
        {
            // Initialize the theme menu item by applying text and get settings resource key.

            RadioMenuItem themeMenuItem = new RadioMenuItem (
                text ( theme.getSettingsResourceKey () )
            );

            // Perform the set toggle group, set selected, set on action, apply theme, add, get items, and put calls
            // required by the create menu bar operation.

            themeMenuItem.setToggleGroup ( themeToggleGroup );
            themeMenuItem.setSelected ( theme == this.currentTheme );
            themeMenuItem.setOnAction ( event -> applyTheme ( theme ) );
            themeMenu.getItems ().add ( themeMenuItem );
            this.themeMenuItems.put ( theme, themeMenuItem );
        }

        // Process each category identifier supplied by category identifiers.

        for ( String categoryIdentifier : CATEGORY_IDENTIFIERS )
        {
            // Initialize the category menu item by applying category name.

            MenuItem categoryMenuItem = new MenuItem ( categoryName ( categoryIdentifier ) );

            // Perform the set on action, select category, add, and get items calls required by the create menu bar
            // operation.

            categoryMenuItem.setOnAction (
                event -> this.presenter.selectCategory ( categoryIdentifier )
            );
            viewMenu.getItems ().add ( categoryMenuItem );
        }

        // Add new separator menu item to the get items.

        viewMenu.getItems ().addAll (
            new SeparatorMenuItem (),
            simulatorMenuItem,
            new SeparatorMenuItem (),
            clearMessagesAndDiagnosticsMenuItem,
            new SeparatorMenuItem (),
            themeMenu
        );

        // Update the operation sensitive menu items from the of result.

        this.operationSensitiveMenuItems = List.of (
            newMenuItem,
            openMenuItem,
            closeMenuItem,
            pullMenuItem,
            settingsMenuItem,
            testConnectionMenuItem
        );

        // Prepare the technical note menu item, github menu item, and about menu item values needed by the create menu
        // bar operation.

        MenuItem technicalNoteMenuItem = menuItem (
            "menu.help.technical.note",
            () -> getHostServices ().showDocument ( TECHNICAL_NOTE_URL )
        );
        MenuItem githubMenuItem = menuItem (
            "menu.help.github",
            () -> getHostServices ().showDocument ( GITHUB_URL )
        );
        MenuItem aboutMenuItem = menuItem ( "menu.help.about", this::showAbout );

        // Add the help menu items to the menu.

        helpMenu.getItems ().addAll (
            technicalNoteMenuItem,
            githubMenuItem,
            new SeparatorMenuItem (),
            aboutMenuItem
        );

        // Return a newly constructed menu bar containing the operation result.

        return new MenuBar ( fileMenu, editMenu, viewMenu, helpMenu );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: clearMessagesAndDiagnostics
    //
    // Description:
    //
    //   Clears all messages and diagnostics from the workspace terminal.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void clearMessagesAndDiagnostics ()
    {
        // Clear every message and diagnostic currently displayed in the shared terminal.

        this.messageTerminal.clear ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createWorkspace
    //
    // Description:
    //
    //   Performs the create workspace operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private SplitPane createWorkspace ()
    {
        // Update the model root item from the text result.

        this.modelRootItem = new TreeItem <NavigationItem> (
            NavigationItem.root ( text ( "tree.model" ) )
        );

        // Set the expanded on the model root item.

        this.modelRootItem.setExpanded ( true );

        // Construct the tree view instance required by the create workspace operation.

        this.modelTree = new TreeView <NavigationItem> ( this.modelRootItem );

        // Perform the set show root, set min width, set max size, add listener, selected item property, get selection
        // model, handle tree selection, render context-sensitive help, set on mouse clicked, get button, get kind, get
        // selection,
        // get selected item, and select model calls required by the create workspace operation.

        this.modelTree.setShowRoot ( true );
        this.modelTree.setMinWidth ( 190.0 );
        this.modelTree.setMaxSize ( Double.MAX_VALUE, Double.MAX_VALUE );
        this.modelTree.getSelectionModel ().selectedItemProperty ().addListener (
            ( observableValue, oldItem, newItem ) ->
            {
                // Perform the handle tree selection and render context-sensitive help calls required by the create
                // workspace
                // operation.

                handleTreeSelection ( newItem );
                renderUserGuide ();
            }
        );
        this.modelTree.setOnMouseClicked (
            event ->
            {
                // Handle the branch where event button equals mouse button primary and rendering is false and rendered
                // session is available and rendered session selection kind equals selection kind simulator and model
                // tree selection model selected item equals model root item.

                if (
                    event.getButton () == MouseButton.PRIMARY
                        && !this.rendering
                        && this.renderedSession != null
                        && this.renderedSession.getSelection ().getKind () == Selection.Kind.SIMULATOR
                        && this.modelTree.getSelectionModel ().getSelectedItem () == this.modelRootItem
                )
                {
                    // Complete the create workspace step by calling select model.

                    this.presenter.selectModel ();
                }
            }
        );

        // Initialize the outline pane by applying create group box and text.

        TitledPane outlinePane = createGroupBox (
            text ( "outline.title" ),
            this.modelTree
        );

        // Perform the set min width, set max size, add, get style class, set accessible text, and text calls required
        // by the create workspace operation.

        outlinePane.setMinWidth ( 210.0 );
        outlinePane.setMaxSize ( Double.MAX_VALUE, Double.MAX_VALUE );
        outlinePane.getStyleClass ().add ( "workspace-panel" );
        outlinePane.setAccessibleText ( text ( "outline.title" ) );

        // Perform the text, create editor pane, and create simulator pane calls required by the create workspace
        // operation.

        this.editorTab = new Tab ( text ( "tab.editor" ), createEditorPane () );
        this.simulatorTab = new Tab ( text ( "tab.simulator" ), createSimulatorPane () );

        // Set the closable on the editor tab.

        this.editorTab.setClosable ( false );
        this.simulatorTab.setClosable ( false );

        // Construct the tab pane instance required by the create workspace operation.

        this.detailTabs = new TabPane ( this.editorTab, this.simulatorTab );

        // Perform the add listener, selected item property, get selection model, select simulator, and render user
        // guide calls required by the create workspace operation.

        this.detailTabs.getSelectionModel ().selectedItemProperty ().addListener (
            ( observableValue, oldTab, newTab ) ->
            {
                // Handle the branch where rendering is false and new tab equals simulator tab.

                if ( !this.rendering && newTab == this.simulatorTab )
                {
                    // Complete the create workspace step by calling select simulator.

                    this.presenter.selectSimulator ();
                }

                // Complete the create workspace step by calling render context-sensitive help.

                renderUserGuide ();
            }
        );

        // Initialize the context-sensitive help pane by applying create context-sensitive help pane.

        TitledPane userGuidePane = createUserGuidePane ();

        // Construct the split pane instance required by the create workspace operation.

        this.masterDetailSplitPane = new SplitPane (
            outlinePane,
            this.detailTabs,
            userGuidePane
        );
        this.messageTerminal = new TextArea ();

        // Perform the set editable, set wrap text, add, and get style class calls required by the create workspace
        // operation.

        this.messageTerminal.setEditable ( false );
        this.messageTerminal.setWrapText ( false );
        this.messageTerminal.getStyleClass ().add ( "message-terminal" );

        // Initialize the messages group by applying create group box and text.

        TitledPane messagesGroup = createGroupBox (
            text ( "messages.group" ),
            this.messageTerminal
        );

        // Perform the set min height, set accessible text, and text calls required by the create workspace operation.

        messagesGroup.setMinHeight ( 105.0 );
        this.messageTerminal.setAccessibleText ( text ( "messages.group" ) );

        // Construct the split pane instance required by the create workspace operation.

        this.workspaceSplitPane = new SplitPane (
            this.masterDetailSplitPane,
            messagesGroup
        );

        // Set the orientation on the workspace split pane.

        this.workspaceSplitPane.setOrientation ( Orientation.VERTICAL );

        // Return the workspace split pane to the caller.

        return this.workspaceSplitPane;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createUserGuidePane
    //
    // Description:
    //
    //   Performs the create context-sensitive help pane operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private TitledPane createUserGuidePane ()
    {
        // Prepare the overview heading and what to do heading values needed by the create context-sensitive help pane
        // operation.

        Label overviewHeading = new Label ( text ( "user.guide.overview.heading" ) );
        Label whatToDoHeading = new Label ( text ( "user.guide.what.to.do.heading" ) );

        // Perform the add and get style class calls required by the create context-sensitive help pane operation.

        overviewHeading.getStyleClass ().add ( "user-guide-section-heading" );
        whatToDoHeading.getStyleClass ().add ( "user-guide-section-heading" );

        // Construct the label instance required by the create context-sensitive help pane operation.

        this.userGuideOverviewValue = new Label ();

        // Perform the set wrap text, set max width, add, and get style class calls required by the create
        // context-sensitive help
        // pane operation.

        this.userGuideOverviewValue.setWrapText ( true );
        this.userGuideOverviewValue.setMaxWidth ( Double.MAX_VALUE );
        this.userGuideOverviewValue.getStyleClass ().add ( "user-guide-copy" );

        // Construct the enum map instance required by the create context-sensitive help pane operation.

        this.userGuideEquationImages = new EnumMap <Theme, Map <ClientUserGuide.Context, Image>> (
            Theme.class
        );

        // Process each theme supplied by theme values.

        for ( Theme theme : Theme.values () )
        {
            // Initialize the theme equation images with a new enum map.

            Map <ClientUserGuide.Context, Image> themeEquationImages =
                new EnumMap <ClientUserGuide.Context, Image> ( ClientUserGuide.Context.class );

            // Process each context supplied by client context-sensitive help context values.

            for ( ClientUserGuide.Context context : ClientUserGuide.Context.values () )
            {
                // Prepare the equation resource name and equation resource values needed by the create
                // context-sensitive help pane
                // operation.

                String equationResourceName = context.getEquationResourceName (
                    theme == Theme.DARK
                );
                URL equationResource = ClientMain.class.getResource ( equationResourceName );

                // Reject the operation when equation resource is unavailable.

                if ( equationResource == null )
                {
                    throw new IllegalStateException (
                        "Missing user-guide equation resource: " + equationResourceName
                    );
                }

                // Store new image equation resource to external form false under context in the theme equation images.

                themeEquationImages.put (
                    context,
                    new Image ( equationResource.toExternalForm (), false )
                );
            }

            // Store theme equation images under theme in the context-sensitive help equation images.

            this.userGuideEquationImages.put ( theme, themeEquationImages );
        }

        // Construct the image view instance required by the create context-sensitive help pane operation.

        this.userGuideEquationImage = new ImageView ();

        // Configure the context-sensitive help equation image with the required preserve ratio, smooth, fit width, and
        // fit height
        // values.

        this.userGuideEquationImage.setPreserveRatio ( true );
        this.userGuideEquationImage.setSmooth ( true );
        this.userGuideEquationImage.setFitWidth ( 0.0 );
        this.userGuideEquationImage.setFitHeight ( 0.0 );

        // Initialize the equation card with a new stack pane.

        StackPane equationCard = new StackPane ( this.userGuideEquationImage );

        // Add "user-guide-equation-card" to the get style class.

        equationCard.getStyleClass ().add ( "user-guide-equation-card" );

        // Prepare the technical note link and github link values needed by the create context-sensitive help pane
        // operation.

        Hyperlink technicalNoteLink = new Hyperlink ( text ( "user.guide.technical.note.link" ) );
        Hyperlink githubLink        = new Hyperlink ( text ( "user.guide.github.link" ) );

        // Perform the set on action, show document, and get host services calls required by the create
        // context-sensitive help pane
        // operation.

        technicalNoteLink.setOnAction (
            event -> getHostServices ().showDocument ( TECHNICAL_NOTE_URL )
        );
        githubLink.setOnAction (
            event -> getHostServices ().showDocument ( GITHUB_URL )
        );

        // Initialize the link column with a new v box.

        VBox linkColumn = new VBox ( 2.0, technicalNoteLink, githubLink );

        // Add "user-guide-links" to the get style class.

        linkColumn.getStyleClass ().add ( "user-guide-links" );

        // Construct the label instance required by the create context-sensitive help pane operation.

        this.userGuideStepsValue = new Label ();

        // Perform the set wrap text, set max width, set max height, set alignment, add, and get style class calls
        // required by the create context-sensitive help pane operation.

        this.userGuideStepsValue.setWrapText ( true );
        this.userGuideStepsValue.setMaxWidth ( Double.MAX_VALUE );
        this.userGuideStepsValue.setMaxHeight ( Double.MAX_VALUE );
        this.userGuideStepsValue.setAlignment ( Pos.TOP_LEFT );
        this.userGuideStepsValue.getStyleClass ().add ( "user-guide-copy" );

        // Initialize the guide content with a new v box.

        VBox guideContent = new VBox (
            10.0,
            overviewHeading,
            this.userGuideOverviewValue,
            equationCard,
            linkColumn,
            whatToDoHeading,
            this.userGuideStepsValue
        );

        // Perform the set fill width, set padding, set max height, add, get style class, and set vgrow calls required
        // by the create context-sensitive help pane operation.

        guideContent.setFillWidth ( true );
        guideContent.setPadding ( new Insets ( 14.0 ) );
        guideContent.setMaxHeight ( Double.MAX_VALUE );
        guideContent.getStyleClass ().add ( "user-guide-content" );
        VBox.setVgrow ( this.userGuideStepsValue, Priority.ALWAYS );

        // Initialize the guide scroll pane with a new scroll pane.

        ScrollPane guideScrollPane = new ScrollPane ( guideContent );

        // Perform the set fit to width, set fit to height, set max size, set hbar policy, add, and get style class
        // calls required by the create context-sensitive help pane operation.

        guideScrollPane.setFitToWidth ( true );
        guideScrollPane.setFitToHeight ( true );
        guideScrollPane.setMaxSize ( Double.MAX_VALUE, Double.MAX_VALUE );
        guideScrollPane.setHbarPolicy ( ScrollPane.ScrollBarPolicy.NEVER );
        guideScrollPane.getStyleClass ().add ( "user-guide-scroll-pane" );

        // Initialize the context-sensitive help pane by applying create group box and text.

        TitledPane userGuidePane = createGroupBox (
            text ( "user.guide.title" ),
            guideScrollPane
        );

        // Perform the set min width, set max size, add, get style class, set accessible text, and text calls required
        // by the create context-sensitive help pane operation.

        userGuidePane.setMinWidth ( 420.0 );
        userGuidePane.setMaxSize ( Double.MAX_VALUE, Double.MAX_VALUE );
        userGuidePane.getStyleClass ().add ( "workspace-panel" );
        userGuidePane.setAccessibleText ( text ( "user.guide.title" ) );

        // Return the context-sensitive help pane to the caller.

        return userGuidePane;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createEditorPane
    //
    // Description:
    //
    //   Performs the create editor pane operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private VBox createEditorPane ()
    {
        // Update the editor heading label from the text result.

        this.editorHeadingLabel = new Label ( text ( "editor.heading" ) );

        // Add "page-heading" to the get style class.

        this.editorHeadingLabel.getStyleClass ().add ( "page-heading" );

        // Construct the text field instance required by the create editor pane operation.

        this.modelIdentifierValue = new TextField ();
        this.modelNameValue = new TextField ();
        this.modelDescriptionValue = new TextArea ();

        // Configure the model description value with the required wrap text and pref row count values.

        this.modelDescriptionValue.setWrapText ( true );
        this.modelDescriptionValue.setPrefRowCount ( 3 );

        // Initialize the overview grid by applying create form grid.

        GridPane overviewGrid = createFormGrid ();

        // Perform the add form row and text calls required by the create editor pane operation.

        addFormRow ( overviewGrid, 0, text ( "model.id.label" ), this.modelIdentifierValue );
        addFormRow ( overviewGrid, 1, text ( "model.name.label" ), this.modelNameValue );
        addFormRow ( overviewGrid, 2, text ( "model.description.label" ), this.modelDescriptionValue );

        // Update the apply model details button from the text result.

        this.applyModelDetailsButton = new Button ( text ( "button.apply.model.details" ) );

        // Initialize the validate button by applying text.

        Button validateButton = new Button ( text ( "button.validate" ) );

        // Perform the set on action, request apply model details, trim, get text, and request validate calls required
        // by the create editor pane operation.

        this.applyModelDetailsButton.setOnAction (
            event -> this.presenter.requestApplyModelDetails (
                this.modelIdentifierValue.getText ().trim (),
                this.modelNameValue.getText ().trim (),
                this.modelDescriptionValue.getText ().trim ()
            )
        );
        validateButton.setOnAction ( event -> this.presenter.requestValidate () );

        // Prepare the overview content and overview group values needed by the create editor pane operation.

        VBox overviewContent = new VBox (
            10.0,
            overviewGrid,
            createGroupActionRow ( this.applyModelDetailsButton, validateButton )
        );
        TitledPane overviewGroup = createGroupBox ( text ( "model.overview.group" ), overviewContent );

        // Construct the client entity editor instance required by the create editor pane operation.

        this.entityEditor = new ClientEntityEditor ( this.resourceBundle, this.presenter );
        this.modelOverviewNode   = overviewGroup;

        // Complete the create editor pane step by calling get node.

        this.entityEditorNode    = this.entityEditor.getNode ();
        this.editorDetailContent = new VBox ();

        // Initialize the editor pane with a new v box.

        VBox editorPane = new VBox (
            12.0,
            this.editorHeadingLabel,
            this.editorDetailContent
        );

        // Perform the set padding, set fill width, and set vgrow calls required by the create editor pane operation.

        editorPane.setPadding ( new Insets ( 14.0 ) );
        this.editorDetailContent.setFillWidth ( true );
        VBox.setVgrow ( this.entityEditorNode, Priority.ALWAYS );
        VBox.setVgrow ( this.editorDetailContent, Priority.ALWAYS );

        // Return the editor pane to the caller.

        return editorPane;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createSimulatorPane
    //
    // Description:
    //
    //   Performs the create simulator pane operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private VBox createSimulatorPane ()
    {
        // Initialize the heading by applying text.

        Label heading = new Label ( text ( "simulator.heading" ) );

        // Add "page-heading" to the get style class.

        heading.getStyleClass ().add ( "page-heading" );

        // Construct the combo box instance required by the create simulator pane operation.

        this.simulatorEventSelector = new ComboBox <EventDescriptor> ();

        // Perform the set max width, add listener, value property, render simulator payload, and clear evaluation
        // result calls required by the create simulator pane operation.

        this.simulatorEventSelector.setMaxWidth ( Double.MAX_VALUE );
        this.simulatorEventSelector.valueProperty ().addListener (
            ( observableValue, oldEvent, newEvent ) ->
            {
                // Handle the branch where rendering is false.

                if ( !this.rendering )
                {
                    // Perform the render simulator payload and clear evaluation result calls required by the create
                    // simulator pane operation.

                    renderSimulatorPayload ( newEvent );
                    clearEvaluationResult ();
                }
            }
        );

        // Initialize the event grid by applying create form grid.

        GridPane eventGrid = createFormGrid ();

        // Perform the add form row and text calls required by the create simulator pane operation.

        addFormRow (
            eventGrid,
            0,
            text ( "simulator.event.label" ),
            this.simulatorEventSelector
        );

        // Construct the label instance required by the create simulator pane operation.

        this.simulatorAvailabilityLabel = new Label ();

        // Perform the set wrap text, add, and get style class calls required by the create simulator pane operation.

        this.simulatorAvailabilityLabel.setWrapText ( true );
        this.simulatorAvailabilityLabel.getStyleClass ().add ( "secondary-text" );

        // Update the simulator empty payload label from the text result.

        this.simulatorEmptyPayloadLabel = new Label ( text ( "simulator.payload.empty" ) );

        // Perform the set wrap text, add, and get style class calls required by the create simulator pane operation.

        this.simulatorEmptyPayloadLabel.setWrapText ( true );
        this.simulatorEmptyPayloadLabel.getStyleClass ().add ( "secondary-text" );

        // Perform the create form grid and text calls required by the create simulator pane operation.

        this.simulatorPayloadGrid     = createFormGrid ();
        this.simulatorPayloadControls = new LinkedHashMap <String, SimulatorPayloadControl> ();
        this.evaluateButton           = new Button ( text ( "button.evaluate" ) );

        // Apply set default button, set on action, and request evaluation to the evaluate button for the create
        // simulator pane operation.

        this.evaluateButton.setDefaultButton ( true );
        this.evaluateButton.setOnAction ( event -> requestEvaluation () );

        // Prepare the occurrence content and occurrence group values needed by the create simulator pane operation.

        VBox occurrenceContent = new VBox (
            10.0,
            eventGrid,
            this.simulatorAvailabilityLabel,
            this.simulatorEmptyPayloadLabel,
            this.simulatorPayloadGrid,
            createGroupActionRow ( this.evaluateButton )
        );
        TitledPane occurrenceGroup = createGroupBox (
            text ( "simulator.occurrence.group" ),
            occurrenceContent
        );

        // Complete the create simulator pane step by calling read only value label.

        this.evaluationOutcomeValue          = readOnlyValueLabel ();
        this.evaluationActionValue           = readOnlyValueLabel ();
        this.evaluationRuleValue             = readOnlyValueLabel ();
        this.evaluationSpecificityValue      = readOnlyValueLabel ();
        this.evaluationServerLatencyValue    = readOnlyValueLabel ();
        this.evaluationRoundTripLatencyValue = readOnlyValueLabel ();

        // Initialize the result grid by applying create form grid.

        GridPane resultGrid = createFormGrid ();

        // Perform the add form row and text calls required by the create simulator pane operation.

        addFormRow ( resultGrid, 0, text ( "simulator.result.outcome" ), this.evaluationOutcomeValue );
        addFormRow ( resultGrid, 1, text ( "simulator.result.action" ), this.evaluationActionValue );
        addFormRow ( resultGrid, 2, text ( "simulator.result.rule" ), this.evaluationRuleValue );
        addFormRow (
            resultGrid,
            3,
            text ( "simulator.result.specificity" ),
            this.evaluationSpecificityValue
        );
        addFormRow (
            resultGrid,
            4,
            text ( "simulator.result.server.latency" ),
            this.evaluationServerLatencyValue
        );
        addFormRow (
            resultGrid,
            5,
            text ( "simulator.result.round.trip.latency" ),
            this.evaluationRoundTripLatencyValue
        );

        // Initialize the result group by applying create group box and text.

        TitledPane resultGroup = createGroupBox ( text ( "simulator.result.group" ), resultGrid );

        // Complete the create simulator pane step by calling read only value label.

        this.localRevisionValue          = readOnlyValueLabel ();
        this.serverRevisionValue         = readOnlyValueLabel ();
        this.revisionReconciliationValue = readOnlyValueLabel ();

        // Initialize the revision grid by applying create form grid.

        GridPane revisionGrid = createFormGrid ();

        // Perform the add form row and text calls required by the create simulator pane operation.

        addFormRow (
            revisionGrid,
            0,
            text ( "simulator.revision.local" ),
            this.localRevisionValue
        );
        addFormRow (
            revisionGrid,
            1,
            text ( "simulator.revision.server" ),
            this.serverRevisionValue
        );
        addFormRow (
            revisionGrid,
            2,
            text ( "simulator.revision.status" ),
            this.revisionReconciliationValue
        );

        // Prepare the revision group, simulator content, simulator scroll pane, and simulator pane values needed by
        // the create simulator pane operation.

        TitledPane revisionGroup = createGroupBox (
            text ( "simulator.revision.group" ),
            revisionGrid
        );
        VBox simulatorContent = new VBox (
            12.0,
            heading,
            occurrenceGroup,
            resultGroup,
            revisionGroup
        );
        ScrollPane simulatorScrollPane = new ScrollPane ( simulatorContent );
        VBox simulatorPane = new VBox ( simulatorScrollPane );

        // Perform the set padding, set fit to width, add, get style class, set vgrow, clear evaluation result, and set
        // model revision status calls required by the create simulator pane operation.

        simulatorContent.setPadding ( new Insets ( 14.0 ) );
        simulatorScrollPane.setFitToWidth ( true );
        simulatorScrollPane.getStyleClass ().add ( "editor-scroll-pane" );
        VBox.setVgrow ( simulatorScrollPane, Priority.ALWAYS );
        clearEvaluationResult ();
        setModelRevisionStatus ( "", "" );

        // Return the simulator pane to the caller.

        return simulatorPane;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createStatusBar
    //
    // Description:
    //
    //   Performs the create status bar operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private HBox createStatusBar ()
    {
        // Complete the create status bar step by calling text.

        this.documentStatusLabel = new Label ();
        this.validationStatusLabel = new Label ();
        this.connectionStatusLabel = new Label ( text ( "status.connection" ) + ": Not tested" );
        this.operationStatusLabel = new Label ( text ( "status.ready" ) );
        this.progressIndicator = new ProgressIndicator ();
        this.cancelOperationButton = new Button ( text ( "button.cancel" ) );

        // Perform the set pref size, set visible, set managed, set disable, set on action, and cancel background
        // operation calls required by the create status bar operation.

        this.progressIndicator.setPrefSize ( 18.0, 18.0 );
        this.progressIndicator.setVisible ( false );
        this.progressIndicator.setManaged ( false );
        this.cancelOperationButton.setVisible ( false );
        this.cancelOperationButton.setManaged ( false );
        this.cancelOperationButton.setDisable ( true );
        this.cancelOperationButton.setOnAction ( event -> this.presenter.cancelBackgroundOperation () );

        // Prepare the spacer and status bar values needed by the create status bar operation.

        Region spacer = new Region ();
        HBox statusBar = new HBox (
            9.0,
            this.documentStatusLabel,
            new Separator ( Orientation.VERTICAL ),
            this.validationStatusLabel,
            new Separator ( Orientation.VERTICAL ),
            this.connectionStatusLabel,
            spacer,
            this.operationStatusLabel,
            this.progressIndicator,
            this.cancelOperationButton
        );

        // Perform the set hgrow, set alignment, add, and get style class calls required by the create status bar
        // operation.

        HBox.setHgrow ( spacer, Priority.ALWAYS );
        statusBar.setAlignment ( Pos.CENTER_LEFT );
        statusBar.getStyleClass ().add ( "status-bar" );

        // Return the status bar to the caller.

        return statusBar;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: renderNavigationTree
    //
    // Description:
    //
    //   Performs the render navigation tree operation.
    //
    // Arguments:
    //
    //   session (ClientDocumentSession):
    //     The session to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void renderNavigationTree ( ClientDocumentSession session )
    {
        // Initialize the expansion state with a new linked hash map.

        Map <String, Boolean> expansionState = new LinkedHashMap <String, Boolean> ();

        // Process each category item supplied by model root item children.

        for ( TreeItem <NavigationItem> categoryItem : this.modelRootItem.getChildren () )
        {
            // Store category item is expanded under category item value category identifier in the expansion state.

            expansionState.put ( categoryItem.getValue ().getCategoryIdentifier (), categoryItem.isExpanded () );
        }

        // Perform the clear and get children calls required by the render navigation tree operation.

        this.modelRootItem.getChildren ().clear ();

        // Process each category identifier supplied by category identifiers.

        for ( String categoryIdentifier : CATEGORY_IDENTIFIERS )
        {
            // Initialize the category item by applying category and category name.

            TreeItem <NavigationItem> categoryItem = new TreeItem <NavigationItem> (
                NavigationItem.category ( categoryIdentifier, categoryName ( categoryIdentifier ) )
            );

            // Perform the set expanded, get or default, and contains calls required by the render navigation tree
            // operation.

            categoryItem.setExpanded (
                expansionState.getOrDefault (
                    categoryIdentifier,
                    this.preferredExpandedCategories.contains ( categoryIdentifier )
                )
            );

            // Process each entity identifier supplied by session content get entity identifiers category identifier.

            for ( String entityIdentifier : session.getContent ().getEntityIdentifiers ( categoryIdentifier ) )
            {
                // Add new tree item navigation item navigation item entity category identifier entity to the get
                // children.

                categoryItem.getChildren ().add (
                    new TreeItem <NavigationItem> (
                        NavigationItem.entity ( categoryIdentifier, entityIdentifier )
                    )
                );
            }

            // Add category item to the get children.

            this.modelRootItem.getChildren ().add ( categoryItem );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: renderDocumentOverview
    //
    // Description:
    //
    //   Performs the render document overview operation.
    //
    // Arguments:
    //
    //   session (ClientDocumentSession):
    //     The session to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void renderDocumentOverview ( ClientDocumentSession session )
    {
        // Initialize the summary by applying get summary and get content.

        Summary summary = session.getContent ().getSummary ();

        // Perform the set text, get model ID, get name, and get description calls required by the render document
        // overview operation.

        this.modelIdentifierValue.setText ( summary.getModelId () );
        this.modelNameValue.setText ( summary.getName () );
        this.modelDescriptionValue.setText ( summary.getDescription () );

        // Initialize the selection label by applying or else, map, get category identifier, get selection, and text.

        String selectionLabel = session.getSelection ().getCategoryIdentifier ()
            .map ( this::categoryName )
            .orElse ( text ( "tree.model" ) );

        // Handle the branch where session selection entity identifier is present.

        if ( session.getSelection ().getEntityIdentifier ().isPresent () )
        {
            // Perform the or else throw, get entity identifier, and get selection calls required by the render
            // document overview operation.

            selectionLabel += " — " + session.getSelection ().getEntityIdentifier ().orElseThrow ();
        }

        // Set the text on the editor heading label.

        this.editorHeadingLabel.setText ( selectionLabel );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: renderEditorDetail
    //
    // Description:
    //
    //   Performs the render editor detail operation.
    //
    // Arguments:
    //
    //   session (ClientDocumentSession):
    //     The session to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void renderEditorDetail ( ClientDocumentSession session )
    {
        // Initialize the selection kind by applying get kind and get selection.

        Selection.Kind selectionKind = session.getSelection ().getKind ();

        // Handle the branch where selection kind equals selection kind model.

        if ( selectionKind == Selection.Kind.MODEL )
        {
            // Perform the set all and get children calls required by the render editor detail operation.

            this.editorDetailContent.getChildren ().setAll ( this.modelOverviewNode );

            return;
        }

        // Handle the branch where selection kind equals selection kind category or selection kind equals selection
        // kind entity.

        if ( selectionKind == Selection.Kind.CATEGORY || selectionKind == Selection.Kind.ENTITY )
        {
            // Perform the render, set all, and get children calls required by the render editor detail operation.

            this.entityEditor.render ( session );
            this.editorDetailContent.getChildren ().setAll ( this.entityEditorNode );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: renderSimulator
    //
    // Description:
    //
    //   Performs the render simulator operation.
    //
    // Arguments:
    //
    //   session (ClientDocumentSession):
    //     The session to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void renderSimulator ( ClientDocumentSession session )
    {
        // Handle the branch where simulator rendered content equals session content.

        if ( this.simulatorRenderedContent == session.getContent () )
        {
            // Complete the render simulator step by calling render simulator command state.

            renderSimulatorCommandState ();

            return;
        }

        // Prepare the selected event identifier and events values needed by the render simulator operation.

        String selectedEventIdentifier = this.simulatorEventSelector.getValue () == null
            ? null
            : this.simulatorEventSelector.getValue ().getIdentifier ();
        List <EventDescriptor> events = this.presenter.getSimulatorEvents ();

        // Update the simulator rendered content from the get content result.

        this.simulatorRenderedContent = session.getContent ();

        // Perform the set all and get items calls required by the render simulator operation.

        this.simulatorEventSelector.getItems ().setAll ( events );

        // Initialize the selected event by applying or else, find first, filter, stream, equals, get identifier, is
        // empty, and get.

        EventDescriptor selectedEvent = events.stream ()
            .filter ( event -> event.getIdentifier ().equals ( selectedEventIdentifier ) )
            .findFirst ()
            .orElse ( events.isEmpty () ? null : events.get ( 0 ) );

        // Perform the set value, set text, can push, is empty, text, set visible, is blank, get text, set managed, is
        // visible, render simulator payload, clear evaluation result, and render simulator command state calls
        // required by the render simulator operation.

        this.simulatorEventSelector.setValue ( selectedEvent );
        this.simulatorAvailabilityLabel.setText (
            session.canPush ()
                ? events.isEmpty ()
                    ? text ( "simulator.no.events" )
                    : ""
                : text ( "simulator.model.invalid" )
        );
        this.simulatorAvailabilityLabel.setVisible (
            !this.simulatorAvailabilityLabel.getText ().isBlank ()
        );
        this.simulatorAvailabilityLabel.setManaged (
            this.simulatorAvailabilityLabel.isVisible ()
        );
        renderSimulatorPayload ( selectedEvent );
        clearEvaluationResult ();
        renderSimulatorCommandState ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: renderSimulatorPayload
    //
    // Description:
    //
    //   Performs the render simulator payload operation.
    //
    // Arguments:
    //
    //   eventDescriptor (EventDescriptor):
    //     The event descriptor to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void renderSimulatorPayload ( EventDescriptor eventDescriptor )
    {
        // Perform the clear and get children calls required by the render simulator payload operation.

        this.simulatorPayloadGrid.getChildren ().clear ();
        this.simulatorPayloadControls.clear ();

        // Handle the branch where event descriptor is available.

        if ( eventDescriptor != null )
        {
            int rowIndex = 0;

            // Process each field descriptor supplied by event descriptor payload fields.

            for ( PayloadFieldDescriptor fieldDescriptor : eventDescriptor.getPayloadFields () )
            {
                // Initialize the payload control with a new simulator payload control.

                SimulatorPayloadControl payloadControl = new SimulatorPayloadControl (
                    fieldDescriptor
                );

                // Perform the put, get identifier, add form row, get name, and get node calls required by the render
                // simulator payload operation.

                this.simulatorPayloadControls.put (
                    fieldDescriptor.getIdentifier (),
                    payloadControl
                );
                addFormRow (
                    this.simulatorPayloadGrid,
                    rowIndex,
                    fieldDescriptor.getName (),
                    payloadControl.getNode ()
                );
                rowIndex++;
            }
        }

        // Initialize the empty payload by applying is empty and get payload fields.

        boolean emptyPayload = eventDescriptor != null
            && eventDescriptor.getPayloadFields ().isEmpty ();

        // Perform the set visible, set managed, is visible, and render simulator command state calls required by the
        // render simulator payload operation.

        this.simulatorEmptyPayloadLabel.setVisible ( emptyPayload );
        this.simulatorEmptyPayloadLabel.setManaged ( emptyPayload );
        this.simulatorPayloadGrid.setVisible ( eventDescriptor != null && !emptyPayload );
        this.simulatorPayloadGrid.setManaged ( this.simulatorPayloadGrid.isVisible () );
        renderSimulatorCommandState ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: renderSimulatorCommandState
    //
    // Description:
    //
    //   Performs the render simulator command state operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void renderSimulatorCommandState ()
    {
        // Stop this path and return its result when simulator event selector is unavailable.

        if ( this.simulatorEventSelector == null )
        {
            return;
        }

        // Initialize the model ready by applying can push.

        boolean modelReady = this.renderedSession != null && this.renderedSession.canPush ();
        boolean disabled   = this.backgroundOperationRunning || !modelReady;

        // Perform the set disable, for each, values, set disabled, and get value calls required by the render
        // simulator command state operation.

        this.simulatorEventSelector.setDisable ( disabled );
        this.simulatorPayloadControls.values ().forEach (
            payloadControl -> payloadControl.setDisabled ( disabled )
        );
        this.evaluateButton.setDisable (
            disabled || this.simulatorEventSelector.getValue () == null
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requestEvaluation
    //
    // Description:
    //
    //   Performs the request evaluation operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void requestEvaluation ()
    {
        // Initialize the event descriptor by applying get value.

        EventDescriptor eventDescriptor = this.simulatorEventSelector.getValue ();

        // Handle the branch where event descriptor is unavailable.

        if ( eventDescriptor == null )
        {
            // Perform the report error and text calls required by the request evaluation operation.

            reportError (
                text ( "simulator.invalid.event.title" ),
                text ( "simulator.invalid.event.message" )
            );

            return;
        }

        // Initialize the payload drafts with a new linked hash map.

        LinkedHashMap <String, PayloadValueDraft> payloadDrafts =
            new LinkedHashMap <String, PayloadValueDraft> ();

        // Perform the for each, put, to draft, clear evaluation result, request evaluate, and get identifier calls
        // required by the request evaluation operation.

        this.simulatorPayloadControls.forEach (
            ( parameterIdentifier, payloadControl ) ->
                payloadDrafts.put ( parameterIdentifier, payloadControl.toDraft () )
        );
        clearEvaluationResult ();
        this.presenter.requestEvaluate ( eventDescriptor.getIdentifier (), payloadDrafts );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: clearEvaluationResult
    //
    // Description:
    //
    //   Performs the clear evaluation result operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void clearEvaluationResult ()
    {
        // Initialize the unavailable by applying text.

        String unavailable = text ( "simulator.result.not.available" );

        // Set the text on the evaluation outcome value.

        this.evaluationOutcomeValue.setText ( unavailable );
        this.evaluationActionValue.setText ( unavailable );
        this.evaluationRuleValue.setText ( unavailable );
        this.evaluationSpecificityValue.setText ( unavailable );
        this.evaluationServerLatencyValue.setText ( unavailable );
        this.evaluationRoundTripLatencyValue.setText ( unavailable );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readOnlyValueLabel
    //
    // Description:
    //
    //   Performs the read only value label operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Label readOnlyValueLabel ()
    {
        // Initialize the value label with a new label.

        Label valueLabel = new Label ();

        // Perform the set max width, set wrap text, add, and get style class calls required by the read only value
        // label operation.

        valueLabel.setMaxWidth ( Double.MAX_VALUE );
        valueLabel.setWrapText ( true );
        valueLabel.getStyleClass ().add ( "read-only-value" );

        // Return the value label to the caller.

        return valueLabel;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: renderStatus
    //
    // Description:
    //
    //   Performs the render status operation.
    //
    // Arguments:
    //
    //   session (ClientDocumentSession):
    //     The session to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void renderStatus ( ClientDocumentSession session )
    {
        // Perform the set text, text, is dirty, can push, size, get validation messages, and get content calls
        // required by the render status operation.

        this.documentStatusLabel.setText (
            text ( "status.document" )
                + ": "
                + ( session.isDirty () ? text ( "status.dirty" ) : text ( "status.clean" ) )
        );
        this.validationStatusLabel.setText (
            text ( "status.validation" )
                + ": "
                + (
                    session.canPush ()
                        ? text ( "status.valid" )
                        : session.getContent ().getValidationMessages ().size () + " error(s)"
                )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: renderSelection
    //
    // Description:
    //
    //   Performs the render selection operation.
    //
    // Arguments:
    //
    //   selection (Selection):
    //     The selection to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void renderSelection ( Selection selection )
    {
        // Handle the branch where selection kind equals selection kind simulator.

        if ( selection.getKind () == Selection.Kind.SIMULATOR )
        {
            // Perform the select and get selection model calls required by the render selection operation.

            this.detailTabs.getSelectionModel ().select ( this.simulatorTab );

            return;
        }

        // Perform the select and get selection model calls required by the render selection operation.

        this.detailTabs.getSelectionModel ().select ( this.editorTab );

        // Handle the branch where selection kind equals selection kind model.

        if ( selection.getKind () == Selection.Kind.MODEL )
        {
            // Perform the select and get selection model calls required by the render selection operation.

            this.modelTree.getSelectionModel ().select ( this.modelRootItem );

            return;
        }

        // Prepare the category identifier and entity identifier values needed by the render selection operation.

        String categoryIdentifier = selection.getCategoryIdentifier ().orElseThrow ();
        String entityIdentifier   = selection.getEntityIdentifier ().orElse ( null );

        // Perform the if present, find navigation item, select, and get selection model calls required by the render
        // selection operation.

        findNavigationItem ( categoryIdentifier, entityIdentifier ).ifPresent (
            treeItem -> this.modelTree.getSelectionModel ().select ( treeItem )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: renderUserGuide
    //
    // Description:
    //
    //   Performs the render context-sensitive help operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void renderUserGuide ()
    {
        // Stop this path and return its result when detail tabs is unavailable or model tree is unavailable or user
        // guide overview value is unavailable.

        if (
            this.detailTabs == null
                || this.modelTree == null
                || this.userGuideOverviewValue == null
        )
        {
            return;
        }

        // Prepare the simulator selected, selected item, category identifier, context, and resource prefix values
        // needed by the render context-sensitive help operation.

        boolean simulatorSelected =
            this.detailTabs.getSelectionModel ().getSelectedItem () == this.simulatorTab;
        TreeItem <NavigationItem> selectedItem =
            this.modelTree.getSelectionModel ().getSelectedItem ();
        String categoryIdentifier = selectedItem == null
            ? null
            : selectedItem.getValue ().getCategoryIdentifier ();
        ClientUserGuide.Context context = ClientUserGuide.resolve (
            categoryIdentifier,
            simulatorSelected
        );
        String resourcePrefix = "user.guide." + context.getLocalizationSuffix ();

        // Perform the set text, text, set image, get, and set accessible text calls required by the render
        // context-sensitive help
        // operation.

        this.userGuideOverviewValue.setText ( text ( resourcePrefix + ".overview" ) );
        this.userGuideStepsValue.setText ( text ( resourcePrefix + ".steps" ) );
        this.userGuideEquationImage.setImage (
            this.userGuideEquationImages.get ( this.currentTheme ).get ( context )
        );
        this.userGuideEquationImage.setAccessibleText (
            text ( resourcePrefix + ".equation.accessible" )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: renderCommandState
    //
    // Description:
    //
    //   Performs the render command state operation.
    //
    // Arguments:
    //
    //   session (ClientDocumentSession):
    //     The session to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void renderCommandState ( ClientDocumentSession session )
    {
        // Perform the set disable, is dirty, is present, get local path, can undo, can redo, can push, get kind, get
        // selection, and for each calls required by the render command state operation.

        this.saveMenuItem.setDisable (
            this.backgroundOperationRunning
                || ( !session.isDirty () && session.getLocalPath ().isPresent () )
        );
        this.saveAsMenuItem.setDisable ( this.backgroundOperationRunning );
        this.undoMenuItem.setDisable ( this.backgroundOperationRunning || !session.canUndo () );
        this.redoMenuItem.setDisable ( this.backgroundOperationRunning || !session.canRedo () );
        this.pushMenuItem.setDisable ( this.backgroundOperationRunning || !session.canPush () );
        this.deleteMenuItem.setDisable (
            this.backgroundOperationRunning
                || session.getSelection ().getKind () != Selection.Kind.ENTITY
        );
        this.operationSensitiveMenuItems.forEach (
            menuItem -> menuItem.setDisable ( this.backgroundOperationRunning )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: handleTreeSelection
    //
    // Description:
    //
    //   Performs the handle tree selection operation.
    //
    // Arguments:
    //
    //   selectedItem (TreeItem <NavigationItem>):
    //     The selected item to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void handleTreeSelection ( TreeItem <NavigationItem> selectedItem )
    {
        // Stop this path and return its result when rendering is true or selected item is unavailable.

        if ( this.rendering || selectedItem == null )
        {
            return;
        }

        // Initialize the navigation item by applying get value.

        NavigationItem navigationItem = selectedItem.getValue ();

        // Handle the branch where navigation item category identifier is unavailable.

        if ( navigationItem.getCategoryIdentifier () == null )
        {
            // Complete the handle tree selection step by calling select model.

            this.presenter.selectModel ();

            return;
        }

        // Handle the branch where navigation item entity identifier is unavailable.

        if ( navigationItem.getEntityIdentifier () == null )
        {
            // Perform the select category and get category identifier calls required by the handle tree selection
            // operation.

            this.presenter.selectCategory ( navigationItem.getCategoryIdentifier () );
        }

        // Handle the alternative path when the preceding condition is not satisfied.

        else
        {
            // Perform the select entity, get category identifier, and get entity identifier calls required by the
            // handle tree selection operation.

            this.presenter.selectEntity (
                navigationItem.getCategoryIdentifier (),
                navigationItem.getEntityIdentifier ()
            );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: findNavigationItem
    //
    // Description:
    //
    //   Performs the find navigation item operation.
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
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private Optional <TreeItem <NavigationItem>> findNavigationItem (
        String categoryIdentifier,
        String entityIdentifier
    )
    {
        // Process each category item supplied by model root item children.

        for ( TreeItem <NavigationItem> categoryItem : this.modelRootItem.getChildren () )
        {
            // Handle the branch where category identifier matches category item value category identifier.

            if ( categoryIdentifier.equals ( categoryItem.getValue ().getCategoryIdentifier () ) )
            {
                // Stop this path and return its result when entity identifier is unavailable.

                if ( entityIdentifier == null )
                {
                    // Return an optional containing the available value when entity identifier is unavailable.

                    return Optional.of ( categoryItem );
                }

                // Process each entity item supplied by category item children.

                for ( TreeItem <NavigationItem> entityItem : categoryItem.getChildren () )
                {
                    // Stop this path and return its result when entity identifier matches entity item value entity
                    // identifier.

                    if ( entityIdentifier.equals ( entityItem.getValue ().getEntityIdentifier () ) )
                    {
                        // Return an optional containing the available value when entity identifier matches entity item
                        // value entity identifier.

                        return Optional.of ( entityItem );
                    }
                }
            }
        }

        // Return an empty optional result because no value is available.

        return Optional.empty ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: presenterAction
    //
    // Description:
    //
    //   Performs the presenter action operation.
    //
    // Arguments:
    //
    //   actionIdentifier (String):
    //     The action identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private Runnable presenterAction ( String actionIdentifier )
    {
        // Return the presenter action result to the caller.

        return () ->
        {
            // Select the processing branch for action identifier.

            switch ( actionIdentifier )
            {
                // Handle "new" through this switch branch.

                case "new"             -> this.presenter.requestNew ();

                // Handle "open" through this switch branch.

                case "open"            -> this.presenter.requestOpen ();

                // Handle "save" through this switch branch.

                case "save"            -> this.presenter.requestSave ();

                // Handle "save-as" through this switch branch.

                case "save-as"         -> this.presenter.requestSaveAs ();

                // Handle "close" through this switch branch.

                case "close"           -> this.presenter.requestClose ();

                // Handle "validate" through this switch branch.

                case "validate"        -> this.presenter.requestValidate ();

                // Handle "push" through this switch branch.

                case "push"            -> this.presenter.requestPush ();

                // Handle "pull" through this switch branch.

                case "pull"            -> this.presenter.requestPull ();

                // Handle "exit" through this switch branch.

                case "exit"            -> this.presenter.requestExit ();

                // Handle "undo" through this switch branch.

                case "undo"            -> this.presenter.requestUndo ();

                // Handle "redo" through this switch branch.

                case "redo"            -> this.presenter.requestRedo ();

                // Handle "settings" through this switch branch.

                case "settings"        -> this.presenter.requestSettings ();

                // Handle "test-connection" through this switch branch.

                case "test-connection" -> this.presenter.requestTestConnection ();

                // Handle the default case through this switch branch.

                default                -> throw new IllegalArgumentException ( "Unknown client action: " + actionIdentifier );
            }
        };
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: menuItem
    //
    // Description:
    //
    //   Performs the menu item operation.
    //
    // Arguments:
    //
    //   resourceKey (String):
    //     The resource key to use.
    //
    //   action (Runnable):
    //     The action to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private MenuItem menuItem ( String resourceKey, Runnable action )
    {
        // Initialize the menu item by applying text.

        MenuItem menuItem = new MenuItem ( text ( resourceKey ) );

        // Perform the set on action and run calls required by the menu item operation.

        menuItem.setOnAction ( event -> action.run () );

        // Return the menu item to the caller.

        return menuItem;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createFileChooser
    //
    // Description:
    //
    //   Performs the create file chooser operation.
    //
    // Arguments:
    //
    //   title (String):
    //     The title to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private FileChooser createFileChooser ( String title )
    {
        // Initialize the file chooser with a new file chooser.

        FileChooser fileChooser = new FileChooser ();

        // Perform the set title, add, get extension filters, text, if present, load last directory, set initial
        // directory, and to file calls required by the create file chooser operation.

        fileChooser.setTitle ( title );
        fileChooser.getExtensionFilters ().add (
            new FileChooser.ExtensionFilter ( text ( "file.json.filter" ), "*.json" )
        );
        this.preferencesStore.loadLastDirectory ().ifPresent (
            directory -> fileChooser.setInitialDirectory ( directory.toFile () )
        );

        // Return the file chooser to the caller.

        return fileChooser;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: saveWindowPreferences
    //
    // Description:
    //
    //   Performs the save window preferences operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void saveWindowPreferences ()
    {
        // Perform the save window state, get x, get y, get width, get height, get divider positions, is expanded,
        // collect, map, filter, stream, get children, get category identifier, get value, and to collection calls
        // required by the save window preferences operation.

        this.preferencesStore.saveWindowState (
            new ClientPreferencesStore.WindowState (
                this.primaryStage.getX (),
                this.primaryStage.getY (),
                this.primaryStage.getWidth (),
                this.primaryStage.getHeight (),
                this.masterDetailSplitPane.getDividerPositions ()[ 0 ],
                this.masterDetailSplitPane.getDividerPositions ()[ 1 ],
                this.workspaceSplitPane.getDividerPositions ()[ 0 ],
                this.modelRootItem.isExpanded (),
                this.modelRootItem.getChildren ().stream ()
                    .filter ( TreeItem::isExpanded )
                    .map ( treeItem -> treeItem.getValue ().getCategoryIdentifier () )
                    .collect ( Collectors.toCollection ( java.util.LinkedHashSet::new ) )
            )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: editFocusedText
    //
    // Description:
    //
    //   Performs the edit focused text operation.
    //
    // Arguments:
    //
    //   actionIdentifier (String):
    //     The action identifier to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void editFocusedText ( String actionIdentifier )
    {
        // Initialize the focus owner by applying get focus owner and get scene.

        Node focusOwner = this.primaryStage.getScene ().getFocusOwner ();

        // Stop this path and return its result when focus owner is not a text input control.

        if ( !( focusOwner instanceof TextInputControl ) )
        {
            return;
        }

        TextInputControl textInputControl = (TextInputControl) focusOwner;

        // Select the processing branch for action identifier.

        switch ( actionIdentifier )
        {
            // Handle "cut" through this switch branch.

            case "cut"   -> textInputControl.cut ();

            // Handle "copy" through this switch branch.

            case "copy"  -> textInputControl.copy ();

            // Handle "paste" through this switch branch.

            case "paste" -> textInputControl.paste ();

            // Handle the default case through this switch branch.

            default      -> throw new IllegalArgumentException (
                "Unknown text edit action: " + actionIdentifier
            );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: applyTheme
    //
    // Description:
    //
    //   Performs the apply theme operation.
    //
    // Arguments:
    //
    //   theme (Theme):
    //     The theme to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void applyTheme ( Theme theme )
    {
        // Reject the operation when theme is unavailable.

        if ( theme == null )
        {
            throw new IllegalArgumentException ( "The application theme is required." );
        }

        this.currentTheme = theme;

        // Perform the remove all, get style class, add, set selected, get, save theme identifier, get identifier,
        // render context-sensitive help, report message, text, and get message resource key calls required by the
        // apply theme
        // operation.

        this.applicationRoot.getStyleClass ().removeAll (
            Theme.LIGHT.getStyleClass (),
            Theme.DARK.getStyleClass ()
        );
        this.applicationRoot.getStyleClass ().add ( theme.getStyleClass () );
        this.themeMenuItems.get ( theme ).setSelected ( true );
        this.preferencesStore.saveThemeIdentifier ( theme.getIdentifier () );
        renderUserGuide ();
        reportMessage (
            ClientView.MessageSeverity.INFO,
            "Appearance",
            text ( theme.getMessageResourceKey () )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: isVisibleWindowPosition
    //
    // Description:
    //
    //   Indicates whether visible window position.
    //
    // Arguments:
    //
    //   windowState (ClientPreferencesStore.WindowState):
    //     The window state to use.
    //
    // Returns:
    //
    //   `true` when the condition is satisfied; otherwise `false`.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static boolean isVisibleWindowPosition ( ClientPreferencesStore.WindowState windowState )
    {
        // Stop this path and return its result when double is not finite or double is not finite.

        if ( !Double.isFinite ( windowState.getX () ) || !Double.isFinite ( windowState.getY () ) )
        {
            // Return false for this outcome when double is not finite or double is not finite.

            return false;
        }

        double minimumVisibleExtent = 80.0;

        // Process each screen supplied by screen screens.

        for ( Screen screen : Screen.getScreens () )
        {
            // Prepare the bounds, horizontally visible, and vertically visible values needed by the is visible window
            // position operation.

            Rectangle2D bounds = screen.getVisualBounds ();
            boolean horizontallyVisible =
                windowState.getX () + minimumVisibleExtent <= bounds.getMaxX ()
                    && windowState.getX () + windowState.getWidth () - minimumVisibleExtent >= bounds.getMinX ();
            boolean verticallyVisible =
                windowState.getY () + minimumVisibleExtent <= bounds.getMaxY ()
                    && windowState.getY () + windowState.getHeight () - minimumVisibleExtent >= bounds.getMinY ();

            // Stop this path and return its result when horizontally visible is true and vertically visible is true.

            if ( horizontallyVisible && verticallyVisible )
            {
                // Return true for this outcome when horizontally visible is true and vertically visible is true.

                return true;
            }
        }

        // Return false for this outcome.

        return false;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: showAbout
    //
    // Description:
    //
    //   Performs the show about operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void showAbout ()
    {
        // Prepare the about dialog, application name label, version label, author label, description label, identity
        // content, and about content values needed by the show about operation.

        Dialog <ButtonType> aboutDialog = new Dialog <ButtonType> ();
        Label applicationNameLabel = new Label ( text ( "about.application.name" ) );
        Label versionLabel = new Label ( text ( "about.version" ) );
        Label authorLabel = new Label ( text ( "about.author" ) );
        Label descriptionLabel = new Label ( text ( "about.description" ) );
        VBox identityContent = new VBox ( 2.0, applicationNameLabel, versionLabel, authorLabel );
        VBox aboutContent = new VBox ( 12.0, identityContent, new Separator (), descriptionLabel );

        // Perform the add, get style class, set wrap text, set padding, set pref width, init owner, set title, text,
        // set header text, set content, get dialog pane, get button types, and show and wait calls required by the
        // show about operation.

        applicationNameLabel.getStyleClass ().add ( "about-application-name" );
        descriptionLabel.setWrapText ( true );
        aboutContent.setPadding ( new Insets ( 12.0 ) );
        aboutContent.setPrefWidth ( 480.0 );

        aboutDialog.initOwner ( this.primaryStage );
        aboutDialog.setTitle ( text ( "about.title" ) );
        aboutDialog.setHeaderText ( null );
        aboutDialog.getDialogPane ().setContent ( aboutContent );
        aboutDialog.getDialogPane ().getButtonTypes ().add (
            new ButtonType ( text ( "button.ok" ), ButtonBar.ButtonData.OK_DONE )
        );
        aboutDialog.showAndWait ();
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
    // Method: ensureJsonExtension
    //
    // Description:
    //
    //   Performs the ensure json extension operation.
    //
    // Arguments:
    //
    //   selectedPath (Path):
    //     The selected path to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Path ensureJsonExtension ( Path selectedPath )
    {
        // Initialize the file name by applying to string and get file name.

        String fileName = selectedPath.getFileName ().toString ();

        // Return the value selected according to file name to lower case locale root ends with succeeds.

        return fileName.toLowerCase ( Locale.ROOT ).endsWith ( ".json" )
            ? selectedPath
            : selectedPath.resolveSibling ( fileName + ".json" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createFormGrid
    //
    // Description:
    //
    //   Performs the create form grid operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    static GridPane createFormGrid ()
    {
        // Prepare the form grid, label column, and value column values needed by the create form grid operation.

        GridPane formGrid = new GridPane ();
        ColumnConstraints labelColumn = new ColumnConstraints ();
        ColumnConstraints valueColumn = new ColumnConstraints ();

        // Perform the set halignment, set min width, set hgrow, set fill width, set hgap, set vgap, add all, and get
        // column constraints calls required by the create form grid operation.

        labelColumn.setHalignment ( HPos.LEFT );
        labelColumn.setMinWidth ( FORM_LABEL_MINIMUM_WIDTH );
        valueColumn.setHgrow ( Priority.ALWAYS );
        valueColumn.setFillWidth ( true );
        formGrid.setHgap ( 12.0 );
        formGrid.setVgap ( 8.0 );
        formGrid.getColumnConstraints ().addAll ( labelColumn, valueColumn );

        // Return the form grid to the caller.

        return formGrid;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: addFormRow
    //
    // Description:
    //
    //   Performs the add form row operation.
    //
    // Arguments:
    //
    //   formGrid (GridPane):
    //     The form grid to use.
    //
    //   rowIndex (int):
    //     The row index to use.
    //
    //   labelText (String):
    //     The label text to use.
    //
    //   valueControl (Node):
    //     The value control to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    static Label addFormRow ( GridPane formGrid, int rowIndex, String labelText, Node valueControl )
    {
        // Initialize the field label with a new label.

        Label fieldLabel = new Label ( labelText );

        // Perform the set label for, set max width, add, get style class, add row, and set hgrow calls required by the
        // add form row operation.

        fieldLabel.setLabelFor ( valueControl );
        fieldLabel.setMaxWidth ( Double.MAX_VALUE );
        fieldLabel.getStyleClass ().add ( "form-label" );
        formGrid.addRow ( rowIndex, fieldLabel, valueControl );
        GridPane.setHgrow ( valueControl, Priority.ALWAYS );

        // Return the field label to the caller.

        return fieldLabel;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createGroupBox
    //
    // Description:
    //
    //   Performs the create group box operation.
    //
    // Arguments:
    //
    //   title (String):
    //     The title to use.
    //
    //   content (Node):
    //     The content to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    static TitledPane createGroupBox ( String title, Node content )
    {
        // Initialize the group box with a new titled pane.

        TitledPane groupBox = new TitledPane ( title, content );

        // Perform the set collapsible, set max width, add, and get style class calls required by the create group box
        // operation.

        groupBox.setCollapsible ( false );
        groupBox.setMaxWidth ( Double.MAX_VALUE );
        groupBox.getStyleClass ().add ( "group-box" );

        // Return the group box to the caller.

        return groupBox;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createGroupActionRow
    //
    // Description:
    //
    //   Performs the create group action row operation.
    //
    // Arguments:
    //
    //   buttons (Button...):
    //     The buttons to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    static HBox createGroupActionRow ( Button... buttons )
    {
        // Initialize the action row with a new h box.

        HBox actionRow = new HBox ( 10.0, buttons );

        // Perform the set alignment, add, and get style class calls required by the create group action row operation.

        actionRow.setAlignment ( Pos.CENTER_RIGHT );
        actionRow.getStyleClass ().add ( "group-action-row" );

        // Return the action row to the caller.

        return actionRow;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createPageActionFooter
    //
    // Description:
    //
    //   Performs the create page action footer operation.
    //
    // Arguments:
    //
    //   buttons (Button...):
    //     The buttons to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    static HBox createPageActionFooter ( Button... buttons )
    {
        // Initialize the footer with a new h box.

        HBox footer = new HBox ( 10.0, buttons );

        // Perform the set alignment, add, and get style class calls required by the create page action footer
        // operation.

        footer.setAlignment ( Pos.CENTER_RIGHT );
        footer.getStyleClass ().add ( "page-action-footer" );

        // Return the footer to the caller.

        return footer;
    }

    //=================================================================================================================
    // User Defined Data Types
    //=================================================================================================================

    //*****************************************************************************************************************
    // Class: SimulatorPayloadControl
    //
    // Description:
    //
    //   Provides the simulator payload control behavior.
    //
    //*****************************************************************************************************************

    private final class SimulatorPayloadControl
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final ComboBox <PayloadValueState> stateSelector;
        private final Node valueControl;
        private final HBox node;
        private boolean externallyDisabled;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
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
        //-------------------------------------------------------------------------------------------------------------

        Node getNode ()
        {
            // Return the node to the caller.

            return this.node;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: SimulatorPayloadControl
        //
        // Description:
        //
        //   Creates the SimulatorPayloadControl instance from the supplied values.
        //
        // Arguments:
        //
        //   fieldDescriptor (PayloadFieldDescriptor):
        //     The field descriptor to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        SimulatorPayloadControl ( PayloadFieldDescriptor fieldDescriptor )
        {
            // Construct the combo box instance required by the simulator payload control operation.

            this.stateSelector = new ComboBox <PayloadValueState> ();

            // Perform the set all, get items, values, set value, set min width, set converter, and text calls required
            // by the simulator payload control operation.

            this.stateSelector.getItems ().setAll ( PayloadValueState.values () );
            this.stateSelector.setValue ( PayloadValueState.OMITTED );
            this.stateSelector.setMinWidth ( 118.0 );
            this.stateSelector.setConverter (
                new StringConverter <PayloadValueState> ()
                {
                    @Override
                    public String toString ( PayloadValueState state )
                    {
                        // Stop this path and return its result when state is unavailable.

                        if ( state == null )
                        {
                            // Return the to string text to the caller when state is unavailable.

                            return "";
                        }

                        // Return the result produced by text.

                        return text (

                            // Select the result branch for state.

                            switch ( state )
                            {
                                // Handle omitted through this switch branch.

                                case OMITTED  -> "simulator.payload.state.omitted";

                                // Handle null through this switch branch.

                                case NULL     -> "simulator.payload.state.null";

                                // Handle concrete through this switch branch.

                                case CONCRETE -> "simulator.payload.state.concrete";
                            }
                        );
                    }

                    @Override
                    public PayloadValueState fromString ( String value )
                    {
                        throw new UnsupportedOperationException (
                            "Simulator payload states are selected, not parsed."
                        );
                    }
                }
            );

            // Handle the branch where field descriptor type matches "boolean".

            if ( fieldDescriptor.getType ().equals ( "BOOLEAN" ) )
            {
                // Initialize the boolean value with a new combo box.

                ComboBox <String> booleanValue = new ComboBox <String> ();

                // Perform the set all, get items, set value, and set max width calls required by the simulator payload
                // control operation.

                booleanValue.getItems ().setAll ( "true", "false" );
                booleanValue.setValue ( "false" );
                booleanValue.setMaxWidth ( Double.MAX_VALUE );
                this.valueControl = booleanValue;
            }

            // Handle the alternative where field descriptor type matches "enum".

            else if ( fieldDescriptor.getType ().equals ( "ENUM" ) )
            {
                // Initialize the enumeration value with a new combo box.

                ComboBox <String> enumerationValue = new ComboBox <String> ();

                // Perform the set all, get items, and get enumeration values calls required by the simulator payload
                // control operation.

                enumerationValue.getItems ().setAll ( fieldDescriptor.getEnumerationValues () );

                // Handle the branch where enumeration value items contains values.

                if ( !enumerationValue.getItems ().isEmpty () )
                {
                    // Perform the set value, get, and get items calls required by the simulator payload control
                    // operation.

                    enumerationValue.setValue ( enumerationValue.getItems ().get ( 0 ) );
                }

                // Set the max width on the enumeration value.

                enumerationValue.setMaxWidth ( Double.MAX_VALUE );
                this.valueControl = enumerationValue;
            }

            // Handle the alternative path when the preceding condition is not satisfied.

            else
            {
                // Initialize the text value by applying equals and get type.

                TextField textValue = new TextField (
                    fieldDescriptor.getType ().equals ( "INTEGER" ) ? "0" : ""
                );

                this.valueControl = textValue;
            }

            // Construct the h box instance required by the simulator payload control operation.

            this.node = new HBox ( 10.0, this.stateSelector, this.valueControl );

            // Perform the set alignment, set hgrow, add listener, value property, and update disabled state calls
            // required by the simulator payload control operation.

            this.node.setAlignment ( Pos.CENTER_LEFT );
            HBox.setHgrow ( this.valueControl, Priority.ALWAYS );
            this.stateSelector.valueProperty ().addListener (
                ( observableValue, oldState, newState ) -> updateDisabledState ()
            );
            updateDisabledState ();
        }

        //=============================================================================================================
        // Mutators
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
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
        //-------------------------------------------------------------------------------------------------------------

        void setDisabled ( boolean disabled )
        {
            this.externallyDisabled = disabled;

            // Refresh the target from the current application state.

            updateDisabledState ();
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

        PayloadValueDraft toDraft ()
        {
            // Initialize the state by applying get value.

            PayloadValueState state = this.stateSelector.getValue ();

            // Return the to draft result to the caller.

            return switch ( state )
            {
                // Handle omitted through this switch branch.

                case OMITTED  -> PayloadValueDraft.omitted ();

                // Handle null through this switch branch.

                case NULL     -> PayloadValueDraft.nullValue ();

                // Handle concrete through this switch branch.

                case CONCRETE -> PayloadValueDraft.concrete ( valueText () );
            };
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: valueText
        //
        // Description:
        //
        //   Performs the value text operation.
        //
        // Returns:
        //
        //   The result of the operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        private String valueText ()
        {
            // Stop this path and return its result when value control is a text field.

            if ( this.valueControl instanceof TextField )
            {
                // Return the result produced by get text when value control is a text field.

                return ( (TextField) this.valueControl ).getText ();
            }

            // Handle the branch where value control is a combo box ?.

            if ( this.valueControl instanceof ComboBox <?> )
            {
                // Initialize the value by applying get value.

                Object value = ( (ComboBox <?>) this.valueControl ).getValue ();

                // Return the value selected according to value is unavailable when value control is a combo box ?.

                return value == null ? "" : value.toString ();
            }

            throw new IllegalStateException ( "Unsupported simulator value control." );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: updateDisabledState
        //
        // Description:
        //
        //   Performs the update disabled state operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        private void updateDisabledState ()
        {
            // Perform the set disable and get value calls required by the update disabled state operation.

            this.stateSelector.setDisable ( this.externallyDisabled );
            this.valueControl.setDisable (
                this.externallyDisabled
                    || this.stateSelector.getValue () != PayloadValueState.CONCRETE
            );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Enum: SettingsGroup
    //
    // Description:
    //
    //   Enumerates the supported settings group values.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private enum SettingsGroup
    {
        //=============================================================================================================
        // Constants
        //=============================================================================================================

        APPEARANCE ( "settings.group.appearance" ),
        SERVER     ( "settings.group.server"     );

        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String resourceKey;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getResourceKey
        //
        // Description:
        //
        //   Returns the resource key.
        //
        // Returns:
        //
        //   The resource key.
        //
        //-------------------------------------------------------------------------------------------------------------

        String getResourceKey ()
        {
            // Return the resource key to the caller.

            return this.resourceKey;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: SettingsGroup
        //
        // Description:
        //
        //   Creates the SettingsGroup instance from the supplied values.
        //
        // Arguments:
        //
        //   resourceKey (String):
        //     The resource key to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        SettingsGroup ( String resourceKey )
        {
            this.resourceKey = resourceKey;
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Enum: Theme
    //
    // Description:
    //
    //   Enumerates the supported theme values.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private enum Theme
    {
        //=============================================================================================================
        // Constants
        //=============================================================================================================

        LIGHT ( "light", "theme-light", "settings.theme.light", "messages.theme.light" ),
        DARK  ( "dark",  "theme-dark",  "settings.theme.dark",  "messages.theme.dark"  );

        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String identifier;
        private final String styleClass;
        private final String settingsResourceKey;
        private final String messageResourceKey;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getIdentifier
        //
        // Description:
        //
        //   Returns the identifier.
        //
        // Returns:
        //
        //   The identifier.
        //
        //-------------------------------------------------------------------------------------------------------------

        String getIdentifier ()
        {
            // Return the identifier to the caller.

            return this.identifier;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getStyleClass
        //
        // Description:
        //
        //   Returns the style class.
        //
        // Returns:
        //
        //   The style class.
        //
        //-------------------------------------------------------------------------------------------------------------

        String getStyleClass ()
        {
            // Return the style class to the caller.

            return this.styleClass;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getSettingsResourceKey
        //
        // Description:
        //
        //   Returns the settings resource key.
        //
        // Returns:
        //
        //   The settings resource key.
        //
        //-------------------------------------------------------------------------------------------------------------

        String getSettingsResourceKey ()
        {
            // Return the settings resource key to the caller.

            return this.settingsResourceKey;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getMessageResourceKey
        //
        // Description:
        //
        //   Returns the message resource key.
        //
        // Returns:
        //
        //   The message resource key.
        //
        //-------------------------------------------------------------------------------------------------------------

        String getMessageResourceKey ()
        {
            // Return the message resource key to the caller.

            return this.messageResourceKey;
        }

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: fromIdentifier
        //
        // Description:
        //
        //   Creates a Theme instance for the from identifier case.
        //
        // Arguments:
        //
        //   identifier (String):
        //     The identifier to use.
        //
        // Returns:
        //
        //   The resulting Theme instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        static Theme fromIdentifier ( String identifier )
        {
            // Return the value selected according to "dark" matches identifier.

            return "dark".equals ( identifier ) ? DARK : LIGHT;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: Theme
        //
        // Description:
        //
        //   Creates the Theme instance from the supplied values.
        //
        // Arguments:
        //
        //   identifier (String):
        //     The identifier to use.
        //
        //   styleClass (String):
        //     The style class to use.
        //
        //   settingsResourceKey (String):
        //     The settings resource key to use.
        //
        //   messageResourceKey (String):
        //     The message resource key to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        Theme (
            String identifier,
            String styleClass,
            String settingsResourceKey,
            String messageResourceKey
        )
        {
            this.identifier          = identifier;
            this.styleClass          = styleClass;
            this.settingsResourceKey = settingsResourceKey;
            this.messageResourceKey  = messageResourceKey;
        }
    }

    //*****************************************************************************************************************
    // Class: NavigationItem
    //
    // Description:
    //
    //   Provides the navigation item behavior.
    //
    //*****************************************************************************************************************

    private static final class NavigationItem
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String categoryIdentifier;
        private final String entityIdentifier;
        private final String displayText;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

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

        String getCategoryIdentifier ()
        {
            // Return the category identifier to the caller.

            return this.categoryIdentifier;
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

        String getEntityIdentifier ()
        {
            // Return the entity identifier to the caller.

            return this.entityIdentifier;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: NavigationItem
        //
        // Description:
        //
        //   Creates the NavigationItem instance from the supplied values.
        //
        // Arguments:
        //
        //   categoryIdentifier (String):
        //     The category identifier to use.
        //
        //   entityIdentifier (String):
        //     The entity identifier to use.
        //
        //   displayText (String):
        //     The display text to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private NavigationItem ( String categoryIdentifier, String entityIdentifier, String displayText )
        {
            this.categoryIdentifier = categoryIdentifier;
            this.entityIdentifier   = entityIdentifier;
            this.displayText        = displayText;
        }

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: root
        //
        // Description:
        //
        //   Creates a NavigationItem instance for the root case.
        //
        // Arguments:
        //
        //   displayText (String):
        //     The display text to use.
        //
        // Returns:
        //
        //   The resulting NavigationItem instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        static NavigationItem root ( String displayText )
        {
            // Return a newly constructed navigation item containing the operation result.

            return new NavigationItem ( null, null, displayText );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: category
        //
        // Description:
        //
        //   Creates a NavigationItem instance for the category case.
        //
        // Arguments:
        //
        //   categoryIdentifier (String):
        //     The category identifier to use.
        //
        //   displayText (String):
        //     The display text to use.
        //
        // Returns:
        //
        //   The resulting NavigationItem instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        static NavigationItem category ( String categoryIdentifier, String displayText )
        {
            // Return a newly constructed navigation item containing the operation result.

            return new NavigationItem ( categoryIdentifier, null, displayText );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: entity
        //
        // Description:
        //
        //   Creates a NavigationItem instance for the entity case.
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
        //   The resulting NavigationItem instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        static NavigationItem entity ( String categoryIdentifier, String entityIdentifier )
        {
            // Return a newly constructed navigation item containing the operation result.

            return new NavigationItem ( categoryIdentifier, entityIdentifier, entityIdentifier );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: toString
        //
        // Description:
        //
        //   Creates the string representation of this value.
        //
        // Returns:
        //
        //   The string representation of this value.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public String toString ()
        {
            // Return the display text to the caller.

            return this.displayText;
        }
    }
}
