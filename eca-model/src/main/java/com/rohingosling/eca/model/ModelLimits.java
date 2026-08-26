//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Defines configurable authoring-model entity-count limits.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.model;

//*********************************************************************************************************************
// Class: ModelLimits
//
// Description:
//
//   Defines configurable authoring-model entity-count limits.
//
//*********************************************************************************************************************

public final class ModelLimits
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final int maximumEntitiesPerCategory;
    private final int maximumBindingsPerConditionSet;

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getMaximumEntitiesPerCategory
    //
    // Description:
    //
    //   Returns the maximum entities per category.
    //
    // Returns:
    //
    //   The maximum entities per category.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public int getMaximumEntitiesPerCategory ()
    {
        // Return the maximum entities per category to the caller.

        return this.maximumEntitiesPerCategory;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getMaximumBindingsPerConditionSet
    //
    // Description:
    //
    //   Returns the maximum bindings per condition set.
    //
    // Returns:
    //
    //   The maximum bindings per condition set.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public int getMaximumBindingsPerConditionSet ()
    {
        // Return the maximum bindings per condition set to the caller.

        return this.maximumBindingsPerConditionSet;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: ModelLimits
    //
    // Description:
    //
    //   Creates the ModelLimits instance from the supplied values.
    //
    // Arguments:
    //
    //   maximumEntitiesPerCategory (int):
    //     The maximum entities per category to use.
    //
    //   maximumBindingsPerConditionSet (int):
    //     The maximum bindings per condition set to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ModelLimits ( int maximumEntitiesPerCategory, int maximumBindingsPerConditionSet )
    {
        // Reject the operation when maximum entities per category is less than 0 or maximum bindings per condition set
        // is less than 0.

        if ( maximumEntitiesPerCategory < 0 || maximumBindingsPerConditionSet < 0 )
        {
            throw new IllegalArgumentException ( "Model limits must not be negative." );
        }

        this.maximumEntitiesPerCategory     = maximumEntitiesPerCategory;
        this.maximumBindingsPerConditionSet = maximumBindingsPerConditionSet;
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: defaults
    //
    // Description:
    //
    //   Creates a ModelLimits instance with the default configuration.
    //
    // Returns:
    //
    //   The resulting ModelLimits instance.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static ModelLimits defaults ()
    {
        // Return a newly constructed model limits containing the operation result.

        return new ModelLimits ( 10000, 1000 );
    }
}
