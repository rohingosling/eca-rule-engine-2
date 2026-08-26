// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    DocumentEditor
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Renders the committed model overview, seven category grids, and accessible entity editing forms.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { useEffect, useId, useRef, useState, type ReactNode } from "react";

import { text } from "../localization/messages";
import type { MessageKey } from "../localization/messages";
import {
    CONDITION_OPERATORS,
    PARAMETER_TYPES,
    conditionSetSpecificity,
    createEntitySummaries,
    createExistingEntityDraft,
    createNewEntityDraft,
    findEntityReferences,
    supportedConditionOperators,
    updateLegacyBinding,
    type AuthoringModel,
    type BindingDraft,
    type ConditionOperator,
    type DeleteResult,
    type DocumentContent,
    type EditResult,
    type EntityDraft,
    type FieldError,
    type ModelCategory,
    type ParameterDefinition,
} from "../model";
import type { GuideContext } from "./NavigationTree";
import { Icon } from "./Icon";
import { GroupBox } from "./WorkspacePanels";

interface DocumentEditorProperties
{
    readonly content: DocumentContent;
    readonly entityEditorRequestSequence: number;
    readonly onApplyEntity: (
        category: ModelCategory,
        originalEntityIdentifier: string | null,
        draft: EntityDraft
    ) => EditResult;
    readonly onApplyModelDetails: (
        modelIdentifier: string,
        name: string,
        description: string
    ) => EditResult;
    readonly onDeleteEntity: ( category: ModelCategory, entityIdentifier: string ) => DeleteResult;
    readonly onDuplicateEntity: ( category: ModelCategory, entityIdentifier: string ) => EditResult;
    readonly onMoveEntity: ( category: ModelCategory, entityIdentifier: string, offset: -1 | 1 ) => EditResult;
    readonly onSelectEntity: ( category: ModelCategory, entityIdentifier: string | null ) => void;
    readonly onValidate: () => void;
    readonly operationRunning: boolean;
    readonly selectedContext: Exclude <GuideContext, "simulator">;
    readonly selectedEntityIdentifier: string | null;
}

interface EntityFormState
{
    readonly draft: EntityDraft;
    readonly originalEntityIdentifier: string | null;
}

interface EntityFormProperties
{
    readonly category: ModelCategory;
    readonly errors: readonly FieldError[];
    readonly formState: EntityFormState;
    readonly model: AuthoringModel;
    readonly onCancel: () => void;
    readonly onChange: ( replacementDraft: EntityDraft ) => void;
    readonly onSubmit: () => void;
    readonly operationRunning: boolean;
}

interface ConditionValueControlProperties
{
    readonly identifier: string;
    readonly onChange: ( value: string ) => void;
    readonly parameter: ParameterDefinition | undefined;
    readonly value: string;
}

const CATEGORY_LABEL_KEYS: Record <ModelCategory, MessageKey> =
{
    parameters: "tree.parameters",
    payloads: "tree.payloads",
    events: "tree.events",
    conditions: "tree.conditions",
    "condition-sets": "tree.condition-sets",
    actions: "tree.actions",
    rules: "tree.rules",
};

const CONDITION_OPERATOR_LABEL_KEYS: Record <ConditionOperator, MessageKey> =
{
    EQUALS: "editor.condition.operator.equals",
    NOT_EQUALS: "editor.condition.operator.not.equals",
    GREATER_THAN: "editor.condition.operator.greater.than",
    GREATER_THAN_OR_EQUAL: "editor.condition.operator.greater.than.or.equal",
    LESS_THAN: "editor.condition.operator.less.than",
    LESS_THAN_OR_EQUAL: "editor.condition.operator.less.than.or.equal",
    BETWEEN_EXCLUSIVE: "editor.condition.operator.between.exclusive",
    BETWEEN_INCLUSIVE: "editor.condition.operator.between.inclusive",
    ANY: "editor.condition.operator.any",
};

function errorsForField ( errors: readonly FieldError[], field: string ): readonly FieldError[]
{
    return errors.filter ( error => error.field === field || error.field.startsWith ( `${field}.` ) );
}

function FieldControl ( properties: {
    readonly children: ReactNode;
    readonly controlIdentifiers: readonly string[];
    readonly errors: readonly FieldError[];
} )
{
    const errorDescriptionIdentifier = useId ();

    useEffect (
        () =>
        {
            const controls = properties.controlIdentifiers
                .map ( identifier => document.getElementById ( identifier ) )
                .filter ( ( control ): control is HTMLElement => control !== null );

            for ( const control of controls )
            {
                const descriptions = ( control.getAttribute ( "aria-describedby" ) ?? "" )
                    .split ( /\s+/ )
                    .filter ( identifier => identifier.length > 0 && identifier !== errorDescriptionIdentifier );

                if ( properties.errors.length > 0 )
                {
                    descriptions.push ( errorDescriptionIdentifier );
                    control.setAttribute ( "aria-invalid", "true" );
                }
                else
                {
                    control.removeAttribute ( "aria-invalid" );
                }

                if ( descriptions.length === 0 )
                {
                    control.removeAttribute ( "aria-describedby" );
                }
                else
                {
                    control.setAttribute ( "aria-describedby", descriptions.join ( " " ) );
                }
            }

            return () =>
            {
                for ( const control of controls )
                {
                    const descriptions = ( control.getAttribute ( "aria-describedby" ) ?? "" )
                        .split ( /\s+/ )
                        .filter (
                            identifier => identifier.length > 0 && identifier !== errorDescriptionIdentifier
                        );

                    control.removeAttribute ( "aria-invalid" );
                    if ( descriptions.length === 0 )
                    {
                        control.removeAttribute ( "aria-describedby" );
                    }
                    else
                    {
                        control.setAttribute ( "aria-describedby", descriptions.join ( " " ) );
                    }
                }
            };
        },
        [ errorDescriptionIdentifier, properties.controlIdentifiers, properties.errors.length ]
    );

    return (
        <div className="field-control">
            { properties.children }
            <div id={ errorDescriptionIdentifier }>
                { properties.errors.map (
                    ( error, index ) => (
                        <span className="field-error" key={ `${error.field}-${index}` } role="alert">
                            { error.message }
                        </span>
                    )
                ) }
            </div>
        </div>
    );
}

function ConditionValueControl ( properties: ConditionValueControlProperties )
{
    if ( properties.parameter?.type === "BOOLEAN" )
    {
        return (
            <select
                id={ properties.identifier }
                onChange={ event => properties.onChange ( event.currentTarget.value ) }
                value={ properties.value }
            >
                <option value="false">false</option>
                <option value="true">true</option>
            </select>
        );
    }

    if ( properties.parameter?.type === "ENUM" )
    {
        return (
            <select
                id={ properties.identifier }
                onChange={ event => properties.onChange ( event.currentTarget.value ) }
                value={ properties.value }
            >
                { properties.parameter.enumerationValues.map (
                    value => <option key={ value } value={ value }>{ value }</option>
                ) }
            </select>
        );
    }

    return (
        <input
            id={ properties.identifier }
            inputMode={ properties.parameter?.type === "INTEGER" ? "numeric" : undefined }
            onChange={ event => properties.onChange ( event.currentTarget.value ) }
            type="text"
            value={ properties.value }
        />
    );
}

function IdentityFields ( properties: {
    readonly draft: EntityDraft;
    readonly errors: readonly FieldError[];
    readonly onChange: ( replacementDraft: EntityDraft ) => void;
} )
{
    return (
        <GroupBox legend={ text ( "editor.identity.group" ) }>
            <div className="form-grid">
                <label htmlFor="entity-identifier">{ text ( "editor.field.id" ) }</label>
                <FieldControl
                    controlIdentifiers={ [ "entity-identifier" ] }
                    errors={ errorsForField ( properties.errors, "id" ) }
                >
                    <input
                        id="entity-identifier"
                        onChange={ event => properties.onChange (
                            { ...properties.draft, id: event.currentTarget.value }
                        ) }
                        type="text"
                        value={ properties.draft.id }
                    />
                </FieldControl>
                <label htmlFor="entity-name">{ text ( "editor.field.name" ) }</label>
                <FieldControl
                    controlIdentifiers={ [ "entity-name" ] }
                    errors={ errorsForField ( properties.errors, "name" ) }
                >
                    <input
                        id="entity-name"
                        onChange={ event => properties.onChange (
                            { ...properties.draft, name: event.currentTarget.value }
                        ) }
                        type="text"
                        value={ properties.draft.name }
                    />
                </FieldControl>
                <label htmlFor="entity-description">{ text ( "editor.field.description" ) }</label>
                <FieldControl
                    controlIdentifiers={ [ "entity-description" ] }
                    errors={ errorsForField ( properties.errors, "description" ) }
                >
                    <textarea
                        id="entity-description"
                        onChange={ event => properties.onChange (
                            { ...properties.draft, description: event.currentTarget.value }
                        ) }
                        rows={ 3 }
                        value={ properties.draft.description }
                    />
                </FieldControl>
            </div>
        </GroupBox>
    );
}

function toggleIdentifier (
    identifiers: readonly string[],
    identifier: string,
    selected: boolean
): readonly string[]
{
    return selected
        ? [ ...identifiers, identifier ]
        : identifiers.filter ( value => value !== identifier );
}

function CheckboxChoices ( properties: {
    readonly choices: readonly EntityDefinitionChoice[];
    readonly identifier: string;
    readonly legend: string;
    readonly onChange: ( identifier: string, selected: boolean ) => void;
    readonly selectedIdentifiers: readonly string[];
} )
{
    return (
        <fieldset className="choice-list" id={ properties.identifier }>
            <legend className="visually-hidden">{ properties.legend }</legend>
            { properties.choices.length === 0 && (
                <p className="secondary-text">{ text ( "editor.reference.none" ) }</p>
            ) }
            { properties.choices.map (
                choice => (
                    <label className="checkbox-choice" key={ choice.identifier }>
                        <input
                            checked={ properties.selectedIdentifiers.includes ( choice.identifier ) }
                            disabled={ choice.disabled }
                            onChange={ event => properties.onChange ( choice.identifier, event.currentTarget.checked ) }
                            type="checkbox"
                        />
                        <span>
                            { choice.label }
                            { choice.details.length > 0 && (
                                <span className="choice-details"> — { choice.details }</span>
                            ) }
                        </span>
                    </label>
                )
            ) }
        </fieldset>
    );
}

interface EntityDefinitionChoice
{
    readonly details: string;
    readonly disabled?: boolean;
    readonly identifier: string;
    readonly label: string;
}

function ParameterFields ( properties: EntityFormProperties )
{
    const draft = properties.formState.draft;

    return (
        <GroupBox legend={ text ( "editor.parameter.group" ) }>
            <div className="form-grid">
                <label htmlFor="parameter-type">{ text ( "editor.parameter.type" ) }</label>
                <FieldControl
                    controlIdentifiers={ [ "parameter-type" ] }
                    errors={ errorsForField ( properties.errors, "parameterType" ) }
                >
                    <select
                        id="parameter-type"
                        onChange={ event => properties.onChange (
                            {
                                ...draft,
                                parameterType: event.currentTarget.value,
                                enumerationValues: event.currentTarget.value === "ENUM"
                                    ? draft.enumerationValues
                                    : [],
                            }
                        ) }
                        value={ draft.parameterType }
                    >
                        { PARAMETER_TYPES.map ( parameterTypeValue => (
                            <option key={ parameterTypeValue } value={ parameterTypeValue }>
                                { parameterTypeValue }
                            </option>
                        ) ) }
                    </select>
                </FieldControl>
                { draft.parameterType === "ENUM" && (
                    <>
                        <label htmlFor="parameter-enumeration-values">
                            { text ( "editor.parameter.enum.values" ) }
                        </label>
                        <FieldControl
                            controlIdentifiers={ [ "parameter-enumeration-values" ] }
                            errors={ errorsForField ( properties.errors, "enumerationValues" ) }
                        >
                            <textarea
                                aria-describedby="parameter-enumeration-prompt"
                                id="parameter-enumeration-values"
                                onChange={ event => properties.onChange (
                                    {
                                        ...draft,
                                        enumerationValues: event.currentTarget.value.length === 0
                                            ? []
                                            : event.currentTarget.value.split ( /\r?\n/ ),
                                    }
                                ) }
                                rows={ 5 }
                                value={ draft.enumerationValues.join ( "\n" ) }
                            />
                            <span className="secondary-text" id="parameter-enumeration-prompt">
                                { text ( "editor.parameter.enum.prompt" ) }
                            </span>
                        </FieldControl>
                    </>
                ) }
            </div>
        </GroupBox>
    );
}

function PayloadFields ( properties: EntityFormProperties )
{
    const draft = properties.formState.draft;
    const choices = properties.model.parameters.map (
        parameter =>
        ({
            identifier: parameter.id,
            label: parameter.name,
            details: `${parameter.id} · ${parameter.type}`,
        })
    );

    return (
        <GroupBox legend={ text ( "editor.payload.group" ) }>
            <div className="form-grid form-grid-start">
                <span id="payload-parameters-label">{ text ( "editor.payload.parameters" ) }</span>
                <FieldControl
                    controlIdentifiers={ [ "payload-parameter-choices" ] }
                    errors={ errorsForField ( properties.errors, "parameterIds" ) }
                >
                    <CheckboxChoices
                        choices={ choices }
                        identifier="payload-parameter-choices"
                        legend={ text ( "editor.payload.parameters" ) }
                        onChange={ ( identifier, selected ) => properties.onChange (
                            {
                                ...draft,
                                parameterIds: toggleIdentifier ( draft.parameterIds, identifier, selected ),
                            }
                        ) }
                        selectedIdentifiers={ draft.parameterIds }
                    />
                </FieldControl>
            </div>
        </GroupBox>
    );
}

function EventFields ( properties: EntityFormProperties )
{
    const draft = properties.formState.draft;

    return (
        <GroupBox legend={ text ( "editor.event.group" ) }>
            <div className="form-grid">
                <label htmlFor="event-payload">{ text ( "editor.event.payload" ) }</label>
                <FieldControl
                    controlIdentifiers={ [ "event-payload" ] }
                    errors={ errorsForField ( properties.errors, "payloadId" ) }
                >
                    <select
                        id="event-payload"
                        onChange={ event => properties.onChange ( { ...draft, payloadId: event.currentTarget.value } ) }
                        value={ draft.payloadId }
                    >
                        <option value="">{ text ( "editor.reference.choose" ) }</option>
                        { properties.model.payloads.map (
                            payload => (
                                <option key={ payload.id } value={ payload.id }>
                                    { payload.name } ({ payload.id })
                                </option>
                            )
                        ) }
                    </select>
                </FieldControl>
            </div>
        </GroupBox>
    );
}

function ConditionFields ( properties: EntityFormProperties )
{
    const draft = properties.formState.draft;
    const parameter = properties.model.parameters.find ( value => value.id === draft.parameterId );
    const operators = supportedConditionOperators ( parameter );
    const selectedOperator = CONDITION_OPERATORS.includes ( draft.conditionOperator as ConditionOperator )
        ? draft.conditionOperator as ConditionOperator
        : "EQUALS";
    const operandCount = selectedOperator === "ANY"
        ? 0
        : selectedOperator === "BETWEEN_EXCLUSIVE" || selectedOperator === "BETWEEN_INCLUSIVE" ? 2 : 1;

    function changeParameter ( parameterIdentifier: string ): void
    {
        const replacementParameter = properties.model.parameters.find ( value => value.id === parameterIdentifier );
        const replacementOperators = supportedConditionOperators ( replacementParameter );
        const replacementOperator = replacementOperators.includes ( selectedOperator )
            ? selectedOperator
            : replacementOperators [ 0 ] ?? "EQUALS";
        const replacementValue = replacementParameter?.type === "INTEGER"
            ? "0"
            : replacementParameter?.type === "BOOLEAN"
                ? "false"
                : replacementParameter?.type === "ENUM"
                    ? replacementParameter.enumerationValues [ 0 ] ?? ""
                    : "";

        properties.onChange (
            {
                ...draft,
                parameterId: parameterIdentifier,
                conditionOperator: replacementOperator,
                conditionValueText: replacementValue,
                secondConditionValueText: replacementParameter?.type === "INTEGER" ? "1" : "",
            }
        );
    }

    return (
        <GroupBox legend={ text ( "editor.condition.group" ) }>
            <div className="form-grid">
                <label htmlFor="condition-parameter">{ text ( "editor.condition.parameter" ) }</label>
                <FieldControl
                    controlIdentifiers={ [ "condition-parameter" ] }
                    errors={ errorsForField ( properties.errors, "parameterId" ) }
                >
                    <select
                        id="condition-parameter"
                        onChange={ event => changeParameter ( event.currentTarget.value ) }
                        value={ draft.parameterId }
                    >
                        <option value="">{ text ( "editor.reference.choose" ) }</option>
                        { properties.model.parameters.map (
                            value => <option key={ value.id } value={ value.id }>{ value.name } ({ value.id })</option>
                        ) }
                    </select>
                </FieldControl>
                <label htmlFor="condition-operator">{ text ( "editor.condition.operator" ) }</label>
                <FieldControl
                    controlIdentifiers={ [ "condition-operator" ] }
                    errors={ errorsForField ( properties.errors, "conditionOperator" ) }
                >
                    <select
                        id="condition-operator"
                        onChange={ event => properties.onChange (
                            { ...draft, conditionOperator: event.currentTarget.value }
                        ) }
                        value={ selectedOperator }
                    >
                        { operators.map (
                            operator => (
                                <option key={ operator } value={ operator }>
                                    { text ( CONDITION_OPERATOR_LABEL_KEYS [ operator ] ) }
                                </option>
                            )
                        ) }
                    </select>
                </FieldControl>
                { operandCount >= 1 && (
                    <>
                        <label htmlFor="condition-value">{ text ( "editor.condition.value" ) }</label>
                        <FieldControl
                            controlIdentifiers={ [ "condition-value" ] }
                            errors={ errorsForField ( properties.errors, "conditionValueText" ) }
                        >
                            <ConditionValueControl
                                identifier="condition-value"
                                onChange={ value => properties.onChange ( { ...draft, conditionValueText: value } ) }
                                parameter={ parameter }
                                value={ draft.conditionValueText }
                            />
                        </FieldControl>
                    </>
                ) }
                { operandCount === 2 && (
                    <>
                        <label htmlFor="condition-second-value">{ text ( "editor.condition.second.value" ) }</label>
                        <FieldControl
                            controlIdentifiers={ [ "condition-second-value" ] }
                            errors={ errorsForField ( properties.errors, "secondConditionValueText" ) }
                        >
                            <ConditionValueControl
                                identifier="condition-second-value"
                                onChange={ value => properties.onChange (
                                    { ...draft, secondConditionValueText: value }
                                ) }
                                parameter={ parameter }
                                value={ draft.secondConditionValueText }
                            />
                        </FieldControl>
                    </>
                ) }
            </div>
        </GroupBox>
    );
}

function LegacyConditionChoices ( properties: EntityFormProperties )
{
    const draft = properties.formState.draft;

    function changeBinding ( conditionIdentifier: string, replacement: BindingDraft ): void
    {
        properties.onChange (
            {
                ...draft,
                bindings: updateLegacyBinding ( draft.bindings, conditionIdentifier, replacement ),
            }
        );
    }

    return (
        <div
            aria-labelledby="condition-selection-label"
            className="condition-binding-list"
            id="condition-selection"
            role="group"
        >
            { properties.model.conditions.map (
                condition =>
                {
                    const parameter = properties.model.parameters.find ( value => value.id === condition.parameterId );
                    const binding = draft.bindings.get ( condition.id )
                        ?? { conditionIdentifier: condition.id, state: "OMITTED", concreteText: "" };
                    const stateIdentifier = `binding-state-${condition.id}`;
                    const valueIdentifier = `binding-value-${condition.id}`;

                    return (
                        <div className="condition-binding" key={ condition.id }>
                            <label htmlFor={ stateIdentifier }>{ condition.name } ({ condition.id })</label>
                            <select
                                id={ stateIdentifier }
                                onChange={ event => changeBinding (
                                    condition.id,
                                    {
                                        ...binding,
                                        state: event.currentTarget.value as BindingDraft["state"],
                                    }
                                ) }
                                value={ binding.state }
                            >
                                <option value="OMITTED">{ text ( "editor.binding.omitted" ) }</option>
                                <option value="WILDCARD">{ text ( "editor.binding.wildcard" ) }</option>
                                <option value="CONCRETE">{ text ( "editor.binding.concrete" ) }</option>
                            </select>
                            { binding.state === "CONCRETE" && (
                                <>
                                    <label className="visually-hidden" htmlFor={ valueIdentifier }>
                                        { text ( "editor.condition.value" ) }: { condition.name }
                                    </label>
                                    <ConditionValueControl
                                        identifier={ valueIdentifier }
                                        onChange={ value => changeBinding (
                                            condition.id,
                                            { ...binding, concreteText: value }
                                        ) }
                                        parameter={ parameter }
                                        value={ binding.concreteText }
                                    />
                                </>
                            ) }
                            { errorsForField ( properties.errors, `bindings.${condition.id}` ).map (
                                ( error, index ) => (
                                    <span className="field-error" key={ index } role="alert">
                                        { error.message }
                                    </span>
                                )
                            ) }
                        </div>
                    );
                }
            ) }
        </div>
    );
}

function ConditionSetFields ( properties: EntityFormProperties )
{
    const draft = properties.formState.draft;
    const choices = properties.model.conditions.map (
        condition =>
        {
            const operator = CONDITION_OPERATORS.includes ( condition.operator as ConditionOperator )
                ? condition.operator as ConditionOperator
                : null;

            return {
                identifier: condition.id,
                label: condition.name,
                details: operator === null
                    ? text ( "editor.condition.set.configuration.required" )
                    : `${condition.parameterId} · ${text ( CONDITION_OPERATOR_LABEL_KEYS [ operator ] )}`,
                disabled: operator === null,
            };
        }
    );

    return (
        <GroupBox legend={ text ( "editor.condition.set.group" ) }>
            <div className="form-grid form-grid-start">
                <span>{ text ( "editor.condition.set.representation" ) }</span>
                <output className="read-only-value">
                    { draft.predefinedConditions
                        ? text ( "editor.condition.set.predefined" )
                        : text ( "editor.condition.set.legacy" ) }
                </output>
                <span id="condition-selection-label">{ text ( "editor.condition.set.conditions" ) }</span>
                <FieldControl
                    controlIdentifiers={ [ "condition-selection" ] }
                    errors={ errorsForField ( properties.errors, "conditionIds" ) }
                >
                    { draft.predefinedConditions
                        ? (
                            <CheckboxChoices
                                choices={ choices }
                                identifier="condition-selection"
                                legend={ text ( "editor.condition.set.conditions" ) }
                                onChange={ ( identifier, selected ) => properties.onChange (
                                    {
                                        ...draft,
                                        conditionIds: toggleIdentifier ( draft.conditionIds, identifier, selected ),
                                    }
                                ) }
                                selectedIdentifiers={ draft.conditionIds }
                            />
                        )
                        : <LegacyConditionChoices { ...properties } /> }
                </FieldControl>
                <span id="condition-specificity-label">{ text ( "editor.condition.set.specificity" ) }</span>
                <output aria-labelledby="condition-specificity-label" className="read-only-value">
                    { conditionSetSpecificity ( properties.model, draft ) }
                </output>
            </div>
        </GroupBox>
    );
}

function RuleFields ( properties: EntityFormProperties )
{
    const draft = properties.formState.draft;
    const selectors: readonly {
        readonly field: "actionId" | "conditionSetId" | "eventId";
        readonly identifier: string;
        readonly labelKey: MessageKey;
        readonly values: readonly EntityDefinitionChoice[];
    }[] =
    [
        {
            field: "eventId",
            identifier: "rule-event",
            labelKey: "editor.rule.event",
            values: properties.model.events.map (
                value => ({ identifier: value.id, label: value.name, details: value.id })
            ),
        },
        {
            field: "conditionSetId",
            identifier: "rule-condition-set",
            labelKey: "editor.rule.condition.set",
            values: properties.model.conditionSets.map (
                value => ({ identifier: value.id, label: value.name, details: value.id })
            ),
        },
        {
            field: "actionId",
            identifier: "rule-action",
            labelKey: "editor.rule.action",
            values: properties.model.actions.map (
                value => ({ identifier: value.id, label: value.name, details: value.id })
            ),
        },
    ];

    return (
        <GroupBox legend={ text ( "editor.rule.group" ) }>
            <div className="form-grid">
                { selectors.map (
                    selector => (
                        <div className="contents" key={ selector.field }>
                            <label htmlFor={ selector.identifier }>{ text ( selector.labelKey ) }</label>
                            <FieldControl
                                controlIdentifiers={ [ selector.identifier ] }
                                errors={ errorsForField ( properties.errors, selector.field ) }
                            >
                                <select
                                    id={ selector.identifier }
                                    onChange={ event => properties.onChange (
                                        { ...draft, [ selector.field ]: event.currentTarget.value }
                                    ) }
                                    value={ draft [ selector.field ] }
                                >
                                    <option value="">{ text ( "editor.reference.choose" ) }</option>
                                    { selector.values.map (
                                        value => (
                                            <option key={ value.identifier } value={ value.identifier }>
                                                { value.label } ({ value.identifier })
                                            </option>
                                        )
                                    ) }
                                </select>
                            </FieldControl>
                        </div>
                    )
                ) }
            </div>
        </GroupBox>
    );
}

function CategorySpecificFields ( properties: EntityFormProperties )
{
    switch ( properties.category )
    {
        case "parameters":     return <ParameterFields { ...properties } />;
        case "payloads":       return <PayloadFields { ...properties } />;
        case "events":         return <EventFields { ...properties } />;
        case "conditions":     return <ConditionFields { ...properties } />;
        case "condition-sets": return <ConditionSetFields { ...properties } />;
        case "rules":          return <RuleFields { ...properties } />;
        case "actions":
            return (
                <GroupBox legend={ text ( "editor.action.group" ) }>
                    <p className="secondary-text">{ text ( "editor.action.explanation" ) }</p>
                </GroupBox>
            );
    }
}

function EntityForm ( properties: EntityFormProperties )
{
    const formReference = useRef <HTMLFormElement> ( null );
    const categoryLabel = text ( CATEGORY_LABEL_KEYS [ properties.category ] );

    useEffect (
        () =>
        {
            if ( properties.errors.length > 0 )
            {
                window.setTimeout (
                    () => formReference.current?.querySelector <HTMLElement> ( "[aria-invalid='true']" )?.focus (),
                    0
                );
            }
        },
        [ properties.errors ]
    );

    return (
        <form
            className="editor-content editor-form-page"
            onSubmit={ event => { event.preventDefault (); properties.onSubmit (); } }
            ref={ formReference }
        >
            <div className="editor-scroll-content">
                <h2>
                    { properties.formState.originalEntityIdentifier === null
                        ? categoryLabel
                        : `${categoryLabel} — ${properties.formState.originalEntityIdentifier}` }
                </h2>
                <IdentityFields
                    draft={ properties.formState.draft }
                    errors={ properties.errors }
                    onChange={ properties.onChange }
                />
                <CategorySpecificFields { ...properties } />
            </div>
            <div className="detail-button-panel form-actions">
                <button disabled={ properties.operationRunning } type="submit">{ text ( "button.apply" ) }</button>
                <button onClick={ properties.onCancel } type="button">{ text ( "button.cancel" ) }</button>
            </div>
        </form>
    );
}

function ModelOverview ( properties: DocumentEditorProperties )
{
    const model = properties.content.authoringModel;
    const [ modelIdentifier, setModelIdentifier ] = useState ( model.modelId );
    const [ modelName, setModelName ] = useState ( model.name );
    const [ modelDescription, setModelDescription ] = useState ( model.description );
    const [ errors, setErrors ] = useState <readonly FieldError[]> ( [] );
    const formReference = useRef <HTMLFormElement> ( null );

    useEffect (
        () =>
        {
            setModelIdentifier ( model.modelId );
            setModelName ( model.name );
            setModelDescription ( model.description );
            setErrors ( [] );
        },
        [ model ]
    );

    useEffect (
        () =>
        {
            if ( errors.length > 0 )
            {
                window.setTimeout (
                    () => formReference.current?.querySelector <HTMLElement> ( "[aria-invalid='true']" )?.focus (),
                    0
                );
            }
        },
        [ errors ]
    );

    function applyDetails (): void
    {
        const result = properties.onApplyModelDetails ( modelIdentifier, modelName, modelDescription );

        setErrors ( result.errors );
    }

    return (
        <form
            className="editor-content editor-form-page"
            onSubmit={ event => { event.preventDefault (); applyDetails (); } }
            ref={ formReference }
        >
            <div className="editor-scroll-content">
                <h2>{ text ( "tree.model" ) }</h2>
                <GroupBox legend={ text ( "model.overview.group" ) }>
                    <div className="form-grid">
                        <label htmlFor="model-identifier">{ text ( "model.id.label" ) }</label>
                        <FieldControl
                            controlIdentifiers={ [ "model-identifier" ] }
                            errors={ errorsForField ( errors, "modelId" ) }
                        >
                            <input
                                id="model-identifier"
                                onChange={ event => setModelIdentifier ( event.currentTarget.value ) }
                                type="text"
                                value={ modelIdentifier }
                            />
                        </FieldControl>
                        <label htmlFor="model-name">{ text ( "model.name.label" ) }</label>
                        <FieldControl
                            controlIdentifiers={ [ "model-name" ] }
                            errors={ errorsForField ( errors, "modelName" ) }
                        >
                            <input
                                id="model-name"
                                onChange={ event => setModelName ( event.currentTarget.value ) }
                                type="text"
                                value={ modelName }
                            />
                        </FieldControl>
                        <label htmlFor="model-description">{ text ( "model.description.label" ) }</label>
                        <FieldControl
                            controlIdentifiers={ [ "model-description" ] }
                            errors={ errorsForField ( errors, "modelDescription" ) }
                        >
                            <textarea
                                id="model-description"
                                onChange={ event => setModelDescription ( event.currentTarget.value ) }
                                rows={ 3 }
                                value={ modelDescription }
                            />
                        </FieldControl>
                    </div>
                </GroupBox>
            </div>
            <div className="detail-button-panel">
                <button disabled={ properties.operationRunning } type="submit">
                    { text ( "button.apply.model.details" ) }
                </button>
                <button disabled={ properties.operationRunning } onClick={ properties.onValidate } type="button">
                    { text ( "button.validate" ) }
                </button>
            </div>
        </form>
    );
}

function CategoryOverview ( properties: DocumentEditorProperties & { readonly category: ModelCategory } )
{
    const model = properties.content.authoringModel;
    const summaries = createEntitySummaries ( model, properties.category );
    const [ formState, setFormState ] = useState <EntityFormState | null> ( null );
    const [ errors, setErrors ] = useState <readonly FieldError[]> ( [] );
    const selectedIdentifier = properties.selectedEntityIdentifier;
    const selectedIndex = summaries.findIndex ( summary => summary.identifier === selectedIdentifier );
    const references = selectedIdentifier === null
        ? []
        : findEntityReferences ( model, properties.category, selectedIdentifier );

    useEffect (
        () =>
        {
            setFormState ( null );
            setErrors ( [] );
        },
        [ model, properties.category ]
    );

    useEffect (
        () =>
        {
            if ( properties.entityEditorRequestSequence === 0 || selectedIdentifier === null )
            {
                return;
            }

            setErrors ( [] );
            setFormState (
                {
                    draft: createExistingEntityDraft ( model, properties.category, selectedIdentifier ),
                    originalEntityIdentifier: selectedIdentifier,
                }
            );
        },
        [ properties.entityEditorRequestSequence ]
    );

    function addEntity (): void
    {
        setErrors ( [] );
        setFormState ( { draft: createNewEntityDraft ( model, properties.category ), originalEntityIdentifier: null } );
    }

    function editEntity (): void
    {
        if ( selectedIdentifier === null )
        {
            return;
        }

        setErrors ( [] );
        setFormState (
            {
                draft: createExistingEntityDraft ( model, properties.category, selectedIdentifier ),
                originalEntityIdentifier: selectedIdentifier,
            }
        );
    }

    function applyEntity (): void
    {
        if ( formState === null )
        {
            return;
        }

        const result = properties.onApplyEntity (
            properties.category,
            formState.originalEntityIdentifier,
            formState.draft
        );

        setErrors ( result.errors );
        if ( result.model !== null )
        {
            setFormState ( null );
        }
    }

    function duplicateSelectedEntity (): void
    {
        if ( selectedIdentifier === null )
        {
            return;
        }

        const result = properties.onDuplicateEntity ( properties.category, selectedIdentifier );

        setErrors ( result.errors );
    }

    function deleteSelectedEntity (): void
    {
        if ( selectedIdentifier === null )
        {
            return;
        }

        const result = properties.onDeleteEntity ( properties.category, selectedIdentifier );

        setErrors (
            result.references.map ( reference => ({ field: "delete", message: reference }) )
        );
    }

    function moveSelectedEntity ( offset: -1 | 1 ): void
    {
        if ( selectedIdentifier === null )
        {
            return;
        }

        const result = properties.onMoveEntity ( properties.category, selectedIdentifier, offset );

        setErrors ( result.errors );
    }

    if ( formState !== null )
    {
        return (
            <EntityForm
                category={ properties.category }
                errors={ errors }
                formState={ formState }
                model={ model }
                onCancel={ () => { setFormState ( null ); setErrors ( [] ); } }
                onChange={ replacementDraft =>
                {
                    setFormState ( { ...formState, draft: replacementDraft } );
                    setErrors ( [] );
                } }
                onSubmit={ applyEntity }
                operationRunning={ properties.operationRunning }
            />
        );
    }

    const categoryLabel = text ( CATEGORY_LABEL_KEYS [ properties.category ] );

    function selectGridRow ( rowIndex: number ): void
    {
        const summary = summaries [ rowIndex ];

        if ( summary === undefined )
        {
            return;
        }

        properties.onSelectEntity ( properties.category, summary.identifier );
        window.setTimeout (
            () => document.querySelector <HTMLElement> (
                `[data-entity-row='${summary.identifier}']`
            )?.focus (),
            0
        );
    }

    return (
        <section className="editor-content editor-list-page">
            <div className="editor-scroll-content">
                <h2>{ categoryLabel }</h2>
                <GroupBox legend={ categoryLabel }>
                    <div className="entity-table-container">
                    <table aria-label={ categoryLabel } aria-rowcount={ summaries.length + 1 } role="grid">
                        <caption className="visually-hidden">{ categoryLabel }</caption>
                        <thead>
                            <tr>
                                <th scope="col">{ text ( "editor.column.id" ) }</th>
                                <th scope="col">{ text ( "editor.column.name" ) }</th>
                                <th scope="col">{ text ( "editor.column.details" ) }</th>
                            </tr>
                        </thead>
                        <tbody>
                            { summaries.length === 0 && (
                                <tr><td className="empty-table" colSpan={ 3 }>{ text ( "editor.grid.empty" ) }</td></tr>
                            ) }
                            { summaries.map (
                                ( summary, summaryIndex ) => (
                                    <tr
                                        aria-selected={ selectedIdentifier === summary.identifier }
                                        className={ selectedIdentifier === summary.identifier
                                            ? "entity-row-selected"
                                            : undefined }
                                        data-entity-row={ summary.identifier }
                                        key={ summary.identifier }
                                        onClick={ () => properties.onSelectEntity (
                                            properties.category,
                                            summary.identifier
                                        ) }
                                        onDoubleClick={ () =>
                                        {
                                            properties.onSelectEntity ( properties.category, summary.identifier );
                                            setErrors ( [] );
                                            setFormState (
                                                {
                                                    draft: createExistingEntityDraft (
                                                        model,
                                                        properties.category,
                                                        summary.identifier
                                                    ),
                                                    originalEntityIdentifier: summary.identifier,
                                                }
                                            );
                                        } }
                                        onKeyDown={ event =>
                                        {
                                            if ( event.key === "ArrowDown" )
                                            {
                                                event.preventDefault ();
                                                selectGridRow ( Math.min ( summaries.length - 1, summaryIndex + 1 ) );
                                            }
                                            else if ( event.key === "ArrowUp" )
                                            {
                                                event.preventDefault ();
                                                selectGridRow ( Math.max ( 0, summaryIndex - 1 ) );
                                            }
                                            else if ( event.key === "Home" )
                                            {
                                                event.preventDefault ();
                                                selectGridRow ( 0 );
                                            }
                                            else if ( event.key === "End" )
                                            {
                                                event.preventDefault ();
                                                selectGridRow ( summaries.length - 1 );
                                            }
                                            else if ( event.key === "Enter" || event.key === " " )
                                            {
                                                event.preventDefault ();
                                                properties.onSelectEntity ( properties.category, summary.identifier );
                                            }
                                        } }
                                        tabIndex={ selectedIdentifier === null
                                            ? summaryIndex === 0 ? 0 : -1
                                            : selectedIdentifier === summary.identifier ? 0 : -1 }
                                    >
                                        <td>{ summary.identifier }</td>
                                        <td>{ summary.name }</td>
                                        <td>{ summary.details }</td>
                                    </tr>
                                )
                            ) }
                        </tbody>
                    </table>
                    </div>
                    { selectedIdentifier !== null && (
                        <div className="reference-preview" id="entity-reference-preview">
                            <strong>{ text ( "editor.references.heading" ) }:</strong>{ " " }
                            { references.length === 0 ? text ( "editor.references.none" ) : references.join ( ", " ) }
                        </div>
                    ) }
                    { errorsForField ( errors, "delete" ).map (
                        ( error, index ) => <p className="field-error" key={ index } role="alert">{ error.message }</p>
                    ) }
                </GroupBox>
            </div>
            <div className="detail-button-panel">
                <button
                    disabled={ properties.operationRunning || selectedIndex <= 0 }
                    onClick={ () => moveSelectedEntity ( -1 ) }
                    type="button"
                >
                    <Icon name="ic_fluent_arrow_up_20_regular.svg" />
                    { text ( "web.button.move.up" ) }
                </button>
                <button
                    disabled={ properties.operationRunning || selectedIndex < 0 || selectedIndex >= summaries.length - 1 }
                    onClick={ () => moveSelectedEntity ( 1 ) }
                    type="button"
                >
                    <Icon name="ic_fluent_arrow_down_20_regular.svg" />
                    { text ( "web.button.move.down" ) }
                </button>
                <button disabled={ properties.operationRunning } onClick={ addEntity } type="button">
                    <Icon name="ic_fluent_add_20_regular.svg" />
                    { text ( "button.add" ) }
                </button>
                <button
                    disabled={ properties.operationRunning || selectedIdentifier === null }
                    onClick={ duplicateSelectedEntity }
                    type="button"
                >
                    <Icon name="ic_fluent_copy_add_20_regular.svg" />
                    { text ( "button.duplicate" ) }
                </button>
                <button
                    aria-describedby={ selectedIdentifier === null ? undefined : "entity-reference-preview" }
                    disabled={ properties.operationRunning || selectedIdentifier === null || references.length > 0 }
                    onClick={ deleteSelectedEntity }
                    type="button"
                >
                    <Icon name="ic_fluent_delete_20_regular.svg" />
                    { text ( "button.delete" ) }
                </button>
                <button
                    disabled={ properties.operationRunning || selectedIdentifier === null }
                    onClick={ editEntity }
                    type="button"
                >
                    <Icon name="ic_fluent_edit_20_regular.svg" />
                    { text ( "button.edit" ) }
                </button>
            </div>
        </section>
    );
}

export function DocumentEditor ( properties: DocumentEditorProperties )
{
    if ( properties.selectedContext === "model" )
    {
        return <ModelOverview { ...properties } />;
    }

    return <CategoryOverview { ...properties } category={ properties.selectedContext } />;
}
