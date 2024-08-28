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
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.OUTPUT_DIR;
import static com.ericsson.oss.management.lcm.constants.HelmfileContentConstants.HELMFILE_BASIC_VERSION;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import com.ericsson.oss.management.lcm.presentation.mappers.HelmfileMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithChartsRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithURLRequestDto;
import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidInputException;
import com.ericsson.oss.management.lcm.presentation.services.clusterconnectioninfo.ClusterConnectionInfoService;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.presentation.services.helmsource.HelmSourceService;
import com.ericsson.oss.management.lcm.presentation.services.helper.docker.DockerRegistrySecretHelper;
import com.ericsson.oss.management.lcm.presentation.services.helper.file.StoreFileHelper;
import com.ericsson.oss.management.lcm.presentation.services.helper.lcm.LcmHelper;
import com.ericsson.oss.management.lcm.presentation.services.secretsmanagement.SecretsManagement;
import com.ericsson.oss.management.lcm.presentation.services.workloadinstance.WorkloadInstanceService;
import com.ericsson.oss.management.lcm.utils.HelmfileHandler;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressTLS;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstantiateServiceImpl implements InstantiateService {

    @Value("${operation.timeout}")
    private int operationTimeout;

    @Value("${management.certificates.enrollment.enabled}")
    private boolean certificatesEnrollmentEnabled;

    private final FileService fileService;
    private final WorkloadInstanceService workloadInstanceService;
    private final HelmSourceService helmSourceService;
    private final StoreFileHelper storeFileHelper;
    private final DockerRegistrySecretHelper dockerRegistrySecretHelper;
    private final HelmfileMapper helmfileMapper;
    private final HelmfileHandler helmfileHandler;
    private final LcmHelper lcmHelper;
    private final SecretsManagement secretsManagement;
    private final ClusterConnectionInfoService clusterConnectionInfoService;

    @Override
    public WorkloadInstance instantiate(WorkloadInstance instance, MultipartFile helmSourceFile, MultipartFile clusterConnectionInfo,
                                        MultipartFile values, int timeout) {
        var helmPath = prepareHelmPath(helmSourceFile);
        var helmSourceType = helmSourceService.resolveHelmSourceType(helmPath);
        var helmSource = helmSourceService.create(helmPath, instance, helmSourceType);
        FilePathDetails paths = prepareFiles(instance, helmPath, values, clusterConnectionInfo, helmSource);
        dockerRegistrySecretHelper.createSecret(instance, paths.getKubeConfigPath());
        manageSecrets(helmPath, instance.getNamespace(), paths.getKubeConfigPath());

        instance.setClusterIdentifier(clusterConnectionInfoService.resolveClusterIdentifier(paths.getKubeConfigPath()));
        executeHelmSource(instance, timeout, helmSource, paths);
        return instance;
    }

    @Override
    public WorkloadInstance instantiate(WorkloadInstanceWithChartsRequestDto requestDto, WorkloadInstance instance, MultipartFile values,
                                        MultipartFile clusterConnectionInfo) {
        var helmPath = buildHelmfile(requestDto, fileService.createDirectory());
        var helmSourceType = helmSourceService.resolveHelmSourceType(helmPath);
        var helmSource = helmSourceService.create(helmPath, instance, helmSourceType);
        FilePathDetails paths = prepareFiles(instance, helmPath, values, clusterConnectionInfo, helmSource);
        dockerRegistrySecretHelper.createSecret(instance, paths.getKubeConfigPath());
        int timeout = Optional.ofNullable(requestDto.getTimeout()).orElse(operationTimeout);
        manageSecrets(helmPath, instance.getNamespace(), paths.getKubeConfigPath());

        instance.setClusterIdentifier(clusterConnectionInfoService.resolveClusterIdentifier(paths.getKubeConfigPath()));
        executeHelmSource(instance, timeout, helmSource, paths);
        return instance;
    }

    @Override
    public WorkloadInstance instantiate(WorkloadInstanceWithURLRequestDto requestDto, Boolean isUrlToHelmRegistry, WorkloadInstance instance,
                                        MultipartFile values,
                                        MultipartFile clusterConnectionInfo) {
        var helmPath = helmSourceService.downloadHelmSource(requestDto.getUrl(), isUrlToHelmRegistry == null || isUrlToHelmRegistry);
        var helmSourceType = helmSourceService.resolveHelmSourceType(helmPath);
        var helmSource = helmSourceService.create(helmPath, instance, helmSourceType);
        FilePathDetails paths = prepareFilesFromDirectory(instance, helmPath, values, clusterConnectionInfo, helmSource);
        dockerRegistrySecretHelper.createSecret(instance, paths.getKubeConfigPath());
        int timeout = Optional.ofNullable(requestDto.getTimeout()).orElse(operationTimeout);
        manageSecrets(helmPath, instance.getNamespace(), paths.getKubeConfigPath());

        instance.setClusterIdentifier(clusterConnectionInfoService.resolveClusterIdentifier(paths.getKubeConfigPath()));
        executeHelmSource(instance, timeout, helmSource, paths);
        return instance;
    }

    private FilePathDetails prepareFiles(WorkloadInstance instance, Path helmPath, MultipartFile values,
                                         MultipartFile clusterConnectionInfo, HelmSource helmSource) {
        FilePathDetails valuesPaths = storeFileHelper.getValuesPath(helmPath.getParent(), values, instance, helmSource, ENABLED_ARGUMENT_VALUE_TRUE,
                                                                    instance.getAdditionalParameters() != null);

        var kubeConfigPath = storeFileHelper.getKubeConfigPath(helmPath.getParent(), instance, clusterConnectionInfo);
        helmSourceService.verifyHelmSource(valuesPaths.getValuesPath(), helmPath, helmSource.getHelmSourceType());
        return lcmHelper.preparePaths(helmPath, valuesPaths.getValuesPath(), kubeConfigPath);
    }

    private FilePathDetails prepareFilesFromDirectory(WorkloadInstance instance, Path helmPath, MultipartFile values,
                                                      MultipartFile clusterConnectionInfo, HelmSource helmSource) {
        FilePathDetails valuesPaths = storeFileHelper
                .getValuesPathFromDirectory(helmPath.getParent(), values, instance, helmSource, ENABLED_ARGUMENT_VALUE_TRUE);

        var kubeConfigPath = storeFileHelper.getKubeConfigPath(helmPath.getParent(), instance, clusterConnectionInfo);
        helmSourceService.verifyHelmSource(valuesPaths.getValuesPath(), helmPath, helmSource.getHelmSourceType());
        return lcmHelper.preparePaths(helmPath, valuesPaths.getValuesPath(), kubeConfigPath);
    }

    private Path prepareHelmPath(MultipartFile helmSourceFile) {
        log.info("Preparing helm path from input resource");
        Path directory = fileService.createDirectory();
        return Optional
                .ofNullable(helmSourceFile)
                .map(file -> fileService.storeFileIn(directory, helmSourceFile, helmSourceFile.getOriginalFilename()))
                .orElseGet(() -> {
                    fileService.deleteDirectory(directory);
                    throw new InvalidInputException("The helmsource.tgz is missing from the request");
                });
    }

    private void executeHelmSource(WorkloadInstance instance, int timeout, HelmSource helmSource,
                                   FilePathDetails paths) {
        log.info("Executing helm source {}", helmSource);
        var operation = helmSourceService.executeHelmSource(helmSource, timeout, OperationType.INSTANTIATE, paths, null);
        lcmHelper.setNewHelmSourceToInstance(instance, helmSource);
        instance.setLatestOperationId(operation.getId());
        workloadInstanceService.update(instance);
    }

    private Path buildHelmfile(WorkloadInstanceWithChartsRequestDto requestDto, Path directory) {
        log.info("Building helmfile from WorkloadInstanceWithChartsRequestDto with a name {}",
                requestDto.getWorkloadInstanceName());
        var helmfileData = helmfileMapper.toHelmfileData(requestDto);
        var helmPath = helmfileHandler.convertHelmfile(helmfileData, HELMFILE_BASIC_VERSION);
        fileService.copyDirectoryContents(helmPath.getParent(), directory);
        return helmPath;
    }

    private void manageSecrets(Path helmPath, String namespace, Path kubeConfig) {
        if (certificatesEnrollmentEnabled) {
            log.info("Creating or updating secrets in namespace {}", namespace);
            List<IngressTLS> ingressTLSList = secretsManagement.getIngressTLSFromHelmSource(helmPath);
            List<Secret> secretList = secretsManagement.createSecretsWithTLS(ingressTLSList, namespace);
            secretsManagement.createOrUpdateSecretsInNamespace(secretList, namespace, kubeConfig);
        }
        fileService.deleteDirectoryIfExists(Paths.get(helmPath.getParent() + OUTPUT_DIR));
    }
}

