// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    phase-21-browser.spec
// Version: 2.0.0
// Date:    2026-08-04
// Author:  Rohin Gosling
//
// Description:
//
//   Smokes the exact local or published Pages artifact, repository-subpath assets, and visitor demo workflow.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { expect, test, type Page } from "@playwright/test";

const PROJECT_PATH = "/eca-rule-engine-2/";

async function openMenuCommand ( page: Page, menuName: string, commandName: string ): Promise <void>
{
    await page.getByRole ( "menuitem", { name: menuName, exact: true } ).click ();
    await page.getByRole ( "menuitem", { name: commandName, exact: true } ).click ();
}

async function waitForBuiltInServer ( page: Page ): Promise <void>
{
    await expect ( page.getByRole ( "log" ) ).toContainText ( "[Built-in Server] Ready" );
}

async function pullExampleModel ( page: Page ): Promise <void>
{
    await openMenuCommand ( page, "File", "Pull Model from Server" );
    await expect ( page.getByRole ( "log" ) ).toContainText ( "[Server] Pulled eca-rule-engine-example" );
    await expect ( page.getByLabel ( "Name", { exact: true } ) ).toHaveValue ( "ECA Rule Engine Example" );
}

test ( "loads the repository-path artifact and starts the browser-local server", async ( { page } ) =>
{
    const failedResources: string[] = [];

    page.on ( "requestfailed", request =>
    {
        failedResources.push ( request.url () );
    } );

    page.on ( "response", response =>
    {
        const responseURL = new URL ( response.url () );

        if ( responseURL.origin === new URL ( page.url () ).origin && response.status () >= 400 )
        {
            failedResources.push ( `${response.status ()} ${responseURL.pathname}` );
        }
    } );

    const navigationResponse = await page.goto ( "./" );

    expect ( navigationResponse?.status () ).toBe ( 200 );
    await waitForBuiltInServer ( page );

    const applicationURL  = new URL ( page.url () );
    const resourceURLs    = await page.evaluate (
        () => performance.getEntriesByType ( "resource" ).map ( entry => entry.name )
    );

    expect ( applicationURL.pathname ).toBe ( PROJECT_PATH );
    expect ( resourceURLs.length ).toBeGreaterThan ( 1 );
    expect ( failedResources ).toEqual ( [] );

    for ( const resourceURL of resourceURLs )
    {
        const parsedResourceURL = new URL ( resourceURL );

        expect ( parsedResourceURL.origin ).toBe ( applicationURL.origin );
        expect ( parsedResourceURL.pathname.startsWith ( PROJECT_PATH ) ).toBe ( true );
    }

    if ( process.env.ECA_PHASE_21_BASE_URL !== undefined )
    {
        expect ( applicationURL.protocol ).toBe ( "https:" );
    }
} );

test ( "matches the desktop Help menu and About dialog", async ( { page } ) =>
{
    await page.goto ( "./" );
    const helpMenuButton = page.getByRole ( "menuitem", { name: "Help", exact: true } );

    await helpMenuButton.click ();
    const helpMenu = page.getByRole ( "menu", { name: "Help", exact: true } );

    await expect ( helpMenu.getByRole ( "menuitem", { name: "Simulator Guide", exact: true } ) ).toHaveCount ( 0 );
    await helpMenu.getByRole ( "menuitem", { name: "About", exact: true } ).click ();

    const aboutDialog = page.getByRole ( "dialog", { name: "About", exact: true } );

    await expect ( aboutDialog ).toBeVisible ();
    await expect ( aboutDialog.locator ( ".about-application-name" ) ).toHaveText ( "ECA Rule Engine Laboratory" );
    await expect ( aboutDialog ).toContainText ( "Version 2.0.0" );
    await expect ( aboutDialog ).toContainText ( "Rohin Gosling (2024)" );
    await expect ( aboutDialog.locator ( ".about-description" ) ).toHaveText (
        "Domain-independent, stateless, ECA (Event Condition Action) Rule Engine modeling, testing and " +
        "simulation environment."
    );
    await expect ( aboutDialog.getByRole ( "button", { name: "OK", exact: true } ) ).toBeFocused ();

    const desktopParityLayout = await aboutDialog.evaluate (
        dialog =>
        {
            const content = dialog.querySelector <HTMLElement> ( ".about-content" );
            const identity = dialog.querySelector <HTMLElement> ( ".about-identity" );
            const applicationName = dialog.querySelector <HTMLElement> ( ".about-application-name" );

            return {
                applicationNameWeight: applicationName === null
                    ? ""
                    : window.getComputedStyle ( applicationName ).fontWeight,
                contentGap: content === null ? "" : window.getComputedStyle ( content ).gap,
                contentWidth: content?.getBoundingClientRect ().width ?? 0,
                identityGap: identity === null ? "" : window.getComputedStyle ( identity ).gap,
            };
        }
    );

    expect ( desktopParityLayout.applicationNameWeight ).toBe ( "700" );
    expect ( desktopParityLayout.contentGap ).toBe ( "12px" );
    expect ( desktopParityLayout.contentWidth ).toBe ( 480 );
    expect ( desktopParityLayout.identityGap ).toBe ( "2px" );

    await aboutDialog.getByRole ( "button", { name: "Close", exact: true } ).click ();
    await expect ( aboutDialog ).not.toBeVisible ();
    await expect ( helpMenuButton ).toBeFocused ();

    await helpMenuButton.click ();
    await page.getByRole ( "menuitem", { name: "About", exact: true } ).click ();
    await page.keyboard.press ( "Escape" );
    await expect ( aboutDialog ).not.toBeVisible ();
    await expect ( helpMenuButton ).toBeFocused ();

    await helpMenuButton.click ();
    await page.getByRole ( "menuitem", { name: "About", exact: true } ).click ();
    await aboutDialog.getByRole ( "button", { name: "OK", exact: true } ).click ();
    await expect ( aboutDialog ).not.toBeVisible ();
    await expect ( helpMenuButton ).toBeFocused ();
} );

test ( "completes Pull, courier ACTION, and cancellation NO_ACTION", async ( { page } ) =>
{
    await page.goto ( "./" );
    await waitForBuiltInServer ( page );
    await pullExampleModel ( page );
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

    await page.locator ( "#simulation-event" ).selectOption ( "event-cancel-order" );
    await page.getByRole ( "button", { name: "Evaluate" } ).click ();
    await expect ( page.getByLabel ( "Outcome" ) ).toHaveText ( "NO_ACTION" );
    await expect ( page.getByRole ( "status", { name: "Action" } ) ).toHaveText ( "Not applicable" );
} );

test ( "starts a fresh example server after refresh", async ( { page } ) =>
{
    await page.goto ( "./" );
    await waitForBuiltInServer ( page );
    await pullExampleModel ( page );
    await page.getByLabel ( "Name", { exact: true } ).fill ( "Page-session replacement" );
    await page.getByRole ( "button", { name: "Apply Details" } ).click ();
    await openMenuCommand ( page, "File", "Push Model to Server" );
    await expect ( page.getByRole ( "log" ) ).toContainText ( "[Server] Pushed eca-rule-engine-example" );

    await page.reload ();
    await waitForBuiltInServer ( page );
    await pullExampleModel ( page );
} );
