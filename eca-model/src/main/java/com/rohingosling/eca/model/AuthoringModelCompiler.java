//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Compiles a validated authoring model into the immutable mathematical-core types consumed by the rule engine.
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.rohingosling.eca.domain.ActionId;
import com.rohingosling.eca.domain.CompiledConditionSet;
import com.rohingosling.eca.domain.CompiledRule;
import com.rohingosling.eca.domain.CompiledRuleBase;
import com.rohingosling.eca.domain.ComparisonValue;
import com.rohingosling.eca.domain.ConditionOperator;
import com.rohingosling.eca.domain.ConditionValue;
import com.rohingosling.eca.domain.ConcreteValue;
import com.rohingosling.eca.domain.EventId;
import com.rohingosling.eca.domain.ExplicitWildcard;
import com.rohingosling.eca.domain.ParameterDefinition;
import com.rohingosling.eca.domain.ParameterId;
import com.rohingosling.eca.domain.ParameterType;
import com.rohingosling.eca.domain.RuleId;

//*********************************************************************************************************************
// Class: AuthoringModelCompiler
//
// Description:
//
//   Compiles a validated authoring model into the immutable mathematical-core types consumed by the rule engine.
//
//*********************************************************************************************************************

public final class AuthoringModelCompiler
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final ModelValidator validator;

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/2: AuthoringModelCompiler
    //
    // Description:
    //
    //   Creates the AuthoringModelCompiler instance from the supplied values.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public AuthoringModelCompiler ()
    {
        // Delegate initialization to the primary authoring model compiler constructor.

        this ( new ModelValidator () );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 2/2: AuthoringModelCompiler
    //
    // Description:
    //
    //   Creates the AuthoringModelCompiler instance from the supplied values.
    //
    // Arguments:
    //
    //   validator (ModelValidator):
    //     The validator to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public AuthoringModelCompiler ( ModelValidator validator )
    {
        // Validate the required validator before continuing.

        this.validator = Objects.requireNonNull ( validator, "validator" );
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: compile
    //
    // Description:
    //
    //   Performs the compile operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   revision (String):
    //     The revision to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public CompiledModel compile ( AuthoringModel model, String revision )
    {
        // Validate the required model and revision before continuing.

        Objects.requireNonNull ( model, "model" );
        Objects.requireNonNull ( revision, "revision" );

        // Initialize the diagnostics by applying validate.

        List <ValidationDiagnostic> diagnostics = this.validator.validate ( model );

        // Reject the operation when diagnostics contains values.

        if ( !diagnostics.isEmpty () )
        {
            throw new ModelValidationException ( diagnostics );
        }

        // Prepare the parameters, payloads, events, conditions, condition sets, actions, and rule base values needed
        // by the compile operation.

        Map <String, ParameterDefinition> parameters = compileParameters ( model );
        Map <String, AuthoringModel.PayloadDefinition> payloads = indexPayloads ( model );
        Map <String, com.rohingosling.eca.domain.EventDefinition> events =
            compileEvents ( model, payloads, parameters );
        Map <String, AuthoringModel.ConditionDefinition> conditions = indexConditions ( model );
        Map <String, CompiledConditionSet> conditionSets =
            compileConditionSets ( model, conditions, parameters );
        Map <String, ActionId> actions = compileActions ( model );
        CompiledRuleBase ruleBase      = compileRules ( model, events, conditionSets, actions );

        // Return a newly constructed compiled model containing the operation result.

        return new CompiledModel (
            model,
            revision,
            parameters,
            events,
            conditionSets,
            actions,
            ruleBase
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: compileParameters
    //
    // Description:
    //
    //   Performs the compile parameters operation.
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

    private static Map <String, ParameterDefinition> compileParameters ( AuthoringModel model )
    {
        // Initialize the parameters with a new linked hash map.

        LinkedHashMap <String, ParameterDefinition> parameters = new LinkedHashMap <String, ParameterDefinition> ();

        // Process each parameter supplied by model parameters.

        for ( AuthoringModel.ParameterDefinition parameter : model.getParameters () )
        {
            // Store new parameter definition new parameter ID parameter ID parameter type value of under parameter ID
            // in the parameters.

            parameters.put (
                parameter.getId (),
                new ParameterDefinition (
                    new ParameterId ( parameter.getId () ),
                    ParameterType.valueOf ( parameter.getType () ),
                    parameter.getEnumerationValues ()
                )
            );
        }

        // Return the parameters to the caller.

        return parameters;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: indexPayloads
    //
    // Description:
    //
    //   Performs the index payloads operation.
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

    private static Map <String, AuthoringModel.PayloadDefinition> indexPayloads ( AuthoringModel model )
    {
        // Initialize the payloads with a new linked hash map.

        LinkedHashMap <String, AuthoringModel.PayloadDefinition> payloads =
            new LinkedHashMap <String, AuthoringModel.PayloadDefinition> ();

        // Process each payload supplied by model payloads.

        for ( AuthoringModel.PayloadDefinition payload : model.getPayloads () )
        {
            // Store payload under payload ID in the payloads.

            payloads.put ( payload.getId (), payload );
        }

        // Return the payloads to the caller.

        return payloads;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: compileEvents
    //
    // Description:
    //
    //   Performs the compile events operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   payloads (Map <String, AuthoringModel.PayloadDefinition>):
    //     The payloads to use.
    //
    //   parameters (Map <String, ParameterDefinition>):
    //     The parameters to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Map <String, com.rohingosling.eca.domain.EventDefinition> compileEvents (
        AuthoringModel model,
        Map <String, AuthoringModel.PayloadDefinition> payloads,
        Map <String, ParameterDefinition> parameters
    )
    {
        // Initialize the events with a new linked hash map.

        LinkedHashMap <String, com.rohingosling.eca.domain.EventDefinition> events =
            new LinkedHashMap <String, com.rohingosling.eca.domain.EventDefinition> ();

        // Process each event supplied by model events.

        for ( AuthoringModel.EventDefinition event : model.getEvents () )
        {
            // Prepare the payload and permitted parameters values needed by the compile events operation.

            AuthoringModel.PayloadDefinition payload = payloads.get ( event.getPayloadId () );
            ArrayList <ParameterDefinition> permittedParameters = new ArrayList <ParameterDefinition> ();

            // Process each parameter ID supplied by payload parameter IDs.

            for ( String parameterId : payload.getParameterIds () )
            {
                // Add parameters get parameter ID to the permitted parameters.

                permittedParameters.add ( parameters.get ( parameterId ) );
            }

            // Store new com rohingosling ECA domain event definition new event ID event ID permit under event ID in
            // the events.

            events.put (
                event.getId (),
                new com.rohingosling.eca.domain.EventDefinition (
                    new EventId ( event.getId () ),
                    permittedParameters
                )
            );
        }

        // Return the events to the caller.

        return events;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: indexConditions
    //
    // Description:
    //
    //   Performs the index conditions operation.
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

    private static Map <String, AuthoringModel.ConditionDefinition> indexConditions ( AuthoringModel model )
    {
        // Initialize the conditions with a new linked hash map.

        LinkedHashMap <String, AuthoringModel.ConditionDefinition> conditions =
            new LinkedHashMap <String, AuthoringModel.ConditionDefinition> ();

        // Process each condition supplied by model conditions.

        for ( AuthoringModel.ConditionDefinition condition : model.getConditions () )
        {
            // Store condition under condition ID in the conditions.

            conditions.put ( condition.getId (), condition );
        }

        // Return the conditions to the caller.

        return conditions;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: compileConditionSets
    //
    // Description:
    //
    //   Performs the compile condition sets operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   conditions (Map <String, AuthoringModel.ConditionDefinition>):
    //     The conditions to use.
    //
    //   parameters (Map <String, ParameterDefinition>):
    //     The parameters to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Map <String, CompiledConditionSet> compileConditionSets (
        AuthoringModel model,
        Map <String, AuthoringModel.ConditionDefinition> conditions,
        Map <String, ParameterDefinition> parameters
    )
    {
        // Initialize the condition sets with a new linked hash map.

        LinkedHashMap <String, CompiledConditionSet> conditionSets =
            new LinkedHashMap <String, CompiledConditionSet> ();

        // Process each condition set supplied by model condition sets.

        for ( AuthoringModel.ConditionSetDefinition conditionSet : model.getConditionSets () )
        {
            // Initialize the bindings with a new linked hash map.

            LinkedHashMap <ParameterId, ConditionValue> bindings =
                new LinkedHashMap <ParameterId, ConditionValue> ();

            // Handle the branch where condition set uses predefined conditions succeeds.

            if ( conditionSet.usesPredefinedConditions () )
            {
                // Process each condition identifier supplied by condition set condition IDs.

                for ( String conditionIdentifier : conditionSet.getConditionIds () )
                {
                    // Prepare the condition, parameter, and operator values needed by the compile condition sets
                    // operation.

                    AuthoringModel.ConditionDefinition condition = conditions.get ( conditionIdentifier );
                    ParameterDefinition parameter = parameters.get ( condition.getParameterId () );
                    ConditionOperator operator = ConditionOperator.valueOf ( condition.getOperator () );
                    ConditionValue conditionValue;

                    // Handle the branch where operator equals condition operator any.

                    if ( operator == ConditionOperator.ANY )
                    {
                        // Construct the explicit wildcard instance required by the compile condition sets operation.

                        conditionValue = new ExplicitWildcard ( parameter );
                    }

                    // Handle the alternative path when the preceding condition is not satisfied.

                    else
                    {
                        // Update the condition value from the get second value result.

                        conditionValue = new ComparisonValue (
                            parameter,
                            operator,
                            normalizeConcreteValue ( parameter, condition.getFirstValue () ),
                            operator.getOperandCount () == 2
                                ? normalizeConcreteValue ( parameter, condition.getSecondValue () )
                                : null
                        );
                    }

                    // Store condition value under parameter ID in the bindings.

                    bindings.put ( parameter.getParameterId (), conditionValue );
                }
            }

            // Handle the alternative path when the preceding condition is not satisfied.

            else
            {
                // Process each binding supplied by condition set bindings entry set.

                for ( Map.Entry <String, Object> binding : conditionSet.getBindings ().entrySet () )
                {
                    // Prepare the condition and parameter values needed by the compile condition sets operation.

                    AuthoringModel.ConditionDefinition condition = conditions.get ( binding.getKey () );
                    ParameterDefinition parameter = parameters.get ( condition.getParameterId () );
                    ConditionValue conditionValue;

                    // Handle the branch where binding value is unavailable.

                    if ( binding.getValue () == null )
                    {
                        // Construct the explicit wildcard instance required by the compile condition sets operation.

                        conditionValue = new ExplicitWildcard ( parameter );
                    }

                    // Handle the alternative path when the preceding condition is not satisfied.

                    else
                    {
                        // Update the condition value from the get value result.

                        conditionValue = new ConcreteValue (
                            parameter,
                            normalizeConcreteValue ( parameter, binding.getValue () )
                        );
                    }

                    // Store condition value under parameter ID in the bindings.

                    bindings.put ( parameter.getParameterId (), conditionValue );
                }
            }

            // Store new compiled condition set bindings under condition set ID in the condition sets.

            conditionSets.put ( conditionSet.getId (), new CompiledConditionSet ( bindings ) );
        }

        // Return the condition sets to the caller.

        return conditionSets;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: compileActions
    //
    // Description:
    //
    //   Performs the compile actions operation.
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

    private static Map <String, ActionId> compileActions ( AuthoringModel model )
    {
        // Initialize the actions with a new linked hash map.

        LinkedHashMap <String, ActionId> actions = new LinkedHashMap <String, ActionId> ();

        // Process each action supplied by model actions.

        for ( AuthoringModel.ActionDefinition action : model.getActions () )
        {
            // Store new action ID action ID under action ID in the actions.

            actions.put ( action.getId (), new ActionId ( action.getId () ) );
        }

        // Return the actions to the caller.

        return actions;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: compileRules
    //
    // Description:
    //
    //   Performs the compile rules operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   events (Map <String, com.rohingosling.eca.domain.EventDefinition>):
    //     The events to use.
    //
    //   conditionSets (Map <String, CompiledConditionSet>):
    //     The condition sets to use.
    //
    //   actions (Map <String, ActionId>):
    //     The actions to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static CompiledRuleBase compileRules (
        AuthoringModel model,
        Map <String, com.rohingosling.eca.domain.EventDefinition> events,
        Map <String, CompiledConditionSet> conditionSets,
        Map <String, ActionId> actions
    )
    {
        // Initialize the rules with a new array list.

        ArrayList <CompiledRule> rules = new ArrayList <CompiledRule> ();

        // Process each rule supplied by model rules.

        for ( AuthoringModel.RuleDefinition rule : model.getRules () )
        {
            // Add new compiled rule new rule ID rule ID events get rule event ID condition sets to the rules.

            rules.add (
                new CompiledRule (
                    new RuleId ( rule.getId () ),
                    events.get ( rule.getEventId () ),
                    conditionSets.get ( rule.getConditionSetId () ),
                    actions.get ( rule.getActionId () )
                )
            );
        }

        // Return a newly constructed compiled rule base containing the operation result.

        return new CompiledRuleBase ( rules );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: normalizeConcreteValue
    //
    // Description:
    //
    //   Performs the normalize concrete value operation.
    //
    // Arguments:
    //
    //   parameter (ParameterDefinition):
    //     The parameter to use.
    //
    //   value (Object):
    //     The value to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Object normalizeConcreteValue ( ParameterDefinition parameter, Object value )
    {
        // Stop this path and return its result when parameter type differs from parameter type integer.

        if ( parameter.getParameterType () != ParameterType.INTEGER )
        {
            // Return the value to the caller when parameter type differs from parameter type integer.

            return value;
        }

        // Stop this path and return its result when value is a big decimal.

        if ( value instanceof BigDecimal )
        {
            // Return the result produced by long value exact when value is a big decimal.

            return ( (BigDecimal) value ).longValueExact ();
        }

        // Stop this path and return its result when value is a big integer.

        if ( value instanceof BigInteger )
        {
            // Return the result produced by long value exact when value is a big integer.

            return ( (BigInteger) value ).longValueExact ();
        }

        // Return the result produced by long value.

        return ( (Number) value ).longValue ();
    }
}
