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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidPaginationQueryException;

class PaginationUtilsTest {

    private static final String WORKLOAD_INSTANCE_ID_COLUMN = "workloadInstanceId";
    private static final String WORKLOAD_INSTANCE_ID_DESC = "workloadInstanceId,desc";
    private static final String DESC_DIRECTION = "desc";
    private static final String STATE_COLUMN = "state";
    private static final String STATE_DESC = "state,desc";

    @Test
    void shouldParseSingleExpression() {
        List<String> instanceIdDefaultDirection = List.of(WORKLOAD_INSTANCE_ID_COLUMN);
        List<String> instanceIdWithDirection = List.of(WORKLOAD_INSTANCE_ID_COLUMN, DESC_DIRECTION);
        List<String> instanceIdWithEmptyDirection = List.of(WORKLOAD_INSTANCE_ID_COLUMN + ",");
        Set<String> validColumns = Set.of(WORKLOAD_INSTANCE_ID_COLUMN, STATE_COLUMN);

        Sort sort = PaginationUtils.parseSort(instanceIdDefaultDirection, validColumns);
        checkSingleOrderSort(sort, Sort.Direction.ASC);

        sort = PaginationUtils.parseSort(instanceIdWithDirection, validColumns);
        checkSingleOrderSort(sort, Sort.Direction.DESC);

        sort = PaginationUtils.parseSort(instanceIdWithEmptyDirection, validColumns);
        checkSingleOrderSort(sort, Sort.Direction.ASC);
    }

    @Test
    void shouldParseMultipleParameters() {
        Set<String> validColumns = Set.of(WORKLOAD_INSTANCE_ID_COLUMN, STATE_COLUMN);
        Sort sort1 = PaginationUtils.parseSort(List.of(WORKLOAD_INSTANCE_ID_COLUMN, STATE_DESC), validColumns);
        assertThat(sort1.isSorted()).isTrue();
        List<Sort.Order> orders1 = sort1.toList();
        assertThat(orders1.size()).isEqualTo(2);
        assertThat(orders1.get(0).getProperty()).isEqualTo(WORKLOAD_INSTANCE_ID_COLUMN);
        assertThat(orders1.get(0).getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(orders1.get(1).getProperty()).isEqualTo(STATE_COLUMN);
        assertThat(orders1.get(1).getDirection()).isEqualTo(Sort.Direction.DESC);

        Sort sort2 = PaginationUtils.parseSort(List.of(WORKLOAD_INSTANCE_ID_DESC, STATE_COLUMN), validColumns);
        assertThat(sort2.isSorted()).isTrue();
        List<Sort.Order> orders2 = sort2.toList();
        assertThat(orders2.size()).isEqualTo(2);
        assertThat(orders2.get(0).getProperty()).isEqualTo(WORKLOAD_INSTANCE_ID_COLUMN);
        assertThat(orders2.get(0).getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(orders2.get(1).getProperty()).isEqualTo(STATE_COLUMN);
        assertThat(orders2.get(1).getDirection()).isEqualTo(Sort.Direction.ASC);

        Sort sort3 = PaginationUtils.parseSort(List.of(WORKLOAD_INSTANCE_ID_COLUMN, STATE_COLUMN), validColumns);
        assertThat(sort3.isSorted()).isTrue();
        List<Sort.Order> orders3 = sort3.toList();
        assertThat(orders3.size()).isEqualTo(2);
        assertThat(orders3.get(0).getProperty()).isEqualTo(WORKLOAD_INSTANCE_ID_COLUMN);
        assertThat(orders3.get(0).getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(orders3.get(1).getProperty()).isEqualTo(STATE_COLUMN);
        assertThat(orders3.get(1).getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void sortShouldFailWithInvalidParameters() {
        assertThat(shouldThrowException(List.of("invalid0", "invalid1")))
                .isInstanceOf(InvalidPaginationQueryException.class).hasMessageContaining("Invalid column value for sorting");
        assertThat(shouldThrowException(List.of("workloadInstanceId,ascending", "state")))
                .isInstanceOf(InvalidPaginationQueryException.class).hasMessageContaining("Invalid sorting values :: ascending.");
        assertThat(shouldThrowException(List.of(WORKLOAD_INSTANCE_ID_COLUMN, "state,descending")))
                .isInstanceOf(InvalidPaginationQueryException.class).hasMessageContaining("Invalid sorting values :: descending.");
    }

    private Throwable shouldThrowException(List<String> sort) {
        Set<String> validColumns = Set.of(WORKLOAD_INSTANCE_ID_COLUMN, STATE_COLUMN);
        return catchThrowable(() -> PaginationUtils
                .parseSort(sort, validColumns));
    }

    private static void checkSingleOrderSort(Sort sort, Sort.Direction direction) {
        assertThat(sort.isSorted()).isTrue();
        List<Sort.Order> orders = sort.toList();
        assertThat(orders.size()).isEqualTo(1);
        assertThat(orders.get(0).getProperty()).isEqualTo(WORKLOAD_INSTANCE_ID_COLUMN);
        assertThat(orders.get(0).getDirection()).isEqualTo(direction);
    }
}
