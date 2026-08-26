// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    run-phase-19-browser-tests
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Packages the real Java server and runs the Phase 19 Chromium and Microsoft Edge browser acceptance suite.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { existsSync }       from "node:fs";
import { dirname, resolve } from "node:path";
import { spawnSync }        from "node:child_process";
import { fileURLToPath }    from "node:url";

const scriptDirectory  = dirname ( fileURLToPath ( import.meta.url ) );
const webDirectory     = resolve ( scriptDirectory, ".." );
const projectDirectory = resolve ( webDirectory, ".." );
const mavenWrapper     = resolve (
    projectDirectory,
    process.platform === "win32" ? "mvnw.cmd" : "mvnw"
);
const serverJar        = resolve ( projectDirectory, "eca-server", "target", "eca-server.jar" );
const playwrightCLI    = resolve ( webDirectory, "node_modules", "@playwright", "test", "cli.js" );

function run ( command, argumentsList, environment = process.env )
{
    const result = spawnSync (
        command,
        argumentsList,
        {
            cwd: webDirectory,
            env: environment,
            stdio: "inherit",
            windowsHide: true,
        }
    );

    if ( result.error !== undefined )
    {
        throw result.error;
    }

    return result.status ?? 1;
}

const mavenCommand = process.platform === "win32"
    ? process.env.ComSpec ?? "cmd.exe"
    : mavenWrapper;
const mavenArguments = process.platform === "win32"
    ? [
        "/d", "/c", mavenWrapper,
        "-f", resolve ( projectDirectory, "pom.xml" ),
        "-pl", "eca-server", "-am", "-DskipTests", "clean", "package",
    ]
    : [
        "-f", resolve ( projectDirectory, "pom.xml" ),
        "-pl", "eca-server", "-am", "-DskipTests", "clean", "package",
    ];

const packageStatus = run ( mavenCommand, mavenArguments );

if ( packageStatus !== 0 || !existsSync ( serverJar ) )
{
    process.exit ( packageStatus === 0 ? 1 : packageStatus );
}

const testEnvironment =
{
    ...process.env,
    ECA_PHASE_19_SERVER_JAR: serverJar,
};
const testStatus = run (
    process.execPath,
    [ playwrightCLI, "test", "--config", "playwright.phase-19.config.ts" ],
    testEnvironment
);

process.exit ( testStatus );
