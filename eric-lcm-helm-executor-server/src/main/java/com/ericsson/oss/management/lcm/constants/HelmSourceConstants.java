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

public final class HelmSourceConstants {
    private HelmSourceConstants(){}
    public static final String HELMFILE_YAML_FILENAME = "helmfile.yaml";
    public static final String REPOSITORIES_YAML_FILENAME = "repositories.yaml";
    public static final String CRD_HELMFILE_YAML_FILENAME = "crds-helmfile.yaml";
    public static final String HELMFILE_VERSION_KEY = "version";
    public static final String HELM_CHART_VERSION_KEY = "version";
    public static final String VALUES_YAML = "values.yaml";
    public static final String GIT_REPO_VALUES_COPY_NAME = "values-copy-from-git.yaml";
    public static final String ROLLBACK_VALUES_YAML = "rollback-values.yaml";
    public static final String METADATA_YAML = "metadata.yaml";
    public static final String VALUES_TEMPLATE = "%s.yaml.gotmpl";
    public static final String VALUES_TEMPLATE_DIR = "values-templates";
    public static final String CHART_YAML = "Chart.yaml";
    public static final String HELMSOURCE_TGZ = "helmsource.tgz";
    public static final String HELMFILE_TGZ = "helmfile.tgz";
    public static final String KUBE_CONFIG = "kube.config";
    public static final boolean ENABLED_ARGUMENT_VALUE_TRUE = true;
    public static final boolean ENABLED_ARGUMENT_VALUE_FALSE = false;
    public static final String OUTPUT_DIR = "/output";

}
