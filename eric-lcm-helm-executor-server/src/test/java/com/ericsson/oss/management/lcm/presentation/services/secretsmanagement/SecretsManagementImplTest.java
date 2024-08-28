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

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.presentation.services.kubernetes.KubernetesService;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressTLS;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SecretsManagementImplTest extends AbstractDbSetupTest {

    private static final String SECRET_NAME = "someSecretName";
    private static final String HOST_NAME = "ingress-demo";
    private static final String TLS_TYPE = "tls";
    private static final String NOT_EXIST_HOST_NAME = "some-demo";
    private static final String CRT_EXTENSION = ".crt";
    private static final String KEY_EXTENSION = ".key";
    private static final String NAMESPACE = "someNamespace";
    private static final Path HELM_PATH = Paths.get("src/test/resources/test-charts/helmfile.yaml");
    private static final Path EMPTY_INGRESS_PATH = Paths.get("src/test/resources/chart-with-empty-ingress/Chart.yaml");
    private static final Path CERTIFICATE_PATH = Paths.get("src/test/resources/test-charts/certificates");
    private static final Optional<Path> CRT_DIRECTORY = Optional.of(CERTIFICATE_PATH);
    private static final String RESULT = "someResult";

    @MockBean
    private FileService fileService;
    @MockBean
    private KubernetesService kubernetesService;
    @Autowired
    private SecretsManagementImpl secretsManagement;

    @Test
    void successfullyGetIngressTLSFromHelmSource() {
        List<IngressTLS> result = secretsManagement.getIngressTLSFromHelmSource(HELM_PATH);

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    void successfullyGetIngressTLSFromHelmSourceWhenIngressIsEmpty() {
        List<IngressTLS> result = secretsManagement.getIngressTLSFromHelmSource(EMPTY_INGRESS_PATH);

        assertThat(result.size()).isZero();
    }

    @Test
    void successfullyCreateOrUpdateSecrets() {
        when(fileService.getCertificatesDirectory()).thenReturn(CRT_DIRECTORY);
        when(fileService.getDataFromDirectoryByNameAndExtension(CERTIFICATE_PATH, HOST_NAME, CRT_EXTENSION))
                .thenReturn(RESULT);
        when(fileService.getDataFromDirectoryByNameAndExtension(CERTIFICATE_PATH, HOST_NAME, KEY_EXTENSION))
                .thenReturn(RESULT);

        List<Secret> result = secretsManagement.createSecretsWithTLS(getIngressTLSList(), NAMESPACE);

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getMetadata().getName()).isEqualTo(SECRET_NAME);
        assertThat(result.get(0).getMetadata().getNamespace()).isEqualTo(NAMESPACE);
        assertThat(result.get(0).getData().size()).isEqualTo(2);
        assertThat(result.get(0).getData().get(TLS_TYPE + CRT_EXTENSION)).isEqualTo(encodeValue());
        assertThat(result.get(0).getData().get(TLS_TYPE + KEY_EXTENSION)).isEqualTo(encodeValue());

    }

    @Test
    void shouldNotCreateOrUpdateSecretsWithoutCertAndKeyNames() {
        when(fileService.getCertificatesDirectory()).thenReturn(CRT_DIRECTORY);
        when(fileService.getDataFromDirectoryByNameAndExtension(CERTIFICATE_PATH, NOT_EXIST_HOST_NAME, CRT_EXTENSION))
                .thenReturn(RESULT);
        when(fileService.getDataFromDirectoryByNameAndExtension(CERTIFICATE_PATH, NOT_EXIST_HOST_NAME, KEY_EXTENSION))
                .thenReturn(RESULT);

        List<Secret> result = secretsManagement.createSecretsWithTLS(getIngressTLSList(), NAMESPACE);

        assertThat(result.size()).isEqualTo(0);
    }

    private List<IngressTLS> getIngressTLSList() {
        IngressTLS ingressTLS = new IngressTLS();
        ingressTLS.setHosts(List.of(HOST_NAME, SECRET_NAME));
        ingressTLS.setSecretName(SECRET_NAME);

        List<IngressTLS> ingressTLSList = new ArrayList<>();
        ingressTLSList.add(ingressTLS);

        return ingressTLSList;
    }

    private String encodeValue() {
        return Base64.getEncoder().encodeToString(RESULT.getBytes(StandardCharsets.UTF_8));
    }
}