// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    vite.config
// Version: 2.0.0
// Date:    2026-08-02
// Author:  Rohin Gosling
//
// Description:
//
//   Configures the self-contained GitHub Pages production bundle.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { defineConfig } from "vite";
import react            from "@vitejs/plugin-react";

export default defineConfig (
    {
        base: "/eca-rule-engine-2/",
        plugins: [ react () ],
        build:
        {
            outDir: "dist",
            emptyOutDir: true,
            sourcemap: false,
        },
    }
);
