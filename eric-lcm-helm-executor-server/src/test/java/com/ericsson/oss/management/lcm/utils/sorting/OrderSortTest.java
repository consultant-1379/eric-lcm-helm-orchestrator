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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ericsson.oss.management.lcm.model.internal.Chart;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidInputException;

@SpringBootTest(classes = { OrderSort.class })
class OrderSortTest {
    @Autowired
    private OrderSort orderSort;

    private static final String VERSION = "1.2.3-89";

    @Test
    void shouldSortedReleasesWithoutOrders() {
        //prepare
        List<Chart> charts = getCharts(false, "release-A", "release-B");
        charts.get(0).setOrder(null);
        charts.get(1).setOrder(null);

        //when
        orderSort.sortedRelease(charts, item -> !item.isCrd());

        //than
        assertThat(charts.get(0).getNeeds()).isNull();
        assertThat(charts.get(1).getNeeds()).isNull();
    }

    @Test
    void shouldSortedReleasesWithoutOrdersAndWithCrd() {
        //prepare
        List<Chart> charts = getCharts(true, "crd-release-1", "crd-release-2");
        charts.get(0).setOrder(null);
        charts.get(1).setOrder(null);

        //when
        orderSort.sortedRelease(charts, Chart::isCrd);

        //than
        assertThat(charts.get(0).getNeeds()).isNull();
        assertThat(charts.get(1).getNeeds()).isNull();
    }

    @Test
    void shouldSortedReleasesWithOrders() {
        //prepare
        List<Chart> charts = getCharts(false, "release-A", "release-B", "release-C", "release-D", "release-E");
        setOrder(charts, 1, 2, 3, 4, 5);

        //when
        orderSort.sortedRelease(charts, item -> !item.isCrd());

        //then
        assertThat(charts.get(0).getNeeds()).isNull();
        assertThat(charts.get(1).getNeeds().toString()).isEqualTo("[release-A]");
        assertThat(charts.get(2).getNeeds().toString()).isEqualTo("[release-B]");
        assertThat(charts.get(3).getNeeds().toString()).isEqualTo("[release-C]");
        assertThat(charts.get(4).getNeeds().toString()).isEqualTo("[release-D]");
    }

    @Test
    void shouldSortedReleasesWithOrdersAndCrd() {
        //prepare
        List<Chart> chartsWithCrd = getCharts(true, "crd-release-A", "crd-release-B", "crd-release-C", "crd-release-D", "crd-release-E");
        setOrder(chartsWithCrd, 1, 2, 3, 4, 5);

        //when
        orderSort.sortedRelease(chartsWithCrd, Chart::isCrd);

        //then
        assertThat(chartsWithCrd.get(0).getNeeds()).isNull();
        assertThat(chartsWithCrd.get(1).getNeeds().toString()).isEqualTo("[crd-release-A]");
        assertThat(chartsWithCrd.get(2).getNeeds().toString()).isEqualTo("[crd-release-B]");
        assertThat(chartsWithCrd.get(3).getNeeds().toString()).isEqualTo("[crd-release-C]");
        assertThat(chartsWithCrd.get(4).getNeeds().toString()).isEqualTo("[crd-release-D]");
    }

    @Test
    void shouldSortedReleasesWithOrdersAndReleaseWithCrd() {
        //prepare
        List<Chart> chartsWithoutCrd = getCharts(false, "release-A", "release-B", "release-C", "release-D", "release-E");
        setOrder(chartsWithoutCrd, 1, 2, 3, 4, 5);
        List<Chart> chartsWithCrd = getCharts(true, "crd-release-A", "crd-release-B", "crd-release-C", "crd-release-D", "crd-release-E");
        setOrder(chartsWithCrd, 1, 2, 3, 4, 5);

        List<Chart> charts = Stream.concat(chartsWithoutCrd.stream(), chartsWithCrd.stream()).collect(Collectors.toList());

        //when
        List<Chart> sortedChartsWithCrd = orderSort.sortedRelease(charts, Chart::isCrd);
        List<Chart> sortedChartsWithoutCrd = orderSort.sortedRelease(charts, item -> !item.isCrd());

        //then
        assertThat(sortedChartsWithoutCrd.get(0).getNeeds()).isNull();
        assertThat(sortedChartsWithoutCrd.get(1).getNeeds().toString()).isEqualTo("[release-A]");
        assertThat(sortedChartsWithoutCrd.get(2).getNeeds().toString()).isEqualTo("[release-B]");
        assertThat(sortedChartsWithoutCrd.get(3).getNeeds().toString()).isEqualTo("[release-C]");
        assertThat(sortedChartsWithoutCrd.get(4).getNeeds().toString()).isEqualTo("[release-D]");

        assertThat(sortedChartsWithCrd.get(0).getNeeds()).isNull();
        assertThat(sortedChartsWithCrd.get(1).getNeeds().toString()).isEqualTo("[crd-release-A]");
        assertThat(sortedChartsWithCrd.get(2).getNeeds().toString()).isEqualTo("[crd-release-B]");
        assertThat(sortedChartsWithCrd.get(3).getNeeds().toString()).isEqualTo("[crd-release-C]");
        assertThat(sortedChartsWithCrd.get(4).getNeeds().toString()).isEqualTo("[crd-release-D]");
    }

    @Test
    void shouldSortedWithSameOrders() {
        //prepare
        List<Chart> charts = getCharts(false, "release-A", "release-B", "release-C", "release-D", "release-E");
        setOrder(charts, 0, 0, 1, 2, 3);

        //when
        orderSort.sortedRelease(charts, item -> !item.isCrd());

        //than
        assertThat(charts.get(0).getNeeds()).isNull();
        assertThat(charts.get(1).getNeeds()).isNull();
        assertThat(charts.get(2).getNeeds().toString()).isEqualTo("[release-B, release-A]");
        assertThat(charts.get(3).getNeeds().toString()).isEqualTo("[release-C]");
        assertThat(charts.get(4).getNeeds().toString()).isEqualTo("[release-D]");
    }

    @Test
    void shouldSortedWithSameOrdersAndWithCrd() {
        //prepare
        List<Chart> charts = getCharts(true, "crd-release-A", "crd-release-B", "crd-release-C", "crd-release-D", "crd-release-E");
        setOrder(charts, 0, 0, 1, 2, 3);

        //when
        orderSort.sortedRelease(charts, Chart::isCrd);

        //than
        assertThat(charts.get(0).getNeeds()).isNull();
        assertThat(charts.get(1).getNeeds()).isNull();
        assertThat(charts.get(2).getNeeds().toString()).isEqualTo("[crd-release-B, crd-release-A]");
        assertThat(charts.get(3).getNeeds().toString()).isEqualTo("[crd-release-C]");
        assertThat(charts.get(4).getNeeds().toString()).isEqualTo("[crd-release-D]");
    }

    @Test
    void shouldSortedNotSequenceOrder() {
        //prepare
        List<Chart> charts = getCharts(false, "release-A", "release-B", "release-C", "release-D", "release-E");
        setOrder(charts, 1, 2, 100, 2, 3);

        //when
        List<Chart> chartsWithoutCrd = orderSort.sortedRelease(charts, item -> !item.isCrd());

        //than
        assertThat(chartsWithoutCrd.get(0).getName()).isEqualTo("release-A");
        assertThat(chartsWithoutCrd.get(1).getName()).isEqualTo("release-B");
        assertThat(chartsWithoutCrd.get(2).getName()).isEqualTo("release-C");
        assertThat(chartsWithoutCrd.get(3).getName()).isEqualTo("release-D");
        assertThat(chartsWithoutCrd.get(4).getName()).isEqualTo("release-E");

        assertThat(chartsWithoutCrd.get(0).getNeeds()).isNull();
        assertThat(chartsWithoutCrd.get(1).getNeeds().toString()).isEqualTo("[release-A]");
        assertThat(chartsWithoutCrd.get(2).getNeeds().toString()).isEqualTo("[release-E]");
        assertThat(chartsWithoutCrd.get(3).getNeeds().toString()).isEqualTo("[release-A]");
        assertThat(chartsWithoutCrd.get(4).getNeeds().toString()).isEqualTo("[release-D, release-B]");
    }

    @Test
    void shouldFailWhenChartsNull() {
        //prepare
        List<Chart> charts = getCharts(false, "release-A", "release-B", "release-C", "release-D", "release-E");
        charts.get(0).setOrder(1);
        charts.get(1).setOrder(null);
        charts.get(2).setOrder(2);
        charts.get(3).setOrder(3);
        charts.get(3).setOrder(null);

        //when
        assertThatThrownBy(() -> orderSort.sortedRelease(charts, item -> !item.isCrd())).isInstanceOf(InvalidInputException.class);
    }

    @Test
    @Timeout(5)
    void shouldTimeoutOfSortedCharts() {
        List<Chart> charts = getGenerateChartWithNameAndOrder();
        List<Chart> result = orderSort.sortedRelease(charts, Chart::isCrd);
        assertThat(result).isNotNull();
    }

    private List<Chart> getGenerateChartWithNameAndOrder() {
        List<Chart> charts = new ArrayList<>();
        int chartSize = 300;
        for (int i = 0; i < chartSize; i++) {
            String name = RandomStringUtils.randomAlphabetic(10);
            int minOrder = 0;
            int maxOrder = 1000;
            int order = (int) (minOrder + (Math.random() * ((maxOrder - minOrder) + 1)));
            charts.add(new Chart(order, name, VERSION, true, null));
        }
        return charts;
    }

    private Chart getUnorderedChart(boolean isCrd, String name) {
        Chart chart = new Chart();
        chart.setOrder(null);
        chart.setName(name);
        chart.setCrd(isCrd);
        chart.setVersion(VERSION);
        chart.setNeeds(null);
        return chart;
    }

    private List<Chart> getCharts(boolean isCrd, String... names) {
        return Stream.of(names)
                .map(item -> getUnorderedChart(isCrd, item))
                .collect(Collectors.toList());
    }

    private void setOrder(List<Chart> charts, int orderReleaseA, int orderReleaseB, int orderReleaseC, int orderReleaseD, int orderReleaseE) {
        charts.get(0).setOrder(orderReleaseA);
        charts.get(1).setOrder(orderReleaseB);
        charts.get(2).setOrder(orderReleaseC);
        charts.get(3).setOrder(orderReleaseD);
        charts.get(4).setOrder(orderReleaseE);
    }
}