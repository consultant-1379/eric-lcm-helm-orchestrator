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

import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.HELMSOURCE_TGZ;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;
import com.ericsson.oss.management.lcm.presentation.services.clusterconnectioninfo.ClusterConnectionInfoService;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.presentation.services.helmsource.HelmSourceService;
import com.ericsson.oss.management.lcm.presentation.services.helper.docker.DockerRegistrySecretHelper;
import com.ericsson.oss.management.lcm.presentation.services.helper.file.StoreFileHelper;
import com.ericsson.oss.management.lcm.presentation.services.helper.lcm.LcmHelper;
import com.ericsson.oss.management.lcm.presentation.services.values.ValuesService;
import com.ericsson.oss.management.lcm.presentation.services.workloadinstance.WorkloadInstanceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RollbackServiceImpl implements RollbackService {

    @Value("${operation.timeout}")
    private int operationTimeout;

    private final FileService fileService;
    private final HelmSourceService helmSourceService;
    private final ValuesService valuesService;
    private final StoreFileHelper storeFileHelper;
    private final DockerRegistrySecretHelper dockerRegistrySecretHelper;
    private final WorkloadInstanceService workloadInstanceService;
    private final LcmHelper lcmHelper;
    private final ClusterConnectionInfoService clusterConnectionInfoService;

    @Override
    public WorkloadInstance rollback(WorkloadInstance instance, WorkloadInstanceVersion version, MultipartFile clusterConnectionInfo) {
        var helmSource = helmSourceService.getByWorkloadInstanceAndVersion(instance, version.getHelmSourceVersion());
        FilePathDetails paths = prepareFiles(instance, version, clusterConnectionInfo, helmSource);
        clusterConnectionInfoService.verifyClusterIdentifier(instance, paths.getKubeConfigPath());
        dockerRegistrySecretHelper.createSecret(instance, paths.getKubeConfigPath());
        var operation = helmSourceService.executeHelmSource(helmSource, operationTimeout, OperationType.ROLLBACK, paths, version);
        instance.setLatestOperationId(operation.getId());
        lcmHelper.setNewHelmSourceToInstance(instance, helmSource);
        workloadInstanceService.update(instance);
        return instance;
    }

    private FilePathDetails prepareFiles(WorkloadInstance instance, WorkloadInstanceVersion version,
                                         MultipartFile clusterConnectionInfo, HelmSource helmSource) {
        Path directory = fileService.createDirectory();
        var helmPath = fileService.createFile(directory, helmSource.getContent(), HELMSOURCE_TGZ);
        helmSourceService.extractArchiveForHelmfile(helmSource, helmPath);
        var valuesPath = valuesService.retrieveByVersion(instance.getWorkloadInstanceName(), version, directory);
        var kubeConfigPath = storeFileHelper.getKubeConfigPath(directory, instance, clusterConnectionInfo);
        return lcmHelper.preparePaths(helmPath, valuesPath, kubeConfigPath);
    }

}
