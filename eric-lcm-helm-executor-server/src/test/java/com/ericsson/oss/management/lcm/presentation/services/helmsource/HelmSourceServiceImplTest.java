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

package com.ericsson.oss.management.lcm.presentation.services.helmsource;

import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.CHART_YAML;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.HELMFILE_YAML_FILENAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.management.lcm.constants.CommandConstants.EXTRACT_ARCHIVE_TIMEOUT;
import static com.ericsson.oss.management.lcm.model.entity.HelmSourceType.HELMFILE;
import static com.ericsson.oss.management.lcm.model.entity.HelmSourceType.INTEGRATION_CHART;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import com.ericsson.oss.management.lcm.utils.HttpClientUtils;
import com.ericsson.oss.management.lcm.utils.UrlUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.HelmSourceType;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.OperationState;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidFileException;
import com.ericsson.oss.management.lcm.presentation.exceptions.NotUniqueHelmSourceException;
import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import com.ericsson.oss.management.lcm.presentation.services.async.executor.AsyncExecutor;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.presentation.services.helper.command.CommandExecutorHelper;
import com.ericsson.oss.management.lcm.presentation.services.operation.OperationService;
import com.ericsson.oss.management.lcm.presentation.services.version.WorkloadInstanceVersionService;
import com.ericsson.oss.management.lcm.repositories.HelmSourceRepository;
import com.ericsson.oss.management.lcm.utils.command.builder.HelmSourceCommandBuilder;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.HttpClientErrorException;

@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {"auto-rollback.enabled=true", "security.serviceMesh.enabled=false"})
class HelmSourceServiceImplTest extends AbstractDbSetupTest {

    private static final String HELMSOURCE_ID = "some_id";
    private static final String OPERATION_ID = "some_id";
    private static final String VALID_HELMFILE_PATH = "helmfile-3.4.5-6.tgz";
    private static final String VALID_HELMFILE_VERSION = "3.4.5-6";
    private static final String HELMFILE_WITHOUT_VERSION = "helmfile-without-version.tgz";
    private static final String VALID_INTEGRATION_CHART_PATH = "integration_chart-1.2.3-4.tgz";
    private static final String INTEGRATION_CHART_WITHOUT_ROOT_CHART = "integration_chart_without_root_chart.tgz";
    private static final String INTEGRATION_CHART_WITHOUT_VERSION = "integration_chart.tgz";
    private static final String VALID_INTEGRATION_CHART_VERSION = "1.2.3-4";
    private static final String HELMFILE_WITH_REPOS = "repository.tgz";
    private static final String HELMFILE_WITHOUT_METADATA = "no-metadata-helmfile.tgz";
    private static final String HELMFILE_WITH_INVALID_HELMFILE_YAML = "invalid_helmfile.tgz";
    private static final String VALID_CLUSTER_CONNECTION_INFO_PATH = "clusterConnectionInfo.yaml";
    private static final String VALUES_YAML_PATH = "values.yaml";
    private static final String MOCK_MULTIPART_FILE_NAME = "TempFile";
    private static final int TIMEOUT = 5;
    private static final String VALID_HELMSOURCE_VERSION = "1.2.3-4";
    private static final String VALUES_VERSION = "0e35ed30-d438-4b07-a82b-cab447424d30";
    private static final String WORKLOAD_INSTANCE_NAME = "successfulpost";
    private static final String HELM_LIST_JSON_CONTENT_WITH_INSTANCE = "[{\"name\":\"successfulpost\",\"namespace\":\"namespace\"," +
            "\"revision\":\"1\",\"updated\":\"2022-11-08 08:27:28.088825031 +0200 EET\",\"status\":\"deployed\",\"chart\":" +
            "\"integration_chart-1.2.3-4\",\"app_version\":\"1.0.12-10\"}]";
    private static final String HELM_LIST_EMPTY_JSON_CONTENT = "[]";
    private static final String NAMESPACE = "namespace";
    private static final String HELM_LIST_COMMAND = "helm list --output json --filter successfulpost -n namespace";
    private static final String HELM_CASCADE_DELETE_COMMAND = "helm uninstall workloadInstanceName --wait --cascade=foreground --timeout 300s -n tom";
    private static final String HELM_SOURCE_URL = "http://localhost:8086/helm-source-url";
    private static final String AUTH_HEADER = "Basic dXNlcjpwYXNz";

    @Autowired
    private HelmSourceServiceImpl helmSourceService;
    @SpyBean
    private FileService fileService;
    @MockBean
    private HelmSourceRepository helmSourceRepository;
    @MockBean
    private OperationService operationService;
    @MockBean
    private AsyncExecutor asyncExecutor;
    @MockBean
    private HelmSourceCommandBuilder commandBuilder;
    @MockBean
    private CommandExecutorHelper commandExecutorHelper;
    @MockBean
    private WorkloadInstanceVersionService workloadInstanceVersionService;
    @MockBean
    private HttpClientUtils httpClientUtils;
    @MockBean
    private UrlUtils urlUtils;
    @Mock
    private Path helmPath;
    @Mock
    private Path valuesPath;
    @Mock
    private Path kubeConfigPath;

    @Test
    void shouldSuccessfullyCreateHelmfileWithoutOptionalArgs() throws IOException {
        final CommandResponse response = new CommandResponse();
        Path storedArchive = storeFile(VALID_HELMFILE_PATH);

        response.setExitCode(0);
        when(commandExecutorHelper.executeWithRetry(anyString(), anyInt())).thenReturn(response);
        WorkloadInstance workloadInstance = basicWorkloadInstance();
        HelmSource helmSource = basicHelmSource(workloadInstance, HelmSourceType.HELMFILE);
        when(helmSourceRepository.save(any())).thenReturn(helmSource);
        HelmSource result =
                helmSourceService.create(storedArchive, workloadInstance, HelmSourceType.HELMFILE);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(HELMSOURCE_ID);
        assertThat(result.getHelmSourceType()).isEqualTo(HelmSourceType.HELMFILE);
    }

    @Test
    void shouldSuccessfullyCreateHelmfileWithOptionalArgs() throws IOException {
        final CommandResponse response = new CommandResponse();
        Path directory = fileService.createDirectory();
        Path storedArchive = storeFileInDirectory(VALID_HELMFILE_PATH, directory);
        storeFileInDirectory(VALID_CLUSTER_CONNECTION_INFO_PATH, directory);
        storeFileInDirectory(VALUES_YAML_PATH, directory);

        response.setExitCode(0);
        when(commandExecutorHelper.executeWithRetry(anyString(), anyInt())).thenReturn(response);
        WorkloadInstance workloadInstance = basicWorkloadInstance();
        HelmSource helmSource = basicHelmSource(workloadInstance, HelmSourceType.HELMFILE);
        when(helmSourceRepository.save(any())).thenReturn(helmSource);
        fileService.extractArchive(storedArchive, 5);
        HelmSource result =
                helmSourceService.create(storedArchive, workloadInstance, HelmSourceType.HELMFILE);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(HELMSOURCE_ID);
        assertThat(result.getHelmSourceType()).isEqualTo(HelmSourceType.HELMFILE);
    }

    @Test
    void shouldSuccessfullyCreateIntegrationChartWithoutOptionalArgs() throws IOException {
        Path storedArchive = storeFile(VALID_INTEGRATION_CHART_PATH);

        WorkloadInstance workloadInstance = basicWorkloadInstance();
        HelmSource helmSource = basicHelmSource(workloadInstance, HelmSourceType.INTEGRATION_CHART);
        when(helmSourceRepository.save(any())).thenReturn(helmSource);
        HelmSource result =
                helmSourceService.create(storedArchive, workloadInstance, HelmSourceType.INTEGRATION_CHART);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(HELMSOURCE_ID);
        assertThat(result.getHelmSourceType()).isEqualTo(HelmSourceType.INTEGRATION_CHART);
    }

    @Test
    void shouldSuccessfullyCreateIntegrationChartWithOptionalArgs() throws IOException {
        Path storedArchive = storeFile(VALID_INTEGRATION_CHART_PATH);
        storeFile(VALID_CLUSTER_CONNECTION_INFO_PATH);
        storeFile(VALUES_YAML_PATH);

        WorkloadInstance workloadInstance = basicWorkloadInstance();
        HelmSource helmSource = basicHelmSource(workloadInstance, HelmSourceType.INTEGRATION_CHART);
        when(helmSourceRepository.save(any())).thenReturn(helmSource);
        HelmSource result = helmSourceService.create(storedArchive, workloadInstance,
                HelmSourceType.INTEGRATION_CHART);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(HELMSOURCE_ID);
        assertThat(result.getHelmSourceType()).isEqualTo(HelmSourceType.INTEGRATION_CHART);
    }

    @Test
    void shouldFailWhenTgzContainsRepos() throws IOException {
        Path storedArchive = storeFile(HELMFILE_WITH_REPOS);

        WorkloadInstance workloadInstance = basicWorkloadInstance();
        HelmSource helmSource = basicHelmSource(workloadInstance, HelmSourceType.HELMFILE);
        when(helmSourceRepository.save(Mockito.any())).thenReturn(helmSource);

        assertThatThrownBy(() -> helmSourceService.create(storedArchive, workloadInstance, HelmSourceType.HELMFILE))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void shouldFailWhenTgzNotContainMetadata() throws IOException {
        Path storedArchive = storeFile(HELMFILE_WITHOUT_METADATA);

        WorkloadInstance workloadInstance = basicWorkloadInstance();
        HelmSource helmSource = basicHelmSource(workloadInstance, HelmSourceType.HELMFILE);
        when(helmSourceRepository.save(Mockito.any())).thenReturn(helmSource);

        assertThatThrownBy(() -> helmSourceService.create(storedArchive, workloadInstance, HelmSourceType.HELMFILE))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void shouldFailWhenTgzContainsInvalidHelmfileYaml() throws IOException {
        Path storedArchive = storeFile(HELMFILE_WITH_INVALID_HELMFILE_YAML);

        WorkloadInstance workloadInstance = basicWorkloadInstance();
        HelmSource helmSource = basicHelmSource(workloadInstance, HelmSourceType.HELMFILE);
        when(helmSourceRepository.save(Mockito.any())).thenReturn(helmSource);

        assertThatThrownBy(() -> helmSourceService.create(storedArchive, workloadInstance, HelmSourceType.HELMFILE))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void shouldFailWhenHelmSourceVersionIsNotUnique() throws IOException {
        final CommandResponse response = new CommandResponse();
        response.setExitCode(0);
        when(commandExecutorHelper.executeWithRetry(anyString(), anyInt())).thenReturn(response);
        Path storedArchive = storeFile(VALID_HELMFILE_PATH);
        WorkloadInstance workloadInstance = basicWorkloadInstance();
        HelmSource helmSource = basicHelmSource(workloadInstance, HelmSourceType.HELMFILE);

        when(helmSourceRepository.findByWorkloadInstanceAndHelmSourceVersion(any(), anyString())).thenReturn(Optional.of(helmSource));

        assertThatThrownBy(() -> helmSourceService.create(storedArchive, workloadInstance, HelmSourceType.HELMFILE))
                .isInstanceOf(NotUniqueHelmSourceException.class);
    }

    @Test
    void shouldGetHelmfileSuccessfully() {
        HelmSource helmSource = basicHelmSource(basicWorkloadInstance(), HelmSourceType.HELMFILE);
        when(helmSourceRepository.findById(HELMSOURCE_ID)).thenReturn(Optional.of(helmSource));
        HelmSource result = helmSourceService.get(HELMSOURCE_ID);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(HELMSOURCE_ID);
    }

    @Test
    void shouldFailWhenHelmfileNotFound() {
        when(helmSourceRepository.findById(HELMSOURCE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> helmSourceService.get(HELMSOURCE_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldGetHelmfileByWorkloadInstanceSuccessfully() {
        WorkloadInstance instance = basicWorkloadInstance();
        HelmSource helmSource = basicHelmSource(instance, HelmSourceType.HELMFILE);
        WorkloadInstanceVersion version = getVersion(instance);
        when(workloadInstanceVersionService.getVersion(instance)).thenReturn(version);
        when(helmSourceRepository.findByWorkloadInstanceAndHelmSourceVersion(any(), anyString())).thenReturn(Optional.of(helmSource));

        HelmSource result = helmSourceService.get(instance);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(HELMSOURCE_ID);
    }

    @Test
    void shouldGetHelmSourceByWorkloadInstanceAndVersionSuccessfully() {
        HelmSource helmSource = basicHelmSource(basicWorkloadInstance(), HelmSourceType.HELMFILE);
        WorkloadInstance instance = basicWorkloadInstance();
        when(helmSourceRepository.findByWorkloadInstanceAndHelmSourceVersion(instance, HELMSOURCE_ID)).thenReturn(Optional.of(helmSource));
        HelmSource result = helmSourceService.getByWorkloadInstanceAndVersion(instance, HELMSOURCE_ID);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(HELMSOURCE_ID);
    }

    @Test
    void shouldFailWhenHelmSourceByWorkloadInstanceAndVersionNotFound() {
        WorkloadInstance instance = basicWorkloadInstance();
        when(helmSourceRepository.findByWorkloadInstanceAndHelmSourceVersion(instance, HELMSOURCE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> helmSourceService.getByWorkloadInstanceAndVersion(instance, HELMSOURCE_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldExecuteHelmfileSuccessfully() {
        WorkloadInstance workloadInstance = basicWorkloadInstance();
        HelmSource helmSource = basicHelmSource(workloadInstance, HelmSourceType.HELMFILE);
        Operation operation = basicInstantiateOperation(workloadInstance);
        String command = "This is valid command.";
        CommandResponse response = new CommandResponse();
        response.setExitCode(0);

        when(operationService.create(any())).thenReturn(operation);
        when(commandBuilder.apply(any(), any(), anyInt(), any())).thenReturn(command);
        FilePathDetails pathDetails = getPaths(helmPath, valuesPath, kubeConfigPath);

        Operation result = helmSourceService.executeHelmSource(helmSource, TIMEOUT, OperationType.INSTANTIATE, pathDetails, null);

        verify(asyncExecutor).executeAndUpdateOperation(operation, workloadInstance, pathDetails, helmSource, command);
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(OperationType.INSTANTIATE);
        assertThat(result.getState()).isEqualTo(OperationState.PROCESSING);
        assertThat(result.getId()).isEqualTo(OPERATION_ID);
    }

    @Test
    void shouldExecuteHelmfileSuccessfullyForRollback() {
        WorkloadInstance workloadInstance = basicWorkloadInstance();
        HelmSource helmSource = basicHelmSource(workloadInstance, HelmSourceType.HELMFILE);
        Operation operation = basicInstantiateOperation(workloadInstance);
        operation.setType(OperationType.ROLLBACK);
        String command = "This is valid command.";
        CommandResponse response = new CommandResponse();
        response.setExitCode(0);
        WorkloadInstanceVersion version =
                new WorkloadInstanceVersion("id", workloadInstance, 1, helmSource.getHelmSourceVersion(), "version",
                        helmSource.getHelmSourceVersion());

        when(operationService.create(any())).thenReturn(operation);
        when(commandBuilder.apply(any(), any(), anyInt(), any())).thenReturn(command);
        FilePathDetails pathDetails = getPaths(helmPath, valuesPath, kubeConfigPath);

        Operation result = helmSourceService.executeHelmSource(helmSource, TIMEOUT, OperationType.ROLLBACK, pathDetails, version);

        verify(asyncExecutor).executeAndUpdateOperationForRollback(operation, pathDetails, command, version, HelmSourceType.HELMFILE);
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(OperationType.ROLLBACK);
        assertThat(result.getState()).isEqualTo(OperationState.PROCESSING);
        assertThat(result.getId()).isEqualTo(OPERATION_ID);
    }

    @Test
    void shouldDestroyHelmfileSuccessfully() {
        WorkloadInstance workloadInstance = basicWorkloadInstance();
        HelmSource helmSource = basicHelmSource(workloadInstance, HelmSourceType.HELMFILE);
        Operation operation = basicTerminateOperation(workloadInstance);
        String command = "destroy helmfile";
        FilePathDetails paths = getPaths(helmPath, null, kubeConfigPath);

        when(operationService.create(any())).thenReturn(operation);
        when(commandBuilder.delete(eq(helmSource), any())).thenReturn(command);

        Operation result = helmSourceService.destroyHelmSource(helmSource, TIMEOUT, paths, false);

        verify(asyncExecutor).executeAndUpdateOperationForTerminate(operation, paths, command, workloadInstance,
                                                                    helmSource.getHelmSourceType(), false);
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(OperationType.TERMINATE);
        assertThat(result.getState()).isEqualTo(OperationState.PROCESSING);
        assertThat(result.getId()).isEqualTo(OPERATION_ID);
    }

    @Test
    void shouldSuccessfullyExtractArchiveAndCreateRepositoriesForHelmFile() throws IOException {
        WorkloadInstance workloadInstance = basicWorkloadInstance();
        HelmSource helmSource = basicHelmSource(workloadInstance, HelmSourceType.HELMFILE);
        Path storedArchive = storeFile(VALID_HELMFILE_PATH);

        helmSourceService.extractArchiveForHelmfile(helmSource, storedArchive);

        verify(fileService).extractArchive(any(), anyInt());
    }

    @Test
    void shouldNotExtractArchiveAndCreateRepositoriesForIntegrationChart() {
        WorkloadInstance workloadInstance = basicWorkloadInstance();
        HelmSource helmSource = basicHelmSource(workloadInstance, HelmSourceType.INTEGRATION_CHART);

        helmSourceService.extractArchiveForHelmfile(helmSource, helmPath);

        verify(fileService, never()).extractArchive(any(), anyInt());
    }

    @Test
    void shouldSuccessfullyVerifyHelmfile() throws IOException {
        //Init
        CommandResponse response = new CommandResponse();
        Path storedArchive = storeFile(VALID_HELMFILE_PATH);
        response.setExitCode(0);
        when(commandExecutorHelper.executeWithRetry(anyString(), anyInt())).thenReturn(response);

        //Method
        helmSourceService.verifyHelmfile(storedArchive);
    }

    @Test
    void shouldThrowExceptionWhenInvalidHelmfile() throws IOException {
        //Init
        CommandResponse response = new CommandResponse();
        Path storedArchive = storeFile(HELMFILE_WITH_INVALID_HELMFILE_YAML);
        response.setExitCode(1);
        when(commandExecutorHelper.executeWithRetry(anyString(), anyInt())).thenReturn(response);

        //Method
        assertThatThrownBy(() -> helmSourceService.verifyHelmfile(storedArchive))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void shouldSuccessfullyVerifyIntegrationChart() throws IOException {
        //Init
        CommandResponse response = new CommandResponse();
        Path storedArchive = storeFile(VALID_INTEGRATION_CHART_PATH);
        response.setExitCode(0);
        when(commandExecutorHelper.executeWithRetry(anyString(), anyInt())).thenReturn(response);

        //Method
        helmSourceService.verifyIntegrationChart(storedArchive);
    }

    @Test
    void shouldThrowExceptionWhenInvalidIntegrationChart() throws IOException {
        //Init
        CommandResponse response = new CommandResponse();
        String invalidIntegrationChart = "bad.tgz";
        Path storedArchive = storeFile(invalidIntegrationChart);
        response.setExitCode(1);
        when(commandExecutorHelper.executeWithRetry(anyString(), anyInt())).thenReturn(response);

        //Method
        assertThatThrownBy(() -> helmSourceService.verifyIntegrationChart(storedArchive))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void shouldVerifyIntegrationChart() throws IOException {
        //Init
        CommandResponse response = new CommandResponse();
        Path storedArchive = storeFile(VALID_INTEGRATION_CHART_PATH);
        response.setExitCode(0);
        when(commandExecutorHelper.executeWithRetry(anyString(), anyInt())).thenReturn(response);

        //Method
        helmSourceService.verifyHelmSource(storedArchive, storedArchive, INTEGRATION_CHART);
        verify(commandBuilder).verifyIntegrationChart(storedArchive);
    }

    @Test
    void shouldVerifyHelmfile() throws IOException {
        //Init
        CommandResponse response = new CommandResponse();
        Path storedArchive = storeFile(VALID_HELMFILE_PATH);
        response.setExitCode(0);
        when(commandExecutorHelper.executeWithRetry(anyString(), anyInt())).thenReturn(response);

        //Method
        helmSourceService.verifyHelmSource(storedArchive, storedArchive, HELMFILE);
        verify(commandBuilder).verifyHelmfile(storedArchive);
    }

    @Test
    void shouldReturnHelmfileType() throws IOException {
        //Init
        Path storedArchive = storeFile(VALID_HELMFILE_PATH);
        doReturn(true).when(fileService).checkFilePresenceInArchive(eq(HELMFILE_YAML_FILENAME), any(), anyInt());

        //Method
        HelmSourceType result = helmSourceService.resolveHelmSourceType(storedArchive);

        //Verify
        assertThat(result).isEqualTo(HELMFILE);
    }

    @Test
    void shouldReturnIntegrationChartType() throws IOException {
        //Init
        Path storedArchive = storeFile(VALID_INTEGRATION_CHART_PATH);
        doReturn(true).when(fileService).checkFilePresenceInArchive(eq(CHART_YAML), any(), anyInt());

        //Method
        HelmSourceType result = helmSourceService.resolveHelmSourceType(storedArchive);

        //Verify
        assertThat(result).isEqualTo(INTEGRATION_CHART);
    }

    @Test
    void shouldThrowExceptionWhenCannotRecognizeHelmSourType() throws IOException {
        //Init
        Path storedArchive = storeFile(HELMFILE_WITHOUT_METADATA);
        doReturn(false).when(fileService).checkFilePresenceInArchive(anyString(), any(), anyInt());

        //Method
        assertThatThrownBy(() -> helmSourceService.resolveHelmSourceType(storedArchive))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("Failed during detection helmSource type. Make sure Chart.yaml" +
                        "or hemfile.yaml is present in an archive");
    }

    @Test
    void shouldReturnHelmFileVersion() throws IOException {
        //Init
        Path storedArchive = storeFile(VALID_HELMFILE_PATH);
        fileService.extractArchive(storedArchive, EXTRACT_ARCHIVE_TIMEOUT);

        //Method
        String result = helmSourceService.getHelmSourceVersion(storedArchive, HELMFILE);

        //Verify
        assertThat(result).isEqualTo(VALID_HELMFILE_VERSION);
    }

    @Test
    void shouldRejectWhenHelmfileWithoutMetadata() throws IOException {
        //Init
        Path storedArchive = storeFile(HELMFILE_WITHOUT_METADATA);

        //Method
        assertThatThrownBy(() -> helmSourceService.getHelmSourceVersion(storedArchive, HELMFILE))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void shouldRejectWhenHelmfileWithoutVersion() throws IOException {
        //Init
        Path storedArchive = storeFile(HELMFILE_WITHOUT_VERSION);

        //Method
        assertThatThrownBy(() -> helmSourceService.getHelmSourceVersion(storedArchive, HELMFILE))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void shouldReturnHelmChartVersion() throws IOException {
        //Init
        Path storedArchive = storeFile(VALID_INTEGRATION_CHART_PATH);
        fileService.extractArchive(storedArchive, EXTRACT_ARCHIVE_TIMEOUT);

        //Method
        String result = helmSourceService.getHelmSourceVersion(storedArchive, INTEGRATION_CHART);

        //Verify
        assertThat(result).isEqualTo(VALID_INTEGRATION_CHART_VERSION);
    }

    @Test
    void shouldRejectWhenHelmChartWithoutRootChart() throws IOException {
        //Init
        Path storedArchive = storeFile(INTEGRATION_CHART_WITHOUT_ROOT_CHART);

        //Method
        assertThatThrownBy(() -> helmSourceService.getHelmSourceVersion(storedArchive, INTEGRATION_CHART))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void shouldRejectWhenHelmChartWithoutVersion() throws IOException {
        //Init
        Path storedArchive = storeFile(INTEGRATION_CHART_WITHOUT_VERSION);

        //Method
        assertThatThrownBy(() -> helmSourceService.getHelmSourceVersion(storedArchive, INTEGRATION_CHART))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void shouldSetOperationStateToCompletedWhenIntegrationChartIfInstanceNotPresentOnCluster() {
        WorkloadInstance testInstance = basicWorkloadInstance();
        HelmSource testHelmSource = basicHelmSource(testInstance, INTEGRATION_CHART);
        FilePathDetails paths = getPaths(helmPath, valuesPath, null);
        Operation terminate = basicTerminateOperation(testInstance);

        when(operationService.create(any())).thenReturn(terminate);
        when(commandBuilder.buildHelmListCommandWithFilterByName(NAMESPACE, WORKLOAD_INSTANCE_NAME, null))
                .thenReturn(HELM_LIST_COMMAND);
        when(commandExecutorHelper.executeWithRetry(HELM_LIST_COMMAND, TIMEOUT))
                .thenReturn(new CommandResponse(HELM_LIST_EMPTY_JSON_CONTENT, 0));

        Operation result = helmSourceService.destroyHelmSource(testHelmSource, TIMEOUT, paths, false);

        verify(operationService, times(2)).create(any());
        verify(commandBuilder).buildHelmListCommandWithFilterByName(NAMESPACE, WORKLOAD_INSTANCE_NAME, null);
        verify(commandExecutorHelper).executeWithRetry(HELM_LIST_COMMAND, TIMEOUT);
        verify(commandBuilder, never()).delete(any(), any());
        assertThat(result).isNotNull();
        assertThat(result.getState()).isEqualTo(OperationState.COMPLETED);
    }

    @Test
    void shouldTerminateInstanceWhenIntegrationChartIfInstancePresentOnCluster() {
        WorkloadInstance testInstance = basicWorkloadInstance();
        HelmSource testHelmSource = basicHelmSource(testInstance, INTEGRATION_CHART);
        FilePathDetails paths = getPaths(helmPath, valuesPath, null);
        Operation terminate = basicTerminateOperation(testInstance);

        when(operationService.create(any())).thenReturn(terminate);
        when(commandBuilder.buildHelmListCommandWithFilterByName(NAMESPACE, WORKLOAD_INSTANCE_NAME, null))
                .thenReturn(HELM_LIST_COMMAND);
        when(commandBuilder.delete(testHelmSource, paths)).thenReturn(HELM_CASCADE_DELETE_COMMAND);
        when(commandExecutorHelper.executeWithRetry(HELM_LIST_COMMAND, TIMEOUT))
                .thenReturn(new CommandResponse(HELM_LIST_JSON_CONTENT_WITH_INSTANCE, 0));

        Operation result = helmSourceService.destroyHelmSource(testHelmSource, TIMEOUT, paths, false);

        verify(operationService).create(any());
        verify(commandBuilder).buildHelmListCommandWithFilterByName(NAMESPACE, WORKLOAD_INSTANCE_NAME, null);
        verify(commandExecutorHelper).executeWithRetry(HELM_LIST_COMMAND, TIMEOUT);
        verify(commandBuilder).delete(testHelmSource, paths);
        verify(asyncExecutor).executeAndUpdateOperationForTerminate(terminate, paths, HELM_CASCADE_DELETE_COMMAND, testInstance,
                                                                    testHelmSource.getHelmSourceType(), false);
        assertThat(result).isNotNull();
        assertThat(result.getState()).isEqualTo(OperationState.PROCESSING);
    }

    @Test
    void shouldFailWhenHelmfileByURLNotFound() {
        when(httpClientUtils.executeHttpRequest(any(), any(), any(), any(), eq(byte[].class)))
                .thenThrow(HttpClientErrorException.class);

        //Test method
        assertThatThrownBy(() -> helmSourceService
                .downloadHelmSource(HELM_SOURCE_URL, true))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("was NOT FETCHED");
    }

    @Test
    void shouldSendProperRequestWhenHelmfileDowloadByURL() {
        ResponseEntity<byte[]> helmSourceURL = new ResponseEntity<>(new byte[]{1, 2, 3}, HttpStatus.OK);
        when(httpClientUtils.executeHttpRequest(any(), any(), any(), any(), eq(byte[].class))).thenReturn(helmSourceURL);
        when(urlUtils.authenticationHeader(any(), any())).thenReturn(AUTH_HEADER);

        helmSourceService
                .downloadHelmSource(HELM_SOURCE_URL, true);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, AUTH_HEADER);
        verify(httpClientUtils).executeHttpRequest(eq(headers), anyString(), eq(HttpMethod.GET), any(), any());
    }

    private Path storeFile(String name) throws IOException {
        File file = new ClassPathResource(name).getFile();
        Path directory = fileService.createDirectory();
        MockMultipartFile multipartFile = new MockMultipartFile(MOCK_MULTIPART_FILE_NAME, new FileInputStream(file));
        return fileService.storeFileIn(directory, multipartFile, name);
    }

    private Path storeFileInDirectory(String name, Path directory) throws IOException {
        File file = new ClassPathResource(name).getFile();
        MockMultipartFile multipartFile = new MockMultipartFile(MOCK_MULTIPART_FILE_NAME, new FileInputStream(file));
        return fileService.storeFileIn(directory, multipartFile, name);
    }

    private Operation basicInstantiateOperation(WorkloadInstance workloadInstance) {
        return Operation.builder()
                .helmSourceId(HELMSOURCE_ID)
                .workloadInstance(workloadInstance)
                .type(OperationType.INSTANTIATE)
                .startTime(LocalDateTime.now())
                .state(OperationState.PROCESSING)
                .id(OPERATION_ID)
                .build();
    }

    private Operation basicTerminateOperation(WorkloadInstance workloadInstance) {
        return Operation.builder()
                .helmSourceId(HELMSOURCE_ID)
                .workloadInstance(workloadInstance)
                .type(OperationType.TERMINATE)
                .startTime(LocalDateTime.now())
                .state(OperationState.PROCESSING)
                .id(OPERATION_ID)
                .build();
    }

    private WorkloadInstance basicWorkloadInstance() {
        return WorkloadInstance.builder()
                .workloadInstanceId("fake_id")
                .workloadInstanceName(WORKLOAD_INSTANCE_NAME)
                .cluster("cluster")
                .namespace(NAMESPACE)
                .additionalParameters("some additional parameters here")
                .build();
    }

    private HelmSource basicHelmSource(WorkloadInstance workloadInstance, HelmSourceType type) {
        return HelmSource.builder()
                .content(new byte[]{})
                .helmSourceType(type)
                .workloadInstance(workloadInstance)
                .created(LocalDateTime.now())
                .id(HELMSOURCE_ID)
                .build();
    }

    private FilePathDetails getPaths(Path helmSourcePath, Path valuesPath, Path kubeConfigPath) {
        return FilePathDetails.builder()
                .helmSourcePath(helmSourcePath)
                .valuesPath(valuesPath)
                .kubeConfigPath(kubeConfigPath)
                .build();
    }

    private WorkloadInstanceVersion getVersion(WorkloadInstance instance) {
        return WorkloadInstanceVersion.builder()
                .workloadInstance(instance)
                .valuesVersion(VALUES_VERSION)
                .helmSourceVersion(VALID_HELMSOURCE_VERSION)
                .id("some_random_id")
                .build();
    }

}
