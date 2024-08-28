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

package com.ericsson.oss.management.lcm.e2e;
import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.model.entity.Values;
import com.ericsson.oss.management.lcm.repositories.ValuesRepository;
import com.ericsson.oss.management.lcm.utils.TestingFileUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
class ValuesControllerTest extends AbstractDbSetupTest {

    private static final String VALUES_ID = "some_id";
    private static final String VALUES_NAME = "some_name";
    private static final String VALUES_YAML = "values.yaml";
    private static final String VALUES_COMPOSE_YAML = "values-compose.yaml";
    private static final String VALUES_REQUEST_DTO = "valuesRequestDto";
    private static final String VALUES_REQUEST_DTO_JSON = "workloadInstancePut/workloadInstancePut.json";
    private static final String ADDITIONAL_PARAMETERS = "testKey: testValue";
    private static final String VALUES = "values";

    private static String jsonRequest;
    private static String valuesFile;
    private static String newValuesFile;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private ValuesRepository valuesRepository;

    @BeforeEach
    public void filesSetup() throws Exception {
        valuesFile = TestingFileUtils.readDataFromFile(VALUES_YAML);
        newValuesFile = TestingFileUtils.readDataFromFile(VALUES_COMPOSE_YAML);
        jsonRequest = TestingFileUtils.readDataFromFile(VALUES_REQUEST_DTO_JSON);
    }

    @AfterEach
    public void cleanDb() {
        valuesRepository.deleteAll();
    }

    @Test
    void shouldAcceptGetByIdRequest() throws Exception {

        //init
        Values values = valuesRepository.save(getValues());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                        .multipart(String.format("/cnwlcm/v1/values/%s", values.getId()))
                        .with(request -> {
                            request.setMethod("GET");
                            return request;
                        }))
                .andReturn();

        //Verify
        assertThatResponseCodeIs(result, OK);

        String response = result
                .getResponse()
                .getContentAsString();
        assertThat(response).contains(valuesFile);
    }

    @Test
    void shouldNotFoundWhenGetByIdWithIncorrectIdRequest() throws Exception {

        //init
        valuesRepository.save(getValues());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                        .multipart(String.format("/cnwlcm/v1/values/%s", VALUES_ID))
                        .with(request -> {
                            request.setMethod("GET");
                            return request;
                        }))
                .andReturn();
        //Verify
        assertThatResponseCodeIs(result, NOT_FOUND);
    }

    @Test
    void shouldAcceptPutRequestWithAllParts() throws Exception {

        //init
        Values values = valuesRepository.save(getValues());

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(VALUES_REQUEST_DTO,
                                                           VALUES_REQUEST_DTO_JSON, MediaType.APPLICATION_JSON_VALUE, jsonRequest.getBytes());

        // values part
        MockMultipartFile valuesPart = new MockMultipartFile(VALUES, VALUES_COMPOSE_YAML, MediaType.TEXT_PLAIN_VALUE,
                                                             newValuesFile.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart(String.format("/cnwlcm/v1/values/%s", values.getId()))
                                 .file(jsonPart)
                                 .file(valuesPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA)
                                 .with(request -> {
                                     request.setMethod("PUT");
                                     return request;
                                 }))
                .andReturn();

        //Verify
        assertThatResponseCodeIs(result, ACCEPTED);

        String response = result
                .getResponse()
                .getContentAsString();
        assertThat(response).contains(ADDITIONAL_PARAMETERS);
    }

    @Test
    void shouldReturnNotFoundResponseWhenWrongValuesId() throws Exception {

        //init
        Values values = valuesRepository.save(getValues());

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(VALUES_REQUEST_DTO,
                                                           VALUES_REQUEST_DTO_JSON, MediaType.APPLICATION_JSON_VALUE, jsonRequest.getBytes());

        // values part
        MockMultipartFile valuesPart = new MockMultipartFile(VALUES, VALUES_COMPOSE_YAML, MediaType.TEXT_PLAIN_VALUE,
                                                             newValuesFile.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart(String.format("/cnwlcm/v1/values/%s", VALUES_ID))
                                 .file(jsonPart)
                                 .file(valuesPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA)
                                 .with(request -> {
                                     request.setMethod("PUT");
                                     return request;
                                 }))
                .andReturn();

        //Verify
        assertThatResponseCodeIs(result, NOT_FOUND);
    }

    @Test
    void shouldAcceptPutRequestWithoutJson() throws Exception {

        //init
        Values values = valuesRepository.save(getValues());

        // values part
        MockMultipartFile valuesPart = new MockMultipartFile(VALUES, VALUES_COMPOSE_YAML, MediaType.TEXT_PLAIN_VALUE,
                                                             newValuesFile.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart(String.format("/cnwlcm/v1/values/%s", values.getId()))
                                 .file(valuesPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA)
                                 .with(request -> {
                                     request.setMethod("PUT");
                                     return request;
                                 }))
                .andReturn();

        //Verify
        assertThatResponseCodeIs(result, ACCEPTED);

        String response = result
                .getResponse()
                .getContentAsString();
        assertThat(response).contains(newValuesFile);
    }

    @Test
    void shouldAcceptPutWithoutValues() throws Exception {

        //init
        Values values = valuesRepository.save(getValues());

        //json part
        MockMultipartFile jsonPart = new MockMultipartFile(VALUES_REQUEST_DTO,
                                                           VALUES_REQUEST_DTO_JSON, MediaType.APPLICATION_JSON_VALUE, jsonRequest.getBytes());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart(String.format("/cnwlcm/v1/values/%s", values.getId()))
                                 .file(jsonPart)
                                 .contentType(MediaType.MULTIPART_FORM_DATA)
                                 .with(request -> {
                                     request.setMethod("PUT");
                                     return request;
                                 }))
                .andReturn();

        //Verify
        assertThatResponseCodeIs(result, ACCEPTED);

        String response = result
                .getResponse()
                .getContentAsString();
        assertThat(response).isNotEqualTo(valuesFile);
        assertThat(response).contains(ADDITIONAL_PARAMETERS);
    }

    @Test
    void shouldReturnBadRequestPutWithoutJsonAndValues() throws Exception {

        //init
        Values values = valuesRepository.save(getValues());

        // build and execute request
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                                 .multipart(String.format("/cnwlcm/v1/values/%s", values.getId()))
                                 .contentType(MediaType.MULTIPART_FORM_DATA)
                                 .with(request -> {
                                     request.setMethod("PUT");
                                     return request;
                                 }))
                .andReturn();

        //Verify
        assertThatResponseCodeIs(result, BAD_REQUEST);
    }

    private Values getValues() {
        return Values.builder()
                .name(VALUES_NAME)
                .content(valuesFile.getBytes())
                .build();
    }

    private void assertThatResponseCodeIs(MvcResult result, HttpStatus httpStatus) {
        assertThat(result).isNotNull();
        assertThat(result.getResponse()).isNotNull();
        assertThat(result
                .getResponse()
                .getStatus()).isEqualTo(httpStatus.value());
    }
}
