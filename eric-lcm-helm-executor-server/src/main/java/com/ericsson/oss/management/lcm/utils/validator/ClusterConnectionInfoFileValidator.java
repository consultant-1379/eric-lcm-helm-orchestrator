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

import static com.ericsson.oss.management.lcm.constants.ClusterConnectionInfoConstants.CLUSTER_INVALID_FILENAME_MESSAGE;
import static com.ericsson.oss.management.lcm.constants.ClusterConnectionInfoConstants.CLUSTER_FILENAME_EMPTY_MESSAGE;
import static com.ericsson.oss.management.lcm.constants.ClusterConnectionInfoConstants.CURRENTLY_SUPPORTED_IS_TEXT_FORMAT;
import static com.ericsson.oss.management.lcm.constants.ClusterConnectionInfoConstants.NAMESPACE_PROVIDED_ERROR_MESSAGE;
import static com.ericsson.oss.management.lcm.constants.ClusterConnectionInfoConstants.REGEX_FOR_CLUSTER_CONFIG_FILE_NAME;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.model.clusterconnection.Context;
import com.ericsson.oss.management.lcm.model.clusterconnection.ContextData;
import com.ericsson.oss.management.lcm.model.clusterconnection.KubeConfig;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.utils.ClusterConnectionInfoUtils;
import com.google.common.base.Strings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClusterConnectionInfoFileValidator {

    private static final Pattern PATTERN_FOR_CLUSTER_CONFIG_NAME = Pattern.compile(REGEX_FOR_CLUSTER_CONFIG_FILE_NAME);
    private final FileService fileService;

    /**
     * Check cluster config file for errors in file structure.
     *
     * @param clusterConnectionInfoPath - path to kube config file
     */

    public void validate(MultipartFile clusterConnectionInfo, Path clusterConnectionInfoPath) {
        log.info("Validating content of kube config file");
        validateFileName(clusterConnectionInfo);
        validateFileTypeAsPlainText(clusterConnectionInfo);

        byte[] configFile = fileService.getFileContent(clusterConnectionInfoPath);
        validateClusterConfigFileContent(configFile);
    }

    private void validateFileName(final MultipartFile configFile) {
        if (configFile == null) {
            throw new ResourceNotFoundException("Config file not found");
        }
        String originalFilename = configFile.getOriginalFilename();
        log.info("Validating {} name of kube config file", originalFilename);

        if (Strings.isNullOrEmpty(originalFilename)) {
            throw new IllegalArgumentException(CLUSTER_FILENAME_EMPTY_MESSAGE);
        }

        if (!PATTERN_FOR_CLUSTER_CONFIG_NAME.matcher(originalFilename).matches()) {
            throw new IllegalArgumentException(CLUSTER_INVALID_FILENAME_MESSAGE);
        }
    }

    private void validateFileTypeAsPlainText(MultipartFile configFile) {
        if (configFile == null) {
            throw new ResourceNotFoundException("Config file not found");
        }
        String contentType = configFile.getContentType();
        if (!Objects.equals(contentType, MediaType.TEXT_PLAIN.toString())) {
            log.error("File content type was {}, expected to be {}", contentType, MediaType.TEXT_PLAIN);
            throw new IllegalArgumentException(CURRENTLY_SUPPORTED_IS_TEXT_FORMAT);
        }
    }

    private void validateClusterConfigFileContent(byte[] configFile) {
        KubeConfig kubeConfig = ClusterConnectionInfoUtils.getKubeConfig(configFile);
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<KubeConfig>> violations = validator.validate(kubeConfig);
        if (!CollectionUtils.isEmpty(violations)) {
            throw new ConstraintViolationException(violations);
        }

        List<Context> contexts = kubeConfig.getContexts();
        validateNamespaceNotPresentInContext(contexts);
    }

    private void validateNamespaceNotPresentInContext(List<Context> allContext) {
        allContext.stream()
                .map(Context::getContextData)
                .map(ContextData::getNamespace)
                .filter(namespace -> !Strings.isNullOrEmpty(namespace))
                .findFirst()
                .ifPresent(namespace -> {
                    throw new IllegalArgumentException(NAMESPACE_PROVIDED_ERROR_MESSAGE);
                });
    }
}
