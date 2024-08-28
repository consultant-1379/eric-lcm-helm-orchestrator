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

import java.nio.file.Path;

import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;

/**
 * Helper class for storing the files
 */
public interface StoreFileHelper {

    /**
     * Store values file from request or retrieve it from storage, compose it with additional params
     *
     * @param directory
     * @param values
     * @param instance
     * @param helmSource
     * @param appEnabled
     * @param hasNewParameters
     * @return filePathDetails which contain paths to plain and encrypted values files in directory
     */
    FilePathDetails getValuesPath(Path directory, MultipartFile values, WorkloadInstance instance, HelmSource helmSource,
                                  boolean appEnabled, boolean hasNewParameters);

    /**
     * Store values file from directory, compose it with additional params
     *
     * @param directory
     * @param values
     * @param instance
     * @param helmSource
     * @param appEnabled
     * @return filePathDetails which contain paths to plain and encrypted values files in directory
     */
    FilePathDetails getValuesPathFromDirectory(Path directory, MultipartFile values, WorkloadInstance instance, HelmSource helmSource,
                                               boolean appEnabled);

    /**
     * Compose values file with additional params and store it
     *
     * @param values
     * @param instance
     * @param appEnabled
     * @return filePathDetails which contain paths to plain and encrypted values files in directory
     */
    Path mergeParamsToValues(Path values, WorkloadInstance instance, boolean appEnabled);

    /**
     * Store clusterConnectionInfo file to the directory
     *
     * @param directory
     * @param instance
     * @param clusterConnectionInfo
     * @return path to file
     */
    Path getKubeConfigPath(Path directory, WorkloadInstance instance, MultipartFile clusterConnectionInfo);
}
