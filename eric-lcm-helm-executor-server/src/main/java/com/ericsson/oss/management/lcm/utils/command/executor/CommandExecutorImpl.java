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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.ericsson.oss.management.lcm.presentation.exceptions.InternalRuntimeException;
import com.ericsson.oss.management.lcm.utils.LoggingUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CommandExecutorImpl implements CommandExecutor {

    private static final boolean IS_WINDOWS = StringUtils.containsIgnoreCase(System.getProperty("os.name", "<none>"), "windows");

    @Override
    public CommandResponse execute(String command, int timeout) {
        List<String> completeCommand = new ArrayList<>();

        if (IS_WINDOWS) {
            completeCommand.add("cmd.exe");
            completeCommand.add("/c");
        } else {
            completeCommand.add("bash");
            completeCommand.add("-c");
        }
        completeCommand.add(command);

        ProcessBuilder pb = new ProcessBuilder(completeCommand);
        pb.redirectErrorStream(true);
        Process process = null;
        BufferConsumer consumer = new BufferConsumer();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            CommandResponse commandResponse = new CommandResponse();
            log.info("Executing {}", LoggingUtils.hideSensitiveData(String.join(" ", pb.command())));
            log.debug("Executing {}", LoggingUtils.hidePassword(String.join(" ", pb.command())));
            process = pb.start();
            consumer.setInputStream(process.getInputStream());
            executor.execute(consumer);

            executor.shutdown();
            final boolean commandTimedOut = executor.awaitTermination(timeout, TimeUnit.MINUTES);
            if (!commandTimedOut) {
                log.error("Command :: {} took more than : {} minutes", String.join(" ", completeCommand), timeout);
                commandResponse.setOutput(consumer.getResponse());
                commandResponse.setExitCode(-1);
                executor.shutdownNow();
                return commandResponse;
            }
            process.waitFor();
            commandResponse.setOutput(consumer.getResponse());
            commandResponse.setExitCode(process.exitValue());
            return commandResponse;
        } catch (IOException e) {
            log.error("Failed to run process due to {}", e.getMessage());
            throw new InternalRuntimeException(e);
        } catch (InterruptedException e) {
            log.error("Failed to complete process due to {}", e.getMessage());
            Thread.currentThread().interrupt();
            throw new InternalRuntimeException(e);
        } finally {
            executor.shutdownNow();
            if (process != null && process.isAlive()) {
                process.destroy();
            }
        }
    }
}
