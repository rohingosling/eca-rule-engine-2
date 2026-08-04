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

export const DEFAULT_SERVER_SETTINGS: ServerSettings = {
    target: "built-in",
    serverURL: "http://127.0.0.1:8080/",
    connectTimeoutSeconds: 5,
    requestTimeoutSeconds: 30,
    bearerToken: "",
};

export function validateServerSettings ( settings: ServerSettings ): readonly string[]
{
    const errors: string[] = [];

    try
    {
        const url = new URL ( settings.serverURL );

        if ( url.protocol !== "http:" && url.protocol !== "https:" )
        {
            errors.push ( "Server URL must use HTTP or HTTPS." );
        }

        if ( url.username.length > 0 || url.password.length > 0 )
        {
            errors.push ( "Server URL must not contain credentials." );
        }

        if ( url.search.length > 0 || url.hash.length > 0 )
        {
            errors.push ( "Server URL must not contain a query or fragment." );
        }
    }
    catch
    {
        errors.push ( "Server URL must be an absolute HTTP or HTTPS URL." );
    }

    if ( !Number.isInteger ( settings.connectTimeoutSeconds ) || settings.connectTimeoutSeconds <= 0 )
    {
        errors.push ( "Connect timeout must be a positive whole number of seconds." );
    }

    if ( !Number.isInteger ( settings.requestTimeoutSeconds ) || settings.requestTimeoutSeconds <= 0 )
    {
        errors.push ( "Request timeout must be a positive whole number of seconds." );
    }

    return errors;
}
