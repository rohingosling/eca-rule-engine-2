// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    phase-16.spec
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies the complete browser-local route contract, atomic replacement, evaluation, and protected operations.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

import { describe, expect, test } from "vitest";

import {
    BuiltInServer,
    EVALUATION_PATH,
    LIVENESS_PATH,
    MODEL_PATH,
    READINESS_PATH,
    STOP_PATH,
    responseHeader,
    type TransportRequest,
} from "../src/server";

const EXAMPLE_MODEL_PATH = fileURLToPath ( new URL ( "../../examples/eca-rule-engine-example.json", import.meta.url ) );
const CAPABILITY = "phase-16-test-capability";

function exampleDocument (): string
{
    return readFileSync ( EXAMPLE_MODEL_PATH, "utf8" );
}

function authorizedRequest ( request: TransportRequest ): TransportRequest
{
    return {
        ...request,
        headers:
        {
            ...request.headers,
            "Authorization": `Bearer ${CAPABILITY}`,
        },
    };
}

describe ( "built-in server lifecycle and HTTP parity", () =>
{
    test ( "starts ready with the bundled example and serves liveness, readiness, and canonical Pull", async () =>
    {
        const server = await BuiltInServer.create ( exampleDocument (), CAPABILITY );
        const liveness = await server.handle ( { method: "GET", path: LIVENESS_PATH } );
        const readiness = await server.handle ( { method: "GET", path: READINESS_PATH } );
        const pull = await server.handle ( { method: "GET", path: MODEL_PATH } );

        expect ( liveness.status ).toBe ( 200 );
        expect ( JSON.parse ( liveness.body ) ).toEqual ( { status: "UP" } );
        expect ( readiness.status ).toBe ( 200 );
        expect ( JSON.parse ( readiness.body ) ).toEqual (
            {
                status: "READY",
                ready: true,
                modelRevision: server.modelRevision,
            }
        );
        expect ( pull.status ).toBe ( 200 );
        expect ( responseHeader ( pull, "ETag" ) ).toBe ( `"${server.modelRevision}"` );
        expect ( responseHeader ( pull, "X-Model-Revision" ) ).toBe ( server.modelRevision );
        expect ( pull.body ).not.toContain ( "\n" );
    } );

    test ( "evaluates the courier ACTION and cancellation NO_ACTION with measured worker time", async () =>
    {
        const server = await BuiltInServer.create ( exampleDocument (), CAPABILITY );
        const action = await server.handle (
            {
                method: "POST",
                path: EVALUATION_PATH,
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify (
                    {
                        eventId: "event-order-product",
                        payload:
                        {
                            "parameter-delivery-type": "STANDARD",
                            "parameter-product-category": "RETAIL",
                            "parameter-quantity": 1,
                            "parameter-region": "LOCAL",
                            "parameter-vip": false,
                        },
                    }
                ),
            }
        );
        const noAction = await server.handle (
            {
                method: "POST",
                path: EVALUATION_PATH,
                headers: { "Content-Type": "application/json" },
                body: "{\"eventId\":\"event-cancel-order\",\"payload\":{}}",
            }
        );
        const actionBody = JSON.parse ( action.body ) as Record <string, unknown>;
        const noActionBody = JSON.parse ( noAction.body ) as Record <string, unknown>;

        expect ( action.status ).toBe ( 200 );
        expect ( actionBody ).toMatchObject (
            {
                outcome: "ACTION",
                actionId: "action-local-courier",
                ruleId: "rule-test",
                specificity: 9,
                modelRevision: server.modelRevision,
            }
        );
        expect ( actionBody.elapsedMicroseconds ).toEqual ( expect.any ( Number ) );
        expect ( noAction.status ).toBe ( 200 );
        expect ( noActionBody.outcome ).toBe ( "NO_ACTION" );
        expect ( noActionBody ).not.toHaveProperty ( "actionId" );
        expect ( noActionBody ).not.toHaveProperty ( "ruleId" );
        expect ( noActionBody ).not.toHaveProperty ( "specificity" );
    } );

    test ( "protects replacement and stop with the page-session capability", async () =>
    {
        const server = await BuiltInServer.create ( exampleDocument (), CAPABILITY );
        const modelRequest = {
            method: "PUT" as const,
            path: MODEL_PATH,
            headers: { "Content-Type": "application/json" },
            body: exampleDocument (),
        };
        const missing = await server.handle ( modelRequest );
        const invalid = await server.handle (
            {
                ...modelRequest,
                headers: { ...modelRequest.headers, "Authorization": "Bearer wrong" },
            }
        );
        const accepted = await server.handle ( authorizedRequest ( modelRequest ) );
        const stopMissing = await server.handle ( { method: "POST", path: STOP_PATH } );
        const stopped = await server.handle (
            authorizedRequest ( { method: "POST", path: STOP_PATH } )
        );
        const afterStop = await server.handle ( { method: "GET", path: LIVENESS_PATH } );

        expect ( missing.status ).toBe ( 401 );
        expect ( invalid.status ).toBe ( 401 );
        expect ( responseHeader ( missing, "WWW-Authenticate" ) ).toBe ( "Bearer" );
        expect ( accepted.status ).toBe ( 200 );
        expect ( stopMissing.status ).toBe ( 401 );
        expect ( stopped.status ).toBe ( 202 );
        expect ( afterStop.status ).toBe ( 503 );
    } );

    test ( "retains the previous immutable snapshot after malformed or invalid replacement", async () =>
    {
        const server = await BuiltInServer.create ( exampleDocument (), CAPABILITY );
        const revision = server.modelRevision;
        const malformed = await server.handle (
            authorizedRequest (
                {
                    method: "PUT",
                    path: MODEL_PATH,
                    headers: { "Content-Type": "application/json" },
                    body: "{\"schemaVersion\":" ,
                }
            )
        );
        const invalidModel = exampleDocument ().replace ( '"payloadId" : "payload-cancel-order"',
            '"payloadId" : "payload-missing"' );
        const invalid = await server.handle (
            authorizedRequest (
                {
                    method: "PUT",
                    path: MODEL_PATH,
                    headers: { "Content-Type": "application/json" },
                    body: invalidModel,
                }
            )
        );
        const pull = await server.handle ( { method: "GET", path: MODEL_PATH } );

        expect ( malformed.status ).toBe ( 400 );
        expect ( invalid.status ).toBe ( 422 );
        expect ( responseHeader ( pull, "ETag" ) ).toBe ( `"${revision}"` );
        expect ( server.modelRevision ).toBe ( revision );
    } );

    test ( "classifies media, body, occurrence, route, and protocol failures distinctly", async () =>
    {
        const server = await BuiltInServer.create ( exampleDocument (), CAPABILITY );
        const unsupported = await server.handle (
            { method: "POST", path: EVALUATION_PATH, body: "{}" }
        );
        const malformed = await server.handle (
            {
                method: "POST",
                path: EVALUATION_PATH,
                headers: { "Content-Type": "application/json" },
                body: "{",
            }
        );
        const invalidOccurrence = await server.handle (
            {
                method: "POST",
                path: EVALUATION_PATH,
                headers: { "Content-Type": "application/json" },
                body: "{\"eventId\":\"event-missing\",\"payload\":{}}",
            }
        );
        const oversized = await server.handle (
            {
                method: "POST",
                path: EVALUATION_PATH,
                headers: { "Content-Type": "application/json" },
                body: "x".repeat ( 1024 * 1024 + 1 ),
            }
        );
        const missingRoute = await server.handle ( { method: "GET", path: "/api/v1/missing" } );

        expect ( unsupported.status ).toBe ( 415 );
        expect ( malformed.status ).toBe ( 400 );
        expect ( invalidOccurrence.status ).toBe ( 422 );
        expect ( oversized.status ).toBe ( 413 );
        expect ( missingRoute.status ).toBe ( 404 );

        for ( const response of [ unsupported, malformed, invalidOccurrence, oversized, missingRoute ] )
        {
            expect ( responseHeader ( response, "Content-Type" ) ).toBe ( "application/problem+json" );
            expect ( JSON.parse ( response.body ).status ).toBe ( response.status );
        }
    } );
} );
