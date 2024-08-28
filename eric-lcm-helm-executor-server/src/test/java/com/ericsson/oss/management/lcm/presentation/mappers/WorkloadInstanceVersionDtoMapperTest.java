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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceVersionDto;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;

@SpringBootTest(classes = WorkloadInstanceVersionDtoMapperImpl.class)
class WorkloadInstanceVersionDtoMapperTest {
    private static final Integer WORKLOAD_INSTANCE_VERSION = 1;
    private static final String HELMSOURCE_VERSION = "1.2.3-4";
    private static final String VALUES_VERSION = "0e35ed30-d438-4b07-a82b-cab447424d30";
    private static final String ID = "84e68dec-4120-4627-b5b3-b6d5b2c32f65";

    @Autowired
    private WorkloadInstanceVersionDtoMapper workloadInstanceVersionDtoMapper;

    @Test
    void shouldMapWorkloadInstanceVersionToWorkloadInstanceVersionDto() {
        WorkloadInstanceVersion workloadInstanceVersion = getVersion();

        WorkloadInstanceVersionDto workloadInstanceVersionDto =
                workloadInstanceVersionDtoMapper.toWorkloadInstanceVersionDto(workloadInstanceVersion);

        assertThat(workloadInstanceVersionDto.getId()).isEqualTo(ID);
        assertThat(workloadInstanceVersionDto.getVersion()).isEqualTo(WORKLOAD_INSTANCE_VERSION);
        assertThat(workloadInstanceVersionDto.getHelmSourceVersion()).isEqualTo(HELMSOURCE_VERSION);
        assertThat(workloadInstanceVersionDto.getValuesVersion()).isEqualTo(VALUES_VERSION);
    }

    @Test
    void shouldMapWorkloadInstanceVersionDtoToWorkloadInstanceVersion() {
        WorkloadInstanceVersionDto workloadInstanceVersionDto = getVersionDto();

        WorkloadInstanceVersion workloadInstanceVersion =
                workloadInstanceVersionDtoMapper.toWorkloadInstanceVersion(workloadInstanceVersionDto);

        assertThat(workloadInstanceVersion.getId()).isEqualTo(ID);
        assertThat(workloadInstanceVersion.getVersion()).isEqualTo(WORKLOAD_INSTANCE_VERSION);
        assertThat(workloadInstanceVersion.getHelmSourceVersion()).isEqualTo(HELMSOURCE_VERSION);
        assertThat(workloadInstanceVersion.getValuesVersion()).isEqualTo(VALUES_VERSION);
    }

    private WorkloadInstanceVersion getVersion() {
        return WorkloadInstanceVersion.builder()
                .id(ID)
                .version(WORKLOAD_INSTANCE_VERSION)
                .helmSourceVersion(HELMSOURCE_VERSION)
                .valuesVersion(VALUES_VERSION)
                .build();
    }

    private WorkloadInstanceVersionDto getVersionDto() {
        WorkloadInstanceVersionDto workloadInstanceVersionDto = new WorkloadInstanceVersionDto();

        workloadInstanceVersionDto.setId(ID);
        workloadInstanceVersionDto.setVersion(WORKLOAD_INSTANCE_VERSION);
        workloadInstanceVersionDto.setHelmSourceVersion(HELMSOURCE_VERSION);
        workloadInstanceVersionDto.setValuesVersion(VALUES_VERSION);

        return workloadInstanceVersionDto;
    }

}