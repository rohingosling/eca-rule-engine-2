// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    phase-17-browser.spec
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies the Phase 17 SDI lifecycle, editor interaction, fallback files, reference preview, and accessibility.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { fileURLToPath } from "node:url";

import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";

const EXAMPLE_MODEL_PATH = fileURLToPath ( new URL ( "../../examples/eca-rule-engine-example.json", import.meta.url ) );

async function openMenuCommand ( page: Page, menuName: string, commandName: string ): Promise <void>
{
    await page.getByRole ( "menuitem", { name: menuName, exact: true } ).click ();
    await page.getByRole ( "menuitem", { name: commandName, exact: true } ).click ();
}

test.beforeEach (
    async ( { page } ) =>
    {
        await page.addInitScript (
            () =>
            {
                Object.defineProperty ( window, "showOpenFilePicker", { configurable: true, value: undefined } );
                Object.defineProperty ( window, "showSaveFilePicker", { configurable: true, value: undefined } );
            }
        );
        await page.goto ( "./" );
        await page.evaluate ( () => window.localStorage.clear () );
        await page.reload ();
    }
);

test ( "tracks dirty state, undo, redo, unload, and every replacing command", async ( { page } ) =>
{
    const modelName = page.getByLabel ( "Name", { exact: true } );

    await expect ( page ).toHaveTitle ( /Untitled\)$/ );
    expect ( await page.evaluate (
        () =>
        {
            const event = new Event ( "beforeunload", { cancelable: true } );

            window.dispatchEvent ( event );
            return event.defaultPrevented;
        }
    ) ).toBe ( false );
    await modelName.fill ( "Changed model" );
    await page.getByRole ( "button", { name: "Apply Details" } ).click ();
    await expect ( page ).toHaveTitle ( /Untitled \*\)$/ );
    await expect ( page.getByRole ( "region", { name: "Console" } ) )
        .toContainText ( "Model details applied" );
    expect ( await page.evaluate (
        () =>
        {
            const event = new Event ( "beforeunload", { cancelable: true } );

            window.dispatchEvent ( event );
            return event.defaultPrevented;
        }
    ) ).toBe ( true );

    await page.keyboard.press ( "Control+z" );
    await expect ( page ).toHaveTitle ( /Untitled\)$/ );
    await expect ( modelName ).toHaveValue ( "Untitled" );

    await page.keyboard.press ( "Control+y" );
    await expect ( modelName ).toHaveValue ( "Changed model" );

    for ( const commandName of [ "Open...", "Close", "Exit" ] )
    {
        await openMenuCommand ( page, "File", commandName );
        await page.getByRole ( "dialog", { name: "Unsaved changes" } )
            .getByRole ( "button", { name: "Cancel", exact: true } )
            .click ();
        await expect ( modelName ).toHaveValue ( "Changed model" );
    }

    await openMenuCommand ( page, "File", "New" );
    await page.getByRole ( "dialog", { name: "Unsaved changes" } )
        .getByRole ( "button", { name: "Cancel", exact: true } )
        .click ();
    await expect ( modelName ).toHaveValue ( "Changed model" );

    await openMenuCommand ( page, "File", "New" );
    await page.getByRole ( "dialog", { name: "Unsaved changes" } )
        .getByRole ( "button", { name: "Discard and Continue", exact: true } )
        .click ();
    await expect ( modelName ).toHaveValue ( "Untitled" );
    await expect ( page ).toHaveTitle ( /Untitled\)$/ );
    expect ( await page.evaluate (
        () =>
        {
            const event = new Event ( "beforeunload", { cancelable: true } );

            window.dispatchEvent ( event );
            return event.defaultPrevented;
        }
    ) ).toBe ( false );
} );

test ( "opens a local JSON file and saves through the download fallback", async ( { page } ) =>
{
    const fileChooserPromise = page.waitForEvent ( "filechooser" );

    await openMenuCommand ( page, "File", "Open..." );
    await ( await fileChooserPromise ).setFiles ( EXAMPLE_MODEL_PATH );

    await expect ( page ).toHaveTitle ( /eca-rule-engine-example\.json\)$/ );
    await expect ( page.getByLabel ( "Name", { exact: true } ) ).toHaveValue ( "ECA Rule Engine Example" );

    await page.getByLabel ( "Description", { exact: true } ).fill ( "Saved from the web editor." );
    await page.getByRole ( "button", { name: "Apply Details" } ).click ();

    const downloadPromise = page.waitForEvent ( "download" );

    await openMenuCommand ( page, "File", "Save" );

    const download = await downloadPromise;

    expect ( download.suggestedFilename () ).toBe ( "eca-rule-engine-example.json" );
    await expect ( page ).toHaveTitle ( /eca-rule-engine-example\.json\)$/ );
    await expect ( page.getByRole ( "region", { name: "Console" } ) )
        .toContainText ( "later saves create another download" );
    const downloadWarningDialog = page.getByRole ( "dialog", { name: "Warning", exact: true } );

    await expect ( downloadWarningDialog ).toContainText ( "later saves create another download" );
    await expect ( downloadWarningDialog.getByRole ( "button", { name: "OK", exact: true } ) ).toBeFocused ();
    await downloadWarningDialog.getByRole ( "button", { name: "OK", exact: true } ).click ();

    const invalidFileChooserPromise = page.waitForEvent ( "filechooser" );

    await openMenuCommand ( page, "File", "Open..." );
    await ( await invalidFileChooserPromise ).setFiles (
        { name: "invalid.json", mimeType: "application/json", buffer: Buffer.from ( "not json" ) }
    );
    await expect ( page.getByRole ( "region", { name: "Console" } ) ).toContainText ( "Open failed" );
    await expect ( page.getByRole ( "dialog", { name: "Error", exact: true } ) ).toContainText ( "Open failed" );
    await expect ( page ).toHaveTitle ( /eca-rule-engine-example\.json\)$/ );
    await expect ( page.getByLabel ( "Name", { exact: true } ) ).toHaveValue ( "ECA Rule Engine Example" );
} );

test ( "keeps a failed save dirty and preserves the active document", async ( { page } ) =>
{
    await page.addInitScript (
        () => Object.defineProperty (
            window,
            "showSaveFilePicker",
            {
                configurable: true,
                value: async () => { throw new Error ( "simulated save failure" ); },
            }
        )
    );
    await page.reload ();

    await page.getByLabel ( "Name", { exact: true } ).fill ( "Unsaved model" );
    await page.getByRole ( "button", { name: "Apply Details" } ).click ();
    await page.getByRole ( "menuitem", { name: "View", exact: true } ).click ();
    await page.getByRole ( "menuitemcheckbox", { name: "Console", exact: true } ).click ();
    await expect ( page.locator ( ".console-panel" ) ).not.toBeVisible ();
    await openMenuCommand ( page, "File", "Save" );

    await expect ( page.locator ( ".console-panel" ) ).toContainText ( "simulated save failure" );
    await expect ( page.getByRole ( "dialog", { name: "Error", exact: true } ) )
        .toContainText ( "simulated save failure" );
    await expect ( page.getByLabel ( "Name", { exact: true } ) ).toHaveValue ( "Unsaved model" );
    await expect ( page ).toHaveTitle ( /Untitled \*\)$/ );
} );

test ( "adds a validated parameter and exposes reference-safe entity controls", async ( { page } ) =>
{
    await page.getByRole ( "treeitem", { name: "Parameters" } ).click ();
    await page.getByRole ( "button", { name: "Add", exact: true } ).click ();

    await page.getByLabel ( "ID", { exact: true } ).fill ( "Invalid ID" );
    await page.getByLabel ( "Name", { exact: true } ).fill ( "Customer code" );
    await page.getByLabel ( "Description", { exact: true } ).fill ( "Identifies the customer." );
    await page.getByRole ( "button", { name: "Apply", exact: true } ).click ();
    await expect ( page.getByRole ( "alert" ) ).toContainText ( "lowercase words" );

    await page.getByLabel ( "ID", { exact: true } ).fill ( "parameter-customer-code" );
    await page.getByRole ( "button", { name: "Apply", exact: true } ).click ();
    const parameterGrid = page.getByRole ( "grid", { name: "Parameters" } );

    await expect ( parameterGrid.getByRole ( "gridcell", { name: "parameter-customer-code", exact: true } ) )
        .toBeVisible ();

    const parameterRow = parameterGrid.getByRole ( "row" ).filter ( { hasText: "parameter-customer-code" } );

    await parameterRow.focus ();
    await parameterRow.press ( "Enter" );
    await expect ( page.getByRole ( "button", { name: "Duplicate" } ) ).toBeEnabled ();

    await page.getByRole ( "button", { name: "Duplicate" } ).click ();
    await expect ( parameterGrid.getByRole ( "gridcell", { name: "parameter-customer-code-copy", exact: true } ) )
        .toBeVisible ();
} );

test ( "shows inbound references and keeps referenced deletion unavailable", async ( { page } ) =>
{
    const fileChooserPromise = page.waitForEvent ( "filechooser" );

    await openMenuCommand ( page, "File", "Open..." );
    await ( await fileChooserPromise ).setFiles ( EXAMPLE_MODEL_PATH );
    await page.getByRole ( "treeitem", { name: "Actions" } ).click ();

    const actionRow = page.getByRole ( "row" ).filter ( { hasText: "action-local-courier" } );

    await actionRow.click ();
    await expect ( page.locator ( ".reference-preview" ) ).toContainText ( "rule rule-test.actionId" );
    await expect ( page.getByRole ( "button", { name: "Delete" } ) ).toBeDisabled ();
} );

test ( "has no detectable accessibility violations in the overview and all seven editors", async ( { page } ) =>
{
    const fileChooserPromise = page.waitForEvent ( "filechooser" );

    await openMenuCommand ( page, "File", "Open..." );
    await ( await fileChooserPromise ).setFiles ( EXAMPLE_MODEL_PATH );

    let accessibilityResults = await new AxeBuilder ( { page } )
        .withTags ( [ "wcag2a", "wcag2aa", "wcag21a", "wcag21aa" ] )
        .analyze ();

    expect ( accessibilityResults.violations ).toEqual ( [] );

    for ( const categoryName of [
        "Parameters",
        "Payloads",
        "Events",
        "Conditions",
        "Condition Sets",
        "Actions",
        "Rules",
    ] )
    {
        await page.getByRole ( "treeitem", { name: categoryName, exact: true } ).click ();
        await page.getByRole ( "button", { name: "Add", exact: true } ).click ();

        accessibilityResults = await new AxeBuilder ( { page } )
            .withTags ( [ "wcag2a", "wcag2aa", "wcag21a", "wcag21aa" ] )
            .analyze ();

        expect ( accessibilityResults.violations, categoryName ).toEqual ( [] );
        await page.getByRole ( "button", { name: "Cancel", exact: true } ).click ();
    }
} );
