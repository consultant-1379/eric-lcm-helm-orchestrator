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

import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceOperationPostRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceOperationPutRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstancePostRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstancePutRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithChartsPutRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithChartsRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithURLRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithURLPutRequestDto;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.OperationState;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.presentation.exceptions.IncorrectRollbackRequestException;
import com.ericsson.oss.management.lcm.presentation.exceptions.InstanceNotTerminatedException;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidInputException;
import com.ericsson.oss.management.lcm.presentation.mappers.WorkloadInstanceDtoMapper;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.operations.InstantiateService;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.operations.RollbackService;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.operations.TerminateService;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.operations.UpdateService;
import com.ericsson.oss.management.lcm.presentation.services.operation.OperationService;
import com.ericsson.oss.management.lcm.presentation.services.version.WorkloadInstanceVersionService;
import com.ericsson.oss.management.lcm.presentation.services.workloadinstance.WorkloadInstanceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkloadInstanceRequestCoordinatorServiceImpl implements WorkloadInstanceRequestCoordinatorService {

    private static final String WILL_RETURN_MESSAGE = "Will return {}";
    private static final String START_INSTANTIATE_MESSAGE = "Starting to instantiate instance with a name {}";
    private static final String START_UPDATE_MESSAGE = "Starting to update instance {}";

    @Value("${operation.timeout}")
    private int operationTimeout;
    @Value("${directory.root}")
    private String rootDirectory;
    @Value("${deleteNamespace.default}")
    private Boolean deleteNamespaceDefault;

    private final WorkloadInstanceDtoMapper workloadInstanceMapper;
    private final WorkloadInstanceService workloadInstanceService;
    private final OperationService operationService;
    private final ClusterConnectionInfoRequestCoordinatorService clusterConnectionInfoRequestCoordinatorService;
    private final WorkloadInstanceVersionService workloadInstanceVersionService;
    private final TerminateService terminateService;
    private final RollbackService rollbackService;
    private final InstantiateService instantiateService;
    private final UpdateService updateService;

    @Override
    @Transactional
    public WorkloadInstanceDto instantiate(WorkloadInstancePostRequestDto requestDto, MultipartFile helmSourceFile, MultipartFile values,
                                           MultipartFile clusterConnectionInfo) {
        log.info(START_INSTANTIATE_MESSAGE, requestDto.getWorkloadInstanceName());
        var instance = workloadInstanceMapper.toWorkloadInstance(requestDto);
        instance = workloadInstanceService.create(instance);
        clusterConnectionInfoRequestCoordinatorService.connectToClusterConnectionInfoIfPresent(instance);
        int timeout = Optional.ofNullable(requestDto.getTimeout()).orElse(operationTimeout);
        instance = instantiateService.instantiate(instance, helmSourceFile, clusterConnectionInfo, values, timeout);
        log.info(WILL_RETURN_MESSAGE, instance);
        return workloadInstanceMapper.toWorkloadInstanceDto(instance);
    }

    @Override
    @Transactional
    public WorkloadInstanceDto instantiate(WorkloadInstanceWithChartsRequestDto requestDto, MultipartFile values,
                                           MultipartFile clusterConnectionInfo) {
        log.info(START_INSTANTIATE_MESSAGE, requestDto.getWorkloadInstanceName());
        var instance = workloadInstanceMapper.toWorkloadInstance(requestDto);
        instance = workloadInstanceService.create(instance);
        clusterConnectionInfoRequestCoordinatorService.connectToClusterConnectionInfoIfPresent(instance);
        instance = instantiateService.instantiate(requestDto, instance, values, clusterConnectionInfo);
        log.info(WILL_RETURN_MESSAGE, instance);
        return workloadInstanceMapper.toWorkloadInstanceDto(instance);
    }

    @Override
    @Transactional
    public WorkloadInstanceDto instantiate(WorkloadInstanceWithURLRequestDto requestDto, Boolean isUrlToHelmRegistry, MultipartFile values,
                                           MultipartFile clusterConnectionInfo) {
        log.info(START_INSTANTIATE_MESSAGE, requestDto.getWorkloadInstanceName());
        var instance = workloadInstanceMapper.toWorkloadInstance(requestDto);
        instance = workloadInstanceService.create(instance);
        clusterConnectionInfoRequestCoordinatorService.connectToClusterConnectionInfoIfPresent(instance);
        instance = instantiateService.instantiate(requestDto, isUrlToHelmRegistry, instance, values, clusterConnectionInfo);
        log.info(WILL_RETURN_MESSAGE, instance);
        return workloadInstanceMapper.toWorkloadInstanceDto(instance);
    }

    @Override
    public WorkloadInstanceDto getWorkloadInstance(String workloadInstanceId) {
        var workloadInstance = workloadInstanceService.get(workloadInstanceId);
        return workloadInstanceMapper.toWorkloadInstanceDto(workloadInstance);
    }

    @Override
    @Transactional
    public WorkloadInstanceDto update(String workloadInstanceId, WorkloadInstancePutRequestDto requestDto, MultipartFile helmSourceFile,
                                      MultipartFile values,
                                      MultipartFile clusterConnectionInfo) {
        var instance = workloadInstanceService.get(workloadInstanceId);
        log.info(START_UPDATE_MESSAGE, instance);
        updateWorkloadInstanceWithRequestValues(requestDto, instance);
        instance = updateService.update(instance, helmSourceFile, requestDto, values, clusterConnectionInfo);
        log.info(WILL_RETURN_MESSAGE, instance);
        return workloadInstanceMapper.toWorkloadInstanceDto(instance);
    }

    @Override
    @Transactional
    public WorkloadInstanceDto update(String workloadInstanceId, WorkloadInstanceWithChartsPutRequestDto requestDto,
                                      MultipartFile values,
                                      MultipartFile clusterConnectionInfo) {
        var instance = workloadInstanceService.get(workloadInstanceId);
        log.info(START_UPDATE_MESSAGE, instance);
        updateWorkloadInstanceWithRequestValuesForHelmfileBuilder(requestDto, instance);
        instance = updateService.update(instance, requestDto, values, clusterConnectionInfo);
        log.info(WILL_RETURN_MESSAGE, instance);
        return workloadInstanceMapper.toWorkloadInstanceDto(instance);
    }

    @Override
    @Transactional
    public WorkloadInstanceDto update(String workloadInstanceId, WorkloadInstanceWithURLPutRequestDto requestDto,
                                      Boolean isUrlToHelmRegistry, MultipartFile values,
                                      MultipartFile clusterConnectionInfo) {
        var instance = workloadInstanceService.get(workloadInstanceId);
        log.info(START_UPDATE_MESSAGE, instance);
        updateWorkloadInstanceWithRequestValuesForFetcher(requestDto, instance);
        instance = updateService.update(instance, isUrlToHelmRegistry, requestDto, values, clusterConnectionInfo);
        log.info(WILL_RETURN_MESSAGE, instance);
        return workloadInstanceMapper.toWorkloadInstanceDto(instance);
    }

    @Override
    public WorkloadInstanceDto rollback(String workloadInstanceId, WorkloadInstanceOperationPutRequestDto workloadInstanceOperationPutRequestDto,
                                        MultipartFile clusterConnectionInfo) {
        var instance = workloadInstanceService.get(workloadInstanceId);
        log.info("Starting to rollback instance {}", instance);
        OperationType type = operationService.get(instance.getLatestOperationId()).getType();
        verifyIfRollbackIsPossible(type, instance, workloadInstanceOperationPutRequestDto.getVersion());
        var version = workloadInstanceVersionService
                .getVersionForRollback(instance, workloadInstanceOperationPutRequestDto.getVersion(), type);

        instance = rollbackService.rollback(instance, version, clusterConnectionInfo);
        log.info(WILL_RETURN_MESSAGE, instance);
        return workloadInstanceMapper.toWorkloadInstanceDto(instance);
    }

    @Override
    public String getLatestOperationId(final String workloadInstanceId) {
        var instance = workloadInstanceService.get(workloadInstanceId);
        return instance.getLatestOperationId();
    }

    @Override
    public void deleteWorkloadInstance(String workloadInstanceId) {
        var workloadInstance = workloadInstanceService.get(workloadInstanceId);
        log.info("Starting to delete instance {}", workloadInstance);
        var operation = operationService.get(workloadInstance.getLatestOperationId());
        log.info("Last operation is {}",  operation);
        if (isOperationTerminated(operation) || isAutoRollbackOperationFinished(operation, workloadInstance)) {
            clusterConnectionInfoRequestCoordinatorService.disconnectFromClusterIfPresent(workloadInstance);
            workloadInstanceService.delete(workloadInstanceId);
            log.info("DELETE operation was SUCCESSFULLY finished for workload instance with id={}", workloadInstanceId);
        } else {
            throw new InstanceNotTerminatedException("Workload instance must be TERMINATED before deletion. As an exclusion instance that doesn't "
                                                             + "have any successful operation can be deleted as well");
        }
    }

    @Override
    public WorkloadInstanceDto terminateWorkloadInstance(String workloadInstanceId, WorkloadInstanceOperationPostRequestDto requestDto,
                                                         MultipartFile clusterConnectionInfo) {

        if (!"terminate".equals(requestDto.getType())) {
            throw new InvalidInputException("Only terminate type is allowed");
        }
        var instance = workloadInstanceService.get(workloadInstanceId);
        log.info("Starting to terminate instance {}", instance);
        checkIfTerminateIsAllowed(instance);

        var deleteNamespace = deleteNamespaceDefault;
        if (requestDto.getDeleteNamespace() != null) {
            deleteNamespace = requestDto.getDeleteNamespace();
        }
        instance = terminateService.terminate(instance, clusterConnectionInfo, deleteNamespace);
        log.info(WILL_RETURN_MESSAGE, instance);

        return workloadInstanceMapper.toWorkloadInstanceDto(instance);
    }

    private boolean isOperationTerminated(Operation operation) {
        return operation.getType() == OperationType.TERMINATE && operation.getState() != OperationState.PROCESSING;
    }

    private boolean isAutoRollbackOperationFinished(Operation operation, WorkloadInstance workloadInstance) {
        if (operation == null) {
            throw new IncorrectRollbackRequestException(
                    String.format("Operation is null for workload instance with id = %s", workloadInstance.getWorkloadInstanceId()));
        }
        return operation.getType() == OperationType.ROLLBACK && workloadInstance.getOperations().size() == 1
                && (operation.getState() == OperationState.FAILED || operation.getState() == OperationState.COMPLETED);
    }

    private void checkIfTerminateIsAllowed(WorkloadInstance instance) {
        Optional.ofNullable(instance.getLatestOperationId())
                .map(operationService::get)
                .ifPresent(this::checkIfTerminate);
    }

    private void checkIfTerminate(Operation operation) {
        if (operation.getType() == OperationType.TERMINATE) {
            throw new InvalidInputException("Terminate can`t be executed for terminated workload instance");
        }
    }

    private void updateWorkloadInstanceWithRequestValues(WorkloadInstancePutRequestDto requestDto, WorkloadInstance instance) {
        instance.setAdditionalParameters(null);
        Optional.ofNullable(requestDto)
                .ifPresent(dto -> workloadInstanceMapper.updateWorkloadInstanceFromWorkloadInstancePutRequestDto(dto, instance));
    }

    private void updateWorkloadInstanceWithRequestValuesForHelmfileBuilder(WorkloadInstanceWithChartsPutRequestDto requestDto,
                                                                           WorkloadInstance instance) {
        instance.setAdditionalParameters(null);
        Optional.ofNullable(requestDto)
                .ifPresent(dto -> workloadInstanceMapper.updateWorkloadInstanceFromWorkloadInstanceWithChartsPutRequestDto(dto, instance));
    }

    private void updateWorkloadInstanceWithRequestValuesForFetcher(WorkloadInstanceWithURLPutRequestDto requestDto,
                                                                           WorkloadInstance instance) {
        instance.setAdditionalParameters(null);
        Optional.ofNullable(requestDto)
                .ifPresent(dto -> workloadInstanceMapper.updateWorkloadInstanceFromWorkloadInstanceWithUrlPutRequestDto(dto, instance));
    }

    private void verifyIfRollbackIsPossible(OperationType type, WorkloadInstance instance, Integer version) {
        if (OperationType.INSTANTIATE == type) {
            throw new IncorrectRollbackRequestException("Manual Rollback request cannot been received right after INSTANTIATE," +
                                                                " if you have intention to terminate this instance, please use terminate request");
        } else if (OperationType.REINSTANTIATE == type && instance.getPreviousVersion() == null) {
            throw new IncorrectRollbackRequestException("Manual Rollback request cannot been received because there is no UPDATES been done");
        } else if (OperationType.TERMINATE == type) {
            throw new IncorrectRollbackRequestException("Manual Rollback can not be done when instance is terminated, please reinstantiate it first");
        } else if (Objects.equals(instance.getVersion(), version)) {
            throw new IncorrectRollbackRequestException("The version which you want to rollback is actual");
        }
    }
}
