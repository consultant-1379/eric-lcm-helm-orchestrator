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

package com.ericsson.oss.management.lcm.presentation.services.helper.docker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;

import com.ericsson.oss.management.lcm.presentation.services.kubernetes.KubernetesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;

@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {"container-registry.url=armdocker.rnd.ericsson.se", "container-registry.username=test-name",
        "container-registry.password=test-password", "operation.timeout=5"})
class DockerRegistrySecretHelperImplTest extends AbstractDbSetupTest {

    private static final String NAMESPACE = "namespace";
    private static final String WORKLOAD_INSTANCE_ID = "wrkld_instance_id";
    private static final String WORKLOAD_INSTANCE_NAME = "workloadInstanceName";
    private static final String ADDITIONAL_PARAMETERS = "{\"key\": \"value\"}";
    private static final String DEFAULT_GLOBAL_CRD_NAMESPACE_VALUE = "eric-crd-ns";

    @Autowired
    private DockerRegistrySecretHelper dockerRegistrySecretHelper;

    @MockBean
    private KubernetesService kubernetesService;

    private static Path kubeConfigPath;
    private byte[] kubeConfig = new byte[1];

    @Test
    void shouldCreateSecretSuccessfully() {
        WorkloadInstance instance = basicWorkloadInstance();
        dockerRegistrySecretHelper.createSecret(instance, kubeConfigPath);

        verify(kubernetesService).createSecretAndNamespaceIfRequired(any(), any(), any());
    }

    @Test
    void shouldDeleteSecret() {
        WorkloadInstance instance = basicWorkloadInstance();
        dockerRegistrySecretHelper.deleteSecret(instance, kubeConfig);

        verify(kubernetesService).deleteSecret(anyString(), anyString(), anyString());
    }

    @Test
    void shouldDeleteSecretWhenKubeConfigNull() {
        WorkloadInstance instance = basicWorkloadInstance();
        dockerRegistrySecretHelper.deleteSecret(instance, null);

        verify(kubernetesService).deleteSecret(eq(null), anyString(), anyString());
    }

    private WorkloadInstance basicWorkloadInstance() {
        return WorkloadInstance.builder()
                .workloadInstanceName(WORKLOAD_INSTANCE_NAME)
                .workloadInstanceId(WORKLOAD_INSTANCE_ID)
                .cluster(null)
                .namespace(NAMESPACE)
                .crdNamespace(DEFAULT_GLOBAL_CRD_NAMESPACE_VALUE)
                .additionalParameters(ADDITIONAL_PARAMETERS)
                .build();
    }

}
