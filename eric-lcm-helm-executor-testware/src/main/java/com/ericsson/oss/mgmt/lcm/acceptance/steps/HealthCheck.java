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

import static com.ericsson.oss.mgmt.lcm.acceptance.utils.TestExecutionGlobalConfig.SERVICE_INSTANCE;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class HealthCheck {
    private HealthCheck(){}

    private static final RestTemplate REST_TEMPLATE;

    static {
        REST_TEMPLATE = new RestTemplate();
    }

    @Step("Check the health of the service")
    public static ResponseEntity<String> getHealthState() {
        String url = String.format(Constants.HEALTH_URL, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort());

        ResponseEntity<String> healthResponse = REST_TEMPLATE.getForEntity(url, String.class);
        log.info("Health Status is: {}", healthResponse.getBody());
        return healthResponse;
    }

    @Step("Get health of the BRO")
    public static ResponseEntity<String> getBROHealth() {
        log.info("Verifying BRO is healthy");
        String url = String.format(Constants.BRO_HEALTH_URL, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort());
        return REST_TEMPLATE.getForEntity(url, String.class);
    }

}
