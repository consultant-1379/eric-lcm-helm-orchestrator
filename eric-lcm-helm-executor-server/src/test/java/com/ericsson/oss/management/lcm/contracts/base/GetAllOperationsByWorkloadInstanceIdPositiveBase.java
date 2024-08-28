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

import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.OperationState;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.presentation.controllers.OperationsController;
import com.ericsson.oss.management.lcm.presentation.mappers.OperationDtoMapper;
import com.ericsson.oss.management.lcm.presentation.services.operation.OperationServiceImpl;
import com.ericsson.oss.management.lcm.repositories.OperationRepository;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ActiveProfiles("test")
@SpringBootTest(classes = { OperationsController.class, OperationServiceImpl.class })
public class GetAllOperationsByWorkloadInstanceIdPositiveBase {

    private static final String WORKLOAD_INSTANCE_ID = "workloadInstanceId";
    private static final OperationState OPERATION_STATE = OperationState.FAILED;
    private static final OperationType OPERATION_TYPE = OperationType.INSTANTIATE;
    private static final String ERROR_MESSAGE = "error";
    private static final LocalDateTime START_TIME = LocalDateTime.now();

    @Autowired
    private OperationsController controller;
    @MockBean
    private OperationRepository operationRepository;
    @MockBean
    private OperationDtoMapper operationDtoMapper;

    @BeforeEach
    public void setup() {
        Pageable pageable = PageRequest.of(1, 2);
        Page<Operation> page = new PageImpl<>(getOperationsList(), pageable, 9L);

        given(operationRepository.findAllByWorkloadInstanceWorkloadInstanceId(anyString(),
                any(Pageable.class))).willReturn(page);

        RestAssuredMockMvc.standaloneSetup(controller);
    }

    private List<Operation> getOperationsList() {
        return List.of(getOperation("firstId"), getOperation("secondId"));
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
                .build();
    }
}