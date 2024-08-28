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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.management.lcm.model.entity.HelmSourceType.HELMFILE;
import static com.ericsson.oss.management.lcm.model.entity.HelmSourceType.INTEGRATION_CHART;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ericsson.oss.management.lcm.presentation.mappers.HelmfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.TestUtils;
import com.ericsson.oss.management.lcm.api.model.ChartDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithChartsRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithURLRequestDto;
import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.HelmSourceType;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;
import com.ericsson.oss.management.lcm.model.internal.HelmfileData;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidInputException;
import com.ericsson.oss.management.lcm.presentation.services.clusterconnectioninfo.ClusterConnectionInfoService;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.presentation.services.helmsource.HelmSourceService;
import com.ericsson.oss.management.lcm.presentation.services.helper.docker.DockerRegistrySecretHelper;
import com.ericsson.oss.management.lcm.presentation.services.helper.file.StoreFileHelper;
import com.ericsson.oss.management.lcm.presentation.services.helper.lcm.LcmHelper;
import com.ericsson.oss.management.lcm.presentation.services.workloadinstance.WorkloadInstanceService;
import com.ericsson.oss.management.lcm.presentation.services.secretsmanagement.SecretsManagement;
import com.ericsson.oss.management.lcm.utils.HelmfileHandler;

@ActiveProfiles("test")
@SpringBootTest(classes = InstantiateServiceImpl.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = { "management.certificates.enrollment.enabled=true", "helmrepo.username=user", "helmrepo.password=pass" })
class InstantiateServiceImplTest {

    @Autowired
    private InstantiateServiceImpl instantiateService;
    @MockBean
    private LcmHelper lcmHelper;
    @MockBean
    private HelmSourceService helmSourceService;
    @MockBean
    private HelmfileHandler converter;
    @MockBean
    private FileService fileService;
    @MockBean
    private StoreFileHelper storeFileHelper;
    @MockBean
    private WorkloadInstanceService workloadInstanceService;
    @MockBean
    private DockerRegistrySecretHelper dockerRegistrySecretHelper;
    @MockBean
    private ClusterConnectionInfoService clusterConnectionInfoService;
    @MockBean
    private HelmfileMapper helmfileMapper;
    @MockBean
    private SecretsManagement secretsManagement;
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
    private static final String WORKLOAD_INSTANCE_ID = "workload_instance_id";
    private static final String WORKLOAD_INSTANCE_NAME = "workloadInstanceName";
    private static final String NAMESPACE = "namespace";
    private static final String ADDITIONAL_PARAMETERS = "{\"key\": \"value\"}";
    private static final String HELM_SOURCE_FILE_NAME = "helm_source-1.2.3-4.tgz";
    private static final String HELMSOURCE_PATH = "./helm_source-1.2.3-4.tgz";
    private static final String HELM_SOURCE_VERSION = "1.2.3-4";
    private static final String GLOBAL_CRD_NAMESPACE_VALUE = "eric-crd-ns";
    private static final String DIRECTORY_PATH = "helmfile-test";
    private static final Integer WORKLOAD_INSTANCE_VERSION = 1;
    private static final String CLUSTER_IDENT = "hahn117 https://mocha.rnd.gic.ericsson.se/k8s/clusters/c-mdw5r";
    private static final String CHART_VERSION = "1.2.3-89";
    private static final String REPOSITORY = "https://arm.sero.gic.ericsson.se/artifactory/proj-adp-gs-all-helm";

    @BeforeEach
    public void setup() throws URISyntaxException {
        Path directory = TestUtils.getResource(DIRECTORY_PATH);
        when(fileService.createDirectory()).thenReturn(directory);
        when(storeFileHelper.getValuesPath(any(), any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(pathData);
        when(storeFileHelper.getKubeConfigPath(eq(directory), any(), eq(clusterConnectionInfo))).thenReturn(kubeConfigPath);
        when(lcmHelper.preparePaths(any(), any(), any())).thenReturn(preparePaths(helmPath, valuesPath, kubeConfigPath));
        when(fileService.createFile(eq(directory), any(), anyString())).thenReturn(helmPath);
        when(helmPath.getParent()).thenReturn(directory);
    }

    @Test
    void shouldSuccessfulInstantiateWorkloadInstanceOfIntegrationChart() {
        //Init
        WorkloadInstance instance = basicWorkloadInstance();
        HelmSource helmSource = getHelmSource(INTEGRATION_CHART);
        var paths = preparePaths(helmPath, valuesPath, kubeConfigPath);

        when(helmSourceFile.getOriginalFilename()).thenReturn(HELM_SOURCE_FILE_NAME);
        when(fileService.storeFileIn(any(), any(), any())).thenReturn(Paths.get(HELMSOURCE_PATH));
        when(helmSourceService.resolveHelmSourceType(any())).thenReturn(INTEGRATION_CHART);
        when(helmSourceService.create(any(), any(), any())).thenReturn(helmSource);
        when(helmSourceService.executeHelmSource(any(), anyInt(), any(), any(), any())).thenReturn(getOperation());
        //Test method
        instantiateService.instantiate(instance, helmSourceFile, clusterConnectionInfo, values, 5);
        //Verify
        verify(fileService).storeFileIn(any(), any(), any());
        verify(helmSourceService).resolveHelmSourceType(any());
        verify(helmSourceFile, atLeastOnce()).getOriginalFilename();
        verify(dockerRegistrySecretHelper).createSecret(instance, kubeConfigPath);
        verify(helmSourceService).executeHelmSource(helmSource, 5, OperationType.INSTANTIATE, paths, null);
        verify(secretsManagement).createOrUpdateSecretsInNamespace(any(), eq(NAMESPACE), any());
        verify(clusterConnectionInfoService).resolveClusterIdentifier(any());
    }

    @Test
    void shouldSetNewHelmSourceToTheWorkloadInstance() {
        WorkloadInstance instance = basicWorkloadInstance();
        HelmSource helmSource = getHelmSource(INTEGRATION_CHART);
        var paths = preparePaths(helmPath, valuesPath, kubeConfigPath);

        when(fileService.storeFileIn(any(), any(), any())).thenReturn(Paths.get(HELMSOURCE_PATH));
        when(helmSourceService.resolveHelmSourceType(any())).thenReturn(INTEGRATION_CHART);
        when(helmSourceService.create(any(), any(), any())).thenReturn(helmSource);
        when(helmSourceService.executeHelmSource(any(), anyInt(), any(), any(), any())).thenReturn(getOperation());
        when(storeFileHelper.getValuesPath(any(), any(), any(), any(), eq(Boolean.TRUE), eq(Boolean.TRUE))).thenReturn(paths);

        //Test method
        instantiateService.instantiate(instance, helmSourceFile, clusterConnectionInfo, values, 5);
        //Verify
        verify(lcmHelper).setNewHelmSourceToInstance(instance, helmSource);
        verify(helmSourceService).verifyHelmSource(valuesPath, Path.of(HELMSOURCE_PATH), INTEGRATION_CHART);
        verify(fileService).storeFileIn(any(), any(), any());
        verify(helmSourceService).resolveHelmSourceType(any());
        verify(helmSourceFile, atLeastOnce()).getOriginalFilename();
        verify(dockerRegistrySecretHelper).createSecret(instance, kubeConfigPath);
        verify(helmSourceService).executeHelmSource(helmSource, 5, OperationType.INSTANTIATE, paths, null);
        verify(secretsManagement).createOrUpdateSecretsInNamespace(any(), eq(NAMESPACE), any());
        verify(clusterConnectionInfoService).resolveClusterIdentifier(any());
    }

    @Test
    void shouldSuccessfulInstantiateWorkloadInstanceOfHelmfile() {
        //Init
        WorkloadInstance instance = basicWorkloadInstance();
        WorkloadInstanceWithChartsRequestDto requestPostDto = setWorkloadInstanceWithChartsPostRequestDto();
        HelmSource helmSource = getHelmSource(HELMFILE);
        setHelmSource(instance, helmSource);
        FilePathDetails paths = preparePaths(helmPath, valuesPath, kubeConfigPath);

        when(helmfileMapper.toHelmfileData(any(WorkloadInstanceWithChartsRequestDto.class))).thenReturn(new HelmfileData());
        when(converter.convertHelmfile(any(), any())).thenReturn(Path.of(HELMSOURCE_PATH));
        when(helmSourceService.executeHelmSource(any(), anyInt(), any(), any(), any())).thenReturn(getOperation());
        when(helmSourceService.resolveHelmSourceType(Path.of(HELMSOURCE_PATH))).thenReturn(HELMFILE);
        when(helmSourceService.create(any(), any(), any())).thenReturn(helmSource);
        when(storeFileHelper.getValuesPath(any(), any(), any(), any(), eq(Boolean.TRUE), eq(Boolean.TRUE))).thenReturn(paths);
        //Test method
        instantiateService.instantiate(requestPostDto, instance, values, clusterConnectionInfo);
        //Verify
        verify(fileService).createDirectory();
        verify(helmSourceService).resolveHelmSourceType(Path.of(HELMSOURCE_PATH));
        verify(helmSourceService).verifyHelmSource(valuesPath, Path.of(HELMSOURCE_PATH), HELMFILE);
        verify(dockerRegistrySecretHelper).createSecret(instance, kubeConfigPath);
        verify(helmSourceService).executeHelmSource(helmSource, 5, OperationType.INSTANTIATE, paths, null);
        verify(secretsManagement).createOrUpdateSecretsInNamespace(any(), eq(NAMESPACE), any());
        verify(clusterConnectionInfoService).resolveClusterIdentifier(any());
    }

    @Test
    void shouldSuccessfulInstantiateWorkloadInstanceOfHelmfileByURL() {
        //Init
        WorkloadInstance instance = basicWorkloadInstance();
        WorkloadInstanceWithURLRequestDto requestPostDto = setWorkloadInstanceWithURLRequestDto();
        HelmSource helmSource = getHelmSource(HELMFILE);
        setHelmSource(instance, helmSource);
        FilePathDetails paths = preparePaths(helmPath, valuesPath, kubeConfigPath);

        when(helmfileMapper.toHelmfileData(any(WorkloadInstanceWithChartsRequestDto.class))).thenReturn(new HelmfileData());
        when(converter.convertHelmfile(any(), any())).thenReturn(Path.of(HELMSOURCE_PATH));
        when(helmSourceService.executeHelmSource(any(), anyInt(), any(), any(), any())).thenReturn(getOperation());
        when(helmSourceService.resolveHelmSourceType(Path.of(HELMSOURCE_PATH))).thenReturn(HELMFILE);
        when(helmSourceService.create(any(), any(), any())).thenReturn(helmSource);
        when(helmSourceService.downloadHelmSource(anyString(), anyBoolean())).thenReturn(Path.of(HELMSOURCE_PATH));
        when(fileService.createFile(any(), any(), anyString())).thenReturn(Path.of(HELMSOURCE_PATH));
        when(storeFileHelper.getValuesPathFromDirectory(any(), any(), any(), any(), eq(Boolean.TRUE))).thenReturn(paths);

        //Test method
        instantiateService.instantiate(requestPostDto, false, instance, values, clusterConnectionInfo);

        //Verify
        verify(helmSourceService).resolveHelmSourceType(Path.of(HELMSOURCE_PATH));
        verify(dockerRegistrySecretHelper).createSecret(instance, kubeConfigPath);
        verify(helmSourceService).executeHelmSource(helmSource, 5, OperationType.INSTANTIATE, paths, null);
        verify(secretsManagement).createOrUpdateSecretsInNamespace(any(), eq(NAMESPACE), any());
        verify(clusterConnectionInfoService).resolveClusterIdentifier(any());
    }

    @Test
    void shouldSuccessfulInstantiateWorkloadInstanceOfHelmfileByURLWithAuthorization() {
        //Init
        WorkloadInstance instance = basicWorkloadInstance();
        WorkloadInstanceWithURLRequestDto requestPostDto = setWorkloadInstanceWithURLRequestDto();
        HelmSource helmSource = getHelmSource(HELMFILE);
        ResponseEntity<byte[]> helmSourceURL = new ResponseEntity<>(new byte[]{1, 2, 3}, HttpStatus.OK);
        setHelmSource(instance, helmSource);
        FilePathDetails paths = preparePaths(helmPath, valuesPath, kubeConfigPath);

        when(converter.convertHelmfile(any(), any())).thenReturn(Path.of(HELMSOURCE_PATH));
        when(helmSourceService.executeHelmSource(any(), anyInt(), any(), any(), any())).thenReturn(getOperation());
        when(helmSourceService.resolveHelmSourceType(Path.of(HELMSOURCE_PATH))).thenReturn(HELMFILE);
        when(helmSourceService.create(any(), any(), any())).thenReturn(helmSource);
        when(helmSourceService.downloadHelmSource(anyString(), anyBoolean())).thenReturn(Path.of(HELMSOURCE_PATH));
        when(fileService.createFile(any(), any(), anyString())).thenReturn(Path.of(HELMSOURCE_PATH));
        when(storeFileHelper.getValuesPathFromDirectory(any(), any(), any(), any(), eq(Boolean.TRUE))).thenReturn(paths);

        //Test method
        instantiateService.instantiate(requestPostDto, true, instance, values, clusterConnectionInfo);

        //Verify
        verify(helmSourceService).resolveHelmSourceType(Path.of(HELMSOURCE_PATH));
        verify(dockerRegistrySecretHelper).createSecret(instance, kubeConfigPath);
        verify(helmSourceService).executeHelmSource(helmSource, 5, OperationType.INSTANTIATE, paths, null);
        verify(secretsManagement).createOrUpdateSecretsInNamespace(any(), eq(NAMESPACE), any());
        verify(clusterConnectionInfoService).resolveClusterIdentifier(any());
    }

    @Test
    void shouldFailWhenHelmSourceMissedInRequest() {
        //Init
        WorkloadInstance instance = basicWorkloadInstance();

        //Test method
        assertThatThrownBy(() -> {
            instantiateService.instantiate(instance, null, clusterConnectionInfo, values, 5);
        })
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("The helmsource.tgz is missing from the request");
    }

    private WorkloadInstance basicWorkloadInstance() {
        return WorkloadInstance.builder()
                .workloadInstanceName(WORKLOAD_INSTANCE_NAME)
                .workloadInstanceId(WORKLOAD_INSTANCE_ID)
                .latestOperationId(OPERATION_ID)
                .version(WORKLOAD_INSTANCE_VERSION)
                .namespace(NAMESPACE)
                .crdNamespace(GLOBAL_CRD_NAMESPACE_VALUE)
                .additionalParameters(ADDITIONAL_PARAMETERS)
                .clusterIdentifier(CLUSTER_IDENT)
                .build();
    }

    private void setHelmSource(WorkloadInstance instance, HelmSource helmSource) {
        List<HelmSource> helmSources = new ArrayList<>();
        helmSources.add(helmSource);
        instance.setHelmSources(helmSources);
    }

    private HelmSource getHelmSource(HelmSourceType helmSourceType) {
        return HelmSource.builder()
                .id(HELMSOURCE_ID)
                .helmSourceType(helmSourceType)
                .helmSourceVersion(HELM_SOURCE_VERSION)
                .build();
    }

    private Operation getOperation() {
        return Operation.builder()
                .id(OPERATION_ID)
                .build();
    }

    private WorkloadInstanceWithChartsRequestDto setWorkloadInstanceWithChartsPostRequestDto() {
        WorkloadInstanceWithChartsRequestDto postRequestDto = new WorkloadInstanceWithChartsRequestDto();
        postRequestDto.setWorkloadInstanceName(WORKLOAD_INSTANCE_NAME);
        postRequestDto.setNamespace(NAMESPACE);
        postRequestDto.setCharts(getCharts("release-1", "release-2"));
        postRequestDto.setRepository(REPOSITORY);
        return postRequestDto;
    }

    private WorkloadInstanceWithURLRequestDto setWorkloadInstanceWithURLRequestDto() {
        WorkloadInstanceWithURLRequestDto postRequestDto = new WorkloadInstanceWithURLRequestDto();
        postRequestDto.setWorkloadInstanceName(WORKLOAD_INSTANCE_NAME);
        postRequestDto.setNamespace(NAMESPACE);
        postRequestDto.setUrl(REPOSITORY);
        return postRequestDto;
    }

    private List<ChartDto> getCharts(String... names) {
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

    private FilePathDetails preparePaths(Path helmPath, Path valuesPath, Path kubeConfigPath) {
        return FilePathDetails.builder()
                .helmSourcePath(helmPath)
                .valuesPath(valuesPath)
                .kubeConfigPath(kubeConfigPath)
                .build();
    }
}
