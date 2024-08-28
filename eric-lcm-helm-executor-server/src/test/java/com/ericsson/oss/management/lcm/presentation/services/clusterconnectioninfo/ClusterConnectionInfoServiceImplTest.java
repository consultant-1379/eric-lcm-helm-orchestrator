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

package com.ericsson.oss.management.lcm.presentation.services.clusterconnectioninfo;

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.HelmfileExecutorApplication;
import com.ericsson.oss.management.lcm.api.model.ClusterConnectionInfoDto;
import com.ericsson.oss.management.lcm.api.model.PagedClusterConnectionInfoDto;
import com.ericsson.oss.management.lcm.model.entity.ClusterConnectionInfo;
import com.ericsson.oss.management.lcm.model.entity.ConnectionInfoStatus;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.presentation.exceptions.ClusterConnectionInfoInUseException;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidInputException;
import com.ericsson.oss.management.lcm.presentation.exceptions.NotUniqueClusterException;
import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import com.ericsson.oss.management.lcm.presentation.mappers.ClusterConnectionInfoMapper;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.repositories.ClusterConnectionInfoRepository;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.ericsson.oss.management.lcm.model.entity.ConnectionInfoStatus.NOT_IN_USE;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ActiveProfiles("test")
@SpringBootTest(classes = HelmfileExecutorApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {"gpg.secret.path=src/test/resources/security/"})
class ClusterConnectionInfoServiceImplTest extends AbstractDbSetupTest {

    private static final String MOCK_MULTIPART_FILE_NAME = "TempFile";
    private static final String CLUSTER_CONNECTION_INFO_ID = "testId";
    private static final String CLUSTER_NAME = "hahn117";
    private static final String SECOND_CLUSTER_NAME = "hahn119";
    private static final String CLUSTER_URL = "https://mocha.rnd.gic.ericsson.se/k8s/clusters/c-mdw5r";
    private static final String TEST_NAME = "testName";
    private static final String SECOND_CLUSTER_CONNECTION_INFO_ID = "secondTestId";
    private static final String CLUSTER_CONNECTION_INFO_YAML = "clusterConnectionInfo.yaml";
    private static final Integer PAGE = 2;
    private static final Integer SIZE = 2;
    private static final String SORT = "name,asc";
    private static final String CRD_NAMESPACE = "crdNamespace";
    private static final String CLUSTER_IDENT = "hahn117 https://mocha.rnd.gic.ericsson.se/k8s/clusters/c-mdw5r";
    private static final Path CLUSTER_CONNECTION_INFO_PATH = Path.of("src/test/resources/" + CLUSTER_CONNECTION_INFO_YAML);

    @Autowired
    private ClusterConnectionInfoServiceImpl clusterConnectionInfoService;
    @MockBean
    private ClusterConnectionInfoMapper dtoMapper;
    @MockBean
    private ClusterConnectionInfoRepository repository;
    @MockBean
    private CommandExecutor commandExecutor;
    @SpyBean
    private FileService fileService;

    @BeforeEach
    void setUp() {
        byte[] clusterConnectionInfo = fileService.getFileContent(CLUSTER_CONNECTION_INFO_PATH);
        doReturn(clusterConnectionInfo).when(fileService).getFileContent(CLUSTER_CONNECTION_INFO_PATH);
    }

    @Test
    void shouldCreateClusterConnectionInfoSuccessfully() throws IOException {
        //Init
        ClusterConnectionInfo connectionInfo = getClusterConnectionInfo();
        ClusterConnectionInfoDto connectionInfoDto = getClusterConnectionInfoDto(CLUSTER_CONNECTION_INFO_ID);

        when(repository.save(any())).thenReturn(connectionInfo);
        when(dtoMapper.toClusterConnectionInfoDto(connectionInfo)).thenReturn(connectionInfoDto);

        //Test method
        ClusterConnectionInfoDto response = clusterConnectionInfoService.create(CLUSTER_CONNECTION_INFO_PATH, CRD_NAMESPACE);

        //Verify
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(CLUSTER_CONNECTION_INFO_ID);
    }

    @Test
    void shouldFailWhenClusterConnectionInfoNameIsNotUnique() throws IOException {
        MockMultipartFile file = getFile();
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException(""));

        assertThatThrownBy(() -> clusterConnectionInfoService.create(CLUSTER_CONNECTION_INFO_PATH, CRD_NAMESPACE))
                .isInstanceOf(NotUniqueClusterException.class);
    }

    @Test
    void shouldGetClusterConnectionInfoSuccessfully() {
        //Init
        ClusterConnectionInfo connectionInfo = getClusterConnectionInfo();
        ClusterConnectionInfoDto connectionInfoDto = getClusterConnectionInfoDto(CLUSTER_CONNECTION_INFO_ID);

        when(repository.findById(CLUSTER_CONNECTION_INFO_ID)).thenReturn(Optional.of(connectionInfo));
        when(dtoMapper.toClusterConnectionInfoDto(connectionInfo)).thenReturn(connectionInfoDto);

        //Test method
        ClusterConnectionInfoDto response = clusterConnectionInfoService.get(CLUSTER_CONNECTION_INFO_ID);

        //Verify
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(CLUSTER_CONNECTION_INFO_ID);
    }

    @Test
    void shouldFailWhenClusterConnectionInfoNotFound() {
        when(repository.findById(CLUSTER_CONNECTION_INFO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clusterConnectionInfoService.get(CLUSTER_CONNECTION_INFO_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldDeleteSuccessfully() {
        when(repository.findById(CLUSTER_CONNECTION_INFO_ID)).thenReturn(Optional.of(getClusterConnectionInfo()));
        clusterConnectionInfoService.delete(CLUSTER_CONNECTION_INFO_ID);
        verify(repository).deleteById(CLUSTER_CONNECTION_INFO_ID);
    }

    @Test
    void shouldFailWhenDeleteNotExistingClusterConnectionInfo() {
        when(repository.findById(CLUSTER_CONNECTION_INFO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clusterConnectionInfoService.delete(CLUSTER_CONNECTION_INFO_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, times(0)).deleteById(CLUSTER_CONNECTION_INFO_ID);
    }

    @Test
    void shouldFailWhenClusterConnectionInfoIsInUse() {
        ClusterConnectionInfo clusterConnectionInfo = getClusterConnectionInfo();
        clusterConnectionInfo.setStatus(ConnectionInfoStatus.IN_USE);
        when(repository.findById(CLUSTER_CONNECTION_INFO_ID)).thenReturn(Optional.of(clusterConnectionInfo));

        assertThatThrownBy(() -> clusterConnectionInfoService.delete(CLUSTER_CONNECTION_INFO_ID))
                .isInstanceOf(ClusterConnectionInfoInUseException.class);

        verify(repository, times(0)).deleteById(CLUSTER_CONNECTION_INFO_ID);
    }

    @Test
    void shouldReturnPagedClusterConnectionInfoDto() {
        //Init
        List<String> sortList = Arrays.asList(SORT);
        Pageable pageable = PageRequest.of(PAGE, SIZE);
        Page<ClusterConnectionInfo> resultsPage = new PageImpl<>(getClusterConnectionInfos(), pageable, 9L);

        ClusterConnectionInfoDto connectionInfoDto1 = getClusterConnectionInfoDto(CLUSTER_CONNECTION_INFO_ID);
        ClusterConnectionInfoDto connectionInfoDto2 = getClusterConnectionInfoDto(SECOND_CLUSTER_CONNECTION_INFO_ID);

        when(repository.findAll(any(Pageable.class))).thenReturn(resultsPage);
        when(dtoMapper.toClusterConnectionInfoDto(any(ClusterConnectionInfo.class))).
                thenReturn(connectionInfoDto1).thenReturn(connectionInfoDto2);

        //Test method
        PagedClusterConnectionInfoDto result = clusterConnectionInfoService.getAllClusterConnectionInfo(PAGE, SIZE, sortList);

        //Verify
        verify(repository, times(1)).findAll(any(Pageable.class));
        verify(dtoMapper, times(2)).toClusterConnectionInfoDto(any());
        assertThat(result.getContent()).hasSize(SIZE);
    }

    @Test
    void shouldGetClusterUrlSuccessfully() throws IOException {
        byte[] file = getFile().getBytes();

        String response = clusterConnectionInfoService.getClusterUrl(file);

        assertThat(response)
                .isNotNull()
                .isEqualTo(CLUSTER_URL);
    }

    @Test
    void shouldNotThrowExceptionWhenClusterIdentsIsEquals() throws IOException {
        MultipartFile clusterConnectionInfoFile = getFile();

        WorkloadInstance workloadInstance = new WorkloadInstance();
        workloadInstance.setClusterIdentifier(CLUSTER_IDENT);

        assertThatCode(() -> clusterConnectionInfoService.verifyClusterIdentifier(workloadInstance, CLUSTER_CONNECTION_INFO_PATH))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldGetClusterIdentifierFromInputFile() throws IOException {
        //Init
        MultipartFile clusterConnectionInfoFile = getFile();
        WorkloadInstance workloadInstance = new WorkloadInstance();

        //testMethod
        String result = clusterConnectionInfoService.resolveClusterIdentifier(CLUSTER_CONNECTION_INFO_PATH);

        //Verify
        assertThat(result).isEqualTo(CLUSTER_IDENT);
    }

    @Test
    void shouldGetClusterIdentifierByClusterName() {
        //Init
        ClusterConnectionInfo clusterConnectionInfo = new ClusterConnectionInfo();
        clusterConnectionInfo.setName(CLUSTER_NAME);
        clusterConnectionInfo.setUrl(CLUSTER_URL);
        WorkloadInstance workloadInstance = new WorkloadInstance();
        workloadInstance.setCluster(CLUSTER_NAME);
        when(clusterConnectionInfoService.findByClusterName(CLUSTER_NAME)).thenReturn(Optional.of(clusterConnectionInfo));

        //testMethod
        String result = clusterConnectionInfoService.resolveClusterIdentifier(CLUSTER_CONNECTION_INFO_PATH);

        //Verify
        assertThat(result).isEqualTo(CLUSTER_IDENT);
    }

    @Test
    void shouldGetClusterIdentifierByClusterConnectionInfoFile() throws IOException {
        //Init
        MultipartFile clusterConnectionInfoFile = getFile();

        //testMethod
        String result = clusterConnectionInfoService.resolveClusterIdentifier(CLUSTER_CONNECTION_INFO_PATH);

        //Verify
        assertThat(result).isEqualTo(CLUSTER_IDENT);
    }

    @Test
    void shouldGetClusterIdentFromFileWhenClusterNameAndFileSent() throws IOException {
        //Init
        MultipartFile clusterConnectionInfoFile = getFile();
        WorkloadInstance workloadInstance = new WorkloadInstance();
        workloadInstance.setCluster(SECOND_CLUSTER_NAME);
        //testMethod
        String result = clusterConnectionInfoService.resolveClusterIdentifier(CLUSTER_CONNECTION_INFO_PATH);
        //Verify
        assertThat(result).isEqualTo(CLUSTER_IDENT);
    }

    @Test
    void shouldThrowExceptionWhenClusterIdentsNotEquals() throws IOException {
        MultipartFile clusterConnectionInfoFile = getFile();

        WorkloadInstance workloadInstance = new WorkloadInstance();
        workloadInstance.setClusterIdentifier(CLUSTER_URL);

        assertThatThrownBy(() -> clusterConnectionInfoService.verifyClusterIdentifier(workloadInstance, CLUSTER_CONNECTION_INFO_PATH))
                .isInstanceOf(InvalidInputException.class);
    }

    private ClusterConnectionInfoDto getClusterConnectionInfoDto(String id) {
        ClusterConnectionInfoDto result = new ClusterConnectionInfoDto();
        result.setId(id);
        result.setName(TEST_NAME);
        result.setStatus(com.ericsson.oss.management.lcm.api.model.ConnectionInfoStatus.NOT_IN_USE);
        return result;
    }

    private List<ClusterConnectionInfo> getClusterConnectionInfos() {
        List<ClusterConnectionInfo> pagesList = new ArrayList<>();
        pagesList.add(getClusterConnectionInfo(CLUSTER_CONNECTION_INFO_ID));
        pagesList.add(getClusterConnectionInfo(SECOND_CLUSTER_CONNECTION_INFO_ID));
        return pagesList;
    }

    private ClusterConnectionInfo getClusterConnectionInfo(String id) {
        return ClusterConnectionInfo.builder()
                .id(id)
                .name(TEST_NAME)
                .content(new byte[] {})
                .status(NOT_IN_USE)
                .build();
    }

    private MockMultipartFile getFile() throws IOException {
        File file = new ClassPathResource(CLUSTER_CONNECTION_INFO_YAML).getFile();
        return new MockMultipartFile(MOCK_MULTIPART_FILE_NAME, new FileInputStream(file));
    }

    private ClusterConnectionInfo getClusterConnectionInfo() {
        return ClusterConnectionInfo.builder()
                .id(CLUSTER_CONNECTION_INFO_ID)
                .name("testName")
                .status(NOT_IN_USE)
                .build();
    }
}