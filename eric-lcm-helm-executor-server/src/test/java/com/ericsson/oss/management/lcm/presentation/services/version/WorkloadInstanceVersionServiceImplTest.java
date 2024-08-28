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

package com.ericsson.oss.management.lcm.presentation.services.version;

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.HelmfileExecutorApplication;
import com.ericsson.oss.management.lcm.api.model.PagedWorkloadInstanceVersionDto;
import com.ericsson.oss.management.lcm.api.model.WorkloadInstanceVersionDto;
import com.ericsson.oss.management.lcm.model.entity.OperationType;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;
import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import com.ericsson.oss.management.lcm.presentation.mappers.WorkloadInstanceVersionDtoMapper;
import com.ericsson.oss.management.lcm.repositories.WorkloadInstanceVersionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = HelmfileExecutorApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WorkloadInstanceVersionServiceImplTest extends AbstractDbSetupTest {

    private static final String VALID_INSTANCE_NAME = "name";
    private static final String VALUES_VERSION = "0e35ed30-d438-4b07-a82b-cab447424d30";
    private static final String HELM_SOURCE_VERSION = "1.2.3-4";
    private static final String VALID_NAMESPACE = "7namespace-3-test";
    private static final String VALID_CRD_NAMESPACE = "crd-7namespace-3-test";
    private static final String VALID_ID = "validId";
    private static final Integer PAGE = 2;
    private static final Integer SIZE = 2;
    private static final String SORT = "version,asc";

    @Autowired
    private WorkloadInstanceVersionServiceImpl workloadInstanceVersionService;
    @Autowired
    private WorkloadInstanceVersionDtoMapper workloadInstanceVersionMapper;
    @MockBean
    private WorkloadInstanceVersionRepository repository;

    @Test
    void shouldCreatePrimaryVersionSuccessfully() {
        WorkloadInstance workloadInstance = getWorkloadInstance();
        WorkloadInstanceVersion savedVersion = getVersion(workloadInstance);
        savedVersion.setVersion(1);
        when(repository.findTopByWorkloadInstanceOrderByVersionDesc(workloadInstance)).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(savedVersion);

        WorkloadInstanceVersion version = workloadInstanceVersionService.createVersion(workloadInstance, VALUES_VERSION, HELM_SOURCE_VERSION);

        assertThat(version).isNotNull();
        assertThat(version.getId()).isNotNull();
        assertThat(version.getVersion()).isEqualTo(1);

        verify(repository).save(any());
    }

    @Test
    void shouldCreateSequenceVersionSuccessfully() {
        WorkloadInstance workloadInstance = getWorkloadInstance();
        WorkloadInstanceVersion previousVersion = getVersion(workloadInstance);
        WorkloadInstanceVersion savedVersion = getVersion(workloadInstance);
        savedVersion.setVersion(3);
        when(repository.findTopByWorkloadInstanceOrderByVersionDesc(workloadInstance)).thenReturn(Optional.of(previousVersion));
        when(repository.save(any())).thenReturn(savedVersion);

        WorkloadInstanceVersion version = workloadInstanceVersionService.createVersion(workloadInstance, VALUES_VERSION, HELM_SOURCE_VERSION);

        assertThat(version).isNotNull();
        assertThat(version.getId()).isNotNull();
        assertThat(version.getVersion()).isEqualTo(3);

        verify(repository).save(any());
    }

    @Test
    void shouldCreateSequenceVersionWithPreviousValuesVersionSuccessfully() {
        WorkloadInstance workloadInstance = getWorkloadInstance();
        WorkloadInstanceVersion previousVersion = getVersion(workloadInstance);
        WorkloadInstanceVersion savedVersion = getVersion(workloadInstance);
        savedVersion.setVersion(3);
        when(repository.findTopByWorkloadInstanceOrderByVersionDesc(workloadInstance)).thenReturn(Optional.of(previousVersion));
        when(repository.save(any())).thenReturn(savedVersion);

        WorkloadInstanceVersion version = workloadInstanceVersionService.createVersion(workloadInstance, null, HELM_SOURCE_VERSION);

        assertThat(version).isNotNull();
        assertThat(version.getId()).isNotNull();
        assertThat(version.getVersion()).isEqualTo(3);
        assertThat(version.getValuesVersion()).isEqualTo(previousVersion.getValuesVersion());

        verify(repository).save(any());
    }

    @Test
    void shouldGetWorkloadInstanceVersionDtoByWorkloadInstanceIdAndVersion() {
        WorkloadInstance workloadInstance = getWorkloadInstance();
        WorkloadInstanceVersion workloadInstanceVersion = getVersion(workloadInstance);
        when(repository.findByWorkloadInstanceWorkloadInstanceIdAndVersion(eq(VALID_ID), any())).thenReturn(Optional.of(workloadInstanceVersion));

        WorkloadInstanceVersionDto workloadInstanceVersionDto =
                workloadInstanceVersionService.getVersionDtoByWorkloadInstanceIdAndVersion(VALID_ID, 2);

        assertThat(workloadInstanceVersionDto).isNotNull();
        assertThat(workloadInstanceVersionDto.getVersion()).isEqualTo(2);
        assertThat(workloadInstanceVersionDto.getHelmSourceVersion()).isEqualTo(HELM_SOURCE_VERSION);
        assertThat(workloadInstanceVersionDto.getValuesVersion()).isEqualTo(VALUES_VERSION);
    }

    @Test
    void shouldGetWorkloadInstanceVersionByWorkloadInstanceIdAndVersion() {
        WorkloadInstance workloadInstance = getWorkloadInstance();
        WorkloadInstanceVersion workloadInstanceVersion = getVersion(workloadInstance);
        when(repository.findByWorkloadInstanceWorkloadInstanceIdAndVersion(eq(VALID_ID), any())).thenReturn(Optional.of(workloadInstanceVersion));

        WorkloadInstanceVersion version =
                workloadInstanceVersionService.getVersionByWorkloadInstanceIdAndVersion(VALID_ID, 2);

        assertThat(version).isNotNull();
        assertThat(version.getVersion()).isEqualTo(2);
        assertThat(version.getHelmSourceVersion()).isEqualTo(HELM_SOURCE_VERSION);
        assertThat(version.getValuesVersion()).isEqualTo(VALUES_VERSION);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenGetWorkloadInstanceVersionByWorkloadInstanceIdAndVersion() {
        when(repository.findByWorkloadInstanceWorkloadInstanceIdAndVersion(eq(VALID_ID), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> workloadInstanceVersionService.getVersionDtoByWorkloadInstanceIdAndVersion(VALID_ID, 2))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldGetWorkloadInstanceVersions() {
        WorkloadInstance workloadInstance = getWorkloadInstance();
        List<String> sortList = Collections.singletonList(SORT);
        Pageable pageable = PageRequest.of(PAGE, SIZE);
        Page<WorkloadInstanceVersion> resultsPage = new PageImpl<>(getWorkloadInstanceVersionsByPage(workloadInstance), pageable, 9L);

        when(repository.findAllByWorkloadInstanceWorkloadInstanceId(any(), any(Pageable.class))).thenReturn(resultsPage);

        PagedWorkloadInstanceVersionDto result = workloadInstanceVersionService.getAllVersionsByWorkloadInstance(VALID_ID, PAGE, SIZE, sortList);

        verify(repository).findAllByWorkloadInstanceWorkloadInstanceId(any(), any(Pageable.class));
        assertThat(result.getContent()).hasSize(SIZE);
    }

    @Test
    void shouldGetVersionForRollbackWhenRequestIsPresent() {
        WorkloadInstance workloadInstance = getWorkloadInstance();
        WorkloadInstanceVersion workloadInstanceVersion = getVersion(workloadInstance);
        workloadInstanceVersion.setVersion(2);
        when(repository.findByWorkloadInstanceWorkloadInstanceIdAndVersion(anyString(), anyInt())).thenReturn(Optional.of(workloadInstanceVersion));

        WorkloadInstanceVersion result = workloadInstanceVersionService.getVersionForRollback(workloadInstance, 2, OperationType.UPDATE);

        assertThat(result).isNotNull();
        verify(repository).findByWorkloadInstanceWorkloadInstanceIdAndVersion(anyString(), eq(2));
    }

    @Test
    void shouldGetVersionForRollbackWhenRequestIsAbsent() {
        WorkloadInstance workloadInstance = getWorkloadInstance();
        workloadInstance.setPreviousVersion(2);
        WorkloadInstanceVersion workloadInstanceVersion = getVersion(workloadInstance);
        workloadInstanceVersion.setVersion(workloadInstance.getPreviousVersion());
        when(repository.findByWorkloadInstanceAndVersion(workloadInstance, workloadInstance.getPreviousVersion()))
                .thenReturn(Optional.of(workloadInstanceVersion));

        WorkloadInstanceVersion result = workloadInstanceVersionService.getVersionForRollback(workloadInstance, null, OperationType.UPDATE);

        assertThat(result).isNotNull();
        verify(repository).findByWorkloadInstanceAndVersion(any(), eq(workloadInstance.getPreviousVersion()));
    }

    private WorkloadInstance getWorkloadInstance() {
        return WorkloadInstance.builder()
                .workloadInstanceId(VALID_ID)
                .workloadInstanceName(VALID_INSTANCE_NAME)
                .namespace(VALID_NAMESPACE)
                .crdNamespace(VALID_CRD_NAMESPACE)
                .build();
    }

    private WorkloadInstanceVersion getVersion(WorkloadInstance workloadInstance) {
        return WorkloadInstanceVersion.builder()
                .workloadInstance(workloadInstance)
                .valuesVersion(VALUES_VERSION)
                .helmSourceVersion(HELM_SOURCE_VERSION)
                .version(2)
                .id("some_random_id")
                .build();
    }

    private List<WorkloadInstanceVersion> getWorkloadInstanceVersionsByPage(WorkloadInstance workloadInstance) {
        List<WorkloadInstanceVersion> pagesList = new ArrayList<>();
        pagesList.add(getVersion(workloadInstance));
        WorkloadInstanceVersion nextVersion = getVersion(workloadInstance);
        nextVersion.setVersion(3);
        pagesList.add(nextVersion);
        return pagesList;
    }
}