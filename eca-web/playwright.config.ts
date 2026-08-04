// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    playwright.config
// Version: 2.0.0
// Date:    2026-08-02
// Author:  Rohin Gosling
//
// Description:
//
//   Pins the Phase 13 browser, viewport, artifact server, and visual-baseline controls.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { defineConfig, devices } from "@playwright/test";

const applicationBaseURL = "http://127.0.0.1:4173/eca-rule-engine-2/";

export default defineConfig (
    {
        testDir: "./tests",
        testMatch: "phase-13.spec.ts",
        fullyParallel: false,
        forbidOnly: true,
        retries: 0,
        workers: 1,
        reporter: "list",
        snapshotPathTemplate: "{testDir}/__screenshots__/{arg}{ext}",
        expect:
        {
            toHaveScreenshot:
            {
                animations: "disabled",
                caret: "hide",
                maxDiffPixelRatio: 0.005,
            },
        },
        use:
        {
            ...devices [ "Desktop Chrome" ],
            baseURL: applicationBaseURL,
            colorScheme: "light",
            locale: "en-US",
            serviceWorkers: "block",
            trace: "on-first-retry",
        },
        projects:
        [
            {
                name: "chromium",
                use:
                {
                    ...devices [ "Desktop Chrome" ],
                    channel: "chromium",
                },
            },
        ],
        webServer:
        {
            command: "npm run preview -- --host 127.0.0.1 --port 4173",
            url: applicationBaseURL,
            reuseExistingServer: true,
            timeout: 30_000,
        },
    }
);
