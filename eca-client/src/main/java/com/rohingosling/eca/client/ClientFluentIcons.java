//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2026
// Author:  Rohin Gosling
//
// Description:
//
//   Loads the curated Microsoft Fluent UI System Icons SVG resources as native JavaFX vector graphics.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.shape.SVGPath;

//*********************************************************************************************************************
// Class: ClientFluentIcons
//
// Description:
//
//   Creates independent JavaFX SVGPath nodes from the application icon resources. The path data is cached, while every
//   caller receives a new Node so graphics can be attached safely to different controls.
//
//*********************************************************************************************************************

final class ClientFluentIcons
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    static final int MENU_SIZE    = 16;
    static final int TOOLBAR_SIZE = 20;

    private static final Pattern SVG_PATH_PATTERN = Pattern.compile ( "<path\\s+[^>]*d=\\\"([^\\\"]+)\\\"" );
    private static final Map <String, String> PATH_CACHE = new ConcurrentHashMap <> ();

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    private ClientFluentIcons ()
    {
        // This class supplies static factory behavior and must not be instantiated.
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    static Node create ( int size, String semanticName )
    {
        if ( size != MENU_SIZE && size != TOOLBAR_SIZE )
        {
            throw new IllegalArgumentException ( "Fluent icons are available only at 16 or 20 pixels." );
        }

        String resourceName = "icons/" + size + "/ic_fluent_" + semanticName + "_" + size + "_regular.svg";
        SVGPath icon = new SVGPath ();

        icon.setContent ( PATH_CACHE.computeIfAbsent ( resourceName, ClientFluentIcons::loadPathContent ) );
        icon.getStyleClass ().add ( "fluent-icon" );
        icon.setFocusTraversable ( false );
        icon.setMouseTransparent ( true );

        Pane iconCanvas = new Pane ( icon );

        iconCanvas.setMinSize ( size, size );
        iconCanvas.setPrefSize ( size, size );
        iconCanvas.setMaxSize ( size, size );
        iconCanvas.getStyleClass ().addAll ( "fluent-icon-canvas", "fluent-icon-" + size );
        iconCanvas.setFocusTraversable ( false );
        iconCanvas.setMouseTransparent ( true );

        return iconCanvas;
    }

    static Node createPlaceholder ( int size )
    {
        if ( size != MENU_SIZE && size != TOOLBAR_SIZE )
        {
            throw new IllegalArgumentException ( "Fluent icons are available only at 16 or 20 pixels." );
        }

        Pane iconCanvas = new Pane ();

        iconCanvas.setMinSize ( size, size );
        iconCanvas.setPrefSize ( size, size );
        iconCanvas.setMaxSize ( size, size );
        iconCanvas.getStyleClass ().addAll ( "fluent-icon-canvas", "fluent-icon-" + size );
        iconCanvas.setFocusTraversable ( false );
        iconCanvas.setMouseTransparent ( true );

        return iconCanvas;
    }

    private static String loadPathContent ( String resourceName )
    {
        try ( InputStream resourceStream = ClientFluentIcons.class.getResourceAsStream ( resourceName ) )
        {
            if ( resourceStream == null )
            {
                throw new IllegalStateException ( "Missing Fluent icon resource: " + resourceName );
            }

            String svgDocument = new String ( resourceStream.readAllBytes (), StandardCharsets.UTF_8 );
            Matcher pathMatcher = SVG_PATH_PATTERN.matcher ( svgDocument );

            if ( !pathMatcher.find () )
            {
                throw new IllegalStateException ( "The Fluent icon has no SVG path: " + resourceName );
            }

            return pathMatcher.group ( 1 );
        }

        catch ( IOException exception )
        {
            throw new IllegalStateException ( "The Fluent icon could not be read: " + resourceName, exception );
        }
    }
}
