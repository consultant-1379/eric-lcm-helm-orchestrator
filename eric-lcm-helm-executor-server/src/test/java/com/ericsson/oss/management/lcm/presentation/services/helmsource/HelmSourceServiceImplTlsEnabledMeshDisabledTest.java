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

package com.ericsson.oss.management.lcm.presentation.services.helmsource;

import com.ericsson.oss.management.lcm.presentation.services.async.executor.AsyncExecutor;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.presentation.services.helper.command.CommandExecutorHelper;
import com.ericsson.oss.management.lcm.presentation.services.operation.OperationService;
import com.ericsson.oss.management.lcm.presentation.services.release.ReleaseService;
import com.ericsson.oss.management.lcm.presentation.services.version.WorkloadInstanceVersionService;
import com.ericsson.oss.management.lcm.repositories.HelmSourceRepository;
import com.ericsson.oss.management.lcm.utils.HttpClientUtils;
import com.ericsson.oss.management.lcm.utils.UrlUtils;
import com.ericsson.oss.management.lcm.utils.command.builder.HelmSourceCommandBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = HelmSourceServiceImpl.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {"auto-rollback.enabled=true", "security.serviceMesh.enabled=true"})
public class HelmSourceServiceImplTlsEnabledMeshDisabledTest {

    private static final String AUTH_HEADER = "Basic dXNlcjpwYXNz";

    private static final String HELM_SOURCE_URL = "http://localhost:8086/helm-source-url";

    @Autowired
    private HelmSourceServiceImpl helmSourceService;
    @MockBean
    private FileService fileService;
    @MockBean
    private HelmSourceRepository helmSourceRepository;
    @MockBean
    private OperationService operationService;
    @MockBean
    private AsyncExecutor asyncExecutor;
    @MockBean
    private HelmSourceCommandBuilder commandBuilder;
    @MockBean
    private CommandExecutorHelper commandExecutorHelper;
    @MockBean
    private WorkloadInstanceVersionService workloadInstanceVersionService;
    @MockBean
    private HttpClientUtils httpClientUtils;
    @MockBean
    private UrlUtils urlUtils;
    @MockBean
    private ReleaseService releaseService;
    @Mock
    private Path helmPath;
    @Mock
    private Path valuesPath;
    @Mock
    private Path kubeConfigPath;

    @Test
    void shouldSendProperRequestWhenHelmfileDowloadByURL() {
        ResponseEntity<byte[]> helmSourceURL = new ResponseEntity<>(new byte[]{1, 2, 3}, HttpStatus.OK);
        when(httpClientUtils.executeHttpRequest(any(), any(), any(), any(), eq(byte[].class))).thenReturn(helmSourceURL);
        when(urlUtils.authenticationHeader(any(), any())).thenReturn(AUTH_HEADER);
        when(fileService.createFile(any(), any(), anyString())).thenReturn(Path.of("/test-tmp/test-chart.tgz"));

        helmSourceService
                .downloadHelmSource(HELM_SOURCE_URL, true);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, AUTH_HEADER);
        verify(httpClientUtils).executeHttpRequest(eq(headers), anyString(), eq(HttpMethod.GET), any(), any());
    }
}
