//---------------------------------------------------------------------------------------------------------------------
// Project: ECA Rule Engine 2
// Version: 2.0
// Date:    2025
// Author:  Rohin Gosling
//
// Description:
//
//   Defines executable package-dependency boundaries for the production modules.
//
// TODO:
//
//   None.
//
//---------------------------------------------------------------------------------------------------------------------

package com.rohingosling.eca.tests;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses (
    packages = "com.rohingosling.eca",
    importOptions = ImportOption.DoNotIncludeTests.class
)

//*********************************************************************************************************************
// Class: ArchitectureRulesTest
//
// Description:
//
//   Defines executable package-dependency boundaries for the production modules.
//
//*********************************************************************************************************************

final class ArchitectureRulesTest
{
    @ArchTest
    static final ArchRule DOMAIN_DEPENDENCIES = noClasses ()
        .that ().resideInAPackage ( "..domain.." )
        .should ().dependOnClassesThat ()
        .resideOutsideOfPackages ( "java..", "com.rohingosling.eca.domain.." )
        .allowEmptyShould ( true );

    @ArchTest
    static final ArchRule MODEL_DEPENDENCIES = noClasses ()
        .that ().resideInAPackage ( "..model.." )
        .should ().dependOnClassesThat ()
        .resideOutsideOfPackages (
            "java..",
            "com.rohingosling.eca.domain..",
            "com.rohingosling.eca.model.."
        )
        .allowEmptyShould ( true );

    @ArchTest
    static final ArchRule ENGINE_DEPENDENCIES = noClasses ()
        .that ().resideInAPackage ( "..engine.." )
        .should ().dependOnClassesThat ()
        .resideOutsideOfPackages (
            "java..",
            "com.rohingosling.eca.domain..",
            "com.rohingosling.eca.engine.."
        )
        .allowEmptyShould ( true );

    @ArchTest
    static final ArchRule APPLICATION_DEPENDENCIES = noClasses ()
        .that ().resideInAPackage ( "..application.." )
        .should ().dependOnClassesThat ()
        .resideOutsideOfPackages (
            "java..",
            "com.rohingosling.eca.application..",
            "com.rohingosling.eca.domain..",
            "com.rohingosling.eca.engine..",
            "com.rohingosling.eca.model.."
        )
        .allowEmptyShould ( true );

    @ArchTest
    static final ArchRule JSON_DEPENDENCIES = noClasses ()
        .that ().resideInAPackage ( "..json.." )
        .should ().dependOnClassesThat ()
        .resideOutsideOfPackages (
            "java..",
            "com.fasterxml.jackson..",
            "com.rohingosling.eca.application..",
            "com.rohingosling.eca.json..",
            "com.rohingosling.eca.model.."
        )
        .allowEmptyShould ( true );

    @ArchTest
    static final ArchRule SERVER_DEPENDENCIES = noClasses ()
        .that ().resideInAPackage ( "..server.." )
        .should ().dependOnClassesThat ()
        .resideOutsideOfPackages (
            "java..",
            "com.rohingosling.eca.application..",
            "com.rohingosling.eca.json..",
            "com.rohingosling.eca.server..",
            "picocli..",
            "io.helidon.."
        )
        .allowEmptyShould ( true );

    @ArchTest
    static final ArchRule CLIENT_DEPENDENCIES = noClasses ()
        .that ().resideInAPackage ( "..client.." )
        .should ().dependOnClassesThat ()
        .resideOutsideOfPackages (
            "java..",
            "com.rohingosling.eca.application..",
            "com.rohingosling.eca.client..",
            "com.rohingosling.eca.json..",
            "javafx.."
        )
        .allowEmptyShould ( true );
}
