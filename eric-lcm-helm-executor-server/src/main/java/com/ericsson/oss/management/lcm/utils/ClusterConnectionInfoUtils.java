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

import static com.ericsson.oss.management.lcm.constants.ClusterConnectionInfoConstants.UNABLE_PARSE_YAML_MESSAGE;
import static com.ericsson.oss.management.lcm.constants.ClusterConnectionInfoConstants.UNABLE_TO_PARSE_YAML_FILE_ERROR_MESSAGE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

import com.ericsson.oss.management.lcm.model.clusterconnection.KubeConfig;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidFileException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ClusterConnectionInfoUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ClusterConnectionInfoUtils() {}

    public static KubeConfig getKubeConfig(byte[] configFile) {
        try (InputStream kubeConfigInputStream = new ByteArrayInputStream(configFile)) {
            return parseKubeConfig(kubeConfigInputStream);
        } catch (IOException ioe) {
            log.error(UNABLE_TO_PARSE_YAML_FILE_ERROR_MESSAGE, ioe);
            throw new IllegalArgumentException(UNABLE_PARSE_YAML_MESSAGE);
        }
    }

    @SuppressWarnings("unchecked")
    private static KubeConfig parseKubeConfig(InputStream kubeConfigInputStream) {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));
        try {
            Object config = yaml.load(kubeConfigInputStream);
            if (config == null) {
                throw new InvalidFileException(
                        String.format(UNABLE_TO_PARSE_YAML_FILE_ERROR_MESSAGE, "Empty file"));
            }
            if (!(config instanceof Map)) {
                throw new InvalidFileException(
                        String.format(UNABLE_TO_PARSE_YAML_FILE_ERROR_MESSAGE, "Invalid Yaml: " + config));
            }
            Map<String, Object> map = (Map) config;
            if (map.get(null) != null) {
                throw new InvalidFileException(
                        String.format(UNABLE_TO_PARSE_YAML_FILE_ERROR_MESSAGE, "Invalid key for " + map.get(null)));
            }
            return MAPPER.convertValue(map, KubeConfig.class);
        } catch (YAMLException e) {
            throw new IllegalArgumentException(String.format(UNABLE_TO_PARSE_YAML_FILE_ERROR_MESSAGE, e.getMessage()), e);
        }
    }
}
