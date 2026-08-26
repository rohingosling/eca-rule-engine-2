// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    vitest.phase-16.config
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Configures the framework-independent Phase 16 browser-local server contract suite.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { defineConfig } from "vitest/config";

export default defineConfig (
    {
        test:
        {
            environment: "node",
            include: [ "tests/phase-16.spec.ts" ],
        },
    }
);
