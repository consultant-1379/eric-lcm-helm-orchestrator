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

import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidInputException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public final class JSONParseUtils {

    private JSONParseUtils(){}

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Parse string in json format to map
     *
     * @param source data which needs to be parsed
     * @return parsed source into map
     */
    public static Map<String, Object> parseJsonToMap(String source) {
        return Optional.ofNullable(source)
                .map(s -> {
                    try {
                        TypeReference<Map<String, Object>> typeRef
                                = new TypeReference<>() { };
                        return OBJECT_MAPPER.readValue(s, typeRef);
                    } catch (JsonProcessingException e) {
                        log.error("Error parsing JSON due to {}", e.getMessage());
                        throw new InvalidInputException(e);
                    }
                }).orElse(new HashMap<>());
    }

    /**
     * Parse map to string in json format
     *
     * @param source data which needs to be parsed
     * @return parsed source into json string
     */
    public static String parseMapToJson(Map<String, Object> source) {
        try {
            return OBJECT_MAPPER.writeValueAsString(source);
        } catch (JsonProcessingException e) {
            throw new InvalidInputException(String.format("Failed to parse map to string due to %s", e.getMessage()), e);
        }
    }

}
