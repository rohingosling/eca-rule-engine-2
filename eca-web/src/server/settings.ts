// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    settings
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Defines the browser-local and experimental real-server connection settings.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

export type ServerTarget = "built-in" | "real";

export interface ServerSettings
{
    readonly target: ServerTarget;
    readonly serverURL: string;
    readonly connectTimeoutSeconds: number;
    readonly requestTimeoutSeconds: number;
    readonly bearerToken: string;
}

export type ServerSettingsField = "serverURL" | "connectTimeoutSeconds" | "requestTimeoutSeconds";

export interface ServerSettingsValidationError
{
    readonly field: ServerSettingsField;
    readonly message: string;
}

export const CONNECT_TIMEOUT_SECONDS_MINIMUM = 1;
export const CONNECT_TIMEOUT_SECONDS_MAXIMUM = 300;
export const REQUEST_TIMEOUT_SECONDS_MINIMUM = 1;
export const REQUEST_TIMEOUT_SECONDS_MAXIMUM = 3600;

export const DEFAULT_SERVER_SETTINGS: ServerSettings = {
    target: "built-in",
    serverURL: "http://127.0.0.1:8080/",
    connectTimeoutSeconds: 5,
    requestTimeoutSeconds: 30,
    bearerToken: "",
};

export function validateServerSettingsFields ( settings: ServerSettings ): readonly ServerSettingsValidationError[]
{
    const errors: ServerSettingsValidationError[] = [];

    try
    {
        const url = new URL ( settings.serverURL );

        if ( url.protocol !== "http:" && url.protocol !== "https:" )
        {
            errors.push ( { field: "serverURL", message: "Server URL must use HTTP or HTTPS." } );
        }

        if ( url.username.length > 0 || url.password.length > 0 )
        {
            errors.push ( { field: "serverURL", message: "Server URL must not contain credentials." } );
        }

        if ( url.search.length > 0 || url.hash.length > 0 )
        {
            errors.push ( { field: "serverURL", message: "Server URL must not contain a query or fragment." } );
        }
    }
    catch
    {
        errors.push ( { field: "serverURL", message: "Server URL must be an absolute HTTP or HTTPS URL." } );
    }

    if (
        !Number.isInteger ( settings.connectTimeoutSeconds )
        || settings.connectTimeoutSeconds < CONNECT_TIMEOUT_SECONDS_MINIMUM
        || settings.connectTimeoutSeconds > CONNECT_TIMEOUT_SECONDS_MAXIMUM
    )
    {
        errors.push (
            {
                field: "connectTimeoutSeconds",
                message: `Connect timeout must be a whole number from ${CONNECT_TIMEOUT_SECONDS_MINIMUM} to `
                    + `${CONNECT_TIMEOUT_SECONDS_MAXIMUM} seconds.`,
            }
        );
    }

    if (
        !Number.isInteger ( settings.requestTimeoutSeconds )
        || settings.requestTimeoutSeconds < REQUEST_TIMEOUT_SECONDS_MINIMUM
        || settings.requestTimeoutSeconds > REQUEST_TIMEOUT_SECONDS_MAXIMUM
    )
    {
        errors.push (
            {
                field: "requestTimeoutSeconds",
                message: `Request timeout must be a whole number from ${REQUEST_TIMEOUT_SECONDS_MINIMUM} to `
                    + `${REQUEST_TIMEOUT_SECONDS_MAXIMUM} seconds.`,
            }
        );
    }

    return errors;
}

export function validateServerSettings ( settings: ServerSettings ): readonly string[]
{
    return validateServerSettingsFields ( settings ).map ( error => error.message );
}
