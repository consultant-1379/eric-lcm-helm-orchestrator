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

package com.ericsson.oss.management.lcm.presentation.services.async.executor;

import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.HelmSourceType;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;

/**
 * Execute an action asynchronously
 */
public interface AsyncExecutor {
    /**
     * Take a list of commands and execute them asynchronously
     * Do the rollback if execution failed
     * @param operation
     * @param instance
     * @param paths
     * @param command
     */
    void executeAndUpdateOperation(Operation operation, WorkloadInstance instance, FilePathDetails paths,
                                   HelmSource helmSource, String command);

    /**
     * Take a list of commands when destroy helmSource and execute them asynchronously
     * @param operation
     * @param paths
     * @param command
     * @param instance
     * @param helmSourceType
     * @param deleteNamespace
     */
    void executeAndUpdateOperationForTerminate(Operation operation, FilePathDetails paths, String command, WorkloadInstance instance,
                                               HelmSourceType helmSourceType, boolean deleteNamespace);

    /**
     * Take a list of commands when rollback helmSource and execute them asynchronously
     * @param operation
     * @param paths
     * @param command
     * @param version WorkloadInstanceVersion
     */
    void executeAndUpdateOperationForRollback(Operation operation, FilePathDetails paths, String command,
                                              WorkloadInstanceVersion version, HelmSourceType helmSourceType);
}
