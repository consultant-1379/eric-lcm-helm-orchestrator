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

package com.ericsson.oss.management.lcm.utils;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpClientUtils {

    private final RestTemplate restTemplate;

    /**
     * Execute http request according to the specified parameters
     *
     * @param headers headers of request
     * @param url address of a given unique resource
     * @param httpMethod http method of the request to execute
     * @param requestBody data sent by the client
     * @param responseDtoClass map dto in response
     * @return ResponseEntity represents the whole HTTP response: status code, headers, and body
     */
    public <T, V> ResponseEntity<T> executeHttpRequest(HttpHeaders headers, String url, HttpMethod httpMethod,
                                                       V requestBody, Class<T> responseDtoClass) {
        HttpEntity<V> request = new HttpEntity<>(requestBody, headers);
        try {
            return restTemplate.exchange(url, httpMethod, request, responseDtoClass);
        } catch (ResourceAccessException e) {
            log.error(e.getMessage());
            throw e;
        }
    }
}
