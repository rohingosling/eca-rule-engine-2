// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    server-gateway
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Provides one strict client gateway over the built-in worker and optional real-server transports.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import {
    AuthoringModelJsonCodec,
    EventOccurrenceJsonCodec,
    type AuthoringModel,
    type OccurrenceDocument,
} from "../model";
import {
    EVALUATION_PATH,
    LIVENESS_PATH,
    MODEL_PATH,
    READINESS_PATH,
} from "./built-in-server";
import {
    responseHeader,
    TransportFailure,
    type ServerTransport,
    type TransportResponse,
} from "./protocol";
import {
    readEvaluation,
    readLiveness,
    readModelSummary,
    readProblem,
    readReadiness,
    type ServerEvaluation,
    type ServerModelSummary,
    type ServerReadiness,
} from "./transport-codecs";

export type GatewayFailureCategory =
    | "CANCELLED"
    | "CONNECTION"
    | "TIMEOUT"
    | "AUTHENTICATION"
    | "NO_MODEL"
    | "VALIDATION"
    | "PROTOCOL"
    | "SERVER";

export class GatewayFailure extends Error
{
    public readonly category: GatewayFailureCategory;
    public readonly status?: number;

    public constructor (
        category: GatewayFailureCategory,
        message: string,
        status?: number,
        cause?: unknown
    )
    {
        super ( message, cause === undefined ? undefined : { cause } );

        this.name     = "GatewayFailure";
        this.category = category;
        this.status   = status;
    }
}

export interface ConnectionTestResult
{
    readonly liveness: "UP";
    readonly readiness: ServerReadiness;
    readonly roundTripMicroseconds: number;
}

export interface PulledModel
{
    readonly model: AuthoringModel;
    readonly modelRevision: string;
    readonly roundTripMicroseconds: number;
}

export interface PushedModel
{
    readonly summary: ServerModelSummary;
    readonly roundTripMicroseconds: number;
}

export interface EvaluatedOccurrence extends ServerEvaluation
{
    readonly roundTripMicroseconds: number;
}

function roundTripMicroseconds ( startMilliseconds: number ): number
{
    return Math.max ( 0, Math.floor ( ( performance.now () - startMilliseconds ) * 1000 ) );
}

function normalizeEntityTag ( value: string | undefined ): string | undefined
{
    if ( value === undefined )
    {
        return undefined;
    }

    const normalized = value.trim ();

    return normalized.startsWith ( "\"" ) && normalized.endsWith ( "\"" )
        ? normalized.slice ( 1, -1 )
        : normalized;
}

function classifyStatus ( status: number ): GatewayFailureCategory
{
    if ( status === 401 || status === 403 )
    {
        return "AUTHENTICATION";
    }

    if ( status === 404 )
    {
        return "NO_MODEL";
    }

    if ( status === 400 || status === 413 || status === 415 || status === 422 )
    {
        return "VALIDATION";
    }

    if ( status >= 500 )
    {
        return "SERVER";
    }

    return "PROTOCOL";
}

function translateTransportFailure ( error: unknown ): GatewayFailure
{
    if ( error instanceof GatewayFailure )
    {
        return error;
    }

    if ( error instanceof TransportFailure )
    {
        switch ( error.category )
        {
            case "CANCELLED":
                return new GatewayFailure ( "CANCELLED", error.message, undefined, error );

            case "CONNECTION_TIMEOUT":
            case "REQUEST_TIMEOUT":
                return new GatewayFailure ( "TIMEOUT", error.message, undefined, error );

            case "PERMISSION_OR_UNREACHABLE":
                return new GatewayFailure ( "CONNECTION", error.message, undefined, error );

            case "PROTOCOL":
                return new GatewayFailure ( "PROTOCOL", error.message, undefined, error );
        }
    }

    return new GatewayFailure (
        "PROTOCOL",
        error instanceof Error ? error.message : String ( error ),
        undefined,
        error
    );
}

function requireJSONResponse ( response: TransportResponse ): void
{
    const mediaType = responseHeader ( response, "Content-Type" )?.split ( ";", 1 ) [ 0 ]?.trim ().toLowerCase ();

    if ( mediaType !== "application/json" )
    {
        throw new GatewayFailure (
            "PROTOCOL",
            `The server returned unsupported media type ${mediaType ?? "(missing)"}.`,
            response.status
        );
    }
}

function requireSuccess ( response: TransportResponse, expectedStatuses: readonly number[] ): void
{
    if ( expectedStatuses.includes ( response.status ) )
    {
        requireJSONResponse ( response );
        return;
    }

    let detail = `The server returned HTTP ${response.status}.`;

    try
    {
        const problem = readProblem ( response.body );

        if ( problem.status !== response.status )
        {
            throw new Error ( "The problem status does not match the HTTP status." );
        }

        detail = `${problem.title}: ${problem.detail}`;
    }
    catch ( error )
    {
        if ( response.status < 400 )
        {
            throw new GatewayFailure ( "PROTOCOL", "The server response is invalid.", response.status, error );
        }
    }

    throw new GatewayFailure ( classifyStatus ( response.status ), detail, response.status );
}

export class ServerGateway
{
    private readonly transport: ServerTransport;
    private readonly modelCodec: AuthoringModelJsonCodec;
    private readonly occurrenceCodec: EventOccurrenceJsonCodec;

    public constructor ( transport: ServerTransport )
    {
        this.transport       = transport;
        this.modelCodec      = new AuthoringModelJsonCodec ();
        this.occurrenceCodec = new EventOccurrenceJsonCodec ();
    }

    public async testConnection ( signal?: AbortSignal ): Promise <ConnectionTestResult>
    {
        const startMilliseconds = performance.now ();

        try
        {
            const livenessResponse = await this.transport.request (
                { method: "GET", path: LIVENESS_PATH },
                signal
            );

            requireSuccess ( livenessResponse, [ 200 ] );

            const readinessResponse = await this.transport.request (
                { method: "GET", path: READINESS_PATH },
                signal
            );

            requireSuccess ( readinessResponse, [ 200, 503 ] );

            return {
                liveness: readLiveness ( livenessResponse.body ),
                readiness: readReadiness ( readinessResponse.body ),
                roundTripMicroseconds: roundTripMicroseconds ( startMilliseconds ),
            };
        }
        catch ( error )
        {
            throw translateTransportFailure ( error );
        }
    }

    public async pullModel ( signal?: AbortSignal ): Promise <PulledModel>
    {
        const startMilliseconds = performance.now ();

        try
        {
            const response = await this.transport.request ( { method: "GET", path: MODEL_PATH }, signal );

            requireSuccess ( response, [ 200 ] );

            const model = this.modelCodec.read ( response.body );
            const calculatedRevision = await this.modelCodec.revision ( model );
            const responseRevision = responseHeader ( response, "X-Model-Revision" )
                ?? normalizeEntityTag ( responseHeader ( response, "ETag" ) );

            if ( responseRevision === undefined || responseRevision !== calculatedRevision )
            {
                throw new GatewayFailure (
                    "PROTOCOL",
                    "The pulled model revision does not match its canonical content."
                );
            }

            return {
                model,
                modelRevision: responseRevision,
                roundTripMicroseconds: roundTripMicroseconds ( startMilliseconds ),
            };
        }
        catch ( error )
        {
            throw translateTransportFailure ( error );
        }
    }

    public async pushModel ( model: AuthoringModel, signal?: AbortSignal ): Promise <PushedModel>
    {
        const startMilliseconds = performance.now ();

        try
        {
            const response = await this.transport.request (
                {
                    method: "PUT",
                    path: MODEL_PATH,
                    headers: { "Content-Type": "application/json" },
                    body: this.modelCodec.writeCanonicalText ( model ),
                },
                signal
            );

            requireSuccess ( response, [ 200, 201 ] );

            const summary = readModelSummary ( response.body );
            const responseRevision = responseHeader ( response, "X-Model-Revision" )
                ?? normalizeEntityTag ( responseHeader ( response, "ETag" ) );

            if ( responseRevision !== summary.modelRevision )
            {
                throw new GatewayFailure ( "PROTOCOL", "The Push revision headers and body disagree." );
            }

            return {
                summary,
                roundTripMicroseconds: roundTripMicroseconds ( startMilliseconds ),
            };
        }
        catch ( error )
        {
            throw translateTransportFailure ( error );
        }
    }

    public async evaluate (
        occurrence: OccurrenceDocument,
        signal?: AbortSignal
    ): Promise <EvaluatedOccurrence>
    {
        const startMilliseconds = performance.now ();

        try
        {
            const response = await this.transport.request (
                {
                    method: "POST",
                    path: EVALUATION_PATH,
                    headers: { "Content-Type": "application/json" },
                    body: this.occurrenceCodec.write ( occurrence ),
                },
                signal
            );

            requireSuccess ( response, [ 200 ] );

            return {
                ...readEvaluation ( response.body ),
                roundTripMicroseconds: roundTripMicroseconds ( startMilliseconds ),
            };
        }
        catch ( error )
        {
            throw translateTransportFailure ( error );
        }
    }
}

export function gatewayFailureMessage ( error: unknown ): string
{
    const failure = translateTransportFailure ( error );

    switch ( failure.category )
    {
        case "CANCELLED":
            return "Operation cancelled.";

        case "CONNECTION":
            return `${failure.message} Check browser local-network permission, CORS, the URL, and server availability.`;

        case "TIMEOUT":
            return `${failure.message} Increase the applicable timeout or check server responsiveness.`;

        case "AUTHENTICATION":
            return `${failure.message} Check the session-only bearer token.`;

        case "NO_MODEL":
            return `${failure.message} Push a valid model or switch back to the built-in demo server.`;

        case "VALIDATION":
        case "PROTOCOL":
        case "SERVER":
            return failure.message;
    }
}
