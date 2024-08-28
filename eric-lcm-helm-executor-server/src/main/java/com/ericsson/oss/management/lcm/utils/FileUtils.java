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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.yaml.snakeyaml.scanner.ScannerException;

import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidFileException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;

public final class FileUtils {
    private static final String CHARTS_FOLDER = "/charts";
    private static final String KIND_INGRESS = "ingress.yaml";
    private static final String TEMPLATES_FOLDER = "/templates";

    private FileUtils(){}

    public static Optional<Path> getFileFromTheDirectory(Path directory, String fileName) {
        try (Stream<Path> walk = Files.walk(directory)) {
            return walk.filter(item -> item.getFileName().toString().equals(fileName))
                    .findAny();
        } catch (IOException e) {
            throw new InvalidFileException(e.getMessage());
        }
    }

    public static Optional<Path> getFileFromTheDirectoryByNamePart(Path directory, String fileNamePart) {
        try (Stream<Path> walk = Files.walk(directory)) {
            return walk.filter(item -> item.getFileName().toString().contains(fileNamePart))
                    .findAny();
        } catch (IOException e) {
            throw new InvalidFileException(e.getMessage());
        }
    }

    public static Optional<Path> getFileFromTheDirectoryExcludingChartsDirectory(Path directory, String fileName) {
        try (Stream<Path> walk = Files.walk(directory)) {
            return walk.filter(item -> item.getFileName().toString().equals(fileName))
                    .filter(item -> !item.toString().contains(CHARTS_FOLDER))
                    .findAny();
        } catch (IOException e) {
            throw new InvalidFileException(e.getMessage());
        }
    }

    public static List<Path> getTemplatesDirectories(Path directory) {
        try (Stream<Path> folders = Files.walk(directory)) {
            return folders.filter(item -> item.toString().contains(TEMPLATES_FOLDER))
                    .toList();
        } catch (NoSuchFileException e) {
            return new ArrayList<>();
        } catch (IOException e) {
            throw new InvalidFileException(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static String getValueByPropertyFromFile(Path file, String propertyName) {
        InputStream inputStream;
        try {
            inputStream = Files.newInputStream(file);
        } catch (final Exception e) {
            throw new IllegalArgumentException(UNABLE_PARSE_YAML_MESSAGE, e);
        }

        Iterable<Object> content = YAMLParseUtils.getYamlContent(inputStream);

        try {
            String resultValue = null;
            for (Object document : content) {
                Map<String, Object> mappedDocument = (Map) document;
                Object value = Optional.ofNullable(mappedDocument.get(propertyName))
                        .orElseThrow(() -> new InvalidFileException(String.format("File %s must contain %s", file.toString(), propertyName)));
                resultValue = value.toString();
            }
            return resultValue;
        } catch (ScannerException se) {
            throw new InvalidFileException(se.getMessage());
        }
    }

    public static List<Ingress> getAllIngressFromDirectory(Path directory) {
        List<Ingress> listWithIngress = new ArrayList<>();
        if (directory.toFile().isDirectory()) {
            var mapper = new ObjectMapper(new YAMLFactory());

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.{yaml}")) {
                for (Path path : stream) {
                    var fileName = path.getFileName().toString();
                    if (fileName.equals(KIND_INGRESS)) {
                        Ingress object = mapper.readValue(path.toFile(), Ingress.class);
                        listWithIngress.add(object);
                    }
                }
            } catch (IOException | DirectoryIteratorException e) {
                throw new InvalidFileException("Unable retrieve list of ingress " + e.getMessage());
            }
        }
        return listWithIngress;
    }

}
