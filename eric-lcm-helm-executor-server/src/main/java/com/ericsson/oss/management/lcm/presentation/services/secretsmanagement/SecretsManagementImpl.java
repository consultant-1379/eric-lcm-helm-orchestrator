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

import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.OUTPUT_DIR;

import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.presentation.services.kubernetes.KubernetesService;
import com.ericsson.oss.management.lcm.utils.FileUtils;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressTLS;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Objects;
import java.util.Base64;
import java.util.stream.Collectors;


@Slf4j
@Service
@AllArgsConstructor
public class SecretsManagementImpl implements SecretsManagement {

    private static final String CERTIFICATE_EXTENSION = ".crt";
    private static final String KEY_EXTENSION = ".key";
    private static final String SECRET_TYPE = "kubernetes.io/tls";
    private static final String TLS_NAME = "tls";

    private final FileService fileService;
    private final KubernetesService kubernetesService;

    @Override
    public List<IngressTLS> getIngressTLSFromHelmSource(Path helmPath) {
        log.info("Get list of ingressTls from helm source");
        var output = Paths.get(helmPath.getParent() + OUTPUT_DIR);
        return FileUtils.getTemplatesDirectories(output)
                .stream()
                .map(this::getIngressTls)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public List<Secret> createSecretsWithTLS(List<IngressTLS> tlsIngresses, String namespace) {
        log.info("Get list of secrets with tls certificates");
        List<Secret> secrets = new ArrayList<>();
        Optional<Path> certificatePath = fileService.getCertificatesDirectory();

        if (certificatePath.isPresent() && tlsIngresses != null) {
            for (IngressTLS ingressTLS : tlsIngresses) {
                ingressTLS.getHosts()
                        .stream()
                        .map(host -> createSecret(certificatePath.get(), host, namespace, ingressTLS.getSecretName()))
                        .forEach(secret -> secret.ifPresent(secrets::add));
            }
        }
        return secrets;
    }

    @Override
    public void createOrUpdateSecretsInNamespace(List<Secret> secrets, String namespace, Path kubeKonfig) {
        log.info("Create or update secrets in namespace {}", namespace);
        if (secrets != null && !secrets.isEmpty()) {
            for (Secret secret : secrets) {
                kubernetesService.createOrReplaceSecretInNamespace(namespace, secret, kubeKonfig);
            }
        }
    }

    private Optional<Secret> createSecret(Path certificatePath, String hostName, String namespace, String secretName) {
        Map<String, String> certificatesAndKeys = new HashMap<>();
        String certificate = fileService.getDataFromDirectoryByNameAndExtension(certificatePath, hostName, CERTIFICATE_EXTENSION);
        String key = fileService.getDataFromDirectoryByNameAndExtension(certificatePath, hostName, KEY_EXTENSION);
        if (Objects.nonNull(certificate) && Objects.nonNull(key)) {
            certificatesAndKeys.put(TLS_NAME + CERTIFICATE_EXTENSION, Base64.getEncoder().encodeToString(certificate.getBytes()));
            certificatesAndKeys.put(TLS_NAME + KEY_EXTENSION, Base64.getEncoder().encodeToString(key.getBytes()));
            return buildSecret(certificatesAndKeys, secretName, namespace);
        }
        return Optional.empty();
    }

    private List<IngressTLS> getIngressTls(Path directory) {
        return FileUtils.getAllIngressFromDirectory(directory.toAbsolutePath())
                .stream()
                .filter(Objects::nonNull)
                .map(Ingress::getSpec)
                .flatMap(spec -> spec.getTls().stream())
                .collect(Collectors.toList());
    }

    private Optional<Secret> buildSecret(Map<String, String> certificatesAndKeys, String secretName, String namespace) {
        log.info("log audit: While receiving a Post request (INSTANTIATE) inject certificate and private key into" +
                        " secret with name {} ", secretName);
        return Optional.ofNullable(new SecretBuilder()
                .withData(certificatesAndKeys)
                .withType(SECRET_TYPE)
                .withNewMetadata()
                .withName(secretName)
                .withNamespace(namespace)
                .endMetadata().build());
    }
}
