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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LcmHelperImpl implements LcmHelper {

    @Override
    public void setNewHelmSourceToInstance(WorkloadInstance instance, HelmSource helmSource) {
        List<HelmSource> helmSources = Optional.ofNullable(instance.getHelmSources())
                .orElseGet(ArrayList::new);
        if (isHelmSourceIsNew(helmSources, helmSource)) {
            helmSources.add(helmSource);
            instance.setHelmSources(helmSources);
        }
    }

    @Override
    public FilePathDetails preparePaths(Path helmPath, Path valuesPath, Path kubeConfigPath) {
        return FilePathDetails.builder()
                .valuesPath(valuesPath)
                .helmSourcePath(helmPath)
                .kubeConfigPath(kubeConfigPath)
                .build();
    }

    private boolean isHelmSourceIsNew(List<HelmSource> helmSources, HelmSource helmSource) {
        return helmSources.stream()
                .filter(item -> item.getId().equals(helmSource.getId()))
                .findAny().isEmpty();
    }

}
