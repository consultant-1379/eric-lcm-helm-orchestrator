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

import static com.ericsson.oss.management.lcm.constants.ClusterConnectionInfoConstants.UNABLE_PARSE_YAML_MESSAGE;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.scanner.ScannerException;

import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidFileException;
import com.ericsson.oss.management.lcm.utils.YAMLParseUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ValidationUtils {
    private ValidationUtils() {
    }

    private static final String CONTAINS_INVALID_YAML = "File is invalid YAML. ";

    public static void validateYamlFile(final Path values) {
        log.info("Validating contents of {}", values);
        InputStream inputStream;
        try {
            inputStream = Files.newInputStream(values);
        } catch (final Exception e) {
            throw new IllegalArgumentException(UNABLE_PARSE_YAML_MESSAGE, e);
        }
        validateYamlInputStream(inputStream);
    }

    public static void validateYamlFile(final MultipartFile file) {
        if (file == null) {
            throw new InvalidFileException("Yaml file not found");
        }
        log.info("Validating contents of {}", file.getOriginalFilename());
        InputStream inputStream;
        try {
            inputStream = file.getInputStream();
        } catch (final Exception e) {
            throw new IllegalArgumentException(UNABLE_PARSE_YAML_MESSAGE, e);
        }
        validateYamlInputStream(inputStream);
    }

    private static void validateYamlInputStream(InputStream inputStream) {
        Iterable<Object> content = YAMLParseUtils.getYamlContent(inputStream);

        if (content == null || !content.iterator().hasNext()) {
            throw new InvalidFileException("Yaml file cannot be empty");
        }

        if (!(content instanceof Iterable)) {
            throw new InvalidFileException(CONTAINS_INVALID_YAML);
        }

        try {
            for (Object document : content) {
                if (!(document instanceof Map)) {
                    throw new InvalidFileException(CONTAINS_INVALID_YAML);
                }
            }
        } catch (ScannerException se) {
            throw new InvalidFileException(String.format("%s %s", CONTAINS_INVALID_YAML, se.getMessage()));
        }
    }

    public static boolean matchByPattern(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.matches();
    }

}
