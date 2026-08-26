// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    verify-resources
// Version: 2.0.0
// Date:    2026-08-02
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies web localization and binary-resource parity with the desktop client.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { createHash }          from "node:crypto";
import { readFile }            from "node:fs/promises";
import { dirname, resolve }    from "node:path";
import { fileURLToPath }       from "node:url";

const scriptDirectory = dirname ( fileURLToPath ( import.meta.url ) );
const webRoot = resolve ( scriptDirectory, ".." );
const projectRoot = resolve ( webRoot, ".." );
const desktopResourceRoot = resolve (
    projectRoot,
    "eca-client",
    "src",
    "main",
    "resources",
    "com",
    "rohingosling",
    "eca",
    "client"
);
const desktopMessagesPath = resolve ( desktopResourceRoot, "messages.properties" );
const webMessagesPath = resolve ( webRoot, "src", "localization", "en.json" );
const webEquationRoot = resolve ( webRoot, "public", "assets", "equations" );
const webNoticeRoot = resolve ( webRoot, "public", "notices" );
const desktopEquationRoot = resolve ( desktopResourceRoot, "user-guide" );
const equationNames =
[
    "actions",
    "conditions",
    "condition-sets",
    "events",
    "model",
    "parameters",
    "payloads",
    "rules",
    "simulator",
];

function decodePropertiesValue ( value )
{
    return value
        .replace ( /\\u([0-9a-fA-F]{4})/g, ( _, code ) => String.fromCharCode ( Number.parseInt ( code, 16 ) ) )
        .replace ( /\\n/g, "\n" )
        .replace ( /\\r/g, "\r" )
        .replace ( /\\t/g, "\t" )
        .replace ( /\\([:=#! ])/g, "$1" )
        .replace ( /\\\\/g, "\\" );
}

function parseProperties ( source )
{
    const messages = new Map ();

    for ( const line of source.split ( /\r?\n/ ) )
    {
        const trimmedLine = line.trim ();

        if ( trimmedLine.length === 0 || trimmedLine.startsWith ( "#" ) || trimmedLine.startsWith ( "!" ) )
        {
            continue;
        }

        const separatorIndex = line.search ( /(?<!\\)[=:]/ );

        if ( separatorIndex < 0 )
        {
            throw new Error ( `Unsupported localization resource line: ${line}` );
        }

        const key = line.slice ( 0, separatorIndex ).trim ();
        const value = line.slice ( separatorIndex + 1 ).trimStart ();

        messages.set ( key, decodePropertiesValue ( value ) );
    }

    return messages;
}

function digest ( content )
{
    return createHash ( "sha256" ).update ( content ).digest ( "hex" );
}

const desktopMessages = parseProperties ( await readFile ( desktopMessagesPath, "utf8" ) );
const webMessages = JSON.parse ( await readFile ( webMessagesPath, "utf8" ) );
const mismatches = [];

for ( const [ key, value ] of Object.entries ( webMessages ) )
{
    if ( key.startsWith ( "web." ) )
    {
        continue;
    }

    const desktopValue = desktopMessages.get ( key );

    if ( desktopValue === undefined )
    {
        mismatches.push ( `${key}: missing from the desktop localization bundle` );
    }
    else if ( desktopValue !== value )
    {
        mismatches.push ( `${key}: web and desktop values differ` );
    }
}

for ( const equationName of equationNames )
{
    for ( const suffix of [ "", "-dark" ] )
    {
        const fileName = `${equationName}${suffix}.png`;
        const desktopContent = await readFile ( resolve ( desktopEquationRoot, fileName ) );
        const webContent = await readFile ( resolve ( webEquationRoot, fileName ) );

        if ( digest ( desktopContent ) !== digest ( webContent ) )
        {
            mismatches.push ( `${fileName}: web and desktop equation assets differ` );
        }
    }
}

const desktopIcon = await readFile ( resolve ( desktopResourceRoot, "eca-rule-engine.png" ) );
const webIcon = await readFile ( resolve ( webRoot, "public", "assets", "eca-rule-engine.png" ) );

if ( digest ( desktopIcon ) !== digest ( webIcon ) )
{
    mismatches.push ( "eca-rule-engine.png: web and desktop application icons differ" );
}

for ( const noticeName of [ "eca-rule-engine-license.txt", "fluent-ui-system-icons.txt" ] )
{
    const desktopNotice = await readFile ( resolve ( desktopResourceRoot, "notices", noticeName ) );
    const webNotice = await readFile ( resolve ( webNoticeRoot, noticeName ) );

    if ( digest ( desktopNotice ) !== digest ( webNotice ) )
    {
        mismatches.push ( `${noticeName}: web and desktop notice assets differ` );
    }
}

if ( mismatches.length > 0 )
{
    throw new Error ( `Web resource parity failed:\n- ${mismatches.join ( "\n- " )}` );
}

console.log ( `Verified ${Object.keys ( webMessages ).length} localized strings and ${equationNames.length * 2 + 1} binary resources.` );
