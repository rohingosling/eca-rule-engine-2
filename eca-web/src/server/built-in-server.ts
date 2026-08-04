// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    built-in-server
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Implements the ephemeral HTTP-shaped browser-local server over immutable compiled model snapshots.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import {
    AuthoringModelCompiler,
    AuthoringModelJsonCodec,
    EventOccurrenceJsonCodec,
    RuleEvaluator,
    compileOccurrence,
    validateAuthoringModel,
    type AuthoringModel,
    type CompiledModel,
} from "../model";
import {
    requestHeader,
    type TransportRequest,
    type TransportResponse,
} from "./protocol";

export const LIVENESS_PATH  = "/api/v1/health/live";
export const READINESS_PATH = "/api/v1/health/ready";
export const MODEL_PATH     = "/api/v1/model";
export const EVALUATION_PATH = "/api/v1/evaluations";
export const STOP_PATH       = "/api/v1/management/stop";

const JSON_MEDIA_TYPE         = "application/json";
const PROBLEM_JSON_MEDIA_TYPE = "application/problem+json";
const MAXIMUM_MODEL_BODY_BYTES      = 16 * 1024 * 1024;
const MAXIMUM_EVALUATION_BODY_BYTES = 1024 * 1024;

interface ActiveModelSnapshot
{
    readonly model: AuthoringModel;
    readonly compiledModel: CompiledModel;
    readonly evaluator: RuleEvaluator;
    readonly canonicalDocument: string;
    readonly revision: string;
}

class ModelValidationFailure extends Error
{
    public constructor ( message: string )
    {
        super ( message );

        this.name = "ModelValidationFailure";
    }
}

function jsonResponse (
    status: number,
    body: object,
    headers: Readonly <Record <string, string>> = {}
): TransportResponse
{
    return {
        status,
        headers: { "Content-Type": JSON_MEDIA_TYPE, ...headers },
        body: JSON.stringify ( body ),
    };
}

function canonicalModelResponse ( snapshot: ActiveModelSnapshot ): TransportResponse
{
    return {
        status: 200,
        headers:
        {
            "Content-Type": JSON_MEDIA_TYPE,
            "ETag": `"${snapshot.revision}"`,
            "X-Model-Revision": snapshot.revision,
        },
        body: snapshot.canonicalDocument,
    };
}

function problemResponse (
    status: number,
    title: string,
    detail: string,
    instance: string,
    headers: Readonly <Record <string, string>> = {}
): TransportResponse
{
    return {
        status,
        headers: { "Content-Type": PROBLEM_JSON_MEDIA_TYPE, ...headers },
        body: JSON.stringify (
            {
                type: "about:blank",
                title,
                status,
                detail,
                instance,
            }
        ),
    };
}

function mediaTypeIsJSON ( request: TransportRequest ): boolean
{
    return requestHeader ( request, "Content-Type" )?.split ( ";", 1 ) [ 0 ]?.trim ().toLowerCase ()
        === JSON_MEDIA_TYPE;
}

function bodySize ( request: TransportRequest ): number
{
    return new TextEncoder ().encode ( request.body ?? "" ).byteLength;
}

function modelSummary ( snapshot: ActiveModelSnapshot ): object
{
    const model = snapshot.model;

    return {
        modelId: model.modelId,
        modelRevision: snapshot.revision,
        parameterCount: model.parameters.length,
        payloadCount: model.payloads.length,
        eventCount: model.events.length,
        conditionCount: model.conditions.length,
        conditionSetCount: model.conditionSets.length,
        actionCount: model.actions.length,
        ruleCount: model.rules.length,
    };
}

export class BuiltInServer
{
    private readonly capability: string;
    private readonly modelCodec: AuthoringModelJsonCodec;
    private readonly occurrenceCodec: EventOccurrenceJsonCodec;
    private activeSnapshot: ActiveModelSnapshot;
    private running: boolean;
    private replacementQueue: Promise <void>;

    private constructor ( capability: string, snapshot: ActiveModelSnapshot )
    {
        this.capability       = capability;
        this.modelCodec       = new AuthoringModelJsonCodec ();
        this.occurrenceCodec  = new EventOccurrenceJsonCodec ();
        this.activeSnapshot   = snapshot;
        this.running          = true;
        this.replacementQueue = Promise.resolve ();
    }

    public static async create ( exampleDocument: string, capability: string ): Promise <BuiltInServer>
    {
        const modelCodec = new AuthoringModelJsonCodec ();
        const snapshot = await BuiltInServer.prepareSnapshot ( modelCodec, exampleDocument );

        return new BuiltInServer ( capability, snapshot );
    }

    public get modelIdentifier (): string
    {
        return this.activeSnapshot.model.modelId;
    }

    public get modelRevision (): string
    {
        return this.activeSnapshot.revision;
    }

    public async handle ( request: TransportRequest ): Promise <TransportResponse>
    {
        if ( !this.running )
        {
            return problemResponse (
                503,
                "Server Stopped",
                "The built-in server is not accepting requests.",
                request.path
            );
        }

        if ( request.method === "GET" && request.path === LIVENESS_PATH )
        {
            return jsonResponse ( 200, { status: "UP" } );
        }

        if ( request.method === "GET" && request.path === READINESS_PATH )
        {
            return jsonResponse (
                200,
                {
                    status: "READY",
                    ready: true,
                    modelRevision: this.activeSnapshot.revision,
                }
            );
        }

        if ( request.method === "GET" && request.path === MODEL_PATH )
        {
            return canonicalModelResponse ( this.activeSnapshot );
        }

        if ( request.method === "PUT" && request.path === MODEL_PATH )
        {
            return this.enqueueReplacement ( request );
        }

        if ( request.method === "POST" && request.path === EVALUATION_PATH )
        {
            return this.evaluate ( request );
        }

        if ( request.method === "POST" && request.path === STOP_PATH )
        {
            if ( !this.authenticated ( request ) )
            {
                return this.unauthorized ( request.path );
            }

            this.running = false;

            return jsonResponse ( 202, { status: "STOPPING" } );
        }

        return problemResponse (
            404,
            "Not Found",
            "No API route matches this request.",
            request.path
        );
    }

    private static async prepareSnapshot (
        modelCodec: AuthoringModelJsonCodec,
        document: string
    ): Promise <ActiveModelSnapshot>
    {
        const model = modelCodec.read ( document );
        const diagnostics = validateAuthoringModel ( model );

        if ( diagnostics.length > 0 )
        {
            const firstDiagnostic = diagnostics [ 0 ];
            const firstDetail = firstDiagnostic === undefined
                ? "The model is invalid."
                : `${firstDiagnostic.entityId}.${firstDiagnostic.field}: ${firstDiagnostic.remedy}`;

            throw new ModelValidationFailure (
                `${diagnostics.length} validation error(s). ${firstDetail}`
            );
        }

        const revision = await modelCodec.revision ( model );
        const compiledModel = new AuthoringModelCompiler ().compile ( model, revision );

        return Object.freeze (
            {
                model,
                compiledModel,
                evaluator: new RuleEvaluator ( compiledModel ),
                canonicalDocument: modelCodec.writeCanonicalText ( model ),
                revision,
            }
        );
    }

    private authenticated ( request: TransportRequest ): boolean
    {
        return requestHeader ( request, "Authorization" ) === `Bearer ${this.capability}`;
    }

    private unauthorized ( path: string ): TransportResponse
    {
        return problemResponse (
            401,
            "Unauthorized",
            "Valid bearer credentials are required.",
            path,
            { "WWW-Authenticate": "Bearer" }
        );
    }

    private enqueueReplacement ( request: TransportRequest ): Promise <TransportResponse>
    {
        const replacement = this.replacementQueue.then ( () => this.replaceModel ( request ) );

        this.replacementQueue = replacement.then ( () => undefined, () => undefined );

        return replacement;
    }

    private async replaceModel ( request: TransportRequest ): Promise <TransportResponse>
    {
        if ( !this.authenticated ( request ) )
        {
            return this.unauthorized ( request.path );
        }

        if ( !mediaTypeIsJSON ( request ) )
        {
            return problemResponse (
                415,
                "Unsupported Media Type",
                "Content-Type must be application/json.",
                request.path
            );
        }

        if ( bodySize ( request ) > MAXIMUM_MODEL_BODY_BYTES )
        {
            return problemResponse (
                413,
                "Payload Too Large",
                `The request body exceeds ${MAXIMUM_MODEL_BODY_BYTES} bytes.`,
                request.path
            );
        }

        try
        {
            const candidateSnapshot = await BuiltInServer.prepareSnapshot (
                this.modelCodec,
                request.body ?? ""
            );

            this.activeSnapshot = candidateSnapshot;

            return jsonResponse (
                200,
                modelSummary ( candidateSnapshot ),
                {
                    "ETag": `"${candidateSnapshot.revision}"`,
                    "X-Model-Revision": candidateSnapshot.revision,
                }
            );
        }
        catch ( error )
        {
            if ( error instanceof ModelValidationFailure )
            {
                return problemResponse (
                    422,
                    "Model Validation Failed",
                    error.message,
                    request.path
                );
            }

            const detail = error instanceof Error ? error.message : "The model document is invalid.";

            return problemResponse ( 400, "Malformed JSON", detail, request.path );
        }
    }

    private evaluate ( request: TransportRequest ): TransportResponse
    {
        if ( !mediaTypeIsJSON ( request ) )
        {
            return problemResponse (
                415,
                "Unsupported Media Type",
                "Content-Type must be application/json.",
                request.path
            );
        }

        if ( bodySize ( request ) > MAXIMUM_EVALUATION_BODY_BYTES )
        {
            return problemResponse (
                413,
                "Payload Too Large",
                `The request body exceeds ${MAXIMUM_EVALUATION_BODY_BYTES} bytes.`,
                request.path
            );
        }

        const snapshot = this.activeSnapshot;

        try
        {
            const occurrenceDocument = this.occurrenceCodec.read ( request.body ?? "" );
            const occurrence = compileOccurrence ( snapshot.compiledModel, occurrenceDocument );
            const startMilliseconds = performance.now ();
            const result = snapshot.evaluator.evaluate ( occurrence );
            const elapsedMicroseconds = Math.max (
                0,
                Math.floor ( ( performance.now () - startMilliseconds ) * 1000 )
            );

            return jsonResponse ( 200, { ...result, elapsedMicroseconds } );
        }
        catch ( error )
        {
            const detail = error instanceof Error ? error.message : "The occurrence document is invalid.";
            const malformed = /JSON|member|object|string|number|token|character|end of input/i.test ( detail );

            return problemResponse (
                malformed ? 400 : 422,
                malformed ? "Malformed JSON" : "Invalid Occurrence",
                detail,
                request.path
            );
        }
    }
}
