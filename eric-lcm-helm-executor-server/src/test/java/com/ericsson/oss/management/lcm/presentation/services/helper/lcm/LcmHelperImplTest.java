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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.HelmfileExecutorApplication;
import com.ericsson.oss.management.lcm.model.entity.HelmSource;
import com.ericsson.oss.management.lcm.model.entity.HelmSourceType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.internal.FilePathDetails;

@ActiveProfiles("test")
@SpringBootTest(classes = HelmfileExecutorApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LcmHelperImplTest extends AbstractDbSetupTest {

    @Autowired
    private LcmHelperImpl lcmHelper;

    private static final String HELMSOURCE_ID = "some_id";
    private static final String HELM_SOURCE_VERSION = "1.2.3-4";
    private static final String WORKLOAD_INSTANCE_ID = "wrkld_instance_id";
    private static final String WORKLOAD_INSTANCE_NAME = "workloadInstanceName";

    @Test
    void shouldSetHelmSourceWhenPrimaryListNull()  {
        WorkloadInstance instance = basicWorkloadInstance();
        HelmSource helmSource = getHelmSource();
        instance.setHelmSources(null);

        lcmHelper.setNewHelmSourceToInstance(instance, helmSource);

        assertThat(instance.getHelmSources()).isNotEmpty();
        assertThat(instance.getHelmSources()).contains(helmSource);
    }

    @Test
    void shouldSetHelmSourceWhenPrimaryListEmpty()  {
        WorkloadInstance instance = basicWorkloadInstance();
        HelmSource helmSource = getHelmSource();
        instance.setHelmSources(new ArrayList<>());

        lcmHelper.setNewHelmSourceToInstance(instance, helmSource);

        assertThat(instance.getHelmSources()).isNotEmpty();
        assertThat(instance.getHelmSources()).contains(helmSource);
    }

    @Test
    void shouldNotSetOldHelmSource()  {
        WorkloadInstance instance = basicWorkloadInstance();
        HelmSource helmSource1 = getHelmSource();
        List<HelmSource> helmSources = new ArrayList<>();
        helmSources.add(helmSource1);
        instance.setHelmSources(helmSources);
        HelmSource helmSource2 = getHelmSource();
        lcmHelper.setNewHelmSourceToInstance(instance, helmSource2);

        assertThat(instance.getHelmSources()).isNotEmpty()
                .hasSize(1)
                .contains(helmSource1);
    }

    @Test
    void shouldSetNewHelmSource()  {
        WorkloadInstance instance = basicWorkloadInstance();
        HelmSource helmSource1 = getHelmSource();
        List<HelmSource> helmSources = new ArrayList<>();
        helmSources.add(helmSource1);
        instance.setHelmSources(helmSources);
        HelmSource helmSource2 = getHelmSource();
        helmSource2.setId("new_id");
        lcmHelper.setNewHelmSourceToInstance(instance, helmSource2);

        assertThat(instance.getHelmSources()).isNotEmpty()
                .hasSize(2)
                .contains(helmSource1)
                .contains(helmSource2);
    }

    @Test
    void shouldPrepareMap() {
        Path helmPath = Path.of("something");
        Path valuesPath = Path.of("something");
        Path kubeConfigPath = Path.of("something");

        FilePathDetails paths = lcmHelper.preparePaths(helmPath, valuesPath, kubeConfigPath);

        assertThat(paths).isNotNull();
        assertThat(paths.getHelmSourcePath()).isNotNull();
        assertThat(paths.getValuesPath()).isNotNull();
        assertThat(paths.getKubeConfigPath()).isNotNull();
    }

    private WorkloadInstance basicWorkloadInstance() {
        return WorkloadInstance.builder()
                .workloadInstanceName(WORKLOAD_INSTANCE_NAME)
                .workloadInstanceId(WORKLOAD_INSTANCE_ID)
                .build();
    }

    private HelmSource getHelmSource() {
        return HelmSource.builder()
                .id(HELMSOURCE_ID)
                .helmSourceType(HelmSourceType.HELMFILE)
                .helmSourceVersion(HELM_SOURCE_VERSION)
                .build();
    }

}