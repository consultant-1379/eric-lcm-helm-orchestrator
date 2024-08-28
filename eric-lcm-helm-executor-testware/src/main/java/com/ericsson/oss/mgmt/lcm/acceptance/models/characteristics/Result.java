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

import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.LABELS;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.DURATION;

import java.util.Collections;
import java.util.List;

import io.qameta.allure.internal.shadowed.jackson.annotation.JsonProperty;
import io.qameta.allure.internal.shadowed.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonPropertyOrder({"use-case", "description", "duration", "labels", "service-configuration",
        "used-resources-configuration", "traffic", "metrics", "additional-results"})
public class Result {
    @JsonProperty("use-case")
    private String useCase;
    private String description;
    private int duration;
    private List<String> labels;
    @JsonProperty("service-configuration")
    private Object serviceConfiguration;
    @JsonProperty("used-resources-configuration")
    private UsedResourcesConfiguration usedResourcesConfiguration;
    private Traffic traffic;
    private List<Metric> metrics;
    @JsonProperty("additional-results")
    private AdditionalResults additionalResults;

    public Result() {
        this.duration = DURATION;
        this.labels = LABELS;
        this.serviceConfiguration = new Object();
        this.usedResourcesConfiguration = new UsedResourcesConfiguration();
        this.traffic = new Traffic();
        this.metrics = Collections.singletonList(new Metric());
        this.additionalResults = new AdditionalResults();
    }
}
