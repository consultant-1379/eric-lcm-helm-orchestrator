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

package com.ericsson.oss.mgmt.lcm.acceptance.tests;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.BRO_RESPONSE_ID;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import com.ericsson.oss.mgmt.lcm.acceptance.models.BRORequest;
import com.ericsson.oss.mgmt.lcm.acceptance.models.PagedBackup;
import com.ericsson.oss.mgmt.lcm.acceptance.models.PagedBackupManager;
import com.ericsson.oss.mgmt.lcm.acceptance.steps.BROOperations;
import com.ericsson.oss.mgmt.lcm.acceptance.steps.HealthCheck;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BackupAndRestoreTest {

    public static final String MANAGER_ID = "default-db";
    public static final String BACKUP_NAME = generateRandomName();
    public static final String EXPORT_URI = "sftp://ericsson@bur-sftp-svc.sftp:22/eso/";
    public static final String IMPORT_URI = "sftp://ericsson@bur-sftp-svc.sftp:22/eso/default-db/" + BACKUP_NAME;
    public static final String CRED = "ericsson";
    public static final String COMPLETED_STATUS = "COMPLETE";

    @Ignore("Temporarily ignored")
    @Test(description = "Process backup and restore operations")
    public void shouldSuccessfullyProcessBackupAndRestoreOperations() {
        ResponseEntity<String> healthResponse = HealthCheck.getBROHealth();
        if (healthResponse.getStatusCode() == HttpStatus.OK) {
            verifyHealthResponseOk(healthResponse);
            // get and verify managers
            PagedBackupManager backupManagers = BROOperations.getBackupManagers();
            verifyManagers(backupManagers);
            // create backup
            BRORequest request = BRORequest.builder()
                    .backupManagerId(MANAGER_ID)
                    .backupName(BACKUP_NAME)
                    .uri(EXPORT_URI)
                    .password(CRED)
                    .build();

            ResponseEntity<HashMap<String, Object>> responseCreate = BROOperations.create(request);
            verifyResponseStatus(responseCreate);

            // get created backup and verify
            PagedBackup response = BROOperations.getBackup(MANAGER_ID);
            verifyResponse(response);

            // export backup
            ResponseEntity<HashMap<String, Object>> responseExtractBackup = BROOperations.exportBackup(request);
            verifyResponseStatus(responseExtractBackup);

            // delete backup and verify if it's successful
            BROOperations.delete(request);
            response = BROOperations.getBackup(MANAGER_ID);
            assertThat(response.getContent()).isEmpty();
            // import backup
            request.setUri(IMPORT_URI);
            BROOperations.importBackup(request);
            response = BROOperations.getBackup(MANAGER_ID);
            verifyResponse(response);
            // restore backup
            ResponseEntity<HashMap<String, Object>> responseRestore = BROOperations.restore(request);
            assertThat(responseRestore.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        }
    }

    private static String generateRandomName() {
        return UUID.randomUUID().toString();
    }

    private void verifyResponse(PagedBackup backup) {
        assertThat(backup.getContent()).isNotNull();
        assertThat(backup.getContent().size()).isEqualTo(1);
        PagedBackup.Backup item = backup.getContent().get(0);
        assertThat(item.getId()).isEqualTo(BACKUP_NAME);
        assertThat(item.getName()).isEqualTo(BACKUP_NAME);
        assertThat(item.getStatus()).isEqualTo(COMPLETED_STATUS);
    }

    private void verifyManagers(PagedBackupManager backupManagers) {
        assertThat(backupManagers.getContent()).isNotNull();
        assertThat(backupManagers.getContent().size()).isGreaterThanOrEqualTo(4);
        List<String> managerIds = backupManagers.getContent().stream()
                .map(PagedBackupManager.BackupManager::getId)
                .collect(Collectors.toList());
        assertThat(managerIds).contains("DEFAULT", MANAGER_ID, "executor-db", "all-application-data");
    }

    private void verifyHealthResponseOk(ResponseEntity<String> healthResponse) {
        log.info("Verifying BRO is healthy");
        log.info("Health Status is: {}", healthResponse.getBody());
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(healthResponse.getBody()).contains("\"status\":\"Healthy\"");
        assertThat(healthResponse.getBody()).contains("\"availability\":\"Available\"");
    }

    private void verifyResponseStatus(ResponseEntity<HashMap<String, Object>> responseCreate) {
        HashMap<String, Object> responseBody = Optional.ofNullable(responseCreate.getBody()).orElseThrow();
        assertThat(responseCreate.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(responseBody.get(BRO_RESPONSE_ID)).isNotNull();
    }
}
