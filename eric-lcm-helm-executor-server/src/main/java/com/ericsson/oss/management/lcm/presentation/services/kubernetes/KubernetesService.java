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

package com.ericsson.oss.management.lcm.presentation.services.kubernetes;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.ericsson.oss.management.lcm.model.internal.PrivateKeyData;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

/**
 * Service for fetching data from kubernetes
 */
public interface KubernetesService {

    /**
     * Create or replace the secret in namespace
     * @param namespace
     * @param secret
     * @param kubeConfig for the target cluster
     */
    void createOrReplaceSecretInNamespace(String namespace, Secret secret, Path kubeConfig);

    /**
     * Save Private Key to the data field in secret
     *
     * @param namespace the namespace where secret is located
     * @param secretName name of secret
     * @param privateKeyData object that contains key name and key content
     */
    void saveKeyToSecret(String namespace, String secretName, PrivateKeyData privateKeyData);

    /**
     * Get secret from namespace by secret name
     *
     * @param namespace the namespace where secret is located
     * @param secretName name of secret
     * @return object which represent secret
     */
    Secret getSecretByNamespaceAndSecretName(String namespace, String secretName);

    /**
     * Get the name and status of the pods from the cluster.
     * @param namespace in the kubernetes cluster
     * @param kubeConfig for the target cluster
     * @return map where key is the name of pod and value is the status of pod
     */
    Map<String, String> getPodsStatusInfo(String namespace, Path kubeConfig);

    /**
     * Fetch data about the pods in namespace
     * @param namespace to check
     * @param kubeConfig for the target cluster
     * @return list of pods
     */
    List<Pod> getPodsInNamespace(String namespace, Path kubeConfig);

    /**
     * Fetch data about the namespaces in cluster
     * @param kubeConfig for the target cluster
     * @return list of namespaces
     */
    List<Namespace> getListNamespaces(Path kubeConfig);

    /**
     * Collect pod logs
     * @param namespace
     * @param workloadInstanceName
     * @param kubeConfig for the target cluster
     * @return log info
     */
    String getLogs(String namespace, String workloadInstanceName, Path kubeConfig);

    /**
     * Collect pod logs by namespace
     * @param namespace
     * @param kubeConfig for the target cluster
     * @return log info
     */
    String getLogsByNamespace(String namespace, Path kubeConfig);

    /**
     * Delete kubernetes Deployments, StatefulSets, ReplicaSets, Pods, Ingresses, Services, ConfigMaps, Secrets in defined namespace
     * and with certain instance name. Instance name defines in the label which helm add during deploy. Structure of label:
     * <p>
     *     key=app.kubernetes.io/instance , value=workloadInstanceName
     * </p>
     * If kubeconfig null the default {@link KubernetesClient} will be used in other case it will configure client from passed kubeconfig file.
     *
     * @param namespace the namespace where resources are located
     * @param labelValue the name of the workload instance to be placed as the value in the label
     * @param kubeconfig optional kubeconfig file for configuring kubernetes client
     */
    void cleanResourcesByNamespaceAndInstanceName(String namespace, String labelValue, Path kubeconfig);

    /**
     * Delete specified namespace in kubernetes cluster
     * @param namespace to delete
     * @param kubeConfig of the target cluster
     */
    void deleteNamespace(String namespace, Path kubeConfig);

    /**
     * Create namespace if required and secret on recently created namespace
     *
     * @param kubeConfig cluster
     * @param namespace data for namespace
     * @param secret data for secret
     */
    void createSecretAndNamespaceIfRequired(Path kubeConfig, String namespace, Secret secret);

    /**
     * Delete secret with defined name from namespace
     *
     * @param kubeConfig path to cluster
     * @param secretName of defined secret
     * @param namespace on the cluster
     */
    void deleteSecret(String kubeConfig, String secretName, String namespace);

}
