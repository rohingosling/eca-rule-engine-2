// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    phase-15.spec
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies compiler and pure evaluator parity, exact integer semantics, null and absence, determinism, and purity.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

import fastCheck from "fast-check";
import { describe, expect, test } from "vitest";

import {
    AuthoringModelCompiler,
    AuthoringModelJsonCodec,
    EventOccurrenceJsonCodec,
    ModelCompilationError,
    OccurrenceCompilationError,
    RuleEvaluator,
    calculateSpecificity,
    compileOccurrence,
    conditionSetMatches,
    createCompiledModelSummary,
    type AuthoringModel,
    type CompiledCondition,
    type CompiledConditionSet,
    type CompiledEventDefinition,
    type CompiledEventOccurrence,
    type CompiledModel,
    type CompiledParameterDefinition,
    type CompiledPayloadValue,
    type EvaluationResult,
} from "../src/model";

const PRIVATE_PROJECT_PATH = fileURLToPath ( new URL ( "../../", import.meta.url ) );
const CONTRACT_FIXTURE_PATH = "eca-tests/src/test/resources/fixtures/contract";
const PHASE_15_MODEL_PATH = `${CONTRACT_FIXTURE_PATH}/valid/phase-15-evaluation.json`;
const VECTOR_PATH = `${CONTRACT_FIXTURE_PATH}/evaluation/phase-15-evaluation-vectors.json`;
const MATRIX_PATH = `${CONTRACT_FIXTURE_PATH}/evaluation/phase-15-null-absence-matrix.json`;
const SIGNED_LONG_MINIMUM = -9223372036854775808n;
const SIGNED_LONG_MAXIMUM = 9223372036854775807n;

interface SharedModelReference
{
    readonly path: string;
    readonly revision: string;
}

interface SharedEvaluationVector
{
    readonly id: string;
    readonly model: string;
    readonly occurrence: string;
    readonly expected: EvaluationResult;
}

interface SharedEvaluationContract
{
    readonly models: Readonly <Record <string, SharedModelReference>>;
    readonly vectors: readonly SharedEvaluationVector[];
}

interface NullAbsenceScenario
{
    readonly payload: string;
    readonly condition: string;
    readonly match: boolean;
    readonly specificity: number;
}

function readProjectFile ( relativePath: string ): string
{
    return readFileSync ( `${PRIVATE_PROJECT_PATH}${relativePath}`, "utf8" ).replace ( /\r\n/g, "\n" );
}

function compileProjectModel ( relativePath: string, revision: string ): CompiledModel
{
    const model = new AuthoringModelJsonCodec ().read ( readProjectFile ( relativePath ) );

    return new AuthoringModelCompiler ().compile ( model, revision );
}

function reverseAuthoringOrder ( model: AuthoringModel ): AuthoringModel
{
    return {
        ...model,
        parameters: [ ...model.parameters ].reverse ().map ( parameter =>
            ( { ...parameter, enumerationValues: [ ...parameter.enumerationValues ].reverse () } )
        ),
        payloads: [ ...model.payloads ].reverse ().map ( payload =>
            ( { ...payload, parameterIds: [ ...payload.parameterIds ].reverse () } )
        ),
        events: [ ...model.events ].reverse (),
        conditions: [ ...model.conditions ].reverse (),
        conditionSets: [ ...model.conditionSets ].reverse ().map ( conditionSet =>
            conditionSet.kind === "predefined"
                ? { ...conditionSet, conditionIds: [ ...conditionSet.conditionIds ].reverse () }
                : { ...conditionSet, bindings: new Map ( [ ...conditionSet.bindings ].reverse () ) }
        ),
        actions: [ ...model.actions ].reverse (),
        rules: [ ...model.rules ].reverse (),
    };
}

function matrixParameterDefinitions (): readonly CompiledParameterDefinition[]
{
    return Object.freeze (
        [ 0, 1, 2 ].map ( index => Object.freeze (
            {
                id: `key-${index}`,
                type: "STRING" as const,
                enumerationValues: Object.freeze ( [] ),
            }
        ) )
    );
}

function matrixConditionSet (
    states: string,
    parameters: readonly CompiledParameterDefinition[]
): CompiledConditionSet
{
    const bindings = new Map <string, CompiledCondition> ();

    for ( let i = 0; i < states.length; i++ )
    {
        const parameter = parameters [ i ];
        const state = states [ i ];

        if ( parameter === undefined || state === "A" )
        {
            continue;
        }

        bindings.set (
            parameter.id,
            state === "N"
                ? Object.freeze ( { kind: "wildcard", parameter } )
                : Object.freeze ( { kind: "concrete", parameter, value: "value" } )
        );
    }

    return Object.freeze ( { id: "matrix-condition-set", bindings } );
}

function matrixOccurrence (
    states: string,
    parameters: readonly CompiledParameterDefinition[]
): CompiledEventOccurrence
{
    const payload = new Map <string, CompiledPayloadValue> ();

    for ( let i = 0; i < states.length; i++ )
    {
        const parameter = parameters [ i ];
        const state = states [ i ];

        if ( parameter === undefined || state === "A" )
        {
            continue;
        }

        payload.set (
            parameter.id,
            state === "N"
                ? Object.freeze ( { kind: "present-null", parameter } )
                : Object.freeze ( { kind: "concrete", parameter, value: "value" } )
        );
    }

    const event: CompiledEventDefinition = Object.freeze (
        {
            id: "matrix-event",
            payload: Object.freeze ( { id: "matrix-payload", parameters: new Map () } ),
        }
    );

    return Object.freeze ( { event, payload } );
}

describe ( "shared Java and TypeScript evaluation contract", () =>
{
    const contract = JSON.parse ( readProjectFile ( VECTOR_PATH ) ) as SharedEvaluationContract;
    const occurrenceCodec = new EventOccurrenceJsonCodec ();
    const compiledModels = new Map <string, CompiledModel> (
        Object.entries ( contract.models ).map ( ( [ modelIdentifier, modelReference ] ) =>
            [ modelIdentifier, compileProjectModel ( modelReference.path, modelReference.revision ) ]
        )
    );

    test ( "matches the checked-in compiled summary", () =>
    {
        const reference = contract.models [ "phase-14" ];

        expect ( reference ).toBeDefined ();

        const compiledModel = compiledModels.get ( "phase-14" );
        const expectedSummary = JSON.parse (
            readProjectFile ( `${CONTRACT_FIXTURE_PATH}/expected/phase-15-compiled-summary.json` )
        );

        expect ( compiledModel ).toBeDefined ();
        expect ( createCompiledModelSummary ( compiledModel! ) ).toEqual ( expectedSummary );
    } );

    test ( "returns every checked-in golden evaluation result exactly", async () =>
    {
        for ( const [ modelIdentifier, modelReference ] of Object.entries ( contract.models ) )
        {
            const sourceModel = new AuthoringModelJsonCodec ().read ( readProjectFile ( modelReference.path ) );

            expect ( await new AuthoringModelJsonCodec ().revision ( sourceModel ), modelIdentifier )
                .toBe ( modelReference.revision );
        }

        for ( const vector of contract.vectors )
        {
            const compiledModel = compiledModels.get ( vector.model );

            expect ( compiledModel, vector.id ).toBeDefined ();

            const occurrence = compileOccurrence (
                compiledModel!,
                occurrenceCodec.read ( vector.occurrence )
            );
            const result = new RuleEvaluator ( compiledModel! ).evaluate ( occurrence );

            expect ( result, vector.id ).toEqual ( vector.expected );
        }
    } );
} );

describe ( "null, omission, and typed comparison semantics", () =>
{
    const revision = "sha256:006f487c5123c12ad14a90c73bfa240198bdafe91eb8e26ba1eee1b7cb337d96";
    const compiledModel = compileProjectModel ( PHASE_15_MODEL_PATH, revision );
    const evaluator = new RuleEvaluator ( compiledModel );
    const occurrenceCodec = new EventOccurrenceJsonCodec ();

    test ( "matches all 24 technical-note null and absence rows", () =>
    {
        const scenarios = JSON.parse ( readProjectFile ( MATRIX_PATH ) ) as readonly NullAbsenceScenario[];
        const parameters = matrixParameterDefinitions ();

        expect ( scenarios ).toHaveLength ( 24 );

        for ( const [ index, scenario ] of scenarios.entries () )
        {
            const conditionSet = matrixConditionSet ( scenario.condition, parameters );
            const occurrence = matrixOccurrence ( scenario.payload, parameters );

            expect ( conditionSetMatches ( conditionSet, occurrence ), `matrix row ${index + 1}` )
                .toBe ( scenario.match );
            expect ( calculateSpecificity ( conditionSet ), `matrix row ${index + 1}` )
                .toBe ( scenario.specificity );
        }
    } );

    test ( "rejects every parameter type mismatch and invalid occurrence reference", () =>
    {
        const invalidDocuments = [
            `{\"eventId\":\"event-type-equality\",\"payload\":{\"parameter-string\":true}}`,
            `{\"eventId\":\"event-type-equality\",\"payload\":{\"parameter-boolean\":\"true\"}}`,
            `{\"eventId\":\"event-type-equality\",\"payload\":{\"parameter-enum\":\"UNKNOWN\"}}`,
            `{\"eventId\":\"event-type-equality\",\"payload\":{\"parameter-integer\":1.5}}`,
            `{\"eventId\":\"event-equals\",\"payload\":{\"parameter-string\":\"VALUE\"}}`,
            `{\"eventId\":\"event-equals\",\"payload\":{\"parameter-unknown\":5}}`,
            `{\"eventId\":\"event-unknown\",\"payload\":{}}`,
            `{\"eventId\":\"event-equals\"}`,
        ];

        for ( const document of invalidDocuments )
        {
            expect ( () => compileOccurrence ( compiledModel, occurrenceCodec.read ( document ) ) )
                .toThrow ( OccurrenceCompilationError );
        }
    } );

    test ( "orders every generated signed 64-bit value without number conversion", () =>
    {
        fastCheck.assert (
            fastCheck.property (
                fastCheck.bigInt ( { min: SIGNED_LONG_MINIMUM, max: SIGNED_LONG_MAXIMUM } ),
                integerValue =>
                {
                    const occurrence = compileOccurrence (
                        compiledModel,
                        {
                            eventId: "event-signed-range",
                            payload: new Map ( [ [ "parameter-integer", integerValue ] ] ),
                            payloadPresent: true,
                        }
                    );

                    expect ( evaluator.evaluate ( occurrence ).outcome ).toBe ( "ACTION" );
                }
            ),
            { numRuns: 250 }
        );
    } );
} );

describe ( "deterministic immutable evaluation", () =>
{
    const revision = "sha256:006f487c5123c12ad14a90c73bfa240198bdafe91eb8e26ba1eee1b7cb337d96";
    const codec = new AuthoringModelJsonCodec ();
    const sourceModel = codec.read ( readProjectFile ( PHASE_15_MODEL_PATH ) );
    const compiledModel = new AuthoringModelCompiler ().compile ( sourceModel, revision );
    const evaluator = new RuleEvaluator ( compiledModel );

    test ( "is independent of source entity, list, and binding order", () =>
    {
        const reversedModel = new AuthoringModelCompiler ().compile (
            reverseAuthoringOrder ( sourceModel ),
            revision
        );
        const occurrenceDocument = {
            eventId: "event-tie",
            payload: new Map ( [ [ "parameter-integer", 5n ] ] ),
            payloadPresent: true,
        };

        expect ( createCompiledModelSummary ( reversedModel ) )
            .toEqual ( createCompiledModelSummary ( compiledModel ) );
        expect ( new RuleEvaluator ( reversedModel ).evaluate (
            compileOccurrence ( reversedModel, occurrenceDocument )
        ) ).toEqual ( evaluator.evaluate ( compileOccurrence ( compiledModel, occurrenceDocument ) ) );
    } );

    test ( "does not retain mutable source collections", () =>
    {
        const mutableModel = codec.read ( readProjectFile ( PHASE_15_MODEL_PATH ) );
        const snapshot = new AuthoringModelCompiler ().compile ( mutableModel, revision );
        const snapshotSummary = createCompiledModelSummary ( snapshot );

        ( mutableModel.rules as unknown as Array <AuthoringModel["rules"][number]> ).splice ( 0 );
        ( mutableModel.parameters as unknown as Array <AuthoringModel["parameters"][number]> ).reverse ();

        expect ( snapshot.ruleBase.rules ).toHaveLength ( 15 );
        expect ( createCompiledModelSummary ( snapshot ) ).toEqual ( snapshotSummary );
    } );

    test ( "preserves winners under arbitrary permitted payload extension", () =>
    {
        fastCheck.assert (
            fastCheck.property (
                fastCheck.string (),
                fastCheck.boolean (),
                fastCheck.constantFrom ( "ALPHA", "ZULU" ),
                ( stringValue, booleanValue, enumerationValue ) =>
                {
                    const occurrence = compileOccurrence (
                        compiledModel,
                        {
                            eventId: "event-specificity",
                            payload: new Map <string, string | boolean | bigint> (
                                [
                                    [ "parameter-integer", 5n ],
                                    [ "parameter-string", stringValue ],
                                    [ "parameter-boolean", booleanValue ],
                                    [ "parameter-enum", enumerationValue ],
                                ]
                            ),
                            payloadPresent: true,
                        }
                    );

                    expect ( evaluator.evaluate ( occurrence ) ).toMatchObject (
                        {
                            outcome: "ACTION",
                            actionId: "action-specific",
                            ruleId: "rule-specificity-specific",
                            specificity: 3,
                        }
                    );
                }
            ),
            { numRuns: 250 }
        );
    } );

    test ( "returns equal results for repeated and concurrent calls", async () =>
    {
        const occurrence = compileOccurrence (
            compiledModel,
            {
                eventId: "event-tie",
                payload: new Map ( [ [ "parameter-integer", 5n ] ] ),
                payloadPresent: true,
            }
        );
        const expectedResult = evaluator.evaluate ( occurrence );
        const repeatedResults = Array.from ( { length: 100 }, () => evaluator.evaluate ( occurrence ) );
        const concurrentResults = await Promise.all (
            Array.from ( { length: 100 }, async () => evaluator.evaluate ( occurrence ) )
        );

        expect ( repeatedResults.every ( result => JSON.stringify ( result ) === JSON.stringify ( expectedResult ) ) )
            .toBe ( true );
        expect ( concurrentResults ).toEqual ( repeatedResults );
        expect ( occurrence.payload.get ( "parameter-integer" ) ).toMatchObject ( { value: 5n } );
    } );

    test ( "rejects invalid authoring models before reference resolution", () =>
    {
        const invalidModel: AuthoringModel = {
            ...sourceModel,
            rules: [ ...sourceModel.rules, sourceModel.rules [ 0 ]! ],
        };

        expect ( () => new AuthoringModelCompiler ().compile ( invalidModel, revision ) )
            .toThrow ( ModelCompilationError );
    } );

    test ( "keeps the evaluator free of UI, browser, HTTP, file, worker, and JSON dependencies", () =>
    {
        const evaluatorSource = readProjectFile ( "eca-web/src/model/rule-evaluator.ts" );

        expect ( evaluatorSource ).not.toMatch ( /from\s+["']react["']/ );
        expect ( evaluatorSource ).not.toMatch (
            /\bwindow\.|\bdocument\.|\b(?:localStorage|sessionStorage)\b|\bfetch\s*\(|\bnew\s+Worker\b/
        );
        expect ( evaluatorSource ).not.toMatch ( /\bJSON\.(?:parse|stringify)\b|node:fs|https?:\/\// );
    } );
} );
