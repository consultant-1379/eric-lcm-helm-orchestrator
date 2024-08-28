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

package com.ericsson.oss.management.lcm.presentation.services.values;

import java.nio.file.Path;

import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.api.model.ValuesRequestDto;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;

/**
 * Service which posts and retrieves values files
 */
public interface ValuesService {

    /**
     * Post file to storage
     *
     * @param workloadInstanceName to create a name for processing
     * @param softwareVersion of a helm source
     * @param path of a file to save
     * @return the identifier of current values version
     */
    String post(String workloadInstanceName, String softwareVersion, Path path);

    /**
     * Retrieve latest file from storage
     *
     * @param workloadInstanceName to get a name for processing
     * @param softwareVersion of a helm source
     * @param directory to store obtained file
     * @return path to file from storage
     */
    Path retrieve(String workloadInstanceName, String softwareVersion, Path directory);

    /**
     * Retrieve file from storage be version
     *
     * @param workloadInstanceName to get a name for processing
     * @param version of proper workloadInstance
     * @param directory to store obtained file
     * @return path to file from storage
     */
    Path retrieveByVersion(String workloadInstanceName, WorkloadInstanceVersion version, Path directory);

    /**
     * Prepare name to create service name for work with values
     *
     * @param workloadInstanceName
     * @param helmSourceVersion
     * @return the name
     */
    default String prepareName(String workloadInstanceName, String helmSourceVersion) {
        return workloadInstanceName + "-" + helmSourceVersion;
    }

    /**
     * Get content of values file as byte array
     *
     * @param valuesId identifier of file
     * @return content
     */
    byte[] getContent(String valuesId);

    /**
     * Update values
     *
     * @param valuesId identifier of file
     * @param request dto which contains additional parameters
     * @param valuesFile new file
     * @return content of updated file
     */
    byte[] updateValues(String valuesId, ValuesRequestDto request, MultipartFile valuesFile);
}
