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

import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.METADATA_YAML;
import static com.ericsson.oss.management.lcm.utils.FileUtils.getFileFromTheDirectory;
import static com.ericsson.oss.management.lcm.utils.FileUtils.getFileFromTheDirectoryByNamePart;
import static com.ericsson.oss.management.lcm.utils.validator.ValidationUtils.validateYamlFile;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Optional;

import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidFileException;

@Slf4j
public final class HelmSourceValidator {
    private HelmSourceValidator(){}

    public static void validateMetadataPresence(Path files) {
        Optional<Path> metadataFile = getFileFromTheDirectoryByNamePart(files, METADATA_YAML);

        if (metadataFile.isEmpty()) {
            throw new InvalidFileException("Directory should contain metadata.yaml.");
        } else {
            validateYamlFile(metadataFile.get());
        }
        log.info("Directory contains file metadata.yaml. Validation completed successfully.");
    }

    public static void validateFilePresence(Path directory, String filename) {
        if (getFileFromTheDirectory(directory, filename).isEmpty()) {
            throw new InvalidFileException(String.format("File %s must exist and locate at the root of the archive helmfile",
                    filename));
        }
    }

}
