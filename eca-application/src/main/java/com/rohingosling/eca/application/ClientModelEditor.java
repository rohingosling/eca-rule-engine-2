//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Applies immutable, reference-aware desktop model edits without depending on JavaFX controls.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import com.rohingosling.eca.domain.ConditionOperator;
import com.rohingosling.eca.domain.ParameterType;
import com.rohingosling.eca.model.AuthoringModel;

//*********************************************************************************************************************
// Class: ClientModelEditor
//
// Description:
//
//   Applies immutable, reference-aware desktop model edits without depending on JavaFX controls.
//
//*********************************************************************************************************************

public final class ClientModelEditor
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    public static final String PARAMETERS     = "parameters";
    public static final String PAYLOADS       = "payloads";
    public static final String EVENTS         = "events";
    public static final String CONDITIONS     = "conditions";
    public static final String CONDITION_SETS = "condition-sets";
    public static final String ACTIONS        = "actions";
    public static final String RULES          = "rules";

    public static final List <String> CATEGORY_IDENTIFIERS = List.of (
        PARAMETERS,
        PAYLOADS,
        EVENTS,
        CONDITIONS,
        CONDITION_SETS,
        ACTIONS,
        RULES
    );

    private static final Pattern IDENTIFIER_PATTERN =
        Pattern.compile ( "^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$" );

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getEntities
    //
    // Description:
    //
    //   Returns the entities.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    // Returns:
    //
    //   The entities.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public List <? extends AuthoringModel.EntityDefinition> getEntities (
        AuthoringModel model,
        String categoryIdentifier
    )
    {
        // Validate the required model before continuing.

        Objects.requireNonNull ( model, "model" );

        // Return the get entities result to the caller.

        return switch ( requireCategory ( categoryIdentifier ) )
        {
            // Handle parameters through this switch branch.

            case PARAMETERS     -> model.getParameters ();

            // Handle payloads through this switch branch.

            case PAYLOADS       -> model.getPayloads ();

            // Handle events through this switch branch.

            case EVENTS         -> model.getEvents ();

            // Handle conditions through this switch branch.

            case CONDITIONS     -> model.getConditions ();

            // Handle condition sets through this switch branch.

            case CONDITION_SETS -> model.getConditionSets ();

            // Handle actions through this switch branch.

            case ACTIONS        -> model.getActions ();

            // Handle rules through this switch branch.

            case RULES          -> model.getRules ();

            // Handle the default case through this switch branch.

            default             -> throw new IllegalStateException ( "Unreachable category." );
        };
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: findEntity
    //
    // Description:
    //
    //   Performs the find entity operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   entityIdentifier (String):
    //     The entity identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Optional <AuthoringModel.EntityDefinition> findEntity (
        AuthoringModel model,
        String categoryIdentifier,
        String entityIdentifier
    )
    {
        // Validate the required entity identifier before continuing.

        Objects.requireNonNull ( entityIdentifier, "entityIdentifier" );

        // Return the result produced by find first.

        return getEntities ( model, categoryIdentifier ).stream ()
            .filter ( entity -> entityIdentifier.equals ( entity.getId () ) )
            .map ( entity -> (AuthoringModel.EntityDefinition) entity )
            .findFirst ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getEntitySummaries
    //
    // Description:
    //
    //   Returns the entity summaries.
    //
    // Arguments:
    //
    //   content (ClientDocumentSession.Content):
    //     The content to use.
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    // Returns:
    //
    //   The entity summaries.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public List <EntitySummary> getEntitySummaries (
        ClientDocumentSession.Content content,
        String categoryIdentifier
    )
    {
        // Initialize the model by applying get authoring model.

        AuthoringModel model = content.getAuthoringModel ();

        // Return the result produced by to list.

        return getEntities ( model, categoryIdentifier ).stream ()
            .map (
                entity -> new EntitySummary (
                    entity.getId (),
                    entity.getName (),
                    describeEntity ( model, categoryIdentifier, entity )
                )
            )
            .toList ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createDraft
    //
    // Description:
    //
    //   Performs the create draft operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public EntityDraft createDraft ( AuthoringModel model, String categoryIdentifier )
    {
        // Prepare the category, noun, prefix, and identifier values needed by the create draft operation.

        String category = requireCategory ( categoryIdentifier );
        String noun     = singularName ( category );

        // Select the result branch for category.

        String prefix   = switch ( category )
        {
            // Handle parameters through this switch branch.

            case PARAMETERS     -> "parameter-new";

            // Handle payloads through this switch branch.

            case PAYLOADS       -> "payload-new";

            // Handle events through this switch branch.

            case EVENTS         -> "event-new";

            // Handle conditions through this switch branch.

            case CONDITIONS     -> "condition-new";

            // Handle condition sets through this switch branch.

            case CONDITION_SETS -> "condition-set-new";

            // Handle actions through this switch branch.

            case ACTIONS        -> "action-new";

            // Handle rules through this switch branch.

            case RULES          -> "rule-new";

            // Handle the default case through this switch branch.

            default             -> throw new IllegalStateException ( "Unreachable category." );
        };
        String identifier = uniqueIdentifier ( model, category, prefix );
        String name       = "New " + noun;
        String description = "Describe this " + noun + ".";

        // Return the create draft result to the caller.

        return switch ( category )
        {
            // Handle parameters through this switch branch.

            case PARAMETERS -> EntityDraft.parameter (
                identifier,
                name,
                description,
                ParameterType.STRING.name (),
                List.of ()
            );

            // Handle payloads through this switch branch.

            case PAYLOADS -> EntityDraft.payload (
                identifier,
                name,
                description,
                List.of ()
            );

            // Handle events through this switch branch.

            case EVENTS -> EntityDraft.event (
                identifier,
                name,
                description,
                firstIdentifier ( model.getPayloads () )
            );

            // Handle conditions through this switch branch.

            case CONDITIONS -> EntityDraft.condition (
                identifier,
                name,
                description,
                firstIdentifier ( model.getParameters () ),
                ConditionOperator.EQUALS.name (),
                defaultConditionValue ( model, firstIdentifier ( model.getParameters () ) ),
                ""
            );

            // Handle condition sets through this switch branch.

            case CONDITION_SETS -> EntityDraft.conditionSet (
                identifier,
                name,
                description,
                List.of ()
            );

            // Handle actions through this switch branch.

            case ACTIONS -> EntityDraft.action ( identifier, name, description );

            // Handle rules through this switch branch.

            case RULES -> EntityDraft.rule (
                identifier,
                name,
                description,
                firstIdentifier ( model.getEvents () ),
                firstIdentifier ( model.getConditionSets () ),
                firstIdentifier ( model.getActions () )
            );

            // Handle the default case through this switch branch.

            default -> throw new IllegalStateException ( "Unreachable category." );
        };
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createDraft
    //
    // Description:
    //
    //   Performs the create draft operation.
    //
    // Arguments:
    //
    //   content (ClientDocumentSession.Content):
    //     The content to use.
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public EntityDraft createDraft (
        ClientDocumentSession.Content content,
        String categoryIdentifier
    )
    {
        // Return the result produced by create draft.

        return createDraft ( content.getAuthoringModel (), categoryIdentifier );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createDraft
    //
    // Description:
    //
    //   Performs the create draft operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   entityIdentifier (String):
    //     The entity identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public EntityDraft createDraft (
        AuthoringModel model,
        String categoryIdentifier,
        String entityIdentifier
    )
    {
        // Initialize the entity by applying or else throw and find entity.

        AuthoringModel.EntityDefinition entity = findEntity (
            model,
            categoryIdentifier,
            entityIdentifier
        ).orElseThrow (
            () -> new IllegalArgumentException ( "Unknown entity: " + entityIdentifier )
        );

        // Handle the branch where entity is an authoring model parameter definition.

        if ( entity instanceof AuthoringModel.ParameterDefinition )
        {
            AuthoringModel.ParameterDefinition parameter = (AuthoringModel.ParameterDefinition) entity;

            // Return the result produced by parameter when entity is an authoring model parameter definition.

            return EntityDraft.parameter (
                parameter.getId (),
                parameter.getName (),
                parameter.getDescription (),
                parameter.getType (),
                parameter.getEnumerationValues ()
            );
        }

        // Handle the branch where entity is an authoring model payload definition.

        if ( entity instanceof AuthoringModel.PayloadDefinition )
        {
            AuthoringModel.PayloadDefinition payload = (AuthoringModel.PayloadDefinition) entity;

            // Return the result produced by payload when entity is an authoring model payload definition.

            return EntityDraft.payload (
                payload.getId (),
                payload.getName (),
                payload.getDescription (),
                payload.getParameterIds ()
            );
        }

        // Handle the branch where entity is an authoring model event definition.

        if ( entity instanceof AuthoringModel.EventDefinition )
        {
            AuthoringModel.EventDefinition event = (AuthoringModel.EventDefinition) entity;

            // Return the result produced by event when entity is an authoring model event definition.

            return EntityDraft.event (
                event.getId (),
                event.getName (),
                event.getDescription (),
                event.getPayloadId ()
            );
        }

        // Handle the branch where entity is an authoring model condition definition.

        if ( entity instanceof AuthoringModel.ConditionDefinition )
        {
            AuthoringModel.ConditionDefinition condition = (AuthoringModel.ConditionDefinition) entity;

            // Return the result produced by condition when entity is an authoring model condition definition.

            return EntityDraft.condition (
                condition.getId (),
                condition.getName (),
                condition.getDescription (),
                condition.getParameterId (),
                condition.hasPredicate ()
                    ? condition.getOperator ()
                    : ConditionOperator.EQUALS.name (),
                condition.hasPredicate ()
                    ? displayValue ( condition.getFirstValue () )
                    : defaultConditionValue ( model, condition.getParameterId () ),
                condition.hasPredicate () ? displayValue ( condition.getSecondValue () ) : ""
            );
        }

        // Handle the branch where entity is an authoring model condition set definition.

        if ( entity instanceof AuthoringModel.ConditionSetDefinition )
        {
            AuthoringModel.ConditionSetDefinition conditionSet =
                (AuthoringModel.ConditionSetDefinition) entity;

            // Stop this path and return its result when condition set uses predefined conditions succeeds.

            if ( conditionSet.usesPredefinedConditions () )
            {
                // Return the result produced by condition set when condition set uses predefined conditions succeeds.

                return EntityDraft.conditionSet (
                    conditionSet.getId (),
                    conditionSet.getName (),
                    conditionSet.getDescription (),
                    conditionSet.getConditionIds ()
                );
            }

            // Return the result produced by legacy condition set when entity is an authoring model condition set
            // definition.

            return EntityDraft.legacyConditionSet (
                conditionSet.getId (),
                conditionSet.getName (),
                conditionSet.getDescription (),
                bindingDrafts ( model, conditionSet )
            );
        }

        // Stop this path and return its result when entity is an authoring model action definition.

        if ( entity instanceof AuthoringModel.ActionDefinition )
        {
            // Return the result produced by action when entity is an authoring model action definition.

            return EntityDraft.action (
                entity.getId (),
                entity.getName (),
                entity.getDescription ()
            );
        }

        AuthoringModel.RuleDefinition rule = (AuthoringModel.RuleDefinition) entity;

        // Return the result produced by rule.

        return EntityDraft.rule (
            rule.getId (),
            rule.getName (),
            rule.getDescription (),
            rule.getEventId (),
            rule.getConditionSetId (),
            rule.getActionId ()
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createDraft
    //
    // Description:
    //
    //   Performs the create draft operation.
    //
    // Arguments:
    //
    //   content (ClientDocumentSession.Content):
    //     The content to use.
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   entityIdentifier (String):
    //     The entity identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public EntityDraft createDraft (
        ClientDocumentSession.Content content,
        String categoryIdentifier,
        String entityIdentifier
    )
    {
        // Return the result produced by create draft.

        return createDraft (
            content.getAuthoringModel (),
            categoryIdentifier,
            entityIdentifier
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: apply
    //
    // Description:
    //
    //   Performs the apply operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   originalEntityIdentifier (String):
    //     The original entity identifier to use.
    //
    //   draft (EntityDraft):
    //     The draft to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public EditResult apply (
        AuthoringModel model,
        String categoryIdentifier,
        String originalEntityIdentifier,
        EntityDraft draft
    )
    {
        // Validate the required model and draft before continuing.

        Objects.requireNonNull ( model, "model" );
        Objects.requireNonNull ( draft, "draft" );

        // Prepare the category and errors values needed by the apply operation.

        String category = requireCategory ( categoryIdentifier );
        ArrayList <FieldError> errors = new ArrayList <FieldError> ();

        // Complete the apply step by calling validate common draft.

        validateCommonDraft ( model, category, originalEntityIdentifier, draft, errors );

        // Initialize the replacement by applying create entity.

        AuthoringModel.EntityDefinition replacement = createEntity (
            model,
            category,
            draft,
            errors
        );

        // Stop this path and return its result when errors contains values.

        if ( !errors.isEmpty () )
        {
            // Return the result produced by failure when errors contains values.

            return EditResult.failure ( errors );
        }

        // Initialize the replacement model by applying replace entity.

        AuthoringModel replacementModel = replaceEntity (
            model,
            category,
            originalEntityIdentifier,
            replacement
        );

        // Handle the branch where original entity identifier is available and original entity identifier differs from
        // replacement ID.

        if (
            originalEntityIdentifier != null
                && !originalEntityIdentifier.equals ( replacement.getId () )
        )
        {
            // Update the replacement model from the get ID result.

            replacementModel = renameReferences (
                replacementModel,
                category,
                originalEntityIdentifier,
                replacement.getId ()
            );
        }

        // Return the result produced by success.

        return EditResult.success ( replacementModel, replacement.getId () );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: apply
    //
    // Description:
    //
    //   Performs the apply operation.
    //
    // Arguments:
    //
    //   content (ClientDocumentSession.Content):
    //     The content to use.
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   originalEntityIdentifier (String):
    //     The original entity identifier to use.
    //
    //   draft (EntityDraft):
    //     The draft to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public EditResult apply (
        ClientDocumentSession.Content content,
        String categoryIdentifier,
        String originalEntityIdentifier,
        EntityDraft draft
    )
    {
        // Return the result produced by apply.

        return apply (
            content.getAuthoringModel (),
            categoryIdentifier,
            originalEntityIdentifier,
            draft
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: applyModelDetails
    //
    // Description:
    //
    //   Performs the apply model details operation.
    //
    // Arguments:
    //
    //   content (ClientDocumentSession.Content):
    //     The content to use.
    //
    //   modelIdentifier (String):
    //     The model identifier to use.
    //
    //   name (String):
    //     The name to use.
    //
    //   description (String):
    //     The description to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public EditResult applyModelDetails (
        ClientDocumentSession.Content content,
        String modelIdentifier,
        String name,
        String description
    )
    {
        // Validate the required content, model identifier, name, and description before continuing.

        Objects.requireNonNull ( content, "content" );
        Objects.requireNonNull ( modelIdentifier, "modelIdentifier" );
        Objects.requireNonNull ( name, "name" );
        Objects.requireNonNull ( description, "description" );

        // Initialize the errors with a new array list.

        ArrayList <FieldError> errors = new ArrayList <FieldError> ();

        // Handle the branch where identifier pattern matcher model identifier does not match the supplied values.

        if ( !IDENTIFIER_PATTERN.matcher ( modelIdentifier ).matches () )
        {
            // Add new field error "model id" "use lowercase words and digits separated by single to the errors.

            errors.add (
                new FieldError (
                    "modelId",
                    "Use lowercase words and digits separated by single hyphens."
                )
            );
        }

        // Handle the branch where name trim contains no values.

        if ( name.trim ().isEmpty () )
        {
            // Add new field error "name" "enter a nonblank display name " to the errors.

            errors.add ( new FieldError ( "name", "Enter a nonblank display name." ) );
        }

        // Handle the branch where description trim contains no values.

        if ( description.trim ().isEmpty () )
        {
            // Add new field error "description" "enter a nonblank description " to the errors.

            errors.add ( new FieldError ( "description", "Enter a nonblank description." ) );
        }

        // Stop this path and return its result when errors contains values.

        if ( !errors.isEmpty () )
        {
            // Return the result produced by failure when errors contains values.

            return EditResult.failure ( errors );
        }

        // Prepare the model and replacement model values needed by the apply model details operation.

        AuthoringModel model = content.getAuthoringModel ();
        AuthoringModel replacementModel = new AuthoringModel (
            model.getSchemaVersion (),
            modelIdentifier,
            name,
            description,
            model.getParameters (),
            model.getPayloads (),
            model.getEvents (),
            model.getConditions (),
            model.getConditionSets (),
            model.getActions (),
            model.getRules ()
        );

        // Return the result produced by success.

        return EditResult.success ( replacementModel, modelIdentifier );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: duplicate
    //
    // Description:
    //
    //   Performs the duplicate operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   entityIdentifier (String):
    //     The entity identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public EditResult duplicate (
        AuthoringModel model,
        String categoryIdentifier,
        String entityIdentifier
    )
    {
        // Prepare the source draft, copy identifier, and copy draft values needed by the duplicate operation.

        EntityDraft sourceDraft = createDraft ( model, categoryIdentifier, entityIdentifier );
        String copyIdentifier = uniqueIdentifier (
            model,
            categoryIdentifier,
            sourceDraft.getId () + "-copy"
        );
        EntityDraft copyDraft = sourceDraft.withIdentity (
            copyIdentifier,
            sourceDraft.getName () + " copy"
        );

        // Return the result produced by apply.

        return apply ( model, categoryIdentifier, null, copyDraft );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: duplicate
    //
    // Description:
    //
    //   Performs the duplicate operation.
    //
    // Arguments:
    //
    //   content (ClientDocumentSession.Content):
    //     The content to use.
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   entityIdentifier (String):
    //     The entity identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public EditResult duplicate (
        ClientDocumentSession.Content content,
        String categoryIdentifier,
        String entityIdentifier
    )
    {
        // Return the result produced by duplicate.

        return duplicate (
            content.getAuthoringModel (),
            categoryIdentifier,
            entityIdentifier
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: delete
    //
    // Description:
    //
    //   Performs the delete operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   entityIdentifier (String):
    //     The entity identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public DeleteResult delete (
        AuthoringModel model,
        String categoryIdentifier,
        String entityIdentifier
    )
    {
        // Validate the required model and entity identifier before continuing.

        Objects.requireNonNull ( model, "model" );
        Objects.requireNonNull ( entityIdentifier, "entityIdentifier" );

        // Initialize the category by applying require category.

        String category = requireCategory ( categoryIdentifier );

        // Stop this path and return its result when find entity model category entity identifier contains no values.

        if ( findEntity ( model, category, entityIdentifier ).isEmpty () )
        {
            // Return the result produced by blocked when find entity model category entity identifier contains no
            // values.

            return DeleteResult.blocked (
                List.of ( "The selected entity no longer exists." )
            );
        }

        // Initialize the references by applying find references.

        List <String> references = findReferences ( model, category, entityIdentifier );

        // Stop this path and return its result when references contains values.

        if ( !references.isEmpty () )
        {
            // Return the result produced by blocked when references contains values.

            return DeleteResult.blocked ( references );
        }

        // Return the result produced by success.

        return DeleteResult.success (
            replaceEntity ( model, category, entityIdentifier, null ),
            entityIdentifier
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: delete
    //
    // Description:
    //
    //   Performs the delete operation.
    //
    // Arguments:
    //
    //   content (ClientDocumentSession.Content):
    //     The content to use.
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   entityIdentifier (String):
    //     The entity identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public DeleteResult delete (
        ClientDocumentSession.Content content,
        String categoryIdentifier,
        String entityIdentifier
    )
    {
        // Return the result produced by delete.

        return delete (
            content.getAuthoringModel (),
            categoryIdentifier,
            entityIdentifier
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getCompatibleIdentifiers
    //
    // Description:
    //
    //   Returns the compatible identifiers.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   field (String):
    //     The field to use.
    //
    // Returns:
    //
    //   The compatible identifiers.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public List <String> getCompatibleIdentifiers (
        AuthoringModel model,
        String categoryIdentifier,
        String field
    )
    {
        // Validate the required field before continuing.

        Objects.requireNonNull ( field, "field" );

        // Initialize the referenced category by applying require category.

        String referencedCategory = switch ( requireCategory ( categoryIdentifier ) + "." + field )
        {
            // Handle payloads + " parameter ids" and conditions + " parameter id" through this switch branch.

            case PAYLOADS + ".parameterIds", CONDITIONS + ".parameterId" -> PARAMETERS;

            // Handle events + " payload id" through this switch branch.

            case EVENTS + ".payloadId"                                      -> PAYLOADS;

            // Handle rules + " event id" through this switch branch.

            case RULES + ".eventId"                                         -> EVENTS;

            // Handle condition sets + " bindings" and condition sets + " condition ids" through this switch branch.

            case CONDITION_SETS + ".bindings", CONDITION_SETS + ".conditionIds" -> CONDITIONS;

            // Handle rules + " condition set id" through this switch branch.

            case RULES + ".conditionSetId"                                  -> CONDITION_SETS;

            // Handle rules + " action id" through this switch branch.

            case RULES + ".actionId"                                        -> ACTIONS;

            // Handle the default case through this switch branch.

            default -> throw new IllegalArgumentException (
                "The field does not contain a compatible model reference: " + field
            );
        };

        // Return the result produced by to list.

        return getEntities ( model, referencedCategory ).stream ()
            .map ( AuthoringModel.EntityDefinition::getId )
            .toList ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getCompatibleIdentifiers
    //
    // Description:
    //
    //   Returns the compatible identifiers.
    //
    // Arguments:
    //
    //   content (ClientDocumentSession.Content):
    //     The content to use.
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   field (String):
    //     The field to use.
    //
    // Returns:
    //
    //   The compatible identifiers.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public List <String> getCompatibleIdentifiers (
        ClientDocumentSession.Content content,
        String categoryIdentifier,
        String field
    )
    {
        // Return the result produced by get compatible identifiers.

        return getCompatibleIdentifiers (
            content.getAuthoringModel (),
            categoryIdentifier,
            field
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getConditionBindingDescriptors
    //
    // Description:
    //
    //   Returns the condition binding descriptors.
    //
    // Arguments:
    //
    //   content (ClientDocumentSession.Content):
    //     The content to use.
    //
    //   conditionSetDraft (EntityDraft):
    //     The condition set draft to use.
    //
    // Returns:
    //
    //   The condition binding descriptors.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public List <ConditionBindingDescriptor> getConditionBindingDescriptors (
        ClientDocumentSession.Content content,
        EntityDraft conditionSetDraft
    )
    {
        // Prepare the model, parameters, and descriptors values needed by the get condition binding descriptors
        // operation.

        AuthoringModel model = content.getAuthoringModel ();
        Map <String, AuthoringModel.ParameterDefinition> parameters = indexParameters ( model );
        ArrayList <ConditionBindingDescriptor> descriptors =
            new ArrayList <ConditionBindingDescriptor> ();

        // Process each condition supplied by model conditions.

        for ( AuthoringModel.ConditionDefinition condition : model.getConditions () )
        {
            // Prepare the parameter and binding values needed by the get condition binding descriptors operation.

            AuthoringModel.ParameterDefinition parameter = parameters.get ( condition.getParameterId () );
            BindingDraft binding = conditionSetDraft.getBindings ().getOrDefault (
                condition.getId (),
                BindingDraft.omitted ( condition.getId () )
            );

            // Add new condition binding descriptor condition ID parameter == null ? null : param to the descriptors.

            descriptors.add (
                new ConditionBindingDescriptor (
                    condition.getId (),
                    parameter == null ? null : parameter.getType (),
                    parameter == null ? List.of () : parameter.getEnumerationValues (),
                    binding
                )
            );
        }

        // Return an immutable copy of descriptors.

        return List.copyOf ( descriptors );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getConditionSelectionDescriptors
    //
    // Description:
    //
    //   Returns the condition selection descriptors.
    //
    // Arguments:
    //
    //   content (ClientDocumentSession.Content):
    //     The content to use.
    //
    //   conditionSetDraft (EntityDraft):
    //     The condition set draft to use.
    //
    // Returns:
    //
    //   The condition selection descriptors.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public List <ConditionSelectionDescriptor> getConditionSelectionDescriptors (
        ClientDocumentSession.Content content,
        EntityDraft conditionSetDraft
    )
    {
        // Prepare the model, parameters, selected condition IDs, and descriptors values needed by the get condition
        // selection descriptors operation.

        AuthoringModel model = content.getAuthoringModel ();
        Map <String, AuthoringModel.ParameterDefinition> parameters = indexParameters ( model );
        Set <String> selectedConditionIds = conditionSetDraft.usesPredefinedConditions ()
            ? new LinkedHashSet <String> ( conditionSetDraft.getConditionIds () )
            : conditionSetDraft.getBindings ().values ().stream ()
                .filter ( binding -> binding.getState () != BindingState.OMITTED )
                .map ( BindingDraft::getConditionIdentifier )
                .collect ( java.util.stream.Collectors.toCollection ( LinkedHashSet::new ) );
        ArrayList <ConditionSelectionDescriptor> descriptors =
            new ArrayList <ConditionSelectionDescriptor> ();

        // Process each condition supplied by model conditions.

        for ( AuthoringModel.ConditionDefinition condition : model.getConditions () )
        {
            // Initialize the parameter by applying get and get parameter ID.

            AuthoringModel.ParameterDefinition parameter = parameters.get ( condition.getParameterId () );

            // Add new condition selection descriptor condition ID condition name describe condit to the descriptors.

            descriptors.add (
                new ConditionSelectionDescriptor (
                    condition.getId (),
                    condition.getName (),
                    describeCondition ( condition, parameter ),
                    selectedConditionIds.contains ( condition.getId () ),
                    condition.hasPredicate ()
                )
            );
        }

        // Return an immutable copy of descriptors.

        return List.copyOf ( descriptors );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getConditionParameterDescriptor
    //
    // Description:
    //
    //   Returns the condition parameter descriptor.
    //
    // Arguments:
    //
    //   content (ClientDocumentSession.Content):
    //     The content to use.
    //
    //   parameterIdentifier (String):
    //     The parameter identifier to use.
    //
    // Returns:
    //
    //   The condition parameter descriptor.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Optional <ConditionParameterDescriptor> getConditionParameterDescriptor (
        ClientDocumentSession.Content content,
        String parameterIdentifier
    )
    {
        // Stop this path and return its result when parameter identifier is unavailable.

        if ( parameterIdentifier == null )
        {
            // Return an empty optional result because no value is available when parameter identifier is unavailable.

            return Optional.empty ();
        }

        // Return the result produced by find first.

        return content.getAuthoringModel ().getParameters ().stream ()
            .filter ( parameter -> parameter.getId ().equals ( parameterIdentifier ) )
            .map (
                parameter -> new ConditionParameterDescriptor (
                    parameter.getId (),
                    parameter.getType (),
                    parameter.getEnumerationValues ()
                )
            )
            .findFirst ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getSupportedConditionOperators
    //
    // Description:
    //
    //   Returns the supported condition operators.
    //
    // Arguments:
    //
    //   parameter (ConditionParameterDescriptor):
    //     The parameter to use.
    //
    // Returns:
    //
    //   The supported condition operators.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public List <ConditionOperatorDraft> getSupportedConditionOperators (
        ConditionParameterDescriptor parameter
    )
    {
        // Stop this path and return its result when parameter is unavailable.

        if ( parameter == null )
        {
            // Return the result produced by of when parameter is unavailable.

            return List.of (
                ConditionOperatorDraft.EQUALS,
                ConditionOperatorDraft.NOT_EQUALS,
                ConditionOperatorDraft.ANY
            );
        }

        // Return the result produced by to list.

        return java.util.Arrays.stream ( ConditionOperatorDraft.values () )
            .filter ( operator -> operator.supports ( parameter.getParameterType () ) )
            .toList ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getDefaultConditionValue
    //
    // Description:
    //
    //   Returns the default condition value.
    //
    // Arguments:
    //
    //   parameter (ConditionParameterDescriptor):
    //     The parameter to use.
    //
    // Returns:
    //
    //   The default condition value.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String getDefaultConditionValue ( ConditionParameterDescriptor parameter )
    {
        // Stop this path and return its result when parameter is unavailable.

        if ( parameter == null )
        {
            // Return the get default condition value text to the caller when parameter is unavailable.

            return "";
        }

        // Return the result produced by default condition value.

        return defaultConditionValue (
            parameter.getParameterType (),
            parameter.getEnumerationValues ()
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getSpecificity
    //
    // Description:
    //
    //   Returns the specificity.
    //
    // Arguments:
    //
    //   bindings (Collection <BindingDraft>):
    //     The bindings to use.
    //
    // Returns:
    //
    //   The specificity.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public int getSpecificity ( Collection <BindingDraft> bindings )
    {
        int specificity = 0;

        // Process each binding supplied by bindings.

        for ( BindingDraft binding : bindings )
        {
            // Complete the get specificity step by calling get state.

            specificity += switch ( binding.getState () )
            {
                // Handle concrete through this switch branch.

                case CONCRETE -> 2;

                // Handle wildcard through this switch branch.

                case WILDCARD -> 1;

                // Handle omitted through this switch branch.

                case OMITTED  -> 0;
            };
        }

        // Return the specificity to the caller.

        return specificity;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: describeEntity
    //
    // Description:
    //
    //   Performs the describe entity operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   entity (AuthoringModel.EntityDefinition):
    //     The entity to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String describeEntity (
        AuthoringModel model,
        String categoryIdentifier,
        AuthoringModel.EntityDefinition entity
    )
    {
        // Handle the branch where entity is an authoring model parameter definition.

        if ( entity instanceof AuthoringModel.ParameterDefinition )
        {
            AuthoringModel.ParameterDefinition parameter = (AuthoringModel.ParameterDefinition) entity;

            // Return the composed describe entity value when entity is an authoring model parameter definition.

            return parameter.getType ()
                + (
                    parameter.getEnumerationValues ().isEmpty ()
                        ? ""
                        : " [" + String.join ( ", ", parameter.getEnumerationValues () ) + "]"
                );
        }

        // Handle the branch where entity is an authoring model payload definition.

        if ( entity instanceof AuthoringModel.PayloadDefinition )
        {
            AuthoringModel.PayloadDefinition payload = (AuthoringModel.PayloadDefinition) entity;

            // Return the completed textual representation when entity is an authoring model payload definition.

            return Integer.toString ( payload.getParameterIds ().size () );
        }

        // Stop this path and return its result when entity is an authoring model event definition.

        if ( entity instanceof AuthoringModel.EventDefinition )
        {
            // Return the result produced by get payload ID when entity is an authoring model event definition.

            return ((AuthoringModel.EventDefinition) entity).getPayloadId ();
        }

        // Handle the branch where entity is an authoring model condition definition.

        if ( entity instanceof AuthoringModel.ConditionDefinition )
        {
            AuthoringModel.ConditionDefinition condition =
                (AuthoringModel.ConditionDefinition) entity;

            // Return the result produced by describe condition when entity is an authoring model condition definition.

            return describeCondition (
                condition,
                indexParameters ( model ).get ( condition.getParameterId () )
            );
        }

        // Handle the branch where entity is an authoring model condition set definition.

        if ( entity instanceof AuthoringModel.ConditionSetDefinition )
        {
            AuthoringModel.ConditionSetDefinition conditionSet =
                (AuthoringModel.ConditionSetDefinition) entity;

            // Initialize the condition count by applying uses predefined conditions, size, get condition IDs, and get
            // bindings.

            int conditionCount = conditionSet.usesPredefinedConditions ()
                ? conditionSet.getConditionIds ().size ()
                : conditionSet.getBindings ().size ();

            // Return the completed textual representation when entity is an authoring model condition set definition.

            return Integer.toString ( conditionCount );
        }

        // Handle the branch where entity is an authoring model rule definition.

        if ( entity instanceof AuthoringModel.RuleDefinition )
        {
            AuthoringModel.RuleDefinition rule = (AuthoringModel.RuleDefinition) entity;

            // Return the composed describe entity value when entity is an authoring model rule definition.

            return rule.getEventId () + " / " + rule.getConditionSetId () + " / " + rule.getActionId ();
        }

        // Return the describe entity text to the caller.

        return "";
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: describeCondition
    //
    // Description:
    //
    //   Performs the describe condition operation.
    //
    // Arguments:
    //
    //   condition (AuthoringModel.ConditionDefinition):
    //     The condition to use.
    //
    //   parameter (AuthoringModel.ParameterDefinition):
    //     The parameter to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String describeCondition (
        AuthoringModel.ConditionDefinition condition,
        AuthoringModel.ParameterDefinition parameter
    )
    {
        // Stop this path and return its result when condition has not predicate.

        if ( !condition.hasPredicate () )
        {
            // Return the composed describe condition value when condition has not predicate.

            return condition.getParameterId () + " (legacy set value)";
        }

        ConditionOperator operator;

        try
        {
            // Update the operator from the get operator result.

            operator = ConditionOperator.valueOf ( condition.getOperator () );
        }

        // Handle illegal argument failures captured as exception.

        catch ( IllegalArgumentException exception )
        {
            // Return the composed describe condition value.

            return condition.getParameterId () + " " + condition.getOperator ();
        }

        // Prepare the type and expression values needed by the describe condition operation.

        String type = parameter == null ? "" : " [" + parameter.getType () + "]";

        // Select the result branch for operator.

        String expression = switch ( operator )
        {
            // Handle equals through this switch branch.

            case EQUALS                -> "= " + displayValue ( condition.getFirstValue () );

            // Handle not equals through this switch branch.

            case NOT_EQUALS            -> "\u2260 " + displayValue ( condition.getFirstValue () );

            // Handle greater than through this switch branch.

            case GREATER_THAN          -> "> " + displayValue ( condition.getFirstValue () );

            // Handle greater than or equal through this switch branch.

            case GREATER_THAN_OR_EQUAL -> "\u2265 " + displayValue ( condition.getFirstValue () );

            // Handle less than through this switch branch.

            case LESS_THAN             -> "< " + displayValue ( condition.getFirstValue () );

            // Handle less than or equal through this switch branch.

            case LESS_THAN_OR_EQUAL    -> "\u2264 " + displayValue ( condition.getFirstValue () );

            // Handle between exclusive through this switch branch.

            case BETWEEN_EXCLUSIVE     ->
                "> " + displayValue ( condition.getFirstValue () )
                    + " and < " + displayValue ( condition.getSecondValue () );

            // Handle between inclusive through this switch branch.

            case BETWEEN_INCLUSIVE     ->
                "\u2265 " + displayValue ( condition.getFirstValue () )
                    + " and \u2264 " + displayValue ( condition.getSecondValue () );

            // Handle any through this switch branch.

            case ANY -> "is any value";
        };

        // Return the composed describe condition value.

        return condition.getParameterId () + " " + expression + type;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validateCommonDraft
    //
    // Description:
    //
    //   Performs the validate common draft operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   originalEntityIdentifier (String):
    //     The original entity identifier to use.
    //
    //   draft (EntityDraft):
    //     The draft to use.
    //
    //   errors (Collection <FieldError>):
    //     The errors to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void validateCommonDraft (
        AuthoringModel model,
        String categoryIdentifier,
        String originalEntityIdentifier,
        EntityDraft draft,
        Collection <FieldError> errors
    )
    {
        // Handle the branch where identifier pattern matcher draft ID does not match the supplied values.

        if ( !IDENTIFIER_PATTERN.matcher ( draft.getId () ).matches () )
        {
            // Add new field error "id" "use lowercase words and digits separated by single hyph to the errors.

            errors.add (
                new FieldError (
                    "id",
                    "Use lowercase words and digits separated by single hyphens."
                )
            );
        }

        // Initialize the duplicate identifier by applying any match, stream, get entities, equals, and get ID.

        boolean duplicateIdentifier = getEntities ( model, categoryIdentifier ).stream ()
            .anyMatch (
                entity ->
                    entity.getId ().equals ( draft.getId () )
                        && !entity.getId ().equals ( originalEntityIdentifier )
            );

        // Handle the branch where duplicate identifier is true.

        if ( duplicateIdentifier )
        {
            // Add new field error "id" "choose a unique identifier in this category " to the errors.

            errors.add ( new FieldError ( "id", "Choose a unique identifier in this category." ) );
        }

        // Handle the branch where draft name trim contains no values.

        if ( draft.getName ().trim ().isEmpty () )
        {
            // Add new field error "name" "enter a nonblank display name " to the errors.

            errors.add ( new FieldError ( "name", "Enter a nonblank display name." ) );
        }

        // Handle the branch where draft description trim contains no values.

        if ( draft.getDescription ().trim ().isEmpty () )
        {
            // Add new field error "description" "enter a nonblank description " to the errors.

            errors.add ( new FieldError ( "description", "Enter a nonblank description." ) );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createEntity
    //
    // Description:
    //
    //   Performs the create entity operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   draft (EntityDraft):
    //     The draft to use.
    //
    //   errors (Collection <FieldError>):
    //     The errors to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private AuthoringModel.EntityDefinition createEntity (
        AuthoringModel model,
        String categoryIdentifier,
        EntityDraft draft,
        Collection <FieldError> errors
    )
    {
        // Return the create entity result to the caller.

        return switch ( categoryIdentifier )
        {
            // Handle parameters through this switch branch.

            case PARAMETERS     -> createParameter ( draft, errors );

            // Handle payloads through this switch branch.

            case PAYLOADS       -> createPayload ( model, draft, errors );

            // Handle events through this switch branch.

            case EVENTS         -> createEvent ( model, draft, errors );

            // Handle conditions through this switch branch.

            case CONDITIONS     -> createCondition ( model, draft, errors );

            // Handle condition sets through this switch branch.

            case CONDITION_SETS -> createConditionSet ( model, draft, errors );

            // Handle actions through this switch branch.

            case ACTIONS        -> new AuthoringModel.ActionDefinition (
                draft.getId (),
                draft.getName (),
                draft.getDescription ()
            );

            // Handle rules through this switch branch.

            case RULES          -> createRule ( model, draft, errors );

            // Handle the default case through this switch branch.

            default             -> throw new IllegalStateException ( "Unreachable category." );
        };
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createParameter
    //
    // Description:
    //
    //   Performs the create parameter operation.
    //
    // Arguments:
    //
    //   draft (EntityDraft):
    //     The draft to use.
    //
    //   errors (Collection <FieldError>):
    //     The errors to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private AuthoringModel.ParameterDefinition createParameter (
        EntityDraft draft,
        Collection <FieldError> errors
    )
    {
        ParameterType parameterType = null;

        try
        {
            // Update the parameter type from the get parameter type result.

            parameterType = ParameterType.valueOf ( draft.getParameterType () );
        }

        // Handle illegal argument or null pointer failures captured as exception.

        catch ( IllegalArgumentException | NullPointerException exception )
        {
            // Add new field error "type" "choose string integer boolean or enum " to the errors.

            errors.add ( new FieldError ( "type", "Choose STRING, INTEGER, BOOLEAN, or ENUM." ) );
        }

        // Initialize the enumeration values by applying get enumeration values.

        List <String> enumerationValues = draft.getEnumerationValues ();

        // Handle the branch where parameter type equals parameter type enum.

        if ( parameterType == ParameterType.ENUM )
        {
            // Prepare the unique values and invalid value values needed by the create parameter operation.

            LinkedHashSet <String> uniqueValues = new LinkedHashSet <String> ();
            boolean invalidValue = enumerationValues.isEmpty ();

            // Process each enumeration value supplied by enumeration values.

            for ( String enumerationValue : enumerationValues )
            {
                // Update the invalid value from the add result.

                invalidValue = invalidValue
                    || enumerationValue.trim ().isEmpty ()
                    || !uniqueValues.add ( enumerationValue );
            }

            // Handle the branch where invalid value is true.

            if ( invalidValue )
            {
                // Add new field error "enum values" "enter at least one unique nonblank enumeration to the errors.

                errors.add (
                    new FieldError (
                        "enumValues",
                        "Enter at least one unique, nonblank enumeration value."
                    )
                );
            }
        }

        // Handle the alternative where enumeration values contains values.

        else if ( !enumerationValues.isEmpty () )
        {
            // Add new field error "enum values" "enumeration values are available only for the e to the errors.

            errors.add (
                new FieldError (
                    "enumValues",
                    "Enumeration values are available only for the ENUM type."
                )
            );
        }

        // Return a newly constructed authoring model parameter definition containing the operation result.

        return new AuthoringModel.ParameterDefinition (
            draft.getId (),
            draft.getName (),
            draft.getDescription (),
            draft.getParameterType (),
            enumerationValues
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createPayload
    //
    // Description:
    //
    //   Performs the create payload operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   draft (EntityDraft):
    //     The draft to use.
    //
    //   errors (Collection <FieldError>):
    //     The errors to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private AuthoringModel.PayloadDefinition createPayload (
        AuthoringModel model,
        EntityDraft draft,
        Collection <FieldError> errors
    )
    {
        // Perform the validate references, get parameter IDs, identifiers, and get parameters calls required by the
        // create payload operation.

        validateReferences (
            draft.getParameterIds (),
            identifiers ( model.getParameters () ),
            "parameterIds",
            "Choose only defined parameters.",
            errors
        );

        // Handle the branch where new linked hash set string draft parameter IDs size differs from draft parameter IDs
        // size.

        if ( new LinkedHashSet <String> ( draft.getParameterIds () ).size () != draft.getParameterIds ().size () )
        {
            // Add new field error "parameter ids" "choose each parameter at most once " to the errors.

            errors.add ( new FieldError ( "parameterIds", "Choose each parameter at most once." ) );
        }

        // Return a newly constructed authoring model payload definition containing the operation result.

        return new AuthoringModel.PayloadDefinition (
            draft.getId (),
            draft.getName (),
            draft.getDescription (),
            draft.getParameterIds ()
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createEvent
    //
    // Description:
    //
    //   Performs the create event operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   draft (EntityDraft):
    //     The draft to use.
    //
    //   errors (Collection <FieldError>):
    //     The errors to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private AuthoringModel.EventDefinition createEvent (
        AuthoringModel model,
        EntityDraft draft,
        Collection <FieldError> errors
    )
    {
        // Perform the validate required reference, get payload ID, identifiers, and get payloads calls required by the
        // create event operation.

        validateRequiredReference (
            draft.getPayloadId (),
            identifiers ( model.getPayloads () ),
            "payloadId",
            "Choose a defined payload.",
            errors
        );

        // Return a newly constructed authoring model event definition containing the operation result.

        return new AuthoringModel.EventDefinition (
            draft.getId (),
            draft.getName (),
            draft.getDescription (),
            draft.getPayloadId ()
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createCondition
    //
    // Description:
    //
    //   Performs the create condition operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   draft (EntityDraft):
    //     The draft to use.
    //
    //   errors (Collection <FieldError>):
    //     The errors to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private AuthoringModel.ConditionDefinition createCondition (
        AuthoringModel model,
        EntityDraft draft,
        Collection <FieldError> errors
    )
    {
        // Perform the validate required reference, get parameter ID, identifiers, and get parameters calls required by
        // the create condition operation.

        validateRequiredReference (
            draft.getParameterId (),
            identifiers ( model.getParameters () ),
            "parameterId",
            "Choose a defined parameter.",
            errors
        );

        // Initialize the parameter by applying get, index parameters, and get parameter ID.

        AuthoringModel.ParameterDefinition parameter = indexParameters ( model ).get (
            draft.getParameterId ()
        );
        ConditionOperator operator = null;

        try
        {
            // Update the operator from the get condition operator result.

            operator = ConditionOperator.valueOf ( draft.getConditionOperator () );
        }

        // Handle illegal argument or null pointer failures captured as exception.

        catch ( IllegalArgumentException | NullPointerException exception )
        {
            // Add new field error "operator" "choose a supported condition operator " to the errors.

            errors.add ( new FieldError ( "operator", "Choose a supported condition operator." ) );
        }

        ParameterType parameterType = null;

        // Handle the branch where parameter is available.

        if ( parameter != null )
        {
            try
            {
                // Update the parameter type from the get type result.

                parameterType = ParameterType.valueOf ( parameter.getType () );
            }

            // Handle illegal argument failures captured as exception.

            catch ( IllegalArgumentException exception )
            {
                // Add new field error "parameter id" "repair the parameter type first " to the errors.

                errors.add ( new FieldError ( "parameterId", "Repair the parameter type first." ) );
            }
        }

        // Handle the branch where operator is available and parameter type is available and operator supports does not
        // succeed.

        if (
            operator != null
                && parameterType != null
                && !operator.supports ( parameterType )
        )
        {
            // Add new field error "operator" "inequality and range operators require an integer to the errors.

            errors.add (
                new FieldError (
                    "operator",
                    "Inequality and range operators require an INTEGER parameter."
                )
            );
        }

        Object firstValue = null;
        Object secondValue = null;

        // Handle the branch where operator is available and parameter is available and operator operand count is at
        // least 1.

        if ( operator != null && parameter != null && operator.getOperandCount () >= 1 )
        {
            // Update the first value from the get condition value text result.

            firstValue = parseConditionValue (
                draft.getConditionValueText (),
                parameter,
                "value",
                errors
            ).orElse ( null );
        }

        // Handle the branch where operator is available and parameter is available and operator operand count equals
        // 2.

        if ( operator != null && parameter != null && operator.getOperandCount () == 2 )
        {
            // Update the second value from the get second condition value text result.

            secondValue = parseConditionValue (
                draft.getSecondConditionValueText (),
                parameter,
                "secondValue",
                errors
            ).orElse ( null );

            // Handle the branch where first value is a long and second value is a long and long first value compare to
            // long second value is at least 0.

            if (
                firstValue instanceof Long
                    && secondValue instanceof Long
                    && ((Long) firstValue).compareTo ( (Long) secondValue ) >= 0
            )
            {
                // Add new field error "second value" "enter an upper value greater than the lower va to the errors.

                errors.add (
                    new FieldError (
                        "secondValue",
                        "Enter an upper value greater than the lower value."
                    )
                );
            }
        }

        // Return a newly constructed authoring model condition definition containing the operation result.

        return new AuthoringModel.ConditionDefinition (
            draft.getId (),
            draft.getName (),
            draft.getDescription (),
            draft.getParameterId (),
            draft.getConditionOperator (),
            firstValue,
            secondValue
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createConditionSet
    //
    // Description:
    //
    //   Performs the create condition set operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   draft (EntityDraft):
    //     The draft to use.
    //
    //   errors (Collection <FieldError>):
    //     The errors to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private AuthoringModel.ConditionSetDefinition createConditionSet (
        AuthoringModel model,
        EntityDraft draft,
        Collection <FieldError> errors
    )
    {
        // Handle the branch where draft uses predefined conditions succeeds.

        if ( draft.usesPredefinedConditions () )
        {
            // Perform the validate references, get condition IDs, identifiers, and get conditions calls required by
            // the create condition set operation.

            validateReferences (
                draft.getConditionIds (),
                identifiers ( model.getConditions () ),
                "conditionIds",
                "Choose only defined conditions.",
                errors
            );

            // Handle the branch where new linked hash set string draft condition IDs size differs from draft condition
            // IDs size.

            if (
                new LinkedHashSet <String> ( draft.getConditionIds () ).size ()
                    != draft.getConditionIds ().size ()
            )
            {
                // Add new field error "condition ids" "choose each condition at most once " to the errors.

                errors.add (
                    new FieldError (
                        "conditionIds",
                        "Choose each condition at most once."
                    )
                );
            }

            // Prepare the conditions by identifier and parameter IDs values needed by the create condition set
            // operation.

            Map <String, AuthoringModel.ConditionDefinition> conditionsByIdentifier =
                indexConditions ( model );
            LinkedHashSet <String> parameterIds = new LinkedHashSet <String> ();

            // Process each condition identifier supplied by draft condition IDs.

            for ( String conditionIdentifier : draft.getConditionIds () )
            {
                // Initialize the condition by applying get.

                AuthoringModel.ConditionDefinition condition = conditionsByIdentifier.get (
                    conditionIdentifier
                );

                // Skip the current item when condition is unavailable.

                if ( condition == null )
                {
                    continue;
                }

                // Handle the branch where condition has not predicate.

                if ( !condition.hasPredicate () )
                {
                    // Add new field error "condition IDs " + condition identifier "configure this conditi to the
                    // errors.

                    errors.add (
                        new FieldError (
                            "conditionIds." + conditionIdentifier,
                            "Configure this condition's operator and value before selecting it."
                        )
                    );
                }

                // Handle the branch where parameter IDs add does not succeed.

                if ( !parameterIds.add ( condition.getParameterId () ) )
                {
                    // Add new field error "condition IDs " + condition identifier "choose at most one con to the
                    // errors.

                    errors.add (
                        new FieldError (
                            "conditionIds." + conditionIdentifier,
                            "Choose at most one condition for each parameter."
                        )
                    );
                }
            }

            // Return a newly constructed authoring model condition set definition containing the operation result when
            // draft uses predefined conditions succeeds.

            return new AuthoringModel.ConditionSetDefinition (
                draft.getId (),
                draft.getName (),
                draft.getDescription (),
                draft.getConditionIds ()
            );
        }

        // Prepare the bindings and parameters by identifier values needed by the create condition set operation.

        LinkedHashMap <String, Object> bindings = new LinkedHashMap <String, Object> ();
        Map <String, AuthoringModel.ParameterDefinition> parametersByIdentifier = indexParameters ( model );

        // Process each condition supplied by model conditions.

        for ( AuthoringModel.ConditionDefinition condition : model.getConditions () )
        {
            // Initialize the binding by applying get or default, get bindings, get ID, and omitted.

            BindingDraft binding = draft.getBindings ().getOrDefault (
                condition.getId (),
                BindingDraft.omitted ( condition.getId () )
            );

            // Skip the current item when binding state equals binding state omitted.

            if ( binding.getState () == BindingState.OMITTED )
            {
                continue;
            }

            // Handle the branch where binding state equals binding state wildcard.

            if ( binding.getState () == BindingState.WILDCARD )
            {
                // Store null under condition ID in the bindings.

                bindings.put ( condition.getId (), null );
                continue;
            }

            // Initialize the parameter by applying get and get parameter ID.

            AuthoringModel.ParameterDefinition parameter = parametersByIdentifier.get (
                condition.getParameterId ()
            );

            // Handle the branch where parameter is unavailable.

            if ( parameter == null )
            {
                // Add new field error "bindings " + condition ID "repair the condition's unresolved to the errors.

                errors.add (
                    new FieldError (
                        "bindings." + condition.getId (),
                        "Repair the condition's unresolved parameter reference first."
                    )
                );
                continue;
            }

            // Perform the if present, parse concrete value, put, and get ID calls required by the create condition set
            // operation.

            parseConcreteValue ( binding, parameter, errors ).ifPresent (
                value -> bindings.put ( condition.getId (), value )
            );
        }

        // Process each condition identifier supplied by draft bindings key set.

        for ( String conditionIdentifier : draft.getBindings ().keySet () )
        {
            // Handle the branch where model conditions stream none match succeeds.

            if (
                model.getConditions ().stream ()
                    .noneMatch ( condition -> condition.getId ().equals ( conditionIdentifier ) )
            )
            {
                // Add new field error "bindings " + condition identifier "remove the binding for the to the errors.

                errors.add (
                    new FieldError (
                        "bindings." + conditionIdentifier,
                        "Remove the binding for the undefined condition."
                    )
                );
            }
        }

        // Return a newly constructed authoring model condition set definition containing the operation result.

        return new AuthoringModel.ConditionSetDefinition (
            draft.getId (),
            draft.getName (),
            draft.getDescription (),
            bindings
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: createRule
    //
    // Description:
    //
    //   Performs the create rule operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   draft (EntityDraft):
    //     The draft to use.
    //
    //   errors (Collection <FieldError>):
    //     The errors to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private AuthoringModel.RuleDefinition createRule (
        AuthoringModel model,
        EntityDraft draft,
        Collection <FieldError> errors
    )
    {
        // Perform the validate required reference, get event ID, identifiers, get events, get condition set ID, get
        // condition sets, get action ID, and get actions calls required by the create rule operation.

        validateRequiredReference (
            draft.getEventId (),
            identifiers ( model.getEvents () ),
            "eventId",
            "Choose a defined event.",
            errors
        );
        validateRequiredReference (
            draft.getConditionSetId (),
            identifiers ( model.getConditionSets () ),
            "conditionSetId",
            "Choose a defined condition set.",
            errors
        );
        validateRequiredReference (
            draft.getActionId (),
            identifiers ( model.getActions () ),
            "actionId",
            "Choose a defined action.",
            errors
        );

        // Return a newly constructed authoring model rule definition containing the operation result.

        return new AuthoringModel.RuleDefinition (
            draft.getId (),
            draft.getName (),
            draft.getDescription (),
            draft.getEventId (),
            draft.getConditionSetId (),
            draft.getActionId ()
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: parseConcreteValue
    //
    // Description:
    //
    //   Performs the parse concrete value operation.
    //
    // Arguments:
    //
    //   binding (BindingDraft):
    //     The binding to use.
    //
    //   parameter (AuthoringModel.ParameterDefinition):
    //     The parameter to use.
    //
    //   errors (Collection <FieldError>):
    //     The errors to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private Optional <Object> parseConcreteValue (
        BindingDraft binding,
        AuthoringModel.ParameterDefinition parameter,
        Collection <FieldError> errors
    )
    {
        // Initialize the field by applying get condition identifier.

        String field = "bindings." + binding.getConditionIdentifier ();

        // Return the result produced by parse condition value.

        return parseConditionValue ( binding.getConcreteText (), parameter, field, errors );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: parseConditionValue
    //
    // Description:
    //
    //   Performs the parse condition value operation.
    //
    // Arguments:
    //
    //   text (String):
    //     The text to use.
    //
    //   parameter (AuthoringModel.ParameterDefinition):
    //     The parameter to use.
    //
    //   field (String):
    //     The field to use.
    //
    //   errors (Collection <FieldError>):
    //     The errors to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private Optional <Object> parseConditionValue (
        String text,
        AuthoringModel.ParameterDefinition parameter,
        String field,
        Collection <FieldError> errors
    )
    {
        ParameterType parameterType;

        try
        {
            // Update the parameter type from the get type result.

            parameterType = ParameterType.valueOf ( parameter.getType () );
        }

        // Handle illegal argument failures captured as exception.

        catch ( IllegalArgumentException exception )
        {
            // Add new field error field "repair the parameter type first " to the errors.

            errors.add ( new FieldError ( field, "Repair the parameter type first." ) );

            // Return an empty optional result because no value is available.

            return Optional.empty ();
        }

        // Select the processing branch for parameter type.

        switch ( parameterType )
        {
            // Handle string through this switch branch.

            case STRING:

                // Return an optional containing the available value.

                return Optional.of ( text );

            // Handle integer through this switch branch.

            case INTEGER:
                try
                {
                    // Return an optional containing the available value.

                    return Optional.of ( Long.valueOf ( text.trim () ) );
                }

                // Handle number format failures captured as exception.

                catch ( NumberFormatException exception )
                {
                    // Add new field error field "enter an integer within the signed 64-bit range " to the errors.

                    errors.add (
                        new FieldError (
                            field,
                            "Enter an integer within the signed 64-bit range."
                        )
                    );

                    // Return an empty optional result because no value is available.

                    return Optional.empty ();
                }

            // Handle boolean through this switch branch.

            case BOOLEAN:

                // Handle the branch where "true" equals ignore case does not succeed and "false" equals ignore case
                // does not succeed.

                if (
                    !"true".equalsIgnoreCase ( text.trim () )
                        && !"false".equalsIgnoreCase ( text.trim () )
                )
                {
                    // Add new field error field "choose true or false " to the errors.

                    errors.add ( new FieldError ( field, "Choose true or false." ) );

                    // Return an empty optional result because no value is available when "true" equals ignore case
                    // does not succeed and "false" equals ignore case does not succeed.

                    return Optional.empty ();
                }

                // Return an optional containing the available value.

                return Optional.of ( Boolean.valueOf ( text.trim () ) );

            // Handle enum through this switch branch.

            case ENUM:

                // Handle the branch where parameter enumeration values does not contain text.

                if ( !parameter.getEnumerationValues ().contains ( text ) )
                {
                    // Add new field error field "choose one of: " + string join " " parameter enumera to the errors.

                    errors.add (
                        new FieldError (
                            field,
                            "Choose one of: " + String.join ( ", ", parameter.getEnumerationValues () ) + "."
                        )
                    );

                    // Return an empty optional result because no value is available when parameter enumeration values
                    // does not contain text.

                    return Optional.empty ();
                }

                // Return an optional containing the available value.

                return Optional.of ( text );

            // Handle the default case through this switch branch.

            default:
                throw new IllegalStateException ( "Unreachable parameter type." );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: replaceEntity
    //
    // Description:
    //
    //   Performs the replace entity operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   originalEntityIdentifier (String):
    //     The original entity identifier to use.
    //
    //   replacement (AuthoringModel.EntityDefinition):
    //     The replacement to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private AuthoringModel replaceEntity (
        AuthoringModel model,
        String categoryIdentifier,
        String originalEntityIdentifier,
        AuthoringModel.EntityDefinition replacement
    )
    {
        // Prepare the parameters, payloads, events, conditions, condition sets, actions, and rules values needed by
        // the replace entity operation.

        ArrayList <AuthoringModel.ParameterDefinition> parameters = new ArrayList <> ( model.getParameters () );
        ArrayList <AuthoringModel.PayloadDefinition> payloads = new ArrayList <> ( model.getPayloads () );
        ArrayList <AuthoringModel.EventDefinition> events = new ArrayList <> ( model.getEvents () );
        ArrayList <AuthoringModel.ConditionDefinition> conditions = new ArrayList <> ( model.getConditions () );
        ArrayList <AuthoringModel.ConditionSetDefinition> conditionSets =
            new ArrayList <> ( model.getConditionSets () );
        ArrayList <AuthoringModel.ActionDefinition> actions = new ArrayList <> ( model.getActions () );
        ArrayList <AuthoringModel.RuleDefinition> rules = new ArrayList <> ( model.getRules () );

        // Select the processing branch for category identifier.

        switch ( categoryIdentifier )
        {
            // Handle parameters through this switch branch.

            case PARAMETERS:
                replaceListEntity (
                    parameters,
                    originalEntityIdentifier,
                    (AuthoringModel.ParameterDefinition) replacement
                );
                break;

            // Handle payloads through this switch branch.

            case PAYLOADS:
                replaceListEntity (
                    payloads,
                    originalEntityIdentifier,
                    (AuthoringModel.PayloadDefinition) replacement
                );
                break;

            // Handle events through this switch branch.

            case EVENTS:
                replaceListEntity (
                    events,
                    originalEntityIdentifier,
                    (AuthoringModel.EventDefinition) replacement
                );
                break;

            // Handle conditions through this switch branch.

            case CONDITIONS:
                replaceListEntity (
                    conditions,
                    originalEntityIdentifier,
                    (AuthoringModel.ConditionDefinition) replacement
                );
                break;

            // Handle condition sets through this switch branch.

            case CONDITION_SETS:
                replaceListEntity (
                    conditionSets,
                    originalEntityIdentifier,
                    (AuthoringModel.ConditionSetDefinition) replacement
                );
                break;

            // Handle actions through this switch branch.

            case ACTIONS:
                replaceListEntity (
                    actions,
                    originalEntityIdentifier,
                    (AuthoringModel.ActionDefinition) replacement
                );
                break;

            // Handle rules through this switch branch.

            case RULES:
                replaceListEntity (
                    rules,
                    originalEntityIdentifier,
                    (AuthoringModel.RuleDefinition) replacement
                );
                break;

            // Handle the default case through this switch branch.

            default:
                throw new IllegalStateException ( "Unreachable category." );
        }

        // Return the result produced by copy model.

        return copyModel ( model, parameters, payloads, events, conditions, conditionSets, actions, rules );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: replaceListEntity
    //
    // Description:
    //
    //   Performs the replace list entity operation.
    //
    // Arguments:
    //
    //   entities (List <T>):
    //     The entities to use.
    //
    //   originalEntityIdentifier (String):
    //     The original entity identifier to use.
    //
    //   replacement (T):
    //     The replacement to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static <T extends AuthoringModel.EntityDefinition> void replaceListEntity (
        List <T> entities,
        String originalEntityIdentifier,
        T replacement
    )
    {
        // Handle the branch where original entity identifier is unavailable.

        if ( originalEntityIdentifier == null )
        {
            // Add objects require non null replacement "replacement" to the entities.

            entities.add ( Objects.requireNonNull ( replacement, "replacement" ) );

            return;
        }

        // Repeat the loop while i is less than entities size.

        for ( int i = 0; i < entities.size (); i++ )
        {
            // Handle the branch where entities get i ID matches original entity identifier.

            if ( entities.get ( i ).getId ().equals ( originalEntityIdentifier ) )
            {
                // Handle the branch where replacement is unavailable.

                if ( replacement == null )
                {
                    // Remove the selected value from the entities.

                    entities.remove ( i );
                }

                // Handle the alternative path when the preceding condition is not satisfied.

                else
                {
                    // Set the set on the entities.

                    entities.set ( i, replacement );
                }

                return;
            }
        }

        throw new IllegalArgumentException ( "Unknown entity: " + originalEntityIdentifier );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: renameReferences
    //
    // Description:
    //
    //   Performs the rename references operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   originalEntityIdentifier (String):
    //     The original entity identifier to use.
    //
    //   replacementEntityIdentifier (String):
    //     The replacement entity identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private AuthoringModel renameReferences (
        AuthoringModel model,
        String categoryIdentifier,
        String originalEntityIdentifier,
        String replacementEntityIdentifier
    )
    {
        // Prepare the payloads, events, conditions, condition sets, and rules values needed by the rename references
        // operation.

        ArrayList <AuthoringModel.PayloadDefinition> payloads = new ArrayList <AuthoringModel.PayloadDefinition> ();
        ArrayList <AuthoringModel.EventDefinition> events = new ArrayList <AuthoringModel.EventDefinition> ();
        ArrayList <AuthoringModel.ConditionDefinition> conditions =
            new ArrayList <AuthoringModel.ConditionDefinition> ();
        ArrayList <AuthoringModel.ConditionSetDefinition> conditionSets =
            new ArrayList <AuthoringModel.ConditionSetDefinition> ();
        ArrayList <AuthoringModel.RuleDefinition> rules = new ArrayList <AuthoringModel.RuleDefinition> ();

        // Process each payload supplied by model payloads.

        for ( AuthoringModel.PayloadDefinition payload : model.getPayloads () )
        {
            // Add new authoring model payload definition payload ID payload name payload descri to the payloads.

            payloads.add (
                new AuthoringModel.PayloadDefinition (
                    payload.getId (),
                    payload.getName (),
                    payload.getDescription (),
                    replaceReferences (
                        payload.getParameterIds (),
                        categoryIdentifier.equals ( PARAMETERS ) ? originalEntityIdentifier : "",
                        replacementEntityIdentifier
                    )
                )
            );
        }

        // Process each event supplied by model events.

        for ( AuthoringModel.EventDefinition event : model.getEvents () )
        {
            // Add new authoring model event definition event ID event name event description r to the events.

            events.add (
                new AuthoringModel.EventDefinition (
                    event.getId (),
                    event.getName (),
                    event.getDescription (),
                    replaceReference (
                        event.getPayloadId (),
                        categoryIdentifier.equals ( PAYLOADS ) ? originalEntityIdentifier : "",
                        replacementEntityIdentifier
                    )
                )
            );
        }

        // Process each condition supplied by model conditions.

        for ( AuthoringModel.ConditionDefinition condition : model.getConditions () )
        {
            // Add new authoring model condition definition condition ID condition name conditio to the conditions.

            conditions.add (
                new AuthoringModel.ConditionDefinition (
                    condition.getId (),
                    condition.getName (),
                    condition.getDescription (),
                    replaceReference (
                        condition.getParameterId (),
                        categoryIdentifier.equals ( PARAMETERS ) ? originalEntityIdentifier : "",
                        replacementEntityIdentifier
                    ),
                    condition.getOperator (),
                    condition.getFirstValue (),
                    condition.getSecondValue ()
                )
            );
        }

        // Process each condition set supplied by model condition sets.

        for ( AuthoringModel.ConditionSetDefinition conditionSet : model.getConditionSets () )
        {
            // Handle the branch where condition set uses predefined conditions succeeds.

            if ( conditionSet.usesPredefinedConditions () )
            {
                // Add new authoring model condition set definition condition set ID condition set name to the
                // condition sets.

                conditionSets.add (
                    new AuthoringModel.ConditionSetDefinition (
                        conditionSet.getId (),
                        conditionSet.getName (),
                        conditionSet.getDescription (),
                        replaceReferences (
                            conditionSet.getConditionIds (),
                            categoryIdentifier.equals ( CONDITIONS ) ? originalEntityIdentifier : "",
                            replacementEntityIdentifier
                        )
                    )
                );

                continue;
            }

            // Initialize the bindings by applying get bindings.

            LinkedHashMap <String, Object> bindings =
                new LinkedHashMap <String, Object> ( conditionSet.getBindings () );

            // Handle the branch where category identifier matches conditions and bindings contains key succeeds.

            if ( categoryIdentifier.equals ( CONDITIONS ) && bindings.containsKey ( originalEntityIdentifier ) )
            {
                // Initialize the value by applying remove.

                Object value = bindings.remove ( originalEntityIdentifier );

                // Store value under replacement entity identifier in the bindings.

                bindings.put ( replacementEntityIdentifier, value );
            }

            // Add new authoring model condition set definition condition set ID condition set name to the condition
            // sets.

            conditionSets.add (
                new AuthoringModel.ConditionSetDefinition (
                    conditionSet.getId (),
                    conditionSet.getName (),
                    conditionSet.getDescription (),
                    bindings
                )
            );
        }

        // Process each rule supplied by model rules.

        for ( AuthoringModel.RuleDefinition rule : model.getRules () )
        {
            // Add new authoring model rule definition rule ID rule name rule description repla to the rules.

            rules.add (
                new AuthoringModel.RuleDefinition (
                    rule.getId (),
                    rule.getName (),
                    rule.getDescription (),
                    replaceReference (
                        rule.getEventId (),
                        categoryIdentifier.equals ( EVENTS ) ? originalEntityIdentifier : "",
                        replacementEntityIdentifier
                    ),
                    replaceReference (
                        rule.getConditionSetId (),
                        categoryIdentifier.equals ( CONDITION_SETS ) ? originalEntityIdentifier : "",
                        replacementEntityIdentifier
                    ),
                    replaceReference (
                        rule.getActionId (),
                        categoryIdentifier.equals ( ACTIONS ) ? originalEntityIdentifier : "",
                        replacementEntityIdentifier
                    )
                )
            );
        }

        // Return the result produced by copy model.

        return copyModel (
            model,
            model.getParameters (),
            payloads,
            events,
            conditions,
            conditionSets,
            model.getActions (),
            rules
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: findReferences
    //
    // Description:
    //
    //   Performs the find references operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   entityIdentifier (String):
    //     The entity identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private List <String> findReferences (
        AuthoringModel model,
        String categoryIdentifier,
        String entityIdentifier
    )
    {
        // Initialize the references with a new array list.

        ArrayList <String> references = new ArrayList <String> ();

        // Handle the branch where category identifier matches parameters.

        if ( categoryIdentifier.equals ( PARAMETERS ) )
        {
            // Perform the for each, filter, stream, get payloads, contains, get parameter IDs, add, get ID, get
            // conditions, equals, and get parameter ID calls required by the find references operation.

            model.getPayloads ().stream ()
                .filter ( payload -> payload.getParameterIds ().contains ( entityIdentifier ) )
                .forEach ( payload -> references.add ( "payload " + payload.getId () + ".parameterIds" ) );
            model.getConditions ().stream ()
                .filter ( condition -> condition.getParameterId ().equals ( entityIdentifier ) )
                .forEach ( condition -> references.add ( "condition " + condition.getId () + ".parameterId" ) );
        }

        // Handle the alternative where category identifier matches payloads.

        else if ( categoryIdentifier.equals ( PAYLOADS ) )
        {
            // Perform the for each, filter, stream, get events, equals, get payload ID, add, and get ID calls required
            // by the find references operation.

            model.getEvents ().stream ()
                .filter ( event -> event.getPayloadId ().equals ( entityIdentifier ) )
                .forEach ( event -> references.add ( "event " + event.getId () + ".payloadId" ) );
        }

        // Handle the alternative where category identifier matches events.

        else if ( categoryIdentifier.equals ( EVENTS ) )
        {
            // Perform the for each, filter, stream, get rules, equals, get event ID, add, and get ID calls required by
            // the find references operation.

            model.getRules ().stream ()
                .filter ( rule -> rule.getEventId ().equals ( entityIdentifier ) )
                .forEach ( rule -> references.add ( "rule " + rule.getId () + ".eventId" ) );
        }

        // Handle the alternative where category identifier matches conditions.

        else if ( categoryIdentifier.equals ( CONDITIONS ) )
        {
            // Perform the for each, filter, stream, get condition sets, uses predefined conditions, contains, get
            // condition IDs, contains key, get bindings, add, and get ID calls required by the find references
            // operation.

            model.getConditionSets ().stream ()
                .filter (
                    conditionSet ->
                        conditionSet.usesPredefinedConditions ()
                            ? conditionSet.getConditionIds ().contains ( entityIdentifier )
                            : conditionSet.getBindings ().containsKey ( entityIdentifier )
                )
                .forEach (
                    conditionSet ->
                        references.add (
                            "condition set "
                                + conditionSet.getId ()
                                + (
                                    conditionSet.usesPredefinedConditions ()
                                        ? ".conditionIds"
                                        : ".bindings"
                                )
                        )
                );
        }

        // Handle the alternative where category identifier matches condition sets.

        else if ( categoryIdentifier.equals ( CONDITION_SETS ) )
        {
            // Perform the for each, filter, stream, get rules, equals, get condition set ID, add, and get ID calls
            // required by the find references operation.

            model.getRules ().stream ()
                .filter ( rule -> rule.getConditionSetId ().equals ( entityIdentifier ) )
                .forEach ( rule -> references.add ( "rule " + rule.getId () + ".conditionSetId" ) );
        }

        // Handle the alternative where category identifier matches actions.

        else if ( categoryIdentifier.equals ( ACTIONS ) )
        {
            // Perform the for each, filter, stream, get rules, equals, get action ID, add, and get ID calls required
            // by the find references operation.

            model.getRules ().stream ()
                .filter ( rule -> rule.getActionId ().equals ( entityIdentifier ) )
                .forEach ( rule -> references.add ( "rule " + rule.getId () + ".actionId" ) );
        }

        // Return an immutable copy of references.

        return List.copyOf ( references );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: bindingDrafts
    //
    // Description:
    //
    //   Performs the binding drafts operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   conditionSet (AuthoringModel.ConditionSetDefinition):
    //     The condition set to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Map <String, BindingDraft> bindingDrafts (
        AuthoringModel model,
        AuthoringModel.ConditionSetDefinition conditionSet
    )
    {
        // Initialize the bindings with a new linked hash map.

        LinkedHashMap <String, BindingDraft> bindings = new LinkedHashMap <String, BindingDraft> ();

        // Process each condition supplied by model conditions.

        for ( AuthoringModel.ConditionDefinition condition : model.getConditions () )
        {
            // Handle the branch where condition set bindings contains key does not succeed.

            if ( !conditionSet.getBindings ().containsKey ( condition.getId () ) )
            {
                // Store binding draft omitted condition ID under condition ID in the bindings.

                bindings.put ( condition.getId (), BindingDraft.omitted ( condition.getId () ) );
            }

            // Handle the alternative where condition set bindings get condition ID is unavailable.

            else if ( conditionSet.getBindings ().get ( condition.getId () ) == null )
            {
                // Store binding draft wildcard condition ID under condition ID in the bindings.

                bindings.put ( condition.getId (), BindingDraft.wildcard ( condition.getId () ) );
            }

            // Handle the alternative path when the preceding condition is not satisfied.

            else
            {
                // Store binding draft concrete condition ID string value of condition set bindings get under condition
                // ID in the bindings.

                bindings.put (
                    condition.getId (),
                    BindingDraft.concrete (
                        condition.getId (),
                        String.valueOf ( conditionSet.getBindings ().get ( condition.getId () ) )
                    )
                );
            }
        }

        // Return an immutable copy of bindings.

        return Collections.unmodifiableMap ( bindings );
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

    private static Map <String, BindingDraft> omittedBindings ( AuthoringModel model )
    {
        // Initialize the bindings with a new linked hash map.

        LinkedHashMap <String, BindingDraft> bindings = new LinkedHashMap <String, BindingDraft> ();

        // Process each condition supplied by model conditions.

        for ( AuthoringModel.ConditionDefinition condition : model.getConditions () )
        {
            // Store binding draft omitted condition ID under condition ID in the bindings.

            bindings.put ( condition.getId (), BindingDraft.omitted ( condition.getId () ) );
        }

        // Return an immutable copy of bindings.

        return Collections.unmodifiableMap ( bindings );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: indexParameters
    //
    // Description:
    //
    //   Performs the index parameters operation.
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

    private static Map <String, AuthoringModel.ParameterDefinition> indexParameters ( AuthoringModel model )
    {
        // Initialize the parameters with a new linked hash map.

        LinkedHashMap <String, AuthoringModel.ParameterDefinition> parameters =
            new LinkedHashMap <String, AuthoringModel.ParameterDefinition> ();

        // Process each parameter supplied by model parameters.

        for ( AuthoringModel.ParameterDefinition parameter : model.getParameters () )
        {
            // Store parameter under parameter ID in the parameters.

            parameters.put ( parameter.getId (), parameter );
        }

        // Return the parameters to the caller.

        return parameters;
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
    // Method: defaultConditionValue
    //
    // Description:
    //
    //   Performs the default condition value operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   parameterIdentifier (String):
    //     The parameter identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String defaultConditionValue ( AuthoringModel model, String parameterIdentifier )
    {
        // Initialize the parameter by applying get and index parameters.

        AuthoringModel.ParameterDefinition parameter = indexParameters ( model ).get ( parameterIdentifier );

        // Stop this path and return its result when parameter is unavailable.

        if ( parameter == null )
        {
            // Return the default condition value text to the caller when parameter is unavailable.

            return "";
        }

        // Return the result produced by default condition value.

        return defaultConditionValue ( parameter.getType (), parameter.getEnumerationValues () );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: defaultConditionValue
    //
    // Description:
    //
    //   Performs the default condition value operation.
    //
    // Arguments:
    //
    //   parameterType (String):
    //     The parameter type to use.
    //
    //   enumerationValues (Collection <String>):
    //     The enumeration values to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String defaultConditionValue (
        String parameterType,
        Collection <String> enumerationValues
    )
    {
        // Return the default condition value result to the caller.

        return switch ( parameterType )
        {
            // Handle "integer" through this switch branch.

            case "INTEGER" -> "0";

            // Handle "boolean" through this switch branch.

            case "BOOLEAN" -> "false";

            // Handle "enum" through this switch branch.

            case "ENUM" -> enumerationValues.stream ().findFirst ().orElse ( "" );

            // Handle the default case through this switch branch.

            default -> "";
        };
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: displayValue
    //
    // Description:
    //
    //   Performs the display value operation.
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

    private static String displayValue ( Object value )
    {
        // Return the value selected according to value is unavailable.

        return value == null ? "" : String.valueOf ( value );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validateRequiredReference
    //
    // Description:
    //
    //   Performs the validate required reference operation.
    //
    // Arguments:
    //
    //   reference (String):
    //     The reference to use.
    //
    //   compatibleIdentifiers (Set <String>):
    //     The compatible identifiers to use.
    //
    //   field (String):
    //     The field to use.
    //
    //   message (String):
    //     The message to use.
    //
    //   errors (Collection <FieldError>):
    //     The errors to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void validateRequiredReference (
        String reference,
        Set <String> compatibleIdentifiers,
        String field,
        String message,
        Collection <FieldError> errors
    )
    {
        // Handle the branch where reference is unavailable or compatible identifiers does not contain reference.

        if ( reference == null || !compatibleIdentifiers.contains ( reference ) )
        {
            // Add new field error field message to the errors.

            errors.add ( new FieldError ( field, message ) );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validateReferences
    //
    // Description:
    //
    //   Performs the validate references operation.
    //
    // Arguments:
    //
    //   references (Collection <String>):
    //     The references to use.
    //
    //   compatibleIdentifiers (Set <String>):
    //     The compatible identifiers to use.
    //
    //   field (String):
    //     The field to use.
    //
    //   message (String):
    //     The message to use.
    //
    //   errors (Collection <FieldError>):
    //     The errors to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void validateReferences (
        Collection <String> references,
        Set <String> compatibleIdentifiers,
        String field,
        String message,
        Collection <FieldError> errors
    )
    {
        // Handle the branch where references stream any match succeeds.

        if ( references.stream ().anyMatch ( reference -> !compatibleIdentifiers.contains ( reference ) ) )
        {
            // Add new field error field message to the errors.

            errors.add ( new FieldError ( field, message ) );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: identifiers
    //
    // Description:
    //
    //   Performs the identifiers operation.
    //
    // Arguments:
    //
    //   entities (Collection <? extends AuthoringModel.EntityDefinition>):
    //     The entities to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Set <String> identifiers (
        Collection <? extends AuthoringModel.EntityDefinition> entities
    )
    {
        // Initialize the identifiers with a new linked hash set.

        LinkedHashSet <String> identifiers = new LinkedHashSet <String> ();

        // Process each entity supplied by entities.

        for ( AuthoringModel.EntityDefinition entity : entities )
        {
            // Add entity ID to the identifiers.

            identifiers.add ( entity.getId () );
        }

        // Return an immutable copy of identifiers.

        return Collections.unmodifiableSet ( identifiers );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: replaceReferences
    //
    // Description:
    //
    //   Performs the replace references operation.
    //
    // Arguments:
    //
    //   references (Collection <String>):
    //     The references to use.
    //
    //   originalEntityIdentifier (String):
    //     The original entity identifier to use.
    //
    //   replacementEntityIdentifier (String):
    //     The replacement entity identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static List <String> replaceReferences (
        Collection <String> references,
        String originalEntityIdentifier,
        String replacementEntityIdentifier
    )
    {
        // Return the result produced by to list.

        return references.stream ()
            .map (
                reference -> replaceReference (
                    reference,
                    originalEntityIdentifier,
                    replacementEntityIdentifier
                )
            )
            .toList ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: replaceReference
    //
    // Description:
    //
    //   Performs the replace reference operation.
    //
    // Arguments:
    //
    //   reference (String):
    //     The reference to use.
    //
    //   originalEntityIdentifier (String):
    //     The original entity identifier to use.
    //
    //   replacementEntityIdentifier (String):
    //     The replacement entity identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String replaceReference (
        String reference,
        String originalEntityIdentifier,
        String replacementEntityIdentifier
    )
    {
        // Return the value selected according to reference matches original entity identifier.

        return reference.equals ( originalEntityIdentifier ) ? replacementEntityIdentifier : reference;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: uniqueIdentifier
    //
    // Description:
    //
    //   Performs the unique identifier operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    //   preferredIdentifier (String):
    //     The preferred identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private String uniqueIdentifier (
        AuthoringModel model,
        String categoryIdentifier,
        String preferredIdentifier
    )
    {
        // Initialize the existing identifiers by applying identifiers and get entities.

        Set <String> existingIdentifiers = identifiers ( getEntities ( model, categoryIdentifier ) );

        // Stop this path and return its result when existing identifiers does not contain preferred identifier.

        if ( !existingIdentifiers.contains ( preferredIdentifier ) )
        {
            // Return the preferred identifier to the caller when existing identifiers does not contain preferred
            // identifier.

            return preferredIdentifier;
        }

        int suffix = 2;

        // Continue processing while existing identifiers contains preferred identifier + "-" + suffix.

        while ( existingIdentifiers.contains ( preferredIdentifier + "-" + suffix ) )
        {
            suffix++;
        }

        // Return the composed unique identifier value.

        return preferredIdentifier + "-" + suffix;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: firstIdentifier
    //
    // Description:
    //
    //   Performs the first identifier operation.
    //
    // Arguments:
    //
    //   entities (Collection <? extends AuthoringModel.EntityDefinition>):
    //     The entities to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String firstIdentifier (
        Collection <? extends AuthoringModel.EntityDefinition> entities
    )
    {
        // Return the result produced by or else.

        return entities.stream ()
            .findFirst ()
            .map ( AuthoringModel.EntityDefinition::getId )
            .orElse ( "" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: copyModel
    //
    // Description:
    //
    //   Performs the copy model operation.
    //
    // Arguments:
    //
    //   model (AuthoringModel):
    //     The model to use.
    //
    //   parameters (Collection <AuthoringModel.ParameterDefinition>):
    //     The parameters to use.
    //
    //   payloads (Collection <AuthoringModel.PayloadDefinition>):
    //     The payloads to use.
    //
    //   events (Collection <AuthoringModel.EventDefinition>):
    //     The events to use.
    //
    //   conditions (Collection <AuthoringModel.ConditionDefinition>):
    //     The conditions to use.
    //
    //   conditionSets (Collection <AuthoringModel.ConditionSetDefinition>):
    //     The condition sets to use.
    //
    //   actions (Collection <AuthoringModel.ActionDefinition>):
    //     The actions to use.
    //
    //   rules (Collection <AuthoringModel.RuleDefinition>):
    //     The rules to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static AuthoringModel copyModel (
        AuthoringModel model,
        Collection <AuthoringModel.ParameterDefinition> parameters,
        Collection <AuthoringModel.PayloadDefinition> payloads,
        Collection <AuthoringModel.EventDefinition> events,
        Collection <AuthoringModel.ConditionDefinition> conditions,
        Collection <AuthoringModel.ConditionSetDefinition> conditionSets,
        Collection <AuthoringModel.ActionDefinition> actions,
        Collection <AuthoringModel.RuleDefinition> rules
    )
    {
        // Return a newly constructed authoring model containing the operation result.

        return new AuthoringModel (
            model.getSchemaVersion (),
            model.getModelId (),
            model.getName (),
            model.getDescription (),
            parameters,
            payloads,
            events,
            conditions,
            conditionSets,
            actions,
            rules
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requireCategory
    //
    // Description:
    //
    //   Performs the require category operation.
    //
    // Arguments:
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String requireCategory ( String categoryIdentifier )
    {
        // Validate the required category identifier before continuing.

        Objects.requireNonNull ( categoryIdentifier, "categoryIdentifier" );

        // Reject the operation when category identifiers does not contain category identifier.

        if ( !CATEGORY_IDENTIFIERS.contains ( categoryIdentifier ) )
        {
            throw new IllegalArgumentException ( "Unknown model category: " + categoryIdentifier );
        }

        // Return the category identifier to the caller.

        return categoryIdentifier;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: singularName
    //
    // Description:
    //
    //   Performs the singular name operation.
    //
    // Arguments:
    //
    //   categoryIdentifier (String):
    //     The category identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String singularName ( String categoryIdentifier )
    {
        // Return the singular name result to the caller.

        return switch ( categoryIdentifier )
        {
            // Handle parameters through this switch branch.

            case PARAMETERS     -> "parameter";

            // Handle payloads through this switch branch.

            case PAYLOADS       -> "payload";

            // Handle events through this switch branch.

            case EVENTS         -> "event";

            // Handle conditions through this switch branch.

            case CONDITIONS     -> "condition";

            // Handle condition sets through this switch branch.

            case CONDITION_SETS -> "condition set";

            // Handle actions through this switch branch.

            case ACTIONS        -> "action";

            // Handle rules through this switch branch.

            case RULES          -> "rule";

            // Handle the default case through this switch branch.

            default             -> categoryIdentifier.toLowerCase ( Locale.ROOT );
        };
    }

    //=================================================================================================================
    // User Defined Data Types
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Enum: ConditionOperatorDraft
    //
    // Description:
    //
    //   Enumerates the supported condition operator draft values.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public enum ConditionOperatorDraft
    {
        //=============================================================================================================
        // Constants
        //=============================================================================================================

        EQUALS ( 1 ),
        NOT_EQUALS ( 1 ),
        GREATER_THAN ( 1 ),
        GREATER_THAN_OR_EQUAL ( 1 ),
        LESS_THAN ( 1 ),
        LESS_THAN_OR_EQUAL ( 1 ),
        BETWEEN_EXCLUSIVE ( 2 ),
        BETWEEN_INCLUSIVE ( 2 ),
        ANY ( 0 );

        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final int operandCount;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getOperandCount
        //
        // Description:
        //
        //   Returns the operand count.
        //
        // Returns:
        //
        //   The operand count.
        //
        //-------------------------------------------------------------------------------------------------------------

        public int getOperandCount ()
        {
            // Return the operand count to the caller.

            return this.operandCount;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: ConditionOperatorDraft
        //
        // Description:
        //
        //   Creates the ConditionOperatorDraft instance from the supplied values.
        //
        // Arguments:
        //
        //   operandCount (int):
        //     The operand count to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        ConditionOperatorDraft ( int operandCount )
        {
            this.operandCount = operandCount;
        }

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: supports
        //
        // Description:
        //
        //   Performs the supports operation.
        //
        // Arguments:
        //
        //   parameterType (String):
        //     The parameter type to use.
        //
        // Returns:
        //
        //   The result of the operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        private boolean supports ( String parameterType )
        {
            // Stop this path and return its result when this equals any or this equals equals or this equals not
            // equals.

            if ( this == ANY || this == EQUALS || this == NOT_EQUALS )
            {
                // Return true for this outcome when this equals any or this equals equals or this equals not equals.

                return true;
            }

            // Return the result produced by equals.

            return ParameterType.INTEGER.name ().equals ( parameterType );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Enum: BindingState
    //
    // Description:
    //
    //   Enumerates the supported binding state values.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public enum BindingState
    {
        CONCRETE,
        WILDCARD,
        OMITTED
    }

    //*****************************************************************************************************************
    // Class: BindingDraft
    //
    // Description:
    //
    //   Provides the binding draft behavior.
    //
    //*****************************************************************************************************************

    public static final class BindingDraft
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String conditionIdentifier;
        private final BindingState state;
        private final String concreteText;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getConditionIdentifier
        //
        // Description:
        //
        //   Returns the condition identifier.
        //
        // Returns:
        //
        //   The condition identifier.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getConditionIdentifier ()
        {
            // Return the condition identifier to the caller.

            return this.conditionIdentifier;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getState
        //
        // Description:
        //
        //   Returns the state.
        //
        // Returns:
        //
        //   The state.
        //
        //-------------------------------------------------------------------------------------------------------------

        public BindingState getState ()
        {
            // Return the state to the caller.

            return this.state;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getConcreteText
        //
        // Description:
        //
        //   Returns the concrete text.
        //
        // Returns:
        //
        //   The concrete text.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getConcreteText ()
        {
            // Return the concrete text to the caller.

            return this.concreteText;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: BindingDraft
        //
        // Description:
        //
        //   Creates the BindingDraft instance from the supplied values.
        //
        // Arguments:
        //
        //   conditionIdentifier (String):
        //     The condition identifier to use.
        //
        //   state (BindingState):
        //     The state to use.
        //
        //   concreteText (String):
        //     The concrete text to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private BindingDraft (
            String conditionIdentifier,
            BindingState state,
            String concreteText
        )
        {
            // Validate the required condition identifier, state, and concrete text before continuing.

            this.conditionIdentifier = Objects.requireNonNull (
                conditionIdentifier,
                "conditionIdentifier"
            );
            this.state        = Objects.requireNonNull ( state, "state" );
            this.concreteText = Objects.requireNonNull ( concreteText, "concreteText" );
        }

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: concrete
        //
        // Description:
        //
        //   Creates a BindingDraft instance for the concrete case.
        //
        // Arguments:
        //
        //   conditionIdentifier (String):
        //     The condition identifier to use.
        //
        //   concreteText (String):
        //     The concrete text to use.
        //
        // Returns:
        //
        //   The resulting BindingDraft instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        public static BindingDraft concrete ( String conditionIdentifier, String concreteText )
        {
            // Return a newly constructed binding draft containing the operation result.

            return new BindingDraft (
                conditionIdentifier,
                BindingState.CONCRETE,
                concreteText
            );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: wildcard
        //
        // Description:
        //
        //   Creates a BindingDraft instance for the wildcard case.
        //
        // Arguments:
        //
        //   conditionIdentifier (String):
        //     The condition identifier to use.
        //
        // Returns:
        //
        //   The resulting BindingDraft instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        public static BindingDraft wildcard ( String conditionIdentifier )
        {
            // Return a newly constructed binding draft containing the operation result.

            return new BindingDraft ( conditionIdentifier, BindingState.WILDCARD, "" );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: omitted
        //
        // Description:
        //
        //   Creates a BindingDraft instance for the omitted case.
        //
        // Arguments:
        //
        //   conditionIdentifier (String):
        //     The condition identifier to use.
        //
        // Returns:
        //
        //   The resulting BindingDraft instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        public static BindingDraft omitted ( String conditionIdentifier )
        {
            // Return a newly constructed binding draft containing the operation result.

            return new BindingDraft ( conditionIdentifier, BindingState.OMITTED, "" );
        }
    }

    //*****************************************************************************************************************
    // Class: EntityDraft
    //
    // Description:
    //
    //   Provides the entity draft behavior.
    //
    //*****************************************************************************************************************

    public static final class EntityDraft
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String id;
        private final String name;
        private final String description;
        private final String parameterType;
        private final List <String> enumerationValues;
        private final List <String> parameterIds;
        private final String payloadId;
        private final String parameterId;
        private final String conditionOperator;
        private final String conditionValueText;
        private final String secondConditionValueText;
        private final Map <String, BindingDraft> bindings;
        private final List <String> conditionIds;
        private final boolean predefinedConditions;
        private final String eventId;
        private final String conditionSetId;
        private final String actionId;

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

        public String getId ()
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

        public String getName ()
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

        public String getDescription ()
        {
            // Return the description to the caller.

            return this.description;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getParameterType
        //
        // Description:
        //
        //   Returns the parameter type.
        //
        // Returns:
        //
        //   The parameter type.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getParameterType ()
        {
            // Return the parameter type to the caller.

            return this.parameterType;
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
        // Method: getConditionOperator
        //
        // Description:
        //
        //   Returns the condition operator.
        //
        // Returns:
        //
        //   The condition operator.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getConditionOperator ()
        {
            // Return the condition operator to the caller.

            return this.conditionOperator;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getConditionValueText
        //
        // Description:
        //
        //   Returns the condition value text.
        //
        // Returns:
        //
        //   The condition value text.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getConditionValueText ()
        {
            // Return the condition value text to the caller.

            return this.conditionValueText;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getSecondConditionValueText
        //
        // Description:
        //
        //   Returns the second condition value text.
        //
        // Returns:
        //
        //   The second condition value text.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getSecondConditionValueText ()
        {
            // Return the second condition value text to the caller.

            return this.secondConditionValueText;
        }

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

        public Map <String, BindingDraft> getBindings ()
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
        // Constructor 1/1: EntityDraft
        //
        // Description:
        //
        //   Creates the EntityDraft instance from the supplied values.
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
        //   parameterType (String):
        //     The parameter type to use.
        //
        //   enumerationValues (Collection <String>):
        //     The enumeration values to use.
        //
        //   parameterIds (Collection <String>):
        //     The parameter ids to use.
        //
        //   payloadId (String):
        //     The payload id to use.
        //
        //   parameterId (String):
        //     The parameter id to use.
        //
        //   conditionOperator (String):
        //     The condition operator to use.
        //
        //   conditionValueText (String):
        //     The condition value text to use.
        //
        //   secondConditionValueText (String):
        //     The second condition value text to use.
        //
        //   bindings (Map <String, BindingDraft>):
        //     The bindings to use.
        //
        //   conditionIds (Collection <String>):
        //     The condition ids to use.
        //
        //   predefinedConditions (boolean):
        //     The predefined conditions to use.
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

        private EntityDraft (
            String id,
            String name,
            String description,
            String parameterType,
            Collection <String> enumerationValues,
            Collection <String> parameterIds,
            String payloadId,
            String parameterId,
            String conditionOperator,
            String conditionValueText,
            String secondConditionValueText,
            Map <String, BindingDraft> bindings,
            Collection <String> conditionIds,
            boolean predefinedConditions,
            String eventId,
            String conditionSetId,
            String actionId
        )
        {
            // Validate the required ID, name, and description before continuing.

            this.id                = Objects.requireNonNull ( id, "id" );
            this.name              = Objects.requireNonNull ( name, "name" );
            this.description       = Objects.requireNonNull ( description, "description" );
            this.parameterType     = parameterType;

            // Complete the entity draft step by calling copy of.

            this.enumerationValues = List.copyOf ( enumerationValues );
            this.parameterIds      = List.copyOf ( parameterIds );
            this.payloadId         = payloadId;
            this.parameterId       = parameterId;
            this.conditionOperator = conditionOperator;

            // Perform the require non null, unmodifiable map, and copy of calls required by the entity draft
            // operation.

            this.conditionValueText = Objects.requireNonNull (
                conditionValueText,
                "conditionValueText"
            );
            this.secondConditionValueText = Objects.requireNonNull (
                secondConditionValueText,
                "secondConditionValueText"
            );
            this.bindings          = Collections.unmodifiableMap (
                new LinkedHashMap <String, BindingDraft> ( bindings )
            );
            this.conditionIds       = List.copyOf ( conditionIds );
            this.predefinedConditions = predefinedConditions;
            this.eventId           = eventId;
            this.conditionSetId    = conditionSetId;
            this.actionId          = actionId;
        }

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: parameter
        //
        // Description:
        //
        //   Creates a EntityDraft instance for the parameter case.
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
        //   parameterType (String):
        //     The parameter type to use.
        //
        //   enumerationValues (Collection <String>):
        //     The enumeration values to use.
        //
        // Returns:
        //
        //   The resulting EntityDraft instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        public static EntityDraft parameter (
            String id,
            String name,
            String description,
            String parameterType,
            Collection <String> enumerationValues
        )
        {
            // Return a newly constructed entity draft containing the operation result.

            return new EntityDraft (
                id,
                name,
                description,
                parameterType,
                enumerationValues,
                List.of (),
                null,
                null,
                null,
                "",
                "",
                Map.of (),
                List.of (),
                false,
                null,
                null,
                null
            );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: payload
        //
        // Description:
        //
        //   Creates a EntityDraft instance for the payload case.
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
        // Returns:
        //
        //   The resulting EntityDraft instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        public static EntityDraft payload (
            String id,
            String name,
            String description,
            Collection <String> parameterIds
        )
        {
            // Return a newly constructed entity draft containing the operation result.

            return new EntityDraft (
                id,
                name,
                description,
                null,
                List.of (),
                parameterIds,
                null,
                null,
                null,
                "",
                "",
                Map.of (),
                List.of (),
                false,
                null,
                null,
                null
            );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: event
        //
        // Description:
        //
        //   Creates a EntityDraft instance for the event case.
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
        // Returns:
        //
        //   The resulting EntityDraft instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        public static EntityDraft event (
            String id,
            String name,
            String description,
            String payloadId
        )
        {
            // Return a newly constructed entity draft containing the operation result.

            return new EntityDraft (
                id,
                name,
                description,
                null,
                List.of (),
                List.of (),
                payloadId,
                null,
                null,
                "",
                "",
                Map.of (),
                List.of (),
                false,
                null,
                null,
                null
            );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: condition
        //
        // Description:
        //
        //   Creates a EntityDraft instance for the condition case.
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
        // Returns:
        //
        //   The resulting EntityDraft instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        public static EntityDraft condition (
            String id,
            String name,
            String description,
            String parameterId
        )
        {
            // Return the result produced by condition.

            return condition ( id, name, description, parameterId, null, "", "" );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: condition
        //
        // Description:
        //
        //   Creates a EntityDraft instance for the condition case.
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
        //   conditionOperator (String):
        //     The condition operator to use.
        //
        //   conditionValueText (String):
        //     The condition value text to use.
        //
        //   secondConditionValueText (String):
        //     The second condition value text to use.
        //
        // Returns:
        //
        //   The resulting EntityDraft instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        public static EntityDraft condition (
            String id,
            String name,
            String description,
            String parameterId,
            String conditionOperator,
            String conditionValueText,
            String secondConditionValueText
        )
        {
            // Return a newly constructed entity draft containing the operation result.

            return new EntityDraft (
                id,
                name,
                description,
                null,
                List.of (),
                List.of (),
                null,
                parameterId,
                conditionOperator,
                conditionValueText,
                secondConditionValueText,
                Map.of (),
                List.of (),
                false,
                null,
                null,
                null
            );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: conditionSet
        //
        // Description:
        //
        //   Creates a EntityDraft instance for the condition set case.
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
        //   bindings (Map <String, BindingDraft>):
        //     The bindings to use.
        //
        // Returns:
        //
        //   The resulting EntityDraft instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        public static EntityDraft conditionSet (
            String id,
            String name,
            String description,
            Map <String, BindingDraft> bindings
        )
        {
            // Return the result produced by legacy condition set.

            return legacyConditionSet ( id, name, description, bindings );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: legacyConditionSet
        //
        // Description:
        //
        //   Creates a EntityDraft instance for the legacy condition set case.
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
        //   bindings (Map <String, BindingDraft>):
        //     The bindings to use.
        //
        // Returns:
        //
        //   The resulting EntityDraft instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        public static EntityDraft legacyConditionSet (
            String id,
            String name,
            String description,
            Map <String, BindingDraft> bindings
        )
        {
            // Return a newly constructed entity draft containing the operation result.

            return new EntityDraft (
                id,
                name,
                description,
                null,
                List.of (),
                List.of (),
                null,
                null,
                null,
                "",
                "",
                bindings,
                List.of (),
                false,
                null,
                null,
                null
            );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: conditionSet
        //
        // Description:
        //
        //   Creates a EntityDraft instance for the condition set case.
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
        // Returns:
        //
        //   The resulting EntityDraft instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        public static EntityDraft conditionSet (
            String id,
            String name,
            String description,
            Collection <String> conditionIds
        )
        {
            // Return a newly constructed entity draft containing the operation result.

            return new EntityDraft (
                id,
                name,
                description,
                null,
                List.of (),
                List.of (),
                null,
                null,
                null,
                "",
                "",
                Map.of (),
                conditionIds,
                true,
                null,
                null,
                null
            );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: action
        //
        // Description:
        //
        //   Creates a EntityDraft instance for the action case.
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
        // Returns:
        //
        //   The resulting EntityDraft instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        public static EntityDraft action ( String id, String name, String description )
        {
            // Return a newly constructed entity draft containing the operation result.

            return new EntityDraft (
                id,
                name,
                description,
                null,
                List.of (),
                List.of (),
                null,
                null,
                null,
                "",
                "",
                Map.of (),
                List.of (),
                false,
                null,
                null,
                null
            );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: rule
        //
        // Description:
        //
        //   Creates a EntityDraft instance for the rule case.
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
        // Returns:
        //
        //   The resulting EntityDraft instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        public static EntityDraft rule (
            String id,
            String name,
            String description,
            String eventId,
            String conditionSetId,
            String actionId
        )
        {
            // Return a newly constructed entity draft containing the operation result.

            return new EntityDraft (
                id,
                name,
                description,
                null,
                List.of (),
                List.of (),
                null,
                null,
                null,
                "",
                "",
                Map.of (),
                List.of (),
                false,
                eventId,
                conditionSetId,
                actionId
            );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: withIdentity
        //
        // Description:
        //
        //   Creates a EntityDraft instance with the identity updated.
        //
        // Arguments:
        //
        //   replacementIdentifier (String):
        //     The replacement identifier to use.
        //
        //   replacementName (String):
        //     The replacement name to use.
        //
        // Returns:
        //
        //   The resulting EntityDraft instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        public EntityDraft withIdentity ( String replacementIdentifier, String replacementName )
        {
            // Return a newly constructed entity draft containing the operation result.

            return new EntityDraft (
                replacementIdentifier,
                replacementName,
                this.description,
                this.parameterType,
                this.enumerationValues,
                this.parameterIds,
                this.payloadId,
                this.parameterId,
                this.conditionOperator,
                this.conditionValueText,
                this.secondConditionValueText,
                this.bindings,
                this.conditionIds,
                this.predefinedConditions,
                this.eventId,
                this.conditionSetId,
                this.actionId
            );
        }
    }

    //*****************************************************************************************************************
    // Class: FieldError
    //
    // Description:
    //
    //   Provides the field error behavior.
    //
    //*****************************************************************************************************************

    public static final class FieldError
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String field;
        private final String message;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getField
        //
        // Description:
        //
        //   Returns the field.
        //
        // Returns:
        //
        //   The field.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getField ()
        {
            // Return the field to the caller.

            return this.field;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getMessage
        //
        // Description:
        //
        //   Returns the message.
        //
        // Returns:
        //
        //   The message.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getMessage ()
        {
            // Return the message to the caller.

            return this.message;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: FieldError
        //
        // Description:
        //
        //   Creates the FieldError instance from the supplied values.
        //
        // Arguments:
        //
        //   field (String):
        //     The field to use.
        //
        //   message (String):
        //     The message to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private FieldError ( String field, String message )
        {
            // Validate the required field and message before continuing.

            this.field   = Objects.requireNonNull ( field, "field" );
            this.message = Objects.requireNonNull ( message, "message" );
        }
    }

    //*****************************************************************************************************************
    // Class: EntitySummary
    //
    // Description:
    //
    //   Provides the entity summary behavior.
    //
    //*****************************************************************************************************************

    public static final class EntitySummary
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String identifier;
        private final String name;
        private final String details;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getIdentifier
        //
        // Description:
        //
        //   Returns the identifier.
        //
        // Returns:
        //
        //   The identifier.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getIdentifier ()
        {
            // Return the identifier to the caller.

            return this.identifier;
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

        public String getName ()
        {
            // Return the name to the caller.

            return this.name;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getDetails
        //
        // Description:
        //
        //   Returns the details.
        //
        // Returns:
        //
        //   The details.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getDetails ()
        {
            // Return the details to the caller.

            return this.details;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: EntitySummary
        //
        // Description:
        //
        //   Creates the EntitySummary instance from the supplied values.
        //
        // Arguments:
        //
        //   identifier (String):
        //     The identifier to use.
        //
        //   name (String):
        //     The name to use.
        //
        //   details (String):
        //     The details to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private EntitySummary ( String identifier, String name, String details )
        {
            this.identifier = identifier;
            this.name       = name;
            this.details    = details;
        }
    }

    //*****************************************************************************************************************
    // Class: ConditionBindingDescriptor
    //
    // Description:
    //
    //   Provides the condition binding descriptor behavior.
    //
    //*****************************************************************************************************************

    public static final class ConditionBindingDescriptor
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String conditionIdentifier;
        private final String parameterType;
        private final List <String> enumerationValues;
        private final BindingDraft binding;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getConditionIdentifier
        //
        // Description:
        //
        //   Returns the condition identifier.
        //
        // Returns:
        //
        //   The condition identifier.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getConditionIdentifier ()
        {
            // Return the condition identifier to the caller.

            return this.conditionIdentifier;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getParameterType
        //
        // Description:
        //
        //   Returns the parameter type.
        //
        // Returns:
        //
        //   The parameter type.
        //
        //-------------------------------------------------------------------------------------------------------------

        public Optional <String> getParameterType ()
        {
            // Return an optional containing the value when it is available.

            return Optional.ofNullable ( this.parameterType );
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

        //-------------------------------------------------------------------------------------------------------------
        // Method: getBinding
        //
        // Description:
        //
        //   Returns the binding.
        //
        // Returns:
        //
        //   The binding.
        //
        //-------------------------------------------------------------------------------------------------------------

        public BindingDraft getBinding ()
        {
            // Return the binding to the caller.

            return this.binding;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: ConditionBindingDescriptor
        //
        // Description:
        //
        //   Creates the ConditionBindingDescriptor instance from the supplied values.
        //
        // Arguments:
        //
        //   conditionIdentifier (String):
        //     The condition identifier to use.
        //
        //   parameterType (String):
        //     The parameter type to use.
        //
        //   enumerationValues (Collection <String>):
        //     The enumeration values to use.
        //
        //   binding (BindingDraft):
        //     The binding to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private ConditionBindingDescriptor (
            String conditionIdentifier,
            String parameterType,
            Collection <String> enumerationValues,
            BindingDraft binding
        )
        {
            this.conditionIdentifier = conditionIdentifier;
            this.parameterType       = parameterType;

            // Update the enumeration values from the copy of result.

            this.enumerationValues   = List.copyOf ( enumerationValues );
            this.binding             = binding;
        }
    }

    //*****************************************************************************************************************
    // Class: ConditionSelectionDescriptor
    //
    // Description:
    //
    //   Provides the condition selection descriptor behavior.
    //
    //*****************************************************************************************************************

    public static final class ConditionSelectionDescriptor
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String conditionIdentifier;
        private final String name;
        private final String expression;
        private final boolean selected;
        private final boolean configured;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getConditionIdentifier
        //
        // Description:
        //
        //   Returns the condition identifier.
        //
        // Returns:
        //
        //   The condition identifier.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getConditionIdentifier ()
        {
            // Return the condition identifier to the caller.

            return this.conditionIdentifier;
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

        public String getName ()
        {
            // Return the name to the caller.

            return this.name;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getExpression
        //
        // Description:
        //
        //   Returns the expression.
        //
        // Returns:
        //
        //   The expression.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getExpression ()
        {
            // Return the expression to the caller.

            return this.expression;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: isSelected
        //
        // Description:
        //
        //   Indicates whether selected.
        //
        // Returns:
        //
        //   `true` when the condition is satisfied; otherwise `false`.
        //
        //-------------------------------------------------------------------------------------------------------------

        public boolean isSelected ()
        {
            // Return the selected to the caller.

            return this.selected;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: isConfigured
        //
        // Description:
        //
        //   Indicates whether configured.
        //
        // Returns:
        //
        //   `true` when the condition is satisfied; otherwise `false`.
        //
        //-------------------------------------------------------------------------------------------------------------

        public boolean isConfigured ()
        {
            // Return the configured to the caller.

            return this.configured;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: ConditionSelectionDescriptor
        //
        // Description:
        //
        //   Creates the ConditionSelectionDescriptor instance from the supplied values.
        //
        // Arguments:
        //
        //   conditionIdentifier (String):
        //     The condition identifier to use.
        //
        //   name (String):
        //     The name to use.
        //
        //   expression (String):
        //     The expression to use.
        //
        //   selected (boolean):
        //     The selected to use.
        //
        //   configured (boolean):
        //     The configured to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private ConditionSelectionDescriptor (
            String conditionIdentifier,
            String name,
            String expression,
            boolean selected,
            boolean configured
        )
        {
            this.conditionIdentifier = conditionIdentifier;
            this.name                = name;
            this.expression          = expression;
            this.selected            = selected;
            this.configured          = configured;
        }
    }

    //*****************************************************************************************************************
    // Class: ConditionParameterDescriptor
    //
    // Description:
    //
    //   Provides the condition parameter descriptor behavior.
    //
    //*****************************************************************************************************************

    public static final class ConditionParameterDescriptor
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String parameterIdentifier;
        private final String parameterType;
        private final List <String> enumerationValues;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getParameterIdentifier
        //
        // Description:
        //
        //   Returns the parameter identifier.
        //
        // Returns:
        //
        //   The parameter identifier.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getParameterIdentifier ()
        {
            // Return the parameter identifier to the caller.

            return this.parameterIdentifier;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getParameterType
        //
        // Description:
        //
        //   Returns the parameter type.
        //
        // Returns:
        //
        //   The parameter type.
        //
        //-------------------------------------------------------------------------------------------------------------

        public String getParameterType ()
        {
            // Return the parameter type to the caller.

            return this.parameterType;
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
        // Constructor 1/1: ConditionParameterDescriptor
        //
        // Description:
        //
        //   Creates the ConditionParameterDescriptor instance from the supplied values.
        //
        // Arguments:
        //
        //   parameterIdentifier (String):
        //     The parameter identifier to use.
        //
        //   parameterType (String):
        //     The parameter type to use.
        //
        //   enumerationValues (Collection <String>):
        //     The enumeration values to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private ConditionParameterDescriptor (
            String parameterIdentifier,
            String parameterType,
            Collection <String> enumerationValues
        )
        {
            this.parameterIdentifier = parameterIdentifier;
            this.parameterType       = parameterType;

            // Update the enumeration values from the copy of result.

            this.enumerationValues   = List.copyOf ( enumerationValues );
        }
    }

    //*****************************************************************************************************************
    // Class: EditResult
    //
    // Description:
    //
    //   Provides the edit result behavior.
    //
    //*****************************************************************************************************************

    public static final class EditResult
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final AuthoringModel model;
        private final String entityIdentifier;
        private final List <FieldError> errors;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: isSuccessful
        //
        // Description:
        //
        //   Indicates whether successful.
        //
        // Returns:
        //
        //   `true` when the condition is satisfied; otherwise `false`.
        //
        //-------------------------------------------------------------------------------------------------------------

        public boolean isSuccessful ()
        {
            // Return whether model is available.

            return this.model != null;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getModel
        //
        // Description:
        //
        //   Returns the model.
        //
        // Returns:
        //
        //   The model.
        //
        //-------------------------------------------------------------------------------------------------------------

        public Optional <AuthoringModel> getModel ()
        {
            // Return an optional containing the value when it is available.

            return Optional.ofNullable ( this.model );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getContent
        //
        // Description:
        //
        //   Returns the content.
        //
        // Returns:
        //
        //   The content.
        //
        //-------------------------------------------------------------------------------------------------------------

        public Optional <ClientDocumentSession.Content> getContent ()
        {
            // Return the value selected according to model is unavailable.

            return this.model == null
                ? Optional.empty ()
                : Optional.of ( new ClientDocumentSession.Content ( this.model ) );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getEntityIdentifier
        //
        // Description:
        //
        //   Returns the entity identifier.
        //
        // Returns:
        //
        //   The entity identifier.
        //
        //-------------------------------------------------------------------------------------------------------------

        public Optional <String> getEntityIdentifier ()
        {
            // Return an optional containing the value when it is available.

            return Optional.ofNullable ( this.entityIdentifier );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getErrors
        //
        // Description:
        //
        //   Returns the errors.
        //
        // Returns:
        //
        //   The errors.
        //
        //-------------------------------------------------------------------------------------------------------------

        public List <FieldError> getErrors ()
        {
            // Return the errors to the caller.

            return this.errors;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: EditResult
        //
        // Description:
        //
        //   Creates the EditResult instance from the supplied values.
        //
        // Arguments:
        //
        //   model (AuthoringModel):
        //     The model to use.
        //
        //   entityIdentifier (String):
        //     The entity identifier to use.
        //
        //   errors (Collection <FieldError>):
        //     The errors to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private EditResult (
            AuthoringModel model,
            String entityIdentifier,
            Collection <FieldError> errors
        )
        {
            this.model            = model;
            this.entityIdentifier = entityIdentifier;

            // Update the errors from the copy of result.

            this.errors           = List.copyOf ( errors );
        }

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: success
        //
        // Description:
        //
        //   Creates a EditResult instance for the success case.
        //
        // Arguments:
        //
        //   model (AuthoringModel):
        //     The model to use.
        //
        //   entityIdentifier (String):
        //     The entity identifier to use.
        //
        // Returns:
        //
        //   The resulting EditResult instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        private static EditResult success ( AuthoringModel model, String entityIdentifier )
        {
            // Return a newly constructed edit result containing the operation result.

            return new EditResult ( model, entityIdentifier, List.of () );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: failure
        //
        // Description:
        //
        //   Creates a EditResult instance for the failure case.
        //
        // Arguments:
        //
        //   errors (Collection <FieldError>):
        //     The errors to use.
        //
        // Returns:
        //
        //   The resulting EditResult instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        private static EditResult failure ( Collection <FieldError> errors )
        {
            // Return a newly constructed edit result containing the operation result.

            return new EditResult ( null, null, errors );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: rejected
        //
        // Description:
        //
        //   Creates a EditResult instance for the rejected case.
        //
        // Arguments:
        //
        //   message (String):
        //     The message to use.
        //
        // Returns:
        //
        //   The resulting EditResult instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        public static EditResult rejected ( String message )
        {
            // Return the result produced by failure.

            return failure ( List.of ( new FieldError ( "operation", message ) ) );
        }
    }

    //*****************************************************************************************************************
    // Class: DeleteResult
    //
    // Description:
    //
    //   Provides the delete result behavior.
    //
    //*****************************************************************************************************************

    public static final class DeleteResult
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final AuthoringModel model;
        private final String entityIdentifier;
        private final List <String> references;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: isSuccessful
        //
        // Description:
        //
        //   Indicates whether successful.
        //
        // Returns:
        //
        //   `true` when the condition is satisfied; otherwise `false`.
        //
        //-------------------------------------------------------------------------------------------------------------

        public boolean isSuccessful ()
        {
            // Return whether model is available.

            return this.model != null;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getModel
        //
        // Description:
        //
        //   Returns the model.
        //
        // Returns:
        //
        //   The model.
        //
        //-------------------------------------------------------------------------------------------------------------

        public Optional <AuthoringModel> getModel ()
        {
            // Return an optional containing the value when it is available.

            return Optional.ofNullable ( this.model );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getContent
        //
        // Description:
        //
        //   Returns the content.
        //
        // Returns:
        //
        //   The content.
        //
        //-------------------------------------------------------------------------------------------------------------

        public Optional <ClientDocumentSession.Content> getContent ()
        {
            // Return the value selected according to model is unavailable.

            return this.model == null
                ? Optional.empty ()
                : Optional.of ( new ClientDocumentSession.Content ( this.model ) );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getEntityIdentifier
        //
        // Description:
        //
        //   Returns the entity identifier.
        //
        // Returns:
        //
        //   The entity identifier.
        //
        //-------------------------------------------------------------------------------------------------------------

        public Optional <String> getEntityIdentifier ()
        {
            // Return an optional containing the value when it is available.

            return Optional.ofNullable ( this.entityIdentifier );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getReferences
        //
        // Description:
        //
        //   Returns the references.
        //
        // Returns:
        //
        //   The references.
        //
        //-------------------------------------------------------------------------------------------------------------

        public List <String> getReferences ()
        {
            // Return the references to the caller.

            return this.references;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: DeleteResult
        //
        // Description:
        //
        //   Creates the DeleteResult instance from the supplied values.
        //
        // Arguments:
        //
        //   model (AuthoringModel):
        //     The model to use.
        //
        //   entityIdentifier (String):
        //     The entity identifier to use.
        //
        //   references (Collection <String>):
        //     The references to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private DeleteResult (
            AuthoringModel model,
            String entityIdentifier,
            Collection <String> references
        )
        {
            this.model            = model;
            this.entityIdentifier = entityIdentifier;

            // Update the references from the copy of result.

            this.references       = List.copyOf ( references );
        }

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: success
        //
        // Description:
        //
        //   Creates a DeleteResult instance for the success case.
        //
        // Arguments:
        //
        //   model (AuthoringModel):
        //     The model to use.
        //
        //   entityIdentifier (String):
        //     The entity identifier to use.
        //
        // Returns:
        //
        //   The resulting DeleteResult instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        private static DeleteResult success ( AuthoringModel model, String entityIdentifier )
        {
            // Return a newly constructed delete result containing the operation result.

            return new DeleteResult ( model, entityIdentifier, List.of () );
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: blocked
        //
        // Description:
        //
        //   Creates a DeleteResult instance for the blocked case.
        //
        // Arguments:
        //
        //   references (Collection <String>):
        //     The references to use.
        //
        // Returns:
        //
        //   The resulting DeleteResult instance.
        //
        //-------------------------------------------------------------------------------------------------------------

        private static DeleteResult blocked ( Collection <String> references )
        {
            // Return a newly constructed delete result containing the operation result.

            return new DeleteResult ( null, null, references );
        }
    }
}
