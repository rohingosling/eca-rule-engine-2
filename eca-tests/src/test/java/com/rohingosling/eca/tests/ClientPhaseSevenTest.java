//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies the immutable model-editing workflows used by the JavaFX category grids and entity forms.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.rohingosling.eca.application.ClientDocumentSession;
import com.rohingosling.eca.application.ClientModelEditor;
import com.rohingosling.eca.application.ClientModelEditor.BindingDraft;
import com.rohingosling.eca.application.ClientModelEditor.ConditionParameterDescriptor;
import com.rohingosling.eca.application.ClientModelEditor.DeleteResult;
import com.rohingosling.eca.application.ClientModelEditor.EditResult;
import com.rohingosling.eca.application.ClientModelEditor.EntityDraft;
import com.rohingosling.eca.domain.CompiledConditionSet;
import com.rohingosling.eca.domain.ConcreteValue;
import com.rohingosling.eca.domain.ConditionOperator;
import com.rohingosling.eca.domain.ParameterDefinition;
import com.rohingosling.eca.domain.ParameterId;
import com.rohingosling.eca.domain.Payload;
import com.rohingosling.eca.engine.ConditionMatcher;
import com.rohingosling.eca.json.AuthoringModelJsonCodec;
import com.rohingosling.eca.model.AuthoringModel;
import com.rohingosling.eca.model.AuthoringModelCompiler;
import com.rohingosling.eca.model.CompiledModel;
import com.rohingosling.eca.model.ModelValidator;

//*********************************************************************************************************************
// Class: ClientPhaseSevenTest
//
// Description:
//
//   Verifies the immutable model-editing workflows used by the JavaFX category grids and entity forms.
//
//*********************************************************************************************************************

class ClientPhaseSevenTest
{
    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: allCategories_supportAddDuplicateEditAndDelete
    //
    // Description:
    //
    //   Verifies that all categories support add duplicate edit and delete.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void allCategories_supportAddDuplicateEditAndDelete ()
    {
        // Initialize the editor with a new client model editor.

        ClientModelEditor editor = new ClientModelEditor ();

        // Process each category identifier supplied by client model editor category identifiers.

        for ( String categoryIdentifier : ClientModelEditor.CATEGORY_IDENTIFIERS )
        {
            // Prepare the original model, original count, new draft, and add result values needed by the all
            // categories support add duplicate edit and delete operation.

            AuthoringModel originalModel = courierModel ();
            int originalCount = editor.getEntities ( originalModel, categoryIdentifier ).size ();
            EntityDraft newDraft = editor.createDraft ( originalModel, categoryIdentifier );
            EditResult addResult = editor.apply (
                originalModel,
                categoryIdentifier,
                null,
                newDraft
            );

            // Verify the all categories support add duplicate edit and delete scenario against its expected outcome.

            assertThat ( addResult.isSuccessful () )
                .as ( "%s Add", categoryIdentifier )
                .isTrue ();

            // Prepare the added model, added identifier, and duplicate result values needed by the all categories
            // support add duplicate edit and delete operation.

            AuthoringModel addedModel = addResult.getModel ().orElseThrow ();
            String addedIdentifier = addResult.getEntityIdentifier ().orElseThrow ();
            EditResult duplicateResult = editor.duplicate (
                addedModel,
                categoryIdentifier,
                addedIdentifier
            );

            // Verify the all categories support add duplicate edit and delete scenario against its expected outcome.

            assertThat ( duplicateResult.isSuccessful () )
                .as ( "%s Duplicate", categoryIdentifier )
                .isTrue ();

            // Prepare the duplicated model, duplicate identifier, and duplicate draft values needed by the all
            // categories support add duplicate edit and delete operation.

            AuthoringModel duplicatedModel = duplicateResult.getModel ().orElseThrow ();
            String duplicateIdentifier = duplicateResult.getEntityIdentifier ().orElseThrow ();
            EntityDraft duplicateDraft = editor.createDraft (
                duplicatedModel,
                categoryIdentifier,
                duplicateIdentifier
            );
            String editedIdentifier = duplicateIdentifier + "-edited";

            // Initialize the edit result by applying apply and with identity.

            EditResult editResult = editor.apply (
                duplicatedModel,
                categoryIdentifier,
                duplicateIdentifier,
                duplicateDraft.withIdentity ( editedIdentifier, "Edited entity" )
            );

            // Verify the all categories support add duplicate edit and delete scenario against its expected outcome.

            assertThat ( editResult.isSuccessful () )
                .as ( "%s Edit", categoryIdentifier )
                .isTrue ();

            // Initialize the delete result by applying delete, or else throw, and get model.

            DeleteResult deleteResult = editor.delete (
                editResult.getModel ().orElseThrow (),
                categoryIdentifier,
                editedIdentifier
            );

            // Verify the all categories support add duplicate edit and delete scenario against its expected outcome.

            assertThat ( deleteResult.isSuccessful () )
                .as ( "%s Delete", categoryIdentifier )
                .isTrue ();
            assertThat (
                editor.getEntities ( deleteResult.getModel ().orElseThrow (), categoryIdentifier )
            ).hasSize ( originalCount + 1 );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: declarationOrder_supportsBoundedImmutableMoves
    //
    // Description:
    //
    //   Verifies that declaration moves swap adjacent entities without mutating the original model and reject moves
    //   beyond a category boundary.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void declarationOrder_supportsBoundedImmutableMoves ()
    {
        ClientModelEditor editor = new ClientModelEditor ();
        AuthoringModel originalModel = courierModel ();
        List <? extends AuthoringModel.EntityDefinition> originalParameters = editor.getEntities (
            originalModel,
            ClientModelEditor.PARAMETERS
        );
        String firstIdentifier = originalParameters.get ( 0 ).getId ();
        String secondIdentifier = originalParameters.get ( 1 ).getId ();

        EditResult moveResult = editor.move (
            originalModel,
            ClientModelEditor.PARAMETERS,
            secondIdentifier,
            -1
        );

        assertThat ( moveResult.isSuccessful () ).isTrue ();
        assertThat ( editor.getEntities ( originalModel, ClientModelEditor.PARAMETERS ) )
            .extracting ( AuthoringModel.EntityDefinition::getId )
            .startsWith ( firstIdentifier, secondIdentifier );
        assertThat ( editor.getEntities ( moveResult.getModel ().orElseThrow (), ClientModelEditor.PARAMETERS ) )
            .extracting ( AuthoringModel.EntityDefinition::getId )
            .startsWith ( secondIdentifier, firstIdentifier );
        assertThat (
            editor.move ( originalModel, ClientModelEditor.PARAMETERS, firstIdentifier, -1 ).isSuccessful ()
        ).isFalse ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: conditionDefaults_followTheSelectedParameterType
    //
    // Description:
    //
    //   Verifies that condition defaults follow the selected parameter type.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void conditionDefaults_followTheSelectedParameterType ()
    {
        // Prepare the editor, content, string parameter, integer parameter, boolean parameter, and enumeration
        // parameter values needed by the condition defaults follow the selected parameter type operation.

        ClientModelEditor editor = new ClientModelEditor ();
        ClientDocumentSession.Content content = new ClientDocumentSession.Content ( courierModel () );
        ConditionParameterDescriptor stringParameter = editor.getConditionParameterDescriptor (
            content,
            "parameter-order-reference"
        ).orElseThrow ();
        ConditionParameterDescriptor integerParameter = editor.getConditionParameterDescriptor (
            content,
            "parameter-quantity"
        ).orElseThrow ();
        ConditionParameterDescriptor booleanParameter = editor.getConditionParameterDescriptor (
            content,
            "parameter-vip"
        ).orElseThrow ();
        ConditionParameterDescriptor enumerationParameter = editor.getConditionParameterDescriptor (
            content,
            "parameter-delivery-type"
        ).orElseThrow ();

        // Verify the condition defaults follow the selected parameter type scenario against its expected outcome.

        assertThat ( editor.getDefaultConditionValue ( stringParameter ) ).isEmpty ();
        assertThat ( editor.getDefaultConditionValue ( integerParameter ) ).isEqualTo ( "0" );
        assertThat ( editor.getDefaultConditionValue ( booleanParameter ) ).isEqualTo ( "false" );
        assertThat ( editor.getDefaultConditionValue ( enumerationParameter ) ).isEqualTo ( "PRIORITY" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: documentDetails_canBeAuthoredForTheCurrentExample
    //
    // Description:
    //
    //   Verifies that document details can be authored for the current example.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void documentDetails_canBeAuthoredForTheCurrentExample ()
    {
        // Prepare the editor, empty content, and result values needed by the document details can be authored for the
        // current example operation.

        ClientModelEditor editor = new ClientModelEditor ();
        ClientDocumentSession.Content emptyContent = ClientDocumentSession.Content.empty ();
        EditResult result = editor.applyModelDetails (
            emptyContent,
            "courier-selection",
            "ECA Model - Courier Selection",
            "Selects a courier for product orders and cancellations."
        );

        // Verify the document details can be authored for the current example scenario against its expected outcome.

        assertThat ( result.isSuccessful () ).isTrue ();
        assertThat ( result.getContent ().orElseThrow ().getSummary ().getModelId () )
            .isEqualTo ( "courier-selection" );
        assertThat ( result.getContent ().orElseThrow ().getSummary ().getName () )
            .isEqualTo ( "ECA Model - Courier Selection" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: existingCourierEntities_roundTripThroughEveryEditor
    //
    // Description:
    //
    //   Verifies that existing courier entities round trip through every editor.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void existingCourierEntities_roundTripThroughEveryEditor ()
    {
        // Initialize the editor with a new client model editor.

        ClientModelEditor editor = new ClientModelEditor ();

        // Process each category identifier supplied by client model editor category identifiers.

        for ( String categoryIdentifier : ClientModelEditor.CATEGORY_IDENTIFIERS )
        {
            // Initialize the model by applying courier model.

            AuthoringModel model = courierModel ();

            // Process each entity supplied by editor get entities model category identifier.

            for (
                AuthoringModel.EntityDefinition entity
                    : editor.getEntities ( model, categoryIdentifier )
            )
            {
                // Prepare the draft and result values needed by the existing courier entities round trip through every
                // editor operation.

                EntityDraft draft = editor.createDraft (
                    model,
                    categoryIdentifier,
                    entity.getId ()
                );
                EditResult result = editor.apply (
                    model,
                    categoryIdentifier,
                    entity.getId (),
                    draft
                );

                // Verify the existing courier entities round trip through every editor scenario against its expected
                // outcome.

                assertThat ( result.isSuccessful () )
                    .as ( "%s %s", categoryIdentifier, entity.getId () )
                    .isTrue ();
                assertThat ( new ModelValidator ().validate ( result.getModel ().orElseThrow () ) )
                    .as ( "%s %s validation", categoryIdentifier, entity.getId () )
                    .isEmpty ();

                // Update the model from the get model result.

                model = result.getModel ().orElseThrow ();
            }
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: identifierEdits_updateEveryCompatibleReferenceAtomically
    //
    // Description:
    //
    //   Verifies that identifier edits update every compatible reference atomically.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void identifierEdits_updateEveryCompatibleReferenceAtomically ()
    {
        // Prepare the editor and model values needed by the identifier edits update every compatible reference
        // atomically operation.

        ClientModelEditor editor = new ClientModelEditor ();
        AuthoringModel model = courierModel ();

        // Update the model from the rename result.

        model = rename (
            editor,
            model,
            ClientModelEditor.PARAMETERS,
            "parameter-delivery-type",
            "parameter-delivery-method"
        );

        // Verify the identifier edits update every compatible reference atomically scenario against its expected
        // outcome.

        assertThat (
            model.getPayloads ().stream ()
                .filter ( payload -> payload.getId ().equals ( "payload-order-product" ) )
                .findFirst ()
                .orElseThrow ()
                .getParameterIds ()
        )
            .contains ( "parameter-delivery-method" )
            .doesNotContain ( "parameter-delivery-type" );
        assertThat (
            model.getConditions ().stream ()
                .filter ( condition -> condition.getId ().equals ( "condition-delivery-type-standard" ) )
                .findFirst ()
                .orElseThrow ()
                .getParameterId ()
        ).isEqualTo ( "parameter-delivery-method" );

        // Update the model from the rename result.

        model = rename (
            editor,
            model,
            ClientModelEditor.PAYLOADS,
            "payload-order-product",
            "payload-product-order"
        );

        // Verify the identifier edits update every compatible reference atomically scenario against its expected
        // outcome.

        assertThat (
            model.getEvents ().stream ()
                .filter ( event -> event.getId ().equals ( "event-order-product" ) )
                .findFirst ()
                .orElseThrow ()
                .getPayloadId ()
        ).isEqualTo ( "payload-product-order" );

        // Update the model from the rename result.

        model = rename (
            editor,
            model,
            ClientModelEditor.EVENTS,
            "event-order-product",
            "event-product-order"
        );

        // Verify the identifier edits update every compatible reference atomically scenario against its expected
        // outcome.

        assertThat ( model.getRules ().stream ().map ( AuthoringModel.RuleDefinition::getEventId ) )
            .contains ( "event-product-order" )
            .doesNotContain ( "event-order-product" );

        // Update the model from the rename result.

        model = rename (
            editor,
            model,
            ClientModelEditor.CONDITIONS,
            "condition-delivery-type-standard",
            "condition-delivery-standard"
        );

        // Verify the identifier edits update every compatible reference atomically scenario against its expected
        // outcome.

        assertThat (
            model.getConditionSets ().stream ()
                .filter ( conditionSet -> conditionSet.getId ().equals (
                    "condition-set-retail-non-vip-local-standard"
                ) )
                .findFirst ()
                .orElseThrow ()
                .getConditionIds ()
        )
            .contains ( "condition-delivery-standard" )
            .doesNotContain ( "condition-delivery-type-standard" );

        // Update the model from the rename result.

        model = rename (
            editor,
            model,
            ClientModelEditor.CONDITION_SETS,
            "condition-set-retail-non-vip-local-standard",
            "condition-set-retail-local-standard"
        );

        // Verify the identifier edits update every compatible reference atomically scenario against its expected
        // outcome.

        assertThat (
            model.getRules ().stream ()
                .filter ( rule -> rule.getId ().equals ( "rule-test" ) )
                .findFirst ()
                .orElseThrow ()
                .getConditionSetId ()
        ).isEqualTo ( "condition-set-retail-local-standard" );

        // Update the model from the rename result.

        model = rename (
            editor,
            model,
            ClientModelEditor.ACTIONS,
            "action-local-courier",
            "action-select-local-courier"
        );

        // Verify the identifier edits update every compatible reference atomically scenario against its expected
        // outcome.

        assertThat ( model.getRules ().stream ().map ( AuthoringModel.RuleDefinition::getActionId ) )
            .contains ( "action-select-local-courier" )
            .doesNotContain ( "action-local-courier" );
        assertThat ( new ModelValidator ().validate ( model ) ).isEmpty ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: referencedDeletion_isBlockedWithAReferencePreview
    //
    // Description:
    //
    //   Verifies that referenced deletion is blocked with a reference preview.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void referencedDeletion_isBlockedWithAReferencePreview ()
    {
        // Prepare the editor, model, parameter deletion, condition set deletion, and action deletion values needed by
        // the referenced deletion is blocked with a reference preview operation.

        ClientModelEditor editor = new ClientModelEditor ();
        AuthoringModel model = courierModel ();

        DeleteResult parameterDeletion = editor.delete (
            model,
            ClientModelEditor.PARAMETERS,
            "parameter-delivery-type"
        );
        DeleteResult conditionSetDeletion = editor.delete (
            model,
            ClientModelEditor.CONDITION_SETS,
            "condition-set-retail-non-vip-local-standard"
        );
        DeleteResult actionDeletion = editor.delete (
            model,
            ClientModelEditor.ACTIONS,
            "action-local-courier"
        );

        // Verify the referenced deletion is blocked with a reference preview scenario against its expected outcome.

        assertThat ( parameterDeletion.isSuccessful () ).isFalse ();
        assertThat ( parameterDeletion.getReferences () )
            .anyMatch ( reference -> reference.contains ( "payload-order-product.parameterIds" ) )
            .anyMatch ( reference -> reference.contains ( "condition-delivery-type-priority.parameterId" ) );
        assertThat ( conditionSetDeletion.isSuccessful () ).isFalse ();
        assertThat ( conditionSetDeletion.getReferences () )
            .containsExactly ( "rule rule-test.conditionSetId" );
        assertThat ( actionDeletion.isSuccessful () ).isFalse ();
        assertThat ( actionDeletion.getReferences () ).isNotEmpty ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: conditionSetDraft_preservesConcreteWildcardAndOmissionStates
    //
    // Description:
    //
    //   Verifies that condition set draft preserves concrete wildcard and omission states.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void conditionSetDraft_preservesConcreteWildcardAndOmissionStates ()
    {
        // Prepare the editor, model, and bindings values needed by the condition set draft preserves concrete wildcard
        // and omission states operation.

        ClientModelEditor editor = new ClientModelEditor ();
        AuthoringModel model = legacyBindingModel ();
        LinkedHashMap <String, BindingDraft> bindings = omittedBindings ( model );

        // Perform the put, concrete, and wildcard calls required by the condition set draft preserves concrete
        // wildcard and omission states operation.

        bindings.put (
            "condition-quantity",
            BindingDraft.concrete ( "condition-quantity", "7" )
        );
        bindings.put (
            "condition-stock-code",
            BindingDraft.wildcard ( "condition-stock-code" )
        );

        // Prepare the draft and result values needed by the condition set draft preserves concrete wildcard and
        // omission states operation.

        EntityDraft draft = EntityDraft.conditionSet (
            "condition-set-editor-state-test",
            "Editor state test",
            "Verifies all three editor binding states.",
            bindings
        );
        EditResult result = editor.apply (
            model,
            ClientModelEditor.CONDITION_SETS,
            null,
            draft
        );

        // Verify the condition set draft preserves concrete wildcard and omission states scenario against its expected
        // outcome.

        assertThat ( result.isSuccessful () ).isTrue ();

        // Initialize the condition set by applying get, get condition sets, or else throw, get model, and size.

        AuthoringModel.ConditionSetDefinition conditionSet = result.getModel ().orElseThrow ()
            .getConditionSets ()
            .get ( model.getConditionSets ().size () );

        // Verify the condition set draft preserves concrete wildcard and omission states scenario against its expected
        // outcome.

        assertThat ( conditionSet.getBindings () )
            .containsEntry ( "condition-quantity", Long.valueOf ( 7L ) )
            .containsEntry ( "condition-stock-code", null )
            .hasSize ( 2 );
        assertThat ( editor.getSpecificity ( bindings.values () ) ).isEqualTo ( 3 );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: invalidConcreteText_remainsARejectedDraftWithoutChangingTheModel
    //
    // Description:
    //
    //   Verifies that invalid concrete text remains a rejected draft without changing the model.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void invalidConcreteText_remainsARejectedDraftWithoutChangingTheModel ()
    {
        // Prepare the editor, model, and bindings values needed by the invalid concrete text remains a rejected draft
        // without changing the model operation.

        ClientModelEditor editor = new ClientModelEditor ();
        AuthoringModel model = legacyBindingModel ();
        LinkedHashMap <String, BindingDraft> bindings = omittedBindings ( model );

        // Store binding draft concrete "condition-quantity" "not-an-integer" under "condition-quantity" in the
        // bindings.

        bindings.put (
            "condition-quantity",
            BindingDraft.concrete ( "condition-quantity", "not-an-integer" )
        );

        // Initialize the result by applying apply and condition set.

        EditResult result = editor.apply (
            model,
            ClientModelEditor.CONDITION_SETS,
            null,
            EntityDraft.conditionSet (
                "condition-set-invalid-draft",
                "Invalid draft",
                "The invalid text must remain outside committed typed state.",
                bindings
            )
        );

        // Verify the invalid concrete text remains a rejected draft without changing the model scenario against its
        // expected outcome.

        assertThat ( result.isSuccessful () ).isFalse ();
        assertThat ( result.getModel () ).isEmpty ();
        assertThat ( result.getErrors () )
            .anyMatch (
                error ->
                    error.getField ().equals ( "bindings.condition-quantity" )
                        && error.getMessage ().contains ( "signed 64-bit" )
            );
        assertThat ( model.getConditionSets () ).isEmpty ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: reusableCondition_ownsItsRangeAndConditionSetOnlySelectsIt
    //
    // Description:
    //
    //   Verifies that reusable condition owns its range and condition set only selects it.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void reusableCondition_ownsItsRangeAndConditionSetOnlySelectsIt ()
    {
        // Prepare the editor, original model, and condition result values needed by the reusable condition owns its
        // range and condition set only selects it operation.

        ClientModelEditor editor = new ClientModelEditor ();
        AuthoringModel originalModel = courierModel ();
        EditResult conditionResult = editor.apply (
            originalModel,
            ClientModelEditor.CONDITIONS,
            null,
            EntityDraft.condition (
                "condition-medium-quantity",
                "Medium quantity",
                "Quantity is greater than 10 and less than 100.",
                "parameter-quantity",
                ConditionOperator.BETWEEN_EXCLUSIVE.name (),
                "10",
                "100"
            )
        );

        // Verify the reusable condition owns its range and condition set only selects it scenario against its expected
        // outcome.

        assertThat ( conditionResult.isSuccessful () ).isTrue ();

        // Prepare the model with condition and condition set result values needed by the reusable condition owns its
        // range and condition set only selects it operation.

        AuthoringModel modelWithCondition = conditionResult.getModel ().orElseThrow ();
        EditResult conditionSetResult = editor.apply (
            modelWithCondition,
            ClientModelEditor.CONDITION_SETS,
            null,
            EntityDraft.conditionSet (
                "condition-set-medium-quantity",
                "Medium quantity",
                "Selects the reusable medium-quantity condition.",
                List.of ( "condition-medium-quantity" )
            )
        );

        // Verify the reusable condition owns its range and condition set only selects it scenario against its expected
        // outcome.

        assertThat ( conditionSetResult.isSuccessful () ).isTrue ();

        // Prepare the edited model, condition, and condition set values needed by the reusable condition owns its
        // range and condition set only selects it operation.

        AuthoringModel editedModel = conditionSetResult.getModel ().orElseThrow ();
        AuthoringModel.ConditionDefinition condition = editedModel.getConditions ().stream ()
            .filter ( value -> value.getId ().equals ( "condition-medium-quantity" ) )
            .findFirst ()
            .orElseThrow ();
        AuthoringModel.ConditionSetDefinition conditionSet = editedModel.getConditionSets ().stream ()
            .filter ( value -> value.getId ().equals ( "condition-set-medium-quantity" ) )
            .findFirst ()
            .orElseThrow ();

        // Verify the reusable condition owns its range and condition set only selects it scenario against its expected
        // outcome.

        assertThat ( condition.getOperator () ).isEqualTo ( "BETWEEN_EXCLUSIVE" );
        assertThat ( condition.getFirstValue () ).isEqualTo ( Long.valueOf ( 10L ) );
        assertThat ( condition.getSecondValue () ).isEqualTo ( Long.valueOf ( 100L ) );
        assertThat ( conditionSet.usesPredefinedConditions () ).isTrue ();
        assertThat ( conditionSet.getConditionIds () )
            .containsExactly ( "condition-medium-quantity" );
        assertThat ( conditionSet.getBindings () ).isEmpty ();

        // Prepare the codec, round tripped model, compiled model, compiled condition set, parameter, parameter ID, and
        // matcher values needed by the reusable condition owns its range and condition set only selects it operation.

        AuthoringModelJsonCodec codec = new AuthoringModelJsonCodec ();
        AuthoringModel roundTrippedModel = codec.read ( codec.writePretty ( editedModel ) );
        CompiledModel compiledModel = new AuthoringModelCompiler ().compile (
            roundTrippedModel,
            "condition-predicate-test"
        );
        CompiledConditionSet compiledConditionSet = compiledModel.getConditionSets ().get (
            "condition-set-medium-quantity"
        );
        ParameterDefinition parameter = compiledModel.getParameterDefinitions ().get (
            "parameter-quantity"
        );
        ParameterId parameterId = parameter.getParameterId ();
        ConditionMatcher matcher = new ConditionMatcher ();

        // Verify the reusable condition owns its range and condition set only selects it scenario against its expected
        // outcome.

        assertThat (
            matcher.matches (
                compiledConditionSet,
                new Payload (
                    Map.of ( parameterId, new ConcreteValue ( parameter, Long.valueOf ( 11L ) ) )
                )
            )
        ).isTrue ();
        assertThat (
            matcher.matches (
                compiledConditionSet,
                new Payload (
                    Map.of ( parameterId, new ConcreteValue ( parameter, Long.valueOf ( 100L ) ) )
                )
            )
        ).isFalse ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: undoAndRedo_keepCountsValidationAndSelectionSynchronized
    //
    // Description:
    //
    //   Verifies that undo and redo keep counts validation and selection synchronized.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void undoAndRedo_keepCountsValidationAndSelectionSynchronized ()
    {
        // Prepare the editor, session, and duplicate result values needed by the undo and redo keep counts validation
        // and selection synchronized operation.

        ClientModelEditor editor = new ClientModelEditor ();
        ClientDocumentSession session = ClientDocumentSession.pulled (
            new ClientDocumentSession.Content ( courierModel () )
        );
        EditResult duplicateResult = editor.duplicate (
            session.getContent ().getAuthoringModel (),
            ClientModelEditor.ACTIONS,
            "action-local-courier"
        );

        // Verify the undo and redo keep counts validation and selection synchronized scenario against its expected
        // outcome.

        session.applyEdit (
            new ClientDocumentSession.Content ( duplicateResult.getModel ().orElseThrow () )
        );
        session.selectEntity (
            ClientModelEditor.ACTIONS,
            duplicateResult.getEntityIdentifier ().orElseThrow ()
        );

        assertThat ( session.getContent ().getSummary ().getActionCount () ).isEqualTo ( 4 );
        assertThat ( session.canPush () ).isTrue ();

        session.undo ();

        assertThat ( session.getContent ().getSummary ().getActionCount () ).isEqualTo ( 3 );
        assertThat ( session.canPush () ).isTrue ();
        assertThat ( session.getSelection ().getKind () )
            .isEqualTo ( ClientDocumentSession.Selection.Kind.CATEGORY );

        session.redo ();

        assertThat ( session.getContent ().getSummary ().getActionCount () ).isEqualTo ( 4 );
        assertThat ( session.canPush () ).isTrue ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: rename
    //
    // Description:
    //
    //   Performs the rename operation.
    //
    // Arguments:
    //
    //   editor (ClientModelEditor):
    //     The editor to use.
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   originalIdentifier (String):
    //     The original identifier to use.
    //
    //   replacementIdentifier (String):
    //     The replacement identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static AuthoringModel rename (
        ClientModelEditor editor,
        AuthoringModel model,
        String categoryIdentifier,
        String originalIdentifier,
        String replacementIdentifier
    )
    {
        // Prepare the draft and result values needed by the rename operation.

        EntityDraft draft = editor.createDraft (
            model,
            categoryIdentifier,
            originalIdentifier
        );
        EditResult result = editor.apply (
            model,
            categoryIdentifier,
            originalIdentifier,
            draft.withIdentity ( replacementIdentifier, draft.getName () )
        );

        // Verify the rename scenario against its expected outcome.

        assertThat ( result.isSuccessful () ).isTrue ();

        // Return the result produced by or else throw.

        return result.getModel ().orElseThrow ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: omittedBindings
    //
    // Description:
    //
    //   Performs the omitted bindings operation.
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

    private static LinkedHashMap <String, BindingDraft> omittedBindings ( AuthoringModel model )
    {
        // Initialize the bindings with a new linked hash map.

        LinkedHashMap <String, BindingDraft> bindings = new LinkedHashMap <String, BindingDraft> ();

        // Process each condition supplied by model conditions.

        for ( AuthoringModel.ConditionDefinition condition : model.getConditions () )
        {
            // Store binding draft omitted condition ID under condition ID in the bindings.

            bindings.put ( condition.getId (), BindingDraft.omitted ( condition.getId () ) );
        }

        // Return the bindings to the caller.

        return bindings;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: legacyBindingModel
    //
    // Description:
    //
    //   Performs the legacy binding model operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static AuthoringModel legacyBindingModel ()
    {
        // Return the result produced by read.

        return new AuthoringModelJsonCodec ().read (
            """
            {
              "schemaVersion": "1.0",
              "modelId": "legacy-editor-test",
              "name": "Legacy editor test",
              "description": "Exercises the retained binding-based condition-set format.",
              "parameters": [
                {
                  "id": "parameter-quantity",
                  "name": "Quantity",
                  "description": "Quantity value.",
                  "type": "INTEGER"
                },
                {
                  "id": "parameter-stock-code",
                  "name": "Stock code",
                  "description": "Stock code value.",
                  "type": "STRING"
                }
              ],
              "payloads": [],
              "events": [],
              "conditions": [
                {
                  "id": "condition-quantity",
                  "name": "Quantity",
                  "description": "Legacy quantity binding.",
                  "parameterId": "parameter-quantity"
                },
                {
                  "id": "condition-stock-code",
                  "name": "Stock code",
                  "description": "Legacy stock-code binding.",
                  "parameterId": "parameter-stock-code"
                }
              ],
              "conditionSets": [],
              "actions": [],
              "rules": []
            }
            """
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: courierModel
    //
    // Description:
    //
    //   Performs the courier model operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static AuthoringModel courierModel ()
    {
        // Initialize the project root by applying of and get property.

        Path projectRoot = Path.of ( System.getProperty ( "eca.project.root" ) );

        try
        {
            // Return the result produced by read.

            return new AuthoringModelJsonCodec ().read (
                Files.readAllBytes ( projectRoot.resolve ( "examples" ).resolve ( "eca-rule-engine-example.json" ) )
            );
        }

        // Handle I/O failures captured as exception.

        catch ( IOException exception )
        {
            throw new IllegalStateException ( "The accepted courier fixture could not be read.", exception );
        }
    }
}
