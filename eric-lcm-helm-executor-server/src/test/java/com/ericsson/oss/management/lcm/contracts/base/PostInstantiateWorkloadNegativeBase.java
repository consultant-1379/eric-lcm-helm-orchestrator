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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ericsson.oss.management.lcm.presentation.mappers.WorkloadInstanceDtoMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;

import com.ericsson.oss.management.lcm.presentation.controllers.WorkloadInstancesController;
import com.ericsson.oss.management.lcm.presentation.exceptions.ApplicationExceptionHandler;
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
import com.ericsson.oss.management.lcm.repositories.WorkloadInstanceRepository;
import com.ericsson.oss.management.lcm.utils.UrlUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("test")
@SpringBootTest(classes = { WorkloadInstancesController.class, WorkloadInstanceServiceImpl.class,
    WorkloadInstanceRequestCoordinatorServiceImpl.class, WorkloadInstanceDtoMapperImpl.class })
public class PostInstantiateWorkloadNegativeBase {

    @Autowired
    private WorkloadInstancesController controller;
    @MockBean
    private WorkloadInstanceVersionService workloadInstanceVersionService;
    @MockBean
    private UrlUtils urlUtils;
    @MockBean
    private WorkloadInstanceRepository workloadInstanceRepository;
    @MockBean
    private ObjectMapper mapper;
    @MockBean
    private OperationService operationService;
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

    @BeforeEach
    public void setup() {
        when(workloadInstanceRepository.existsByWorkloadInstanceName(any())).thenReturn(true);

        StandaloneMockMvcBuilder mvcBuilderWithExceptionHandler = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApplicationExceptionHandler());
        RestAssuredMockMvc.standaloneSetup(mvcBuilderWithExceptionHandler);
    }
}
