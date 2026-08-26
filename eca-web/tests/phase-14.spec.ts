// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    phase-14.spec
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies strict JSON, authoring-model validation, exact integers, serialization, revisions, occurrences, and UI
//   data contracts against the Java fixture inventory.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { existsSync, readFileSync, readdirSync } from "node:fs";
import { fileURLToPath } from "node:url";

import fastCheck from "fast-check";
import { describe, expect, test } from "vitest";

import {
    AuthoringModelJsonCodec,
    EventOccurrenceJsonCodec,
    INVALID_CONDITION_RANGE,
    NON_INTEGRAL_INTEGER,
    createDocumentContent,
    createEntityDraft,
    modelContainsUnsafeNumericValue,
    validateAuthoringModel,
    type AuthoringModel,
    type AuthoringValue,
} from "../src/model";

const PRIVATE_PROJECT_PATH = fileURLToPath ( new URL ( "../../", import.meta.url ) );
const INVALID_FIXTURE_PATH = "eca-tests/src/test/resources/fixtures/invalid";
const CONTRACT_FIXTURE_PATH = "eca-tests/src/test/resources/fixtures/contract";
const SIGNED_LONG_MINIMUM = -9223372036854775808n;
const SIGNED_LONG_MAXIMUM = 9223372036854775807n;

function readProjectFile ( relativePath: string ): string
{
    return readFileSync ( `${PRIVATE_PROJECT_PATH}${relativePath}`, "utf8" ).replace ( /\r\n/g, "\n" );
}

function projectFileExists ( relativePath: string ): boolean
{
    return existsSync ( `${PRIVATE_PROJECT_PATH}${relativePath}` );
}

function minimalModelWithConditionValue ( integerLexeme: string ): string
{
    return `{"schemaVersion":"1.0","modelId":"property-model","name":"Property model","description":"Property fixture.",`
        + `"parameters":[{"id":"parameter-integer","name":"Integer","description":"Integer value.","type":"INTEGER"}],`
        + `"payloads":[],"events":[],"conditions":[{"id":"condition-integer","name":"Integer condition",`
        + `"description":"Integer predicate.","parameterId":"parameter-integer","operator":"EQUALS",`
        + `"value":${integerLexeme}}],"conditionSets":[],"actions":[],"rules":[]}`;
}

function modelWithEntityDocument ( category: string, entityDocument: string ): string
{
    const categoryNames = [
        "parameters",
        "payloads",
        "events",
        "conditions",
        "conditionSets",
        "actions",
        "rules",
    ];
    const categories = categoryNames.map (
        categoryName => `"${categoryName}":${categoryName === category ? `[${entityDocument}]` : "[]"}`
    );

    return `{"schemaVersion":"1.0","modelId":"duplicate-model","name":"Duplicate",`
        + `"description":"Duplicate fixture.",${categories.join ( "," )}}`;
}

function reverseEntityOrdering ( model: AuthoringModel ): AuthoringModel
{
    return {
        ...model,
        parameters: [ ...model.parameters ].reverse (),
        payloads: [ ...model.payloads ].reverse (),
        events: [ ...model.events ].reverse (),
        conditions: [ ...model.conditions ].reverse (),
        conditionSets: [ ...model.conditionSets ].reverse (),
        actions: [ ...model.actions ].reverse (),
        rules: [ ...model.rules ].reverse (),
    };
}

function assertNoOrdinaryNumbers ( value: AuthoringValue | undefined ): void
{
    if ( value === undefined || value === null || typeof value === "string" || typeof value === "boolean" )
    {
        return;
    }

    expect ( typeof value ).not.toBe ( "number" );

    if ( Array.isArray ( value ) )
    {
        value.forEach ( assertNoOrdinaryNumbers );
    }
    else if ( value instanceof Map )
    {
        value.forEach ( assertNoOrdinaryNumbers );
    }
}

describe ( "Java fixture parity", () =>
{
    const codec = new AuthoringModelJsonCodec ();
    const semanticCodes = new Map (
        [
            [ "unsupported-schema-version.json", "UNSUPPORTED_SCHEMA_VERSION" ],
            [ "blank-field.json", "BLANK_FIELD" ],
            [ "invalid-identifier.json", "INVALID_IDENTIFIER" ],
            [ "duplicate-identifier.json", "DUPLICATE_IDENTIFIER" ],
            [ "unresolved-reference.json", "UNRESOLVED_REFERENCE" ],
            [ "invalid-parameter-type.json", "INVALID_PARAMETER_TYPE" ],
            [ "invalid-enum-domain.json", "INVALID_ENUM_DOMAIN" ],
            [ "duplicate-reference.json", "DUPLICATE_REFERENCE" ],
            [ "invalid-concrete-value.json", "INVALID_CONCRETE_VALUE" ],
            [ "non-integral-integer.json", "NON_INTEGRAL_INTEGER" ],
            [ "enum-value-out-of-domain.json", "ENUM_VALUE_OUT_OF_DOMAIN" ],
            [ "duplicate-parameter-binding.json", "DUPLICATE_PARAMETER_BINDING" ],
            [ "rule-condition-not-permitted.json", "RULE_CONDITION_NOT_PERMITTED" ],
        ]
    );

    test ( "accepts every valid Java authoring-model fixture", () =>
    {
        const fixturePaths = [ "examples/eca-rule-engine-example.json" ];
        const privateCourierFixturePath = "examples/courier-selection-example.json";

        if ( projectFileExists ( privateCourierFixturePath ) )
        {
            fixturePaths.push ( privateCourierFixturePath );
        }

        for ( const fixturePath of fixturePaths )
        {
            const model = codec.read ( readProjectFile ( fixturePath ) );

            expect ( validateAuthoringModel ( model ), fixturePath ).toEqual ( [] );
            expect ( codec.read ( codec.writePretty ( model ) ), fixturePath ).toEqual ( model );
        }
    } );

    test ( "rejects every structurally invalid Java fixture", () =>
    {
        for ( const fixtureName of [
            "malformed-json.json",
            "duplicate-member.json",
            "unknown-field.json",
            "wrong-json-type.json",
        ] )
        {
            expect (
                () => codec.read ( readProjectFile ( `${INVALID_FIXTURE_PATH}/${fixtureName}` ) ),
                fixtureName
            ).toThrow ();
        }
    } );

    test ( "emits every Java semantic fixture diagnostic", () =>
    {
        for ( const [ fixtureName, expectedCode ] of semanticCodes )
        {
            const model = codec.read ( readProjectFile ( `${INVALID_FIXTURE_PATH}/${fixtureName}` ) );
            const codes = validateAuthoringModel ( model ).map ( diagnostic => diagnostic.code );

            expect ( codes, fixtureName ).toContain ( expectedCode );
        }

        const sizeLimitedModel = codec.read ( readProjectFile ( `${INVALID_FIXTURE_PATH}/size-limit.json` ) );
        const sizeCodes = validateAuthoringModel (
            sizeLimitedModel,
            { maximumEntitiesPerCategory: 0, maximumBindingsPerConditionSet: 0 }
        ).map ( diagnostic => diagnostic.code );

        expect ( sizeCodes ).toContain ( "SIZE_LIMIT_EXCEEDED" );
    } );
} );

describe ( "strict lossless model JSON", () =>
{
    const codec = new AuthoringModelJsonCodec ();
    const contractPath = `${CONTRACT_FIXTURE_PATH}/valid/phase-14-ordering-and-integers.json`;

    test ( "retains signed 64-bit boundaries as bigint and preserves UI ordering", () =>
    {
        const model = codec.read ( readProjectFile ( contractPath ) );
        const minimumCondition = model.conditions.find (
            condition => condition.id === "condition-integer-minimum"
        );
        const rangeCondition = model.conditions.find (
            condition => condition.id === "condition-integer-range"
        );

        expect ( minimumCondition?.value ).toBe ( SIGNED_LONG_MINIMUM );
        expect ( rangeCondition?.secondValue ).toBe ( SIGNED_LONG_MAXIMUM );
        expect ( model.parameters.map ( parameter => parameter.id ) ).toEqual (
            [ "parameter-string", "parameter-integer", "parameter-enum" ]
        );
        expect ( validateAuthoringModel ( model ) ).toEqual ( [] );
        expect ( modelContainsUnsafeNumericValue ( model ) ).toBe ( false );

        const roundTripModel = codec.read ( codec.writePretty ( model ) );
        const legacyConditionSet = roundTripModel.conditionSets.find (
            conditionSet => conditionSet.id === "condition-set-legacy"
        );
        const emptyConditionSet = roundTripModel.conditionSets.find (
            conditionSet => conditionSet.id === "condition-set-empty"
        );

        expect ( legacyConditionSet?.kind ).toBe ( "legacy" );
        expect (
            legacyConditionSet?.kind === "legacy"
                ? legacyConditionSet.bindings.get ( "condition-string-any" )
                : undefined
        ).toBeNull ();
        expect (
            emptyConditionSet?.kind === "legacy"
                ? emptyConditionSet.bindings.has ( "condition-string-any" )
                : true
        ).toBe ( false );
        expect (
            roundTripModel.conditions.find ( condition => condition.id === "condition-string-any" )?.operator
        ).toBe ( "ANY" );
        expect (
            roundTripModel.conditions.find (
                condition => condition.id === "condition-string-unconfigured"
            )?.operator
        ).toBeUndefined ();

        for ( const condition of model.conditions )
        {
            assertNoOrdinaryNumbers ( condition.value );
            assertNoOrdinaryNumbers ( condition.secondValue );
        }
    } );

    test ( "canonicalizes entity, list, enum, and binding order independently of input order", async () =>
    {
        const model = codec.read ( readProjectFile ( contractPath ) );
        const reversedModel = reverseEntityOrdering ( model );
        const expectedPretty = readProjectFile (
            `${CONTRACT_FIXTURE_PATH}/expected/phase-14-ordering-and-integers.pretty.json`
        );
        const expectedCanonical = readProjectFile (
            `${CONTRACT_FIXTURE_PATH}/expected/phase-14-ordering-and-integers.canonical.json`
        ).trimEnd ();
        const expectedRevision = readProjectFile (
            `${CONTRACT_FIXTURE_PATH}/expected/phase-14-ordering-and-integers.revision.txt`
        ).trim ();

        expect ( codec.writeCanonical ( reversedModel ) ).toEqual ( codec.writeCanonical ( model ) );
        expect ( codec.writePretty ( reversedModel ) ).toBe ( codec.writePretty ( model ) );
        expect ( await codec.revision ( reversedModel ) ).toBe ( await codec.revision ( model ) );
        expect ( codec.writePretty ( model ) ).toBe ( expectedPretty );
        expect ( codec.writeCanonicalText ( model ) ).toBe ( expectedCanonical );
        expect ( await codec.revision ( model ) ).toBe ( expectedRevision );
    } );

    test ( "rejects duplicate members at every authoring-model object level", () =>
    {
        const duplicateEntityDocuments: readonly ( readonly [ string, string ] )[] = [
            [ "parameters", `{"id":"parameter","id":"parameter-two","name":"Parameter","description":"Parameter.","type":"STRING"}` ],
            [ "payloads", `{"id":"payload","id":"payload-two","name":"Payload","description":"Payload.","parameterIds":[]}` ],
            [ "events", `{"id":"event","id":"event-two","name":"Event","description":"Event.","payloadId":"payload"}` ],
            [ "conditions", `{"id":"condition","id":"condition-two","name":"Condition","description":"Condition.","parameterId":"parameter"}` ],
            [ "conditionSets", `{"id":"condition-set","id":"condition-set-two","name":"Set","description":"Set.","bindings":{}}` ],
            [ "actions", `{"id":"action","id":"action-two","name":"Action","description":"Action."}` ],
            [ "rules", `{"id":"rule","id":"rule-two","name":"Rule","description":"Rule.","eventId":"event","conditionSetId":"condition-set","actionId":"action"}` ],
        ];

        expect (
            () => codec.read ( readProjectFile ( `${INVALID_FIXTURE_PATH}/duplicate-member.json` ) )
        ).toThrow ( /duplicate member/ );

        for ( const [ category, entityDocument ] of duplicateEntityDocuments )
        {
            expect ( () => codec.read ( modelWithEntityDocument ( category, entityDocument ) ) )
                .toThrow ( /duplicate member/ );
        }

        expect (
            () => codec.read (
                readProjectFile (
                    `${CONTRACT_FIXTURE_PATH}/invalid/phase-14-nested-duplicate-member.json`
                )
            )
        ).toThrow ( /duplicate member/ );
    } );

    test ( "rejects unknown fields at every authoring-model object level", () =>
    {
        const documents = [
            `{"schemaVersion":"1.0","modelId":"unknown","name":"Unknown","description":"Unknown.","extra":true,"parameters":[],"payloads":[],"events":[],"conditions":[],"conditionSets":[],"actions":[],"rules":[]}`,
            `{"schemaVersion":"1.0","modelId":"unknown","name":"Unknown","description":"Unknown.","parameters":[{"id":"parameter","name":"Parameter","description":"Parameter.","type":"STRING","extra":true}],"payloads":[],"events":[],"conditions":[],"conditionSets":[],"actions":[],"rules":[]}`,
            `{"schemaVersion":"1.0","modelId":"unknown","name":"Unknown","description":"Unknown.","parameters":[],"payloads":[{"id":"payload","name":"Payload","description":"Payload.","parameterIds":[],"extra":true}],"events":[],"conditions":[],"conditionSets":[],"actions":[],"rules":[]}`,
            `{"schemaVersion":"1.0","modelId":"unknown","name":"Unknown","description":"Unknown.","parameters":[],"payloads":[],"events":[{"id":"event","name":"Event","description":"Event.","payloadId":"payload","extra":true}],"conditions":[],"conditionSets":[],"actions":[],"rules":[]}`,
            `{"schemaVersion":"1.0","modelId":"unknown","name":"Unknown","description":"Unknown.","parameters":[],"payloads":[],"events":[],"conditions":[{"id":"condition","name":"Condition","description":"Condition.","parameterId":"parameter","extra":true}],"conditionSets":[],"actions":[],"rules":[]}`,
            `{"schemaVersion":"1.0","modelId":"unknown","name":"Unknown","description":"Unknown.","parameters":[],"payloads":[],"events":[],"conditions":[],"conditionSets":[{"id":"condition-set","name":"Set","description":"Set.","bindings":{},"extra":true}],"actions":[],"rules":[]}`,
            `{"schemaVersion":"1.0","modelId":"unknown","name":"Unknown","description":"Unknown.","parameters":[],"payloads":[],"events":[],"conditions":[],"conditionSets":[],"actions":[{"id":"action","name":"Action","description":"Action.","extra":true}],"rules":[]}`,
            `{"schemaVersion":"1.0","modelId":"unknown","name":"Unknown","description":"Unknown.","parameters":[],"payloads":[],"events":[],"conditions":[],"conditionSets":[],"actions":[],"rules":[{"id":"rule","name":"Rule","description":"Rule.","eventId":"event","conditionSetId":"condition-set","actionId":"action","extra":true}]}`,
        ];

        for ( const document of documents )
        {
            expect ( () => codec.read ( document ) ).toThrow ( /unknown field extra/ );
        }
    } );

    test ( "diagnoses signed 64-bit overflow and reversed ranges", () =>
    {
        const overflowModel = codec.read (
            readProjectFile ( `${CONTRACT_FIXTURE_PATH}/invalid/phase-14-integer-overflow.json` )
        );
        const rangeModel = codec.read (
            readProjectFile ( `${CONTRACT_FIXTURE_PATH}/invalid/phase-14-invalid-range.json` )
        );

        expect ( validateAuthoringModel ( overflowModel ).map ( value => value.code ) )
            .toContain ( NON_INTEGRAL_INTEGER );
        expect ( modelContainsUnsafeNumericValue ( overflowModel ) ).toBe ( true );
        expect ( validateAuthoringModel ( rangeModel ).map ( value => value.code ) )
            .toContain ( INVALID_CONDITION_RANGE );
    } );

    test ( "round-trips arbitrary signed 64-bit integers without a number conversion", () =>
    {
        fastCheck.assert (
            fastCheck.property (
                fastCheck.bigInt ( { min: SIGNED_LONG_MINIMUM, max: SIGNED_LONG_MAXIMUM } ),
                integerValue =>
                {
                    const model = codec.read ( minimalModelWithConditionValue ( integerValue.toString () ) );
                    const value = model.conditions [ 0 ]?.value;

                    expect ( typeof value ).toBe ( "bigint" );
                    expect ( value ).toBe ( integerValue );
                    expect ( codec.read ( codec.writePretty ( model ) ).conditions [ 0 ]?.value )
                        .toBe ( integerValue );
                }
            ),
            { numRuns: 250 }
        );
    } );
} );

describe ( "occurrence null and omission states", () =>
{
    const codec = new EventOccurrenceJsonCodec ();

    test ( "round-trips concrete, present-null, omitted-property, and omitted-payload fixtures", () =>
    {
        for ( const fixtureName of [
            "concrete.json",
            "present-null.json",
            "omitted-property.json",
            "omitted-payload.json",
        ] )
        {
            const document = readProjectFile (
                `${CONTRACT_FIXTURE_PATH}/occurrence/${fixtureName}`
            ).trimEnd ();
            const occurrence = codec.read ( document );

            expect ( codec.write ( occurrence ), fixtureName ).toBe ( document );
        }

        const presentNull = codec.read (
            readProjectFile ( `${CONTRACT_FIXTURE_PATH}/occurrence/present-null.json` )
        );
        const omittedProperty = codec.read (
            readProjectFile ( `${CONTRACT_FIXTURE_PATH}/occurrence/omitted-property.json` )
        );
        const omittedPayload = codec.read (
            readProjectFile ( `${CONTRACT_FIXTURE_PATH}/occurrence/omitted-payload.json` )
        );

        expect ( presentNull.payload.has ( "parameter-string" ) ).toBe ( true );
        expect ( presentNull.payload.get ( "parameter-string" ) ).toBeNull ();
        expect ( omittedProperty.payload.has ( "parameter-string" ) ).toBe ( false );
        expect ( omittedProperty.payloadPresent ).toBe ( true );
        expect ( omittedPayload.payloadPresent ).toBe ( false );
    } );
} );

describe ( "UI-safe framework boundary", () =>
{
    const codec = new AuthoringModelJsonCodec ();
    const model = codec.read (
        readProjectFile ( `${CONTRACT_FIXTURE_PATH}/valid/phase-14-ordering-and-integers.json` )
    );

    test ( "creates summaries and drafts without React state", () =>
    {
        const content = createDocumentContent ( model );
        const anyDraft = createEntityDraft ( model, "conditions", "condition-string-any" );
        const unconfiguredDraft = createEntityDraft (
            model,
            "conditions",
            "condition-string-unconfigured"
        );
        const legacyDraft = createEntityDraft ( model, "condition-sets", "condition-set-legacy" );

        expect ( content.summary.parameterCount ).toBe ( 3 );
        expect ( content.validationErrors ).toBe ( false );
        expect ( anyDraft.conditionOperator ).toBe ( "ANY" );
        expect ( unconfiguredDraft.conditionOperator ).toBe ( "" );
        expect ( legacyDraft.bindings.get ( "condition-string-any" )?.state ).toBe ( "WILDCARD" );
        expect ( legacyDraft.bindings.get ( "condition-string-unconfigured" )?.state ).toBe ( "OMITTED" );
    } );

    test ( "keeps the implementation free of React and browser integration dependencies", () =>
    {
        const modelDirectory = `${PRIVATE_PROJECT_PATH}eca-web/src/model`;

        for ( const sourceName of readdirSync ( modelDirectory ).filter ( name => name.endsWith ( ".ts" ) ) )
        {
            const source = readFileSync ( `${modelDirectory}/${sourceName}`, "utf8" );

            expect ( source, sourceName ).not.toMatch ( /from\s+["']react["']/ );
            expect ( source, sourceName ).not.toMatch (
                /\bwindow\.|\bglobalThis\.document\b|\bdocument\.(?:body|createElement|getElementById|querySelector)|\b(?:localStorage|sessionStorage)\b|\bfetch\s*\(|\bnew\s+Worker\b/
            );
        }
    } );
} );
