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

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ClusterData {

    @JsonProperty("certificate-authority-data")
    private String certificateAuthorityData;

    @NotNull(message = "Server details required")
    @Size(min = 1, message = "Server details empty")
    private String server;

    @JsonProperty("insecure-skip-tls-verify")
    private Boolean insecureSkipTlsVerify;
}
