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

package com.ericsson.oss.management.lcm.presentation.services.workloadinstance;

import static com.ericsson.oss.management.lcm.constants.WorkloadInstanceConstants.SORT_COLUMNS;
import static com.ericsson.oss.management.lcm.constants.WorkloadInstanceConstants.WORKLOAD_INSTANCE_ID;
import static com.ericsson.oss.management.lcm.utils.pagination.PaginationUtils.buildLinks;
import static com.ericsson.oss.management.lcm.utils.pagination.PaginationUtils.buildPaginationInfo;
import static com.ericsson.oss.management.lcm.utils.pagination.PaginationUtils.createPageable;
import static java.lang.Boolean.TRUE;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.api.model.PagedWorkloadInstanceDto;
import com.ericsson.oss.management.lcm.model.entity.ClusterConnectionInfo;
import com.ericsson.oss.management.lcm.model.entity.ClusterConnectionInfoInstance;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.OperationState;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;
import com.ericsson.oss.management.lcm.presentation.exceptions.NotUniqueWorkloadInstanceException;
import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import com.ericsson.oss.management.lcm.presentation.mappers.WorkloadInstanceDtoMapper;
import com.ericsson.oss.management.lcm.repositories.WorkloadInstanceRepository;
import com.ericsson.oss.management.lcm.utils.pagination.CustomPageRequest;
import com.ericsson.oss.management.lcm.utils.validator.WorkloadInstanceValidator;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkloadInstanceServiceImpl implements WorkloadInstanceService {

    private final WorkloadInstanceRepository workloadInstanceRepository;
    private final WorkloadInstanceDtoMapper workloadInstanceDtoMapper;

    @Value("${default.crd.namespace}")
    private String defaultCrdNamespace;

    @Override
    public WorkloadInstance create(WorkloadInstance workloadInstance) {
        log.info("Will validate and persist {}", workloadInstance);

        WorkloadInstanceValidator.validate(workloadInstance);

        boolean isWorkloadWithSameNameExist = workloadInstanceRepository.existsByWorkloadInstanceName(workloadInstance.getWorkloadInstanceName());
        if (!isWorkloadWithSameNameExist) {
            return workloadInstanceRepository.save(workloadInstance);
        } else {
            throw new NotUniqueWorkloadInstanceException(
                    String.format("WorkloadName %s exist. Name of the workloadInstance must be unique",
                                  workloadInstance.getWorkloadInstanceName()));
        }
    }

    @Override
    public WorkloadInstance get(String workloadInstanceId) {
        return workloadInstanceRepository.findById(workloadInstanceId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("WorkloadInstance with id %s not found",
                                                                               workloadInstanceId)));
    }

    @Override
    public PagedWorkloadInstanceDto getAllWorkloadInstances(Integer page, Integer size, List<String> sort) {
        Pageable pageable = createPageable(page, size, sort, SORT_COLUMNS, CustomPageRequest.of(WORKLOAD_INSTANCE_ID));
        Page<WorkloadInstance> resultsPage = workloadInstanceRepository.findAll(pageable);
        return new PagedWorkloadInstanceDto()
                .page(buildPaginationInfo(resultsPage))
                .links(buildLinks(resultsPage))
                .content(resultsPage.map(workloadInstanceDtoMapper::toWorkloadInstanceDto)
                                 .getContent());
    }

    @Override
    public WorkloadInstance updateAdditionalParameters(WorkloadInstance workloadInstance) {
        WorkloadInstanceValidator.validate(workloadInstance);

        String workloadInstanceId = workloadInstance.getWorkloadInstanceId();
        WorkloadInstance savedWorkloadInstance = get(workloadInstanceId);
        savedWorkloadInstance.setAdditionalParameters(workloadInstance.getAdditionalParameters());
        return workloadInstanceRepository.save(savedWorkloadInstance);
    }

    @Override
    public WorkloadInstance update(WorkloadInstance workloadInstance) {
        return workloadInstanceRepository.save(workloadInstance);
    }

    @Transactional
    @Override
    public boolean validateInstancesForDeletionInNamespace(String namespace, String clusterIdentifier) {
        List<WorkloadInstance> allByNamespaceAndClusterIdentifier =
                workloadInstanceRepository.getAllByNamespaceAndClusterIdentifier(namespace, clusterIdentifier);
        if (allByNamespaceAndClusterIdentifier == null || allByNamespaceAndClusterIdentifier.isEmpty()) {
            return true;
        }
        return allByNamespaceAndClusterIdentifier.stream().allMatch(it -> TRUE.equals(workloadInstanceToDelete(it)));
    }

    @Override
    public void delete(String workloadInstanceId) {
        try {
            workloadInstanceRepository.deleteById(workloadInstanceId);
        } catch (EmptyResultDataAccessException e) {
            throw new ResourceNotFoundException(String.format("Workload instance with id %s does not exist", workloadInstanceId));
        }
    }

    @Override
    public void updateCrdNamespaceIfRequired(WorkloadInstance instance, MultipartFile clusterConnectionInfo) {
        if (clusterContainsCrdNamespace(clusterConnectionInfo, instance)) {
            String crdNamespaceFromCluster = getCrdNamespaceFromCluster(instance);
            instance.setCrdNamespace(crdNamespaceFromCluster);
        } else if (!instanceContainsCrdNamespace(clusterConnectionInfo, instance)) {
            instance.setCrdNamespace(defaultCrdNamespace);
        }
    }

    @Override
    public void updateVersion(WorkloadInstance instance, WorkloadInstanceVersion workloadInstanceVersion) {
        instance.setPreviousVersion(instance.getVersion());
        instance.setVersion(workloadInstanceVersion.getVersion());
        workloadInstanceRepository.save(instance);
    }

    private String getCrdNamespaceFromCluster(WorkloadInstance instance) {
        ClusterConnectionInfoInstance clusterConnectionInfoInstance = Optional.ofNullable(instance.getClusterConnectionInfoInstance())
                .orElseThrow(() -> new ResourceNotFoundException(String.format(
                        "WorkloadInstance with id = %s does not contain a ClusterConnectionInfo", instance.getWorkloadInstanceId())));
        ClusterConnectionInfo clusterConnection = clusterConnectionInfoInstance.getClusterConnectionInfo();
        return clusterConnection.getCrdNamespace();
    }

    private boolean clusterContainsCrdNamespace(MultipartFile clusterConnectionInfo, WorkloadInstance instance) {
        return clusterConnectionInfo == null && instance.getCluster() != null && getCrdNamespaceFromCluster(instance) != null;
    }

    private boolean instanceContainsCrdNamespace(MultipartFile clusterConnectionInfo, WorkloadInstance instance) {
        return clusterConnectionInfo != null && instance.getCrdNamespace() != null;
    }

    private boolean workloadInstanceToDelete(WorkloadInstance workloadInstance) {
        List<Operation> listOperations = workloadInstance.getOperations();

        if (listOperations.size() == 1) {
            return isOperationRollback(listOperations.get(0));
        }

        if (listOperations.size() > 1) {
            String latestOperationId = workloadInstance.getLatestOperationId();
            return listOperations.stream()
                    .filter(operation -> operation.getId().equals(latestOperationId))
                    .findAny()
                    .map(this::isOperationTerminated)
                    .orElse(false);
        }
        return true;
    }

    private boolean isOperationTerminated(Operation operation) {
        return OperationType.TERMINATE.equals(operation.getType()) && OperationState.COMPLETED.equals(operation.getState());
    }

    private boolean isOperationRollback(Operation operation) {
        return OperationType.ROLLBACK.equals(operation.getType()) && OperationState.COMPLETED.equals(operation.getState());
    }
}