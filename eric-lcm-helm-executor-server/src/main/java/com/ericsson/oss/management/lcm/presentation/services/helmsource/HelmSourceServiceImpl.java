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

package com.ericsson.oss.management.lcm.presentation.services.helmsource;

import static com.ericsson.oss.management.lcm.constants.CommandConstants.CHECK_FILE_PRESENCE_IN_ARCHIVE_TIMEOUT;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.EXTRACT_ARCHIVE_TIMEOUT;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.SPACE;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.CHART_YAML;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.HELMFILE_VERSION_KEY;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.HELMFILE_TGZ;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.HELMFILE_YAML_FILENAME;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.HELM_CHART_VERSION_KEY;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.METADATA_YAML;
import static com.ericsson.oss.management.lcm.model.entity.HelmSourceType.HELMFILE;
import static com.ericsson.oss.management.lcm.model.entity.HelmSourceType.INTEGRATION_CHART;
import static com.ericsson.oss.management.lcm.utils.validator.HelmSourceValidator.validateFilePresence;
import static com.ericsson.oss.management.lcm.utils.validator.HelmSourceValidator.validateMetadataPresence;
import static java.lang.Boolean.TRUE;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidInputException;
import com.ericsson.oss.management.lcm.utils.HttpClientUtils;
import com.ericsson.oss.management.lcm.utils.UrlUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.HelmSourceType;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.OperationState;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidFileException;
import com.ericsson.oss.management.lcm.presentation.exceptions.NotUniqueHelmSourceException;
import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import com.ericsson.oss.management.lcm.presentation.services.async.executor.AsyncExecutor;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.presentation.services.helper.command.CommandExecutorHelper;
import com.ericsson.oss.management.lcm.presentation.services.operation.OperationService;
import com.ericsson.oss.management.lcm.presentation.services.release.ReleaseService;
import com.ericsson.oss.management.lcm.presentation.services.version.WorkloadInstanceVersionService;
import com.ericsson.oss.management.lcm.repositories.HelmSourceRepository;
import com.ericsson.oss.management.lcm.utils.FileUtils;
import com.ericsson.oss.management.lcm.utils.command.builder.HelmSourceCommandBuilder;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Service
@RequiredArgsConstructor
public class HelmSourceServiceImpl implements HelmSourceService {

    private static final String PULL_COMMAND_TEMPLATE = "helm pull %s --username %s --password %s --ca-file %s --cert-file %s --key-file %s -d %s";

    @Value("${operation.timeout}")
    private int operationTimeout;

    @Value("${helmrepo.username}")
    private String username;

    @Value("${helmrepo.password}")
    private String password;

    @Value("${helmrepo.ca-cert}")
    String caCert;

    @Value("${helmrepo.client-cert}")
    String clientCert;

    @Value("${helmrepo.client-key}")
    String clientKey;

    @Value("${security.tls.enabled}")
    boolean tlsEnabled;

    @Value("${auto-rollback.enabled}")
    private boolean autoRollbackEnabled;

    @Value("${security.serviceMesh.enabled}")
    private boolean serviceMeshEnabled;

    private final HelmSourceRepository helmSourceRepository;
    private final FileService fileService;
    private final OperationService operationService;
    private final HelmSourceCommandBuilder commandBuilder;
    private final AsyncExecutor asyncExecutor;
    private final CommandExecutorHelper commandExecutor;
    private final ReleaseService releaseService;
    private final WorkloadInstanceVersionService workloadInstanceVersionService;
    private final HttpClientUtils httpClientUtils;
    private final UrlUtils urlUtils;

    @Override
    public HelmSource create(Path helmSourcePath, WorkloadInstance workloadInstance, HelmSourceType helmSourceType) {
        log.info("Will validate and persist helmfile");

        fileService.extractArchive(helmSourcePath, EXTRACT_ARCHIVE_TIMEOUT);
        String version = getHelmSourceVersion(helmSourcePath, helmSourceType);
        checkHelmSourceVersionIsUnique(workloadInstance, version);

        if (helmSourceType.equals(HELMFILE)) {
            var parentDirectory = helmSourcePath.getParent();
            validateMetadataPresence(parentDirectory);
            validateFilePresence(parentDirectory, HELMFILE_YAML_FILENAME);
        }

        HelmSource result = HelmSource.builder()
                                  .helmSourceType(helmSourceType)
                                  .workloadInstance(workloadInstance)
                                  .content(fileService.getFileContentIfPresent(helmSourcePath))
                                  .created(LocalDateTime.now())
                                  .helmSourceVersion(version)
                                  .build();

        return helmSourceRepository.save(result);
    }

    @Override
    public String getHelmSourceVersion(Path helmPath, HelmSourceType helmSourceType) {
        log.info("Will retrieve helmfileVersion or helmChartVersion.");
        String result = helmSourceType.equals(HELMFILE) ? getHelmfileVersion(helmPath) : getHelmChartVersion(helmPath);
        return result.replace("+", "--");
    }

    @Override
    public HelmSource get(String helmSourceId) {
        return helmSourceRepository.findById(helmSourceId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("HelmSource with id = %s NOT FOUND", helmSourceId)));
    }

    @Override
    public HelmSource get(WorkloadInstance instance) {
        var workloadInstanceVersion = workloadInstanceVersionService.getVersion(instance);
        return getByWorkloadInstanceAndVersion(instance, workloadInstanceVersion.getHelmSourceVersion());
    }

    @Override
    public HelmSource getByWorkloadInstanceAndVersion(WorkloadInstance instance, String helmSourceVersion) {
        return helmSourceRepository.findByWorkloadInstanceAndHelmSourceVersion(instance, helmSourceVersion)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("HelmSource for instance = %s and version = %s was NOT FOUND",
                                      instance.getWorkloadInstanceId(), helmSourceVersion)));
    }

    @Override
    public Operation executeHelmSource(HelmSource helmSource, int timeout, OperationType type, FilePathDetails paths,
                                       WorkloadInstanceVersion version) {
        var workloadInstance = helmSource.getWorkloadInstance();
        var operation = Operation.builder()
                .helmSourceId(helmSource.getId())
                .workloadInstance(workloadInstance)
                .type(type)
                .startTime(LocalDateTime.now())
                .state(OperationState.PROCESSING)
                .timeout(timeout)
                .build();
        operation = operationService.create(operation);

        String command = commandBuilder.apply(helmSource, paths, timeout, type);
        if (type == OperationType.ROLLBACK) {
            asyncExecutor.executeAndUpdateOperationForRollback(operation, paths, command, version, helmSource.getHelmSourceType());
        } else {
            asyncExecutor.executeAndUpdateOperation(operation, workloadInstance, paths, helmSource, command);
        }
        return operation;
    }

    @Override
    public Operation destroyHelmSource(HelmSource helmSource, int timeout, FilePathDetails paths, boolean deleteNamespace) {
        var instance = helmSource.getWorkloadInstance();
        var operation = Operation.builder()
                .helmSourceId(helmSource.getId())
                .workloadInstance(instance)
                .type(OperationType.TERMINATE)
                .startTime(LocalDateTime.now())
                .state(OperationState.PROCESSING)
                .timeout(timeout)
                .build();
        operation = operationService.create(operation);

        if (helmSource.getHelmSourceType() == INTEGRATION_CHART && autoRollbackEnabled) {
            performTerminateForIntegrationChart(helmSource, paths, instance, operation, deleteNamespace);
        } else {
            performExecuteAndUpdateOperationForTerminate(helmSource, paths, operation, instance, deleteNamespace);
        }
        releaseService.deleteReleasesByWorkloadInstance(instance);
        return operation;
    }

    @Override
    public void extractArchiveForHelmfile(HelmSource helmSource, Path helmPath) {
        if (HelmSourceType.HELMFILE.equals(helmSource.getHelmSourceType())) {
            fileService.extractArchive(helmPath, EXTRACT_ARCHIVE_TIMEOUT);
        }
    }

    @Override
    public HelmSourceType resolveHelmSourceType(Path helmSource) {
        if (fileService.checkFilePresenceInArchive(HELMFILE_YAML_FILENAME, helmSource,
                CHECK_FILE_PRESENCE_IN_ARCHIVE_TIMEOUT)) {
            return HELMFILE;
        } else if (fileService.checkFilePresenceInArchive(CHART_YAML, helmSource,
                CHECK_FILE_PRESENCE_IN_ARCHIVE_TIMEOUT)) {
            return INTEGRATION_CHART;
        } else {
            throw new InvalidFileException("Failed during detection helmSource type. Make sure Chart.yaml" +
                    "or hemfile.yaml is present in an archive");
        }
    }

    @Override
    public void verifyHelmfile(Path valuesPath) {
        String command = commandBuilder.verifyHelmfile(valuesPath);
        CommandResponse execute = commandExecutor.executeWithRetry(String.join(SPACE, command), operationTimeout);
        if (execute.getExitCode() != 0) {
            throw new InvalidFileException(String.format("Not valid helmfile was sent. Details: %s", execute.getOutput()));
        }
    }

    @Override
    public void verifyIntegrationChart(Path helmPath) {
        String command = commandBuilder.verifyIntegrationChart(helmPath);
        CommandResponse execute = commandExecutor.executeWithRetry(String.join(SPACE, command), operationTimeout);
        if (execute.getExitCode() != 0) {
            throw new InvalidFileException(String.format("Not valid integration chart was sent. Details: %s", execute.getOutput()));
        }
    }

    @Override
    public void verifyHelmSource(Path valuesPath, Path helmPath, HelmSourceType helmSourceType) {
        if (helmSourceType.equals(INTEGRATION_CHART)) {
            verifyIntegrationChart(helmPath);
        } else {
            verifyHelmfile(valuesPath);
        }
    }

    @Override
    public Path downloadHelmSource(String url, boolean isUrlToHelmRegistry) {
        if (tlsEnabled && !serviceMeshEnabled && isUrlToHelmRegistry) {
            return pullChartByHelm(url);
        }
        try {
            HttpHeaders headers = getAuthHeadersIfRequired(isUrlToHelmRegistry);
            byte[] file = httpClientUtils
                    .executeHttpRequest(headers, url, HttpMethod.GET, null, byte[].class)
                    .getBody();
            return prepareHelmPathFromArray(file);
        } catch (HttpClientErrorException e) {
            throw new ResourceNotFoundException(String.format("Helmfile or integration chart by url = %s was NOT FETCHED. Something went wrong, "
                    + "details: %s", url, e.getMessage()));
        }
    }

    private Path pullChartByHelm(String url) {
        var directory = fileService.createDirectory();
        var urlWithCorrectProtocol = url.contains("http://") ? url.replace("http://", "https://") : url;
        var pullCommand = String.format(PULL_COMMAND_TEMPLATE, urlWithCorrectProtocol, username, password, caCert, clientCert, clientKey,
                directory.toString());
        var commandResponse = commandExecutor.execute(pullCommand, operationTimeout);
        if (commandResponse.getExitCode() != 0) {
            fileService.deleteDirectory(directory);
            throw new ResourceNotFoundException(String.format("Helmfile or integration chart by url = %s was NOT FETCHED. Something went wrong, "
                    + "details: %s", urlWithCorrectProtocol, commandResponse.getOutput()));
        }
        var helmSourceFileName = urlWithCorrectProtocol.substring(urlWithCorrectProtocol.lastIndexOf("/") + 1);
        return directory.resolve(helmSourceFileName);
    }

    private HttpHeaders getAuthHeadersIfRequired(boolean helmChartCredsRequired) {
        HttpHeaders headers = null;
        if (helmChartCredsRequired) {
            headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, urlUtils.authenticationHeader(username, password));
        }
        return headers;
    }

    private Path prepareHelmPathFromArray(byte[] helmSourceFile) {
        log.info("Preparing helm path from helm source byte array");
        Path directory = fileService.createDirectory();
        return Optional
                .ofNullable(helmSourceFile)
                .map(file -> fileService.createFile(directory, helmSourceFile, HELMFILE_TGZ))
                .orElseGet(() -> {
                    fileService.deleteDirectory(directory);
                    throw new InvalidInputException("The helmsource.tgz is missing from the request");
                });
    }

    private String getHelmfileVersion(Path helmSourcePath) {
        Path fileFromTheDirectory = FileUtils.getFileFromTheDirectoryByNamePart(helmSourcePath.getParent(), METADATA_YAML)
                .orElseThrow(() -> new InvalidFileException("Directory should contain metadata.yaml."));
        return FileUtils.getValueByPropertyFromFile(fileFromTheDirectory, HELMFILE_VERSION_KEY);
    }

    private String getHelmChartVersion(Path helmSourcePath) {
        Path fileFromTheDirectory = FileUtils.getFileFromTheDirectoryExcludingChartsDirectory(helmSourcePath.getParent(), CHART_YAML)
                .orElseThrow(() -> new InvalidFileException("Chart.yaml must be present in the root directory"));
        return FileUtils.getValueByPropertyFromFile(fileFromTheDirectory, HELM_CHART_VERSION_KEY);
    }

    private void checkHelmSourceVersionIsUnique(WorkloadInstance instance, String version) {
        Optional<HelmSource> helmSource = helmSourceRepository.findByWorkloadInstanceAndHelmSourceVersion(instance, version);
        if (helmSource.isPresent()) {
            throw new NotUniqueHelmSourceException(String.format("HelmSource with version = %s is already present for this " +
                                                                         "Workload instance = %s, and it must be unique.",
                                                                 version, instance.getWorkloadInstanceName()));
        }
    }

    private void performTerminateForIntegrationChart(HelmSource helmSource, FilePathDetails paths, WorkloadInstance instance,
                                                     Operation operation, boolean deleteNamespace) {
        var commandResponse = performHelmListCommand(instance, paths.getKubeConfigPath());
        boolean isInstanceNotPresentOnCluster = commandResponse.getExitCode() == 0 &&
                !commandResponse.getOutput().contains(instance.getWorkloadInstanceName());
        if (isInstanceNotPresentOnCluster) {
            operation.setState(OperationState.COMPLETED);
            operationService.create(operation);
        } else {
            performExecuteAndUpdateOperationForTerminate(helmSource, paths, operation, instance, deleteNamespace);
        }
    }

    private CommandResponse performHelmListCommand(WorkloadInstance instance, Path kubeConfig) {
        var listCommand = commandBuilder.buildHelmListCommandWithFilterByName(instance.getNamespace(), instance.getWorkloadInstanceName(),
                                                                              kubeConfig);
        return commandExecutor.executeWithRetry(listCommand, operationTimeout);
    }

    private void performExecuteAndUpdateOperationForTerminate(HelmSource helmSource, FilePathDetails paths, Operation operation,
                                                              WorkloadInstance instance, boolean deleteNamespace) {
        String command;
        if (TRUE.equals(deleteNamespace) && helmSource.getHelmSourceType().equals(INTEGRATION_CHART)) {
            command = commandBuilder.deleteCascadeIntegrationChart(helmSource);
        } else {
            command = commandBuilder.delete(helmSource, paths);
        }
        asyncExecutor.executeAndUpdateOperationForTerminate(operation, paths, command, instance,
                                                            helmSource.getHelmSourceType(), deleteNamespace);
    }
}
