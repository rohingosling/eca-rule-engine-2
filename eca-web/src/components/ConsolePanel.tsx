// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    Console Panel
// Version: 2.0.0
// Date:    2026-08-14
// Author:  Rohin Gosling
//
// Description:
//
//   Presents bounded structured Console entries with filters, follow-tail, clearing, navigation, and copy support.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { useEffect, useMemo, useRef, useState } from "react";
import type { KeyboardEvent } from "react";

import { text } from "../localization/messages";
import type { GuideContext } from "./NavigationTree";

export const MAXIMUM_CONSOLE_ENTRY_COUNT = 1_000;

export type ConsoleSeverity = "error" | "message" | "warning";

export interface ConsoleContext
{
    readonly entityIdentifier?: string;
    readonly label:             string;
    readonly route:             GuideContext;
}

export interface ConsoleEntry
{
    readonly code:        string;
    readonly context?:    ConsoleContext;
    readonly identifier:  string;
    readonly severity:    ConsoleSeverity;
    readonly source:      string;
    readonly text:        string;
    readonly timestamp:   string;
}

export interface ConsoleFilterState
{
    readonly error:   boolean;
    readonly message: boolean;
    readonly warning: boolean;
}

interface ConsolePanelProperties
{
    readonly entries:             readonly ConsoleEntry[];
    readonly filters:             ConsoleFilterState;
    readonly followTail:          boolean;
    readonly visible:             boolean;
    readonly onClear:             () => void;
    readonly onFiltersChange:     ( filters: ConsoleFilterState ) => void;
    readonly onFollowTailChange:  ( followTail: boolean ) => void;
    readonly onNavigateToContext: ( context: ConsoleContext ) => void;
}

export function appendConsoleEntry (
    entries: readonly ConsoleEntry[],
    entry: ConsoleEntry
): readonly ConsoleEntry[]
{
    return [ ...entries, entry ].slice ( -MAXIMUM_CONSOLE_ENTRY_COUNT );
}

export function consoleEntryFromMessage (
    message: string,
    sequence: number,
    explicitSeverity?: ConsoleSeverity
): ConsoleEntry
{
    const match = /^\[([^\]]+)\]\s*(.*)$/u.exec ( message );
    const source = match?.[ 1 ] ?? "Application";
    const entryText = match?.[ 2 ] ?? message;

    return createConsoleEntry ( source, entryText, sequence, explicitSeverity );
}

export function createConsoleEntry (
    source: string,
    entryText: string,
    sequence: number,
    explicitSeverity: ConsoleSeverity = "message",
    explicitCode?: string,
    context?: ConsoleContext
): ConsoleEntry
{
    const severity = explicitSeverity;
    const normalizedSource = source.toLocaleUpperCase ().replace ( /[^A-Z0-9]+/gu, "_" ).replace ( /^_|_$/gu, "" );

    return {
        code: explicitCode ?? `${normalizedSource || "APPLICATION"}_${severity.toLocaleUpperCase ()}`,
        context,
        identifier: `console-${sequence}`,
        severity,
        source,
        text: entryText,
        timestamp: new Date ().toISOString (),
    };
}

export function consoleAnnouncementForEntries (
    entries: readonly ConsoleEntry[],
    filters: ConsoleFilterState
): string
{
    const newestEntry = entries.at ( -1 );

    if ( newestEntry === undefined || !filters [ newestEntry.severity ] )
    {
        return "";
    }

    return `[${newestEntry.source}] ${newestEntry.text}`;
}

function formatTime ( timestamp: string ): string
{
    return new Date ( timestamp ).toLocaleTimeString ( [],
        {
            hour:   "2-digit",
            hour12: false,
            minute: "2-digit",
            second: "2-digit",
        }
    );
}

function formatEntryForCopy ( entry: ConsoleEntry ): string
{
    return [
        formatTime ( entry.timestamp ),
        entry.severity,
        entry.code,
        entry.source,
        entry.text,
    ].join ( "\t" );
}

function severitySymbol ( severity: ConsoleSeverity ): string
{
    return severity === "error" ? "E" : severity === "warning" ? "W" : "M";
}

export function ConsolePanel ( properties: ConsolePanelProperties )
{
    const [ selectedIdentifier, setSelectedIdentifier ] = useState <string | null> ( null );
    const bodyReference = useRef <HTMLDivElement> ( null );
    const rowReferences = useRef <Map <string, HTMLDivElement>> ( new Map () );
    const visibleEntries = useMemo (
        () => properties.entries.filter ( entry => properties.filters [ entry.severity ] ),
        [ properties.entries, properties.filters ]
    );

    useEffect ( () =>
    {
        if ( properties.visible && properties.followTail && bodyReference.current !== null )
        {
            bodyReference.current.scrollTop = bodyReference.current.scrollHeight;
        }
    }, [ properties.followTail, properties.visible, visibleEntries ] );

    const effectiveSelectedIdentifier = selectedIdentifier !== null
        && visibleEntries.some ( entry => entry.identifier === selectedIdentifier )
        ? selectedIdentifier
        : visibleEntries [ 0 ]?.identifier ?? null;

    function toggleFilter ( severity: keyof ConsoleFilterState ): void
    {
        properties.onFiltersChange ( { ...properties.filters, [ severity ]: !properties.filters [ severity ] } );
    }

    function copyEntry ( entry: ConsoleEntry ): void
    {
        void navigator.clipboard?.writeText ( formatEntryForCopy ( entry ) );
    }

    function selectRelativeEntry ( event: KeyboardEvent <HTMLDivElement>, offset: number ): void
    {
        const currentIndex = visibleEntries.findIndex ( entry => entry.identifier === effectiveSelectedIdentifier );
        const nextIndex = Math.min ( visibleEntries.length - 1, Math.max ( 0, currentIndex + offset ) );
        const nextEntry = visibleEntries [ nextIndex ];

        if ( nextEntry !== undefined )
        {
            event.preventDefault ();
            setSelectedIdentifier ( nextEntry.identifier );
            rowReferences.current.get ( nextEntry.identifier )?.focus ();
        }
    }

    function handleRowKeyDown ( event: KeyboardEvent <HTMLDivElement>, entry: ConsoleEntry ): void
    {
        if ( event.key === "ArrowDown" || event.key === "ArrowUp" )
        {
            selectRelativeEntry ( event, event.key === "ArrowDown" ? 1 : -1 );
        }
        else if ( event.key === "Home" || event.key === "End" )
        {
            const targetEntry = event.key === "Home" ? visibleEntries [ 0 ] : visibleEntries.at ( -1 );

            if ( targetEntry !== undefined )
            {
                event.preventDefault ();
                setSelectedIdentifier ( targetEntry.identifier );
                rowReferences.current.get ( targetEntry.identifier )?.focus ();
            }
        }
        else if ( event.key.toLocaleLowerCase () === "c" && ( event.ctrlKey || event.metaKey ) )
        {
            event.preventDefault ();
            copyEntry ( entry );
        }
        else if ( ( event.key === "Enter" || event.key === " " ) && entry.context !== undefined )
        {
            event.preventDefault ();
            properties.onNavigateToContext ( entry.context );
        }
    }

    return (
        <section aria-labelledby="console-title" className="console-panel">
            <header className="console-title-bar">
                <h2 id="console-title">{ text ( "web.console.title" ) }</h2>
                <div className="console-controls">
                    <label>
                        <input
                            checked={ properties.filters.message }
                            onChange={ () => toggleFilter ( "message" ) }
                            type="checkbox"
                        />
                        <span>{ text ( "web.console.filter.messages" ) }</span>
                    </label>
                    <label>
                        <input
                            checked={ properties.filters.warning }
                            onChange={ () => toggleFilter ( "warning" ) }
                            type="checkbox"
                        />
                        <span>{ text ( "web.console.filter.warnings" ) }</span>
                    </label>
                    <label>
                        <input
                            checked={ properties.filters.error }
                            onChange={ () => toggleFilter ( "error" ) }
                            type="checkbox"
                        />
                        <span>{ text ( "web.console.filter.errors" ) }</span>
                    </label>
                    <label>
                        <input
                            checked={ properties.followTail }
                            onChange={ event => properties.onFollowTailChange ( event.currentTarget.checked ) }
                            type="checkbox"
                        />
                        <span>{ text ( "web.console.follow.tail" ) }</span>
                    </label>
                    <button onClick={ properties.onClear } type="button">{ text ( "web.console.clear" ) }</button>
                </div>
            </header>
            <p className="visually-hidden" id="console-copy-hint">{ text ( "web.console.copy.hint" ) }</p>
            <div
                aria-colcount={ 6 }
                aria-describedby="console-copy-hint"
                aria-label={ text ( "web.console.title" ) }
                className="console-table"
                ref={ bodyReference }
                role="grid"
            >
                <div className="visually-hidden" role="row">
                    <span role="columnheader">{ text ( "web.console.column.time" ) }</span>
                    <span role="columnheader">{ text ( "web.console.column.severity" ) }</span>
                    <span role="columnheader">{ text ( "web.console.column.code" ) }</span>
                    <span role="columnheader">{ text ( "web.console.column.source" ) }</span>
                    <span role="columnheader">{ text ( "web.console.column.message" ) }</span>
                    <span role="columnheader">{ text ( "web.console.column.context" ) }</span>
                </div>
                { visibleEntries.length === 0
                    ? (
                        <div role="row">
                            <div aria-colspan={ 6 } className="console-empty" role="gridcell">
                                { text ( "web.console.empty" ) }
                            </div>
                        </div>
                    )
                    : visibleEntries.map ( ( entry, entryIndex ) =>
                    {
                        const selected = entry.identifier === effectiveSelectedIdentifier
                            || ( effectiveSelectedIdentifier === null && entryIndex === 0 );

                        return (
                            <div
                                aria-selected={ selected }
                                className={ `console-row console-row-${entry.severity}` }
                                key={ entry.identifier }
                                onClick={ () => setSelectedIdentifier ( entry.identifier ) }
                                onDoubleClick={ () =>
                                {
                                    if ( entry.context !== undefined )
                                    {
                                        properties.onNavigateToContext ( entry.context );
                                    }
                                } }
                                onKeyDown={ event => handleRowKeyDown ( event, entry ) }
                                ref={ element =>
                                {
                                    if ( element === null )
                                    {
                                        rowReferences.current.delete ( entry.identifier );
                                    }
                                    else
                                    {
                                        rowReferences.current.set ( entry.identifier, element );
                                    }
                                } }
                                role="row"
                                tabIndex={ selected ? 0 : -1 }
                            >
                                <span className="console-time" role="gridcell">{ formatTime ( entry.timestamp ) }</span>
                                <span className="console-severity" role="gridcell">
                                    <span aria-hidden="true" className="severity-symbol">
                                        { severitySymbol ( entry.severity ) }
                                    </span>
                                    <span>{ text ( `web.console.severity.${entry.severity}` ) }</span>
                                </span>
                                <code className="console-code" role="gridcell">{ entry.code }</code>
                                <span className="console-source" role="gridcell">{ entry.source }</span>
                                <span className="console-text" role="gridcell">{ entry.text }</span>
                                <span className="console-context" role="gridcell">
                                    { entry.context !== undefined && (
                                        <button
                                            onClick={ () => properties.onNavigateToContext ( entry.context! ) }
                                            type="button"
                                        >
                                            { entry.context.label }
                                        </button>
                                    ) }
                                </span>
                            </div>
                        );
                    } ) }
            </div>
            <div aria-atomic="true" aria-live="polite" className="visually-hidden" role="status">
                { consoleAnnouncementForEntries ( properties.entries, properties.filters ) }
            </div>
        </section>
    );
}
