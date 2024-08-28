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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ericsson.oss.management.lcm.presentation.exceptions.InternalRuntimeException;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandExecutor;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommandExecutorHelperImpl implements CommandExecutorHelper {

    @Value("${operation.extra-time}")
    private int extraTimeForTimeout;

    private final CommandExecutor commandExecutor;

    private static final String COMMAND_EXECUTION_FAILED = "Command execution failed";

    @Override
    public CommandResponse executeWithRetry(String command, int timeout) {
        var commandResponse = new CommandResponse(COMMAND_EXECUTION_FAILED, -1);
        int timeoutWithExtraTime = timeout + extraTimeForTimeout;
        try {
            commandResponse = commandExecutor.execute(command, timeoutWithExtraTime);
            verifyCommandExecutedSuccessfully(commandResponse);
        } catch (RuntimeException e) {
            var firstOutput = commandResponse.getOutput();
            commandResponse = commandExecutor.execute(command, timeoutWithExtraTime);
            var secondOutput = commandResponse.getOutput();
            var combineOutput = getCombineOutput(firstOutput, secondOutput);
            commandResponse.setOutput(combineOutput);
        }
        return commandResponse;
    }

    @Override
    public CommandResponse execute(String command, int timeout) {
        int timeoutWithExtraTime = timeout + extraTimeForTimeout;
        return commandExecutor.execute(command, timeoutWithExtraTime);
    }

    private String getCombineOutput(String commandResponse1, String commandResponse2) {
        return new StringBuilder()
                .append("Command output 1: ")
                .append(commandResponse1)
                .append(System.lineSeparator())
                .append("Command output 2: ")
                .append(commandResponse2)
                .toString();
    }

    private void verifyCommandExecutedSuccessfully(CommandResponse commandResponse) {
        if (commandResponse.getExitCode() != 0) {
            throw new InternalRuntimeException();
        }
    }
}
