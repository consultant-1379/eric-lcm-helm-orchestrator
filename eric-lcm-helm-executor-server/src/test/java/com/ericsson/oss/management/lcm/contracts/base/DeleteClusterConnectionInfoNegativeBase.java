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

import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

import com.ericsson.oss.management.lcm.model.entity.ClusterConnectionInfo;
import com.ericsson.oss.management.lcm.model.entity.ConnectionInfoStatus;
import com.ericsson.oss.management.lcm.presentation.controllers.ClusterConnectionInfoController;
import com.ericsson.oss.management.lcm.presentation.exceptions.ApplicationExceptionHandler;
import com.ericsson.oss.management.lcm.presentation.mappers.ClusterConnectionInfoMapper;
import com.ericsson.oss.management.lcm.presentation.services.clusterconnectioninfo.ClusterConnectionInfoServiceImpl;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.ClusterConnectionInfoRequestCoordinatorService;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.repositories.ClusterConnectionInfoRepository;

@ActiveProfiles("test")
@SpringBootTest(classes = { ClusterConnectionInfoController.class, ClusterConnectionInfoServiceImpl.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DeleteClusterConnectionInfoNegativeBase {

    @Autowired
    private ClusterConnectionInfoController controller;
    @MockBean
    private ClusterConnectionInfoRepository repository;
    @MockBean
    private ClusterConnectionInfoRequestCoordinatorService clusterConnectionInfoRequestCoordinatorService;
    @MockBean
    private ClusterConnectionInfoMapper mapper;
    @MockBean
    private FileService fileService;

    private static final String NOT_FOUND_ID = "not_found_id";
    private static final String IN_USE_ID = "in_use_id";
    private static final String CLUSTER_CONNECTION_INFO_NAME = "testName";

    @BeforeEach
    public void setup() {
        //not found scenario
        given(repository.findById(NOT_FOUND_ID)).willReturn(Optional.empty());

        //have conflict scenario
        ClusterConnectionInfo clusterConnectionInfo = getClusterConnectionInfo();
        given(repository.findById(IN_USE_ID)).willReturn(Optional.of(clusterConnectionInfo));

        StandaloneMockMvcBuilder mvcBuilderWithExceptionHandler = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApplicationExceptionHandler());
        RestAssuredMockMvc.standaloneSetup(mvcBuilderWithExceptionHandler);
    }

    private ClusterConnectionInfo getClusterConnectionInfo() {
        return ClusterConnectionInfo.builder()
                .id(IN_USE_ID)
                .name(CLUSTER_CONNECTION_INFO_NAME)
                .status(ConnectionInfoStatus.IN_USE)
                .build();
    }
}
