//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Persists non-sensitive desktop connection and window preferences with bounded restoration.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.client;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

//*********************************************************************************************************************
// Class: ClientPreferencesStore
//
// Description:
//
//   Persists non-sensitive desktop connection and window preferences with bounded restoration.
//
//*********************************************************************************************************************

public final class ClientPreferencesStore
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    private static final String BASE_URL_KEY                    = "server-base-url";
    private static final String CONNECT_TIMEOUT_KEY             = "connect-timeout-seconds";
    private static final String REQUEST_TIMEOUT_KEY             = "request-timeout-seconds";
    private static final String THEME_KEY                       = "theme";
    private static final String LAST_DIRECTORY_KEY              = "last-directory";
    private static final String WINDOW_X_KEY                    = "window-x";
    private static final String WINDOW_Y_KEY                    = "window-y";
    private static final String WINDOW_WIDTH_KEY                = "window-width";
    private static final String WINDOW_HEIGHT_KEY               = "window-height";
    private static final String MASTER_DETAIL_SPLITTER_KEY      = "splitter-position";
    private static final String USER_GUIDE_SPLITTER_KEY         = "user-guide-splitter-position";
    private static final String DIAGNOSTICS_SPLITTER_KEY        = "diagnostics-splitter-position";
    private static final String TREE_EXPANDED_KEY               = "tree-expanded";
    private static final String TREE_CATEGORIES_KEY             = "tree-expanded-categories";
    private static final double USER_GUIDE_SPLITTER_DEFAULT     = 0.70;
    private static final double DIAGNOSTICS_SPLITTER_DEFAULT    = 0.72;

    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final Preferences preferences;

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/2: ClientPreferencesStore
    //
    // Description:
    //
    //   Creates the ClientPreferencesStore instance from the supplied values.
    //
    //-----------------------------------------------------------------------------------------------------------------

    ClientPreferencesStore ()
    {
        // Apply this and user node for package to the preferences for the client preferences store operation.

        this ( Preferences.userNodeForPackage ( ClientMain.class ) );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 2/2: ClientPreferencesStore
    //
    // Description:
    //
    //   Creates the ClientPreferencesStore instance from the supplied values.
    //
    // Arguments:
    //
    //   preferences (Preferences):
    //     The preferences to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    ClientPreferencesStore ( Preferences preferences )
    {
        // Validate the required preferences before continuing.

        this.preferences = Objects.requireNonNull ( preferences, "preferences" );
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: loadConnectionSettings
    //
    // Description:
    //
    //   Performs the load connection settings operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    ClientConnectionSettings loadConnectionSettings ()
    {
        try
        {
            // Return a newly constructed client connection settings containing the operation result.

            return new ClientConnectionSettings (
                URI.create (
                    this.preferences.get (
                        BASE_URL_KEY,
                        ClientConnectionSettings.DEFAULT_BASE_URI.toString ()
                    )
                ),
                Duration.ofSeconds (
                    this.preferences.getLong (
                        CONNECT_TIMEOUT_KEY,
                        ClientConnectionSettings.DEFAULT_CONNECT_TIMEOUT.toSeconds ()
                    )
                ),
                Duration.ofSeconds (
                    this.preferences.getLong (
                        REQUEST_TIMEOUT_KEY,
                        ClientConnectionSettings.DEFAULT_REQUEST_TIMEOUT.toSeconds ()
                    )
                ),
                ""
            );
        }

        // Handle illegal argument failures captured as exception.

        catch ( IllegalArgumentException exception )
        {
            // Return the result produced by defaults.

            return ClientConnectionSettings.defaults ();
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: saveConnectionSettings
    //
    // Description:
    //
    //   Performs the save connection settings operation.
    //
    // Arguments:
    //
    //   settings (ClientConnectionSettings):
    //     The settings to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    void saveConnectionSettings ( ClientConnectionSettings settings )
    {
        // Perform the require non null, put, to string, get base URI, put long, to seconds, get connect timeout, and
        // get request timeout calls required by the save connection settings operation.

        Objects.requireNonNull ( settings, "settings" );

        this.preferences.put ( BASE_URL_KEY, settings.getBaseUri ().toString () );
        this.preferences.putLong ( CONNECT_TIMEOUT_KEY, settings.getConnectTimeout ().toSeconds () );
        this.preferences.putLong ( REQUEST_TIMEOUT_KEY, settings.getRequestTimeout ().toSeconds () );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: loadThemeIdentifier
    //
    // Description:
    //
    //   Performs the load theme identifier operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    String loadThemeIdentifier ()
    {
        // Return the result produced by sanitize theme identifier.

        return sanitizeThemeIdentifier ( this.preferences.get ( THEME_KEY, "light" ) );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: saveThemeIdentifier
    //
    // Description:
    //
    //   Performs the save theme identifier operation.
    //
    // Arguments:
    //
    //   themeIdentifier (String):
    //     The theme identifier to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    void saveThemeIdentifier ( String themeIdentifier )
    {
        // Store sanitize theme identifier theme identifier under theme key in the preferences.

        this.preferences.put ( THEME_KEY, sanitizeThemeIdentifier ( themeIdentifier ) );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: flush
    //
    // Description:
    //
    //   Flushes pending preference updates to their backing store.
    //
    //-----------------------------------------------------------------------------------------------------------------

    void flush ()
    {
        try
        {
            this.preferences.flush ();
        }

        catch ( BackingStoreException exception )
        {
            throw new IllegalStateException ( "The client preferences could not be persisted.", exception );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: loadLastDirectory
    //
    // Description:
    //
    //   Performs the load last directory operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    Optional <Path> loadLastDirectory ()
    {
        // Initialize the stored directory by applying get.

        String storedDirectory = this.preferences.get ( LAST_DIRECTORY_KEY, "" );

        // Stop this path and return its result when stored directory is blank.

        if ( storedDirectory.isBlank () )
        {
            // Return an empty optional result because no value is available when stored directory is blank.

            return Optional.empty ();
        }

        try
        {
            // Initialize the directory by applying of.

            Path directory = Path.of ( storedDirectory );

            // Return the value selected according to files is directory.

            return Files.isDirectory ( directory ) ? Optional.of ( directory ) : Optional.empty ();
        }

        // Handle runtime failures captured as exception.

        catch ( RuntimeException exception )
        {
            // Return an empty optional result because no value is available.

            return Optional.empty ();
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: rememberDirectory
    //
    // Description:
    //
    //   Performs the remember directory operation.
    //
    // Arguments:
    //
    //   filePath (Path):
    //     The file path to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    void rememberDirectory ( Path filePath )
    {
        // Initialize the parent directory by applying get parent, to absolute path, and require non null.

        Path parentDirectory = Objects.requireNonNull ( filePath, "filePath" )
            .toAbsolutePath ()
            .getParent ();

        // Handle the branch where parent directory is available.

        if ( parentDirectory != null )
        {
            // Store parent directory to string under last directory key in the preferences.

            this.preferences.put ( LAST_DIRECTORY_KEY, parentDirectory.toString () );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: loadWindowState
    //
    // Description:
    //
    //   Performs the load window state operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    WindowState loadWindowState ()
    {
        // Return a newly constructed window state containing the operation result.

        return new WindowState (
            sanitizeCoordinate ( this.preferences.getDouble ( WINDOW_X_KEY, Double.NaN ) ),
            sanitizeCoordinate ( this.preferences.getDouble ( WINDOW_Y_KEY, Double.NaN ) ),
            sanitizeDimension ( this.preferences.getDouble ( WINDOW_WIDTH_KEY, 1_320.0 ), 1_000.0, 4_000.0 ),
            sanitizeDimension ( this.preferences.getDouble ( WINDOW_HEIGHT_KEY, 720.0 ), 560.0, 2_500.0 ),
            sanitizeDividerPosition ( this.preferences.getDouble ( MASTER_DETAIL_SPLITTER_KEY, 0.24 ) ),
            sanitizeUserGuideDividerPosition (
                this.preferences.getDouble (
                    USER_GUIDE_SPLITTER_KEY,
                    USER_GUIDE_SPLITTER_DEFAULT
                )
            ),
            sanitizeDiagnosticsDividerPosition (
                this.preferences.getDouble (
                    DIAGNOSTICS_SPLITTER_KEY,
                    DIAGNOSTICS_SPLITTER_DEFAULT
                )
            ),
            this.preferences.getBoolean ( TREE_EXPANDED_KEY, true ),
            readExpandedCategories ()
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: saveWindowState
    //
    // Description:
    //
    //   Performs the save window state operation.
    //
    // Arguments:
    //
    //   windowState (WindowState):
    //     The window state to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    void saveWindowState ( WindowState windowState )
    {
        // Validate the required window state before continuing.

        Objects.requireNonNull ( windowState, "windowState" );

        // Handle the branch where double is finite.

        if ( Double.isFinite ( windowState.getX () ) )
        {
            // Perform the put double and get x calls required by the save window state operation.

            this.preferences.putDouble ( WINDOW_X_KEY, windowState.getX () );
        }

        // Handle the branch where double is finite.

        if ( Double.isFinite ( windowState.getY () ) )
        {
            // Perform the put double and get y calls required by the save window state operation.

            this.preferences.putDouble ( WINDOW_Y_KEY, windowState.getY () );
        }

        // Perform the put double, get width, get height, get master detail splitter position, get context-sensitive
        // help splitter
        // position, get diagnostics splitter position, put boolean, is tree expanded, put, collect, stream, get
        // expanded categories, and joining calls required by the save window state operation.

        this.preferences.putDouble ( WINDOW_WIDTH_KEY, windowState.getWidth () );
        this.preferences.putDouble ( WINDOW_HEIGHT_KEY, windowState.getHeight () );
        this.preferences.putDouble (
            MASTER_DETAIL_SPLITTER_KEY,
            windowState.getMasterDetailSplitterPosition ()
        );
        this.preferences.putDouble (
            USER_GUIDE_SPLITTER_KEY,
            windowState.getUserGuideSplitterPosition ()
        );
        this.preferences.putDouble (
            DIAGNOSTICS_SPLITTER_KEY,
            windowState.getDiagnosticsSplitterPosition ()
        );
        this.preferences.putBoolean ( TREE_EXPANDED_KEY, windowState.isTreeExpanded () );
        this.preferences.put (
            TREE_CATEGORIES_KEY,
            windowState.getExpandedCategories ().stream ().collect ( Collectors.joining ( "," ) )
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: sanitizeDividerPosition
    //
    // Description:
    //
    //   Performs the sanitize divider position operation.
    //
    // Arguments:
    //
    //   dividerPosition (double):
    //     The divider position to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static double sanitizeDividerPosition ( double dividerPosition )
    {
        // Return the value selected according to double is finite and divider position is at least 0 12 and divider
        // position is at most 0 6.

        return Double.isFinite ( dividerPosition ) && dividerPosition >= 0.12 && dividerPosition <= 0.60
            ? dividerPosition
            : 0.24;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: sanitizeDiagnosticsDividerPosition
    //
    // Description:
    //
    //   Performs the sanitize diagnostics divider position operation.
    //
    // Arguments:
    //
    //   dividerPosition (double):
    //     The divider position to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static double sanitizeDiagnosticsDividerPosition ( double dividerPosition )
    {
        // Return the value selected according to double is finite and divider position is at least 0 45 and divider
        // position is at most 0 9.

        return Double.isFinite ( dividerPosition ) && dividerPosition >= 0.45 && dividerPosition <= 0.90
            ? dividerPosition
            : DIAGNOSTICS_SPLITTER_DEFAULT;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: sanitizeUserGuideDividerPosition
    //
    // Description:
    //
    //   Performs the sanitize context-sensitive help divider position operation.
    //
    // Arguments:
    //
    //   dividerPosition (double):
    //     The divider position to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static double sanitizeUserGuideDividerPosition ( double dividerPosition )
    {
        // Return the value selected according to double is finite and divider position is at least 0 55 and divider
        // position is at most 0 9.

        return Double.isFinite ( dividerPosition ) && dividerPosition >= 0.55 && dividerPosition <= 0.90
            ? dividerPosition
            : USER_GUIDE_SPLITTER_DEFAULT;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: sanitizeDimension
    //
    // Description:
    //
    //   Performs the sanitize dimension operation.
    //
    // Arguments:
    //
    //   dimension (double):
    //     The dimension to use.
    //
    //   minimum (double):
    //     The minimum to use.
    //
    //   maximum (double):
    //     The maximum to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static double sanitizeDimension ( double dimension, double minimum, double maximum )
    {
        // Return the value selected according to double is finite and dimension is at least minimum and dimension is
        // at most maximum.

        return Double.isFinite ( dimension ) && dimension >= minimum && dimension <= maximum
            ? dimension
            : minimum;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: sanitizeThemeIdentifier
    //
    // Description:
    //
    //   Performs the sanitize theme identifier operation.
    //
    // Arguments:
    //
    //   themeIdentifier (String):
    //     The theme identifier to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static String sanitizeThemeIdentifier ( String themeIdentifier )
    {
        // Return the value selected according to "dark" matches theme identifier.

        return "dark".equals ( themeIdentifier ) ? "dark" : "light";
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: sanitizeCoordinate
    //
    // Description:
    //
    //   Performs the sanitize coordinate operation.
    //
    // Arguments:
    //
    //   coordinate (double):
    //     The coordinate to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static double sanitizeCoordinate ( double coordinate )
    {
        // Return the value selected according to double is finite and math abs coordinate is at most 100000 0.

        return Double.isFinite ( coordinate ) && Math.abs ( coordinate ) <= 100_000.0
            ? coordinate
            : Double.NaN;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readExpandedCategories
    //
    // Description:
    //
    //   Performs the read expanded categories operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private Set <String> readExpandedCategories ()
    {
        // Initialize the stored categories by applying get.

        String storedCategories = this.preferences.get ( TREE_CATEGORIES_KEY, "" );

        // Stop this path and return its result when stored categories is blank.

        if ( storedCategories.isBlank () )
        {
            // Return the result produced by of when stored categories is blank.

            return Set.of ();
        }

        // Return the result produced by collect.

        return Arrays.stream ( storedCategories.split ( "," ) )
            .map ( String::trim )
            .filter ( categoryIdentifier -> !categoryIdentifier.isBlank () )
            .collect ( Collectors.toCollection ( LinkedHashSet::new ) );
    }

    //*****************************************************************************************************************
    // Class: WindowState
    //
    // Description:
    //
    //   Provides the window state behavior.
    //
    //*****************************************************************************************************************

    static final class WindowState
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final double       x;
        private final double       y;
        private final double       width;
        private final double       height;
        private final double       masterDetailSplitterPosition;
        private final double       userGuideSplitterPosition;
        private final double       diagnosticsSplitterPosition;
        private final boolean      treeExpanded;
        private final Set <String> expandedCategories;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getX
        //
        // Description:
        //
        //   Returns the x.
        //
        // Returns:
        //
        //   The x.
        //
        //-------------------------------------------------------------------------------------------------------------

        double getX ()
        {
            // Return the x to the caller.

            return this.x;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getY
        //
        // Description:
        //
        //   Returns the y.
        //
        // Returns:
        //
        //   The y.
        //
        //-------------------------------------------------------------------------------------------------------------

        double getY ()
        {
            // Return the y to the caller.

            return this.y;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getWidth
        //
        // Description:
        //
        //   Returns the width.
        //
        // Returns:
        //
        //   The width.
        //
        //-------------------------------------------------------------------------------------------------------------

        double getWidth ()
        {
            // Return the width to the caller.

            return this.width;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getHeight
        //
        // Description:
        //
        //   Returns the height.
        //
        // Returns:
        //
        //   The height.
        //
        //-------------------------------------------------------------------------------------------------------------

        double getHeight ()
        {
            // Return the height to the caller.

            return this.height;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getMasterDetailSplitterPosition
        //
        // Description:
        //
        //   Returns the master detail splitter position.
        //
        // Returns:
        //
        //   The master detail splitter position.
        //
        //-------------------------------------------------------------------------------------------------------------

        double getMasterDetailSplitterPosition ()
        {
            // Return the master detail splitter position to the caller.

            return this.masterDetailSplitterPosition;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getUserGuideSplitterPosition
        //
        // Description:
        //
        //   Returns the context-sensitive help splitter position.
        //
        // Returns:
        //
        //   The context-sensitive help splitter position.
        //
        //-------------------------------------------------------------------------------------------------------------

        double getUserGuideSplitterPosition ()
        {
            // Return the context-sensitive help splitter position to the caller.

            return this.userGuideSplitterPosition;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getDiagnosticsSplitterPosition
        //
        // Description:
        //
        //   Returns the diagnostics splitter position.
        //
        // Returns:
        //
        //   The diagnostics splitter position.
        //
        //-------------------------------------------------------------------------------------------------------------

        double getDiagnosticsSplitterPosition ()
        {
            // Return the diagnostics splitter position to the caller.

            return this.diagnosticsSplitterPosition;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: isTreeExpanded
        //
        // Description:
        //
        //   Indicates whether tree expanded.
        //
        // Returns:
        //
        //   `true` when the condition is satisfied; otherwise `false`.
        //
        //-------------------------------------------------------------------------------------------------------------

        boolean isTreeExpanded ()
        {
            // Return the tree expanded to the caller.

            return this.treeExpanded;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getExpandedCategories
        //
        // Description:
        //
        //   Returns the expanded categories.
        //
        // Returns:
        //
        //   The expanded categories.
        //
        //-------------------------------------------------------------------------------------------------------------

        Set <String> getExpandedCategories ()
        {
            // Return the expanded categories to the caller.

            return this.expandedCategories;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: WindowState
        //
        // Description:
        //
        //   Creates the WindowState instance from the supplied values.
        //
        // Arguments:
        //
        //   x (double):
        //     The x to use.
        //
        //   y (double):
        //     The y to use.
        //
        //   width (double):
        //     The width to use.
        //
        //   height (double):
        //     The height to use.
        //
        //   masterDetailSplitterPosition (double):
        //     The master detail splitter position to use.
        //
        //   userGuideSplitterPosition (double):
        //     The context-sensitive help splitter position to use.
        //
        //   diagnosticsSplitterPosition (double):
        //     The diagnostics splitter position to use.
        //
        //   treeExpanded (boolean):
        //     The tree expanded to use.
        //
        //   expandedCategories (Set <String>):
        //     The expanded categories to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        WindowState (
            double x,
            double y,
            double width,
            double height,
            double masterDetailSplitterPosition,
            double userGuideSplitterPosition,
            double diagnosticsSplitterPosition,
            boolean treeExpanded,
            Set <String> expandedCategories
        )
        {
            this.x                            = x;
            this.y                            = y;
            this.width                        = width;
            this.height                       = height;

            // Apply sanitize divider position, max, sanitize context-sensitive help divider position, min, and
            // sanitize
            // diagnostics divider position to the math for the window state operation.

            this.masterDetailSplitterPosition = sanitizeDividerPosition ( masterDetailSplitterPosition );
            this.userGuideSplitterPosition    = Math.max (
                sanitizeUserGuideDividerPosition ( userGuideSplitterPosition ),
                Math.min ( 0.90, this.masterDetailSplitterPosition + 0.25 )
            );
            this.diagnosticsSplitterPosition  =
                sanitizeDiagnosticsDividerPosition ( diagnosticsSplitterPosition );
            this.treeExpanded                 = treeExpanded;

            // Update the expanded categories from the require non null result.

            this.expandedCategories           = Collections.unmodifiableSet (
                new LinkedHashSet <String> (
                    Objects.requireNonNull ( expandedCategories, "expandedCategories" )
                )
            );
        }
    }
}
