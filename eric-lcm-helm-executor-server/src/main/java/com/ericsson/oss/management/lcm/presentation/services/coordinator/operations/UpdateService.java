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

import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithURLPutRequestDto;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.api.model.WorkloadInstancePutRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithChartsPutRequestDto;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;

/**
 * Service which update workload instance
 */
public interface UpdateService {

    /**
     * Update workload instance
     *
     * @param instance to update
     * @param helmSourceFile
     * @param requestDto
     * @param values
     * @param clusterConnectionInfo
     * @return updated instance
     */
    WorkloadInstance update(WorkloadInstance instance, MultipartFile helmSourceFile,
                            WorkloadInstancePutRequestDto requestDto, MultipartFile values, MultipartFile clusterConnectionInfo);

    /**
     * Update workload instance through the helmfile builder
     *
     * @param instance to update
     * @param requestDto
     * @param values
     * @param clusterConnectionInfo
     * @return updated instance
     */
    WorkloadInstance update(WorkloadInstance instance, WorkloadInstanceWithChartsPutRequestDto requestDto, MultipartFile values,
                            MultipartFile clusterConnectionInfo);

    /**
     * Update workload instance through the helmfile fetcher
     *
     * @param instance to update
     * @param isUrlToHelmRegistry
     * @param requestDto
     * @param values
     * @param clusterConnectionInfo
     * @return updated instance
     */
    WorkloadInstance update(WorkloadInstance instance, Boolean isUrlToHelmRegistry, WorkloadInstanceWithURLPutRequestDto requestDto,
                            MultipartFile values, MultipartFile clusterConnectionInfo);
}
