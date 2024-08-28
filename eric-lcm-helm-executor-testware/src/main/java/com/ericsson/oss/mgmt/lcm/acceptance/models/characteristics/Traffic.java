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

import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.LATENCY;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.REQUEST_AVG_SIZE;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.TOTAL_ERRORS;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.TOTAL_REQUESTS;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.TPS;

import io.qameta.allure.internal.shadowed.jackson.annotation.JsonProperty;
import io.qameta.allure.internal.shadowed.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonPropertyOrder({"tps", "Request-total-avg-size", "total-requests", "total-errors", "latency"})
public class Traffic {
    private String tps;
    @JsonProperty("Request-total-avg-size")
    private String requestAvgSize;
    @JsonProperty("total-requests")
    private String totalRequests;
    @JsonProperty("total-errors")
    private String totalErrors;
    private String latency;

    public Traffic() {
        this.tps = TPS;
        this.requestAvgSize = REQUEST_AVG_SIZE;
        this.totalRequests = TOTAL_REQUESTS;
        this.totalErrors = TOTAL_ERRORS;
        this.latency = LATENCY;
    }
}
