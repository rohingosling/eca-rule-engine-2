//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Resolves the context-aware desktop context-sensitive help from the active detail panel and navigation category.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.client;

//*********************************************************************************************************************
// Class: ClientUserGuide
//
// Description:
//
//   Resolves the context-aware desktop context-sensitive help from the active detail panel and navigation category.
//
//*********************************************************************************************************************

public final class ClientUserGuide
{
    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: ClientUserGuide
    //
    // Description:
    //
    //   Creates the ClientUserGuide instance from the supplied values.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private ClientUserGuide ()
    {
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: resolve
    //
    // Description:
    //
    //   Performs the resolve operation.
    //
    // Arguments:
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   simulatorSelected (boolean):
    //     The simulator selected to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static Context resolve ( String categoryIdentifier, boolean simulatorSelected )
    {
        // Stop this path and return its result when simulator selected is true.

        if ( simulatorSelected )
        {
            // Return the simulator to the caller when simulator selected is true.

            return Context.SIMULATOR;
        }

        // Stop this path and return its result when category identifier is unavailable.

        if ( categoryIdentifier == null )
        {
            // Return the model to the caller when category identifier is unavailable.

            return Context.MODEL;
        }

        // Return the resolve result to the caller.

        return switch ( categoryIdentifier )
        {
            // Handle "parameters" through this switch branch.

            case "parameters"     -> Context.PARAMETERS;

            // Handle "payloads" through this switch branch.

            case "payloads"       -> Context.PAYLOADS;

            // Handle "events" through this switch branch.

            case "events"         -> Context.EVENTS;

            // Handle "conditions" through this switch branch.

            case "conditions"     -> Context.CONDITIONS;

            // Handle "condition-sets" through this switch branch.

            case "condition-sets" -> Context.CONDITION_SETS;

            // Handle "actions" through this switch branch.

            case "actions"        -> Context.ACTIONS;

            // Handle "rules" through this switch branch.

            case "rules"          -> Context.RULES;

            // Handle the default case through this switch branch.

            default               -> throw new IllegalArgumentException (
                "Unsupported user-guide category: " + categoryIdentifier
            );
        };
    }

    //=================================================================================================================
    // User Defined Data Types
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Enum: Context
    //
    // Description:
    //
    //   Enumerates the supported context values.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public enum Context
    {
        //=============================================================================================================
        // Constants
        //=============================================================================================================

        MODEL          ( "model"          ),
        PARAMETERS     ( "parameters"     ),
        PAYLOADS       ( "payloads"       ),
        EVENTS         ( "events"         ),
        CONDITIONS     ( "conditions"     ),
        CONDITION_SETS ( "condition-sets" ),
        ACTIONS        ( "actions"        ),
        RULES          ( "rules"          ),
        SIMULATOR      ( "simulator"      );

        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String localizationSuffix;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getLocalizationSuffix
        //
        // Description:
        //
        //   Returns the localization suffix.
        //
        // Returns:
        //
        //   The localization suffix.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getLocalizationSuffix ()
        {
            // Return the localization suffix to the caller.

            return this.localizationSuffix;
        }

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getEquationResourceName
        //
        // Description:
        //
        //   Returns the equation resource name.
        //
        // Arguments:
        //
        //   darkMode (boolean):
        //     The dark mode to use.
        //
        // Returns:
        //
        //   The equation resource name.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getEquationResourceName ( boolean darkMode )
        {
            // Return the composed get equation resource name value.

            return "user-guide/" + this.localizationSuffix + ( darkMode ? "-dark" : "" ) + ".png";
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: Context
        //
        // Description:
        //
        //   Creates the Context instance from the supplied values.
        //
        // Arguments:
        //
        //   localizationSuffix (String):
        //     The localization suffix to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        Context ( String localizationSuffix )
        {
            this.localizationSuffix = localizationSuffix;
        }
    }
}
