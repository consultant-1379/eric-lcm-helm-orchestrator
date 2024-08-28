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

public final class ClusterConnectionInfoConstants {
    private ClusterConnectionInfoConstants(){}
    public static final Set<String> SORT_COLUMNS = Set.of("name", "status");
    public static final String NAME = "name";
    public static final String CLUSTER_CONFIG_PREFIX_REGEX = "[0-9a-zA-Z][0-9a-zA-Z-_]*";
    public static final String REGEX_FOR_CLUSTER_CONFIG_FILE_NAME = CLUSTER_CONFIG_PREFIX_REGEX + "\\.config$";
    public static final String CLUSTER_INVALID_FILENAME_MESSAGE =
            "ClusterConfig name not in correct format";
    public static final String CURRENTLY_SUPPORTED_IS_TEXT_FORMAT =
            "Invalid upload content type. Valid content type that is currently supported is 'plain/text' format";
    public static final String UNABLE_TO_PARSE_YAML_FILE_ERROR_MESSAGE =
            "Unable to parse the yaml file due to [%s], Please provide a valid yaml";
    public static final String NAMESPACE_PROVIDED_ERROR_MESSAGE = "Namespace details should not be provided in context";
    public static final String CLUSTER_FILENAME_EMPTY_MESSAGE = "ClusterConfig name cannot be empty";
    public static final String UNABLE_PARSE_YAML_MESSAGE = "Unable to parse the yaml file";
    public static final String CONTENT_TYPE = "text/plain";
}
