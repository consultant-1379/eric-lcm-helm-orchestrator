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

package com.ericsson.oss.management.lcm.presentation.services.clusterconnectioninfo;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.ericsson.oss.management.lcm.api.model.ClusterConnectionInfoDto;
import com.ericsson.oss.management.lcm.api.model.PagedClusterConnectionInfoDto;
import com.ericsson.oss.management.lcm.model.entity.ClusterConnectionInfo;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;

/**
 * Working with Cluster Connection Info
 */
public interface ClusterConnectionInfoService {

    /**
     * Create clusterConnectionInfo entity
     * @param clusterConnectionInfoPath
     * @param crdNamespace
     * @return ClusterConnectionInfoDto
     */
    ClusterConnectionInfoDto create(Path clusterConnectionInfoPath, String crdNamespace);

    /**
     * Get clusterConnectionInfo by id
     * @param id
     * @return ClusterConnectionInfoDto
     */
    ClusterConnectionInfoDto get(String id);

    /**
     * Get All Cluster Connection Info by page, size and sort parameters
     * @param page
     * @param size
     * @param sort
     * @return PagedClusterConnectionInfoDto
     */
    PagedClusterConnectionInfoDto getAllClusterConnectionInfo(Integer page, Integer size, List<String> sort);

    /**
     * Delete clusterConnectionInfo entity
     * @param id
     */
    void delete(String id);

    /**
     * Find a cluster by it's name
     * @param clusterName
     * @return Optional either containing the cluster or empty
     */
    Optional<ClusterConnectionInfo> findByClusterName(String clusterName);

    /**
     * Get clusterUrl from multipart file
     * @param clusterConnectionInfoFile
     * @return clusterUrl
     */
    String getClusterUrl(byte[] clusterConnectionInfoFile);

    /**
     * Get cluster identifier from file
     * @param clusterConnectionInfoPath
     * @return string cluster identifier
     */
    String resolveClusterIdentifier(Path clusterConnectionInfoPath);

    /**
     * verification of clusters identifiers
     * @param instance
     * @param clusterConnectionInfoPath
     */
    void verifyClusterIdentifier(WorkloadInstance instance, Path clusterConnectionInfoPath);
}
