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

package com.ericsson.oss.management.lcm.presentation.services.deployment;

import java.nio.file.Path;

import com.ericsson.oss.management.lcm.api.model.DeploymentStateInfoDTO;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceHelmSourceUrl;

/**
 * Service for fetching data about deployment
 */
public interface DeploymentService {

    /**
     * Get deployment state in the cluster by namespace.
     * @param dto Object with workload instance details that contains namespace
     * @param clusterConfigInfoPath Path to kube config file
     * @return DeploymentStateInfoDTO, which contains general deployment information as well as the name and status of each pod
     */
    DeploymentStateInfoDTO getCurrentDeploymentState(WorkloadInstanceHelmSourceUrl dto, Path clusterConfigInfoPath);

}
