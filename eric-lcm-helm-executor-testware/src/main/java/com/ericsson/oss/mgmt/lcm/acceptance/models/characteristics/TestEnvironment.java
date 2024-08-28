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

import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.CAAS;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.KUBERNETES_VERSION;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.TEST_ENVIRONMENT_CLUSTER;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.TEST_ENVIRONMENT_MEMORY;

import io.qameta.allure.internal.shadowed.jackson.annotation.JsonProperty;
import io.qameta.allure.internal.shadowed.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonPropertyOrder({"cluster", "cpu", "memory", "caas", "kubernetes-version", "other-info"})
public class TestEnvironment {
    private String cluster;
    private CPU cpu;
    private String memory;
    private String caas;
    @JsonProperty("kubernetes-version")
    private String kubernetesVersion;
    @JsonProperty("other-info")
    private OtherInfo otherInfo;

    public TestEnvironment() {
        this.cluster = TEST_ENVIRONMENT_CLUSTER;
        this.cpu = new CPU();
        this.memory = TEST_ENVIRONMENT_MEMORY;
        this.caas = CAAS;
        this.kubernetesVersion = KUBERNETES_VERSION;
        this.otherInfo = new OtherInfo();
    }
}
