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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.SENSITIVE_DATA_REPLACEMENT;
import static com.toomuchcoding.jsonassert.JsonAssertion.assertThat;

import org.junit.jupiter.api.Test;

import com.ericsson.oss.management.lcm.presentation.exceptions.InternalRuntimeException;

public class LoggingUtilsTest {

    private static final String COMMAND_WITH_SENSITIVE_DATA = "helm pull https://test-chart-rgistry:8080/internal/charts/test-chart-1.0.0.tgz" +
            " --username admin --password pass --ca-file /tmp/cafile.pem --cert-file /tmp/certfile.pem " +
            "--key-file /tmp/keyfile.pem -d /tmp/test-folder";
    private static final String COMMAND_WITH_WRONG_DATA = "helm pull https://test-chart-rgistry:8080/internal/charts/test-chart-1.0.0.tgz" +
            " --username admin --password";
    private static final String COMMAND_WITHOUT_SENSITIVE_DATA = "some test command";
    private static final String NOT_HIDED_ARGUMENT = "--username admin";

    @Test
    public void shouldHideSensitiveData() {
        String hidedCommand = LoggingUtils.hideSensitiveData(COMMAND_WITH_SENSITIVE_DATA);
        assertThat(hidedCommand).contains(SENSITIVE_DATA_REPLACEMENT);
    }

    @Test
    public void shouldHidePassword() {
        String hidedCommand = LoggingUtils.hidePassword(COMMAND_WITH_SENSITIVE_DATA);
        assertThat(hidedCommand).contains(SENSITIVE_DATA_REPLACEMENT);
        assertThat(hidedCommand).contains(NOT_HIDED_ARGUMENT);
    }

    @Test
    public void shouldNotHideAnything() {
        String notHidedCommand = LoggingUtils.hideSensitiveData(COMMAND_WITHOUT_SENSITIVE_DATA);
        assertThat(notHidedCommand).contains(COMMAND_WITHOUT_SENSITIVE_DATA);
    }

    @Test
    public void shouldThrowInternalRuntimeExceptionWhenHideSensitiveData() {
        assertThatThrownBy(() -> LoggingUtils.hideSensitiveData(COMMAND_WITH_WRONG_DATA))
                .isInstanceOf(InternalRuntimeException.class);
    }

    @Test
    public void shouldThrowInternalRuntimeExceptionWhenHidePassword() {
        assertThatThrownBy(() -> LoggingUtils.hideSensitiveData(COMMAND_WITH_WRONG_DATA))
                .isInstanceOf(InternalRuntimeException.class);
    }
}