// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    phase-13.spec
// Version: 2.0.0
// Date:    2026-08-04
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies the Phase 13 shell, keyboard, storage, accessibility, visual, and responsive gates.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import AxeBuilder from "@axe-core/playwright";
import { expect, test } from "@playwright/test";

test.beforeEach (
    async ( { page } ) =>
    {
        await page.goto ( "./" );
        await page.evaluate ( () => window.localStorage.clear () );
        await page.reload ();
    }
);

test ( "loads every shell asset beneath the GitHub Pages project path", async ( { page } ) =>
{
    const failedResponses: string[] = [];

    page.on (
        "response",
        response =>
        {
            if ( response.status () >= 400 )
            {
                failedResponses.push ( `${response.status ()} ${response.url ()}` );
            }
        }
    );

    await page.reload ();
    await expect ( page ).toHaveTitle ( "ECA Rule Engine Laboratory (Version 2.0.0 - Untitled)" );
    await expect ( page.getByRole ( "heading", { level: 1 } ) ).toContainText ( "Version 2.0" );
    await expect ( page.getByRole ( "group", { name: "Outline" } ) ).toBeVisible ();
    await expect ( page.getByRole ( "group", { name: "User Guide" } ) ).toBeVisible ();
    await expect ( page.getByRole ( "group", { name: "Messages and Diagnostics" } ) ).toBeVisible ();
    await expect ( page.locator ( ".equation-card img" ) ).toHaveJSProperty ( "complete", true );

    const equationWidth = await page.locator ( ".equation-card img" ).evaluate (
        image => ( image as HTMLImageElement ).naturalWidth
    );

    expect ( equationWidth ).toBeGreaterThan ( 0 );
    expect ( page.url () ).toContain ( "/eca-rule-engine-2/" );
    expect ( failedResponses ).toEqual ( [] );
} );

test ( "supports keyboard operation for menus, tabs, tree, and splitters", async ( { page } ) =>
{
    const fileMenu = page.getByRole ( "menuitem", { name: "File" } );

    await fileMenu.focus ();
    await fileMenu.press ( "Enter" );
    await expect ( page.getByRole ( "menuitem", { name: "New", exact: true } ) ).toBeFocused ();
    await page.keyboard.press ( "Escape" );
    await expect ( fileMenu ).toBeFocused ();

    const editorTab = page.getByRole ( "tab", { name: "ECA Model Editor" } );

    await editorTab.focus ();
    await editorTab.press ( "ArrowRight" );
    await expect ( page.getByRole ( "tab", { name: "Simulator" } ) ).toHaveAttribute ( "aria-selected", "true" );

    const modelTreeItem = page.getByRole ( "treeitem", { name: "ECA Model" } );

    await modelTreeItem.focus ();
    await modelTreeItem.press ( "ArrowLeft" );
    await expect ( modelTreeItem ).toHaveAttribute ( "aria-expanded", "false" );
    await modelTreeItem.press ( "ArrowRight" );
    await expect ( modelTreeItem ).toHaveAttribute ( "aria-expanded", "true" );
    await modelTreeItem.press ( "ArrowDown" );
    await expect ( page.getByRole ( "treeitem", { name: "Parameters" } ) ).toHaveAttribute ( "aria-selected", "true" );

    const outlineSplitter = page.getByRole ( "separator", { name: "Resize Outline pane" } );
    const initialSplitterValue = Number ( await outlineSplitter.getAttribute ( "aria-valuenow" ) );

    await outlineSplitter.focus ();
    await outlineSplitter.press ( "ArrowRight" );
    await expect ( outlineSplitter ).toHaveAttribute ( "aria-valuenow", String ( initialSplitterValue + 10 ) );
} );

test ( "defaults to Dark and keeps the Theme submenu open for pointer selection", async ( { page } ) =>
{
    await expect ( page.locator ( ".application-shell" ) ).toHaveAttribute ( "data-theme", "dark" );
    await page.getByRole ( "menuitem", { name: "View", exact: true } ).click ();
    await page.getByRole ( "menuitem", { name: "Theme", exact: true } ).click ();

    const lightThemeItem = page.getByRole ( "menuitemradio", { name: "Light", exact: true } );

    await lightThemeItem.hover ();
    await expect ( lightThemeItem ).toBeVisible ();
    await lightThemeItem.click ();
    await expect ( page.locator ( ".application-shell" ) ).toHaveAttribute ( "data-theme", "light" );
} );

test ( "sizes every main menu for its longest item without wrapping", async ( { page } ) =>
{
    for ( const menuName of [ "File", "Edit", "View", "Help" ] )
    {
        await page.getByRole ( "menuitem", { name: menuName, exact: true } ).click ();

        const menu = page.getByRole ( "menu", { name: menuName, exact: true } );
        const measurements = await menu.evaluate (
            menuElement =>
            {
                const menuItems = Array.from (
                    menuElement.querySelectorAll <HTMLButtonElement> (
                        ":scope > .menu-item-container > .menu-item"
                    )
                );

                return {
                    clientWidth: menuElement.clientWidth,
                    scrollWidth: menuElement.scrollWidth,
                    labels: menuItems.map (
                        menuItem =>
                        {
                            const label = menuItem.children.item ( 1 ) as HTMLElement | null;

                            return {
                                clientWidth: label?.clientWidth ?? 0,
                                scrollWidth: label?.scrollWidth ?? 0,
                                text: label?.textContent ?? "",
                                whiteSpace: label === null ? "" : window.getComputedStyle ( label ).whiteSpace,
                            };
                        }
                    ),
                };
            }
        );

        expect ( measurements.scrollWidth ).toBeLessThanOrEqual ( measurements.clientWidth + 1 );

        for ( const label of measurements.labels )
        {
            expect ( label.whiteSpace, label.text ).toBe ( "nowrap" );
            expect ( label.scrollWidth, label.text ).toBeLessThanOrEqual ( label.clientWidth + 1 );
        }

        await page.keyboard.press ( "Escape" );
    }
} );

test ( "provides one-fifth defaults and three-fifths splitter travel", async ( { page } ) =>
{
    const viewport = page.viewportSize ();

    if ( viewport === null )
    {
        throw new Error ( "The Phase 13 browser project requires a fixed viewport." );
    }

    const oneFifthViewportWidth = Math.round ( viewport.width / 5 );
    const threeFifthsViewportWidth = Math.round ( viewport.width * 3 / 5 );
    const oneFifthViewportHeight = Math.round ( viewport.height / 5 );
    const threeFifthsViewportHeight = Math.round ( viewport.height * 3 / 5 );
    const outlineSplitter = page.getByRole ( "separator", { name: "Resize Outline pane" } );
    const userGuideSplitter = page.getByRole ( "separator", { name: "Resize User Guide pane" } );
    const diagnosticsSplitter = page.getByRole (
        "separator",
        { name: "Resize Messages and Diagnostics pane" }
    );

    await expect ( outlineSplitter ).toHaveAttribute ( "aria-valuemin", String ( oneFifthViewportWidth ) );
    await expect ( outlineSplitter ).toHaveAttribute ( "aria-valuemax", String ( threeFifthsViewportWidth ) );
    await expect ( outlineSplitter ).toHaveAttribute ( "aria-valuenow", String ( oneFifthViewportWidth ) );
    await expect ( userGuideSplitter ).toHaveAttribute ( "aria-valuemin", "240" );
    await expect ( userGuideSplitter ).toHaveAttribute ( "aria-valuemax", String ( threeFifthsViewportWidth ) );
    await expect ( userGuideSplitter ).toHaveAttribute ( "aria-valuenow", "440" );
    await expect ( diagnosticsSplitter ).toHaveAttribute ( "aria-valuemin", String ( oneFifthViewportHeight ) );
    await expect ( diagnosticsSplitter ).toHaveAttribute ( "aria-valuemax", String ( threeFifthsViewportHeight ) );
    await expect ( diagnosticsSplitter ).toHaveAttribute ( "aria-valuenow", String ( oneFifthViewportHeight ) );

    await outlineSplitter.press ( "Home" );
    await expect ( outlineSplitter ).toHaveAttribute ( "aria-valuenow", String ( oneFifthViewportWidth ) );
    await outlineSplitter.press ( "End" );
    await expect ( outlineSplitter ).toHaveAttribute ( "aria-valuenow", String ( threeFifthsViewportWidth ) );

    await userGuideSplitter.press ( "End" );
    await expect ( userGuideSplitter ).toHaveAttribute ( "aria-valuenow", String ( threeFifthsViewportWidth ) );

    await diagnosticsSplitter.press ( "End" );
    await expect ( diagnosticsSplitter ).toHaveAttribute ( "aria-valuenow", String ( threeFifthsViewportHeight ) );

    await page.setViewportSize ( { width: 1200, height: 800 } );
    await expect ( outlineSplitter ).toHaveAttribute ( "aria-valuemin", "240" );
    await expect ( outlineSplitter ).toHaveAttribute ( "aria-valuemax", "720" );
    await expect ( userGuideSplitter ).toHaveAttribute ( "aria-valuemax", "720" );
    await expect ( diagnosticsSplitter ).toHaveAttribute ( "aria-valuemin", "160" );
    await expect ( diagnosticsSplitter ).toHaveAttribute ( "aria-valuemax", "480" );
} );

test ( "persists only UI state and non-sensitive server preferences", async ( { page } ) =>
{
    await page.getByRole ( "menuitem", { name: "View" } ).click ();
    await page.getByRole ( "menuitem", { name: "Theme" } ).click ();
    await page.getByRole ( "menuitemradio", { name: "Light" } ).click ();
    await expect ( page.locator ( ".application-shell" ) ).toHaveAttribute ( "data-theme", "light" );

    await page.getByRole ( "separator", { name: "Resize Outline pane" } ).press ( "ArrowRight" );
    await page.reload ();
    await expect ( page.locator ( ".application-shell" ) ).toHaveAttribute ( "data-theme", "light" );

    const storedEntries = await page.evaluate (
        () => Object.entries ( window.localStorage )
    );

    expect ( storedEntries ).toHaveLength ( 1 );
    expect ( storedEntries [ 0 ]?.[ 0 ] ).toBe ( "eca-shell-preferences-v1" );

    const storedPreferences = JSON.parse ( storedEntries [ 0 ]?.[ 1 ] ?? "{}" ) as Record <string, unknown>;

    expect ( Object.keys ( storedPreferences ).sort () ).toEqual (
        [
            "connectTimeoutSeconds",
            "diagnosticsHeight",
            "expandedTreeItems",
            "outlineWidth",
            "requestTimeoutSeconds",
            "serverTarget",
            "serverURL",
            "theme",
            "userGuideWidth",
        ]
    );
    expect ( storedPreferences ).not.toHaveProperty ( "bearerToken" );
} );

test ( "has no automatically detectable initial accessibility violations", async ( { page } ) =>
{
    const accessibilityResults = await new AxeBuilder ( { page } )
        .withTags ( [ "wcag2a", "wcag2aa", "wcag21a", "wcag21aa" ] )
        .analyze ();

    expect ( accessibilityResults.violations ).toEqual ( [] );
} );

test ( "matches the accepted light and dark visual baselines", async ( { page } ) =>
{
    const screenshotViewport = { width: 1691, height: 1212 };

    await page.setViewportSize ( screenshotViewport );
    await page.evaluate ( () => window.localStorage.clear () );
    await page.reload ();
    await expect ( page.getByRole ( "separator", { name: "Resize Outline pane" } ) ).toHaveAttribute (
        "aria-valuenow",
        String ( Math.round ( screenshotViewport.width / 5 ) )
    );
    await expect (
        page.getByRole ( "separator", { name: "Resize Messages and Diagnostics pane" } )
    ).toHaveAttribute (
        "aria-valuenow",
        String ( Math.round ( screenshotViewport.height / 5 ) )
    );

    await page.getByRole ( "menuitem", { name: "View" } ).click ();
    await page.getByRole ( "menuitem", { name: "Theme" } ).click ();
    await page.getByRole ( "menuitemradio", { name: "Light" } ).click ();
    await expect ( page ).toHaveScreenshot ( "shell-light.png", { fullPage: true } );

    await page.getByRole ( "menuitem", { name: "View" } ).click ();
    await page.getByRole ( "menuitem", { name: "Theme" } ).click ();
    await page.getByRole ( "menuitemradio", { name: "Dark" } ).click ();
    await expect ( page ).toHaveScreenshot ( "shell-dark.png", { fullPage: true } );
} );

for ( const viewport of [ { width: 1024, height: 768 }, { width: 760, height: 720 } ] )
{
    test ( `fits the ${viewport.width} by ${viewport.height} responsive smoke viewport`, async ( { page } ) =>
    {
        await page.setViewportSize ( viewport );
        await expect ( page.getByRole ( "tabpanel", { name: "ECA Model Editor" } ) ).toBeVisible ();

        const dimensions = await page.evaluate (
            () =>
            ({
                documentHeight: document.documentElement.scrollHeight,
                documentWidth: document.documentElement.scrollWidth,
                viewportHeight: window.innerHeight,
                viewportWidth: window.innerWidth,
            })
        );

        expect ( dimensions.documentWidth ).toBeLessThanOrEqual ( dimensions.viewportWidth );
        expect ( dimensions.documentHeight ).toBeLessThanOrEqual ( dimensions.viewportHeight );
    } );
}
