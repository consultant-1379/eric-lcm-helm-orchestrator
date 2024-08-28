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
package com.ericsson.oss.mgmt.lcm.acceptance.utils;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.deleteDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class FileUtils {
    private FileUtils(){}

    /**
     * Get a file resource, first checks the file system and if not found there, checks the classpath
     *
     * @param fileToLocate
     *         the file to search for
     *
     * @return the file as a FileSystemResource
     */
    public static FileSystemResource getFileResource(final String fileToLocate) {
        log.info("File to locate is {}", fileToLocate);
        File file = new File(fileToLocate);
        if (!file.exists()) {
            log.error("The file {} does not exist", file.getAbsolutePath());
        }
        if (file.isFile()) {
            logFilePath(file.getAbsolutePath());
            return new FileSystemResource(file);
        }
        ClassLoader classLoader = FileUtils.class.getClassLoader();
        file = new File(requireNonNull(classLoader.getResource(fileToLocate)).getFile());
        logFilePath(file.getAbsolutePath());
        return new FileSystemResource(file);
    }

    public static void copyFile(String sourcePath, String targetPath) {
        log.info("Will copy file {} to {}", sourcePath, targetPath);
        try {
            Files.copy(Path.of(sourcePath), Path.of(targetPath));
        } catch (IOException e) {
            log.error(String.format("Something went wrong during copying file %s. Details: %s",
                                    sourcePath, e.getMessage()));
        }
    }

    public static void copyContentToDirectory(String sourceDir, String targetDir) {
        log.info("Copy directory {} to {}", sourceDir, targetDir);
        try {
            copyDirectory(new File(sourceDir), new File(targetDir));
        } catch (IOException e) {
            log.error(String.format("Can't copy content from directory %s. Details: %s", sourceDir, e.getMessage()));
        }
    }

    public static void removeDirectory(String directoryPath) {
        log.info("Delete directory {}", directoryPath);
        try {
            deleteDirectory(new File(directoryPath));
        } catch (IOException e) {
            log.error(String.format("Can't delete directory %s. Details: %s", directoryPath, e.getMessage()));
        }
    }

    private static void logFilePath(final String absolutePath) {
        log.info("Full path to file is: {}", absolutePath);
    }
}
