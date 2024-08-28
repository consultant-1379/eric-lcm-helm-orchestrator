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

import static com.ericsson.oss.management.lcm.utils.JSONParseUtils.parseJsonToMap;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class JSONParseUtilsTest {
    private static final String VALID_JSON = "{\"key\": \"value\"}";
    private static final String INVALID_JSON = "dummy";

    @Test
    void shouldParseValidJson() {
        assertThat(parseJsonToMap(VALID_JSON)).containsEntry("key", "value");
    }

    @Test
    void shouldThrowInvalidInputExceptionInvalidJson() {
        assertThatThrownBy(() -> parseJsonToMap(INVALID_JSON))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void shouldNotParseNull() {
        assertThat(parseJsonToMap(null)).isEmpty();
    }
}