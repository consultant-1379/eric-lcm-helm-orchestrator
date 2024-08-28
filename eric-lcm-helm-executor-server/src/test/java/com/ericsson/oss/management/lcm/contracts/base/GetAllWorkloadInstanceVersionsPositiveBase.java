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
import static org.mockito.Mockito.when;

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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;

import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;
import com.ericsson.oss.management.lcm.presentation.controllers.WorkloadInstancesController;
import com.ericsson.oss.management.lcm.presentation.exceptions.ApplicationExceptionHandler;
import com.ericsson.oss.management.lcm.presentation.mappers.WorkloadInstanceDtoMapper;
import com.ericsson.oss.management.lcm.presentation.mappers.WorkloadInstanceVersionDtoMapper;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.WorkloadInstanceRequestCoordinatorService;
import com.ericsson.oss.management.lcm.presentation.services.version.WorkloadInstanceVersionServiceImpl;
import com.ericsson.oss.management.lcm.presentation.services.workloadinstance.WorkloadInstanceServiceImpl;
import com.ericsson.oss.management.lcm.repositories.WorkloadInstanceRepository;
import com.ericsson.oss.management.lcm.repositories.WorkloadInstanceVersionRepository;
import com.ericsson.oss.management.lcm.utils.UrlUtils;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("test")
@SpringBootTest(classes = { WorkloadInstancesController.class, WorkloadInstanceServiceImpl.class,
        WorkloadInstanceVersionServiceImpl.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class GetAllWorkloadInstanceVersionsPositiveBase {

    private static final String FIRST_WORKLOAD_INSTANCE_VERSION_ID = "firstId";
    private static final String SECOND_WORKLOAD_INSTANCE_VERSION_ID = "secondId";
    private static final String WORKLOAD_INSTANCE_ID = "testId";
    private static final String HELMSOURCE_VERSION = "1.2.3-4";
    private static final String SECOND_HELMSOURCE_VERSION = "1.2.3-5";
    private static final String VALUES_VERSION = "0e35ed30-d438-4b07-a82b-cab447424d30";
    private static final Integer FIRST_VERSION = 1;
    private static final Integer SECOND_VERSION = 2;

    @Autowired
    private WorkloadInstancesController controller;
    @MockBean
    private WorkloadInstanceRequestCoordinatorService workloadInstanceRequestCoordinatorService;
    @MockBean
    private UrlUtils urlUtils;
    @MockBean
    private WorkloadInstanceRepository workloadInstanceRepository;
    @MockBean
    private WorkloadInstanceDtoMapper workloadInstanceDtoMapper;
    @MockBean
    private WorkloadInstanceVersionRepository workloadInstanceVersionRepository;
    @MockBean
    private WorkloadInstanceVersionDtoMapper workloadInstanceVersionMapper;

    @BeforeEach
    public void setup() {
        Pageable pageable = PageRequest.of(1, 2);
        Page<WorkloadInstanceVersion> page = new PageImpl<>(getWorkloadInstanceVersionsList(), pageable, 9L);

        when(workloadInstanceVersionRepository.findAllByWorkloadInstanceWorkloadInstanceId(any(), any(Pageable.class))).thenReturn(page);

        StandaloneMockMvcBuilder mvcBuilderWithExceptionHandler = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApplicationExceptionHandler());
        RestAssuredMockMvc.standaloneSetup(mvcBuilderWithExceptionHandler);
    }

    private List<WorkloadInstanceVersion> getWorkloadInstanceVersionsList() {
        List<WorkloadInstanceVersion> list = new ArrayList<>();
        WorkloadInstance workloadInstance = getWorkloadInstance();

        WorkloadInstanceVersion workloadInstanceVersionFirst =
                getWorkloadInstanceVersion(FIRST_WORKLOAD_INSTANCE_VERSION_ID, workloadInstance, FIRST_VERSION, HELMSOURCE_VERSION);
        WorkloadInstanceVersion workloadInstanceVersionSecond =
                getWorkloadInstanceVersion(SECOND_WORKLOAD_INSTANCE_VERSION_ID, workloadInstance, SECOND_VERSION,
                                           SECOND_HELMSOURCE_VERSION);

        list.add(workloadInstanceVersionFirst);
        list.add(workloadInstanceVersionSecond);

        return list;
    }

    private WorkloadInstanceVersion getWorkloadInstanceVersion(String id, WorkloadInstance workloadInstance,
                                                               Integer version, String helmSourceVersion) {
        return WorkloadInstanceVersion.builder()
                .id(id)
                .workloadInstance(workloadInstance)
                .version(version)
                .helmSourceVersion(helmSourceVersion)
                .valuesVersion(VALUES_VERSION)
                .build();
    }

    private WorkloadInstance getWorkloadInstance() {
        return WorkloadInstance.builder()
                .workloadInstanceId(WORKLOAD_INSTANCE_ID)
                .build();
    }
}
