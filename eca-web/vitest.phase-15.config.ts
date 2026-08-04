// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    vitest.phase-15.config
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Configures the framework-independent Phase 15 TypeScript compiler, evaluator, parity, and property suite.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { defineConfig } from "vitest/config";

export default defineConfig (
    {
        test:
        {
            environment: "node",
            include: [ "tests/phase-15.spec.ts" ],
        },
    }
);
