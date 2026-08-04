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

export function DiagnosticsPanel ( properties: { readonly messages: readonly string[] } )
{
    return (
        <GroupBox className="diagnostics-group" legend={ text ( "messages.group" ) }>
            <pre aria-live="polite" className="message-terminal" role="log" tabIndex={ 0 }>
                { properties.messages.join ( "\n" ) }
            </pre>
        </GroupBox>
    );
}

export function StatusBar ( properties: {
    readonly backgroundOperation: string | null;
    readonly connectionState: string;
    readonly dirty: boolean;
    readonly documentName: string;
    readonly onCancel: () => void;
    readonly serverTarget: string;
    readonly statusMessage: string;
    readonly validationErrorCount: number;
} )
{
    return (
        <footer aria-label={ text ( "web.application.status" ) } className="status-bar">
            <span>
                { text ( "status.document" ) }: { properties.documentName } —{ " " }
                { properties.dirty ? text ( "status.dirty" ) : text ( "status.clean" ) }
            </span>
            <span aria-hidden="true" className="status-separator" />
            <span>
                { text ( "status.validation" ) }:{ " " }
                { properties.validationErrorCount === 0
                    ? text ( "status.valid" )
                    : `${properties.validationErrorCount} ${text ( "status.errors" )}` }
            </span>
            <span aria-hidden="true" className="status-separator" />
            <span>{ text ( "web.status.target" ) }: { properties.serverTarget }</span>
            <span>{ text ( "status.connection" ) }: { properties.connectionState }</span>
            <span className="status-spacer" />
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
