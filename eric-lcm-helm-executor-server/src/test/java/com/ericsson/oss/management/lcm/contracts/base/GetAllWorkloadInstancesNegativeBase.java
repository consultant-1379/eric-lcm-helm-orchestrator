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

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;

import com.ericsson.oss.management.lcm.presentation.controllers.WorkloadInstancesController;
import com.ericsson.oss.management.lcm.presentation.exceptions.ApplicationExceptionHandler;
import com.ericsson.oss.management.lcm.presentation.mappers.WorkloadInstanceDtoMapper;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.WorkloadInstanceRequestCoordinatorService;
import com.ericsson.oss.management.lcm.presentation.services.version.WorkloadInstanceVersionService;
import com.ericsson.oss.management.lcm.presentation.services.workloadinstance.WorkloadInstanceServiceImpl;
import com.ericsson.oss.management.lcm.repositories.WorkloadInstanceRepository;
import com.ericsson.oss.management.lcm.utils.UrlUtils;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("test")
@SpringBootTest(classes = { WorkloadInstancesController.class, WorkloadInstanceServiceImpl.class })
public class GetAllWorkloadInstancesNegativeBase {

    @Autowired
    private WorkloadInstancesController controller;
    @MockBean
    private WorkloadInstanceRepository workloadInstanceRepository;
    @MockBean
    private WorkloadInstanceVersionService workloadInstanceVersionService;
    @MockBean
    private WorkloadInstanceRequestCoordinatorService workloadInstanceRequestCoordinatorService;
    @MockBean
    private UrlUtils urlUtils;
    @MockBean
    private WorkloadInstanceDtoMapper workloadInstanceDtoMapper;


    @BeforeEach
    public void setup() {
        StandaloneMockMvcBuilder mvcBuilderWithExceptionHandler = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApplicationExceptionHandler());
        RestAssuredMockMvc.standaloneSetup(mvcBuilderWithExceptionHandler);
    }
}
