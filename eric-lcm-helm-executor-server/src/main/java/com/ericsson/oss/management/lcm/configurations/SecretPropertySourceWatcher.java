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

package com.ericsson.oss.management.lcm.configurations;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Profile("kubernetes")
public class SecretPropertySourceWatcher {

    private final ContextRefresher refresher;
    private final KubernetesClient kubernetesClient;

    private SharedIndexInformer<Secret> informer;

    @Value("${refresher.secret-name}")
    private String secretName;
    @Value("${refresher.namespace}")
    private String namespace;

    @EventListener(ApplicationReadyEvent.class)
    private void createSecretWatcherHandler() {
        informer = kubernetesClient.secrets()
                .inNamespace(namespace)
                .withName(secretName)
                .inform(new SecretContextRefreshEventHandler(refresher));
    }

    @PreDestroy
    private void closeInformer() {
        if (Objects.nonNull(informer)) {
            informer.close();
        }
        kubernetesClient.close();
    }
}
