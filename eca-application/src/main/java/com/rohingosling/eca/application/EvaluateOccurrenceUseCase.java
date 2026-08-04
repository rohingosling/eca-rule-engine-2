//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Defines the application port for evaluating one framework-neutral occurrence document.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.application;

import com.rohingosling.eca.model.OccurrenceDocument;

//---------------------------------------------------------------------------------------------------------------------
// Interface: EvaluateOccurrenceUseCase
//
// Description:
//
//   Defines the application port for evaluating one framework-neutral occurrence document.
//
//---------------------------------------------------------------------------------------------------------------------

public interface EvaluateOccurrenceUseCase
{
    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: evaluateOccurrence
    //
    // Description:
    //
    //   Performs the evaluate occurrence operation.
    //
    // Arguments:
    //
    //   occurrenceDocument (OccurrenceDocument):
    //     The occurrence document to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    HostedEvaluation evaluateOccurrence ( OccurrenceDocument occurrenceDocument );
}
