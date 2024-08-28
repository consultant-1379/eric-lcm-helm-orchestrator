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

import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.BOGOMIPS;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.CPU_MHZ;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.CPU_MODEL;

import io.qameta.allure.internal.shadowed.jackson.annotation.JsonProperty;
import io.qameta.allure.internal.shadowed.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonPropertyOrder({"model", "CPU-MHz", "bogomips"})
public class CPU {
    private String model;
    @JsonProperty("CPU-MHz")
    private int cpuMhz;
    private double bogomips;

    public CPU() {
        this.model = CPU_MODEL;
        this.cpuMhz = CPU_MHZ;
        this.bogomips = BOGOMIPS;
    }
}
