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
package com.ericsson.oss.mgmt.lcm.acceptance.tests;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.oss.mgmt.lcm.acceptance.utils.FileUtils.copyFile;
import static com.ericsson.oss.mgmt.lcm.acceptance.utils.TestExecutionGlobalConfig.SERVICE_INSTANCE;

import java.util.HashMap;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.ericsson.oss.mgmt.lcm.acceptance.models.ClusterConnectionInfoTestData;
import com.ericsson.oss.mgmt.lcm.acceptance.steps.ClusterConnectionInfo;
import com.ericsson.oss.mgmt.lcm.acceptance.steps.HealthCheck;

public class ClusterConnectionInfoLcmTest {

    private static final String CLUSTER_CONFIG_INFO_PATH = "src/main/resources/testData/clusterConnectionInfo/cluster-connection-info.config";
    private static final String CLUSTER_CONFIG_INFO_PATH_LOCAL =
            "src/main/resources/testData/clusterConnectionInfo/cluster-connection-info-local.config";

    private static String clusterConfigInfoPath;

    @BeforeSuite
    public static void setup() {

        boolean isLocal = SERVICE_INSTANCE.getIsLocal();
        clusterConfigInfoPath = isLocal ? CLUSTER_CONFIG_INFO_PATH_LOCAL : CLUSTER_CONFIG_INFO_PATH;
        if (!isLocal) {
            copyFile(SERVICE_INSTANCE.getClusterConnectionInfoPath(), clusterConfigInfoPath);
        }

    }

    @Test(description = "Upload a valid cluster connection info")
    public void uploadValidClusterConnectionInfo() {
        doUploadValidClusterConnectionInfo(clusterConfigInfoPath);
    }

    private void doUploadValidClusterConnectionInfo(String configInfoPath) {
        ResponseEntity<String> healthResponse = HealthCheck.getHealthState();
        checkHealthStatusOk(healthResponse);

        final var clusterConnectionInfoTestData = new ClusterConnectionInfoTestData();
        clusterConnectionInfoTestData.setPathToFile(configInfoPath);
        ResponseEntity<HashMap<String, String>> response = ClusterConnectionInfo.uploadValidConnectionInfo(clusterConnectionInfoTestData);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String clusterConnectionInfoId = Optional.ofNullable(response.getBody()).orElseThrow().get("id");
        ClusterConnectionInfo.removeClusterConnectionInfo(clusterConnectionInfoId);
    }

    private void checkHealthStatusOk(ResponseEntity<String> healthResponse) {
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(healthResponse.getBody()).contains("\"status\":\"UP\"");
    }
}
