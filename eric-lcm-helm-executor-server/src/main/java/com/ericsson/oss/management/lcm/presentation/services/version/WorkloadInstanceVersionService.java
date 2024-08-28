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

package com.ericsson.oss.management.lcm.presentation.services.version;

import java.util.List;

import com.ericsson.oss.management.lcm.api.model.PagedWorkloadInstanceVersionDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceVersionDto;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;

public interface WorkloadInstanceVersionService {

    /**
     * @param instance workloadInstance we find the version by
     * @param valuesVersion to save
     * @param helmSourceVersion to save
     * @return object
     */
    WorkloadInstanceVersion createVersion(WorkloadInstance instance, String valuesVersion,
                                          String helmSourceVersion);

    /**
     * Get version by workload instance
     *
     * @param instance
     * @return version
     */
    WorkloadInstanceVersion getVersion(WorkloadInstance instance);

    /**
     * Get previous version by workload instance
     *
     * @param instance
     * @return previous version
     */
    WorkloadInstanceVersion getPreviousVersion(WorkloadInstance instance);

    /**
     * Get WorkloadInstanceVersion entity by WorkloadInstanceId and version from the request
     * @param workloadInstanceId
     * @param version
     * @return entity from DB
     */
    WorkloadInstanceVersion getVersionByWorkloadInstanceIdAndVersion(String workloadInstanceId, Integer version);

    /**
     * Get WorkloadInstanceVersionDto entity by WorkloadInstanceId and version from the request
     * @param workloadInstanceId
     * @param version
     * @return dto of entity from DB
     */
    WorkloadInstanceVersionDto getVersionDtoByWorkloadInstanceIdAndVersion(String workloadInstanceId, Integer version);

    /**
     * Get WorkloadInstanceVersion entities by WorkloadInstanceId and version
     * @param workloadInstanceId
     * @param page
     * @param size
     * @param sort
     * @return entities from DB
     */
    PagedWorkloadInstanceVersionDto getAllVersionsByWorkloadInstance(String workloadInstanceId, Integer page,
                                                                     Integer size, List<String> sort);

    /**
     * Get version for rollback based on presence integer version to which rollback is required or previous type of operation
     *
     * @param instance
     * @param version to rollback, can be null
     * @param operationType of previous operation
     * @return workloadInstanceVersion entity
     */
    WorkloadInstanceVersion getVersionForRollback(WorkloadInstance instance, Integer version, OperationType operationType);

}
