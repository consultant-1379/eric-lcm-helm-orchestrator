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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ericsson.oss.management.lcm.api.model.ChartDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstancePostRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstancePutRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithChartsPutRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithChartsRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithURLPutRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithURLRequestDto;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceHelmSourceUrl;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.HelmSourceType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;

@SpringBootTest(classes = WorkloadInstanceDtoMapperImpl.class)
class WorkloadInstanceDtoMapperTest {

    private static final String WORKLOAD_INSTANCE_ID = "workloadInstanceId";
    private static final String WORKLOAD_INSTANCE_NAME = "workloadInstanceName";
    private static final String NAMESPACE = "namespace";
    private static final String CRD_NAMESPACE = "crd-namespace";
    private static final String CLUSTER = "cluster";
    private static final Integer TIMEOUT = 5;
    private static final String ADDITIONAL_PARAMETERS = "{\"key\":\"value\"}";
    private static final String COMPLEX_ADDITIONAL_PARAMETERS = "{\"key1\":\"value\",\"key2\":true,\"key3\":5,\"key4\":{\"key\":\"value\"}}";
    private static final String HELM_SOURCE_VERSION_1 = "1.2.3-4";
    private static final String HELM_SOURCE_VERSION_2 = "5.6.7-8";
    private static final String REPOSITORY = "repository";
    private static final String URL = "http://eric-lcm-helm-chart-registry.test.svc.cluster.local:8080/internal/charts/eric-ctrl-bro-7.8.0-34.tgz";
    private static final int VERSION = 3;

    @Autowired
    private WorkloadInstanceDtoMapper workloadInstanceDtoMapper;

    @Test
    void shouldMapWorkloadInstanceToWorkloadInstanceDto() {
        WorkloadInstance workloadInstance = getWorkloadInstance();

        WorkloadInstanceDto workloadInstanceDto = workloadInstanceDtoMapper.toWorkloadInstanceDto(workloadInstance);

        assertThat(workloadInstanceDto.getWorkloadInstanceId()).isEqualTo(WORKLOAD_INSTANCE_ID);
        assertThat(workloadInstanceDto.getWorkloadInstanceName()).isEqualTo(WORKLOAD_INSTANCE_NAME);
        assertThat(workloadInstanceDto.getNamespace()).isEqualTo(NAMESPACE);
        assertThat(workloadInstanceDto.getCluster()).isEqualTo(CLUSTER);
        assertThat(workloadInstanceDto.getAdditionalParameters()).containsEntry("key1", "value");
        assertThat(workloadInstanceDto.getHelmSourceVersions()).hasSize(2);
        assertThat(workloadInstanceDto.getHelmSourceVersions().get(0)).isEqualTo(HELM_SOURCE_VERSION_1);
        assertThat(workloadInstanceDto.getHelmSourceVersions().get(1)).isEqualTo(HELM_SOURCE_VERSION_2);
        assertThat(workloadInstanceDto.getCrdNamespace()).isEqualTo(CRD_NAMESPACE);
        assertThat(workloadInstanceDto.getVersion()).isEqualTo(VERSION);
    }

    @Test
    void shouldMapWorkloadInstanceDtoToWorkloadInstance() {
        WorkloadInstanceDto workloadInstanceDto = getWorkloadInstanceDto();

        WorkloadInstance workloadInstance = workloadInstanceDtoMapper.toWorkloadInstance(workloadInstanceDto);

        assertThat(workloadInstance.getWorkloadInstanceId()).isEqualTo(WORKLOAD_INSTANCE_ID);
        assertThat(workloadInstance.getWorkloadInstanceName()).isEqualTo(WORKLOAD_INSTANCE_NAME);
        assertThat(workloadInstance.getNamespace()).isEqualTo(NAMESPACE);
        assertThat(workloadInstance.getCluster()).isEqualTo(CLUSTER);
        assertThat(workloadInstance.getAdditionalParameters()).isEqualTo(ADDITIONAL_PARAMETERS);
        assertThat(workloadInstance.getHelmSources()).isNull();
        assertThat(workloadInstance.getReleases()).isNull();
        assertThat(workloadInstance.getWorkloadInstanceVersions()).isNull();
        assertThat(workloadInstance.getClusterConnectionInfoInstance()).isNull();
    }

    @Test
    void shouldMapWorkloadInstancePostRequestDtoToWorkloadInstance() {
        WorkloadInstancePostRequestDto dto = getPostRequestDto();

        WorkloadInstance result = workloadInstanceDtoMapper.toWorkloadInstance(dto);

        assertThat(result.getWorkloadInstanceName()).isEqualTo(WORKLOAD_INSTANCE_NAME);
        assertThat(result.getCluster()).isEqualTo(CLUSTER);
        assertThat(result.getCrdNamespace()).isEqualTo(CRD_NAMESPACE);
        assertThat(result.getNamespace()).isEqualTo(NAMESPACE);

        String additionalParametersResult = result.getAdditionalParameters();

        assertThat(additionalParametersResult).isEqualTo(COMPLEX_ADDITIONAL_PARAMETERS);
        assertThat(result.getHelmSources()).isNull();
        assertThat(result.getReleases()).isNull();
        assertThat(result.getWorkloadInstanceVersions()).isNull();
        assertThat(result.getClusterConnectionInfoInstance()).isNull();
    }

    @Test
    void shouldMapWorkloadInstanceWithChartsRequestDtoToWorkloadInstance() {
        WorkloadInstanceWithChartsRequestDto dto = getWithChartsRequestDto();

        WorkloadInstance result = workloadInstanceDtoMapper.toWorkloadInstance(dto);

        assertThat(result.getWorkloadInstanceName()).isEqualTo(WORKLOAD_INSTANCE_NAME);
        assertThat(result.getCluster()).isEqualTo(CLUSTER);
        assertThat(result.getCrdNamespace()).isEqualTo(CRD_NAMESPACE);
        assertThat(result.getNamespace()).isEqualTo(NAMESPACE);

        String additionalParametersResult = result.getAdditionalParameters();

        assertThat(additionalParametersResult).isEqualTo(COMPLEX_ADDITIONAL_PARAMETERS);
        assertThat(result.getHelmSources()).isNull();
        assertThat(result.getReleases()).isNull();
        assertThat(result.getWorkloadInstanceVersions()).isNull();
        assertThat(result.getClusterConnectionInfoInstance()).isNull();
    }

    @Test
    void shouldMapWorkloadInstanceWithURLRequestDtoToWorkloadInstance() {
        WorkloadInstanceWithURLRequestDto dto = getWithURLRequestDto();

        WorkloadInstance result = workloadInstanceDtoMapper.toWorkloadInstance(dto);

        assertThat(result.getWorkloadInstanceName()).isEqualTo(WORKLOAD_INSTANCE_NAME);
        assertThat(result.getCluster()).isEqualTo(CLUSTER);
        assertThat(result.getCrdNamespace()).isEqualTo(CRD_NAMESPACE);
        assertThat(result.getNamespace()).isEqualTo(NAMESPACE);

        String additionalParametersResult = result.getAdditionalParameters();

        assertThat(additionalParametersResult).isEqualTo(COMPLEX_ADDITIONAL_PARAMETERS);
        assertThat(result.getHelmSources()).isNull();
        assertThat(result.getReleases()).isNull();
        assertThat(result.getWorkloadInstanceVersions()).isNull();
        assertThat(result.getClusterConnectionInfoInstance()).isNull();
    }

    @Test
    void shouldMapWorkloadInstanceHelmSourceUrlToWorkloadInstance() {
        WorkloadInstanceHelmSourceUrl dto = getWorkloadInstanceHelmSourceUrl();

        WorkloadInstance result = workloadInstanceDtoMapper.toWorkloadInstance(dto);

        assertThat(result.getWorkloadInstanceName()).isEqualTo(WORKLOAD_INSTANCE_NAME);
        assertThat(result.getCluster()).isEqualTo(CLUSTER);
        assertThat(result.getCrdNamespace()).isEqualTo(CRD_NAMESPACE);
        assertThat(result.getNamespace()).isEqualTo(NAMESPACE);

        String additionalParametersResult = result.getAdditionalParameters();

        assertThat(additionalParametersResult).isEqualTo(COMPLEX_ADDITIONAL_PARAMETERS);
        assertThat(result.getHelmSources()).isNull();
        assertThat(result.getReleases()).isNull();
        assertThat(result.getWorkloadInstanceVersions()).isNull();
        assertThat(result.getClusterConnectionInfoInstance()).isNull();
    }
    @Test
    void shouldUpdateWorkloadInstanceFromWorkloadInstancePutRequestDto() {
        WorkloadInstance instance = getWorkloadInstance();
        instance.setAdditionalParameters(ADDITIONAL_PARAMETERS);
        WorkloadInstancePutRequestDto dto = getPutRequestDto();

        workloadInstanceDtoMapper.updateWorkloadInstanceFromWorkloadInstancePutRequestDto(dto, instance);

        assertThat(instance.getAdditionalParameters()).isEqualTo(COMPLEX_ADDITIONAL_PARAMETERS);
    }

    @Test
    void shouldUpdateWorkloadInstanceFromWorkloadInstanceWithChartsPutRequestDto() {
        WorkloadInstance instance = getWorkloadInstance();
        instance.setAdditionalParameters(ADDITIONAL_PARAMETERS);
        WorkloadInstanceWithChartsPutRequestDto dto = getWithChartsPutRequestDto();

        workloadInstanceDtoMapper.updateWorkloadInstanceFromWorkloadInstanceWithChartsPutRequestDto(dto, instance);

        assertThat(instance.getAdditionalParameters()).isEqualTo(COMPLEX_ADDITIONAL_PARAMETERS);
    }

    @Test
    void shouldUpdateWorkloadInstanceFromWorkloadInstanceWithUrlPutRequestDto() {
        WorkloadInstance instance = getWorkloadInstance();
        instance.setAdditionalParameters(ADDITIONAL_PARAMETERS);
        WorkloadInstanceWithURLPutRequestDto dto = getWithURLPutRequestDto();

        workloadInstanceDtoMapper.updateWorkloadInstanceFromWorkloadInstanceWithUrlPutRequestDto(dto, instance);

        assertThat(instance.getAdditionalParameters()).isEqualTo(COMPLEX_ADDITIONAL_PARAMETERS);
    }

    @Test
    void shouldMapStringAdditionalParametersToMap() {
        Map<String, Object> result = workloadInstanceDtoMapper.jsonAdditionalParametersToMap(COMPLEX_ADDITIONAL_PARAMETERS);

        assertThat(result).isNotEmpty()
                .hasSize(4)
                .containsEntry("key1", "value")
                .containsEntry("key2", true)
                .containsEntry("key3", 5);
        assertThat(result.get("key4")).isInstanceOf(Map.class);
        Map<String, Object> resultNestedMap = (Map<String, Object>) result.get("key4");
        assertThat(resultNestedMap).containsEntry("key", "value");
    }

    @Test
    void shouldMapToJsonAdditionalParameters() {
        String result = workloadInstanceDtoMapper.mapToJsonAdditionalParameters(getAdditionalParams());
        assertThat(result).isEqualTo(COMPLEX_ADDITIONAL_PARAMETERS);
    }

    @Test
    void shouldMapListHelmSourcesToListHelmVersions() {
        WorkloadInstance workloadInstance = new WorkloadInstance();
        HelmSource helmSourceFirst = getHelmSource(workloadInstance, HELM_SOURCE_VERSION_1);
        HelmSource helmSourceSecond = getHelmSource(workloadInstance, HELM_SOURCE_VERSION_2);
        List<HelmSource> helmSources = List.of(helmSourceFirst, helmSourceSecond);

        List<String> result = workloadInstanceDtoMapper.listHelmSourcesToListHelmVersions(helmSources);

        assertThat(result).isNotEmpty()
                .hasSize(2)
                .contains(HELM_SOURCE_VERSION_1, HELM_SOURCE_VERSION_2);
    }

    private WorkloadInstance getWorkloadInstance() {
        WorkloadInstance workloadInstance = new WorkloadInstance();

        workloadInstance.setWorkloadInstanceId(WORKLOAD_INSTANCE_ID);
        workloadInstance.setWorkloadInstanceName(WORKLOAD_INSTANCE_NAME);
        workloadInstance.setNamespace(NAMESPACE);
        workloadInstance.setCrdNamespace(CRD_NAMESPACE);
        workloadInstance.setCluster(CLUSTER);
        workloadInstance.setAdditionalParameters(COMPLEX_ADDITIONAL_PARAMETERS);
        workloadInstance.setVersion(VERSION);

        HelmSource helmSourceFirst = getHelmSource(workloadInstance, HELM_SOURCE_VERSION_1);
        HelmSource helmSourceSecond = getHelmSource(workloadInstance, HELM_SOURCE_VERSION_2);
        workloadInstance.setHelmSources(List.of(helmSourceFirst, helmSourceSecond));

        return workloadInstance;
    }

    private WorkloadInstancePostRequestDto getPostRequestDto() {
        WorkloadInstancePostRequestDto dto = new WorkloadInstancePostRequestDto();
        dto.setWorkloadInstanceName(WORKLOAD_INSTANCE_NAME);
        dto.setNamespace(NAMESPACE);
        dto.setCrdNamespace(CRD_NAMESPACE);
        dto.setCluster(CLUSTER);
        dto.setTimeout(TIMEOUT);
        dto.setAdditionalParameters(getAdditionalParams());

        return dto;
    }

    private WorkloadInstanceWithChartsRequestDto getWithChartsRequestDto() {
        WorkloadInstanceWithChartsRequestDto dto = new WorkloadInstanceWithChartsRequestDto();
        dto.setWorkloadInstanceName(WORKLOAD_INSTANCE_NAME);
        dto.setNamespace(NAMESPACE);
        dto.setCrdNamespace(CRD_NAMESPACE);
        dto.setCluster(CLUSTER);
        dto.setTimeout(TIMEOUT);
        dto.setAdditionalParameters(getAdditionalParams());
        dto.setRepository(REPOSITORY);
        dto.setCharts(List.of(new ChartDto()));

        return dto;
    }

    private WorkloadInstanceWithChartsPutRequestDto getWithChartsPutRequestDto() {
        WorkloadInstanceWithChartsPutRequestDto dto = new WorkloadInstanceWithChartsPutRequestDto();
        dto.setAdditionalParameters(getAdditionalParams());
        dto.setRepository(REPOSITORY);
        dto.setCharts(List.of(new ChartDto()));

        return dto;
    }

    private WorkloadInstanceWithURLRequestDto getWithURLRequestDto() {
        WorkloadInstanceWithURLRequestDto dto = new WorkloadInstanceWithURLRequestDto();
        dto.setWorkloadInstanceName(WORKLOAD_INSTANCE_NAME);
        dto.setNamespace(NAMESPACE);
        dto.setCrdNamespace(CRD_NAMESPACE);
        dto.setCluster(CLUSTER);
        dto.setTimeout(TIMEOUT);
        dto.setAdditionalParameters(getAdditionalParams());
        dto.setUrl(URL);

        return dto;
    }

    private WorkloadInstanceWithURLPutRequestDto getWithURLPutRequestDto() {
        WorkloadInstanceWithURLPutRequestDto dto = new WorkloadInstanceWithURLPutRequestDto();
        dto.setAdditionalParameters(getAdditionalParams());
        dto.setUrl(URL);

        return dto;
    }

    private WorkloadInstanceHelmSourceUrl getWorkloadInstanceHelmSourceUrl() {
        WorkloadInstanceHelmSourceUrl instance = new WorkloadInstanceHelmSourceUrl();
        instance.setWorkloadInstanceName(WORKLOAD_INSTANCE_NAME);
        instance.setNamespace(NAMESPACE);
        instance.setCrdNamespace(CRD_NAMESPACE);
        instance.setCluster(CLUSTER);
        instance.setTimeout(TIMEOUT);
        instance.setAdditionalParameters(getAdditionalParams());
        instance.setHelmSourceUrl(URL);

        return instance;
    }

    private WorkloadInstancePutRequestDto getPutRequestDto() {
        WorkloadInstancePutRequestDto dto = new WorkloadInstancePutRequestDto();
        dto.setAdditionalParameters(getAdditionalParams());
        return dto;
    }

    private Map<String, Object> getAdditionalParams() {
        Map<String, Object> nestedAdditionalParameters = new LinkedHashMap<>();
        nestedAdditionalParameters.put("key", "value");

        Map<String, Object> additionalParameters = new LinkedHashMap<>();
        additionalParameters.put("key1", "value");
        additionalParameters.put("key2", true);
        additionalParameters.put("key3", 5);
        additionalParameters.put("key4", nestedAdditionalParameters);
        return additionalParameters;
    }

    private WorkloadInstanceDto getWorkloadInstanceDto() {
        WorkloadInstanceDto workloadInstanceDto = new WorkloadInstanceDto();

        workloadInstanceDto.setWorkloadInstanceId(WORKLOAD_INSTANCE_ID);
        workloadInstanceDto.setWorkloadInstanceName(WORKLOAD_INSTANCE_NAME);
        workloadInstanceDto.setNamespace(NAMESPACE);
        workloadInstanceDto.setCluster(CLUSTER);
        Map<String, Object> additionalParametersMap = new LinkedHashMap<>();
        additionalParametersMap.put("key", "value");
        workloadInstanceDto.setAdditionalParameters(additionalParametersMap);
        workloadInstanceDto.setHelmSourceVersions(List.of(HELM_SOURCE_VERSION_1, HELM_SOURCE_VERSION_2));

        return workloadInstanceDto;
    }

    private HelmSource getHelmSource(WorkloadInstance workloadInstance, String helmSourceVersion) {
        HelmSource helmSource = new HelmSource();
        helmSource.setHelmSourceType(HelmSourceType.HELMFILE);
        helmSource.setWorkloadInstance(workloadInstance);
        helmSource.setHelmSourceVersion(helmSourceVersion);
        return helmSource;
    }

}
