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

import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithChartsRequestDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceWithURLRequestDto;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;

/**
 * Service which instantiates workload instance
 */
public interface InstantiateService {

    /**
     * Instantiate workload instance
     * @param instance - specified instance
     * @param helmSourceFile - helm source that will be instantiated
     * @param clusterConnectionInfo - cluster details
     * @param values - values.yaml file
     * @param timeout - specified time to execute request
     * @return workloadInstance - object after instantiate
     */
    WorkloadInstance instantiate(WorkloadInstance instance, MultipartFile helmSourceFile,
                     MultipartFile clusterConnectionInfo, MultipartFile values, int timeout);

    /**
     * Instantiate workload instance through helm file builder
     * @param requestDto - request data
     * @param instance - specified instance
     * @param clusterConnectionInfo - cluster details
     * @param values - values.yaml file
     * @return workloadInstance - object after instantiate
     */
    WorkloadInstance instantiate(WorkloadInstanceWithChartsRequestDto requestDto, WorkloadInstance instance, MultipartFile values,
                     MultipartFile clusterConnectionInfo);

    /**
     * Instantiate workload instance with downloaded helmfile
     * @param requestDto - request data
     * @param isUrlToHelmRegistry - boolean flag for indicate if url in dto is a helm chart registry
     * @param instance - specified instance
     * @param clusterConnectionInfo - cluster details
     * @param values - values.yaml file
     * @return workloadInstance - object after instantiate
     */
    WorkloadInstance instantiate(WorkloadInstanceWithURLRequestDto requestDto, Boolean isUrlToHelmRegistry, WorkloadInstance instance,
                                 MultipartFile values, MultipartFile clusterConnectionInfo);
}
