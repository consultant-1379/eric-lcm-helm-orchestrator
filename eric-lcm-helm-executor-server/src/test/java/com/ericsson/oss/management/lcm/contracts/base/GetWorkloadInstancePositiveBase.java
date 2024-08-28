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

import io.restassured.module.mockmvc.RestAssuredMockMvc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceDto;
import com.ericsson.oss.management.lcm.presentation.controllers.WorkloadInstancesController;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.WorkloadInstanceRequestCoordinatorServiceImpl;
import com.ericsson.oss.management.lcm.presentation.services.workloadinstance.WorkloadInstanceServiceImpl;

@ExtendWith(MockitoExtension.class)
public class GetWorkloadInstancePositiveBase {

    @InjectMocks
    @Autowired
    private WorkloadInstancesController controller;
    @Mock
    private WorkloadInstanceServiceImpl workloadInstanceService;
    @Mock
    private WorkloadInstanceRequestCoordinatorServiceImpl requestCoordinatorService;

    @BeforeEach
    public void setup() {
        given(requestCoordinatorService.getWorkloadInstance(anyString())).willReturn(fillResponse());

        RestAssuredMockMvc.standaloneSetup(controller);
    }

    private WorkloadInstanceDto fillResponse() {
        WorkloadInstanceDto response = new WorkloadInstanceDto();
        response.setWorkloadInstanceId("testId");
        response.setWorkloadInstanceName("testName");
        response.setNamespace("testNamespace");
        response.setCluster("testCluster");
        Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put("testKey", "testValue");
        response.setAdditionalParameters(additionalParameters);
        return response;
    }
}