//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Represents validated server connection settings while keeping the bearer token in memory only.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.client;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

//*********************************************************************************************************************
// Class: ClientConnectionSettings
//
// Description:
//
//   Represents validated server connection settings while keeping the bearer token in memory only.
//
//*********************************************************************************************************************

public final class ClientConnectionSettings
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    public static final URI DEFAULT_BASE_URI = URI.create ( "http://127.0.0.1:8080/" );
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds ( 5 );
    public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds ( 30 );

    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final URI baseUri;
    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final String bearerToken;

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getBaseUri
    //
    // Description:
    //
    //   Returns the base uri.
    //
    // Returns:
    //
    //   The base uri.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public URI getBaseUri ()
    {
        // Return the base URI to the caller.

        return this.baseUri;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getConnectTimeout
    //
    // Description:
    //
    //   Returns the connect timeout.
    //
    // Returns:
    //
    //   The connect timeout.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Duration getConnectTimeout ()
    {
        // Return the connect timeout to the caller.

        return this.connectTimeout;
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
    // Method: getBearerToken
    //
    // Description:
    //
    //   Returns the bearer token.
    //
    // Returns:
    //
    //   The bearer token.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public String getBearerToken ()
    {
        // Return the bearer token to the caller.

        return this.bearerToken;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: ClientConnectionSettings
    //
    // Description:
    //
    //   Creates the ClientConnectionSettings instance from the supplied values.
    //
    // Arguments:
    //
    //   baseUri (URI):
    //     The base uri to use.
    //
    //   connectTimeout (Duration):
    //     The connect timeout to use.
    //
    //   requestTimeout (Duration):
    //     The request timeout to use.
    //
    //   bearerToken (String):
    //     The bearer token to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ClientConnectionSettings (
        URI baseUri,
        Duration connectTimeout,
        Duration requestTimeout,
        String bearerToken
    )
    {
        // Perform the normalize base URI, require positive, trim, and require non null calls required by the client
        // connection settings operation.

        this.baseUri        = normalizeBaseUri ( baseUri );
        this.connectTimeout = requirePositive ( connectTimeout, "connectTimeout" );
        this.requestTimeout = requirePositive ( requestTimeout, "requestTimeout" );
        this.bearerToken    = Objects.requireNonNull ( bearerToken, "bearerToken" ).trim ();
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: defaults
    //
    // Description:
    //
    //   Creates a ClientConnectionSettings instance with the default configuration.
    //
    // Returns:
    //
    //   The resulting ClientConnectionSettings instance.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static ClientConnectionSettings defaults ()
    {
        // Return a newly constructed client connection settings containing the operation result.

        return new ClientConnectionSettings (
            DEFAULT_BASE_URI,
            DEFAULT_CONNECT_TIMEOUT,
            DEFAULT_REQUEST_TIMEOUT,
            ""
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: equals
    //
    // Description:
    //
    //   Compares this value with another object for equality.
    //
    // Arguments:
    //
    //   other (Object):
    //     The other to use.
    //
    // Returns:
    //
    //   `true` when the objects are equal; otherwise `false`.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public boolean equals ( Object other )
    {
        // Stop this path and return its result when this equals other.

        if ( this == other )
        {
            // Return true for this outcome when this equals other.

            return true;
        }

        // Stop this path and return its result when other is not a client connection settings.

        if ( !( other instanceof ClientConnectionSettings ) )
        {
            // Return false for this outcome when other is not a client connection settings.

            return false;
        }

        ClientConnectionSettings otherSettings = (ClientConnectionSettings) other;

        // Return whether base URI matches other settings base URI and connect timeout matches other settings connect
        // timeout and request timeout matches other settings request timeout and bearer token matches other settings
        // bearer token.

        return this.baseUri.equals ( otherSettings.baseUri )
            && this.connectTimeout.equals ( otherSettings.connectTimeout )
            && this.requestTimeout.equals ( otherSettings.requestTimeout )
            && this.bearerToken.equals ( otherSettings.bearerToken );
    }

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: hashCode
    //
    // Description:
    //
    //   Calculates the hash code for this value.
    //
    // Returns:
    //
    //   The hash code for this value.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public int hashCode ()
    {
        // Return the stable hash code derived from the object's value components.

        return Objects.hash (
            this.baseUri,
            this.connectTimeout,
            this.requestTimeout,
            this.bearerToken
        );
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: normalizeBaseUri
    //
    // Description:
    //
    //   Performs the normalize base uri operation.
    //
    // Arguments:
    //
    //   baseUri (URI):
    //     The base uri to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static URI normalizeBaseUri ( URI baseUri )
    {
        // Prepare the required base URI and scheme values needed by the normalize base URI operation.

        URI requiredBaseUri = Objects.requireNonNull ( baseUri, "baseUri" );
        String scheme       = requiredBaseUri.getScheme ();

        // Reject the operation when scheme is unavailable or scheme equals ignore case does not succeed and scheme
        // equals ignore case does not succeed or required base URI host is unavailable.

        if (
            scheme == null
                || (
                    !scheme.equalsIgnoreCase ( "http" )
                        && !scheme.equalsIgnoreCase ( "https" )
                )
                || requiredBaseUri.getHost () == null
        )
        {
            throw new IllegalArgumentException ( "The server URL must be an absolute HTTP or HTTPS URL." );
        }

        // Initialize the normalized text by applying to string.

        String normalizedText = requiredBaseUri.toString ();

        // Handle the branch where normalized text ends with does not succeed.

        if ( !normalizedText.endsWith ( "/" ) )
        {
            normalizedText = normalizedText + "/";
        }

        // Return the result produced by create.

        return URI.create ( normalizedText );
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
    //   duration (Duration):
    //     The duration to use.
    //
    //   fieldName (String):
    //     The field name to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Duration requirePositive ( Duration duration, String fieldName )
    {
        // Validate the required duration before continuing.

        Duration requiredDuration = Objects.requireNonNull ( duration, fieldName );

        // Reject the operation when required duration is zero or required duration is negative.

        if ( requiredDuration.isZero () || requiredDuration.isNegative () )
        {
            throw new IllegalArgumentException ( fieldName + " must be greater than zero." );
        }

        // Return the required duration to the caller.

        return requiredDuration;
    }
}
