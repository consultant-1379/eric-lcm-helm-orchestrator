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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.api.model.ValuesRequestDto;
import com.ericsson.oss.management.lcm.presentation.controllers.ValuesController;
import com.ericsson.oss.management.lcm.presentation.exceptions.ApplicationExceptionHandler;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidInputException;
import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import com.ericsson.oss.management.lcm.presentation.services.values.InternalStoreServiceImpl;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("test")
@SpringBootTest(classes = ValuesController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PutUpdateValuesNegativeBase {

    private static final String NOT_EXISTENT_VERSION = "not_existent_version";
    private static final String EXISTING_VERSION = "existing_version";
    private static final String EXCEPTION_MESSAGE_NOT_EXISTENT_VERSION = "Values with version not_existent_version not found";
    private static final String EXCEPTION_MESSAGE_EMPTY_INPUT = "Values and additional parameters can't be empty";

    @MockBean
    private InternalStoreServiceImpl internalStoreService;

    @Autowired
    private ValuesController controller;

    @BeforeEach
    public void setup() {
        given(internalStoreService.updateValues(
                eq(NOT_EXISTENT_VERSION),
                any(ValuesRequestDto.class),
                any())
        ).willThrow(new ResourceNotFoundException(EXCEPTION_MESSAGE_NOT_EXISTENT_VERSION));

        given(internalStoreService.updateValues(
                eq(EXISTING_VERSION),
                any(ValuesRequestDto.class),
                any(MultipartFile.class))
        ).willThrow(new InvalidInputException(EXCEPTION_MESSAGE_EMPTY_INPUT));

        StandaloneMockMvcBuilder mvcBuilderWithExceptionHandler = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApplicationExceptionHandler());
        RestAssuredMockMvc.standaloneSetup(mvcBuilderWithExceptionHandler);
    }
}
