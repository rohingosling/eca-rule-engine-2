//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Applies the opt-in exact-origin browser-access policy at the Helidon routing boundary.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.server;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import io.helidon.http.HeaderName;
import io.helidon.http.HeaderNames;
import io.helidon.http.Method;
import io.helidon.http.Status;
import io.helidon.webserver.http.Filter;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;

//*********************************************************************************************************************
// Class: BrowserAccessPolicy
//
// Description:
//
//   Applies the opt-in exact-origin browser-access policy at the Helidon routing boundary.
//
//*********************************************************************************************************************

final class BrowserAccessPolicy implements Filter
{
    //=================================================================================================================
    // Constants
    //=================================================================================================================

    private static final String MODEL_REVISION_HEADER                = "X-Model-Revision";
    private static final String REQUEST_PRIVATE_NETWORK_HEADER_NAME  = "Access-Control-Request-Private-Network";
    private static final String RESPONSE_PRIVATE_NETWORK_HEADER_NAME = "Access-Control-Allow-Private-Network";
    private static final String EXPOSED_RESPONSE_HEADERS             = "ETag, " + MODEL_REVISION_HEADER;

    private static final HeaderName REQUEST_PRIVATE_NETWORK_HEADER = HeaderNames.create (
        REQUEST_PRIVATE_NETWORK_HEADER_NAME
    );

    //=================================================================================================================
    // Fields
    //=================================================================================================================

    private final ServerConfiguration configuration;

    //=================================================================================================================
    // Constructors
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Constructor 1/1: BrowserAccessPolicy
    //
    // Description:
    //
    //   Creates the browser-access policy from the validated server configuration.
    //
    // Arguments:
    //
    //   configuration (ServerConfiguration):
    //     The server configuration to enforce.
    //
    //-----------------------------------------------------------------------------------------------------------------

    BrowserAccessPolicy ( ServerConfiguration configuration )
    {
        // Retain the validated configuration for request filtering.

        this.configuration = configuration;
    }

    //=================================================================================================================
    // Filter Implementations
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: filter
    //
    // Description:
    //
    //   Handles valid preflights before route authentication and annotates actual responses for allowed origins.
    //
    // Arguments:
    //
    //   chain (FilterChain):
    //     The remaining routing filter chain.
    //
    //   request (RoutingRequest):
    //     The incoming request.
    //
    //   response (RoutingResponse):
    //     The outgoing response.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public void filter ( FilterChain chain, RoutingRequest request, RoutingResponse response )
    {
        // Handle browser preflight before any protected route can authenticate the request.

        if ( isPreflight ( request ) )
        {
            this.handlePreflight ( request, response );
            return;
        }

        // Add actual-response headers only when the request carries one exact allowed origin.

        this.allowedOrigin ( request ).ifPresent ( origin -> addActualResponseHeaders ( response, origin ) );

        // Continue through the ordinary API routing and authentication boundary.

        chain.proceed ();
    }

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: handlePreflight
    //
    // Description:
    //
    //   Validates and answers one browser preflight without authenticating a protected route.
    //
    // Arguments:
    //
    //   request (RoutingRequest):
    //     The incoming preflight request.
    //
    //   response (RoutingResponse):
    //     The outgoing preflight response.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private void handlePreflight ( RoutingRequest request, RoutingResponse response )
    {
        // Resolve each independently validated preflight component.

        Optional <String> allowedOrigin              = this.allowedOrigin ( request );
        Optional <String> requestedMethod            = requestedMethod ( request );
        Optional <List <String>> requestedHeaders    = requestedHeaders ( request );
        Optional <Boolean> privateNetworkRequested   = privateNetworkRequested ( request );
        String path                                  = request.path ().absolute ().path ();

        // Reject any preflight that is not an exact allowlist, route, method, header, and compatibility match.

        if (
            allowedOrigin.isEmpty ()
                || requestedMethod.isEmpty ()
                || requestedHeaders.isEmpty ()
                || privateNetworkRequested.isEmpty ()
                || !routeAllowsMethod ( path, requestedMethod.get () )
        )
        {
            rejectPreflight ( response );
            return;
        }

        // Emit the minimum successful preflight response for the validated request.

        addOriginResponseHeaders ( response, allowedOrigin.get () );
        response.header ( HeaderNames.ACCESS_CONTROL_ALLOW_METHODS, requestedMethod.get () );

        if ( !requestedHeaders.get ().isEmpty () )
        {
            response.header (
                HeaderNames.ACCESS_CONTROL_ALLOW_HEADERS,
                String.join ( ", ", requestedHeaders.get () )
            );
        }

        if ( privateNetworkRequested.get () )
        {
            response.header ( RESPONSE_PRIVATE_NETWORK_HEADER_NAME, "true" );
        }

        response.status ( Status.NO_CONTENT_204 );
        response.send ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: allowedOrigin
    //
    // Description:
    //
    //   Resolves one exact allowed Origin header value.
    //
    // Arguments:
    //
    //   request (RoutingRequest):
    //     The incoming request.
    //
    // Returns:
    //
    //   The allowed origin when the request carries exactly one match.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private Optional <String> allowedOrigin ( RoutingRequest request )
    {
        // Reject missing, repeated, wildcard, opaque, malformed, and unconfigured Origin values by exact comparison.

        List <String> originValues = request.headers ().values ( HeaderNames.ORIGIN );

        if ( originValues.size () != 1 )
        {
            return Optional.empty ();
        }

        String origin = originValues.getFirst ();

        return this.configuration.getAllowedOrigins ().contains ( origin )
            ? Optional.of ( origin )
            : Optional.empty ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: isPreflight
    //
    // Description:
    //
    //   Indicates whether the request is a browser CORS preflight.
    //
    // Arguments:
    //
    //   request (RoutingRequest):
    //     The incoming request.
    //
    // Returns:
    //
    //   `true` for an OPTIONS request carrying Origin and Access-Control-Request-Method; otherwise `false`.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static boolean isPreflight ( RoutingRequest request )
    {
        // Return whether the request has the complete CORS preflight shape.

        return request.prologue ().method ().equals ( Method.OPTIONS )
            && request.headers ().contains ( HeaderNames.ORIGIN )
            && request.headers ().contains ( HeaderNames.ACCESS_CONTROL_REQUEST_METHOD );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requestedMethod
    //
    // Description:
    //
    //   Validates the requested preflight method token.
    //
    // Arguments:
    //
    //   request (RoutingRequest):
    //     The incoming request.
    //
    // Returns:
    //
    //   The uppercase method token when valid.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Optional <String> requestedMethod ( RoutingRequest request )
    {
        // Read exactly one requested method value.

        List <String> methodValues = request.headers ().values ( HeaderNames.ACCESS_CONTROL_REQUEST_METHOD );

        if ( methodValues.size () != 1 )
        {
            return Optional.empty ();
        }

        String requestedMethod = methodValues.getFirst ().trim ();
        String normalizedMethod = requestedMethod.toUpperCase ( Locale.ROOT );

        // Reject blank, mixed-case, and otherwise non-canonical method tokens.

        return requestedMethod.equals ( normalizedMethod ) && !requestedMethod.isEmpty ()
            ? Optional.of ( normalizedMethod )
            : Optional.empty ();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: requestedHeaders
    //
    // Description:
    //
    //   Validates requested preflight headers against the fixed browser transport header set.
    //
    // Arguments:
    //
    //   request (RoutingRequest):
    //     The incoming request.
    //
    // Returns:
    //
    //   The canonical requested header names, or empty when validation fails.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Optional <List <String>> requestedHeaders ( RoutingRequest request )
    {
        // Initialize a stable, duplicate-free canonical response collection.

        LinkedHashSet <String> canonicalHeaders = new LinkedHashSet <> ();

        // Process each header line and comma-separated requested header token.

        for ( String headerValue : request.headers ().values ( HeaderNames.ACCESS_CONTROL_REQUEST_HEADERS ) )
        {
            for ( String requestedHeader : headerValue.split ( ",", -1 ) )
            {
                String canonicalHeader = canonicalRequestHeader ( requestedHeader.trim () );

                if ( canonicalHeader == null )
                {
                    return Optional.empty ();
                }

                canonicalHeaders.add ( canonicalHeader );
            }
        }

        // Return the validated immutable requested-header sequence to the caller.

        return Optional.of ( List.copyOf ( canonicalHeaders ) );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: canonicalRequestHeader
    //
    // Description:
    //
    //   Maps one allowed request header to its stable response spelling.
    //
    // Arguments:
    //
    //   value (String):
    //     The requested header name.
    //
    // Returns:
    //
    //   The canonical allowed name, or `null` when denied.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String canonicalRequestHeader ( String value )
    {
        // Return the fixed canonical spelling for the browser transport's permitted request headers.

        return switch ( value.toLowerCase ( Locale.ROOT ) )
        {
            case "accept"        -> "Accept";
            case "authorization" -> "Authorization";
            case "content-type"  -> "Content-Type";
            default              -> null;
        };
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: privateNetworkRequested
    //
    // Description:
    //
    //   Validates the transitional private-network preflight request header when present.
    //
    // Arguments:
    //
    //   request (RoutingRequest):
    //     The incoming request.
    //
    // Returns:
    //
    //   Whether the compatibility response is requested, or empty when the request is malformed.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Optional <Boolean> privateNetworkRequested ( RoutingRequest request )
    {
        // Read the transitional header without enabling it for requests that did not ask for it.

        List <String> values = request.headers ().values ( REQUEST_PRIVATE_NETWORK_HEADER );

        if ( values.isEmpty () )
        {
            return Optional.of ( false );
        }

        if ( values.size () != 1 || !values.getFirst ().equalsIgnoreCase ( "true" ) )
        {
            return Optional.empty ();
        }

        return Optional.of ( true );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: routeAllowsMethod
    //
    // Description:
    //
    //   Indicates whether one API path exposes the requested browser method.
    //
    // Arguments:
    //
    //   path (String):
    //     The absolute API path.
    //
    //   method (String):
    //     The requested uppercase HTTP method.
    //
    // Returns:
    //
    //   `true` when the exact route and method are browser-accessible; otherwise `false`.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static boolean routeAllowsMethod ( String path, String method )
    {
        // Return the fixed method policy for each versioned API route.

        return switch ( path )
        {
            case EcaHttpServer.LIVENESS_PATH, EcaHttpServer.READINESS_PATH -> method.equals ( "GET" );
            case EcaHttpServer.MODEL_PATH                                  -> method.equals ( "GET" )
                || method.equals ( "PUT" );
            case EcaHttpServer.EVALUATION_PATH, EcaHttpServer.STOP_PATH    -> method.equals ( "POST" );
            default                                                        -> false;
        };
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: addActualResponseHeaders
    //
    // Description:
    //
    //   Adds exact-origin and revision-exposure headers to one actual response.
    //
    // Arguments:
    //
    //   response (RoutingResponse):
    //     The outgoing response.
    //
    //   origin (String):
    //     The validated exact origin.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void addActualResponseHeaders ( RoutingResponse response, String origin )
    {
        // Add the shared exact-origin boundary and expose only model revision response headers.

        addOriginResponseHeaders ( response, origin );
        response.header ( HeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS, EXPOSED_RESPONSE_HEADERS );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: addOriginResponseHeaders
    //
    // Description:
    //
    //   Adds the shared exact-origin and cache-variance response headers.
    //
    // Arguments:
    //
    //   response (RoutingResponse):
    //     The outgoing response.
    //
    //   origin (String):
    //     The validated exact origin.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void addOriginResponseHeaders ( RoutingResponse response, String origin )
    {
        // Emit an exact origin rather than a wildcard or arbitrary reflected request value.

        response.header ( HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, origin );
        response.header ( HeaderNames.VARY, "Origin" );
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: rejectPreflight
    //
    // Description:
    //
    //   Rejects a preflight without emitting any cross-origin readability header.
    //
    // Arguments:
    //
    //   response (RoutingResponse):
    //     The outgoing response.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static void rejectPreflight ( RoutingResponse response )
    {
        // Fail closed with an unreadable empty response.

        response.status ( Status.FORBIDDEN_403 );
        response.send ();
    }
}
