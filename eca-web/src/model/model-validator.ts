// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    model-validator
// Version: 2.0.0
// Date:    2026-08-02
// Author:  Rohin Gosling
//
// Description:
//
//   Validates TypeScript authoring models with the same diagnostics, ordering, limits, and remedies as Java.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { isExactJsonNumber } from "./lossless-json";
import {
    CONDITION_OPERATORS,
    PARAMETER_TYPES,
    type AuthoringModel,
    type AuthoringValue,
    type ConditionDefinition,
    type ConditionOperator,
    type ConditionSetDefinition,
    type EntityDefinition,
    type ModelLimits,
    type ParameterDefinition,
    type ParameterType,
    type ValidationDiagnostic,
} from "./types";

export const BLANK_FIELD = "BLANK_FIELD";
export const DUPLICATE_IDENTIFIER = "DUPLICATE_IDENTIFIER";
export const DUPLICATE_PARAMETER_BINDING = "DUPLICATE_PARAMETER_BINDING";
export const DUPLICATE_REFERENCE = "DUPLICATE_REFERENCE";
export const ENUM_VALUE_OUT_OF_DOMAIN = "ENUM_VALUE_OUT_OF_DOMAIN";
export const INVALID_CONCRETE_VALUE = "INVALID_CONCRETE_VALUE";
export const INVALID_CONDITION_OPERATOR = "INVALID_CONDITION_OPERATOR";
export const INVALID_CONDITION_RANGE = "INVALID_CONDITION_RANGE";
export const INVALID_ENUM_DOMAIN = "INVALID_ENUM_DOMAIN";
export const INVALID_IDENTIFIER = "INVALID_IDENTIFIER";
export const INVALID_PARAMETER_TYPE = "INVALID_PARAMETER_TYPE";
export const NON_INTEGRAL_INTEGER = "NON_INTEGRAL_INTEGER";
export const RULE_CONDITION_NOT_PERMITTED = "RULE_CONDITION_NOT_PERMITTED";
export const SIZE_LIMIT_EXCEEDED = "SIZE_LIMIT_EXCEEDED";
export const UNRESOLVED_REFERENCE = "UNRESOLVED_REFERENCE";
export const UNSUPPORTED_SCHEMA_VERSION = "UNSUPPORTED_SCHEMA_VERSION";

export const DEFAULT_MODEL_LIMITS: ModelLimits = {
    maximumEntitiesPerCategory: 10000,
    maximumBindingsPerConditionSet: 1000,
};

const SUPPORTED_SCHEMA_VERSION = "1.0";
const IDENTIFIER_PATTERN = /^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$/;
const SIGNED_LONG_MINIMUM = -9223372036854775808n;
const SIGNED_LONG_MAXIMUM = 9223372036854775807n;

function addDiagnostic (
    diagnostics: ValidationDiagnostic[],
    entityId: string,
    field: string,
    code: string,
    remedy: string
): void
{
    diagnostics.push ( { entityId, field, code, severity: "ERROR", remedy } );
}

function validateIdentifier (
    diagnostics: ValidationDiagnostic[],
    entityId: string,
    value: string,
    field: string
): void
{
    if ( !IDENTIFIER_PATTERN.test ( value ) )
    {
        addDiagnostic (
            diagnostics,
            entityId,
            field,
            INVALID_IDENTIFIER,
            "Use lowercase kebab case beginning with a letter."
        );
    }
}

function validateText (
    diagnostics: ValidationDiagnostic[],
    entityId: string,
    field: string,
    value: string
): void
{
    if ( value.trim ().length === 0 )
    {
        addDiagnostic ( diagnostics, entityId, field, BLANK_FIELD, "Provide nonblank text." );
    }
}

function indexEntities <Entity extends EntityDefinition> (
    entities: readonly Entity[],
    category: string,
    diagnostics: ValidationDiagnostic[]
): ReadonlyMap <string, Entity>
{
    const entitiesByIdentifier = new Map <string, Entity> ();

    for ( const entity of entities )
    {
        validateIdentifier ( diagnostics, entity.id, entity.id, "id" );
        validateText ( diagnostics, entity.id, "name", entity.name );
        validateText ( diagnostics, entity.id, "description", entity.description );

        if ( entitiesByIdentifier.has ( entity.id ) )
        {
            addDiagnostic (
                diagnostics,
                entity.id,
                "id",
                DUPLICATE_IDENTIFIER,
                `Choose a unique identifier within ${category}.`
            );
        }
        else
        {
            entitiesByIdentifier.set ( entity.id, entity );
        }
    }

    return entitiesByIdentifier;
}

function parseParameterType ( value: string ): ParameterType | undefined
{
    return ( PARAMETER_TYPES as readonly string[] ).includes ( value )
        ? value as ParameterType
        : undefined;
}

function parseConditionOperator ( value: string | undefined ): ConditionOperator | undefined
{
    return value !== undefined && ( CONDITION_OPERATORS as readonly string[] ).includes ( value )
        ? value as ConditionOperator
        : undefined;
}

function operatorOperandCount ( operator: ConditionOperator ): number
{
    if ( operator === "ANY" )
    {
        return 0;
    }

    if ( operator === "BETWEEN_EXCLUSIVE" || operator === "BETWEEN_INCLUSIVE" )
    {
        return 2;
    }

    return 1;
}

function operatorSupports ( operator: ConditionOperator, parameterType: ParameterType ): boolean
{
    return operator === "ANY"
        || operator === "EQUALS"
        || operator === "NOT_EQUALS"
        || parameterType === "INTEGER";
}

function validateReference <Target> (
    diagnostics: ValidationDiagnostic[],
    entityId: string,
    field: string,
    reference: string,
    targets: ReadonlyMap <string, Target>,
    remedy: string
): void
{
    if ( !targets.has ( reference ) )
    {
        addDiagnostic ( diagnostics, entityId, field, UNRESOLVED_REFERENCE, remedy );
    }
}

function validateIntegerValue (
    diagnostics: ValidationDiagnostic[],
    entityId: string,
    field: string,
    value: AuthoringValue | undefined
): void
{
    const valid = typeof value === "bigint"
        && value >= SIGNED_LONG_MINIMUM
        && value <= SIGNED_LONG_MAXIMUM;

    if ( !valid )
    {
        addDiagnostic (
            diagnostics,
            entityId,
            field,
            NON_INTEGRAL_INTEGER,
            "Use an integral JSON number within the signed 64-bit range."
        );
    }
}

function validateConcreteValue (
    diagnostics: ValidationDiagnostic[],
    entityId: string,
    field: string,
    value: AuthoringValue | undefined,
    parameter: ParameterDefinition
): void
{
    const parameterType = parseParameterType ( parameter.type );

    if ( parameterType === undefined )
    {
        return;
    }

    switch ( parameterType )
    {
        case "STRING":
            if ( typeof value !== "string" )
            {
                addDiagnostic ( diagnostics, entityId, field, INVALID_CONCRETE_VALUE, "Use a JSON string." );
            }
            break;

        case "BOOLEAN":
            if ( typeof value !== "boolean" )
            {
                addDiagnostic ( diagnostics, entityId, field, INVALID_CONCRETE_VALUE, "Use true or false." );
            }
            break;

        case "INTEGER":
            validateIntegerValue ( diagnostics, entityId, field, value );
            break;

        case "ENUM":
            if ( typeof value !== "string" || !parameter.enumerationValues.includes ( value ) )
            {
                addDiagnostic (
                    diagnostics,
                    entityId,
                    field,
                    ENUM_VALUE_OUT_OF_DOMAIN,
                    "Use one of the parameter's declared enumValues."
                );
            }
            break;
    }
}

function validateUniqueConditionParameter (
    diagnostics: ValidationDiagnostic[],
    conditionSet: ConditionSetDefinition,
    condition: ConditionDefinition,
    encounteredParameterIdentifiers: Set <string>,
    field: string
): void
{
    if ( encounteredParameterIdentifiers.has ( condition.parameterId ) )
    {
        addDiagnostic (
            diagnostics,
            conditionSet.id,
            field,
            DUPLICATE_PARAMETER_BINDING,
            "Keep at most one included condition for each parameter."
        );
    }
    else
    {
        encounteredParameterIdentifiers.add ( condition.parameterId );
    }
}

function validateRangeOrder (
    diagnostics: ValidationDiagnostic[],
    condition: ConditionDefinition,
    parameterType: ParameterType
): void
{
    if ( parameterType !== "INTEGER" )
    {
        return;
    }

    if ( typeof condition.value === "bigint" && typeof condition.secondValue === "bigint" )
    {
        if ( condition.value >= condition.secondValue )
        {
            addDiagnostic (
                diagnostics,
                condition.id,
                "secondValue",
                INVALID_CONDITION_RANGE,
                "Enter an upper value greater than the lower value."
            );
        }

        return;
    }

}

function validateCategorySize (
    diagnostics: ValidationDiagnostic[],
    modelId: string,
    field: string,
    size: number,
    limits: ModelLimits
): void
{
    if ( size > limits.maximumEntitiesPerCategory )
    {
        addDiagnostic (
            diagnostics,
            modelId,
            field,
            SIZE_LIMIT_EXCEEDED,
            `Reduce the category to ${limits.maximumEntitiesPerCategory} entities or fewer.`
        );
    }
}

export function validateAuthoringModel (
    model: AuthoringModel,
    limits: ModelLimits = DEFAULT_MODEL_LIMITS
): readonly ValidationDiagnostic[]
{
    const diagnostics: ValidationDiagnostic[] = [];

    if ( model.schemaVersion !== SUPPORTED_SCHEMA_VERSION )
    {
        addDiagnostic (
            diagnostics,
            model.modelId,
            "schemaVersion",
            UNSUPPORTED_SCHEMA_VERSION,
            `Use schema version ${SUPPORTED_SCHEMA_VERSION}.`
        );
    }

    validateIdentifier ( diagnostics, model.modelId, model.modelId, "modelId" );
    validateText ( diagnostics, model.modelId, "name", model.name );
    validateText ( diagnostics, model.modelId, "description", model.description );

    validateCategorySize ( diagnostics, model.modelId, "parameters", model.parameters.length, limits );
    validateCategorySize ( diagnostics, model.modelId, "payloads", model.payloads.length, limits );
    validateCategorySize ( diagnostics, model.modelId, "events", model.events.length, limits );
    validateCategorySize ( diagnostics, model.modelId, "conditions", model.conditions.length, limits );
    validateCategorySize ( diagnostics, model.modelId, "conditionSets", model.conditionSets.length, limits );
    validateCategorySize ( diagnostics, model.modelId, "actions", model.actions.length, limits );
    validateCategorySize ( diagnostics, model.modelId, "rules", model.rules.length, limits );

    for ( const conditionSet of model.conditionSets )
    {
        const conditionCount = conditionSet.kind === "predefined"
            ? conditionSet.conditionIds.length
            : conditionSet.bindings.size;

        if ( conditionCount > limits.maximumBindingsPerConditionSet )
        {
            addDiagnostic (
                diagnostics,
                conditionSet.id,
                conditionSet.kind === "predefined" ? "conditionIds" : "bindings",
                SIZE_LIMIT_EXCEEDED,
                `Reduce the binding count to ${limits.maximumBindingsPerConditionSet} or fewer.`
            );
        }
    }

    const parameters = indexEntities ( model.parameters, "parameters", diagnostics );
    const payloads = indexEntities ( model.payloads, "payloads", diagnostics );
    const events = indexEntities ( model.events, "events", diagnostics );
    const conditions = indexEntities ( model.conditions, "conditions", diagnostics );
    const conditionSets = indexEntities ( model.conditionSets, "conditionSets", diagnostics );
    const actions = indexEntities ( model.actions, "actions", diagnostics );

    indexEntities ( model.rules, "rules", diagnostics );

    for ( const parameter of model.parameters )
    {
        const parameterType = parseParameterType ( parameter.type );

        if ( parameterType === undefined )
        {
            addDiagnostic (
                diagnostics,
                parameter.id,
                "type",
                INVALID_PARAMETER_TYPE,
                "Use STRING, INTEGER, BOOLEAN, or ENUM."
            );
            continue;
        }

        const encounteredValues = new Set <string> ();
        let validEnumerationValues = true;

        for ( const enumerationValue of parameter.enumerationValues )
        {
            if ( enumerationValue.trim ().length === 0 || encounteredValues.has ( enumerationValue ) )
            {
                validEnumerationValues = false;
            }

            encounteredValues.add ( enumerationValue );
        }

        if ( parameterType === "ENUM" && ( parameter.enumerationValues.length === 0 || !validEnumerationValues ) )
        {
            addDiagnostic (
                diagnostics,
                parameter.id,
                "enumValues",
                INVALID_ENUM_DOMAIN,
                "Provide at least one unique, nonblank enumeration value."
            );
        }
        else if ( parameterType !== "ENUM" && parameter.enumerationValues.length > 0 )
        {
            addDiagnostic (
                diagnostics,
                parameter.id,
                "enumValues",
                INVALID_ENUM_DOMAIN,
                "Remove enumValues from non-ENUM parameters."
            );
        }
    }

    for ( const payload of model.payloads )
    {
        const encounteredParameterIdentifiers = new Set <string> ();

        for ( const parameterId of payload.parameterIds )
        {
            if ( encounteredParameterIdentifiers.has ( parameterId ) )
            {
                addDiagnostic (
                    diagnostics,
                    payload.id,
                    "parameterIds",
                    DUPLICATE_REFERENCE,
                    "List each permitted parameter once."
                );
            }

            encounteredParameterIdentifiers.add ( parameterId );
            validateReference (
                diagnostics,
                payload.id,
                "parameterIds",
                parameterId,
                parameters,
                "Define the referenced parameter or remove it from the payload."
            );
        }
    }

    for ( const event of model.events )
    {
        validateReference (
            diagnostics,
            event.id,
            "payloadId",
            event.payloadId,
            payloads,
            "Reference a defined payload."
        );
    }

    for ( const condition of model.conditions )
    {
        validateReference (
            diagnostics,
            condition.id,
            "parameterId",
            condition.parameterId,
            parameters,
            "Reference a defined parameter."
        );

        const parameter = parameters.get ( condition.parameterId );

        if ( condition.operator === undefined || parameter === undefined )
        {
            continue;
        }

        const operator = parseConditionOperator ( condition.operator );
        const parameterType = parseParameterType ( parameter.type );

        if ( operator === undefined )
        {
            addDiagnostic (
                diagnostics,
                condition.id,
                "operator",
                INVALID_CONDITION_OPERATOR,
                "Choose a supported condition operator."
            );
            continue;
        }

        if ( parameterType === undefined )
        {
            continue;
        }

        if ( !operatorSupports ( operator, parameterType ) )
        {
            addDiagnostic (
                diagnostics,
                condition.id,
                "operator",
                INVALID_CONDITION_OPERATOR,
                "Use equality for this type, or choose an INTEGER parameter for inequalities."
            );
            continue;
        }

        if ( operatorOperandCount ( operator ) >= 1 )
        {
            validateConcreteValue ( diagnostics, condition.id, "value", condition.value, parameter );
        }

        if ( operatorOperandCount ( operator ) === 2 )
        {
            validateConcreteValue (
                diagnostics,
                condition.id,
                "secondValue",
                condition.secondValue,
                parameter
            );
            validateRangeOrder ( diagnostics, condition, parameterType );
        }
    }

    for ( const conditionSet of model.conditionSets )
    {
        const encounteredParameterIdentifiers = new Set <string> ();

        if ( conditionSet.kind === "predefined" )
        {
            const encounteredConditionIdentifiers = new Set <string> ();

            for ( const conditionIdentifier of conditionSet.conditionIds )
            {
                const condition = conditions.get ( conditionIdentifier );

                if ( encounteredConditionIdentifiers.has ( conditionIdentifier ) )
                {
                    addDiagnostic (
                        diagnostics,
                        conditionSet.id,
                        "conditionIds",
                        DUPLICATE_REFERENCE,
                        "Choose each predefined condition at most once."
                    );
                }

                encounteredConditionIdentifiers.add ( conditionIdentifier );

                if ( condition === undefined )
                {
                    addDiagnostic (
                        diagnostics,
                        conditionSet.id,
                        "conditionIds",
                        UNRESOLVED_REFERENCE,
                        "Reference only defined conditions."
                    );
                    continue;
                }

                if ( condition.operator === undefined )
                {
                    addDiagnostic (
                        diagnostics,
                        conditionSet.id,
                        "conditionIds",
                        INVALID_CONDITION_OPERATOR,
                        "Configure the selected condition's operator and value first."
                    );
                }

                validateUniqueConditionParameter (
                    diagnostics,
                    conditionSet,
                    condition,
                    encounteredParameterIdentifiers,
                    "conditionIds"
                );
            }

            continue;
        }

        for ( const [ conditionIdentifier, bindingValue ] of conditionSet.bindings )
        {
            const condition = conditions.get ( conditionIdentifier );

            if ( condition === undefined )
            {
                addDiagnostic (
                    diagnostics,
                    conditionSet.id,
                    `bindings.${conditionIdentifier}`,
                    UNRESOLVED_REFERENCE,
                    "Reference a defined condition or remove the binding."
                );
                continue;
            }

            validateUniqueConditionParameter (
                diagnostics,
                conditionSet,
                condition,
                encounteredParameterIdentifiers,
                `bindings.${conditionIdentifier}`
            );

            const parameter = parameters.get ( condition.parameterId );

            if ( parameter !== undefined && bindingValue !== null )
            {
                validateConcreteValue (
                    diagnostics,
                    conditionSet.id,
                    `bindings.${conditionIdentifier}`,
                    bindingValue,
                    parameter
                );
            }
        }
    }

    for ( const rule of model.rules )
    {
        validateReference (
            diagnostics,
            rule.id,
            "eventId",
            rule.eventId,
            events,
            "Reference a defined event."
        );
        validateReference (
            diagnostics,
            rule.id,
            "conditionSetId",
            rule.conditionSetId,
            conditionSets,
            "Reference a defined condition set."
        );
        validateReference (
            diagnostics,
            rule.id,
            "actionId",
            rule.actionId,
            actions,
            "Reference a defined action."
        );

        const event = events.get ( rule.eventId );
        const conditionSet = conditionSets.get ( rule.conditionSetId );

        if ( event === undefined || conditionSet === undefined )
        {
            continue;
        }

        const payload = payloads.get ( event.payloadId );

        if ( payload === undefined )
        {
            continue;
        }

        const permittedParameterIdentifiers = new Set ( payload.parameterIds );
        const includedConditionIdentifiers = conditionSet.kind === "predefined"
            ? conditionSet.conditionIds
            : conditionSet.bindings.keys ();

        for ( const conditionIdentifier of includedConditionIdentifiers )
        {
            const condition = conditions.get ( conditionIdentifier );
            const concreteCondition = conditionSet.kind === "predefined"
                ? condition !== undefined && condition.operator !== undefined && condition.operator !== "ANY"
                : conditionSet.bindings.get ( conditionIdentifier ) !== null;

            if ( concreteCondition && condition !== undefined
                && !permittedParameterIdentifiers.has ( condition.parameterId ) )
            {
                addDiagnostic (
                    diagnostics,
                    rule.id,
                    "conditionSetId",
                    RULE_CONDITION_NOT_PERMITTED,
                    "Remove the concrete condition or permit its parameter in the event payload."
                );
            }
        }
    }

    return diagnostics;
}

export function modelContainsUnsafeNumericValue ( model: AuthoringModel ): boolean
{
    const containsUnsafeValue = ( value: AuthoringValue | undefined ): boolean =>
    {
        if ( value === undefined || value === null || typeof value === "string" || typeof value === "boolean" )
        {
            return false;
        }

        if ( typeof value === "bigint" )
        {
            return value < SIGNED_LONG_MINIMUM || value > SIGNED_LONG_MAXIMUM;
        }

        if ( isExactJsonNumber ( value ) )
        {
            return true;
        }

        if ( Array.isArray ( value ) )
        {
            return value.some ( containsUnsafeValue );
        }

        return Array.from ( value.values () ).some ( containsUnsafeValue );
    };

    return model.conditions.some (
        condition => containsUnsafeValue ( condition.value ) || containsUnsafeValue ( condition.secondValue )
    ) || model.conditionSets.some (
        conditionSet => conditionSet.kind === "legacy"
            && Array.from ( conditionSet.bindings.values () ).some ( containsUnsafeValue )
    );
}
