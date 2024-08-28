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

import com.ericsson.oss.management.lcm.api.model.BackupDto;
import com.ericsson.oss.management.lcm.api.model.GetBackupsResponse;
import com.ericsson.oss.management.lcm.api.model.GetHealthResponse;
import com.ericsson.oss.management.lcm.api.model.OngoingAction;
import com.ericsson.oss.management.lcm.model.entity.Availability;
import com.ericsson.oss.management.lcm.presentation.controllers.BackupAndRestoreController;
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

import java.util.ArrayList;
import java.util.List;

import static com.ericsson.oss.management.lcm.api.model.BackupDto.StatusEnum.COMPLETE;
import static com.ericsson.oss.management.lcm.constants.BackupAndRestoreConstants.STATUS_HEALTHY;
import static com.ericsson.oss.management.lcm.model.entity.Availability.AVAILABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ActiveProfiles("test")
@SpringBootTest(classes = { BackupAndRestoreController.class, BackupAndRestoreServiceImpl.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class GetAllBackupsByBackupManagerIdPositiveBase {

    private static final String CREATION_TIME = "2022-03-17T11:07:52.592902Z";

    @Autowired
    private BackupAndRestoreController controller;
    @MockBean
    private BroHttpClient routingClient;

    @BeforeEach
    public void setup() {
        ResponseEntity<GetHealthResponse> healthResponse = getHealthResponseEntity(HttpStatus.OK, AVAILABLE);
        given(routingClient.executeHttpRequest(any(), anyString(), any(), any(), eq(GetHealthResponse.class)))
                .willReturn(healthResponse);

        ResponseEntity<GetBackupsResponse> response = getBackupsResponse();
        given(routingClient.executeHttpRequest(any(), any(), any(), any(), eq(GetBackupsResponse.class)))
                .willReturn(response);

        RestAssuredMockMvc.standaloneSetup(controller);
    }

    private ResponseEntity<GetHealthResponse> getHealthResponseEntity(HttpStatus httpStatus, Availability availability) {
        GetHealthResponse healthResponse = getHealthResponse(availability);
        return new ResponseEntity<>(healthResponse, httpStatus);
    }

    private GetHealthResponse getHealthResponse(Availability availability) {
        return new GetHealthResponse()
                .availability(availability.toString())
                .status(STATUS_HEALTHY)
                .registeredAgents(new ArrayList<>())
                .ongoingAction(new OngoingAction());
    }

    private ResponseEntity<GetBackupsResponse> getBackupsResponse() {
        GetBackupsResponse backupsResponse = new GetBackupsResponse()
                .backups(getBackupList());
        return new ResponseEntity(backupsResponse, HttpStatus.OK);
    }

    private List<BackupDto> getBackupList() {
        return List.of(getBackup("myBackupFirst"),
                getBackup("myBackupSecond"),
                getBackup("myBackupThird"),
                getBackup("myBackupFourth"),
                getBackup("myBackupFifth"));
    }

    private BackupDto getBackup(String id) {
        return new BackupDto()
        .id(id)
        .name(id)
        .status(COMPLETE)
        .creationTime(CREATION_TIME);
    }
}
