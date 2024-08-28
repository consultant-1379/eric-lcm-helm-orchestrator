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

import com.ericsson.oss.management.lcm.presentation.controllers.ValuesController;
import com.ericsson.oss.management.lcm.presentation.services.values.InternalStoreServiceImpl;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class GetValuesPositiveBase {

    private static final String VALUES_ID = "values_id";

    private static final String VALUES = "global:\n" +
            "                                  crd:\n" +
            "                                    enabled: true\n" +
            "                                    namespace: eric-crd-ns\n" +
            "                                  pullSecret: regcred-successfulpost\n" +
            "                                  registry:\n" +
            "                                    url: armdocker.rnd.ericsson.se\n" +
            "                                  app:\n" +
            "                                    namespace: dima\n" +
            "                                    enabled: true\n" +
            "                                  chart:\n" +
            "                                    registry: ''\n" +
            "                                cn-am-test-app-a:\n" +
            "                                  enabled: true\n" +
            "                                  fuu: bar\n" +
            "                                  name: cn-am-test-app-a\n" +
            "                                cn-am-test-app-b:\n" +
            "                                  enabled: true\n" +
            "                                  fuu: bar\n" +
            "                                  name: cn-am-test-app-b\n" +
            "                                cn-am-test-app-c:\n" +
            "                                  enabled: false\n" +
            "                                  fuu: bar\n" +
            "                                  name: cn-am-test-app-c\n" +
            "                                cn-am-test-crd:\n" +
            "                                  enabled: false\n" +
            "                                  fuu: bar";

    @InjectMocks
    @Autowired
    private ValuesController controller;

    @Mock
    private InternalStoreServiceImpl internalStoreService;

    @BeforeEach
    public void setup() {
        byte[] values = getValuesContent();
        given(internalStoreService.getContent(VALUES_ID)).willReturn(values);

        RestAssuredMockMvc.standaloneSetup(controller);
    }

    private byte[] getValuesContent() {
        return VALUES.getBytes();
    }
}
