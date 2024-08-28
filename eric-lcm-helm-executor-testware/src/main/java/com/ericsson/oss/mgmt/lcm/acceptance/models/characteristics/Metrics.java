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

import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.CPU_AVG_MILLI_CORES;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.MEMORY_AVG_MIB;

import io.qameta.allure.internal.shadowed.jackson.annotation.JsonProperty;
import io.qameta.allure.internal.shadowed.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonPropertyOrder({"cpu-avg-milli-cores", "memory_avg_mib"})
public class Metrics {
    @JsonProperty("cpu-avg-milli-cores")
    private double cpuAvgMilliCores;
    @JsonProperty("memory_avg_mib")
    private int memoryAvgMib;

    public Metrics() {
        this.cpuAvgMilliCores = CPU_AVG_MILLI_CORES;
        this.memoryAvgMib = MEMORY_AVG_MIB;
    }
}
