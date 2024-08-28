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

package com.ericsson.oss.management.lcm.presentation.services.release;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.model.entity.Release;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.OperationState;
import com.ericsson.oss.management.lcm.model.entity.HelmSourceType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;
import com.ericsson.oss.management.lcm.presentation.services.helper.command.CommandExecutorHelper;
import com.ericsson.oss.management.lcm.repositories.ReleaseRepository;
import com.ericsson.oss.management.lcm.utils.ReleaseParser;
import com.ericsson.oss.management.lcm.utils.command.builder.HelmSourceCommandBuilder;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandResponse;

@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {"operation.timeout=5"})
class ReleaseServiceImplTest extends AbstractDbSetupTest {

    @Autowired
    private ReleaseServiceImpl releaseService;
    @Autowired
    private ReleaseParser parser;
    @MockBean
    private HelmSourceCommandBuilder builder;
    @MockBean
    private CommandExecutorHelper commandExecutorHelper;
    @MockBean
    private ReleaseRepository repository;

    private static final String VALUES_YAML_PATH = "env-profiles/values.yaml";
    private static final String KUBE_CONFIG_YAML_PATH = "env-profiles/kubeConfigs.yaml";
    private static final String LIST_COMMAND = "cd env-profiles ; KUBECONFIG=env-profiles/kubeConfigs.yaml helmfile --state-values-file "
            + "env-profiles/disable-everything.yaml --selector namespace=tom list "
            + "--output json";
    private static final String DELETE_RELEASE_COMMAND = "KUBECONFIG=env-profiles/kubeConfigs.yaml helm -n tom delete service-c";
    private static final String LIST_OUTPUT = "["
            + "{\"name\":\"cn-am-test-app-a\",\"namespace\":\"demo-sprint-9\",\"enabled\":true,\"labels\":\"\","
            + "\"chart\":\"test-charts/cn-am-test-app-a\",\"version\":\"\"},"
            + "{\"name\":\"cn-am-test-app-c\","
            + "\"namespace\":\"demo-sprint-9\",\"enabled\":false,\"labels\":\"\",\"chart\":\"./test-charts/cn-am-test-app-c\",\"version\":\"\"}]\n";
    private static final String WORKLOAD_INSTANCE_NAME = "workloadInstanceName";
    private static final String WORKLOAD_INSTANCE_ID = "098j2-sawdh38-asndku-728683";
    private static final String NAMESPACE = "namespace";
    private static final int TIMEOUT = 5;
    private static final String SERVICE_A = "cn-am-test-app-a";
    private static final String SERVICE_B = "cn-am-test-app-b";
    private static final String SERVICE_C = "cn-am-test-app-c";
    private static final String SERVICE_D = "cn-am-test-app-d";
    private static final String HELMSOURCE_ID = "some_id";
    private static final String OPERATION_ID = "some_id";
    private static final String RELEASE_ERROR_MSG = "Handling orphaned releases response";

    private static Path valuesPath;
    private static Path kubeConfigPath;

    @BeforeAll
    static void setup() {
        valuesPath = Path.of(VALUES_YAML_PATH);
        kubeConfigPath = Path.of(KUBE_CONFIG_YAML_PATH);
    }

    @Test
    void shouldExtractAndSaveReleasesSuccessfully() {
        FilePathDetails paths = getPaths(valuesPath, kubeConfigPath);
        WorkloadInstance workloadInstance = getWorkloadInstance();
        CommandResponse commandResponse = getCommand(LIST_OUTPUT);
        List<Release> list = parser.parse(commandResponse.getOutput());
        list.forEach(item -> item.setWorkloadInstance(workloadInstance));

        when(builder.buildListCommand(paths, NAMESPACE)).thenReturn(LIST_COMMAND);
        when(commandExecutorHelper.executeWithRetry(LIST_COMMAND, TIMEOUT)).thenReturn(commandResponse);
        when(repository.saveAll(any())).thenReturn(list);

        CommandResponse result = releaseService.extractAndSaveReleases(paths, workloadInstance);

        assertThat(result).isNotNull();

        verify(repository).saveAll(list);
    }

    @Test
    void shouldHandleOrphanedReleasesSuccessfully() {
        FilePathDetails paths = getPaths(valuesPath, kubeConfigPath);
        WorkloadInstance workloadInstance = getWorkloadInstance();
        CommandResponse commandResponse = getCommand(LIST_OUTPUT);
        Operation operation = getOperation(workloadInstance);
        List<Release> list = parser.parse(commandResponse.getOutput());
        list.forEach(item -> item.setWorkloadInstance(workloadInstance));

        when(builder.buildListCommand(paths, NAMESPACE)).thenReturn(LIST_COMMAND);
        when(commandExecutorHelper.executeWithRetry(LIST_COMMAND, TIMEOUT)).thenReturn(commandResponse);
        when(repository.saveAll(any())).thenReturn(list);

        CommandResponse result = releaseService.handleOrphanedReleases(operation, workloadInstance, paths,
                HelmSourceType.HELMFILE, commandResponse);


        verify(repository).saveAll(list);
        assertThat(result.getExitCode()).isZero();
    }

    @Test
    void shouldHandleOrphanedReleasesFailed() {
        FilePathDetails paths = getPaths(valuesPath, kubeConfigPath);
        WorkloadInstance workloadInstance = getWorkloadInstance();
        CommandResponse commandResponse = getCommand(LIST_OUTPUT);
        commandResponse.setExitCode(1);
        Operation operation = getOperationRollbackType(workloadInstance);
        List<Release> list = parser.parse(commandResponse.getOutput());
        list.forEach(item -> item.setWorkloadInstance(workloadInstance));

        when(builder.buildListCommand(paths, NAMESPACE)).thenReturn(LIST_COMMAND);
        when(commandExecutorHelper.executeWithRetry(LIST_COMMAND, TIMEOUT)).thenReturn(getFailedCommand());
        when(repository.saveAll(any())).thenReturn(list);

        CommandResponse result = releaseService.handleOrphanedReleases(operation, workloadInstance, paths,
                HelmSourceType.HELMFILE, commandResponse);

        assertThat(result.getExitCode()).isEqualTo(1);
    }

    @Test
    void shouldExtractAndSaveNothingForEmptyResponseSuccessfully() {
        FilePathDetails paths = getPaths(valuesPath, kubeConfigPath);
        WorkloadInstance workloadInstance = getWorkloadInstance();
        CommandResponse commandResponse = getCommand(LIST_OUTPUT);
        commandResponse.setOutput("[]");

        when(builder.buildListCommand(paths, NAMESPACE)).thenReturn(LIST_COMMAND);
        when(commandExecutorHelper.executeWithRetry(LIST_COMMAND, TIMEOUT)).thenReturn(commandResponse);
        when(repository.saveAll(any())).thenReturn(new ArrayList<>());

        CommandResponse result = releaseService.extractAndSaveReleases(paths, workloadInstance);

        assertThat(result).isNotNull();

        verify(repository).saveAll(any());
    }

    @Test
    void shouldNoSaveIfExecutionFailed() {
        FilePathDetails paths = getPaths(valuesPath, kubeConfigPath);
        WorkloadInstance workloadInstance = getWorkloadInstance();
        CommandResponse commandResponse = getCommand(LIST_OUTPUT);
        commandResponse.setExitCode(1);

        when(builder.buildListCommand(paths, NAMESPACE)).thenReturn(LIST_COMMAND);
        when(commandExecutorHelper.executeWithRetry(LIST_COMMAND, TIMEOUT)).thenReturn(commandResponse);

        CommandResponse result = releaseService.extractAndSaveReleases(paths, workloadInstance);

        assertThat(result).isNotNull();

        verify(repository, times(0)).saveAll(any());
    }

    @Test
    void shouldGetListOfReleasesSuccessfully() {
        WorkloadInstance workloadInstance = getWorkloadInstance();
        Release release = getRelease(workloadInstance);
        when(repository.findByWorkloadInstance(workloadInstance)).thenReturn(Collections.singletonList(release));

        List<Release> result = releaseService.getByWorkloadInstance(workloadInstance);

        assertThat(result).isNotNull();
        verify(repository).findByWorkloadInstance(any());
    }

    @Test
    void shouldDeleteOrphanedReleasesSuccessfully() {
        FilePathDetails paths = getPaths(valuesPath, kubeConfigPath);
        WorkloadInstance workloadInstance = getWorkloadInstance();
        CommandResponse listCommand = getCommand(LIST_OUTPUT);
        List<Release> storedReleases = getReleases(workloadInstance, SERVICE_A, SERVICE_B, SERVICE_C);
        CommandResponse deleteCommand = getCommand(DELETE_RELEASE_COMMAND);
        List<Release> orphanedReleases = getReleases(workloadInstance, SERVICE_B);
        Operation operation = getOperationRollbackType(workloadInstance);

        when(builder.buildListCommand(paths, NAMESPACE)).thenReturn(LIST_COMMAND);
        when(commandExecutorHelper.executeWithRetry(LIST_COMMAND, TIMEOUT)).thenReturn(listCommand);
        when(repository.findByWorkloadInstance(workloadInstance)).thenReturn(storedReleases);
        when(builder.deleteReleases(orphanedReleases, NAMESPACE, paths.getKubeConfigPath())).thenReturn(DELETE_RELEASE_COMMAND);
        when(commandExecutorHelper.executeWithRetry(DELETE_RELEASE_COMMAND, TIMEOUT)).thenReturn(deleteCommand);

        CommandResponse response = releaseService.handleOrphanedReleases(operation, workloadInstance, paths,
                HelmSourceType.HELMFILE, listCommand);

        assertThat(response).isNotNull();

        verify(repository).deleteAll(orphanedReleases);
        verify(repository).saveAll(any());
    }

    @Test
    void shouldNotDeleteReleasesIfNoOrphanedSuccessfully() {
        FilePathDetails paths = getPaths(valuesPath, kubeConfigPath);
        WorkloadInstance workloadInstance = getWorkloadInstance();
        String deployedReleases = getReleasesListInput(SERVICE_A, SERVICE_C);
        CommandResponse listCommand = getCommand(deployedReleases);
        List<Release> storedReleases = getReleases(workloadInstance, SERVICE_A, SERVICE_C);
        List<Release> orphanedReleases = new ArrayList<>();
        Operation operation = getOperationTerminateType(workloadInstance);

        when(builder.buildListCommand(paths, NAMESPACE)).thenReturn(LIST_COMMAND);
        when(commandExecutorHelper.executeWithRetry(LIST_COMMAND, TIMEOUT)).thenReturn(listCommand);
        when(repository.findByWorkloadInstance(workloadInstance)).thenReturn(storedReleases);

        CommandResponse response = releaseService.handleOrphanedReleases(operation, workloadInstance, paths,
                HelmSourceType.HELMFILE, listCommand);

        assertThat(response).isNotNull();
        assertThat(response.getOutput()).isEqualTo(deployedReleases);

        verify(repository, times(0)).deleteAll(orphanedReleases);
        verify(repository).saveAll(any());
    }

    @Test
    void shouldAddNewReleasesSuccessfully() {
        FilePathDetails paths = getPaths(valuesPath, kubeConfigPath);
        WorkloadInstance workloadInstance = getWorkloadInstance();
        String deployedReleases = getReleasesListInput(SERVICE_A, SERVICE_B, SERVICE_C);
        CommandResponse listCommand = getCommand(deployedReleases);
        List<Release> storedReleases = getReleases(workloadInstance, SERVICE_A, SERVICE_C);
        List<Release> orphanedReleases = new ArrayList<>();
        Operation operation = getOperationTerminateType(workloadInstance);

        when(builder.buildListCommand(paths, NAMESPACE)).thenReturn(LIST_COMMAND);
        when(commandExecutorHelper.executeWithRetry(LIST_COMMAND, TIMEOUT)).thenReturn(listCommand);
        when(repository.findByWorkloadInstance(workloadInstance)).thenReturn(storedReleases);

        CommandResponse response = releaseService.handleOrphanedReleases(operation, workloadInstance, paths,
                HelmSourceType.HELMFILE, listCommand);

        assertThat(response).isNotNull();
        assertThat(response.getOutput()).isEqualTo(deployedReleases);

        List<Release> expectedToSaveReleases = getReleases(workloadInstance, SERVICE_A, SERVICE_B, SERVICE_C);

        verify(repository, times(0)).deleteAll(orphanedReleases);
        verify(repository).saveAll(expectedToSaveReleases);
    }

    @Test
    void shouldDeleteOrphanedAndSaveNewReleasesSuccessfully() {
        FilePathDetails paths = getPaths(valuesPath, kubeConfigPath);
        WorkloadInstance workloadInstance = getWorkloadInstance();
        CommandResponse listCommand = getCommand(
                getReleasesListInput(SERVICE_A, SERVICE_C, SERVICE_D));
        List<Release> storedReleases = getReleases(workloadInstance, SERVICE_A, SERVICE_B, SERVICE_C);
        CommandResponse deleteCommand = getCommand(DELETE_RELEASE_COMMAND);
        List<Release> orphanedReleases = getReleases(workloadInstance, SERVICE_B);
        Operation operation = getOperationRollbackType(workloadInstance);

        when(builder.buildListCommand(paths, NAMESPACE)).thenReturn(LIST_COMMAND);
        when(commandExecutorHelper.executeWithRetry(LIST_COMMAND, TIMEOUT)).thenReturn(listCommand);
        when(repository.findByWorkloadInstance(workloadInstance)).thenReturn(storedReleases);
        when(builder.deleteReleases(orphanedReleases, NAMESPACE, paths.getKubeConfigPath())).thenReturn(DELETE_RELEASE_COMMAND);
        when(commandExecutorHelper.executeWithRetry(DELETE_RELEASE_COMMAND, TIMEOUT)).thenReturn(deleteCommand);

        CommandResponse response = releaseService.handleOrphanedReleases(operation, workloadInstance, paths,
                HelmSourceType.HELMFILE, listCommand);

        assertThat(response).isNotNull();

        List<Release> expectedToSaveReleases = getReleases(workloadInstance, SERVICE_A, SERVICE_C, SERVICE_D);
        verify(repository).deleteAll(orphanedReleases);
        verify(repository).saveAll(expectedToSaveReleases);
    }

    @Test
    void shouldExitEarlyIfListExecutionFailed() {
        FilePathDetails paths = getPaths(valuesPath, kubeConfigPath);
        WorkloadInstance workloadInstance = getWorkloadInstance();
        CommandResponse listCommand = getCommand(LIST_OUTPUT);
        listCommand.setExitCode(1);
        Operation operation = getOperationRollbackType(workloadInstance);

        when(builder.buildListCommand(paths, NAMESPACE)).thenReturn(LIST_COMMAND);
        when(commandExecutorHelper.executeWithRetry(LIST_COMMAND, TIMEOUT)).thenReturn(listCommand);

        CommandResponse response = releaseService.handleOrphanedReleases(operation, workloadInstance, paths,
                HelmSourceType.HELMFILE, listCommand);

        assertThat(response).isNotNull();
        assertThat(response.getOutput()).contains(LIST_OUTPUT);
        assertThat(response.getOutput()).contains(RELEASE_ERROR_MSG);

        verify(repository, times(0)).findByWorkloadInstance(any());
        verify(repository, times(0)).deleteAll(any());
        verify(repository, times(0)).saveAll(any());
    }

    @Test
    void shouldExitEarlyIfDeleteExecutionFailed() {
        FilePathDetails paths = getPaths(valuesPath, kubeConfigPath);
        WorkloadInstance workloadInstance = getWorkloadInstance();
        CommandResponse listCommand = getCommand(LIST_OUTPUT);
        List<Release> storedReleases = getReleases(workloadInstance, SERVICE_A, SERVICE_B, SERVICE_C);
        CommandResponse deleteCommand = getCommand(DELETE_RELEASE_COMMAND);
        deleteCommand.setExitCode(1);
        List<Release> orphanedReleases = getReleases(workloadInstance, SERVICE_B);
        Operation operation = getOperationRollbackType(workloadInstance);

        when(builder.buildListCommand(paths, NAMESPACE)).thenReturn(LIST_COMMAND);
        when(commandExecutorHelper.executeWithRetry(LIST_COMMAND, TIMEOUT)).thenReturn(listCommand);
        when(repository.findByWorkloadInstance(workloadInstance)).thenReturn(storedReleases);
        when(builder.deleteReleases(orphanedReleases, NAMESPACE, paths.getKubeConfigPath())).thenReturn(DELETE_RELEASE_COMMAND);
        when(commandExecutorHelper.executeWithRetry(DELETE_RELEASE_COMMAND, TIMEOUT)).thenReturn(deleteCommand);

        CommandResponse response = releaseService.handleOrphanedReleases(operation, workloadInstance, paths,
                HelmSourceType.HELMFILE, listCommand);

        assertThat(response).isNotNull();
        assertThat(response.getOutput()).contains(DELETE_RELEASE_COMMAND);
        assertThat(response.getOutput()).contains(RELEASE_ERROR_MSG);

        verify(repository, times(0)).deleteAll(any());
        verify(repository, times(0)).saveAll(any());
    }

    private Operation getOperationRollbackType(WorkloadInstance workloadInstance) {
        return Operation.builder()
                .helmSourceId(HELMSOURCE_ID)
                .workloadInstance(workloadInstance)
                .type(OperationType.ROLLBACK)
                .startTime(LocalDateTime.now())
                .state(OperationState.COMPLETED)
                .id(OPERATION_ID)
                .timeout(TIMEOUT)
                .build();
    }

    private Operation getOperationTerminateType(WorkloadInstance workloadInstance) {
        return Operation.builder()
                .helmSourceId(HELMSOURCE_ID)
                .workloadInstance(workloadInstance)
                .type(OperationType.TERMINATE)
                .startTime(LocalDateTime.now())
                .state(OperationState.COMPLETED)
                .id(OPERATION_ID)
                .timeout(TIMEOUT)
                .build();
    }

    private Operation getOperation(WorkloadInstance workloadInstance) {
        return Operation.builder()
                .helmSourceId(HELMSOURCE_ID)
                .workloadInstance(workloadInstance)
                .type(OperationType.INSTANTIATE)
                .startTime(LocalDateTime.now())
                .state(OperationState.PROCESSING)
                .id(OPERATION_ID)
                .timeout(TIMEOUT)
                .build();
    }

    private FilePathDetails getPaths(Path valuesPath, Path kubeConfigPath) {
        return FilePathDetails.builder()
                .valuesPath(valuesPath)
                .kubeConfigPath(kubeConfigPath)
                .build();
    }

    private WorkloadInstance getWorkloadInstance() {
        return WorkloadInstance.builder()
                .workloadInstanceId(WORKLOAD_INSTANCE_ID)
                .workloadInstanceName(WORKLOAD_INSTANCE_NAME)
                .namespace(NAMESPACE)
                .build();
    }

    private CommandResponse getFailedCommand() {
        return CommandResponse.builder()
                .exitCode(1)
                .output(null)
                .build();
    }

    private CommandResponse getCommand(String output) {
        return CommandResponse.builder()
                .exitCode(0)
                .output(output)
                .build();
    }

    private Release getRelease(WorkloadInstance workloadInstance) {
        return Release.builder()
                .id("sfkalskfa;s,d;asl,")
                .enabled(true)
                .name(SERVICE_A)
                .workloadInstance(workloadInstance)
                .build();
    }

    private List<Release> getReleases(WorkloadInstance instance, String... names) {
        return Stream.of(names)
                .map(name -> {
                    Release release = getRelease(instance);
                    release.setName(name);
                    return release;
                }).collect(Collectors.toList());
    }

    private String getReleasesListInput(String ... names) {
        String prototype = "{\"name\":\"%1$s\",\"namespace\":\"demo-sprint-9\",\"enabled\":true,\"labels\":\"\","
                + "\"chart\":\"test-charts/%1$s\",\"version\":\"\"}";
        return Stream.of(names)
                .map(name -> String.format(prototype, name))
                .collect(Collectors.joining(", ", "[", "]"));
    }

}