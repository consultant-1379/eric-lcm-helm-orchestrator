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

import com.ericsson.oss.management.lcm.model.entity.ClusterConnectionInfo;
import com.ericsson.oss.management.lcm.model.entity.ConnectionInfoStatus;
import com.ericsson.oss.management.lcm.presentation.controllers.ClusterConnectionInfoController;
import com.ericsson.oss.management.lcm.presentation.mappers.ClusterConnectionInfoMapper;
import com.ericsson.oss.management.lcm.presentation.services.clusterconnectioninfo.ClusterConnectionInfoServiceImpl;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.ClusterConnectionInfoRequestCoordinatorService;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.repositories.ClusterConnectionInfoRepository;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ActiveProfiles("test")
@SpringBootTest(classes = {ClusterConnectionInfoController.class, ClusterConnectionInfoServiceImpl.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class GetAllClusterConnectionInfoPositiveBase {

    @Autowired
    private ClusterConnectionInfoController controller;
    @MockBean
    private ClusterConnectionInfoRepository clusterConnectionInfoRepository;
    @MockBean
    private ClusterConnectionInfoRequestCoordinatorService clusterConnectionInfoRequestCoordinatorService;
    @MockBean
    private ClusterConnectionInfoMapper mapper;
    @MockBean
    private FileService fileService;

    private static final String CLUSTER_CONNECTION_INFO_ID = "firstId";
    private static final String SECOND_CLUSTER_CONNECTION_INFO_ID = "secondId";
    private static final String CLUSTER_CONNECTION_INFO_NAME = "testName";
    private static final String SECOND_CLUSTER_CONNECTION_INFO_NAME = "secondTestName";


    @BeforeEach
    public void setup() {
        Pageable pageable = PageRequest.of(1, 2);
        Page<ClusterConnectionInfo> page = new PageImpl<>(getClusterConnectionInfo(), pageable, 9L);

        given(clusterConnectionInfoRepository.findAll(any(Pageable.class))).willReturn(page);

        RestAssuredMockMvc.standaloneSetup(controller);
    }

    private List<ClusterConnectionInfo> getClusterConnectionInfo() {
        List<ClusterConnectionInfo> list = new ArrayList<>();

        ClusterConnectionInfo clusterConnectionInfoFirst = getClusterConnectionInfo(CLUSTER_CONNECTION_INFO_ID, CLUSTER_CONNECTION_INFO_NAME);
        ClusterConnectionInfo clusterConnectionInfoSecond = getClusterConnectionInfo(SECOND_CLUSTER_CONNECTION_INFO_ID,
                                                                               SECOND_CLUSTER_CONNECTION_INFO_NAME);

        list.add(clusterConnectionInfoFirst);
        list.add(clusterConnectionInfoSecond);

        return list;
    }

    private ClusterConnectionInfo getClusterConnectionInfo(String id, String name) {
        ClusterConnectionInfo result = new ClusterConnectionInfo();
        result.setId(id);
        result.setName(name);
        result.setStatus(ConnectionInfoStatus.NOT_IN_USE);
        return result;
    }
}
