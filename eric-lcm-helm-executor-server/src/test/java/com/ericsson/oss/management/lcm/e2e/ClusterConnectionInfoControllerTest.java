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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import static com.ericsson.oss.management.lcm.constants.ClusterConnectionInfoConstants.CONTENT_TYPE;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.api.model.ClusterConnectionInfoDto;
import com.ericsson.oss.management.lcm.api.model.ConnectionInfoStatus;
import com.ericsson.oss.management.lcm.model.entity.ClusterConnectionInfo;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileServiceImpl;
import com.ericsson.oss.management.lcm.presentation.services.kubernetes.KubernetesService;
import com.ericsson.oss.management.lcm.repositories.ClusterConnectionInfoRepository;
import com.ericsson.oss.management.lcm.utils.TestingFileUtils;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.api.model.Namespace;

@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
class ClusterConnectionInfoControllerTest extends AbstractDbSetupTest {

    private static final String CLUSTER_CONNECTION_INFO = "clusterConnectionInfo";
    private static final String KUBE_CONFIG = "kube.config";
    private static final String CLUSTER_NAME = "hahn117";
    private static final String CLUSTER_CONNECTION_INFO_YAML = "clusterConnectionInfo.yaml";
    private static final String EMPTY_YAML = "empty.yaml";
    private static final String ROOT_CLUSTER_CONNECTION_INFO_URL = "/cnwlcm/v1/cluster_connection_info";
    private static final String CLUSTER_URL = "test_url";
    private static final String SLASH = "/";

    private static String connectionInfo;
    private static String emptyYaml;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private ClusterConnectionInfoRepository repository;
    @MockBean
    private CommandExecutor commandExecutor;
    @MockBean
    private KubernetesService kubernetesService;
    @SpyBean
    private FileServiceImpl fileService;

    @BeforeEach
    public void filesSetup() throws Exception {
        connectionInfo = TestingFileUtils.readDataFromFile(CLUSTER_CONNECTION_INFO_YAML);
        emptyYaml = TestingFileUtils.readDataFromFile(EMPTY_YAML);
    }

    @AfterEach
    public void cleanDb() {
        repository.deleteAll();
    }

    @Test
    void shouldAcceptPostRequestWithClusterConnectionInfoFile() throws Exception {
        when(kubernetesService.getListNamespaces(any())).thenReturn(getNamespaceList());
        // kube config part
        MockMultipartFile configPart = new MockMultipartFile(CLUSTER_CONNECTION_INFO, KUBE_CONFIG, CONTENT_TYPE,
                                                             connectionInfo.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                        .multipart(ROOT_CLUSTER_CONNECTION_INFO_URL)
                        .file(configPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, CREATED);
        ClusterConnectionInfoDto response = mapper.readValue(result
                .getResponse()
                .getContentAsString(), ClusterConnectionInfoDto.class);
        assertThat(response.getName()).isEqualTo(CLUSTER_NAME);
        assertThat(response.getId()).isNotEmpty();
        assertThat(response.getStatus()).isEqualTo(ConnectionInfoStatus.NOT_IN_USE);

        Optional<ClusterConnectionInfo> clusterConnectionInfoById = repository
                .findById(response.getId());
        assertThat(clusterConnectionInfoById).isNotNull();
    }

    @Test
    void shouldRejectPostRequestWithEmptyClusterConnectionInfoFile() throws Exception {
        // kube connectionInfo part
        MockMultipartFile configPart = new MockMultipartFile(CLUSTER_CONNECTION_INFO, KUBE_CONFIG, CONTENT_TYPE,
                                                             emptyYaml.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                        .multipart(ROOT_CLUSTER_CONNECTION_INFO_URL)
                        .file(configPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, BAD_REQUEST);
    }

    @Test
    void shouldRejectPostRequestWhenClusterConnectivityTestNotPassed() throws Exception {
        // kube connectionInfo part
        MockMultipartFile configPart = new MockMultipartFile(CLUSTER_CONNECTION_INFO, KUBE_CONFIG, CONTENT_TYPE,
                                                             emptyYaml.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart(ROOT_CLUSTER_CONNECTION_INFO_URL)
                                 .file(configPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, BAD_REQUEST);
    }

    @Test
    void shouldConflictPostRequestIfClusterConnectionInfoIsAlreadyExists() throws Exception {
        when(kubernetesService.getListNamespaces(any())).thenReturn(getNamespaceList());

        // kube connectionInfo part
        MockMultipartFile configPart = new MockMultipartFile(CLUSTER_CONNECTION_INFO, KUBE_CONFIG, CONTENT_TYPE,
                                                             connectionInfo.getBytes());

        // build and execute primary request
        MvcResult primaryResult = mockMvc
                .perform(MockMvcRequestBuilders
                        .multipart(ROOT_CLUSTER_CONNECTION_INFO_URL)
                        .file(configPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        // build and execute conflict request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                        .multipart(ROOT_CLUSTER_CONNECTION_INFO_URL)
                        .file(configPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        assertThatResponseCodeIs(result, CONFLICT);

        ClusterConnectionInfoDto response = mapper.readValue(primaryResult
                .getResponse()
                .getContentAsString(), ClusterConnectionInfoDto.class);
    }

    @Test
    void shouldDeleteClusterConnectionInfo() throws Exception {
        //prepare initial data
        ClusterConnectionInfo clusterConnectionInfo = repository.save(getClusterConnectionInfo());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders.delete(ROOT_CLUSTER_CONNECTION_INFO_URL + SLASH + clusterConnectionInfo.getId()))
                .andReturn();

        assertThatResponseCodeIs(result, NO_CONTENT);
    }

    @Test
    void shouldNotDeleteClusterConnectionInfoInUse() throws Exception {
        //prepare initial data
        ClusterConnectionInfo clusterConnectionInfo = getClusterConnectionInfo();
        clusterConnectionInfo.setStatus(com.ericsson.oss.management.lcm.model.entity.ConnectionInfoStatus.IN_USE);
        clusterConnectionInfo = repository.save(clusterConnectionInfo);

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders.delete(ROOT_CLUSTER_CONNECTION_INFO_URL + SLASH + clusterConnectionInfo.getId()))
                .andReturn();

        assertThatResponseCodeIs(result, CONFLICT);
    }

    @Test
    void shouldReturnNotFoundWhenClusterConnectionInfoNotExist() throws Exception {
        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders.delete(ROOT_CLUSTER_CONNECTION_INFO_URL + SLASH + "FAKE_ID"))
                .andReturn();

        assertThatResponseCodeIs(result, NOT_FOUND);
    }

    private void assertThatResponseCodeIs(MvcResult result, HttpStatus httpStatus) {
        assertThat(result).isNotNull();
        assertThat(result.getResponse()).isNotNull();
        assertThat(result
                .getResponse()
                .getStatus()).isEqualTo(httpStatus.value());
    }

    private ClusterConnectionInfo getClusterConnectionInfo() {
        return ClusterConnectionInfo
                .builder()
                .name(CLUSTER_CONNECTION_INFO)
                .url(CLUSTER_URL)
                .status(com.ericsson.oss.management.lcm.model.entity.ConnectionInfoStatus.NOT_IN_USE)
                .content(new byte[] {})
                .build();
    }

    private List<Namespace> getNamespaceList() {
        return List.of(new Namespace());
    }
}
