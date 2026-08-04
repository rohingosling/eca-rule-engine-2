//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2026-08-02
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies the shared Java and TypeScript Phase 14 authoring-model and occurrence contract fixtures.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.rohingosling.eca.json.AuthoringModelJsonCodec;
import com.rohingosling.eca.json.EventOccurrenceJsonCodec;
import com.rohingosling.eca.model.AuthoringModel;
import com.rohingosling.eca.model.ModelValidator;
import com.rohingosling.eca.model.ValidationDiagnostic;

//*********************************************************************************************************************
// Class: WebModelParityPhaseFourteenTest
//
// Description:
//
//   Verifies the shared Java and TypeScript Phase 14 authoring-model and occurrence contract fixtures.
//
//*********************************************************************************************************************

final class WebModelParityPhaseFourteenTest
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    private static final String CONTRACT_ROOT = "/fixtures/contract/";

    //=================================================================================================================
    // Tests
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: sharedModelFixtureMatchesPrettyCanonicalAndRevisionContracts
    //
    // Description:
    //
    //   Verifies that the shared model fixture matches the exact pretty, canonical, and revision contracts.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void sharedModelFixtureMatchesPrettyCanonicalAndRevisionContracts ()
    {
        // Prepare the codec, model, expected pretty JSON, expected canonical JSON, and expected revision values.

        AuthoringModelJsonCodec codec = new AuthoringModelJsonCodec ();
        AuthoringModel model = codec.read (
            readResource ( CONTRACT_ROOT + "valid/phase-14-ordering-and-integers.json" )
        );
        String expectedPretty = readResource (
            CONTRACT_ROOT + "expected/phase-14-ordering-and-integers.pretty.json"
        );
        String expectedCanonical = readResource (
            CONTRACT_ROOT + "expected/phase-14-ordering-and-integers.canonical.json"
        ).stripTrailing ();
        String expectedRevision = readResource (
            CONTRACT_ROOT + "expected/phase-14-ordering-and-integers.revision.txt"
        ).trim ();

        // Verify that Java produces the exact shared TypeScript contract values.

        assertThat ( new ModelValidator ().validate ( model ) ).isEmpty ();
        assertThat ( codec.writePretty ( model ) ).isEqualTo ( expectedPretty );
        assertThat ( codec.writeCanonical ( model ) )
            .isEqualTo ( expectedCanonical.getBytes ( StandardCharsets.UTF_8 ) );
        assertThat ( codec.revision ( model ) ).isEqualTo ( expectedRevision );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: sharedInvalidFixturesMatchJavaRejectionAndDiagnosticContracts
    //
    // Description:
    //
    //   Verifies that the shared invalid fixtures match the Java structural and semantic outcomes.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void sharedInvalidFixturesMatchJavaRejectionAndDiagnosticContracts ()
    {
        // Initialize the JSON codec and model validator used by the shared invalid-fixture checks.

        AuthoringModelJsonCodec codec = new AuthoringModelJsonCodec ();
        ModelValidator validator      = new ModelValidator ();

        // Verify nested duplicate-member rejection before semantic decoding.

        assertThatThrownBy (
            () -> codec.read (
                readResource ( CONTRACT_ROOT + "invalid/phase-14-nested-duplicate-member.json" )
            )
        ).isInstanceOf ( IllegalArgumentException.class );

        // Verify signed 64-bit overflow and range-order diagnostics.

        AuthoringModel overflowModel = codec.read (
            readResource ( CONTRACT_ROOT + "invalid/phase-14-integer-overflow.json" )
        );
        AuthoringModel invalidRangeModel = codec.read (
            readResource ( CONTRACT_ROOT + "invalid/phase-14-invalid-range.json" )
        );
        List <String> overflowCodes = validator.validate ( overflowModel ).stream ()
            .map ( ValidationDiagnostic::getCode )
            .toList ();
        List <String> invalidRangeCodes = validator.validate ( invalidRangeModel ).stream ()
            .map ( ValidationDiagnostic::getCode )
            .toList ();

        assertThat ( overflowCodes ).contains ( ModelValidator.NON_INTEGRAL_INTEGER );
        assertThat ( invalidRangeCodes ).contains ( ModelValidator.INVALID_CONDITION_RANGE );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: sharedOccurrenceFixturesPreserveConcreteNullAndOmissionStates
    //
    // Description:
    //
    //   Verifies that the shared occurrence fixtures preserve concrete, null, property-omitted, and payload-omitted
    //   states through the Java codec.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void sharedOccurrenceFixturesPreserveConcreteNullAndOmissionStates ()
    {
        // Initialize the occurrence codec used by each shared occurrence fixture.

        EventOccurrenceJsonCodec codec = new EventOccurrenceJsonCodec ();

        // Verify exact round trips for every shared occurrence state.

        for ( String fixtureName : List.of (
            "concrete.json",
            "present-null.json",
            "omitted-property.json",
            "omitted-payload.json"
        ) )
        {
            String document = readResource ( CONTRACT_ROOT + "occurrence/" + fixtureName ).stripTrailing ();

            assertThat ( codec.write ( codec.read ( document ) ) )
                .as ( fixtureName )
                .isEqualTo ( document );
        }

        // Verify the distinct payload-presence and payload-member states.

        assertThat (
            codec.read ( readResource ( CONTRACT_ROOT + "occurrence/present-null.json" ) )
                .getPayload ()
        ).containsEntry ( "parameter-string", null );
        assertThat (
            codec.read ( readResource ( CONTRACT_ROOT + "occurrence/omitted-property.json" ) )
                .getPayload ()
        ).doesNotContainKey ( "parameter-string" );
        assertThat (
            codec.read ( readResource ( CONTRACT_ROOT + "occurrence/omitted-payload.json" ) )
                .isPayloadPresent ()
        ).isFalse ();
    }

    //=================================================================================================================
    // Helpers
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readResource
    //
    // Description:
    //
    //   Reads one UTF-8 classpath resource with normalized line endings.
    //
    // Arguments:
    //
    //   resourceName (String):
    //     The absolute classpath resource name to read.
    //
    // Returns:
    //
    //   The normalized resource text.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String readResource ( String resourceName )
    {
        try ( InputStream resourceStream = WebModelParityPhaseFourteenTest.class.getResourceAsStream ( resourceName ) )
        {
            if ( resourceStream == null )
            {
                throw new IllegalArgumentException ( "Missing test resource: " + resourceName );
            }

            return new String ( resourceStream.readAllBytes (), StandardCharsets.UTF_8 )
                .replace ( "\r\n", "\n" );
        }
        catch ( IOException exception )
        {
            throw new IllegalStateException ( "Could not read test resource: " + resourceName, exception );
        }
    }
}
