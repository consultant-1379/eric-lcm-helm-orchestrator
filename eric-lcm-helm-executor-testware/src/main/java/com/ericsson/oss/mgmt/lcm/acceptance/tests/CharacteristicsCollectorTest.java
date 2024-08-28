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

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.oss.mgmt.lcm.acceptance.steps.LifecycleOperations.deleteInstance;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.CHARACTERISTICS_REPORT_PATH;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.CPU_METRIC;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.INSTANTIATE_USE_CASE;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.INSTANTIATE_USE_CASE_DESCRIPTION;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.MEMORY_METRIC;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.POD_METRICS_COMMAND;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.REINSTANTIATE_USE_CASE;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.REINSTANTIATE_USE_CASE_DESCRIPTION;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.ROLLBACK_USE_CASE;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.ROLLBACK_USE_CASE_DESCRIPTION;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.TERMINATE_USE_CASE;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.TERMINATE_USE_CASE_DESCRIPTION;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.UPGRADE_USE_CASE;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.UPGRADE_USE_CASE_DESCRIPTION;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.CLUSTER_CONFIG_INFO_PATH;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.CLUSTER_CONFIG_INFO_PATH_LOCAL;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.COMPLETED_STATE;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.OPERATION_ID;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.OPERATION_TYPE_INSTANTIATE;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.OPERATION_TYPE_REINSTANTIATE;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.OPERATION_TYPE_ROLLBACK;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.OPERATION_TYPE_TERMINATE;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.OPERATION_TYPE_UPDATE;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.POD_NAME_COMMAND;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.VALUES_PATH;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.VALUES_TO_UPDATE_PATH;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.WORKLOAD_INSTANCE_ID;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.FileUtils.copyFile;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.TestExecutionGlobalConfig.SERVICE_INSTANCE;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ericsson.oss.mgmt.lcm.acceptance.models.RollbackData;
import com.ericsson.oss.mgmt.lcm.acceptance.models.WorkloadInstanceTestData;
import com.ericsson.oss.mgmt.lcm.acceptance.models.characteristics.CharacteristicsReport;
import com.ericsson.oss.mgmt.lcm.acceptance.models.characteristics.MetricResponse;
import com.ericsson.oss.mgmt.lcm.acceptance.models.characteristics.Resource;
import com.ericsson.oss.mgmt.lcm.acceptance.models.characteristics.ResourceConfiguration;
import com.ericsson.oss.mgmt.lcm.acceptance.models.characteristics.Result;
import com.ericsson.oss.mgmt.lcm.acceptance.models.characteristics.UsedResourcesConfiguration;
import com.ericsson.oss.mgmt.lcm.acceptance.steps.DatabaseCleaner;
import com.ericsson.oss.mgmt.lcm.acceptance.steps.HealthCheck;
import com.ericsson.oss.mgmt.lcm.acceptance.steps.LifecycleOperations;
import com.ericsson.oss.mgmt.lcm.acceptance.steps.Setup;
import com.ericsson.oss.mgmt.lcm.acceptance.utils.Parser;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CharacteristicsCollectorTest {
    private static final int TEST_TIMEOUT = 2;
    private static final String STATUS_CODE = "StatusCode";

    private static String namespace;
    private static String clusterConfigInfoPath;
    private static String deploymentNamespace;
    private static String podName;
    private static boolean isLocal;


    @BeforeClass
    public static void prepareData() {
        isLocal = SERVICE_INSTANCE.getIsLocal();
        clusterConfigInfoPath = isLocal ? CLUSTER_CONFIG_INFO_PATH_LOCAL : CLUSTER_CONFIG_INFO_PATH;
        namespace = SERVICE_INSTANCE.getNamespace();
        deploymentNamespace = SERVICE_INSTANCE.getDeploymentNamespace();
        podName = Setup.execute(String.format(POD_NAME_COMMAND, deploymentNamespace), TEST_TIMEOUT);

        if (!isLocal) {
            copyFile(SERVICE_INSTANCE.getClusterConnectionInfoPath(), clusterConfigInfoPath);
        }
    }

    @AfterTest
    public static void cleanDatabaseAfterTestsWithLocalProfile() {
        if (isLocal)
            DatabaseCleaner.cleanDatabaseAfterTests();
    }

    @Test(description = "Process series of use cases and collect the pod metrics for each use case")
    public void shouldSuccessfullyCollectMetricsForUseCases() {
        //Setup
        WorkloadInstanceTestData instance = new WorkloadInstanceTestData();
        instance.setHelmSourceLocation("src/main/resources/testData/instantiate/helmfile-test/");
        instance.setHelmSourceName("helmfile-test-1.2.3-4.tgz");
        instance.setWorkloadInstanceName("collect-metrics-for-report");
        instance.setNamespace(namespace);
        instance.setValuesYaml(VALUES_PATH);
        instance.setClusterConnectionLocation(clusterConfigInfoPath);

        Setup.zipUpHelmfile(instance);

        WorkloadInstanceTestData instanceForUpdate = new WorkloadInstanceTestData();
        instanceForUpdate.setValuesYaml(VALUES_TO_UPDATE_PATH);
        instanceForUpdate.setClusterConnectionLocation(clusterConfigInfoPath);

        RollbackData rollbackRequest = new RollbackData(null, clusterConfigInfoPath);

        CharacteristicsReport report = new CharacteristicsReport();

        ResponseEntity<String> healthResponse = HealthCheck.getHealthState();
        checkHealthStatusOk(healthResponse);

        //Instantiate workloadInstance
        Object workloadInstanceResponse =
                instantiate(instance);
        collectMetricsToReport(report, INSTANTIATE_USE_CASE, INSTANTIATE_USE_CASE_DESCRIPTION);

        //Upgrade workloadInstance
        upgrade(instanceForUpdate, workloadInstanceResponse);
        collectMetricsToReport(report, UPGRADE_USE_CASE, UPGRADE_USE_CASE_DESCRIPTION);

        //Terminate workloadInstance
        terminate(workloadInstanceResponse);
        collectMetricsToReport(report, TERMINATE_USE_CASE, TERMINATE_USE_CASE_DESCRIPTION);

        //Reinstantiate workloadInstance
        instanceForUpdate.setValuesYaml(null);
        reinstantiate(instanceForUpdate, workloadInstanceResponse);
        collectMetricsToReport(report, REINSTANTIATE_USE_CASE, REINSTANTIATE_USE_CASE_DESCRIPTION);

        //Rollback workloadInstance to the previous version
        rollback(workloadInstanceResponse, rollbackRequest);
        collectMetricsToReport(report, ROLLBACK_USE_CASE, ROLLBACK_USE_CASE_DESCRIPTION);

        //Terminate and delete
        terminateAndDeleteInstance(workloadInstanceResponse);

        saveReportToDirectory(report);
    }

    private void terminateAndDeleteInstance(Object workloadInstance) {
        ResponseEntity<String> response = LifecycleOperations.terminate(workloadInstance, clusterConfigInfoPath, true);
        String operationId = getOperationId(response.getHeaders());
        LifecycleOperations.verifyOperation(operationId, COMPLETED_STATE, OPERATION_TYPE_TERMINATE);
        deleteInstanceAndCheckStatus(workloadInstance);
    }

    private void deleteInstanceAndCheckStatus(Object workloadInstance) {
        ResponseEntity<String> response = deleteInstance(workloadInstance);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private Object instantiate(WorkloadInstanceTestData instance) {
        ResponseEntity<HashMap<String, Object>> instantiate = LifecycleOperations.instantiate(instance);
        Map<String, Object> ids = getResponseBodyByInstantiate(instantiate);
        LifecycleOperations.verifyOperation(ids.get(OPERATION_ID), COMPLETED_STATE, OPERATION_TYPE_INSTANTIATE);
        assertThat(ids.get(STATUS_CODE)).isEqualTo(HttpStatus.ACCEPTED);
        return ids.get(WORKLOAD_INSTANCE_ID);
    }

    private void upgrade(WorkloadInstanceTestData instanceForUpdate, Object workloadInstanceResponse) {
        ResponseEntity<String> response = LifecycleOperations.update(instanceForUpdate, workloadInstanceResponse);
        String operationId =  getOperationId(response.getHeaders());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        LifecycleOperations.verifyOperation(operationId, COMPLETED_STATE, OPERATION_TYPE_UPDATE);
    }

    private void reinstantiate(WorkloadInstanceTestData instanceForUpdate, Object workloadInstanceResponse) {
        ResponseEntity<String> response = LifecycleOperations.update(instanceForUpdate, workloadInstanceResponse);
        String operationId =  getOperationId(response.getHeaders());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        LifecycleOperations.verifyOperation(operationId, COMPLETED_STATE, OPERATION_TYPE_REINSTANTIATE);
    }

    private void rollback(Object workloadInstanceResponse, RollbackData rollbackRequest) {
        String operationId = manualRollbackAndCheckStatus(workloadInstanceResponse, rollbackRequest);
        LifecycleOperations.verifyOperation(operationId, COMPLETED_STATE, OPERATION_TYPE_ROLLBACK);
    }

    private String manualRollbackAndCheckStatus(Object workloadInstanceResponse, RollbackData rollbackRequest) {
        ResponseEntity<String> response = LifecycleOperations.manualRollback(workloadInstanceResponse, rollbackRequest);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        return getOperationId(response.getHeaders());
    }

    private void terminate(Object workloadInstanceResponse) {
        ResponseEntity<String> terminate = LifecycleOperations.terminate(workloadInstanceResponse, clusterConfigInfoPath, false);
        String operationId = getOperationId(terminate.getHeaders());
        LifecycleOperations.verifyOperation(operationId, COMPLETED_STATE, OPERATION_TYPE_TERMINATE);
    }

    private void collectMetricsToReport(CharacteristicsReport report, String useCase, String description) {
        Result result = new Result();
        result.setUseCase(useCase);
        result.setDescription(description);
        UsedResourcesConfiguration usedResourcesConfiguration = result.getUsedResourcesConfiguration();
        Resource resource = usedResourcesConfiguration
                .getResources()
                .stream()
                .findFirst()
                .orElseThrow();

        resource.setPod(podName);
        Resource.Container container = resource.getContainers()
                .stream()
                .findFirst()
                .orElseThrow();

        Map<String, String> metrics = getPodMetrics(podName);
        container.setCpuReq(metrics.get(CPU_METRIC));
        container.setMemReq(metrics.get(MEMORY_METRIC));

        report.getReport()
                .getResults()
                .add(result);
    }

    private Map<String, String> getPodMetrics(String podName) {
        String command = String.format(POD_METRICS_COMMAND, podName, deploymentNamespace);
        String commandOutput = Setup.execute(command, TEST_TIMEOUT);

        MetricResponse podMetricsResponse = Parser.parse(commandOutput, MetricResponse.class);

        MetricResponse.Container executorContainer = podMetricsResponse.getContainers()
                .stream()
                .filter(container -> container.getName().equals("eric-lcm-helm-executor"))
                .findFirst()
                .orElseThrow();
        String cpu = executorContainer.getUsage().getCpu();
        cpu = cpu.replace("n", "");
        String memory = executorContainer.getUsage().getMemory();

        Map<String, String> metrics = new HashMap<>();
        metrics.put(CPU_METRIC, cpu);
        metrics.put(MEMORY_METRIC, memory);

        return metrics;
    }

    private void setPodNameToReport(CharacteristicsReport report) {
        report.getReport()
                .getResourceConfigurations()
                .stream()
                .map(ResourceConfiguration::getResources)
                .flatMap(Collection::stream)
                .forEach(resource -> resource.setPod(podName));

        report.getReport()
                .getResults()
                .stream()
                .map(Result::getMetrics)
                .flatMap(Collection::stream)
                .forEach(metric -> metric.setPod(podName));
    }

    private void saveReportToDirectory(CharacteristicsReport report) {
        File reportFile = new File(CHARACTERISTICS_REPORT_PATH);
        setPodNameToReport(report);
        Parser.writeToFile(reportFile, report);
    }

    private void checkHealthStatusOk(ResponseEntity<String> healthResponse) {
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(healthResponse.getBody()).contains("\"status\":\"UP\"");
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
}
