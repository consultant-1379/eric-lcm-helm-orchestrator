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

package com.ericsson.oss.management.lcm.presentation.services.coordinator.operations;

import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;

/**
 * Service which terminate workload instance
 */
public interface TerminateService {

    /**
     * Terminate workload instance
     *
     * @param instance to terminate
     * @param clusterConnectionInfo to run operation with cluster
     * @param deleteNamespace flag to specify if namespace must be deleted
     * @return updated workload instance
     */
    WorkloadInstance terminate(WorkloadInstance instance, MultipartFile clusterConnectionInfo, boolean deleteNamespace);

}
