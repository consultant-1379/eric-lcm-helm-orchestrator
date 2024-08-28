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

package com.ericsson.oss.management.lcm.utils.command.builder;

import java.nio.file.Path;
import java.util.List;

import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.Release;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;

/**
 * Build system commands for helmsource
 */
public interface HelmSourceCommandBuilder {

    /**
     * Build helmsource apply system command
     * @param helmSource with has parameters for building command
     * @param paths to the helmSource, values and kube config files
     * @param timeout specified time to execute command
     * @param operationType of the operation to execute
     * @return string with apply command
     */
    String apply(HelmSource helmSource, FilePathDetails paths, int timeout, OperationType operationType);

    /**
     * Build helmsource destroy system command
     * @param helmSource with has parameters for building command
     * @param paths to the helmSource and kube config files
     * @return string with destroy command
     */
    String delete(HelmSource helmSource, FilePathDetails paths);

    /**
     * Build Integration Chart destroy system command with cascade type
     * @param helmSource with has parameters for building command
     * @return string with destroy command
     */
    String deleteCascadeIntegrationChart(HelmSource helmSource);

    /**
     * Verify if helmsource and helmfile.yaml itself is valid
     * @param valuesPath - path to the values file
     * @return verify command
     */
    String verifyHelmfile(Path valuesPath);

    /**
     * Verify if integration chart itself is valid
     * @param helmPath - path to the values file
     * @return verify command
     */
    String verifyIntegrationChart(Path helmPath);

    /**
     * Build helmfile list command with selector by namespace
     *
     * @param paths of values and kube config part
     * @param namespace for selector
     * @return list command
     */
    String buildListCommand(FilePathDetails paths, String namespace);

    /**
     * Delete list of releases with a helm delete command
     *
     * @param releases to delete
     * @param namespace
     * @param kubeConfigPath
     * @return helm delete command
     */
    String deleteReleases(List<Release> releases, String namespace, Path kubeConfigPath);

    /**
     * Build command helm list with filter by instance name.
     *
     * Example of command with kube config:
     * KUBECONFIG=kubeConfig helm list --output json --filter workloadInstanceName -n namespace
     *
     * Example of command without kube config:
     * helm list --output json --filter workloadInstanceName -n namespace
     *
     * @param namespace of workload instance for selector
     * @param workloadInstanceName for filter selector
     * @param kubeConfig optional add kube config
     * @return string with command
     */
    String buildHelmListCommandWithFilterByName(String namespace, String workloadInstanceName, Path kubeConfig);
}