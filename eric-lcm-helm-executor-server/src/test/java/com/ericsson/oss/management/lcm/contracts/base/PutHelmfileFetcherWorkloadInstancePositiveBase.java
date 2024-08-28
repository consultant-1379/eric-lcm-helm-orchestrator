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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithURLPutRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceDto;
import com.ericsson.oss.management.lcm.presentation.controllers.HelmfileFetcherController;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.WorkloadInstanceRequestCoordinatorServiceImpl;
import com.ericsson.oss.management.lcm.utils.UrlUtils;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("test")
@SpringBootTest(classes = HelmfileFetcherController.class)
public class PutHelmfileFetcherWorkloadInstancePositiveBase extends AbstractDbSetupTest {

    @Autowired
    private HelmfileFetcherController defaultApi;
    @MockBean
    private WorkloadInstanceRequestCoordinatorServiceImpl operationCoordinatorService;
    @MockBean
    private UrlUtils urlUtils;

    @BeforeEach
    void setup() {
        when(operationCoordinatorService.update(
                anyString(),
                any(WorkloadInstanceWithURLPutRequestDto.class),
                anyBoolean(),
                any(), any()))
                .thenReturn(getWorkloadInstanceDto());
        HttpHeaders headers = getHttpHeaders();
        when(urlUtils.getHttpHeaders(any())).thenReturn(headers);
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "http://localhost/cnwlcm/v1/operations/test-operation-id");
        return headers;
    }

    private WorkloadInstanceDto getWorkloadInstanceDto() {
        RestAssuredMockMvc.standaloneSetup(defaultApi);
        WorkloadInstanceDto workloadInstanceDto = new WorkloadInstanceDto();
        workloadInstanceDto.setWorkloadInstanceId("testId");
        workloadInstanceDto.setWorkloadInstanceName("testName");
        workloadInstanceDto.setNamespace("testNamespace");
        workloadInstanceDto.setCluster("testCluster");
        Map<String, Object> additionalParameters = Collections.singletonMap("testKey", "testValue");
        workloadInstanceDto.setAdditionalParameters(additionalParameters);
        return workloadInstanceDto;
    }
}
