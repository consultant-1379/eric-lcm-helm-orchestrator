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
import io.restassured.module.mockmvc.RestAssuredMockMvc;

import com.ericsson.oss.management.lcm.api.model.ClusterConnectionInfoDto;
import com.ericsson.oss.management.lcm.api.model.ConnectionInfoStatus;
import com.ericsson.oss.management.lcm.presentation.controllers.ClusterConnectionInfoController;
import com.ericsson.oss.management.lcm.presentation.services.clusterconnectioninfo.ClusterConnectionInfoServiceImpl;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.ClusterConnectionInfoRequestCoordinatorServiceImpl;

@ActiveProfiles("test")
@SpringBootTest(classes = ClusterConnectionInfoController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PostClusterConnectionInfoPositiveBase {
    @Autowired
    private ClusterConnectionInfoController controller;
    @MockBean
    private ClusterConnectionInfoRequestCoordinatorServiceImpl service;
    @MockBean
    private ClusterConnectionInfoServiceImpl clusterConnectionInfoService;

    @BeforeEach
    public void setup() {
        given(service.create(any(), any())).willReturn(getClusterConnectionInfo());
        RestAssuredMockMvc.standaloneSetup(controller);
    }

    private ClusterConnectionInfoDto getClusterConnectionInfo() {
        ClusterConnectionInfoDto result = new ClusterConnectionInfoDto();
        result.setId("testId");
        result.setName("testName");
        result.setStatus(ConnectionInfoStatus.NOT_IN_USE);
        return result;
    }

}
