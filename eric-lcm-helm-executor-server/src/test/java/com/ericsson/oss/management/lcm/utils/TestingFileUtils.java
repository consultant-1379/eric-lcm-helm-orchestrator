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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import com.google.common.io.Resources;

@Slf4j
public final class TestingFileUtils {
    private TestingFileUtils(){}

    public static String readDataFromFile(String fileName) throws IOException, URISyntaxException {
        return Files
                .lines(getResource(fileName)).collect(Collectors.joining("\n"));
    }

    public static Path getResource(String fileToLocate) throws URISyntaxException {
        final Path path = Paths.get(Resources
                .getResource(fileToLocate)
                .toURI());
        log.info("path to {} is {}", fileToLocate, path);
        return path;
    }
}
