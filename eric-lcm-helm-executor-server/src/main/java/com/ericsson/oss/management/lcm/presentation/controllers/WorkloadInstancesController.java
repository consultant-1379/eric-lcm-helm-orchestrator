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

import java.util.List;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.api.WorkloadInstancesApi;
import com.ericsson.oss.management.lcm.api.model.PagedWorkloadInstanceDto;
import com.ericsson.oss.management.lcm.api.model.PagedWorkloadInstanceVersionDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceOperationPostRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceOperationPutRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstancePostRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstancePutRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceVersionDto;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.WorkloadInstanceRequestCoordinatorService;
import com.ericsson.oss.management.lcm.presentation.services.version.WorkloadInstanceVersionService;
import com.ericsson.oss.management.lcm.presentation.services.workloadinstance.WorkloadInstanceService;
import com.ericsson.oss.management.lcm.utils.UrlUtils;

import io.micrometer.core.instrument.Metrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/cnwlcm/v1")
@RequiredArgsConstructor
public class WorkloadInstancesController implements WorkloadInstancesApi {

    private static final String OPERATION = "operation";

    private final WorkloadInstanceRequestCoordinatorService workloadInstanceRequestCoordinatorService;
    private final WorkloadInstanceService workloadInstanceService;
    private final WorkloadInstanceVersionService workloadInstanceVersionService;
    private final UrlUtils urlUtils;

    @Override
    public ResponseEntity<WorkloadInstanceDto> workloadInstancesPost(
            @Valid WorkloadInstancePostRequestDto workloadInstancePostRequestDto,
            MultipartFile helmSource,
            MultipartFile values,
            MultipartFile clusterConnectionInfo) {
        log.info("log audit: Received a Post request to workload instances (INSTANTIATE). WorkloadInstance name {}",
                workloadInstancePostRequestDto.getWorkloadInstanceName());
        WorkloadInstanceDto response =
                workloadInstanceRequestCoordinatorService.instantiate(workloadInstancePostRequestDto, helmSource,
                                                                      values, clusterConnectionInfo);
        Metrics.counter("hfe.instantiation.requests.total", OPERATION, "instantiate").increment();
        String latestOperationId = workloadInstanceRequestCoordinatorService.getLatestOperationId(response.getWorkloadInstanceId());
        var headers = urlUtils.getHttpHeaders(latestOperationId);
        return new ResponseEntity<>(response, headers, HttpStatus.ACCEPTED);
    }

    @Override
    public ResponseEntity<PagedWorkloadInstanceDto> workloadInstancesGet(@Valid @RequestParam(required = false) final Integer page,
                                                                         @Valid @RequestParam(required = false) final Integer size,
                                                                         @Valid @RequestParam(required = false) final List<String> sort) {
        log.info("Received a Get request to get all paged operations");
        PagedWorkloadInstanceDto response = workloadInstanceService.getAllWorkloadInstances(page, size, sort);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> workloadInstancesWorkloadInstanceIdDelete(
            @PathVariable("workloadInstanceId") String workloadInstanceId) {
        log.info("log audit: Received a Delete request to workload instances (DELETE). WorkloadInstanceId {}", workloadInstanceId);
        workloadInstanceRequestCoordinatorService.deleteWorkloadInstance(workloadInstanceId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<WorkloadInstanceDto> workloadInstancesWorkloadInstanceIdGet(
            @PathVariable("workloadInstanceId") String workloadInstanceId) {
        WorkloadInstanceDto response = workloadInstanceRequestCoordinatorService.getWorkloadInstance(workloadInstanceId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> workloadInstancesWorkloadInstanceIdOperationsPost(
            @PathVariable("workloadInstanceId") String workloadInstanceId,
            @Valid WorkloadInstanceOperationPostRequestDto workloadInstanceOperationPostRequestDto,
            MultipartFile clusterConnectionInfo) {
        log.info("log audit: Received a Post request to workload instances operations (TERMINATE). WorkloadInstanceId {}", workloadInstanceId);
        Metrics.counter("hfe.termination.requests.total", OPERATION, "terminate").increment();
        workloadInstanceRequestCoordinatorService.terminateWorkloadInstance(workloadInstanceId,
                                                                            workloadInstanceOperationPostRequestDto,
                                                                            clusterConnectionInfo);
        String latestOperationId = workloadInstanceRequestCoordinatorService.getLatestOperationId(workloadInstanceId);
        var headers = urlUtils.getHttpHeaders(latestOperationId);
        return new ResponseEntity<>(headers, HttpStatus.ACCEPTED);
    }

    @Override
    public ResponseEntity<WorkloadInstanceDto> workloadInstancesWorkloadInstanceIdOperationsPut(
            @PathVariable("workloadInstanceId") String workloadInstanceId,
            @Valid WorkloadInstanceOperationPutRequestDto workloadInstanceOperationPutRequestDto,
            MultipartFile clusterConnectionInfo) {
        log.info("log audit: Received a Put request to workload instances operations (ROLLBACK). WorkloadInstanceId {}", workloadInstanceId);

        WorkloadInstanceDto response =
                workloadInstanceRequestCoordinatorService.rollback(workloadInstanceId, workloadInstanceOperationPutRequestDto, clusterConnectionInfo);
        String latestOperationId = workloadInstanceRequestCoordinatorService.getLatestOperationId(response.getWorkloadInstanceId());
        var headers = urlUtils.getHttpHeaders(latestOperationId);

        return new ResponseEntity<>(response, headers, HttpStatus.ACCEPTED);
    }

    @Override
    public ResponseEntity<WorkloadInstanceDto> workloadInstancesWorkloadInstanceIdPut(
            @PathVariable("workloadInstanceId") String workloadInstanceId,
            @Valid WorkloadInstancePutRequestDto workloadInstancePutRequestDto,
            MultipartFile helmSource,
            MultipartFile values,
            MultipartFile clusterConnectionInfo) {
        log.info("log audit: Received a Put request to workload instances (UPDATE). WorkloadInstanceId {}", workloadInstanceId);
        Metrics.counter("hfe.upgrade.requests.total", OPERATION, "update").increment();
        WorkloadInstanceDto response =
                workloadInstanceRequestCoordinatorService.update(workloadInstanceId, workloadInstancePutRequestDto,
                                                                 helmSource, values,
                                                                 clusterConnectionInfo);
        String latestOperationId = workloadInstanceRequestCoordinatorService.getLatestOperationId(response.getWorkloadInstanceId());
        var headers = urlUtils.getHttpHeaders(latestOperationId);

        return new ResponseEntity<>(response, headers, HttpStatus.ACCEPTED);
    }

    @Override
    public ResponseEntity<PagedWorkloadInstanceVersionDto> workloadInstancesWorkloadInstanceIdVersionsGet(
            @PathVariable("workloadInstanceId") String workloadInstanceId,
            @Valid @RequestParam(required = false) Integer page,
            @Valid @RequestParam(required = false) Integer size,
            @Valid @RequestParam(required = false) List<String> sort) {
        log.info("Received a Get request to get all versions of workloadInstance with id = {}", workloadInstanceId);
        PagedWorkloadInstanceVersionDto response =
                workloadInstanceVersionService.getAllVersionsByWorkloadInstance(workloadInstanceId, page, size, sort);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<WorkloadInstanceVersionDto> workloadInstancesWorkloadInstanceIdVersionsVersionGet(
            @PathVariable("workloadInstanceId") String workloadInstanceId,
            @PathVariable("version") Integer version) {
        log.info("Received a Get request to get version = {} of workloadInstance with id = {}",
                               version, workloadInstanceId);
        WorkloadInstanceVersionDto response =
                workloadInstanceVersionService.getVersionDtoByWorkloadInstanceIdAndVersion(workloadInstanceId, version);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
