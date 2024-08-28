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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import static com.ericsson.oss.management.lcm.constants.ClusterConnectionInfoConstants.CONTENT_TYPE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.ericsson.oss.management.lcm.presentation.exceptions.InternalRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.HelmfileExecutorApplication;
import com.ericsson.oss.management.lcm.api.model.ClusterConnectionInfoDto;
import com.ericsson.oss.management.lcm.model.entity.ClusterConnectionInfo;
import com.ericsson.oss.management.lcm.model.entity.ClusterConnectionInfoInstance;
import com.ericsson.oss.management.lcm.model.entity.ConnectionInfoStatus;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.presentation.exceptions.ClusterConnectionInfoConnectionException;
import com.ericsson.oss.management.lcm.presentation.exceptions.NotUniqueClusterException;
import com.ericsson.oss.management.lcm.presentation.exceptions.NotValidClusterNameException;
import com.ericsson.oss.management.lcm.presentation.services.clusterconnectioninfo.ClusterConnectionInfoService;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileServiceImpl;
import com.ericsson.oss.management.lcm.presentation.services.helper.command.CommandExecutorHelper;
import com.ericsson.oss.management.lcm.presentation.services.kubernetes.KubernetesService;
import com.ericsson.oss.management.lcm.repositories.ClusterConnectionInfoInstanceRepository;
import com.ericsson.oss.management.lcm.repositories.ClusterConnectionInfoRepository;
import com.ericsson.oss.management.lcm.utils.validator.ClusterConnectionInfoFileValidator;

import io.fabric8.kubernetes.api.model.Namespace;

@ActiveProfiles("test")
@SpringBootTest(classes = HelmfileExecutorApplication.class)
class ClusterConnectionInfoRequestCoordinatorServiceTest extends AbstractDbSetupTest {

    private static final String MOCK_MULTIPART_FILE_NAME = "TempFile";
    private static final String MOCK_MULTIPART_ORIGINAL_FILE_NAME = "TempFile.config";
    private static final String CLUSTER_CONNECTION_INFO_YAML = "clusterConnectionInfo.yaml";
    private static final String WORKLOAD_INSTANCE_ID = "wrkld_instance_id";
    private static final String WORKLOAD_INSTANCE_NAME = "workloadInstanceName";
    private static final String NAMESPACE = "namespace";
    private static final String CLUSTER = "cluster";
    private static final String CLUSTER_ID = "clusterId";
    private static final String CLUSTER_INSTANCE_ID = "clusterInstanceId";
    private static final String CRD_NAMESPACE = "crdNamespace";

    @MockBean
    private ClusterConnectionInfoRepository clusterConnectionInfoRepository;
    @MockBean
    private ClusterConnectionInfoInstanceRepository clusterConnectionInfoInstanceRepository;
    @MockBean
    private ClusterConnectionInfoFileValidator clusterConnectionInfoFileValidator;

    @Autowired
    private ClusterConnectionInfoRequestCoordinatorService clusterConnectionInfoRequestCoordinatorService;

    @Autowired
    private FileServiceImpl fileService;

    @MockBean
    private ClusterConnectionInfoService clusterConnectionInfoService;

    @MockBean
    private KubernetesService kubernetesService;

    @MockBean
    private CommandExecutorHelper commandExecutorHelper;

    @TempDir
    private Path folder;

    @BeforeEach
    void setUp() {
        setField(fileService, "rootDirectory", folder.toString());
    }

    @Test
    void shouldAcceptCorrectClusterConnectionInfo() throws IOException {
        when(clusterConnectionInfoService.create(any(), any())).thenReturn(new ClusterConnectionInfoDto());
        when(kubernetesService.getListNamespaces(any())).thenReturn(getNamespaceList());

        MultipartFile clusterConnectionInfoFile = getFile();
        ClusterConnectionInfoDto clusterConnectionInfoDto =
                clusterConnectionInfoRequestCoordinatorService.create(clusterConnectionInfoFile, CRD_NAMESPACE);

        assertThat(clusterConnectionInfoDto).isNotNull();
        assertThat(folder).isEmptyDirectory();
    }

    @Test
    void shouldAcceptCorrectClusterConnectionInfoWhenNoNamespaces() throws IOException {
        when(clusterConnectionInfoService.create(any(), any())).thenReturn(new ClusterConnectionInfoDto());
        when(kubernetesService.getListNamespaces(any())).thenReturn(getEmptyNamespaceList());

        MultipartFile clusterConnectionInfoFile = getFile();
        ClusterConnectionInfoDto clusterConnectionInfoDto =
                clusterConnectionInfoRequestCoordinatorService.create(clusterConnectionInfoFile, CRD_NAMESPACE);

        assertThat(clusterConnectionInfoDto).isNotNull();
        assertThat(folder).isEmptyDirectory();
    }

    @Test
    void shouldRejectInvalidClusterConnectionInfo() throws IOException {
        when(clusterConnectionInfoService.create(any(), any())).thenReturn(new ClusterConnectionInfoDto());
        when(kubernetesService.getListNamespaces(any())).thenThrow(ClusterConnectionInfoConnectionException.class);
        MultipartFile clusterConnectionInfoFile = getFile();

        assertThatThrownBy(() -> clusterConnectionInfoRequestCoordinatorService.create(clusterConnectionInfoFile, CRD_NAMESPACE))
                .isInstanceOf(ClusterConnectionInfoConnectionException.class)
                .hasMessageContaining("Could not connect to the cluster");

        assertThat(folder).isEmptyDirectory();
    }

    @Test
    void shouldRejectAlreadyExistingClusterConnectionInfo() throws IOException {
        when(clusterConnectionInfoRepository.findByName(anyString())).thenReturn(Optional.of(new ClusterConnectionInfo()));
        MultipartFile clusterConnectionInfoFile = getFile();
        assertThatThrownBy(() -> clusterConnectionInfoRequestCoordinatorService.create(clusterConnectionInfoFile, CRD_NAMESPACE))
                .isInstanceOf(NotUniqueClusterException.class)
                .hasMessageContaining("already exists");

        verify(commandExecutorHelper, times(0)).executeWithRetry(anyString(), anyInt());
    }

    @Test
    void shouldConnectWorkloadInstanceToClusterSuccessfully() {
        ClusterConnectionInfo clusterConnectionInfo = getClusterConnectionInfo();
        when(clusterConnectionInfoRepository.findByName(CLUSTER)).thenReturn(Optional.of(clusterConnectionInfo));

        clusterConnectionInfoRequestCoordinatorService.connectToClusterConnectionInfoIfPresent(getWorkloadInstance());

        assertThat(clusterConnectionInfo.getStatus()).isEqualTo(ConnectionInfoStatus.IN_USE);

        verify(clusterConnectionInfoInstanceRepository).save(any());
        verify(clusterConnectionInfoRepository).save(any());
    }

    @Test
    void shouldNotConnectWorkloadInstanceToClusterIfClusterNameNotExistSuccessfully() {
        WorkloadInstance workloadInstance = getWorkloadInstance();
        workloadInstance.setCluster(null);

        clusterConnectionInfoRequestCoordinatorService.connectToClusterConnectionInfoIfPresent(workloadInstance);

        verify(clusterConnectionInfoRepository, times(0)).findByName(any());
        verify(clusterConnectionInfoInstanceRepository, times(0)).save(any());
        verify(clusterConnectionInfoRepository, times(0)).save(any());
    }

    @Test
    void shouldFailWhenClusterNameIsNotExist() {
        WorkloadInstance workloadInstance = getWorkloadInstance();
        when(clusterConnectionInfoRepository.findByName(CLUSTER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clusterConnectionInfoRequestCoordinatorService.connectToClusterConnectionInfoIfPresent(workloadInstance))
                .isInstanceOf(NotValidClusterNameException.class)
                .hasMessageContaining("Cluster with a name " + CLUSTER + " not exist. Please check it or create create a new one.");

        verify(clusterConnectionInfoInstanceRepository, times(0)).save(any());
        verify(clusterConnectionInfoRepository, times(0)).save(any());
    }

    @Test
    void shouldDisconnectWorkloadInstanceAndDisableClusterSuccessfully() {
        ClusterConnectionInfo clusterConnectionInfo = getClusterConnectionInfo();
        clusterConnectionInfo.setStatus(ConnectionInfoStatus.IN_USE);
        WorkloadInstance workloadInstance = getWorkloadInstance();
        ClusterConnectionInfoInstance relation = getRelation(workloadInstance, clusterConnectionInfo);
        List<ClusterConnectionInfoInstance> clusterConnectionInfoInstances = new ArrayList<>();
        clusterConnectionInfoInstances.add(relation);
        clusterConnectionInfo.setClusterConnectionInfoInstances(clusterConnectionInfoInstances);
        when(clusterConnectionInfoRepository.findByName(CLUSTER)).thenReturn(Optional.of(clusterConnectionInfo));
        when(clusterConnectionInfoInstanceRepository.findClusterConnectionInfoInstanceByClusterConnectionInfoId(
                clusterConnectionInfo.getId())).thenReturn(Collections.emptyList());

        clusterConnectionInfoRequestCoordinatorService.disconnectFromClusterIfPresent(workloadInstance);

        assertThat(clusterConnectionInfo.getStatus()).isEqualTo(ConnectionInfoStatus.NOT_IN_USE);
        assertThat(clusterConnectionInfo.getClusterConnectionInfoInstances()).isEmpty();

        verify(clusterConnectionInfoInstanceRepository).delete(any());
        verify(clusterConnectionInfoRepository).save(any());
    }

    @Test
    void shouldFailedToDisconnectWorkloadInstanceIfClusterInstancesIsNull() {
        ClusterConnectionInfo clusterConnectionInfo = getClusterConnectionInfo();
        clusterConnectionInfo.setStatus(ConnectionInfoStatus.IN_USE);
        WorkloadInstance workloadInstance = getWorkloadInstance();

        clusterConnectionInfo.setClusterConnectionInfoInstances(null);
        when(clusterConnectionInfoRepository.findByName(CLUSTER)).thenReturn(Optional.of(clusterConnectionInfo));
        when(clusterConnectionInfoInstanceRepository.findClusterConnectionInfoInstanceByClusterConnectionInfoId(
                clusterConnectionInfo.getId())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> clusterConnectionInfoRequestCoordinatorService.disconnectFromClusterIfPresent(workloadInstance))
                .isInstanceOf(InternalRuntimeException.class)
                .hasMessageContaining("ClusterConnectionInfo instances list for workload instance");
    }

    @Test
    void shouldDisconnectWorkloadInstanceAndLeaveClusterEnabledSuccessfully() {
        ClusterConnectionInfo clusterConnectionInfo = getClusterConnectionInfo();
        clusterConnectionInfo.setStatus(ConnectionInfoStatus.IN_USE);
        WorkloadInstance workloadInstance = getWorkloadInstance();
        ClusterConnectionInfoInstance relation = getRelation(workloadInstance, clusterConnectionInfo);
        List<ClusterConnectionInfoInstance> clusterConnectionInfoInstances = new ArrayList<>();
        clusterConnectionInfoInstances.add(relation);
        clusterConnectionInfo.setClusterConnectionInfoInstances(clusterConnectionInfoInstances);
        ClusterConnectionInfoInstance anotherWorkloadInstanceOnCluster = getRelation(workloadInstance, clusterConnectionInfo);
        anotherWorkloadInstanceOnCluster.setId("another_id");
        when(clusterConnectionInfoRepository.findByName(CLUSTER)).thenReturn(Optional.of(clusterConnectionInfo));
        when(clusterConnectionInfoInstanceRepository.findClusterConnectionInfoInstanceByClusterConnectionInfoId(
                clusterConnectionInfo.getId())).thenReturn(Collections.singletonList(anotherWorkloadInstanceOnCluster));

        clusterConnectionInfoRequestCoordinatorService.disconnectFromClusterIfPresent(workloadInstance);

        assertThat(clusterConnectionInfo.getStatus()).isEqualTo(ConnectionInfoStatus.IN_USE);

        verify(clusterConnectionInfoInstanceRepository).delete(any());
        verify(clusterConnectionInfoRepository, times(0)).save(any());
    }

    private WorkloadInstance getWorkloadInstance() {
        return WorkloadInstance
                .builder()
                .workloadInstanceId(WORKLOAD_INSTANCE_ID)
                .workloadInstanceName(WORKLOAD_INSTANCE_NAME)
                .namespace(NAMESPACE)
                .cluster(CLUSTER)
                .build();
    }

    private List<Namespace> getEmptyNamespaceList() {
        return new ArrayList<>();
    }

    private List<Namespace> getNamespaceList() {
        return List.of(new Namespace());
    }

    private ClusterConnectionInfo getClusterConnectionInfo() {
        return ClusterConnectionInfo
                .builder()
                .id(CLUSTER_ID)
                .name(CLUSTER)
                .status(ConnectionInfoStatus.NOT_IN_USE)
                .build();
    }

    private ClusterConnectionInfoInstance getRelation(WorkloadInstance workloadInstance, ClusterConnectionInfo clusterConnectionInfo) {
        return ClusterConnectionInfoInstance
                .builder()
                .id(CLUSTER_INSTANCE_ID)
                .clusterConnectionInfo(clusterConnectionInfo)
                .workloadInstance(workloadInstance)
                .build();
    }

    private MockMultipartFile getFile() throws IOException {
        File file = new ClassPathResource(CLUSTER_CONNECTION_INFO_YAML).getFile();
        return new MockMultipartFile(MOCK_MULTIPART_FILE_NAME, MOCK_MULTIPART_ORIGINAL_FILE_NAME, CONTENT_TYPE, new FileInputStream(file));
    }
}
