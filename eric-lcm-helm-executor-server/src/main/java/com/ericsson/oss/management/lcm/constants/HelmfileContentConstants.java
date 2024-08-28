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

public final class HelmfileContentConstants {
    private HelmfileContentConstants(){}

    public static final String HELMFILE_NAME = "helmfile-generated-by-executor";
    public static final String HELMFILE_BASIC_VERSION = "0.1.0-1";
    public static final String CRD_NAMESPACE_TEMPLATED = "{{ .Values | get \"global.crd.namespace\" }}";
    public static final String NAMESPACE_TEMPLATED = "{{ .Values | get \"global.app.namespace\" }}";
    public static final String CHART_TEMPLATED = "{{ .Values | get \"repository\" \"repo\" }}/%s";
    public static final String REPOSITORY_NAME = "repo";
    public static final String RELEASES_HEADER = "releases:\n";
    public static final String METADATA_CONTENT = "name: %s\nversion: %s";
    public static final String CRD_HEADER =
              "helmfiles:\n"
            + "  - path: crds-helmfile.yaml\n"
            + "    values:\n"
            + "    - {{ toYaml .Values | nindent 6 }}\n"
            + "\n"
            + "---\n\n";
    public static final String INSTALLED_TEMPLATED = "{{ and ( .Values | get \"global.app.enabled\") ( .Values | get \"%s"
            + ".enabled\") }}";
    public static final String VALUES_TEMPLATE_CONTENT =
              "{{ if hasKey .Values \"global\" }}\n"
            + "global:\n"
            + "{{ .Values | get \"global\" | toYaml | indent 2 }}\n"
            + "{{ end }}\n"
            + "{{ if hasKey .Values .Release.Name }}\n"
            + "{{ .Values | get .Release.Name | toYaml }}\n"
            + "{{ end }}";

}
