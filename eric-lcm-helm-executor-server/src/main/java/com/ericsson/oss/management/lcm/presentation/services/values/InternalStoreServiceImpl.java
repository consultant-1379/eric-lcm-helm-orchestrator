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
package com.ericsson.oss.management.lcm.presentation.services.values;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.api.model.ValuesRequestDto;
import com.ericsson.oss.management.lcm.constants.HelmSourceConstants;
import com.ericsson.oss.management.lcm.model.entity.Values;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidFileException;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidInputException;
import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.repositories.ValuesRepository;
import com.ericsson.oss.management.lcm.utils.ValuesFileComposer;
import com.ericsson.oss.management.lcm.utils.validator.ValidationUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InternalStoreServiceImpl implements ValuesService {

    private final ValuesRepository repository;
    private final FileService fileService;
    private final ValuesFileComposer composer;

    @Override
    public String post(String workloadInstanceName, String softwareVersion, Path path) {
        String name = prepareName(workloadInstanceName, softwareVersion);
        byte[] content = fileService.getFileContent(path);
        var values = Values.builder()
                .content(content)
                .name(name)
                .build();
        repository.save(values);
        return values.getId();
    }

    @Override
    public Path retrieve(String workloadInstanceName, String softwareVersion, Path directory) {
        String name = prepareName(workloadInstanceName, softwareVersion);
        var values = repository.findTopByName(name)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Values by name=%s is not found", name)));
        return fileService.createFile(directory, values.getContent(), LocalDateTime.now().getNano() + HelmSourceConstants.VALUES_YAML);
    }

    @Override
    public Path retrieveByVersion(String workloadInstanceName, WorkloadInstanceVersion version, Path directory) {
        String name = prepareName(workloadInstanceName, version.getHelmSourceVersion());
        String valuesVersion = version.getValuesVersion();
        var values = repository.findById(valuesVersion)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Values by name=%s and version=%s is not found", name, valuesVersion)));
        return fileService.createFile(directory, values.getContent(), LocalDateTime.now().getNano() + HelmSourceConstants.VALUES_YAML);
    }

    @Override
    public byte[] getContent(String valuesId) {
        return repository.findById(valuesId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Values with version %s was not found", valuesId)))
                .getContent();
    }

    @Override
    public byte[] updateValues(String valuesId, ValuesRequestDto request, MultipartFile valuesFile) {
        var values = repository.findById(valuesId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Values with version=%s was not found", valuesId)));
        return updateContent(values, valuesFile, request);
    }

    private byte[] updateContent(Values storedValues, MultipartFile values, ValuesRequestDto request) {
        String result;
        if (request != null && request.getAdditionalParameters() != null) {
            String valuesContent = Optional.ofNullable(values)
                    .map(this::getValidatedContent)
                    .orElse(new String(storedValues.getContent(), StandardCharsets.UTF_8));
            result = composer.composeStrings(valuesContent, request.getAdditionalParameters());
        } else if (values != null) {
            result = getValidatedContent(values);
        } else {
            throw new InvalidInputException("Values and additional parameters can't be empty");
        }
        storedValues.setContent(result.getBytes(StandardCharsets.UTF_8));
        repository.save(storedValues);
        return result.getBytes(StandardCharsets.UTF_8);
    }

    private String getValidatedContent(MultipartFile file) {
        try {
            ValidationUtils.validateYamlFile(file);
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new InvalidFileException("Values file for modification is not valid.");
        }
    }

}
