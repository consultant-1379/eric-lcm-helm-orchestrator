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

package com.ericsson.oss.management.lcm.presentation.services;

import com.ericsson.oss.management.lcm.api.model.BackupDto;
import com.ericsson.oss.management.lcm.api.model.BackupManagerDto;
import com.ericsson.oss.management.lcm.api.model.ExportBroRequestDto;
import com.ericsson.oss.management.lcm.api.model.BroRequestDto;
import com.ericsson.oss.management.lcm.api.model.BroResponseActionDto;
import com.ericsson.oss.management.lcm.api.model.GetBackupManagersResponse;
import com.ericsson.oss.management.lcm.api.model.GetBackupsResponse;
import com.ericsson.oss.management.lcm.api.model.GetHealthResponse;
import com.ericsson.oss.management.lcm.api.model.ImportBroRequestDto;
import com.ericsson.oss.management.lcm.api.model.OngoingAction;
import com.ericsson.oss.management.lcm.api.model.PagedBackupManagersDto;
import com.ericsson.oss.management.lcm.api.model.PagedBackupsDto;
import com.ericsson.oss.management.lcm.model.entity.Availability;
import com.ericsson.oss.management.lcm.presentation.exceptions.BackupAndRestoreConnectionException;
import com.ericsson.oss.management.lcm.presentation.exceptions.BackupAndRestoreHttpClientException;
import com.ericsson.oss.management.lcm.presentation.routing.BroHttpClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.ericsson.oss.management.lcm.constants.BackupAndRestoreConstants.DEFAULT_BACKUP_MANAGER_ID;
import static com.ericsson.oss.management.lcm.constants.BackupAndRestoreConstants.STATUS_HEALTHY;
import static com.ericsson.oss.management.lcm.model.entity.Availability.AVAILABLE;
import static com.ericsson.oss.management.lcm.model.entity.Availability.BUSY;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = BackupAndRestoreServiceImpl.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BackupAndRestoreServiceImplTest {

    private static final String STATUS_FAILED = "Don't Healthy";
    private static final String BACKUP_NAME = "backupName";
    private static final String URI = "sftp://host:port/remotepath/backupManagerId/myBackup";
    private static final String PASSWORD = "123456";
    private static final String RESPONSE_ID = "1234";
    private static final String EXPORT_URI = "sftp://host:port/remotepath";
    private static final Integer ONE_PAGE = 1;
    private static final Integer SIZE_TWO = 2;
    private static final Integer SIZE_TEN = 10;
    private static final String ONE_ID = "1";
    private static final String TWO_ID = "2";
    private static final String FIRST_TYPE = "firstType";
    private static final String SECOND_TYPE = "secondType";
    private static final String FIRST_DOMAIN = "firstDomain";
    private static final String SECOND_DOMAIN = "secondDomain";

    @Autowired
    private BackupAndRestoreServiceImpl backupAndRestoreService;
    @MockBean
    private BroHttpClient routingClient;

    @BeforeEach
    void setUp() {
        //Init Health Response
        ResponseEntity<GetHealthResponse> healthResponse = getHealthResponseEntity(HttpStatus.OK, AVAILABLE);
        when(routingClient.executeHttpRequest(any(), anyString(), eq(HttpMethod.GET), any(), eq(GetHealthResponse.class)))
                .thenReturn(healthResponse);
    }

    @Test
    void shouldReturnResponseWhenGetHealth() {
        //Test method
        ResponseEntity<GetHealthResponse> response = backupAndRestoreService.health();

        //Verify
        assertThat(response.getBody().getStatus()).isEqualTo(STATUS_HEALTHY);
        assertThat(response.getBody().getAvailability()).hasToString(AVAILABLE.toString());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturnExceptionWhenBroUnavailable() {
        //Init
        ResponseEntity<GetHealthResponse> responseEntity = getHealthResponseEntity(HttpStatus.NOT_FOUND, AVAILABLE);
        responseEntity.getBody().setStatus(STATUS_FAILED);
        when(routingClient.executeHttpRequest(any(), anyString(), any(), any(), eq(GetHealthResponse.class)))
                .thenReturn(responseEntity);

        //Test method
        assertThatThrownBy(() -> backupAndRestoreService.health())
                .isInstanceOf(BackupAndRestoreConnectionException.class);
    }

    @Test
    void shouldReturnExceptionWhenBroAgentsAreBusy() {
        //Init
        ResponseEntity<GetHealthResponse> responseEntity = getHealthResponseEntity(HttpStatus.OK, BUSY);
        when(routingClient.executeHttpRequest(any(), anyString(), any(), any(), eq(GetHealthResponse.class)))
                .thenReturn(responseEntity);

        //Test method
        assertThatThrownBy(() -> backupAndRestoreService.health())
                .isInstanceOf(BackupAndRestoreConnectionException.class);
    }

    @Test
    void shouldReturnResponseWhenPostCreateBackup() {
        //Init
        BroRequestDto request = getBroRequest();
        ResponseEntity<BroResponseActionDto> responseAction = getActionResponse();
        when(routingClient.executeHttpRequest(any(), anyString(), eq(HttpMethod.POST), any(), eq(BroResponseActionDto.class)))
                .thenReturn(responseAction);

        //Test method
        BroResponseActionDto response = backupAndRestoreService.createBackup(request);

        //Verify
        assertThat(response.getId()).isNotNull();
        verify(routingClient).executeHttpRequest(any(), anyString(), any(), any(), eq(BroResponseActionDto.class));
    }

    @Test
    void shouldReturnResponseWhenDeleteBackup() {
        //Init
        BroRequestDto request = getBroRequest();
        ResponseEntity<BroResponseActionDto> responseAction = getActionResponse();
        when(routingClient.executeHttpRequest(any(), anyString(), eq(HttpMethod.POST), any(), eq(BroResponseActionDto.class)))
                .thenReturn(responseAction);

        //Test method
        backupAndRestoreService.deleteBackup(request);

        //Verify
        verify(routingClient).executeHttpRequest(any(), anyString(), eq(HttpMethod.POST), any(), eq(BroResponseActionDto.class));
    }

    @Test
    void shouldReturnResponseWhenImportBackup() {
        //Init
        ImportBroRequestDto request = getImportBroRequest();
        ResponseEntity<BroResponseActionDto> responseAction = getActionResponse();
        when(routingClient.executeHttpRequest(any(), anyString(), eq(HttpMethod.POST), any(), eq(BroResponseActionDto.class)))
                .thenReturn(responseAction);

        //Test method
        BroResponseActionDto response = backupAndRestoreService.importBackup(request);

        //Verify
        assertThat(response.getId()).isNotNull();
        verify(routingClient).executeHttpRequest(any(), anyString(), eq(HttpMethod.POST), any(), eq(BroResponseActionDto.class));
    }

    @Test
    void shouldReturnResponseWhenPostExportBackup() {
        //Init
        ResponseEntity<BroResponseActionDto> responseAction = getActionResponse();
        when(routingClient.executeHttpRequest(any(), anyString(), eq(HttpMethod.POST), any(), eq(BroResponseActionDto.class)))
                .thenReturn(responseAction);

        //Test method
        BroResponseActionDto response = backupAndRestoreService.exportBackup(getBroExportRequest());

        //Verify
        assertThat(response.getId()).isNotNull();
        verify(routingClient).executeHttpRequest(any(), anyString(), any(), any(), eq(BroResponseActionDto.class));
    }

    @Test
    void shouldReturnResponseWhenRestoreBackup() {
        //Init
        BroRequestDto request = getBroRequest();
        ResponseEntity<BroResponseActionDto> responseAction = getActionResponse();
        when(routingClient.executeHttpRequest(any(), anyString(), eq(HttpMethod.POST), any(), eq(BroResponseActionDto.class)))
                .thenReturn(responseAction);

        //Test method
        BroResponseActionDto response = backupAndRestoreService.restoreBackup(request);

        //Verify
        assertThat(response.getId()).isNotNull();
        verify(routingClient).executeHttpRequest(any(), anyString(), eq(HttpMethod.POST), any(), eq(BroResponseActionDto.class));
    }

    @Test
    void shouldReturnResponseWhenGetBackups() {
        //Init
        ResponseEntity<GetBackupsResponse> response = getBackupsResponse();
        when(routingClient.executeHttpRequest(any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupsResponse.class)))
                 .thenReturn(response);

        //Test method
        PagedBackupsDto result = backupAndRestoreService.getPagedBackups(DEFAULT_BACKUP_MANAGER_ID,
                ONE_PAGE, SIZE_TWO, Collections.singletonList("creationTime,asc"));

        //Verify
        verify(routingClient).executeHttpRequest(any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupsResponse.class));
        assertThat(result.getContent().size()).isEqualTo(SIZE_TWO);
    }

    @Test
    void shouldReturnResponseWhenGetBackupsWithoutSort() {
        //Init
        ResponseEntity<GetBackupsResponse> response = getBackupsResponse();
        List<BackupDto> backupsList = response.getBody().getBackups();
        when(routingClient.executeHttpRequest(any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupsResponse.class)))
                .thenReturn(response);

        //Test method
        PagedBackupsDto result = backupAndRestoreService.getPagedBackups(DEFAULT_BACKUP_MANAGER_ID,
                                                                         ONE_PAGE, SIZE_TWO, null);

        //Verify
        verify(routingClient).executeHttpRequest(any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupsResponse.class));
        assertThat(result.getContent()).isEqualTo(backupsList);
    }

    @Test
    void shouldReturnResponseWhenGetBackupsStatusDesc() {
        //Init
        ResponseEntity<GetBackupsResponse> response = getBackupsResponse();
        when(routingClient.executeHttpRequest(any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupsResponse.class)))
                .thenReturn(response);

        //Test method
        PagedBackupsDto result = backupAndRestoreService.getPagedBackups(DEFAULT_BACKUP_MANAGER_ID,
                ONE_PAGE, SIZE_TWO, Collections.singletonList("status,desc"));

        //Verify
        verify(routingClient).executeHttpRequest(any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupsResponse.class));
        assertThat(result.getContent().size()).isEqualTo(SIZE_TWO);
        assertThat(result.getContent().get(0).getStatus().toString()).hasToString(BackupDto.StatusEnum.INCOMPLETE.toString());
        assertThat(result.getContent().get(1).getStatus().toString()).hasToString(BackupDto.StatusEnum.COMPLETE.toString());
    }

    @Test
    void shouldReturnResponseWhenGetBackupsStatusAsc() {
        //Init
        ResponseEntity<GetBackupsResponse> response = getBackupsResponse();
        when(routingClient.executeHttpRequest(any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupsResponse.class)))
                .thenReturn(response);

        //Test method
        PagedBackupsDto result = backupAndRestoreService.getPagedBackups(DEFAULT_BACKUP_MANAGER_ID,
                ONE_PAGE, SIZE_TWO, Collections.singletonList("status,asc"));

        //Verify
        verify(routingClient).executeHttpRequest(any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupsResponse.class));
        assertThat(result.getContent().size()).isEqualTo(SIZE_TWO);
        assertThat(result.getContent().get(0).getStatus().toString()).hasToString(BackupDto.StatusEnum.COMPLETE.toString());
        assertThat(result.getContent().get(1).getStatus().toString()).hasToString(BackupDto.StatusEnum.INCOMPLETE.toString());
    }

    @Test
    void shouldReturnResponseWhenGetBackupsWithNoDirection() {
        //Init
        ResponseEntity<GetBackupsResponse> response = getBackupsResponse();
        when(routingClient.executeHttpRequest(any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupsResponse.class)))
                .thenReturn(response);

        //Test method
        PagedBackupsDto result = backupAndRestoreService.getPagedBackups(DEFAULT_BACKUP_MANAGER_ID,
                ONE_PAGE, SIZE_TWO, Collections.singletonList("status"));

        //Verify
        verify(routingClient).executeHttpRequest(any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupsResponse.class));
        assertThat(result.getContent().size()).isEqualTo(SIZE_TWO);
        assertThat(result.getContent().get(0).getStatus().toString()).hasToString(BackupDto.StatusEnum.COMPLETE.toString());
        assertThat(result.getContent().get(1).getStatus().toString()).hasToString(BackupDto.StatusEnum.INCOMPLETE.toString());
    }

    @Test
    void shouldReturnResponseWhenGetBackupManagersWithoutSort() {
        //Init
        ResponseEntity<GetBackupManagersResponse> response = getBackupManagersResponse();
        List<BackupManagerDto> backupManagersList = response.getBody().getBackupManagers();
        when(routingClient.executeHttpRequest(any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupManagersResponse.class)))
                .thenReturn(response);

        //Test method
        PagedBackupManagersDto result = backupAndRestoreService.getPagedBackupManagers(ONE_PAGE, SIZE_TWO, null);

        //Verify
        verify(routingClient).executeHttpRequest(any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupManagersResponse.class));
        assertThat(result.getContent()).isEqualTo(backupManagersList);
    }

    @Test
    void shouldReturnResponseWhenGetBackupManagersIdAsc() {
        //Init
        ResponseEntity<GetBackupManagersResponse> response = getBackupManagersResponse();
        when(routingClient.executeHttpRequest(
                any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupManagersResponse.class)))
                .thenReturn(response);

        //Test method
        PagedBackupManagersDto result = backupAndRestoreService.getPagedBackupManagers(
                ONE_PAGE, SIZE_TWO, Collections.singletonList("id,asc"));

        //Verify
        verify(routingClient).executeHttpRequest(
                any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupManagersResponse.class));
        assertThat(result.getContent().size()).isEqualTo(SIZE_TWO);
        assertThat(result.getContent().get(0).getId()).isEqualTo(ONE_ID);
        assertThat(result.getContent().get(1).getId()).isEqualTo(TWO_ID);
    }

    @Test
    void shouldReturnResponseWhenGetBackupManagersIdDesc() {
        //Init
        ResponseEntity<GetBackupManagersResponse> response = getBackupManagersResponse();
        when(routingClient.executeHttpRequest(
                any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupManagersResponse.class)))
                .thenReturn(response);

        //Test method
        PagedBackupManagersDto result = backupAndRestoreService.getPagedBackupManagers(
                ONE_PAGE, SIZE_TWO, Collections.singletonList("id,desc"));

        //Verify
        verify(routingClient).executeHttpRequest(
                any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupManagersResponse.class));
        assertThat(result.getContent().size()).isEqualTo(SIZE_TWO);
        assertThat(result.getContent().get(0).getId()).isEqualTo(TWO_ID);
        assertThat(result.getContent().get(1).getId()).isEqualTo(ONE_ID);
    }

    @Test
    void shouldReturnResponseWhenGetBackupManagersWithNoDirection() {
        //Init
        ResponseEntity<GetBackupManagersResponse> response = getBackupManagersResponse();
        when(routingClient.executeHttpRequest(
                any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupManagersResponse.class)))
                .thenReturn(response);

        //Test method
        PagedBackupManagersDto result = backupAndRestoreService.getPagedBackupManagers(
                ONE_PAGE, SIZE_TWO, Collections.singletonList("id,asc"));

        //Verify
        verify(routingClient).executeHttpRequest(
                any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupManagersResponse.class));
        assertThat(result.getContent().size()).isEqualTo(SIZE_TWO);
        assertThat(result.getContent().get(0).getId()).isEqualTo(ONE_ID);
        assertThat(result.getContent().get(1).getId()).isEqualTo(TWO_ID);
    }

    @Test
    void shouldReturnResponseWhenGetBackupManagersTypeDesc() {
        //Init
        ResponseEntity<GetBackupManagersResponse> response = getBackupManagersResponse();
        when(routingClient.executeHttpRequest(
                any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupManagersResponse.class)))
                .thenReturn(response);

        //Test method
        PagedBackupManagersDto result = backupAndRestoreService.getPagedBackupManagers(
                ONE_PAGE, SIZE_TWO, Collections.singletonList("backupType,desc"));

        //Verify
        verify(routingClient).executeHttpRequest(
                any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupManagersResponse.class));
        assertThat(result.getContent().size()).isEqualTo(SIZE_TWO);
        assertThat(result.getContent().get(0).getBackupType()).isEqualTo(SECOND_TYPE);
        assertThat(result.getContent().get(1).getBackupType()).isEqualTo(FIRST_TYPE);
    }

    @Test
    void shouldReturnResponseWhenGetBackupManagersTypeAsc() {
        //Init
        ResponseEntity<GetBackupManagersResponse> response = getBackupManagersResponse();
        when(routingClient.executeHttpRequest(
                any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupManagersResponse.class)))
                .thenReturn(response);

        //Test method
        PagedBackupManagersDto result = backupAndRestoreService.getPagedBackupManagers(
                ONE_PAGE, SIZE_TWO, Collections.singletonList("backupType,asc"));

        //Verify
        verify(routingClient).executeHttpRequest(
                any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupManagersResponse.class));
        assertThat(result.getContent().size()).isEqualTo(SIZE_TWO);
        assertThat(result.getContent().get(0).getBackupType()).isEqualTo(FIRST_TYPE);
        assertThat(result.getContent().get(1).getBackupType()).isEqualTo(SECOND_TYPE);
    }

    @Test
    void shouldReturnResponseWhenGetBackupManagersDomainDesc() {
        //Init
        ResponseEntity<GetBackupManagersResponse> response = getBackupManagersResponse();
        when(routingClient.executeHttpRequest(
                any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupManagersResponse.class)))
                .thenReturn(response);

        //Test method
        PagedBackupManagersDto result = backupAndRestoreService.getPagedBackupManagers(
                ONE_PAGE, SIZE_TWO, Collections.singletonList("backupDomain,desc"));

        //Verify
        verify(routingClient).executeHttpRequest(
                any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupManagersResponse.class));
        assertThat(result.getContent().size()).isEqualTo(SIZE_TWO);
        assertThat(result.getContent().get(0).getBackupDomain()).isEqualTo(SECOND_DOMAIN);
        assertThat(result.getContent().get(1).getBackupDomain()).isEqualTo(FIRST_DOMAIN);
    }

    @Test
    void shouldReturnResponseWhenGetBackupManagersDomainAsc() {
        //Init
        ResponseEntity<GetBackupManagersResponse> response = getBackupManagersResponse();
        when(routingClient.executeHttpRequest(
                any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupManagersResponse.class)))
                .thenReturn(response);

        //Test method
        PagedBackupManagersDto result = backupAndRestoreService.getPagedBackupManagers(
                ONE_PAGE, SIZE_TWO, Collections.singletonList("backupDomain,asc"));

        //Verify
        verify(routingClient).executeHttpRequest(
                any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupManagersResponse.class));
        assertThat(result.getContent().size()).isEqualTo(SIZE_TWO);
        assertThat(result.getContent().get(0).getBackupDomain()).isEqualTo(FIRST_DOMAIN);
        assertThat(result.getContent().get(1).getBackupDomain()).isEqualTo(SECOND_DOMAIN);
    }

    @Test
    void shouldReturnExceptionWhenBroReturnError() {
        //Init
        when(routingClient.executeHttpRequest(any(), anyString(), eq(HttpMethod.GET), any(), any()))
                .thenThrow(BackupAndRestoreHttpClientException.class);

        //Test method
        Throwable throwable = catchThrowable(() -> backupAndRestoreService
                .getPagedBackups(DEFAULT_BACKUP_MANAGER_ID,
                ONE_PAGE, SIZE_TEN, Collections.singletonList("creationTime,asc")));

        //Verify
        verify(routingClient).executeHttpRequest(any(), anyString(), eq(HttpMethod.GET), any(), any());
        assertThat(throwable).isInstanceOf(BackupAndRestoreHttpClientException.class);
    }

    @Test
    void shouldReturnResponseWhenGetBackupManagers() {
        //Init
        ResponseEntity<GetBackupManagersResponse> response = getBackupManagersResponse();
        when(routingClient.executeHttpRequest(
                any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupManagersResponse.class)))
                .thenReturn(response);

        //Test method
        PagedBackupManagersDto result = backupAndRestoreService.getPagedBackupManagers(
                ONE_PAGE, SIZE_TWO, Collections.singletonList("id,asc"));

        //Verify
        verify(routingClient).executeHttpRequest(
                any(), anyString(), eq(HttpMethod.GET), any(), eq(GetBackupManagersResponse.class));
        assertThat(result.getContent().size()).isEqualTo(SIZE_TWO);
    }

    @Test
    void shouldThrowExceptionWhenGetBackupManagers() {
        //Init
        when(routingClient.executeHttpRequest(any(), anyString(), eq(HttpMethod.GET), any(), any()))
                .thenThrow(BackupAndRestoreHttpClientException.class);

        //Test method
        Throwable throwable = catchThrowable(() -> backupAndRestoreService.getPagedBackupManagers(
                ONE_PAGE, SIZE_TWO, Collections.singletonList("id,asc")));

        //Verify
        verify(routingClient).executeHttpRequest(any(), anyString(), eq(HttpMethod.GET), any(), any());
        assertThat(throwable).isInstanceOf(BackupAndRestoreHttpClientException.class);
    }

    @Test
    void shouldSetDefaultManagerIdWhenAbsent() {
        //Init
        BroRequestDto request = getBroRequest();
        ResponseEntity<BroResponseActionDto> responseAction = getActionResponse();
        when(routingClient.executeHttpRequest(any(), anyString(), eq(HttpMethod.POST), any(), eq(BroResponseActionDto.class)))
                .thenReturn(responseAction);

        //Test method
        BroResponseActionDto response = backupAndRestoreService.createBackup(request);

        //Verify
        assertThat(response.getId()).isNotNull();
        verify(routingClient).executeHttpRequest(any(), anyString(), any(), any(), eq(BroResponseActionDto.class));
    }

    private GetHealthResponse getHealthResponse(Availability availability) {
        return new GetHealthResponse()
                .availability(availability.toString())
                .status(STATUS_HEALTHY)
                .registeredAgents(new ArrayList<>())
                .ongoingAction(new OngoingAction());
    }

    private ResponseEntity<GetHealthResponse> getHealthResponseEntity(HttpStatus httpStatus, Availability availability) {
        GetHealthResponse healthResponse = getHealthResponse(availability);
        return new ResponseEntity<>(healthResponse, httpStatus);
    }

    private ResponseEntity<GetBackupsResponse> getBackupsResponse() {
        GetBackupsResponse backupsResponse = new GetBackupsResponse()
                .backups(getBackupList());
        return new ResponseEntity(backupsResponse, HttpStatus.OK);
    }

    private BroRequestDto getBroRequest() {
        BroRequestDto requestDto = new BroRequestDto();
        requestDto.setBackupName(BACKUP_NAME);
        requestDto.setBackupManagerId(DEFAULT_BACKUP_MANAGER_ID);
        return requestDto;
    }

    private ImportBroRequestDto getImportBroRequest() {
        ImportBroRequestDto requestDto = new ImportBroRequestDto();
        requestDto.setBackupManagerId(DEFAULT_BACKUP_MANAGER_ID);
        requestDto.setUri(URI);
        requestDto.setPassword(PASSWORD);
        return requestDto;
    }

    private ExportBroRequestDto getBroExportRequest() {
        ExportBroRequestDto requestDto = new ExportBroRequestDto();
        requestDto.setBackupName(BACKUP_NAME);
        requestDto.setBackupManagerId(DEFAULT_BACKUP_MANAGER_ID);
        requestDto.setUri(EXPORT_URI);
        requestDto.setPassword(PASSWORD);
        return requestDto;
    }

    private ResponseEntity<BroResponseActionDto> getActionResponse() {
        BroResponseActionDto responseDto = new BroResponseActionDto();
        responseDto.setId(RESPONSE_ID);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    private List<BackupDto> getBackupList() {
        List<BackupDto> backupDtoList = new ArrayList<>();
        backupDtoList.add(new BackupDto().id(ONE_ID).status(BackupDto.StatusEnum.COMPLETE).creationTime("10:05:2022"));
        backupDtoList.add(new BackupDto().id(TWO_ID).status(BackupDto.StatusEnum.INCOMPLETE).creationTime("01:05:2022"));
        return backupDtoList;
    }

    private ResponseEntity<GetBackupManagersResponse> getBackupManagersResponse() {
        GetBackupManagersResponse backupManagersResponse = new GetBackupManagersResponse();
        backupManagersResponse.setBackupManagers(getBackupManagersList());

        return new ResponseEntity<>(backupManagersResponse, HttpStatus.OK);
    }

    private List<BackupManagerDto> getBackupManagersList() {
        List<BackupManagerDto> managersDto = new ArrayList<>();
        managersDto.add(new BackupManagerDto().id(ONE_ID).backupType(FIRST_TYPE).backupDomain(FIRST_DOMAIN));
        managersDto.add(new BackupManagerDto().id(TWO_ID).backupType(SECOND_TYPE).backupDomain(SECOND_DOMAIN));
        return managersDto;
    }
}
