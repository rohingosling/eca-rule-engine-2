// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    Modal Dialog
// Version: 2.0.0
// Date:    2026-08-14
// Author:  Rohin Gosling
//
// Description:
//
//   Provides the shared Automata-style modal shell, focus containment, cancellation, and invoker restoration.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { useEffect, useLayoutEffect, useRef } from "react";
import type { KeyboardEvent, MouseEvent, ReactNode } from "react";

import { text } from "../localization/messages";

const FORM_LABEL_COLUMN_MARGIN_FACTOR = 1.1;

interface ModalDialogProperties
{
    readonly actions:               ReactNode;
    readonly children:              ReactNode;
    readonly className?:            string;
    readonly initialFocusSelector?: string;
    readonly onRequestClose:        () => void;
    readonly open:                  boolean;
    readonly title:                 string;
    readonly titleIdentifier:       string;
}

function focusableElements ( dialog: HTMLDialogElement ): HTMLElement[]
{
    return Array.from ( dialog.querySelectorAll <HTMLElement> (
        "button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), "
        + "a[href], [tabindex]:not([tabindex='-1'])"
    ) ).filter ( element => !element.hidden );
}

function isVisibleModalLabel ( label: HTMLElement, dialog: HTMLDialogElement ): boolean
{
    let ancestor: HTMLElement | null = label;

    while ( ancestor !== null )
    {
        const style = window.getComputedStyle ( ancestor );

        if (
            ancestor.hidden
            || ancestor.getAttribute ( "aria-hidden" ) === "true"
            || style.display === "none"
            || style.visibility === "hidden"
            || style.visibility === "collapse"
        )
        {
            return false;
        }

        if ( ancestor === dialog )
        {
            break;
        }

        ancestor = ancestor.parentElement;
    }

    return ancestor === dialog;
}

function alignModalFormValues ( dialog: HTMLDialogElement ): void
{
    dialog.style.removeProperty ( "--form-label-column-width" );
    const labels = Array.from ( dialog.querySelectorAll <HTMLElement> ( ".form-field-label-text" ) )
        .filter ( label => isVisibleModalLabel ( label, dialog ) );
    const labelWidths = labels.map ( label =>
    {
        const measurement = label.cloneNode ( true ) as HTMLElement;

        measurement.style.display       = "block";
        measurement.style.inlineSize    = "max-content";
        measurement.style.maxInlineSize = "none";
        measurement.style.position      = "absolute";
        measurement.style.visibility    = "hidden";
        measurement.style.whiteSpace    = "nowrap";
        dialog.append ( measurement );
        const width = measurement.getBoundingClientRect ().width;

        measurement.remove ();
        return width;
    } );
    const longestLabelWidth = Math.max ( 0, ...labelWidths );

    if ( longestLabelWidth > 0 )
    {
        dialog.style.setProperty (
            "--form-label-column-width",
            `${Math.ceil ( longestLabelWidth * FORM_LABEL_COLUMN_MARGIN_FACTOR )}px`
        );
    }
}

export function ModalDialog ( properties: ModalDialogProperties )
{
    const dialogReference = useRef <HTMLDialogElement> ( null );
    const invokerReference = useRef <HTMLElement | null> ( null );
    const previouslyOpen = useRef ( false );

    useLayoutEffect ( () =>
    {
        const dialog = dialogReference.current;

        if ( properties.open && dialog !== null && dialog.hasAttribute ( "open" ) )
        {
            alignModalFormValues ( dialog );
        }
    } );

    useEffect ( () =>
    {
        const dialog = dialogReference.current;

        if ( dialog === null )
        {
            return;
        }

        if ( properties.open && !previouslyOpen.current )
        {
            invokerReference.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;

            if ( typeof dialog.showModal === "function" )
            {
                dialog.showModal ();
            }
            else
            {
                dialog.setAttribute ( "open", "" );
            }

            alignModalFormValues ( dialog );

            const initialFocus = properties.initialFocusSelector === undefined
                ? focusableElements ( dialog ) [ 0 ]
                : dialog.querySelector <HTMLElement> ( properties.initialFocusSelector );

            initialFocus?.focus ();
        }
        else if ( !properties.open && previouslyOpen.current )
        {
            if ( dialog.open && typeof dialog.close === "function" )
            {
                dialog.close ();
            }
            else
            {
                dialog.removeAttribute ( "open" );
            }

            window.setTimeout ( () => invokerReference.current?.focus (), 0 );
        }

        previouslyOpen.current = properties.open;
    }, [ properties.initialFocusSelector, properties.open ] );

    useEffect ( () =>
    {
        const dialog = dialogReference.current;

        if ( !properties.open || dialog === null )
        {
            return;
        }

        const alignValues = () => alignModalFormValues ( dialog );

        window.addEventListener ( "resize", alignValues );
        return () => window.removeEventListener ( "resize", alignValues );
    }, [ properties.open ] );

    function containFocus ( event: KeyboardEvent <HTMLDialogElement> ): void
    {
        if ( event.key !== "Tab" )
        {
            return;
        }

        const focusable = focusableElements ( event.currentTarget );
        const first = focusable [ 0 ];
        const last = focusable.at ( -1 );

        if ( first === undefined || last === undefined )
        {
            event.preventDefault ();
        }
        else if ( event.shiftKey && document.activeElement === first )
        {
            event.preventDefault ();
            last.focus ();
        }
        else if ( !event.shiftKey && document.activeElement === last )
        {
            event.preventDefault ();
            first.focus ();
        }
    }

    function closeFromBackdrop ( event: MouseEvent <HTMLDialogElement> ): void
    {
        if ( event.target === event.currentTarget )
        {
            properties.onRequestClose ();
        }
    }

    return (
        <dialog
            aria-labelledby={ properties.titleIdentifier }
            className={ properties.className ?? "modal-dialog" }
            onCancel={ event =>
            {
                event.preventDefault ();
                properties.onRequestClose ();
            } }
            onClick={ closeFromBackdrop }
            onKeyDown={ containFocus }
            ref={ dialogReference }
        >
            <div className="dialog-window">
                <header className="dialog-title-bar">
                    <h2 id={ properties.titleIdentifier }>{ properties.title }</h2>
                    <button
                        aria-label={ text ( "web.dialog.close.label" ) }
                        className="dialog-close-button"
                        onClick={ properties.onRequestClose }
                        type="button"
                    >
                        { "\u00d7" }
                    </button>
                </header>
                <div className="dialog-content">{ properties.children }</div>
                <footer className="dialog-footer">{ properties.actions }</footer>
            </div>
        </dialog>
    );
}
