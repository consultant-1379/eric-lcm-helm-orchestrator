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

import java.util.List;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ericsson.oss.management.lcm.model.entity.Release;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;

@Repository
public interface ReleaseRepository extends JpaRepository<Release, String> {

    List<Release> findByWorkloadInstance(WorkloadInstance instance);

    @Transactional
    void deleteAllByWorkloadInstance(WorkloadInstance instance);

}
