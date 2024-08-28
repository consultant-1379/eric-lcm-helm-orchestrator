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

package com.ericsson.oss.management.lcm.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_MINUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.KUBE_CONFIG;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.VALUES_YAML;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.ericsson.oss.management.lcm.presentation.services.kubernetes.KubernetesService;
import org.awaitility.core.ConditionTimeoutException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceDto;
import com.ericsson.oss.management.lcm.model.entity.ClusterConnectionInfo;
import com.ericsson.oss.management.lcm.model.entity.ConnectionInfoStatus;
import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.HelmSourceType;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.OperationState;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;
import com.ericsson.oss.management.lcm.presentation.services.clusterconnectioninfo.ClusterConnectionInfoService;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileServiceImpl;
import com.ericsson.oss.management.lcm.presentation.services.helper.command.CommandExecutorHelperImpl;
import com.ericsson.oss.management.lcm.repositories.ClusterConnectionInfoInstanceRepository;
import com.ericsson.oss.management.lcm.repositories.ClusterConnectionInfoRepository;
import com.ericsson.oss.management.lcm.repositories.HelmSourceRepository;
import com.ericsson.oss.management.lcm.repositories.OperationRepository;
import com.ericsson.oss.management.lcm.repositories.WorkloadInstanceRepository;
import com.ericsson.oss.management.lcm.repositories.WorkloadInstanceVersionRepository;
import com.ericsson.oss.management.lcm.utils.TestingFileUtils;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
class HelmfileBuilderControllerTest extends AbstractDbSetupTest {

    private static final String WORKLOAD_INSTANCE_POST_WITH_CHARTS_JSON = "helmfileBuilder/helmfileBuilderPostWithCharts.json";
    private static final String WORKLOAD_INSTANCE_PUT_WITH_CHARTS_JSON = "helmfileBuilder/helmfileBuilderPutWithCharts.json";
    private static final String WORKLOAD_INSTANCE_POST_REQUEST_DTO = "workloadInstanceWithChartsRequestDto";
    private static final String WORKLOAD_INSTANCE_PUT_REQUEST_DTO = "workloadInstanceWithChartsPutRequestDto";
    private static final String GLOBAL_VALUES = "globalValues";
    private static final String CLUSTER_CONNECTION_INFO_YAML = "clusterConnectionInfo.yaml";
    private static final String CLUSTER_CONNECTION_INFO = "clusterConnectionInfo";
    private static final String VALID_HELMFILE_TGZ = "helmfile-test-1.2.3+7.tgz";
    private static final String CLUSTER_URL = "test_url";
    private static final String CRD_NAMESPACE = "crdNamespace";
    private static final String WORKLOAD_INSTANCE_NAME = "workloadInstanceName";
    private static final String NAMESPACE = "namespace";
    private static final String ADDITIONAL_PARAMETERS = "{\"key\": \"value\"}";
    private static final String CLUSTER_IDENT = "hahn117 https://mocha.rnd.gic.ericsson.se/k8s/clusters/c-mdw5r";
    private static final Path CLUSTER_CONNECTION_INFO_PATH = Path.of("src/test/resources/" + CLUSTER_CONNECTION_INFO_YAML);

    private static byte[] helmfile;
    private static String jsonWithCluster;
    private static String updateJson;
    private static String values;
    private static String connectionInfo;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FileServiceImpl fileService;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private WorkloadInstanceRepository workloadInstanceRepository;
    @Autowired
    private WorkloadInstanceVersionRepository workloadInstanceVersionRepository;
    @Autowired
    private HelmSourceRepository helmSourceRepository;
    @Autowired
    private OperationRepository operationRepository;
    @Autowired
    private ClusterConnectionInfoRepository clusterConnectionInfoRepository;
    @Autowired
    private ClusterConnectionInfoInstanceRepository clusterConnectionInfoInstanceRepository;
    @Autowired
    private ClusterConnectionInfoService clusterConnectionInfoService;
    @MockBean
    private CommandExecutorHelperImpl commandExecutor;
    @MockBean
    private KubernetesService kubernetesService;
    @TempDir
    private Path folder;

    @BeforeEach
    private void setUp() {
        String rootDirectory = folder.toString();
        setField(fileService, "rootDirectory", rootDirectory);

        CommandResponse res = new CommandResponse();
        res.setExitCode(0);
        when(commandExecutor.executeWithRetry(anyString(), anyInt())).thenReturn(res);
        when(commandExecutor.execute(anyString(), anyInt())).thenReturn(res);
        doNothing().when(kubernetesService).createSecretAndNamespaceIfRequired(any(), any(), any());
        doNothing().when(kubernetesService).deleteSecret(anyString(), anyString(), anyString());
        clusterConnectionInfoService.create(CLUSTER_CONNECTION_INFO_PATH, CRD_NAMESPACE);
    }

    @BeforeEach
    private void filesSetup() throws Exception {
        helmfile = Files.readAllBytes(TestingFileUtils.getResource(VALID_HELMFILE_TGZ));
        jsonWithCluster = TestingFileUtils.readDataFromFile(WORKLOAD_INSTANCE_POST_WITH_CHARTS_JSON);
        updateJson = TestingFileUtils.readDataFromFile(WORKLOAD_INSTANCE_PUT_WITH_CHARTS_JSON);
        values = TestingFileUtils.readDataFromFile(VALUES_YAML);
        connectionInfo = TestingFileUtils.readDataFromFile(CLUSTER_CONNECTION_INFO_YAML);
    }

    @AfterEach
    private void cleanDb() {
        clusterConnectionInfoInstanceRepository.deleteAll();
        clusterConnectionInfoRepository.deleteAll();
        helmSourceRepository.deleteAll();
        operationRepository.deleteAll();

        try {
            waitUntilVersionIsCreated();
        } catch (ConditionTimeoutException e) {
            e.printStackTrace();
        }

        workloadInstanceVersionRepository.deleteAll();
        workloadInstanceRepository.deleteAll();
    }

    @Test
    void positivePostWithAllRequestBodyParams() throws Exception {
        ClusterConnectionInfo clusterConnectionInfo = getDisabledClusterConnectionInfo();
        clusterConnectionInfoRepository.save(clusterConnectionInfo);

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_POST_REQUEST_DTO,
                                                           WORKLOAD_INSTANCE_POST_WITH_CHARTS_JSON,
                                                           MediaType.APPLICATION_JSON_VALUE, jsonWithCluster.getBytes());

        // values part
        MockMultipartFile valuesPart = new MockMultipartFile(GLOBAL_VALUES, VALUES_YAML, MediaType.TEXT_PLAIN_VALUE,
                                                             values.getBytes());

        // kube config part
        MockMultipartFile configPart = new MockMultipartFile(CLUSTER_CONNECTION_INFO, KUBE_CONFIG, MediaType.TEXT_PLAIN_VALUE,
                                                             connectionInfo.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart("/cnwlcm/v1/helmfile_builder/workload_instances")
                                 .file(jsonPart)
                                 .file(valuesPart)
                                 .file(configPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, ACCEPTED);

        MockHttpServletResponse resultResponse = result.getResponse();

        assertThat(resultResponse.getHeader("Location")).isNotEmpty();
        final String[] splitOperationUrl = Objects.requireNonNull(resultResponse.getHeader("Location")).split("/");

        String operationIdFromHeader = splitOperationUrl[splitOperationUrl.length - 1];
        WorkloadInstanceDto response = mapper.readValue(resultResponse.getContentAsString(), WorkloadInstanceDto.class);
        assertThat(response.getWorkloadInstanceName()).isEqualTo("successfulpost");

        Optional<WorkloadInstance> workloadInstance = Objects.requireNonNull(workloadInstanceRepository.findById(response.getWorkloadInstanceId()));
        assertThat(workloadInstance).isNotNull();

        WorkloadInstance instance = workloadInstance.get();

        String latestOperationId = instance.getLatestOperationId();

        assertThat(latestOperationId).isNotEmpty();
        assertThat(latestOperationId).isEqualTo(operationIdFromHeader);

        Optional<Operation> operationRepositoryById = operationRepository.findById(latestOperationId);

        assertThat(operationRepositoryById).isNotNull();
        Operation operation = operationRepositoryById.get();

        assertThat(operation.getType()).isEqualTo(OperationType.INSTANTIATE);
        assertThat(operation.getState()).isIn(OperationState.PROCESSING, OperationState.COMPLETED);
    }

    @Test
    void positivePutRequestWithAllParts() throws Exception {
        WorkloadInstance workloadInstance = getExistingWorkloadInstance();

        Operation instantiateOperation = getOperation(workloadInstance, OperationType.INSTANTIATE);
        instantiateOperation = operationRepository.save(instantiateOperation);

        workloadInstance.setLatestOperationId(instantiateOperation.getId());

        WorkloadInstanceVersion workloadInstanceVersion = getWorkloadInstanceVersion(workloadInstance, 1, "1.2.3-4",
                                                                                     "test");

        workloadInstance.setWorkloadInstanceVersions(Collections.singletonList(workloadInstanceVersion));
        workloadInstanceRepository.save(workloadInstance);

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_PUT_REQUEST_DTO,
                                                           WORKLOAD_INSTANCE_PUT_WITH_CHARTS_JSON,
                                                           MediaType.APPLICATION_JSON_VALUE, updateJson.getBytes());

        // values part
        MockMultipartFile valuesPart = new MockMultipartFile(GLOBAL_VALUES, VALUES_YAML, MediaType.TEXT_PLAIN_VALUE,
                                                             values.getBytes());

        // kube config part
        MockMultipartFile configPart = new MockMultipartFile(CLUSTER_CONNECTION_INFO, KUBE_CONFIG, MediaType.TEXT_PLAIN_VALUE,
                                                             connectionInfo.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart(String.format("/cnwlcm/v1/helmfile_builder/workload_instances/%s",
                                                          workloadInstance.getWorkloadInstanceId()))
                                 .file(jsonPart)
                                 .file(valuesPart)
                                 .file(configPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA)
                                 .with(request -> {
                                     request.setMethod("PUT");
                                     return request;
                                 }))
                .andReturn();

        assertThatResponseCodeIs(result, ACCEPTED);

        MockHttpServletResponse resultResponse = result.getResponse();

        assertThat(resultResponse
                           .getHeader("Location")).isNotEmpty();
        String[] splitOperationUrl = Objects.requireNonNull(resultResponse.getHeader("Location")).split("/");

        String operationIdFromHeader = splitOperationUrl[splitOperationUrl.length - 1];
        WorkloadInstanceDto response = mapper.readValue(resultResponse.getContentAsString(), WorkloadInstanceDto.class);
        Optional<WorkloadInstance> workloadInstanceRepositoryById = workloadInstanceRepository.findById(response.getWorkloadInstanceId());

        assertThat(workloadInstanceRepositoryById).isNotNull();
        WorkloadInstance instance = workloadInstanceRepositoryById.get();

        String latestOperationId = instance.getLatestOperationId();

        assertThat(latestOperationId).isNotEmpty();
        assertThat(latestOperationId).isEqualTo(operationIdFromHeader);

        Optional<Operation> operationRepositoryById = operationRepository.findById(latestOperationId);
        assertThat(operationRepositoryById).isNotNull();

        Operation operation = operationRepositoryById.get();

        assertThat(operation.getType()).isEqualTo(OperationType.UPDATE);
        assertThat(operation.getState()).isIn(OperationState.PROCESSING, OperationState.COMPLETED);
    }

    @NotNull
    private WorkloadInstanceVersion getWorkloadInstanceVersion(WorkloadInstance workloadInstance, int version, String helmSourceVersion,
                                                               String valuesVersion) {
        WorkloadInstanceVersion workloadInstanceVersion = new WorkloadInstanceVersion();
        workloadInstanceVersion.setWorkloadInstance(workloadInstance);
        workloadInstanceVersion.setVersion(version);
        workloadInstanceVersion.setHelmSourceVersion(helmSourceVersion);
        workloadInstanceVersion.setValuesVersion(valuesVersion);
        workloadInstanceVersion = workloadInstanceVersionRepository.save(workloadInstanceVersion);
        return workloadInstanceVersion;
    }

    private ClusterConnectionInfo getDisabledClusterConnectionInfo() {
        return ClusterConnectionInfo.builder()
                .status(ConnectionInfoStatus.NOT_IN_USE)
                .name(CLUSTER_CONNECTION_INFO)
                .url(CLUSTER_URL)
                .content("kube: config".getBytes())
                .build();
    }

    private void waitUntilVersionIsCreated() {
        if (instanceRepositoryIsNotEmpty()) {
            await().atMost(ONE_MINUTE).until(() -> versionRepositorySize() > 0);
        }
    }

    private Integer versionRepositorySize() {
        List<WorkloadInstanceVersion> versionRepositoryList = workloadInstanceVersionRepository.findAll();
        return versionRepositoryList.size();
    }

    private boolean instanceRepositoryIsNotEmpty() {
        List<WorkloadInstance> workloadInstanceRepositoryList = workloadInstanceRepository.findAll();
        return workloadInstanceRepositoryList.size() > 0;
    }

    private WorkloadInstance getExistingWorkloadInstance() {
        WorkloadInstance existingInstance = getWorkloadInstance();
        existingInstance.setCluster(null);
        existingInstance.setVersion(1);
        existingInstance = workloadInstanceRepository.save(existingInstance);
        return existingInstance;
    }

    private WorkloadInstance getWorkloadInstance() {
        return WorkloadInstance.builder()
                .workloadInstanceName(WORKLOAD_INSTANCE_NAME)
                .namespace(NAMESPACE)
                .cluster(CLUSTER_CONNECTION_INFO)
                .clusterIdentifier(CLUSTER_IDENT)
                .additionalParameters(ADDITIONAL_PARAMETERS)
                .build();
    }

    private HelmSource getHelmSource(WorkloadInstance workloadInstance, String version) {
        return HelmSource.builder()
                .content(helmfile)
                .helmSourceType(HelmSourceType.HELMFILE)
                .helmSourceVersion(version)
                .workloadInstance(workloadInstance)
                .created(LocalDateTime.now())
                .build();
    }

    private Operation getOperation(WorkloadInstance workloadInstance, OperationType type) {
        return Operation.builder()
                .workloadInstance(workloadInstance)
                .type(type)
                .startTime(LocalDateTime.now())
                .state(OperationState.COMPLETED)
                .build();
    }

    private void assertThatResponseCodeIs(MvcResult result, HttpStatus httpStatus) {
        assertThat(result).isNotNull();
        assertThat(result.getResponse()).isNotNull();
        assertThat(result
                .getResponse()
                .getStatus()).isEqualTo(httpStatus.value());
    }
}