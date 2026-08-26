// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    phase-19-browser.spec
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies GitHub-Pages-origin browser access to the real loopback Java server and denied-origin opacity.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { randomBytes }                    from "node:crypto";
import { mkdtemp, readFile, rm }          from "node:fs/promises";
import { tmpdir }                         from "node:os";
import { dirname, resolve }               from "node:path";
import { spawn, type ChildProcess }       from "node:child_process";
import { fileURLToPath }                  from "node:url";
import { expect, test, type Page }        from "@playwright/test";

const PAGES_ORIGIN  = "https://rohingosling.github.io";
const DENIED_ORIGIN = "https://attacker.example";

interface BrowserWorkflowResult
{
    readonly livenessStatus: number;
    readonly readinessStatus: number;
    readonly pullStatus: number;
    readonly pullEntityTag: string | null;
    readonly pullRevision: string | null;
    readonly pushStatus: number;
    readonly pushEntityTag: string | null;
    readonly evaluationStatus: number;
    readonly evaluationOutcome: string;
    readonly evaluationActionIdentifier: string;
}

const testDirectory    = dirname ( fileURLToPath ( import.meta.url ) );
const webDirectory     = resolve ( testDirectory, ".." );
const projectDirectory = resolve ( webDirectory, ".." );
const serverJar        = process.env.ECA_PHASE_19_SERVER_JAR
    ?? resolve ( projectDirectory, "eca-server", "target", "eca-server.jar" );
const modelPath        = resolve ( projectDirectory, "examples", "eca-rule-engine-example.json" );

let serverProcess: ChildProcess | undefined;
let serverBaseURL  = "";
let bearerToken    = "";
let dataDirectory  = "";
let modelDocument  = "";
let serverOutput   = "";

function waitForServerAddress ( processHandle: ChildProcess ): Promise <string>
{
    return new Promise (
        ( resolveAddress, rejectAddress ) =>
        {
            const timeout = setTimeout (
                () => rejectAddress ( new Error ( `Java server startup timed out.\n${serverOutput}` ) ),
                30_000
            );
            const inspectOutput = ( chunk: Buffer ): void =>
            {
                serverOutput += chunk.toString ( "utf8" );
                const addressMatch = serverOutput.match ( /http:\/\/127\.0\.0\.1:(\d+)/ );

                if ( addressMatch !== null )
                {
                    clearTimeout ( timeout );
                    resolveAddress ( `http://127.0.0.1:${addressMatch [ 1 ]}` );
                }
            };

            processHandle.stdout?.on ( "data", inspectOutput );
            processHandle.stderr?.on ( "data", inspectOutput );
            processHandle.once (
                "exit",
                exitCode =>
                {
                    clearTimeout ( timeout );
                    rejectAddress (
                        new Error ( `Java server exited during startup with code ${exitCode}.\n${serverOutput}` )
                    );
                }
            );
            processHandle.once ( "error", rejectAddress );
        }
    );
}

async function openInterceptedOrigin ( page: Page, origin: string ): Promise <void>
{
    await page.route (
        `${origin}/**`,
        async route => route.fulfill (
            {
                status: 200,
                contentType: "text/html",
                body: "<!doctype html><html lang=\"en\"><title>Phase 19</title><body>Phase 19</body></html>",
            }
        )
    );
    await page.goto ( `${origin}/eca-rule-engine-2/phase-19.html` );
}

test.beforeAll (
    async () =>
    {
        bearerToken   = randomBytes ( 32 ).toString ( "base64url" );
        dataDirectory = await mkdtemp ( resolve ( tmpdir (), "eca-phase-19-" ) );
        modelDocument = await readFile ( modelPath, "utf8" );
        serverProcess = spawn (
            "java",
            [
                "-jar",
                serverJar,
                "start",
                "--port",
                "0",
                "--data-directory",
                dataDirectory,
                "--model",
                modelPath,
                "--allowed-origin",
                PAGES_ORIGIN,
            ],
            {
                cwd: projectDirectory,
                env:
                {
                    ...process.env,
                    ECA_SERVER_TOKEN: bearerToken,
                },
                stdio: [ "ignore", "pipe", "pipe" ],
                windowsHide: true,
            }
        );
        serverBaseURL = await waitForServerAddress ( serverProcess );
    }
);

test.afterAll (
    async () =>
    {
        if ( serverProcess !== undefined && serverProcess.exitCode === null )
        {
            try
            {
                await fetch (
                    `${serverBaseURL}/api/v1/management/stop`,
                    {
                        method: "POST",
                        headers: { Authorization: `Bearer ${bearerToken}` },
                    }
                );
            }
            catch
            {
                serverProcess.kill ();
            }
        }

        await rm ( dataDirectory, { recursive: true, force: true } );
    }
);

test ( "accepts the complete Pages-origin workflow through the real Java server", async ( { context, page } ) =>
{
    await context.grantPermissions ( [ "local-network-access" ], { origin: PAGES_ORIGIN } );
    await openInterceptedOrigin ( page, PAGES_ORIGIN );

    const developmentToolsSession = await context.newCDPSession ( page );
    const networkFailures: string[] = [];

    await developmentToolsSession.send ( "Network.enable" );
    developmentToolsSession.on ( "Network.loadingFailed", event =>
    {
        networkFailures.push (
            JSON.stringify (
                {
                    errorText: event.errorText,
                    blockedReason: event.blockedReason,
                    corsErrorStatus: event.corsErrorStatus,
                }
            )
        );
    } );

    let result: BrowserWorkflowResult;

    try
    {
        result = await page.evaluate <BrowserWorkflowResult, {
            serverBaseURL: string;
            bearerToken: string;
            modelDocument: string;
        }> (
            async input =>
            {
            const request = async ( path: string, options: RequestInit = {} ): Promise <Response> =>
            {
                const fetchOptions: RequestInit =
                {
                    ...options,
                    mode: "cors",
                };

                return fetch ( `${input.serverBaseURL}${path}`, fetchOptions );
            };
            const livenessResponse = await request ( "/api/v1/health/live" );
            const readinessResponse = await request ( "/api/v1/health/ready" );
            const pullResponse = await request ( "/api/v1/model" );
            const pullEntityTag = pullResponse.headers.get ( "ETag" );
            const pullRevision = pullResponse.headers.get ( "X-Model-Revision" );

            await pullResponse.text ();

            const pushResponse = await request (
                "/api/v1/model",
                {
                    method: "PUT",
                    headers:
                    {
                        Authorization: `Bearer ${input.bearerToken}`,
                        "Content-Type": "application/json",
                    },
                    body: input.modelDocument,
                }
            );
            const pushEntityTag = pushResponse.headers.get ( "ETag" );

            await pushResponse.text ();

            const evaluationResponse = await request (
                "/api/v1/evaluations",
                {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify (
                        {
                            eventId: "event-order-product",
                            payload:
                            {
                                "parameter-quantity": 1,
                                "parameter-delivery-type": "STANDARD",
                                "parameter-product-category": "RETAIL",
                                "parameter-region": "LOCAL",
                                "parameter-vip": false,
                            },
                        }
                    ),
                }
            );
            const evaluation = await evaluationResponse.json () as {
                outcome: string;
                actionId: string;
            };

            return {
                livenessStatus: livenessResponse.status,
                readinessStatus: readinessResponse.status,
                pullStatus: pullResponse.status,
                pullEntityTag,
                pullRevision,
                pushStatus: pushResponse.status,
                pushEntityTag,
                evaluationStatus: evaluationResponse.status,
                evaluationOutcome: evaluation.outcome,
                evaluationActionIdentifier: evaluation.actionId,
            };
            },
            { serverBaseURL, bearerToken, modelDocument }
        );
    }
    catch ( error )
    {
        throw new Error (
            `${error instanceof Error ? error.message : String ( error )}\n${networkFailures.join ( "\n" )}`
        );
    }

    expect ( result ).toMatchObject (
        {
            livenessStatus: 200,
            readinessStatus: 200,
            pullStatus: 200,
            pushStatus: 200,
            evaluationStatus: 200,
            evaluationOutcome: "ACTION",
            evaluationActionIdentifier: "action-local-courier",
        }
    );
    expect ( result.pullEntityTag ).toMatch ( /^"sha256:[0-9a-f]{64}"$/ );
    expect ( result.pullRevision ).toMatch ( /^sha256:[0-9a-f]{64}$/ );
    expect ( result.pushEntityTag ).toMatch ( /^"sha256:[0-9a-f]{64}"$/ );
} );

test ( "keeps an unconfigured secure origin unreadable and observable as a browser block", async ( { page } ) =>
{
    await openInterceptedOrigin ( page, DENIED_ORIGIN );

    const failure = await page.evaluate <string, string> (
        async targetURL =>
        {
            try
            {
                const options: RequestInit =
                {
                    mode: "cors",
                };

                await fetch ( targetURL, options );
                return "unexpected-success";
            }
            catch ( error )
            {
                return error instanceof Error ? error.name : String ( error );
            }
        },
        `${serverBaseURL}/api/v1/health/live`
    );

    expect ( failure ).toBe ( "TypeError" );
} );

test ( "fails safely when the allowed Pages origin lacks local-network permission", async ( { page } ) =>
{
    await openInterceptedOrigin ( page, PAGES_ORIGIN );

    const result = await page.evaluate <{ failure: string; permission: string }, string> (
        async targetURL =>
        {
            const permission = await navigator.permissions.query (
                { name: "local-network-access" as PermissionName }
            );

            try
            {
                await fetch ( targetURL, { mode: "cors" } );
                return { failure: "unexpected-success", permission: permission.state };
            }
            catch ( error )
            {
                return {
                    failure: error instanceof Error ? error.name : String ( error ),
                    permission: permission.state,
                };
            }
        },
        `${serverBaseURL}/api/v1/health/live`
    );

    expect ( result.permission ).not.toBe ( "granted" );
    expect ( result.failure ).toBe ( "TypeError" );
} );
