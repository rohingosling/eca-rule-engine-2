//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Performs aggregate semantic validation of a lossless authoring model.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import com.rohingosling.eca.domain.ConditionOperator;
import com.rohingosling.eca.domain.ParameterType;

//*********************************************************************************************************************
// Class: ModelValidator
//
// Description:
//
//   Performs aggregate semantic validation of a lossless authoring model.
//
//*********************************************************************************************************************

public final class ModelValidator
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    public static final String BLANK_FIELD                    = "BLANK_FIELD";
    public static final String DUPLICATE_IDENTIFIER           = "DUPLICATE_IDENTIFIER";
    public static final String DUPLICATE_PARAMETER_BINDING    = "DUPLICATE_PARAMETER_BINDING";
    public static final String DUPLICATE_REFERENCE            = "DUPLICATE_REFERENCE";
    public static final String ENUM_VALUE_OUT_OF_DOMAIN       = "ENUM_VALUE_OUT_OF_DOMAIN";
    public static final String INVALID_CONCRETE_VALUE         = "INVALID_CONCRETE_VALUE";
    public static final String INVALID_CONDITION_OPERATOR     = "INVALID_CONDITION_OPERATOR";
    public static final String INVALID_CONDITION_RANGE        = "INVALID_CONDITION_RANGE";
    public static final String INVALID_ENUM_DOMAIN            = "INVALID_ENUM_DOMAIN";
    public static final String INVALID_IDENTIFIER             = "INVALID_IDENTIFIER";
    public static final String INVALID_PARAMETER_TYPE         = "INVALID_PARAMETER_TYPE";
    public static final String NON_INTEGRAL_INTEGER           = "NON_INTEGRAL_INTEGER";
    public static final String RULE_CONDITION_NOT_PERMITTED   = "RULE_CONDITION_NOT_PERMITTED";
    public static final String SIZE_LIMIT_EXCEEDED            = "SIZE_LIMIT_EXCEEDED";
    public static final String UNRESOLVED_REFERENCE           = "UNRESOLVED_REFERENCE";
    public static final String UNSUPPORTED_SCHEMA_VERSION     = "UNSUPPORTED_SCHEMA_VERSION";

    private static final String SUPPORTED_SCHEMA_VERSION = "1.0";
    private static final Pattern IDENTIFIER_PATTERN =
        Pattern.compile ( "^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$" );

    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final ModelLimits limits;

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/2: ModelValidator
    //
    // Description:
    //
    //   Creates the ModelValidator instance from the supplied values.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ModelValidator ()
    {
        // Apply this and defaults to the model limits for the model validator operation.

        this ( ModelLimits.defaults () );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 2/2: ModelValidator
    //
    // Description:
    //
    //   Creates the ModelValidator instance from the supplied values.
    //
    // Arguments:
    //
    //   limits (ModelLimits):
    //     The limits to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ModelValidator ( ModelLimits limits )
    {
        // Validate the required limits before continuing.

        this.limits = Objects.requireNonNull ( limits, "limits" );
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validate
    //
    // Description:
    //
    //   Performs the validate operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public List <ValidationDiagnostic> validate ( AuthoringModel model )
    {
        // Validate the required model before continuing.

        Objects.requireNonNull ( model, "model" );

        // Initialize the diagnostics with a new array list.

        ArrayList <ValidationDiagnostic> diagnostics = new ArrayList <ValidationDiagnostic> ();

        // Perform the validate top level and validate sizes calls required by the validate operation.

        validateTopLevel ( model, diagnostics );
        validateSizes ( model, diagnostics );

        // Prepare the parameters, payloads, events, conditions, condition sets, and actions values needed by the
        // validate operation.

        Map <String, AuthoringModel.ParameterDefinition> parameters =
            indexEntities ( model.getParameters (), "parameters", diagnostics );
        Map <String, AuthoringModel.PayloadDefinition> payloads =
            indexEntities ( model.getPayloads (), "payloads", diagnostics );
        Map <String, AuthoringModel.EventDefinition> events =
            indexEntities ( model.getEvents (), "events", diagnostics );
        Map <String, AuthoringModel.ConditionDefinition> conditions =
            indexEntities ( model.getConditions (), "conditions", diagnostics );
        Map <String, AuthoringModel.ConditionSetDefinition> conditionSets =
            indexEntities ( model.getConditionSets (), "conditionSets", diagnostics );
        Map <String, AuthoringModel.ActionDefinition> actions =
            indexEntities ( model.getActions (), "actions", diagnostics );

        // Apply index entities, get rules, validate parameters, get parameters, validate payloads, get payloads,
        // validate events, get events, validate conditions, get conditions, validate condition sets, get condition
        // sets, and validate rules to the model for the validate operation.

        indexEntities ( model.getRules (), "rules", diagnostics );

        validateParameters ( model.getParameters (), diagnostics );
        validatePayloads ( model.getPayloads (), parameters, diagnostics );
        validateEvents ( model.getEvents (), payloads, diagnostics );
        validateConditions ( model.getConditions (), parameters, diagnostics );
        validateConditionSets ( model.getConditionSets (), conditions, parameters, diagnostics );
        validateRules (
            model.getRules (),
            events,
            payloads,
            conditions,
            conditionSets,
            actions,
            diagnostics
        );

        // Return an immutable copy of diagnostics.

        return List.copyOf ( diagnostics );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validateTopLevel
    //
    // Description:
    //
    //   Performs the validate top level operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   diagnostics (Collection <ValidationDiagnostic>):
    //     The diagnostics to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void validateTopLevel (
        AuthoringModel model,
        Collection <ValidationDiagnostic> diagnostics
    )
    {
        // Handle the branch where supported schema version differs from model schema version.

        if ( !SUPPORTED_SCHEMA_VERSION.equals ( model.getSchemaVersion () ) )
        {
            // Apply add diagnostic and get model ID to the model for the validate top level operation.

            addDiagnostic (
                diagnostics,
                model.getModelId (),
                "schemaVersion",
                UNSUPPORTED_SCHEMA_VERSION,
                "Use schema version " + SUPPORTED_SCHEMA_VERSION + "."
            );
        }

        // Apply validate identifier, get model ID, validate text, get name, and get description to the model for the
        // validate top level operation.

        validateIdentifier ( model.getModelId (), model.getModelId (), "modelId", diagnostics );
        validateText ( model.getModelId (), "name", model.getName (), diagnostics );
        validateText ( model.getModelId (), "description", model.getDescription (), diagnostics );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validateSizes
    //
    // Description:
    //
    //   Performs the validate sizes operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   diagnostics (Collection <ValidationDiagnostic>):
    //     The diagnostics to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void validateSizes (
        AuthoringModel model,
        Collection <ValidationDiagnostic> diagnostics
    )
    {
        // Perform the validate category size, get model ID, size, get parameters, get payloads, get events, get
        // conditions, get condition sets, get actions, and get rules calls required by the validate sizes operation.

        validateCategorySize ( model.getModelId (), "parameters", model.getParameters ().size (), diagnostics );
        validateCategorySize ( model.getModelId (), "payloads", model.getPayloads ().size (), diagnostics );
        validateCategorySize ( model.getModelId (), "events", model.getEvents ().size (), diagnostics );
        validateCategorySize ( model.getModelId (), "conditions", model.getConditions ().size (), diagnostics );
        validateCategorySize ( model.getModelId (), "conditionSets", model.getConditionSets ().size (), diagnostics );
        validateCategorySize ( model.getModelId (), "actions", model.getActions ().size (), diagnostics );
        validateCategorySize ( model.getModelId (), "rules", model.getRules ().size (), diagnostics );

        // Process each condition set supplied by model condition sets.

        for ( AuthoringModel.ConditionSetDefinition conditionSet : model.getConditionSets () )
        {
            // Initialize the condition count by applying uses predefined conditions, size, get condition IDs, and get
            // bindings.

            int conditionCount = conditionSet.usesPredefinedConditions ()
                ? conditionSet.getConditionIds ().size ()
                : conditionSet.getBindings ().size ();

            // Handle the branch where condition count exceeds limits maximum bindings per condition set.

            if ( conditionCount > this.limits.getMaximumBindingsPerConditionSet () )
            {
                // Perform the add diagnostic, get ID, uses predefined conditions, and get maximum bindings per
                // condition set calls required by the validate sizes operation.

                addDiagnostic (
                    diagnostics,
                    conditionSet.getId (),
                    conditionSet.usesPredefinedConditions () ? "conditionIds" : "bindings",
                    SIZE_LIMIT_EXCEEDED,
                    "Reduce the binding count to "
                        + this.limits.getMaximumBindingsPerConditionSet ()
                        + " or fewer."
                );
            }
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validateCategorySize
    //
    // Description:
    //
    //   Performs the validate category size operation.
    //
    // Arguments:
    //
    //   modelId (String):
    //     The model id to use.
    //
    //   field (String):
    //     The field to use.
    //
    //   size (int):
    //     The size to use.
    //
    //   diagnostics (Collection <ValidationDiagnostic>):
    //     The diagnostics to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void validateCategorySize (
        String modelId,
        String field,
        int size,
        Collection <ValidationDiagnostic> diagnostics
    )
    {
        // Handle the branch where size exceeds limits maximum entities per category.

        if ( size > this.limits.getMaximumEntitiesPerCategory () )
        {
            // Apply add diagnostic and get maximum entities per category to the limits for the validate category size
            // operation.

            addDiagnostic (
                diagnostics,
                modelId,
                field,
                SIZE_LIMIT_EXCEEDED,
                "Reduce the category to "
                    + this.limits.getMaximumEntitiesPerCategory ()
                    + " entities or fewer."
            );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: indexEntities
    //
    // Description:
    //
    //   Performs the index entities operation.
    //
    // Arguments:
    //
    //   entities (Collection <T>):
    //     The entities to use.
    //
    //   category (String):
    //     The category to use.
    //
    //   diagnostics (Collection <ValidationDiagnostic>):
    //     The diagnostics to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static <T extends AuthoringModel.EntityDefinition> Map <String, T> indexEntities (
        Collection <T> entities,
        String category,
        Collection <ValidationDiagnostic> diagnostics
    )
    {
        // Initialize the entities by ID with a new linked hash map.

        LinkedHashMap <String, T> entitiesById = new LinkedHashMap <String, T> ();

        // Process each entity supplied by entities.

        for ( T entity : entities )
        {
            // Apply validate identifier, get ID, validate text, get name, and get description to the entity for the
            // index entities operation.

            validateIdentifier ( entity.getId (), entity.getId (), "id", diagnostics );
            validateText ( entity.getId (), "name", entity.getName (), diagnostics );
            validateText ( entity.getId (), "description", entity.getDescription (), diagnostics );

            // Handle the branch where entities by ID put if absent entity ID entity is available.

            if ( entitiesById.putIfAbsent ( entity.getId (), entity ) != null )
            {
                // Apply add diagnostic and get ID to the entity for the index entities operation.

                addDiagnostic (
                    diagnostics,
                    entity.getId (),
                    "id",
                    DUPLICATE_IDENTIFIER,
                    "Choose a unique identifier within " + category + "."
                );
            }
        }

        // Return the entities by ID to the caller.

        return entitiesById;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validateParameters
    //
    // Description:
    //
    //   Performs the validate parameters operation.
    //
    // Arguments:
    //
    //   parameters (Collection <AuthoringModel.ParameterDefinition>):
    //     The parameters to use.
    //
    //   diagnostics (Collection <ValidationDiagnostic>):
    //     The diagnostics to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void validateParameters (
        Collection <AuthoringModel.ParameterDefinition> parameters,
        Collection <ValidationDiagnostic> diagnostics
    )
    {
        // Process each parameter supplied by parameters.

        for ( AuthoringModel.ParameterDefinition parameter : parameters )
        {
            // Initialize the parameter type by applying parse parameter type and get type.

            ParameterType parameterType = parseParameterType ( parameter.getType () );

            // Handle the branch where parameter type is unavailable.

            if ( parameterType == null )
            {
                // Apply add diagnostic and get ID to the parameter for the validate parameters operation.

                addDiagnostic (
                    diagnostics,
                    parameter.getId (),
                    "type",
                    INVALID_PARAMETER_TYPE,
                    "Use STRING, INTEGER, BOOLEAN, or ENUM."
                );

                continue;
            }

            // Initialize the encountered values with a new hash set.

            Set <String> encounteredValues = new HashSet <String> ();
            boolean validEnumerationValues = true;

            // Process each enumeration value supplied by parameter enumeration values.

            for ( String enumerationValue : parameter.getEnumerationValues () )
            {
                // Handle the branch where enumeration value trim contains no values or encountered values add does not
                // succeed.

                if ( enumerationValue.trim ().isEmpty () || !encounteredValues.add ( enumerationValue ) )
                {
                    validEnumerationValues = false;
                }
            }

            // Handle the branch where parameter type equals parameter type enum and parameter enumeration values
            // contains no values or valid enumeration values is false.

            if (
                parameterType == ParameterType.ENUM
                && ( parameter.getEnumerationValues ().isEmpty () || !validEnumerationValues )
            )
            {
                // Apply add diagnostic and get ID to the parameter for the validate parameters operation.

                addDiagnostic (
                    diagnostics,
                    parameter.getId (),
                    "enumValues",
                    INVALID_ENUM_DOMAIN,
                    "Provide at least one unique, nonblank enumeration value."
                );
            }

            // Handle the alternative where parameter type differs from parameter type enum and parameter enumeration
            // values contains values.

            else if ( parameterType != ParameterType.ENUM && !parameter.getEnumerationValues ().isEmpty () )
            {
                // Apply add diagnostic and get ID to the parameter for the validate parameters operation.

                addDiagnostic (
                    diagnostics,
                    parameter.getId (),
                    "enumValues",
                    INVALID_ENUM_DOMAIN,
                    "Remove enumValues from non-ENUM parameters."
                );
            }
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validatePayloads
    //
    // Description:
    //
    //   Performs the validate payloads operation.
    //
    // Arguments:
    //
    //   payloads (Collection <AuthoringModel.PayloadDefinition>):
    //     The payloads to use.
    //
    //   parameters (Map <String, AuthoringModel.ParameterDefinition>):
    //     The parameters to use.
    //
    //   diagnostics (Collection <ValidationDiagnostic>):
    //     The diagnostics to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void validatePayloads (
        Collection <AuthoringModel.PayloadDefinition> payloads,
        Map <String, AuthoringModel.ParameterDefinition> parameters,
        Collection <ValidationDiagnostic> diagnostics
    )
    {
        // Process each payload supplied by payloads.

        for ( AuthoringModel.PayloadDefinition payload : payloads )
        {
            // Initialize the encountered parameter IDs with a new hash set.

            Set <String> encounteredParameterIds = new HashSet <String> ();

            // Process each parameter ID supplied by payload parameter IDs.

            for ( String parameterId : payload.getParameterIds () )
            {
                // Handle the branch where encountered parameter IDs add does not succeed.

                if ( !encounteredParameterIds.add ( parameterId ) )
                {
                    // Apply add diagnostic and get ID to the payload for the validate payloads operation.

                    addDiagnostic (
                        diagnostics,
                        payload.getId (),
                        "parameterIds",
                        DUPLICATE_REFERENCE,
                        "List each permitted parameter once."
                    );
                }

                // Apply validate reference and get ID to the payload for the validate payloads operation.

                validateReference (
                    payload.getId (),
                    "parameterIds",
                    parameterId,
                    parameters,
                    "Define the referenced parameter or remove it from the payload.",
                    diagnostics
                );
            }
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validateEvents
    //
    // Description:
    //
    //   Performs the validate events operation.
    //
    // Arguments:
    //
    //   events (Collection <AuthoringModel.EventDefinition>):
    //     The events to use.
    //
    //   payloads (Map <String, AuthoringModel.PayloadDefinition>):
    //     The payloads to use.
    //
    //   diagnostics (Collection <ValidationDiagnostic>):
    //     The diagnostics to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void validateEvents (
        Collection <AuthoringModel.EventDefinition> events,
        Map <String, AuthoringModel.PayloadDefinition> payloads,
        Collection <ValidationDiagnostic> diagnostics
    )
    {
        // Process each event supplied by events.

        for ( AuthoringModel.EventDefinition event : events )
        {
            // Apply validate reference, get ID, and get payload ID to the event for the validate events operation.

            validateReference (
                event.getId (),
                "payloadId",
                event.getPayloadId (),
                payloads,
                "Reference a defined payload.",
                diagnostics
            );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validateConditions
    //
    // Description:
    //
    //   Performs the validate conditions operation.
    //
    // Arguments:
    //
    //   conditions (Collection <AuthoringModel.ConditionDefinition>):
    //     The conditions to use.
    //
    //   parameters (Map <String, AuthoringModel.ParameterDefinition>):
    //     The parameters to use.
    //
    //   diagnostics (Collection <ValidationDiagnostic>):
    //     The diagnostics to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void validateConditions (
        Collection <AuthoringModel.ConditionDefinition> conditions,
        Map <String, AuthoringModel.ParameterDefinition> parameters,
        Collection <ValidationDiagnostic> diagnostics
    )
    {
        // Process each condition supplied by conditions.

        for ( AuthoringModel.ConditionDefinition condition : conditions )
        {
            // Apply validate reference, get ID, and get parameter ID to the condition for the validate conditions
            // operation.

            validateReference (
                condition.getId (),
                "parameterId",
                condition.getParameterId (),
                parameters,
                "Reference a defined parameter.",
                diagnostics
            );

            // Initialize the parameter by applying get and get parameter ID.

            AuthoringModel.ParameterDefinition parameter = parameters.get ( condition.getParameterId () );

            // Skip the current item when condition has not predicate or parameter is unavailable.

            if ( !condition.hasPredicate () || parameter == null )
            {
                continue;
            }

            // Prepare the operator and parameter type values needed by the validate conditions operation.

            ConditionOperator operator = parseConditionOperator ( condition, diagnostics );
            ParameterType parameterType = parseParameterType ( parameter.getType () );

            // Skip the current item when operator is unavailable or parameter type is unavailable.

            if ( operator == null || parameterType == null )
            {
                continue;
            }

            // Handle the branch where operator supports does not succeed.

            if ( !operator.supports ( parameterType ) )
            {
                // Apply add diagnostic and get ID to the condition for the validate conditions operation.

                addDiagnostic (
                    diagnostics,
                    condition.getId (),
                    "operator",
                    INVALID_CONDITION_OPERATOR,
                    "Use equality for this type, or choose an INTEGER parameter for inequalities."
                );

                continue;
            }

            // Handle the branch where operator operand count is at least 1.

            if ( operator.getOperandCount () >= 1 )
            {
                // Apply validate concrete value, get ID, and get first value to the condition for the validate
                // conditions operation.

                validateConcreteValue (
                    condition.getId (),
                    "value",
                    condition.getFirstValue (),
                    parameter,
                    diagnostics
                );
            }

            // Handle the branch where operator operand count equals 2.

            if ( operator.getOperandCount () == 2 )
            {
                // Apply validate concrete value, get ID, get second value, and validate range order to the condition
                // for the validate conditions operation.

                validateConcreteValue (
                    condition.getId (),
                    "secondValue",
                    condition.getSecondValue (),
                    parameter,
                    diagnostics
                );

                validateRangeOrder ( condition, parameterType, diagnostics );
            }
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validateConditionSets
    //
    // Description:
    //
    //   Performs the validate condition sets operation.
    //
    // Arguments:
    //
    //   conditionSets (Collection <AuthoringModel.ConditionSetDefinition>):
    //     The condition sets to use.
    //
    //   conditions (Map <String, AuthoringModel.ConditionDefinition>):
    //     The conditions to use.
    //
    //   parameters (Map <String, AuthoringModel.ParameterDefinition>):
    //     The parameters to use.
    //
    //   diagnostics (Collection <ValidationDiagnostic>):
    //     The diagnostics to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void validateConditionSets (
        Collection <AuthoringModel.ConditionSetDefinition> conditionSets,
        Map <String, AuthoringModel.ConditionDefinition> conditions,
        Map <String, AuthoringModel.ParameterDefinition> parameters,
        Collection <ValidationDiagnostic> diagnostics
    )
    {
        // Process each condition set supplied by condition sets.

        for ( AuthoringModel.ConditionSetDefinition conditionSet : conditionSets )
        {
            // Initialize the encountered parameter IDs with a new hash set.

            Set <String> encounteredParameterIds = new HashSet <String> ();

            // Handle the branch where condition set uses predefined conditions succeeds.

            if ( conditionSet.usesPredefinedConditions () )
            {
                // Initialize the encountered condition IDs with a new hash set.

                Set <String> encounteredConditionIds = new HashSet <String> ();

                // Process each condition identifier supplied by condition set condition IDs.

                for ( String conditionIdentifier : conditionSet.getConditionIds () )
                {
                    // Initialize the condition by applying get.

                    AuthoringModel.ConditionDefinition condition = conditions.get ( conditionIdentifier );

                    // Handle the branch where encountered condition IDs add does not succeed.

                    if ( !encounteredConditionIds.add ( conditionIdentifier ) )
                    {
                        // Apply add diagnostic and get ID to the condition set for the validate condition sets
                        // operation.

                        addDiagnostic (
                            diagnostics,
                            conditionSet.getId (),
                            "conditionIds",
                            DUPLICATE_REFERENCE,
                            "Choose each predefined condition at most once."
                        );
                    }

                    // Handle the branch where condition is unavailable.

                    if ( condition == null )
                    {
                        // Apply add diagnostic and get ID to the condition set for the validate condition sets
                        // operation.

                        addDiagnostic (
                            diagnostics,
                            conditionSet.getId (),
                            "conditionIds",
                            UNRESOLVED_REFERENCE,
                            "Reference only defined conditions."
                        );

                        continue;
                    }

                    // Handle the branch where condition has not predicate.

                    if ( !condition.hasPredicate () )
                    {
                        // Apply add diagnostic and get ID to the condition set for the validate condition sets
                        // operation.

                        addDiagnostic (
                            diagnostics,
                            conditionSet.getId (),
                            "conditionIds",
                            INVALID_CONDITION_OPERATOR,
                            "Configure the selected condition's operator and value first."
                        );
                    }

                    // Complete the validate condition sets step by calling validate unique condition parameter.

                    validateUniqueConditionParameter (
                        conditionSet,
                        condition,
                        encounteredParameterIds,
                        "conditionIds",
                        diagnostics
                    );
                }

                continue;
            }

            // Process each binding supplied by condition set bindings entry set.

            for ( Map.Entry <String, Object> binding : conditionSet.getBindings ().entrySet () )
            {
                // Initialize the condition by applying get and get key.

                AuthoringModel.ConditionDefinition condition = conditions.get ( binding.getKey () );

                // Handle the branch where condition is unavailable.

                if ( condition == null )
                {
                    // Perform the add diagnostic, get ID, and get key calls required by the validate condition sets
                    // operation.

                    addDiagnostic (
                        diagnostics,
                        conditionSet.getId (),
                        "bindings." + binding.getKey (),
                        UNRESOLVED_REFERENCE,
                        "Reference a defined condition or remove the binding."
                    );

                    continue;
                }

                // Apply validate unique condition parameter and get key to the binding for the validate condition sets
                // operation.

                validateUniqueConditionParameter (
                    conditionSet,
                    condition,
                    encounteredParameterIds,
                    "bindings." + binding.getKey (),
                    diagnostics
                );

                // Initialize the parameter by applying get and get parameter ID.

                AuthoringModel.ParameterDefinition parameter = parameters.get ( condition.getParameterId () );

                // Handle the branch where parameter is available and binding value is available.

                if ( parameter != null && binding.getValue () != null )
                {
                    // Perform the validate concrete value, get ID, get key, and get value calls required by the
                    // validate condition sets operation.

                    validateConcreteValue (
                        conditionSet.getId (),
                        "bindings." + binding.getKey (),
                        binding.getValue (),
                        parameter,
                        diagnostics
                    );
                }
            }
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validateRules
    //
    // Description:
    //
    //   Performs the validate rules operation.
    //
    // Arguments:
    //
    //   rules (Collection <AuthoringModel.RuleDefinition>):
    //     The rules to use.
    //
    //   events (Map <String, AuthoringModel.EventDefinition>):
    //     The events to use.
    //
    //   payloads (Map <String, AuthoringModel.PayloadDefinition>):
    //     The payloads to use.
    //
    //   conditions (Map <String, AuthoringModel.ConditionDefinition>):
    //     The conditions to use.
    //
    //   conditionSets (Map <String, AuthoringModel.ConditionSetDefinition>):
    //     The condition sets to use.
    //
    //   actions (Map <String, AuthoringModel.ActionDefinition>):
    //     The actions to use.
    //
    //   diagnostics (Collection <ValidationDiagnostic>):
    //     The diagnostics to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void validateRules (
        Collection <AuthoringModel.RuleDefinition> rules,
        Map <String, AuthoringModel.EventDefinition> events,
        Map <String, AuthoringModel.PayloadDefinition> payloads,
        Map <String, AuthoringModel.ConditionDefinition> conditions,
        Map <String, AuthoringModel.ConditionSetDefinition> conditionSets,
        Map <String, AuthoringModel.ActionDefinition> actions,
        Collection <ValidationDiagnostic> diagnostics
    )
    {
        // Process each rule supplied by rules.

        for ( AuthoringModel.RuleDefinition rule : rules )
        {
            // Apply validate reference, get ID, get event ID, get condition set ID, and get action ID to the rule for
            // the validate rules operation.

            validateReference (
                rule.getId (),
                "eventId",
                rule.getEventId (),
                events,
                "Reference a defined event.",
                diagnostics
            );
            validateReference (
                rule.getId (),
                "conditionSetId",
                rule.getConditionSetId (),
                conditionSets,
                "Reference a defined condition set.",
                diagnostics
            );
            validateReference (
                rule.getId (),
                "actionId",
                rule.getActionId (),
                actions,
                "Reference a defined action.",
                diagnostics
            );

            // Prepare the event and condition set values needed by the validate rules operation.

            AuthoringModel.EventDefinition event               = events.get ( rule.getEventId () );
            AuthoringModel.ConditionSetDefinition conditionSet = conditionSets.get ( rule.getConditionSetId () );

            // Skip the current item when event is unavailable or condition set is unavailable.

            if ( event == null || conditionSet == null )
            {
                continue;
            }

            // Initialize the payload by applying get and get payload ID.

            AuthoringModel.PayloadDefinition payload = payloads.get ( event.getPayloadId () );

            // Skip the current item when payload is unavailable.

            if ( payload == null )
            {
                continue;
            }

            // Prepare the permitted parameter IDs and included condition IDs values needed by the validate rules
            // operation.

            Set <String> permittedParameterIds = new HashSet <String> ( payload.getParameterIds () );

            Collection <String> includedConditionIds = conditionSet.usesPredefinedConditions ()
                ? conditionSet.getConditionIds ()
                : conditionSet.getBindings ().keySet ();

            // Process each condition identifier supplied by included condition IDs.

            for ( String conditionIdentifier : includedConditionIds )
            {
                // Prepare the condition and concrete condition values needed by the validate rules operation.

                AuthoringModel.ConditionDefinition condition = conditions.get ( conditionIdentifier );
                boolean concreteCondition = conditionSet.usesPredefinedConditions ()
                    ? (
                        condition != null
                            && condition.hasPredicate ()
                            && !ConditionOperator.ANY.name ().equals ( condition.getOperator () )
                    )
                    : conditionSet.getBindings ().get ( conditionIdentifier ) != null;

                // Handle the branch where concrete condition is true and condition is available and permitted
                // parameter IDs does not contain condition parameter ID.

                if (
                    concreteCondition
                    && condition != null
                    && !permittedParameterIds.contains ( condition.getParameterId () )
                )
                {
                    // Apply add diagnostic and get ID to the rule for the validate rules operation.

                    addDiagnostic (
                        diagnostics,
                        rule.getId (),
                        "conditionSetId",
                        RULE_CONDITION_NOT_PERMITTED,
                        "Remove the concrete condition or permit its parameter in the event payload."
                    );
                }
            }
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: parseConditionOperator
    //
    // Description:
    //
    //   Performs the parse condition operator operation.
    //
    // Arguments:
    //
    //   condition (AuthoringModel.ConditionDefinition):
    //     The condition to use.
    //
    //   diagnostics (Collection <ValidationDiagnostic>):
    //     The diagnostics to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static ConditionOperator parseConditionOperator (
        AuthoringModel.ConditionDefinition condition,
        Collection <ValidationDiagnostic> diagnostics
    )
    {
        try
        {
            // Return the result produced by value of.

            return ConditionOperator.valueOf ( condition.getOperator () );
        }

        // Handle illegal argument or null pointer failures captured as exception.

        catch ( IllegalArgumentException | NullPointerException exception )
        {
            // Apply add diagnostic and get ID to the condition for the parse condition operator operation.

            addDiagnostic (
                diagnostics,
                condition.getId (),
                "operator",
                INVALID_CONDITION_OPERATOR,
                "Choose a supported condition operator."
            );

            // Return a null result to indicate that no value is available.

            return null;
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validateRangeOrder
    //
    // Description:
    //
    //   Performs the validate range order operation.
    //
    // Arguments:
    //
    //   condition (AuthoringModel.ConditionDefinition):
    //     The condition to use.
    //
    //   parameterType (ParameterType):
    //     The parameter type to use.
    //
    //   diagnostics (Collection <ValidationDiagnostic>):
    //     The diagnostics to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void validateRangeOrder (
        AuthoringModel.ConditionDefinition condition,
        ParameterType parameterType,
        Collection <ValidationDiagnostic> diagnostics
    )
    {
        // Stop this path and return its result when parameter type differs from parameter type integer or condition
        // first value is not a number or condition second value is not a number.

        if (
            parameterType != ParameterType.INTEGER
                || !( condition.getFirstValue () instanceof Number )
                || !( condition.getSecondValue () instanceof Number )
        )
        {
            return;
        }

        // Prepare the first value and second value values needed by the validate range order operation.

        BigDecimal firstValue  = new BigDecimal ( condition.getFirstValue ().toString () );
        BigDecimal secondValue = new BigDecimal ( condition.getSecondValue ().toString () );

        // Handle the branch where first value compare to second value is at least 0.

        if ( firstValue.compareTo ( secondValue ) >= 0 )
        {
            // Apply add diagnostic and get ID to the condition for the validate range order operation.

            addDiagnostic (
                diagnostics,
                condition.getId (),
                "secondValue",
                INVALID_CONDITION_RANGE,
                "Enter an upper value greater than the lower value."
            );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validateUniqueConditionParameter
    //
    // Description:
    //
    //   Performs the validate unique condition parameter operation.
    //
    // Arguments:
    //
    //   conditionSet (AuthoringModel.ConditionSetDefinition):
    //     The condition set to use.
    //
    //   condition (AuthoringModel.ConditionDefinition):
    //     The condition to use.
    //
    //   encounteredParameterIds (Set <String>):
    //     The encountered parameter ids to use.
    //
    //   field (String):
    //     The field to use.
    //
    //   diagnostics (Collection <ValidationDiagnostic>):
    //     The diagnostics to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void validateUniqueConditionParameter (
        AuthoringModel.ConditionSetDefinition conditionSet,
        AuthoringModel.ConditionDefinition condition,
        Set <String> encounteredParameterIds,
        String field,
        Collection <ValidationDiagnostic> diagnostics
    )
    {
        // Handle the branch where encountered parameter IDs add does not succeed.

        if ( !encounteredParameterIds.add ( condition.getParameterId () ) )
        {
            // Apply add diagnostic and get ID to the condition set for the validate unique condition parameter
            // operation.

            addDiagnostic (
                diagnostics,
                conditionSet.getId (),
                field,
                DUPLICATE_PARAMETER_BINDING,
                "Keep at most one included condition for each parameter."
            );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validateConcreteValue
    //
    // Description:
    //
    //   Performs the validate concrete value operation.
    //
    // Arguments:
    //
    //   entityId (String):
    //     The entity id to use.
    //
    //   field (String):
    //     The field to use.
    //
    //   value (Object):
    //     The value to use.
    //
    //   parameter (AuthoringModel.ParameterDefinition):
    //     The parameter to use.
    //
    //   diagnostics (Collection <ValidationDiagnostic>):
    //     The diagnostics to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void validateConcreteValue (
        String entityId,
        String field,
        Object value,
        AuthoringModel.ParameterDefinition parameter,
        Collection <ValidationDiagnostic> diagnostics
    )
    {
        // Initialize the parameter type by applying parse parameter type and get type.

        ParameterType parameterType = parseParameterType ( parameter.getType () );

        // Stop this path and return its result when parameter type is unavailable.

        if ( parameterType == null )
        {
            return;
        }

        // Select the processing branch for parameter type.

        switch ( parameterType )
        {
            // Handle string through this switch branch.

            case STRING:

                // Handle the branch where value is not a string.

                if ( !( value instanceof String ) )
                {
                    // Record a validation diagnostic for the rejected value.

                    addInvalidConcreteValue ( entityId, field, "Use a JSON string.", diagnostics );
                }
                break;

            // Handle boolean through this switch branch.

            case BOOLEAN:

                // Handle the branch where value is not a boolean.

                if ( !( value instanceof Boolean ) )
                {
                    // Record a validation diagnostic for the rejected value.

                    addInvalidConcreteValue ( entityId, field, "Use true or false.", diagnostics );
                }
                break;

            // Handle integer through this switch branch.

            case INTEGER:
                validateIntegerValue ( entityId, field, value, diagnostics );
                break;

            // Handle enum through this switch branch.

            case ENUM:

                // Handle the branch where value is not a string or parameter enumeration values does not contain
                // value.

                if ( !( value instanceof String ) || !parameter.getEnumerationValues ().contains ( value ) )
                {
                    // Record a validation diagnostic for the rejected value.

                    addDiagnostic (
                        diagnostics,
                        entityId,
                        field,
                        ENUM_VALUE_OUT_OF_DOMAIN,
                        "Use one of the parameter's declared enumValues."
                    );
                }
                break;

            // Handle the default case through this switch branch.

            default:
                throw new IllegalStateException ( "Unsupported parameter type: " + parameterType );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validateIntegerValue
    //
    // Description:
    //
    //   Performs the validate integer value operation.
    //
    // Arguments:
    //
    //   entityId (String):
    //     The entity id to use.
    //
    //   field (String):
    //     The field to use.
    //
    //   value (Object):
    //     The value to use.
    //
    //   diagnostics (Collection <ValidationDiagnostic>):
    //     The diagnostics to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void validateIntegerValue (
        String entityId,
        String field,
        Object value,
        Collection <ValidationDiagnostic> diagnostics
    )
    {
        boolean isIntegralAndInRange = value instanceof Byte
            || value instanceof Short
            || value instanceof Integer
            || value instanceof Long;

        // Handle the branch where value is a big integer.

        if ( value instanceof BigInteger )
        {
            BigInteger integerValue = (BigInteger) value;

            // Update the is integral and in range from the value of result.

            isIntegralAndInRange = integerValue.compareTo ( BigInteger.valueOf ( Long.MIN_VALUE ) ) >= 0
                && integerValue.compareTo ( BigInteger.valueOf ( Long.MAX_VALUE ) ) <= 0;
        }

        // Handle the alternative where value is a big decimal.

        else if ( value instanceof BigDecimal )
        {
            try
            {
                // Verify that the numeric value has an exact integral representation.

                ( (BigDecimal) value ).longValueExact ();
                isIntegralAndInRange = true;
            }

            // Handle arithmetic failures captured as exception.

            catch ( ArithmeticException exception )
            {
                isIntegralAndInRange = false;
            }
        }

        // Handle the branch where is integral and in range is false.

        if ( !isIntegralAndInRange )
        {
            // Record a validation diagnostic for the rejected value.

            addDiagnostic (
                diagnostics,
                entityId,
                field,
                NON_INTEGRAL_INTEGER,
                "Use an integral JSON number within the signed 64-bit range."
            );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validateIdentifier
    //
    // Description:
    //
    //   Performs the validate identifier operation.
    //
    // Arguments:
    //
    //   entityId (String):
    //     The entity id to use.
    //
    //   value (String):
    //     The value to use.
    //
    //   field (String):
    //     The field to use.
    //
    //   diagnostics (Collection <ValidationDiagnostic>):
    //     The diagnostics to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void validateIdentifier (
        String entityId,
        String value,
        String field,
        Collection <ValidationDiagnostic> diagnostics
    )
    {
        // Handle the branch where identifier pattern matcher value does not match the supplied values.

        if ( !IDENTIFIER_PATTERN.matcher ( value ).matches () )
        {
            // Record a validation diagnostic for the rejected value.

            addDiagnostic (
                diagnostics,
                entityId,
                field,
                INVALID_IDENTIFIER,
                "Use lowercase kebab case beginning with a letter."
            );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validateText
    //
    // Description:
    //
    //   Performs the validate text operation.
    //
    // Arguments:
    //
    //   entityId (String):
    //     The entity id to use.
    //
    //   field (String):
    //     The field to use.
    //
    //   value (String):
    //     The value to use.
    //
    //   diagnostics (Collection <ValidationDiagnostic>):
    //     The diagnostics to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void validateText (
        String entityId,
        String field,
        String value,
        Collection <ValidationDiagnostic> diagnostics
    )
    {
        // Handle the branch where value trim contains no values.

        if ( value.trim ().isEmpty () )
        {
            // Record a validation diagnostic for the rejected value.

            addDiagnostic (
                diagnostics,
                entityId,
                field,
                BLANK_FIELD,
                "Provide nonblank text."
            );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validateReference
    //
    // Description:
    //
    //   Performs the validate reference operation.
    //
    // Arguments:
    //
    //   entityId (String):
    //     The entity id to use.
    //
    //   field (String):
    //     The field to use.
    //
    //   reference (String):
    //     The reference to use.
    //
    //   targets (Map <String, ?>):
    //     The targets to use.
    //
    //   remedy (String):
    //     The remedy to use.
    //
    //   diagnostics (Collection <ValidationDiagnostic>):
    //     The diagnostics to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void validateReference (
        String entityId,
        String field,
        String reference,
        Map <String, ?> targets,
        String remedy,
        Collection <ValidationDiagnostic> diagnostics
    )
    {
        // Handle the branch where targets contains key does not succeed.

        if ( !targets.containsKey ( reference ) )
        {
            // Record a validation diagnostic for the rejected value.

            addDiagnostic ( diagnostics, entityId, field, UNRESOLVED_REFERENCE, remedy );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: parseParameterType
    //
    // Description:
    //
    //   Performs the parse parameter type operation.
    //
    // Arguments:
    //
    //   value (String):
    //     The value to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static ParameterType parseParameterType ( String value )
    {
        try
        {
            // Return the result produced by value of.

            return ParameterType.valueOf ( value );
        }

        // Handle illegal argument failures captured as exception.

        catch ( IllegalArgumentException exception )
        {
            // Return a null result to indicate that no value is available.

            return null;
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: addInvalidConcreteValue
    //
    // Description:
    //
    //   Performs the add invalid concrete value operation.
    //
    // Arguments:
    //
    //   entityId (String):
    //     The entity id to use.
    //
    //   field (String):
    //     The field to use.
    //
    //   remedy (String):
    //     The remedy to use.
    //
    //   diagnostics (Collection <ValidationDiagnostic>):
    //     The diagnostics to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void addInvalidConcreteValue (
        String entityId,
        String field,
        String remedy,
        Collection <ValidationDiagnostic> diagnostics
    )
    {
        // Record a validation diagnostic for the rejected value.

        addDiagnostic ( diagnostics, entityId, field, INVALID_CONCRETE_VALUE, remedy );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: addDiagnostic
    //
    // Description:
    //
    //   Performs the add diagnostic operation.
    //
    // Arguments:
    //
    //   diagnostics (Collection <ValidationDiagnostic>):
    //     The diagnostics to use.
    //
    //   entityId (String):
    //     The entity id to use.
    //
    //   field (String):
    //     The field to use.
    //
    //   code (String):
    //     The code to use.
    //
    //   remedy (String):
    //     The remedy to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void addDiagnostic (
        Collection <ValidationDiagnostic> diagnostics,
        String entityId,
        String field,
        String code,
        String remedy
    )
    {
        // Add new validation diagnostic entity ID field code validation severity error rem to the diagnostics.

        diagnostics.add (
            new ValidationDiagnostic (
                entityId,
                field,
                code,
                ValidationSeverity.ERROR,
                remedy
            )
        );
    }
}
