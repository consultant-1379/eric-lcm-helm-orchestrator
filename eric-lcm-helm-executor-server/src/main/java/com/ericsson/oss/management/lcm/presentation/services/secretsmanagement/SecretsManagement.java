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

package com.ericsson.oss.management.lcm.presentation.services.secretsmanagement;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressTLS;
import java.nio.file.Path;
import java.util.List;

/**
 * Service for updating secrets
 */
public interface SecretsManagement {

    /**
     * Fetch data from helm source
     * @param helmPath
     * @return list of ingress tls
     */
    List<IngressTLS> getIngressTLSFromHelmSource(Path helmPath);

    /**
     * Create or update data in namespace
     * @param tlsList
     * @param namespace
     * @return list of secrets
     */
    List<Secret> createSecretsWithTLS(List<IngressTLS> tlsList, String namespace);

    /**
     * Create or update data in namespace
     * @param secrets
     * @param namespace
     * @param kubeConfig for the target cluster
     */
    void createOrUpdateSecretsInNamespace(List<Secret> secrets, String namespace, Path kubeConfig);
}
