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

import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.FLAVOR;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.INSTANCES;

import java.util.Collections;
import java.util.List;

import io.qameta.allure.internal.shadowed.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonPropertyOrder({"flavor", "resources"})
public class ResourceConfiguration {
    private String flavor;
    private List<Resource> resources;

    public ResourceConfiguration() {
        this.flavor = FLAVOR;
        this.resources = Collections.singletonList(new Resource());
    }

    @Getter
    @Setter
    @JsonPropertyOrder({"pod", "instances", "containers"})
    public static class Resource {
        private String pod;
        private int instances;
        private List<Container> containers;

        public Resource() {
            this.instances = INSTANCES;
            this.containers = Collections.singletonList(new Container());
        }
    }
}

