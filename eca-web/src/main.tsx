// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    main
// Version: 2.0.0
// Date:    2026-08-02
// Author:  Rohin Gosling
//
// Description:
//
//   Starts the React presentation adapter for the static web client.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { StrictMode } from "react";
import { createRoot }  from "react-dom/client";

import { Application } from "./Application";
import "./application.css";

const applicationRoot = document.getElementById ( "root" );

if ( applicationRoot === null )
{
    throw new Error ( "The ECA web application root element is missing." );
}

createRoot ( applicationRoot ).render (
    <StrictMode>
        <Application />
    </StrictMode>
);
