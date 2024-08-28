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

public final class CommandConstants {
    private CommandConstants(){}

    public static final int EXTRACT_ARCHIVE_TIMEOUT = 5;
    public static final int CHECK_FILE_PRESENCE_IN_ARCHIVE_TIMEOUT = 5;

    public static final String SPACE = " ";
    public static final String SLASH = "/";
    public static final String BASE_COMMAND = "helmfile";
    public static final String HELM_COMMAND = "helm";
    public static final String EXTRACT_ARCHIVE_COMMAND = "tar -xf %s -C %s";
    public static final String FETCH_ARCHIVE_CONTENT = "tar -tf %s";
    public static final String TAR_COMMAND = "cd %s; tar -cvf %s .";
    public static final String NAMESPACE_ARGUMENT = "-n";
    public static final String INSTALL_ARGUMENT = "install";
    public static final String UPGRADE_ARGUMENT = "upgrade --install";
    public static final String APPLY_ARGUMENT = "apply";
    public static final String LIST_ARGUMENT = "list --output json";
    public static final String FILTER_ARGUMENT = "--filter";
    public static final String NAMESPACE_SELECTOR_ARGUMENT = "--selector namespace=%s";
    public static final String UNINSTALL_ARGUMENT = "uninstall";
    public static final String VALUES_ARGUMENT = "--values";
    public static final String FILE_ARGUMENT = "--file";
    public static final String STATE_VALUES_FILE_ARGUMENT = "--state-values-file";
    public static final String WAIT_ARGUMENT = "--wait";
    public static final String SENSITIVE_DATA_REPLACEMENT = "*****";
    public static final String PASSWORD_ARGUMENT = "--password";
    public static final String USERNAME_ARGUMENT = "--username";
    public static final String CA_FILE_ARGUMENT = "--ca-file";
    public static final String CERT_FILE_ARGUMENT = "--cert-file";
    public static final String KEY_FILE_ARGUMENT = "--key-file";
    public static final String CASCADE_TYPE = "--cascade=foreground";
    public static final String HELM_TIMEOUT_ARGUMENT = "--timeout %ss";
    public static final String HELMFILE_TIMEOUT_ARGUMENT = "--state-values-set global.default.timeout=%s";
    public static final String DOCKER_URL_KEY = "global.registry.url";
    public static final String IMAGE_REPO_PREFIX = "image.repo.prefix";
    public static final String DOCKER_CREDS_SECRET_NAME = "regcred-%s";
    public static final String GLOBAL_APP_NAMESPACE = "global.app.namespace";
    public static final String GLOBAL_CRD_NAMESPACE = "global.crd.namespace";
    public static final String GLOBAL_CHART_REGISTRY = "global.chart.registry";
    public static final String KUBE_CONFIG_VALUE = "KUBECONFIG=%s";
    public static final String GLOBAL_PULL_SECRET = "global.pullSecret";
    public static final String GLOBAL_APP_ENABLED_ARGUMENT = "global.app.enabled";
    public static final String VERIFY_HELMFILE = "helmfile --state-values-file %s --file %s template --output-dir output";
    public static final String VERIFY_INTEGRATION_CHART = "helm template %s --output-dir %s/output";
    public static final String DEFAULT_CLUSTER_IDENTIFIER = "default-cluster";
}
