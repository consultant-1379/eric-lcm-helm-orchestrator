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

package com.ericsson.oss.management.lcm.presentation.services.clusterconnectioninfo;

import static java.util.Objects.nonNull;

import static com.ericsson.oss.management.lcm.constants.ClusterConnectionInfoConstants.NAME;
import static com.ericsson.oss.management.lcm.constants.ClusterConnectionInfoConstants.SORT_COLUMNS;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.DEFAULT_CLUSTER_IDENTIFIER;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.SPACE;
import static com.ericsson.oss.management.lcm.utils.pagination.PaginationUtils.buildLinks;
import static com.ericsson.oss.management.lcm.utils.pagination.PaginationUtils.buildPaginationInfo;
import static com.ericsson.oss.management.lcm.utils.pagination.PaginationUtils.createPageable;

import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.ericsson.oss.management.lcm.presentation.mappers.ClusterConnectionInfoMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.ericsson.oss.management.lcm.api.model.ClusterConnectionInfoDto;
import com.ericsson.oss.management.lcm.api.model.PagedClusterConnectionInfoDto;
import com.ericsson.oss.management.lcm.model.clusterconnection.Cluster;
import com.ericsson.oss.management.lcm.model.clusterconnection.ClusterData;
import com.ericsson.oss.management.lcm.model.entity.ClusterConnectionInfo;
import com.ericsson.oss.management.lcm.model.entity.ConnectionInfoStatus;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.presentation.exceptions.ClusterConnectionInfoInUseException;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidInputException;
import com.ericsson.oss.management.lcm.presentation.exceptions.NotUniqueClusterException;
import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.repositories.ClusterConnectionInfoRepository;
import com.ericsson.oss.management.lcm.utils.ClusterConnectionInfoUtils;
import com.ericsson.oss.management.lcm.utils.pagination.CustomPageRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClusterConnectionInfoServiceImpl implements ClusterConnectionInfoService {

    private final ClusterConnectionInfoRepository repository;
    private final ClusterConnectionInfoMapper mapper;
    private final FileService fileService;

    @Override
    public ClusterConnectionInfoDto create(Path clusterConnectionInfoPath, String crdNamespace) {
        byte[] clusterConnectionInfoFile = fileService.getFileContent(clusterConnectionInfoPath);
        List<Cluster> clusters = ClusterConnectionInfoUtils.getKubeConfig(clusterConnectionInfoFile).getClusters();
        String defaultCluster = clusters.get(0).getName();

        var clusterConnectionInfo = ClusterConnectionInfo.builder()
                .name(defaultCluster)
                .url(getClusterUrl(clusterConnectionInfoFile))
                .content(clusterConnectionInfoFile)
                .status(ConnectionInfoStatus.NOT_IN_USE)
                .crdNamespace(crdNamespace)
                .build();
        try {
            clusterConnectionInfo = repository.save(clusterConnectionInfo);
        } catch (DataIntegrityViolationException e) {
            throw new NotUniqueClusterException(String.format("Cluster name must be unique: %s", e.getMessage()));
        }
        return mapper.toClusterConnectionInfoDto(clusterConnectionInfo);
    }

    @Override
    public ClusterConnectionInfoDto get(final String id) {
        var clusterConnectionInfo = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("ClusterConnectionInfo with id %s not found", id)));
        return mapper.toClusterConnectionInfoDto(clusterConnectionInfo);
    }

    @Override
    public PagedClusterConnectionInfoDto getAllClusterConnectionInfo(Integer page, Integer size, List<String> sort) {
        var pageable = createPageable(page, size, sort, SORT_COLUMNS, CustomPageRequest.of(NAME));
        Page<ClusterConnectionInfo> resultsPage = repository.findAll(pageable);
        return new PagedClusterConnectionInfoDto()
                .page(buildPaginationInfo(resultsPage))
                .links(buildLinks(resultsPage))
                .content(resultsPage.map(mapper::toClusterConnectionInfoDto).getContent());
    }

    @Override
    public void delete(final String id) {
        var clusterConnectionInfo = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("ClusterConnectionInfo with id %s not found", id)));
        if (clusterConnectionInfo.getStatus() == ConnectionInfoStatus.IN_USE) {
            throw new ClusterConnectionInfoInUseException(String.format("Cluster with id %s still IN_USE, you can't delete it.", id));
        }
        repository.deleteById(id);
    }

    @Override
    public Optional<ClusterConnectionInfo> findByClusterName(final String clusterName) {
        return repository.findByName(clusterName);
    }

    @Override
    public String getClusterUrl(byte[] clusterConnectionInfoFile) {
        var kubeConfig = ClusterConnectionInfoUtils.getKubeConfig(clusterConnectionInfoFile);
        return kubeConfig.getClusters()
                .stream()
                .map(Cluster::getClusterData)
                .map(ClusterData::getServer)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Server url is absent"));
    }
    @Override
    public void verifyClusterIdentifier(WorkloadInstance instance, Path clusterConnectionInfoPath) {
        if (nonNull(clusterConnectionInfoPath) && instance.getClusterIdentifier().equals(DEFAULT_CLUSTER_IDENTIFIER)) {
            throw new InvalidInputException("WorkloadInstance was instantiated on default cluster. ClusterConnectionInfo can't be passed in request");
        }
        String clusterIdentifier = getClusterIdentifier(instance, clusterConnectionInfoPath);
        if (clusterConnectionInfoPath != null && !getInputClusterIdentifier(clusterConnectionInfoPath).equals(clusterIdentifier)) {
            throw new InvalidInputException("Cluster from request does not match the cluster from workloadInstance");
        }
    }

    @Override
    public String resolveClusterIdentifier(Path clusterConnectionInfoPath) {
        return clusterConnectionInfoPath != null ? getInputClusterIdentifier(clusterConnectionInfoPath) :
                DEFAULT_CLUSTER_IDENTIFIER;
    }

    private String getClusterIdentifier(WorkloadInstance instance, Path clusterConnectionInfoPath) {
        if (instance.getClusterIdentifier() == null) {
            instance.setClusterIdentifier(resolveClusterIdentifier(clusterConnectionInfoPath));
        }
        return instance.getClusterIdentifier();
    }

    private String getInputClusterIdentifier(Path clusterConnectionInfoPath) {
        byte[] kubeConfigFile = fileService.getFileContentIfPresent(clusterConnectionInfoPath);
        var kubeConfig = ClusterConnectionInfoUtils.getKubeConfig(kubeConfigFile);
        var cluster = kubeConfig.getClusters().get(0);
        String clusterName = cluster.getName();
        String clusterUrl = cluster.getClusterData().getServer();
        return clusterName + SPACE + clusterUrl;
    }
}
