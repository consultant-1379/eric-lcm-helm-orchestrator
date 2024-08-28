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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstancePostRequestDto;
import com.ericsson.oss.management.lcm.presentation.controllers.WorkloadInstancesController;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.WorkloadInstanceRequestCoordinatorServiceImpl;
import com.ericsson.oss.management.lcm.repositories.WorkloadInstanceRepository;
import com.ericsson.oss.management.lcm.utils.UrlUtils;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ExtendWith(MockitoExtension.class)
public class PostInstantiateWorkloadPositiveBase {

    @InjectMocks
    WorkloadInstancesController defaultApi;
    @Mock
    private WorkloadInstanceRequestCoordinatorServiceImpl operationCoordinatorService;
    @Mock
    private WorkloadInstanceRepository workloadInstanceRepository;
    @Mock
    private UrlUtils urlUtils;

    @BeforeEach
    public void setup() {
        RestAssuredMockMvc.standaloneSetup(defaultApi);
        final WorkloadInstanceDto workloadInstanceDto = new WorkloadInstanceDto();
        workloadInstanceDto.setWorkloadInstanceId("testId");
        workloadInstanceDto.setWorkloadInstanceName("testName");
        workloadInstanceDto.setNamespace("testNamespace");
        workloadInstanceDto.setCluster("testCluster");
        Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put("testKey", "testValue");
        workloadInstanceDto.setAdditionalParameters(additionalParameters);
        when(operationCoordinatorService.instantiate(any(WorkloadInstancePostRequestDto.class), any(), any(), any())).thenReturn(
                workloadInstanceDto);
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "http://localhost/cnwlcm/v1/operations/test-operation-id");
        when(urlUtils.getHttpHeaders(any())).thenReturn(headers);
    }
}
