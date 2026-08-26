// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    playwright.phase-18.config
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Pins the Phase 18 browser-local worker, Simulator, settings, privacy, cancellation, and accessibility suite.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { defineConfig, devices } from "@playwright/test";

const applicationPort = process.env.ECA_PHASE_18_PORT ?? "4173";
const applicationBaseURL = `http://127.0.0.1:${applicationPort}/eca-rule-engine-2/`;

export default defineConfig (
    {
        testDir: "./tests",
        testMatch: "phase-18-browser.spec.ts",
        fullyParallel: false,
        forbidOnly: true,
        retries: 0,
        workers: 1,
        reporter: "list",
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
            command: `npm run preview -- --host 127.0.0.1 --port ${applicationPort}`,
            url: applicationBaseURL,
            reuseExistingServer: true,
            timeout: 30_000,
        },
    }
);
