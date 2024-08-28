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

import io.restassured.module.mockmvc.RestAssuredMockMvc;

import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;

import com.ericsson.oss.management.lcm.presentation.controllers.WorkloadInstancesController;
import com.ericsson.oss.management.lcm.presentation.exceptions.ApplicationExceptionHandler;
import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.WorkloadInstanceRequestCoordinatorServiceImpl;
import com.ericsson.oss.management.lcm.presentation.services.version.WorkloadInstanceVersionServiceImpl;
import com.ericsson.oss.management.lcm.presentation.services.workloadinstance.WorkloadInstanceServiceImpl;
import com.ericsson.oss.management.lcm.utils.UrlUtils;

@ActiveProfiles("test")
@SpringBootTest(classes = WorkloadInstancesController.class)
public class GetWorkloadInstanceNegativeBase {

    @Autowired
    private WorkloadInstancesController controller;
    @MockBean
    private WorkloadInstanceVersionServiceImpl workloadInstanceVersionService;
    @MockBean
    private WorkloadInstanceRequestCoordinatorServiceImpl workloadInstanceRequestCoordinatorService;
    @MockBean
    private WorkloadInstanceServiceImpl workloadInstanceService;
    @MockBean
    private UrlUtils urlUtils;

    @BeforeEach
    public void setup() {
        given(workloadInstanceRequestCoordinatorService.getWorkloadInstance("not_found"))
                .willThrow(new ResourceNotFoundException("WorkloadInstance with id not_found not found"));

        StandaloneMockMvcBuilder mvcBuilderWithExceptionHandler = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApplicationExceptionHandler());
        RestAssuredMockMvc.standaloneSetup(mvcBuilderWithExceptionHandler);
    }

}
