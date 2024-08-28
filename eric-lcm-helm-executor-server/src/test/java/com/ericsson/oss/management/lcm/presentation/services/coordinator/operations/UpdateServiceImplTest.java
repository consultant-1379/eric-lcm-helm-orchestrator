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

package com.ericsson.oss.management.lcm.presentation.services.coordinator.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.HelmfileExecutorApplication;
import com.ericsson.oss.management.lcm.TestUtils;
import com.ericsson.oss.management.lcm.api.model.ChartDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithURLPutRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstancePutRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithChartsPutRequestDto;
import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.HelmSourceType;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;
import com.ericsson.oss.management.lcm.presentation.services.clusterconnectioninfo.ClusterConnectionInfoService;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.presentation.services.helmsource.HelmSourceService;
import com.ericsson.oss.management.lcm.presentation.services.helper.docker.DockerRegistrySecretHelper;
import com.ericsson.oss.management.lcm.presentation.services.helper.file.StoreFileHelper;
import com.ericsson.oss.management.lcm.presentation.services.helper.lcm.LcmHelper;
import com.ericsson.oss.management.lcm.presentation.services.operation.OperationService;
import com.ericsson.oss.management.lcm.presentation.services.version.WorkloadInstanceVersionService;
import com.ericsson.oss.management.lcm.presentation.services.workloadinstance.WorkloadInstanceService;
import com.ericsson.oss.management.lcm.utils.HelmfileHandler;

@ActiveProfiles("test")
@SpringBootTest(classes = HelmfileExecutorApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UpdateServiceImplTest extends AbstractDbSetupTest {

    @Autowired
    private UpdateServiceImpl updateService;
    @MockBean
    private LcmHelper lcmHelper;
    @MockBean
    private WorkloadInstanceService workloadInstanceService;
    @MockBean
    private OperationService operationService;
    @MockBean
    private HelmSourceService helmSourceService;
    @MockBean
    private HelmfileHandler converter;
    @MockBean
    private FileService fileService;
    @MockBean
    private StoreFileHelper storeFileHelper;
    @MockBean
    private DockerRegistrySecretHelper dockerRegistrySecretHelper;
    @MockBean
    private WorkloadInstanceVersionService workloadInstanceVersionService;
    @MockBean
    private ClusterConnectionInfoService clusterConnectionInfoService;
    @Mock
    private MultipartFile helmSourceFile;
    @Mock
    private MultipartFile values;
    @Mock
    private MultipartFile clusterConnectionInfo;
    @Mock
    private Path helmPath;
    @Mock
    private Path valuesPath;
    @Mock
    private FilePathDetails pathData;
    @Mock
    private Path kubeConfigPath;

    private static final String HELMSOURCE_ID = "some_id";
    private static final String OPERATION_ID = "some_id";
    private static final String WORKLOAD_INSTANCE_ID = "wrkld_instance_id";
    private static final String ADDITIONAL_PARAMETERS_TEST_KEY = "testKey";
    private static final String WORKLOAD_INSTANCE_NAME = "workloadInstanceName";
    private static final String NAMESPACE = "namespace";
    private static final String ADDITIONAL_PARAMETERS = "{\"key\": \"value\"}";
    private static final String HELM_SOURCE_FILE_NAME = "helmsource-1.2.3-4.tgz";
    private static final String HELMSOURCE_PATH = "./helm_source-1.2.3-4.tgz";
    private static final String HELM_SOURCE_VERSION = "1.2.3-4";
    private static final String GLOBAL_CRD_NAMESPACE_VALUE = "eric-crd-ns";
    private static final String DIRECTORY_PATH = "helmfile-test";
    private static final Integer WORKLOAD_INSTANCE_VERSION = 1;
    private static final Integer TIMEOUT = 2;
    private static final Integer DEFAULT_TIMEOUT = 5;
    private static final String VALUES_VERSION = "0e35ed30-d438-4b07-a82b-cab447424d30";
    private static final String CLUSTER_IDENT = "hahn117 https://mocha.rnd.gic.ericsson.se/k8s/clusters/c-mdw5r";
    private static final String CHART_VERSION = "1.2.3-89";
    private static final String REPOSITORY = "https://arm.sero.gic.ericsson.se/artifactory/proj-adp-gs-all-helm";

    @BeforeEach
    void setup() throws URISyntaxException {
        Path directory = TestUtils.getResource(DIRECTORY_PATH);
        when(fileService.createDirectory()).thenReturn(directory);
        when(fileService.storeFileIn(eq(directory), eq(helmSourceFile), anyString())).thenReturn(helmPath);
        when(storeFileHelper.getValuesPath(any(), any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(pathData);
        when(storeFileHelper.getKubeConfigPath(eq(directory), any(), eq(clusterConnectionInfo))).thenReturn(kubeConfigPath);
        when(lcmHelper.preparePaths(helmPath, valuesPath, kubeConfigPath)).thenReturn(preparePaths(helmPath, valuesPath, kubeConfigPath));
        when(fileService.createFile(eq(directory), any(), anyString())).thenReturn(helmPath);
        when(helmPath.getParent()).thenReturn(directory);
    }

    @Test
    void shouldReturnSuccessfulResponseWhenUpdateWithNewHelmSource() {
        WorkloadInstance instance = basicWorkloadInstance();
        instance.setVersion(WORKLOAD_INSTANCE_VERSION);
        Operation operation = getOperation();
        operation.setType(OperationType.INSTANTIATE);
        String versionFromUpdate = "1.2.3-5";
        HelmSource helmSource = getHelmSource();
        helmSource.setHelmSourceVersion(versionFromUpdate);

        when(operationService.get(OPERATION_ID)).thenReturn(operation);
        when(helmSourceFile.getOriginalFilename()).thenReturn(HELM_SOURCE_FILE_NAME);
        when(helmSourceService.resolveHelmSourceType(any())).thenReturn(HelmSourceType.HELMFILE);
        when(helmSourceService.getHelmSourceVersion(any(), eq(HelmSourceType.HELMFILE))).thenReturn(versionFromUpdate);
        when(helmSourceService.create(any(), any(), any())).thenReturn(helmSource);
        when(helmSourceService.executeHelmSource(eq(helmSource), anyInt(), any(), any(), any())).thenReturn(operation);
        when(workloadInstanceService.update(any())).thenReturn(instance);

        WorkloadInstance result =
                updateService.update(instance, helmSourceFile, setWorkloadInstancePutRequestDto(), values, clusterConnectionInfo);

        assertThat(result).isNotNull();
        verify(helmSourceService).verifyHelmSource(any(), any(), eq(HelmSourceType.HELMFILE));
        verify(helmSourceService).executeHelmSource(any(), eq(TIMEOUT), any(), any(), any());
        verify(dockerRegistrySecretHelper).createSecret(any(), any());
        verify(lcmHelper).setNewHelmSourceToInstance(any(), any());
        verify(clusterConnectionInfoService).verifyClusterIdentifier(any(), any());
    }

    @Test
    void shouldReturnSuccessfulResponseWhenUpdateWithExistingHelmSource() {
        WorkloadInstance instance = basicWorkloadInstance();
        instance.setVersion(WORKLOAD_INSTANCE_VERSION);
        Operation operation = getOperation();
        operation.setType(OperationType.INSTANTIATE);
        HelmSource helmSource = getHelmSource();
        WorkloadInstanceVersion version = getVersion(instance);

        when(operationService.get(OPERATION_ID)).thenReturn(operation);
        when(workloadInstanceVersionService.getVersion(instance)).thenReturn(version);
        when(helmSourceService.getByWorkloadInstanceAndVersion(instance, HELM_SOURCE_VERSION)).thenReturn(helmSource);

        when(helmSourceService.executeHelmSource(eq(helmSource), anyInt(), any(), any(), any())).thenReturn(operation);
        when(workloadInstanceService.update(any())).thenReturn(instance);

        WorkloadInstance result =
                updateService.update(instance, null, setWorkloadInstancePutRequestDto(), values, clusterConnectionInfo);

        assertThat(result).isNotNull();
        verify(helmSourceService, never()).verifyHelmSource(any(), any(), eq(HelmSourceType.HELMFILE));
        verify(helmSourceService).executeHelmSource(any(), eq(TIMEOUT), any(), any(), any());
        verify(dockerRegistrySecretHelper).createSecret(any(), any());
        verify(lcmHelper).setNewHelmSourceToInstance(any(), any());
        verify(helmSourceService).extractArchiveForHelmfile(any(), any());
        verify(clusterConnectionInfoService).verifyClusterIdentifier(any(), any());
    }

    @Test
    void shouldReturnSuccessfulResponseWhenReinstantiate() {
        WorkloadInstance instance = basicWorkloadInstance();
        instance.setVersion(WORKLOAD_INSTANCE_VERSION);
        Operation operation = getOperation();
        operation.setType(OperationType.TERMINATE);
        HelmSource helmSource = getHelmSource();
        WorkloadInstanceVersion version = getVersion(instance);

        when(operationService.get(OPERATION_ID)).thenReturn(operation);
        when(workloadInstanceVersionService.getVersion(instance)).thenReturn(version);
        when(helmSourceService.getByWorkloadInstanceAndVersion(instance, HELM_SOURCE_VERSION)).thenReturn(helmSource);

        when(helmSourceService.executeHelmSource(eq(helmSource), anyInt(), any(), any(), any())).thenReturn(operation);
        when(workloadInstanceService.update(any())).thenReturn(instance);

        WorkloadInstance result =
                updateService.update(instance, null, (WorkloadInstancePutRequestDto) null, null, null);

        assertThat(result).isNotNull();
        verify(helmSourceService, never()).verifyHelmSource(any(), any(), eq(HelmSourceType.HELMFILE));
        verify(dockerRegistrySecretHelper).createSecret(any(), any());
        verify(lcmHelper).setNewHelmSourceToInstance(any(), any());
        verify(helmSourceService).extractArchiveForHelmfile(any(), any());
        verify(helmSourceService).executeHelmSource(any(), eq(DEFAULT_TIMEOUT), any(), any(), any());
        verify(clusterConnectionInfoService).verifyClusterIdentifier(any(), any());
    }

    @Test
    void shouldReturnSuccessfulResponseWhenUpdateWithNewChartsThroughHelmfileBuilder() {
        WorkloadInstance instance = basicWorkloadInstance();
        instance.setVersion(WORKLOAD_INSTANCE_VERSION);
        Operation operation = getOperation();
        operation.setType(OperationType.INSTANTIATE);
        String versionAfterUpdate = "1.2.3-5";
        HelmSource helmSource = getHelmSource();
        helmSource.setHelmSourceVersion(versionAfterUpdate);
        WorkloadInstanceVersion version = getVersion(instance);

        when(operationService.get(OPERATION_ID)).thenReturn(operation);
        when(workloadInstanceVersionService.getVersion(instance)).thenReturn(version);
        when(converter.convertHelmfile(any(), anyString())).thenReturn(helmPath);

        when(helmSourceService.create(any(), any(), any())).thenReturn(helmSource);
        when(helmSourceService.executeHelmSource(eq(helmSource), anyInt(), any(), any(), any())).thenReturn(operation);
        when(workloadInstanceService.update(any())).thenReturn(instance);

        WorkloadInstance result =
                updateService.update(instance, setWorkloadInstanceWithChartsPutRequestDto(), values, clusterConnectionInfo);

        assertThat(result).isNotNull();
        verify(helmSourceService).verifyHelmfile(any());
        verify(helmSourceService).executeHelmSource(any(), eq(TIMEOUT), any(), any(), any());
        verify(dockerRegistrySecretHelper).createSecret(any(), any());
        verify(lcmHelper).setNewHelmSourceToInstance(any(), any());
        verify(clusterConnectionInfoService).verifyClusterIdentifier(any(), any());
    }

    @Test
    void shouldReturnSuccessfulResponseWhenUpdateWithRequestNullThroughHelmfileBuilder() {
        WorkloadInstance instance = basicWorkloadInstance();
        instance.setVersion(WORKLOAD_INSTANCE_VERSION);
        Operation operation = getOperation();
        operation.setType(OperationType.INSTANTIATE);
        HelmSource helmSource = getHelmSource();
        WorkloadInstanceVersion version = getVersion(instance);

        when(operationService.get(OPERATION_ID)).thenReturn(operation);
        when(workloadInstanceVersionService.getVersion(instance)).thenReturn(version);
        when(helmSourceService.getByWorkloadInstanceAndVersion(instance, HELM_SOURCE_VERSION)).thenReturn(helmSource);

        when(helmSourceService.executeHelmSource(eq(helmSource), anyInt(), any(), any(), any())).thenReturn(operation);
        when(workloadInstanceService.update(any())).thenReturn(instance);

        WorkloadInstance result =
                updateService.update(instance, null, values, clusterConnectionInfo);

        assertThat(result).isNotNull();
        verify(helmSourceService).verifyHelmfile(any());
        verify(helmSourceService).executeHelmSource(any(), eq(DEFAULT_TIMEOUT), any(), any(), any());
        verify(dockerRegistrySecretHelper).createSecret(any(), any());
        verify(lcmHelper).setNewHelmSourceToInstance(any(), any());
        verify(helmSourceService).extractArchiveForHelmfile(any(), any());
        verify(clusterConnectionInfoService).verifyClusterIdentifier(any(), any());
    }

    @Test
    void shouldReturnSuccessfulResponseWhenUpdateWithoutUrlThroughHelmfileFetcher() {
        WorkloadInstance instance = basicWorkloadInstance();
        instance.setVersion(WORKLOAD_INSTANCE_VERSION);
        Operation operation = getOperation();
        operation.setType(OperationType.INSTANTIATE);
        HelmSource helmSource = getHelmSource();
        WorkloadInstanceVersion version = getVersion(instance);

        when(operationService.get(OPERATION_ID)).thenReturn(operation);
        when(workloadInstanceVersionService.getVersion(instance)).thenReturn(version);
        when(helmSourceService.getByWorkloadInstanceAndVersion(instance, HELM_SOURCE_VERSION)).thenReturn(helmSource);

        when(helmSourceService.executeHelmSource(eq(helmSource), anyInt(), any(), any(), any())).thenReturn(operation);
        when(workloadInstanceService.update(any())).thenReturn(instance);

        WorkloadInstance result =
                updateService.update(instance, true, null, values, clusterConnectionInfo);

        assertThat(result).isNotNull();
        verify(helmSourceService).verifyHelmSource(any(), any(), any());
        verify(helmSourceService).executeHelmSource(any(), eq(DEFAULT_TIMEOUT), any(), any(), any());
        verify(dockerRegistrySecretHelper).createSecret(any(), any());
        verify(lcmHelper).setNewHelmSourceToInstance(any(), any());
        verify(helmSourceService).extractArchiveForHelmfile(any(), any());
        verify(clusterConnectionInfoService).verifyClusterIdentifier(any(), any());
    }

    @Test
    void shouldReturnSuccessfulResponseWhenUpdateWithUrlThroughHelmfileFetcher() {
        WorkloadInstance instance = basicWorkloadInstance();
        instance.setVersion(WORKLOAD_INSTANCE_VERSION);
        Operation operation = getOperation();
        operation.setType(OperationType.INSTANTIATE);
        HelmSource helmSource = getHelmSource();
        WorkloadInstanceVersion version = getVersion(instance);
        WorkloadInstanceWithURLPutRequestDto requestDto = setWorkloadInstanceWithURLRequestDto();

        when(operationService.get(OPERATION_ID)).thenReturn(operation);
        when(workloadInstanceVersionService.getVersion(instance)).thenReturn(version);
        when(helmSourceService.downloadHelmSource(requestDto.getUrl(), Boolean.TRUE)).thenReturn(Path.of(DIRECTORY_PATH));
        when(helmSourceService.resolveHelmSourceType(any())).thenReturn(HelmSourceType.HELMFILE);
        when(helmSourceService.create(any(), any(), any(HelmSourceType.class))).thenReturn(helmSource);

        when(helmSourceService.executeHelmSource(eq(helmSource), anyInt(), any(), any(), any())).thenReturn(operation);
        when(workloadInstanceService.update(any())).thenReturn(instance);

        WorkloadInstance result =
                updateService.update(instance, true, requestDto, values, clusterConnectionInfo);

        assertThat(result).isNotNull();
        verify(helmSourceService).verifyHelmSource(any(), any(), any());
        verify(helmSourceService).executeHelmSource(any(), eq(TIMEOUT), any(), any(), any());
        verify(dockerRegistrySecretHelper).createSecret(any(), any());
        verify(lcmHelper).setNewHelmSourceToInstance(any(), any());
        verify(clusterConnectionInfoService).verifyClusterIdentifier(any(), any());
    }

    @Test
    void shouldReturnSuccessfulResponseWhenUpdateWithoutRequestHelmfileFetcher() {
        WorkloadInstance instance = basicWorkloadInstance();
        instance.setVersion(WORKLOAD_INSTANCE_VERSION);
        Operation operation = getOperation();
        operation.setType(OperationType.INSTANTIATE);
        HelmSource helmSource = getHelmSource();
        WorkloadInstanceVersion version = getVersion(instance);

        when(operationService.get(OPERATION_ID)).thenReturn(operation);
        when(workloadInstanceVersionService.getVersion(instance)).thenReturn(version);
        when(helmSourceService.downloadHelmSource(null, Boolean.TRUE)).thenReturn(Path.of(DIRECTORY_PATH));
        when(helmSourceService.resolveHelmSourceType(any())).thenReturn(HelmSourceType.HELMFILE);
        when(helmSourceService.create(any(), any(), any(HelmSourceType.class))).thenReturn(helmSource);
        when(helmSourceService.getByWorkloadInstanceAndVersion(any(), anyString())).thenReturn(helmSource);
        when(helmSourceService.executeHelmSource(eq(helmSource), anyInt(), any(), any(), any())).thenReturn(operation);
        when(workloadInstanceService.update(any())).thenReturn(instance);

        WorkloadInstance result =
                updateService.update(instance, true, null, null, clusterConnectionInfo);

        assertThat(result).isNotNull();
        verify(helmSourceService).verifyHelmSource(any(), any(), any());
        verify(helmSourceService).executeHelmSource(any(), eq(DEFAULT_TIMEOUT), any(), any(), any());
        verify(dockerRegistrySecretHelper).createSecret(any(), any());
        verify(lcmHelper).setNewHelmSourceToInstance(any(), any());
        verify(clusterConnectionInfoService).verifyClusterIdentifier(any(), any());
    }

    @Test
    void shouldReturnSuccessfulResponseWhenUpdateWithChartsNullThroughHelmfileBuilder() {
        WorkloadInstance instance = basicWorkloadInstance();
        instance.setVersion(WORKLOAD_INSTANCE_VERSION);
        Operation operation = getOperation();
        operation.setType(OperationType.INSTANTIATE);
        HelmSource helmSource = getHelmSource();
        WorkloadInstanceVersion version = getVersion(instance);

        when(operationService.get(OPERATION_ID)).thenReturn(operation);
        when(workloadInstanceVersionService.getVersion(instance)).thenReturn(version);
        when(helmSourceService.getByWorkloadInstanceAndVersion(instance, HELM_SOURCE_VERSION)).thenReturn(helmSource);

        when(helmSourceService.executeHelmSource(eq(helmSource), anyInt(), any(), any(), any())).thenReturn(operation);
        when(workloadInstanceService.update(any())).thenReturn(instance);

        WorkloadInstanceWithChartsPutRequestDto request = setWorkloadInstanceWithChartsPutRequestDto();
        request.setCharts(null);
        WorkloadInstance result =
                updateService.update(instance, request, values, clusterConnectionInfo);

        assertThat(result).isNotNull();
        verify(helmSourceService).verifyHelmfile(any());
        verify(helmSourceService).executeHelmSource(any(), eq(TIMEOUT), any(), any(), any());
        verify(dockerRegistrySecretHelper).createSecret(any(), any());
        verify(lcmHelper).setNewHelmSourceToInstance(any(), any());
        verify(helmSourceService).extractArchiveForHelmfile(any(), any());
        verify(clusterConnectionInfoService).verifyClusterIdentifier(any(), any());
    }

    @Test
    void shouldReturnSuccessfulResponseWhenUpdateWithChartsEmptyThroughHelmfileBuilder() {
        WorkloadInstance instance = basicWorkloadInstance();
        instance.setVersion(WORKLOAD_INSTANCE_VERSION);
        Operation operation = getOperation();
        operation.setType(OperationType.INSTANTIATE);
        HelmSource helmSource = getHelmSource();
        WorkloadInstanceVersion version = getVersion(instance);

        when(operationService.get(OPERATION_ID)).thenReturn(operation);
        when(workloadInstanceVersionService.getVersion(instance)).thenReturn(version);
        when(helmSourceService.getByWorkloadInstanceAndVersion(instance, HELM_SOURCE_VERSION)).thenReturn(helmSource);

        when(helmSourceService.executeHelmSource(eq(helmSource), anyInt(), any(), any(), any())).thenReturn(operation);
        when(workloadInstanceService.update(any())).thenReturn(instance);

        WorkloadInstanceWithChartsPutRequestDto request = setWorkloadInstanceWithChartsPutRequestDto();
        request.setCharts(new ArrayList<>());
        WorkloadInstance result =
                updateService.update(instance, request, values, clusterConnectionInfo);

        assertThat(result).isNotNull();
        verify(helmSourceService).verifyHelmfile(any());
        verify(helmSourceService).executeHelmSource(any(), eq(TIMEOUT), any(), any(), any());
        verify(dockerRegistrySecretHelper).createSecret(any(), any());
        verify(lcmHelper).setNewHelmSourceToInstance(any(), any());
        verify(helmSourceService).extractArchiveForHelmfile(any(), any());
        verify(clusterConnectionInfoService).verifyClusterIdentifier(any(), any());
    }

    @Test
    void shouldReturnSuccessfulResponseWhenReinstantiateThroughHelmfileBuilder() {
        WorkloadInstance instance = basicWorkloadInstance();
        instance.setVersion(WORKLOAD_INSTANCE_VERSION);
        Operation operation = getOperation();
        operation.setType(OperationType.TERMINATE);
        HelmSource helmSource = getHelmSource();
        WorkloadInstanceVersion version = getVersion(instance);

        when(operationService.get(OPERATION_ID)).thenReturn(operation);
        when(workloadInstanceVersionService.getVersion(instance)).thenReturn(version);
        when(helmSourceService.getByWorkloadInstanceAndVersion(instance, HELM_SOURCE_VERSION)).thenReturn(helmSource);

        when(helmSourceService.executeHelmSource(eq(helmSource), anyInt(), eq(OperationType.REINSTANTIATE), any(), any())).thenReturn(operation);
        when(workloadInstanceService.update(any())).thenReturn(instance);

        WorkloadInstance result =
                updateService.update(instance, (WorkloadInstanceWithChartsPutRequestDto) null, null, null);

        assertThat(result).isNotNull();
        verify(helmSourceService).verifyHelmfile(any());
        verify(helmSourceService).executeHelmSource(any(), eq(DEFAULT_TIMEOUT), any(), any(), any());
        verify(dockerRegistrySecretHelper).createSecret(any(), any());
        verify(lcmHelper).setNewHelmSourceToInstance(any(), any());
        verify(helmSourceService).extractArchiveForHelmfile(any(), any());
        verify(clusterConnectionInfoService).verifyClusterIdentifier(any(), any());
    }

    private WorkloadInstanceWithURLPutRequestDto setWorkloadInstanceWithURLRequestDto() {
        WorkloadInstanceWithURLPutRequestDto putRequestDto = new WorkloadInstanceWithURLPutRequestDto();
        putRequestDto.setTimeout(TIMEOUT);
        putRequestDto.setUrl(REPOSITORY);
        return putRequestDto;
    }

    private Operation getOperation() {
        return Operation.builder()
                .id(OPERATION_ID)
                .build();
    }

    private WorkloadInstancePutRequestDto setWorkloadInstancePutRequestDto() {
        WorkloadInstancePutRequestDto workloadInstancePutRequestDto = new WorkloadInstancePutRequestDto();
        workloadInstancePutRequestDto.setTimeout(TIMEOUT);
        workloadInstancePutRequestDto.setAdditionalParameters(Collections.singletonMap(ADDITIONAL_PARAMETERS_TEST_KEY, " "));
        return workloadInstancePutRequestDto;
    }

    private WorkloadInstance basicWorkloadInstance() {
        List<HelmSource> helmSources = new ArrayList<>();
        helmSources.add(getHelmSource());
        return WorkloadInstance.builder()
                .workloadInstanceName(WORKLOAD_INSTANCE_NAME)
                .workloadInstanceId(WORKLOAD_INSTANCE_ID)
                .latestOperationId(OPERATION_ID)
                .version(WORKLOAD_INSTANCE_VERSION)
                .namespace(NAMESPACE)
                .crdNamespace(GLOBAL_CRD_NAMESPACE_VALUE)
                .additionalParameters(ADDITIONAL_PARAMETERS)
                .helmSources(helmSources)
                .clusterIdentifier(CLUSTER_IDENT)
                .build();
    }

    private HelmSource getHelmSource() {
        return HelmSource.builder()
                .id(HELMSOURCE_ID)
                .helmSourceType(HelmSourceType.HELMFILE)
                .helmSourceVersion(HELM_SOURCE_VERSION)
                .build();
    }

    private FilePathDetails preparePaths(Path helmPath, Path valuesPath, Path kubeConfigPath) {
        return FilePathDetails.builder()
                .helmSourcePath(helmPath)
                .valuesPath(valuesPath)
                .kubeConfigPath(kubeConfigPath)
                .build();
    }

    private WorkloadInstanceVersion getVersion(WorkloadInstance instance) {
        return WorkloadInstanceVersion.builder()
                .workloadInstance(instance)
                .valuesVersion(VALUES_VERSION)
                .helmSourceVersion(HELM_SOURCE_VERSION)
                .id("some_random_id")
                .build();
    }

    private WorkloadInstanceWithChartsPutRequestDto setWorkloadInstanceWithChartsPutRequestDto() {
        WorkloadInstanceWithChartsPutRequestDto putRequestDto = new WorkloadInstanceWithChartsPutRequestDto();
        putRequestDto.setCharts(getCharts("release-1", "release-2"));
        putRequestDto.setRepository(REPOSITORY);
        putRequestDto.setTimeout(TIMEOUT);
        return putRequestDto;
    }

    private List<ChartDto> getCharts(String ... names) {
        return Stream.of(names)
                .map(this::getChart)
                .collect(Collectors.toList());
    }

    private ChartDto getChart(String name) {
        ChartDto chart = new ChartDto();
        chart.setName(name);
        chart.setCrd(false);
        chart.setVersion(CHART_VERSION);
        return chart;
    }

}