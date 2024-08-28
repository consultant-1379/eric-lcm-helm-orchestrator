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

import java.util.List;

import org.springframework.stereotype.Component;

import com.ericsson.oss.management.lcm.model.entity.Release;
import com.ericsson.oss.management.lcm.presentation.exceptions.InternalRuntimeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReleaseParser {

    private final ObjectMapper mapper;

    /**
     * Parse string to list of releases
     *
     * @param source to parse
     * @return list of releases
     */
    public List<Release> parse(String source) {
        try {
            return mapper.readValue(source, new TypeReference<>() { });
        } catch (JsonProcessingException e) {
            throw new InternalRuntimeException(
                    String.format("Error happened during parsing list of releases. Details: %s", e.getMessage()));
        }
    }

}
