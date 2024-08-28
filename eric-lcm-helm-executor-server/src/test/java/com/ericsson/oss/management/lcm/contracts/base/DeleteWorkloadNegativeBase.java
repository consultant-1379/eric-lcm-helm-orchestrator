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

import org.junit.jupiter.api.BeforeEach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.OperationState;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.presentation.exceptions.ApplicationExceptionHandler;
import com.ericsson.oss.management.lcm.presentation.mappers.OperationDtoMapper;
import com.ericsson.oss.management.lcm.presentation.mappers.WorkloadInstanceDtoMapper;
import com.ericsson.oss.management.lcm.presentation.mappers.WorkloadInstanceVersionDtoMapper;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.ClusterConnectionInfoRequestCoordinatorService;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.WorkloadInstanceRequestCoordinatorServiceImpl;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.operations.InstantiateService;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.operations.RollbackService;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.operations.TerminateService;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.operations.UpdateService;
import com.ericsson.oss.management.lcm.presentation.services.deployment.DeploymentService;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.presentation.services.operation.OperationService;
import com.ericsson.oss.management.lcm.presentation.services.version.WorkloadInstanceVersionService;
import com.ericsson.oss.management.lcm.presentation.services.workloadinstance.WorkloadInstanceServiceImpl;
import com.ericsson.oss.management.lcm.repositories.OperationRepository;
import com.ericsson.oss.management.lcm.repositories.WorkloadInstanceRepository;
import com.ericsson.oss.management.lcm.presentation.controllers.WorkloadInstancesController;
import com.ericsson.oss.management.lcm.repositories.WorkloadInstanceVersionRepository;
import com.ericsson.oss.management.lcm.utils.UrlUtils;

@ActiveProfiles("test")
@SpringBootTest(classes = { WorkloadInstancesController.class, WorkloadInstanceServiceImpl.class,
        WorkloadInstanceRequestCoordinatorServiceImpl.class })
public class DeleteWorkloadNegativeBase {

    @Autowired
    private WorkloadInstancesController controller;
    @MockBean
    private WorkloadInstanceRepository repository;
    @MockBean
    private OperationService operationService;
    @MockBean
    private WorkloadInstanceVersionService workloadInstanceVersionService;
    @MockBean
    private UrlUtils urlUtils;
    @MockBean
    private WorkloadInstanceDtoMapper workloadInstanceDtoMapper;
    @MockBean
    private OperationRepository operationRepository;
    @MockBean
    private OperationDtoMapper operationDtoMapper;
    @MockBean
    private WorkloadInstanceVersionRepository workloadInstanceVersionRepository;
    @MockBean
    private WorkloadInstanceVersionDtoMapper workloadInstanceVersionMapper;
    @MockBean
    private ClusterConnectionInfoRequestCoordinatorService clusterConnectionInfoRequestCoordinatorService;
    @MockBean
    private TerminateService terminateService;
    @MockBean
    private RollbackService rollbackService;
    @MockBean
    private InstantiateService instantiateService;
    @MockBean
    private UpdateService updateService;
    @MockBean
    private FileService fileService;
    @MockBean
    private DeploymentService deploymentService;

    private static final String NOT_FOUND_ID = "not_found";
    private static final String NOT_TERMINATED_ID = "not_terminated";
    private static final String OPERATION_ID = "operation_id";

    @BeforeEach
    public void setup() {
        //not terminated scenario
        WorkloadInstance instance = basicWorkloadInstance();
        given(repository.findById(NOT_TERMINATED_ID)).willReturn(Optional.of(instance));
        given(operationService.get(OPERATION_ID)).willReturn(notFinishedTerminateOperation(instance));

        //not found scenario
        given(repository.findById(NOT_FOUND_ID)).willReturn(Optional.empty());

        StandaloneMockMvcBuilder mvcBuilderWithExceptionHandler = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApplicationExceptionHandler());
        RestAssuredMockMvc.standaloneSetup(mvcBuilderWithExceptionHandler);
    }

    private Operation notFinishedTerminateOperation(WorkloadInstance workloadInstance) {
        return Operation.builder()
                .workloadInstance(workloadInstance)
                .type(OperationType.TERMINATE)
                .startTime(LocalDateTime.now())
                .state(OperationState.PROCESSING)
                .id(OPERATION_ID)
                .build();
    }

    private WorkloadInstance basicWorkloadInstance() {
        return WorkloadInstance.builder()
                .workloadInstanceId("fake_id")
                .cluster("cluster")
                .namespace("namespace")
                .additionalParameters("some connection info here")
                .latestOperationId(OPERATION_ID)
                .build();
    }
}
