// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    UX Upgrade Tests
// Version: 2.0.0
// Date:    2026-08-14
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies bounded structured Console history and declaration-order editing without a browser runtime.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { describe, expect, test } from "vitest";

import {
    MAXIMUM_CONSOLE_ENTRY_COUNT,
    appendConsoleEntry,
    consoleAnnouncementForEntries,
    consoleEntryFromMessage,
    type ConsoleEntry,
} from "../src/components/ConsolePanel";
import { reorderEntity, type AuthoringModel } from "../src/model";

function modelWithParameters (): AuthoringModel
{
    return {
        actions: [],
        conditions: [],
        conditionSets: [],
        description: "Test model",
        events: [],
        modelId: "test-model",
        name: "Test Model",
        parameters:
        [
            { description: "First", enumerationValues: [], id: "first", name: "First", type: "STRING" },
            { description: "Second", enumerationValues: [], id: "second", name: "Second", type: "STRING" },
            { description: "Third", enumerationValues: [], id: "third", name: "Third", type: "STRING" },
        ],
        payloads: [],
        rules: [],
        schemaVersion: "2.0",
    };
}

describe ( "structured Console history", () =>
{
    test ( "uses explicit severity and preserves a bracketed structured source", () =>
    {
        const entry = consoleEntryFromMessage ( "[Server] Pull failed: unavailable", 17, "error" );

        expect ( entry ).toMatchObject (
            {
                code: "SERVER_ERROR",
                identifier: "console-17",
                severity: "error",
                source: "Server",
                text: "Pull failed: unavailable",
            }
        );
    } );

    test ( "retains only the newest one thousand entries", () =>
    {
        let entries: readonly ConsoleEntry[] = [];

        for ( let sequence = 0; sequence < MAXIMUM_CONSOLE_ENTRY_COUNT + 4; sequence += 1 )
        {
            entries = appendConsoleEntry ( entries, consoleEntryFromMessage ( `[Test] Entry ${sequence}`, sequence ) );
        }

        expect ( entries ).toHaveLength ( MAXIMUM_CONSOLE_ENTRY_COUNT );
        expect ( entries [ 0 ]?.identifier ).toBe ( "console-4" );
        expect ( entries.at ( -1 )?.identifier ).toBe ( "console-1003" );
    } );

    test ( "announces only a newest entry whose severity filter is active", () =>
    {
        const messageEntry = consoleEntryFromMessage ( "[Application] Model loaded", 1, "message" );
        const warningEntry = consoleEntryFromMessage ( "[Validation] Name is recommended", 2, "warning" );

        expect ( consoleAnnouncementForEntries (
            [ messageEntry, warningEntry ],
            { error: true, message: true, warning: false }
        ) ).toBe ( "" );
        expect ( consoleAnnouncementForEntries (
            [ messageEntry, warningEntry ],
            { error: true, message: true, warning: true }
        ) ).toBe ( "[Validation] Name is recommended" );
        expect ( consoleAnnouncementForEntries (
            [ messageEntry ],
            { error: true, message: false, warning: true }
        ) ).toBe ( "" );
    } );
} );

describe ( "master-detail declaration order", () =>
{
    test ( "moves the selected declaration and preserves all entity data", () =>
    {
        const result = reorderEntity ( modelWithParameters (), "parameters", "second", -1 );

        expect ( result.errors ).toEqual ( [] );
        expect ( result.entityIdentifier ).toBe ( "second" );
        expect ( result.model?.parameters.map ( parameter => parameter.id ) ).toEqual (
            [ "second", "first", "third" ]
        );
    } );

    test ( "rejects movement beyond a declaration-list boundary", () =>
    {
        const result = reorderEntity ( modelWithParameters (), "parameters", "first", -1 );

        expect ( result.model ).toBeNull ();
        expect ( result.errors [ 0 ]?.field ).toBe ( "order" );
    } );
} );
