// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    vitest.phase-17.config
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Configures the framework-independent Phase 17 document lifecycle, file boundary, and model-editor suite.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { defineConfig } from "vitest/config";

export default defineConfig (
    {
        test:
        {
            environment: "node",
            include: [ "tests/phase-17.spec.ts" ],
        },
    }
);
