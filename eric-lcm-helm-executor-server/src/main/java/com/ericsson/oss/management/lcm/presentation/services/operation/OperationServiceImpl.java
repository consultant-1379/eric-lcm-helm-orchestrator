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

import static com.ericsson.oss.management.lcm.constants.OperationConstants.INSTANCE_ID;
import static com.ericsson.oss.management.lcm.constants.OperationConstants.SORT_COLUMNS;
import static com.ericsson.oss.management.lcm.utils.pagination.PaginationUtils.buildLinks;
import static com.ericsson.oss.management.lcm.utils.pagination.PaginationUtils.buildPaginationInfo;
import static com.ericsson.oss.management.lcm.utils.pagination.PaginationUtils.createPageable;

import java.time.LocalDateTime;
import java.util.List;

import com.ericsson.oss.management.lcm.api.model.PagedOperationDto;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.OperationState;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import com.ericsson.oss.management.lcm.presentation.mappers.OperationDtoMapper;
import com.ericsson.oss.management.lcm.repositories.OperationRepository;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandResponse;
import com.ericsson.oss.management.lcm.utils.pagination.CustomPageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationServiceImpl implements OperationService {

    private static final String ROLLBACK_ERROR_MESSAGE = "An error occured during %s operation. Please, see details in logs. "
            + "Performing rollback to the previous version";

    private final OperationRepository operationRepository;
    private final OperationDtoMapper operationDtoMapper;

    @Value("${operation.timeout}")
    private int operationTimeout;

    @Override
    public Operation create(Operation operation) {
        return operationRepository.save(operation);
    }

    @Override
    public Operation get(final String operationId) {
        return operationRepository.findById(operationId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Operation with id %s does not exist",  operationId)));
    }

    @Override
    public List<Operation> getAll() {
        return operationRepository.findAll();
    }

    @Override
    public PagedOperationDto getByWorkloadInstanceId(String workloadInstanceId, Integer page, Integer size, List<String> sort) {
        Pageable pageable = createPageable(page, size, sort, SORT_COLUMNS, CustomPageRequest.of(INSTANCE_ID));
        Page<Operation> resultsPage = operationRepository
                .findAllByWorkloadInstanceWorkloadInstanceId(workloadInstanceId, pageable);
        return new PagedOperationDto()
                .page(buildPaginationInfo(resultsPage))
                .links(buildLinks(resultsPage))
                .content(resultsPage.map(operationDtoMapper::toOperationDto).getContent());
    }

    @Override
    public Operation updateWithCommandResponse(Operation operation, CommandResponse commandResponse) {
        if (commandResponse.getExitCode() == 0) {
            operation.setState(OperationState.COMPLETED);
        } else {
            operation.setState(OperationState.FAILED);
            operation.setOutput(commandResponse.getOutput());
        }
        return operationRepository.save(operation);
    }

    @Override
    public String getLogs(String operationId) {
        return get(operationId).getOutput();
    }

    @Override
    public Operation updateOperationWithRollbackTypeAndProcessingState(Operation operation, String rollbackHelmSourceId) {
        operation.setHelmSourceId(rollbackHelmSourceId);
        operation.setTimeout(operationTimeout);
        operation.setOutput(String.format(ROLLBACK_ERROR_MESSAGE, operation.getType()));
        operation.setType(OperationType.ROLLBACK);
        operation.setState(OperationState.PROCESSING);
        operation.setStartTime(LocalDateTime.now());

        return operation;
    }
}
