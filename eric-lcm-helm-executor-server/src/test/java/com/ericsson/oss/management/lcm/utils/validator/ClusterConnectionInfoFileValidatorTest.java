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

package com.ericsson.oss.management.lcm.utils.validator;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.management.lcm.constants.ClusterConnectionInfoConstants.CLUSTER_INVALID_FILENAME_MESSAGE;
import static com.ericsson.oss.management.lcm.constants.ClusterConnectionInfoConstants.CLUSTER_FILENAME_EMPTY_MESSAGE;
import static com.ericsson.oss.management.lcm.constants.ClusterConnectionInfoConstants.CONTENT_TYPE;
import static com.ericsson.oss.management.lcm.constants.ClusterConnectionInfoConstants.UNABLE_PARSE_YAML_MESSAGE;
import static com.ericsson.oss.management.lcm.constants.ClusterConnectionInfoConstants.CURRENTLY_SUPPORTED_IS_TEXT_FORMAT;
import static com.ericsson.oss.management.lcm.constants.ClusterConnectionInfoConstants.NAMESPACE_PROVIDED_ERROR_MESSAGE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.HelmfileExecutorApplication;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidFileException;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;

@ActiveProfiles("test")
@SpringBootTest(classes = HelmfileExecutorApplication.class)
class ClusterConnectionInfoFileValidatorTest extends AbstractDbSetupTest {

    private static final String EMPTY_CLUSTER_CONNECTION_INFO_YAML = "empty.yaml";
    private static final String INVALID_CLUSTER_CONNECTION_INFO_YAML = "not_valid_file.yaml";
    private static final String INVALID_KEY_CLUSTER_CONNECTION_INFO_YAML = "invalidKeyException.yaml";
    private static final String DUPLICATE_CLUSTER_CONNECTION_INFO_YAML = "duplicateDataClusterConnectionInfo.yaml";
    private static final String CLUSTER_CONNECTION_INFO_YAML_WITH_NAMESPACE = "clusterConnectionInfoWithNamespace.yaml";
    private static final String MOCK_MULTIPART_FILE_NAME = "TempFile";
    private static final String MOCK_MULTIPART_ORIGINAL_FILE_NAME = "TempFile.config";
    private static final String MOCK_MULTIPART_WRONG_ORIGINAL_FILE_NAME = "TempFile.config3";
    private static final String CLUSTER_CONNECTION_INFO_YAML = "clusterConnectionInfo.yaml";
    private static final String BAD_CLUSTER_CONNECTION_INFO_YAML = "bad.yaml";
    private static final String WRONG_CONTENT_TYPE = "application/x-yaml";
    private static final String EMPTY_NAME = "";

    @Autowired
    private ClusterConnectionInfoFileValidator clusterConnectionInfoFileValidator;

    @MockBean
    private FileService fileService;

    @Mock
    private Path clusterConnectionInfoPath;


    @Test
    void shouldSuccessfullyValidateClusterConnectionInfo() throws IOException {
        //Init
        MultipartFile clusterConnectionInfoFile = getFile(CLUSTER_CONNECTION_INFO_YAML, CONTENT_TYPE);
        when(fileService.getFileContent(clusterConnectionInfoPath)).thenReturn(clusterConnectionInfoFile.getBytes());

        //Test method
        clusterConnectionInfoFileValidator.validate(clusterConnectionInfoFile, clusterConnectionInfoPath);
    }

    @Test
    void shouldThrowExceptionWhenEmptyName() throws IOException {
        //Init
        MultipartFile clusterConnectionInfoFile = getFileWithCustomFileName(EMPTY_NAME);

        //Test method
        assertThatThrownBy(() -> clusterConnectionInfoFileValidator.validate(clusterConnectionInfoFile, clusterConnectionInfoPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(CLUSTER_FILENAME_EMPTY_MESSAGE);
    }

    @Test
    void shouldThrowExceptionWhenWrongName() throws IOException {
        //Init
        MultipartFile clusterConnectionInfoFile = getFileWithCustomFileName(MOCK_MULTIPART_WRONG_ORIGINAL_FILE_NAME);

        //Test method
        assertThatThrownBy(() -> clusterConnectionInfoFileValidator.validate(clusterConnectionInfoFile, clusterConnectionInfoPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(CLUSTER_INVALID_FILENAME_MESSAGE);
    }

    @Test
    void shouldThrowExceptionWhenWrongContentType() throws IOException {
        //Init
        MultipartFile clusterConnectionInfoFile = getFile(CLUSTER_CONNECTION_INFO_YAML, WRONG_CONTENT_TYPE);

        //Test method
        assertThatThrownBy(() -> clusterConnectionInfoFileValidator.validate(clusterConnectionInfoFile, clusterConnectionInfoPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(CURRENTLY_SUPPORTED_IS_TEXT_FORMAT);
    }

    @Test
    void shouldThrowExceptionWhenWrongContent() throws IOException {
        //Init
        MultipartFile clusterConnectionInfoFile = getFileWithScannerException();
        when(fileService.getFileContent(clusterConnectionInfoPath)).thenReturn(clusterConnectionInfoFile.getBytes());

        //Test method
        assertThatThrownBy(() -> clusterConnectionInfoFileValidator.validate(clusterConnectionInfoFile, clusterConnectionInfoPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(UNABLE_PARSE_YAML_MESSAGE);
    }

    @Test
    void shouldThrowExceptionForIncorrectContentType() throws IOException {
        //Init
        MultipartFile clusterConnectionInfoFile = getFile(CLUSTER_CONNECTION_INFO_YAML, WRONG_CONTENT_TYPE);

        //Test method
        assertThatThrownBy(() -> clusterConnectionInfoFileValidator.validate(clusterConnectionInfoFile, clusterConnectionInfoPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(CURRENTLY_SUPPORTED_IS_TEXT_FORMAT);
    }

    @Test
    void shouldThrowExceptionForEmptyConfigFile() throws IOException {
        //Init
        MultipartFile clusterConnectionInfoFile = getFile(EMPTY_CLUSTER_CONNECTION_INFO_YAML, CONTENT_TYPE);
        when(fileService.getFileContent(clusterConnectionInfoPath)).thenReturn(clusterConnectionInfoFile.getBytes());

        //Test method
        assertThatThrownBy(() -> clusterConnectionInfoFileValidator.validate(clusterConnectionInfoFile, clusterConnectionInfoPath))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("Empty file")
                .hasMessageContaining(UNABLE_PARSE_YAML_MESSAGE);
    }

    @Test
    void shouldThrowExceptionForInvalidConfigFile() throws IOException {
        //Init
        MultipartFile clusterConnectionInfoFile = getFile(INVALID_CLUSTER_CONNECTION_INFO_YAML, CONTENT_TYPE);
        when(fileService.getFileContent(clusterConnectionInfoPath)).thenReturn(clusterConnectionInfoFile.getBytes());

        //Test method
        assertThatThrownBy(() -> clusterConnectionInfoFileValidator.validate(clusterConnectionInfoFile, clusterConnectionInfoPath))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("Invalid Yaml:")
                .hasMessageContaining(UNABLE_PARSE_YAML_MESSAGE);
    }

    @Test
    void shouldThrowExceptionForConfigFileWithInvalidKey() throws IOException {
        //Init
        MultipartFile clusterConnectionInfoFile = getFile(INVALID_KEY_CLUSTER_CONNECTION_INFO_YAML, CONTENT_TYPE);
        when(fileService.getFileContent(clusterConnectionInfoPath)).thenReturn(clusterConnectionInfoFile.getBytes());

        //Test method
        assertThatThrownBy(() -> clusterConnectionInfoFileValidator.validate(clusterConnectionInfoFile, clusterConnectionInfoPath))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("Invalid key for")
                .hasMessageContaining(UNABLE_PARSE_YAML_MESSAGE);
    }

    @Test
    void shouldThrowExceptionWhenDuplicateDataInConfigFile() throws IOException {
        //Init
        MultipartFile clusterConnectionInfoFile = getFile(DUPLICATE_CLUSTER_CONNECTION_INFO_YAML, CONTENT_TYPE);
        when(fileService.getFileContent(clusterConnectionInfoPath)).thenReturn(clusterConnectionInfoFile.getBytes());

        //Test method
        assertThatThrownBy(() -> clusterConnectionInfoFileValidator.validate(clusterConnectionInfoFile, clusterConnectionInfoPath))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Only one user allowed")
                .hasMessageContaining("Only one cluster allowed")
                .hasMessageContaining("Only one context allowed");
    }

    @Test
    void shouldThrowExceptionForConfigFileWithNamespaceInIncorrectPlace() throws IOException {
        //Init
        MultipartFile clusterConnectionInfoFile = getFile(CLUSTER_CONNECTION_INFO_YAML_WITH_NAMESPACE, CONTENT_TYPE);
        when(fileService.getFileContent(clusterConnectionInfoPath)).thenReturn(clusterConnectionInfoFile.getBytes());

        //Test method
        assertThatThrownBy(() -> clusterConnectionInfoFileValidator.validate(clusterConnectionInfoFile, clusterConnectionInfoPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(NAMESPACE_PROVIDED_ERROR_MESSAGE);
    }

    private MockMultipartFile getFile(String path, String contentType) throws IOException {
        File file = new ClassPathResource(path).getFile();
        return new MockMultipartFile(MOCK_MULTIPART_FILE_NAME, MOCK_MULTIPART_ORIGINAL_FILE_NAME, contentType, new FileInputStream(file));
    }

    private MockMultipartFile getFileWithCustomFileName(String fileName) throws IOException {
        File file = new ClassPathResource(CLUSTER_CONNECTION_INFO_YAML).getFile();
        return new MockMultipartFile(MOCK_MULTIPART_FILE_NAME, fileName, CONTENT_TYPE, new FileInputStream(file));
    }

    private MockMultipartFile getFileWithScannerException() throws IOException {
        File file = new ClassPathResource(BAD_CLUSTER_CONNECTION_INFO_YAML).getFile();
        return new MockMultipartFile(MOCK_MULTIPART_FILE_NAME, MOCK_MULTIPART_ORIGINAL_FILE_NAME, CONTENT_TYPE, new FileInputStream(file));
    }
}
