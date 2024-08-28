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

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;

import com.ericsson.oss.management.lcm.presentation.controllers.ClusterConnectionInfoController;
import com.ericsson.oss.management.lcm.presentation.exceptions.ApplicationExceptionHandler;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidFileException;
import com.ericsson.oss.management.lcm.presentation.services.clusterconnectioninfo.ClusterConnectionInfoService;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.ClusterConnectionInfoRequestCoordinatorService;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("test")
@SpringBootTest(classes = ClusterConnectionInfoController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PostClusterConnectionInfoNegativeBase {
    @Autowired
    private ClusterConnectionInfoController controller;
    @MockBean
    private ClusterConnectionInfoRequestCoordinatorService service;
    @MockBean
    private ClusterConnectionInfoService clusterConnectionInfoService;

    @BeforeEach
    public void setup() {
        Mockito.when(service.create(any(), any())).thenThrow(new InvalidFileException("Yaml file cannot be empty"));

        StandaloneMockMvcBuilder mvcBuilderWithExceptionHandler = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApplicationExceptionHandler());
        RestAssuredMockMvc.standaloneSetup(mvcBuilderWithExceptionHandler);
    }
}
