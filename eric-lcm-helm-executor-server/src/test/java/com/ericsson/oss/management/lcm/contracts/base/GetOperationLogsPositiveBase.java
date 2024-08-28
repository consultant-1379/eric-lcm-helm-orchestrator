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

import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.management.lcm.presentation.controllers.OperationsController;
import com.ericsson.oss.management.lcm.presentation.mappers.OperationDtoMapper;
import com.ericsson.oss.management.lcm.presentation.services.operation.OperationService;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("test")
@SpringBootTest(classes = OperationsController.class)
public class GetOperationLogsPositiveBase {

    private static final String OPERATION_ID = "operation_id";
    private static final String ERROR_MESSAGE = "error";

    @Autowired
    private OperationsController controller;
    @MockBean
    private OperationService operationService;
    @MockBean
    private OperationDtoMapper operationDtoMapper;

    @BeforeEach
    public void setup() {
        given(operationService.getLogs(OPERATION_ID)).willReturn(ERROR_MESSAGE);
        RestAssuredMockMvc.standaloneSetup(controller);
    }
}
