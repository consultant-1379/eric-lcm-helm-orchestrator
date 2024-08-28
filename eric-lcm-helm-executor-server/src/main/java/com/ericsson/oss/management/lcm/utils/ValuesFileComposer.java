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

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidFileException;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidInputException;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * This class composes values and additionalParameters in one file
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ValuesFileComposer {

    private final YamlUtility yamlUtility;
    private final FileService fileService;

    private static final String VALUES_YAML = "values.yaml";

    /**
     * Compose values and additional parameters
     *
     * @param path
     * @param additionalParams
     * @return path of a file with composed values and additional parameters
     */
    public Path compose(Path path, Map<String, Object> additionalParams) {
        var valuesContent = "";
        Path values;
        if (path.toFile().isFile()) {
            valuesContent = fileService.readDataFromFile(path);
            values = path;
        } else {
            if (CollectionUtils.isEmpty(additionalParams)) {
                throw new InvalidInputException("Values and additionalParameters can't be both empty");
            }
            values = path.resolve(VALUES_YAML);
        }

        var composedResult = composeStrings(valuesContent, additionalParams);
        fileService.storeContentInFile(values, composedResult);
        return values;
    }

    /**
     * Compose values and additional parameters
     *
     * @param fileYamlValues
     * @param additionalParams
     * @return string with composed values and additional parameters
     */
    public String composeStrings(String fileYamlValues, Map<String, Object> additionalParams) {
        if (CollectionUtils.isEmpty(additionalParams)) {
            return StringUtils.defaultString(fileYamlValues);
        }

        Map<String, Object> additionalParamsToAppend = extractAdditionalParamsToAppend(additionalParams);
        return mergeAdditionalParams(fileYamlValues, additionalParamsToAppend);
    }

    private Map<String, Object> extractAdditionalParamsToAppend(Map<String, Object> requestAdditionalParams) {
        validateAdditionalParamsForNullValue(requestAdditionalParams);
        return requestAdditionalParams.entrySet().stream().filter(entry -> entry.getKey() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String mergeAdditionalParams(String fileYamlValues, Map<String, Object> additionalParamsToAppend) {
        Map<String, Object> additionalParamsWithoutDelimiters = yamlUtility.removeDotDelimitersFromYamlMap(additionalParamsToAppend);
        String additionalParamsYaml = convertMapToYamlFormat(additionalParamsWithoutDelimiters);
        if (StringUtils.isEmpty(fileYamlValues)) {
            return additionalParamsYaml;
        }
        return yamlUtility.mergeYamls(additionalParamsYaml, fileYamlValues);
    }

    private static void validateAdditionalParamsForNullValue(final Map<String, Object> requestAdditionalParams) {
        List<String> additionalParamsWithNullValue = requestAdditionalParams.entrySet()
                .stream()
                .filter(elem -> elem.getValue() == null)
                .map(Map.Entry::getKey).collect(Collectors.toList());

        if (!additionalParamsWithNullValue.isEmpty()) {
            throw new InvalidFileException(String.format("You cannot merge yaml where value is null for %s",
                                                             StringUtils.join(additionalParamsWithNullValue, ", ")));
        }
    }

    private static String convertMapToYamlFormat(final Map<String, Object> values) {
        var options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setWidth(Integer.MAX_VALUE);
        var yaml = new Yaml(options);
        return yaml.dump(values);
    }

}
