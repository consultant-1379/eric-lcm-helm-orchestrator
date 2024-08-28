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

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Component
public class UrlUtils {

    private static final String FORWARDED_PROTO = "x-forwarded-proto";
    private static final String FORWARDED_HOST = "x-forwarded-host";
    private static final String HTTP_PROTOCOL = "http";
    private static final String HTTPS_PROTOCOL = "https";
    public static final String OPERATIONS_URL = "/cnwlcm/v1/operations/";

    /**
     * Prepare HttpHeaders
     *
     * @param operationId the id of the operation
     * @return HttpHeaders with operationId
     */
    public HttpHeaders getHttpHeaders(final String operationId) {
        HttpHeaders headers = new HttpHeaders();
        final String host = getHostUrl();
        headers.add(HttpHeaders.LOCATION, host + OPERATIONS_URL + operationId);
        return headers;
    }

    /**
     * Prepare data for authentication header
     *
     * @param user
     * @param password
     * @return data with encoded user and password
     */
    public String authenticationHeader(String user, String password) {
        var auth = String.format("%s:%s", user, password);
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.US_ASCII));
        return "Basic " + new String(encodedAuth, StandardCharsets.US_ASCII);
    }

    private HttpServletRequest getCurrentHttpRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    }

    private String getHostUrl() {
        StringBuilder resolvedUrl = new StringBuilder();
        HttpServletRequest request = getCurrentHttpRequest();
        String protocol = StringUtils.containsIgnoreCase(request.getHeader(FORWARDED_PROTO), HTTPS_PROTOCOL) ? HTTPS_PROTOCOL : HTTP_PROTOCOL;
        String host = request.getHeader(FORWARDED_HOST) != null ? request.getHeader(FORWARDED_HOST) : request.getRemoteHost();
        resolvedUrl.append(protocol).append("://").append(host);
        log.info("Resolved URL is {} ", resolvedUrl);
        return resolvedUrl.toString();
    }
}
