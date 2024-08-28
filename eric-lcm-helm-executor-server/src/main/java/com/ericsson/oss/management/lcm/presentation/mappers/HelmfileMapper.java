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

import com.ericsson.oss.management.lcm.api.model.ChartDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithChartsPutRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithChartsRequestDto;
import com.ericsson.oss.management.lcm.model.internal.Chart;
import com.ericsson.oss.management.lcm.model.internal.HelmfileData;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface HelmfileMapper {
    HelmfileData toHelmfileData(WorkloadInstanceWithChartsRequestDto workloadInstanceWithChartsRequestDto);
    HelmfileData toHelmfileData(WorkloadInstanceWithChartsPutRequestDto workloadInstanceWithChartsPutRequestDto);

    Chart toChart(ChartDto chartDto);
}
