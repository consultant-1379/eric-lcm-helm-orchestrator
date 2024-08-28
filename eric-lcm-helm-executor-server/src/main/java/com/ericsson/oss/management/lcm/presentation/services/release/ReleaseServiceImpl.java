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

package com.ericsson.oss.management.lcm.presentation.services.release;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ericsson.oss.management.lcm.model.entity.Release;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.HelmSourceType;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;
import com.ericsson.oss.management.lcm.presentation.services.helper.command.CommandExecutorHelper;
import com.ericsson.oss.management.lcm.repositories.ReleaseRepository;
import com.ericsson.oss.management.lcm.utils.ReleaseParser;
import com.ericsson.oss.management.lcm.utils.command.builder.HelmSourceCommandBuilder;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReleaseServiceImpl implements ReleaseService {

    private final HelmSourceCommandBuilder builder;
    private final CommandExecutorHelper commandExecutor;
    private final ReleaseParser releaseParser;
    private final ReleaseRepository repository;

    @Value("${operation.timeout}")
    private int timeout;

    @Override
    public CommandResponse extractAndSaveReleases(FilePathDetails paths, WorkloadInstance instance) {
        CommandResponse response = extractReleases(paths, instance.getNamespace());
        if (response.getExitCode() == 0) {
            List<Release> releases = releaseParser.parse(response.getOutput());
            releases.forEach(item -> item.setWorkloadInstance(instance));
            repository.saveAll(releases);
        } else {
            log.info("Helmfile list command failed with msg {}", response.getOutput());
        }
        return response;
    }

    @Override
    public CommandResponse handleOrphanedReleases(Operation operation, WorkloadInstance instance, FilePathDetails paths,
                                       HelmSourceType helmSourceType, CommandResponse result) {
        if (helmSourceType == HelmSourceType.HELMFILE) {
            CommandResponse handlingReleasesResponse = handleReleases(operation, paths, instance);
            mergeResponses(result, handlingReleasesResponse, operation);
        }
        return result;
    }

    @Override
    public List<Release> getByWorkloadInstance(WorkloadInstance instance) {
        return repository.findByWorkloadInstance(instance);
    }

    @Override
    public void deleteReleasesByWorkloadInstance(WorkloadInstance instance) {
        repository.deleteAllByWorkloadInstance(instance);
    }

    private void mergeResponses(CommandResponse mainResponse, CommandResponse extraResponse, Operation operation) {
        if (extraResponse.getExitCode() != 0) {
            var dividingLine = "\nHandling orphaned releases response:\n";
            String combinedOutput = mainResponse.getOutput() + dividingLine + extraResponse.getOutput();
            mainResponse.setOutput(combinedOutput);
            operation.setOutput(mainResponse.getOutput());
        }
    }

    private CommandResponse handleReleases(FilePathDetails paths, WorkloadInstance instance) {
        CommandResponse listResponse = extractReleases(paths, instance.getNamespace());
        return listResponse.getExitCode() != 0
                ? listResponse
                : deleteReleasesIfRequired(listResponse, instance, paths.getKubeConfigPath());
    }

    private CommandResponse handleReleases(Operation operation, FilePathDetails paths, WorkloadInstance instance) {
        OperationType type = operation.getType();
        return type == OperationType.INSTANTIATE || type == OperationType.REINSTANTIATE
                ? extractAndSaveReleases(paths, instance)
                : handleReleases(paths, instance);
    }

    private CommandResponse extractReleases(FilePathDetails paths, String namespace) {
        String listCommand = builder.buildListCommand(paths, namespace);
        return commandExecutor.executeWithRetry(listCommand, timeout);
    }

    private List<Release> getOrphanedReleases(List<Release> savedReleases, List<Release> deployedReleases) {
        Map<String, Release> orderedReleases = orderByName(deployedReleases);
        return savedReleases.stream()
                .filter(item -> !orderedReleases.containsKey(item.getName()))
                .filter(Release::isEnabled)
                .collect(Collectors.toList());
    }

    private Map<String, Release> orderByName(List<Release> releases) {
        return releases.stream().collect(Collectors.toMap(Release::getName, Function.identity()));
    }

    private CommandResponse deleteReleasesIfRequired(CommandResponse listResponse, WorkloadInstance instance, Path kubeConfigPath) {
        List<Release> deployedReleases = releaseParser.parse(listResponse.getOutput());
        List<Release> storedReleases = getByWorkloadInstance(instance);
        List<Release> orphanedReleases = getOrphanedReleases(storedReleases, deployedReleases);
        CommandResponse response = listResponse;
        if (!orphanedReleases.isEmpty()) {
            String deleteCommand = builder.deleteReleases(orphanedReleases, instance.getNamespace(), kubeConfigPath);
            CommandResponse deleteResponse = commandExecutor.executeWithRetry(deleteCommand, timeout);
            if (deleteResponse.getExitCode() == 0) {
                repository.deleteAll(orphanedReleases);
                saveOrUpdateReleases(storedReleases, deployedReleases, instance);
            }
            response = deleteResponse;
        } else {
            saveOrUpdateReleases(storedReleases, deployedReleases, instance);
        }
        return response;
    }

    private void saveOrUpdateReleases(List<Release> storedReleases, List<Release> deployedReleases,
                                 WorkloadInstance instance) {
        Map<String, Release> orderedReleases = orderByName(storedReleases);
        List<Release> releasesToSave = deployedReleases.stream()
                .map(item -> getOrUpdate(orderedReleases, item, instance))
                .collect(Collectors.toList());
        repository.saveAll(releasesToSave);
    }

    private Release getOrUpdate(Map<String, Release> orderedReleases, Release release, WorkloadInstance instance) {
        var storedRelease = orderedReleases.get(release.getName());
        if (storedRelease == null) {
            storedRelease = release;
            storedRelease.setWorkloadInstance(instance);
        } else {
            storedRelease.setEnabled(release.isEnabled());
        }
        return storedRelease;
    }

}
