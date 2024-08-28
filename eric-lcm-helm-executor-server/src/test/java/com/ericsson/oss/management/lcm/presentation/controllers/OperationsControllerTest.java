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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;

import com.ericsson.oss.management.lcm.api.model.OperationDto;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.presentation.mappers.OperationDtoMapper;
import com.ericsson.oss.management.lcm.presentation.services.operation.OperationService;
import org.springframework.http.HttpStatusCode;

@SpringBootTest(classes = OperationsController.class)
class OperationsControllerTest {

    @Autowired
    private OperationsController controller;

    @MockBean
    private OperationService operationService;

    @MockBean
    private OperationDtoMapper operationDtoMapper;

    private static final String VALID_ID = "existing_id";

    @Test
    void shouldReturnResponseWhenGetOperation() {
        Operation operation = new Operation();
        OperationDto operationDto = new OperationDto();
        when(operationService.get(anyString())).thenReturn(operation);
        when(operationDtoMapper.toOperationDto(operation)).thenReturn(operationDto);

        HttpStatusCode result = controller.operationsOperationIdGet(VALID_ID)
                .getStatusCode();

        assertThat(result).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturnResponseWhenGetAllOperations() {
        HttpStatusCode result = controller.operationsWorkloadInstancesWorkloadInstanceIdGet(VALID_ID,
                1, 20, Collections.singletonList("startTime,asc"))
                .getStatusCode();

        assertThat(result).isEqualTo(HttpStatus.OK);
    }

}