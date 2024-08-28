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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;

import com.ericsson.oss.management.lcm.presentation.controllers.WorkloadInstancesController;
import com.ericsson.oss.management.lcm.presentation.exceptions.ApplicationExceptionHandler;
import com.ericsson.oss.management.lcm.presentation.mappers.WorkloadInstanceDtoMapper;
import com.ericsson.oss.management.lcm.presentation.mappers.WorkloadInstanceVersionDtoMapper;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.WorkloadInstanceRequestCoordinatorService;
import com.ericsson.oss.management.lcm.presentation.services.version.WorkloadInstanceVersionServiceImpl;
import com.ericsson.oss.management.lcm.presentation.services.workloadinstance.WorkloadInstanceService;
import com.ericsson.oss.management.lcm.repositories.WorkloadInstanceVersionRepository;
import com.ericsson.oss.management.lcm.utils.UrlUtils;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("test")
@SpringBootTest(classes = { WorkloadInstancesController.class, WorkloadInstanceVersionServiceImpl.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class GetWorkloadInstanceVersionNegativeBase {

    private static final String WORKLOAD_INSTANCE_ID = "workloadInstanceId";
    private static final String INVALID_WORKLOAD_INSTANCE_ID = "not_found";

    @Autowired
    private WorkloadInstancesController controller;
    @MockBean
    private WorkloadInstanceRequestCoordinatorService workloadInstanceRequestCoordinatorService;
    @MockBean
    private WorkloadInstanceDtoMapper workloadInstanceDtoMapper;
    @MockBean
    private WorkloadInstanceService workloadInstanceService;
    @MockBean
    private UrlUtils urlUtils;
    @MockBean
    private WorkloadInstanceVersionRepository workloadInstanceVersionRepository;
    @MockBean
    private WorkloadInstanceVersionDtoMapper workloadInstanceVersionMapper;

    @BeforeEach
    public void setup() {
        when(workloadInstanceVersionRepository.findByWorkloadInstanceWorkloadInstanceIdAndVersion(eq(WORKLOAD_INSTANCE_ID), anyInt()))
                .thenReturn(Optional.empty());
        when(workloadInstanceVersionRepository.findByWorkloadInstanceWorkloadInstanceIdAndVersion(eq(INVALID_WORKLOAD_INSTANCE_ID), anyInt()))
                .thenReturn(Optional.empty());

        StandaloneMockMvcBuilder mvcBuilderWithExceptionHandler = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApplicationExceptionHandler());
        RestAssuredMockMvc.standaloneSetup(mvcBuilderWithExceptionHandler);
    }
}
