// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    occurrence-json
// Version: 2.0.0
// Date:    2026-08-02
// Author:  Rohin Gosling
//
// Description:
//
//   Translates strict occurrence JSON while retaining payload omission, explicit null, and exact integer values.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import {
    exactJsonNumberToSignedLong,
    findJsonMember,
    hasJsonMember,
    parseLosslessJson,
    rejectUnknownJsonMembers,
    requireJsonObject,
    requireJsonString,
    serializeLosslessJson,
    type LosslessJsonMember,
    type LosslessJsonObject,
    type LosslessJsonValue,
} from "./lossless-json";
import type { AuthoringValue, ExactJsonNumber, OccurrenceDocument } from "./types";

const TOP_LEVEL_FIELDS = new Set ( [ "eventId", "payload" ] );

function scalarValue ( value: LosslessJsonValue, path: string ): AuthoringValue
{
    if ( value === null || typeof value === "string" || typeof value === "boolean" )
    {
        return value;
    }

    if ( value.kind === "exact-json-number" )
    {
        return exactJsonNumberToSignedLong ( value ) ?? value;
    }

    throw new Error ( `${path} must be a scalar value or null.` );
}

function scalarNode ( value: AuthoringValue ): LosslessJsonValue
{
    if ( value === null || typeof value === "string" || typeof value === "boolean" )
    {
        return value;
    }

    if ( typeof value === "bigint" )
    {
        return { kind: "exact-json-number", lexeme: value.toString () };
    }

    if ( typeof value === "object" && "kind" in value && value.kind === "exact-json-number" )
    {
        return value as ExactJsonNumber;
    }

    throw new Error ( "Occurrence payload values must be scalar values or null." );
}

function objectNode ( members: readonly LosslessJsonMember[] ): LosslessJsonObject
{
    return { kind: "object", members };
}

export class EventOccurrenceJsonCodec
{
    public read ( document: string ): OccurrenceDocument
    {
        const root = requireJsonObject ( parseLosslessJson ( document ), "$" );

        rejectUnknownJsonMembers ( root, TOP_LEVEL_FIELDS, "$" );

        const eventId = requireJsonString ( findJsonMember ( root, "eventId" ), "$.eventId" );

        if ( !hasJsonMember ( root, "payload" ) )
        {
            return { eventId, payload: new Map (), payloadPresent: false };
        }

        const payloadObject = requireJsonObject ( findJsonMember ( root, "payload" )!, "$.payload" );
        const payload = new Map (
            payloadObject.members.map (
                member => [ member.name, scalarValue ( member.value, `$.payload.${member.name}` ) ]
            )
        );

        return { eventId, payload, payloadPresent: true };
    }

    public write ( occurrence: OccurrenceDocument ): string
    {
        const members: LosslessJsonMember[] = [ { name: "eventId", value: occurrence.eventId } ];

        if ( occurrence.payloadPresent )
        {
            const payloadMembers = Array.from ( occurrence.payload.entries () )
                .sort ( ( first, second ) => first [ 0 ] < second [ 0 ] ? -1 : first [ 0 ] > second [ 0 ] ? 1 : 0 )
                .map ( ( [ name, value ] ) => ( { name, value: scalarNode ( value ) } ) );

            members.push ( { name: "payload", value: objectNode ( payloadMembers ) } );
        }

        return serializeLosslessJson ( objectNode ( members ), false );
    }
}
