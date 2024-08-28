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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.HELMFILE_YAML_FILENAME;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.REPOSITORIES_YAML_FILENAME;

import java.net.URISyntaxException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.ericsson.oss.management.lcm.TestUtils;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidFileException;

@SpringBootTest(classes = { HelmSourceValidator.class })
class HelmSourceValidatorTest {

    @Test
    void shouldValidateHelmfileWithMetadata() throws URISyntaxException {
        Path helmfile = TestUtils.getResource("helmfile-test");
        assertThatCode(() -> HelmSourceValidator.validateMetadataPresence(helmfile))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldValidateHelmfileWithEricMetadata() throws URISyntaxException {
        Path helmfile = TestUtils.getResource("helmfile-test-with-eric-metadata");
        assertThatCode(() -> HelmSourceValidator.validateMetadataPresence(helmfile))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldFailWhenHelmfileWithoutMetadata() throws URISyntaxException {
        Path helmfile = TestUtils.getResource("no-metadata");
        assertThatThrownBy(() -> HelmSourceValidator.validateMetadataPresence(helmfile))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("Directory should contain metadata.yaml");
    }

    @Test
    void shouldValidateHelmfilePresence() throws URISyntaxException {
        Path directory = TestUtils.getResource("helmfile-test");
        assertThatCode(() -> HelmSourceValidator.validateFilePresence(directory, HELMFILE_YAML_FILENAME))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldValidateRepositoriesFilePresence() throws URISyntaxException {
        Path directory = TestUtils.getResource("helmfile-repository");
        assertThatCode(() -> HelmSourceValidator.validateFilePresence(directory, REPOSITORIES_YAML_FILENAME))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldFailForHelmfilePackage() throws URISyntaxException {
        Path directory = TestUtils.getResource("helmfile-package");
        assertThatThrownBy(() -> HelmSourceValidator.validateFilePresence(directory, HELMFILE_YAML_FILENAME))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void shouldFailForNoFile() throws URISyntaxException {
        Path directory = TestUtils.getResource("no-helmfile");
        assertThatThrownBy(() -> HelmSourceValidator.validateFilePresence(directory, HELMFILE_YAML_FILENAME))
                .isInstanceOf(InvalidFileException.class);
    }

}