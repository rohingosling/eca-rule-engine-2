//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Creates and reads high-entropy bearer tokens stored in current-user-protected local files.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

//*********************************************************************************************************************
// Class: ControlTokenFile
//
// Description:
//
//   Creates and reads high-entropy bearer tokens stored in current-user-protected local files.
//
//*********************************************************************************************************************

public final class ControlTokenFile
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom ();

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: ControlTokenFile
    //
    // Description:
    //
    //   Creates the ControlTokenFile instance from the supplied values.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private ControlTokenFile ()
    {
        throw new UnsupportedOperationException ( "ControlTokenFile cannot be instantiated." );
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: loadOrCreate
    //
    // Description:
    //
    //   Performs the load or create operation.
    //
    // Arguments:
    //
    //   tokenFile (Path):
    //     The token file to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static String loadOrCreate ( Path tokenFile )
    {
        // Initialize the normalized token file by applying normalize.

        Path normalizedTokenFile = normalize ( tokenFile );

        // Handle the branch where files exists succeeds.

        if ( Files.exists ( normalizedTokenFile ) )
        {
            // Apply restrictive permissions to the control-token file.

            protect ( normalizedTokenFile );

            // Return the result produced by read validated when files exists succeeds.

            return readValidated ( normalizedTokenFile );
        }

        try
        {
            // Perform the create directories and get parent calls required by the load or create operation.

            Files.createDirectories ( normalizedTokenFile.getParent () );

            byte [] randomBytes = new byte [ TOKEN_BYTES ];

            // Fill the token buffer with cryptographically secure random bytes.

            SECURE_RANDOM.nextBytes ( randomBytes );

            // Initialize the token by applying encode to string, without padding, and get URL encoder.

            String token = Base64.getUrlEncoder ().withoutPadding ().encodeToString ( randomBytes );

            // Perform the write string, line separator, and protect calls required by the load or create operation.

            Files.writeString (
                normalizedTokenFile,
                token + System.lineSeparator (),
                StandardCharsets.UTF_8
            );
            protect ( normalizedTokenFile );

            // Return the token to the caller.

            return token;
        }

        // Handle I/O failures captured as exception.

        catch ( IOException exception )
        {
            throw new IllegalStateException (
                "The control-token file could not be created.",
                exception
            );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: loadExisting
    //
    // Description:
    //
    //   Performs the load existing operation.
    //
    // Arguments:
    //
    //   tokenFile (Path):
    //     The token file to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static String loadExisting ( Path tokenFile )
    {
        // Initialize the normalized token file by applying normalize.

        Path normalizedTokenFile = normalize ( tokenFile );

        // Reject the operation when files is not regular file.

        if ( !Files.isRegularFile ( normalizedTokenFile ) )
        {
            throw new IllegalArgumentException ( "The configured token file does not exist." );
        }

        // Return the result produced by read validated.

        return readValidated ( normalizedTokenFile );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: normalize
    //
    // Description:
    //
    //   Performs the normalize operation.
    //
    // Arguments:
    //
    //   tokenFile (Path):
    //     The token file to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Path normalize ( Path tokenFile )
    {
        // Initialize the normalized token file by applying normalize and to absolute path.

        Path normalizedTokenFile = tokenFile.toAbsolutePath ().normalize ();

        // Reject the operation when normalized token file parent is unavailable.

        if ( normalizedTokenFile.getParent () == null )
        {
            throw new IllegalArgumentException ( "The token file must have a parent directory." );
        }

        // Return the normalized token file to the caller.

        return normalizedTokenFile;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readValidated
    //
    // Description:
    //
    //   Performs the read validated operation.
    //
    // Arguments:
    //
    //   tokenFile (Path):
    //     The token file to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String readValidated ( Path tokenFile )
    {
        try
        {
            // Initialize the token by applying trim and read string.

            String token = Files.readString ( tokenFile, StandardCharsets.UTF_8 ).trim ();

            // Reject the operation when token length is less than 32 or token chars any match succeeds.

            if ( token.length () < 32 || token.chars ().anyMatch ( Character::isWhitespace ) )
            {
                throw new IllegalArgumentException (
                    "The configured token file does not contain one valid high-entropy token."
                );
            }

            // Return the token to the caller.

            return token;
        }

        // Handle I/O failures captured as exception.

        catch ( IOException exception )
        {
            throw new IllegalStateException ( "The configured token file could not be read.", exception );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: protect
    //
    // Description:
    //
    //   Performs the protect operation.
    //
    // Arguments:
    //
    //   tokenFile (Path):
    //     The token file to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void protect ( Path tokenFile )
    {
        // Initialize the acl view by applying get file attribute view.

        AclFileAttributeView aclView = Files.getFileAttributeView (
            tokenFile,
            AclFileAttributeView.class
        );

        // Handle the branch where acl view is available.

        if ( aclView != null )
        {
            // Apply restrictive permissions to the control-token file.

            protectWithAcl ( aclView );

            return;
        }

        // Initialize the posix view by applying get file attribute view.

        PosixFileAttributeView posixView = Files.getFileAttributeView (
            tokenFile,
            PosixFileAttributeView.class
        );

        // Handle the branch where posix view is available.

        if ( posixView != null )
        {
            try
            {
                // Initialize the permissions by applying of.

                Set <PosixFilePermission> permissions = EnumSet.of (
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
                );

                // Set the posix file permissions on the files.

                Files.setPosixFilePermissions ( tokenFile, permissions );

                return;
            }

            // Handle I/O failures captured as exception.

            catch ( IOException exception )
            {
                throw new IllegalStateException (
                    "The control-token file permissions could not be restricted.",
                    exception
                );
            }
        }

        throw new IllegalStateException (
            "The filesystem does not expose ACL or POSIX permissions for the control-token file."
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: protectWithAcl
    //
    // Description:
    //
    //   Performs the protect with acl operation.
    //
    // Arguments:
    //
    //   aclView (AclFileAttributeView):
    //     The acl view to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void protectWithAcl ( AclFileAttributeView aclView )
    {
        try
        {
            // Initialize the owner entry by applying build, set permissions, set principal, set type, new builder, get
            // owner, and all of.

            AclEntry ownerEntry = AclEntry.newBuilder ()
                .setType ( AclEntryType.ALLOW )
                .setPrincipal ( aclView.getOwner () )
                .setPermissions ( EnumSet.allOf ( AclEntryPermission.class ) )
                .build ();

            // Perform the set acl and of calls required by the protect with acl operation.

            aclView.setAcl ( List.of ( ownerEntry ) );
        }

        // Handle I/O failures captured as exception.

        catch ( IOException exception )
        {
            throw new IllegalStateException (
                "The control-token file ACL could not be restricted.",
                exception
            );
        }
    }
}
