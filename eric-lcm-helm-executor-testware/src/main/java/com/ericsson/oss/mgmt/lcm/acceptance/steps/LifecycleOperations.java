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

package com.ericsson.oss.mgmt.lcm.acceptance.steps;

import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.ADDITIONAL_PARAMETERS;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.IS_URL_TO_HELM_REGISTRY_KEY;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.POST_INSTANCE_HELMFILE_FETCHER;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.PUT_INSTANCE_HELMFILE_FETCHER;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.URL_KEY;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.WORKLOAD_INSTANCE_WITH_URL_PUT_REQUEST_DTO;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.WORKLOAD_INSTANCE_WITH_URL_REQUEST_DTO;
import static org.awaitility.Awaitility.await;

import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.CHARTS_KEY;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.CLUSTER_KEY;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.CLUSTER_REQUEST_KEY;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.GET_OPERATIONS_URL;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.GET_OPERATION_URL;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.GET_VERSIONS_URL;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.GET_VERSION_URL;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.GLOBAL_VALUES_KEY;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.HELMFILE_BUILDER_WORKLOAD_INSTANCES_URL;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.HELM_SOURCE_KEY;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.NAMESPACE_KEY;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.PUT_HELMFILE_BUILDER_WORKLOAD_INSTANCES_URL;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.PUT_WORKLOAD_INSTANCES_URL;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.REPOSITORY_KEY;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.RESPONSE_PREFIX;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.TIMEOUT_KEY;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.TYPE_TERMINATE;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.VALUES_KEY;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.WORKLOAD_INSTANCES_OPERATIONS_URL;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.WORKLOAD_INSTANCES_URL;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.WORKLOAD_INSTANCE_NAME_KEY;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.WORKLOAD_INSTANCE_POST_HELMFILE_BUILDER_REQUEST_DTO;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.WORKLOAD_INSTANCE_POST_REQUEST_DTO;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.WORKLOAD_INSTANCE_PUT_HELMFILE_BUILDER_REQUEST_DTO;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.FileUtils.getFileResource;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.TestExecutionGlobalConfig.SERVICE_INSTANCE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.ericsson.oss.mgmt.lcm.acceptance.models.WorkloadInstanceTestDataWithUrl;
import org.awaitility.core.ConditionTimeoutException;
import org.awaitility.pollinterval.FibonacciPollInterval;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.ericsson.oss.mgmt.lcm.acceptance.models.HelmfileBuilderRequest;
import com.ericsson.oss.mgmt.lcm.acceptance.models.PagedOperation;
import com.ericsson.oss.mgmt.lcm.acceptance.models.RollbackData;
import com.ericsson.oss.mgmt.lcm.acceptance.models.Version;
import com.ericsson.oss.mgmt.lcm.acceptance.models.WorkloadInstanceTestData;
import com.ericsson.oss.mgmt.lcm.acceptance.utils.FileUtils;
import com.ericsson.oss.mgmt.lcm.acceptance.utils.LogOperationLogsOnTimeout;
import com.ericsson.oss.mgmt.lcm.acceptance.utils.Parser;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class LifecycleOperations {

    private LifecycleOperations(){}

    private static final RestTemplate REST_TEMPLATE;

    static {
        REST_TEMPLATE = new RestTemplate();
    }

    @Step("Instantiate the helmfile archive with name {instance.workloadInstanceName}")
    public static ResponseEntity<HashMap<String, Object>> instantiate(final WorkloadInstanceTestData instance) {
        log.info("Instantiate a helmfile");
        String url = String.format(WORKLOAD_INSTANCES_URL, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort());
        HttpEntity<MultiValueMap<String, Object>> requestEntity = createInstantiateRequestBody(instance);

        ResponseEntity<HashMap<String, Object>> response = REST_TEMPLATE.exchange(url, HttpMethod.POST, requestEntity,
                new ParameterizedTypeReference<>() { });

        log.info(RESPONSE_PREFIX, response);
        return response;
    }

    @Step("Instantiate workload instance with name {instance.workloadInstanceName} through helmfile builder")
    public static ResponseEntity<HashMap<String, Object>> instantiateThroughHelmfileBuilder(HelmfileBuilderRequest instance) {
        log.info("Instantiate a helmfile through helmfile builder");
        String url = String.format(HELMFILE_BUILDER_WORKLOAD_INSTANCES_URL, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort());
        HttpEntity<MultiValueMap<String, Object>> requestEntity = createInstantiateHelmfileBuilderRequestBody(instance);

        ResponseEntity<HashMap<String, Object>> response = REST_TEMPLATE.exchange(url, HttpMethod.POST, requestEntity,
                                          new ParameterizedTypeReference<>() { });
        log.info(RESPONSE_PREFIX, response);
        return response;
    }

    @Step("Instantiate the workloadInstance with name {instanceDataWithUrl.workloadInstanceName}")
    public static ResponseEntity<HashMap<String, Object>> instantiateThroughHelmfileFetcher(WorkloadInstanceTestDataWithUrl instanceDataWithUrl) {
        log.info("Instantiate via helmfile fetcher");
        var url = String.format(POST_INSTANCE_HELMFILE_FETCHER, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort());
        HttpEntity<MultiValueMap<String, Object>> requestEntity = createHttpEntityForInstantiateViaHelmfileFetcher(instanceDataWithUrl);
        ResponseEntity<HashMap<String, Object>> response = REST_TEMPLATE.exchange(url, HttpMethod.POST, requestEntity,
                new ParameterizedTypeReference<>() { });
        log.info(RESPONSE_PREFIX, response);
        return response;
    }

    @Step("Update the workloadInstance with name {instanceDataWithUrl.workloadInstanceName}")
    public static ResponseEntity<HashMap<String, Object>> updateThroughHelmfileFetcher(WorkloadInstanceTestDataWithUrl instanceDataWithUrl,
                                                                                       Object workloadInstance) {
        log.info("Update the workload instance {} with new helm source", workloadInstance);
        var url = String.format(PUT_INSTANCE_HELMFILE_FETCHER, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort(), workloadInstance);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = createHttpEntityForUpdateViaHelmfileFetcher(instanceDataWithUrl);
        ResponseEntity<HashMap<String, Object>> response = REST_TEMPLATE.exchange(url, HttpMethod.PUT, requestEntity,
                new ParameterizedTypeReference<>() { });
        log.info(RESPONSE_PREFIX, response);
        return response;
    }

    @Step("Verify that the operation {operationId} reaches the state {state}")
    public static void verifyOperation(final Object operationId, final String state, final String type) {
        log.info("Verify operation reaches state");
        await()
                .conditionEvaluationListener(new LogOperationLogsOnTimeout((String) operationId))
                .pollInterval(new FibonacciPollInterval())
                .atMost(240, TimeUnit.SECONDS)
                .until(operationReaches((String) operationId, state, type));
    }

    @Step("Verify that the operation {operationId} reaches the state {state} without interrupting")
    public static void verifyOperationWithoutInterrupting(Object operationId, String state, String type) {
        log.info("Verify operation reaches state");
        await()
                .conditionEvaluationListener(new LogOperationLogsOnTimeout((String) operationId))
                .pollInterval(new FibonacciPollInterval())
                .atMost(240, TimeUnit.SECONDS)
                .until(operationReachesNoInterrupt((String) operationId, state, type));
    }


    @Step("Terminate the helmfile archive with name {instance.workloadInstanceName}")
    public static ResponseEntity<String> terminate(final Object workloadInstance, String clusterConnectionInfoPath,
                                                   boolean deleteNamespace) {
        log.info("Terminate the workload instance {}", workloadInstance);
        String url = String.format(WORKLOAD_INSTANCES_OPERATIONS_URL, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort(),
                workloadInstance);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = createTerminateRequestBody(clusterConnectionInfoPath, deleteNamespace);

        ResponseEntity<String> response = REST_TEMPLATE.exchange(url, HttpMethod.POST, requestEntity, String.class);

        log.info(RESPONSE_PREFIX, response);
        return response;
    }

    @Step("Reinstantiate the helmsource archive with name {instance.workloadInstanceName}")
    public static ResponseEntity<String> reinstantiate(Object workloadInstance) {
        log.info("Reinstantiate the workload instance {}", workloadInstance);
        String url = String.format(PUT_WORKLOAD_INSTANCES_URL, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort(),
                                   workloadInstance);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = createWorkloadInstancePutRequestBody();

        ResponseEntity<String> response = REST_TEMPLATE.exchange(url, HttpMethod.PUT, requestEntity, String.class);

        log.info(RESPONSE_PREFIX, response);
        return response;
    }

    @Step("Rollback the workload instance with name {instance.workloadInstanceName}")
    public static ResponseEntity<String> manualRollback(Object workloadInstance, RollbackData request) {
        log.info("Update the workload instance {}", workloadInstance);
        String url = String.format(WORKLOAD_INSTANCES_OPERATIONS_URL, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort(),
                workloadInstance);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = createRollbackRequestBody(request);

        ResponseEntity<String> response = REST_TEMPLATE.exchange(url, HttpMethod.PUT, requestEntity, String.class);

        log.info(RESPONSE_PREFIX, response);
        return response;
    }

    @Step("Update the helmsource archive with name {instance.workloadInstanceName}")
    public static ResponseEntity<String> update(WorkloadInstanceTestData instanceData, Object workloadInstance) {
        log.info("Update the workload instance {} with new helm source", workloadInstance);
        String url = String.format(PUT_WORKLOAD_INSTANCES_URL, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort(),
                                   workloadInstance);

        HttpEntity<MultiValueMap<String, Object>> requestEntity =
                createUpdateRequestBody(instanceData);

        ResponseEntity<String> response = REST_TEMPLATE.exchange(url, HttpMethod.PUT, requestEntity, String.class);

        log.info(RESPONSE_PREFIX, response);
        return response;
    }

    @Step("Update workload instance archive with name {instance.workloadInstanceName} through helmfile builder")
    public static ResponseEntity<String> updateWithHelmfileBuilder(HelmfileBuilderRequest instanceData, Object workloadInstance) {
        log.info("Update the workload instance {} with new helm source", workloadInstance);
        String url = String.format(PUT_HELMFILE_BUILDER_WORKLOAD_INSTANCES_URL, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort(),
                                   workloadInstance);

        HttpEntity<MultiValueMap<String, Object>> requestEntity =
                createUpdateWithHelmfileBuilderRequestBody(instanceData);

        ResponseEntity<String> response = REST_TEMPLATE.exchange(url, HttpMethod.PUT, requestEntity, String.class);

        log.info(RESPONSE_PREFIX, response);
        return response;
    }

    @Step("Delete the workload Instance with name {instance.workloadInstanceName}")
    public static ResponseEntity<String> deleteInstance(final Object workloadInstance) {
        log.info("Delete the workload instance {}", workloadInstance);
        String url = String.format(WORKLOAD_INSTANCES_URL, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort()) + "/"
                + workloadInstance;

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> response = REST_TEMPLATE.exchange(url, HttpMethod.DELETE, requestEntity, String.class);
        return response;
    }

    @Step("Get versions by workload instance {workloadInstance}")
    public static List<Version.Item> getVersions(final Object workloadInstance) {
        String url = String.format(GET_VERSIONS_URL, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort(),
                                   workloadInstance);
        String body = queryByGetMethod(url);
        return Parser.parse(body, Version.class).getContent();
    }

    @Step("Get operations by workload instance {workloadInstance}")
    public static List<PagedOperation.Operation> getOperationsByWorkloadId(final Object workloadInstance) {
        String url = String.format(GET_OPERATIONS_URL, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort(),
                workloadInstance);
        String body = queryByGetMethod(url);

        return Parser.parse(body, PagedOperation.class).getContent();
    }

    @Step("Get version by workload instance {workloadInstance} and version {version}")
    public static Version.Item getVersion(final Object workloadInstance, Version.Item version) {
        String url = String.format(GET_VERSION_URL, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort(),
                                   workloadInstance, version.getVersion());
        String body = queryByGetMethod(url);
        return Parser.parse(body, Version.Item.class);
    }

    private static HttpEntity<MultiValueMap<String, Object>> createInstantiateRequestBody(
            final WorkloadInstanceTestData instance) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        String pathToHelmSource = instance.getHelmSourceLocation() + instance.getHelmSourceName();
        body.add(HELM_SOURCE_KEY, getFileResource(pathToHelmSource));
        Map<String, Object> dto = new HashMap<>();
        dto.put(WORKLOAD_INSTANCE_NAME_KEY, instance.getWorkloadInstanceName());
        dto.put(NAMESPACE_KEY, instance.getNamespace());

        Optional.ofNullable(instance.getClusterConnectionInfo())
                .ifPresent(clusterConnectionInfo -> dto.put(CLUSTER_KEY, clusterConnectionInfo));

        Optional.ofNullable(instance.getTimeout())
                .ifPresent(timeout -> dto.put(TIMEOUT_KEY, timeout));

        Optional.ofNullable(instance.getAdditionalParameters())
                .ifPresent(item -> dto.putAll(instance.getAdditionalParameters()));

        body.add(WORKLOAD_INSTANCE_POST_REQUEST_DTO, dto);
        Optional.ofNullable(instance.getValuesYaml())
                .map(FileUtils::getFileResource)
                .ifPresent(item -> body.add(VALUES_KEY, item));
        addClusterConnectionIfRequired(instance.getClusterConnectionLocation(), body);

        return new HttpEntity<>(body, headers);
    }

    private static HttpEntity<MultiValueMap<String, Object>> createInstantiateHelmfileBuilderRequestBody(
            HelmfileBuilderRequest instance) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        Map<String, Object> dto = new HashMap<>();
        dto.put(WORKLOAD_INSTANCE_NAME_KEY, instance.getWorkloadInstanceName());
        dto.put(NAMESPACE_KEY, instance.getNamespace());
        dto.put(REPOSITORY_KEY, instance.getRepository());
        dto.put(CHARTS_KEY, instance.getCharts());
        dto.put(TIMEOUT_KEY, instance.getTimeout());

        Optional.ofNullable(instance.getClusterConnectionInfo())
                .ifPresent(clusterConnectionInfo -> dto.put(CLUSTER_KEY, clusterConnectionInfo));

        Optional.ofNullable(instance.getAdditionalParameters())
                .ifPresent(item -> dto.putAll(instance.getAdditionalParameters()));

        body.add(WORKLOAD_INSTANCE_POST_HELMFILE_BUILDER_REQUEST_DTO, dto);
        Optional.ofNullable(instance.getValuesYaml())
                .map(FileUtils::getFileResource)
                .ifPresent(item -> body.add(GLOBAL_VALUES_KEY, item));
        addClusterConnectionIfRequired(instance.getClusterConnectionLocation(), body);

        return new HttpEntity<>(body, headers);
    }

    private static HttpEntity<MultiValueMap<String, Object>> createTerminateRequestBody(String clusterConnectionInfoPath,
                                                                                        boolean deleteNamespace) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        Map<String, String> dto = new HashMap<>();
        dto.put("type", TYPE_TERMINATE);
        dto.put("deleteNamespace", Boolean.toString(deleteNamespace));

        body.add("workloadInstanceOperationPostRequestDto", dto);
        addClusterConnectionIfRequired(clusterConnectionInfoPath, body);

        return new HttpEntity<>(body, headers);
    }

    private static HttpEntity<MultiValueMap<String, Object>> createUpdateRequestBody(
            WorkloadInstanceTestData instance) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        if (instance.getHelmSourceLocation() != null && instance.getHelmSourceName() != null) {
            String pathToHelmSource = instance.getHelmSourceLocation() + instance.getHelmSourceName();
            body.add(HELM_SOURCE_KEY, getFileResource(pathToHelmSource));
        }
        Optional.ofNullable(instance.getValuesYaml())
                .map(FileUtils::getFileResource)
                .ifPresent(item -> body.add(VALUES_KEY, item));
        addClusterConnectionIfRequired(instance.getClusterConnectionLocation(), body);
        Optional.ofNullable(instance.getAdditionalParameters())
                .ifPresent(item -> body.add("workloadInstancePutRequestDto", item));

        return new HttpEntity<>(body, headers);
    }

    private static HttpEntity<MultiValueMap<String, Object>> createUpdateWithHelmfileBuilderRequestBody(
            HelmfileBuilderRequest instance) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        Map<String, Object> dto = new HashMap<>();
        dto.put(REPOSITORY_KEY, instance.getRepository());
        dto.put(CHARTS_KEY, instance.getCharts());

        Optional.ofNullable(instance.getAdditionalParameters())
                .ifPresent(item -> dto.putAll(instance.getAdditionalParameters()));
        body.add(WORKLOAD_INSTANCE_PUT_HELMFILE_BUILDER_REQUEST_DTO, dto);

        Optional.ofNullable(instance.getValuesYaml())
                .map(FileUtils::getFileResource)
                .ifPresent(item -> body.add(VALUES_KEY, item));
        addClusterConnectionIfRequired(instance.getClusterConnectionLocation(), body);

        return new HttpEntity<>(body, headers);
    }

    private static HttpEntity<MultiValueMap<String, Object>> createWorkloadInstancePutRequestBody() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        return new HttpEntity<>(body, headers);
    }

    private static HttpEntity<MultiValueMap<String, Object>> createRollbackRequestBody(RollbackData data) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("workloadInstanceOperationPutRequestDto", data.getVersionRequest());
        addClusterConnectionIfRequired(data.getClusterConnectionInfoPath(), body);

        return new HttpEntity<>(body, headers);
    }

    private static Callable<Boolean> operationReaches(final String operationId, final String state, final String type) {
        return () -> {
            String url = String.format(GET_OPERATION_URL, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort(),
                                       operationId);
            String body = queryByGetMethod(url);
            log.info("Operation state is: {}", body);
            if (body.contains("FAILED") || !body.contains(type)) {
                throw new ConditionTimeoutException("State became FAILED or type became different with primary "
                                                            + "(AUTOROLLBACK was triggered), no need to wait more.");
            }
            return operationStateReaches(body, state) && body.contains(type);
        };
    }

    private static Boolean operationStateReaches(String body, String state) {
        String expectedState = String.format("\"state\":\"%s\"", state);
        return body.contains(expectedState);
    }

    private static HttpEntity<FileSystemResource> getTextPlainEntity(FileSystemResource file) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.TEXT_PLAIN);
        return new HttpEntity<>(file, httpHeaders);
    }

    private static void addClusterConnectionIfRequired(String clusterLocation, MultiValueMap<String, Object> body) {
        Optional.ofNullable(clusterLocation)
                .map(FileUtils::getFileResource)
                .map(LifecycleOperations::getTextPlainEntity)
                .ifPresent(item -> body.add(CLUSTER_REQUEST_KEY, item));
    }

    private static String queryByGetMethod(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> response = REST_TEMPLATE.exchange(url, HttpMethod.GET, requestEntity, String.class);
        return response.getBody();
    }

    private static HttpEntity<MultiValueMap<String, Object>> createHttpEntityForInstantiateViaHelmfileFetcher(
            WorkloadInstanceTestDataWithUrl instanceDataWithUrl) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();

        Map<String, Object> workloadInstanceWithUrl = new HashMap<>();
        workloadInstanceWithUrl.put(URL_KEY, instanceDataWithUrl.getUrl());
        workloadInstanceWithUrl.put(WORKLOAD_INSTANCE_NAME_KEY, instanceDataWithUrl.getWorkloadInstanceName());
        workloadInstanceWithUrl.put(NAMESPACE_KEY, instanceDataWithUrl.getNamespace());
        Optional.ofNullable(instanceDataWithUrl.getClusterConnectionInfo())
                .ifPresent(clusterConnectionInfo -> workloadInstanceWithUrl.put(CLUSTER_KEY, clusterConnectionInfo));
        Optional.ofNullable(instanceDataWithUrl.getAdditionalParameters())
                .ifPresent(additionalParams -> workloadInstanceWithUrl.put(ADDITIONAL_PARAMETERS, additionalParams));

        requestBody.add(WORKLOAD_INSTANCE_WITH_URL_REQUEST_DTO, workloadInstanceWithUrl);
        requestBody.add(IS_URL_TO_HELM_REGISTRY_KEY, instanceDataWithUrl.isUrlToHelmRegistry());
        requestBody.add(VALUES_KEY, FileUtils.getFileResource(instanceDataWithUrl.getValuesYaml()));
        return new HttpEntity<>(requestBody, headers);
    }

    private static HttpEntity<MultiValueMap<String, Object>> createHttpEntityForUpdateViaHelmfileFetcher(
            WorkloadInstanceTestDataWithUrl instanceDataWithUrl) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();

        Map<String, Object> workloadInstanceWithUrl = new HashMap<>();
        workloadInstanceWithUrl.put(URL_KEY, instanceDataWithUrl.getUrl());
        Optional.ofNullable(instanceDataWithUrl.getClusterConnectionInfo())
                .ifPresent(clusterConnectionInfo -> workloadInstanceWithUrl.put(CLUSTER_KEY, clusterConnectionInfo));
        Optional.ofNullable(instanceDataWithUrl.getAdditionalParameters())
                .ifPresent(additionalParams -> workloadInstanceWithUrl.put(ADDITIONAL_PARAMETERS, additionalParams));

        requestBody.add(WORKLOAD_INSTANCE_WITH_URL_PUT_REQUEST_DTO, workloadInstanceWithUrl);
        requestBody.add(IS_URL_TO_HELM_REGISTRY_KEY, instanceDataWithUrl.isUrlToHelmRegistry());
        requestBody.add(VALUES_KEY, FileUtils.getFileResource(instanceDataWithUrl.getValuesYaml()));
        return new HttpEntity<>(requestBody, headers);
    }

    private static Callable<Boolean> operationReachesNoInterrupt(String operationId, String state, String type) {
        return () -> {
            String url = String.format(GET_OPERATION_URL, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort(),
                    operationId);
            String body = queryByGetMethod(url);
            log.info("Operation state is: {}", body);
            return operationStateReaches(body, state) && body.contains(type);
        };
    }
}