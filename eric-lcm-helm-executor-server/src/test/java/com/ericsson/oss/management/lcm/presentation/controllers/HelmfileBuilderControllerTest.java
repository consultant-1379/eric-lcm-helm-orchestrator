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

package com.ericsson.oss.management.lcm.presentation.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithChartsPutRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithChartsRequestDto;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.WorkloadInstanceRequestCoordinatorServiceImpl;
import com.ericsson.oss.management.lcm.utils.UrlUtils;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(classes = HelmfileBuilderController.class)
class HelmfileBuilderControllerTest extends AbstractDbSetupTest {

    @Autowired
    private HelmfileBuilderController controller;
    @MockBean
    private WorkloadInstanceRequestCoordinatorServiceImpl coordinatorService;
    @MockBean
    private UrlUtils urlUtils;
    @MockBean
    private WorkloadInstanceWithChartsRequestDto requestDto;

    private static final String WORKLOAD_INSTANCE_ID = "ef1ce-4cf4-477c-aab3-21c454e6a379";
    private static final String OPERATION_ID = "tydsf-98y6-477c-aab3-fas85ljoa9sd";

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.standaloneSetup(controller);
    }

    @Test
    void shouldReturnAcceptedWhenInstantiate() {
        WorkloadInstanceDto workloadInstanceDto = new WorkloadInstanceDto();
        workloadInstanceDto.setWorkloadInstanceId(WORKLOAD_INSTANCE_ID);
        when(coordinatorService.instantiate(any(WorkloadInstanceWithChartsRequestDto.class), any(), any())).thenReturn(
                workloadInstanceDto);
        when(coordinatorService.getLatestOperationId(anyString())).thenReturn(OPERATION_ID);
        when(urlUtils.getHttpHeaders(any())).thenReturn(new HttpHeaders());

        HttpStatusCode result = controller
                .helmfileBuilderWorkloadInstancesPost(new WorkloadInstanceWithChartsRequestDto(), null, null)
                .getStatusCode();

        assertThat(result).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    void shouldReturnAcceptedWhenUpdate() {
        WorkloadInstanceDto workloadInstanceDto = new WorkloadInstanceDto();
        workloadInstanceDto.setWorkloadInstanceId(WORKLOAD_INSTANCE_ID);

        when(coordinatorService.update(anyString(), any(WorkloadInstanceWithChartsPutRequestDto.class), any(), any())).
                thenReturn(workloadInstanceDto);
        when(coordinatorService.getLatestOperationId(anyString())).thenReturn(OPERATION_ID);
        when(urlUtils.getHttpHeaders(any())).thenReturn(new HttpHeaders());

        ResponseEntity<WorkloadInstanceDto> result = controller
                .helmfileBuilderWorkloadInstancesWorkloadInstanceIdPut(WORKLOAD_INSTANCE_ID, new WorkloadInstanceWithChartsPutRequestDto(),
                                                        null, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

}