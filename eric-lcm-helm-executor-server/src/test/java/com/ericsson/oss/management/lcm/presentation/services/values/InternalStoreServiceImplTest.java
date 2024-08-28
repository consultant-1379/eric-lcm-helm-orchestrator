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

package com.ericsson.oss.management.lcm.presentation.services.values;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.HelmfileExecutorApplication;
import com.ericsson.oss.management.lcm.api.model.ValuesRequestDto;
import com.ericsson.oss.management.lcm.model.entity.Values;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidFileException;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidInputException;
import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.repositories.ValuesRepository;
import com.ericsson.oss.management.lcm.utils.ValuesFileComposer;

@ActiveProfiles("test")
@SpringBootTest(classes = HelmfileExecutorApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InternalStoreServiceImplTest extends AbstractDbSetupTest {

    @Autowired
    @Qualifier("internalStoreServiceImpl")
    private InternalStoreServiceImpl internalStoreService;
    @SpyBean
    private FileService fileService;
    @MockBean
    private ValuesRepository valuesRepository;
    @MockBean
    private ValuesFileComposer composer;

    private static final String WORKLOAD_INSTANCE_NAME = "name";
    private static final String HELMSOURCE_VERSION = "1.2.3-4";
    private static final String VALUES_FILE = "values.yaml";
    private static final String VALUES_COMPOSE_FILE = "values-compose.yaml";
    private static final String MOCK_MULTIPART_FILE_NAME = "TempFile";
    private static final String NOT_VALID_FILE = "not_valid_file.yaml";
    private static final String VALUES_ID = "some_id";
    private static final String VALUES = "global:\n" +
            "  crd:\n" +
            "    enabled: true\n" +
            "    namespace: eric-crd-ns\n" +
            "  pullSecret: regcred-successfulpost\n" +
            "  registry:\n" +
            "    url: armdocker.rnd.ericsson.se\n" +
            "  app:\n" +
            "    namespace: dima\n" +
            "    enabled: true\n" +
            "  chart:\n" +
            "    registry: ''\n" +
            "cn-am-test-app-a:\n" +
            "  enabled: true\n" +
            "  fuu: bar\n" +
            "  name: cn-am-test-app-a\n" +
            "cn-am-test-app-b:\n" +
            "  enabled: true\n" +
            "  fuu: bar\n" +
            "  name: cn-am-test-app-b\n" +
            "cn-am-test-app-c:\n" +
            "  enabled: false\n" +
            "  fuu: bar\n" +
            "  name: cn-am-test-app-c\n" +
            "cn-am-test-crd:\n" +
            "  enabled: false\n" +
            "  fuu: bar";

    @Test
    void shouldPrimaryPostFileSuccessfully() throws IOException {
        Path path = storeFile(VALUES_FILE);
        when(valuesRepository.findTopByName(any())).thenReturn(Optional.empty());

        internalStoreService.post(WORKLOAD_INSTANCE_NAME, HELMSOURCE_VERSION, path);
        deleteDirectory(path.getParent());

        verify(valuesRepository).save(any());
    }

    @Test
    void shouldFailWhenFileIsBroken() {
        Path path = Path.of("bad_uri");

        assertThatThrownBy(() -> internalStoreService.post(WORKLOAD_INSTANCE_NAME, HELMSOURCE_VERSION, path))
                .isInstanceOf(InvalidFileException.class);

        verify(valuesRepository, times(0)).save(any());
    }

    @Test
    void shouldRetrieveFileSuccessfully() {
        Values values = prepareValues();
        Path directory = fileService.createDirectory();
        when(valuesRepository.findTopByName(any())).thenReturn(Optional.of(values));

        Path result = internalStoreService.retrieve(WORKLOAD_INSTANCE_NAME, HELMSOURCE_VERSION, directory);
        deleteDirectory(directory);

        assertThat(result).isNotNull();
    }

    @Test
    void shouldFailWhenFileNotFound() {
        Path directory = fileService.createDirectory();
        when(valuesRepository.findTopByName(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> internalStoreService.retrieve(WORKLOAD_INSTANCE_NAME, HELMSOURCE_VERSION, directory))
                .isInstanceOf(ResourceNotFoundException.class);
        deleteDirectory(directory);

        verify(fileService, times(0)).createFile(any(), any(), any());
    }

    @Test
    void shouldRetrieveFileByVersionSuccessfully() {
        Values values = prepareValues();
        Path directory = fileService.createDirectory();
        when(valuesRepository.findById(VALUES_ID)).thenReturn(Optional.of(values));

        WorkloadInstanceVersion version = getVersion();
        Path result = internalStoreService.retrieveByVersion(WORKLOAD_INSTANCE_NAME, version, directory);
        deleteDirectory(directory);

        assertThat(result).isNotNull();
    }

    @Test
    void shouldFailWhenFileByVersionNotFound() {
        Path directory = fileService.createDirectory();
        when(valuesRepository.findTopByName(any())).thenReturn(Optional.empty());

        WorkloadInstanceVersion version = getVersion();
        assertThatThrownBy(() -> internalStoreService.retrieveByVersion(WORKLOAD_INSTANCE_NAME, version, directory))
                .isInstanceOf(ResourceNotFoundException.class);
        deleteDirectory(directory);

        verify(fileService, times(0)).createFile(any(), any(), any());
    }

    @Test
    void shouldGetByVersionSuccessfully() {
        //Init
        Values values = prepareValues();
        when(valuesRepository.findById(any())).thenReturn(Optional.of(values));

        //Test method
        byte[] content = internalStoreService.getContent(VALUES_ID);

        //Verify
        assertThat(content)
                .isNotNull()
                .isEqualTo(VALUES.getBytes());
    }

    @Test
    void shouldFailWhenGetByVersionNotFound() {
        //Init
        when(valuesRepository.findById(any())).thenReturn(Optional.empty());

        //Test method
        assertThatThrownBy(() -> internalStoreService.getContent(VALUES_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldUpdateNewValuesSuccessfully() throws IOException {
        Values values = prepareValues();
        MockMultipartFile newValues = getFile(VALUES_COMPOSE_FILE);
        when(valuesRepository.findById(any())).thenReturn(Optional.of(values));

        internalStoreService.updateValues(values.getId(), null, newValues);

        assertThat(values.getContent()).isEqualTo(newValues.getBytes());
    }

    @Test
    void shouldUpdateNewValuesWithAdditionalParamsSuccessfully() throws IOException {
        ValuesRequestDto requestDto = getValuesRequestDto();
        Values values = prepareValues();
        byte[] initialContent = values.getContent();
        when(valuesRepository.findById(values.getId())).thenReturn(Optional.of(values));
        when(valuesRepository.save(values)).thenReturn(values);
        when(composer.composeStrings(anyString(), any())).thenReturn(VALUES_COMPOSE_FILE);

        internalStoreService.updateValues(values.getId(), requestDto, getFile(VALUES_COMPOSE_FILE));

        assertThat(values.getContent()).isNotNull();
        assertThat(values.getContent()).isNotEmpty();
        assertThat(initialContent).isNotEqualTo(values.getContent());
    }

    @Test
    void shouldFailWhenRequestAndValuesAreNull() {
        Values values = prepareValues();
        var valuesId = values.getId();
        when(valuesRepository.findById(any())).thenReturn(Optional.of(values));

        assertThatThrownBy(() -> internalStoreService.updateValues(valuesId, null, null))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void shouldFailWhenAdditionalParametersAndValuesAreNull() {
        ValuesRequestDto requestDto = getEmptyValuesRequestDto();
        requestDto.setAdditionalParameters(null);
        Values values = prepareValues();
        var valuesId = values.getId();
        when(valuesRepository.findById(values.getId())).thenReturn(Optional.of(values));

        assertThatThrownBy(() -> internalStoreService.updateValues(valuesId, requestDto, null))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void shouldUpdateOldValuesWithAdditionalParamsSuccessfully() {
        ValuesRequestDto requestDto = getValuesRequestDto();
        Values values = prepareValues();
        byte[] initialContent = values.getContent();
        String oldValuesContent = new String(initialContent);
        when(valuesRepository.findById(values.getId())).thenReturn(Optional.of(values));
        when(valuesRepository.save(values)).thenReturn(values);
        when(composer.composeStrings(anyString(), any())).thenReturn(VALUES);

        internalStoreService.updateValues(values.getId(), requestDto, null);

        verify(composer).composeStrings(eq(oldValuesContent), any());
        assertThat(values.getContent()).isNotNull();
        assertThat(values.getContent()).isNotEmpty();
        assertThat(initialContent).isEqualTo(values.getContent());
    }

    @Test
    void shouldFailWhenInvalidFile() throws IOException {
        ValuesRequestDto requestDto = getEmptyValuesRequestDto();
        Values values = prepareValues();
        var valuesId = values.getId();
        MockMultipartFile multipartFile = getFile(NOT_VALID_FILE);
        when(valuesRepository.findById(values.getId())).thenReturn(Optional.of(values));

        assertThatThrownBy(() -> internalStoreService.updateValues(valuesId, requestDto, multipartFile))
                .isInstanceOf(InvalidFileException.class);
    }

    private Path storeFile(String name) throws IOException {
        MockMultipartFile multipartFile = getFile(name);
        Path directory = fileService.createDirectory();
        return fileService.storeFileIn(directory, multipartFile, name);
    }

    private Values prepareValues() {
        return Values.builder()
                .id(VALUES_ID)
                .content(VALUES.getBytes())
                .name("name-1.2.3-4")
                .build();
    }

    private WorkloadInstanceVersion getVersion() {
        return WorkloadInstanceVersion.builder()
                .valuesVersion(VALUES_ID)
                .helmSourceVersion(HELMSOURCE_VERSION)
                .build();
    }

    private ValuesRequestDto getValuesRequestDto() {
        ValuesRequestDto valuesRequestDto = new ValuesRequestDto();
        HashMap<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put("", new Object());
        valuesRequestDto.setAdditionalParameters(additionalParameters);
        return valuesRequestDto;
    }

    private ValuesRequestDto getEmptyValuesRequestDto() {
        return new ValuesRequestDto();
    }

    private MockMultipartFile getFile(String name) throws IOException {
        File file = new ClassPathResource(name).getFile();
        return new MockMultipartFile(MOCK_MULTIPART_FILE_NAME, new FileInputStream(file));
    }

    private void deleteDirectory(Path path) {
        fileService.deleteDirectory(path);
    }
}