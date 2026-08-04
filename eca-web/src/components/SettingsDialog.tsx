// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    SettingsDialog
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Renders the Appearance/Server master-detail settings dialog and browser-local target controls.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { useEffect, useRef, useState } from "react";

import { text } from "../localization/messages";
import {
    validateServerSettings,
    type ServerSettings,
} from "../server";
import type { Theme } from "../preferences";
import { GroupBox } from "./WorkspacePanels";

interface SettingsDialogProperties
{
    readonly builtInModelIdentifier: string | null;
    readonly builtInModelRevision: string | null;
    readonly builtInStatus: string;
    readonly open: boolean;
    readonly operationRunning: boolean;
    readonly serverSettings: ServerSettings;
    readonly theme: Theme;
    readonly onApply: ( theme: Theme, settings: ServerSettings ) => void;
    readonly onCancel: () => void;
    readonly onResetBuiltIn: () => void;
    readonly onTestConnection: ( settings: ServerSettings ) => void;
}

type SettingsGroup = "appearance" | "server";

export function SettingsDialog ( properties: SettingsDialogProperties )
{
    const dialogReference = useRef <HTMLDialogElement> ( null );
    const returnFocusReference = useRef <HTMLElement | null> ( null );
    const [ selectedGroup, setSelectedGroup ] = useState <SettingsGroup> ( "appearance" );
    const [ draftTheme, setDraftTheme ] = useState ( properties.theme );
    const [ draftSettings, setDraftSettings ] = useState ( properties.serverSettings );
    const [ validationMessages, setValidationMessages ] = useState <readonly string[]> ( [] );

    useEffect (
        () =>
        {
            const dialog = dialogReference.current;

            if ( properties.open && dialog !== null && !dialog.open )
            {
                returnFocusReference.current = document.activeElement instanceof HTMLElement
                    ? document.activeElement
                    : null;
                setDraftTheme ( properties.theme );
                setDraftSettings ( properties.serverSettings );
                setSelectedGroup ( "appearance" );
                setValidationMessages ( [] );
                dialog.showModal ();
            }
            else if ( !properties.open && dialog?.open )
            {
                dialog.close ();
                window.setTimeout ( () => returnFocusReference.current?.focus (), 0 );
            }
        },
        [ properties.open, properties.serverSettings, properties.theme ]
    );

    function apply (): void
    {
        const errors = draftSettings.target === "real"
            ? validateServerSettings ( draftSettings )
            : [];

        setValidationMessages ( errors );

        if ( errors.length === 0 )
        {
            properties.onApply ( draftTheme, draftSettings );
        }
        else
        {
            window.setTimeout (
                () => dialogReference.current?.querySelector <HTMLElement> ( "[aria-invalid='true']" )?.focus (),
                0
            );
        }
    }

    function testConnection (): void
    {
        const errors = draftSettings.target === "real"
            ? validateServerSettings ( draftSettings )
            : [];

        setValidationMessages ( errors );

        if ( errors.length === 0 )
        {
            properties.onTestConnection ( draftSettings );
        }
        else
        {
            window.setTimeout (
                () => dialogReference.current?.querySelector <HTMLElement> ( "[aria-invalid='true']" )?.focus (),
                0
            );
        }
    }

    return (
        <dialog
            aria-labelledby="settings-dialog-title"
            className="settings-dialog"
            onCancel={ event =>
            {
                event.preventDefault ();
                properties.onCancel ();
            } }
            ref={ dialogReference }
        >
            <form method="dialog" onSubmit={ event => event.preventDefault () }>
                <h2 id="settings-dialog-title">{ text ( "settings.dialog.title" ) }</h2>
                <div className="settings-workspace">
                    <label className="visually-hidden" htmlFor="settings-group-selector">
                        { text ( "settings.groups.accessible" ) }
                    </label>
                    <select
                        autoFocus
                        className="settings-group-selector"
                        id="settings-group-selector"
                        onChange={ event => setSelectedGroup ( event.currentTarget.value as SettingsGroup ) }
                        size={ 2 }
                        value={ selectedGroup }
                    >
                        <option value="appearance">{ text ( "settings.group.appearance" ) }</option>
                        <option value="server">{ text ( "settings.group.server" ) }</option>
                    </select>
                    <div className="settings-detail">
                        { selectedGroup === "appearance" ? (
                            <GroupBox legend={ text ( "settings.appearance.group.title" ) }>
                                <div className="form-grid">
                                    <label htmlFor="settings-theme">{ text ( "settings.theme.label" ) }</label>
                                    <select
                                        id="settings-theme"
                                        onChange={ event => setDraftTheme ( event.currentTarget.value as Theme ) }
                                        value={ draftTheme }
                                    >
                                        <option value="light">{ text ( "settings.theme.light" ) }</option>
                                        <option value="dark">{ text ( "settings.theme.dark" ) }</option>
                                    </select>
                                </div>
                            </GroupBox>
                        ) : (
                            <>
                                <GroupBox legend={ text ( "web.settings.target.group" ) }>
                                    <div className="form-grid">
                                        <label htmlFor="settings-server-target">
                                            { text ( "web.settings.target.label" ) }
                                        </label>
                                        <select
                                            id="settings-server-target"
                                            onChange={ event =>
                                            {
                                                const target = event.currentTarget.value as ServerSettings["target"];

                                                setDraftSettings ( currentSettings => ( { ...currentSettings, target } ) );
                                            } }
                                            value={ draftSettings.target }
                                        >
                                            <option value="built-in">{ text ( "web.server.built.in" ) }</option>
                                            <option value="real">{ text ( "web.server.real" ) }</option>
                                        </select>
                                    </div>
                                </GroupBox>
                                { draftSettings.target === "built-in" ? (
                                    <GroupBox legend={ text ( "web.settings.built.in.group" ) }>
                                        <div className="form-grid result-grid">
                                            <span id="settings-built-in-status-label">
                                                { text ( "web.settings.status" ) }
                                            </span>
                                            <output
                                                aria-labelledby="settings-built-in-status-label"
                                                className="read-only-value"
                                            >
                                                { properties.builtInStatus }
                                            </output>
                                            <span id="settings-built-in-model-label">
                                                { text ( "web.settings.model" ) }
                                            </span>
                                            <output
                                                aria-labelledby="settings-built-in-model-label"
                                                className="read-only-value"
                                            >
                                                { properties.builtInModelIdentifier
                                                    ?? text ( "simulator.revision.unknown" ) }
                                            </output>
                                            <span id="settings-built-in-revision-label">
                                                { text ( "web.settings.revision" ) }
                                            </span>
                                            <output
                                                aria-labelledby="settings-built-in-revision-label"
                                                className="read-only-value"
                                            >
                                                { properties.builtInModelRevision
                                                    ?? text ( "simulator.revision.unknown" ) }
                                            </output>
                                        </div>
                                        <p className="secondary-text">{ text ( "web.settings.built.in.note" ) }</p>
                                        <div className="group-actions">
                                            <button
                                                disabled={ properties.operationRunning }
                                                onClick={ properties.onResetBuiltIn }
                                                type="button"
                                            >
                                                { text ( "web.button.reset.example" ) }
                                            </button>
                                            <button
                                                disabled={ properties.operationRunning }
                                                onClick={ testConnection }
                                                type="button"
                                            >
                                                { text ( "menu.file.test.connection" ) }
                                            </button>
                                        </div>
                                    </GroupBox>
                                ) : (
                                    <GroupBox legend={ text ( "settings.server.group.title" ) }>
                                        <div className="form-grid">
                                            <label htmlFor="settings-server-url">{ text ( "connection.url.label" ) }</label>
                                            <input
                                                aria-describedby={ validationMessages.length > 0
                                                    ? "settings-validation-errors"
                                                    : undefined }
                                                aria-invalid={ validationMessages.length > 0 || undefined }
                                                id="settings-server-url"
                                                onChange={ event =>
                                                {
                                                    const serverURL = event.currentTarget.value;

                                                    setDraftSettings ( currentSettings => ( { ...currentSettings, serverURL } ) );
                                                } }
                                                type="url"
                                                value={ draftSettings.serverURL }
                                            />
                                            <label htmlFor="settings-connect-timeout">
                                                { text ( "connection.connect.timeout.label" ) }
                                            </label>
                                            <input
                                                aria-describedby={ validationMessages.length > 0
                                                    ? "settings-validation-errors"
                                                    : undefined }
                                                aria-invalid={ validationMessages.length > 0 || undefined }
                                                id="settings-connect-timeout"
                                                min="1"
                                                onChange={ event =>
                                                {
                                                    const connectTimeoutSeconds = Number ( event.currentTarget.value );

                                                    setDraftSettings ( currentSettings => ( {
                                                        ...currentSettings,
                                                        connectTimeoutSeconds,
                                                    } ) );
                                                } }
                                                step="1"
                                                type="number"
                                                value={ draftSettings.connectTimeoutSeconds }
                                            />
                                            <label htmlFor="settings-request-timeout">
                                                { text ( "connection.request.timeout.label" ) }
                                            </label>
                                            <input
                                                aria-describedby={ validationMessages.length > 0
                                                    ? "settings-validation-errors"
                                                    : undefined }
                                                aria-invalid={ validationMessages.length > 0 || undefined }
                                                id="settings-request-timeout"
                                                min="1"
                                                onChange={ event =>
                                                {
                                                    const requestTimeoutSeconds = Number ( event.currentTarget.value );

                                                    setDraftSettings ( currentSettings => ( {
                                                        ...currentSettings,
                                                        requestTimeoutSeconds,
                                                    } ) );
                                                } }
                                                step="1"
                                                type="number"
                                                value={ draftSettings.requestTimeoutSeconds }
                                            />
                                            <label htmlFor="settings-bearer-token">
                                                { text ( "connection.token.label" ) }
                                            </label>
                                            <input
                                                autoComplete="off"
                                                id="settings-bearer-token"
                                                onChange={ event =>
                                                {
                                                    const bearerToken = event.currentTarget.value;

                                                    setDraftSettings ( currentSettings => ( {
                                                        ...currentSettings,
                                                        bearerToken,
                                                    } ) );
                                                } }
                                                type="password"
                                                value={ draftSettings.bearerToken }
                                            />
                                        </div>
                                        <p className="secondary-text">{ text ( "connection.token.note" ) }</p>
                                        <p className="secondary-text">{ text ( "web.settings.real.permission.note" ) }</p>
                                        <div className="group-actions">
                                            <button
                                                disabled={ properties.operationRunning }
                                                onClick={ testConnection }
                                                type="button"
                                            >
                                                { text ( "menu.file.test.connection" ) }
                                            </button>
                                        </div>
                                    </GroupBox>
                                ) }
                            </>
                        ) }
                    </div>
                </div>
                { validationMessages.length > 0 && (
                    <div
                        aria-live="assertive"
                        className="settings-errors"
                        id="settings-validation-errors"
                        role="alert"
                    >
                        { validationMessages.map ( message => <p key={ message }>{ message }</p> ) }
                    </div>
                ) }
                <footer className="form-actions settings-footer">
                    <button disabled={ properties.operationRunning } onClick={ apply } type="button">
                        { text ( "button.apply" ) }
                    </button>
                    <button onClick={ properties.onCancel } type="button">{ text ( "button.cancel" ) }</button>
                </footer>
            </form>
        </dialog>
    );
}
