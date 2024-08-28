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

import com.ericsson.oss.management.lcm.api.model.BroResponseActionDto;
import com.ericsson.oss.management.lcm.presentation.exceptions.BackupAndRestoreConnectionException;
import com.ericsson.oss.management.lcm.presentation.exceptions.BackupAndRestoreHttpClientException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = BroHttpClientImpl.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BroHttpClientImplTest {

    private static final String RESPONSE_ID = "56324";
    private static final String BRO_REQUEST_URL = "URL";
    private static final String JSON_REQUEST = "json";

    @Autowired
    private BroHttpClient routingClient;
    @MockBean
    private RestTemplate restTemplate;

    @Test
    void shouldReturnResponseWhenExecuteHttpRequest() {
        //Init
        when(restTemplate.exchange(anyString(), any(), any(), eq(BroResponseActionDto.class))).thenReturn(getResponseEntity());

        //Test method
        ResponseEntity<BroResponseActionDto> response = routingClient.executeHttpRequest(
                createHeaders(),
                BRO_REQUEST_URL,
                HttpMethod.POST,
                JSON_REQUEST,
                BroResponseActionDto.class);

        //Verify
        assertThat(response.getBody().getId()).isNotNull();
        verify(restTemplate).exchange(anyString(), any(), any(), eq(BroResponseActionDto.class));
    }

    @Test
    void shouldReturnExceptionWhenHttpClientError() {
        //Init
        when(restTemplate.exchange(anyString(), any(), any(), eq(BroResponseActionDto.class)))
                .thenThrow(HttpClientErrorException.class);

        //Test method
        Throwable throwable = catchThrowable(() -> routingClient.executeHttpRequest(
                createHeaders(),
                BRO_REQUEST_URL,
                HttpMethod.POST,
                JSON_REQUEST, BroResponseActionDto.class));

        //Verify
        verify(restTemplate).exchange(anyString(), any(), any(), eq(BroResponseActionDto.class));
        assertThat(throwable).isInstanceOf(BackupAndRestoreHttpClientException.class);
    }

    @Test
    void shouldReturnExceptionWhenResourceAccessError() {
        //Init
        when(restTemplate.exchange(anyString(), any(), any(), eq(BroResponseActionDto.class)))
                .thenThrow(ResourceAccessException.class);

        //Test method
        Throwable throwable = catchThrowable(() -> routingClient.executeHttpRequest(
                createHeaders(),
                BRO_REQUEST_URL,
                HttpMethod.POST,
                JSON_REQUEST,
                BroResponseActionDto.class));

        //Verify
        verify(restTemplate).exchange(anyString(), any(), any(), eq(BroResponseActionDto.class));
        assertThat(throwable).isInstanceOf(BackupAndRestoreConnectionException.class);
    }

    private ResponseEntity<BroResponseActionDto> getResponseEntity() {
        return new ResponseEntity<>(getActionResponse(), HttpStatus.CREATED);
    }

    private BroResponseActionDto getActionResponse() {
        BroResponseActionDto responseDto = new BroResponseActionDto();
        responseDto.setId(RESPONSE_ID);
        return responseDto;
    }

    private static HttpHeaders createHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString());
        return headers;
    }

}
