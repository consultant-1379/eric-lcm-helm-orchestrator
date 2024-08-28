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

package com.ericsson.oss.management.lcm.constants;

import java.util.Set;

public final class BackupAndRestoreConstants {
    private BackupAndRestoreConstants(){}

    public static final String BACKUP_TYPE = "backupType";
    public static final String BACKUP_STATUS = "status";
    public static final Set<String> SORT_COLUMNS = Set.of("creationTime", BACKUP_STATUS);
    public static final Set<String> BACKUP_MANAGERS_SORT_COLUMNS = Set.of("id", BACKUP_TYPE, "backupDomain");
    public static final String BACKUP_ID = "id";
    public static final String DEFAULT_BACKUP_MANAGER_ID = "DEFAULT";
    public static final String STATUS_HEALTHY = "Healthy";
}
