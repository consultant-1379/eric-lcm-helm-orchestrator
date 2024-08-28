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

package com.ericsson.oss.management.lcm.contracts.base;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import com.ericsson.oss.management.lcm.api.model.ValuesRequestDto;
import com.ericsson.oss.management.lcm.presentation.controllers.ValuesController;
import com.ericsson.oss.management.lcm.presentation.services.values.InternalStoreServiceImpl;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ExtendWith(MockitoExtension.class)
public class PutUpdateValuesPositiveBase {

    private static final String EXISTING_VERSION = "existing_version";
    private static final String VALUES =
            "one:\n" +
            "                  one-one: one-one\n" +
            "                two: two\n" +
            "                three: three\n" +
            "                four: four";

    @InjectMocks
    @Autowired
    private ValuesController controller;

    @Mock
    private InternalStoreServiceImpl internalStoreService;

    @BeforeEach
    public void setup() {
        given(internalStoreService.updateValues(
                eq(EXISTING_VERSION),
                any(ValuesRequestDto.class),
                any())
        ).willReturn(VALUES.getBytes());

        RestAssuredMockMvc.standaloneSetup(controller);
    }
}
