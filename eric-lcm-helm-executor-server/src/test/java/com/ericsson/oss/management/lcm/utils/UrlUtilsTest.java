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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(classes = { UrlUtils.class })
public class UrlUtilsTest {

    @Autowired
    private UrlUtils urlUtils;

    public static final String OPERATION_ID = "some_id";
    public static final String USER = "user";
    public static final String PASSWORD = "password";

    @Test
    void shouldReturnHttpHeadersWithOperationId() {
        //Test method
        HttpHeaders httpHeaders = urlUtils.getHttpHeaders(OPERATION_ID);

        //Verify
        assertThat(httpHeaders.getLocation()).isNotNull();
        assertThat(httpHeaders.getLocation().toString()).contains(OPERATION_ID);
    }

    @Test
    void shouldReturnEncodedAuthHeader() {
        //Init
        String expectedValue = getEncodedString(USER, PASSWORD);

        //Test method
        String authHeader = urlUtils.authenticationHeader(USER, PASSWORD);

        //Verify
        assertThat(authHeader)
                .isNotNull()
                .contains(expectedValue);
    }

    private String getEncodedString(String a, String b) {
        return Base64.getEncoder().encodeToString(String.format("%s:%s", a, b).getBytes());
    }
}
