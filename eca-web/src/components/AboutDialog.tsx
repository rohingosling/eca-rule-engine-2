// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    About Dialog
// Version: 2.0.0
// Date:    2026-08-25
// Author:  Rohin Gosling
//
// Description:
//
//   Renders application identity, licences, and release notes in the shared modal shell.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { useEffect, useState } from "react";

import { text } from "../localization/messages";
import { ModalDialog } from "./ModalDialog";
import { Tabs } from "./Tabs";

interface AboutDialogProperties
{
    readonly open: boolean;
    readonly onClose: () => void;
}

type AboutTabIdentifier = "about-licences" | "about-release-notes";

interface LicenceNoticeProperties
{
    readonly identifier: string;
    readonly label: string;
    readonly text: string;
}

function LicenceNotice ( properties: LicenceNoticeProperties )
{
    const titleIdentifier = `${properties.identifier}-title`;

    return (
        <section className="about-license">
            <h3 id={ titleIdentifier }>{ properties.label }</h3>
            <textarea
                aria-labelledby={ titleIdentifier }
                className="about-license-text"
                id={ properties.identifier }
                readOnly
                rows={ 8 }
                spellCheck={ false }
                value={ properties.text }
            />
        </section>
    );
}

export function AboutDialog ( properties: AboutDialogProperties )
{
    const [ applicationLicence, setApplicationLicence ] = useState ( "" );
    const [ fluentLicence, setFluentLicence ] = useState ( "" );

    const [ activeTab, setActiveTab ] = useState <AboutTabIdentifier> ( "about-licences" );
    useEffect ( () =>
    {
        if ( !properties.open )
        {
            return;
        }

        const abortController = new AbortController ();

        setActiveTab ( "about-licences" );

        async function loadLicence ( resourceName: string ): Promise <string>
        {
            try
            {
                const response = await fetch (
                    `${import.meta.env.BASE_URL}notices/${resourceName}`,
                    { signal: abortController.signal }
                );

                if ( !response.ok )
                {
                    throw new Error ( `Licence request failed with status ${response.status}.` );
                }

                return await response.text ();
            }
            catch ( error )
            {
                if ( error instanceof DOMException && error.name === "AbortError" )
                {
                    return "";
                }

                return text ( "about.license.unavailable" );
            }
        }

        void Promise.all (
            [
                loadLicence ( "eca-rule-engine-license.txt" ),
                loadLicence ( "fluent-ui-system-icons.txt" ),
            ]
        ).then ( ( [ nextApplicationLicence, nextFluentLicence ] ) =>
        {
            if ( !abortController.signal.aborted )
            {
                setApplicationLicence ( nextApplicationLicence );
                setFluentLicence ( nextFluentLicence );
            }
        } );

        return () => abortController.abort ();
    }, [ properties.open ] );

    function closeDialog (): void
    {
        setActiveTab ( "about-licences" );
        properties.onClose ();
    }

    return (
        <ModalDialog
            actions={
                <button onClick={ closeDialog } type="button">{ text ( "web.dialog.close" ) }</button>
            }
            className="modal-dialog about-dialog"
            initialFocusSelector=".dialog-footer button"
            onRequestClose={ closeDialog }
            open={ properties.open }
            title={ text ( "about.title" ) }
            titleIdentifier="about-dialog-title"
        >
            <div className="about-identity">
                <img
                    alt=""
                    aria-hidden="true"
                    className="about-application-icon"
                    src={ `${import.meta.env.BASE_URL}assets/eca-rule-engine.png` }
                />
                <div>
                    <strong>{ text ( "about.application.name" ) }</strong>
                    <span>{ text ( "about.version" ) } { text ( "about.author" ) }</span>
                </div>
            </div>
            <p>{ text ( "about.description" ) }</p>
            <p className="about-technical-note">
                <strong>{ text ( "about.technical.note.label" ) }</strong>{ " " }
                <a
                    href={ text ( "about.technical.note.url" ) }
                    rel="noreferrer"
                    target="_blank"
                >
                    { text ( "about.technical.note.url" ) }
                </a>
            </p>
            <Tabs
                accessibleLabel={ text ( "about.tabs.label" ) }
                activeTab={ activeTab }
                className="about-tabs"
                onSelect={ setActiveTab }
                tabs={
                    [
                        {
                            content: (
                                <div className="about-licences">
                                    <LicenceNotice
                                        identifier="about-application-licence"
                                        label={ text ( "about.license.application" ) }
                                        text={ applicationLicence }
                                    />
                                    <LicenceNotice
                                        identifier="about-fluent-licence"
                                        label={ text ( "about.license.fluent" ) }
                                        text={ fluentLicence }
                                    />
                                </div>
                            ),
                            identifier: "about-licences",
                            label: text ( "about.tab.licences" ),
                        },
                        {
                            content: (
                                <div className="about-release-notes">
                                    <LicenceNotice
                                        identifier="about-release-notes"
                                        label={ text ( "about.tab.release.notes" ) }
                                        text={ text ( "about.release.notes" ) }
                                    />
                                </div>
                            ),
                            identifier: "about-release-notes",
                            label: text ( "about.tab.release.notes" ),
                        },
                    ]
                }
                />
        </ModalDialog>
    );
}
