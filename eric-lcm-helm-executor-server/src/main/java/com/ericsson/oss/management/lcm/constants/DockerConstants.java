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

public final class DockerConstants {
    private DockerConstants() {}
    public static final String METADATA_AUTH_KEY = ".dockerconfigjson";
    public static final String LABEL_APP_KEY = "app";
    public static final String DOCKER_SECRET_TYPE = "kubernetes.io/dockerconfigjson";
    public static final String LABEL_APP_NAME_VALUE = "eric-lcm-helm-orchestrator";
    public static final String NS_METADATA_LABELS_NAME = "name";

}
