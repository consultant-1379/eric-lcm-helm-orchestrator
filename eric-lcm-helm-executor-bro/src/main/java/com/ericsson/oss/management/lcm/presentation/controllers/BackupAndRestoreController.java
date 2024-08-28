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

import com.ericsson.oss.management.lcm.api.BackupAndRestoreApi;
import com.ericsson.oss.management.lcm.api.model.BroResponseActionDto;
import com.ericsson.oss.management.lcm.api.model.BroRequestDto;
import com.ericsson.oss.management.lcm.api.model.ExportBroRequestDto;
import com.ericsson.oss.management.lcm.api.model.GetHealthResponse;
import com.ericsson.oss.management.lcm.api.model.PagedBackupManagersDto;
import com.ericsson.oss.management.lcm.api.model.PagedBackupsDto;
import com.ericsson.oss.management.lcm.api.model.ImportBroRequestDto;
import com.ericsson.oss.management.lcm.presentation.services.BackupAndRestoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/cnwlcm/v1")
@RequiredArgsConstructor
public class BackupAndRestoreController implements BackupAndRestoreApi {

    private final BackupAndRestoreService backupAndRestoreService;

    @Override
    public ResponseEntity<GetHealthResponse> backupAndRestoreHealthGet() {
        log.info("Received GET request to BRO health");
        return backupAndRestoreService.health();
    }

    @Override
    public ResponseEntity<BroResponseActionDto> backupAndRestoreImportsPost(
            @Valid ImportBroRequestDto importBroRequestDto) {
        log.info("Received POST request to import backup");
        BroResponseActionDto actionDto = backupAndRestoreService.importBackup(importBroRequestDto);
        return new ResponseEntity<>(actionDto, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<BroResponseActionDto> backupAndRestorePost(
            @Valid BroRequestDto broRequestDto) {
        log.info("Received POST request to create a backup");
        BroResponseActionDto actionDto = backupAndRestoreService.createBackup(broRequestDto);
        return new ResponseEntity<>(actionDto, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<BroResponseActionDto> backupAndRestorePut(
            @Valid BroRequestDto broRequestDto) {
        log.info("Received PUT request to restore a backup");
        BroResponseActionDto actionDto = backupAndRestoreService.restoreBackup(broRequestDto);
        return new ResponseEntity<>(actionDto, HttpStatus.ACCEPTED);
    }

    @Override
    public ResponseEntity<Void> backupAndRestoreDelete(
            @Valid BroRequestDto broRequestDto) {
        log.info("Received DELETE request to delete a backup");
        backupAndRestoreService.deleteBackup(broRequestDto);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<BroResponseActionDto> backupAndRestoreExportsPost(@Valid ExportBroRequestDto exportBroRequestDto) {
        log.info("Received POST request to export a backup");
        BroResponseActionDto actionDto = backupAndRestoreService.exportBackup(exportBroRequestDto);
        return new ResponseEntity<>(actionDto, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<PagedBackupsDto> backupAndRestoreBackupManagerIdGet(
            String backupManagerId, @Valid Integer page, @Valid Integer size, @Valid List<String> sort) {
        log.info("Received a GET request to get all paged backups by backupManagerId {}", backupManagerId);
        PagedBackupsDto response = backupAndRestoreService.getPagedBackups(backupManagerId, page, size, sort);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<PagedBackupManagersDto> backupAndRestoreManagersGet(
            @Valid Integer page, @Valid Integer size, @Valid List<String> sort) {
        log.info("Received GET request to get all backupManagers");
        PagedBackupManagersDto response = backupAndRestoreService.getPagedBackupManagers(page, size, sort);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
