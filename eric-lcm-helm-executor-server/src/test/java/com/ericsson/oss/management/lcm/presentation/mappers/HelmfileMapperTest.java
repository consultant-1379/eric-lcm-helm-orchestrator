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

package com.ericsson.oss.management.lcm.presentation.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ericsson.oss.management.lcm.api.model.ChartDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithChartsPutRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithChartsRequestDto;
import com.ericsson.oss.management.lcm.model.internal.HelmfileData;

@SpringBootTest(classes = HelmfileMapperImpl.class)
class HelmfileMapperTest {

    private static final String REPOSITORY = "https://arm.sero.gic.ericsson.se/artifactory/proj-adp-gs-all-helm";
    private static final String VERSION = "1.2.3-89";
    private static final String[] RELEASES = new String[] {"release-1", "release-2", "release-3"};

    @Autowired
    private HelmfileMapper helmfileMapper;

    @Test
    void shouldMapWorkloadInstanceWithChartsRequestDtoToHelmfileData() {
        WorkloadInstanceWithChartsRequestDto request = getInstantiateRequest();

        HelmfileData result = helmfileMapper.toHelmfileData(request);

        assertThat(result).isNotNull();
        assertThat(result.getRepository()).isEqualTo(REPOSITORY);
        assertThat(result.getCharts()).isNotEmpty();
        assertThat(result.getCharts().size()).isEqualTo(3);
        assertThat(result.getCharts().get(0).getVersion()).isEqualTo(VERSION);
        assertThat(result.getCharts()).allSatisfy(chart -> assertThat(chart.getNeeds()).isNull());
        assertThat(result.getTimeout()).isNull();
    }

    @Test
    void shouldMapWorkloadInstanceWithChartsPutRequestDtoToHelmfileData() {
        WorkloadInstanceWithChartsPutRequestDto request = getUpdateRequest();

        HelmfileData result = helmfileMapper.toHelmfileData(request);

        assertThat(result).isNotNull();
        assertThat(result.getRepository()).isEqualTo(REPOSITORY);
        assertThat(result.getCharts()).isNotEmpty();
        assertThat(result.getCharts().size()).isEqualTo(3);
        assertThat(result.getCharts().get(0).getVersion()).isEqualTo(VERSION);
        assertThat(result.getCharts()).allSatisfy(chart -> assertThat(chart.getNeeds()).isNull());
        assertThat(result.getTimeout()).isNull();
    }

    private WorkloadInstanceWithChartsRequestDto getInstantiateRequest() {
        WorkloadInstanceWithChartsRequestDto requestDto = new WorkloadInstanceWithChartsRequestDto();
        requestDto.setNamespace("something");
        requestDto.setRepository(REPOSITORY);
        requestDto.setCharts(getCharts(RELEASES));
        return requestDto;
    }

    private WorkloadInstanceWithChartsPutRequestDto getUpdateRequest() {
        WorkloadInstanceWithChartsPutRequestDto requestDto = new WorkloadInstanceWithChartsPutRequestDto();
        requestDto.setRepository(REPOSITORY);
        requestDto.setCharts(getCharts(RELEASES));
        return requestDto;
    }

    private List<ChartDto> getCharts(String ... names) {
        return Stream.of(names)
                .map(this::getChart)
                .collect(Collectors.toList());
    }

    private ChartDto getChart(String name) {
        ChartDto chart = new ChartDto();
        chart.setOrder(1);
        chart.setName(name);
        chart.setCrd(false);
        chart.setVersion(VERSION);
        return chart;
    }

}