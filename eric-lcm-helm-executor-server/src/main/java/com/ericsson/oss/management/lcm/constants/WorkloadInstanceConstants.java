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

package com.ericsson.oss.management.lcm.constants;

import java.util.Set;

public final class WorkloadInstanceConstants {
    private WorkloadInstanceConstants(){}
    public static final String WORKLOAD_INSTANCE_ID = "workloadInstanceId";
    public static final Set<String> SORT_COLUMNS = Set.of(WORKLOAD_INSTANCE_ID, "workloadInstanceName");

    public static final String WORKLOAD_INSTANCE_VERSION_ID = "id";
    public static final Set<String> WORKLOAD_INSTANCE_VERSION_SORT_COLUMNS = Set.of(WORKLOAD_INSTANCE_VERSION_ID, "version");
}
