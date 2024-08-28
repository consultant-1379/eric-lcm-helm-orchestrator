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

package com.ericsson.oss.management.lcm.presentation.services.workloadinstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.KUBE_CONFIG;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.api.model.PagedWorkloadInstanceDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceDto;
import com.ericsson.oss.management.lcm.model.entity.ClusterConnectionInfo;
import com.ericsson.oss.management.lcm.model.entity.ClusterConnectionInfoInstance;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.OperationState;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidInputException;
import com.ericsson.oss.management.lcm.presentation.exceptions.NotUniqueWorkloadInstanceException;
import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import com.ericsson.oss.management.lcm.presentation.mappers.WorkloadInstanceDtoMapper;
import com.ericsson.oss.management.lcm.repositories.WorkloadInstanceRepository;
import com.ericsson.oss.management.lcm.utils.TestingFileUtils;

@ActiveProfiles("test")
@SpringBootTest()
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WorkloadInstanceServiceTest extends AbstractDbSetupTest {

    private static final String VALID_INSTANCE_NAME = "name";
    private static final String INVALID_INSTANCE_NAME = "InvalidName-";
    private static final String VALID_NAMESPACE = "7namespace-3-test";
    private static final String VALID_CRD_NAMESPACE = "crd-7namespace-3-test";
    private static final String INVALID_NAMESPACE = "InvalidNameSpace-";
    private static final String VALID_ID = "validId";
    private static final String SECOND_VALID_ID = "validId2";
    private static final String INVALID_ID = "invalidId";
    private static final String VALID_CLUSTER = "cluster name";
    private static final Map<String, Object> ADDITIONAL_PARAMETERS_MAP = Collections.singletonMap("testKey", "testValue");
    private static final String ADDITIONAL_PARAMETERS_JSON = "{\"testKey\": \"testValue\"}";
    private static final String UPDATED_ADDITIONAL_PARAMETERS = "additionalParameters_updated";
    private static final Integer PAGE = 2;
    private static final Integer SIZE = 2;
    private static final String SORT = "workloadInstanceId,asc";
    private static final String CLUSTER_CONNECTION_INFO = "clusterConnectionInfo";
    private static final String CLUSTER_CONNECTION_INFO_YAML = "clusterConnectionInfo.yaml";
    private static final String CLUSTER_CONNECTION_INFO_CRD_NAMESPACE = "cluster-connection-info-crd-namespace";
    private static final String VALUES_VERSION = "0e35ed30-d438-4b07-a82b-cab447424d30";
    private static final String HELM_SOURCE_VERSION = "1.2.3-4";
    private static final String NAMESPACE = "NAMESPACE";
    private static final String CLUSTER_IDENTIFIER = "CLUSTER";
    private static MockMultipartFile configPart;

    @Autowired
    private WorkloadInstanceServiceImpl workloadInstanceService;

    @MockBean
    private WorkloadInstanceRepository workloadInstanceRepository;

    @MockBean
    private WorkloadInstanceDtoMapper workloadInstanceDtoMapper;

    @Value("${default.crd.namespace}")
    private String defaultCrdNamespace;

    @BeforeAll
    static void filesSetup() throws Exception {
        String connectionInfo = TestingFileUtils.readDataFromFile(CLUSTER_CONNECTION_INFO_YAML);
        configPart = new MockMultipartFile(CLUSTER_CONNECTION_INFO, KUBE_CONFIG, MediaType.TEXT_PLAIN_VALUE,
                                                             connectionInfo.getBytes());
    }

    @Test
    void shouldSaveWhenCreateWithValidInstance() {
        //Init
        WorkloadInstance workloadInstance = getInstance(VALID_ID);
        when(workloadInstanceRepository.save(workloadInstance)).thenReturn(workloadInstance);

        //Test method
        WorkloadInstance result = workloadInstanceService.create(workloadInstance);

        //Verify
        verify(workloadInstanceRepository, times(1)).save(workloadInstance);
        assertThat(result).isNotNull();
    }

    @Test
    void shouldThrowExceptionWhenCreateWithInvalidInstanceName() {
        //Init
        WorkloadInstance workloadInstance = getInstance(VALID_ID);
        workloadInstance.setWorkloadInstanceName(INVALID_INSTANCE_NAME);

        //Test method
        assertThatThrownBy(() -> workloadInstanceService.create(workloadInstance))
                .isInstanceOf(InvalidInputException.class);

        //Verify
        verify(workloadInstanceRepository, times(0)).save(workloadInstance);
    }

    @Test
    void shouldThrowExceptionWhenCreateWithInvalidInstanceNamespace() {
        //Init
        WorkloadInstance workloadInstance = getInstance(VALID_ID);
        workloadInstance.setNamespace(INVALID_NAMESPACE);

        //Test method
        assertThatThrownBy(() -> workloadInstanceService.create(workloadInstance))
                .isInstanceOf(InvalidInputException.class);

        //Verify
        verify(workloadInstanceRepository, times(0)).save(workloadInstance);
    }

    @Test
    void shouldThrowExceptionWhenCreateWithInvalidNameNamespaceClusterCombination() {
        WorkloadInstance workloadInstance = getInstance(VALID_ID);
        when(workloadInstanceRepository.existsByWorkloadInstanceName(any())).thenReturn(true);

        assertThatThrownBy(() -> workloadInstanceService.create(workloadInstance))
                .isInstanceOf(NotUniqueWorkloadInstanceException.class);
    }

    @Test
    void shouldReturnWorkloadInstanceWhenGetValidId() {
        //Init
        WorkloadInstance workloadInstance = getInstance(VALID_ID);
        when(workloadInstanceRepository.findById(VALID_ID)).thenReturn(Optional.of(workloadInstance));

        //Test method
        WorkloadInstance result = workloadInstanceService.get(VALID_ID);

        //Verify
        verify(workloadInstanceRepository, times(1)).findById(VALID_ID);
        assertThat(result).isNotNull();
    }

    @Test
    void shouldThrowExceptionWhenGetInvalidId() {
        //Init
        when(workloadInstanceRepository.findById(INVALID_ID)).thenReturn(Optional.empty());

        //Test method
        assertThatThrownBy(() -> workloadInstanceService.get(INVALID_ID)).isInstanceOf(ResourceNotFoundException.class);

        //Verify
        verify(workloadInstanceRepository, times(1)).findById(INVALID_ID);
    }

    @Test
    void shouldReturnPagedOperationDto() {
        //Init
        List<String> sortList = Collections.singletonList(SORT);
        Pageable pageable = PageRequest.of(PAGE, SIZE);
        Page<WorkloadInstance> resultsPage = new PageImpl<>(getWorkloadInstancesByPage(), pageable, 9L);

        WorkloadInstanceDto workloadInstanceDto1 = getWorkloadInstanceDto(VALID_ID);
        WorkloadInstanceDto workloadInstanceDto2 = getWorkloadInstanceDto(SECOND_VALID_ID);

        when(workloadInstanceRepository.findAll(any(Pageable.class))).thenReturn(resultsPage);
        when(workloadInstanceDtoMapper.toWorkloadInstanceDto(any(WorkloadInstance.class)))
                .thenReturn(workloadInstanceDto1)
                .thenReturn(workloadInstanceDto2);

        //Test method
        PagedWorkloadInstanceDto result = workloadInstanceService.getAllWorkloadInstances(PAGE, SIZE, sortList);

        //Verify
        verify(workloadInstanceRepository, times(1)).findAll(any(Pageable.class));
        verify(workloadInstanceDtoMapper, times(2)).toWorkloadInstanceDto(any());
        Assertions.assertThat(result.getContent().size()).isEqualTo(SIZE);
    }

    @Test
    void shouldReturnUpdatedWorkloadInstanceWhenUpdate() {
        //Init
        WorkloadInstance workloadInstance = getInstance(VALID_ID);
        workloadInstance.setAdditionalParameters(UPDATED_ADDITIONAL_PARAMETERS);
        WorkloadInstance savedWorkloadInstance = getInstance(VALID_ID);
        when(workloadInstanceRepository.findById(VALID_ID)).thenReturn(Optional.of(savedWorkloadInstance));
        when(workloadInstanceRepository.save(any(WorkloadInstance.class))).thenReturn(savedWorkloadInstance);

        //Test method
        WorkloadInstance result = workloadInstanceService.updateAdditionalParameters(workloadInstance);

        //Verify
        verify(workloadInstanceRepository, times(1)).findById(VALID_ID);
        verify(workloadInstanceRepository, times(1)).save(any(WorkloadInstance.class));
        assertThat(result.getAdditionalParameters()).isEqualTo(workloadInstance.getAdditionalParameters());
    }

    @Test
    void shouldCallDeleteMethodWhenDelete() {

        //Test method
        workloadInstanceService.delete(VALID_ID);

        //Verify
        verify(workloadInstanceRepository, times(1)).deleteById(VALID_ID);
    }

    @Test
    void shouldFailWorkloadInstanceNotFoundWhenDelete() {
        //Init
        doThrow(new EmptyResultDataAccessException(0)).when(workloadInstanceRepository).deleteById(INVALID_ID);

        //Test method
        assertThatThrownBy(() -> workloadInstanceService.delete(INVALID_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldNotChangeCrdNamespaceWhenClusterConnectionInfoFileExistAndCrdNamespaceInWorkloadInstanceExist() {
        //Init
        WorkloadInstance workloadInstance = getInstance(VALID_ID);
        String initialCrdNamespace = workloadInstance.getCrdNamespace();

        //Test method
        workloadInstanceService.updateCrdNamespaceIfRequired(workloadInstance, configPart);

        //Verify
        assertThat(initialCrdNamespace).isEqualTo(workloadInstance.getCrdNamespace());
    }

    @Test
    void shouldReturnTrueIfAllWorkloadInstancesTerminated() {
        //Init
        when(workloadInstanceRepository.getAllByNamespaceAndClusterIdentifier(anyString(), anyString()))
                .thenReturn(getWorkloadInstancesWithAllOperations());

        //Test method
        Boolean result = workloadInstanceService.validateInstancesForDeletionInNamespace(NAMESPACE, CLUSTER_IDENTIFIER);

        //Verify
        assertThat(result).isEqualTo(Boolean.TRUE);
    }

    @Test
    void shouldReturnTrueIfWorkloadInstanceNull() {
        when(workloadInstanceRepository.getAllByNamespaceAndClusterIdentifier(anyString(), anyString())).thenReturn(null);

        //Test method
        Boolean result = workloadInstanceService.validateInstancesForDeletionInNamespace(NAMESPACE, CLUSTER_IDENTIFIER);

        //Verify
        assertThat(result).isEqualTo(Boolean.TRUE);
    }

    @Test
    void shouldReturnFalseIfWorkloadInstanceNotTerminated() {
        when(workloadInstanceRepository.getAllByNamespaceAndClusterIdentifier(anyString(), anyString()))
                .thenReturn(getWorkloadInstancesTerminatedOperations());

        //Test method
        Boolean result = workloadInstanceService.validateInstancesForDeletionInNamespace(NAMESPACE, CLUSTER_IDENTIFIER);

        //Verify
        assertThat(result).isEqualTo(Boolean.FALSE);
    }

    @Test
    void shouldReturnFalseIfWorkloadInstanceTerminationInProcess() {
        when(workloadInstanceRepository.getAllByNamespaceAndClusterIdentifier(anyString(), anyString()))
                .thenReturn(getWorkloadInstancesProcessingTerminationOperations());

        //Test method
        Boolean result = workloadInstanceService.validateInstancesForDeletionInNamespace(NAMESPACE, CLUSTER_IDENTIFIER);

        //Verify
        assertThat(result).isEqualTo(Boolean.FALSE);
    }

    @Test
    void shouldReturnFalseIfTwoWorkloadInstanceTerminationInProcess() {
        when(workloadInstanceRepository.getAllByNamespaceAndClusterIdentifier(anyString(), anyString()))
                .thenReturn(getTwoWorkloadInstancesWithAllOperations());

        //Test method
        Boolean result = workloadInstanceService.validateInstancesForDeletionInNamespace(NAMESPACE, CLUSTER_IDENTIFIER);

        //Verify
        assertThat(result).isEqualTo(Boolean.FALSE);
    }

    @Test
    void shouldReturnTrueIfWorkloadInstanceRollbackCompleted() {
        when(workloadInstanceRepository.getAllByNamespaceAndClusterIdentifier(anyString(), anyString()))
                .thenReturn(getWorkloadInstancesCompletedRollback());

        //Test method
        Boolean result = workloadInstanceService.validateInstancesForDeletionInNamespace(NAMESPACE, CLUSTER_IDENTIFIER);

        //Verify
        assertThat(result).isEqualTo(Boolean.TRUE);
    }

    @Test
    void shouldSetCrdNamespaceFromClusterConnectionInfoInstance() {
        //Init
        WorkloadInstance workloadInstance = getInstance(VALID_ID);
        String initialCrdNamespace = workloadInstance.getCrdNamespace();

        //Test method
        workloadInstanceService.updateCrdNamespaceIfRequired(workloadInstance, null);

        //Verify
        assertThat(initialCrdNamespace).isNotEqualTo(workloadInstance.getCrdNamespace());
        assertThat(workloadInstance.getCrdNamespace()).isEqualTo(CLUSTER_CONNECTION_INFO_CRD_NAMESPACE);
    }

    @Test
    void shouldSetDefaultCrdNamespaceWhenClusterConnectionInfoFileExistAndCrdNamespaceInWorkloadInstanceIsEmpty() {
        //Init
        WorkloadInstance workloadInstance = getInstance(VALID_ID);
        workloadInstance.setCrdNamespace(null);

        //Test method
        workloadInstanceService.updateCrdNamespaceIfRequired(workloadInstance, configPart);

        //Verify
        assertThat(defaultCrdNamespace).isEqualTo(workloadInstance.getCrdNamespace());
    }

    @Test
    void shouldSetDefaultCrdNamespaceWhenClusterConnectionInfoFileNotExistAndClusterInWorkloadInstanceIsEmpty() {
        //Init
        WorkloadInstance workloadInstance = getInstance(VALID_ID);
        workloadInstance.setCluster(null);

        //Test method
        workloadInstanceService.updateCrdNamespaceIfRequired(workloadInstance, null);

        //Verify
        assertThat(defaultCrdNamespace).isEqualTo(workloadInstance.getCrdNamespace());
    }

    @Test
    void shouldSetDefaultCrdNamespaceWhenClusterConnectionInfoFileNotExistAndClusterAndConnectionInfoInWorkloadInstanceAreEmpty() {
        //Init
        ClusterConnectionInfo clusterConnectionInfo = getClusterConnectionInfo();
        clusterConnectionInfo.setCrdNamespace(null);
        ClusterConnectionInfoInstance clusterConnectionInfoInstance = getClusterConnectionInfoInstance();
        clusterConnectionInfoInstance.setClusterConnectionInfo(clusterConnectionInfo);
        WorkloadInstance workloadInstance = getInstance(VALID_ID);
        workloadInstance.setClusterConnectionInfoInstance(clusterConnectionInfoInstance);

        //Test method
        workloadInstanceService.updateCrdNamespaceIfRequired(workloadInstance, null);

        //Verify
        assertThat(defaultCrdNamespace).isEqualTo(workloadInstance.getCrdNamespace());
    }

    @Test
    void shouldThrowExceptionWhenClusterConnectionInfoFileNotExistClusterConnectionInfoInstanceNotExist() {
        //Init
        WorkloadInstance workloadInstance = getInstance(VALID_ID);
        workloadInstance.setClusterConnectionInfoInstance(null);

        //Test method
        assertThatThrownBy(() -> workloadInstanceService.updateCrdNamespaceIfRequired(workloadInstance, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("does not contain a ClusterConnectionInfo");
    }

    @Test
    void shouldSetPrimaryVersionSuccessfully() {
        WorkloadInstance workloadInstance = getInstance(VALID_ID);
        WorkloadInstanceVersion version = getVersion(workloadInstance);
        workloadInstanceService.updateVersion(workloadInstance, version);

        assertThat(workloadInstance.getVersion()).isEqualTo(1);
        assertThat(workloadInstance.getPreviousVersion()).isNull();
        verify(workloadInstanceRepository).save(any());
    }

    @Test
    void shouldUpdateVersionSuccessfully() {
        WorkloadInstance workloadInstance = getInstance(VALID_ID);
        workloadInstance.setVersion(1);
        WorkloadInstanceVersion version = getVersion(workloadInstance);
        version.setVersion(2);
        workloadInstanceService.updateVersion(workloadInstance, version);

        assertThat(workloadInstance.getVersion()).isEqualTo(2);
        assertThat(workloadInstance.getPreviousVersion()).isEqualTo(1);
        verify(workloadInstanceRepository).save(any());
    }

    private WorkloadInstance getInstance(String workloadInstanceId) {
        return WorkloadInstance.builder()
                .workloadInstanceId(workloadInstanceId)
                .workloadInstanceName(VALID_INSTANCE_NAME)
                .namespace(VALID_NAMESPACE)
                .crdNamespace(VALID_CRD_NAMESPACE)
                .cluster(VALID_CLUSTER)
                .additionalParameters(ADDITIONAL_PARAMETERS_JSON)
                .clusterConnectionInfoInstance(getClusterConnectionInfoInstance())
                .build();
    }

    private ClusterConnectionInfoInstance getClusterConnectionInfoInstance() {
        return ClusterConnectionInfoInstance.builder()
                .clusterConnectionInfo(getClusterConnectionInfo())
                .build();
    }

    private ClusterConnectionInfo getClusterConnectionInfo() {
        return ClusterConnectionInfo.builder()
                .crdNamespace(CLUSTER_CONNECTION_INFO_CRD_NAMESPACE)
                .build();
    }

    private WorkloadInstanceDto getWorkloadInstanceDto(String id) {
        WorkloadInstanceDto workloadInstanceDto = new WorkloadInstanceDto();
        workloadInstanceDto.setWorkloadInstanceId(id);
        workloadInstanceDto.setWorkloadInstanceName(VALID_INSTANCE_NAME);
        workloadInstanceDto.setNamespace(VALID_NAMESPACE);
        workloadInstanceDto.setCrdNamespace(VALID_CRD_NAMESPACE);
        workloadInstanceDto.setCluster(VALID_CLUSTER);
        workloadInstanceDto.setAdditionalParameters(ADDITIONAL_PARAMETERS_MAP);
        return workloadInstanceDto;
    }

    private List<WorkloadInstance> getWorkloadInstancesByPage() {
        List<WorkloadInstance> pagesList = new ArrayList<>();
        pagesList.add(getInstance(VALID_ID));
        pagesList.add(getInstance(SECOND_VALID_ID));
        return pagesList;
    }

    private WorkloadInstanceVersion getVersion(WorkloadInstance workloadInstance) {
        return WorkloadInstanceVersion.builder()
                .workloadInstance(workloadInstance)
                .valuesVersion(VALUES_VERSION)
                .helmSourceVersion(HELM_SOURCE_VERSION)
                .version(1)
                .id("some_random_id")
                .build();
    }

    private List<WorkloadInstance> getWorkloadInstancesWithAllOperations() {
        return List.of(WorkloadInstance.builder()
                .workloadInstanceName(VALID_INSTANCE_NAME)
                .namespace(VALID_NAMESPACE)
                .crdNamespace(VALID_CRD_NAMESPACE)
                .cluster(VALID_CLUSTER)
                        .latestOperationId("3")
                .additionalParameters(ADDITIONAL_PARAMETERS_JSON)
                .clusterConnectionInfoInstance(getClusterConnectionInfoInstance())
                        .operations(List.of(Operation.builder().id("1").state(OperationState.COMPLETED).type(OperationType.INSTANTIATE).build(),
                                Operation.builder().id("2").state(OperationState.PROCESSING).type(OperationType.TERMINATE).build(),
                                Operation.builder().id("3").state(OperationState.COMPLETED).type(OperationType.TERMINATE).build()))
                .build());
    }

    private List<WorkloadInstance> getTwoWorkloadInstancesWithAllOperations() {
        return List.of(WorkloadInstance.builder()
                        .workloadInstanceName(VALID_INSTANCE_NAME)
                        .namespace(VALID_NAMESPACE)
                        .crdNamespace(VALID_CRD_NAMESPACE)
                        .cluster(VALID_CLUSTER)
                        .latestOperationId("2")
                        .additionalParameters(ADDITIONAL_PARAMETERS_JSON)
                        .clusterConnectionInfoInstance(getClusterConnectionInfoInstance())
                        .operations(List.of(Operation.builder().id("1").state(OperationState.COMPLETED).type(OperationType.INSTANTIATE).build(),
                                Operation.builder().id("2").state(OperationState.PROCESSING).type(OperationType.TERMINATE).build()))
                        .build(),
                WorkloadInstance.builder()
                        .workloadInstanceName(VALID_INSTANCE_NAME)
                        .namespace(VALID_NAMESPACE)
                        .crdNamespace(VALID_CRD_NAMESPACE)
                        .cluster(VALID_CLUSTER)
                        .latestOperationId("1")
                        .additionalParameters(ADDITIONAL_PARAMETERS_JSON)
                        .clusterConnectionInfoInstance(getClusterConnectionInfoInstance())
                        .operations(List.of(Operation.builder().id("1").state(OperationState.COMPLETED).type(OperationType.INSTANTIATE).build(),
                                Operation.builder().id("2").state(OperationState.PROCESSING).type(OperationType.TERMINATE).build()))
                        .build()
                );
    }

    private List<WorkloadInstance> getWorkloadInstancesTerminatedOperations() {
        return List.of(WorkloadInstance.builder()
                .workloadInstanceName(VALID_INSTANCE_NAME)
                .namespace(VALID_NAMESPACE)
                .crdNamespace(VALID_CRD_NAMESPACE)
                .cluster(VALID_CLUSTER)
                .latestOperationId("2")
                .additionalParameters(ADDITIONAL_PARAMETERS_JSON)
                .clusterConnectionInfoInstance(getClusterConnectionInfoInstance())
                .operations(List.of(Operation.builder().id("1").state(OperationState.COMPLETED).type(OperationType.INSTANTIATE).build(),
                        Operation.builder().id("2").state(OperationState.PROCESSING).type(OperationType.TERMINATE).build()))
                .build());
    }

    private List<WorkloadInstance> getWorkloadInstancesProcessingTerminationOperations() {
        return List.of(WorkloadInstance.builder()
                .workloadInstanceName(VALID_INSTANCE_NAME)
                .namespace(VALID_NAMESPACE)
                .crdNamespace(VALID_CRD_NAMESPACE)
                .cluster(VALID_CLUSTER)
                .latestOperationId("1")
                .additionalParameters(ADDITIONAL_PARAMETERS_JSON)
                .clusterConnectionInfoInstance(getClusterConnectionInfoInstance())
                .operations(List.of(Operation.builder().id("2").state(OperationState.COMPLETED).type(OperationType.TERMINATE).build(),
                        Operation.builder().id("1").state(OperationState.PROCESSING).type(OperationType.TERMINATE).build()))
                .build());
    }

    private List<WorkloadInstance> getWorkloadInstancesCompletedRollback() {
        return List.of(WorkloadInstance.builder()
                .workloadInstanceName(VALID_INSTANCE_NAME)
                .namespace(VALID_NAMESPACE)
                .crdNamespace(VALID_CRD_NAMESPACE)
                .cluster(VALID_CLUSTER)
                .additionalParameters(ADDITIONAL_PARAMETERS_JSON)
                .clusterConnectionInfoInstance(getClusterConnectionInfoInstance())
                .operations(List.of(Operation.builder().state(OperationState.COMPLETED).type(OperationType.ROLLBACK).build()))
                .build());
    }
}
