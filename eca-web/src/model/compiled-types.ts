// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    compiled-types
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Defines the immutable framework-independent compiled model, occurrence, and evaluation-result contracts.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import type { ConditionOperator, ParameterType, ValidationDiagnostic } from "./types";

export type CompiledScalarValue = string | boolean | bigint;

export interface CompiledParameterDefinition
{
    readonly id: string;
    readonly type: ParameterType;
    readonly enumerationValues: readonly string[];
}

export interface CompiledPayloadDefinition
{
    readonly id: string;
    readonly parameters: ReadonlyMap <string, CompiledParameterDefinition>;
}

export interface CompiledEventDefinition
{
    readonly id: string;
    readonly payload: CompiledPayloadDefinition;
}

export interface CompiledConcreteCondition
{
    readonly kind: "concrete";
    readonly parameter: CompiledParameterDefinition;
    readonly value: CompiledScalarValue;
}

export interface CompiledComparisonCondition
{
    readonly kind: "comparison";
    readonly parameter: CompiledParameterDefinition;
    readonly operator: Exclude <ConditionOperator, "ANY">;
    readonly firstOperand: CompiledScalarValue;
    readonly secondOperand?: CompiledScalarValue;
}

export interface CompiledExplicitWildcard
{
    readonly kind: "wildcard";
    readonly parameter: CompiledParameterDefinition;
}

export type CompiledCondition =
    | CompiledConcreteCondition
    | CompiledComparisonCondition
    | CompiledExplicitWildcard;

export interface CompiledConditionSet
{
    readonly id: string;
    readonly bindings: ReadonlyMap <string, CompiledCondition>;
}

export interface CompiledActionDefinition
{
    readonly id: string;
}

export interface CompiledRule
{
    readonly id: string;
    readonly event: CompiledEventDefinition;
    readonly conditionSet: CompiledConditionSet;
    readonly action: CompiledActionDefinition;
    readonly specificity: number;
}

export interface CompiledRuleBase
{
    readonly rules: readonly CompiledRule[];
}

export interface CompiledModel
{
    readonly modelId: string;
    readonly revision: string;
    readonly parameters: ReadonlyMap <string, CompiledParameterDefinition>;
    readonly payloads: ReadonlyMap <string, CompiledPayloadDefinition>;
    readonly events: ReadonlyMap <string, CompiledEventDefinition>;
    readonly conditionSets: ReadonlyMap <string, CompiledConditionSet>;
    readonly actions: ReadonlyMap <string, CompiledActionDefinition>;
    readonly ruleBase: CompiledRuleBase;
}

export interface CompiledConcretePayloadValue
{
    readonly kind: "concrete";
    readonly parameter: CompiledParameterDefinition;
    readonly value: CompiledScalarValue;
}

export interface CompiledPresentNullPayloadValue
{
    readonly kind: "present-null";
    readonly parameter: CompiledParameterDefinition;
}

export type CompiledPayloadValue = CompiledConcretePayloadValue | CompiledPresentNullPayloadValue;

export interface CompiledEventOccurrence
{
    readonly event: CompiledEventDefinition;
    readonly payload: ReadonlyMap <string, CompiledPayloadValue>;
}

export interface ActionEvaluationResult
{
    readonly outcome: "ACTION";
    readonly actionId: string;
    readonly ruleId: string;
    readonly specificity: number;
    readonly modelRevision: string;
}

export interface NoActionEvaluationResult
{
    readonly outcome: "NO_ACTION";
    readonly modelRevision: string;
}

export type EvaluationResult = ActionEvaluationResult | NoActionEvaluationResult;

export interface CompiledRuleSummary
{
    readonly id: string;
    readonly eventId: string;
    readonly conditionSetId: string;
    readonly actionId: string;
    readonly specificity: number;
}

export interface CompiledModelSummary
{
    readonly modelId: string;
    readonly revision: string;
    readonly parameterIds: readonly string[];
    readonly payloadIds: readonly string[];
    readonly eventIds: readonly string[];
    readonly conditionSetIds: readonly string[];
    readonly actionIds: readonly string[];
    readonly rules: readonly CompiledRuleSummary[];
}

export class ModelCompilationError extends Error
{
    public readonly diagnostics: readonly ValidationDiagnostic[];

    public constructor ( diagnostics: readonly ValidationDiagnostic[] )
    {
        super ( "The authoring model is invalid and cannot be compiled." );

        this.name        = "ModelCompilationError";
        this.diagnostics = Object.freeze ( [ ...diagnostics ] );
    }
}

export class OccurrenceCompilationError extends Error
{
    public constructor ( message: string )
    {
        super ( message );

        this.name = "OccurrenceCompilationError";
    }
}
