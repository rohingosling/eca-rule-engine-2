//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Identifies a safe, non-sensitive hosted-model failure category for structured logs.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.application;

//---------------------------------------------------------------------------------------------------------------------
// Enum: HostedModelFailureStage
//
// Description:
//
//   Identifies a safe, non-sensitive hosted-model failure category for structured logs.
//
//---------------------------------------------------------------------------------------------------------------------

public enum HostedModelFailureStage
{
    STARTUP,
    PREPARATION,
    PERSISTENCE
}
