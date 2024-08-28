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

package com.ericsson.oss.mgmt.lcm.acceptance.utils;

import java.util.Collections;
import java.util.List;

public final class CharacteristicsReportConstants {
    private CharacteristicsReportConstants() {}

    public static final String CHARACTERISTICS_REPORT_PATH = System.getenv("HOME");

    public static final String POD_METRICS_COMMAND = "kubectl get PodMetrics %s -n %s -o json";

    public static final String INSTANTIATE_USE_CASE = "Manually instantiate application";
    public static final String INSTANTIATE_USE_CASE_DESCRIPTION = "This service will accept Helmfile.tgz and values and apply them to a target "
            + "cluster.";
    public static final String UPGRADE_USE_CASE = "Manually upgrade application";
    public static final String UPGRADE_USE_CASE_DESCRIPTION = "This service will accept Helmfile.tgz and values and apply them to a target cluster "
            + "to update existing instance.";
    public static final String TERMINATE_USE_CASE = "Manually terminate application";
    public static final String TERMINATE_USE_CASE_DESCRIPTION = "This service will accept id of deployed workload instance and terminate it on a "
            + "target cluster.";
    public static final String REINSTANTIATE_USE_CASE = "Manually reinstantiate application";
    public static final String REINSTANTIATE_USE_CASE_DESCRIPTION = "This service will accept id of previously terminated workload instance and "
            + "redeploy it on a target cluster.";
    public static final String ROLLBACK_USE_CASE = "Manually rollback application";
    public static final String ROLLBACK_USE_CASE_DESCRIPTION = "This service will accept id of workload instance and "
            + "rollback it to the previous version on a target cluster.";

    public static final String CPU_METRIC = "CPU";
    public static final String MEMORY_METRIC = "Memory";

    public static final String MODEL_VERSION = "0.0.4";
    public static final String SERVICE_NAME = "Helmfile Executor";
    public static final String SERVICE_VERSION = "0.3.1";
    public static final String FLAVOR = "minimum";
    public static final String CONTAINER_NAME = "eric-lcm-helm-executor";
    public static final String CONTAINER_CPU_REQ = "250m";
    public static final String CONTAINER_CPU_LIMIT = "500m";
    public static final String CONTAINER_MEM_REQ = "500Mi";
    public static final String CONTAINER_MEM_LIMIT = "1Gi";
    public static final String IMAGE_SIZE = "0.67Mi";
    public static final String TPS = "5";
    public static final String REQUEST_AVG_SIZE = "6.73kb";
    public static final String TOTAL_REQUESTS = "20";
    public static final String TOTAL_ERRORS = "0";
    public static final String LATENCY = "4343ms";
    public static final String TEST_ENVIRONMENT_CLUSTER = "hahn117";
    public static final String TEST_ENVIRONMENT_MEMORY = "298Gi";
    public static final String CAAS = "CCD-2.15.1";
    public static final String KUBERNETES_VERSION = "1.21.1";
    public static final String DESCRIPTION_URL = "http://url";
    public static final String CPU_MODEL = "Xeon_Phi-31S1P";
    public static final int INSTANCES = 1;
    public static final int DURATION = 120;
    public static final int MEMORY_AVG_MIB = 18;
    public static final int CPU_MHZ = 2400;
    public static final double CPU_AVG_MILLI_CORES = 2.52;
    public static final double BOGOMIPS = 4589.37;
    public static final List<String> LABELS = Collections.singletonList("performance");
}
