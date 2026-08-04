//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Defines the immutable, lossless authoring representation for all seven model entity categories.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

//*********************************************************************************************************************
// Class: AuthoringModel
//
// Description:
//
//   Defines the immutable, lossless authoring representation for all seven model entity categories.
//
//*********************************************************************************************************************

public final class AuthoringModel
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final String schemaVersion;
    private final String modelId;
    private final String name;
    private final String description;
    private final List <ParameterDefinition> parameters;
    private final List <PayloadDefinition> payloads;
    private final List <EventDefinition> events;
    private final List <ConditionDefinition> conditions;
    private final List <ConditionSetDefinition> conditionSets;
    private final List <ActionDefinition> actions;
    private final List <RuleDefinition> rules;

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getSchemaVersion
    //
    // Description:
    //
    //   Returns the schema version.
    //
    // Returns:
    //
    //   The schema version.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String getSchemaVersion ()
    {
        // Return the schema version to the caller.

        return this.schemaVersion;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getModelId
    //
    // Description:
    //
    //   Returns the model id.
    //
    // Returns:
    //
    //   The model id.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String getModelId ()
    {
        // Return the model ID to the caller.

        return this.modelId;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getName
    //
    // Description:
    //
    //   Returns the name.
    //
    // Returns:
    //
    //   The name.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String getName ()
    {
        // Return the name to the caller.

        return this.name;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getDescription
    //
    // Description:
    //
    //   Returns the description.
    //
    // Returns:
    //
    //   The description.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String getDescription ()
    {
        // Return the description to the caller.

        return this.description;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getParameters
    //
    // Description:
    //
    //   Returns the parameters.
    //
    // Returns:
    //
    //   The parameters.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public List <ParameterDefinition> getParameters ()
    {
        // Return the parameters to the caller.

        return this.parameters;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getPayloads
    //
    // Description:
    //
    //   Returns the payloads.
    //
    // Returns:
    //
    //   The payloads.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public List <PayloadDefinition> getPayloads ()
    {
        // Return the payloads to the caller.

        return this.payloads;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getEvents
    //
    // Description:
    //
    //   Returns the events.
    //
    // Returns:
    //
    //   The events.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public List <EventDefinition> getEvents ()
    {
        // Return the events to the caller.

        return this.events;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getConditions
    //
    // Description:
    //
    //   Returns the conditions.
    //
    // Returns:
    //
    //   The conditions.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public List <ConditionDefinition> getConditions ()
    {
        // Return the conditions to the caller.

        return this.conditions;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getConditionSets
    //
    // Description:
    //
    //   Returns the condition sets.
    //
    // Returns:
    //
    //   The condition sets.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public List <ConditionSetDefinition> getConditionSets ()
    {
        // Return the condition sets to the caller.

        return this.conditionSets;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getActions
    //
    // Description:
    //
    //   Returns the actions.
    //
    // Returns:
    //
    //   The actions.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public List <ActionDefinition> getActions ()
    {
        // Return the actions to the caller.

        return this.actions;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getRules
    //
    // Description:
    //
    //   Returns the rules.
    //
    // Returns:
    //
    //   The rules.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public List <RuleDefinition> getRules ()
    {
        // Return the rules to the caller.

        return this.rules;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: AuthoringModel
    //
    // Description:
    //
    //   Creates the AuthoringModel instance from the supplied values.
    //
    // Arguments:
    //
    //   schemaVersion (String):
    //     The schema version to use.
    //
    //   modelId (String):
    //     The model id to use.
    //
    //   name (String):
    //     The name to use.
    //
    //   description (String):
    //     The description to use.
    //
    //   parameters (Collection <ParameterDefinition>):
    //     The parameters to use.
    //
    //   payloads (Collection <PayloadDefinition>):
    //     The payloads to use.
    //
    //   events (Collection <EventDefinition>):
    //     The events to use.
    //
    //   conditions (Collection <ConditionDefinition>):
    //     The conditions to use.
    //
    //   conditionSets (Collection <ConditionSetDefinition>):
    //     The condition sets to use.
    //
    //   actions (Collection <ActionDefinition>):
    //     The actions to use.
    //
    //   rules (Collection <RuleDefinition>):
    //     The rules to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public AuthoringModel (
        String schemaVersion,
        String modelId,
        String name,
        String description,
        Collection <ParameterDefinition> parameters,
        Collection <PayloadDefinition> payloads,
        Collection <EventDefinition> events,
        Collection <ConditionDefinition> conditions,
        Collection <ConditionSetDefinition> conditionSets,
        Collection <ActionDefinition> actions,
        Collection <RuleDefinition> rules
    )
    {
        // Apply require non null and immutable list to the objects for the authoring model operation.

        this.schemaVersion = Objects.requireNonNull ( schemaVersion, "schemaVersion" );
        this.modelId       = Objects.requireNonNull ( modelId, "modelId" );
        this.name          = Objects.requireNonNull ( name, "name" );
        this.description   = Objects.requireNonNull ( description, "description" );
        this.parameters    = immutableList ( parameters, "parameters" );
        this.payloads      = immutableList ( payloads, "payloads" );
        this.events        = immutableList ( events, "events" );
        this.conditions    = immutableList ( conditions, "conditions" );
        this.conditionSets = immutableList ( conditionSets, "conditionSets" );
        this.actions       = immutableList ( actions, "actions" );
        this.rules         = immutableList ( rules, "rules" );
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: immutableList
    //
    // Description:
    //
    //   Performs the immutable list operation.
    //
    // Arguments:
    //
    //   values (Collection <T>):
    //     The values to use.
    //
    //   fieldName (String):
    //     The field name to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static <T> List <T> immutableList ( Collection <T> values, String fieldName )
    {
        // Validate the required values before continuing.

        Objects.requireNonNull ( values, fieldName );

        // Initialize the copied values with a new array list.

        ArrayList <T> copiedValues = new ArrayList <T> ();

        // Process each value supplied by values.

        for ( T value : values )
        {
            // Add objects require non null value field name + " must not contain null" to the copied values.

            copiedValues.add ( Objects.requireNonNull ( value, fieldName + " must not contain null" ) );
        }

        // Return an immutable copy of copied values.

        return Collections.unmodifiableList ( copiedValues );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: immutableValueMap
    //
    // Description:
    //
    //   Performs the immutable value map operation.
    //
    // Arguments:
    //
    //   values (Map <String, ?>):
    //     The values to use.
    //
    //   fieldName (String):
    //     The field name to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Map <String, Object> immutableValueMap ( Map <String, ?> values, String fieldName )
    {
        // Validate the required values before continuing.

        Objects.requireNonNull ( values, fieldName );

        // Initialize the copied values with a new linked hash map.

        LinkedHashMap <String, Object> copiedValues = new LinkedHashMap <String, Object> ();

        // Process each entry supplied by values entry set.

        for ( Map.Entry <String, ?> entry : values.entrySet () )
        {
            // Initialize the key by applying require non null and get key.

            String key = Objects.requireNonNull ( entry.getKey (), fieldName + " must not contain null keys" );

            // Store immutable value entry value under key in the copied values.

            copiedValues.put ( key, immutableValue ( entry.getValue () ) );
        }

        // Return an immutable copy of copied values.

        return Collections.unmodifiableMap ( copiedValues );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: immutableValue
    //
    // Description:
    //
    //   Performs the immutable value operation.
    //
    // Arguments:
    //
    //   value (Object):
    //     The value to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Object immutableValue ( Object value )
    {
        // Handle the branch where value is a map ? ?.

        if ( value instanceof Map <?, ?> )
        {
            Map <?, ?> sourceMap                    = (Map <?, ?>) value;

            // Initialize the copied map with a new linked hash map.

            LinkedHashMap <Object, Object> copiedMap = new LinkedHashMap <Object, Object> ();

            // Process each entry supplied by source map entry set.

            for ( Map.Entry <?, ?> entry : sourceMap.entrySet () )
            {
                // Store immutable value entry value under entry key in the copied map.

                copiedMap.put ( entry.getKey (), immutableValue ( entry.getValue () ) );
            }

            // Return an immutable copy of copied map when value is a map ? ?.

            return Collections.unmodifiableMap ( copiedMap );
        }

        // Handle the branch where value is a collection ?.

        if ( value instanceof Collection <?> )
        {
            // Initialize the copied values with a new array list.

            ArrayList <Object> copiedValues = new ArrayList <Object> ();

            // Process each element supplied by collection ? value.

            for ( Object element : (Collection <?>) value )
            {
                // Add immutable value element to the copied values.

                copiedValues.add ( immutableValue ( element ) );
            }

            // Return an immutable copy of copied values when value is a collection ?.

            return Collections.unmodifiableList ( copiedValues );
        }

        // Return the value to the caller.

        return value;
    }

    //*****************************************************************************************************************
    // Class: EntityDefinition
    //
    // Description:
    //
    //   Provides the entity definition behavior.
    //
    //*****************************************************************************************************************

    public abstract static class EntityDefinition
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String id;
        private final String name;
        private final String description;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getId
        //
        // Description:
        //
        //   Returns the id.
        //
        // Returns:
        //
        //   The id.
        //
        //-------------------------------------------------------------------------------------------------------------

        public final String getId ()
        {
            // Return the ID to the caller.

            return this.id;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getName
        //
        // Description:
        //
        //   Returns the name.
        //
        // Returns:
        //
        //   The name.
        //
        //-------------------------------------------------------------------------------------------------------------

        public final String getName ()
        {
            // Return the name to the caller.

            return this.name;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getDescription
        //
        // Description:
        //
        //   Returns the description.
        //
        // Returns:
        //
        //   The description.
        //
        //-------------------------------------------------------------------------------------------------------------

        public final String getDescription ()
        {
            // Return the description to the caller.

            return this.description;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: EntityDefinition
        //
        // Description:
        //
        //   Creates the EntityDefinition instance from the supplied values.
        //
        // Arguments:
        //
        //   id (String):
        //     The id to use.
        //
        //   name (String):
        //     The name to use.
        //
        //   description (String):
        //     The description to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        protected EntityDefinition ( String id, String name, String description )
        {
            // Validate the required ID, name, and description before continuing.

            this.id          = Objects.requireNonNull ( id, "id" );
            this.name        = Objects.requireNonNull ( name, "name" );
            this.description = Objects.requireNonNull ( description, "description" );
        }
    }

    //*****************************************************************************************************************
    // Class: ParameterDefinition
    //
    // Description:
    //
    //   Provides the parameter definition behavior.
    //
    //*****************************************************************************************************************

    public static final class ParameterDefinition extends EntityDefinition
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String type;
        private final List <String> enumerationValues;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getType
        //
        // Description:
        //
        //   Returns the type.
        //
        // Returns:
        //
        //   The type.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getType ()
        {
            // Return the type to the caller.

            return this.type;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getEnumerationValues
        //
        // Description:
        //
        //   Returns the enumeration values.
        //
        // Returns:
        //
        //   The enumeration values.
        //
        //-------------------------------------------------------------------------------------------------------------

        public List <String> getEnumerationValues ()
        {
            // Return the enumeration values to the caller.

            return this.enumerationValues;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: ParameterDefinition
        //
        // Description:
        //
        //   Creates the ParameterDefinition instance from the supplied values.
        //
        // Arguments:
        //
        //   id (String):
        //     The id to use.
        //
        //   name (String):
        //     The name to use.
        //
        //   description (String):
        //     The description to use.
        //
        //   type (String):
        //     The type to use.
        //
        //   enumerationValues (Collection <String>):
        //     The enumeration values to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        public ParameterDefinition (
            String id,
            String name,
            String description,
            String type,
            Collection <String> enumerationValues
        )
        {
            // Initialize the inherited state through the base-class constructor.

            super ( id, name, description );

            // Apply require non null and immutable list to the objects for the parameter definition operation.

            this.type              = Objects.requireNonNull ( type, "type" );
            this.enumerationValues = immutableList ( enumerationValues, "enumerationValues" );
        }
    }

    //*****************************************************************************************************************
    // Class: PayloadDefinition
    //
    // Description:
    //
    //   Provides the payload definition behavior.
    //
    //*****************************************************************************************************************

    public static final class PayloadDefinition extends EntityDefinition
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final List <String> parameterIds;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getParameterIds
        //
        // Description:
        //
        //   Returns the parameter ids.
        //
        // Returns:
        //
        //   The parameter ids.
        //
        //-------------------------------------------------------------------------------------------------------------

        public List <String> getParameterIds ()
        {
            // Return the parameter IDs to the caller.

            return this.parameterIds;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: PayloadDefinition
        //
        // Description:
        //
        //   Creates the PayloadDefinition instance from the supplied values.
        //
        // Arguments:
        //
        //   id (String):
        //     The id to use.
        //
        //   name (String):
        //     The name to use.
        //
        //   description (String):
        //     The description to use.
        //
        //   parameterIds (Collection <String>):
        //     The parameter ids to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        public PayloadDefinition (
            String id,
            String name,
            String description,
            Collection <String> parameterIds
        )
        {
            // Initialize the inherited state through the base-class constructor.

            super ( id, name, description );

            // Update the parameter IDs from the immutable list result.

            this.parameterIds = immutableList ( parameterIds, "parameterIds" );
        }
    }

    //*****************************************************************************************************************
    // Class: EventDefinition
    //
    // Description:
    //
    //   Provides the event definition behavior.
    //
    //*****************************************************************************************************************

    public static final class EventDefinition extends EntityDefinition
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String payloadId;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getPayloadId
        //
        // Description:
        //
        //   Returns the payload id.
        //
        // Returns:
        //
        //   The payload id.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getPayloadId ()
        {
            // Return the payload ID to the caller.

            return this.payloadId;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: EventDefinition
        //
        // Description:
        //
        //   Creates the EventDefinition instance from the supplied values.
        //
        // Arguments:
        //
        //   id (String):
        //     The id to use.
        //
        //   name (String):
        //     The name to use.
        //
        //   description (String):
        //     The description to use.
        //
        //   payloadId (String):
        //     The payload id to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        public EventDefinition ( String id, String name, String description, String payloadId )
        {
            // Initialize the inherited state through the base-class constructor.

            super ( id, name, description );

            // Validate the required payload ID before continuing.

            this.payloadId = Objects.requireNonNull ( payloadId, "payloadId" );
        }
    }

    //*****************************************************************************************************************
    // Class: ConditionDefinition
    //
    // Description:
    //
    //   Provides the condition definition behavior.
    //
    //*****************************************************************************************************************

    public static final class ConditionDefinition extends EntityDefinition
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String parameterId;
        private final String operator;
        private final Object firstValue;
        private final Object secondValue;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getParameterId
        //
        // Description:
        //
        //   Returns the parameter id.
        //
        // Returns:
        //
        //   The parameter id.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getParameterId ()
        {
            // Return the parameter ID to the caller.

            return this.parameterId;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getOperator
        //
        // Description:
        //
        //   Returns the operator.
        //
        // Returns:
        //
        //   The operator.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getOperator ()
        {
            // Return the operator to the caller.

            return this.operator;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getFirstValue
        //
        // Description:
        //
        //   Returns the first value.
        //
        // Returns:
        //
        //   The first value.
        //
        //-------------------------------------------------------------------------------------------------------------

        public Object getFirstValue ()
        {
            // Return the first value to the caller.

            return this.firstValue;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getSecondValue
        //
        // Description:
        //
        //   Returns the second value.
        //
        // Returns:
        //
        //   The second value.
        //
        //-------------------------------------------------------------------------------------------------------------

        public Object getSecondValue ()
        {
            // Return the second value to the caller.

            return this.secondValue;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: hasPredicate
        //
        // Description:
        //
        //   Indicates whether predicate.
        //
        // Returns:
        //
        //   `true` when the condition is satisfied; otherwise `false`.
        //
        //-------------------------------------------------------------------------------------------------------------

        public boolean hasPredicate ()
        {
            // Return whether operator is available.

            return this.operator != null;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/2: ConditionDefinition
        //
        // Description:
        //
        //   Creates the ConditionDefinition instance from the supplied values.
        //
        // Arguments:
        //
        //   id (String):
        //     The id to use.
        //
        //   name (String):
        //     The name to use.
        //
        //   description (String):
        //     The description to use.
        //
        //   parameterId (String):
        //     The parameter id to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        public ConditionDefinition ( String id, String name, String description, String parameterId )
        {
            // Delegate initialization to the primary condition definition constructor.

            this ( id, name, description, parameterId, null, null, null );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 2/2: ConditionDefinition
        //
        // Description:
        //
        //   Creates the ConditionDefinition instance from the supplied values.
        //
        // Arguments:
        //
        //   id (String):
        //     The id to use.
        //
        //   name (String):
        //     The name to use.
        //
        //   description (String):
        //     The description to use.
        //
        //   parameterId (String):
        //     The parameter id to use.
        //
        //   operator (String):
        //     The operator to use.
        //
        //   firstValue (Object):
        //     The first value to use.
        //
        //   secondValue (Object):
        //     The second value to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        public ConditionDefinition (
            String id,
            String name,
            String description,
            String parameterId,
            String operator,
            Object firstValue,
            Object secondValue
        )
        {
            // Initialize the inherited state through the base-class constructor.

            super ( id, name, description );

            // Validate the required parameter ID before continuing.

            this.parameterId = Objects.requireNonNull ( parameterId, "parameterId" );
            this.operator    = operator;

            // Convert the supplied authoring value to an immutable representation.

            this.firstValue  = immutableValue ( firstValue );
            this.secondValue = immutableValue ( secondValue );
        }
    }

    //*****************************************************************************************************************
    // Class: ConditionSetDefinition
    //
    // Description:
    //
    //   Provides the condition set definition behavior.
    //
    //*****************************************************************************************************************

    public static final class ConditionSetDefinition extends EntityDefinition
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final Map <String, Object> bindings;
        private final List <String> conditionIds;
        private final boolean predefinedConditions;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getBindings
        //
        // Description:
        //
        //   Returns the bindings.
        //
        // Returns:
        //
        //   The bindings.
        //
        //-------------------------------------------------------------------------------------------------------------

        public Map <String, Object> getBindings ()
        {
            // Return the bindings to the caller.

            return this.bindings;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getConditionIds
        //
        // Description:
        //
        //   Returns the condition ids.
        //
        // Returns:
        //
        //   The condition ids.
        //
        //-------------------------------------------------------------------------------------------------------------

        public List <String> getConditionIds ()
        {
            // Return the condition IDs to the caller.

            return this.conditionIds;
        }

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: usesPredefinedConditions
        //
        // Description:
        //
        //   Performs the uses predefined conditions operation.
        //
        // Returns:
        //
        //   The result of the operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        public boolean usesPredefinedConditions ()
        {
            // Return the predefined conditions to the caller.

            return this.predefinedConditions;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/2: ConditionSetDefinition
        //
        // Description:
        //
        //   Creates the ConditionSetDefinition instance from the supplied values.
        //
        // Arguments:
        //
        //   id (String):
        //     The id to use.
        //
        //   name (String):
        //     The name to use.
        //
        //   description (String):
        //     The description to use.
        //
        //   bindings (Map <String, ?>):
        //     The bindings to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        public ConditionSetDefinition (
            String id,
            String name,
            String description,
            Map <String, ?> bindings
        )
        {
            // Initialize the inherited state through the base-class constructor.

            super ( id, name, description );

            // Apply immutable value map and of to the list for the condition set definition operation.

            this.bindings             = immutableValueMap ( bindings, "bindings" );
            this.conditionIds         = List.of ();
            this.predefinedConditions = false;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 2/2: ConditionSetDefinition
        //
        // Description:
        //
        //   Creates the ConditionSetDefinition instance from the supplied values.
        //
        // Arguments:
        //
        //   id (String):
        //     The id to use.
        //
        //   name (String):
        //     The name to use.
        //
        //   description (String):
        //     The description to use.
        //
        //   conditionIds (Collection <String>):
        //     The condition ids to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        public ConditionSetDefinition (
            String id,
            String name,
            String description,
            Collection <String> conditionIds
        )
        {
            // Initialize the inherited state through the base-class constructor.

            super ( id, name, description );

            // Apply of and immutable list to the map for the condition set definition operation.

            this.bindings             = Map.of ();
            this.conditionIds         = immutableList ( conditionIds, "conditionIds" );
            this.predefinedConditions = true;
        }
    }

    //*****************************************************************************************************************
    // Class: ActionDefinition
    //
    // Description:
    //
    //   Provides the action definition behavior.
    //
    //*****************************************************************************************************************

    public static final class ActionDefinition extends EntityDefinition
    {
        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: ActionDefinition
        //
        // Description:
        //
        //   Creates the ActionDefinition instance from the supplied values.
        //
        // Arguments:
        //
        //   id (String):
        //     The id to use.
        //
        //   name (String):
        //     The name to use.
        //
        //   description (String):
        //     The description to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        public ActionDefinition ( String id, String name, String description )
        {
            // Initialize the inherited state through the base-class constructor.

            super ( id, name, description );
        }
    }

    //*****************************************************************************************************************
    // Class: RuleDefinition
    //
    // Description:
    //
    //   Provides the rule definition behavior.
    //
    //*****************************************************************************************************************

    public static final class RuleDefinition extends EntityDefinition
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String eventId;
        private final String conditionSetId;
        private final String actionId;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getEventId
        //
        // Description:
        //
        //   Returns the event id.
        //
        // Returns:
        //
        //   The event id.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getEventId ()
        {
            // Return the event ID to the caller.

            return this.eventId;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getConditionSetId
        //
        // Description:
        //
        //   Returns the condition set id.
        //
        // Returns:
        //
        //   The condition set id.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getConditionSetId ()
        {
            // Return the condition set ID to the caller.

            return this.conditionSetId;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getActionId
        //
        // Description:
        //
        //   Returns the action id.
        //
        // Returns:
        //
        //   The action id.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getActionId ()
        {
            // Return the action ID to the caller.

            return this.actionId;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: RuleDefinition
        //
        // Description:
        //
        //   Creates the RuleDefinition instance from the supplied values.
        //
        // Arguments:
        //
        //   id (String):
        //     The id to use.
        //
        //   name (String):
        //     The name to use.
        //
        //   description (String):
        //     The description to use.
        //
        //   eventId (String):
        //     The event id to use.
        //
        //   conditionSetId (String):
        //     The condition set id to use.
        //
        //   actionId (String):
        //     The action id to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        public RuleDefinition (
            String id,
            String name,
            String description,
            String eventId,
            String conditionSetId,
            String actionId
        )
        {
            // Initialize the inherited state through the base-class constructor.

            super ( id, name, description );

            // Validate the required event ID, condition set ID, and action ID before continuing.

            this.eventId       = Objects.requireNonNull ( eventId, "eventId" );
            this.conditionSetId = Objects.requireNonNull ( conditionSetId, "conditionSetId" );
            this.actionId      = Objects.requireNonNull ( actionId, "actionId" );
        }
    }
}
