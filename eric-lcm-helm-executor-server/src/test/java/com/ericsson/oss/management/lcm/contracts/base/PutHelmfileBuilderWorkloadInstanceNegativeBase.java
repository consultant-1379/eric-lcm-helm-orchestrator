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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;

import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithChartsPutRequestDto;
import com.ericsson.oss.management.lcm.presentation.controllers.HelmfileBuilderController;
import com.ericsson.oss.management.lcm.presentation.exceptions.ApplicationExceptionHandler;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidInputException;
import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.WorkloadInstanceRequestCoordinatorServiceImpl;
import com.ericsson.oss.management.lcm.utils.UrlUtils;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("test")
@SpringBootTest(classes = HelmfileBuilderController.class)
public class PutHelmfileBuilderWorkloadInstanceNegativeBase {
    @Autowired
    private HelmfileBuilderController controller;
    @MockBean
    private WorkloadInstanceRequestCoordinatorServiceImpl coordinatorService;
    @MockBean
    private UrlUtils urlUtils;

    @BeforeEach
    public void setup() {
        given(coordinatorService.update(eq("not_existed_id"), any(WorkloadInstanceWithChartsPutRequestDto.class), any(), any()))
                .willThrow(new ResourceNotFoundException("WorkloadInstance with id not_existed_id not found"));
        given(coordinatorService.update(eq("some_valid_id"), any(WorkloadInstanceWithChartsPutRequestDto.class), any(), any()))
                .willThrow(new InvalidInputException("Failed to parse json part of request due to Unrecognized token 'invalid': was expecting"
                                                             + " (JSON String, Number, Array, Object or token 'null', 'true' or 'false')\n at "
                                                             + "[Source: (String)\"invalid value\"; line:"
                                                             + " 1, column: 8]"));

        StandaloneMockMvcBuilder mvcBuilderWithExceptionHandler = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApplicationExceptionHandler());
        RestAssuredMockMvc.standaloneSetup(mvcBuilderWithExceptionHandler);
    }

}
