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

package com.ericsson.oss.management.lcm.presentation.services.version;

import static com.ericsson.oss.management.lcm.constants.WorkloadInstanceConstants.WORKLOAD_INSTANCE_VERSION_ID;
import static com.ericsson.oss.management.lcm.constants.WorkloadInstanceConstants.WORKLOAD_INSTANCE_VERSION_SORT_COLUMNS;
import static com.ericsson.oss.management.lcm.utils.pagination.PaginationUtils.buildLinks;
import static com.ericsson.oss.management.lcm.utils.pagination.PaginationUtils.buildPaginationInfo;
import static com.ericsson.oss.management.lcm.utils.pagination.PaginationUtils.createPageable;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ericsson.oss.management.lcm.api.model.PagedWorkloadInstanceVersionDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceVersionDto;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;
import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import com.ericsson.oss.management.lcm.presentation.mappers.WorkloadInstanceVersionDtoMapper;
import com.ericsson.oss.management.lcm.repositories.WorkloadInstanceVersionRepository;
import com.ericsson.oss.management.lcm.utils.pagination.CustomPageRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkloadInstanceVersionServiceImpl implements WorkloadInstanceVersionService {

    private final WorkloadInstanceVersionRepository workloadInstanceVersionRepository;
    private final WorkloadInstanceVersionDtoMapper workloadInstanceVersionMapper;

    @Override
    public WorkloadInstanceVersion createVersion(WorkloadInstance instance, String valuesVersion,
                                                 String helmSourceVersion) {
        Optional<WorkloadInstanceVersion> actualVersion = workloadInstanceVersionRepository
                .findTopByWorkloadInstanceOrderByVersionDesc(instance);
        Integer newVersion = getNewVersion(actualVersion);
        WorkloadInstanceVersion workloadInstanceVersion = WorkloadInstanceVersion.builder()
                .workloadInstance(instance)
                .helmSourceVersion(helmSourceVersion)
                .valuesVersion(Optional.ofNullable(valuesVersion).orElseGet(() -> getValuesVersion(actualVersion)))
                .version(newVersion)
                .serviceNameIdentifier(valuesVersion == null ? getHelmSourceVersion(actualVersion) : helmSourceVersion)
                .build();
        return workloadInstanceVersionRepository.save(workloadInstanceVersion);
    }

    @Override
    public WorkloadInstanceVersion getVersion(WorkloadInstance instance) {
        Integer version = instance.getVersion();
        return getByInstanceAndVersionIfExists(instance, version);
    }

    @Override
    public WorkloadInstanceVersion getPreviousVersion(WorkloadInstance instance) {
        Integer version = instance.getPreviousVersion();
        return getByInstanceAndVersionIfExists(instance, version);
    }

    @Override
    public WorkloadInstanceVersionDto getVersionDtoByWorkloadInstanceIdAndVersion(String workloadInstanceId, Integer version) {
        WorkloadInstanceVersion workloadInstanceVersion = getVersionByWorkloadInstanceIdAndVersion(workloadInstanceId, version);
        return workloadInstanceVersionMapper.toWorkloadInstanceVersionDto(workloadInstanceVersion);
    }

    @Override
    public WorkloadInstanceVersion getVersionByWorkloadInstanceIdAndVersion(String workloadInstanceId, Integer version) {
        return workloadInstanceVersionRepository
                .findByWorkloadInstanceWorkloadInstanceIdAndVersion(workloadInstanceId, version)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Version %s by workloadInstanceId = %s was not found", version, workloadInstanceId)));
    }

    @Override
    public PagedWorkloadInstanceVersionDto getAllVersionsByWorkloadInstance(String workloadInstanceId, Integer page,
                                                                            Integer size, List<String> sort) {
        Pageable pageable = createPageable(page, size, sort, WORKLOAD_INSTANCE_VERSION_SORT_COLUMNS,
                                           CustomPageRequest.of(WORKLOAD_INSTANCE_VERSION_ID));

        Page<WorkloadInstanceVersion> resultsPage =
                workloadInstanceVersionRepository.findAllByWorkloadInstanceWorkloadInstanceId(workloadInstanceId, pageable);
        return new PagedWorkloadInstanceVersionDto()
                .page(buildPaginationInfo(resultsPage))
                .links(buildLinks(resultsPage))
                .content(resultsPage.map(workloadInstanceVersionMapper::toWorkloadInstanceVersionDto)
                                 .getContent());
    }

    @Override
    public WorkloadInstanceVersion getVersionForRollback(WorkloadInstance instance, Integer version, OperationType operationType) {
        return Optional.ofNullable(version)
                .map(item -> getVersionByWorkloadInstanceIdAndVersion(instance.getWorkloadInstanceId(), item))
                .orElseGet(() -> getPreviousVersion(instance));
    }

    private Integer getNewVersion(Optional<WorkloadInstanceVersion> version) {
        return version
                .map(WorkloadInstanceVersion::getVersion)
                .map(item -> item + 1)
                .orElse(1);
    }

    private WorkloadInstanceVersion getByInstanceAndVersionIfExists(WorkloadInstance instance, Integer version) {
        return workloadInstanceVersionRepository
                .findByWorkloadInstanceAndVersion(instance, version)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Version %s by workloadInstance = %s was not found", version, instance.getWorkloadInstanceId())));
    }

    private String getValuesVersion(Optional<WorkloadInstanceVersion> version) {
        return version.orElseThrow(() -> new ResourceNotFoundException("Current version of workload instance was not found"))
                .getValuesVersion();
    }

    private String getHelmSourceVersion(Optional<WorkloadInstanceVersion> version) {
        return version.orElseThrow(() -> new ResourceNotFoundException("Current version of workload instance was not found"))
                .getHelmSourceVersion();
    }

}
