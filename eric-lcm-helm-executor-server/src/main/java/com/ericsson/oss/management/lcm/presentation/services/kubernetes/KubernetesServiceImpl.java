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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.stereotype.Service;

import com.ericsson.oss.management.lcm.model.internal.PrivateKeyData;

import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.ericsson.oss.management.lcm.constants.DockerConstants.NS_METADATA_LABELS_NAME;

@Slf4j
@Service
@RequiredArgsConstructor
public class KubernetesServiceImpl implements KubernetesService {

    private static final String LABEL_CONSTANT = "app.kubernetes.io/instance";
    private static final String RUNNING = "running=";
    private static final String TERMINATED = "terminated=";
    private static final String WAITING = "waiting=";
    private static final String NEW_LINE = "\n";

    private final KubernetesClient kubernetesClient;
    private final FileService fileService;

    @Override
    public void createOrReplaceSecretInNamespace(String namespace, Secret secret, Path kubeConfig) {
        var definedKubernetesClient = defineKubernetesClient(kubeConfig);
        createOrReplaceSecret(secret, definedKubernetesClient, namespace);
    }

    @Override
    public void saveKeyToSecret(String namespace, String secretName, PrivateKeyData privateKeyData) {
        log.info("Save key {} to secret", privateKeyData.getKeyName());
        kubernetesClient.secrets()
                .inNamespace(namespace)
                .withName(secretName)
                .edit(secret -> new SecretBuilder(secret)
                        .addToStringData(privateKeyData.getKeyName(), privateKeyData.getKeyContent())
                        .build());
    }

    @Override
    public Map<String, String> getPodsStatusInfo(String namespace, Path kubeConfig) {
        log.info("Get pods statuses from namespace {}", namespace);
        List<Pod> pods = getPodsInNamespace(namespace, kubeConfig);
        return pods.stream()
                .collect(Collectors.toMap(this::getPodName, this::getPodStatus));
    }

    @Override
    public List<Pod> getPodsInNamespace(String namespace, Path kubeConfig) {
        var definedKubernetesClient = defineKubernetesClient(kubeConfig);
        return definedKubernetesClient.pods()
                .inNamespace(namespace)
                .list()
                .getItems();
    }

    @Override
    public List<Namespace> getListNamespaces(Path kubeConfig) {
        var definedKubernetesClient = defineKubernetesClient(kubeConfig);
        return definedKubernetesClient.namespaces()
                .list()
                .getItems();
    }

    @Override
    public Secret getSecretByNamespaceAndSecretName(String namespace, String secretName) {
        return kubernetesClient.secrets()
                .inNamespace(namespace)
                .withName(secretName)
                .get();
    }

    @Override
    public String getLogs(String namespace, String workloadInstanceName, Path kubeConfig) {
        var definedKubernetesClient = defineKubernetesClient(kubeConfig);
        return definedKubernetesClient.pods()
                .inNamespace(namespace)
                .withLabel(LABEL_CONSTANT, workloadInstanceName)
                .list().getItems().stream()
                .filter(Objects::nonNull)
                .map(item -> constructOutput(getPodName(item), getContainerInfo(item)))
                .collect(Collectors.joining(NEW_LINE));
    }

    @Override
    public String getLogsByNamespace(String namespace, Path kubeConfig) {
        var definedKubernetesClient = defineKubernetesClient(kubeConfig);
        return definedKubernetesClient.pods()
                .inNamespace(namespace)
                .list().getItems().stream()
                .map(item -> constructOutput(getPodName(item), getContainerInfo(item)))
                .collect(Collectors.joining(NEW_LINE));
    }

    @Override
    public void cleanResourcesByNamespaceAndInstanceName(String namespace, String labelValue, Path kubeconfig) {
        log.info("Starting to delete resources...");
        var definedKubernetesClient = defineKubernetesClient(kubeconfig);
        deleteDeployments(namespace, labelValue, definedKubernetesClient);
        deleteStatefulSets(namespace, labelValue, definedKubernetesClient);
        deleteReplicaSets(namespace, labelValue, definedKubernetesClient);
        deletePods(namespace, labelValue, definedKubernetesClient);
        deleteIngresses(namespace, labelValue, definedKubernetesClient);
        deleteServices(namespace, labelValue, definedKubernetesClient);
        deleteConfigMaps(namespace, labelValue, definedKubernetesClient);
        deleteSecrets(namespace, labelValue, definedKubernetesClient);
        deleteNetworkPolicy(namespace, labelValue, definedKubernetesClient);
        deletePodDisruptionBudget(namespace, labelValue, definedKubernetesClient);
        deleteServiceAccount(namespace, labelValue, definedKubernetesClient);
        deleteBatchJobs(namespace, labelValue, definedKubernetesClient);
        deleteRoleBinding(namespace, labelValue, definedKubernetesClient);
        deleteRoles(namespace, labelValue, definedKubernetesClient);
        deletePVC(namespace, labelValue, definedKubernetesClient);
    }

    @Override
    public void deleteNamespace(String namespace, Path kubeConfig) {
        defineKubernetesClient(kubeConfig)
                .namespaces()
                .withName(namespace)
                .delete();
    }

    @Override
    public void createSecretAndNamespaceIfRequired(Path kubeConfig, String namespace, Secret secret) {
        KubernetesClient client = defineKubernetesClient(kubeConfig);
        if (client.namespaces().withName(namespace).get() == null) {
            client.namespaces().resource(mapToNamespace(namespace)).create();
            log.info("Namespace {} was created successfully", namespace);
        }
        createOrReplaceSecret(secret, client, namespace);
    }

    @Override
    public void deleteSecret(String kubeConfig, String secretName, String namespace) {
        defineKubernetesClient(kubeConfig)
                .secrets().inNamespace(namespace).withName(secretName)
                .delete();
    }

    private Namespace mapToNamespace(String namespace) {
        return new NamespaceBuilder()
                .withNewMetadata()
                .withName(namespace)
                .addToLabels(NS_METADATA_LABELS_NAME, namespace)
                .endMetadata()
                .build();
    }

    private void createOrReplaceSecret(Secret secret, KubernetesClient client, String namespaceName) {
        client.resource(secret)
                .inNamespace(namespaceName)
                .createOrReplace();
    }

    private KubernetesClient defineKubernetesClient(Path kubeconfig) {
        return Optional.ofNullable(kubeconfig)
                .map(this::createKubernetesClientFromConfig)
                .orElse(kubernetesClient);
    }

    private KubernetesClient defineKubernetesClient(String kubeconfig) {
        return Optional.ofNullable(kubeconfig)
                .map(this::createKubernetesClientFromConfig)
                .orElse(kubernetesClient);
    }

    private void deleteSecrets(String namespace, String labelValue, KubernetesClient definedKubernetesClient) {
        definedKubernetesClient.secrets()
                .inNamespace(namespace)
                .withLabel(LABEL_CONSTANT, labelValue)
                .delete();
        log.info("Secrets in namespace={} by label {}={}, were deleted", namespace, LABEL_CONSTANT, labelValue);
    }

    private void deleteConfigMaps(String namespace, String labelValue, KubernetesClient definedKubernetesClient) {
        definedKubernetesClient.configMaps()
                .inNamespace(namespace)
                .withLabel(LABEL_CONSTANT, labelValue)
                .delete();
        log.info("ConfigMaps in namespace={} by label {}={}, were deleted", namespace, LABEL_CONSTANT, labelValue);
    }

    private void deleteServices(String namespace, String labelValue, KubernetesClient definedKubernetesClient) {
        definedKubernetesClient.services()
                .inNamespace(namespace)
                .withLabel(LABEL_CONSTANT, labelValue)
                .delete();
        log.info("Services in namespace={} by label {}={}, were deleted", namespace, LABEL_CONSTANT, labelValue);
    }

    private void deleteIngresses(String namespace, String labelValue, KubernetesClient definedKubernetesClient) {
        definedKubernetesClient.network()
                .v1()
                .ingresses()
                .inNamespace(namespace)
                .withLabel(LABEL_CONSTANT, labelValue)
                .delete();
        log.info("Ingresses in namespace={} by label {}={}, were deleted", namespace, LABEL_CONSTANT, labelValue);
    }

    private void deletePods(String namespace, String labelValue, KubernetesClient definedKubernetesClient) {
        definedKubernetesClient.pods()
                .inNamespace(namespace)
                .withLabel(LABEL_CONSTANT, labelValue)
                .delete();
        log.info("Pods in namespace={} by label {}={}, were deleted", namespace, LABEL_CONSTANT, labelValue);
    }

    private void deleteReplicaSets(String namespace, String labelValue, KubernetesClient definedKubernetesClient) {
        definedKubernetesClient.apps()
                .replicaSets()
                .inNamespace(namespace)
                .withLabel(LABEL_CONSTANT, labelValue)
                .delete();
        log.info("ReplicaSets in namespace={} by label {}={}, were deleted", namespace, LABEL_CONSTANT, labelValue);
    }

    private void deleteStatefulSets(String namespace, String labelValue, KubernetesClient definedKubernetesClient) {
        definedKubernetesClient.apps()
                .statefulSets()
                .inNamespace(namespace)
                .withLabel(LABEL_CONSTANT, labelValue)
                .delete();
        log.info("StatefulSets in namespace={} by label {}={}, were deleted", namespace, LABEL_CONSTANT, labelValue);
    }

    private void deleteDeployments(String namespace, String labelValue, KubernetesClient definedKubernetesClient) {
        definedKubernetesClient.apps()
                .deployments()
                .inNamespace(namespace)
                .withLabel(LABEL_CONSTANT, labelValue)
                .delete();
        log.info("Deployments in namespace={} by label {}={}, were deleted", namespace, LABEL_CONSTANT, labelValue);
    }

    private void deleteNetworkPolicy(String namespace, String labelValue, KubernetesClient definedKubernetesClient) {
        definedKubernetesClient.network()
                .networkPolicies()
                .inNamespace(namespace)
                .withLabel(LABEL_CONSTANT, labelValue)
                .delete();
        log.info("NetworkPolicy in namespace={} by label {}={}, were deleted", namespace, LABEL_CONSTANT, labelValue);
    }

    private void deleteBatchJobs(String namespace, String labelValue, KubernetesClient definedKubernetesClient) {
        definedKubernetesClient.batch().v1().jobs()
                .inNamespace(namespace)
                .withLabel(LABEL_CONSTANT, labelValue)
                .delete();
        log.info("BatchJobs in namespace={} by label {}={}, were deleted", namespace, LABEL_CONSTANT, labelValue);
    }

    private void deleteRoleBinding(String namespace, String labelValue, KubernetesClient definedKubernetesClient) {
        definedKubernetesClient.rbac().roleBindings()
                .inNamespace(namespace)
                .withLabel(LABEL_CONSTANT, labelValue)
                .delete();
        log.info("RoleBindings in namespace={} by label {}={}, were deleted", namespace, LABEL_CONSTANT, labelValue);
    }

    private void deletePodDisruptionBudget(String namespace, String labelValue, KubernetesClient definedKubernetesClient) {
        definedKubernetesClient.policy().v1().podDisruptionBudget()
                .inNamespace(namespace)
                .withLabel(LABEL_CONSTANT, labelValue)
                .delete();
        log.info("PodDisruptionBudget( in namespace={} by label {}={}, were deleted", namespace, LABEL_CONSTANT, labelValue);
    }

    private void deleteServiceAccount(String namespace, String labelValue, KubernetesClient definedKubernetesClient) {
        definedKubernetesClient.serviceAccounts()
                .inNamespace(namespace)
                .withLabel(LABEL_CONSTANT, labelValue)
                .delete();
        log.info("ServiceAccount in namespace={} by label {}={}, were deleted", namespace, LABEL_CONSTANT, labelValue);
    }

    private void deleteRoles(String namespace, String labelValue, KubernetesClient definedKubernetesClient) {
        definedKubernetesClient.rbac().roles()
                .inNamespace(namespace)
                .withLabel(LABEL_CONSTANT, labelValue)
                .delete();
        log.info("Roles in namespace={} by label {}={}, were deleted", namespace, LABEL_CONSTANT, labelValue);
    }

    private void deletePVC(String namespace, String labelValue, KubernetesClient definedKubernetesClient) {
        definedKubernetesClient.persistentVolumeClaims()
                .inNamespace(namespace)
                .withLabel(LABEL_CONSTANT, labelValue)
                .delete();
        log.info("PVC in namespace={} by label {}={}, were deleted", namespace, LABEL_CONSTANT, labelValue);
    }

    private KubernetesClient createKubernetesClientFromConfig(Path kubeConfig) {
        var config = Config.fromKubeconfig(fileService.readStringFromPath(kubeConfig));
        return new KubernetesClientBuilder().withConfig(config).build();
    }

    private KubernetesClient createKubernetesClientFromConfig(String kubeConfig) {
        Config config = Config.fromKubeconfig(kubeConfig);
        return new KubernetesClientBuilder().withConfig(config).build();
    }

    private String getContainerInfo(Pod pod) {
        var result = new StringBuilder();
        for (ContainerStatus status: pod.getStatus().getContainerStatuses()) {
            result.append(status.getName()).append(NEW_LINE);
            ContainerState state = status.getState();
            if (state != null) {
                result.append(RUNNING).append(getStateInfo(state.getRunning())).append(NEW_LINE);
                result.append(TERMINATED).append(getStateInfo(state.getTerminated())).append(NEW_LINE);
                result.append(WAITING).append(getStateInfo(state.getWaiting())).append(NEW_LINE);
            }
        }
        return result.toString();
    }

    private String getStateInfo(KubernetesResource resource) {
        return Objects.isNull(resource) ? "null" : resource.toString();
    }

    private String constructOutput(String podName, String containerInfo) {
        return "Pod " + podName + ": \n" + containerInfo;
    }

    private String getPodName(Pod pod) {
        return pod.getMetadata().getName();
    }

    private String getPodStatus(Pod pod) {
        return pod.getStatus().getPhase();
    }

}
