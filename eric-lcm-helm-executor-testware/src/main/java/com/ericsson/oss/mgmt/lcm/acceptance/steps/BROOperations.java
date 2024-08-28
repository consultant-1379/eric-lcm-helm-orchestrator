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

import static org.awaitility.Awaitility.await;

import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.BRO_BASIC_URL;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.BRO_EXPORT_URL;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.BRO_GET_URL;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.BRO_IMPORT_URL;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.BRO_MANAGERS_IDENTIFIER;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.BRO_MANAGER_ID_KEY;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.BRO_NAME_KEY;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.BRO_PASSWORD_KEY;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.BRO_URI_KEY;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.RESPONSE_PREFIX;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.TestExecutionGlobalConfig.SERVICE_INSTANCE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.awaitility.core.ConditionEvaluationLogger;
import org.awaitility.pollinterval.FibonacciPollInterval;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.ericsson.oss.mgmt.lcm.acceptance.models.BRORequest;
import com.ericsson.oss.mgmt.lcm.acceptance.models.PagedBackup;
import com.ericsson.oss.mgmt.lcm.acceptance.models.PagedBackupManager;
import com.ericsson.oss.mgmt.lcm.acceptance.utils.Parser;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class BROOperations {

    private BROOperations() {}

    private static final RestTemplate REST_TEMPLATE;

    static {
        REST_TEMPLATE = new RestTemplate();
    }

    @Step("Create backup")
    public static ResponseEntity<HashMap<String, Object>> create(BRORequest instance) {
        log.info("Create backup");
        String url = String.format(BRO_BASIC_URL, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort());
        HttpEntity<Map<String, Object>> requestEntity = createBasicRequest(instance);
        ResponseEntity<HashMap<String, Object>> response = REST_TEMPLATE.exchange(url, HttpMethod.POST, requestEntity,
                                                                                  new ParameterizedTypeReference<>() { });
        log.info(RESPONSE_PREFIX, response);
        return response;
    }

    @Step("Get backup by managerId")
    public static PagedBackup getBackup(String managerId) {
        await()
                .conditionEvaluationListener(new ConditionEvaluationLogger())
                .pollInterval(new FibonacciPollInterval())
                .atMost(240, TimeUnit.SECONDS)
                .until(getResponse(managerId));
        ResponseEntity<String> response = executeGetRequest(managerId);
        log.info(RESPONSE_PREFIX, response);
        return Parser.parse(response.getBody(), PagedBackup.class);
    }

    @Step("Export backup to sftp server")
    public static ResponseEntity<HashMap<String, Object>> exportBackup(BRORequest request) {
        log.info("Export backup");
        String url = String.format(BRO_EXPORT_URL, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort());
        return executePostRequest(url, request);
    }

    @Step("Import backup from sftp server")
    public static void importBackup(BRORequest request) {
        log.info("Import backup");
        String url = String.format(BRO_IMPORT_URL, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort());
        executePostRequest(url, request);
    }

    @Step("Delete backup")
    public static void delete(BRORequest instance) {
        await()
                .conditionEvaluationListener(new ConditionEvaluationLogger())
                .pollInterval(new FibonacciPollInterval())
                .atMost(40, TimeUnit.SECONDS)
                .until(deleteResponse(instance));
    }

    @Step("Restore backup")
    public static ResponseEntity<HashMap<String, Object>> restore(BRORequest instance) {
        log.info("Restore backup");
        String url = String.format(BRO_BASIC_URL, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort());
        HttpEntity<Map<String, Object>> requestEntity = createBasicRequest(instance);
        ResponseEntity<HashMap<String, Object>> response = REST_TEMPLATE.exchange(url, HttpMethod.PUT, requestEntity,
                                                                                  new ParameterizedTypeReference<>() { });
        log.info(RESPONSE_PREFIX, response);
        return response;
    }

    @Step("Get backup managers")
    public static PagedBackupManager getBackupManagers() {
        log.info("Get backup managers");
        await()
                .conditionEvaluationListener(new ConditionEvaluationLogger())
                .pollInterval(new FibonacciPollInterval())
                .atMost(40, TimeUnit.SECONDS)
                .until(getManagers());
        ResponseEntity<String> response = executeGetManagersRequest();
        log.info(RESPONSE_PREFIX, response);
        return Parser.parse(response.getBody(), PagedBackupManager.class);
    }

    private static Callable<Boolean> getManagers() {
        return () -> {
            log.info("Get backup managers");
            try {
                ResponseEntity<String> response = executeGetManagersRequest();
                return response.getStatusCode() == HttpStatus.OK;
            } catch (Exception e) {
                return false;
            }
        };
    }

    private static Callable<Boolean> getResponse(String managerId) {
        return () -> {
            log.info("Get backup");
            try {
                ResponseEntity<String> response = executeGetRequest(managerId);
                return response.getStatusCode() == HttpStatus.OK;
            } catch (Exception e) {
                return false;
            }
        };
    }

    private static Callable<Boolean> deleteResponse(BRORequest request) {
        return () -> {
            log.info("Delete backup");
            try {
                ResponseEntity<HashMap<String, Object>> response = executeDeleteRequest(request);
                return response.getStatusCode() == HttpStatus.NO_CONTENT;
            } catch (Exception e) {
                return false;
            }
        };
    }

    private static ResponseEntity<String> executeGetManagersRequest() {
        String url = String.format(BRO_GET_URL, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort(), BRO_MANAGERS_IDENTIFIER);
        return executeBasicGetRequest(url);
    }

    private static ResponseEntity<String> executeGetRequest(String managerId) {
        String url = String.format(BRO_GET_URL, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort(), managerId);
        return executeBasicGetRequest(url);
    }

    private static ResponseEntity<HashMap<String, Object>> executeDeleteRequest(BRORequest request) {
        String url = String.format(BRO_BASIC_URL, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort());
        HttpEntity<Map<String, Object>> requestEntity = createBasicRequest(request);
        return REST_TEMPLATE.exchange(url, HttpMethod.DELETE, requestEntity, new ParameterizedTypeReference<>() { });
    }

    private static ResponseEntity<String> executeBasicGetRequest(String url) {
        HttpEntity<String> requestEntity = new HttpEntity<>(getHeaders());
        return REST_TEMPLATE.exchange(url, HttpMethod.GET, requestEntity, String.class);
    }

    private static ResponseEntity<HashMap<String, Object>> executePostRequest(String url, BRORequest request) {
        HttpEntity<Map<String, Object>> requestEntity = createExportImportRequest(request);
        ResponseEntity<HashMap<String, Object>> response = REST_TEMPLATE.exchange(url, HttpMethod.POST, requestEntity,
                                                                                  new ParameterizedTypeReference<>() { });
        log.info(RESPONSE_PREFIX, response);
        return response;
    }

    private static HttpEntity<Map<String, Object>> createBasicRequest(BRORequest request) {
        Map<String, Object> dto = new HashMap<>();
        dto.put(BRO_NAME_KEY, request.getBackupName());
        dto.put(BRO_MANAGER_ID_KEY, request.getBackupManagerId());
        return new HttpEntity<>(dto, getHeaders());
    }

    private static HttpEntity<Map<String, Object>> createExportImportRequest(BRORequest request) {
        Map<String, Object> dto = new HashMap<>();
        dto.put(BRO_NAME_KEY, request.getBackupName());
        dto.put(BRO_MANAGER_ID_KEY, request.getBackupManagerId());
        dto.put(BRO_URI_KEY, request.getUri());
        dto.put(BRO_PASSWORD_KEY, request.getPassword());
        return new HttpEntity<>(dto, getHeaders());
    }

    private static HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

}
