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

package com.ericsson.oss.mgmt.lcm.acceptance.utils;

import io.qameta.allure.internal.shadowed.jackson.core.JsonProcessingException;
import io.qameta.allure.internal.shadowed.jackson.databind.DeserializationFeature;
import io.qameta.allure.internal.shadowed.jackson.databind.ObjectMapper;
import io.qameta.allure.internal.shadowed.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;

public final class Parser {
    private Parser() { }

    public static <T> T parse(String source, Class<T> targetClass) {
        try {
            var mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper.readValue(source, targetClass);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    String.format("Error happened during the parsing source json to %s. Details: %s", targetClass.getName(), e.getMessage()));
        }
    }

    public static void writeToFile(File file, Object source) {
        var mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, source);
        } catch (IOException e) {
            throw new IllegalStateException(
                    String.format("Error happened during the saving json to file. Details: %s", e.getMessage()));
        }
    }

}
