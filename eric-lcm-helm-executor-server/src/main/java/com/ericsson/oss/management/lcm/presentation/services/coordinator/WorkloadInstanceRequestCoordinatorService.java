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

/**
 * This Service will be the main coordinating service of a request to apply a Helmfile or integration chart to a cluster.
 * To keep the Controllers simple this class will call all services involved in an operation.
 */
public interface WorkloadInstanceRequestCoordinatorService {

    /**
     * Take all the parts of a Post request, persist the information and execute the Helmfile or integration chart.
     * @param requestDto
     * @param helmSourceFile
     * @param values
     * @param clusterConnectionInfo
     * @return the workloadInstanceDto
     */
    WorkloadInstanceDto instantiate(WorkloadInstancePostRequestDto requestDto, MultipartFile helmSourceFile, MultipartFile values,
                                    MultipartFile clusterConnectionInfo);

    /**
     * Take all the parts of a Post request, persist the information, build helmfile and execute it.
     * @param requestDto
     * @param values
     * @param clusterConnectionInfo
     * @return the workloadInstanceDto
     */
    WorkloadInstanceDto instantiate(WorkloadInstanceWithChartsRequestDto requestDto, MultipartFile values,
                                    MultipartFile clusterConnectionInfo);

    /**
     * Take all the parts of a Post request, persist the information, fetch helmfile by url and execute it.
     * @param requestDto
     * @param isUrlToHelmRegistry
     * @param values
     * @param clusterConnectionInfo
     * @return the workloadInstanceDto
     */
    WorkloadInstanceDto instantiate(WorkloadInstanceWithURLRequestDto requestDto, Boolean isUrlToHelmRegistry, MultipartFile values,
                                    MultipartFile clusterConnectionInfo);
    /**
     * Get workloadInstance by id.
     * @param  workloadInstanceId
     * @return WorkloadInstanceDto
     */
    WorkloadInstanceDto getWorkloadInstance(String workloadInstanceId);

    /**
     * For a given workloadInstanceId, return the latestOperationId
     * @param workloadInstanceId
     * @return latest operation id
     */
    String getLatestOperationId(String workloadInstanceId);

    /**
     * Delete workloadInstance by id.
     * @param  workloadInstanceId
     */
    void deleteWorkloadInstance(String workloadInstanceId);

    /**
     * Terminate workloadInstance by id.
     * @param workloadInstanceId
     * @param requestDto
     * @param clusterConnectionInfo
     * @return WorkloadInstanceDto
     */
    WorkloadInstanceDto terminateWorkloadInstance(String workloadInstanceId, WorkloadInstanceOperationPostRequestDto requestDto,
                                                  MultipartFile clusterConnectionInfo);


    /**
     * Take all the parts of a Put request, persist incoming files, update workloadInstance and execute the Helmfile.
     * @param workloadInstanceId
     * @param requestDto
     * @param helmSourceFile
     * @param values
     * @param clusterConnectionInfo
     * @return the workloadInstanceDto
     */
    WorkloadInstanceDto update(String workloadInstanceId, WorkloadInstancePutRequestDto requestDto, MultipartFile helmSourceFile,
                               MultipartFile values, MultipartFile clusterConnectionInfo);

    /**
     * Take all the parts of a Put request, persist incoming files, update workloadInstance and execute the Helmfile.
     * @param workloadInstanceId
     * @param requestDto
     * @param values
     * @param clusterConnectionInfo
     * @return the workloadInstanceDto
     */
    WorkloadInstanceDto update(String workloadInstanceId, WorkloadInstanceWithChartsPutRequestDto requestDto,
                               MultipartFile values, MultipartFile clusterConnectionInfo);

    /**
     * Take all the parts of a Put request, persist the information, fetch helmfile by url, update instance and execute it.
     * @param workloadInstanceId
     * @param requestDto
     * @param isUrlToHelmRegistry
     * @param values
     * @param clusterConnectionInfo
     * @return the workloadInstanceDto
     */
    WorkloadInstanceDto update(String workloadInstanceId, WorkloadInstanceWithURLPutRequestDto requestDto,
                               Boolean isUrlToHelmRegistry, MultipartFile values, MultipartFile clusterConnectionInfo);
    /**
     * Rollback workload instance to specific helmsource version by id.
     * @param workloadInstanceId
     * @param requestDto
     * @param clusterConnectionInfo
     * @return the workloadInstanceDto
     */
    WorkloadInstanceDto rollback(String workloadInstanceId, WorkloadInstanceOperationPutRequestDto requestDto,
                                 MultipartFile clusterConnectionInfo);
}
