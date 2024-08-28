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

import java.util.ArrayList;
import java.util.List;

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

import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.presentation.controllers.WorkloadInstancesController;
import com.ericsson.oss.management.lcm.presentation.mappers.WorkloadInstanceDtoMapper;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.WorkloadInstanceRequestCoordinatorService;
import com.ericsson.oss.management.lcm.presentation.services.version.WorkloadInstanceVersionService;
import com.ericsson.oss.management.lcm.presentation.services.workloadinstance.WorkloadInstanceServiceImpl;
import com.ericsson.oss.management.lcm.repositories.WorkloadInstanceRepository;
import com.ericsson.oss.management.lcm.utils.UrlUtils;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("test")
@SpringBootTest(classes = { WorkloadInstancesController.class, WorkloadInstanceServiceImpl.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class GetAllWorkloadInstancesPositiveBase {

    private static final String WORKLOAD_INSTANCE_ID = "firstId";
    private static final String SECOND_WORKLOAD_INSTANCE_ID = "secondId2";
    private static final String WORKLOAD_INSTANCE_NAME = "name";
    private static final String NAMESPACE = "namespace";
    private static final String CLUSTER = "cluster";
    private static final String ADDITIONAL_PARAMETERS = "{\"testKey\": \"testValue\"}";

    @Autowired
    private WorkloadInstancesController controller;
    @MockBean
    private WorkloadInstanceRepository workloadInstanceRepository;
    @MockBean
    private WorkloadInstanceVersionService workloadInstanceVersionService;
    @MockBean
    private WorkloadInstanceRequestCoordinatorService workloadInstanceRequestCoordinatorService;
    @MockBean
    private UrlUtils urlUtils;
    @MockBean
    private WorkloadInstanceDtoMapper workloadInstanceDtoMapper;

    @BeforeEach
    public void setup() {
        Pageable pageable = PageRequest.of(1, 2);
        Page<WorkloadInstance> page = new PageImpl<>(getWorkloadInstancesList(), pageable, 9L);

        given(workloadInstanceRepository.findAll(any(Pageable.class))).willReturn(page);

        RestAssuredMockMvc.standaloneSetup(controller);
    }

    private List<WorkloadInstance> getWorkloadInstancesList() {
        List<WorkloadInstance> list = new ArrayList<>();

        WorkloadInstance workloadInstanceFirst = getWorkloadInstance(WORKLOAD_INSTANCE_ID);
        WorkloadInstance workloadInstanceSecond = getWorkloadInstance(SECOND_WORKLOAD_INSTANCE_ID);

        list.add(workloadInstanceFirst);
        list.add(workloadInstanceSecond);

        return list;
    }

    private WorkloadInstance getWorkloadInstance(String id) {
        return WorkloadInstance.builder()
                .workloadInstanceId(id)
                .workloadInstanceName(WORKLOAD_INSTANCE_NAME)
                .namespace(NAMESPACE)
                .cluster(CLUSTER)
                .additionalParameters(ADDITIONAL_PARAMETERS)
                .build();
    }
}
