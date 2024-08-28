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

package com.ericsson.oss.management.lcm.presentation.services.release;

import java.util.List;

import com.ericsson.oss.management.lcm.model.entity.HelmSourceType;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.Release;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandResponse;

/**
 * Work with releases
 */
public interface ReleaseService {

    /**
     * Extract and save releases in DB
     *
     * @param paths
     * @param instance
     * @return response of list command execution
     */
    CommandResponse extractAndSaveReleases(FilePathDetails paths, WorkloadInstance instance);

    /**
     * Get releases by workload Instance
     *
     * @param instance
     * @return list of releases
     */
    List<Release> getByWorkloadInstance(WorkloadInstance instance);

    /**
     * Delete releases by workload instance
     *
     * @param instance
     */
    void deleteReleasesByWorkloadInstance(WorkloadInstance instance);

    /**
     * Handle orphaned release
     *
     * @param operation
     * @param instance
     * @param paths
     * @param helmSourceType
     * @param result
     * @return response of list command execution
     */
    CommandResponse handleOrphanedReleases(Operation operation, WorkloadInstance instance, FilePathDetails paths,
                                HelmSourceType helmSourceType, CommandResponse result);
}
