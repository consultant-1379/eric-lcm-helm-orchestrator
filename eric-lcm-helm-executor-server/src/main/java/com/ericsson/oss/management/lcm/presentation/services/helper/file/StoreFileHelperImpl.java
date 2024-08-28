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

package com.ericsson.oss.management.lcm.presentation.services.helper.file;

import static com.ericsson.oss.management.lcm.constants.CommandConstants.DOCKER_CREDS_SECRET_NAME;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.DOCKER_URL_KEY;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.GLOBAL_APP_ENABLED_ARGUMENT;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.GLOBAL_APP_NAMESPACE;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.GLOBAL_CHART_REGISTRY;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.GLOBAL_CRD_NAMESPACE;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.GLOBAL_PULL_SECRET;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.IMAGE_REPO_PREFIX;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.KUBE_CONFIG;
import static com.ericsson.oss.management.lcm.constants.HelmSourceConstants.VALUES_YAML;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;
import com.ericsson.oss.management.lcm.presentation.exceptions.NotValidClusterNameException;
import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import com.ericsson.oss.management.lcm.presentation.services.clusterconnectioninfo.ClusterConnectionInfoService;
import com.ericsson.oss.management.lcm.presentation.services.fileservice.FileService;
import com.ericsson.oss.management.lcm.presentation.services.values.ValuesService;
import com.ericsson.oss.management.lcm.presentation.services.version.WorkloadInstanceVersionService;
import com.ericsson.oss.management.lcm.utils.JSONParseUtils;
import com.ericsson.oss.management.lcm.utils.ValuesFileComposer;
import com.ericsson.oss.management.lcm.utils.validator.ClusterConnectionInfoFileValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StoreFileHelperImpl implements StoreFileHelper {

    private final FileService fileService;
    private final ClusterConnectionInfoService clusterConnectionInfoService;
    private final ValuesService valuesService;
    private final WorkloadInstanceVersionService workloadInstanceVersionService;
    private final ValuesFileComposer composer;
    private final ClusterConnectionInfoFileValidator clusterConnectionInfoFileValidator;

    @Value("${container-registry.url}")
    private String containerRegistryUrl;

    @Value("${helmrepo.name}")
    private String helmRepoName;

    @Value("${default.crd.namespace}")
    private String defaultCrdNamespace;

    @Override
    public FilePathDetails getValuesPath(Path directory, MultipartFile values, WorkloadInstance instance, HelmSource helmSource,
                                         boolean appEnabled, boolean hasNewParameters) {
        Optional<Path> possibleValuesPath = fileService.storeFileInIfPresent(directory, values, VALUES_YAML);
        boolean isValuesNew = true;

        Path valuesPath;

        if (possibleValuesPath.isPresent()) {
            valuesPath = possibleValuesPath.get();
        } else {
            valuesPath = getValuesFromStorageIfExists(instance, helmSource.getHelmSourceVersion(), directory, appEnabled);
            if (valuesPath != null) {
                isValuesNew = false;
            } else if (hasNewParameters) {
                valuesPath = directory;
            } else {
                throw new ResourceNotFoundException("There is no day-0-config, values.yaml and values in storage. You need to have at least one.");
            }
        }
        helmSource.setValuesRefreshed(isValuesNew || hasNewParameters);

        return FilePathDetails.builder()
                .valuesPath(mergeParamsToValues(valuesPath, instance, appEnabled))
                .build();
    }

    @Override
    public FilePathDetails getValuesPathFromDirectory(Path directory, MultipartFile values, WorkloadInstance instance, HelmSource helmSource,
                                                      boolean appEnabled) {
        Optional<Path> possibleValuesPath = fileService.storeFileInIfPresent(directory, values, VALUES_YAML);
        Path valuesPath;
        valuesPath = possibleValuesPath.orElseGet(() -> getValuesPathFromHelmSourceDirectory(directory, instance));
        helmSource.setValuesRefreshed(true);
        return FilePathDetails.builder()
                .valuesPath(mergeParamsToValues(valuesPath, instance, appEnabled))
                .build();
    }

    @Override
    public Path mergeParamsToValues(Path values, WorkloadInstance instance, boolean appEnabled) {
        Map<String, Object> additionalParameters = JSONParseUtils.parseJsonToMap(instance.getAdditionalParameters());
        fillAdditionalParameters(instance, additionalParameters, appEnabled);
        return composer.compose(values, additionalParameters);
    }

    @Override
    public Path getKubeConfigPath(Path directory, WorkloadInstance instance, MultipartFile clusterConnectionInfo) {
        Path clusterConnectionInfoPath;
        if (clusterConnectionInfo == null) {
            clusterConnectionInfoPath = getClusterConnectionInfoPathFromInstance(instance, directory);
            return clusterConnectionInfoPath;
        } else {
            clusterConnectionInfoPath = fileService.storeFileIn(directory, clusterConnectionInfo, KUBE_CONFIG);
            clusterConnectionInfoFileValidator.validate(clusterConnectionInfo, clusterConnectionInfoPath);
            return clusterConnectionInfoPath;
        }
    }

    private Path getValuesFromStorageIfExists(WorkloadInstance instance, String helmSourceVersion, Path directory, boolean appEnabled) {
        boolean helmSourceVersionNotExist = checkExistenceHelmSourceVersion(instance, helmSourceVersion);

        Path storedValues = null;
        if (helmSourceVersionNotExist || !appEnabled) {
            storedValues = getPreExistingValuesFromStorageByHelmSource(instance, helmSourceVersion, directory);
        }
        return Optional.ofNullable(storedValues)
                .orElseGet(() -> getPreviousValuesFromStorage(instance, directory));
    }

    private boolean checkExistenceHelmSourceVersion(WorkloadInstance instance, String helmSourceVersion) {
        return Optional.ofNullable(instance.getWorkloadInstanceVersions())
                .orElseGet(Collections::emptyList)
                .stream()
                .map(WorkloadInstanceVersion::getHelmSourceVersion)
                .noneMatch(item -> item.equals(helmSourceVersion));
    }

    private Path getPreExistingValuesFromStorageByHelmSource(WorkloadInstance instance, String helmSourceVersion, Path directory) {
        Path storedValues;
        try {
            storedValues = valuesService.retrieve(instance.getWorkloadInstanceName(), helmSourceVersion, directory);
        } catch (ResourceNotFoundException e) {
            storedValues = null;
        }
        return storedValues;
    }

    private Path getPreviousValuesFromStorage(WorkloadInstance instance, Path directory) {

        Path storedValues;
        try {
            var version = workloadInstanceVersionService.getVersion(instance);
            storedValues = valuesService.retrieveByVersion(instance.getWorkloadInstanceName(), version, directory);
        } catch (ResourceNotFoundException e) {
            storedValues = null;
        }
        return storedValues;
    }

    private void fillAdditionalParameters(WorkloadInstance instance, Map<String, Object> additionalParameters,
                                          boolean appEnabled) {
        if (StringUtils.isNotEmpty(containerRegistryUrl)) {
            String registry = changeRegistryUrlIfRequired(additionalParameters);
            additionalParameters.putIfAbsent(DOCKER_URL_KEY, registry);
        }

        String crdNamespace = Optional.ofNullable(instance.getCrdNamespace())
                .orElse(defaultCrdNamespace);
        additionalParameters.put(GLOBAL_CRD_NAMESPACE, crdNamespace);
        additionalParameters.put(GLOBAL_APP_NAMESPACE, instance.getNamespace());
        additionalParameters.put(GLOBAL_CHART_REGISTRY, helmRepoName);
        additionalParameters.put(GLOBAL_APP_ENABLED_ARGUMENT, appEnabled);
        additionalParameters.put(GLOBAL_PULL_SECRET, String.format(DOCKER_CREDS_SECRET_NAME, instance.getWorkloadInstanceName()));
    }

    private Path getClusterConnectionInfoPathFromInstance(final WorkloadInstance instance, final Path directory) {
        String clusterName = instance.getCluster();
        if (StringUtils.isNotEmpty(clusterName)) {
            return clusterConnectionInfoService
                    .findByClusterName(clusterName)
                    .stream()
                    .map(clusterConnectionInfo -> fileService.createFile(directory, clusterConnectionInfo.getContent(), KUBE_CONFIG))
                    .findAny()
                    .orElseThrow(() -> new NotValidClusterNameException(
                            String.format("Cluster with name %s not found", clusterName)));
        } else {
            return null;
        }
    }

    private Path getValuesPathFromHelmSourceDirectory(Path helmSourceDirectory, WorkloadInstance instance) {
        Path possibleValues = helmSourceDirectory.resolve(VALUES_YAML);
        if (fileService.fileExists(possibleValues)) {
            return possibleValues;
        } else if (instance.getAdditionalParameters() != null) {
            return helmSourceDirectory;
        } else {
            throw new ResourceNotFoundException("There is no values and additional parameters in request or directory. You need to have at "
                                                        + "least one.");
        }
    }

    private String changeRegistryUrlIfRequired(Map<String, Object> additionalParameters) {
        String registry = containerRegistryUrl;
        if (additionalParameters.containsKey(IMAGE_REPO_PREFIX)) {
            registry = containerRegistryUrl + "/" + additionalParameters.get(IMAGE_REPO_PREFIX);
            additionalParameters.remove(IMAGE_REPO_PREFIX);
        }
        return registry;
    }

}
