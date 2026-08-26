// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    preferences
// Version: 2.0.0
// Date:    2026-08-04
// Author:  Rohin Gosling
//
// Description:
//
//   Persists only UI state and non-sensitive real-server preferences; bearer tokens remain page-session-only.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import {
    CONNECT_TIMEOUT_SECONDS_MAXIMUM,
    CONNECT_TIMEOUT_SECONDS_MINIMUM,
    DEFAULT_SERVER_SETTINGS,
    REQUEST_TIMEOUT_SECONDS_MAXIMUM,
    REQUEST_TIMEOUT_SECONDS_MINIMUM,
    validateServerSettingsFields,
    type ServerTarget,
} from "./server/settings";

export type Theme = "light" | "dark";

export interface ShellPreferences
{
    readonly consoleVisible: boolean;
    readonly followConsoleTail: boolean;
    readonly theme: Theme;
    readonly outlineWidth: number;
    readonly userGuideWidth: number;
    readonly diagnosticsHeight: number;
    readonly expandedTreeItems: readonly string[];
    readonly serverTarget: ServerTarget;
    readonly serverURL: string;
    readonly connectTimeoutSeconds: number;
    readonly requestTimeoutSeconds: number;
}

export interface ShellLayoutBounds
{
    readonly outlineWidthMinimum: number;
    readonly outlineWidthMaximum: number;
    readonly userGuideWidthMinimum: number;
    readonly userGuideWidthMaximum: number;
    readonly diagnosticsHeightMinimum: number;
    readonly diagnosticsHeightMaximum: number;
}

const PREFERENCES_KEY = "eca-shell-preferences-v1";
const MINIMUM_PANE_FRACTION    = 1 / 5;
const MAXIMUM_PANE_FRACTION    = 3 / 5;
const MAXIMUM_CONSOLE_FRACTION = 2 / 3;
const CONSOLE_HEIGHT_MINIMUM   = 122;
const CONSOLE_HEIGHT_DEFAULT   = 196;
const USER_GUIDE_WIDTH_MINIMUM = 240;
const USER_GUIDE_WIDTH_DEFAULT = 440;

function numberInRange ( value: unknown, minimum: number, maximum: number, fallback: number ): number
{
    if ( typeof value !== "number" || !Number.isFinite ( value ) )
    {
        return fallback;
    }

    return Math.min ( maximum, Math.max ( minimum, value ) );
}

function integerInRange ( value: unknown, minimum: number, maximum: number, fallback: number ): number
{
    if ( typeof value !== "number" || !Number.isInteger ( value ) )
    {
        return fallback;
    }

    return Math.min ( maximum, Math.max ( minimum, value ) );
}

function safeServerURL ( value: unknown ): string
{
    if ( typeof value !== "string" )
    {
        return DEFAULT_SERVER_SETTINGS.serverURL;
    }

    const validationErrors = validateServerSettingsFields ( { ...DEFAULT_SERVER_SETTINGS, serverURL: value } );

    return validationErrors.some ( error => error.field === "serverURL" )
        ? DEFAULT_SERVER_SETTINGS.serverURL
        : value;
}

export function calculateShellLayoutBounds (
    viewportWidth: number,
    viewportHeight: number
): ShellLayoutBounds
{
    const outlineWidthMinimum = Math.round ( viewportWidth * MINIMUM_PANE_FRACTION );
    const outlineWidthMaximum = Math.round ( viewportWidth * MAXIMUM_PANE_FRACTION );
    const diagnosticsHeightMinimum = CONSOLE_HEIGHT_MINIMUM;
    const diagnosticsHeightMaximum = Math.max (
        CONSOLE_HEIGHT_MINIMUM,
        Math.floor ( viewportHeight * MAXIMUM_CONSOLE_FRACTION )
    );

    return {
        outlineWidthMinimum,
        outlineWidthMaximum,
        userGuideWidthMinimum: USER_GUIDE_WIDTH_MINIMUM,
        userGuideWidthMaximum: Math.max (
            USER_GUIDE_WIDTH_MINIMUM,
            Math.round ( viewportWidth * MAXIMUM_PANE_FRACTION )
        ),
        diagnosticsHeightMinimum,
        diagnosticsHeightMaximum,
    };
}

function defaultPreferences ( layoutBounds: ShellLayoutBounds ): ShellPreferences
{
    return {
        consoleVisible: true,
        followConsoleTail: true,
        theme: "dark",
        outlineWidth: layoutBounds.outlineWidthMinimum,
        userGuideWidth: numberInRange (
            USER_GUIDE_WIDTH_DEFAULT,
            layoutBounds.userGuideWidthMinimum,
            layoutBounds.userGuideWidthMaximum,
            layoutBounds.userGuideWidthMinimum
        ),
        diagnosticsHeight: numberInRange (
            CONSOLE_HEIGHT_DEFAULT,
            layoutBounds.diagnosticsHeightMinimum,
            layoutBounds.diagnosticsHeightMaximum,
            layoutBounds.diagnosticsHeightMinimum
        ),
        expandedTreeItems: [ "model" ],
        serverTarget: DEFAULT_SERVER_SETTINGS.target,
        serverURL: DEFAULT_SERVER_SETTINGS.serverURL,
        connectTimeoutSeconds: DEFAULT_SERVER_SETTINGS.connectTimeoutSeconds,
        requestTimeoutSeconds: DEFAULT_SERVER_SETTINGS.requestTimeoutSeconds,
    };
}

export function loadShellPreferences ( layoutBounds: ShellLayoutBounds ): ShellPreferences
{
    const defaultShellPreferences = defaultPreferences ( layoutBounds );

    try
    {
        const storedValue = window.localStorage.getItem ( PREFERENCES_KEY );

        if ( storedValue === null )
        {
            return defaultShellPreferences;
        }

        const parsedValue = JSON.parse ( storedValue ) as Record <string, unknown>;
        const expandedTreeItems = Array.isArray ( parsedValue.expandedTreeItems )
            ? parsedValue.expandedTreeItems.filter (
                ( item ): item is string => typeof item === "string"
            )
            : defaultShellPreferences.expandedTreeItems;

        return {
            consoleVisible: typeof parsedValue.consoleVisible === "boolean"
                ? parsedValue.consoleVisible
                : defaultShellPreferences.consoleVisible,
            followConsoleTail: typeof parsedValue.followConsoleTail === "boolean"
                ? parsedValue.followConsoleTail
                : defaultShellPreferences.followConsoleTail,
            theme: parsedValue.theme === "light" || parsedValue.theme === "dark"
                ? parsedValue.theme
                : defaultShellPreferences.theme,
            outlineWidth: numberInRange (
                parsedValue.outlineWidth,
                layoutBounds.outlineWidthMinimum,
                layoutBounds.outlineWidthMaximum,
                defaultShellPreferences.outlineWidth
            ),
            userGuideWidth: numberInRange (
                parsedValue.userGuideWidth,
                layoutBounds.userGuideWidthMinimum,
                layoutBounds.userGuideWidthMaximum,
                defaultShellPreferences.userGuideWidth
            ),
            diagnosticsHeight: numberInRange (
                parsedValue.diagnosticsHeight,
                layoutBounds.diagnosticsHeightMinimum,
                layoutBounds.diagnosticsHeightMaximum,
                defaultShellPreferences.diagnosticsHeight
            ),
            expandedTreeItems,
            serverTarget: parsedValue.serverTarget === "real" ? "real" : "built-in",
            serverURL: safeServerURL ( parsedValue.serverURL ),
            connectTimeoutSeconds: integerInRange (
                parsedValue.connectTimeoutSeconds,
                CONNECT_TIMEOUT_SECONDS_MINIMUM,
                CONNECT_TIMEOUT_SECONDS_MAXIMUM,
                defaultShellPreferences.connectTimeoutSeconds
            ),
            requestTimeoutSeconds: integerInRange (
                parsedValue.requestTimeoutSeconds,
                REQUEST_TIMEOUT_SECONDS_MINIMUM,
                REQUEST_TIMEOUT_SECONDS_MAXIMUM,
                defaultShellPreferences.requestTimeoutSeconds
            ),
        };
    }
    catch
    {
        return defaultShellPreferences;
    }
}

export function saveShellPreferences ( preferences: ShellPreferences ): boolean
{
    try
    {
        const validationErrors = validateServerSettingsFields (
            {
                target: preferences.serverTarget,
                serverURL: preferences.serverURL,
                connectTimeoutSeconds: preferences.connectTimeoutSeconds,
                requestTimeoutSeconds: preferences.requestTimeoutSeconds,
                bearerToken: "",
            }
        );

        if ( validationErrors.length > 0 )
        {
            return false;
        }

        window.localStorage.setItem ( PREFERENCES_KEY, JSON.stringify ( preferences ) );
        return true;
    }
    catch
    {
        return false;
    }
}
