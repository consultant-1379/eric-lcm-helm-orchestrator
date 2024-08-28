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

package com.ericsson.oss.management.lcm.repositories;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceVersion;

@Repository
public interface WorkloadInstanceVersionRepository extends JpaRepository<WorkloadInstanceVersion, String> {

    Optional<WorkloadInstanceVersion> findTopByWorkloadInstanceOrderByVersionDesc(WorkloadInstance workloadInstance);

    Optional<WorkloadInstanceVersion> findByWorkloadInstanceAndVersion(WorkloadInstance workloadInstance, Integer version);

    Optional<WorkloadInstanceVersion> findByWorkloadInstanceWorkloadInstanceIdAndVersion(String workloadInstanceId, Integer version);

    Page<WorkloadInstanceVersion> findAllByWorkloadInstanceWorkloadInstanceId(String workloadInstanceId, Pageable pageable);
}
