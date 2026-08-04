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
    await expect ( page.getByRole ( "log" ) ).toContainText ( "[Built-in Server] Ready" );
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
    await expect ( page.getByRole ( "log" ) ).toContainText ( "[Server] Pulled eca-rule-engine-example" );
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
    await expect ( page.getByRole ( "log" ) ).toContainText ( "[Server] Pulled eca-rule-engine-example" );
    await page.getByLabel ( "Name", { exact: true } ).fill ( "Browser-local replacement" );
    await page.getByRole ( "button", { name: "Apply Details" } ).click ();
    await expect ( page.getByLabel ( "Name", { exact: true } ) ).toHaveValue ( "Browser-local replacement" );
    await openMenuCommand ( page, "File", "Push Model to Server" );
    await expect ( page.getByRole ( "log" ) ).toContainText ( "[Server] Pushed eca-rule-engine-example" );

    await page.getByRole ( "tab", { name: "Simulator" } ).click ();
    await expect ( page.getByLabel ( "Reconciliation" ) ).toHaveText ( "Local and server revisions match." );

    await openServerSettings ( page );
    await expect ( page.getByLabel ( "Hosted model" ) ).toHaveText ( "eca-rule-engine-example" );
    await page.getByRole ( "button", { name: "Test Connection" } ).click ();
    await expect ( page.getByRole ( "log" ) ).toContainText ( "Connection test passed" );
    await page.getByRole ( "button", { name: "Reset to Example Model" } ).click ();
    await expect ( page.getByRole ( "log" ) ).toContainText ( "[Built-in Server] Reset to eca-rule-engine-example" );
    await page.getByRole ( "button", { name: "Cancel", exact: true } ).click ();
    await page.getByRole ( "tab", { name: "ECA Model Editor" } ).click ();
    await expect ( page.getByLabel ( "Name", { exact: true } ) ).toHaveValue ( "Browser-local replacement" );
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

    await expect ( page.getByRole ( "contentinfo" ) ).toContainText ( "Real ECA Server (Experimental)" );

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
    await expect ( page.getByRole ( "contentinfo" ) ).toContainText ( "Built-in Demo Server (browser-local)" );
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

    await openMenuCommand ( page, "File", "Test Connection" );
    const cancelButton = page.getByRole ( "button", { name: "Cancel background operation" } );

    await expect ( cancelButton ).toBeVisible ();
    await cancelButton.click ();
    await expect ( page.getByRole ( "log" ) ).toContainText ( "Operation cancelled." );

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
