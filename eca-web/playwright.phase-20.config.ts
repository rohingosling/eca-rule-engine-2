// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    playwright.phase-20.config
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Pins the Phase 20 Chromium, Firefox, WebKit, accessibility, recovery, zoom, and visual-hardening matrix.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { defineConfig, devices } from "@playwright/test";

const applicationPort = process.env.ECA_PHASE_20_PORT ?? "4173";
const applicationBaseURL = `http://127.0.0.1:${applicationPort}/eca-rule-engine-2/`;

export default defineConfig (
    {
        testDir: "./tests",
        testMatch: "phase-20-browser.spec.ts",
        fullyParallel: false,
        forbidOnly: true,
        retries: 0,
        workers: 1,
        reporter: "list",
        snapshotPathTemplate: "{testDir}/__screenshots__/phase-20/{projectName}-{arg}{ext}",
        timeout: 45_000,
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
            baseURL: applicationBaseURL,
            colorScheme: "light",
            locale: "en-US",
            serviceWorkers: "block",
            trace: "on-first-retry",
            viewport: { width: 1691, height: 1212 },
        },
        projects:
        [
            {
                name: "chromium",
                use:
                {
                    ...devices [ "Desktop Chrome" ],
                    channel: "chromium",
                    viewport: { width: 1691, height: 1212 },
                },
            },
            {
                name: "firefox",
                use:
                {
                    ...devices [ "Desktop Firefox" ],
                    viewport: { width: 1691, height: 1212 },
                },
            },
            {
                name: "webkit",
                use:
                {
                    ...devices [ "Desktop Safari" ],
                    viewport: { width: 1691, height: 1212 },
                },
            },
        ],
        webServer:
        {
            command: `npm run preview -- --host 127.0.0.1 --port ${applicationPort}`,
            url: applicationBaseURL,
            reuseExistingServer: true,
            timeout: 30_000,
        },
    }
);
