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

package com.ericsson.oss.management.lcm.utils.command.builder;

import static com.ericsson.oss.management.lcm.constants.CommandConstants.APPLY_ARGUMENT;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.BASE_COMMAND;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.CASCADE_TYPE;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.UNINSTALL_ARGUMENT;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.FILE_ARGUMENT;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.FILTER_ARGUMENT;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.HELMFILE_TIMEOUT_ARGUMENT;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.HELM_COMMAND;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.HELM_TIMEOUT_ARGUMENT;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.INSTALL_ARGUMENT;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.KUBE_CONFIG_VALUE;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.LIST_ARGUMENT;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.NAMESPACE_ARGUMENT;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.NAMESPACE_SELECTOR_ARGUMENT;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.SLASH;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.SPACE;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.STATE_VALUES_FILE_ARGUMENT;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.UPGRADE_ARGUMENT;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.VALUES_ARGUMENT;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.VERIFY_HELMFILE;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.VERIFY_INTEGRATION_CHART;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.WAIT_ARGUMENT;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.HELMFILE_YAML_FILENAME;
import static com.ericsson.oss.management.lcm.model.entity.HelmSourceType.HELMFILE;
import static com.ericsson.oss.management.lcm.utils.FileUtils.getFileFromTheDirectory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.Release;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidFileException;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Setter
class HelmSourceCommandBuilderImpl implements HelmSourceCommandBuilder {

    @Value("${operation.timeout}")
    private int operationTimeout;

    @Override
    public String apply(HelmSource helmSource, FilePathDetails paths, int timeout, OperationType operationType) {
        Path helmSourcePath = paths.getHelmSourcePath();
        Path valuesPath = paths.getValuesPath();
        Path kubeConfigPath = paths.getKubeConfigPath();

        var workloadInstance = helmSource.getWorkloadInstance();

        String command = HELMFILE.equals(helmSource.getHelmSourceType()) ?
                buildApplyCommandForHelmfile(valuesPath, kubeConfigPath, helmSourcePath.getParent(), timeout) :
                buildApplyCommandForIntegrationChart(valuesPath, kubeConfigPath, helmSourcePath, workloadInstance, timeout, operationType);

        log.info("Built command: {}", command);
        return command;
    }

    @Override
    public String delete(HelmSource helmSource, FilePathDetails paths) {
        Path helmSourcePath = paths.getHelmSourcePath();
        Path valuesPath = paths.getValuesPath();
        Path kubeConfigPath = paths.getKubeConfigPath();

        return HELMFILE.equals(helmSource.getHelmSourceType()) ?
                buildApplyCommandForHelmfile(valuesPath, kubeConfigPath, helmSourcePath.getParent(), operationTimeout) :
                buildDeleteCommandForIntegrationChart(helmSource.getWorkloadInstance());
    }

    @Override
    public String deleteCascadeIntegrationChart(HelmSource helmSource) {
        log.info("Building delete command with cascade type for integration chart");
        String timeoutArgument = String.format(HELM_TIMEOUT_ARGUMENT, convertTimeout(operationTimeout));
        WorkloadInstance workloadInstance = helmSource.getWorkloadInstance();

        List<String> destroyList =
                new ArrayList<>(List.of(HELM_COMMAND, UNINSTALL_ARGUMENT, workloadInstance.getWorkloadInstanceName(),
                        WAIT_ARGUMENT, CASCADE_TYPE, timeoutArgument, NAMESPACE_ARGUMENT, workloadInstance.getNamespace()));

        return String.join(SPACE, destroyList);
    }

    @Override
    public String verifyHelmfile(Path valuesPath) {
        Path directory = valuesPath.getParent();
        Optional<Path> helmfile = getFileFromTheDirectory(directory, HELMFILE_YAML_FILENAME);
        String valuesFile = valuesPath.toAbsolutePath().toString();
        if (helmfile.isPresent()) {
            return String.format(VERIFY_HELMFILE, valuesFile, helmfile.get());
        } else {
            throw new InvalidFileException("helmfile.yaml must exist inside helmfile.tgz");
        }
    }

    @Override
    public String verifyIntegrationChart(Path helmPath) {
        Path integrationChart = helmPath.toAbsolutePath();
        if (Files.exists(integrationChart)) {
            return String.format(VERIFY_INTEGRATION_CHART, integrationChart, integrationChart.getParent());
        } else {
            throw new InvalidFileException(String.format("Integration chart %s was not exist", integrationChart));
        }
    }

    @Override
    public String buildListCommand(FilePathDetails paths, String namespace) {
        log.info("Building helmfile list command");
        Path valuesPath = paths.getValuesPath();
        Path kubeConfigPath = paths.getKubeConfigPath();
        Path helmfilePath = getFileFromTheDirectory(valuesPath.getParent(), HELMFILE_YAML_FILENAME)
                .orElseThrow(() -> new InvalidFileException(String.format("Helmfile was not found in the directory %s", valuesPath.getParent())));
        List<String> applyList = new ArrayList<>();
        Optional.ofNullable(kubeConfigPath).map(path -> String.format(KUBE_CONFIG_VALUE, path.toAbsolutePath()))
                .ifPresent(applyList::add);
        applyList.addAll(List.of(BASE_COMMAND, FILE_ARGUMENT, helmfilePath.toAbsolutePath().toString(),
                                  STATE_VALUES_FILE_ARGUMENT, valuesPath.toAbsolutePath().toString(),
                                  String.format(NAMESPACE_SELECTOR_ARGUMENT, namespace), LIST_ARGUMENT));
        return String.join(SPACE, applyList);
    }

    @Override
    public String deleteReleases(List<Release> releases, String namespace, Path kubeConfigPath) {
        log.info("Building helm uninstall command");
        List<String> applyList = new ArrayList<>();
        Optional.ofNullable(kubeConfigPath)
                .map(path -> List.of(String.format(KUBE_CONFIG_VALUE, path.toAbsolutePath())))
                .ifPresent(applyList::addAll);
        String releasesSequence = releases.stream()
                .map(Release::getName)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.joining(SPACE));
        applyList.addAll(List.of(HELM_COMMAND, NAMESPACE_ARGUMENT, namespace, UNINSTALL_ARGUMENT, releasesSequence));
        return String.join(SPACE, applyList);
    }

    @Override
    public String buildHelmListCommandWithFilterByName(String namespace, String workloadInstanceName, Path kubeConfig) {
        log.info("Building helm list command with filter by name: {}", workloadInstanceName);
        List<String> applyList = new ArrayList<>();
        Optional.ofNullable(kubeConfig)
                .map(kubeConfigPath -> String.format(KUBE_CONFIG_VALUE, kubeConfigPath.toAbsolutePath()))
                .ifPresent(applyList::add);
        applyList.addAll(List.of(HELM_COMMAND, LIST_ARGUMENT, FILTER_ARGUMENT, workloadInstanceName, NAMESPACE_ARGUMENT, namespace));
        return String.join(SPACE, applyList);
    }

    private String buildApplyCommandForHelmfile(Path valuesPath, Path kubeConfigPath, Path directory, int timeout) {
        log.info("Building apply command for helmfile");
        List<String> applyList = new ArrayList<>();
        Optional.ofNullable(kubeConfigPath).map(path -> String.format(KUBE_CONFIG_VALUE, path))
                .ifPresent(applyList::add);

        applyList.add(BASE_COMMAND);

        Optional.ofNullable(directory).map(pathToDirectory -> List.of(FILE_ARGUMENT, pathToDirectory + SLASH + HELMFILE_YAML_FILENAME))
                .ifPresent(applyList::addAll);

        applyList.add(String.format(HELMFILE_TIMEOUT_ARGUMENT, convertTimeout(timeout)));
        Optional.ofNullable(valuesPath).map(path -> List.of(STATE_VALUES_FILE_ARGUMENT, path.toString()))
                .ifPresent(applyList::addAll);

        applyList.add(APPLY_ARGUMENT);
        applyList.add(WAIT_ARGUMENT);

        return String.join(SPACE, applyList);
    }

    private String buildApplyCommandForIntegrationChart(Path valuesPath, Path kubeConfigPath, Path directory,
                                                        WorkloadInstance workloadInstance, int timeout, OperationType operationType) {
        log.info("Building apply command for integration chart");

        List<String> applyList = new ArrayList<>();
        Optional.ofNullable(kubeConfigPath).map(path -> String.format(KUBE_CONFIG_VALUE, path))
                .ifPresent(applyList::add);

        String timeoutArgument = String.format(HELM_TIMEOUT_ARGUMENT, convertTimeout(timeout));
        String operation = getOperationToExecute(operationType);
        applyList.addAll(List.of(HELM_COMMAND, operation, WAIT_ARGUMENT, timeoutArgument, NAMESPACE_ARGUMENT,
                                         workloadInstance.getNamespace(), workloadInstance.getWorkloadInstanceName(), directory.toString()));
        Optional.ofNullable(valuesPath).map(path -> List.of(VALUES_ARGUMENT, path.toString()))
                .ifPresent(applyList::addAll);

        return String.join(SPACE, applyList);
    }

    private String buildDeleteCommandForIntegrationChart(WorkloadInstance workloadInstance) {
        log.info("Building delete command for integration chart");

        List<String> destroyList =
                new ArrayList<>(List.of(HELM_COMMAND, UNINSTALL_ARGUMENT, NAMESPACE_ARGUMENT,
                        workloadInstance.getNamespace(), workloadInstance.getWorkloadInstanceName()));

        return String.join(SPACE, destroyList);
    }

    private int convertTimeout(int timeout) {
        log.info("Converting timeout of {} minutes to seconds", timeout);
        return timeout * 60;
    }

    private String getOperationToExecute(OperationType operationType) {
        if (operationType == OperationType.INSTANTIATE || operationType == OperationType.REINSTANTIATE) {
            return INSTALL_ARGUMENT;
        } else {
            return UPGRADE_ARGUMENT;
        }
    }
}
