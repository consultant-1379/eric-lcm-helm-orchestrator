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

import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.ENABLED_ARGUMENT_VALUE_FALSE;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.HELMSOURCE_TGZ;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;
import com.ericsson.oss.management.lcm.presentation.services.clusterconnectioninfo.ClusterConnectionInfoService;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.presentation.services.helmsource.HelmSourceService;
import com.ericsson.oss.management.lcm.presentation.services.helper.file.StoreFileHelper;
import com.ericsson.oss.management.lcm.presentation.services.helper.lcm.LcmHelper;
import com.ericsson.oss.management.lcm.repositories.WorkloadInstanceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TerminateServiceImpl implements TerminateService {

    @Value("${operation.timeout}")
    private int operationTimeout;

    private final HelmSourceService helmSourceService;
    private final FileService fileService;
    private final StoreFileHelper storeFileHelper;
    private final WorkloadInstanceRepository workloadInstanceRepository;
    private final LcmHelper lcmHelper;
    private final ClusterConnectionInfoService clusterConnectionInfoService;

    @Override
    public WorkloadInstance terminate(WorkloadInstance instance, MultipartFile clusterConnectionInfo, boolean deleteNamespace) {
        var helmSource = helmSourceService.get(instance);

        FilePathDetails paths = prepareFiles(instance, clusterConnectionInfo, helmSource);
        Path kubeConfigPath = paths.getKubeConfigPath();
        clusterConnectionInfoService.verifyClusterIdentifier(instance, kubeConfigPath);
        var operation = helmSourceService.destroyHelmSource(helmSource, operationTimeout, paths, deleteNamespace);
        instance.setLatestOperationId(operation.getId());

        return workloadInstanceRepository.save(instance);
    }

    private FilePathDetails prepareFiles(WorkloadInstance instance, MultipartFile clusterConnectionInfo, HelmSource helmSource) {
        Path directory = fileService.createDirectory();
        var helmPath = fileService.createFile(directory, helmSource.getContent(), HELMSOURCE_TGZ);
        helmSourceService.extractArchiveForHelmfile(helmSource, helmPath);
        FilePathDetails valuesPaths = storeFileHelper
                .getValuesPath(directory, null, instance, helmSource, ENABLED_ARGUMENT_VALUE_FALSE, false);
        var valuesPath = valuesPaths.getValuesPath();
        var kubeConfigPath = storeFileHelper.getKubeConfigPath(directory, instance, clusterConnectionInfo);

        return lcmHelper.preparePaths(helmPath, valuesPath, kubeConfigPath);
    }

}
