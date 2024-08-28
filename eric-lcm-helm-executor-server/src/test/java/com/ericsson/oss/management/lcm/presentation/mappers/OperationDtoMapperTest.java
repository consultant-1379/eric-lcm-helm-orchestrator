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

package com.ericsson.oss.management.lcm.presentation.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ericsson.oss.management.lcm.api.model.OperationDto;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.OperationState;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;

@SpringBootTest(classes = OperationDtoMapperImpl.class)
class OperationDtoMapperTest {

    private static final String OPERATION_ID = "id";
    private static final String WORKLOAD_INSTANCE_ID = "workloadInstanceId";
    private static final OperationState OPERATION_STATE = OperationState.FAILED;
    private static final OperationType OPERATION_TYPE = OperationType.INSTANTIATE;
    private static final LocalDateTime START_TIME = LocalDateTime.now();

    @Autowired
    private OperationDtoMapper operationDtoMapper;

    @Test
    void shouldMapOperationToOperationDto() {
        Operation operation = getOperation();

        OperationDto operationDto = operationDtoMapper.toOperationDto(operation);

        assertThat(operationDto.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(operationDto.getWorkloadInstanceId()).isEqualTo(WORKLOAD_INSTANCE_ID);
        assertThat(operationDto.getState()).isEqualTo(OPERATION_STATE.toString());
        assertThat(operationDto.getType()).isEqualTo(OPERATION_TYPE.toString());
        assertThat(operationDto.getStartTime()).isEqualTo(START_TIME.toString());
    }

    @Test
    void shouldMapOperationDtoToOperation() {
        OperationDto operationDto = getOperationDto();

        Operation operation = operationDtoMapper.toOperation(operationDto);

        assertThat(operation.getId()).isEqualTo(OPERATION_ID);
        assertThat(operation.getWorkloadInstance().getWorkloadInstanceId()).isEqualTo(WORKLOAD_INSTANCE_ID);
        assertThat(operation.getState()).isEqualTo(OPERATION_STATE);
        assertThat(operation.getType()).isEqualTo(OPERATION_TYPE);
        assertThat(operation.getStartTime()).isEqualTo(START_TIME);
        assertThat(operation.getHelmSourceId()).isNull();
        assertThat(operation.getOutput()).isNull();
        assertThat(operation.getTimeout()).isZero();
    }

    private Operation getOperation() {
        Operation operation = new Operation();
        operation.setId(OPERATION_ID);
        operation.setWorkloadInstance(getWorkloadInstance());
        operation.setState(OPERATION_STATE);
        operation.setType(OPERATION_TYPE);
        operation.setStartTime(START_TIME);

        return operation;
    }

    private WorkloadInstance getWorkloadInstance() {
        WorkloadInstance instance = new WorkloadInstance();
        instance.setWorkloadInstanceId(WORKLOAD_INSTANCE_ID);

        return instance;
    }

    private OperationDto getOperationDto() {
        OperationDto operationDto = new OperationDto();
        operationDto.setOperationId(OPERATION_ID);
        operationDto.setWorkloadInstanceId(WORKLOAD_INSTANCE_ID);
        operationDto.setState(OPERATION_STATE.toString());
        operationDto.setType(OPERATION_TYPE.toString());
        operationDto.setStartTime(START_TIME.toString());

        return operationDto;
    }

}
