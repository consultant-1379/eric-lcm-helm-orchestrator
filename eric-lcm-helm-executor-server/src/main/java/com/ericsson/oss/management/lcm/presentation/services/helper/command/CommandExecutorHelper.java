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

package com.ericsson.oss.management.lcm.presentation.services.helper.command;

import com.ericsson.oss.management.lcm.utils.command.executor.CommandResponse;

public interface CommandExecutorHelper {

    /***
     * Execute system command
     * @param command
     * @param timeout
     * @return the result of the command execution
     */
    CommandResponse execute(String command, int timeout);

    /***
     * Execute system command with retryable
     * @param command
     * @param timeout
     * @return the result of the command execution
     */
    CommandResponse executeWithRetry(String command, int timeout);
}
