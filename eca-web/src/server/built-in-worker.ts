// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    built-in-worker
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Hosts the browser-local ECA server in a dedicated Web Worker with request cancellation.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import exampleModelDocument from "../../../examples/eca-rule-engine-example.json?raw";

import { BuiltInServer } from "./built-in-server";
import type { WorkerInboundMessage, WorkerOutboundMessage } from "./protocol";

interface WorkerScope
{
    onmessage: ( event: MessageEvent <WorkerInboundMessage> ) => void;
    postMessage ( message: WorkerOutboundMessage ): void;
}

const workerScope = self as unknown as WorkerScope;
const cancelledRequests = new Set <string> ();

function createCapability (): string
{
    const bytes = new Uint8Array ( 32 );

    crypto.getRandomValues ( bytes );

    return Array.from ( bytes ).map ( value => value.toString ( 16 ).padStart ( 2, "0" ) ).join ( "" );
}

const capability = createCapability ();
const serverPromise = BuiltInServer.create ( exampleModelDocument, capability );

serverPromise.then (
    server => workerScope.postMessage (
        {
            kind: "ready",
            capability,
            modelIdentifier: server.modelIdentifier,
            modelRevision: server.modelRevision,
        }
    ),
    error => workerScope.postMessage (
        {
            kind: "failure",
            message: error instanceof Error ? error.message : String ( error ),
        }
    )
);

workerScope.onmessage = event =>
{
    const message = event.data;

    if ( message.kind === "cancel" )
    {
        cancelledRequests.add ( message.requestIdentifier );
        return;
    }

    void serverPromise.then (
        async server =>
        {
            if ( cancelledRequests.delete ( message.requestIdentifier ) )
            {
                return;
            }

            const response = await server.handle ( message.request );

            if ( cancelledRequests.delete ( message.requestIdentifier ) )
            {
                return;
            }

            workerScope.postMessage (
                {
                    kind: "response",
                    requestIdentifier: message.requestIdentifier,
                    response,
                }
            );
        },
        error => workerScope.postMessage (
            {
                kind: "failure",
                requestIdentifier: message.requestIdentifier,
                message: error instanceof Error ? error.message : String ( error ),
            }
        )
    );
};
