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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RollbackData {

    private Map<String, Object> versionRequest = new HashMap<>();
    private String clusterConnectionInfoPath;

    public RollbackData(Integer version, String clusterConnectionInfoPath) {
        this.versionRequest = Collections.singletonMap("version", version);
        this.clusterConnectionInfoPath = clusterConnectionInfoPath;
    }

}
