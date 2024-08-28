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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

import com.ericsson.oss.management.lcm.presentation.services.coordinator.WorkloadInstanceRequestCoordinatorService;
import com.ericsson.oss.management.lcm.presentation.controllers.WorkloadInstancesController;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class DeleteWorkloadPositiveBase {

    @InjectMocks
    WorkloadInstancesController controller;
    @Mock
    WorkloadInstanceRequestCoordinatorService service;

    @BeforeEach
    public void setup() {
        RestAssuredMockMvc.standaloneSetup(controller);
    }
}
