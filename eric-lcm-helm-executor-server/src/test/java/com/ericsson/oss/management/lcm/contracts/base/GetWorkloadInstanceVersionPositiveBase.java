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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceVersionDto;
import com.ericsson.oss.management.lcm.presentation.controllers.WorkloadInstancesController;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.WorkloadInstanceRequestCoordinatorServiceImpl;
import com.ericsson.oss.management.lcm.presentation.services.version.WorkloadInstanceVersionServiceImpl;
import com.ericsson.oss.management.lcm.presentation.services.workloadinstance.WorkloadInstanceServiceImpl;
import com.ericsson.oss.management.lcm.utils.UrlUtils;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("test")
@SpringBootTest(classes = WorkloadInstancesController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class GetWorkloadInstanceVersionPositiveBase {

    @Autowired
    private WorkloadInstancesController controller;
    @MockBean
    private WorkloadInstanceVersionServiceImpl workloadInstanceVersionService;
    @MockBean
    private WorkloadInstanceRequestCoordinatorServiceImpl workloadInstanceRequestCoordinatorService;
    @MockBean
    private WorkloadInstanceServiceImpl workloadInstanceService;
    @MockBean
    private UrlUtils urlUtils;

    @BeforeEach
    public void setup() {
        when(workloadInstanceVersionService.getVersionDtoByWorkloadInstanceIdAndVersion(any(), anyInt()))
                .thenReturn(fillResponse());

        RestAssuredMockMvc.standaloneSetup(controller);
    }

    private WorkloadInstanceVersionDto fillResponse() {
        WorkloadInstanceVersionDto response = new WorkloadInstanceVersionDto();
        response.setId("firstId");
        response.setVersion(1);
        response.setHelmSourceVersion("1.2.3-4");
        response.setValuesVersion("0e35ed30-d438-4b07-a82b-cab447424d30");
        return response;
    }
}
