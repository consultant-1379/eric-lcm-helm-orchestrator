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

import static com.ericsson.oss.management.lcm.constants.CommandConstants.TAR_COMMAND;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.CRD_HELMFILE_YAML_FILENAME;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.HELMFILE_YAML_FILENAME;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.METADATA_YAML;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.VALUES_TEMPLATE;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.VALUES_TEMPLATE_DIR;
import static com.ericsson.oss.management.lcm.constants.HelmfileContentConstants.CHART_TEMPLATED;
import static com.ericsson.oss.management.lcm.constants.HelmfileContentConstants.CRD_HEADER;
import static com.ericsson.oss.management.lcm.constants.HelmfileContentConstants.CRD_NAMESPACE_TEMPLATED;
import static com.ericsson.oss.management.lcm.constants.HelmfileContentConstants.HELMFILE_NAME;
import static com.ericsson.oss.management.lcm.constants.HelmfileContentConstants.INSTALLED_TEMPLATED;
import static com.ericsson.oss.management.lcm.constants.HelmfileContentConstants.METADATA_CONTENT;
import static com.ericsson.oss.management.lcm.constants.HelmfileContentConstants.NAMESPACE_TEMPLATED;
import static com.ericsson.oss.management.lcm.constants.HelmfileContentConstants.RELEASES_HEADER;
import static com.ericsson.oss.management.lcm.constants.HelmfileContentConstants.REPOSITORY_NAME;
import static com.ericsson.oss.management.lcm.constants.HelmfileContentConstants.VALUES_TEMPLATE_CONTENT;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.ericsson.oss.management.lcm.presentation.exceptions.InternalRuntimeException;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ericsson.oss.management.lcm.model.internal.Chart;
import com.ericsson.oss.management.lcm.model.internal.HelmfileData;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidInputException;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandExecutor;
import com.ericsson.oss.management.lcm.utils.sorting.OrderSort;

@Component
public class HelmfileHandler {

    private final int operationTimeout;
    private final FileService fileService;
    private final CommandExecutor executor;
    private final OrderSort orderSort;

    @Autowired
    public HelmfileHandler(@Value("${operation.timeout}") int operationTimeout,
                           FileService fileService,
                           CommandExecutor executor,
                           OrderSort orderSort) {
        this.operationTimeout = operationTimeout;
        this.fileService = fileService;
        this.executor = executor;
        this.orderSort = orderSort;
    }

    /**
     * Convert charts from request to helmfile.tgz
     *
     * @param request to build helmfile from
     * @return path to the archived tgz (contains helmfile.yaml, crds-helmfile.yaml - optionally, metadata.yaml,
     * values-template folder with values file for each release)
     */
    public Path convertHelmfile(HelmfileData request, String version) {
        Path directory = fileService.createDirectory();
        String filename = HELMFILE_NAME + "-" + version + ".tgz";
        createHelmfiles(request, directory);
        createMetadata(version, directory);
        createValuesTemplate(request.getCharts(), directory);
        var command = String.format(TAR_COMMAND, directory.toAbsolutePath(), filename);
        int timeout = Optional.ofNullable(request.getTimeout()).orElse(operationTimeout);
        CommandResponse response = executor.execute(command, timeout);
        if (response.getExitCode() != 0) {
            throw new InternalRuntimeException(String.format("Failed to create helmfile.tgz. Details: %s", response.getOutput()));
        }
        return fileService.getFileFromDirectory(directory, filename);
    }

    private void createHelmfiles(HelmfileData request, Path directory) {
        List<Chart> charts = request.getCharts();
        if (charts == null || charts.isEmpty()) {
            throw new InvalidInputException("Charts can't be empty!");
        }

        String crdHelmfile = getReleases(orderSort.sortedRelease(charts, Chart::isCrd), CRD_NAMESPACE_TEMPLATED);
        var helmfile = getRepositoryHeader(request.getRepository()).toString();
        if (!crdHelmfile.isEmpty()) {
            crdHelmfile = RELEASES_HEADER + crdHelmfile;
            helmfile = helmfile + CRD_HEADER;
            fileService.createFile(directory, crdHelmfile.getBytes(StandardCharsets.UTF_8), CRD_HELMFILE_YAML_FILENAME);
        }
        String helmfileReleases = getReleases(orderSort.sortedRelease(charts, item -> !item.isCrd()), NAMESPACE_TEMPLATED);
        helmfile = helmfile + RELEASES_HEADER + helmfileReleases;
        fileService.createFile(directory, helmfile.getBytes(StandardCharsets.UTF_8), HELMFILE_YAML_FILENAME);
    }

    private void createMetadata(String version, Path directory) {
        var fileContent = String.format(METADATA_CONTENT, HELMFILE_NAME, version);
        fileService.createFile(directory, fileContent.getBytes(StandardCharsets.UTF_8), METADATA_YAML);
    }

    private void createValuesTemplate(List<Chart> charts, Path directory) {
        Path valuesDir = fileService.createDirectory(directory, VALUES_TEMPLATE_DIR);
        charts.stream().filter(chart -> !chart.isCrd())
                .map(Chart::getName)
                .forEach(name -> createChartValues(name, valuesDir));
    }

    private void createChartValues(String name, Path directory) {
        var filename = String.format(VALUES_TEMPLATE, name);
        fileService.createFile(directory, VALUES_TEMPLATE_CONTENT.getBytes(StandardCharsets.UTF_8), filename);
    }

    private String getReleases(List<Chart> charts, String namespace) {
        return charts.stream()
                .map(item -> getRelease(item, namespace))
                .collect(Collectors.joining("\n"));
    }

    private StringBuilder getRelease(Chart chart, String namespace) {
        String name = chart.getName();
        var stringBuilder = new StringBuilder();
        stringBuilder
                .append("  - name: ").append(name).append("\n")
                .append("    chart: ").append(String.format(CHART_TEMPLATED, name)).append("\n")
                .append("    version: ").append(chart.getVersion()).append("\n")
                .append("    namespace: ").append(namespace).append("\n")
                .append("    installed: ").append(String.format(INSTALLED_TEMPLATED, name))
                .append(getValues(chart))
                .append(getNeeds(chart.getNeeds()));
        return stringBuilder;
    }

    private StringBuilder getRepositoryHeader(String repository) {
        return new StringBuilder()
                .append("repositories:").append("\n")
                .append("  - name: ").append(REPOSITORY_NAME).append("\n")
                .append("    url: ").append(repository).append("\n\n")
                .append("---").append("\n\n");
    }

    private String getNeeds(Set<String> needs) {
        var stringBuilder = new StringBuilder();
        if (needs != null) {
            stringBuilder.append("\n").append("    needs:");
            needs.forEach(chartName -> stringBuilder.append("\n").append("    - ").append(chartName));
        }
        return stringBuilder.toString();
    }

    private String getValues(Chart chart) {
        var values = new StringBuilder();
        if (!chart.isCrd()) {
            values.append("\n")
                    .append("    values: ").append("\n")
                    .append(String.format("      - \"./values-templates/%s.yaml.gotmpl\"", chart.getName()));
        }
        return values.toString();
    }

}
