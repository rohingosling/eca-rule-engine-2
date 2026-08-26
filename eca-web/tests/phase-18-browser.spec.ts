// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    phase-18-browser.spec
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies the browser-local Pull/Push/Simulator workflow, settings targets, cancellation, privacy, and accessibility.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";

async function openMenuCommand ( page: Page, menuName: string, commandName: string ): Promise <void>
{
    await page.getByRole ( "menuitem", { name: menuName, exact: true } ).click ();
    await page.getByRole ( "menuitem", { name: commandName, exact: true } ).click ();
}

async function openServerSettings ( page: Page ): Promise <void>
{
    await openMenuCommand ( page, "File", "Settings" );
    await page.locator ( "#settings-group-selector" ).selectOption ( "server" );
}

async function waitForBuiltInServer ( page: Page ): Promise <void>
{
    await expect ( page.getByRole ( "region", { name: "Console" } ) ).toContainText ( "Built-in ServerReady" );
}

test.beforeEach (
    async ( { page } ) =>
    {
        await page.goto ( "./" );
        await page.evaluate ( () => window.localStorage.clear () );
        await page.reload ();
        await waitForBuiltInServer ( page );
    }
);

test ( "completes Pull, courier ACTION, and cancellation NO_ACTION through the dedicated worker", async ( { page } ) =>
{
    await openMenuCommand ( page, "File", "Pull Model from Server" );
    await expect ( page.getByRole ( "region", { name: "Console" } ) )
        .toContainText ( "ServerPulled eca-rule-engine-example" );
    await expect ( page.getByLabel ( "Name", { exact: true } ) ).toHaveValue ( "ECA Rule Engine Example" );

    await page.getByRole ( "tab", { name: "Simulator" } ).click ();
    await page.locator ( "#simulation-event" ).selectOption ( "event-order-product" );

    const concreteValues: Readonly <Record <string, string>> =
    {
        "parameter-delivery-type": "STANDARD",
        "parameter-product-category": "RETAIL",
        "parameter-quantity": "1",
        "parameter-region": "LOCAL",
        "parameter-vip": "false",
    };

    for ( const [ parameterIdentifier, value ] of Object.entries ( concreteValues ) )
    {
        await page.locator ( `#simulation-${parameterIdentifier}-state` ).selectOption ( "CONCRETE" );
        const valueControl = page.locator ( `#simulation-${parameterIdentifier}-value` );

        if ( await valueControl.evaluate ( element => element instanceof HTMLSelectElement ) )
        {
            await valueControl.selectOption ( value );
        }
        else
        {
            await valueControl.fill ( value );
        }
    }

    await page.getByRole ( "button", { name: "Evaluate" } ).click ();
    await expect ( page.getByLabel ( "Outcome" ) ).toHaveText ( "ACTION" );
    await expect ( page.getByRole ( "status", { name: "Action" } ) ).toHaveText ( "action-local-courier" );
    await expect ( page.getByLabel ( "Winning rule" ) ).toHaveText ( "rule-test" );
    await expect ( page.getByLabel ( "Specificity" ) ).toHaveText ( "9" );
    await expect ( page.getByLabel ( "Reconciliation" ) ).toHaveText ( "Local and server revisions match." );

    await page.locator ( "#simulation-event" ).selectOption ( "event-cancel-order" );
    await page.getByRole ( "button", { name: "Evaluate" } ).click ();
    await expect ( page.getByLabel ( "Outcome" ) ).toHaveText ( "NO_ACTION" );
    await expect ( page.getByRole ( "status", { name: "Action" } ) ).toHaveText ( "Not applicable" );
} );

test ( "pushes canonical edits, reconciles revisions, and resets only the browser-local server", async ( { page } ) =>
{
    await openMenuCommand ( page, "File", "Pull Model from Server" );
    await expect ( page.getByRole ( "region", { name: "Console" } ) )
        .toContainText ( "ServerPulled eca-rule-engine-example" );
    await page.getByLabel ( "Name", { exact: true } ).fill ( "Browser-local replacement" );
    await page.getByRole ( "button", { name: "Apply Details" } ).click ();
    await expect ( page.getByLabel ( "Name", { exact: true } ) ).toHaveValue ( "Browser-local replacement" );
    await openMenuCommand ( page, "File", "Push Model to Server" );
    await expect ( page.getByRole ( "region", { name: "Console" } ) )
        .toContainText ( "ServerPushed eca-rule-engine-example" );

    await page.getByRole ( "tab", { name: "Simulator" } ).click ();
    await expect ( page.getByLabel ( "Reconciliation" ) ).toHaveText ( "Local and server revisions match." );

    await openServerSettings ( page );
    await expect ( page.getByLabel ( "Hosted model" ) ).toHaveText ( "eca-rule-engine-example" );
    await page.getByRole ( "button", { name: "Test Connection" } ).click ();
    await expect ( page.getByRole ( "region", { name: "Console" } ) ).toContainText ( "Connection test passed" );
    await page.getByRole ( "button", { name: "Reset to Example Model" } ).click ();
    await expect ( page.getByRole ( "region", { name: "Console" } ) )
        .toContainText ( "Built-in ServerReset to eca-rule-engine-example" );
    await page.getByRole ( "button", { name: "Cancel", exact: true } ).click ();
    await page.getByRole ( "tab", { name: "ECA Model Editor" } ).click ();
    await expect ( page.getByLabel ( "Name", { exact: true } ) ).toHaveValue ( "Browser-local replacement" );
} );

test ( "fits full SHA-256 revisions in Settings and Console context buttons", async ( { page } ) =>
{
    await openServerSettings ( page );

    const settingsDialog = page.getByRole ( "dialog", { name: "Settings", exact: true } );
    const revisionValue = settingsDialog.getByLabel ( "Revision", { exact: true } );

    await expect ( revisionValue ).toHaveText ( /^sha256:[0-9a-f]{64}$/u );

    const settingsGeometry = await settingsDialog.evaluate ( element => ( {
        clientWidth: element.clientWidth,
        revisionClientWidth: ( element.querySelector ( ".settings-revision-value" ) as HTMLElement ).clientWidth,
        revisionScrollWidth: ( element.querySelector ( ".settings-revision-value" ) as HTMLElement ).scrollWidth,
    } ) );

    expect ( settingsGeometry.clientWidth ).toBeGreaterThanOrEqual ( 995 );
    expect ( settingsGeometry.revisionScrollWidth - settingsGeometry.revisionClientWidth ).toBeLessThanOrEqual ( 1 );

    await settingsDialog.getByRole ( "button", { name: "Cancel", exact: true } ).click ();
    await openMenuCommand ( page, "File", "Pull Model from Server" );

    const readyContextButton = page.locator ( ".console-context button" )
        .filter ( { hasText: /^Ready with /u } )
        .last ();
    const revisionContextButton = page.locator ( ".console-context button" )
        .filter ( { hasText: /Pulled .* at sha256:[0-9a-f]{64}/u } )
        .last ();

    await expect ( readyContextButton ).toBeVisible ();
    await expect ( revisionContextButton ).toBeVisible ();

    const contextGeometries = await Promise.all (
        [ readyContextButton, revisionContextButton ].map (
            button => button.evaluate (
                element =>
                {
                    const style = window.getComputedStyle ( element );

                    return {
                        backgroundColor: style.backgroundColor,
                        borderColor: style.borderColor,
                        borderRadius: style.borderRadius,
                        clientWidth: element.clientWidth,
                        color: style.color,
                        justifyContent: style.justifyContent,
                        minHeight: style.minHeight,
                        scrollWidth: element.scrollWidth,
                        textAlign: style.textAlign,
                    };
                }
            )
        )
    );
    const readyContextGeometry = contextGeometries [ 0 ]!;
    const revisionContextGeometry = contextGeometries [ 1 ]!;
    const clearButtonStyle = await page.getByRole ( "button", { name: "Clear", exact: true } ).evaluate (
        element =>
        {
            const style = window.getComputedStyle ( element );

            return {
                backgroundColor: style.backgroundColor,
                borderColor: style.borderColor,
                borderRadius: style.borderRadius,
                color: style.color,
            };
        }
    );

    expect ( revisionContextGeometry.clientWidth ).toBeGreaterThan ( readyContextGeometry.clientWidth );
    expect ( contextGeometries.every (
        geometry => geometry.scrollWidth - geometry.clientWidth <= 1
            && geometry.justifyContent === "center"
            && geometry.textAlign === "center"
            && geometry.minHeight === "21px"
    ) ).toBe ( true );
    expect ( contextGeometries ).toEqual ( expect.arrayContaining (
        [ expect.objectContaining ( clearButtonStyle ), expect.objectContaining ( clearButtonStyle ) ]
    ) );
} );

test ( "persists only non-sensitive real settings and keeps the token, document, payload, and results out of storage", async ( { page } ) =>
{
    const secretToken = "phase-18-secret-token";

    await openServerSettings ( page );
    await page.locator ( "#settings-server-target" ).selectOption ( "real" );
    await page.locator ( "#settings-server-url" ).fill ( "http://127.0.0.1:9999/" );
    await page.locator ( "#settings-connect-timeout" ).fill ( "7" );
    await page.locator ( "#settings-request-timeout" ).fill ( "31" );
    await page.locator ( "#settings-bearer-token" ).fill ( secretToken );
    await page.getByRole ( "button", { name: "Apply", exact: true } ).click ();

    await expect ( page.getByRole ( "dialog", { name: "Settings", exact: true } ) ).not.toBeVisible ();
    await expect ( page.getByRole ( "region", { name: "Console" } ) )
        .toContainText ( "Real ECA Server (Experimental) selected." );
    await expect ( page.getByRole ( "contentinfo", { name: "Application status" } ) )
        .toContainText ( "Server: Disconnected" );

    const storedData = await page.evaluate ( () => JSON.stringify ( window.localStorage ) );
    const storedPreferences = await page.evaluate <Record <string, unknown>> (
        () => JSON.parse ( window.localStorage.getItem ( "eca-shell-preferences-v1" ) ?? "{}" )
    );

    expect ( storedData ).not.toContain ( secretToken );
    expect ( storedData ).not.toContain ( "event-order-product" );
    expect ( storedData ).not.toContain ( "action-local-courier" );
    expect ( storedPreferences ).toMatchObject (
        {
            serverTarget: "real",
            serverURL: "http://127.0.0.1:9999/",
            connectTimeoutSeconds: 7,
            requestTimeoutSeconds: 31,
        }
    );
    expect ( storedPreferences ).not.toHaveProperty ( "bearerToken" );

    await openServerSettings ( page );
    await expect ( page.locator ( "#settings-bearer-token" ) ).toHaveValue ( secretToken );
    await page.locator ( "#settings-server-target" ).selectOption ( "built-in" );
    await page.getByRole ( "button", { name: "Apply", exact: true } ).click ();
    await expect ( page.getByRole ( "dialog", { name: "Settings", exact: true } ) ).not.toBeVisible ();
    await expect ( page.getByRole ( "contentinfo", { name: "Application status" } ) )
        .toContainText ( "Server: Connected" );
    await expect ( page.getByRole ( "region", { name: "Console" } ) )
        .toContainText ( "Built-in Demo Server selected." );
} );

test ( "validates hidden real-server fields and sanitizes unsafe stored preferences", async ( { page } ) =>
{
    await openServerSettings ( page );
    await page.locator ( "#settings-server-target" ).selectOption ( "real" );

    const settingsDialog = page.getByRole ( "dialog", { name: "Settings" } );
    const readFormLayout = () => settingsDialog.evaluate ( element =>
    {
        const labels = Array.from ( element.querySelectorAll <HTMLElement> ( ".form-field-label-text" ) );
        const valueOffsets = Array.from ( element.querySelectorAll <HTMLElement> (
            ".form-grid > input, .form-grid > output, .form-grid > select"
        ) ).map ( value => Math.round ( value.getBoundingClientRect ().left ) );

        return {
            labelColumnWidth: Number.parseFloat (
                getComputedStyle ( element ).getPropertyValue ( "--form-label-column-width" )
            ),
            longestLabelWidth: Math.max ( ...labels.map ( label => label.getBoundingClientRect ().width ) ),
            valueOffsets,
            whiteSpaceValues: labels.map ( label => getComputedStyle ( label ).whiteSpace ),
        };
    } );
    const initialLayout = await readFormLayout ();

    expect ( new Set ( initialLayout.valueOffsets ).size ).toBe ( 1 );
    expect ( initialLayout.labelColumnWidth ).toBe ( Math.ceil ( initialLayout.longestLabelWidth * 1.1 ) );
    expect ( new Set ( initialLayout.whiteSpaceValues ) ).toEqual ( new Set ( [ "nowrap" ] ) );

    await page.setViewportSize ( { width: 1000, height: 720 } );
    await expect.poll ( async () => ( await readFormLayout () ).labelColumnWidth )
        .toBe ( Math.ceil ( initialLayout.longestLabelWidth * 1.1 ) );

    await page.locator ( "#settings-server-url" ).fill ( "http://user:secret@127.0.0.1:9999/" );
    await page.locator ( "#settings-connect-timeout" ).fill ( "301" );
    await page.locator ( "#settings-request-timeout" ).fill ( "3601" );
    await page.getByRole ( "button", { name: "Apply", exact: true } ).click ();

    await expect ( page.getByRole ( "dialog", { name: "Settings", exact: true } ) ).toBeVisible ();
    await expect ( page.locator ( "#settings-server-url" ) ).toHaveAttribute ( "aria-invalid", "true" );
    await expect ( page.locator ( "#settings-connect-timeout" ) ).toHaveAttribute ( "aria-invalid", "true" );
    await expect ( page.locator ( "#settings-request-timeout" ) ).toHaveAttribute ( "aria-invalid", "true" );
    await expect ( page.locator ( "#settings-server-url" ) ).toBeFocused ();
    await expect ( page.locator ( "#settings-connect-timeout" ) ).toHaveAttribute ( "max", "300" );
    await expect ( page.locator ( "#settings-request-timeout" ) ).toHaveAttribute ( "max", "3600" );

    await page.locator ( "#settings-server-target" ).selectOption ( "built-in" );
    await expect ( page.getByRole ( "alert" ) ).toHaveCount ( 0 );
    await page.getByRole ( "button", { name: "Apply", exact: true } ).click ();
    await expect ( page.locator ( "#settings-server-target" ) ).toHaveValue ( "real" );
    await expect ( page.locator ( "#settings-server-url" ) ).toBeFocused ();
    await expect ( page.getByRole ( "alert" ) ).toContainText ( "Server URL must not contain credentials." );

    await page.getByRole ( "button", { name: "Cancel", exact: true } ).click ();
    await page.evaluate ( () =>
    {
        window.localStorage.setItem (
            "eca-shell-preferences-v1",
            JSON.stringify (
                {
                    serverTarget: "built-in",
                    serverURL: "https://user:secret@example.test/?token=unsafe",
                    connectTimeoutSeconds: 301,
                    requestTimeoutSeconds: 3601,
                }
            )
        );
    } );
    await page.reload ();
    await waitForBuiltInServer ( page );
    await openServerSettings ( page );
    await page.locator ( "#settings-server-target" ).selectOption ( "real" );
    await expect ( page.locator ( "#settings-server-url" ) ).toHaveValue ( "http://127.0.0.1:8080/" );
    await expect ( page.locator ( "#settings-connect-timeout" ) ).toHaveValue ( "300" );
    await expect ( page.locator ( "#settings-request-timeout" ) ).toHaveValue ( "3600" );

    const storedPreferences = await page.evaluate <Record <string, unknown>> (
        () => JSON.parse ( window.localStorage.getItem ( "eca-shell-preferences-v1" ) ?? "{}" )
    );

    expect ( storedPreferences ).toMatchObject (
        {
            serverURL: "http://127.0.0.1:8080/",
            connectTimeoutSeconds: 300,
            requestTimeoutSeconds: 3600,
        }
    );
} );

test ( "applies appearance-only settings without clearing evaluation or server state", async ( { page } ) =>
{
    await openMenuCommand ( page, "File", "Pull Model from Server" );
    await page.getByRole ( "tab", { name: "Simulator" } ).click ();
    await page.locator ( "#simulation-event" ).selectOption ( "event-cancel-order" );
    await page.getByRole ( "button", { name: "Evaluate" } ).click ();
    await expect ( page.getByLabel ( "Outcome" ) ).toHaveText ( "NO_ACTION" );

    const serverRevision = await page.getByLabel ( "Server", { exact: true } ).textContent ();

    await openMenuCommand ( page, "File", "Settings" );
    await page.getByLabel ( "Dark" ).check ();
    await page.locator ( "#settings-group-selector" ).selectOption ( "console" );
    await page.locator ( "input[name='settings-follow-tail']" ).uncheck ();
    await page.getByRole ( "button", { name: "Apply", exact: true } ).click ();
    await expect ( page.getByRole ( "dialog", { name: "Settings", exact: true } ) ).not.toBeVisible ();

    await expect ( page.getByLabel ( "Outcome" ) ).toHaveText ( "NO_ACTION" );
    await expect ( page.getByLabel ( "Server", { exact: true } ) ).toHaveText ( serverRevision ?? "" );
    await expect ( page.getByLabel ( "Reconciliation" ) ).toHaveText ( "Local and server revisions match." );
    await expect ( page.getByRole ( "contentinfo", { name: "Application status" } ) )
        .toContainText ( "Server: Connected" );
} );

test ( "preserves a connected target on cancellation and application-level responses", async ( { page } ) =>
{
    let delayConnection = false;

    await page.route ( "http://127.0.0.1:9999/**", async route =>
    {
        const path = new URL ( route.request ().url () ).pathname;

        if ( delayConnection && path.endsWith ( "/health/live" ) )
        {
            await new Promise ( resolve => setTimeout ( resolve, 1500 ) );
        }

        if ( path.endsWith ( "/health/live" ) )
        {
            await route.fulfill ( { status: 200, contentType: "application/json", body: '{"status":"UP"}' } );
            return;
        }

        if ( path.endsWith ( "/health/ready" ) )
        {
            await route.fulfill (
                {
                    status: 200,
                    contentType: "application/json",
                    body: '{"status":"READY","ready":true,"modelRevision":"sha256:test"}',
                }
            );
            return;
        }

        if ( path.endsWith ( "/model" ) )
        {
            await new Promise ( resolve => setTimeout ( resolve, 250 ) );
            await route.fulfill (
                {
                    status: 404,
                    contentType: "application/problem+json",
                    body: JSON.stringify (
                        {
                            type: "about:blank",
                            title: "No Model",
                            status: 404,
                            detail: "No model is hosted.",
                            instance: "/api/v1/model",
                        }
                    ),
                }
            );
            return;
        }

        await route.abort ();
    } );

    await openServerSettings ( page );
    await page.locator ( "#settings-server-target" ).selectOption ( "real" );
    await page.locator ( "#settings-server-url" ).fill ( "http://127.0.0.1:9999/" );
    await page.locator ( "#settings-connect-timeout" ).fill ( "30" );
    await page.locator ( "#settings-request-timeout" ).fill ( "30" );
    await page.getByRole ( "button", { name: "Apply", exact: true } ).click ();
    await expect ( page.getByRole ( "dialog", { name: "Settings", exact: true } ) ).not.toBeVisible ();
    await openMenuCommand ( page, "File", "Test Connection" );
    await expect ( page.getByRole ( "contentinfo", { name: "Application status" } ) )
        .toContainText ( "Server: Connected" );

    delayConnection = true;
    await openMenuCommand ( page, "File", "Test Connection" );
    const cancelButton = page.getByRole ( "button", { name: "Cancel background operation" } );

    await expect ( cancelButton ).toBeVisible ();
    await cancelButton.click ();
    await expect ( page.getByRole ( "region", { name: "Console" } ) ).toContainText ( "Operation cancelled." );
    await expect ( page.getByRole ( "contentinfo", { name: "Application status" } ) )
        .toContainText ( "Server: Connected" );

    delayConnection = false;
    await openMenuCommand ( page, "File", "Pull Model from Server" );
    await expect ( page.getByRole ( "contentinfo", { name: "Application status" } ) )
        .toContainText ( "Server: Connected" );
    await expect ( page.getByRole ( "region", { name: "Console" } ) ).toContainText ( "No Model" );
    await expect ( page.getByRole ( "contentinfo", { name: "Application status" } ) )
        .toContainText ( "Server: Connected" );
    const noModelDialog = page.getByRole ( "dialog", { name: "Error", exact: true } );

    await expect ( noModelDialog ).toContainText ( "No Model" );
    await noModelDialog.getByRole ( "button", { name: "OK", exact: true } ).click ();

    await openServerSettings ( page );
    await expect ( page.locator ( "#settings-server-target option[value='built-in']" ) ).toHaveText (
        "Built-in Demo Server"
    );
} );

test ( "cancels an in-flight real request and leaves the built-in target available", async ( { page } ) =>
{
    await page.route ( "http://127.0.0.1:9999/**", async route =>
    {
        await new Promise ( resolve => setTimeout ( resolve, 1500 ) );
        await route.fulfill (
            {
                status: 200,
                contentType: "application/json",
                body: '{"status":"UP"}',
            }
        );
    } );

    await openServerSettings ( page );
    await page.locator ( "#settings-server-target" ).selectOption ( "real" );
    await page.locator ( "#settings-server-url" ).fill ( "http://127.0.0.1:9999/" );
    await page.locator ( "#settings-connect-timeout" ).fill ( "30" );
    await page.locator ( "#settings-request-timeout" ).fill ( "30" );
    await page.getByRole ( "button", { name: "Apply", exact: true } ).click ();
    await expect ( page.getByRole ( "dialog", { name: "Settings", exact: true } ) ).not.toBeVisible ();

    await openMenuCommand ( page, "File", "Test Connection" );
    const cancelButton = page.getByRole ( "button", { name: "Cancel background operation" } );

    await expect ( cancelButton ).toBeVisible ();
    await cancelButton.click ();
    await expect ( page.getByRole ( "region", { name: "Console" } ) ).toContainText ( "Operation cancelled." );

    await openServerSettings ( page );
    await expect ( page.locator ( "#settings-server-target option[value='built-in']" ) ).toHaveText (
        "Built-in Demo Server"
    );
} );

test ( "has no automatic accessibility violations in the complete Simulator and Settings views", async ( { page } ) =>
{
    await openMenuCommand ( page, "File", "Pull Model from Server" );
    await page.getByRole ( "tab", { name: "Simulator" } ).click ();

    const simulatorAccessibility = await new AxeBuilder ( { page } )
        .withTags ( [ "wcag2a", "wcag2aa", "wcag21a", "wcag21aa" ] )
        .analyze ();

    expect ( simulatorAccessibility.violations ).toEqual ( [] );

    await openServerSettings ( page );

    const settingsAccessibility = await new AxeBuilder ( { page } )
        .withTags ( [ "wcag2a", "wcag2aa", "wcag21a", "wcag21aa" ] )
        .analyze ();

    expect ( settingsAccessibility.violations ).toEqual ( [] );
} );
