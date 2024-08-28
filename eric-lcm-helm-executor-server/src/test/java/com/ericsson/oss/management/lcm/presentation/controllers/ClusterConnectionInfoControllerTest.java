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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.management.lcm.api.model.ClusterConnectionInfoDto;
import com.ericsson.oss.management.lcm.api.model.ConnectionInfoStatus;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.ClusterConnectionInfoRequestCoordinatorService;
import com.ericsson.oss.management.lcm.presentation.services.clusterconnectioninfo.ClusterConnectionInfoServiceImpl;

@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(classes = ClusterConnectionInfoController.class)
class ClusterConnectionInfoControllerTest {

    private static final String MOCK_MULTIPART_FILE_NAME = "TempFile";
    private static final String CLUSTER_CONNECTION_INFO_ID = "testId";
    private static final String CLUSTER_CONNECTION_INFO_NAME = "testName";
    private static final String CLUSTER_CONNECTION_INFO_YAML = "clusterConnectionInfo.yaml";
    private static final String CRD_NAMESPACE = "crdNamespace";

    @Autowired
    private ClusterConnectionInfoController controller;
    @MockBean
    private ClusterConnectionInfoServiceImpl clusterConnectionInfoService;
    @MockBean
    private ClusterConnectionInfoRequestCoordinatorService clusterConnectionInfoRequestCoordinatorService;

    @Test
    void shouldReturnAcceptedWhenCreateClusterConnectionInfo() throws IOException {
        MockMultipartFile file = getFile();
        when(clusterConnectionInfoRequestCoordinatorService.create(file, CRD_NAMESPACE)).thenReturn(getClusterConnectionInfo());

        ResponseEntity<ClusterConnectionInfoDto> response = controller.clusterConnectionInfoPost(file, CRD_NAMESPACE);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(CLUSTER_CONNECTION_INFO_ID);
    }

    @Test
    void shouldReturnOkWhenGet() {
        when(clusterConnectionInfoService.get(CLUSTER_CONNECTION_INFO_ID)).thenReturn(getClusterConnectionInfo());
        ResponseEntity<ClusterConnectionInfoDto> response = controller.clusterConnectionInfoClusterConnectionInfoIdGet(CLUSTER_CONNECTION_INFO_ID);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(CLUSTER_CONNECTION_INFO_ID);
    }

    @Test
    void shouldReturnResponseWhenGetAllClusterConnectionInfo() {
        HttpStatusCode result = controller.clusterConnectionInfoGet(1, 20, Arrays.asList("workloadInstanceId,asc"))
                .getStatusCode();

        assertThat(result).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturnNoContentWhenDelete() {
        ResponseEntity<Void> response = controller.clusterConnectionInfoClusterConnectionInfoIdDelete(CLUSTER_CONNECTION_INFO_ID);

        verify(clusterConnectionInfoService).delete(CLUSTER_CONNECTION_INFO_ID);
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private MockMultipartFile getFile() throws IOException {
        File file = new ClassPathResource(CLUSTER_CONNECTION_INFO_YAML).getFile();
        return new MockMultipartFile(MOCK_MULTIPART_FILE_NAME, new FileInputStream(file));
    }

    private ClusterConnectionInfoDto getClusterConnectionInfo() {
        ClusterConnectionInfoDto result = new ClusterConnectionInfoDto();
        result.setId(CLUSTER_CONNECTION_INFO_ID);
        result.setName(CLUSTER_CONNECTION_INFO_NAME);
        result.setStatus(ConnectionInfoStatus.NOT_IN_USE);
        return result;
    }

}