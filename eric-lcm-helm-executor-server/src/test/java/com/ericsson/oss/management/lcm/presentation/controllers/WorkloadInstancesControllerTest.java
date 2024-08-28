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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.api.model.PagedWorkloadInstanceDto;
import com.ericsson.oss.management.lcm.api.model.PagedWorkloadInstanceVersionDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceOperationPostRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstancePostRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstancePutRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceVersionDto;
import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.WorkloadInstanceRequestCoordinatorServiceImpl;
import com.ericsson.oss.management.lcm.presentation.services.version.WorkloadInstanceVersionService;
import com.ericsson.oss.management.lcm.presentation.services.workloadinstance.WorkloadInstanceService;
import com.ericsson.oss.management.lcm.utils.UrlUtils;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(classes = WorkloadInstancesController.class)
class WorkloadInstancesControllerTest {

    @Autowired
    private WorkloadInstancesController controller;
    @MockBean
    private WorkloadInstanceRequestCoordinatorServiceImpl coordinatorService;
    @MockBean
    private WorkloadInstanceService workloadInstanceService;
    @MockBean
    private WorkloadInstanceVersionService workloadInstanceVersionService;
    @MockBean
    private UrlUtils urlUtils;
    @Mock
    private MultipartFile helmSource;
    @Mock
    private WorkloadInstanceDto workloadInstanceDto;
    @Mock
    private WorkloadInstanceVersionDto workloadInstanceVersionDto;

    private static final String WORKLOAD_INSTANCE_ID = "ef1ce-4cf4-477c-aab3-21c454e6a379";
    private static final String OPERATION_ID = "tydsf-98y6-477c-aab3-fas85ljoa9sd";
    private static final String TERMINATE = "terminate";

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.standaloneSetup(controller);
    }

    @Test
    void shouldReturnAcceptedWhenInstantiate() {
        //Init
        final WorkloadInstanceDto workloadInstanceDto = new WorkloadInstanceDto();
        workloadInstanceDto.setWorkloadInstanceId(WORKLOAD_INSTANCE_ID);
        when(coordinatorService.instantiate(any(WorkloadInstancePostRequestDto.class), any(), any(), any())).thenReturn(
                workloadInstanceDto);
        when(urlUtils.getHttpHeaders(any())).thenReturn(new HttpHeaders());

        //Test method
        HttpStatusCode result = controller
                .workloadInstancesPost(new WorkloadInstancePostRequestDto(), helmSource, null, null)
                .getStatusCode();

        //Verify
        assertThat(result).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    void shouldReturnOKWhenGetAllWorkloadInstances() {
        ResponseEntity<PagedWorkloadInstanceDto> result =
                controller.workloadInstancesGet(1, 20, Collections.singletonList("workloadInstanceId,asc"));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturnOKWhenGetByWorkloadInstanceId() {
        //Init
        String workloadInstanceId = "id";
        when(coordinatorService.getWorkloadInstance(anyString())).thenReturn(workloadInstanceDto);

        //Test method
        ResponseEntity<WorkloadInstanceDto> result = controller.workloadInstancesWorkloadInstanceIdGet(workloadInstanceId);

        //Verify
        verify(coordinatorService, times(1)).getWorkloadInstance(workloadInstanceId);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldThrowResourceNotFoundException() {
        //Init
        String workloadInstanceId = "notfound_id";
        when(coordinatorService.getWorkloadInstance(anyString()))
                .thenThrow(new ResourceNotFoundException(String.format("WorkloadInstance with id %s not found",
                           workloadInstanceId)));

        //Test method
        assertThatThrownBy(() -> {
            controller.workloadInstancesWorkloadInstanceIdGet(workloadInstanceId);
        }).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.format("WorkloadInstance with id %s not found",
                        workloadInstanceId));

        //Verify
        verify(coordinatorService, times(1)).getWorkloadInstance(workloadInstanceId);
    }

    @Test
    void shouldReturnNoContentWhenDelete() {
        HttpStatusCode result = controller
                .workloadInstancesWorkloadInstanceIdDelete(WORKLOAD_INSTANCE_ID).getStatusCode();

        assertThat(result).isEqualTo(HttpStatus.NO_CONTENT);
        verify(coordinatorService).deleteWorkloadInstance(WORKLOAD_INSTANCE_ID);
    }

    @Test
    void shouldReturnAcceptedWhenTerminateWithCorrectType() {
        WorkloadInstanceOperationPostRequestDto operationPostRequestDto = new WorkloadInstanceOperationPostRequestDto();
        operationPostRequestDto.setType(TERMINATE);
        when(coordinatorService.getLatestOperationId(WORKLOAD_INSTANCE_ID)).thenReturn(OPERATION_ID);
        when(urlUtils.getHttpHeaders(OPERATION_ID)).thenReturn(new HttpHeaders());

        HttpStatusCode result = controller
                .workloadInstancesWorkloadInstanceIdOperationsPost(WORKLOAD_INSTANCE_ID,
                                                                   operationPostRequestDto, null)
                .getStatusCode();

        assertThat(result)
                .isNotNull()
                .isEqualTo(HttpStatus.ACCEPTED);
        verify(coordinatorService).terminateWorkloadInstance(WORKLOAD_INSTANCE_ID,
                                                             operationPostRequestDto, null);
    }

    @Test
    void shouldReturnAcceptedWhenUpdate() {
        WorkloadInstanceDto workloadInstanceDto = new WorkloadInstanceDto();
        workloadInstanceDto.setWorkloadInstanceId(WORKLOAD_INSTANCE_ID);

        when(coordinatorService.update(anyString(), any(WorkloadInstancePutRequestDto.class), any(), any(), any())).thenReturn(
                workloadInstanceDto);
        when(urlUtils.getHttpHeaders(any())).thenReturn(new HttpHeaders());

        ResponseEntity<WorkloadInstanceDto> result = controller
                .workloadInstancesWorkloadInstanceIdPut(WORKLOAD_INSTANCE_ID, new WorkloadInstancePutRequestDto(), helmSource,
                                                        null, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    void shouldReturnOKWhenGetAllWorkloadInstanceVersions() {
        ResponseEntity<PagedWorkloadInstanceVersionDto> result =
                controller.workloadInstancesWorkloadInstanceIdVersionsGet(WORKLOAD_INSTANCE_ID, 1, 20,
                                                                          Collections.singletonList("workloadInstanceId,asc"));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturnOKWhenGetVersionByWorkloadInstanceIdAndVersion() {
        //Init
        String workloadInstanceId = "id";
        when(workloadInstanceVersionService.getVersionDtoByWorkloadInstanceIdAndVersion(any(), any()))
                .thenReturn(workloadInstanceVersionDto);

        //Test method
        ResponseEntity<WorkloadInstanceVersionDto> result =
                controller.workloadInstancesWorkloadInstanceIdVersionsVersionGet(workloadInstanceId, 1);

        //Verify
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

}