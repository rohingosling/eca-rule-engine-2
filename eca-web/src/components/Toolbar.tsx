// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    Toolbar
// Version: 2.0.0
// Date:    2026-08-14
// Author:  Rohin Gosling
//
// Description:
//
//   Renders the fixed application command toolbar with roving focus, theme choices, and narrow-width overflow.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { useEffect, useRef, useState } from "react";
import type { KeyboardEvent } from "react";

import { text } from "../localization/messages";
import { Icon } from "./Icon";

export interface ToolbarChoice
{
    readonly checked:    boolean;
    readonly identifier: string;
    readonly label:      string;
    readonly onSelect:   () => void;
}

export type ToolbarEntry =
    | { readonly kind: "separator" }
    | {
        readonly choices?:   readonly ToolbarChoice[];
        readonly disabled?:  boolean;
        readonly icon:       string;
        readonly identifier: string;
        readonly kind:       "button";
        readonly label:      string;
        readonly onSelect?:  () => void;
        readonly pressed?:   boolean;
    };

interface ToolbarProperties
{
    readonly entries: readonly ToolbarEntry[];
}

export function Toolbar ( properties: ToolbarProperties )
{
    const buttons = properties.entries.filter (
        ( entry ): entry is Extract <ToolbarEntry, { kind: "button" }> => entry.kind === "button"
    );
    const firstEnabledIdentifier = buttons.find ( button => button.disabled !== true )?.identifier ?? "";
    const [ activeIdentifier, setActiveIdentifier ] = useState ( firstEnabledIdentifier );
    const [ openMenuIdentifier, setOpenMenuIdentifier ] = useState <string | null> ( null );
    const toolbarReference = useRef <HTMLDivElement> ( null );
    const buttonReferences = useRef <Map <string, HTMLButtonElement>> ( new Map () );
    const choiceReferences = useRef <Map <string, HTMLButtonElement>> ( new Map () );
    const effectiveActiveIdentifier = buttons.some (
        button => button.identifier === activeIdentifier && button.disabled !== true
    )
        ? activeIdentifier
        : firstEnabledIdentifier;

    useEffect ( () =>
    {
        function closeFromOutside ( event: PointerEvent ): void
        {
            if ( toolbarReference.current?.contains ( event.target as Node ) !== true )
            {
                setOpenMenuIdentifier ( null );
            }
        }

        document.addEventListener ( "pointerdown", closeFromOutside );

        return () => document.removeEventListener ( "pointerdown", closeFromOutside );
    }, [] );

    function openChoices ( entry: Extract <ToolbarEntry, { kind: "button" }> ): void
    {
        if ( entry.choices === undefined )
        {
            entry.onSelect?.();
            return;
        }

        const opening = openMenuIdentifier !== entry.identifier;

        setOpenMenuIdentifier ( opening ? entry.identifier : null );
        if ( opening )
        {
            window.setTimeout ( () =>
            {
                const selectedChoice = entry.choices?.find ( choice => choice.checked ) ?? entry.choices?.[ 0 ];

                if ( selectedChoice !== undefined )
                {
                    choiceReferences.current.get ( selectedChoice.identifier )?.focus ();
                }
            }, 0 );
        }
    }

    function handleButtonKeyDown (
        event: KeyboardEvent <HTMLButtonElement>,
        entry: Extract <ToolbarEntry, { kind: "button" }>
    ): void
    {
        if ( event.key === "ArrowDown" && entry.choices !== undefined )
        {
            event.preventDefault ();
            openChoices ( entry );
            return;
        }

        const enabledButtons = buttons.filter ( button => button.disabled !== true );
        const currentIndex = enabledButtons.findIndex ( button => button.identifier === entry.identifier );
        let nextIndex: number | null = null;

        if ( event.key === "ArrowLeft" )
        {
            nextIndex = currentIndex - 1;
        }
        else if ( event.key === "ArrowRight" )
        {
            nextIndex = currentIndex + 1;
        }
        else if ( event.key === "Home" )
        {
            nextIndex = 0;
        }
        else if ( event.key === "End" )
        {
            nextIndex = enabledButtons.length - 1;
        }

        if ( nextIndex !== null && enabledButtons.length > 0 )
        {
            event.preventDefault ();
            const normalizedIndex = ( nextIndex + enabledButtons.length ) % enabledButtons.length;
            const nextButton = enabledButtons [ normalizedIndex ];

            if ( nextButton !== undefined )
            {
                setOpenMenuIdentifier ( null );
                setActiveIdentifier ( nextButton.identifier );
                buttonReferences.current.get ( nextButton.identifier )?.focus ();
            }
        }
    }

    function handleChoiceKeyDown (
        event: KeyboardEvent <HTMLButtonElement>,
        entry: Extract <ToolbarEntry, { kind: "button" }>,
        choiceIndex: number
    ): void
    {
        if ( event.key === "Escape" )
        {
            event.preventDefault ();
            setOpenMenuIdentifier ( null );
            buttonReferences.current.get ( entry.identifier )?.focus ();
            return;
        }

        if ( ![ "ArrowDown", "ArrowUp", "Home", "End" ].includes ( event.key ) )
        {
            return;
        }

        event.preventDefault ();
        const choiceCount = entry.choices?.length ?? 1;
        let nextIndex = choiceIndex;

        if ( event.key === "ArrowDown" )
        {
            nextIndex = ( choiceIndex + 1 ) % choiceCount;
        }
        else if ( event.key === "ArrowUp" )
        {
            nextIndex = ( choiceIndex - 1 + choiceCount ) % choiceCount;
        }
        else if ( event.key === "Home" )
        {
            nextIndex = 0;
        }
        else
        {
            nextIndex = choiceCount - 1;
        }

        const nextChoice = entry.choices?.[ nextIndex ];

        if ( nextChoice !== undefined )
        {
            choiceReferences.current.get ( nextChoice.identifier )?.focus ();
        }
    }

    return (
        <nav aria-label={ text ( "web.toolbar.label" ) } className="toolbar-landmark">
            <div aria-label={ text ( "web.toolbar.label" ) } className="toolbar" ref={ toolbarReference } role="toolbar">
                <div className="toolbar-main">
                    { properties.entries.map ( ( entry, entryIndex ) =>
                    {
                        if ( entry.kind === "separator" )
                        {
                            return (
                                <span
                                    aria-hidden="true"
                                    className="toolbar-separator"
                                    key={ `separator-${entryIndex}` }
                                />
                            );
                        }

                        const menuOpen = openMenuIdentifier === entry.identifier;

                        return (
                            <div className="toolbar-entry" key={ entry.identifier }>
                                <button
                                    aria-expanded={ entry.choices === undefined ? undefined : menuOpen }
                                    aria-haspopup={ entry.choices === undefined ? undefined : "menu" }
                                    aria-label={ entry.label }
                                    aria-pressed={ entry.pressed }
                                    className="toolbar-button"
                                    data-toolbar-entry={ entry.identifier }
                                    disabled={ entry.disabled }
                                    onClick={ () => openChoices ( entry ) }
                                    onFocus={ () => setActiveIdentifier ( entry.identifier ) }
                                    onKeyDown={ event => handleButtonKeyDown ( event, entry ) }
                                    ref={ element =>
                                    {
                                        if ( element === null )
                                        {
                                            buttonReferences.current.delete ( entry.identifier );
                                        }
                                        else
                                        {
                                            buttonReferences.current.set ( entry.identifier, element );
                                        }
                                    } }
                                    tabIndex={ effectiveActiveIdentifier === entry.identifier ? 0 : -1 }
                                    title={ entry.label }
                                    type="button"
                                >
                                    <Icon name={ entry.icon } />
                                </button>
                                { menuOpen && entry.choices !== undefined && (
                                    <div aria-label={ entry.label } className="toolbar-choice-menu" role="menu">
                                        { entry.choices.map ( ( choice, choiceIndex ) => (
                                            <button
                                                aria-checked={ choice.checked }
                                                key={ choice.identifier }
                                                onClick={ () =>
                                                {
                                                    choice.onSelect ();
                                                    setOpenMenuIdentifier ( null );
                                                    buttonReferences.current.get ( entry.identifier )?.focus ();
                                                } }
                                                onKeyDown={ event => handleChoiceKeyDown ( event, entry, choiceIndex ) }
                                                ref={ element =>
                                                {
                                                    if ( element === null )
                                                    {
                                                        choiceReferences.current.delete ( choice.identifier );
                                                    }
                                                    else
                                                    {
                                                        choiceReferences.current.set ( choice.identifier, element );
                                                    }
                                                } }
                                                role="menuitemradio"
                                                type="button"
                                            >
                                                <span aria-hidden="true">{ choice.checked ? "✓" : "" }</span>
                                                <span>{ choice.label }</span>
                                            </button>
                                        ) ) }
                                    </div>
                                ) }
                            </div>
                        );
                    } ) }
                </div>
                <details className="toolbar-overflow">
                    <summary aria-label={ text ( "web.button.more" ) }>⋯</summary>
                    <div className="toolbar-overflow-menu">
                        { buttons.flatMap ( entry =>
                        {
                            if ( entry.choices !== undefined )
                            {
                                return entry.choices.map ( choice => (
                                    <button
                                        aria-pressed={ choice.checked }
                                        key={ choice.identifier }
                                        onClick={ choice.onSelect }
                                        type="button"
                                    >
                                        <Icon name={ entry.icon } />
                                        <span>{ entry.label }: { choice.label }</span>
                                    </button>
                                ) );
                            }

                            return [
                                <button
                                    disabled={ entry.disabled }
                                    key={ entry.identifier }
                                    onClick={ entry.onSelect }
                                    type="button"
                                >
                                    <Icon name={ entry.icon } />
                                    <span>{ entry.label }</span>
                                </button>,
                            ];
                        } ) }
                    </div>
                </details>
            </div>
        </nav>
    );
}
