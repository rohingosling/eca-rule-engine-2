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
import { DocumentEditor } from "./components/DocumentEditor";
import { MenuBar } from "./components/MenuBar";
import type { MenuDefinition, MenuEntry } from "./components/MenuBar";
import { NavigationTree } from "./components/NavigationTree";
import { SettingsDialog } from "./components/SettingsDialog";
import { SimulatorPanel } from "./components/SimulatorPanel";
import type { GuideContext } from "./components/NavigationTree";
import { Splitter } from "./components/Splitter";
import { Tabs } from "./components/Tabs";
import type { TabIdentifier } from "./components/Tabs";
import {
    DiagnosticsPanel,
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
    "--diagnostics-height": string;
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
const TECHNICAL_NOTE_URL              =
    "https://github.com/rohingosling/eca-rule-engine-2/blob/main/docs/technical-note/"
    + "stateless-eca-rule-engine.pdf";

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

function separator (): MenuEntry
{
    return { kind: "separator" };
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
    return error instanceof DOMException && error.name === "AbortError";
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
    const [ outlineWidth, setOutlineWidth ] = useState ( initialPreferences.outlineWidth );
    const [ userGuideWidth, setUserGuideWidth ] = useState ( initialPreferences.userGuideWidth );
    const [ diagnosticsHeight, setDiagnosticsHeight ] = useState ( initialPreferences.diagnosticsHeight );
    const [ expandedTreeItems, setExpandedTreeItems ] = useState <ReadonlySet <string>> (
        new Set ( initialPreferences.expandedTreeItems )
    );
    const [ documentSession, setDocumentSession ] = useState ( createNewDocumentSession );
    const [ activeTab, setActiveTab ] = useState <TabIdentifier> ( "editor" );
    const [ activityMessages, setActivityMessages ] = useState <readonly string[]> (
        [ text ( "web.diagnostics.ready" ) ]
    );
    const [ statusMessage, setStatusMessage ] = useState ( text ( "status.ready" ) );
    const [ serverSettings, setServerSettings ] = useState <ServerSettings> (
        {
            target: initialPreferences.serverTarget,
            serverURL: initialPreferences.serverURL,
            connectTimeoutSeconds: initialPreferences.connectTimeoutSeconds,
            requestTimeoutSeconds: initialPreferences.requestTimeoutSeconds,
            bearerToken: "",
        }
    );
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
    const [ backgroundOperation, setBackgroundOperation ] = useState <BackgroundOperation | null> ( null );
    const backgroundOperationReference = useRef <BackgroundOperation | null> ( null );
    const storageWarningReportedReference = useRef ( false );
    const currentContext = selectedContext ( documentSession.selection );
    const currentEntityIdentifier = selectedEntityIdentifier ( documentSession );
    const dirty = documentIsDirty ( documentSession );
    const validationMessages = documentSession.content.validationMessages.map (
        diagnostic =>
            `[Validation] ${diagnostic.entityId}.${diagnostic.field}: ${diagnostic.remedy} (${diagnostic.code})`
    );
    const diagnosticsMessages = [ ...activityMessages, ...validationMessages ];

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
                reportActivity ( "[Browser] Preference storage is unavailable; settings remain page-session-only." );
            }
        },
        [ diagnosticsHeight, expandedTreeItems, outlineWidth, serverSettings, theme, userGuideWidth ]
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

                    if ( serverSettings.target === "built-in" )
                    {
                        setConnectionState ( "READY" );
                        setServerRevision ( readyState.modelRevision );
                    }

                    reportActivity ( `[Built-in Server] Ready with ${readyState.modelIdentifier}.` );
                },
                error =>
                {
                    if ( !current )
                    {
                        return;
                    }

                    if ( serverSettings.target === "built-in" )
                    {
                        setConnectionState ( "FAILED" );
                    }

                    reportActivity ( `[Built-in Server] Startup failed: ${gatewayFailureMessage ( error )}` );
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

    function reportActivity ( message: string ): void
    {
        setActivityMessages ( currentMessages => [ ...currentMessages, message ] );
        setStatusMessage ( message );
    }

    function reportUnavailableCommand ( commandLabel: string ): void
    {
        reportActivity ( `[Command] ${commandLabel}: ${text ( "web.menu.unavailable" )}` );
    }

    function confirmDiscard ( action: string ): boolean
    {
        return !dirty || window.confirm (
            `${text ( "dialog.discard.header" )}\n\n${text ( "dialog.discard.message.prefix" )} ${action}?`
        );
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

    function beginBackgroundOperation ( label: string ): BackgroundOperation | null
    {
        if ( backgroundOperationReference.current !== null )
        {
            return null;
        }

        const operation = { controller: new AbortController (), label };

        backgroundOperationReference.current = operation;
        setBackgroundOperation ( operation );
        setStatusMessage ( label );
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
        const operation = beginBackgroundOperation ( "Testing server connection..." );

        if ( operation === null )
        {
            return;
        }

        try
        {
            const result = await gatewayForSettings ( settings ).testConnection ( operation.controller.signal );
            const sameTarget = settings.target === serverSettings.target
                && ( settings.target === "built-in" || settings.serverURL === serverSettings.serverURL );

            if ( sameTarget )
            {
                setConnectionState ( result.readiness.ready ? "READY" : "FAILED" );
                setServerRevision ( result.readiness.modelRevision ?? null );
            }

            reportActivity (
                `[Server] Connection test passed (${result.roundTripMicroseconds} µs; ${result.readiness.status}).`
            );
        }
        catch ( error )
        {
            if ( settings.target === serverSettings.target )
            {
                setConnectionState ( error instanceof GatewayFailure && error.category === "CANCELLED"
                    ? connectionState
                    : "FAILED" );
            }

            reportActivity ( `[Server] Connection test failed: ${gatewayFailureMessage ( error )}` );
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
            reportActivity ( `[Server] Push rejected: ${text ( "simulator.model.invalid" )}` );
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
                `[Server] Pushed ${result.summary.modelId} at ${result.summary.modelRevision} `
                + `(${result.roundTripMicroseconds} µs).`
            );
        }
        catch ( error )
        {
            reportActivity ( `[Server] Push failed: ${gatewayFailureMessage ( error )}` );
        }
        finally
        {
            finishBackgroundOperation ( operation );
        }
    }

    async function requestPullModel (): Promise <void>
    {
        if ( !confirmDiscard ( "pull the server model" ) )
        {
            return;
        }

        const operation = beginBackgroundOperation ( "Pulling model..." );

        if ( operation === null )
        {
            return;
        }

        try
        {
            const result = await gatewayForSettings ( serverSettings ).pullModel ( operation.controller.signal );

            setDocumentSession ( createPulledDocumentSession ( result.model ) );
            setActiveTab ( "editor" );
            setConnectionState ( "READY" );
            setServerRevision ( result.modelRevision );
            setEvaluation ( null );
            reportActivity (
                `[Server] Pulled ${result.model.modelId} at ${result.modelRevision} `
                + `(${result.roundTripMicroseconds} µs).`
            );
        }
        catch ( error )
        {
            reportActivity ( `[Server] Pull failed; the current document was preserved: ${gatewayFailureMessage ( error )}` );
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

            reportActivity ( `[Simulator] Invalid occurrence: ${field}${errorMessage ( error )}` );
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
                `[Simulator] ${result.outcome} returned in ${result.roundTripMicroseconds} µs.`
            );
        }
        catch ( error )
        {
            reportActivity ( `[Simulator] Evaluation failed: ${gatewayFailureMessage ( error )}` );
        }
        finally
        {
            finishBackgroundOperation ( operation );
        }
    }

    async function requestResetBuiltIn (): Promise <void>
    {
        const operation = beginBackgroundOperation ( "Resetting built-in server..." );

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

            reportActivity ( `[Built-in Server] Reset to ${readyState.modelIdentifier}.` );
        }
        catch ( error )
        {
            if ( serverSettings.target === "built-in" )
            {
                setConnectionState ( "FAILED" );
            }

            reportActivity ( `[Built-in Server] Reset failed: ${gatewayFailureMessage ( error )}` );
        }
        finally
        {
            finishBackgroundOperation ( operation );
        }
    }

    function applySettings ( nextTheme: Theme, nextSettings: ServerSettings ): void
    {
        const targetChanged = nextSettings.target !== serverSettings.target
            || ( nextSettings.target === "real" && nextSettings.serverURL !== serverSettings.serverURL );

        if ( targetChanged )
        {
            cancelBackgroundOperation ();
        }

        setTheme ( nextTheme );
        setServerSettings ( nextSettings );
        setSettingsOpen ( false );
        setEvaluation ( null );
        setConnectionState (
            nextSettings.target === "built-in"
                ? builtInTransport.state === "READY" ? "READY" : "STARTING"
                : "UNTESTED"
        );
        setServerRevision ( nextSettings.target === "built-in" ? builtInModelRevision : null );
        reportActivity (
            `[Settings] ${nextSettings.target === "built-in"
                ? text ( "web.server.built.in" )
                : text ( "web.server.real" )} selected.`
        );
    }

    function requestNewDocument (): void
    {
        if ( !confirmDiscard ( text ( "web.discard.action.new" ) ) )
        {
            return;
        }

        setDocumentSession ( createNewDocumentSession () );
        setActiveTab ( "editor" );
        reportActivity ( text ( "web.document.new" ) );
    }

    async function requestOpenDocument (): Promise <void>
    {
        if ( !confirmDiscard ( text ( "web.discard.action.open" ) ) )
        {
            return;
        }

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
            setActiveTab ( "editor" );
            reportActivity ( `${text ( "web.document.opened" )} ${openedDocument.fileName}` );
        }
        catch ( error )
        {
            if ( operationWasCancelled ( error ) )
            {
                return;
            }

            reportActivity ( `${text ( "web.document.open.failed" )} ${errorMessage ( error )}` );
        }
    }

    async function requestSaveDocument ( saveAs: boolean ): Promise <void>
    {
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
            reportActivity (
                savedDocument.kind === "file-handle"
                    ? `${text ( "web.document.saved" )} ${savedDocument.fileName}`
                    : [
                        text ( "web.document.downloaded" ),
                        `${savedDocument.fileName}.`,
                        text ( "web.document.download.note" ),
                    ].join ( " " )
            );
        }
        catch ( error )
        {
            if ( operationWasCancelled ( error ) )
            {
                return;
            }

            reportActivity ( `${text ( "web.document.save.failed" )} ${errorMessage ( error )}` );
        }
    }

    function requestCloseDocument (): void
    {
        if ( !confirmDiscard ( text ( "web.discard.action.close" ) ) )
        {
            return;
        }

        setDocumentSession ( createNewDocumentSession () );
        setActiveTab ( "editor" );
        reportActivity ( text ( "web.document.closed" ) );
    }

    function requestExit (): void
    {
        if ( !confirmDiscard ( text ( "web.discard.action.exit" ) ) )
        {
            return;
        }

        window.close ();
        reportActivity ( text ( "web.exit.adaptation" ) );
    }

    function requestValidate (): void
    {
        const count = documentSession.content.validationMessages.length;

        reportActivity (
            count === 0
                ? text ( "messages.validation.valid" )
                : `${text ( "web.validation.count" )} ${count}`
        );
    }

    function requestUndo (): void
    {
        if ( documentSession.undoHistory.length === 0 )
        {
            return;
        }

        setDocumentSession ( currentSession => undoDocumentEdit ( currentSession ) );
        reportActivity ( text ( "web.document.undo" ) );
    }

    function requestRedo (): void
    {
        if ( documentSession.redoHistory.length === 0 )
        {
            return;
        }

        setDocumentSession ( currentSession => redoDocumentEdit ( currentSession ) );
        reportActivity ( text ( "web.document.redo" ) );
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
            reportActivity ( text ( "web.document.details.applied" ) );
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
            reportActivity ( `${text ( "web.entity.applied" )} ${entityIdentifier}` );
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
            reportActivity ( `${text ( "web.entity.duplicated" )} ${duplicateIdentifier}` );
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
            reportActivity ( `${text ( "web.entity.deleted" )} ${entityIdentifier}` );
        }
        else if ( result.references.length > 0 )
        {
            reportActivity ( `${text ( "web.entity.delete.blocked" )} ${result.references.join ( ", " )}` );
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
        setStatusMessage ( `${text ( "web.editor.selected" )} ${text ( categoryMenuItem.labelKey )}` );
    }

    function selectTreeContext ( context: Exclude <GuideContext, "simulator"> ): void
    {
        setDocumentSession (
            currentSession => context === "model"
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
        reportActivity ( message );
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
            onSelect: onSelect ?? ( () => reportUnavailableCommand ( text ( labelKey ) ) ),
        };
    }

    const menus: readonly MenuDefinition[] =
    [
        {
            identifier: "file",
            label: text ( "menu.file" ),
            entries:
            [
                menuItem ( "file-new", "menu.file.new", requestNewDocument ),
                menuItem ( "file-open", "menu.file.open", () => void requestOpenDocument () ),
                menuItem ( "file-save", "menu.file.save", () => void requestSaveDocument ( false ) ),
                menuItem ( "file-save-as", "menu.file.save.as", () => void requestSaveDocument ( true ) ),
                menuItem ( "file-close", "menu.file.close", requestCloseDocument ),
                separator (),
                menuItem ( "file-validate", "menu.file.validate", requestValidate ),
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
                menuItem ( "file-settings", "menu.file.settings", () => setSettingsOpen ( true ) ),
                separator (),
                menuItem ( "file-exit", "menu.file.exit", requestExit ),
            ],
        },
        {
            identifier: "edit",
            label: text ( "menu.edit" ),
            entries:
            [
                menuItem ( "edit-undo", "menu.edit.undo", requestUndo, documentSession.undoHistory.length === 0 ),
                menuItem ( "edit-redo", "menu.edit.redo", requestRedo, documentSession.redoHistory.length === 0 ),
                separator (),
                menuItem ( "edit-cut", "menu.edit.cut" ),
                menuItem ( "edit-copy", "menu.edit.copy" ),
                menuItem ( "edit-paste", "menu.edit.paste" ),
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
                        setStatusMessage ( text ( "simulator.heading" ) );
                    }
                ),
                separator (),
                menuItem (
                    "view-clear-diagnostics",
                    "menu.view.clear.messages.and.diagnostics",
                    () =>
                    {
                        setActivityMessages ( [] );
                        setStatusMessage ( text ( "status.ready" ) );
                    }
                ),
                separator (),
                {
                    kind: "item",
                    identifier: "view-theme",
                    label: text ( "menu.view.theme" ),
                    children:
                    [
                        {
                            kind: "item",
                            identifier: "theme-light",
                            label: text ( "settings.theme.light" ),
                            checked: theme === "light",
                            onSelect: () => applyTheme ( "light" ),
                        },
                        {
                            kind: "item",
                            identifier: "theme-dark",
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

    const guideContext: GuideContext = activeTab === "simulator" ? "simulator" : currentContext;
    const shellStyle: ShellStyle =
    {
        "--diagnostics-height": `${diagnosticsHeight}px`,
        "--outline-width": `${outlineWidth}px`,
        "--user-guide-width": `${userGuideWidth}px`,
    };

    return (
        <div className="application-shell" data-theme={ theme } style={ shellStyle }>
            <h1 className="visually-hidden">{ documentWindowTitle ( documentSession ) }</h1>
            <header>
                <MenuBar menus={ menus } />
            </header>
            <main className="workspace">
                <div className="upper-workspace">
                    <GroupBox className="workspace-panel outline-panel" legend={ text ( "outline.title" ) }>
                        <NavigationTree
                            expandedItems={ expandedTreeItems }
                            onExpandedItemsChange={ setExpandedTreeItems }
                            onSelect={ selectTreeContext }
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
                                            onApplyEntity={ requestApplyEntity }
                                            onApplyModelDetails={ requestApplyModelDetails }
                                            onDeleteEntity={ requestDeleteEntity }
                                            onDuplicateEntity={ requestDuplicateEntity }
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
                    ariaLabel={ text ( "web.splitter.diagnostics" ) }
                    className="diagnostics-splitter"
                    direction={ -1 }
                    maximum={ layoutBounds.diagnosticsHeightMaximum }
                    minimum={ layoutBounds.diagnosticsHeightMinimum }
                    onChange={ setDiagnosticsHeight }
                    orientation="horizontal"
                    value={ diagnosticsHeight }
                />
                <DiagnosticsPanel messages={ diagnosticsMessages } />
            </main>
            <AboutDialog onClose={ () => setAboutOpen ( false ) } open={ aboutOpen } />
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
                onApply={ applySettings }
                onCancel={ () => setSettingsOpen ( false ) }
                onResetBuiltIn={ () => void requestResetBuiltIn () }
                onTestConnection={ settings => void requestTestConnection ( settings ) }
                open={ settingsOpen }
                operationRunning={ backgroundOperation !== null }
                serverSettings={ serverSettings }
                theme={ theme }
            />
            <StatusBar
                backgroundOperation={ backgroundOperation?.label ?? null }
                connectionState={ text (
                    connectionState === "READY"
                        ? "web.server.ready"
                        : connectionState === "FAILED"
                            ? "web.server.failed"
                            : connectionState === "UNTESTED"
                                ? "web.server.untested"
                                : "web.server.starting"
                ) }
                dirty={ dirty }
                documentName={ documentDisplayName ( documentSession ) }
                onCancel={ cancelBackgroundOperation }
                serverTarget={ serverSettings.target === "built-in"
                    ? text ( "web.server.target" )
                    : text ( "web.server.real" ) }
                statusMessage={ statusMessage }
                validationErrorCount={ documentSession.content.validationMessages.length }
            />
        </div>
    );
}
