//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Defines validated listener, persistence, credential, request-limit, and lifecycle configuration.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.server;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

//*********************************************************************************************************************
// Class: ServerConfiguration
//
// Description:
//
//   Defines validated listener, persistence, credential, request-limit, and lifecycle configuration.
//
//*********************************************************************************************************************

public final class ServerConfiguration
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    public static final int DEFAULT_MODEL_BODY_BYTES      = 16 * 1024 * 1024;
    public static final int DEFAULT_EVALUATION_BODY_BYTES = 1024 * 1024;
    public static final int DEFAULT_HEADER_BYTES          = 32 * 1024;

    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final String host;
    private final int port;
    private final Path dataDirectory;
    private final Path tokenFile;
    private final String configuredBearerToken;
    private final Set <String> allowedOrigins;
    private final int maximumModelBodyBytes;
    private final int maximumEvaluationBodyBytes;
    private final int maximumHeaderBytes;
    private final Duration requestTimeout;
    private final Duration shutdownGracePeriod;

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getHost
    //
    // Description:
    //
    //   Returns the host.
    //
    // Returns:
    //
    //   The host.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String getHost ()
    {
        // Return the host to the caller.

        return this.host;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getPort
    //
    // Description:
    //
    //   Returns the port.
    //
    // Returns:
    //
    //   The port.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public int getPort ()
    {
        // Return the port to the caller.

        return this.port;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getDataDirectory
    //
    // Description:
    //
    //   Returns the data directory.
    //
    // Returns:
    //
    //   The data directory.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Path getDataDirectory ()
    {
        // Return the data directory to the caller.

        return this.dataDirectory;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getTokenFile
    //
    // Description:
    //
    //   Returns the token file.
    //
    // Returns:
    //
    //   The token file.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Path getTokenFile ()
    {
        // Return the token file to the caller.

        return this.tokenFile;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getConfiguredBearerToken
    //
    // Description:
    //
    //   Returns the configured bearer token.
    //
    // Returns:
    //
    //   The configured bearer token.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String getConfiguredBearerToken ()
    {
        // Return the configured bearer token to the caller.

        return this.configuredBearerToken;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getAllowedOrigins
    //
    // Description:
    //
    //   Returns the exact browser origins allowed to read cross-origin responses.
    //
    // Returns:
    //
    //   The immutable allowed-origin collection.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Set <String> getAllowedOrigins ()
    {
        // Return the allowed origins to the caller.

        return this.allowedOrigins;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getMaximumModelBodyBytes
    //
    // Description:
    //
    //   Returns the maximum model body bytes.
    //
    // Returns:
    //
    //   The maximum model body bytes.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public int getMaximumModelBodyBytes ()
    {
        // Return the maximum model body bytes to the caller.

        return this.maximumModelBodyBytes;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getMaximumEvaluationBodyBytes
    //
    // Description:
    //
    //   Returns the maximum evaluation body bytes.
    //
    // Returns:
    //
    //   The maximum evaluation body bytes.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public int getMaximumEvaluationBodyBytes ()
    {
        // Return the maximum evaluation body bytes to the caller.

        return this.maximumEvaluationBodyBytes;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getMaximumHeaderBytes
    //
    // Description:
    //
    //   Returns the maximum header bytes.
    //
    // Returns:
    //
    //   The maximum header bytes.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public int getMaximumHeaderBytes ()
    {
        // Return the maximum header bytes to the caller.

        return this.maximumHeaderBytes;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getRequestTimeout
    //
    // Description:
    //
    //   Returns the request timeout.
    //
    // Returns:
    //
    //   The request timeout.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Duration getRequestTimeout ()
    {
        // Return the request timeout to the caller.

        return this.requestTimeout;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getShutdownGracePeriod
    //
    // Description:
    //
    //   Returns the shutdown grace period.
    //
    // Returns:
    //
    //   The shutdown grace period.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Duration getShutdownGracePeriod ()
    {
        // Return the shutdown grace period to the caller.

        return this.shutdownGracePeriod;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/2: ServerConfiguration
    //
    // Description:
    //
    //   Creates the ServerConfiguration instance from the supplied values.
    //
    // Arguments:
    //
    //   host (String):
    //     The host to use.
    //
    //   port (int):
    //     The port to use.
    //
    //   dataDirectory (Path):
    //     The data directory to use.
    //
    //   tokenFile (Path):
    //     The token file to use.
    //
    //   configuredBearerToken (String):
    //     The configured bearer token to use.
    //
    //   maximumModelBodyBytes (int):
    //     The maximum model body bytes to use.
    //
    //   maximumEvaluationBodyBytes (int):
    //     The maximum evaluation body bytes to use.
    //
    //   maximumHeaderBytes (int):
    //     The maximum header bytes to use.
    //
    //   requestTimeout (Duration):
    //     The request timeout to use.
    //
    //   shutdownGracePeriod (Duration):
    //     The shutdown grace period to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ServerConfiguration (
        String host,
        int port,
        Path dataDirectory,
        Path tokenFile,
        String configuredBearerToken,
        int maximumModelBodyBytes,
        int maximumEvaluationBodyBytes,
        int maximumHeaderBytes,
        Duration requestTimeout,
        Duration shutdownGracePeriod
    )
    {
        // Delegate to the complete constructor with browser access disabled by default.

        this (
            host,
            port,
            dataDirectory,
            tokenFile,
            configuredBearerToken,
            Set.of (),
            maximumModelBodyBytes,
            maximumEvaluationBodyBytes,
            maximumHeaderBytes,
            requestTimeout,
            shutdownGracePeriod
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 2/2: ServerConfiguration
    //
    // Description:
    //
    //   Creates the ServerConfiguration instance from the supplied values and exact browser-origin allowlist.
    //
    // Arguments:
    //
    //   host (String):
    //     The host to use.
    //
    //   port (int):
    //     The port to use.
    //
    //   dataDirectory (Path):
    //     The data directory to use.
    //
    //   tokenFile (Path):
    //     The token file to use.
    //
    //   configuredBearerToken (String):
    //     The configured bearer token to use.
    //
    //   allowedOrigins (Collection <String>):
    //     The exact browser origins allowed to read cross-origin responses.
    //
    //   maximumModelBodyBytes (int):
    //     The maximum model body bytes to use.
    //
    //   maximumEvaluationBodyBytes (int):
    //     The maximum evaluation body bytes to use.
    //
    //   maximumHeaderBytes (int):
    //     The maximum header bytes to use.
    //
    //   requestTimeout (Duration):
    //     The request timeout to use.
    //
    //   shutdownGracePeriod (Duration):
    //     The shutdown grace period to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ServerConfiguration (
        String host,
        int port,
        Path dataDirectory,
        Path tokenFile,
        String configuredBearerToken,
        Collection <String> allowedOrigins,
        int maximumModelBodyBytes,
        int maximumEvaluationBodyBytes,
        int maximumHeaderBytes,
        Duration requestTimeout,
        Duration shutdownGracePeriod
    )
    {
        // Perform the require text, require port, normalize, to absolute path, require non null, normalize optional
        // text, require positive, and require positive duration calls required by the server configuration operation.

        this.host                       = requireText ( host, "host" );
        this.port                       = requirePort ( port );
        this.dataDirectory              = Objects.requireNonNull ( dataDirectory, "dataDirectory" )
            .toAbsolutePath ()
            .normalize ();
        this.tokenFile                  = tokenFile == null
            ? null
            : tokenFile.toAbsolutePath ().normalize ();
        this.configuredBearerToken      = normalizeOptionalText ( configuredBearerToken );
        this.allowedOrigins             = validateAllowedOrigins ( allowedOrigins );
        this.maximumModelBodyBytes      = requirePositive (
            maximumModelBodyBytes,
            "maximumModelBodyBytes"
        );
        this.maximumEvaluationBodyBytes = requirePositive (
            maximumEvaluationBodyBytes,
            "maximumEvaluationBodyBytes"
        );
        this.maximumHeaderBytes         = requirePositive ( maximumHeaderBytes, "maximumHeaderBytes" );
        this.requestTimeout             = requirePositiveDuration ( requestTimeout, "requestTimeout" );
        this.shutdownGracePeriod        = requirePositiveDuration (
            shutdownGracePeriod,
            "shutdownGracePeriod"
        );

        // Reject the operation when maximum evaluation body bytes exceeds maximum model body bytes.

        if ( this.maximumEvaluationBodyBytes > this.maximumModelBodyBytes )
        {
            throw new IllegalArgumentException (
                "The evaluation body limit must not exceed the model body limit."
            );
        }
    }

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: isLoopbackListener
    //
    // Description:
    //
    //   Indicates whether loopback listener.
    //
    // Returns:
    //
    //   `true` when the condition is satisfied; otherwise `false`.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public boolean isLoopbackListener ()
    {
        try
        {
            // Return whether loopback address.

            return InetAddress.getByName ( this.host ).isLoopbackAddress ();
        }

        // Handle unknown host failures captured as exception.

        catch ( UnknownHostException exception )
        {
            throw new IllegalArgumentException (
                "The listener host could not be resolved: " + this.host,
                exception
            );
        }
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: resolveControlTokenFile
    //
    // Description:
    //
    //   Performs the resolve control token file operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Path resolveControlTokenFile ()
    {
        // Return the value selected according to token file is unavailable.

        return this.tokenFile == null
            ? this.dataDirectory.resolve ( "control-token" )
            : this.tokenFile;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: defaultDataDirectory
    //
    // Description:
    //
    //   Performs the default data directory operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static Path defaultDataDirectory ()
    {
        // Initialize the local application data by applying getenv.

        String localApplicationData = System.getenv ( "LOCALAPPDATA" );

        // Stop this path and return its result when local application data is available and local application data
        // contains text.

        if ( localApplicationData != null && !localApplicationData.isBlank () )
        {
            // Return the result produced by of when local application data is available and local application data
            // contains text.

            return Path.of ( localApplicationData, "EcaRuleEngine2" );
        }

        // Return the result produced by of.

        return Path.of (
            System.getProperty ( "user.home" ),
            "AppData",
            "Local",
            "EcaRuleEngine2"
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requireText
    //
    // Description:
    //
    //   Performs the require text operation.
    //
    // Arguments:
    //
    //   value (String):
    //     The value to use.
    //
    //   fieldName (String):
    //     The field name to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String requireText ( String value, String fieldName )
    {
        // Validate the required value before continuing.

        Objects.requireNonNull ( value, fieldName );

        // Initialize the normalized value by applying trim.

        String normalizedValue = value.trim ();

        // Reject the operation when normalized value contains no values.

        if ( normalizedValue.isEmpty () )
        {
            throw new IllegalArgumentException ( fieldName + " must not be blank." );
        }

        // Return the normalized value to the caller.

        return normalizedValue;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: normalizeOptionalText
    //
    // Description:
    //
    //   Performs the normalize optional text operation.
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

    private static String normalizeOptionalText ( String value )
    {
        // Stop this path and return its result when value is unavailable.

        if ( value == null )
        {
            // Return a null result to indicate that no value is available when value is unavailable.

            return null;
        }

        // Initialize the normalized value by applying trim.

        String normalizedValue = value.trim ();

        // Return the value selected according to normalized value contains no values.

        return normalizedValue.isEmpty () ? null : normalizedValue;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validateAllowedOrigins
    //
    // Description:
    //
    //   Validates and normalizes an exact browser-origin allowlist.
    //
    // Arguments:
    //
    //   values (Collection <String>):
    //     The configured origin values.
    //
    // Returns:
    //
    //   An immutable normalized origin collection.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Set <String> validateAllowedOrigins ( Collection <String> values )
    {
        // Validate the configured collection before processing its entries.

        Objects.requireNonNull ( values, "allowedOrigins" );

        // Initialize the normalized origins while preserving command-line order and removing duplicates.

        LinkedHashSet <String> normalizedOrigins = new LinkedHashSet <> ();

        // Process each configured origin supplied by the caller.

        for ( String value : values )
        {
            normalizedOrigins.add ( validateAllowedOrigin ( value ) );
        }

        // Return an immutable exact-origin collection to the caller.

        return Collections.unmodifiableSet ( normalizedOrigins );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: validateAllowedOrigin
    //
    // Description:
    //
    //   Validates and normalizes one exact HTTP or HTTPS browser origin.
    //
    // Arguments:
    //
    //   value (String):
    //     The configured origin value.
    //
    // Returns:
    //
    //   The normalized origin serialization.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String validateAllowedOrigin ( String value )
    {
        // Normalize required text and reject special CORS origin forms before parsing.

        String origin = requireText ( value, "allowedOrigin" );

        if ( origin.equals ( "*" ) || origin.equalsIgnoreCase ( "null" ) )
        {
            throw new IllegalArgumentException ( "allowedOrigin must be one exact HTTP or HTTPS origin." );
        }

        try
        {
            // Parse and validate every component admitted by an origin serialization.

            URI originURI = new URI ( origin );
            String scheme = originURI.getScheme ();
            String host   = originURI.getHost ();

            if (
                !originURI.isAbsolute ()
                    || originURI.isOpaque ()
                    || scheme == null
                    || host == null
                    || !( scheme.equalsIgnoreCase ( "http" ) || scheme.equalsIgnoreCase ( "https" ) )
                    || originURI.getRawUserInfo () != null
                    || ( originURI.getRawPath () != null && !originURI.getRawPath ().isEmpty () )
                    || originURI.getRawQuery () != null
                    || originURI.getRawFragment () != null
                    || originURI.getPort () > 65535
                    || originURI.getRawAuthority ().endsWith ( ":" )
            )
            {
                throw new IllegalArgumentException ( "allowedOrigin must be one exact HTTP or HTTPS origin." );
            }

            // Return the browser-compatible ASCII origin serialization with normalized scheme and host casing.

            return new URI (
                scheme.toLowerCase ( Locale.ROOT ),
                null,
                host.toLowerCase ( Locale.ROOT ),
                originURI.getPort (),
                null,
                null,
                null
            ).toASCIIString ();
        }

        // Handle malformed URI failures captured as exceptions.

        catch ( URISyntaxException exception )
        {
            throw new IllegalArgumentException (
                "allowedOrigin must be one exact HTTP or HTTPS origin.",
                exception
            );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requirePort
    //
    // Description:
    //
    //   Performs the require port operation.
    //
    // Arguments:
    //
    //   value (int):
    //     The value to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static int requirePort ( int value )
    {
        // Reject the operation when value is less than 0 or value exceeds 65535.

        if ( value < 0 || value > 65535 )
        {
            throw new IllegalArgumentException ( "port must be between 0 and 65535." );
        }

        // Return the value to the caller.

        return value;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requirePositive
    //
    // Description:
    //
    //   Performs the require positive operation.
    //
    // Arguments:
    //
    //   value (int):
    //     The value to use.
    //
    //   fieldName (String):
    //     The field name to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static int requirePositive ( int value, String fieldName )
    {
        // Reject the operation when value is at most 0.

        if ( value <= 0 )
        {
            throw new IllegalArgumentException ( fieldName + " must be positive." );
        }

        // Return the value to the caller.

        return value;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requirePositiveDuration
    //
    // Description:
    //
    //   Performs the require positive duration operation.
    //
    // Arguments:
    //
    //   value (Duration):
    //     The value to use.
    //
    //   fieldName (String):
    //     The field name to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Duration requirePositiveDuration ( Duration value, String fieldName )
    {
        // Validate the required value before continuing.

        Objects.requireNonNull ( value, fieldName );

        // Reject the operation when value is zero or value is negative.

        if ( value.isZero () || value.isNegative () )
        {
            throw new IllegalArgumentException ( fieldName + " must be positive." );
        }

        // Return the value to the caller.

        return value;
    }
}
