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

package com.ericsson.oss.management.lcm.utils.command.executor;

/**
 * Executing system commands
 */
public interface CommandExecutor {

    /**
     * Execute system command
     * @param command to execute
     * @param timeout for the command. If command execution exceeds this timeout, execution will be terminated
     * @return the result of the command execution
     */
    CommandResponse execute(String command,  int timeout);
}
