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

import com.ericsson.oss.management.lcm.model.internal.PrivateKeyData;

import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateRunning;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStateWaiting;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyBuilder;
import io.fabric8.kubernetes.api.model.policy.v1.PodDisruptionBudgetBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@EnableKubernetesMockClient(https = false, crud = true)
@ExtendWith(MockitoExtension.class)
class KubernetesServiceImplTest {
    private static final String NAMESPACE = "namespace";
    private static final String SECRET_NAME = "someSecretName";
    private static final String SECRET_KIND = "Secret";
    private static final String CRT_NAME = "test.crt";
    private static final String KEY_NAME = "test.key";
    private static final String CRT_VALUE = "someCertificateResult";
    private static final String KEY_VALUE = "someKeyResult";
    private static final String POD_STATUS_RUNNING = "Running";
    private static final String PROPERTY_NAME = "property1";
    private static final String PROPERTY_VALUE = "testProperty";
    private static final String TEST_WAITING_MESSAGE = "Test waiting message";
    private static final String TEST_LABEL = "testLabel";
    private static final String POD_NAME = "Pod_1";
    private static final String KUBERNETES_INSTANCE_KEY = "app.kubernetes.io/instance";
    private static final String RESOURCE_NAME = "test-resource";

    private KubernetesClient kubernetesClient;
    @Mock
    private FileService fileService;

    @Test
    void shouldReturnSecretSuccessfully() {
        KubernetesService kubernetesService = new KubernetesServiceImpl(kubernetesClient, fileService);

        kubernetesService.createOrReplaceSecretInNamespace(NAMESPACE, getSecret(), null);

        Secret secret = kubernetesClient.secrets().inNamespace(NAMESPACE).withName(SECRET_NAME).get();

        assertThat(secret).isNotNull();
        assertThat(secret.getData()).containsEntry(CRT_NAME, CRT_VALUE)
                .containsEntry(KEY_NAME, KEY_VALUE);
        assertThat(secret.getMetadata().getNamespace()).isEqualTo(NAMESPACE);
        assertThat(secret.getMetadata().getName()).isEqualTo(SECRET_NAME);
        assertThat(secret.getKind()).isEqualTo(SECRET_KIND);
    }

    @Test
    void shouldUpdateStringDataFieldInTheSecretAndReturnUpdatedSecret() {
        ObjectMeta metadata = new ObjectMetaBuilder().withName(SECRET_NAME).withNamespace(NAMESPACE).build();
        kubernetesClient.secrets().inNamespace(NAMESPACE).resource(new SecretBuilder().withKind(SECRET_KIND)
                .withMetadata(metadata).build()).create();
        KubernetesService kubernetesService = new KubernetesServiceImpl(kubernetesClient, fileService);
        PrivateKeyData testDataKey = new PrivateKeyData(KEY_NAME, KEY_VALUE);

        kubernetesService.saveKeyToSecret(NAMESPACE, SECRET_NAME, testDataKey);

        Secret secret = kubernetesClient.secrets().inNamespace(NAMESPACE).withName(SECRET_NAME).get();
        assertThat(secret).isNotNull();
        assertThat(secret.getStringData()).hasSize(1).containsEntry(KEY_NAME, KEY_VALUE);
    }

    @Test
    void shouldReturnMapWherePodWithStatusRunning() {
        KubernetesService kubernetesService = new KubernetesServiceImpl(kubernetesClient, fileService);
        createPod();

        Map<String, String> result = kubernetesService.getPodsStatusInfo(NAMESPACE, null);

        assertThat(result).hasSize(1).containsEntry(POD_NAME, POD_STATUS_RUNNING);
    }

    @Test
    void shouldReturnSecretBySecretNameAndNamespace() {
        KubernetesService kubernetesService = new KubernetesServiceImpl(kubernetesClient, fileService);
        kubernetesClient.secrets().inNamespace(NAMESPACE).resource(getSecret()).create();

        Secret result = kubernetesService.getSecretByNamespaceAndSecretName(NAMESPACE, SECRET_NAME);

        assertThat(result).isNotNull();
        assertThat(result.getMetadata()).isNotNull();
        assertThat(result.getMetadata().getName()).isEqualTo(SECRET_NAME);
        assertThat(result.getMetadata().getNamespace()).isEqualTo(NAMESPACE);
    }

    @Test
    void shouldReturnPodLogs() {
        KubernetesService kubernetesService = new KubernetesServiceImpl(kubernetesClient, fileService);
        getPodWithAdditionalInfo();

        String result = kubernetesService.getLogs(NAMESPACE, TEST_LABEL, null);
        assertThat(result).contains(TEST_WAITING_MESSAGE).endsWith("\n");
    }

    @Test
    void shouldReturnPodLogsWithoutWorkloadInstanceNameLabel() {
        KubernetesService kubernetesService = new KubernetesServiceImpl(kubernetesClient, fileService);
        getPodWithAdditionalInfo();

        String result = kubernetesService.getLogsByNamespace(NAMESPACE, null);
        assertThat(result).contains(TEST_WAITING_MESSAGE).endsWith("\n");
    }

    @Test
    void shouldDeleteAllResourcesWithSpecificLabelInNamespace() {
        KubernetesService kubernetesService = new KubernetesServiceImpl(kubernetesClient, fileService);
        initializeTestResourcesWithInstanceLabel(RESOURCE_NAME, TEST_LABEL);
        String instanceNameValue = "generic-resources";
        initializeTestResourcesWithInstanceLabel("must-be-present", instanceNameValue);

        kubernetesService.cleanResourcesByNamespaceAndInstanceName(NAMESPACE, TEST_LABEL, null);

        assertThatResourcesDeletedOnlyWithCertainLabel(instanceNameValue);
    }

    @Test
    void shouldNotThrowExceptionIfAreNoResourcesWithLabel() {
        var kubernetesService = new KubernetesServiceImpl(kubernetesClient, fileService);

        assertDoesNotThrow(() -> kubernetesService.cleanResourcesByNamespaceAndInstanceName(NAMESPACE, TEST_LABEL, null));
    }

    @Test
    void shouldDeleteNamespace() {
        var kubernetesService = new KubernetesServiceImpl(kubernetesClient, fileService);
        kubernetesClient.resource(buildNamespace()).create();

        kubernetesService.deleteNamespace(NAMESPACE, null);

        boolean namespaceDeleted = kubernetesClient.namespaces().withName(NAMESPACE).get() == null;
        assertThat(namespaceDeleted).isTrue();
    }

    @Test
    void shouldCreateSecretAndNamespace() {
        var kubernetesService = new KubernetesServiceImpl(kubernetesClient, fileService);
        kubernetesService.createSecretAndNamespaceIfRequired(null, NAMESPACE, getSecret());

        NamespaceList namespaces = kubernetesClient.namespaces().list();
        assertThat(namespaces.getItems().get(0).getMetadata().getName()).isEqualTo(NAMESPACE);

        SecretList secrets = kubernetesClient.secrets().inNamespace(NAMESPACE).list();
        assertThat(secrets.getItems().get(0).getMetadata().getName()).isEqualTo(SECRET_NAME);
    }

    @Test
    void shouldGetListNamespaces() {
        var kubernetesService = new KubernetesServiceImpl(kubernetesClient, fileService);
        Namespace namespace = buildNamespace();
        kubernetesClient.resource(namespace).create();

        List<Namespace> namespaces = kubernetesService.getListNamespaces(null);
        assertThat(namespaces.get(0).getMetadata().getName()).isEqualTo(NAMESPACE);
    }

    @Test
    void shouldCreateSecretInAlreadyCreatedNamespace() {
        var kubernetesService = new KubernetesServiceImpl(kubernetesClient, fileService);
        Namespace namespace = buildNamespace();
        kubernetesClient.resource(namespace).create();
        kubernetesService.createSecretAndNamespaceIfRequired(null, namespace.getMetadata().getName(), getSecret());

        SecretList secrets = kubernetesClient.secrets().inNamespace(NAMESPACE).list();
        assertThat(secrets.getItems().get(0).getMetadata().getName()).isEqualTo(SECRET_NAME);
    }

    @Test
    void shouldReplaceSecretWhenAlreadyExist() {
        var kubernetesService = new KubernetesServiceImpl(kubernetesClient, fileService);
        Namespace namespace = buildNamespace();
        kubernetesClient.resource(namespace).create();
        Secret secret = getSecret();
        kubernetesClient.resource(secret).create();
        kubernetesService.createSecretAndNamespaceIfRequired(null, namespace.getMetadata().getName(), secret);

        SecretList secrets = kubernetesClient.secrets().inNamespace(NAMESPACE).list();
        assertThat(secrets.getItems().get(0).getMetadata().getName()).isEqualTo(SECRET_NAME);
    }

    @Test
    void shouldDeleteSecretSuccessfully() {
        var kubernetesService = new KubernetesServiceImpl(kubernetesClient, fileService);
        Namespace namespace = buildNamespace();
        kubernetesClient.resource(namespace).create();
        Secret secret = getSecret();
        kubernetesClient.resource(secret).create();

        kubernetesService.deleteSecret(null, SECRET_NAME, NAMESPACE);

        SecretList secrets = kubernetesClient.secrets().inNamespace(NAMESPACE).list();
        assertThat(secrets.getItems()).isEmpty();
    }

    @Test
    void shouldProcessDeleteSecretSuccessfullyWhenNoSecret() {
        var kubernetesService = new KubernetesServiceImpl(kubernetesClient, fileService);
        Namespace namespace = buildNamespace();
        kubernetesClient.resource(namespace).create();

        kubernetesService.deleteSecret(null, SECRET_NAME, NAMESPACE);

        SecretList secrets = kubernetesClient.secrets().inNamespace(NAMESPACE).list();
        assertThat(secrets.getItems()).isEmpty();
    }

    private Secret getSecret() {
        Map<String, String> certificatesAndKeys = new HashMap<>();
        certificatesAndKeys.put(CRT_NAME, CRT_VALUE);
        certificatesAndKeys.put(KEY_NAME, KEY_VALUE);
        ObjectMeta metadata = new ObjectMeta();
        metadata.setNamespace(NAMESPACE);
        metadata.setName(SECRET_NAME);

        Secret secret = new Secret();
        secret.setKind(SECRET_KIND);
        secret.setData(certificatesAndKeys);
        secret.setMetadata(metadata);
        return secret;
    }

    private void createPod() {
        PodStatus status = new PodStatus();
        status.setPhase(KubernetesServiceImplTest.POD_STATUS_RUNNING);
        kubernetesClient.pods()
                .inNamespace(NAMESPACE)
                .resource(new PodBuilder().withNewMetadata()
                                .withName(KubernetesServiceImplTest.POD_NAME)
                                .endMetadata()
                                .withStatus(status)
                                .build())
                .create();
    }

    private void getPodWithAdditionalInfo() {
        PodStatus status = new PodStatus();
        status.setContainerStatuses(List.of(getRunning(), getWaiting(), getTerminated()));

        kubernetesClient.pods()
                .inNamespace(NAMESPACE)
                .resource(new PodBuilder().withNewMetadata()
                        .withName(KubernetesServiceImplTest.POD_NAME)
                        .addToLabels(KUBERNETES_INSTANCE_KEY, TEST_LABEL)
                        .endMetadata()
                        .withStatus(status)
                        .build())
                .create();
    }

    private void createDeploymentWithInstanceLabel(String deploymentName, String instanceNameValue) {
        kubernetesClient.apps()
                .deployments()
                .inNamespace(NAMESPACE)
                .resource(new DeploymentBuilder()
                        .withNewMetadata()
                        .withName("deployment-" + deploymentName)
                        .addToLabels(KUBERNETES_INSTANCE_KEY, instanceNameValue)
                        .endMetadata()
                        .build())
                .create();
    }

    private void createStatefulSetWithInstanceLabel(String statefulSetName, String instanceNameValue) {
        kubernetesClient.apps()
                .statefulSets()
                .inNamespace(NAMESPACE)
                .resource(new StatefulSetBuilder()
                        .withNewMetadata()
                        .withName("stateful-set-" + statefulSetName)
                        .addToLabels(KUBERNETES_INSTANCE_KEY, instanceNameValue)
                        .endMetadata()
                        .build())
                .create();
    }

    private void createReplicaSetWithInstanceLabel(String replicaSetName, String instanceNameValue) {
        kubernetesClient.apps()
                .replicaSets()
                .inNamespace(NAMESPACE)
                .resource(new ReplicaSetBuilder()
                        .withNewMetadata()
                        .withName("replica-set-" + replicaSetName)
                        .addToLabels(KUBERNETES_INSTANCE_KEY, instanceNameValue)
                        .endMetadata()
                        .build())
                .create();
    }

    private void createPodWithInstanceLabel(String podName, String instanceNameValue) {
        kubernetesClient.pods()
                .inNamespace(NAMESPACE)
                .resource(new PodBuilder().withNewMetadata()
                        .withName("pod-" + podName)
                        .addToLabels(KUBERNETES_INSTANCE_KEY, instanceNameValue)
                        .endMetadata()
                        .build())
                .create();
    }

    private void createServiceWithInstanceLabel(String serviceName, String instanceNameValue) {
        kubernetesClient.services()
                .inNamespace(NAMESPACE)
                .resource(new ServiceBuilder()
                        .withNewMetadata()
                        .withName("service-" + serviceName)
                        .addToLabels(KUBERNETES_INSTANCE_KEY, instanceNameValue)
                        .endMetadata()
                        .build())
                .create();
    }

    private void createIngressWithInstanceLabel(String ingressName, String instanceNameValue) {
        kubernetesClient.network()
                .v1()
                .ingresses()
                .inNamespace(NAMESPACE)
                .resource(new IngressBuilder()
                        .withNewMetadata()
                        .withName("ingress-" + ingressName)
                        .addToLabels(KUBERNETES_INSTANCE_KEY, instanceNameValue)
                        .endMetadata()
                        .build())
                .create();
    }

    private void createSecretWithInstanceLabel(String secretName, String instanceNameValue) {
        kubernetesClient.secrets()
                .inNamespace(NAMESPACE)
                .resource(new SecretBuilder()
                        .withNewMetadata()
                        .withName("secret-" + secretName)
                        .addToLabels(KUBERNETES_INSTANCE_KEY, instanceNameValue)
                        .endMetadata()
                        .build())
                .create();
    }

    private void createConfigMapWithInstanceLabel(String configMapName, String instanceNameValue) {
        kubernetesClient.configMaps()
                .inNamespace(NAMESPACE)
                .resource(new ConfigMapBuilder()
                        .withNewMetadata()
                        .withName("config-map-" + configMapName)
                        .addToLabels(KUBERNETES_INSTANCE_KEY, instanceNameValue)
                        .endMetadata()
                        .build())
                .create();
    }

    private void createNetworkPolicyWithInstanceLabel(String resourceName, String instanceNameValue) {
        kubernetesClient.network().networkPolicies()
                .inNamespace(NAMESPACE)
                .resource(new NetworkPolicyBuilder()
                        .withNewMetadata()
                        .withName("network-policy-" + resourceName)
                        .addToLabels(KUBERNETES_INSTANCE_KEY, instanceNameValue)
                        .endMetadata()
                        .build())
                .create();
    }

    private void createPodDisruptionBudgetWithInstanceLabel(String resourceName, String instanceNameValue) {
        kubernetesClient.policy().v1().podDisruptionBudget()
                .inNamespace(NAMESPACE)
                .resource(new PodDisruptionBudgetBuilder()
                        .withNewMetadata()
                        .withName("pod-disruption-budget-" + resourceName)
                        .addToLabels(KUBERNETES_INSTANCE_KEY, instanceNameValue)
                        .endMetadata()
                        .build())
                .create();
    }

    private void createServiceAccountWithInstanceLabel(String resourceName, String instanceNameValue) {
        kubernetesClient.serviceAccounts()
                .inNamespace(NAMESPACE)
                .resource(new ServiceAccountBuilder()
                        .withNewMetadata()
                        .withName("service-account-" + resourceName)
                        .addToLabels(KUBERNETES_INSTANCE_KEY, instanceNameValue)
                        .endMetadata()
                        .build())
                .create();
    }

    private void createBatchJobsWithInstanceLabel(String resourceName, String instanceNameValue) {
        kubernetesClient.batch().v1().jobs()
                .inNamespace(NAMESPACE)
                .resource(new JobBuilder()
                        .withNewMetadata()
                        .withName("batch-job-" + resourceName)
                        .addToLabels(KUBERNETES_INSTANCE_KEY, instanceNameValue)
                        .endMetadata()
                        .build())
                .create();
    }

    private void createRoleBindingWithInstanceLabel(String resourceName, String instanceNameValue) {
        kubernetesClient.rbac().roleBindings()
                .inNamespace(NAMESPACE)
                .resource(new RoleBindingBuilder()
                        .withNewMetadata()
                        .withName("role-binding-" + resourceName)
                        .addToLabels(KUBERNETES_INSTANCE_KEY, instanceNameValue)
                        .endMetadata()
                        .build())
                .create();
    }

    private void createRolesWithInstanceLabel(String resourceName, String instanceNameValue) {
        kubernetesClient.rbac().roles()
                .inNamespace(NAMESPACE)
                .resource(new RoleBuilder()
                        .withNewMetadata()
                        .withName("role-" + resourceName)
                        .addToLabels(KUBERNETES_INSTANCE_KEY, instanceNameValue)
                        .endMetadata()
                        .build())
                .create();
    }

    private void createPVCWithInstanceLabel(String resourceName, String instanceNameValue) {
        kubernetesClient.persistentVolumeClaims()
                .inNamespace(NAMESPACE)
                .resource(new PersistentVolumeClaimBuilder()
                        .withNewMetadata()
                        .withName("pvc-" + resourceName)
                        .addToLabels(KUBERNETES_INSTANCE_KEY, instanceNameValue)
                        .endMetadata()
                        .build())
                .create();
    }

    private Namespace buildNamespace() {
        Namespace namespace = new Namespace();
        ObjectMeta metadata = new ObjectMeta();
        metadata.setName(NAMESPACE);
        namespace.setMetadata(metadata);
        return namespace;
    }

    private void initializeTestResourcesWithInstanceLabel(String resourcesGenericName, String instanceNameValue) {
        createDeploymentWithInstanceLabel(resourcesGenericName, instanceNameValue);
        createStatefulSetWithInstanceLabel(resourcesGenericName, instanceNameValue);
        createReplicaSetWithInstanceLabel(resourcesGenericName, instanceNameValue);
        createPodWithInstanceLabel(resourcesGenericName, instanceNameValue);
        createServiceWithInstanceLabel(resourcesGenericName, instanceNameValue);
        createIngressWithInstanceLabel(resourcesGenericName, instanceNameValue);
        createSecretWithInstanceLabel(resourcesGenericName, instanceNameValue);
        createConfigMapWithInstanceLabel(resourcesGenericName, instanceNameValue);
        createNetworkPolicyWithInstanceLabel(resourcesGenericName, instanceNameValue);
        createPodDisruptionBudgetWithInstanceLabel(resourcesGenericName, instanceNameValue);
        createServiceAccountWithInstanceLabel(resourcesGenericName, instanceNameValue);
        createBatchJobsWithInstanceLabel(resourcesGenericName, instanceNameValue);
        createRoleBindingWithInstanceLabel(resourcesGenericName, instanceNameValue);
        createRolesWithInstanceLabel(resourcesGenericName, instanceNameValue);
        createPVCWithInstanceLabel(resourcesGenericName, instanceNameValue);
    }

    private ContainerStatus getRunning() {
        ContainerStatus containerStatus = new ContainerStatus();
        ContainerState state = new ContainerState();
        ContainerStateRunning containerStateRunning = new ContainerStateRunning();

        containerStateRunning.setAdditionalProperty(PROPERTY_NAME, PROPERTY_VALUE);
        containerStateRunning.setStartedAt("28/11/2022");
        state.setRunning(containerStateRunning);
        containerStatus.setState(state);
        containerStatus.setName("testContainerRunning");

        return containerStatus;
    }

    private ContainerStatus getWaiting() {
        ContainerStatus containerStatus = new ContainerStatus();
        ContainerState state = new ContainerState();
        ContainerStateWaiting containerStateWaiting = new ContainerStateWaiting();

        containerStateWaiting.setAdditionalProperty(PROPERTY_NAME, PROPERTY_VALUE);
        containerStateWaiting.setMessage(TEST_WAITING_MESSAGE);
        state.setWaiting(containerStateWaiting);
        containerStatus.setState(state);
        containerStatus.setName("testContainerWaiting");

        return containerStatus;
    }

    private ContainerStatus getTerminated() {
        ContainerStatus containerStatus = new ContainerStatus();
        ContainerState state = new ContainerState();
        ContainerStateTerminated containerStateTerminated = new ContainerStateTerminated();

        containerStateTerminated.setAdditionalProperty(PROPERTY_NAME, PROPERTY_VALUE);
        containerStateTerminated.setMessage("Test terminated message");
        containerStateTerminated.setReason("Test terminated reason");
        state.setTerminated(containerStateTerminated);
        containerStatus.setState(state);
        containerStatus.setName("testContainerTerminated");

        return containerStatus;
    }

    private void assertThatResourcesDeletedOnlyWithCertainLabel(String instanceNameValue) {
        var deploymentList = kubernetesClient.apps().deployments().inNamespace(NAMESPACE).list();
        assertThat(deploymentList).isNotNull();
        assertThat(deploymentList.getItems()).hasSize(1)
                .allMatch(deployment -> deployment.getMetadata().getLabels().containsValue(instanceNameValue));
        var statefulSetList = kubernetesClient.apps().statefulSets().inNamespace(NAMESPACE).list();
        assertThat(statefulSetList).isNotNull();
        assertThat(statefulSetList.getItems()).hasSize(1)
                .allMatch(statefulSet -> statefulSet.getMetadata().getLabels().containsValue(instanceNameValue));
        var replicaSetList = kubernetesClient.apps().replicaSets().inNamespace(NAMESPACE).list();
        assertThat(replicaSetList).isNotNull();
        assertThat(replicaSetList.getItems()).hasSize(1)
                .allMatch(replicaSet -> replicaSet.getMetadata().getLabels().containsValue(instanceNameValue));
        var podList = kubernetesClient.pods().inNamespace(NAMESPACE).list();
        assertThat(podList).isNotNull();
        assertThat(podList.getItems()).hasSize(1)
                .allMatch(pod -> pod.getMetadata().getLabels().containsValue(instanceNameValue));
        var servicesList = kubernetesClient.services().inNamespace(NAMESPACE).list();
        assertThat(servicesList).isNotNull();
        assertThat(servicesList.getItems()).hasSize(1)
                .allMatch(service -> service.getMetadata().getLabels().containsValue(instanceNameValue));
        var ingressList = kubernetesClient.network().v1().ingresses().inNamespace(NAMESPACE).list();
        assertThat(ingressList).isNotNull();
        assertThat(ingressList.getItems()).hasSize(1)
                .allMatch(ingress -> ingress.getMetadata().getLabels().containsValue(instanceNameValue));
        var secretList = kubernetesClient.secrets().inNamespace(NAMESPACE).list();
        assertThat(secretList).isNotNull();
        assertThat(secretList.getItems()).hasSize(1)
                .allMatch(secret -> secret.getMetadata().getLabels().containsValue(instanceNameValue));
        var configMapList = kubernetesClient.configMaps().inNamespace(NAMESPACE).list();
        assertThat(configMapList).isNotNull();
        assertThat(configMapList.getItems()).hasSize(1)
                .allMatch(configMap -> configMap.getMetadata().getLabels().containsValue(instanceNameValue));
        var networkPolicyList = kubernetesClient.network().networkPolicies().inNamespace(NAMESPACE).list();
        assertThat(networkPolicyList).isNotNull();
        assertThat(networkPolicyList.getItems()).hasSize(1)
                .allMatch(networkPolicy -> networkPolicy.getMetadata().getLabels().containsValue(instanceNameValue));
        var podDisruptionBudgetList = kubernetesClient.policy().v1().podDisruptionBudget().inNamespace(NAMESPACE).list();
        assertThat(podDisruptionBudgetList).isNotNull();
        assertThat(podDisruptionBudgetList.getItems()).hasSize(1)
                .allMatch(podDisruptionBudget -> podDisruptionBudget.getMetadata().getLabels().containsValue(instanceNameValue));
        var serviceAccountList = kubernetesClient.serviceAccounts().inNamespace(NAMESPACE).list();
        assertThat(serviceAccountList).isNotNull();
        assertThat(serviceAccountList.getItems()).hasSize(1)
                .allMatch(serviceAccount -> serviceAccount.getMetadata().getLabels().containsValue(instanceNameValue));
        var batchJobList = kubernetesClient.batch().v1().jobs().inNamespace(NAMESPACE).list();
        assertThat(batchJobList).isNotNull();
        assertThat(batchJobList.getItems()).hasSize(1)
                .allMatch(batchJob -> batchJob.getMetadata().getLabels().containsValue(instanceNameValue));
        var roleBindingList = kubernetesClient.rbac().roleBindings().inNamespace(NAMESPACE).list();
        assertThat(roleBindingList).isNotNull();
        assertThat(roleBindingList.getItems()).hasSize(1)
                .allMatch(roleBinding -> roleBinding.getMetadata().getLabels().containsValue(instanceNameValue));
        var roleList = kubernetesClient.rbac().roles().inNamespace(NAMESPACE).list();
        assertThat(roleList).isNotNull();
        assertThat(roleList.getItems()).hasSize(1)
                .allMatch(role -> role.getMetadata().getLabels().containsValue(instanceNameValue));
        var pvcList = kubernetesClient.persistentVolumeClaims().inNamespace(NAMESPACE).list();
        assertThat(pvcList).isNotNull();
        assertThat(pvcList.getItems()).hasSize(1)
                .allMatch(pvc -> pvc.getMetadata().getLabels().containsValue(instanceNameValue));
    }
}
