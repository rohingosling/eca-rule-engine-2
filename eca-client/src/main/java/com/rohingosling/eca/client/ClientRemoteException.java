//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Classifies remote client failures into stable categories that can be presented without leaking credentials.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.client;

import java.util.Objects;

//*********************************************************************************************************************
// Class: ClientRemoteException
//
// Description:
//
//   Classifies remote client failures into stable categories that can be presented without leaking credentials.
//
//*********************************************************************************************************************

public final class ClientRemoteException extends RuntimeException
{
    //=================================================================================================================
    // User Defined Data Types
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Enum: Kind
    //
    // Description:
    //
    //   Enumerates the supported kind values.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public enum Kind
    {
        CONNECTION,
        TIMEOUT,
        AUTHENTICATION,
        NO_MODEL,
        VALIDATION,
        PROTOCOL,
        REMOTE
    }

    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final Kind kind;

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getKind
    //
    // Description:
    //
    //   Returns the kind.
    //
    // Returns:
    //
    //   The kind.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Kind getKind ()
    {
        // Return the kind to the caller.

        return this.kind;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/2: ClientRemoteException
    //
    // Description:
    //
    //   Creates the ClientRemoteException instance from the supplied values.
    //
    // Arguments:
    //
    //   kind (Kind):
    //     The kind to use.
    //
    //   message (String):
    //     The message to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ClientRemoteException ( Kind kind, String message )
    {
        // Initialize the inherited state through the base-class constructor.

        super ( message );

        // Validate the required kind before continuing.

        this.kind = Objects.requireNonNull ( kind, "kind" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 2/2: ClientRemoteException
    //
    // Description:
    //
    //   Creates the ClientRemoteException instance from the supplied values.
    //
    // Arguments:
    //
    //   kind (Kind):
    //     The kind to use.
    //
    //   message (String):
    //     The message to use.
    //
    //   cause (Throwable):
    //     The exception that caused the operation to fail.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ClientRemoteException ( Kind kind, String message, Throwable cause )
    {
        // Initialize the inherited state through the base-class constructor.

        super ( message, cause );

        // Validate the required kind before continuing.

        this.kind = Objects.requireNonNull ( kind, "kind" );
    }
}
