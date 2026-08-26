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
    await expect ( page.getByRole ( "region", { name: "Console" } ) ).toContainText ( "Built-in ServerReady" );
}

async function pullExampleModel ( page: Page ): Promise <void>
{
    await openMenuCommand ( page, "File", "Pull Model from Server" );
    await expect ( page.getByRole ( "region", { name: "Console" } ) )
        .toContainText ( "ServerPulled eca-rule-engine-example" );
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

test ( "matches the Automata Lab About identity and modal behavior", async ( { page } ) =>
{
    await page.goto ( "./" );
    const helpMenuButton = page.getByRole ( "menuitem", { name: "Help", exact: true } );

    await helpMenuButton.click ();
    const helpMenu = page.getByRole ( "menu", { name: "Help", exact: true } );

    await expect ( helpMenu.getByRole ( "menuitem", { name: "Simulator Guide", exact: true } ) ).toHaveCount ( 0 );
    await helpMenu.getByRole ( "menuitem", { name: "About", exact: true } ).click ();

    const aboutDialog = page.getByRole ( "dialog", { name: "About", exact: true } );

    await expect ( aboutDialog ).toBeVisible ();
    await expect ( aboutDialog.locator ( ".about-identity strong" ) ).toHaveText ( "ECA Rule Engine Laboratory" );
    await expect ( aboutDialog.locator ( ".about-identity span" ) ).toHaveText (
        "Version 2.1.0 (Rohin Gosling 2024)"
    );
    await expect ( aboutDialog.getByText (
        "Domain-independent, stateless, ECA (Event Condition Action) Rule Engine modeling, testing and " +
        "simulation environment."
    ) ).toBeVisible ();
    await expect ( aboutDialog.getByRole ( "button", { name: "Close", exact: true } ) ).toBeFocused ();

    const desktopParityLayout = await aboutDialog.evaluate (
        dialog =>
        {
            const identity = dialog.querySelector <HTMLElement> ( ".about-identity" );
            const applicationIcon = dialog.querySelector <HTMLElement> ( ".about-application-icon" );
            const applicationName = dialog.querySelector <HTMLElement> ( ".about-identity strong" );
            const identityText = dialog.querySelector <HTMLElement> ( ".about-identity > div" );

            const iconBounds = applicationIcon?.getBoundingClientRect ();
            const identityTextBounds = identityText?.getBoundingClientRect ();

            return {
                applicationNameWeight: applicationName === null
                    ? ""
                    : window.getComputedStyle ( applicationName ).fontWeight,
                identityGap: identity === null ? "" : window.getComputedStyle ( identity ).gap,
                identityTextLineCount: identityText?.children.length ?? 0,
                identityWidth: identity?.getBoundingClientRect ().width ?? 0,
                iconHeight: applicationIcon?.getBoundingClientRect ().height ?? 0,
                iconLeft: iconBounds?.left ?? 0,
                identityTextLeft: identityTextBounds?.left ?? 0,
            };
        }
    );

    expect ( desktopParityLayout.applicationNameWeight ).toBe ( "700" );
    expect ( desktopParityLayout.identityGap ).toBe ( "12px" );
    expect ( desktopParityLayout.identityTextLineCount ).toBe ( 2 );
    expect ( desktopParityLayout.identityWidth ).toBeGreaterThan ( 250 );
    expect ( desktopParityLayout.iconHeight ).toBe ( 40 );
    expect ( desktopParityLayout.identityTextLeft ).toBeGreaterThan ( desktopParityLayout.iconLeft );

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
    await aboutDialog.getByRole ( "button", { name: "Close", exact: true } ).click ();
    await expect ( aboutDialog ).not.toBeVisible ();
    await expect ( helpMenuButton ).toBeFocused ();
} );

test ( "uses desktop-style cursors for composite controls and text cursors only for editable text", async ( { page } ) =>
{
    await page.goto ( "./" );

    const defaultCursorControls =
    [
        page.getByRole ( "menuitem", { name: "File", exact: true } ),
        page.getByRole ( "tab", { name: "ECA Model Editor", exact: true } ),
        page.getByRole ( "treeitem" ).first (),
        page.locator ( ".tree-disclosure" ).first (),
        page.locator ( ".console-row" ).first (),
        page.locator ( "input[type='checkbox']" ).first (),
    ];

    for ( const control of defaultCursorControls )
    {
        await expect ( control ).toHaveCSS ( "cursor", "default" );
    }

    await expect ( page.locator ( ".splitter-vertical" ).first () )
        .toHaveCSS ( "cursor", "col-resize" );
    await expect ( page.locator ( ".splitter-horizontal" ).first () )
        .toHaveCSS ( "cursor", "row-resize" );
    await expect ( page.locator ( "input[type='text']" ).first () )
        .toHaveCSS ( "cursor", "text" );
    await expect ( page.locator ( ".user-guide-links a" ).first () )
        .toHaveCSS ( "cursor", "pointer" );

    await page.getByRole ( "menuitem", { name: "Help", exact: true } ).click ();
    await page.getByRole ( "menuitem", { name: "About", exact: true } ).click ();
    await expect ( page.getByRole ( "dialog", { name: "About", exact: true } ).locator ( "textarea" ).first () )
        .toHaveCSS ( "cursor", "text" );
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
    await expect ( page.getByRole ( "region", { name: "Console" } ) )
        .toContainText ( "ServerPushed eca-rule-engine-example" );

    await page.reload ();
    await waitForBuiltInServer ( page );
    await pullExampleModel ( page );
} );
