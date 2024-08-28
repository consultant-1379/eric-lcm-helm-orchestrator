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
package com.ericsson.oss.mgmt.lcm.acceptance.models;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class WorkloadInstanceTestData {

    private String helmSourceLocation;
    private String helmSourceName;
    private String workloadInstanceName;
    private String namespace;
    private String valuesYaml;
    private String clusterConnectionInfo;
    private String clusterConnectionLocation;
    private Integer timeout;
    private Map<String, Object> additionalParameters;

}
