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
    DEFAULT_SERVER_SETTINGS,
    type ServerTarget,
} from "./server/settings";

export type Theme = "light" | "dark";

export interface ShellPreferences
{
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

export function calculateShellLayoutBounds (
    viewportWidth: number,
    viewportHeight: number
): ShellLayoutBounds
{
    const outlineWidthMinimum = Math.round ( viewportWidth * MINIMUM_PANE_FRACTION );
    const outlineWidthMaximum = Math.round ( viewportWidth * MAXIMUM_PANE_FRACTION );
    const diagnosticsHeightMinimum = Math.round ( viewportHeight * MINIMUM_PANE_FRACTION );
    const diagnosticsHeightMaximum = Math.round ( viewportHeight * MAXIMUM_PANE_FRACTION );

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
        theme: "dark",
        outlineWidth: layoutBounds.outlineWidthMinimum,
        userGuideWidth: numberInRange (
            USER_GUIDE_WIDTH_DEFAULT,
            layoutBounds.userGuideWidthMinimum,
            layoutBounds.userGuideWidthMaximum,
            layoutBounds.userGuideWidthMinimum
        ),
        diagnosticsHeight: layoutBounds.diagnosticsHeightMinimum,
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
            serverURL: typeof parsedValue.serverURL === "string"
                ? parsedValue.serverURL
                : defaultShellPreferences.serverURL,
            connectTimeoutSeconds: numberInRange (
                parsedValue.connectTimeoutSeconds,
                1,
                300,
                defaultShellPreferences.connectTimeoutSeconds
            ),
            requestTimeoutSeconds: numberInRange (
                parsedValue.requestTimeoutSeconds,
                1,
                3600,
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
        window.localStorage.setItem ( PREFERENCES_KEY, JSON.stringify ( preferences ) );
        return true;
    }
    catch
    {
        return false;
    }
}
