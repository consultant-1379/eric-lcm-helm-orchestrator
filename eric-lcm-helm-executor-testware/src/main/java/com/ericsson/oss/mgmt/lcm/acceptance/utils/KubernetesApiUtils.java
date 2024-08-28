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
package com.ericsson.oss.mgmt.lcm.acceptance.utils;

import java.util.List;
import java.util.stream.Stream;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

public final class KubernetesApiUtils {
    private KubernetesApiUtils() { }

    public static List<Pod> getPodsInNamespace(KubernetesClient client, String namespace) {
        return client.pods()
                .inNamespace(namespace)
                .list()
                .getItems();
    }

    public static List<Secret> getSecretsInNamespace(KubernetesClient client, String namespace) {
        return client.secrets()
                .inNamespace(namespace)
                .list()
                .getItems();
    }

    public static boolean secretWithNameIsPresent(List<Secret> secrets, String name) {
        return secrets.stream()
                .map(Secret::getMetadata)
                .map(ObjectMeta::getName)
                .anyMatch(item -> item.equals(name));
    }

    public static Stream<String> getStatusesByPodName(List<Pod> pods, String podName) {
        return pods.stream()
                .filter(pod -> getName(pod).contains(podName))
                .map(KubernetesApiUtils::getPhase);
    }

    public static boolean namespaceExist(KubernetesClient client, String namespaceToCheck) {
        return client.namespaces()
                .withName(namespaceToCheck)
                .get() != null;
    }

    private static String getPhase(Pod pod) {
        return pod.getStatus().getPhase();
    }

    private static String getName(Pod pod) {
        return pod.getMetadata().getName();
    }

}
