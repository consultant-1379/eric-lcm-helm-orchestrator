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
import java.util.Arrays;
import java.util.Map;

import org.apache.groovy.util.Maps;
import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ericsson.oss.management.lcm.TestUtils;
import com.ericsson.oss.management.lcm.configurations.Configs;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidInputException;

@SpringBootTest(classes = { Configs.class, YamlUtility.class})
class YamlUtilityTest {
    @Autowired
    private YamlUtility yamlUtility;

    @Test
    void readSmallJsonIntoYamlObject() {
        String json = "{\"keyA\": \"valueA\",\"keyB\": \"valueB\"}";
        String converted = yamlUtility.convertEscapedJsonToYaml(json);
        assertThat(converted).matches("keyA: valueA\nkeyB: valueB\n");
    }

    @Test
    void readNestedJsonIntoYamlObject() {
        String json = "{\"a\": {\"b\": \"c\"}}";
        String converted = yamlUtility.convertEscapedJsonToYaml(json);
        assertThat(converted).matches("a:\n  b: c\n");
    }

    @Test
    void readInvalidJsonIntoYamlObjectThrowsException() {
        String json = "{\"a\": {\"b: \"c\"}}";
        assertThatThrownBy(() -> yamlUtility.convertEscapedJsonToYaml(json))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void readLargeEscapedJsonIntoYaml() throws IOException, URISyntaxException {
        String escapedJson = TestUtils.readDataFromFile("valueFiles/large-escaped-json-a.txt");
        String converted = yamlUtility.convertEscapedJsonToYaml(escapedJson);
        assertThat(converted).contains("brAgent");
    }

    @Test
    void mergeYamlsAndVerifyPrecedence() throws IOException, URISyntaxException {
        String valuesA = TestUtils.readDataFromFile("valueFiles/values-merge-a.yaml");
        System.out.println(valuesA);
        String valuesB = TestUtils.readDataFromFile("valueFiles/values-merge-b.yaml");
        System.out.println(valuesB);
        String merged = yamlUtility.mergeYamls(valuesA, valuesB);
        assertThat(merged).contains("true");
    }

    @Test
    void mergeLargeYamlsAndVerifyContents() throws IOException, URISyntaxException {
        String valuesA = TestUtils.readDataFromFile("valueFiles/large-values-a.yaml");
        System.out.println(valuesA);
        String valuesB = TestUtils.readDataFromFile("valueFiles/large-values-b.yaml");
        System.out.println(valuesB);
        String merged = yamlUtility.mergeYamls(valuesA, valuesB);
        assertThat(merged).contains("agentToBro").contains("brAgent");
    }

    @Test
    void convertMapToYamlFormatWithListType() {
        Map<String, Object> mapWithListType = Maps.of("backupTypeList",
                                                      Arrays.asList("configuration-data1", "configuration-data2"));

        String actualYaml = yamlUtility.convertMapToYamlFormat(mapWithListType);

        assertThat(actualYaml).contains("backupTypeList").contains("configuration-data1", "configuration-data2");
    }

    @Test
    void convertMapToYamlFormatWithMapType() {
        Map<String, Object> mapWithMapType = Maps.of("eric-pm-server",
                                                     Maps.of("rbac", "false",
                                                             "server", "true"));

        String actualYamlContent = yamlUtility.convertMapToYamlFormat(mapWithMapType);

        AbstractStringAssert<?> actualContentAssert = assertThat(actualYamlContent);
        actualContentAssert.contains("eric-pm-server").contains("rbac").contains("false");
        actualContentAssert.contains("eric-pm-server").contains("server").contains("true");
    }

}