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

import static com.ericsson.oss.management.lcm.constants.FileDetails.DOT_DELIMITER;
import static com.ericsson.oss.management.lcm.utils.JSONParseUtils.parseJsonToMap;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * This class contains methods for working with Yaml
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class YamlUtility {

    private static final Pattern ESCAPED_DOT_REGEX_PATTERN = Pattern.compile("(?<!\\\\)\\.");
    private static final String BACKSLASH_DOT_DELIMITER = "\\.";

    private final YamlMapFactoryBean yamlMapFactoryBean;

    /**
     * Take a string of escaped Json and return a string of Yaml
     *
     * @param escapedJson
     * @return a string of Yaml
     */
    public String convertEscapedJsonToYaml(final String escapedJson) {
        try {
            Map<String, Object> parsedJson = parseJsonToMap(escapedJson);
            return convertMapToYamlFormat(parsedJson);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Contents of values.yaml additional parameter are malformed. " + e.getMessage(), e);
        }
    }

    /**
     * Take a Yaml document in map format and convert it to a String
     *
     * @param values
     *
     * @return
     */
    public String convertMapToYamlFormat(final Map<String, Object> values) {
        var options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setWidth(Integer.MAX_VALUE);
        var yaml = new Yaml(options);
        return yaml.dump(values);
    }

    /**
     * Take two Yaml documents in string format and merge them.
     * Note: the second parameter takes precedence
     *
     * @param yaml
     * @param valuesYamlFileAsString
     *
     * @return
     */
    public String mergeYamls(final String yaml, final String valuesYamlFileAsString) {
        var valuesYamlFromAdditionalParams = new ByteArrayResource(yaml.getBytes());
        var valuesYamlFromFile = new ByteArrayResource(valuesYamlFileAsString.getBytes());
        yamlMapFactoryBean.setResources(valuesYamlFromFile, valuesYamlFromAdditionalParams);
        Map<String, Object> mergedValues = yamlMapFactoryBean.getObject();
        return convertMapToYamlFormat(mergedValues);
    }

    /**
     * Remove dot delimiters from yaml map
     *
     * @param mapWithDotDelimiters
     * @return map without dot delimiters
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> removeDotDelimitersFromYamlMap(Map<String, Object> mapWithDotDelimiters) {
        if (!CollectionUtils.isEmpty(mapWithDotDelimiters)) {
            Map<String, Object> resultMap = new LinkedHashMap<>();

            for (Map.Entry<String, Object> entry : mapWithDotDelimiters.entrySet()) {
                String key = entry.getKey();
                if (key.contains(DOT_DELIMITER)) {
                    splitAndPutParameterWithDelimiter(mapWithDotDelimiters, resultMap, key);
                } else if (mapWithDotDelimiters.get(key) instanceof Map) {
                    resultMap.put(key, removeDotDelimitersFromYamlMap((Map<String, Object>) mapWithDotDelimiters.get(key)));
                } else {
                    resultMap.put(key, mapWithDotDelimiters.get(key));
                }
            }
            return resultMap;
        }
        return mapWithDotDelimiters;
    }

    /**
     * Calculates the nesting depth of already inserted keys, in case the key parameters are repeated.
     *
     * @param keyChainMap - a map that potentially already contains keys from the chain.
     * @param keyChain    - a chain potentially already contained in the map, possibly partially.
     * @return - the number of keys already contained in the map.
     */
    @SuppressWarnings("unchecked")
    private int getContainedKeysNestingDepthNumber(Map<String, Object> keyChainMap, String[] keyChain) {
        var nestingDepthNumber = 0;
        Map<String, Object> iterativeKeyChainMap = keyChainMap;
        for (var i = 0; i < keyChain.length
                && iterativeKeyChainMap.containsKey(keyChain[i])
                && iterativeKeyChainMap.get(keyChain[i]) instanceof Map; i++) {
            iterativeKeyChainMap = (Map<String, Object>) iterativeKeyChainMap.get(keyChain[i]);
            nestingDepthNumber++;
        }
        return nestingDepthNumber;
    }

    private void splitAndPutParameterWithDelimiter(Map<String, Object> mapWithDotDelimiters,
                                                   Map<String, Object> resultMap, String keyWithDelimiters) {
        String[] keyChain = Arrays.stream(keyWithDelimiters.split(ESCAPED_DOT_REGEX_PATTERN.pattern()))
                .map(key -> key.replace(BACKSLASH_DOT_DELIMITER, DOT_DELIMITER))
                .toArray(String[]::new);
        Object originalParamValue = mapWithDotDelimiters.get(keyWithDelimiters);
        int nestingDepthNumber = getContainedKeysNestingDepthNumber(resultMap, keyChain);
        Map<String, Object> nestedExistingMap = extractNestedMap(resultMap, keyChain, nestingDepthNumber);

        for (int i = keyChain.length - 1; i > nestingDepthNumber; i--) {
            Map<String, Object> mapWrapper = new LinkedHashMap<>();
            mapWrapper.put(keyChain[i], originalParamValue);
            originalParamValue = mapWrapper;
        }
        nestedExistingMap.put(keyChain[nestingDepthNumber], originalParamValue);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractNestedMap(Map<String, Object> keyChainMap,
                                                        String[] keyChain, int nestingDepthNumber) {
        Map<String, Object> resultNestedMap = keyChainMap;
        for (var j = 0; j < nestingDepthNumber; j++) {
            resultNestedMap = (Map<String, Object>) resultNestedMap.get(keyChain[j]);
        }
        return resultNestedMap;
    }

}
