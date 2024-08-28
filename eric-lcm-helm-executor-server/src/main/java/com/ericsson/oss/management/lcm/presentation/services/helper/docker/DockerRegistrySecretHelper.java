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

package com.ericsson.oss.management.lcm.presentation.services.helper.docker;

import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;

import java.nio.file.Path;

/**
 * Helper class for work with docker registry secret
 */
public interface DockerRegistrySecretHelper {

    /**
     * Create docker registry secret
     * @param instance
     * @param kubeConfigPath
     */
    void createSecret(WorkloadInstance instance, Path kubeConfigPath);

    /**
     * Delete docker registry secret
     * @param instance
     * @param kubeConfig
     */
    void deleteSecret(WorkloadInstance instance, byte[] kubeConfig);
}
