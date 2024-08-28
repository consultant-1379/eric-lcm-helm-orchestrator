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

package com.ericsson.oss.management.lcm.presentation.services.operation;

import com.ericsson.oss.management.lcm.api.model.PagedOperationDto;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandResponse;

import java.util.List;

/**
 * Working with Operation
 */
public interface OperationService {
    /**
     * Create an instance of Operation class and save it to DB
     * @param operation to save
     * @return instance of operation already saved to DB
     */
    Operation create(Operation operation);

    /**
     * Get Operation by id
     * @param operationId to get from DB
     * @return Operation object
     */
    Operation get(String operationId);

    /**
     * Get all Operations
     * @return Operations list
     */
    List<Operation> getAll();

    /**
     * Get Paged Operations by page, size and sort parameters searched by workload instance id
     * @param workloadInstanceId
     * @param page
     * @param size
     * @param sort
     * @return PagedOperationDto
     */
    PagedOperationDto getByWorkloadInstanceId(String workloadInstanceId, Integer page, Integer size, List<String> sort);

    /**
     * Update Operation state and errorMessage
     * @param operation to update
     * @param commandResponse contains information about operation execution
     * @return operation with command response
     */
    Operation updateWithCommandResponse(Operation operation, CommandResponse commandResponse);

    /**
     * Get Operation logs by id
     * @param operationId to get from DB
     * @return String logs
     */
    String getLogs(String operationId);

    /**
     * Update Operation type
     * @param operation to update
     * @param rollbackHelmSourceId to rollback
     * @return updated operation
     */
    Operation updateOperationWithRollbackTypeAndProcessingState(Operation operation, String rollbackHelmSourceId);
}
