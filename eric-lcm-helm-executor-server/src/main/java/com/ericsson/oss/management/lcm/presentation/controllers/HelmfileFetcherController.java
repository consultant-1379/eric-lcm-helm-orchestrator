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

import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithURLPutRequestDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.api.HelmfileFetcherApi;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithURLRequestDto;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.WorkloadInstanceRequestCoordinatorService;
import com.ericsson.oss.management.lcm.utils.UrlUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/cnwlcm/v1")
@RequiredArgsConstructor
public class HelmfileFetcherController implements HelmfileFetcherApi {

    private final WorkloadInstanceRequestCoordinatorService coordinatorService;
    private final UrlUtils urlUtils;

    @Override
    public ResponseEntity<WorkloadInstanceDto> helmfileFetcherWorkloadInstancesPost(
            @Valid WorkloadInstanceWithURLRequestDto workloadInstanceWithURLRequestDto,
            @Valid Boolean isUrlToHelmRegistry,
            MultipartFile values,
            MultipartFile clusterConnectionInfo) {
        log.info("log audit: Received a Post request to workload instances (INSTANTIATE). WorkloadInstance name {}, url {}",
                 workloadInstanceWithURLRequestDto.getWorkloadInstanceName(), workloadInstanceWithURLRequestDto.getUrl());
        WorkloadInstanceDto response = coordinatorService.instantiate(workloadInstanceWithURLRequestDto, isUrlToHelmRegistry, values,
                                                                      clusterConnectionInfo);
        String latestOperationId = coordinatorService.getLatestOperationId(response.getWorkloadInstanceId());
        var headers = urlUtils.getHttpHeaders(latestOperationId);
        return new ResponseEntity<>(response, headers, HttpStatus.ACCEPTED);
    }

    @Override
    public ResponseEntity<WorkloadInstanceDto> helmfileFetcherWorkloadInstancesWorkloadInstanceIdPut(
            @PathVariable("workloadInstanceId") String workloadInstanceId,
            @Valid WorkloadInstanceWithURLPutRequestDto workloadInstanceWithURLPutRequestDto,
            @Valid Boolean isUrlToHelmRegistry,
            MultipartFile values, MultipartFile clusterConnectionInfo) {
        log.info("log audit: Received a Put request to workload instances (UPDATE). WorkloadInstanceId {}, url {}",
                 workloadInstanceId, workloadInstanceWithURLPutRequestDto.getUrl());
        WorkloadInstanceDto response = coordinatorService.update(workloadInstanceId, workloadInstanceWithURLPutRequestDto,
                isUrlToHelmRegistry, values, clusterConnectionInfo);
        String latestOperationId = coordinatorService.getLatestOperationId(response.getWorkloadInstanceId());
        var headers = urlUtils.getHttpHeaders(latestOperationId);
        return new ResponseEntity<>(response, headers, HttpStatus.ACCEPTED);
    }
}
