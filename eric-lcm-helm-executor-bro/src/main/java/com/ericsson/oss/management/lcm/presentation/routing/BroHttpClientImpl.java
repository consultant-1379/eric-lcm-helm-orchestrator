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

package com.ericsson.oss.management.lcm.presentation.routing;

import com.ericsson.oss.management.lcm.presentation.exceptions.BackupAndRestoreConnectionException;
import com.ericsson.oss.management.lcm.presentation.exceptions.BackupAndRestoreHttpClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;


@Slf4j
@Component
@RequiredArgsConstructor
public class BroHttpClientImpl implements BroHttpClient {

    private final RestTemplate restTemplate;

    private static final String SERVICE_INACCESSIBLE_MESSAGE = "Requested service is currently inaccessible: %s";

    @Override
    public <T, V> ResponseEntity<T> executeHttpRequest(HttpHeaders headers,
                                       String url,
                                       HttpMethod httpMethod,
                                       V requestBody,
                                       Class<T> responseDtoClass) {
        HttpEntity<V> request = new HttpEntity<>(requestBody, headers);
        try {
            return restTemplate.exchange(url, httpMethod, request, responseDtoClass);
        } catch (ResourceAccessException e) {
            log.error(e.getMessage());
            throw new BackupAndRestoreConnectionException(String.format(SERVICE_INACCESSIBLE_MESSAGE, url));
        } catch (HttpClientErrorException e) {
            throw new BackupAndRestoreHttpClientException(e.getMessage());
        }
    }
}