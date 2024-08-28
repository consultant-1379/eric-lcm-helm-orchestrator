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

package com.ericsson.oss.management.lcm.utils.sorting;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ericsson.oss.management.lcm.model.internal.Chart;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidInputException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSort {

    /**
     * Sort order for charts in a correct way
     *
     * @param charts    list with all releases
     * @param predicate sort with crd or without crd
     * @return sorted releases
     */
    public List<Chart> sortedRelease(List<Chart> charts, Predicate<Chart> predicate) {
        log.info("Sort order for charts");
        List<Chart> filteredCharts = charts.stream()
                .filter(predicate)
                .collect(Collectors.toList());
        return sortCharts(filteredCharts);
    }

    private List<Chart> doSortChartsWithNeeds(List<Chart> charts) {
        Map<Integer, List<Chart>> unsortedChartsWithDuplicateOrder = charts.stream()
                .collect(Collectors.groupingBy(Chart::getOrder));
        NavigableMap<Integer, List<Chart>> sortedChartsWithDuplicateOrder = new TreeMap<>(unsortedChartsWithDuplicateOrder);
        putNeeds(sortedChartsWithDuplicateOrder);
        return charts;
    }

    private void putNeeds(final NavigableMap<Integer, List<Chart>> sortedChartsWithDuplicateOrder) {
        Map.Entry<Integer, List<Chart>> firstChart = sortedChartsWithDuplicateOrder.firstEntry();

        Integer firstOrder = firstChart.getKey();
        Set<String> chartNames = null;
        for (var orderWithCharts : sortedChartsWithDuplicateOrder.entrySet()) {
            if (!orderWithCharts.getKey().equals(firstOrder)) {
                List<Chart> charts = orderWithCharts.getValue();
                for (Chart chart : charts) {
                    chart.setNeeds(chartNames);
                }
            }
            chartNames = orderWithCharts.getValue().stream()
                    .map(Chart::getName)
                    .collect(Collectors.toSet());
        }
    }

    private List<Chart> sortCharts(List<Chart> charts) {
        long chartsWithOrder = charts.stream().filter(item -> item.getOrder() != null).count();
        if (chartsWithOrder == 0) {
            return charts;
        } else if (charts.size() == chartsWithOrder) {
            return doSortChartsWithNeeds(charts);
        } else {
            throw new InvalidInputException("You can set order for each release or don't set anywhere, "
                                                    + "setting order partially is forbidden!");
        }
    }
}
