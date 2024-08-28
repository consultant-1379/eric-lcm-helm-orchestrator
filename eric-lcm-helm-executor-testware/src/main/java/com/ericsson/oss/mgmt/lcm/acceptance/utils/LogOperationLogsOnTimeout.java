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

package com.ericsson.oss.mgmt.lcm.acceptance.utils;

import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.GET_OPERATION_LOGS_URL;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.TestExecutionGlobalConfig.SERVICE_INSTANCE;

import org.awaitility.core.ConditionEvaluationLogger;
import org.awaitility.core.TimeoutEvent;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * This class is an extension of the {@link ConditionEvaluationLogger} class in awaitility.
 * This extension logs the output of the operations/{id}/logs endpoint if the operation times out.
 */
@Slf4j
public class LogOperationLogsOnTimeout extends ConditionEvaluationLogger {

    private static final RestTemplate REST_TEMPLATE;

    static {
        REST_TEMPLATE = new RestTemplate();
    }

    private final String operationId;

    public LogOperationLogsOnTimeout(String operationId) {
        this.operationId = operationId;
    }

    @Override
    public void onTimeout(final TimeoutEvent timeoutEvent) {
        String url = String.format(GET_OPERATION_LOGS_URL, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort(),
                operationId);
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> response = REST_TEMPLATE.exchange(url, HttpMethod.GET, requestEntity,
                String.class);
        String body = response.getBody();
        log.info("Operation logs are:\n{}", body);
        super.onTimeout(timeoutEvent);
    }
}
