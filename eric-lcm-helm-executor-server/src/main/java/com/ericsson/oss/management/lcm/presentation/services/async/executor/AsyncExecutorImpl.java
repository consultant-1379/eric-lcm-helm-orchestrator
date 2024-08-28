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

package com.ericsson.oss.management.lcm.presentation.services.async.executor;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.ericsson.oss.management.lcm.presentation.services.helper.command.CommandExecutorHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.HelmSourceType;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.presentation.services.helper.docker.DockerRegistrySecretHelper;
import com.ericsson.oss.management.lcm.presentation.services.kubernetes.KubernetesService;
import com.ericsson.oss.management.lcm.presentation.services.operation.OperationService;
import com.ericsson.oss.management.lcm.presentation.services.release.ReleaseService;
import com.ericsson.oss.management.lcm.presentation.services.rollbackhandler.RollbackHandler;
import com.ericsson.oss.management.lcm.presentation.services.values.ValuesService;
import com.ericsson.oss.management.lcm.presentation.services.version.WorkloadInstanceVersionService;
import com.ericsson.oss.management.lcm.presentation.services.workloadinstance.WorkloadInstanceService;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncExecutorImpl implements AsyncExecutor {

    private static final String RESULT_OF_EXECUTION_MSG = "Result of execution: {}";
    @Value("${auto-rollback.enabled}")
    private boolean autoRollbackEnabled;
    private final CommandExecutorHelper commandExecutor;
    private final OperationService operationService;
    private final FileService fileService;
    private final RollbackHandler rollbackHandler;
    private final WorkloadInstanceVersionService workloadInstanceVersionService;
    private final ValuesService valuesService;
    private final WorkloadInstanceService workloadInstanceService;
    private final ReleaseService releaseService;
    private final DockerRegistrySecretHelper dockerRegistrySecretHelper;
    private final KubernetesService kubernetesService;

    @Async
    @Override
    public void executeAndUpdateOperation(Operation operation, WorkloadInstance instance, FilePathDetails paths,
                                          HelmSource helmSource, String command) {
        var result = new CommandResponse();
        try {
            result = executeCommand(operation, command);
            postValuesAndUpdateVersion(helmSource, instance, paths.getValuesPath(), operation);
            if (result.getExitCode() == 0) {
                log.info(RESULT_OF_EXECUTION_MSG, result.getOutput());
                result = releaseService.handleOrphanedReleases(operation, instance, paths, helmSource.getHelmSourceType(), result);
            } else {
                log.error("Process failed: {}.\nPod details: {}", result.getOutput(),
                        kubernetesService.getLogs(instance.getNamespace(), instance.getWorkloadInstanceName(), paths.getKubeConfigPath()));
                result = performAutoRollbackIfRequired(instance, operation, paths, result);
            }
        } catch (Exception e) {
            updateResponse(result, e.getMessage());
        }
        var updatedOperation = operationService.updateWithCommandResponse(operation, result);
        if (updatedOperation.getType() == OperationType.ROLLBACK && instance.getOperations() == null) {
            byte[] kubeConfig = fileService.getFileContentIfPresent(paths.getKubeConfigPath());
            dockerRegistrySecretHelper.deleteSecret(instance, kubeConfig);
        }
        fileService.deleteDirectory(paths.getValuesPath().getParent());
    }

    @Async
    @Override
    public void executeAndUpdateOperationForTerminate(Operation operation, final FilePathDetails paths, final String command,
                                                      final WorkloadInstance instance, HelmSourceType helmSourceType,
                                                      boolean deleteNamespace) {
        executeAndUpdate(operation, command, instance, paths.getKubeConfigPath(), helmSourceType);
        byte[] kubeConfig = fileService.getFileContentIfPresent(paths.getKubeConfigPath());
        dockerRegistrySecretHelper.deleteSecret(instance, kubeConfig);
        deleteTempDirAndNamespaceIfNeeded(deleteNamespace, instance, paths, helmSourceType);
    }

    @Async
    @Override
    public void executeAndUpdateOperationForRollback(Operation operation, final FilePathDetails paths,
                                                     final String command, WorkloadInstanceVersion version,
                                                     HelmSourceType helmSourceType) {
        var result = new CommandResponse();
        try {
            result = executeCommand(operation, command);
            log.info(RESULT_OF_EXECUTION_MSG, result.getOutput());
            if (result.getExitCode() == 0) {
                workloadInstanceService.updateVersion(version.getWorkloadInstance(), version);
                result = releaseService.handleOrphanedReleases(operation, version.getWorkloadInstance(), paths, helmSourceType, result);
            }
        } catch (Exception e) {
            updateResponse(result, e.getMessage());
        }
        operationService.updateWithCommandResponse(operation, result);
        fileService.deleteDirectory(paths.getHelmSourcePath().getParent());
    }

    private CommandResponse performAutoRollbackIfRequired(WorkloadInstance instance, Operation operation,
                                                          FilePathDetails paths, CommandResponse result) {
        if (autoRollbackEnabled) {
            CommandResponse rollbackResponse = executeRollback(instance, operation, paths);
            return mergeRollbackResponses(result, rollbackResponse, operation);
        } else {
            return result;
        }
    }

    private CommandResponse executeRollback(WorkloadInstance instance, Operation operation, FilePathDetails paths) {
        Path directory = fileService.createDirectory();
        OperationType initialType = operation.getType();
        var rollbackData = rollbackHandler.prepareRollbackData(instance, operation, paths, directory);

        CommandResponse result = executeCommand(operation, rollbackData.getCommand());
        log.info(RESULT_OF_EXECUTION_MSG, result.getOutput());
        if (initialType == OperationType.INSTANTIATE && result.getExitCode() != 0) {
            updateResponse(result, "autoRollback failed after failed INSTANTIATE operation, you can delete instance" +
                    " and try again");
        }
        var handledResult = releaseService.handleOrphanedReleases(operation, instance,
                rollbackData.getPaths(), rollbackData.getHelmSourceType(), result);
        fileService.deleteDirectory(directory);
        return handledResult;
    }

    private void executeAndUpdate(final Operation operation, final String command, final WorkloadInstance instance,
                                                    Path kubeConfigPath, HelmSourceType helmSourceType) {
        var result = new CommandResponse();
        try {
            result = executeCommand(operation, command);
            log.info(RESULT_OF_EXECUTION_MSG, result.getOutput());
            if (helmSourceType == HelmSourceType.INTEGRATION_CHART && result.getExitCode() != 0) {
                kubernetesService.cleanResourcesByNamespaceAndInstanceName(instance.getNamespace(),
                        instance.getWorkloadInstanceName(), kubeConfigPath);
            }
        } catch (Exception e) {
            log.error("Process failed: {}.\nPod details: {}", result.getOutput(),
                    kubernetesService.getLogs(instance.getNamespace(), instance.getWorkloadInstanceName(), kubeConfigPath), e);
            updateResponse(result, e.getMessage());
        }
        operationService.updateWithCommandResponse(operation, result);
        instance.setLatestOperationId(operation.getId());
        workloadInstanceService.update(instance);
    }

    private CommandResponse executeCommand(Operation operation, String command) {
        return commandExecutor.execute(command, operation.getTimeout());
    }

    private void postValuesAndUpdateVersion(HelmSource helmSource, WorkloadInstance instance, Path values, Operation operation) {
        String helmSourceVersion = helmSource.getHelmSourceVersion();
        String valuesVersion = null;
        if (helmSource.isValuesRefreshed()) {
            valuesVersion = valuesService.post(instance.getWorkloadInstanceName(), helmSourceVersion, values);
        }
        setVersionIfRequired(operation, instance, valuesVersion, helmSourceVersion);
    }

    private void setVersionIfRequired(Operation operation, WorkloadInstance instance, String valuesVersion, String helmSourceVersion) {
        OperationType type = operation.getType();
        if (type == OperationType.INSTANTIATE || type == OperationType.UPDATE) {
            setVersion(instance, valuesVersion, helmSourceVersion);
        }
    }

    private void setVersion(WorkloadInstance instance, String valuesVersion, String helmSourceVersion) {
        var workloadInstanceVersion = workloadInstanceVersionService
                .createVersion(instance, valuesVersion, helmSourceVersion);
        workloadInstanceService.updateVersion(instance, workloadInstanceVersion);
    }

    private CommandResponse mergeRollbackResponses(CommandResponse mainResponse, CommandResponse rollbackResponse,
                                                   Operation operation) {
        var failedResponseHeader = "Failed response after main operation:\n\n";
        var rollbackResponseHeader = "\n\nResponse from rollback operation:\n\n";
        var combinedOutput = new StringBuilder()
                .append(failedResponseHeader)
                .append(mainResponse.getOutput())
                .append(rollbackResponseHeader)
                .append(rollbackResponse.getOutput())
                .toString();
        rollbackResponse.setOutput(combinedOutput);
        operation.setOutput(combinedOutput);
        return rollbackResponse;
    }

    private void updateResponse(CommandResponse response, String message) {
        String output = Optional.ofNullable(response.getOutput())
                .map(item -> item + "\nApplication error:\n" + message)
                .orElse(message);
        response.setOutput(output);
    }

    private void deleteTempDirAndNamespaceIfNeeded(boolean deleteNamespace, WorkloadInstance workloadInstance, FilePathDetails paths,
                                          HelmSourceType helmSourceType) {
        var executor = Executors.newSingleThreadScheduledExecutor();
        if (helmSourceType == HelmSourceType.HELMFILE && deleteNamespace) {
            executor.schedule(() -> cleanupResources(true, workloadInstance, paths), 40, TimeUnit.SECONDS);
            executor.shutdown();
        } else {
            cleanupResources(deleteNamespace, workloadInstance, paths);
        }
    }

    private void cleanupResources(boolean deleteNamespace, WorkloadInstance workloadInstance, FilePathDetails paths) {
        if (validateConditionForNamespaceDeletion(deleteNamespace, workloadInstance, paths.getKubeConfigPath())) {
            log.info("Deleting the namespace {}", workloadInstance.getNamespace());
            kubernetesService.deleteNamespace(workloadInstance.getNamespace(), paths.getKubeConfigPath());
        }
        fileService.deleteDirectory(paths.getHelmSourcePath().getParent());
    }

    private boolean validateConditionForNamespaceDeletion(boolean deleteNamespace, WorkloadInstance workloadInstance,
                                                          Path kubeConfigPath) {
        return deleteNamespace && workloadInstanceService.validateInstancesForDeletionInNamespace(
                workloadInstance.getNamespace(), workloadInstance.getClusterIdentifier())
                && kubernetesService.getPodsInNamespace(workloadInstance.getNamespace(), kubeConfigPath)
                .isEmpty();
    }
}
