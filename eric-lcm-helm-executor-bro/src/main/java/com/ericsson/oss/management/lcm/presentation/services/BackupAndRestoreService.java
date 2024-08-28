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

import com.ericsson.oss.management.lcm.api.model.BroRequestDto;
import com.ericsson.oss.management.lcm.api.model.BroResponseActionDto;
import com.ericsson.oss.management.lcm.api.model.ExportBroRequestDto;
import com.ericsson.oss.management.lcm.api.model.GetHealthResponse;
import com.ericsson.oss.management.lcm.api.model.PagedBackupManagersDto;
import com.ericsson.oss.management.lcm.api.model.PagedBackupsDto;
import com.ericsson.oss.management.lcm.api.model.ImportBroRequestDto;

import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Working with BRO service
 */
public interface BackupAndRestoreService {

    /**
     * Get health check of BRO
     *
     * @return response from BRO with information about Health and Availability of service
     */
    ResponseEntity<GetHealthResponse> health();

    /**
     * Create backup on BRO
     * @param request object with managerId and backupName (BroRequestDto)
     * @return BackupAndRestoreActionDto
     */
    BroResponseActionDto createBackup(BroRequestDto request);

    /**
     * Delete backup on BRO
     * @param request object with managerId and backupName (BroRequestDto)
     */
    void deleteBackup(BroRequestDto request);

    /**
     * Import backup to BRO
     * @param request object with uri and password
     * @return BroResponseActionDto
     */
    BroResponseActionDto importBackup(ImportBroRequestDto request);

    /**
     * Export backup from BRO
     * @param request that provides information about "backupName",
     *                "backupManagerId", "uri" and "password"
     * @return BackupAndRestoreActionDto
     */
    BroResponseActionDto exportBackup(ExportBroRequestDto request);

    /**
     * Get Paged backups by page, size and sort parameters searched by backupManagerId
     * @param backupManagerId
     * @param page
     * @param size
     * @param sort
     * @return PagedOperationDto
     */
    PagedBackupsDto getPagedBackups(String backupManagerId, Integer page, Integer size, List<String> sort);

    /**
     * Get Paged backupManagers by page, size and sort parameters
     * @param page
     * @param size
     * @param sort
     * @return PagedOBackupManagerDto
     */
    PagedBackupManagersDto getPagedBackupManagers(Integer page, Integer size, List<String> sort);

    /**
     * Restore backup
     * @param request object with managerId and backupName (BroRequestDto)
     * @return BroResponseActionDto
     */
    BroResponseActionDto restoreBackup(BroRequestDto request);

}
