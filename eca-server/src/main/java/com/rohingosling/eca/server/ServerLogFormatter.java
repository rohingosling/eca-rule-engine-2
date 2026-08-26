//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Formats server and framework log records as readable terminal sections with aligned structured fields.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.server;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

//*********************************************************************************************************************
// Class: ServerLogFormatter
//
// Description:
//
//   Formats server and framework log records as readable terminal sections with aligned structured fields.
//
//*********************************************************************************************************************

public final class ServerLogFormatter extends Formatter
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    private static final String            LINE_SEPARATOR   = System.lineSeparator ();
    private static final String            LISTENING_PREFIX = "eca-server listening on ";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter
        .ofPattern ( "MMM d, uuuu h:mm:ss a", Locale.ENGLISH )
        .withZone ( ZoneId.systemDefault () );

    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private boolean awaitingListeningContinuation;

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: format
    //
    // Description:
    //
    //   Performs the format operation.
    //
    // Arguments:
    //
    //   logRecord (LogRecord):
    //     The log record to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public synchronized String format ( LogRecord logRecord )
    {
        // Initialize the message by applying format message.

        String message = this.formatMessage ( logRecord );

        // Handle the branch where awaiting listening continuation is true and message starts with succeeds.

        if ( this.awaitingListeningContinuation && message.startsWith ( LISTENING_PREFIX ) )
        {
            this.awaitingListeningContinuation = false;

            // Return the composed format value when awaiting listening continuation is true and message starts with
            // succeeds.

            return "  " + message + LINE_SEPARATOR + LINE_SEPARATOR;
        }

        // Initialize the output with a new string builder.

        StringBuilder output = new StringBuilder ();

        // Handle the branch where awaiting listening continuation is true.

        if ( this.awaitingListeningContinuation )
        {
            // Append the prepared text to the message buffer.

            output.append ( LINE_SEPARATOR );
            this.awaitingListeningContinuation = false;
        }

        // Perform the append header, append, get localized name, and get level calls required by the format operation.

        appendHeader ( output, logRecord );
        output.append ( LINE_SEPARATOR );
        output.append ( LINE_SEPARATOR );
        output.append ( logRecord.getLevel ().getLocalizedName () );
        output.append ( ':' );
        output.append ( LINE_SEPARATOR );

        // Initialize the structured fields by applying parse structured fields.

        List <StructuredField> structuredFields = parseStructuredFields ( message );

        // Handle the branch where structured fields contains no values.

        if ( structuredFields.isEmpty () )
        {
            // Append the human-readable log message to the output buffer.

            appendProse ( output, message );
        }

        // Handle the alternative path when the preceding condition is not satisfied.

        else
        {
            // Append the structured log fields to the output buffer.

            appendStructuredFields ( output, structuredFields );
        }

        // Append the captured failure details to the log output.

        appendThrowable ( output, logRecord );

        // Update the awaiting listening continuation from the is server started record result.

        this.awaitingListeningContinuation = isServerStartedRecord ( logRecord, message );

        // Handle the branch where awaiting listening continuation is false.

        if ( !this.awaitingListeningContinuation )
        {
            // Append the prepared text to the message buffer.

            output.append ( LINE_SEPARATOR );
        }

        // Return the completed textual representation.

        return output.toString ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: appendHeader
    //
    // Description:
    //
    //   Performs the append header operation.
    //
    // Arguments:
    //
    //   output (StringBuilder):
    //     The output to use.
    //
    //   logRecord (LogRecord):
    //     The log record to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void appendHeader ( StringBuilder output, LogRecord logRecord )
    {
        // Prepare the source class name and source method name values needed by the append header operation.

        String sourceClassName = logRecord.getSourceClassName ();
        String sourceMethodName = logRecord.getSourceMethodName ();

        // Handle the branch where source class name is unavailable or source class name is blank.

        if ( sourceClassName == null || sourceClassName.isBlank () )
        {
            // Update the source class name from the get logger name result.

            sourceClassName = logRecord.getLoggerName ();
        }

        // Perform the append, format, and get instant calls required by the append header operation.

        output.append ( TIMESTAMP_FORMAT.format ( logRecord.getInstant () ) );

        // Handle the branch where source class name is available and source class name contains text.

        if ( sourceClassName != null && !sourceClassName.isBlank () )
        {
            // Append the prepared text to the message buffer.

            output.append ( ' ' );
            output.append ( sourceClassName );
        }

        // Handle the branch where source method name is available and source method name contains text.

        if ( sourceMethodName != null && !sourceMethodName.isBlank () )
        {
            // Append the prepared text to the message buffer.

            output.append ( ' ' );
            output.append ( sourceMethodName );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: appendProse
    //
    // Description:
    //
    //   Performs the append prose operation.
    //
    // Arguments:
    //
    //   output (StringBuilder):
    //     The output to use.
    //
    //   message (String):
    //     The message to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void appendProse ( StringBuilder output, String message )
    {
        // Prepare the normalized message and logical lines values needed by the append prose operation.

        String normalizedMessage = message
            .replace ( "\r\n", "\n" )
            .replace ( '\r', '\n' );
        String[] logicalLines = normalizedMessage.split ( "\n", -1 );

        // Process each logical line supplied by logical lines.

        for ( String logicalLine : logicalLines )
        {
            // Initialize the sentences by applying split.

            String[] sentences = logicalLine.split ( "(?<=\\.)\\s+" );

            // Process each sentence supplied by sentences.

            for ( String sentence : sentences )
            {
                // Append the prepared text to the message buffer.

                output.append ( "  " );
                output.append ( sentence );
                output.append ( LINE_SEPARATOR );
            }
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: appendStructuredFields
    //
    // Description:
    //
    //   Performs the append structured fields operation.
    //
    // Arguments:
    //
    //   output (StringBuilder):
    //     The output to use.
    //
    //   structuredFields (List <StructuredField>):
    //     The structured fields to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void appendStructuredFields (
        StringBuilder output,
        List <StructuredField> structuredFields
    )
    {
        // Initialize the maximum name length by applying or else, max, map to int, stream, length, and get name.

        int maximumNameLength = structuredFields.stream ()
            .mapToInt ( field -> field.getName ().length () )
            .max ()
            .orElse ( 0 );

        // Process each field supplied by structured fields.

        for ( StructuredField field : structuredFields )
        {
            // Perform the append, get name, repeat, length, and get value calls required by the append structured
            // fields operation.

            output.append ( "  " );
            output.append ( field.getName () );
            output.append ( " ".repeat ( maximumNameLength - field.getName ().length () ) );
            output.append ( " = " );
            output.append ( field.getValue () );
            output.append ( LINE_SEPARATOR );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: appendThrowable
    //
    // Description:
    //
    //   Performs the append throwable operation.
    //
    // Arguments:
    //
    //   output (StringBuilder):
    //     The output to use.
    //
    //   logRecord (LogRecord):
    //     The log record to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void appendThrowable ( StringBuilder output, LogRecord logRecord )
    {
        // Stop this path and return its result when log record thrown is unavailable.

        if ( logRecord.getThrown () == null )
        {
            return;
        }

        // Initialize the stack trace with a new string writer.

        StringWriter stackTrace = new StringWriter ();

        // Open the scoped resources for the protected operation and close them automatically afterward.

        try ( PrintWriter stackTraceWriter = new PrintWriter ( stackTrace ) )
        {
            // Perform the print stack trace and get thrown calls required by the append throwable operation.

            logRecord.getThrown ().printStackTrace ( stackTraceWriter );
        }

        // Perform the append prose, strip trailing, and to string calls required by the append throwable operation.

        appendProse ( output, stackTrace.toString ().stripTrailing () );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: parseStructuredFields
    //
    // Description:
    //
    //   Performs the parse structured fields operation.
    //
    // Arguments:
    //
    //   message (String):
    //     The message to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static List <StructuredField> parseStructuredFields ( String message )
    {
        // Prepare the structured fields and tokens values needed by the parse structured fields operation.

        List <StructuredField> structuredFields = new ArrayList <> ();
        String[] tokens = message.split ( "\\s+" );

        // Process each token supplied by tokens.

        for ( String token : tokens )
        {
            // Initialize the separator index by applying index of.

            int separatorIndex = token.indexOf ( '=' );

            // Stop this path and return its result when separator index is at most 0 or separator index equals token
            // length - 1.

            if ( separatorIndex <= 0 || separatorIndex == token.length () - 1 )
            {
                // Return the result produced by of when separator index is at most 0 or separator index equals token
                // length - 1.

                return List.of ();
            }

            // Prepare the name and value values needed by the parse structured fields operation.

            String name  = token.substring ( 0, separatorIndex );
            String value = token.substring ( separatorIndex + 1 );

            // Stop this path and return its result when name does not match the supplied values.

            if ( !name.matches ( "[a-z][a-z0-9-]*" ) )
            {
                // Return the result produced by of when name does not match the supplied values.

                return List.of ();
            }

            // Add new structured field name value to the structured fields.

            structuredFields.add ( new StructuredField ( name, value ) );
        }

        // Return the structured fields to the caller.

        return structuredFields;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: isServerStartedRecord
    //
    // Description:
    //
    //   Indicates whether server started record.
    //
    // Arguments:
    //
    //   logRecord (LogRecord):
    //     The log record to use.
    //
    //   message (String):
    //     The message to use.
    //
    // Returns:
    //
    //   `true` when the condition is satisfied; otherwise `false`.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static boolean isServerStartedRecord ( LogRecord logRecord, String message )
    {
        // Return whether "io helidon webserver loom server" matches log record logger name and message starts with
        // succeeds.

        return "io.helidon.webserver.LoomServer".equals ( logRecord.getLoggerName () )
            && message.startsWith ( "Started all channels " );
    }

    //*****************************************************************************************************************
    // Class: StructuredField
    //
    // Description:
    //
    //   Provides the structured field behavior.
    //
    //*****************************************************************************************************************

    private static final class StructuredField
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final String name;
        private final String value;

        //=============================================================================================================
        // Accessors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: getName
        //
        // Description:
        //
        //   Returns the name.
        //
        // Returns:
        //
        //   The name.
        //
        //-------------------------------------------------------------------------------------------------------------

        private String getName ()
        {
            // Return the name to the caller.

            return this.name;
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: getValue
        //
        // Description:
        //
        //   Returns the value.
        //
        // Returns:
        //
        //   The value.
        //
        //-------------------------------------------------------------------------------------------------------------

        private String getValue ()
        {
            // Return the value to the caller.

            return this.value;
        }

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: StructuredField
        //
        // Description:
        //
        //   Creates the StructuredField instance from the supplied values.
        //
        // Arguments:
        //
        //   name (String):
        //     The name to use.
        //
        //   value (String):
        //     The value to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private StructuredField ( String name, String value )
        {
            this.name  = name;
            this.value = value;
        }
    }
}
