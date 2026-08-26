//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Configures the server console presentation and renders the plain-text product identity banner.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.server;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

//*********************************************************************************************************************
// Class: ServerTerminalOutput
//
// Description:
//
//   Configures the server console presentation and renders the plain-text product identity banner.
//
//*********************************************************************************************************************

public final class ServerTerminalOutput
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    public static final String PRODUCT_NAME    = "ECA (Event Condition Action) Rule Engine Server";
    public static final String PRODUCT_VERSION = "2.1.0";
    public static final String PRODUCT_AUTHOR  = "Rohin Gosling (2024)";

    public static final String FRAMEWORK_LOGGING_VARIABLE = "ECA_SERVER_FRAMEWORK_LOGGING";

    private static final String PRODUCT_NAMESPACE     = "com.rohingosling.eca";
    private static final String FRAMEWORK_LOGGING_ALL = "all";

    private static final Logger TERMINAL_LOGGER = Logger.getLogger (
        ServerTerminalOutput.class.getName ()
    );

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: ServerTerminalOutput
    //
    // Description:
    //
    //   Creates the ServerTerminalOutput instance from the supplied values.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private ServerTerminalOutput ()
    {
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: configureLogging
    //
    // Description:
    //
    //   Performs the configure logging operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static void configureLogging ()
    {
        // Initialize the root logger by applying get logger.

        Logger rootLogger = Logger.getLogger ( "" );

        // Process each handler supplied by root logger handlers.

        for ( Handler handler : rootLogger.getHandlers () )
        {
            // Perform the remove handler and close calls required by the configure logging operation.

            rootLogger.removeHandler ( handler );
            handler.close ();
        }

        // Apply set level and add handler to the root logger for the configure logging operation.

        rootLogger.setLevel ( Level.INFO );
        rootLogger.addHandler ( new TerminalHandler ( System.out ) );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: isReportableRecord
    //
    // Description:
    //
    //   Reports whether the supplied log record belongs in the server terminal.
    //
    //   The terminal is a product surface. A warning or an error raised inside a web framework, a serialization
    //   library, or a Java runtime deprecation notice describes that component rather than the rule engine, so it is
    //   withheld. Records below the warning threshold are unaffected, because the restriction covers warnings and
    //   errors only. Set the environment variable named by FRAMEWORK_LOGGING_VARIABLE to "all" to restore every record
    //   while diagnosing a framework problem.
    //
    // Arguments:
    //
    //   logRecord (LogRecord):
    //     The log record to use.
    //
    // Returns:
    //
    //   True when the record belongs in the server terminal.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static boolean isReportableRecord ( LogRecord logRecord )
    {
        // Stop this path and return its result when the record is unavailable or the caller asked for every record.

        if ( logRecord == null || frameworkLoggingRequested () )
        {
            // Return true because no restriction applies.

            return true;
        }

        // Stop this path and return its result when the record sits below the warning threshold.

        if ( logRecord.getLevel ().intValue () < Level.WARNING.intValue () )
        {
            // Return true because the restriction covers warnings and errors only.

            return true;
        }

        // Return the result produced by originates from product.

        return originatesFromProduct ( logRecord );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: frameworkLoggingRequested
    //
    // Description:
    //
    //   Reports whether the caller asked for unrestricted framework logging.
    //
    // Returns:
    //
    //   True when the caller asked for unrestricted framework logging.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static boolean frameworkLoggingRequested ()
    {
        // Return the result produced by equals ignore case.

        return FRAMEWORK_LOGGING_ALL.equalsIgnoreCase ( System.getenv ( FRAMEWORK_LOGGING_VARIABLE ) );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: originatesFromProduct
    //
    // Description:
    //
    //   Reports whether the supplied log record was raised by the rule engine itself.
    //
    // Arguments:
    //
    //   logRecord (LogRecord):
    //     The log record to use.
    //
    // Returns:
    //
    //   True when the record was raised by the rule engine itself.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static boolean originatesFromProduct ( LogRecord logRecord )
    {
        // Return the result produced by is product name.

        return isProductName ( logRecord.getLoggerName () )
            || isProductName ( logRecord.getSourceClassName () );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: isProductName
    //
    // Description:
    //
    //   Reports whether the supplied logger or class name sits inside the product namespace.
    //
    // Arguments:
    //
    //   name (String):
    //     The logger or class name to use.
    //
    // Returns:
    //
    //   True when the name sits inside the product namespace.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static boolean isProductName ( String name )
    {
        // Return the result produced by starts with.

        return name != null && name.startsWith ( PRODUCT_NAMESPACE );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: writeBanner
    //
    // Description:
    //
    //   Performs the write banner operation.
    //
    // Arguments:
    //
    //   output (PrintWriter):
    //     The output to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static void writeBanner ( PrintWriter output )
    {
        // Perform the require non null, print, banner, and flush calls required by the write banner operation.

        Objects.requireNonNull ( output, "output" );

        output.print ( banner () );
        output.flush ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: banner
    //
    // Description:
    //
    //   Performs the banner operation.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static String banner ()
    {
        // Initialize the line separator by applying line separator.

        String lineSeparator = System.lineSeparator ();

        // Return the composed banner value.

        return lineSeparator
            + PRODUCT_NAME + lineSeparator
            + "Version " + PRODUCT_VERSION + lineSeparator
            + "Author: " + PRODUCT_AUTHOR + lineSeparator
            + lineSeparator;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: logListening
    //
    // Description:
    //
    //   Performs the log listening operation.
    //
    // Arguments:
    //
    //   host (String):
    //     The host to use.
    //
    //   port (int):
    //     The port to use.
    //
    //-----------------------------------------------------------------------------------------------------------------

    public static void logListening ( String host, int port )
    {
        // Record the operation through the terminal logger.

        TERMINAL_LOGGER.log (
            Level.INFO,
            "eca-server listening on http://" + host + ":" + port
        );
    }

    //*****************************************************************************************************************
    // Class: TerminalHandler
    //
    // Description:
    //
    //   Provides the terminal handler behavior.
    //
    //*****************************************************************************************************************

    private static final class TerminalHandler extends Handler
    {
        //=============================================================================================================
        // Fields
        //=============================================================================================================

        private final PrintStream output;

        //=============================================================================================================
        // Constructors
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Constructor 1/1: TerminalHandler
        //
        // Description:
        //
        //   Creates the TerminalHandler instance from the supplied values.
        //
        // Arguments:
        //
        //   output (PrintStream):
        //     The output to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        private TerminalHandler ( PrintStream output )
        {
            // Validate the required output before continuing.

            this.output = Objects.requireNonNull ( output, "output" );

            // Configure the this with the required level, filter, and formatter values.

            this.setLevel ( Level.INFO );
            this.setFilter ( ServerTerminalOutput::isReportableRecord );
            this.setFormatter ( new ServerLogFormatter () );
        }

        //=============================================================================================================
        // Methods
        //=============================================================================================================

        //-------------------------------------------------------------------------------------------------------------
        // Method: publish
        //
        // Description:
        //
        //   Performs the publish operation.
        //
        // Arguments:
        //
        //   logRecord (LogRecord):
        //     The log record to use.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public synchronized void publish ( LogRecord logRecord )
        {
            // Stop this path and return its result when this is not loggable.

            if ( !this.isLoggable ( logRecord ) )
            {
                return;
            }

            // Perform the print, format, get formatter, and flush calls required by the publish operation.

            this.output.print ( this.getFormatter ().format ( logRecord ) );
            this.output.flush ();
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: flush
        //
        // Description:
        //
        //   Performs the flush operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public void flush ()
        {
            // Flush buffered output to its destination.

            this.output.flush ();
        }

        //-------------------------------------------------------------------------------------------------------------
        // Method: close
        //
        // Description:
        //
        //   Performs the close operation.
        //
        //-------------------------------------------------------------------------------------------------------------

        @Override
        public void close ()
        {
            // Flush buffered output to its destination.

            this.flush ();
        }
    }
}
