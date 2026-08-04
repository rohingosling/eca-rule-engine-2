// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    playwright.phase-19.config
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Pins the Phase 19 GitHub-Pages-origin-to-loopback Java-server acceptance matrix.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { defineConfig, devices, type Project } from "@playwright/test";

const projects: Project[] =
[
    {
        name: "chromium",
        use:
        {
            ...devices [ "Desktop Chrome" ],
            channel: "chromium",
        },
    },
];

if ( process.platform === "win32" )
{
    projects.push (
        {
            name: "microsoft-edge",
            use:
            {
                ...devices [ "Desktop Edge" ],
                channel: "msedge",
            },
        }
    );
}

export default defineConfig (
    {
        testDir: "./tests",
        testMatch: "phase-19-browser.spec.ts",
        fullyParallel: false,
        forbidOnly: true,
        retries: 0,
        workers: 1,
        reporter: "list",
        timeout: 45_000,
        use:
        {
            colorScheme: "light",
            locale: "en-US",
            serviceWorkers: "block",
            trace: "on-first-retry",
        },
        projects,
    }
);
