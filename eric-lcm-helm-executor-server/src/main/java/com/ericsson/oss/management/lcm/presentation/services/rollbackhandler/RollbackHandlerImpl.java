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

package com.ericsson.oss.management.lcm.presentation.services.rollbackhandler;

import static com.ericsson.oss.management.lcm.constants.CommandConstants.EXTRACT_ARCHIVE_TIMEOUT;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.ENABLED_ARGUMENT_VALUE_FALSE;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.HELMSOURCE_TGZ;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.ROLLBACK_VALUES_YAML;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.ericsson.oss.management.lcm.presentation.services.operation.OperationService;
import org.springframework.stereotype.Service;

import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.HelmSourceType;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;
import com.ericsson.oss.management.lcm.model.internal.RollbackData;
import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.presentation.services.helper.file.StoreFileHelper;
import com.ericsson.oss.management.lcm.presentation.services.values.ValuesService;
import com.ericsson.oss.management.lcm.presentation.services.version.WorkloadInstanceVersionService;
import com.ericsson.oss.management.lcm.presentation.services.workloadinstance.WorkloadInstanceService;
import com.ericsson.oss.management.lcm.repositories.HelmSourceRepository;
import com.ericsson.oss.management.lcm.utils.HelmfileHandler;
import com.ericsson.oss.management.lcm.utils.command.builder.HelmSourceCommandBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RollbackHandlerImpl implements RollbackHandler {

    private final FileService fileService;
    private final HelmSourceRepository helmSourceRepository;
    private final HelmSourceCommandBuilder commandBuilder;
    private final ValuesService valuesService;
    private final WorkloadInstanceVersionService workloadInstanceVersionService;
    private final WorkloadInstanceService workloadInstanceService;
    private final StoreFileHelper storeFileHelper;
    private final HelmfileHandler helmfileHandler;
    private final OperationService operationService;

    @Override
    public RollbackData prepareRollbackData(WorkloadInstance instance, Operation operation, FilePathDetails paths, Path directory) {
        OperationType type = operation.getType();
        log.info("Performing rollback operation after unsuccessful {}", type);
        var valuesFile = paths.getValuesPath();
        var kubeConfigFile = paths.getKubeConfigPath();

        RollbackData rollbackData;

        if (type == OperationType.UPDATE) {
            rollbackData = buildRollbackDataAfterUpdate(instance, operation, directory, kubeConfigFile);
        } else if (type == OperationType.INSTANTIATE || type == OperationType.REINSTANTIATE) {
            rollbackData = buildRollbackDataAfterInstantiate(instance, operation, directory, valuesFile, kubeConfigFile);
        } else {
            throw new UnsupportedOperationException(String.format("Auto-rollback is not supported for the operation %s. "
                                                                          + "Right now application supports auto-rollback only after UPDATE, "
                                                                          + "INSTANTIATE and REINSTANTIATE", operation.getType()));
        }

        log.info("Rollback command to execute: {}", rollbackData.getCommand());
        return rollbackData;
    }

    private RollbackData buildRollbackDataAfterUpdate(WorkloadInstance instance, Operation operation, Path directory, Path kubeConfigFile) {
        var previousVersion = workloadInstanceVersionService.getVersion(instance);
        var rollbackHelmSource = findHelmSourceByWorkloadInstanceAndVersion(instance, previousVersion.getHelmSourceVersion());
        Path values = valuesService.retrieveByVersion(instance.getWorkloadInstanceName(), previousVersion, directory);
        FilePathDetails rollbackPaths = storeFiles(rollbackHelmSource, directory, values, kubeConfigFile);
        operationService.updateOperationWithRollbackTypeAndProcessingState(operation, rollbackHelmSource.getId());
        return RollbackData.builder()
                .command(commandBuilder.apply(rollbackHelmSource, rollbackPaths, operation.getTimeout(), OperationType.ROLLBACK))
                .paths(rollbackPaths)
                .helmSourceType(rollbackHelmSource.getHelmSourceType())
                .build();
    }

    private RollbackData buildRollbackDataAfterInstantiate(WorkloadInstance instance, Operation operation, Path directory, Path valuesFile,
                                                           Path kubeConfigFile) {
        var rollbackHelmSource = operation.getType() == OperationType.INSTANTIATE ?
                getHelmSourceWhenVersionNotSet(instance) : getHelmSourceByVersion(instance);

        var rollbackPaths = getFilePathDetails(instance, directory, valuesFile, kubeConfigFile, rollbackHelmSource);

        operationService.updateOperationWithRollbackTypeAndProcessingState(operation, rollbackHelmSource.getId());
        return RollbackData.builder()
                .command(commandBuilder.delete(rollbackHelmSource, rollbackPaths))
                .paths(rollbackPaths)
                .helmSourceType(rollbackHelmSource.getHelmSourceType())
                .build();
    }

    private FilePathDetails getFilePathDetails(WorkloadInstance instance, Path directory, Path values, Path kubeConfig, HelmSource helmSource) {
        var rollbackValues = fileService.copyFile(values, directory, ROLLBACK_VALUES_YAML);
        var valuesPath = storeFileHelper.mergeParamsToValues(rollbackValues, instance, ENABLED_ARGUMENT_VALUE_FALSE);
        return storeFiles(helmSource, directory, valuesPath, kubeConfig);
    }

    private FilePathDetails storeFiles(HelmSource rollbackHelmSource, Path directory, Path valuesFile, Path kubeConfigFile) {
        log.info(String.format("Storing files in %s", directory.toString()));
        byte[] storedHelmSource = rollbackHelmSource.getContent();

        var helmPath = fileService.createFile(directory, storedHelmSource, HELMSOURCE_TGZ);

        if (rollbackHelmSource.getHelmSourceType() == HelmSourceType.HELMFILE) {
            fileService.extractArchive(helmPath, EXTRACT_ARCHIVE_TIMEOUT);
        }

        return generatePaths(directory, valuesFile, kubeConfigFile, helmPath);
    }

    private FilePathDetails generatePaths(Path directory, Path valuesFile, Path kubeConfigFile, Path helmPath) {
        var kubeConfigPath = Optional.ofNullable(kubeConfigFile)
                .map(item -> fileService.copyFile(kubeConfigFile, directory, null))
                .orElse(null);

        return FilePathDetails.builder()
                .helmSourcePath(helmPath)
                .valuesPath(valuesFile)
                .kubeConfigPath(kubeConfigPath)
                .build();
    }

    private HelmSource getHelmSourceWhenVersionNotSet(WorkloadInstance instance) {
        List<HelmSource> helmSources = Optional.ofNullable(instance.getHelmSources())
                .orElseGet(() -> workloadInstanceService.get(instance.getWorkloadInstanceId()).getHelmSources());
        return Optional.ofNullable(helmSources)
                .orElseGet(ArrayList::new) //this extra checking we need if helmSources from storage are null
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Helm sources for workloadInstance with id=%s were not found", instance.getWorkloadInstanceId())));
    }

    private HelmSource getHelmSourceByVersion(WorkloadInstance instance) {
        var latestVersion = workloadInstanceVersionService.getVersion(instance);
        return findHelmSourceByWorkloadInstanceAndVersion(instance, latestVersion.getHelmSourceVersion());
    }

    private HelmSource findHelmSourceByWorkloadInstanceAndVersion(WorkloadInstance instance, String version) {
        return helmSourceRepository
                .findByWorkloadInstanceAndHelmSourceVersion(instance, version)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("HelmSource for instance = %s and version = %s was NOT FOUND",
                                      instance.getWorkloadInstanceId(), version)));
    }
}
