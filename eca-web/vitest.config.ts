// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    vitest.config
// Version: 2.0.0
// Date:    2026-08-02
// Author:  Rohin Gosling
//
// Description:
//
//   Configures the framework-independent Phase 14 TypeScript model and JSON contract suite.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { defineConfig } from "vitest/config";

export default defineConfig (
    {
        test:
        {
            environment: "node",
            include: [ "tests/phase-14.spec.ts" ],
        },
    }
);
