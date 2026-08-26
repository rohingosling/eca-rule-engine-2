// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    WorkspacePanels
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Renders the presentation-only guide, diagnostics, group-box, and document/server status panels.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import type { ReactNode } from "react";

import { text } from "../localization/messages";
import type { MessageKey } from "../localization/messages";
import type { GuideContext } from "./NavigationTree";

interface GroupBoxProperties
{
    readonly children: ReactNode;
    readonly className?: string;
    readonly legend: string;
}

interface UserGuidePanelProperties
{
    readonly context: GuideContext;
    readonly theme: "light" | "dark";
}

interface GuideDefinition
{
    readonly accessibleEquationKey: MessageKey;
    readonly equationFile: string;
    readonly overviewKey: MessageKey;
    readonly stepsKey: MessageKey;
}

const GUIDE_DEFINITIONS: Record <GuideContext, GuideDefinition> =
{
    "model":
    {
        accessibleEquationKey: "user.guide.model.equation.accessible",
        equationFile: "model",
        overviewKey: "user.guide.model.overview",
        stepsKey: "user.guide.model.steps",
    },
    "parameters":
    {
        accessibleEquationKey: "user.guide.parameters.equation.accessible",
        equationFile: "parameters",
        overviewKey: "user.guide.parameters.overview",
        stepsKey: "user.guide.parameters.steps",
    },
    "payloads":
    {
        accessibleEquationKey: "user.guide.payloads.equation.accessible",
        equationFile: "payloads",
        overviewKey: "user.guide.payloads.overview",
        stepsKey: "user.guide.payloads.steps",
    },
    "events":
    {
        accessibleEquationKey: "user.guide.events.equation.accessible",
        equationFile: "events",
        overviewKey: "user.guide.events.overview",
        stepsKey: "user.guide.events.steps",
    },
    "conditions":
    {
        accessibleEquationKey: "user.guide.conditions.equation.accessible",
        equationFile: "conditions",
        overviewKey: "user.guide.conditions.overview",
        stepsKey: "user.guide.conditions.steps",
    },
    "condition-sets":
    {
        accessibleEquationKey: "user.guide.condition-sets.equation.accessible",
        equationFile: "condition-sets",
        overviewKey: "user.guide.condition-sets.overview",
        stepsKey: "user.guide.condition-sets.steps",
    },
    "actions":
    {
        accessibleEquationKey: "user.guide.actions.equation.accessible",
        equationFile: "actions",
        overviewKey: "user.guide.actions.overview",
        stepsKey: "user.guide.actions.steps",
    },
    "rules":
    {
        accessibleEquationKey: "user.guide.rules.equation.accessible",
        equationFile: "rules",
        overviewKey: "user.guide.rules.overview",
        stepsKey: "user.guide.rules.steps",
    },
    "simulator":
    {
        accessibleEquationKey: "user.guide.simulator.equation.accessible",
        equationFile: "simulator",
        overviewKey: "user.guide.simulator.overview",
        stepsKey: "user.guide.simulator.steps",
    },
};

export function GroupBox ( properties: GroupBoxProperties )
{
    const className = [ "group-box", properties.className ].filter ( Boolean ).join ( " " );

    return (
        <fieldset className={ className }>
            <legend>{ properties.legend }</legend>
            <div className="group-box-content">
                { properties.children }
            </div>
        </fieldset>
    );
}

export function UserGuidePanel ( properties: UserGuidePanelProperties )
{
    const guideDefinition = GUIDE_DEFINITIONS [ properties.context ];
    const darkSuffix = properties.theme === "dark" ? "-dark" : "";
    const equationPath = `${import.meta.env.BASE_URL}assets/equations/${guideDefinition.equationFile}${darkSuffix}.png`;

    return (
        <article className="user-guide-content">
            <h2>{ text ( "user.guide.overview.heading" ) }</h2>
            <p>{ text ( guideDefinition.overviewKey ) }</p>
            <figure className="equation-card">
                <img
                    alt={ text ( guideDefinition.accessibleEquationKey ) }
                    src={ equationPath }
                />
            </figure>
            <nav aria-label={ text ( "user.guide.title" ) } className="user-guide-links">
                <a
                    href={
                        "https://github.com/rohingosling/eca-rule-engine-2/blob/main/docs/technical-note/"
                        + "stateless-eca-rule-engine.pdf"
                    }
                    rel="noreferrer"
                    target="_blank"
                >
                    { text ( "user.guide.technical.note.link" ) }
                </a>
                <a
                    href="https://github.com/rohingosling/eca-rule-engine-2"
                    rel="noreferrer"
                    target="_blank"
                >
                    { text ( "user.guide.github.link" ) }
                </a>
            </nav>
            <h2>{ text ( "user.guide.what.to.do.heading" ) }</h2>
            <p className="guide-steps">{ text ( guideDefinition.stepsKey ) }</p>
        </article>
    );
}

export function StatusBar ( properties: {
    readonly actionCount: number;
    readonly backgroundOperation: string | null;
    readonly conditionCount: number;
    readonly conditionSetCount: number;
    readonly eventCount: number;
    readonly modelIdentifier: string;
    readonly onCancel: () => void;
    readonly parameterCount: number;
    readonly payloadCount: number;
    readonly ruleCount: number;
    readonly serverConnection: "connected" | "connecting" | "disconnected";
    readonly serverStatus: string;
    readonly statusMessage: string;
} )
{
    return (
        <footer aria-label={ text ( "web.application.status" ) } className="status-bar" tabIndex={ 0 }>
            <span>{ text ( "web.status.model.id" ) }: { properties.modelIdentifier }</span>
            <span>{ text ( "web.status.parameters" ) }: { properties.parameterCount }</span>
            <span>{ text ( "web.status.payloads" ) }: { properties.payloadCount }</span>
            <span>{ text ( "web.status.events" ) }: { properties.eventCount }</span>
            <span>{ text ( "web.status.conditions" ) }: { properties.conditionCount }</span>
            <span>{ text ( "web.status.condition.sets" ) }: { properties.conditionSetCount }</span>
            <span>{ text ( "web.status.actions" ) }: { properties.actionCount }</span>
            <span>{ text ( "web.status.rules" ) }: { properties.ruleCount }</span>
            <span className={ `connection-status connection-${properties.serverConnection}` }>
                <span aria-hidden="true" className="connection-symbol">
                    { properties.serverConnection === "connected" ? "●" : "○" }
                </span>
                { text ( "web.status.server" ) }:{ " " }
                <span className={ `connection-value connection-value-${properties.serverConnection}` }>
                    { properties.serverStatus }
                </span>
            </span>
            <span aria-live="polite">{ properties.statusMessage }</span>
            <progress
                aria-label={ text ( "web.progress.label" ) }
                aria-valuetext={ properties.backgroundOperation ?? undefined }
                hidden={ properties.backgroundOperation === null }
            />
            <button
                aria-label={ text ( "web.cancel.label" ) }
                disabled={ properties.backgroundOperation === null }
                hidden={ properties.backgroundOperation === null }
                onClick={ properties.onCancel }
                type="button"
            >
                { text ( "button.cancel" ) }
            </button>
        </footer>
    );
}
