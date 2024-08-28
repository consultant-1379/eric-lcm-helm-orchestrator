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

package com.ericsson.oss.management.lcm.presentation.services.helper.docker;

import static com.ericsson.oss.management.lcm.constants.CommandConstants.DOCKER_CREDS_SECRET_NAME;
import static com.ericsson.oss.management.lcm.constants.DockerConstants.DOCKER_SECRET_TYPE;
import static com.ericsson.oss.management.lcm.constants.DockerConstants.LABEL_APP_KEY;
import static com.ericsson.oss.management.lcm.constants.DockerConstants.METADATA_AUTH_KEY;
import static com.ericsson.oss.management.lcm.constants.DockerConstants.LABEL_APP_NAME_VALUE;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import com.ericsson.oss.management.lcm.presentation.services.kubernetes.KubernetesService;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DockerRegistrySecretHelperImpl implements DockerRegistrySecretHelper {

    @Value("${container-registry.url}")
    private String containerRegistryUrl;

    private final ContainerRegistryCredentials registryCredentials;

    private static final String AUTH_VALUE_TEMPLATE = "{\"auths\":{\"%s\":{\"username\":\"%s\"," +
            "\"password\":\"%s\",\"auth\":\"%s\"}}}";
    private static final String AUTH_TEMPLATE = "%s:%s";

    private final KubernetesService kubernetesService;

    @Override
    public void createSecret(WorkloadInstance instance, Path kubeConfigPath) {
        log.info("Start creating secret for docker registry for instance with name {}",
                instance.getWorkloadInstanceName());
        Secret secret = buildSecret(instance.getWorkloadInstanceName());
        kubernetesService.createSecretAndNamespaceIfRequired(kubeConfigPath, instance.getNamespace(), secret);
    }

    @Override
    public void deleteSecret(WorkloadInstance instance, byte[] kubeConfig) {
        log.info("Start deleting secret for docker registry for instance with name {}",
                 instance.getWorkloadInstanceName());
        String kubeConfigContent = Objects.nonNull(kubeConfig) ? new String(kubeConfig, StandardCharsets.UTF_8) : null;
        String secretName = String.format(DOCKER_CREDS_SECRET_NAME, instance.getWorkloadInstanceName());
        kubernetesService.deleteSecret(kubeConfigContent, secretName, instance.getNamespace());
    }

    private Secret buildSecret(String workloadInstanceName) {
        return new SecretBuilder()
                .withType(DOCKER_SECRET_TYPE)
                .withNewMetadata()
                .withName(String.format(DOCKER_CREDS_SECRET_NAME, workloadInstanceName))
                .addToLabels(LABEL_APP_KEY, LABEL_APP_NAME_VALUE)
                .endMetadata()
                .addToData(buildSecretData())
                .build();
    }

    private Map<String, String> buildSecretData() {
        String auth = String.format(AUTH_TEMPLATE, registryCredentials.getUsername(), registryCredentials.getPassword());
        String authResult = encodeString(auth);
        String fullAuth = String.format(AUTH_VALUE_TEMPLATE, containerRegistryUrl, registryCredentials.getUsername(),
                registryCredentials.getPassword(), authResult);
        return Collections.singletonMap(METADATA_AUTH_KEY, encodeString(fullAuth));
    }

    private String encodeString(String stringToEncode) {
        byte[] encodedAuth = Base64.getEncoder().encode(stringToEncode.getBytes(StandardCharsets.US_ASCII));
        return new String(encodedAuth, StandardCharsets.US_ASCII);
    }

}
