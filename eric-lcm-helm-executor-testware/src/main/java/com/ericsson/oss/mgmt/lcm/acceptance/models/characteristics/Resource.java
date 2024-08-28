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

import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.CONTAINER_CPU_LIMIT;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.CONTAINER_MEM_LIMIT;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.CONTAINER_NAME;

import java.util.Collections;
import java.util.List;

import io.qameta.allure.internal.shadowed.jackson.annotation.JsonProperty;
import io.qameta.allure.internal.shadowed.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonPropertyOrder({"pod", "containers"})
public class Resource {
    private String pod;
    private List<Container> containers;

    public Resource() {
        this.containers = Collections.singletonList(new Container());
    }

    @Getter
    @Setter
    @JsonPropertyOrder({"name", "cpu-req", "cpu-limit", "mem-req", "mem-limit"})
    public static class Container {
        private String name;
        @JsonProperty("cpu-req")
        private String cpuReq;
        @JsonProperty("cpu-limit")
        private String cpuLimit;
        @JsonProperty("mem-req")
        private String memReq;
        @JsonProperty("mem-limit")
        private String memLimit;

        public Container() {
            this.name = CONTAINER_NAME;
            this.cpuLimit = CONTAINER_CPU_LIMIT;
            this.memLimit = CONTAINER_MEM_LIMIT;
        }
    }
}
