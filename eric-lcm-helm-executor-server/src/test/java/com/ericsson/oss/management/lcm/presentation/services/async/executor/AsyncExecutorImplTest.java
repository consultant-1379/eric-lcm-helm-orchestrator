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

package com.ericsson.oss.management.lcm.presentation.services.async.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import com.ericsson.oss.management.lcm.presentation.services.helper.docker.DockerRegistrySecretHelper;
import com.ericsson.oss.management.lcm.presentation.services.kubernetes.KubernetesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.HelmSourceType;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.OperationState;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;
import com.ericsson.oss.management.lcm.model.internal.RollbackData;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.presentation.services.helper.command.CommandExecutorHelper;
import com.ericsson.oss.management.lcm.presentation.services.operation.OperationService;
import com.ericsson.oss.management.lcm.presentation.services.release.ReleaseService;
import com.ericsson.oss.management.lcm.presentation.services.rollbackhandler.RollbackHandler;
import com.ericsson.oss.management.lcm.presentation.services.values.ValuesService;
import com.ericsson.oss.management.lcm.presentation.services.version.WorkloadInstanceVersionService;
import com.ericsson.oss.management.lcm.presentation.services.workloadinstance.WorkloadInstanceService;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandResponse;

@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = "auto-rollback.enabled=true")
class AsyncExecutorImplTest extends AbstractDbSetupTest {
    private static final String HELMSOURCE_ID = "some_id";
    private static final String OPERATION_ID = "some_id";
    private static final String WORKLOAD_INSTANCE_ID = "some_id";
    private static final String WORKLOAD_INSTANCE_NAME = "some_name";
    private static final String NAMESPACE = "namespace";
    private static final String CLUSTER = "cluster";
    private static final String CLUSTER_IDENT = "hahn117 https://mocha.rnd.gic.ericsson.se/k8s/clusters/c-mdw5r";
    private static final String COMMAND = "helmfile apply";
    private static final String ROLLBACK_COMMAND = "helmfile apply for rollback";
    private static final String EMPTY_COMMAND = "";
    private static final String VALUES_VERSION = "some_version";
    private static final String HELMSOURCE_VERSION = "some_version";
    private static final String ERROR_MESSAGE = "some_message";
    private static final int TIMEOUT = 5;

    @Autowired
    private AsyncExecutor asyncExecutor;
    @MockBean
    private CommandExecutorHelper commandExecutor;
    @MockBean
    private OperationService operationService;
    @MockBean
    private FileService fileService;
    @MockBean
    private RollbackHandler rollbackHandler;
    @MockBean
    private WorkloadInstanceVersionService workloadInstanceVersionService;
    @MockBean
    private ValuesService valuesService;
    @MockBean
    private WorkloadInstanceService workloadInstanceService;
    @MockBean
    private ReleaseService releaseService;
    @MockBean
    private KubernetesService kubernetesService;
    @MockBean
    private DockerRegistrySecretHelper dockerRegistrySecretHelper;
    @Mock
    private Path helmSourcePath;
    @Mock
    private Path valuesPath;
    @Mock
    private Path kubeConfigPath;
    @Mock
    private Path directory;

    private WorkloadInstance workloadInstance;
    private WorkloadInstanceVersion workloadInstanceVersion;
    private Operation operation;
    private HelmSource helmSource;
    private FilePathDetails pathDetails;
    private CommandResponse commandResponse;

    @BeforeEach
    void setup() {
        workloadInstance = getWorkloadInstance();
        workloadInstanceVersion = getWorkloadInstanceVersion(workloadInstance);
        operation = getOperation(workloadInstance);
        helmSource = getHelmSource(workloadInstance);
        pathDetails = getPaths();
        commandResponse = new CommandResponse(null, 0);

        when(workloadInstanceVersionService.createVersion(any(), anyString(), anyString()))
                .thenReturn(workloadInstanceVersion);
        when(valuesService.post(anyString(), anyString(), any())).thenReturn(VALUES_VERSION);
        when(valuesPath.getParent()).thenReturn(directory);
        when(operationService.updateWithCommandResponse(any(), any())).thenReturn(operation);
    }

    @Test
    void shouldSuccessfullyPerformInstantiateOperation() {
        //Init
        when(commandExecutor.execute(anyString(), anyInt())).thenReturn(commandResponse);
        when(releaseService.extractAndSaveReleases(any(), any())).thenReturn(commandResponse);
        when(releaseService.handleOrphanedReleases(any(), any(), any(), any(), any())).thenReturn(commandResponse);

        //Test method
        asyncExecutor.executeAndUpdateOperation(operation, workloadInstance, pathDetails, helmSource, EMPTY_COMMAND);

        //Verify:
        //Command is executed
        verify(commandExecutor).execute(anyString(), eq(TIMEOUT));
        //Values file saved
        verify(valuesService).post(WORKLOAD_INSTANCE_NAME, HELMSOURCE_VERSION, valuesPath);
        //Version is updated
        verify(workloadInstanceVersionService).createVersion(workloadInstance, VALUES_VERSION, HELMSOURCE_VERSION);
        verify(workloadInstanceService).updateVersion(workloadInstance, workloadInstanceVersion);
        //Orphaned releases check performed
        verify(releaseService).handleOrphanedReleases(operation, workloadInstance, pathDetails, HelmSourceType.HELMFILE,
                commandResponse);
        //Operation is updated
        verify(operationService).updateWithCommandResponse(operation, commandResponse);
        //Temp folder is deleted
        verify(fileService).deleteDirectory(directory);
    }

    @Test
    void shouldSuccessfullyPerformUpgradeOperationWhenValuesNotRefreshed() {
        //Init
        operation.setType(OperationType.UPDATE);
        helmSource.setValuesRefreshed(false);
        when(commandExecutor.execute(anyString(), anyInt())).thenReturn(commandResponse);
        when(workloadInstanceVersionService.createVersion(any(), any(), anyString())).thenReturn(workloadInstanceVersion);
        when(releaseService.handleOrphanedReleases(any(), any(), any(), any(), any())).thenReturn(commandResponse);

        //Test method
        asyncExecutor.executeAndUpdateOperation(operation, workloadInstance, pathDetails, helmSource, EMPTY_COMMAND);

        //Verify:
        //Command is executed
        verify(commandExecutor).execute(anyString(), eq(TIMEOUT));
        //Values file not saved
        verify(valuesService, never()).post(WORKLOAD_INSTANCE_NAME, HELMSOURCE_VERSION, valuesPath);
        //Version is updated
        verify(workloadInstanceVersionService).createVersion(workloadInstance, null, HELMSOURCE_VERSION);
        verify(workloadInstanceService).updateVersion(workloadInstance, workloadInstanceVersion);
        //Orphaned releases check performed
        verify(releaseService).handleOrphanedReleases(operation, workloadInstance, pathDetails, HelmSourceType.HELMFILE,
                commandResponse);
        //Operation is updated
        verify(operationService).updateWithCommandResponse(operation, commandResponse);
        //Temp folder is deleted
        verify(fileService).deleteDirectory(directory);
    }

    @Test
    void shouldPerformAutoRollbackWhenInstantiateOperationFailed() {
        //Init
        commandResponse.setExitCode(1);
        CommandResponse rollbackCommandResponse = new CommandResponse(null, 0);
        RollbackData rollbackData = getRollbackData();

        when(commandExecutor.execute(anyString(), anyInt())).thenReturn(commandResponse);
        when(commandExecutor.execute(eq(ROLLBACK_COMMAND), anyInt())).thenReturn(rollbackCommandResponse);
        when(rollbackHandler.prepareRollbackData(any(WorkloadInstance.class), any(), any(), any())).thenReturn(rollbackData);
        when(releaseService.handleOrphanedReleases(any(), any(), any(), any(), any())).thenReturn(rollbackCommandResponse);
        when(operationService.updateWithCommandResponse(any(), any())).thenReturn(operation);

        //Test method
        asyncExecutor.executeAndUpdateOperation(operation, workloadInstance, pathDetails, helmSource, EMPTY_COMMAND);

        //Verify:
        //Command is executed
        verify(commandExecutor, times(2)).execute(anyString(), eq(TIMEOUT));
        //Values file saved after rollback
        verify(valuesService).post(WORKLOAD_INSTANCE_NAME, HELMSOURCE_VERSION, valuesPath);
        //Version is updated
        verify(workloadInstanceVersionService).createVersion(workloadInstance, VALUES_VERSION, HELMSOURCE_VERSION);
        verify(workloadInstanceService).updateVersion(workloadInstance, workloadInstanceVersion);
        //Orphaned releases check performed
        verify(releaseService).handleOrphanedReleases(operation, workloadInstance, rollbackData.getPaths(), HelmSourceType.HELMFILE,
                rollbackCommandResponse);
        //Operation is updated
        verify(operationService).updateWithCommandResponse(operation, rollbackCommandResponse);
        //Temp folder is deleted
        verify(fileService).deleteDirectory(directory);
    }

    @Test
    void shouldSetRollbackTypeAndPerformAutoRollbackWhenInstantiateFailed() {
        //Init
        commandResponse.setExitCode(1);
        CommandResponse rollbackCommandResponse = new CommandResponse(null, 0);
        RollbackData rollbackData = getRollbackData();

        when(commandExecutor.execute(anyString(), anyInt())).thenReturn(commandResponse);
        when(commandExecutor.execute(eq(ROLLBACK_COMMAND), anyInt())).thenReturn(rollbackCommandResponse);
        when(rollbackHandler.prepareRollbackData(any(WorkloadInstance.class), any(), any(), any())).thenReturn(rollbackData);
        when(releaseService.extractAndSaveReleases(any(), any())).thenReturn(rollbackCommandResponse);
        when(operationService.updateWithCommandResponse(any(), any())).thenReturn(getOperationWithTypeRollback(workloadInstance));

        //Test method
        asyncExecutor.executeAndUpdateOperation(operation, workloadInstance, pathDetails, helmSource, EMPTY_COMMAND);

        //Verify:
        //Command is executed
        verify(commandExecutor, times(2)).execute(anyString(), eq(TIMEOUT));
        //Values file saved after rollback
        verify(valuesService).post(WORKLOAD_INSTANCE_NAME, HELMSOURCE_VERSION, valuesPath);
        //File content is present
        verify(fileService).getFileContentIfPresent(eq(pathDetails.getKubeConfigPath()));
        //Secret deleted
        verify(dockerRegistrySecretHelper).deleteSecret(any(), any());
        //Temp folder is deleted
        verify(fileService).deleteDirectory(eq(directory));
    }

    @Test
    void shouldSaveValuesWhenPerformAutoRollbackWhenUpgradeOperationFailed() {
        //Init
        operation.setType(OperationType.UPDATE);
        commandResponse.setExitCode(1);
        CommandResponse rollbackCommandResponse = new CommandResponse(null, 0);
        RollbackData rollbackData = getRollbackData();

        when(releaseService.handleOrphanedReleases(any(), any(), any(), any(), any())).thenReturn(rollbackCommandResponse);
        when(commandExecutor.execute(anyString(), anyInt())).thenReturn(commandResponse);
        when(commandExecutor.execute(eq(ROLLBACK_COMMAND), anyInt())).thenReturn(rollbackCommandResponse);
        when(rollbackHandler.prepareRollbackData(any(WorkloadInstance.class), any(), any(), any())).thenReturn(rollbackData);

        //Test method
        asyncExecutor.executeAndUpdateOperation(operation, workloadInstance, pathDetails, helmSource, EMPTY_COMMAND);

        //Verify:
        //Command is executed
        verify(commandExecutor, times(2)).execute(anyString(), eq(TIMEOUT));
        //Values file not saved after rollback
        verify(valuesService).post(WORKLOAD_INSTANCE_NAME, HELMSOURCE_VERSION, valuesPath);
        //Version is not updated
        verify(workloadInstanceVersionService).createVersion(workloadInstance, VALUES_VERSION, HELMSOURCE_VERSION);
        verify(workloadInstanceService).updateVersion(workloadInstance, workloadInstanceVersion);
        //Orphaned releases check performed and use paths from rollback data
        verify(releaseService).handleOrphanedReleases(operation, workloadInstance, rollbackData.getPaths(), HelmSourceType.HELMFILE,
                rollbackCommandResponse);
        //Operation is updated
        verify(operationService).updateWithCommandResponse(operation, rollbackCommandResponse);
        //Temp folder is deleted
        verify(fileService).deleteDirectory(directory);
    }

    @Test
    void shouldOnlyUpdateOperationWhenExceptionOccurredDuringInstantiateOperation() {
        //Init
        when(commandExecutor.execute(anyString(), anyInt()))
                .thenThrow(new RuntimeException(ERROR_MESSAGE));

        //Test method
        asyncExecutor.executeAndUpdateOperation(operation, workloadInstance, pathDetails, helmSource, EMPTY_COMMAND);

        //Verify:
        //Command is executed
        verify(commandExecutor).execute(anyString(), eq(TIMEOUT));
        //Values file is not saved after rollback
        verify(valuesService, never()).post(WORKLOAD_INSTANCE_NAME, HELMSOURCE_VERSION, valuesPath);
        //Version is not updated
        verify(workloadInstanceVersionService, never()).createVersion(workloadInstance, VALUES_VERSION, HELMSOURCE_VERSION);
        verify(workloadInstanceService, never()).updateVersion(workloadInstance, workloadInstanceVersion);
        //Orphaned releases check not performed
        verify(releaseService, never()).extractAndSaveReleases(pathDetails, workloadInstance);
        //Operation is updated
        verify(operationService).updateWithCommandResponse(eq(operation), any());
        //Temp folder is deleted
        verify(fileService).deleteDirectory(directory);
    }

    @Test
    void shouldNotHandleOrphanedReleasesWhenInstantiateOperationWithIntegrationChart() {
        //Init
        helmSource.setHelmSourceType(HelmSourceType.INTEGRATION_CHART);
        when(commandExecutor.execute(anyString(), anyInt())).thenReturn(commandResponse);
        when(releaseService.handleOrphanedReleases(any(), any(), any(), any(), any())).thenReturn(commandResponse);

        //Test method
        asyncExecutor.executeAndUpdateOperation(operation, workloadInstance, pathDetails, helmSource, EMPTY_COMMAND);

        //Verify:
        //Command is executed
        verify(commandExecutor).execute(anyString(), eq(TIMEOUT));
        //Values file saved
        verify(valuesService).post(WORKLOAD_INSTANCE_NAME, HELMSOURCE_VERSION, valuesPath);
        //Version is updated
        verify(workloadInstanceVersionService).createVersion(workloadInstance, VALUES_VERSION, HELMSOURCE_VERSION);
        verify(workloadInstanceService).updateVersion(workloadInstance, workloadInstanceVersion);
        //Orphaned releases check not performed
        verify(releaseService, never()).extractAndSaveReleases(pathDetails, workloadInstance);
        //Operation is updated
        verify(operationService).updateWithCommandResponse(operation, commandResponse);
        //Temp folder is deleted
        verify(fileService).deleteDirectory(directory);
    }

    @Test
    void shouldSuccessfullyPerformTerminateOperation() {
        //Init
        var paths = getPaths();
        operation.setType(OperationType.TERMINATE);
        when(commandExecutor.execute(anyString(), anyInt())).thenReturn(commandResponse);

        //Test method
        asyncExecutor.executeAndUpdateOperationForTerminate(operation, paths, COMMAND, workloadInstance,
                                                            HelmSourceType.HELMFILE, false);

        //Verify:
        //Command is executed
        verify(commandExecutor).execute(anyString(), eq(TIMEOUT));
        //Operation is updated
        verify(operationService).updateWithCommandResponse(operation, commandResponse);
        //Temp folder is deleted
        verify(fileService).deleteDirectory(paths.getHelmSourcePath().getParent());
        // Namespace is not deleted when not required
        verify(kubernetesService, never()).deleteNamespace(anyString(), any());
        //Secret is deleted
        verify(dockerRegistrySecretHelper).deleteSecret(any(), any());
    }

    @Test
    void shouldSuccessfullyPerformTerminateOperationAndDeleteNamespace() {
        //Init
        var paths = getPaths();
        operation.setType(OperationType.TERMINATE);
        when(commandExecutor.execute(anyString(), anyInt())).thenReturn(commandResponse);
        when(kubernetesService.getPodsInNamespace(anyString(), any(Path.class))).thenReturn(Collections.emptyList());
        when(workloadInstanceService.validateInstancesForDeletionInNamespace(anyString(), anyString())).thenReturn(true);

        //Test method
        asyncExecutor.executeAndUpdateOperationForTerminate(operation, paths, COMMAND, workloadInstance,
                HelmSourceType.HELMFILE, true);

        //Verify:
        //Command is executed
        verify(commandExecutor).execute(anyString(), eq(TIMEOUT));
        //Operation is updated
        verify(operationService).updateWithCommandResponse(operation, commandResponse);
        // Namespace and temp dir are deleted with delay
        await().atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
            verify(kubernetesService).deleteNamespace(anyString(), any());
            verify(fileService).deleteDirectory(paths.getHelmSourcePath().getParent());
        });
        //Secret is deleted
        verify(dockerRegistrySecretHelper).deleteSecret(any(), any());
    }

    @Test
    void shouldUpdateOperationStateToFailedWhenExceptionOccurredDuringTerminateOperation() {
        //Init
        var paths = getPaths();
        operation.setType(OperationType.TERMINATE);
        when(commandExecutor.execute(anyString(), anyInt()))
                .thenThrow(new RuntimeException(ERROR_MESSAGE));

        //Test method
        asyncExecutor.executeAndUpdateOperationForTerminate(operation, paths, COMMAND, workloadInstance,
                                                            HelmSourceType.HELMFILE, false);

        //Verify:
        //Command is executed
        verify(commandExecutor).execute(anyString(), eq(TIMEOUT));
        //Operation is updated
        verify(operationService).updateWithCommandResponse(eq(operation), any());
        //Temp folder is deleted
        verify(fileService).deleteDirectory(paths.getHelmSourcePath().getParent());
        // Namespace is not deleted
        verify(kubernetesService, never()).deleteNamespace(anyString(), any());
        //Secret is deleted
        verify(dockerRegistrySecretHelper).deleteSecret(any(), any());
    }

    @Test
    void shouldCleanResourcesInNamespaceByLabelIfTerminateFailedForIntegrationChart() {
        var paths = getPaths();
        operation.setType(OperationType.TERMINATE);
        when(commandExecutor.execute(anyString(), anyInt()))
                .thenReturn(new CommandResponse("Error", 1));
        when(workloadInstanceService.validateInstancesForDeletionInNamespace(anyString(), anyString())).thenReturn(Boolean.TRUE);

        //Test method
        asyncExecutor.executeAndUpdateOperationForTerminate(operation, paths, COMMAND, workloadInstance,
                                                            HelmSourceType.INTEGRATION_CHART, true);

        //Verify:
        //Command is executed
        verify(commandExecutor).execute(anyString(), eq(TIMEOUT));
        // Run clean resources for integration chart
        verify(kubernetesService)
                .cleanResourcesByNamespaceAndInstanceName(workloadInstance.getNamespace(),
                        workloadInstance.getWorkloadInstanceName(), paths.getKubeConfigPath());
        //Operation is updated
        verify(operationService).updateWithCommandResponse(eq(operation), any());
        //Temp folder is deleted
        verify(fileService).deleteDirectory(paths.getHelmSourcePath().getParent());
        // Namespace is deleted when required
        verify(kubernetesService).deleteNamespace(anyString(), any());
        //Secret is deleted
        verify(dockerRegistrySecretHelper).deleteSecret(any(), any());
    }

    @Test
    void shouldSuccessfullyPerformRollbackOperation() {
        //Init
        operation.setType(OperationType.ROLLBACK);
        when(commandExecutor.execute(anyString(), anyInt())).thenReturn(commandResponse);
        when(helmSourcePath.getParent()).thenReturn(directory);
        when(releaseService.handleOrphanedReleases(any(), any(), any(), any(), any())).thenReturn(commandResponse);

        //Test method
        asyncExecutor.executeAndUpdateOperationForRollback(operation, pathDetails, COMMAND,
                                                           workloadInstanceVersion, HelmSourceType.HELMFILE);

        //Verify:
        //Command is executed
        verify(commandExecutor).execute(anyString(), eq(TIMEOUT));
        //Version is updated
        verify(workloadInstanceService).updateVersion(workloadInstance, workloadInstanceVersion);
        //Orphaned releases check performed
        verify(releaseService).handleOrphanedReleases(operation, workloadInstance, pathDetails, HelmSourceType.HELMFILE,
                commandResponse);
        //Operation is updated
        verify(operationService).updateWithCommandResponse(operation, commandResponse);
        //Temp folder is deleted
        verify(fileService).deleteDirectory(directory);
    }

    @Test
    void shouldOnlyUpdateOperationWhenExceptionOccurredDuringRollbackOperation() {
        //Init
        operation.setType(OperationType.ROLLBACK);
        when(commandExecutor.execute(anyString(), anyInt()))
                .thenThrow(new RuntimeException(ERROR_MESSAGE));
        when(helmSourcePath.getParent()).thenReturn(directory);

        //Test method
        asyncExecutor.executeAndUpdateOperationForRollback(operation, pathDetails, COMMAND,
                                                           workloadInstanceVersion, HelmSourceType.HELMFILE);

        //Verify:
        //Command is executed
        verify(commandExecutor).execute(anyString(), eq(TIMEOUT));
        //Version is not updated
        verify(workloadInstanceService, never()).updateVersion(workloadInstance, workloadInstanceVersion);
        //Orphaned releases check not performed
        verify(releaseService, never()).extractAndSaveReleases(pathDetails, workloadInstance);
        //Operation is updated
        verify(operationService).updateWithCommandResponse(eq(operation), any());
        //Temp folder is deleted
        verify(fileService).deleteDirectory(directory);
    }

    @Test
    void shouldOnlyUpdateOperationWhenRollbackOperationFailed() {
        //Init
        operation.setType(OperationType.ROLLBACK);
        commandResponse.setExitCode(1);
        when(commandExecutor.execute(anyString(), anyInt())).thenReturn(commandResponse);
        when(helmSourcePath.getParent()).thenReturn(directory);

        //Test method
        asyncExecutor.executeAndUpdateOperationForRollback(operation, pathDetails, COMMAND,
                                                           workloadInstanceVersion, HelmSourceType.HELMFILE);

        //Verify:
        //Command is executed
        verify(commandExecutor).execute(anyString(), eq(TIMEOUT));
        //Version is not updated
        verify(workloadInstanceService, never()).updateVersion(workloadInstance, workloadInstanceVersion);
        //Orphaned releases check not performed
        verify(releaseService, never()).extractAndSaveReleases(pathDetails, workloadInstance);
        //Operation is updated
        verify(operationService).updateWithCommandResponse(operation, commandResponse);
        //Temp folder is deleted
        verify(fileService).deleteDirectory(directory);
    }

    @Test
    void shouldMergeResponsesWhenOrphanedReleasesHandlingFailedAfterRollbackAndSetCommandOutputToOperation() {
        //Init
        operation.setType(OperationType.ROLLBACK);
        operation.setOutput(ERROR_MESSAGE);
        CommandResponse orphanedReleasesResponse = new CommandResponse(ERROR_MESSAGE, 1);
        when(commandExecutor.execute(anyString(), anyInt())).thenReturn(commandResponse);
        when(helmSourcePath.getParent()).thenReturn(directory);
        when(releaseService.handleOrphanedReleases(any(), any(), any(), any(), any())).thenReturn(orphanedReleasesResponse);

        //Test method
        asyncExecutor.executeAndUpdateOperationForRollback(operation, pathDetails, COMMAND,
                                                           workloadInstanceVersion, HelmSourceType.HELMFILE);

        //Verify output from releases handling is merged with original response
        assertThat(orphanedReleasesResponse.getOutput()).contains(ERROR_MESSAGE);
        //Verify that operation contains output with error message
        assertThat(operation.getOutput()).contains(ERROR_MESSAGE);
    }

    @Test
    void shouldMergeResponsesWhenOrphanedReleasesHandlingFailedAfterUpdateAndSetCommandOutputToOperation() {
        //Init
        operation.setType(OperationType.UPDATE);
        operation.setOutput(ERROR_MESSAGE);
        CommandResponse orphanedReleases = new CommandResponse(ERROR_MESSAGE, 1);
        when(commandExecutor.execute(anyString(), anyInt())).thenReturn(commandResponse);
        when(releaseService.handleOrphanedReleases(any(), any(), any(), any(), any())).thenReturn(orphanedReleases);

        //Test method
        asyncExecutor.executeAndUpdateOperation(operation, workloadInstance, pathDetails, helmSource, COMMAND);

        //Verify handling is perform
        verify(releaseService, only()).handleOrphanedReleases(operation, workloadInstance, pathDetails, HelmSourceType.HELMFILE,
                commandResponse);
        //Verify command responses are merged
        assertThat(orphanedReleases.getOutput()).contains(ERROR_MESSAGE);
        //Verify operation contains output
        assertThat(operation.getOutput()).contains(ERROR_MESSAGE);
    }

    private Operation getOperationWithTypeRollback(WorkloadInstance workloadInstance) {
        workloadInstance.setOperations(null);
        Operation operation = getOperation(workloadInstance);
        operation.setType(OperationType.ROLLBACK);
        return operation;
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

    private WorkloadInstance getWorkloadInstance() {
        return WorkloadInstance.builder()
                .workloadInstanceId(WORKLOAD_INSTANCE_ID)
                .workloadInstanceName(WORKLOAD_INSTANCE_NAME)
                .cluster(CLUSTER)
                .namespace(NAMESPACE)
                .clusterIdentifier(CLUSTER_IDENT)
                .additionalParameters("some additional parameters here")
                .build();
    }

    private HelmSource getHelmSource(WorkloadInstance workloadInstance) {
        return HelmSource.builder()
                .content(new byte[] {})
                .helmSourceType(HelmSourceType.HELMFILE)
                .workloadInstance(workloadInstance)
                .created(LocalDateTime.now())
                .id(HELMSOURCE_ID)
                .isValuesRefreshed(true)
                .helmSourceVersion(HELMSOURCE_VERSION)
                .build();
    }

    private RollbackData getRollbackData() {
        Path path = Path.of("fromRollback");
        var rollbackPaths = FilePathDetails.builder()
                .helmSourcePath(path)
                .kubeConfigPath(Path.of("kubeConfigPath"))
                .valuesPath(path)
                .build();
        return RollbackData.builder()
                .paths(rollbackPaths)
                .command(ROLLBACK_COMMAND)
                .helmSourceType(HelmSourceType.HELMFILE)
                .build();
    }

    private FilePathDetails getPaths() {
        return FilePathDetails.builder()
                .helmSourcePath(helmSourcePath)
                .valuesPath(valuesPath)
                .kubeConfigPath(kubeConfigPath)
                .build();
    }

    private WorkloadInstanceVersion getWorkloadInstanceVersion(WorkloadInstance workloadInstance) {
        return WorkloadInstanceVersion.builder()
                .valuesVersion(VALUES_VERSION)
                .helmSourceVersion(HELMSOURCE_VERSION)
                .workloadInstance(workloadInstance)
                .version(2)
                .id("some_random_id")
                .build();
    }
}
