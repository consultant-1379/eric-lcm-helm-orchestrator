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
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.HelmfileExecutorApplication;
import com.ericsson.oss.management.lcm.presentation.controllers.HelmfileBuilderController;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.ClusterConnectionInfoRequestCoordinatorService;
import com.ericsson.oss.management.lcm.repositories.WorkloadInstanceRepository;
import com.ericsson.oss.management.lcm.utils.HttpClientUtils;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("test")
@SpringBootTest(classes = HelmfileExecutorApplication.class)
public class PostHelmfileFetcherWorkloadInstanceNegativeBase extends AbstractDbSetupTest {
    @InjectMocks
    private HelmfileBuilderController defaultApi;

    @Autowired
    private WebApplicationContext applicationContext;

    @MockBean
    private WorkloadInstanceRepository workloadInstanceRepository;

    @MockBean
    private HttpClientUtils httpClientUtils;

    @MockBean
    private ClusterConnectionInfoRequestCoordinatorService clusterConnectionInfoRequestCoordinatorService;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.webAppContextSetup(applicationContext);
        when(workloadInstanceRepository.existsByWorkloadInstanceName("not-unique-name")).thenReturn(true);
        when(workloadInstanceRepository.existsByWorkloadInstanceName("invalid-url")).thenReturn(false);
        when(httpClientUtils.executeHttpRequest(any(), eq("https://localhost:8080/invalid-url"),
                                                eq(HttpMethod.GET), any(), any())).thenThrow(HttpClientErrorException.class);
    }
}
