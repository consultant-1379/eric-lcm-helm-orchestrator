/*******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.management.lcm;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures.LayeredArchitecture;

class ArchitecturalUnitTest {

    public static final String CONTROLLERS_LAYER = "Controllers";
    public static final String CONTROLLERS_PACKAGE = "..controllers";
    public static final String REST_MODEL_LAYER = "RestModel";
    public static final String REST_MODEL_PACKAGE = "..lcm.api.model";
    public static final String DTO_PACKAGE = "..dto";
    public static final String API_LAYER = "API";
    public static final String API_PACKAGE = "..lcm.api";
    public static final String EXCEPTIONS_LAYER = "Exceptions";
    public static final String EXCEPTIONS_PACKAGE = "..exceptions";
    public static final String SERVICES_LAYER = "Services";
    public static final String SERVICES_PACKAGE = "..services..";
    public static final String MAPPERS_LAYER = "Mappers";
    public static final String MAPPERS_PACKAGE = "..mappers..";
    public static final String CONFIGURATION_LAYER = "Configuration";
    public static final String CONFIGURATION_PACKAGE = "..configurations..";
    public static final String ENTITIES_LAYER = "Entities";
    public static final String ENTITIES_PACKAGE = "..entity";
    public static final String REPOSITORIES_LAYER = "Repositories";
    public static final String REPOSITORIES_PACKAGE = "..repositories";
    public static final String UTILS_LAYER = "Utils";
    public static final String UTILS_PACKAGE = "..utils..";
    public static final String MODEL_INTERNAL_LAYER = "ModelInternal";
    public static final String MODEL_INTERNAL_PACKAGE = "..lcm.model.internal";

    private JavaClasses importedClasses = new ClassFileImporter()
            .withImportOption(new DoNotIncludeTests())
            .importPackages("com.ericsson.oss.management.lcm");

    private LayeredArchitecture architecture = layeredArchitecture()
            .layer(API_LAYER)
            .definedBy(API_PACKAGE)
            .layer(REST_MODEL_LAYER)
            .definedBy(REST_MODEL_PACKAGE, DTO_PACKAGE)
            .layer(CONTROLLERS_LAYER)
            .definedBy(CONTROLLERS_PACKAGE)
            .layer(EXCEPTIONS_LAYER)
            .definedBy(EXCEPTIONS_PACKAGE)
            .layer(SERVICES_LAYER)
            .definedBy(SERVICES_PACKAGE)
            .layer(MAPPERS_LAYER)
            .definedBy(MAPPERS_PACKAGE)
            .layer(ENTITIES_LAYER)
            .definedBy(ENTITIES_PACKAGE)
            .layer(REPOSITORIES_LAYER)
            .definedBy(REPOSITORIES_PACKAGE)
            .layer(UTILS_LAYER)
            .definedBy(UTILS_PACKAGE)
            .layer(CONFIGURATION_LAYER)
            .definedBy(CONFIGURATION_PACKAGE)
            .layer(MODEL_INTERNAL_LAYER)
            .definedBy(MODEL_INTERNAL_PACKAGE);

    @Test
    void verifyControllerClasses() {
        ArchRule controllerAccess = architecture
                .whereLayer(CONTROLLERS_LAYER)
                .mayNotBeAccessedByAnyLayer();

        controllerAccess.check(importedClasses);
    }

    @Test
    void verifyRestModelClasses() {
        ArchRule restModelAccess = architecture
                .whereLayer(REST_MODEL_LAYER)
                .mayOnlyBeAccessedByLayers(CONTROLLERS_LAYER, API_LAYER, EXCEPTIONS_LAYER, SERVICES_LAYER,
                        MAPPERS_LAYER, UTILS_LAYER);

        restModelAccess.check(importedClasses);
    }

    @Test
    void verifyServiceClassesStructure() {
        ArchRule serviceRule = architecture
                .whereLayer(SERVICES_LAYER)
                .mayOnlyBeAccessedByLayers(CONTROLLERS_LAYER, SERVICES_LAYER, UTILS_LAYER, CONFIGURATION_LAYER);

        serviceRule.check(importedClasses);
    }

    @Test
    void verifyConfigurationClasses() {
        ArchRule controllerAccess = layeredArchitecture()
                .layer(CONFIGURATION_LAYER)
                .definedBy(CONFIGURATION_PACKAGE)
                .whereLayer(CONFIGURATION_LAYER)
                .mayNotBeAccessedByAnyLayer();
        controllerAccess.check(importedClasses);
    }

    @Test
    void verifyEntityModelClasses() {
        ArchRule entityRule = architecture
                .whereLayer(ENTITIES_LAYER)
                .mayOnlyBeAccessedByLayers(SERVICES_LAYER, REPOSITORIES_LAYER, MAPPERS_LAYER, UTILS_LAYER,
                        MODEL_INTERNAL_LAYER);

        entityRule.check(importedClasses);
    }

    @Test
    void verifyRepositoryClasses() {
        ArchRule repositoryRule = architecture
                .whereLayer(REPOSITORIES_LAYER)
                .mayOnlyBeAccessedByLayers(SERVICES_LAYER, CONFIGURATION_LAYER, UTILS_LAYER);

        repositoryRule.check(importedClasses);
    }

}
