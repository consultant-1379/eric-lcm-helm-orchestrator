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

import static com.ericsson.oss.mgmt.lcm.acceptance.utils.FileUtils.getFileResource;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.TestExecutionGlobalConfig.SERVICE_INSTANCE;

import java.util.HashMap;
import java.util.List;

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

import com.ericsson.oss.mgmt.lcm.acceptance.models.ClusterConnectionInfoTestData;
import com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ClusterConnectionInfo {
    private ClusterConnectionInfo() {
    }

    private static final RestTemplate REST_TEMPLATE;

    static {
        REST_TEMPLATE = new RestTemplate();
    }

    @Step("upload a valid cluster connection info, {clusterConnectionInfoTestData.pathToFile}")
    public static ResponseEntity<HashMap<String, String>> uploadValidConnectionInfo(
            final ClusterConnectionInfoTestData clusterConnectionInfoTestData) {
        log.info("Will upload valid cluster connection info");
        String url = String.format(Constants.CLUSTER_CONNECTION_INFO_URL, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        FileSystemResource fileResource = getFileResource(clusterConnectionInfoTestData.getPathToFile());
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.TEXT_PLAIN);
        body.add("clusterConnectionInfo", new HttpEntity<>(fileResource, httpHeaders));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<HashMap<String, String>> response = REST_TEMPLATE.exchange(url, HttpMethod.POST, requestEntity,
                new ParameterizedTypeReference<HashMap<String, String>>() {
                });

        log.info("Response is {}", response);
        return response;
    }

    @Step("remove cluster connection info")
    public static void removeClusterConnectionInfo(final String clusterConnectionInfoId) {
        log.info("Will remove cluster connection info by id: " + clusterConnectionInfoId);

        String url = String.format(Constants.CLUSTER_CONNECTION_INFO_URL, SERVICE_INSTANCE.getIp(), SERVICE_INSTANCE.getPort());
        String entityUrl = url + "/" + clusterConnectionInfoId;
        REST_TEMPLATE.delete(entityUrl);
    }
}
