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

package com.ericsson.oss.management.lcm.presentation.services.coordinator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import com.ericsson.oss.management.lcm.api.model.ChartDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithURLPutRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceOperationPostRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceOperationPutRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstancePostRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstancePutRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithChartsPutRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithChartsRequestDto;
import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.HelmSourceType;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.OperationState;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;
import com.ericsson.oss.management.lcm.presentation.exceptions.IncorrectRollbackRequestException;
import com.ericsson.oss.management.lcm.presentation.exceptions.InstanceNotTerminatedException;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidInputException;
import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.operations.InstantiateService;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.operations.RollbackService;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.operations.TerminateService;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.operations.UpdateService;
import com.ericsson.oss.management.lcm.presentation.services.operation.OperationService;
import com.ericsson.oss.management.lcm.presentation.services.version.WorkloadInstanceVersionService;
import com.ericsson.oss.management.lcm.presentation.services.workloadinstance.WorkloadInstanceService;

@ActiveProfiles("test")
@SpringBootTest(classes = HelmfileExecutorApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WorkloadInstanceRequestCoordinatorServiceImplTest extends AbstractDbSetupTest {

    @Autowired
    private WorkloadInstanceRequestCoordinatorServiceImpl requestCoordinatorService;
    @MockBean
    private InstantiateService instantiateService;
    @MockBean
    private UpdateService updateService;
    @MockBean
    private TerminateService terminateService;
    @MockBean
    private RollbackService rollbackService;
    @MockBean
    private WorkloadInstanceService workloadInstanceService;
    @MockBean
    private OperationService operationService;
    @MockBean
    private ClusterConnectionInfoRequestCoordinatorService clusterConnectionInfoRequestCoordinatorService;
    @MockBean
    private WorkloadInstanceVersionService workloadInstanceVersionService;
    @Mock
    private MultipartFile helmSourceFile;
    @Mock
    private MultipartFile values;
    @Mock
    private MultipartFile clusterConnectionInfo;

    private static final String HELMSOURCE_ID = "some_id";
    private static final String OPERATION_ID = "some_id";
    private static final String WORKLOAD_INSTANCE_ID = "wrkld_instance_id";
    private static final String ADDITIONAL_PARAMETERS_TEST_KEY = "testKey";
    private static final String WORKLOAD_INSTANCE_NAME = "workloadInstanceName";
    private static final String NAMESPACE = "namespace";
    private static final String CLUSTER = "cluster";
    private static final String TERMINATE = "terminate";
    private static final String ADDITIONAL_PARAMETERS = "{\"key\": \"value\"}";
    private static final String HELM_SOURCE_VERSION = "1.2.3-4";
    private static final String GLOBAL_CRD_NAMESPACE_VALUE = "eric-crd-ns";
    private static final Integer WORKLOAD_INSTANCE_VERSION = 1;
    private static final Integer WORKLOAD_INSTANCE_VERSION_3 = 3;
    private static final String VALUES_VERSION = "0e35ed30-d438-4b07-a82b-cab447424d30";
    private static final String CLUSTER_IDENT = "hahn117 https://mocha.rnd.gic.ericsson.se/k8s/clusters/c-mdw5r";
    private static final String CHART_VERSION = "1.2.3-89";
    private static final String REPOSITORY = "https://arm.sero.gic.ericsson.se/artifactory/proj-adp-gs-all-helm";
    private static final String HELMSOURCE_PATH = "./helm_source-1.2.3-4.tgz";

    @Test
    void shouldInstantiateWorkloadInstanceWithAllParamsWithoutCluster() {
        //Init
        WorkloadInstance instance = basicWorkloadInstance(null);
        WorkloadInstancePostRequestDto requestDto = setWorkloadInstancePostRequestDto();
        when(workloadInstanceService.create(any())).thenReturn(instance);
        when(instantiateService.instantiate(any(), any(), any(), any(), anyInt())).thenReturn(instance);

        //Test method
        WorkloadInstanceDto result = requestCoordinatorService.instantiate(requestDto, helmSourceFile, values, null);

        //Verify
        verify(instantiateService).instantiate(any(), any(), any(), any(), anyInt());
        assertThat(result.getWorkloadInstanceId()).isEqualTo(WORKLOAD_INSTANCE_ID);
        assertThat(result.getWorkloadInstanceName()).isEqualTo(WORKLOAD_INSTANCE_NAME);
        assertThat(result.getNamespace()).isEqualTo(NAMESPACE);
    }

    @Test
    void shouldInstantiateWorkloadInstanceWithAllParamsWithCluster() {
        //Init
        WorkloadInstance instance = basicWorkloadInstance(CLUSTER);
        instance.setHelmSources(null);
        WorkloadInstancePostRequestDto requestDto = setWorkloadInstancePostRequestDto();
        when(workloadInstanceService.create(any())).thenReturn(instance);
        when(instantiateService.instantiate(any(), any(), any(), any(), anyInt())).thenReturn(instance);
        //Test
        WorkloadInstanceDto result = requestCoordinatorService.instantiate(requestDto, helmSourceFile,
                                                                           values, clusterConnectionInfo);
        //Verify
        verify(instantiateService).instantiate(any(), any(), any(), any(), anyInt());
        assertThat(result.getWorkloadInstanceId()).isEqualTo(WORKLOAD_INSTANCE_ID);
        assertThat(result.getWorkloadInstanceName()).isEqualTo(WORKLOAD_INSTANCE_NAME);
        assertThat(result.getNamespace()).isEqualTo(NAMESPACE);
    }

    @Test
    void shouldInstantiateThroughHelmfileBuilderWithAllParamsWithoutCluster() {
        //Init
        WorkloadInstance instance = basicWorkloadInstance(null);
        WorkloadInstanceWithChartsRequestDto requestDto = setWorkloadInstanceWithChartsPostRequestDto();
        when(workloadInstanceService.create(any())).thenReturn(instance);
        when(instantiateService.instantiate(any(WorkloadInstanceWithChartsRequestDto.class), any(), any(), eq(null))).thenReturn(instance);
        //Test method
        WorkloadInstanceDto result = requestCoordinatorService.instantiate(requestDto, values, null);

        //Verify
        verify(instantiateService).instantiate(any(WorkloadInstanceWithChartsRequestDto.class), any(), any(), eq(null));
        assertThat(result.getWorkloadInstanceId()).isEqualTo(WORKLOAD_INSTANCE_ID);
        assertThat(result.getWorkloadInstanceName()).isEqualTo(WORKLOAD_INSTANCE_NAME);
        assertThat(result.getNamespace()).isEqualTo(NAMESPACE);
    }

    @Test
    void shouldInstantiateThroughHelmfileBuilderWithAllParamsWithCluster() {
        //Init
        WorkloadInstance instance = basicWorkloadInstance(null);
        WorkloadInstanceWithChartsRequestDto requestDto = setWorkloadInstanceWithChartsPostRequestDto();
        instance.setHelmSources(null);
        when(workloadInstanceService.create(any())).thenReturn(instance);
        when(instantiateService.instantiate(any(WorkloadInstanceWithChartsRequestDto.class), any(), any(), any())).thenReturn(instance);

        //Test method
        WorkloadInstanceDto result = requestCoordinatorService.instantiate(requestDto, values, clusterConnectionInfo);

        //Verify
        verify(instantiateService).instantiate(any(WorkloadInstanceWithChartsRequestDto.class), any(), any(), any());
        assertThat(result.getWorkloadInstanceId()).isEqualTo(WORKLOAD_INSTANCE_ID);
        assertThat(result.getWorkloadInstanceName()).isEqualTo(WORKLOAD_INSTANCE_NAME);
        assertThat(result.getNamespace()).isEqualTo(NAMESPACE);
    }

    @Test
    void shouldDeleteWorkloadInstanceSuccessfully() {
        WorkloadInstance workloadInstance = basicWorkloadInstance(CLUSTER);
        Operation operation = basicTerminateOperation(workloadInstance);
        when(workloadInstanceService.get(WORKLOAD_INSTANCE_ID)).thenReturn(workloadInstance);
        when(operationService.get(OPERATION_ID)).thenReturn(operation);

        requestCoordinatorService.deleteWorkloadInstance(WORKLOAD_INSTANCE_ID);

        verify(workloadInstanceService).delete(WORKLOAD_INSTANCE_ID);
        verify(clusterConnectionInfoRequestCoordinatorService).disconnectFromClusterIfPresent(any());
    }

    @Test
    void shouldDeleteWorkloadInstanceSuccessfullyAfterFailedInstantiate() {
        WorkloadInstance workloadInstance = failedWorkloadInstance(OperationState.FAILED);
        Operation operation = failedInstantiateOperation(workloadInstance, OperationState.FAILED);
        when(workloadInstanceService.get(WORKLOAD_INSTANCE_ID)).thenReturn(workloadInstance);
        when(operationService.get(OPERATION_ID)).thenReturn(operation);

        requestCoordinatorService.deleteWorkloadInstance(WORKLOAD_INSTANCE_ID);

        verify(workloadInstanceService).delete(WORKLOAD_INSTANCE_ID);
        verify(clusterConnectionInfoRequestCoordinatorService).disconnectFromClusterIfPresent(any());
    }

    @Test
    void shouldDeleteWorkloadInstanceSuccessfullyAfterFailedInstantiateIfAutoRollbackCompleted() {
        WorkloadInstance workloadInstance = failedWorkloadInstance(OperationState.COMPLETED);
        Operation operation = failedInstantiateOperation(workloadInstance, OperationState.COMPLETED);
        when(workloadInstanceService.get(WORKLOAD_INSTANCE_ID)).thenReturn(workloadInstance);
        when(operationService.get(OPERATION_ID)).thenReturn(operation);

        requestCoordinatorService.deleteWorkloadInstance(WORKLOAD_INSTANCE_ID);

        verify(workloadInstanceService).delete(WORKLOAD_INSTANCE_ID);
        verify(clusterConnectionInfoRequestCoordinatorService).disconnectFromClusterIfPresent(any());
    }

    @Test
    void shouldFailWhenDeleteWorkloadInstanceAfterFailedInstantiate() {
        WorkloadInstance workloadInstance = failedWorkloadInstanceWithTwoOperations();
        Operation operation = failedInstantiateOperation(workloadInstance, OperationState.FAILED);
        when(workloadInstanceService.get(WORKLOAD_INSTANCE_ID)).thenReturn(workloadInstance);
        when(operationService.get(OPERATION_ID)).thenReturn(operation);

        assertThatThrownBy(() -> requestCoordinatorService.deleteWorkloadInstance(WORKLOAD_INSTANCE_ID))
                .isInstanceOf(InstanceNotTerminatedException.class)
                .hasMessageContaining("Workload instance must be TERMINATED before deletion. As an exclusion instance that doesn't have any "
                                              + "successful operation can be deleted as well");

        verify(workloadInstanceService, never()).delete(WORKLOAD_INSTANCE_ID);
        verify(clusterConnectionInfoRequestCoordinatorService, never()).disconnectFromClusterIfPresent(any());
    }

    @Test
    void shouldFailToDeleteWorkloadInstanceIfNotTerminated() {
        WorkloadInstance workloadInstance = basicWorkloadInstance(CLUSTER);
        Operation operation = basicInstantiateOperation(workloadInstance);
        when(workloadInstanceService.get(WORKLOAD_INSTANCE_ID)).thenReturn(workloadInstance);
        when(operationService.get(OPERATION_ID)).thenReturn(operation);

        assertThatThrownBy(() -> requestCoordinatorService.deleteWorkloadInstance(WORKLOAD_INSTANCE_ID))
                .isInstanceOf(InstanceNotTerminatedException.class)
                .hasMessageContaining("Workload instance must be TERMINATED before deletion. As an exclusion instance that doesn't have any "
                                              + "successful operation can be deleted as well");

        verify(workloadInstanceService, times(0)).delete(WORKLOAD_INSTANCE_ID);
        verify(clusterConnectionInfoRequestCoordinatorService, times(0)).disconnectFromClusterIfPresent(any());
    }

    @Test
    void shouldFailToDeleteWorkloadInstanceIfNotTerminationInProcessing() {
        WorkloadInstance workloadInstance = basicWorkloadInstance(CLUSTER);
        Operation operation = basicTerminateOperation(workloadInstance);
        operation.setState(OperationState.PROCESSING);
        when(workloadInstanceService.get(WORKLOAD_INSTANCE_ID)).thenReturn(workloadInstance);
        when(operationService.get(OPERATION_ID)).thenReturn(operation);

        assertThatThrownBy(() -> requestCoordinatorService.deleteWorkloadInstance(WORKLOAD_INSTANCE_ID))
                .isInstanceOf(InstanceNotTerminatedException.class)
                .hasMessageContaining("Workload instance must be TERMINATED before deletion. As an exclusion instance that doesn't have any "
                                              + "successful operation can be deleted as well");

        verify(workloadInstanceService, times(0)).delete(WORKLOAD_INSTANCE_ID);
        verify(clusterConnectionInfoRequestCoordinatorService, times(0)).disconnectFromClusterIfPresent(any());
    }

    @Test
    void shouldSuccessfullyDeleteWorkloadInstanceSuccessfullyIfTerminateFailed() {
        WorkloadInstance workloadInstance = basicWorkloadInstance(CLUSTER);
        Operation operation = basicTerminateOperation(workloadInstance);
        operation.setState(OperationState.FAILED);
        when(workloadInstanceService.get(WORKLOAD_INSTANCE_ID)).thenReturn(workloadInstance);
        when(operationService.get(OPERATION_ID)).thenReturn(operation);

        requestCoordinatorService.deleteWorkloadInstance(WORKLOAD_INSTANCE_ID);

        verify(workloadInstanceService).delete(WORKLOAD_INSTANCE_ID);
        verify(clusterConnectionInfoRequestCoordinatorService).disconnectFromClusterIfPresent(any());
    }

    @Test
    void shouldTerminateWorkloadInstanceSuccessfullyWithoutCluster() {
        WorkloadInstance workloadInstance = basicWorkloadInstance(null);
        HelmSource helmSource = basicHelmfile(workloadInstance);
        workloadInstance.setHelmSources(Collections.singletonList(helmSource));
        when(workloadInstanceService.get(WORKLOAD_INSTANCE_ID)).thenReturn(workloadInstance);
        when(terminateService.terminate(workloadInstance, null, false)).thenReturn(workloadInstance);

        WorkloadInstanceDto result = requestCoordinatorService.terminateWorkloadInstance(WORKLOAD_INSTANCE_ID,
                                                                                         setWorkloadInstanceOperationPostRequestDto(),
                                                                                         null);
        assertThat(result).isNotNull();
    }

    @Test
    void shouldTerminateWorkloadInstanceSuccessfullyDeletionNamespaceWithoutCluster() {
        WorkloadInstance workloadInstance = basicWorkloadInstance(null);
        HelmSource helmSource = basicHelmfile(workloadInstance);
        workloadInstance.setHelmSources(Collections.singletonList(helmSource));
        when(workloadInstanceService.get(WORKLOAD_INSTANCE_ID)).thenReturn(workloadInstance);
        when(terminateService.terminate(workloadInstance, null, true)).thenReturn(workloadInstance);

        WorkloadInstanceDto result = requestCoordinatorService.terminateWorkloadInstance(WORKLOAD_INSTANCE_ID,
                setWorkloadInstanceOperationPostRequestDtoWithoutDeleteNamespace(),
                null);
        assertThat(result).isNotNull();
    }

    @Test
    void shouldTerminateWorkloadInstanceSuccessfullyWithCluster() {
        WorkloadInstance workloadInstance = basicWorkloadInstance(null);
        when(workloadInstanceService.get(WORKLOAD_INSTANCE_ID)).thenReturn(workloadInstance);
        when(terminateService.terminate(workloadInstance, clusterConnectionInfo, false)).thenReturn(workloadInstance);

        WorkloadInstanceDto result = requestCoordinatorService.terminateWorkloadInstance(WORKLOAD_INSTANCE_ID,
                                                                                         setWorkloadInstanceOperationPostRequestDto(),
                                                                                         clusterConnectionInfo);
        assertThat(result).isNotNull();
    }

    @Test
    void shouldTerminateWorkloadInstanceSuccessfullyWithClusterAndDeletionNamespace() {
        WorkloadInstance workloadInstance = basicWorkloadInstance(null);
        when(workloadInstanceService.get(WORKLOAD_INSTANCE_ID)).thenReturn(workloadInstance);
        when(terminateService.terminate(workloadInstance, clusterConnectionInfo, true)).thenReturn(workloadInstance);

        WorkloadInstanceDto result = requestCoordinatorService.terminateWorkloadInstance(WORKLOAD_INSTANCE_ID,
                setWorkloadInstanceOperationPostRequestDtoWithoutDeleteNamespace(),
                clusterConnectionInfo);
        assertThat(result).isNotNull();
    }

    @Test
    void shouldFailWhenPreviousOperationIsTerminate() {
        WorkloadInstance workloadInstance = basicWorkloadInstance(null);
        Operation previousOperation = Operation.builder()
                .type(OperationType.TERMINATE)
                .build();
        when(operationService.get(anyString())).thenReturn(previousOperation);
        when(workloadInstanceService.get(WORKLOAD_INSTANCE_ID)).thenReturn(workloadInstance);
        WorkloadInstanceOperationPostRequestDto workloadInstanceOperationPostRequestDto = setWorkloadInstanceOperationPostRequestDto();

        assertThatThrownBy(() ->
                                   requestCoordinatorService.terminateWorkloadInstance(WORKLOAD_INSTANCE_ID, workloadInstanceOperationPostRequestDto,
                                                                                       clusterConnectionInfo)
        ).isInstanceOf(InvalidInputException.class)

                .hasMessageContaining("Terminate can`t be executed for terminated workload instance");
    }

    @Test
    void shouldFailWhenTerminateWithInvalidType() {
        WorkloadInstance workloadInstance = basicWorkloadInstance(CLUSTER);
        when(workloadInstanceService.get(WORKLOAD_INSTANCE_ID)).thenReturn(workloadInstance);
        WorkloadInstanceOperationPostRequestDto workloadInstanceOperationPostRequestDto = setWorkloadInstanceOperationPostRequestDtoWithEmptyType();

        assertThatThrownBy(() -> requestCoordinatorService.terminateWorkloadInstance(WORKLOAD_INSTANCE_ID,
                                                                                     workloadInstanceOperationPostRequestDto, clusterConnectionInfo))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Only terminate type is allowed");
    }

    @Test
    void shouldReturnSuccessfulResponseWhenUpdate() {
        WorkloadInstance instance = basicWorkloadInstance(null);
        when(workloadInstanceService.get(WORKLOAD_INSTANCE_ID)).thenReturn(instance);
        when(updateService.update(any(), any(), any(WorkloadInstancePutRequestDto.class), any(), any())).thenReturn(instance);

        WorkloadInstanceDto result =
                requestCoordinatorService.update(WORKLOAD_INSTANCE_ID, setWorkloadInstancePutRequestDto(), helmSourceFile, values,
                                                 clusterConnectionInfo);

        assertThat(result.getWorkloadInstanceId()).isEqualTo(WORKLOAD_INSTANCE_ID);
        assertThat(result.getWorkloadInstanceName()).isEqualTo(WORKLOAD_INSTANCE_NAME);
        assertThat(result.getNamespace()).isEqualTo(NAMESPACE);
    }

    @Test
    void shouldReturnSuccessfulResponseWhenUpdateThroughHelmfileBuilder() {
        WorkloadInstance instance = basicWorkloadInstance(null);
        WorkloadInstanceWithChartsPutRequestDto requestDto = setWorkloadInstanceWithChartsPutRequestDto();
        when(workloadInstanceService.get(WORKLOAD_INSTANCE_ID)).thenReturn(instance);
        when(updateService.update(any(), any(), any(), any())).thenReturn(instance);

        WorkloadInstanceDto result = requestCoordinatorService.update(WORKLOAD_INSTANCE_ID,
                                                                      requestDto, values, null);

        assertThat(result.getWorkloadInstanceId()).isEqualTo(WORKLOAD_INSTANCE_ID);
        assertThat(result.getWorkloadInstanceName()).isEqualTo(WORKLOAD_INSTANCE_NAME);
        assertThat(result.getNamespace()).isEqualTo(NAMESPACE);
    }

    @Test
    void shouldReturnSuccessfulResponseWhenUpdateThroughHelmfileFetcher() {
        WorkloadInstance instance = basicWorkloadInstance(null);
        WorkloadInstanceWithURLPutRequestDto requestDto = setWorkloadInstanceWithURLPutRequestDto();
        when(workloadInstanceService.get(WORKLOAD_INSTANCE_ID)).thenReturn(instance);
        when(updateService.update(any(), anyBoolean(), any(), any(), any())).thenReturn(instance);

        WorkloadInstanceDto result = requestCoordinatorService.update(WORKLOAD_INSTANCE_ID,
                requestDto, Boolean.TRUE, values, null);

        assertThat(result.getWorkloadInstanceId()).isEqualTo(WORKLOAD_INSTANCE_ID);
        assertThat(result.getWorkloadInstanceName()).isEqualTo(WORKLOAD_INSTANCE_NAME);
        assertThat(result.getNamespace()).isEqualTo(NAMESPACE);
    }

    @Test
    void shouldGetWorkloadInstanceWithValidId() {
        //Init
        WorkloadInstance workloadInstance = getWorkloadInstance();
        WorkloadInstance updatedWorkloadInstance = getWorkloadInstance();
        updatedWorkloadInstance.setVersion(WORKLOAD_INSTANCE_VERSION);
        when(workloadInstanceService.get(WORKLOAD_INSTANCE_ID)).thenReturn(workloadInstance);

        //Test method
        WorkloadInstanceDto result = requestCoordinatorService.getWorkloadInstance(WORKLOAD_INSTANCE_ID);

        //Verify
        assertThat(result.getWorkloadInstanceId()).isEqualTo(WORKLOAD_INSTANCE_ID);
        assertThat(result.getWorkloadInstanceName()).isEqualTo(WORKLOAD_INSTANCE_NAME);
        assertThat(result.getNamespace()).isEqualTo(NAMESPACE);
        assertThat(result.getCluster()).isEqualTo(CLUSTER);
        Map<String, Object> resultAdditionalParameters = result.getAdditionalParameters();
        assertThat(resultAdditionalParameters).containsEntry("key", "value");
    }

    @Test
    void shouldReturnLatestOperationId() {
        //Init
        WorkloadInstance workloadInstance = getWorkloadInstance();
        workloadInstance.setLatestOperationId(OPERATION_ID);
        when(workloadInstanceService.get(WORKLOAD_INSTANCE_ID)).thenReturn(workloadInstance);

        //Test method
        String result = requestCoordinatorService.getLatestOperationId(WORKLOAD_INSTANCE_ID);

        //Verify
        assertThat(result).isEqualTo(OPERATION_ID);
    }

    @Test
    void shouldReturnSuccessfulResponseWhenRollback() {
        //Init
        WorkloadInstance instance = basicWorkloadInstance(null);
        WorkloadInstanceVersion workloadInstanceVersion = getVersion(instance);
        Operation operation = getOperation(OperationType.UPDATE);
        when(operationService.get(OPERATION_ID)).thenReturn(operation);
        when(workloadInstanceService.get(WORKLOAD_INSTANCE_ID)).thenReturn(instance);
        when(workloadInstanceVersionService.getVersionForRollback(instance, WORKLOAD_INSTANCE_VERSION_3, operation.getType()))
                .thenReturn(workloadInstanceVersion);
        when(rollbackService.rollback(any(), any(), any())).thenReturn(instance);
        //Test method
        WorkloadInstanceDto result =
                requestCoordinatorService.rollback(WORKLOAD_INSTANCE_ID, setWorkloadInstanceOperationPutRequestDto(WORKLOAD_INSTANCE_VERSION_3),
                                                   clusterConnectionInfo);

        //Verify
        assertThat(result.getWorkloadInstanceId()).isEqualTo(WORKLOAD_INSTANCE_ID);
        assertThat(result.getWorkloadInstanceName()).isEqualTo(WORKLOAD_INSTANCE_NAME);
        assertThat(result.getNamespace()).isEqualTo(NAMESPACE);
        assertThat(result.getVersion()).isEqualTo(WORKLOAD_INSTANCE_VERSION);
    }

    @Test
    void shouldFailWhenRollbackWithInvalidVersion() {
        //Init
        WorkloadInstance instance = basicWorkloadInstance(null);
        Operation operation = getOperation(OperationType.UPDATE);
        when(operationService.get(OPERATION_ID)).thenReturn(operation);
        when(workloadInstanceService.get(WORKLOAD_INSTANCE_ID)).thenReturn(instance);
        when(workloadInstanceVersionService.getVersionForRollback(instance, 3, operation.getType()))
                .thenThrow(new ResourceNotFoundException("Version 3 by workloadInstanceId = workload_instance_id was not found"));
        WorkloadInstanceOperationPutRequestDto workloadInstanceVersionThree = setWorkloadInstanceOperationPutRequestDto(WORKLOAD_INSTANCE_VERSION_3);

        //Test method
        assertThatThrownBy(() -> requestCoordinatorService.rollback(WORKLOAD_INSTANCE_ID, workloadInstanceVersionThree, clusterConnectionInfo))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Version " + WORKLOAD_INSTANCE_VERSION_3 + " by workloadInstanceId = workload_instance_id was not found");
    }

    @Test
    void shouldFailWhenRollbackAfterInstansiate() {
        //Init
        WorkloadInstance instance = basicWorkloadInstance(null);
        Operation operation = getOperation(OperationType.INSTANTIATE);
        when(operationService.get(OPERATION_ID)).thenReturn(operation);
        when(workloadInstanceService.get(WORKLOAD_INSTANCE_ID)).thenReturn(instance);
        WorkloadInstanceOperationPutRequestDto workloadInstanceVersionOne = setWorkloadInstanceOperationPutRequestDto(
                WORKLOAD_INSTANCE_VERSION);
        //Test method
        assertThatThrownBy(() -> requestCoordinatorService.rollback(WORKLOAD_INSTANCE_ID, workloadInstanceVersionOne, clusterConnectionInfo))
                .isInstanceOf(IncorrectRollbackRequestException.class)
                .hasMessageContaining("Manual Rollback request cannot been received right after INSTANTIATE, if you have intention to terminate this"
                                              + " instance, please use terminate request");

        verify(rollbackService, never()).rollback(any(), any(), any());
        verify(workloadInstanceVersionService, never()).getVersionForRollback(any(), anyInt(), any());
    }

    @Test
    void shouldFailWhenRollbackAfterRollbackWithoutVersion() {
        //Init
        WorkloadInstance instance = basicWorkloadInstance(null);
        instance.setPreviousVersion(null);
        Operation operation = getOperation(OperationType.ROLLBACK);
        when(operationService.get(OPERATION_ID)).thenReturn(operation);
        when(workloadInstanceService.get(WORKLOAD_INSTANCE_ID)).thenReturn(instance);
        WorkloadInstanceOperationPutRequestDto workloadInstanceVersionOne = setWorkloadInstanceOperationPutRequestDto(WORKLOAD_INSTANCE_VERSION);

        //Test method
        assertThatThrownBy(() -> requestCoordinatorService.rollback(WORKLOAD_INSTANCE_ID, workloadInstanceVersionOne, clusterConnectionInfo))
                .isInstanceOf(IncorrectRollbackRequestException.class)
                .hasMessageContaining("The version which you want to rollback is actual");

        verify(rollbackService, never()).rollback(any(), any(), any());
        verify(workloadInstanceVersionService, never()).getVersionForRollback(any(), anyInt(), any());
    }

    @Test
    void shouldFailWhenRollbackToTheActualVersion() {
        //Init
        WorkloadInstance instance = basicWorkloadInstance(null);
        Operation operation = getOperation(OperationType.UPDATE);
        when(operationService.get(OPERATION_ID)).thenReturn(operation);
        when(workloadInstanceService.get(WORKLOAD_INSTANCE_ID)).thenReturn(instance);
        WorkloadInstanceOperationPutRequestDto workloadInstanceVersionOne = setWorkloadInstanceOperationPutRequestDto(WORKLOAD_INSTANCE_VERSION);

        //Test method
        assertThatThrownBy(() -> requestCoordinatorService.rollback(WORKLOAD_INSTANCE_ID, workloadInstanceVersionOne, clusterConnectionInfo))
                .isInstanceOf(IncorrectRollbackRequestException.class)
                .hasMessageContaining("The version which you want to rollback is actual");

        verify(rollbackService, never()).rollback(any(), any(), any());
        verify(workloadInstanceVersionService, never()).getVersionForRollback(any(), anyInt(), any());
    }

    @Test
    void shouldFailWhenRollbackAfterReinstantiateWithoutPreviousVersion() {
        //Init
        WorkloadInstance instance = basicWorkloadInstance(null);
        Operation operation = getOperation(OperationType.REINSTANTIATE);
        instance.setPreviousVersion(null);
        when(operationService.get(OPERATION_ID)).thenReturn(operation);
        when(workloadInstanceService.get(WORKLOAD_INSTANCE_ID)).thenReturn(instance);
        WorkloadInstanceOperationPutRequestDto workloadInstanceVersionOne = setWorkloadInstanceOperationPutRequestDto(WORKLOAD_INSTANCE_VERSION);

        //Test method
        assertThatThrownBy(() -> requestCoordinatorService.rollback(WORKLOAD_INSTANCE_ID, workloadInstanceVersionOne, clusterConnectionInfo))
                .isInstanceOf(IncorrectRollbackRequestException.class)
                .hasMessageContaining("Manual Rollback request cannot been received because there is no UPDATES been done");

        verify(rollbackService, never()).rollback(any(), any(), any());
        verify(workloadInstanceVersionService, never()).getVersionForRollback(any(), anyInt(), any());
    }

    @Test
    void shouldFailWhenRollbackAfterTerminate() {
        //Init
        WorkloadInstance instance = basicWorkloadInstance(null);
        Operation operation = getOperation(OperationType.TERMINATE);
        when(operationService.get(OPERATION_ID)).thenReturn(operation);
        when(workloadInstanceService.get(WORKLOAD_INSTANCE_ID)).thenReturn(instance);
        WorkloadInstanceOperationPutRequestDto workloadInstanceVersionOne = setWorkloadInstanceOperationPutRequestDto(
                WORKLOAD_INSTANCE_VERSION);
        //Test method
        assertThatThrownBy(() -> requestCoordinatorService.rollback(WORKLOAD_INSTANCE_ID, workloadInstanceVersionOne, clusterConnectionInfo))
                .isInstanceOf(IncorrectRollbackRequestException.class)
                .hasMessageContaining("Manual Rollback can not be done when instance is terminated, please reinstantiate it first");

        verify(rollbackService, never()).rollback(any(), any(), any());
        verify(workloadInstanceVersionService, never()).getVersionForRollback(any(), anyInt(), any());
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
                .state(OperationState.COMPLETED)
                .id(OPERATION_ID)
                .build();
    }

    private Operation failedInstantiateOperation(WorkloadInstance workloadInstance, OperationState state) {
        return Operation.builder()
                .helmSourceId(HELMSOURCE_ID)
                .workloadInstance(workloadInstance)
                .type(OperationType.ROLLBACK)
                .startTime(LocalDateTime.now())
                .state(state)
                .id(OPERATION_ID)
                .build();
    }

    private WorkloadInstance failedWorkloadInstance(OperationState state) {
        Operation operation = Operation.builder().type(OperationType.ROLLBACK).state(state).build();
        return WorkloadInstance.builder()
                .workloadInstanceName(WORKLOAD_INSTANCE_NAME)
                .workloadInstanceId(WORKLOAD_INSTANCE_ID)
                .latestOperationId(OPERATION_ID)
                .version(WORKLOAD_INSTANCE_VERSION)
                .operations(Collections.singletonList(operation))
                .build();
    }

    private WorkloadInstance failedWorkloadInstanceWithTwoOperations() {
        Operation operation = Operation.builder().type(OperationType.ROLLBACK).state(OperationState.FAILED).build();
        return WorkloadInstance.builder()
                .workloadInstanceName(WORKLOAD_INSTANCE_NAME)
                .workloadInstanceId(WORKLOAD_INSTANCE_ID)
                .latestOperationId(OPERATION_ID)
                .version(WORKLOAD_INSTANCE_VERSION)
                .operations(List.of(operation, operation))
                .build();
    }

    private WorkloadInstance basicWorkloadInstance(final String cluster) {
        List<HelmSource> helmSources = new ArrayList<>();
        helmSources.add(getHelmSource());
        return WorkloadInstance.builder()
                .workloadInstanceName(WORKLOAD_INSTANCE_NAME)
                .workloadInstanceId(WORKLOAD_INSTANCE_ID)
                .latestOperationId(OPERATION_ID)
                .version(WORKLOAD_INSTANCE_VERSION)
                .cluster(cluster)
                .namespace(NAMESPACE)
                .crdNamespace(GLOBAL_CRD_NAMESPACE_VALUE)
                .additionalParameters(ADDITIONAL_PARAMETERS)
                .latestOperationId(OPERATION_ID)
                .helmSources(helmSources)
                .clusterIdentifier(CLUSTER_IDENT)
                .build();
    }

    private HelmSource basicHelmfile(WorkloadInstance workloadInstance) {
        return HelmSource.builder()
                .content(new byte[] {})
                .workloadInstance(workloadInstance)
                .helmSourceType(HelmSourceType.HELMFILE)
                .created(LocalDateTime.now())
                .id(HELMSOURCE_ID)
                .build();
    }

    private WorkloadInstance getWorkloadInstance() {
        return WorkloadInstance.builder()
                .workloadInstanceId(WORKLOAD_INSTANCE_ID)
                .workloadInstanceName(WORKLOAD_INSTANCE_NAME)
                .namespace(NAMESPACE)
                .cluster(CLUSTER)
                .additionalParameters(ADDITIONAL_PARAMETERS)
                .helmSources(Collections.singletonList(getHelmSource()))
                .build();
    }

    private HelmSource getHelmSource() {
        return HelmSource.builder()
                .id(HELMSOURCE_ID)
                .helmSourceType(HelmSourceType.HELMFILE)
                .helmSourceVersion(HELM_SOURCE_VERSION)
                .build();
    }

    private Operation getOperation(OperationType operationType) {
        return Operation.builder()
                .id(OPERATION_ID)
                .type(operationType)
                .build();
    }

    private WorkloadInstancePostRequestDto setWorkloadInstancePostRequestDto() {
        WorkloadInstancePostRequestDto postRequestDto = new WorkloadInstancePostRequestDto();
        postRequestDto.setWorkloadInstanceName(WORKLOAD_INSTANCE_NAME);
        postRequestDto.setNamespace(NAMESPACE);
        return postRequestDto;
    }

    private WorkloadInstanceWithChartsRequestDto setWorkloadInstanceWithChartsPostRequestDto() {
        WorkloadInstanceWithChartsRequestDto postRequestDto = new WorkloadInstanceWithChartsRequestDto();
        postRequestDto.setWorkloadInstanceName(WORKLOAD_INSTANCE_NAME);
        postRequestDto.setNamespace(NAMESPACE);
        postRequestDto.setCharts(getCharts("release-1", "release-2"));
        postRequestDto.setRepository(REPOSITORY);
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

    private WorkloadInstanceOperationPostRequestDto setWorkloadInstanceOperationPostRequestDto() {
        WorkloadInstanceOperationPostRequestDto operationPostRequestDto = new WorkloadInstanceOperationPostRequestDto();
        operationPostRequestDto.setType(TERMINATE);
        operationPostRequestDto.deleteNamespace(Boolean.FALSE);
        return operationPostRequestDto;
    }

    private WorkloadInstanceOperationPostRequestDto setWorkloadInstanceOperationPostRequestDtoWithoutDeleteNamespace() {
        WorkloadInstanceOperationPostRequestDto operationPostRequestDto = new WorkloadInstanceOperationPostRequestDto();
        operationPostRequestDto.setType(TERMINATE);
        return operationPostRequestDto;
    }

    private WorkloadInstanceOperationPostRequestDto setWorkloadInstanceOperationPostRequestDtoWithEmptyType() {
        WorkloadInstanceOperationPostRequestDto operationPostRequestDto = new WorkloadInstanceOperationPostRequestDto();
        operationPostRequestDto.setType(" ");
        return operationPostRequestDto;
    }

    private WorkloadInstancePutRequestDto setWorkloadInstancePutRequestDto() {
        WorkloadInstancePutRequestDto workloadInstancePutRequestDto = new WorkloadInstancePutRequestDto();
        workloadInstancePutRequestDto.setAdditionalParameters(Collections.singletonMap(ADDITIONAL_PARAMETERS_TEST_KEY, " "));
        return workloadInstancePutRequestDto;
    }

    private WorkloadInstanceWithChartsPutRequestDto setWorkloadInstanceWithChartsPutRequestDto() {
        WorkloadInstanceWithChartsPutRequestDto requestDto = new WorkloadInstanceWithChartsPutRequestDto();
        requestDto.setAdditionalParameters(Collections.singletonMap(ADDITIONAL_PARAMETERS_TEST_KEY, " "));
        requestDto.setCharts(getCharts("release-1", "release-2", "release-3"));
        requestDto.setRepository(REPOSITORY);
        return requestDto;
    }

    private WorkloadInstanceWithURLPutRequestDto setWorkloadInstanceWithURLPutRequestDto() {
        WorkloadInstanceWithURLPutRequestDto requestDto = new WorkloadInstanceWithURLPutRequestDto();
        requestDto.setUrl(HELMSOURCE_PATH);
        requestDto.setAdditionalParameters(Collections.singletonMap(ADDITIONAL_PARAMETERS_TEST_KEY, " "));
        return requestDto;
    }

    private WorkloadInstanceOperationPutRequestDto setWorkloadInstanceOperationPutRequestDto(Integer version) {
        WorkloadInstanceOperationPutRequestDto operationPutRequestDto = new WorkloadInstanceOperationPutRequestDto();
        operationPutRequestDto.setVersion(version);
        return operationPutRequestDto;
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
