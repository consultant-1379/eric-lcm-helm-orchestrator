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

package com.ericsson.oss.mgmt.lcm.acceptance.models.characteristics;

import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.MODEL_VERSION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.qameta.allure.internal.shadowed.jackson.annotation.JsonProperty;
import io.qameta.allure.internal.shadowed.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonPropertyOrder({"model_version", "service", "resource-configuration", "results", "test-environment"})
public class ADPMicroserviceCharacteristicsReport {
    @JsonProperty("model_version")
    private String modelVersion;
    private Service service;
    @JsonProperty("resource-configuration")
    private List<ResourceConfiguration> resourceConfigurations;
    private List<Result> results;
    @JsonProperty("test-environment")
    private TestEnvironment testEnvironment;

    public ADPMicroserviceCharacteristicsReport() {
        this.modelVersion = MODEL_VERSION;
        this.service = new Service();
        this.resourceConfigurations = Collections.singletonList(new ResourceConfiguration());
        this.results = new ArrayList<>();
        this.testEnvironment = new TestEnvironment();
    }
}
