// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    simulator-service
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Derives typed occurrence controls and lossless Concrete/null/Omitted documents from an authoring model.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import type {
    AuthoringModel,
    OccurrenceDocument,
    ParameterType,
    PayloadValueDraft,
} from "../model";

const SIGNED_LONG_MINIMUM = -9223372036854775808n;
const SIGNED_LONG_MAXIMUM = 9223372036854775807n;

export interface SimulatorPayloadField
{
    readonly identifier: string;
    readonly name: string;
    readonly type: ParameterType;
    readonly enumerationValues: readonly string[];
}

export interface SimulatorEvent
{
    readonly identifier: string;
    readonly name: string;
    readonly payloadFields: readonly SimulatorPayloadField[];
}

export class SimulatorDraftFailure extends Error
{
    public readonly parameterIdentifier: string;

    public constructor ( parameterIdentifier: string, message: string )
    {
        super ( message );

        this.name                = "SimulatorDraftFailure";
        this.parameterIdentifier = parameterIdentifier;
    }
}

function defaultConcreteText ( field: SimulatorPayloadField ): string
{
    switch ( field.type )
    {
        case "INTEGER":
            return "0";

        case "BOOLEAN":
            return "false";

        case "ENUM":
            return field.enumerationValues [ 0 ] ?? "";

        case "STRING":
            return "";
    }
}

export function simulatorEvents ( model: AuthoringModel ): readonly SimulatorEvent[]
{
    const payloads = new Map ( model.payloads.map ( payload => [ payload.id, payload ] ) );
    const parameters = new Map ( model.parameters.map ( parameter => [ parameter.id, parameter ] ) );

    return model.events.map ( event =>
    {
        const payload = payloads.get ( event.payloadId );
        const payloadFields = ( payload?.parameterIds ?? [] ).flatMap ( parameterIdentifier =>
        {
            const parameter = parameters.get ( parameterIdentifier );

            if (
                parameter === undefined
                || ![ "STRING", "INTEGER", "BOOLEAN", "ENUM" ].includes ( parameter.type )
            )
            {
                return [];
            }

            return [
                {
                    identifier: parameter.id,
                    name: parameter.name,
                    type: parameter.type as ParameterType,
                    enumerationValues: parameter.enumerationValues,
                },
            ];
        } );

        return {
            identifier: event.id,
            name: event.name,
            payloadFields,
        };
    } );
}

export function defaultPayloadDrafts (
    event: SimulatorEvent | undefined
): ReadonlyMap <string, PayloadValueDraft>
{
    return new Map (
        ( event?.payloadFields ?? [] ).map ( field =>
            [
                field.identifier,
                { state: "OMITTED", text: defaultConcreteText ( field ) },
            ] as const
        )
    );
}

function concreteValue ( field: SimulatorPayloadField, text: string ): string | boolean | bigint
{
    switch ( field.type )
    {
        case "STRING":
            return text;

        case "BOOLEAN":
            if ( text === "true" )
            {
                return true;
            }

            if ( text === "false" )
            {
                return false;
            }

            throw new SimulatorDraftFailure ( field.identifier, "Choose true or false." );

        case "ENUM":
            if ( field.enumerationValues.includes ( text ) )
            {
                return text;
            }

            throw new SimulatorDraftFailure (
                field.identifier,
                "Choose one of the parameter's enumeration values."
            );

        case "INTEGER":
            if ( !/^-?(?:0|[1-9][0-9]*)$/.test ( text ) )
            {
                throw new SimulatorDraftFailure ( field.identifier, "Enter a whole decimal integer." );
            }

            const integerValue = BigInt ( text );

            if ( integerValue < SIGNED_LONG_MINIMUM || integerValue > SIGNED_LONG_MAXIMUM )
            {
                throw new SimulatorDraftFailure (
                    field.identifier,
                    "Enter a signed 64-bit integer."
                );
            }

            return integerValue;
    }
}

export function createOccurrence (
    event: SimulatorEvent,
    drafts: ReadonlyMap <string, PayloadValueDraft>
): OccurrenceDocument
{
    const payload = new Map <string, string | boolean | bigint | null> ();

    for ( const field of event.payloadFields )
    {
        const draft = drafts.get ( field.identifier ) ?? { state: "OMITTED", text: "" };

        if ( draft.state === "NULL" )
        {
            payload.set ( field.identifier, null );
        }
        else if ( draft.state === "CONCRETE" )
        {
            payload.set ( field.identifier, concreteValue ( field, draft.text ) );
        }
    }

    return {
        eventId: event.identifier,
        payload,
        payloadPresent: true,
    };
}
