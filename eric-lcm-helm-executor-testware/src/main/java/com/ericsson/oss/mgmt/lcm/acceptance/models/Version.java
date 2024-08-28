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

package com.ericsson.oss.mgmt.lcm.acceptance.models;

import java.util.List;

import lombok.Getter;

@Getter
public class Version {

    private List<Item> content;

    @Getter
    public static class Item {
        private String id;
        private Integer version;
        private String helmSourceVersion;
        private String valuesVersion;
    }

}
