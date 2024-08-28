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

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.api.model.OperationDto;
import com.ericsson.oss.management.lcm.api.model.PagedOperationDto;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.OperationState;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import com.ericsson.oss.management.lcm.presentation.mappers.OperationDtoMapper;
import com.ericsson.oss.management.lcm.repositories.OperationRepository;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OperationServiceTest extends AbstractDbSetupTest {

    private static final String VALID_ID = "validId";
    private static final String INVALID_ID = "invalidId";
    private static final String ERROR_MESSAGE = "test";

    private static final OperationState OPERATION_STATE = OperationState.FAILED;
    private static final OperationType OPERATION_TYPE = OperationType.INSTANTIATE;
    private static final LocalDateTime START_TIME = LocalDateTime.now();
    private static final Integer PAGE = 2;
    private static final Integer SIZE = 2;
    private static final String SORT = "startTime,asc";
    private static final String WORKLOAD_INSTANCE_ID = "wrkld_instance_id";
    private static final String OPERATION_ID = "some_id";
    private static final String OPERATION_SECOND_ID = "some_id_2";
    private static final String NAMESPACE = "NAMESPACE";

    @Autowired
    private OperationServiceImpl operationService;
    @MockBean
    private OperationRepository operationRepository;
    @MockBean
    private OperationDtoMapper operationDtoMapper;

    @Test
    void shouldSaveWhenCreate() {
        Operation operation = new Operation();

        operationService.create(operation);

        verify(operationRepository).save(operation);
    }

    @Test
    void shouldReturnOperationWhenGetWithValidId() {
        Operation operation = new Operation();
        when(operationRepository.findById(VALID_ID)).thenReturn(Optional.of(operation));

        assertThat(operationService.get(VALID_ID)).isEqualTo(operation);
    }

    @Test
    void shouldReturnOperationLogsWhenGetWithValidId() {
        Operation operation = getOperation(VALID_ID);
        when(operationRepository.findById(VALID_ID)).thenReturn(Optional.of(operation));

        assertThat(operationService.getLogs(VALID_ID)).isEqualTo(ERROR_MESSAGE);
    }

    @Test
    void shouldThrowExceptionWhenGetWithInvalidId() {
        when(operationRepository.findById(INVALID_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> operationService.get(INVALID_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldReturnOperationsWhenGetAll() {
        Operation operation = new Operation();
        List<Operation> operations = new ArrayList<>();
        operations.add(operation);

        when(operationRepository.findAll()).thenReturn(operations);

        assertThat(operationService.getAll()).isEqualTo(operations);
    }

    @Test
    void shouldUpdateOperationWithFailedWhenCodeIsNotZero() {
        CommandResponse commandResponse = new CommandResponse(ERROR_MESSAGE, -1);
        Operation operation = new Operation();

        operationService.updateWithCommandResponse(operation, commandResponse);

        assertThat(operation.getState()).isEqualTo(OperationState.FAILED);
        assertThat(operation.getOutput()).isEqualTo(ERROR_MESSAGE);
        verify(operationRepository).save(operation);
    }

    @Test
    void shouldUpdateOperationIfOperationTypeIsTerminate() {
        CommandResponse commandResponse = new CommandResponse("", 0);
        Operation operation = Operation.builder().type(OperationType.TERMINATE).build();

        operationService.updateWithCommandResponse(operation, commandResponse);

        assertThat(operation.getState()).isEqualTo(OperationState.COMPLETED);
        verify(operationRepository).save(operation);
    }

    @Test
    void shouldUpdateOperationWithCompletedWhenCodeIsZero() {
        CommandResponse commandResponse = new CommandResponse("", 0);
        Operation operation = Operation.builder().build();

        operationService.updateWithCommandResponse(operation, commandResponse);

        assertThat(operation.getState()).isEqualTo(OperationState.COMPLETED);
        verify(operationRepository).save(operation);
    }

    @Test
    void shouldReturnPagedOperationDto() {
        //Init
        List<String> sortList = Arrays.asList(SORT);
        Pageable pageable = PageRequest.of(PAGE, SIZE);
        Page<Operation> resultsPage = new PageImpl<>(getOperationsByPage(), pageable, 9L);

        OperationDto operationDto1 = getOperationDto(OPERATION_ID);
        OperationDto operationDto2 = getOperationDto(OPERATION_SECOND_ID);

        when(operationRepository.findAllByWorkloadInstanceWorkloadInstanceId(anyString(), any(Pageable.class)))
                .thenReturn(resultsPage);
        when(operationDtoMapper.toOperationDto(any(Operation.class))).thenReturn(operationDto1).thenReturn(operationDto2);

        //Test method
        PagedOperationDto result = operationService.getByWorkloadInstanceId(WORKLOAD_INSTANCE_ID, PAGE, SIZE, sortList);

        //Verify
        verify(operationRepository, times(1)).findAllByWorkloadInstanceWorkloadInstanceId(anyString(), any(Pageable.class));
        verify(operationDtoMapper, times(2)).toOperationDto(any());
        assertThat(result.getContent().size()).isEqualTo(SIZE);
    }

    private OperationDto getOperationDto(String id) {
        OperationDto operation = new OperationDto();
        operation.setOperationId(id);
        operation.setWorkloadInstanceId(WORKLOAD_INSTANCE_ID);
        operation.setState(OPERATION_STATE.name());
        operation.setType(OPERATION_TYPE.name());
        operation.setStartTime(START_TIME.toString());
        return operation;
    }

    private List<Operation> getOperationsByPage() {
        List<Operation> pagesList = new ArrayList<>();
        pagesList.add(getOperation(OPERATION_ID));
        pagesList.add(getOperation(OPERATION_SECOND_ID));
        return pagesList;
    }

    private Operation getOperation(String id) {
        return Operation.builder()
                .id(id)
                .workloadInstance(getWorkloadInstance())
                .state(OPERATION_STATE)
                .type(OPERATION_TYPE)
                .output(ERROR_MESSAGE)
                .startTime(START_TIME)
                .build();
    }

    private WorkloadInstance getWorkloadInstance() {
        return WorkloadInstance.builder()
                .workloadInstanceId(WORKLOAD_INSTANCE_ID)
                .namespace(NAMESPACE)
                .build();

    }
}
