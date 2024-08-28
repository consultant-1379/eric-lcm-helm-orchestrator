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

package com.ericsson.oss.management.lcm.e2e;

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.api.model.BackupDto;
import com.ericsson.oss.management.lcm.api.model.BackupManagerDto;
import com.ericsson.oss.management.lcm.api.model.BroResponseActionDto;
import com.ericsson.oss.management.lcm.api.model.GetBackupManagersResponse;
import com.ericsson.oss.management.lcm.api.model.GetBackupsResponse;
import com.ericsson.oss.management.lcm.api.model.GetHealthResponse;
import com.ericsson.oss.management.lcm.api.model.OngoingAction;
import com.ericsson.oss.management.lcm.model.entity.Availability;
import com.ericsson.oss.management.lcm.utils.TestingFileUtils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import static com.ericsson.oss.management.lcm.api.model.BackupDto.StatusEnum.COMPLETE;
import static com.ericsson.oss.management.lcm.constants.BackupAndRestoreConstants.STATUS_HEALTHY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
class BackupAndRestoreControllerTest extends AbstractDbSetupTest {

    private static final String HEALTH_URL = "/cnwlcm/v1/backup_and_restore/health";
    private static final String IMPORT_URL = "/cnwlcm/v1/backup_and_restore/imports";
    private static final String ACTIONS_URL = "/cnwlcm/v1/backup_and_restore";
    private static final String BACKUPS_URL = "/cnwlcm/v1/backup_and_restore/DEFAULT";
    private static final String MANAGERS_URL = "/cnwlcm/v1/backup_and_restore/managers";
    private static final String EXPORT_URL = "/cnwlcm/v1/backup_and_restore/exports";
    private static final String CREATION_TIME = "2022-03-17T11:07:52.592902Z";
    private static final String RESPONSE_ID = "12335";
    private static final String BRO_EXPORT_JSON = "backupAndRestore/exportBackupRequest.json";
    private static final String BRO_IMPORT_JSON = "backupAndRestore/importBackupRequest.json";
    private static final String BRO_REQUEST_JSON = "backupAndRestore/backupRequest.json";

    private static String jsonRequest;
    private static String jsonExportRequest;
    private static String jsonImportRequest;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestTemplate restTemplate;

    @BeforeEach
    public void setUp() {
        //Init Health Response
        ResponseEntity<GetHealthResponse> healthResponse = getHealthResponseEntity(OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(GetHealthResponse.class)))
                .thenReturn(healthResponse);
    }

    @BeforeAll
    public static void filesSetup() throws Exception {
        jsonExportRequest = TestingFileUtils.readDataFromFile(BRO_EXPORT_JSON);
        jsonImportRequest = TestingFileUtils.readDataFromFile(BRO_IMPORT_JSON);
        jsonRequest = TestingFileUtils.readDataFromFile(BRO_REQUEST_JSON);
    }

    @Test
    void shouldReturnHealthResponse() throws Exception {
        //Verify
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get(HEALTH_URL)).andReturn();
        assertThatResponseCodeIs(result, OK);
    }

    @Test
    void shouldFailedWhenBroServiceUnavailable() throws Exception {
        //init
        ResponseEntity<GetHealthResponse> response = getHealthResponseEntity(SERVICE_UNAVAILABLE);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(GetHealthResponse.class)))
                .thenReturn(response);
        //Verify
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get(HEALTH_URL)).andReturn();
        assertThatResponseCodeIs(result, SERVICE_UNAVAILABLE);
        assertThat(result
                .getResponse()
                .getContentAsString()).containsIgnoringCase("Backup and Restore Orchestrator is unavailable right now");
    }

    @Test
    void shouldReturnGetBackupsResponse() throws Exception {
        //init
        ResponseEntity<GetBackupsResponse> backupsResponse = getBackupsResponse();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(GetBackupsResponse.class)))
                .thenReturn(backupsResponse);

        //Verify
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get(BACKUPS_URL)).andReturn();

        assertThatResponseCodeIs(result, OK);
        String response = result
                .getResponse()
                .getContentAsString();
        assertThat(response).contains("myBackupFirst");
        assertThat(response).contains("myBackupSecond");
        assertThat(response).contains("myBackupThird");
        assertThat(response).contains("myBackupFourth");
        assertThat(response).contains("myBackupFifth");
    }

    @Test
    void shouldReturnGetBackupManagersResponse() throws Exception {
        //init
        ResponseEntity<GetBackupManagersResponse> managersResponse = getBackupManagersResponse();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(GetBackupManagersResponse.class)))
                .thenReturn(managersResponse);

        //Verify
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get(MANAGERS_URL)).andReturn();

        assertThatResponseCodeIs(result, OK);
        String response = result
                .getResponse()
                .getContentAsString();
        assertThat(response).contains("FirstManager");
        assertThat(response).contains("SecondManager");
    }

    @Test
    void shouldSuccessfullyImportBackup() throws Exception {
        //init
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(BroResponseActionDto.class)))
                .thenReturn(getActionResponse(CREATED));

        //Verify
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                                                   .post(IMPORT_URL)
                                                   .contentType(MediaType.APPLICATION_JSON)
                                                   .content(jsonImportRequest))
                .andReturn();
        assertThatResponseCodeIs(result, CREATED);
        ObjectMapper mapper = new ObjectMapper();
        BroResponseActionDto responseBody = mapper.readValue(result.getResponse().getContentAsString(), BroResponseActionDto.class);
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.getId()).isEqualTo(RESPONSE_ID);
    }

    @Test
    void shouldSuccessfullyCreateBackup() throws Exception {
        //init
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(BroResponseActionDto.class)))
                .thenReturn(getActionResponse(CREATED));

        //Verify
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                                                   .post(ACTIONS_URL)
                                                   .contentType(MediaType.APPLICATION_JSON)
                                                   .content(jsonRequest))
                .andReturn();
        assertThatResponseCodeIs(result, CREATED);
        ObjectMapper mapper = new ObjectMapper();
        BroResponseActionDto responseBody = mapper.readValue(result.getResponse().getContentAsString(), BroResponseActionDto.class);
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.getId()).isEqualTo(RESPONSE_ID);
    }

    @Test
    void shouldSuccessfullyDeleteBackup() throws Exception {
        //init
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(BroResponseActionDto.class)))
                .thenReturn(getActionResponse(NO_CONTENT));

        //Verify
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                                                   .delete(ACTIONS_URL)
                                                   .contentType(MediaType.APPLICATION_JSON)
                                                   .content(jsonRequest))
                .andReturn();
        assertThatResponseCodeIs(result, NO_CONTENT);
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).isEmpty();
    }

    @Test
    void shouldRestoreBackupResponse() throws Exception {
        //init
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(BroResponseActionDto.class)))
                .thenReturn(getActionResponse(CREATED));

        //Verify
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                                                   .put(ACTIONS_URL)
                                                   .contentType(MediaType.APPLICATION_JSON)
                                                   .content(jsonRequest))
                .andReturn();

        assertThatResponseCodeIs(result, ACCEPTED);
        ObjectMapper mapper = new ObjectMapper();
        BroResponseActionDto responseBody = mapper.readValue(result.getResponse().getContentAsString(), BroResponseActionDto.class);
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.getId()).isEqualTo(RESPONSE_ID);
    }

    @Test
    void shouldReturnExportBackupsResponse() throws Exception {
        //init
        ResponseEntity<BroResponseActionDto> responseAction = getActionResponse(CREATED);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(BroResponseActionDto.class)))
                .thenReturn(responseAction);

        //Verify
        MvcResult result = mockMvc.perform(
                MockMvcRequestBuilders.post(EXPORT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonExportRequest))
                .andReturn();

        assertThatResponseCodeIs(result, CREATED);
        String response = result
                .getResponse()
                .getContentAsString();
        assertThat(response).contains(RESPONSE_ID);
    }

    private GetHealthResponse getHealthResponse() {
        return new GetHealthResponse()
                .availability(Availability.AVAILABLE.toString())
                .status(STATUS_HEALTHY)
                .registeredAgents(new ArrayList<>())
                .ongoingAction(new OngoingAction());
    }

    private ResponseEntity<GetHealthResponse> getHealthResponseEntity(HttpStatus httpStatus) {
        GetHealthResponse healthResponse = getHealthResponse();
        return new ResponseEntity<>(healthResponse, httpStatus);
    }

    private void assertThatResponseCodeIs(MvcResult result, HttpStatus httpStatus) {
        assertThat(result).isNotNull();
        assertThat(result.getResponse()).isNotNull();
        assertThat(result
                .getResponse()
                .getStatus()).isEqualTo(httpStatus.value());
    }

    private ResponseEntity<GetBackupsResponse> getBackupsResponse() {
        GetBackupsResponse backupsResponse =  new GetBackupsResponse()
                .backups(getBackupList());
        return new ResponseEntity<>(backupsResponse, HttpStatus.OK);
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

    private ResponseEntity<BroResponseActionDto> getActionResponse(HttpStatus httpStatus) {
        BroResponseActionDto action = new BroResponseActionDto()
                .id(RESPONSE_ID);
        return new ResponseEntity<>(action, httpStatus);
    }

    private ResponseEntity<GetBackupManagersResponse> getBackupManagersResponse() {
        GetBackupManagersResponse managersResponse = new GetBackupManagersResponse()
                .backupManagers(getBackupManagersList());
        return new ResponseEntity<>(managersResponse, OK);
    }

    private List<BackupManagerDto> getBackupManagersList() {
        return List.of(
                getBackupManagerDto("FirstManager"),
                getBackupManagerDto("SecondManager"));
    }

    private BackupManagerDto getBackupManagerDto(String id) {
        return new BackupManagerDto()
                .id(id)
                .backupType("type")
                .backupDomain("domain");
    }
}

