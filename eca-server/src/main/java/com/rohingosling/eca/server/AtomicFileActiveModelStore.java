//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Persists the canonical active model through a flushed same-directory temporary file and atomic replacement.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import com.rohingosling.eca.application.ActiveModelStore;
import com.rohingosling.eca.application.HostedModelPersistenceException;

//*********************************************************************************************************************
// Class: AtomicFileActiveModelStore
//
// Description:
//
//   Persists the canonical active model through a flushed same-directory temporary file and atomic replacement.
//
//*********************************************************************************************************************

public final class AtomicFileActiveModelStore implements ActiveModelStore
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final Path activeModelPath;

    //=================================================================================================================
    // Accessors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: getActiveModelPath
    //
    // Description:
    //
    //   Returns the active model path.
    //
    // Returns:
    //
    //   The active model path.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public Path getActiveModelPath ()
    {
        // Return the active model path to the caller.

        return this.activeModelPath;
    }

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: AtomicFileActiveModelStore
    //
    // Description:
    //
    //   Creates the AtomicFileActiveModelStore instance from the supplied values.
    //
    // Arguments:
    //
    //   activeModelPath (Path):
    //     The active model path to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public AtomicFileActiveModelStore ( Path activeModelPath )
    {
        // Update the active model path from the require non null result.

        this.activeModelPath = Objects.requireNonNull ( activeModelPath, "activeModelPath" )
            .toAbsolutePath ()
            .normalize ();
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: load
    //
    // Description:
    //
    //   Performs the load operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public Optional <byte []> load ()
    {
        // Stop this path and return its result when files exists does not succeed.

        if ( !Files.exists ( this.activeModelPath ) )
        {
            // Return an empty optional result because no value is available when files exists does not succeed.

            return Optional.empty ();
        }

        try
        {
            // Return an optional containing the available value.

            return Optional.of ( Files.readAllBytes ( this.activeModelPath ) );
        }

        // Handle I/O failures captured as exception.

        catch ( IOException exception )
        {
            throw new HostedModelPersistenceException (
                "The active model could not be read from " + this.activeModelPath + ".",
                exception
            );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: persist
    //
    // Description:
    //
    //   Performs the persist operation.
    //
    // Arguments:
    //
    //   canonicalDocument (byte []):
    //     The canonical document to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public void persist ( byte [] canonicalDocument )
    {
        // Prepare the copied document and parent directory values needed by the persist operation.

        byte [] copiedDocument = Arrays.copyOf (
            Objects.requireNonNull ( canonicalDocument, "canonicalDocument" ),
            canonicalDocument.length
        );
        Path parentDirectory = this.activeModelPath.getParent ();
        Path temporaryPath   = null;

        try
        {
            // Create the parent directory hierarchy before writing the file.

            Files.createDirectories ( parentDirectory );

            // Update the temporary path from the create temp file result.

            temporaryPath = Files.createTempFile ( parentDirectory, ".active-model-", ".tmp" );

            // Apply write and flush and move to the files for the persist operation.

            writeAndFlush ( temporaryPath, copiedDocument );

            Files.move (
                temporaryPath,
                this.activeModelPath,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            );
            temporaryPath = null;
        }

        // Handle I/O failures captured as exception.

        catch ( IOException exception )
        {
            throw new HostedModelPersistenceException (
                "The active model could not be atomically persisted to " + this.activeModelPath + ".",
                exception
            );
        }

        // Complete the required cleanup regardless of how the protected operation finishes.

        finally
        {
            // Remove the temporary persistence file after the operation.

            deleteTemporaryFile ( temporaryPath );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: writeAndFlush
    //
    // Description:
    //
    //   Performs the write and flush operation.
    //
    // Arguments:
    //
    //   temporaryPath (Path):
    //     The temporary path to use.
    //
    //   canonicalDocument (byte []):
    //     The canonical document to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void writeAndFlush ( Path temporaryPath, byte [] canonicalDocument ) throws IOException
    {
        // Open the scoped resources for the protected operation and close them automatically afterward.

        try (
            FileChannel fileChannel = FileChannel.open (
                temporaryPath,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
            )
        )
        {
            // Initialize the document buffer by applying wrap.

            ByteBuffer documentBuffer = ByteBuffer.wrap ( canonicalDocument );

            // Write the remaining buffered data until the buffer has been drained.

            while ( documentBuffer.hasRemaining () )
            {
                // Write the prepared data through the file channel.

                fileChannel.write ( documentBuffer );
            }

            // Flush the file contents and metadata to durable storage.

            fileChannel.force ( true );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: deleteTemporaryFile
    //
    // Description:
    //
    //   Performs the delete temporary file operation.
    //
    // Arguments:
    //
    //   temporaryPath (Path):
    //     The temporary path to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void deleteTemporaryFile ( Path temporaryPath )
    {
        // Stop this path and return its result when temporary path is unavailable.

        if ( temporaryPath == null )
        {
            return;
        }

        try
        {
            // Delete the temporary file when it still exists.

            Files.deleteIfExists ( temporaryPath );
        }

        // Handle I/O failures captured as exception.

        catch ( IOException exception )
        {
            // Perform the delete on exit and to file calls required by the delete temporary file operation.

            temporaryPath.toFile ().deleteOnExit ();
        }
    }
}
