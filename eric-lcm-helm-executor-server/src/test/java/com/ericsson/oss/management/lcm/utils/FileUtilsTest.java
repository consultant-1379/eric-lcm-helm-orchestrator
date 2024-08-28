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

package com.ericsson.oss.management.lcm.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ericsson.oss.management.lcm.TestUtils;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidFileException;

import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;

@ExtendWith(SpringExtension.class)
class FileUtilsTest {

    private static final String TEST_FILE = "configmapTest.yaml";
    private static final String TEST_FILE_PART = "Test.yaml";
    private static final String NOT_FOUND_DIRECTORY = "notFound";
    private static final String PROPERTY_NAME = "kind";
    private static Path directory;
    private static Path configMapFile;

    @BeforeAll
    static void setup() throws URISyntaxException {
        directory = TestUtils.getResource("templates");
        configMapFile = TestUtils.getResource("templates/" + TEST_FILE);
    }

    @Test
    void shouldGetFileFromTheDirectory() throws IOException {
        Path fileFromTheDirectory = FileUtils.getFileFromTheDirectory(directory, TEST_FILE).orElse(null);

        assertThat(fileFromTheDirectory).isNotNull();

        String expectedLineFromFile = "kind: ConfigMap";
        String dataFromFile = TestUtils.readDataFromFile(fileFromTheDirectory);

        assertThat(dataFromFile).contains(expectedLineFromFile);
    }

    @Test
    void shouldGetFileFromTheDirectoryByNamePart() throws IOException {
        Path fileFromTheDirectory = FileUtils.getFileFromTheDirectoryByNamePart(directory, TEST_FILE_PART).orElse(null);

        assertThat(fileFromTheDirectory).isNotNull();

        String expectedLineFromFile = "kind: ConfigMap";
        String dataFromFile = TestUtils.readDataFromFile(fileFromTheDirectory);

        assertThat(dataFromFile).contains(expectedLineFromFile);
    }

    @Test
    void shouldGetFileFromTheDirectoryByNamePartWithFullName() throws IOException {
        Path fileFromTheDirectory = FileUtils.getFileFromTheDirectoryByNamePart(directory, TEST_FILE).orElse(null);

        assertThat(fileFromTheDirectory).isNotNull();

        String expectedLineFromFile = "kind: ConfigMap";
        String dataFromFile = TestUtils.readDataFromFile(fileFromTheDirectory);

        assertThat(dataFromFile).contains(expectedLineFromFile);
    }

    @Test
    void shouldThrowExceptionWhenDirectoryIsNotFound() {
        Path invalidPath = Path.of(NOT_FOUND_DIRECTORY);
        assertThatThrownBy(() -> FileUtils.getFileFromTheDirectory(invalidPath, TEST_FILE))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void shouldThrowExceptionWhenDirectoryToGetFileByFilePartIsNotFound() {
        Path invalidPath = Path.of(NOT_FOUND_DIRECTORY);
        assertThatThrownBy(() -> FileUtils.getFileFromTheDirectoryByNamePart(invalidPath, TEST_FILE))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void shouldGetFileOutsideChartDirectory() throws IOException {
        String filename = "testExcludingChartsDirectory.yaml";
        Path fileFromTheDirectory = FileUtils.getFileFromTheDirectoryExcludingChartsDirectory(directory, filename).orElse(null);

        assertThat(fileFromTheDirectory).isNotNull();

        String expectedLineFromFile = "not in charts directory";
        String dataFromFile = TestUtils.readDataFromFile(fileFromTheDirectory);

        assertThat(dataFromFile).contains(expectedLineFromFile);
    }

    @Test
    void shouldNotGetFileFromChartsDirectory() {
        String fileInChartsDirectory = "exisitsOnlyInChartsDirectory.yaml";
        Optional<Path> path = FileUtils.getFileFromTheDirectoryExcludingChartsDirectory(
                directory,
                fileInChartsDirectory);

        assertThat(path).isEmpty();
    }

    @Test
    void shouldThrowExceptionIfDirectoryIsNotFound() {
        Path invalidPath = Path.of(NOT_FOUND_DIRECTORY);
        assertThatThrownBy(() -> FileUtils.getFileFromTheDirectoryExcludingChartsDirectory(invalidPath, TEST_FILE))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void shouldGetTemplatesDirectories() {
        int expectedQuantity = 11;
        List<Path> templatesDirectories = FileUtils.getTemplatesDirectories(directory);

        assertThat(templatesDirectories.size()).isEqualTo(expectedQuantity);
        templatesDirectories.forEach(directory -> assertThat(directory.toString()).contains("templates"));
    }

    @Test
    void shouldReturnEmptyList() {
        List<Path> templatesDirectories = FileUtils.getTemplatesDirectories(Path.of("directoryWithoutTemplates"));

        assertThat(templatesDirectories).isEmpty();
    }

    @Test
    void shouldGetValueByProperty() {
        String expectedValue = "ConfigMap";
        String actualValue = FileUtils.getValueByPropertyFromFile(configMapFile, PROPERTY_NAME);

        assertThat(actualValue).isEqualTo(expectedValue);
    }

    @Test
    void shouldThrowExceptionWhenFileIsNotFound() {
        Path nonExistentFile = Path.of("nonExistentFile.yaml");

        assertThatThrownBy(() -> FileUtils.getValueByPropertyFromFile(nonExistentFile, PROPERTY_NAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unable to parse the yaml file");
    }

    @Test
    void shouldThrowExceptionWhenPropertyIsNotFound() {
        String propertyName = "notFound";

        assertThatThrownBy(() -> FileUtils.getValueByPropertyFromFile(configMapFile, propertyName))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage(String.format("File %s must contain %s", configMapFile.toString(), propertyName));
    }

    @Test
    void shouldParseIngress() {
        List<Ingress> result = FileUtils.getAllIngressFromDirectory(directory);
        //verify result
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        verifyIngressObjects(result.get(0));
    }

    private void verifyIngressObjects(Ingress ingress) {
        assertThat(ingress.getKind()).isEqualTo("Ingress");
        verifyProperties(ingress);
    }

    private void verifyProperties(Ingress ingress) {
        assertThat(ingress.getApiVersion()).isEqualTo("v1");
        assertThat(ingress.getMetadata()).isNotNull();
        assertThat(ingress.getMetadata().getName()).isEqualTo("ingress-demo1");
    }
}