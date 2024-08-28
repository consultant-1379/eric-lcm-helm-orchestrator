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

import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstancePostRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstancePutRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithChartsPutRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithChartsRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithURLPutRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithURLRequestDto;
import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceHelmSourceUrl;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.ericsson.oss.management.lcm.utils.JSONParseUtils.parseJsonToMap;
import static com.ericsson.oss.management.lcm.utils.JSONParseUtils.parseMapToJson;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WorkloadInstanceDtoMapper {

    WorkloadInstance toWorkloadInstance(WorkloadInstanceDto workloadInstanceDto);

    WorkloadInstance toWorkloadInstance(WorkloadInstancePostRequestDto workloadInstancePostRequestDto);

    WorkloadInstance toWorkloadInstance(WorkloadInstanceWithChartsRequestDto workloadInstanceWithChartsRequestDto);

    WorkloadInstance toWorkloadInstance(WorkloadInstanceWithURLRequestDto requestDto);

    WorkloadInstance toWorkloadInstance(WorkloadInstanceHelmSourceUrl workloadInstanceHelmSourceUrl);

    @Mapping(target = "helmSourceVersions", source = "helmSources")
    WorkloadInstanceDto toWorkloadInstanceDto(WorkloadInstance workloadInstance);

    @BeanMapping(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateWorkloadInstanceFromWorkloadInstancePutRequestDto(WorkloadInstancePutRequestDto instancePutRequestDto,
                                                                 @MappingTarget WorkloadInstance targetInstance);

    @BeanMapping(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateWorkloadInstanceFromWorkloadInstanceWithChartsPutRequestDto(WorkloadInstanceWithChartsPutRequestDto instanceWithChartPutRequestDto,
                                                                           @MappingTarget WorkloadInstance targetInstance);
    @BeanMapping(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateWorkloadInstanceFromWorkloadInstanceWithUrlPutRequestDto(WorkloadInstanceWithURLPutRequestDto instanceWithUrlPutRequestDto,
                                                                        @MappingTarget WorkloadInstance targetInstance);

    default Map<String, Object> jsonAdditionalParametersToMap(String jsonAdditionalParameters) {
        return parseJsonToMap(jsonAdditionalParameters);
    }

    default String mapToJsonAdditionalParameters(Map<String, Object> additionalParameters) {
        return parseMapToJson(additionalParameters);
    }

    default List<String> listHelmSourcesToListHelmVersions(List<HelmSource> helmSources) {
        return Optional.ofNullable(helmSources)
                .map(this::getHelmSourceVersions)
                .orElseGet(Collections::emptyList);
    }

    private List<String> getHelmSourceVersions(List<HelmSource> helmSources) {
        return helmSources.stream()
                .map(HelmSource::getHelmSourceVersion)
                .collect(Collectors.toList());
    }
}
