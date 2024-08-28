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

import static org.assertj.core.api.Assertions.assertThat;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.TestExecutionGlobalConfig.SERVICE_INSTANCE;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.ericsson.oss.mgmt.lcm.acceptance.utils.FileUtils;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Slf4j
public final class HelmChartRegistryOperations {
    private HelmChartRegistryOperations() {}

    private static final RestTemplate REST_TEMPLATE = new RestTemplate();

    private static final String API_CHARTS = "%s/api/charts";
    private static final String BASIC = "Basic %s";

    @Step("Upload a new chart version to chart registry")
    public static ResponseEntity<String> uploadHelmChart(String pathToChart) {
        var httpHeaders = createAuthHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        var requestEntity = new HttpEntity<>(FileUtils.getFileResource(pathToChart), httpHeaders);
        ResponseEntity<String> response = REST_TEMPLATE.exchange(String.format(API_CHARTS, SERVICE_INSTANCE.getChartRegistryUrl()),
                HttpMethod.POST, requestEntity, String.class);
        assertThat(response.getStatusCode()).isIn(HttpStatus.CREATED);
        log.info("Response is: " + response);
        return response;
    }

    private static HttpHeaders createAuthHeaders() {
        var httpHeaders = new HttpHeaders();
        var credentials = String.format("%s:%s", SERVICE_INSTANCE.getChartRegistryUsername(), SERVICE_INSTANCE.getChartRegistryPassword());
        var encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        httpHeaders.add(HttpHeaders.AUTHORIZATION, String.format(BASIC, encodedCredentials));
        return httpHeaders;
    }
}
