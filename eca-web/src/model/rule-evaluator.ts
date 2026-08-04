// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    rule-evaluator
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Implements pure conjunctive matching, specificity selection, and lexicographic rule tie-breaking.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { immutableMap } from "./immutable-map";
import type {
    CompiledComparisonCondition,
    CompiledConditionSet,
    CompiledEventOccurrence,
    CompiledModel,
    CompiledRule,
    CompiledScalarValue,
    EvaluationResult,
} from "./compiled-types";

function compareValues ( firstValue: CompiledScalarValue, secondValue: CompiledScalarValue ): number
{
    return firstValue < secondValue ? -1 : firstValue > secondValue ? 1 : 0;
}

function comparisonMatches (
    condition: CompiledComparisonCondition,
    candidateValue: CompiledScalarValue
): boolean
{
    if ( typeof candidateValue !== typeof condition.firstOperand )
    {
        return false;
    }

    const firstComparison = compareValues ( candidateValue, condition.firstOperand );

    switch ( condition.operator )
    {
        case "EQUALS":
            return firstComparison === 0;

        case "NOT_EQUALS":
            return firstComparison !== 0;

        case "GREATER_THAN":
            return firstComparison > 0;

        case "GREATER_THAN_OR_EQUAL":
            return firstComparison >= 0;

        case "LESS_THAN":
            return firstComparison < 0;

        case "LESS_THAN_OR_EQUAL":
            return firstComparison <= 0;

        case "BETWEEN_EXCLUSIVE":
            return condition.secondOperand !== undefined
                && firstComparison > 0
                && compareValues ( candidateValue, condition.secondOperand ) < 0;

        case "BETWEEN_INCLUSIVE":
            return condition.secondOperand !== undefined
                && firstComparison >= 0
                && compareValues ( candidateValue, condition.secondOperand ) <= 0;
    }
}

export function conditionSetMatches (
    conditionSet: CompiledConditionSet,
    occurrence: CompiledEventOccurrence
): boolean
{
    for ( const [ parameterIdentifier, condition ] of conditionSet.bindings )
    {
        if ( condition.kind === "wildcard" )
        {
            continue;
        }

        const payloadValue = occurrence.payload.get ( parameterIdentifier );

        if ( payloadValue === undefined || payloadValue.kind !== "concrete" )
        {
            return false;
        }

        if ( condition.kind === "comparison" )
        {
            if ( !comparisonMatches ( condition, payloadValue.value ) )
            {
                return false;
            }
        }
        else if ( condition.value !== payloadValue.value )
        {
            return false;
        }
    }

    return true;
}

function createRuleIndex ( model: CompiledModel ): ReadonlyMap <string, readonly CompiledRule[]>
{
    const mutableIndex = new Map <string, CompiledRule[]> ();

    for ( const rule of model.ruleBase.rules )
    {
        const indexedRules = mutableIndex.get ( rule.event.id ) ?? [];

        indexedRules.push ( rule );
        mutableIndex.set ( rule.event.id, indexedRules );
    }

    return immutableMap (
        [ ...mutableIndex ].map ( ( [ eventIdentifier, rules ] ) =>
            [ eventIdentifier, Object.freeze ( [ ...rules ] ) ] as const
        )
    );
}

export class RuleEvaluator
{
    private readonly model: CompiledModel;
    private readonly rulesByEventIdentifier: ReadonlyMap <string, readonly CompiledRule[]>;

    public constructor ( model: CompiledModel )
    {
        this.model                  = model;
        this.rulesByEventIdentifier = createRuleIndex ( model );

        Object.freeze ( this );
    }

    public evaluate ( occurrence: CompiledEventOccurrence ): EvaluationResult
    {
        const candidateRules = this.rulesByEventIdentifier.get ( occurrence.event.id ) ?? [];
        let selectedRule: CompiledRule | undefined;

        for ( const rule of candidateRules )
        {
            if ( !conditionSetMatches ( rule.conditionSet, occurrence ) )
            {
                continue;
            }

            if (
                selectedRule === undefined
                || rule.specificity > selectedRule.specificity
                || (
                    rule.specificity === selectedRule.specificity
                    && rule.id < selectedRule.id
                )
            )
            {
                selectedRule = rule;
            }
        }

        if ( selectedRule === undefined )
        {
            return Object.freeze (
                {
                    outcome: "NO_ACTION",
                    modelRevision: this.model.revision,
                }
            );
        }

        return Object.freeze (
            {
                outcome: "ACTION",
                actionId: selectedRule.action.id,
                ruleId: selectedRule.id,
                specificity: selectedRule.specificity,
                modelRevision: this.model.revision,
            }
        );
    }
}
