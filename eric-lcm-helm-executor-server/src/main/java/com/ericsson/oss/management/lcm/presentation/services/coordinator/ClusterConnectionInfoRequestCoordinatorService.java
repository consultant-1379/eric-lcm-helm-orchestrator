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

package com.ericsson.oss.management.lcm.presentation.services.coordinator;

import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.api.model.ClusterConnectionInfoDto;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;

/**
 * This Service will coordinate cluster connection info requests and coordination between cluster connection info and workload instances
 */
public interface ClusterConnectionInfoRequestCoordinatorService {

    /**
     * Verify a cluster connection info file is valid and can connect to cluster before persisting
     *
     * @param clusterConnectionInfoFile
     * @param crdNamespace
     * @return ClusterConnectionInfoDto which has been persisted.
     */
    ClusterConnectionInfoDto create(MultipartFile clusterConnectionInfoFile, String crdNamespace);


    /**
     * Connect workload instance to cluster if it's required
     * @param instance
     */
    void connectToClusterConnectionInfoIfPresent(WorkloadInstance instance);

    /**
     * Disconnect workloadInstance from cluster connection info if present
     * @param instance
     */
    void disconnectFromClusterIfPresent(WorkloadInstance instance);
}
