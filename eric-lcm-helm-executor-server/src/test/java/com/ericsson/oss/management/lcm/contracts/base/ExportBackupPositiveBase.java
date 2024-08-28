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

package com.ericsson.oss.management.lcm.contracts.base;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.management.lcm.api.model.BroResponseActionDto;
import com.ericsson.oss.management.lcm.presentation.controllers.BackupAndRestoreController;
import com.ericsson.oss.management.lcm.presentation.services.BackupAndRestoreService;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("test")
@SpringBootTest(classes = BackupAndRestoreController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ExportBackupPositiveBase {

    private static final String RESPONSE_ID = "123456";

    @Autowired
    private BackupAndRestoreController controller;
    @MockBean
    private BackupAndRestoreService service;

    @BeforeEach
    public void setup() {
        given(service.exportBackup(any())).willReturn(getResponse());
        RestAssuredMockMvc.standaloneSetup(controller);
    }

    private BroResponseActionDto getResponse() {
        BroResponseActionDto response = new BroResponseActionDto();
        response.setId(RESPONSE_ID);
        return response;
    }
}
