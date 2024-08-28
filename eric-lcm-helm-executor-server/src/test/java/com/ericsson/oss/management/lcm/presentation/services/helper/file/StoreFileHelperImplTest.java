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

package com.ericsson.oss.management.lcm.presentation.services.helper.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.management.lcm.constants.CommandConstants.DOCKER_CREDS_SECRET_NAME;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.DOCKER_URL_KEY;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.GLOBAL_APP_ENABLED_ARGUMENT;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.GLOBAL_APP_NAMESPACE;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.GLOBAL_CHART_REGISTRY;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.GLOBAL_CRD_NAMESPACE;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.GLOBAL_PULL_SECRET;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.model.entity.ClusterConnectionInfo;
import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;
import com.ericsson.oss.management.lcm.presentation.exceptions.NotValidClusterNameException;
import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import com.ericsson.oss.management.lcm.presentation.services.clusterconnectioninfo.ClusterConnectionInfoService;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.presentation.services.values.ValuesService;
import com.ericsson.oss.management.lcm.presentation.services.version.WorkloadInstanceVersionService;
import com.ericsson.oss.management.lcm.utils.JSONParseUtils;
import com.ericsson.oss.management.lcm.utils.ValuesFileComposer;

@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = { "container-registry.url=armdocker.rnd.ericsson.se", "helmrepo.name=test-chart-registry" })
class StoreFileHelperImplTest extends AbstractDbSetupTest {

    private static final String NAMESPACE = "namespace";
    private static final String CLUSTER = "cluster";
    private static final String WORKLOAD_INSTANCE_ID = "wrkld_instance_id";
    private static final String WORKLOAD_INSTANCE_NAME = "workloadInstanceName";
    private static final String ADDITIONAL_PARAMETERS = "{\"key\": \"value\"}";
    private static final String ADDITIONAL_PARAMETERS_WITH_DOCKER_URL_FROM_USER =
            "{\"key\": \"value\", \"global.registry.url\": \"test-url\"}";
    private static final String ADDITIONAL_PARAMETERS_WITH_PREFIX =
            "{\"image.repo.prefix\": \"appmgr\"}";
    private static final String DEFAULT_DOCKER_URL_VALUE = "armdocker.rnd.ericsson.se";
    private static final String DEFAULT_GLOBAL_CRD_NAMESPACE_VALUE = "eric-crd-ns";
    private static final String DEFAULT_GLOBAL_APP_NAMESPACE_VALUE = "namespace";
    private static final String USER_DOCKER_URL_VALUE = "test-url";
    private static final String DOCKER_URL_VALUE_WITH_PREFIX = DEFAULT_DOCKER_URL_VALUE + "/appmgr";
    private static final String USER_GLOBAL_CRD_NAMESPACE_VALUE = "test-app-namespace";
    private static final String USER_GLOBAL_APP_NAMESPACE_VALUE = "test-crd-namespace";
    private static final String GLOBAL_CHART_REGISTRY_VALUE = "test-chart-registry";
    private static final String VALID_HELMSOURCE_VERSION = "1.2.3-4";
    private static final String VALID_VALUES_VERSION = "some-version";
    private static final String ERIC_OSS_PERFORMANCE_MONITORING_ENABLER = "eric-oss-performance-monitoring-enabler";
    private static final String VALUES_YAML = "values.yaml";

    @Autowired
    private StoreFileHelper storeFileHelper;

    @MockBean
    private FileService fileService;

    @MockBean
    private ClusterConnectionInfoService clusterConnectionInfoService;

    @MockBean
    private ValuesService valuesService;

    @MockBean
    private WorkloadInstanceVersionService workloadInstanceVersionService;

    @MockBean
    private ValuesFileComposer valuesFileComposer;

    @Mock
    private Path directory;

    @Mock
    private Path valuesPath;

    @Mock
    private MultipartFile values;

    @Mock
    private File file;

    @TempDir
    private Path folder;

    @Test
    void shouldContainNeededValuesWhenFillAdditionalParameters() {
        //Init
        HelmSource helmSource = getHelmSource();
        WorkloadInstance instance = basicWorkloadInstance(null);
        Map<String, Object> expectedParameters = JSONParseUtils.parseJsonToMap(instance.getAdditionalParameters());
        expectedParameters.put(DOCKER_URL_KEY, DEFAULT_DOCKER_URL_VALUE);
        expectedParameters.put(GLOBAL_CRD_NAMESPACE, DEFAULT_GLOBAL_CRD_NAMESPACE_VALUE);
        expectedParameters.put(GLOBAL_APP_NAMESPACE, DEFAULT_GLOBAL_APP_NAMESPACE_VALUE);
        expectedParameters.put(GLOBAL_CHART_REGISTRY, GLOBAL_CHART_REGISTRY_VALUE);
        expectedParameters.put(GLOBAL_APP_ENABLED_ARGUMENT, true);
        expectedParameters.put(GLOBAL_PULL_SECRET, String.format(DOCKER_CREDS_SECRET_NAME, instance.getWorkloadInstanceName()));
        when(fileService.storeFileInIfPresent(any(), any(), anyString())).thenReturn(Optional.of(valuesPath));
        when(valuesPath.toFile()).thenReturn(file);

        //testMethod
        storeFileHelper.getValuesPath(directory, values, instance, helmSource, true, true);

        //Verify
        verify(valuesFileComposer).compose(any(), eq(expectedParameters));
        assertThat(helmSource.isValuesRefreshed()).isTrue();
    }

    @Test
    void shouldRetrieveValuesFromDirectoryWhenTheyAreNotInRequest() throws IOException {
        Path integrationChart = Files.createDirectory(folder.resolve(ERIC_OSS_PERFORMANCE_MONITORING_ENABLER));
        Path values = Files.createFile(integrationChart.resolve(VALUES_YAML));
        HelmSource helmSource = getHelmSource();
        WorkloadInstance instance = basicWorkloadInstance(null);
        instance.setAdditionalParameters(null);
        when(valuesFileComposer.compose(any(), any())).thenReturn(values);
        when(fileService.fileExists(any())).thenReturn(true);

        Path result = storeFileHelper.getValuesPathFromDirectory(integrationChart, null, instance, helmSource, true).getValuesPath();

        //Verify
        assertThat(result)
                .isNotNull()
                .isEqualTo(values);
    }

    @Test
    void shouldNotRetrieveValuesFromDirectoryWhenTheyAreNested() throws IOException {
        Path directory = Files.createDirectory(folder.resolve(ERIC_OSS_PERFORMANCE_MONITORING_ENABLER));
        Path nestedDir = Files.createDirectory(directory.resolve("test"));
        Files.createFile(nestedDir.resolve(VALUES_YAML));
        HelmSource helmSource = getHelmSource();
        WorkloadInstance instance = basicWorkloadInstance(null);
        when(valuesFileComposer.compose(any(), any())).thenReturn(Path.of("any"));
        when(fileService.fileExists(any())).thenReturn(false);

        Path result = storeFileHelper.getValuesPathFromDirectory(directory, null, instance, helmSource, true).getValuesPath();

        //Verify
        assertThat(result).isNotNull();
    }

    @Test
    void shouldContainCorrectOrderOfPrecedenceWhenFillAdditionalParameters() {
        //Init
        HelmSource helmSource = getHelmSource();
        WorkloadInstance instance = basicWorkloadInstance(null);
        instance.setNamespace(USER_GLOBAL_APP_NAMESPACE_VALUE);
        instance.setCrdNamespace(USER_GLOBAL_CRD_NAMESPACE_VALUE);
        instance.setAdditionalParameters(ADDITIONAL_PARAMETERS_WITH_DOCKER_URL_FROM_USER);
        Map<String, Object> expectedParameters = JSONParseUtils.parseJsonToMap(instance.getAdditionalParameters());
        expectedParameters.put(DOCKER_URL_KEY, USER_DOCKER_URL_VALUE);
        expectedParameters.put(GLOBAL_CRD_NAMESPACE, USER_GLOBAL_CRD_NAMESPACE_VALUE);
        expectedParameters.put(GLOBAL_APP_NAMESPACE, USER_GLOBAL_APP_NAMESPACE_VALUE);
        expectedParameters.put(GLOBAL_CHART_REGISTRY, GLOBAL_CHART_REGISTRY_VALUE);
        expectedParameters.put(GLOBAL_APP_ENABLED_ARGUMENT, true);
        expectedParameters.put(GLOBAL_PULL_SECRET, String.format(DOCKER_CREDS_SECRET_NAME, instance.getWorkloadInstanceName()));
        when(fileService.storeFileInIfPresent(any(), any(), anyString())).thenReturn(Optional.of(valuesPath));
        when(valuesPath.toFile()).thenReturn(file);

        //testMethod
        storeFileHelper.getValuesPath(directory, values, instance, helmSource, true, true);

        //Verify
        verify(valuesFileComposer).compose(any(), eq(expectedParameters));
        assertThat(helmSource.isValuesRefreshed()).isTrue();
    }

    @Test
    void shouldMergeParametersSuccessfully() {
        //Init
        WorkloadInstance instance = basicWorkloadInstance(null);
        instance.setNamespace(USER_GLOBAL_APP_NAMESPACE_VALUE);
        instance.setCrdNamespace(USER_GLOBAL_CRD_NAMESPACE_VALUE);
        instance.setAdditionalParameters(ADDITIONAL_PARAMETERS_WITH_DOCKER_URL_FROM_USER);
        Map<String, Object> expectedParameters = JSONParseUtils.parseJsonToMap(instance.getAdditionalParameters());
        expectedParameters.put(DOCKER_URL_KEY, USER_DOCKER_URL_VALUE);
        expectedParameters.put(GLOBAL_CRD_NAMESPACE, USER_GLOBAL_CRD_NAMESPACE_VALUE);
        expectedParameters.put(GLOBAL_APP_NAMESPACE, USER_GLOBAL_APP_NAMESPACE_VALUE);
        expectedParameters.put(GLOBAL_CHART_REGISTRY, GLOBAL_CHART_REGISTRY_VALUE);
        expectedParameters.put(GLOBAL_APP_ENABLED_ARGUMENT, true);
        expectedParameters.put(GLOBAL_PULL_SECRET, String.format(DOCKER_CREDS_SECRET_NAME, instance.getWorkloadInstanceName()));

        //testMethod
        storeFileHelper.mergeParamsToValues(valuesPath, instance, true);

        //Verify
        verify(valuesFileComposer).compose(any(), eq(expectedParameters));
    }

    @Test
    void shouldMergeParametersWithRepoPrefixSuccessfully() {
        //Init
        WorkloadInstance instance = basicWorkloadInstance(null);
        instance.setNamespace(USER_GLOBAL_APP_NAMESPACE_VALUE);
        instance.setCrdNamespace(USER_GLOBAL_CRD_NAMESPACE_VALUE);
        instance.setAdditionalParameters(ADDITIONAL_PARAMETERS_WITH_PREFIX);
        Map<String, Object> expectedParameters = getExpectedParamsWithRegistryPrefix(instance.getWorkloadInstanceName());

        //testMethod
        storeFileHelper.mergeParamsToValues(valuesPath, instance, true);

        //Verify
        verify(valuesFileComposer).compose(any(), eq(expectedParameters));
    }

    @Test
    void shouldGetPathOfExistingClusterIfCLusterConnectionInfoNotExistInRequest() {
        //Init
        WorkloadInstance instance = basicWorkloadInstance("cluster");

        ClusterConnectionInfo clusterConnectionInfo = new ClusterConnectionInfo();
        clusterConnectionInfo.setContent("This is a connection info".getBytes());
        when(clusterConnectionInfoService.findByClusterName(anyString())).thenReturn(Optional.of(clusterConnectionInfo));

        Path clusterConnectionInfoPath = Paths.get("path/to/connection_info");
        when(fileService.createFile(any(), any(), any())).thenReturn(clusterConnectionInfoPath);

        //Test Method
        Path result = storeFileHelper.getKubeConfigPath(directory, instance, null);

        //Verify
        assertThat(result).isEqualTo(clusterConnectionInfoPath);
    }

    @Test
    void shouldThrowNotValidClusterNameExceptionWhenNonExistingCluster() {
        WorkloadInstance instance = basicWorkloadInstance(CLUSTER);
        when(clusterConnectionInfoService.findByClusterName(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeFileHelper.getKubeConfigPath(directory, instance, null))
                .isInstanceOf(NotValidClusterNameException.class)
                .hasMessageContaining("cluster not found");
    }

    @Test
    void shouldNotRetrieveValuesWhenValuesExistInRequest() {
        HelmSource helmSource = getHelmSource();
        WorkloadInstance instance = basicWorkloadInstance(null);
        when(fileService.storeFileInIfPresent(any(), any(), anyString())).thenReturn(Optional.of(valuesPath));
        when(valuesPath.toFile()).thenReturn(file);

        storeFileHelper.getValuesPath(directory, values, instance, helmSource, true, true);

        verify(valuesService, never()).retrieve(any(), any(), any());
        verify(valuesFileComposer).compose(any(), any());
        assertThat(helmSource.isValuesRefreshed()).isTrue();
    }

    @Test
    void shouldRetrieveValuesWhenValuesNotExistInRequest() {
        HelmSource helmSource = getHelmSource();
        WorkloadInstance instance = basicWorkloadInstance(null);
        instance.setAdditionalParameters(null);
        when(valuesService.retrieve(any(), any(), any())).thenReturn(valuesPath);
        when(valuesFileComposer.compose(eq(valuesPath), any())).thenReturn(valuesPath);
        when(valuesPath.toFile()).thenReturn(file);

        Path result = storeFileHelper.getValuesPath(directory, null, instance, helmSource, true, false).getValuesPath();

        verify(valuesService).retrieve(any(), any(), any());
        assertThat(result).isEqualTo(valuesPath);
        assertThat(helmSource.isValuesRefreshed()).isFalse();
    }

    @Test
    void shouldOverrideUserAdditionalParamsWhenPreExistingValuesFromStorage() {
        WorkloadInstance instance = basicWorkloadInstance(null);
        HelmSource helmSource = getHelmSource();
        when(valuesService.retrieve(any(), any(), any())).thenReturn(valuesPath);
        when(valuesPath.toFile()).thenReturn(file);

        storeFileHelper.getValuesPath(directory, null, instance, helmSource, true, true);

        verify(valuesService).retrieve(eq(WORKLOAD_INSTANCE_NAME), eq(VALID_HELMSOURCE_VERSION), any());
        verify(valuesFileComposer).compose(any(), any());
        assertThat(helmSource.isValuesRefreshed()).isTrue();
    }

    @Test
    void shouldOverrideUserAdditionalParamsWhenPreviousValuesFromStorage() {
        WorkloadInstance instance = basicWorkloadInstance(null);
        WorkloadInstanceVersion version = basicWorkloadInstanceVersion(instance);
        HelmSource helmSource = getHelmSource();
        when(workloadInstanceVersionService.getVersion(instance)).thenReturn(version);
        when(valuesService.retrieveByVersion(any(), any(), any())).thenReturn(valuesPath);
        when(valuesPath.toFile()).thenReturn(file);

        storeFileHelper.getValuesPath(directory, null, instance, helmSource, true, true);

        verify(valuesService).retrieveByVersion(eq(WORKLOAD_INSTANCE_NAME), eq(version), any());
        verify(valuesFileComposer).compose(any(), any());
        assertThat(helmSource.isValuesRefreshed()).isTrue();
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenValuesNotExistInStorageAndRequest() {
        WorkloadInstance instance = basicWorkloadInstance(null);
        instance.setAdditionalParameters(null);
        when(valuesService.retrieve(any(), any(), any())).thenThrow(ResourceNotFoundException.class);
        HelmSource helmSource = getHelmSource();

        assertThatThrownBy(() -> {
            storeFileHelper.getValuesPath(directory, null, instance, helmSource, true, false);
        }).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("There is no day-0-config, values.yaml and values in storage. You need to have at least one.");
    }

    private WorkloadInstance basicWorkloadInstance(String cluster) {
        return WorkloadInstance.builder()
                .workloadInstanceName(WORKLOAD_INSTANCE_NAME)
                .workloadInstanceId(WORKLOAD_INSTANCE_ID)
                .cluster(cluster)
                .namespace(NAMESPACE)
                .crdNamespace(DEFAULT_GLOBAL_CRD_NAMESPACE_VALUE)
                .additionalParameters(ADDITIONAL_PARAMETERS)
                .build();
    }

    private WorkloadInstanceVersion basicWorkloadInstanceVersion(WorkloadInstance workloadInstance) {
        return WorkloadInstanceVersion.builder()
                .workloadInstance(workloadInstance)
                .version(1)
                .helmSourceVersion(VALID_HELMSOURCE_VERSION)
                .valuesVersion(VALID_VALUES_VERSION)
                .build();
    }

    private HelmSource getHelmSource() {
        return HelmSource.builder()
                .helmSourceVersion(VALID_HELMSOURCE_VERSION)
                .build();
    }

    private Map<String, Object> getExpectedParamsWithRegistryPrefix(String workloadInstanceName) {
        Map<String, Object> expectedParameters = new HashMap<>();
        expectedParameters.put(DOCKER_URL_KEY, DOCKER_URL_VALUE_WITH_PREFIX);
        expectedParameters.put(GLOBAL_CRD_NAMESPACE, USER_GLOBAL_CRD_NAMESPACE_VALUE);
        expectedParameters.put(GLOBAL_APP_NAMESPACE, USER_GLOBAL_APP_NAMESPACE_VALUE);
        expectedParameters.put(GLOBAL_CHART_REGISTRY, GLOBAL_CHART_REGISTRY_VALUE);
        expectedParameters.put(GLOBAL_APP_ENABLED_ARGUMENT, true);
        expectedParameters.put(GLOBAL_PULL_SECRET, String.format(DOCKER_CREDS_SECRET_NAME, workloadInstanceName));
        return expectedParameters;
    }
}
