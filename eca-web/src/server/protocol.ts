// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    protocol
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Defines the HTTP-shaped web transport and dedicated-worker message contracts.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

export type HTTPMethod = "GET" | "POST" | "PUT";

export interface TransportRequest
{
    readonly method: HTTPMethod;
    readonly path: string;
    readonly headers?: Readonly <Record <string, string>>;
    readonly body?: string;
}

export interface TransportResponse
{
    readonly status: number;
    readonly headers: Readonly <Record <string, string>>;
    readonly body: string;
}

export interface ServerTransport
{
    request ( request: TransportRequest, signal?: AbortSignal ): Promise <TransportResponse>;
}

export type TransportFailureCategory =
    | "CANCELLED"
    | "CONNECTION_TIMEOUT"
    | "REQUEST_TIMEOUT"
    | "PERMISSION_OR_UNREACHABLE"
    | "PROTOCOL";

export class TransportFailure extends Error
{
    public readonly category: TransportFailureCategory;

    public constructor ( category: TransportFailureCategory, message: string, cause?: unknown )
    {
        super ( message, cause === undefined ? undefined : { cause } );

        this.name     = "TransportFailure";
        this.category = category;
    }
}

export interface WorkerRequestMessage
{
    readonly kind: "request";
    readonly requestIdentifier: string;
    readonly request: TransportRequest;
}

export interface WorkerCancellationMessage
{
    readonly kind: "cancel";
    readonly requestIdentifier: string;
}

export type WorkerInboundMessage = WorkerRequestMessage | WorkerCancellationMessage;

export interface WorkerReadyMessage
{
    readonly kind: "ready";
    readonly capability: string;
    readonly modelIdentifier: string;
    readonly modelRevision: string;
}

export interface WorkerResponseMessage
{
    readonly kind: "response";
    readonly requestIdentifier: string;
    readonly response: TransportResponse;
}

export interface WorkerFailureMessage
{
    readonly kind: "failure";
    readonly requestIdentifier?: string;
    readonly message: string;
}

export type WorkerOutboundMessage = WorkerReadyMessage | WorkerResponseMessage | WorkerFailureMessage;

export function requestHeader ( request: TransportRequest, name: string ): string | undefined
{
    const normalizedName = name.toLowerCase ();

    return Object.entries ( request.headers ?? {} ).find (
        ( [ headerName ] ) => headerName.toLowerCase () === normalizedName
    )?.[ 1 ];
}

export function responseHeader ( response: TransportResponse, name: string ): string | undefined
{
    const normalizedName = name.toLowerCase ();

    return Object.entries ( response.headers ).find (
        ( [ headerName ] ) => headerName.toLowerCase () === normalizedName
    )?.[ 1 ];
}
