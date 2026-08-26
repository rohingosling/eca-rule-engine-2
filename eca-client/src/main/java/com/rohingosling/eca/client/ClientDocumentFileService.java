//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Performs desktop document reads and atomic local-file replacements away from the JavaFX application thread.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.rohingosling.eca.application.ClientDocumentCodec;
import com.rohingosling.eca.application.ClientDocumentSession;

//*********************************************************************************************************************
// Class: ClientDocumentFileService
//
// Description:
//
//   Performs desktop document reads and atomic local-file replacements away from the JavaFX application thread.
//
//*********************************************************************************************************************

public final class ClientDocumentFileService implements ClientDocumentOperations
{
    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final ClientDocumentCodec clientDocumentCodec;
    private final Executor            executor;
    private final ExecutorService     ownedExecutorService;

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/3: ClientDocumentFileService
    //
    // Description:
    //
    //   Creates the ClientDocumentFileService instance from the supplied values.
    //
    // Arguments:
    //
    //   clientDocumentCodec (ClientDocumentCodec):
    //     The client document codec to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ClientDocumentFileService ( ClientDocumentCodec clientDocumentCodec )
    {
        // Apply this and new single thread executor to the executors for the client document file service operation.

        this (
            clientDocumentCodec,
            Executors.newSingleThreadExecutor ( new ClientFileThreadFactory () ),
            true
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 2/3: ClientDocumentFileService
    //
    // Description:
    //
    //   Creates the ClientDocumentFileService instance from the supplied values.
    //
    // Arguments:
    //
    //   clientDocumentCodec (ClientDocumentCodec):
    //     The client document codec to use.
    //
    //   executor (Executor):
    //     The executor to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public ClientDocumentFileService ( ClientDocumentCodec clientDocumentCodec, Executor executor )
    {
        // Delegate initialization to the primary client document file service constructor.

        this ( clientDocumentCodec, executor, false );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 3/3: ClientDocumentFileService
    //
    // Description:
    //
    //   Creates the ClientDocumentFileService instance from the supplied values.
    //
    // Arguments:
    //
    //   clientDocumentCodec (ClientDocumentCodec):
    //     The client document codec to use.
    //
    //   executor (Executor):
    //     The executor to use.
    //
    //   ownsExecutor (boolean):
    //     The owns executor to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private ClientDocumentFileService (
        ClientDocumentCodec clientDocumentCodec,
        Executor executor,
        boolean ownsExecutor
    )
    {
        // Validate the required client document codec and executor before continuing.

        this.clientDocumentCodec = Objects.requireNonNull ( clientDocumentCodec, "clientDocumentCodec" );
        this.executor            = Objects.requireNonNull ( executor, "executor" );
        this.ownedExecutorService = ownsExecutor ? (ExecutorService) executor : null;
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: open
    //
    // Description:
    //
    //   Performs the open operation.
    //
    // Arguments:
    //
    //   path (Path):
    //     The path to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public CompletableFuture <ClientDocumentSession.Content> open ( Path path )
    {
        // Initialize the source path by applying normalize, to absolute path, and require non null.

        Path sourcePath = Objects.requireNonNull ( path, "path" ).toAbsolutePath ().normalize ();

        // Return the asynchronous open operation.

        return CompletableFuture.supplyAsync (
            () ->
            {
                try
                {
                    // Return the result produced by read.

                    return this.clientDocumentCodec.read ( Files.readAllBytes ( sourcePath ) );
                }

                // Handle I/O or runtime failures captured as exception.

                catch ( IOException | RuntimeException exception )
                {
                    throw new CompletionException (
                        "The model could not be opened from " + sourcePath + ".",
                        exception
                    );
                }
            },
            this.executor
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: save
    //
    // Description:
    //
    //   Performs the save operation.
    //
    // Arguments:
    //
    //   path (Path):
    //     The path to use.
    //
    //   content (ClientDocumentSession.Content):
    //     The content to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public CompletableFuture <Void> save ( Path path, ClientDocumentSession.Content content )
    {
        // Initialize the target path by applying normalize, to absolute path, and require non null.

        Path targetPath = Objects.requireNonNull ( path, "path" ).toAbsolutePath ().normalize ();

        // Validate the required content before continuing.

        Objects.requireNonNull ( content, "content" );

        // Return the asynchronous save operation.

        return CompletableFuture.runAsync (
            () -> writeAtomically ( targetPath, this.clientDocumentCodec.write ( content ) ),
            this.executor
        );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: close
    //
    // Description:
    //
    //   Performs the close operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public void close ()
    {
        // Handle the branch where owned executor service is available.

        if ( this.ownedExecutorService != null )
        {
            // Stop the owned executor service as part of the shutdown sequence.

            this.ownedExecutorService.shutdownNow ();
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: writeAtomically
    //
    // Description:
    //
    //   Performs the write atomically operation.
    //
    // Arguments:
    //
    //   targetPath (Path):
    //     The target path to use.
    //
    //   document (byte []):
    //     The document to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void writeAtomically ( Path targetPath, byte [] document )
    {
        // Initialize the parent directory by applying get parent.

        Path parentDirectory = targetPath.getParent ();
        Path temporaryPath   = null;

        // Reject the operation when parent directory is unavailable.

        if ( parentDirectory == null )
        {
            throw new CompletionException (
                new IOException ( "The model path has no parent directory: " + targetPath + "." )
            );
        }

        try
        {
            // Update the temporary path from the create temp file result.

            temporaryPath = Files.createTempFile ( parentDirectory, ".eca-client-", ".tmp" );

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

                ByteBuffer documentBuffer = ByteBuffer.wrap ( document );

                // Write the remaining buffered data until the buffer has been drained.

                while ( documentBuffer.hasRemaining () )
                {
                    // Write the prepared data through the file channel.

                    fileChannel.write ( documentBuffer );
                }

                // Flush the file contents and metadata to durable storage.

                fileChannel.force ( true );
            }

            try
            {
                // Move the prepared file into its destination.

                Files.move (
                    temporaryPath,
                    targetPath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                );
            }

            // Handle atomic move not supported failures captured as exception.

            catch ( AtomicMoveNotSupportedException exception )
            {
                // Move the prepared file into its destination.

                Files.move ( temporaryPath, targetPath, StandardCopyOption.REPLACE_EXISTING );
            }

            temporaryPath = null;
        }

        // Handle I/O or runtime failures captured as exception.

        catch ( IOException | RuntimeException exception )
        {
            throw new CompletionException (
                "The model could not be saved to " + targetPath + ".",
                exception
            );
        }

        // Complete the required cleanup regardless of how the protected operation finishes.

        finally
        {
            // Handle the branch where temporary path is available.

            if ( temporaryPath != null )
            {
                try
                {
                    // Delete the temporary file when it still exists.

                    Files.deleteIfExists ( temporaryPath );
                }

                // Handle I/O failures captured as ignored exception.

                catch ( IOException ignoredException )
                {
                    // A failed best-effort cleanup must not hide the original persistence error.

                }
            }
        }
    }

    //*****************************************************************************************************************
    // Class: ClientFileThreadFactory
    //
    // Description:
    //
    //   Provides the client file thread factory behavior.
    //
    //*****************************************************************************************************************

    private static final class ClientFileThreadFactory implements ThreadFactory
    {
        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: newThread
        //
        // Description:
        //
        //   Performs the new thread operation.
        //
        // Arguments:
        //
        //   runnable (Runnable):
        //     The runnable to use.
        //
        // Returns:
        //
        //   The result of the operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public Thread newThread ( Runnable runnable )
        {
            // Initialize the thread with a new thread.

            Thread thread = new Thread ( runnable, "eca-client-file" );

            // Set the daemon on the thread.

            thread.setDaemon ( true );

            // Return the thread to the caller.

            return thread;
        }
    }
}
