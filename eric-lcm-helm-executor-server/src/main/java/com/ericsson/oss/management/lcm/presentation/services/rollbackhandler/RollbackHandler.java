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

package com.ericsson.oss.management.lcm.presentation.services.rollbackhandler;

import java.nio.file.Path;

import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;
import com.ericsson.oss.management.lcm.model.internal.RollbackData;

/**
 * This service will handle rollback logic, when requested operation failed
 */
public interface RollbackHandler {
    /**
     * Fetch previous helmsource, values, store them and prepare rollback command
     *
     * @param instance
     * @param failedOperation
     * @param paths
     * @param directory
     * @return rollback data which contains command, paths and helmSourceType required for auto-rollback
     */
    RollbackData prepareRollbackData(WorkloadInstance instance, Operation failedOperation, FilePathDetails paths, Path directory);
}
