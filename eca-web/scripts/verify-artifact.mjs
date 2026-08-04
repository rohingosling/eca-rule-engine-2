// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    verify-artifact
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies the static GitHub Pages artifact and its repository-subpath asset references.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { access, readFile, readdir } from "node:fs/promises";
import { dirname, extname, resolve } from "node:path";
import { fileURLToPath }             from "node:url";

const PROJECT_BASE = "/eca-rule-engine-2/";
const scriptDirectory = dirname ( fileURLToPath ( import.meta.url ) );
const distributionRoot = resolve ( scriptDirectory, "..", "dist" );
const indexPath = resolve ( distributionRoot, "index.html" );
const indexSource = await readFile ( indexPath, "utf8" );
const packageLock = JSON.parse (
    await readFile ( resolve ( scriptDirectory, "..", "package-lock.json" ), "utf8" )
);
const assetReferences = Array.from ( indexSource.matchAll ( /(?:href|src)="([^"]+)"/g ) )
    .map ( match => match [ 1 ] )
    .filter ( reference => reference !== undefined && !reference.startsWith ( "http" ) );

if ( assetReferences.length === 0 )
{
    throw new Error ( "The production index does not reference any bundled assets." );
}

for ( const assetReference of assetReferences )
{
    if ( !assetReference.startsWith ( PROJECT_BASE ) )
    {
        throw new Error ( `Production asset does not use the GitHub Pages base path: ${assetReference}` );
    }

    const relativePath = assetReference.slice ( PROJECT_BASE.length ).split ( /[?#]/, 1 ) [ 0 ];

    if ( relativePath === undefined || relativePath.length === 0 )
    {
        continue;
    }

    await access ( resolve ( distributionRoot, relativePath ) );
}

const distributionEntries = await readdir ( distributionRoot, { recursive: true } );
const allowedExtensions = new Set ( [ ".css", ".html", ".js", ".png", ".txt" ] );
const unexpectedEntries = distributionEntries.filter (
    entry => extname ( entry ) !== "" && !allowedExtensions.has ( extname ( entry ).toLowerCase () )
);
const forbiddenEntries = distributionEntries.filter (
    entry => /(^|[\\/])(pom\.xml|mvnw(?:\.cmd)?|target|src|tests?|node_modules)([\\/]|$)/i.test ( entry )
        || entry.endsWith ( ".map" )
);

if ( unexpectedEntries.length > 0 )
{
    throw new Error ( `The static artifact contains unexpected file types: ${unexpectedEntries.join ( ", " )}` );
}

if ( forbiddenEntries.length > 0 )
{
    throw new Error ( `The static artifact contains prohibited build content: ${forbiddenEntries.join ( ", " )}` );
}

const contentSecurityPolicy = indexSource.match (
    /<meta\s+http-equiv="Content-Security-Policy"\s+content="([^"]+)"/i
)?.[ 1 ];
const requiredPolicyDirectives =
[
    "default-src 'self'",
    "base-uri 'none'",
    "connect-src 'self' https: http://127.0.0.1:* http://localhost:*",
    "form-action 'self'",
    "object-src 'none'",
    "script-src 'self'",
    "worker-src 'self'",
];

if ( contentSecurityPolicy === undefined )
{
    throw new Error ( "The production index does not define a Content Security Policy." );
}

for ( const directive of requiredPolicyDirectives )
{
    if ( !contentSecurityPolicy.includes ( directive ) )
    {
        throw new Error ( `The Content Security Policy is missing: ${directive}` );
    }
}

const textEntries = distributionEntries.filter (
    entry => [ ".css", ".html", ".js", ".txt" ].includes ( extname ( entry ).toLowerCase () )
);
const textContents = await Promise.all (
    textEntries.map ( async entry => ( {
        entry,
        source: await readFile ( resolve ( distributionRoot, entry ), "utf8" ),
    } ) )
);
const prohibitedTextPatterns =
[
    { label: "a Windows user path", pattern: /[a-z]:\\users\\/i },
    { label: "a private workspace path", pattern: /[\\/]private[\\/]/i },
    { label: "an agent instruction file", pattern: /\b(?:AGENTS|CLAUDE)\.md\b/i },
    { label: "a development secret fixture", pattern: /phase-\d+-secret-token|user:secret@/i },
    { label: "a source map marker", pattern: /sourceMappingURL=/i },
];

for ( const { entry, source } of textContents )
{
    for ( const prohibitedPattern of prohibitedTextPatterns )
    {
        if ( prohibitedPattern.pattern.test ( source ) )
        {
            throw new Error ( `The static artifact ${entry} contains ${prohibitedPattern.label}.` );
        }
    }
}

const runtimePackages =
[
    { name: "react", noticeName: "React", version: "19.2.8", license: "MIT" },
    { name: "react-dom", noticeName: "React DOM", version: "19.2.8", license: "MIT" },
    { name: "scheduler", noticeName: "Scheduler", version: "0.27.0", license: "MIT" },
];
const noticesPath = resolve ( distributionRoot, "THIRD_PARTY_NOTICES.txt" );
const noticesSource = await readFile ( noticesPath, "utf8" );

for ( const runtimePackage of runtimePackages )
{
    const lockEntry = packageLock.packages [ `node_modules/${runtimePackage.name}` ];

    if ( lockEntry?.version !== runtimePackage.version || lockEntry?.license !== runtimePackage.license )
    {
        throw new Error ( `Runtime license metadata drifted for ${runtimePackage.name}.` );
    }

    if ( !noticesSource.includes ( `${runtimePackage.noticeName} ${runtimePackage.version}` ) )
    {
        throw new Error ( `Third-party notices omit ${runtimePackage.name} ${runtimePackage.version}.` );
    }
}

console.log (
    `Verified ${assetReferences.length} GitHub Pages asset references, ${textEntries.length} text files, `
    + `${runtimePackages.length} runtime licenses, and the production CSP beneath ${PROJECT_BASE}.`
);
