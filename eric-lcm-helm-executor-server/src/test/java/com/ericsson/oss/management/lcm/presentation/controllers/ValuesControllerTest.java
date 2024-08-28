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

package com.ericsson.oss.management.lcm.presentation.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.annotation.DirtiesContext;

import com.ericsson.oss.management.lcm.api.model.ValuesRequestDto;
import com.ericsson.oss.management.lcm.presentation.services.values.InternalStoreServiceImpl;

@SpringBootTest(classes = ValuesController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ValuesControllerTest {

    @Autowired
    private ValuesController controller;

    @MockBean
    private InternalStoreServiceImpl internalStoreService;

    private static final String VALUES = "some values";

    @Test
    void shouldReturnResponseWhenGetValues() {
        //Init
        byte[] values = getValuesContent();
        when(internalStoreService.getContent(anyString())).thenReturn(values);

        //Test method
        HttpStatusCode result = controller.valuesVersionGet("existing_id")
                .getStatusCode();

        //Verify
        assertThat(result).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturnAcceptedWhenUpdateValuesVersion() throws Exception {
        //Init
        when(internalStoreService.updateValues(anyString(), any(ValuesRequestDto.class), any()))
                .thenReturn(getValuesContent());

        //Test method
        HttpStatusCode statusCode = controller.valuesVersionPut("existing_version", new ValuesRequestDto(), null)
                .getStatusCode();

        //Verify
        assertThat(statusCode).isEqualTo(HttpStatus.ACCEPTED);
    }

    private byte[] getValuesContent() {
        return VALUES.getBytes();
    }

}
