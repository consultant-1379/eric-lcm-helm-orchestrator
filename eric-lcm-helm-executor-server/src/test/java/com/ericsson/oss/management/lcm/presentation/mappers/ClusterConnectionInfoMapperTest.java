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

package com.ericsson.oss.management.lcm.presentation.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ericsson.oss.management.lcm.api.model.ClusterConnectionInfoDto;
import com.ericsson.oss.management.lcm.api.model.ConnectionInfoStatus;
import com.ericsson.oss.management.lcm.model.entity.ClusterConnectionInfo;

@SpringBootTest(classes = ClusterConnectionInfoMapperImpl.class)
class ClusterConnectionInfoMapperTest {

    private static final String CLUSTER_CONNECTION_INFO_ID = "id";
    private static final String CLUSTER_CONNECTION_INFO_NAME = "name";

    @Autowired
    private ClusterConnectionInfoMapper clusterConnectionInfoMapper;

    @Test
    void shouldMapClusterConnectionInfoToClusterConnectionInfoDto() {
        ClusterConnectionInfo clusterConnectionInfo = getClusterConnectionInfo();

        ClusterConnectionInfoDto result = clusterConnectionInfoMapper.toClusterConnectionInfoDto(clusterConnectionInfo);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(CLUSTER_CONNECTION_INFO_ID);
        assertThat(result.getName()).isEqualTo(CLUSTER_CONNECTION_INFO_NAME);
        assertThat(result.getStatus()).isEqualTo(ConnectionInfoStatus.NOT_IN_USE);
    }

    private ClusterConnectionInfo getClusterConnectionInfo() {
        return ClusterConnectionInfo.builder()
                .id(CLUSTER_CONNECTION_INFO_ID)
                .name(CLUSTER_CONNECTION_INFO_NAME)
                .content(new byte[]{})
                .status(com.ericsson.oss.management.lcm.model.entity.ConnectionInfoStatus.NOT_IN_USE)
                .build();
    }

}