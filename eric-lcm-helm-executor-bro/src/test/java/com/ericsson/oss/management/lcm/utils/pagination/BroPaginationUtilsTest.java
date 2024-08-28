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

package com.ericsson.oss.management.lcm.utils.pagination;

import static com.ericsson.oss.management.lcm.constants.BackupAndRestoreConstants.BACKUP_MANAGERS_SORT_COLUMNS;
import static com.ericsson.oss.management.lcm.constants.BackupAndRestoreConstants.SORT_COLUMNS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.ericsson.oss.management.lcm.api.model.BackupDto;
import com.ericsson.oss.management.lcm.api.model.BackupManagerDto;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidPaginationQueryException;

class BroPaginationUtilsTest {

    private static final String BACKUP_ID = "id";
    private static final String BACKUP_STATUS = "status";
    private static final String DESC_DIRECTION = "desc";
    private static final String ASC_DIRECTION = "asc";
    private static final String NAME_COLUMN = "name";
    private static final String ONE_ID = "1";
    private static final String TWO_ID = "2";
    private static final Integer SIZE_TWO = 2;
    private static final String FIRST_TYPE = "firstType";
    private static final String SECOND_TYPE = "secondType";
    private static final String FIRST_DOMAIN = "firstDomain";
    private static final String SECOND_DOMAIN = "secondDomain";

    @Test
    void shouldParseSingleExpressionBackupsDesc() {
        List<String> backupSortAndDirection = List.of(BACKUP_STATUS, DESC_DIRECTION);

        List<BackupDto> backupDtos = BroPaginationUtils.parseSortBackupDto(getBackupList(), backupSortAndDirection, SORT_COLUMNS);
        assertThat(backupDtos.size()).isEqualTo(SIZE_TWO);
        assertThat(backupDtos.get(0).getStatus()).isEqualTo(BackupDto.StatusEnum.INCOMPLETE);
        assertThat(backupDtos.get(1).getStatus()).isEqualTo(BackupDto.StatusEnum.COMPLETE);
    }

    @Test
    void shouldParseSingleExpressionBackupsAsc() {
        List<String> backupSortAndDirection = List.of(BACKUP_STATUS, ASC_DIRECTION);

        List<BackupDto> backupDtos = BroPaginationUtils.parseSortBackupDto(getBackupList(), backupSortAndDirection, SORT_COLUMNS);
        assertThat(backupDtos.size()).isEqualTo(SIZE_TWO);
        assertThat(backupDtos.get(1).getStatus()).isEqualTo(BackupDto.StatusEnum.INCOMPLETE);
        assertThat(backupDtos.get(0).getStatus()).isEqualTo(BackupDto.StatusEnum.COMPLETE);
    }

    @Test
    void shouldParseSingleExpressionBackupManagersAsc() {
        List<String> backupManagerSortAndDirection = List.of(BACKUP_ID, ASC_DIRECTION);

        List<BackupManagerDto> backupDtos = BroPaginationUtils.parseSortBackupManagersDto(getBackupManagersList(),
                backupManagerSortAndDirection, BACKUP_MANAGERS_SORT_COLUMNS);
        assertThat(backupDtos.size()).isEqualTo(SIZE_TWO);
        assertThat(backupDtos.get(0).getId()).isEqualTo(ONE_ID);
        assertThat(backupDtos.get(1).getId()).isEqualTo(TWO_ID);
    }

    @Test
    void shouldParseSingleExpressionBackupManagersDesc() {
        List<String> backupManagerSortAndDirection = List.of(BACKUP_ID, DESC_DIRECTION);

        List<BackupManagerDto> backupDtos = BroPaginationUtils.parseSortBackupManagersDto(getBackupManagersList(),
                backupManagerSortAndDirection, BACKUP_MANAGERS_SORT_COLUMNS);
        assertThat(backupDtos.size()).isEqualTo(SIZE_TWO);
        assertThat(backupDtos.get(1).getId()).isEqualTo(ONE_ID);
        assertThat(backupDtos.get(0).getId()).isEqualTo(TWO_ID);
    }

    @Test
    void sortShouldFailWithInvalidParameters() {
        assertThat(shouldThrowException(List.of("invalid0", "invalid1")))
                .isInstanceOf(InvalidPaginationQueryException.class)
                .hasMessageContaining("Invalid column value for sorting");

        assertThat(shouldThrowException(List.of("id,ascending", "status")))
                .isInstanceOf(InvalidPaginationQueryException.class)
                .hasMessageContaining("Invalid sorting values :: ascending.");

        assertThat(shouldThrowException(List.of(BACKUP_ID, "status,descending")))
                .isInstanceOf(InvalidPaginationQueryException.class)
                .hasMessageContaining("Invalid sorting values :: descending.");
    }

    private Throwable shouldThrowException(List<String> sort) {
        Set<String> validColumns = Set.of(BACKUP_ID, NAME_COLUMN);
        return catchThrowable(() -> BroPaginationUtils
                .parseSortBackupDto(getBackupList(), sort, validColumns));
    }

    private List<BackupDto> getBackupList() {
        List<BackupDto> backupDtoList = new ArrayList<>();
        backupDtoList.add(new BackupDto().id(ONE_ID).status(BackupDto.StatusEnum.COMPLETE).creationTime("10:05:2022"));
        backupDtoList.add(new BackupDto().id(TWO_ID).status(BackupDto.StatusEnum.INCOMPLETE).creationTime("01:05:2022"));
        return backupDtoList;
    }

    private List<BackupManagerDto> getBackupManagersList() {
        List<BackupManagerDto> managersDto = new ArrayList<>();
        managersDto.add(new BackupManagerDto().id(ONE_ID).backupType(FIRST_TYPE).backupDomain(FIRST_DOMAIN));
        managersDto.add(new BackupManagerDto().id(TWO_ID).backupType(SECOND_TYPE).backupDomain(SECOND_DOMAIN));
        return managersDto;
    }
}
