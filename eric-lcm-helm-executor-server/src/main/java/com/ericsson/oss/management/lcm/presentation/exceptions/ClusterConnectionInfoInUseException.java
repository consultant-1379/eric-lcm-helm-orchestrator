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
package com.ericsson.oss.management.lcm.presentation.exceptions;

public class ClusterConnectionInfoInUseException extends RuntimeException {

    public ClusterConnectionInfoInUseException() {
    }

    public ClusterConnectionInfoInUseException(final String message) {
        super(message);
    }
}
