// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    SettingsDialog
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Renders the Appearance, Console, and Server master-detail settings dialog and connection controls.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { useEffect, useRef, useState } from "react";

import { text } from "../localization/messages";
import {
    CONNECT_TIMEOUT_SECONDS_MAXIMUM,
    CONNECT_TIMEOUT_SECONDS_MINIMUM,
    REQUEST_TIMEOUT_SECONDS_MAXIMUM,
    REQUEST_TIMEOUT_SECONDS_MINIMUM,
    validateServerSettingsFields,
    type ServerSettings,
    type ServerSettingsField,
    type ServerSettingsValidationError,
} from "../server";
import type { Theme } from "../preferences";
import { ModalDialog } from "./ModalDialog";
import { GroupBox } from "./WorkspacePanels";

interface SettingsDialogProperties
{
    readonly builtInModelIdentifier: string | null;
    readonly builtInModelRevision: string | null;
    readonly builtInStatus: string;
    readonly followConsoleTail: boolean;
    readonly open: boolean;
    readonly operationRunning: boolean;
    readonly serverSettings: ServerSettings;
    readonly theme: Theme;
    readonly onApply: ( theme: Theme, settings: ServerSettings, followConsoleTail: boolean ) => void;
    readonly onCancel: () => void;
    readonly onResetBuiltIn: () => void;
    readonly onTestConnection: ( settings: ServerSettings ) => void;
}

type SettingsGroup = "appearance" | "console" | "server";

export function SettingsDialog ( properties: SettingsDialogProperties )
{
    const [ selectedGroup, setSelectedGroup ] = useState <SettingsGroup> ( "appearance" );
    const [ draftTheme, setDraftTheme ] = useState ( properties.theme );
    const [ draftSettings, setDraftSettings ] = useState ( properties.serverSettings );
    const [ draftFollowConsoleTail, setDraftFollowConsoleTail ] = useState ( properties.followConsoleTail );
    const [ validationErrors, setValidationErrors ] = useState <readonly ServerSettingsValidationError[]> ( [] );
    const wasOpenReference = useRef ( false );

    useEffect (
        () =>
        {
            if ( properties.open && !wasOpenReference.current )
            {
                setDraftTheme ( properties.theme );
                setDraftSettings ( properties.serverSettings );
                setDraftFollowConsoleTail ( properties.followConsoleTail );
                setSelectedGroup ( "appearance" );
                setValidationErrors ( [] );
            }

            wasOpenReference.current = properties.open;
        },
        [ properties.followConsoleTail, properties.open, properties.serverSettings, properties.theme ]
    );

    function apply (): void
    {
        const errors = validateServerSettingsFields ( draftSettings );

        if ( errors.length === 0 )
        {
            setValidationErrors ( [] );
            properties.onApply ( draftTheme, draftSettings, draftFollowConsoleTail );
        }
        else
        {
            presentValidationErrors ( errors );
        }
    }

    function testConnection (): void
    {
        const errors = draftSettings.target === "real" ? validateServerSettingsFields ( draftSettings ) : [];

        if ( errors.length === 0 )
        {
            setValidationErrors ( [] );
            properties.onTestConnection ( draftSettings );
        }
        else
        {
            presentValidationErrors ( errors );
        }
    }

    function clearValidationErrorsFor ( field: ServerSettingsField ): void
    {
        setValidationErrors ( currentErrors => currentErrors.filter ( error => error.field !== field ) );
    }

    function errorsFor ( field: ServerSettingsField ): readonly ServerSettingsValidationError[]
    {
        return validationErrors.filter ( error => error.field === field );
    }

    function errorIdentifierFor ( field: ServerSettingsField ): string
    {
        return `settings-${field}-errors`;
    }

    function controlIdentifierFor ( field: ServerSettingsField ): string
    {
        switch ( field )
        {
            case "serverURL":
                return "settings-server-url";
            case "connectTimeoutSeconds":
                return "settings-connect-timeout";
            case "requestTimeoutSeconds":
                return "settings-request-timeout";
        }
    }

    function presentValidationErrors ( errors: readonly ServerSettingsValidationError[] ): void
    {
        const firstError = errors [ 0 ];

        if ( firstError === undefined )
        {
            return;
        }

        setValidationErrors ( errors );
        setSelectedGroup ( "server" );

        if ( draftSettings.target === "built-in" )
        {
            setDraftSettings ( currentSettings => ( { ...currentSettings, target: "real" } ) );
        }

        window.setTimeout (
            () => document.getElementById ( controlIdentifierFor ( firstError.field ) )?.focus (),
            0
        );
    }

    return (
        <ModalDialog
            actions={
                <>
                    <button onClick={ properties.onCancel } type="button">{ text ( "button.cancel" ) }</button>
                    <button disabled={ properties.operationRunning } onClick={ apply } type="button">
                        { text ( "button.apply" ) }
                    </button>
                </>
            }
            className="settings-dialog"
            initialFocusSelector="#settings-group-selector"
            onRequestClose={ properties.onCancel }
            open={ properties.open }
            title={ text ( "settings.dialog.title" ) }
            titleIdentifier="settings-dialog-title"
        >
                <div className="settings-workspace">
                    <label className="visually-hidden" htmlFor="settings-group-selector">
                        { text ( "settings.groups.accessible" ) }
                    </label>
                    <select
                        autoFocus
                        className="settings-group-selector"
                        id="settings-group-selector"
                        onChange={ event => setSelectedGroup ( event.currentTarget.value as SettingsGroup ) }
                        size={ 3 }
                        value={ selectedGroup }
                    >
                        <option value="appearance">{ text ( "settings.group.appearance" ) }</option>
                        <option value="console">{ text ( "settings.group.console" ) }</option>
                        <option value="server">{ text ( "settings.group.server" ) }</option>
                    </select>
                    <div className="settings-detail">
                        { selectedGroup === "appearance" ? (
                            <GroupBox legend={ text ( "settings.appearance.group.title" ) }>
                                <fieldset className="settings-choice-group">
                                    <legend>{ text ( "settings.theme.label" ) }</legend>
                                    <label>
                                        <input
                                            checked={ draftTheme === "light" }
                                            name="settings-theme"
                                            onChange={ () => setDraftTheme ( "light" ) }
                                            type="radio"
                                        />
                                        <span>{ text ( "settings.theme.light" ) }</span>
                                    </label>
                                    <label>
                                        <input
                                            checked={ draftTheme === "dark" }
                                            name="settings-theme"
                                            onChange={ () => setDraftTheme ( "dark" ) }
                                            type="radio"
                                        />
                                        <span>{ text ( "settings.theme.dark" ) }</span>
                                    </label>
                                </fieldset>
                            </GroupBox>
                        ) : selectedGroup === "console" ? (
                            <GroupBox legend={ text ( "settings.console.group.title" ) }>
                                <label className="settings-checkbox-setting">
                                    <input
                                        checked={ draftFollowConsoleTail }
                                        name="settings-follow-tail"
                                        onChange={ event => setDraftFollowConsoleTail ( event.currentTarget.checked ) }
                                        type="checkbox"
                                    />
                                    <span>{ text ( "web.console.follow.tail" ) }</span>
                                </label>
                            </GroupBox>
                        ) : (
                            <>
                                <GroupBox legend={ text ( "web.settings.target.group" ) }>
                                    <div className="form-grid">
                                        <label htmlFor="settings-server-target">
                                            <span className="form-field-label-text">
                                                { text ( "web.settings.target.label" ) }
                                            </span>
                                        </label>
                                        <select
                                            id="settings-server-target"
                                            onChange={ event =>
                                            {
                                                const target = event.currentTarget.value as ServerSettings["target"];

                                                setDraftSettings ( currentSettings => ( { ...currentSettings, target } ) );
                                                setValidationErrors ( [] );
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
                                            <span className="form-field-label-text" id="settings-built-in-status-label">
                                                { text ( "web.settings.status" ) }
                                            </span>
                                            <output
                                                aria-labelledby="settings-built-in-status-label"
                                                className="read-only-value"
                                            >
                                                { properties.builtInStatus }
                                            </output>
                                            <span className="form-field-label-text" id="settings-built-in-model-label">
                                                { text ( "web.settings.model" ) }
                                            </span>
                                            <output
                                                aria-labelledby="settings-built-in-model-label"
                                                className="read-only-value"
                                            >
                                                { properties.builtInModelIdentifier
                                                    ?? text ( "simulator.revision.unknown" ) }
                                            </output>
                                            <span className="form-field-label-text" id="settings-built-in-revision-label">
                                                { text ( "web.settings.revision" ) }
                                            </span>
                                            <output
                                                aria-labelledby="settings-built-in-revision-label"
                                                className="read-only-value settings-revision-value"
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
                                            <label htmlFor="settings-server-url">
                                                <span className="form-field-label-text">
                                                    { text ( "connection.url.label" ) }
                                                </span>
                                            </label>
                                            <input
                                                aria-describedby={ errorsFor ( "serverURL" ).length > 0
                                                    ? errorIdentifierFor ( "serverURL" )
                                                    : undefined }
                                                aria-invalid={ errorsFor ( "serverURL" ).length > 0 || undefined }
                                                id="settings-server-url"
                                                onChange={ event =>
                                                {
                                                    const serverURL = event.currentTarget.value;

                                                    setDraftSettings ( currentSettings => ( { ...currentSettings, serverURL } ) );
                                                    clearValidationErrorsFor ( "serverURL" );
                                                } }
                                                type="url"
                                                value={ draftSettings.serverURL }
                                            />
                                            <label htmlFor="settings-connect-timeout">
                                                <span className="form-field-label-text">
                                                    { text ( "connection.connect.timeout.label" ) }
                                                </span>
                                            </label>
                                            <input
                                                aria-describedby={ errorsFor ( "connectTimeoutSeconds" ).length > 0
                                                    ? errorIdentifierFor ( "connectTimeoutSeconds" )
                                                    : undefined }
                                                aria-invalid={ errorsFor ( "connectTimeoutSeconds" ).length > 0
                                                    || undefined }
                                                id="settings-connect-timeout"
                                                max={ CONNECT_TIMEOUT_SECONDS_MAXIMUM }
                                                min={ CONNECT_TIMEOUT_SECONDS_MINIMUM }
                                                onChange={ event =>
                                                {
                                                    const connectTimeoutSeconds = Number ( event.currentTarget.value );

                                                    setDraftSettings ( currentSettings => ( {
                                                        ...currentSettings,
                                                        connectTimeoutSeconds,
                                                    } ) );
                                                    clearValidationErrorsFor ( "connectTimeoutSeconds" );
                                                } }
                                                step="1"
                                                type="number"
                                                value={ draftSettings.connectTimeoutSeconds }
                                            />
                                            <label htmlFor="settings-request-timeout">
                                                <span className="form-field-label-text">
                                                    { text ( "connection.request.timeout.label" ) }
                                                </span>
                                            </label>
                                            <input
                                                aria-describedby={ errorsFor ( "requestTimeoutSeconds" ).length > 0
                                                    ? errorIdentifierFor ( "requestTimeoutSeconds" )
                                                    : undefined }
                                                aria-invalid={ errorsFor ( "requestTimeoutSeconds" ).length > 0
                                                    || undefined }
                                                id="settings-request-timeout"
                                                max={ REQUEST_TIMEOUT_SECONDS_MAXIMUM }
                                                min={ REQUEST_TIMEOUT_SECONDS_MINIMUM }
                                                onChange={ event =>
                                                {
                                                    const requestTimeoutSeconds = Number ( event.currentTarget.value );

                                                    setDraftSettings ( currentSettings => ( {
                                                        ...currentSettings,
                                                        requestTimeoutSeconds,
                                                    } ) );
                                                    clearValidationErrorsFor ( "requestTimeoutSeconds" );
                                                } }
                                                step="1"
                                                type="number"
                                                value={ draftSettings.requestTimeoutSeconds }
                                            />
                                            <label htmlFor="settings-bearer-token">
                                                <span className="form-field-label-text">
                                                    { text ( "connection.token.label" ) }
                                                </span>
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
                { validationErrors.length > 0 && (
                    <div
                        aria-live="assertive"
                        className="settings-errors"
                        id="settings-validation-errors"
                        role="alert"
                    >
                        { ( [ "serverURL", "connectTimeoutSeconds", "requestTimeoutSeconds" ] as const ).map (
                            field => errorsFor ( field ).length > 0 && (
                                <div id={ errorIdentifierFor ( field ) } key={ field }>
                                    { errorsFor ( field ).map (
                                        error => <p key={ error.message }>{ error.message }</p>
                                    ) }
                                </div>
                            )
                        ) }
                    </div>
                ) }
        </ModalDialog>
    );
}
