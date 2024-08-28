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

package com.ericsson.oss.mgmt.lcm.acceptance.models.characteristics;

import static com.ericsson.oss.mgmt.lcm.acceptance.utils.CharacteristicsReportConstants.DESCRIPTION_URL;

import io.qameta.allure.internal.shadowed.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtherInfo {
    @JsonProperty("description_url")
    private String descriptionUrl;

    public OtherInfo() {
        this.descriptionUrl = DESCRIPTION_URL;
    }
}
