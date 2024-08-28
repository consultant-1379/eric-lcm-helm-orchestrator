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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
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
import com.ericsson.oss.management.lcm.TestUtils;
import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.HelmSourceType;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.OperationState;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;
import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import com.ericsson.oss.management.lcm.presentation.services.clusterconnectioninfo.ClusterConnectionInfoService;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.presentation.services.helmsource.HelmSourceService;
import com.ericsson.oss.management.lcm.presentation.services.helper.file.StoreFileHelper;
import com.ericsson.oss.management.lcm.repositories.WorkloadInstanceRepository;

@ActiveProfiles("test")
@SpringBootTest(classes = HelmfileExecutorApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TerminateServiceImplTest extends AbstractDbSetupTest {

    @Autowired
    private TerminateServiceImpl terminateService;
    @MockBean
    private HelmSourceService helmSourceService;
    @MockBean
    private WorkloadInstanceRepository repository;
    @MockBean
    private FileService fileService;
    @MockBean
    private StoreFileHelper storeFileHelper;
    @MockBean
    private ClusterConnectionInfoService clusterConnectionInfoService;
    @Mock
    private MultipartFile clusterConnectionInfo;
    @Mock
    private Path kubeConfigPath;
    @Mock
    private Path helmPath;
    @Mock
    private Path valuesPath;
    @Mock
    private FilePathDetails pathData;

    private static final String HELMSOURCE_ID = "some_id";
    private static final String OPERATION_ID = "some_id";
    private static final String WORKLOAD_INSTANCE_ID = "wrkld_instance_id";
    private static final String WORKLOAD_INSTANCE_NAME = "workloadInstanceName";
    private static final String NAMESPACE = "namespace";
    private static final String CLUSTER = "cluster";
    private static final String ADDITIONAL_PARAMETERS = "{\"key\": \"value\"}";
    private static final String GLOBAL_CRD_NAMESPACE_VALUE = "eric-crd-ns";
    private static final String DIRECTORY_PATH = "helmfile-test";
    private static final Integer WORKLOAD_INSTANCE_VERSION = 1;
    private static final String CLUSTER_IDENT = "hahn117 https://mocha.rnd.gic.ericsson.se/k8s/clusters/c-mdw5r";

    @BeforeEach
    void setup() throws URISyntaxException {
        Path directory = TestUtils.getResource(DIRECTORY_PATH);
        when(fileService.createDirectory()).thenReturn(directory);
        when(fileService.createFile(eq(directory), any(), anyString())).thenReturn(helmPath);
        when(storeFileHelper.getValuesPath(any(), any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(pathData);
        when(storeFileHelper.getKubeConfigPath(eq(directory), any(), any())).thenReturn(kubeConfigPath);
    }

    @Test
    void shouldTerminateWorkloadInstanceSuccessfullyWithoutCluster() {
        WorkloadInstance workloadInstance = basicWorkloadInstance(null);
        HelmSource helmSource = basicHelmfile(workloadInstance);
        workloadInstance.setHelmSources(Collections.singletonList(helmSource));
        Operation operation = basicTerminateOperation(workloadInstance);
        when(helmSourceService.get(workloadInstance)).thenReturn(helmSource);
        when(helmSourceService.destroyHelmSource(eq(helmSource), anyInt(), any(), anyBoolean())).thenReturn(operation);
        when(repository.save(workloadInstance)).thenReturn(workloadInstance);
        WorkloadInstance result = terminateService.terminate(workloadInstance,
                                                             null, false);

        assertThat(result).isNotNull();
        verify(helmSourceService).destroyHelmSource(eq(helmSource), anyInt(), any(), anyBoolean());
        verify(helmSourceService).extractArchiveForHelmfile(any(), any());
        verify(repository).save(any());
        verify(clusterConnectionInfoService).verifyClusterIdentifier(any(), any());
    }

    @Test
    void shouldTerminateWorkloadInstanceSuccessfullyWithCluster() {
        WorkloadInstance workloadInstance = basicWorkloadInstance(CLUSTER);
        HelmSource helmSource = basicHelmfile(workloadInstance);
        workloadInstance.setHelmSources(Collections.singletonList(helmSource));
        Operation operation = basicTerminateOperation(workloadInstance);
        when(helmSourceService.get(workloadInstance)).thenReturn(helmSource);
        when(helmSourceService.destroyHelmSource(eq(helmSource), anyInt(), any(), anyBoolean())).thenReturn(operation);
        when(repository.save(workloadInstance)).thenReturn(workloadInstance);
        WorkloadInstance result = terminateService.terminate(workloadInstance,
                                                             clusterConnectionInfo, false);

        assertThat(result).isNotNull();
        verify(helmSourceService).destroyHelmSource(eq(helmSource), anyInt(), any(), anyBoolean());
        verify(helmSourceService).extractArchiveForHelmfile(any(), any());
        verify(repository).save(any());
        verify(clusterConnectionInfoService).verifyClusterIdentifier(any(), any());
    }

    @Test
    void shouldFailWhenLatestHelmfileNotFound() {
        WorkloadInstance workloadInstance = basicWorkloadInstance(CLUSTER);
        HelmSource helmSource = basicHelmfile(workloadInstance);
        workloadInstance.setHelmSources(Collections.emptyList());
        when(helmSourceService.get(workloadInstance)).thenThrow(ResourceNotFoundException.class);

        assertThatThrownBy(() -> terminateService.terminate(workloadInstance, clusterConnectionInfo, false))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(helmSourceService, never()).destroyHelmSource(eq(helmSource), anyInt(), any(), anyBoolean());
        verify(helmSourceService, never()).extractArchiveForHelmfile(any(), any());
        verify(repository, times(0)).save(any());
    }

    private WorkloadInstance basicWorkloadInstance(final String cluster) {
        return WorkloadInstance.builder()
                .workloadInstanceName(WORKLOAD_INSTANCE_NAME)
                .workloadInstanceId(WORKLOAD_INSTANCE_ID)
                .latestOperationId(OPERATION_ID)
                .version(WORKLOAD_INSTANCE_VERSION)
                .cluster(cluster)
                .namespace(NAMESPACE)
                .crdNamespace(GLOBAL_CRD_NAMESPACE_VALUE)
                .additionalParameters(ADDITIONAL_PARAMETERS)
                .clusterIdentifier(CLUSTER_IDENT)
                .build();
    }

    private HelmSource basicHelmfile(WorkloadInstance workloadInstance) {
        return HelmSource.builder()
                .content(new byte[]{})
                .workloadInstance(workloadInstance)
                .helmSourceType(HelmSourceType.HELMFILE)
                .created(LocalDateTime.now())
                .id(HELMSOURCE_ID)
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

}