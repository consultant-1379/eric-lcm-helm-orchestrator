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

import com.ericsson.oss.management.lcm.api.model.GetBackupManagersResponse;
import com.ericsson.oss.management.lcm.api.model.GetHealthResponse;
import com.ericsson.oss.management.lcm.api.model.OngoingAction;
import com.ericsson.oss.management.lcm.model.entity.Availability;
import com.ericsson.oss.management.lcm.presentation.controllers.BackupAndRestoreController;
import com.ericsson.oss.management.lcm.presentation.exceptions.ApplicationExceptionHandler;
import com.ericsson.oss.management.lcm.presentation.routing.BroHttpClient;
import com.ericsson.oss.management.lcm.presentation.services.BackupAndRestoreServiceImpl;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;

import java.util.ArrayList;
import java.util.Collections;

import static com.ericsson.oss.management.lcm.constants.BackupAndRestoreConstants.STATUS_HEALTHY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ActiveProfiles("test")
@SpringBootTest(classes = { BackupAndRestoreController.class, BackupAndRestoreServiceImpl.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class GetAllBackupManagersNegativeBase {

    @Autowired
    private BackupAndRestoreController controller;
    @MockBean
    private BroHttpClient routingClient;

    @BeforeEach
    public void setup() {
        ResponseEntity<GetHealthResponse> healthResponse = getHealthResponseEntity();
        given(routingClient.executeHttpRequest(any(), anyString(), any(), any(), eq(GetHealthResponse.class)))
                .willReturn(healthResponse);

        ResponseEntity<GetBackupManagersResponse> backupManagersResponse = getBackupManagersResponseEntity();
        given(routingClient.executeHttpRequest(any(), anyString(), any(), any(), eq(GetBackupManagersResponse.class)))
                .willReturn(backupManagersResponse);

        StandaloneMockMvcBuilder mvcBuilderWithExceptionHandler = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApplicationExceptionHandler());
        RestAssuredMockMvc.standaloneSetup(mvcBuilderWithExceptionHandler);
    }

    private ResponseEntity<GetHealthResponse> getHealthResponseEntity() {
        GetHealthResponse healthResponse = getHealthResponse();
        return new ResponseEntity<>(healthResponse, HttpStatus.OK);
    }

    private ResponseEntity<GetBackupManagersResponse> getBackupManagersResponseEntity() {
        GetBackupManagersResponse backupManagersResponse = getBackupManagersResponse();
        return new ResponseEntity<>(backupManagersResponse, HttpStatus.OK);
    }

    private GetHealthResponse getHealthResponse() {
        return new GetHealthResponse()
                .availability(Availability.AVAILABLE.toString())
                .status(STATUS_HEALTHY)
                .registeredAgents(new ArrayList<>())
                .ongoingAction(new OngoingAction());
    }

    private GetBackupManagersResponse getBackupManagersResponse() {
        return new GetBackupManagersResponse()
                .backupManagers(Collections.emptyList());
    }

}
