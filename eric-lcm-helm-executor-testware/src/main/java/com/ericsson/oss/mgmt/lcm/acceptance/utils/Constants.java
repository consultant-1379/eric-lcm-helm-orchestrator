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

public final class Constants {
    private Constants(){}

    public static final String SERVICE_IP = "access.ip";
    public static final String SERVICE_PORT = "access.port";
    public static final String DEPLOYMENT_NAMESPACE = "deployment.namespace";
    public static final String IS_LOCAL = "isLocal";
    public static final String NAMESPACE = "func_namespace";
    public static final String KUBE_CONFIG_PATH = "kubeconfig_path";
    public static final String CHART_REGISTRY_URL = "chart.registry.url";
    public static final String CHART_REGISTRY_USER = "chart.registry.user";
    public static final String CHART_REGISTRY_PASSWD = "chart.registry.passwd";

    public static final String HEALTH_URL = "http://%s:%s/actuator/health";
    public static final String BRO_HEALTH_URL = "http://%s:%s/cnwlcm/v1/backup_and_restore/health";
    public static final String BRO_BASIC_URL = "http://%s:%s/cnwlcm/v1/backup_and_restore";
    public static final String BRO_GET_URL = "http://%s:%s/cnwlcm/v1/backup_and_restore/%s";
    public static final String BRO_IMPORT_URL = "http://%s:%s/cnwlcm/v1/backup_and_restore/imports";
    public static final String BRO_EXPORT_URL = "http://%s:%s/cnwlcm/v1/backup_and_restore/exports";
    public static final String WORKLOAD_INSTANCES_URL = "http://%s:%s/cnwlcm/v1/workload_instances";
    public static final String POST_INSTANCE_HELMFILE_FETCHER = "http://%s:%s/cnwlcm/v1/helmfile_fetcher/workload_instances";
    public static final String PUT_INSTANCE_HELMFILE_FETCHER = "http://%s:%s/cnwlcm/v1/helmfile_fetcher/workload_instances/%s";
    public static final String HELMFILE_BUILDER_WORKLOAD_INSTANCES_URL = "http://%s:%s/cnwlcm/v1/helmfile_builder/workload_instances";
    public static final String PUT_WORKLOAD_INSTANCES_URL = "http://%s:%s/cnwlcm/v1/workload_instances/%s";
    public static final String PUT_HELMFILE_BUILDER_WORKLOAD_INSTANCES_URL = "http://%s:%s/cnwlcm/v1/helmfile_builder/workload_instances/%s";
    public static final String GET_OPERATION_URL = "http://%s:%s/cnwlcm/v1/operations/%s";
    public static final String GET_OPERATIONS_URL = "http://%s:%s/cnwlcm/v1/operations/workload_instances/%s";
    public static final String GET_VERSIONS_URL = "http://%s:%s/cnwlcm/v1/workload_instances/%s/versions";
    public static final String GET_VERSION_URL = "http://%s:%s/cnwlcm/v1/workload_instances/%s/versions/%s";
    public static final String GET_OPERATION_LOGS_URL = GET_OPERATION_URL + "/logs";
    public static final String WORKLOAD_INSTANCES_OPERATIONS_URL = WORKLOAD_INSTANCES_URL + "/%s/operations";
    public static final String CLUSTER_CONNECTION_INFO_URL = "http://%s:%s/cnwlcm/v1/cluster_connection_info";
    public static final String TYPE_TERMINATE = "terminate";
    public static final String BRO_MANAGERS_IDENTIFIER = "managers";
    public static final String TAR_COMMAND = "cd %s; tar -cvf %s .";
    public static final String HELM_PACKAGE_COMMAND = "cd %s; helm package %s -d ./";
    public static final String POD_NAME_COMMAND = "kubectl get pods --no-headers -o custom-columns=\":metadata.name\" -n %s | grep "
            + "eric-lcm-helm-executor|head -n1";
    public static final String CLUSTER_REQUEST_KEY = "clusterConnectionInfo";
    public static final String HELM_SOURCE_KEY = "helmSource";
    public static final String WORKLOAD_INSTANCE_NAME_KEY = "workloadInstanceName";
    public static final String NAMESPACE_KEY = "namespace";
    public static final String REPOSITORY_KEY = "repository";
    public static final String TIMEOUT_KEY = "timeout";
    public static final String CHARTS_KEY = "charts";
    public static final String CLUSTER_KEY = "cluster";
    public static final String ADDITIONAL_PARAMETERS = "additionalParameters";
    public static final String IS_URL_TO_HELM_REGISTRY_KEY = "isUrlToHelmRegistry";
    public static final String WORKLOAD_INSTANCE_POST_REQUEST_DTO = "workloadInstancePostRequestDto";
    public static final String WORKLOAD_INSTANCE_WITH_URL_REQUEST_DTO = "workloadInstanceWithURLRequestDto";
    public static final String WORKLOAD_INSTANCE_WITH_URL_PUT_REQUEST_DTO = "workloadInstanceWithURLPutRequestDto";
    public static final String WORKLOAD_INSTANCE_POST_HELMFILE_BUILDER_REQUEST_DTO = "workloadInstanceWithChartsRequestDto";
    public static final String WORKLOAD_INSTANCE_PUT_HELMFILE_BUILDER_REQUEST_DTO = "workloadInstanceWithChartsPutRequestDto";
    public static final String VALUES_KEY = "values";
    public static final String GLOBAL_VALUES_KEY = "globalValues";
    public static final String BRO_NAME_KEY = "backupName";
    public static final String BRO_MANAGER_ID_KEY = "backupManagerId";
    public static final String BRO_URI_KEY = "uri";
    public static final String BRO_PASSWORD_KEY = "password";
    public static final String BRO_RESPONSE_ID = "id";
    public static final String URL_KEY = "url";
    public static final String VALUES_PATH = "src/main/resources/testData/instantiate/helmfile-test/values.yaml";
    public static final String VALUES_TO_UPDATE_PATH = "src/main/resources/testData/instantiate/helmfile-test-for-update/values.yaml";
    public static final String CLUSTER_CONFIG_INFO_PATH = "src/main/resources/testData/clusterConnectionInfo/cluster-connection-info.config";
    public static final String CLUSTER_CONFIG_INFO_PATH_LOCAL =
            "src/main/resources/testData/clusterConnectionInfo/cluster-connection-info-local.config";
    public static final String COMPLETED_STATE = "COMPLETED";
    public static final String OPERATION_TYPE_INSTANTIATE = "INSTANTIATE";
    public static final String OPERATION_TYPE_TERMINATE = "TERMINATE";
    public static final String OPERATION_TYPE_UPDATE = "UPDATE";
    public static final String OPERATION_TYPE_REINSTANTIATE = "REINSTANTIATE";
    public static final String OPERATION_TYPE_ROLLBACK = "ROLLBACK";
    public static final String WORKLOAD_INSTANCE_ID = "WorkloadInstance";
    public static final String OPERATION_ID = "Operation";
    public static final String RESPONSE_PREFIX = "Response is {}";
    public static final String CHARTS_PART = "/charts/";
}