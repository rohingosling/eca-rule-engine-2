// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    built-in-worker-transport
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Adapts the dedicated browser-local worker to the shared HTTP-shaped transport boundary.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import {
    TransportFailure,
    type ServerTransport,
    type TransportRequest,
    type TransportResponse,
    type WorkerInboundMessage,
    type WorkerOutboundMessage,
} from "./protocol";
import { MODEL_PATH, STOP_PATH } from "./built-in-server";

interface PendingRequest
{
    readonly resolve: ( response: TransportResponse ) => void;
    readonly reject: ( reason: unknown ) => void;
    readonly removeAbortListener: () => void;
}

interface ReadyState
{
    readonly capability: string;
    readonly modelIdentifier: string;
    readonly modelRevision: string;
}

export type BuiltInTransportState = "STARTING" | "READY" | "FAILED" | "CLOSED";

export class BuiltInWorkerTransport implements ServerTransport
{
    private worker: Worker;
    private readyPromise: Promise <ReadyState>;
    private resolveReady: ( state: ReadyState ) => void = () => undefined;
    private rejectReady: ( reason: unknown ) => void = () => undefined;
    private readonly pendingRequests: Map <string, PendingRequest>;
    private requestSequence: number;
    private readyState: ReadyState | null;
    private stateValue: BuiltInTransportState;

    public constructor ()
    {
        this.pendingRequests = new Map ();
        this.requestSequence = 0;
        this.readyState      = null;
        this.stateValue      = "STARTING";
        this.worker          = this.createWorker ();
        this.readyPromise    = this.createReadyPromise ();
        this.attachWorker ();
    }

    public get state (): BuiltInTransportState
    {
        return this.stateValue;
    }

    public get modelIdentifier (): string | null
    {
        return this.readyState?.modelIdentifier ?? null;
    }

    public get modelRevision (): string | null
    {
        return this.readyState?.modelRevision ?? null;
    }

    public async ready (): Promise <ReadyState>
    {
        return this.readyPromise;
    }

    public async request (
        request: TransportRequest,
        signal?: AbortSignal
    ): Promise <TransportResponse>
    {
        if ( this.stateValue === "CLOSED" )
        {
            throw new TransportFailure ( "PROTOCOL", "The built-in server transport is closed." );
        }

        const readyState = await this.readyPromise;

        if ( signal?.aborted )
        {
            throw new TransportFailure ( "CANCELLED", "The operation was cancelled." );
        }

        const requestIdentifier = `request-${++this.requestSequence}`;
        const protectedOperation =
            ( request.method === "PUT" && request.path === MODEL_PATH )
            || ( request.method === "POST" && request.path === STOP_PATH );
        const authorizedRequest: TransportRequest = protectedOperation
            ? {
                ...request,
                headers:
                {
                    ...request.headers,
                    "Authorization": `Bearer ${readyState.capability}`,
                },
            }
            : request;

        return new Promise <TransportResponse> ( ( resolve, reject ) =>
        {
            const abort = (): void =>
            {
                this.worker.postMessage (
                    { kind: "cancel", requestIdentifier } satisfies WorkerInboundMessage
                );
                this.pendingRequests.delete ( requestIdentifier );
                reject ( new TransportFailure ( "CANCELLED", "The operation was cancelled." ) );
            };

            signal?.addEventListener ( "abort", abort, { once: true } );
            this.pendingRequests.set (
                requestIdentifier,
                {
                    resolve,
                    reject,
                    removeAbortListener: () => signal?.removeEventListener ( "abort", abort ),
                }
            );
            this.worker.postMessage (
                {
                    kind: "request",
                    requestIdentifier,
                    request: authorizedRequest,
                } satisfies WorkerInboundMessage
            );
        } );
    }

    public async reset (): Promise <ReadyState>
    {
        this.rejectAllPending ( new TransportFailure ( "CANCELLED", "The built-in server was reset." ) );
        this.worker.terminate ();
        this.readyState   = null;
        this.stateValue   = "STARTING";
        this.worker       = this.createWorker ();
        this.readyPromise = this.createReadyPromise ();
        this.attachWorker ();

        return this.readyPromise;
    }

    public close (): void
    {
        if ( this.stateValue === "CLOSED" )
        {
            return;
        }

        this.stateValue = "CLOSED";
        this.worker.terminate ();
        this.rejectAllPending ( new TransportFailure ( "CANCELLED", "The transport was closed." ) );
    }

    private createWorker (): Worker
    {
        return new Worker ( new URL ( "./built-in-worker.ts", import.meta.url ), { type: "module" } );
    }

    private createReadyPromise (): Promise <ReadyState>
    {
        return new Promise <ReadyState> ( ( resolve, reject ) =>
        {
            this.resolveReady = resolve;
            this.rejectReady  = reject;
        } );
    }

    private attachWorker (): void
    {
        this.worker.onmessage = event => this.receiveMessage ( event.data as WorkerOutboundMessage );
        this.worker.onerror = event =>
        {
            const failure = new TransportFailure (
                "PROTOCOL",
                event.message || "The built-in server worker failed."
            );

            this.stateValue = "FAILED";
            this.rejectReady ( failure );
            this.rejectAllPending ( failure );
        };
    }

    private receiveMessage ( message: WorkerOutboundMessage ): void
    {
        if ( message.kind === "ready" )
        {
            this.readyState = {
                capability: message.capability,
                modelIdentifier: message.modelIdentifier,
                modelRevision: message.modelRevision,
            };
            this.stateValue = "READY";
            this.resolveReady ( this.readyState );
            return;
        }

        if ( message.kind === "failure" )
        {
            const failure = new TransportFailure ( "PROTOCOL", message.message );

            if ( message.requestIdentifier === undefined )
            {
                this.stateValue = "FAILED";
                this.rejectReady ( failure );
                this.rejectAllPending ( failure );
                return;
            }

            const pendingRequest = this.pendingRequests.get ( message.requestIdentifier );

            pendingRequest?.removeAbortListener ();
            this.pendingRequests.delete ( message.requestIdentifier );
            pendingRequest?.reject ( failure );
            return;
        }

        const pendingRequest = this.pendingRequests.get ( message.requestIdentifier );

        if ( pendingRequest === undefined )
        {
            return;
        }

        pendingRequest.removeAbortListener ();
        this.pendingRequests.delete ( message.requestIdentifier );
        pendingRequest.resolve ( message.response );
    }

    private rejectAllPending ( reason: unknown ): void
    {
        for ( const pendingRequest of this.pendingRequests.values () )
        {
            pendingRequest.removeAbortListener ();
            pendingRequest.reject ( reason );
        }

        this.pendingRequests.clear ();
    }
}
