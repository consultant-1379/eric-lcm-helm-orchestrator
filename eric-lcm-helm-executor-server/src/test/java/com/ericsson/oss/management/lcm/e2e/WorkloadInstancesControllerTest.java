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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import static com.ericsson.oss.management.lcm.constants.ClusterConnectionInfoConstants.CONTENT_TYPE;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.KUBE_CONFIG;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.VALUES_YAML;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import com.ericsson.oss.management.lcm.presentation.services.kubernetes.KubernetesService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceDto;
import com.ericsson.oss.management.lcm.model.entity.ClusterConnectionInfo;
import com.ericsson.oss.management.lcm.model.entity.ClusterConnectionInfoInstance;
import com.ericsson.oss.management.lcm.model.entity.ConnectionInfoStatus;
import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.HelmSourceType;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.OperationState;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.Values;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;
import com.ericsson.oss.management.lcm.presentation.services.clusterconnectioninfo.ClusterConnectionInfoService;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileServiceImpl;
import com.ericsson.oss.management.lcm.presentation.services.helmsource.HelmSourceService;
import com.ericsson.oss.management.lcm.presentation.services.helper.command.CommandExecutorHelperImpl;
import com.ericsson.oss.management.lcm.presentation.services.release.ReleaseService;
import com.ericsson.oss.management.lcm.presentation.services.secretsmanagement.SecretsManagement;
import com.ericsson.oss.management.lcm.repositories.ClusterConnectionInfoInstanceRepository;
import com.ericsson.oss.management.lcm.repositories.ClusterConnectionInfoRepository;
import com.ericsson.oss.management.lcm.repositories.HelmSourceRepository;
import com.ericsson.oss.management.lcm.repositories.OperationRepository;
import com.ericsson.oss.management.lcm.repositories.ValuesRepository;
import com.ericsson.oss.management.lcm.repositories.WorkloadInstanceRepository;
import com.ericsson.oss.management.lcm.repositories.WorkloadInstanceVersionRepository;
import com.ericsson.oss.management.lcm.utils.HttpClientUtils;
import com.ericsson.oss.management.lcm.utils.TestingFileUtils;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
@TestPropertySource(properties = {"gpg.secret.path=src/test/resources/security/", "encrypt.type=sops"})
class WorkloadInstancesControllerTest extends AbstractDbSetupTest {

    public static final String WORKLOAD_INSTANCE_POST_JSON = "workloadInstancePost/workloadInstancePost.json";
    public static final String WORKLOAD_INSTANCE_POST_WITH_URL_JSON = "workloadInstancePost/requestForHelmfileFetcher.json";
    public static final String WORKLOAD_INSTANCE_POST_JSON_EMPTY = "workloadInstancePost/workloadInstancePostEmpty.json";
    public static final String WORKLOAD_INSTANCE_POST_WITH_CLUSTER_JSON = "workloadInstancePost/workloadInstancePostWithCluster.json";
    public static final String INVALID_WORKLOAD_INSTANCE_POST_WITH_CLUSTER_JSON = "workloadInstancePost/invalidWorkloadInstancePostWithCluster.json";
    public static final String WORKLOAD_INSTANCE_PUT_JSON = "workloadInstancePut/workloadInstancePut.json";
    public static final String WORKLOAD_INSTANCE_ROLLBACK_JSON = "workloadInstanceRollback/workloadInstanceRollback.json";
    public static final String WORKLOAD_INSTANCE_ROLLBACK_JSON_WITH_CURRENT_VERSION = "workloadInstanceRollback"
            + "/workloadInstanceRollbackWithCurrentVersion.json";
    public static final String INSTANCE_ROLLBACK_WITHOUT_VERSION_JSON = "workloadInstanceRollback/workloadInstanceRollbackWithoutVersion.json";
    public static final String WORKLOAD_INSTANCE_OPERATION_POST_JSON = "workloadInstanceOperationPost/workloadInstanceOperationPost.json";
    public static final String INVALID_WORKLOAD_INSTANCE_OPERATION_POST_JSON = "workloadInstanceOperationPost/"
            + "workloadInstanceOperationPostInvalid.json";
    public static final String HELM_SOURCE = "helmSource";
    public static final String WORKLOAD_INSTANCE_POST_REQUEST_DTO = "workloadInstancePostRequestDto";
    public static final String WORKLOAD_INSTANCE_POST_URL_REQUEST_DTO = "workloadInstanceWithURLRequestDto";
    public static final String WORKLOAD_INSTANCE_PUT_REQUEST_DTO = "workloadInstancePutRequestDto";
    public static final String WORKLOAD_INSTANCE_OPERATION_PUT_REQUEST_DTO = "workloadInstanceOperationPutRequestDto";
    public static final String WORKLOAD_INSTANCE_OPERATION_POST_REQUEST_DTO = "workloadInstanceOperationPostRequestDto";
    public static final String VALUES = "values";
    public static final String VALUES_NAME = "successfulpost-1.2.3--7";
    public static final String CLUSTER_CONNECTION_INFO_YAML = "clusterConnectionInfo.yaml";
    public static final String CLUSTER_CONNECTION_INFO = "clusterConnectionInfo";
    private static final String WORKLOAD_INSTANCE_NAME = "workloadInstanceName";
    private static final String NAMESPACE = "namespace";
    private static final String CLUSTER = "clusterConnectionInfo";
    private static final String ADDITIONAL_PARAMETERS = "{\"key\": \"value\"}";
    private static final String VALID_HELMFILE_TGZ = "helmfile-test-1.2.3+7.tgz";
    private static final String INVALID_HELMFILE_TGZ = "invalid_helmfile.tgz";
    private static final String VALID_INTEGRATION_CHART_TGZ = "integration_chart-1.2.3-4.tgz";
    private static final String INVALID_INTEGRATION_CHART_TGZ = "integration_chart.tgz";
    private static final String INTEGRATION_CHART_TGZ_WITHOUT_ROOT_CHART = "integration_chart_without_root_chart.tgz";
    private static final String VALID_HELMSOURCE_VERSION = "1.2.3-4";
    private static final String ANOTHER_HELMSOURCE_VERSION = "1.2.3+7";
    private static final String VALID_VALUES_YAML = "values.yaml";
    private static final String MOCK_MULTIPART_FILE_NAME = "TempFile";
    private static final String CLUSTER_URL = "test_url";
    private static final String CRD_NAMESPACE = "crdNamespace";
    private static final String CLUSTER_IDENT = "hahn117 https://mocha.rnd.gic.ericsson.se/k8s/clusters/c-mdw5r";
    private static final Path CLUSTER_CONNECTION_INFO_PATH = Path.of("src/test/resources/" + CLUSTER_CONNECTION_INFO_YAML);
    private static final String HELM_LIST_INTEGRATION_CHART_COMMAND = "helm list --output json --filter " +
            "workloadInstanceName -n namespace";
    private static final String EMPTY_OUTPUT_JSON = "[]";
    private static final int OPERATION_TIMEOUT = 5;

    @Autowired
    private MockMvc mockMvc;

    @SpyBean
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

    @SpyBean
    private HelmSourceService helmSourceService;

    @Autowired
    private ValuesRepository valuesRepository;

    @MockBean
    private SecretsManagement secretsManagement;

    @MockBean
    private CommandExecutorHelperImpl commandExecutor;

    @Autowired
    private ReleaseService releaseService;

    @MockBean
    private HttpClientUtils httpClientUtils;

    @MockBean
    private KubernetesService kubernetesService;

    @TempDir
    private Path folder;
    private static byte[] helmfile;
    private static byte[] invalidHelmfile;
    private static byte[] integrationChart;
    private static byte[] invalidIntegrationChart;
    private static byte[] integrationChartWithoutRootChart;
    private static String json;
    private static String emptyJson;
    private static String jsonWithCluster;
    private static String jsonWithUrl;
    private static String jsonWithInvalidCluster;
    private static String updateJson;
    private static String rollbackJson;
    private static String rollbackJsonWithCurrentVersion;
    private static String rollbackJsonWithoutVersion;
    private static String terminateJson;
    private static String invalidTerminateJson;
    private static String values;
    private static String connectionInfo;

    @BeforeEach
    public void setUp() {
        String rootDirectory = folder.toString();
        setField(fileService, "rootDirectory", rootDirectory);

        final CommandResponse commandResponse = new CommandResponse();
        commandResponse.setExitCode(0);
        when(commandExecutor.executeWithRetry(anyString(), anyInt())).thenReturn(commandResponse);
        when(commandExecutor.execute(anyString(), anyInt())).thenReturn(commandResponse);

        MockMultipartFile configPart = new MockMultipartFile(CLUSTER_CONNECTION_INFO, KUBE_CONFIG, MediaType.TEXT_PLAIN_VALUE,
                                                             connectionInfo.getBytes());
        byte[] clusterConnectionInfo = fileService.getFileContent(CLUSTER_CONNECTION_INFO_PATH);
        doReturn(clusterConnectionInfo).when(fileService).getFileContent(CLUSTER_CONNECTION_INFO_PATH);
        clusterConnectionInfoService.create(CLUSTER_CONNECTION_INFO_PATH, CRD_NAMESPACE);
    }

    @BeforeEach
    public void filesSetup() throws Exception {
        helmfile = Files.readAllBytes(TestingFileUtils.getResource(VALID_HELMFILE_TGZ));
        invalidHelmfile = Files.readAllBytes(TestingFileUtils.getResource(INVALID_HELMFILE_TGZ));
        integrationChart = Files.readAllBytes(TestingFileUtils.getResource(VALID_INTEGRATION_CHART_TGZ));
        invalidIntegrationChart = Files.readAllBytes(TestingFileUtils.getResource(INVALID_INTEGRATION_CHART_TGZ));
        integrationChartWithoutRootChart = Files.readAllBytes(TestingFileUtils.getResource(INTEGRATION_CHART_TGZ_WITHOUT_ROOT_CHART));
        json = TestingFileUtils.readDataFromFile(WORKLOAD_INSTANCE_POST_JSON);
        emptyJson = TestingFileUtils.readDataFromFile(WORKLOAD_INSTANCE_POST_JSON_EMPTY);
        jsonWithCluster = TestingFileUtils.readDataFromFile(WORKLOAD_INSTANCE_POST_WITH_CLUSTER_JSON);
        jsonWithUrl = TestingFileUtils.readDataFromFile(WORKLOAD_INSTANCE_POST_WITH_URL_JSON);
        jsonWithInvalidCluster = TestingFileUtils.readDataFromFile(INVALID_WORKLOAD_INSTANCE_POST_WITH_CLUSTER_JSON);
        updateJson = TestingFileUtils.readDataFromFile(WORKLOAD_INSTANCE_PUT_JSON);
        rollbackJson = TestingFileUtils.readDataFromFile(WORKLOAD_INSTANCE_ROLLBACK_JSON);
        rollbackJsonWithCurrentVersion = TestingFileUtils.readDataFromFile(WORKLOAD_INSTANCE_ROLLBACK_JSON_WITH_CURRENT_VERSION);
        rollbackJsonWithoutVersion = TestingFileUtils.readDataFromFile(INSTANCE_ROLLBACK_WITHOUT_VERSION_JSON);
        terminateJson = TestingFileUtils.readDataFromFile(WORKLOAD_INSTANCE_OPERATION_POST_JSON);
        invalidTerminateJson = TestingFileUtils.readDataFromFile(INVALID_WORKLOAD_INSTANCE_OPERATION_POST_JSON);
        values = TestingFileUtils.readDataFromFile(VALUES_YAML);
        connectionInfo = TestingFileUtils.readDataFromFile(CLUSTER_CONNECTION_INFO_YAML);
    }

    @AfterEach
    public void cleanDb() {
        clusterConnectionInfoInstanceRepository.deleteAll();
        clusterConnectionInfoRepository.deleteAll();
        helmSourceRepository.deleteAll();
        operationRepository.deleteAll();
        workloadInstanceVersionRepository.deleteAll();
        workloadInstanceRepository.deleteAll();
        valuesRepository.deleteAll();
    }

    @Test
    void shouldAcceptPostRequestWithAllPartsWithHelmfileWithoutCluster() throws Exception {
        //helmfile part
        MockMultipartFile helmfilePart = new MockMultipartFile(HELM_SOURCE, VALID_HELMFILE_TGZ,
                                                               APPLICATION_OCTET_STREAM_VALUE, helmfile);

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_POST_REQUEST_DTO,
                                                           WORKLOAD_INSTANCE_POST_JSON, MediaType.APPLICATION_JSON_VALUE, json.getBytes());

        // values part
        MockMultipartFile valuesPart = new MockMultipartFile(VALUES, VALUES_YAML, MediaType.TEXT_PLAIN_VALUE,
                                                             values.getBytes());

        // kube config part
        MockMultipartFile configPart = new MockMultipartFile(CLUSTER_CONNECTION_INFO, KUBE_CONFIG, MediaType.TEXT_PLAIN_VALUE,
                                                             connectionInfo.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart("/cnwlcm/v1/workload_instances")
                                 .file(jsonPart)
                                 .file(valuesPart)
                                 .file(helmfilePart)
                                 .file(configPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, ACCEPTED);
        assertThat(result
                           .getResponse()
                           .getHeader("Location")).isNotEmpty();
        final String[] splitOperationUrl = result
                .getResponse()
                .getHeader("Location")
                .split("/");

        String operationIdFromHeader = splitOperationUrl[splitOperationUrl.length - 1];
        WorkloadInstanceDto response = mapper.readValue(result
                                                                .getResponse()
                                                                .getContentAsString(), WorkloadInstanceDto.class);
        assertThat(response.getWorkloadInstanceName()).isEqualTo("successfulpost");

        WorkloadInstance instance = workloadInstanceRepository
                .findById(response.getWorkloadInstanceId())
                .get();
        assertThat(instance.getLatestOperationId()).isNotEmpty();
        assertThat(instance.getLatestOperationId()).isEqualTo(operationIdFromHeader);

        Operation operation = operationRepository
                .findById(instance.getLatestOperationId())
                .get();
        assertThat(operation.getType()).isEqualTo(OperationType.INSTANTIATE);
        assertThat(operation.getState()).isIn(OperationState.PROCESSING, OperationState.COMPLETED);
    }

    @Test
    void shouldAcceptPostRequestWithAllPartsWithHelmfileWithExistingCluster() throws Exception {
        ClusterConnectionInfo clusterConnectionInfo = getDisabledClusterConnectionInfo();
        clusterConnectionInfoRepository.save(clusterConnectionInfo);

        //helmfile part
        MockMultipartFile helmfilePart = new MockMultipartFile(HELM_SOURCE, VALID_HELMFILE_TGZ,
                                                               APPLICATION_OCTET_STREAM_VALUE, helmfile);

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_POST_REQUEST_DTO,
                                                           WORKLOAD_INSTANCE_POST_WITH_CLUSTER_JSON,
                                                           MediaType.APPLICATION_JSON_VALUE, jsonWithCluster.getBytes());

        // values part
        MockMultipartFile valuesPart = new MockMultipartFile(VALUES, VALUES_YAML, "application/x-yaml",
                                                             values.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart("/cnwlcm/v1/workload_instances")
                                 .file(jsonPart)
                                 .file(valuesPart)
                                 .file(helmfilePart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, ACCEPTED);
        assertThat(result
                           .getResponse()
                           .getHeader("Location")).isNotEmpty();
        final String[] splitOperationUrl = result
                .getResponse()
                .getHeader("Location")
                .split("/");

        String operationIdFromHeader = splitOperationUrl[splitOperationUrl.length - 1];
        WorkloadInstanceDto response = mapper.readValue(result
                                                                .getResponse()
                                                                .getContentAsString(), WorkloadInstanceDto.class);
        assertThat(response.getWorkloadInstanceName()).isEqualTo("successfulpost");

        WorkloadInstance instance = workloadInstanceRepository
                .findById(response.getWorkloadInstanceId())
                .get();
        assertThat(instance.getLatestOperationId()).isNotEmpty();
        assertThat(instance.getLatestOperationId()).isEqualTo(operationIdFromHeader);

        Operation operation = operationRepository
                .findById(instance.getLatestOperationId())
                .get();
        assertThat(operation.getType()).isEqualTo(OperationType.INSTANTIATE);
        assertThat(operation.getState()).isIn(OperationState.PROCESSING, OperationState.COMPLETED);
    }

    @Test
    void shouldRejectPostRequestWithAllPartsWithHelmfileWithWrongClusterName() throws Exception {
        ClusterConnectionInfo clusterConnectionInfo = getDisabledClusterConnectionInfo();
        clusterConnectionInfoRepository.save(clusterConnectionInfo);

        //helmfile part
        MockMultipartFile helmfilePart = new MockMultipartFile(HELM_SOURCE, VALID_HELMFILE_TGZ,
                                                               APPLICATION_OCTET_STREAM_VALUE, helmfile);

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_POST_REQUEST_DTO,
                                                           INVALID_WORKLOAD_INSTANCE_POST_WITH_CLUSTER_JSON,
                                                           MediaType.APPLICATION_JSON_VALUE, jsonWithInvalidCluster.getBytes());

        // values part
        MockMultipartFile valuesPart = new MockMultipartFile(VALUES, VALUES_YAML, "application/x-yaml",
                                                             values.getBytes());

        // kube config part
        MockMultipartFile configPart = new MockMultipartFile(CLUSTER_CONNECTION_INFO, KUBE_CONFIG, "application/x-yaml",
                                                             connectionInfo.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart("/cnwlcm/v1/workload_instances")
                                 .file(jsonPart)
                                 .file(valuesPart)
                                 .file(helmfilePart)
                                 .file(configPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, BAD_REQUEST);
    }

    @Test
    void shouldRejectPostRequestWithAllPartsWithInvalidHelmfile() throws Exception {
        ClusterConnectionInfo clusterConnectionInfo = getDisabledClusterConnectionInfo();
        clusterConnectionInfoRepository.save(clusterConnectionInfo);

        //helmfile part
        MockMultipartFile helmfilePart = new MockMultipartFile(HELM_SOURCE, INVALID_HELMFILE_TGZ,
                                                               APPLICATION_OCTET_STREAM_VALUE, invalidHelmfile);

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_POST_REQUEST_DTO,
                                                           INVALID_WORKLOAD_INSTANCE_POST_WITH_CLUSTER_JSON,
                                                           MediaType.APPLICATION_JSON_VALUE, json.getBytes());

        // values part
        MockMultipartFile valuesPart = new MockMultipartFile(VALUES, VALUES_YAML, "application/x-yaml",
                                                             values.getBytes());

        // kube config part
        MockMultipartFile configPart = new MockMultipartFile(CLUSTER_CONNECTION_INFO, KUBE_CONFIG, "application/x-yaml",
                                                             connectionInfo.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart("/cnwlcm/v1/workload_instances")
                                 .file(jsonPart)
                                 .file(valuesPart)
                                 .file(helmfilePart)
                                 .file(configPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, BAD_REQUEST);
    }

    @Test
    void shouldRejectPostRequestWithAllPartsWithInvalidIntegrationChart() throws Exception {
        ClusterConnectionInfo clusterConnectionInfo = getDisabledClusterConnectionInfo();
        clusterConnectionInfoRepository.save(clusterConnectionInfo);

        //helmSource part
        MockMultipartFile helmSourcePart = new MockMultipartFile(HELM_SOURCE, INVALID_INTEGRATION_CHART_TGZ,
                                                                 APPLICATION_OCTET_STREAM_VALUE, invalidIntegrationChart);

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_POST_REQUEST_DTO,
                                                           INVALID_WORKLOAD_INSTANCE_POST_WITH_CLUSTER_JSON,
                                                           MediaType.APPLICATION_JSON_VALUE, json.getBytes());

        // values part
        MockMultipartFile valuesPart = new MockMultipartFile(VALUES, VALUES_YAML, "application/x-yaml",
                                                             values.getBytes());

        // kube config part
        MockMultipartFile configPart = new MockMultipartFile(CLUSTER_CONNECTION_INFO, KUBE_CONFIG, "application/x-yaml",
                                                             connectionInfo.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart("/cnwlcm/v1/workload_instances")
                                 .file(jsonPart)
                                 .file(valuesPart)
                                 .file(helmSourcePart)
                                 .file(configPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, BAD_REQUEST);
    }

    @Test
    void shouldAcceptPostRequestWithoutKubeConfigPart() throws Exception {
        //helmfile part
        MockMultipartFile helmfilePart = new MockMultipartFile(HELM_SOURCE, VALID_HELMFILE_TGZ,
                                                               APPLICATION_OCTET_STREAM_VALUE, helmfile);

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_POST_REQUEST_DTO, "",
                                                           MediaType.APPLICATION_JSON_VALUE, json.getBytes());

        // values part
        MockMultipartFile valuesPart = new MockMultipartFile(VALUES, VALUES_YAML, MediaType.TEXT_PLAIN_VALUE,
                                                             values.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart("/cnwlcm/v1/workload_instances")
                                 .file(jsonPart)
                                 .file(valuesPart)
                                 .file(helmfilePart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, ACCEPTED);
    }

    @Test
    void shouldAcceptPostRequestWithoutValuesPart() throws Exception {
        //values
        Path valuesPath = storeFile(VALID_VALUES_YAML);
        saveValuesToDB(valuesPath);

        //helmfile part
        MockMultipartFile helmfilePart = new MockMultipartFile(HELM_SOURCE, VALID_HELMFILE_TGZ,
                                                               APPLICATION_OCTET_STREAM_VALUE, helmfile);

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_POST_REQUEST_DTO, "",
                                                           MediaType.APPLICATION_JSON_VALUE, jsonWithCluster.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart("/cnwlcm/v1/workload_instances")
                                 .file(jsonPart)
                                 .file(helmfilePart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, ACCEPTED);
    }

    @Test
    void shouldRejectPostRequestWithAllPartsWithNotUniqueWorkloadInstanceName() throws Exception {
        ClusterConnectionInfo clusterConnectionInfo = getDisabledClusterConnectionInfo();
        clusterConnectionInfoRepository.save(clusterConnectionInfo);

        WorkloadInstance existingInstance = getWorkloadInstance();
        existingInstance.setWorkloadInstanceName("successfulpost");
        workloadInstanceRepository.save(existingInstance);

        WorkloadInstanceVersion version = getVersion(existingInstance);
        workloadInstanceVersionRepository.save(version);

        //helmfile part
        MockMultipartFile helmfilePart = new MockMultipartFile(HELM_SOURCE, VALID_HELMFILE_TGZ,
                                                               APPLICATION_OCTET_STREAM_VALUE, helmfile);

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_POST_REQUEST_DTO,
                                                           INVALID_WORKLOAD_INSTANCE_POST_WITH_CLUSTER_JSON,
                                                           MediaType.APPLICATION_JSON_VALUE, json.getBytes());

        // values part
        MockMultipartFile valuesPart = new MockMultipartFile(VALUES, VALUES_YAML, "application/x-yaml",
                                                             values.getBytes());

        // kube config part
        MockMultipartFile configPart = new MockMultipartFile(CLUSTER_CONNECTION_INFO, KUBE_CONFIG, "application/x-yaml",
                                                             connectionInfo.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart("/cnwlcm/v1/workload_instances")
                                 .file(jsonPart)
                                 .file(valuesPart)
                                 .file(helmfilePart)
                                 .file(configPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, CONFLICT);
    }

    @Test
    void shouldFailOperationWhenHelmfileExecutionFails() throws Exception {
        final CommandResponse res = new CommandResponse();
        res.setExitCode(1);
        res.setOutput("missing arguments");
        when(commandExecutor.execute(contains("apply"), eq(3))).thenReturn(res);

        //helmfile part
        MockMultipartFile helmfilePart = new MockMultipartFile(HELM_SOURCE, VALID_HELMFILE_TGZ,
                                                               APPLICATION_OCTET_STREAM_VALUE, helmfile);

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_POST_REQUEST_DTO, "",
                                                           MediaType.APPLICATION_JSON_VALUE, json.getBytes());

        // values part
        MockMultipartFile valuesPart = new MockMultipartFile(VALUES, VALUES_YAML, MediaType.TEXT_PLAIN_VALUE,
                                                             values.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart("/cnwlcm/v1/workload_instances")
                                 .file(jsonPart)
                                 .file(valuesPart)
                                 .file(helmfilePart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, ACCEPTED);

        final String[] splitOperationUrl = result
                .getResponse()
                .getHeader("Location")
                .split("/");

        String operationIdFromHeader = splitOperationUrl[splitOperationUrl.length - 1];

        await().until(() -> operationRepository
                .findById(operationIdFromHeader)
                .get()
                .getState()
                .equals(OperationState.FAILED));
    }

    @Test
    void shouldAcceptGetByIdRequest() throws Exception {
        //init
        WorkloadInstance existingInstance = getExistingWorkloadInstance();
        existingInstance.setVersion(2);
        existingInstance.setPreviousVersion(1);
        WorkloadInstanceVersion version = getVersion(existingInstance);
        workloadInstanceVersionRepository.save(version);
        workloadInstanceRepository.save(existingInstance);

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                        .multipart(String.format("/cnwlcm/v1/workload_instances/%s", existingInstance.getWorkloadInstanceId()))
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(request -> {
                            request.setMethod("GET");
                            return request;
                        }))
                .andReturn();

        assertThatResponseCodeIs(result, OK);

        WorkloadInstanceDto response = mapper.readValue(result
                .getResponse()
                .getContentAsString(), WorkloadInstanceDto.class);
        assertThat(response.getWorkloadInstanceId()).isEqualTo(existingInstance.getWorkloadInstanceId());
        assertThat(response.getVersion()).isEqualTo(2);

        WorkloadInstance instance = workloadInstanceRepository
                .findById(response.getWorkloadInstanceId())
                .get();
        assertThat(instance.getNamespace()).isEqualTo(response.getNamespace());
        assertThat(instance.getWorkloadInstanceName()).isEqualTo(response.getWorkloadInstanceName());
    }

    @Test
    void shouldRejectPostRequestWithBadHelmChartVersion() throws Exception {
        //helmfile part
        MockMultipartFile helmSourcePart = new MockMultipartFile(HELM_SOURCE, INVALID_INTEGRATION_CHART_TGZ,
                                                                 APPLICATION_OCTET_STREAM_VALUE, invalidIntegrationChart);

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_POST_REQUEST_DTO, "",
                                                           MediaType.APPLICATION_JSON_VALUE, json.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart("/cnwlcm/v1/workload_instances")
                                 .file(jsonPart)
                                 .file(helmSourcePart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, BAD_REQUEST);
        assertThat(result
                           .getResponse()
                           .getContentAsString()).containsIgnoringCase("Chart.yaml must contain version");
    }

    @Test
    void shouldRejectPostRequestMissingHelmfilePart() throws Exception {
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_POST_REQUEST_DTO, "",
                                                           MediaType.APPLICATION_JSON_VALUE, json.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart("/cnwlcm/v1/workload_instances")
                                 .file(jsonPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, BAD_REQUEST);
        assertThat(result
                           .getResponse()
                           .getContentAsString()).containsIgnoringCase("helmsource.tgz is missing from the request");
    }

    @Test
    void shouldRejectPostRequestMissingMandatoryJsonPart() throws Exception {
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart("/cnwlcm/v1/workload_instances")
                                 .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, BAD_REQUEST);
        assertThat(result
                           .getResponse()
                           .getContentAsString()).containsIgnoringCase("'workloadInstancePostRequestDto' is not present");
    }

    @Test
    void shouldRejectPostRequestEmptyJsonPart() throws Exception {
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_POST_REQUEST_DTO, "request.json",
                                                           MediaType.APPLICATION_JSON_VALUE, emptyJson.getBytes());

        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart("/cnwlcm/v1/workload_instances")
                                 .file(jsonPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, BAD_REQUEST);
        assertThat(result
                           .getResolvedException()
                           .getMessage())
                .containsIgnoringCase("Validation failed for argument")
                .containsIgnoringCase("namespace")
                .containsIgnoringCase("workloadInstanceName");
    }

    @Test
    void shouldAcceptPostRequestWithAllPartsWithIntegrationChart() throws Exception {
        //helmSource part
        MockMultipartFile helmSourcePart = new MockMultipartFile(HELM_SOURCE, VALID_INTEGRATION_CHART_TGZ,
                                                                 APPLICATION_OCTET_STREAM_VALUE, integrationChart);

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_POST_REQUEST_DTO,
                                                           WORKLOAD_INSTANCE_POST_JSON, MediaType.APPLICATION_JSON_VALUE, json.getBytes());

        // values part
        MockMultipartFile valuesPart = new MockMultipartFile(VALUES, VALUES_YAML, MediaType.TEXT_PLAIN_VALUE,
                                                             values.getBytes());

        // kube config part
        MockMultipartFile configPart = new MockMultipartFile(CLUSTER_CONNECTION_INFO, KUBE_CONFIG, MediaType.TEXT_PLAIN_VALUE,
                                                             connectionInfo.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart("/cnwlcm/v1/workload_instances")
                                 .file(jsonPart)
                                 .file(valuesPart)
                                 .file(helmSourcePart)
                                 .file(configPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, ACCEPTED);
        assertThat(result
                           .getResponse()
                           .getHeader("Location")).isNotEmpty();
        final String[] splitOperationUrl = result
                .getResponse()
                .getHeader("Location")
                .split("/");

        String operationIdFromHeader = splitOperationUrl[splitOperationUrl.length - 1];
        WorkloadInstanceDto response = mapper.readValue(result
                                                                .getResponse()
                                                                .getContentAsString(), WorkloadInstanceDto.class);
        assertThat(response.getWorkloadInstanceName()).isEqualTo("successfulpost");

        WorkloadInstance instance = workloadInstanceRepository
                .findById(response.getWorkloadInstanceId())
                .get();
        assertThat(instance.getLatestOperationId()).isNotEmpty();
        assertThat(instance.getLatestOperationId()).isEqualTo(operationIdFromHeader);

        Operation operation = operationRepository
                .findById(instance.getLatestOperationId())
                .get();
        assertThat(operation.getType()).isEqualTo(OperationType.INSTANTIATE);
        assertThat(operation.getState()).isIn(OperationState.PROCESSING, OperationState.COMPLETED);
    }

    @Test
    void shouldAcceptPostRequestWithoutKubeConfigPartWithIntegrationChart() throws Exception {
        //helmSource part
        MockMultipartFile helmSourcePart = new MockMultipartFile(HELM_SOURCE, VALID_INTEGRATION_CHART_TGZ,
                                                                 APPLICATION_OCTET_STREAM_VALUE, integrationChart);

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_POST_REQUEST_DTO, "request.json",
                                                           MediaType.APPLICATION_JSON_VALUE, json.getBytes());

        // values part
        MockMultipartFile valuesPart = new MockMultipartFile(VALUES, VALUES_YAML, MediaType.TEXT_PLAIN_VALUE,
                                                             values.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart("/cnwlcm/v1/workload_instances")
                                 .file(jsonPart)
                                 .file(valuesPart)
                                 .file(helmSourcePart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, ACCEPTED);
    }

    @Test
    void shouldRejectPostRequestWithAllPartsWithoutRootChartWithIntegrationChart() throws Exception {
        //helmSource part
        MockMultipartFile helmSourcePart = new MockMultipartFile(HELM_SOURCE, INTEGRATION_CHART_TGZ_WITHOUT_ROOT_CHART,
                                                                 APPLICATION_OCTET_STREAM_VALUE, integrationChartWithoutRootChart);

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_POST_REQUEST_DTO,
                                                           WORKLOAD_INSTANCE_POST_JSON, MediaType.APPLICATION_JSON_VALUE, json.getBytes());

        // values part
        MockMultipartFile valuesPart = new MockMultipartFile(VALUES, VALUES_YAML, MediaType.TEXT_PLAIN_VALUE,
                                                             values.getBytes());

        // kube config part
        MockMultipartFile configPart = new MockMultipartFile(CLUSTER_CONNECTION_INFO, KUBE_CONFIG, MediaType.TEXT_PLAIN_VALUE,
                                                             connectionInfo.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart("/cnwlcm/v1/workload_instances")
                                 .file(jsonPart)
                                 .file(valuesPart)
                                 .file(helmSourcePart)
                                 .file(configPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, BAD_REQUEST);
        assertThat(result
                           .getResponse()
                           .getContentAsString()).containsIgnoringCase("Chart.yaml must be present in the root directory");
    }

    @Test
    void shouldAcceptDeleteRequest() throws Exception {
        // init logic
        WorkloadInstance existingInstance = getWorkloadInstance();
        existingInstance = workloadInstanceRepository.save(existingInstance);
        Operation operation = getOperation(existingInstance, OperationType.TERMINATE);
        operation = operationRepository.save(operation);
        existingInstance.setLatestOperationId(operation.getId());
        existingInstance = workloadInstanceRepository.save(existingInstance);
        String existingInstanceId = existingInstance.getWorkloadInstanceId();
        ClusterConnectionInfo clusterConnectionInfo = getEnabledClusterConnectionInfo();
        clusterConnectionInfo = clusterConnectionInfoRepository.save(clusterConnectionInfo);
        clusterConnectionInfoInstanceRepository.save(getClusterConnectionInfoInstance(clusterConnectionInfo, existingInstance));

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .delete(String.format("/cnwlcm/v1/workload_instances/%s", existingInstanceId)))
                .andReturn();

        assertThatResponseCodeIs(result, NO_CONTENT);
    }

    @Test
    void shouldAcceptDeleteRequestAfterSuccessfulRollback() throws Exception {
        // init logic
        WorkloadInstance existingInstance = getWorkloadInstance();
        existingInstance = workloadInstanceRepository.save(existingInstance);
        Operation operation = getOperation(existingInstance, OperationType.ROLLBACK);
        operation = operationRepository.save(operation);
        existingInstance.setLatestOperationId(operation.getId());
        existingInstance = workloadInstanceRepository.save(existingInstance);
        String existingInstanceId = existingInstance.getWorkloadInstanceId();
        ClusterConnectionInfo clusterConnectionInfo = getEnabledClusterConnectionInfo();
        clusterConnectionInfo = clusterConnectionInfoRepository.save(clusterConnectionInfo);
        clusterConnectionInfoInstanceRepository.save(getClusterConnectionInfoInstance(clusterConnectionInfo, existingInstance));

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                        .delete(String.format("/cnwlcm/v1/workload_instances/%s", existingInstanceId)))
                .andReturn();

        assertThatResponseCodeIs(result, NO_CONTENT);
    }

    @Test
    void shouldAcceptTerminateWithCorrectRequestWithoutCluster() throws Exception {
        // init logic
        Path storedArchive = storeFile(VALID_HELMFILE_TGZ);
        Path valuesPath = storeFile(VALID_VALUES_YAML);
        Values values = saveValuesToDB(valuesPath);
        WorkloadInstance existingInstance = getWorkloadInstance();
        existingInstance.setCluster(null);
        existingInstance = workloadInstanceRepository.save(existingInstance);
        doReturn(ANOTHER_HELMSOURCE_VERSION).when(helmSourceService).getHelmSourceVersion(storedArchive, HelmSourceType.HELMFILE);
        helmSourceService.create(storedArchive, existingInstance, HelmSourceType.HELMFILE);
        existingInstance.setVersion(1);
        existingInstance = workloadInstanceRepository.save(existingInstance);
        WorkloadInstanceVersion version = getVersionDependsOnValues(existingInstance, values);
        workloadInstanceVersionRepository.save(version);
        String existingInstanceId = existingInstance.getWorkloadInstanceId();

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_OPERATION_POST_REQUEST_DTO, WORKLOAD_INSTANCE_OPERATION_POST_JSON,
                                                           MediaType.APPLICATION_JSON_VALUE, terminateJson.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart(String.format("/cnwlcm/v1/workload_instances/%s/operations", existingInstanceId))
                                 .file(jsonPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, ACCEPTED);
        assertThat(result
                           .getResponse()
                           .getHeader("Location")).isNotEmpty();
        final String[] splitOperationUrl = result
                .getResponse()
                .getHeader("Location")
                .split("/");

        String operationIdFromHeader = splitOperationUrl[splitOperationUrl.length - 1];

        Operation operation = operationRepository
                .findById(operationIdFromHeader)
                .get();
        assertThat(operation.getType()).isEqualTo(OperationType.TERMINATE);
        assertThat(operation.getState()).isIn(OperationState.PROCESSING, OperationState.COMPLETED);
    }

    @Test
    void shouldAcceptTerminateWithCorrectRequestWithCluster() throws Exception {
        // init logic
        Path storedArchive = storeFile(VALID_HELMFILE_TGZ);
        Path valuesPath = storeFile(VALID_VALUES_YAML);
        Values values = saveValuesToDB(valuesPath);
        WorkloadInstance existingInstance = getWorkloadInstance();
        existingInstance.setCluster(null);
        existingInstance = workloadInstanceRepository.save(existingInstance);
        doReturn(ANOTHER_HELMSOURCE_VERSION).when(helmSourceService).getHelmSourceVersion(storedArchive, HelmSourceType.HELMFILE);
        helmSourceService.create(storedArchive, existingInstance, HelmSourceType.HELMFILE);
        existingInstance.setVersion(1);
        existingInstance = workloadInstanceRepository.save(existingInstance);
        WorkloadInstanceVersion version = getVersionDependsOnValues(existingInstance, values);
        workloadInstanceVersionRepository.save(version);

        String existingInstanceId = existingInstance.getWorkloadInstanceId();

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_OPERATION_POST_REQUEST_DTO, WORKLOAD_INSTANCE_OPERATION_POST_JSON,
                                                           MediaType.APPLICATION_JSON_VALUE, terminateJson.getBytes());

        // kube config part
        MockMultipartFile configPart = new MockMultipartFile(CLUSTER_CONNECTION_INFO, KUBE_CONFIG, MediaType.TEXT_PLAIN_VALUE,
                                                             connectionInfo.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart(String.format("/cnwlcm/v1/workload_instances/%s/operations", existingInstanceId))
                                 .file(jsonPart)
                                 .file(configPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, ACCEPTED);
        assertThat(result
                           .getResponse()
                           .getHeader("Location")).isNotEmpty();
        final String[] splitOperationUrl = result
                .getResponse()
                .getHeader("Location")
                .split("/");

        String operationIdFromHeader = splitOperationUrl[splitOperationUrl.length - 1];

        Operation operation = operationRepository
                .findById(operationIdFromHeader)
                .get();
        assertThat(operation.getType()).isEqualTo(OperationType.TERMINATE);
        assertThat(operation.getState()).isIn(OperationState.PROCESSING, OperationState.COMPLETED);
    }

    @Test
    void shouldRejectTerminateRequestWhenBodyIncorrect() throws Exception {
        // init logic
        WorkloadInstance existingInstance = getWorkloadInstance();
        existingInstance = workloadInstanceRepository.save(existingInstance);
        WorkloadInstanceVersion version = getVersion(existingInstance);
        workloadInstanceVersionRepository.save(version);
        String existingInstanceId = existingInstance.getWorkloadInstanceId();

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_OPERATION_POST_REQUEST_DTO,
                                                           INVALID_WORKLOAD_INSTANCE_OPERATION_POST_JSON,
                                                           MediaType.APPLICATION_JSON_VALUE, invalidTerminateJson.getBytes());

        // kube config part
        MockMultipartFile configPart = new MockMultipartFile(CLUSTER_CONNECTION_INFO, KUBE_CONFIG, "application/x-yaml",
                                                             connectionInfo.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart(String.format("/cnwlcm/v1/workload_instances/%s/operations", existingInstanceId))
                                 .file(jsonPart)
                                 .file(configPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, BAD_REQUEST);
    }

    @Test
    void shouldAcceptTerminateWithCorrectRequestWithoutClusterForIntegrationChart() throws Exception {
        // init logic
        Path storedArchive = storeFile(VALID_INTEGRATION_CHART_TGZ);
        Path valuesPath = storeFile(VALID_VALUES_YAML);
        WorkloadInstance existingInstance = getWorkloadInstance();
        existingInstance.setCluster(null);
        existingInstance = workloadInstanceRepository.save(existingInstance);
        helmSourceService.create(storedArchive, existingInstance,
                                 HelmSourceType.INTEGRATION_CHART);
        existingInstance.setVersion(1);
        existingInstance = workloadInstanceRepository.save(existingInstance);
        Values values = saveValuesToDB(valuesPath);
        WorkloadInstanceVersion version = getVersionDependsOnValues(existingInstance, values);
        version.setHelmSourceVersion(VALID_HELMSOURCE_VERSION);
        workloadInstanceVersionRepository.save(version);
        String existingInstanceId = existingInstance.getWorkloadInstanceId();
        when(commandExecutor.executeWithRetry(HELM_LIST_INTEGRATION_CHART_COMMAND, OPERATION_TIMEOUT))
                .thenReturn(new CommandResponse(EMPTY_OUTPUT_JSON, 0));

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_OPERATION_POST_REQUEST_DTO, WORKLOAD_INSTANCE_OPERATION_POST_JSON,
                                                           MediaType.APPLICATION_JSON_VALUE, terminateJson.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart(String.format("/cnwlcm/v1/workload_instances/%s/operations", existingInstanceId))
                                 .file(jsonPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, ACCEPTED);
        assertThat(result
                           .getResponse()
                           .getHeader("Location")).isNotEmpty();
        final String[] splitOperationUrl = result
                .getResponse()
                .getHeader("Location")
                .split("/");

        String operationIdFromHeader = splitOperationUrl[splitOperationUrl.length - 1];

        Operation operation = operationRepository
                .findById(operationIdFromHeader)
                .get();
        assertThat(operation.getType()).isEqualTo(OperationType.TERMINATE);
        assertThat(operation.getState()).isIn(OperationState.PROCESSING, OperationState.COMPLETED);
    }

    @Test
    void shouldAcceptTerminateWithCorrectRequestWithClusterForIntegrationChart() throws Exception {
        // init logic
        Path storedArchive = storeFile(VALID_INTEGRATION_CHART_TGZ);
        Path valuesPath = storeFile(VALID_VALUES_YAML);
        Values values = saveValuesToDB(valuesPath);
        WorkloadInstance existingInstance = getWorkloadInstance();
        existingInstance.setCluster(null);
        existingInstance = workloadInstanceRepository.save(existingInstance);
        helmSourceService.create(storedArchive, existingInstance,
                                 HelmSourceType.INTEGRATION_CHART);
        existingInstance.setVersion(1);
        existingInstance = workloadInstanceRepository.save(existingInstance);
        WorkloadInstanceVersion version = getVersionDependsOnValues(existingInstance, values);
        version.setHelmSourceVersion(VALID_HELMSOURCE_VERSION);
        workloadInstanceVersionRepository.save(version);
        String existingInstanceId = existingInstance.getWorkloadInstanceId();
        when(commandExecutor.executeWithRetry(contains(HELM_LIST_INTEGRATION_CHART_COMMAND), eq(OPERATION_TIMEOUT)))
                .thenReturn(new CommandResponse(EMPTY_OUTPUT_JSON, 0));
        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_OPERATION_POST_REQUEST_DTO, WORKLOAD_INSTANCE_OPERATION_POST_JSON,
                                                           MediaType.APPLICATION_JSON_VALUE, terminateJson.getBytes());

        // kube config part
        MockMultipartFile configPart = new MockMultipartFile(CLUSTER_CONNECTION_INFO, KUBE_CONFIG, CONTENT_TYPE,
                                                             connectionInfo.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart(String.format("/cnwlcm/v1/workload_instances/%s/operations", existingInstanceId))
                                 .file(jsonPart)
                                 .file(configPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, ACCEPTED);
        assertThat(result
                           .getResponse()
                           .getHeader("Location")).isNotEmpty();
        final String[] splitOperationUrl = result
                .getResponse()
                .getHeader("Location")
                .split("/");

        String operationIdFromHeader = splitOperationUrl[splitOperationUrl.length - 1];

        Operation operation = operationRepository
                .findById(operationIdFromHeader)
                .get();
        assertThat(operation.getType()).isEqualTo(OperationType.TERMINATE);
        assertThat(operation.getState()).isIn(OperationState.PROCESSING, OperationState.COMPLETED);
    }

    @Test
    void shouldRejectTerminateAfterTerminate() throws Exception {
        // init logic
        Path storedArchive = storeFile(VALID_INTEGRATION_CHART_TGZ);
        Path valuesPath = storeFile(VALID_VALUES_YAML);
        WorkloadInstance existingInstance = getExistingWorkloadInstance();
        Operation terminateOperation = getOperation(existingInstance, OperationType.TERMINATE);
        terminateOperation = operationRepository.save(terminateOperation);
        existingInstance.setLatestOperationId(terminateOperation.getId());
        existingInstance = workloadInstanceRepository.save(existingInstance);
        helmSourceService.create(storedArchive, existingInstance,
                                 HelmSourceType.INTEGRATION_CHART);
        existingInstance.setVersion(1);
        existingInstance = workloadInstanceRepository.save(existingInstance);
        WorkloadInstanceVersion version = getVersion(existingInstance);
        version.setHelmSourceVersion(VALID_HELMSOURCE_VERSION);
        workloadInstanceVersionRepository.save(version);
        String existingInstanceId = existingInstance.getWorkloadInstanceId();

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_OPERATION_POST_REQUEST_DTO, WORKLOAD_INSTANCE_OPERATION_POST_JSON,
                                                           MediaType.APPLICATION_JSON_VALUE, terminateJson.getBytes());

        // kube config part
        MockMultipartFile configPart = new MockMultipartFile(CLUSTER_CONNECTION_INFO, KUBE_CONFIG, CONTENT_TYPE,
                                                             connectionInfo.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart(String.format("/cnwlcm/v1/workload_instances/%s/operations", existingInstanceId))
                                 .file(jsonPart)
                                 .file(configPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, BAD_REQUEST);
    }

    @Test
    void shouldAcceptPutRequestWithAllParts() throws Exception {
        WorkloadInstance workloadInstance = getExistingWorkloadInstance();

        Operation instantiateOperation = getOperation(workloadInstance, OperationType.INSTANTIATE);
        instantiateOperation = operationRepository.save(instantiateOperation);

        WorkloadInstanceVersion version = getVersion(workloadInstance);
        version.setHelmSourceVersion(ANOTHER_HELMSOURCE_VERSION);
        workloadInstanceVersionRepository.save(version);

        workloadInstance.setLatestOperationId(instantiateOperation.getId());
        workloadInstance.setVersion(version.getVersion());
        workloadInstanceRepository.save(workloadInstance);

        //helmfile part
        MockMultipartFile helmfilePart = new MockMultipartFile(HELM_SOURCE, VALID_HELMFILE_TGZ,
                                                               APPLICATION_OCTET_STREAM_VALUE, helmfile);

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_PUT_REQUEST_DTO,
                                                           WORKLOAD_INSTANCE_PUT_JSON, MediaType.APPLICATION_JSON_VALUE, updateJson.getBytes());

        // values part
        MockMultipartFile valuesPart = new MockMultipartFile(VALUES, VALUES_YAML, "application/x-yaml",
                                                             values.getBytes());

        // kube config part
        MockMultipartFile configPart = new MockMultipartFile(CLUSTER_CONNECTION_INFO, KUBE_CONFIG, MediaType.TEXT_PLAIN_VALUE,
                                                             connectionInfo.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart(String.format("/cnwlcm/v1/workload_instances/%s", workloadInstance.getWorkloadInstanceId()))
                                 .file(jsonPart)
                                 .file(valuesPart)
                                 .file(helmfilePart)
                                 .file(configPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA)
                                 .with(request -> {
                                     request.setMethod("PUT");
                                     return request;
                                 }))
                .andReturn();

        assertThatResponseCodeIs(result, ACCEPTED);
        assertThat(result
                           .getResponse()
                           .getHeader("Location")).isNotEmpty();
        final String[] splitOperationUrl = result
                .getResponse()
                .getHeader("Location")
                .split("/");

        String operationIdFromHeader = splitOperationUrl[splitOperationUrl.length - 1];
        WorkloadInstanceDto response = mapper.readValue(result
                                                                .getResponse()
                                                                .getContentAsString(), WorkloadInstanceDto.class);

        WorkloadInstance instance = workloadInstanceRepository
                .findById(response.getWorkloadInstanceId())
                .get();
        assertThat(instance.getLatestOperationId()).isNotEmpty();
        assertThat(instance.getLatestOperationId()).isEqualTo(operationIdFromHeader);

        Operation operation = operationRepository
                .findById(instance.getLatestOperationId())
                .get();
        assertThat(operation.getType()).isEqualTo(OperationType.UPDATE);
        assertThat(operation.getState()).isIn(OperationState.PROCESSING, OperationState.COMPLETED);
    }

    @Test
    void shouldAcceptPutRequestWithoutKubeConfigPart() throws Exception {
        WorkloadInstance existingInstance = getExistingWorkloadInstance();

        ClusterConnectionInfo clusterConnectionInfo = getEnabledClusterConnectionInfo();
        clusterConnectionInfo = clusterConnectionInfoRepository.save(clusterConnectionInfo);
        clusterConnectionInfoInstanceRepository.save(getClusterConnectionInfoInstance(clusterConnectionInfo, existingInstance));
        Operation instantiateOperation = getOperation(existingInstance, OperationType.INSTANTIATE);

        instantiateOperation = operationRepository.save(instantiateOperation);
        existingInstance.setLatestOperationId(instantiateOperation.getId());
        workloadInstanceRepository.save(existingInstance);

        //helmfile part
        MockMultipartFile helmfilePart = new MockMultipartFile(HELM_SOURCE, VALID_HELMFILE_TGZ,
                                                               APPLICATION_OCTET_STREAM_VALUE, helmfile);

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_PUT_REQUEST_DTO, "request.json",
                                                           MediaType.APPLICATION_JSON_VALUE, updateJson.getBytes());

        // values part
        MockMultipartFile valuesPart = new MockMultipartFile(VALUES, VALUES_YAML, "application/x-yaml",
                                                             values.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart(String.format("/cnwlcm/v1/workload_instances/%s", existingInstance.getWorkloadInstanceId()))
                                 .file(jsonPart)
                                 .file(valuesPart)
                                 .file(helmfilePart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA)
                                 .with(request -> {
                                     request.setMethod("PUT");
                                     return request;
                                 }))
                .andReturn();

        assertThatResponseCodeIs(result, ACCEPTED);
        Optional<ClusterConnectionInfo> savedCluster = clusterConnectionInfoRepository.findById(clusterConnectionInfo.getId());
        assertThat(savedCluster).isNotEmpty();
        assertThat(savedCluster.get().getStatus()).isEqualTo(ConnectionInfoStatus.IN_USE);
    }

    @Test
    void shouldAcceptPutRequestWithoutValuesPart() throws Exception {
        WorkloadInstance existingInstance = getExistingWorkloadInstance();
        Operation instantiateOperation = getOperation(existingInstance, OperationType.INSTANTIATE);
        instantiateOperation = operationRepository.save(instantiateOperation);
        existingInstance.setLatestOperationId(instantiateOperation.getId());
        String helmSourceVersion = getExistingHelmSource(existingInstance);
        WorkloadInstanceVersion version = getVersion(existingInstance);
        version.setHelmSourceVersion(helmSourceVersion);
        existingInstance.setVersion(version.getVersion());
        workloadInstanceVersionRepository.save(version);
        workloadInstanceRepository.save(existingInstance);

        //helmfile part
        MockMultipartFile helmfilePart = new MockMultipartFile(HELM_SOURCE, VALID_HELMFILE_TGZ,
                                                               APPLICATION_OCTET_STREAM_VALUE, helmfile);

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_PUT_REQUEST_DTO, "request.json",
                                                           MediaType.APPLICATION_JSON_VALUE, updateJson.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart(String.format("/cnwlcm/v1/workload_instances/%s", existingInstance.getWorkloadInstanceId()))
                                 .file(jsonPart)
                                 .file(helmfilePart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA)
                                 .with(request -> {
                                     request.setMethod("PUT");
                                     return request;
                                 }))
                .andReturn();

        assertThatResponseCodeIs(result, ACCEPTED);
    }

    @Test
    void shouldAcceptPutRequestMissingHelmfilePart() throws Exception {
        WorkloadInstance existingInstance = getExistingWorkloadInstance();
        Operation instantiateOperation = getOperation(existingInstance, OperationType.INSTANTIATE);
        instantiateOperation = operationRepository.save(instantiateOperation);
        existingInstance.setLatestOperationId(instantiateOperation.getId());
        String helmSourceVersion = getExistingHelmSource(existingInstance);
        WorkloadInstanceVersion version = getVersion(existingInstance);
        version.setHelmSourceVersion(helmSourceVersion);
        existingInstance.setVersion(version.getVersion());
        workloadInstanceVersionRepository.save(version);
        workloadInstanceRepository.save(existingInstance);

        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_PUT_REQUEST_DTO, "request.json",
                                                           MediaType.APPLICATION_JSON_VALUE, updateJson.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart(String.format("/cnwlcm/v1/workload_instances/%s", existingInstance.getWorkloadInstanceId()))
                                 .file(jsonPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA)
                                 .with(request -> {
                                     request.setMethod("PUT");
                                     return request;
                                 }))
                .andReturn();

        assertThatResponseCodeIs(result, ACCEPTED);
    }

    @Test
    void shouldAcceptPutRequestMissingJsonPart() throws Exception {
        WorkloadInstance existingInstance = getExistingWorkloadInstance();
        Path valuesPath = storeFile(VALID_VALUES_YAML);
        Values values = saveValuesToDB(valuesPath);
        existingInstance.setVersion(1);
        WorkloadInstanceVersion version = getVersionDependsOnValues(existingInstance, values);
        workloadInstanceVersionRepository.save(version);
        Operation instantiateOperation = getOperation(existingInstance, OperationType.INSTANTIATE);
        instantiateOperation = operationRepository.save(instantiateOperation);
        existingInstance.setLatestOperationId(instantiateOperation.getId());
        workloadInstanceRepository.save(existingInstance);

        MockMultipartFile helmfilePart = new MockMultipartFile(HELM_SOURCE, VALID_HELMFILE_TGZ,
                                                               APPLICATION_OCTET_STREAM_VALUE, helmfile);

        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart(String.format("/cnwlcm/v1/workload_instances/%s", existingInstance.getWorkloadInstanceId()))
                                 .file(helmfilePart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA)
                                 .with(request -> {
                                     request.setMethod("PUT");
                                     return request;
                                 }))
                .andReturn();

        assertThatResponseCodeIs(result, ACCEPTED);
    }

    @Test
    void shouldRejectPutRequestWithAllPartsWithInvalidHelmfile() throws Exception {
        WorkloadInstance workloadInstance = getExistingWorkloadInstance();

        Operation instantiateOperation = getOperation(workloadInstance, OperationType.INSTANTIATE);
        instantiateOperation = operationRepository.save(instantiateOperation);

        WorkloadInstanceVersion version = getVersion(workloadInstance);
        version.setHelmSourceVersion(ANOTHER_HELMSOURCE_VERSION);
        workloadInstanceVersionRepository.save(version);

        workloadInstance.setLatestOperationId(instantiateOperation.getId());
        workloadInstance.setVersion(version.getVersion());
        workloadInstanceRepository.save(workloadInstance);

        //helmfile part
        MockMultipartFile helmfilePart = new MockMultipartFile(HELM_SOURCE, INVALID_HELMFILE_TGZ,
                                                               APPLICATION_OCTET_STREAM_VALUE, invalidHelmfile);

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_PUT_REQUEST_DTO,
                                                           WORKLOAD_INSTANCE_PUT_JSON, MediaType.APPLICATION_JSON_VALUE, updateJson.getBytes());

        // values part
        MockMultipartFile valuesPart = new MockMultipartFile(VALUES, VALUES_YAML, "application/x-yaml",
                                                             values.getBytes());

        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart(String.format("/cnwlcm/v1/workload_instances/%s", workloadInstance.getWorkloadInstanceId()))
                                 .file(jsonPart)
                                 .file(valuesPart)
                                 .file(helmfilePart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA)
                                 .with(request -> {
                                     request.setMethod("PUT");
                                     return request;
                                 }))
                .andReturn();

        assertThatResponseCodeIs(result, BAD_REQUEST);
    }

    @Test
    void shouldRejectPutRequestWithAllPartsWithInvalidIntegrationChart() throws Exception {
        WorkloadInstance workloadInstance = getExistingWorkloadInstance();

        Operation instantiateOperation = getOperation(workloadInstance, OperationType.INSTANTIATE);
        instantiateOperation = operationRepository.save(instantiateOperation);

        WorkloadInstanceVersion version = getVersion(workloadInstance);
        version.setHelmSourceVersion(ANOTHER_HELMSOURCE_VERSION);
        workloadInstanceVersionRepository.save(version);

        workloadInstance.setLatestOperationId(instantiateOperation.getId());
        workloadInstance.setVersion(version.getVersion());
        workloadInstanceRepository.save(workloadInstance);

        //helmSource part
        MockMultipartFile helmSourcePart = new MockMultipartFile(HELM_SOURCE, INVALID_INTEGRATION_CHART_TGZ,
                                                                 APPLICATION_OCTET_STREAM_VALUE, invalidIntegrationChart);

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_PUT_REQUEST_DTO,
                                                           WORKLOAD_INSTANCE_PUT_JSON, MediaType.APPLICATION_JSON_VALUE, updateJson.getBytes());

        // values part
        MockMultipartFile valuesPart = new MockMultipartFile(VALUES, VALUES_YAML, "application/x-yaml",
                                                             values.getBytes());

        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart(String.format("/cnwlcm/v1/workload_instances/%s", workloadInstance.getWorkloadInstanceId()))
                                 .file(jsonPart)
                                 .file(valuesPart)
                                 .file(helmSourcePart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA)
                                 .with(request -> {
                                     request.setMethod("PUT");
                                     return request;
                                 }))
                .andReturn();

        assertThatResponseCodeIs(result, BAD_REQUEST);
    }

    @Test
    void shouldAcceptRollbackRequestWithAllParts() throws Exception {
        WorkloadInstance existingInstance = getExistingWorkloadInstance();

        Operation updateOperation = getOperation(existingInstance, OperationType.UPDATE);
        updateOperation = operationRepository.save(updateOperation);

        existingInstance.setLatestOperationId(updateOperation.getId());

        String helmSourceVersion = getExistingHelmSource(existingInstance);
        WorkloadInstanceVersion version = getVersion(existingInstance);
        existingInstance.setVersion(2);
        version.setHelmSourceVersion(helmSourceVersion);
        Values values = saveValues(existingInstance.getWorkloadInstanceName() + "-" + helmSourceVersion);
        version.setValuesVersion(values.getId());
        workloadInstanceVersionRepository.save(version);
        existingInstance = workloadInstanceRepository.save(existingInstance);

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_OPERATION_PUT_REQUEST_DTO,
                                                           WORKLOAD_INSTANCE_PUT_REQUEST_DTO,
                                                           MediaType.APPLICATION_JSON_VALUE, rollbackJson.getBytes());

        // kube config part
        MockMultipartFile configPart = new MockMultipartFile(CLUSTER_CONNECTION_INFO, KUBE_CONFIG, CONTENT_TYPE,
                connectionInfo.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                        .multipart(String.format("/cnwlcm/v1/workload_instances/%s/operations", existingInstance.getWorkloadInstanceId()))
                        .file(jsonPart)
                        .file(configPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andReturn();

        assertThatResponseCodeIs(result, ACCEPTED);
        assertThat(result
                .getResponse()
                .getHeader("Location")).isNotEmpty();
        final String[] splitOperationUrl = result
                .getResponse()
                .getHeader("Location")
                .split("/");

        String operationIdFromHeader = splitOperationUrl[splitOperationUrl.length - 1];
        WorkloadInstanceDto response = mapper.readValue(result
                .getResponse()
                .getContentAsString(), WorkloadInstanceDto.class);

        WorkloadInstance instance = workloadInstanceRepository
                .findById(response.getWorkloadInstanceId())
                .get();
        assertThat(instance.getLatestOperationId()).isNotEmpty();
        assertThat(instance.getLatestOperationId()).isEqualTo(operationIdFromHeader);

        Operation operation = operationRepository
                .findById(instance.getLatestOperationId())
                .get();
        assertThat(operation.getType()).isEqualTo(OperationType.ROLLBACK);
        assertThat(operation.getState()).isIn(OperationState.PROCESSING, OperationState.COMPLETED);
    }

    @Test
    void shouldAcceptRollbackRequestWithoutVersion() throws Exception {

        WorkloadInstance existingInstance = getExistingWorkloadInstance();

        Operation updateOperation = getOperation(existingInstance, OperationType.UPDATE);
        updateOperation = operationRepository.save(updateOperation);

        existingInstance.setVersion(2);
        existingInstance.setPreviousVersion(1);
        existingInstance.setLatestOperationId(updateOperation.getId());
        workloadInstanceRepository.save(existingInstance);

        String helmSourceVersion = getExistingHelmSourceForRollback(existingInstance);
        Values values1 = saveValues(existingInstance.getWorkloadInstanceName() + "-" + helmSourceVersion);
        saveVersion(existingInstance, 2, helmSourceVersion, values1.getId());
        Values values2 = saveValues(existingInstance.getWorkloadInstanceName() + "-" + helmSourceVersion);
        saveVersion(existingInstance, 1, helmSourceVersion, values2.getId());

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_OPERATION_PUT_REQUEST_DTO,
                                                           INSTANCE_ROLLBACK_WITHOUT_VERSION_JSON,
                                                           MediaType.APPLICATION_JSON_VALUE,
                                                           rollbackJsonWithoutVersion.getBytes());

        // kube config part
        MockMultipartFile configPart = new MockMultipartFile(CLUSTER_CONNECTION_INFO, KUBE_CONFIG, CONTENT_TYPE,
                connectionInfo.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                        .multipart(String.format("/cnwlcm/v1/workload_instances/%s/operations", existingInstance.getWorkloadInstanceId()))
                        .file(jsonPart)
                        .file(configPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andReturn();

        assertThatResponseCodeIs(result, ACCEPTED);
        assertThat(result
                .getResponse()
                .getHeader("Location")).isNotEmpty();
        final String[] splitOperationUrl = result
                .getResponse()
                .getHeader("Location")
                .split("/");

        String operationIdFromHeader = splitOperationUrl[splitOperationUrl.length - 1];
        WorkloadInstanceDto response = mapper.readValue(result
                .getResponse()
                .getContentAsString(), WorkloadInstanceDto.class);

        WorkloadInstance instance = workloadInstanceRepository
                .findById(response.getWorkloadInstanceId())
                .get();
        assertThat(instance.getLatestOperationId()).isNotEmpty();
        assertThat(instance.getLatestOperationId()).isEqualTo(operationIdFromHeader);

        Operation operation = operationRepository
                .findById(instance.getLatestOperationId())
                .get();
        assertThat(operation.getType()).isEqualTo(OperationType.ROLLBACK);
        assertThat(operation.getState()).isIn(OperationState.PROCESSING, OperationState.COMPLETED);
        if (OperationState.COMPLETED == operation.getState()) {
            assertThat(instance.getVersion()).isEqualTo(1);
            assertThat(instance.getPreviousVersion()).isEqualTo(2);
        }
    }

    @Test
    void shouldRejectRollbackRequestWithoutVersionAfterInstantiateTerminateReinstantiate() throws Exception {
        WorkloadInstance existingInstance = getExistingWorkloadInstance();

        //instantiate
        Operation instantiateOperation = getOperation(existingInstance, OperationType.INSTANTIATE);
        operationRepository.save(instantiateOperation);
        existingInstance.setVersion(1);
        existingInstance.setPreviousVersion(null);
        existingInstance.setLatestOperationId(instantiateOperation.getId());
        workloadInstanceRepository.save(existingInstance);

        //terminate
        Operation terminateOperation = getOperation(existingInstance, OperationType.TERMINATE);
        terminateOperation = operationRepository.save(terminateOperation);
        existingInstance.setLatestOperationId(terminateOperation.getId());
        workloadInstanceRepository.save(existingInstance);

        //reinstantiate
        Operation updateOperation = getOperation(existingInstance, OperationType.REINSTANTIATE);
        updateOperation = operationRepository.save(updateOperation);
        existingInstance.setLatestOperationId(updateOperation.getId());
        workloadInstanceRepository.save(existingInstance);

        String helmSourceVersion = getExistingHelmSourceForRollback(existingInstance);
        Values values1 = saveValues(existingInstance.getWorkloadInstanceName() + "-" + helmSourceVersion);
        saveVersion(existingInstance, 2, helmSourceVersion, values1.getId());
        Values values2 = saveValues(existingInstance.getWorkloadInstanceName() + "-" + helmSourceVersion);
        saveVersion(existingInstance, 1, helmSourceVersion, values2.getId());

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_OPERATION_PUT_REQUEST_DTO,
                                                           INSTANCE_ROLLBACK_WITHOUT_VERSION_JSON,
                                                           MediaType.APPLICATION_JSON_VALUE,
                                                           rollbackJsonWithoutVersion.getBytes());

        // kube config part
        MockMultipartFile configPart = new MockMultipartFile(CLUSTER_CONNECTION_INFO, KUBE_CONFIG, CONTENT_TYPE,
                                                             connectionInfo.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart(String.format("/cnwlcm/v1/workload_instances/%s/operations", existingInstance.getWorkloadInstanceId()))
                                 .file(jsonPart)
                                 .file(configPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA)
                                 .with(request -> {
                                     request.setMethod("PUT");
                                     return request;
                                 }))
                .andReturn();

        assertThatResponseCodeIs(result, BAD_REQUEST);
    }

    @Test
    void shouldRejectRollbackRequestWithActualVersionInRequest() throws Exception {
        WorkloadInstance existingInstance = getExistingWorkloadInstance();

        //instantiate
        Operation instantiateOperation = getOperation(existingInstance, OperationType.INSTANTIATE);
        operationRepository.save(instantiateOperation);
        existingInstance.setVersion(1);
        existingInstance.setPreviousVersion(null);
        existingInstance.setLatestOperationId(instantiateOperation.getId());
        workloadInstanceRepository.save(existingInstance);

        //update
        Operation updateOperation = getOperation(existingInstance, OperationType.UPDATE);
        updateOperation = operationRepository.save(updateOperation);
        existingInstance.setVersion(2);
        existingInstance.setPreviousVersion(1);
        existingInstance.setLatestOperationId(updateOperation.getId());
        workloadInstanceRepository.save(existingInstance);

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_OPERATION_PUT_REQUEST_DTO,
                                                           WORKLOAD_INSTANCE_PUT_REQUEST_DTO,
                                                           MediaType.APPLICATION_JSON_VALUE, rollbackJsonWithCurrentVersion.getBytes());

        // kube config part
        MockMultipartFile configPart = new MockMultipartFile(CLUSTER_CONNECTION_INFO, KUBE_CONFIG, CONTENT_TYPE,
                                                             connectionInfo.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart(String.format("/cnwlcm/v1/workload_instances/%s/operations", existingInstance.getWorkloadInstanceId()))
                                 .file(jsonPart)
                                 .file(configPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA)
                                 .with(request -> {
                                     request.setMethod("PUT");
                                     return request;
                                 }))
                .andReturn();

        assertThatResponseCodeIs(result, BAD_REQUEST);
    }

    @Test
    void shouldRejectRollbackRequestMissingJsonPart() throws Exception {
        WorkloadInstance existingInstance = getWorkloadInstance();

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                        .multipart("/cnwlcm/v1/workload_instances/%s/operations", existingInstance.getWorkloadInstanceId())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andReturn();

        assertThatResponseCodeIs(result, BAD_REQUEST);
        assertThat(result
                .getResponse()
                .getContentAsString())
                .contains("Required part 'workloadInstanceOperationPutRequestDto' is not present");
    }

    @Test
    void shouldAcceptPostRequestWithUrlToHelmfileWithoutValues() throws Exception {
        //helmfile part
        ResponseEntity<byte[]> helmSourceResponse = new ResponseEntity<>(helmfile, HttpStatus.OK);
        when(httpClientUtils.executeHttpRequest(any(), any(), any(), any(), eq(byte[].class))).thenReturn(helmSourceResponse);

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(WORKLOAD_INSTANCE_POST_URL_REQUEST_DTO,
                                                           WORKLOAD_INSTANCE_POST_URL_REQUEST_DTO,
                                                           MediaType.APPLICATION_JSON_VALUE, jsonWithUrl.getBytes());
        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart("/cnwlcm/v1/helmfile_fetcher/workload_instances")
                                 .file(jsonPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, ACCEPTED);
        assertThat(result
                           .getResponse()
                           .getHeader("Location")).isNotEmpty();
        final String[] splitOperationUrl = result
                .getResponse()
                .getHeader("Location")
                .split("/");

        String operationIdFromHeader = splitOperationUrl[splitOperationUrl.length - 1];
        WorkloadInstanceDto response = mapper.readValue(result
                                                                .getResponse()
                                                                .getContentAsString(), WorkloadInstanceDto.class);
        assertThat(response.getWorkloadInstanceName()).isEqualTo("successfulpost");

        WorkloadInstance instance = workloadInstanceRepository
                .findById(response.getWorkloadInstanceId())
                .get();
        assertThat(instance.getLatestOperationId()).isNotEmpty();
        assertThat(instance.getLatestOperationId()).isEqualTo(operationIdFromHeader);

        Operation operation = operationRepository
                .findById(instance.getLatestOperationId())
                .get();
        assertThat(operation.getType()).isEqualTo(OperationType.INSTANTIATE);
        assertThat(operation.getState()).isIn(OperationState.PROCESSING, OperationState.COMPLETED);
    }

    private WorkloadInstance getExistingWorkloadInstance() {
        WorkloadInstance existingInstance = getWorkloadInstance();
        existingInstance.setCluster(null);
        existingInstance = workloadInstanceRepository.save(existingInstance);
        return existingInstance;
    }

    private String getExistingHelmSource(WorkloadInstance workloadInstance) {
        HelmSource existingHelmSource = getHelmSource(workloadInstance, VALID_HELMSOURCE_VERSION);
        existingHelmSource = helmSourceRepository.save(existingHelmSource);
        return existingHelmSource.getHelmSourceVersion();
    }

    private String getExistingHelmSourceForRollback(WorkloadInstance workloadInstance) {
        HelmSource existingHelmSource = getHelmSource(workloadInstance, ANOTHER_HELMSOURCE_VERSION);
        existingHelmSource = helmSourceRepository.save(existingHelmSource);
        return existingHelmSource.getHelmSourceVersion();
    }

    private void assertThatResponseCodeIs(MvcResult result, HttpStatus httpStatus) {
        assertThat(result).isNotNull();
        assertThat(result.getResponse()).isNotNull();
        assertThat(result
                .getResponse()
                .getStatus()).isEqualTo(httpStatus.value());
    }

    private WorkloadInstance getWorkloadInstance() {
        return WorkloadInstance.builder()
                .workloadInstanceName(WORKLOAD_INSTANCE_NAME)
                .namespace(NAMESPACE)
                .cluster(CLUSTER)
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

    private Path storeFile(String name) throws IOException {
        File file = new ClassPathResource(name).getFile();
        Path directory = fileService.createDirectory();
        MockMultipartFile multipartFile = new MockMultipartFile(MOCK_MULTIPART_FILE_NAME, new FileInputStream(file));
        return fileService.storeFileIn(directory, multipartFile, name);
    }

    private ClusterConnectionInfo getEnabledClusterConnectionInfo() {
        return ClusterConnectionInfo.builder()
                .status(ConnectionInfoStatus.IN_USE)
                .name(CLUSTER)
                .url(CLUSTER_URL)
                .build();
    }

    private ClusterConnectionInfo getDisabledClusterConnectionInfo() {
        return ClusterConnectionInfo.builder()
                .status(ConnectionInfoStatus.NOT_IN_USE)
                .name(CLUSTER)
                .url(CLUSTER_URL)
                .content("kube: config".getBytes())
                .build();
    }

    private ClusterConnectionInfoInstance getClusterConnectionInfoInstance(ClusterConnectionInfo cluster, WorkloadInstance instance) {
        return ClusterConnectionInfoInstance.builder()
                .clusterConnectionInfo(cluster)
                .workloadInstance(instance)
                .build();
    }

    private WorkloadInstanceVersion getVersion(WorkloadInstance instance) {
        return WorkloadInstanceVersion.builder()
                .workloadInstance(instance)
                .version(1)
                .helmSourceVersion(ANOTHER_HELMSOURCE_VERSION)
                .valuesVersion("some_version")
                .build();
    }

    private WorkloadInstanceVersion getVersionDependsOnValues(WorkloadInstance instance, Values values) {
        return WorkloadInstanceVersion.builder()
                .workloadInstance(instance)
                .version(1)
                .helmSourceVersion(ANOTHER_HELMSOURCE_VERSION)
                .valuesVersion(values.getId())
                .build();
    }

    private void saveVersion(WorkloadInstance instance, int version, String helmSourceVersion, String valuesVersion) {
        WorkloadInstanceVersion workloadInstanceVersion =  WorkloadInstanceVersion.builder()
                .workloadInstance(instance)
                .version(version)
                .helmSourceVersion(ANOTHER_HELMSOURCE_VERSION)
                .valuesVersion(valuesVersion)
                .helmSourceVersion(helmSourceVersion)
                .build();
        workloadInstanceVersionRepository.save(workloadInstanceVersion);
    }

    private Values saveValues(String name) {
        Values valuesEntity = Values.builder()
                .name(name)
                .content(values.getBytes())
                .build();
        return valuesRepository.save(valuesEntity);
    }

    private Values saveValuesToDB(final Path valuesPath) {
        byte[] content = fileService.getFileContent(valuesPath);
        Values values = Values.builder()
                .content(content)
                .name(VALUES_NAME)
                .build();
        return valuesRepository.save(values);
    }
}