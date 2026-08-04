// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    playwright.phase-21.config
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Pins the Phase 21 local-artifact and emitted-GitHub-Pages smoke controls.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { defineConfig, devices } from "@playwright/test";

const localBaseURL       = "http://127.0.0.1:4173/eca-rule-engine-2/";
const publishedBaseURL   = process.env.ECA_PHASE_21_BASE_URL;
const applicationBaseURL = publishedBaseURL === undefined
    ? localBaseURL
    : `${publishedBaseURL.replace ( /\/+$/, "" )}/`;

export default defineConfig (
    {
        testDir: "./tests",
        testMatch: "phase-21-browser.spec.ts",
        fullyParallel: false,
        forbidOnly: true,
        retries: publishedBaseURL === undefined ? 0 : 2,
        workers: 1,
        reporter:
        [
            [ "list" ],
            [ "html", { outputFolder: "playwright-report/phase-21", open: "never" } ],
        ],
        timeout: 60_000,
        use:
        {
            ...devices [ "Desktop Chrome" ],
            baseURL: applicationBaseURL,
            colorScheme: "light",
            locale: "en-US",
            screenshot: "only-on-failure",
            serviceWorkers: "block",
            trace: "retain-on-failure",
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
        webServer: publishedBaseURL === undefined
            ? {
                command: "npm run preview -- --host 127.0.0.1 --port 4173",
                url: localBaseURL,
                reuseExistingServer: true,
                timeout: 30_000,
            }
            : undefined,
    }
);
