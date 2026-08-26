// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    SimulatorPanel
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Renders typed event occurrence controls, evaluation results, latency, and model revision reconciliation.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { useEffect, useMemo, useState } from "react";

import { text } from "../localization/messages";
import type { MessageKey } from "../localization/messages";
import type { AuthoringModel, PayloadValueDraft, PayloadValueState } from "../model";
import type { EvaluatedOccurrence } from "../server";
import {
    defaultPayloadDrafts,
    simulatorEvents,
    type SimulatorEvent,
    type SimulatorPayloadField,
} from "../simulator";
import { GroupBox } from "./WorkspacePanels";

interface SimulatorPanelProperties
{
    readonly model: AuthoringModel;
    readonly modelValid: boolean;
    readonly operationRunning: boolean;
    readonly evaluation: EvaluatedOccurrence | null;
    readonly localRevision: string | null;
    readonly serverRevision: string | null;
    readonly onEvaluate: (
        event: SimulatorEvent,
        drafts: ReadonlyMap <string, PayloadValueDraft>
    ) => void;
}

function displayedValue ( value: string | number | null | undefined, fallbackKey: MessageKey ): string
{
    return value === null || value === undefined ? text ( fallbackKey ) : String ( value );
}

function ResultRow ( properties: { readonly identifier: string; readonly labelKey: MessageKey; readonly value: string } )
{
    const labelIdentifier = `${properties.identifier}-label`;

    return (
        <>
            <span id={ labelIdentifier }>{ text ( properties.labelKey ) }</span>
            <output aria-labelledby={ labelIdentifier } className="read-only-value">
                { properties.value }
            </output>
        </>
    );
}

function PayloadValueControl ( properties: {
    readonly disabled: boolean;
    readonly draft: PayloadValueDraft;
    readonly field: SimulatorPayloadField;
    readonly onChange: ( draft: PayloadValueDraft ) => void;
} )
{
    const fieldIdentifier = `simulation-${properties.field.identifier}`;
    const fieldLabelIdentifier = `${fieldIdentifier}-label`;
    const stateDescriptionIdentifier = `${fieldIdentifier}-state-description`;
    const valueDescriptionIdentifier = `${fieldIdentifier}-value-description`;
    const concreteDisabled = properties.disabled || properties.draft.state !== "CONCRETE";

    function changeState ( state: PayloadValueState ): void
    {
        properties.onChange ( { ...properties.draft, state } );
    }

    function changeText ( value: string ): void
    {
        properties.onChange ( { ...properties.draft, text: value } );
    }

    function valueControl ()
    {
        const commonProperties = {
            "aria-labelledby": `${fieldLabelIdentifier} ${valueDescriptionIdentifier}`,
            disabled: concreteDisabled,
            id: `${fieldIdentifier}-value`,
        };

        if ( properties.field.type === "BOOLEAN" )
        {
            return (
                <select
                    { ...commonProperties }
                    onChange={ event => changeText ( event.currentTarget.value ) }
                    value={ properties.draft.text }
                >
                    <option value="false">false</option>
                    <option value="true">true</option>
                </select>
            );
        }

        if ( properties.field.type === "ENUM" )
        {
            return (
                <select
                    { ...commonProperties }
                    onChange={ event => changeText ( event.currentTarget.value ) }
                    value={ properties.draft.text }
                >
                    { properties.field.enumerationValues.map ( value => (
                        <option key={ value } value={ value }>{ value }</option>
                    ) ) }
                </select>
            );
        }

        return (
            <input
                { ...commonProperties }
                inputMode={ properties.field.type === "INTEGER" ? "numeric" : undefined }
                onChange={ event => changeText ( event.currentTarget.value ) }
                type="text"
                value={ properties.draft.text }
            />
        );
    }

    return (
        <div className="contents">
            <span id={ fieldLabelIdentifier }>{ properties.field.name }</span>
            <div className="payload-control">
                <span className="visually-hidden" id={ stateDescriptionIdentifier }>
                    { text ( "web.simulator.payload.state" ) }
                </span>
                <select
                    aria-labelledby={ `${fieldLabelIdentifier} ${stateDescriptionIdentifier}` }
                    disabled={ properties.disabled }
                    id={ `${fieldIdentifier}-state` }
                    onChange={ event => changeState ( event.currentTarget.value as PayloadValueState ) }
                    value={ properties.draft.state }
                >
                    <option value="OMITTED">{ text ( "simulator.payload.state.omitted" ) }</option>
                    <option value="NULL">{ text ( "simulator.payload.state.null" ) }</option>
                    <option value="CONCRETE">{ text ( "simulator.payload.state.concrete" ) }</option>
                </select>
                <span className="visually-hidden" id={ valueDescriptionIdentifier }>
                    { text ( "web.simulator.payload.value" ) }
                </span>
                { valueControl () }
            </div>
        </div>
    );
}

export function SimulatorPanel ( properties: SimulatorPanelProperties )
{
    const events = useMemo ( () => simulatorEvents ( properties.model ), [ properties.model ] );
    const [ selectedEventIdentifier, setSelectedEventIdentifier ] = useState ( events [ 0 ]?.identifier ?? "" );
    const selectedEvent = events.find ( event => event.identifier === selectedEventIdentifier ) ?? events [ 0 ];
    const [ payloadDrafts, setPayloadDrafts ] = useState <ReadonlyMap <string, PayloadValueDraft>> (
        () => defaultPayloadDrafts ( selectedEvent )
    );

    useEffect (
        () =>
        {
            const retainedEvent = events.find ( event => event.identifier === selectedEventIdentifier );
            const nextEvent = retainedEvent ?? events [ 0 ];

            setSelectedEventIdentifier ( nextEvent?.identifier ?? "" );
            setPayloadDrafts ( defaultPayloadDrafts ( nextEvent ) );
        },
        [ events, properties.model ]
    );

    function selectEvent ( eventIdentifier: string ): void
    {
        const event = events.find ( candidate => candidate.identifier === eventIdentifier );

        setSelectedEventIdentifier ( eventIdentifier );
        setPayloadDrafts ( defaultPayloadDrafts ( event ) );
    }

    function changePayloadDraft ( parameterIdentifier: string, draft: PayloadValueDraft ): void
    {
        setPayloadDrafts ( currentDrafts =>
        {
            const replacementDrafts = new Map ( currentDrafts );

            replacementDrafts.set ( parameterIdentifier, draft );
            return replacementDrafts;
        } );
    }

    const unavailable = text ( "simulator.result.not.available" );
    const notApplicable = text ( "simulator.result.not.applicable" );
    const result = properties.evaluation;
    const localRevision = properties.localRevision ?? text ( "simulator.revision.unknown" );
    const serverRevision = result?.modelRevision
        ?? properties.serverRevision
        ?? text ( "simulator.revision.unknown" );
    const reconciliation = properties.localRevision === null || serverRevision === text ( "simulator.revision.unknown" )
        ? text ( "simulator.revision.not.compared" )
        : properties.localRevision === serverRevision
            ? text ( "simulator.revision.matches" )
            : text ( "simulator.revision.differs" );
    const availabilityMessage = !properties.modelValid
        ? text ( "simulator.model.invalid" )
        : events.length === 0
            ? text ( "simulator.no.events" )
            : selectedEvent?.payloadFields.length === 0
                ? text ( "simulator.payload.empty" )
                : "";
    const disabled = properties.operationRunning || !properties.modelValid;

    return (
        <form className="editor-content editor-form-page" onSubmit={ event => event.preventDefault () }>
            <div className="editor-scroll-content">
                <h2>{ text ( "simulator.heading" ) }</h2>
                <GroupBox legend={ text ( "simulator.occurrence.group" ) }>
                    <div className="form-grid">
                        <label htmlFor="simulation-event">{ text ( "simulator.event.label" ) }</label>
                        <select
                            disabled={ disabled || events.length === 0 }
                            id="simulation-event"
                            onChange={ event => selectEvent ( event.currentTarget.value ) }
                            value={ selectedEvent?.identifier ?? "" }
                        >
                            { events.map ( event => (
                                <option key={ event.identifier } value={ event.identifier }>
                                    { event.name } ({ event.identifier })
                                </option>
                            ) ) }
                        </select>
                        { selectedEvent?.payloadFields.map ( field => (
                            <PayloadValueControl
                                disabled={ disabled }
                                draft={ payloadDrafts.get ( field.identifier )
                                    ?? { state: "OMITTED", text: "" } }
                                field={ field }
                                key={ field.identifier }
                                onChange={ draft => changePayloadDraft ( field.identifier, draft ) }
                            />
                        ) ) }
                    </div>
                    { availabilityMessage.length > 0 && <p className="secondary-text">{ availabilityMessage }</p> }
                </GroupBox>
                <GroupBox legend={ text ( "simulator.result.group" ) }>
                    <div className="form-grid result-grid">
                        <ResultRow
                            identifier="result-outcome"
                            labelKey="simulator.result.outcome"
                            value={ result?.outcome ?? unavailable }
                        />
                        <ResultRow
                            identifier="result-action"
                            labelKey="simulator.result.action"
                            value={ result?.outcome === "ACTION" ? result.actionId ?? unavailable
                                : result?.outcome === "NO_ACTION" ? notApplicable : unavailable }
                        />
                        <ResultRow
                            identifier="result-rule"
                            labelKey="simulator.result.rule"
                            value={ result?.outcome === "ACTION" ? result.ruleId ?? unavailable
                                : result?.outcome === "NO_ACTION" ? notApplicable : unavailable }
                        />
                        <ResultRow
                            identifier="result-specificity"
                            labelKey="simulator.result.specificity"
                            value={ result?.outcome === "ACTION"
                                ? displayedValue ( result.specificity, "simulator.result.not.available" )
                                : result?.outcome === "NO_ACTION" ? notApplicable : unavailable }
                        />
                        <ResultRow
                            identifier="result-server-latency"
                            labelKey="simulator.result.server.latency"
                            value={ result === null ? unavailable : `${result.elapsedMicroseconds} µs` }
                        />
                        <ResultRow
                            identifier="result-round-trip"
                            labelKey="simulator.result.round.trip.latency"
                            value={ result === null ? unavailable : `${result.roundTripMicroseconds} µs` }
                        />
                    </div>
                </GroupBox>
                <GroupBox legend={ text ( "simulator.revision.group" ) }>
                    <div className="form-grid result-grid">
                        <ResultRow
                            identifier="revision-local"
                            labelKey="simulator.revision.local"
                            value={ localRevision }
                        />
                        <ResultRow
                            identifier="revision-server"
                            labelKey="simulator.revision.server"
                            value={ serverRevision }
                        />
                        <ResultRow
                            identifier="revision-status"
                            labelKey="simulator.revision.status"
                            value={ reconciliation }
                        />
                    </div>
                </GroupBox>
            </div>
            <div className="detail-button-panel">
                <button
                    disabled={ disabled || selectedEvent === undefined }
                    onClick={ () => selectedEvent !== undefined
                        && properties.onEvaluate ( selectedEvent, payloadDrafts ) }
                    type="button"
                >
                    { text ( "button.evaluate" ) }
                </button>
            </div>
        </form>
    );
}
