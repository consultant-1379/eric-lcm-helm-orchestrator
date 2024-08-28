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

package com.ericsson.oss.management.lcm.presentation.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.testcontainers.shaded.org.yaml.snakeyaml.error.YAMLException;

import com.ericsson.oss.management.lcm.api.model.ProblemDetails;

class DefaultExceptionHandlerTest {
    DefaultExceptionHandler defaultExceptionHandler = new DefaultExceptionHandler();

    @Test
    void handleAllTest() {
        String error = "This is a test";
        try {
            throw new YAMLException(error);
        } catch (Exception e) {
            ResponseEntity<ProblemDetails> objectResponseEntity = defaultExceptionHandler.handleAll(e);
            String title = "Error occurred YAMLException";
            assertThat(objectResponseEntity.getBody().getTitle()).isEqualTo(title);
            assertThat(objectResponseEntity.getBody().getDetail()).isEqualTo(error);
        }
    }

}