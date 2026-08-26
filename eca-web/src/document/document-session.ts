// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    document-session
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Owns the immutable web document, clean checkpoint, local file identity, selection, and bounded edit history.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import type { DocumentFileHandle } from "./file-service";
import { createDocumentContent } from "../model/document-types";
import type { AuthoringModel, DocumentContent, ModelCategory } from "../model/types";

export const DOCUMENT_HISTORY_LIMIT = 100;
export const UNTITLED_FILE_NAME = "Untitled";

export type DocumentSelection =
    | { readonly kind: "model" }
    | { readonly category: ModelCategory; readonly kind: "category" }
    | { readonly category: ModelCategory; readonly entityIdentifier: string; readonly kind: "entity" };

export interface DocumentSession
{
    readonly cleanContent: DocumentContent;
    readonly content: DocumentContent;
    readonly documentIdentity: symbol;
    readonly fileHandle: DocumentFileHandle | null;
    readonly localName: string | null;
    readonly redoHistory: readonly DocumentContent[];
    readonly selection: DocumentSelection;
    readonly undoHistory: readonly DocumentContent[];
}

export function createEmptyAuthoringModel (): AuthoringModel
{
    return {
        schemaVersion: "1.0",
        modelId: "untitled-model",
        name: "Untitled",
        description: "New ECA model.",
        parameters: [],
        payloads: [],
        events: [],
        conditions: [],
        conditionSets: [],
        actions: [],
        rules: [],
    };
}

function createSession (
    content: DocumentContent,
    localName: string | null,
    fileHandle: DocumentFileHandle | null
): DocumentSession
{
    return {
        cleanContent: content,
        content,
        documentIdentity: Symbol ( "document-session" ),
        fileHandle,
        localName,
        redoHistory: [],
        selection: { kind: "model" },
        undoHistory: [],
    };
}

function boundedHistory (
    history: readonly DocumentContent[],
    content: DocumentContent
): readonly DocumentContent[]
{
    const replacementHistory = [ ...history, content ];

    return replacementHistory.length <= DOCUMENT_HISTORY_LIMIT
        ? replacementHistory
        : replacementHistory.slice ( replacementHistory.length - DOCUMENT_HISTORY_LIMIT );
}

function normalizeSelection (
    content: DocumentContent,
    selection: DocumentSelection
): DocumentSelection
{
    if ( selection.kind !== "entity" )
    {
        return selection;
    }

    const identifiers = content.entityIdentifiers.get ( selection.category ) ?? [];

    return identifiers.includes ( selection.entityIdentifier )
        ? selection
        : { kind: "category", category: selection.category };
}

export function createNewDocumentSession (): DocumentSession
{
    return createSession ( createDocumentContent ( createEmptyAuthoringModel () ), null, null );
}

export function createLoadedDocumentSession (
    model: AuthoringModel,
    localName: string,
    fileHandle: DocumentFileHandle | null
): DocumentSession
{
    return createSession ( createDocumentContent ( model ), localName, fileHandle );
}

export function createPulledDocumentSession ( model: AuthoringModel ): DocumentSession
{
    return createSession ( createDocumentContent ( model ), null, null );
}

export function applyDocumentEdit (
    session: DocumentSession,
    model: AuthoringModel,
    selection: DocumentSelection = session.selection
): DocumentSession
{
    const replacementContent = createDocumentContent ( model );

    return {
        ...session,
        content: replacementContent,
        redoHistory: [],
        selection: normalizeSelection ( replacementContent, selection ),
        undoHistory: boundedHistory ( session.undoHistory, session.content ),
    };
}

export function undoDocumentEdit ( session: DocumentSession ): DocumentSession
{
    const replacementContent = session.undoHistory.at ( -1 );

    if ( replacementContent === undefined )
    {
        return session;
    }

    return {
        ...session,
        content: replacementContent,
        redoHistory: boundedHistory ( session.redoHistory, session.content ),
        selection: normalizeSelection ( replacementContent, session.selection ),
        undoHistory: session.undoHistory.slice ( 0, -1 ),
    };
}

export function redoDocumentEdit ( session: DocumentSession ): DocumentSession
{
    const replacementContent = session.redoHistory.at ( -1 );

    if ( replacementContent === undefined )
    {
        return session;
    }

    return {
        ...session,
        content: replacementContent,
        redoHistory: session.redoHistory.slice ( 0, -1 ),
        selection: normalizeSelection ( replacementContent, session.selection ),
        undoHistory: boundedHistory ( session.undoHistory, session.content ),
    };
}

export function markDocumentSaved (
    session: DocumentSession,
    localName: string,
    fileHandle: DocumentFileHandle | null,
    savedContent: DocumentContent = session.content
): DocumentSession
{
    return {
        ...session,
        cleanContent: savedContent,
        fileHandle,
        localName,
    };
}

export function selectDocumentModel ( session: DocumentSession ): DocumentSession
{
    return { ...session, selection: { kind: "model" } };
}

export function selectDocumentCategory (
    session: DocumentSession,
    category: ModelCategory
): DocumentSession
{
    return { ...session, selection: { kind: "category", category } };
}

export function selectDocumentEntity (
    session: DocumentSession,
    category: ModelCategory,
    entityIdentifier: string
): DocumentSession
{
    const identifiers = session.content.entityIdentifiers.get ( category ) ?? [];

    if ( !identifiers.includes ( entityIdentifier ) )
    {
        return selectDocumentCategory ( session, category );
    }

    return {
        ...session,
        selection: { kind: "entity", category, entityIdentifier },
    };
}

export function documentIsDirty ( session: DocumentSession ): boolean
{
    return session.content !== session.cleanContent;
}

export function documentDisplayName ( session: DocumentSession ): string
{
    return session.localName ?? UNTITLED_FILE_NAME;
}

export function documentWindowTitle ( session: DocumentSession ): string
{
    const dirtyMarker = documentIsDirty ( session ) ? " *" : "";

    return `ECA Rule Engine Laboratory (Version 2.1.0 - ${documentDisplayName ( session )}${dirtyMarker})`;
}
