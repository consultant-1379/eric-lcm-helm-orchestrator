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

package com.ericsson.oss.management.lcm.presentation.services.workloadinstance;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.api.model.PagedWorkloadInstanceDto;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;

/**
 * Working with WorkloadInstance
 */
public interface WorkloadInstanceService {

    /**
     * Create an instance of WorkloadInstance class
     * @param workloadInstance
     * @return instance of WorkloadInstance already saved to DB
     */
    WorkloadInstance create(WorkloadInstance workloadInstance);

    /**
     * Get WorkloadInstance by id
     * @param workloadInstanceId
     * @return WorkloadInstance object
     */
    WorkloadInstance get(String workloadInstanceId);

    /**
     * Get Paged Operations by page, size and sort parameters
     * @param page
     * @param size
     * @param sort
     * @return PagedOperationDto
     */
    PagedWorkloadInstanceDto getAllWorkloadInstances(Integer page, Integer size, List<String> sort);

    /**
     * Update additionalParameters of WorkloadInstance
     * @param workloadInstance
     * @return updated WorkloadInstance object
     */
    WorkloadInstance updateAdditionalParameters(WorkloadInstance workloadInstance);

    /**
     * Update WorkloadInstance with the new values
     * @param workloadInstance
     * @return updated WorkloadInstance object
     */
    WorkloadInstance update(WorkloadInstance workloadInstance);

    /**
     * Get true if all WorkloadInstances can be deleted by namespace and cluster clusterIdentifier
     * @param namespace
     * @param clusterIdentifier
     * @return boolean
     */
    boolean validateInstancesForDeletionInNamespace(String namespace, String clusterIdentifier);


    /**
     * Delete WorkloadInstance by Id
     * @param workloadInstanceId
     */
    void delete(String workloadInstanceId);

    /**
     * Update crdNamespace value in WorkloadInstance
     * @param instance
     * @param clusterConnectionInfo
     */
    void updateCrdNamespaceIfRequired(WorkloadInstance instance, MultipartFile clusterConnectionInfo);

    /**
     * Update version and previous version
     *
     * @param instance to update
     * @param workloadInstanceVersion version which must be set to workloadInstance
     */
    void updateVersion(WorkloadInstance instance, WorkloadInstanceVersion workloadInstanceVersion);
}
