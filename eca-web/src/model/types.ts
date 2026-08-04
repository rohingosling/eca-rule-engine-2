// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    types
// Version: 2.0.0
// Date:    2026-08-02
// Author:  Rohin Gosling
//
// Description:
//
//   Defines the framework-independent authoring-model, occurrence, diagnostic, summary, and editor-draft contracts.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

export const PARAMETER_TYPES = [ "STRING", "INTEGER", "BOOLEAN", "ENUM" ] as const;

export const CONDITION_OPERATORS = [
    "EQUALS",
    "NOT_EQUALS",
    "GREATER_THAN",
    "GREATER_THAN_OR_EQUAL",
    "LESS_THAN",
    "LESS_THAN_OR_EQUAL",
    "BETWEEN_EXCLUSIVE",
    "BETWEEN_INCLUSIVE",
    "ANY",
] as const;

export const MODEL_CATEGORIES = [
    "parameters",
    "payloads",
    "events",
    "conditions",
    "condition-sets",
    "actions",
    "rules",
] as const;

export type ParameterType = typeof PARAMETER_TYPES[number];
export type ConditionOperator = typeof CONDITION_OPERATORS[number];
export type ModelCategory = typeof MODEL_CATEGORIES[number];
export type ValidationSeverity = "ERROR" | "WARNING";

export interface ExactJsonNumber
{
    readonly kind: "exact-json-number";
    readonly lexeme: string;
}

export type AuthoringValue =
    | string
    | boolean
    | bigint
    | ExactJsonNumber
    | null
    | readonly AuthoringValue[]
    | ReadonlyMap <string, AuthoringValue>;

export interface EntityDefinition
{
    readonly id: string;
    readonly name: string;
    readonly description: string;
}

export interface ParameterDefinition extends EntityDefinition
{
    readonly type: string;
    readonly enumerationValues: readonly string[];
}

export interface PayloadDefinition extends EntityDefinition
{
    readonly parameterIds: readonly string[];
}

export interface EventDefinition extends EntityDefinition
{
    readonly payloadId: string;
}

export interface ConditionDefinition extends EntityDefinition
{
    readonly parameterId: string;
    readonly operator?: string;
    readonly value?: AuthoringValue;
    readonly secondValue?: AuthoringValue;
}

export interface PredefinedConditionSetDefinition extends EntityDefinition
{
    readonly kind: "predefined";
    readonly conditionIds: readonly string[];
}

export interface LegacyConditionSetDefinition extends EntityDefinition
{
    readonly kind: "legacy";
    readonly bindings: ReadonlyMap <string, AuthoringValue>;
}

export type ConditionSetDefinition = PredefinedConditionSetDefinition | LegacyConditionSetDefinition;

export interface ActionDefinition extends EntityDefinition
{
}

export interface RuleDefinition extends EntityDefinition
{
    readonly eventId: string;
    readonly conditionSetId: string;
    readonly actionId: string;
}

export interface AuthoringModel
{
    readonly schemaVersion: string;
    readonly modelId: string;
    readonly name: string;
    readonly description: string;
    readonly parameters: readonly ParameterDefinition[];
    readonly payloads: readonly PayloadDefinition[];
    readonly events: readonly EventDefinition[];
    readonly conditions: readonly ConditionDefinition[];
    readonly conditionSets: readonly ConditionSetDefinition[];
    readonly actions: readonly ActionDefinition[];
    readonly rules: readonly RuleDefinition[];
}

export interface ModelLimits
{
    readonly maximumEntitiesPerCategory: number;
    readonly maximumBindingsPerConditionSet: number;
}

export interface ValidationDiagnostic
{
    readonly entityId: string;
    readonly field: string;
    readonly code: string;
    readonly severity: ValidationSeverity;
    readonly remedy: string;
}

export interface OccurrenceDocument
{
    readonly eventId: string;
    readonly payload: ReadonlyMap <string, AuthoringValue>;
    readonly payloadPresent: boolean;
}

export interface DocumentSummary
{
    readonly modelId: string;
    readonly name: string;
    readonly description: string;
    readonly parameterCount: number;
    readonly payloadCount: number;
    readonly eventCount: number;
    readonly conditionCount: number;
    readonly conditionSetCount: number;
    readonly actionCount: number;
    readonly ruleCount: number;
}

export interface DocumentContent
{
    readonly authoringModel: AuthoringModel;
    readonly summary: DocumentSummary;
    readonly validationMessages: readonly ValidationDiagnostic[];
    readonly validationErrors: boolean;
    readonly entityIdentifiers: ReadonlyMap <ModelCategory, readonly string[]>;
}

export interface EntitySummary
{
    readonly identifier: string;
    readonly name: string;
    readonly details: string;
}

export type BindingState = "CONCRETE" | "WILDCARD" | "OMITTED";

export interface BindingDraft
{
    readonly conditionIdentifier: string;
    readonly state: BindingState;
    readonly concreteText: string;
}

export interface EntityDraft
{
    readonly id: string;
    readonly name: string;
    readonly description: string;
    readonly parameterType: string;
    readonly enumerationValues: readonly string[];
    readonly parameterIds: readonly string[];
    readonly payloadId: string;
    readonly parameterId: string;
    readonly conditionOperator: string;
    readonly conditionValueText: string;
    readonly secondConditionValueText: string;
    readonly bindings: ReadonlyMap <string, BindingDraft>;
    readonly conditionIds: readonly string[];
    readonly predefinedConditions: boolean;
    readonly eventId: string;
    readonly conditionSetId: string;
    readonly actionId: string;
}

export type PayloadValueState = "OMITTED" | "NULL" | "CONCRETE";

export interface PayloadValueDraft
{
    readonly state: PayloadValueState;
    readonly text: string;
}
