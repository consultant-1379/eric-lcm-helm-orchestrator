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

package com.ericsson.oss.management.lcm.presentation.services.coordinator.operations;

import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.ENABLED_ARGUMENT_VALUE_TRUE;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.HELMSOURCE_TGZ;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.OUTPUT_DIR;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithURLPutRequestDto;
import com.ericsson.oss.management.lcm.presentation.mappers.HelmfileMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.api.model.WorkloadInstancePutRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithChartsPutRequestDto;
import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.HelmSourceType;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;
import com.ericsson.oss.management.lcm.presentation.services.clusterconnectioninfo.ClusterConnectionInfoService;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.presentation.services.helmsource.HelmSourceService;
import com.ericsson.oss.management.lcm.presentation.services.helper.docker.DockerRegistrySecretHelper;
import com.ericsson.oss.management.lcm.presentation.services.helper.file.StoreFileHelper;
import com.ericsson.oss.management.lcm.presentation.services.helper.lcm.LcmHelper;
import com.ericsson.oss.management.lcm.presentation.services.operation.OperationService;
import com.ericsson.oss.management.lcm.presentation.services.version.WorkloadInstanceVersionService;
import com.ericsson.oss.management.lcm.presentation.services.workloadinstance.WorkloadInstanceService;
import com.ericsson.oss.management.lcm.utils.HelmfileHandler;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateServiceImpl implements UpdateService {

    @Value("${operation.timeout}")
    private int operationTimeout;

    private final FileService fileService;
    private final WorkloadInstanceVersionService workloadInstanceVersionService;
    private final HelmSourceService helmSourceService;
    private final StoreFileHelper storeFileHelper;
    private final DockerRegistrySecretHelper dockerRegistrySecretHelper;
    private final WorkloadInstanceService workloadInstanceService;
    private final LcmHelper lcmHelper;
    private final OperationService operationService;
    private final HelmfileMapper helmfileMapper;
    private final HelmfileHandler converter;
    private final ClusterConnectionInfoService clusterConnectionInfoService;

    @Override
    public WorkloadInstance update(WorkloadInstance instance, MultipartFile helmSourceFile,
                                   WorkloadInstancePutRequestDto requestDto, MultipartFile values, MultipartFile clusterConnectionInfo) {
        var type = defineOperationType(instance, requestDto, helmSourceFile, values);
        Path directory = fileService.createDirectory();
        HelmSource helmSource;
        Path helmPath;

        if (helmSourceFile == null || helmSourceFile.isEmpty()) {
            helmSource = extractHelmSource(instance);
            helmPath = fileService.createFile(directory, helmSource.getContent(), HELMSOURCE_TGZ);
            helmSourceService.extractArchiveForHelmfile(helmSource, helmPath);
        } else {
            helmPath = fileService.storeFileIn(directory, helmSourceFile, helmSourceFile.getOriginalFilename());
            var helmSourceType = helmSourceService.resolveHelmSourceType(helmPath);
            helmSource = helmSourceService.create(helmPath, instance, helmSourceType);
        }

        boolean hasAdditionalParams = requestDto != null && requestDto.getAdditionalParameters() != null;
        var filePathDetails = storeFileHelper.getValuesPath(directory, values, instance, helmSource, ENABLED_ARGUMENT_VALUE_TRUE,
                                                                        hasAdditionalParams);
        var kubeConfigPath = storeFileHelper.getKubeConfigPath(directory, instance, clusterConnectionInfo);
        clusterConnectionInfoService.verifyClusterIdentifier(instance, kubeConfigPath);
        if (helmSourceFile != null) {
            helmSourceService.verifyHelmSource(filePathDetails.getValuesPath(), helmPath, helmSource.getHelmSourceType());
        }
        fileService.deleteDirectoryIfExists(Paths.get(directory + OUTPUT_DIR));
        dockerRegistrySecretHelper.createSecret(instance, kubeConfigPath);
        FilePathDetails paths = lcmHelper.preparePaths(helmPath,
                                                       filePathDetails.getValuesPath(),
                                                       kubeConfigPath);
        int timeout = (Objects.nonNull(requestDto)) && Objects.nonNull(requestDto.getTimeout()) ?
                requestDto.getTimeout() : operationTimeout;
        var operation = helmSourceService.executeHelmSource(helmSource, timeout, type, paths, null);
        lcmHelper.setNewHelmSourceToInstance(instance, helmSource);
        instance.setLatestOperationId(operation.getId());
        return workloadInstanceService.update(instance);
    }

    @Override
    public WorkloadInstance update(WorkloadInstance instance, WorkloadInstanceWithChartsPutRequestDto requestDto, MultipartFile values,
                                   MultipartFile clusterConnectionInfo) {
        Path directory = fileService.createDirectory();

        String helmSourceVersion = workloadInstanceVersionService.getVersion(instance).getHelmSourceVersion();
        HelmSource helmSource;
        Path helmPath;
        if (requestDto == null || requestDto.getCharts() == null || requestDto.getCharts().isEmpty()) {
            helmSource = helmSourceService.getByWorkloadInstanceAndVersion(instance, helmSourceVersion);
            helmPath = fileService.createFile(directory, helmSource.getContent(), HELMSOURCE_TGZ);
            helmSourceService.extractArchiveForHelmfile(helmSource, helmPath);
        } else {
            helmSourceVersion = incrementVersion(helmSourceVersion);
            helmPath = buildHelmfile(requestDto, directory, helmSourceVersion);
            helmSource = helmSourceService.create(helmPath, instance, HelmSourceType.HELMFILE);
        }
        boolean hasAdditionalParams = requestDto != null && requestDto.getAdditionalParameters() != null;

        var valuesPath = storeFileHelper.getValuesPath(directory, values, instance, helmSource, ENABLED_ARGUMENT_VALUE_TRUE,
                                                        hasAdditionalParams).getValuesPath();
        var kubeConfigPath = storeFileHelper.getKubeConfigPath(directory, instance, clusterConnectionInfo);
        clusterConnectionInfoService.verifyClusterIdentifier(instance, kubeConfigPath);
        helmSourceService.verifyHelmfile(valuesPath);
        fileService.deleteDirectoryIfExists(Paths.get(directory + OUTPUT_DIR));
        dockerRegistrySecretHelper.createSecret(instance, kubeConfigPath);

        FilePathDetails paths = lcmHelper.preparePaths(helmPath, valuesPath, kubeConfigPath);

        var type = defineOperationType(instance, requestDto, values);
        int timeout = (Objects.nonNull(requestDto)) && Objects.nonNull(requestDto.getTimeout()) ?
                requestDto.getTimeout() : operationTimeout;
        var operation = helmSourceService.executeHelmSource(helmSource, timeout, type, paths, null);
        lcmHelper.setNewHelmSourceToInstance(instance, helmSource);
        instance.setLatestOperationId(operation.getId());
        return workloadInstanceService.update(instance);
    }

    @Override
    public WorkloadInstance update(WorkloadInstance instance, Boolean isUrlToHelmRegistry, WorkloadInstanceWithURLPutRequestDto requestDto,
                                   MultipartFile values, MultipartFile clusterConnectionInfo) {
        Path directory = fileService.createDirectory();
        String helmSourceVersion = workloadInstanceVersionService.getVersion(instance).getHelmSourceVersion();
        HelmSource helmSource;
        Path helmPath;
        if (requestDto == null || requestDto.getUrl() == null || requestDto.getUrl().isEmpty()) {
            helmSource = helmSourceService.getByWorkloadInstanceAndVersion(instance, helmSourceVersion);
            helmPath = fileService.createFile(directory, helmSource.getContent(), HELMSOURCE_TGZ);
            helmSourceService.extractArchiveForHelmfile(helmSource, helmPath);
        } else {
            helmPath = helmSourceService.downloadHelmSource(requestDto.getUrl(), isUrlToHelmRegistry);
            var helmSourceType = helmSourceService.resolveHelmSourceType(helmPath);
            helmSource = helmSourceService.create(helmPath, instance, helmSourceType);
        }

        boolean hasAdditionalParams = requestDto != null && requestDto.getAdditionalParameters() != null;
        var valuesPath = storeFileHelper.getValuesPath(directory, values, instance, helmSource, ENABLED_ARGUMENT_VALUE_TRUE,
                hasAdditionalParams).getValuesPath();

        var kubeConfigPath = storeFileHelper.getKubeConfigPath(directory, instance, clusterConnectionInfo);
        clusterConnectionInfoService.verifyClusterIdentifier(instance, kubeConfigPath);
        if (helmSource != null) {
            helmSourceService.verifyHelmSource(valuesPath, helmPath, helmSource.getHelmSourceType());
        }
        fileService.deleteDirectoryIfExists(Paths.get(directory + OUTPUT_DIR));
        dockerRegistrySecretHelper.createSecret(instance, kubeConfigPath);

        FilePathDetails paths = lcmHelper.preparePaths(helmPath, valuesPath, kubeConfigPath);

        var type = defineOperationType(instance, requestDto, values);
        int timeout = (Objects.nonNull(requestDto)) && Objects.nonNull(requestDto.getTimeout()) ?
                requestDto.getTimeout() : operationTimeout;
        var operation = helmSourceService.executeHelmSource(helmSource, timeout, type, paths, null);
        lcmHelper.setNewHelmSourceToInstance(instance, helmSource);
        instance.setLatestOperationId(operation.getId());
        return workloadInstanceService.update(instance);
    }

    private HelmSource extractHelmSource(WorkloadInstance instance) {
        String helmSourceVersion = workloadInstanceVersionService.getVersion(instance).getHelmSourceVersion();
        return helmSourceService.getByWorkloadInstanceAndVersion(instance, helmSourceVersion);
    }

    private OperationType defineOperationType(WorkloadInstance instance, WorkloadInstancePutRequestDto requestDto,
                                              MultipartFile helmSourceFile, MultipartFile values) {
        OperationType previousType = operationService.get(instance.getLatestOperationId()).getType();
        return previousType == OperationType.TERMINATE && isRequestEmpty(requestDto, helmSourceFile, values) ?
                OperationType.REINSTANTIATE : OperationType.UPDATE;
    }

    private boolean isRequestEmpty(WorkloadInstancePutRequestDto requestDto, MultipartFile helmSourceFile, MultipartFile values) {
        return requestDto == null && helmSourceFile == null && values == null;
    }

    private OperationType defineOperationType(WorkloadInstance instance, WorkloadInstanceWithChartsPutRequestDto requestDto,
                                              MultipartFile values) {
        OperationType previousType = operationService.get(instance.getLatestOperationId()).getType();
        return previousType == OperationType.TERMINATE && isRequestEmpty(requestDto, values) ?
                OperationType.REINSTANTIATE : OperationType.UPDATE;
    }

    private OperationType defineOperationType(WorkloadInstance instance, WorkloadInstanceWithURLPutRequestDto requestDto,
                                              MultipartFile values) {
        OperationType previousType = operationService.get(instance.getLatestOperationId()).getType();
        return previousType == OperationType.TERMINATE && isRequestEmpty(requestDto, values) ?
                OperationType.REINSTANTIATE : OperationType.UPDATE;
    }

    private boolean isRequestEmpty(WorkloadInstanceWithChartsPutRequestDto requestDto, MultipartFile values) {
        return (requestDto == null || requestDto.getCharts() == null && requestDto.getAdditionalParameters() == null)
                && values == null;
    }

    private boolean isRequestEmpty(WorkloadInstanceWithURLPutRequestDto requestDto, MultipartFile values) {
        return (requestDto == null || requestDto.getUrl() == null && requestDto.getAdditionalParameters() == null)
                && values == null;
    }

    private String incrementVersion(String version) {
        int lastDash = version.lastIndexOf("-");
        int newVersion = Integer.parseInt(version.substring(lastDash + 1)) + 1;
        return version.substring(0, lastDash) + "-" + newVersion;
    }

    private Path buildHelmfile(WorkloadInstanceWithChartsPutRequestDto requestDto, Path directory, String version) {
        var helmfileData = helmfileMapper.toHelmfileData(requestDto);
        var helmPath = converter.convertHelmfile(helmfileData, version);
        fileService.copyDirectoryContents(helmPath.getParent(), directory);
        return helmPath;
    }

}