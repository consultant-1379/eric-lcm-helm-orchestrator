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

package com.ericsson.oss.management.lcm.utils.validator;

import java.util.Optional;
import java.util.regex.Pattern;

import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidInputException;

public final class WorkloadInstanceValidator {
    private WorkloadInstanceValidator(){}
    /**
     * Template requirements:
     * contain at most 63 characters
     * contain only lowercase alphanumeric characters or '-'
     * start with an alphanumeric character
     * end with an alphanumeric character
     */
    private static final Pattern STATE_PATTERN_EXPRESSION = Pattern.compile("^[a-z0-9][a-z0-9-]{0,61}[a-z0-9]$");

    private static final Pattern WORKLOAD_INSTANCE_NAME_PATTERN_EXPRESSION = Pattern.compile("[a-z0-9]([-a-z0-9]*[a-z0-9])?");

    public static void validate(WorkloadInstance workloadInstance) {

        StringBuilder errors = new StringBuilder();

        String name = workloadInstance.getWorkloadInstanceName();
        validate(WORKLOAD_INSTANCE_NAME_PATTERN_EXPRESSION, name, "WorkloadInstanceName %s is invalid", errors);

        String namespace = workloadInstance.getNamespace();
        validate(STATE_PATTERN_EXPRESSION, namespace, "Namespace %s is invalid", errors);

        Optional<String> crdNamespace = Optional.ofNullable(workloadInstance.getCrdNamespace());
        crdNamespace.ifPresent(crdNamespaceGet ->
                    validate(STATE_PATTERN_EXPRESSION, crdNamespaceGet, "CrdNamespace %s is invalid", errors)
        );

        if (errors.length() != 0) {
            throw new InvalidInputException(errors.toString());
        }
    }

    private static void validate(Pattern pattern, String toBeValidated, String message, StringBuilder errors) {
        boolean isValid = ValidationUtils.matchByPattern(pattern, toBeValidated);
        if (!isValid) {
            errors.append(String.format(message, toBeValidated)).append("\n");
        }
    }
}
