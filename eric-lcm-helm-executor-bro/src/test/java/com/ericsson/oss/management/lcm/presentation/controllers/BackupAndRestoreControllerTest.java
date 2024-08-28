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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.management.lcm.constants.BackupAndRestoreConstants.STATUS_HEALTHY;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import com.ericsson.oss.management.lcm.api.model.BroRequestDto;
import com.ericsson.oss.management.lcm.api.model.BroResponseActionDto;
import com.ericsson.oss.management.lcm.api.model.ExportBroRequestDto;
import com.ericsson.oss.management.lcm.api.model.GetBackupsResponse;
import com.ericsson.oss.management.lcm.api.model.GetHealthResponse;
import com.ericsson.oss.management.lcm.api.model.ImportBroRequestDto;
import com.ericsson.oss.management.lcm.api.model.OngoingAction;
import com.ericsson.oss.management.lcm.model.entity.Availability;
import com.ericsson.oss.management.lcm.presentation.services.BackupAndRestoreServiceImpl;

@SpringBootTest(classes = BackupAndRestoreController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BackupAndRestoreControllerTest {

    @Autowired
    private BackupAndRestoreController controller;
    @MockBean
    private BackupAndRestoreServiceImpl backupAndRestoreService;

    private static final String BACKUP_NAME = "backupName";
    private static final String RESPONSE_ID = "1234";
    private static final String BACKUP_MANAGER_ID = "DEFAULT";
    private static final String URI = "sftp://host:port/remotepath/backupManagerId/myBackup";
    private static final String PASSWORD = "123456";
    private static final String EXPORT_URI = "sftp://host:port/remotepath";
    private static final Integer ONE_PAGE = 1;
    private static final Integer SIZE_TEN = 10;

    @Test
    void shouldSuccessfullyPerformBroHealthCheck() {
        //Init
        ResponseEntity<GetHealthResponse> response = getHealthResponseEntity();
        when(backupAndRestoreService.health()).thenReturn(response);

        //Test method
        ResponseEntity<GetHealthResponse> result = controller.backupAndRestoreHealthGet();

        //Verify
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturnCreatedWhenCreateBackup() {
        //Init
        BroResponseActionDto response = getActionResponse();
        BroRequestDto request = getBroRequest();
        when(backupAndRestoreService.createBackup(request)).thenReturn(response);

        //Test method
        ResponseEntity<BroResponseActionDto> result = controller.backupAndRestorePost(request);

        //Verify
        verify(backupAndRestoreService).createBackup(request);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void shouldReturnNoContentWhenDeleteBackup() {
        //Init
        BroRequestDto request = getBroRequest();
        doNothing().when(backupAndRestoreService).deleteBackup(request);

        //Test method
        ResponseEntity<Void> result = controller.backupAndRestoreDelete(request);

        //Verify
        verify(backupAndRestoreService).deleteBackup(request);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void shouldReturnCreatedWhenExportBackup() {
        //Init
        BroResponseActionDto response = getActionResponse();
        ExportBroRequestDto request = getBroExportRequest();
        when(backupAndRestoreService.exportBackup(request)).thenReturn(response);

        //Test method
        ResponseEntity<BroResponseActionDto> result = controller.backupAndRestoreExportsPost(request);

        //Verify
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(backupAndRestoreService).exportBackup(request);
    }

    @Test
    void shouldReturnAcceptedWhenRestoreBackup() {
        //Init
        BroResponseActionDto restoreResponse = getActionResponse();
        BroRequestDto request = getBroRequest();
        when(backupAndRestoreService.restoreBackup(request)).thenReturn(restoreResponse);

        //Test method
        ResponseEntity<BroResponseActionDto> result = controller.backupAndRestorePut(request);

        //Verify
        verify(backupAndRestoreService).restoreBackup(request);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    void shouldSuccessfullyPerformGetBackups() {
        HttpStatusCode result = controller.backupAndRestoreBackupManagerIdGet(BACKUP_MANAGER_ID,
                ONE_PAGE, SIZE_TEN, Collections.singletonList("creationTime,asc"))
                .getStatusCode();

        assertThat(result).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldSuccessfullyPerformGetBackupManagers() {
        HttpStatusCode result = controller.backupAndRestoreManagersGet(
                ONE_PAGE, SIZE_TEN, Collections.singletonList("id,asc")).getStatusCode();

        assertThat(result).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturnCreatedWhenImportBackup() {
        //Init
        BroResponseActionDto response = getActionResponse();
        ImportBroRequestDto request = getImportBroRequest();
        when(backupAndRestoreService.importBackup(request)).thenReturn(response);

        //Test method
        ResponseEntity<BroResponseActionDto> result = controller.backupAndRestoreImportsPost(request);

        //Verify
        verify(backupAndRestoreService).importBackup(request);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private ResponseEntity<GetHealthResponse> getHealthResponseEntity() {
        GetHealthResponse healthResponse = getHealthResponse();
        return new ResponseEntity<>(healthResponse, HttpStatus.OK);
    }

    private ResponseEntity<GetBackupsResponse> getBackupsResponseEntity() {
        GetBackupsResponse backupsResponse = getBackupsResponse();
        return new ResponseEntity<>(backupsResponse, HttpStatus.OK);
    }

    private GetHealthResponse getHealthResponse() {
        return new GetHealthResponse()
                .availability(Availability.AVAILABLE.toString())
                .status(STATUS_HEALTHY)
                .registeredAgents(new ArrayList<>())
                .ongoingAction(new OngoingAction());
    }

    private GetBackupsResponse getBackupsResponse() {
        return new GetBackupsResponse()
                .backups(new ArrayList<>());
    }

    private BroResponseActionDto getActionResponse() {
        BroResponseActionDto responseDto = new BroResponseActionDto();
        responseDto.setId(RESPONSE_ID);
        return responseDto;
    }

    private BroRequestDto getBroRequest() {
        BroRequestDto requestDto = new BroRequestDto();
        requestDto.setBackupName(BACKUP_NAME);
        requestDto.setBackupManagerId(BACKUP_MANAGER_ID);
        return requestDto;
    }

    private ImportBroRequestDto getImportBroRequest() {
        ImportBroRequestDto requestDto = new ImportBroRequestDto();
        requestDto.setBackupManagerId(BACKUP_MANAGER_ID);
        requestDto.setUri(URI);
        requestDto.setPassword(PASSWORD);
        return requestDto;
    }

    private ExportBroRequestDto getBroExportRequest() {
        ExportBroRequestDto requestDto = new ExportBroRequestDto();
        requestDto.setBackupName(BACKUP_NAME);
        requestDto.setBackupManagerId(BACKUP_MANAGER_ID);
        requestDto.setUri(EXPORT_URI);
        requestDto.setPassword(PASSWORD);
        return requestDto;
    }
}
