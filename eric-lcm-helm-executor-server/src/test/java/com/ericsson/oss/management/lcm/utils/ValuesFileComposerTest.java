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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.HelmfileExecutorApplication;
import com.ericsson.oss.management.lcm.TestUtils;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidFileException;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidInputException;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;

@ActiveProfiles("test")
@SpringBootTest(classes = HelmfileExecutorApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ValuesFileComposerTest extends AbstractDbSetupTest {

    @Autowired
    private ValuesFileComposer valuesFileComposer;
    @Autowired
    private FileService fileService;

    private Map<String, Object> testAdditionalParams;

    private static final String MOCK_MULTIPART_FILE_NAME = "TempFile";
    private static final String VALUES_PATH = "values-compose.yaml";
    private static final String VALUES_FOR_OVERRIDE_PATH = "valuesForOverride.yaml";

    @BeforeEach
    void setUp() {
        testAdditionalParams = new HashMap<>();
        testAdditionalParams.put("tags.all", false);
        testAdditionalParams.put("tags.pm", true);
        testAdditionalParams.put("eric-adp-gs-testapp.ingress.enabled", false);
        testAdditionalParams.put("eric-pm-server.server.ingress.enabled", false);
        testAdditionalParams.put("influxdb.ext.apiAccessHostname", "influxdb-service2-instantiate");
        testAdditionalParams.put("pm-testapp.ingress.domain", "server-instantiate");
    }

    @AfterEach
    void clean() {
        testAdditionalParams = new HashMap<>();
    }

    @Test
    void shouldComposeValuesWithoutAdditionalParametersSuccessfully() throws IOException {
        Path values = storeFile(VALUES_PATH);

        String primaryValuesContent = TestUtils.readDataFromFile(values);
        Path composedValues = valuesFileComposer.compose(values, null);

        String resultContent = TestUtils.readDataFromFile(composedValues);

        assertThat(resultContent).isEqualTo(primaryValuesContent);
    }

    @Test
    void shouldComposeAdditionalParametersWithoutValuesSuccessfully() throws IOException {
        Path values = fileService.createDirectory();

        Path composedValues = valuesFileComposer.compose(values, testAdditionalParams);

        String resultContent = TestUtils.readDataFromFile(composedValues);
        String additionalParametersContent = getAdditionalParametersContent();
        assertThat(resultContent).isEqualTo(additionalParametersContent);
    }

    @Test
    void shouldComposeValuesWithAdditionalParametersSuccessfully() throws IOException {
        Path values = storeFile(VALUES_PATH);

        String expectedContent = TestUtils.readDataFromFile(values) + "\n" + getAdditionalParametersContent();
        Path composedValues = valuesFileComposer.compose(values, testAdditionalParams);

        String resultContent = TestUtils.readDataFromFile(composedValues);

        assertThat(resultContent).isEqualTo(expectedContent);
    }

    @Test
    void shouldOverridePropertiesFromAdditionalParametersSuccessfully() throws IOException {
        Path values = storeFile(VALUES_FOR_OVERRIDE_PATH);

        String expectedContent = "eric-pm-server:\n"
                + "  server:\n"
                + "    ingress:\n"
                + "      enabled: false";
        String unexpectedContent = "eric-pm-server:\n"
                + "  server:\n"
                + "    ingress:\n"
                + "      enabled: true";
        Path composedValues = valuesFileComposer.compose(values, testAdditionalParams);

        String resultContent = TestUtils.readDataFromFile(composedValues);

        assertThat(resultContent).contains(expectedContent)
                .doesNotContain(unexpectedContent);
    }

    @Test
    void shouldFailWhenValuesAndAdditionalParametersAreEmpty() {
        Path values = fileService.createDirectory();
        assertThatThrownBy(() -> valuesFileComposer.compose(values, null))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void shouldFailWhenValuesFileIsInvalid() throws IOException {
        Path values = storeFile(VALUES_PATH);
        testAdditionalParams.put("key", null);

        assertThatThrownBy(() -> valuesFileComposer.compose(values, testAdditionalParams))
                .isInstanceOf(InvalidFileException.class);
    }

    private String getAdditionalParametersContent() {
        return  "influxdb:\n"
                + "  ext:\n"
                + "    apiAccessHostname: influxdb-service2-instantiate\n"
                + "pm-testapp:\n"
                + "  ingress:\n"
                + "    domain: server-instantiate\n"
                + "tags:\n"
                + "  pm: true\n"
                + "  all: false\n"
                + "eric-adp-gs-testapp:\n"
                + "  ingress:\n"
                + "    enabled: false\n"
                + "eric-pm-server:\n"
                + "  server:\n"
                + "    ingress:\n"
                + "      enabled: false";
    }

    private Path storeFile(String name) throws IOException {
        File file = new ClassPathResource(name).getFile();
        Path directory = fileService.createDirectory();
        MockMultipartFile multipartFile = new MockMultipartFile(MOCK_MULTIPART_FILE_NAME, new FileInputStream(file));
        return fileService.storeFileIn(directory, multipartFile, name);
    }

}
