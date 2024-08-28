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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@ActiveProfiles("test")
@SpringBootTest(classes = HttpClientUtils.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class HttpClientUtilsTest {

    @Autowired
    private HttpClientUtils httpClientUtils;

    @MockBean
    private RestTemplate restTemplate;

    private static final String URL = "http://test.url";
    private static final String RESPONSE = "test response";

    @Test
    void shouldSuccessfullyExecuteHttpRequest() {
        //Init
        ResponseEntity<String> responseEntity = new ResponseEntity<>(RESPONSE, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class))).thenReturn(responseEntity);

        //Test method
        ResponseEntity<String> result = httpClientUtils.executeHttpRequest(null, URL, HttpMethod.GET, null, String.class);

        //Verify
        verify(restTemplate).exchange(eq(URL), eq(HttpMethod.GET), any(), eq(String.class));
        assertThat(result).isEqualTo(responseEntity);
    }

    @Test
    void shouldReturnExceptionWhenHttpClientError() {
        //Init
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenThrow(ResourceAccessException.class);

        //Test method and verify
        assertThatThrownBy(() -> httpClientUtils.executeHttpRequest(null, URL, HttpMethod.GET,
                                                              null, String.class))
                .isInstanceOf(ResourceAccessException.class);
    }

}
