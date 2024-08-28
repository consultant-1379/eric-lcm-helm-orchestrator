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

package com.ericsson.oss.management.lcm.presentation.services.rollbackhandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.ENABLED_ARGUMENT_VALUE_FALSE;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.ROLLBACK_VALUES_YAML;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.TestUtils;
import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.HelmSourceType;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.OperationState;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;
import com.ericsson.oss.management.lcm.model.internal.RollbackData;
import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.presentation.services.helper.file.StoreFileHelper;
import com.ericsson.oss.management.lcm.presentation.services.values.ValuesService;
import com.ericsson.oss.management.lcm.presentation.services.version.WorkloadInstanceVersionService;
import com.ericsson.oss.management.lcm.presentation.services.workloadinstance.WorkloadInstanceService;
import com.ericsson.oss.management.lcm.repositories.HelmSourceRepository;
import com.ericsson.oss.management.lcm.utils.command.builder.HelmSourceCommandBuilder;

@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RollbackHandlerImplTest extends AbstractDbSetupTest {

    private static final Integer VERSION = 2;
    private static final String VALUES_VERSION = "0e35ed30-d438-4b07-a82b-cab447424d30";
    private static final String HELM_SOURCE_VERSION = "1.2.3-4";
    private static final String HELMSOURCE_ID = "some_id";
    private static final String OPERATION_ID = "some_id";
    private static final String WORKLOAD_INSTANCE_ID = "wrkld_instance_id";
    private static final String WORKLOAD_INSTANCE_NAME = "workloadInstanceName";
    private static final String NAMESPACE = "namespace";
    private static final String CLUSTER = "cluster";
    private static final String DIRECTORY_PATH = "helmfile-test";
    private static final String VALUES_FILE_NAME = "test-value";
    private static final String KUBE_CONFIG_FILE_NAME = "test-kubeConfig";
    private static final String PREVIOUS_VALUES_PATH = "values1.yaml";

    @Autowired
    private RollbackHandler rollbackHandler;
    @SpyBean
    private FileService fileService;
    @MockBean
    private WorkloadInstanceVersionService workloadInstanceVersionService;
    @MockBean
    private HelmSourceRepository helmSourceRepository;
    @MockBean
    private HelmSourceCommandBuilder helmSourceCommandBuilder;
    @MockBean
    private WorkloadInstanceService workloadInstanceService;
    @MockBean
    private StoreFileHelper storeFileHelper;
    @MockBean
    private ValuesService valuesService;

    private Path directory;
    private byte[] helmSourceContent;
    private Path previousValues;

    @BeforeEach
    void setup() throws URISyntaxException {
        directory = TestUtils.getResource(DIRECTORY_PATH);
        helmSourceContent = new byte[3];
        previousValues = Paths.get(PREVIOUS_VALUES_PATH);
    }

    @Test
    void shouldRollbackToPreviousVersionAfterFailedUpdateWithoutKubeConfig() {
        HelmSource helmSource = getHelmSource(HELMSOURCE_ID);
        WorkloadInstance instance = getWorkloadInstance();
        Operation operation = getOperation(OperationType.UPDATE);
        instance.setVersion(2);
        instance.setPreviousVersion(1);
        WorkloadInstanceVersion version = getVersion(instance);
        version.setVersion(2);
        version.setHelmSourceVersion(HELM_SOURCE_VERSION);
        version.setValuesVersion("prev_version");
        FilePathDetails paths = getPaths(false);

        when(workloadInstanceVersionService.getVersion(instance)).thenReturn(version);
        when(helmSourceRepository.findByWorkloadInstanceAndHelmSourceVersion(instance, HELM_SOURCE_VERSION))
                .thenReturn(Optional.of(helmSource));
        when(valuesService.retrieveByVersion(instance.getWorkloadInstanceName(), version, directory)).thenReturn(previousValues);
        doReturn(paths.getHelmSourcePath()).when(fileService).createFile(any(), any(), anyString());
        doNothing().when(fileService).extractArchive(any(), anyInt());

        RollbackData result = rollbackHandler.prepareRollbackData(instance, operation, paths, directory);

        assertThat(operation.getType()).isEqualTo(OperationType.ROLLBACK);
        assertThat(operation.getState()).isEqualTo(OperationState.PROCESSING);
        assertThat(operation.getOutput()).contains(OperationType.UPDATE.name());
        assertThat(instance.getVersion()).isEqualTo(2); //version should not change during auto-rollback
        assertThat(result.getPaths().getKubeConfigPath()).isNull();
        assertThat(result.getPaths().getValuesPath()).isEqualTo(previousValues);

        verify(fileService).createFile(eq(directory), eq(helmSourceContent), any());
        verify(helmSourceCommandBuilder).apply(eq(helmSource), any(), anyInt(), eq(OperationType.ROLLBACK));
        // should not copy values file during update
        // should not copy kubeConfig if file don't exist in the paths map
        verify(fileService, never()).copyFile(any(), any(), anyString());
    }

    @Test
    void shouldRollbackToPreviousVersionAfterFailedUpdateAndAddKubeConfigToRollbackData() {
        HelmSource helmSource = getHelmSource(HELMSOURCE_ID);
        WorkloadInstance instance = getWorkloadInstance();
        Operation operation = getOperation(OperationType.UPDATE);
        instance.setVersion(2);
        instance.setPreviousVersion(1);
        WorkloadInstanceVersion version = getVersion(instance);
        version.setVersion(2);
        version.setHelmSourceVersion(HELM_SOURCE_VERSION);
        version.setValuesVersion("prev_version");
        FilePathDetails paths = getPaths(true);
        Path kubeConfig = paths.getKubeConfigPath();
        Path rollbackKubeConfig = directory.resolve(kubeConfig);

        when(workloadInstanceVersionService.getVersion(instance)).thenReturn(version);
        when(helmSourceRepository.findByWorkloadInstanceAndHelmSourceVersion(instance, HELM_SOURCE_VERSION))
                .thenReturn(Optional.of(helmSource));
        when(valuesService.retrieveByVersion(instance.getWorkloadInstanceName(), version, directory)).thenReturn(previousValues);
        doReturn(rollbackKubeConfig).when(fileService).copyFile(kubeConfig, directory, null);
        doReturn(paths.getHelmSourcePath()).when(fileService).createFile(any(), any(), anyString());
        doNothing().when(fileService).extractArchive(any(), anyInt());

        RollbackData result = rollbackHandler.prepareRollbackData(instance, operation, paths, directory);

        assertThat(operation.getType()).isEqualTo(OperationType.ROLLBACK);
        assertThat(operation.getState()).isEqualTo(OperationState.PROCESSING);
        assertThat(operation.getOutput()).contains(OperationType.UPDATE.name());
        assertThat(instance.getVersion()).isEqualTo(2); //version should not change during auto-rollback
        assertThat(result.getPaths().getKubeConfigPath()).isEqualTo(rollbackKubeConfig);
        assertThat(result.getPaths().getValuesPath()).isEqualTo(previousValues);

        verify(fileService).createFile(eq(directory), eq(helmSourceContent), any());
        verify(helmSourceCommandBuilder).apply(eq(helmSource), any(), anyInt(), eq(OperationType.ROLLBACK));
        // should not copy values file during update
        // should copy kubeConfig if file exists in the paths map
        verify(fileService).copyFile(kubeConfig, directory, null);
    }

    @Test
    void shouldTerminateAfterFailedInstantiateWithoutKubeConfig() {
        HelmSource helmSource = getHelmSource(HELMSOURCE_ID);
        WorkloadInstance instance = getWorkloadInstance();
        Operation operation = getOperation(OperationType.INSTANTIATE);
        instance.setHelmSources(Collections.singletonList(helmSource));
        FilePathDetails paths = getPaths(false);
        Path values = paths.getValuesPath();
        Path rollbackValues = directory.resolve(ROLLBACK_VALUES_YAML);

        doReturn(rollbackValues).when(fileService).copyFile(values, directory, ROLLBACK_VALUES_YAML);
        doReturn(paths.getHelmSourcePath()).when(fileService).createFile(any(), any(), anyString());
        doNothing().when(fileService).extractArchive(any(), anyInt());
        when(storeFileHelper.mergeParamsToValues(rollbackValues, instance, false))
                .thenReturn(rollbackValues);

        RollbackData result = rollbackHandler.prepareRollbackData(instance, operation, paths, directory);

        assertThat(operation.getType()).isEqualTo(OperationType.ROLLBACK);
        assertThat(operation.getState()).isEqualTo(OperationState.PROCESSING);
        assertThat(operation.getOutput()).contains(OperationType.INSTANTIATE.name());
        assertThat(result.getPaths().getKubeConfigPath()).isNull();
        assertThat(result.getPaths().getValuesPath()).isEqualTo(rollbackValues);

        verify(workloadInstanceVersionService, times(0)).getVersion(any());
        verify(helmSourceRepository, times(0)).findByWorkloadInstanceAndHelmSourceVersion(any(), anyString());
        verify(valuesService, times(0)).retrieveByVersion(anyString(), any(), any());
        verify(workloadInstanceService, times(0)).get(anyString());
        verify(fileService).createFile(eq(directory), eq(helmSourceContent), any());
        verify(helmSourceCommandBuilder).delete(eq(helmSource), any());
        verify(storeFileHelper).mergeParamsToValues(any(), any(), eq(ENABLED_ARGUMENT_VALUE_FALSE));
        // should copy only values file
        verify(fileService).copyFile(values, directory, ROLLBACK_VALUES_YAML);
    }

    @Test
    void shouldTerminateAfterFailedInstantiateAndAddKubeConfigToRollbackData() {
        HelmSource helmSource = getHelmSource(HELMSOURCE_ID);
        WorkloadInstance instance = getWorkloadInstance();
        Operation operation = getOperation(OperationType.INSTANTIATE);
        instance.setHelmSources(Collections.singletonList(helmSource));
        FilePathDetails paths = getPaths(true);
        Path values = paths.getValuesPath();
        Path rollbackValues = directory.resolve(ROLLBACK_VALUES_YAML);
        Path kubeConfig = paths.getKubeConfigPath();
        Path rollbackKubeConfig = directory.resolve(kubeConfig);

        doReturn(rollbackValues).when(fileService).copyFile(values, directory, ROLLBACK_VALUES_YAML);
        doReturn(rollbackKubeConfig).when(fileService).copyFile(kubeConfig, directory, null);
        doReturn(paths.getHelmSourcePath()).when(fileService).createFile(any(), any(), anyString());
        doNothing().when(fileService).extractArchive(any(), anyInt());
        when(storeFileHelper.mergeParamsToValues(rollbackValues, instance, false))
                .thenReturn(rollbackValues);

        RollbackData result = rollbackHandler.prepareRollbackData(instance, operation, paths, directory);

        assertThat(operation.getType()).isEqualTo(OperationType.ROLLBACK);
        assertThat(operation.getState()).isEqualTo(OperationState.PROCESSING);
        assertThat(operation.getOutput()).contains(OperationType.INSTANTIATE.name());
        assertThat(result.getPaths().getKubeConfigPath()).isEqualTo(rollbackKubeConfig);
        assertThat(result.getPaths().getValuesPath()).isEqualTo(rollbackValues);

        verify(workloadInstanceVersionService, times(0)).getVersion(any());
        verify(helmSourceRepository, times(0)).findByWorkloadInstanceAndHelmSourceVersion(any(), anyString());
        verify(valuesService, times(0)).retrieveByVersion(anyString(), any(), any());
        verify(workloadInstanceService, times(0)).get(anyString());
        verify(fileService).createFile(eq(directory), eq(helmSourceContent), any());
        verify(helmSourceCommandBuilder).delete(eq(helmSource), any());
        verify(storeFileHelper).mergeParamsToValues(any(), any(), eq(ENABLED_ARGUMENT_VALUE_FALSE));
        // should copy values file and kubeConfig file
        verify(fileService).copyFile(values, directory, ROLLBACK_VALUES_YAML);
        verify(fileService).copyFile(kubeConfig, directory, null);
    }

    @Test
    void shouldTerminateAfterFailedInstantiateAndHelmSourcesAreRetrievedFromStorage() {
        HelmSource helmSource = getHelmSource(HELMSOURCE_ID);
        WorkloadInstance instance = getWorkloadInstance();
        instance.setHelmSources(null);
        Operation operation = getOperation(OperationType.INSTANTIATE);
        WorkloadInstance storedInstance = getWorkloadInstance();
        storedInstance.setHelmSources(Collections.singletonList(helmSource));
        FilePathDetails paths = getPaths(false);
        Path values = paths.getValuesPath();
        Path rollbackValues = directory.resolve(ROLLBACK_VALUES_YAML);

        doReturn(rollbackValues).when(fileService).copyFile(values, directory, ROLLBACK_VALUES_YAML);
        doReturn(paths.getHelmSourcePath()).when(fileService).createFile(any(), any(), anyString());
        doNothing().when(fileService).extractArchive(any(), anyInt());
        when(workloadInstanceService.get(instance.getWorkloadInstanceId())).thenReturn(storedInstance);
        when(storeFileHelper.mergeParamsToValues(values, instance, false))
                .thenReturn(values);

        rollbackHandler.prepareRollbackData(instance, operation, paths, directory);

        assertThat(operation.getType()).isEqualTo(OperationType.ROLLBACK);
        assertThat(operation.getState()).isEqualTo(OperationState.PROCESSING);
        assertThat(operation.getOutput()).contains(OperationType.INSTANTIATE.name());

        verify(workloadInstanceVersionService, times(0)).getVersion(any());
        verify(helmSourceRepository, times(0)).findByWorkloadInstanceAndHelmSourceVersion(any(), anyString());
        verify(valuesService, times(0)).retrieveByVersion(anyString(), any(), any());
        verify(workloadInstanceService).get(anyString());
        verify(fileService).createFile(eq(directory), eq(helmSourceContent), any());
        verify(helmSourceCommandBuilder).delete(eq(helmSource), any());
        verify(storeFileHelper).mergeParamsToValues(eq(rollbackValues), any(), eq(ENABLED_ARGUMENT_VALUE_FALSE));
        verify(fileService).copyFile(values, directory, ROLLBACK_VALUES_YAML);
    }

    @Test
    void shouldTerminateAfterFailedReinstantiate() {
        HelmSource helmSource = getHelmSource(HELMSOURCE_ID);
        WorkloadInstance instance = getWorkloadInstance();
        instance.setVersion(2);
        instance.setPreviousVersion(1);
        Operation operation = getOperation(OperationType.REINSTANTIATE);
        WorkloadInstanceVersion version = getVersion(instance);
        version.setVersion(2);
        FilePathDetails paths = getPaths(false);
        Path values = paths.getValuesPath();
        Path rollbackValues = directory.resolve(ROLLBACK_VALUES_YAML);

        doReturn(rollbackValues).when(fileService).copyFile(values, directory, ROLLBACK_VALUES_YAML);
        doReturn(paths.getHelmSourcePath()).when(fileService).createFile(any(), any(), anyString());
        doNothing().when(fileService).extractArchive(any(), anyInt());
        when(workloadInstanceVersionService.getVersion(instance)).thenReturn(version);
        when(helmSourceRepository.findByWorkloadInstanceAndHelmSourceVersion(instance, HELM_SOURCE_VERSION))
                .thenReturn(Optional.of(helmSource));
        when(storeFileHelper.mergeParamsToValues(rollbackValues, instance, false))
                .thenReturn(rollbackValues);

        rollbackHandler.prepareRollbackData(instance, operation, paths, directory);

        assertThat(operation.getType()).isEqualTo(OperationType.ROLLBACK);
        assertThat(operation.getState()).isEqualTo(OperationState.PROCESSING);
        assertThat(operation.getOutput()).contains(OperationType.REINSTANTIATE.name());

        verify(valuesService, times(0)).retrieveByVersion(anyString(), any(), any());
        verify(workloadInstanceService, times(0)).get(anyString());
        verify(workloadInstanceVersionService).getVersion(any());
        verify(helmSourceRepository).findByWorkloadInstanceAndHelmSourceVersion(any(), anyString());
        verify(fileService).createFile(eq(directory), eq(helmSourceContent), any());
        verify(helmSourceCommandBuilder).delete(eq(helmSource), any());
        verify(storeFileHelper).mergeParamsToValues(eq(rollbackValues), any(), eq(ENABLED_ARGUMENT_VALUE_FALSE));
        // should copy only values file
        verify(fileService).copyFile(values, directory, ROLLBACK_VALUES_YAML);
    }

    @Test
    void shouldFailWhenHelmSourcesCantBeRetrievedNeitherFromStorageNeitherFromObject() {
        HelmSource helmSource = getHelmSource(HELMSOURCE_ID);
        WorkloadInstance instance = getWorkloadInstance();
        instance.setHelmSources(null);
        Operation operation = getOperation(OperationType.INSTANTIATE);
        WorkloadInstance storedInstance = getWorkloadInstance();
        storedInstance.setHelmSources(null);
        FilePathDetails paths = getPaths(false);

        when(workloadInstanceService.get(instance.getWorkloadInstanceId())).thenReturn(storedInstance);

        assertThatThrownBy(() -> rollbackHandler.prepareRollbackData(instance, operation, paths, directory))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(workloadInstanceVersionService, times(0)).getVersion(any());
        verify(helmSourceRepository, times(0)).findByWorkloadInstanceAndHelmSourceVersion(any(), anyString());
        verify(valuesService, times(0)).retrieveByVersion(anyString(), any(), any());
        verify(fileService, times(0)).createFile(eq(directory), eq(helmSourceContent), any());
        verify(fileService, never()).copyFile(any(), any(), anyString());
        verify(helmSourceCommandBuilder, times(0)).delete(eq(helmSource), any());
        verify(storeFileHelper, times(0)).mergeParamsToValues(any(), any(), eq(ENABLED_ARGUMENT_VALUE_FALSE));
        verify(workloadInstanceService).get(anyString());
    }

    @Test
    void shouldFailDuringRollbackToPreviousVersionAfterFailedUpdateWhenHelmSourceNotFound() {
        WorkloadInstance instance = getWorkloadInstance();
        Operation operation = getOperation(OperationType.UPDATE);
        instance.setVersion(2);
        instance.setPreviousVersion(1);
        WorkloadInstanceVersion version = getVersion(instance);
        version.setVersion(2);
        version.setHelmSourceVersion(HELM_SOURCE_VERSION);
        version.setValuesVersion("prev_version");
        FilePathDetails paths = getPaths(true);

        when(workloadInstanceVersionService.getVersion(instance)).thenReturn(version);
        when(helmSourceRepository.findByWorkloadInstanceAndHelmSourceVersion(instance, HELM_SOURCE_VERSION))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> rollbackHandler.prepareRollbackData(instance, operation, paths, directory))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(fileService, never()).createFile(eq(directory), any(), any());
        verify(valuesService, never()).retrieveByVersion(anyString(), any(), any());
        verify(helmSourceCommandBuilder, never()).apply(any(), any(), anyInt(), eq(OperationType.ROLLBACK));
        verify(fileService, never()).copyFile(any(), any(), anyString());
    }

    @Test
    void shouldFailDuringTerminateAfterFailedReinstantiateWhenHelmSourceNotFound() {
        WorkloadInstance instance = getWorkloadInstance();
        instance.setVersion(2);
        instance.setPreviousVersion(1);
        Operation operation = getOperation(OperationType.REINSTANTIATE);
        WorkloadInstanceVersion version = getVersion(instance);
        version.setVersion(2);
        FilePathDetails paths = getPaths(false);

        when(workloadInstanceVersionService.getVersion(instance)).thenReturn(version);
        when(helmSourceRepository.findByWorkloadInstanceAndHelmSourceVersion(instance, HELM_SOURCE_VERSION))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> rollbackHandler.prepareRollbackData(instance, operation, paths, directory))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(valuesService, times(0)).retrieveByVersion(anyString(), any(), any());
        verify(workloadInstanceService, times(0)).get(anyString());
        verify(fileService, times(0)).createFile(eq(directory), eq(helmSourceContent), any());
        verify(fileService, never()).copyFile(any(), any(), anyString());
        verify(helmSourceCommandBuilder, times(0)).delete(any(), any());
        verify(storeFileHelper, times(0)).mergeParamsToValues(any(), any(), eq(ENABLED_ARGUMENT_VALUE_FALSE));
        verify(helmSourceRepository).findByWorkloadInstanceAndHelmSourceVersion(any(), anyString());
        verify(workloadInstanceVersionService).getVersion(any());
    }

    @Test
    void shouldFailWhenRollbackOperationFailed() {
        WorkloadInstance instance = getWorkloadInstance();
        Operation operation = getOperation(OperationType.ROLLBACK);
        FilePathDetails paths = getPaths(false);

        assertThatThrownBy(() -> rollbackHandler.prepareRollbackData(instance, operation, paths, directory))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldFailWhenTerminateOperationFailed() {
        WorkloadInstance instance = getWorkloadInstance();
        Operation operation = getOperation(OperationType.TERMINATE);
        FilePathDetails paths = getPaths(false);

        assertThatThrownBy(() -> rollbackHandler.prepareRollbackData(instance, operation, paths, directory))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private WorkloadInstance getWorkloadInstance() {
        return WorkloadInstance.builder()
                .workloadInstanceName(WORKLOAD_INSTANCE_NAME)
                .workloadInstanceId(WORKLOAD_INSTANCE_ID)
                .latestOperationId(OPERATION_ID)
                .version(VERSION)
                .cluster(CLUSTER)
                .namespace(NAMESPACE)
                .build();
    }

    private FilePathDetails getPaths(boolean addKubeConfig) {
        Path paths = addKubeConfig ? Paths.get(KUBE_CONFIG_FILE_NAME) : null;
        return FilePathDetails.builder()
                .valuesPath(Paths.get(VALUES_FILE_NAME))
                .helmSourcePath(Paths.get("test-helmSource"))
                .kubeConfigPath(paths)
                .build();
    }

    private Operation getOperation(OperationType type) {
        return Operation.builder()
                .id(OPERATION_ID)
                .startTime(LocalDateTime.now())
                .state(OperationState.FAILED)
                .type(type)
                .build();
    }

    private HelmSource getHelmSource(String helmSourceId) {
        return HelmSource.builder()
                .id(helmSourceId)
                .helmSourceType(HelmSourceType.HELMFILE)
                .content(helmSourceContent)
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
}
