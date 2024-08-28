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
package com.ericsson.oss.mgmt.lcm.acceptance.utils;

import com.ericsson.oss.mgmt.lcm.acceptance.models.ServiceInstance;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TestExecutionGlobalConfig {
    private TestExecutionGlobalConfig() {}

    public static final ServiceInstance SERVICE_INSTANCE;

    static {
        String ip = System.getProperty(Constants.SERVICE_IP);
        String port = System.getProperty(Constants.SERVICE_PORT);
        String deploymentNamespace = System.getProperty(Constants.DEPLOYMENT_NAMESPACE);
        String isLocalString = System.getProperty(Constants.IS_LOCAL);
        boolean isLocal = Boolean.parseBoolean(isLocalString);
        String namespace = System.getProperty(Constants.NAMESPACE);
        String clusterConnectionInfoPath = System.getProperty(Constants.KUBE_CONFIG_PATH);
        String chartRegistryUrl = System.getProperty(Constants.CHART_REGISTRY_URL);
        String chartRegistryUsername = System.getProperty(Constants.CHART_REGISTRY_USER);
        String chartRegistryPassword = System.getProperty(Constants.CHART_REGISTRY_PASSWD);
        log.info("Helm executor service url :: " + ip);
        log.info("Helm Chart Registry service url :: " + chartRegistryUrl);

        SERVICE_INSTANCE = new ServiceInstance(ip, port, deploymentNamespace, isLocal, namespace, clusterConnectionInfoPath,
                                  chartRegistryUrl, chartRegistryUsername, chartRegistryPassword);
    }

}
