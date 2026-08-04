// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    compiled-model
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Resolves a validated authoring model and occurrence documents into immutable typed compiled snapshots.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { immutableMap } from "./immutable-map";
import { validateAuthoringModel } from "./model-validator";
import type {
    CompiledActionDefinition,
    CompiledCondition,
    CompiledConditionSet,
    CompiledEventDefinition,
    CompiledEventOccurrence,
    CompiledModel,
    CompiledModelSummary,
    CompiledParameterDefinition,
    CompiledPayloadDefinition,
    CompiledPayloadValue,
    CompiledRule,
    CompiledScalarValue,
} from "./compiled-types";
import {
    ModelCompilationError,
    OccurrenceCompilationError,
} from "./compiled-types";
import type {
    AuthoringModel,
    AuthoringValue,
    ConditionDefinition,
    ConditionOperator,
    EntityDefinition,
    OccurrenceDocument,
    ParameterType,
} from "./types";

function compareIdentifiers ( firstIdentifier: string, secondIdentifier: string ): number
{
    return firstIdentifier < secondIdentifier ? -1 : firstIdentifier > secondIdentifier ? 1 : 0;
}

function sortedEntities <Entity extends EntityDefinition> ( entities: readonly Entity[] ): readonly Entity[]
{
    return [ ...entities ].sort ( ( first, second ) => compareIdentifiers ( first.id, second.id ) );
}

function requireValue <Value> ( value: Value | undefined, message: string ): Value
{
    if ( value === undefined )
    {
        throw new Error ( message );
    }

    return value;
}

function compiledScalarValue (
    parameter: CompiledParameterDefinition,
    value: AuthoringValue | undefined
): CompiledScalarValue
{
    if ( value === null || value === undefined )
    {
        throw new Error ( `Parameter ${parameter.id} requires a concrete value.` );
    }

    switch ( parameter.type )
    {
        case "STRING":
            if ( typeof value === "string" )
            {
                return value;
            }
            break;

        case "INTEGER":
            if ( typeof value === "bigint" )
            {
                return value;
            }
            break;

        case "BOOLEAN":
            if ( typeof value === "boolean" )
            {
                return value;
            }
            break;

        case "ENUM":
            if ( typeof value === "string" && parameter.enumerationValues.includes ( value ) )
            {
                return value;
            }
            break;
    }

    throw new Error ( `Value does not belong to parameter ${parameter.id}.` );
}

function immutableObject <Value extends object> ( value: Value ): Readonly <Value>
{
    return Object.freeze ( value );
}

function compileParameters ( model: AuthoringModel ): ReadonlyMap <string, CompiledParameterDefinition>
{
    return immutableMap (
        sortedEntities ( model.parameters ).map ( parameter =>
        {
            const compiledParameter = immutableObject (
                {
                    id: parameter.id,
                    type: parameter.type as ParameterType,
                    enumerationValues: Object.freeze ( [ ...parameter.enumerationValues ] ),
                }
            );

            return [ parameter.id, compiledParameter ] as const;
        } )
    );
}

function compilePayloads (
    model: AuthoringModel,
    parameters: ReadonlyMap <string, CompiledParameterDefinition>
): ReadonlyMap <string, CompiledPayloadDefinition>
{
    return immutableMap (
        sortedEntities ( model.payloads ).map ( payload =>
        {
            const payloadParameters = payload.parameterIds
                .map ( parameterId =>
                {
                    const parameter = requireValue (
                        parameters.get ( parameterId ),
                        `Unresolved parameter: ${parameterId}`
                    );

                    return [ parameterId, parameter ] as const;
                } )
                .sort ( ( first, second ) => compareIdentifiers ( first [ 0 ], second [ 0 ] ) );
            const compiledPayload = immutableObject (
                {
                    id: payload.id,
                    parameters: immutableMap ( payloadParameters ),
                }
            );

            return [ payload.id, compiledPayload ] as const;
        } )
    );
}

function compileEvents (
    model: AuthoringModel,
    payloads: ReadonlyMap <string, CompiledPayloadDefinition>
): ReadonlyMap <string, CompiledEventDefinition>
{
    return immutableMap (
        sortedEntities ( model.events ).map ( event =>
        {
            const compiledEvent = immutableObject (
                {
                    id: event.id,
                    payload: requireValue (
                        payloads.get ( event.payloadId ),
                        `Unresolved payload: ${event.payloadId}`
                    ),
                }
            );

            return [ event.id, compiledEvent ] as const;
        } )
    );
}

function compilePredefinedCondition (
    condition: ConditionDefinition,
    parameters: ReadonlyMap <string, CompiledParameterDefinition>
): CompiledCondition
{
    const parameter = requireValue (
        parameters.get ( condition.parameterId ),
        `Unresolved parameter: ${condition.parameterId}`
    );
    const operator = condition.operator as ConditionOperator;

    if ( operator === "ANY" )
    {
        return immutableObject ( { kind: "wildcard", parameter } );
    }

    const firstOperand = compiledScalarValue ( parameter, condition.value );
    const secondOperand = condition.secondValue === undefined
        ? undefined
        : compiledScalarValue ( parameter, condition.secondValue );

    return immutableObject (
        {
            kind: "comparison",
            parameter,
            operator,
            firstOperand,
            ...( secondOperand === undefined ? {} : { secondOperand } ),
        }
    );
}

function compileConditionSets (
    model: AuthoringModel,
    parameters: ReadonlyMap <string, CompiledParameterDefinition>
): ReadonlyMap <string, CompiledConditionSet>
{
    const conditions = new Map ( model.conditions.map ( condition => [ condition.id, condition ] ) );

    return immutableMap (
        sortedEntities ( model.conditionSets ).map ( conditionSet =>
        {
            const bindings: ( readonly [ string, CompiledCondition ] )[] = [];

            if ( conditionSet.kind === "predefined" )
            {
                for ( const conditionIdentifier of conditionSet.conditionIds )
                {
                    const condition = requireValue (
                        conditions.get ( conditionIdentifier ),
                        `Unresolved condition: ${conditionIdentifier}`
                    );
                    const compiledCondition = compilePredefinedCondition ( condition, parameters );

                    bindings.push ( [ compiledCondition.parameter.id, compiledCondition ] );
                }
            }
            else
            {
                for ( const [ conditionIdentifier, value ] of conditionSet.bindings )
                {
                    const condition = requireValue (
                        conditions.get ( conditionIdentifier ),
                        `Unresolved condition: ${conditionIdentifier}`
                    );
                    const parameter = requireValue (
                        parameters.get ( condition.parameterId ),
                        `Unresolved parameter: ${condition.parameterId}`
                    );
                    const compiledCondition: CompiledCondition = value === null
                        ? immutableObject ( { kind: "wildcard", parameter } )
                        : immutableObject (
                            {
                                kind: "concrete",
                                parameter,
                                value: compiledScalarValue ( parameter, value ),
                            }
                        );

                    bindings.push ( [ parameter.id, compiledCondition ] );
                }
            }

            bindings.sort ( ( first, second ) => compareIdentifiers ( first [ 0 ], second [ 0 ] ) );

            const compiledConditionSet = immutableObject (
                {
                    id: conditionSet.id,
                    bindings: immutableMap ( bindings ),
                }
            );

            return [ conditionSet.id, compiledConditionSet ] as const;
        } )
    );
}

function compileActions ( model: AuthoringModel ): ReadonlyMap <string, CompiledActionDefinition>
{
    return immutableMap (
        sortedEntities ( model.actions ).map ( action =>
        {
            const compiledAction = immutableObject ( { id: action.id } );

            return [ action.id, compiledAction ] as const;
        } )
    );
}

export function calculateSpecificity ( conditionSet: CompiledConditionSet ): number
{
    let specificity = 0;

    for ( const condition of conditionSet.bindings.values () )
    {
        specificity += condition.kind === "wildcard" ? 1 : 2;
    }

    return specificity;
}

function compileRules (
    model: AuthoringModel,
    events: ReadonlyMap <string, CompiledEventDefinition>,
    conditionSets: ReadonlyMap <string, CompiledConditionSet>,
    actions: ReadonlyMap <string, CompiledActionDefinition>
): readonly CompiledRule[]
{
    return Object.freeze (
        sortedEntities ( model.rules ).map ( rule =>
        {
            const conditionSet = requireValue (
                conditionSets.get ( rule.conditionSetId ),
                `Unresolved condition set: ${rule.conditionSetId}`
            );

            return immutableObject (
                {
                    id: rule.id,
                    event: requireValue ( events.get ( rule.eventId ), `Unresolved event: ${rule.eventId}` ),
                    conditionSet,
                    action: requireValue ( actions.get ( rule.actionId ), `Unresolved action: ${rule.actionId}` ),
                    specificity: calculateSpecificity ( conditionSet ),
                }
            );
        } )
    );
}

export class AuthoringModelCompiler
{
    public compile ( model: AuthoringModel, revision: string ): CompiledModel
    {
        const diagnostics = validateAuthoringModel ( model );

        if ( diagnostics.length > 0 )
        {
            throw new ModelCompilationError ( diagnostics );
        }

        const parameters    = compileParameters ( model );
        const payloads      = compilePayloads ( model, parameters );
        const events        = compileEvents ( model, payloads );
        const conditionSets = compileConditionSets ( model, parameters );
        const actions       = compileActions ( model );
        const rules         = compileRules ( model, events, conditionSets, actions );

        return immutableObject (
            {
                modelId: model.modelId,
                revision,
                parameters,
                payloads,
                events,
                conditionSets,
                actions,
                ruleBase: immutableObject ( { rules } ),
            }
        );
    }
}

export function compileOccurrence (
    model: CompiledModel,
    occurrence: OccurrenceDocument
): CompiledEventOccurrence
{
    const event = model.events.get ( occurrence.eventId );

    if ( event === undefined )
    {
        throw new OccurrenceCompilationError ( `Unknown event identifier: ${occurrence.eventId}` );
    }

    if ( !occurrence.payloadPresent && event.payload.parameters.size > 0 )
    {
        throw new OccurrenceCompilationError (
            "payload may be omitted only for an event with an empty payload schema."
        );
    }

    const bindings: ( readonly [ string, CompiledPayloadValue ] )[] = [];

    for ( const [ parameterIdentifier, value ] of occurrence.payload )
    {
        const parameter = model.parameters.get ( parameterIdentifier );

        if ( parameter === undefined )
        {
            throw new OccurrenceCompilationError ( `Unknown parameter identifier: ${parameterIdentifier}` );
        }

        if ( !event.payload.parameters.has ( parameterIdentifier ) )
        {
            throw new OccurrenceCompilationError (
                `Payload parameter is not permitted by event ${event.id}.`
            );
        }

        try
        {
            const payloadValue: CompiledPayloadValue = value === null
                ? immutableObject ( { kind: "present-null", parameter } )
                : immutableObject (
                    {
                        kind: "concrete",
                        parameter,
                        value: compiledScalarValue ( parameter, value ),
                    }
                );

            bindings.push ( [ parameterIdentifier, payloadValue ] );
        }
        catch ( error )
        {
            const message = error instanceof Error ? error.message : String ( error );

            throw new OccurrenceCompilationError ( message );
        }
    }

    bindings.sort ( ( first, second ) => compareIdentifiers ( first [ 0 ], second [ 0 ] ) );

    return immutableObject ( { event, payload: immutableMap ( bindings ) } );
}

export function createCompiledModelSummary ( model: CompiledModel ): CompiledModelSummary
{
    return immutableObject (
        {
            modelId: model.modelId,
            revision: model.revision,
            parameterIds: Object.freeze ( [ ...model.parameters.keys () ] ),
            payloadIds: Object.freeze ( [ ...model.payloads.keys () ] ),
            eventIds: Object.freeze ( [ ...model.events.keys () ] ),
            conditionSetIds: Object.freeze ( [ ...model.conditionSets.keys () ] ),
            actionIds: Object.freeze ( [ ...model.actions.keys () ] ),
            rules: Object.freeze (
                model.ruleBase.rules.map ( rule => immutableObject (
                    {
                        id: rule.id,
                        eventId: rule.event.id,
                        conditionSetId: rule.conditionSet.id,
                        actionId: rule.action.id,
                        specificity: rule.specificity,
                    }
                ) )
            ),
        }
    );
}
