// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    document-types
// Version: 2.0.0
// Date:    2026-08-02
// Author:  Rohin Gosling
//
// Description:
//
//   Creates UI-safe document summaries and editor drafts without React, browser, storage, or transport dependencies.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { isExactJsonNumber } from "./lossless-json";
import { validateAuthoringModel } from "./model-validator";
import {
    type AuthoringModel,
    type AuthoringValue,
    type BindingDraft,
    type DocumentContent,
    type DocumentSummary,
    type EntityDefinition,
    type EntityDraft,
    type EntitySummary,
    type ModelCategory,
    type PayloadValueDraft,
} from "./types";

function entitiesForCategory ( model: AuthoringModel, category: ModelCategory ): readonly EntityDefinition[]
{
    switch ( category )
    {
        case "parameters":     return model.parameters;
        case "payloads":       return model.payloads;
        case "events":         return model.events;
        case "conditions":     return model.conditions;
        case "condition-sets": return model.conditionSets;
        case "actions":        return model.actions;
        case "rules":          return model.rules;
    }
}

function authoringValueText ( value: AuthoringValue | undefined ): string
{
    if ( value === undefined || value === null )
    {
        return "";
    }

    if ( typeof value === "string" )
    {
        return value;
    }

    if ( typeof value === "boolean" || typeof value === "bigint" )
    {
        return String ( value );
    }

    if ( isExactJsonNumber ( value ) )
    {
        return value.lexeme;
    }

    return "";
}

function emptyEntityDraft ( entity: EntityDefinition ): EntityDraft
{
    return {
        id: entity.id,
        name: entity.name,
        description: entity.description,
        parameterType: "",
        enumerationValues: [],
        parameterIds: [],
        payloadId: "",
        parameterId: "",
        conditionOperator: "",
        conditionValueText: "",
        secondConditionValueText: "",
        bindings: new Map (),
        conditionIds: [],
        predefinedConditions: false,
        eventId: "",
        conditionSetId: "",
        actionId: "",
    };
}

export function createDocumentSummary ( model: AuthoringModel ): DocumentSummary
{
    return {
        modelId: model.modelId,
        name: model.name,
        description: model.description,
        parameterCount: model.parameters.length,
        payloadCount: model.payloads.length,
        eventCount: model.events.length,
        conditionCount: model.conditions.length,
        conditionSetCount: model.conditionSets.length,
        actionCount: model.actions.length,
        ruleCount: model.rules.length,
    };
}

export function createEntityIdentifiers (
    model: AuthoringModel
): ReadonlyMap <ModelCategory, readonly string[]>
{
    return new Map (
        [
            [ "parameters", model.parameters.map ( entity => entity.id ) ],
            [ "payloads", model.payloads.map ( entity => entity.id ) ],
            [ "events", model.events.map ( entity => entity.id ) ],
            [ "conditions", model.conditions.map ( entity => entity.id ) ],
            [ "condition-sets", model.conditionSets.map ( entity => entity.id ) ],
            [ "actions", model.actions.map ( entity => entity.id ) ],
            [ "rules", model.rules.map ( entity => entity.id ) ],
        ] as const
    );
}

export function createDocumentContent ( model: AuthoringModel ): DocumentContent
{
    const validationMessages = validateAuthoringModel ( model );

    return {
        authoringModel: model,
        summary: createDocumentSummary ( model ),
        validationMessages,
        validationErrors: validationMessages.some ( message => message.severity === "ERROR" ),
        entityIdentifiers: createEntityIdentifiers ( model ),
    };
}

export function createEntitySummaries (
    model: AuthoringModel,
    category: ModelCategory
): readonly EntitySummary[]
{
    return entitiesForCategory ( model, category ).map (
        entity =>
        {
            let details = entity.description;

            switch ( category )
            {
                case "parameters":
                    details = model.parameters.find ( value => value.id === entity.id )?.type ?? details;
                    break;

                case "payloads":
                    details = `${model.payloads.find ( value => value.id === entity.id )?.parameterIds.length ?? 0} parameters`;
                    break;

                case "events":
                    details = model.events.find ( value => value.id === entity.id )?.payloadId ?? details;
                    break;

                case "conditions":
                {
                    const condition = model.conditions.find ( value => value.id === entity.id );
                    details = condition === undefined
                        ? details
                        : `${condition.parameterId} · ${condition.operator ?? "UNCONFIGURED"}`;
                    break;
                }

                case "condition-sets":
                {
                    const conditionSet = model.conditionSets.find ( value => value.id === entity.id );
                    details = conditionSet === undefined
                        ? details
                        : `${conditionSet.kind === "predefined" ? conditionSet.conditionIds.length : conditionSet.bindings.size} conditions`;
                    break;
                }

                case "rules":
                {
                    const rule = model.rules.find ( value => value.id === entity.id );
                    details = rule === undefined
                        ? details
                        : `${rule.eventId} → ${rule.conditionSetId} → ${rule.actionId}`;
                    break;
                }

                case "actions":
                    break;
            }

            return { identifier: entity.id, name: entity.name, details };
        }
    );
}

export function createEntityDraft (
    model: AuthoringModel,
    category: ModelCategory,
    entityIdentifier: string
): EntityDraft
{
    const entity = entitiesForCategory ( model, category ).find ( value => value.id === entityIdentifier );

    if ( entity === undefined )
    {
        throw new Error ( `Unknown ${category} entity: ${entityIdentifier}` );
    }

    const draft = emptyEntityDraft ( entity );

    switch ( category )
    {
        case "parameters":
        {
            const parameter = model.parameters.find ( value => value.id === entityIdentifier )!;
            return { ...draft, parameterType: parameter.type, enumerationValues: parameter.enumerationValues };
        }

        case "payloads":
        {
            const payload = model.payloads.find ( value => value.id === entityIdentifier )!;
            return { ...draft, parameterIds: payload.parameterIds };
        }

        case "events":
        {
            const event = model.events.find ( value => value.id === entityIdentifier )!;
            return { ...draft, payloadId: event.payloadId };
        }

        case "conditions":
        {
            const condition = model.conditions.find ( value => value.id === entityIdentifier )!;
            return {
                ...draft,
                parameterId: condition.parameterId,
                conditionOperator: condition.operator ?? "",
                conditionValueText: authoringValueText ( condition.value ),
                secondConditionValueText: authoringValueText ( condition.secondValue ),
            };
        }

        case "condition-sets":
        {
            const conditionSet = model.conditionSets.find ( value => value.id === entityIdentifier )!;

            if ( conditionSet.kind === "predefined" )
            {
                return {
                    ...draft,
                    conditionIds: conditionSet.conditionIds,
                    predefinedConditions: true,
                };
            }

            const bindings = new Map <string, BindingDraft> ();

            for ( const condition of model.conditions )
            {
                if ( !conditionSet.bindings.has ( condition.id ) )
                {
                    bindings.set (
                        condition.id,
                        { conditionIdentifier: condition.id, state: "OMITTED", concreteText: "" }
                    );
                    continue;
                }

                const bindingValue = conditionSet.bindings.get ( condition.id )!;

                bindings.set (
                    condition.id,
                    bindingValue === null
                        ? { conditionIdentifier: condition.id, state: "WILDCARD", concreteText: "" }
                        : {
                            conditionIdentifier: condition.id,
                            state: "CONCRETE",
                            concreteText: authoringValueText ( bindingValue ),
                        }
                );
            }

            return { ...draft, bindings, predefinedConditions: false };
        }

        case "actions":
            return draft;

        case "rules":
        {
            const rule = model.rules.find ( value => value.id === entityIdentifier )!;
            return {
                ...draft,
                eventId: rule.eventId,
                conditionSetId: rule.conditionSetId,
                actionId: rule.actionId,
            };
        }
    }
}

export function omittedPayloadValueDraft (): PayloadValueDraft
{
    return { state: "OMITTED", text: "" };
}

export function nullPayloadValueDraft (): PayloadValueDraft
{
    return { state: "NULL", text: "" };
}

export function concretePayloadValueDraft ( text: string ): PayloadValueDraft
{
    return { state: "CONCRETE", text };
}
