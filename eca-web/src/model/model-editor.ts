// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    model-editor
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Applies validated immutable edits for the web model overview and all seven authoring-model categories.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { createEntityDraft } from "./document-types";
import {
    CONDITION_OPERATORS,
    PARAMETER_TYPES,
    type ActionDefinition,
    type AuthoringModel,
    type AuthoringValue,
    type BindingDraft,
    type ConditionDefinition,
    type ConditionOperator,
    type ConditionSetDefinition,
    type EntityDefinition,
    type EntityDraft,
    type EventDefinition,
    type ModelCategory,
    type ParameterDefinition,
    type ParameterType,
    type PayloadDefinition,
    type RuleDefinition,
} from "./types";

export interface FieldError
{
    readonly field: string;
    readonly message: string;
}

export interface EditResult
{
    readonly entityIdentifier: string | null;
    readonly errors: readonly FieldError[];
    readonly model: AuthoringModel | null;
}

export interface DeleteResult
{
    readonly deletedIdentifier: string | null;
    readonly model: AuthoringModel | null;
    readonly references: readonly string[];
}

const IDENTIFIER_PATTERN = /^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$/;
const SIGNED_LONG_MINIMUM = -9223372036854775808n;
const SIGNED_LONG_MAXIMUM = 9223372036854775807n;

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

function uniqueIdentifier (
    model: AuthoringModel,
    category: ModelCategory,
    preferredIdentifier: string
): string
{
    const identifiers = new Set ( entitiesForCategory ( model, category ).map ( entity => entity.id ) );

    if ( !identifiers.has ( preferredIdentifier ) )
    {
        return preferredIdentifier;
    }

    let suffix = 2;

    while ( identifiers.has ( `${preferredIdentifier}-${suffix}` ) )
    {
        suffix++;
    }

    return `${preferredIdentifier}-${suffix}`;
}

function firstIdentifier ( entities: readonly EntityDefinition[] ): string
{
    return entities [ 0 ]?.id ?? "";
}

function defaultConditionValue ( parameter: ParameterDefinition | undefined ): string
{
    if ( parameter?.type === "INTEGER" )
    {
        return "0";
    }

    if ( parameter?.type === "BOOLEAN" )
    {
        return "false";
    }

    if ( parameter?.type === "ENUM" )
    {
        return parameter.enumerationValues [ 0 ] ?? "";
    }

    return "";
}

function emptyDraft ( identifier: string, name: string, description: string ): EntityDraft
{
    return {
        id: identifier,
        name,
        description,
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

export function createNewEntityDraft ( model: AuthoringModel, category: ModelCategory ): EntityDraft
{
    const singularNames: Record <ModelCategory, string> =
    {
        parameters: "parameter",
        payloads: "payload",
        events: "event",
        conditions: "condition",
        "condition-sets": "condition set",
        actions: "action",
        rules: "rule",
    };
    const identifierPrefixes: Record <ModelCategory, string> =
    {
        parameters: "parameter-new",
        payloads: "payload-new",
        events: "event-new",
        conditions: "condition-new",
        "condition-sets": "condition-set-new",
        actions: "action-new",
        rules: "rule-new",
    };
    const singularName = singularNames [ category ];
    const draft = emptyDraft (
        uniqueIdentifier ( model, category, identifierPrefixes [ category ] ),
        `New ${singularName}`,
        `Describe this ${singularName}.`
    );

    switch ( category )
    {
        case "parameters":
            return { ...draft, parameterType: "STRING" };

        case "payloads":
        case "actions":
            return draft;

        case "events":
            return { ...draft, payloadId: firstIdentifier ( model.payloads ) };

        case "conditions":
        {
            const parameterIdentifier = firstIdentifier ( model.parameters );
            const parameter = model.parameters.find ( value => value.id === parameterIdentifier );

            return {
                ...draft,
                conditionOperator: "EQUALS",
                conditionValueText: defaultConditionValue ( parameter ),
                parameterId: parameterIdentifier,
            };
        }

        case "condition-sets":
            return { ...draft, predefinedConditions: true };

        case "rules":
            return {
                ...draft,
                eventId: firstIdentifier ( model.events ),
                conditionSetId: firstIdentifier ( model.conditionSets ),
                actionId: firstIdentifier ( model.actions ),
            };
    }
}

export function createExistingEntityDraft (
    model: AuthoringModel,
    category: ModelCategory,
    entityIdentifier: string
): EntityDraft
{
    return createEntityDraft ( model, category, entityIdentifier );
}

function validateCommonDraft (
    model: AuthoringModel,
    category: ModelCategory,
    originalEntityIdentifier: string | null,
    draft: EntityDraft,
    errors: FieldError[]
): void
{
    if ( !IDENTIFIER_PATTERN.test ( draft.id ) )
    {
        errors.push ( { field: "id", message: "Use lowercase words and digits separated by single hyphens." } );
    }

    if (
        entitiesForCategory ( model, category ).some (
            entity => entity.id === draft.id && entity.id !== originalEntityIdentifier
        )
    )
    {
        errors.push ( { field: "id", message: "Choose a unique identifier in this category." } );
    }

    if ( draft.name.trim ().length === 0 )
    {
        errors.push ( { field: "name", message: "Enter a nonblank display name." } );
    }

    if ( draft.description.trim ().length === 0 )
    {
        errors.push ( { field: "description", message: "Enter a nonblank description." } );
    }
}

function validateReferences (
    values: readonly string[],
    permittedValues: ReadonlySet <string>,
    field: string,
    errors: FieldError[]
): void
{
    if ( values.some ( value => !permittedValues.has ( value ) ) )
    {
        errors.push ( { field, message: "Choose only defined references." } );
    }

    if ( new Set ( values ).size !== values.length )
    {
        errors.push ( { field, message: "Choose each reference at most once." } );
    }
}

function parseConcreteValue (
    text: string,
    parameter: ParameterDefinition,
    field: string,
    errors: FieldError[]
): AuthoringValue | undefined
{
    if ( parameter.type === "STRING" )
    {
        return text;
    }

    if ( parameter.type === "INTEGER" )
    {
        if ( !/^-?(?:0|[1-9][0-9]*)$/.test ( text ) )
        {
            errors.push ( { field, message: "Enter a signed 64-bit integer." } );
            return undefined;
        }

        const value = BigInt ( text );

        if ( value < SIGNED_LONG_MINIMUM || value > SIGNED_LONG_MAXIMUM )
        {
            errors.push ( { field, message: "Enter a signed 64-bit integer." } );
            return undefined;
        }

        return value;
    }

    if ( parameter.type === "BOOLEAN" )
    {
        if ( text !== "true" && text !== "false" )
        {
            errors.push ( { field, message: "Choose true or false." } );
            return undefined;
        }

        return text === "true";
    }

    if ( parameter.type === "ENUM" )
    {
        if ( !parameter.enumerationValues.includes ( text ) )
        {
            errors.push ( { field, message: "Choose a value from the parameter enumeration." } );
            return undefined;
        }

        return text;
    }

    errors.push ( { field, message: "Repair the referenced parameter type first." } );
    return undefined;
}

function conditionOperator ( value: string ): ConditionOperator | null
{
    return ( CONDITION_OPERATORS as readonly string[] ).includes ( value )
        ? value as ConditionOperator
        : null;
}

function operatorOperandCount ( operator: ConditionOperator ): number
{
    if ( operator === "ANY" )
    {
        return 0;
    }

    return operator === "BETWEEN_EXCLUSIVE" || operator === "BETWEEN_INCLUSIVE" ? 2 : 1;
}

export function supportedConditionOperators (
    parameter: ParameterDefinition | undefined
): readonly ConditionOperator[]
{
    return CONDITION_OPERATORS.filter (
        operator =>
            operator === "ANY"
                || operator === "EQUALS"
                || operator === "NOT_EQUALS"
                || parameter?.type === "INTEGER"
    );
}

function createParameter ( draft: EntityDraft, errors: FieldError[] ): ParameterDefinition
{
    if ( !( PARAMETER_TYPES as readonly string[] ).includes ( draft.parameterType ) )
    {
        errors.push ( { field: "parameterType", message: "Choose STRING, INTEGER, BOOLEAN, or ENUM." } );
    }

    if ( draft.parameterType === "ENUM" )
    {
        if (
            draft.enumerationValues.length === 0
                || draft.enumerationValues.some ( value => value.trim ().length === 0 )
                || new Set ( draft.enumerationValues ).size !== draft.enumerationValues.length
        )
        {
            errors.push (
                {
                    field: "enumerationValues",
                    message: "Enter at least one unique, nonblank enumeration value.",
                }
            );
        }
    }
    else if ( draft.enumerationValues.length > 0 )
    {
        errors.push (
            {
                field: "enumerationValues",
                message: "Enumeration values are available only for the ENUM type.",
            }
        );
    }

    return {
        id: draft.id,
        name: draft.name,
        description: draft.description,
        type: draft.parameterType,
        enumerationValues: [ ...draft.enumerationValues ],
    };
}

function createPayload (
    model: AuthoringModel,
    draft: EntityDraft,
    errors: FieldError[]
): PayloadDefinition
{
    validateReferences (
        draft.parameterIds,
        new Set ( model.parameters.map ( parameter => parameter.id ) ),
        "parameterIds",
        errors
    );

    return {
        id: draft.id,
        name: draft.name,
        description: draft.description,
        parameterIds: [ ...draft.parameterIds ],
    };
}

function createEvent ( model: AuthoringModel, draft: EntityDraft, errors: FieldError[] ): EventDefinition
{
    if ( !model.payloads.some ( payload => payload.id === draft.payloadId ) )
    {
        errors.push ( { field: "payloadId", message: "Choose a defined payload." } );
    }

    return {
        id: draft.id,
        name: draft.name,
        description: draft.description,
        payloadId: draft.payloadId,
    };
}

function createCondition (
    model: AuthoringModel,
    draft: EntityDraft,
    errors: FieldError[]
): ConditionDefinition
{
    const parameter = model.parameters.find ( value => value.id === draft.parameterId );
    const operator = conditionOperator ( draft.conditionOperator );

    if ( parameter === undefined )
    {
        errors.push ( { field: "parameterId", message: "Choose a defined parameter." } );
    }

    if ( operator === null )
    {
        errors.push ( { field: "conditionOperator", message: "Choose a supported condition operator." } );
    }
    else if ( !supportedConditionOperators ( parameter ).includes ( operator ) )
    {
        errors.push (
            {
                field: "conditionOperator",
                message: "Inequality and range operators require an INTEGER parameter.",
            }
        );
    }

    if ( parameter === undefined || operator === null )
    {
        return {
            id: draft.id,
            name: draft.name,
            description: draft.description,
            parameterId: draft.parameterId,
        };
    }

    const operandCount = operatorOperandCount ( operator );
    const firstValue = operandCount >= 1
        ? parseConcreteValue ( draft.conditionValueText, parameter, "conditionValueText", errors )
        : undefined;
    const secondValue = operandCount === 2
        ? parseConcreteValue ( draft.secondConditionValueText, parameter, "secondConditionValueText", errors )
        : undefined;

    if (
        typeof firstValue === "bigint"
            && typeof secondValue === "bigint"
            && firstValue >= secondValue
    )
    {
        errors.push (
            {
                field: "secondConditionValueText",
                message: "Enter an upper value greater than the lower value.",
            }
        );
    }

    return {
        id: draft.id,
        name: draft.name,
        description: draft.description,
        parameterId: draft.parameterId,
        operator,
        ...( operandCount >= 1 && firstValue !== undefined ? { value: firstValue } : {} ),
        ...( operandCount === 2 && secondValue !== undefined ? { secondValue } : {} ),
    };
}

function createConditionSet (
    model: AuthoringModel,
    draft: EntityDraft,
    errors: FieldError[]
): ConditionSetDefinition
{
    if ( draft.predefinedConditions )
    {
        validateReferences (
            draft.conditionIds,
            new Set ( model.conditions.map ( condition => condition.id ) ),
            "conditionIds",
            errors
        );

        const selectedParameters = new Set <string> ();

        for ( const conditionIdentifier of draft.conditionIds )
        {
            const condition = model.conditions.find ( value => value.id === conditionIdentifier );

            if ( condition === undefined )
            {
                continue;
            }

            if ( conditionOperator ( condition.operator ?? "" ) === null )
            {
                errors.push (
                    {
                        field: `conditionIds.${conditionIdentifier}`,
                        message: "Configure this condition before selecting it.",
                    }
                );
            }

            if ( selectedParameters.has ( condition.parameterId ) )
            {
                errors.push (
                    {
                        field: `conditionIds.${conditionIdentifier}`,
                        message: "Choose at most one condition for each parameter.",
                    }
                );
            }

            selectedParameters.add ( condition.parameterId );
        }

        return {
            kind: "predefined",
            id: draft.id,
            name: draft.name,
            description: draft.description,
            conditionIds: [ ...draft.conditionIds ],
        };
    }

    const bindings = new Map <string, AuthoringValue> ();
    const parameters = new Map ( model.parameters.map ( parameter => [ parameter.id, parameter ] ) );

    for ( const [ conditionIdentifier, binding ] of draft.bindings )
    {
        const condition = model.conditions.find ( value => value.id === conditionIdentifier );

        if ( condition === undefined )
        {
            errors.push (
                {
                    field: `bindings.${conditionIdentifier}`,
                    message: "Remove the binding for the undefined condition.",
                }
            );
            continue;
        }

        if ( binding.state === "OMITTED" )
        {
            continue;
        }

        if ( binding.state === "WILDCARD" )
        {
            bindings.set ( conditionIdentifier, null );
            continue;
        }

        const parameter = parameters.get ( condition.parameterId );

        if ( parameter === undefined )
        {
            errors.push (
                {
                    field: `bindings.${conditionIdentifier}`,
                    message: "Repair the condition's parameter reference first.",
                }
            );
            continue;
        }

        const value = parseConcreteValue (
            binding.concreteText,
            parameter,
            `bindings.${conditionIdentifier}`,
            errors
        );

        if ( value !== undefined )
        {
            bindings.set ( conditionIdentifier, value );
        }
    }

    return {
        kind: "legacy",
        id: draft.id,
        name: draft.name,
        description: draft.description,
        bindings,
    };
}

function createRule ( model: AuthoringModel, draft: EntityDraft, errors: FieldError[] ): RuleDefinition
{
    const references: readonly [ string, string, readonly EntityDefinition[] ][] =
    [
        [ "eventId", draft.eventId, model.events ],
        [ "conditionSetId", draft.conditionSetId, model.conditionSets ],
        [ "actionId", draft.actionId, model.actions ],
    ];

    for ( const [ field, value, entities ] of references )
    {
        if ( !entities.some ( entity => entity.id === value ) )
        {
            errors.push ( { field, message: "Choose a defined reference." } );
        }
    }

    return {
        id: draft.id,
        name: draft.name,
        description: draft.description,
        eventId: draft.eventId,
        conditionSetId: draft.conditionSetId,
        actionId: draft.actionId,
    };
}

function createEntity (
    model: AuthoringModel,
    category: ModelCategory,
    draft: EntityDraft,
    errors: FieldError[]
): EntityDefinition
{
    switch ( category )
    {
        case "parameters":     return createParameter ( draft, errors );
        case "payloads":       return createPayload ( model, draft, errors );
        case "events":         return createEvent ( model, draft, errors );
        case "conditions":     return createCondition ( model, draft, errors );
        case "condition-sets": return createConditionSet ( model, draft, errors );
        case "actions":
            return { id: draft.id, name: draft.name, description: draft.description };
        case "rules":          return createRule ( model, draft, errors );
    }
}

function replaceEntityInList <Entity extends EntityDefinition> (
    entities: readonly Entity[],
    originalEntityIdentifier: string | null,
    replacement: Entity | null
): readonly Entity[]
{
    if ( originalEntityIdentifier === null )
    {
        return replacement === null ? entities : [ ...entities, replacement ];
    }

    if ( !entities.some ( entity => entity.id === originalEntityIdentifier ) )
    {
        throw new Error ( `Unknown entity: ${originalEntityIdentifier}` );
    }

    return replacement === null
        ? entities.filter ( entity => entity.id !== originalEntityIdentifier )
        : entities.map ( entity => entity.id === originalEntityIdentifier ? replacement : entity );
}

function replaceEntity (
    model: AuthoringModel,
    category: ModelCategory,
    originalEntityIdentifier: string | null,
    replacement: EntityDefinition | null
): AuthoringModel
{
    switch ( category )
    {
        case "parameters":
            return {
                ...model,
                parameters: replaceEntityInList (
                    model.parameters,
                    originalEntityIdentifier,
                    replacement as ParameterDefinition | null
                ),
            };

        case "payloads":
            return {
                ...model,
                payloads: replaceEntityInList (
                    model.payloads,
                    originalEntityIdentifier,
                    replacement as PayloadDefinition | null
                ),
            };

        case "events":
            return {
                ...model,
                events: replaceEntityInList (
                    model.events,
                    originalEntityIdentifier,
                    replacement as EventDefinition | null
                ),
            };

        case "conditions":
            return {
                ...model,
                conditions: replaceEntityInList (
                    model.conditions,
                    originalEntityIdentifier,
                    replacement as ConditionDefinition | null
                ),
            };

        case "condition-sets":
            return {
                ...model,
                conditionSets: replaceEntityInList (
                    model.conditionSets,
                    originalEntityIdentifier,
                    replacement as ConditionSetDefinition | null
                ),
            };

        case "actions":
            return {
                ...model,
                actions: replaceEntityInList (
                    model.actions,
                    originalEntityIdentifier,
                    replacement as ActionDefinition | null
                ),
            };

        case "rules":
            return {
                ...model,
                rules: replaceEntityInList (
                    model.rules,
                    originalEntityIdentifier,
                    replacement as RuleDefinition | null
                ),
            };
    }
}

function replaceReference ( value: string, originalIdentifier: string, replacementIdentifier: string ): string
{
    return value === originalIdentifier ? replacementIdentifier : value;
}

function renameReferences (
    model: AuthoringModel,
    category: ModelCategory,
    originalIdentifier: string,
    replacementIdentifier: string
): AuthoringModel
{
    return {
        ...model,
        payloads: model.payloads.map (
            payload =>
                category === "parameters"
                    ? {
                        ...payload,
                        parameterIds: payload.parameterIds.map (
                            value => replaceReference ( value, originalIdentifier, replacementIdentifier )
                        ),
                    }
                    : payload
        ),
        events: model.events.map (
            event =>
                category === "payloads"
                    ? {
                        ...event,
                        payloadId: replaceReference ( event.payloadId, originalIdentifier, replacementIdentifier ),
                    }
                    : event
        ),
        conditions: model.conditions.map (
            condition =>
                category === "parameters"
                    ? {
                        ...condition,
                        parameterId: replaceReference (
                            condition.parameterId,
                            originalIdentifier,
                            replacementIdentifier
                        ),
                    }
                    : condition
        ),
        conditionSets: model.conditionSets.map (
            conditionSet =>
            {
                if ( category !== "conditions" )
                {
                    return conditionSet;
                }

                if ( conditionSet.kind === "predefined" )
                {
                    return {
                        ...conditionSet,
                        conditionIds: conditionSet.conditionIds.map (
                            value => replaceReference ( value, originalIdentifier, replacementIdentifier )
                        ),
                    };
                }

                const bindings = new Map ( conditionSet.bindings );

                if ( bindings.has ( originalIdentifier ) )
                {
                    const value = bindings.get ( originalIdentifier )!;

                    bindings.delete ( originalIdentifier );
                    bindings.set ( replacementIdentifier, value );
                }

                return { ...conditionSet, bindings };
            }
        ),
        rules: model.rules.map (
            rule =>
            ({
                ...rule,
                eventId: category === "events"
                    ? replaceReference ( rule.eventId, originalIdentifier, replacementIdentifier )
                    : rule.eventId,
                conditionSetId: category === "condition-sets"
                    ? replaceReference ( rule.conditionSetId, originalIdentifier, replacementIdentifier )
                    : rule.conditionSetId,
                actionId: category === "actions"
                    ? replaceReference ( rule.actionId, originalIdentifier, replacementIdentifier )
                    : rule.actionId,
            })
        ),
    };
}

export function applyEntityDraft (
    model: AuthoringModel,
    category: ModelCategory,
    originalEntityIdentifier: string | null,
    draft: EntityDraft
): EditResult
{
    const errors: FieldError[] = [];

    validateCommonDraft ( model, category, originalEntityIdentifier, draft, errors );

    const replacement = createEntity ( model, category, draft, errors );

    if ( errors.length > 0 )
    {
        return { entityIdentifier: null, errors, model: null };
    }

    let replacementModel = replaceEntity ( model, category, originalEntityIdentifier, replacement );

    if ( originalEntityIdentifier !== null && originalEntityIdentifier !== replacement.id )
    {
        replacementModel = renameReferences (
            replacementModel,
            category,
            originalEntityIdentifier,
            replacement.id
        );
    }

    return { entityIdentifier: replacement.id, errors: [], model: replacementModel };
}

export function applyModelDetails (
    model: AuthoringModel,
    modelIdentifier: string,
    name: string,
    description: string
): EditResult
{
    const errors: FieldError[] = [];

    if ( !IDENTIFIER_PATTERN.test ( modelIdentifier ) )
    {
        errors.push ( { field: "modelId", message: "Use lowercase words and digits separated by single hyphens." } );
    }

    if ( name.trim ().length === 0 )
    {
        errors.push ( { field: "modelName", message: "Enter a nonblank display name." } );
    }

    if ( description.trim ().length === 0 )
    {
        errors.push ( { field: "modelDescription", message: "Enter a nonblank description." } );
    }

    return errors.length > 0
        ? { entityIdentifier: null, errors, model: null }
        : {
            entityIdentifier: null,
            errors: [],
            model: { ...model, modelId: modelIdentifier, name, description },
        };
}

export function duplicateEntity (
    model: AuthoringModel,
    category: ModelCategory,
    entityIdentifier: string
): EditResult
{
    const sourceDraft = createExistingEntityDraft ( model, category, entityIdentifier );
    const copyIdentifier = uniqueIdentifier ( model, category, `${sourceDraft.id}-copy` );

    return applyEntityDraft (
        model,
        category,
        null,
        { ...sourceDraft, id: copyIdentifier, name: `${sourceDraft.name} copy` }
    );
}

function reorderEntityList <Entity extends EntityDefinition> (
    entities: readonly Entity[],
    entityIdentifier: string,
    offset: -1 | 1
): readonly Entity[] | null
{
    const currentIndex = entities.findIndex ( entity => entity.id === entityIdentifier );
    const replacementIndex = currentIndex + offset;

    if ( currentIndex < 0 || replacementIndex < 0 || replacementIndex >= entities.length )
    {
        return null;
    }

    const replacement = [ ...entities ];
    const currentEntity = replacement [ currentIndex ];
    const adjacentEntity = replacement [ replacementIndex ];

    if ( currentEntity === undefined || adjacentEntity === undefined )
    {
        return null;
    }

    replacement [ currentIndex ] = adjacentEntity;
    replacement [ replacementIndex ] = currentEntity;
    return replacement;
}

export function reorderEntity (
    model: AuthoringModel,
    category: ModelCategory,
    entityIdentifier: string,
    offset: -1 | 1
): EditResult
{
    const entities = entitiesForCategory ( model, category );
    const replacement = reorderEntityList ( entities, entityIdentifier, offset );

    if ( replacement === null )
    {
        return {
            entityIdentifier: null,
            errors: [ { field: "order", message: "The selected entity cannot move further in that direction." } ],
            model: null,
        };
    }

    const replacementModel = (() =>
    {
        switch ( category )
        {
            case "parameters":
                return { ...model, parameters: replacement as readonly ParameterDefinition[] };
            case "payloads":
                return { ...model, payloads: replacement as readonly PayloadDefinition[] };
            case "events":
                return { ...model, events: replacement as readonly EventDefinition[] };
            case "conditions":
                return { ...model, conditions: replacement as readonly ConditionDefinition[] };
            case "condition-sets":
                return { ...model, conditionSets: replacement as readonly ConditionSetDefinition[] };
            case "actions":
                return { ...model, actions: replacement as readonly ActionDefinition[] };
            case "rules":
                return { ...model, rules: replacement as readonly RuleDefinition[] };
        }
    }) ();

    return { entityIdentifier, errors: [], model: replacementModel };
}

export function findEntityReferences (
    model: AuthoringModel,
    category: ModelCategory,
    entityIdentifier: string
): readonly string[]
{
    const references: string[] = [];

    if ( category === "parameters" )
    {
        model.payloads.filter ( payload => payload.parameterIds.includes ( entityIdentifier ) )
            .forEach ( payload => references.push ( `payload ${payload.id}.parameterIds` ) );
        model.conditions.filter ( condition => condition.parameterId === entityIdentifier )
            .forEach ( condition => references.push ( `condition ${condition.id}.parameterId` ) );
    }
    else if ( category === "payloads" )
    {
        model.events.filter ( event => event.payloadId === entityIdentifier )
            .forEach ( event => references.push ( `event ${event.id}.payloadId` ) );
    }
    else if ( category === "events" )
    {
        model.rules.filter ( rule => rule.eventId === entityIdentifier )
            .forEach ( rule => references.push ( `rule ${rule.id}.eventId` ) );
    }
    else if ( category === "conditions" )
    {
        model.conditionSets.filter (
            conditionSet =>
                conditionSet.kind === "predefined"
                    ? conditionSet.conditionIds.includes ( entityIdentifier )
                    : conditionSet.bindings.has ( entityIdentifier )
        ).forEach (
            conditionSet => references.push (
                `condition set ${conditionSet.id}.${conditionSet.kind === "predefined" ? "conditionIds" : "bindings"}`
            )
        );
    }
    else if ( category === "condition-sets" )
    {
        model.rules.filter ( rule => rule.conditionSetId === entityIdentifier )
            .forEach ( rule => references.push ( `rule ${rule.id}.conditionSetId` ) );
    }
    else if ( category === "actions" )
    {
        model.rules.filter ( rule => rule.actionId === entityIdentifier )
            .forEach ( rule => references.push ( `rule ${rule.id}.actionId` ) );
    }

    return references;
}

export function deleteEntity (
    model: AuthoringModel,
    category: ModelCategory,
    entityIdentifier: string
): DeleteResult
{
    if ( !entitiesForCategory ( model, category ).some ( entity => entity.id === entityIdentifier ) )
    {
        return {
            deletedIdentifier: null,
            model: null,
            references: [ "The selected entity no longer exists." ],
        };
    }

    const references = findEntityReferences ( model, category, entityIdentifier );

    return references.length > 0
        ? { deletedIdentifier: null, model: null, references }
        : {
            deletedIdentifier: entityIdentifier,
            model: replaceEntity ( model, category, entityIdentifier, null ),
            references: [],
        };
}

export function conditionSetSpecificity ( model: AuthoringModel, draft: EntityDraft ): number
{
    if ( draft.predefinedConditions )
    {
        return draft.conditionIds.reduce (
            ( specificity, conditionIdentifier ) =>
            {
                const condition = model.conditions.find ( value => value.id === conditionIdentifier );
                const operator = conditionOperator ( condition?.operator ?? "" );

                return specificity + ( operator === "ANY" ? 1 : operator === null ? 0 : 2 );
            },
            0
        );
    }

    return Array.from ( draft.bindings.values () ).reduce (
        ( specificity, binding ) =>
            specificity + ( binding.state === "CONCRETE" ? 2 : binding.state === "WILDCARD" ? 1 : 0 ),
        0
    );
}

export function updateLegacyBinding (
    bindings: ReadonlyMap <string, BindingDraft>,
    conditionIdentifier: string,
    replacement: BindingDraft
): ReadonlyMap <string, BindingDraft>
{
    const replacementBindings = new Map ( bindings );

    replacementBindings.set ( conditionIdentifier, replacement );

    return replacementBindings;
}

export function parameterType ( value: string ): ParameterType | null
{
    return ( PARAMETER_TYPES as readonly string[] ).includes ( value ) ? value as ParameterType : null;
}
