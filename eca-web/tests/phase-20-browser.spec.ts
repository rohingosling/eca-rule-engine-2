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
    await expect ( page.getByRole ( "region", { name: "Console" } ) ).toContainText ( "Built-in ServerReady" );
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

    const gridRows = page.getByRole ( "grid", { name: "Parameters" } ).getByRole ( "row" );
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
    await expect ( page.getByRole ( "region", { name: "Console" } ) )
        .toContainText ( "Preference storage is unavailable" );
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
    await expect ( page.getByRole ( "region", { name: "Console" } ) ).toContainText ( "Built-in ServerReset to" );
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

test ( "presents the Automata-style title, command, Console, tree, and status chrome", async ( { page } ) =>
{
    await page.goto ( "./" );
    await waitForBuiltInServer ( page );

    const shell = page.locator ( ".application-shell" );
    const toolbar = page.getByRole ( "toolbar", { name: "Application toolbar" } );

    await expect ( shell ).toHaveAttribute ( "data-theme", "dark" );
    await expect ( page.locator ( ".application-title-bar" ) ).toContainText ( "ECA Rule Engine Laboratory" );
    await expect ( toolbar.getByRole ( "button" ) ).toHaveCount ( 13 );

    const toolbarLabels = await toolbar.getByRole ( "button" ).evaluateAll (
        buttons => buttons.map ( button => button.getAttribute ( "aria-label" ) )
    );

    expect ( toolbarLabels ).toEqual (
        [
            "New",
            "Open...",
            "Save",
            "Save As...",
            "Pull Model from Server",
            "Push Model to Server",
            "Undo",
            "Redo",
            "ECA Model Editor",
            "Simulator",
            "Expand All",
            "Collapse All",
            "Theme",
        ]
    );
    await expect ( toolbar.locator ( ".command-icon" ).first () ).toHaveCSS ( "width", "20px" );

    const visibleTreeItems = page.getByRole ( "treeitem" );

    await expect ( visibleTreeItems ).toHaveCount ( 8 );
    await expect ( visibleTreeItems.locator ( ".tree-icon" ) ).toHaveCount ( 8 );
    await expect ( visibleTreeItems.locator ( ".tree-icon" ).first () ).toHaveCSS ( "width", "16px" );

    await page.getByRole ( "menuitem", { name: "View", exact: true } ).click ();
    const viewCommandLabels = await page.locator ( ".menu-popup:not(.menu-popup-nested) .menu-label" ).evaluateAll (
        labels => labels.map ( label => label.textContent?.trim () ?? "" )
    );

    expect ( viewCommandLabels.slice ( 7, 12 ) ).toEqual (
        [ "Simulator", "Expand All", "Collapse All", "Clear Console", "Console" ]
    );
    const themeItem = page.getByRole ( "menuitem", { name: "Theme", exact: true } );
    const themeColumns = await themeItem.evaluate (
        item => window.getComputedStyle ( item ).gridTemplateColumns.split ( " " ).length
    );

    expect ( themeColumns ).toBe ( 5 );
    await expect ( page.getByRole ( "menuitemcheckbox", { name: "Console", exact: true } ) ).toBeChecked ();
    await page.keyboard.press ( "Escape" );

    const modelTreeItem = page.getByRole ( "treeitem", { name: "ECA Model", exact: true } );

    await openMenuCommand ( page, "View", "Collapse All" );
    await expect ( modelTreeItem ).toHaveAttribute ( "aria-expanded", "false" );
    await toolbar.getByRole ( "button", { name: "Expand All", exact: true } ).click ();
    await expect ( modelTreeItem ).toHaveAttribute ( "aria-expanded", "true" );
    await expect ( toolbar.getByRole ( "button", { name: "Theme", exact: true } ) ).not.toContainText ( "▼" );

    const fileMenuButton = page.getByRole ( "menuitem", { name: "File", exact: true } );

    await expect ( fileMenuButton ).toHaveCSS ( "border-top-width", "0px" );
    await expect ( fileMenuButton ).toHaveCSS ( "border-radius", "0px" );

    const documentOverview = page.getByRole ( "group", { name: "Document Overview" } );
    const editorPanel = page.getByRole ( "tabpanel", { name: "ECA Model Editor" } );
    const overviewBounds = await documentOverview.boundingBox ();
    const editorBounds = await editorPanel.boundingBox ();

    expect ( overviewBounds ).not.toBeNull ();
    expect ( editorBounds ).not.toBeNull ();
    expect ( ( overviewBounds?.y ?? 0 ) - ( editorBounds?.y ?? 0 ) ).toBeLessThan ( 100 );
    await expect ( editorPanel.getByRole ( "heading", { level: 2, name: "ECA Model" } ) ).toBeVisible ();
    await expect ( editorPanel.getByRole ( "group", { name: "Entity Summary" } ) ).toHaveCount ( 0 );

    const consoleRegion = page.getByRole ( "region", { name: "Console" } );

    await expect ( consoleRegion ).toBeVisible ();
    await expect ( consoleRegion.getByRole ( "columnheader" ) ).toHaveCount ( 6 );
    await expect ( consoleRegion.getByLabel ( "Messages" ) ).toBeChecked ();
    await expect ( consoleRegion.getByLabel ( "Warnings" ) ).toBeChecked ();
    await expect ( consoleRegion.getByLabel ( "Errors" ) ).toBeChecked ();
    await expect ( consoleRegion.getByLabel ( "Follow Tail" ) ).toBeChecked ();
    const consoleDataRow = consoleRegion.getByRole ( "grid" ).getByRole ( "row" )
        .filter ( { has: page.getByRole ( "gridcell" ) } )
        .last ();

    await expect ( consoleDataRow.getByRole ( "gridcell" ) ).toHaveCount ( 6 );
    await expect ( consoleDataRow ).toContainText ( "Ready" );
    await expect ( consoleDataRow ).toHaveCSS ( "border-bottom-width", "0px" );
    await expect ( consoleDataRow.locator ( ".console-text" ) ).toHaveCSS ( "white-space", "nowrap" );
    await expect ( consoleDataRow.locator ( ".console-context" ) ).toHaveCSS ( "white-space", "nowrap" );

    await openMenuCommand ( page, "Help", "About" );
    const aboutDialog = page.getByRole ( "dialog", { name: "About" } );
    const licenceTextAreas = aboutDialog.locator ( ".about-licences textarea" );

    await expect ( aboutDialog ).not.toContainText ( "Open-source notices" );
    await expect ( aboutDialog.locator ( ".about-technical-note > strong" ) ).toHaveText ( "Technical Note:" );
    await expect ( aboutDialog.getByRole ( "link", { name: "https://zenodo.org/records/21804777", exact: true } ) )
        .toHaveAttribute ( "href", "https://zenodo.org/records/21804777" );
    await expect ( aboutDialog.getByRole ( "link", { name: /Technical Note:/u } ) ).toHaveCount ( 0 );
    const aboutGeometry = await aboutDialog.evaluate (
        element => ( {
            height: element.getBoundingClientRect ().height,
            width: element.getBoundingClientRect ().width,
        } )
    );

    expect ( aboutGeometry.height ).toBeGreaterThan ( 620 );
    expect ( aboutGeometry.height ).toBeLessThanOrEqual ( 660 );
    expect ( aboutGeometry.width ).toBeCloseTo ( 640, 0 );
    await expect ( aboutDialog.getByRole ( "tablist", { name: "About content" } ) ).toBeVisible ();
    const licencesTab = aboutDialog.getByRole ( "tab", { name: "Licences" } );
    const releaseNotesTab = aboutDialog.getByRole ( "tab", { name: "Release Notes" } );

    await expect ( aboutDialog.locator ( ".dialog-window" ) ).toHaveCSS ( "background-color", "rgb(36, 36, 36)" );
    await expect ( aboutDialog.locator ( ".dialog-window" ) ).toHaveCSS ( "border-color", "rgb(85, 85, 85)" );
    await expect ( aboutDialog.locator ( ".dialog-title-bar" ) ).toHaveCSS ( "background-color", "rgb(43, 43, 43)" );
    await expect ( aboutDialog.locator ( ".dialog-title-bar" ) ).toHaveCSS ( "color", "rgb(255, 255, 255)" );
    await expect ( aboutDialog.locator ( ".dialog-footer" ) ).toHaveCSS ( "background-color", "rgb(27, 27, 27)" );
    await expect ( aboutDialog.getByRole ( "link" ) ).toHaveCSS ( "color", "rgb(224, 224, 224)" );
    await expect ( licencesTab ).toHaveCSS ( "background-color", "rgb(36, 36, 36)" );
    await expect ( licencesTab ).toHaveCSS ( "font-weight", "600" );
    await expect ( releaseNotesTab ).toHaveCSS ( "background-color", "rgb(41, 41, 41)" );
    await expect ( licenceTextAreas ).toHaveCount ( 2 );
    await expect ( licenceTextAreas.first () ).toHaveCSS ( "background-color", "rgb(24, 24, 24)" );
    await expect ( licenceTextAreas.first () ).toHaveCSS ( "border-color", "rgb(112, 112, 112)" );
    await expect ( licenceTextAreas.first () ).toHaveCSS ( "color", "rgb(242, 242, 242)" );
    await expect ( licenceTextAreas.nth ( 0 ) ).toHaveValue ( /Copyright \(c\) 2025 Rohin Gosling/ );
    await expect ( licenceTextAreas.nth ( 1 ) ).toHaveValue ( /Copyright \(c\) 2020 Microsoft Corporation/ );
    const licenceGeometry = await licenceTextAreas.evaluateAll ( elements => elements.map ( element => ( {
        clientHeight: element.clientHeight,
        clientWidth: element.clientWidth,
        scrollHeight: element.scrollHeight,
    } ) ) );

    expect ( licenceGeometry [ 0 ]?.clientHeight ).toBe ( licenceGeometry [ 1 ]?.clientHeight );
    expect ( licenceGeometry [ 0 ]?.clientWidth ).toBe ( licenceGeometry [ 1 ]?.clientWidth );
    expect ( licenceGeometry.every ( geometry => geometry.scrollHeight > geometry.clientHeight ) ).toBe ( true );
    const licencesTabPageGeometry = await aboutDialog.locator ( ".about-tabs .tab-panel:not([hidden])" ).evaluate (
        element => ( {
            clientHeight: element.clientHeight,
            scrollHeight: element.scrollHeight,
        } )
    );

    expect ( licencesTabPageGeometry.scrollHeight - licencesTabPageGeometry.clientHeight ).toBeLessThanOrEqual ( 1 );
    await expect ( aboutDialog.locator ( ".about-tabs .tab-panel:not([hidden])" ) ).toHaveCSS ( "overflow-y", "hidden" );
    await expect ( aboutDialog.getByRole ( "tab", { name: "Licences" } ) )
        .toHaveAttribute ( "aria-selected", "true" );
    const licencesDialogHeight = await aboutDialog.evaluate ( element => element.getBoundingClientRect ().height );

    await releaseNotesTab.click ();
    await expect ( aboutDialog.getByRole ( "textbox", { name: "Release Notes" } ) ).toHaveValue (
        [
            "Version 2.1.0",
            "",
            "- Added Licences and Release Notes tabs to the About dialog in both clients.",
            "- Refined the web About dialog and Console navigation controls with a cleaner Fluent-style appearance.",
            "- Improved web cursor feedback so interactive controls use intuitive desktop-style pointers.",
            "- Updated Console navigation buttons in both clients to fit their content and use left-aligned text.",
            "- Made web Apply buttons commit changes and close their modal dialogs.",
            "- Expanded web revision fields and dialogs for long revision hashes, and made Dark the default web theme.",
        ].join ( "\n" )
    );
    expect ( await aboutDialog.evaluate ( element => element.getBoundingClientRect ().height ) )
        .toBe ( licencesDialogHeight );

    await licencesTab.click ();
    await expect ( licencesTab ).toHaveAttribute ( "aria-selected", "true" );
    await expect ( licenceTextAreas ).toHaveCount ( 2 );

    await aboutDialog.getByRole ( "button", { name: "Close", exact: true } ).click ();

    await toolbar.getByRole ( "button", { name: "Theme", exact: true } ).click ();
    await page.getByRole ( "menuitemradio", { name: "Light", exact: true } ).click ();
    await openMenuCommand ( page, "Help", "About" );

    await expect ( aboutDialog.locator ( ".dialog-window" ) ).toHaveCSS ( "background-color", "rgb(255, 255, 255)" );
    await expect ( aboutDialog.locator ( ".dialog-window" ) ).toHaveCSS ( "border-color", "rgb(174, 184, 194)" );
    await expect ( aboutDialog.locator ( ".dialog-title-bar" ) ).toHaveCSS ( "background-color", "rgb(14, 73, 127)" );
    await expect ( aboutDialog.locator ( ".dialog-footer" ) ).toHaveCSS ( "background-color", "rgb(241, 243, 245)" );
    await expect ( aboutDialog.getByRole ( "link" ) ).toHaveCSS ( "color", "rgb(7, 94, 168)" );
    await expect ( licencesTab ).toHaveCSS ( "background-color", "rgb(255, 255, 255)" );
    await expect ( releaseNotesTab ).toHaveCSS ( "background-color", "rgb(247, 248, 250)" );
    await expect ( licenceTextAreas.first () ).toHaveCSS ( "background-color", "rgb(255, 255, 255)" );
    await expect ( licenceTextAreas.first () ).toHaveCSS ( "border-color", "rgb(142, 154, 167)" );
    await expect ( licenceTextAreas.first () ).toHaveCSS ( "color", "rgb(23, 33, 43)" );

    await aboutDialog.getByRole ( "button", { name: "Close", exact: true } ).click ();

    await consoleRegion.getByLabel ( "Messages" ).uncheck ();
    await expect ( consoleRegion.getByRole ( "status" ) ).toBeEmpty ();

    const emptyConsoleRow = consoleRegion.getByRole ( "grid" ).getByRole ( "row" )
        .filter ( { has: page.getByRole ( "gridcell" ) } );

    await expect ( emptyConsoleRow ).toHaveCount ( 1 );
    await expect ( emptyConsoleRow.getByRole ( "gridcell" ) ).toHaveAttribute ( "aria-colspan", "6" );
    await verifyAccessibility ( page );

    const statusSegments = await page.locator ( ".status-bar > span" ).evaluateAll (
        segments => segments.map ( segment => segment.textContent?.trim () ?? "" )
    );

    expect ( statusSegments.slice ( 0, 9 ).map (
        segment => segment.split ( ":", 1 ) [ 0 ]?.replace ( /^[●○]/u, "" )
    ) ).toEqual (
        [ "Model ID", "Parameters", "Payloads", "Events", "Conditions", "Condition Sets", "Actions", "Rules", "Server" ]
    );
} );

test ( "uses theme-specific drop-down selection bars", async ( { page } ) =>
{
    await page.goto ( "./" );
    await waitForBuiltInServer ( page );
    await page.getByRole ( "button", { name: "Theme", exact: true } ).click ();
    await page.getByRole ( "menuitemradio", { name: "Light", exact: true } ).click ();
    await openMenuCommand ( page, "File", "Settings" );

    const lightSelectedOption = page.locator ( "#settings-group-selector option:checked" );

    await expect ( lightSelectedOption ).toHaveCSS (
        "box-shadow",
        /rgb\(207, 231, 255\).*inset/
    );
    await page.getByRole ( "button", { name: "Cancel", exact: true } ).click ();
    await page.getByRole ( "button", { name: "Theme", exact: true } ).click ();
    await page.getByRole ( "menuitemradio", { name: "Dark", exact: true } ).click ();
    await openMenuCommand ( page, "File", "Settings" );

    const darkSelectedOption = page.locator ( "#settings-group-selector option:checked" );

    await expect ( darkSelectedOption ).toHaveCSS (
        "box-shadow",
        /rgb\(61, 61, 61\).*inset/
    );
} );

test ( "expands, collapses, and routes the complete entity tree", async ( { page } ) =>
{
    await page.goto ( "./" );
    await waitForBuiltInServer ( page );
    await openMenuCommand ( page, "File", "Pull Model from Server" );

    const toolbar = page.getByRole ( "toolbar", { name: "Application toolbar" } );
    const modelTreeItem = page.getByRole ( "treeitem", { name: "ECA Model", exact: true } );

    await toolbar.getByRole ( "button", { name: "Expand All", exact: true } ).click ();

    const parametersTreeItem = page.getByRole ( "treeitem", { name: "Parameters", exact: true } );
    const entityTreeItem = page.getByRole ( "treeitem", { name: "parameter-assigned-courier", exact: true } );

    await expect ( modelTreeItem ).toHaveAttribute ( "aria-expanded", "true" );
    await expect ( parametersTreeItem ).toHaveAttribute ( "aria-expanded", "true" );
    await expect ( entityTreeItem ).toBeVisible ();
    await entityTreeItem.click ();
    await expect ( entityTreeItem ).toHaveAttribute ( "aria-selected", "true" );
    await expect ( page.getByRole ( "heading", {
        level: 2,
        name: "Parameters — parameter-assigned-courier",
    } ) ).toBeVisible ();

    await toolbar.getByRole ( "button", { name: "Collapse All", exact: true } ).click ();
    await expect ( modelTreeItem ).toHaveAttribute ( "aria-expanded", "false" );
    await expect ( entityTreeItem ).toHaveCount ( 0 );
} );

test ( "contains focus in and restores focus from the dirty-document modal", async ( { page } ) =>
{
    await page.goto ( "./" );
    await waitForBuiltInServer ( page );
    await page.getByLabel ( "Name", { exact: true } ).fill ( "Modified model" );
    await page.getByRole ( "button", { name: "Apply Details" } ).click ();
    await page.getByRole ( "toolbar" ).getByRole ( "button", { name: "Collapse All", exact: true } ).click ();

    const newButton = page.getByRole ( "toolbar" ).getByRole ( "button", { name: "New", exact: true } );

    await newButton.click ();
    const dialog = page.getByRole ( "dialog", { name: "Unsaved changes" } );

    await expect ( dialog ).toBeVisible ();
    await expect ( dialog ).toContainText (
        "The current document contains unsaved changes. Choose what to do before continuing."
    );
    await expect ( dialog.getByRole ( "button", { name: "Cancel" } ) ).toBeFocused ();
    await page.keyboard.press ( "Shift+Tab" );
    await expect ( dialog.getByRole ( "button", { name: "Close dialog" } ) ).toBeFocused ();
    await page.keyboard.press ( "Shift+Tab" );
    await expect ( dialog.getByRole ( "button", { name: "Discard and Continue" } ) ).toBeFocused ();
    await page.keyboard.press ( "Escape" );
    await expect ( newButton ).toBeFocused ();

    await newButton.click ();
    await dialog.getByRole ( "button", { name: "Discard and Continue" } ).click ();
    await expect ( page.getByLabel ( "Name", { exact: true } ) ).toHaveValue ( "Untitled" );
    await expect ( page.getByRole ( "treeitem", { name: "ECA Model", exact: true } ) )
        .toHaveAttribute ( "aria-expanded", "true" );
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
