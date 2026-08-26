//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Adds the client version resource to GluonFX's icon resource conversion so Microsoft LINK receives one
//   merged resource object.
//
// TODO:
//
//   1. None.
//
//---------------------------------------------------------------------------------------------------------------------

#include <process.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

//---------------------------------------------------------------------------------------------------------------------
// Function: containsCaseInsensitive
//
// Description:
//
//   Returns nonzero when text contains the requested value without regard to ASCII letter case.
//
//---------------------------------------------------------------------------------------------------------------------

static int containsCaseInsensitive ( const char *text, const char *value )
{
	size_t textLength;
	size_t valueLength;
	size_t i;

	// Measure both strings before testing the candidate substring positions.

	textLength  = strlen ( text );
	valueLength = strlen ( value );

	// Reject values that cannot fit within the supplied text.

	if ( valueLength > textLength )
	{
		// Return the no-match result to the caller.

		return 0;
	}

	// Test every viable starting position for an ASCII case-insensitive match.

	for ( i = 0; i <= textLength - valueLength; i++ )
	{
		// Accept the first substring whose characters match the requested value.

		if ( _strnicmp ( text + i, value, valueLength ) == 0 )
		{
			// Return the match result to the caller.

			return 1;
		}
	}

	// Return the no-match result after every candidate position has been checked.

	return 0;
}

//---------------------------------------------------------------------------------------------------------------------
// Function: main
//
// Description:
//
//   Forwards CVTRES arguments to the real Visual Studio executable and merges the client version resource into
//   GluonFX's IconGroup object conversion.
//
//---------------------------------------------------------------------------------------------------------------------

int main ( int argumentCount, char *argumentValues[] )
{
	char       *realResourceConverterPath;
	char       *quotedResourceConverterPath;
	char       *versionResourcePath;
	const char **childArgumentValues;
	int         childArgumentCount;
	int         childExitCode;
	int         mergeVersionResource;
	int         i;

	// Initialize the environment-owned paths before requesting their allocated values.

	realResourceConverterPath   = NULL;
	quotedResourceConverterPath = NULL;
	versionResourcePath         = NULL;

	// Stop when the real Visual Studio resource converter path is unavailable.

	if ( _dupenv_s ( &realResourceConverterPath, NULL, "ECA_REAL_CVTRES" ) != 0
		|| realResourceConverterPath == NULL )
	{
		// Report the missing converter path before returning the failure status.

		fputs ( "ERROR: ECA_REAL_CVTRES does not identify the Visual Studio x64 resource converter.\n", stderr );

		// Return a failure status because argument forwarding cannot continue.

		return 1;
	}

	// Stop when the compiled client version resource path is unavailable.

	if ( _dupenv_s ( &versionResourcePath, NULL, "ECA_CLIENT_VERSION_RESOURCE" ) != 0
		|| versionResourcePath == NULL )
	{
		// Report the missing resource and release the previously allocated environment value.

		fputs ( "ERROR: ECA_CLIENT_VERSION_RESOURCE does not identify the compiled client version resource.\n", stderr );
		free ( realResourceConverterPath );

		// Return a failure status because the resource cannot be merged.

		return 1;
	}

	// Allocate enough space to quote the converter path for the child argument vector.

	quotedResourceConverterPath = malloc ( strlen ( realResourceConverterPath ) + 3 );

	// Stop and release acquired paths when the quoted-path allocation fails.

	if ( quotedResourceConverterPath == NULL )
	{
		// Report the allocation failure and release both environment-owned path values.

		fputs ( "ERROR: Unable to allocate the quoted CVTRES executable path.\n", stderr );
		free ( versionResourcePath );
		free ( realResourceConverterPath );

		// Return a failure status because the child command cannot be constructed.

		return 1;
	}

	// Format the converter path as the quoted first argument expected by the child process.

	sprintf_s (
		quotedResourceConverterPath,
		strlen ( realResourceConverterPath ) + 3,
		"\"%s\"",
		realResourceConverterPath
	);

	mergeVersionResource = 0;

	// Inspect forwarded arguments to determine whether this is the IconGroup conversion.

	for ( i = 1; i < argumentCount; i++ )
	{
		// Mark the matching conversion so the version resource is appended exactly once.

		if ( containsCaseInsensitive ( argumentValues[ i ], "IconGroup.obj" ) )
		{
			mergeVersionResource = 1;
			break;
		}
	}

	// Size and allocate the child argument vector, including any appended resource and its null terminator.

	childArgumentCount  = argumentCount + mergeVersionResource;
	childArgumentValues = malloc ( ( childArgumentCount + 1 ) * sizeof ( *childArgumentValues ) );

	// Stop and release all acquired paths when the child argument allocation fails.

	if ( childArgumentValues == NULL )
	{
		// Report the allocation failure and release the quoted and environment-owned paths.

		fputs ( "ERROR: Unable to allocate the CVTRES forwarding argument list.\n", stderr );
		free ( quotedResourceConverterPath );
		free ( versionResourcePath );
		free ( realResourceConverterPath );

		// Return a failure status because the child argument vector cannot be constructed.

		return 1;
	}

	// Seed the child argument vector with the quoted converter executable path.

	childArgumentValues[ 0 ] = quotedResourceConverterPath;

	// Copy each caller-supplied argument into the child argument vector.

	for ( i = 1; i < argumentCount; i++ )
	{
		childArgumentValues[ i ] = argumentValues[ i ];
	}

	// Append the compiled version resource only for the IconGroup conversion.

	if ( mergeVersionResource )
	{
		childArgumentValues[ argumentCount ] = versionResourcePath;
	}

	// Terminate the child argument vector and wait for the real converter to finish.

	childArgumentValues[ childArgumentCount ] = NULL;
	childExitCode = ( int ) _spawnv ( _P_WAIT, realResourceConverterPath, childArgumentValues );

	// Report a process-launch failure while preserving the child status for the caller.

	if ( childExitCode == -1 )
	{
		// Add the operating-system error detail to the launch-failure message.

		perror ( "ERROR: Unable to start the real Visual Studio resource converter" );
	}

	// Release every dynamically allocated path and argument buffer after the child exits.

	free ( childArgumentValues );
	free ( quotedResourceConverterPath );
	free ( versionResourcePath );
	free ( realResourceConverterPath );

	// Return the real resource converter's exit status to the calling build.

	return childExitCode;
}
