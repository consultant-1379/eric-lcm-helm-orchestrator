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

package com.ericsson.oss.management.lcm;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import com.google.common.io.Resources;

public final class TestUtils {
    private TestUtils() {}

    public static String readDataFromFile(String fileName) throws IOException, URISyntaxException {
        return Files
                .lines(getResource(fileName)).collect(Collectors.joining("\n"));
    }

    public static String readDataFromFile(Path filePath) throws IOException {
        return Files
                .lines(filePath).collect(Collectors.joining("\n"));
    }

    public static Path getResource(String fileToLocate) throws URISyntaxException {
        return Paths.get(Resources.getResource(fileToLocate).toURI());
    }

}
