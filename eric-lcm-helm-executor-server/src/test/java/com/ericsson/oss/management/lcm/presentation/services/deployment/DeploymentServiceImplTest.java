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

package com.ericsson.oss.management.lcm.presentation.services.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.management.lcm.api.model.DeploymentStateInfoDTO;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceHelmSourceUrl;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileServiceImpl;
import com.ericsson.oss.management.lcm.presentation.services.kubernetes.KubernetesService;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandExecutor;

@ActiveProfiles("test")
@SpringBootTest(classes = {DeploymentServiceImpl.class, FileServiceImpl.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DeploymentServiceImplTest {

    private static final String VALID_CLUSTER_CONNECTION_INFO_PATH = "clusterConnectionInfo.yaml";
    private static final String CLUSTER_NAME = "hahn117";
    private static final String MOCK_MULTIPART_FILE_NAME = "TempFile";
    private static final String TEST_NAMESPACE = "namespace";
    private static final String TEST_INSTANCE_NAME = "testInstance";
    private static final String POD_STATUS_RUNNING = "Running";
    private static final String POD_STATUS_PENDING = "Pending";
    private static final String POD_STATUS_FAILED = "Failed";

    @Autowired
    private DeploymentServiceImpl deploymentService;
    @Autowired
    private FileServiceImpl fileService;
    @MockBean
    private KubernetesService kubernetesService;
    @MockBean
    private CommandExecutor commandExecutor;

    @Test
    void shouldReturnPodsStateDtoWithDifferentStates() throws IOException {
        //Init
        Path directory = fileService.createDirectory();
        Path kubeConfigInfoPath = storeFile(VALID_CLUSTER_CONNECTION_INFO_PATH, directory);
        WorkloadInstanceHelmSourceUrl dto = createWorkloadInstanceHelmSourceUrlDto();
        DeploymentStateInfoDTO expected = createDeploymentStateInfoDTO();
        when(kubernetesService.getPodsStatusInfo(anyString(), any())).thenReturn(getPods());

        //Test method
        DeploymentStateInfoDTO result = deploymentService.getCurrentDeploymentState(dto, kubeConfigInfoPath);

        //Verify
        assertThat(result).isEqualTo(expected);

        fileService.deleteDirectoryIfExists(directory);
    }

    private Path storeFile(String name, Path directory) throws IOException {
        File file = new ClassPathResource(name).getFile();
        MockMultipartFile multipartFile = new MockMultipartFile(MOCK_MULTIPART_FILE_NAME, new FileInputStream(file));
        return fileService.storeFileIn(directory, multipartFile, name);
    }

    private WorkloadInstanceHelmSourceUrl createWorkloadInstanceHelmSourceUrlDto() {
        return WorkloadInstanceHelmSourceUrl
                .builder()
                .workloadInstanceName(TEST_INSTANCE_NAME)
                .namespace(TEST_NAMESPACE)
                .build();
    }

    private DeploymentStateInfoDTO createDeploymentStateInfoDTO() {
        return new DeploymentStateInfoDTO()
                .namespace(TEST_NAMESPACE)
                .workloadInstanceName(TEST_INSTANCE_NAME)
                .clusterName(CLUSTER_NAME)
                .pods(getPods());
    }

    private Map<String, String> getPods() {
        return Map.of("Pod_1", POD_STATUS_RUNNING,
                      "Pod_2", POD_STATUS_PENDING,
                      "Pod_3", POD_STATUS_RUNNING,
                      "Pod_4", POD_STATUS_FAILED);
    }

}
