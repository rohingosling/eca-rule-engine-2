//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies the version-controlled native build, reachability metadata, resource, and executable identity contracts
//   without requiring a native toolchain during the JVM test suite.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import com.rohingosling.eca.client.ClientMain;

import org.junit.jupiter.api.Test;

//*********************************************************************************************************************
// Class: NativeIntegrationTest
//
// Description:
//
//   Verifies the version-controlled native build, reachability metadata, resource, and executable identity contracts
//   without requiring a native toolchain during the JVM test suite.
//
//*********************************************************************************************************************

final class NativeIntegrationTest
{
    private static final Path PROJECT_DIRECTORY = Path.of (
        System.getProperty ( "eca.project.root" )
    );

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: NAT_001_NAT_008_nativeBuildContract_isStrictAndReportable
    //
    // Description:
    //
    //   Verifies that nat 001 nat 008 native build contract is strict and reportable.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void NAT_001_NAT_008_nativeBuildContract_isStrictAndReportable () throws IOException
    {
        // Prepare the server project, client project, server build, client build, and client cvtres values needed by
        // the nat 001 nat 008 native build contract is strict and reportable operation.

        String serverProject = read ( "eca-server/pom.xml" );
        String clientProject = read ( "eca-client/pom.xml" );
        String serverBuild   = read ( ".github/scripts/native/build-native-server.bat" );
        String clientBuild   = read ( ".github/scripts/native/build-native-client.bat" );
        String clientCvtres  = read ( ".github/scripts/native/cvtres_wrapper.c" );
        String serverMain    = read ( "eca-server/src/main/java/com/rohingosling/eca/server/ServerMain.java" );

        // Verify the nat 001 nat 008 native build contract is strict and reportable scenario against its expected
        // outcome.

        assertThat ( serverProject )
            .contains ( "<imageName>eca-server</imageName>" )
            .contains ( "--no-fallback" )
            .contains ( "-march=compatibility" )
            .contains ( "--exact-reachability-metadata=com.rohingosling.eca" )
            .contains ( "<artifactId>picocli</artifactId>" )
            .contains ( "<artifactId>jackson-annotations</artifactId>" )
            .contains ( "<excluded>true</excluded>" )
            .contains ( "BuildOutputJSONFile" )
            .contains ( "DashboardDump" )
            .contains ( "DashboardAll" )
            .contains ( "NativeLinkerOption=${eca.server.resource}" );
        assertThat ( clientProject )
            .contains ( "<finalName>eca-client</finalName>" )
            .contains ( "--no-fallback" )
            .contains ( "<javafxStaticSdkVersion>${javafx.static.sdk.version}</javafxStaticSdkVersion>" )
            .contains ( "-H:-NativeArchitecture" )
            .contains ( "-H:CPUFeatures=SSE2" )
            .contains ( "DashboardDump" );
        assertThat ( serverBuild )
            .contains ( "build-windows-resources.ps1\" -Target Server" )
            .contains ( "eca-server.obj" )
            .contains ( "eca-server\\target\\eca-server.exe" );
        assertThat ( clientBuild )
            .contains ( "build-windows-resources.ps1\" -Target Client" )
            .contains ( "prepare-javafx-static-sdk.ps1" )
            .contains ( "ECA_CLIENT_VERSION_RESOURCE" )
            .contains ( "eca-client\\target\\gluonfx\\x86_64-windows\\eca-client.exe" );
        assertThat ( clientCvtres )
            .contains ( "IconGroup.obj" )
            .contains ( "ECA_CLIENT_VERSION_RESOURCE" )
            .contains ( "ECA_REAL_CVTRES" )
            .contains ( "_spawnv" );
        assertThat ( serverMain )
            .contains ( "arguments.length == 0" )
            .contains ( "new String[] { \"start\" }" );
        assertThat ( serverProject + clientProject + serverBuild + clientBuild )
            .doesNotContainIgnoringCase ( "jpackage" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: NAT_010_reachabilityMetadata_coversReviewedProductionResources
    //
    // Description:
    //
    //   Verifies that nat 010 reachability metadata covers reviewed production resources.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void NAT_010_reachabilityMetadata_coversReviewedProductionResources () throws IOException
    {
        // Prepare the server reflection, service fields, client resources, client main, client styles, client
        // messages, equation generator, and client jni values needed by the nat 010 reachability metadata covers
        // reviewed production resources operation.

        String serverReflection = read (
            "eca-server/src/main/resources/META-INF/native-image/com.rohingosling.eca/"
                + "eca-server/reflect-config.json"
        );
        String serviceFields = read (
            "eca-server/src/main/resources/META-INF/native-image/com.rohingosling.eca/"
                + "eca-server-service-fields/reflect-config.json"
        );
        String clientResources = read (
            "eca-client/src/main/resources/META-INF/native-image/com.rohingosling.eca/"
                + "eca-client/resource-config.json"
        );
        String clientMain = read (
            "eca-client/src/main/java/com/rohingosling/eca/client/ClientMain.java"
        );
        String clientConsole = read (
            "eca-client/src/main/java/com/rohingosling/eca/client/ClientConsole.java"
        );
        String clientEntityEditor = read (
            "eca-client/src/main/java/com/rohingosling/eca/client/ClientEntityEditor.java"
        );
        String clientFluentIcons = read (
            "eca-client/src/main/java/com/rohingosling/eca/client/ClientFluentIcons.java"
        );
        String clientModalDialogs = read (
            "eca-client/src/main/java/com/rohingosling/eca/client/ClientModalDialogs.java"
        );
        String clientPreferences = read (
            "eca-client/src/main/java/com/rohingosling/eca/client/ClientPreferencesStore.java"
        );
        String clientProject = read ( "eca-client/pom.xml" );
        String clientStyles = read (
            "eca-client/src/main/resources/com/rohingosling/eca/client/client.css"
        );
        String clientMessages = read (
            "eca-client/src/main/resources/com/rohingosling/eca/client/messages.properties"
        );
        String equationGenerator = read ( ".github/scripts/user-guide/generate-equations.ps1" );
        String fluentNotice = read (
            "eca-client/src/main/resources/com/rohingosling/eca/client/notices/fluent-ui-system-icons.txt"
        );
        String applicationLicence = read (
            "eca-client/src/main/resources/com/rohingosling/eca/client/notices/eca-rule-engine-license.txt"
        );
        String clientJni = read (
            "eca-client/src/main/resources/META-INF/substrate/config/"
                + "jniconfig-x86_64-windows.json"
        );

        // Verify the nat 010 reachability metadata covers reviewed production resources scenario against its expected
        // outcome.

        assertThat ( serverReflection )
            .contains ( "io.helidon.webserver.LoomServer__ServiceDescriptor" )
            .contains ( "\"allDeclaredConstructors\": true" )
            .doesNotContain ( "\"allDeclaredMethods\": true" );
        assertThat ( serviceFields )
            .contains ( "\"allDeclaredFields\": true" )
            .doesNotContain ( "\"allDeclaredMethods\": true" );
        assertThat ( clientResources )
            .contains ( "messages.properties" )
            .contains ( "client.css" )
            .contains ( "eca-rule-engine.png" )
            .contains ( "user-guide" )
            .contains ( "icons" )
            .contains ( "fluent-ui-system-icons.txt" )
            .contains ( "eca-rule-engine-license.txt" )
            .contains ( "com.rohingosling.eca.client.messages" );
        assertThat ( clientMain )
            .contains ( "text ( \"outline.title\" )" )
            .contains ( "this.modelTree.setMaxSize ( Double.MAX_VALUE, Double.MAX_VALUE )" )
            .contains ( "outlinePane.setMaxSize ( Double.MAX_VALUE, Double.MAX_VALUE )" )
            .contains ( "guideScrollPane.setMaxSize ( Double.MAX_VALUE, Double.MAX_VALUE )" )
            .contains ( "userGuidePane.setMaxSize ( Double.MAX_VALUE, Double.MAX_VALUE )" )
            .contains ( "userGuidePane.setMinWidth ( 420.0 )" )
            .contains ( "getStyleClass ().add ( \"workspace-panel\" )" )
            .contains ( "setFitWidth ( 0.0 )" )
            .contains ( "setFitHeight ( 0.0 )" )
            .contains ( "setFitToHeight ( true )" )
            .contains ( "new MenuBar ( fileMenu, editMenu, viewMenu, helpMenu )" )
            .contains ( "ListView <SettingsGroup> settingsGroupList" )
            .contains ( "setSelectionMode ( SelectionMode.SINGLE )" )
            .contains ( "SettingsGroup.APPEARANCE" )
            .contains ( "SettingsGroup.SERVER" )
            .contains ( "this.userGuideEquationImages.get ( this.currentTheme ).get ( context )" )
            .contains ( "Menu themeMenu = new Menu ( text ( \"menu.view.theme\" ) );" )
            .contains ( "ToggleGroup themeToggleGroup = new ToggleGroup ();" )
            .contains ( "themeMenuItem.setToggleGroup ( themeToggleGroup );" )
            .contains ( "themeMenuItem.setSelected ( theme == this.currentTheme );" )
            .contains ( "themeMenuItem.setOnAction ( event -> applyTheme ( theme ) );" )
            .contains ( "this.themeMenuItems.get ( theme ).setSelected ( true );" )
            .contains ( "tlsResponse.statusCode () < 100" )
            .contains ( "tlsResponse.statusCode () > 599" )
            .contains (
                "pullMenuItem,\n"
                    + "            new SeparatorMenuItem (),\n"
                    + "            testConnectionMenuItem,\n"
                    + "            settingsMenuItem,\n"
                    + "            new SeparatorMenuItem (),\n"
                    + "            exitMenuItem"
            )
            .contains (
                "viewMenu.getItems ().addAll (\n"
                    + "            new SeparatorMenuItem (),\n"
                    + "            simulatorMenuItem,\n"
                    + "            new SeparatorMenuItem (),\n"
                    + "            expandAllMenuItem,\n"
                    + "            collapseAllMenuItem,\n"
                    + "            new SeparatorMenuItem (),\n"
                    + "            clearConsoleMenuItem,\n"
                    + "            this.consoleVisibilityMenuItem,\n"
                    + "            new SeparatorMenuItem (),\n"
                    + "            themeMenu\n"
                    + "        );"
            )
            .contains (
                "private void clearConsole ()\n"
                    + "    {\n"
                    + "        this.console.clear ();\n"
                    + "    }"
            )
            .contains ( "this.applicationRoot.setTop ( new VBox ( createMenuBar (), createToolBar () ) );" )
            .contains ( "ClientFluentIcons.create ( ClientFluentIcons.TOOLBAR_SIZE, \"dark_theme\" )" )
            .contains (
                "ToolBar toolBar = new ToolBar (\n"
                    + "            newButton,\n"
                    + "            openButton,\n"
                    + "            this.saveToolbarButton,\n"
                    + "            this.saveAsToolbarButton,\n"
                    + "            new Separator ( Orientation.VERTICAL ),\n"
                    + "            pullButton,\n"
                    + "            this.pushToolbarButton,\n"
                    + "            new Separator ( Orientation.VERTICAL ),\n"
                    + "            this.undoToolbarButton,\n"
                    + "            this.redoToolbarButton,\n"
                    + "            new Separator ( Orientation.VERTICAL ),\n"
                    + "            this.editorToolbarButton,\n"
                    + "            this.simulatorToolbarButton,\n"
                    + "            new Separator ( Orientation.VERTICAL ),\n"
                    + "            expandAllButton,\n"
                    + "            collapseAllButton,\n"
                    + "            new Separator ( Orientation.VERTICAL ),\n"
                    + "            themeButton\n"
                    + "        );"
            )
            .contains ( "public DirtyDocumentDecision chooseDirtyDocumentDecision" )
            .contains ( "text ( \"button.save.and.continue\" )" )
            .contains ( "text ( \"button.discard.and.continue\" )" )
            .contains ( "ButtonBar.ButtonData.OTHER" )
            .contains ( "text ( \"button.close\" )" )
            .contains ( "modelOverviewContent.setTop ( overviewGroup );" )
            .contains ( "setNavigationNodeExpansion ( this.modelRootItem, true );" )
            .contains ( "setNavigationNodeExpansion ( this.modelRootItem, false );" )
            .contains ( "https://zenodo.org/records/21804777" )
            .contains ( "new Label ( text ( \"about.technical.note.label\" ) )" )
            .contains ( "new Hyperlink ( text ( \"about.technical.note.url\" ) )" )
            .contains ( "new Label ( text ( \"about.version\" ) + \" \" + text ( \"about.author\" ) )" )
            .contains ( "new VBox ( 2.0, applicationNameLabel, versionAuthorLabel )" )
            .contains ( "identityRow.setAlignment ( Pos.CENTER_LEFT );" )
            .contains ( "new HBox ( 4.0, technicalNoteLabel, technicalNoteLink )" )
            .contains ( "new Tab ( text ( \"about.tab.licences\" ), licencesContent )" )
            .contains ( "new Tab ( text ( \"about.tab.release.notes\" ), releaseNotesContent )" )
            .contains ( "new TabPane ( licencesTab, releaseNotesTab )" )
            .contains ( "TabPane.TabClosingPolicy.UNAVAILABLE" )
            .contains ( "text ( \"about.release.notes\" )" )
            .contains ( "releaseNotesLabel.setLabelFor ( releaseNotesText )" )
            .contains ( "licencesContent.getStyleClass ().add ( \"about-licences-content\" )" )
            .contains ( "releaseNotesContent.getStyleClass ().add ( \"about-release-notes-content\" )" )
            .contains ( "aboutTabs.getStyleClass ().add ( \"about-tab-pane\" )" )
            .contains ( "readBundledText ( \"notices/eca-rule-engine-license.txt\" )" )
            .contains ( "readBundledText ( \"notices/fluent-ui-system-icons.txt\" )" )
            .doesNotContain ( "showThirdPartyNotices" )
            .contains ( "errorDialog.showAndWait ();" )
            .contains ( "this.presenter.applyConnectionSettings ( settings );" )
            .contains ( "Label connectionStatusPrefixLabel = new Label ( text ( \"status.server\" ) + \":\" );" )
            .contains ( "HBox.setHgrow ( statusSpacer, Priority.ALWAYS );" )
            .contains ( "ClientFluentIcons.create ( ClientFluentIcons.MENU_SIZE, iconName )" )
            .doesNotContain ( "Menu serverMenu" )
            .doesNotContain (
                "validateMenuItem,\n"
                    + "            new SeparatorMenuItem (),\n"
                    + "            this.pushMenuItem,"
            )
            .doesNotContain ( "userGuideEquationImage.fitWidthProperty ().bind" )
            .doesNotContain ( "equationCard.setMinHeight" )
            .doesNotContain ( "equationCard.setPrefHeight" )
            .doesNotContain ( "equationCard.setMaxHeight" );
        assertThat ( clientStyles )
            .contains ( ".group-box.workspace-panel > .content" )
            .contains ( "-fx-padding: 0;" )
            .contains ( ".user-guide-content" )
            .contains ( "-fx-padding: 14;" )
            .contains ( "-eca-equation-background: transparent;" )
            .contains ( "-fx-background: #f1f3f5;" )
            .contains ( "-fx-accent: #1663ad;" )
            .contains ( "-fx-selection-bar: #cfe7ff;" )
            .contains ( "-eca-dropdown-selection: #cfe7ff;" )
            .contains ( "-eca-menu-surface: #f4f6f8;" )
            .contains ( "-eca-console-surface: #f8fafc;" )
            .contains ( "-fx-background: #1b1b1b;" )
            .contains ( "-fx-accent: #f0f0f0;" )
            .contains ( "-fx-selection-bar: #3d3d3d;" )
            .contains ( "-eca-dropdown-selection: #3d3d3d;" )
            .contains ( "-eca-menu-surface: #222222;" )
            .contains ( "-eca-console-surface: #161616;" )
            .contains ( ".root.theme-light .tree-view:focused" )
            .contains ( ".root.theme-dark .tree-view:focused" )
            .contains ( ".console-table .column-header-background" )
            .contains ( "-fx-max-height: 0;" )
            .contains ( ".console-table .table-cell" )
            .contains ( "-fx-table-cell-border-color: transparent;" )
            .contains ( ".console-context-button" )
            .contains ( "-fx-alignment: center;" )
            .contains ( "-fx-padding: 1 10 1 10;" )
            .contains ( "-fx-text-alignment: center;" )
            .contains ( ".about-technical-note-label" )
            .contains ( ".about-technical-note-link" )
            .contains ( ".console-severity-symbol" )
            .contains ( "-fx-pref-width: 17;" )
            .contains ( ".context-menu .theme-submenu > .right-container" )
            .contains ( ".command-toolbar" )
            .contains ( ".menu-bar > .container > .menu-button" )
            .contains ( ".root.theme-dark .context-menu .separator:horizontal .line" )
            .contains ( ".combo-box-popup .list-view .list-cell:filled:selected" )
            .contains ( "-fx-background-color: -eca-dropdown-selection;" )
            .contains ( ".toolbar-theme-button > .arrow-button" )
            .contains ( ".toolbar-theme-button > .label" )
            .contains ( "-fx-alignment: center;" )
            .contains ( ".text-input," )
            .contains ( ".button," )
            .contains ( ".page-action-footer" )
            .contains ( ".about-tab-pane" )
            .contains ( "-fx-border-width: 0 0 2 0;" )
            .contains ( ".about-licences-content," )
            .contains ( ".about-release-notes-content" )
            .contains ( "-fx-padding: 10 2 2 2;" )
            .contains ( ".page-action-footer-scroll" )
            .contains ( ".modal-owner-dimmed" )
            .doesNotContain ( "-fx-selection-bar-text:" );
        assertThat ( clientMessages )
            .contains ( "menu.file.test.connection=Test Connection" )
            .contains ( "menu.file.settings=Settings" )
            .contains ( "menu.view.clear.console=Clear Console" )
            .contains ( "menu.view.expand.all=Expand All" )
            .contains ( "menu.view.collapse.all=Collapse All" )
            .contains ( "menu.view.console=Console" )
            .contains ( "menu.view.theme=Theme" )
            .contains ( "messages.group=Console" )
            .contains ( "console.filter.messages=Messages" )
            .contains ( "console.filter.warnings=Warnings" )
            .contains ( "console.filter.errors=Errors" )
            .contains ( "console.follow.tail=Follow Tail" )
            .contains ( "status.model.id=Model ID" )
            .contains ( "status.server=Server" )
            .contains ( "button.save.and.continue=Save and Continue" )
            .contains ( "button.discard.and.continue=Discard and Continue" )
            .contains ( "button.close=Close" )
            .contains ( "about.author=(Rohin Gosling 2024)" )
            .contains ( "settings.group.appearance=Appearance" )
            .contains ( "settings.group.server=Server" )
            .doesNotContain ( "Messages and Diagnostics" )
            .doesNotContain ( "Open-source notices" )
            .doesNotContain ( "menu.server=" );
        assertThat ( clientConsole )
            .contains ( "MAXIMUM_ENTRY_COUNT = 1_000" )
            .contains ( "new FilteredList <> ( this.entries )" )
            .contains ( "console.column.time" )
            .contains ( "console.column.severity" )
            .contains ( "console.column.code" )
            .contains ( "console.column.source" )
            .contains ( "console.column.text" )
            .contains ( "console.column.context" )
            .contains ( "KeyCode.C && keyEvent.isShortcutDown ()" )
            .contains ( "KeyCode.ENTER || keyEvent.getCode () == KeyCode.SPACE" )
            .contains ( "mouseEvent.getClickCount () == 2" )
            .contains ( "console-severity-symbol" )
            .contains ( "entry.getSeverity ().getSymbol ()" )
            .contains ( "contextButton.setMinWidth ( Region.USE_PREF_SIZE );" )
            .contains ( "contextButton.setMaxWidth ( Region.USE_PREF_SIZE );" )
            .contains ( "contextButton.prefWidth ( Region.USE_COMPUTED_SIZE )" )
            .contains ( "getTableColumn ().setPrefWidth (" )
            .contains ( "entry.activateContext ()" );
        assertThat ( clientEntityEditor )
            .contains ( "ClientFluentIcons.TOOLBAR_SIZE, \"arrow_up\"" )
            .contains ( "ClientFluentIcons.TOOLBAR_SIZE, \"arrow_down\"" )
            .contains (
                "ClientMain.createPageActionFooter (\n"
                    + "            moveUpButton,\n"
                    + "            moveDownButton,\n"
                    + "            addButton,\n"
                    + "            duplicateButton,\n"
                    + "            deleteButton,\n"
                    + "            editButton\n"
                    + "        );"
            )
            .doesNotContain ( "deleteButton.getStyleClass ().add ( \"danger-button\" )" );
        assertThat ( clientFluentIcons )
            .contains ( "static final int MENU_SIZE    = 16;" )
            .contains ( "static final int TOOLBAR_SIZE = 20;" )
            .contains ( "SVGPath icon = new SVGPath ();" )
            .contains ( "icons/" )
            .contains ( "_regular.svg" );
        assertThat ( clientModalDialogs )
            .contains ( "Button closeButton = new Button ( \"\\u00d7\" );" )
            .contains ( "closeButton.setAccessibleText ( closeLabel );" )
            .contains ( "closeButton.setOnAction ( actionEvent -> dialog.close () );" )
            .contains ( "buttonBar.setButtonOrder ( ButtonBar.BUTTON_ORDER_NONE );" )
            .contains ( "dimOwner ( owner );" )
            .contains ( "restoreOwner ( owner );" )
            .contains ( "safeControl.requestFocus ();" )
            .contains ( "invoker.requestFocus ();" )
            .contains ( "danger-button" );
        assertThat ( clientPreferences )
            .contains ( "CONSOLE_VISIBLE_KEY" )
            .contains ( "CONSOLE_FOLLOW_TAIL_KEY" )
            .contains ( "loadConsoleVisible" )
            .contains ( "saveConsoleFollowTail" );
        assertThat ( clientProject )
            .contains ( "<list>.*\\\\.svg$</list>" )
            .contains ( "<list>.*\\\\.txt$</list>" );
        assertThat ( fluentNotice )
            .contains ( "Fluent UI System Icons" )
            .contains ( "MIT License" )
            .contains ( "Copyright (c) 2020 Microsoft Corporation" );
        assertThat ( applicationLicence )
            .contains ( "MIT License" )
            .contains ( "Copyright (c) 2025 Rohin Gosling" );

        String[] expectedFluentIcons = {
            "16/ic_fluent_document_add_16_regular.svg",
            "16/ic_fluent_save_multiple_16_regular.svg",
            "16/ic_fluent_clipboard_task_list_16_regular.svg",
            "16/ic_fluent_plug_connected_16_regular.svg",
            "16/ic_fluent_document_16_regular.svg",
            "16/ic_fluent_database_16_regular.svg",
            "16/ic_fluent_box_16_regular.svg",
            "16/ic_fluent_flash_16_regular.svg",
            "16/ic_fluent_checkmark_circle_16_regular.svg",
            "16/ic_fluent_clipboard_bullet_list_16_regular.svg",
            "16/ic_fluent_gavel_16_regular.svg",
            "16/ic_fluent_arrow_expand_all_16_regular.svg",
            "16/ic_fluent_arrow_collapse_all_16_regular.svg",
            "20/ic_fluent_document_add_20_regular.svg",
            "20/ic_fluent_arrow_expand_all_20_regular.svg",
            "20/ic_fluent_arrow_collapse_all_20_regular.svg",
            "20/ic_fluent_save_copy_20_regular.svg",
            "20/ic_fluent_dark_theme_20_regular.svg",
            "20/ic_fluent_arrow_up_20_regular.svg",
            "20/ic_fluent_arrow_down_20_regular.svg",
            "20/ic_fluent_copy_add_20_regular.svg"
        };

        for ( String expectedFluentIcon : expectedFluentIcons )
        {
            assertThat (
                Files.isRegularFile (
                    PROJECT_DIRECTORY.resolve (
                        "eca-client/src/main/resources/com/rohingosling/eca/client/icons/" + expectedFluentIcon
                    )
                )
            ).as ( expectedFluentIcon ).isTrue ();
        }
        assertThat ( equationGenerator )
            .contains ( "-r 96" )
            .contains ( "-transp" )
            .contains ( "\\nopagecolor" )
            .contains ( "ForegroundColor = '17212B'" )
            .contains ( "ForegroundColor = 'F3F3F3'" )
            .contains ( "\\fontsize{12pt}{14.4pt}\\selectfont" )
            .contains ( "'-dark'" )
            .contains ( "Set-EquationImagePerimeter" )
            .doesNotContain ( "\\pagecolor[HTML]" )
            .doesNotContain ( "BackgroundColor" )
            .doesNotContain ( "\\Large" );
        assertThat ( clientJni )
            .contains ( "com.sun.glass.ui.win.WinAccessible" )
            .contains ( "com.sun.glass.ui.win.WinTextRangeProvider" )
            .contains ( "com.sun.glass.ui.win.WinVariant" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: NAT_010_applicationIdentityResources_areCheckedInAndLoadable
    //
    // Description:
    //
    //   Verifies that nat 010 application identity resources are checked in and loadable.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void NAT_010_applicationIdentityResources_areCheckedInAndLoadable () throws IOException
    {
        // Prepare the icon bytes, gluon icon bytes, and application icon values needed by the nat 010 application
        // identity resources are checked in and loadable operation.

        byte[] iconBytes = Files.readAllBytes (
            PROJECT_DIRECTORY.resolve ( "assets/images/eca-rule-engine.ico" )
        );
        byte[] gluonIconBytes = Files.readAllBytes (
            PROJECT_DIRECTORY.resolve ( "eca-client/src/windows/assets/icon.ico" )
        );
        BufferedImage applicationIcon = ImageIO.read (
            PROJECT_DIRECTORY.resolve (
                "eca-client/src/main/resources/com/rohingosling/eca/client/eca-rule-engine.png"
            ).toFile ()
        );

        // Verify the nat 010 application identity resources are checked in and loadable scenario against its expected
        // outcome.

        assertThat ( iconBytes )
            .hasSizeGreaterThan ( 1000 )
            .startsWith (
                ( byte ) 0,
                ( byte ) 0,
                ( byte ) 1,
                ( byte ) 0,
                ( byte ) 7,
                ( byte ) 0
            );
        assertThat ( gluonIconBytes ).isEqualTo ( iconBytes );
        assertThat ( applicationIcon ).isNotNull ();
        assertThat ( applicationIcon.getWidth () ).isEqualTo ( 256 );
        assertThat ( applicationIcon.getHeight () ).isEqualTo ( 256 );
        assertThat ( alpha ( applicationIcon.getRGB ( 0, 0 ) ) ).isZero ();
        assertThat ( alpha ( applicationIcon.getRGB ( 20, 25 ) ) ).isGreaterThan ( 0 );
        assertThat ( applicationIcon.getRGB ( 128, 100 ) ).isEqualTo ( 0xFFFFFFFF );
        assertThat ( applicationIcon.getRGB ( 150, 69 ) ).isEqualTo ( 0xFF000000 );
        assertThat ( countNonGrayscalePixels ( applicationIcon ) ).isZero ();
        assertThat ( read ( "assets/windows/eca-server.rc" ) )
            .contains ( "FILEVERSION 2,1,0,0" )
            .contains ( "\"FileVersion\", \"2.1.0\\0\"" )
            .contains ( "\"ProductVersion\", \"2.1.0\\0\"" )
            .contains ( "eca-rule-engine.ico" )
            .contains ( "\"eca-server.exe\\0\"" );
        assertThat ( read ( "assets/windows/eca-client.rc" ) )
            .contains ( "FILEVERSION 2,1,0,0" )
            .contains ( "\"FileVersion\", \"2.1.0\\0\"" )
            .contains ( "\"ProductVersion\", \"2.1.0\\0\"" )
            .contains ( "\"eca-client.exe\\0\"" );

        // Open the scoped resources for the protected operation and close them automatically afterward.

        try ( InputStream iconStream = ClientMain.class.getResourceAsStream (
            "eca-rule-engine.png"
        ) )
        {
            // Verify the nat 010 application identity resources are checked in and loadable scenario against its
            // expected outcome.

            assertThat ( iconStream ).isNotNull ();
            assertThat ( iconStream.readNBytes ( 8 ) )
                .containsExactly (
                    ( byte ) 0x89,
                    ( byte ) 0x50,
                    ( byte ) 0x4E,
                    ( byte ) 0x47,
                    ( byte ) 0x0D,
                    ( byte ) 0x0A,
                    ( byte ) 0x1A,
                    ( byte ) 0x0A
                );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: alpha
    //
    // Description:
    //
    //   Returns the alpha component of the supplied ARGB pixel.
    //
    // Arguments:
    //
    //   pixel (int):
    //     The ARGB pixel.
    //
    // Returns:
    //
    //   The alpha component.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static int alpha ( int pixel )
    {
        // Return the calculated alpha result.

        return ( pixel >>> 24 ) & 0xFF;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: countNonGrayscalePixels
    //
    // Description:
    //
    //   Counts the visible pixels whose red, green, and blue components are not equal.
    //
    // Arguments:
    //
    //   image (BufferedImage):
    //     The image to inspect.
    //
    // Returns:
    //
    //   The number of visible non-grayscale pixels.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static int countNonGrayscalePixels ( BufferedImage image )
    {
        int nonGrayscalePixelCount = 0;

        // Repeat the loop while y is less than image height.

        for ( int y = 0; y < image.getHeight (); y++ )
        {
            // Repeat the loop while x is less than image width.

            for ( int x = 0; x < image.getWidth (); x++ )
            {
                // Initialize the pixel by applying get rgb.

                int pixel = image.getRGB ( x, y );
                int red   = ( pixel >>> 16 ) & 0xFF;
                int green = ( pixel >>> 8 ) & 0xFF;
                int blue  = pixel & 0xFF;

                // Handle the branch where alpha pixel exceeds 0 and red differs from green or green differs from blue.

                if ( alpha ( pixel ) > 0 && ( red != green || green != blue ) )
                {
                    nonGrayscalePixelCount++;
                }
            }
        }

        // Return the non grayscale pixel count to the caller.

        return nonGrayscalePixelCount;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: nativeVerificationScripts_coverArtifactAndPerformanceGates
    //
    // Description:
    //
    //   Verifies that native verification scripts cover artifact and performance gates.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void nativeVerificationScripts_coverArtifactAndPerformanceGates () throws IOException
    {
        // Prepare the artifact verification, performance, continuous integration, and dependency inventory values
        // needed by the native verification scripts cover artifact and performance gates operation.

        String artifactVerification = read ( ".github/scripts/native/verify-native-artifacts.ps1" );
        String performance           = read ( ".github/scripts/native/measure-native-performance.ps1" );
        String continuousIntegration = read ( ".github/workflows/ci.yml" );
        String staticRuntimeAudit    = read ( ".github/scripts/native/audit-native-runtime.ps1" );
        String staticSdkPreparation  = read ( ".github/scripts/native/prepare-javafx-static-sdk.ps1" );
        String cleanSandboxHost      = read ( ".github/scripts/native/verify-clean-windows-sandbox.ps1" );
        String cleanSandboxGuest     = read ( ".github/scripts/native/invoke-clean-windows-acceptance.ps1" );
        String releaseContract       = read ( ".github/scripts/release/verify-release-contract.ps1" );

        // Verify the native verification scripts cover artifact and performance gates scenario against its expected
        // outcome.

        assertThat ( artifactVerification )
            .contains ( "/DEPENDENTS" )
            .contains ( "Get-FileHash" )
            .contains ( "Get-AuthenticodeSignature" )
            .contains ( "Get-VisualCppRuntimeEvidence" )
            .contains ( "Get-MsvcToolsetVersion" )
            .contains ( "RegistryView]::Registry64" )
            .contains ( "O=Microsoft Corporation" )
            .contains ( "MSVCP140_2.dll" )
            .contains ( "System32" )
            .contains ( "--native-smoke" )
            .contains ( "--native-smoke-file-chooser=true" )
            .contains ( "--native-smoke-phase=read-preferences" )
            .contains ( "--native-smoke-ui-automation=true" )
            .contains ( "UIAutomationClient" )
            .contains ( "native-smoke-server-url" )
            .contains ( "native-smoke-tls-url" )
            .contains ( "eca-server.exe" )
            .contains ( "eca-client.exe" )
            .contains ( "unapprovedRuntimeDependencies" )
            .contains ( "Assert-AllowedLoadedModules" )
            .contains ( "Assert-MachineTargetEvidence" )
            .contains ( "server-loaded-modules.txt" )
            .contains ( "client-loaded-modules.txt" )
            .contains ( "visual-cpp-runtime.txt" );
        assertThat ( staticRuntimeAudit )
            .contains ( "/DIRECTIVES" )
            .contains ( "MSVCRT" )
            .contains ( "MSVCPRT" )
            .contains ( "static-runtime-audit.md" );
        assertThat ( staticSdkPreparation )
            .contains ( "21-ea+11.3" )
            .contains ( "E3BB86AA840695164C3D5ED634AAD7A7C89ECC311FD509E5ED4A427BDD1B1ED9" );
        assertThat ( cleanSandboxHost )
            .contains ( "WindowsSandbox.exe" )
            .contains ( "vmmemWindowsSandbox" )
            .contains ( "PrepareOnly" )
            .contains ( "ShutdownWhenComplete" )
            .contains ( "candidate-manifest.json" )
            .contains ( "invoke-clean-windows-acceptance.ps1" )
            .contains ( "clean-windows-11.md" );
        assertThat ( cleanSandboxGuest )
            .contains ( "https://aka.ms/vc14/vc_redist.x64.exe" )
            .contains ( "Get-AuthenticodeSignature" )
            .contains ( "RegistryView]::Registry64" )
            .contains ( "VerifiedAndReputablePolicyState" )
            .contains ( "Unblock-File" )
            .contains ( "application-control-events.txt" )
            .contains ( "--native-smoke-ui-automation=true" )
            .contains ( "No temporary runtime extraction" );
        assertThat ( performance )
            .contains ( "10000" )
            .contains ( "MeasuredRequestCount" )
            .contains ( "RequestCount 100" )
            .contains ( "replacementStopwatch" )
            .contains ( "native-performance.md" );
        assertThat ( releaseContract )
            .contains ( "(?<Version>[^\\r\\n]+)\\r?$" );
        assertThat ( continuousIntegration )
            .contains ( "runs-on: windows-2025" )
            .contains ( "prepare-javafx-static-sdk.ps1" )
            .contains ( "https://aka.ms/vc14/vc_redist.x64.exe" )
            .contains ( "Get-AuthenticodeSignature" )
            .contains ( "verify-native-artifacts.ps1" )
            .contains ( "-UseAsciiTemporaryPath" )
            .contains ( "-SkipClientLaunch" )
            .contains ( "measure-native-performance.ps1" )
            .contains ( "-Filter \"dumpbin.exe\"" )
            .contains ( "name: Publish verified tag release" )
            .contains ( "needs: native" )
            .doesNotContain ( "runs-on: [self-hosted" )
            .doesNotContain ( "eca-clean-windows-11" );

        // Verify the private dependency inventory when that internal document is present in the current distribution.

        if ( Files.isRegularFile ( PROJECT_DIRECTORY.resolve ( "docs/dependency-license-inventory.md" ) ) )
        {
            // Check the dependency inventory against its native verification evidence.

            assertThat ( read ( "docs/dependency-license-inventory.md" ) )
                .contains ( "Production native baseline" )
                .contains ( "base_sbom.json" )
                .contains ( "GluonFX `used_packages`" )
                .contains ( "verify-native-artifacts.ps1" );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: read
    //
    // Description:
    //
    //   Performs the read operation.
    //
    // Arguments:
    //
    //   projectRelativePath (String):
    //     The project relative path to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String read ( String projectRelativePath ) throws IOException
    {
        // Return the result produced by replace.

        return Files.readString (
            PROJECT_DIRECTORY.resolve ( projectRelativePath ),
            StandardCharsets.UTF_8
        ).replace ( "\r\n", "\n" );
    }
}
