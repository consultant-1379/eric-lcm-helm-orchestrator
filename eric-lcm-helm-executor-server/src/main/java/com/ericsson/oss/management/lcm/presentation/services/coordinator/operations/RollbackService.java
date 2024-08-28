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
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;

/**
 * Service which rollback workload instance
 */
public interface RollbackService {

    /**
     * Rollback workload instance
     *
     * @param instance to rollback
     * @param version to which rollback is required, can be null
     * @param clusterConnectionInfo
     * @return instance
     */
    WorkloadInstance rollback(WorkloadInstance instance, WorkloadInstanceVersion version, MultipartFile clusterConnectionInfo);

}
