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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import com.ericsson.oss.management.lcm.presentation.exceptions.InternalRuntimeException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BufferConsumer implements Runnable {

    private static final Pattern NON_ASCII_CHARS = Pattern.compile("[^\\p{ASCII}]");

    private InputStream inputStream;
    private String output;

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public String getResponse() {
        return output;
    }

    private void parseProcessOutput(InputStream processInput) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(processInput, StandardCharsets.UTF_8))) {
            output = br.lines().map(String::trim)
                    .collect(Collectors.joining(System.lineSeparator()));
            Matcher m = NON_ASCII_CHARS.matcher(output);
            output = m.replaceAll("");
        }
        log.debug("ProcessExecutorResponse :: {} ", output);
    }

    public void run() {
        try {
            parseProcessOutput(inputStream);
        } catch (IOException e) {
            throw new InternalRuntimeException(e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }
}
