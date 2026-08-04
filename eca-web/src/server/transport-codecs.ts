// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    transport-codecs
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Strictly decodes the small JSON response documents shared by built-in and real ECA server transports.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import {
    findJsonMember,
    parseLosslessJson,
    rejectUnknownJsonMembers,
    requireJsonObject,
    requireJsonString,
    type LosslessJsonObject,
    type LosslessJsonValue,
} from "../model/lossless-json";

export interface ServerReadiness
{
    readonly status: "READY" | "NOT_READY";
    readonly ready: boolean;
    readonly modelRevision?: string;
}

export interface ServerModelSummary
{
    readonly modelId: string;
    readonly modelRevision: string;
    readonly parameterCount: number;
    readonly payloadCount: number;
    readonly eventCount: number;
    readonly conditionCount: number;
    readonly conditionSetCount: number;
    readonly actionCount: number;
    readonly ruleCount: number;
}

export interface ServerEvaluation
{
    readonly outcome: "ACTION" | "NO_ACTION";
    readonly actionId?: string;
    readonly ruleId?: string;
    readonly specificity?: number;
    readonly modelRevision: string;
    readonly elapsedMicroseconds: number;
}

export interface ServerProblem
{
    readonly type: string;
    readonly title: string;
    readonly status: number;
    readonly detail: string;
    readonly instance: string;
}

function requiredMember ( object: LosslessJsonObject, name: string, path: string ): LosslessJsonValue
{
    const value = findJsonMember ( object, name );

    if ( value === undefined )
    {
        throw new Error ( `${path}.${name} is required.` );
    }

    return value;
}

function requireBoolean ( value: LosslessJsonValue, path: string ): boolean
{
    if ( typeof value !== "boolean" )
    {
        throw new Error ( `${path} must be a Boolean.` );
    }

    return value;
}

function requireNonnegativeInteger ( value: LosslessJsonValue, path: string ): number
{
    if (
        typeof value !== "object"
        || value === null
        || value.kind !== "exact-json-number"
        || !/^(?:0|[1-9][0-9]*)$/.test ( value.lexeme )
    )
    {
        throw new Error ( `${path} must be a nonnegative integer.` );
    }

    const parsedValue = Number ( value.lexeme );

    if ( !Number.isSafeInteger ( parsedValue ) )
    {
        throw new Error ( `${path} is outside the supported response range.` );
    }

    return parsedValue;
}

function readRoot ( document: string ): LosslessJsonObject
{
    return requireJsonObject ( parseLosslessJson ( document ), "$" );
}

export function readLiveness ( document: string ): "UP"
{
    const root = readRoot ( document );

    rejectUnknownJsonMembers ( root, new Set ( [ "status" ] ), "$" );

    if ( requireJsonString ( requiredMember ( root, "status", "$" ), "$.status" ) !== "UP" )
    {
        throw new Error ( "$.status must be UP." );
    }

    return "UP";
}

export function readReadiness ( document: string ): ServerReadiness
{
    const root = readRoot ( document );

    rejectUnknownJsonMembers ( root, new Set ( [ "status", "ready", "modelRevision" ] ), "$" );

    const status = requireJsonString ( requiredMember ( root, "status", "$" ), "$.status" );
    const ready = requireBoolean ( requiredMember ( root, "ready", "$" ), "$.ready" );

    if ( status !== "READY" && status !== "NOT_READY" )
    {
        throw new Error ( "$.status must be READY or NOT_READY." );
    }

    const revisionNode = findJsonMember ( root, "modelRevision" );

    if ( ready && revisionNode === undefined )
    {
        throw new Error ( "$.modelRevision is required when the server is ready." );
    }

    if ( !ready && revisionNode !== undefined )
    {
        throw new Error ( "$.modelRevision must be omitted when the server is not ready." );
    }

    return {
        status,
        ready,
        ...( revisionNode === undefined
            ? {}
            : { modelRevision: requireJsonString ( revisionNode, "$.modelRevision" ) } ),
    };
}

export function readModelSummary ( document: string ): ServerModelSummary
{
    const root = readRoot ( document );
    const fields = new Set (
        [
            "modelId",
            "modelRevision",
            "parameterCount",
            "payloadCount",
            "eventCount",
            "conditionCount",
            "conditionSetCount",
            "actionCount",
            "ruleCount",
        ]
    );

    rejectUnknownJsonMembers ( root, fields, "$" );

    return {
        modelId: requireJsonString ( requiredMember ( root, "modelId", "$" ), "$.modelId" ),
        modelRevision: requireJsonString (
            requiredMember ( root, "modelRevision", "$" ),
            "$.modelRevision"
        ),
        parameterCount: requireNonnegativeInteger (
            requiredMember ( root, "parameterCount", "$" ),
            "$.parameterCount"
        ),
        payloadCount: requireNonnegativeInteger (
            requiredMember ( root, "payloadCount", "$" ),
            "$.payloadCount"
        ),
        eventCount: requireNonnegativeInteger (
            requiredMember ( root, "eventCount", "$" ),
            "$.eventCount"
        ),
        conditionCount: requireNonnegativeInteger (
            requiredMember ( root, "conditionCount", "$" ),
            "$.conditionCount"
        ),
        conditionSetCount: requireNonnegativeInteger (
            requiredMember ( root, "conditionSetCount", "$" ),
            "$.conditionSetCount"
        ),
        actionCount: requireNonnegativeInteger (
            requiredMember ( root, "actionCount", "$" ),
            "$.actionCount"
        ),
        ruleCount: requireNonnegativeInteger (
            requiredMember ( root, "ruleCount", "$" ),
            "$.ruleCount"
        ),
    };
}

export function readEvaluation ( document: string ): ServerEvaluation
{
    const root = readRoot ( document );
    const fields = new Set (
        [
            "outcome",
            "actionId",
            "ruleId",
            "specificity",
            "modelRevision",
            "elapsedMicroseconds",
        ]
    );

    rejectUnknownJsonMembers ( root, fields, "$" );

    const outcome = requireJsonString ( requiredMember ( root, "outcome", "$" ), "$.outcome" );
    const modelRevision = requireJsonString (
        requiredMember ( root, "modelRevision", "$" ),
        "$.modelRevision"
    );
    const elapsedMicroseconds = requireNonnegativeInteger (
        requiredMember ( root, "elapsedMicroseconds", "$" ),
        "$.elapsedMicroseconds"
    );

    if ( outcome === "NO_ACTION" )
    {
        if (
            findJsonMember ( root, "actionId" ) !== undefined
            || findJsonMember ( root, "ruleId" ) !== undefined
            || findJsonMember ( root, "specificity" ) !== undefined
        )
        {
            throw new Error ( "NO_ACTION must omit actionId, ruleId, and specificity." );
        }

        return { outcome, modelRevision, elapsedMicroseconds };
    }

    if ( outcome !== "ACTION" )
    {
        throw new Error ( "$.outcome must be ACTION or NO_ACTION." );
    }

    return {
        outcome,
        actionId: requireJsonString ( requiredMember ( root, "actionId", "$" ), "$.actionId" ),
        ruleId: requireJsonString ( requiredMember ( root, "ruleId", "$" ), "$.ruleId" ),
        specificity: requireNonnegativeInteger (
            requiredMember ( root, "specificity", "$" ),
            "$.specificity"
        ),
        modelRevision,
        elapsedMicroseconds,
    };
}

export function readProblem ( document: string ): ServerProblem
{
    const root = readRoot ( document );

    rejectUnknownJsonMembers (
        root,
        new Set ( [ "type", "title", "status", "detail", "instance" ] ),
        "$"
    );

    return {
        type: requireJsonString ( requiredMember ( root, "type", "$" ), "$.type" ),
        title: requireJsonString ( requiredMember ( root, "title", "$" ), "$.title" ),
        status: requireNonnegativeInteger ( requiredMember ( root, "status", "$" ), "$.status" ),
        detail: requireJsonString ( requiredMember ( root, "detail", "$" ), "$.detail" ),
        instance: requireJsonString ( requiredMember ( root, "instance", "$" ), "$.instance" ),
    };
}
