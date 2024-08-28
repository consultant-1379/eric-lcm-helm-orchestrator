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

import static com.ericsson.oss.management.lcm.constants.BackupAndRestoreConstants.BACKUP_ID;
import static com.ericsson.oss.management.lcm.constants.BackupAndRestoreConstants.BACKUP_MANAGERS_SORT_COLUMNS;
import static com.ericsson.oss.management.lcm.constants.BackupAndRestoreConstants.DEFAULT_BACKUP_MANAGER_ID;
import static com.ericsson.oss.management.lcm.constants.BackupAndRestoreConstants.SORT_COLUMNS;
import static com.ericsson.oss.management.lcm.constants.BackupAndRestoreConstants.STATUS_HEALTHY;
import static com.ericsson.oss.management.lcm.model.entity.Availability.BUSY;
import static com.ericsson.oss.management.lcm.utils.pagination.BroPaginationUtils.buildLinks;
import static com.ericsson.oss.management.lcm.utils.pagination.BroPaginationUtils.buildPaginationInfo;
import static com.ericsson.oss.management.lcm.utils.pagination.BroPaginationUtils.parseSortBackupManagersDto;
import static com.ericsson.oss.management.lcm.utils.pagination.BroPaginationUtils.parseSortBackupDto;
import static com.ericsson.oss.management.lcm.utils.pagination.BroPaginationUtils.createPageable;
import com.ericsson.oss.management.lcm.api.model.BackupDto;
import com.ericsson.oss.management.lcm.api.model.BackupManagerDto;
import com.ericsson.oss.management.lcm.api.model.BroRequestDto;
import com.ericsson.oss.management.lcm.api.model.BroResponseActionDto;
import com.ericsson.oss.management.lcm.api.model.ExportBroRequestDto;
import com.ericsson.oss.management.lcm.api.model.GetBackupManagersResponse;
import com.ericsson.oss.management.lcm.api.model.GetBackupsResponse;
import com.ericsson.oss.management.lcm.api.model.GetHealthResponse;
import com.ericsson.oss.management.lcm.api.model.ImportBroRequestDto;
import com.ericsson.oss.management.lcm.api.model.PagedBackupManagersDto;
import com.ericsson.oss.management.lcm.api.model.PagedBackupsDto;
import com.ericsson.oss.management.lcm.model.entity.ActionType;
import com.ericsson.oss.management.lcm.model.entity.BroActionRequest;
import com.ericsson.oss.management.lcm.model.entity.Payload;
import com.ericsson.oss.management.lcm.presentation.exceptions.BackupAndRestoreConnectionException;
import com.ericsson.oss.management.lcm.presentation.exceptions.BackupAndRestoreException;
import com.ericsson.oss.management.lcm.presentation.routing.BroHttpClient;
import com.ericsson.oss.management.lcm.utils.pagination.CustomPageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupAndRestoreServiceImpl implements BackupAndRestoreService {

    private final BroHttpClient routingClient;

    @Value("${bro.host}")
    private String host;

    @Value("${bro.port}")
    private String port;

    private static final String HEALTH_URL = "http://%s:%s/v1/health";
    private static final String ACTION_URL = "http://%s:%s/v1/backup-manager/%s/action";
    private static final String BACKUPS_URL = "http://%s:%s/v1/backup-manager/%s/backup";
    private static final String BACKUP_MANAGERS_URL = "http://%s:%s/v1/backup-manager";
    private static final String BRO_EXCEPTION_MESSAGE = "Backup and Restore Orchestrator is unavailable right now";
    private static final String BRO_AGENTS_BUSY_MESSAGE = "BRO agents are busy. Please try to make request later";
    private static final String BRO_BODY_MESSAGE = "BRO get Backups response body is null, broManagerId is %s";
    private static final String BACKUP_MANAGERS_NOT_FOUND_MESSAGE = "BRO get all backupManagers response body is null";

    public ResponseEntity<GetHealthResponse> health() {
        ResponseEntity<GetHealthResponse> result = routingClient.executeHttpRequest(
                null,
                buildRequestUrl(HEALTH_URL, null),
                HttpMethod.GET,
                null,
                GetHealthResponse.class);
        checkBroHealthStatus(result);
        return result;
    }

    @Override
    public BroResponseActionDto createBackup(BroRequestDto request) {
        health();
        String backupManagerId = defineBackupManagerId(request.getBackupManagerId());
        var broActionRequest = getBroActionRequest(request.getBackupName(), null,
                null, ActionType.CREATE_BACKUP);
        return executeRequest(createHeaders(MediaType.APPLICATION_JSON),
                buildRequestUrl(ACTION_URL, backupManagerId),
                HttpMethod.POST,
                broActionRequest,
                BroResponseActionDto.class);
    }

    @Override
    public void deleteBackup(BroRequestDto request) {
        health();
        String backupManagerId = defineBackupManagerId(request.getBackupManagerId());
        var broActionRequest = getBroActionRequest(request.getBackupName(), null,
                null, ActionType.DELETE_BACKUP);
        executeRequest(createHeaders(MediaType.APPLICATION_JSON),
                buildRequestUrl(ACTION_URL, backupManagerId),
                HttpMethod.POST,
                broActionRequest,
                BroResponseActionDto.class);
    }

    @Override
    public BroResponseActionDto restoreBackup(BroRequestDto request) {
        health();
        String backupManagerId = defineBackupManagerId(request.getBackupManagerId());
        var broActionRequest = getBroActionRequest(request.getBackupName(), null,
               null, ActionType.RESTORE);
        return executeRequest(createHeaders(MediaType.APPLICATION_JSON),
                buildRequestUrl(ACTION_URL, backupManagerId),
                HttpMethod.POST,
                broActionRequest,
                BroResponseActionDto.class);
    }

    @Override
    public BroResponseActionDto importBackup(ImportBroRequestDto request) {
        health();
        String backupManagerId = defineBackupManagerId(request.getBackupManagerId());
        var broActionRequest = getBroActionRequest(null, request.getUri(),
                request.getPassword(), ActionType.IMPORT);
        return executeRequest(createHeaders(MediaType.APPLICATION_JSON),
                buildRequestUrl(ACTION_URL, backupManagerId),
                HttpMethod.POST,
                broActionRequest,
                BroResponseActionDto.class);
    }

    @Override
    public BroResponseActionDto exportBackup(ExportBroRequestDto request) {
        health();
        String backupManagerId = defineBackupManagerId(request.getBackupManagerId());
        var broActionRequest = getBroActionRequest(request.getBackupName(), request.getUri(),
                request.getPassword(), ActionType.EXPORT);
        return executeRequest(createHeaders(MediaType.APPLICATION_JSON),
                buildRequestUrl(ACTION_URL, backupManagerId),
                HttpMethod.POST,
                broActionRequest,
                BroResponseActionDto.class);
    }

    @Override
    public PagedBackupsDto getPagedBackups(String backupManagerId, Integer page, Integer size, List<String> sort) {
        health();
        var pageable = createPageable(page, size, CustomPageRequest.of(BACKUP_ID));
        List<BackupDto> backups = getBackups(backupManagerId);
        List<BackupDto> sortedBackups = sort == null ? backups : parseSortBackupDto(backups, sort, SORT_COLUMNS);
        Page<BackupDto> pagedBackups = new PageImpl<>(sortedBackups, pageable, backups.size());
        return new PagedBackupsDto()
                .page(buildPaginationInfo(pagedBackups))
                .links(buildLinks(pagedBackups))
                .content(pagedBackups.getContent());
    }

    @Override
    public PagedBackupManagersDto getPagedBackupManagers(Integer page, Integer size, List<String> sort) {
        health();
        var pageable = createPageable(page, size, CustomPageRequest.of(BACKUP_ID));
        List<BackupManagerDto> backupManagers = getBackupManagers();
        List<BackupManagerDto> sortedBackupManagers = sort == null ?
                backupManagers : parseSortBackupManagersDto(backupManagers, sort, BACKUP_MANAGERS_SORT_COLUMNS);
        Page<BackupManagerDto> pagedManagers = new PageImpl<>(sortedBackupManagers, pageable, backupManagers.size());
        return new PagedBackupManagersDto()
                .page(buildPaginationInfo(pagedManagers))
                .links(buildLinks(pagedManagers))
                .content(pagedManagers.getContent());
    }

    private List<BackupDto> getBackups(String backupManagerId) {
        ResponseEntity<GetBackupsResponse> result = routingClient.executeHttpRequest(
                new HttpHeaders(),
                buildRequestUrl(BACKUPS_URL, backupManagerId),
                HttpMethod.GET,
                null,
                GetBackupsResponse.class);
        return Optional.ofNullable(result.getBody())
                .map(GetBackupsResponse::getBackups)
                .orElseThrow(() -> new BackupAndRestoreException((String.format(BRO_BODY_MESSAGE, backupManagerId))));
    }

    private List<BackupManagerDto> getBackupManagers() {
        ResponseEntity<GetBackupManagersResponse> result = routingClient.executeHttpRequest(
                null,
                buildRequestUrl(BACKUP_MANAGERS_URL, null),
                HttpMethod.GET,
                null,
                GetBackupManagersResponse.class);
        return Optional.ofNullable(result.getBody())
                .map(GetBackupManagersResponse::getBackupManagers)
                .orElseThrow(() -> new BackupAndRestoreException(BACKUP_MANAGERS_NOT_FOUND_MESSAGE));
    }

    private void checkBroHealthStatus(ResponseEntity<GetHealthResponse> response) {
        log.info("Checking BRO health status");
        if (response.getStatusCode().equals(HttpStatus.OK) && STATUS_HEALTHY.equals(response.getBody().getStatus())) {
            checkAvailability(response);
        } else {
            throw new BackupAndRestoreConnectionException(BRO_EXCEPTION_MESSAGE);
        }
    }

    private void checkAvailability(ResponseEntity<GetHealthResponse> response) {
        log.info("Checking BRO agents availability");
        var availability = response.getBody().getAvailability();
        if (BUSY.toString().equals(availability)) {
            throw new BackupAndRestoreConnectionException(BRO_AGENTS_BUSY_MESSAGE);
        }
    }

    private String buildRequestUrl(String baseUrl, String backupManagerId) {
        if (backupManagerId != null) {
            return String.format(baseUrl, host, port, backupManagerId);
        } else {
            return String.format(baseUrl, host, port);
        }
    }

    private HttpHeaders createHeaders(MediaType contentType) {
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, contentType.toString());
        return headers;
    }

    private BroActionRequest getBroActionRequest(String backupName, String uri, String password, ActionType action) {
        return BroActionRequest.builder()
                .action(action.toString())
                .payload(getPayload(backupName, uri, password))
                .build();
    }

    private Payload getPayload(String backupName, String uri, String password) {
        return Payload.builder()
                .backupName(backupName)
                .uri(uri)
                .password(password)
                .build();
    }


    private <T, V> T executeRequest(HttpHeaders headers, String url, HttpMethod method,
                                    V requestBody, Class<T> responseDtoClass) {
        return routingClient
                .executeHttpRequest(headers, url, method, requestBody, responseDtoClass)
                .getBody();
    }

    private String defineBackupManagerId(String backupManagerId) {
        return Optional.ofNullable(backupManagerId).orElse(DEFAULT_BACKUP_MANAGER_ID);
    }
}