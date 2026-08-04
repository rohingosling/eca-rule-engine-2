// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    phase-20-browser.spec
// Version: 2.0.0
// Date:    2026-08-04
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies the complete multi-engine workflow, keyboard models, recovery boundaries, CSP, accessibility, and visuals.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";

interface CrashWindow extends Window
{
    __crashBuiltInWorker?: () => void;
}

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

async function verifyAccessibility ( page: Page ): Promise <void>
{
    const accessibilityResults = await new AxeBuilder ( { page } )
        .withTags ( [ "wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa" ] )
        .analyze ();

    expect ( accessibilityResults.violations ).toEqual ( [] );
}

test ( "completes the built-in workflow without accessibility or CSP failures", async ( { page } ) =>
{
    const browserErrors: string[] = [];

    page.on ( "console", message =>
    {
        if ( message.type () === "error" )
        {
            browserErrors.push ( message.text () );
        }
    } );

    await page.goto ( "./" );
    await waitForBuiltInServer ( page );
    await openMenuCommand ( page, "File", "Pull Model from Server" );
    await page.getByRole ( "tab", { name: "Simulator" } ).click ();
    await page.locator ( "#simulation-event" ).selectOption ( "event-cancel-order" );
    await page.getByRole ( "button", { name: "Evaluate" } ).click ();

    await expect ( page.getByLabel ( "Outcome" ) ).toHaveText ( "NO_ACTION" );
    await expect ( page.getByRole ( "status", { name: "Action" } ) ).toHaveText ( "Not applicable" );
    await verifyAccessibility ( page );

    const contentSecurityPolicy = await page.locator ( "meta[http-equiv='Content-Security-Policy']" )
        .getAttribute ( "content" );

    expect ( contentSecurityPolicy ).toContain ( "script-src 'self'" );
    expect ( contentSecurityPolicy ).toContain ( "worker-src 'self'" );
    expect ( browserErrors ).toEqual ( [] );
} );

test ( "supports menu, tree, tab, grid, splitter, and dialog keyboard models", async ( { page } ) =>
{
    await page.goto ( "./" );
    await waitForBuiltInServer ( page );

    await page.keyboard.press ( "F10" );
    await expect ( page.getByRole ( "menuitem", { name: "File", exact: true } ) ).toBeFocused ();
    await page.keyboard.press ( "ArrowDown" );
    await expect ( page.getByRole ( "menuitem", { name: "New", exact: true } ) ).toBeFocused ();
    await page.keyboard.press ( "ArrowRight" );
    await expect ( page.getByRole ( "menuitem", { name: "Cut", exact: true } ) ).toBeFocused ();
    await page.keyboard.press ( "ArrowLeft" );
    await expect ( page.getByRole ( "menuitem", { name: "New", exact: true } ) ).toBeFocused ();
    await page.keyboard.press ( "Escape" );
    await expect ( page.getByRole ( "menuitem", { name: "File", exact: true } ) ).toBeFocused ();

    const modelTreeItem = page.getByRole ( "treeitem", { name: "ECA Model", exact: true } );

    await modelTreeItem.focus ();
    await modelTreeItem.press ( "ArrowDown" );

    const parametersTreeItem = page.getByRole ( "treeitem", { name: "Parameters", exact: true } );
    const payloadsTreeItem = page.getByRole ( "treeitem", { name: "Payloads", exact: true } );

    await expect ( parametersTreeItem ).toHaveAttribute ( "aria-selected", "true" );
    await parametersTreeItem.press ( "ArrowDown" );
    await expect ( payloadsTreeItem ).toBeFocused ();
    await expect ( payloadsTreeItem ).toHaveAttribute ( "aria-selected", "true" );
    await payloadsTreeItem.press ( "ArrowUp" );
    await expect ( parametersTreeItem ).toBeFocused ();
    await parametersTreeItem.click ();
    await expect ( parametersTreeItem ).toBeFocused ();

    const editorTab = page.getByRole ( "tab", { name: "ECA Model Editor" } );

    await editorTab.focus ();
    await editorTab.press ( "ArrowRight" );
    await expect ( page.getByRole ( "tab", { name: "Simulator" } ) ).toHaveAttribute ( "aria-selected", "true" );

    const outlineSplitter = page.getByRole ( "separator", { name: "Resize Outline" } );
    const originalValue = Number ( await outlineSplitter.getAttribute ( "aria-valuenow" ) );

    await outlineSplitter.focus ();
    await outlineSplitter.press ( "ArrowRight" );
    await expect ( outlineSplitter ).toHaveAttribute ( "aria-valuenow", String ( originalValue + 10 ) );
    await outlineSplitter.press ( "End" );
    await expect ( outlineSplitter ).toHaveAttribute (
        "aria-valuenow",
        await outlineSplitter.getAttribute ( "aria-valuemax" ) ?? ""
    );
    await outlineSplitter.press ( "Home" );
    await expect ( outlineSplitter ).toHaveAttribute (
        "aria-valuenow",
        await outlineSplitter.getAttribute ( "aria-valuemin" ) ?? ""
    );

    await openMenuCommand ( page, "File", "Pull Model from Server" );
    await page.getByRole ( "treeitem", { name: "Parameters", exact: true } ).click ();

    const gridRows = page.getByRole ( "grid" ).getByRole ( "row" );
    const firstDataRow = gridRows.nth ( 1 );
    const lastDataRow = gridRows.last ();

    await firstDataRow.focus ();
    await firstDataRow.press ( "End" );
    await expect ( lastDataRow ).toHaveAttribute ( "aria-selected", "true" );

    await page.getByRole ( "button", { name: "Add", exact: true } ).click ();
    const entityIdentifier = page.locator ( "#entity-identifier" );

    await entityIdentifier.fill ( "Invalid ID" );
    await page.getByRole ( "button", { name: "Apply", exact: true } ).click ();
    await expect ( entityIdentifier ).toHaveAttribute ( "aria-invalid", "true" );
    await expect ( entityIdentifier ).toBeFocused ();
    const errorDescriptionIdentifier = await entityIdentifier.getAttribute ( "aria-describedby" );

    expect ( errorDescriptionIdentifier ).not.toBeNull ();
    await expect ( page.locator ( `[id="${errorDescriptionIdentifier ?? "missing-error"}"]` ) )
        .toContainText ( "lowercase words" );
    await page.getByRole ( "button", { name: "Cancel", exact: true } ).click ();

    await openMenuCommand ( page, "File", "Settings" );
    await expect ( page.getByRole ( "dialog", { name: "Settings" } ) ).toBeVisible ();
    await expect ( page.locator ( "#settings-group-selector" ) ).toBeFocused ();
    await page.keyboard.press ( "Escape" );
    await expect ( page.getByRole ( "menuitem", { name: "File", exact: true } ) ).toBeFocused ();
} );

test ( "survives unavailable storage, reload, and history navigation", async ( { page } ) =>
{
    await page.addInitScript (
        () =>
        {
            Object.defineProperty ( Storage.prototype, "getItem", {
                configurable: true,
                value: () => { throw new DOMException ( "Storage blocked", "SecurityError" ); },
            } );
            Object.defineProperty ( Storage.prototype, "setItem", {
                configurable: true,
                value: () => { throw new DOMException ( "Storage blocked", "SecurityError" ); },
            } );
        }
    );

    await page.goto ( "./" );
    await waitForBuiltInServer ( page );
    await expect ( page.getByRole ( "log" ) ).toContainText ( "Preference storage is unavailable" );
    await page.getByLabel ( "Name", { exact: true } ).fill ( "Navigation-safe model" );
    await page.getByRole ( "button", { name: "Apply Details" } ).click ();
    await page.evaluate ( () => window.history.pushState ( { phase: 20 }, "", "?phase=20" ) );
    await page.goBack ();
    await expect ( page.getByLabel ( "Name", { exact: true } ) ).toHaveValue ( "Navigation-safe model" );

    await page.reload ();
    await waitForBuiltInServer ( page );
    await expect ( page.getByLabel ( "Name", { exact: true } ) ).toHaveValue ( "Untitled" );
} );

test ( "recovers the browser-local server after a simulated worker crash", async ( { page } ) =>
{
    await page.addInitScript (
        () =>
        {
            const crashWindow = window as CrashWindow;
            const NativeWorker = window.Worker;

            class CrashableWorker extends NativeWorker
            {
                public constructor ( scriptURL: string | URL, options?: WorkerOptions )
                {
                    super ( scriptURL, options );

                    if ( crashWindow.__crashBuiltInWorker === undefined )
                    {
                        crashWindow.__crashBuiltInWorker = () =>
                        {
                            const errorHandler = this.onerror;

                            this.terminate ();
                            if ( typeof errorHandler === "function" )
                            {
                                errorHandler.call (
                                    this,
                                    new ErrorEvent ( "error", { message: "Simulated built-in worker crash." } )
                                );
                            }
                        };
                    }
                }
            }

            Object.defineProperty ( window, "Worker", { configurable: true, value: CrashableWorker } );
        }
    );

    await page.goto ( "./" );
    await waitForBuiltInServer ( page );
    await page.evaluate ( () => ( window as CrashWindow ).__crashBuiltInWorker?.() );
    await openServerSettings ( page );
    const builtInStatus = page.getByRole ( "status", { name: "Status", exact: true } );

    await expect ( builtInStatus ).toHaveText ( "Unavailable" );
    await page.getByRole ( "button", { name: "Reset to Example Model" } ).click ();
    await expect ( page.getByRole ( "log" ) ).toContainText ( "[Built-in Server] Reset to" );
    await expect ( builtInStatus ).toHaveText ( "Ready" );
} );

test ( "reflows at the supported 200-percent-equivalent viewport", async ( { page } ) =>
{
    await page.setViewportSize ( { width: 640, height: 560 } );
    await page.goto ( "./" );
    await waitForBuiltInServer ( page );

    const dimensions = await page.evaluate ( () => ( {
        clientWidth: document.documentElement.clientWidth,
        scrollWidth: document.documentElement.scrollWidth,
    } ) );

    expect ( dimensions.scrollWidth ).toBeLessThanOrEqual ( dimensions.clientWidth );
    await expect ( page.getByRole ( "tab", { name: "Simulator" } ) ).toBeVisible ();
    await verifyAccessibility ( page );
} );

test ( "matches the reviewed Light and Dark browser baselines", async ( { page } ) =>
{
    await page.goto ( "./" );
    await waitForBuiltInServer ( page );
    await openMenuCommand ( page, "File", "Pull Model from Server" );

    await page.getByRole ( "menuitem", { name: "View", exact: true } ).click ();
    await page.getByRole ( "menuitem", { name: "Theme", exact: true } ).hover ();
    await page.getByRole ( "menuitemradio", { name: "Light", exact: true } ).click ();
    await expect ( page ).toHaveScreenshot ( "shell-light.png" );

    await page.getByRole ( "menuitem", { name: "View", exact: true } ).click ();
    await page.getByRole ( "menuitem", { name: "Theme", exact: true } ).hover ();
    await page.getByRole ( "menuitemradio", { name: "Dark", exact: true } ).click ();
    await expect ( page ).toHaveScreenshot ( "shell-dark.png" );
} );
