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

package com.ericsson.oss.management.lcm.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.management.lcm.constants.HelmfileContentConstants.HELMFILE_BASIC_VERSION;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ericsson.oss.management.lcm.presentation.exceptions.InternalRuntimeException;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.HelmfileExecutorApplication;
import com.ericsson.oss.management.lcm.constants.HelmSourceConstants;
import com.ericsson.oss.management.lcm.model.internal.Chart;
import com.ericsson.oss.management.lcm.model.internal.HelmfileData;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidInputException;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandExecutor;
import com.ericsson.oss.management.lcm.utils.sorting.OrderSort;

@ActiveProfiles("test")
@SpringBootTest(classes = HelmfileExecutorApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class HelmfileHandlerTest extends AbstractDbSetupTest {

    @Autowired
    private HelmfileHandler handler;
    @Autowired
    private FileService fileService;
    @SpyBean
    private CommandExecutor commandExecutor;
    @Autowired
    private OrderSort orderSort;

    private static final String REPOSITORY = "https://arm.sero.gic.ericsson.se/artifactory/proj-adp-gs-all-helm";
    private static final String VERSION = "1.2.3-89";
    private static final String CRD_HELMFILE_NAME = "crds-helmfile.yaml";
    private static final Integer TIMEOUT = 5;

    @Test
    void shouldConvertUnorderedNonCrdChartsOnlyToHelmfileSuccessfully() throws IOException {
        HelmfileData request = getRequest(getCharts(false, "release-1", "release-2"));

        Path helmfileTgz = handler.convertHelmfile(request, HELMFILE_BASIC_VERSION);

        String helmfileContent = getFileContentByName(helmfileTgz, HelmSourceConstants.HELMFILE_YAML_FILENAME);
        assertThat(helmfileContent).isEqualTo(helmfileWithoutCrdContent());
        String metadataContent = getFileContentByName(helmfileTgz, HelmSourceConstants.METADATA_YAML);
        assertThat(metadataContent).isEqualTo(metadataContent());

        fileService.deleteDirectory(helmfileTgz.getParent());
    }

    @Test
    void shouldConvertUnorderedChartsToHelmfileSuccessfully() throws IOException {
        List<Chart> charts = getCharts(false, "release-1", "release-2");
        charts.addAll(getCharts(true, "crd-release"));
        HelmfileData request = getRequest(charts);

        Path helmfileTgz = handler.convertHelmfile(request, HELMFILE_BASIC_VERSION);
        String helmfileContent = getFileContentByName(helmfileTgz, HelmSourceConstants.HELMFILE_YAML_FILENAME);
        assertThat(helmfileContent).isEqualTo(helmfileWithCrdContent());

        String crdHelmfileContent = getFileContentByName(helmfileTgz, CRD_HELMFILE_NAME);
        assertThat(crdHelmfileContent).isEqualTo(crdHelmfileContent());

        fileService.deleteDirectory(helmfileTgz.getParent());
    }

    @Test
    void shouldConvertOrderedChartsToHelmfileSuccessfully() throws IOException {
        List<Chart> charts = getCharts(false, "release-2", "release-1");
        charts.get(0).setOrder(2);
        charts.get(1).setOrder(1);
        List<Chart> crdCharts = getCharts(true, "crd-release");
        crdCharts.get(0).setOrder(3);
        charts.addAll(crdCharts);
        HelmfileData request = getRequest(charts);

        Path helmfileTgz = handler.convertHelmfile(request, HELMFILE_BASIC_VERSION);
        String helmfileContent = getFileContentByName(helmfileTgz, HelmSourceConstants.HELMFILE_YAML_FILENAME);
        assertThat(helmfileContent).isEqualTo(orderedHelmfileWithCrdContent());

        String crdHelmfileContent = getFileContentByName(helmfileTgz, CRD_HELMFILE_NAME);
        assertThat(crdHelmfileContent).isEqualTo(crdHelmfileContent());

        fileService.deleteDirectory(helmfileTgz.getParent());
    }

    @Test
    void shouldConvertOrderedChartsWithDuplicatedOrderToHelmfileSuccessfully() throws IOException {
        List<Chart> charts = getCharts(false, "release-1", "release-2");
        charts.get(0).setOrder(1);
        charts.get(1).setOrder(1);
        List<Chart> crdCharts = getCharts(true, "crd-release");
        crdCharts.get(0).setOrder(3);
        charts.addAll(crdCharts);
        HelmfileData request = getRequest(charts);

        Path helmfileTgz = handler.convertHelmfile(request, HELMFILE_BASIC_VERSION);
        String helmfileContent = getFileContentByName(helmfileTgz, HelmSourceConstants.HELMFILE_YAML_FILENAME);
        assertThat(helmfileContent).isEqualTo(helmfileWithCrdContent());

        String crdHelmfileContent = getFileContentByName(helmfileTgz, CRD_HELMFILE_NAME);
        assertThat(crdHelmfileContent).isEqualTo(crdHelmfileContent());

        fileService.deleteDirectory(helmfileTgz.getParent());
    }

    @Test
    void shouldOrderCrdSuccessfully() throws IOException {
        List<Chart> charts = getCharts(false, "release-1", "release-2");
        charts.get(0).setOrder(1);
        charts.get(1).setOrder(2);
        List<Chart> crdCharts = getCharts(true, "crd-release-3", "crd-release-2", "crd-release-1");
        crdCharts.get(0).setOrder(6);
        crdCharts.get(1).setOrder(5);
        crdCharts.get(2).setOrder(4);
        charts.addAll(crdCharts);
        HelmfileData request = getRequest(charts);

        Path helmfileTgz = handler.convertHelmfile(request, HELMFILE_BASIC_VERSION);
        String helmfileContent = getFileContentByName(helmfileTgz, HelmSourceConstants.HELMFILE_YAML_FILENAME);
        assertThat(helmfileContent).isEqualTo(secondOrderedHelmfileWithCrdContent());

        String crdHelmfileContent = getFileContentByName(helmfileTgz, CRD_HELMFILE_NAME);

        assertThat(crdHelmfileContent).isEqualTo(orderedCrdWithNeedsHelmfileContent());

        fileService.deleteDirectory(helmfileTgz.getParent());
    }

    @Test
    void shouldFailWhenOrderIsNotSetForEachChart() {
        List<Chart> charts = getCharts(false, "release-1", "release-2");
        charts.get(0).setOrder(null);
        charts.get(1).setOrder(1);
        List<Chart> crdCharts = getCharts(true, "crd-release");
        crdCharts.get(0).setOrder(3);
        charts.addAll(crdCharts);
        HelmfileData request = getRequest(charts);

        assertThatThrownBy(() -> handler.convertHelmfile(request, HELMFILE_BASIC_VERSION))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void shouldFailWhenChartsNull() {
        HelmfileData request = getRequest(null);

        assertThatThrownBy(() -> handler.convertHelmfile(request, HELMFILE_BASIC_VERSION))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void shouldFailWhenChartsEmpty() {
        HelmfileData request = getRequest(new ArrayList<>());

        assertThatThrownBy(() -> handler.convertHelmfile(request, HELMFILE_BASIC_VERSION))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void shouldThrowExceptionWhenFailsToExecuteCreateHelmfileCommand() {
        List<Chart> charts = getCharts(false, "release-1", "release-2");
        HelmfileData request = getRequest(charts);
        CommandResponse response = new CommandResponse();
        response.setExitCode(1);
        when(commandExecutor.execute(anyString(), anyInt())).thenReturn(response);

        assertThatThrownBy(() -> handler.convertHelmfile(request, HELMFILE_BASIC_VERSION))
                .isInstanceOf(InternalRuntimeException.class)
                .hasMessageContaining("Failed to create helmfile.tgz. Details:");

    }

    private HelmfileData getRequest(List<Chart> charts) {
        HelmfileData request = new HelmfileData();
        request.setRepository(REPOSITORY);
        request.setTimeout(TIMEOUT);
        request.setCharts(charts);
        return request;
    }

    private List<Chart> getCharts(boolean isCrd, String... names) {
        return Stream.of(names)
                .map(item -> getUnorderedChart(isCrd, item))
                .collect(Collectors.toList());
    }

    private Chart getUnorderedChart(boolean isCrd, String name) {
        Chart chart = new Chart();
        chart.setOrder(null);
        chart.setName(name);
        chart.setCrd(isCrd);
        chart.setVersion(VERSION);
        return chart;
    }

    private String getFileContentByName(Path archive, String filename) throws IOException {
        Path directory = archive.getParent();
        Path file = fileService.getFileFromDirectory(directory, filename);
        return Files.readString(file);
    }

    private String helmfileWithoutCrdContent() {
        return "repositories:\n"
                + "  - name: repo\n"
                + "    url: https://arm.sero.gic.ericsson.se/artifactory/proj-adp-gs-all-helm\n"
                + "\n"
                + "---\n"
                + "\n"
                + "releases:\n"
                + "  - name: release-1\n"
                + "    chart: {{ .Values | get \"repository\" \"repo\" }}/release-1\n"
                + "    version: 1.2.3-89\n"
                + "    namespace: {{ .Values | get \"global.app.namespace\" }}\n"
                + "    installed: {{ and ( .Values | get \"global.app.enabled\") ( .Values | get \"release-1.enabled\") }}\n"
                + "    values: \n"
                + "      - \"./values-templates/release-1.yaml.gotmpl\"\n"
                + "  - name: release-2\n"
                + "    chart: {{ .Values | get \"repository\" \"repo\" }}/release-2\n"
                + "    version: 1.2.3-89\n"
                + "    namespace: {{ .Values | get \"global.app.namespace\" }}\n"
                + "    installed: {{ and ( .Values | get \"global.app.enabled\") ( .Values | get \"release-2.enabled\") }}\n"
                + "    values: \n"
                + "      - \"./values-templates/release-2.yaml.gotmpl\"";
    }

    private String helmfileWithCrdContent() {
        return "repositories:\n"
                + "  - name: repo\n"
                + "    url: https://arm.sero.gic.ericsson.se/artifactory/proj-adp-gs-all-helm\n"
                + "\n"
                + "---\n"
                + "\n"
                + "helmfiles:\n"
                + "  - path: crds-helmfile.yaml\n"
                + "    values:\n"
                + "    - {{ toYaml .Values | nindent 6 }}\n"
                + "\n"
                + "---\n"
                + "\n"
                + "releases:\n"
                + "  - name: release-1\n"
                + "    chart: {{ .Values | get \"repository\" \"repo\" }}/release-1\n"
                + "    version: 1.2.3-89\n"
                + "    namespace: {{ .Values | get \"global.app.namespace\" }}\n"
                + "    installed: {{ and ( .Values | get \"global.app.enabled\") ( .Values | get \"release-1.enabled\") }}\n"
                + "    values: \n"
                + "      - \"./values-templates/release-1.yaml.gotmpl\"\n"
                + "  - name: release-2\n"
                + "    chart: {{ .Values | get \"repository\" \"repo\" }}/release-2\n"
                + "    version: 1.2.3-89\n"
                + "    namespace: {{ .Values | get \"global.app.namespace\" }}\n"
                + "    installed: {{ and ( .Values | get \"global.app.enabled\") ( .Values | get \"release-2.enabled\") }}\n"
                + "    values: \n"
                + "      - \"./values-templates/release-2.yaml.gotmpl\"";
    }

    private String orderedHelmfileWithCrdContent() {
        return "repositories:\n"
                + "  - name: repo\n"
                + "    url: https://arm.sero.gic.ericsson.se/artifactory/proj-adp-gs-all-helm\n"
                + "\n"
                + "---\n"
                + "\n"
                + "helmfiles:\n"
                + "  - path: crds-helmfile.yaml\n"
                + "    values:\n"
                + "    - {{ toYaml .Values | nindent 6 }}\n"
                + "\n"
                + "---\n"
                + "\n"
                + "releases:\n"
                + "  - name: release-2\n"
                + "    chart: {{ .Values | get \"repository\" \"repo\" }}/release-2\n"
                + "    version: 1.2.3-89\n"
                + "    namespace: {{ .Values | get \"global.app.namespace\" }}\n"
                + "    installed: {{ and ( .Values | get \"global.app.enabled\") ( .Values | get \"release-2.enabled\") }}\n"
                + "    values: \n"
                + "      - \"./values-templates/release-2.yaml.gotmpl\"\n"
                + "    needs:\n"
                + "    - release-1\n"
                + "  - name: release-1\n"
                + "    chart: {{ .Values | get \"repository\" \"repo\" }}/release-1\n"
                + "    version: 1.2.3-89\n"
                + "    namespace: {{ .Values | get \"global.app.namespace\" }}\n"
                + "    installed: {{ and ( .Values | get \"global.app.enabled\") ( .Values | get \"release-1.enabled\") }}\n"
                + "    values: \n"
                + "      - \"./values-templates/release-1.yaml.gotmpl\"";
    }

    private String secondOrderedHelmfileWithCrdContent() {
        return "repositories:\n"
                + "  - name: repo\n"
                + "    url: https://arm.sero.gic.ericsson.se/artifactory/proj-adp-gs-all-helm\n"
                + "\n"
                + "---\n"
                + "\n"
                + "helmfiles:\n"
                + "  - path: crds-helmfile.yaml\n"
                + "    values:\n"
                + "    - {{ toYaml .Values | nindent 6 }}\n"
                + "\n"
                + "---\n"
                + "\n"
                + "releases:\n"
                + "  - name: release-1\n"
                + "    chart: {{ .Values | get \"repository\" \"repo\" }}/release-1\n"
                + "    version: 1.2.3-89\n"
                + "    namespace: {{ .Values | get \"global.app.namespace\" }}\n"
                + "    installed: {{ and ( .Values | get \"global.app.enabled\") ( .Values | get \"release-1.enabled\") }}\n"
                + "    values: \n"
                + "      - \"./values-templates/release-1.yaml.gotmpl\"\n"
                + "  - name: release-2\n"
                + "    chart: {{ .Values | get \"repository\" \"repo\" }}/release-2\n"
                + "    version: 1.2.3-89\n"
                + "    namespace: {{ .Values | get \"global.app.namespace\" }}\n"
                + "    installed: {{ and ( .Values | get \"global.app.enabled\") ( .Values | get \"release-2.enabled\") }}\n"
                + "    values: \n"
                + "      - \"./values-templates/release-2.yaml.gotmpl\"\n"
                + "    needs:\n"
                + "    - release-1";
    }

    private String crdHelmfileContent() {
        return "releases:\n"
                + "  - name: crd-release\n"
                + "    chart: {{ .Values | get \"repository\" \"repo\" }}/crd-release\n"
                + "    version: 1.2.3-89\n"
                + "    namespace: {{ .Values | get \"global.crd.namespace\" }}\n"
                + "    installed: {{ and ( .Values | get \"global.app.enabled\") ( .Values | get \"crd-release.enabled\") }}";
    }

    private String orderedCrdWithNeedsHelmfileContent() {
        return "releases:\n"
                + "  - name: crd-release-3\n"
                + "    chart: {{ .Values | get \"repository\" \"repo\" }}/crd-release-3\n"
                + "    version: 1.2.3-89\n"
                + "    namespace: {{ .Values | get \"global.crd.namespace\" }}\n"
                + "    installed: {{ and ( .Values | get \"global.app.enabled\") ( .Values | get \"crd-release-3.enabled\") }}\n"
                + "    needs:\n"
                + "    - crd-release-2\n"
                + "  - name: crd-release-2\n"
                + "    chart: {{ .Values | get \"repository\" \"repo\" }}/crd-release-2\n"
                + "    version: 1.2.3-89\n"
                + "    namespace: {{ .Values | get \"global.crd.namespace\" }}\n"
                + "    installed: {{ and ( .Values | get \"global.app.enabled\") ( .Values | get \"crd-release-2.enabled\") }}\n"
                + "    needs:\n"
                + "    - crd-release-1\n"
                + "  - name: crd-release-1\n"
                + "    chart: {{ .Values | get \"repository\" \"repo\" }}/crd-release-1\n"
                + "    version: 1.2.3-89\n"
                + "    namespace: {{ .Values | get \"global.crd.namespace\" }}\n"
                + "    installed: {{ and ( .Values | get \"global.app.enabled\") ( .Values | get \"crd-release-1.enabled\") }}";
    }

    private String metadataContent() {
        return "name: helmfile-generated-by-executor\n"
                + "version: 0.1.0-1";
    }
}