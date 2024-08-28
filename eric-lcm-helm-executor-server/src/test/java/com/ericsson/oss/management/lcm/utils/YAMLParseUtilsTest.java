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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;

import com.ericsson.oss.management.lcm.configurations.Configs;

@SpringBootTest(classes = { Configs.class, YAMLParseUtils.class })
public class YAMLParseUtilsTest {
    private static final String VALUES_TEST_YAML = "values_test.yaml";

    @Test
    void shouldReturnArrayBytesFromFile() throws IOException {
        File file = new ClassPathResource(VALUES_TEST_YAML).getFile();
        byte[] expectedResult = Files.readAllBytes(file.toPath());
        MockMultipartFile multipartFile = new MockMultipartFile(VALUES_TEST_YAML, new FileInputStream(file));
        byte[] actualResult = YAMLParseUtils.readAsBytes(multipartFile);
        assertThat(actualResult).contains(expectedResult);
    }

    @Test
    void shouldGetYamlContent() throws IOException {
        File file = new ClassPathResource(VALUES_TEST_YAML).getFile();
        Iterable<Object> content = YAMLParseUtils.getYamlContent(new FileInputStream(file));
        assertThat(content.iterator()).isNotNull();
        assertThat(content.iterator().next().toString()).contains("{test={test=true}}");
    }
}
