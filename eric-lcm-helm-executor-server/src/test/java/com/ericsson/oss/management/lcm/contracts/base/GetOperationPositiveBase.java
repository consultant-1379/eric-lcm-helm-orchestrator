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

package com.ericsson.oss.management.lcm.contracts.base;

import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;

import com.ericsson.oss.management.lcm.presentation.mappers.OperationDtoMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.OperationState;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.presentation.controllers.OperationsController;
import com.ericsson.oss.management.lcm.presentation.services.operation.OperationServiceImpl;

@ActiveProfiles("test")
@SpringBootTest(classes = { OperationsController.class, OperationDtoMapperImpl.class })
public class GetOperationPositiveBase {

    private static final String OPERATION_ID = "operation_id";
    private static final String WORKLOAD_INSTANCE_ID = "workloadInstanceId";
    private static final OperationState OPERATION_STATE = OperationState.FAILED;
    private static final OperationType OPERATION_TYPE = OperationType.INSTANTIATE;
    private static final String ERROR_MESSAGE = "error";
    private static final LocalDateTime START_TIME = LocalDateTime.now();

    @Autowired
    private OperationsController controller;
    @MockBean
    private OperationServiceImpl operationService;

    @BeforeEach
    public void setup() {
        Operation operation = getOperation();
        given(operationService.get(OPERATION_ID)).willReturn(operation);

        RestAssuredMockMvc.standaloneSetup(controller);
    }

    private Operation getOperation() {
        Operation operation = new Operation();
        operation.setId(OPERATION_ID);
        operation.setWorkloadInstance(getWorkloadInstance());
        operation.setState(OPERATION_STATE);
        operation.setType(OPERATION_TYPE);
        operation.setOutput(ERROR_MESSAGE);
        operation.setStartTime(START_TIME);

        return operation;
    }

    private WorkloadInstance getWorkloadInstance() {
        WorkloadInstance instance = new WorkloadInstance();
        instance.setWorkloadInstanceId(WORKLOAD_INSTANCE_ID);

        return instance;
    }
}
