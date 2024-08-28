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

import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceDto;
import com.ericsson.oss.management.lcm.presentation.controllers.WorkloadInstancesController;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.WorkloadInstanceRequestCoordinatorServiceImpl;
import com.ericsson.oss.management.lcm.presentation.services.version.WorkloadInstanceVersionServiceImpl;
import com.ericsson.oss.management.lcm.presentation.services.workloadinstance.WorkloadInstanceServiceImpl;
import com.ericsson.oss.management.lcm.utils.UrlUtils;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ActiveProfiles("test")
@SpringBootTest(classes = WorkloadInstancesController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PutRollbackWorkloadPositiveBase {

    private static final String WORKLOAD_INSTANCE_ID = "testId";
    private static final String WORKLOAD_INSTANCE_NAME = "testName";
    private static final String NAMESPACE = "testNamespace";
    private static final String CLUSTER = "testCluster";

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
        WorkloadInstanceDto workloadInstanceDto = getWorkloadInstanceDto();
        Map<String, Object> additionalParameters = Collections.singletonMap("testKey", "testValue");
        workloadInstanceDto.setAdditionalParameters(additionalParameters);
        given(workloadInstanceRequestCoordinatorService.rollback(anyString(), any(), any())).willReturn(workloadInstanceDto);
        given(workloadInstanceRequestCoordinatorService.getLatestOperationId(anyString())).willReturn("test-operation-id");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "http://localhost/cnwlcm/v1/operations/test-operation-id");
        given(urlUtils.getHttpHeaders(any())).willReturn(headers);

        RestAssuredMockMvc.standaloneSetup(controller);
    }

    private WorkloadInstanceDto getWorkloadInstanceDto() {
        WorkloadInstanceDto workloadInstanceDto = new WorkloadInstanceDto();
        workloadInstanceDto.setWorkloadInstanceId(WORKLOAD_INSTANCE_ID);
        workloadInstanceDto.setWorkloadInstanceName(WORKLOAD_INSTANCE_NAME);
        workloadInstanceDto.setNamespace(NAMESPACE);
        workloadInstanceDto.setCluster(CLUSTER);

        return workloadInstanceDto;
    }

}
