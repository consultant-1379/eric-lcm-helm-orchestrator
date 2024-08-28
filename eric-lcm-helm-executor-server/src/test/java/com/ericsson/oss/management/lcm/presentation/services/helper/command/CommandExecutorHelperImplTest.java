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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.oss.management.lcm.presentation.exceptions.InternalRuntimeException;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandExecutor;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandResponse;

@SpringBootTest(classes = { CommandExecutorHelperImpl.class })
class CommandExecutorHelperImplTest {

    @MockBean
    private CommandExecutor commandExecutor;

    @Autowired
    private CommandExecutorHelperImpl commandExecutorHelper;

    private static final String COMMAND = "echo 'test'; sleep 5";

    private static final String ERROR = "ERROR";

    @Test
    void shouldNotRetryExecutorWhenExitCodeIsZero() {
        CommandResponse response = new CommandResponse();
        response.setExitCode(0);
        when(commandExecutor.execute(anyString(), anyInt())).thenReturn(response);
        commandExecutorHelper.executeWithRetry(COMMAND, 5);
        verify(commandExecutor, times(1)).execute(anyString(), anyInt());
    }

    @Test
    void shouldRetryExecutorWhenExitCodeNotEqualToZero() {
        CommandResponse response = new CommandResponse();
        response.setExitCode(1);
        when(commandExecutor.execute(anyString(), anyInt())).thenReturn(response);
        commandExecutorHelper.executeWithRetry(COMMAND, 5);
        verify(commandExecutor, times(2)).execute(anyString(), anyInt());
    }

    @Test
    void shouldRetryExecutorWhenThrowInternalRuntimeException() {
        CommandResponse response = new CommandResponse();
        response.setExitCode(1);
        response.setOutput(ERROR);
        when(commandExecutor.execute(anyString(), anyInt())).thenReturn(response);
        commandExecutorHelper.executeWithRetry(COMMAND, 5);
        when(commandExecutor.execute(anyString(), anyInt())).thenThrow(new InternalRuntimeException(""));
        verify(commandExecutor, times(2)).execute(anyString(), anyInt());
    }

    @Test
    void shouldExecuteWithoutRetry() {
        CommandResponse response = new CommandResponse();
        response.setExitCode(0);
        when(commandExecutor.execute(anyString(), anyInt())).thenReturn(response);
        commandExecutorHelper.execute(COMMAND, 5);
        verify(commandExecutor, times(1)).execute(anyString(), anyInt());
    }
}