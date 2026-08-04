// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    phase-17.spec
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies immutable SDI history, browser-file fallbacks, seven editors, reference safety, and typed controls.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

import { describe, expect, test, vi } from "vitest";

import {
    DOCUMENT_HISTORY_LIMIT,
    BrowserDocumentFileService,
    applyDocumentEdit,
    createLoadedDocumentSession,
    createNewDocumentSession,
    documentIsDirty,
    documentWindowTitle,
    markDocumentSaved,
    redoDocumentEdit,
    selectDocumentEntity,
    undoDocumentEdit,
    type BrowserFileEnvironment,
    type DocumentFileHandle,
    type DocumentWritableStream,
} from "../src/document";
import {
    AuthoringModelJsonCodec,
    applyEntityDraft,
    applyModelDetails,
    conditionSetSpecificity,
    createExistingEntityDraft,
    createNewEntityDraft,
    deleteEntity,
    duplicateEntity,
    findEntityReferences,
    supportedConditionOperators,
    updateLegacyBinding,
    type AuthoringModel,
    type EntityDraft,
    type ModelCategory,
} from "../src/model";

const PRIVATE_PROJECT_PATH = fileURLToPath ( new URL ( "../../", import.meta.url ) );

function loadExampleModel (): AuthoringModel
{
    const source = readFileSync ( `${PRIVATE_PROJECT_PATH}examples/eca-rule-engine-example.json`, "utf8" );

    return new AuthoringModelJsonCodec ().read ( source );
}

function requireEditedModel ( model: AuthoringModel | null ): AuthoringModel
{
    if ( model === null )
    {
        throw new Error ( "Expected the model edit to succeed." );
    }

    return model;
}

function testFile ( name: string, contents: string ): File
{
    return new File ( [ contents ], name, { type: "application/json" } );
}

describe ( "document session", () =>
{
    test ( "tracks a clean checkpoint across immutable edits, undo, redo, and save", () =>
    {
        const initialSession = createNewDocumentSession ();
        const editedModel = {
            ...initialSession.content.authoringModel,
            description: "First edit",
        };
        const editedSession = applyDocumentEdit ( initialSession, editedModel );

        expect ( documentIsDirty ( initialSession ) ).toBe ( false );
        expect ( documentIsDirty ( editedSession ) ).toBe ( true );
        expect ( documentWindowTitle ( editedSession ) ).toContain ( "Untitled *" );
        expect ( initialSession.content.authoringModel.description ).toBe ( "New ECA model." );

        const undoneSession = undoDocumentEdit ( editedSession );
        const redoneSession = redoDocumentEdit ( undoneSession );

        expect ( undoneSession.content ).toBe ( initialSession.content );
        expect ( documentIsDirty ( undoneSession ) ).toBe ( false );
        expect ( redoneSession.content.authoringModel.description ).toBe ( "First edit" );

        const savedSession = markDocumentSaved ( redoneSession, "model.json", null );

        expect ( documentIsDirty ( savedSession ) ).toBe ( false );
        expect ( documentWindowTitle ( savedSession ) ).toContain ( "model.json" );
    } );

    test ( "preserves a later edit when an earlier asynchronous save completes", () =>
    {
        const initialSession = createNewDocumentSession ();
        const saveContentSession = applyDocumentEdit (
            initialSession,
            { ...initialSession.content.authoringModel, name: "Save snapshot" }
        );
        const laterSession = applyDocumentEdit (
            saveContentSession,
            { ...saveContentSession.content.authoringModel, name: "Later edit" }
        );
        const completedSession = markDocumentSaved (
            laterSession,
            "model.json",
            null,
            saveContentSession.content
        );

        expect ( completedSession.content.authoringModel.name ).toBe ( "Later edit" );
        expect ( completedSession.cleanContent.authoringModel.name ).toBe ( "Save snapshot" );
        expect ( documentIsDirty ( completedSession ) ).toBe ( true );
    } );

    test ( "assigns replacement documents a new identity for asynchronous completion guards", () =>
    {
        const firstDocument = createNewDocumentSession ();
        const replacementDocument = createNewDocumentSession ();
        const editedDocument = applyDocumentEdit (
            firstDocument,
            { ...firstDocument.content.authoringModel, name: "Edit" }
        );

        expect ( editedDocument.documentIdentity ).toBe ( firstDocument.documentIdentity );
        expect ( replacementDocument.documentIdentity ).not.toBe ( firstDocument.documentIdentity );
    } );

    test ( "bounds history and normalizes a selection removed by an edit", () =>
    {
        const model = loadExampleModel ();
        let session = createLoadedDocumentSession ( model, "example.json", null );

        session = selectDocumentEntity ( session, "actions", "action-concierge" );
        session = applyDocumentEdit (
            session,
            { ...model, actions: model.actions.filter ( action => action.id !== "action-concierge" ) }
        );

        expect ( session.selection ).toEqual ( { kind: "category", category: "actions" } );

        for ( let i = 0; i < DOCUMENT_HISTORY_LIMIT + 5; i++ )
        {
            session = applyDocumentEdit (
                session,
                { ...session.content.authoringModel, description: `Edit ${i}` }
            );
        }

        expect ( session.undoHistory ).toHaveLength ( DOCUMENT_HISTORY_LIMIT );
    } );
} );

describe ( "browser file service", () =>
{
    test ( "opens through a retained browser file handle", async () =>
    {
        const fileHandle: DocumentFileHandle =
        {
            name: "opened.json",
            createWritable: vi.fn (),
            getFile: async () => testFile ( "opened.json", "{\"modelId\":\"opened\"}" ),
        };
        const environment: BrowserFileEnvironment =
        {
            chooseFallbackFile: vi.fn (),
            download: vi.fn (),
            showOpenFilePicker: async () => [ fileHandle ],
        };

        const openedDocument = await new BrowserDocumentFileService ( environment ).open ();

        expect ( openedDocument ).toEqual (
            {
                contents: "{\"modelId\":\"opened\"}",
                fileHandle,
                fileName: "opened.json",
            }
        );
        expect ( environment.chooseFallbackFile ).not.toHaveBeenCalled ();
    } );

    test ( "uses fallback open and truthful download save without retaining a handle", async () =>
    {
        const download = vi.fn ();
        const environment: BrowserFileEnvironment =
        {
            chooseFallbackFile: async () => testFile ( "fallback.json", "{}" ),
            download,
        };
        const fileService = new BrowserDocumentFileService ( environment );

        await expect ( fileService.open () ).resolves.toMatchObject (
            { contents: "{}", fileHandle: null, fileName: "fallback.json" }
        );
        await expect ( fileService.save ( "saved", "fallback.json", null, false ) ).resolves.toEqual (
            { fileHandle: null, fileName: "fallback.json", kind: "download" }
        );
        expect ( download ).toHaveBeenCalledWith ( "fallback.json", "saved" );
    } );

    test ( "writes retained and Save As handles and aborts a failed write", async () =>
    {
        const writtenContents: string[] = [];
        const writableStream: DocumentWritableStream =
        {
            close: vi.fn ( async () => undefined ),
            write: vi.fn ( async contents => { writtenContents.push ( contents ); } ),
        };
        const retainedHandle: DocumentFileHandle =
        {
            name: "retained.json",
            createWritable: async () => writableStream,
            getFile: async () => testFile ( "retained.json", "{}" ),
        };
        const saveAsHandle: DocumentFileHandle =
        {
            name: "save-as.json",
            createWritable: async () => writableStream,
            getFile: async () => testFile ( "save-as.json", "{}" ),
        };
        const environment: BrowserFileEnvironment =
        {
            chooseFallbackFile: async () => null,
            download: vi.fn (),
            showSaveFilePicker: async () => saveAsHandle,
        };
        const fileService = new BrowserDocumentFileService ( environment );

        await expect ( fileService.save ( "one", "ignored.json", retainedHandle, false ) ).resolves.toMatchObject (
            { fileHandle: retainedHandle, fileName: "retained.json", kind: "file-handle" }
        );
        await expect ( fileService.save ( "two", "suggested.json", retainedHandle, true ) ).resolves.toMatchObject (
            { fileHandle: saveAsHandle, fileName: "save-as.json", kind: "file-handle" }
        );
        expect ( writtenContents ).toEqual ( [ "one", "two" ] );

        const abort = vi.fn ( async () => undefined );
        const failedHandle: DocumentFileHandle =
        {
            name: "failed.json",
            createWritable: async () =>
            ({
                abort,
                close: async () => undefined,
                write: async () => { throw new Error ( "disk full" ); },
            }),
            getFile: async () => testFile ( "failed.json", "{}" ),
        };

        await expect ( fileService.save ( "three", "ignored.json", failedHandle, false ) )
            .rejects.toThrow ( "disk full" );
        expect ( abort ).toHaveBeenCalledOnce ();
    } );
} );

describe ( "model editors", () =>
{
    test ( "applies immutable edits for all seven categories", () =>
    {
        const originalModel = loadExampleModel ();
        const targets: readonly [ ModelCategory, string ][] =
        [
            [ "parameters",     "parameter-quantity" ],
            [ "payloads",       "payload-order-product" ],
            [ "events",         "event-order-product" ],
            [ "conditions",     "condition-quantity-any" ],
            [ "condition-sets", "condition-set-retail-non-vip-local-standard" ],
            [ "actions",        "action-local-courier" ],
            [ "rules",          "rule-test" ],
        ];

        let editedModel = originalModel;

        for ( const [ category, identifier ] of targets )
        {
            const draft = createExistingEntityDraft ( editedModel, category, identifier );
            const result = applyEntityDraft (
                editedModel,
                category,
                identifier,
                { ...draft, description: `${draft.description} Edited.` }
            );

            expect ( result.errors ).toEqual ( [] );
            editedModel = requireEditedModel ( result.model );
        }

        expect ( editedModel ).not.toBe ( originalModel );
        expect ( originalModel.rules.find ( rule => rule.id === "rule-test" )?.description ).toBe ( "Test rule." );
        expect ( editedModel.rules.find ( rule => rule.id === "rule-test" )?.description )
            .toBe ( "Test rule. Edited." );
    } );

    test ( "validates model details, identifiers, typed values, and compatible operators", () =>
    {
        const model = loadExampleModel ();

        expect ( applyModelDetails ( model, "Invalid ID", "", "" ).errors.map ( error => error.field ) )
            .toEqual ( [ "modelId", "modelName", "modelDescription" ] );

        const invalidParameterDraft = {
            ...createNewEntityDraft ( model, "parameters" ),
            id: "Invalid ID",
            parameterType: "ENUM",
            enumerationValues: [ "DUPLICATE", "DUPLICATE" ],
        };
        const invalidParameter = applyEntityDraft ( model, "parameters", null, invalidParameterDraft );

        expect ( invalidParameter.model ).toBeNull ();
        expect ( invalidParameter.errors.map ( error => error.field ) ).toContain ( "id" );
        expect ( invalidParameter.errors.map ( error => error.field ) ).toContain ( "enumerationValues" );

        const integerParameter = model.parameters.find ( parameter => parameter.type === "INTEGER" );
        const stringParameter = model.parameters.find ( parameter => parameter.type === "STRING" );

        expect ( supportedConditionOperators ( integerParameter ) ).toContain ( "BETWEEN_INCLUSIVE" );
        expect ( supportedConditionOperators ( stringParameter ) ).not.toContain ( "GREATER_THAN" );

        const invalidConditionDraft = {
            ...createNewEntityDraft ( model, "conditions" ),
            parameterId: "parameter-quantity",
            conditionOperator: "BETWEEN_INCLUSIVE",
            conditionValueText: "100",
            secondConditionValueText: "10",
        };
        const invalidCondition = applyEntityDraft ( model, "conditions", null, invalidConditionDraft );

        expect ( invalidCondition.model ).toBeNull ();
        expect ( invalidCondition.errors.map ( error => error.field ) ).toContain ( "secondConditionValueText" );
    } );

    test ( "adds, duplicates, deletes, protects references, and propagates renames", () =>
    {
        const originalModel = loadExampleModel ();
        const actionDraft = {
            ...createNewEntityDraft ( originalModel, "actions" ),
            id: "action-audit",
            name: "Audit",
            description: "Return the audit action.",
        };
        const addedActionResult = applyEntityDraft ( originalModel, "actions", null, actionDraft );
        const addedActionModel = requireEditedModel ( addedActionResult.model );
        const duplicateResult = duplicateEntity ( addedActionModel, "actions", "action-audit" );
        const duplicatedModel = requireEditedModel ( duplicateResult.model );

        expect ( duplicateResult.entityIdentifier ).toBe ( "action-audit-copy" );
        expect ( deleteEntity ( duplicatedModel, "actions", "action-audit-copy" ).model?.actions )
            .toHaveLength ( addedActionModel.actions.length );

        expect ( findEntityReferences ( originalModel, "actions", "action-local-courier" ) )
            .toEqual (
                [
                    "rule rule-cancel-order-local-courier.actionId",
                    "rule rule-order-product-non-vip-local.actionId",
                    "rule rule-return-product-non-vip-local.actionId",
                    "rule rule-test.actionId",
                ]
            );
        expect ( deleteEntity ( originalModel, "actions", "action-local-courier" ).model ).toBeNull ();

        const renamedDraft = {
            ...createExistingEntityDraft ( originalModel, "parameters", "parameter-quantity" ),
            id: "parameter-item-count",
        };
        const renamedModel = requireEditedModel (
            applyEntityDraft (
                originalModel,
                "parameters",
                "parameter-quantity",
                renamedDraft
            ).model
        );

        expect ( renamedModel.payloads [ 1 ]?.parameterIds ).toContain ( "parameter-item-count" );
        expect ( renamedModel.conditions.filter ( condition => condition.parameterId === "parameter-item-count" ) )
            .toHaveLength ( 4 );
        expect ( originalModel.payloads [ 1 ]?.parameterIds ).toContain ( "parameter-quantity" );
    } );

    test ( "edits predefined and legacy condition sets with computed specificity", () =>
    {
        const model = loadExampleModel ();
        const predefinedDraft = createExistingEntityDraft (
            model,
            "condition-sets",
            "condition-set-retail-non-vip-local-standard"
        );

        expect ( conditionSetSpecificity ( model, predefinedDraft ) ).toBe ( 9 );

        const legacyDraft = {
            ...predefinedDraft,
            bindings: new Map (),
            conditionIds: [],
            predefinedConditions: false,
        };
        const firstBinding =
        {
            conditionIdentifier: "condition-quantity-tier-1",
            state: "CONCRETE" as const,
            concreteText: "42",
        };
        const wildcardBinding =
        {
            conditionIdentifier: "condition-vip-true",
            state: "WILDCARD" as const,
            concreteText: "",
        };
        const populatedDraft: EntityDraft = {
            ...legacyDraft,
            bindings: updateLegacyBinding (
                updateLegacyBinding ( legacyDraft.bindings, firstBinding.conditionIdentifier, firstBinding ),
                wildcardBinding.conditionIdentifier,
                wildcardBinding
            ),
        };

        expect ( conditionSetSpecificity ( model, populatedDraft ) ).toBe ( 3 );

        const appliedResult = applyEntityDraft (
            model,
            "condition-sets",
            "condition-set-retail-non-vip-local-standard",
            populatedDraft
        );
        const appliedModel = requireEditedModel ( appliedResult.model );
        const conditionSet = appliedModel.conditionSets.find (
            candidate => candidate.id === "condition-set-retail-non-vip-local-standard"
        );

        expect ( conditionSet?.kind ).toBe ( "legacy" );
        expect ( conditionSet?.kind === "legacy" ? conditionSet.bindings.get ( "condition-quantity-tier-1" ) : null )
            .toBe ( 42n );
        expect ( conditionSet?.kind === "legacy" ? conditionSet.bindings.get ( "condition-vip-true" ) : undefined )
            .toBeNull ();
    } );

    test ( "rejects unconfigured predefined conditions and excludes them from specificity", () =>
    {
        const model = loadExampleModel ();
        const invalidModel: AuthoringModel = {
            ...model,
            conditions: model.conditions.map (
                condition => condition.id === "condition-quantity-any"
                    ? { ...condition, operator: "UNKNOWN" }
                    : condition
            ),
        };
        const draft = {
            ...createNewEntityDraft ( invalidModel, "condition-sets" ),
            conditionIds: [ "condition-quantity-any" ],
        };
        const result = applyEntityDraft ( invalidModel, "condition-sets", null, draft );

        expect ( conditionSetSpecificity ( invalidModel, draft ) ).toBe ( 0 );
        expect ( result.model ).toBeNull ();
        expect ( result.errors.map ( error => error.field ) )
            .toContain ( "conditionIds.condition-quantity-any" );
    } );
} );
