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

import io.restassured.module.mockmvc.RestAssuredMockMvc;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.management.lcm.presentation.controllers.WorkloadInstancesController;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.WorkloadInstanceRequestCoordinatorServiceImpl;
import com.ericsson.oss.management.lcm.presentation.services.version.WorkloadInstanceVersionServiceImpl;
import com.ericsson.oss.management.lcm.presentation.services.workloadinstance.WorkloadInstanceServiceImpl;
import com.ericsson.oss.management.lcm.utils.UrlUtils;

@ActiveProfiles("test")
@SpringBootTest(classes = WorkloadInstancesController.class)
public class TerminateWorkloadPositiveBase {
    @Autowired
    private WorkloadInstancesController controller;
    @MockBean
    private WorkloadInstanceVersionServiceImpl workloadInstanceVersionService;
    @MockBean
    private WorkloadInstanceRequestCoordinatorServiceImpl service;
    @MockBean
    private WorkloadInstanceServiceImpl workloadInstanceService;
    @MockBean
    private UrlUtils utils;

    private static final String OPERATION_ID = "operation_id";
    private static final String WORKLOAD_INSTANCE_ID = "dummy_id";
    private static final String OPERATION_URL = "http://localhost/cnwlcm/v1/operations/test-operation-id";

    @BeforeEach
    public void setup() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, OPERATION_URL);

        given(service.getLatestOperationId(WORKLOAD_INSTANCE_ID)).willReturn(OPERATION_ID);
        given(utils.getHttpHeaders(OPERATION_ID)).willReturn(headers);

        RestAssuredMockMvc.standaloneSetup(controller);
    }

}
