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
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.CONTAINER_CPU_REQ;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.CONTAINER_MEM_LIMIT;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.CONTAINER_MEM_REQ;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.CONTAINER_NAME;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.IMAGE_SIZE;

import io.qameta.allure.internal.shadowed.jackson.annotation.JsonProperty;
import io.qameta.allure.internal.shadowed.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonPropertyOrder({"image-size", "name", "cpu-req", "cpu-limit", "mem-req", "mem-limit"})
public class Container {
    @JsonProperty("image-size")
    private String imageSize;
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
        this.imageSize = IMAGE_SIZE;
        this.name = CONTAINER_NAME;
        this.cpuReq = CONTAINER_CPU_REQ;
        this.cpuLimit = CONTAINER_CPU_LIMIT;
        this.memReq = CONTAINER_MEM_REQ;
        this.memLimit = CONTAINER_MEM_LIMIT;
    }
}
