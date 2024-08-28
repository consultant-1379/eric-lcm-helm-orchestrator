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

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.Operation;
import com.ericsson.oss.management.lcm.model.entity.OperationState;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;
import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import com.ericsson.oss.management.lcm.presentation.services.async.executor.AsyncExecutor;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.presentation.services.helper.command.CommandExecutorHelper;
import com.ericsson.oss.management.lcm.presentation.services.operation.OperationService;
import com.ericsson.oss.management.lcm.utils.command.builder.HelmSourceCommandBuilder;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Path;
import java.time.LocalDateTime;

import static com.ericsson.oss.management.lcm.model.entity.HelmSourceType.INTEGRATION_CHART;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {"auto-rollback.enabled=false", "security.serviceMesh.enabled=false",
        "helmrepo.ca-cert=/tmp/cacert.pem", "helmrepo.client-cert=/tmp/client.pem",
        "helmrepo.client-key=/tmp/clientkey.pem", "security.tls.enabled=true",
        "helmrepo.username=admin", "helmrepo.password=pass"})
class HelmSourceServiceDisabledAutoRollbackAndEnabledTlsTest extends AbstractDbSetupTest {

    private static final String HELMSOURCE_ID = "some_id";
    private static final String OPERATION_ID = "some_id";
    private static final int TIMEOUT = 5;
    private static final String WORKLOAD_INSTANCE_NAME = "successfulpost";
    private static final String HELM_LIST_JSON_CONTENT_WITH_INSTANCE = "[{\"name\":\"successfulpost\",\"namespace\":\"namespace\"," +
            "\"revision\":\"1\",\"updated\":\"2022-11-08 08:27:28.088825031 +0200 EET\",\"status\":\"deployed\",\"chart\":" +
            "\"integration_chart-1.2.3-4\",\"app_version\":\"1.0.12-10\"}]";
    private static final String NAMESPACE = "namespace";
    private static final String HELM_LIST_COMMAND = "helm list --output json --filter successfulpost -n namespace";
    private static final String HELM_CASCADE_DELETE_COMMAND = "helm uninstall workloadInstanceName --wait --cascade=foreground --timeout 300s -n tom";
    private static final String CORRECT_PULL_COMMAND = "helm pull https://test-chart-rgistry:8080/internal/charts/eric-test-chart-1.0.0.tgz" +
            " --username admin --password pass --ca-file /tmp/cacert.pem --cert-file /tmp/client.pem " +
            "--key-file /tmp/clientkey.pem -d /tmp/test-folder";
    private static final String CORRECT_CHART_URL = "https://test-chart-rgistry:8080/internal/charts/eric-test-chart-1.0.0.tgz";
    private static final String CORRECT_CHART_URL_WITH_HTTP = "http://test-chart-rgistry:8080/internal/charts/eric-test-chart-1.0.0.tgz";
    public static final String PULL_CHART_ERROR_MESSAGE = "Helmfile or integration chart by url = " +
            "https://test-chart-rgistry:8080/internal/charts/eric-test-chart-1.0.0.tgz was NOT FETCHED. Something went wrong, details: 404 Not Found";
    @Autowired
    private HelmSourceServiceImpl helmSourceService;
    @MockBean
    private OperationService operationService;
    @MockBean
    private AsyncExecutor asyncExecutor;
    @MockBean
    private HelmSourceCommandBuilder commandBuilder;
    @MockBean
    private CommandExecutorHelper commandExecutorHelper;
    @MockBean
    private FileService fileService;
    @Mock
    private Path helmPath;
    @Mock
    private Path valuesPath;

    @Test
    void shouldTerminateInstanceForIntegrationChart() {
        WorkloadInstance testInstance = basicWorkloadInstance();
        HelmSource testHelmSource = basicIntegrationChart(testInstance);
        FilePathDetails paths = getPaths(helmPath, valuesPath);
        Operation terminate = basicTerminateOperation(testInstance);

        when(operationService.create(any())).thenReturn(terminate);
        when(commandBuilder.delete(testHelmSource, paths)).thenReturn(HELM_CASCADE_DELETE_COMMAND);
        when(commandExecutorHelper.executeWithRetry(HELM_LIST_COMMAND, TIMEOUT))
                .thenReturn(new CommandResponse(HELM_LIST_JSON_CONTENT_WITH_INSTANCE, 0));

        Operation result = helmSourceService.destroyHelmSource(testHelmSource, TIMEOUT, paths, false);

        verify(operationService).create(any());
        verify(commandBuilder).delete(testHelmSource, paths);
        verify(asyncExecutor).executeAndUpdateOperationForTerminate(terminate, paths, HELM_CASCADE_DELETE_COMMAND, testInstance,
                                                                    testHelmSource.getHelmSourceType(), false);
        assertThat(result).isNotNull();
        assertThat(result.getState()).isEqualTo(OperationState.PROCESSING);
    }

    @Test
    void shouldDownloadChartByHelmCommand() {
        Path tmpDir = Path.of("/tmp/test-folder");
        when(fileService.createDirectory()).thenReturn(tmpDir);
        when(commandExecutorHelper.execute(anyString(), anyInt())).thenReturn(new CommandResponse("", 0));

        Path result = helmSourceService.downloadHelmSource(CORRECT_CHART_URL, true);

        assertThat(result).isEqualTo(tmpDir.resolve("eric-test-chart-1.0.0.tgz"));
        verify(fileService).createDirectory();
        verify(commandExecutorHelper).execute(CORRECT_PULL_COMMAND, 5);
    }

    @Test
    void shouldDownloadChartByHelmCommandIfUrlContainsHttp() {
        Path tmpDir = Path.of("/tmp/test-folder");
        when(fileService.createDirectory()).thenReturn(tmpDir);
        when(commandExecutorHelper.execute(anyString(), anyInt())).thenReturn(new CommandResponse("", 0));

        Path result = helmSourceService.downloadHelmSource(CORRECT_CHART_URL_WITH_HTTP, true);

        assertThat(result).isEqualTo(tmpDir.resolve("eric-test-chart-1.0.0.tgz"));
        verify(fileService).createDirectory();
        verify(commandExecutorHelper).execute(CORRECT_PULL_COMMAND, 5);
    }

    @Test
    void shouldThrowExceptionIfPullCommandWithExitCodeNonZero() {
        Path tmpDir = Path.of("/tmp/test-folder");
        when(fileService.createDirectory()).thenReturn(tmpDir);
        when(commandExecutorHelper.execute(anyString(), anyInt())).thenReturn(new CommandResponse("404 Not Found", -1));

        assertThatThrownBy(() -> helmSourceService.downloadHelmSource(CORRECT_CHART_URL_WITH_HTTP, true))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(PULL_CHART_ERROR_MESSAGE);

        verify(fileService).createDirectory();
        verify(fileService).deleteDirectory(tmpDir);
        verify(commandExecutorHelper).execute(CORRECT_PULL_COMMAND, 5);
    }

    private Operation basicTerminateOperation(WorkloadInstance workloadInstance) {
        return Operation.builder()
                .helmSourceId(HELMSOURCE_ID)
                .workloadInstance(workloadInstance)
                .type(OperationType.TERMINATE)
                .startTime(LocalDateTime.now())
                .state(OperationState.PROCESSING)
                .id(OPERATION_ID)
                .build();
    }

    private WorkloadInstance basicWorkloadInstance() {
        return WorkloadInstance.builder()
                .workloadInstanceId("fake_id")
                .workloadInstanceName(WORKLOAD_INSTANCE_NAME)
                .cluster("cluster")
                .namespace(NAMESPACE)
                .additionalParameters("some additional parameters here")
                .build();
    }

    private HelmSource basicIntegrationChart(WorkloadInstance workloadInstance) {
        return HelmSource.builder()
                .content(new byte[]{})
                .helmSourceType(INTEGRATION_CHART)
                .workloadInstance(workloadInstance)
                .created(LocalDateTime.now())
                .id(HELMSOURCE_ID)
                .build();
    }

    private FilePathDetails getPaths(Path helmSourcePath, Path valuesPath) {
        return FilePathDetails.builder()
                .helmSourcePath(helmSourcePath)
                .valuesPath(valuesPath)
                .build();
    }

}
