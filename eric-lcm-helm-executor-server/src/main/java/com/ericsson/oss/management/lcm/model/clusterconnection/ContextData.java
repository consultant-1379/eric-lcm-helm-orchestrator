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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ContextData {

    @NotNull(message = "Cluster name required in contextData")
    @Size(min = 1, message = "Cluster name empty in contextData")
    private String cluster;

    @NotNull(message = "Username required in context")
    @Size(min = 1, message = "Username empty in context")
    private String user;

    private String namespace;
}
