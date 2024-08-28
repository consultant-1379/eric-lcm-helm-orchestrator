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
package com.ericsson.oss.mgmt.lcm.acceptance.tests;

import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.CHARTS_PART;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.CLUSTER_CONFIG_INFO_PATH_LOCAL;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.ADDITIONAL_PARAMETERS;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.CLUSTER_CONFIG_INFO_PATH;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.COMPLETED_STATE;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.OPERATION_ID;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.OPERATION_TYPE_INSTANTIATE;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.OPERATION_TYPE_REINSTANTIATE;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.OPERATION_TYPE_ROLLBACK;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.OPERATION_TYPE_TERMINATE;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.OPERATION_TYPE_UPDATE;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.VALUES_PATH;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.WORKLOAD_INSTANCE_ID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import static com.ericsson.oss.mgmt.lcm.acceptance.steps.LifecycleOperations.deleteInstance;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.FileUtils.copyFile;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.KubernetesApiUtils.getPodsInNamespace;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.KubernetesApiUtils.getStatusesByPodName;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.KubernetesApiUtils.namespaceExist;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.KubernetesApiUtils.secretWithNameIsPresent;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.TestExecutionGlobalConfig.SERVICE_INSTANCE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.ericsson.oss.mgmt.lcm.acceptance.models.WorkloadInstanceTestDataWithUrl;
import com.ericsson.oss.mgmt.lcm.acceptance.steps.HelmChartRegistryOperations;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.awaitility.pollinterval.FibonacciPollInterval;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.annotations.Ignore;

import com.ericsson.oss.mgmt.lcm.acceptance.models.Chart;
import com.ericsson.oss.mgmt.lcm.acceptance.models.ClusterConnectionInfoTestData;
import com.ericsson.oss.mgmt.lcm.acceptance.models.HelmfileBuilderRequest;
import com.ericsson.oss.mgmt.lcm.acceptance.models.PagedOperation;
import com.ericsson.oss.mgmt.lcm.acceptance.models.RollbackData;
import com.ericsson.oss.mgmt.lcm.acceptance.models.Version;
import com.ericsson.oss.mgmt.lcm.acceptance.models.WorkloadInstanceTestData;
import com.ericsson.oss.mgmt.lcm.acceptance.steps.ClusterConnectionInfo;
import com.ericsson.oss.mgmt.lcm.acceptance.steps.DatabaseCleaner;
import com.ericsson.oss.mgmt.lcm.acceptance.steps.HealthCheck;
import com.ericsson.oss.mgmt.lcm.acceptance.steps.LifecycleOperations;
import com.ericsson.oss.mgmt.lcm.acceptance.steps.Setup;
import com.ericsson.oss.mgmt.lcm.acceptance.utils.FileUtils;
import com.ericsson.oss.mgmt.lcm.acceptance.utils.KubernetesApiUtils;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HelmSourceLCMTest {
    private static final String VALUES_TO_UPDATE_PATH = "src/main/resources/testData/instantiate/helmfile-test-for-update/values.yaml";
    private static final String VALUES_FUNC_ENTITY_PATH = "src/main/resources/testData/instantiate/helmfile-func-entity/values.yaml";
    private static final String VALUES_WITH_INGRESS_PATH = "src/main/resources/testData/instantiate/helmfile-test-with-ingress/values.yaml";
    private static final String HELMFILE_BUILDER_VALUES_PATH = "src/main/resources/testData/instantiate/helmfile-builder/values.yaml";
    private static final String HELMFILE_WITH_BRO_VALUES_PATH = "src/main/resources/testData/instantiate/helmfile-with-bro/values.yaml";
    private static final String HELMFILE_WITH_BRO_LOCATION = "src/main/resources/testData/instantiate/helmfile-with-bro/";
    private static final String HELMFILE_WITH_BUSYBOX_VALUES_PATH = "src/main/resources/testData/instantiate/helmfile-with-busybox/values.yaml";
    private static final String HELMFILE_INVALID_IMAGE_LOCATION = "src/main/resources/testData/instantiate/helmfile-with-invalid-image/";
    private static final String HELMFILE_INVALID_IMAGE_VALUES = "src/main/resources/testData/instantiate/helmfile-with-invalid-image/values.yaml";
    private static final String HELMFILE_WITH_BUSYBOX_LOCATION = "src/main/resources/testData/instantiate/helmfile-with-busybox/";
    private static final String HELM_SOURCE_LOCATION = "src/main/resources/testData/instantiate/helmfile-test/";
    private static final String HELM_SOURCE_NAME = "helmfile-test-1.2.3-4.tgz";
    private static final String BUSYBOX_TEST_TGZ  = "busybox-test-1.32.0.tgz";
    private static final String ERIC_CTRL_BRO_TEST_TGZ  = "eric-ctrl-bro-test-4.9.0-21.tgz";
    private static final String HELMFILE_BUILDER_WITH_ORDER_VALUES_PATH =
            "src/main/resources/testData/instantiate/helmfile-builder/helmfile-builder-with-order/values.yaml";
    private static final String CLUSTER_NAME = "hahn117";
    private static final String CLUSTER_NAME_LOCAL = "hahn117";
    private static final String REPOSITORY = "https://arm.sero.gic.ericsson.se/artifactory/proj-adp-gs-all-helm";
    private static final String APP_A_RELEASE_NAME = "cn-am-test-app-a";
    private static final String APP_B_RELEASE_NAME = "cn-am-test-app-b";
    private static final String APP_C_RELEASE_NAME = "cn-am-test-app-c";
    private static final String CRD_RELEASE_NAME = "cn-am-test-crd";
    private static final String DOCKER_SECRET_RELEASE_NAME = "regcred-%s";
    private static final String EXECUTE_CRD_COMMAND = "helm list --namespace eric-crd-ns";
    private static final String POD_NAME = "test-bro";
    private static final String POD_STATE_RUNNING = "running";
    private static final int TEST_TIMEOUT = 5;
    private static final String DATETIME_PATTERN = "\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}.\\d{3}";
    private static final String DATETIME_FORMATTER = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final String NEW_LINE = "[\\r\\n]+";
    private static final String SECRET_NAME = "ingress-demo-tls1";
    private static final String CERTIFICATES_PATH = "/tmp/certificates";
    private static final String SOURCE_PATH = "src/main/resources/testData/util";
    private static final String CHART_VERSION = "5.0.0-9";
    private static final String CHART_BRO_NAME = "eric-ctrl-bro";
    private static final String CHART_SERVER_NAME = "eric-pm-server";
    private static final String STATUS_CODE = "StatusCode";
    private static final String PATH_TO_INTEGRATION_CHART_FOLDER = "src/main/resources/testData/instantiate/integration-chart/";
    private static final String ERIC_CTRL_BRO_ENABLED = "eric-ctrl-bro.enabled";
    private static final String TEST_INTEGRATION_1080_TGZ = "test-integration-0.1.0-1080.tgz";
    private static final String TEST_INTEGRATION_1081_TGZ = "test-integration-0.1.0-1081.tgz";
    private static final String TEST_INTEGRATION_1082_TGZ = "test-integration-0.1.0-1082.tgz";
    private static final String BUSYBOX_SIMPLE_CHART_1_32_0_TGZ = "busybox-simple-chart-1.32.0.tgz";
    private static final String ARCHIVE_UPDATE_PATH = "archiveUpdate/";
    private static final String ARCHIVE_INSTANTIATE_PATH = "archiveInstantiate/";
    private static final String VALUES_INSTANTIATE_INTEGRATION_CHART = "src/main/resources/testData/instantiate/integration-chart/values.yaml";

    private static String namespace;
    private static String clusterName;
    private static String clusterConnectionInfoId;
    private static String clusterConfigInfoPath;
    private static String kubeConfigPath;
    private static String releaseListCommand;
    private static String helmfileLsCommand;
    private static KubernetesClient kubernetesClient;

    @BeforeClass
    public static void createClusterConnectionInfo() throws IOException {
        boolean isLocal = SERVICE_INSTANCE.getIsLocal();
        clusterConfigInfoPath = isLocal ? CLUSTER_CONFIG_INFO_PATH_LOCAL : CLUSTER_CONFIG_INFO_PATH;
        clusterName = isLocal ? CLUSTER_NAME_LOCAL : CLUSTER_NAME;
        kubeConfigPath = "--kubeconfig " + clusterConfigInfoPath;
        namespace = SERVICE_INSTANCE.getNamespace();
        releaseListCommand = "helm list --namespace " + namespace;
        helmfileLsCommand = "helm ls -n " + namespace;

        if (!isLocal) {
            copyFile(SERVICE_INSTANCE.getClusterConnectionInfoPath(), clusterConfigInfoPath);
        }
        Config config = Config.fromKubeconfig(Files.readString(Path.of(clusterConfigInfoPath)));
        log.info("Config context is: {}, cluster name {}", config.getCurrentContext().getContext(),
                 config.getCurrentContext().getContext().getCluster());
        kubernetesClient = new KubernetesClientBuilder().withConfig(config).build();
        final ClusterConnectionInfoTestData clusterConnectionInfoTestData = new ClusterConnectionInfoTestData();
        clusterConnectionInfoTestData.setPathToFile(clusterConfigInfoPath);
        ResponseEntity<HashMap<String, String>> response = ClusterConnectionInfo.uploadValidConnectionInfo(clusterConnectionInfoTestData);
        HashMap<String, String> responseBody = Optional.ofNullable(response.getBody()).orElseThrow();
        assertThat(responseBody).isNotNull();
        clusterConnectionInfoId = responseBody.get("id");
    }

    @AfterClass
    public static void removeClusterConnectionInfo() {
        ClusterConnectionInfo.removeClusterConnectionInfo(clusterConnectionInfoId);
    }

    @AfterTest
    public static void cleanDatabaseAfterTestsWithLocalProfile() {
        if (SERVICE_INSTANCE.getIsLocal()) {
            DatabaseCleaner.cleanDatabaseAfterTests();
        }
    }

    @Test(description = "Process of instantiating and terminating workloadInstance based on helmfile with deleting namespace")
    public void shouldSuccessfullyInstantiateHelmfileAndDeleteNamespaceAfterTerminate() {
        //Setup
        WorkloadInstanceTestData instance = new WorkloadInstanceTestData();
        instance.setHelmSourceLocation(HELMFILE_WITH_BRO_LOCATION);
        instance.setHelmSourceName(ERIC_CTRL_BRO_TEST_TGZ);
        instance.setWorkloadInstanceName("instantiate-helmfile-with-pod-entity");
        instance.setNamespace(namespace);
        instance.setValuesYaml(HELMFILE_WITH_BRO_VALUES_PATH);
        instance.setClusterConnectionInfo(clusterName);

        Setup.zipUpHelmfile(instance);

        //Health Check
        ResponseEntity<String> healthResponse = HealthCheck.getHealthState();
        checkHealthStatusOk(healthResponse);

        //Instantiate
        Object workloadInstanceResponse = instantiate(instance, getReleasesToCheck(false, false, false));

        //Terminate and delete namespace
        terminateAndDeleteInstance(workloadInstanceResponse, clusterConfigInfoPath, true);
    }

    @Test(description = "Process of instantiating and terminating two workloadInstances based on different helmfiles " +
            "with deleting namespace")
    public void shouldNotDeleteNsWhenNsNotEmptyForHelmfile() {
        //Setup instance with Bro
        WorkloadInstanceTestData instanceWithBro = new WorkloadInstanceTestData();
        instanceWithBro.setHelmSourceLocation(HELMFILE_WITH_BRO_LOCATION);
        instanceWithBro.setHelmSourceName(ERIC_CTRL_BRO_TEST_TGZ);
        instanceWithBro.setWorkloadInstanceName("instantiate-helmfile-with-bro");
        instanceWithBro.setNamespace(namespace);
        instanceWithBro.setValuesYaml(HELMFILE_WITH_BRO_VALUES_PATH);
        instanceWithBro.setClusterConnectionInfo(clusterName);

        //Setup instance with Busybox
        WorkloadInstanceTestData instanceWithBusyBox = new WorkloadInstanceTestData();
        instanceWithBusyBox.setHelmSourceLocation(HELMFILE_WITH_BUSYBOX_LOCATION);
        instanceWithBusyBox.setHelmSourceName(BUSYBOX_TEST_TGZ);
        instanceWithBusyBox.setWorkloadInstanceName("instantiate-helmfile-with-busybox");
        instanceWithBusyBox.setNamespace(namespace);
        instanceWithBusyBox.setValuesYaml(HELMFILE_WITH_BUSYBOX_VALUES_PATH);
        instanceWithBusyBox.setClusterConnectionInfo(clusterName);

        Setup.zipUpHelmfile(instanceWithBro);
        Setup.zipUpHelmfile(instanceWithBusyBox);

        //Health Check
        ResponseEntity<String> healthResponse = HealthCheck.getHealthState();
        checkHealthStatusOk(healthResponse);

        //Instantiate two instances
        Object workloadInstanceResponseWithBro = instantiate(instanceWithBro, getReleasesToCheck(false, false, false));
        Object workloadInstanceResponseWithBusybox = instantiate(instanceWithBusyBox, getReleasesToCheck(false, false, false));

        //Terminate instance with Bro and set flag delete namespace true
        ResponseEntity<String> terminate = LifecycleOperations.terminate(workloadInstanceResponseWithBro, clusterConfigInfoPath, true);
        String operationId = getOperationId(terminate.getHeaders());
        LifecycleOperations.verifyOperation(operationId, COMPLETED_STATE, OPERATION_TYPE_TERMINATE);

        //Check that namespace not deleted in case that not all instances in namespace are terminated
        checkNamespaceDeletion(false);

        //Delete instance with Bro
        ResponseEntity<String> deleteBroResponse = deleteInstance(workloadInstanceResponseWithBro);
        assertThat(deleteBroResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        //Terminate and delete instance with Busybox and ns deletion
        terminateAndDeleteInstance(workloadInstanceResponseWithBusybox, clusterConfigInfoPath, true);
    }

    @Test(description = "Process another series of lifecycle operations in different combinations and order "
            + "according to the second flow")
    public void shouldSuccessfullyPerformOperationsForHelmfileSecondFlow() {
        //Setup
        WorkloadInstanceTestData instance = new WorkloadInstanceTestData();
        instance.setHelmSourceLocation(HELM_SOURCE_LOCATION);
        instance.setHelmSourceName(HELM_SOURCE_NAME);
        instance.setWorkloadInstanceName("instantiate-with-helmfile-second-flow");
        instance.setNamespace(namespace);
        instance.setValuesYaml(VALUES_PATH);
        instance.setClusterConnectionInfo(clusterName);

        Setup.zipUpHelmfile(instance);

        WorkloadInstanceTestData instanceForUpdate = new WorkloadInstanceTestData();
        instanceForUpdate.setValuesYaml(VALUES_TO_UPDATE_PATH);

        RollbackData rollbackRequest = new RollbackData(null, null);
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(ADDITIONAL_PARAMETERS, fillAdditionalParams(true, false, false));

        ResponseEntity<String> healthResponse = HealthCheck.getHealthState();
        checkHealthStatusOk(healthResponse);

        //Instantiate workloadInstance with pre-existing values
        Object workloadInstanceResponse =
                instantiate(instance, getReleasesToCheck(true, true, false));

        //Upgrade without clusterConnectionInfo when WorkloadInstance instantiated with clusterName
        upgrade(instanceForUpdate, workloadInstanceResponse,
                getReleasesToCheck(true, false, true));

        //Terminate after upgrade
        terminate(workloadInstanceResponse, getReleasesToCheck(false, false, false));

        //Upgrade after terminate (reinstantiate)
        instanceForUpdate.setValuesYaml(null);
        reinstantiate(instanceForUpdate, workloadInstanceResponse,
                      getReleasesToCheck(true, false, true));

        //Rollback without version after instantiate-upgrade-terminate-reinstantiate
        rollback(workloadInstanceResponse, rollbackRequest,
                 getReleasesToCheck(true, true, false));

        //Upgrade without values file with additional params only
        instanceForUpdate.setAdditionalParameters(additionalParams);
        upgrade(instanceForUpdate, workloadInstanceResponse, getReleasesToCheck(true, false, false));

        //Rollback with the specified version after upgrade
        Version.Item versionItem = getVersionItemByNumber(1, workloadInstanceResponse);
        Integer version = getVersionAndCheckFields(workloadInstanceResponse, versionItem);
        RollbackData rollbackWithVersion = new RollbackData(version, null);
        rollback(workloadInstanceResponse, rollbackWithVersion,
                 getReleasesToCheck(true, true, false));

        //Terminate after rollback
        terminate(workloadInstanceResponse, getReleasesToCheck(false, false, false));

        //Upgrade after terminate (reinstantiate)
        instanceForUpdate.setAdditionalParameters(null);
        reinstantiate(instanceForUpdate, workloadInstanceResponse,
                      getReleasesToCheck(true, true, false));

        //Rollback with the specified version after reinstantiate
        versionItem = getVersionItemByNumber(2, workloadInstanceResponse);
        version = getVersionAndCheckFields(workloadInstanceResponse, versionItem);
        rollbackWithVersion = new RollbackData(version, null);
        rollback(workloadInstanceResponse, rollbackWithVersion,
                 getReleasesToCheck(true, false, true));

        //Rollback with the specified version after terminate
        versionItem = getVersionItemByNumber(1, workloadInstanceResponse);
        version = getVersionAndCheckFields(workloadInstanceResponse, versionItem);
        rollbackWithVersion = new RollbackData(version, null);
        rollback(workloadInstanceResponse, rollbackWithVersion,
                 getReleasesToCheck(true, true, false));
        //retrieve all operations by workload instance
        getOperationsByWorkloadId(workloadInstanceResponse, 11);

        //Terminate and delete
        terminateAndDeleteInstance(workloadInstanceResponse, null,  false);
    }

    @Test(description = "Process series of upgrades and rollbacks in different combinations and order based on integration chart "
            + "according to the first flow")
    public void shouldSuccessfullyPerformOperationsForIntegrationChartFirstFlow() {
        WorkloadInstanceTestData instance = new WorkloadInstanceTestData();
        instance.setHelmSourceLocation(PATH_TO_INTEGRATION_CHART_FOLDER);
        instance.setHelmSourceName(TEST_INTEGRATION_1080_TGZ);
        instance.setWorkloadInstanceName("instantiate-integration-chart");
        instance.setNamespace(namespace);
        instance.setValuesYaml(VALUES_INSTANTIATE_INTEGRATION_CHART);
        instance.setClusterConnectionLocation(clusterConfigInfoPath);

        Setup.zipUpIntegrationChart(instance, ARCHIVE_INSTANTIATE_PATH);

        ResponseEntity<String> healthResponse = HealthCheck.getHealthState();
        checkHealthStatusOk(healthResponse);

        //Instantiate
        Object workloadInstanceResponse = instantiateIntegrationChart(instance);

        //Upgrade with new integration chart and clusterConnectionInfo
        Setup.zipUpIntegrationChart(instance, ARCHIVE_UPDATE_PATH);
        instance.setHelmSourceName(TEST_INTEGRATION_1081_TGZ);
        upgradeIntegrationChart(instance, workloadInstanceResponse, true);

        //Upgrade with new values and clusterConnectionInfo
        instance.setHelmSourceName(null);
        instance.setHelmSourceLocation(null);
        instance.setValuesYaml("src/main/resources/testData/instantiate/integration-chart/values-update.yaml");
        upgradeIntegrationChart(instance, workloadInstanceResponse, false);

        //Upgrade without values file with additional params and clusterConnectionInfo
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(ADDITIONAL_PARAMETERS, Collections.singletonMap(ERIC_CTRL_BRO_ENABLED, true));
        instance.setValuesYaml(null);
        instance.setAdditionalParameters(additionalParams);
        upgradeIntegrationChart(instance, workloadInstanceResponse, true);

        //Rollback without version after upgrade with clusterConnectionInfo
        RollbackData rollbackRequest = new RollbackData(null, clusterConfigInfoPath);
        rollbackIntegrationChart(workloadInstanceResponse, instance.getWorkloadInstanceName(), rollbackRequest,
                                 false);

        //Rollback without version after rollback with clusterConnectionInfo
        rollbackIntegrationChart(workloadInstanceResponse, instance.getWorkloadInstanceName(), rollbackRequest, true);

        //retrieve all versions of workload instance and take any
        Version.Item versionItem = checkVersionsAndGetAny(workloadInstanceResponse);

        //Get individual version
        Integer version = getVersionAndCheckFields(workloadInstanceResponse, versionItem);

        //Rollback with the specified version after rollback with clusterConnectionInfo
        RollbackData rollbackWithVersion = new RollbackData(version, clusterConfigInfoPath);
        rollbackIntegrationChart(workloadInstanceResponse, instance.getWorkloadInstanceName(), rollbackWithVersion, true);
        //retrieve all operations by workload instance
        getOperationsByWorkloadId(workloadInstanceResponse, 7);

        terminateAndDeleteInstance(workloadInstanceResponse, clusterConfigInfoPath, false);
    }

    @Test(description = "Process series of upgrades and rollbacks in different combinations and order based on integration chart "
            + "according to the second flow")
    public void shouldSuccessfullyPerformOperationsForIntegrationChartSecondFlow() {
        //Setup
        WorkloadInstanceTestData instance = new WorkloadInstanceTestData();
        instance.setHelmSourceLocation(PATH_TO_INTEGRATION_CHART_FOLDER);
        instance.setHelmSourceName(TEST_INTEGRATION_1080_TGZ);
        instance.setWorkloadInstanceName("instantiate-integration-chart-second-flow");
        instance.setNamespace(namespace);
        instance.setValuesYaml(VALUES_INSTANTIATE_INTEGRATION_CHART);
        instance.setClusterConnectionInfo(clusterName);

        Setup.zipUpIntegrationChart(instance, ARCHIVE_INSTANTIATE_PATH);

        ResponseEntity<String> healthResponse = HealthCheck.getHealthState();
        checkHealthStatusOk(healthResponse);

        //Instantiate
        Object workloadInstanceResponse = instantiateIntegrationChart(instance);

        //Upgrade without clusterConnectionInfo when WorkloadInstance instantiated with clusterName
        Setup.zipUpIntegrationChart(instance, ARCHIVE_UPDATE_PATH);
        instance.setHelmSourceName(TEST_INTEGRATION_1081_TGZ);
        upgradeIntegrationChart(instance, workloadInstanceResponse, true);

        //Terminate after upgrade
        terminateIntegrationChart(workloadInstanceResponse);

        //Upgrade after terminate (reinstantiate)
        instance.setHelmSourceName(null);
        instance.setHelmSourceLocation(null);
        instance.setValuesYaml(null);
        reinstantiateIntegrationChart(instance, workloadInstanceResponse, true);

        //Rollback without version after instantiate-upgrade-terminate-reinstantiate
        RollbackData rollbackRequest = new RollbackData(null, null);
        rollbackIntegrationChart(workloadInstanceResponse, instance.getWorkloadInstanceName(), rollbackRequest, false);

        //Upgrade without values file with additional params
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(ADDITIONAL_PARAMETERS, Collections.singletonMap(ERIC_CTRL_BRO_ENABLED, false));
        instance.setAdditionalParameters(additionalParams);
        upgradeIntegrationChart(instance, workloadInstanceResponse, false);

        //Rollback with the specified version after upgrade
        Version.Item versionItem = getVersionItemByNumber(1, workloadInstanceResponse);
        Integer version = getVersionAndCheckFields(workloadInstanceResponse, versionItem);
        RollbackData rollbackWithVersion = new RollbackData(version, null);
        rollbackIntegrationChart(workloadInstanceResponse, instance.getWorkloadInstanceName(), rollbackWithVersion, false);

        //Terminate after rollback
        terminateIntegrationChart(workloadInstanceResponse);

        //Upgrade after terminate (reinstantiate)
        instance.setAdditionalParameters(null);
        reinstantiateIntegrationChart(instance, workloadInstanceResponse, false);

        //Rollback with the specified version after reinstantiate
        versionItem = getVersionItemByNumber(2, workloadInstanceResponse);
        version = getVersionAndCheckFields(workloadInstanceResponse, versionItem);
        rollbackWithVersion = new RollbackData(version, null);
        rollbackIntegrationChart(workloadInstanceResponse, instance.getWorkloadInstanceName(), rollbackWithVersion, true);

        //Rollback with the specified version after terminate
        versionItem = getVersionItemByNumber(1, workloadInstanceResponse);
        version = getVersionAndCheckFields(workloadInstanceResponse, versionItem);
        rollbackWithVersion = new RollbackData(version, null);
        rollbackIntegrationChart(workloadInstanceResponse, instance.getWorkloadInstanceName(), rollbackWithVersion, false);
        //retrieve all operations by workload instance
        getOperationsByWorkloadId(workloadInstanceResponse, 11);

        //Terminate and delete
        terminateAndDeleteInstance(workloadInstanceResponse, null, false);
    }

    @Test(description = "Instantiate workload instance with grouped enable")
    public void testInstantiateFunctionalEntityWithHelmfile() {
        WorkloadInstanceTestData instance = new WorkloadInstanceTestData();
        instance.setHelmSourceLocation("src/main/resources/testData/instantiate/helmfile-func-entity/");
        instance.setHelmSourceName("helmfile-func-entity-1.2.3-4.tgz");
        instance.setWorkloadInstanceName("instantiate-functional-entity");
        instance.setNamespace(namespace);
        instance.setValuesYaml(VALUES_FUNC_ENTITY_PATH);
        instance.setClusterConnectionInfo(clusterName);

        Setup.zipUpHelmfile(instance);

        ResponseEntity<String> healthResponse = HealthCheck.getHealthState();
        checkHealthStatusOk(healthResponse);

        ResponseEntity<HashMap<String, Object>> response = LifecycleOperations.instantiate(instance);
        Map<String, Object> ids = getResponseBodyByInstantiate(response);
        assertThat(ids.get(STATUS_CODE)).isEqualTo(HttpStatus.ACCEPTED);

        Object workloadInstanceResponse = ids.get(WORKLOAD_INSTANCE_ID);

        LifecycleOperations.verifyOperation(ids.get(OPERATION_ID), COMPLETED_STATE, OPERATION_TYPE_INSTANTIATE);

        checkInstalledReleases(getReleasesToCheck(true, true, false));

        terminateAndDeleteInstance(workloadInstanceResponse, null, false);

        checkCrdsIsInstalled();
    }

    @Test(description = "Instantiate workload instance with creating secrets")
    public void testInstantiateFunctionalWithCreateSecretWithHelmfile() {
        WorkloadInstanceTestData instance = new WorkloadInstanceTestData();
        instance.setHelmSourceLocation("src/main/resources/testData/instantiate/helmfile-test-with-ingress/");
        instance.setHelmSourceName("helmfile-ingress-entity-1.2.3-4.tgz");
        instance.setWorkloadInstanceName("instantiate-create-secrets");
        instance.setNamespace(namespace);
        instance.setValuesYaml(VALUES_WITH_INGRESS_PATH);
        instance.setClusterConnectionInfo(clusterName);

        Setup.zipUpHelmfile(instance);

        ResponseEntity<String> healthResponse = HealthCheck.getHealthState();
        checkHealthStatusOk(healthResponse);

        if (SERVICE_INSTANCE.getIsLocal()) {
            FileUtils.copyContentToDirectory(SOURCE_PATH, CERTIFICATES_PATH);
        }
        ResponseEntity<HashMap<String, Object>> response = LifecycleOperations.instantiate(instance);
        Map<String, Object> ids = getResponseBodyByInstantiate(response);
        assertThat(ids.get(STATUS_CODE)).isEqualTo(HttpStatus.ACCEPTED);

        checkSecretsAreCreated();

        Object workloadInstanceResponse = ids.get(WORKLOAD_INSTANCE_ID);
        LifecycleOperations.verifyOperation(ids.get(OPERATION_ID), COMPLETED_STATE, OPERATION_TYPE_INSTANTIATE);

        if (SERVICE_INSTANCE.getIsLocal()) {
            FileUtils.removeDirectory(CERTIFICATES_PATH);
        }
        terminateAndDeleteInstance(workloadInstanceResponse, null, false);
    }

    @Test(description = "Instantiate and rollback workload instance")
    public void testHelmfileManualRollbackAfterInstantiate() {
        WorkloadInstanceTestData instance = new WorkloadInstanceTestData();
        instance.setHelmSourceLocation(HELM_SOURCE_LOCATION);
        instance.setHelmSourceName(HELM_SOURCE_NAME);
        instance.setWorkloadInstanceName("manual-rollback-after-instantiate");
        instance.setNamespace(namespace);
        instance.setValuesYaml(VALUES_PATH);
        Setup.zipUpHelmfile(instance);

        ResponseEntity<String> healthResponse = HealthCheck.getHealthState();
        checkHealthStatusOk(healthResponse);

        ResponseEntity<HashMap<String, Object>> response = LifecycleOperations.instantiate(instance);
        Map<String, Object> ids = getResponseBodyByInstantiate(response);
        assertThat(ids.get(STATUS_CODE)).isEqualTo(HttpStatus.ACCEPTED);
        Object workloadInstanceResponse = ids.get(WORKLOAD_INSTANCE_ID);

        LifecycleOperations.verifyOperation(ids.get(OPERATION_ID), COMPLETED_STATE, OPERATION_TYPE_INSTANTIATE);

        checkInstalledReleases(getReleasesToCheck(true, true, false));

        RollbackData rollbackData = new RollbackData(null, null);
        try {
            manualRollbackAndCheckStatus(ids.get(WORKLOAD_INSTANCE_ID), rollbackData);
        } catch (HttpStatusCodeException e) {
            checkInstalledReleases(getReleasesToCheck(true, true, false));
            assertThat(e.getRawStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        } finally {
            terminateAndDeleteInstance(workloadInstanceResponse, null, false);
        }
    }

    @Test
    public void testHelmfileManualRollbackAfterReinstantiate() {
        WorkloadInstanceTestData instance = new WorkloadInstanceTestData();
        instance.setHelmSourceLocation(HELM_SOURCE_LOCATION);
        instance.setHelmSourceName(HELM_SOURCE_NAME);
        instance.setWorkloadInstanceName("manual-rollback-after-reinstantiate");
        instance.setNamespace(namespace);
        instance.setValuesYaml(VALUES_PATH);

        Setup.zipUpHelmfile(instance);

        ResponseEntity<String> healthResponse = HealthCheck.getHealthState();
        checkHealthStatusOk(healthResponse);

        ResponseEntity<HashMap<String, Object>> responseInstantiate = LifecycleOperations.instantiate(instance);
        Map<String, Object> ids = getResponseBodyByInstantiate(responseInstantiate);
        Object workloadInstanceResponse = ids.get(WORKLOAD_INSTANCE_ID);
        assertThat(ids.get(STATUS_CODE)).isEqualTo(HttpStatus.ACCEPTED);

        LifecycleOperations.verifyOperation(ids.get(OPERATION_ID), COMPLETED_STATE, OPERATION_TYPE_INSTANTIATE);

        ResponseEntity<String> response =
                LifecycleOperations.terminate(ids.get(WORKLOAD_INSTANCE_ID), null, false);
        String operationId = getOperationId(response.getHeaders());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        LifecycleOperations.verifyOperation(operationId, COMPLETED_STATE, OPERATION_TYPE_TERMINATE);

        response = LifecycleOperations.reinstantiate(ids.get(WORKLOAD_INSTANCE_ID));
        operationId = getOperationId(response.getHeaders());
        LifecycleOperations.verifyOperation(operationId, COMPLETED_STATE, OPERATION_TYPE_REINSTANTIATE);

        RollbackData rollbackData = new RollbackData(null, null);
        try {
            manualRollbackAndCheckStatus(ids.get(WORKLOAD_INSTANCE_ID), rollbackData);
        } catch (HttpStatusCodeException e) {
            checkInstalledReleases(getReleasesToCheck(true, true, false));
            assertThat(e.getRawStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        } finally {
            terminateAndDeleteInstance(workloadInstanceResponse, null, false);
        }
    }

    @Test(description = "Instantiate two workload instances on the same time")
    public void testInstantiateWithHelmfileAsIndividualWorkloadFunction() {
        WorkloadInstanceTestData instance = new WorkloadInstanceTestData();
        instance.setHelmSourceLocation(HELM_SOURCE_LOCATION);
        instance.setHelmSourceName(HELM_SOURCE_NAME);
        instance.setWorkloadInstanceName("helmfile-individual-workload-function");
        instance.setNamespace(namespace);
        instance.setValuesYaml(VALUES_PATH);
        Setup.zipUpHelmfile(instance);

        ResponseEntity<String> healthResponseFirst = HealthCheck.getHealthState();
        checkHealthStatusOk(healthResponseFirst);

        ResponseEntity<HashMap<String, Object>> response = LifecycleOperations.instantiate(instance);
        Map<String, Object> ids = getResponseBodyByInstantiate(response);
        Object workloadInstanceResponse = ids.get(WORKLOAD_INSTANCE_ID);
        assertThat(ids.get(STATUS_CODE)).isEqualTo(HttpStatus.ACCEPTED);

        LifecycleOperations.verifyOperation(ids.get(OPERATION_ID), COMPLETED_STATE, OPERATION_TYPE_INSTANTIATE);
        checkInstalledReleases(getReleasesToCheck(true, true, false));
        // verify if secret is installed
        checkSecretPresence(String.format(DOCKER_SECRET_RELEASE_NAME, instance.getWorkloadInstanceName()), true);

        instance.setValuesYaml(VALUES_TO_UPDATE_PATH);
        instance.setWorkloadInstanceName("helmfile-individual-workload-function-2");
        Setup.zipUpHelmfile(instance);

        checkHealthStatusOk(HealthCheck.getHealthState());

        ResponseEntity<HashMap<String, Object>> responseInstantiate = LifecycleOperations.instantiate(instance);
        Map<String, Object> idsInstantiate = getResponseBodyByInstantiate(responseInstantiate);
        Object secondWorkloadInstanceResponse = idsInstantiate.get(WORKLOAD_INSTANCE_ID);
        assertThat(ids.get(STATUS_CODE)).isEqualTo(HttpStatus.ACCEPTED);

        LifecycleOperations.verifyOperation(idsInstantiate.get(OPERATION_ID), COMPLETED_STATE, OPERATION_TYPE_INSTANTIATE);
        checkInstalledReleases(getReleasesToCheck(true, false, true));
        checkSecretPresence(String.format(DOCKER_SECRET_RELEASE_NAME, instance.getWorkloadInstanceName()), true);

        terminateAndDeleteInstance(workloadInstanceResponse, null, false);
        terminateAndDeleteInstance(secondWorkloadInstanceResponse, null, false);
    }

    @Test(description = "Instantiate and update workload instance through helmfile builder")
    public void shouldSuccessfullyInstantiateAndUpdateWorkloadInstanceThroughHelmfileBuilder() {
        //Setup
        HelmfileBuilderRequest instance = new HelmfileBuilderRequest();
        instance.setWorkloadInstanceName("instantiate-with-helmfile-builder");
        instance.setNamespace(namespace);
        instance.setValuesYaml(HELMFILE_BUILDER_VALUES_PATH);
        instance.setClusterConnectionInfo(clusterName);
        instance.setClusterConnectionLocation(clusterConfigInfoPath);
        instance.setRepository(REPOSITORY);
        instance.setTimeout(1);
        instance.setCharts(List.of(new Chart(null, CHART_BRO_NAME, CHART_VERSION, false, null)));

        Map<String, Object> additionalParams = new HashMap<>();
        instance.setAdditionalParameters(additionalParams);
        ResponseEntity<String> healthResponse = HealthCheck.getHealthState();
        checkHealthStatusOk(healthResponse);

        //Instantiate
        Object workloadInstanceResponse = instantiateThroughHelmfileBuilder(instance, Collections.singletonMap(CHART_BRO_NAME, true));

        //Upgrade with new chart and clusterConnectionInfo
        instance.setCharts(List.of(new Chart(null, CHART_BRO_NAME, CHART_VERSION, false, null),
                                   new Chart(null, CHART_SERVER_NAME, "10.18.0-47", false, null)));
        instance.setValuesYaml(null);
        Map<String, Boolean> releasesToCheck = new HashMap<>();
        releasesToCheck.put(CHART_BRO_NAME, true);
        releasesToCheck.put(CHART_SERVER_NAME, true);
        upgradeWithHelmfileBuilder(instance, workloadInstanceResponse, releasesToCheck);
        terminateAndDeleteInstance(workloadInstanceResponse, clusterConfigInfoPath, false);
    }

    @Test(description = "Instantiate and update workload instance through helmfile builder with charts in specified order")
    public void shouldSuccessfullyInstantiateAndUpdateWorkloadInstanceThroughHelmfileBuilderWithOrder() {
        //Setup
        HelmfileBuilderRequest instance = new HelmfileBuilderRequest();
        instance.setWorkloadInstanceName("instantiate-with-helmfile-builder-order");
        instance.setNamespace(namespace);
        instance.setValuesYaml(HELMFILE_BUILDER_WITH_ORDER_VALUES_PATH);
        instance.setClusterConnectionInfo(clusterName);
        instance.setClusterConnectionLocation(clusterConfigInfoPath);
        instance.setRepository(REPOSITORY);
        instance.setTimeout(1);
        instance.setCharts(List.of(new Chart(2, CHART_BRO_NAME, CHART_VERSION, false, null),
                                   new Chart(1, CHART_SERVER_NAME, "10.18.0-47", false, null)
        ));

        ResponseEntity<String> healthResponse = HealthCheck.getHealthState();
        checkHealthStatusOk(healthResponse);
        Map<String, Boolean> releasesToCheck = new HashMap<>();
        releasesToCheck.put(CHART_BRO_NAME, true);
        releasesToCheck.put(CHART_SERVER_NAME, true);

        //Instantiate
        Object workloadInstanceResponse = instantiateThroughHelmfileBuilder(instance, releasesToCheck);

        String releases = getReleasesList();
        List<String> chartNames = instance.getCharts().stream().map(Chart::getName).collect(Collectors.toList());
        Map<String, LocalDateTime> releasesWithUpdateTime = getReleasesWithUpdateTime(chartNames, releases);
        checkReleasesOrdering(releasesWithUpdateTime, instance.getCharts());

        terminateAndDeleteInstance(workloadInstanceResponse, clusterConfigInfoPath, false);
    }

    @Test(description = "Instantiate, update and upgrade workloadInstance via helmfile fetcher with Helm Chart Registry and isHelmChartRegistry true")
    public void instantiateUpdateAndUpgradeViaHelmfileFetcherEndpointWithHelmChartRegistryUrl() {
        var instanceDataWithUrl =  new WorkloadInstanceTestDataWithUrl();
        var helmSourceName = TEST_INTEGRATION_1081_TGZ;
        var url = SERVICE_INSTANCE.getChartRegistryUrl() + CHARTS_PART + helmSourceName;
        instanceDataWithUrl.setUrl(url);
        instanceDataWithUrl.setUrlToHelmRegistry(true);
        instanceDataWithUrl.setNamespace(namespace);
        instanceDataWithUrl.setClusterConnectionInfo(clusterName);
        instanceDataWithUrl.setWorkloadInstanceName("helmfile-fetcher-with-helm-registry");
        instanceDataWithUrl.setHelmSourceName(helmSourceName);
        instanceDataWithUrl.setHelmSourceLocation(PATH_TO_INTEGRATION_CHART_FOLDER);
        instanceDataWithUrl.setValuesYaml("src/main/resources/testData/instantiate/integration-chart/values-update.yaml");
        instanceDataWithUrl.setAdditionalParameters(Map.of(ERIC_CTRL_BRO_ENABLED, true));

        ResponseEntity<String> healthResponse = HealthCheck.getHealthState();
        checkHealthStatusOk(healthResponse);

        Setup.zipUpIntegrationChart(instanceDataWithUrl, ARCHIVE_UPDATE_PATH);
        HelmChartRegistryOperations.uploadHelmChart(
                instanceDataWithUrl.getHelmSourceLocation() + instanceDataWithUrl.getHelmSourceName());

        //instantiate
        Object workloadInstanceId = instantiateViaHelmfileFetcher(instanceDataWithUrl);

        checkPodStatus(true);

        //update with values
        instanceDataWithUrl.setUrl(null);
        instanceDataWithUrl.setAdditionalParameters(null);
        instanceDataWithUrl.setValuesYaml("src/main/resources/testData/instantiate/integration-chart/values-update.yaml");

        //update
        workloadInstanceId = updateViaHelmfileFetcher(instanceDataWithUrl, workloadInstanceId);

        checkPodStatus(false);

        //upgrade instanceDataWithUrl with new helm source
        url = SERVICE_INSTANCE.getChartRegistryUrl() + CHARTS_PART + TEST_INTEGRATION_1080_TGZ;
        instanceDataWithUrl.setHelmSourceName(TEST_INTEGRATION_1080_TGZ);
        instanceDataWithUrl.setUrl(url);
        instanceDataWithUrl.setValuesYaml(VALUES_INSTANTIATE_INTEGRATION_CHART);

        Setup.zipUpIntegrationChart(instanceDataWithUrl, ARCHIVE_INSTANTIATE_PATH);
        HelmChartRegistryOperations.uploadHelmChart(
                instanceDataWithUrl.getHelmSourceLocation() + instanceDataWithUrl.getHelmSourceName());

        //upgrade
        workloadInstanceId = updateViaHelmfileFetcher(instanceDataWithUrl, workloadInstanceId);

        checkPodStatus(false);

        terminateAndDeleteInstance(workloadInstanceId, null, false);
    }

    @Test(description = "Process series of instantiates and termination in different combinations based on integration chart" +
            " with and without deletion namespace process during terminate")
    public void shouldSuccessfullyDeleteNamespaceForIntegrationChart() {
        //Setup
        WorkloadInstanceTestData instance = new WorkloadInstanceTestData();
        instance.setHelmSourceLocation(PATH_TO_INTEGRATION_CHART_FOLDER);
        instance.setHelmSourceName(TEST_INTEGRATION_1082_TGZ);
        instance.setWorkloadInstanceName("instantiate-delete-namespace");
        instance.setNamespace(namespace);
        instance.setValuesYaml(VALUES_INSTANTIATE_INTEGRATION_CHART);
        instance.setClusterConnectionInfo(clusterName);

        Setup.zipUpIntegrationChart(instance, "chartWithBro/");

        ResponseEntity<String> healthResponse = HealthCheck.getHealthState();
        checkHealthStatusOk(healthResponse);

        //Instantiate
        Object workloadInstanceResponse = instantiateIntegrationChart(instance);

        checkPodStatus(true);

        //Terminate after instantiate with flag delete instance false
        terminateIntegrationChart(workloadInstanceResponse);

        //Check if namespace is not deleted
        checkNamespaceDeletion(false);

        //Upgrade after terminate (reinstantiate)
        instance.setHelmSourceName(null);
        instance.setHelmSourceLocation(null);
        instance.setValuesYaml(null);
        reinstantiateIntegrationChart(instance, workloadInstanceResponse, true);

        //Terminate after update with flag delete namespace true and verify if namespace is deleted
        terminateAndDeleteInstance(workloadInstanceResponse, null, true);
    }

    @Test(description = "Process instantiate and termination based on integration chart with post hook" +
            "with deletion namespace process during terminate")
    public void shouldSuccessfullyInstantiateForIntegrationChartWithPostHook() {
        //Setup
        WorkloadInstanceTestData instance = new WorkloadInstanceTestData();
        instance.setHelmSourceLocation(PATH_TO_INTEGRATION_CHART_FOLDER);
        instance.setHelmSourceName("eric-ran-rad-rce-base-1.22.0-3.tgz");
        instance.setWorkloadInstanceName("instantiate-with-post-hook");
        instance.setNamespace(namespace);
        instance.setValuesYaml("src/main/resources/testData/instantiate/integration-chart/eric-ran-rad-rce-base/values.yaml");
        instance.setClusterConnectionInfo(clusterName);

        Setup.zipUpIntegrationChart(instance, "eric-ran-rad-rce-base/");

        ResponseEntity<String> healthResponse = HealthCheck.getHealthState();
        checkHealthStatusOk(healthResponse);

        //Instantiate
        Object workloadInstanceResponse = instantiateIntegrationChart(instance);

        //Terminate after update with flag delete namespace true and verify if namespace is deleted
        terminateAndDeleteInstance(workloadInstanceResponse, null, true);
    }

    @Test(description = "Instantiates two instances, then terminates one with the namespace delete flag and terminates the second")
    public void shouldNotDeleteNsWhenNsNotEmptyForIntegrationChart() {
        WorkloadInstanceTestData instanceWithBro = new WorkloadInstanceTestData();
        instanceWithBro.setHelmSourceLocation(PATH_TO_INTEGRATION_CHART_FOLDER);
        instanceWithBro.setHelmSourceName(TEST_INTEGRATION_1082_TGZ);
        instanceWithBro.setWorkloadInstanceName("test-delete-with-instances-bro");
        instanceWithBro.setNamespace(namespace);
        instanceWithBro.setValuesYaml(VALUES_INSTANTIATE_INTEGRATION_CHART);
        instanceWithBro.setClusterConnectionInfo(clusterName);
        WorkloadInstanceTestData instanceWithBusybox = new WorkloadInstanceTestData();
        instanceWithBusybox.setHelmSourceLocation(PATH_TO_INTEGRATION_CHART_FOLDER);
        instanceWithBusybox.setHelmSourceName(BUSYBOX_SIMPLE_CHART_1_32_0_TGZ);
        instanceWithBusybox.setWorkloadInstanceName("test-delete-with-instances-busybox");
        instanceWithBusybox.setNamespace(namespace);
        instanceWithBusybox.setValuesYaml("src/main/resources/testData/instantiate/integration-chart/chartWithBusybox/values.yaml");
        instanceWithBusybox.setClusterConnectionInfo(clusterName);

        Setup.zipUpIntegrationChart(instanceWithBro, "chartWithBro/");
        Setup.zipUpIntegrationChart(instanceWithBusybox, "chartWithBusybox/");

        // instantiate two instances
        Object instanceWithBroResponse = instantiateIntegrationChart(instanceWithBro);
        Object instanceWithBusyboxResponse = instantiateIntegrationChart(instanceWithBusybox);

        checkPodStatus(true);

        // delete with flag delete namespace
        ResponseEntity<String> terminate = LifecycleOperations.terminate(instanceWithBroResponse, clusterConfigInfoPath, true);
        String operationId = getOperationId(terminate.getHeaders());
        LifecycleOperations.verifyOperation(operationId, COMPLETED_STATE, OPERATION_TYPE_TERMINATE);

        // check that namespace not deleted in case that not all instances in namespace are terminated
        checkNamespaceDeletion(false);

        // delete instance with bro
        ResponseEntity<String> deleteBroResponse = deleteInstance(instanceWithBroResponse);
        assertThat(deleteBroResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // terminate and delete last instance with ns deletion
        terminateAndDeleteInstance(instanceWithBusyboxResponse, null, true);
    }

    @Ignore
    @Test(description = "Broken instance instantiation, trigger auto rollback, terminate and ns delete check")
    public void shouldDeleteWhenTerminateAfterAutoRollback() {
        WorkloadInstanceTestData instanceWithBro = new WorkloadInstanceTestData();
        instanceWithBro.setHelmSourceLocation(PATH_TO_INTEGRATION_CHART_FOLDER);
        instanceWithBro.setHelmSourceName(TEST_INTEGRATION_1082_TGZ);
        instanceWithBro.setWorkloadInstanceName("delete-ns-after-auto-rollback");
        instanceWithBro.setNamespace(namespace);
        instanceWithBro.setValuesYaml(VALUES_INSTANTIATE_INTEGRATION_CHART);
        instanceWithBro.setClusterConnectionInfo(clusterName);
        instanceWithBro.setTimeout(1);
        Map<String, Object> additionalParameters = Map.of(ADDITIONAL_PARAMETERS, Collections.singletonMap("global.registry.url", "invalid-host"));
        instanceWithBro.setAdditionalParameters(additionalParameters);

        Setup.zipUpIntegrationChart(instanceWithBro, "chartWithBro/");

        ResponseEntity<HashMap<String, Object>> response = LifecycleOperations.instantiate(instanceWithBro);
        Map<String, Object> ids = getResponseBodyByInstantiate(response);

        verifyAutoRollbackCompleted(ids);

        terminateAndDeleteInstance(ids.get(WORKLOAD_INSTANCE_ID), null, true);
    }


    @Test(description = "Instantiate helmfile in which one of the releases has invalid image, trigger auto rollback, terminate and delete ns")
    public void shouldTriggerAutoRollbackAndDeleteNsWhenInstantiateFailed() {
        WorkloadInstanceTestData instance = new WorkloadInstanceTestData();
        instance.setHelmSourceLocation(HELMFILE_INVALID_IMAGE_LOCATION);
        instance.setHelmSourceName(HELM_SOURCE_NAME);
        instance.setWorkloadInstanceName("helmfile-with-one-release-broken");
        instance.setNamespace(namespace);
        instance.setValuesYaml(HELMFILE_INVALID_IMAGE_VALUES);
        instance.setClusterConnectionInfo(clusterName);
        instance.setTimeout(1);

        Setup.zipUpHelmfile(instance);

        ResponseEntity<HashMap<String, Object>> response = LifecycleOperations.instantiate(instance);
        Map<String, Object> ids = getResponseBodyByInstantiate(response);

        verifyAutoRollbackCompleted(ids);

        terminateAndDeleteInstance(ids.get(WORKLOAD_INSTANCE_ID), null, true);
    }

    private Object instantiateViaHelmfileFetcher(WorkloadInstanceTestDataWithUrl instanceDataWithUrl) {
        ResponseEntity<HashMap<String, Object>> workloadInstanceResponse = LifecycleOperations.instantiateThroughHelmfileFetcher(instanceDataWithUrl);
        Map<String, Object> ids = getResponseBodyByInstantiate(workloadInstanceResponse);
        assertThat(ids.get(STATUS_CODE)).isEqualTo(HttpStatus.ACCEPTED);
        LifecycleOperations.verifyOperation(ids.get(OPERATION_ID), COMPLETED_STATE, OPERATION_TYPE_INSTANTIATE);

        String installedReleases = getReleasesList();
        checkReleaseState(instanceDataWithUrl.getWorkloadInstanceName(), true, installedReleases);
        checkSecretPresence(String.format(DOCKER_SECRET_RELEASE_NAME, instanceDataWithUrl.getWorkloadInstanceName()), true);
        return ids.get(WORKLOAD_INSTANCE_ID);
    }

    private Object updateViaHelmfileFetcher(WorkloadInstanceTestDataWithUrl instanceDataWithUrl, Object workloadInstanceResponse) {
        ResponseEntity<HashMap<String, Object>> response = LifecycleOperations
                .updateThroughHelmfileFetcher(instanceDataWithUrl, workloadInstanceResponse);

        Map<String, Object> ids = getResponseBodyByInstantiate(response);
        assertThat(ids.get(STATUS_CODE)).isEqualTo(HttpStatus.ACCEPTED);
        LifecycleOperations.verifyOperation(ids.get(OPERATION_ID), COMPLETED_STATE, OPERATION_TYPE_UPDATE);

        String installedReleases = getReleasesList();
        checkReleaseState(instanceDataWithUrl.getWorkloadInstanceName(), true, installedReleases);
        checkSecretPresence(String.format(DOCKER_SECRET_RELEASE_NAME, instanceDataWithUrl.getWorkloadInstanceName()), true);
        return ids.get(WORKLOAD_INSTANCE_ID);
    }

    private void getOperationsByWorkloadId(Object workloadInstanceResponse, int amount) {
        List<PagedOperation.Operation> operationList = LifecycleOperations.getOperationsByWorkloadId(workloadInstanceResponse);
        assertThat(operationList.size()).isEqualTo(amount);
    }

    private void terminateAndDeleteInstance(Object workloadInstance, String clusterConnectionInfoPath, boolean deleteNamespace) {
        ResponseEntity<String> terminate =
                LifecycleOperations.terminate(workloadInstance, clusterConnectionInfoPath, deleteNamespace);
        String operationId = getOperationId(terminate.getHeaders());
        LifecycleOperations.verifyOperation(operationId, COMPLETED_STATE, OPERATION_TYPE_TERMINATE);
        ResponseEntity<String> response = deleteInstance(workloadInstance);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        checkNamespaceDeletion(deleteNamespace);
    }

    private Object instantiate(WorkloadInstanceTestData instance, Map<String, Boolean> releasesToCheck) {
        ResponseEntity<HashMap<String, Object>> response = LifecycleOperations.instantiate(instance);
        Map<String, Object> ids = getResponseBodyByInstantiate(response);
        assertThat(ids.get(STATUS_CODE)).isEqualTo(HttpStatus.ACCEPTED);
        LifecycleOperations.verifyOperation(ids.get(OPERATION_ID), COMPLETED_STATE, OPERATION_TYPE_INSTANTIATE);

        checkInstalledReleases(releasesToCheck);
        checkCrdsIsInstalled();

        return ids.get(WORKLOAD_INSTANCE_ID);
    }

    private Object instantiateIntegrationChart(WorkloadInstanceTestData instance) {
        ResponseEntity<HashMap<String, Object>> response = LifecycleOperations.instantiate(instance);
        Map<String, Object> ids = getResponseBodyByInstantiate(response);
        assertThat(ids.get(STATUS_CODE)).isEqualTo(HttpStatus.ACCEPTED);
        LifecycleOperations.verifyOperation(ids.get(OPERATION_ID), COMPLETED_STATE, OPERATION_TYPE_INSTANTIATE);

        String installedReleases = getReleasesList();
        checkReleaseState(instance.getWorkloadInstanceName(), true, installedReleases);
        checkSecretPresence(String.format(DOCKER_SECRET_RELEASE_NAME, instance.getWorkloadInstanceName()), true);
        return ids.get(WORKLOAD_INSTANCE_ID);
    }

    private Object instantiateThroughHelmfileBuilder(HelmfileBuilderRequest instance, Map<String, Boolean> releasesToCheck) {
        ResponseEntity<HashMap<String, Object>> response = LifecycleOperations.instantiateThroughHelmfileBuilder(instance);
        Map<String, Object> ids = getResponseBodyByInstantiate(response);
        assertThat(ids.get(STATUS_CODE)).isEqualTo(HttpStatus.ACCEPTED);
        LifecycleOperations.verifyOperation(ids.get(OPERATION_ID), COMPLETED_STATE, OPERATION_TYPE_INSTANTIATE);

        checkInstalledReleases(releasesToCheck);

        return ids.get(WORKLOAD_INSTANCE_ID);
    }

    private void upgrade(WorkloadInstanceTestData instanceForUpdate, Object workloadInstanceResponse,
                         Map<String, Boolean> releasesToCheck) {
        String operationId = updateAndCheckStatus(instanceForUpdate, workloadInstanceResponse);
        LifecycleOperations.verifyOperation(operationId, COMPLETED_STATE, OPERATION_TYPE_UPDATE);

        checkInstalledReleases(releasesToCheck);
    }

    private String updateAndCheckStatus(WorkloadInstanceTestData instanceForUpdate, Object workloadInstanceResponse) {
        ResponseEntity<String> response = LifecycleOperations.update(instanceForUpdate, workloadInstanceResponse);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        return getOperationId(response.getHeaders());
    }

    private void upgradeWithHelmfileBuilder(HelmfileBuilderRequest instance, Object workloadInstanceResponse,
                                            Map<String, Boolean> releasesToCheck) {
        ResponseEntity<String> response = LifecycleOperations.updateWithHelmfileBuilder(instance, workloadInstanceResponse);
        String operationId = getOperationId(response.getHeaders());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        LifecycleOperations.verifyOperation(operationId, COMPLETED_STATE, OPERATION_TYPE_UPDATE);

        checkInstalledReleases(releasesToCheck);
    }

    private void upgradeIntegrationChart(WorkloadInstanceTestData instanceForUpdate, Object workloadInstanceResponse,
                                         boolean isRunning) {
        String operationId = updateAndCheckStatus(instanceForUpdate, workloadInstanceResponse);
        LifecycleOperations.verifyOperation(operationId, COMPLETED_STATE, OPERATION_TYPE_UPDATE);

        String installedReleases = getReleasesList();
        checkReleaseState(instanceForUpdate.getWorkloadInstanceName(), true, installedReleases);
        checkPodStatus(isRunning);
    }

    private void reinstantiate(WorkloadInstanceTestData instanceForUpdate, Object workloadInstanceResponse,
                               Map<String, Boolean> releasesToCheck) {
        String operationId = updateAndCheckStatus(instanceForUpdate, workloadInstanceResponse);
        LifecycleOperations.verifyOperation(operationId, COMPLETED_STATE, OPERATION_TYPE_REINSTANTIATE);

        checkInstalledReleases(releasesToCheck);
    }

    private void reinstantiateIntegrationChart(WorkloadInstanceTestData instanceForUpdate, Object workloadInstanceResponse,
                                               boolean isRunning) {
        String operationId = updateAndCheckStatus(instanceForUpdate, workloadInstanceResponse);
        LifecycleOperations.verifyOperation(operationId, COMPLETED_STATE, OPERATION_TYPE_REINSTANTIATE);

        String installedReleases = getReleasesList();
        checkReleaseState(instanceForUpdate.getWorkloadInstanceName(), true, installedReleases);
        checkPodStatus(isRunning);
    }

    private void rollback(Object workloadInstanceResponse, RollbackData rollbackRequest,
                          Map<String, Boolean> releasesToCheck) {
        String operationId = manualRollbackAndCheckStatus(workloadInstanceResponse, rollbackRequest);
        LifecycleOperations.verifyOperation(operationId, COMPLETED_STATE, OPERATION_TYPE_ROLLBACK);

        checkInstalledReleases(releasesToCheck);
    }

    private void rollbackIntegrationChart(Object workloadInstanceResponse, String releaseName, RollbackData rollbackRequest,
                                          boolean isRunning) {
        String operationId = manualRollbackAndCheckStatus(workloadInstanceResponse, rollbackRequest);
        LifecycleOperations.verifyOperation(operationId, COMPLETED_STATE, OPERATION_TYPE_ROLLBACK);

        String installedReleases = getReleasesList();
        checkReleaseState(releaseName, true, installedReleases);
        checkPodStatus(isRunning);
    }

    private void terminate(Object workloadInstanceResponse, Map<String, Boolean> releasesToCheck) {
        ResponseEntity<String> terminate = LifecycleOperations.terminate(workloadInstanceResponse, clusterConfigInfoPath, false);
        String operationId = getOperationId(terminate.getHeaders());
        LifecycleOperations.verifyOperation(operationId, COMPLETED_STATE, OPERATION_TYPE_TERMINATE);

        checkInstalledReleases(releasesToCheck);
    }

    private void terminateIntegrationChart(Object workloadInstanceResponse) {
        ResponseEntity<String> terminate = LifecycleOperations.terminate(workloadInstanceResponse, clusterConfigInfoPath, false);
        String operationId = getOperationId(terminate.getHeaders());
        LifecycleOperations.verifyOperation(operationId, COMPLETED_STATE, OPERATION_TYPE_TERMINATE);
        checkPodStatus(false);
    }

    private Version.Item checkVersionsAndGetAny(Object workloadInstanceResponse) {
        List<Version.Item> versions = LifecycleOperations.getVersions(workloadInstanceResponse);
        assertThat(versions).isNotEmpty();
        assertThat(versions.size()).isEqualTo(4);
        Optional<Version.Item> possibleVersion = versions.stream().filter(item -> item.getVersion() == 2).findAny();
        assertThat(possibleVersion.isPresent()).isTrue();
        assertThat(versions.stream().map(Version.Item::getVersion).anyMatch(item -> item == 1)).isTrue();
        assertThat(versions.stream().map(Version.Item::getVersion).anyMatch(item -> item == 3)).isTrue();
        assertThat(versions.stream().map(Version.Item::getVersion).anyMatch(item -> item == 4)).isTrue();

        return possibleVersion.orElse(null);
    }

    private Integer getVersionAndCheckFields(Object workloadInstanceResponse, Version.Item version) {
        Version.Item response = LifecycleOperations.getVersion(workloadInstanceResponse, version);

        assertThat(response.getVersion()).isNotNull();
        assertThat(response.getHelmSourceVersion()).isNotEmpty();
        assertThat(response.getValuesVersion()).isNotEmpty();
        assertThat(response.getId()).isNotEmpty();
        return response.getVersion();
    }

    private Version.Item getVersionItemByNumber(Integer version, Object workloadInstanceResponse) {
        List<Version.Item> versions = LifecycleOperations.getVersions(workloadInstanceResponse);
        return versions.stream()
                .filter(item -> item.getVersion().equals(version))
                .findFirst()
                .orElse(null);
    }

    private Map<String, Boolean> getReleasesToCheck(Boolean a, Boolean b, Boolean c) {
        Map<String, Boolean> releasesToCheck = new HashMap<>();

        releasesToCheck.put(APP_A_RELEASE_NAME, a);
        releasesToCheck.put(APP_B_RELEASE_NAME, b);
        releasesToCheck.put(APP_C_RELEASE_NAME, c);

        return releasesToCheck;
    }

    private void checkInstalledReleases(Map<String, Boolean> expected) {
        String installedReleases = getReleasesList();

        expected.forEach((key, value) -> checkReleaseState(key, value, installedReleases));
    }

    private void checkReleaseState(String releaseName, Boolean installed, String installedReleases) {
        if (installed != null) {
            log.info("Checking release {} state is installed: {}", releaseName, installed);
            Pattern appPattern = Pattern.compile(releaseName);
            Matcher matcherApp = appPattern.matcher(installedReleases);
            assertThat(matcherApp.find()).isEqualTo(installed);
        }
    }

    private void checkSecretPresence(String secretName, boolean installed) {
        List<Secret> secrets = KubernetesApiUtils.getSecretsInNamespace(kubernetesClient, namespace);
        List<String> secretNames = secrets.stream()
                .map(Secret::getMetadata)
                .map(ObjectMeta::getName)
                .toList();
        if (installed) {
            log.info("Verifying that secret {} exists", secretName);
            assertThat(secretNames).contains(secretName);
        } else {
            log.info("Verifying that secret {} does not exist", secretName);
            assertThat(secretNames).doesNotContain(secretName);
        }
    }

    private String getReleasesList() {
        return Setup.execute(releaseListCommand + " " + kubeConfigPath, TEST_TIMEOUT);
    }

    private void checkCrdsIsInstalled() {
        Pattern crdPattern = Pattern.compile(CRD_RELEASE_NAME);
        String executeCrdResult = Setup.execute(EXECUTE_CRD_COMMAND + " " + kubeConfigPath, TEST_TIMEOUT);
        Matcher matcherCrd = crdPattern.matcher(executeCrdResult);
        assertThat(matcherCrd.find()).isTrue();
    }

    private void checkPodStatus(boolean isRunning) {
        log.info("Getting pods to check if state is {}", isRunning ? POD_STATE_RUNNING : "terminating");
        await()
                .pollInterval(new FibonacciPollInterval())
                .atMost(15, TimeUnit.SECONDS)
                .until(() -> checkPodState(isRunning));
    }

    private void checkReleasesOrdering(Map<String, LocalDateTime> releasesWithUpdateTime, List<Chart> charts) {
        List<String> sortedReleasesOrders = sortedReleasesOrders(charts);
        LocalDateTime localDateTimeFirstRelease = releasesWithUpdateTime.get(sortedReleasesOrders.get(0));
        LocalDateTime localDateTimeSecondRelease = releasesWithUpdateTime.get(sortedReleasesOrders.get(1));

        assertThat(localDateTimeFirstRelease.isBefore(localDateTimeSecondRelease)).isTrue();
    }

    private Map<String, LocalDateTime> getReleasesWithUpdateTime(List<String> chartNames, String releases) {
        Map<String, LocalDateTime> releasesWithUpdateTime = new HashMap<>();
        String[] lines = releases.split(NEW_LINE);
        for (String row : lines) {
            for (String chart : chartNames) {
                if (row.startsWith(chart)) {
                    Pattern datePattern = Pattern.compile(DATETIME_PATTERN);
                    Matcher matcher = datePattern.matcher(row);
                    while (matcher.find()) {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATETIME_FORMATTER);
                        LocalDateTime timeToReleaseUpdate = LocalDateTime.parse(matcher.group(), formatter);
                        releasesWithUpdateTime.put(chart, timeToReleaseUpdate);
                    }
                }
            }
        }
        return releasesWithUpdateTime;
    }

    private static Map<String, Object> fillAdditionalParams(boolean a, boolean b, boolean c) {
        Map<String, Object> testAdditionalParams = new HashMap<>();
        testAdditionalParams.put("cn-am-test-app-a.enabled", a);
        testAdditionalParams.put("cn-am-test-app-b.enabled", b);
        testAdditionalParams.put("cn-am-test-app-c.enabled", c);
        return testAdditionalParams;
    }

    private List<String> sortedReleasesOrders(List<Chart> charts) {
        return charts.stream()
                .sorted(Comparator.comparingInt(Chart::getOrder))
                .map(Chart::getName)
                .collect(Collectors.toList());
    }

    private void checkSecretsAreCreated() {
        log.info("Getting secrets with KubernetesClient API");
        List<Secret> secrets = KubernetesApiUtils.getSecretsInNamespace(kubernetesClient, namespace);
        boolean secretExists = secretWithNameIsPresent(secrets, SECRET_NAME);
        assertThat(secretExists).isTrue();
    }

    private static boolean checkPodState(boolean isRunning) {
        List<Pod> items = getPodsInNamespace(kubernetesClient, namespace);
        if (isRunning) {
            return getStatusesByPodName(items, POD_NAME)
                    .anyMatch(item -> item.equalsIgnoreCase(POD_STATE_RUNNING));
        } else {
            return items.isEmpty() || getStatusesByPodName(items, POD_NAME)
                    .anyMatch(item -> !item.equalsIgnoreCase(POD_STATE_RUNNING));
        }
    }

    private void checkHealthStatusOk(ResponseEntity<String> healthResponse) {
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(healthResponse.getBody()).contains("\"status\":\"UP\"");
    }

    private void verifyAutoRollbackCompleted(Map<String, Object> ids) {
        assertThat(ids.get(STATUS_CODE)).isEqualTo(HttpStatus.ACCEPTED);
        LifecycleOperations.verifyOperationWithoutInterrupting(ids.get(OPERATION_ID), COMPLETED_STATE, OPERATION_TYPE_ROLLBACK);
        assertThat(KubernetesApiUtils.getPodsInNamespace(kubernetesClient, namespace)).isEmpty();
    }

    private Map<String, Object> getResponseBodyByInstantiate(ResponseEntity<HashMap<String, Object>> response) {
        String operationId = getOperationId(response.getHeaders());
        Map<String, Object> ids = new HashMap<>();
        HashMap<String, Object> responseBody = Optional.ofNullable(response.getBody()).orElseThrow();
        ids.put("Operation", operationId);
        ids.put("WorkloadInstance", responseBody
                .get("workloadInstanceId"));
        ids.put(STATUS_CODE, response.getStatusCode());
        return ids;
    }

    private String getOperationId(HttpHeaders headers) {
        log.info("Headers are {}", headers);
        String operationUrl = Optional
                .ofNullable(headers.get("Location"))
                .orElseThrow()
                .get(0);
        assertThat(operationUrl).isNotEmpty();
        log.info("Operation url is {}", operationUrl);
        return extractOperationId(operationUrl);
    }

    private String extractOperationId(String operationUrl) {
        final String[] splitOperationUrl = operationUrl.split("/");
        return splitOperationUrl[splitOperationUrl.length - 1];
    }

    private String manualRollbackAndCheckStatus(Object workloadInstanceResponse, RollbackData rollbackRequest) {
        ResponseEntity<String> response = LifecycleOperations.manualRollback(workloadInstanceResponse, rollbackRequest);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        return getOperationId(response.getHeaders());
    }

    private void checkNamespaceDeletion(boolean deleteNamespace) {
        String action = deleteNamespace ? "deleted" : "present";
        log.info("Check if namespace {} is {}", namespace, action);

        await().pollInterval(Duration.ofSeconds(10))
                .atMost(Duration.ofSeconds(180))
                .until(() -> deleteNamespace != namespaceExist(kubernetesClient, namespace));
    }

}