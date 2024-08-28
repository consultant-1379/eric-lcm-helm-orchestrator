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

public final class OperationConstants {
    private OperationConstants(){}
    public static final Set<String> SORT_COLUMNS = Set.of("startTime", "state");
    public static final String INSTANCE_ID = "workloadInstance.workloadInstanceId";
}
