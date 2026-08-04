// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    vitest.phase-18.config
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Configures the Phase 18 gateway, Simulator, settings, and transport classification suite.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { defineConfig } from "vitest/config";

export default defineConfig (
    {
        test:
        {
            environment: "node",
            include: [ "tests/phase-18.spec.ts" ],
        },
    }
);
