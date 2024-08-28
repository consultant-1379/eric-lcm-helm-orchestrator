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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.nio.file.Path;

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
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;
import com.ericsson.oss.management.lcm.presentation.services.clusterconnectioninfo.ClusterConnectionInfoService;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.presentation.services.helmsource.HelmSourceService;
import com.ericsson.oss.management.lcm.presentation.services.helper.docker.DockerRegistrySecretHelper;
import com.ericsson.oss.management.lcm.presentation.services.helper.file.StoreFileHelper;
import com.ericsson.oss.management.lcm.presentation.services.values.ValuesService;
import com.ericsson.oss.management.lcm.presentation.services.workloadinstance.WorkloadInstanceService;

@ActiveProfiles("test")
@SpringBootTest(classes = HelmfileExecutorApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RollbackServiceImplTest extends AbstractDbSetupTest {

    @Autowired
    private RollbackServiceImpl rollbackService;
    @MockBean
    private HelmSourceService helmSourceService;
    @MockBean
    private FileService fileService;
    @MockBean
    private StoreFileHelper storeFileHelper;
    @MockBean
    private DockerRegistrySecretHelper dockerRegistrySecretHelper;
    @MockBean
    private ValuesService valuesService;
    @MockBean
    private WorkloadInstanceService workloadInstanceService;
    @MockBean
    private ClusterConnectionInfoService clusterConnectionInfoService;
    @Mock
    private MultipartFile helmSourceFile;
    @Mock
    private MultipartFile clusterConnectionInfo;
    @Mock
    private Path helmPath;
    @Mock
    private Path valuesPath;
    @Mock
    private Path kubeConfigPath;

    private static final String OPERATION_ID = "some_id";
    private static final String WORKLOAD_INSTANCE_ID = "wrkld_instance_id";
    private static final String WORKLOAD_INSTANCE_NAME = "workloadInstanceName";
    private static final String NAMESPACE = "namespace";
    private static final String ADDITIONAL_PARAMETERS = "{\"key\": \"value\"}";
    private static final String GLOBAL_CRD_NAMESPACE_VALUE = "eric-crd-ns";
    private static final String DIRECTORY_PATH = "helmfile-test";
    private static final Integer WORKLOAD_INSTANCE_VERSION = 1;
    private static final String CLUSTER_IDENT = "hahn117 https://mocha.rnd.gic.ericsson.se/k8s/clusters/c-mdw5r";
    private static final String VALUES_VERSION = "0e35ed30-d438-4b07-a82b-cab447424d30";
    private static final String HELM_SOURCE_VERSION = "1.2.3-4";
    private static final String HELM_SOURCE_FILE_NAME = "helmsource-1.2.3-4.tgz";
    private static final String ROLLBACK_HELMSOURCE_ID = "rollback_id";

    @BeforeEach
    public void setup() throws URISyntaxException {
        Path directory = TestUtils.getResource(DIRECTORY_PATH);
        when(fileService.createDirectory()).thenReturn(directory);
        when(fileService.createFile(eq(directory), any(), anyString())).thenReturn(helmPath);
        when(storeFileHelper.getKubeConfigPath(eq(directory), any(), any())).thenReturn(kubeConfigPath);
        when(valuesService.retrieveByVersion(anyString(), any(), eq(directory))).thenReturn(valuesPath);
    }

    @Test
    void shouldRollbackSuccessfully() {
        //Init
        WorkloadInstance instance = basicWorkloadInstance();
        WorkloadInstanceVersion workloadInstanceVersion = getVersion(instance);
        when(helmSourceFile.getOriginalFilename()).thenReturn(HELM_SOURCE_FILE_NAME);
        when(helmSourceService
                     .getByWorkloadInstanceAndVersion(instance, HELM_SOURCE_VERSION)).thenReturn(getHelmSourceForRollback());
        when(helmSourceService.executeHelmSource(any(), anyInt(), any(), any(), any())).thenReturn(getOperation());

        //Test method
        WorkloadInstance result = rollbackService.rollback(instance, workloadInstanceVersion, clusterConnectionInfo);

        //Verify
        assertThat(result.getWorkloadInstanceId()).isEqualTo(WORKLOAD_INSTANCE_ID);
        assertThat(result.getWorkloadInstanceName()).isEqualTo(WORKLOAD_INSTANCE_NAME);
        assertThat(result.getNamespace()).isEqualTo(NAMESPACE);
        assertThat(result.getVersion()).isEqualTo(WORKLOAD_INSTANCE_VERSION);
        assertThat(result.getLatestOperationId()).isEqualTo(OPERATION_ID);
        verify(helmSourceService).extractArchiveForHelmfile(any(), any());
        verify(dockerRegistrySecretHelper).createSecret(any(), any());
        verify(workloadInstanceService).update(any());
        verify(clusterConnectionInfoService).verifyClusterIdentifier(any(), any());
    }

    private WorkloadInstance basicWorkloadInstance() {
        return WorkloadInstance.builder()
                .workloadInstanceName(WORKLOAD_INSTANCE_NAME)
                .workloadInstanceId(WORKLOAD_INSTANCE_ID)
                .latestOperationId(OPERATION_ID)
                .version(WORKLOAD_INSTANCE_VERSION)
                .namespace(NAMESPACE)
                .crdNamespace(GLOBAL_CRD_NAMESPACE_VALUE)
                .additionalParameters(ADDITIONAL_PARAMETERS)
                .clusterIdentifier(CLUSTER_IDENT)
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

    private HelmSource getHelmSourceForRollback() {
        return HelmSource.builder()
                .id(ROLLBACK_HELMSOURCE_ID)
                .helmSourceType(HelmSourceType.HELMFILE)
                .helmSourceVersion(HELM_SOURCE_VERSION)
                .build();
    }

    private Operation getOperation() {
        return Operation.builder()
                .id(OPERATION_ID)
                .build();
    }

}