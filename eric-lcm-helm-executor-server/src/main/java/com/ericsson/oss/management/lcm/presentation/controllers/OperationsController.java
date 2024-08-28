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

import com.ericsson.oss.management.lcm.api.OperationsApi;
import com.ericsson.oss.management.lcm.api.model.OperationDto;
import com.ericsson.oss.management.lcm.api.model.PagedOperationDto;
import com.ericsson.oss.management.lcm.presentation.mappers.OperationDtoMapper;
import com.ericsson.oss.management.lcm.presentation.services.operation.OperationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/cnwlcm/v1")
@RequiredArgsConstructor
public class OperationsController implements OperationsApi {

    private final OperationService operationService;
    private final OperationDtoMapper operationDtoMapper;

    @Override
    public ResponseEntity<OperationDto> operationsOperationIdGet(@PathVariable("operationId") String operationId) {
        log.info("Received a Get request to get operation by id");
        var operation = operationService.get(operationId);
        OperationDto operationDto = operationDtoMapper.toOperationDto(operation);

        return new ResponseEntity<>(operationDto, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<PagedOperationDto> operationsWorkloadInstancesWorkloadInstanceIdGet(
            @PathVariable("workloadInstanceId") String workloadInstanceId,
            @Valid @RequestParam(required = false) final Integer page,
            @Valid @RequestParam(required = false) final Integer size,
            @Valid @RequestParam(required = false) final List<String> sort) {
        log.info("Received a Get request to get all paged operations by workload instance id " + workloadInstanceId);
        PagedOperationDto response = operationService.getByWorkloadInstanceId(workloadInstanceId, page, size, sort);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> operationsOperationIdLogsGet(@PathVariable("operationId") String operationId) {
        log.info("Received a Get request to get operation logs by id");

        return new ResponseEntity<>(operationService.getLogs(operationId), HttpStatus.OK);
    }
}
