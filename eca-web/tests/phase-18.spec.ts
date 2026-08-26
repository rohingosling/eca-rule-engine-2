// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    phase-18.spec
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies the shared gateway, response codecs, Simulator payload states, settings, and cancellation categories.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

import { describe, expect, test } from "vitest";

import {
    AuthoringModelJsonCodec,
    type AuthoringValue,
    type PayloadValueDraft,
} from "../src/model";
import {
    BuiltInServer,
    CONNECT_TIMEOUT_SECONDS_MAXIMUM,
    GatewayFailure,
    MODEL_PATH,
    REQUEST_TIMEOUT_SECONDS_MAXIMUM,
    ServerGateway,
    TransportFailure,
    readEvaluation,
    validateServerSettings,
    validateServerSettingsFields,
    type ServerTransport,
    type TransportRequest,
    type TransportResponse,
} from "../src/server";
import {
    SimulatorDraftFailure,
    createOccurrence,
    defaultPayloadDrafts,
    simulatorEvents,
} from "../src/simulator";

const EXAMPLE_MODEL_PATH = fileURLToPath ( new URL ( "../../examples/eca-rule-engine-example.json", import.meta.url ) );
const CAPABILITY = "phase-18-test-capability";

function exampleDocument (): string
{
    return readFileSync ( EXAMPLE_MODEL_PATH, "utf8" );
}

class DirectBuiltInTransport implements ServerTransport
{
    private readonly server: BuiltInServer;

    public constructor ( server: BuiltInServer )
    {
        this.server = server;
    }

    public request ( request: TransportRequest ): Promise <TransportResponse>
    {
        const protectedRequest = request.method === "PUT" && request.path === MODEL_PATH
            ? {
                ...request,
                headers: { ...request.headers, "Authorization": `Bearer ${CAPABILITY}` },
            }
            : request;

        return this.server.handle ( protectedRequest );
    }
}

describe ( "shared server gateway", () =>
{
    test ( "tests, pulls, pushes, and evaluates through one strict transport boundary", async () =>
    {
        const server = await BuiltInServer.create ( exampleDocument (), CAPABILITY );
        const gateway = new ServerGateway ( new DirectBuiltInTransport ( server ) );
        const connection = await gateway.testConnection ();
        const pulled = await gateway.pullModel ();
        const pushed = await gateway.pushModel ( pulled.model );
        const evaluated = await gateway.evaluate (
            {
                eventId: "event-order-product",
                payload: new Map <string, AuthoringValue> (
                    [
                        [ "parameter-delivery-type", "STANDARD" ],
                        [ "parameter-product-category", "RETAIL" ],
                        [ "parameter-quantity", 1n ],
                        [ "parameter-region", "LOCAL" ],
                        [ "parameter-vip", false ],
                    ]
                ),
                payloadPresent: true,
            }
        );

        expect ( connection.liveness ).toBe ( "UP" );
        expect ( connection.readiness.modelRevision ).toBe ( server.modelRevision );
        expect ( pulled.modelRevision ).toBe ( await new AuthoringModelJsonCodec ().revision ( pulled.model ) );
        expect ( pushed.summary.modelRevision ).toBe ( pulled.modelRevision );
        expect ( evaluated ).toMatchObject (
            {
                outcome: "ACTION",
                actionId: "action-local-courier",
                ruleId: "rule-test",
                modelRevision: pulled.modelRevision,
            }
        );
        expect ( evaluated.roundTripMicroseconds ).toBeGreaterThanOrEqual ( 0 );
    } );

    test ( "maps authentication, no-model, validation, timeout, connection, protocol, and cancellation failures", async () =>
    {
        const cases: readonly [ TransportResponse | Error, GatewayFailure["category"] ][] =
        [
            [ problem ( 401, "Unauthorized" ), "AUTHENTICATION" ],
            [ problem ( 404, "No Model" ), "NO_MODEL" ],
            [ problem ( 422, "Invalid" ), "VALIDATION" ],
            [ new TransportFailure ( "REQUEST_TIMEOUT", "timed out" ), "TIMEOUT" ],
            [ new TransportFailure ( "PERMISSION_OR_UNREACHABLE", "blocked" ), "CONNECTION" ],
            [ new TransportFailure ( "PROTOCOL", "invalid" ), "PROTOCOL" ],
            [ new TransportFailure ( "CANCELLED", "cancelled" ), "CANCELLED" ],
        ];

        for ( const [ outcome, category ] of cases )
        {
            const transport: ServerTransport = {
                request: async () =>
                {
                    if ( outcome instanceof Error )
                    {
                        throw outcome;
                    }

                    return outcome;
                },
            };

            await expect ( new ServerGateway ( transport ).pullModel () ).rejects.toMatchObject ( { category } );
        }
    } );

    test ( "rejects duplicate, unknown, inconsistent, and malformed response members", () =>
    {
        expect ( () => readEvaluation (
            '{"outcome":"NO_ACTION","outcome":"ACTION","modelRevision":"sha256:x","elapsedMicroseconds":1}'
        ) ).toThrow ( /duplicate/i );
        expect ( () => readEvaluation (
            '{"outcome":"NO_ACTION","modelRevision":"sha256:x","elapsedMicroseconds":1,"extra":true}'
        ) ).toThrow ( /unknown/i );
        expect ( () => readEvaluation (
            '{"outcome":"NO_ACTION","actionId":"x","modelRevision":"sha256:x","elapsedMicroseconds":1}'
        ) ).toThrow ( /must omit/ );
        expect ( () => readEvaluation ( "{" ) ).toThrow ();
    } );
} );

describe ( "Simulator occurrence drafts", () =>
{
    const model = new AuthoringModelJsonCodec ().read ( exampleDocument () );
    const events = simulatorEvents ( model );
    const orderEvent = events.find ( event => event.identifier === "event-order-product" )!;

    test ( "derives fields and preserves Concrete, null, and Omitted states", () =>
    {
        const drafts = new Map ( defaultPayloadDrafts ( orderEvent ) );

        expect ( orderEvent.payloadFields.map ( field => field.identifier ) ).toEqual (
            [
                "parameter-delivery-type",
                "parameter-product-category",
                "parameter-quantity",
                "parameter-region",
                "parameter-vip",
            ]
        );

        drafts.set ( "parameter-delivery-type", concrete ( "STANDARD" ) );
        drafts.set ( "parameter-product-category", concrete ( "RETAIL" ) );
        drafts.set ( "parameter-quantity", concrete ( "9223372036854775807" ) );
        drafts.set ( "parameter-region", { state: "NULL", text: "LOCAL" } );

        const occurrence = createOccurrence ( orderEvent, drafts );

        expect ( occurrence.payload.get ( "parameter-delivery-type" ) ).toBe ( "STANDARD" );
        expect ( occurrence.payload.get ( "parameter-quantity" ) ).toBe ( 9223372036854775807n );
        expect ( occurrence.payload.get ( "parameter-region" ) ).toBeNull ();
        expect ( occurrence.payload.has ( "parameter-vip" ) ).toBe ( false );
    } );

    test ( "rejects invalid concrete Boolean, enum, integer, and signed 64-bit text", () =>
    {
        const invalidDrafts = [
            [ "parameter-vip", "not-boolean" ],
            [ "parameter-region", "UNKNOWN" ],
            [ "parameter-quantity", "1.5" ],
            [ "parameter-quantity", "9223372036854775808" ],
        ] as const;

        for ( const [ parameterIdentifier, value ] of invalidDrafts )
        {
            const drafts = new Map ( defaultPayloadDrafts ( orderEvent ) );

            drafts.set ( parameterIdentifier, concrete ( value ) );
            expect ( () => createOccurrence ( orderEvent, drafts ) ).toThrow ( SimulatorDraftFailure );
        }
    } );
} );

describe ( "real-server settings", () =>
{
    test ( "accepts bounded safe HTTP settings and rejects credentials or invalid timeouts", () =>
    {
        expect ( validateServerSettings (
            {
                target: "real",
                serverURL: "http://127.0.0.1:8080/",
                connectTimeoutSeconds: 5,
                requestTimeoutSeconds: 30,
                bearerToken: "session-only",
            }
        ) ).toEqual ( [] );
        expect ( validateServerSettings (
            {
                target: "real",
                serverURL: "http://user:secret@127.0.0.1:8080/?token=bad",
                connectTimeoutSeconds: 0,
                requestTimeoutSeconds: 1.5,
                bearerToken: "",
            }
        ) ).toHaveLength ( 4 );
    } );

    test ( "validates preserved real-server fields for the built-in target and enforces persisted maxima", () =>
    {
        const validationErrors = validateServerSettingsFields (
            {
                target: "built-in",
                serverURL: "http://user:secret@127.0.0.1:8080/",
                connectTimeoutSeconds: CONNECT_TIMEOUT_SECONDS_MAXIMUM + 1,
                requestTimeoutSeconds: REQUEST_TIMEOUT_SECONDS_MAXIMUM + 1,
                bearerToken: "",
            }
        );

        expect ( validationErrors.map ( error => error.field ) ).toEqual (
            [ "serverURL", "connectTimeoutSeconds", "requestTimeoutSeconds" ]
        );
        expect ( validateServerSettings (
            {
                target: "real",
                serverURL: "https://example.test/eca/",
                connectTimeoutSeconds: CONNECT_TIMEOUT_SECONDS_MAXIMUM,
                requestTimeoutSeconds: REQUEST_TIMEOUT_SECONDS_MAXIMUM,
                bearerToken: "session-only",
            }
        ) ).toEqual ( [] );
    } );
} );

function concrete ( text: string ): PayloadValueDraft
{
    return { state: "CONCRETE", text };
}

function problem ( status: number, title: string ): TransportResponse
{
    return {
        status,
        headers: { "Content-Type": "application/problem+json" },
        body: JSON.stringify (
            {
                type: "about:blank",
                title,
                status,
                detail: title,
                instance: MODEL_PATH,
            }
        ),
    };
}
