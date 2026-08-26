// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    Application
// Version: 2.0.0
// Date:    2026-08-04
// Author:  Rohin Gosling
//
// Description:
//
//   Composes the web SDI document lifecycle, model editors, accessible shell, diagnostics, and status presentation.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { useEffect, useMemo, useRef, useState } from "react";
import type { CSSProperties } from "react";

import { AboutDialog } from "./components/AboutDialog";
import {
    ConsolePanel,
    MAXIMUM_CONSOLE_ENTRY_COUNT,
    appendConsoleEntry,
    createConsoleEntry,
    type ConsoleContext,
    type ConsoleEntry,
    type ConsoleFilterState,
    type ConsoleSeverity,
} from "./components/ConsolePanel";
import { DirtyDocumentDialog } from "./components/DirtyDocumentDialog";
import { DocumentEditor } from "./components/DocumentEditor";
import { MessageDialog } from "./components/MessageDialog";
import { MenuBar } from "./components/MenuBar";
import type { MenuDefinition, MenuEntry, MenuIcon } from "./components/MenuBar";
import { NavigationTree } from "./components/NavigationTree";
import { SettingsDialog } from "./components/SettingsDialog";
import { SimulatorPanel } from "./components/SimulatorPanel";
import type { GuideContext } from "./components/NavigationTree";
import { Splitter } from "./components/Splitter";
import { Tabs } from "./components/Tabs";
import type { TabIdentifier } from "./components/Tabs";
import { Toolbar } from "./components/Toolbar";
import type { ToolbarEntry } from "./components/Toolbar";
import {
    GroupBox,
    StatusBar,
    UserGuidePanel,
} from "./components/WorkspacePanels";
import {
    BrowserDocumentFileService,
    applyDocumentEdit,
    createLoadedDocumentSession,
    createNewDocumentSession,
    createPulledDocumentSession,
    documentDisplayName,
    documentIsDirty,
    documentWindowTitle,
    markDocumentSaved,
    redoDocumentEdit,
    selectDocumentCategory,
    selectDocumentEntity,
    selectDocumentModel,
    undoDocumentEdit,
    type DocumentSelection,
    type DocumentSession,
} from "./document";
import { text } from "./localization/messages";
import type { MessageKey } from "./localization/messages";
import {
    AuthoringModelJsonCodec,
    applyEntityDraft,
    applyModelDetails,
    deleteEntity,
    duplicateEntity,
    reorderEntity,
    type DeleteResult,
    type EditResult,
    type EntityDraft,
    type ModelCategory,
    type PayloadValueDraft,
} from "./model";
import {
    calculateShellLayoutBounds,
    loadShellPreferences,
    saveShellPreferences,
} from "./preferences";
import type { ShellLayoutBounds, Theme } from "./preferences";
import {
    BuiltInWorkerTransport,
    FetchTransport,
    GatewayFailure,
    ServerGateway,
    gatewayFailureMessage,
    type EvaluatedOccurrence,
    type ServerSettings,
} from "./server";
import {
    createOccurrence,
    SimulatorDraftFailure,
    type SimulatorEvent,
} from "./simulator";

interface ShellStyle extends CSSProperties
{
    "--console-panel-height": string;
    "--outline-width": string;
    "--user-guide-width": string;
}

interface ViewportDimensions
{
    readonly width: number;
    readonly height: number;
}

interface BackgroundOperation
{
    readonly controller: AbortController;
    readonly label: string;
}

type ConnectionState = "STARTING" | "READY" | "UNTESTED" | "FAILED";

const HORIZONTAL_SPLITTER_WIDTH_TOTAL = 12;
const DETAIL_PANE_MINIMUM_WIDTH       = 200;
const TECHNICAL_NOTE_URL              = "https://zenodo.org/records/21804777";

const CATEGORY_MENU_ITEMS: readonly {
    readonly context: Exclude <GuideContext, "model" | "simulator">;
    readonly labelKey: MessageKey;
}[] =
[
    { context: "parameters",     labelKey: "tree.parameters"     },
    { context: "payloads",       labelKey: "tree.payloads"       },
    { context: "events",         labelKey: "tree.events"         },
    { context: "conditions",     labelKey: "tree.conditions"     },
    { context: "condition-sets", labelKey: "tree.condition-sets" },
    { context: "actions",        labelKey: "tree.actions"        },
    { context: "rules",          labelKey: "tree.rules"          },
];

const MENU_ICON_NAMES: Readonly <Record <string, string>> =
{
    "file-new": "ic_fluent_document_add_16_regular.svg",
    "file-open": "ic_fluent_folder_open_16_regular.svg",
    "file-save": "ic_fluent_save_16_regular.svg",
    "file-save-as": "ic_fluent_save_multiple_16_regular.svg",
    "file-close": "ic_fluent_document_dismiss_16_regular.svg",
    "file-validate": "ic_fluent_clipboard_task_list_16_regular.svg",
    "file-pull": "ic_fluent_arrow_download_16_regular.svg",
    "file-push": "ic_fluent_arrow_upload_16_regular.svg",
    "file-test-connection": "ic_fluent_plug_connected_16_regular.svg",
    "file-settings": "ic_fluent_settings_16_regular.svg",
    "file-exit": "ic_fluent_arrow_exit_16_regular.svg",
    "edit-undo": "ic_fluent_arrow_undo_16_regular.svg",
    "edit-redo": "ic_fluent_arrow_redo_16_regular.svg",
    "edit-cut": "ic_fluent_cut_16_regular.svg",
    "edit-copy": "ic_fluent_copy_16_regular.svg",
    "edit-paste": "ic_fluent_clipboard_paste_16_regular.svg",
    "edit-delete": "ic_fluent_delete_16_regular.svg",
    "view-parameters": "ic_fluent_database_16_regular.svg",
    "view-payloads": "ic_fluent_box_16_regular.svg",
    "view-events": "ic_fluent_flash_16_regular.svg",
    "view-conditions": "ic_fluent_checkmark_circle_16_regular.svg",
    "view-condition-sets": "ic_fluent_clipboard_bullet_list_16_regular.svg",
    "view-actions": "ic_fluent_play_16_regular.svg",
    "view-rules": "ic_fluent_gavel_16_regular.svg",
    "view-simulator": "ic_fluent_play_16_regular.svg",
    "view-expand-all": "ic_fluent_arrow_expand_all_16_regular.svg",
    "view-collapse-all": "ic_fluent_arrow_collapse_all_16_regular.svg",
    "view-clear-console": "ic_fluent_broom_16_regular.svg",
    "view-console": "ic_fluent_code_16_regular.svg",
    "view-theme": "ic_fluent_weather_moon_16_regular.svg",
    "help-technical-note": "ic_fluent_book_open_16_regular.svg",
    "help-github": "ic_fluent_code_16_regular.svg",
    "help-about": "ic_fluent_info_16_regular.svg",
};

const MENU_SHORTCUTS: Readonly <Record <string, string>> =
{
    "file-new": "Ctrl+N",
    "file-open": "Ctrl+O",
    "file-save": "Ctrl+S",
    "file-save-as": "Ctrl+Shift+S",
    "file-close": "Ctrl+W",
    "edit-undo": "Ctrl+Z",
    "edit-redo": "Ctrl+Y",
    "edit-cut": "Ctrl+X",
    "edit-copy": "Ctrl+C",
    "edit-paste": "Ctrl+V",
};

function fluentIcon ( name: string ): MenuIcon
{
    return { name };
}

function separator (): MenuEntry
{
    return { kind: "separator" };
}

function fullyExpandedTreeItems (): ReadonlySet <string>
{
    return new Set ( [ "model", ...CATEGORY_MENU_ITEMS.map ( category => category.context ) ] );
}

function readViewportDimensions (): ViewportDimensions
{
    return {
        width: window.innerWidth,
        height: window.innerHeight,
    };
}

function clamp ( value: number, minimum: number, maximum: number ): number
{
    return Math.min ( maximum, Math.max ( minimum, value ) );
}

function selectedContext ( selection: DocumentSelection ): Exclude <GuideContext, "simulator">
{
    return selection.kind === "model" ? "model" : selection.category;
}

function selectedEntityIdentifier ( session: DocumentSession ): string | null
{
    return session.selection.kind === "entity" ? session.selection.entityIdentifier : null;
}

function errorMessage ( error: unknown ): string
{
    return error instanceof Error ? error.message : String ( error );
}

function operationWasCancelled ( error: unknown ): boolean
{
    return ( error instanceof DOMException && error.name === "AbortError" )
        || ( error instanceof GatewayFailure && error.category === "CANCELLED" );
}

function serverFailureIndicatesDisconnection ( error: unknown ): boolean
{
    return !( error instanceof GatewayFailure )
        || error.category === "CONNECTION"
        || error.category === "TIMEOUT"
        || error.category === "PROTOCOL";
}

function suggestedFileName ( session: DocumentSession ): string
{
    const localName = session.localName;

    if ( localName !== null )
    {
        return localName.toLowerCase ().endsWith ( ".json" ) ? localName : `${localName}.json`;
    }

    return `${session.content.authoringModel.modelId || "eca-model"}.json`;
}

export function Application ()
{
    const [ viewportDimensions, setViewportDimensions ] = useState <ViewportDimensions> ( readViewportDimensions );
    const layoutBounds = useMemo <ShellLayoutBounds> (
        () => calculateShellLayoutBounds ( viewportDimensions.width, viewportDimensions.height ),
        [ viewportDimensions.height, viewportDimensions.width ]
    );
    const initialPreferences = useMemo ( () => loadShellPreferences ( layoutBounds ), [] );
    const fileService = useMemo ( () => new BrowserDocumentFileService (), [] );
    const modelCodec = useMemo ( () => new AuthoringModelJsonCodec (), [] );
    const builtInTransport = useMemo ( () => new BuiltInWorkerTransport (), [] );
    const builtInGateway = useMemo ( () => new ServerGateway ( builtInTransport ), [ builtInTransport ] );
    const [ theme, setTheme ] = useState <Theme> ( initialPreferences.theme );
    const [ consoleVisible, setConsoleVisible ] = useState ( initialPreferences.consoleVisible );
    const [ followConsoleTail, setFollowConsoleTail ] = useState ( initialPreferences.followConsoleTail );
    const [ outlineWidth, setOutlineWidth ] = useState ( initialPreferences.outlineWidth );
    const [ userGuideWidth, setUserGuideWidth ] = useState ( initialPreferences.userGuideWidth );
    const [ diagnosticsHeight, setDiagnosticsHeight ] = useState ( initialPreferences.diagnosticsHeight );
    const [ expandedTreeItems, setExpandedTreeItems ] = useState <ReadonlySet <string>> (
        new Set ( initialPreferences.expandedTreeItems )
    );
    const [ entityEditorRequestSequence, setEntityEditorRequestSequence ] = useState ( 0 );
    const [ documentSession, setDocumentSession ] = useState ( createNewDocumentSession );
    const [ activeTab, setActiveTab ] = useState <TabIdentifier> ( "editor" );
    const consoleSequenceReference = useRef ( 1 );
    const [ consoleEntries, setConsoleEntries ] = useState <readonly ConsoleEntry[]> (
        [ createConsoleEntry ( "Application", text ( "web.diagnostics.ready" ), 0 ) ]
    );
    const [ consoleFilters, setConsoleFilters ] = useState <ConsoleFilterState> (
        { error: true, message: true, warning: true }
    );
    const [ serverSettings, setServerSettings ] = useState <ServerSettings> (
        {
            target: initialPreferences.serverTarget,
            serverURL: initialPreferences.serverURL,
            connectTimeoutSeconds: initialPreferences.connectTimeoutSeconds,
            requestTimeoutSeconds: initialPreferences.requestTimeoutSeconds,
            bearerToken: "",
        }
    );
    const serverTargetReference = useRef ( serverSettings.target );
    const [ connectionState, setConnectionState ] = useState <ConnectionState> (
        initialPreferences.serverTarget === "built-in" ? "STARTING" : "UNTESTED"
    );
    const [ builtInModelIdentifier, setBuiltInModelIdentifier ] = useState <string | null> ( null );
    const [ builtInModelRevision, setBuiltInModelRevision ] = useState <string | null> ( null );
    const [ serverRevision, setServerRevision ] = useState <string | null> ( null );
    const [ localRevision, setLocalRevision ] = useState <string | null> ( null );
    const [ evaluation, setEvaluation ] = useState <EvaluatedOccurrence | null> ( null );
    const [ aboutOpen, setAboutOpen ] = useState ( false );
    const [ settingsOpen, setSettingsOpen ] = useState ( false );
    const [ messageDialog, setMessageDialog ] = useState <{
        readonly body: string;
        readonly severity: ConsoleSeverity;
    } | null> ( null );
    const [ dirtyDialogAction, setDirtyDialogAction ] = useState <string | null> ( null );
    const [ backgroundOperation, setBackgroundOperation ] = useState <BackgroundOperation | null> ( null );
    const backgroundOperationReference = useRef <BackgroundOperation | null> ( null );
    const pendingDiscardReference = useRef <( () => void ) | null> ( null );
    const storageWarningReportedReference = useRef ( false );
    const currentContext = selectedContext ( documentSession.selection );
    const currentEntityIdentifier = selectedEntityIdentifier ( documentSession );
    const dirty = documentIsDirty ( documentSession );
    const documentValid = !documentSession.content.validationErrors;
    const displayedConsoleEntries = consoleEntries;

    useEffect (
        () =>
        {
            function updateViewportDimensions (): void
            {
                setViewportDimensions ( readViewportDimensions () );
            }

            window.addEventListener ( "resize", updateViewportDimensions );

            return () => window.removeEventListener ( "resize", updateViewportDimensions );
        },
        []
    );

    useEffect (
        () =>
        {
            setOutlineWidth (
                currentWidth => clamp (
                    currentWidth,
                    layoutBounds.outlineWidthMinimum,
                    layoutBounds.outlineWidthMaximum
                )
            );
            setUserGuideWidth (
                currentWidth => clamp (
                    currentWidth,
                    layoutBounds.userGuideWidthMinimum,
                    layoutBounds.userGuideWidthMaximum
                )
            );
            setDiagnosticsHeight (
                currentHeight => clamp (
                    currentHeight,
                    layoutBounds.diagnosticsHeightMinimum,
                    layoutBounds.diagnosticsHeightMaximum
                )
            );
        },
        [ layoutBounds ]
    );

    useEffect (
        () =>
        {
            if ( viewportDimensions.width <= 1150 )
            {
                return;
            }

            const maximumCombinedSidePaneWidth = viewportDimensions.width
                - HORIZONTAL_SPLITTER_WIDTH_TOTAL
                - DETAIL_PANE_MINIMUM_WIDTH;

            if ( outlineWidth + userGuideWidth > maximumCombinedSidePaneWidth )
            {
                setUserGuideWidth (
                    clamp (
                        maximumCombinedSidePaneWidth - outlineWidth,
                        layoutBounds.userGuideWidthMinimum,
                        layoutBounds.userGuideWidthMaximum
                    )
                );
            }
        },
        [
            layoutBounds.userGuideWidthMaximum,
            layoutBounds.userGuideWidthMinimum,
            outlineWidth,
            userGuideWidth,
            viewportDimensions.width,
        ]
    );

    useEffect (
        () =>
        {
            const preferencesSaved = saveShellPreferences (
                {
                    consoleVisible,
                    followConsoleTail,
                    theme,
                    outlineWidth,
                    userGuideWidth,
                    diagnosticsHeight,
                    expandedTreeItems: Array.from ( expandedTreeItems ).sort (),
                    serverTarget: serverSettings.target,
                    serverURL: serverSettings.serverURL,
                    connectTimeoutSeconds: serverSettings.connectTimeoutSeconds,
                    requestTimeoutSeconds: serverSettings.requestTimeoutSeconds,
                }
            );

            if ( !preferencesSaved && !storageWarningReportedReference.current )
            {
                storageWarningReportedReference.current = true;
                reportActivity (
                    "Browser",
                    "Preference storage is unavailable; settings remain page-session-only.",
                    "warning",
                    "PREFERENCE_STORAGE_UNAVAILABLE"
                );
            }
        },
        [
            consoleVisible,
            diagnosticsHeight,
            expandedTreeItems,
            followConsoleTail,
            outlineWidth,
            serverSettings,
            theme,
            userGuideWidth,
        ]
    );

    useEffect (
        () =>
        {
            let current = true;

            void builtInTransport.ready ().then (
                readyState =>
                {
                    if ( !current )
                    {
                        return;
                    }

                    setBuiltInModelIdentifier ( readyState.modelIdentifier );
                    setBuiltInModelRevision ( readyState.modelRevision );

                    if ( serverTargetReference.current === "built-in" )
                    {
                        setConnectionState ( "READY" );
                        setServerRevision ( readyState.modelRevision );
                    }

                    reportActivity ( "Built-in Server", `Ready with ${readyState.modelIdentifier}.` );
                },
                error =>
                {
                    if ( !current )
                    {
                        return;
                    }

                    if ( serverTargetReference.current === "built-in" )
                    {
                        setConnectionState ( "FAILED" );
                    }

                    reportActivity (
                        "Built-in Server",
                        `Startup failed: ${gatewayFailureMessage ( error )}`,
                        "error",
                        "BUILT_IN_SERVER_STARTUP_FAILED"
                    );
                }
            );

            return () =>
            {
                current = false;
            };
        },
        [ builtInTransport ]
    );

    useEffect (
        () =>
        {
            let current = true;

            setLocalRevision ( null );
            setEvaluation ( null );
            void modelCodec.revision ( documentSession.content.authoringModel ).then (
                revision =>
                {
                    if ( current )
                    {
                        setLocalRevision ( revision );
                    }
                }
            );

            return () =>
            {
                current = false;
            };
        },
        [ documentSession.content, modelCodec ]
    );

    useEffect (
        () =>
        {
            document.title = documentWindowTitle ( documentSession );
        },
        [ documentSession ]
    );

    useEffect (
        () =>
        {
            function protectDirtyDocument ( event: BeforeUnloadEvent ): void
            {
                if ( !dirty )
                {
                    return;
                }

                event.preventDefault ();
                event.returnValue = "";
            }

            window.addEventListener ( "beforeunload", protectDirtyDocument );

            return () => window.removeEventListener ( "beforeunload", protectDirtyDocument );
        },
        [ dirty ]
    );

    function reportActivity (
        source: string,
        message: string,
        severity: ConsoleSeverity = "message",
        code?: string,
        showDialog = severity === "error"
    ): void
    {
        let route: GuideContext | undefined;

        if ( source === "Built-in Server" || source === "Server" || source === "Simulator" )
        {
            route = "simulator";
        }
        else if ( source === "Document" || source === "Validation" )
        {
            route = "model";
        }
        else if ( source === "Model Editor" )
        {
            route = currentContext;
        }

        const context: ConsoleContext | undefined = route === undefined
            ? undefined
            : { label: message, route };
        const entry = createConsoleEntry (
            source,
            message,
            consoleSequenceReference.current,
            severity,
            code,
            context
        );

        consoleSequenceReference.current += 1;
        setConsoleEntries ( currentEntries => appendConsoleEntry ( currentEntries, entry ) );

        if ( showDialog )
        {
            setMessageDialog ( { body: message, severity } );
        }
    }

    function reportUnavailableCommand ( commandLabel: string ): void
    {
        reportActivity ( "Command", `${commandLabel}: ${text ( "web.menu.unavailable" )}`, "warning" );
    }

    function runNativeEditCommand ( command: "copy" | "cut" | "paste" ): void
    {
        if ( !document.execCommand ( command ) )
        {
            reportUnavailableCommand ( command );
        }
    }

    function requestDocumentReplacement ( action: string, replacement: () => void ): void
    {
        if ( !dirty )
        {
            replacement ();
            return;
        }

        pendingDiscardReference.current = replacement;
        setDirtyDialogAction ( action );
    }

    function cancelPendingReplacement (): void
    {
        pendingDiscardReference.current = null;
        setDirtyDialogAction ( null );
    }

    function discardAndContinue (): void
    {
        const replacement = pendingDiscardReference.current;

        cancelPendingReplacement ();
        replacement?.();
    }

    function gatewayForSettings ( settings: ServerSettings ): ServerGateway
    {
        if ( settings.target === "built-in" )
        {
            return builtInGateway;
        }

        return new ServerGateway (
            new FetchTransport (
                {
                    serverURL: settings.serverURL,
                    connectTimeoutMilliseconds: settings.connectTimeoutSeconds * 1000,
                    requestTimeoutMilliseconds: settings.requestTimeoutSeconds * 1000,
                    bearerToken: settings.bearerToken,
                }
            )
        );
    }

    function beginBackgroundOperation ( label: string, updateConnectionState = false ): BackgroundOperation | null
    {
        if ( backgroundOperationReference.current !== null )
        {
            return null;
        }

        const operation = { controller: new AbortController (), label };

        backgroundOperationReference.current = operation;
        setBackgroundOperation ( operation );
        if ( updateConnectionState )
        {
            setConnectionState ( "STARTING" );
        }
        return operation;
    }

    function finishBackgroundOperation ( operation: BackgroundOperation ): void
    {
        if ( backgroundOperationReference.current !== operation )
        {
            return;
        }

        backgroundOperationReference.current = null;
        setBackgroundOperation ( null );
    }

    function cancelBackgroundOperation (): void
    {
        backgroundOperationReference.current?.controller.abort ();
    }

    async function requestTestConnection ( settings: ServerSettings = serverSettings ): Promise <void>
    {
        const sameTarget = settings.target === serverSettings.target
            && ( settings.target === "built-in" || settings.serverURL === serverSettings.serverURL );
        const connectionStateAtStart = connectionState;
        const operation = beginBackgroundOperation ( "Testing server connection...", sameTarget );

        if ( operation === null )
        {
            return;
        }

        try
        {
            const result = await gatewayForSettings ( settings ).testConnection ( operation.controller.signal );
            if ( sameTarget )
            {
                setConnectionState ( result.readiness.ready ? "READY" : "FAILED" );
                setServerRevision ( result.readiness.modelRevision ?? null );
            }

            reportActivity (
                "Server",
                `Connection test passed (${result.roundTripMicroseconds} µs; ${result.readiness.status}).`
            );
        }
        catch ( error )
        {
            const cancelled = operationWasCancelled ( error );

            if ( sameTarget )
            {
                setConnectionState ( cancelled
                    ? connectionStateAtStart
                    : "FAILED" );
            }

            if ( cancelled )
            {
                reportActivity ( "Server", gatewayFailureMessage ( error ), "message", "SERVER_OPERATION_CANCELLED" );
            }
            else
            {
                reportActivity (
                    "Server",
                    `Connection test failed: ${gatewayFailureMessage ( error )}`,
                    "error",
                    "SERVER_CONNECTION_TEST_FAILED"
                );
            }
        }
        finally
        {
            finishBackgroundOperation ( operation );
        }
    }

    async function requestPushModel (): Promise <void>
    {
        if ( documentSession.content.validationErrors )
        {
            reportActivity (
                "Server",
                `Push rejected: ${text ( "simulator.model.invalid" )}`,
                "error",
                "SERVER_PUSH_REJECTED"
            );
            return;
        }

        const operation = beginBackgroundOperation ( "Pushing model..." );

        if ( operation === null )
        {
            return;
        }

        const modelAtStart = documentSession.content.authoringModel;

        try
        {
            const result = await gatewayForSettings ( serverSettings ).pushModel (
                modelAtStart,
                operation.controller.signal
            );

            setConnectionState ( "READY" );
            setServerRevision ( result.summary.modelRevision );
            setEvaluation ( null );

            if ( serverSettings.target === "built-in" )
            {
                setBuiltInModelIdentifier ( result.summary.modelId );
                setBuiltInModelRevision ( result.summary.modelRevision );
            }

            reportActivity (
                "Server",
                `Pushed ${result.summary.modelId} at ${result.summary.modelRevision} `
                + `(${result.roundTripMicroseconds} µs).`
            );
        }
        catch ( error )
        {
            if ( operationWasCancelled ( error ) )
            {
                reportActivity ( "Server", gatewayFailureMessage ( error ), "message", "SERVER_OPERATION_CANCELLED" );
                return;
            }

            if ( serverFailureIndicatesDisconnection ( error ) )
            {
                setConnectionState ( "FAILED" );
            }
            reportActivity (
                "Server",
                `Push failed: ${gatewayFailureMessage ( error )}`,
                "error",
                "SERVER_PUSH_FAILED"
            );
        }
        finally
        {
            finishBackgroundOperation ( operation );
        }
    }

    function requestPullModel (): void
    {
        requestDocumentReplacement ( text ( "web.discard.action.pull" ), () => void pullModel () );
    }

    async function pullModel (): Promise <void>
    {
        const operation = beginBackgroundOperation ( "Pulling model..." );

        if ( operation === null )
        {
            return;
        }

        try
        {
            const result = await gatewayForSettings ( serverSettings ).pullModel ( operation.controller.signal );

            setDocumentSession ( createPulledDocumentSession ( result.model ) );
            setExpandedTreeItems ( new Set ( [ "model" ] ) );
            setActiveTab ( "editor" );
            setConnectionState ( "READY" );
            setServerRevision ( result.modelRevision );
            setEvaluation ( null );
            reportActivity (
                "Server",
                `Pulled ${result.model.modelId} at ${result.modelRevision} `
                + `(${result.roundTripMicroseconds} µs).`
            );
        }
        catch ( error )
        {
            if ( operationWasCancelled ( error ) )
            {
                reportActivity ( "Server", gatewayFailureMessage ( error ), "message", "SERVER_OPERATION_CANCELLED" );
                return;
            }

            if ( serverFailureIndicatesDisconnection ( error ) )
            {
                setConnectionState ( "FAILED" );
            }
            reportActivity (
                "Server",
                `Pull failed; the current document was preserved: ${gatewayFailureMessage ( error )}`,
                "error",
                "SERVER_PULL_FAILED"
            );
        }
        finally
        {
            finishBackgroundOperation ( operation );
        }
    }

    async function requestEvaluation (
        event: SimulatorEvent,
        drafts: ReadonlyMap <string, PayloadValueDraft>
    ): Promise <void>
    {
        let occurrence: ReturnType <typeof createOccurrence>;

        try
        {
            occurrence = createOccurrence ( event, drafts );
        }
        catch ( error )
        {
            const field = error instanceof SimulatorDraftFailure
                ? `${error.parameterIdentifier}: `
                : "";

            reportActivity (
                "Simulator",
                `Invalid occurrence: ${field}${errorMessage ( error )}`,
                "error",
                "SIMULATOR_OCCURRENCE_INVALID"
            );
            return;
        }

        const operation = beginBackgroundOperation ( "Evaluating occurrence..." );

        if ( operation === null )
        {
            return;
        }

        setEvaluation ( null );

        try
        {
            const result = await gatewayForSettings ( serverSettings ).evaluate (
                occurrence,
                operation.controller.signal
            );

            setEvaluation ( result );
            setConnectionState ( "READY" );
            setServerRevision ( result.modelRevision );
            reportActivity (
                "Simulator",
                `${result.outcome} returned in ${result.roundTripMicroseconds} µs.`
            );
        }
        catch ( error )
        {
            if ( operationWasCancelled ( error ) )
            {
                reportActivity ( "Simulator", gatewayFailureMessage ( error ), "message", "SERVER_OPERATION_CANCELLED" );
                return;
            }

            if ( serverFailureIndicatesDisconnection ( error ) )
            {
                setConnectionState ( "FAILED" );
            }
            reportActivity (
                "Simulator",
                `Evaluation failed: ${gatewayFailureMessage ( error )}`,
                "error",
                "SIMULATOR_EVALUATION_FAILED"
            );
        }
        finally
        {
            finishBackgroundOperation ( operation );
        }
    }

    async function requestResetBuiltIn (): Promise <void>
    {
        const operation = beginBackgroundOperation (
            "Resetting built-in server...",
            serverSettings.target === "built-in"
        );

        if ( operation === null )
        {
            return;
        }

        try
        {
            const readyState = await builtInTransport.reset ();

            setBuiltInModelIdentifier ( readyState.modelIdentifier );
            setBuiltInModelRevision ( readyState.modelRevision );
            setEvaluation ( null );

            if ( serverSettings.target === "built-in" )
            {
                setConnectionState ( "READY" );
                setServerRevision ( readyState.modelRevision );
            }

            reportActivity ( "Built-in Server", `Reset to ${readyState.modelIdentifier}.` );
        }
        catch ( error )
        {
            if ( serverSettings.target === "built-in" )
            {
                setConnectionState ( "FAILED" );
            }

            reportActivity (
                "Built-in Server",
                `Reset failed: ${gatewayFailureMessage ( error )}`,
                "error",
                "BUILT_IN_SERVER_RESET_FAILED"
            );
        }
        finally
        {
            finishBackgroundOperation ( operation );
        }
    }

    function applySettings (
        nextTheme: Theme,
        nextSettings: ServerSettings,
        nextFollowConsoleTail: boolean
    ): void
    {
        const targetChanged = nextSettings.target !== serverSettings.target
            || ( nextSettings.target === "real" && nextSettings.serverURL !== serverSettings.serverURL );

        if ( targetChanged )
        {
            cancelBackgroundOperation ();
        }

        serverTargetReference.current = nextSettings.target;
        setTheme ( nextTheme );
        setServerSettings ( nextSettings );
        setFollowConsoleTail ( nextFollowConsoleTail );

        if ( targetChanged )
        {
            setEvaluation ( null );
            setConnectionState (
                nextSettings.target === "built-in"
                    ? builtInTransport.state === "READY" ? "READY" : "STARTING"
                    : "UNTESTED"
            );
            setServerRevision ( nextSettings.target === "built-in" ? builtInModelRevision : null );
        }

        reportActivity (
            "Settings",
            targetChanged
                ? `${nextSettings.target === "built-in"
                    ? text ( "web.server.built.in" )
                    : text ( "web.server.real" )} selected.`
                : "Settings applied."
        );
        setSettingsOpen ( false );
    }

    function requestNewDocument (): void
    {
        if ( backgroundOperation !== null )
        {
            return;
        }

        requestDocumentReplacement ( text ( "web.discard.action.new" ), () =>
        {
            setDocumentSession ( createNewDocumentSession () );
            setExpandedTreeItems ( new Set ( [ "model" ] ) );
            setActiveTab ( "editor" );
            reportActivity ( "Document", text ( "web.document.new" ) );
        } );
    }

    function requestOpenDocument (): void
    {
        if ( backgroundOperation !== null )
        {
            return;
        }

        requestDocumentReplacement ( text ( "web.discard.action.open" ), () => void openDocument () );
    }

    async function openDocument (): Promise <void>
    {
        try
        {
            const openedDocument = await fileService.open ();

            if ( openedDocument === null )
            {
                return;
            }

            const model = modelCodec.read ( openedDocument.contents );

            setDocumentSession (
                createLoadedDocumentSession (
                    model,
                    openedDocument.fileName,
                    openedDocument.fileHandle
                )
            );
            setExpandedTreeItems ( new Set ( [ "model" ] ) );
            setActiveTab ( "editor" );
            reportActivity ( "Document", `${text ( "web.document.opened" )} ${openedDocument.fileName}` );
        }
        catch ( error )
        {
            if ( operationWasCancelled ( error ) )
            {
                return;
            }

            reportActivity (
                "Document",
                `${text ( "web.document.open.failed" )} ${errorMessage ( error )}`,
                "error",
                "DOCUMENT_OPEN_FAILED"
            );
        }
    }

    async function requestSaveDocument ( saveAs: boolean ): Promise <boolean>
    {
        if ( !documentValid || backgroundOperation !== null )
        {
            return false;
        }

        const sessionAtSaveStart = documentSession;
        const savedContent = sessionAtSaveStart.content;
        const contents = `${modelCodec.writePretty ( savedContent.authoringModel )}\n`;

        try
        {
            const savedDocument = await fileService.save (
                contents,
                suggestedFileName ( sessionAtSaveStart ),
                sessionAtSaveStart.fileHandle,
                saveAs
            );

            setDocumentSession (
                currentSession => currentSession.documentIdentity === sessionAtSaveStart.documentIdentity
                    ? markDocumentSaved (
                        currentSession,
                        savedDocument.fileName,
                        savedDocument.fileHandle,
                        savedContent
                    )
                    : currentSession
            );
            if ( savedDocument.kind === "file-handle" )
            {
                reportActivity ( "Document", `${text ( "web.document.saved" )} ${savedDocument.fileName}` );
            }
            else
            {
                reportActivity (
                    "Document",
                    [
                        text ( "web.document.downloaded" ),
                        `${savedDocument.fileName}.`,
                        text ( "web.document.download.note" ),
                    ].join ( " " ),
                    "warning",
                    "DOCUMENT_DOWNLOAD_FALLBACK",
                    true
                );
            }
            return true;
        }
        catch ( error )
        {
            if ( operationWasCancelled ( error ) )
            {
                return false;
            }

            reportActivity (
                "Document",
                `${text ( "web.document.save.failed" )} ${errorMessage ( error )}`,
                "error",
                "DOCUMENT_SAVE_FAILED"
            );
            return false;
        }
    }

    async function saveAndContinuePendingReplacement (): Promise <void>
    {
        const requestedReplacement = pendingDiscardReference.current;

        if ( requestedReplacement !== null && await requestSaveDocument ( false )
            && pendingDiscardReference.current === requestedReplacement )
        {
            cancelPendingReplacement ();
            requestedReplacement ();
        }
    }

    function requestCloseDocument (): void
    {
        if ( backgroundOperation !== null )
        {
            return;
        }

        requestDocumentReplacement ( text ( "web.discard.action.close" ), () =>
        {
            setDocumentSession ( createNewDocumentSession () );
            setExpandedTreeItems ( new Set ( [ "model" ] ) );
            setActiveTab ( "editor" );
            reportActivity ( "Document", text ( "web.document.closed" ) );
        } );
    }

    function requestExit (): void
    {
        requestDocumentReplacement ( text ( "web.discard.action.exit" ), () =>
        {
            window.close ();
            reportActivity ( "Application", text ( "web.exit.adaptation" ) );
        } );
    }

    function requestValidate (): void
    {
        if ( backgroundOperation !== null )
        {
            return;
        }

        const count = documentSession.content.validationMessages.length;
        const validationEntries: ConsoleEntry[] = documentSession.content.validationMessages.map ( diagnostic =>
        {
            const sequence = consoleSequenceReference.current;
            const category = CATEGORY_MENU_ITEMS.find (
                item => documentSession.content.entityIdentifiers.get ( item.context )?.includes ( diagnostic.entityId )
            )?.context;

            consoleSequenceReference.current += 1;
            return {
                code: diagnostic.code,
                context:
                {
                    entityIdentifier: category === undefined ? undefined : diagnostic.entityId,
                    label: `${diagnostic.entityId}.${diagnostic.field}`,
                    route: ( category ?? "model" ) as GuideContext,
                },
                identifier: `console-${sequence}`,
                severity: diagnostic.severity === "WARNING" ? "warning" as const : "error" as const,
                source: "Validation",
                text: `${diagnostic.entityId}.${diagnostic.field}: ${diagnostic.remedy}`,
                timestamp: new Date ().toISOString (),
            };
        } );

        if ( validationEntries.length > 0 )
        {
            setConsoleEntries (
                currentEntries => [ ...currentEntries, ...validationEntries ].slice ( -MAXIMUM_CONSOLE_ENTRY_COUNT )
            );
        }

        reportActivity (
            "Validation",
            count === 0
                ? text ( "messages.validation.valid" )
                : `${text ( "web.validation.count" )} ${count}`,
            count === 0 ? "message" : "warning"
        );
    }

    function requestUndo (): void
    {
        if ( backgroundOperation !== null || documentSession.undoHistory.length === 0 )
        {
            return;
        }

        setDocumentSession ( currentSession => undoDocumentEdit ( currentSession ) );
        reportActivity ( "Document", text ( "web.document.undo" ) );
    }

    function requestRedo (): void
    {
        if ( backgroundOperation !== null || documentSession.redoHistory.length === 0 )
        {
            return;
        }

        setDocumentSession ( currentSession => redoDocumentEdit ( currentSession ) );
        reportActivity ( "Document", text ( "web.document.redo" ) );
    }

    function requestApplyModelDetails (
        modelIdentifier: string,
        name: string,
        description: string
    ): EditResult
    {
        const result = applyModelDetails (
            documentSession.content.authoringModel,
            modelIdentifier,
            name,
            description
        );

        if ( result.model !== null )
        {
            const replacementModel = result.model;

            setDocumentSession (
                currentSession => applyDocumentEdit ( currentSession, replacementModel, { kind: "model" } )
            );
            reportActivity ( "Model Editor", text ( "web.document.details.applied" ) );
        }

        return result;
    }

    function requestApplyEntity (
        category: ModelCategory,
        originalEntityIdentifier: string | null,
        draft: EntityDraft
    ): EditResult
    {
        const result = applyEntityDraft (
            documentSession.content.authoringModel,
            category,
            originalEntityIdentifier,
            draft
        );

        if ( result.model !== null && result.entityIdentifier !== null )
        {
            const entityIdentifier = result.entityIdentifier;
            const replacementModel = result.model;

            setDocumentSession (
                currentSession => applyDocumentEdit (
                    currentSession,
                    replacementModel,
                    { kind: "entity", category, entityIdentifier }
                )
            );
            reportActivity ( "Model Editor", `${text ( "web.entity.applied" )} ${entityIdentifier}` );
        }

        return result;
    }

    function requestDuplicateEntity ( category: ModelCategory, entityIdentifier: string ): EditResult
    {
        const result = duplicateEntity ( documentSession.content.authoringModel, category, entityIdentifier );

        if ( result.model !== null && result.entityIdentifier !== null )
        {
            const duplicateIdentifier = result.entityIdentifier;
            const replacementModel = result.model;

            setDocumentSession (
                currentSession => applyDocumentEdit (
                    currentSession,
                    replacementModel,
                    { kind: "entity", category, entityIdentifier: duplicateIdentifier }
                )
            );
            reportActivity ( "Model Editor", `${text ( "web.entity.duplicated" )} ${duplicateIdentifier}` );
        }

        return result;
    }

    function requestDeleteEntity ( category: ModelCategory, entityIdentifier: string ): DeleteResult
    {
        const result = deleteEntity ( documentSession.content.authoringModel, category, entityIdentifier );

        if ( result.model !== null )
        {
            const replacementModel = result.model;

            setDocumentSession (
                currentSession => applyDocumentEdit (
                    currentSession,
                    replacementModel,
                    { kind: "category", category }
                )
            );
            reportActivity ( "Model Editor", `${text ( "web.entity.deleted" )} ${entityIdentifier}` );
        }
        else if ( result.references.length > 0 )
        {
            reportActivity (
                "Model Editor",
                `${text ( "web.entity.delete.blocked" )} ${result.references.join ( ", " )}`,
                "warning"
            );
        }

        return result;
    }

    function requestMoveEntity (
        category: ModelCategory,
        entityIdentifier: string,
        offset: -1 | 1
    ): EditResult
    {
        const result = reorderEntity (
            documentSession.content.authoringModel,
            category,
            entityIdentifier,
            offset
        );

        if ( result.model !== null )
        {
            const replacementModel = result.model;

            setDocumentSession (
                currentSession => applyDocumentEdit (
                    currentSession,
                    replacementModel,
                    { kind: "entity", category, entityIdentifier }
                )
            );
            reportActivity (
                "Model Editor",
                `${text ( offset < 0 ? "web.entity.moved.up" : "web.entity.moved.down" )} ${entityIdentifier}`
            );
        }

        return result;
    }

    function selectCategory ( context: Exclude <GuideContext, "model" | "simulator"> ): void
    {
        const categoryMenuItem = CATEGORY_MENU_ITEMS.find ( item => item.context === context );

        if ( categoryMenuItem === undefined )
        {
            throw new Error ( `Unknown editor category: ${context}` );
        }

        setDocumentSession ( currentSession => selectDocumentCategory ( currentSession, context ) );
        setActiveTab ( "editor" );
    }

    function selectTreeContext (
        context: Exclude <GuideContext, "simulator">,
        entityIdentifier?: string
    ): void
    {
        if ( entityIdentifier !== undefined )
        {
            setEntityEditorRequestSequence ( currentSequence => currentSequence + 1 );
        }

        setDocumentSession (
            currentSession => entityIdentifier !== undefined && context !== "model"
                ? selectDocumentEntity ( currentSession, context, entityIdentifier )
                : context === "model"
                    ? selectDocumentModel ( currentSession )
                    : selectDocumentCategory ( currentSession, context )
        );
        setActiveTab ( "editor" );
    }

    function applyTheme ( nextTheme: Theme ): void
    {
        const message = nextTheme === "dark"
            ? text ( "web.diagnostics.theme.dark" )
            : text ( "web.diagnostics.theme.light" );

        setTheme ( nextTheme );
        reportActivity ( "Appearance", message );
    }

    function changeOutlineWidth ( nextOutlineWidth: number ): void
    {
        const clampedOutlineWidth = clamp (
            nextOutlineWidth,
            layoutBounds.outlineWidthMinimum,
            layoutBounds.outlineWidthMaximum
        );

        setOutlineWidth ( clampedOutlineWidth );

        if ( viewportDimensions.width > 1150 )
        {
            const availableUserGuideWidth = viewportDimensions.width
                - HORIZONTAL_SPLITTER_WIDTH_TOTAL
                - DETAIL_PANE_MINIMUM_WIDTH
                - clampedOutlineWidth;

            setUserGuideWidth (
                currentWidth => clamp (
                    Math.min ( currentWidth, availableUserGuideWidth ),
                    layoutBounds.userGuideWidthMinimum,
                    layoutBounds.userGuideWidthMaximum
                )
            );
        }
    }

    function changeUserGuideWidth ( nextUserGuideWidth: number ): void
    {
        const clampedUserGuideWidth = clamp (
            nextUserGuideWidth,
            layoutBounds.userGuideWidthMinimum,
            layoutBounds.userGuideWidthMaximum
        );

        setUserGuideWidth ( clampedUserGuideWidth );

        if ( viewportDimensions.width > 1150 )
        {
            const availableOutlineWidth = viewportDimensions.width
                - HORIZONTAL_SPLITTER_WIDTH_TOTAL
                - DETAIL_PANE_MINIMUM_WIDTH
                - clampedUserGuideWidth;

            setOutlineWidth (
                currentWidth => clamp (
                    Math.min ( currentWidth, availableOutlineWidth ),
                    layoutBounds.outlineWidthMinimum,
                    layoutBounds.outlineWidthMaximum
                )
            );
        }
    }

    useEffect (
        () =>
        {
            function handleShortcut ( event: globalThis.KeyboardEvent ): void
            {
                if ( !event.ctrlKey || event.altKey )
                {
                    return;
                }

                const key = event.key.toLowerCase ();

                if ( key === "n" )
                {
                    event.preventDefault ();
                    requestNewDocument ();
                }
                else if ( key === "o" )
                {
                    event.preventDefault ();
                    void requestOpenDocument ();
                }
                else if ( key === "s" )
                {
                    event.preventDefault ();
                    void requestSaveDocument ( event.shiftKey );
                }
                else if ( key === "w" )
                {
                    event.preventDefault ();
                    requestCloseDocument ();
                }
                else if ( key === "z" && !event.shiftKey )
                {
                    event.preventDefault ();
                    requestUndo ();
                }
                else if ( key === "y" || ( key === "z" && event.shiftKey ) )
                {
                    event.preventDefault ();
                    requestRedo ();
                }
            }

            window.addEventListener ( "keydown", handleShortcut );

            return () => window.removeEventListener ( "keydown", handleShortcut );
        }
    );

    function menuItem (
        identifier: string,
        labelKey: MessageKey,
        onSelect?: () => void,
        disabled = false
    ): MenuEntry
    {
        return {
            kind: "item",
            identifier,
            label: text ( labelKey ),
            disabled,
            icon: MENU_ICON_NAMES [ identifier ] === undefined
                ? undefined
                : fluentIcon ( MENU_ICON_NAMES [ identifier ] ),
            onSelect: onSelect ?? ( () => reportUnavailableCommand ( text ( labelKey ) ) ),
            shortcut: MENU_SHORTCUTS [ identifier ],
        };
    }

    const menus: readonly MenuDefinition[] =
    [
        {
            identifier: "file",
            label: text ( "menu.file" ),
            entries:
            [
                menuItem ( "file-new", "menu.file.new", requestNewDocument, backgroundOperation !== null ),
                menuItem (
                    "file-open",
                    "menu.file.open",
                    () => void requestOpenDocument (),
                    backgroundOperation !== null
                ),
                menuItem (
                    "file-save",
                    "menu.file.save",
                    () => void requestSaveDocument ( false ),
                    !documentValid || backgroundOperation !== null
                ),
                menuItem (
                    "file-save-as",
                    "menu.file.save.as",
                    () => void requestSaveDocument ( true ),
                    !documentValid || backgroundOperation !== null
                ),
                menuItem ( "file-close", "menu.file.close", requestCloseDocument, backgroundOperation !== null ),
                separator (),
                menuItem ( "file-validate", "menu.file.validate", requestValidate, backgroundOperation !== null ),
                menuItem (
                    "file-push",
                    "menu.file.push",
                    () => void requestPushModel (),
                    documentSession.content.validationErrors || backgroundOperation !== null
                ),
                menuItem (
                    "file-pull",
                    "menu.file.pull",
                    () => void requestPullModel (),
                    backgroundOperation !== null
                ),
                separator (),
                menuItem (
                    "file-test-connection",
                    "menu.file.test.connection",
                    () => void requestTestConnection (),
                    backgroundOperation !== null
                ),
                menuItem (
                    "file-settings",
                    "menu.file.settings",
                    () => setSettingsOpen ( true ),
                    backgroundOperation !== null
                ),
                separator (),
                menuItem ( "file-exit", "menu.file.exit", requestExit ),
            ],
        },
        {
            identifier: "edit",
            label: text ( "menu.edit" ),
            entries:
            [
                menuItem (
                    "edit-undo",
                    "menu.edit.undo",
                    requestUndo,
                    backgroundOperation !== null || documentSession.undoHistory.length === 0
                ),
                menuItem (
                    "edit-redo",
                    "menu.edit.redo",
                    requestRedo,
                    backgroundOperation !== null || documentSession.redoHistory.length === 0
                ),
                separator (),
                menuItem ( "edit-cut", "menu.edit.cut", () => runNativeEditCommand ( "cut" ) ),
                menuItem ( "edit-copy", "menu.edit.copy", () => runNativeEditCommand ( "copy" ) ),
                menuItem ( "edit-paste", "menu.edit.paste", () => runNativeEditCommand ( "paste" ) ),
                menuItem (
                    "edit-delete",
                    "menu.edit.delete",
                    currentEntityIdentifier === null || currentContext === "model"
                        ? undefined
                        : () => requestDeleteEntity ( currentContext, currentEntityIdentifier ),
                    currentEntityIdentifier === null || currentContext === "model"
                ),
            ],
        },
        {
            identifier: "view",
            label: text ( "menu.view" ),
            entries:
            [
                ...CATEGORY_MENU_ITEMS.map (
                    category => menuItem (
                        `view-${category.context}`,
                        category.labelKey,
                        () => selectCategory ( category.context )
                    )
                ),
                separator (),
                menuItem (
                    "view-simulator",
                    "menu.view.simulator",
                    () =>
                    {
                        setActiveTab ( "simulator" );
                    }
                ),
                separator (),
                menuItem (
                    "view-expand-all",
                    "menu.view.expand.all",
                    () => setExpandedTreeItems ( fullyExpandedTreeItems () )
                ),
                menuItem (
                    "view-collapse-all",
                    "menu.view.collapse.all",
                    () => setExpandedTreeItems ( new Set () )
                ),
                separator (),
                menuItem (
                    "view-clear-console",
                    "web.menu.view.clear.console",
                    () => setConsoleEntries ( [] )
                ),
                {
                    checked: consoleVisible,
                    checkRole: "checkbox",
                    icon: fluentIcon ( "ic_fluent_code_16_regular.svg" ),
                    identifier: "view-console",
                    kind: "item",
                    label: text ( "web.menu.view.console" ),
                    onSelect: () => setConsoleVisible ( currentValue => !currentValue ),
                },
                separator (),
                {
                    icon: fluentIcon ( "ic_fluent_weather_moon_16_regular.svg" ),
                    kind: "item",
                    identifier: "view-theme",
                    label: text ( "menu.view.theme" ),
                    children:
                    [
                        {
                            kind: "item",
                            identifier: "theme-light",
                            icon: fluentIcon ( "ic_fluent_weather_sunny_16_regular.svg" ),
                            label: text ( "settings.theme.light" ),
                            checked: theme === "light",
                            onSelect: () => applyTheme ( "light" ),
                        },
                        {
                            kind: "item",
                            identifier: "theme-dark",
                            icon: fluentIcon ( "ic_fluent_weather_moon_16_regular.svg" ),
                            label: text ( "settings.theme.dark" ),
                            checked: theme === "dark",
                            onSelect: () => applyTheme ( "dark" ),
                        },
                    ],
                },
            ],
        },
        {
            identifier: "help",
            label: text ( "menu.help" ),
            entries:
            [
                menuItem (
                    "help-technical-note",
                    "menu.help.technical.note",
                    () => window.open (
                        TECHNICAL_NOTE_URL,
                        "_blank",
                        "noopener,noreferrer"
                    )
                ),
                menuItem (
                    "help-github",
                    "menu.help.github",
                    () => window.open (
                        "https://github.com/rohingosling/eca-rule-engine-2",
                        "_blank",
                        "noopener,noreferrer"
                    )
                ),
                separator (),
                menuItem ( "help-about", "menu.help.about", () => setAboutOpen ( true ) ),
            ],
        },
    ];

    const toolbarEntries: readonly ToolbarEntry[] =
    [
        {
            disabled: backgroundOperation !== null,
            icon: "ic_fluent_document_add_20_regular.svg",
            identifier: "toolbar-new",
            kind: "button",
            label: text ( "menu.file.new" ),
            onSelect: requestNewDocument,
        },
        {
            disabled: backgroundOperation !== null,
            icon: "ic_fluent_folder_open_20_regular.svg",
            identifier: "toolbar-open",
            kind: "button",
            label: text ( "menu.file.open" ),
            onSelect: requestOpenDocument,
        },
        {
            disabled: !documentValid || backgroundOperation !== null,
            icon: "ic_fluent_save_20_regular.svg",
            identifier: "toolbar-save",
            kind: "button",
            label: text ( "menu.file.save" ),
            onSelect: () => void requestSaveDocument ( false ),
        },
        {
            disabled: !documentValid || backgroundOperation !== null,
            icon: "ic_fluent_save_copy_20_regular.svg",
            identifier: "toolbar-save-as",
            kind: "button",
            label: text ( "menu.file.save.as" ),
            onSelect: () => void requestSaveDocument ( true ),
        },
        { kind: "separator" },
        {
            disabled: backgroundOperation !== null,
            icon: "ic_fluent_arrow_download_20_regular.svg",
            identifier: "toolbar-pull",
            kind: "button",
            label: text ( "menu.file.pull" ),
            onSelect: requestPullModel,
        },
        {
            disabled: documentSession.content.validationErrors || backgroundOperation !== null,
            icon: "ic_fluent_arrow_upload_20_regular.svg",
            identifier: "toolbar-push",
            kind: "button",
            label: text ( "menu.file.push" ),
            onSelect: () => void requestPushModel (),
        },
        { kind: "separator" },
        {
            disabled: backgroundOperation !== null || documentSession.undoHistory.length === 0,
            icon: "ic_fluent_arrow_undo_20_regular.svg",
            identifier: "toolbar-undo",
            kind: "button",
            label: text ( "menu.edit.undo" ),
            onSelect: requestUndo,
        },
        {
            disabled: backgroundOperation !== null || documentSession.redoHistory.length === 0,
            icon: "ic_fluent_arrow_redo_20_regular.svg",
            identifier: "toolbar-redo",
            kind: "button",
            label: text ( "menu.edit.redo" ),
            onSelect: requestRedo,
        },
        { kind: "separator" },
        {
            icon: "ic_fluent_edit_20_regular.svg",
            identifier: "toolbar-editor",
            kind: "button",
            label: text ( "tab.editor" ),
            onSelect: () => setActiveTab ( "editor" ),
            pressed: activeTab === "editor",
        },
        {
            icon: "ic_fluent_play_20_regular.svg",
            identifier: "toolbar-simulator",
            kind: "button",
            label: text ( "tab.simulator" ),
            onSelect: () => setActiveTab ( "simulator" ),
            pressed: activeTab === "simulator",
        },
        { kind: "separator" },
        {
            icon: "ic_fluent_arrow_expand_all_20_regular.svg",
            identifier: "toolbar-expand-all",
            kind: "button",
            label: text ( "menu.view.expand.all" ),
            onSelect: () => setExpandedTreeItems ( fullyExpandedTreeItems () ),
        },
        {
            icon: "ic_fluent_arrow_collapse_all_20_regular.svg",
            identifier: "toolbar-collapse-all",
            kind: "button",
            label: text ( "menu.view.collapse.all" ),
            onSelect: () => setExpandedTreeItems ( new Set () ),
        },
        { kind: "separator" },
        {
            choices:
            [
                {
                    checked: theme === "light",
                    identifier: "toolbar-theme-light",
                    label: text ( "settings.theme.light" ),
                    onSelect: () => applyTheme ( "light" ),
                },
                {
                    checked: theme === "dark",
                    identifier: "toolbar-theme-dark",
                    label: text ( "settings.theme.dark" ),
                    onSelect: () => applyTheme ( "dark" ),
                },
            ],
            icon: "ic_fluent_dark_theme_20_regular.svg",
            identifier: "toolbar-theme",
            kind: "button",
            label: text ( "menu.view.theme" ),
        },
    ];

    const guideContext: GuideContext = activeTab === "simulator" ? "simulator" : currentContext;
    const shellStyle: ShellStyle =
    {
        "--console-panel-height": `${diagnosticsHeight}px`,
        "--outline-width": `${outlineWidth}px`,
        "--user-guide-width": `${userGuideWidth}px`,
    };

    return (
        <div
            className="application-shell"
            data-console-visible={ consoleVisible }
            data-theme={ theme }
            style={ shellStyle }
        >
            <h1 className="visually-hidden">{ documentWindowTitle ( documentSession ) }</h1>
            <header className="application-title-bar">
                <img
                    alt=""
                    aria-hidden="true"
                    className="application-icon"
                    src={ `${import.meta.env.BASE_URL}assets/eca-rule-engine.png` }
                />
                <strong>{ text ( "about.application.name" ) }</strong>
                <span> - { documentDisplayName ( documentSession ) }</span>
                { dirty && <span aria-hidden="true" className="dirty-marker">*</span> }
                <span aria-live="polite" className="visually-hidden">
                    { dirty ? text ( "status.dirty" ) : "" }
                </span>
                <span className="title-version">{ text ( "about.version" ) }</span>
            </header>
            <MenuBar accessibleLabel={ text ( "web.application.menu" ) } menus={ menus } />
            <Toolbar entries={ toolbarEntries } />
            <main className="workspace">
                <div className="upper-workspace">
                    <GroupBox className="workspace-panel outline-panel" legend={ text ( "outline.title" ) }>
                        <NavigationTree
                            entityIdentifiers={ documentSession.content.entityIdentifiers }
                            expandedItems={ expandedTreeItems }
                            onExpandedItemsChange={ setExpandedTreeItems }
                            onSelect={ selectTreeContext }
                            selectedEntityIdentifier={ currentEntityIdentifier }
                            selectedItem={ currentContext }
                        />
                    </GroupBox>
                    <Splitter
                        ariaLabel={ text ( "web.splitter.outline" ) }
                        className="outline-splitter"
                        maximum={ layoutBounds.outlineWidthMaximum }
                        minimum={ layoutBounds.outlineWidthMinimum }
                        onChange={ changeOutlineWidth }
                        orientation="vertical"
                        value={ outlineWidth }
                    />
                    <Tabs
                        activeTab={ activeTab }
                        onSelect={ setActiveTab }
                        tabs={
                            [
                                {
                                    identifier: "editor",
                                    label: text ( "tab.editor" ),
                                    content: (
                                        <DocumentEditor
                                            content={ documentSession.content }
                                            entityEditorRequestSequence={ entityEditorRequestSequence }
                                            onApplyEntity={ requestApplyEntity }
                                            onApplyModelDetails={ requestApplyModelDetails }
                                            onDeleteEntity={ requestDeleteEntity }
                                            onDuplicateEntity={ requestDuplicateEntity }
                                            onMoveEntity={ requestMoveEntity }
                                            onSelectEntity={ ( category, entityIdentifier ) =>
                                            {
                                                setDocumentSession (
                                                    currentSession => entityIdentifier === null
                                                        ? selectDocumentCategory ( currentSession, category )
                                                        : selectDocumentEntity (
                                                            currentSession,
                                                            category,
                                                            entityIdentifier
                                                        )
                                                );
                                            } }
                                            onValidate={ requestValidate }
                                            operationRunning={ backgroundOperation !== null }
                                            selectedContext={ currentContext }
                                            selectedEntityIdentifier={ currentEntityIdentifier }
                                        />
                                    ),
                                },
                                {
                                    identifier: "simulator",
                                    label: text ( "tab.simulator" ),
                                    content: (
                                        <SimulatorPanel
                                            evaluation={ evaluation }
                                            localRevision={ localRevision }
                                            model={ documentSession.content.authoringModel }
                                            modelValid={ !documentSession.content.validationErrors }
                                            onEvaluate={ ( event, drafts ) => void requestEvaluation ( event, drafts ) }
                                            operationRunning={ backgroundOperation !== null }
                                            serverRevision={ serverRevision }
                                        />
                                    ),
                                },
                            ]
                        }
                    />
                    <Splitter
                        ariaLabel={ text ( "web.splitter.user.guide" ) }
                        className="user-guide-splitter"
                        direction={ -1 }
                        maximum={ layoutBounds.userGuideWidthMaximum }
                        minimum={ layoutBounds.userGuideWidthMinimum }
                        onChange={ changeUserGuideWidth }
                        orientation="vertical"
                        value={ userGuideWidth }
                    />
                    <GroupBox className="workspace-panel user-guide-panel" legend={ text ( "user.guide.title" ) }>
                        <UserGuidePanel context={ guideContext } theme={ theme } />
                    </GroupBox>
                </div>
                <Splitter
                    ariaLabel={ text ( "web.splitter.console" ) }
                    className="console-splitter"
                    direction={ -1 }
                    minimum={ layoutBounds.diagnosticsHeightMinimum }
                    onChange={ setDiagnosticsHeight }
                    orientation="horizontal"
                    value={ diagnosticsHeight }
                />
                <ConsolePanel
                    entries={ displayedConsoleEntries }
                    filters={ consoleFilters }
                    followTail={ followConsoleTail }
                    onClear={ () =>
                    {
                        setConsoleEntries ( [] );
                    } }
                    onFiltersChange={ setConsoleFilters }
                    onFollowTailChange={ setFollowConsoleTail }
                    onNavigateToContext={ context =>
                    {
                        if ( context.route === "simulator" )
                        {
                            setActiveTab ( "simulator" );
                        }
                        else if ( context.entityIdentifier !== undefined && context.route !== "model" )
                        {
                            selectTreeContext ( context.route, context.entityIdentifier );
                        }
                        else
                        {
                            selectTreeContext ( context.route );
                        }
                    } }
                    visible={ consoleVisible }
                />
            </main>
            <AboutDialog onClose={ () => setAboutOpen ( false ) } open={ aboutOpen } />
            <MessageDialog
                body={ messageDialog?.body ?? "" }
                onClose={ () => setMessageDialog ( null ) }
                open={ messageDialog !== null }
                severity={ messageDialog?.severity ?? "message" }
            />
            <SettingsDialog
                builtInModelIdentifier={ builtInModelIdentifier }
                builtInModelRevision={ builtInModelRevision }
                builtInStatus={ text (
                    builtInTransport.state === "READY"
                        ? "web.server.ready"
                        : builtInTransport.state === "FAILED"
                            ? "web.server.failed"
                            : "web.server.starting"
                ) }
                followConsoleTail={ followConsoleTail }
                onApply={ applySettings }
                onCancel={ () => setSettingsOpen ( false ) }
                onResetBuiltIn={ () => void requestResetBuiltIn () }
                onTestConnection={ settings => void requestTestConnection ( settings ) }
                open={ settingsOpen }
                operationRunning={ backgroundOperation !== null }
                serverSettings={ serverSettings }
                theme={ theme }
            />
            <DirtyDocumentDialog
                canSave={ documentValid && backgroundOperation === null }
                onCancel={ cancelPendingReplacement }
                onDiscard={ discardAndContinue }
                onSave={ () => void saveAndContinuePendingReplacement () }
                open={ dirtyDialogAction !== null }
            />
            <StatusBar
                actionCount={ documentSession.content.summary.actionCount }
                backgroundOperation={ backgroundOperation?.label ?? null }
                conditionCount={ documentSession.content.summary.conditionCount }
                conditionSetCount={ documentSession.content.summary.conditionSetCount }
                eventCount={ documentSession.content.summary.eventCount }
                modelIdentifier={ documentSession.content.summary.modelId }
                onCancel={ cancelBackgroundOperation }
                parameterCount={ documentSession.content.summary.parameterCount }
                payloadCount={ documentSession.content.summary.payloadCount }
                ruleCount={ documentSession.content.summary.ruleCount }
                serverConnection={ connectionState === "READY"
                    ? "connected"
                    : connectionState === "STARTING" ? "connecting" : "disconnected" }
                serverStatus={ text (
                    connectionState === "READY"
                        ? "status.connected"
                        : connectionState === "STARTING"
                            ? "status.connecting"
                            : "status.disconnected"
                ) }
                statusMessage={ backgroundOperation?.label ?? ( activeTab === "simulator"
                    ? text ( "simulator.heading" )
                    : currentContext === "model"
                        ? text ( "editor.heading" )
                        : text ( CATEGORY_MENU_ITEMS.find ( item => item.context === currentContext )!.labelKey ) ) }
            />
        </div>
    );
}
