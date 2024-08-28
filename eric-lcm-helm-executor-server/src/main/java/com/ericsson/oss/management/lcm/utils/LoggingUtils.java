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

import static com.ericsson.oss.management.lcm.constants.CommandConstants.CA_FILE_ARGUMENT;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.CERT_FILE_ARGUMENT;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.KEY_FILE_ARGUMENT;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.PASSWORD_ARGUMENT;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.SENSITIVE_DATA_REPLACEMENT;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.USERNAME_ARGUMENT;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.SPACE;

import java.util.Arrays;
import java.util.List;

import com.ericsson.oss.management.lcm.presentation.exceptions.InternalRuntimeException;

public final class LoggingUtils {

    private LoggingUtils() {
    }

    /**
     * Hide sensitive data witch present in command
     *
     * @param command data which needs to be hided
     * @return command with hided sensitive data into string
     */
    public static String hideSensitiveData(String command) {
        List<String> commandParams = Arrays.asList(command.split(SPACE));
        try {
            for (int i = 0; i < commandParams.size(); i++) {
                if (containsSensitiveData(commandParams.get(i))) {
                    commandParams.set(i + 1, SENSITIVE_DATA_REPLACEMENT);
                }
            }
            return String.join(SPACE, commandParams);
        } catch (IndexOutOfBoundsException e) {
            throw new InternalRuntimeException(String.format("Failed to hide sensitive data in command due to %s", e.getMessage()));
        }
    };

    /**
     * Hide password witch present in command
     *
     * @param command data which needs to be hided
     * @return command with hided password into string
     */
    public static String hidePassword(String command) {
        List<String> commandParams = Arrays.asList(command.split(SPACE));
        try {
            for (int i = 0; i < commandParams.size(); i++) {
                if (commandParams.get(i).equals(PASSWORD_ARGUMENT)) {
                    commandParams.set(i + 1, SENSITIVE_DATA_REPLACEMENT);
                }
            }
            return String.join(SPACE, commandParams);
        } catch (IndexOutOfBoundsException e) {
            throw new InternalRuntimeException(String.format("Failed to hide password in command due to %s", e.getMessage()));
        }
    };

    private static boolean containsSensitiveData(String commandParam) {
        return Arrays.asList(PASSWORD_ARGUMENT, USERNAME_ARGUMENT, CA_FILE_ARGUMENT, CERT_FILE_ARGUMENT, KEY_FILE_ARGUMENT)
                .contains(commandParam);
    }
}
