//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Verifies the production Maven module graph against the explicitly permitted inter-module dependencies.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

//*********************************************************************************************************************
// Class: ModuleDependencyGraphTest
//
// Description:
//
//   Verifies the production Maven module graph against the explicitly permitted inter-module dependencies.
//
//*********************************************************************************************************************

final class ModuleDependencyGraphTest
{
    private static final Map <String, Set <String>> EXPECTED_DEPENDENCIES = Map.of (
        "eca-domain", Set.of (),
        "eca-model", Set.of ( "eca-domain" ),
        "eca-engine", Set.of ( "eca-domain" ),
        "eca-application", Set.of ( "eca-domain", "eca-model", "eca-engine" ),
        "eca-json", Set.of ( "eca-model", "eca-application" ),
        "eca-server", Set.of ( "eca-application", "eca-json" ),
        "eca-client", Set.of ( "eca-application", "eca-json" )
    );

    //=================================================================================================================
    // Methods
    //=================================================================================================================

    //-----------------------------------------------------------------------------------------------------------------
    // Method: productionModules_obeyPermittedProjectDependencyGraph
    //
    // Description:
    //
    //   Verifies that production modules obey permitted project dependency graph.
    //
    //-----------------------------------------------------------------------------------------------------------------

    @Test
    void productionModules_obeyPermittedProjectDependencyGraph () throws Exception
    {
        // Initialize the project root by applying of and get property.

        Path projectRoot = Path.of ( System.getProperty ( "eca.project.root" ) );

        // Process each expected entry supplied by expected dependencies entry set.

        for ( Map.Entry <String, Set <String>> expectedEntry : EXPECTED_DEPENDENCIES.entrySet () )
        {
            // Initialize the actual dependencies by applying read production project dependencies, resolve, and get
            // key.

            Set <String> actualDependencies = readProductionProjectDependencies (
                projectRoot.resolve ( expectedEntry.getKey () ).resolve ( "pom.xml" )
            );

            // Verify the production modules obey permitted project dependency graph scenario against its expected
            // outcome.

            assertThat ( actualDependencies )
                .as ( expectedEntry.getKey () + " project dependencies" )
                .containsExactlyInAnyOrderElementsOf ( expectedEntry.getValue () );
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: readProductionProjectDependencies
    //
    // Description:
    //
    //   Performs the read production project dependencies operation.
    //
    // Arguments:
    //
    //   pomPath (Path):
    //     The pom path to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static Set <String> readProductionProjectDependencies ( Path pomPath ) throws Exception
    {
        // Prepare the document builder factory, dependency nodes, and dependencies values needed by the read
        // production project dependencies operation.

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance ();
        NodeList dependencyNodes = documentBuilderFactory.newDocumentBuilder ()
            .parse ( pomPath.toFile () )
            .getElementsByTagName ( "dependency" );
        Set <String> dependencies = new LinkedHashSet <> ();

        // Repeat the loop while i is less than dependency nodes length.

        for ( int i = 0; i < dependencyNodes.getLength (); i++ )
        {
            // Prepare the dependency element, group ID, and scope values needed by the read production project
            // dependencies operation.

            Element dependencyElement = ( Element ) dependencyNodes.item ( i );
            String groupId = childText ( dependencyElement, "groupId" );
            String scope = childText ( dependencyElement, "scope" );

            // Handle the branch where "com rohingosling eca" matches group ID and "test" differs from scope.

            if ( "com.rohingosling.eca".equals ( groupId ) && !"test".equals ( scope ) )
            {
                // Add child text dependency element "artifact id" to the dependencies.

                dependencies.add ( childText ( dependencyElement, "artifactId" ) );
            }
        }

        // Return the dependencies to the caller.

        return dependencies;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Method: childText
    //
    // Description:
    //
    //   Performs the child text operation.
    //
    // Arguments:
    //
    //   parentElement (Element):
    //     The parent element to use.
    //
    //   childName (String):
    //     The child name to use.
    //
    // Returns:
    //
    //   The result of the operation.
    //
    //-----------------------------------------------------------------------------------------------------------------

    private static String childText ( Element parentElement, String childName )
    {
        // Initialize the child nodes by applying get elements by tag name.

        NodeList childNodes = parentElement.getElementsByTagName ( childName );

        // Return the value selected according to child nodes length equals 0.

        return childNodes.getLength () == 0 ? "" : childNodes.item ( 0 ).getTextContent ().trim ();
    }
}
