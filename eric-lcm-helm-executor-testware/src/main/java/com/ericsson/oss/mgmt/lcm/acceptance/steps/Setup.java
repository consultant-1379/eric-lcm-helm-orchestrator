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

package com.ericsson.oss.mgmt.lcm.acceptance.steps;

import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.HELM_PACKAGE_COMMAND;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.Constants.TAR_COMMAND;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.ericsson.oss.mgmt.lcm.acceptance.models.WorkloadInstanceTestData;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Setup {
    private Setup() {
    }

    private static final boolean IS_WINDOWS = StringUtils
            .containsIgnoreCase(System.getProperty("os.name", "<none>"), "windows");
    private static final int TAR_TIMEOUT = 2;
    private static final int HELM_PACKAGE_TIMEOUT = 4;

    @Step("Create helmfile archive from folder contents")
    public static void zipUpHelmfile(final WorkloadInstanceTestData instance) {
        log.info("Creating helmfile.tgz");
        var command = String.format(TAR_COMMAND, instance.getHelmSourceLocation(), instance.getHelmSourceName());
        execute(command, TAR_TIMEOUT);
    }

    @Step("Create helm chart archive from folder contents")
    public static void zipUpIntegrationChart(WorkloadInstanceTestData instance, String archiveContentPath) {
        log.info("Creating integration_chart.tgz");
        var command = String.format(HELM_PACKAGE_COMMAND, instance.getHelmSourceLocation(), archiveContentPath);
        execute(command, HELM_PACKAGE_TIMEOUT);
    }

    public static String execute(String command, int timeout) {
        List<String> completeCommand = new ArrayList<>();

        if (IS_WINDOWS) {
            completeCommand.add("cmd.exe");
            completeCommand.add("/c");
        } else {
            completeCommand.add("bash");
            completeCommand.add("-c");
        }
        completeCommand.add(command);

        var pb = new ProcessBuilder(completeCommand);
        pb.redirectErrorStream(true);
        Process process;

        try {
            log.info("Executing {}", String.join(" ", pb.command()));
            process = pb.start();

            final boolean commandCompletedSuccessfully = process.waitFor(timeout, TimeUnit.MINUTES);
            if (!commandCompletedSuccessfully) {
                throw new IllegalStateException("Failed to execute command, output: " + getOutput(process));
            }
            String output = getOutput(process);
            log.info("Execute output: " + output);
            return output;
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to execute command due to " + e.getMessage());
        }
    }

    private static String getOutput(Process process) throws IOException {
        try (var br = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            return br
                    .lines()
                    .map(String::trim)
                    .collect(Collectors.joining(System.lineSeparator()));
        }
    }
}