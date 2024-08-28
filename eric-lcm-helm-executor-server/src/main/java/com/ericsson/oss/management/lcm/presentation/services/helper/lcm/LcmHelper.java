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

package com.ericsson.oss.management.lcm.presentation.services.helper.lcm;


import java.nio.file.Path;

import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;

/**
 * Helps to perform different steps of LCM operations
 */
public interface LcmHelper {

    /**
     * Set helmSource to instance to set connection between them
     *
     * @param instance to update
     * @param helmSource to set
     */
    void setNewHelmSourceToInstance(WorkloadInstance instance, HelmSource helmSource);

    /**
     * Fill map with determined paths
     *
     * @param helmPath
     * @param valuesPath
     * @param kubeConfigPath
     * @return filePathDetails data which contains paths
     */
    FilePathDetails preparePaths(Path helmPath, Path valuesPath, Path kubeConfigPath);

}
