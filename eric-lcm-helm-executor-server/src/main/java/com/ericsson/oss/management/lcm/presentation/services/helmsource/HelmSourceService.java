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

package com.ericsson.oss.management.lcm.presentation.services.helmsource;

import java.nio.file.Path;

import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.HelmSourceType;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;

/**
 * Working with Helmfile
 */
public interface HelmSourceService {

    /**
     * Create an instance of Helmfile class, create new Operation related to it
     * and call HelmfileExecutor to proceed with command execution further
     * @param helmSourcePath
     * @param workloadInstance
     * @param helmSourceType
     * @return instance of Helmfile already saved to DB
     */
    HelmSource create(Path helmSourcePath, WorkloadInstance workloadInstance, HelmSourceType helmSourceType);

    /**
     * Get HelmSource by id
     * @param helmSourceId
     * @return HelmSource object
     */
    HelmSource get(String helmSourceId);

    /**
     * Get helmSource by workload instance (based on version)
     *
     * @param instance
     * @return helmSource
     */
    HelmSource get(WorkloadInstance instance);

    /**
     * Get helmSource by workload instance and helmSource version
     *
     * @param instance
     * @param helmSourceVersion
     * @return helmSource
     */
    HelmSource getByWorkloadInstanceAndVersion(WorkloadInstance instance, String helmSourceVersion);

    /**
     * Build command and call executor. Describe state of instance with Operation
     * @param helmSource
     * @param timeout
     * @param type
     * @param paths to the helmSource, values and kube config files
     * @param version WorkloadInstanceVersion
     * @return the created operation
     */
    Operation executeHelmSource(HelmSource helmSource, int timeout, OperationType type, FilePathDetails paths, WorkloadInstanceVersion version);

    /**
     * Build destroy command and call executor. Describe state of instance with Operation
     * @param helmSource
     * @param timeout
     * @param paths to the helmSource and kube config files
     * @param deleteNamespace flag to specify if namespace must be deleted
     * @return the created operation
     */
    Operation destroyHelmSource(HelmSource helmSource, int timeout, FilePathDetails paths, boolean deleteNamespace);

    /**
     * Extract archive if helmsource type is helmfile and create repositories.yaml file
     * @param helmSource
     * @param helmPath
     */
    void extractArchiveForHelmfile(HelmSource helmSource, Path helmPath);

    /**
     * Extract archive and returns helmsource version
     * @param helmPath
     * @param helmSourceType
     * @return helmSourceVersion
     */
    String getHelmSourceVersion(Path helmPath, HelmSourceType helmSourceType);

    /**
     * Returns type of helmsource
     * @param helmPath
     * @return helmSourceType
     */
    HelmSourceType resolveHelmSourceType(Path helmPath);

    /**
     * Checks if received helmfile valid
     * @param valuesPath - path to the values file
     */
    void verifyHelmfile(Path valuesPath);

    /**
     * Checks if received integration chart valid
     * @param valuesPath - path to the values file
     */
    void verifyIntegrationChart(Path valuesPath);

    /**
     * Defines verify method based on helm source type
     * @param valuesPath - path to the values file
     * @param helmPath - path to the helm source
     * @param helmSourceType - type of the helm source
     */
    void verifyHelmSource(Path valuesPath, Path helmPath, HelmSourceType helmSourceType);

    /**
     * Download helm source by URL and return path
     * @param url - URL to the helm source
     * @param isUrlToHelmRegistry
     * @return Path - path to the downloaded helm source
     */
    Path downloadHelmSource(String url, boolean isUrlToHelmRegistry);

}
