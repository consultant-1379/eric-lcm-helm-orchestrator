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

import java.io.IOException;
import java.io.InputStream;

import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidFileException;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public final class YAMLParseUtils {
    private YAMLParseUtils(){}
    private static final String CONTAINS_INVALID_YAML = "File is invalid YAML. ";

    /**
     * Convert the given file to a byte array
     *
     * @param file source to convert to a byte array
     * @return byte array
     */
    public static byte[] readAsBytes(final MultipartFile file) {
        if (file == null) {
            throw new ResourceNotFoundException("Yaml file not found");
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to parse the yaml file to byte array", e);
        }
    }

    /**
     * Get yaml content from given inputStream
     *
     * @param inputStream source to convert to yaml content
     * @return yaml content
     */
    public static Iterable<Object> getYamlContent(InputStream inputStream) {
        var loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        final var yaml = new Yaml(new SafeConstructor(loaderOptions));

        try {
            return yaml.loadAll(inputStream);
        } catch (final Exception e) {
            throw new InvalidFileException(CONTAINS_INVALID_YAML + e.getMessage());
        }
    }

}
