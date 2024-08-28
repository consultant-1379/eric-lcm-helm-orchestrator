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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithChartsPutRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceDto;
import com.ericsson.oss.management.lcm.presentation.controllers.HelmfileBuilderController;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.WorkloadInstanceRequestCoordinatorServiceImpl;
import com.ericsson.oss.management.lcm.utils.UrlUtils;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ExtendWith(MockitoExtension.class)
public class PutHelmfileBuilderWorkloadInstancePositiveBase {
    @InjectMocks
    HelmfileBuilderController defaultApi;
    @Mock
    private WorkloadInstanceRequestCoordinatorServiceImpl operationCoordinatorService;
    @Mock
    private UrlUtils urlUtils;

    private static final String OPERATION_ID = "test-operation-id";

    @BeforeEach
    public void setup() {
        RestAssuredMockMvc.standaloneSetup(defaultApi);
        final WorkloadInstanceDto workloadInstanceDto = new WorkloadInstanceDto();
        workloadInstanceDto.setWorkloadInstanceId("testId");
        workloadInstanceDto.setWorkloadInstanceName("testName");
        workloadInstanceDto.setNamespace("testNamespace");
        workloadInstanceDto.setCluster("testCluster");
        when(operationCoordinatorService.update(anyString(), any(WorkloadInstanceWithChartsPutRequestDto.class), any(), any())).
                thenReturn(workloadInstanceDto);
        when(operationCoordinatorService.getLatestOperationId(anyString())).thenReturn(OPERATION_ID);
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "http://localhost/cnwlcm/v1/operations/test-operation-id");
        when(urlUtils.getHttpHeaders(any())).thenReturn(headers);
    }

}
