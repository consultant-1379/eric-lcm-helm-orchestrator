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

package com.ericsson.oss.management.lcm.utils.command.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ericsson.oss.management.lcm.TestUtils;
import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.HelmSourceType;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.Release;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidFileException;

@SpringBootTest(classes = { HelmSourceCommandBuilderImpl.class })
class HelmSourceCommandBuilderImplTest {

    private static final String HELMFILE_PATH = "/tmp/b3ui3ofn3/helmfile.yaml";
    private static final String INTEGRATION_CHART_PATH = "/tmp/b3ui3ofn3/helmsource-1.2.3-4.tgz";
    private static final String VALUES_YAML_PATH = "/tmp/b3ui3ofn3/env-profiles/disable-everything.yaml";
    private static final String KUBE_CONFIG_YAML_PATH = "/tmp/b3ui3ofn3/env-profiles/kubeConfigs.yaml";
    private static final String NAMESPACE = "tom";
    private static final String WORKLOAD_INSTANCE_NAME = "workloadInstanceName";
    private static final String COMMAND_INSTALL_INTEGRATION_CHART_FULL = "KUBECONFIG=/tmp/b3ui3ofn3/env-profiles/kubeConfigs.yaml "
            + "helm install --wait --timeout 300s -n tom workloadInstanceName "
            + "/tmp/b3ui3ofn3/helmsource-1.2.3-4.tgz --values /tmp/b3ui3ofn3/env-profiles/disable-everything.yaml";
    private static final String COMMAND_INSTALL_INTEGRATION_CHART_SHORT = "helm install --wait --timeout 300s "
            + "-n tom workloadInstanceName /tmp/b3ui3ofn3/helmsource-1.2.3-4.tgz";
    private static final String COMMAND_UPDATE_INTEGRATION_CHART_FULL = "KUBECONFIG=/tmp/b3ui3ofn3/env-profiles/kubeConfigs.yaml "
            + "helm upgrade --install --wait --timeout 300s -n tom workloadInstanceName "
            + "/tmp/b3ui3ofn3/helmsource-1.2.3-4.tgz --values /tmp/b3ui3ofn3/env-profiles/disable-everything.yaml";
    private static final String COMMAND_UPDATE_INTEGRATION_CHART_SHORT = "helm upgrade --install --wait --timeout 300s "
            + "-n tom workloadInstanceName /tmp/b3ui3ofn3/helmsource-1.2.3-4.tgz";
    private static final String COMMAND_APPLY_FULL = "KUBECONFIG=/tmp/b3ui3ofn3/env-profiles/kubeConfigs.yaml helmfile "
            + "--file /tmp/b3ui3ofn3/helmfile.yaml --state-values-set global.default.timeout=300 --state-values-file "
            + "/tmp/b3ui3ofn3/env-profiles/disable-everything.yaml apply --wait";
    private static final String COMMAND_APPLY_SHORT = "helmfile --file /tmp/b3ui3ofn3/helmfile.yaml --state-values-set "
            + "global.default.timeout=300 apply --wait";
    private static final String COMMAND_DESTROY_INTEGRATION_CHART = "helm uninstall -n tom workloadInstanceName";
    private static final String COMMAND_CASCADE_DESTROY_INTEGRATION_CHART = "helm uninstall workloadInstanceName --wait" +
            " --cascade=foreground --timeout 300s -n tom";
    private static final String COMMAND_DESTROY_SHORT = "helmfile --file /tmp/b3ui3ofn3/helmfile.yaml --state-values-set "
            + "global.default.timeout=300 " +
            "--state-values-file /tmp/b3ui3ofn3/env-profiles/disable-everything.yaml apply --wait";
    private static final String ADDITIONAL_PARAMETERS = "{\n" +
            "  \"eric.lcm.helm.chart.registry.enabled\": \"true\",\n" +
            "  \"eric.lcm.container.registry.enabled\":\"true\"\n" +
            " }";
    private static final String VERIFY_INTEGRATION_CHART_COMMAND = "helm template %s/integration_chart-1.2.3-4.tgz --output-dir %s/output";
    private static final String VERIFY_SINGLE_HELMFILE_COMMAND = "helmfile --state-values-file %s/values.yaml " +
            "--file %s/helmfile.yaml template --output-dir output";
    private static final String VERIFY_SINGLE_HELMFILE_COMMAND_WITH_EXTERNAL_VALUES = "helmfile --state-values-file %s/123456789values.yaml" +
            " --file %s/helmfile.yaml template --output-dir output";
    private static final String LIST_COMMAND = "KUBECONFIG=/tmp/b3ui3ofn3/env-profiles/kubeConfigs.yaml "
            + "helmfile --file %s/helmfile.yaml --state-values-file %s --selector namespace=tom list "
            + "--output json";
    private static final String DELETE_RELEASES_COMMAND = "KUBECONFIG=/tmp/b3ui3ofn3/env-profiles/kubeConfigs.yaml "
            + "helm -n tom uninstall service-a service-b service-c";
    private static final String DELETE_ONE_RELEASE_COMMAND = "KUBECONFIG=/tmp/b3ui3ofn3/env-profiles/kubeConfigs.yaml helm -n " +
            "tom uninstall service-c";
    private static final String VALUES_FILE = "command-builder-test/values.yaml";
    private static final String INTEGRATION_CHART_NAME = "command-builder-test/integration_chart-1.2.3-4.tgz";
    private static final String EXTERNAL_VALUES_FILE = "command-builder-test/123456789values.yaml";
    private static final String HELMFILE_TEST_VALUES_YAML = "helmfile-test/values.yaml";
    private static final String LIST_INTEGRATION_CHART_COMMAND_WITH_KUBECONFIG = "KUBECONFIG=/tmp/b3ui3ofn3/env-profiles/kubeConfigs.yaml "
            + "helm list --output json --filter workloadInstanceName -n tom";
    private static final String LIST_INTEGRATION_CHART_COMMAND_WITHOUT_KUBECONFIG = "helm list --output json " +
            "--filter workloadInstanceName -n tom";

    @Autowired
    private HelmSourceCommandBuilderImpl helmSourceCommandBuilder;

    private static Path helmSourcePath;
    private static Path valuesPath;
    private static Path kubeConfigYamlPath;

    @BeforeAll
    static void setup() {
        helmSourcePath = Path.of(HELMFILE_PATH);
        valuesPath = Path.of(VALUES_YAML_PATH);
        kubeConfigYamlPath = Path.of(KUBE_CONFIG_YAML_PATH);
    }

    @Test
    void shouldBuildApplyCommandWithOnlyMandatoryParametersForHelmfile() {
        HelmSource helmfile = getHelmfile();
        helmfile.getWorkloadInstance().setAdditionalParameters(null);

        assertThat(helmSourceCommandBuilder.apply(helmfile, getPaths(helmSourcePath, null, null), 5,
                                                  OperationType.INSTANTIATE))
                .isEqualTo(COMMAND_APPLY_SHORT);
    }

    @Test
    void shouldBuildApplyCommandWithAllParametersForHelmfile() {
        HelmSource helmfile = getHelmfile();

        assertThat(helmSourceCommandBuilder.apply(helmfile, getPaths(helmSourcePath, valuesPath, kubeConfigYamlPath), 5,
                                                  OperationType.INSTANTIATE))
                .isEqualTo(COMMAND_APPLY_FULL);
    }

    @Test
    void shouldBuildInstallCommandWithAllParametersForIntegrationChart() {
        helmSourcePath = Path.of(INTEGRATION_CHART_PATH);
        HelmSource integrationChart = getIntegrationChart();

        assertThat(helmSourceCommandBuilder.apply(integrationChart, getPaths(helmSourcePath, valuesPath, kubeConfigYamlPath), 5,
                                                  OperationType.INSTANTIATE))
                .isEqualTo(COMMAND_INSTALL_INTEGRATION_CHART_FULL);
    }

    @Test
    void shouldBuildInstallCommandWithOnlyMandatoryParametersForIntegrationChart() {
        helmSourcePath = Path.of(INTEGRATION_CHART_PATH);
        HelmSource integrationChart = getIntegrationChart();
        integrationChart.getWorkloadInstance().setAdditionalParameters(null);

        assertThat(helmSourceCommandBuilder.apply(integrationChart, getPaths(helmSourcePath, null, null), 5,
                                                  OperationType.INSTANTIATE))
                .isEqualTo(COMMAND_INSTALL_INTEGRATION_CHART_SHORT);
    }

    @Test
    void shouldBuildUpdateCommandWithAllParametersForIntegrationChart() {
        helmSourcePath = Path.of(INTEGRATION_CHART_PATH);
        HelmSource integrationChart = getIntegrationChart();

        assertThat(helmSourceCommandBuilder.apply(integrationChart, getPaths(helmSourcePath, valuesPath, kubeConfigYamlPath), 5,
                                                  OperationType.UPDATE))
                .isEqualTo(COMMAND_UPDATE_INTEGRATION_CHART_FULL);
    }

    @Test
    void shouldBuildUpdateCommandWithOnlyMandatoryParametersForIntegrationChart() {
        helmSourcePath = Path.of(INTEGRATION_CHART_PATH);
        HelmSource integrationChart = getIntegrationChart();
        integrationChart.getWorkloadInstance().setAdditionalParameters(null);

        assertThat(helmSourceCommandBuilder.apply(integrationChart, getPaths(helmSourcePath, null, null), 5,
                                                  OperationType.UPDATE))
                .isEqualTo(COMMAND_UPDATE_INTEGRATION_CHART_SHORT);
    }

    @Test
    void shouldBuildDeleteCommandWithOnlyMandatoryParameters() {
        HelmSource helmfile = getHelmfile();

        assertThat(helmSourceCommandBuilder.delete(helmfile, getPaths(helmSourcePath, valuesPath, null)))
                .isEqualTo(COMMAND_DESTROY_SHORT);
    }

    @Test
    void shouldBuildDeleteCommandWithAllParameters() {
        HelmSource helmfile = getHelmfile();

        assertThat(helmSourceCommandBuilder.delete(helmfile, getPaths(helmSourcePath, valuesPath, kubeConfigYamlPath)))
                .isEqualTo(COMMAND_APPLY_FULL);
    }

    @Test
    void shouldBuildDeleteCommandForIntegrationChart() {
        HelmSource integrationChart = getIntegrationChart();

        assertThat(helmSourceCommandBuilder.delete(integrationChart, getPaths(null, null, null)))
                .isEqualTo(COMMAND_DESTROY_INTEGRATION_CHART);
    }

    @Test
    void shouldBuildDeleteCascadeCommandForIntegrationChart() {
        HelmSource integrationChart = getIntegrationChart();

        assertThat(helmSourceCommandBuilder.deleteCascadeIntegrationChart(integrationChart))
                .isEqualTo(COMMAND_CASCADE_DESTROY_INTEGRATION_CHART);
    }

    @Test
    void shouldReturnCommandToVerifySingleHelmfile() throws URISyntaxException {
        Path values = TestUtils.getResource(HELMFILE_TEST_VALUES_YAML);

        String expectedResult = String.format(VERIFY_SINGLE_HELMFILE_COMMAND, values.getParent(), values.getParent());

        String result = helmSourceCommandBuilder.verifyHelmfile(values);
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void shouldReturnCommandToVerifySingleHelmfileWhenExternalValues() throws URISyntaxException {
        Path values = TestUtils.getResource(EXTERNAL_VALUES_FILE);
        String result = helmSourceCommandBuilder.verifyHelmfile(values);
        String expectedResult = String.format(VERIFY_SINGLE_HELMFILE_COMMAND_WITH_EXTERNAL_VALUES, values.getParent(), values.getParent());
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void shouldReturnCommandToVerifyIntegrationChart() throws URISyntaxException {
        Path integrationChartPath = TestUtils.getResource(INTEGRATION_CHART_NAME);
        String result = helmSourceCommandBuilder.verifyIntegrationChart(integrationChartPath);

        String expectedResult = String.format(VERIFY_INTEGRATION_CHART_COMMAND, integrationChartPath.getParent(), integrationChartPath.getParent());

        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void shouldFailWhenIntegrationChartNotFound() {
        Path integrationChartPath = Path.of("some-path/invalid");

        assertThatThrownBy(() -> helmSourceCommandBuilder.verifyIntegrationChart(integrationChartPath))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void shouldReturnListCommand() throws URISyntaxException {
        Path values = TestUtils.getResource(HELMFILE_TEST_VALUES_YAML);

        String result = helmSourceCommandBuilder.buildListCommand(getPaths(null, values, kubeConfigYamlPath), NAMESPACE);

        String correctResult = String.format(LIST_COMMAND, values.getParent(), values);
        assertThat(result).isEqualTo(correctResult);
    }

    @Test
    void shouldReturnDeleteReleasesCommand() {
        List<Release> responses = new ArrayList<>(List.of(
                Release.builder().name("service-a").enabled(true).build(),
                Release.builder().name("service-b").enabled(true).build(),
                Release.builder().name("service-c").enabled(true).build()
        ));

        String result = helmSourceCommandBuilder.deleteReleases(responses, NAMESPACE, kubeConfigYamlPath);

        assertThat(result).isEqualTo(DELETE_RELEASES_COMMAND);
    }

    @Test
    void shouldIgnoreReleasesWithEmptyOrNullableNames() {
        List<Release> responses = new ArrayList<>(List.of(
                Release.builder().name("").enabled(true).build(),
                Release.builder().name(null).enabled(true).build(),
                Release.builder().name("service-c").enabled(true).build()
        ));

        String result = helmSourceCommandBuilder.deleteReleases(responses, NAMESPACE, kubeConfigYamlPath);

        assertThat(result).isEqualTo(DELETE_ONE_RELEASE_COMMAND);
    }

    @Test
    void shouldFailWhenHelmfileNotFound() throws URISyntaxException {
        String noHelmFile = TestUtils.getResource("no-helmfile") + "/" + VALUES_FILE;
        Path values = Paths.get(noHelmFile);

        assertThatThrownBy(() -> helmSourceCommandBuilder.verifyHelmfile(values))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void shouldReturnHelmListCommandForIntegrationChartWithKubeconfig() {
        String result = helmSourceCommandBuilder.buildHelmListCommandWithFilterByName(NAMESPACE, WORKLOAD_INSTANCE_NAME, kubeConfigYamlPath);

        assertThat(result).isEqualTo(LIST_INTEGRATION_CHART_COMMAND_WITH_KUBECONFIG);
    }

    @Test
    void shouldReturnHelmListCommandForIntegrationChartWithoutKubeconfig() {
        String result = helmSourceCommandBuilder.buildHelmListCommandWithFilterByName(NAMESPACE, WORKLOAD_INSTANCE_NAME, null);

        assertThat(result).isEqualTo(LIST_INTEGRATION_CHART_COMMAND_WITHOUT_KUBECONFIG);
    }

    private HelmSource getHelmfile() {
        HelmSource helmfile = new HelmSource();
        helmfile.setHelmSourceType(HelmSourceType.HELMFILE);
        helmfile.setWorkloadInstance(getWorkloadInstance());
        return helmfile;
    }

    private HelmSource getIntegrationChart() {
        HelmSource integrationChart = new HelmSource();
        integrationChart.setHelmSourceType(HelmSourceType.INTEGRATION_CHART);
        integrationChart.setWorkloadInstance(getWorkloadInstance());
        return integrationChart;
    }

    private WorkloadInstance getWorkloadInstance() {
        WorkloadInstance instance = new WorkloadInstance();
        instance.setNamespace(NAMESPACE);
        instance.setWorkloadInstanceName(WORKLOAD_INSTANCE_NAME);
        instance.setAdditionalParameters(ADDITIONAL_PARAMETERS);
        return instance;
    }

    private FilePathDetails getPaths(Path helmSourcePath, Path valuesPath, Path kubeConfigPath) {
        return FilePathDetails.builder()
                .helmSourcePath(helmSourcePath)
                .valuesPath(valuesPath)
                .kubeConfigPath(kubeConfigPath)
                .build();
    }
}
