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

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.api.HelmfileBuilderApi;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithChartsPutRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithChartsRequestDto;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.WorkloadInstanceRequestCoordinatorService;
import com.ericsson.oss.management.lcm.utils.UrlUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/cnwlcm/v1")
@RequiredArgsConstructor
public class HelmfileBuilderController implements HelmfileBuilderApi {

    private final WorkloadInstanceRequestCoordinatorService coordinatorService;
    private final UrlUtils urlUtils;

    @Override
    public ResponseEntity<WorkloadInstanceDto> helmfileBuilderWorkloadInstancesPost(
            @Valid WorkloadInstanceWithChartsRequestDto workloadInstanceWithChartsRequestDto,
            MultipartFile globalValues, MultipartFile clusterConnectionInfo) {
        WorkloadInstanceDto response = coordinatorService.instantiate(workloadInstanceWithChartsRequestDto, globalValues, clusterConnectionInfo);
        String latestOperationId = coordinatorService.getLatestOperationId(response.getWorkloadInstanceId());
        var headers = urlUtils.getHttpHeaders(latestOperationId);
        return new ResponseEntity<>(response, headers, HttpStatus.ACCEPTED);
    }

    @Override
    public ResponseEntity<WorkloadInstanceDto> helmfileBuilderWorkloadInstancesWorkloadInstanceIdPut(
            final String workloadInstanceId,
            @Valid WorkloadInstanceWithChartsPutRequestDto workloadInstanceWithChartsPutRequestDto,
            MultipartFile globalValues, MultipartFile clusterConnectionInfo) {
        WorkloadInstanceDto response =
                coordinatorService.update(workloadInstanceId, workloadInstanceWithChartsPutRequestDto, globalValues, clusterConnectionInfo);
        String latestOperationId = coordinatorService.getLatestOperationId(response.getWorkloadInstanceId());
        var headers = urlUtils.getHttpHeaders(latestOperationId);

        return new ResponseEntity<>(response, headers, HttpStatus.ACCEPTED);
    }
}
