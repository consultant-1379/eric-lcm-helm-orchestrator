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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

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
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.presentation.services.helper.command.CommandExecutorHelper;
import com.ericsson.oss.management.lcm.presentation.services.kubernetes.KubernetesService;
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
@TestPropertySource(properties = "auto-rollback.enabled=false")
class AsyncExecutorDisabledAutoRollbackTest extends AbstractDbSetupTest {

    private static final String HELM_SOURCE_ID = "some_id";
    private static final String OPERATION_ID = "some_id";
    private static final String WORKLOAD_INSTANCE_ID = "some_id";
    private static final String WORKLOAD_INSTANCE_NAME = "some_name";
    private static final String NAMESPACE = "namespace";
    private static final String CLUSTER = "cluster";
    private static final String EMPTY_COMMAND = "";
    private static final String VALUES_VERSION = "some_version";
    private static final String HELM_SOURCE_VERSION = "some_version";
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
    }

    @Test
    void shouldNotPerformAutoRollbackWhenInstantiateOperationFailed() {
        commandResponse.setExitCode(1);
        workloadInstance.setHelmSources(List.of(helmSource));

        when(commandExecutor.execute(anyString(), anyInt())).thenReturn(commandResponse);
        when(operationService.updateWithCommandResponse(any(), any())).thenReturn(operation);

        asyncExecutor.executeAndUpdateOperation(operation, workloadInstance, pathDetails, helmSource, EMPTY_COMMAND);

        verify(commandExecutor, times(1)).execute(anyString(), eq(TIMEOUT));
        verify(valuesService).post(WORKLOAD_INSTANCE_NAME, HELM_SOURCE_VERSION, valuesPath);
        verify(workloadInstanceVersionService).createVersion(workloadInstance, VALUES_VERSION, HELM_SOURCE_VERSION);
        verify(workloadInstanceService).updateVersion(workloadInstance, workloadInstanceVersion);
        verify(releaseService, never()).handleOrphanedReleases(any(), any(), any(), any(), any());
        verify(rollbackHandler, never()).prepareRollbackData(any(WorkloadInstance.class), any(Operation.class), any(), any());
        verify(kubernetesService).getLogs(anyString(), anyString(), any());
        verify(operationService).updateWithCommandResponse(operation, commandResponse);
        verify(fileService).deleteDirectory(directory);
    }

    @Test
    void shouldNotPerformAutoRollbackWhenUpgradeOperationFailed() {
        operation.setType(OperationType.UPDATE);
        commandResponse.setExitCode(1);

        when(releaseService.handleOrphanedReleases(any(), any(), any(), any(), any())).thenReturn(commandResponse);
        when(operationService.updateWithCommandResponse(any(), any())).thenReturn(operation);
        when(commandExecutor.execute(anyString(), anyInt())).thenReturn(commandResponse);

        asyncExecutor.executeAndUpdateOperation(operation, workloadInstance, pathDetails, helmSource, EMPTY_COMMAND);

        verify(commandExecutor).execute(anyString(), eq(TIMEOUT));
        verify(valuesService).post(WORKLOAD_INSTANCE_NAME, HELM_SOURCE_VERSION, valuesPath);
        verify(workloadInstanceVersionService).createVersion(workloadInstance, VALUES_VERSION, HELM_SOURCE_VERSION);
        verify(workloadInstanceService).updateVersion(workloadInstance, workloadInstanceVersion);
        verify(releaseService, never()).handleOrphanedReleases(operation, workloadInstance, pathDetails,
                HelmSourceType.HELMFILE, commandResponse);
        verify(kubernetesService).getLogs(anyString(), anyString(), any());
        verify(rollbackHandler, never()).prepareRollbackData(any(WorkloadInstance.class), any(Operation.class), any(), any());
        verify(operationService).updateWithCommandResponse(operation, commandResponse);
        verify(fileService).deleteDirectory(directory);
    }

    private WorkloadInstanceVersion getWorkloadInstanceVersion(WorkloadInstance workloadInstance) {
        return WorkloadInstanceVersion.builder()
                .valuesVersion(VALUES_VERSION)
                .helmSourceVersion(HELM_SOURCE_VERSION)
                .workloadInstance(workloadInstance)
                .version(2)
                .id("some_random_id")
                .build();
    }

    private Operation getOperation(WorkloadInstance workloadInstance) {
        return Operation.builder()
                .helmSourceId(HELM_SOURCE_ID)
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
                .additionalParameters("some additional parameters here")
                .build();
    }

    private HelmSource getHelmSource(WorkloadInstance workloadInstance) {
        return HelmSource.builder()
                .content(new byte[] {})
                .helmSourceType(HelmSourceType.HELMFILE)
                .workloadInstance(workloadInstance)
                .created(LocalDateTime.now())
                .id(HELM_SOURCE_ID)
                .isValuesRefreshed(true)
                .helmSourceVersion(HELM_SOURCE_VERSION)
                .build();
    }

    private FilePathDetails getPaths() {
        return FilePathDetails.builder()
                .helmSourcePath(helmSourcePath)
                .valuesPath(valuesPath)
                .kubeConfigPath(kubeConfigPath)
                .build();
    }

}
