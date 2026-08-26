// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    authoring-model-json
// Version: 2.0.0
// Date:    2026-08-02
// Author:  Rohin Gosling
//
// Description:
//
//   Implements strict authoring-model decoding, stable pretty JSON, canonical JSON, and Web Crypto revisions.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import {
    exactJsonNumberToSignedLong,
    findJsonMember,
    hasJsonMember,
    isExactJsonNumber,
    parseLosslessJson,
    rejectUnknownJsonMembers,
    requireJsonArray,
    requireJsonObject,
    requireJsonString,
    serializeLosslessJson,
    type LosslessJsonArray,
    type LosslessJsonMember,
    type LosslessJsonObject,
    type LosslessJsonValue,
} from "./lossless-json";
import type {
    ActionDefinition,
    AuthoringModel,
    AuthoringValue,
    ConditionDefinition,
    ConditionSetDefinition,
    EntityDefinition,
    EventDefinition,
    ExactJsonNumber,
    ParameterDefinition,
    PayloadDefinition,
    RuleDefinition,
} from "./types";

const TOP_LEVEL_FIELDS = new Set (
    [
        "schemaVersion",
        "modelId",
        "name",
        "description",
        "parameters",
        "payloads",
        "events",
        "conditions",
        "conditionSets",
        "actions",
        "rules",
    ]
);

const PARAMETER_FIELDS = new Set ( [ "id", "name", "description", "type", "enumValues" ] );
const PAYLOAD_FIELDS = new Set ( [ "id", "name", "description", "parameterIds" ] );
const EVENT_FIELDS = new Set ( [ "id", "name", "description", "payloadId" ] );
const CONDITION_FIELDS = new Set (
    [ "id", "name", "description", "parameterId", "operator", "value", "secondValue" ]
);
const CONDITION_SET_FIELDS = new Set (
    [ "id", "name", "description", "bindings", "conditionIds" ]
);
const ACTION_FIELDS = new Set ( [ "id", "name", "description" ] );
const RULE_FIELDS = new Set (
    [ "id", "name", "description", "eventId", "conditionSetId", "actionId" ]
);

function requireMember ( object: LosslessJsonObject, name: string, path: string ): LosslessJsonValue
{
    const value = findJsonMember ( object, name );

    if ( value === undefined )
    {
        throw new Error ( `${path}.${name} is required.` );
    }

    return value;
}

function readEntityFields ( object: LosslessJsonObject, path: string ): EntityDefinition
{
    return {
        id: requireJsonString ( requireMember ( object, "id", path ), `${path}.id` ),
        name: requireJsonString ( requireMember ( object, "name", path ), `${path}.name` ),
        description: requireJsonString (
            requireMember ( object, "description", path ),
            `${path}.description`
        ),
    };
}

function readTextArray ( value: LosslessJsonValue, path: string ): readonly string[]
{
    const array = requireJsonArray ( value, path );

    return array.values.map (
        ( item, index ) => requireJsonString ( item, `${path}[${index}]` )
    );
}

function readAuthoringValue ( value: LosslessJsonValue ): AuthoringValue
{
    if ( value === null || typeof value === "string" || typeof value === "boolean" )
    {
        return value;
    }

    if ( value.kind === "exact-json-number" )
    {
        return exactJsonNumberToSignedLong ( value ) ?? value;
    }

    if ( value.kind === "array" )
    {
        return value.values.map ( readAuthoringValue );
    }

    return new Map (
        value.members.map ( member => [ member.name, readAuthoringValue ( member.value ) ] )
    );
}

function readParameters ( array: LosslessJsonArray ): readonly ParameterDefinition[]
{
    return array.values.map (
        ( value, index ) =>
        {
            const path = `$.parameters[${index}]`;
            const object = requireJsonObject ( value, path );
            const entity = readEntityFields ( object, path );
            const enumerationValues = hasJsonMember ( object, "enumValues" )
                ? readTextArray ( requireMember ( object, "enumValues", path ), `${path}.enumValues` )
                : [];

            rejectUnknownJsonMembers ( object, PARAMETER_FIELDS, path );

            return {
                ...entity,
                type: requireJsonString ( requireMember ( object, "type", path ), `${path}.type` ),
                enumerationValues,
            };
        }
    );
}

function readPayloads ( array: LosslessJsonArray ): readonly PayloadDefinition[]
{
    return array.values.map (
        ( value, index ) =>
        {
            const path = `$.payloads[${index}]`;
            const object = requireJsonObject ( value, path );

            rejectUnknownJsonMembers ( object, PAYLOAD_FIELDS, path );

            return {
                ...readEntityFields ( object, path ),
                parameterIds: readTextArray (
                    requireMember ( object, "parameterIds", path ),
                    `${path}.parameterIds`
                ),
            };
        }
    );
}

function readEvents ( array: LosslessJsonArray ): readonly EventDefinition[]
{
    return array.values.map (
        ( value, index ) =>
        {
            const path = `$.events[${index}]`;
            const object = requireJsonObject ( value, path );

            rejectUnknownJsonMembers ( object, EVENT_FIELDS, path );

            return {
                ...readEntityFields ( object, path ),
                payloadId: requireJsonString (
                    requireMember ( object, "payloadId", path ),
                    `${path}.payloadId`
                ),
            };
        }
    );
}

function conditionOperandCount ( operator: string ): number
{
    switch ( operator )
    {
        case "ANY":
            return 0;

        case "BETWEEN_EXCLUSIVE":
        case "BETWEEN_INCLUSIVE":
            return 2;

        case "EQUALS":
        case "NOT_EQUALS":
        case "GREATER_THAN":
        case "GREATER_THAN_OR_EQUAL":
        case "LESS_THAN":
        case "LESS_THAN_OR_EQUAL":
            return 1;

        default:
            throw new Error ( `Unknown condition operator: ${operator}` );
    }
}

function readConditions ( array: LosslessJsonArray ): readonly ConditionDefinition[]
{
    return array.values.map (
        ( value, index ) =>
        {
            const path = `$.conditions[${index}]`;
            const object = requireJsonObject ( value, path );
            const entity = readEntityFields ( object, path );
            const parameterId = requireJsonString (
                requireMember ( object, "parameterId", path ),
                `${path}.parameterId`
            );

            rejectUnknownJsonMembers ( object, CONDITION_FIELDS, path );

            if ( !hasJsonMember ( object, "operator" ) )
            {
                if ( hasJsonMember ( object, "value" ) || hasJsonMember ( object, "secondValue" ) )
                {
                    throw new Error ( `${path} must define operator before defining condition operands.` );
                }

                return { ...entity, parameterId };
            }

            const operator = requireJsonString (
                requireMember ( object, "operator", path ),
                `${path}.operator`
            );
            const operandCount = conditionOperandCount ( operator );

            if ( operandCount === 0 && ( hasJsonMember ( object, "value" ) || hasJsonMember ( object, "secondValue" ) ) )
            {
                throw new Error ( `${path} must not define operands for operator ${operator}.` );
            }

            if ( operandCount === 1 && hasJsonMember ( object, "secondValue" ) )
            {
                throw new Error ( `${path} must not define secondValue for operator ${operator}.` );
            }

            const condition: ConditionDefinition = { ...entity, parameterId, operator };

            if ( hasJsonMember ( object, "value" ) )
            {
                ( condition as { value?: AuthoringValue } ).value = readAuthoringValue (
                    requireMember ( object, "value", path )
                );
            }

            if ( hasJsonMember ( object, "secondValue" ) )
            {
                ( condition as { secondValue?: AuthoringValue } ).secondValue = readAuthoringValue (
                    requireMember ( object, "secondValue", path )
                );
            }

            return condition;
        }
    );
}

function readValueMap ( object: LosslessJsonObject ): ReadonlyMap <string, AuthoringValue>
{
    return new Map (
        object.members.map ( member => [ member.name, readAuthoringValue ( member.value ) ] )
    );
}

function readConditionSets ( array: LosslessJsonArray ): readonly ConditionSetDefinition[]
{
    return array.values.map (
        ( value, index ) =>
        {
            const path = `$.conditionSets[${index}]`;
            const object = requireJsonObject ( value, path );
            const entity = readEntityFields ( object, path );

            rejectUnknownJsonMembers ( object, CONDITION_SET_FIELDS, path );

            if ( hasJsonMember ( object, "conditionIds" ) )
            {
                if ( hasJsonMember ( object, "bindings" ) )
                {
                    throw new Error ( `${path} must use conditionIds or legacy bindings, not both.` );
                }

                return {
                    ...entity,
                    kind: "predefined",
                    conditionIds: readTextArray (
                        requireMember ( object, "conditionIds", path ),
                        `${path}.conditionIds`
                    ),
                };
            }

            return {
                ...entity,
                kind: "legacy",
                bindings: readValueMap (
                    requireJsonObject (
                        requireMember ( object, "bindings", path ),
                        `${path}.bindings`
                    )
                ),
            };
        }
    );
}

function readActions ( array: LosslessJsonArray ): readonly ActionDefinition[]
{
    return array.values.map (
        ( value, index ) =>
        {
            const path = `$.actions[${index}]`;
            const object = requireJsonObject ( value, path );

            rejectUnknownJsonMembers ( object, ACTION_FIELDS, path );

            return readEntityFields ( object, path );
        }
    );
}

function readRules ( array: LosslessJsonArray ): readonly RuleDefinition[]
{
    return array.values.map (
        ( value, index ) =>
        {
            const path = `$.rules[${index}]`;
            const object = requireJsonObject ( value, path );

            rejectUnknownJsonMembers ( object, RULE_FIELDS, path );

            return {
                ...readEntityFields ( object, path ),
                eventId: requireJsonString (
                    requireMember ( object, "eventId", path ),
                    `${path}.eventId`
                ),
                conditionSetId: requireJsonString (
                    requireMember ( object, "conditionSetId", path ),
                    `${path}.conditionSetId`
                ),
                actionId: requireJsonString (
                    requireMember ( object, "actionId", path ),
                    `${path}.actionId`
                ),
            };
        }
    );
}

function numberNode ( value: bigint ): ExactJsonNumber
{
    return { kind: "exact-json-number", lexeme: value.toString () };
}

function authoringValueNode ( value: AuthoringValue ): LosslessJsonValue
{
    if ( value === null || typeof value === "string" || typeof value === "boolean" )
    {
        return value;
    }

    if ( typeof value === "bigint" )
    {
        return numberNode ( value );
    }

    if ( Array.isArray ( value ) )
    {
        return { kind: "array", values: value.map ( authoringValueNode ) };
    }

    if ( isExactJsonNumber ( value ) )
    {
        return value;
    }

    const valueMap = value as ReadonlyMap<string, AuthoringValue>;

    return {
        kind: "object",
        members: Array.from ( valueMap.entries (), ( [ name, memberValue ] ) => (
            { name, value: authoringValueNode ( memberValue ) }
        ) ),
    };
}

function objectNode ( members: readonly LosslessJsonMember[] ): LosslessJsonObject
{
    return { kind: "object", members };
}

function arrayNode ( values: readonly LosslessJsonValue[] ): LosslessJsonArray
{
    return { kind: "array", values };
}

function stringArrayNode ( values: readonly string[], sort: boolean ): LosslessJsonArray
{
    const outputValues = sort ? [ ...values ].sort () : [ ...values ];

    return arrayNode ( outputValues );
}

function entityMembers ( entity: EntityDefinition ): LosslessJsonMember[]
{
    return [
        { name: "id", value: entity.id },
        { name: "name", value: entity.name },
        { name: "description", value: entity.description },
    ];
}

function sortedEntities <Entity extends EntityDefinition> ( values: readonly Entity[] ): readonly Entity[]
{
    return [ ...values ].sort ( ( first, second ) => first.id < second.id ? -1 : first.id > second.id ? 1 : 0 );
}

function createCanonicalTree ( model: AuthoringModel ): LosslessJsonObject
{
    const parameters = sortedEntities ( model.parameters ).map (
        parameter =>
        {
            const members = [ ...entityMembers ( parameter ), { name: "type", value: parameter.type } ];

            if ( parameter.enumerationValues.length > 0 )
            {
                members.push (
                    { name: "enumValues", value: stringArrayNode ( parameter.enumerationValues, true ) }
                );
            }

            return objectNode ( members );
        }
    );
    const payloads = sortedEntities ( model.payloads ).map (
        payload => objectNode (
            [
                ...entityMembers ( payload ),
                { name: "parameterIds", value: stringArrayNode ( payload.parameterIds, true ) },
            ]
        )
    );
    const events = sortedEntities ( model.events ).map (
        event => objectNode (
            [ ...entityMembers ( event ), { name: "payloadId", value: event.payloadId } ]
        )
    );
    const conditions = sortedEntities ( model.conditions ).map (
        condition =>
        {
            const members = [
                ...entityMembers ( condition ),
                { name: "parameterId", value: condition.parameterId },
            ];

            if ( condition.operator !== undefined )
            {
                const operandCount = conditionOperandCount ( condition.operator );

                members.push ( { name: "operator", value: condition.operator } );

                if ( operandCount >= 1 )
                {
                    members.push ( { name: "value", value: authoringValueNode ( condition.value ?? null ) } );
                }

                if ( operandCount === 2 )
                {
                    members.push (
                        {
                            name: "secondValue",
                            value: authoringValueNode ( condition.secondValue ?? null ),
                        }
                    );
                }
            }

            return objectNode ( members );
        }
    );
    const conditionSets = sortedEntities ( model.conditionSets ).map (
        conditionSet =>
        {
            const members = entityMembers ( conditionSet );

            if ( conditionSet.kind === "predefined" )
            {
                members.push (
                    { name: "conditionIds", value: stringArrayNode ( conditionSet.conditionIds, true ) }
                );
            }
            else
            {
                const bindings = Array.from ( conditionSet.bindings.entries () )
                    .sort ( ( first, second ) => first [ 0 ] < second [ 0 ] ? -1 : first [ 0 ] > second [ 0 ] ? 1 : 0 )
                    .map ( ( [ name, value ] ) => ( { name, value: authoringValueNode ( value ) } ) );

                members.push ( { name: "bindings", value: objectNode ( bindings ) } );
            }

            return objectNode ( members );
        }
    );
    const actions = sortedEntities ( model.actions ).map (
        action => objectNode ( entityMembers ( action ) )
    );
    const rules = sortedEntities ( model.rules ).map (
        rule => objectNode (
            [
                ...entityMembers ( rule ),
                { name: "eventId", value: rule.eventId },
                { name: "conditionSetId", value: rule.conditionSetId },
                { name: "actionId", value: rule.actionId },
            ]
        )
    );

    return objectNode (
        [
            { name: "schemaVersion", value: model.schemaVersion },
            { name: "modelId", value: model.modelId },
            { name: "name", value: model.name },
            { name: "description", value: model.description },
            { name: "parameters", value: arrayNode ( parameters ) },
            { name: "payloads", value: arrayNode ( payloads ) },
            { name: "events", value: arrayNode ( events ) },
            { name: "conditions", value: arrayNode ( conditions ) },
            { name: "conditionSets", value: arrayNode ( conditionSets ) },
            { name: "actions", value: arrayNode ( actions ) },
            { name: "rules", value: arrayNode ( rules ) },
        ]
    );
}

export class AuthoringModelJsonCodec
{
    public read ( document: string ): AuthoringModel
    {
        const root = requireJsonObject ( parseLosslessJson ( document ), "$" );

        rejectUnknownJsonMembers ( root, TOP_LEVEL_FIELDS, "$" );

        return {
            schemaVersion: requireJsonString (
                requireMember ( root, "schemaVersion", "$" ),
                "$.schemaVersion"
            ),
            modelId: requireJsonString ( requireMember ( root, "modelId", "$" ), "$.modelId" ),
            name: requireJsonString ( requireMember ( root, "name", "$" ), "$.name" ),
            description: requireJsonString (
                requireMember ( root, "description", "$" ),
                "$.description"
            ),
            parameters: readParameters (
                requireJsonArray ( requireMember ( root, "parameters", "$" ), "$.parameters" )
            ),
            payloads: readPayloads (
                requireJsonArray ( requireMember ( root, "payloads", "$" ), "$.payloads" )
            ),
            events: readEvents (
                requireJsonArray ( requireMember ( root, "events", "$" ), "$.events" )
            ),
            conditions: readConditions (
                requireJsonArray ( requireMember ( root, "conditions", "$" ), "$.conditions" )
            ),
            conditionSets: readConditionSets (
                requireJsonArray ( requireMember ( root, "conditionSets", "$" ), "$.conditionSets" )
            ),
            actions: readActions (
                requireJsonArray ( requireMember ( root, "actions", "$" ), "$.actions" )
            ),
            rules: readRules (
                requireJsonArray ( requireMember ( root, "rules", "$" ), "$.rules" )
            ),
        };
    }

    public writePretty ( model: AuthoringModel ): string
    {
        return serializeLosslessJson ( createCanonicalTree ( model ), true );
    }

    public writeCanonical ( model: AuthoringModel ): Uint8Array <ArrayBuffer>
    {
        return new TextEncoder ().encode ( this.writeCanonicalText ( model ) );
    }

    public writeCanonicalText ( model: AuthoringModel ): string
    {
        return serializeLosslessJson ( createCanonicalTree ( model ), false );
    }

    public async revision ( model: AuthoringModel ): Promise <string>
    {
        const canonicalBytes = this.writeCanonical ( model );
        const digest = await globalThis.crypto.subtle.digest ( "SHA-256", canonicalBytes.buffer );
        const hexadecimalDigest = Array.from ( new Uint8Array ( digest ) )
            .map ( value => value.toString ( 16 ).padStart ( 2, "0" ) )
            .join ( "" );

        return `sha256:${hexadecimalDigest}`;
    }
}
