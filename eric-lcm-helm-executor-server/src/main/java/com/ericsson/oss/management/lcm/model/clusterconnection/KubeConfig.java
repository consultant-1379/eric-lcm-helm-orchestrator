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
package com.ericsson.oss.management.lcm.model.clusterconnection;

import java.util.List;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class KubeConfig {

    @NotNull(message = "Api version required")
    @Size(min = 1, message = "Api version empty")
    private String apiVersion;

    @NotNull(message = "Kind required")
    @Size(min = 1, message = "Kind empty")
    private String kind;

    @NotNull(message = "Current context required")
    @Size(min = 1, message = "Current context empty")
    @JsonProperty("current-context")
    private String currentContext;

    @NotNull(message = "Clusters required")
    @Size(min = 1, message = "Clusters empty")
    @Size(max = 1, message = "Only one cluster allowed")
    private List<Cluster> clusters;

    @NotNull(message = "Context required")
    @Size(min = 1, message = "Context empty")
    @Size(max = 1, message = "Only one context allowed")
    private List<Context> contexts;

    @NotNull(message = "User required")
    @Size(min = 1, message = "User empty")
    @Size(max = 1, message = "Only one user allowed")
    private List<User> users;

    private Object preferences;
}
