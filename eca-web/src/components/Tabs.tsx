// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    Tabs
// Version: 2.0.0
// Date:    2026-08-25
// Author:  Rohin Gosling
//
// Description:
//
//   Provides the accessible shared tab presentation control.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import type { KeyboardEvent, ReactNode } from "react";

import { text } from "../localization/messages";

export type TabIdentifier = "editor" | "simulator";

interface TabDefinition <Identifier extends string>
{
    readonly identifier: Identifier;
    readonly label: string;
    readonly content: ReactNode;
}

interface TabsProperties <Identifier extends string>
{
    readonly accessibleLabel?: string;
    readonly activeTab: Identifier;
    readonly className?: string;
    readonly tabs: readonly TabDefinition <Identifier>[];
    readonly onSelect: ( tabIdentifier: Identifier ) => void;
}

export function Tabs <Identifier extends string> ( properties: TabsProperties <Identifier> )
{
    function moveFocus ( event: KeyboardEvent <HTMLButtonElement>, nextIndex: number ): void
    {
        const tabList = event.currentTarget.closest ( "[role='tablist']" );
        const tabButtons = Array.from ( tabList?.querySelectorAll <HTMLButtonElement> ( "[role='tab']" ) ?? [] );
        const nextButton = tabButtons [ nextIndex ];

        if ( nextButton === undefined )
        {
            return;
        }

        event.preventDefault ();
        nextButton.focus ();
        nextButton.click ();
    }

    function handleKeyDown ( event: KeyboardEvent <HTMLButtonElement>, tabIndex: number ): void
    {
        if ( event.key === "ArrowRight" )
        {
            moveFocus ( event, ( tabIndex + 1 ) % properties.tabs.length );
        }
        else if ( event.key === "ArrowLeft" )
        {
            moveFocus ( event, ( tabIndex - 1 + properties.tabs.length ) % properties.tabs.length );
        }
        else if ( event.key === "Home" )
        {
            moveFocus ( event, 0 );
        }
        else if ( event.key === "End" )
        {
            moveFocus ( event, properties.tabs.length - 1 );
        }
    }

    return (
        <section className={ [ "detail-tabs", properties.className ].filter ( Boolean ).join ( " " ) }>
            <div
                aria-label={ properties.accessibleLabel ?? text ( "web.workspace.views" ) }
                className="tab-list"
                role="tablist"
            >
                { properties.tabs.map (
                    ( tab, tabIndex ) =>
                    {
                        const selected = properties.activeTab === tab.identifier;

                        return (
                            <button
                                aria-controls={ `${tab.identifier}-panel` }
                                aria-selected={ selected }
                                className="tab-button"
                                id={ `${tab.identifier}-tab` }
                                key={ tab.identifier }
                                onClick={ () => properties.onSelect ( tab.identifier ) }
                                onKeyDown={ event => handleKeyDown ( event, tabIndex ) }
                                role="tab"
                                tabIndex={ selected ? 0 : -1 }
                                type="button"
                            >
                                { tab.label }
                            </button>
                        );
                    }
                ) }
            </div>
            { properties.tabs.map (
                tab =>
                {
                    const selected = properties.activeTab === tab.identifier;

                    return (
                        <div
                            aria-labelledby={ `${tab.identifier}-tab` }
                            className="tab-panel"
                            hidden={ !selected }
                            id={ `${tab.identifier}-panel` }
                            key={ tab.identifier }
                            role="tabpanel"
                            tabIndex={ 0 }
                        >
                            { tab.content }
                        </div>
                    );
                }
            ) }
        </section>
    );
}
