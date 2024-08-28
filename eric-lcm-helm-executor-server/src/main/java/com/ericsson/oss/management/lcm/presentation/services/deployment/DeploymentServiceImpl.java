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

package com.ericsson.oss.management.lcm.presentation.services.deployment;

import java.nio.file.Path;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ericsson.oss.management.lcm.api.model.DeploymentStateInfoDTO;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceHelmSourceUrl;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.presentation.services.kubernetes.KubernetesService;

import io.fabric8.kubernetes.client.Config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeploymentServiceImpl implements DeploymentService {

    private final KubernetesService kubernetesService;
    private final FileService fileService;

    @Override
    public DeploymentStateInfoDTO getCurrentDeploymentState(WorkloadInstanceHelmSourceUrl instance, Path clusterConfigInfoPath) {
        String namespace = instance.getNamespace();
        var configPathString = fileService.readStringFromPath(clusterConfigInfoPath);
        var config = Config.fromKubeconfig(configPathString);
        var context = config.getCurrentContext().getContext();

        Map<String, String> pods = kubernetesService.getPodsStatusInfo(namespace, clusterConfigInfoPath);
        log.info("Request get deployment state finished, namespace: {}.", namespace);

        fileService.deleteFile(clusterConfigInfoPath);
        return new DeploymentStateInfoDTO()
                .workloadInstanceName(instance.getWorkloadInstanceName())
                .namespace(namespace)
                .clusterName(context.getCluster())
                .pods(pods);
    }

}
