//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2026
// Author:  Rohin Gosling
//
// Description:
//
//   Applies the shared themed modal-dialog presentation, safe focus, and invoker restoration behavior.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.client;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.StageStyle;
import javafx.stage.Window;

//*********************************************************************************************************************
// Class: ClientModalDialogs
//*********************************************************************************************************************

final class ClientModalDialogs
{
    private static final String MODAL_DEPTH_PROPERTY = ClientModalDialogs.class.getName () + ".modalDepth";

    private ClientModalDialogs ()
    {
        // This class supplies static dialog behavior and must not be instantiated.
    }

    static void prepare (
        Dialog <?> dialog,
        Window owner,
        String title,
        String closeLabel,
        String themeStyleClass,
        ButtonType safeInitialButton,
        ButtonType destructiveButton
    )
    {
        prepare (
            dialog,
            owner,
            title,
            closeLabel,
            themeStyleClass,
            safeInitialButton,
            null,
            destructiveButton
        );
    }

    static void prepare (
        Dialog <?> dialog,
        Window owner,
        String title,
        String closeLabel,
        String themeStyleClass,
        ButtonType safeInitialButton,
        Node safeInitialControl,
        ButtonType destructiveButton
    )
    {
        Node invoker = owner.getScene () == null ? null : owner.getScene ().getFocusOwner ();
        String stylesheet = ClientMain.class.getResource ( "client.css" ).toExternalForm ();

        dialog.initOwner ( owner );
        dialog.initStyle ( StageStyle.UNDECORATED );
        dialog.setTitle ( title );
        dialog.setHeaderText ( null );
        dialog.getDialogPane ().getStylesheets ().add ( stylesheet );
        dialog.getDialogPane ().getStyleClass ().addAll ( "themed-dialog", themeStyleClass );
        wrapContent ( dialog.getDialogPane () );

        Label titleLabel = new Label ( title );
        titleLabel.getStyleClass ().add ( "dialog-title" );

        Region titleSpacer = new Region ();
        Button closeButton = new Button ( "\u00d7" );
        closeButton.setAccessibleText ( closeLabel );
        closeButton.setCancelButton ( true );
        closeButton.setOnAction ( actionEvent -> dialog.close () );
        closeButton.getStyleClass ().add ( "dialog-close-button" );

        HBox titleBar = new HBox ( titleLabel, titleSpacer, closeButton );
        titleBar.getStyleClass ().add ( "dialog-title-bar" );
        HBox.setHgrow ( titleSpacer, Priority.ALWAYS );
        dialog.getDialogPane ().setHeader ( titleBar );

        dialog.setOnShown ( windowEvent -> Platform.runLater ( () ->
        {
            dimOwner ( owner );
            preserveButtonInsertionOrder ( dialog.getDialogPane () );

            if ( destructiveButton != null )
            {
                Node destructiveControl = dialog.getDialogPane ().lookupButton ( destructiveButton );

                if ( destructiveControl != null )
                {
                    destructiveControl.getStyleClass ().add ( "danger-button" );
                }
            }

            Node safeControl = safeInitialControl != null
                ? safeInitialControl
                : safeInitialButton == null
                    ? null
                    : dialog.getDialogPane ().lookupButton ( safeInitialButton );

            if ( safeControl != null )
            {
                safeControl.requestFocus ();
            }
        } ) );

        dialog.setOnHidden ( windowEvent -> Platform.runLater ( () ->
        {
            restoreOwner ( owner );

            if ( invoker != null && invoker.getScene () != null )
            {
                invoker.requestFocus ();
            }
        } ) );
    }

    private static void dimOwner ( Window owner )
    {
        if ( owner.getScene () == null )
        {
            return;
        }

        Node ownerRoot = owner.getScene ().getRoot ();
        Object depthValue = ownerRoot.getProperties ().get ( MODAL_DEPTH_PROPERTY );
        int modalDepth = depthValue instanceof Integer ? (Integer) depthValue : 0;

        ownerRoot.getProperties ().put ( MODAL_DEPTH_PROPERTY, modalDepth + 1 );
        if ( modalDepth == 0 )
        {
            ownerRoot.getStyleClass ().add ( "modal-owner-dimmed" );
        }
    }

    private static void restoreOwner ( Window owner )
    {
        if ( owner.getScene () == null )
        {
            return;
        }

        Node ownerRoot = owner.getScene ().getRoot ();
        Object depthValue = ownerRoot.getProperties ().get ( MODAL_DEPTH_PROPERTY );
        int modalDepth = depthValue instanceof Integer ? (Integer) depthValue : 0;

        if ( modalDepth <= 1 )
        {
            ownerRoot.getProperties ().remove ( MODAL_DEPTH_PROPERTY );
            ownerRoot.getStyleClass ().remove ( "modal-owner-dimmed" );
        }
        else
        {
            ownerRoot.getProperties ().put ( MODAL_DEPTH_PROPERTY, modalDepth - 1 );
        }
    }

    private static void preserveButtonInsertionOrder ( DialogPane dialogPane )
    {
        dialogPane.applyCss ();

        Node buttonBarNode = dialogPane.lookup ( ".button-bar" );

        if ( buttonBarNode instanceof ButtonBar buttonBar )
        {
            buttonBar.setButtonOrder ( ButtonBar.BUTTON_ORDER_NONE );
        }
    }

    private static void wrapContent ( DialogPane dialogPane )
    {
        Node content = dialogPane.getContent ();

        if ( content == null && dialogPane.getContentText () != null && !dialogPane.getContentText ().isBlank () )
        {
            Label contentLabel = new Label ( dialogPane.getContentText () );

            contentLabel.setMaxWidth ( Double.MAX_VALUE );
            contentLabel.setWrapText ( true );
            dialogPane.setContentText ( null );
            content = contentLabel;
        }

        if ( content == null || content instanceof ScrollPane )
        {
            return;
        }

        ScrollPane contentScrollPane = new ScrollPane ( content );

        contentScrollPane.setFitToHeight ( true );
        contentScrollPane.setFitToWidth ( true );
        contentScrollPane.setPannable ( false );
        contentScrollPane.getStyleClass ().add ( "dialog-content-scroll" );
        dialogPane.setContent ( contentScrollPane );
    }
}
