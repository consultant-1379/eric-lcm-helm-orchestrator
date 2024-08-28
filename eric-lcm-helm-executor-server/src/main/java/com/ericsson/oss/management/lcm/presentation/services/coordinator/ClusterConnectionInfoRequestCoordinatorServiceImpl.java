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

package com.ericsson.oss.management.lcm.presentation.services.coordinator;

import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.KUBE_CONFIG;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.ericsson.oss.management.lcm.presentation.exceptions.InternalRuntimeException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.api.model.ClusterConnectionInfoDto;
import com.ericsson.oss.management.lcm.model.entity.ClusterConnectionInfo;
import com.ericsson.oss.management.lcm.model.entity.ClusterConnectionInfoInstance;
import com.ericsson.oss.management.lcm.model.entity.ConnectionInfoStatus;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.presentation.exceptions.ClusterConnectionInfoConnectionException;
import com.ericsson.oss.management.lcm.presentation.exceptions.NotUniqueClusterException;
import com.ericsson.oss.management.lcm.presentation.exceptions.NotValidClusterNameException;
import com.ericsson.oss.management.lcm.presentation.services.clusterconnectioninfo.ClusterConnectionInfoService;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.presentation.services.kubernetes.KubernetesService;
import com.ericsson.oss.management.lcm.repositories.ClusterConnectionInfoInstanceRepository;
import com.ericsson.oss.management.lcm.repositories.ClusterConnectionInfoRepository;
import com.ericsson.oss.management.lcm.utils.validator.ClusterConnectionInfoFileValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClusterConnectionInfoRequestCoordinatorServiceImpl implements ClusterConnectionInfoRequestCoordinatorService {

    private final ClusterConnectionInfoService clusterConnectionInfoService;
    private final FileService fileService;
    private final ClusterConnectionInfoRepository clusterConnectionInfoRepository;
    private final ClusterConnectionInfoInstanceRepository clusterConnectionInfoInstanceRepository;
    private final ClusterConnectionInfoFileValidator clusterConnectionInfoFileValidator;
    private final KubernetesService kubernetesService;

    @Override
    public ClusterConnectionInfoDto create(final MultipartFile clusterConnectionInfoFile, String crdNamespace) {
        log.info("Received request to upload cluster credentials with the file name {}",
                 clusterConnectionInfoFile.getOriginalFilename());
        doesClusterConnectionInfoAlreadyExists(clusterConnectionInfoFile);
        Path directory = fileService.createDirectory();
        var clusterConnectionInfoPath = fileService.storeFileIn(directory, clusterConnectionInfoFile, KUBE_CONFIG);
        clusterConnectionInfoFileValidator.validate(clusterConnectionInfoFile, clusterConnectionInfoPath);
        clusterConnectivityCheck(clusterConnectionInfoPath, directory);
        var clusterConnectionInfoDto = clusterConnectionInfoService.create(clusterConnectionInfoPath, crdNamespace);
        fileService.deleteDirectory(directory);
        return clusterConnectionInfoDto;
    }

    @Override
    public void connectToClusterConnectionInfoIfPresent(final WorkloadInstance instance) {
        Optional
                .ofNullable(instance.getCluster())
                .map(this::getClusterConnectionInfoByName)
                .ifPresent(clusterConnectionInfoEntity -> {
                    connectInstanceToCluster(instance, clusterConnectionInfoEntity);
                    enableCluster(clusterConnectionInfoEntity);
                });
    }

    @Override
    public void disconnectFromClusterIfPresent(final WorkloadInstance instance) {
        Optional
                .ofNullable(instance.getCluster())
                .map(this::getClusterConnectionInfoByName)
                .ifPresent(clusterConnectionInfoEntity -> {
                    disconnectInstanceFromCluster(clusterConnectionInfoEntity, instance);
                    disableCluster(clusterConnectionInfoEntity);
                });
    }

    private void disconnectInstanceFromCluster(final ClusterConnectionInfo clusterConnectionInfoEntity,
            WorkloadInstance workloadInstance) {
        String workloadInstanceId = workloadInstance.getWorkloadInstanceId();
        if (clusterConnectionInfoEntity.getClusterConnectionInfoInstances() == null) {
            throw new InternalRuntimeException(String.format(
                    "ClusterConnectionInfo instances list for workload instance with id = %s is null", workloadInstanceId));
        }
        List<ClusterConnectionInfoInstance> clusterConnectionInfoInstances = clusterConnectionInfoEntity.getClusterConnectionInfoInstances();
        var clusterConnectionInfoInstance = clusterConnectionInfoInstances
                .stream()
                .filter(item -> item
                        .getWorkloadInstance()
                        .getWorkloadInstanceId()
                        .equals(workloadInstanceId))
                .findAny()
                .orElseThrow(() -> new InternalRuntimeException(
                        String.format("Appropriate cluster for workload instance with id = %s was not found", workloadInstanceId)));
        clusterConnectionInfoInstanceRepository.delete(clusterConnectionInfoInstance);
        clusterConnectionInfoInstances.remove(clusterConnectionInfoInstance);
        clusterConnectionInfoEntity.setClusterConnectionInfoInstances(clusterConnectionInfoInstances);
        workloadInstance.setClusterConnectionInfoInstance(null);
        log.info(String.format("Instance with id = %s disconnected to cluster %s", workloadInstanceId,
                               clusterConnectionInfoEntity.getName()));
    }

    private void disableCluster(final ClusterConnectionInfo clusterConnectionInfoEntity) {
        if (clusterConnectionInfoInstanceRepository
                .findClusterConnectionInfoInstanceByClusterConnectionInfoId(clusterConnectionInfoEntity.getId())
                .isEmpty()) {
            clusterConnectionInfoEntity.setStatus(ConnectionInfoStatus.NOT_IN_USE);
            clusterConnectionInfoRepository.save(clusterConnectionInfoEntity);
            log.info(String.format("Cluster %s is disabled", clusterConnectionInfoEntity.getName()));
        }
    }

    private ClusterConnectionInfo getClusterConnectionInfoByName(final String cluster) {
        return clusterConnectionInfoRepository
                .findByName(cluster)
                .orElseThrow(() -> new NotValidClusterNameException(
                        String.format("Cluster with a name %s not exist. Please check it or create create a new one.",
                                cluster)));
    }

    private void connectInstanceToCluster(WorkloadInstance instance, ClusterConnectionInfo clusterConnectionInfoEntity) {
        var clusterConnectionInfoInstance = ClusterConnectionInfoInstance
                .builder()
                .clusterConnectionInfo(clusterConnectionInfoEntity)
                .workloadInstance(instance)
                .build();
        var savedInfoInstance = clusterConnectionInfoInstanceRepository.save(clusterConnectionInfoInstance);
        instance.setClusterConnectionInfoInstance(savedInfoInstance);
        log.info(String.format("Instance with id = %s connected to cluster %s", instance.getWorkloadInstanceId(),
                               clusterConnectionInfoEntity.getName()));
    }

    private void enableCluster(ClusterConnectionInfo clusterConnectionInfoEntity) {
        clusterConnectionInfoEntity.setStatus(ConnectionInfoStatus.IN_USE);
        clusterConnectionInfoRepository.save(clusterConnectionInfoEntity);
        log.info(String.format("Cluster %s is enabled", clusterConnectionInfoEntity.getName()));
    }

    private void doesClusterConnectionInfoAlreadyExists(final MultipartFile clusterConnectionInfoFile) {
        clusterConnectionInfoRepository
                .findByName(clusterConnectionInfoFile.getOriginalFilename())
                .ifPresent(clusterConnectionInfo -> {
                    throw new NotUniqueClusterException(String.format("A connection info with the name %s already exists",
                                                                      clusterConnectionInfoFile.getOriginalFilename()));
                });
    }

    private void clusterConnectivityCheck(Path clusterConfigInfoPath, Path directory) {
        try {
            kubernetesService.getListNamespaces(clusterConfigInfoPath);
        } catch (Exception e) {
            fileService.deleteDirectory(directory);
            throw new ClusterConnectionInfoConnectionException(String.format("Could not connect to the cluster. Details: %s", e.getMessage()));
        }
    }
}
