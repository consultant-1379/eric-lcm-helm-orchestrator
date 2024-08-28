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

import com.ericsson.oss.management.lcm.api.model.OperationDto;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = LocalDateTime.class)
public interface OperationDtoMapper {

    @Mapping(target = "operationId", source = "id")
    @Mapping(target = "workloadInstanceId", source = "workloadInstance.workloadInstanceId")
    @Mapping(target = "startTime", expression = "java(operation.getStartTime().toString())")
    OperationDto toOperationDto(Operation operation);

    @Mapping(target = "id", source = "operationId")
    @Mapping(target = "workloadInstance.workloadInstanceId", source = "workloadInstanceId")
    @Mapping(target = "startTime", expression = "java(LocalDateTime.parse(operationDto.getStartTime()))")
    Operation toOperation(OperationDto operationDto);
}
