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

import java.util.List;

import io.qameta.allure.internal.shadowed.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MetricResponse {
    private List<Container> containers;

    @Getter
    @Setter
    @JsonPropertyOrder({"name", "usage"})
    public static class Container {
        private String name;
        private Usage usage;

        @Getter
        @Setter
        @JsonPropertyOrder({"cpu", "memory"})
        public static class Usage {
            private String cpu;
            private String memory;
        }
    }
}
