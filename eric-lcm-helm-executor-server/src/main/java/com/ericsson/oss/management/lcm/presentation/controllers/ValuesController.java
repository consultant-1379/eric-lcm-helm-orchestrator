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

package com.ericsson.oss.management.lcm.presentation.controllers;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.api.ValuesApi;
import com.ericsson.oss.management.lcm.api.model.ValuesRequestDto;
import com.ericsson.oss.management.lcm.presentation.services.values.ValuesService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/cnwlcm/v1")
@RequiredArgsConstructor
public class ValuesController implements ValuesApi {

    private final ValuesService valuesService;

    @Override
    public ResponseEntity<byte[]> valuesVersionGet(String version) {
        byte[] content = valuesService.getContent(version);
        return new ResponseEntity<>(content, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<byte[]> valuesVersionPut(String version, @Valid ValuesRequestDto valuesRequestDto, MultipartFile values) {
        byte[] result = valuesService.updateValues(version, valuesRequestDto, values);
        return new ResponseEntity<>(result, HttpStatus.ACCEPTED);
    }


}
