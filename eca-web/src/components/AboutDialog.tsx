// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    AboutDialog
// Version: 2.0.0
// Date:    2026-08-04
// Author:  Rohin Gosling
//
// Description:
//
//   Renders the desktop-parity application identity dialog for the web client.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { useEffect, useRef } from "react";

import { text } from "../localization/messages";

interface AboutDialogProperties
{
    readonly open: boolean;
    readonly onClose: () => void;
}

export function AboutDialog ( properties: AboutDialogProperties )
{
    const dialogReference = useRef <HTMLDialogElement> ( null );
    const okButtonReference = useRef <HTMLButtonElement> ( null );
    const returnFocusReference = useRef <HTMLElement | null> ( null );

    useEffect (
        () =>
        {
            const dialog = dialogReference.current;

            if ( properties.open && dialog !== null && !dialog.open )
            {
                returnFocusReference.current = document.activeElement instanceof HTMLElement
                    ? document.activeElement
                    : null;
                dialog.showModal ();
                okButtonReference.current?.focus ();
            }
            else if ( !properties.open && dialog?.open )
            {
                dialog.close ();
                window.setTimeout ( () => returnFocusReference.current?.focus (), 0 );
            }
        },
        [ properties.open ]
    );

    return (
        <dialog
            aria-labelledby="about-dialog-title"
            className="about-dialog"
            onCancel={ event =>
            {
                event.preventDefault ();
                properties.onClose ();
            } }
            ref={ dialogReference }
        >
            <form method="dialog" onSubmit={ event => event.preventDefault () }>
                <header className="about-title-bar">
                    <h2 id="about-dialog-title">{ text ( "about.title" ) }</h2>
                    <button
                        aria-label={ text ( "web.dialog.close" ) }
                        className="about-close-button"
                        onClick={ properties.onClose }
                        type="button"
                    >
                        <span aria-hidden="true">&times;</span>
                    </button>
                </header>
                <div className="about-content">
                    <div className="about-identity">
                        <p className="about-application-name">{ text ( "about.application.name" ) }</p>
                        <p>{ text ( "about.version" ) }</p>
                        <p>{ text ( "about.author" ) }</p>
                    </div>
                    <hr />
                    <p className="about-description">{ text ( "about.description" ) }</p>
                </div>
                <footer className="form-actions about-footer">
                    <button autoFocus onClick={ properties.onClose } ref={ okButtonReference } type="button">
                        { text ( "button.ok" ) }
                    </button>
                </footer>
            </form>
        </dialog>
    );
}
