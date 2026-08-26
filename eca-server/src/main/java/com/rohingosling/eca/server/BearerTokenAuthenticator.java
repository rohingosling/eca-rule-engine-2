//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Authenticates bearer credentials using a constant-time comparison of fixed-length token digests.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

//*********************************************************************************************************************
// Class: BearerTokenAuthenticator
//
// Description:
//
//   Authenticates bearer credentials using a constant-time comparison of fixed-length token digests.
//
//*********************************************************************************************************************

final class BearerTokenAuthenticator
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final byte [] expectedDigest;

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: BearerTokenAuthenticator
    //
    // Description:
    //
    //   Creates the BearerTokenAuthenticator instance from the supplied values.
    //
    // Arguments:
    //
    //   expectedToken (String):
    //     The expected token to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    BearerTokenAuthenticator ( String expectedToken )
    {
        // Update the expected digest from the require non null result.

        this.expectedDigest = digest ( Objects.requireNonNull ( expectedToken, "expectedToken" ) );
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: authenticate
    //
    // Description:
    //
    //   Performs the authenticate operation.
    //
    // Arguments:
    //
    //   suppliedToken (String):
    //     The supplied token to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    boolean authenticate ( String suppliedToken )
    {
        // Initialize the supplied digest by applying digest.

        byte [] suppliedDigest = digest ( suppliedToken == null ? "" : suppliedToken );

        // Return whether equal.

        return MessageDigest.isEqual ( this.expectedDigest, suppliedDigest );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: digest
    //
    // Description:
    //
    //   Performs the digest operation.
    //
    // Arguments:
    //
    //   token (String):
    //     The token to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static byte [] digest ( String token )
    {
        try
        {
            // Return the result produced by digest.

            return MessageDigest.getInstance ( "SHA-256" )
                .digest ( token.getBytes ( StandardCharsets.UTF_8 ) );
        }

        // Handle no such algorithm failures captured as exception.

        catch ( NoSuchAlgorithmException exception )
        {
            throw new IllegalStateException ( "SHA-256 is unavailable.", exception );
        }
    }
}
