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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * Send REST calls
 */
public interface BroHttpClient {
    /**
     * Executes any REST call.
     * @param headers http headers
     * @param url Url which will be called
     * @param httpMethod Http method for request
     * @param <V> request body, can be any object (dto)
     * @param <T> response type.
     * @return ResponseEntity<T>
     */
    <T, V> ResponseEntity<T> executeHttpRequest(HttpHeaders headers, String url, HttpMethod httpMethod, V requestBody, Class<T> responseDtoClass);
}