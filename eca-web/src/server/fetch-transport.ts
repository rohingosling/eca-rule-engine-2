// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    fetch-transport
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Implements cancellable real-server Fetch requests with bounded connection and response waits.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import {
    TransportFailure,
    type ServerTransport,
    type TransportRequest,
    type TransportResponse,
} from "./protocol";

export interface RealServerSettings
{
    readonly serverURL: string;
    readonly connectTimeoutMilliseconds: number;
    readonly requestTimeoutMilliseconds: number;
    readonly bearerToken: string;
}

function cancellationFailure (): TransportFailure
{
    return new TransportFailure ( "CANCELLED", "The operation was cancelled." );
}

export class FetchTransport implements ServerTransport
{
    private readonly settings: RealServerSettings;

    public constructor ( settings: RealServerSettings )
    {
        this.settings = settings;
    }

    public async request (
        request: TransportRequest,
        signal?: AbortSignal
    ): Promise <TransportResponse>
    {
        const targetURL = new URL ( request.path, this.settings.serverURL );
        const requestController = new AbortController ();
        let timeoutCategory: "CONNECTION_TIMEOUT" | "REQUEST_TIMEOUT" | null = null;
        const abortFromCaller = (): void => requestController.abort ();

        signal?.addEventListener ( "abort", abortFromCaller, { once: true } );

        const headers = new Headers ( request.headers );

        headers.set ( "Accept", "application/json, application/problem+json" );

        if ( this.settings.bearerToken.length > 0 )
        {
            headers.set ( "Authorization", `Bearer ${this.settings.bearerToken}` );
        }

        const connectionTimer = window.setTimeout (
            () =>
            {
                timeoutCategory = "CONNECTION_TIMEOUT";
                requestController.abort ();
            },
            this.settings.connectTimeoutMilliseconds
        );

        try
        {
            const options: RequestInit = {
                method: request.method,
                headers,
                body: request.body,
                signal: requestController.signal,
            };
            const response = await fetch ( targetURL, options );

            window.clearTimeout ( connectionTimer );

            const requestTimer = window.setTimeout (
                () =>
                {
                    timeoutCategory = "REQUEST_TIMEOUT";
                    requestController.abort ();
                },
                this.settings.requestTimeoutMilliseconds
            );

            try
            {
                const body = await response.text ();
                const responseHeaders: Record <string, string> = {};

                response.headers.forEach ( ( value, name ) =>
                {
                    responseHeaders [ name ] = value;
                } );

                return {
                    status: response.status,
                    headers: responseHeaders,
                    body,
                };
            }
            finally
            {
                window.clearTimeout ( requestTimer );
            }
        }
        catch ( error )
        {
            if ( signal?.aborted )
            {
                throw cancellationFailure ();
            }

            if ( timeoutCategory !== null )
            {
                throw new TransportFailure (
                    timeoutCategory,
                    timeoutCategory === "CONNECTION_TIMEOUT"
                        ? "The server connection timed out."
                        : "The server response timed out.",
                    error
                );
            }

            if ( error instanceof TypeError )
            {
                throw new TransportFailure (
                    "PERMISSION_OR_UNREACHABLE",
                    "The browser blocked the request or the real server is unreachable.",
                    error
                );
            }

            if ( error instanceof DOMException && error.name === "AbortError" )
            {
                throw cancellationFailure ();
            }

            throw new TransportFailure ( "PROTOCOL", "The real-server request failed.", error );
        }
        finally
        {
            window.clearTimeout ( connectionTimer );
            signal?.removeEventListener ( "abort", abortFromCaller );
        }
    }
}
