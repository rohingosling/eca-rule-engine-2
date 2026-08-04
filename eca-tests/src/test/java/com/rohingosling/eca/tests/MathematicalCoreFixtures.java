//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Provides compact, invariant-preserving constructors for the mathematical-core tests.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.tests;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.rohingosling.eca.domain.ActionId;
import com.rohingosling.eca.domain.CompiledConditionSet;
import com.rohingosling.eca.domain.CompiledRule;
import com.rohingosling.eca.domain.ConditionValue;
import com.rohingosling.eca.domain.ConcreteValue;
import com.rohingosling.eca.domain.EventDefinition;
import com.rohingosling.eca.domain.EventId;
import com.rohingosling.eca.domain.ExplicitWildcard;
import com.rohingosling.eca.domain.ParameterDefinition;
import com.rohingosling.eca.domain.ParameterId;
import com.rohingosling.eca.domain.ParameterType;
import com.rohingosling.eca.domain.Payload;
import com.rohingosling.eca.domain.PayloadValue;
import com.rohingosling.eca.domain.PresentNullPayloadValue;
import com.rohingosling.eca.domain.RuleId;

//*********************************************************************************************************************
// Class: MathematicalCoreFixtures
//
// Description:
//
//   Provides compact, invariant-preserving constructors for the mathematical-core tests.
//
//*********************************************************************************************************************

final class MathematicalCoreFixtures
{
    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: MathematicalCoreFixtures
    //
    // Description:
    //
    //   Creates the MathematicalCoreFixtures instance from the supplied values.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private MathematicalCoreFixtures ()
    {
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: stringParameter
    //
    // Description:
    //
    //   Performs the string parameter operation.
    //
    // Arguments:
    //
    //   identifier (String):
    //     The identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    static ParameterDefinition stringParameter ( String identifier )
    {
        // Return a newly constructed parameter definition containing the operation result.

        return new ParameterDefinition (
            new ParameterId ( identifier ),
            ParameterType.STRING,
            Collections.emptyList ()
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: integerParameter
    //
    // Description:
    //
    //   Performs the integer parameter operation.
    //
    // Arguments:
    //
    //   identifier (String):
    //     The identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    static ParameterDefinition integerParameter ( String identifier )
    {
        // Return a newly constructed parameter definition containing the operation result.

        return new ParameterDefinition (
            new ParameterId ( identifier ),
            ParameterType.INTEGER,
            Collections.emptyList ()
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: enumerationParameter
    //
    // Description:
    //
    //   Performs the enumeration parameter operation.
    //
    // Arguments:
    //
    //   identifier (String):
    //     The identifier to use.
    //
    //   enumerationValues (String...):
    //     The enumeration values to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    static ParameterDefinition enumerationParameter ( String identifier, String... enumerationValues )
    {
        // Return a newly constructed parameter definition containing the operation result.

        return new ParameterDefinition (
            new ParameterId ( identifier ),
            ParameterType.ENUM,
            Arrays.asList ( enumerationValues )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: event
    //
    // Description:
    //
    //   Performs the event operation.
    //
    // Arguments:
    //
    //   identifier (String):
    //     The identifier to use.
    //
    //   parameterDefinitions (ParameterDefinition...):
    //     The parameter definitions to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    static EventDefinition event ( String identifier, ParameterDefinition... parameterDefinitions )
    {
        // Return a newly constructed event definition containing the operation result.

        return new EventDefinition (
            new EventId ( identifier ),
            Arrays.asList ( parameterDefinitions )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: concrete
    //
    // Description:
    //
    //   Performs the concrete operation.
    //
    // Arguments:
    //
    //   parameterDefinition (ParameterDefinition):
    //     The parameter definition to use.
    //
    //   value (Object):
    //     The value to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    static ConcreteValue concrete ( ParameterDefinition parameterDefinition, Object value )
    {
        // Return a newly constructed concrete value containing the operation result.

        return new ConcreteValue ( parameterDefinition, value );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: presentNull
    //
    // Description:
    //
    //   Performs the present null operation.
    //
    // Arguments:
    //
    //   parameterDefinition (ParameterDefinition):
    //     The parameter definition to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    static PresentNullPayloadValue presentNull ( ParameterDefinition parameterDefinition )
    {
        // Return a newly constructed present null payload value containing the operation result.

        return new PresentNullPayloadValue ( parameterDefinition );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: wildcard
    //
    // Description:
    //
    //   Performs the wildcard operation.
    //
    // Arguments:
    //
    //   parameterDefinition (ParameterDefinition):
    //     The parameter definition to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    static ExplicitWildcard wildcard ( ParameterDefinition parameterDefinition )
    {
        // Return a newly constructed explicit wildcard containing the operation result.

        return new ExplicitWildcard ( parameterDefinition );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: payload
    //
    // Description:
    //
    //   Performs the payload operation.
    //
    // Arguments:
    //
    //   payloadValues (PayloadValue...):
    //     The payload values to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    static Payload payload ( PayloadValue... payloadValues )
    {
        // Initialize the bindings with a new linked hash map.

        LinkedHashMap <ParameterId, PayloadValue> bindings = new LinkedHashMap <ParameterId, PayloadValue> ();

        // Process each payload value supplied by payload values.

        for ( PayloadValue payloadValue : payloadValues )
        {
            // Store payload value under payload value parameter definition parameter ID in the bindings.

            bindings.put ( payloadValue.getParameterDefinition ().getParameterId (), payloadValue );
        }

        // Return a newly constructed payload containing the operation result.

        return new Payload ( bindings );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: conditions
    //
    // Description:
    //
    //   Performs the conditions operation.
    //
    // Arguments:
    //
    //   conditionValues (ConditionValue...):
    //     The condition values to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    static CompiledConditionSet conditions ( ConditionValue... conditionValues )
    {
        // Initialize the bindings with a new linked hash map.

        LinkedHashMap <ParameterId, ConditionValue> bindings = new LinkedHashMap <ParameterId, ConditionValue> ();

        // Process each condition value supplied by condition values.

        for ( ConditionValue conditionValue : conditionValues )
        {
            // Store condition value under condition value parameter definition parameter ID in the bindings.

            bindings.put ( conditionValue.getParameterDefinition ().getParameterId (), conditionValue );
        }

        // Return a newly constructed compiled condition set containing the operation result.

        return new CompiledConditionSet ( bindings );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: payloadBindings
    //
    // Description:
    //
    //   Performs the payload bindings operation.
    //
    // Arguments:
    //
    //   payloadValues (PayloadValue...):
    //     The payload values to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    static Map <ParameterId, PayloadValue> payloadBindings ( PayloadValue... payloadValues )
    {
        // Initialize the bindings with a new linked hash map.

        LinkedHashMap <ParameterId, PayloadValue> bindings = new LinkedHashMap <ParameterId, PayloadValue> ();

        // Process each payload value supplied by payload values.

        for ( PayloadValue payloadValue : payloadValues )
        {
            // Store payload value under payload value parameter definition parameter ID in the bindings.

            bindings.put ( payloadValue.getParameterDefinition ().getParameterId (), payloadValue );
        }

        // Return the bindings to the caller.

        return bindings;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: conditionBindings
    //
    // Description:
    //
    //   Performs the condition bindings operation.
    //
    // Arguments:
    //
    //   conditionValues (ConditionValue...):
    //     The condition values to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    static Map <ParameterId, ConditionValue> conditionBindings ( ConditionValue... conditionValues )
    {
        // Initialize the bindings with a new linked hash map.

        LinkedHashMap <ParameterId, ConditionValue> bindings = new LinkedHashMap <ParameterId, ConditionValue> ();

        // Process each condition value supplied by condition values.

        for ( ConditionValue conditionValue : conditionValues )
        {
            // Store condition value under condition value parameter definition parameter ID in the bindings.

            bindings.put ( conditionValue.getParameterDefinition ().getParameterId (), conditionValue );
        }

        // Return the bindings to the caller.

        return bindings;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: rule
    //
    // Description:
    //
    //   Performs the rule operation.
    //
    // Arguments:
    //
    //   ruleIdentifier (String):
    //     The rule identifier to use.
    //
    //   eventDefinition (EventDefinition):
    //     The event definition to use.
    //
    //   conditionSet (CompiledConditionSet):
    //     The condition set to use.
    //
    //   actionIdentifier (String):
    //     The action identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    static CompiledRule rule (
        String ruleIdentifier,
        EventDefinition eventDefinition,
        CompiledConditionSet conditionSet,
        String actionIdentifier
    )
    {
        // Return a newly constructed compiled rule containing the operation result.

        return new CompiledRule (
            new RuleId ( ruleIdentifier ),
            eventDefinition,
            conditionSet,
            new ActionId ( actionIdentifier )
        );
    }
}
