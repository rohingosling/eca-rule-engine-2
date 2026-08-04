//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies the plain-text server banner, structured fields, and wrapped framework messages.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;

import com.rohingosling.eca.server.ServerMain;
import com.rohingosling.eca.server.ServerLogFormatter;
import com.rohingosling.eca.server.ServerTerminalOutput;

import picocli.CommandLine;

//*********************************************************************************************************************
// Class: ServerTerminalFormattingTest
//
// Description:
//
//   Verifies the plain-text server banner, structured fields, and wrapped framework messages.
//
//*********************************************************************************************************************

final class ServerTerminalFormattingTest
{
    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: rootHelp_separatesUsageDescriptionAndCommands
    //
    // Description:
    //
    //   Verifies that root help separates usage description and commands.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void rootHelp_separatesUsageDescriptionAndCommands ()
    {
        // Prepare the line separator and expected help values needed by the root help separates usage description and
        // commands operation.

        String lineSeparator = System.lineSeparator ();
        String expectedHelp = String.join (
            lineSeparator,
            "",
            "Usage: eca-server [-hV] [COMMAND]",
            "",
            "ECA Rule Engine 2 server.",
            "  -h, --help      Show this help message and exit.",
            "  -V, --version   Print version information and exit.",
            "",
            "Commands:",
            "  start  Start the HTTP server in the foreground.",
            "  stop   Request authenticated graceful shutdown.",
            "  model  Validate and upload an authoring-model JSON file.",
            "",
            ""
        );

        // Verify the root help separates usage description and commands scenario against its expected outcome.

        assertThat ( executeServerCommand () ).isEqualTo ( expectedHelp );
        assertThat ( executeServerCommand ( "--help" ) ).isEqualTo ( expectedHelp );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: banner_matchesPlainTextProductIdentity
    //
    // Description:
    //
    //   Verifies that banner matches plain text product identity.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void banner_matchesPlainTextProductIdentity ()
    {
        // Prepare the banner and line separator values needed by the banner matches plain text product identity
        // operation.

        String banner = ServerTerminalOutput.banner ();
        String lineSeparator = System.lineSeparator ();
        String expectedBanner = lineSeparator
            + "ECA (Event Condition Action) Rule Engine Server" + lineSeparator
            + "Version 2.0.0" + lineSeparator
            + "Author: Rohin Gosling (2024)" + lineSeparator
            + lineSeparator;

        // Verify the banner matches plain text product identity scenario against its expected outcome.

        assertThat ( banner ).isEqualTo ( expectedBanner );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: structuredMessage_alignsNamesAndValues
    //
    // Description:
    //
    //   Verifies that structured message aligns names and values.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void structuredMessage_alignsNamesAndValues ()
    {
        // Prepare the formatter, log record, and output values needed by the structured message aligns names and
        // values operation.

        ServerLogFormatter formatter = new ServerLogFormatter ();
        LogRecord logRecord = logRecord (
            "com.rohingosling.eca.server.StructuredHostedModelLogger",
            "log",
            "event=model-replacement status=accepted model-id=courier-selection "
                + "condition-sets=1 rules=1"
        );
        String output = formatter.format ( logRecord );

        // Verify the structured message aligns names and values scenario against its expected outcome.

        assertThat ( output )
            .contains (
                "INFO:",
                "  event          = model-replacement",
                "  status         = accepted",
                "  model-id       = courier-selection",
                "  condition-sets = 1",
                "  rules          = 1"
            );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: serverStartup_wrapsSentencesAndAppendsListeningAddress
    //
    // Description:
    //
    //   Verifies that server startup wraps sentences and appends listening address.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void serverStartup_wrapsSentencesAndAppendsListeningAddress ()
    {
        // Prepare the formatter, startup record, listening record, and output values needed by the server startup
        // wraps sentences and appends listening address operation.

        ServerLogFormatter formatter = new ServerLogFormatter ();
        LogRecord startupRecord = logRecord (
            "io.helidon.webserver.LoomServer",
            "startIt",
            "Started all channels in 4 milliseconds. "
                + "29 milliseconds since JVM startup. "
                + "Java 25.0.4+7-LTS-jvmci-b01"
        );
        LogRecord listeningRecord = logRecord (
            "com.rohingosling.eca.server.ServerTerminalOutput",
            "logListening",
            "eca-server listening on http://127.0.0.1:8080"
        );
        String output = formatter.format ( startupRecord ) + formatter.format ( listeningRecord );

        // Verify the server startup wraps sentences and appends listening address scenario against its expected
        // outcome.

        assertThat ( output )
            .contains (
                "io.helidon.webserver.LoomServer startIt",
                "  Started all channels in 4 milliseconds.",
                "  29 milliseconds since JVM startup.",
                "  Java 25.0.4+7-LTS-jvmci-b01",
                "  eca-server listening on http://127.0.0.1:8080"
            )
            .doesNotContain (
                "com.rohingosling.eca.server.ServerTerminalOutput logListening"
            );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: executeServerCommand
    //
    // Description:
    //
    //   Performs the execute server command operation.
    //
    // Arguments:
    //
    //   arguments (String...):
    //     The command-line arguments.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String executeServerCommand ( String... arguments )
    {
        // Prepare the output and command line values needed by the execute server command operation.

        StringWriter output = new StringWriter ();
        CommandLine commandLine = new CommandLine ( new ServerMain () );

        // Verify the execute server command scenario against its expected outcome.

        commandLine.setOut ( new PrintWriter ( output ) );

        assertThat ( commandLine.execute ( arguments ) ).isZero ();

        // Return the completed textual representation.

        return output.toString ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: logRecord
    //
    // Description:
    //
    //   Performs the log record operation.
    //
    // Arguments:
    //
    //   sourceClassName (String):
    //     The source class name to use.
    //
    //   sourceMethodName (String):
    //     The source method name to use.
    //
    //   message (String):
    //     The message to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static LogRecord logRecord (
        String sourceClassName,
        String sourceMethodName,
        String message
    )
    {
        // Initialize the log record with a new log record.

        LogRecord logRecord = new LogRecord ( Level.INFO, message );

        // Perform the set instant, parse, set logger name, set source class name, and set source method name calls
        // required by the log record operation.

        logRecord.setInstant ( Instant.parse ( "2026-07-27T09:22:31Z" ) );
        logRecord.setLoggerName ( sourceClassName );
        logRecord.setSourceClassName ( sourceClassName );
        logRecord.setSourceMethodName ( sourceMethodName );

        // Return the log record to the caller.

        return logRecord;
    }
}
